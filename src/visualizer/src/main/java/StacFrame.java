
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;


import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

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
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JButton;

import java.awt.Font;


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
	private JSplitPane centerPanel;
	public VizPanel vizPanel, contextPanel;
//	private JTextArea property, consoleArea;
//	private JLabel propertyLabel, consoleLabel;
	public JButton zoomMinus, zoomPlus;
	private JPanel menuPanel, leftPanel, rightPanel;
//	public JSplitPane leftSplitPane, rightSplitPane;
	public JCheckBox showContext, showEdge;
	private boolean context=false, mouseDrag=false;
//	private boolean mouseDrag=false;
	
	public enum searchType
	{
		ID, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
	}
	
	public StacFrame()
	{
		this(false);
	}
	
	public StacFrame(boolean cont)
	{
		super("STAC Visualization");
		width = Parameters.width;
		height = Parameters.height;
//		this.context = cont;
		this.setLocation(0, 0);

/*
		if(cont)
		{
			this.setTitle("Context View");
			width = Parameters.contextWidth;
			height = Parameters.contextHeight;
			this.setLocation(Parameters.width, 0);
		}
*/
		
		setSize(this.width,this.height);
		
		makeMenu();
		makeLayout();
	}
	
	
	public void makeMenu()
	{
		menuBar = new JMenuBar();
		
		//File menu
		menuFile = new JMenu("File");
		menuBar.add(menuFile);
		JMenuItem loadGraph = new JMenuItem("Load graph");
		menuFile.add(loadGraph);
		loadGraph.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						loadGraphOperation(false);
					}
				}
		);

		JMenuItem loadCFG = new JMenuItem("Load graph in CFG mode");
		menuFile.add(loadCFG);
		loadCFG.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						loadGraphOperation(true);
					}
					
				}
		);
		
		JMenuItem saveGraph = new JMenuItem("Save graph (not implemented)");
		menuFile.add(saveGraph);
		saveGraph.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
					}
				}
		);		
		
		JMenuItem saveCFG = new JMenuItem("Save graph in CFG mode (not implemented)");		
		menuFile.add(saveCFG);
		saveCFG.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
					}
				}
		);
		
		
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
						Parameters.refreshAll();
					}
				}
		);

		JMenuItem searchByInst = new JMenuItem("by Instruction");
		menuSearch.add(searchByInst);
		searchByInst.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.INSTRUCTION);
						Parameters.refreshAll();
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
						Parameters.refreshAll();
					}
				}
		);

		JMenuItem searchLeaves = new JMenuItem("All leaves");
		menuSearch.add(searchLeaves);
		searchLeaves.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.ALL_LEAVES);
						Parameters.refreshAll();
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
						Parameters.refreshAll();
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
						Parameters.refreshAll();
					}
				}
		);

		
		JMenuItem searchOutgoingclosed = new JMenuItem("Outgoing neighborhood (closed)");
		menuSearch.add(searchOutgoingclosed);
		searchOutgoingclosed.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.OUT_CLOSED);
						Parameters.refreshAll();
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
						Parameters.refreshAll();
					}
				}
		);

		
		JMenuItem searchIncomingclosed = new JMenuItem("Incoming neighborhood (closed)");
		menuSearch.add(searchIncomingclosed);
		searchIncomingclosed.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						searchAndHighlight(searchType.IN_CLOSED);
						Parameters.refreshAll();
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
						Parameters.refreshAll();
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
						StacViz.graph.clearHighlights();
						Parameters.leftArea.clear();
						Parameters.refreshAll();
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
						StacViz.graph.increaseZoom(Parameters.zoomFactor);
						Parameters.refreshAll();
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
						StacViz.graph.increaseZoom(1/Parameters.zoomFactor);
						Parameters.refreshAll();
					}
				}
		);
		
		JMenuItem resetGraph = new JMenuItem("Reset view");
		menuNavigation.add(resetGraph);
		resetGraph.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.resetZoom();
						Parameters.refreshAll();
					}
				}
		);
		resetGraph.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
		
		JMenuItem collapse = new JMenuItem("Collapse nodes");
		menuNavigation.add(collapse);
		collapse.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.collapseOnce();
						StacViz.graph.computeShowViz();
						Parameters.refreshAll();
					}
				}
		);
		collapse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

		JMenuItem decollapse = new JMenuItem("Expand nodes");
		menuNavigation.add(decollapse);
		decollapse.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.deCollapseOnce();
						StacViz.graph.computeShowViz();
						Parameters.refreshAll();
					}
				}
		);
		decollapse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
		
		JMenuItem previous = new JMenuItem("Previous view");
		menuNavigation.add(previous);
		previous.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.loadPreviousView();
						Parameters.refreshAll();
					}
				}
		);
		previous.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
		
		
		JMenuItem next = new JMenuItem("Return to next view");
		menuNavigation.add(next);
		next.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.restoreNewView();
						Parameters.refreshAll();
					}
				}
		);
		next.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
		
		JMenuItem panUp = new JMenuItem("Pan up");
		menuNavigation.add(panUp);
		panUp.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.shiftView(0, -1);
						Parameters.refreshAll();
					}
				}
		);
		panUp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
		
		JMenuItem panDown = new JMenuItem("Pan down");
		menuNavigation.add(panDown);
		panDown.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.shiftView(0, 1);
						Parameters.refreshAll();
					}
				}
		);
		panDown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
		
		JMenuItem panLeft = new JMenuItem("Pan left");
		menuNavigation.add(panLeft);
		panLeft.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.shiftView(-1, 0);
						Parameters.refreshAll();
					}
				}
		);
		panLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
		
		JMenuItem panRight = new JMenuItem("Pan right");
		menuNavigation.add(panRight);
		panRight.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						StacViz.graph.shiftView(1, 0);
						Parameters.refreshAll();
					}
				}
		);
		panRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
		
		
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
						String newFontSize = JOptionPane.showInputDialog(null, "The current font size is: " + Parameters.font.getSize() + ". Please enter a new font size");
						Parameters.font = new Font("Serif", Font.PLAIN, Integer.parseInt(newFontSize));
						Parameters.leftArea.setFont(Parameters.font);
						Parameters.rightArea.setFont(Parameters.font);
						Parameters.refreshAll();
					}
				}
		);
		changeFont.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
		
		//Help menu
		menuHelp = new JMenu("Help");
		menuBar.add(menuHelp);
		JMenuItem help = new JMenuItem("shortcuts");
		menuHelp.add(help);
		help.addActionListener
		(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent ev)
					{
						JOptionPane.showMessageDialog(getParent(), 
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
						);
					}
				}
		);
		help.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0));
	}
	
	public void makeLayout()
	{		
		//centerPanel and vizPanel
		this.setLayout(new BorderLayout());
		
		
		///////////////////*************************** Layout *******************************///////////////////
		
		////////*********** centerPanel and vizPanel *************////////
		
		setLayout(new BorderLayout());
		
//		this.vizPanel = new VizPanel(this,this.context);
		this.vizPanel = new VizPanel(this,false);
		this.getContentPane().add(this.vizPanel,BorderLayout.CENTER);
		
		this.addMouseToViz();
		this.addKeyboard(vizPanel);
		
		
		//menuPanel
		this.menuPanel = new JPanel();
		this.menuPanel.setBorder(BorderFactory.createEtchedBorder());
		this.menuPanel.setLayout(new FlowLayout());
		this.getContentPane().add(this.menuPanel, BorderLayout.NORTH);
		
		
		JPanel contextPanel = new JPanel();
		contextPanel.setBorder(BorderFactory.createEtchedBorder());
		contextPanel.setLayout(new GridLayout(1,1));
		this.menuPanel.add(contextPanel);
		
		
		showEdge = new JCheckBox("Show Edge");
		showEdge.setEnabled(true);
		showEdge.setSelected(true);
		showEdge.addItemListener
		(
			new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					if(showEdge.isSelected())
					{
						vizPanel.showEdge = true;
					}
					else
					{
						vizPanel.showEdge = false;
					}
					refresh();
				}
			}
		);
		contextPanel.add(showEdge);

		
