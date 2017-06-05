package org.ucombinator.jaam.util

import java.io._

import scala.collection.JavaConverters._

import org.objectweb.asm._
import org.objectweb.asm.tree._
import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.tools.app.{App, PathElement, Origin}
import soot.{ClassProvider, Scene, SootClass, SootMethod, SootResolver, SourceLocator}

object Soot {

  def useJaamClassProvider(): Unit = {
    SourceLocator.v.setClassProviders(List[ClassProvider](new JaamClassProvider).asJava)
  }

  // TODO: classes track whether app/lib/java
  case class ClassData(source: String, role: Origin, data: Array[Byte])
  var classes = Map[String, ClassData]()

  private def load(p: PathElement) {
    //println(f"p ${p.path} and ${p.root}")
    for (d <- p.classData()) {
      //println(f"d ${d.length}")
      val cr = new ClassReader(new ByteArrayInputStream(d))
      val cn = new ClassNode()
      cr.accept(cn, 0)
      //println(f"cn.name: ${cn.name}")
      classes += cn.name.replace('/', '.') -> ClassData("TODO:JaamClassProvider", p.role, d)
    }
  }

  def addClasses(app: App): Unit = { app.classpath.map(load) }

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
