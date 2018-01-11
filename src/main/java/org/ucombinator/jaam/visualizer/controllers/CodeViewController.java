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
import soot.SootClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodeViewController {

    @FXML public final VBox root = null; // Initialized by Controllers.loadFXML()
    @FXML public final TabPane codeTabs = null; // Initialized by Controllers.loadFXML()

    HashMap<String, CodeTab> tabMap;
    HashSet<String> classNames;

    public CodeViewController(List<CompilationUnit> compilationUnits, Set<SootClass> sootClasses) throws IOException {
        Controllers.loadFXML("/CodeView.fxml", this);

        this.tabMap     = new HashMap<>();
        this.classNames = new HashSet<>();

        for (SootClass s : sootClasses)
        {
            System.out.println(s.getName());
        }

        HashMap<String,SootClass> lookup = new HashMap<String,SootClass>();

        sootClasses.stream().forEach(s -> lookup.put(s.getName(), s));


        for (CompilationUnit u : compilationUnits) {
            String fullClassName = getFullClassName(u);

            SootClass s = lookup.get(fullClassName);

            if (s == null)
            {
                System.out.println("ERROR Didn't find soot class matching " + fullClassName);
            }

            addClass(u, s);
        }
    }

    public void addSelectHandler(BorderPane centerPane) {
        centerPane.addEventHandler(SelectEvent.STATE_VERTEX_SELECTED, onVertexSelect);
    }

    public void addClass(CompilationUnit unit, SootClass s)
    {
        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;

        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        typeDeclaration.getText();

        String className = typeDeclaration.getName();
        String fullClassName = new String(unit.getPackage().getName() + "." + className);

        System.out.println("Soot class name: " + s.getName());
        CodeTab tab = new CodeTab(unit, s, className, fullClassName);
        tab.setTooltip(new Tooltip(fullClassName));
        this.tabMap.put(fullClassName, tab);

        this.classNames.add(fullClassName);
    }

    public HashSet<String> getClassNames() {
        return classNames;
    }

    EventHandler<SelectEvent<StateVertex>> onVertexSelect = new EventHandler<SelectEvent<StateVertex>>() {
        @Override
        public void handle(SelectEvent<StateVertex> selectEvent) {

            StateVertex av = selectEvent.getVertex();

            if(av instanceof CodeEntity)
            {
                CodeEntity v = (CodeEntity)av;

                CodeTab t = tabMap.get(v.getClassName());

                if(t == null)
                {
                    System.out.println("Didn't find code associated to " + v.getClassName());
                    return;
                }

                if(!isDisplayed(t))
                {
                    codeTabs.getTabs().add(t);
                }

                codeTabs.getSelectionModel().select(t);
                t.highlightMethod(v.getMethodName());
            }
        }
    };

    boolean isDisplayed(CodeTab t)
    {
        return codeTabs.getTabs().stream().filter(
                c-> ((CodeTab)c).fullClassName.equals(t.fullClassName)).findFirst().orElse(null)
                != null;
    }


    private String getFullClassName(CompilationUnit u)
    {
        return u.getPackage().getName() + "." + u.getTypes().firstOrNullObject().getName();
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
