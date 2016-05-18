
public class Edge implements Comparable<Edge>
{
	int source, dest;
	
	public Edge(int source, int dest)
	{
		this.source = source;
		this.dest = dest;
	}
	
	public int compareTo(Edge otherEdge)
	{
		return ((Integer)source).compareTo(otherEdge.source);
	}
}
