package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.Group;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.graph.Graph;

public class VizPanel extends StackPane
{
	private ScrollPane graphScrollPane;
	private Group graphContentGroup;
	private HashSet<AbstractLayoutVertex> highlighted;
	private LayoutRootVertex panelRoot;

	// The dimensions of the background for our graph
	private final double initRootWidth = 500.0, initRootHeight = 500.0;

	// Store the count for vertex width and height when everything is expanded
	private double maxVertexWidth, maxVertexHeight;

	private double factorX = 1;
	private double factorY = 1;
	private static final double factorMultiple = 1.1;

	public LayoutRootVertex getPanelRoot()
	{
		return this.panelRoot;
	}

	public VizPanel()
	{
		super();

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
		ArrayList<AbstractLayoutVertex> tempHighlighted = new ArrayList<AbstractLayoutVertex>();
		for(AbstractLayoutVertex currHighlighted : this.highlighted)
			tempHighlighted.add(currHighlighted);

		for(AbstractLayoutVertex currHighlighted : tempHighlighted)
			currHighlighted.setHighlighted(false, this);
		highlighted = new HashSet<AbstractLayoutVertex>();

		if(newHighlighted != null) {
			highlighted.add(newHighlighted);
			newHighlighted.setHighlighted(true, this);
		}
	}

	public void drawGraph() {
		panelRoot.setVisible(false);
		drawNodes(null, panelRoot);
		drawEdges(panelRoot);
		panelRoot.setVisible(true);
	}
	
	private boolean zoomEnabled = true;
	private boolean zoomButtonReleased = false;
	
	private void keepButton(int zoom, Button button){
		if(zoomEnabled && !zoomButtonReleased){
			zoomEnabled = false;
			VizPanel.this.zoom(zoom, button);
		}
		if(zoomButtonReleased){
			zoomButtonReleased = false;	
		}
	}
	
	private void drawZoomButtons() {
		VBox buttonBox = new VBox(5);
		Button zoomIn = new Button("+");
		Button zoomOut = new Button("-");
		
		zoomIn.setOnMousePressed(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				keepButton(1,zoomIn);
			}
		});
		
		zoomIn.setOnMouseReleased(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				zoomButtonReleased = true;
			}
		});
		
		
		zoomOut.setOnMousePressed(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				keepButton(-1,zoomOut);
			}
		});
		
		zoomOut.setOnMouseReleased(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				zoomButtonReleased = true;
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

	public void resetZoom() {
		factorX = 1;
		factorY = 1;
	}

	public void initZoom() {
		this.panelRoot.getGraphics().setScaleX(factorX);
		this.panelRoot.getGraphics().setScaleY(factorY);
	}

	public void resetAndRedraw(boolean edgeVisible) {
		// Using resetZoom sets the zoom to 1 each time we change modes.
		// Using initZoom applies the current zoom level to the new mode.

		this.graphContentGroup.getChildren().remove(this.panelRoot.getGraphics());
		LayoutAlgorithm.layout(this.panelRoot);
		this.resetPanelSize();
		//this.resetZoom();
		this.getPanelRoot().setEdgeVisibility(edgeVisible);
		this.drawGraph();
		this.initZoom();
	}

	private void zoom(int zoomDistance, Button button) {
		factorX *= Math.pow(factorMultiple, zoomDistance);
		factorY *= Math.pow(factorMultiple, zoomDistance);

		ScaleTransition st = new ScaleTransition(new Duration(100), panelRoot.getGraphics());
		st.setToX(factorX);
		st.setToY(factorY);
		st.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				zoomEnabled = true;
				keepButton(zoomDistance, button);
			}
		});
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
			if(v.isExpanded())
				drawNodes(node, child);
		}
	}

	private void drawEdges(AbstractLayoutVertex v)
	{
		GUINode node = v.getGraphics();
		if(v.isExpanded()) {
			// TODO: Adjust arrow length?
			//Edge.arrowLength = this.getWidthPerVertex() / 10.0;
			for (LayoutEdge e : v.getInnerGraph().getEdges().values()) {
				//System.out.println("Drawing edge: " + e.getID());
				e.draw(node);
				e.setVisible(v.isEdgeVisible());
			}

			for (AbstractLayoutVertex child : v.getInnerGraph().getVertices().values())
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
