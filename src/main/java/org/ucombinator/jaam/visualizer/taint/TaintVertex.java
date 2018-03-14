package org.ucombinator.jaam.visualizer.taint;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalGraph;
import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
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

    private HierarchicalGraph<TaintVertex, TaintEdge> visibleSelfGraph;
    private HierarchicalGraph<TaintVertex, TaintEdge> visibleInnerGraph;
    private HierarchicalGraph<TaintVertex, TaintEdge> immutableSelfGraph;
    private HierarchicalGraph<TaintVertex, TaintEdge> immutableInnerGraph;

    // TODO: How do we initialize the self graphs?
    public TaintVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
        this.immutableInnerGraph = new HierarchicalGraph<>(this);
        this.visibleInnerGraph = new HierarchicalGraph<>(this);
        this.immutableSelfGraph = new HierarchicalGraph<>(null);
        this.visibleSelfGraph = new HierarchicalGraph<>(null);
    }

    public HierarchicalGraph<TaintVertex, TaintEdge> getVisibleSelfGraph() {
        return this.visibleSelfGraph;
    }

    public HierarchicalGraph<TaintVertex, TaintEdge> getVisibleInnerGraph() {
        return this.visibleInnerGraph;
    }

    public HierarchicalGraph<TaintVertex, TaintEdge> getImmutableSelfGraph() {
        return this.immutableSelfGraph;
    }

    public HierarchicalGraph<TaintVertex, TaintEdge> getImmutableInnerGraph() {
        return this.immutableInnerGraph;
    }

    public void setVisibleSelfGraph(HierarchicalGraph<TaintVertex, TaintEdge> graph) {
        this.visibleSelfGraph = graph;
    }

    public void setVisibleInnerGraph(HierarchicalGraph<TaintVertex, TaintEdge> graph) {
        this.visibleInnerGraph = graph;
    }

    public void setImmutableSelfGraph(HierarchicalGraph<TaintVertex, TaintEdge> graph) {
        this.immutableSelfGraph = graph;
    }

    public void setImmutableInnerGraph(HierarchicalGraph<TaintVertex, TaintEdge> graph) {
        this.immutableInnerGraph = graph;
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
        for(TaintVertex v : this.getImmutableInnerGraph().getVertices()) {
            HashSet<String> currMethodNames = v.getMethodNames();
            // System.out.println("Vertex: " + v);
            // System.out.println("Methods: " + String.join(" ", currMethodNames));
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

    // TODO: Can we combine this version with the identical version in StateVertex somehow?
    public Set<LayoutEdge<TaintVertex>> getVisibleIncidentEdges() {
        Set<LayoutEdge<TaintVertex>> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.visibleSelfGraph.getInEdges(this));
        incidentEdges.addAll(this.visibleSelfGraph.getOutEdges(this));
        return incidentEdges;
    }

    public Set<LayoutEdge<TaintVertex>> getImmutableIncidentEdge() {
        Set<LayoutEdge<TaintVertex>> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.immutableSelfGraph.getInEdges(this));
        incidentEdges.addAll(this.immutableSelfGraph.getOutEdges(this));
        return incidentEdges;
    }
}
