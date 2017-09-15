/**
 * Copyright (c) 2016, CodiLime Inc.
 */

package io.deepsense.sessionmanager.service.sessionspawner.sparklauncher.clusters

import scalaz.Scalaz._
import scalaz._

import org.apache.spark.launcher.SparkLauncher

import io.deepsense.commons.models.ClusterDetails
import io.deepsense.sessionmanager.service.sessionspawner._
import io.deepsense.sessionmanager.service.sessionspawner.sparklauncher.{SparkLauncherConfig, SparkLauncherError, UnexpectedException}

object SeahorseSparkLauncher {

  def apply(
      sessionConfig: SessionConfig,
      sparkLauncherConfig: SparkLauncherConfig,
      clusterConfig: ClusterDetails) = handleUnexpectedExceptions {
      for {
        sparkLauncher <- clusterConfig.clusterType match {
          case "local" =>
            LocalSparkLauncher(sessionConfig, sparkLauncherConfig, clusterConfig)
          case "standalone" =>
            StandaloneSparkLauncher(sessionConfig, sparkLauncherConfig, clusterConfig)
          case "yarn" =>
            YarnSparkLauncher(sessionConfig, sparkLauncherConfig, clusterConfig)
          case "mesos" =>
            MesosSparkLauncher(sessionConfig, sparkLauncherConfig, clusterConfig)
        }
      } yield sparkLauncher
        .setConfOpt("spark.executor.memory", clusterConfig.executorMemory)
        .setConfOpt("spark.executor.cores", clusterConfig.executorCores.map(_.toString))
        .setConfOpt("spark.cores.max", clusterConfig.totalExecutorCores.map(_.toString))
        .setConfOpt("spark.executor.instances", clusterConfig.numExecutors.map(_.toString))
        .setConf("spark.driver.extraJavaOptions", "-Dfile.encoding=UTF8")
  }

  private def handleUnexpectedExceptions[T, E <: SparkLauncherError](code: => Validation[E, T]) =
    try {
      code
    } catch {
      case ex: Exception => UnexpectedException(ex).failure
    }

  implicit class RichSparkLauncher(self: SparkLauncher) {
    def setConfOpt(key: String, valueOpt: Option[String]): SparkLauncher = {
      valueOpt match {
        case Some(value) => self.setConf(key, value)
        case None => self
      }
    }

    def setSparkArgs(args: Map[String, String]): SparkLauncher =
      args.filterKeys(key =>
        !disabledKeys.contains(key)
      ).foldLeft(self) { (sparkLauncher, keyValue) =>
        val (key, value) = keyValue
        val isNoValueArgument = value == null
        if (isNoValueArgument) {
          sparkLauncher.addSparkArg(key)
        } else {
          sparkLauncher.addSparkArg(key, value)
        }
      }

    val disabledKeys = Set(
      "--class",
      "--deploy-mode",
      "--driver-class-path",
      "--driver-java-options",
      "--master",
      "--name",
      "--verbose"
    )
  }

}

