package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.graph.Edge;

public class StateEdge extends LayoutEdge<StateVertex> implements Edge<StateVertex> {

    public StateEdge(StateVertex src, StateVertex dst) {
        super(src, dst, EDGE_TYPE.EDGE_REGULAR);
    }
}
