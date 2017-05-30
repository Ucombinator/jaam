package org.ucombinator.jaam.visualizer.gui;


import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutMethodVertex;
import org.ucombinator.jaam.visualizer.graph.Instruction;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.main.Parameters;

public class CodeArea extends TextFlow
{
	ArrayList<Instruction> description;
    private int currentCaret = 0;
	
	public CodeArea()
	{
		description = new ArrayList<Instruction>();

		// TODO: Is there a  JavaFX equivalent for this?
		//((DefaultCaret)this.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
	}

	EventHandler<MouseEvent> onMouseClickedEventHandler = new javafx.event.EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			event.consume();
			Text lineText = (Text) event.getSource();
			int row = CodeArea.this.getChildren().indexOf(lineText);
			Instruction lineInstr = CodeArea.this.description.get(row);

			VizPanel mainPanel = ((StacFrame) Main.getOuterFrame().getCurrentFrame()).getMainPanel();

			if (event.isShiftDown()) {
				if (lineInstr.isRealInstruction()) {
					if (lineInstr.isSelected()) {
						mainPanel.searchByJimpleIndex(
								lineInstr.getMethodName(), lineInstr.getJimpleIndex(), false, false);
					} else {
						mainPanel.searchByJimpleIndex(
								lineInstr.getMethodName(), lineInstr.getJimpleIndex(), false, true);
					}
				}
			} else {
				if (lineInstr.isRealInstruction()) {
					mainPanel.searchByJimpleIndex(
							lineInstr.getMethodName(), lineInstr.getJimpleIndex(), true, true);
				}
			}
			Main.getOuterFrame().getCurrentFrame().repaintAll();
		}
	};

	public void clear()
	{
		this.description = new ArrayList<Instruction>();
		this.writeText();
	}

	public void resetFont() {
		for(Node textNode : this.getChildren()) {
			Text line = (Text) textNode;
			line.setFont(Parameters.jfxFont);
		}
	}

	// Rewrite the text area based on which vertices are highlighted
	public void setDescription()
	{
		VizPanel mainPanel = ((StacFrame) Main.getOuterFrame().getCurrentFrame()).getMainPanel();
		HashSet<AbstractLayoutVertex> highlighted = mainPanel.getHighlighted();
		if(highlighted.size() > 0)
		{
			//TODO: Add function for getting all methods
			HashSet<LayoutMethodVertex> methodVertices = new LinkedHashSet<LayoutMethodVertex>();
			for(AbstractLayoutVertex v : highlighted)
				methodVertices.addAll(v.getMethodVertices());

			description = new ArrayList<Instruction>();
			for(LayoutMethodVertex v : methodVertices) {
				String methodName = v.getMethodName();
				ArrayList<Instruction> currInstructions = new ArrayList<Instruction>(v.getInstructions());
				Collections.sort(currInstructions);
				//System.out.println(currInstructions.size());

				// Add header line with method name and blank separator line after
				description.add(new Instruction(methodName, methodName, -1, false));
				description.addAll(currInstructions);
				description.add(new Instruction("", methodName, -1, false));
				//System.out.println("Instructions printed: " + description.size());
			}

			this.writeText();
			this.setHighlights(Parameters.fxColorSelection);
		}
	}
	
	// Set the text for the area to the sum of all of the lines in the description
	private void writeText()
	{
		this.getChildren().clear();
		for(Instruction line : description) {
			String lineStr = line.getText() + "\n";
			if(line.getJimpleIndex() >= 0) // Print line numbers for actual instruction lines
				lineStr = line.getJimpleIndex() + " " + lineStr;

			Text lineText = new Text(lineStr);
			lineText.setOnMouseClicked(onMouseClickedEventHandler);
			this.getChildren().add(lineText);
		}
	}
	
	private void setHighlights(Color lineSelectionColor)
	{
		for(int i = 0; i < this.description.size(); i++)
		{
			Instruction line = this.description.get(i);
			Text lineText = (Text) this.getChildren().get(i);
            /*if(line.isSelected()) {
            	lineText.setStyle("-fx-highlight-fill: orange");
				//lineText.setFill(lineSelectionColor);
			}
			else {
				lineText.setStyle("-fx-highlight-fill: white");
            	//lineText.setFill(Color.WHITE);
			}*/
		}
	}
}
