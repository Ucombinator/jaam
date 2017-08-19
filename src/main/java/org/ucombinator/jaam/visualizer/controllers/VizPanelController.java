package org.ucombinator.jaam.visualizer.controllers;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.TimelineProperty;
import org.ucombinator.jaam.visualizer.gui.ZoomSpinnerValueFactory;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class VizPanelController {
    @FXML private Node root;
    public Node getRoot() { return this.root; }

    @FXML private Pane vizPanel;
    public Pane getVizPanel() { return this.vizPanel; }

    @FXML private CheckBox showEdges;
    @FXML private CheckBox showLabels;
    @FXML private CheckBox methodsExpanded;
    @FXML private CheckBox chainsExpanded;
    @FXML private Spinner<Double> zoomSpinner;

    // TODO: should this stuff be moved to a model class?
    private Group graphContentGroup;
    private HashSet<AbstractLayoutVertex> highlighted;
    private LayoutRootVertex panelRoot;

    // The dimensions of the background for our graph
    private static final double initRootWidth = 500.0;
    private static final double initRootHeight = 500.0;
    private double desiredRootTranslateX;
    private double desiredRootTranslateY;

    // Store the count for vertex width and height when everything is expanded
    private double maxVertexWidth;
    private double maxVertexHeight;

    private double factorX = 1;
    private double factorY = 1;
    private static final double factorMultiple = 1.1;

    public VizPanelController(File file, Graph graph) throws IOException {
        Controllers.loadFXML("/VizPanel.fxml", this);

        this.zoomSpinner.setValueFactory(new ZoomSpinnerValueFactory(1.0, 1.2));
        TimelineProperty.bind(this.getVizPanel().scaleXProperty(), this.zoomSpinner.valueProperty(), 300);
        TimelineProperty.bind(this.getVizPanel().scaleYProperty(), this.zoomSpinner.valueProperty(), 300);

        // TODO: should this stuff be moved to a model class?
        this.highlighted = new LinkedHashSet<>();

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.highlighted = new LinkedHashSet<>();

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.vizPanel.setVisible(true);
        this.vizPanel.getChildren().add(graphContentGroup);
        //this.requestFocus();

    }

    @FXML private void resetButtonPressed() {
        Main.getSelectedVizPanelController().resetRootPosition(true);
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

    @FXML private void methodCollapseAction(ActionEvent event) {
        this.getPanelRoot().toggleNodesOfType(
                AbstractLayoutVertex.VertexType.METHOD, methodsExpanded.isSelected());
        this.resetAndRedraw(showEdges.isSelected());
        this.resetRootPosition(false);
    }

    @FXML private void chainCollapseAction(ActionEvent event) {
        this.getPanelRoot().toggleNodesOfType(
                AbstractLayoutVertex.VertexType.CHAIN, chainsExpanded.isSelected());
        this.resetAndRedraw(showEdges.isSelected());
        this.resetRootPosition(false);
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
        File file = fileChooser.showSaveDialog(getRoot().getScene().getWindow());

        if (file != null) {
            WritableImage image = vizPanel.snapshot(new SnapshotParameters(), null);

            System.out.println(file.getAbsolutePath());
            // TODO: probably use a file chooser here
            File newFile = new File(file.getAbsolutePath());

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), extension, newFile);
        }
    }

/*
    public void zoom(int zoomDistance, Button button) {
        double scaleFactor = Math.pow(factorMultiple, zoomDistance);
        factorX *= scaleFactor;
        factorY *= scaleFactor;

        ScaleTransition st = new ScaleTransition(new Duration(100), panelRoot.getGraphics());

        st.setToX(factorX);
        st.setToY(factorY);
        st.setOnFinished(event -> {
            if(button != null) {
                Controllers.<MainTabController>get(this).setZoomEnabled(true);
                Controllers.<MainTabController>get(this).keepButton(zoomDistance, button);
            }

            this.panelRoot.setVisible(false);
            this.resetStrokeWidth();
            this.panelRoot.setVisible(true);
        });
        st.play();
    }
    */


