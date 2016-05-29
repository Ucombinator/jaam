name := "jaam-interpreter"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./jars/jaam-interpreter.jar")

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot-all-in-one" % "nightly.20150205",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.esotericsoftware" % "minlog" % "1.3.0"
)
