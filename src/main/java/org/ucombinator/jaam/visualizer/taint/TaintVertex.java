package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.layout.StateEdge;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class TaintVertex extends AbstractLayoutVertex<TaintVertex>
        implements HierarchicalVertex<TaintVertex, TaintEdge> {

    public static Color defaultColor = Color.LIGHTGREEN;
    public static Color currMethodColor = Color.DARKGREEN;
    public static Color upColor = Color.RED;
    public static Color downColor = Color.BLUE;
    public static Color bothColor = Color.DARKGOLDENROD;
    public static Color constColor = Color.BEIGE;
    public static Color sccColor = Color.GRAY;

    private Graph<TaintVertex, TaintEdge> selfGraph;
    private Graph<TaintVertex, TaintEdge> innerGraph;

    // TODO: How do we initialize the self graphs?
    public TaintVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
        this.selfGraph = new Graph<>();
        this.innerGraph = new Graph<>();
    }

    public Graph<TaintVertex, TaintEdge> getSelfGraph() {
        return this.selfGraph;
    }

    public Graph<TaintVertex, TaintEdge> getInnerGraph() {
        return this.innerGraph;
    }

    public void setSelfGraph(Graph<TaintVertex, TaintEdge> graph) {
        this.selfGraph = graph;
    }

    public TaintVertex groupByStatement() {
        return GraphUtils.constructCompressedGraph(
                this,
                TaintVertex::getStmtString,
                TaintStmtVertex::new,
                TaintEdge::new);
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

    public void searchByMethodNames(HashSet<String> searchMethodNames, HashSet<TaintVertex> results) {
        System.out.println("Searching for methods: " + String.join(" ", searchMethodNames));
        for(TaintVertex v : this.getInnerGraph().getVertices()) {
            HashSet<String> currMethodNames = v.getMethodNames();
            System.out.println("Vertex: " + v);
            System.out.println("Vertex methods: " + String.join(" ", currMethodNames));
            HashSet<String> intersection = (HashSet<String>) searchMethodNames.clone();
            intersection.retainAll(currMethodNames);

            if (intersection.size() > 0) {
                results.add(v);
                v.searchByMethodNames(searchMethodNames, results);
            }
        }
    }

    public Set<TaintEdge> getIncidentEdges() {
        Set<TaintEdge> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.getSelfGraph().getInEdges(this));
        incidentEdges.addAll(this.getSelfGraph().getOutEdges(this));
        return incidentEdges;
    }

    public abstract HashSet<String> getMethodNames();

    public abstract boolean hasField();

    // This should probably less specific
    public abstract void getFields(Collection<TaintAddress> store);

    public abstract String getStmtString();
}
