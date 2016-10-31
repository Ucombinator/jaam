
import java.io.File;
import java.util.ArrayList;

/*import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.text.DefaultCaret;*/

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.*;


/**
 * JFrame showing a map
 * @author Jawaherul
 *
 */

public class StacFrame extends Stage
{
	private Scene rootScene;
	private BorderPane rootPane;
	private MenuBar menuBar;
	private Menu menuFile, menuSearch, menuNavigation, menuCustomize, menuHelp;
	private int width, height;
	public VizPane vizPanel, contextPanel;
	private Pane menuPanel, leftPanel, rightPanel, searchPanel;
	public CheckBox showContext, showEdge;
    public SearchField searchF;
	private boolean context = false, mouseDrag = false;
	
	public enum searchType
	{
		ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
	}
	
	public StacFrame()
	{
		this.setTitle("STAC Visualization");
		this.width = Parameters.width;
		this.height = Parameters.height;

		this.rootPane = new BorderPane();
		this.rootScene = new Scene(this.rootPane, this.width, this.height);
		this.setScene(rootScene);

		makeMenuBar();
		makeLayout();
		this.show();
	}
	
	public void makeMenuBar()
	{
		menuBar = new MenuBar();
		
		//File menu
		menuFile = new Menu("File");
		menuBar.getMenus().add(menuFile);
		MenuItem loadMessages = new MenuItem("Load graph from message file");
		loadMessages.setOnAction(
				new EventHandler<ActionEvent>()
				{
					@Override
					public void handle(ActionEvent e)
					{
						loadGraph(true);
					}
				}
		);
		// TODO: Set up keyboard shortcuts
		//loadMessages.acceleratorProperty(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		//loadMessages.setAccelerator();
		menuFile.getItems().addAll(loadMessages);

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
		menuSearch = new Menu("Search");
		menuBar.getMenus().add(menuSearch);
		MenuItem searchByID = new MenuItem("by ID");
		menuSearch.getItems().add(searchByID);
		searchByID.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.ID);
						Parameters.repaintAll();
					}
				}
		);

		MenuItem searchByInst = new MenuItem("by Statement");
		menuSearch.getItems().add(searchByInst);
		searchByInst.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.INSTRUCTION);
						Parameters.repaintAll();
					}
				}
		);

		MenuItem searchByMethod = new MenuItem("by Method");
		menuSearch.getItems().add(searchByMethod);
		searchByMethod.setOnAction
		(
            new EventHandler<ActionEvent>()
            {
                public void handle(ActionEvent ev)
                {
                    searchAndHighlight(searchType.METHOD);
                    Parameters.repaintAll();
                }
            }
		);

        MenuItem searchTags = new MenuItem("Allocation Tags");
		menuSearch.getItems().add(searchTags);
        searchTags.setOnAction
        (
            new EventHandler<ActionEvent>()
            {
                public void handle(ActionEvent ev)
                {
                    searchAndHighlight(searchType.TAG);
                    Parameters.repaintAll();
                }
            }
        );
        //searchTags.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
        
        
		MenuItem searchLeaves = new MenuItem("All leaves");
		menuSearch.getItems().add(searchLeaves);
		searchLeaves.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.ALL_LEAVES);
						Parameters.repaintAll();
					}
				}
		);

		
		MenuItem searchSources = new MenuItem("All sources");
		menuSearch.getItems().add(searchSources);
		searchSources.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.ALL_SOURCES);
						Parameters.repaintAll();
					}
				}
		);

		
		MenuItem searchOutgoing = new MenuItem("Outgoing neighborhood (open)");
		menuSearch.getItems().add(searchOutgoing);
		searchOutgoing.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.OUT_OPEN);
						Parameters.repaintAll();
					}
				}
		);

		
		MenuItem searchOutgoingClosed = new MenuItem("Outgoing neighborhood (closed)");
		menuSearch.getItems().add(searchOutgoingClosed);
		searchOutgoingClosed.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.OUT_CLOSED);
						Parameters.repaintAll();
					}
				}
		);

		
		MenuItem searchIncoming = new MenuItem("Incoming neighborhood (open)");
		menuSearch.getItems().add(searchIncoming);
		searchIncoming.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.IN_OPEN);
						Parameters.repaintAll();
					}
				}
		);

		
		MenuItem searchIncomingClosed = new MenuItem("Incoming neighborhood (closed)");
		menuSearch.getItems().add(searchIncomingClosed);
		searchIncomingClosed.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.IN_CLOSED);
						Parameters.repaintAll();
					}
				}
		);

		MenuItem searchPathToRoot = new MenuItem("Path to Root");
		menuSearch.getItems().add(searchPathToRoot);
		searchPathToRoot.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						searchAndHighlight(searchType.ROOT_PATH);
						Parameters.repaintAll();
					}
				}
		);

		
		MenuItem clearAll = new MenuItem("Clear All");
		menuSearch.getItems().add(clearAll);
		clearAll.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.clearHighlights();
						Parameters.leftArea.clear();
						Parameters.repaintAll();
					}
				}
		);
		

		//Navigation menu
		menuNavigation = new Menu("Navigation");
		menuBar.getMenus().add(menuNavigation);
		MenuItem zoomIn = new MenuItem("Zoom in        (Mouse wheel up)");
		menuNavigation.getItems().add(zoomIn);
		zoomIn.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.increaseZoom(Parameters.zoomFactor, -1, -1);
						Parameters.repaintAll();
					}
				}
		);
    
		MenuItem zoomOut = new MenuItem("Zoom out      (Mouse wheel down)");
		menuNavigation.getItems().add(zoomOut);
		zoomOut.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.increaseZoom(1/Parameters.zoomFactor, -1, -1);
						Parameters.repaintAll();
					}
				}
		);
		
        MenuItem rearrange = new MenuItem("Rearrange graph");
        menuNavigation.getItems().add(rearrange);
        rearrange.setOnAction
        (
         	new EventHandler<ActionEvent>()
         	{
				public void handle(ActionEvent ev)
				{
					Main.graph.root.rearrangeByWidth();
					// Main.graph.root.rearrangeByLoopHeight();
					Main.graph.root.centerizeXCoordinate();
					Parameters.repaintAll();
				}
        	}
		);
        //rearrange.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));

        
		MenuItem resetGraph = new MenuItem("Reset view");
		menuNavigation.getItems().add(resetGraph);
		resetGraph.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.resetZoom();
						Parameters.repaintAll();
					}
				}
		);
		//resetGraph.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		
		MenuItem collapse = new MenuItem("Collapse nodes");
		menuNavigation.getItems().add(collapse);
		collapse.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.collapseOnce();
                        Main.graph.root.centerizeXCoordinate();
						Parameters.repaintAll();
					}
				}
		);
		//collapse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

		MenuItem decollapse = new MenuItem("Expand nodes");
		menuNavigation.getItems().add(decollapse);
		decollapse.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.deCollapseOnce();
                        Main.graph.root.centerizeXCoordinate();
						Parameters.repaintAll();
					}
				}
		);
		//decollapse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0));
		
		MenuItem previous = new MenuItem("Previous view");
		menuNavigation.getItems().add(previous);
		previous.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.loadPreviousView();
						Parameters.repaintAll();
					}
				}
		);
		//previous.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));

        MenuItem next = new MenuItem("Next view");
		menuNavigation.getItems().add(next);
		next.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.restoreNewView();
						Parameters.repaintAll();
					}
				}
		);
		//next.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
		
		MenuItem panUp = new MenuItem("Pan up");
		menuNavigation.getItems().add(panUp);
		panUp.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.shiftView(0, -1);
						Parameters.repaintAll();
					}
				}
		);
		//panUp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
		
		MenuItem panDown = new MenuItem("Pan down");
		menuNavigation.getItems().add(panDown);
		panDown.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.shiftView(0, 1);
						Parameters.repaintAll();
					}
				}
		);
		//panDown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
		
		MenuItem panLeft = new MenuItem("Pan left");
		menuNavigation.getItems().add(panLeft);
		panLeft.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.shiftView(-1, 0);
						Parameters.repaintAll();
					}
				}
		);
		//panLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
		
		MenuItem panRight = new MenuItem("Pan right");
		menuNavigation.getItems().add(panRight);
		panRight.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						Main.graph.shiftView(1, 0);
						Parameters.repaintAll();
					}
				}
		);
		//panRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));

		//Customize display
		menuCustomize = new Menu("Customize");
		menuBar.getMenus().add(menuCustomize);
		
		MenuItem changeFont = new MenuItem("Change font size");
		menuCustomize.getItems().add(changeFont);
		changeFont.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						// TODO: Find JOPtionPane replacement
						//String newFontSize = JOptionPane.showInputDialog(null, "The current font size is: " + Parameters.font.getSize() + ". Please enter a new font size");
						//Parameters.font = new Font("Serif", Font.PLAIN, Integer.parseInt(newFontSize));
						Parameters.leftArea.setFont(Parameters.font);
						Parameters.rightArea.setFont(Parameters.font);
						Parameters.repaintAll();
					}
				}
		);
		//changeFont.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
		
		//Help menu
		menuHelp = new Menu("Help");
		menuBar.getMenus().add(menuHelp);
		MenuItem help = new MenuItem("Shortcuts");
		menuHelp.getItems().add(help);
		help.setOnAction
		(
				new EventHandler<ActionEvent>()
				{
					public void handle(ActionEvent ev)
					{
						/*JOptionPane.showMessageDialog(getParent(),
								"The following keyboard shortcuts are implemented.\n"
								+ "R: Reset zoom level to show entire graph \n"
								+ "C: Collapse all nodes by method \n"
								+ "D: Uncollapse all nodes \n"
								+ "P: Return to previous view \n"
								+ "N: Continue from previous to next view \n"
								+ "F: Change font size \n"
								+ "Arrows: Pan up, down, left or right \n"
								+ "Left-Click: Uncollapse a node \n"
								+ "Right-Click: Collapse all nodes of a single method \n"
								+ "Shift-click: Select/de-select multiple vertices \n"
								+ "CTRL + <Digit>: Hotkey view with all vertices currently on screen to <Digit> \n"
								+ "<Digit>: Go to hotkeyed view \n"
								+ "H: Open this list of shortcuts"
						);*/
					}
				}
		);
		//help.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
	}

	public void buildWindow(ArrayList<ArrayList<Pane>> layout, ArrayList<ArrayList<Double>> layoutRowWeights, ArrayList<Double> layoutColumnWeights)
	{
		// Construct columns
		ArrayList<GUIPanelColumn> columns = new ArrayList<GUIPanelColumn>();
		for(int i = 0; i < layout.size(); i++)
		{
			ArrayList<Pane> panelList = layout.get(i);
			columns.add(new GUIPanelColumn(panelList, layoutRowWeights.get(i)));
		}

		// Connect columns with horizontal split panes
		ArrayList<SplitPane> horizontalSplits = new ArrayList<SplitPane>();
		for(int i = 0; i < layout.size() - 1; i++) {
			SplitPane nextSplit = new SplitPane();
			nextSplit.setOrientation(Orientation.HORIZONTAL);
			if (i == 0)
				nextSplit.getItems().add(columns.get(0).getComponentLink());
			else
				nextSplit.getItems().add(horizontalSplits.get(i - 1));

			nextSplit.getItems().add(columns.get(i + 1).getComponentLink());
			horizontalSplits.add(nextSplit);
		}

		// TODO: Set initial sizes
		/*for(int i = 0; i < horizontalSplits.size(); i++)
		{
			horizontalSplits.get(i).setResizeWeight(layoutColumnWeights.get(i));
			horizontalSplits.get(i).resetToPreferredSizes();
		}*/

		// Add to GUI
		if(layout.size() == 1)
			this.rootPane.setCenter(columns.get(0).getComponentLink());
		else
			this.rootPane.setCenter(horizontalSplits.get(horizontalSplits.size() - 1));
	}

	public void makeLayout()
	{

		//centerPanel and vizPanel
		setSplitScreen();
		this.addMouseToViz();
		this.addKeyboard(vizPane);
		this.addMouseToContext();
		this.addKeyboard(contextPane);
		

		// TODO: Set etched border?
        BorderPane topPane = new BorderPane();
        this.rootPane.setTop(topPane);

		// TODO: Set etched border?
		this.menuPanel = new FlowPane();
        topPane.setCenter(this.menuPanel);

		GridPane contextPanel = new GridPane();
		this.menuPanel.getChildren().add(contextPanel);
		
		showEdge = new CheckBox("Show Edge");
		showEdge.setSelected(true);
		showEdge.setOnAction
		(
			new EventHandler<ActionEvent>()
			{
				public void handle(ActionEvent e)
				{
					vizPanel.showEdge = showEdge.isSelected();					
					StacFrame.this.repaint();
				}
			}
		);
		contextPanel.getChildren().add(showEdge);

		// TODO: Set etched border?
		GridPane sizePanel = new GridPane();
		this.menuPanel.getChildren().add(sizePanel);
		
		Button sizeMinus = new Button("-");
		sizeMinus.setOnAction
		(
			new EventHandler<ActionEvent>()
			{
				public void handle(ActionEvent e)
				{
					vizPanel.boxSize *= Parameters.boxFactor;
					Parameters.repaintAll();
				}
			}
		);
		sizePanel.getChildren().add(sizeMinus);
		
		Label sizeL = new Label("box size");
		sizeL.setAlignment(Pos.CENTER);
		sizePanel.getChildren().add(sizeL);
		
		Button sizePlus = new Button("+");
		sizePlus.setOnAction
		(
			new EventHandler<ActionEvent>()
			{
				public void handle(ActionEvent e)
				{
					vizPanel.boxSize /= Parameters.boxFactor;
					Parameters.repaintAll();
				}
			}
		);
		sizePanel.getChildren().add(sizePlus);
        
        
        /// bottom panel
        
        this.searchF = new SearchField();
        this.rootPane.setBottom(searchF);

		this.rootPane.getChildren().add(menuBar);
		this.repaint();
	}
	
	public void setSplitScreen()
	{
		// Declare each panel
		leftPanel = new JPanel();
		rightPanel = new JPanel();
        searchPanel = new JPanel();
		this.vizPanel = new VizPanel(this, false);
		this.contextPanel = new VizPanel(this,true);

		// Build each panel
		JLabel leftL = new JLabel("Context", JLabel.CENTER);
		Parameters.leftArea = new CodeArea();
		leftPanel.setLayout(new BorderLayout());
		leftPanel.add(leftL,BorderLayout.NORTH);
		JScrollPane scrollL = new JScrollPane (Parameters.leftArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		leftPanel.add(scrollL, BorderLayout.CENTER);
		leftPanel.setFont(Parameters.font);
		
		JLabel rightL = new JLabel("Description", JLabel.CENTER);
		Parameters.rightArea = new JTextArea();
		Parameters.rightArea.setEditable(false);
        ((DefaultCaret)Parameters.rightArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(rightL, BorderLayout.NORTH);
		JScrollPane scrollR = new JScrollPane (Parameters.rightArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		rightPanel.add(scrollR, BorderLayout.CENTER);
		rightPanel.setFont(Parameters.font);
		
        JLabel searchL = new JLabel("Search Results", JLabel.CENTER);
        Parameters.searchArea = new SearchArea();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(searchL,BorderLayout.NORTH);
        JScrollPane scrollS = new JScrollPane (Parameters.searchArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        searchPanel.add(scrollS, BorderLayout.CENTER);
        searchPanel.setFont(Parameters.font);

		// Build data structure to hold panels
		ArrayList<ArrayList<JPanel>> layout = new ArrayList<ArrayList<JPanel>>();
		ArrayList<ArrayList<Double>> layoutRowWeights = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> layoutColumnWeights = new ArrayList<Double>();

		ArrayList<JPanel> left = new ArrayList<JPanel>();
		left.add(leftPanel);
		left.add(contextPanel);

		ArrayList<Double> leftWeights = new ArrayList<Double>();
		leftWeights.add(0.6);
		layoutRowWeights.add(leftWeights);

		ArrayList<JPanel> center = new ArrayList<JPanel>();
		center.add(vizPanel);
		ArrayList<Double> centerWeights = new ArrayList<Double>();
		layoutRowWeights.add(centerWeights);

		ArrayList<JPanel> right = new ArrayList<JPanel>();
		right.add(rightPanel);
		right.add(searchPanel);
		ArrayList<Double> rightWeights = new ArrayList<Double>();
		rightWeights.add(0.6);
		layoutRowWeights.add(rightWeights);

		layout.add(left);
		layoutColumnWeights.add(0.2);
		layout.add(center);
		layoutColumnWeights.add(0.75);
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
	
	public void searchAndHighlight(searchType search)
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
		else if(search == searchType.OUT_OPEN || search == searchType.OUT_CLOSED || search == searchType.IN_OPEN || search == searchType.IN_CLOSED)
		{
			title = "Enter node ID";
		}

		String input = "";
		if(search != searchType.ALL_LEAVES && search != searchType.ALL_SOURCES && search != searchType.TAG)
		{
			input = JOptionPane.showInputDialog(null, title);
			if(input == null)
				return;
			else
				input = input.trim();

			if(input.equals(""))
				return;
		}

		Main.graph.searchNodes(search, input);
		
		Parameters.vertexHighlight = true;

		Parameters.highlightOutgoing = search != searchType.OUT_OPEN;
		Parameters.highlightIncoming = search != searchType.IN_OPEN;
    
    }
	
	public boolean isGraphLoaded()
	{
		return (Main.graph != null);
	}
	
	public void addMouseToViz()
	{
		vizPanel.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent m)
				{
                    vizPanel.requestFocusInWindow();
					if(!isGraphLoaded())
						return;
					
					context = false;
					double x = vizPanel.getRelativeFracFromAbsolutePixelX(getRelativeXPixels(m))*Main.graph.getWidth();
					double y = vizPanel.getRelativeFracFromAbsolutePixelY(getRelativeYPixels(m))*Main.graph.getHeight();
					
					if(System.currentTimeMillis() - Parameters.mouseLastTime > Parameters.mouseInterval)
					{
						Parameters.mouseLastTime = System.currentTimeMillis();
						
						if(SwingUtilities.isLeftMouseButton(m) && Main.graph != null)
						{	
							if(m.isShiftDown())
							{
								AbstractVertex ver = Main.graph.getVertexNearestCoordinate(x, y);
								if(ver != null)
								{
//									if(ver.isHighlighted())
									if(ver.isSelected())
									{
//										ver.clearAllHighlights();
										ver.clearAllSelect();
										Main.graph.redoCycleHighlights();
									}
									else
									{
										ver.addHighlight(true, false, true, true);
										if(ver.vertexType == AbstractVertex.VertexType.LINE)
											((Vertex) ver).highlightCycles();
                                        Parameters.ping();
									}
								}
							}
							else if(m.getClickCount() == 1)
							{
								AbstractVertex ver = Main.graph.getVertexNearestCoordinate(x, y);
								Parameters.leftArea.clear();
                                Main.graph.clearSelects();

								if(ver != null)
								{
									Parameters.highlightIncoming = true;
									Parameters.highlightOutgoing = true;
									Parameters.vertexHighlight = true;
									ver.addHighlight(true, false, true, true);
									if(ver.vertexType == AbstractVertex.VertexType.LINE)
										((Vertex) ver).highlightCycles();
                                    Parameters.ping();
                                    Parameters.fixCaretPositions();
								}
							}
							
							else if(m.getClickCount() == 2)
							{
								AbstractVertex ver = Main.graph.getVertexNearestCoordinate(x, y);
								if(ver != null)
								{
									if(ver.getMergeChildren().size() > 0) //We have a merge vertex
									{
										ver.deCollapse();
										Parameters.repaintAll();
									}
								}
							}
						}
						
						if(SwingUtilities.isRightMouseButton(m) && Main.graph != null)
						{
							//if(m.getClickCount() == 1)
							{
								AbstractVertex ver = Main.graph.getVertexNearestCoordinate(x, y);
								if(ver != null)
								{
									if(ver.getMergeParent() != null)
									{
										ver.getMergeParent().collapse();
									}
								}
							}
						}
						
						Parameters.repaintAll();
					}
				}
				
				public void mouseEntered(MouseEvent arg0){}
				
				public void mouseExited(MouseEvent arg0){}

				public void mousePressed(MouseEvent m)
				{
					if(!isGraphLoaded())
						return;
					context = false;
					vizPanel.selectLeft = getRelativeXPixels(m);
					vizPanel.selectTop = getRelativeYPixels(m);
					vizPanel.showSelection = true;
					
					Parameters.startTime = System.currentTimeMillis();
					Parameters.lastInterval = -1;
				}

				public void mouseReleased(MouseEvent ev)
				{
					if(!isGraphLoaded())
						return;
					vizPanel.requestFocus();
					vizPanel.showSelection = false;
					context = false;
					
					if(mouseDrag)
					{
						mouseDrag = false;
						if(Main.graph != null)
						{
							double x1 = vizPanel.getIndexFromCurrentPixelX(vizPanel.selectLeft);
							double x2 = vizPanel.getIndexFromCurrentPixelX(vizPanel.selectRight);
							double y1 = vizPanel.getIndexFromCurrentPixelY(vizPanel.selectTop);
							double y2 = vizPanel.getIndexFromCurrentPixelY(vizPanel.selectBottom);

							Main.graph.selectVertices(x1, x2, y1, y2);
						}
						Parameters.repaintAll();
					}
				}
			}
		);
		
		vizPanel.addMouseMotionListener
		(
			new MouseMotionListener()
			{
				public void mouseMoved(MouseEvent m){}				

				public void mouseDragged(MouseEvent m)
				{
					if(!isGraphLoaded())
						return;
					context = false;
					mouseDrag = true;
					vizPanel.selectRight = getRelativeXPixels(m);
					vizPanel.selectBottom = getRelativeYPixels(m);

					if((System.currentTimeMillis()-Parameters.startTime)/Parameters.refreshInterval > Parameters.lastInterval)
					{
						StacFrame.this.repaint();
						Parameters.lastInterval = (System.currentTimeMillis()-Parameters.startTime)/Parameters.refreshInterval;
					}
				}
			}
		);

		vizPanel.addMouseWheelListener
		(
			new MouseWheelListener()
			{
				public void mouseWheelMoved(MouseWheelEvent e)
				{
					if(!isGraphLoaded())
						return;

					int notches = e.getWheelRotation();
					
					//Zoom in or box++ for mouse wheel up, zoom out or box-- for mouse wheel down
					if(notches > 0)
					{
						if(e.isShiftDown())
						{
							vizPanel.boxSize /= Parameters.boxFactor;
						}
						else
						{
							double x = vizPanel.getRelativeFracFromAbsolutePixelX(getRelativeXPixels(e));
							double y = vizPanel.getRelativeFracFromAbsolutePixelY(getRelativeYPixels(e));
							Main.graph.increaseZoom(Parameters.zoomFactor, x, y);
						}
					}
					else
					{
						if(e.isShiftDown())
						{
							vizPanel.boxSize *= Parameters.boxFactor;
						}
						else
						{
							double x = vizPanel.getRelativeFracFromAbsolutePixelX(getRelativeXPixels(e));
							double y = vizPanel.getRelativeFracFromAbsolutePixelY(getRelativeYPixels(e));
							Main.graph.increaseZoom(1/Parameters.zoomFactor, x, y);
						}
					}
					
					Parameters.repaintAll();
				}
			}
		);
		
	}
	
	public void addMouseToContext()
	{
		contextPanel.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent m)
				{
					if(!isGraphLoaded())
						return;
					contextPanel.requestFocusInWindow();
					context = true;
					
					double x = contextPanel.getRelativeFracFromAbsolutePixelX(getRelativeXPixels(m));
					double y = contextPanel.getRelativeFracFromAbsolutePixelY(getRelativeYPixels(m));

					Main.graph.zoomNPan(x, y, 1.0);
					Parameters.repaintAll();

					if(System.currentTimeMillis() - Parameters.mouseLastTime > Parameters.mouseInterval)
					{
						Parameters.mouseLastTime = System.currentTimeMillis();						
					}
				}
				
				public void mouseEntered(MouseEvent arg0)
				{
				}
				
				public void mouseExited(MouseEvent arg0)
				{
				}

				public void mousePressed(MouseEvent m)
				{
					if(!isGraphLoaded())
						return;
					context = true;
					contextPanel.selectLeft = getRelativeXPixels(m);
					contextPanel.selectTop = getRelativeYPixels(m);
					contextPanel.showSelection = true;
					
					Parameters.startTime = System.currentTimeMillis();
					Parameters.lastInterval = -1;
				}

				public void mouseReleased(MouseEvent ev)
				{
					if(!isGraphLoaded())
						return;
					context = true;
//					vizPanel.requestFocus();
					contextPanel.showSelection = false;
					
					if(mouseDrag)
					{
						mouseDrag = false;
						if(Main.graph != null)
						{
							double x1 = contextPanel.getRelativeFracFromAbsolutePixelX(contextPanel.selectLeft);
							double x2 = contextPanel.getRelativeFracFromAbsolutePixelX(contextPanel.selectRight);
							double y1 = contextPanel.getRelativeFracFromAbsolutePixelY(contextPanel.selectTop);
							double y2 = contextPanel.getRelativeFracFromAbsolutePixelY(contextPanel.selectBottom);
							Main.graph.zoomNPan(x1, x2, y1, y2);
						}
						Parameters.repaintAll();
					}
				}

			}

		);
		
		contextPanel.addMouseMotionListener
		(
			new MouseMotionListener()
			{
				public void mouseMoved(MouseEvent m){}
				

				public void mouseDragged(MouseEvent m)
				{
					if(!isGraphLoaded())
						return;
					context = true;
					mouseDrag = true;
					contextPanel.selectRight = getRelativeXPixels(m);
					contextPanel.selectBottom = getRelativeYPixels(m);

					if((System.currentTimeMillis()-Parameters.startTime)/Parameters.refreshInterval > Parameters.lastInterval)
					{
						StacFrame.this.repaint();
						Parameters.lastInterval = (System.currentTimeMillis()-Parameters.startTime)/Parameters.refreshInterval;
					}
				}
			}
		);

		
		contextPanel.addMouseWheelListener
		(
			new MouseWheelListener()
			{
				public void mouseWheelMoved(MouseWheelEvent e)
				{
					if(!isGraphLoaded())
						return;
					int notches = e.getWheelRotation();
					
					//box++ for mouse wheel up, box-- for mouse wheel down
					if(notches > 0)
					{
						contextPanel.boxSize /= Parameters.boxFactor;
					}
					else
					{
						contextPanel.boxSize *= Parameters.boxFactor;
					}
					Parameters.repaintAll();
				}
			}
		);

	}
	
	//Gets the x location of a mouse event in pixels, shifted so that the left side is 0.
	public double getRelativeXPixels(MouseEvent m)
	{
		if(this.context)
			return m.getX() - contextPanel.getLocation().x;
		else
			return m.getX() - vizPanel.getLocation().x - this.leftPanel.getWidth() - centerSplitPane.getDividerSize();
	}

	//Gets the y location of a mouse event in pixels, shifted so that the top of the current panel is 0.
	public double getRelativeYPixels(MouseEvent m)
	{
		//TODO: Why is this not different for the context menu?
		//Subtract the top bar, the menu panel height, and the start height of the current panel
		return m.getY() - (this.getHeight() - this.getContentPane().getSize().height) - vizPanel.getY() - this.menuPanel.getHeight();
	}

	public void addKeyboard(JPanel viz)
	{
		viz.addKeyListener(new KeyListener()
			{
				public void keyTyped(KeyEvent ev){}
				
				public void keyPressed(KeyEvent ev)
				{
                    if(SearchField.focused)
                        return;
					int code = ev.getKeyCode();
//					System.out.println("Key pressed: " + code);
					
					if(code == 'L')
					{
						String lim = JOptionPane.showInputDialog(null, "Set Limit on the number of vertices:");
						Parameters.limitV = Long.parseLong(lim);
						Parameters.repaintAll();
					}

					if(Character.isDigit(code))
					{
						int digit = code - '0';
						if(ev.isControlDown())
						{
							//Assign hotkey to view
							System.out.println("Assigning hotkey to " + digit);
							Main.graph.addHotkeyedView(digit);
						}
						else
						{
							//Go to hotkeyed view
							System.out.println("Loading hotkeyed view: " + digit);
							Main.graph.loadHotkeyedView(digit);
							Parameters.repaintAll();
						}
					}
				}
					
				public void keyReleased(KeyEvent ev)
				{
//					System.out.println("key released: "+ev.getKeyCode());
				}
			});
		
		viz.setFocusable(true);
		viz.requestFocusInWindow();
	}
}
