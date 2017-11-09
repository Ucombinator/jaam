package org.ucombinator.jaam.visualizer.layout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

public class LayoutAlgorithm
{
    // This works on a graph whose vertices have been assigned a bounding box
    private static final double MARGIN_PADDING = 10;
    private static final double NODES_PADDING = 10;
    private static final double ROOT_V_OFFSET = 10;
    private static HashMap<Integer, Double> bboxWidthTable;
    private static HashMap<Integer, Double> bboxHeightTable;

    public static void layout(AbstractLayoutVertex parentVertex) {
        bboxWidthTable = new LinkedHashMap<>();
        bboxHeightTable = new LinkedHashMap<>();
        initializeSizes(parentVertex);
        bfsLayout(parentVertex);
        parentVertex.setY(parentVertex.getY() + ROOT_V_OFFSET);
    }

    private static void initializeSizes(AbstractLayoutVertex parentVertex) {
        parentVertex.setWidth(AbstractLayoutVertex.DEFAULT_WIDTH);
        parentVertex.setHeight(AbstractLayoutVertex.DEFAULT_HEIGHT);
        parentVertex.setX(0);
        parentVertex.setY(0);
        for(AbstractLayoutVertex v : parentVertex.getInnerGraph().getVisibleVertices()) {
            initializeSizes(v);
        }
    }

    private static void expandSubGraphs(AbstractLayoutVertex parentVertex) {
        for(AbstractLayoutVertex v: parentVertex.getInnerGraph().getVisibleVertices()) {
            HierarchicalGraph innerGraph = v.getInnerGraph();
            if (innerGraph.getVisibleVertices().size() != 0)
            {
                // Layout the inner graphs of each node and assign width W and height H to each node
                // X and Y coordinates are RELATIVE to the parent
                if (v.isExpanded()) {
                    dfsLayout(v);
                } else {
                    System.out.println("Collapsed node: " + v.getId());
                }
            }
        }
    }

