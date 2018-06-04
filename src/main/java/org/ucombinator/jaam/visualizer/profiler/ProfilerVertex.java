package org.ucombinator.jaam.visualizer.profiler;

import javafx.scene.input.MouseEvent;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ProfilerVertex extends AbstractLayoutVertex<ProfilerVertex> {

    private double currentWeight;
    private double treeWeight;
    private ArrayList<ProfilerVertex> children;
    private ProfilerVertex parent;
    private ProfilerTree tree;

    public ProfilerVertex(ProfilerVertex parent, int id, String label, double weight) {
        super(id, label, VertexType.PROFILER);
        this.parent = parent;
        this.currentWeight = weight;
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

    // Parses XML snapshot from VisualVM
    public static ArrayList<ProfilerVertex> parseXML(String filename) {
        ArrayList<ProfilerVertex> vertices = new ArrayList<>();


        return vertices;
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
