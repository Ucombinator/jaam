package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Graph
{
    private ArrayList<AbstractVertex> vertices;
    private HashMap<AbstractVertex, HashSet<AbstractVertex>> outEdges;
    private HashMap<AbstractVertex, HashSet<AbstractVertex>> inEdges;
    
    public Graph()
    {
        this.vertices = new ArrayList<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
    }

    public ArrayList<AbstractVertex> getVertices() {
        return this.vertices;
    }

    public void addVertex(AbstractVertex vertex){
    	this.vertices.add(vertex);
    }

    public void addEdge(int src, int dest)
    {
        //System.out.println("Adding input edge: " + src + ", " + dest);
        AbstractVertex vSrc, vDest;

        vSrc = this.containsInputVertex(src);
        vDest = this.containsInputVertex(dest);
        
        this.outEdges.putIfAbsent(vSrc, new HashSet<>());
        this.outEdges.get(vSrc).add(vDest);

        this.inEdges.putIfAbsent(vDest, new HashSet<>());
        this.inEdges.get(vDest).add(vSrc);
        System.out.println("Adding edge: " + vSrc.getLabel() + " --> " + vDest.getLabel());
    }

    public HashSet<AbstractVertex> getOutNeighbors(AbstractVertex v) {
        return this.outEdges.getOrDefault(v, new HashSet<>());
    }

    public HashSet<AbstractVertex> getInNeighbors(AbstractVertex v) {
        return this.inEdges.getOrDefault(v, new HashSet<>());
    }
    
    public AbstractVertex containsInputVertex(int id)
    {
        for(AbstractVertex v : this.vertices)
        {
            if(v.getId() == id)
                return v;
        }

        return null;
    }
}
