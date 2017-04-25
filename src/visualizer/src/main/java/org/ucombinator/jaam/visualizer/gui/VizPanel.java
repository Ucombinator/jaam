package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ScaleTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.Group;

import java.util.HashSet;
import java.util.Iterator;

import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.graph.Graph;

public class VizPanel extends StackPane
{
	private StacFrame stFrame;

	private ScrollPane graphScrollPane;
	private Group graphContentGroup;
	private HashSet<AbstractLayoutVertex> highlighted;
	private LayoutRootVertex panelRoot;

	// The dimensions of the background for our graph
	private final double rootWidth = 500.0, rootHeight = 500.0;

	// Store the count for vertex width and height when everything is expanded
	private double maxVertexWidth, maxVertexHeight;

	private double factorX = 1;
	private double factorY = 1;
	private static final double factorMultiple = 1.1;

	public LayoutRootVertex getPanelRoot()
	{
		return this.panelRoot;
	}

	public VizPanel(StacFrame stFrame)
	{
		super();
		this.stFrame = stFrame;

		this.graphScrollPane = new ScrollPane();
		graphContentGroup = new Group();
		graphContentGroup.setVisible(true);
		graphScrollPane.setVisible(true);
		graphScrollPane.setContent(graphContentGroup);
		graphScrollPane.setPannable(true);

		this.getChildren().add(graphScrollPane);
		drawZoomButtons();

		highlighted = new HashSet<AbstractLayoutVertex>();
	}

	public HashSet<AbstractLayoutVertex> getHighlighted() {
		return this.highlighted;
	}

	public void initFX(Graph graph)
	{
		//System.out.println("Running layout...");
		this.panelRoot = LayerFactory.getLayeredGraph(graph);
		LayoutAlgorithm.layout(this.panelRoot);
		resetPanelSize();

		drawGraph();
	}

	public void resetContent() {
		graphContentGroup = new Group();
		graphContentGroup.setVisible(true);
		graphScrollPane.setContent(graphContentGroup);
	}

	public void resetPanelSize() {
		this.maxVertexWidth = this.panelRoot.getWidth();
		this.maxVertexHeight = this.panelRoot.getHeight();		
	}

	public double scaleX(double coordinate)
	{
		return factorX * (coordinate * rootWidth / this.maxVertexWidth);
	}

	public double scaleY(double coordinate)
	{
		return factorY * (coordinate * rootHeight / this.maxVertexHeight);
	}

	public double invScaleX(double pixelCoordinate)
	{
		return (pixelCoordinate * this.maxVertexWidth / rootWidth) / factorX;
	}

	public double invScaleY(double pixelCoordinate)
	{
		return (pixelCoordinate * this.maxVertexHeight / rootHeight) / factorY;
	}

