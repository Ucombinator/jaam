package org.ucombinator.jaam.tools

case class Config(
                   mode : String = null,
                   targetFile : String = null,
                   targetState : Integer = null
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
      cmd("truncate") action { (_, c) =>
        c.copy(mode = "truncate")
      } text("Amend an aborted JAAM serialization to allow reading.") children(
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
          case "truncate" => Truncate.truncateFile(config.targetFile)
          case "info" => Info.analyzeForInfo(config.targetFile)
          case _ => println("Invalid command given: " + config.mode)
        }
    }
  }
}
