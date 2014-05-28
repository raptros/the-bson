organization := "io.github.raptros"

name := "the-bson"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % "2.12.2",
  "org.joda" % "joda-convert" % "1.2",
  "joda-time" % "joda-time" % "2.3",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test"
)

(sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen

resolvers += Resolver.sonatypeRepo("releases")

// include the macro classes and resources in the main jar
mappings in (Compile, packageBin) ++= mappings.in(macroSub, Compile, packageBin).value

// include the macro sources in the main source jar
mappings in (Compile, packageSrc) ++= mappings.in(macroSub, Compile, packageSrc).value