package org.ucombinator.jaam.visualizer.taint;

import javafx.animation.ParallelTransition;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.MethodGuiNode;
import org.ucombinator.jaam.visualizer.gui.TransitionFactory;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.*;
import java.util.function.Consumer;

public class TaintMethodVertex extends TaintVertex {

    public static final double ELEM_WIDTH = 10.0;
    public static final double ELEM_HEIGHT = 10.0;
    public static final double LABEL_HEIGHT = 10.0;

    private Color defaultColor = Color.DEEPSKYBLUE;
    private LayoutAlgorithm.LAYOUT_ALGORITHM innerLayout = LayoutAlgorithm.LAYOUT_ALGORITHM.SUMMARY;

    private String className;
    private String methodName;

    private ArrayList<TaintVertex> inputs, inner, outputs;
    private MethodGuiNode methodGraphic;

    public TaintMethodVertex(String className, String methodName, LayoutAlgorithm.LAYOUT_ALGORITHM preferredLayout,
                             ArrayList<TaintVertex> inputs, ArrayList<TaintVertex> inner, ArrayList<TaintVertex> outputs) {
        super(className + "." + methodName, VertexType.SCC, true);

        innerLayout = preferredLayout;

        this.className = className;
        this.methodName = methodName;

        if (!Main.getSelectedMainTabController().codeViewController.haveCode(className)) {
            this.color = Color.DARKGRAY;
            this.setExpanded(false);
        }

        System.out.println("Creating a taint method vertex Sizes (in, inner, out)" + inputs.size() + " " + inner.size() + " " + outputs.size());

        this.inputs = inputs;
        this.inner = inner;
        this.outputs = outputs;

        inputs.forEach(this::addVertex);
        inner.forEach(this::addVertex);
        outputs.forEach(this::addVertex);
    }

    private void addVertex(TaintVertex v) {
       this.getInnerGraph().addVertex(v);
       v.setOuterGraph(this.getInnerGraph());
    }
    public TaintMethodVertex copy() {
        return new TaintMethodVertex(className, methodName, getPreferredLayout(), inputs, inner, outputs);
    }

    @Override
    public MethodGuiNode getGraphics() {
        return methodGraphic;
    }
    @Override
    public MethodGuiNode setGraphics(GUINode parent) {
        System.out.println("Calling taintMethod setGraphics");
        this.methodGraphic = new MethodGuiNode(parent, this);
        return this.methodGraphic;
    }

    public HashSet<String> getMethodNames() {
        HashSet<String> methodNames = new HashSet<>();
        for (TaintVertex v : this.getInnerGraph().getVertices()) {
            methodNames.addAll(v.getMethodNames());
        }
        return methodNames;
    }

    @Override
    public boolean hasField() {
        for (TaintVertex v : this.getInnerGraph().getVertices()) {
            if (v.hasField()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void getFields(Collection<TaintAddress> store) {
        this.getInnerGraph().getVertices().forEach(v -> v.getFields(store));
    }


    @Override
    public String getLongText() {
        if(getInnerGraph().isEmpty()) return "Empty method vertex\n";

        Set<TaintVertex> vertices = this.getInnerGraph().getVertices();

        StringBuilder builder = new StringBuilder(className + "\n" + methodName + "\n");



        for(TaintVertex v : vertices) {
            if (v.getStmtString() == null) continue;

            builder.append("\t" + v.getStmtString() + "\n");
        }

        return builder.toString();
    }

    @Override
    public String getStmtString() {
        return null;
    }

    @Override
    public LayoutAlgorithm.LAYOUT_ALGORITHM getPreferredLayout() {
        return LayoutAlgorithm.LAYOUT_ALGORITHM.SUMMARY;
    }

    @Override
    public List<TaintVertex> expand() {
        List<TaintVertex> expandedVertices = new ArrayList<>();
        for (TaintVertex v : this.getInnerGraph().getVertices()) {
            expandedVertices.addAll(v.expand());
        }
        return expandedVertices;
    }

    public void handleDoubleClick() {
        TaintRootVertex root = Main.getSelectedTaintPanelController().getVisibleRoot();
        Graph<TaintVertex, TaintEdge> innerGraph = this.getInnerGraph();
        boolean isExpanded = this.isExpanded();

        double newOpacity = isExpanded ? 0.0 : 1.0;
        boolean newVisible = !isExpanded;

        // First we want the content of the clicked node to appear/disappear.
        System.out.println("Changing opacity of child graph...");

        for(TaintVertex v: innerGraph.getVertices()) {
            v.setOpacity(newOpacity);
        }

        for(TaintEdge e: innerGraph.getEdges()){
            e.setOpacity(newOpacity);
        }

        ParallelTransition pt = TransitionFactory.buildRecursiveTransition(root);
        pt.setOnFinished(
                event1 -> {
                    // Then we want the vertices to move to their final positions and the clicked vertex
                    // to change its size.
                    this.setExpanded(!isExpanded);

                    for (TaintVertex v: innerGraph.getVertices()) {
                        v.setVisible(newVisible);
                    }

                    for (TaintEdge e: innerGraph.getEdges()) {
                        e.redrawAndSetVisible(newVisible);
                    }

                    LayoutAlgorithm.layout(root);
                    ParallelTransition pt1 = TransitionFactory.buildRecursiveTransition(root);

                    // Lastly we redraw the edges that may have been moved.
                    // We don't need to do anything to the vertices, so we pass an empty function.
                    // TODO: Eliminate the need for this by adding a new apply function that only takes the second function.
                    pt1.setOnFinished(event2 -> root.applyToEdgesRecursive(
                            new Consumer<HierarchicalVertex<TaintVertex, TaintEdge>>() {
                                @Override
                                public void accept(HierarchicalVertex<TaintVertex, TaintEdge> stateVertexStateEdgeHierarchicalVertex) {}
                            },
                            (TaintEdge e) -> e.redrawEdge()));
                    pt1.play();
                }
        );

        pt.play();

    }

    public ArrayList<TaintVertex> getInputs() {
        return inputs;
    }

    public ArrayList<TaintVertex> getOutputs() {
        return outputs;
    }
}
