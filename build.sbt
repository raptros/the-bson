import SonatypeKeys._
import com.typesafe.sbt.pgp.PgpKeys._
import sbtrelease._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._

lazy val siteManaged = settingKey[File]("where to put the thing")
lazy val mkSiteManaged = taskKey[Unit]("do the thing")

lazy val baseDeps = Seq(
  "org.mongodb" % "mongo-java-driver" % "2.12.2",
  "org.joda" % "joda-convert" % "1.2",
  "joda-time" % "joda-time" % "2.3",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test"
)

lazy val buildSettings = Defaults.defaultSettings ++ releaseSettings ++ Seq(
  scalaVersion := "2.11.1",
  crossScalaVersions := Seq(scalaVersion.value),
  startYear := Some(2014),
  organization := "io.github.raptros",
  resolvers += Resolver.sonatypeRepo("releases"),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  libraryDependencies ++= baseDeps,
  licenses += "BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause"),
  homepage := Some(url("http://raptros.github.io/the-bson/")),
  pomExtra :=
    <scm>
      <connection>scm:git:git@github.com:raptros/the-bson.git</connection>
      <developerConnection>scm:git:git@github.com:raptros/the-bson.git</developerConnection>
      <url>git@github.com:raptros/the-bson.git</url>
    </scm>
      <developers>
        <developer>
          <id>raptros</id>
          <name>Aidan Coyne</name>
          <url>http://github.com/raptros</url>
        </developer>
      </developers>,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val noPublish = buildSettings ++ Seq(
  publishArtifact in Compile := false
)

lazy val core = project settings (noPublish: _*) settings (
  name := "the-bson-core",
  description := "typeclass-based utility for encoding and decoding mongo db objects in Scala",
  (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen
)

lazy val macros = project settings (noPublish: _*) settings (
  name := "the-bson-macros",
  description := "macros for generating encoder and decoder instances for case classes",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
) dependsOn core

lazy val root = (project in file(".")).
  settings(buildSettings: _*).
  settings(sonatypeSettings: _*).
  settings(unidocSettings: _*).
  settings(site.settings: _*).
  settings(ghpages.settings: _*).
  aggregate(core, macros)

name := "the-bson"

description := "typeclass-based utility for encoding and decoding mongo db objects in Scala"

siteManaged := file("target") / "site-managed"

mappings in (Compile, packageBin) ++= mappings.in(core, Compile, packageBin).value

mappings in (Compile, packageSrc) ++= mappings.in(core, Compile, packageSrc).value

mappings in (Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value

mappings in (Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value

mkSiteManaged := {
  val target = siteManaged.value / "index.md"
  IO.writeLines(target, "---" :: "layout: index" :: "---" :: Nil)
  val readme = IO.readLines(file("README.md"))
  IO.writeLines(target, readme, append = true)
}

SiteKeys.makeSite <<= SiteKeys.makeSite dependsOn mkSiteManaged

GhPagesKeys.ghpagesNoJekyll := false

site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "docs/latest/api")

site.pamfletSupport("docs")

SiteKeys.siteMappings += (siteManaged.value / "index.md" -> "index.md")

(includeFilter in SiteKeys.makeSite) := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"

git.remoteRepo := "git@github.com:raptros/the-bson.git"

GhPagesKeys.synchLocal <<= GhPagesKeys.synchLocal dependsOn SiteKeys.makeSite


releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseTask(publishSigned),
  releaseTask(sonatypeReleaseAll),
  setNextVersion,
  commitNextVersion,
  pushChanges
)



