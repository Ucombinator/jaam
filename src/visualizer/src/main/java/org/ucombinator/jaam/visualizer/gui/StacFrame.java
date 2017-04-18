package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;

import java.util.ArrayList;

import java.util.StringTokenizer;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
import org.ucombinator.jaam.visualizer.layout.LayoutRootVertex;
import org.ucombinator.jaam.visualizer.main.Parameters;
import org.ucombinator.jaam.visualizer.graph.Graph;

/**
 * JFrame showing a map
 * @author Jawaherul
 *
 */

public class StacFrame extends BorderPane
{

	public VizPanel mainPanel; //TODO: Make private
	private TextArea rightArea;
	private CodeArea bytecodeArea;
	private SearchResults searchResults;
	private Graph graph;

	private SplitPane horizontalSplitPane;
	private FlowPane buttonsFlowPane;
	private BorderPane searchPanel, bytecodePanel, rightPanel;
	public CheckBox showEdge;

	public enum searchType
	{
		ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
	}
	
	public StacFrame(Graph graph)
	{
		this.graph = graph;
		if (Parameters.debugMode)
			makeSimpleLayout();
		else
			makeLayout();

		this.mainPanel.initFX(this.graph);
		this.setVisible(true);
	}

	public TextArea getRightArea() {
		return this.rightArea;
	}

	public CodeArea getBytecodeArea() {
		return this.bytecodeArea;
	}

	public SearchResults getSearchResults() {
		return this.searchResults;
	}

	public Graph getGraph() {
		return this.graph;
	}

	public void repaintAll()
	{
		System.out.println("Repainting all...");
		if (!Parameters.debugMode)
		{
			bytecodeArea.setDescription();
			setRightText();
			searchResults.writeText(this.mainPanel);
		}

		//stFrame.repaint();

        /*if(Parameters.fixCaret)
        {
            Parameters.fixCaret = false;
            Parameters.fixCaretPositions();
        }*/
	}

	public void setRightText()
	{
		StringBuilder text = new StringBuilder();
		for(AbstractLayoutVertex v : this.mainPanel.highlighted)
			text.append(v.getRightPanelContent() + "\n");

		this.getRightArea().setText(text.toString());
	}

	public void buildCenter(ArrayList<ArrayList<Region>> layout, ArrayList<Double> dividerPositions)
	{
		horizontalSplitPane = new SplitPane();
		horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);
		this.setCenter(horizontalSplitPane);

		for(ArrayList<Region> column : layout) {
			if(column.size() == 1) {
				horizontalSplitPane.getItems().add(column.get(0));
			}
			else {
				SplitPane verticalSplitPane = new SplitPane();
				verticalSplitPane.setOrientation(Orientation.VERTICAL);
				for(Region r : column) {
					verticalSplitPane.getItems().add(r);
				}

				horizontalSplitPane.getItems().add(verticalSplitPane);
			}
		}

		for(int i = 0; i < layout.size() - 1; i++)
			horizontalSplitPane.setDividerPosition(i, dividerPositions.get(i));

