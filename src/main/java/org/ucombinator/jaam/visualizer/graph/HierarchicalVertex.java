package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.main.Taint;
import org.ucombinator.jaam.visualizer.state.StateRootVertex;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.taint.TaintRootVertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.*;

public interface HierarchicalVertex<T extends HierarchicalVertex<T, S>, S extends Edge<T>> extends Vertex, Comparable<T> {

    Graph<T, S> getParentGraph();
    Graph<T, S> getChildGraph();
    void setParentGraph(Graph<T, S> graph);
    T copy(); // Copy constructor, used to construct new vertices in visible graph.

    default void applyToVerticesRecursive(Consumer<HierarchicalVertex<T, S>> f) {
        f.accept(this);
        this.getChildGraph().getVertices().forEach(w -> w.applyToVerticesRecursive(f));
    }

    default void applyToEdgesRecursive(Consumer<HierarchicalVertex<T, S>> vertexFunc, Consumer<S> edgeFunc) {
        vertexFunc.accept(this);
        for(S edge: this.getChildGraph().getEdges()) {
            edgeFunc.accept(edge);
        }

        this.getChildGraph().getVertices().forEach(w -> w.applyToEdgesRecursive(vertexFunc, edgeFunc));
    }

    // Overridden in base case, LayoutInstructionVertex
    default int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(HierarchicalVertex<T, S> v : this.getChildGraph().getVertices()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    @Override
    default int compareTo(T v) {
        return Integer.compare(this.getMinInstructionLine(), v.getMinInstructionLine());
    }

    // We assume that only vertices within the same level can be combined.
    default T constructCompressedGraph(Function<T, String> hash, BiFunction<String, Set<T>, T> componentVertexBuilder, BiFunction<T, T, S> edgeBuilder) {
        // If we have no child vertices, just copy ourselves.
        if (this.getChildGraph().getVertices().size() == 0) {
            return this.copy();
        }

        // Otherwise, build a map of components based on matching hash values.
        int nullCounter = 0;
        HashMap<T, String> hashStrings = new HashMap<>();
        HashMap<String, Set<T>> components = new HashMap<>();
        for(T v : this.getChildGraph().getVertices()) {
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
        Graph<T, S> newChildGraph = newRoot.getChildGraph();
        HashMap<String, T> mapStringToVertex = new HashMap<>();
        for(Map.Entry<String, Set<T>> componentEntry : components.entrySet()) {
            Set<T> component = componentEntry.getValue();

            // Preserve the singleton vertices, but build component vertices for larger components.
            // Note that we apply our compression recursively on each vertex before it is added.
            T componentVertex;
            if(component.size() == 1) {
                componentVertex =
                        component.iterator().next()
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
                for(T compressedVertex : compressedComponent) {
                    T origVertex = mapCompressedToOriginal.get(compressedVertex);
                    for(T adjOriginalVertex : this.getChildGraph().getOutNeighbors(origVertex)) {
                        T adjCompressedVertex = mapOriginalToCompressed.get(adjOriginalVertex);
                        // This will be null if it is outside our current component.
                        // That's okay, because it will be handled below when we add edges between component vertices.
                        if (adjCompressedVertex != null) {
                            componentVertex.getChildGraph().addEdge(edgeBuilder.apply(compressedVertex, adjCompressedVertex));
                        }
                    }
                }
            }

            newChildGraph.addVertex(componentVertex);
            mapStringToVertex.put(componentEntry.getKey(), componentVertex);
        }

        // Add edges between component vertices.
        // From an old vertex, we get the hash string from one map, then pass it to a different map
        // to get the new vertex.
        for(T currVertexOld: this.getChildGraph().getVertices()) {
            T currVertexNew = mapStringToVertex.get(hashStrings.get(currVertexOld));
            for(T nextVertexOld : this.getChildGraph().getOutNeighbors(currVertexOld)) {
                T nextVertexNew = mapStringToVertex.get(hashStrings.get(nextVertexOld));

                // Add edges if the new vertices are different, or if we already had a self-loop before.
                // This way, we don't add any new self-loops.
                if((currVertexNew != nextVertexNew) || (currVertexOld == nextVertexOld)) {
                    newChildGraph.addEdge(edgeBuilder.apply(currVertexNew, nextVertexNew));
                }
            }
        }

        return newRoot;
    }

    default HashSet<T> getAncestors()
    {
        HashSet<T> ancestors = new HashSet<>();
        this.getAncestors(ancestors);

        return ancestors;
    }

    default void getAncestors(HashSet<T> ancestors)
    {
        if(this instanceof StateRootVertex || this instanceof TaintRootVertex)
            return;

        ancestors.add((T) this);
        this.getParentGraph().getInNeighbors((T) this).forEach(v -> {
            if (!ancestors.contains(v)) {
                v.getAncestors(ancestors);
            }
        });
    }

    default HashSet<T> getDescendants()
    {
        HashSet<T> descendants = new HashSet<>();
        this.getDescendants(descendants);

        return descendants;
    }

    default void getDescendants(HashSet<T> descendants)
    {
        if(this instanceof StateRootVertex || this instanceof TaintRootVertex)
            return;

        descendants.add((T) this);
        this.getParentGraph().getOutNeighbors((T) this).forEach(v -> {
            if (!descendants.contains(v)) {
                v.getDescendants(descendants);
            }
        });
    }
}
