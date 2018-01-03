package org.ucombinator.jaam.visualizer.controllers;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.taint.*;

import java.io.IOException;
import java.util.HashSet;

// TODO: Make base PanelController class or interface?
public class TaintPanelController {
    @FXML public final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final ScrollPane scrollPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final Pane taintPanel = null; // Initialized by Controllers.loadFXML()

    private TaintRootVertex panelRoot;
    private Group graphContentGroup;

    public TaintPanelController(TaintGraph graph) throws IOException {
        Controllers.loadFXML("/TaintPanel.fxml", this);

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.taintPanel.setVisible(true);
        this.taintPanel.getChildren().add(graphContentGroup);

        this.panelRoot = new TaintRootVertex();
        LayerFactory.getLayeredGraph(graph, this.panelRoot);
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

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.VERTEX_SELECTED, onVertexSelect);
    }

    // Draw the graph of taint addresses for the selected node, and addresses connected to them.
    EventHandler<SelectEvent<StateVertex>> onVertexSelect = new EventHandler<SelectEvent<StateVertex>>() {
        @Override
        public void handle(SelectEvent<StateVertex> selectEvent) {

            StateVertex v = selectEvent.getVertex();
            HashSet<TaintVertex> methodAddresses = findAddressesByMethods(v.getMethodNames());
            HashSet<TaintVertex> verticesToDraw = findConnectedAddresses(methodAddresses);
            System.out.println("Taint vertices to draw: " + verticesToDraw.size());
            // TODO: Redraw graph with only this set of vertices.
        }
    };

    public HashSet<TaintVertex> findAddressesByMethods(HashSet<String> methodNames) {
        HashSet<TaintVertex> results = new HashSet<>();
        this.panelRoot.searchByMethodNames(methodNames, results);
        return results;
    }

    public HashSet<TaintVertex> findConnectedAddresses(HashSet<TaintVertex> taintVertices) {
        HashSet<TaintVertex> results = (HashSet<TaintVertex>) (taintVertices.clone());
        HashSet<TaintVertex> toSearch = (HashSet<TaintVertex>) (taintVertices.clone());

        // Search upwards
        while(toSearch.size() > 0) {
            HashSet<TaintVertex> newSearch = new HashSet<>();
            for (TaintVertex v : toSearch) {
                results.add(v);
                HierarchicalGraph<TaintVertex> selfGraph = v.getSelfGraph();
                for (TaintVertex vIn : selfGraph.getVisibleInNeighbors(v)) {
                    if (!results.contains(vIn)) {
                        newSearch.add(vIn);
                    }
                }
            }
            toSearch = newSearch;
        }

        // Search downwards
        toSearch = (HashSet<TaintVertex>) (taintVertices.clone());
        while(toSearch.size() > 0) {
            HashSet<TaintVertex> newSearch = new HashSet<>();
            for (TaintVertex v : toSearch) {
                results.add(v);
                HierarchicalGraph<TaintVertex> selfGraph = v.getSelfGraph();
                for (TaintVertex vOut : selfGraph.getVisibleOutNeighbors(v)) {
                    if (!results.contains(vOut)) {
                        newSearch.add(vOut);
                    }
                }
            }
            toSearch = newSearch;
        }
        return results;
    }
}
