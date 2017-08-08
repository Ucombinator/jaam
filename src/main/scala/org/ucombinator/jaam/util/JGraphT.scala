package org.ucombinator.jaam.util

import scala.collection.JavaConverters._

import scala.collection.immutable

import org.jgrapht._

object JGraphT {
  def dominators[V,E](graph: DirectedGraph[V,E], start: V):
      immutable.Map[V, immutable.Set[V]] = {
    val vs = graph.vertexSet.asScala.toSet
    var dom = immutable.Map[V, immutable.Set[V]]()

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
        dom = dom + (v -> (immutable.Set(v) ++ Graphs.predecessorListOf(graph, v).asScala.map(p => dom(p)).fold(vs)(_ & _)))
      }
    } while (old_dom != dom)

    return dom
  }
}
