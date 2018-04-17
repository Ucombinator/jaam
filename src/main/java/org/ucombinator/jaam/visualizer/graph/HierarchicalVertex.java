package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
import org.ucombinator.jaam.visualizer.state.StateRootVertex;
import org.ucombinator.jaam.visualizer.taint.TaintRootVertex;

import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

public interface HierarchicalVertex<T extends HierarchicalVertex<T, S>, S extends Edge<T>> extends Vertex, Comparable<T> {

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

    // TODO: These functions only look at vertices on the same level.
    default Set<T> getAncestors()
    {
        HashSet<T> ancestors = new HashSet<>();
        this.getAncestors(ancestors);

        return ancestors;
    }

    default void getAncestors(HashSet<T> ancestors)
    {
        if(this instanceof StateRootVertex || this instanceof TaintRootVertex)
            return;

        if (!ancestors.contains((T) this)) {
            ancestors.add((T) this);
            this.getParentGraph().getInNeighbors((T) this).forEach(v -> {
                v.getAncestors(ancestors);
            });
        }
    }

    default Set<T> getDescendants()
    {
        HashSet<T> descendants = new HashSet<>();
        this.getDescendants(descendants);

        return descendants;
    }

    default void getDescendants(HashSet<T> descendants)
    {
        if(this instanceof StateRootVertex || this instanceof TaintRootVertex)
            return;

        if (!descendants.contains((T) this)) {
            descendants.add((T) this);
            this.getParentGraph().getOutNeighbors((T) this).forEach(v -> {
                v.getDescendants(descendants);
            });
        }
    }

    default LayoutAlgorithm.LAYOUT_ALGORITHM getPreferredLayout() {
        return LayoutAlgorithm.LAYOUT_ALGORITHM.DFS;
    }
}
