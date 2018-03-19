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
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.state.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

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
    private StateRootVertex visibleRoot, immutableRoot;

    private boolean inBatchMode = false;
    private boolean changedWhileInBatchMode = false;

    public VizPanelController(Graph<StateVertex, StateEdge> graph) throws IOException {
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
        this.visibleRoot = new StateRootVertex();
        this.immutableRoot = LayerFactory.getLayeredGraph(graph);
        this.drawGraph(new HashSet<>());
    }

    // TODO: Can we avoid the redraw and just set our edges to be visible again here?
    @FXML private void showEdgesAction(ActionEvent event) {
        this.visibleRoot.setVisible(false);
        this.visibleRoot.applyToEdgesRecursive(
                (HierarchicalVertex<StateVertex, StateEdge> w)
                        -> ((AbstractLayoutVertex<StateVertex>) w).setEdgeVisible(showEdges.isSelected()),
                (StateEdge e) -> e.redrawAndSetVisible(showEdges.isSelected()));
        this.visibleRoot.setVisible(true);
    }

    @FXML private void showLabelsAction(ActionEvent event) {
        this.getVisibleRoot().setVisible(false);
        this.visibleRoot.applyToVerticesRecursive((HierarchicalVertex<StateVertex, StateEdge> w)
                -> ((AbstractLayoutVertex<StateVertex>) w).setLabelVisible(showLabels.isSelected()));
        this.getVisibleRoot().setVisible(true);
    }

    @FXML private void groupByClassAction(ActionEvent event) {
        this.redrawGraph(Main.getSelectedMainTabController().getHidden());
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

    public StateRootVertex getVisibleRoot() {
        return this.visibleRoot;
    }

    public StateRootVertex getImmutableRoot() {
        return this.immutableRoot;
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
        // TODO: Right now we're not hiding anything at the start, so we just pass an empty set.
        // It would take extra work to be able to access the hidden set at this point. That's because the
        // tab that this is created inside of hasn't been added to the tabPane yet, so calling
        // Main.getSelectedMainTabController() returns null.
        this.visibleRoot = this.immutableRoot.constructVisibleGraphExcept(hidden);
        /*if (groupByClass.isSelected()) {
            this.visibleRoot = (StateRootVertex) GraphUtils.constructCompressedGraph(this.immutableRoot,
                    new Function<StateVertex, String>() {
                        @Override
                        public String apply(StateVertex v) {
                            if (v instanceof ClassEntity) {
                                return ((ClassEntity) v).getClassName();
                            } else {
                                return null;
                            }
                        }
                    },
                    new BiFunction<String, Set<StateVertex>, StateVertex>() {
                        @Override
                        public StateClassVertex apply(String className, Set<StateVertex> classVertices) {
                            StateClassVertex classVertex = new StateClassVertex(className);
                            for (StateVertex v : classVertices) {
                                classVertex.getChildGraph().addVertex(v);
                            }
                            return classVertex;
                        }
                    },
                    StateEdge::new);
        }*/

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

    private void drawNodes(GUINode<StateVertex> parent, StateVertex v) {
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

        Graph<StateVertex, StateEdge> childGraph = v.getChildGraph();
        for (StateVertex child : childGraph.getVertices()) {
            if (v.isExpanded()) {
                drawNodes(node, child);
            }
        }
    }

    private void drawEdges(StateVertex v) {
        if(v.isExpanded()) {
            Graph<StateVertex, StateEdge> childGraph = v.getChildGraph();
            for (LayoutEdge<StateVertex> e : childGraph.getEdges()) {
                e.setVisible(v.isEdgeVisible());
                e.draw();
            }

            for (StateVertex child : childGraph.getVertices()) {
                drawEdges(child);
            }
        }
    }

    public HashSet<StateVertex> pruneVisibleGraph() {
        return this.visibleRoot.getChildGraph().getVerticesToPrune(v -> (v.getType() == AbstractLayoutVertex.VertexType.METHOD));
    }

    @Override
    public void onChanged(Change<? extends StateVertex> change) {
        System.out.println("JUAN: Hidden changed: " + change);
        if(change.wasAdded()) {
            StateVertex v = change.getElementAdded();
            v.setHighlighted(false);
            v.setHidden();
        } else {
            StateVertex v = change.getElementRemoved();
            v.setUnhidden();
        }

        if(!inBatchMode) {
            System.out.println("Immediately redrawing...");
            this.redrawGraph(Main.getSelectedMainTabController().getHidden());
        } else {
            System.out.println("Waiting to redraw batch...");
            changedWhileInBatchMode = true;
        }
    }

    public void startBatchMode() {
        System.out.println("Starting batch mode for loop graph.");
        inBatchMode = true;
        changedWhileInBatchMode = false;
    }

    public void endBatchMode() {
        System.out.println("Ending batch mode for loop graph.");
        inBatchMode = false;
        if(changedWhileInBatchMode) {
            this.redrawGraph(Main.getSelectedMainTabController().getHidden());
        }
    }

    public boolean isInBatchMode() {
        return inBatchMode;
    }
}
