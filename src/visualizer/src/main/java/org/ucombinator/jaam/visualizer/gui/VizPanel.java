package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ScaleTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import javafx.scene.control.ScrollPane;
import javafx.scene.Group;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Iterator;

import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.main.Parameters;

public class VizPanel extends ScrollPane
{
	private Group contentGroup;
	public HashSet<AbstractLayoutVertex> highlighted;

	public static float hues[]; //Used for shading nodes from green to red
	private LayoutRootVertex panelRoot;
	private Color[] colors = {Color.GREEN, Color.AZURE, Color.AQUAMARINE, Color.BLUEVIOLET, Color.DARKTURQUOISE};
	public static int maxLoopHeight;

	// The dimensions of the background for our graph
	public final double rootWidth = 500.0, rootHeight = 500.0;

	// Store the count for vertex width and height when everything is expanded
	public double maxVertexWidth, maxVertexHeight;

	public LayoutRootVertex getPanelRoot()
	{
		return this.panelRoot;
	}

	public VizPanel()
	{
		super();
		contentGroup = new Group();
		contentGroup.setVisible(true);
		this.setContent(contentGroup);

		createZoomPane();
		highlighted = new HashSet<AbstractLayoutVertex>();
	}

	public void initFX(LayoutRootVertex root)
	{
		if(root == null)
		{
			//System.out.println("Running layout...");
			Graph g = Main.graph;
			this.panelRoot = LayerFactory.getLayeredGraph(g);
			LayoutAlgorithm.layout(this.panelRoot);
			resetPanelSize();
		}
		else
		{
			this.panelRoot = root;
		}

		drawNodes(null, this.panelRoot);
		drawEdges(this.panelRoot);
	}

	public void resetContent() {
		contentGroup = new Group();
		contentGroup.setVisible(true);
		this.setContent(contentGroup);
	}

	public void resetPanelSize() {
		this.maxVertexWidth = this.panelRoot.getWidth();
		this.maxVertexHeight = this.panelRoot.getHeight();		
	}

