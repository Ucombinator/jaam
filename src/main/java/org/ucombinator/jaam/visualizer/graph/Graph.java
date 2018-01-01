package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Graph<T extends AbstractVertex>
{
    private ArrayList<T> vertices;
    private HashMap<T, HashSet<T>> outEdges;
    private HashMap<T, HashSet<T>> inEdges;
    
    public Graph()
    {
        this.vertices = new ArrayList<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
    }

    public ArrayList<T> getVertices() {
        return this.vertices;
    }

    public void addVertex(T vertex){
    	this.vertices.add(vertex);
    }

    public void addEdge(int src, int dest)
    {
        //System.out.println("Adding input edge: " + src + ", " + dest);
        T vSrc, vDest;

        vSrc = this.containsInputVertex(src);
        vDest = this.containsInputVertex(dest);
        
        this.outEdges.putIfAbsent(vSrc, new HashSet<>());
        this.outEdges.get(vSrc).add(vDest);

        this.inEdges.putIfAbsent(vDest, new HashSet<>());
        this.inEdges.get(vDest).add(vSrc);
        System.out.println("Adding edge: " + vSrc.getLabel() + " --> " + vDest.getLabel());
    }

    public void addEdge(T src, T dest) {
        this.outEdges.putIfAbsent(src, new HashSet<>());
        this.outEdges.get(src).add(dest);

        this.inEdges.putIfAbsent(dest, new HashSet<>());
        this.inEdges.get(dest).add(src);
    }

    public HashSet<T> getOutNeighbors(T v) {
        return this.outEdges.getOrDefault(v, new HashSet<>());
    }

    public HashSet<T> getInNeighbors(T v) {
        return this.inEdges.getOrDefault(v, new HashSet<>());
    }
    
    public T containsInputVertex(int id)
    {
        for(T v : this.vertices)
        {
            if(v.getId() == id)
                return v;
        }

        return null;
    }
}
