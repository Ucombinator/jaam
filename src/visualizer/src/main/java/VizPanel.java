
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import java.awt.geom.GeneralPath;


public class VizPanel extends JPanel
{
	private boolean context;
	private StacFrame parent;
	private double maxWidth, maxHeight, minWidth, minHeight, boxwidth, boxheight;
	private double left, top;
	public boolean showSelection = false;
	public double selectLeft, selectRight, selectTop, selectBottom;
	public double boxSize = 1.0;
	public static float hues[]; //Used for shading edges from green to red
	public boolean showEdge = true;

	public VizPanel(StacFrame p, boolean cont)
	{
		super();
		this.parent = p;
		this.context = cont;
	}
	
	public void vizPaint(Graphics2D g)
	{		
		Graph graph = StacViz.graph;

		this.boxwidth = this.getWidth()*0.96/(graph.getWidth()*(graph.currWindow.right-graph.currWindow.left));
		this.boxheight = this.getHeight()*0.96/(graph.getHeight()*(graph.currWindow.bottom-graph.currWindow.top));
		
		if(this.boxwidth>this.maxWidth)
		{
			this.boxwidth = this.maxWidth;
		}

		if(this.boxheight>this.maxHeight)
		{
			this.boxheight = this.maxHeight;
		}

		this.left = (this.getWidth()-this.boxwidth*graph.getWidth()*(graph.currWindow.right-graph.currWindow.left))/2;
		this.top = (this.getHeight()-this.boxheight*graph.getHeight()*(graph.currWindow.bottom-graph.currWindow.top))/2;

		//Draw directed edges between connected vertices
		if(this.showEdge)
		{
			for(Vertex ver : graph.vertices)
			{
				AbstractVertex v = ver;
				while(!v.isVisible)
					v = v.getMergeParent();
				
				for(AbstractVertex nbr : ver.neighbors)
				{	
					while(!nbr.isVisible)
						nbr = nbr.getMergeParent();
					
					if(v != nbr && v.drawEdges && nbr.drawEdges)
						drawEdge(g, v, nbr, true);
				}
			}
		}

		//Draw boxes for vertices
		//If a vertex is highlighted, it will be added to highlightedVertices
		for(Vertex ver : graph.vertices)
		{
			if(ver.isVisible)
				drawVertex(g, ver, true);
		}

		for(MethodVertex ver : graph.methodVertices)
		{
			if(ver.isVisible)
				drawVertex(g, ver, true);
		}
		
		for(MethodPathVertex ver : graph.methodPathVertices)
		{
			if(ver.isVisible)
				drawVertex(g, ver, true);
		}
		
		//Draw arrows toward highlighted vertices that are off the main screen
		//drawHighlightedVertexMap(g);
	}
	
	public static void computeHues()
	{
		float start = 0.4f; //green
		float end = 0.0f; //red
		
		int maxLoopHeight = 0;
		for(Vertex v : StacViz.graph.vertices)
		{
			if(v.loopHeight > maxLoopHeight)
				maxLoopHeight = v.loopHeight;
		}
		
		System.out.println("Max loop height: " + maxLoopHeight);
		
		hues = new float[maxLoopHeight + 1];
		for(int i = 0; i <= maxLoopHeight; i++)
		{
			hues[i] = start - ((float) i)/(maxLoopHeight + 1)*(start - end);
		}
	}
	
