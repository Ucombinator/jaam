package org.ucombinator.jaam.visualizer.hierarchical;

import javafx.scene.control.TreeItem;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.function.Consumer;

public interface HierarchicalVertex<T extends HierarchicalVertex<T, S>,
        S extends HierarchicalEdge<T>> extends Comparable<T> {
    String getLabel();
    HierarchicalGraph<T, S> getVisibleSelfGraph();
    HierarchicalGraph<T, S> getVisibleInnerGraph();
    HierarchicalGraph<T, S> getImmutableSelfGraph();
    HierarchicalGraph<T, S> getImmutableInnerGraph();

    void setVisibleSelfGraph(HierarchicalGraph<T, S> graph);
    void setVisibleInnerGraph(HierarchicalGraph<T, S> graph);
    void setImmutableSelfGraph(HierarchicalGraph<T, S> graph);
    void setImmutableInnerGraph(HierarchicalGraph<T, S> graph);

    default boolean isVisibleInnerGraphEmpty() {
        return this.getVisibleInnerGraph().getVertices().isEmpty();
    }

    static <T extends HierarchicalVertex<T, S>, S extends HierarchicalEdge<T>>
    void applyToVisibleRec(HierarchicalVertex<T, S> v, Consumer<HierarchicalVertex<T, S>> f) {
        f.accept(v);
        v.getVisibleInnerGraph().getVertices().forEach(w -> HierarchicalVertex.applyToVisibleRec(w, f));
    }

    static <T extends HierarchicalVertex<T, S>, S extends HierarchicalEdge<T>>
    void applyToImmutableRec(HierarchicalVertex<T, S> v, Consumer<HierarchicalVertex<T, S>> f) {
        f.accept(v);
        v.getImmutableInnerGraph().getVertices().forEach(w -> HierarchicalVertex.applyToImmutableRec(w, f));
    }

    static <T extends HierarchicalVertex<T, S>, S extends HierarchicalEdge<T>>
    void applyToVisibleEdgesRec(HierarchicalVertex<T, S> v, Consumer<HierarchicalVertex<T, S>> fVertex, Consumer<S> fEdge) {
        fVertex.accept(v);
        for(S edge: v.getVisibleInnerGraph().getEdges()) {
            fEdge.accept(edge);
        }

        v.getVisibleInnerGraph().getVertices().forEach(w -> HierarchicalVertex.applyToVisibleEdgesRec(w, fVertex, fEdge));
    }

    // Override in base case, LayoutInstructionVertex
    default int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(HierarchicalVertex v : this.getImmutableInnerGraph().getVertices()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    default int compareTo(T v) {
        return Integer.compare(this.getMinInstructionLine(), v.getMinInstructionLine());
    }

    /*default boolean addTreeNodes(TreeItem<T> parentNode, MainTabController mainTab) {
        boolean addedNodes = false;
        TreeItem<T> newNode = new TreeItem<>((T)this);
        for (T v : this.getVisibleInnerGraph().getVertices()) { // TODO: Is this the right one?
            addedNodes |= v.addTreeNodes(newNode, mainTab);
        }

        if(mainTab.getVizHighlighted().contains(this) || addedNodes) {
            parentNode.getChildren().add(newNode);
            return true;
        } else {
            return false;
        }
    }*/
}
