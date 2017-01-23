package org.ucombinator.jaam.tools

import java.io.File
import java.lang.NoClassDefFoundError
import java.lang.reflect.{Method, Modifier}
import java.net.URLClassLoader
import java.util.jar.JarFile

import scala.collection.JavaConversions._
import scala.collection.mutable

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

  def checkJarFile(jarFile: JarFile, jarFileName: String): Array[Method] = {
    val possibilities = mutable.MutableList.empty[Method]
    // The ClassLoader requires a URL.
    val jarFileURL = new File(jarFileName).toURI.toURL
    val classLoader = new URLClassLoader(Array(jarFileURL))

    for (jarEntry <- jarFile.entries().filter(_.getName.endsWith(classEnding))) {
      val fileName = jarEntry.getName
      val binaryName = fileName.dropRight(classEnding.length).replaceAll(File.separator, ".")
      try {
        val loadedClass = classLoader.loadClass(binaryName)
        loadedClass.getDeclaredMethods.foreach(
          method => if (matchesMainSignature(method)) { possibilities += method }
        )
      }catch {
        case n: NoClassDefFoundError => println("Could not search referenced class: " + binaryName)
        case e: Throwable => e.printStackTrace()
      }
    }
    possibilities.toArray
  }

  def main(jarFileName: String): Unit = {
    val verify = false
    val jarFile = new JarFile(jarFileName, verify)

    checkManifest(jarFile) match {
      case Some(value) => println("Main class from manifest: " + value)
      case None =>
        println("No Main-Class attribute in manifest. Checking possibilities through reflection.")
        checkJarFile(jarFile, jarFileName).foreach(m => println("Possibility: " + m))
    }
  }
}
