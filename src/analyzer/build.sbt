name := "jaam-analyzer"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scala-lang" % "scala-reflect" % "2.10.6",
  "com.esotericsoftware" % "minlog" % "1.3.0"
)

scalacOptions ++= Seq(
  // Emit warning and location for usages of deprecated APIs.
  "-deprecation",
  // Explain type errors in more detail.
  "–explaintypes",
  // Emit warning and location for usages of features that should be imported explicitly.
  "-feature",
  // Generates faster bytecode by applying optimisations to the program
  "-optimise",
  // Enable additional warnings where generated code depends on assumptions.
  "-unchecked"
)

// Fixes "Cannot check match for unreachability" warning
// TODO: not sure if this is the best way to do this
initialize ~= { _ => sys.props("scalac.patmat.analysisBudget") = "1024" }

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
