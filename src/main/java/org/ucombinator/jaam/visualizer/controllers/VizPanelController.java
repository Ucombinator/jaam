package org.ucombinator.jaam.visualizer.controllers;

import javafx.collections.SetChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalGraph;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class VizPanelController implements EventHandler<SelectEvent<StateVertex>>, SetChangeListener<StateVertex> {
    @FXML public final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML public final Spinner<Double> zoomSpinner = null; // Initialized by Controllers.loadFXML()

    @FXML private final CheckBox showEdges = null; // Initialized by Controllers.loadFXML()
    @FXML private final CheckBox showLabels = null; // Initialized by Controllers.loadFXML()
    @FXML private final CheckBox groupByClass = null; // Initialized by Controllers.loadFXML()
    @FXML private final ScrollPane scrollPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final Pane vizPanel = null; // Initialized by Controllers.loadFXML()

    // TODO: should this stuff be moved to a model class?
    private Group graphContentGroup;
    private LayoutRootVertex immutableRoot;
    private StateVertex panelRoot;
    private Graph<StateVertex> loopGraph;

    private boolean inBatchMode = false;
    private boolean changedWhileInBatchMode = false;

    public VizPanelController(Graph<StateVertex> graph) throws IOException {
        Controllers.loadFXML("/VizPanel.fxml", this);

        this.zoomSpinner.setValueFactory(new ZoomSpinnerValueFactory(1.0, 1.2));
        TimelineProperty.bind(this.vizPanel.scaleXProperty(), this.zoomSpinner.valueProperty(), 300);
        TimelineProperty.bind(this.vizPanel.scaleYProperty(), this.zoomSpinner.valueProperty(), 300);

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.vizPanel.setVisible(true);
        this.vizPanel.getChildren().add(graphContentGroup);
        //this.requestFocus();

        //Custom event handlers
        graphContentGroup.addEventFilter(SelectEvent.STATE_VERTEX_SELECTED, this);

        this.scrollPane.addEventFilter(ScrollEvent.SCROLL, this::scrollAction);
        this.loopGraph = graph;
        this.immutableRoot = new LayoutRootVertex();
        LayerFactory.getLayeredGraph(this.loopGraph, this.immutableRoot);

        this.panelRoot = new LayoutRootVertex();
        this.drawGraph();
    }

    @FXML private void showEdgesAction(ActionEvent event) {
        this.getPanelRoot().setVisible(false);
        this.getPanelRoot().setEdgeVisibility(showEdges.isSelected());
        LayoutEdge.redrawEdges(this.getPanelRoot(), true);
        this.getPanelRoot().setVisible(true);
    }

    @FXML private void showLabelsAction(ActionEvent event) {
        this.getPanelRoot().setVisible(false);
        this.getPanelRoot().setLabelVisibility(showLabels.isSelected());
        this.getPanelRoot().setVisible(true);
    }

    @FXML private void groupByClassAction(ActionEvent event) {
        /*this.groupedByClass = groupByClass.isSelected();
        this.resetGraphGrouping();*/
    }

    @FXML private void exportImageAction(ActionEvent event) throws IOException {
        event.consume(); // TODO: Is this necessary?
        String extension = "png";
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                extension.toUpperCase() + " files (*." + extension + ")", "*." + extension);
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName(Main.getSelectedMainTab().getText() + "." + extension);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(this.root.getScene().getWindow());

        if (file != null) {
            WritableImage image = vizPanel.snapshot(new SnapshotParameters(), null);

            System.out.println(file.getAbsolutePath());
            // TODO: probably use a file chooser here
            File newFile = new File(file.getAbsolutePath());

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), extension, newFile);
        }
    }

    private void scrollAction(ScrollEvent event) {
        if (event.isControlDown()) {
            // Zoom in or out
            event.consume();
            if (event.getDeltaY() > 0) {
                zoomSpinner.increment();
            } else if (event.getDeltaY() < 0) {
                zoomSpinner.decrement();
            }
        }
    }

    public StateVertex getPanelRoot() {
        return this.panelRoot;
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
        if(vertex instanceof LayoutLoopVertex) {
            currentFrame.setRightText((LayoutLoopVertex) vertex);
        } else if(vertex instanceof LayoutMethodVertex) {
            currentFrame.setRightText((LayoutMethodVertex) vertex);
        } else if(vertex instanceof LayoutSccVertex) {
            currentFrame.setRightText((LayoutSccVertex) vertex);
        }
        else {
            currentFrame.setVizRightText("Text");
        }
    }

    @FXML public void resetGraphGrouping() {
        /*if(this.groupedByClass) {
            LayerFactory.getGraphByClass(this.loopGraph, this.panelRoot);
        } else {
            LayerFactory.getLayeredGraph(this.loopGraph, this.panelRoot);

        LayoutAlgorithm.layout(this.panelRoot);
        this.drawGraph();*/
    }

    public void drawGraph() {
        panelRoot.setVisible(false);
        // TODO: Right now we're not hiding anything at the start, so we just pass an empty set.
        // It would take extra work to be able to access the hidden set at this point. That's because the
        // tab that this is created inside of hasn't been added to the tabPane yet, so calling
        // Main.getSelectedMainTabController() returns null.
        this.panelRoot = this.immutableRoot.getVisibleGraphExcept(new HashSet<>());
        LayoutAlgorithm.layout(this.panelRoot);
        drawNodes(null, panelRoot);
        drawEdges(panelRoot);
        this.resetStrokeWidth();
        panelRoot.setVisible(true);
    }

    public void redrawGraph() {
        panelRoot.setVisible(false);
        this.panelRoot = this.immutableRoot.getVisibleGraphExcept(
                Main.getSelectedMainTabController().getHidden());
        LayoutAlgorithm.layout(this.panelRoot);
        drawNodes(null, panelRoot);
        LayoutEdge.redrawEdges(panelRoot, true);
        this.resetStrokeWidth();
        panelRoot.setVisible(true);
    }

    public void resetAndRedraw() {
        this.graphContentGroup.getChildren().remove(this.panelRoot.getGraphics());
        LayoutAlgorithm.layout(this.panelRoot);
        this.getPanelRoot().setEdgeVisibility(showEdges.isSelected());
        this.redrawGraph();
    }

    public void resetStrokeWidth() {
        this.getPanelRoot().resetStrokeWidth(1.0 / this.zoomSpinner.getValue());
    }

    private void drawNodes(GUINode<StateVertex> parent, StateVertex v)
    {
        System.out.println("Drawing node for vertex: " + v);
        GUINode<StateVertex> node = new GUINode<>(parent, v);

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

        HierarchicalGraph<StateVertex, LayoutEdge<StateVertex>> innerGraph = v.getVisibleInnerGraph();
        for (StateVertex child : innerGraph.getVertices()) {
            if (v.isExpanded()) {
                drawNodes(node, child);
            }
        }
    }

    private void drawEdges(StateVertex v)
    {
        if(v.isExpanded()) {
            HierarchicalGraph<StateVertex, LayoutEdge<StateVertex>> innerGraph = v.getVisibleInnerGraph();
            for (LayoutEdge<StateVertex> e : innerGraph.getEdges()) {
                e.setVisible(v.isEdgeVisible());
                e.draw();
            }

            for (StateVertex child : innerGraph.getVertices()) {
                drawEdges(child);
            }
        }
    }

    public HashSet<StateVertex> pruneVisibleGraph() {
        return this.panelRoot.getVisibleInnerGraph().getVerticesToPrune(AbstractLayoutVertex.VertexType.METHOD);
    }

    @Override
    public void onChanged(Change<? extends StateVertex> change) {
        System.out.println("JUAN: Hidden changed: " + change);
        if(change.wasAdded()) {
            StateVertex v = change.getElementAdded();
            v.setHighlighted(false);
            v.setHidden();
            this.redrawGraph();
        } else {
            StateVertex v = change.getElementRemoved();
            v.setUnhidden();

            if(!inBatchMode) {
                this.resetAndRedraw();
            } else {
                changedWhileInBatchMode = true;
            }
        }
    }

    public void startBatchMode()
    {
        inBatchMode = true;
        changedWhileInBatchMode = false;
    }

    public void endBatchMode() {
        inBatchMode = false;
        if(changedWhileInBatchMode) {
            this.resetAndRedraw();
        }
    }
}
