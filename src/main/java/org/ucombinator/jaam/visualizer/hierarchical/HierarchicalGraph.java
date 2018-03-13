package org.ucombinator.jaam.visualizer.hierarchical;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.*;
import java.util.stream.Collectors;

public class HierarchicalGraph<T extends HierarchicalGraph.Vertex<T>, S extends HierarchicalGraph.Edge<T>> {
    public interface Edge<T> {
        T getSrc();
        T getDest();
    }

    public interface Vertex<T> extends Comparable<T> {
        // TODO: remove once ImmutableHierarchicalGraph.constructVisibleGraph takes a predicate
        AbstractLayoutVertex.VertexType getType();
        String getLabel();
    }

    protected T root; // Every hierarchical graph has a root node that contains it.
    protected HashSet<T> vertices;
    protected HashSet<S> edges;
    protected HashMap<T, HashMap<T, S>> outEdges;
    protected HashMap<T, HashMap<T, S>> inEdges;

    public HierarchicalGraph(T root) {
        this.vertices = new HashSet<>();
        this.edges = new HashSet<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
        this.root = root;
    }

    public T getRoot() {
        return root;
    }

    public void addVertex(T vertex) {
        this.vertices.add(vertex);
    }

    public void addEdge(S edge) {
        this.edges.add(edge);
        this.outEdges.putIfAbsent(edge.getSrc(), new HashMap<>());
        this.outEdges.get(edge.getSrc()).put(edge.getDest(), edge);

        this.inEdges.putIfAbsent(edge.getDest(), new HashMap<>());
        this.inEdges.get(edge.getDest()).put(edge.getSrc(), edge);
    }

    public Set<T> getVertices() {
        return this.vertices;
    }

    public Set<S> getEdges() {
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

    // TODO: rename to getRoots
    // TODO: explain "vertex root" versus "graph root".  Maybe rename to "sources"? (and rename `isRoot`)
    public List<T> getVisibleRoots() {
        // TODO: delete this code so we just return an empty list instead
        if(this.vertices.size() == 0) {
            System.out.println("Error: No vertices!");
            return null;
        }

        List<T> roots = this.vertices.stream()
                .filter(this::isRoot)
                .collect(Collectors.toList());

        // If there is no root (as for a strongly connected component), choose just the first vertex
        // in our ordering. But this should never be necessary, since we bundle SCC's into their own
        // vertices.
        // TODO: could we do this instead: assert this.vertices.size() == 0 || roots.size() != 0;
        if (roots.size() == 0) {
            ArrayList<T> vertices = new ArrayList<>(this.vertices);
            if (!this.vertices.isEmpty()) {
                Collections.sort(vertices);
                roots.add(vertices.get(0));
            }
        }

        return roots;
    }

    // A node is a root if it has no incoming edges from anything other than itself
    private boolean isRoot(T v) {
        Set<T> inNeighbors = this.getInNeighbors(v);
        return (inNeighbors.size() == 0
                || (inNeighbors.size() == 1 && inNeighbors.contains(v)));
    }

    // DFS for list of pruned leaf vertices of the given type
    // TODO: pass a predicate instead of a type?
    public HashSet<T> getVerticesToPrune(AbstractLayoutVertex.VertexType type) {
        HashSet<T> toPrune = new HashSet<>();
        HashSet<T> searched = new HashSet<>();

        for (T v : this.getVisibleRoots()) {
            this.getVerticesToPrune(v, toPrune, searched, type);
        }

        return toPrune;
    }

    private void getVerticesToPrune(T v, HashSet<T> toPrune, HashSet<T> searched, AbstractLayoutVertex.VertexType type) {
        if (!searched.contains(v)) {
            searched.add(v);
            for (T w : this.getOutNeighbors(v)) {
                this.getVerticesToPrune(w, toPrune, searched, type);
            }

            if (v.getType() == type && this.getOutNeighbors(v).stream().allMatch(w -> toPrune.contains(w))) {
                toPrune.add(v);
            }
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
        for (S e : this.getEdges()) {
            output.append("( " + e.getSrc().getLabel() + "->" + e.getDest().getLabel() + " ), ");
        }
        output.append("\n");
        return output.toString();
    }
}
