package org.ucombinator.jaam.tools

// TODO: nodes for loops (allows us to group methods that are in the same loop)
// TODO: vertical separation
// TODO: how confident are we in the coverage of rsa_commander?
// TODO: headlabel
// TODO: Compound graphs so loops live inside methods?
// TODO: test coverage

import org.jgrapht.graph.{DefaultDirectedGraph, DefaultEdge}
import org.jgrapht.{Graph, Graphs}
import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.util.{CachedHashCode, Loop, Stmt}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.jimple.{IfStmt, Stmt => SootStmt}
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}

import scala.collection.JavaConversions._
import scala.collection.{immutable, mutable}

object LoopAnalyzer {
  case class LoopTree(loop: SootLoop, method: SootMethod, children: Set[LoopTree]) {
    def contains(stmt: SootStmt): Boolean = {
      loop.getLoopStatements.contains(stmt)
    }
    def isParent(other: LoopTree): Boolean = {
      other.loop.getLoopStatements.toSet.subsetOf(loop.getLoopStatements.toSet)
    }
    def insert(child: LoopTree): LoopTree = {
      val grandchildren: Set[LoopTree] = children filter child.isParent
      val parents: Set[LoopTree] = children filter { _.isParent(child) }

      if (parents.nonEmpty) {
        assert(parents.size <= 1,
            "two disparate loops contain the same child")
        assert(grandchildren.isEmpty, "malformed tree")
        val parent = parents.head
        val newParent = parent.insert(child)
        LoopTree(loop, method, (children - parent) + newParent)
      } else {
        // child becomes a direct descendant containing 0 or more children
        val node = LoopTree(child.loop, method, child.children ++ grandchildren)
        LoopTree(loop, method, (children -- grandchildren) + node)
      }
    }
    // assumes that its loop contains the stmt in question
    def parent(stmt: SootStmt): LoopTree = {
      val parents = children filter  { _.loop.getLoopStatements.contains(stmt) }
      if (parents.isEmpty) {
        this
      } else {
        assert(parents.size <= 1,
            "two disparate loops contain the same child")
        parents.head.parent(stmt)
      }
    }
    def prettyPrint(indent: Int = 0): Unit = {
      println(f"Method: $method")
      println("Head:")
      println(loop.getHead)
      for (stmt <- loop.getLoopStatements) {
        val next = Stmt(stmt, method).nextSemantic.map(_.sootStmt).map({x => Stmt.getIndex(x, method)})
        println(f"Stmt: ${Stmt.getIndex(stmt, method)} $next")
        println(stmt)
      }
      println()
      println("Children:")
      for (child <- children) {
        child.prettyPrint(indent+1)
        println()
      }
      println("End Children:")

      val graph = new DefaultDirectedGraph[SootStmt, DefaultEdge](classOf[DefaultEdge])

      // TODO: all nextSemantic that are not in the loop are exit jumps
      // Include them in graph along with a synthetic final statement
      for (node <- loop.getLoopStatements) {
        graph.addVertex(node)
      }

      for (node <- loop.getLoopStatements) {
        for (target <- Stmt(node, method).nextSemantic) {
          if (graph.containsVertex(target.sootStmt)) {
            graph.addEdge(node, target.sootStmt)
          }
        }
      }

      println("START_GRAPH")
      println(graph)
      println("END_GRAPH")

      val imm = dominatorTree(loop.getHead, graph)

      //new DOTExporter().exportGraph(imm, System.out)
      println("START_IMM")
      for (i <- imm.keys) {
        println(f"${Stmt.getIndex(i, method)}:$i -> ${Stmt.getIndex(imm(i), method)}:${imm(i)}")
      }
      println("END_IMM")
    }

    def dominatorTree[V,E](root: V, graph: Graph[V,E]): immutable.Map[V, V] = {
      val dom = new mutable.HashMap[V, mutable.Set[V]] with mutable.MultiMap[V, V]

      dom.addBinding(root, root)
      for (i <- graph.vertexSet if i != root) {
        dom(i) = graph.vertexSet
      }

      println("START_DOM")
      for (i <- dom.keys) {
        println(f"key: $i")
        println("val:"+dom(i))
      }
      println("END_DOM")

      var done = false
      while (!done) {
        done = true
        for (i <- graph.vertexSet if i != root) {
          var newDom = dom(i).clone()
          for (j <- Graphs.predecessorListOf(graph, i)) {
            newDom = (newDom & dom(j)) + i
          }
          if (newDom != dom(i)) {
            dom(i) = newDom
            done = false
          }
        }
      }

      println("START_DOM2")
      for (i <- dom.keys) {
        println(f"key: $i")
        println("val:"+dom(i))
      }
      println("END_DOM2")

      var imm: immutable.Map[V, V] = Map.empty

      for (i <- graph.vertexSet if i != root) {
        for (j <- dom(i)) {
          println(f"imm: ${dom(i).size} ${dom(j).size} $i $j")
          if (dom(j).size == dom(i).size - 1) {
            imm += (i -> j)
          }
        }
      }

      return imm
    }
  }
  object LoopTree {
    def apply(loop: SootLoop, method: SootMethod): LoopTree = new LoopTree(loop, method, Set.empty)
  }

