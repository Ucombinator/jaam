package org.ucombinator.jaam.tools

import java.io.File
import java.lang.reflect.Method
import java.net.{URL, URLClassLoader}
import java.util.jar.JarFile

import org.ucombinator.jaam.serializer._

import scala.collection.JavaConversions._
import scala.collection.mutable

//sealed trait MethodSet
//case class EmptyMethodSet() extends MethodSet
//case class SomeMethodsInitializedByJAR(methods: Set[Method]) extends MethodSet
//case class SomeMethodsInitializedByJAAM(methods: Set[Method]) extends MethodSet

object Coverage {
  private val methodMap = mutable.Map[Method, Set[Stmt]]()

  def findCoverage(jaamFile: String, jarFileNames: Seq[String]) = {
    initializeHashFromJarFiles(jarFileNames)
    loadStmtsFromJaamFile(jaamFile)
    // Iterate over statements in jaam file.
    //   For each statement, add statement to method in hash.
    // Print results.
  }

  def initializeHashFromJarFiles(jarFileNames: Seq[String]) = {
    // Iterate over the jarfile names.
    for (jarFileName <- jarFileNames) {
      println(jarFileName)
      // Initialize class loader.  Unfortunately, this only works if you know
      // the names of the classes to load... which is unhelpful to us.
      val classLoader = new URLClassLoader(Array[URL](new File(jarFileName).toURI.toURL))
      // So open the JAR file and find things named "*.class".
      val jarFile = new JarFile(jarFileName, false)
      for (jarEntry <- jarFile.entries().filter(_.getName.endsWith(".class"))) {
        // The class loader requires the names to be in "binary name" format,
        // e.g. "com.example.MyGreatClass".
        val fileName = jarEntry.getName
        val binaryName = fileName.dropRight(".class".length).replaceAll(File.separator, ".")
        println("    " + binaryName)
        try {
          val loadedClass = classLoader.loadClass(binaryName)
          loadedClass.getMethods.foreach(m => methodMap(m) = Set[Stmt]())
        } catch {
          // Loading of classes does not always seem to succeed. Unsure why.
          case _: Throwable => println("        " + "Unable to load class.")
        }
      }
    }
  }

  def loadStmtsFromJaamFile(jaamFile: String) = {

  }
}

/*
Notes from Michael:

h: method -> Set<Stmt>

- tool coverage <jaam> <jar>

---

classes = ...jar...
methods = ...classes...
for m <- methods
  h[m] = (empty set)
for stmt <- jaam
  h[stmt.method] += stmt
for (m, ss) <- h
  if ss not (empty set)
    print method
  else
    (count) ++
print "(count) of h.size"
 */
