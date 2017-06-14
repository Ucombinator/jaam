package org.ucombinator.jaam.visualizer.graph;

public class Edge
{
    private int source, dest;
    private String strId;

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
