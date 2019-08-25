name := "puppet-to-diagram"

version := "0.1"

scalaVersion := "2.13.0"

val circeVersion = "0.12.0-RC3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "org.specs2" %% "specs2-core" % "4.6.0" % "test",
)

scalacOptions in Test ++= Seq("-Yrangepos")