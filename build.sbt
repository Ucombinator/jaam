organization in ThisBuild := "org.ucombinator"

scalaVersion in ThisBuild := "2.10.6"

version := "0.1-SNAPSHOT"

resolvers += "Ucombinator maven repository on github" at "https://ucombinator.github.io/maven-repo"
resolvers += Resolver.sonatypeRepo("public")

lazy val serializer = (project in file("src/serializer")).disablePlugins(sbtassembly.AssemblyPlugin)

lazy val interpreter = (project in file("src/interpreter")).dependsOn(serializer)

lazy val visualizer = (project in file("src/visualizer")).dependsOn((serializer))
