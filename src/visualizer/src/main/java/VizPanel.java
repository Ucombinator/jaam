import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.embed.swing.JFXPanel;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.Group;

import java.awt.Color;
import java.awt.Graphics;

public class VizPanel extends JFXPanel
{
	private boolean context;
	private BorderPane root;
	private Group paneContent;
	private ZoomableScrollPane scrollPane;
	private double maxWidth, maxHeight, minWidth, minHeight, boxWidth, boxHeight;

	//The leftMargin margin is the width of the panel minus the width of the currently displayed graph, divided by 2.
	//The topMargin margin is the height of the panel minus the height of the currently displayed graph, divided by 2.
	private double leftMargin, topMargin;

	public boolean showSelection = false;
	public double selectLeft, selectRight, selectTop, selectBottom;
	public double boxSize = 1.0;
	public static float hues[]; //Used for shading nodes from green to red
	public boolean showEdge = true;
	private Rectangle currentView; // The current view is highlighted in the context window
	private Rectangle selection; // While dragging, show the current selection in the main window.

	public VizPanel(boolean cont)
	{
		super();
		this.context = cont;
		this.createScene();
	}

	public void createScene()
	{
		paneContent = new Group();
		scrollPane = new ZoomableScrollPane(paneContent);
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);

		root = new BorderPane();
		root.setCenter(scrollPane);

