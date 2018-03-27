package org.ucombinator.jaam.visualizer.layout;

import javafx.geometry.Point2D;
import javafx.util.Pair;
import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
import org.ucombinator.jaam.visualizer.state.StateSccVertex;
import org.ucombinator.jaam.visualizer.taint.TaintSccVertex;

import java.util.*;

// The vertices in this layout must extend both AbstractLayoutVertex and Vertex
public class LayoutAlgorithm
{
    // This works on a graph whose vertices have been assigned a bounding box
    private static final double MARGIN_PADDING = 10;
    private static final double NODES_PADDING = 10;
    private static final double ROOT_V_OFFSET = 10;

    public static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    void layout(T parentVertex) {
        initializeSizes(parentVertex);
        bfsLayout(parentVertex);
        parentVertex.setY(parentVertex.getY() + ROOT_V_OFFSET);
    }

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    void initializeSizes(T parentVertex) {
        parentVertex.setWidth(AbstractLayoutVertex.DEFAULT_WIDTH);
        parentVertex.setHeight(AbstractLayoutVertex.DEFAULT_HEIGHT);
        parentVertex.setX(0);
        parentVertex.setY(0);
        Graph<T, S> childGraph = parentVertex.getChildGraph();
        for (T v : childGraph.getVertices()) {
            initializeSizes(v);
        }
    }

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    void expandSubGraphs(T parentVertex) {
        Graph<T, S> parentChildGraph = parentVertex.getChildGraph();
        for(T v: parentChildGraph.getVertices()) {
            Graph<T, S> childGraph = v.getChildGraph();
            if (childGraph.getVertices().size() != 0)
            {
                // Layout the child graphs of each node and assign width W and height H to each node
                // X and Y coordinates are RELATIVE to the parent
                if (v.isExpanded()) {
                    dfsLayout(v);
                } else {
                    System.out.println("Collapsed node: " + v.getId());
                }
            }
        }
    }

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    void dfsLayout(T parentVertex) {
        Graph<T, S> graph = parentVertex.getChildGraph();

        expandSubGraphs(parentVertex);

        graph.getVertices().forEach(v -> v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE));

        Pair<List<T>, HashMap<T, ArrayList<T>> > rootsAndchildrenMap = getDFSChildMap(graph);

