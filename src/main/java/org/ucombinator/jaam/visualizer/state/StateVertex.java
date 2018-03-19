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

    private Graph<StateVertex, StateEdge> parentGraph;
    private Graph<StateVertex, StateEdge> childGraph;

    // TODO: How do we initialize the self graphs?
    public StateVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
        this.childGraph = new Graph<>();
        this.parentGraph = new Graph<>();
    }

    public StateVertex(int id, String label, VertexType type) {
        super(id, label, type);
        this.childGraph = new Graph<>();
        this.parentGraph = new Graph<>();
    }

    public Graph<StateVertex, StateEdge> getParentGraph() {
        return this.parentGraph;
    }

    public Graph<StateVertex, StateEdge> getChildGraph() {
        return this.childGraph;
    }

    public void setParentGraph(Graph<StateVertex, StateEdge> graph) {
        this.parentGraph = graph;
    }

    public void setChildGraph(Graph<StateVertex, StateEdge> graph) {
        this.childGraph = graph;
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

    private void handleDoubleClick(MouseEvent event){
        StateRootVertex root = Main.getSelectedVizPanelController().getVisibleRoot();
        Graph<StateVertex, StateEdge> childGraph = this.getChildGraph();
        boolean isExpanded = this.isExpanded();

        double newOpacity = isExpanded ? 0.0 : 1.0;
        boolean newVisible = !isExpanded;

        // First we want the content of the clicked node to appear/disappear.
        System.out.println("Changing opacity of child graph...");

        for(StateVertex v: childGraph.getVertices()) {
            v.setOpacity(newOpacity);
        }

        for(StateEdge e: childGraph.getEdges()){
            e.setOpacity(newOpacity);
        }

        ParallelTransition pt = TransitionFactory.buildRecursiveTransition(root);
        pt.setOnFinished(
            event1 -> {
                // Then we want the vertices to move to their final positions and the clicked vertex
                // to change its size.
                this.setExpanded(!isExpanded);

                for (StateVertex v: childGraph.getVertices()) {
                    v.setVisible(newVisible);
                }

                for (StateEdge e: childGraph.getEdges()) {
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

        System.out.println("Simultaneous transitions: " + pt.getChildren().size());
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

        for(StateVertex v : this.getChildGraph().getVertices())
            v.searchByIDRange(id1, id2, mainTab);
    }

    public HashSet<StateVertex> getAncestors()
    {
        HashSet<StateVertex> ancestors = new HashSet<>();
        this.getAncestors(ancestors);

        return ancestors;
    }

    private void getAncestors(HashSet<StateVertex> ancestors)
    {
       if(this instanceof StateRootVertex)
           return;

       ancestors.add(this);
       this.getParentGraph().getInNeighbors(this).stream().forEach(v -> {
           if (!ancestors.contains(v)) {
               v.getAncestors(ancestors);
           }
       });
    }

    public HashSet<StateVertex> getDescendants()
    {
        HashSet<StateVertex> descendants = new HashSet<>();
        this.getDescendants(descendants);

        return descendants;
    }

    private void getDescendants(HashSet<StateVertex> descendants)
    {
        if(this instanceof StateRootVertex)
            return;

        descendants.add(this);
        this.getParentGraph().getOutNeighbors(this).stream().forEach(v -> {
            if (!descendants.contains(v)) {
                v.getDescendants(descendants);
            }
        });
    }

    public Set<StateEdge> getIncidentEdges() {
        Set<StateEdge> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.getParentGraph().getInEdges(this));
        incidentEdges.addAll(this.getParentGraph().getOutEdges(this));
        return incidentEdges;
    }

    public HashSet<String> getMethodNames() {
        HashSet<StateMethodVertex> methodVertices = this.getMethodVertices();
        HashSet<String> methodNames = new HashSet<>();
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
    public abstract HashSet<StateMethodVertex> getMethodVertices();

    public abstract HashSet<String> getClassNames();
}
