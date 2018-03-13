package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.HashSet;

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

}
