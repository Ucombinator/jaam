package org.ucombinator.jaam.visualizer.layout;

import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.HierarchicalVertex;
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
            Graph<T, S> childChildGraph = v.getChildGraph();
            if (childChildGraph.getVertices().size() != 0)
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

        for (T v : graph.getVertices()) {
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        HashMap<T, ArrayList<T>> childrenMap = new HashMap<>();

        for (T v : graph.getVertices()) {
            childrenMap.put(v, new ArrayList<>());
            childrenMap.get(v).addAll(graph.getOutNeighbors(v));
        }

        doLayout(parentVertex, childrenMap);
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

        doLayout(parentVertex, childrenMap, new ClassComp<>());
    }

    private static class ClassComp<T extends AbstractLayoutVertex<T>> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            if(o1 instanceof LayoutSccVertex || o1 instanceof TaintSccVertex)
            {
                if(o2 instanceof LayoutSccVertex || o2 instanceof TaintSccVertex)
                    return Integer.compare(o1.getId(), o2.getId());
                else
                    return -1;
            }
            else if(o2 instanceof LayoutSccVertex || o2 instanceof TaintSccVertex)
            {
                return 1;
            }
            if(o1 instanceof CodeEntity && o2 instanceof CodeEntity)
            {
                CodeEntity c1 = (CodeEntity)o1;
                CodeEntity c2 = (CodeEntity)o2;

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

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    void doLayout(T parentVertex, HashMap<T, ArrayList<T>> childrenMap)
    {
        doLayout(parentVertex, childrenMap, null);
    }

    private static <T extends AbstractLayoutVertex<T> & HierarchicalVertex<T, S>, S extends Edge<T>>
    void doLayout(T parentVertex, HashMap<T, ArrayList<T>> childrenMap, Comparator<T> childrenSortOrder)
    {
        if(childrenSortOrder != null) {
            for (ArrayList<T> l : childrenMap.values()) {
                l.sort(childrenSortOrder);
            }
        }

        Graph<T, S> graph = parentVertex.getChildGraph();
        List<T> roots = graph.getSources();
        if(roots == null || roots.isEmpty()) {
            return;
        }

        double parentWidth = AbstractLayoutVertex.DEFAULT_WIDTH;
        double parentHeight = AbstractLayoutVertex.DEFAULT_HEIGHT;
        parentWidth += (roots.size() + 1) * MARGIN_PADDING;
        parentHeight += 2 * MARGIN_PADDING;

        double currentWidth = MARGIN_PADDING;
        for(T root : roots) {
            storeBBoxWidthAndHeight(root, childrenMap);
            for (T v : graph.getVertices()) {
                v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
            }

            assignChildCoordinates(root, currentWidth, MARGIN_PADDING, childrenMap);
            currentWidth += root.getBboxWidth() + MARGIN_PADDING;

            parentWidth += root.getBboxWidth();
            parentHeight  = Math.max(parentHeight, root.getBboxHeight() + 2 * MARGIN_PADDING);
        }

        parentVertex.setWidth(parentWidth);
        parentVertex.setHeight(parentHeight);
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
        if(roots == null || roots.isEmpty()) {
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

    /**
     * Preconditions: Height and Width of the child nodes of the graph is (recursively known)
     * input: graph and left/top offset
     * Changes of Status: assigns X and Y to the child vertices of the graph
     * Output: returns the W and H to be assign to the parent node
     * */
    private static <T extends AbstractLayoutVertex<T>> void storeBBoxWidthAndHeight(
            T root,
            HashMap<T, ArrayList<T>> childrenMap)
    {
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        ArrayList<T> grayChildren = new ArrayList<>();
        for(T child: childrenMap.get(root)) {
            if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE) {
                child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                grayChildren.add(child);
            }
        }

        double currentWidth = 0;
        double currentHeight = 0;
        for (T curVer: grayChildren) {
            storeBBoxWidthAndHeight(curVer, childrenMap);
            currentWidth += curVer.getBboxWidth();
            currentHeight = Math.max(currentHeight, curVer.getBboxHeight());
        }
        currentWidth += (grayChildren.size() - 1) * NODES_PADDING;

        double currBboxWidth, currBboxHeight;
        currBboxWidth = Math.max(root.getWidth(), currentWidth);
        if(grayChildren.size() == 0) {
            currBboxHeight = root.getHeight();
        }
        else {
            currBboxHeight = NODES_PADDING + root.getHeight() + currentHeight;
        }

        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
        root.setBboxWidth(currBboxWidth);
        root.setBboxHeight(currBboxHeight);
    }

    /**
     * Preconditions: Height and width of the child nodes of the graph is known (recursively)
     * Input: graph and left/top offset
     * State changes: assigns X and Y coordinates to the child vertices of the graph
     * */
    private static <T extends AbstractLayoutVertex<T>> void assignChildCoordinates (T root, double left, double top,
            HashMap<T, ArrayList<T>> childrenMap)
    {
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        ArrayList<T> grayChildren = new ArrayList<>();
        for(T child: childrenMap.get(root))
        {
            if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE)
            {
                child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                grayChildren.add(child);
            }
        }

        double currentWidth = 0;
        double currentHeight = 0;
        for (T curVer: grayChildren) {
            currentWidth += curVer.getBboxWidth();
        }
        currentWidth += NODES_PADDING * (grayChildren.size() - 1);

        // Check if the root is wider than the total width of its children.
        double rootOverlap;
        if (root.getWidth() >= currentWidth) {
            rootOverlap = (root.getWidth() - currentWidth)/2;
        } else {
            rootOverlap = 0;
        }

        currentWidth = 0;
        for (T curVer: grayChildren) {
            assignChildCoordinates(curVer,currentWidth + left + rootOverlap,
                    NODES_PADDING + top + root.getHeight(), childrenMap);
            currentWidth += curVer.getBboxWidth() + NODES_PADDING;
            currentHeight = Math.max(currentHeight, curVer.getBboxHeight());
        }

        root.setX(left + ((root.getBboxWidth() - root.getWidth()) / 2.0));  //left-most corner x
        root.setY(top);                                                    //top-most corner y
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
    }
}
