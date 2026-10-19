ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.arjun"

lazy val root = (project in file("."))
  .settings(
    name := "logscope"
  )