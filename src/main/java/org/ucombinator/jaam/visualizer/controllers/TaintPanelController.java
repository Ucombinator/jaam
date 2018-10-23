package org.ucombinator.jaam.visualizer.controllers;

import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;
import org.ucombinator.jaam.tools.taint3.Address;
import org.ucombinator.jaam.util.Loop;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.state.StateLoopVertex;
import org.ucombinator.jaam.visualizer.state.StateMethodVertex;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.taint.*;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.internal.JimpleLocal;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TaintPanelController extends GraphPanelController<TaintVertex, TaintEdge>
        implements EventHandler<SelectEvent<TaintVertex>> {

    private GraphTransform<TaintRootVertex, TaintVertex> immAndVis;
    private HashMap<String, TaintAddress> fieldVertices;

    private boolean collapseAll = false, expandAll = false;

    private CodeViewController codeController;

    HashSet<TaintVertex> visibleAncestors;
    HashSet<TaintVertex> visibleDescendants;

    // Graph is the statement graph
    public TaintPanelController(Graph<TaintVertex, TaintEdge> graph, CodeViewController codeController, MainTabController tabController) throws IOException {
        super(TaintRootVertex::new, tabController);

        this.codeController = codeController;

        for (TaintVertex v : graph.getVertices()) {
            if (v.getClassName() == null && v.getMethodName() == null) {
                System.out.println("Don't have class or method for " + v.toString());
            }
            else if (v.getClassName() == null) {
                System.out.println("Don't have class for " + v.toString());
            }
        }

        // Custom event handlers
        graphContentGroup.addEventFilter(SelectEvent.TAINT_VERTEX_SELECTED, this);

        this.visibleRoot = new TaintRootVertex();
        this.immutableRoot = new TaintRootVertex();

        //this.immutableRoot.setInnerGraph(this.cleanTaintGraph(graph));

        for (TaintVertex v : graph.getVertices()) {
            v.setOuterGraph(graph);
        }

        this.immutableRoot.setInnerGraph(graph);
        fillFieldDictionary();
        immAndVis = null;
    }

    public TaintRootVertex getVisibleRoot() {
        return (TaintRootVertex) this.visibleRoot;
    }

    public TaintRootVertex getImmutableRoot() {
        return (TaintRootVertex) this.immutableRoot;
    }

    public void drawGraph() {
        visibleRoot.setVisible(false);

        Set<TaintVertex> verticesToDraw;

        verticesToDraw = visibleAncestors;
        verticesToDraw.addAll(visibleDescendants);

        System.out.println("Taint Vertices to draw: " + verticesToDraw.size());
        GraphTransform<TaintRootVertex, TaintVertex> immToFlatVisible = this.getImmutableRoot().constructVisibleGraph(verticesToDraw);

        GraphTransform<TaintRootVertex, TaintVertex> visibleToNonInner = this.compressInnerNodes(immToFlatVisible.newRoot);

        GraphTransform<TaintRootVertex, TaintVertex> immToNonInner = GraphTransform.transfer(immToFlatVisible, visibleToNonInner);

        // Groups by method
        GraphTransform<TaintRootVertex, TaintVertex> nonInnerToLayerVisible = LayerFactory.getLayeredTaintGraph(visibleToNonInner.newRoot);
        // Groups by Class
        //GraphTransform<TaintRootVertex, TaintVertex> flatToLayerVisible = LayerFactory.getTaintClassGrouping(immToFlatVisible.newRoot);

        immAndVis = GraphTransform.transfer(immToNonInner, nonInnerToLayerVisible);
        this.visibleRoot = immAndVis.newRoot;

        if (expandAll) {
            for (TaintVertex v : visibleRoot.getInnerGraph().getVertices()) {
                if (v instanceof TaintMethodVertex) {
                    v.setExpanded(true);
                }
            }
            expandAll = false;
        }
        if (collapseAll) {
            for (TaintVertex v : visibleRoot.getInnerGraph().getVertices()) {
                if (v instanceof TaintMethodVertex) {
                    v.setExpanded(false);
                }
            }
            collapseAll = false;
        }

        LayoutAlgorithm.layout(visibleRoot);
        drawNodes(null, visibleRoot);
        drawEdges(visibleRoot);
        visibleRoot.setVisible(true);
    }

    public void redrawGraph() {
        // System.out.println("Redrawing loop graph...");
        this.graphContentGroup.getChildren().remove(this.visibleRoot.getGraphics());
        this.drawGraph();
    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.STATE_VERTEX_SELECTED, onVertexSelect);
    }

    @Override
    public void redrawGraphAction(ActionEvent event) throws IOException {
        event.consume();
        this.redrawGraph();
    }

    @Override
    public void hideSelectedAction(ActionEvent event) throws IOException {
        event.consume();
        this.tabController.hideSelectedTaintNodes();
    }

    @Override
    public void hideUnrelatedAction(ActionEvent event) throws IOException {
        event.consume();
        this.tabController.hideUnrelatedToHighlightedTaint();
    }

    @Override
    public void expandAll(ActionEvent event) throws IOException {
        event.consume();
        expandAll = true;
        this.redrawGraph();
    }
    @Override
    public void collapseAll(ActionEvent event) throws IOException {
        event.consume();
        collapseAll = true;
        this.redrawGraph();
    }

    /*
    // Changes to the visible set
    @Override
    public void onChanged(Change<? extends TaintVertex> change) {
        // System.out.println("TaintPanel responding to change in visible set...");
        if (change.wasAdded()) {
            TaintVertex immV = change.getElementAdded();
            immV.setHidden();
            if (immAndVis != null && immAndVis.getNew(immV) != null) {
                immAndVis.getNew(immV).setHidden();
            }
        } else {
            TaintVertex immV = change.getElementRemoved();
            immV.setUnhidden();
            if (immAndVis != null && immAndVis.getNew(immV) != null) {
                immAndVis.getNew(immV).setUnhidden();
            }
        }
    }
    */

    @Override
    public void handle(SelectEvent<TaintVertex> event) {
        TaintVertex vertex = event.getVertex(); // A visible vertex

        if (vertex.getType() == AbstractLayoutVertex.VertexType.ROOT) {
            System.out.println("Ignoring click on vertex root.");
            event.consume();
            return;
        }

        System.out.println("Received event from vertex " + vertex.toString());

        this.tabController.setTaintRightText(vertex);
    }

    // Draw the graph of taint addresses for the selected state vertex, and addresses connected to them.
    private EventHandler<SelectEvent<StateVertex>> onVertexSelect = new EventHandler<SelectEvent<StateVertex>>() {
        @Override
        public void handle(SelectEvent<StateVertex> selectEvent) {
            StateVertex v = selectEvent.getVertex();
            Set<TaintVertex> startVertices = new HashSet<>();
            VizPanelController vizController = TaintPanelController.this.tabController.vizPanelController;
            if (v instanceof StateMethodVertex) {
                StateVertex immV = vizController.getImmutable(v);
                // If we click on a method vertex, we should get all taint addresses for that method.
                startVertices = findAddressesByMethods(immV.getMethodNames());
            }
            else if (v instanceof StateLoopVertex) {
                // Otherwise, if we click on a loop, we just want the addresses controlling the loop.
                StateLoopVertex immV = (StateLoopVertex)vizController.getImmutable(v);
                SootMethod method =  immV.getCompilationUnit().method();
                if (immV.isUnidentifiedLoop()) {
                    // Default to drawing methods
                    startVertices = findAddressesByMethods(immV.getMethodNames());
                }
                else {
                    ArrayList<Value> loopValues = immV.getLoopValues();
                    for (Value value : loopValues) {
                        addTaintVertex(startVertices, value, method);
                    }
                }
            }
            System.out.println("Start vertices: " + startVertices.size());
            drawConnectedVertices(startVertices);
        }
    };

    private void addTaintVertex(Set<TaintVertex> taintVertices, Value value, SootMethod method) {
        TaintVertex v = getTaintVertex(value, method);
        if (v != null) {
            taintVertices.add(v);
        }
    }

    private TaintVertex getTaintVertex(Value value, SootMethod method) {

        for (TaintVertex v : this.getImmutableRoot().getInnerGraph().getVertices()) {

            if (!v.getInnerGraph().isEmpty()) {
                System.out.println("Found a non empty inner graph " + v);
            }

            if(v instanceof TaintAddress) {
                TaintAddress vAddr = (TaintAddress) v;
                if (testAddress(vAddr, value, method)) {
                    return v;
                }
            }
            else if (v instanceof TaintStmtVertex) { // Might be the loop
                for (TaintAddress a : ((TaintStmtVertex) v).getAddresses()) {
                    if (testAddress(a, value, method)) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    private boolean testAddress(TaintAddress vAddr, Value value, SootMethod method) {
        Address addr = vAddr.getAddress();
        if (addr instanceof Address.Value) {
            Value taintValue = ((Address.Value) addr).sootValue();
            //System.out.println("Comparing values: " + taintValue.equivTo(value) + ", " + taintValue + ", " + value);
            //System.out.println("Method: " + addr.sootMethod().toString());
            //System.out.println("Classes: " + taintValue.getClass() + ", " + value.getClass());
            if (compareValues(taintValue, value, vAddr.getSootMethod(), method)) {
                //System.out.println("Found match!");
                return true;
            }
        }

        return false;
    }

    private TaintVertex getTaintVertexRec(TaintVertex v, Value value, SootMethod method) {
        if(v instanceof TaintAddress) {
            TaintAddress vAddr = (TaintAddress) v;
            Address addr = vAddr.getAddress();
            if (addr instanceof Address.Value) {
                Value taintValue = ((Address.Value) addr).sootValue();
                System.out.println("Comparing values: " + taintValue.equivTo(value) + ", " + taintValue + ", " + value);
                System.out.println("Method: " + addr.sootMethod().toString());
                System.out.println("Classes: " + taintValue.getClass() + ", " + value.getClass());
                if (compareValues(taintValue, value, vAddr.getSootMethod(), method)) {
                    System.out.println("Found match!");
                    return v;
                }
            }
        }

        for (TaintVertex w : v.getInnerGraph().getVertices()) {
            TaintVertex result = getTaintVertexRec(w, value, method);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private boolean compareValues(Value taintValue, Value value, SootMethod taintMethod, SootMethod method) {
        if (taintValue.equals(value)) {
            //System.out.println("Equal taint values!");
            return true;
        }
        else if (taintValue.equivTo(value)) {
            //System.out.println("Equivalent taint values!");
            return true;
        }
        else if (value instanceof JimpleLocal) {
            //System.out.println("One JimpleLocal!");
            if (taintValue instanceof JimpleLocal) {
                //System.out.println("Both JimpleLocal!");
                if (taintMethod.getSubSignature().equals(method.getSubSignature())) {
                    //System.out.println("Equal methods!");
                    //System.out.println("SubSignature: " + taintMethod.getSubSignature());
                    if (((JimpleLocal) taintValue).getName().equals(((JimpleLocal) value).getName())) {
                        //System.out.println("Equal names!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void drawConnectedVertices(Set<TaintVertex> addresses) {
        long time1 = System.nanoTime();
        Pair<HashSet<TaintVertex>, HashSet<TaintVertex>> verticesToDraw = findConnectedAddresses(addresses);

        visibleAncestors = verticesToDraw.getKey();
        visibleDescendants = verticesToDraw.getValue();

        long time2 = System.nanoTime();
        this.drawGraph();
        long time3 = System.nanoTime();

        System.out.println("Time to compute connected vertices: " + (time2 - time1) / 1000000000.0);
        System.out.println("Time to draw graph: " + (time3 - time2) / 1000000000.0);
    }

    private HashSet<TaintVertex> findAddressesByMethods(Set<String> methodNames) {
        HashSet<TaintVertex> results = new HashSet<>();
        this.immutableRoot.searchByMethodNames(methodNames, results); // TODO: This step is a little inefficient.
        return results;
    }

    private Pair<HashSet<TaintVertex>, HashSet<TaintVertex> > findConnectedAddresses(Set<TaintVertex> startVertices) {
        HashSet<TaintVertex> ancestors = new HashSet<>();
        HashSet<TaintVertex> descendants = new HashSet<>();

        // TODO: This code is cleaner, but might take longer?
        for (TaintVertex v : startVertices) {
            ancestors.addAll(v.getAncestors());
            descendants.addAll(v.getDescendants());
        }

        return new Pair<>(ancestors, descendants);
    }

    public void showFieldTaintGraph(String fullClassName, String fieldName) {

        String fieldId = fullClassName + ":" + fieldName;
        TaintAddress a = fieldVertices.get(fieldId);

        if (a != null) {
            HashSet<TaintVertex> vertices = new HashSet<>();
            vertices.add(a);
            drawConnectedVertices(vertices);
        }
        else {
            System.out.println("\tWarning: Did not find taint vertex " + fieldId);
        }
    }

    private void fillFieldDictionary() {
        fieldVertices = new HashMap<>();

        ArrayList<TaintAddress> allFields = new ArrayList<>();

        this.immutableRoot.getFields(allFields);

        allFields.forEach(v -> {
            fieldVertices.put(v.getFieldId(), v);
        });
    }

    private GraphTransform<TaintRootVertex, TaintVertex> compressInnerNodes(TaintRootVertex root) {
        return GraphUtils.constructVisibleGraph(root,
                (TaintVertex superV) ->
                {
                    if (! (superV instanceof TaintAddress)){
                        throw new IllegalArgumentException("Non address immutable vertex");
                    }

                    TaintAddress v = (TaintAddress)superV;

                    return v.type != TaintAddress.Type.Inner;

                },
                TaintEdge::new);
    }

    private Graph<TaintVertex, TaintEdge> cleanTaintGraph(Graph<TaintVertex, TaintEdge> graph) {

        int initialSize = graph.getVertices().size();
        graph = removeNonCodeRootAddresses(graph);
        graph = removeNonCodePaths(graph);
        graph = removeDegree2Addresses(graph);

        int numRemoved = initialSize-graph.getVertices().size();

        System.out.println("Removed " + numRemoved + "(" + (((double)numRemoved)/initialSize)*100 + ") final taint size " + graph.getVertices().size());

        return graph;
    }

    private Graph<TaintVertex, TaintEdge> removeNonCodeRootAddresses(Graph<TaintVertex, TaintEdge> graph) {

        HashSet<TaintVertex> toRemove;
        do {
            toRemove = graph.getSources().stream()
                    .filter(v -> v instanceof TaintAddress && isLibrary(v))
                    .collect(Collectors.toCollection(HashSet::new));

            System.out.println("JUAN: Removing " + toRemove.size() + " Non Code Roots of " + graph.getVertices().size()
                    + "(" + ((double)graph.getVertices().size())/toRemove.size() + ")");

            for (TaintVertex v : toRemove) {
                graph.cutVertex(v);
            }

        }while (!toRemove.isEmpty());


        return graph;

    }

    private boolean isLibrary(TaintVertex v) {

        if (v instanceof TaintAddress && ((TaintAddress)v).isArrayRef()) {
            return false;
        }

        return !codeController.haveCode(v.getClassName());
    }

    private Graph<TaintVertex, TaintEdge> removeDegree2Addresses(Graph<TaintVertex, TaintEdge> graph) {

        int initialSize = graph.getVertices().size();

        TaintRootVertex temp = new TaintRootVertex();
        temp.setInnerGraph(graph);
        GraphTransform<TaintRootVertex, TaintVertex> transform = GraphUtils.constructVisibleGraph(temp, v -> {
            return graph.getOutEdges(v).size() != 1 || graph.getInEdges(v).size() != 1;
        }, TaintEdge::new);

        int numRemoved = initialSize - transform.newRoot.getInnerGraph().getVertices().size();

        System.out.println("JUAN: Removing " + numRemoved + " Degree 2 addresses " + transform.newRoot.getInnerGraph().getVertices().size()
                + "(" + ((double)graph.getVertices().size())/numRemoved + ")");

        return transform.newRoot.getInnerGraph();
    }

    private Graph<TaintVertex, TaintEdge> removeNonCodePaths(Graph<TaintVertex, TaintEdge> graph) {

        List<TaintVertex> nonCodeLeafs;

        do {
            nonCodeLeafs = graph.getVertices().stream()
                    .filter(v -> graph.getOutNeighbors(v).isEmpty() && isLibrary(v))
                    .collect(Collectors.toList());

            System.out.println("JUAN: Removing " + nonCodeLeafs.size() + " Non Code Leafs of " + graph.getVertices().size()
                    + "(" + ((double)graph.getVertices().size())/nonCodeLeafs.size() + ")");

            for (TaintVertex v : nonCodeLeafs) {
                graph.cutVertex(v);
            }

        } while (!nonCodeLeafs.isEmpty());

        return graph;
    }

    public HashSet<TaintVertex> getImmutable(HashSet<TaintVertex> visible) {
        return visible.stream()
                .flatMap(v -> v.expand().stream())
                .map(v -> immAndVis.getOld(v)).collect(Collectors.toCollection(HashSet::new));
    }

    public TaintVertex getImmutable(TaintVertex visible) {
        if (immAndVis.containsNew(visible)) {
            return immAndVis.getOld(visible);
        }
        return null;
    }

    // Selection are **visible** nodes
    // Returns the immutable nodes related to the selection of visible nodes
    public HashSet<TaintVertex> getUnrelatedVisible(HashSet<TaintVertex> selection) {

        HashSet<TaintVertex> keep = new HashSet<>();
        Graph<TaintVertex, TaintEdge> topLevel = getVisibleRoot().getInnerGraph();

        selection.forEach(v -> keep.addAll(v.getAncestors()));
        selection.forEach(v -> keep.addAll(v.getDescendants()));

        HashSet<TaintVertex> toHide = new HashSet<>();
        topLevel.getVertices().forEach(v -> {
            if (!keep.contains(v) && !keepAnyInterior(v, keep, toHide)) {
                toHide.add(v);
            }
        });

        return toHide;
    }

    private boolean keepAnyInterior(TaintVertex root, HashSet<TaintVertex> keep, HashSet<TaintVertex> toHide) {

        boolean foundAVertexToKeep = false;
        for (TaintVertex v : root.getInnerGraph().getVertices()) {
            if (keep.contains(v) || keepAnyInterior(v, keep, toHide)) {
                foundAVertexToKeep = true;
            }
            else {
                toHide.add(v);
            }
        }

        return foundAVertexToKeep;
    }

    public void hideVisibleVertices(HashSet<TaintVertex> taintHighlighted) {

        visibleAncestors.removeAll(taintHighlighted);
        visibleDescendants.removeAll(taintHighlighted);
        redrawGraph();
    }
}
