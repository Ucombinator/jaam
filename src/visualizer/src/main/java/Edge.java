
import javafx.scene.shape.Line;

public class Edge implements Comparable<Edge>
{
	int source, dest;
	Line line;

	// TODO: Compute and draw line
	public Edge(int source, int dest)
	{
		this.source = source;
		this.dest = dest;
		this.line = new Line();
	}
	
	public int compareTo(Edge otherEdge)
	{
		return ((Integer)source).compareTo(otherEdge.source);
	}
}
