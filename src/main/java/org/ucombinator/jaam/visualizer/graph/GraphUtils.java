package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.visualizer.layout.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

        Set<T> neighbors = graph.getOutNeighbors(v);
        for (T n : neighbors) {
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
        System.out.println("Finding strongly connected components...");
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

    // The immutable graph for our root has already been set, so now we construct its visible graph of vertices
    // matching our predicate.
    public static <R extends T, T extends HierarchicalVertex<T, S>, S extends Edge<T>>
    GraphTransform<R,T> constructVisibleGraph(R self, Predicate<T> isVisible, BiFunction<T, T, S> edgeBuilder) {
        Graph<T, S> immutableGraph = self.getInnerGraph();

        R visibleRoot = (R) self.copy(); // Have to cast it, is there a less ugly way?

        GraphTransform<R,T> transform = new GraphTransform<>(self, visibleRoot);

        Graph<T, S> visibleGraph = visibleRoot.getInnerGraph();

        // Add all visible vertices
        for(T v : immutableGraph.getVertices()) {
            if(isVisible.test(v)) {
                visibleGraph.addVertex(v);
                v.setOuterGraph(visibleGraph);
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

        return transform;
    }

    private static <T extends HierarchicalVertex<T, S>, S extends Edge<T>> void findVisibleEdges(T self, T v, Graph<T, S> visibleGraph, BiFunction<T, T, S> edgeBuilder) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        Graph<T, S> immutableGraph = self.getInnerGraph();
        queue.addAll(immutableGraph.getInNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            if (!found.contains(w)) {
                found.add(w);
                if (visibleGraph.getVertices().contains(w)) {
                    visibleGraph.addEdge(edgeBuilder.apply(w, v));
                } else {
                    queue.addAll(immutableGraph.getInNeighbors(w));
                }
            }
        }
    }

    // We assume that only vertices within the same level can be combined.
    public static <T extends HierarchicalVertex<T, S>, S extends Edge<T>, U>
    T copyAndCompressGraph(T root, Function<T, U> hash, Function<List<T>, T> componentVertexBuilder,
                           BiFunction<T, T, S> edgeBuilder) {
        // If we have no child vertices, just copy ourselves.
        if (root.getInnerGraph().getVertices().size() == 0) {
            return root.copy();
        }

        // Otherwise, copy all of the inner vertices.
        Map<T, T> mapVertexToCopy = root.getInnerGraph().getVertices().stream()
                .collect(Collectors.toMap(v -> v, HierarchicalVertex::copy));

        // Then build a map of components based on matching hash values.
        Map<T, U> hashValues = root.getInnerGraph().getVertices().stream()
                .collect(Collectors.toMap(v -> v, hash));

        Map<U, List<T>> components = root.getInnerGraph().getVertices().stream()
                .collect(Collectors.groupingBy(hashValues::get));


        // Preserve the singleton vertices, but build component vertices for larger components.
        Map<U, T> mapHashToComponentVertex = components.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, new Function<Map.Entry<U, List<T>>, T>() {
                    @Override
                    public T apply(Map.Entry<U, List<T>> entry) {
                        List<T> component = entry.getValue();
                        if(component.size() == 1) {
                            return mapVertexToCopy.get(component.get(0));
                        } else {
                            return componentVertexBuilder.apply(component.stream().map(mapVertexToCopy::get)
                                    .collect(Collectors.toList()));
                        }
                    }
                }));

        // Next, make new graph and add component vertices.
        T newRoot = root.copy();
        Graph<T, S> newGraph = newRoot.getInnerGraph();
        mapHashToComponentVertex.entrySet().forEach(entry -> {
            newGraph.addVertex(entry.getValue());
            entry.getValue().setOuterGraph(newGraph);
        });

        // Lastly, add edges between the new vertices.
        for (T currVertexOld: root.getInnerGraph().getVertices()) {
            T currComponentVertex = mapHashToComponentVertex.get(hashValues.get(currVertexOld));
            for (T nextVertexOld : root.getInnerGraph().getOutNeighbors(currVertexOld)) {
                T nextComponentVertex = mapHashToComponentVertex.get(hashValues.get(nextVertexOld));

                // Add edge at the top level of the new graph if it's a self-loop, or if the two vertices are
                // now in different components.
                if ((currVertexOld == nextVertexOld) || (currComponentVertex != nextComponentVertex)) {
                    newGraph.addEdge(edgeBuilder.apply(currComponentVertex, nextComponentVertex));
                }

                // Add edge between two different vertices if they are inside the same component.
                if (currComponentVertex == nextComponentVertex) {
                    T currInnerVertex = mapVertexToCopy.get(currVertexOld);
                    T nextInnerVertex = mapVertexToCopy.get(nextVertexOld);
                    currComponentVertex.getInnerGraph().addEdge(edgeBuilder.apply(currInnerVertex, nextInnerVertex));
                }
            }
        }

        return newRoot;
    }

    // We assume that only vertices within the same level can be combined.
    public static <T extends HierarchicalVertex<T, S>, S extends Edge<T>, U>
    T compressGraph(T root, Function<T, U> hash, Function<List<T>, T> componentVertexBuilder,
                               BiFunction<T, T, S> edgeBuilder) {

        Graph<T, S> flatGraph = root.getInnerGraph();

        // If we have no child vertices, just copy ourselves.
        if (flatGraph.getVertices().size() == 0) {
            return root.copy();
        }

        // Then build a map of components based on matching hash values.
        Map<T, U> hashValues = flatGraph.getVertices().stream()
                .collect(Collectors.toMap(v -> v, hash));

        Map<U, List<T>> components = flatGraph.getVertices().stream()
                .collect(Collectors.groupingBy(hashValues::get));


        // Preserve the singleton vertices, but build component vertices for larger components.
        Map<U, T> mapHashToComponentVertex = components.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, new Function<Map.Entry<U, List<T>>, T>() {
                    @Override
                    public T apply(Map.Entry<U, List<T>> entry) {
                        List<T> component = entry.getValue();
                        if(component.size() == 1) {
                            return component.get(0);
                        } else {
                            return componentVertexBuilder.apply(component.stream().collect(Collectors.toList()));
                        }
                    }
                }));

        // Next, make new graph and add component vertices.
        T newRoot = root.copy();
        Graph<T, S> newGraph = newRoot.getInnerGraph();
        mapHashToComponentVertex.entrySet().forEach(entry -> {
            newGraph.addVertex(entry.getValue());
            entry.getValue().setOuterGraph(newGraph);
        });

        // Lastly, add edges between the new vertices.
        for (T currVertexOld: flatGraph.getVertices()) {
            T currComponentVertex = mapHashToComponentVertex.get(hashValues.get(currVertexOld));
            for (T nextVertexOld : flatGraph.getOutNeighbors(currVertexOld)) {
                T nextComponentVertex = mapHashToComponentVertex.get(hashValues.get(nextVertexOld));

                // Add edge at the top level of the new graph if it's a self-loop, or if the two vertices are
                // now in different components.
                if ((currVertexOld == nextVertexOld) || (currComponentVertex != nextComponentVertex)) {
                    newGraph.addEdge(edgeBuilder.apply(currComponentVertex, nextComponentVertex));
                }

                // Add edge between two different vertices if they are inside the same component.
                if (currComponentVertex == nextComponentVertex) {
                    currComponentVertex.getInnerGraph().addEdge(edgeBuilder.apply(currVertexOld, nextVertexOld));
                }
            }
        }

        return newRoot;
    }

}
