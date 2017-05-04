package org.ucombinator.jaam.tools

import java.io.File
import java.lang.reflect.{Method, Modifier}
import java.net.URLClassLoader
import java.util.jar.JarFile

import scala.collection.JavaConversions._
import scala.collection.mutable

class FindMain extends Main("find-main") {
  banner("Attempt to find the Main class from which to run the JAR file")
  footer("")

  val showerrs = opt[Boolean](name = "show-errors", short = 's', descr = "Show errors for unloadable classes")
  val force = opt[Boolean](name = "force-possibilities", short = 'f', descr = "Show all possibilities found manually, even if a main class is found in the manifest")
  val verifymanual = opt[Boolean](name = "validate", short = 'v', descr = "Check potential Main classes for a valid `main` method")
  val anyClass = opt[Boolean](descr = "Check all classes not just those named Main")

  val jars = trailArg[String](descr = "Colon-separated list of JAR files to directly search for `main` methods")

  def run(conf: Conf) {
    FindMain.main(jars().split(":"), showerrs(), force(), verifymanual(), anyClass())
  }
}

object FindMain {
  val classEnding = ".class"

  def checkManifest(jarFile: JarFile): Option[String] = {
    Option(jarFile.getManifest) match {
      case Some(manifest) =>
        Option(manifest.getMainAttributes.getValue("Main-Class"))
      case None => None
    }
  }

  def matchesMainSignature(method: Method): Boolean = {
    // We're looking for methods which match the signature:
    //   public static void main(String[] args)
    if (
      // Check "main":
      method.getName.endsWith("main") &&
      // Check PUBLIC STATIC:
      ((method.getModifiers & (Modifier.PUBLIC | Modifier.STATIC)) != 0) &&
      // Check VOID:
      method.getReturnType.equals(Void.TYPE) &&
      // Check (String[] args):
      ((method.getParameterCount == 1) && (method.getParameterTypes.head == Array.empty[String].getClass))
    ) { true }
    else { false }
  }

  def checkJarFile(showErrors: Boolean, validate: Boolean, jarFile: JarFile, jarFileName: String, anyClass: Boolean): Array[String] = {
    val possibilities = mutable.MutableList.empty[String]
    // The ClassLoader requires a URL.
    val jarFileURL = new File(jarFileName).toURI.toURL
    val classLoader = new URLClassLoader(Array(jarFileURL))

    for (jarClass <- jarFile.entries().filter(_.getName.endsWith(classEnding))) {
      val className = jarClass.getName
      val binaryClassName = className.dropRight(classEnding.length).replaceAll(File.separator, ".")
      if (anyClass || binaryClassName.endsWith(".Main")) {
        var valid = false
        if (validate) {
          // Check for an enclosed `main` method which matches regular main method definitions.
          try {
            val loadedClass = classLoader.loadClass(binaryClassName)
            loadedClass.getDeclaredMethods.foreach(
              method => if (matchesMainSignature(method)) { valid = true }
            )
          }catch {
            case n: NoClassDefFoundError =>
              println("Could not search potential matching class: " + binaryClassName)
              if (showErrors) {
                n.printStackTrace()
              }
            case e: Throwable => e.printStackTrace()
          }
        } else {
          // Don't do validation.
          valid = true
        }
        if (valid) {
          possibilities += binaryClassName
        }
      }
    }
    possibilities.toArray
  }

  def main(jarFileNames: Seq[String], showErrors: Boolean, forcePossibilities: Boolean, validate: Boolean, anyClass: Boolean): Unit = {
    var foundSomething = false
    val verifyJarFiles = false

    for (jarFileName <- jarFileNames) {
      println("Searching file for `Main` class: " + jarFileName)
      val jarFile = new JarFile(jarFileName, verifyJarFiles)
      var printPossibilities = forcePossibilities
      checkManifest(jarFile) match {
        case Some(value) =>
          foundSomething = true
          println("Main class from manifest: " + value)
        case None =>
          println("No Main-Class attribute in manifest.")
          printPossibilities = true
      }
      if (printPossibilities) {
        println("Checking possibilities manually...")
        val possibilities = checkJarFile(showErrors, validate, jarFile, jarFileName, anyClass)
        if (possibilities.nonEmpty) {
          foundSomething = true
        }
        possibilities.foreach(m => println("Possibility: " + m))
      }
    }

    if (!foundSomething) {
      // Nothing was found, so show an error.
      sys.exit(1)
    }
  }
}
