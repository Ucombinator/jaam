package org.ucombinator.jaam.visualizer.controllers;

import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.state.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class VizPanelController extends GraphPanelController<StateVertex, StateEdge>
        implements EventHandler<SelectEvent<StateVertex>>, SetChangeListener<StateVertex> {

    GraphTransform<StateRootVertex, StateVertex> immToVis;

    public VizPanelController(Graph<StateVertex, StateEdge> graph) throws IOException {
        super(StateRootVertex::new);

        // Custom event handlers
        this.graphContentGroup.addEventFilter(SelectEvent.STATE_VERTEX_SELECTED, this);

        this.visibleRoot = new StateRootVertex();
        this.immutableRoot = LayerFactory.getLayeredLoopGraph(graph);
        this.drawGraph(new HashSet<>());
    }

    @Override
    public void redrawGraphAction(ActionEvent event) throws IOException {
        event.consume();
        this.redrawGraph(Main.getSelectedMainTabController().getHidden());
    }
    @Override
    public void hideUnrelatedAction(ActionEvent event) throws IOException {
        event.consume();
        Main.getSelectedMainTabController().hideUnrelatedToHighlighted();
    }

    // Handles select events
    @Override
    public void handle(SelectEvent<StateVertex> event) {
        StateVertex vertex = event.getVertex();

        if (vertex.getType() == AbstractLayoutVertex.VertexType.ROOT) {
            event.consume();
            return;
        }

        System.out.println("Received event from vertex " + vertex.toString());

        MainTabController currentFrame = Main.getSelectedMainTabController();
        if(vertex instanceof StateLoopVertex) {
            currentFrame.setRightText((StateLoopVertex) vertex);
        } else if(vertex instanceof StateMethodVertex) {
            currentFrame.setRightText((StateMethodVertex) vertex);
        } else if(vertex instanceof StateSccVertex) {
            currentFrame.setRightText((StateSccVertex) vertex);
        }
        else {
            currentFrame.setVizRightText("Text");
        }
    }

    public void drawGraph(Set<StateVertex> hidden) {
        visibleRoot.setVisible(false);


        immToVis = ((StateRootVertex) this.immutableRoot).constructVisibleGraphExcept(hidden);

        this.visibleRoot = immToVis.newRoot;

        LayoutAlgorithm.layout(this.visibleRoot);
        drawNodes(null, visibleRoot);
        drawEdges(visibleRoot);
        this.resetStrokeWidth();
        visibleRoot.setVisible(true);
    }

    public void redrawGraph(Set<StateVertex> hidden) {
        System.out.println("Redrawing loop graph...");
        this.graphContentGroup.getChildren().remove(this.visibleRoot.getGraphics());
        this.drawGraph(hidden);
    }

    public void resetStrokeWidth() {
        this.visibleRoot.applyToVerticesRecursive(
                (HierarchicalVertex<StateVertex, StateEdge> w) -> ((AbstractLayoutVertex<StateVertex>) w)
                        .resetStrokeWidth(1.0 / this.zoomSpinner.getValue()));
    }

    public HashSet<StateVertex> pruneVisibleGraph() {
        return this.visibleRoot.getInnerGraph().getVerticesToPrune(v -> (v.getType() == AbstractLayoutVertex.VertexType.METHOD));
    }

    // Changes to the hidden set
    @Override
    public void onChanged(Change<? extends StateVertex> change) {
        if (change.wasAdded()) {
            StateVertex v = change.getElementAdded();
            v.setHighlighted(false);
            v.setHidden();
        } else {
            StateVertex v = change.getElementRemoved();
            v.setUnhidden();
        }
    }
}
