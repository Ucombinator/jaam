package org.ucombinator.jaam.visualizer.controllers;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;

import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.ucombinator.jaam.visualizer.gui.CodeArea;
import org.ucombinator.jaam.visualizer.gui.SearchResults;
import org.ucombinator.jaam.visualizer.gui.VizPanel;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.layout.LayoutRootVertex;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.graph.Graph;

public class MainTabController {
    @FXML private BorderPane root;
    public BorderPane getRoot() { return root; }

    @FXML private VizPanel mainPanel;
    public VizPanel getMainPanel() { return this.mainPanel; }

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

    public enum SearchType {
        ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
    }

    public MainTabController(Graph graph) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/tab.fxml"));
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // Using instead of Platform.exit because we want a non-zero exit code
        }

        this.mainPanel.setMainTabController(this);
        this.mainPanel.initFX(graph);
    }

    private static final boolean debugPanelMode = false;
    public void repaintAll() {
        System.out.println("Repainting all...");
        if (!debugPanelMode) {
            bytecodeArea.setDescription();
            setRightText();
            searchResults.writeText(this.mainPanel);
        }
    }

    public void setRightText() {
        StringBuilder text = new StringBuilder();
        for (AbstractLayoutVertex v : this.mainPanel.getHighlighted())
            text.append(v.getRightPanelContent() + "\n");

        this.getRightArea().setText(text.toString());
    }

    public void showEdgesAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot().setVisible(false);
        this.getMainPanel().getPanelRoot().setEdgeVisibility(showEdges.isSelected());
        LayoutEdge.redrawEdges(mainPanel.getPanelRoot(), true);
        this.getMainPanel().getPanelRoot().setVisible(true);
    }

    public void showLabelsAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot().setVisible(false);
        this.getMainPanel().getPanelRoot().setLabelVisibility(showLabels.isSelected());
        this.getMainPanel().getPanelRoot().setVisible(true);
    }

    public void xScalePanelMinusAction(ActionEvent event) {
        this.getMainPanel().decrementScaleXFactor();
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
    }

    public void xScalePanelPlusAction(ActionEvent event) {
        this.getMainPanel().incrementScaleXFactor();
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
    }

    public void yScalePanelMinusAction(ActionEvent event) {
        this.getMainPanel().decrementScaleYFactor();
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
    }

    public void yScalePanelPlusAction(ActionEvent event) {
        this.getMainPanel().incrementScaleYFactor();
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
    }

    public void methodCollapseAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot().toggleNodesOfType(AbstractLayoutVertex.VertexType.METHOD,
                methodsExpanded.isSelected());
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
    }

    public void chainCollapseAction(ActionEvent event) {
        this.getMainPanel().getPanelRoot()
                .toggleNodesOfType(AbstractLayoutVertex.VertexType.CHAIN, chainsExpanded.isSelected());
        this.getMainPanel().resetAndRedraw(showEdges.isSelected());
        this.getMainPanel().resetRootPosition(false);
    }

    public void exportImageAction(ActionEvent event) {
        event.consume(); // TODO: Is this necessary?
        String extension = "png";
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(extension.toUpperCase() + " files (*." + extension + ")", "*." + extension);
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName(Main.getSelectedStacTab().getText() + "." + extension);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(Main.getMainPane().getRoot().getScene().getWindow());

        if (file != null) {
            WritableImage image = mainPanel.snapshot(new SnapshotParameters(), null);

            System.out.println(file.getAbsolutePath());
            // TODO: probably use a file chooser here
            File newFile = new File(file.getAbsolutePath());

            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), extension, newFile);
            } catch (IOException exception) {
                System.out.println(exception);
            }
        }
    }

    // Clean up info from previous searches
    public void initSearch(SearchType search) {
        this.mainPanel.resetHighlighted(null);
        String query = getSearchInput(search);

        if (search == SearchType.ID)
            searchByID(query); // TODO: Fix inconsistency with panel root
        else if (search == SearchType.INSTRUCTION)
            this.mainPanel.getPanelRoot().searchByInstruction(query, mainPanel);
        else if (search == SearchType.METHOD)
            this.mainPanel.getPanelRoot().searchByMethod(query, mainPanel);

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
            if (input == null)
                return "";
            else
                input = input.trim();

            if (input.equals(""))
                return "";
        }

        return input;
    }

    public void searchByID(String input) {
        LayoutRootVertex panelRoot = this.mainPanel.getPanelRoot();
        StringTokenizer token = new StringTokenizer(input, ", ");

        int id1, id2;
        String tok;

        while (token.hasMoreTokens()) {
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

    public void resetButtonPressed() {
        Main.getSelectedMainPanel().resetRootPosition(true);
    }

    public void zoomInPressed(MouseEvent event) {
        Main.getSelectedStacTabController().keepButton(1, (Button) event.getSource());
    }

    public void zoomOutPressed(MouseEvent event) {
        Main.getSelectedStacTabController().keepButton(-1, (Button) event.getSource());
    }

    public void zoomReleased(MouseEvent event) {
        this.setZoomButtonReleased(true);
    }

    private boolean zoomEnabled = true;
    private boolean zoomButtonReleased = false;

    public void keepButton(int zoom, Button button) {
        if(zoomEnabled && !zoomButtonReleased) {
            zoomEnabled = false;
            this.mainPanel.zoom(zoom, button);
        }
        if(zoomButtonReleased) {
            zoomButtonReleased = false;
        }
    }

    public void setZoomEnabled(boolean isEnabled) {
        this.zoomEnabled = isEnabled;
    }

    public void setZoomButtonReleased(boolean isReleased) {
        this.zoomButtonReleased = isReleased;
    }
}
