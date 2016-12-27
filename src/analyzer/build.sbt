name := "jaam-analyzer"
assemblyOutputPath in assembly := new File("./jars/jaam-analyzer.jar")

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "2.0.1",
  "org.ucombinator.soot" % "soot" % "nightly.20161021"
)
