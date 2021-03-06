package org.ucombinator.jaam.util

import java.io._
import java.util.jar._
import java.util.zip._

trait CachedHashCode extends Product {
  override lazy val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(this)
}

object Misc {
  def toByteArray(input: InputStream): Array[Byte] = {
    val output = new ByteArrayOutputStream()

    var size = 0
    val buffer = new Array[Byte](128*1024)

    while ({size = input.read(buffer, 0, buffer.length); size != -1}) {
      output.write(buffer, 0, size)
    }

    output.flush()

    return output.toByteArray
  }
}

object Jar {
  def jar(input: Array[Byte]): JarInputStream = {
    jar(new ByteArrayInputStream(input))
  }

  def jar(input: InputStream): JarInputStream = {
    new JarInputStream(input)
  }

  // TODO: lots of code needs context closers
  def entries(input: Array[Byte]): List[(JarEntry, Array[Byte])] = {
    entries(new ByteArrayInputStream(input))
  }

  def entries(input: InputStream): List[(JarEntry, Array[Byte])] = {
    entries(jar(input))
  }

  def entries(input: JarInputStream): List[(JarEntry, Array[Byte])] = {
    var entries = List[(JarEntry, Array[Byte])]()

    var entry: JarEntry = null
    while ({entry = input.getNextJarEntry(); entry != null}) {
      // `entry.getSize` may be -1 to signal unknown so we use `toByteArray`
      entries ++= List((entry, Misc.toByteArray(input)))
    }

    return entries
  }
}

object Zip {
  def zip(input: InputStream): ZipInputStream = {
    new ZipInputStream(input)
  }

  def entries(input: ZipInputStream): List[(ZipEntry, Array[Byte])] = {
    var entries = List[(ZipEntry, Array[Byte])]()
    var entry: ZipEntry = null
    while ({entry = input.getNextEntry(); entry != null}) {
      // `entry.getSize` may be -1 to signal unknown so we use `toByteArray`
      entries ++= List((entry, Misc.toByteArray(input)))
    }
    return entries
  }

  // TODO: It seems this is a better implementation of `entries`,
  // TODO: (continue) but it will trigger errors of taint2 (not app, but the generated app.jaam is different, which is
  // TODO: (continue) the direct source of triggering errors of taint2)

  // TODO: The problem comes from the `yield` part ???

  //  def entries(input: ZipInputStream): List[(ZipEntry, Array[Byte])] = {
  //    for (entry <- Stream.continually(input.getNextEntry).takeWhile(_ != null).toList)
  //      yield (entry, Misc.toByteArray(input))
  //  }
}
