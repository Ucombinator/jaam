package graph;

import java.util.Random;
import java.util.List;

import gui.RectangleCell;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

import gui.ZoomableScrollPane;
import gui.CellLayer;
import gui.Cell;

public class Graph {
	public static int width = 1000;
	public static int height = 1000;

	private Model model;
	private Group canvas;
	private ZoomableScrollPane scrollPane;
	MouseGestures mouseGestures;

	/**
	 * the pane wrapper is necessary or else the scrollpane would always align
	 * the top-most and left-most child to the top and left eg when you drag the
	 * top child down, the entire scrollpane would move down
	 */
	CellLayer cellLayer;

	public Graph() {

		this.model = new Model();

		canvas = new Group();
		cellLayer = new CellLayer();

		canvas.getChildren().add(cellLayer);

		mouseGestures = new MouseGestures(this);

		scrollPane = new ZoomableScrollPane(canvas);

		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);

	}

	public ScrollPane getScrollPane() {
		return this.scrollPane;
	}

	public Pane getCellLayer() {
		return this.cellLayer;
	}

	public Model getModel() {
		return model;
	}

	public void beginUpdate() {
	}

	public void endUpdate() {

		// add components to graph pane
		getCellLayer().getChildren().addAll(model.getAddedEdges());
		getCellLayer().getChildren().addAll(model.getAddedCells());

		// remove components from graph pane
		getCellLayer().getChildren().removeAll(model.getRemovedCells());
		getCellLayer().getChildren().removeAll(model.getRemovedEdges());

		// enable dragging of cells
		for (Cell cell : model.getAddedCells()) {
			mouseGestures.makeDraggable(cell);
		}

		// every cell must have a parent, if it doesn't, then the graphParent is
		// the parent
		getModel().attachOrphansToGraphParent(model.getAddedCells());

		// remove reference to graphParent
		getModel().disconnectFromGraphParent(model.getRemovedCells());

		// merge added & removed cells with all cells
		getModel().merge();

		System.out.println("Finished setting up graph...");
	}

	public double getScale() {
		return this.scrollPane.getScaleValue();
	}

	public void randomLayout() {
		Random rnd = new Random();
		int index = 0;
		for (Cell cell : this.getModel().getAllCells()) {

			if(index%100 == 0)
				System.out.println("Relocating cell " + index);
			double x = rnd.nextDouble() * width;
			double y = rnd.nextDouble() * height;

			cell.relocate(0, 0);
			index++;
		}

		System.out.println("Finished layout...");
	}

	public void gridLayout() {
		int numCells = this.getModel().getAllCells().size();
		int heightPerCell = RectangleCell.dim + 5;
		int numRows = Graph.height/heightPerCell;
		int cellsPerRow = numCells/numRows;
		if (cellsPerRow == 0)
			cellsPerRow = 1;
		int widthPerCell = Graph.width/cellsPerRow;
		int index = 0;
		System.out.println("Height: " + heightPerCell);
		System.out.println("Width: " + widthPerCell);
		System.out.println("Cells per row: " + cellsPerRow);
		System.out.println("Rows: " + numRows);
		for (Cell cell : this.getModel().getAllCells()) {

			if(index%100 == 0)
				System.out.println("Relocating cell " + index);
			int xIndex = index % cellsPerRow;
			int yIndex = index / cellsPerRow;
			//cell.setTranslateX(xIndex*widthPerCell);
			//cell.setTranslateY(yIndex*heightPerCell);
			cell.relocate(xIndex*widthPerCell, yIndex*heightPerCell);
			index++;
		}

		System.out.println("Finished layout...");
	}
}
