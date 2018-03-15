package org.ucombinator.jaam.visualizer.graph;

public interface Edge<T extends Vertex> {
    T getSrc();
    T getDest();
}
