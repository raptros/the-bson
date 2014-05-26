organization := "io.github.raptros"

name := "the-bson"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.mongodb" %% "casbah" % "2.7.0",
  "org.scalaz" %% "scalaz-core" % "7.0.6"
)

(sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen
