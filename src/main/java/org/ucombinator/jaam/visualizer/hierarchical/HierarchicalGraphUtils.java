package org.ucombinator.jaam.visualizer.hierarchical;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class HierarchicalGraphUtils {

    public static <T extends AbstractLayoutVertex<T>> HierarchicalGraph<T, LayoutEdge<T>> create(T root, boolean isImmutable) {
        HierarchicalGraph<T, LayoutEdge<T>> graph = new HierarchicalGraph<>(root);

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
    public static <T extends AbstractLayoutVertex<T>> void constructVisibleGraph(T root, Predicate<T> isVisible) {
        System.out.println("Constructing visible graph for vertex: " + root);
        HierarchicalGraph<T, LayoutEdge<T>> immutableGraph = root.getImmutableInnerGraph();
        HierarchicalGraph<T, LayoutEdge<T>> visibleGraph = HierarchicalGraphUtils.create(root, false);
        for(T v : immutableGraph.getVertices()) {
            if(isVisible.test(v)) {
                visibleGraph.addVertex(v);
                v.setVisibleSelfGraph(visibleGraph);
                constructVisibleGraph(v, isVisible);
            }
        }

        for(T v : immutableGraph.getVertices()) {
            // TODO: This might be an inefficient way to construct the currently visible graph.
            if(isVisible.test(v)) {
                findVisibleInEdges(immutableGraph, v, visibleGraph);
                findVisibleOutEdges(immutableGraph, v, visibleGraph);
            }
        }
    }

    private static <T extends AbstractLayoutVertex<T>> void findVisibleInEdges(HierarchicalGraph<T, LayoutEdge<T>> immutableGraph,
                                                                               T v, HierarchicalGraph<T, LayoutEdge<T>> visibleGraph) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        queue.addAll(immutableGraph.getInNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            if(!found.contains(w)) {
                found.add(w);
                if (visibleGraph.getVertices().contains(w)) {
                    System.out.println("Adding edge to visible graph: " + w + " --> " + v);
                    visibleGraph.addEdge(new LayoutEdge<>(w, v, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                } else {
                    for (T nextW : immutableGraph.getInNeighbors(w)) {
                        queue.add(nextW);
                    }
                }
            }
        }
    }

    private static <T extends AbstractLayoutVertex<T>> void findVisibleOutEdges(HierarchicalGraph<T, LayoutEdge<T>> immutableGraph,
                                                                               T v, HierarchicalGraph<T, LayoutEdge<T>> visibleGraph) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        queue.addAll(immutableGraph.getOutNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            if(!found.contains(w)) {
                found.add(w);
                if (visibleGraph.getVertices().contains(w)) {
                    System.out.println("Adding edge to visible graph: " + v + " --> " + w);
                    visibleGraph.addEdge(new LayoutEdge<>(v, w, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                } else {
                    for (T nextW : immutableGraph.getOutNeighbors(w)) {
                        queue.add(nextW);
                    }
                }
            }
        }
    }
}
