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
import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.TimelineProperty;
import org.ucombinator.jaam.visualizer.gui.ZoomSpinnerValueFactory;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.main.Main;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public abstract class GraphPanelController<T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends LayoutEdge<T> & Edge<T>> {

    @FXML protected final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML protected final Spinner<Double> zoomSpinner = null; // Initialized by Controllers.loadFXML()

    @FXML protected final Button redrawGraph = null;
    @FXML protected final Button hideUnrelated = null;

    @FXML protected final CheckBox showEdges = null; // Initialized by Controllers.loadFXML()
    @FXML protected final CheckBox showLabels = null; // Initialized by Controllers.loadFXML()
    @FXML protected final ScrollPane scrollPane = null; // Initialized by Controllers.loadFXML()
    @FXML protected final Pane pane = null; // Initialized by Controllers.loadFXML()

    protected Group graphContentGroup;
    protected T visibleRoot, immutableRoot;

    public GraphPanelController(Supplier<T> rootBuilder) throws IOException {
        Controllers.loadFXML("/GraphPane.fxml", this);

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.pane.setVisible(true);
        this.pane.getChildren().add(graphContentGroup);

        this.zoomSpinner.setValueFactory(new ZoomSpinnerValueFactory(1.0, 1.2));
        TimelineProperty.bind(this.pane.scaleXProperty(), this.zoomSpinner.valueProperty(), 300);
        TimelineProperty.bind(this.pane.scaleYProperty(), this.zoomSpinner.valueProperty(), 300);
        this.scrollPane.addEventFilter(ScrollEvent.SCROLL, this::scrollAction);

        // Set up graph, but don't draw the entire thing yet.
        this.visibleRoot = rootBuilder.get();
        this.immutableRoot = rootBuilder.get();
    }

    public T getVisibleRoot() {
        return this.visibleRoot;
    }

    public T getImmutableRoot() {
        return this.immutableRoot;
    }

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

    // TODO: Can we avoid the redraw and just set our edges to be visible again here?
    @FXML public void showEdgesAction(ActionEvent event) {
        this.visibleRoot.setVisible(false);
        this.visibleRoot.applyToEdgesRecursive(
                (HierarchicalVertex<T, S> w)
                        -> ((AbstractLayoutVertex<T>) w).setEdgeVisible(this.showEdges.isSelected()),
                (S e) -> e.redrawAndSetVisible(this.showEdges.isSelected()));
        this.visibleRoot.setVisible(true);
    }

    @FXML public void showLabelsAction(ActionEvent event) {
        this.visibleRoot.setVisible(false);
        this.visibleRoot.applyToVerticesRecursive((HierarchicalVertex<T, S> w)
                -> ((AbstractLayoutVertex<T>) w).setLabelVisible(this.showLabels.isSelected()));
        this.visibleRoot.setVisible(true);
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

}
