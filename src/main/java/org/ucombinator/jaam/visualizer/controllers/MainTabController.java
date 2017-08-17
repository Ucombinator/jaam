package org.ucombinator.jaam.visualizer.controllers;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.main.Main;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class MainTabController {
    public final Tab tab;

    @FXML private BorderPane root;
    public BorderPane getRoot() { return root; }

    @FXML private Pane centerPane; // TODO: rename
    public Pane getCenterPane() { return this.centerPane; }

    private final VizPanelController vizPanelController;

    private VizPanel mainPanel; // TODO: rename to "vizPanel"
    public VizPanel getVizPanel() { return this.mainPanel; }

    @FXML private TextArea descriptionArea;
    public TextArea getRightArea() { return this.descriptionArea; }

    @FXML private CodeArea bytecodeArea;
    public CodeArea getBytecodeArea() { return this.bytecodeArea; }

    @FXML private SearchResults searchResults;
    public SearchResults getSearchResults() { return this.searchResults; }

    @FXML private CheckBox showEdges;
    @FXML private CheckBox showLabels;
    @FXML private CheckBox methodsExpanded;
    @FXML private CheckBox chainsExpanded;

    @FXML private Spinner<Double> zoomSpinner;

    public enum SearchType {
        ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
    }

    public MainTabController(File file, Graph graph) throws IOException {
        Controllers.loadFXML("/MainTabContent.fxml", this);
        this.vizPanelController = new VizPanelController(file, graph);
        this.mainPanel = this.vizPanelController.getMainPanel();
        this.centerPane.getChildren().add(this.vizPanelController.getRoot());

        this.mainPanel.initFX(graph);
        this.tab = new Tab(file.getName(), this.getRoot());
        this.tab.tooltipProperty().set(new Tooltip(file.getAbsolutePath()));
        Controllers.put(this.tab, this);
    }

    public void repaintAll() {
        System.out.println("Repainting all...");
        bytecodeArea.setDescription();
        setRightText();
        searchResults.writeText(this.mainPanel);
    }

    public void setRightText() {
        StringBuilder text = new StringBuilder();
        for (AbstractLayoutVertex v : this.mainPanel.getHighlighted()) {
            text.append(v.getRightPanelContent() + "\n");
        }

        this.getRightArea().setText(text.toString());
    }

    // Clean up info from previous searches
    public void initSearch(SearchType search) {
        this.mainPanel.resetHighlighted(null);
        String query = getSearchInput(search);

        if (search == SearchType.ID) {
            searchByID(query); // TODO: Fix inconsistency with panel root
        } else if (search == SearchType.INSTRUCTION) {
            this.mainPanel.getPanelRoot().searchByInstruction(query, mainPanel);
        } else if (search == SearchType.METHOD) {
            this.mainPanel.getPanelRoot().searchByMethod(query, mainPanel);
        }

        this.repaintAll();
        /*Parameters.bytecodeArea.clear();
        Parameters.rightArea.setText("");*/
    }

    public String getSearchInput(SearchType search) {
        String title = "";
        System.out.println("Search type: " + search);
        if (search == SearchType.ID || search == SearchType.ROOT_PATH) {
            title = "Enter node ID(s)";
        } else if (search == SearchType.INSTRUCTION) {
            title = "Instruction contains ...";
        } else if (search == SearchType.METHOD) {
            title = "Method name contains ...";
        } else if (search == SearchType.OUT_OPEN || search == SearchType.OUT_CLOSED || search == SearchType.IN_OPEN
                || search == SearchType.IN_CLOSED) {
            title = "Enter node ID";
        }

        String input = "";
        if (search != SearchType.ALL_LEAVES && search != SearchType.ALL_SOURCES && search != SearchType.TAG) {
            System.out.println("Showing dialog...");
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText(title);
            dialog.showAndWait();
            input = dialog.getResult();
            if (input == null) {
                return "";
            } else {
                input = input.trim();
            }

            if (input.equals("")) {
                return "";
            }
        }

        return input;
    }

    public void searchByID(String input) {
        for (String token : input.split(", ")) {
            if (token.trim().equalsIgnoreCase("")) {
                /* Do nothing */
            } else if (token.indexOf('-') == -1) {
                int id1 = Integer.parseInt(token.trim());
                this.mainPanel.getPanelRoot().searchByID(id1, mainPanel);
            } else {
                int id1 = Integer.parseInt(token.substring(0, token.indexOf('-')).trim());
                int id2 = Integer.parseInt(token.substring(token.lastIndexOf('-') + 1).trim());
                this.mainPanel.getPanelRoot().searchByIDRange(id1, id2, mainPanel);
            }
        }
    }
}