	public double getZoomLevel()
	{
		// We assume that scaleX and scaleY are equal
		return graphContentGroup.getScaleX();
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
				highlighted.remove(v);
				v.setHighlighted(false, this);
			}
		}

		if(addChosen) {
			//Next we add the highlighted vertices
			HashSet<AbstractLayoutVertex> toAddHighlights = panelRoot.getVerticesWithInstructionID(index, method);
			for (AbstractLayoutVertex v : toAddHighlights) {
				highlighted.add(v);
				v.setHighlighted(true, this);
			}
		} else {
			HashSet<AbstractLayoutVertex> toRemoveHighlights = panelRoot.getVerticesWithInstructionID(index, method);
			for(AbstractLayoutVertex v : toRemoveHighlights) {
				highlighted.remove(v);
				v.setHighlighted(false, this);
			}
		}
	}

	public void resetHighlighted(AbstractLayoutVertex newHighlighted)
	{
		for(AbstractLayoutVertex currHighlighted : this.highlighted)
			currHighlighted.setHighlighted(false, this);
		highlighted = new HashSet<AbstractLayoutVertex>();

		if(newHighlighted != null) {
			highlighted.add(newHighlighted);
			newHighlighted.setHighlighted(true, this);
		}
	}

	public void drawGraph() {
		drawNodes(null, panelRoot);
		drawEdges(panelRoot);
	}

	private void drawZoomButtons() {
		VBox buttonBox = new VBox(5);
		Button zoomIn = new Button("+");
		Button zoomOut = new Button("-");

		zoomIn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				VizPanel.this.zoom(1);
			}
		});

		zoomOut.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				VizPanel.this.zoom(-1);
			}
		});

		// We add the buttons directly to our StackPane, so when we scroll they stay in the same place.
		buttonBox.getChildren().add(zoomIn);
		buttonBox.getChildren().add(zoomOut);
		this.getChildren().add(buttonBox);
		buttonBox.setLayoutX(10);
		buttonBox.setLayoutY(10);

		zoomIn.setVisible(true);
		zoomOut.setVisible(true);
		buttonBox.setVisible(true);

		// Allow mouse events to be passed to visible nodes on the lower layer of our stack pane
		buttonBox.setPickOnBounds(false);
	}

	private void zoom(int zoomDistance) {
		factorX *= Math.pow(factorMultiple, zoomDistance);
		factorY *= Math.pow(factorMultiple, zoomDistance);

		ScaleTransition st = new ScaleTransition(new Duration(100), panelRoot.getGraphics());
		st.setToX(factorX);
		st.setToY(factorY);
		st.play();
	}

	private void drawNodes(GUINode parent, AbstractLayoutVertex v)
	{
		GUINode node = new GUINode(parent, v);

		if (parent == null) {
			graphContentGroup.getChildren().add(node);
		}
		else {
			parent.getChildren().add(node);
		}

		double translateX = scaleX(v.getX());
		double translateY = scaleY(v.getY());
		double width = scaleX(v.getWidth());
		double height = scaleY(v.getHeight());
		node.setTranslateLocation(translateX, translateY, width, height);

		// Move these to initialization?
		node.setArcWidth(scaleX(0.5));
		node.setArcHeight(scaleY(0.5));
		//node.setLabel("  " + v.getLabel());

		node.setFill(v.getColor());
		node.setStroke(javafx.scene.paint.Color.BLACK);
		node.setStrokeWidth(.5);
		node.setOpacity(1);

		if (v.getInnerGraph().getVertices().size() == 0)
			return;

		Iterator<AbstractLayoutVertex> it = v.getInnerGraph().getVertices().values().iterator();
		while (it.hasNext())
		{
			AbstractLayoutVertex child = it.next();
			if(v.isExpanded()) {
				drawNodes(node, child);
			}
			else {
				// TODO: Click on the "C" button to collapse all chains, then try to expand a chain.
				// The internal nodes in the chain should appear, but their GUINodes are still null, which gives an error.
				// This is an attempt to fix the bug, but it still doesn't work yet.
				//initializeCollapsedNodes(node, child);
			}
		}
	}

	public void initializeCollapsedNodes(GUINode parent, AbstractLayoutVertex v) {
		GUINode node = new GUINode(parent, v);
		if (parent == null) {
			graphContentGroup.getChildren().add(node);
		}
		else {
			parent.getChildren().add(node);
		}

		Iterator<AbstractLayoutVertex> it = v.getInnerGraph().getVertices().values().iterator();
		while (it.hasNext())
		{
			AbstractLayoutVertex child = it.next();
			initializeCollapsedNodes(node, child);
		}
	}

	private void drawEdges(AbstractLayoutVertex v)
	{
		if(!stFrame.isEdgeVisible()) {
			System.out.println("Skipping drawEdges - edges set to invisible...");
			return;
		}
		
		GUINode node = v.getGraphics();
		if(v.isExpanded())
		{
			//System.out.println("Drawing edges of vertex: " + v.getStrID());

			//Edge.arrowLength = this.getWidthPerVertex() / 10.0;
			for(LayoutEdge e : v.getInnerGraph().getEdges().values()) {
				System.out.println("Drawing edge: " + e.getID());
				e.draw(node);
			}
		
			for(AbstractLayoutVertex child : v.getInnerGraph().getVertices().values())
				drawEdges(child);
		}
	}

	// TODO: Use this when drawing edges
	public void scaleLines()
	{
		//System.out.println("Scaling lines and arrowheads...");
		for(LayoutEdge e : this.panelRoot.getInnerGraph().getEdges().values())
			e.setScale(this);

		for(AbstractLayoutVertex v : this.panelRoot.getInnerGraph().getVertices().values())
		{
			for(LayoutEdge e : v.getInnerGraph().getEdges().values())
				e.setScale(this);
		}
	}

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
}
