package org.ucombinator.jaam.visualizer.profiler;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

public class ProfilerEdge implements LayoutEdge<ProfilerVertex>, Edge<ProfilerVertex> {

    private static final Color DEFAULT_COLOR = Color.BLACK;
    private static final Color HIGHLIGHT_COLOR = Color.ORANGERED;
    private static final double DEFAULT_STROKE_WIDTH = 0.5;
    private static final double HIGHLIGHT_STROKE_WIDTH = 2;

    private Group group;
    ProfilerVertex parent, child;
    Line line;

    public ProfilerEdge(ProfilerVertex parent, ProfilerVertex child) {
        this.parent = parent;
        this.child = child;
    }

    public ProfilerVertex getSrc() {
        return this.parent;
    }

    public ProfilerVertex getDest() {
        return this.child;
    }

    // When we first draw the edge, we need to know its graphics parent.
    public void drawEdge(Group contentGroup) {
        this.group = contentGroup;
        this.redrawEdge();
    }

    // After we've drawn the edge once, the graphics parent won't change.
    public void redrawEdge() {
        this.group.getChildren().remove(this.line);

        double x, y1, y2;
        x = child.getEdgeColumn() * ProfilerTree.UNIT_SIZE;
        // System.out.println("Drawing edge for vertex: " + this.child.getId() + " at column " + child.getEdgeColumn());
        if (parent == null) {
            y1 = ProfilerTree.MARGIN_SIZE;
        }
        else {
            y1 = (parent.getRow() + 1) * ProfilerTree.UNIT_SIZE - ProfilerTree.MARGIN_SIZE;
        }
        y2 = child.getRow() * ProfilerTree.UNIT_SIZE + ProfilerTree.MARGIN_SIZE;
        this.line = new Line(x, y1, x, y2);
        this.setEdgeDefaultStyle();
        this.group.getChildren().add(line);
    }

    public void setEdgeHighlight() {
        if (line != null) {
            line.setStroke(HIGHLIGHT_COLOR);
            line.setStrokeWidth(HIGHLIGHT_STROKE_WIDTH);
        }
    }

    public void setEdgeDefaultStyle() {
        if (line != null) {
            line.setStroke(DEFAULT_COLOR);
            line.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        }
    }
}
