
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.embed.swing.JFXPanel;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.text.DefaultCaret;

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
	private JPanel menuPanel, bytecodePanel, decompiledPanel, rightPanel, searchPanel;
	public JCheckBox showEdge;
    public SearchField searchF;
	
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
						searchAndHighlight(searchType.ID);
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
						searchAndHighlight(searchType.INSTRUCTION);
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
                    searchAndHighlight(searchType.METHOD);
                    Parameters.repaintAll();
                }
            }
		);

        JMenuItem searchTags = new JMenuItem("Allocation Tags");
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
		
		JMenuItem searchOutgoing = new JMenuItem("Outgoing neighborhood (open)");
		menuSearch.add(searchOutgoing);
		searchOutgoing.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.OUT_OPEN);
						Parameters.repaintAll();
					}
				}
		);

		JMenuItem searchOutgoingClosed = new JMenuItem("Outgoing neighborhood (closed)");
		menuSearch.add(searchOutgoingClosed);
		searchOutgoingClosed.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.OUT_CLOSED);
						Parameters.repaintAll();
					}
				}
		);

		JMenuItem searchIncoming = new JMenuItem("Incoming neighborhood (open)");
		menuSearch.add(searchIncoming);
		searchIncoming.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.IN_OPEN);
						Parameters.repaintAll();
					}
				}
		);

		JMenuItem searchIncomingClosed = new JMenuItem("Incoming neighborhood (closed)");
		menuSearch.add(searchIncomingClosed);
		searchIncomingClosed.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.IN_CLOSED);
						Parameters.repaintAll();
					}
				}
		);

		JMenuItem searchPathToRoot = new JMenuItem("Path to Root");
		menuSearch.add(searchPathToRoot);
		searchPathToRoot.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.ROOT_PATH);
						Parameters.repaintAll();
					}
				}
		);
		
		JMenuItem clearAll = new JMenuItem("Clear All");
		menuSearch.add(clearAll);
		clearAll.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						Main.graph.clearHighlights();
						Parameters.bytecodeArea.clear();
						Parameters.repaintAll();
					}
				}
		);

		//Navigation menu
		menuNavigation = new JMenu("Navigation");
		menuBar.add(menuNavigation);
		JMenuItem zoomIn = new JMenuItem("Zoom in        (Mouse wheel up)");
		menuNavigation.add(zoomIn);
		zoomIn.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						// TODO: Fill in correct function here
						//Main.graph.increaseZoom(Parameters.zoomFactor, -1, -1);
						Parameters.repaintAll();
					}
				}
		);
    
		JMenuItem zoomOut = new JMenuItem("Zoom out      (Mouse wheel down)");
		menuNavigation.add(zoomOut);
		zoomOut.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						// TODO: Fill in correct function here
						//Main.graph.increaseZoom(1/Parameters.zoomFactor, -1, -1);
						Parameters.repaintAll();
					}
				}
		);
		
        JMenuItem rearrange = new JMenuItem("Rearrange graph");
        menuNavigation.add(rearrange);
        rearrange.addActionListener
        (
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						Main.graph.root.rearrangeByWidth();
						Main.graph.root.centerizeXCoordinate();
						Parameters.repaintAll();
					}
				}
		);
        rearrange.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));

        
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
						String newFontSize = JOptionPane.showInputDialog(null, "The current font size is: " +
								Parameters.font.getSize() + ". Please enter a new font size");
						Parameters.font = new Font("Serif", Font.PLAIN, Integer.parseInt(newFontSize));
						Parameters.bytecodeArea.setFont(Parameters.font);
						Parameters.rightArea.setFont(Parameters.font);
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
		// Construct columns
		System.out.println("Layout size: " + Integer.toString(layout.size()));
		ArrayList<GUIPanelColumn> columns = new ArrayList<GUIPanelColumn>();
		for(int i = 0; i < layout.size(); i++)
		{
			System.out.println("Constructing column: " + Integer.toString(i) + ", height " +
					Integer.toString(layout.get(i).size()));
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
        
        JPanel topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEtchedBorder());
        topPanel.setLayout(new BorderLayout());
        this.getContentPane().add(topPanel, BorderLayout.NORTH);

		//menuPanel
		this.menuPanel = new JPanel();
		this.menuPanel.setBorder(BorderFactory.createEtchedBorder());
		this.menuPanel.setLayout(new FlowLayout());
        topPanel.add(this.menuPanel, BorderLayout.CENTER);

		JPanel controlPanel = new JPanel();
		controlPanel.setBorder(BorderFactory.createEtchedBorder());
		controlPanel.setLayout(new GridLayout(1,1));
		this.menuPanel.add(controlPanel);
		
		showEdge = new JCheckBox("Show edges");
		showEdge.setEnabled(true);
		showEdge.setSelected(true);
		showEdge.addItemListener
		(
			new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					mainPanel.showEdge = showEdge.isSelected();
					mainPanel.getPanelRoot().setEdgeVisibility(showEdge.isSelected());
					for(AbstractVertex v : mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
						v.setEdgeVisibility(showEdge.isSelected());
				}
			}
		);
		controlPanel.add(showEdge);
		
		JPanel sizePanel = new JPanel();
		sizePanel.setBorder(BorderFactory.createEtchedBorder());
		sizePanel.setLayout(new GridLayout(1,3));
		this.menuPanel.add(sizePanel);
		
		JButton sizeMinus = new JButton("-");
		sizeMinus.setEnabled(true);
		sizeMinus.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					ParallelTransition pt = new ParallelTransition();
					for(AbstractVertex v : mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
					{
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
		sizePanel.add(sizeMinus);
		
		JLabel sizeL = new JLabel("Box size");
		sizeL.setHorizontalAlignment(SwingConstants.CENTER);
		sizePanel.add(sizeL);
		
		JButton sizePlus = new JButton("+");
		sizePlus.setEnabled(true);
		sizePlus.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					ParallelTransition pt = new ParallelTransition();
					for(AbstractVertex v : mainPanel.getPanelRoot().getInnerGraph().getVertices().values())
					{
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
		sizePanel.add(sizePlus);
        
        
        // Search bar
        this.searchF = new SearchField();
        this.getContentPane().add(searchF, BorderLayout.SOUTH);

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

	public void setSplitScreen()
	{
		// Declare each panel
		decompiledPanel = new JPanel();
		bytecodePanel = new JPanel();
		rightPanel = new JPanel();
        searchPanel = new JPanel();
		this.mainPanel = new VizPanel();

		// Build each panel
		JLabel leftL = new JLabel("Code", JLabel.CENTER);
		Parameters.bytecodeArea = new CodeArea();
		bytecodePanel.setLayout(new BorderLayout());
		bytecodePanel.add(leftL,BorderLayout.NORTH);
		JScrollPane bytecodeScroll = new JScrollPane (Parameters.bytecodeArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		bytecodePanel.add(bytecodeScroll, BorderLayout.CENTER);
		bytecodePanel.setFont(Parameters.font);

		decompiledPanel.setLayout(new BorderLayout());
		Parameters.decompiledArea = new CodeArea();
		JScrollPane decompiledScroll = new JScrollPane(Parameters.decompiledArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		decompiledPanel.add(decompiledScroll, BorderLayout.CENTER);
		decompiledPanel.setFont(Parameters.font);
		
		JLabel rightL = new JLabel("Description", JLabel.CENTER);
		Parameters.rightArea = new JTextArea();
		Parameters.rightArea.setEditable(false);
        ((DefaultCaret)Parameters.rightArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(rightL, BorderLayout.NORTH);
		JScrollPane scrollR = new JScrollPane (Parameters.rightArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		rightPanel.add(scrollR, BorderLayout.CENTER);
		rightPanel.setFont(Parameters.font);
		
        JLabel searchL = new JLabel("Search Results", JLabel.CENTER);
        Parameters.searchArea = new SearchArea();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(searchL,BorderLayout.NORTH);
        JScrollPane scrollS = new JScrollPane (Parameters.searchArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        searchPanel.add(scrollS, BorderLayout.CENTER);
        searchPanel.setFont(Parameters.font);

		// Build data structure to hold panels
		ArrayList<ArrayList<JComponent>> layout = new ArrayList<ArrayList<JComponent>>();
		ArrayList<ArrayList<Double>> layoutRowWeights = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> layoutColumnWeights = new ArrayList<Double>();

		ArrayList<JComponent> left = new ArrayList<JComponent>();
		left.add(bytecodePanel);
		left.add(decompiledPanel);

		ArrayList<Double> leftWeights = new ArrayList<Double>();
		leftWeights.add(0.6);
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

	public void addKeyboard(JFXPanel viz)
	{
		viz.addKeyListener(new KeyListener()
		{
			public void keyTyped(KeyEvent ev){}

			public void keyPressed(KeyEvent ev)
			{
				if(SearchField.focused)
					return;

				int code = ev.getKeyCode();
				if(code == 'L')
				{
					String lim = JOptionPane.showInputDialog(null, "Set limit on the number of vertices:");
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