	public void drawVertex(Graphics2D g, AbstractVertex ver, boolean isMainWindow)
	{
		Graph graph = StacViz.graph;
		double x1temp, x2temp, y1temp, y2temp;
		int x1, x2, y1, y2;
		double fac = 0.25*this.boxSize;
		
		if(this.boxwidth < this.minWidth)
		{
			x1temp = ver.x - fac*this.minWidth/this.boxwidth;
			x2temp = ver.x + fac*this.minWidth/this.boxwidth;
		}
		else
		{
			x1temp = ver.x - fac;
			x2temp = ver.x + fac;
		}
		
		if(isMainWindow)
		{
			x1temp -= graph.currWindow.left*graph.getWidth();
			x2temp -= graph.currWindow.left*graph.getWidth();
		}
		
		x1 = (int) this.getX(x1temp);
		x2 = (int) this.getX(x2temp);
		
		
		if(this.boxheight < this.minHeight)
		{
			y1temp = ver.y - fac*this.minHeight/this.boxheight;
			y2temp = ver.y + fac*this.minHeight/this.boxheight;
		}
		else
		{
			y1temp = ver.y - fac;
			y2temp = ver.y + fac;
		}
		
		if(isMainWindow)
		{
			y1temp -= graph.currWindow.top*graph.getHeight();
			y2temp -= graph.currWindow.top*graph.getHeight();
		}
		
		y1 = (int) this.getY(y1temp);
		y2 = (int) this.getY(y2temp);		
		
		if(Parameters.vertexHighlight && (ver.isHighlighted() || ver.isChildHighlighted()))
		{
			//System.out.println("Drawing highlighted vertex: " + ver.id);
			g.setColor(Parameters.colorHighlight);
			g.fillRect(x1, y1, x2 - x1, y2 - y1);
		}
		else
		{
			//Hue ranges from green to red
			//Allows for varying shades up to 5 nested loops
			float hue = hues[0];
			if(ver.loopHeight > 0)
				hue = hues[Math.min(hues.length - 1, ver.loopHeight)];
			
			float sat = 1f;
			float brightness = 1f;
			Color c = getHSBColorT(hue, sat, brightness);
			g.setColor(c);
			g.fillRect(x1, y1, x2 - x1, y2 - y1);
		}
		
		Font ff = new Font("Serif", Font.BOLD, Parameters.font.getSize());
		if(isMainWindow)
			this.drawCenteredString(g, ver.getName(), (int) (this.getX(ver.x - graph.currWindow.left*graph.getWidth())), (int) (this.getY(ver.y-graph.currWindow.top*graph.getHeight())), ff, Color.BLACK);
		else
			this.drawCenteredString(g, ver.getName(), (int) (this.getX(ver.x)), (int) (this.getY(ver.y)), ff, Color.BLACK);
		
		if(this.boxwidth < this.minWidth || this.boxheight < this.minHeight)
			g.setColor(this.getColorT(Color.BLACK.getRGB()));
		else
			g.setColor(Color.BLACK);
		
		g.setStroke(new BasicStroke(1));
		g.drawRect(x1, y1, x2 - x1, y2 - y1);
	}
	
	//Checks if an edge between two different vertices should be highlighted
	//We highlight it only if the vertices on both ends want it to be highlighted.
	public boolean isHighlightedEdge(AbstractVertex v, AbstractVertex nbr)
	{
		if(!v.isOutgoingHighlighted || !nbr.isIncomingHighlighted)
			return false;
		else if(Parameters.highlightOutgoing && !Parameters.highlightIncoming && v.isOutgoingHighlighted)
			return false;
		else if(Parameters.highlightIncoming && !Parameters.highlightOutgoing && nbr.isIncomingHighlighted)
			return false;
		else
			return true;
	}
	
	public void drawEdge(Graphics2D g, AbstractVertex v, AbstractVertex nbr, boolean isMainWindow)
	{
		Graph graph = StacViz.graph;
		double x1temp, x2temp, y1temp, y2temp;
		int x1, x2, y1, y2;
		double fac = 0.25*this.boxSize;
		boolean isCurved;
		
		
		x1temp = v.x;
		x2temp = nbr.x;
		
		if(isMainWindow)
		{
			x1temp -= graph.getWidth()*graph.currWindow.left;
			x2temp -= graph.getWidth()*graph.currWindow.left;
		}
		
		x1 = (int) this.getX(x1temp);
		x2 = (int) this.getX(x2temp);
		
		//Start of arrow is at center of first box
		y1temp = v.y;
		
		//If box 1 is above box 2, draw arrow to the center of the top line
		if(v.y > nbr.y)
		{
			isCurved = true;
			
			if(this.boxheight < this.minHeight)
			{
				y2temp = nbr.y + fac*this.minHeight/this.boxheight;
			}
			else
			{
				y2temp = nbr.y + fac;
			}
		}
		
		//Otherwise, draw arrow to the center of the bottom line
		else
		{
			isCurved = false;
			if(this.boxheight < this.minHeight)
			{
				y2temp = nbr.y - fac*this.minHeight/this.boxheight;
			}
			else
			{
				y2temp = nbr.y - fac;
			}
		}
		
		if(isMainWindow)
		{
			y1temp -= graph.currWindow.top*graph.getHeight();
			y2temp -= graph.currWindow.top*graph.getHeight();
		}
		
		y1 = (int) this.getY(y1temp);
		y2 = (int) this.getY(y2temp);

		
		if(isHighlightedEdge(v, nbr))
		{
			g.setColor(Color.RED);
			g.setStroke(new BasicStroke(4));
		}
		else
		{
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(1));
		}
		
