package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class LayoutRootVertex extends AbstractLayoutVertex {

    Color defaultColor = Color.WHITE;

    public LayoutRootVertex() {
        super("root", VertexType.ROOT, false);
        this.color = defaultColor;
    }

    public String getRightPanelContent() {
        return "Root vertex";
    }

    public String getShortDescription() {
        return "Root vertex";
    }

    public boolean searchByMethod(String query, MainTabController mainTab) {
        boolean found = false;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
            found = found || v.searchByMethod(query, mainTab);
        }

        this.setHighlighted(found);
        mainTab.getHighlighted().add(this);

        return found;
    }

    public HashSet<LayoutMethodVertex> getMethodVertices()
    {
        HashSet<LayoutMethodVertex> methodVertices = new LinkedHashSet<LayoutMethodVertex>();
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
            if(v instanceof LayoutMethodVertex)
                methodVertices.add((LayoutMethodVertex) v);
            else
                methodVertices.addAll(v.getMethodVertices());
        }

        return methodVertices;
    }

}
