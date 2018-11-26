package org.ucombinator.jaam.visualizer.controllers;

import javafx.event.EventHandler;
import javafx.scene.layout.BorderPane;
import org.ucombinator.jaam.tools.taint3.Address;
import org.ucombinator.jaam.util.Loop;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
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

public class TaintPanelController extends GraphPanelController<TaintVertex, TaintEdge>
        implements EventHandler<SelectEvent<TaintVertex>> {

    private GraphTransform<TaintRootVertex, TaintVertex> immToVis;

    private HashMap<String, TaintAddress> fieldVertices;

    // Graph is the statement graph
    public TaintPanelController(Graph<TaintVertex, TaintEdge> graph) throws IOException {
        super(TaintRootVertex::new);

        // Custom event handlers
        graphContentGroup.addEventFilter(SelectEvent.TAINT_VERTEX_SELECTED, this);

        this.visibleRoot = new TaintRootVertex();
        this.immutableRoot = new TaintRootVertex();

        //this.immutableRoot.setInnerGraph(this.removeDegree2Addresses(graph));
        this.immutableRoot.setInnerGraph(graph);
        fillFieldDictionary();
        immToVis = null;
    }

    public TaintRootVertex getVisibleRoot() {
        return (TaintRootVertex) this.visibleRoot;
    }

    public TaintRootVertex getImmutableRoot() {
        return (TaintRootVertex) this.immutableRoot;
    }

    public void drawGraph(HashSet<TaintVertex> verticesToDraw) {
        System.out.println("Drawing taint graph... initial set has " + verticesToDraw.size() + " vertices");

        for(TaintVertex v : verticesToDraw) {
            System.out.println("Checking " + v + " is it immutable? " + (v.getOuterGraph() == immutableRoot.getInnerGraph()) );
            System.out.println("v --> " + " --> " + v.getOuterGraph().getVertices().size() +
                " r --> " + " --> " + immutableRoot.getInnerGraph().getVertices().size());
        }

        visibleRoot.setVisible(false);

        GraphTransform<TaintRootVertex, TaintVertex> immToFlatVisible =
                this.getImmutableRoot().constructVisibleGraph(verticesToDraw);



        GraphTransform<TaintRootVertex, TaintVertex> flatToLayerVisible = LayerFactory.getLayeredTaintGraph(immToFlatVisible.newRoot);

        immToVis = GraphTransform.transfer(immToFlatVisible, flatToLayerVisible);

        //immToVis = immToFlatVisible;
        this.visibleRoot = immToVis.newRoot;

        System.out.println("VISIBLE HAS " + this.visibleRoot.getInnerGraph().getVertices().size());

        LayoutAlgorithm.layout(visibleRoot);
        drawNodes(null, visibleRoot);
        drawEdges(visibleRoot);
        visibleRoot.setVisible(true);
    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.STATE_VERTEX_SELECTED, onVertexSelect);
    }

    @Override
    public void handle(SelectEvent<TaintVertex> event) {
        TaintVertex vertex = event.getVertex(); // A visible vertex

        if (vertex.getType() == AbstractLayoutVertex.VertexType.ROOT) {
            System.out.println("Ignoring click on vertex root.");
            event.consume();
            return;
        }

        System.out.println("Received event from vertex " + vertex.toString());

        MainTabController currentFrame = Main.getSelectedMainTabController();

        if(vertex instanceof TaintAddress) {
            currentFrame.setRightText((TaintAddress) vertex);
        }
        else if(vertex instanceof TaintSccVertex)
        {
            currentFrame.setRightText((TaintSccVertex) vertex);
        }
        else if(vertex instanceof TaintStmtVertex) {
            currentFrame.setRightText((TaintStmtVertex) vertex);
        }
        else if(vertex instanceof TaintMethodVertex) {
            currentFrame.setRightText((TaintMethodVertex) vertex);
        }
        else {
            //currentFrame.bytecodeArea.setDescription();
            currentFrame.setTaintRightText("Text");
        }
    }

    // Draw the graph of taint addresses for the selected state vertex, and addresses connected to them.
    private EventHandler<SelectEvent<StateVertex>> onVertexSelect = new EventHandler<SelectEvent<StateVertex>>() {
        @Override
        public void handle(SelectEvent<StateVertex> selectEvent) {
            StateVertex v = selectEvent.getVertex();
            Set<TaintVertex> startVertices = new HashSet<>();
            if (v instanceof StateMethodVertex) {
                v = Main.getSelectedVizPanelController().getImmutable(v);
                // If we click on a method vertex, we should get all taint addresses for that method.
                startVertices = findAddressesByMethods(v.getMethodNames());
            }
            else if (v instanceof StateLoopVertex) {
                // Otherwise, if we click on a loop, we just want the addresses controlling the loop.
                v = Main.getSelectedVizPanelController().getImmutable(v);
                Loop.LoopInfo loopInfo = ((StateLoopVertex) v).getCompilationUnit().loopInfo();
                SootMethod method = ((StateLoopVertex) v).getCompilationUnit().method();
                if (loopInfo instanceof Loop.UnidentifiedLoop) {
                    // Default to drawing methods
                    startVertices = findAddressesByMethods(v.getMethodNames());
                }
                else if (loopInfo instanceof Loop.IteratorLoop) {
                    Value value = ((Loop.IteratorLoop) loopInfo).iterable();
                    addTaintVertex(startVertices, value, method);
                }
                else if (loopInfo instanceof Loop.ArrayLoop) {
                    Value value = ((Loop.ArrayLoop) loopInfo).iterable();
                    addTaintVertex(startVertices, value, method);
                }
                else if (loopInfo instanceof Loop.SimpleCountUpForLoop) {
                    Value valueLower = ((Loop.SimpleCountUpForLoop) loopInfo).lowerBound();
                    Value valueUpper = ((Loop.SimpleCountUpForLoop) loopInfo).upperBound();
                    Value valueIncrement = ((Loop.SimpleCountUpForLoop) loopInfo).increment();

                    addTaintVertex(startVertices, valueLower, method);
                    addTaintVertex(startVertices, valueUpper, method);
                    addTaintVertex(startVertices, valueIncrement, method);
                }
                else if (loopInfo instanceof Loop.SimpleCountDownForLoop) {
                    Value valueLower = ((Loop.SimpleCountDownForLoop) loopInfo).lowerBound();
                    Value valueUpper = ((Loop.SimpleCountDownForLoop) loopInfo).upperBound();
                    Value valueIncrement = ((Loop.SimpleCountDownForLoop) loopInfo).increment();

                    addTaintVertex(startVertices, valueLower, method);
                    addTaintVertex(startVertices, valueUpper, method);
                    addTaintVertex(startVertices, valueIncrement, method);
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
            System.out.println("Equal taint values!");
            return true;
        }
        else if (taintValue.equivTo(value)) {
            System.out.println("Equivalent taint values!");
            return true;
        }
        else if (value instanceof JimpleLocal) {
            System.out.println("One JimpleLocal!");
            if (taintValue instanceof JimpleLocal) {
                System.out.println("Both JimpleLocal!");
                if (taintMethod.getSubSignature().equals(method.getSubSignature())) {
                    System.out.println("Equal methods!");
                    System.out.println("SubSignature: " + taintMethod.getSubSignature());
                    if (((JimpleLocal) taintValue).getName().equals(((JimpleLocal) value).getName())) {
                        System.out.println("Equal names!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void drawConnectedVertices(Set<TaintVertex> addresses) {
        long time1 = System.nanoTime();
        HashSet<TaintVertex> verticesToDraw = findConnectedAddresses(addresses);
        System.out.println("Taint vertices to draw: " + verticesToDraw.size());

        long time2 = System.nanoTime();
        // Redraw graph with only this set of vertices.
        this.drawGraph(verticesToDraw);
        long time3 = System.nanoTime();

        System.out.println("Time to compute connected vertices: " + (time2 - time1) / 1000000000.0);
        System.out.println("Time to draw graph: " + (time3 - time2) / 1000000000.0);
    }

    private HashSet<TaintVertex> findAddressesByMethods(Set<String> methodNames) {
        HashSet<TaintVertex> results = new HashSet<>();
        this.immutableRoot.searchByMethodNames(methodNames, results); // TODO: This step is a little inefficient.
        return results;
    }

    private HashSet<TaintVertex> findConnectedAddresses(Set<TaintVertex> startVertices) {
        HashSet<TaintVertex> ancestors = new HashSet<>();
        HashSet<TaintVertex> descendants = new HashSet<>();

        // TODO: This code is cleaner, but might take longer?
        for (TaintVertex v : startVertices) {
            ancestors.addAll(v.getAncestors());
            descendants.addAll(v.getDescendants());
        }

        HashSet<TaintVertex> allResults = new HashSet<>();
        allResults.addAll(startVertices);
        allResults.addAll(ancestors);
        allResults.addAll(descendants);

        for (TaintVertex v : allResults) {
            v.setColor(TaintVertex.defaultColor);
            if(v instanceof TaintAddress) {
                TaintAddress vAddr = (TaintAddress) v;
                Address addr = vAddr.getAddress();
                if (addr instanceof Address.Value) {
                    Value value = ((Address.Value) addr).sootValue();
                    if(value instanceof Constant) {
                        v.setColor(TaintVertex.constColor);
                    }
                }
            }
            else if (v instanceof TaintSccVertex) {
                v.setColor(TaintVertex.sccColor);
            }
            else if (startVertices.contains(v)) {
                v.setColor(TaintVertex.currMethodColor);
            } else if (ancestors.contains(v)) {
                if (descendants.contains(v)) {
                    v.setColor(TaintVertex.bothColor);
                }
                else {
                    v.setColor(TaintVertex.upColor);
                }
            }
            else if (descendants.contains(v)) {
                v.setColor(TaintVertex.downColor);
            }
        }

        return allResults;
    }

    public void showFieldTaintGraph(String fullClassName, String fieldName) {

        String fieldId = fullClassName + ":" + fieldName;

        TaintAddress a = fieldVertices.get(fieldId);

        if (a != null) {
            HashSet<TaintVertex> vertices = new HashSet<>();
            vertices.add(a);
            drawConnectedVertices(vertices);
        }
        else
        {
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

    private Graph<TaintVertex, TaintEdge> removeDegree2Addresses(Graph<TaintVertex, TaintEdge> graph) {

        TaintRootVertex temp = new TaintRootVertex();
        temp.setInnerGraph(graph);
        GraphTransform<TaintRootVertex, TaintVertex> transform = GraphUtils.constructVisibleGraph(temp, v -> {
            return graph.getOutEdges(v).size() != 1 || graph.getInEdges(v).size() != 1;
        }, TaintEdge::new);

        return transform.newRoot.getInnerGraph();
    }
}
