name := "jaam-analyzer"
version := "0.1-SNAPSHOT"
assemblyOutputPath in assembly := new File("./jars/jaam-analyzer.jar")

libraryDependencies ++= Seq(
  "org.ucombinator.soot" % "soot" % "nightly.20161021",
  "org.rogach" %% "scallop" % "2.0.1"
)
