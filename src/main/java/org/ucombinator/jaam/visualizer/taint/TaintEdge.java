package org.ucombinator.jaam.visualizer.taint;

import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalEdge;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

public class TaintEdge extends LayoutEdge<TaintVertex> implements HierarchicalEdge<TaintVertex> {

    public TaintEdge(TaintVertex src, TaintVertex dst) {
        super(src, dst, EDGE_TYPE.EDGE_REGULAR);
    }
}
