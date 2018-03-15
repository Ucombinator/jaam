package org.ucombinator.jaam.visualizer.graph;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public interface HierarchicalVertex<T extends HierarchicalVertex<T, S>, S extends Edge<T>> extends AbstractVertex, Comparable<T> {

    Graph<T, S> getSelfGraph();
    Graph<T, S> getInnerGraph();
    void setSelfGraph(Graph<T, S> graph);
    T copy(); // Copy constructor, used to construct new vertices in visible graph.

    // The immutable graph for our root has already been set, so now we construct its visible graph of vertices
    // matching our predicate.
    default T constructVisibleGraph(Predicate<T> isVisible, BiFunction<T, T, S> edgeBuilder) {
        System.out.println("Constructing visible graph for HierarchicalVertex: " + this);
        Graph<T, S> immutableGraph = this.getInnerGraph();
        T visibleRoot = this.copy();
        Graph<T, S> visibleGraph = visibleRoot.getInnerGraph();

        // Add all visible vertices
        for(T v : immutableGraph.getVertices()) {
            if(isVisible.test(v)) {
                visibleGraph.addVertex(v);
                v.setSelfGraph(visibleGraph);
                v.constructVisibleGraph(isVisible, edgeBuilder);
            }
        }

        // For each HierarchicalVertex, search for its visible incoming edges
        for(T v : immutableGraph.getVertices()) {
            // TODO: This might be an inefficient way to construct the currently visible graph.
            if(isVisible.test(v)) {
                this.findVisibleEdges(v, visibleGraph, edgeBuilder);
            }
        }

        return visibleRoot;
    }

    default void findVisibleEdges(T v, Graph<T, S> visibleGraph, BiFunction<T, T, S> edgeBuilder) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        Graph<T, S> immutableGraph = this.getInnerGraph();
        queue.addAll(immutableGraph.getInNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            if (!found.contains(w)) {
                found.add(w);
                if (visibleGraph.getVertices().contains(w)) {
                    System.out.println("Adding edge to visible graph: " + w + " --> " + v);
                    visibleGraph.addEdge(edgeBuilder.apply(w, v));
                } else {
                    queue.addAll(immutableGraph.getInNeighbors(w));
                }
            }
        }
    }

    // We assume that only vertices within the same level can be combined.
    default T constructCompressedGraph(Function<T, String> hash, BiFunction<String, Set<T>, T> componentVertexBuilder, BiFunction<T, T, S> edgeBuilder) {
        // If we have no inner vertices, just copy ourselves.
        if (this.getInnerGraph().getVertices().size() == 0) {
            return this.copy();
        }

        // Otherwise, build a map of components based on matching hash values.
        int nullCounter = 0;
        HashMap<T, String> hashStrings = new HashMap<>();
        HashMap<String, Set<T>> components = new HashMap<>();
        for(T v : this.getInnerGraph().getVertices()) {
            String newHash = hash.apply(v);
            if(newHash == null) {
                // Create a unique key for each null hash value
                String nullKey = Integer.toString(nullCounter++);
                hashStrings.put(v, nullKey);

                HashSet<T> newSet = new HashSet<T>();
                newSet.add(v);
                components.put(nullKey, newSet);
            } else {
                hashStrings.put(v, newHash);
                if(components.containsKey(newHash)) {
                    components.get(newHash).add(v);

                } else {
                    HashSet<T> newSet = new HashSet<T>();
                    newSet.add(v);
                    components.put(newHash, newSet);
                }
            }
        }

        T newRoot = this.copy();
        Graph<T, S> newInnerGraph = newRoot.getInnerGraph();
        HashMap<String, T> mapStringToVertex = new HashMap<>();
        for(Map.Entry<String, Set<T>> componentEntry : components.entrySet()) {
            Set<T> component = componentEntry.getValue();

            // Preserve the singleton vertices, but build component vertices for larger components.
            // Note that we apply our compression recursively on each vertex before it is added.
            T componentVertex;
            if(component.size() == 1) {
                componentVertex = component.iterator().next()
                        .constructCompressedGraph(hash, componentVertexBuilder, edgeBuilder);
            } else {
                // We need these maps so we can add edges inside our component vertex.
                // TODO: There has to be a cleaner way to do this...
                HashMap<T, T> mapCompressedToOriginal = new HashMap<>();
                HashMap<T, T> mapOriginalToCompressed = new HashMap<>();
                Set<T> compressedComponent = new HashSet<>();
                for(T vertex : component) {
                    T compressedVertex = vertex.constructCompressedGraph(hash, componentVertexBuilder, edgeBuilder);
                    compressedComponent.add(compressedVertex);
                    mapCompressedToOriginal.put(compressedVertex, vertex);
                    mapOriginalToCompressed.put(vertex, compressedVertex);
                }

                // Build component of compressed vertices, and add edges.
                // We go from a compressed vertex inside our component, to its corresponding vertex in the original graph,
                // to each neighbor of that original vertex, and then to the compressed vertices corresponding to those neighbors.
                componentVertex = componentVertexBuilder.apply(componentEntry.getKey(), compressedComponent);
                for(T compressedVertex : componentVertex.getInnerGraph().getVertices()) {
                    T origVertex = mapCompressedToOriginal.get(compressedVertex);
                    for(T adjOriginalVertex : this.getInnerGraph().getOutNeighbors(origVertex)) {
                        T adjCompressedVertex = mapOriginalToCompressed.get(adjOriginalVertex);
                        componentVertex.getInnerGraph().addEdge(edgeBuilder.apply(compressedVertex, adjCompressedVertex));
                    }
                }
            }

            newInnerGraph.addVertex(componentVertex);
            mapStringToVertex.put(componentEntry.getKey(), componentVertex);
        }

        // Add edges between component vertices.
        // From an old vertex, we get the hash string from one map, then pass it to a different map
        // to get the new vertex.
        for(T currVertexOld: this.getInnerGraph().getVertices()) {
            T currVertexNew = mapStringToVertex.get(hashStrings.get(currVertexOld));
            for(T nextVertexOld : this.getInnerGraph().getOutNeighbors(currVertexOld)) {
                T nextVertexNew = mapStringToVertex.get(hashStrings.get(nextVertexOld));

                // Add edges if the new vertices are different, or if we already had a self-loop before.
                // This way, we don't add any new self-loops.
                if((currVertexNew != nextVertexNew) || (currVertexOld == nextVertexOld)) {
                    newInnerGraph.addEdge(edgeBuilder.apply(currVertexNew, nextVertexNew));
                }
            }
        }

        return newRoot;
    }

    default void applyToVerticesRecursive(Consumer<HierarchicalVertex<T, S>> f) {
        f.accept(this);
        this.getInnerGraph().getVertices().forEach(w -> w.applyToVerticesRecursive(f));
    }

    default void applyToEdgesRecursive(Consumer<HierarchicalVertex<T, S>> vertexFunc, Consumer<S> edgeFunc) {
        vertexFunc.accept(this);
        for(S edge: this.getInnerGraph().getEdges()) {
            edgeFunc.accept(edge);
        }

        this.getInnerGraph().getVertices().forEach(w -> w.applyToEdgesRecursive(vertexFunc, edgeFunc));
    }

    // Overridden in base case, LayoutInstructionVertex
    default int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(HierarchicalVertex<T, S> v : this.getInnerGraph().getVertices()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    @Override
    default int compareTo(T v) {
        return Integer.compare(this.getMinInstructionLine(), v.getMinInstructionLine());
    }

}