	double factorX = 1;
	double factorY = 1;
	double factorMultiple = 1.1;
	double maxFactorMultiple = 3;

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
		return contentGroup.getScaleX();
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
				v.setHighlighted(false);
			}
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
				highlighted.remove(v);
				v.setHighlighted(false);
			}
		}
	}

	public void resetHighlighted(AbstractLayoutVertex newHighlighted)
	{
		for(AbstractLayoutVertex currHighlighted : this.highlighted)
			currHighlighted.setHighlighted(false);
		highlighted = new HashSet<AbstractLayoutVertex>();

		if(newHighlighted != null) {
			highlighted.add(newHighlighted);
			newHighlighted.setHighlighted(true);
		}
	}

	public void drawNodes(GUINode parent, AbstractLayoutVertex v)
	{
		GUINode node = new GUINode(parent, v);

		if (parent == null) {
			contentGroup.getChildren().add(node);
		}
		else {
			parent.getChildren().add(node);
		}

		double translateX = scaleX(v.getX());
		double translateY = scaleY(v.getY());
		double width = scaleX(v.getWidth());
		double height = scaleY(v.getHeight());
		node.setLocation(translateX, translateY, width, height);

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
			contentGroup.getChildren().add(node);
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

	public void drawEdges(AbstractLayoutVertex v)
	{
		System.out.println("Edges of vertex: " + v.getStrID());
		if(!Parameters.edgeVisible){
			return;
		}
		
		GUINode node = v.getGraphics();
		if(v.isExpanded())
		{
			System.out.println(v.getStrID());

			//Edge.arrowLength = this.getWidthPerVertex() / 10.0;
			for(LayoutEdge e : v.getInnerGraph().getEdges().values()) {
				System.out.println("Drawing edge: " + e.getID());
				e.draw(node);
			}
		
			for(AbstractLayoutVertex child : v.getInnerGraph().getVertices().values())
				drawEdges(child);
		}
	}

	/*public static void computeHues()
	{
		float start = 0.4f; //green
		float end = 0.0f; //red

		VizPanel.maxLoopHeight = 0;
		for(Vertex v : Main.graph.vertices)
		{
			if(v.loopHeight > maxLoopHeight)
				maxLoopHeight = v.loopHeight;
		}
		
		System.out.println("Max loop height: " + maxLoopHeight);
		
		hues = new float[maxLoopHeight + 1];
		for(int i = 0; i <= maxLoopHeight; i++)
		{
			// Linear interpolation of color values
			hues[i] = start - ((float) i)/(maxLoopHeight + 1)*(start - end);
		}
	}*/

	// Next three methods copied from solution here: https://community.oracle.com/thread/2541811
	// Feature request (inactive) to have an easier way to zoom inside a ScrollPane:
	// https://bugs.openjdk.java.net/browse/JDK-8091618
	private void createZoomPane()
	{
		final double SCALE_DELTA = 1.1;
		this.viewportBoundsProperty().addListener(new ChangeListener<Bounds>()
		{
			@Override
			public void changed(ObservableValue<? extends Bounds> observable,
								Bounds oldValue, Bounds newValue)
			{
				VizPanel.this.setMinSize(newValue.getWidth(), newValue.getHeight());
			}
		});

		
		final EventHandler<ScrollEvent> zoomInProgressHandle =  new EventHandler<ScrollEvent>()
		{
			@Override
			public void handle(ScrollEvent event)
			{
				event.consume();
				System.out.println("zoomInProgressHandle");
			}
		}; 
		

		EventHandler<ScrollEvent> activeHandle = new EventHandler<ScrollEvent>()
		{
			@Override
			public void handle(ScrollEvent event)
			{
				event.consume();
				System.out.println("ZOOM: " + event.getDeltaY());
				//VizPanel.this.setOnScroll(zoomInProgressHandle);
				

				if (event.getDeltaY() == 0)
					return;

				final double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA
						: 1 / SCALE_DELTA;

				// amount of scrolling in each direction in scrollContent coordinate units
				final Point2D scrollOffset = figureScrollOffset();

				ScaleTransition st = new ScaleTransition(Duration.millis(5), contentGroup);
				st.setToX(contentGroup.getScaleX() * scaleFactor);
				st.setToY(contentGroup.getScaleX() * scaleFactor);
				Parameters.stFrame.mainPanel.getPanelRoot().toggleEdges();

				st.setOnFinished(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event)
					{

						// TODO: Our repositioning fails miserably.
						// move viewport so that old center remains in the center after the scaling
						//repositionScroller(scrollContent, VizPanel.this, scaleFactor, scrollOffset);

						Parameters.stFrame.mainPanel.getPanelRoot().toggleEdges();
						// Adjust stroke width of lines and length of arrows
						VizPanel.this.scaleLines();
						System.out.println("Total scale: " + contentGroup.getScaleX());
					}
				});

				st.play();
			}
		};

		this.setOnScroll(activeHandle);

		// Panning via drag....
		final ObjectProperty<Point2D> lastMouseCoordinates = new SimpleObjectProperty<Point2D>();
		contentGroup.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				lastMouseCoordinates.set(new Point2D(event.getX(), event.getY()));
			}
		});

		// Fix drag location when node is scaled
		contentGroup.setOnMouseDragged(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				double deltaX = event.getX() - lastMouseCoordinates.get().getX();
				double extraWidth = contentGroup.getLayoutBounds().getWidth() - VizPanel.this.getViewportBounds().getWidth();
				double deltaH = deltaX * (VizPanel.this.getHmax() - VizPanel.this.getHmin()) / extraWidth;
				double desiredH = VizPanel.this.getHvalue() - deltaH;
				VizPanel.this.setHvalue(Math.max(0, Math.min(VizPanel.this.getHmax(), desiredH)));

				double deltaY = event.getY() - lastMouseCoordinates.get().getY();
				double extraHeight = contentGroup.getLayoutBounds().getHeight() - VizPanel.this.getViewportBounds().getHeight();
				double deltaV = deltaY * (VizPanel.this.getHmax() - VizPanel.this.getHmin()) / extraHeight;
				double desiredV = VizPanel.this.getVvalue() - deltaV;
				VizPanel.this.setVvalue(Math.max(0, Math.min(VizPanel.this.getVmax(), desiredV)));
			}
		});
	}

	private Point2D figureScrollOffset()
	{
		double extraWidth = contentGroup.getLayoutBounds().getWidth() - VizPanel.this.getViewportBounds().getWidth();
		double hScrollProportion = (VizPanel.this.getHvalue() - VizPanel.this.getHmin()) / (VizPanel.this.getHmax() - VizPanel.this.getHmin());
		double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
		double extraHeight = contentGroup.getLayoutBounds().getHeight() - VizPanel.this.getViewportBounds().getHeight();
		double vScrollProportion = (VizPanel.this.getVvalue() - VizPanel.this.getVmin()) / (VizPanel.this.getVmax() - VizPanel.this.getVmin());
		double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
		return new Point2D(scrollXOffset, scrollYOffset);
	}

	private void repositionScroller(double scaleFactor, Point2D scrollOffset)
	{
		double scrollXOffset = scrollOffset.getX();
		double scrollYOffset = scrollOffset.getY();
		double extraWidth = contentGroup.getLayoutBounds().getWidth() - VizPanel.this.getViewportBounds().getWidth();
		if (extraWidth > 0)
		{
			double halfWidth = VizPanel.this.getViewportBounds().getWidth() / 2 ;
			double newScrollXOffset = (scaleFactor - 1) *  halfWidth + scaleFactor * scrollXOffset;
			VizPanel.this.setHvalue(VizPanel.this.getHmin() + newScrollXOffset * (VizPanel.this.getHmax() - VizPanel.this.getHmin()) / extraWidth);
		}
		else
		{
			VizPanel.this.setHvalue(VizPanel.this.getHmin());
		}

		double extraHeight = contentGroup.getLayoutBounds().getHeight() - VizPanel.this.getViewportBounds().getHeight();
		if (extraHeight > 0)
		{
			double halfHeight = VizPanel.this.getViewportBounds().getHeight() / 2 ;
			double newScrollYOffset = (scaleFactor - 1) * halfHeight + scaleFactor * scrollYOffset;
			VizPanel.this.setVvalue(VizPanel.this.getVmin() + newScrollYOffset * (VizPanel.this.getVmax() - VizPanel.this.getVmin()) / extraHeight);
		}
		else
		{
			VizPanel.this.setHvalue(VizPanel.this.getHmin());
		}
	}

	public void scaleLines()
	{
		//System.out.println("Scaling lines and arrowheads...");
		for(LayoutEdge e : this.panelRoot.getInnerGraph().getEdges().values())
			e.setScale();

		for(AbstractLayoutVertex v : this.panelRoot.getInnerGraph().getVertices().values())
		{
			for(LayoutEdge e : v.getInnerGraph().getEdges().values())
				e.setScale();
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
