package org.ucombinator.jaam.tools

case class Config(
                   mode : String = null,
                   targetFile : String = null,
                   targetState : Integer = null,
                   fixEOF : Boolean = false,
                   addMissingStates : Boolean = false,
                   removeMissingStates : Boolean = false,
                   sourceFiles : Seq[String] = Seq()
                 )

object Main {
  def main(args : Array[String]) = {
    val parser = new scopt.OptionParser[Config]("jaam-tools") {
      override def showUsageOnError = true

      help("help") text("prints this usage text")

      note("")
      cmd("print") action { (_, c) =>
        c.copy(mode = "print")
      } text("Print a JAAM file in human-readable format") children(
        opt[Int]("state") action { (x, c) => c.copy(targetState = x) }
          text("a specific state ID to print"),
        arg[String]("<file>") action { (x, c) => c.copy(targetFile = x) }
          text("a .jaam file to be printed")
      )

      note("")
      cmd("validate") action { (_, c) =>
        c.copy(mode = "validate")
      } text("Amend an aborted JAAM serialization to allow reading.") children(
        opt[Unit]("fixEOF") action { (_, c) => c.copy(fixEOF = true) }
          text("whether to amend a JAAM file which ends abruptly"),
        opt[Unit]("addMissingStates") action { (_, c) => c.copy(addMissingStates =  true) }
          text("find hanging edges and add MissingState states so they go somewhere"),
        opt[Unit]("removeMissingStates") action { (_, c) => c.copy(removeMissingStates = true) }
          text("remove any MissingState states found in the serialization; overrides --addMissingStates"),
        arg[String]("<file>") action { (x, c) => c.copy(targetFile = x) }
          text("a .jaam file to be truncated")
      )

      note("")
      cmd("info") action { (_, c) =>
        c.copy(mode = "info")
      } text("Get simple information about a JAAM interpretation.") children(
        arg[String]("<file>") action { (x, c) => c.copy(targetFile = x) }
          text("a .jaam file to be analyzed")
      )

      note("")
      cmd("cat") action { (_, c) =>
        c.copy(mode = "cat")
      } text("Combine multile JAAM files into a single, cohesive file.") children(
        arg[String]("<file>").required() action { (x, c) => c.copy(targetFile = x)
        } text("The desired output filename"),
        arg[Seq[String]]("<file>[,<file>,...]").required() action { (x, c) =>
          c.copy(sourceFiles = x)
        } text("The list of files to be concatenated.")
      )
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Bad arguments")

      case Some(config) =>
        config.mode match {
          case "print" => Option(config.targetState) match {
            case None => Print.printFile(config.targetFile)
            case Some(state) => Print.printStateFromFile(config.targetFile, state)
          }
          case "validate" => Validate.validateFile(
            jaamFile = config.targetFile,
            shouldAppendMissingEOF = config.fixEOF,
            addMissingStates = config.addMissingStates,
            removeMissingStates = config.removeMissingStates)
          case "info" => Info.analyzeForInfo(config.targetFile)
          case "cat" => Cat.concatenateFiles(config.sourceFiles, config.targetFile)
          case _ => println("Invalid command given: " + config.mode)
        }
    }
  }
}
