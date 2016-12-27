package org.ucombinator.jaam.interpreter
// TODO: move to `common` project
// TODO: combine with tools/Command (maybe make this a trait?)

import org.rogach.scallop._

/**
  * A manual converter to handle calls to `--help`. This implementation allows
  * the `--help` option to be given at any point in the options -- not only at
  * the very beginning of the list of arguments.
  */
object HelpConverter extends ValueConverter[Unit] {
  // Override the default `parse` method so that any instance of `--help` is
  // handled appropriately, i.e. a `Help` is thrown.
  override def parse(s: List[(String, List[String])]): Either[String, Option[Unit]] = s match {
    case Nil  => Right(None)
    case _    => throw exceptions.Help("")
  }
  val tag = scala.reflect.runtime.universe.typeTag[Unit]
  val argType = org.rogach.scallop.ArgType.FLAG
}

class JaamConf(args : Seq[String]) extends ScallopConf(args = args) {
  val help        = opt[Unit](short = 'h', descr = "show this help message")(HelpConverter)

  /**
    * Override the built-in `onError` method to ensure that `--help` information
    * is displayed every time there is an error during option parsing.
    *
    * @param e the error which was thrown during parsing
    */
  override def onError(e: Throwable) = {
    e match {
      case exceptions.ScallopException(_) => printHelp()
      case _ => ()
    }
    // After printing the help information (if needed), allow the call to
    // continue as it would have.
    super.onError(e)
  }
}
