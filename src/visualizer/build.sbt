name := "jaam-visualizer"

version := "0.1-SNAPSHOT"

// Assembly-specific configuration
test in assembly := {}
assemblyOutputPath in assembly := new File("./visualizer.jar")
