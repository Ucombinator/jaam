package org.ucombinator.jaam.visualizer.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class HierarchicalGraph
{
    private HashSet<AbstractLayoutVertex> vertices;
    private HashMap<AbstractLayoutVertex, HashMap<AbstractLayoutVertex, LayoutEdge>> outEdges;
    private HashMap<AbstractLayoutVertex, HashMap<AbstractLayoutVertex, LayoutEdge>> inEdges;

    private HashSet<AbstractLayoutVertex> visibleVertices;
    private HashMap<AbstractLayoutVertex, HashMap<AbstractLayoutVertex, LayoutEdge>> visibleOutEdges;
    private HashMap<AbstractLayoutVertex, HashMap<AbstractLayoutVertex, LayoutEdge>> visibleInEdges;

    public HierarchicalGraph()
    {
    	super();
    	this.vertices = new HashSet<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();

        this.visibleVertices = new HashSet<>();
        this.visibleOutEdges = new HashMap<>();
        this.visibleInEdges = new HashMap<>();
    }

    public HashSet<AbstractLayoutVertex> getVisibleVertices() {
        return this.visibleVertices;
    }

    public void setVertices(HashSet<AbstractLayoutVertex> vertices) {
        this.vertices = vertices;
        for(AbstractLayoutVertex vertex : this.vertices) {
            this.visibleVertices.add(vertex);
        }
    }

    private HashSet<LayoutEdge> getEdges() {
        HashSet<LayoutEdge> edgeSet = new HashSet<>();
        for (HashMap<AbstractLayoutVertex, LayoutEdge> inEdgeSet : inEdges.values()) {
            edgeSet.addAll(inEdgeSet.values());
        }
        return edgeSet;
    }

    public HashSet<LayoutEdge> getVisibleEdges() {
        HashSet<LayoutEdge> edgeSet = new HashSet<>();
        for(HashMap<AbstractLayoutVertex, LayoutEdge> inEdgeSet : visibleInEdges.values()) {
            edgeSet.addAll(inEdgeSet.values());
        }
        return edgeSet;
    }

    public void addVertex(AbstractLayoutVertex vertex)
    {
        this.vertices.add(vertex);
        this.visibleVertices.add(vertex);
        vertex.setSelfGraph(this);
    }
    
    public void addEdge(LayoutEdge edge)
    {
        this.outEdges.putIfAbsent(edge.getSource(), new HashMap<>());
        this.outEdges.get(edge.getSource()).put(edge.getDest(), edge);

        this.inEdges.putIfAbsent(edge.getDest(), new HashMap<>());
        this.inEdges.get(edge.getDest()).put(edge.getSource(), edge);

        this.visibleOutEdges.putIfAbsent(edge.getSource(), new HashMap<>());
        this.visibleOutEdges.get(edge.getSource()).put(edge.getDest(), edge);

        this.visibleInEdges.putIfAbsent(edge.getDest(), new HashMap<>());
        this.visibleInEdges.get(edge.getDest()).put(edge.getSource(), edge);
    }

    public void addVisibleEdge(LayoutEdge edge) {
        System.out.println("Adding visible edge: " + edge.getSource().getId() + " --> " + edge.getDest().getId());
        this.visibleOutEdges.get(edge.getSource()).putIfAbsent(edge.getDest(), edge);
        this.visibleInEdges.get(edge.getDest()).putIfAbsent(edge.getSource(), edge);

        System.out.print("In neighbors for destination:");
        for(AbstractLayoutVertex inNeighbor : this.getVisibleInNeighbors(edge.getDest())) {
            System.out.print(inNeighbor.getId() + " ");
        }
        System.out.println();
    }
    
    public String toString()
    {
        StringBuilder output = new StringBuilder();
        if(this.vertices.size() == 0)
            return "";

        output.append("Vertices: ");
        for(AbstractLayoutVertex v : this.vertices)
        {
            output.append(v.getLabel() + ", ");
            output.append("\n");
            output.append("Inner graph: \n");
            output.append(v.getInnerGraph().toString());
            output.append("\n");
        }
        output.append("\n");

        output.append("Edges: ");
        for(LayoutEdge e : this.getEdges()) {
            output.append("( " + e.getSource().getLabel() + "->" + e.getDest().getLabel() + " ), ");
        }
        output.append("\n");
        return output.toString();
    }
    
    public AbstractLayoutVertex getRoot() {
        if(this.vertices.size() == 0){
            //System.out.println("getRoot on empty graph");
            return null;
        }

        ArrayList<AbstractLayoutVertex> arrayList = new ArrayList<AbstractLayoutVertex>(this.vertices);
        Collections.sort(arrayList);
        //System.out.println("Root ID: " + arrayList.get(0).getId());

        // Return the first vertex with no incoming edges
        for(AbstractLayoutVertex v : arrayList) {
            if(this.isRoot(v))
                return v;
        }

        // Otherwise, return the first vertex, period.
        return arrayList.get(0);
    }

    public boolean isRoot(AbstractLayoutVertex v) {
        return (this.getInNeighbors(v).size() == 0 || (this.getInNeighbors(v).size() == 1 && this.getInNeighbors(v).contains(v)));
    }

    private Set<AbstractLayoutVertex> getOutNeighbors(AbstractLayoutVertex v) {
        return this.outEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    private Set<AbstractLayoutVertex> getInNeighbors(AbstractLayoutVertex v) {
        return this.inEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    public Set<AbstractLayoutVertex> getVisibleOutNeighbors(AbstractLayoutVertex v) {
        return this.visibleOutEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    public Set<AbstractLayoutVertex> getVisibleInNeighbors(AbstractLayoutVertex v) {
        return this.visibleInEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    // Hides a vertex, and reconnects every incoming vertex to every outgoing vertex.
    // We don't allow hiding the root, since it would disconnect the graph.
    public void setHidden(AbstractLayoutVertex v) {
        for (AbstractLayoutVertex src : this.getVisibleInNeighbors(v)) {
            for (AbstractLayoutVertex dest : this.getVisibleOutNeighbors(v)) {
                if(!src.equals(v) && !dest.equals(v)) {
                    this.visibleInEdges.get(dest).remove(v);
                    this.visibleOutEdges.get(src).remove(v);
                    LayoutEdge edge = new LayoutEdge(src, dest, LayoutEdge.EDGE_TYPE.EDGE_REGULAR);
                    this.addVisibleEdge(edge);
                }
            }
        }

        System.out.println("Hiding vertex: " + v.getId());
        this.visibleVertices.remove(v);
        this.visibleInEdges.put(v, new HashMap<>());
        this.visibleOutEdges.put(v, new HashMap<>());
    }

    // Restores the graph to its original set of edges. Note that we can't simply assign visibleInEdges
    // and visibleOutEdges; we need to make a deep copy.
    public void setUnhidden() {
        this.visibleVertices = new HashSet<>();
        this.visibleInEdges = new HashMap<>();
        this.visibleOutEdges = new HashMap<>();
        for(AbstractLayoutVertex v : this.vertices) {
            this.visibleVertices.add(v);
            this.visibleInEdges.put(v, new HashMap<>());
            for(AbstractLayoutVertex w : this.getInNeighbors(v)) {
                LayoutEdge e = this.inEdges.get(v).get(w);
                this.visibleInEdges.get(v).putIfAbsent(w, e);
            }

            this.visibleOutEdges.put(v, new HashMap<>());
            for(AbstractLayoutVertex w : this.getOutNeighbors(v)) {
                LayoutEdge e = this.outEdges.get(v).get(w);
                this.visibleOutEdges.get(v).putIfAbsent(w, e);
            }
        }
    }
}
