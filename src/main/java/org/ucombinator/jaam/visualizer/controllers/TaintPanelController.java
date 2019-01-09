package org.ucombinator.jaam.visualizer.controllers;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;
import org.ucombinator.jaam.tools.taint3.Address;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.state.StateLoopVertex;
import org.ucombinator.jaam.visualizer.state.StateMethodVertex;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.taint.*;
import soot.SootMethod;
import soot.Value;
import soot.jimple.internal.JimpleLocal;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TaintPanelController extends GraphPanelController<TaintVertex, TaintEdge>
        implements EventHandler<SelectEvent<TaintVertex>> {

    private HashMap<String, TaintAddress> fieldVertices;

    private boolean collapseAll = false, expandAll = false;

    private CodeViewController codeController;

    private HashSet<TaintVertex> immAncestors;
    private GraphTransform<TaintRootVertex, TaintVertex> immAndVisAncestors;
    private HashSet<TaintVertex> immDescendants;
    private GraphTransform<TaintRootVertex, TaintVertex> immAndVisDescendants;
    private Set<TaintVertex> immsplitVertices; // Nodes that split the visible graph
    private Set<TaintVertex> visibleSplitVertices;

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
        immAndVisAncestors = null;
        immAndVisDescendants = null;
    }

    public TaintRootVertex getVisibleRoot() {
        return (TaintRootVertex) this.visibleRoot;
    }

    public TaintRootVertex getImmutableRoot() {
        return (TaintRootVertex) this.immutableRoot;
    }

    public void drawGraph() {
        visibleRoot.setVisible(false);

        Instant startDraw = Instant.now();

        immAndVisAncestors = prepareGraph(immAncestors);
        immAndVisDescendants = prepareGraph(immDescendants);
        immsplitVertices.removeIf(v -> ((TaintAddress)v).type == TaintAddress.Type.Inner);

        Instant afterPrepare = Instant.now();

        // Merge ascendants and descendants into special split graph
        createSplitGraph();

        Instant afterSplitGraph = Instant.now();

        this.visibleRoot = immAndVisAncestors.newRoot;

        /*
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
        */

        LayoutAlgorithm.layoutSplitGraph(getVisibleRoot(), visibleSplitVertices);

        Instant afterLayout = Instant.now();

        //LayoutAlgorithm.layout(visibleRoot);
        drawNodes(null, visibleRoot);

        Instant afterNodeDraw = Instant.now();

        visibleRoot.getGraphics().requestLayout();

        drawEdges(visibleRoot);

        Instant endDraw = Instant.now();

        {

            long totalDraw = Duration.between(startDraw, endDraw).toMillis();
            long prepare = Duration.between(startDraw,afterPrepare).toMillis();
            long createSplit = Duration.between(afterPrepare,afterSplitGraph).toMillis();
            long layout = Duration.between(afterSplitGraph, afterLayout).toMillis();
            long nodeDraw = Duration.between(afterLayout, afterNodeDraw).toMillis();
            long edgeDraw = Duration.between(afterNodeDraw, endDraw).toMillis();

            System.out.println("TIME\tDraw Taint Graph took: " + totalDraw);
            System.out.println("TIME\t\tPrepare:\t" + prepare);
            System.out.println("TIME\t\tCreateSplit:\t" + createSplit);
            System.out.println("TIME\t\tLayout:\t" + layout);
            System.out.println("TIME\t\tNodeDraw:\t" + nodeDraw);
            System.out.println("TIME\t\tEdgeDraw:\t" + edgeDraw);
        }

        visibleRoot.setVisible(true);
    }

    // Recieves a set of immutable vertices to draw
    private GraphTransform<TaintRootVertex, TaintVertex> prepareGraph(Set<TaintVertex> verticesToDraw) {

        Instant startPrepare = Instant.now();

        System.out.println("Taint Vertices to draw: " + verticesToDraw.size());
        GraphTransform<TaintRootVertex, TaintVertex> immToFlatVisible = this.getImmutableRoot().constructVisibleGraph(verticesToDraw);

        Instant afterFlatVisible = Instant.now();

        /*
        System.out.println("---------------END-------------------------");
        System.out.println("Flat Graph");
        for (TaintVertex V : immToFlatVisible.newRoot.getInnerGraph().getVertices()) {
            System.out.println("\t"
                    + V.getOuterGraph().getInNeighbors(V).stream()
                    .count()
                    + ","
                    + V.getOuterGraph().getOutNeighbors(V).stream()
                    .count()
                    + "\t" + V);

        }
        */

        GraphTransform<TaintRootVertex, TaintVertex> visibleToNonInner = this.compressInnerNodes(immToFlatVisible.newRoot);

        Instant afterNonInner = Instant.now();

        GraphTransform<TaintRootVertex, TaintVertex> immToNonInner = GraphTransform.transfer(immToFlatVisible, visibleToNonInner);

        Instant afterNonInnerTransf = Instant.now();

        // Groups by method
        GraphTransform<TaintRootVertex, TaintVertex> nonInnerToLayerVisible = LayerFactory.getLayeredTaintGraph(visibleToNonInner.newRoot);

        Instant endPrepare = Instant.now();

        {
            long totalPrepare = Duration.between(startPrepare,endPrepare).toMillis();
            long flatVisible = Duration.between(startPrepare,afterFlatVisible).toMillis();
            long nonInner = Duration.between(afterFlatVisible,afterNonInner).toMillis();
            long nonInnerTranf = Duration.between(afterNonInner,afterNonInnerTransf).toMillis();
            long layerVisible = Duration.between(afterNonInnerTransf,endPrepare).toMillis();

            System.out.println("TIME\t\t\tPrepare took: " + totalPrepare);
            System.out.println("TIME\t\t\t\tFlatVisible:\t" + flatVisible);
            System.out.println("TIME\t\t\t\tNonInner:\t" + nonInner);
            System.out.println("TIME\t\t\t\tTransfer:\t" + nonInnerTranf);
            System.out.println("TIME\t\t\t\tLayerVisible:\t" + layerVisible);
        }

        // Groups by Class
        //GraphTransform<TaintRootVertex, TaintVertex> flatToLayerVisible = LayerFactory.getTaintClassGrouping(immToFlatVisible.newRoot);

        return GraphTransform.transfer(immToNonInner, nonInnerToLayerVisible);
    }

    // "Merges" ancestors and descendants into a single graph. To maintain connectivity the special
    // node of the ascendants replaces the descendant version.
    private void createSplitGraph() {

        HashSet<TaintVertex> ascSplit = new HashSet<>(), desSplit = new HashSet<>();
        HashMap<TaintVertex, TaintVertex> desToSplit = new HashMap<>();

        System.out.println("Imm Split vertices");
        for (TaintVertex imm : immsplitVertices) {
            System.out.println("\t" + imm);
        }

        for (TaintVertex imm : immsplitVertices) {
            TaintVertex splitA = getSplit(immAndVisAncestors.getNew(imm), ascSplit, immAndVisAncestors.newRoot);
            ascSplit.add(splitA);
            TaintVertex splitD = getSplit(immAndVisDescendants.getNew(imm), desSplit, immAndVisDescendants.newRoot);
            desSplit.add(splitD);
            desToSplit.putIfAbsent(splitD, splitA);
        }

        // Seed with the ascendant graph
        Graph<TaintVertex, TaintEdge> splitGraph = immAndVisAncestors.newRoot.getInnerGraph();
        visibleSplitVertices = ascSplit;

        // Add descendantVertices
        immAndVisDescendants.newRoot.getInnerGraph().getVertices().stream()
                .filter(d -> !desSplit.contains(d))
                .forEach(d -> {
                    splitGraph.addVertex(d);
                    d.setOuterGraph(splitGraph);
                });


        System.out.println("Now adding edges: "
                + "\n\tA : " + immAndVisAncestors.newRoot.getInnerGraph().getEdges().size()
                + "\n\tD : " + immAndVisDescendants.newRoot.getInnerGraph().getEdges().size()
        );

        // Now add the edges, only the outgoing to do the edges once
        for (TaintEdge e : immAndVisDescendants.newRoot.getInnerGraph().getEdges()) {
            TaintVertex src = desSplit.contains(e.getSrc()) ? desToSplit.get(e.getSrc()) : e.getSrc();
            TaintVertex dest = desSplit.contains(e.getDest()) ? desToSplit.get(e.getDest()) : e.getDest();

            splitGraph.addEdge(new TaintEdge(src, dest));
        }

        System.out.println("\tF: " + splitGraph.getEdges().size());

        immAndVisDescendants.newRoot = immAndVisAncestors.newRoot;
    }

    private TaintVertex getSplit(TaintVertex vis, HashSet<TaintVertex> alreadyFound, TaintRootVertex root ) {

        System.out.print("Checking " + vis);

        if (vis.getOuterGraph() == root.getInnerGraph()) {
            System.out.println(" Was not part of a method");
            return vis;
        }
        // Find which method node I am part of
        // This is a horrible piece of code but there isn't anyway of going up the hierarchy
        // Adding that (which should have always been there) would improve this code a lot...

        // Have we seen your method before?
        Optional<TaintVertex> alreadyAdded = alreadyFound.stream()
                .filter(v -> v.getInnerGraph() == vis.getOuterGraph())
                .findAny();

        if (alreadyAdded.isPresent()) {
            System.out.println(" my method was already added");
            return alreadyAdded.get();
        }
        else {
            // So slow...
            Optional<TaintVertex> any = root.getInnerGraph().getVertices().stream()
                    .filter(v -> v.getInnerGraph() == vis.getOuterGraph())
                    .findAny();

            if (!any.isPresent()) {
                throw new IllegalArgumentException("Found a wrong method vertex while splitting");
            }

            System.out.println("Found method " + any.get());

            return any.get();
        }
    }

    public void redrawGraph() {
        // System.out.println("Redrawing loop graph...");
        //this.graphContentGroup.getChildren().remove(this.visibleRoot.getGraphics());
        //this.drawGraph();
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
            Instant handleStart = Instant.now();
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
            Instant afterGetting = Instant.now();
            /*
            System.out.println("Start vertices: " + startVertices.size());
            for (TaintVertex V : startVertices)  {
                System.out.println("\t" + V);
            }
            */

            drawConnectedVertices(startVertices);
            Instant handleEnd = Instant.now();

            long totalHandle = Duration.between(handleStart, handleEnd).toMillis();
            long gettingTook = Duration.between(handleStart, afterGetting).toMillis();
            long drawingTook = Duration.between(afterGetting, handleEnd).toMillis();
            System.out.println("TIME Handle took " + totalHandle);
            System.out.println("TIME \tGetting took " + gettingTook + "\nTIME \tDrawing took " + drawingTook);

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
        Instant startDraw = Instant.now();
        immsplitVertices = addresses;
        Pair<HashSet<TaintVertex>, HashSet<TaintVertex>> verticesToDraw = findConnectedAddresses(addresses);

        immAncestors = verticesToDraw.getKey();
        immDescendants = verticesToDraw.getValue();

        Instant afterFind = Instant.now();
        this.drawGraph();
        Instant afterDraw = Instant.now();

        System.out.println("TIME\t\t Time to compute connected vertices: " + Duration.between(startDraw,afterFind).toMillis());
        System.out.println("TIME\t\t Time to draw graph: " + Duration.between(afterFind,afterDraw).toMillis());
    }

    private HashSet<TaintVertex> findAddressesByMethods(Set<String> methodNames) {
        HashSet<TaintVertex> results = new HashSet<>();
        this.immutableRoot.searchByMethodNames(methodNames, results); // TODO: This step is a little inefficient.
        return results;
    }

    private Pair<HashSet<TaintVertex>, HashSet<TaintVertex> > findConnectedAddresses(Set<TaintVertex> startVertices) {
        HashSet<TaintVertex> ancestors = new HashSet<>();
        HashSet<TaintVertex> descendants = new HashSet<>();

        for (TaintVertex v : startVertices) {
            v.getAncestors(ancestors);
            v.getDescendants(descendants);
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
            .map(v -> immAndVisAncestors.containsNew(v) ? immAndVisAncestors.getOld(v) : immAndVisDescendants.getOld(v))
            .collect(Collectors.toCollection(HashSet::new));
    }

    public TaintVertex getImmutable(TaintVertex visible) {
        if (immAndVisAncestors.containsNew(visible)) {
            return immAndVisAncestors.getOld(visible);
        }
        if (immAndVisDescendants.containsNew(visible)) {
            return immAndVisDescendants.getOld(visible);
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

        immAncestors.removeAll(taintHighlighted);
        immDescendants.removeAll(taintHighlighted);
        redrawGraph();
    }
}
