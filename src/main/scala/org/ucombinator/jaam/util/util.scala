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

  object Soot {

    // TODO: classes track whether app/lib/java
    var classes = Map[String, (String, Array[Byte])]()

    def useAppProvider(): Unit = {
      SourceLocator.v.setClassProviders(List[ClassProvider](new JaamClassProvider).asJava)
    }

    def addClasses(app: App): Unit = {
      app.classpath.java.map(load)
      app.classpath.lib.map(load)
      app.classpath.app.map(load)
    }

    // TODO: optional flags to load only some parts?
    def addClasses(file: String): Unit = {
      val stream = new FileInputStream(file)
      val pi = new serializer.PacketInput(stream)

      var packet: serializer.Packet = null
      while ({packet = pi.read(); !packet.isInstanceOf[serializer.EOF]}) {
      // TODO: for (packet <- pi) {
        packet match { case packet: App => addClasses(packet) }
      }
    }


    private def load(p: PathElement) {
      //println(f"p ${p.path} and ${p.root}")
      for (d <- p.classData()) {
        //println(f"d ${d.length}")
        val cr = new ClassReader(new ByteArrayInputStream(d))
        val cn = new ClassNode()
        cr.accept(cn, 0)
        //println(f"cn.name: ${cn.name}")
        classes += cn.name.replace('/', '.') -> (("TODO:JaamClassProvider", d))
      }
    }
  }

  class JaamClassProvider extends ClassProvider {

    override def find(cls: String): ClassSource = {
      //println(f"find: $cls")
      Soot.classes.get(cls) match {
        case None => null
        case Some((path, bytes)) => new JaamClassSource(path, new JaamFoundFile(cls, bytes))
      }
    }
  }
}

package soot {
  import java.io.{ByteArrayInputStream, File, InputStream}

  class JaamFoundFile(path: String, data: Array[Byte]) extends SourceLocator.FoundFile(new File(f"JaamFoundFile:$path")) {
    override def inputStream(): InputStream = new ByteArrayInputStream(data)
  }
}

package soot.asm {
  import soot.SourceLocator
  class JaamClassSource(cls: String, foundFile: SourceLocator.FoundFile) extends AsmClassSource(cls, foundFile)
}
