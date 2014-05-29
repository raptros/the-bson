import sbt._
import Keys._

object Common extends Build {
  lazy val main = Project("main", file("."))

  lazy val macroSub = Project("macros", file("macros")) settings(
    scalaVersion := "2.11.1",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"
    ) dependsOn main
}
