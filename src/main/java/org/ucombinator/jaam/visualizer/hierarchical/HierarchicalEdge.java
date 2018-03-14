package org.ucombinator.jaam.visualizer.hierarchical;

public interface HierarchicalEdge<T extends HierarchicalVertex> {
    T getSrc();
    T getDest();
}