  abstract sealed class Node extends CachedHashCode {
    val tag: String
  }
  case class LoopNode(m: SootMethod, loop: SootLoop) extends Node {
    override val tag: String = m.getSignature + "\ninstruction #" + Stmt(loop.getHead, m).index
    val index: Int = Stmt(loop.getHead, m).index
  }
  // TODO we might have uniqueness problems with SootMethod objects.
  // For now, SootMethod.getSignature will do.
  case class MethodNode(method: SootMethod) extends Node {
    override val tag: String = method.getSignature
  }
}

import LoopAnalyzer._

case class LoopGraph(m: SootMethod, private val g: Map[Node, Set[Node]],
    private val recurEdges: Set[(Node, Node)]) {
  private val mNode = MethodNode(m)

  def apply(n: Node): Set[Node] = g.getOrElse(n, Set.empty)

  def keySet: Set[String] = g.keySet.
    withFilter(_.isInstanceOf[MethodNode]).
    map(_.tag)

  def +(binding: (Node, Set[Node])): LoopGraph = {
    val (k, v) = binding
    LoopGraph(m, g + (k -> (this(k) ++ v)), recurEdges)
  }

  // remove method leaves
  def prune: LoopGraph = {
    var keepMap: Map[Node, Boolean] = Map.empty[Node, Boolean]
    var parentMap: Map[Node, Set[Node]] = Map.empty[Node, Set[Node]]
    var recursionEdges = recurEdges

    def analyze(n: Node, path: List[Node]): Unit = {
      if (!keepMap.isDefinedAt(n)) {
        if (path.contains(n)) {
          val loopNodes = n :: path.takeWhile(_ != n)
          val rotated = loopNodes.tail :+ n
          recursionEdges = recursionEdges ++ loopNodes.zip(rotated)
          keepMap = keepMap + (n -> true)
        } else {
          val succs = this(n)
          for {
            succ <- succs
          } {
            val parents = parentMap.getOrElse(succ, Set.empty) + n
            parentMap = parentMap + (succ -> parents)
            analyze(succ, n :: path)
          }
          val keep = succs.foldLeft(n.isInstanceOf[LoopNode] ||
                                    keepMap.getOrElse(n, false))({
              (keep: Boolean, succ: Node) =>
            keepMap.get(succ) match {
              case Some(keepSucc) => keep || keepSucc
              case None =>
                println("WARNING: " + succ +
                    " should already have been analyzed")
                keep
            }
          })
          keepMap = keepMap + (n -> keep)
        }
      }
    }
    analyze(mNode, List())
    val newGraph = keepMap.foldLeft(g)({
        (g: Map[Node, Set[Node]], pair: (Node, Boolean)) =>
      val (n, keep) = pair
      if (keep) {
        g
      } else {
        val parents = parentMap.getOrElse(n, Set.empty)
        parents.foldLeft(g - n)({
            (g: Map[Node, Set[Node]], parent: Node) =>
          g + (parent -> (g.getOrElse(parent, Set.empty) - n))
        })
      }
    })
    LoopGraph(m, newGraph, recursionEdges)
  }

  // remove loopless method calls, replacing them with downstream loops
  def shrink: LoopGraph = {
    // keepers is a set of MethodNode objects that should remain. All LoopNode
    // objects are kept, so there's no need to add them to a set.
    var keepers = Set(mNode)
    // descMap keeps track of the descendants to be kept from a node. Nodes
    // that should be kept return a set containing just themselves; nodes that
    // are to be discarded return the merged results from their children.
    var descMap = Map.empty[Node, Set[Node]]
    var newGraph = g
    var recursionEdges = recurEdges
    def shouldKeep(n: Node): Boolean = {
      keepers.contains(n) || n.isInstanceOf[LoopNode]
    }
    def analyze(n: Node, path: List[Node]): Set[Node] = {
      //println(f"analyzer $n $path")
      n match {
        // if there is a loop,
        case m: MethodNode if path.contains(n) =>
          // get the method nodes in the loop and mark them
          val loopNodes = n :: path.takeWhile(_ != n)
          val rotated = loopNodes.tail :+ n
          recursionEdges = recursionEdges ++ loopNodes.zip(rotated)
          val toKeep = loopNodes flatMap {
            case m: MethodNode => Some(m)
            case _ => None
          }
          keepers = keepers ++ toKeep
          Set(n)
        case _ =>
          // recur and store the resulting sets of descendants
          for {
            child <- this(n)
          } {
            if (!descMap.isDefinedAt(child)) {
              descMap = descMap + (child -> analyze(child, n :: path))
            }
          }
          // in the case that n should be kept,
          if (shouldKeep(n)) {
            // replace each child with the set returned by its call to analyze
            for {
              child <- this(n) filter { !shouldKeep(_) }
            } {
              val newChildren = (newGraph(n) - child) ++ descMap(child)
              newGraph = newGraph + (n -> newChildren)
            }
            // and keep n
            Set(n)
          } else {
            // otherwise, roll all of the children's sets together
            // crucially, the set returned does not include n
            this(n).foldLeft(Set.empty[Node])({
              (descendants: Set[Node], child: Node) =>
                descendants ++ descMap(child)
            })
          }
      }
    }
    analyze(mNode, List.empty)
    LoopGraph(m, newGraph, recursionEdges)
  }

/*
  override def toString: String = {
    val builder = new StringBuilder
    var seen = Set.empty[Node]
    def inner(from: Node): Unit = {
      if (!seen.contains(from)) {
        seen = seen + from
        builder ++= from.toString
        for {
          to <- this(from)
        } {
          val maybeColored = if (recurEdges.contains((from, to))) {
            " [penwidth=10, color=\"blue\"]"
          } else " [penwidth=10]"
          builder ++= "  " + quote(from.tag) + " -> " + quote(to.tag) +
            maybeColored + ";\n"
        }
        // enforce a BFS order
        for {
          to <- this(from)
        } {
          inner(to)
        }
      }
    }
    inner(mNode)
    builder.toString
  }
  */

  def toJaam(s: serializer.PacketOutput,
             roots: Set[SootMethod] = Set()) {
    var seen = Set.empty[Node]
    var names = Map.empty[Node, serializer.Id[serializer.LoopNode]]
    def name(node: Node): serializer.Id[serializer.LoopNode] = {
      names.get(node) match {
        case Some(id) => id
        case None =>
          val id = serializer.Id[serializer.LoopNode](names.size)
          names += (node -> id)
          id
      }
    }
    def inner(from: Node): Unit = {
      if (!seen.contains(from)) {
        seen = seen + from
        val id = name(from)
        val packet = from match {
          case MethodNode(m) =>
            println(f"Serializing method: $m")
            serializer.LoopMethodNode(id, m)
          case n@LoopNode(m, _) =>
            val stmt = Taint.getByIndex(m, n.index+1) // add one because the loop node is apparently the instruction before...?
            val addrs = stmt match {
              case sootStmt: IfStmt => Taint.addrsOf(sootStmt.getCondition, m)
              case _ =>
                println("TODO: investigate why the loop guard is not an IfStmt (" + stmt + ")")
                Set.empty[serializer.TaintAddress]
            }
            serializer.LoopLoopNode(id, m, addrs, n.index)
        }

        // println("Writing: " + packet)
        s.write(packet)
        for {
          to: Node <- this(from)
        } {
          // TODO: Instead of ignoring the roots this way, modify the BFS, both here and in makeLoopGraph in Loop3.
          if (!to.isInstanceOf[MethodNode] || !roots.contains(to.asInstanceOf[MethodNode].method)) {
            // println("Edge: " + name(from) + "->" + name(to))
            s.write(serializer.LoopEdge(
              name(from), name(to), recurEdges.contains((from, to))))
          }
          else {
            // println("Skipping edge: " + name(from) + "->" + name(to))
          }
        }

        // enforce a BFS order
        for (to <- this(from)) inner(to)
      }
    }
    inner(mNode)
  }
}
