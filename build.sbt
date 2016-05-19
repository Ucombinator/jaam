organization in ThisBuild := "org.ucombinator"

scalaVersion in ThisBuild := "2.10.6"

resolvers += "Ucombinator maven repository on github" at "https://ucombinator.github.io/maven-repo"
resolvers += Resolver.sonatypeRepo("public")

lazy val check_rt_jar = project in file("src/check_rt_jar")

lazy val analyzer = project in file("src/analyzer")

lazy val visualizer = project in file("src/visualizer")

lazy val messaging = project in file("src/messaging")