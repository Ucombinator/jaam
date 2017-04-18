package org.ucombinator.jaam.visualizer.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;

import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.main.TakeInput;

import java.awt.*;  //TODO: Is this necessary?
import java.io.File;

/**
 * Created by timothyjohnson on 4/17/17.
 */
public class OuterFrame extends BorderPane {
    private MenuBar menuBar;
    private Menu menuFile, menuSearch, menuNavigation, menuCustomize, menuHelp;

    private TabPane tabPane;

    public OuterFrame() {
        makeMenuBar();
        makeTabPane();
    }

    public void makeMenuBar()
    {
        menuBar = new MenuBar();
        this.setTop(menuBar);

        //File menu
        // TODO: Add hotkeys for commands
        menuFile = new Menu("File");
        menuBar.getMenus().add(menuFile);
        MenuItem loadMessages = new MenuItem("Load graph from message file");
        menuFile.getItems().add(loadMessages);
        loadMessages.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                // TODO: Make new StacFrame in tab
                loadGraph(true);
            }
        });

        MenuItem loadImage = new MenuItem("Load image");
        menuFile.getItems().add(loadImage);
        loadImage.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                File savedFile = GUIUtils.openFile(OuterFrame.this, "Open Resource File");
                if (savedFile != null) {
                    buildImageTab(savedFile);
                }
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
                        Tab currentTab = OuterFrame.this.tabPane.getSelectionModel().getSelectedItem();
                        if(currentTab.getContent() instanceof StacFrame) {
                            StacFrame currentFrame = (StacFrame) currentTab.getContent();
                            currentFrame.initSearch(StacFrame.searchType.ID);
                        }
                        else {
                            System.out.println("Error! Current tab is not a StacFrame.");
                        }
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
                        Tab currentTab = OuterFrame.this.tabPane.getSelectionModel().getSelectedItem();
                        if(currentTab.getContent() instanceof StacFrame) {
                            StacFrame currentFrame = (StacFrame) currentTab.getContent();
                            currentFrame.initSearch(StacFrame.searchType.INSTRUCTION);
                        }
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
                        Tab currentTab = OuterFrame.this.tabPane.getSelectionModel().getSelectedItem();
                        if(currentTab.getContent() instanceof StacFrame) {
                            StacFrame currentFrame = (StacFrame) currentTab.getContent();
                            currentFrame.initSearch(StacFrame.searchType.METHOD);
                        }
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
						OuterFrame.this.initSearch();
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
                        //OuterFrame.this.repaintAll();
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
                        //OuterFrame.this.repaintAll();
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
                        //OuterFrame.this.repaintAll();
                    }
                }
        );

        //Customize display
        menuCustomize = new Menu("Customize");
        menuBar.getMenus().add(menuCustomize);

        /*MenuItem changeFont = new MenuItem("Change font size");
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
                        OuterFrame.this.bytecodeArea.resetFont();
                        OuterFrame.this.rightArea.setFont(Parameters.jfxFont);
                        OuterFrame.this.repaintAll();
                    }
                }
        );*/

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

    public void buildImageTab(File savedFile)
    {
        Tab imageTab = new Tab();
        imageTab.setText("Image View");

        ImageView imageView = new ImageView();
        imageView.setImage(new javafx.scene.image.Image("file:"+savedFile.getAbsolutePath()));


        ScrollPane sp = new ScrollPane();
        sp.setContent(imageView);

        imageTab.setContent(sp);
        tabPane.getTabs().add(imageTab);
    }

    public void makeTabPane() {
        this.tabPane = new TabPane();
        this.tabPane.setVisible(true);
        this.setCenter(this.tabPane);
    }

    public void loadGraph(boolean fromMessages)
    {
        File file = GUIUtils.openFile(this, "Load graph file");
        if(file == null) {
            System.out.println("Error! Invalid file.");
            return;
        }

        TakeInput ti = new TakeInput();
        Graph graph = ti.parsePackets(file.getAbsolutePath());
        StacFrame newFrame = new StacFrame(graph);

        Tab newTab = new Tab();
        newTab.setText(file.getName());
        newTab.setContent(newFrame);
        this.tabPane.getTabs().add(newTab);
    }

    public StacFrame getCurrentFrame() {
        Tab currentTab = this.tabPane.getSelectionModel().getSelectedItem();
        if(currentTab.getContent() instanceof StacFrame)
            return (StacFrame) currentTab.getContent();
        else
            return null;
    }
}