    private static void dfsLayout(AbstractLayoutVertex parentVertex) {
        System.out.println("DFS layout: " + parentVertex.getId());
        HierarchicalGraph graph = parentVertex.getInnerGraph();

        expandSubGraphs(parentVertex);

        for (AbstractLayoutVertex v : graph.getVisibleVertices()) {
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = new HashMap<>();

        for (AbstractLayoutVertex v : graph.getVisibleVertices()) {
            childrenMap.put(v, new ArrayList<>());
            childrenMap.get(v).addAll(graph.getVisibleOutNeighbors(v));
        }

        doLayout(parentVertex, childrenMap);
    }

    private static void bfsLayout(AbstractLayoutVertex parentVertex) {
        HierarchicalGraph graph = parentVertex.getInnerGraph();

        // Interior graphs use the DFS Layout
        expandSubGraphs(parentVertex);

        // Initialize all the nodes to be WHITE
        for(AbstractLayoutVertex v: graph.getVisibleVertices()) {
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        AbstractLayoutVertex root = graph.getRoot();

        // Do the BFS Pass
        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = maxDepthChildren(graph, root);

        // Reset all the nodes to be white AND check that we visited everybody...
        for(AbstractLayoutVertex v: graph.getVisibleVertices()) {
            if(v.getVertexStatus() != AbstractLayoutVertex.VertexStatus.BLACK)
            {
                System.out.println("ERROR in Max Depth Drawings. Does your graph have a cycle?");
                System.out.println("BFS ERROR Didn't process " + v.getId() + " in BFS Children Pass " + v.getVertexStatus());
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        doLayout(parentVertex, childrenMap);
    }

    private static void doLayout(AbstractLayoutVertex parentVertex,
                                 HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap) {
        HierarchicalGraph graph = parentVertex.getInnerGraph();

        AbstractLayoutVertex root = graph.getRoot();

        if(root != null) {
            storeBBoxWidthAndHeight(root, childrenMap);
        }

        for(AbstractLayoutVertex v: graph.getVisibleVertices()){
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        if(root != null) {
            assignXandYtoInnerNodesAndGiveParentBBox(root, MARGIN_PADDING, MARGIN_PADDING, childrenMap);
            if(root.getInnerGraph().getVisibleVertices().size() > 1)
            {
                parentVertex.setWidth(bboxWidthTable.get(root.getId()) + 1000 * MARGIN_PADDING);
                parentVertex.setHeight(bboxHeightTable.get(root.getId()) + 2 * MARGIN_PADDING);
            }
            else
            {
                parentVertex.setWidth(bboxWidthTable.get(root.getId()) + 2 * MARGIN_PADDING);
                parentVertex.setHeight(bboxHeightTable.get(root.getId()) + 2 * MARGIN_PADDING);
            }
        } else {
            parentVertex.setWidth(AbstractLayoutVertex.DEFAULT_WIDTH);
            parentVertex.setHeight(AbstractLayoutVertex.DEFAULT_HEIGHT);
        }
    }

    /**
     * Preconditions: Graph has no Cycles
     * We generate the children map for the layout, where every node is added to the map twice, once as a key
     * and once in the children list of some other node. The root doesn't appear in any children list, and
     * cannot be hidden.
     * Every node appears as a child as deep as possible in the tree (ties, broken arbitrarily)
     */
    private static HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> maxDepthChildren(
            HierarchicalGraph graph, AbstractLayoutVertex root) {
        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = new HashMap<>();
        HashMap<AbstractLayoutVertex, Integer> vertexCounters = new HashMap<>();
        Queue<AbstractLayoutVertex> vertexQueue = new ArrayDeque<>();
        HashSet<AbstractLayoutVertex> seen = new HashSet<>();

        vertexQueue.add(root);
        seen.add(root);

        while(!vertexQueue.isEmpty())
        {
           AbstractLayoutVertex v = vertexQueue.remove();
           childrenMap.put(v, new ArrayList<>());

           for(AbstractLayoutVertex child : graph.getVisibleOutNeighbors(v))
           {
               if(child.equals(v)) {
                   continue; // Skip recursive edge
               }

               for(AbstractLayoutVertex inNeighbor : graph.getVisibleInNeighbors(child)) {
                   System.out.print(inNeighbor.getId() + " ");
               }
               System.out.println();
               if (!seen.contains(child)) {
                   seen.add(child);
                   child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                   int numberOfIncomingEdges = graph.getVisibleInNeighbors(child).size() - 1; // v's incoming edge (v --> child)
                   if (graph.getVisibleInNeighbors(child).contains(child)) {
                       numberOfIncomingEdges -= 1; // Ignore recursive call in edge count
                   }

                   if (numberOfIncomingEdges > 0) {
                       vertexCounters.put(child, numberOfIncomingEdges);
                   } else if (numberOfIncomingEdges == 0) {
                       childrenMap.get(v).add(child);
                       vertexQueue.add(child);
                   }
                   else {
                       for(AbstractLayoutVertex inNeighbor : graph.getVisibleInNeighbors(child)) {
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
            for (Map.Entry<AbstractLayoutVertex, Integer> entry : vertexCounters.entrySet()) {

                System.out.println("\t\t" + entry + " --> " + entry.getKey().getId() + " "
                        + entry.getKey().getVertexStatus() + " " +  entry.getKey().getMethodVertices());
                for(AbstractLayoutVertex n : graph.getVisibleInNeighbors(entry.getKey()))
                {
                    System.out.println("\t\t\t" + n + " --> " + n.getId() + " " + n.getVertexStatus());
                }
            }
        }

        return childrenMap;
    }

    /**
     * Preconditions: Height and Width of the inner nodes of the graph is (recursively known)
     * input: graph and left/top offset
     * Changes of Status: assigns X and Y to the inner vertices of the graph
     * Output: returns the W and H to be assign to the parent node
     * */
    private static double[] storeBBoxWidthAndHeight(
            AbstractLayoutVertex root,
            HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap)
    {
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        ArrayList<AbstractLayoutVertex> grayChildren = new ArrayList<AbstractLayoutVertex>();
        for(AbstractLayoutVertex child: childrenMap.get(root))
        {
            if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE)
            {
                child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                grayChildren.add(child);
            }
        }

        double currentWidth = 0;
        double currentHeight = 0;
        for(AbstractLayoutVertex curVer: grayChildren)
        {
            double[] boundBox = storeBBoxWidthAndHeight(curVer, childrenMap);
            currentWidth += boundBox[0] + NODES_PADDING;
            currentHeight = Math.max(currentHeight, boundBox[1]);
        }

        double currBboxWidth, currBboxHeight;
        currBboxWidth = Math.max(root.getWidth(), currentWidth - NODES_PADDING);
        if(grayChildren.size() == 0)
        {
            currBboxHeight = root.getHeight();
        }
        else
        {
            currBboxHeight = NODES_PADDING + root.getHeight() + currentHeight;
        }

        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);

        bboxWidthTable.put(root.getId(), currBboxWidth);
        bboxHeightTable.put(root.getId(), currBboxHeight);

        double[] result = {currBboxWidth, currBboxHeight};
        return result;
    }

    /**
     * Preconditions: Height and Width of the inner nodes of the graph is known (recursively)
     * Input: graph and left/top offset
     * Changes of Status: assigns X and Y to the inner vertices of the graph
     * Output: returns the W and H to be assigned to the parent node
     * */
    private static void assignXandYtoInnerNodesAndGiveParentBBox(
            AbstractLayoutVertex root, double left, double top,
            HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap)
    {
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        ArrayList<AbstractLayoutVertex> grayChildren = new ArrayList<>();
        for(AbstractLayoutVertex child: childrenMap.get(root))
        {
            if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE)
            {
                child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                grayChildren.add(child);
            }
        }

        double currentWidth = 0;
        double currentHeight = 0;

        for(AbstractLayoutVertex curVer: grayChildren)
        {
            currentWidth += bboxWidthTable.get(curVer.getId()) + NODES_PADDING;
        }

        // TODO: What is AX?
        double AX;
        if(root.getWidth() >= currentWidth - NODES_PADDING) {
            AX = (root.getWidth() - (currentWidth - NODES_PADDING))/2;
        } else {
            AX = 0;
        }

        currentWidth = 0;
        for(AbstractLayoutVertex curVer: grayChildren)
        {
            assignXandYtoInnerNodesAndGiveParentBBox(curVer,currentWidth + left + AX,
                    NODES_PADDING + top + root.getHeight(), childrenMap);
            currentWidth += bboxWidthTable.get(curVer.getId()) + NODES_PADDING;
            currentHeight = Math.max(currentHeight, bboxHeightTable.get(curVer.getId()));
        }

        root.setX(left + ((bboxWidthTable.get(root.getId()) - root.getWidth()) / 2.0));  //left-most corner x
        root.setY(top);                                                    //top-most corner y
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
    }

}


