package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class TaintVertex extends AbstractLayoutVertex<TaintVertex>
        implements HierarchicalVertex<TaintVertex, TaintEdge> {

    public static Color defaultColor = Color.LIGHTGREEN;
    public static Color currMethodColor = Color.DARKGREEN;
    public static Color upColor = Color.RED;
    public static Color downColor = Color.BLUE;
    public static Color bothColor = Color.DARKGOLDENROD;
    public static Color constColor = Color.BEIGE;
    public static Color sccColor = Color.GRAY;

    private Graph<TaintVertex, TaintEdge> parentGraph;
    private Graph<TaintVertex, TaintEdge> childGraph;

    public TaintVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
        this.parentGraph = new Graph<>();
        this.childGraph = new Graph<>();
    }

    public Graph<TaintVertex, TaintEdge> getParentGraph() {
        return this.parentGraph;
    }

    public Graph<TaintVertex, TaintEdge> getChildGraph() {
        return this.childGraph;
    }

    public void setParentGraph(Graph<TaintVertex, TaintEdge> graph) {
        this.parentGraph = graph;
    }

    public void setChildGraph(Graph<TaintVertex, TaintEdge> graph) {
        this.childGraph = graph;
    }

    public TaintVertex groupByStatement() {
        return GraphUtils.constructCompressedGraph(this,
                new Function<TaintVertex, String>() {
                    @Override
                    public String apply(TaintVertex v) {
                        if(v.getStmtString() == null) {
                            Random r = new Random();
                            return Long.toString(r.nextLong()); // These will almost certainly be unique
                        }
                        else {
                            return v.getStmtString();
                        }
                    }
                },
                new Function<List<TaintVertex>, TaintVertex>() {
                    @Override
                    public TaintVertex apply(List<TaintVertex> list) {
                        return new TaintStmtVertex(list);
                    }
                },
                new BiFunction<TaintVertex, TaintVertex, TaintEdge>() {
                    @Override
                    public TaintEdge apply(TaintVertex v1, TaintVertex v2) {
                        return new TaintEdge(v1, v2);
                    }
        });
    }

    public void onMouseClick(MouseEvent event) {
        if (event.isShiftDown()) {
            System.out.println("Shift is down!\n");
            Main.getSelectedMainTabController().addToHighlighted(this);
        } else {
            Main.getSelectedMainTabController().resetHighlighted(this);
        }
        this.getGraphics().fireEvent(new SelectEvent<TaintVertex>(MouseButton.PRIMARY, this.getGraphics(), this));
    }

    public void searchByMethodNames(Set<String> searchMethodNames, Set<TaintVertex> results) {
        for(TaintVertex v : this.getChildGraph().getVertices()) {
            HashSet<String> currMethodNames = v.getMethodNames();
            HashSet<String> intersection = new HashSet<>(searchMethodNames);
            intersection.retainAll(currMethodNames);

            if (intersection.size() > 0) {
                results.add(v);
                v.searchByMethodNames(searchMethodNames, results);
            }
        }
    }

    public Set<TaintEdge> getIncidentEdges() {
        Set<TaintEdge> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.getParentGraph().getInEdges(this));
        incidentEdges.addAll(this.getParentGraph().getOutEdges(this));
        return incidentEdges;
    }

    public abstract HashSet<String> getMethodNames();

    public abstract boolean hasField();

    // This should probably less specific
    public abstract void getFields(Collection<TaintAddress> store);

    public abstract String getStmtString();
}
