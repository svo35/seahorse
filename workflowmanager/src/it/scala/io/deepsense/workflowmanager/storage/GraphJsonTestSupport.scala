/**
 * Copyright (c) 2015, CodiLime Inc.
 */

package io.deepsense.workflowmanager.storage

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import spray.json.{DefaultJsonProtocol, JsObject}

import io.deepsense.deeplang.DOperation
import io.deepsense.graph.Endpoint

trait GraphJsonTestSupport
  extends WordSpec
  with MockitoSugar
  with DefaultJsonProtocol
  with Matchers {

  def assertEndpointMatchesJsObject(edgeEnd: Endpoint, edgeEndJs: JsObject): Unit = {
    assert(edgeEndJs.fields("nodeId").convertTo[String] == edgeEnd.nodeId.value.toString)
    assert(edgeEndJs.fields("portIndex").convertTo[Int] == edgeEnd.portIndex)
  }

  def endpointMatchesJsObject(edgeEnd: Endpoint, edgeEndJs: JsObject): Boolean = {
    edgeEndJs.fields("nodeId").convertTo[String] == edgeEnd.nodeId.value.toString &&
    edgeEndJs.fields("portIndex").convertTo[Int] == edgeEnd.portIndex
  }

  def mockOperation(
      inArity: Int,
      outArity: Int,
      id: DOperation.Id,
      name: String): DOperation = {
    val dOperation = mock[DOperation]
    when(dOperation.inArity).thenReturn(inArity)
    when(dOperation.outArity).thenReturn(outArity)
    when(dOperation.id).thenReturn(id)
    when(dOperation.name).thenReturn(name)
    when(dOperation.paramValuesToJson).thenReturn(JsObject())
    dOperation
  }
}
