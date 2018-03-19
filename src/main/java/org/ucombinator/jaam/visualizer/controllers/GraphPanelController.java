package org.ucombinator.jaam.visualizer.controllers;

import javafx.scene.Group;
import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

public interface GraphPanelController<T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends LayoutEdge<T> & Edge<T>> {

    Group getGraphContentGroup();
    T getVisibleRoot();
    T getImmutableRoot();

    default void drawNodes(GUINode<T> parent, T v) {
        GUINode<T> node = new GUINode<>(parent, v);

        if (parent == null) {
            this.getGraphContentGroup().getChildren().add(node);
        } else {
            parent.getChildren().add(node);
        }

        double translateX = v.getX();
        double translateY = v.getY();
        double width = v.getWidth();
        double height = v.getHeight();
        node.setTranslateLocation(translateX, translateY, width, height);

        Graph<T, S> childGraph = v.getChildGraph();
        for (T child : childGraph.getVertices()) {
            if (v.isExpanded()) {
                drawNodes(node, child);
            }
        }
    }

    default void drawEdges(T v) {
        if (v.isExpanded()) {
            Graph<T, S> childGraph = v.getChildGraph();
            for (S e : childGraph.getEdges()) {
                e.setVisible(v.isEdgeVisible());
                e.draw();
            }

            for (T child : childGraph.getVertices()) {
                drawEdges(child);
            }
        }
    }
}
