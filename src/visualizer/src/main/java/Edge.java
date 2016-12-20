
import javafx.scene.shape.Line;

public class Edge implements Comparable<Edge>
{
	int source, dest;
	final static int EDGE_REGULAR = 0;
	final static int EDGE_DUMMY = 1;
	public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY};

	private AbstractVertex sourceVertex, destVertex;
	private EDGE_TYPE type;
	protected String str_id;

	public Edge(int source, int dest)
	{
		this.str_id = this.createID(source,dest);
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
		return "edge:"+source+"-->"+dest;
	}

	public int compareTo(Edge otherEdge)
	{
		return ((Integer)source).compareTo(otherEdge.source);
	}

	public String getID() {
		return "edge:"+source+"-->"+dest;
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
		double centerStartX = sourceVertex.getX() + sourceVertex.getWidth() / 2;
		double centerStartY = sourceVertex.getY() + sourceVertex.getHeight() / 2;
		double centerDestX = destVertex.getX() + destVertex.getWidth() / 2;
		double centerDestY = destVertex.getY() + destVertex.getHeight() / 2;

		// double startDiagSlope = sourceVertex.getHeight() / sourceVertex.getWidth();


		Line l = new Line(panel.scaleX(centerStartX), panel.scaleY(centerStartY), panel.scaleX(centerDestX), panel.scaleY(centerDestY));
		if (this.getType() == Edge.EDGE_TYPE.EDGE_DUMMY)
		{
			l.getStrokeDashArray().addAll(5d, 4d);
		}

		node.getChildren().add(l);
	}
}
