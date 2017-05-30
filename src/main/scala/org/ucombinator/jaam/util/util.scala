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
  // TODO: lots of code needs context closers
  def entries(input: InputStream): List[(JarEntry, Array[Byte])] = {
    val jar = new JarInputStream(input)
    var entries = List[(JarEntry, Array[Byte])]()

    var entry: java.util.jar.JarEntry = null
    while ({entry = jar.getNextJarEntry(); entry != null}) {
      // `entry.getSize` may be -1 to signal unknown so we use `toByteArray`
      entries ++= List((entry, Misc.toByteArray(jar)))
    }

    return entries
  }
}