		if(!isCurved)
			drawArrow(g, x1, y1, x2, y2);
		else
			drawCurvedArrow(g, x1, y1, x2, y2);
	}
	
	public void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2)
	{
		g.drawLine(x1, y1, x2, y2);
		double angle = Math.atan2(y2 - y1, x2 - x1);		
		double length = 15.0;
		if(this.boxSize < 1)
			length = length * this.boxSize;

		if(length < 2.0 && !this.context)
			length = 2.0;
		if(length < 1.0 && this.context)
			length = 1.0;
		
		this.drawArrowhead(g, x2, y2, angle, length);
	}
	
	public void drawCurvedArrow(Graphics2D g, int x1, int y1, int x2, int y2)
	{
		//Our arrow will start and end pointing toward our control point.
		//We pick the control point by starting at the midpoint of the line from (x1, y1) to (x2, y2),
		//and moving a distance of Parameters.minBoxWidth from this line perpendicularly.
		
		int x3, y3;
		int midX = (x1 + x2)/2;
		int midY = (y1 + y2)/2;
		double dist = Parameters.minBoxWidth;
		
		if(x1 == x2)
		{
			x3 = (int) (midX - dist);
			y3 = midY;
		}
		else
		{
			//We know that y2 > y1, since our arrow is pointing up.
			double mPerp = ((double) x2 - x1)/(y2 - y1);
			double deltaX = dist/Math.sqrt(mPerp*mPerp + 1);
			double deltaY = (dist*mPerp)/Math.sqrt(mPerp*mPerp + 1);
			
			if(x2 > x1)
			{
				x3 = (int) (midX + deltaX);
				y3 = (int) (midY - deltaY);
			}
			else
			{
				x3 = (int) (midX - deltaX);
				y3 = (int) (midY + deltaY);
			}
		}
		
		GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
		path.moveTo(x1, y1);
		path.quadTo(x3, y3, x2, y2);
		g.draw(path);
		
		double angle = Math.atan2(y2 - y3, x2 - x3);
		double length = 15.0;
		if(this.boxSize < 1)
			length = length * this.boxSize;

		if(length < 2.0)
			length = 2.0;
		
		this.drawArrowhead(g, x2, y2, angle, length);
	}
	
	public void drawArrowhead(Graphics2D g, int x, int y, double angle, double length)
	{
		double angleDiff = 20.0*Math.PI/180.0; //30 degrees
		
		//For the legs of our arrow, rotate the opposite direction, then offset by angleDiff degrees.
		double anglePlus = (angle + Math.PI) + angleDiff;
		double angleMinus = (angle + Math.PI) - angleDiff;
		int xPlus = (int)(x + length*Math.cos(anglePlus));
		int yPlus = (int)(y + length*Math.sin(anglePlus));
		int xMinus = (int)(x + length*Math.cos(angleMinus));
		int yMinus = (int)(y + length*Math.sin(angleMinus));
		
		/*System.out.println("Head point: " + x + ", " + y);
		System.out.println("Angle: " + angle*180.0/Math.PI + " degrees");
		System.out.println("Plus point: " + xPlus + ", " + yPlus);
		System.out.println("Minus point: " + xMinus + ", " + yMinus);*/
		
		GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
		path.moveTo(x, y);
		path.lineTo(xPlus, yPlus);
		path.lineTo(xMinus, yMinus);
		path.lineTo(x, y);
		g.fill(path);
	}
	
	public void drawCenteredString(Graphics2D g, String text, int x, int y, Font font, Color col)
	{
	    g.setFont(font);
	    g.setColor(col);
	    FontMetrics metrics = g.getFontMetrics(font);
	    
	    double width;
	    if(this.boxwidth < this.minWidth)
	    {
	    	width = this.minWidth * 0.5 * this.boxSize;
	    }
	    else
	    {
	    	width = this.boxwidth * 0.5 * this.boxSize;
	    }
	    
	    double height;
	    if(this.boxheight < this.minHeight)
	    {
	    	height = this.minHeight * 0.5 * this.boxSize;
	    }
	    else
	    {
	    	height = this.boxheight * 0.5 * this.boxSize;
	    }

	    String[] lines = text.split("\n");
	    
	    height = height/metrics.getHeight();
	    if(height<1)
	    	height = 1;
	    int i;
	    for(i=0; i<lines.length-height; i++);
	    for(; i < lines.length; i++)
	    {
		    String line = lines[i];
		    if(width < metrics.stringWidth(line))
		    {
		    	line = shortenString(line, width, metrics);
		    }
		    
		    int xCentered = x - metrics.stringWidth(line)/2;
		    g.drawString(line, xCentered, y);
		    y += metrics.getHeight();
	    }
	}
	
	//Do a binary search to see how much of the line we can print.
	public String shortenString(String str, double width, FontMetrics metrics)
	{
    	int start = 0;
    	int end = str.length();
    	while(end - start > 1)
    	{
    		int center = (start + end)/2;
    		String newSubstr = str.substring(0, center) + "..";
    		if(width < metrics.stringWidth(newSubstr))
    			end = center;
    		else
    			start = center;
    	}

    	String newSubstr1 = str.substring(0, start) + "..";
    	String newSubstr2 = str.substring(0, end) + "..";
    	if(width < metrics.stringWidth(newSubstr2))
    		str = newSubstr1;
    	else
    		str = newSubstr2;
    	
    	return str;
	}
	
	/*public void drawHighlightedVertexMap(Graphics2D g)
	{
		double left = 0;
		double right = (StacViz.graph.currWindow.right - StacViz.graph.currWindow.left)*StacViz.graph.getWidth();
		double top = 0;
		double bottom = (StacViz.graph.currWindow.bottom - StacViz.graph.currWindow.top)*StacViz.graph.getHeight();
		double middleX = (left + right)/2.0;
		double middleY = (top + bottom)/2.0;
		
		int[] countVertices = new int[9];
		for(int i = 0; i < 9; i++)
		{
			countVertices[i] = 0;
		}
		
		for(AbstractVertex v : highlightedVertices)
		{
			//We split our window by the left, right, top, and bottom of the current view.
			//This gives us nine possible regions. The eight outer regions are not contained
			//in our current view, so we add an arrow for each one with the number of highlighted
			//vertices it contains.
			/* _____________
			 * | 1 | 2 | 3 |
			 * _____________
			 * | 4 | 5 | 6 |
			 * _____________
			 * | 7 | 8 | 9 |
			 * _____________
			 *
			
			int currIndex = 0;
			//x-position: left side = 0, middle = 1, right = 2
			if(v.x >= left && v.x <= right)
			{
				currIndex++;
			}
			else if(v.x > right)
			{
				currIndex += 2;
			}
			
			//y-position: top = 0, middle = 3, bottom = 6
			if(v.y >= top && v.y <= bottom)
			{
				currIndex += 3;
			}
			else if(v.y > bottom)
			{
				currIndex += 6;
			}
			
			countVertices[currIndex]++;
		}
		
		double[] arrowAngle = {225, 270, 315, 180, -1, 0, 135, 90, 45};
		double[] xPositions = {left, middleX, right, left, -1, right, left, middleX, right};
		double[] yPositions = {top, top, top, middleY, middleY, middleY, bottom, bottom, bottom};
		int arrowLength = 20;
		
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(2));
		
		for(int i = 0; i < 9; i++)
		{
			//Take arrow pointing to right, place tip at xPositions[i], yPositions[i], and rotate by arrowAngle[i]
			//Then label with value
			if(i != 4)
			{
				arrowAngle[i] *= Math.PI/180.0; //Convert angles to radians
				int x1 = (int) (this.getX(xPositions[i]));
				int y1 = (int) (this.getY(yPositions[i]));
				int x2 = x1 + (int)(arrowLength*Math.cos(arrowAngle[i]));
				int y2 = y1 + (int)(arrowLength*Math.sin(arrowAngle[i]));
				//System.out.println(xPositions[i] + ", " + yPositions[i]);
				//System.out.println(x1 + ", " + y1);
				
				drawArrow(g, x1, y1, x2, y2);
			}
		}
	}*/
	
	public void contextPaint(Graphics2D g)
	{
		Graph graph = StacViz.graph;

		this.boxwidth = this.getWidth()*0.96/graph.getWidth();
		this.boxheight = this.getHeight()*0.96/graph.getHeight();

		if(this.boxwidth>this.maxWidth)
			this.boxwidth = this.maxWidth;

		if(this.boxheight>this.maxHeight)
			this.boxheight = this.maxHeight;
		
		this.left = (this.getWidth()-this.boxwidth*graph.getWidth())/2;
		this.top = (this.getHeight()-this.boxheight*graph.getHeight())/2;

		//Draw directed edges between connected vertices
		if(this.showEdge)
		{
			for(Vertex ver : graph.vertices)
			{
				AbstractVertex v = ver;
				while(!v.isVisible)
					v = v.getMergeParent();
				
				for(AbstractVertex nbr : ver.neighbors)
				{	
					while(!nbr.isVisible)
						nbr = nbr.getMergeParent();
					
					if(v != nbr)
						drawEdge(g, v, nbr, false);
				}
			}
		}

		//Draw boxes for vertices
		for(Vertex ver : StacViz.graph.vertices)
		{
			if(ver.isVisible)
				drawVertex(g, ver, false);
		}

		for(MethodVertex ver : graph.methodVertices)
		{
			if(ver.isVisible)
				drawVertex(g, ver, false);
		}
		
		for(MethodPathVertex ver : graph.methodPathVertices)
		{
			if(ver.isVisible)
				drawVertex(g, ver, false);
		}
	}
	
	public Color getHSBColorT(float H, float S, float B)
	{
		int rgb = Color.HSBtoRGB((float)H, (float)S, (float)B);
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
//		super.repaint();
		super.paintComponent(g);
		this.setBackground(Color.WHITE);

		if(StacViz.graph == null)
			return;
	
//		g.fillOval(this.getWidth()-5, this.getHeight()-5, 5, 5);
		
		this.minWidth = Parameters.minBoxWidth;
		this.minHeight = Parameters.minBoxHeight;
		this.maxWidth = this.getWidth();
		this.maxHeight = this.getHeight();
		
		if(this.maxWidth<this.minWidth)
			this.maxWidth = this.minWidth;
		if(this.maxHeight<this.minHeight)
			this.maxHeight = this.minHeight;
		
		Graphics2D g2 = (Graphics2D) g.create();
		
		int x1, x2, y1, y2;
		
		Graph graph = StacViz.graph;
		
		
		if(this.context)
		{
			g.setColor(Parameters.colorFocus);
			
			if(graph.currWindow.left>0 || graph.currWindow.right<1 || graph.currWindow.top>0 || graph.currWindow.bottom<1)
			{
				x1 = (int) this.getX(graph.getWidth()*graph.currWindow.left);
				y1 = (int) this.getY(graph.getHeight()*graph.currWindow.top);
				x2 = (int) this.getX(graph.getWidth()*graph.currWindow.right);
				y2 = (int) this.getY(graph.getHeight()*graph.currWindow.bottom);
				g.fillRect(x1, y1, x2-x1, y2-y1);
				g.setColor(Color.YELLOW);
				g.drawRect(x1, y1, x2-x1, y2-y1);
			}
			

		}
		if(this.showSelection)
		{
			g2.setColor(Parameters.colorSelection);

			if(this.selectLeft < this.selectRight)
			{
				x1 = (int) this.selectLeft;
				x2 = (int) this.selectRight;
			}
			else
			{
				x2 = (int) this.selectLeft;
				x1 = (int) this.selectRight;
			}

			if(this.selectTop < this.selectBottom)
			{
				y1 = (int) this.selectTop;
				y2 = (int) this.selectBottom;
			}
			else
			{
				y2 = (int) this.selectTop;
				y1 = (int) this.selectBottom;
			}
						
			g2.fillRect(x1, y1, x2-x1, y2-y1);
			
			float dash[] = {10.0f};
		    BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
		    g2.setStroke(dashed);
		    g2.setColor(Color.GRAY);
		    g2.drawRect(x1, y1, x2-x1, y2-y1);
		}

		
		
		if(this.context)
			this.contextPaint(g2);
		else
			this.vizPaint(g2);
		
		
		g.dispose();
	}
	
	public double getBackX(double x)
	{
		Graph graph = StacViz.graph;
		if(this.context)
			return (x-this.left)/(this.boxwidth*graph.getWidth());
		else
			return (((x-this.left)/this.boxwidth)+(graph.getWidth()*graph.currWindow.left))/graph.getWidth();
//			return (x-this.left)/(this.boxwidth*graph.getWidth()*(graph.currWindow.right-graph.currWindow.left))+graph.currWindow.left;
	}
	
	public double getBackY(double y)
	{
		Graph graph = StacViz.graph;
		if(this.context)
			return (y-this.top)/(this.boxheight*graph.getHeight());
//			return (y-this.top)/(this.boxheight);
		else
			return (((y-this.top)/this.boxheight)+(graph.getHeight()*graph.currWindow.top))/graph.getHeight();
//			return (y-this.top)/(this.boxheight*graph.getHeight()*(graph.currWindow.bottom-graph.currWindow.top))+graph.currWindow.top;
	}
	
	public StacFrame getParent()
	{
		return parent;
	}
	
	public double getX(double x)
	{
		return this.left + this.boxwidth * x;
	}
	
	public double getY(double y)
	{
		return this.top + this.boxheight * y;
	}
	
	public void console(String str)
	{
		this.parent.addToConsole(str);
	}
	
}
