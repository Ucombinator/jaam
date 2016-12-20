
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.control.ScrollPane;
import javafx.scene.Group;

import java.awt.Color;
import java.util.Iterator;

public class VizPanel extends JFXPanel
{
	private boolean context;
	private Group contentGroup;
	private ScrollPane scrollPane;
	double minWidth, minHeight, boxWidth, boxHeight;

	public boolean showSelection = false;
	public double selectLeft, selectRight, selectTop, selectBottom;
	public double boxSize = 1.0;
	public static float hues[]; //Used for shading nodes from green to red
	public boolean showEdge = true;
	private Rectangle currentView; // The current view is highlighted in the context window
	private Rectangle selection; // While dragging, show the current selection in the main window.

	private AbstractVertex main;
	private javafx.scene.paint.Color[] colors = {javafx.scene.paint.Color.RED,
			javafx.scene.paint.Color.GREEN, javafx.scene.paint.Color.AZURE,
			javafx.scene.paint.Color.BLUEVIOLET, javafx.scene.paint.Color.DARKTURQUOISE};
	private int index = 0;

	public VizPanel(boolean cont)
	{
		super();
		this.context = cont;
		contentGroup = new Group();
		scrollPane = createZoomPane(contentGroup);
		this.setScene(new Scene(scrollPane));
		this.setBackground(Color.WHITE);
	}