        Point2D dimensions = treeLayout(rootsAndchildrenMap.getKey(), rootsAndchildrenMap.getValue());
        parentVertex.setWidth(dimensions.getX());
        parentVertex.setHeight(dimensions.getY());
    }

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    Pair<List<T>, HashMap<T, ArrayList<T>>> getDFSChildMap(Graph<T,S> graph) {
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

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
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

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    void bfsLayout(T parentVertex) {
        Graph<T, S> graph = parentVertex.getChildGraph();

        // Interior graphs use the DFS Layout
        expandSubGraphs(parentVertex);

        // Initialize all the nodes to be WHITE
        for(T v: graph.getVertices()) {
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

        Point2D dimensions = treeLayout(parentVertex.getChildGraph().getSources(), childrenMap, new ClassComp<>());
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
    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    HashMap<T, ArrayList<T>> maxDepthChildren(Graph<T, S> graph)
    {
        HashMap<T, ArrayList<T>> childrenMap = new HashMap<>();
        HashMap<T, Integer> vertexCounters = new HashMap<>();
        Queue<T> vertexQueue = new ArrayDeque<>();
        HashSet<T> seen = new HashSet<>();

        List<T> roots = graph.getSources();
        if(roots.isEmpty()) {
            System.out.println("Error! Could not build children map.");
            return childrenMap; // No vertices!
        }

        for(T root : roots) {
            vertexQueue.add(root);
            seen.add(root);
        }

        while(!vertexQueue.isEmpty())
        {
            T v = vertexQueue.remove();
            childrenMap.put(v, new ArrayList<>());

            for(T child : graph.getOutNeighbors(v))
            {
                if(child == null) {
                    System.out.println("Error! Null child found.");
                }

                if(child.equals(v)) {
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
                    }
                    else {
                        for(T inNeighbor : graph.getInNeighbors(child)) {
                            System.out.print(inNeighbor.getId() + " ");
                        }
                        System.out.println();
                    }
                } else if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.GRAY) {
                    Integer numberOfIncomingEdges = vertexCounters.get(child);

                    if (numberOfIncomingEdges == null) {
                        System.out.println("Error Map\n\t " + vertexCounters);
                    }
                    else {
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

        if(vertexCounters.size() > 0) {
            System.out.println("BFS uncounted vertices, what happened to the incoming?!!! " + vertexCounters);
            for (Map.Entry<T, Integer> entry : vertexCounters.entrySet()) {

                System.out.println("\t\t" + entry + " --> " + entry.getKey().getId() + " "
                        + entry.getKey().getVertexStatus() /*+ " " +  entry.getKey().getMethodVertices()*/);
                for(T n : graph.getInNeighbors(entry.getKey()))
                {
                    System.out.println("\t\t\t" + n + " --> " + n.getId() + " " + n.getVertexStatus());
                }
            }
        }

        return childrenMap;
    }

    private static class ClassComp<T extends AbstractLayoutVertex<T>> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            if(o1 instanceof StateSccVertex || o1 instanceof TaintSccVertex)
            {
                if(o2 instanceof StateSccVertex || o2 instanceof TaintSccVertex)
                    return Integer.compare(o1.getId(), o2.getId());
                else
                    return -1;
            }
            else if(o2 instanceof StateSccVertex || o2 instanceof TaintSccVertex)
            {
                return 1;
            }
            if(o1 instanceof MethodEntity && o2 instanceof MethodEntity)
            {
                MethodEntity c1 = (MethodEntity)o1;
                MethodEntity c2 = (MethodEntity)o2;

                int shortClassComp = c1.getShortClassName().compareTo(c2.getShortClassName());

                if(shortClassComp != 0)
                    return shortClassComp;

                int fullClassComp = c1.getClassName().compareTo(c2.getClassName());

                if(fullClassComp != 0)
                    return fullClassComp;

                int methodComp = c1.getMethodName().compareTo(c2.getMethodName());

                if(methodComp != 0)
                    return methodComp;
            }

            return Integer.compare(o1.getId(), o2.getId());
        }
    }

    /**
     * Does a tree layout of the graph (defined by the roots and the childmap)
     * Returns: the size of bounding box of the whole graph
     * ChildMap/Roots condition: Every node appears twice:
     *     once as a key in the childrenMap, and
     *     either as a root or in the child list of a different node
     * Note that if a childrenSort order is provided the roots and child lists will be sorted
     * */
    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    Point2D treeLayout(List<T> roots, HashMap<T, ArrayList<T>> childrenMap) {
        return treeLayout(roots, childrenMap, null);
    }

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    Point2D treeLayout(List<T> roots, HashMap<T, ArrayList<T>> childrenMap, Comparator<T> childrenSortOrder)
    {
        if(childrenSortOrder != null) {
            roots.sort(childrenSortOrder);
            childrenMap.values().forEach(l -> l.sort(childrenSortOrder));
        }

        assert !roots.isEmpty();

        double parentWidth = AbstractLayoutVertex.DEFAULT_WIDTH + (roots.size() + 1) * MARGIN_PADDING;
        double parentHeight = AbstractLayoutVertex.DEFAULT_HEIGHT + 2 * MARGIN_PADDING;

        double currentWidth = MARGIN_PADDING;
        for(T root : roots) {
            storeBBoxWidthAndHeight(root, childrenMap);

            assignChildCoordinates(root, currentWidth, MARGIN_PADDING, childrenMap);
            currentWidth += root.getBboxWidth() + MARGIN_PADDING;

            parentWidth += root.getBboxWidth();
            parentHeight  = Math.max(parentHeight, root.getBboxHeight() + 2 * MARGIN_PADDING);
        }

        return new Point2D(parentWidth, parentHeight);
    }


    /**
     * Preconditions: Height and Width of the child nodes of the graph is (recursively known)
     * input: graph and left/top offset
     * Changes of Status: assigns X and Y to the child vertices of the graph
     * Output: returns the W and H to be assign to the parent node
     * */
    private static <T extends AbstractLayoutVertex<T>> void storeBBoxWidthAndHeight(
            T root, HashMap<T, ArrayList<T>> childrenMap) {

        ArrayList<T> children = childrenMap.get(root);
        children.forEach(c -> storeBBoxWidthAndHeight(c, childrenMap));

        double subtreeWidth = children.stream().mapToDouble(T::getBboxWidth).sum() + (children.size() - 1) * NODES_PADDING;
        double subtreeHeight = children.stream().mapToDouble(T::getBboxHeight).max().orElse(0);

        root.setBboxWidth(Math.max(root.getWidth(), subtreeWidth));
        root.setBboxHeight(root.getHeight() + (children.isEmpty() ? 0 : (NODES_PADDING + subtreeHeight)) );
    }

    /**
     * Preconditions: Height and width of the child nodes of the graph is known (recursively)
     * Input: graph and left/top offset
     * State changes: assigns X and Y coordinates to the child vertices of the graph
     * */
    private static <T extends AbstractLayoutVertex<T>> void assignChildCoordinates (T root, double left, double top,
            HashMap<T, ArrayList<T>> childrenMap)
    {
        ArrayList<T> children = childrenMap.get(root);

        // Check if the root is wider than the total width of its children.
        double totalChildWidth = children.stream().mapToDouble(T::getBboxWidth).sum()
                + (NODES_PADDING * (children.size()-1) );
        double rootOverlap;
        if (root.getWidth() >= totalChildWidth) {
            rootOverlap = (root.getWidth() - totalChildWidth)/2;
        } else {
            rootOverlap = 0;
        }

        double currentLeft = left + rootOverlap;
        final double childTop = top + root.getHeight() + NODES_PADDING;
        for (T curVer: children) {
            assignChildCoordinates(curVer, currentLeft, childTop, childrenMap);
            currentLeft += curVer.getBboxWidth() + NODES_PADDING;
        }

        root.setX(left + ((root.getBboxWidth() - root.getWidth()) / 2.0));  //left-most corner x
        root.setY(top);                                                    //top-most corner y
    }
}
