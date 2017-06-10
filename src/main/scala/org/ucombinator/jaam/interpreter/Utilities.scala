package org.ucombinator.jaam.interpreter

import java.util.zip.ZipInputStream
import java.io.{FileInputStream, File}

import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}

import org.ucombinator.jaam.main.Log
import org.ucombinator.jaam.util.Soot

object Utilities {
  def stringToType(s: String): Type = {
    s match {
      case "boolean" => BooleanType.v()
      case "byte" => ByteType.v()
      case "char" => CharType.v()
      case "double" => DoubleType.v()
      case "float" => FloatType.v()
      case "int" => IntType.v()
      case "long" => LongType.v()
      case "short" => ShortType.v()
      case "void" => VoidType.v()
      case clazz => Soot.getSootClass(clazz).getType()
    }
  }

  def parseType(name: String): Type = {
    var i = 0
    while (name(i) == '[') {
      i += 1
    }
    val baseType = name(i) match {
      //case '[' => ArrayType.v(parseType(name.substring(1)), 1)
      case 'Z' => BooleanType.v()
      case 'B' => ByteType.v()
      case 'C' => CharType.v()
      case 'L' => Soot.getSootClass(name.substring(i + 1, name.length() - 1)).getType()
      case 'D' => DoubleType.v()
      case 'F' => FloatType.v()
      case 'I' => IntType.v()
      case 'J' => LongType.v()
      case 'S' => ShortType.v()
    }
    if (i == 0) { baseType }
    else { ArrayType.v(baseType, i) }
  }

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
