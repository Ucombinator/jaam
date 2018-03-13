package org.ucombinator.jaam.visualizer.layout;

public class VisibleHierarchicalGraph {
    public static <T extends AbstractLayoutVertex<T>> HierarchicalGraph<T, LayoutEdge<T>> create(T root) {
        HierarchicalGraph<T, LayoutEdge<T>> graph = new HierarchicalGraph<>(root);
        root.setVisibleInnerGraph(graph);
        return graph;
    }
}
