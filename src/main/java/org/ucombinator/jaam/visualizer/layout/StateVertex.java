package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.Instruction;

import java.util.HashSet;
import java.util.LinkedHashSet;

public abstract class StateVertex extends AbstractLayoutVertex<StateVertex> {

    public StateVertex(String label, VertexType type, boolean drawEdges) {
        super(label, type, drawEdges);
    }

    public StateVertex(int id, String label, VertexType type) {
        super(id, label, type);
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

        for(StateVertex v : this.getInnerGraph().getVisibleVertices())
            v.searchByIDRange(id1, id2, mainTab);
    }

    public HashSet<StateVertex> getAncestors()
    {
        HashSet<StateVertex> ancestors = new HashSet<>();
        this.getAncestors(ancestors);

        return ancestors;
    }

    public void getAncestors(HashSet<StateVertex> ancestors)
    {
       if(this instanceof LayoutRootVertex)
           return;
       ancestors.add(this);
       this.getSelfGraph().getVisibleInNeighbors(this).stream().forEach(v -> {
           if (!ancestors.contains(v)) {
               v.getAncestors(ancestors);
           }
       });
    }

    public HashSet<StateVertex> getDescendants()
    {
        HashSet<StateVertex> ancestors = new HashSet<>();
        this.getDescendants(ancestors);

        return ancestors;
    }

    public void getDescendants(HashSet<StateVertex> ancestors)
    {
        if(this instanceof LayoutRootVertex)
            return;
        ancestors.add(this);
        this.getSelfGraph().getVisibleOutNeighbors(this).stream().forEach(v -> {
            if (!ancestors.contains(v)) {
                v.getDescendants(ancestors);
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


}
