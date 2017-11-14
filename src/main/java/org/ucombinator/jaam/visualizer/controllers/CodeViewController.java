package org.ucombinator.jaam.visualizer.controllers;

import com.strobel.decompiler.languages.java.ast.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.ucombinator.jaam.visualizer.codeView.CodeHighlighter;
import org.ucombinator.jaam.visualizer.codeView.CodeTab;
import org.ucombinator.jaam.visualizer.gui.SelectEvent;
import org.ucombinator.jaam.visualizer.layout.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class CodeViewController {

    @FXML public final VBox root = null; // Initialized by Controllers.loadFXML()
    @FXML public final TabPane codeTabs = null; // Initialized by Controllers.loadFXML()

    HashMap<String, CodeTab> tabMap;

    public CodeViewController(List<CompilationUnit> compilationUnits) throws IOException {
        Controllers.loadFXML("/CodeView.fxml", this);

        this.tabMap = new HashMap<>();

        for(CompilationUnit u : compilationUnits)
            addClass(u);
    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.VERTEX_SELECTED, onVertexSelect);
    }

    public void addClass(CompilationUnit unit)
    {
        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;

        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        typeDeclaration.getText();

        String className = typeDeclaration.getName();
        String fullClassName = new String(unit.getPackage().getName() + "." + className);

        this.tabMap.put(fullClassName, new CodeTab(unit, className, fullClassName));
    }

    EventHandler<SelectEvent> onVertexSelect = new EventHandler<SelectEvent>() {
        @Override
        public void handle(SelectEvent selectEvent) {

            AbstractLayoutVertex av = selectEvent.getVertex();

            if(av instanceof CodeEntity)
            {
                CodeEntity v = (CodeEntity)av;

                CodeTab t = tabMap.get(v.getClassName());

                if(!isDisplayed(t))
                {
                    codeTabs.getTabs().add(t);
                }

                codeTabs.getSelectionModel().select(t);
            }
        }
    };

    boolean isDisplayed(CodeTab t)
    {
        return codeTabs.getTabs().stream().filter(
                c-> ((CodeTab)c).fullClassName.equals(t.fullClassName)).findFirst().orElse(null)
                != null;
    }

    /*
    private class ClassInfo{
        public CompilationUnit unit;
        public CodeArea codeArea;
        public StackPane stackPane;
        public boolean isDisplayed;
        private HashMap<String, ParagraphRange> methodParagraphs;
        public String highlightedMethod;

        public ClassInfo(CompilationUnit unit)
        {
            this.unit = unit;
            this.isDisplayed = false;

            this.methodParagraphs = new HashMap<>();
            this.highlightedMethod = null;
        }

        public void addMethod(String methodName, int begin, int end)
        {
            this.methodParagraphs.put(methodName, new ParagraphRange(begin, end));
        }

        public ParagraphRange getMethodParagraph(String methodName)
        {
            return methodParagraphs.get(methodName);
        }

        public class ParagraphRange{

            ParagraphRange(int begin, int end)
            {
                this.begin = begin;
                this.end = end;
            }

            public int begin;
            public int end;
        }

    }
    */

}
