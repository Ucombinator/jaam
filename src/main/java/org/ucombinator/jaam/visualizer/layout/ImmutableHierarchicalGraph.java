package org.ucombinator.jaam.visualizer.layout;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

public class ImmutableHierarchicalGraph<T extends AbstractLayoutVertex<T>>
        extends HierarchicalGraph<T> {

    public ImmutableHierarchicalGraph(T root) {
        super(root);
        this.root.setImmutableInnerGraph(this);
    }

    static public <T extends AbstractLayoutVertex<T>> VisibleHierarchicalGraph<T> constructVisibleGraph(HierarchicalGraph<T> self, Predicate<T> p) {
        System.out.println("Constructing visible graph for vertex: " + self.getRoot());
        VisibleHierarchicalGraph<T> visibleGraph = new VisibleHierarchicalGraph(self.getRoot());
        for(T v : self.getVertices()) {
            if(p.test(v)) {
                visibleGraph.addVertex(v);
                v.setVisibleSelfGraph(visibleGraph);
                v.setVisibleInnerGraph(constructVisibleGraph(v.getImmutableInnerGraph(), p));
            }
        }

        for(T v : self.getVertices()) {
            // TODO: This might be an inefficient way to construct the currently visible graph.
            if(p.test(v)) {
                findVisibleInEdges(self, v, visibleGraph);
                findVisibleOutEdges(self, v, visibleGraph);
            }
        }

        return visibleGraph;
    }

    static private <T extends AbstractLayoutVertex<T>> void findVisibleInEdges(HierarchicalGraph<T> self, T v, HierarchicalGraph<T> visibleGraph) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        queue.addAll(self.getInNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            found.add(w);
            if(visibleGraph.getVertices().contains(w)) {
                System.out.println("Adding edge to visible graph: " + w + " --> " + v);
                visibleGraph.addEdge(new LayoutEdge<>(w, v, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
            }
            else {
                for (T nextW : self.getInNeighbors(w)) {
                    if (!found.contains(nextW)) {
                        queue.add(nextW);
                    }
                }
            }
        }
    }

    static private <T extends AbstractLayoutVertex<T>> void findVisibleOutEdges(HierarchicalGraph<T> self, T v, HierarchicalGraph<T> visibleGraph) {
        Queue<T> queue = new LinkedList<>();
        HashSet<T> found = new HashSet<>();
        found.add(v);
        queue.addAll(self.getOutNeighbors(v));

        while (queue.size() > 0) {
            T w = queue.poll();
            found.add(w);
            if(visibleGraph.getVertices().contains(w)) {
                System.out.println("Adding edge to visible graph: " + v + " --> " + w);
                visibleGraph.addEdge(new LayoutEdge<>(v, w, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
            }
            else {
                for (T nextW : self.getOutNeighbors(w)) {
                    if (!found.contains(nextW)) {
                        queue.add(nextW);
                    }
                }
            }
        }
    }
}
