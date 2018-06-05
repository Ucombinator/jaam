package org.ucombinator.jaam.visualizer.profiler;

import org.ucombinator.jaam.visualizer.graph.Graph;

import java.util.ArrayList;

public class ProfilerTree extends Graph<ProfilerVertex, ProfilerEdge> {

    ArrayList<ProfilerVertex> roots;
    int size = 0;

    ProfilerTree(DataTree dataTree) {
        super();

        roots = new ArrayList<>();

        for (DataNode d : dataTree.getRoots()) {

            ProfilerVertex newRoot = createVertex(null, d);
            roots.add(newRoot);

            addSubTree(newRoot, d);
        }
    }

    private double calcWeight(DataNode d) {
        return d.getMyTime();
    }

    private void addSubTree(ProfilerVertex parent, DataNode subTreeRoot) {
        for (DataNode d : subTreeRoot.getChildren()) {
            ProfilerVertex newChild = createVertex(parent, d);

            parent.addChild(newChild);
            addSubTree(newChild, d);
        }
    }

    private ProfilerVertex createVertex(ProfilerVertex parent, DataNode d) {
        ProfilerVertex newVertex = new ProfilerVertex(parent, this, size++, d.getName(), calcWeight(d), d);

        this.vertices.add(newVertex);

        if (parent != null) {
            this.addEdge(new ProfilerEdge(parent, newVertex));
        }

        return newVertex;
    }

}
