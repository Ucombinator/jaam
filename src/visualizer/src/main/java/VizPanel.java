
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ScrollPane;
import javafx.scene.Group;

import java.awt.Color;
import java.util.Iterator;

public class VizPanel extends JFXPanel
{
	private boolean context;
	private Group contentGroup;
	private ScrollPane scrollPane;
	double boxWidth, boxHeight; // Are these initialized?

	public boolean showSelection = false;
	public double selectLeft, selectRight, selectTop, selectBottom;
	public double boxSize = 1.0;
	public static float hues[]; //Used for shading nodes from green to red
	public boolean showEdge = true;
	private Rectangle currentView; // The current view is highlighted in the context window
	private Rectangle selection; // While dragging, show the current selection in the main window.

	private AbstractVertex main;
	private javafx.scene.paint.Color[] colors = {javafx.scene.paint.Color.AQUAMARINE,
			javafx.scene.paint.Color.GREEN, javafx.scene.paint.Color.AZURE,
			javafx.scene.paint.Color.BLUEVIOLET, javafx.scene.paint.Color.DARKTURQUOISE};
	private int index = 0;

	public AbstractVertex getRoot()
	{
		return this.main;
	}

	public VizPanel(boolean isContextPanel)
	{
		super();
		this.context = isContextPanel;
		contentGroup = new Group();
		scrollPane = createZoomPane(contentGroup);
		this.setScene(new Scene(scrollPane));
		this.setBackground(Color.WHITE);
	}

	public void initFX(AbstractVertex root)
	{
		// TODO: Put something useful on the context panel.
		if(this.context)
			return;

		if(root == null)
		{
			Graph g = Main.graph;			
			this.main = LayerFactory.get2layer(g);
			LayoutAlgorithm.layout(this.main);
		}
		else
		{
			this.main = root;
		}
		draw(null, this.main);
	}

	public double scaleX(double coordinate)
	{
		return (coordinate * 500.0 / this.main.getWidth());
	}

	public double scaleY(double coordinate)
	{
		return (coordinate * 500.0 / this.main.getHeight());
	}

	public void draw(GUINode parent, AbstractVertex v)
	{
		GUINode node = new GUINode(parent, v);

		if (parent == null)
			contentGroup.getChildren().add(node);
		else
			parent.getChildren().add(node);

		// v.printCoordinates();
		double layoutX = scaleX(v.getX());
		double layoutY = scaleY(v.getY());
		double width = scaleX(v.getWidth());
		double height = scaleY(v.getHeight());
		node.setLocation(layoutX, layoutY, width, height);

		node.setArcWidth(scaleX(0.5));
		node.setArcHeight(scaleY(0.5));
		node.setLabel("  " + v.getLabel());

		node.setFill(colors[index++ % colors.length]);
		node.setStroke(javafx.scene.paint.Color.BLACK);
		node.setStrokeWidth(0);
		node.setOpacity(1);
		

		if (v.getInnerGraph().getVertices().size() == 0)
			return;

		Iterator<Edge> itEdge = v.getInnerGraph().getEdges().values().iterator();
		while (itEdge.hasNext())
		{
			Edge e = itEdge.next();
			e.draw(this, node);
		}

		Iterator<AbstractVertex> it = v.getInnerGraph().getVertices().values().iterator();
		while (it.hasNext())
		{
			draw(node, it.next());
		}
	}

	public static void computeHues()
	{
		float start = 0.4f; //green
		float end = 0.0f; //red
		
		int maxLoopHeight = 0;
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
	}

	// Currently unused.
	// Checks if an edge between two different vertices should be highlighted
	// We highlight it only if the vertices on both ends want it to be highlighted
	public boolean isHighlightedEdge(AbstractVertex v, AbstractVertex nbr)
	{
		if(!v.isOutgoingHighlighted || !nbr.isIncomingHighlighted)
			return false;
		else if(Parameters.highlightOutgoing && !Parameters.highlightIncoming && v.isOutgoingHighlighted)
			return false;
		else return !(Parameters.highlightIncoming && !Parameters.highlightOutgoing && nbr.isIncomingHighlighted);
	}

	// Convert a current pixel location to a horizontal value between 0 and 1
	public double getRelativeFracFromAbsolutePixelX(double x)
	{
		Graph graph = Main.graph;
		if(this.context)
		{
			double xFrac = x / (this.boxWidth * graph.getWidth());
			if(xFrac < 0)
				return 0;
			else if(xFrac > 1)
				return 1;
			else
				return xFrac;
		}
		else
		{
			double xFrac = x / (this.boxWidth * graph.getWidth()) + graph.currWindow.left;
			if(xFrac < 0)
				return 0;
			else if(xFrac > 1)
				return 1;
			else
				return xFrac;
		}
	}

	//Convert an absolute pixel location to a vertical value between 0 and 1
	public double getRelativeFracFromAbsolutePixelY(double y)
	{
		Graph graph = Main.graph;
		if(this.context)
		{
			double yFrac = y / (this.boxHeight * graph.getHeight());
			if(yFrac < 0)
				return 0;
			else if(yFrac > 1)
				return 1;
			else
				return yFrac;
		}
		else
		{
			double yFrac = y / (this.boxHeight * graph.getHeight()) + graph.currWindow.top;
			if(yFrac < 0)
				return 0;
			else if(yFrac > 1)
				return 1;
			else
				return yFrac;
		}
	}

