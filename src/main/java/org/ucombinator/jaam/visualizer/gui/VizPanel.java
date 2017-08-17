package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.*;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class VizPanel extends Pane
{
    private Group graphContentGroup;
    private HashSet<AbstractLayoutVertex> highlighted;
    private LayoutRootVertex panelRoot;

    // The dimensions of the background for our graph
    private final double initRootWidth = 500.0, initRootHeight = 500.0;
    private double desiredRootTranslateX, desiredRootTranslateY;

    // Store the count for vertex width and height when everything is expanded
    private double maxVertexWidth, maxVertexHeight;

    private double factorX = 1;
    private double factorY = 1;
    private static final double factorMultiple = 1.1;

    public LayoutRootVertex getPanelRoot()
    {
        return this.panelRoot;
    }
    
    public void resetRootPosition(boolean resetScale) {
        GUINode rootGraphics = VizPanel.this.panelRoot.getGraphics();

        if(resetScale) {
            rootGraphics.setScaleX(1.0);
            rootGraphics.setScaleY(1.0);
        }

        rootGraphics.setTranslateLocation(VizPanel.this.desiredRootTranslateX, VizPanel.this.desiredRootTranslateY);
    }

    public VizPanel()
    {
        super();
        this.highlighted = new LinkedHashSet<>();

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.highlighted = new LinkedHashSet<>();

        this.graphContentGroup = new Group();
        this.graphContentGroup.setVisible(true);
        this.setVisible(true);
        this.getChildren().add(graphContentGroup);
        //this.requestFocus();
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
        this.getChildren().add(graphContentGroup);
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
