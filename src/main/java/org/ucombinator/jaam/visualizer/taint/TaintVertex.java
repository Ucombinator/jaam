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

public abstract class TaintVertex extends AbstractLayoutVertex
        implements HierarchicalVertex<TaintVertex, TaintEdge> {

    public static Color defaultColor = Color.LIGHTGREEN;
    public static Color currMethodColor = Color.DARKGREEN;
    public static Color upColor = Color.RED;
    public static Color downColor = Color.BLUE;
    public static Color bothColor = Color.DARKGOLDENROD;
    public static Color constColor = Color.BEIGE;
    public static Color sccColor = Color.GRAY;

    private Graph<TaintVertex, TaintEdge> outerGraph;
    private Graph<TaintVertex, TaintEdge> innerGraph;

    public TaintVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
        this.outerGraph = new Graph<>();
        this.innerGraph = new Graph<>();
    }

    public Graph<TaintVertex, TaintEdge> getOuterGraph() {
        return this.outerGraph;
    }

    public Graph<TaintVertex, TaintEdge> getInnerGraph() {
        return this.innerGraph;
    }

    public void setOuterGraph(Graph<TaintVertex, TaintEdge> graph) {
        this.outerGraph = graph;
    }

    public void setInnerGraph(Graph<TaintVertex, TaintEdge> graph) {
        this.innerGraph = graph;
    }

    public TaintVertex groupByStatement() {
        return GraphUtils.compressGraph(this,
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

        switch (event.getClickCount()) {
            case 1:
                if (event.isShiftDown()) {
                    System.out.println("Shift is down!\n");
                    Main.getSelectedMainTabController().addToHighlighted(this);
                } else {
                    Main.getSelectedMainTabController().resetHighlighted(this);
                }
                this.getGraphics().fireEvent(new SelectEvent<TaintVertex>(MouseButton.PRIMARY, this.getGraphics()
                        , this, SelectEvent.VERTEX_TYPE.TAINT));
                break;
            case 2:
                if (this instanceof TaintMethodVertex) {
                    ((TaintMethodVertex)this).handleDoubleClick();
                }

                break;
            default:
                // Do nothing
                break;
        }
    }

    public void searchByMethodNames(Set<String> searchMethodNames, Set<TaintVertex> results) {
        for(TaintVertex v : this.getInnerGraph().getVertices()) {
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
        incidentEdges.addAll(this.getOuterGraph().getInEdges(this));
        incidentEdges.addAll(this.getOuterGraph().getOutEdges(this));
        return incidentEdges;
    }

    public abstract HashSet<String> getMethodNames();
    public abstract boolean hasField();
    public abstract void getFields(Collection<TaintAddress> store);
    public abstract String getStmtString();
    public abstract List<TaintVertex> expand();

    public String getClassName() {
        return null;
    }

    public String getMethodName() {
        return null;
    }

}
