package org.ucombinator.jaam.visualizer.layout;

import javafx.geometry.Point2D;
import javafx.util.Pair;
import java.lang.Math;
import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.state.StateSccVertex;
import org.ucombinator.jaam.visualizer.taint.*;

import java.lang.reflect.Array;
import java.util.*;

// The vertices in this layout must extend both AbstractLayoutVertex and Vertex
public class LayoutAlgorithm {
    // This works on a graph whose vertices have been assigned a bounding box
    private static final double MARGIN_PADDING = 10;
    private static final double NODES_PADDING = 10;
    private static final double ROOT_V_OFFSET = 10;
    private static final double NODE_WIDTH = 40;
    private enum LAYERS_TO_CONSIDER{
        TOP,BOTTOM,BOTH,ALL
    }
    public enum LAYOUT_ALGORITHM {
        DFS, BFS, SUMMARY, SPLIT
    }

    public static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void layout(T parentVertex) {
        initializeSizes(parentVertex);
        doLayout(parentVertex);
        parentVertex.setY(parentVertex.getY() + ROOT_V_OFFSET);
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void initializeSizes(T parentVertex) {
        parentVertex.setWidth(AbstractLayoutVertex.DEFAULT_WIDTH);
        parentVertex.setHeight(AbstractLayoutVertex.DEFAULT_HEIGHT);
        parentVertex.setX(0);
        parentVertex.setY(0);
        Graph<T, S> childGraph = parentVertex.getInnerGraph();
        for (T v : childGraph.getVertices()) {
            initializeSizes(v);
        }
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void expandSubGraphs(T parentVertex) {
        Graph<T, S> parentChildGraph = parentVertex.getInnerGraph();
        for (T v : parentChildGraph.getVertices()) {
            Graph<T, S> childGraph = v.getInnerGraph();
            if (childGraph.getVertices().size() != 0) {
                // Layout the child graphs of each node and assign width W and height H to each node
                // X and Y coordinates are RELATIVE to the parent
                if (v.isExpanded()) {
                    doLayout(v);
                }
            }
        }
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void doLayout(T parentVertex) {
        doLayout(parentVertex.getPreferredLayout(), parentVertex);
    }


    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void doLayout(LAYOUT_ALGORITHM alg, T parentVertex) {
        if (alg == LAYOUT_ALGORITHM.DFS) {
            dfsLayout(parentVertex);
        } else if (alg == LAYOUT_ALGORITHM.BFS) {
            bfsLayout(parentVertex);
        } else if (alg == LAYOUT_ALGORITHM.SUMMARY) {
            if (!(parentVertex instanceof TaintMethodVertex)) {
                throw new IllegalArgumentException("SUMMARY Layout requested but " + parentVertex + " not TaintMethodVertex");
            }
            summaryLayout((TaintMethodVertex) parentVertex);
        }
    }

    private static void summaryLayout(TaintMethodVertex parentVertex) {
        // Set width of
        for (TaintVertex v : parentVertex.getInputs()) {
            v.setWidth(TaintMethodVertex.ELEM_WIDTH);
            v.setHeight(TaintMethodVertex.ELEM_HEIGHT);
        }
        for (TaintVertex v : parentVertex.getOutputs()) {
            v.setWidth(TaintMethodVertex.ELEM_WIDTH);
            v.setHeight(TaintMethodVertex.ELEM_HEIGHT);
        }

        int max = Math.max(parentVertex.getInputs().size(), parentVertex.getOutputs().size());

        parentVertex.setWidth(max * TaintMethodVertex.ELEM_WIDTH);
        parentVertex.setHeight(2 * TaintMethodVertex.ELEM_HEIGHT + TaintMethodVertex.LABEL_HEIGHT);
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void dfsLayout(T parentVertex) {
        Graph<T, S> graph = parentVertex.getInnerGraph();

        expandSubGraphs(parentVertex);

        graph.getVertices().forEach(v -> v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE));

        Pair<List<T>, HashMap<T, ArrayList<T>>> rootsAndchildrenMap = getDFSChildMap(graph);

        Point2D dimensions = treeLayout(rootsAndchildrenMap.getKey(), rootsAndchildrenMap.getValue());
        parentVertex.setWidth(dimensions.getX());
        parentVertex.setHeight(dimensions.getY());
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    Pair<List<T>, HashMap<T, ArrayList<T>>> getDFSChildMap(Graph<T, S> graph) {
        graph.getVertices().forEach(v -> v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE));

        HashMap<T, ArrayList<T>> childMap = new HashMap<>();

        List<T> roots = graph.getSources();

        roots.forEach(s -> {
            ArrayList<T> subVertices = new ArrayList<>();
            ArrayList<T> oldKey = childMap.put(s, subVertices);
            assert oldKey == null; // Don't add twice
            s.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);

            addDescendants(s, graph, childMap);
        });

        graph.getVertices().forEach(v -> {
            if (v.getVertexStatus() != AbstractLayoutVertex.VertexStatus.BLACK) {
                System.out.println("ERROR in DFS Pass. ");
                System.out.println("\tDFS ERROR Didn't process " + v.toString());
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        });

        return new Pair<>(roots, childMap);
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void addDescendants(T v, Graph<T, S> graph, HashMap<T, ArrayList<T>> childMap) {

        ArrayList<T> subVertices = new ArrayList<>();
        ArrayList<T> oldKey = childMap.put(v, subVertices);
        assert oldKey == null; // Don't add twice

        graph.getOutNeighbors(v).stream().filter(n -> n.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE).forEach(n -> {
            subVertices.add(n);
            n.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
            addDescendants(n, graph, childMap);
        });

        v.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    void bfsLayout(T parentVertex) {
        Graph<T, S> graph = parentVertex.getInnerGraph();

        // Interior graphs use the DFS Layout
        expandSubGraphs(parentVertex);

        // Initialize all the nodes to be WHITE
        for (T v : graph.getVertices()) {
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        // Do the BFS Pass
        HashMap<T, ArrayList<T>> childrenMap = maxDepthChildren(graph);

        // Reset all the nodes to be white AND check that we visited everybody...
        for (T v : graph.getVertices()) {
            if (v.getVertexStatus() != AbstractLayoutVertex.VertexStatus.BLACK) {
                System.out.println("ERROR in Max Depth Drawings. Does your graph have a cycle?");
                System.out.println("BFS ERROR Didn't process " + v.getId()
                        + " in BFS Children Pass " + v.getVertexStatus());
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        Point2D dimensions = treeLayout(parentVertex.getInnerGraph().getSources(), childrenMap, new ClassComp<>());
        parentVertex.setWidth(dimensions.getX());
        parentVertex.setHeight(dimensions.getY());
    }

    /**
     * Preconditions: Graph has no Cycles
     * We generate the children map for the layout, where every node is added to the map twice, once as a key
     * and once in the children list of some other node. The root doesn't appear in any children list, and
     * cannot be hidden.
     * Every node appears as a child as deep as possible in the tree (ties broken arbitrarily)
     */
    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    HashMap<T, ArrayList<T>> maxDepthChildren(Graph<T, S> graph) {
        HashMap<T, ArrayList<T>> childrenMap = new HashMap<>();
        HashMap<T, Integer> vertexCounters = new HashMap<>();
        Queue<T> vertexQueue = new ArrayDeque<>();
        HashSet<T> seen = new HashSet<>();

        List<T> roots = graph.getSources();
        if (roots.isEmpty()) {
            System.out.println("Error! Could not build children map.");
            return childrenMap; // No vertices!
        }

        for (T root : roots) {
            vertexQueue.add(root);
            seen.add(root);
        }

        while (!vertexQueue.isEmpty()) {
            T v = vertexQueue.remove();
            childrenMap.put(v, new ArrayList<>());

            for (T child : graph.getOutNeighbors(v)) {
                if (child == null) {
                    System.out.println("Error! Null child found.");
                }

                if (child.equals(v)) {
                    continue; // Skip recursive edge
                }

                if (!seen.contains(child)) {
                    seen.add(child);
                    child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);

                    // Subtract v's incoming edge (v --> child)
                    int numberOfIncomingEdges = graph.getInNeighbors(child).size() - 1;
                    if (graph.getInNeighbors(child).contains(child)) {
                        numberOfIncomingEdges -= 1; // Ignore recursive call in edge count
                    }

                    if (numberOfIncomingEdges > 0) {
                        vertexCounters.put(child, numberOfIncomingEdges);
                    } else if (numberOfIncomingEdges == 0) {
                        childrenMap.get(v).add(child);
                        vertexQueue.add(child);
                    } else {
                        for (T inNeighbor : graph.getInNeighbors(child)) {
                            System.out.print(inNeighbor.getId() + " ");
                        }
                        System.out.println();
                    }
                } else if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.GRAY) {
                    Integer numberOfIncomingEdges = vertexCounters.get(child);

                    if (numberOfIncomingEdges == null) {
                        System.out.println("Error Map\n\t " + vertexCounters);
                    } else {
                        numberOfIncomingEdges -= 1;  // v --> child
                        vertexCounters.put(child, numberOfIncomingEdges);

                        if (numberOfIncomingEdges == 0) {
                            childrenMap.get(v).add(child);
                            vertexQueue.add(child);
                            vertexCounters.remove(child);
                        }
                    }
                }
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
        }

        if (vertexCounters.size() > 0) {
            System.out.println("BFS uncounted vertices, what happened to the incoming?!!! " + vertexCounters);
            for (Map.Entry<T, Integer> entry : vertexCounters.entrySet()) {

                System.out.println("\t\t" + entry + " --> " + entry.getKey().getId() + " "
                        + entry.getKey().getVertexStatus() /*+ " " +  entry.getKey().getMethodVertices()*/);
                for (T n : graph.getInNeighbors(entry.getKey())) {
                    System.out.println("\t\t\t" + n + " --> " + n.getId() + " " + n.getVertexStatus());
                }
            }
        }

        return childrenMap;
    }

    private static class ClassComp<T extends AbstractLayoutVertex> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            if (o1 instanceof StateSccVertex || o1 instanceof TaintSccVertex) {
                if (o2 instanceof StateSccVertex || o2 instanceof TaintSccVertex)
                    return Integer.compare(o1.getId(), o2.getId());
                else
                    return -1;
            } else if (o2 instanceof StateSccVertex || o2 instanceof TaintSccVertex) {
                return 1;
            }
            if (o1 instanceof MethodEntity && o2 instanceof MethodEntity) {
                MethodEntity c1 = (MethodEntity) o1;
                MethodEntity c2 = (MethodEntity) o2;

                int shortClassComp = c1.getShortClassName().compareTo(c2.getShortClassName());

                if (shortClassComp != 0)
                    return shortClassComp;

                int fullClassComp = c1.getClassName().compareTo(c2.getClassName());

                if (fullClassComp != 0)
                    return fullClassComp;

                int methodComp = c1.getMethodName().compareTo(c2.getMethodName());

                if (methodComp != 0)
                    return methodComp;
            }

            return Integer.compare(o1.getId(), o2.getId());
        }
    }

    /**
     * Does a tree layout of the graph (defined by the roots and the childmap)
     * Returns: the size of bounding box of the whole graph
     * ChildMap/Roots condition: Every node appears twice:
     * once as a key in the childrenMap, and
     * either as a root or in the child list of a different node
     * Note that if a childrenSort order is provided the roots and child lists will be sorted
     */
    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    Point2D treeLayout(List<T> roots, HashMap<T, ArrayList<T>> childrenMap) {
        return treeLayout(roots, childrenMap, null);
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    Point2D treeLayout(List<T> roots, HashMap<T, ArrayList<T>> childrenMap, Comparator<T> childrenSortOrder) {
        //setSpanningEdges(childrenMap);
        if (childrenSortOrder != null) {
            roots.sort(childrenSortOrder);
            childrenMap.values().forEach(l -> l.sort(childrenSortOrder));
        }

        assert !roots.isEmpty();

        double parentWidth = AbstractLayoutVertex.DEFAULT_WIDTH + (roots.size() + 1) * MARGIN_PADDING;
        double parentHeight = AbstractLayoutVertex.DEFAULT_HEIGHT + 2 * MARGIN_PADDING;

        double currentWidth = MARGIN_PADDING;
        for (T root : roots) {
            storeBBoxWidthAndHeight(root, childrenMap);

            assignChildCoordinates(root, currentWidth, MARGIN_PADDING, childrenMap);
            currentWidth += root.getBboxWidth() + MARGIN_PADDING;

            parentWidth += root.getBboxWidth();
            parentHeight = Math.max(parentHeight, root.getBboxHeight() + 2 * MARGIN_PADDING);
        }

        return new Point2D(parentWidth, parentHeight);
    }

    private static <T extends AbstractLayoutVertex & HierarchicalVertex<T, S>, S extends Edge<T>>
    Point2D modifiedTreeLayout(List<T> roots, HashMap<T, ArrayList<T>> childrenMap, Comparator<T> childrenSortOrder) {
        //setSpanningEdges(childrenMap);
        if (childrenSortOrder != null) {
            roots.sort(childrenSortOrder);
            childrenMap.values().forEach(l -> l.sort(childrenSortOrder));
        }

        assert !roots.isEmpty();

        double parentWidth = AbstractLayoutVertex.DEFAULT_WIDTH + (roots.size() + 1) * MARGIN_PADDING;
        double parentHeight = AbstractLayoutVertex.DEFAULT_HEIGHT + 2 * MARGIN_PADDING;

        double currentWidth = MARGIN_PADDING;
        for (T root : roots) {
            storeBBoxWidthAndHeight(root, childrenMap);

            assignChildCoordinates(root, currentWidth, MARGIN_PADDING, childrenMap);
            currentWidth += root.getBboxWidth() + MARGIN_PADDING;

            parentWidth += root.getBboxWidth();
            parentHeight = Math.max(parentHeight, root.getBboxHeight() + 2 * MARGIN_PADDING);
        }

        return new Point2D(parentWidth, parentHeight);
    }
    /*
     * Preconditions: Height and Width of the child nodes of the graph is (recursively known)
     * input: graph and left/top offset
     * Changes of Status: assigns width and height of BBOX to root and all children in the same graph (no inner recursion)
     * */
    private static <T extends AbstractLayoutVertex> void storeBBoxWidthAndHeight(
            T root, HashMap<T, ArrayList<T>> childrenMap) {

        ArrayList<T> children = childrenMap.get(root);
        children.forEach(c -> storeBBoxWidthAndHeight(c, childrenMap));

        double subtreeWidth = children.stream().mapToDouble(T::getBboxWidth).sum() + (children.size() - 1) * NODES_PADDING;
        double subtreeHeight = children.stream().mapToDouble(T::getBboxHeight).max().orElse(0);

        root.setBboxWidth(Math.max(root.getWidth(), subtreeWidth));
        root.setBboxHeight(root.getHeight() + (children.isEmpty() ? 0 : (NODES_PADDING + subtreeHeight)));
    }

    /**
     * Preconditions: Height and width of the child nodes of the graph is known (recursively)
     * Input: graph and left/top offset
     * State changes: assigns X and Y coordinates to the child vertices of the graph
     */
    private static <T extends AbstractLayoutVertex> void assignChildCoordinates(T root, double left, double top,
                                                                                HashMap<T, ArrayList<T>> childrenMap) {
        ArrayList<T> children = childrenMap.get(root);

        // We want the root to be centered on top of its subtree, if the subtree is wider. Or the subtree
        // centered on the root if the root is wider
        double totalChildWidth = children.stream().mapToDouble(T::getBboxWidth).sum()
                + (NODES_PADDING * (children.size() - 1));

        double leftOffset = Math.abs(totalChildWidth - root.getWidth()) / 2;


        double currentChildLeft = left;
        if (root.getWidth() > totalChildWidth) {
            currentChildLeft += leftOffset; // Offset the children to center
            root.setX(left);
        } else {
            root.setX(left + leftOffset); // Offset the root to center
        }

        final double childTop = top + root.getHeight() + NODES_PADDING;
        for (T curVer : children) {
            assignChildCoordinates(curVer, currentChildLeft, childTop, childrenMap);
            currentChildLeft += curVer.getBboxWidth() + NODES_PADDING;
        }

        root.setY(top); //top-most corner y
    }

    /*
    private static <T extends AbstractLayoutVertex> void setSpanningEdges(HashMap<T, ArrayList<T>> childrenMap) {


        // Mark which edges are in our spanning tree, and which are not.
        for (T parent : childrenMap.keySet()) {
            Set<T> childrenSet = new HashSet<>(childrenMap.get(parent));
            for (LayoutEdge<?> edge : parent.getIncidentEdges()) {
                edge.setSpanningEdge(childrenSet.contains(edge.getDest()));
            }
        }
    }
    */

    // Receives the root vertex and the split vertices.
    // Sets the coordinates and sizes of every vertex
    // Note: I think the getWidth is not working... I'll work on it
    public static double layoutFindTempX(TaintVertex v) {
        HashSet<TaintVertex> adjacent = (HashSet<TaintVertex>) v.getOuterGraph().getInNeighbors(v);
        adjacent.addAll(v.getOuterGraph().getOutNeighbors(v));
        int tempX = 0;
        int count = 0;
        for (TaintVertex u : adjacent) {
            if (u.getVertexStatus() == AbstractLayoutVertex.VertexStatus.GRAY) {
                tempX += u.getX();
                count++;
            }
        }
        return tempX / count;
    }

    public static ArrayList<TaintVertex> findDirectAncestors(ArrayList<TaintVertex> prev) {
        ArrayList<TaintVertex> curr = new ArrayList<>();
        for (TaintVertex v : prev) {
            for (TaintVertex n : v.getOuterGraph().getInNeighbors(v)) {
                if (n.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE) {
                    curr.add(n);
                    n.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                }
            }
        }
        return curr;
    }

    private static ArrayList<TaintVertex> findDirectDescendants(ArrayList<TaintVertex> prev) {
        ArrayList<TaintVertex> curr = new ArrayList<>();
        for (TaintVertex v : prev) {
            for (TaintVertex n : v.getOuterGraph().getOutNeighbors(v)) {
                if (n.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE) {
                    curr.add(n);
                    n.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                }
            }
        }
        return curr;
    }

    private static void sortSemiTopo(ArrayList<TaintVertex> curr) {
        final HashMap<TaintVertex, Integer> helper = new HashMap<>();
        Comparator<TaintVertex> topo = new Comparator<TaintVertex>() {
            @Override
            public int compare(TaintVertex o1, TaintVertex o2) {
                if (helper.get(o1) < helper.get(o2)) {
                    return -1;
                }
                if (helper.get(o1) > helper.get(o2)) {
                    return 1;
                }
                return o1.toString().compareTo(o2.toString());
            }
        };
        HashSet<TaintVertex> temp = new HashSet<>();
        for (TaintVertex v : curr) {
            temp = new HashSet<TaintVertex>(v.getOuterGraph().getOutNeighbors(v));
            temp.addAll(v.getOuterGraph().getInNeighbors(v));
            temp.retainAll(curr);
            helper.put(v, temp.size());
        }
        curr.sort(topo);
    }
    private static void weightedPlaceLayer(ArrayList<TaintVertex> layer, Collection<TaintVertex> adj) {
        Comparator<TaintVertex> byX = new Comparator<TaintVertex>() {
            @Override
            public int compare(TaintVertex o1, TaintVertex o2) {
                int compare = Double.compare(o1.getX(), o2.getX());
                if(compare==0){
                    compare=o1.toString().compareTo(o2.toString());
                }
                return compare;
            }
        };
        for (TaintVertex v : layer) {
            HashSet<TaintVertex> neighbors = new HashSet<TaintVertex>(v.getOuterGraph().getOutNeighbors(v));
            neighbors.addAll(v.getOuterGraph().getInNeighbors(v));
            neighbors.retainAll(adj);
            double x = 0;
            double count = 0;
            double weight;
            for (TaintVertex n : neighbors) {
                if (n.getVertexStatus() == AbstractLayoutVertex.VertexStatus.BLACK) {
                    weight = Math.abs(n.getY()-v.getY())/NODE_WIDTH;
                    x += n.getX()/weight;
                    count += weight;
                }
            }
            if (count != 0) {
                v.setX(x*1./ count);
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        }
        layer.sort(byX);
        System.out.print("Placed: ");
        for(TaintVertex v:layer){
            System.out.print(v.getX()+", ");
        }
        System.out.print("\n");
    }
    private static void partialWeightedPlaceLayer(ArrayList<TaintVertex> layer, Collection<TaintVertex> adj) {
        Comparator<TaintVertex> byX = new Comparator<TaintVertex>() {
            @Override
            public int compare(TaintVertex o1, TaintVertex o2) {
                int compare = Double.compare(o1.getX(), o2.getX());
                if(compare==0){
                    compare=o1.toString().compareTo(o2.toString());
                }
                return compare;
            }
        };
        for (TaintVertex v : layer) {
            HashSet<TaintVertex> neighbors = new HashSet<TaintVertex>(v.getOuterGraph().getOutNeighbors(v));
            neighbors.addAll(v.getOuterGraph().getInNeighbors(v));
            neighbors.retainAll(adj);
            double x = 0;
            double count = 0;
            double weight;
            for (TaintVertex n : neighbors) {
                if (n.getVertexStatus() == AbstractLayoutVertex.VertexStatus.BLACK) {
                    weight = Math.abs(n.getY()-v.getY())/NODE_WIDTH;
                    x += n.getX()/weight;
                    count += weight;
                }
            }
            if (count != 0) {
                v.setX(x / count);
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        }
        layer.sort(byX);
        System.out.print("Placed: ");
        for(TaintVertex v:layer){
            System.out.print(v.getX()+", ");
        }
        System.out.print("\n");
    }
    private static void indexPlaceLayer(ArrayList<TaintVertex> layer, Collection<TaintVertex> adj){
        Comparator<TaintVertex> byX = new Comparator<TaintVertex>() {
            @Override
            public int compare(TaintVertex o1, TaintVertex o2) {
                int compare = Double.compare(o1.getX(), o2.getX());
                if(compare==0){
                    compare=o1.toString().compareTo(o2.toString());
                }
                return compare;
            }
        };
        for (TaintVertex v : layer) {
            double x = 0;
            double count = 0;
            double weight;
            HashSet<TaintVertex> neighbors = new HashSet<TaintVertex>(v.getOuterGraph().getOutNeighbors(v));
            neighbors.addAll(v.getOuterGraph().getInNeighbors(v));
            for (TaintVertex n : neighbors) {
                if (n.getVertexStatus() == AbstractLayoutVertex.VertexStatus.BLACK) {
                    weight = Math.abs(n.getY()-v.getY())/NODE_WIDTH;
                    x += n.getX()/weight;
                    count += weight;
                }
            }
            neighbors.retainAll(adj);
            if (count != 0) {
                v.setX(x *1./ count);
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        }
        layer.sort(byX);
        System.out.print("Placed: ");
        for(TaintVertex v:layer){
            System.out.print(v.getX()+", ");
        }
        System.out.print("\n");
    }
    private static void placeLayer(ArrayList<TaintVertex> layer, LAYERS_TO_CONSIDER adj) {
        Comparator<TaintVertex> byX = new Comparator<TaintVertex>() {
            @Override
            public int compare(TaintVertex o1, TaintVertex o2) {
                int compare = Double.compare(o1.getX(), o2.getX());
                if(compare==0){
                    compare=o1.toString().compareTo(o2.toString());
                }
                return compare;
            }
        };
        for (TaintVertex v : layer) {
            placeVertex(v, adj);
        }
        layer.sort(byX);
    }

    private static void placeVertex(TaintVertex v, LAYERS_TO_CONSIDER adj) {
        HashSet<TaintVertex> neighbors = new HashSet<TaintVertex>(v.getOuterGraph().getOutNeighbors(v));
        neighbors.addAll(v.getOuterGraph().getInNeighbors(v));
        switch(adj){
            case BOTH: neighbors.removeIf(u -> (Math.abs(u.getY() - v.getY()) > NODE_WIDTH)); break;
            case TOP: neighbors.removeIf(u -> ((Math.abs(u.getY()-v.getY())>NODE_WIDTH)&&(u.getY()<=v.getY()))); break;
            case BOTTOM: neighbors.removeIf(u -> ((Math.abs(u.getY()-v.getY())>NODE_WIDTH)&&(u.getY()>=v.getY()))); break;
        }
        double x = 0;
        int count = 0;
        for (TaintVertex n : neighbors) {
            if (n.getVertexStatus() != AbstractLayoutVertex.VertexStatus.WHITE) {
                x += n.getX(); count++;
            }
        }
        if (count != 0) { v.setX(x*1./ count);
        }
        v.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
    }

    private static void evenlySpace(ArrayList<TaintVertex> layer, int i) {
        if (i < layer.size()) {
            if(i==0){
                layer.get(i).setX(NODE_WIDTH/2.);
            }else{
                layer.get(i).setX(layer.get(i-1).getX()+NODE_WIDTH);
            }
            evenlySpace(layer,i+1);
        }
    }
    private static void spaceOutLayer(ArrayList<TaintVertex> layer, int i) {
        if (i < layer.size()) {
            TaintVertex curr = layer.get(i);
            TaintVertex prev = null;
            TaintVertex next = null;
            if (i > 1) {
                prev = layer.get(i - 1);
            }
            if (i < layer.size() - 1) {
                next = layer.get(i + 1);
            }

            //if space to next isn't enough
            if ((next != null) && (next.getX() - curr.getX() < NODE_WIDTH)) {
                // if no previous
                if (prev == null) {
                    if ((curr.getX() < NODE_WIDTH / 2.)) {
                        curr.setX(NODE_WIDTH / 2.);
                    }
                    if ((next.getX() - curr.getX() < NODE_WIDTH)) {
                        next.setX(curr.getX() + NODE_WIDTH);
                    }
                    // if not enough room
                } else if (next.getX() - prev.getX() < 2 * NODE_WIDTH) {
                    curr.setX(prev.getX() + NODE_WIDTH);
                    next.setX(curr.getX() + NODE_WIDTH);
                } else {
                    curr.setX(next.getX() - NODE_WIDTH);
                }
            }
            curr.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
            spaceOutLayer(layer, i + 1);
        }
    }
    private static ArrayList<ArrayList<TaintVertex>> findLayers(Set<TaintVertex> splitVertices) {
        ArrayList<ArrayList<TaintVertex>> layers = new ArrayList<>();
        layers.add(new ArrayList<TaintVertex>(splitVertices));
        for (TaintVertex v : splitVertices) {
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        }
        while (!layers.get(0).isEmpty()) {
            layers.add(0, findDirectAncestors(layers.get(0)));
        }
        ArrayList<TaintVertex> last = layers.get(layers.size() - 1);
        while (!last.isEmpty()) {
            layers.add(findDirectDescendants(last));
            last = layers.get(layers.size() - 1);
        }
        return layers;
    }
    private static HashSet<TaintVertex> addSemiLoners(ArrayList<ArrayList<TaintVertex>> layers,HashSet<TaintVertex> drawn){
        HashSet<TaintVertex> out = new HashSet<>();
        out.addAll(drawn);
        for (ArrayList<TaintVertex> layer : layers) { out.addAll(layer); }
        ArrayList<TaintVertex> temp;
        int i=0;
        int changes=0;
        while(changes!=0) {
            changes=0;
            i=0;
            for (ArrayList<TaintVertex> layer : layers) {
                temp=new ArrayList<TaintVertex>();
                for(TaintVertex v:layer){
                    temp.addAll(v.getOuterGraph().getInNeighbors(v));
                    temp.removeAll(out);
                    if(temp.size()>0){
                        layers.get(i-1).addAll(temp);
                        temp=new ArrayList<>();
                        out.addAll(temp);
                    }
                }

                temp=new ArrayList<TaintVertex>();
                for(TaintVertex v:layer){
                    temp.addAll(v.getOuterGraph().getOutNeighbors(v));
                    temp.removeAll(out);
                    if(temp.size()>0){
                        layers.get(i+1).addAll(temp);
                        temp=new ArrayList<>();
                        out.addAll(temp);
                    }
                }
                i++;
            }
        }
        return out;
    }
    public static void layoutSplitGraph(TaintRootVertex root, Set<TaintVertex> splitVertices) {
        initializeSizes(root);
        for (TaintVertex v : root.getInnerGraph().getVertices()) {
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
            v.setY(NODE_WIDTH);
            v.setX(NODE_WIDTH);
        }
        ArrayList<ArrayList<TaintVertex>> layers = findLayers(splitVertices);
        HashSet<TaintVertex> drawn = addSemiLoners(layers, new HashSet<>());
        final double[] H = {NODE_WIDTH / 2.};
        for (ArrayList<TaintVertex> layer : layers) {
            System.out.println("Layer " + H[0]);
            layer.forEach(v -> v.setY(H[0]));
            sortSemiTopo(layer);
            H[0] += NODE_WIDTH;
        }
        int i = 0;
        int j = 0;
        double[] W = {0};
        // first
        for (ArrayList<TaintVertex> layer : layers) {
            placeLayer(layer, LAYERS_TO_CONSIDER.TOP);
            System.out.print("Placed: ");
            for (TaintVertex v : layer) {
                System.out.print(v.getX() + ", ");
            }
            System.out.print("\n");
            spaceOutLayer(layer, 0);
            System.out.print("Spaced: ");
            for (TaintVertex v : layer) {
                System.out.print(v.getX() + ", ");
            }
            System.out.print("\n");
            double lastX;
            try {
                lastX = layer.get(layer.size() - 1).getX();
            } catch (Exception e) {
                lastX = NODE_WIDTH * 2;
            }
            if (W[0] < lastX) {
                W[0] = lastX;
                j = i;
            }
            i++;
        }
        if (layers.size() > 1) {
            // upwards
            for (i = j - 1; i >= 0; i--) {
                placeLayer(layers.get(i), LAYERS_TO_CONSIDER.BOTTOM);
                spaceOutLayer(layers.get(i), 0);
            }
            //downwards
            for (i = j + 1; i < layers.size(); i++) {
                placeLayer(layers.get(i), LAYERS_TO_CONSIDER.TOP);
                spaceOutLayer(layers.get(i), 0);
            }
        }
        // peek at tree
//        System.out.println("Tree: ");
//        for (ArrayList<TaintVertex> layer : layers) {
//            double lastX;
//            try {
//                lastX = layer.get(layer.size() - 1).getX();
//            } catch (Exception e) {
//                lastX = NODE_WIDTH * 2;
//            }
//            if (lastX > W[0]) {
//                W[0] = lastX;
//            }
//        }
//        for (ArrayList<TaintVertex> layer : layers) {
//            for (TaintVertex v : layer) {
//                System.out.print(v.getId() + ": (" + v.getX() + ", " + v.getY() + ")\t");
//            }
//            System.out.print("\n");
//        }
        // find loners
        ArrayList<TaintVertex> loners = new ArrayList<>(root.getInnerGraph().getVertices());
        loners.removeIf(v -> (drawn.contains(v) || (v.getVertexStatus() != AbstractLayoutVertex.VertexStatus.WHITE)));
        System.out.println("Drawing " + loners.size() + " Loners");
        HashSet<TaintVertex> temp;
        double height = H[0];
        double width = W[0] + NODE_WIDTH / 2.;
        int numLoners = loners.size();
        ArrayList<ArrayList<TaintVertex>> lonerLayers = new ArrayList<>();
        lonerLayers.add(new ArrayList<>());
        lonerLayers.add(loners);
        lonerLayers.add(new ArrayList<>());
        i = 0;
        TaintVertex loner;
        HashSet<TaintVertex> toMove = new HashSet<>();
        while (i < loners.size()) {
            loner = loners.get(i);
            //move up
            toMove = new HashSet<>();
            toMove.addAll(loner.getOuterGraph().getInNeighbors(loner));
            toMove.retainAll(loners);
            lonerLayers.get(0).addAll(toMove);
            loners.removeAll(toMove);
            //move down
            toMove = new HashSet<>();
            toMove.addAll(loner.getOuterGraph().getOutNeighbors(loner));
            toMove.retainAll(loners);
            lonerLayers.get(2).addAll(toMove);
            loners.removeAll(toMove);
            i++;
        }
        H[0] = NODE_WIDTH / 2.;
        lonerLayers.add(0, new ArrayList<>());
        for (ArrayList<TaintVertex> layer : lonerLayers) {
            layer.forEach(v -> v.setY(H[0]));
            H[0] += NODE_WIDTH;
        }
        for (ArrayList<TaintVertex> layer : lonerLayers) {
            placeLayer(layer, LAYERS_TO_CONSIDER.TOP);
            System.out.print("Placed: ");
            for (TaintVertex v : layer) {
                System.out.print(v.getX() + ", ");
            }
            System.out.print("\n");
            spaceOutLayer(layer, 0);
            System.out.print("Spaced: ");
            for (TaintVertex v : layer) {
                System.out.print(v.getX() + ", ");
            }
            System.out.print("\n");
            double lastX;
            try {
                lastX = layer.get(layer.size() - 1).getX();
            } catch (Exception e) {
                lastX = NODE_WIDTH * 2;
            }
            if (width < lastX + W[0]) {
                width = lastX + W[0];
            }
        }
        W[0] = W[0] + NODE_WIDTH;
        for (ArrayList<TaintVertex> layer : lonerLayers) {
            layer.forEach(v -> v.setX(v.getX() + W[0]));
        }
        W[0] = width + NODE_WIDTH;
        System.out.println("Done.");
        System.out.println("Total Vertex In: " + root.getInnerGraph().getVertices().size());
        System.out.println("Loners:" + numLoners);
        root.setWidth(W[0] + NODE_WIDTH / 2);
        root.setHeight(height);
    }
}