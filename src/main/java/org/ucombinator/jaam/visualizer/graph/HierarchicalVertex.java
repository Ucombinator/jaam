package org.ucombinator.jaam.visualizer.graph;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface HierarchicalVertex<T extends HierarchicalVertex<T, S>, S extends Edge<T>> extends AbstractVertex, Comparable<T> {

    Graph<T, S> getSelfGraph();
    Graph<T, S> getInnerGraph();
    void setSelfGraph(Graph<T, S> graph);
    T copy(); // Copy constructor, used to construct new vertices in visible graph.

    // The immutable graph for our root has already been set, so now we construct its visible graph of vertices
    // matching our predicate.
    default T constructVisibleGraph(Predicate<T> isVisible, BiFunction<T, T, S> edgeBuilder) {
        System.out.println("Constructing visible graph for HierarchicalVertex: " + this);
        Graph<T, S> immutableGraph = this.getInnerGraph();
        T visibleRoot = this.copy();
        Graph<T, S> visibleGraph = visibleRoot.getInnerGraph();

        // Add all visible vertices
        for(T v : immutableGraph.getVertices()) {
            if(isVisible.test(v)) {
                visibleGraph.addVertex(v);
                v.setSelfGraph(visibleGraph);
                v.constructVisibleGraph(isVisible, edgeBuilder);
            }
        }

        // For each HierarchicalVertex, search for its visible incoming edges
        for(T v : immutableGraph.getVertices()) {
            // TODO: This might be an inefficient way to construct the currently visible graph.
            if(isVisible.test(v)) {
                this.findVisibleEdges(v, visibleGraph, edgeBuilder);
            }
        }

        return visibleRoot;
    }

    default void findVisibleEdges(T v, Graph<T, S> visibleGraph, BiFunction<T, T, S> edgeBuilder) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        Graph<T, S> immutableGraph = this.getInnerGraph();
        queue.addAll(immutableGraph.getInNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            if (!found.contains(w)) {
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

    default void applyToVerticesRecursive(Consumer<HierarchicalVertex<T, S>> f) {
        f.accept(this);
        this.getInnerGraph().getVertices().forEach(w -> w.applyToVerticesRecursive(f));
    }

    default void applyToEdgesRecursive(Consumer<HierarchicalVertex<T, S>> vertexFunc, Consumer<S> edgeFunc) {
        vertexFunc.accept(this);
        for(S edge: this.getInnerGraph().getEdges()) {
            edgeFunc.accept(edge);
        }

        this.getInnerGraph().getVertices().forEach(w -> w.applyToEdgesRecursive(vertexFunc, edgeFunc));
    }

    // Overridden in base case, LayoutInstructionVertex
    default int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(HierarchicalVertex<T, S> v : this.getInnerGraph().getVertices()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    @Override
    default int compareTo(T v) {
        return Integer.compare(this.getMinInstructionLine(), v.getMinInstructionLine());
    }

}
