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
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.taint.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

// TODO: Make base PanelController class or interface?
public class TaintPanelController implements EventHandler<SelectEvent<TaintVertex>> {
    @FXML public final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final ScrollPane scrollPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final Pane taintPanel = null; // Initialized by Controllers.loadFXML()

    private TaintRootVertex panelRoot;
    private Group graphContentGroup;

    private HashMap<String, TaintAddress> fieldVertices;

    public TaintPanelController(TaintGraph graph) throws IOException {
        Controllers.loadFXML("/TaintPanel.fxml", this);

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.taintPanel.setVisible(true);
        this.taintPanel.getChildren().add(graphContentGroup);
        graphContentGroup.addEventFilter(SelectEvent.TAINT_VERTEX_SELECTED, this);

        this.panelRoot = new TaintRootVertex();

        // Set up graph, but avoid drawing the entire thing
        LayerFactory.getLayeredGraph(graph, this.panelRoot);
        /*LayoutAlgorithm.layout(this.panelRoot);
        this.drawGraph();*/
        fillFieldDictionary();
    }

    public void drawGraph() {
        panelRoot.setVisible(false);
        drawNodes(null, panelRoot);
        drawEdges(panelRoot);
        // this.resetStrokeWidth();
        panelRoot.setVisible(true);
    }

    private void drawNodes(GUINode<TaintVertex> parent, TaintVertex v)
    {
        GUINode<TaintVertex> node = new GUINode<>(parent, v);

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

        HierarchicalGraph<TaintVertex> innerGraph = v.getInnerGraph();
        for (TaintVertex child : innerGraph.getVisibleVertices()) {
            if (v.isExpanded()) {
                drawNodes(node, child);
            }
        }
    }

