package org.ucombinator.jaam.visualizer.graph;

public abstract class AbstractVertex
{
    private static int idCounter = 0; // Used to assign unique id numbers to each vertex
    private String label;
    private int id;

    public enum VertexStatus
    {
        WHITE,
        GRAY,
        BLACK
    }
    private VertexStatus vertexStatus;

    public AbstractVertex(String label) {
        this.label = label;
        this.id = idCounter++;
        this.vertexStatus = VertexStatus.WHITE;
    }
    
    public AbstractVertex(int id, String label) {
    	this(label);
        this.id = id;
    }
    
    public String getLabel() {
        return this.label;
    }

    public int getId() {
        return this.id;
    }

    public VertexStatus getVertexStatus() {
        return this.vertexStatus;
    }

    public void setVertexStatus(VertexStatus vertexStatus) {
        this.vertexStatus = vertexStatus;
    }
}
