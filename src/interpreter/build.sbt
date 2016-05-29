name := "jaam-interpreter"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./assembled/jaam-interpreter.jar")

// Fixes "Cannot check match for unreachability" warning
// TODO: not sure if this is the best way to do this
initialize ~= { _ => sys.props("scalac.patmat.analysisBudget") = "1024" }

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.esotericsoftware" % "minlog" % "1.3.0"
)
