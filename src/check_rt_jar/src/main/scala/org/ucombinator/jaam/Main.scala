package org.ucombinator.jaam

import java.io.File
import java.util.jar
import java.util.jar.JarFile
import java.util.jar.Attributes.Name

import scala.util.Success
import scala.util.Failure
import scala.util.Try

object Main {
  def main(args: Array[String]) {
    if (args.length != 1) {
      Console.err.println("Accepts one argument: a path to an rt.jar file.")
      sys.exit(1)
    }
    val path = args.head
    Try(new File(path)) match {
      case Success(f) => Try(new JarFile(f)) match {
        case Success(jf : JarFile) => Try(jf.getManifest()) match {
          case Success(manifest : jar.Manifest) => checkManifest(manifest)
          case Failure(_) => sys.exit(4)
        }
        case Failure(_) => sys.exit(3)
      }
      case Failure(_) => sys.exit(2)
    }
  }

  def checkManifest(manifest: jar.Manifest): Unit = {
    if (!hasVersion(manifest)) {
      sys.exit(5)
    }
  }

  def hasVersion(manifest: jar.Manifest): Boolean = {
    val attributes = manifest.getMainAttributes()
    val version = attributes.getValue(Name.SPECIFICATION_VERSION)
    if (version == null) {
      return false
    }
    else {
      println(version.toString())
      return true
    }
  }
}
