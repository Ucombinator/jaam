name := "jaam-visualizer"
assemblyOutputPath in assembly := new File("./jars/jaam-visualizer.jar")

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "20.0"
)
