package org.ucombinator.jaam.util

import java.io._
import java.util.jar._

object Misc {
  def toByteArray(input: InputStream): Array[Byte] = {
    val output = new ByteArrayOutputStream()

    var size = 0
    var buffer = new Array[Byte](128*1024)

    while ({size = input.read(buffer, 0, buffer.length); size != -1}) {
      output.write(buffer, 0, size);
    }

    output.flush()

    return output.toByteArray()
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
