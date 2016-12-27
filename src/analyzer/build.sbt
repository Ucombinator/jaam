name := "jaam-analyzer"
assemblyOutputPath in assembly := new File("./jars/jaam-analyzer.jar")

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "2.0.1",
  "org.slf4j" % "slf4j-nop" % "1.7.22",
  "org.ucombinator.soot" % "soot" % "nightly.20161021"
)
