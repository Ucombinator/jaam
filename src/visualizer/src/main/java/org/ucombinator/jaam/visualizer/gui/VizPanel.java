package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

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
	private Pane graphPane;
	private Group graphContentGroup;
	private HashSet<AbstractLayoutVertex> highlighted;
	private LayoutRootVertex panelRoot;
	private Button zoomIn, zoomOut, resetButton;

	private double deriredRootTranslateY, deriredRootTranslateX;
	
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

	public VizPanel()
	{
		super();
		this.graphPane = new Pane();
		highlighted = new HashSet<AbstractLayoutVertex>();

		zoomIn = new Button("+");
		zoomOut = new Button("-");
		resetButton = new Button("=");
		
		
		resetButton.setOnMousePressed(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				GUINode rootGraphics = VizPanel.this.panelRoot.getGraphics();
				rootGraphics.setTranslateLocation(VizPanel.this.desiredRootTranslateX, VizPanel.this.desiredRootTranslateY);
			}
		});
		
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

		this.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {

				if (event.getEventType()!= KeyEvent.KEY_PRESSED){
					return;
				}
				//System.out.println(event.getCode().toString());

				VizPanel.this.graphPane.layout();
				GUINode rootGraphics =  VizPanel.this.getPanelRoot().getGraphics();
				switch(event.getCode().toString()) {
					case "RIGHT":
					{
						rootGraphics.setTranslateX(rootGraphics.getTranslateX() + 10);
						break;
					}
					case "LEFT":
					{
						rootGraphics.setTranslateX(rootGraphics.getTranslateX() - 10);
						break;
					}
					case "UP":
					{
						rootGraphics.setTranslateY(rootGraphics.getTranslateY() - 10);
						break;
					}
					case "DOWN":
					{
						rootGraphics.setTranslateY(rootGraphics.getTranslateY() + 10);
						break;
					}
					case "EQUALS":
					{
						VizPanel.this.zoom(1, null);
						break;
					}
					case "MINUS":
					{
						VizPanel.this.zoom(-1, null);
						break;
					}
		            default: break;
				}

			}
		});

		graphContentGroup = new Group();
		graphContentGroup.setVisible(true);
		graphPane.setVisible(true);
		graphPane.getChildren().add(graphContentGroup);

		this.getChildren().add(graphPane);
		this.requestFocus();
		drawZoomButtons();
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
		graphPane.getChildren().add(graphContentGroup);
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
		this.resetStrokeWidth();
		panelRoot.setVisible(true);
	}
	
	private boolean zoomEnabled = true;
	private boolean zoomButtonReleased = false;
	
	private void keepButton(int zoom, Button button) {
		if(zoomEnabled && !zoomButtonReleased) {
			zoomEnabled = false;
			VizPanel.this.zoom(zoom, button);
		}
		if(zoomButtonReleased) {
			zoomButtonReleased = false;	
		}
	}
	
	private void drawZoomButtons() {
		VBox buttonBox = new VBox(5);

		// We add the buttons directly to our StackPane, so when we scroll they stay in the same place.
		buttonBox.getChildren().add(zoomIn);
		buttonBox.getChildren().add(zoomOut);
		buttonBox.getChildren().add(resetButton);
		this.getChildren().add(buttonBox);
		buttonBox.setLayoutX(10);
		buttonBox.setLayoutY(10);

		zoomIn.setVisible(true);
		zoomOut.setVisible(true);
		resetButton.setVisible(true);
		buttonBox.setVisible(true);

		// Allow mouse events to be passed to visible nodes on the lower layer of our stack pane
		buttonBox.setPickOnBounds(false);
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
		this.initZoom();
	}

	public void resetStrokeWidth() {
		this.getPanelRoot().resetStrokeWidth(1.0 / (Math.sqrt(factorX * factorY)));
	}

	private void zoom(int zoomDistance, Button button) {
		double scaleFactor = Math.pow(factorMultiple, zoomDistance);
		factorX *= scaleFactor;
		factorY *= scaleFactor;

		ScaleTransition st = new ScaleTransition(new Duration(100), panelRoot.getGraphics());

		st.setToX(factorX);
		st.setToY(factorY);
		st.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(button != null) {
					zoomEnabled = true;
					keepButton(zoomDistance, button);
				}

				VizPanel.this.panelRoot.setVisible(false);
				VizPanel.this.resetStrokeWidth();
				VizPanel.this.panelRoot.setVisible(true);
			}
		});
		st.play();
	}

	private void drawNodes(GUINode parent, AbstractLayoutVertex v)
	{
		GUINode node = new GUINode(parent, v);
		node.setArcWidth(scaleX(0.5));
		node.setArcHeight(scaleY(0.5));
		node.setFill(v.getColor());
		node.setStroke(Color.BLACK);
		node.setStrokeWidth(.5);
		node.setOpacity(1);

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
			for (LayoutEdge e : v.getInnerGraph().getEdges().values()) {
				e.draw(node);
				e.setVisible(v.isEdgeVisible());
			}

			for (AbstractLayoutVertex child : v.getInnerGraph().getVertices().values())
				drawEdges(child);
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
