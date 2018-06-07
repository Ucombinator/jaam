package org.ucombinator.jaam.visualizer.profiler;

import javafx.scene.input.MouseEvent;
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

    public DataNode data;

    public ProfilerVertex(ProfilerVertex parent, ProfilerTree tree, int id, String label, double weight, DataNode dataNode) {
        super(id, label, VertexType.PROFILER);
        this.parent = parent;
        this.tree = tree;
        this.children = new ArrayList<>();
        this.currentWeight = weight;
        this.data = dataNode;
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

    public void computeWeights() {
        this.treeWeight = currentWeight;
        for (ProfilerVertex child : children) {
            child.computeWeights();
            this.treeWeight += child.treeWeight;
        }
    }

    public void computeAllRows() {
        if (this.parent == null) {
            this.row = 0;
        }
        else {
            this.row = this.parent.row + this.parent.getEdgeLength();
        }
    }

    public void shiftSubtree(double shiftDist) {
        this.shiftRight(shiftDist);
        for (ProfilerVertex child : this.children) {
            child.shiftSubtree(shiftDist);
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

    public void addSubtreeConstraints(ArrayList<ArrayList<ProfilerVertexValue>> constraintRows) {
        ProfilerVertexValue leftConstraint = new ProfilerVertexValue(this, ProfilerVertexValue.ValueType.LEFT_SIDE);
        ProfilerVertexValue edgeConstraint = new ProfilerVertexValue(this, ProfilerVertexValue.ValueType.INCOMING_EDGE);
        ProfilerVertexValue rightConstraint = new ProfilerVertexValue(this, ProfilerVertexValue.ValueType.RIGHT_SIDE);

        // Add incoming edge. Note that we can't add the constraints for our incoming edge on the row for our vertex,
        // because that would interact with the constraints on the outgoing edges. So these self-adjacencies are not
        // considered here. Instead they are added when the HashMap is created in ProfilerTree.
        for (int edgeRow = this.parent.row; edgeRow < this.row; edgeRow++) {
            constraintRows.get(edgeRow).add(edgeConstraint);
        }

        // In-order traversal: left side, children, right side
        constraintRows.get(this.row).add(leftConstraint);
        for (ProfilerVertex child : this.children) {
            child.addSubtreeConstraints(constraintRows);
        }
        constraintRows.get(this.row).add(rightConstraint);
    }

    public void computeGreedyLayout() {
        //TODO
    }
}
