package org.ucombinator.jaam.visualizer.layout;

import java.util.*;

public class LayoutAlgorithm
{
    // This works on a graph whose vertices have been assigned a bounding box
    private static final double MARGIN_PADDING = 10;
    private static final double NODES_PADDING = 10;
    private static final double ROOT_V_OFFSET = 10;

    public static void layout(AbstractLayoutVertex parentVertex) {
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

        // Do the BFS Pass
        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = maxDepthChildren(graph);


        // Reset all the nodes to be white AND check that we visited everybody...
        for (AbstractLayoutVertex v : graph.getVisibleVertices()) {
            if (v.getVertexStatus() != AbstractLayoutVertex.VertexStatus.BLACK) {
                System.out.println("ERROR in Max Depth Drawings. Does your graph have a cycle?");
                System.out.println("BFS ERROR Didn't process " + v.getId() + " in BFS Children Pass " + v.getVertexStatus());
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        doLayout(parentVertex, childrenMap, classComp);
    }

    private static Comparator<AbstractLayoutVertex> classComp = new Comparator<AbstractLayoutVertex>() {
        @Override
        public int compare(AbstractLayoutVertex o1, AbstractLayoutVertex o2) {
            if(o1 instanceof LayoutSccVertex)
            {
                if(o2 instanceof  LayoutSccVertex)
                    return o1.getId() < o2.getId() ? -1 : o1.getId() == o2.getId() ? 0 : 1;
                else
                    return -1;
            }
            else if(o2 instanceof  LayoutSccVertex)
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

            return o1.getId() < o2.getId() ? -1 : o1.getId() == o2.getId() ? 0 : 1;
        }
    };

    private static void doLayout(AbstractLayoutVertex parentVertex,
                                 HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap)
    {
        doLayout(parentVertex, childrenMap, null);
    }

    private static void doLayout(AbstractLayoutVertex parentVertex,
                                 HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap,
                                 Comparator<AbstractLayoutVertex> childrenSortOrder)
    {
        if(childrenSortOrder != null) {
            for (ArrayList<AbstractLayoutVertex> l : childrenMap.values()) {
                l.sort(childrenSortOrder);
            }
        }

        HierarchicalGraph graph = parentVertex.getInnerGraph();
        ArrayList<AbstractLayoutVertex> roots = graph.getRoots();

        double parentWidth = AbstractLayoutVertex.DEFAULT_WIDTH;
        double parentHeight = AbstractLayoutVertex.DEFAULT_HEIGHT;
        if(roots.size() > 0) {
            parentWidth += (roots.size() + 1) * MARGIN_PADDING;
            parentHeight += 2 * MARGIN_PADDING;
        }

        double currentWidth = MARGIN_PADDING;
        for(AbstractLayoutVertex root : roots) {
            storeBBoxWidthAndHeight(root, childrenMap);
            for (AbstractLayoutVertex v : graph.getVisibleVertices()) {
                v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
            }

            assignInnerCoordinates(root, currentWidth, MARGIN_PADDING, childrenMap);
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
     * Every node appears as a child as deep as possible in the tree (ties, broken arbitrarily)
     */
    private static HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> maxDepthChildren(
            HierarchicalGraph graph)
    {
        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = new HashMap<>();
        HashMap<AbstractLayoutVertex, Integer> vertexCounters = new HashMap<>();
        Queue<AbstractLayoutVertex> vertexQueue = new ArrayDeque<>();
        HashSet<AbstractLayoutVertex> seen = new HashSet<>();

        ArrayList<AbstractLayoutVertex> roots = graph.getRoots();
        for(AbstractLayoutVertex root : roots) {
            vertexQueue.add(root);
            seen.add(root);
        }

        while(!vertexQueue.isEmpty())
        {
           AbstractLayoutVertex v = vertexQueue.remove();
           childrenMap.put(v, new ArrayList<>());

           for(AbstractLayoutVertex child : graph.getVisibleOutNeighbors(v))
           {
               if(child.equals(v)) {
                   continue; // Skip recursive edge
               }

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
    private static void storeBBoxWidthAndHeight(
            AbstractLayoutVertex root,
            HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap)
    {
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        ArrayList<AbstractLayoutVertex> grayChildren = new ArrayList<AbstractLayoutVertex>();
        for(AbstractLayoutVertex child: childrenMap.get(root)) {
            if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE) {
                child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                grayChildren.add(child);
            }
        }

        double currentWidth = 0;
        double currentHeight = 0;
        for(AbstractLayoutVertex curVer: grayChildren)
        {
            storeBBoxWidthAndHeight(curVer, childrenMap);
            currentWidth += curVer.getBboxWidth() + NODES_PADDING;
            currentHeight = Math.max(currentHeight, curVer.getBboxHeight());
        }

        double currBboxWidth, currBboxHeight;
        currBboxWidth = Math.max(root.getWidth(), currentWidth - NODES_PADDING);
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
     * Preconditions: Height and width of the inner nodes of the graph is known (recursively)
     * Input: graph and left/top offset
     * State changes: assigns X and Y coordinates to the inner vertices of the graph
     * */
    private static void assignInnerCoordinates (AbstractLayoutVertex root, double left, double top,
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
        for(AbstractLayoutVertex curVer: grayChildren) {
            currentWidth += curVer.getBboxWidth();
        }
        currentWidth += NODES_PADDING * (grayChildren.size() - 1);

        // Check if the root is wider than the total width of its children.
        double rootOverlap;
        if(root.getWidth() >= currentWidth) {
            rootOverlap = (root.getWidth() - currentWidth)/2;
        } else {
            rootOverlap = 0;
        }

        currentWidth = 0;
        for(AbstractLayoutVertex curVer: grayChildren)
        {
            assignInnerCoordinates(curVer,currentWidth + left + rootOverlap,
                    NODES_PADDING + top + root.getHeight(), childrenMap);
            currentWidth += curVer.getBboxWidth() + NODES_PADDING;
            currentHeight = Math.max(currentHeight, curVer.getBboxHeight());
        }

        root.setX(left + ((root.getBboxWidth() - root.getWidth()) / 2.0));  //left-most corner x
        root.setY(top);                                                    //top-most corner y
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
    }
}
