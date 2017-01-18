name := "jaam-agent"
assemblyOutputPath in assembly := new File("./jars/jaam-agent.jar")

// Set the code to run for the `-javaagent:` argument
packageOptions in (Compile, packageBin) +=
  Package.ManifestAttributes(
    new java.util.jar.Attributes.Name("Premain-Class")
      -> "org.ucombinator.jaam.agent.Main")

assemblyOption in assembly :=
  (assemblyOption in assembly).value
  .copy(includeScala = false, includeDependency = false)

// TODO: this option doesn't succeed at silencing the warning
// We are mucking around with internals we expect and can safely ignore the warning:
//   <class> is internal proprietary API and may be removed in a future release
javacOptions in compile += "-XDignore.symbol.file"