	public void initFX()
	{
		Graph g = Main.graph;
		this.main = LayerFactory.get2layer(g);
		LayoutAlgorithm.layout(main);
		draw(null, main);
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

	public void updateVertices()
	{
		for(Vertex ver : Main.graph.vertices)
		{
			this.updateVertex(ver);
		}

		for(MethodVertex ver : Main.graph.methodVertices)
		{
			this.updateVertex(ver);
		}

		for(MethodPathVertex ver : Main.graph.methodPathVertices)
		{
			this.updateVertex(ver);
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
	
	public void initVertex(AbstractVertex ver)
	{
		Text text;
		if (this.context)
		{
			text = ver.contextNode.rectLabel;
			text.setText(Integer.toString(ver.id));
			contentGroup.getChildren().add(ver.contextNode);
		}
		else
		{
			text = ver.mainNode.rectLabel;
			// TODO: Determine if we should use the full name of the vertex in the main panel
			// text.setText(ver.getName());
			text.setText(Integer.toString(ver.id));
			contentGroup.getChildren().add(ver.mainNode);
		}

		updateVertex(ver);
	}

	public void updateVertex(AbstractVertex ver)
	{
		GUINode node;
		if (this.context)
			node = ver.mainNode;
		else
			node = ver.contextNode;

		Graph graph = Main.graph;
		double x1temp, x2temp, y1temp, y2temp;
		double x1, x2, y1, y2;
		double fac = 0.25*this.boxSize;
		
		if(this.boxWidth < this.minWidth)
		{
			x1temp = ver.location.x - fac*this.minWidth/this.boxWidth;
			x2temp = ver.location.x + fac*this.minWidth/this.boxWidth;
		}
		else
		{
			x1temp = ver.location.x - fac;
			x2temp = ver.location.x + fac;
		}
		
		if(!this.context)
		{
			x1temp -= graph.currWindow.left*graph.getWidth();
			x2temp -= graph.currWindow.left*graph.getWidth();
		}
		
		x1 = this.getPixelXFromIndex(x1temp);
		x2 = this.getPixelXFromIndex(x2temp);
		
		
		if(this.boxHeight < this.minHeight)
		{
			y1temp = ver.location.y - fac*this.minHeight/this.boxHeight;
			y2temp = ver.location.y + fac*this.minHeight/this.boxHeight;
		}
		else
		{
			y1temp = ver.location.y - fac;
			y2temp = ver.location.y + fac;
		}
		
		if(!this.context)
		{
			y1temp -= graph.currWindow.top*graph.getHeight();
			y2temp -= graph.currWindow.top*graph.getHeight();
		}
		
		y1 = this.getPixelYFromIndex(y1temp);
		y2 = this.getPixelYFromIndex(y2temp);
		node.setLocation(x1, y1, x2, y2);

		/*if(!this.context)
		{
			System.out.println("Setting vertex " + ver.id);
			System.out.println("(" + ver.location.x + ", " + ver.location.y + ")");
			System.out.println("(" + x1 + ", " + y1 + "),  (" + x2 + ", " + y2 + ")");
		}*/

		if(Parameters.vertexHighlight && (ver.isSelected() || ver.isChildSelected()))
		{
			node.setFill(Parameters.fxColorHighlight);
		}
		else if(Parameters.vertexHighlight && (ver.isHighlighted() || ver.isChildHighlighted()))
		{
			node.setFill(Parameters.fxColorSelection);
		}
		else
		{
			//Hue ranges from green to red
			//Allows for varying shades up to the maximum number of nested loops
			float hue = hues[0];
			if(ver.loopHeight > 0)
				hue = hues[Math.min(hues.length - 1, ver.loopHeight)];
			
			float sat = 1f;
			float brightness = 1f;
			Color c = getHSBColorT(hue, sat, brightness);

			// TODO: Fix this
			node.setFill(javafx.scene.paint.Color.GREENYELLOW);
			node.setOpacity(1);
			//System.out.println("Setting color to green...");
			//rect.setFill(toJavaFXColor(c));
		}

		//Draw outline of boxes only in main window or in context for selected or highlighted vertices
		if(this.context)
		{
            if(Parameters.vertexHighlight && (ver.isSelected() || ver.isChildSelected() || ver.isHighlighted()
					|| ver.isChildHighlighted()))
            {
				node.setStroke(javafx.scene.paint.Color.BLACK);
            }
		}
        else
        {
           node.setStroke(javafx.scene.paint.Color.BLACK);
        }
        
        // TODO: Re-implement ping?
        /*if(Parameters.pingStart && ver.isSelected)
        {
            int width = x2-x1;
            int height = y2-y1;
            if(width>height)
                height = width;
            else
                width = height;
            
            int x = (x1+x2)/2 - width;
            int y = (y1+y2)/2 - height;
            
            g.setColor(Color.BLUE);
            g.drawOval(x,y,2*width,2*height);
            
            if(Parameters.pingEnd)
            {
                if(this.context)
                    Parameters.pingRespondedContext = true;
                else
                    Parameters.pingRespondedMain = true;
                if(Parameters.pingRespondedMain && Parameters.pingRespondedContext)
                    Parameters.pingStart = false;
            }
        }*/
	}
	
	//If edges are turned on, draw all visible graph edges.
	public void initEdges()
	{
		Graph graph = Main.graph;
		for(Edge e : graph.edges)
		{
			AbstractVertex v = graph.vertices.get(e.source);
			AbstractVertex w = graph.vertices.get(e.dest);

			double x1temp, x2temp, y1temp, y2temp;
			int x1, x2, y1, y2;
			double fac = 0.25*this.boxSize;
			boolean isCurved;


			x1temp = v.location.x;
			x2temp = w.location.x;

			if(!this.context)
			{
				x1temp -= graph.getWidth()*graph.currWindow.left;
				x2temp -= graph.getWidth()*graph.currWindow.left;
			}

			x1 = (int) this.getPixelXFromIndex(x1temp);
			x2 = (int) this.getPixelXFromIndex(x2temp);

			//Start of arrow is at center of first box
			y1temp = v.location.y;

			// TODO: Curved edges?
			//If box 1 is above box 2, draw arrow to the center of the topMargin line
			if(v.location.y > w.location.y)
			{
				if(this.boxHeight < this.minHeight)
				{
					y2temp = w.location.y + fac*this.minHeight/this.boxHeight;
				}
				else
				{
					y2temp = w.location.y + fac;
				}
			}

			//Otherwise, draw arrow to the center of the bottom line
			else
			{
				if(this.boxHeight < this.minHeight)
				{
					y2temp = w.location.y - fac*this.minHeight/this.boxHeight;
				}
				else
				{
					y2temp = w.location.y - fac;
				}
			}

			if(!this.context)
			{
				y1temp -= graph.currWindow.top*graph.getHeight();
				y2temp -= graph.currWindow.top*graph.getHeight();
			}

			y1 = (int) this.getPixelYFromIndex(y1temp);
			y2 = (int) this.getPixelYFromIndex(y2temp);

			/*e.line.setStartX(x1);
			e.line.setEndX(x2);
			e.line.setStartY(y1);
			e.line.setEndY(y2);

			if(isHighlightedEdge(v, w))
			{
				e.line.setFill(javafx.scene.paint.Color.RED);
				e.line.setStrokeWidth(4);
			}
			else
			{
				e.line.setFill(javafx.scene.paint.Color.BLACK);
				e.line.setStrokeWidth(1);
			}*/
		}
	}

	public void update()
	{
		/*ParallelTransition parTrans = new ParallelTransition();
		for(Vertex v : Main.graph.vertices)
		{
			if(v.updateLocation)
			{
				System.out.println("Updating location of vertex: " + v.getName());
				v.updateLocation = false;
				parTrans.getChildren().add(v.constructPathTransition(this.context));
			}
		}

		for(MethodVertex v : Main.graph.methodVertices)
		{
			if(v.updateLocation)
			{
				System.out.println("Updating location of vertex: " + v.getName());
				v.updateLocation = false;
				parTrans.getChildren().add(v.constructPathTransition(this.context));
			}
		}

		for(MethodPathVertex v : Main.graph.methodPathVertices)
		{
			if(v.updateLocation)
			{
				System.out.println("Updating location of vertex: " + v.getName());
				v.updateLocation = false;
				parTrans.getChildren().add(v.constructPathTransition(this.context));
			}
		}

		for(Vertex v : Main.graph.vertices)
		{
			if(v.isVisible)
			{
				System.out.println("Visible vertex: " + v.getName());
				System.out.println(v.location);
			}
		}

		for(MethodVertex v : Main.graph.methodVertices)
		{
			if(v.isVisible)
			{
				System.out.println("Visible vertex: " + v.getName());
				System.out.println(v.location);
			}
		}

		for(MethodPathVertex v : Main.graph.methodPathVertices)
		{
			if(v.isVisible)
			{
				System.out.println("Visible vertex: " + v.getName());
				System.out.println(v.location);
			}
		}

		if(parTrans.getChildren().size() > 0)
			parTrans.play();*/
	}
	
	//Checks if an edge between two different vertices should be highlighted
	//We highlight it only if the vertices on both ends want it to be highlighted.
	public boolean isHighlightedEdge(AbstractVertex v, AbstractVertex nbr)
	{
		if(!v.isOutgoingHighlighted || !nbr.isIncomingHighlighted)
			return false;
		else if(Parameters.highlightOutgoing && !Parameters.highlightIncoming && v.isOutgoingHighlighted)
			return false;
		else return !(Parameters.highlightIncoming && !Parameters.highlightOutgoing && nbr.isIncomingHighlighted);
	}

	private double getEuclideanDistance(double x1, double y1, double x2, double y2)
	{
		return Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
	}
	
	public Color getHSBColorT(float H, float S, float B)
	{
		int rgb = Color.HSBtoRGB(H, S, B);
	    int red = (rgb >> 16) & 0xFF;
	    int green = (rgb >> 8) & 0xFF;
	    int blue = rgb & 0xFF;
	    Color color = new Color(red, green, blue, Parameters.transparency);
	    return color;
	}
	
	public Color getColorT(int rgb)
	{
	    int red = (rgb >> 16) & 0xFF;
	    int green = (rgb >> 8) & 0xFF;
	    int blue = rgb & 0xFF;
	    Color color = new Color(red, green, blue, Parameters.transparency);
	    return color;
	}

	//Convert a current pixel location to a horizontal value between 0 and 1
	public double getRelativeFracFromAbsolutePixelX(double x)
	{
		Graph graph = Main.graph;
		if(this.context)
		{
			double xFrac = x / (this.boxWidth*graph.getWidth());
			if(xFrac < 0)
				return 0;
			else if(xFrac > 1)
				return 1;
			else
				return xFrac;
		}
		else
		{
			double xFrac = x / (this.boxWidth*graph.getWidth()) + graph.currWindow.left;
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

	public javafx.scene.paint.Color toJavaFXColor(java.awt.Color c)
	{
		double r = c.getRed()/255.0;
		double g = c.getGreen()/255.0;
		double b = c.getBlue()/255.0;
		double a = c.getAlpha()/255.0;
		return new javafx.scene.paint.Color(r, g, b, a);
	}


	// Next three methods opied from solution here: https://community.oracle.com/thread/2541811
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