	//Convert a horizontal box index to a current x pixel location
	public double getPixelXFromIndex(double x)
	{
		return this.boxWidth * x;
	}

	//Convert a vertical box index to a current y pixel location
	public double getPixelYFromIndex(double y)
	{
		return this.boxHeight * y;
	}

	public double getIndexFromCurrentPixelX(double currXPixel)
	{
		double xFrac = getRelativeFracFromAbsolutePixelX(currXPixel);
		return xFrac * Main.graph.getWidth();
	}

	public double getIndexFromCurrentPixelY(double currYPixel)
	{
		double yFrac = getRelativeFracFromAbsolutePixelY(currYPixel);
		return yFrac * Main.graph.getHeight();
	}

	// Next three methods copied from solution here: https://community.oracle.com/thread/2541811
	// Feature request (inactive) to have an easier way to zoom inside a ScrollPane:
	// https://bugs.openjdk.java.net/browse/JDK-8091618
	private ScrollPane createZoomPane(final Group group)
	{
		final double SCALE_DELTA = 1.1;
		final StackPane zoomPane = new StackPane();

		zoomPane.getChildren().add(group);

		final ScrollPane scroller = new ScrollPane();
		final Group scrollContent = new Group(zoomPane);
		scroller.setContent(scrollContent);

		scroller.viewportBoundsProperty().addListener(new ChangeListener<Bounds>()
		{
			@Override
			public void changed(ObservableValue<? extends Bounds> observable,
								Bounds oldValue, Bounds newValue)
			{
				zoomPane.setMinSize(newValue.getWidth(), newValue.getHeight());
			}
		});

		zoomPane.setOnScroll(new EventHandler<ScrollEvent>()
		{
			@Override
			public void handle(ScrollEvent event)
			{
				event.consume();

				if (event.getDeltaY() == 0)
					return;

				double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA
						: 1 / SCALE_DELTA;

				// amount of scrolling in each direction in scrollContent coordinate
				// units
				Point2D scrollOffset = figureScrollOffset(scrollContent, scroller);

				group.setScaleX(group.getScaleX() * scaleFactor);
				group.setScaleY(group.getScaleY() * scaleFactor);

				// move viewport so that old center remains in the center after the
				// scaling
				repositionScroller(scrollContent, scroller, scaleFactor, scrollOffset);
			}
		});

		// Panning via drag....
		final ObjectProperty<Point2D> lastMouseCoordinates = new SimpleObjectProperty<Point2D>();
		scrollContent.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				lastMouseCoordinates.set(new Point2D(event.getX(), event.getY()));
			}
		});

		scrollContent.setOnMouseDragged(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				double deltaX = event.getX() - lastMouseCoordinates.get().getX();
				double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
				double deltaH = deltaX * (scroller.getHmax() - scroller.getHmin()) / extraWidth;
				double desiredH = scroller.getHvalue() - deltaH;
				scroller.setHvalue(Math.max(0, Math.min(scroller.getHmax(), desiredH)));

				double deltaY = event.getY() - lastMouseCoordinates.get().getY();
				double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
				double deltaV = deltaY * (scroller.getHmax() - scroller.getHmin()) / extraHeight;
				double desiredV = scroller.getVvalue() - deltaV;
				scroller.setVvalue(Math.max(0, Math.min(scroller.getVmax(), desiredV)));
			}
		});

		return scroller;
	}

	private Point2D figureScrollOffset(Node scrollContent, ScrollPane scroller)
	{
		double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
		double hScrollProportion = (scroller.getHvalue() - scroller.getHmin()) / (scroller.getHmax() - scroller.getHmin());
		double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
		double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
		double vScrollProportion = (scroller.getVvalue() - scroller.getVmin()) / (scroller.getVmax() - scroller.getVmin());
		double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
		return new Point2D(scrollXOffset, scrollYOffset);
	}

	private void repositionScroller(Node scrollContent, ScrollPane scroller, double scaleFactor, Point2D scrollOffset)
	{
		double scrollXOffset = scrollOffset.getX();
		double scrollYOffset = scrollOffset.getY();
		double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
		if (extraWidth > 0)
		{
			double halfWidth = scroller.getViewportBounds().getWidth() / 2 ;
			double newScrollXOffset = (scaleFactor - 1) *  halfWidth + scaleFactor * scrollXOffset;
			scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
		}
		else
		{
			scroller.setHvalue(scroller.getHmin());
		}

		double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
		if (extraHeight > 0)
		{
			double halfHeight = scroller.getViewportBounds().getHeight() / 2 ;
			double newScrollYOffset = (scaleFactor - 1) * halfHeight + scaleFactor * scrollYOffset;
			scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
		}
		else
		{
			scroller.setHvalue(scroller.getHmin());
		}
	}
}
