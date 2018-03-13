package org.ucombinator.jaam.visualizer.layout;

import java.util.HashSet;
import java.util.Set;

public class VisibleHierarchicalGraph<T extends AbstractLayoutVertex<T>>
        extends HierarchicalGraph<T> {

    public VisibleHierarchicalGraph(T root) {
        super();
        this.root = root;
        this.root.setVisibleInnerGraph(this);
    }
}
