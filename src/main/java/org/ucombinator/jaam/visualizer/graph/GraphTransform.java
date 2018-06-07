package org.ucombinator.jaam.visualizer.graph;

import java.util.HashMap;

public class GraphTransform <R extends T, T extends HierarchicalVertex> {

    public R oldRoot;
    public R newRoot;
    private HashMap<T,T> oldToNew;
    private HashMap<T,T> newToOld;

    public GraphTransform(R oldRoot, R newRoot) {
        this.oldRoot = oldRoot;
        this.newRoot = newRoot;

        oldToNew = new HashMap<>();
        newToOld = new HashMap<>();
    }

    public void add(T oldV, T newV) {
        oldToNew.put(oldV, newV);
        newToOld.put(newV, oldV);
    }

    public T getNew(T oldV) {
        return oldToNew.get(oldV);
    }
    public T getOld(T newV) {
        return newToOld.get(newV);
    }

    public boolean containsNew(T newV) { return newToOld.containsKey(newV); }
    public boolean containsOld(T oldV) { return oldToNew.containsKey(oldV); }

    public static <R extends T, T extends HierarchicalVertex>
    GraphTransform<R,T> transfer(GraphTransform<R,T> aToB, GraphTransform<R,T> bToC) {

        GraphTransform<R,T> aToC = new GraphTransform<>(aToB.oldRoot, bToC.newRoot);

        for (T b : bToC.oldToNew.keySet()) {
            aToC.add(aToB.getOld(b), bToC.getNew(b));
        }

        return aToC;
    }

}
