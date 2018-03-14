package org.ucombinator.jaam.visualizer.hierarchical;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class HierarchicalGraphUtils {

    public static <T extends HierarchicalVertex<T, S>, S extends HierarchicalEdge<T>>
    HierarchicalGraph<T, S> create(T root, boolean isImmutable) {
        HierarchicalGraph<T, S> graph = new HierarchicalGraph<>(root);

        if(isImmutable) {
            root.setImmutableInnerGraph(graph);
        }
        else {
            root.setVisibleInnerGraph(graph);
        }

        return graph;
    }

    // The immutable graph for our root has already been set, so now we construct its visible graph of vertices
    // matching our predicate.
    public static <T extends HierarchicalVertex<T, S>, S extends HierarchicalEdge<T>>
    void constructVisibleGraph(T root, Predicate<T> isVisible, BiFunction<T, T, S> edgeBuilder) {
        System.out.println("Constructing visible graph for vertex: " + root);
        HierarchicalGraph<T, S> immutableGraph = root.getImmutableInnerGraph();
        HierarchicalGraph<T, S> visibleGraph = HierarchicalGraphUtils.create(root, false);

        // Add all visible vertices
        for(T v : immutableGraph.getVertices()) {
            if(isVisible.test(v)) {
                visibleGraph.addVertex(v);
                v.setVisibleSelfGraph(visibleGraph);
                constructVisibleGraph(v, isVisible, edgeBuilder);
            }
        }

        // For each vertex, search for its visible incoming edges
        for(T v : immutableGraph.getVertices()) {
            // TODO: This might be an inefficient way to construct the currently visible graph.
            if(isVisible.test(v)) {
                findVisibleEdges(immutableGraph, v, visibleGraph, edgeBuilder);
            }
        }
    }

    private static <T extends HierarchicalVertex<T, S>, S extends HierarchicalEdge<T>> void findVisibleEdges(
            HierarchicalGraph<T, S> immutableGraph, T v,
            HierarchicalGraph<T, S> visibleGraph, BiFunction<T, T, S> edgeBuilder) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        queue.addAll(immutableGraph.getInNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            if(!found.contains(w)) {
                found.add(w);
                if (visibleGraph.getVertices().contains(w)) {
                    System.out.println("Adding edge to visible graph: " + w + " --> " + v);
                    visibleGraph.addEdge(edgeBuilder.apply(w, v));
                } else {
                    queue.addAll(immutableGraph.getInNeighbors(w));
                }
            }
        }
    }
}
