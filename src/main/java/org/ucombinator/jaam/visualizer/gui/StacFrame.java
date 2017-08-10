package org.ucombinator.jaam.visualizer.gui;

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

import java.net.URL;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.layout.LayoutRootVertex;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.main.Parameters;
import org.ucombinator.jaam.visualizer.graph.Graph;

/**
 * JFrame showing a map
 * @author Jawaherul
 *
 */

public class StacFrame extends Tab {
    @FXML
    private VizPanel mainPanel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private CodeArea bytecodeArea;
    @FXML
    private SearchResults searchResults;

    @FXML
    private CheckBox showEdges;
    @FXML
    private CheckBox showLabels;
    @FXML
    private Button methodCollapse;
    @FXML
    private Button chainCollapse;

    private Graph graph;
    boolean methodsExpanded, chainsExpanded;
    private final javafx.scene.paint.Color activeColor = javafx.scene.paint.Color.CYAN;
    private final javafx.scene.paint.Color inactiveColor = javafx.scene.paint.Color.BLACK;
    private boolean edgeVisible, labelsVisible;

    public enum searchType {
        ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
    }

    public StacFrame(Graph graph) {
        methodsExpanded = true;
        chainsExpanded = true;
        edgeVisible = true;
        labelsVisible = false; // If you change this, also change the initialization for AbstractLayoutVertex
        this.graph = graph;
        try {
            URL url = getClass().getResource("/tab.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            fxmlLoader.setController(this);
            System.out.println("Loading url: " + url);
            BorderPane borderPane = fxmlLoader.load();
            this.setContent(borderPane);
            System.out.println("Border pane loaded: " + borderPane);
            System.out.println("VizPanel loaded: " + this.mainPanel);
            this.mainPanel.setStacFrame(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mainPanel.initFX(this.graph);
    }

    public VizPanel getMainPanel() {
        return this.mainPanel;
    }

    public TextArea getRightArea() {
        return this.descriptionArea;
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

    public void repaintAll() {
        System.out.println("Repainting all...");
        if (!Parameters.debugPanelMode) {
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
        System.out.println("Edges checkbox set to: " + showEdges.isSelected());
        edgeVisible = showEdges.isSelected();
        mainPanel.getPanelRoot().setVisible(false);
        mainPanel.getPanelRoot().setEdgeVisibility(edgeVisible);
        LayoutEdge.redrawEdges(mainPanel.getPanelRoot(), true);
        mainPanel.getPanelRoot().setVisible(true);
    }

    public void showLabelsAction(ActionEvent event) {
        labelsVisible = showLabels.isSelected();
        mainPanel.getPanelRoot().setVisible(false);
        mainPanel.getPanelRoot().setLabelVisibility(labelsVisible);
        mainPanel.getPanelRoot().setVisible(true);
    }

    public void xScalePanelMinusAction(ActionEvent event) {
        StacFrame.this.mainPanel.decrementScaleXFactor();
        StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
        StacFrame.this.mainPanel.resetRootPosition(false);
    }

    public void xScalePanelPlusAction(ActionEvent event) {
        StacFrame.this.mainPanel.incrementScaleXFactor();
        StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
        StacFrame.this.mainPanel.resetRootPosition(false);
    }

    public void yScalePanelMinusAction(ActionEvent event) {
        StacFrame.this.mainPanel.decrementScaleYFactor();
        StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
        StacFrame.this.mainPanel.resetRootPosition(false);
    }

    public void yScalePanelPlusAction(ActionEvent event) {
        StacFrame.this.mainPanel.incrementScaleYFactor();
        StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
        StacFrame.this.mainPanel.resetRootPosition(false);
    }

    public void methodCollapseAction(ActionEvent event) {
        methodsExpanded = !methodsExpanded;
        StacFrame.this.mainPanel.getPanelRoot().toggleNodesOfType(AbstractLayoutVertex.VertexType.METHOD,
                methodsExpanded);

        if (methodCollapse.getTextFill() == activeColor) {
            methodCollapse.setTextFill(inactiveColor);
        } else {
            methodCollapse.setTextFill(activeColor);
        }

        StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
        StacFrame.this.mainPanel.resetRootPosition(false);
    }

    public void chainCollapseAction(ActionEvent event) {
        chainsExpanded = !chainsExpanded;
        StacFrame.this.mainPanel.getPanelRoot()
                .toggleNodesOfType(AbstractLayoutVertex.VertexType.CHAIN, chainsExpanded);
        if (chainCollapse.getTextFill() == activeColor) {
            chainCollapse.setTextFill(inactiveColor);
        } else {
            chainCollapse.setTextFill(activeColor);
        }

        StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
        StacFrame.this.mainPanel.resetRootPosition(false);
    }

    public void exportImageAction(ActionEvent event) {
        event.consume(); // TODO: Is this necessary?
        String extension = "png";
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(extension.toUpperCase() + " files (*." + extension + ")", "*." + extension);
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName(Main.getSelectedStacFrame().getText() + "." + extension);

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
    public void initSearch(searchType search) {
        this.mainPanel.resetHighlighted(null);
        String query = getSearchInput(search);

        if (search == searchType.ID)
            searchByID(query); // TODO: Fix inconsistency with panel root
        else if (search == searchType.INSTRUCTION)
            this.mainPanel.getPanelRoot().searchByInstruction(query, mainPanel);
        else if (search == searchType.METHOD)
            this.mainPanel.getPanelRoot().searchByMethod(query, mainPanel);

        this.repaintAll();
        /*Parameters.bytecodeArea.clear();
        Parameters.rightArea.setText("");*/
    }

    public String getSearchInput(searchType search) {
        String title = "";
        System.out.println("Search type: " + search);
        if (search == searchType.ID || search == searchType.ROOT_PATH) {
            title = "Enter node ID(s)";
        } else if (search == searchType.INSTRUCTION) {
            title = "Instruction contains ...";
        } else if (search == searchType.METHOD) {
            title = "Method name contains ...";
        } else if (search == searchType.OUT_OPEN || search == searchType.OUT_CLOSED || search == searchType.IN_OPEN
                || search == searchType.IN_CLOSED) {
            title = "Enter node ID";
        }

        String input = "";
        if (search != searchType.ALL_LEAVES && search != searchType.ALL_SOURCES && search != searchType.TAG) {
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
        Main.getSelectedStacFrame().getMainPanel().resetRootPosition(true);
    }

    public void zoomInPressed(MouseEvent event) {
        Main.getSelectedStacFrame().keepButton(1, (Button) event.getSource());
    }

    public void zoomOutPressed(MouseEvent event) {
        Main.getSelectedStacFrame().keepButton(-1, (Button) event.getSource());
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