		double[] positions = horizontalSplitPane.getDividerPositions();
		System.out.println("Divider positions:");
		for(double d : positions)
			System.out.println(d);
	}

	// We only have one pane per column, so this is unnecessary for now.
	/*public SplitPane createColumn(ArrayList<Region> panelList, ArrayList<Double> weights)
	{
		assert(panelList.size() > 0);
		SplitPane splitPane = new SplitPane();
		splitPane.setOrientation(Orientation.VERTICAL);

		for(Region p : panelList) {
			splitPane.getItems().add(p);
		}

		for(int i = 0; i < panelList.size() - 1; i++) {
			splitPane.getDividers().get(i).setPosition(weights.get(i));
		}

		return splitPane;

        /*for(int i = 0; i < splitPanes.size(); i++)
        {
            splitPanes.get(i).setResizeWeight(weights.get(i));
            splitPanes.get(i).resetToPreferredSizes();
        }

		//System.out.println("Finished constructing column! Panels = " + Integer.toString(panels.size())
			+ ", split panes = " + Integer.toString(splitPanes.size()));
	}*/

	public void makeLayout()
	{
		setSplitScreen();
		this.setBottom(this.buttonsFlowPane);
		this.setVisible(true);
	}

	public void makeSimpleLayout()
	{
		this.mainPanel = new VizPanel(this);
		this.setCenter(this.mainPanel);
	}

	public void makePanes() {
		this.mainPanel = new VizPanel(this);

		buttonsFlowPane = new FlowPane();
		buttonsFlowPane.setPadding(new Insets(5, 0, 5, 0));
		buttonsFlowPane.setVgap(5);
		buttonsFlowPane.setHgap(5);
		buttonsFlowPane.setPrefWrapLength(400);
		buttonsFlowPane.setMinHeight(50);

		GridPane controlPanel = new GridPane();
		controlPanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

		showEdge = new CheckBox("Show edges");
		showEdge.setSelected(true);
		showEdge.setOnAction
				(
						new EventHandler<ActionEvent>()
						{
							@Override
							public void handle(ActionEvent e) {
								// TODO: When this is checked off and then back on, the edges don't reappear.
								Parameters.edgeVisible = showEdge.isSelected();
								mainPanel.getPanelRoot().setEdgeVisibility(Parameters.edgeVisible);
								for (AbstractLayoutVertex v : mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
									v.setEdgeVisibility(showEdge.isSelected());
							}
						}
				);
		controlPanel.add(showEdge, 0, 0);
		buttonsFlowPane.getChildren().add(controlPanel);

		GridPane sizePanel = new GridPane();
		sizePanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

		buttonsFlowPane.getChildren().add(sizePanel);

		Button sizeMinus = new Button("-");
		sizeMinus.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							@Override
							public void handle(ActionEvent e) {
								ParallelTransition pt = new ParallelTransition();
								for (AbstractLayoutVertex v : mainPanel.getPanelRoot().getInnerGraph().getVertices().values()) {
									GUINode node = v.getGraphics();
									ScaleTransition st = new ScaleTransition(Duration.millis(300), node);
									st.setToX(node.getScaleX() * Parameters.boxFactor);
									st.setToY(node.getScaleY() * Parameters.boxFactor);
									pt.getChildren().add(st);
								}
								pt.play();
							}
						}
				);
		sizePanel.add(sizeMinus, 0, 0);

		Label sizeL = new Label("Box size");
		sizeL.setAlignment(Pos.CENTER);
		sizePanel.add(sizeL, 1, 0);

		Button sizePlus = new Button("+");
		sizePlus.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							@Override
							public void handle(ActionEvent e) {
								ParallelTransition pt = new ParallelTransition();
								for (AbstractLayoutVertex v : mainPanel.getPanelRoot().getInnerGraph().getVertices().values()) {
									GUINode node = v.getGraphics();
									ScaleTransition st = new ScaleTransition(Duration.millis(300), node);
									st.setToX(node.getScaleX() * 1.0 / Parameters.boxFactor);
									st.setToY(node.getScaleY() * 1.0 / Parameters.boxFactor);
									pt.getChildren().add(st);
								}
								pt.play();
							}
						}
				);
		sizePanel.add(sizePlus, 2, 0);

		GridPane xScalePanel = new GridPane();
		xScalePanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

		buttonsFlowPane.getChildren().add(xScalePanel);

		Button xScalePanelMinus = new Button("-");
		xScalePanelMinus.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							@Override
							public void handle(ActionEvent event) {
								StacFrame.this.mainPanel.decrementScaleXFactor();
								GUINode rootGraphics = StacFrame.this.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								StacFrame.this.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.resetPanelSize();

								StacFrame.this.mainPanel.drawNodes(null, StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.drawEdges(StacFrame.this.mainPanel.getPanelRoot());
							}
						}
				);
		xScalePanel.add(xScalePanelMinus, 0, 0);

		Label xScaleL = new Label("X scale");
		xScaleL.setAlignment(Pos.CENTER);
		xScalePanel.add(xScaleL, 1, 0);

		Button xScalePlus = new Button("+");
		xScalePlus.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							@Override
							public void handle(ActionEvent e) {
								StacFrame.this.mainPanel.incrementScaleXFactor();
								GUINode rootGraphics = StacFrame.this.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								StacFrame.this.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.resetPanelSize();

								StacFrame.this.mainPanel.drawNodes(null, StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.drawEdges(StacFrame.this.mainPanel.getPanelRoot());
							}
						}
				);
		xScalePanel.add(xScalePlus, 2, 0);


		GridPane yScalePanel = new GridPane();
		yScalePanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		buttonsFlowPane.getChildren().add(yScalePanel);

		Button yScalePanelMinus = new Button("-");
		yScalePanelMinus.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							@Override
							public void handle(ActionEvent event) {
								StacFrame.this.mainPanel.decrementScaleYFactor();
								GUINode rootGraphics = StacFrame.this.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								StacFrame.this.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.resetPanelSize();

								StacFrame.this.mainPanel.drawNodes(null, StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.drawEdges(StacFrame.this.mainPanel.getPanelRoot());
							}
						}
				);
		yScalePanel.add(yScalePanelMinus, 0, 0);

		Label yScaleL = new Label("Y scale");
		yScaleL.setAlignment(Pos.CENTER);
		yScalePanel.add(yScaleL, 1, 0);

		Button yScalePlus = new Button("+");
		yScalePlus.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							@Override
							public void handle(ActionEvent event) {
								StacFrame.this.mainPanel.incrementScaleYFactor();
								GUINode rootGraphics = StacFrame.this.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								StacFrame.this.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.resetPanelSize();

								StacFrame.this.mainPanel.drawNodes(null, StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.drawEdges(StacFrame.this.mainPanel.getPanelRoot());
							}
						}
				);
		yScalePanel.add(yScalePlus, 2, 0);


		FlowPane collapsePanel = new FlowPane();
		collapsePanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		buttonsFlowPane.getChildren().add(collapsePanel);

		final javafx.scene.paint.Color activeColor = javafx.scene.paint.Color.CYAN;
		final javafx.scene.paint.Color inactiveColor = javafx.scene.paint.Color.BLACK;

		final Button methodCollapse = new Button("M");
		methodCollapse.setTextFill(inactiveColor);
		methodCollapse.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							boolean methodExpanded = true;

							@Override
							public void handle(ActionEvent e) {
								methodExpanded = !methodExpanded;
								StacFrame.this.mainPanel.getPanelRoot().toggleNodesOfType(AbstractLayoutVertex.VertexType.METHOD,
										methodExpanded);

								if (methodCollapse.getTextFill() == activeColor) {
									methodCollapse.setTextFill(inactiveColor);
								} else {
									methodCollapse.setTextFill(activeColor);
								}

								GUINode rootGraphics = StacFrame.this.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								StacFrame.this.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.resetPanelSize();

								StacFrame.this.mainPanel.drawNodes(null, StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.drawEdges(StacFrame.this.mainPanel.getPanelRoot());
							}
						}
				);
		collapsePanel.getChildren().add(methodCollapse);

		final Button chainCollapse = new Button("C");
		chainCollapse.setTextFill(inactiveColor);
		chainCollapse.setOnAction
				(
						new EventHandler<ActionEvent>()
						{
							boolean chainExpanded = true;

							@Override
							public void handle(ActionEvent e) {
								chainExpanded = !chainExpanded;
								StacFrame.this.mainPanel.getPanelRoot()
										.toggleNodesOfType(AbstractLayoutVertex.VertexType.CHAIN, chainExpanded);
								if (chainCollapse.getTextFill() == activeColor) {
									chainCollapse.setTextFill(inactiveColor);
								} else {
									chainCollapse.setTextFill(activeColor);
								}

								GUINode rootGraphics = StacFrame.this.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								StacFrame.this.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.resetPanelSize();

								StacFrame.this.mainPanel.drawNodes(null, StacFrame.this.mainPanel.getPanelRoot());
								StacFrame.this.mainPanel.drawEdges(StacFrame.this.mainPanel.getPanelRoot());
							}
						}
				);
		collapsePanel.getChildren().add(chainCollapse);
		System.out.println("Menu sections: " + buttonsFlowPane.getChildren().size());


		// TODO: Set sizes to fill parent
		bytecodePanel = new BorderPane();
		Label leftLabel = new Label("Code");
		ScrollPane scrollLeft = new ScrollPane();
		this.bytecodeArea = new CodeArea();
		scrollLeft.setContent(this.bytecodeArea);
		bytecodePanel.setTop(leftLabel);
		bytecodePanel.setCenter(scrollLeft);

		rightPanel = new BorderPane();
		Label rightLabel = new Label("Description");
		ScrollPane rightScroll = new ScrollPane();
		this.rightArea = new TextArea();
		this.rightArea.setEditable(false);
		rightScroll.setContent(this.rightArea);
		rightPanel.setTop(rightLabel);
		rightPanel.setCenter(rightScroll);

		searchPanel = new BorderPane();
		Label searchL = new Label("Search Results");
		this.searchResults = new SearchResults();
		ScrollPane scrollS = new ScrollPane();
		scrollS.setContent(this.searchResults);
		searchPanel.setTop(searchL);
		searchPanel.setCenter(scrollS);
	}

	public void setSplitScreen()
	{
		makePanes();

		// Build data structure to hold panes
		// We need an ancestor of both panes and scroll panes; the lowest one is Region.
		ArrayList<ArrayList<Region>> layout = new ArrayList<ArrayList<Region>>();
		ArrayList<Double> layoutColumnWeights = new ArrayList<Double>();

		ArrayList<Region> left = new ArrayList<Region>();
		ArrayList<Region> center = new ArrayList<Region>();
		ArrayList<Region> right = new ArrayList<Region>();

		center.add(mainPanel);
		left.add(bytecodePanel);
		right.add(rightPanel);
		right.add(searchPanel);

		layout.add(left);
		layout.add(center);
		layout.add(right);
		layoutColumnWeights.add(0.2);
		layoutColumnWeights.add(0.7);

		buildCenter(layout, layoutColumnWeights);
	}

	// Clean up info from previous searches
	public void initSearch(searchType search)
	{
		this.mainPanel.resetHighlighted(null);
		String query = getSearchInput(search);

		if(search == searchType.ID)
			searchByID(query); // TODO: Fix inconsistency with panel root
		else if(search == searchType.INSTRUCTION)
			this.mainPanel.getPanelRoot().searchByInstruction(query, mainPanel);
		else if(search == searchType.METHOD)
			this.mainPanel.getPanelRoot().searchByMethod(query, mainPanel);

		this.repaintAll();
		/*Parameters.bytecodeArea.clear();
		Parameters.rightArea.setText("");*/
	}

	public String getSearchInput(searchType search)
	{
		String title = "";
		System.out.println("Search type: " + search);
		if(search == searchType.ID || search == searchType.ROOT_PATH)
		{
			title = "Enter node ID(s)";
		}
		else if(search == searchType.INSTRUCTION)
		{
			title = "Instruction contains ...";
		}
		else if(search == searchType.METHOD)
		{
			title = "Method name contains ...";
		}
		else if(search == searchType.OUT_OPEN || search == searchType.OUT_CLOSED || search == searchType.IN_OPEN
				|| search == searchType.IN_CLOSED)
		{
			title = "Enter node ID";
		}

		String input = "";
		if(search != searchType.ALL_LEAVES && search != searchType.ALL_SOURCES && search != searchType.TAG)
		{
			System.out.println("Showing dialog...");
			TextInputDialog dialog = new TextInputDialog();
			dialog.setHeaderText(title);
			dialog.showAndWait();
			input = dialog.getResult();
			if(input == null)
				return "";
			else
				input = input.trim();

			if(input.equals(""))
				return "";
		}

		return input;
    }

	public void searchByID(String input)
	{
		LayoutRootVertex panelRoot = this.mainPanel.getPanelRoot();
		StringTokenizer token = new StringTokenizer(input,", ");

		int id1, id2;
		String tok;

		while(token.hasMoreTokens()) {
			tok = token.nextToken();
			if (tok.trim().equalsIgnoreCase(""))
				continue;
			if (tok.indexOf('-') == -1) {
				id1 = Integer.parseInt(tok.trim());
				panelRoot.searchByID(id1, mainPanel);
			} else {
				id1 = Integer.parseInt(tok.substring(0, tok.indexOf('-')).trim());
				id2 = Integer.parseInt(tok.substring(tok.lastIndexOf('-') + 1).trim());
				panelRoot.searchByIDRange(id1, id2, mainPanel);
			}
		}
	}
}
