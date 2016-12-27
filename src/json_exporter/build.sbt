name := "jaam-json_exporter"
assemblyOutputPath in assembly := new File("./jars/jaam-json_exporter.jar")

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "org.slf4j" % "slf4j-nop" % "1.7.22"
)
