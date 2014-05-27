organization := "io.github.raptros"

name := "the-bson"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % "2.12.2",
  "org.joda" % "joda-convert" % "1.2",
  "joda-time" % "joda-time" % "2.3",
  "org.scalaz" %% "scalaz-core" % "7.0.6"
)

(sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen
