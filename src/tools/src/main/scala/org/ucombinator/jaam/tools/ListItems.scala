package org.ucombinator.jaam.tools

import java.io.File
import java.lang.reflect.Executable
import java.net.URLClassLoader
import java.util.jar.JarFile

import scala.collection.JavaConversions._
import scala.collection.mutable

case class ListPrintOption(classes: Boolean, methods: Boolean)

object ListItems {
  private val classEnding = ".class"

  def getString(executable: Executable): String = {
    "<" + executable.getClass.getSimpleName + "> " + executable.toString
  }

  def main(jarFileName: String, printOption: ListPrintOption): Unit = {
    val jarFileURL = new File(jarFileName).toURI.toURL
    val classLoader = new URLClassLoader(Array(jarFileURL))

    val verify = false
    val jarFile = new JarFile(jarFileName, verify)

    // Iterate through the JarEntries to find things.
    for (jarEntry <- jarFile.entries().filter(_.getName.endsWith(classEnding))) {
      val fileName = jarEntry.getName
      val binaryName = fileName.dropRight(classEnding.length).replaceAll(File.separator, ".")
      if (printOption.classes) {
        println("<Class> " + binaryName)
      }
      // Load the class and print the methods and constructors inside of it.
      if (printOption.methods) {
        try {
          val loadedClass = classLoader.loadClass(binaryName)
          val items = mutable.MutableList.empty[Executable]
          loadedClass.getDeclaredConstructors.foreach(items += _)
          loadedClass.getDeclaredMethods.foreach(items += _)
          val front = if (printOption.classes) "  " else ""
          items.foreach(i => println(front + getString(i)))
        } catch {
          case e: Throwable => e.printStackTrace()
        }
      }
    }
  }
}
