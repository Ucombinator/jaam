
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

public class Edge implements Comparable<Edge>
{
	public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY}
	public enum INTERSECT_TYPE {LEFT, RIGHT, TOP, BOTTOM}

	int source, dest;
	private AbstractVertex sourceVertex, destVertex;
	private EDGE_TYPE type;
	private INTERSECT_TYPE exitStart;
	private INTERSECT_TYPE enterDest;
	protected String str_id;

	public Edge(int source, int dest) {
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
			return;

		double exitStartX, exitStartY, enterDestX, enterDestY;
		double centerStartX = sourceVertex.getX() + sourceVertex.getWidth() / 2;
		double centerStartY = sourceVertex.getY() + sourceVertex.getHeight() / 2;
		double centerDestX = destVertex.getX() + destVertex.getWidth() / 2;
		double centerDestY = destVertex.getY() + destVertex.getHeight() / 2;

		// To find which side a line exits from, we compute both diagonals of the rectangle and determine whether
		// the other end lies above or below each diagonal. The positive diagonal uses the positive slope, and the
		// negative diagonal uses the negative slope.
		// Keep in mind that the increasing y direction is downward.
		double startDiagSlope = sourceVertex.getHeight() / sourceVertex.getWidth();
		double startInterceptPos = centerStartY - centerStartX * startDiagSlope;
		double startInterceptNeg = centerStartY + centerStartX * startDiagSlope;
		boolean aboveStartPosDiag = (centerDestX * startDiagSlope + startInterceptPos > centerDestY);
		boolean aboveStartNegDiag = (-centerDestX * startDiagSlope + startInterceptNeg > centerDestY);
		// System.out.println(aboveStartPosDiag + ", " + aboveStartNegDiag);

		if (aboveStartPosDiag && aboveStartNegDiag)
		{
			exitStart = INTERSECT_TYPE.TOP;
			exitStartY = sourceVertex.getY();

			if (centerDestY == centerStartY)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double invSlope = (centerDestX - centerStartX) / (centerDestY - centerStartY);
			exitStartX = centerStartX + invSlope * (exitStartY - centerStartY);
		}
		else if (!aboveStartPosDiag && aboveStartNegDiag)
		{
			exitStart = INTERSECT_TYPE.LEFT;
			exitStartX = sourceVertex.getX();

			if (centerDestX == centerStartX)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double slope = (centerDestY - centerStartY) / (centerDestX - centerStartX);
			exitStartY = centerStartY + slope * (exitStartX - centerStartX);
		}
		else if (aboveStartPosDiag && !aboveStartNegDiag)
		{
			exitStart = INTERSECT_TYPE.RIGHT;
			exitStartX = sourceVertex.getX() + sourceVertex.getWidth();

			if (centerDestX == centerStartX)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double slope = (centerDestY - centerStartY) / (centerDestX - centerStartX);
			exitStartY = centerStartY + slope * (exitStartX - centerStartX);
		}
		else
		{
			exitStart = INTERSECT_TYPE.BOTTOM;
			exitStartY = sourceVertex.getY() + sourceVertex.getHeight();

			if (centerDestY == centerStartY)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double invSlope = (centerDestX - centerStartX) / (centerDestY - centerStartY);
			exitStartX = centerStartX + invSlope * (exitStartY - centerStartY);
		}

		double destDiagSlope = destVertex.getHeight() / destVertex.getWidth();
		double destInterceptPos = centerDestY - centerDestX * destDiagSlope;
		double destInterceptNeg = centerDestY + centerDestX * destDiagSlope;
		boolean aboveDestPosDiag = (centerStartX * destDiagSlope + destInterceptPos > centerStartY);
		boolean aboveDestNegDiag = (-centerStartX * destDiagSlope + destInterceptNeg > centerStartY);
		// System.out.println(aboveDestPosDiag + ", " + aboveDestNegDiag);

		if (aboveDestPosDiag && aboveDestNegDiag)
		{
			enterDest = INTERSECT_TYPE.TOP;
			enterDestY = destVertex.getY();

			if (centerDestY == centerStartY)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double invSlope = (centerStartX - centerDestX) / (centerStartY - centerDestY);
			enterDestX = centerDestX + invSlope * (enterDestY - centerDestY);
		}
		else if(!aboveDestPosDiag && aboveDestNegDiag)
		{
			enterDest = INTERSECT_TYPE.LEFT;
			enterDestX = destVertex.getX();

			if (centerDestX == centerStartX)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double slope = (centerStartY - centerDestY) / (centerStartX - centerDestX);
			enterDestY = centerDestY + slope * (enterDestX - centerDestX);
		}
		else if (aboveDestPosDiag && !aboveDestNegDiag)
		{
			enterDest = INTERSECT_TYPE.RIGHT;
			enterDestX = destVertex.getX() + destVertex.getWidth();

			if (centerDestX == centerStartX)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double slope = (centerStartY - centerDestY) / (centerStartX - centerDestX);
			enterDestY = centerDestY + slope * (enterDestX - centerDestX);
		}
		else
		{
			enterDest = INTERSECT_TYPE.BOTTOM;
			enterDestY = destVertex.getY() + destVertex.getHeight();

			if (centerDestY == centerStartY)
			{
				System.out.println("Error! Invalid edge.");
				System.out.println(source + ", " + dest);
				return;
			}

			double invSlope = (centerStartX - centerDestX) / (centerStartY - centerDestY);
			enterDestX = centerDestX + invSlope * (enterDestY - centerDestY);
		}

		/*System.out.println(source + ", " + dest);
		System.out.println("Start: " + exitStartX + ", " + exitStartY);
		System.out.println("End: " + enterDestX + ", " + enterDestY + "\n");*/
		Line line = new Line(panel.scaleX(exitStartX), panel.scaleY(exitStartY), panel.scaleX(enterDestX), panel.scaleY(enterDestY));
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

		node.getChildren().add(line);
		node.getChildren().add(arrowhead);
	}
}
