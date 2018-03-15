package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.visualizer.layout.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class GraphUtils {

    private static class SCCVertex
    {
        SCCVertex(Integer id, Integer myIndex)
        {
            vertexId = id;
            index = myIndex;
            lowlink = -1;
        }

        @Override
        public String toString()
        {
                return "{"+ vertexId + ":" + index + ":" + lowlink + "}";
        }

        public final int vertexId;
        public final int index; // Order in which the vertices were visited by the DFS
        public int lowlink; // minIndex of a vertex reachable from my subtree that is not already part of a SCC
    }

    private static <T extends AbstractLayoutVertex<T>, S extends Edge<T>> void visit(
            Graph<T, S> graph, T v, HashMap<Integer, SCCVertex> visitedVertices, Stack<Integer> stack,
            List<List<Integer>> components)
    {
        SCCVertex vSCC = new SCCVertex(v.getId(), visitedVertices.size());
        visitedVertices.put(v.getId(), vSCC);
        stack.push(v.getId());
        vSCC.lowlink = vSCC.index;

        System.out.println("Visiting vertex in SCC construction: " + v);

        Set<T> neighbors = graph.getOutNeighbors(v);
        for (T n : neighbors) {
            System.out.println("Neighbor: " + n);
            if (n.getId() == v.getId()) { // No self loops
                continue;
            }
            //System.out.print("\tTERE Neighbor " + n.getId());
            if (!visitedVertices.containsKey(n.getId())) {
                //System.out.println(" Hadn't been visited");
                visit(graph, n, visitedVertices, stack, components);
                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).lowlink);
            } else if (stack.contains(n.getId())) { // Should be fast because the stack is small
                //System.out.println(" Still On Stack" + stack );

                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).index);
            } else {
                //System.out.println(" Already processed and popped " + stack + visitedVertices);
            }
        }

        //System.out.println("TERE Finished Visiting " + v.getId() + " == " + vSCC);

        if (vSCC.lowlink == vSCC.index) {
            //System.out.println("\t\t\tTERE Found a leader " + vSCC);
            ArrayList<Integer> newComponent = new ArrayList<>();
            while (true) {
                int w = stack.pop();
                // System.out.println("\t\t\t\tTERE Popped " + w);
                newComponent.add(w);
                if(w == v.getId())
                    break;
            }
            components.add(newComponent);
        }
    }

    public static <T extends AbstractLayoutVertex<T>, S extends Edge<T>> List<List<Integer>>
    StronglyConnectedComponents(final Graph<T, S> graph) {
        List<List<Integer>> components = new ArrayList<>();

        Stack<Integer> stack = new Stack<>();
        HashMap<Integer, SCCVertex> visitedVertices = new HashMap<>();

        Set<T> vertices = graph.getVertices();
        System.out.println("Vertices: " + vertices.size());

        for(T v : vertices) {
            if(stack.size() > 0) {
                System.out.println("JUAN FOUND A NON EMPTY STACK!");
            }

            if(!visitedVertices.containsKey(v.getId())) {
                visit(graph, v, visitedVertices, stack, components);
            }
        }

        return components;
    }

    public static HashMap<String, ArrayList<StateVertex>> groupByClass(final Graph<StateVertex, StateEdge> graph) {
        HashMap<String, ArrayList<StateVertex>> visitedVertices = new HashMap<>();

        Stack<StateVertex> stack = new Stack<>();
        stack.addAll(graph.getVertices());

        while(stack.size() > 0) {
            StateVertex v = stack.pop();
            if (v instanceof LayoutMethodVertex) {
                String className = ((LayoutMethodVertex) v).getClassName();
                addVertexToClassGroup(visitedVertices, className, v);
            } else if (v instanceof LayoutLoopVertex) {
                String className = ((LayoutLoopVertex) v).getClassName();
                addVertexToClassGroup(visitedVertices, className, v);
            } else {
                System.out.println("Error! Unhandled vertex type in GraphUtils.groupByClass.");
            }
        }

        return visitedVertices;
    }

    private static void addVertexToClassGroup(HashMap<String, ArrayList<StateVertex>> visitedVertices,
                                             String className, StateVertex vertex) {

        if(visitedVertices.containsKey(className)) {
            visitedVertices.get(className).add(vertex);
        }
        else {
            ArrayList<StateVertex> list = new ArrayList<>();
            list.add(vertex);
            visitedVertices.put(className, list);
        }
    }

    // The immutable graph for our root has already been set, so now we construct its visible graph of vertices
    // matching our predicate.
    public static <T extends HierarchicalVertex<T, S>, S extends Edge<T>> T constructVisibleGraph(T self, Predicate<T> isVisible, BiFunction<T, T, S> edgeBuilder) {
        System.out.println("Constructing visible graph for HierarchicalVertex: " + self);
        Graph<T, S> immutableGraph = self.getChildGraph();
        T visibleRoot = self.copy();
        Graph<T, S> visibleGraph = visibleRoot.getChildGraph();

        // Add all visible vertices
        for(T v : immutableGraph.getVertices()) {
            if(isVisible.test(v)) {
                visibleGraph.addVertex(v);
                v.setParentGraph(visibleGraph);
                GraphUtils.constructVisibleGraph(v, isVisible, edgeBuilder);
            }
        }

        // For each HierarchicalVertex, search for its visible incoming edges
        for(T v : immutableGraph.getVertices()) {
            // TODO: This might be an inefficient way to construct the currently visible graph.
            if(isVisible.test(v)) {
                GraphUtils.findVisibleEdges(self, v, visibleGraph, edgeBuilder);
            }
        }

        return visibleRoot;
    }

    private static <T extends HierarchicalVertex<T, S>, S extends Edge<T>> void findVisibleEdges(T self, T v, Graph<T, S> visibleGraph, BiFunction<T, T, S> edgeBuilder) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        Graph<T, S> immutableGraph = self.getChildGraph();
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
    public static <T extends HierarchicalVertex<T, S>, S extends Edge<T>> T constructCompressedGraph(T self, Function<T, String> hash, BiFunction<String, Set<T>, T> componentVertexBuilder, BiFunction<T, T, S> edgeBuilder) {
        // If we have no child vertices, just copy ourselves.
        if (self.getChildGraph().getVertices().size() == 0) {
            return self.copy();
        }

        // Otherwise, build a map of components based on matching hash values.
        int nullCounter = 0;
        HashMap<T, String> hashStrings = new HashMap<>();
        HashMap<String, Set<T>> components = new HashMap<>();
        for(T v : self.getChildGraph().getVertices()) {
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

        T newRoot = self.copy();
        Graph<T, S> newChildGraph = newRoot.getChildGraph();
        HashMap<String, T> mapStringToVertex = new HashMap<>();
        for(Map.Entry<String, Set<T>> componentEntry : components.entrySet()) {
            Set<T> component = componentEntry.getValue();

            // Preserve the singleton vertices, but build component vertices for larger components.
            // Note that we apply our compression recursively on each vertex before it is added.
            T componentVertex;
            if(component.size() == 1) {
                componentVertex =
                        GraphUtils.constructCompressedGraph(component.iterator().next(), hash, componentVertexBuilder, edgeBuilder);
            } else {
                // We need these maps so we can add edges inside our component vertex.
                // TODO: There has to be a cleaner way to do this...
                HashMap<T, T> mapCompressedToOriginal = new HashMap<>();
                HashMap<T, T> mapOriginalToCompressed = new HashMap<>();
                Set<T> compressedComponent = new HashSet<>();
                for(T vertex : component) {
                    T compressedVertex = GraphUtils.constructCompressedGraph(vertex, hash, componentVertexBuilder, edgeBuilder);
                    compressedComponent.add(compressedVertex);
                    mapCompressedToOriginal.put(compressedVertex, vertex);
                    mapOriginalToCompressed.put(vertex, compressedVertex);
                }

                // Build component of compressed vertices, and add edges.
                // We go from a compressed vertex inside our component, to its corresponding vertex in the original graph,
                // to each neighbor of that original vertex, and then to the compressed vertices corresponding to those neighbors.
                componentVertex = componentVertexBuilder.apply(componentEntry.getKey(), compressedComponent);
                for(T compressedVertex : componentVertex.getChildGraph().getVertices()) {
                    T origVertex = mapCompressedToOriginal.get(compressedVertex);
                    for(T adjOriginalVertex : self.getChildGraph().getOutNeighbors(origVertex)) {
                        T adjCompressedVertex = mapOriginalToCompressed.get(adjOriginalVertex);
                        componentVertex.getChildGraph().addEdge(edgeBuilder.apply(compressedVertex, adjCompressedVertex));
                    }
                }
            }

            newChildGraph.addVertex(componentVertex);
            mapStringToVertex.put(componentEntry.getKey(), componentVertex);
        }

        // Add edges between component vertices.
        // From an old vertex, we get the hash string from one map, then pass it to a different map
        // to get the new vertex.
        for(T currVertexOld: self.getChildGraph().getVertices()) {
            T currVertexNew = mapStringToVertex.get(hashStrings.get(currVertexOld));
            for(T nextVertexOld : self.getChildGraph().getOutNeighbors(currVertexOld)) {
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
}