    private void drawEdges(TaintVertex v)
    {
        if(v.isExpanded()) {
            HierarchicalGraph<TaintVertex> innerGraph = v.getInnerGraph();
            for (LayoutEdge<TaintVertex> e : innerGraph.getVisibleEdges()) {
                e.setVisible(v.isEdgeVisible());
                e.draw();
            }

            for (TaintVertex child : innerGraph.getVisibleVertices()) {
                drawEdges(child);
            }
        }
    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.STATE_VERTEX_SELECTED, onVertexSelect);
    }

    @Override
    public void handle(SelectEvent<TaintVertex> event) {
        TaintVertex vertex = event.getVertex();

        if (vertex.getType() == AbstractLayoutVertex.VertexType.ROOT) {
            System.out.println("Ignoring click on vertex root.");
            event.consume();
            return;
        }

        System.out.println("Received event from vertex " + vertex.toString());

        MainTabController currentFrame = Main.getSelectedMainTabController();
        //currentFrame.resetHighlighted(vertex);

        if(vertex instanceof TaintAddress) {
            currentFrame.setRightText((TaintAddress) vertex);
        }
        else if(vertex instanceof TaintSccVertex)
        {
            currentFrame.setRightText((TaintSccVertex) vertex);
        }
        else if(vertex instanceof TaintStmtVertex) {
            currentFrame.setRightText((TaintStmtVertex) vertex);
        }
        else {
            //currentFrame.bytecodeArea.setDescription();
            currentFrame.setTaintRightText("Text");
        }
    }

    // Draw the graph of taint addresses for the selected node, and addresses connected to them.
    EventHandler<SelectEvent<StateVertex>> onVertexSelect = new EventHandler<SelectEvent<StateVertex>>() {
        @Override
        public void handle(SelectEvent<StateVertex> selectEvent) {

            long time1 = System.nanoTime();
            StateVertex v = selectEvent.getVertex();
            HashSet<TaintVertex> methodAddresses = findAddressesByMethods(v.getMethodNames());
            HashSet<TaintVertex> verticesToDraw = findConnectedAddresses(methodAddresses);
            verticesToDraw.add(panelRoot);
            System.out.println("Taint vertices in method: " + methodAddresses.size());
            System.out.println("Taint vertices to draw: " + verticesToDraw.size());
            drawConnectedVertices(methodAddresses);
        }
    };

    private void drawConnectedVertices(HashSet<TaintVertex> addresses) {
        long time1 = System.nanoTime();
        HashSet<TaintVertex> verticesToDraw = findConnectedAddresses(addresses);
        verticesToDraw.add(panelRoot);
        System.out.println("Taint vertices to draw: " + verticesToDraw.size());

        long time2 = System.nanoTime();
        // Redraw graph with only this set of vertices.
        panelRoot.getInnerGraph().setGraphUnhidden(true);
        panelRoot.setHiddenExcept(verticesToDraw);
        LayoutAlgorithm.layout(panelRoot);
        TaintPanelController.this.drawGraph();
        long time3 = System.nanoTime();

        System.out.println("Time to compute connected vertices: " + (time2 - time1) / 1000000000.0);
        System.out.println("Time to draw graph: " + (time3 - time2) / 1000000000.0);
    }

    public HashSet<TaintVertex> findAddressesByMethods(HashSet<String> methodNames) {
        HashSet<TaintVertex> results = new HashSet<>();
        this.panelRoot.searchByMethodNames(methodNames, results); // TODO: This step is a little inefficient.
        return results;
    }

    public HashSet<TaintVertex> findConnectedAddresses(HashSet<TaintVertex> taintVertices) {
        HashSet<TaintVertex> upResults = new HashSet<>();
        HashSet<TaintVertex> downResults = new HashSet<>();

        // Search upwards
        HashSet<TaintVertex> toSearch = (HashSet<TaintVertex>) (taintVertices.clone());
        while (toSearch.size() > 0) {
            HashSet<TaintVertex> newSearch = new HashSet<>();
            for (TaintVertex v : toSearch) {
                upResults.add(v);
                HierarchicalGraph<TaintVertex> selfGraph = v.getSelfGraph();
                for (TaintVertex vIn : selfGraph.getInNeighbors(v)) {
                    if (!upResults.contains(vIn)) {
                        newSearch.add(vIn);
                    }
                }
            }
            toSearch = newSearch;
        }

        // Search downwards
        toSearch = (HashSet<TaintVertex>) (taintVertices.clone());
        while (toSearch.size() > 0) {
            HashSet<TaintVertex> newSearch = new HashSet<>();
            for (TaintVertex v : toSearch) {
                downResults.add(v);
                HierarchicalGraph<TaintVertex> selfGraph = v.getSelfGraph();
                for (TaintVertex vOut : selfGraph.getOutNeighbors(v)) {
                    if (!downResults.contains(vOut)) {
                        newSearch.add(vOut);
                    }
                }
            }
            toSearch = newSearch;
        }

        HashSet<TaintVertex> allResults = new HashSet<>();
        allResults.addAll(taintVertices);
        allResults.addAll(upResults);
        allResults.addAll(downResults);

        // TODO: Set colors for results
        // Do we want to use colors to distinguish statements from addresses,
        // and also to distinguish positions in the graph?
        /*for (TaintVertex v : allResults) {
            if(taintVertices.contains(v)) {
                v.setDefaultColor();
            }
            else if(upResults.contains(v)) {
                if(downResults.contains(v)) {
                    v.setColor(TaintVertex.bothDirColor);
                }
            }
        } */

        return allResults;
    }

    public void showFieldTaintGraph(String fullClassName, String fieldName) {

        String fieldId = fullClassName + ":" + fieldName;

        TaintAddress a = fieldVertices.get(fieldId);

        if (a != null) {
            HashSet<TaintVertex> vertices = new HashSet<>();
            vertices.add(a);
            drawConnectedVertices(vertices);
        }
        else
        {
            System.out.println("\tWarning: Did not find taint vertex " + fieldId);
        }
    }

    public void fillFieldDictionary()
    {
        fieldVertices = new HashMap<>();

        ArrayList<TaintAddress> allFields = new ArrayList<>();

        this.panelRoot.getFields(allFields);

        allFields.stream().forEach(v -> {
            fieldVertices.put(v.getFieldId(), v);
        });
    }
}
