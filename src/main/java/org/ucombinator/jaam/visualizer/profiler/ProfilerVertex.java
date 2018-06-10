package org.ucombinator.jaam.visualizer.profiler;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ProfilerVertex extends AbstractLayoutVertex<ProfilerVertex> {

    private double currentWeight;
    private double treeWeight;
    private ArrayList<ProfilerVertex> children; // Ordered List of children
    private ProfilerVertex parent;
    private ProfilerTree tree;
    private int row;
    // The left and right columns should still be integers, but the edge should lie in the middle of a column.
    private double leftColumn, edgeColumn, rightColumn;
    private ProfilerVertexValue leftValue, edgeValue, rightValue;

    public DataNode data;

    public ProfilerVertex(ProfilerVertex parent, ProfilerTree tree, int id, String label, double weight, DataNode dataNode) {
        super(id, label, VertexType.PROFILER);
        this.parent = parent;
        this.tree = tree;
        this.children = new ArrayList<>();
        this.currentWeight = weight;
        this.data = dataNode;
        this.color = Color.LIGHTBLUE;

        this.leftValue = new ProfilerVertexValue(this, ProfilerVertexValue.ValueType.LEFT_SIDE, id * 3);
        this.edgeValue = new ProfilerVertexValue(this, ProfilerVertexValue.ValueType.INCOMING_EDGE, id * 3 + 1);
        this.rightValue = new ProfilerVertexValue(this, ProfilerVertexValue.ValueType.RIGHT_SIDE, id * 3 + 2);
    }

    public ProfilerTree getTree() {
        return this.tree;
    }

    public double getCurrentWeight() {
        return this.currentWeight;
    }

    public int getEdgeLength() {
        return (int) ((this.currentWeight / this.tree.getWeightPerUnit()) + 1.5);
    }

    public int getRow() {
        return this.row;
    }

    public double getLeftColumn() {
        return this.leftColumn;
    }

    public double getEdgeColumn() {
        return this.edgeColumn;
    }

    public double getRightColumn() {
        return this.rightColumn;
    }

    public void setLeftColumn(double column) {
        this.leftColumn = column;
    }

    public void setIncomingEdgeColumn(double column) {
        this.edgeColumn = column;
    }

    public void setRightColumn(double column) {
        this.rightColumn = column;
    }

    public double getTreeWeight() {
        return this.treeWeight;
    }

    public ArrayList<ProfilerVertex> getChildren() {
        return this.children;
    }

    public ProfilerVertex getParent() {
        return this.parent;
    }

    public void addChild(ProfilerVertex child) {
        this.children.add(child);
    }

    public ProfilerVertexValue getLeftValue() {
        return this.leftValue;
    }

    public ProfilerVertexValue getEdgeValue() {
        return this.edgeValue;
    }

    public ProfilerVertexValue getRightValue() {
        return this.rightValue;
    }

    public void computeWeights() {
        this.treeWeight = currentWeight;
        for (ProfilerVertex child : children) {
            child.computeWeights();
            this.treeWeight += child.treeWeight;
        }
    }

    public void computeAllRows() {
        this.row = this.getEdgeLength();
        if (this.parent != null) {
            this.row += this.parent.row;
        }

        for (ProfilerVertex child : this.children) {
            child.computeAllRows();
        }
    }

    @Override
    public void onMouseClick(MouseEvent event) {

    }

    @Override
    public Set<ProfilerEdge> getIncidentEdges() {
        Set<ProfilerEdge> incidentEdges = new HashSet<>();
        incidentEdges.addAll(this.tree.getInEdges(this));
        incidentEdges.addAll(this.tree.getOutEdges(this));
        return incidentEdges;
    }

    public void buildSubtreeRows(ArrayList<ArrayList<ProfilerVertexValue>> valueRows) {
        // Add incoming edge. Note that we can't add the constraints for our incoming edge on the row for our vertex,
        // because that would interact with the constraints on the outgoing edges. So these self-adjacencies are not
        // considered here. Instead they are added when the HashMap is created in ProfilerTree.
        if (this.parent != null) {
            for (int edgeRow = this.parent.row; edgeRow < this.row; edgeRow++) {
                valueRows.get(edgeRow).add(this.edgeValue);
            }
        }
        else {
            for (int edgeRow = 0; edgeRow < this.row; edgeRow++) {
                valueRows.get(edgeRow).add(this.edgeValue);
            }
        }

        // In-order traversal: left side, children, right side
        valueRows.get(this.row).add(this.leftValue);
        for (ProfilerVertex child : this.children) {
            child.buildSubtreeRows(valueRows);
        }
        valueRows.get(this.row).add(this.rightValue);
    }

    public void computeGreedyLayout() {
        //TODO
    }
}
