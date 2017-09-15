// Copyright (c) 2015, CodiLime Inc.
//
// Owner: Jacek Laskowski

organization := "io.deepsense"
name         := "backend"
version      := "0.1.0"
scalaVersion := "2.11.6"

lazy val deeplang          = project
lazy val experimentmanager = project dependsOn deeplang
lazy val graph             = project dependsOn deeplang
