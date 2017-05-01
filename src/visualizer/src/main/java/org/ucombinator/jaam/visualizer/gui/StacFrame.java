package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutAlgorithm;
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

public class StacFrame extends BorderPane
{
	private VizPanel mainPanel;
	private TextArea rightArea;
	private CodeArea bytecodeArea;
	private SearchResults searchResults;
	private Graph graph;

	private SplitPane horizontalSplitPane;
	private FlowPane buttonsFlowPane;
	private BorderPane searchPanel, bytecodePanel, rightPanel;
	private CheckBox showEdge;
	private CheckBox showLabels;

	private boolean edgeVisible;
	private boolean labelsVisible;

	public enum searchType
	{
		ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
	}
	
	public StacFrame(Graph graph)
	{
		this.graph = graph;
		if (Parameters.debugMode)
			makeSimpleLayout();
		else
			makeLayout();

		edgeVisible = true;
		labelsVisible = true;
		this.mainPanel.initFX(this.graph);
		this.setVisible(true);
	}

	public VizPanel getMainPanel() {
		return this.mainPanel;
	}

	public TextArea getRightArea() {
		return this.rightArea;
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

	public void repaintAll()
	{
		System.out.println("Repainting all...");
		if (!Parameters.debugMode)
		{
			bytecodeArea.setDescription();
			setRightText();
			searchResults.writeText(this.mainPanel);
		}
	}

	public void setRightText()
	{
		StringBuilder text = new StringBuilder();
		for(AbstractLayoutVertex v : this.mainPanel.getHighlighted())
			text.append(v.getRightPanelContent() + "\n");

		this.getRightArea().setText(text.toString());
	}

	public void buildCenter(ArrayList<ArrayList<Region>> layout, ArrayList<Double> dividerPositions)
	{
		horizontalSplitPane = new SplitPane();
		horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);
		this.setCenter(horizontalSplitPane);

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
	}

	public void makeLayout()
	{
		setSplitScreen();
		this.setBottom(this.buttonsFlowPane);
		this.setPrefPanelSizes();
		this.setVisible(true);
	}

