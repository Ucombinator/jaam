package org.ucombinator.jaam.visualizer.profiler;

import org.ucombinator.jaam.visualizer.graph.Graph;

import java.util.*;

public class ProfilerTree extends Graph<ProfilerVertex, ProfilerEdge> {

    private static final int TOTAL_UNITS = 1000;
    public static final int UNIT_SIZE = 20;
    public static final int MARGIN_SIZE = 5;

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

        // The ordering of values across each row.
        ArrayList<ArrayList<ProfilerVertexValue>> profilerValueRows = this.getProfilerValueRows();
        // A map that shows, for a given value, what constraints include it on the left side.
        HashMap<ProfilerVertexValue, Set<Constraint>> leftConstraints = this.getAllConstraints(profilerValueRows,true);
        // A map that shows, for a given value, what constraints include it on the right side.
        HashMap<ProfilerVertexValue, Set<Constraint>> rightConstraints = this.getAllConstraints(profilerValueRows, false);

        // A count of the dependencies that have already been assigned for a given value.
        HashMap<ProfilerVertexValue, Integer> dependenciesMet = new HashMap<>();
        for (ProfilerVertex v : this.vertices) {
            dependenciesMet.put(v.getLeftValue(), 0);
            dependenciesMet.put(v.getEdgeValue(), 0);
            dependenciesMet.put(v.getRightValue(), 0);
        }

        LinkedList<Constraint> constraintQueue = new LinkedList<>();
        for (ProfilerVertex v : this.vertices) {
            ProfilerVertexValue leftValue = v.getLeftValue();
            if (rightConstraints.get(leftValue).size() == 0) {
                v.setLeftColumn(0);
                constraintQueue.add(new Constraint(null, leftValue));
            }
        }

        // Find the leftmost allowed location for every possible vertex / edge.
        double maxColumn = 0;
        while (constraintQueue.size() > 0) {
            // When we pop a constraint, its right value has just been solved. So we find every constraint for which
            // the newly solved value was on the left side. If that is the final constraint for any value,
            // then we compute its location and add all of its right constraints to our constraint queue.
            Constraint constraint = constraintQueue.pop();
            for (Constraint nextConstraint : leftConstraints.get(constraint.getRightValue())) {
                ProfilerVertexValue nextValue = nextConstraint.getRightValue();
                dependenciesMet.put(nextValue, dependenciesMet.get(nextValue) + 1);
                if (dependenciesMet.get(nextValue) == rightConstraints.get(nextValue).size()) {
                    maxColumn = Math.max(maxColumn, Constraint.applyConstraintsRight(rightConstraints.get(nextValue)));
                    constraintQueue.add(nextConstraint);
                }
            }
        }

        // Keep the right side fixed, and move in reverse order through our DAG to shrink vertices that are wider
        // than they need to be.
        for (ProfilerVertex v : this.vertices) {
            dependenciesMet.put(v.getLeftValue(), 0);
            dependenciesMet.put(v.getEdgeValue(), 0);
            dependenciesMet.put(v.getRightValue(), 0);
        }

        constraintQueue = new LinkedList<>();
        for (ProfilerVertex v : this.vertices) {
            ProfilerVertexValue rightV = v.getRightValue();
            if (leftConstraints.get(rightV).size() == 0) {
                constraintQueue.add(new Constraint(rightV, null));
            }
        }

        while (constraintQueue.size() > 0) {
            // When we pop a constraint, its left value has just been solved.
            Constraint constraint = constraintQueue.pop();
            for (Constraint nextConstraint : rightConstraints.get(constraint.getLeftValue())) {
                ProfilerVertexValue nextValue = nextConstraint.getLeftValue();
                dependenciesMet.put(nextValue, dependenciesMet.get(nextValue) + 1);
                if (dependenciesMet.get(nextValue) == leftConstraints.get(nextValue).size()) {
                    // Leave the right side of each vertex, but move the edges and the left sides closer.
                    if (nextValue.getValueType() != ProfilerVertexValue.ValueType.RIGHT_SIDE) {
                        Constraint.applyConstraintsLeft(leftConstraints.get(nextValue));
                    }
                    constraintQueue.add(nextConstraint);
                }
            }
        }

        return maxColumn;
    }

    private ArrayList<ArrayList<ProfilerVertexValue>> getProfilerValueRows() {
        int totalRows = this.getMaxRow() + 1;
        ArrayList<ArrayList<ProfilerVertexValue>> profilerValueRows = new ArrayList<>();
        for(int i = 0; i < totalRows; i++) {
            profilerValueRows.add(new ArrayList<>());
        }

        for (ProfilerVertex v : this.roots) {
            v.addSubtreeConstraints(profilerValueRows);
        }

        return profilerValueRows;
    }

    private HashMap<ProfilerVertexValue, Set<Constraint>> getAllConstraints(
            ArrayList<ArrayList<ProfilerVertexValue>> constraintRows, boolean leftToRight) {
        HashMap<ProfilerVertexValue, Set<Constraint>> constraints = new HashMap<>();
        for (ProfilerVertex v : this.vertices) {
            constraints.put(v.getLeftValue(), new HashSet<>());
            constraints.put(v.getEdgeValue(), new HashSet<>());
            constraints.put(v.getRightValue(), new HashSet<>());

            Constraint leftToEdgeConstraint = new Constraint(v.getLeftValue(), v.getEdgeValue());
            Constraint edgeToRightConstraint = new Constraint(v.getEdgeValue(), v.getRightValue());
            if (leftToRight) {
                constraints.get(v.getLeftValue()).add(leftToEdgeConstraint);
                constraints.get(v.getEdgeValue()).add(edgeToRightConstraint);
            }
            else {
                constraints.get(v.getRightValue()).add(edgeToRightConstraint);
                constraints.get(v.getEdgeValue()).add(leftToEdgeConstraint);
            }
        }

        // Note that the adjacency between two edges can be found on multiple rows. But we use a
        // set so that it doesn't get repeated.
        for (ArrayList<ProfilerVertexValue> constraintRow : constraintRows) {
            if (leftToRight) {
                for (int i = 0; i < constraintRow.size() - 1; i++) {
                    ProfilerVertexValue leftValue = constraintRow.get(i);
                    ProfilerVertexValue rightValue = constraintRow.get(i + 1);
                    Constraint constraint = new Constraint(leftValue, rightValue);
                    constraints.get(leftValue).add(constraint);
                }
            }
            else {
                for (int i = constraintRow.size() - 1; i > 0; i--) {
                    ProfilerVertexValue leftValue = constraintRow.get(i - 1);
                    ProfilerVertexValue rightValue = constraintRow.get(i);
                    Constraint constraint = new Constraint(leftValue, rightValue);
                    constraints.get(rightValue).add(constraint);
                }
            }
        }

        return constraints;
    }

    private int getMaxRow() {
        int maxRow = 0;
        for (ProfilerVertex v : this.vertices) {
            maxRow = Math.max(maxRow, v.getRow());
        }
        return maxRow;
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
