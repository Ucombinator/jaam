package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

import java.util.Collection;
import java.util.HashSet;

public abstract class TaintVertex extends AbstractLayoutVertex<TaintVertex> {

    public static Color defaultColor = Color.LIGHTGREEN;
    public static Color currMethodColor = Color.DARKGREEN;
    public static Color upColor = Color.RED;
    public static Color downColor = Color.BLUE;
    public static Color bothColor = Color.DARKGOLDENROD;
    public static Color constColor = Color.BEIGE;
    public static Color sccColor = Color.GRAY;

    public TaintVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
    }

    public void searchByMethodNames(HashSet<String> searchMethodNames, HashSet<TaintVertex> results) {
        System.out.println("Searching for methods: " + String.join(" ", searchMethodNames));
        for(TaintVertex v : this.getImmutableInnerGraph().getVertices()) {
            HashSet<String> currMethodNames = v.getMethodNames();
            System.out.println("Vertex: " + v);
            System.out.println("Methods: " + String.join(" ", currMethodNames));
            HashSet<String> intersection = (HashSet<String>) searchMethodNames.clone();
            intersection.retainAll(currMethodNames);

            if (intersection.size() > 0) {
                results.add(v);
                v.searchByMethodNames(searchMethodNames, results);
            }
        }
    }

    public abstract HashSet<String> getMethodNames();

    public abstract boolean hasField();

    // This should probably less specific
    public abstract void getFields(Collection<TaintAddress> store);
}
