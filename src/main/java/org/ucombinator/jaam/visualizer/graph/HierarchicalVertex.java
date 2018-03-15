package org.ucombinator.jaam.visualizer.graph;

import java.util.function.*;

public interface HierarchicalVertex<T extends HierarchicalVertex<T, S>, S extends Edge<T>> extends AbstractVertex, Comparable<T> {

    Graph<T, S> getParentGraph();
    Graph<T, S> getChildGraph();
    void setParentGraph(Graph<T, S> graph);
    T copy(); // Copy constructor, used to construct new vertices in visible graph.

    default void applyToVerticesRecursive(Consumer<HierarchicalVertex<T, S>> f) {
        f.accept(this);
        this.getChildGraph().getVertices().forEach(w -> w.applyToVerticesRecursive(f));
    }

    default void applyToEdgesRecursive(Consumer<HierarchicalVertex<T, S>> vertexFunc, Consumer<S> edgeFunc) {
        vertexFunc.accept(this);
        for(S edge: this.getChildGraph().getEdges()) {
            edgeFunc.accept(edge);
        }

        this.getChildGraph().getVertices().forEach(w -> w.applyToEdgesRecursive(vertexFunc, edgeFunc));
    }

    // Overridden in base case, LayoutInstructionVertex
    default int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(HierarchicalVertex<T, S> v : this.getChildGraph().getVertices()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    @Override
    default int compareTo(T v) {
        return Integer.compare(this.getMinInstructionLine(), v.getMinInstructionLine());
    }

}
