package org.ucombinator.jaam.visualizer.layout;

import java.util.*;

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

    public boolean isEmpty()
    {
        return vertices.isEmpty();
    }

    public HashSet<AbstractLayoutVertex> getVertices() {
        return vertices;
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

    public ArrayList<AbstractLayoutVertex> getVisibleRoots() {
        if(this.vertices.size() == 0) {
            return null;
        }

        ArrayList<AbstractLayoutVertex> roots = new ArrayList<>();
        for(AbstractLayoutVertex v : this.getVisibleVertices()) {
            if(this.isVisibleRoot(v)) {
                roots.add(v);
            }
        }

        // If there is no root (as for a strongly connected component), choose just the first vertex
        // in our ordering. But this should never be necessary, since we bundle SCC's into their own
        // vertices.
        if(roots.size() == 0) {
            System.out.println("Error: couldn't find root!");
            ArrayList<AbstractLayoutVertex> vertices = new ArrayList<>(this.getVisibleVertices());
            Collections.sort(vertices);
            roots.add(vertices.get(0));
        }

        return roots;
    }

    public boolean isVisibleRoot(AbstractLayoutVertex v) {
        return (this.getVisibleInNeighbors(v).size() == 0 || (this.getVisibleInNeighbors(v).size() == 1 && this.getVisibleInNeighbors(v).contains(v)));
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
    public void setHidden(AbstractLayoutVertex v) {
        ArrayList<AbstractLayoutVertex> srcs = new ArrayList<>();
        ArrayList<AbstractLayoutVertex> dests = new ArrayList<>();

        for (AbstractLayoutVertex src : this.getVisibleInNeighbors(v)) {
            if(!src.equals(v)) {
                srcs.add(src);
                this.visibleOutEdges.get(src).remove(v);
            }
        }

        for (AbstractLayoutVertex dest : this.getVisibleOutNeighbors(v)) {
            if(!dest.equals(v)) {
                dests.add(dest);
                this.visibleInEdges.get(dest).remove(v);
            }
        }

        for(AbstractLayoutVertex src : srcs) {
            for(AbstractLayoutVertex dest : dests) {
                if(!src.equals(v) && !dest.equals(v)) {
                    LayoutEdge edge = new LayoutEdge(src, dest, LayoutEdge.EDGE_TYPE.EDGE_REGULAR);
                    this.addVisibleEdge(edge);
                }
            }
        }

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
