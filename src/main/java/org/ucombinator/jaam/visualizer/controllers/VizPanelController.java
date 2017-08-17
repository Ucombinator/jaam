package org.ucombinator.jaam.visualizer.controllers;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.TimelineProperty;
import org.ucombinator.jaam.visualizer.gui.VizPanel;
import org.ucombinator.jaam.visualizer.gui.ZoomSpinnerValueFactory;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.main.Main;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class VizPanelController {
    @FXML private Node root;
    public Node getRoot() { return this.root; }

    @FXML private VizPanel mainPanel;
    public VizPanel getMainPanel() { return this.mainPanel; } // TODO: rename

    @FXML private CheckBox showEdges;
    @FXML private CheckBox showLabels;
    @FXML private CheckBox methodsExpanded;
    @FXML private CheckBox chainsExpanded;

    @FXML private Spinner<Double> zoomSpinner;

    public VizPanelController(File file, Graph graph) throws IOException {
        Controllers.loadFXML("/VizPanel.fxml", this);

        this.zoomSpinner.setValueFactory(new ZoomSpinnerValueFactory(1.0, 1.2));
        TimelineProperty.bind(this.getMainPanel().scaleXProperty(), this.zoomSpinner.valueProperty(), 300);
        TimelineProperty.bind(this.getMainPanel().scaleYProperty(), this.zoomSpinner.valueProperty(), 300);
    }

    @FXML private void resetButtonPressed() {
        Main.getSelectedVizPanel().resetRootPosition(true);
    }

    @FXML private void showEdgesAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot().setVisible(false);
        this.getMainPanel().getPanelRoot().setEdgeVisibility(showEdges.isSelected());
        LayoutEdge.redrawEdges(mainPanel.getPanelRoot(), true);
        this.getMainPanel().getPanelRoot().setVisible(true);
    }

    @FXML private void showLabelsAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot().setVisible(false);
        this.getMainPanel().getPanelRoot().setLabelVisibility(showLabels.isSelected());
        this.getMainPanel().getPanelRoot().setVisible(true);
    }

    @FXML private void methodCollapseAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot().toggleNodesOfType(
                AbstractLayoutVertex.VertexType.METHOD, methodsExpanded.isSelected());
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
    }

    @FXML private void chainCollapseAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot().toggleNodesOfType(
                AbstractLayoutVertex.VertexType.CHAIN, chainsExpanded.isSelected());
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
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
            WritableImage image = mainPanel.snapshot(new SnapshotParameters(), null);

            System.out.println(file.getAbsolutePath());
            // TODO: probably use a file chooser here
            File newFile = new File(file.getAbsolutePath());

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), extension, newFile);
        }
    }
}
