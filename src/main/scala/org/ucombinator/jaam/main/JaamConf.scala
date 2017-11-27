package org.ucombinator.jaam.main

import org.rogach.scallop._
import scala.collection.immutable
import reflect.runtime.universe.TypeTag

object JaamConf {
  def extractSeqFromOptString(optString: ScallopOption[String], separator: String = ":"): Seq[String] = {
    optString.toOption.getOrElse("").split(separator).filter(_.nonEmpty)
  }
}

// Rename to JaamConf
trait JaamConf extends ScallopConf {
  appendDefaultToDescription = true
  // TODO: input/output? (as functions like toggle)
  // TODO: better usage printing with trail args (or maybe we have no trail args)
  // TODO: printedName
  // TODO: printHelp
  // TODO: banner/footer

  //================
  // Help
  //================
  val help = opt[Unit](short = 'h', descr = "show this help message")(HelpConverter)

  // TODO: show subcommand help when error is in subcommand
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

  //================
  // Custom option types
  //================

  // --input and --output are so common that we provide the following
  def inputOpt(required: Boolean = true)(implicit tt: TypeTag[List[String]]): ScallopOption[List[String]] =
    opt[List[String]](required = required, default = Some(List()), descr = "input Jaam files")
  def outputOpt()(implicit tt: TypeTag[String]): ScallopOption[String] =
    opt[String](required = true, descr = "the output Jaam file")

  // Change the default prefix to "no-"
  override def toggle(
    name: String = null,
    default: => Option[Boolean] = None,
    short: Char = '\u0000',
    noshort: Boolean = false,
    prefix: String = "no-",
    descrYes: String = "",
    descrNo: String = "",
    hidden: Boolean = false) =
    super.toggle(
      name = name,
      default = default,
      short = short,
      noshort = noshort,
      prefix = prefix,
      descrYes = descrYes,
      descrNo = descrNo,
      hidden = hidden)

  // Options that take an argument from a fixed list
  def enum[A](
    name: String = null,
    short: Char = '\u0000',
    descr: String = "",
    default: Option[String] = None,
    validate: A => Boolean = (_:A) => true,
    required: Boolean = false,
    argName: String = "arg",
    argType: String = "argument",
    hidden: Boolean = false,
    noshort: Boolean = false,
    elems: immutable.ListMap[String, A],
    conv: ValueConverter[A] = null)(
    implicit tt: TypeTag[A]): ScallopOption[A] = {
    val conv2 =
      if (conv != null) { conv } else { enumConverter(argType, elems) }
    opt[A](
      name = name,
      short = short,
      descr = descr + "; one of " + elems.keys.mkString("'", "', '", "'") +
        (default match {
          case None => ""
          case Some(d) => s"; default: '$d'"
        }),
      default = default.map(elems(_)),
      validate = validate,
      required = required,
      argName = argName,
      hidden = hidden,
      noshort = noshort)(
      conv = conv2)
  }

  def enumConverter[A](name: String, elems: Map[String, A])(implicit tt: TypeTag[A]) = {
    def conv(s: String): A =
      elems.getOrElse(s, throw new IllegalArgumentException(s"bad $name `$s` (expected one of: %s)" format elems.keys.mkString(" ")))
  // TODO: allow `handler` to be specified
    val handler: PartialFunction[Throwable, Either[String, Option[A]]] = {
      case e: IllegalArgumentException => Left(e.getMessage)
    }

    singleArgConverter[A](conv, handler)(tt)
  }
}

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
