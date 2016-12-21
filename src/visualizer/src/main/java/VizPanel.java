
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
import javafx.scene.control.ScrollPane;
import javafx.scene.Group;

import java.awt.Color;
import java.util.Iterator;

public class VizPanel extends JFXPanel
{
	private boolean context;
	private Group contentGroup;
	private ScrollPane scrollPane;

	public static float hues[]; //Used for shading nodes from green to red
	public boolean showEdge = true;

	private AbstractVertex panelRoot;
	private javafx.scene.paint.Color[] colors = {javafx.scene.paint.Color.AQUAMARINE,
			javafx.scene.paint.Color.GREEN, javafx.scene.paint.Color.AZURE,
			javafx.scene.paint.Color.BLUEVIOLET, javafx.scene.paint.Color.DARKTURQUOISE};
	private int index = 0;

	public AbstractVertex getPanelRoot()
	{
		return this.panelRoot;
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
			this.panelRoot = LayerFactory.get2layer(g);
			LayoutAlgorithm.layout(this.panelRoot);
		}
		else
		{
			this.panelRoot = root;
		}
		draw(null, this.panelRoot);
	}

	public double scaleX(double coordinate)
	{
		return (coordinate * 500.0 / this.panelRoot.getWidth());
	}

	public double scaleY(double coordinate)
	{
		return (coordinate * 500.0 / this.panelRoot.getHeight());
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

		if(this.showEdge)
		{
			Iterator<Edge> itEdge = v.getInnerGraph().getEdges().values().iterator();
			while (itEdge.hasNext()) {
				Edge e = itEdge.next();
				e.draw(this, node);
			}
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

	// Currently unused: Do we need it?
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
