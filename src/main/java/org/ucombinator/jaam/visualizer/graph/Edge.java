package org.ucombinator.jaam.visualizer.graph;

public class Edge<T> {

    protected T src, dest;

    public Edge() {
        this.src = null;
        this.dest = null;
    }

    public Edge(T src, T dest) {
        this.construct(src, dest);
    }

    public void construct(T src, T dest) {
        this.src = src;
        this.dest = dest;
    }

    public T getSrc() {
        return this.src;
    }

    public T getDest() {
        return this.dest;
    }
}
