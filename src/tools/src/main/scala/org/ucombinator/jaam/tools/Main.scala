package org.ucombinator.jaam.tools

case class Config(
                 mode : String = null,
                 target : String = null
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
        arg[String]("<file>") action { (x, c) => c.copy(target = x) }
          text("a .jaam file to be printed")
      )

      note("")
      cmd("truncate") action { (_, c) =>
        c.copy(mode = "truncate")
      } text("Amend an aborted JAAM serialization to allow reading.")

      note("")
      cmd("info") action { (_, c) =>
        c.copy(mode = "info")
      } text("Get simple information about a JAAM interpretation.")
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Bad arguments")

      case Some(config) =>
        config.mode match {
          case "print" => Print.printFile(config.target)
          case "truncate" => println("truncate")
          case "info" => println("info")
          case _ => println("Invalid command given: " + config.mode)
        }
    }
  }
}
