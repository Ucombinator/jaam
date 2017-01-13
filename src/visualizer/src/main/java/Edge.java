
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.Group;

public class Edge implements Comparable<Edge>
{
	Group graphics;
	Line line;
	Polygon arrowhead;

	public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY};
	private EDGE_TYPE type;

	int source, dest;
	private AbstractVertex sourceVertex, destVertex;
	private GUINode node;
	protected String strId;

	static double defaultStrokeWidth = 1;
	static double arrowheadAngleDiff = 0.15 * Math.PI;
	public static double arrowLength;

	private Edge(int source, int dest)
	{
		this.strId = this.createID(source, dest);
		this.source = source;
		this.dest = dest;
	}

	public Edge(AbstractVertex sourceVertex, AbstractVertex destVertex, EDGE_TYPE edgeType)
	{
		this(sourceVertex.id, destVertex.id);
		this.type = edgeType;
		this.sourceVertex = sourceVertex;
		this.destVertex = destVertex;
		this.sourceVertex.addOutgoingAbstractNeighbor(this.destVertex);
		this.destVertex.addIncomingAbstractNeighbor(this.sourceVertex);

		graphics = new Group();
		line = new Line();
		arrowhead = new Polygon();
	}

	public EDGE_TYPE getType() {
		return type;
	}

	public static String createID(int source, int dest) {
		return "edge:" + source + "-->" + dest;
	}

	public int compareTo(Edge otherEdge) {
		return ((Integer) source).compareTo(otherEdge.source);
	}

	public String getID() {
		return Edge.createID(this.source, this.dest);
	}
	
	public AbstractVertex getSourceVertex() {
		return sourceVertex;
	}

	public AbstractVertex getDestVertex() {
		return destVertex;
	}

	public void draw(VizPanel panel, GUINode node)
	{
		if (this.source == this.dest)
		{
			System.out.println("Error in Edge.draw(): The source and destination vertices are the same.");
			System.out.println(this.source +"---"+ this.dest);
			System.out.println(this.sourceVertex.getLabel() +"---"+ this.destVertex.getLabel());
			System.out.println(this.getType());
			return;
		}
		else if (sourceVertex.getX() == destVertex.getX() && sourceVertex.getY() == destVertex.getY())
		{
			System.out.println("Error in Edge.draw(): The two vertices are at the same location.");
			System.out.println(this.source + " --- " + this.dest);
			System.out.println(this.sourceVertex.getLabel() + " --- " + this.destVertex.getLabel());
			System.out.println(this.getType());
			return;
		}

		this.node = node;
		double exitStartX, exitStartY, enterDestX, enterDestY;
		double centerStartX = sourceVertex.getX() + sourceVertex.getWidth() / 2.0;
		double centerStartY = sourceVertex.getY() + sourceVertex.getHeight() / 2.0;
		double centerDestX = destVertex.getX() + destVertex.getWidth() / 2.0;
		double centerDestY = destVertex.getY() + destVertex.getHeight() / 2.0;

		// To find which side a line exits from, we compute both diagonals of the rectangle and determine whether
		// the other end lies above or below each diagonal. The positive diagonal uses the positive slope, and the
		// negative diagonal uses the negative slope.
		// Keep in mind that the increasing y direction is downward.
		double startDiagSlope = sourceVertex.getHeight() / sourceVertex.getWidth();
		double startInterceptPos = centerStartY - centerStartX * startDiagSlope;
		double startInterceptNeg = centerStartY + centerStartX * startDiagSlope;
		boolean aboveStartPosDiag = (centerDestX * startDiagSlope + startInterceptPos > centerDestY);
		boolean aboveStartNegDiag = (-centerDestX * startDiagSlope + startInterceptNeg > centerDestY);

		if (aboveStartPosDiag && aboveStartNegDiag)
		{
			// Top
			double invSlope = (centerDestX - centerStartX) / (centerDestY - centerStartY);
			exitStartY = sourceVertex.getY();
			exitStartX = centerStartX + invSlope * (exitStartY - centerStartY);
		}
		else if (!aboveStartPosDiag && aboveStartNegDiag)
		{
			// Left
			double slope = (centerDestY - centerStartY) / (centerDestX - centerStartX);
			exitStartX = sourceVertex.getX();
			exitStartY = centerStartY + slope * (exitStartX - centerStartX);
		}
		else if (aboveStartPosDiag && !aboveStartNegDiag)
		{
			// Right
			double slope = (centerDestY - centerStartY) / (centerDestX - centerStartX);
			exitStartX = sourceVertex.getX() + sourceVertex.getWidth();
			exitStartY = centerStartY + slope * (exitStartX - centerStartX);
		}
		else
		{
			// Bottom
			double invSlope = (centerDestX - centerStartX) / (centerDestY - centerStartY);
			exitStartY = sourceVertex.getY() + sourceVertex.getHeight();
			exitStartX = centerStartX + invSlope * (exitStartY - centerStartY);
		}

		double destDiagSlope = destVertex.getHeight() / destVertex.getWidth();
		double destInterceptPos = centerDestY - centerDestX * destDiagSlope;
		double destInterceptNeg = centerDestY + centerDestX * destDiagSlope;
		boolean aboveDestPosDiag = (centerStartX * destDiagSlope + destInterceptPos > centerStartY);
		boolean aboveDestNegDiag = (-centerStartX * destDiagSlope + destInterceptNeg > centerStartY);

		if (aboveDestPosDiag && aboveDestNegDiag)
		{
			// Top
			double invSlope = (centerStartX - centerDestX) / (centerStartY - centerDestY);
			enterDestY = destVertex.getY();
			enterDestX = centerDestX + invSlope * (enterDestY - centerDestY);
		}
		else if(!aboveDestPosDiag && aboveDestNegDiag)
		{
			// Left
			double slope = (centerStartY - centerDestY) / (centerStartX - centerDestX);
			enterDestX = destVertex.getX();
			enterDestY = centerDestY + slope * (enterDestX - centerDestX);
		}
		else if (aboveDestPosDiag && !aboveDestNegDiag)
		{
			// Right
			double slope = (centerStartY - centerDestY) / (centerStartX - centerDestX);
			enterDestX = destVertex.getX() + destVertex.getWidth();
			enterDestY = centerDestY + slope * (enterDestX - centerDestX);
		}
		else
		{
			// Bottom
			double invSlope = (centerStartX - centerDestX) / (centerStartY - centerDestY);
			enterDestY = destVertex.getY() + destVertex.getHeight();
			enterDestX = centerDestX + invSlope * (enterDestY - centerDestY);
		}

		this.line = new Line(panel.scaleX(exitStartX), panel.scaleY(exitStartY),
				panel.scaleX(enterDestX), panel.scaleY(enterDestY));
		if (this.getType() == EDGE_TYPE.EDGE_DUMMY)
		{
			line.getStrokeDashArray().addAll(5d, 4d);
		}
		line.setStrokeWidth(defaultStrokeWidth);

		// Compute arrowhead
		double angle = Math.PI + Math.atan2(panel.scaleY(enterDestY - exitStartY), panel.scaleX(enterDestX - exitStartX));

		double x1 = panel.scaleX(enterDestX);
		double y1 = panel.scaleY(enterDestY);
		double x2 = panel.scaleX(enterDestX) + arrowLength * Math.cos(angle + arrowheadAngleDiff);
		double y2 = panel.scaleY(enterDestY) + arrowLength * Math.sin(angle + arrowheadAngleDiff);
		double x3 = panel.scaleX(enterDestX) + arrowLength * Math.cos(angle - arrowheadAngleDiff);
		double y3 = panel.scaleY(enterDestY) + arrowLength * Math.sin(angle - arrowheadAngleDiff);

		arrowhead = new Polygon();
		arrowhead.getPoints().addAll(new Double[]{
				x1, y1,
				x2, y2,
				x3, y3 });
		arrowhead.setFill(Color.BLACK);
		//System.out.println("Arrowhead points: " + arrowhead.toString());

		this.graphics.getChildren().add(line);
		this.graphics.getChildren().add(arrowhead);
		node.getChildren().add(graphics);
	}

	public static void redrawEdges(AbstractVertex v)
	{
		if(v.getSelfGraph() != null)
		{
			for (Edge e : v.getSelfGraph().getEdges().values())
			{
				if (v.id == e.source || v.id == e.dest)
				{
					// Clear current graphics...
					e.graphics.getChildren().remove(e.line);
					e.graphics.getChildren().remove(e.arrowhead);
					e.node.getChildren().remove(e.graphics);

					// ...And draw new ones
					e.draw(Parameters.stFrame.mainPanel, e.node);
				}
			}
		}
	}

	public Line getLine()
	{
		return this.line;
	}

	public Polygon getArrowhead()
	{
		return this.arrowhead;
	}

	public Group getGraphics() {
		return graphics;
	}

	public void setGraphics(Group graphics)
	{
		this.graphics = graphics;
	}

	public void setVisible(boolean isVisible)
	{
		this.graphics.setVisible(isVisible);
	}

	public void setScale()
	{
		if(this.node != null) {
			// Make the line for our edge thinner
			double zoomLevel = Parameters.stFrame.mainPanel.getZoomLevel();
			line.setStrokeWidth(defaultStrokeWidth / zoomLevel);

			// TODO: The arrowhead will scale around the center, not the tip.
			// It could be shifted, but that might not make much difference in the end.
			this.arrowhead.setScaleX(1.0 / zoomLevel);
			this.arrowhead.setScaleY(1.0 / zoomLevel);
		}
	}
}
