name := "puppet-to-diagram"

version := "0.1"

scalaVersion := "2.12.9"

resolvers += Resolver.sonatypeRepo("snapshots")

val circeVersion = "0.12.0-RC3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-yaml" % "0.10.0",
  "org.specs2" %% "specs2-core" % "4.6.0" % "test",
  "guru.nidi" % "graphviz-java" % "0.10.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
)

scalacOptions in Test ++= Seq("-Yrangepos")