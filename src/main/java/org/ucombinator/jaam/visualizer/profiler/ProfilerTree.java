package org.ucombinator.jaam.visualizer.profiler;

import javafx.util.Pair;
import org.ucombinator.jaam.visualizer.graph.Graph;

import java.util.*;

public class ProfilerTree extends Graph<ProfilerVertex, ProfilerEdge> {

    private static final int TOTAL_UNITS = 1000;
    public static final int UNIT_SIZE = 10;
    public static final int MARGIN_SIZE = 2;

    private double weightPerUnit;
    private ArrayList<ProfilerVertex> roots;

    public ProfilerTree(DataTree dataTree) {
        super();

        roots = new ArrayList<>();

        for (DataNode d : dataTree.getRoots()) {

            ProfilerVertex newRoot = createVertex(null, d);
            roots.add(newRoot);

            addSubTree(newRoot, d);
        }

        this.computeWeightPerUnit();
    }

    private double calcWeight(DataNode d) {
        return d.getMyTime();
    }

    private void computeWeightPerUnit() {
        double totalWeight = 0;
        for (ProfilerVertex v : this.vertices) {
            totalWeight += v.getCurrentWeight();
        }
        this.weightPerUnit = totalWeight / TOTAL_UNITS;
    }

    public double getWeightPerUnit() {
        return this.weightPerUnit;
    }

    private void addSubTree(ProfilerVertex parent, DataNode subTreeRoot) {
        for (DataNode d : subTreeRoot.getChildren()) {
            ProfilerVertex newChild = createVertex(parent, d);

            parent.addChild(newChild);
            addSubTree(newChild, d);
        }
    }

    private ProfilerVertex createVertex(ProfilerVertex parent, DataNode d) {
        ProfilerVertex newVertex = new ProfilerVertex(parent, this, this.vertices.size(), d.getName(), calcWeight(d), d);
        this.vertices.add(newVertex);

        if (parent != null) {
            this.addEdge(new ProfilerEdge(parent, newVertex));
        }

        return newVertex;
    }

    // Computes the location for each ProfilerVertex, assuming that the order of the list of children
    // is their left-to-right order in our drawing.
    public double computeCurrentLayout() {
        // First, we compute the DAG of relationships between left and right sides and incoming edges of our vertices.
        // Since we have the ordering of the children, we know what the ordering in each row is.
        // We also require that each parent overlaps with all of its children.
        for (ProfilerVertex v : this.roots) {
            // System.out.println("Computing rows...");
            v.computeAllRows();
        }

        // A map that shows, for a given value, what values it constrains on its right.
        HashMap<ProfilerVertexValue, Set<Constraint>> rightConstraints = this.getAllConstraints();

        // A map of the dependencies for a given value, i.e. ,the constraints in which the given value is on the right side.
        HashMap<ProfilerVertexValue, Pair<ArrayList<Constraint>, Integer>> dependencyMap = this.getDependencyMap(rightConstraints);

        LinkedList<Constraint> constraintQueue = new LinkedList<>();
        for (ProfilerVertex v : this.vertices) {
            ProfilerVertexValue leftV = v.getLeftValue();
            if (dependencyMap.get(leftV).getValue() == 0) {
                leftV.assignSolution(0);
                constraintQueue.add(new Constraint(null, leftV));
            }
        }

        double maxColumn = 0;
        while (constraintQueue.size() > 0) {
            // When we pop an constraint, its right constraint has just been solved. So we find every constraint for which
            // the newly solved constraint was on the left side. If that is the final constraint for any value,
            // then we compute its location and add all of its right adjacencies to our constraint queue.
            Constraint constraint = constraintQueue.pop();
            for (Constraint nextConstraint : rightConstraints.get(constraint.getRightValue())) {
                ProfilerVertexValue nextValue = nextConstraint.getRightValue();
                Pair<ArrayList<Constraint>, Integer> blockingCheck = dependencyMap.get(nextValue);

                blockingCheck.getKey().add(nextConstraint);
                if (blockingCheck.getKey().size() == blockingCheck.getValue()) {
                    maxColumn = Math.max(maxColumn, Constraint.applyConstraints(blockingCheck.getKey()));
                    constraintQueue.add(nextConstraint);
                }
            }
        }

        return maxColumn;
    }

