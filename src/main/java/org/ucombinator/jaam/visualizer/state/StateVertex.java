package org.ucombinator.jaam.visualizer.state;

import javafx.animation.ParallelTransition;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.gui.TransitionFactory;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class StateVertex extends AbstractLayoutVertex<StateVertex>
        implements HierarchicalVertex<StateVertex, StateEdge> {

    private Graph<StateVertex, StateEdge> outerGraph;
    private Graph<StateVertex, StateEdge> innerGraph;

    public StateVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
        this.innerGraph = new Graph<>();
        this.outerGraph = new Graph<>();
    }

    public StateVertex(int id, String label, VertexType type) {
        super(id, label, type);
        this.innerGraph = new Graph<>();
        this.outerGraph = new Graph<>();
    }

    public Graph<StateVertex, StateEdge> getOuterGraph() {
        return this.outerGraph;
    }

    public Graph<StateVertex, StateEdge> getInnerGraph() {
        return this.innerGraph;
    }

    public void setOuterGraph(Graph<StateVertex, StateEdge> graph) {
        this.outerGraph = graph;
    }

    public void setInnerGraph(Graph<StateVertex, StateEdge> graph) {
        this.innerGraph = graph;
    }

    public void onMouseClick(MouseEvent event) {
        switch (event.getClickCount()) {
            case 1:
                if (event.isShiftDown()) {
                    Main.getSelectedMainTabController().addToHighlighted(this);
                } else {
                    Main.getSelectedMainTabController().resetHighlighted(this);
                }

                this.getGraphics().fireEvent(new SelectEvent<StateVertex>(MouseButton.PRIMARY, this.getGraphics(), this));
                break;
            case 2:
                handleDoubleClick(event);
                break;
            default:
                // Do nothing
                break;
        }
    }

    private void handleDoubleClick(MouseEvent event) {
        StateRootVertex root = (StateRootVertex) Main.getSelectedVizPanelController().getVisibleRoot();
        Graph<StateVertex, StateEdge> innerGraph = this.getInnerGraph();
        boolean isExpanded = this.isExpanded();

        double newOpacity = isExpanded ? 0.0 : 1.0;
        boolean newVisible = !isExpanded;

        // First we want the content of the clicked node to appear/disappear.
        System.out.println("Changing opacity of child graph...");

        for(StateVertex v: innerGraph.getVertices()) {
            v.setOpacity(newOpacity);
        }

        for(StateEdge e: innerGraph.getEdges()){
            e.setOpacity(newOpacity);
        }

        ParallelTransition pt = TransitionFactory.buildRecursiveTransition(root);
        pt.setOnFinished(
            event1 -> {
                // Then we want the vertices to move to their final positions and the clicked vertex
                // to change its size.
                this.setExpanded(!isExpanded);

                for (StateVertex v: innerGraph.getVertices()) {
                    v.setVisible(newVisible);
                }

                for (StateEdge e: innerGraph.getEdges()) {
                    e.redrawAndSetVisible(newVisible);
                }

                LayoutAlgorithm.layout(root);
                ParallelTransition pt1 = TransitionFactory.buildRecursiveTransition(root);

                // Lastly we redraw the edges that may have been moved.
                // We don't need to do anything to the vertices, so we pass an empty function.
                // TODO: Eliminate the need for this by adding a new apply function that only takes the second function.
                pt1.setOnFinished(event2 -> root.applyToEdgesRecursive(
                        new Consumer<HierarchicalVertex<StateVertex, StateEdge>>() {
                            @Override
                            public void accept(HierarchicalVertex<StateVertex, StateEdge> stateVertexStateEdgeHierarchicalVertex) {}
                        },
                        (StateEdge e) -> e.redrawEdge()));
                pt1.play();
            }
        );

        pt.play();
    }

    public void searchByID(int id, MainTabController mainTab)
    {
        this.searchByIDRange(id, id, mainTab);
    }

    public void searchByIDRange(int id1, int id2, MainTabController mainTab)
    {
        if(this.getId() >= id1 && this.getId() <= id2) {
            this.setHighlighted(true);
            mainTab.getVizHighlighted().add(this);
            System.out.println("Search successful: " + this.getId());
        }

        for(StateVertex v : this.getInnerGraph().getVertices())
            v.searchByIDRange(id1, id2, mainTab);
    }

    public Set<StateEdge> getIncidentEdges() {
        Set<StateEdge> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.getOuterGraph().getInEdges(this));
        incidentEdges.addAll(this.getOuterGraph().getOutEdges(this));
        return incidentEdges;
    }

    public Set<String> getMethodNames() {
        Set<StateMethodVertex> methodVertices = this.getMethodVertices();
        Set<String> methodNames = new HashSet<>();
        for(StateMethodVertex v : methodVertices) {
            methodNames.add(v.getMethodName());
        }

        return methodNames;
    }

    // Subclasses must implement these so that we have descriptions for each of them,
    // and so that our generic collapsing can work for all of them
    public abstract String getRightPanelContent();

    // These searches may be different for different subclasses, so we implement them there.
    public abstract boolean searchByMethod(String query, MainTabController mainTab);

    // This is needed so that we can show the code for the methods that correspond to selected vertices
    public Set<StateMethodVertex> getMethodVertices() {
        return this.getInnerGraph().getVertices()
                .stream()
                .map(StateVertex::getMethodVertices)
                .reduce(new HashSet<>(), (x, y) -> {
                    x.addAll(y);
                    return x;
                });
    }

    public Set<String> getClassNames() {
        return this.getInnerGraph().getVertices()
                .stream()
                .map(StateVertex::getClassNames)
                .reduce(new HashSet<>(), (x, y) -> {
                    x.addAll(y);
                    return x;
                });
    }
}