	public void makeSimpleLayout()
	{
		this.mainPanel = new VizPanel();
		this.setCenter(this.mainPanel);
		this.setVisible(true);
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

		showEdge = new CheckBox("Edges");
		showEdge.setSelected(true);
		showEdge.setOnAction
				(
						new EventHandler<ActionEvent>()
						{
							@Override
							public void handle(ActionEvent e) {
								edgeVisible = showEdge.isSelected();
								mainPanel.getPanelRoot().setVisible(false);
								mainPanel.getPanelRoot().setEdgeVisibility(edgeVisible);
								LayoutEdge.redrawEdges(mainPanel.getPanelRoot(), true);
								mainPanel.getPanelRoot().setVisible(true);
							}
						}
				);
		controlPanel.add(showEdge, 0, 0);
		
		
		showLabels = new CheckBox("Labels");
		showLabels.setSelected(true);
		showLabels.setOnAction
				(
						new EventHandler<ActionEvent>()
						{
							@Override
							public void handle(ActionEvent e) {
								labelsVisible = showLabels.isSelected();
								mainPanel.getPanelRoot().setVisible(false);
								mainPanel.getPanelRoot().setLabelVisibility(labelsVisible);
								mainPanel.getPanelRoot().setVisible(true);
							}
						}
				);
		controlPanel.add(showLabels, 0, 1);
		
		
		
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
								StacFrame.this.mainPanel.decrementScaleXFactor();
								StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
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
								StacFrame.this.mainPanel.incrementScaleXFactor();
								StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
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
								StacFrame.this.mainPanel.decrementScaleYFactor();
								StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
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
								StacFrame.this.mainPanel.incrementScaleYFactor();
								StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
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
								StacFrame.this.mainPanel.getPanelRoot().toggleNodesOfType(AbstractLayoutVertex.VertexType.METHOD,
										methodExpanded);

								if (methodCollapse.getTextFill() == activeColor) {
									methodCollapse.setTextFill(inactiveColor);
								} else {
									methodCollapse.setTextFill(activeColor);
								}

								StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
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
								StacFrame.this.mainPanel.getPanelRoot()
										.toggleNodesOfType(AbstractLayoutVertex.VertexType.CHAIN, chainExpanded);
								if (chainCollapse.getTextFill() == activeColor) {
									chainCollapse.setTextFill(inactiveColor);
								} else {
									chainCollapse.setTextFill(activeColor);
								}

								StacFrame.this.mainPanel.resetAndRedraw(edgeVisible);
							}
						}
				);
		collapsePanel.getChildren().add(chainCollapse);



		FlowPane utilitiesPanel = new FlowPane();
		utilitiesPanel.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.BLACK,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
		buttonsFlowPane.getChildren().add(utilitiesPanel);


		String extension = "png";
		final Button exportImageButton = new Button(extension.toUpperCase());
		exportImageButton.setOnAction
				(
						new EventHandler<ActionEvent>()

						{
							@Override
							public void handle(ActionEvent e) {
								e.consume();

								FileChooser fileChooser = new FileChooser();

								//Set extension filter

								FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(extension.toUpperCase()+" files (*."+extension+")", "*."+extension);
								fileChooser.getExtensionFilters().add(extFilter);
								fileChooser.setInitialFileName(Main.getOuterFrame().getCurrentTab().getText()+"."+extension);

								//Show save file dialog
								File file = fileChooser.showSaveDialog(Main.getOuterFrame().getScene().getWindow());

								if(file != null){
									WritableImage image = mainPanel.snapshot(new SnapshotParameters(), null);

									System.out.println(file.getAbsolutePath());
									// TODO: probably use a file chooser here
									File newFile = new File(file.getAbsolutePath());

									try {
										ImageIO.write(SwingFXUtils.fromFXImage(image, null), extension, newFile);
									} catch (IOException exception) {
										// TODO: handle exception here
									}					            }


							}
						}
				);
		utilitiesPanel.getChildren().add(exportImageButton);


		// TODO: Set sizes to fill parent. (Right now we just make the sizes all very large.)
		bytecodePanel = new BorderPane();
		Label leftLabel = new Label("Code");
		ScrollPane scrollLeft = new ScrollPane();
		this.bytecodeArea = new CodeArea();
		this.bytecodeArea.setStyle("-fx-padding: 0 0 0 5"); // Add left margin of five pixels
		scrollLeft.setContent(this.bytecodeArea);
		bytecodePanel.setTop(leftLabel);
		bytecodePanel.setCenter(scrollLeft);

		rightPanel = new BorderPane();
		Label rightLabel = new Label("Description");
		ScrollPane rightScroll = new ScrollPane();
		this.rightArea = new TextArea();
		this.rightArea.setEditable(false);
		rightScroll.setContent(this.rightArea);
		rightPanel.setTop(rightLabel);
		rightPanel.setCenter(rightScroll);

		searchPanel = new BorderPane();
		Label searchL = new Label("Search Results");
		this.searchResults = new SearchResults();
		ScrollPane scrollS = new ScrollPane();
		scrollS.setContent(this.searchResults);
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

	public void setPrefPanelSizes() {
		this.rightArea.setPrefColumnCount(100);
		this.rightArea.setPrefRowCount(100);
		this.searchResults.setPrefSize(1000, 500);
	}

	// Clean up info from previous searches
	public void initSearch(searchType search)
	{
		this.mainPanel.resetHighlighted(null);
		String query = getSearchInput(search);

		if(search == searchType.ID)
			searchByID(query); // TODO: Fix inconsistency with panel root
		else if(search == searchType.INSTRUCTION)
			this.mainPanel.getPanelRoot().searchByInstruction(query, mainPanel);
		else if(search == searchType.METHOD)
			this.mainPanel.getPanelRoot().searchByMethod(query, mainPanel);

		this.repaintAll();
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
		LayoutRootVertex panelRoot = this.mainPanel.getPanelRoot();
		StringTokenizer token = new StringTokenizer(input,", ");

		int id1, id2;
		String tok;

		while(token.hasMoreTokens()) {
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
}