/*
    public void incrementScaleXFactor() {
        factorX *= factorMultiple;
    }

    public void decrementScaleXFactor() {
        factorX /= factorMultiple;
    }

    public void incrementScaleYFactor() {
        factorY *= factorMultiple;
    }

    public void decrementScaleYFactor() {
        factorY /= factorMultiple;
    }
    */

    public LayoutRootVertex getPanelRoot()
    {
        return this.panelRoot;
    }

    public void resetRootPosition(boolean resetScale) {
        GUINode rootGraphics = this.panelRoot.getGraphics();

        if(resetScale) {
            rootGraphics.setScaleX(1.0);
            rootGraphics.setScaleY(1.0);
        }

        rootGraphics.setTranslateLocation(this.desiredRootTranslateX, this.desiredRootTranslateY);
    }

    public HashSet<AbstractLayoutVertex> getHighlighted() {
        return this.highlighted;
    }

    public void initFX(Graph graph)
    {
        this.panelRoot = LayerFactory.getLayeredGraph(graph);
        LayoutAlgorithm.layout(this.panelRoot);
        resetPanelSize();
        drawGraph();
        this.desiredRootTranslateX = this.panelRoot.getGraphics().getTranslateX();
        this.desiredRootTranslateY = this.panelRoot.getGraphics().getTranslateY();
    }

    public void resetContent() {
        graphContentGroup = new Group();
        graphContentGroup.setVisible(true);
        this.vizPanel.getChildren().add(graphContentGroup);
    }

    private void resetPanelSize() {
        this.maxVertexWidth = this.panelRoot.getWidth();
        this.maxVertexHeight = this.panelRoot.getHeight();
    }

    public double scaleX(double coordinate)
    {
        return coordinate * initRootWidth / this.maxVertexWidth;
    }

    public double scaleY(double coordinate)
    {
        return coordinate * initRootHeight / this.maxVertexHeight;
    }

    public double invScaleX(double pixelCoordinate)
    {
        return pixelCoordinate * this.maxVertexWidth / initRootWidth;
    }

    public double invScaleY(double pixelCoordinate)
    {
        return pixelCoordinate * this.maxVertexHeight / initRootHeight;
    }

    // Divides the actual width in pixels by the width in vertex units
    public double getWidthPerVertex()
    {
        return panelRoot.getGraphics().getWidth() / panelRoot.getWidth();
    }

    //Called when the user clicks on a line in the left area.
    //Updates the vertex highlights to those that correspond to the instruction clicked.
    public void searchByJimpleIndex(String method, int index, boolean removeCurrent, boolean addChosen)
    {
        if(removeCurrent) {
            // Unhighlight currently highlighted vertices
            for (AbstractLayoutVertex v : this.highlighted) {
                v.setHighlighted(false);
            }
            highlighted.clear();
        }

        if(addChosen) {
            //Next we add the highlighted vertices
            HashSet<AbstractLayoutVertex> toAddHighlights = panelRoot.getVerticesWithInstructionID(index, method);
            for (AbstractLayoutVertex v : toAddHighlights) {
                highlighted.add(v);
                v.setHighlighted(true);
            }
        } else {
            HashSet<AbstractLayoutVertex> toRemoveHighlights = panelRoot.getVerticesWithInstructionID(index, method);
            for(AbstractLayoutVertex v : toRemoveHighlights) {
                v.setHighlighted(false);
            }
            highlighted.removeAll(toRemoveHighlights);
        }
    }

    public void resetHighlighted(AbstractLayoutVertex newHighlighted)
    {
        for(AbstractLayoutVertex currHighlighted : highlighted) {
            currHighlighted.setHighlighted(false);
        }
        highlighted.clear();

        if(newHighlighted != null) {
            highlighted.add(newHighlighted);
            newHighlighted.setHighlighted(true);
        }
    }

    public void drawGraph() {
        panelRoot.setVisible(false);
        drawNodes(null, panelRoot);
        drawEdges(panelRoot);
        //this.resetStrokeWidth();
        panelRoot.setVisible(true);
    }

    public void initZoom() {
        this.panelRoot.getGraphics().setScaleX(factorX);
        this.panelRoot.getGraphics().setScaleY(factorY);
    }

    public void resetAndRedraw(boolean edgeVisible) {
        // Using initZoom applies the current zoom level to the new mode.
        this.graphContentGroup.getChildren().remove(this.panelRoot.getGraphics());
        LayoutAlgorithm.layout(this.panelRoot);
        this.resetPanelSize();
        this.getPanelRoot().setEdgeVisibility(edgeVisible);
        this.drawGraph();
        //this.initZoom();
    }

    public void resetStrokeWidth() {
        this.getPanelRoot().resetStrokeWidth(1.0 / (Math.sqrt(factorX * factorY)));
    }

/*
    public void zoom(int zoomDistance, Button button) {
        double scaleFactor = Math.pow(factorMultiple, zoomDistance);
        factorX *= scaleFactor;
        factorY *= scaleFactor;

        ScaleTransition st = new ScaleTransition(new Duration(100), panelRoot.getGraphics());

        st.setToX(factorX);
        st.setToY(factorY);
        st.setOnFinished(event -> {
            if(button != null) {
                Controllers.<MainTabController>get(this).setZoomEnabled(true);
                Controllers.<MainTabController>get(this).keepButton(zoomDistance, button);
            }

            this.panelRoot.setVisible(false);
            this.resetStrokeWidth();
            this.panelRoot.setVisible(true);
        });
        st.play();
    }
    */

    private void drawNodes(GUINode parent, AbstractLayoutVertex v)
    {
        GUINode node = new GUINode(parent, v);
        node.setArcWidth(scaleX(0.5));
        node.setArcHeight(scaleY(0.5));
        node.setFill(v.getFill());
        node.setStroke(Color.BLACK);
        node.setStrokeWidth(.5);
        node.setOpacity(1);

        if (parent == null) {
            graphContentGroup.getChildren().add(node);
        } else {
            parent.getChildren().add(node);
        }

        double translateX = scaleX(v.getX());
        double translateY = scaleY(v.getY());
        double width = scaleX(v.getWidth());
        double height = scaleY(v.getHeight());
        node.setTranslateLocation(translateX, translateY, width, height);

        for (AbstractLayoutVertex child : v.getInnerGraph().getVertices()) {
            if (v.isExpanded()) {
                drawNodes(node, child);
            }
        }
    }

    private void drawEdges(AbstractLayoutVertex v)
    {
        if(v.isExpanded()) {
            GUINode node = v.getGraphics();
            for (LayoutEdge e : v.getInnerGraph().getEdges()) {
                e.draw(node);
                e.setVisible(v.isEdgeVisible());
            }

            for (AbstractLayoutVertex child : v.getInnerGraph().getVertices()) {
                drawEdges(child);
            }
        }
    }

/*
    public void incrementScaleXFactor() {
        factorX *= factorMultiple;
    }

    public void decrementScaleXFactor() {
        factorX /= factorMultiple;
    }

    public void incrementScaleYFactor() {
        factorY *= factorMultiple;
    }

    public void decrementScaleYFactor() {
        factorY /= factorMultiple;
    }
    */

}
