package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.ucombinator.jaam.visualizer.controllers.VizPanelController;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutMethodVertex;
import org.ucombinator.jaam.visualizer.graph.Instruction;
import org.ucombinator.jaam.visualizer.main.Main;

public class CodeArea extends TextFlow
{
    private static final Color fxColorSelection = Color.ALICEBLUE;
    private static final Font jfxFont = new Font("Serif", 14);

    ArrayList<Instruction> description;
    private int currentCaret = 0;
    
    public CodeArea()
    {
        description = new ArrayList<Instruction>();
    }

    EventHandler<MouseEvent> onMouseClickedEventHandler = new javafx.event.EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            event.consume();
            Text lineText = (Text) event.getSource();
            int row = CodeArea.this.getChildren().indexOf(lineText);
            Instruction lineInstr = CodeArea.this.description.get(row);

            VizPanelController mainPanel = Main.getSelectedVizPanelController();

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
            Main.getSelectedMainTabController().repaintAll();
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
            line.setFont(jfxFont);
        }
    }

    // Rewrite the text area based on which vertices are highlighted
    public void setDescription()
    {
        VizPanelController mainPanel = Main.getSelectedVizPanelController();
        HashSet<AbstractLayoutVertex> highlighted = mainPanel.getHighlighted();
        if(highlighted.size() > 0)
        {
            HashSet<LayoutMethodVertex> methodVertices = new LinkedHashSet<>();
            for(AbstractLayoutVertex v : highlighted)
                methodVertices.addAll(v.getMethodVertices());

            description = new ArrayList<>();
            for(LayoutMethodVertex v : methodVertices) {
                String methodName = v.getMethodName();
                //ArrayList<Instruction> currInstructions = new ArrayList<>(v.getInstructions());
                //Collections.sort(currInstructions);
                //System.out.println(currInstructions.size());

                // Add header line with method name and blank separator line after
                description.add(new Instruction(methodName, methodName, -1, false));
                //description.addAll(currInstructions);
                //description.add(new Instruction("", methodName, -1, false));
                //System.out.println("Instructions printed: " + description.size());
            }

            this.writeText();
            this.setHighlights(fxColorSelection);
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
