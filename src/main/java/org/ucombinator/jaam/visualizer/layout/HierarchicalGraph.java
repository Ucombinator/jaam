package org.ucombinator.jaam.visualizer.layout;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class HierarchicalGraph<T extends AbstractLayoutVertex<T>> {

    protected T root; // Every hierarchical graph has a root node that contains it.
    protected HashSet<T> vertices;
    protected HashSet<LayoutEdge<T>> edges;
    protected HashMap<T, HashMap<T, LayoutEdge>> outEdges;
    protected HashMap<T, HashMap<T, LayoutEdge>> inEdges;

    public HierarchicalGraph() {
        this.vertices = new HashSet<>();
        this.edges = new HashSet<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
    }

    public HierarchicalGraph(T root) {
        this();
        this.root = root;
        //this.root.setVisibleInnerGraph(this);
    }

    public T getRoot() {
        return root;
    }

    public void addVertex(T vertex) {
        this.vertices.add(vertex);
    }

    public void addEdge(LayoutEdge<T> edge) {
        this.edges.add(edge);
        this.outEdges.putIfAbsent(edge.getSrc(), new HashMap<>());
        this.outEdges.get(edge.getSrc()).put(edge.getDest(), edge);

        this.inEdges.putIfAbsent(edge.getDest(), new HashMap<>());
        this.inEdges.get(edge.getDest()).put(edge.getSrc(), edge);
    }

    public Set<T> getVertices() {
        return this.vertices;
    }

    public Set<LayoutEdge<T>> getEdges() {
        return this.edges;
    }

    public Set<T> getOutNeighbors(T v) {
        return this.outEdges.getOrDefault(v, new HashMap<>()).entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Set<T> getInNeighbors(T v) {
        return this.inEdges.getOrDefault(v, new HashMap<>()).entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public List<T> getVisibleRoots() {
        if(this.vertices.size() == 0) {
            System.out.println("Error: No vertices!");
            return null;
        }

        List<T> roots = this.vertices.stream()
                .filter(this::isVisibleRoot)
                .collect(Collectors.toList());

        // If there is no root (as for a strongly connected component), choose just the first vertex
        // in our ordering. But this should never be necessary, since we bundle SCC's into their own
        // vertices.
        if(roots.size() == 0) {
            ArrayList<T> vertices = new ArrayList<>(this.vertices);
            if(!this.vertices.isEmpty()) {
                Collections.sort(vertices);
                roots.add(vertices.get(0));
            }
        }

        return roots;
    }

    private boolean isVisibleRoot(T v) {
        Set<T> inNeighbors = this.getInNeighbors(v);
        return (inNeighbors.size() == 0
                || (inNeighbors.size() == 1 && inNeighbors.contains(v)));
    }

    // DFS for list of pruned leaf vertices of the given type
    public HashSet<T> getVerticesToPrune(AbstractLayoutVertex.VertexType type) {
        HashSet<T> toPrune = new HashSet<>();
        HashSet<T> searched = new HashSet<>();

        Stack<T> toSearch = new Stack<>();
        toSearch.addAll(this.getVisibleRoots());

        while(toSearch.size() > 0) {
            T v = toSearch.pop();
            if(!searched.contains(v)) {
                this.getVerticesToPrune(v, toPrune, searched, type);
            }
        }

        return toPrune;
    }

    private void getVerticesToPrune(T v, HashSet<T> toPrune, HashSet<T> searched, AbstractLayoutVertex.VertexType type) {
        searched.add(v);
        boolean shouldPruneThis = (v.getType() == type);
        for(T w : this.getOutNeighbors(v)) {
            if(!searched.contains(w)) {
                this.getVerticesToPrune(w, toPrune, searched, type);
            }

            if(!toPrune.contains(w)) {
                shouldPruneThis = false;
            }
        }

        if(shouldPruneThis) {
            toPrune.add(v);
        }
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        if (this.vertices.size() == 0) {
            return "";
        }

        output.append("Vertices: ");
        for (T v : this.vertices) {
            output.append(v.getLabel() + "\n");
        }
        output.append("\n");

        output.append("Edges: ");
        for (LayoutEdge<T> e : this.getEdges()) {
            output.append("( " + e.getSrc().getLabel() + "->" + e.getDest().getLabel() + " ), ");
        }
        output.append("\n");
        return output.toString();
    }
}
