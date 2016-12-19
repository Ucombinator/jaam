package graphs.jviz.uci.edu;

import jviz.uci.edu.UCIJSObject;

public class Edge implements Graphics{
	private String id;
	private String label;
	private Node start;
	private Node to;
	
	private UCIJSObject obj;
	public String getId() {
		return this.id;
	}


	public Node getStart() {
		return this.start;
	}

	public Node getTo() {
		return this.to;
	}

	
	
	public Edge(String id, String label, Node start, Node to) {
		super();
		this.id = id;
		this.label = label;
		this.start = start;
		this.to = to;
	}

	@Override
	public UCIJSObject getGraphics() {
		return obj;
	}

	@Override
	public UCIJSObject setGraphics(UCIJSObject obj) {
		return this.obj = obj;
	}
	


}
