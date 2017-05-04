package org.ucombinator.jaam.interpreter

import java.util.zip.ZipInputStream
import java.io.{FileInputStream, File}

object Utilities {
  def getAllClasses(classpath: String): Set[String] = {
    Log.info(s"Getting all class names from ${classpath}")
    def fileToClassName(fn: String): String = {
      val fqn = fn.replace('/', '.')
      fqn.substring(0, fqn.length - ".class".length)
    }
    def getDirClasses(d: File): Set[String] = {
      if (d.exists) {
        if (d.isDirectory) {
          d.listFiles.toSet flatMap getDirClasses
        } else {
          if (d.getName.endsWith(".class")) {
            Set(fileToClassName(d.getName))
          } else Set.empty
        }
      } else Set.empty
    }
    def getJarClasses(j: File): Set[String] = {
      val zip = new ZipInputStream(new FileInputStream(j))
      var result: Set[String] = Set.empty[String]
      var entry = zip.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.getName.endsWith(".class")) {
          val className = fileToClassName(entry.getName)
          result = result + className
        }
        entry = zip.getNextEntry
      }
      result
    }
    classpath.split(":").toSet flatMap { (path: String) =>
      val f = new File(path)
      if (path.endsWith(".jar")) {
        getJarClasses(f)
      } else {
        getDirClasses(f)
      }
    }
  }
}
