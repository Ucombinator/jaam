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
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.gui.TimelineProperty;
import org.ucombinator.jaam.visualizer.gui.ZoomSpinnerValueFactory;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.profiler.DataTree;
import org.ucombinator.jaam.visualizer.profiler.ProfilerTree;
import org.ucombinator.jaam.visualizer.profiler.ProfilerVertex;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

// TODO: Use the layout function in ProfileTree to draw the graph.
public class ProfilerPanelController {

    @FXML protected final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML protected final Spinner<Double> zoomSpinner = null; // Initialized by Controllers.loadFXML()

    @FXML protected final Button redrawGraph = null;
    @FXML protected final Button hideUnrelated = null;
    @FXML protected final Button loadProfile = null;

    @FXML protected final CheckBox showEdges = null; // Initialized by Controllers.loadFXML()
    @FXML protected final CheckBox showLabels = null; // Initialized by Controllers.loadFXML()
    @FXML protected final ScrollPane scrollPane = null; // Initialized by Controllers.loadFXML()
    @FXML protected final Pane pane = null; // Initialized by Controllers.loadFXML()

    protected Group graphContentGroup;
    protected ProfilerTree currentTree;

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
        this.drawNodes();
        this.drawEdges();
    }

    private void drawNodes() {
        for (ProfilerVertex v : this.currentTree.getVertices()) {
            double x = v.getLeftColumn() * ProfilerTree.UNIT_SIZE + ProfilerTree.MARGIN_SIZE;
            double width = (v.getRightColumn() - v.getLeftColumn()) * ProfilerTree.UNIT_SIZE - 2 * ProfilerTree.MARGIN_SIZE;
            double y = v.getRow() * ProfilerTree.UNIT_SIZE + ProfilerTree.MARGIN_SIZE;
            double height = ProfilerTree.UNIT_SIZE - 2 * ProfilerTree.MARGIN_SIZE;
            Rectangle rect = new Rectangle(x, y, width, height);
            graphContentGroup.getChildren().add(rect);
        }
    }

    private void drawEdges() {
        for (ProfilerVertex v : this.currentTree.getVertices()) {
            double x, y1, y2;
            x = v.getEdgeColumn() * ProfilerTree.UNIT_SIZE;
            if (v.getParent() == null) {
                y1 = ProfilerTree.MARGIN_SIZE;
            }
            else {
                y1 = (v.getParent().getRow() + 1) * ProfilerTree.UNIT_SIZE - ProfilerTree.MARGIN_SIZE;
            }
            y2 = v.getRow() * ProfilerTree.UNIT_SIZE + ProfilerTree.MARGIN_SIZE;
            Line line = new Line(x, y1, x, y2);
            graphContentGroup.getChildren().add(line);
        }
    }

    /*
    protected void drawNodes(GUINode<T> parent, T v) {
        GUINode<T> node = new GUINode<>(parent, v);

        if (parent == null) {
            this.graphContentGroup.getChildren().clear();
            this.graphContentGroup.getChildren().add(node);
        } else {
            parent.getChildren().add(node);
        }

        double translateX = v.getX();
        double translateY = v.getY();
        double width = v.getWidth();
        double height = v.getHeight();
        node.setTranslateLocation(translateX, translateY, width, height);

        Graph<T, S> childGraph = v.getInnerGraph();
        for (T child : childGraph.getVertices()) {
            if (v.isExpanded()) {
                drawNodes(node, child);
            }
        }
    }

    protected void drawEdges(T v) {
        if (v.isExpanded()) {
            Graph<T, S> childGraph = v.getInnerGraph();
            for (S e : childGraph.getEdges()) {

                e.setVisible(v.isEdgeVisible());
                e.draw();
            }

            for (T child : childGraph.getVertices()) {
                drawEdges(child);
            }
        }
    }
    */

    // TODO: Can we avoid the redraw and just set our edges to be visible again here?
    @FXML public void showEdgesAction(ActionEvent event) {
        /*
        this.visibleRoot.setVisible(false);
        this.visibleRoot.applyToEdgesRecursive(
                (HierarchicalVertex<T, S> w)
                        -> ((AbstractLayoutVertex<T>) w).setEdgeVisible(this.showEdges.isSelected()),
                (S e) -> e.redrawAndSetVisible(this.showEdges.isSelected()));
        this.visibleRoot.setVisible(true);
        */
    }

    @FXML public void showLabelsAction(ActionEvent event) {
        /*
        this.visibleRoot.setVisible(false);
        this.visibleRoot.applyToVerticesRecursive((HierarchicalVertex<T, S> w)
                -> ((AbstractLayoutVertex<T>) w).setLabelVisible(this.showLabels.isSelected()));
        this.visibleRoot.setVisible(true);
        */
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
        this.currentTree.computeCurrentLayout();
        this.drawGraph();
    }
}