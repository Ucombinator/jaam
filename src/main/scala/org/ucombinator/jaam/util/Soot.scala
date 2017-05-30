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
  import soot.{Scene,SootClass,SootMethod,SootResolver}

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
    def addJaamClasses(file: String): Unit = {
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

    def getSootClass(s : String) = Scene.v().loadClass(s, SootClass.SIGNATURES)
    def getBody(m : SootMethod) = {
      if (m.isNative) { throw new Exception("Attempt to Soot.getBody on native method: " + m) }
      if (m.isAbstract) { throw new Exception("Attempt to Soot.getBody on abstract method: " + m) }
      // TODO: do we need to test for phantom here?
      if (!m.hasActiveBody()) {
        SootResolver.v().resolveClass(m.getDeclaringClass.getName, SootClass.BODIES)
        m.retrieveActiveBody()
      }
      m.getActiveBody
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
