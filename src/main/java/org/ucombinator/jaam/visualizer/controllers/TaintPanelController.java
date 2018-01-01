package org.ucombinator.jaam.visualizer.controllers;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.taint.TaintGraph;

import java.io.IOException;

// TODO: Make base PanelController class or interface?
public class TaintPanelController {
    @FXML public final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final ScrollPane scrollPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final Pane taintPanel = null; // Initialized by Controllers.loadFXML()

    private LayoutRootVertex panelRoot;
    private Group graphContentGroup;

    public TaintPanelController(TaintGraph graph) throws IOException {
        Controllers.loadFXML("/TaintPanel.fxml", this);

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.taintPanel.setVisible(true);
        this.taintPanel.getChildren().add(graphContentGroup);

        this.panelRoot = LayerFactory.getLayeredGraph(graph);
        LayoutAlgorithm.layout(this.panelRoot);
        this.drawGraph();
    }

    public void drawGraph() {
        panelRoot.setVisible(false);
        drawNodes(null, panelRoot);
        drawEdges(panelRoot);
        // this.resetStrokeWidth();
        panelRoot.setVisible(true);
    }

    private void drawNodes(GUINode parent, AbstractLayoutVertex v)
    {
        GUINode node = new GUINode(parent, v);

        if (parent == null) {
            graphContentGroup.getChildren().add(node);
        } else {
            parent.getChildren().add(node);
        }

        double translateX = v.getX();
        double translateY = v.getY();
        double width = v.getWidth();
        double height = v.getHeight();
        node.setTranslateLocation(translateX, translateY, width, height);

        HierarchicalGraph<AbstractLayoutVertex> innerGraph = v.getInnerGraph();
        for (AbstractLayoutVertex child : innerGraph.getVisibleVertices()) {
            if (v.isExpanded()) {
                drawNodes(node, child);
            }
        }
    }

    private void drawEdges(AbstractLayoutVertex v)
    {
        if(v.isExpanded()) {
            HierarchicalGraph<AbstractLayoutVertex> innerGraph = v.getInnerGraph();
            for (LayoutEdge e : innerGraph.getVisibleEdges()) {
                e.setVisible(v.isEdgeVisible());
                e.draw();
            }

            for (AbstractLayoutVertex child : innerGraph.getVisibleVertices()) {
                drawEdges(child);
            }
        }
    }
}

