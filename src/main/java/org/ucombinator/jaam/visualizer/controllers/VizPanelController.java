package org.ucombinator.jaam.visualizer.controllers;

import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.state.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class VizPanelController extends GraphPanelController<StateVertex, StateEdge>
        implements EventHandler<SelectEvent<StateVertex>>, SetChangeListener<StateVertex> {

    public GraphTransform<StateRootVertex, StateVertex> immAndVis;

    public VizPanelController(Graph<StateVertex, StateEdge> graph) throws IOException {
        super(StateRootVertex::new);

        // Custom event handlers
        this.graphContentGroup.addEventFilter(SelectEvent.STATE_VERTEX_SELECTED, this);

        this.visibleRoot = new StateRootVertex();
        this.immutableRoot = new StateRootVertex();
        this.immutableRoot.setInnerGraph(graph);
        setAllImmutable(this.immutableRoot);
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

        System.out.println("JUAN: Immutable graph #Edges: " + this.getImmutableRoot().getInnerGraph().getEdges().size());
        GraphUtils.printGraph(this.getImmutableRoot(), 0);

        if (!hidden.isEmpty()) {
            GraphTransform<StateRootVertex, StateVertex> hiddenTransform = getImmutableRoot().constructVisibleGraphExcept(hidden);
            GraphTransform<StateRootVertex, StateVertex> compact = LayerFactory.getLayeredLoopGraph(hiddenTransform.newRoot);

            immAndVis = GraphTransform.transfer(hiddenTransform, compact);
        }
        else { // Work directly on immutable graph
            this.immAndVis = LayerFactory.getLayeredLoopGraph(getImmutableRoot());
        }
        this.visibleRoot = immAndVis.newRoot;

        System.out.println("JUAN: Print visible graph #Edges: " + this.getImmutableRoot().getInnerGraph().getEdges().size());
        GraphUtils.printGraph(this.visibleRoot, 0);

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
            StateVertex immV = change.getElementAdded();

            checkImmutable(immV);
            //immV.setHighlighted(false);
            immV.setHidden();

            //immAndVis.getNew(immV).setHighlighted(false);
            immAndVis.getNew(immV).setHidden();
        } else {
            StateVertex immV = change.getElementRemoved();
            checkImmutable(immV);
            immV.setUnhidden();
        }
    }

    public StateRootVertex getImmutableRoot() {
        return (StateRootVertex) this.immutableRoot;
    }

    public void setClassHighlight(HashSet<StateVertex> vertices, boolean value) {
        for (StateVertex immV : vertices) {
            checkImmutable(immV);
            StateVertex v = immAndVis.getNew(immV);
            if (!v.isHidden()) {
                v.setClassHighlight(value);
            }
        }
    }

    // Selection are **visible** nodes
    // Returns the immutable nodes related to the selection of visible nodes
    public HashSet<StateVertex> getUnrelatedVisible(HashSet<StateVertex> selection) {

        HashSet<StateVertex> keep = new HashSet<>();

        Graph<StateVertex, StateEdge> topLevel = getVisibleRoot().getInnerGraph();

        selection.forEach(v -> keep.addAll(v.getAncestors()));
        selection.forEach(v -> keep.addAll(v.getDescendants()));

        HashSet<StateVertex> toHide = new HashSet<>();
        topLevel.getVertices().forEach(v -> {
            if (!keep.contains(v) && !keepAnyInterior(v, keep, toHide)) {
                toHide.add(v);
            }
        });

        return toHide;
    }

    private boolean keepAnyInterior(StateVertex root, HashSet<StateVertex> keep, HashSet<StateVertex> toHide) {

        boolean foundAVertexToKeep = false;
        for (StateVertex v : root.getInnerGraph().getVertices()) {
            if (keep.contains(v) || keepAnyInterior(v, keep, toHide)) {
                foundAVertexToKeep = true;
            }
            else {
                toHide.add(v);
            }
        }

        return foundAVertexToKeep;
    }

    public HashSet<StateVertex> getImmutable(HashSet<StateVertex> visible) {
        return visible.stream().
                filter(immAndVis::containsNew).
                map(v -> { return immAndVis.getOld(v);}).collect(Collectors.toCollection(HashSet::new));
    }

    private void setAllImmutable(StateVertex v) {
        v.isImmutable = true;

        for (StateVertex i : v.getInnerGraph().getVertices()) {
            setAllImmutable(i);
        }
    }

    private void checkImmutable(StateVertex v) {
        if (!v.isImmutable)
            throw new Error(v + " is not immutable");
    }

}
