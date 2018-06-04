package org.ucombinator.jaam.visualizer.profiler;

import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

public class ProfilerEdge extends LayoutEdge<ProfilerVertex> implements Edge<ProfilerVertex> {

    public ProfilerEdge(ProfilerVertex parent, ProfilerVertex child) {
        super(parent, child, EDGE_TYPE.EDGE_REGULAR);
    }
}
