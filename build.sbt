name := "analyzer"

organization := "org.ucombinator"

version := "0-SNAPSHOT"

scalaVersion := "2.10.6"

resolvers += "Ucombinator maven repository on github" at "https://ucombinator.github.io/maven-repo"
resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "org.ucombinator.jgraphx" % "jgraphx" % "3.2.0.0",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scala-lang" % "scala-reflect" % "2.10.6"
)

mainClass in Compile := Some("org.ucombinator.analyzer.Main")