//		if(!this.context)
		{
/*			
			showContext = new JCheckBox("Context View");
			showContext.setEnabled(true);
			showContext.addItemListener
			(
				new ItemListener()
				{
					public void itemStateChanged(ItemEvent e)
					{
						if(showContext.isSelected())
						{
							Parameters.contextFrame.setVisible(true);
						}
						else
						{
							Parameters.contextFrame.setVisible(false);
						}
					}
				}
			);
			contextPanel.add(showContext);
*/		
		
			JPanel zoomPanel = new JPanel();
			zoomPanel.setBorder(BorderFactory.createEtchedBorder());
			zoomPanel.setLayout(new GridLayout(1,3));
			this.menuPanel.add(zoomPanel);
			
			zoomMinus = new JButton("-");
			zoomMinus.setEnabled(false);
			zoomMinus.addActionListener
			(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						StacViz.graph.increaseZoom(1.0/Parameters.zoomFactor);
						Parameters.refreshAll();
					}
				}
			);
			zoomPanel.add(zoomMinus);
			
			JLabel zoomL = new JLabel("zoom");
			zoomL.setHorizontalAlignment(SwingConstants.CENTER);
			zoomPanel.add(zoomL);
			
			zoomPlus = new JButton("+");
			zoomPlus.setEnabled(false);
			zoomPlus.addActionListener
			(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						StacViz.graph.increaseZoom(Parameters.zoomFactor);
						Parameters.refreshAll();
					}
				}
			);
			zoomPanel.add(zoomPlus);

		}

		
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
					vizPanel.boxSize *= Parameters.boxFactor;
					Parameters.refreshAll();
				}
			}
		);
		sizePanel.add(sizeMinus);
		
		JLabel sizeL = new JLabel("box size");
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
					vizPanel.boxSize /= Parameters.boxFactor;
					Parameters.refreshAll();
				}
			}
		);
		sizePanel.add(sizePlus);
		
		
		this.setJMenuBar(menuBar);
