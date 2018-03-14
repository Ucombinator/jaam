package org.ucombinator.jaam.visualizer.layout;

import javafx.animation.ParallelTransition;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.gui.TransitionFactory;
import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalGraph;
import org.ucombinator.jaam.visualizer.hierarchical.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.taint.TaintVertex;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class StateVertex extends AbstractLayoutVertex<StateVertex>
        implements HierarchicalVertex<StateVertex, StateEdge> {

    private HierarchicalGraph<StateVertex, StateEdge> visibleSelfGraph;
    private HierarchicalGraph<StateVertex, StateEdge> visibleInnerGraph;
    private HierarchicalGraph<StateVertex, StateEdge> immutableSelfGraph;
    private HierarchicalGraph<StateVertex, StateEdge> immutableInnerGraph;

    // TODO: How do we initialize the self graphs?
    public StateVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
        this.immutableInnerGraph = new HierarchicalGraph<>(this);
        this.visibleInnerGraph = new HierarchicalGraph<>(this);
        this.immutableSelfGraph = new HierarchicalGraph<>(null);
        this.visibleSelfGraph = new HierarchicalGraph<>(null);
    }

    public StateVertex(int id, String label, VertexType type) {
        super(id, label, type);
        this.immutableInnerGraph = new HierarchicalGraph<>(this);
        this.visibleInnerGraph = new HierarchicalGraph<>(this);
        this.immutableSelfGraph = new HierarchicalGraph<>(null);
        this.visibleSelfGraph = new HierarchicalGraph<>(null);
    }

    public HierarchicalGraph<StateVertex, StateEdge> getVisibleSelfGraph() {
        return this.visibleSelfGraph;
    }

    public HierarchicalGraph<StateVertex, StateEdge> getVisibleInnerGraph() {
        return this.visibleInnerGraph;
    }

    public HierarchicalGraph<StateVertex, StateEdge> getImmutableSelfGraph() {
        return this.immutableSelfGraph;
    }

    public HierarchicalGraph<StateVertex, StateEdge> getImmutableInnerGraph() {
        return this.immutableInnerGraph;
    }

    public void setVisibleSelfGraph(HierarchicalGraph<StateVertex, StateEdge> graph) {
        this.visibleSelfGraph = graph;
    }

    public void setVisibleInnerGraph(HierarchicalGraph<StateVertex, StateEdge> graph) {
        this.visibleInnerGraph = graph;
    }

    public void setImmutableSelfGraph(HierarchicalGraph<StateVertex, StateEdge> graph) {
        this.immutableSelfGraph = graph;
    }

    public void setImmutableInnerGraph(HierarchicalGraph<StateVertex, StateEdge> graph) {
        this.immutableInnerGraph = graph;
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
        LayoutRootVertex root = Main.getSelectedVizPanelController().getPanelRoot();
        HierarchicalGraph<StateVertex, StateEdge> innerGraph = this.getVisibleInnerGraph();
        boolean isExpanded = this.isExpanded();

        double newOpacity = isExpanded ? 0.0 : 1.0;
        boolean newVisible = !isExpanded;

        // First we want the content of the clicked node to appear/disappear.
        System.out.println("Changing opacity of inner graph...");

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
                pt1.setOnFinished(event2 -> HierarchicalVertex.applyToVisibleEdgesRec(root,
                        // We don't need to do anything to the vertices, so we pass an empty function.
                        // TODO: Eliminate the need for this by adding a new apply function that only takes the second function.
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

        for(StateVertex v : this.getVisibleInnerGraph().getVertices())
            v.searchByIDRange(id1, id2, mainTab);
    }

    public HashSet<StateVertex> getVisibleAncestors()
    {
        HashSet<StateVertex> ancestors = new HashSet<>();
        this.getVisibleAncestors(ancestors);

        return ancestors;
    }

    private void getVisibleAncestors(HashSet<StateVertex> ancestors)
    {
       if(this instanceof LayoutRootVertex)
           return;

       ancestors.add(this);
       this.getVisibleSelfGraph().getInNeighbors(this).stream().forEach(v -> {
           if (!ancestors.contains(v)) {
               v.getVisibleAncestors(ancestors);
           }
       });
    }

    public HashSet<StateVertex> getVisibleDescendants()
    {
        HashSet<StateVertex> descendants = new HashSet<>();
        this.getVisibleDescendants(descendants);

        return descendants;
    }

    private void getVisibleDescendants(HashSet<StateVertex> descendants)
    {
        if(this instanceof LayoutRootVertex)
            return;

        descendants.add(this);
        this.getVisibleSelfGraph().getOutNeighbors(this).stream().forEach(v -> {
            if (!descendants.contains(v)) {
                v.getVisibleDescendants(descendants);
            }
        });
    }

    // Subclasses must implement these so that we have descriptions for each of them,
    // and so that our generic collapsing can work for all of them
    public abstract String getRightPanelContent();

    // These searches may be different for different subclasses, so we implement them there.
    public abstract boolean searchByMethod(String query, MainTabController mainTab);

    // This is needed so that we can show the code for the methods that correspond to selected vertices
    public abstract HashSet<LayoutMethodVertex> getMethodVertices();

    public HashSet<String> getMethodNames() {
        HashSet<LayoutMethodVertex> methodVertices = this.getMethodVertices();
        HashSet<String> methodNames = new HashSet<>();
        for(LayoutMethodVertex v : methodVertices) {
            methodNames.add(v.getMethodName());
        }

        return methodNames;
    }

    public abstract HashSet<String> getClassNames();

    public Set<LayoutEdge<StateVertex>> getVisibleIncidentEdges() {
        Set<LayoutEdge<StateVertex>> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.visibleSelfGraph.getInEdges(this));
        incidentEdges.addAll(this.visibleSelfGraph.getOutEdges(this));
        return incidentEdges;
    }

    public Set<LayoutEdge<StateVertex>> getImmutableIncidentEdge() {
        Set<LayoutEdge<StateVertex>> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.immutableSelfGraph.getInEdges(this));
        incidentEdges.addAll(this.immutableSelfGraph.getOutEdges(this));
        return incidentEdges;
    }
}
