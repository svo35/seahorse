/**
 * Copyright (c) 2015, CodiLime Inc.
 *
 * Owner: Radoslaw Kotowski
 */

package io.deepsense.graph

import java.util.UUID

import scala.collection.mutable.{Map, Set}
import scala.reflect.runtime.{universe => ru}

import io.deepsense.deeplang.{DKnowledge, DOperable, DOperation, InferContext}
import io.deepsense.graph.Node.State
import io.deepsense.graph.Node.State.{Progress, Status}

/**
 * Colours of node.
 *
 * Used for cycle checking. This object should be inner object
 * of Graph. However it makes serialization of Graph impossible.
 * This is why a decision was made to move it outside of Graph.
 */
private[graph] object Color extends Enumeration {
  type Color = Value
  val WHITE, GREY, BLACK = Value
}

/**
 * Execution Graph of the experiment.
 * Nodes of this graph contain operations and state.
 * State of each node can be changed during the execution.
 * This class is not thread safe.
 */
@SerialVersionUID(1L)
class Graph extends Serializable {
  private[graph] case class GraphNode(id: Node.Id, operation: DOperation) extends Node {
    var color: Color.Color = Color.WHITE
    var state: State = State.inDraft
    val predecessors: Array[Option[GraphNode]] = Array.fill(operation.inArity) { None }
    val successors: Array[Set[GraphNode]] = Array.fill(operation.outArity) { Set() }

    def addPredecessor(index: Int, node: GraphNode): Unit = {
      require(predecessors(index) == None)
      predecessors(index) = Some(node)
    }

    def addSuccessor(index: Int, node: GraphNode): Unit = successors(index) += node

    /** Returns port index which contains the given successor. */
    def getSuccessorPort(node: GraphNode): Option[Int] = {
      val successorIndex = successors.indexWhere(_.contains(node))
      if (successorIndex != -1) Some(successorIndex) else None
    }

    /** Returns graph knowledge with knowledge inferred for the given node. */
    def inferKnowledge(context: InferContext, graphKnowledge: GraphKnowledge): GraphKnowledge = {
      val knowledge = for (portIndex <- 0 until predecessors.size)
        yield inputKnowledgeForInputPort(context, graphKnowledge, portIndex)
      val inferredKnowledge = operation.inferKnowledge(context)(knowledge.toVector)
      graphKnowledge.addKnowledge(this.id, inferredKnowledge)
    }

    /** Returns suitable input knowledge for the given input port index. */
    private def inputKnowledgeForInputPort(
        context: InferContext,
        graphKnowledge: GraphKnowledge,
        portIndex: Int): DKnowledge[DOperable] = {
      // TODO: find a way to delete this cast
      val inPortType = operation.inPortTypes(portIndex).asInstanceOf[ru.TypeTag[DOperable]]
      predecessors(portIndex) match {
        case None => DKnowledge(context.dOperableCatalog.concreteSubclassesInstances(inPortType))
        case Some(predecessor) =>
          val outPortIndex = predecessor.getSuccessorPort(this).get
          val predecessorKnowledge = graphKnowledge.getKnowledge(predecessor.id)(outPortIndex)
          predecessorKnowledge.filterTypes(inPortType.tpe)
          // here it is possible to decide what color graph edge should have
      }
    }

    def markWhite(): Unit = color = Color.WHITE

    def markGrey(): Unit = color = Color.GREY

    def markBlack(): Unit = color = Color.BLACK

    def resetState(): Unit = markWhite()

    override def toString: String = id.toString
  }

  private val nodes: Map[Node.Id, GraphNode] = Map()

  def addNode(id: Node.Id, operation: DOperation): Node = {
    val node = GraphNode(id, operation)
    nodes(id) = node
    node
  }

  /**
   * Provides access to all graph nodes.
   * @return List of graph nodes
   */
  def nodesList : List[Node] = {
    nodes.values.toList
  }

  def addEdge(nodeFrom: Node.Id, nodeTo: Node.Id, portFrom: Int, portTo: Int): Unit = {
    nodes(nodeFrom).addSuccessor(portFrom, nodes(nodeTo))
    nodes(nodeTo).addPredecessor(portTo, nodes(nodeFrom))
  }

  def readyNodes: List[Node] = {
    val queuedNodes = nodes.values.filter(_.state.status == Status.QUEUED)
    queuedNodes.filter(_.predecessors.forall(
      (p: Option[Node]) => p.isDefined && p.get.state.status == Status.COMPLETED)).toList
  }

  def getNode(id: Node.Id): Node = nodes(id)

  def markAsInDraft(id: Node.Id): Unit = {
    nodes(id).state = State.inDraft
  }

  def markAsQueued(id: Node.Id): Unit = {
    nodes(id).state = State.queued
  }

  def markAsRunning(id: Node.Id): Unit = {
    val node = nodes(id)
    val total = 10 // TODO: just a default value. Change it when DOperation will support it.
    node.state = State.running(Progress(0, total))
  }

  def markAsCompleted(id: Node.Id, results: List[UUID]): Unit = {
    val node = nodes(id)
    node.state = node.state.completed(results)
  }

  def markAsFailed(id: Node.Id): Unit = {
    val node = nodes(id)
    node.state = node.state.failed
  }

  def markAsAborted(id: Node.Id): Unit = {
    val node = nodes(id)
    node.state = node.state.aborted
  }

  def reportProgress(id: Node.Id, current: Int): Unit = {
    val node = nodes(id)
    val total = 10 // TODO: just a default value. Change it when DOperation will support it.
    node.state = node.state.withProgress(Progress(current, total))
  }

  def containsCycle: Boolean = !topologicallySorted.isDefined

  /**
   * Returns a list of topologically sorted nodes.
   * Returns None if any node reachable from the start node lays on cycle.
   */
  private[graph] def topologicallySorted: Option[List[GraphNode]] = {
    var sorted: Option[List[GraphNode]] = Some(List.empty)
    nodes.values.foreach(n => sorted = topologicalSort(n, sorted))
    resetState()
    sorted
  }

  /** Sorts nodes topologically. */
  private def topologicalSort(
      node: GraphNode,
      sortedSoFar: Option[List[GraphNode]]): Option[List[GraphNode]] = {
    node.color match {
      case Color.BLACK => sortedSoFar
      case Color.GREY => None
      case Color.WHITE => {
        node.markGrey()
        var l = sortedSoFar
        node.successors.foreach(_.foreach(s => l = topologicalSort(s, l)))
        node.markBlack()
        l.map(node::_)
      }
    }
  }

  private def resetState(): Unit = nodes.values.foreach(_.resetState())

  /** Returns a graph knowledge with inferred knowledge for every node. */
  def inferKnowledge(context: InferContext): GraphKnowledge = {
    val sorted = topologicallySorted.get
    sorted.foldLeft(GraphKnowledge())((knowledge, node) => node.inferKnowledge(context, knowledge))
  }

  def size: Int = nodes.size

  override def hashCode(): Int = {
    val state = Seq(nodes)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}