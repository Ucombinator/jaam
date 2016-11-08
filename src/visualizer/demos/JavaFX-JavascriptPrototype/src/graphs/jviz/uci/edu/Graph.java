package graphs.jviz.uci.edu;

import java.util.HashMap;
import java.util.Map;

public class Graph {
	Map<String,Node> nodes;
	Map<String,Edge> edges;

	
	public Graph(){
		super();
		this.nodes = new HashMap<String,Node>();
		this.edges = new HashMap<String,Edge>();
	}
	
	public Map<String, Node> getNodes() {
		return nodes;
	}

	public Map<String, Edge> getEdges() {
		return edges;
	}
	
	public Node addNode(String id, String label){
		Node n = new Node(id, label);
		this.nodes.put(id, n);
		return n;
	}
	
	public Edge addEdge(String id, String label, String from, String to){
		Edge e = new Edge(id, label, this.nodes.get(from),  this.nodes.get(to));
		this.edges.put(id, e);
		return e;
	}

	public static Graph getSampleGraph() {
		Graph g = new Graph();
		g.addNode("n1","node1").setCenter(280,150);
		g.addNode("n2","node2").setCenter(140,70);
		g.addNode("n3","node3").setCenter(400,80);
		g.addNode("n4","node4").setCenter(500,180);
		g.addNode("n5","node5").setCenter(300,380);
		g.addNode("n6","node6").setCenter(550,300);
		g.addNode("n7","node7").setCenter(500,400);
		g.addEdge("e1","edge1", "n1","n2");
		g.addEdge("e2","edge2", "n2","n3");
		g.addEdge("e3","edge3", "n3","n4");
		g.addEdge("e4","edge4", "n4","n5");
		g.addEdge("e5","edge5", "n3","n5");
		g.addEdge("e6","edge6", "n6","n7");
		g.addEdge("e7","edge7", "n6","n4");
		g.addEdge("e8","edge8", "n5","n7");
		return g;
	}

	public static Graph getSampleGraph(int nodes, int edges) {
		Graph g = new Graph();
		int width = 1600, height = 1200;
		
		// Add each node to a random point
		for(int i = 1; i <= nodes; i++)
		{
			String id = "n" + Integer.toString(i);
			String label = "node" + Integer.toString(i);
			int xCenter = (int)(Math.random()*width);
			int yCenter = (int)(Math.random()*height);
			
			g.addNode(id, label).setCenter(xCenter, yCenter);
		}

		if(nodes > 1)
		{
			// Add random edges
			for(int i = 1; i <= edges; i++)
			{
				int index1 = (int)(Math.random()*nodes);
				int index2 = (int)(Math.random()*nodes);
				while(index1 == index2)
					index2 = (int)(Math.random()*nodes);
	
				String id = "e" + Integer.toString(i);
				String label = "edge" + Integer.toString(i);
				String n1 = "n" + Integer.toString(index1 + 1);
				String n2 = "n" + Integer.toString(index2 + 1);
				g.addEdge(id, label, n1, n2);
			}
		}
		
		return g;
	}
}
