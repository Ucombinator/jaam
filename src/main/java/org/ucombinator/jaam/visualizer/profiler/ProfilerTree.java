package org.ucombinator.jaam.visualizer.profiler;

import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import org.ucombinator.jaam.visualizer.graph.Graph;

import java.util.*;
import java.util.stream.Collectors;

public class ProfilerTree extends Graph<ProfilerVertex, ProfilerEdge> {

    private static final int TOTAL_UNITS = 1000;
    public static final int UNIT_SIZE = 20;
    public static final int MARGIN_SIZE = 4;

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

    public double computeCurrentLayoutLP() {
        // Call our DAG layout to get an starting point
        double totalWidth = this.computeCurrentLayout();

        // Construct list of values for our tree.
        ArrayList<ProfilerVertexValue> vertexValues = new ArrayList<>();
        for (ProfilerVertex v : this.vertices) {
            vertexValues.add(v.getLeftValue());
            vertexValues.add(v.getEdgeValue());
            vertexValues.add(v.getRightValue());
        }

        ProfilerVertex dummyVertex = new ProfilerVertex(null, null, -1, null, -1, null);
        ProfilerVertexValue rightBoundary = new ProfilerVertexValue(
                dummyVertex, ProfilerVertexValue.ValueType.LEFT_SIDE, this.vertices.size() * 3);
        vertexValues.add(rightBoundary);
        rightBoundary.assignSolution(totalWidth);

        vertexValues.sort(new Comparator<ProfilerVertexValue>() {
            @Override
            public int compare(ProfilerVertexValue o1, ProfilerVertexValue o2) {
                return Double.compare(o1.getColumn(), o2.getColumn());
            }
        });
        HashMap<ProfilerVertexValue, Integer> valuePositions = new HashMap<>();
        for (int i = 0; i < vertexValues.size(); i++) {
            valuePositions.put(vertexValues.get(i), i);
        }

        for (ProfilerVertex v : this.roots) {
            v.computeAllRows();
        }

        // The ordering of values across each row.
        ArrayList<ArrayList<ProfilerVertexValue>> profilerValueRows = this.getProfilerValueRows();
        // A map that shows, for a given value, what constraints include it on the left side.
        Set<Constraint> allConstraints = this.getAllConstraints(profilerValueRows);

        for (ArrayList<ProfilerVertexValue> valueRow : profilerValueRows) {
            if (valueRow.size() > 0) {
                ProfilerVertexValue rightmost = valueRow.get(valueRow.size() - 1);
                Constraint rightmostConstraint = new Constraint(rightmost, rightBoundary);
                allConstraints.add(rightmostConstraint);
            }
        }

        try {
            // Each constraint gives one equation.
            int numEqs = allConstraints.size();
            // Each vertex has three values, and we also need the maxWidth value.
            int numVars = this.vertices.size() * 3 + 1;

            double[][] matrix = new double[numEqs][numVars];
            double[] constants = new double[numEqs];
            double[] lb = new double[numVars];
            for(int i = 0; i < numEqs; i++) {
                constants[i] = 0;
                for(int j = 0; j < numVars; j++) {
                    matrix[i][j] = 0;
                }
            }
            for (int i = 0; i < numVars; i++) {
                lb[i] = 0;
            }

            int rowIndex = 0;
            for (Constraint constraint : allConstraints) {
                int leftIndex = valuePositions.get(constraint.getLeftValue());
                int rightIndex = valuePositions.get(constraint.getRightValue());
                matrix[rowIndex][leftIndex] = 1;
                matrix[rowIndex][rightIndex] = -1;
                constants[rowIndex] = -1 * constraint.getDistance();
                /*System.out.println("New constraint: x" + rightIndex + " - x" + leftIndex + " > " + constraint.getDistance());
                    System.out.println("Actual distance: " + (constraint.getRightValue().getColumn() - constraint.getLeftValue().getColumn()));*/
                rowIndex++;
            }

            double[] objective = new double[numVars];
            for(int i = 0; i < numVars; i++) {
                objective[i] = 0;
            }
            for (ProfilerVertex v : this.vertices) {
                // Add (right - left) for each vertex, so that we minimize each width.
                objective[valuePositions.get(v.getLeftValue())] = -1;
                objective[valuePositions.get(v.getRightValue())] = 1;
            }
            objective[valuePositions.get(rightBoundary)] = 1; // Minimize total width.

            double tolerance = 0.000000001;
            double[] initial = new double[numVars];
            for (int i = 0; i < numVars; i++) {
                // Increase spacing so our previous solution is strictly feasible.
                initial[i] = vertexValues.get(i).getColumn() + ((i + 1) * tolerance);
            }

            // Solve!
            System.out.println("Solving LP with " + numVars + " variables and " + numEqs + " equations...");
            // BasicConfigurator.configure();
            LPOptimizationRequest or = new LPOptimizationRequest();
            or.setC(objective);
            or.setG(matrix);
            or.setH(constants);
            or.setLb(lb);
            // or.setInitialPoint(initial);
            LPPrimalDualMethod opt = new LPPrimalDualMethod();
            opt.setOptimizationRequest(or);
            opt.optimize();

            // Assign solutions to our values, but ignore the extra maxWidth value.
            double[] results = opt.getOptimizationResponse().getSolution();
            for (int i = 0; i < numVars; i++) {
                System.out.println("x" + i + " moved from " + initial[i] + " to " + results[i]);
                vertexValues.get(i).assignSolution(results[i]);
            }

            return results[3 * this.vertices.size()];
        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Computes the location for each ProfilerVertex, assuming that the order of the list of children
    // is their left-to-right order in our drawing.
    public double computeCurrentLayout() {
        // First, we compute the DAG of relationships between left and right sides and incoming edges of our vertices.
        // Since we have the ordering of the children, we know what the ordering in each row is.
        // We also require that every edge is a vertical segment, so each parent overlaps horizontally with all of its children.
        for (ProfilerVertex v : this.roots) {
            // System.out.println("Computing rows...");
            v.computeAllRows();
        }

        // The ordering of values across each row.
        ArrayList<ArrayList<ProfilerVertexValue>> profilerValueRows = this.getProfilerValueRows();
        // A set of all constraints for our tree.
        Set<Constraint> allConstraints = this.getAllConstraints(profilerValueRows);
        // A map that shows, for a given value, what constraints include it on the left side.
        Map<ProfilerVertexValue, List<Constraint>> leftConstraints = allConstraints.stream()
                .collect(Collectors.groupingBy(Constraint::getLeftValue));
        // A map that shows, for a given value, what constraints include it on the right side.
        Map<ProfilerVertexValue, List<Constraint>> rightConstraints = allConstraints.stream()
                .collect(Collectors.groupingBy(Constraint::getRightValue));

        // The right side of a vertex might not be on the left side of any constraints, and vice versa.
        // So we add empty sets to our maps.
        for (ProfilerVertex v : this.vertices) {
            ProfilerVertexValue leftValue = v.getLeftValue();
            ProfilerVertexValue rightValue = v.getRightValue();
            if (!leftConstraints.containsKey(rightValue)) {
                leftConstraints.put(rightValue, new ArrayList<>());
            }
            if (!rightConstraints.containsKey(leftValue)) {
                rightConstraints.put(leftValue, new ArrayList<>());
            }
        }

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
            v.buildSubtreeRows(profilerValueRows);
        }

        return profilerValueRows;
    }

    private Set<Constraint> getAllConstraints(ArrayList<ArrayList<ProfilerVertexValue>> constraintRows) {
        Set<Constraint> allConstraints = new HashSet<>();
        for (ProfilerVertex v : this.vertices) {
            Constraint leftToEdgeConstraint = new Constraint(v.getLeftValue(), v.getEdgeValue());
            Constraint edgeToRightConstraint = new Constraint(v.getEdgeValue(), v.getRightValue());
            allConstraints.add(leftToEdgeConstraint);
            allConstraints.add(edgeToRightConstraint);
        }

        // Note that the adjacency between two edges can be found on multiple rows. But we use a
        // set so that it doesn't get repeated.
        for (ArrayList<ProfilerVertexValue> constraintRow : constraintRows) {
            for (int i = 0; i < constraintRow.size() - 1; i++) {
                ProfilerVertexValue leftValue = constraintRow.get(i);
                ProfilerVertexValue rightValue = constraintRow.get(i + 1);
                Constraint constraint = new Constraint(leftValue, rightValue);
                allConstraints.add(constraint);
            }
        }

        return allConstraints;
    }

    private int getMaxRow() {
        int maxRow = 0;
        for (ProfilerVertex v : this.vertices) {
            maxRow = Math.max(maxRow, v.getRow());
        }
        return maxRow;
    }

    // Since computing the optimal ordering is NP-hard, we use a recursive greedy approximation.
    // To drawEdge a tree, we first drawEdge each subtree. Then we repeatedly take the subtrees that overlap the most,
    // and put them together, until we have an order of all of the subtrees.
    private void computeGreedyLayout() {
        for (ProfilerVertex root : this.roots) {
            root.computeGreedyLayout();
        }
        //TODO
    }

    private void JOptimizerTest() {
        //Objective function
        double[] c = new double[] { -1., -1. };

        //Inequalities constraints
        double[][] G = new double[][] {{4./3., -1}, {-1./2., 1.}, {-2., -1.}, {1./3., 1.}};
        double[] h = new double[] {2., 1./2., 2., 1./2.};

        //Bounds on variables
        double[] lb = new double[] {0 , 0};
        double[] ub = new double[] {10, 10};

        //optimization problem
        LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(c);
        or.setG(G);
        or.setH(h);
        or.setLb(lb);
        or.setUb(ub);
        or.setDumpProblem(true);

        //optimization
        LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        try {
            opt.optimize();
            double[] sol = opt.getOptimizationResponse().getSolution();
            System.out.println(sol[0] + ", " + sol[1]);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
