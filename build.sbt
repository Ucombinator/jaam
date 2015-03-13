name := "analyzer"

organization := "org.ucombinator"

version := "0-SNAPSHOT"

resolvers += "Ucombinator maven repository on github" at "https://ucombinator.github.io/maven-repo"

libraryDependencies ++= Seq(
        "org.ucombinator" % "soot-wrapper" % "0.1.1",
        "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
        "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
)

mainClass in Compile := Some("org.ucombinator.analyzer.Main")

