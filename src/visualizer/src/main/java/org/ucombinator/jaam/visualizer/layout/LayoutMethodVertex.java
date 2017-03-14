package org.ucombinator.jaam.visualizer.layout;

/**
 * Created by timothyjohnson on 2/15/17.
 */
public class LayoutMethodVertex extends AbstractLayoutVertex {

    private String methodName;

    public LayoutMethodVertex(String methodName) {
        super(methodName, VertexType.METHOD);
        this.methodName = methodName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getRightPanelContent() {
        return "Method vertex: " + this.getMethodName();
    }

    public String getShortDescription() {
        return this.getMethodName();
    }

    public boolean searchByMethod(String query) {
        boolean found = query.contains(this.getMethodName());
        this.setHighlighted(found);

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            v.searchByMethod(query);
        }

        return found;
    }

    public void computeLoopHeight()
    {
        this.setLoopHeight(0);
    }
}
