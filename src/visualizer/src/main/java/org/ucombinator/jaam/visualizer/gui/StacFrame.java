package org.ucombinator.jaam.visualizer.gui;

import com.sun.org.apache.regexp.internal.RE;
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

import java.io.File;
import java.util.ArrayList;

import java.awt.Font;
import java.util.StringTokenizer;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
import org.ucombinator.jaam.visualizer.layout.LayoutRootVertex;
import org.ucombinator.jaam.visualizer.main.Parameters;
import org.ucombinator.jaam.visualizer.main.TakeInput;

/**
 * JFrame showing a map
 * @author Jawaherul
 *
 */

public class StacFrame extends BorderPane
{
	private Menu menuFile, menuSearch, menuNavigation, menuCustomize, menuHelp;
	public VizPanel mainPanel; //TODO: Make private
	private SplitPane horizontalSplitPane;
	private FlowPane buttonsFlowPane;
	private MenuBar menuBar;
	private BorderPane searchPanel, bytecodePanel, rightPanel;
	public CheckBox showEdge;

	public enum searchType
	{
		ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
	}
	
	public StacFrame()
	{
		makeMenuBar();
		if (Parameters.debugMode)
			makeSimpleLayout();
		else
			makeLayout();

		this.setVisible(true);
	}
	
	public void makeMenuBar()
	{
		menuBar = new MenuBar();
		
		//File menu
		// TODO: Add hotkeys for commands
		menuFile = new Menu("File");
		menuBar.getMenus().add(menuFile);
		MenuItem loadMessages = new MenuItem("Load graph from message file");
		menuFile.getItems().add(loadMessages);
		loadMessages.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent t) {
				loadGraph(true);
			}
		});
		//loadMessages.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		/*final MenuItem loadJavaCode = new MenuItem("Load matching decompiled code");
		menuFile.add(loadJavaCode);
		loadJavaCode.addActionListener(
				new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						TakeInput.loadDecompiledCode();
					}
				}
		);*/
		
		//Search menu
		menuSearch = new Menu("Search");
		menuBar.getMenus().add(menuSearch);

		MenuItem searchByID = new MenuItem("by ID");
		menuSearch.getItems().add(searchByID);
		searchByID.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						initSearch();
						String query = getSearchInput(searchType.ID);
						searchByID(query);
						Parameters.repaintAll();
					}
				}
		);

		MenuItem searchByInst = new MenuItem("by Statement");
		menuSearch.getItems().add(searchByInst);
		searchByInst.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						initSearch();
						String query = getSearchInput(searchType.INSTRUCTION);
						StacFrame.this.mainPanel.getPanelRoot().searchByInstruction(query);
						Parameters.repaintAll();
					}
				}
		);

		MenuItem searchByMethod = new MenuItem("by Method");
		menuSearch.getItems().add(searchByMethod);
		searchByMethod.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						initSearch();
						String query = getSearchInput(searchType.METHOD);
						Parameters.stFrame.mainPanel.getPanelRoot().searchByMethod(query);
						Parameters.repaintAll();
					}
				}
		);

        /*MenuItem searchTags = new MenuItem("Allocation Tags");
        menuSearch.add(searchTags);
        searchTags.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent ev)
                {
                    searchAndHighlight(searchType.TAG);
                    Parameters.repaintAll();
                }
            }
        );
        searchTags.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));

		MenuItem searchLeaves = new MenuItem("All leaves");
		menuSearch.add(searchLeaves);
		searchLeaves.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.ALL_LEAVES);
						Parameters.repaintAll();
					}
				}
		);

		MenuItem searchSources = new MenuItem("All sources");
		menuSearch.add(searchSources);
		searchSources.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.ALL_SOURCES);
						Parameters.repaintAll();
					}
				}
		);

        // TODO: Re-implement this menu item.
		/*MenuItem clearAll = new MenuItem("Clear All");
		menuSearch.add(clearAll);
		clearAll.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						Main.graph.clearHighlights();
						StacFrame.this.initSearch();
						Parameters.repaintAll();
					}
				}
		);*/

		//Navigation menu
		menuNavigation = new Menu("Navigation");
		menuBar.getMenus().add(menuNavigation);

		MenuItem resetGraph = new MenuItem("Reset view");
		menuNavigation.getItems().add(resetGraph);
		resetGraph.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						// TODO: Write reset function
						//Main.graph.resetZoom();
						Parameters.repaintAll();
					}
				}
		);
		
		MenuItem collapse = new MenuItem("Collapse nodes");
		menuNavigation.getItems().add(collapse);
		collapse.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						// TODO: Write new collapse function
						//Main.graph.collapseOnce();
						//Main.graph.root.centerizeXCoordinate();
						Parameters.repaintAll();
					}
				}
		);

		MenuItem expand = new MenuItem("Expand nodes");
		menuNavigation.getItems().add(expand);
		expand.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						// TODO: Write new expand function
						//Main.graph.deCollapseOnce();
						//Main.graph.root.centerizeXCoordinate();
						Parameters.repaintAll();
					}
				}
		);
		
		//Customize display
		menuCustomize = new Menu("Customize");
		menuBar.getMenus().add(menuCustomize);
		
		MenuItem changeFont = new MenuItem("Change font size");
		menuCustomize.getItems().add(changeFont);
		changeFont.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						TextInputDialog dialog = new TextInputDialog();
						dialog.showAndWait();
						int newFontSize = Integer.parseInt(dialog.getResult());
						Parameters.font = new Font("Serif", Font.PLAIN, newFontSize);
						Parameters.jfxFont = new javafx.scene.text.Font("Serif", newFontSize);
						Parameters.bytecodeArea.resetFont();
						Parameters.rightArea.setFont(Parameters.jfxFont);
						Parameters.repaintAll();
					}
				}
		);
		
		// Help menu
		menuHelp = new Menu("Help");
		menuBar.getMenus().add(menuHelp);
		MenuItem help = new MenuItem("Shortcuts");
		menuHelp.getItems().add(help);
		help.setOnAction(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Alert alert = new Alert(Alert.AlertType.INFORMATION);
						alert.setTitle("Help");
						alert.setContentText("The following keyboard shortcuts are implemented.\n"
								+ "(Outdated, needs to be fixed)"
								+ "R: Reset zoom level to show entire graph \n"
								+ "C: Collapse all nodes by method \n"
								+ "E: Expand all nodes \n"
								+ "F: Change font size \n"
								+ "Left click: Select a vertex \n"
								+ "Left double click: Collapse/Uncollapse a node \n"
								+ "Shift-click: Select/de-select multiple vertices \n"
								+ "H: Open this list of shortcuts");
						alert.showAndWait();
					}
				}
		);
	}

	public void buildCenter(ArrayList<ArrayList<Region>> layout, ArrayList<Double> dividerPositions)
	{
		horizontalSplitPane = new SplitPane();
		horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);

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

		this.setCenter(horizontalSplitPane);
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
		this.setTop(this.menuBar);
		this.setBottom(this.buttonsFlowPane);
		this.setVisible(true);
	}

	public void makeSimpleLayout()
	{
		this.mainPanel = new VizPanel();
		this.setCenter(this.mainPanel);
	}

	public void makePanes() {
		this.mainPanel = new VizPanel();

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
								Parameters.stFrame.mainPanel.decrementScaleXFactor();
								GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								Parameters.stFrame.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.resetPanelSize();

								Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
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
								Parameters.stFrame.mainPanel.incrementScaleXFactor();
								GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								Parameters.stFrame.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.resetPanelSize();

								Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
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
								Parameters.stFrame.mainPanel.decrementScaleYFactor();
								GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								Parameters.stFrame.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.resetPanelSize();

								Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
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
								Parameters.stFrame.mainPanel.incrementScaleYFactor();
								GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								Parameters.stFrame.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.resetPanelSize();

								Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
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
								Parameters.stFrame.mainPanel.getPanelRoot().toggleNodesOfType(AbstractLayoutVertex.VertexType.METHOD,
										methodExpanded);

								if (methodCollapse.getTextFill() == activeColor) {
									methodCollapse.setTextFill(inactiveColor);
								} else {
									methodCollapse.setTextFill(activeColor);
								}

								GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								Parameters.stFrame.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.resetPanelSize();

								Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
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
								Parameters.stFrame.mainPanel.getPanelRoot()
										.toggleNodesOfType(AbstractLayoutVertex.VertexType.CHAIN, chainExpanded);
								if (chainCollapse.getTextFill() == activeColor) {
									chainCollapse.setTextFill(inactiveColor);
								} else {
									chainCollapse.setTextFill(activeColor);
								}

								GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
								((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
								Parameters.stFrame.mainPanel.getPanelRoot().reset();
								LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.resetPanelSize();

								Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
								Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
							}
						}
				);
		collapsePanel.getChildren().add(chainCollapse);
		System.out.println("Menu sections: " + buttonsFlowPane.getChildren().size());


		// TODO: Set sizes to fill parent
		bytecodePanel = new BorderPane();
		Label leftLabel = new Label("Code");
		ScrollPane scrollLeft = new ScrollPane();
		Parameters.bytecodeArea = new CodeArea();
		scrollLeft.setContent(Parameters.bytecodeArea);
		bytecodePanel.setTop(leftLabel);
		bytecodePanel.setCenter(scrollLeft);

		rightPanel = new BorderPane();
		Label rightLabel = new Label("Description");
		ScrollPane rightScroll = new ScrollPane();
		Parameters.rightArea = new TextArea();
		Parameters.rightArea.setEditable(false);
		rightScroll.setContent(Parameters.rightArea);
		rightPanel.setTop(rightLabel);
		rightPanel.setCenter(rightScroll);

		searchPanel = new BorderPane();
		Label searchL = new Label("Search Results");
		Parameters.searchResults = new SearchResults();
		ScrollPane scrollS = new ScrollPane();
		scrollS.setContent(Parameters.searchResults);
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
    
	public void loadGraph(boolean fromMessages)
	{
		File file = Parameters.openFile(false);
		if(file == null)
			return;

		TakeInput ti = new TakeInput();
		ti.run(file.getAbsolutePath(), fromMessages);
		Parameters.repaintAll();
	}

	// Clean up info from previous searches
	public void initSearch()
	{
		this.mainPanel.resetHighlighted(null);
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
		LayoutRootVertex panelRoot = Parameters.stFrame.mainPanel.getPanelRoot();
		StringTokenizer token = new StringTokenizer(input,", ");

		int id1, id2;
		String tok;

		while(token.hasMoreTokens()) {
			tok = token.nextToken();
			if (tok.trim().equalsIgnoreCase(""))
				continue;
			if (tok.indexOf('-') == -1) {
				id1 = Integer.parseInt(tok.trim());
				panelRoot.searchByID(id1);
			} else {
				id1 = Integer.parseInt(tok.substring(0, tok.indexOf('-')).trim());
				id2 = Integer.parseInt(tok.substring(tok.lastIndexOf('-') + 1).trim());
				panelRoot.searchByIDRange(id1, id2);
			}
		}
	}
}
