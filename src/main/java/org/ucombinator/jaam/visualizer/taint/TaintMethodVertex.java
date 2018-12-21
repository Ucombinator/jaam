package org.ucombinator.jaam.visualizer.taint;

import javafx.animation.ParallelTransition;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.classTree.PackageNode;
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

    public final boolean isCodeAvailable;

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
            this.color = Color.LIGHTGRAY;
            this.setExpanded(false);
            this.isCodeAvailable = false;
        }
        else {
            isCodeAvailable = true;
        }

        this.inputs = inputs;
        this.inner = inner;
        this.outputs = outputs;

        inputs.forEach(this::addVertex);
        inner.forEach(this::addVertex);
        outputs.forEach(this::addVertex);

        this.setExpanded(false);
    }

    private void addVertex(TaintVertex v) {
       this.getInnerGraph().addVertex(v);
       v.setOuterGraph(this.getInnerGraph());
    }
    public TaintMethodVertex copy() {
        return new TaintMethodVertex(className, methodName, getPreferredLayout(), inputs, inner, outputs);
    }

    @Override
    public GUINode getGraphics() {
        if (isCodeAvailable)
            return methodGraphic;
        else
            return super.getGraphics();
    }
    @Override
    public GUINode setGraphics(GUINode parent) {
        if (isCodeAvailable) {
            this.methodGraphic = new MethodGuiNode(parent, this);

            this.setHeight(this.methodGraphic.getHeight());
            this.setWidth(this.methodGraphic.getWidth());

            return this.methodGraphic;
        }
        else {
            return super.setGraphics(parent);
        }
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
        StringBuilder builder = new StringBuilder(className + "\n" + methodName + "\n");

        builder.append("Parameters:\n");
        if (inputs.isEmpty()) {
            builder.append("\tNone\n");
        }
        else {
            for (TaintVertex v : inputs) {
                builder.append("\t" + v.toString() + "\n");
            }
        }
        builder.append("Returns:\n");
        if (outputs.isEmpty()) {
            builder.append("\tNone\n");
        }
        else {
            for (TaintVertex v : outputs) {
                builder.append("\t" + v.toString() + "\n");
            }
        }
        builder.append("Inner:\n");
        if (inner.isEmpty()) {
            builder.append("\tNone\n");
        }
        else {
            for (TaintVertex v : inner) {
                builder.append("\t" + v.toString() + "\n");
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return className + "." + methodName;
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
