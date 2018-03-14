package org.ucombinator.jaam.visualizer.hierarchical;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HierarchicalGraph<T extends HierarchicalVertex<T, S>, S extends HierarchicalEdge<T>> {

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

    // TODO: Maybe we want to set the vertex's self graph here, instead of having it be the responsibility of the caller?
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

    public Set<S> getOutEdges(T v) {
        return this.outEdges.getOrDefault(v, new HashMap<>()).entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    public Set<S> getInEdges(T v) {
        return this.inEdges.getOrDefault(v, new HashMap<>()).entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    public List<T> getSources() {
        // TODO: delete this code so we just return an empty list instead
        if(this.vertices.size() == 0) {
            System.out.println("Error: No vertices!");
            return null;
        }

        List<T> roots = this.vertices.stream()
                .filter(this::isSource)
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
                System.out.println("Choosing arbitary first vertex as source: " + vertices.get(0));
            }
        }

        return roots;
    }

    // A node is a source if it has no incoming edges from anything other than itself
    private boolean isSource(T v) {
        Set<T> inNeighbors = this.getInNeighbors(v);
        return (inNeighbors.size() == 0
                || (inNeighbors.size() == 1 && inNeighbors.contains(v)));
    }

    // DFS for list of pruned leaf vertices of the given type
    public HashSet<T> getVerticesToPrune(Predicate<T> p) {
        HashSet<T> toPrune = new HashSet<>();
        HashSet<T> searched = new HashSet<>();

        for (T v : this.getSources()) {
            this.getVerticesToPrune(v, toPrune, searched, p);
        }

        return toPrune;
    }

    private void getVerticesToPrune(T v, HashSet<T> toPrune, HashSet<T> searched, Predicate<T> p) {
        if (!searched.contains(v)) {
            searched.add(v);
            for (T w : this.getOutNeighbors(v)) {
                this.getVerticesToPrune(w, toPrune, searched, p);
            }

            if (p.test(v) && this.getOutNeighbors(v).stream().allMatch(w -> toPrune.contains(w))) {
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
