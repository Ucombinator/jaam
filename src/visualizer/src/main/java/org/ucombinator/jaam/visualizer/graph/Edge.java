package org.ucombinator.jaam.visualizer.graph;

public class Edge
{
	int source, dest;
	protected String strId;

	private Edge(int source, int dest)
	{
		this.strId = "edge:" + source + "-->" + dest;
		this.source = source;
		this.dest = dest;
	}

	public String getID() {
		return strId;
	}
}
