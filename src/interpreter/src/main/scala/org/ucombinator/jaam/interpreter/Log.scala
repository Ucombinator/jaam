package org.ucombinator.jaam.interpreter

import com.esotericsoftware.minlog

// TODO: fix SLF4J error

// Logging that does not evaluate its message unless it is actually printed
object Log {
  type Level = Int
  val LEVEL_NONE = minlog.Log.LEVEL_NONE
  val LEVEL_ERROR = minlog.Log.LEVEL_ERROR
  val LEVEL_WARN = minlog.Log.LEVEL_WARN
  val LEVEL_INFO = minlog.Log.LEVEL_INFO
  val LEVEL_DEBUG = minlog.Log.LEVEL_DEBUG
  val LEVEL_TRACE = minlog.Log.LEVEL_TRACE

  // TODO: make these more efficient by making them macros
  def error(message : => String) : Unit = if (minlog.Log.ERROR) minlog.Log.error(null, message, null)
  def warn(message : => String) : Unit = if (minlog.Log.WARN) minlog.Log.warn(null, message, null)
  def info(message : => String) : Unit = if (minlog.Log.INFO) minlog.Log.info(null, message, null)
  def debug(message : => String) : Unit = if (minlog.Log.DEBUG) minlog.Log.debug(null, message, null)
  def trace(message : => String) : Unit = if (minlog.Log.TRACE) minlog.Log.trace(null, message, null)

  def setLogging(level : Level) = minlog.Log.set(level)

  minlog.Log.setLogger(new JaamLogger)

  class JaamLogger extends minlog.Log.Logger {
    private var level = 0

    override def log(level : Int, category : String, message : String, ex : Throwable) {
      this.level = level
      super.log(level, category, message, ex)
    }

    override def print(s : String) = {
      this.level match {
        case minlog.Log.LEVEL_ERROR => super.print(Console.RED + s + Console.RESET)
        case minlog.Log.LEVEL_WARN => super.print(Console.YELLOW + s + Console.RESET)
        case _ => super.print(s)
      }
    }
  }
}
