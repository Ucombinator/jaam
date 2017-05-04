package org.ucombinator.jaam.visualizer.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import java.io.File;

import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.main.TakeInput;

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
                OuterFrame.this.loadGraph(true, false);
            }
        });

        MenuItem loadLoop = new MenuItem("Load graph from loop file");
        menuFile.getItems().add(loadLoop);
        loadLoop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                OuterFrame.this.loadGraph(true, true);
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
        //menuBar.getMenus().add(menuNavigation);

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

        //Customize display
        menuCustomize = new Menu("Customize");
        //menuBar.getMenus().add(menuCustomize);

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
        //menuBar.getMenus().add(menuHelp);
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
        String url = "file:/logo.jpg";
        Image img = new Image(url);
        Double factor = 1.5;
        BackgroundImage bgImg = new BackgroundImage(img, 
            BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER, 
            new BackgroundSize(542/factor,409/factor, false, false, false, false));
        // TODO: What are the constants 542 and 409?

        //this.tabPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        this.tabPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        this.tabPane.setBackground(new Background(bgImg));
        this.tabPane.setVisible(true);
        this.setCenter(this.tabPane);
    }

    public void loadGraph(boolean chooseFile, boolean isLoopGraph)
    {
        Graph graph;
        TakeInput ti = new TakeInput();
        String filename = "";

    	System.out.println("Load graph: start...");

    	if(chooseFile) {
            File file = GUIUtils.openFile(this, "Load graph file");
            if (file == null) {
                System.out.println("Error! Invalid file.");
                return;
            }
            if(isLoopGraph)
                graph = ti.parseLoopGraph(file.getAbsolutePath());
            else
                graph = ti.parsePackets(file.getAbsolutePath());
            filename = file.getName();
        }
        else {
            graph = ti.parsePackets("");
        }
        
        System.out.println("--> Create visualization: start...");
        StacFrame newFrame = new StacFrame(graph);
        System.out.println("<-- Create visualization: Done!");

        Tab newTab = new Tab();
        if(filename.equals(""))
            newTab.setText("Sample");
        else
            newTab.setText(filename);
        newTab.setContent(newFrame);
        this.tabPane.getTabs().add(newTab);
        System.out.println("Load graph: done!");

        tabPane.getSelectionModel().select(newTab);
    }

    public StacFrame getCurrentFrame() {
        Tab currentTab = this.getCurrentTab();
        if(currentTab.getContent() instanceof StacFrame)
            return (StacFrame) currentTab.getContent();
        else
            return null;
    }
    
    public Tab getCurrentTab() {
        return this.tabPane.getSelectionModel().getSelectedItem();
    }
    
    
}
