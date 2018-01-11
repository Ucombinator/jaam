package org.ucombinator.jaam.visualizer.layout;

import java.util.*;
import java.util.stream.Collectors;

public class HierarchicalGraph<T extends AbstractLayoutVertex<T>>
{
    private HashSet<T> vertices;
    private HashMap<T, HashMap<T, LayoutEdge<T>>> outEdges;
    private HashMap<T, HashMap<T, LayoutEdge<T>>> inEdges;

    private HashSet<T> visibleVertices;
    private HashMap<T, HashMap<T, LayoutEdge<T>>> visibleOutEdges;
    private HashMap<T, HashMap<T, LayoutEdge<T>>> visibleInEdges;

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

    public HashSet<T> getVisibleVertices() {
        return this.visibleVertices;
    }

    public boolean isEmpty()
    {
        return vertices.isEmpty();
    }

    public HashSet<T> getVertices() {
        return vertices;
    }

    public void setVertices(HashSet<T> vertices) {
        this.vertices = vertices;
        for(T vertex : this.vertices) {
            this.visibleVertices.add(vertex);
        }
    }

    private HashSet<LayoutEdge<T>> getEdges() {
        HashSet<LayoutEdge<T>> edgeSet = new HashSet<>();
        for (HashMap<T, LayoutEdge<T>> inEdgeSet : inEdges.values()) {
            edgeSet.addAll(inEdgeSet.values());
        }
        return edgeSet;
    }

    public HashSet<LayoutEdge<T>> getVisibleEdges() {
        HashSet<LayoutEdge<T>> edgeSet = new HashSet<>();
        for(HashMap<T, LayoutEdge<T>> inEdgeSet : visibleInEdges.values()) {
            edgeSet.addAll(inEdgeSet.values());
        }
        return edgeSet;
    }

    public void addVertex(T vertex) {
        this.vertices.add(vertex);
        this.visibleVertices.add(vertex);
        vertex.setSelfGraph(this);
    }
    
    public void addEdge(LayoutEdge<T> edge) {
        this.outEdges.putIfAbsent(edge.getSource(), new HashMap<>());
        this.outEdges.get(edge.getSource()).put(edge.getDest(), edge);

        this.inEdges.putIfAbsent(edge.getDest(), new HashMap<>());
        this.inEdges.get(edge.getDest()).put(edge.getSource(), edge);

        this.visibleOutEdges.putIfAbsent(edge.getSource(), new HashMap<>());
        this.visibleOutEdges.get(edge.getSource()).put(edge.getDest(), edge);

        this.visibleInEdges.putIfAbsent(edge.getDest(), new HashMap<>());
        this.visibleInEdges.get(edge.getDest()).put(edge.getSource(), edge);
    }

    public void addVisibleEdge(LayoutEdge<T> edge) {
        this.visibleOutEdges.get(edge.getSource()).putIfAbsent(edge.getDest(), edge);
        this.visibleInEdges.get(edge.getDest()).putIfAbsent(edge.getSource(), edge);
    }
    
    public String toString() {
        StringBuilder output = new StringBuilder();
        if (this.vertices.size() == 0) {
            return "";
        }

        output.append("Vertices: ");
        for (T v : this.vertices) {
            output.append(v.getLabel() + ", ");
            output.append("\n");
            output.append("Inner graph: \n");
            output.append(v.getInnerGraph().toString());
            output.append("\n");
        }
        output.append("\n");

        output.append("Edges: ");
        for (LayoutEdge<T> e : this.getEdges()) {
            output.append("( " + e.getSource().getLabel() + "->" + e.getDest().getLabel() + " ), ");
        }
        output.append("\n");
        return output.toString();
    }

    public ArrayList<T> getVisibleRoots() {
        if(this.vertices.size() == 0) {
            System.out.println("Error: No vertices!");
            return null;
        }

        ArrayList<T> roots = this.getVisibleVertices().stream()
                .filter(v -> this.isVisibleRoot(v))
                .collect(Collectors.toCollection(ArrayList::new));

        // If there is no root (as for a strongly connected component), choose just the first vertex
        // in our ordering. But this should never be necessary, since we bundle SCC's into their own
        // vertices.
        if(roots.size() == 0) {
            ArrayList<T> vertices = new ArrayList<>(this.getVisibleVertices());
            if(!this.getVisibleVertices().isEmpty()) {
                Collections.sort(vertices);
                roots.add(vertices.get(0));
            }
        }

        return roots;
    }

