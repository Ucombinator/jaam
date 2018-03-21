package org.ucombinator.jaam.visualizer.controllers;

import javafx.collections.SetChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
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



    private int batchModeCount = 0;
    private boolean changedWhileInBatchMode = false;

    public VizPanelController(Graph<StateVertex, StateEdge> graph) throws IOException {
        super(StateRootVertex::new);

        //Custom event handlers
        this.graphContentGroup.addEventFilter(SelectEvent.STATE_VERTEX_SELECTED, this);


        this.visibleRoot = new StateRootVertex();
        this.immutableRoot = LayerFactory.getLayeredGraph(graph);
        this.drawGraph(new HashSet<>());
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
        this.visibleRoot = ((StateRootVertex) this.immutableRoot).constructVisibleGraphExcept(hidden);

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
        return this.visibleRoot.getChildGraph().getVerticesToPrune(v -> (v.getType() == AbstractLayoutVertex.VertexType.METHOD));
    }

    @Override
    public void onChanged(Change<? extends StateVertex> change) {
        System.out.println("JUAN: Hidden changed: " + change);
        if (change.wasAdded()) {
            StateVertex v = change.getElementAdded();
            v.setHighlighted(false);
            v.setHidden();
        } else {
            StateVertex v = change.getElementRemoved();
            v.setUnhidden();
        }

        if (!inBatchMode()) {
            this.redrawGraph(Main.getSelectedMainTabController().getHidden());
        } else {
            System.out.println("Waiting to redraw batch...");
            changedWhileInBatchMode = true;
        }
    }

    public void startBatchMode() {
        ++batchModeCount;
        changedWhileInBatchMode = false;
    }

    public void endBatchMode() {
        if (!inBatchMode()) {
            System.out.println("ERROR: Not in batch mode, but tried to leave anyway");
        }
        else {
            --batchModeCount;
        }
        if (!inBatchMode() && changedWhileInBatchMode) {
            this.redrawGraph(Main.getSelectedMainTabController().getHidden());
        }
    }

    private boolean inBatchMode() {
        return batchModeCount > 0;
    }
}
