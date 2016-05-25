name := "jaam-analyzer"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "org.ucombinator.jgraphx" % "jgraphx" % "3.2.0.0",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scala-lang" % "scala-reflect" % "2.10.6"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

mainClass in (Compile, assembly) := Some("org.ucombinator.jaam.Main")

// Assembly-specific configuration
test in assembly := {}
assemblyOutputPath in assembly := new File("./assembled/jaam-analyzer.jar")

// META-INF discarding
assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
{
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
}
