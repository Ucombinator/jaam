package org.ucombinator.jaam.tools.taint2

import org.ucombinator.jaam.util.Soot
import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools.app.{Origin, App}
import org.ucombinator.jaam.util.Stmt

import org.ucombinator.jaam.util.Soot.unitToStmt

import org.jgrapht._
import org.jgrapht.graph._

import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.options.Options
import soot.jimple.{Stmt => SootStmt, _}

import scala.collection.immutable
import scala.collection.JavaConverters._

abstract class TaintAddress

case class LocalAddr(local: Local) extends TaintAddress // TODO: Include method and maybe stmt (but probably not)

object Taint2 {
  def main(input: List[String], output: String) {

    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    //Options.v().set_soot_classpath(classpath.mkString(":"))
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)
    //Options.v().set_whole_program(true)-
    //Options.v().set_app(true)-
    soot.Main.v().autoSetOptions()

    //Options.v().setPhaseOption("cg", "verbose:true")
    //Options.v().setPhaseOption("cg.cha", "enabled:true")

    //Options.v.set_main_class(mainClass)
    //Scene.v.setMainClass(clazz)
    //Scene.v.addBasicClass(className, SootClass.HIERARCHY)
    //Scene.v.setSootClassPath(classpath)
    //Scene.v.loadNecessaryClasses

    Soot.useJaamClassProvider()

    val inputPackets = input.map(Serializer.readAll(_)).flatten

    for (a <- inputPackets) { Soot.addClasses(a.asInstanceOf[App]) }

    val mainClasses = for (a <- inputPackets) yield { a.asInstanceOf[App].main.className }
    val mainMethods = for (a <- inputPackets) yield { a.asInstanceOf[App].main.methodName }
    val mainClass = mainClasses(0).get // TODO: fix
    val mainMethod = mainMethods(0).get // TODO: fix

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()

    val graph = new DefaultDirectedGraph[TaintAddress, DefaultEdge](classOf[DefaultEdge])

    var class_count = 0
    var method_count = 0
    var stmt_count = 0
    var target_count = 0

    var edges = immutable.Map[Stmt, Set[SootMethod]]()

    def invokeExprTargets(expr: InvokeExpr): Set[SootMethod] = {
      val m = expr.getMethod
      val c = m.getDeclaringClass
      val cs: Set[SootClass] = expr match {
        case expr : DynamicInvokeExpr =>
          // Could only come from non-Java sources
          throw new Exception(s"Unexpected DynamicInvokeExpr: $expr")
        case expr : StaticInvokeExpr => Set(c)
        // SpecialInvokeExpr is also a subclasses of InstanceInvokeExpr but we need to treat it special
        case expr: SpecialInvokeExpr => Set(c)
        case expr: InstanceInvokeExpr =>
          if (c.isInterface) {
            // TODO: Main performance cost, but can't cache because new new hierarchy when scene changes (due to getSootClass?)
            Scene.v.getActiveHierarchy.getImplementersOf(c).asScala.toSet
          } else {
            Scene.v.getActiveHierarchy.getSubclassesOfIncluding(c).asScala.toSet
          }
      }

      var ms = Set[SootMethod]()

      for (c2 <- cs) {
        if (!c2.isInterface) {
          val m2 = c2.getMethodUnsafe(m.getNumberedSubSignature)
          if (m2 != null) {
            ms += m2
          }
        }
      }

      ms
    }

    def stmtTargets(stmt: Stmt): Set[SootMethod] = stmt.sootStmt match {
      case s: InvokeStmt => invokeExprTargets(s.getInvokeExpr)
      case s: DefinitionStmt =>
        s.getRightOp match {
          case s: InvokeExpr => invokeExprTargets(s)
          case _ => Set()
        }
      case _ => Set()
    }

    def addrsOf(value: SootValue): Set[TaintAddress] = {
      value match {
        case value: Local => Set(LocalAddr(value))
        // TODO: add more
        case _ => Set() // TODO: throw error instead of returning empty set
      }
    }

    // Get all classes loaded so Soot doesn't keep recomputing the Hierarchy
    for (name <- Soot.loadedClasses.keys) {
      Soot.getSootClass(name)
      println(f"class name: $name")
    }

    for (name <- Soot.loadedClasses.keys) {
        class_count += 1
        //val name = entry.getName.replace("/", ".").replaceAll("\\.class$", "")
        println(f"class origin ${Soot.loadedClasses(name).origin} $class_count: $name")

      if (Soot.loadedClasses(name).origin == Origin.APP) {
        val c = Soot.getSootClass(name)
        // The .toList prevents a concurrent access exception
        for (m <- c.getMethods.asScala.toList) {
          method_count += 1
          //println(f"method $method_count: $m")
          if (m.isNative) { println("skipping body because native") }
          else if (m.isAbstract) { println("skipping body because abstract") }
          else {
            for (sootStmt <- Soot.getBody(m).getUnits.asScala) {

              println(sootStmt)

              sootStmt match {
                case sootStmt: DefinitionStmt =>
                  for (src <- addrsOf(sootStmt.getRightOp)) {
                    for (dst <- addrsOf(sootStmt.getLeftOp)) {
                      Graphs.addEdgeWithVertices(graph, src, dst)
                    }
                  }
                  /// TODO: more cases
                case _ => {} // TODO: error instead of noop
              }
/*
              stmt_count += 1
              //println(f"stmt $stmt_count: $sootStmt")
              val s = Stmt(Stmt.unitToStmt(sootStmt), m)
              val ts = stmtTargets(s)
              target_count += ts.size
              if (edges.contains(s)) {
                println(f"already have edge from $s")
              }
              edges += s -> ts
              //edges += s -> (ts ++ edges.get(s).getOrElse(Set()))
              // TODO: cache ts
              if (!ts.isEmpty) {
                //println(f"$target_count.$c.$m.${s.index}: $ts")
              }
*/
            }
          }
          //println(f"end method $c $m")
        }
        println(f"end class $c")
      }
    }

    println(f"Graph: $graph")

    for (e <- graph.edgeSet.asScala) {
      println(f"Edge: ${e}")
    }
    // TODO: print in GraphViz (dot) format
    // TODO: serialize to "output"

    // TODO: option to allow selecting only sub-part of graph

    // TODO: work with visualizer team to get it visualized
  }
}
