package org.ucombinator.jaam.util

import com.esotericsoftware.minlog

// Logging that does not evaluate its message unless it is actually printed
object Log {
  sealed abstract class Level {
    val level: Int
    val name: String

    override def toString: String = name
  }

  object LEVEL_NONE extends Level {
    override val level = minlog.Log.LEVEL_NONE
    override val name = "none"
  }
  object LEVEL_ERROR extends Level {
    override val level = minlog.Log.LEVEL_ERROR
    override val name = "error"
  }
  object LEVEL_WARN extends Level {
    override val level = minlog.Log.LEVEL_WARN
    override val name = "warn"
  }
  object LEVEL_INFO extends Level {
    override val level = minlog.Log.LEVEL_INFO
    override val name = "info"
  }
  object LEVEL_DEBUG extends Level {
    override val level = minlog.Log.LEVEL_DEBUG
    override val name = "debug"
  }
  object LEVEL_TRACE extends Level {
    override val level = minlog.Log.LEVEL_TRACE
    override val name = "trace"
  }

  val levels = List(
    LEVEL_NONE,
    LEVEL_ERROR,
    LEVEL_WARN,
    LEVEL_INFO,
    LEVEL_DEBUG,
    LEVEL_TRACE)

  // TODO: make these more efficient by making them macros
  def error(message: => String) { if (minlog.Log.ERROR) minlog.Log.error(null, message, null) }
  def warn(message: => String)  { if (minlog.Log.WARN) minlog.Log.warn(null, message, null)   }
  def info(message: => String)  { if (minlog.Log.INFO) minlog.Log.info(null, message, null)   }
  def debug(message: => String) { if (minlog.Log.DEBUG) minlog.Log.debug(null, message, null) }
  def trace(message: => String) { if (minlog.Log.TRACE) minlog.Log.trace(null, message, null) }

  def setLogging(level: Level): Unit = minlog.Log.set(level.level)

  var color: Boolean = true

  minlog.Log.setLogger(JaamLogger)

  private object JaamLogger extends minlog.Log.Logger {
    private var level = 0

    override def log(level : Int, category : String, message : String, ex : Throwable) {
      this.level = level
      super.log(level, category, message, ex)
    }

    override def print(s : String): Unit = {
      def ifColor(s: String): String =
        if (color) s
        else ""

      this.level match {
        case minlog.Log.LEVEL_ERROR =>
          super.print(ifColor(Console.RED)  + s + ifColor(Console.RESET))
        case minlog.Log.LEVEL_WARN =>
          super.print(ifColor(Console.YELLOW) + s + ifColor(Console.RESET))
        case _ => super.print(s)
      }
    }
  }
}
