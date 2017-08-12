package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;

//TODO make it abstract
public class AbstractVertex
{
    private static int idCounter = 0; // Used to assign unique id numbers to each vertex
    private String label;
    private int id;
    private ArrayList<String> tags;

    public enum VertexStatus
    {
        WHITE,
        GRAY,
        BLACK
    }
    private VertexStatus vertexStatus;

    public AbstractVertex(String label)
    {
        this(label, new ArrayList<String>());
    }

    private AbstractVertex(String label, ArrayList<String> tags) {
        this.label = label;
        this.id = idCounter++;
        this.vertexStatus = VertexStatus.WHITE;
        this.tags = tags;
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

    public ArrayList<String> getTags() {
        return this.tags;
    }

//    public void addTag(String tag) {
//        this.tags.add(tag);
//    }


    public VertexStatus getVertexStatus() {
        return this.vertexStatus;
    }

    public void setVertexStatus(VertexStatus vertexStatus) {
        this.vertexStatus = vertexStatus;
    }

//    public void clean() {
//        this.vertexStatus = AbstractVertex.VertexStatus.WHITE;
//    }
}
