package org.ucombinator.jaam.tools.taint2

import java.io.{BufferedWriter, FileWriter}

import org.ucombinator.jaam.util.Soot
import org.ucombinator.jaam.serializer._
import org.ucombinator.jaam.tools.app.{App, Origin}
import org.ucombinator.jaam.util.Stmt
import org.ucombinator.jaam.util.Soot.unitToStmt
import org.jgrapht._
import org.jgrapht.graph._
import org.jgrapht.ext.DOTExporter
import soot.options.Options
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import scala.collection.immutable
import scala.collection.JavaConverters._


object Taint2 {
  def main(input: List[String], output: String) {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)

    soot.Main.v().autoSetOptions()

    Soot.useJaamClassProvider()

    val inputPackets = input.flatMap(Serializer.readAll)

    for (a <- inputPackets) { Soot.addClasses(a.asInstanceOf[App]) }

//    val mainClasses = for (a <- inputPackets) yield { a.asInstanceOf[App].main.className }
//    val mainMethods = for (a <- inputPackets) yield { a.asInstanceOf[App].main.methodName }
//    val mainClass = mainClasses.head.get // TODO: fix
//    val mainMethod = mainMethods.head.get // TODO: fix

    Scene.v.loadBasicClasses()   // ???
    PackManager.v.runPacks()    // ???

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
          throw new Exception(s"Unexpected DynamicInvokeExpr: $expr")
        case _ : StaticInvokeExpr => Set(c)  // --
        // SpecialInvokeExpr is also a subclasses of InstanceInvokeExpr but we need to treat it special
        case _: SpecialInvokeExpr => Set(c)  // --
        case _: InterfaceInvokeExpr => // A subclasses of InstanceInvokeExpr
          // TODO: Main performance cost, but can't cache because new new hierarchy when scene changes (due to getSootClass?)
          Scene.v.getActiveHierarchy.getImplementersOf(c).asScala.toSet
        case _: VirtualInvokeExpr => // A subclasses of InstanceInvokeExpr
          Scene.v.getActiveHierarchy.getSubclassesOfIncluding(c).asScala.toSet
      }

      (for (c2 <- cs if !c2.isInterface) yield
        c2.getMethodUnsafe(m.getNumberedSubSignature) match {
          case null => None
          case m2   => Some(m2)
        }).flatten
    }

    def stmtTargets(stmt: Stmt): Set[SootMethod] = stmt.sootStmt match {
      case s: InvokeStmt => invokeExprTargets(s.getInvokeExpr)
      case s: DefinitionStmt =>
        s.getRightOp match {
          case ie: InvokeExpr => invokeExprTargets(ie)
          case _ => Set()
        }
      case _ => Set()
    }


    // TODO (petey/michael): is InvokeExpr the only expr with side effects?
    def addrsOf(expr: SootValue, m: SootMethod): Set[TaintAddress] = {
      expr match {
        case l : Local => Set(LocalTaintAddress(m, l))
        // TODO this could throw an exception
        case pr: ParameterRef => Set(ParameterTaintAddress(m, pr.getIndex))
        case _: ThisRef => Set(ThisRefTaintAddress(m))
        case r : Ref => Set(RefTaintAddress(m, r))
        case c : Constant => Set(ConstantTaintAddress(m, c))
        case unop : UnopExpr => addrsOf(unop.getOp, m)
        case binop : BinopExpr =>
          // TODO in the case of division, this could throw an exception
          addrsOf(binop.getOp1, m) ++
            addrsOf(binop.getOp2, m)
        case io : InstanceOfExpr => addrsOf(io.getOp, m)
        // TODO this could throw an exception
        case cast : CastExpr => addrsOf(cast.getOp, m)
        case invoke : InvokeExpr => Set(InvokeTaintAddress(m, invoke))
        case na : NewArrayExpr =>
          addrsOf(na.getSize, m)
        case _ : NewExpr => Set.empty
        case nma : NewMultiArrayExpr =>
          nma.getSizes.asScala.toSet flatMap { (exp: SootValue) => addrsOf(exp, m) }
        case _ =>
          println(expr)
          Set.empty
      }
    }

    val updated = collection.mutable.Map[SootClass, Set[SootMethod]]().withDefaultValue(Set())

    def updateGraph(c: SootClass, m: SootMethod): Unit = {
      if (updated.contains(c) && updated(c).contains(m)) { () }
      else {
        if (m.isNative) { println("skipping body because native") }
        else if (m.isAbstract) { println("skipping body because abstract") }
        else {
          for (sootUnit <- Soot.getBody(m).getUnits.asScala) {
            sootUnit match {
              case sootStmt: DefinitionStmt =>
                for (src <- addrsOf(sootStmt.getRightOp, m);
                     dst <- addrsOf(sootStmt.getLeftOp, m)) {
                  Graphs.addEdgeWithVertices(graph, src, dst)
                }

                for (sootMethod <- stmtTargets(Stmt(sootUnit, m))) {
                  updateGraph(sootMethod.getDeclaringClass, sootMethod)
                }
              case sootStmt : InvokeStmt => ()
                for (sootMethod <- invokeExprTargets(sootStmt.getInvokeExpr)) {
                  updateGraph(sootMethod.getDeclaringClass, sootMethod)
                }
              case _ : ReturnVoidStmt => ()
              case _ : NopStmt => ()
              case _ : GotoStmt => ()
              case _ => ()
            }
          }
        }
        updated(c) += m
      }
    }

    def printToDOT(filename: String): Unit = {
      val dotExporter = new DOTExporter[TaintAddress, DefaultEdge]()
      val out = new BufferedWriter(new FileWriter(filename))
      dotExporter.exportGraph(graph, out)
    }

    for (name <- Soot.loadedClasses.keys) {
      class_count += 1
      println(f"class origin ${Soot.loadedClasses(name).origin} $class_count: $name")

      if (Soot.loadedClasses(name).origin == Origin.APP) {
        val c = Soot.getSootClass(name)
        println(f"class name: $name")
        // The .toList prevents a concurrent access exception
        for (m <- c.getMethods.asScala.toList) {
          method_count += 1
          updateGraph(c, m)
        }
        println(f"end class $c")
      }
    }

    println(f"Graph: $graph")

    printToDOT("Test.gv")


    // For Test
    for (e <- graph.edgeSet.asScala) {
      println(f"Edge: ${e}")
    }
    // TODO: print in GraphViz (dot) format   ----- DONE
    // TODO: serialize to "output"            ----- ???
    // TODO: option to allow selecting only sub-part of graph          ----- ???
    // TODO: work with visualizer team to get it visualized            ----- TODO
  }
}
