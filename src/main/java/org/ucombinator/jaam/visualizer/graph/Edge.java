package org.ucombinator.jaam.visualizer.graph;

public interface Edge<T extends AbstractVertex> {
    T getSrc();
    T getDest();
}