//		if(this.context)
//			this.setVisible(false);
//		else
			this.setVisible(true);
		
//		if(!this.context)
		{
//			Parameters.contextFrame = new StacFrame(true);
			
			this.refresh();
			
			
			
//			this.vizPanel.requestFocus();
//			this.requestFocus();
			
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
/*		else
		{
			this.addWindowListener
			(
				new WindowAdapter()
				{
					public void windowClosing(WindowEvent windowEvent)
					{
						Parameters.stFrame.showContext.setSelected(false);
						Parameters.stFrame.refresh();
			        }
			    }
			);
		}
*/		
	}
	
	
	public void setSplitScreen()
	{
		double rw1 = 0.2, rw2 = 0.75, rw3 = 0.6;
		if(StacViz.graph != null)
		{
			rw1 = centerPanel.getDividerLocation()*1.0/centerPanel.getWidth();
			rw2 = ((JSplitPane)centerPanel.getRightComponent()).getDividerLocation()*1.0/(((JSplitPane)centerPanel.getRightComponent()).getWidth());
			rw3 = ((JSplitPane)centerPanel.getLeftComponent()).getDividerLocation()*1.0/(((JSplitPane)centerPanel.getLeftComponent()).getHeight());
		}
		this.getContentPane().remove(((BorderLayout)this.getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER));
		centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		this.getContentPane().add(this.centerPanel,BorderLayout.CENTER);
		centerPanel.setLeftComponent(new JSplitPane(JSplitPane.VERTICAL_SPLIT, true));
		centerPanel.setRightComponent(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true));
		
		centerPanel.setOneTouchExpandable(true);
		((JSplitPane)centerPanel.getLeftComponent()).setOneTouchExpandable(true);		
		((JSplitPane)centerPanel.getRightComponent()).setOneTouchExpandable(true);		
		
		
		leftPanel = new JPanel();
		((JSplitPane)centerPanel.getLeftComponent()).setLeftComponent(leftPanel);
		rightPanel = new JPanel();
		((JSplitPane)centerPanel.getRightComponent()).setRightComponent(rightPanel);

		JLabel leftL = new JLabel("Context", JLabel.CENTER);
		Parameters.leftArea = new CodeArea();
		leftPanel.setLayout(new BorderLayout());
		leftPanel.add(leftL,BorderLayout.NORTH);
		JScrollPane scrollL = new JScrollPane (Parameters.leftArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		leftPanel.add(scrollL, BorderLayout.CENTER);
		
		JLabel rightL = new JLabel("Description", JLabel.CENTER);
		Parameters.rightArea = new JTextArea();
		Parameters.rightArea.setEditable(false);
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(rightL, BorderLayout.NORTH);
		JScrollPane scrollR = new JScrollPane (Parameters.rightArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		rightPanel.add(scrollR, BorderLayout.CENTER);
		rightPanel.setFont(Parameters.font);
		

		((JSplitPane)centerPanel.getRightComponent()).setLeftComponent(this.vizPanel);
		this.contextPanel = new VizPanel(this,true);
		((JSplitPane)centerPanel.getLeftComponent()).setRightComponent(this.contextPanel);

		
		centerPanel.setResizeWeight(rw1);
		((JSplitPane)centerPanel.getRightComponent()).setResizeWeight(rw2);
		((JSplitPane)centerPanel.getLeftComponent()).setResizeWeight(rw3);
//		centerPanel.resetToPreferredSizes();
		
		this.addMouseToContext();
		this.addKeyboard(contextPanel);
		
	}
	
	public void loadGraphOperation(boolean fromJSON)
	{
		String file = Parameters.openFile();
		if(file==null)
			return;
		TakeInput ti = new TakeInput();
		ti.setFileInput(file);
		ti.run(fromJSON);		
		Parameters.refreshAll();
	}
	

	public void refresh()
	{
		this.paintAll(getGraphics());
	}

	
	public void addToConsole(String str)
	{
		System.out.println(str);
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
		if(search != searchType.ALL_LEAVES && search != searchType.ALL_SOURCES)
		{
			input = JOptionPane.showInputDialog(null, title);
			if(input == null)
				return;
			else
				input = input.trim();

			if(input.equals(""))
				return;
		}

		StacViz.graph.searchNodes(search, input);
		
		Parameters.vertexHighlight = true;
		
		if(search == searchType.OUT_OPEN)
			Parameters.highlightOutgoing = false;
		else
			Parameters.highlightOutgoing = true;
		if(search == searchType.IN_OPEN)
			Parameters.highlightIncoming = false;
		else
			Parameters.highlightIncoming = true;
	}
	
	public boolean checkGraph()
	{
		return (StacViz.graph!=null);
	}

//	public void addMouse()
//	{
//		this.addMouseToViz();
//		if(StacViz.graph != null)
//			this.addMouseToContext();
//	}
	
	
	public void addMouseToViz()
	{
		vizPanel.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent m)
				{
					if(!checkGraph())
						return;
					vizPanel.requestFocusInWindow();
					context = false;
					double x = vizPanel.getBackX(getX(m))*StacViz.graph.getWidth();
					double y = vizPanel.getBackY(getY(m))*StacViz.graph.getHeight();
					
//					System.out.println("x="+x+", y="+y);
					
					if(System.currentTimeMillis() - Parameters.mouseLastTime > Parameters.mouseInterval)
					{
						Parameters.mouseLastTime = System.currentTimeMillis();
						
						if(SwingUtilities.isLeftMouseButton(m) && StacViz.graph != null)
						{	
							if(m.isShiftDown())
							{
								AbstractVertex ver = StacViz.graph.getVertexNearestCoordinate(x, y);
								if(ver != null)
								{
									if(ver.isHighlighted())
									{
										ver.clearAllHighlights();
										StacViz.graph.redoCycleHighlights();
									}
									else
									{
										ver.addHighlight(true, true, true);
										if(ver.vertexType == AbstractVertex.VertexType.LINE)
											((Vertex) ver).highlightCycles();
									}
								}
							}
							else if(m.getClickCount() == 1)
							{
								AbstractVertex ver = StacViz.graph.getVertexNearestCoordinate(x, y);
								Parameters.leftArea.clear();
								StacViz.graph.clearHighlights();

								if(ver != null)
								{
									Parameters.highlightIncoming = true;
									Parameters.highlightOutgoing = true;
									Parameters.vertexHighlight = true;
									ver.addHighlight(true, true, true);
									if(ver.vertexType == AbstractVertex.VertexType.LINE)
										((Vertex) ver).highlightCycles();
								}
							}
							
							else if(m.getClickCount() == 2)
							{
								AbstractVertex ver = StacViz.graph.getVertexNearestCoordinate(x, y);
								if(ver != null)
								{
									if(ver.getMergeChildren().size() > 0) //We have a merge vertex
									{
										ver.deCollapse();
										StacViz.graph.computeShowViz();
										Parameters.refreshAll();
									}
								}
							}
						}
						
						if(SwingUtilities.isRightMouseButton(m) && StacViz.graph != null)
						{
							//if(m.getClickCount() == 1)
							{
								AbstractVertex ver = StacViz.graph.getVertexNearestCoordinate(x, y);
								if(ver != null)
								{
									if(ver.getMergeParent() != null)
									{
										ver.getMergeParent().collapse();
										StacViz.graph.computeShowViz();
									}
								}
							}
						}
						
						Parameters.refreshAll();
					}
				}
				
				public void mouseEntered(MouseEvent arg0){}
				
				public void mouseExited(MouseEvent arg0){}

				public void mousePressed(MouseEvent m)
				{
					if(!checkGraph())
						return;
					context = false;
					vizPanel.selectLeft = getX(m);
					vizPanel.selectTop = getY(m);
					vizPanel.showSelection = true;
					
					Parameters.startTime = System.currentTimeMillis();
					Parameters.lastInterval = -1;
				}

				public void mouseReleased(MouseEvent ev)
				{
					if(!checkGraph())
						return;
					vizPanel.requestFocus();
					vizPanel.showSelection = false;
					context = false;
					
					if(mouseDrag)
					{
						mouseDrag = false;
						if(StacViz.graph != null)
						{
							double x1 = vizPanel.getBackX(vizPanel.selectLeft);
							double x2 = vizPanel.getBackX(vizPanel.selectRight);
							double y1 = vizPanel.getBackY(vizPanel.selectTop);
							double y2 = vizPanel.getBackY(vizPanel.selectBottom);

							StacViz.graph.selectVertices(x1, x2, y1, y2);
						}
						Parameters.refreshAll();
					}
				}
			}
		);
		
		vizPanel.addMouseMotionListener
		(
			new MouseMotionListener()
			{
/*
				public void mouseMoved(MouseEvent m)
				{
					if(Parameters.graph == null)
						return;
//					if(System.currentTimeMillis() - Parameters.mouseLastTime < Parameters.mouseInterval)
//						return;
					Parameters.mouseLastTime = System.currentTimeMillis();
//					System.out.println("mouse moving...");
					double x = vizPanel.getBackX(getX(m))*Parameters.graph.getWidth();
					double y = vizPanel.getBackY(getY(m))*Parameters.graph.getHeight();
					Vertex ver = Parameters.graph.getVertexAtCoordinate(x, y);
					if(ver==null)
					{
						Parameters.shownVertex = null;
					}
					else if(ver!=Parameters.shownVertex)
					{
						Parameters.shownVertex = ver;
						Parameters.rightArea.setText(ver.getRightPanelContent());
						Parameters.rightArea.setCaretPosition(0);
						
						int i=0;
						Vertex ver2;
						
						for(ver2=ver; ver2.mergeParent!=null; ver2 = ver2.mergeParent, i++);
						
						if(i==2)
							repopulateLeftArea(ver,ver.mergeParent);
						else
							repopulateLeftArea(null,ver);
						
						
						Parameters.highlightTo = true;
						Parameters.highlightFrom = true;
						Parameters.vertexHighlight = false;
						Parameters.graph.clearHighlights();
						ver.findCycles();
						refreshBoth();
						
					}
				}
*/
				public void mouseMoved(MouseEvent m){}
				

				public void mouseDragged(MouseEvent m)
				{
					if(!checkGraph())
						return;
					context = false;
					mouseDrag = true;
					vizPanel.selectRight = getX(m);
					vizPanel.selectBottom = getY(m);

					if((System.currentTimeMillis()-Parameters.startTime)/Parameters.refreshInterval > Parameters.lastInterval)
					{
						refresh();
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
					if(!checkGraph())
						return;
					int notches = e.getWheelRotation();
					//System.out.println("Moved mouse wheel: " + notches);

					
					//Zoom in or box++ for mouse wheel up, zoom out or box-- for mouse wheel down
					if(notches > 0)
					{
						if(e.isShiftDown())
						{
							vizPanel.boxSize /= Parameters.boxFactor;
						}
						else
						{
							StacViz.graph.increaseZoom(Parameters.zoomFactor);
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
							StacViz.graph.increaseZoom(1/Parameters.zoomFactor);
						}
					}
					Parameters.refreshAll();
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
					if(!checkGraph())
						return;
					contextPanel.requestFocusInWindow();
					context = true;
					
//					double x = contextPanel.getBackX(getX(m))*StacViz.graph.getWidth();
//					double y = contextPanel.getBackY(getY(m))*StacViz.graph.getHeight();
					double x = contextPanel.getBackX(getX(m));
					double y = contextPanel.getBackY(getY(m));
					
					
//					System.out.println("x="+x+", y="+y);

					StacViz.graph.zoomNPan(x, y, 1.0);
					Parameters.refreshAll();

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
					if(!checkGraph())
						return;
					context = true;
					contextPanel.selectLeft = getX(m);
					contextPanel.selectTop = getY(m);
					contextPanel.showSelection = true;
					
					Parameters.startTime = System.currentTimeMillis();
					Parameters.lastInterval = -1;
				}

				public void mouseReleased(MouseEvent ev)
				{
					if(!checkGraph())
						return;
					context = true;
//					vizPanel.requestFocus();
					contextPanel.showSelection = false;
					
					if(mouseDrag)
					{
						mouseDrag = false;
						if(StacViz.graph != null)
						{
							double x1 = contextPanel.getBackX(contextPanel.selectLeft);
							double x2 = contextPanel.getBackX(contextPanel.selectRight);
							double y1 = contextPanel.getBackY(contextPanel.selectTop);
							double y2 = contextPanel.getBackY(contextPanel.selectBottom);
							StacViz.graph.zoomNPan(x1, x2, y1, y2);
						}
						Parameters.refreshAll();
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
					if(!checkGraph())
						return;
					context = true;
					mouseDrag = true;
					contextPanel.selectRight = getX(m);
					contextPanel.selectBottom = getY(m);

					if((System.currentTimeMillis()-Parameters.startTime)/Parameters.refreshInterval > Parameters.lastInterval)
					{
						refresh();
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
					if(!checkGraph())
						return;
					int notches = e.getWheelRotation();
//					System.out.println("Moved mouse wheel: " + notches);
					
					//box++ for mouse wheel up, box-- for mouse wheel down
					if(notches > 0)
					{
						contextPanel.boxSize /= Parameters.boxFactor;
					}
					else
					{
						contextPanel.boxSize *= Parameters.boxFactor;
					}
					Parameters.refreshAll();
				}
			}
		);

	}
	
	
	public double getX(MouseEvent m)
	{
		if(this.context)
			return m.getX() - contextPanel.getLocation().x;// - this.leftPanel.getWidth() - centerPanel.getDividerSize();
		else
			return m.getX() - vizPanel.getLocation().x - this.leftPanel.getWidth() - centerPanel.getDividerSize();
	}
	
	public double getY(MouseEvent m)
	{
		return m.getY() - (this.getHeight() - this.getContentPane().getSize().height) - vizPanel.getY() - this.menuPanel.getHeight();
	}


	public void addKeyboard(JPanel viz)
	{
		viz.addKeyListener(new KeyListener()
			{
				public void keyTyped(KeyEvent ev){}
				
				public void keyPressed(KeyEvent ev)
				{
					int code = ev.getKeyCode();
//					System.out.println("Key pressed: " + code);
					
					if(code == 'L')
					{
						String lim = JOptionPane.showInputDialog(null, "Set Limit on the number of vertices:");
						Parameters.limitV = Long.parseLong(lim);
						Parameters.refreshAll();
					}

					if(Character.isDigit(code))
					{
						int digit = code - '0';
						if(ev.isControlDown())
						{
							//Assign hotkey to view
							System.out.println("Assigning hotkey to " + digit);
							StacViz.graph.addHotkeyedView(digit);
						}
						else
						{
							//Go to hotkeyed view
							System.out.println("Loading hotkeyed view: " + digit);
							StacViz.graph.loadHotkeyedView(digit);
							Parameters.refreshAll();
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
