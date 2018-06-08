package org.ucombinator.jaam.visualizer.layout;

public interface LayoutEdge<T extends AbstractLayoutVertex<T>> {

    T getSrc();
    void redrawEdge();
    void setEdgeHighlight();
    void setEdgeDefaultStyle();
}
