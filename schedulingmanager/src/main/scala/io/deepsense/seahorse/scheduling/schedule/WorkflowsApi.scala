/**
 * Copyright (c) 2016, CodiLime Inc.
 */

package io.deepsense.seahorse.scheduling.schedule

import java.net.URL
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import spray.http.BasicHttpCredentials

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import io.deepsense.commons.utils.LoggerForCallerClass
import io.deepsense.models.workflows.{Workflow, WorkflowInfo}
import io.deepsense.seahorse.scheduling.SchedulingManagerConfig
import io.deepsense.workflowmanager.client.WorkflowManagerClient
import io.deepsense.workflowmanager.model.WorkflowDescription

private[schedule] object WorkflowsApi {
  private[this] val logger = LoggerForCallerClass()
  private[this] lazy val workflowManagerConfig = SchedulingManagerConfig.config.getConfig("workflow-manager")
  private[this] lazy val workflowClient = {
    val url = new URL(workflowManagerConfig.getString("url") + "/v1/workflows/")
    logger.info(s"Creating workflows client with url $url")
    val timeout = Timeout(FiniteDuration(workflowManagerConfig.getDuration("timeout").toMillis, TimeUnit.MILLISECONDS))
    new WorkflowManagerClient(
      apiUrl = url,
      mandatoryUserId = RunWorkflowJobContext.userId,
      mandatoryUserName = RunWorkflowJobContext.userName,
      credentials = Some(BasicHttpCredentials(
        workflowManagerConfig.getString("user"),
        workflowManagerConfig.getString("pass"))))(
      RunWorkflowJobContext.actorSystem, timeout)
  }

  def getWorkflowInfo(id: Workflow.Id): Future[WorkflowInfo] = {
    workflowClient.fetchWorkflowInfo(id)
  }

  def cloneWorkflow(id: Workflow.Id, workflowInfo: WorkflowInfo): Future[Workflow.Id] = {
    logger.info(s"Cloning workflow $id")
    workflowClient.cloneWorkflow(id, WorkflowDescription(
      s"""Scheduled execution of "${workflowInfo.name}"""",
      workflowInfo.description))
  }
}
