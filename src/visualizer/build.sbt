name := "jaam-visualizer"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205"
)

// Assembly-specific configuration
test in assembly := {}
assemblyOutputPath in assembly := new File("./assembled/visualizer.jar")

// META-INF discarding
assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
{
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
}
