ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.arjun"

lazy val root = (project in file("."))
  .settings(
    name := "logscope",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-sql"   % "3.5.9",
      "org.apache.spark" %% "spark-mllib" % "3.5.9",
      "org.scalatest"    %% "scalatest"   % "3.2.19" % Test
    ),
    fork := true,
    javaOptions ++= Seq(
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens=java.base/java.io=ALL-UNNAMED",
      "--add-opens=java.base/java.net=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
      "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
      "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
      "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
      "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED"
    ),

    // --- Fat-jar packaging (sbt-assembly) ---
    assembly / mainClass       := Some("Main"),
    assembly / assemblyJarName := "logscope.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", _*)  => MergeStrategy.concat
      case PathList("META-INF", _*)              => MergeStrategy.discard
      case "module-info.class"                   => MergeStrategy.discard
      case x if x.endsWith("/module-info.class") => MergeStrategy.discard
      case "reference.conf"                      => MergeStrategy.concat
      case _                                     => MergeStrategy.first
    },
    assembly / packageOptions += Package.ManifestAttributes(
      "Add-Opens" -> ("java.base/java.lang java.base/java.lang.invoke java.base/java.lang.reflect " +
        "java.base/java.io java.base/java.net java.base/java.nio java.base/java.util " +
        "java.base/java.util.concurrent java.base/java.util.concurrent.atomic " +
        "java.base/sun.nio.ch java.base/sun.nio.cs java.base/sun.security.action " +
        "java.base/sun.util.calendar java.security.jgss/sun.security.krb5")
    )
  )