    public boolean isVisibleRoot(T v) {
        return (this.getVisibleInNeighbors(v).size() == 0
                || (this.getVisibleInNeighbors(v).size() == 1 && this.getVisibleInNeighbors(v).contains(v)));
    }

    private Set<T> getOutNeighbors(T v) {
        return this.outEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    private Set<T> getInNeighbors(T v) {
        return this.inEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    public Set<T> getVisibleOutNeighbors(T v) {
        return this.visibleOutEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    public Set<T> getVisibleInNeighbors(T v) {
        return this.visibleInEdges.getOrDefault(v, new HashMap<>()).keySet();
    }

    // Hides a vertex, and reconnects every incoming vertex to every outgoing vertex.
    public void setHidden(T v) {
        ArrayList<T> srcs = new ArrayList<>();
        ArrayList<T> dests = new ArrayList<>();

        for (T src : this.getVisibleInNeighbors(v)) {
            if(!src.equals(v)) {
                srcs.add(src);
                this.visibleOutEdges.get(src).remove(v);
            }
        }

        for (T dest : this.getVisibleOutNeighbors(v)) {
            if(!dest.equals(v)) {
                dests.add(dest);
                this.visibleInEdges.get(dest).remove(v);
            }
        }

        for(T src : srcs) {
            for(T dest : dests) {
                if(!src.equals(v) && !dest.equals(v)) {
                    LayoutEdge<T> edge = new LayoutEdge<>(src, dest, LayoutEdge.EDGE_TYPE.EDGE_REGULAR);
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
    public void setGraphUnhidden(boolean recurse) {
        this.visibleVertices = new HashSet<>();
        this.visibleInEdges = new HashMap<>();
        this.visibleOutEdges = new HashMap<>();
        for(T v : this.vertices) {
            if(recurse) {
                v.getInnerGraph().setGraphUnhidden(recurse);
            }

            this.visibleVertices.add(v);
            this.visibleInEdges.put(v, new HashMap<>());
            for(T w : this.getInNeighbors(v)) {
                LayoutEdge<T> e = this.inEdges.get(v).get(w);
                this.visibleInEdges.get(v).putIfAbsent(w, e);
            }

            this.visibleOutEdges.put(v, new HashMap<>());
            for(T w : this.getOutNeighbors(v)) {
                LayoutEdge<T> e = this.outEdges.get(v).get(w);
                this.visibleOutEdges.get(v).putIfAbsent(w, e);
            }
        }
    }

    public void setUnhidden(T v, boolean recurse) {
        if(recurse) {
            v.getInnerGraph().setGraphUnhidden(recurse);
        }

        this.visibleVertices.add(v);
        this.visibleInEdges.put(v, new HashMap<>());
        this.visibleOutEdges.put(v, new HashMap<>());
        this.findVisibleInEdges(v);
        this.findVisibleOutEdges(v);

        for(T w : this.getVisibleInNeighbors(v)) {
            this.visibleOutEdges.put(w, new HashMap<>());
            this.findVisibleOutEdges(w);
        }

        for(T w : this.getVisibleOutNeighbors(v)) {
            this.visibleInEdges.put(w, new HashMap<>());
            this.findVisibleInEdges(w);
        }
    }

    private void findVisibleInEdges(T v) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        for (T w : this.getInNeighbors(v)) {
            queue.add(w);
        }

        while (queue.size() > 0) {
            T w = queue.poll();
            found.add(w);
            if(w.isVisible()) {
                this.addVisibleEdge(new LayoutEdge<>(w, v, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
            }
            else {
                for (T nextW : this.getInNeighbors(w)) {
                    if (!found.contains(nextW)) {
                        queue.add(nextW);
                    }
                }
            }
        }
    }

    private void findVisibleOutEdges(T v) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        found.add(v);
        for (T w : this.getOutNeighbors(v)) {
            queue.add(w);
        }

        while (queue.size() > 0) {
            T w = queue.poll();
            found.add(w);
            if(w.isVisible()) {
                this.addVisibleEdge(new LayoutEdge<>(v, w, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
            }
            else {
                for (T nextW : this.getOutNeighbors(w)) {
                    if (!found.contains(nextW)) {
                        queue.add(nextW);
                    }
                }
            }
        }
    }
}
