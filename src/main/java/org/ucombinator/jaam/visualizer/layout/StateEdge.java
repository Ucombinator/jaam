package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalEdge;

public class StateEdge extends LayoutEdge<StateVertex> implements HierarchicalEdge<StateVertex> {

    public StateEdge(StateVertex src, StateVertex dst) {
        super(src, dst, EDGE_TYPE.EDGE_REGULAR);
    }
}
