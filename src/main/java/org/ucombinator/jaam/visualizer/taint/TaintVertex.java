package org.ucombinator.jaam.visualizer.taint;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public abstract class TaintVertex extends AbstractLayoutVertex<TaintVertex> {

    public TaintVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
    }

    public void searchByMethodNames(HashSet<String> searchMethodNames, HashSet<TaintVertex> results) {
        for(TaintVertex v : this.getInnerGraph().getVertices()) {
            HashSet<String> currMethodNames = v.getMethodNames();
            HashSet<String> intersection = (HashSet<String>) searchMethodNames.clone();
            intersection.retainAll(currMethodNames);

            if (intersection.size() > 0) {
                results.add(v);
                v.searchByMethodNames(searchMethodNames, results);
            }
        }
    }

    public void setHiddenExcept(HashSet<TaintVertex> verticesToDraw) {
        if(!verticesToDraw.contains(this)) {
            this.getSelfGraph().setHidden(this);
        }
        else {
            for(TaintVertex v : this.getInnerGraph().getVertices()) {
                v.setHiddenExcept(verticesToDraw);
            }
        }
    }

    public abstract HashSet<String> getMethodNames();

    public abstract boolean hasField();

    // This should probably less specific
    public abstract void getFields(Collection<TaintAddress> store);
}
