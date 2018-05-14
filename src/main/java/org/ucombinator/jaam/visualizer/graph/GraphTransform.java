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

}
