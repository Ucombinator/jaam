package org.ucombinator.jaam.util

import scala.collection.JavaConverters._

import scala.collection.immutable
import scala.collection.mutable

import org.jgrapht._

object JGraphT {
  // Returns a mapping from nodes to the set of nodes that dominate them
  def dominators[V,E](graph: DirectedGraph[V,E], start: V):
  immutable.Map[V, immutable.Set[V]] = {
    val vs = graph.vertexSet.asScala.toSet
    var dom: immutable.Map[V, immutable.Set[V]]  = Map.empty

    // Initial assignment
    dom = dom + (start -> immutable.Set(start))
    for (v <- vs if v != start) {
      dom = dom + (v -> vs)
    }

    // Iteration until fixed point
    var old_dom = dom
    do {
      old_dom = dom
      for (v <- vs if v != start) {
        dom = dom +
          (v -> (
            immutable.Set(v) ++
              Graphs.predecessorListOf(graph, v).
                asScala.map(dom).
                fold(vs)(_ & _)))
      }
    } while (old_dom != dom)

    return dom
  }

  // Returns a mapping from head nodes to back-jump nodes
  def loopHeads[V,E](graph: DirectedGraph[V,E], start: V):
  immutable.Map[V, immutable.Set[V]] = {

    val dom = JGraphT.dominators(graph, start)
    val heads = new mutable.HashMap[V, mutable.Set[V]] with mutable.MultiMap[V, V]

    for (v <- graph.vertexSet.asScala) {
      for (s <- Graphs.successorListOf(graph, v).asScala if dom(v).contains(s)) {
        heads.addBinding(s, v)
      }
    }

    // Make it immutable
    return heads.map({ case (k, vs) => (k, vs.toSet) }).toMap
  }

  // Takes head and back-jump nodes for a loop, and returns the set of
  // nodes that are part of that loop
  def loopNodes[V,E](graph: DirectedGraph[V,E], head: V, backJumpNodes: immutable.Set[V]):
  immutable.Set[V] = {
    var loop: immutable.Set[V] = Set.empty

    // Iteration until work list is empty
    var work = backJumpNodes.toList
    while (work.nonEmpty) {
      val v = work.head
      work = work.tail
      if (!loop.contains(v)) {
        loop = loop + v
        work = Graphs.predecessorListOf(graph, v).asScala.toList ++ work
      }
    }

    return loop
  }

  // Maps head nodes to sets of nodes that are part of the loop
  def loopNodes[V,E](graph: DirectedGraph[V,E], start: V):
  immutable.Map[V, immutable.Set[V]] = {
    val heads = JGraphT.loopHeads(graph, start)
    return heads.map({ case (k, vs) => (k, loopNodes(graph, k, vs)) })
  }
}
