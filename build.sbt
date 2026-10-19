ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.arjun"

lazy val root = (project in file("."))
  .settings(
    name := "logscope",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.9"
  )