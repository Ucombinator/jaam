package org.ucombinator.jaam.visualizer.state;

import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.layout.HierarchicalEdge;

public class StateEdge extends HierarchicalEdge<StateVertex> implements Edge<StateVertex> {

    public StateEdge(StateVertex src, StateVertex dst) {
        super(src, dst, EDGE_TYPE.EDGE_REGULAR);
    }
}
