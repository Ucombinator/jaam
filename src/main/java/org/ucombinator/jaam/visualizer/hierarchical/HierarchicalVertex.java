package org.ucombinator.jaam.visualizer.hierarchical;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

public interface HierarchicalVertex<T> extends Comparable<T> {
    // TODO: remove once ImmutableHierarchicalGraph.constructVisibleGraph takes a predicate
    AbstractLayoutVertex.VertexType getType();
    String getLabel();
}
