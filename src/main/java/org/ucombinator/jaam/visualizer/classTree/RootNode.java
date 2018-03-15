package org.ucombinator.jaam.visualizer.classTree;

import org.ucombinator.jaam.visualizer.controllers.CodeViewController;
import org.ucombinator.jaam.visualizer.state.StateVertex;

import java.util.HashSet;

public class RootNode extends ClassTreeNode implements Comparable<RootNode>{

    public RootNode(String name, String className) {
       super(name, className);
    }

    @Override
    public void handleDoubleClick(CodeViewController codeView) { /* do nothing */ }

    @Override
    public HashSet<StateVertex> getChildVertices() { return new HashSet<>(); }

    @Override
    public int compareTo(RootNode o) {
        return name.compareTo(o.name);
    }
}
