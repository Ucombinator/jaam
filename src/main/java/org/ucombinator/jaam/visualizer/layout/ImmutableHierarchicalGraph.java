package org.ucombinator.jaam.visualizer.layout;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;


public class ImmutableHierarchicalGraph<T extends AbstractLayoutVertex<T>>
        extends HierarchicalGraph<T> {

    public ImmutableHierarchicalGraph(T root) {
        super();
        this.root = root;
        this.root.setImmutableInnerGraph(this);
    }

    public VisibleHierarchicalGraph<T> constructVisibleGraph(Predicate<T> p) {
        System.out.println("Constructing visible graph for vertex: " + this.root);
        VisibleHierarchicalGraph<T> visibleGraph = new VisibleHierarchicalGraph(this.root);
        for(T v : this.vertices) {
            if(p.test(v)) {
                visibleGraph.addVertex(v);
                v.setVisibleSelfGraph(visibleGraph);
                v.setVisibleInnerGraph(v.getImmutableInnerGraph().constructVisibleGraph(p));
            }
        }

        for(T v : this.vertices) {
            // TODO: This might be an inefficient way to construct the currently visible graph.
            if(p.test(v)) {
                this.findVisibleInEdges(v, visibleGraph);
                this.findVisibleOutEdges(v, visibleGraph);
            }
        }

        return visibleGraph;
    }

    private void findVisibleInEdges(T v, VisibleHierarchicalGraph<T> visibleGraph) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        queue.addAll(this.getInNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            found.add(w);
            if(visibleGraph.getVertices().contains(w)) {
                System.out.println("Adding edge to visible graph: " + w + " --> " + v);
                visibleGraph.addEdge(new LayoutEdge<>(w, v, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
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

    private void findVisibleOutEdges(T v, VisibleHierarchicalGraph<T> visibleGraph) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        found.add(v);
        queue.addAll(this.getOutNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            found.add(w);
            if(visibleGraph.getVertices().contains(w)) {
                System.out.println("Adding edge to visible graph: " + v + " --> " + w);
                visibleGraph.addEdge(new LayoutEdge<>(v, w, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
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
