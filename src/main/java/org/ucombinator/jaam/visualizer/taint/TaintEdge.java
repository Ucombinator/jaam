package org.ucombinator.jaam.visualizer.taint;

import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.layout.HierarchicalEdge;

public class TaintEdge extends HierarchicalEdge<TaintVertex> implements Edge<TaintVertex> {

    public TaintEdge(TaintVertex src, TaintVertex dst) {
        super(src, dst, EDGE_TYPE.EDGE_REGULAR);
    }
}
