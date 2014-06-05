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
  organization := "io.github.raptros",
  resolvers += Resolver.sonatypeRepo("releases"),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
  libraryDependencies ++= baseDeps
)

lazy val noPublish = buildSettings ++ Seq(
  publish := {},
  publishLocal := {}
)

lazy val core = project settings (noPublish: _*) settings (
  name := "the-bson-core",
  (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen
)

lazy val macros = project settings (noPublish: _*) settings (
  name := "the-bson-macros",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
) dependsOn core

lazy val root = (project in file(".")).
  settings(buildSettings: _*).
  settings(unidocSettings: _*).
  settings(site.settings: _*).
  settings(ghpages.settings: _*).
  settings(LaikaPlugin.defaults: _*).
  aggregate(core, macros)

name := "the-bson"

siteManaged := file("target") / "site-managed"

mappings in (Compile, packageBin) ++= mappings.in(core, Compile, packageBin).value

mappings in (Compile, packageSrc) ++= mappings.in(core, Compile, packageSrc).value

mappings in (Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value

mappings in (Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value

sourceDirectories in LaikaKeys.Laika += siteManaged.value

mkSiteManaged := {
  IO.copyFile(file("README.md"), siteManaged.value / "index.md")
}

LaikaKeys.site in LaikaKeys.Laika <<= (LaikaKeys.site in LaikaKeys.Laika) dependsOn mkSiteManaged

site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api")

site.addMappingsToSiteDir(mappings in (LaikaKeys.Laika, LaikaKeys.site), ".")

git.remoteRepo := "git@github.com:raptros/the-bson.git"

