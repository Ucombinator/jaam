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
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.TimelineProperty;
import org.ucombinator.jaam.visualizer.gui.VizPanel;
import org.ucombinator.jaam.visualizer.gui.ZoomSpinnerValueFactory;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class VizPanelController {
    @FXML private Node root;
    public Node getRoot() { return this.root; }

    @FXML private VizPanel vizPanel;
    public VizPanel getVizPanel() { return this.vizPanel; }

    @FXML private CheckBox showEdges;
    @FXML private CheckBox showLabels;
    @FXML private CheckBox methodsExpanded;
    @FXML private CheckBox chainsExpanded;

    @FXML private Spinner<Double> zoomSpinner;

    public VizPanelController(File file, Graph graph) throws IOException {
        Controllers.loadFXML("/VizPanel.fxml", this);

        this.zoomSpinner.setValueFactory(new ZoomSpinnerValueFactory(1.0, 1.2));
        TimelineProperty.bind(this.getVizPanel().scaleXProperty(), this.zoomSpinner.valueProperty(), 300);
        TimelineProperty.bind(this.getVizPanel().scaleYProperty(), this.zoomSpinner.valueProperty(), 300);
    }

    @FXML private void resetButtonPressed() {
        Main.getSelectedVizPanel().resetRootPosition(true);
    }

    @FXML private void showEdgesAction(ActionEvent event) {
        this.getVizPanel().getPanelRoot().setVisible(false);
        this.getVizPanel().getPanelRoot().setEdgeVisibility(showEdges.isSelected());
        LayoutEdge.redrawEdges(vizPanel.getPanelRoot(), true);
        this.getVizPanel().getPanelRoot().setVisible(true);
    }

    @FXML private void showLabelsAction(ActionEvent event) {
        this.getVizPanel().getPanelRoot().setVisible(false);
        this.getVizPanel().getPanelRoot().setLabelVisibility(showLabels.isSelected());
        this.getVizPanel().getPanelRoot().setVisible(true);
    }

    @FXML private void methodCollapseAction(ActionEvent event) {
        this.getVizPanel().getPanelRoot().toggleNodesOfType(
                AbstractLayoutVertex.VertexType.METHOD, methodsExpanded.isSelected());
        this.getVizPanel().resetAndRedraw(showEdges.isSelected());
        this.getVizPanel().resetRootPosition(false);
    }

    @FXML private void chainCollapseAction(ActionEvent event) {
        this.getVizPanel().getPanelRoot().toggleNodesOfType(
                AbstractLayoutVertex.VertexType.CHAIN, chainsExpanded.isSelected());
        this.getVizPanel().resetAndRedraw(showEdges.isSelected());
        this.getVizPanel().resetRootPosition(false);
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

    public LayoutRootVertex getPanelRoot() { return this.getPanelRoot(); }
    public HashSet<AbstractLayoutVertex> getHighlighted() { return this.getVizPanel().getHighlighted(); }
    public void initFX(Graph graph) { this.getVizPanel().initFX(graph);}
    public void resetContent() { this.resetContent(); }
    public void resetPanelSize() { this.getVizPanel().resetPanelSize(); }
    public double scaleX(double coordinate) { return this.getVizPanel().scaleX(coordinate); }
    public double scaleY(double coordinate) { return this.getVizPanel().scaleY(coordinate); }
    public double invScaleX(double pixelCoordinate) { return this.getVizPanel().invScaleX(pixelCoordinate); }
    public double invScaleY(double pixelCoordinate) { return this.getVizPanel().invScaleY(pixelCoordinate); }
    public double getWidthPerVertex() { return this.getVizPanel().getWidthPerVertex(); }
    public void searchByJimpleIndex(String method, int index, boolean removeCurrent, boolean addChosen) { this.getVizPanel().searchByJimpleIndex(method, index, removeCurrent, addChosen); }
    public void resetHighlighted(AbstractLayoutVertex newHighlighted) { this.getVizPanel().resetHighlighted(newHighlighted); }
    public void drawGraph() { this.getVizPanel().drawGraph(); }
    public void initZoom() { this.getVizPanel().initZoom(); }
    public void resetAndRedraw(boolean edgeVisible) { this.getVizPanel().resetAndRedraw(edgeVisible); }
    public void resetStrokeWidth() { this.getVizPanel().resetStrokeWidth(); }
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

}
