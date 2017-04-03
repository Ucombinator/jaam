package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.scene.control.ScrollPane;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.event.EventHandler;

import java.io.File;
import java.util.ArrayList;

import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Font;
import java.util.StringTokenizer;

import javax.swing.*;

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

public class StacFrame extends JFrame
{
	private JMenuBar menuBar;
	private JMenu menuFile, menuSearch, menuNavigation, menuCustomize, menuHelp;
	private int width, height;
	private ArrayList<JSplitPane> horizontalSplitPanes;
	public VizPanel mainPanel;
	private JPanel searchPanel;
	private JFXPanel menuPanel, bytecodePanel, rightPanel;
	public CheckBox showEdge;

	public enum searchType
	{
		ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
	}
	
	public StacFrame()
	{
		super("STAC Visualization");
		width = Parameters.width;
		height = Parameters.height;
		this.setLocation(0, 0);
		setSize(this.width, this.height);

		makeMenuBar();
		if (Parameters.debugMode)
			makeSimpleLayout();
		else
			makeLayout();

		this.setVisible(true);
	}
	
	public void makeMenuBar()
	{
		menuBar = new JMenuBar();
		
		//File menu
		menuFile = new JMenu("File");
		menuBar.add(menuFile);
		JMenuItem loadMessages = new JMenuItem("Load graph from message file");
		menuFile.add(loadMessages);
		loadMessages.addActionListener(
				new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						loadGraph(true);
					}
				}
		);
        loadMessages.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		/*final JMenuItem loadJavaCode = new JMenuItem("Load matching decompiled code");
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
		menuSearch = new JMenu("Search");
		menuBar.add(menuSearch);
		JMenuItem searchByID = new JMenuItem("by ID");
		menuSearch.add(searchByID);
		searchByID.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						initSearch();
						String query = getSearchInput(searchType.ID);
						searchByID(query);
						Parameters.repaintAll();
					}
				}
		);

		JMenuItem searchByInst = new JMenuItem("by Statement");
		menuSearch.add(searchByInst);
		searchByInst.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						initSearch();
						String query = getSearchInput(searchType.INSTRUCTION);
						Parameters.stFrame.mainPanel.getPanelRoot().searchByInstruction(query);
						Parameters.repaintAll();
					}
				}
		);

		JMenuItem searchByMethod = new JMenuItem("by Method");
		menuSearch.add(searchByMethod);
		searchByMethod.addActionListener
		(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent ev)
                {
					initSearch();
                	String query = getSearchInput(searchType.METHOD);
					Parameters.stFrame.mainPanel.getPanelRoot().searchByMethod(query);
                    Parameters.repaintAll();
                }
            }
		);

        /*JMenuItem searchTags = new JMenuItem("Allocation Tags");
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

		JMenuItem searchLeaves = new JMenuItem("All leaves");
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

		JMenuItem searchSources = new JMenuItem("All sources");
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
		/*JMenuItem clearAll = new JMenuItem("Clear All");
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
		menuNavigation = new JMenu("Navigation");
		menuBar.add(menuNavigation);

		JMenuItem resetGraph = new JMenuItem("Reset view");
		menuNavigation.add(resetGraph);
		resetGraph.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						// TODO: Write reset function
						//Main.graph.resetZoom();
						Parameters.repaintAll();
					}
				}
		);
		resetGraph.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		
		JMenuItem collapse = new JMenuItem("Collapse nodes");
		menuNavigation.add(collapse);
		collapse.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						// TODO: Write new collapse function
						//Main.graph.collapseOnce();
                        //Main.graph.root.centerizeXCoordinate();
						Parameters.repaintAll();
					}
				}
		);
		collapse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

		JMenuItem expand = new JMenuItem("Expand nodes");
		menuNavigation.add(expand);
		expand.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						// TODO: Write new expand function
						//Main.graph.deCollapseOnce();
                        //Main.graph.root.centerizeXCoordinate();
						Parameters.repaintAll();
					}
				}
		);
		expand.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0));
		
		//Customize display
		menuCustomize = new JMenu("Customize");
		menuBar.add(menuCustomize);
		
		JMenuItem changeFont = new JMenuItem("Change font size");
		menuCustomize.add(changeFont);
		changeFont.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						String newFontSizeStr = JOptionPane.showInputDialog(null,
								"The current font size is: " + Parameters.font.getSize()
										+ ". Please enter a new font size");
						int newFontSize = Integer.parseInt(newFontSizeStr);
						Parameters.font = new Font("Serif", Font.PLAIN, newFontSize);
						Parameters.jfxFont = new javafx.scene.text.Font("Serif", newFontSize);
						Parameters.bytecodeArea.resetFont();
						Parameters.rightArea.setFont(Parameters.jfxFont);
						Parameters.repaintAll();
					}
				}
		);
		changeFont.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
		
		// Help menu
		menuHelp = new JMenu("Help");
		menuBar.add(menuHelp);
		JMenuItem help = new JMenuItem("Shortcuts");
		menuHelp.add(help);
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev)
			{
				JOptionPane.showMessageDialog(getParent(),
						"The following keyboard shortcuts are implemented.\n"
								+ "(Outdated, needs to be fixed)"
								+ "R: Reset zoom level to show entire graph \n"
								+ "C: Collapse all nodes by method \n"
								+ "E: Expand all nodes \n"
								+ "F: Change font size \n"
								+ "Left click: Select a vertex \n"
								+ "Left double click: Collapse/Uncollapse a node \n"
								+ "Shift-click: Select/de-select multiple vertices \n"
								+ "H: Open this list of shortcuts"
						);
					}
				}
		);
		help.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));

		this.setJMenuBar(menuBar);
	}

	public void buildWindow(ArrayList<ArrayList<JComponent>> layout, ArrayList<ArrayList<Double>> layoutRowWeights,
							ArrayList<Double> layoutColumnWeights)
	{
		ArrayList<GUIPanelColumn> columns = new ArrayList<GUIPanelColumn>();
		for(int i = 0; i < layout.size(); i++)
		{
			ArrayList<JComponent> panelList = layout.get(i);
			columns.add(new GUIPanelColumn(panelList, layoutRowWeights.get(i)));
		}

		// Connect columns with horizontal split panes
		this.horizontalSplitPanes = new ArrayList<JSplitPane>();
		for(int i = 0; i < layout.size() - 1; i++)
		{
			JSplitPane nextSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			nextSplit.setOneTouchExpandable(true);
			if (i == 0)
				nextSplit.setLeftComponent(columns.get(0).getComponentLink());
			else
				nextSplit.setLeftComponent(horizontalSplitPanes.get(i - 1));

			nextSplit.setRightComponent(columns.get(i + 1).getComponentLink());
			horizontalSplitPanes.add(nextSplit);
		}

		for(int i = 0; i < horizontalSplitPanes.size(); i++)
		{
			horizontalSplitPanes.get(i).setResizeWeight(layoutColumnWeights.get(i));
			horizontalSplitPanes.get(i).resetToPreferredSizes();
		}

		// Add to GUI
		if(layout.size() == 1)
			this.getContentPane().add(columns.get(0).getComponentLink(), BorderLayout.CENTER);
		else
			this.getContentPane().add(horizontalSplitPanes.get(horizontalSplitPanes.size() - 1), BorderLayout.CENTER);
	}

	public void makeLayout()
	{
		//centerPanel and mainPanel
		this.setLayout(new BorderLayout());
		setSplitScreen();
		this.addKeyboard(mainPanel);
		this.getContentPane().add(this.menuPanel, BorderLayout.NORTH);

		this.setVisible(true);
		this.repaint();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
	}

	public void makeSimpleLayout()
	{
		this.mainPanel = new VizPanel();
		this.setLayout(new BorderLayout());
		this.getContentPane().add(this.mainPanel);
	}

	public void makeJFXPanels() {
		// Why is this still not visible?
		menuPanel = new JFXPanel();
		/*BorderPane menuPane = new BorderPane();
		menuPane.setTop(new Label("Menu test"));
		menuPanel.setScene(new Scene(menuPane));*/

		FlowPane menuFlowPane = new FlowPane();
		menuFlowPane.setPadding(new Insets(5, 0, 5, 0));
		menuFlowPane.setVgap(5);
		menuFlowPane.setHgap(5);
		menuFlowPane.setPrefWrapLength(400);
		menuFlowPane.setMinHeight(50);

		menuPanel.setScene(new Scene(menuFlowPane));

		GridPane controlPanel = new GridPane();
		controlPanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

		showEdge = new CheckBox("Show edges");
		showEdge.setSelected(true);
		showEdge.setOnAction
				(
						new EventHandler<javafx.event.ActionEvent>()
						{
							@Override
							public void handle(javafx.event.ActionEvent e) {
								Platform.runLater(new Runnable() {

									@Override
									public void run() {
										// TODO: When this is checked off and then back on, the edges don't reappear.
										Parameters.edgeVisible = showEdge.isSelected();
										mainPanel.getPanelRoot().setEdgeVisibility(Parameters.edgeVisible);
										for (AbstractLayoutVertex v : mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
											v.setEdgeVisibility(showEdge.isSelected());
									}
								});
							}
						}
				);
		controlPanel.add(showEdge, 0, 0);
		menuFlowPane.getChildren().add(controlPanel);

		GridPane sizePanel = new GridPane();
		sizePanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

		menuFlowPane.getChildren().add(sizePanel);

		Button sizeMinus = new Button("-");
		sizeMinus.setOnAction
				(
						new EventHandler<javafx.event.ActionEvent>()

						{
							@Override
							public void handle(javafx.event.ActionEvent e) {
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
						new EventHandler<javafx.event.ActionEvent>()

						{
							@Override
							public void handle(javafx.event.ActionEvent e) {
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

		menuFlowPane.getChildren().add(xScalePanel);

		Button xScalePanelMinus = new Button("-");
		xScalePanelMinus.setOnAction
				(
						new EventHandler<javafx.event.ActionEvent>()

						{
							@Override
							public void handle(javafx.event.ActionEvent event) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										Parameters.stFrame.mainPanel.decrementScaleXFactor();
										GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
										((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
										Parameters.stFrame.mainPanel.getPanelRoot().reset();
										LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.resetPanelSize();

										Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
									}
								});
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
						new EventHandler<javafx.event.ActionEvent>()

						{
							@Override
							public void handle(javafx.event.ActionEvent e) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										Parameters.stFrame.mainPanel.incrementScaleXFactor();
										GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
										((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
										Parameters.stFrame.mainPanel.getPanelRoot().reset();
										LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.resetPanelSize();

										Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
									}
								});
							}
						}
				);
		xScalePanel.add(xScalePlus, 2, 0);


		GridPane yScalePanel = new GridPane();
		yScalePanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		menuFlowPane.getChildren().add(yScalePanel);

		Button yScalePanelMinus = new Button("-");
		yScalePanelMinus.setOnAction
				(
						new EventHandler<javafx.event.ActionEvent>()

						{
							@Override
							public void handle(javafx.event.ActionEvent event) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										Parameters.stFrame.mainPanel.decrementScaleYFactor();
										GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
										((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
										Parameters.stFrame.mainPanel.getPanelRoot().reset();
										LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.resetPanelSize();

										Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
									}
								});
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
						new EventHandler<javafx.event.ActionEvent>()

						{
							@Override
							public void handle(javafx.event.ActionEvent event) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										Parameters.stFrame.mainPanel.incrementScaleYFactor();
										GUINode rootGraphics = Parameters.stFrame.mainPanel.getPanelRoot().getGraphics();
										((Group) rootGraphics.getParent()).getChildren().remove(rootGraphics);
										Parameters.stFrame.mainPanel.getPanelRoot().reset();
										LayoutAlgorithm.layout(Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.resetPanelSize();

										Parameters.stFrame.mainPanel.drawNodes(null, Parameters.stFrame.mainPanel.getPanelRoot());
										Parameters.stFrame.mainPanel.drawEdges(Parameters.stFrame.mainPanel.getPanelRoot());
									}
								});
							}
						}
				);
		yScalePanel.add(yScalePlus, 2, 0);


		FlowPane collapsePanel = new FlowPane();
		collapsePanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		menuFlowPane.getChildren().add(collapsePanel);

		final javafx.scene.paint.Color activeColor = javafx.scene.paint.Color.CYAN;
		final javafx.scene.paint.Color inactiveColor = javafx.scene.paint.Color.BLACK;

		final Button methodCollapse = new Button("M");
		methodCollapse.setTextFill(inactiveColor);
		methodCollapse.setOnAction
				(
						new EventHandler<javafx.event.ActionEvent>()

						{
							boolean methodExpanded = true;

							@Override
							public void handle(javafx.event.ActionEvent e) {
								methodExpanded = !methodExpanded;
								Parameters.stFrame.mainPanel.getPanelRoot().toggleNodesOfType(AbstractLayoutVertex.VertexType.METHOD,
										methodExpanded);

								Platform.runLater(new Runnable() {
									@Override
									public void run() {
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
								});
							}
						}
				);
		collapsePanel.getChildren().add(methodCollapse);

		final Button chainCollapse = new Button("C");
		chainCollapse.setTextFill(inactiveColor);
		chainCollapse.setOnAction
				(
						new EventHandler<javafx.event.ActionEvent>()
						{
							boolean chainExpanded = true;

							@Override
							public void handle(javafx.event.ActionEvent e) {
								chainExpanded = !chainExpanded;
								Parameters.stFrame.mainPanel.getPanelRoot()
										.toggleNodesOfType(AbstractLayoutVertex.VertexType.CHAIN, chainExpanded);
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
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
								});
							}
						}
				);
		collapsePanel.getChildren().add(chainCollapse);
		System.out.println("Menu sections: " + menuFlowPane.getChildren().size());


		// TODO: Set sizes to fill parent
		bytecodePanel = new JFXPanel();
		BorderPane leftRoot = new BorderPane();
		Label leftLabel = new Label("Code");
		ScrollPane scrollLeft = new ScrollPane();
		Parameters.bytecodeArea = new CodeArea();
		scrollLeft.setContent(Parameters.bytecodeArea);
		leftRoot.setTop(leftLabel);
		leftRoot.setCenter(scrollLeft);
		bytecodePanel.setScene(new Scene(leftRoot));

		rightPanel = new JFXPanel();
		BorderPane rightRoot = new BorderPane();
		Label rightLabel = new Label("Description");
		ScrollPane rightScroll = new ScrollPane();
		Parameters.rightArea = new TextArea();
		Parameters.rightArea.setEditable(false);
		rightScroll.setContent(Parameters.rightArea);
		rightRoot.setTop(rightLabel);
		rightRoot.setCenter(rightScroll);
		rightPanel.setScene(new Scene(rightRoot));
	}

	public void setSplitScreen()
	{
		// Make these panels on JavaFX thread instead of Swing thread
		this.mainPanel = new VizPanel();
		Platform.runLater(new Runnable() {
			@Override
			public void run()
			{
				makeJFXPanels();
			}
		});

        searchPanel = new JPanel();
        JLabel searchL = new JLabel("Search Results", JLabel.CENTER);
        Parameters.searchResults = new SearchResults();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(searchL,BorderLayout.NORTH);
        JScrollPane scrollS = new JScrollPane (Parameters.searchResults, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        searchPanel.add(scrollS, BorderLayout.CENTER);
        searchPanel.setFont(Parameters.font);

		// Build data structure to hold panels
		ArrayList<ArrayList<JComponent>> layout = new ArrayList<ArrayList<JComponent>>();
		ArrayList<ArrayList<Double>> layoutRowWeights = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> layoutColumnWeights = new ArrayList<Double>();

		ArrayList<JComponent> left = new ArrayList<JComponent>();
		left.add(bytecodePanel);

		ArrayList<Double> leftWeights = new ArrayList<Double>();
		layoutRowWeights.add(leftWeights);

		ArrayList<JComponent> center = new ArrayList<JComponent>();
		center.add(mainPanel);
		ArrayList<Double> centerWeights = new ArrayList<Double>();
		layoutRowWeights.add(centerWeights);

		ArrayList<JComponent> right = new ArrayList<JComponent>();
		right.add(rightPanel);
		right.add(searchPanel);
		ArrayList<Double> rightWeights = new ArrayList<Double>();
		rightWeights.add(0.6);
		layoutRowWeights.add(rightWeights);

		layout.add(left);
		layoutColumnWeights.add(0.2);
		layout.add(center);
		layoutColumnWeights.add(0.9);
		layout.add(right);
		buildWindow(layout, layoutRowWeights, layoutColumnWeights);
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
			input = JOptionPane.showInputDialog(null, title);
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

	public void addKeyboard(JFXPanel viz)
	{
		viz.addKeyListener(new KeyListener()
		{
			public void keyTyped(KeyEvent ev){}

			public void keyPressed(KeyEvent ev)
			{
				int code = ev.getKeyCode();
				if(code == 'L')
				{
					String lim = JOptionPane.showInputDialog(null,
							"Set limit on the number of vertices:");
					Parameters.limitV = Long.parseLong(lim);
					Parameters.repaintAll();
				}
			}

			public void keyReleased(KeyEvent ev) {}
		});
		
		viz.setFocusable(true);
		viz.requestFocusInWindow();
	}
}
