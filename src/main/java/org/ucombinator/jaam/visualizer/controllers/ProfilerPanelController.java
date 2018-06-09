package org.ucombinator.jaam.visualizer.controllers;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.TimelineProperty;
import org.ucombinator.jaam.visualizer.gui.ZoomSpinnerValueFactory;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.profiler.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

// TODO: Use the layout function in ProfileTree to drawEdge the graph.
public class ProfilerPanelController {

    @FXML protected final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final Spinner<Double> zoomSpinner = null; // Initialized by Controllers.loadFXML()

    @FXML private final Button redrawGraph = null;
    @FXML private final Button hideUnrelated = null;
    @FXML private final Button loadProfile = null;

    @FXML private final CheckBox showLabels = null; // Initialized by Controllers.loadFXML()
    @FXML private final ScrollPane scrollPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final Pane pane = null; // Initialized by Controllers.loadFXML()

    private Group graphContentGroup;
    private ProfilerTree currentTree;

    public ProfilerPanelController() throws IOException {
        Controllers.loadFXML("/ProfilerPane.fxml", this);

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.pane.setVisible(true);
        this.pane.getChildren().add(graphContentGroup);

        this.zoomSpinner.setValueFactory(new ZoomSpinnerValueFactory(1.0, 1.2));
        TimelineProperty.bind(this.pane.scaleXProperty(), this.zoomSpinner.valueProperty(), 300);
        TimelineProperty.bind(this.pane.scaleYProperty(), this.zoomSpinner.valueProperty(), 300);
        this.scrollPane.addEventFilter(ScrollEvent.SCROLL, this::scrollAction);

        currentTree = null;
    }

    private void drawGraph() {
        graphContentGroup.getChildren().clear();
        this.drawNodes();
        this.drawEdges();
    }

    private void drawNodes() {
        for (ProfilerVertex v : this.currentTree.getVertices()) {
            GUINode<ProfilerVertex> node = new GUINode<>(null, v);
            v.setLabelVisible(true);
            double x = v.getLeftColumn() * ProfilerTree.UNIT_SIZE + ProfilerTree.MARGIN_SIZE;
            double width = (v.getRightColumn() - v.getLeftColumn()) * ProfilerTree.UNIT_SIZE - 2 * ProfilerTree.MARGIN_SIZE;
            double y = v.getRow() * ProfilerTree.UNIT_SIZE + ProfilerTree.MARGIN_SIZE;
            double height = ProfilerTree.UNIT_SIZE - 2 * ProfilerTree.MARGIN_SIZE;
            node.setTranslateLocation(x, y, width, height);
            System.out.println("Drawing node " + v.getId() + " between rows " + v.getLeftColumn() + " and " + v.getRightColumn());
            graphContentGroup.getChildren().add(node);
        }
    }

    private void drawEdges() {
        for (ProfilerEdge e : currentTree.getEdges()) {
            e.drawEdge(graphContentGroup);
        }
    }

    @FXML public void showLabelsAction(ActionEvent event) {
        // TODO
        System.out.println("Error: labels not implemented.");
    }

    @FXML public void exportImageAction(ActionEvent event) throws IOException {
        event.consume();
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
            WritableImage image = this.pane.snapshot(new SnapshotParameters(), null);

            System.out.println(file.getAbsolutePath());
            // TODO: probably use a file chooser here
            File newFile = new File(file.getAbsolutePath());

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), extension, newFile);
        }
    }


    protected void scrollAction(ScrollEvent event) {
        if (event.isControlDown()) {
            // Zoom in or out
            event.consume();
            if (event.getDeltaY() > 0) {
                this.zoomSpinner.increment();
            } else if (event.getDeltaY() < 0) {
                this.zoomSpinner.decrement();
            }
        }
    }

    @FXML public void redrawGraphAction(ActionEvent event) throws IOException {
        event.consume();
        System.out.println("Redraw Graph not implemented");
    }

    @FXML public void hideUnrelatedAction(ActionEvent event) throws IOException {
        event.consume();
        System.out.println("Hide Unrelated not implemented");
    }

    @FXML public void loadProfileAction(ActionEvent event) throws IOException {
        event.consume();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(this.root.getScene().getWindow());

        DataTree dataTree = new DataTree(selectedFile.getAbsolutePath()).prune(100000);
        this.currentTree = new ProfilerTree(dataTree);
        this.currentTree.computeCurrentLayoutLP();
        this.drawGraph();
    }
}
