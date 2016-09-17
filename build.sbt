import io.gatling.sbt.GatlingPlugin
import sbt.Keys._

val akkaVersion = "2.4.10"

lazy val root = (project in file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    name := "reactive-query-scheduler",

    version := "1.0",

    scalaVersion := "2.11.8",

    libraryDependencies ++= Seq(
      "io.spray" %% "spray-json" % "1.3.2",

      "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,

      "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
      "ch.qos.logback" % "logback-classic" % "1.1.7" % Runtime,

      "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % Test,
      "org.scalatest" %% "scalatest" % "2.2.4" % Test,

      "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2",
      "io.gatling" % "gatling-test-framework" % "2.2.2"
    ),

    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-deprecation",
      "-encoding", "UTF-8", // yes, this is 2 args
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_", // Causes Gatling string interpolation to crash build
      "-Yno-adapted-args",
      //"-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture" //,
      //"-Ywarn-unused-import"     // 2.11 only, seems to cause issues with generated sources?
    )
  )
