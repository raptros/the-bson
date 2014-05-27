import sbt._
import Keys._

object Common extends Build {
  lazy val main = Project("main", file("."))

  lazy val macroSub = Project("macros", file("macros")) settings(
    scalaVersion := "2.11.1",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value//,
//      "org.scalamacros" %% "quasiquotes" % "2.0.0"
    )//,
//    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)
    ) dependsOn main
}