		Scene scene = new Scene(root);
		this.setScene(scene);
	}

	public void initDrawing()
	{
		if(Main.graph == null)
		{
			System.out.println("How did we get here? We should never initialize a drawing until a graph has been read.");
			return;
		}

		// Initially, the current view is everything, and nothing is selected.
		if(this.context)
		{
			currentView = new Rectangle();
			currentView.setVisible(false);
		}
		else
		{
			selection = new Rectangle();
			selection.setVisible(false);
		}

		initParameters();
		initVertices();
		initEdges();
		this.setVisible(true);

		AbstractVertex v = Main.graph.methodPathVertices.get(0);
		System.out.println(v.mainRect.toString());
	}

	public void initParameters()
	{
		this.minWidth = Parameters.minBoxWidth;
		this.minHeight = Parameters.minBoxHeight;
		this.maxWidth = this.getWidth();
		this.maxHeight = this.getHeight();

		if(this.maxWidth<this.minWidth)
			this.maxWidth = this.minWidth;
		if(this.maxHeight<this.minHeight)
			this.maxHeight = this.minHeight;

		Graph graph = Main.graph;
		//Provide a margin of 2% on each side?
		this.boxWidth = this.getWidth()*0.96/(graph.getWidth()*(graph.currWindow.right-graph.currWindow.left));
		this.boxHeight = this.getHeight()*0.96/(graph.getHeight()*(graph.currWindow.bottom-graph.currWindow.top));

		if(this.boxWidth > this.maxWidth)
		{
			this.boxWidth = this.maxWidth;
		}

		if(this.boxHeight > this.maxHeight)
		{
			this.boxHeight = this.maxHeight;
		}

		this.leftMargin = (this.getWidth() - this.boxWidth*graph.getWidth()
				*(graph.currWindow.right - graph.currWindow.left))/2;
		this.topMargin = (this.getHeight() - this.boxHeight*graph.getHeight()
				*(graph.currWindow.bottom - graph.currWindow.top))/2;
	}

	public void initVertices()
	{
		for(Vertex ver : Main.graph.vertices)
		{
			this.initVertex(ver);
		}

		for(MethodVertex ver : Main.graph.methodVertices)
		{
			this.initVertex(ver);
		}

		for(MethodPathVertex ver : Main.graph.methodPathVertices)
		{
			this.initVertex(ver);
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

		for(MethodPathVertex ver : Main.graph.methodPathVertices) {
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
	
	public void initVertex(AbstractVertex ver) {
		StackPane rectPane;
		Rectangle rect;
		Text text;
		if (this.context) {
			text = ver.contextRectLabel;
			text.setText(Integer.toString(ver.id));
			paneContent.getChildren().add(ver.contextRectPane);
		} else {
			text = ver.mainRectLabel;
			text.setText(ver.getName());
			paneContent.getChildren().add(ver.mainRectPane);
		}

		updateVertex(ver);
	}

	public void updateVertex(AbstractVertex ver)
	{
		StackPane rectPane;
		Rectangle rect;
		if (this.context) {
			rectPane = ver.contextRectPane;
			rect = ver.contextRect;
		}
		else
		{
			rectPane = ver.mainRectPane;
			rect = ver.mainRect;
		}

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

		//this.root.setLeftAnchor(rectPane, x1);
		//this.root.setTopAnchor(rectPane, y1);
		rectPane.setLayoutX(x1);
		rectPane.setLayoutY(y1);
		rect.setWidth(x2 - x1);
		rect.setHeight(y2 - y1);

		/*if(!this.context)
		{
			System.out.println("Setting vertex " + ver.id);
			System.out.println("(" + x1 + ", " + y1 + "),  (" + x2 + ", " + y2 + ")");
		}*/

		if(Parameters.vertexHighlight && (ver.isSelected() || ver.isChildSelected()))
		{
			rect.setFill(Parameters.fxColorHighlight);
		}
		else if(Parameters.vertexHighlight && (ver.isHighlighted() || ver.isChildHighlighted()))
		{
			rect.setFill(Parameters.fxColorSelection);
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
			rect.setFill(javafx.scene.paint.Color.GREENYELLOW);
			rect.setOpacity(1);
			//System.out.println("Setting color to green...");
			//rect.setFill(toJavaFXColor(c));
		}

		//Draw outline of boxes only in main window or in context for selected or highlighted vertices
		if(this.context)
		{
            if(Parameters.vertexHighlight && (ver.isSelected() || ver.isChildSelected() || ver.isHighlighted()
					|| ver.isChildHighlighted()))
            {
				rect.setStroke(javafx.scene.paint.Color.BLACK);
            }
		}
        else
        {
           rect.setStroke(javafx.scene.paint.Color.BLACK);
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

			e.line.setStartX(x1);
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
			}
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
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		this.setBackground(Color.WHITE);
	}

	//Convert a current pixel location to a horizontal value between 0 and 1
	public double getRelativeFracFromAbsolutePixelX(double x)
	{
		Graph graph = Main.graph;
		if(this.context)
		{
			double xFrac = (x - this.leftMargin) / (this.boxWidth*graph.getWidth());
			if(xFrac < 0)
				return 0;
			else if(xFrac > 1)
				return 1;
			else
				return xFrac;
		}
		else
		{
			double xFrac = (x - this.leftMargin) / (this.boxWidth*graph.getWidth()) + graph.currWindow.left;
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
			double yFrac = (y - this.topMargin) / (this.boxHeight * graph.getHeight());
			if(yFrac < 0)
				return 0;
			else if(yFrac > 1)
				return 1;
			else
				return yFrac;
		}
		else
		{
			double yFrac = (y - this.topMargin) / (this.boxHeight * graph.getHeight()) + graph.currWindow.top;
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
		return this.leftMargin + this.boxWidth * x;
	}

	//Convert a vertical box index to a current y pixel location
	public double getPixelYFromIndex(double y)
	{
		return this.topMargin + this.boxHeight * y;
	}

	public double getIndexFromCurrentPixelX(double currXPixel)
	{
		double xFrac = getRelativeFracFromAbsolutePixelX(currXPixel);
		return xFrac*Main.graph.getWidth();
	}

	public double getIndexFromCurrentPixelY(double currYPixel)
	{
		double yFrac = getRelativeFracFromAbsolutePixelY(currYPixel);
		return yFrac*Main.graph.getHeight();
	}

	public javafx.scene.paint.Color toJavaFXColor(java.awt.Color c)
	{
		double r = c.getRed()/255.0;
		double g = c.getGreen()/255.0;
		double b = c.getBlue()/255.0;
		double a = c.getAlpha()/255.0;
		return new javafx.scene.paint.Color(r, g, b, a);
	}
}
