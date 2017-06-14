package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

public abstract class AbstractVertex<T extends AbstractVertex>
{
    private static int idCounter = 0; // Used to assign unique id numbers to each vertex
    private String label;
    private int id;
    private ArrayList<String> tags;

    private HashSet<T> outgoingNeighbors;
    private HashSet<T> incomingNeighbors;

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

    public AbstractVertex(String label, ArrayList<String> tags) {
        this.label = label;
        this.outgoingNeighbors = new LinkedHashSet<T>();
        this.incomingNeighbors = new LinkedHashSet<T>();
        this.id = idCounter++;
        this.vertexStatus = VertexStatus.WHITE;
        this.tags = tags;
    }
    
    public String getLabel() {
        return this.label;
    }

    public int getId() {
        return this.id;
    }

    // TODO: Remove this function?
    public String getStrID()
    {
        String strId ="vertex:"+this.id; // the id might been set after the vertex constructor is called
        return strId;
    }

    public ArrayList<String> getTags() {
        return this.tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    /* TODO: Why did we use integers for our tags instead of strings?
    public void addTag(int t)
    {
        for(Integer i : this.tags)
        {
            if(i.intValue()==t)
                return;
        }
        this.tags.add(new Integer(t));
    }

    public boolean hasTag(int t)
    {
        for(Integer i : this.tags)
        {
            if(i.intValue()==t)
                return true;
        }
        return false;
    }
    */

    public VertexStatus getVertexStatus() {
        return this.vertexStatus;
    }

    public void setVertexStatus(VertexStatus vertexStatus) {
        this.vertexStatus = vertexStatus;
    }

    public void addIncomingNeighbor(T v) {
        this.incomingNeighbors.add(v);
    }

    public void addOutgoingNeighbor(T v) {
        this.outgoingNeighbors.add(v);
    }
    
    public HashSet<T> getOutgoingNeighbors()
    {
        return this.outgoingNeighbors;
    }
    
    public HashSet<T> getIncomingNeighbors()
    {
        return this.incomingNeighbors;
    }

    public int getOutDegree()
    {
        return this.outgoingNeighbors.size();
    }

    public int getInDegree() {
        return this.incomingNeighbors.size();
    }

    public void removeOutgoingAbstractNeighbor(AbstractVertex destVertex) {
        this.outgoingNeighbors.remove(destVertex);
    }
    
    public void removeIncomingAbstractNeighbor(AbstractVertex destVertex) {
        this.incomingNeighbors.remove(destVertex);
    }

    public void clean() {
        this.vertexStatus = AbstractVertex.VertexStatus.WHITE;
    }
}
