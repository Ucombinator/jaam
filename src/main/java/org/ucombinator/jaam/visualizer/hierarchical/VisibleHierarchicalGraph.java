package org.ucombinator.jaam.visualizer.hierarchical;

import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalGraph;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

public class VisibleHierarchicalGraph {
    public static <T extends AbstractLayoutVertex<T>> HierarchicalGraph<T, LayoutEdge<T>> create(T root) {
        HierarchicalGraph<T, LayoutEdge<T>> graph = new HierarchicalGraph<>(root);
        root.setVisibleInnerGraph(graph);
        return graph;
    }
}