    private HashMap<ProfilerVertexValue, Set<Constraint>> getAllConstraints() {
        HashMap<ProfilerVertexValue, Set<Constraint>> rightConstraints = new HashMap<>();
        for (ProfilerVertex v : this.vertices) {
            rightConstraints.put(v.getLeftValue(), new HashSet<>());
            rightConstraints.put(v.getEdgeValue(), new HashSet<>());
            rightConstraints.put(v.getRightValue(), new HashSet<>());

            Constraint leftToEdgeConstraint = new Constraint(v.getLeftValue(), v.getEdgeValue());
            Constraint edgeToRightConstraint = new Constraint(v.getEdgeValue(), v.getRightValue());
            rightConstraints.get(v.getLeftValue()).add(leftToEdgeConstraint);
            rightConstraints.get(v.getEdgeValue()).add(edgeToRightConstraint);
        }

        int totalRows = this.getMaxRow() + 1;
        ArrayList<ArrayList<ProfilerVertexValue>> constraintRows = new ArrayList<>();
        for(int i = 0; i < totalRows; i++) {
            constraintRows.add(new ArrayList<>());
        }

        for (ProfilerVertex v : this.roots) {
            v.addSubtreeConstraints(constraintRows);
        }

        // Note that the adjacency between two edges can be found on multiple rows. But we use a
        // set so that it doesn't get repeated.
        for (ArrayList<ProfilerVertexValue> constraintRow : constraintRows) {
            for (int i = 0; i < constraintRow.size() - 1; i++) {
                ProfilerVertexValue leftValue = constraintRow.get(i);
                ProfilerVertexValue rightValue = constraintRow.get(i + 1);
                Constraint constraint = new Constraint(leftValue, rightValue);
                rightConstraints.get(leftValue).add(constraint);
            }
        }

        return rightConstraints;
    }

    private int getMaxRow() {
        int maxRow = 0;
        for (ProfilerVertex v : this.vertices) {
            maxRow = Math.max(maxRow, v.getRow());
        }
        return maxRow;
    }

    // We return a map with an empty list of known dependencies, and a count of the total number to be discovered.
    private HashMap<ProfilerVertexValue, Pair<ArrayList<Constraint>, Integer>> getDependencyMap(
            HashMap<ProfilerVertexValue, Set<Constraint>> rightAdjacencies) {
        HashMap<ProfilerVertexValue, Integer> blockingCounts = new HashMap<>();
        for (ProfilerVertexValue value : rightAdjacencies.keySet()) {
            blockingCounts.put(value, 0);
        }

        for (ProfilerVertexValue leftValue : rightAdjacencies.keySet()) {
            for (Constraint constraint : rightAdjacencies.get(leftValue)) {
                ProfilerVertexValue rightValue = constraint.getRightValue();
                blockingCounts.put(rightValue, blockingCounts.get(rightValue) + 1);
            }
        }

        HashMap<ProfilerVertexValue, Pair<ArrayList<Constraint>, Integer>> dependencyMap = new HashMap<>();
        for (ProfilerVertexValue value : rightAdjacencies.keySet()) {
            dependencyMap.put(value, new Pair<>(new ArrayList<>(), blockingCounts.get(value)));
        }
        return dependencyMap;
    }

    // Since computing the optimal ordering is NP-hard, we use a recursive greedy approximation.
    // To draw a tree, we first draw each subtree. Then we repeatedly take the subtrees that overlap the most,
    // and put them together, until we have an order of all of the subtrees.
    private void computeGreedyLayout() {
        for (ProfilerVertex root : this.roots) {
            root.computeGreedyLayout();
        }
        //TODO
    }
}
