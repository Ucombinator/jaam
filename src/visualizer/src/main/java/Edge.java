
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
	protected String str_id;

	public Edge(int source, int dest)
	{
		this.str_id = this.createID(source, dest);
		this.source = source;
		this.dest = dest;
	}

	public Edge(AbstractVertex sourceVertex, AbstractVertex destVertex, EDGE_TYPE edge_type)
	{
		this(sourceVertex.id, destVertex.id);
		this.type = edge_type;
		this.sourceVertex = sourceVertex;
		this.destVertex = destVertex;
		this.sourceVertex.addAbstractNeighbor(this.destVertex);

		graphics = new Group();
		line = new Line();
		arrowhead = new Polygon();
	}

	public EDGE_TYPE getType() {
		return type;
	}

	private String createID(int source, int dest) {
		return "edge:" + source + "-->" + dest;
	}

	public int compareTo(Edge otherEdge) {
		return ((Integer) source).compareTo(otherEdge.source);
	}

	public String getID() {
		return "edge:" + source + "-->" + dest;
	}

	public AbstractVertex getSourceVertex() {
		return sourceVertex;
	}

	public AbstractVertex getDestVertex() {
		return destVertex;
	}

	// TODO: Make lines thinner when zoomed in.
	public void draw(VizPanel panel, GUINode node)
	{
		if (this.source == this.dest)
		{
			System.out.println("NOOOOOOOOOOOOOOOO!!!!");
			System.out.println(this.source +"---"+ this.dest);
			System.out.println(this.sourceVertex.getLabel() +"---"+ this.destVertex.getLabel());
			System.out.println(this.getType());
			return;
		}

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
			double invSlope = (centerDestX - centerStartX) / (centerDestY - centerStartY);
			exitStartY = sourceVertex.getY();
			exitStartX = centerStartX + invSlope * (exitStartY - centerStartY);
		}
		else if (!aboveStartPosDiag && aboveStartNegDiag)
		{
			double slope = (centerDestY - centerStartY) / (centerDestX - centerStartX);
			exitStartX = sourceVertex.getX();
			exitStartY = centerStartY + slope * (exitStartX - centerStartX);
		}
		else if (aboveStartPosDiag && !aboveStartNegDiag)
		{
			double slope = (centerDestY - centerStartY) / (centerDestX - centerStartX);
			exitStartX = sourceVertex.getX() + sourceVertex.getWidth();
			exitStartY = centerStartY + slope * (exitStartX - centerStartX);
		}
		else
		{
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
			double invSlope = (centerStartX - centerDestX) / (centerStartY - centerDestY);
			enterDestY = destVertex.getY();
			enterDestX = centerDestX + invSlope * (enterDestY - centerDestY);
		}
		else if(!aboveDestPosDiag && aboveDestNegDiag)
		{
			double slope = (centerStartY - centerDestY) / (centerStartX - centerDestX);
			enterDestX = destVertex.getX();
			enterDestY = centerDestY + slope * (enterDestX - centerDestX);
		}
		else if (aboveDestPosDiag && !aboveDestNegDiag)
		{
			double slope = (centerStartY - centerDestY) / (centerStartX - centerDestX);
			enterDestX = destVertex.getX() + destVertex.getWidth();
			enterDestY = centerDestY + slope * (enterDestX - centerDestX);
		}
		else
		{
			double invSlope = (centerStartX - centerDestX) / (centerStartY - centerDestY);
			enterDestY = destVertex.getY() + destVertex.getHeight();
			enterDestX = centerDestX + invSlope * (enterDestY - centerDestY);
		}

		this.line = new Line(panel.scaleX(exitStartX), panel.scaleY(exitStartY),
				panel.scaleX(enterDestX), panel.scaleY(enterDestY));
		if (this.getType() == Edge.EDGE_TYPE.EDGE_DUMMY)
		{
			line.getStrokeDashArray().addAll(5d, 4d);
		}

		// Compute arrowhead
		// TODO: Pick better length?
		double angle = Math.PI + Math.atan2(panel.scaleY(enterDestY - exitStartY), panel.scaleX(enterDestX - exitStartX));
		double angleDiff = 0.15 * Math.PI;
		double length = 10.0;

		double x1 = panel.scaleX(enterDestX);
		double y1 = panel.scaleY(enterDestY);
		double x2 = panel.scaleX(enterDestX) + length * Math.cos(angle + angleDiff);
		double y2 = panel.scaleY(enterDestY) + length * Math.sin(angle + angleDiff);
		double x3 = panel.scaleX(enterDestX) + length * Math.cos(angle - angleDiff);
		double y3 = panel.scaleY(enterDestY) + length * Math.sin(angle - angleDiff);

		Polygon arrowhead = new Polygon();
		arrowhead.getPoints().addAll(new Double[]{
				x1, y1,
				x2, y2,
				x3, y3 });
		arrowhead.setFill(Color.BLACK);

		this.graphics.getChildren().add(line);
		this.graphics.getChildren().add(arrowhead);
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
}
