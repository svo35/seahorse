/**
 * Copyright 2015, CodiLime Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepsense.models.json.workflow

import org.joda.time.DateTime
import spray.json._

import io.deepsense.graph.{State, Status}
import io.deepsense.models.entities.Entity
import io.deepsense.models.json.graph.GraphJsonProtocol.GraphWriter
import io.deepsense.models.workflows._

class WorkflowWithSavedResultsJsonProtocolSpec extends WorkflowJsonTestSupport
    with WorkflowWithSavedResultsJsonProtocol {

  "WorkflowWithSavedResults" should {

    "be serialized to json" in {
      val (workflow, json) = workflowWithSavedResultsFixture
      workflow.toJson shouldBe json
    }

    "be deserialized from json" in {
      val (workflow, json) = workflowWithSavedResultsFixture
      json.convertTo[WorkflowWithSavedResults] shouldBe workflow
    }
  }

  def workflowWithSavedResultsFixture: (WorkflowWithSavedResults, JsObject) = {

    val (executionReport, executionReportJson) = executionReportFixture

    val workflowId = Workflow.Id.randomId

    val workflow = WorkflowWithSavedResults(
      workflowId,
      WorkflowMetadata(WorkflowType.Batch, "0.4.0"),
      graph,
      ThirdPartyData("{ \"example\": [1, 2, 3] }"),
      executionReport)

    val workflowJson = JsObject(
      "id" -> JsString(workflowId.toString),
      "metadata" -> JsObject(
        "type" -> JsString("batch"),
        "apiVersion" -> JsString("0.4.0")
      ),
      "workflow" -> graph.toJson(GraphWriter),
      "thirdPartyData" -> JsObject(
        "example" -> JsArray(Vector(1, 2, 3).map(JsNumber(_)))
      ),
      "executionReport" -> executionReportJson
    )

    (workflow, workflowJson)
  }

  def executionReportFixture: (ExecutionReportWithId, JsObject) = {

    val startTimestamp = "2015-05-12T21:11:09.000Z"
    val finishTimestamp = "2015-05-12T21:12:50.000Z"

    val entity1Id = Entity.Id.randomId
    val entity2Id = Entity.Id.randomId

    val executionReportId: ExecutionReportWithId.Id = ExecutionReportWithId.Id.randomId

    val executionReport = ExecutionReportWithId(
      executionReportId,
      Status.Completed,
      None,
      Map(
        node1.id -> State(
          Status.Completed,
          Some(DateTime.parse(startTimestamp)),
          Some(DateTime.parse(finishTimestamp)),
          progress = None,
          Some(Seq(entity1Id, entity2Id)),
          None
        )
      ),
      EntitiesMap()
    )
    val executionReportJson = JsObject(
      "id" -> JsString(executionReportId.toString()),
      "status" -> JsString("COMPLETED"),
      "error" -> JsNull,
      "nodes" -> JsObject(
        node1.id.toString -> JsObject(
          "status" -> JsString("COMPLETED"),
          "started" -> JsString(startTimestamp),
          "ended" -> JsString(finishTimestamp),
          "results" -> JsArray(
            JsString(entity1Id.toString),
            JsString(entity2Id.toString)
          ),
          "error" -> JsNull
        )
      ),
      "resultEntities" -> JsObject()
    )

    (executionReport, executionReportJson)
  }

}
