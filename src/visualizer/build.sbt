name := "jaam-visualizer"
version := "0.1-SNAPSHOT"
mainClass in assembly := Some("org.ucombinator.jaam.visualizer.main.Main")
assemblyOutputPath in assembly := new File("./jars/jaam-visualizer.jar")

libraryDependencies ++= Seq()
