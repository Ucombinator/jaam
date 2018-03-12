package org.ucombinator.jaam.visualizer.graph;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Graph<T extends AbstractVertex>
{
    protected HashSet<T> vertices;
    protected HashMap<T, HashSet<T>> outEdges;
    protected HashMap<T, HashSet<T>> inEdges;
    protected HashMap<Integer, T> vertexIndex;
    
    public Graph()
    {
        this.vertices = new HashSet<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
        this.vertexIndex = new HashMap<>();
    }

    public HashSet<T> getVertices() {
        return this.vertices;
    }

    public boolean isEmpty() {
        return this.vertices.isEmpty();
    }

    public void addVertex(T vertex){
    	this.vertices.add(vertex);
    	this.outEdges.put(vertex, new HashSet<>());
    	this.inEdges.put(vertex, new HashSet<>());
    	this.vertexIndex.put(vertex.getId(), vertex);
    }

    public void addEdge(int src, int dest)
    {
        T vSrc = this.containsInputVertex(src);
        T vDest = this.containsInputVertex(dest);
        this.addEdge(vSrc, vDest);
    }

    public void addEdge(T src, T dest) {
        assert src != null;
        assert dest != null;

        this.outEdges.putIfAbsent(src, new HashSet<>());
        this.outEdges.get(src).add(dest);
        this.inEdges.putIfAbsent(dest, new HashSet<>());
        this.inEdges.get(dest).add(src);
    }

    public Set<T> getOutNeighbors(T v) {
        return this.outEdges.get(v);
    }

    public Set<T> getInNeighbors(T v) {
        return this.inEdges.get(v);
    }

    public T containsInputVertex(int id) {
        return this.vertexIndex.getOrDefault(id, null);
    }
}
