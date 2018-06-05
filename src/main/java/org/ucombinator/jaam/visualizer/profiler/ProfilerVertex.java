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

    public DataNode data;

    public ProfilerVertex(ProfilerVertex parent, ProfilerTree tree, int id, String label, double weight, DataNode dataNode) {
        super(id, label, VertexType.PROFILER);
        this.parent = parent;
        this.tree = tree;
        this.currentWeight = weight;
        this.data = dataNode;
    }

    public double getCurrentWeight() {
        return this.currentWeight;
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
}
