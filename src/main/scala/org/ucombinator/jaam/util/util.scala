package org.ucombinator.jaam.util {

  import java.io.File

  import scala.collection.JavaConverters._

  import org.ucombinator.jaam.tools.app.{App, PathElement}
  import soot.{ClassProvider, ClassSource, JaamFoundFile, SourceLocator}
  import soot.asm.JaamClassSource
  import org.objectweb.asm._
  import org.objectweb.asm.tree._
  import java.io._
  import org.ucombinator.jaam.serializer

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
    import java.util.jar._

    // TODO: lots of code needs context closers
    def entries(input: InputStream): List[(JarEntry, Array[Byte])] = {
      val jar = new java.util.jar.JarInputStream(input)
      var entries = List[(JarEntry, Array[Byte])]()

      var entry: java.util.jar.JarEntry = null
      while ({entry = jar.getNextJarEntry(); entry != null}) {
        // `entry.getSize` may be -1 to signal unknown so we use `toByteArray`
        entries ++= List((entry, Misc.toByteArray(jar)))
      }

      return entries
    }
  }
}
