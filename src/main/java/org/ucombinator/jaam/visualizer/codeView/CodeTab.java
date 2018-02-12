package org.ucombinator.jaam.visualizer.codeView;

import com.strobel.decompiler.languages.EntityType;
import com.strobel.decompiler.languages.java.ast.*;
import com.sun.org.apache.bcel.internal.classfile.Code;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.ucombinator.jaam.util.Soot;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.taint.FieldSelectEvent;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;

public class CodeTab extends Tab{

    public final String shortClassName;
    public final String fullClassName;

    private CodeArea javaCodeArea;
    private CodeArea sootCodeArea;

    private HashMap<String, IndexRange> javaMethodParagraphs;
    private HashMap<String, IndexRange> sootMethodParagraphs;
    private IndexRange javaCurrentlySelected;
    private IndexRange sootCurrentlySelected;

    private HashMap<Integer, String> sootParagraphField;

    public CodeTab(CompilationUnit unit, SootClass soot, String shortClassName, String fullClassName) {
        super(shortClassName);

        this.shortClassName = shortClassName;
        this.fullClassName = fullClassName;
        this.setId(this.fullClassName);

        this.javaMethodParagraphs = new HashMap<>();
        this.sootMethodParagraphs = new HashMap<>();
        this.sootParagraphField   = new HashMap<>();

        this.setContent(generateCodeAreas(unit, soot));

        this.javaCurrentlySelected = new IndexRange(0,0); // Empty range
        this.sootCurrentlySelected = new IndexRange(0,0); // EMpty range
    }

    private Node generateCodeAreas(CompilationUnit unit, SootClass soot)
    {
        assert unit != null;

        this.javaCodeArea = buildJavaCodeArea(unit);
        VirtualizedScrollPane<CodeArea> javaScrollPane = new VirtualizedScrollPane<>(javaCodeArea);
        //javaScrollPane.setMaxHeight(Double.MAX_VALUE);

        this.sootCodeArea = buildSootCodeArea(soot);
        VirtualizedScrollPane<CodeArea> sootScrollPane = new VirtualizedScrollPane<>(sootCodeArea);
        //sootScrollPane.setMaxHeight(Double.MAX_VALUE);
        sootScrollPane.setVisible(false);

        StackPane stackPane = new StackPane(javaScrollPane, sootScrollPane);
        stackPane.setMaxWidth(Double.MAX_VALUE);
        stackPane.setMaxHeight(Double.MAX_VALUE);

        ToggleButton viewToggle = new ToggleButton("Soot");

        viewToggle.selectedProperty().addListener( (observable, oldValue, newValue) -> {
                    javaScrollPane.setVisible(!newValue);
                    sootScrollPane.setVisible( newValue);
                    viewToggle.setText( newValue ? "Java" : "Soot");
                } );

        stackPane.setAlignment(viewToggle, Pos.TOP_RIGHT);

        stackPane.getChildren().add(viewToggle);

        return stackPane;
    }


    private CodeArea buildJavaCodeArea(CompilationUnit unit)
    {
        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(false);

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, CodeHighlighter.computeHighlighting(codeArea.getText()));
                });


        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;
        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        // Preamble

        codeArea.appendText(unit.getPackage().getText());

        unit.getImports().stream().forEach(i -> codeArea.appendText(i.getText()));

        // Class Declaration

        StringBuilder classDeclaration = new StringBuilder("class ");

        {
            Identifier className = typeDeclaration.getChildrenByRole(Roles.IDENTIFIER).firstOrNullObject();
            if (!className.isNull()) {
                classDeclaration.append(className.getText());
            } else {
                classDeclaration.append("?????");
            }
        }
        {
            AstType baseType = typeDeclaration.getChildrenByRole(Roles.BASE_TYPE).firstOrNullObject();
            if (!baseType.isNull()) {
                classDeclaration.append(" extends ");
                classDeclaration.append(baseType.getText());
            }
        }
        {
            AstNodeCollection<AstType> interfaces = typeDeclaration.getChildrenByRole(Roles.IMPLEMENTED_INTERFACE);
            if(!interfaces.isEmpty())
            {
                classDeclaration.append(" implements ");
                interfaces.stream().forEach(i -> classDeclaration.append(i.getText() + ", "));
            }
        }

        codeArea.appendText(classDeclaration.toString() + "\n\n");

        // Methods

        for(AstNode i: typeDeclaration.getChildrenByRole(Roles.TYPE_MEMBER))
        {
            EntityDeclaration entity = (EntityDeclaration)i;

            int startParagraph = codeArea.getParagraphs().size()-1;
            if(entity.getEntityType() == EntityType.METHOD || entity.getEntityType() == EntityType.CONSTRUCTOR)
                codeArea.appendText("\n");
            codeArea.appendText(entity.getText());
            int endParagraph = codeArea.getParagraphs().size()-1;

            if(entity.getEntityType() == EntityType.METHOD || entity.getEntityType() == EntityType.CONSTRUCTOR)
            {
                this.javaMethodParagraphs.put(entity.getName(), new IndexRange(startParagraph, endParagraph));
            }
        }

        codeArea.setMaxHeight(Double.MAX_VALUE);

        return codeArea;
    }

    private CodeArea buildSootCodeArea(SootClass soot) {

        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(false);

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, CodeHighlighter.computeHighlighting(codeArea.getText()));
                });

        if (soot == null) {
            codeArea.appendText("Soot class not found!\n");
            return codeArea;
        }

        codeArea.appendText(soot.getName() + "\n");
        codeArea.appendText(soot.getClass().toString() + "\n\n");

        soot.getFields().stream().forEach(f -> {
            int currentParagraph = codeArea.getCurrentParagraph();
            codeArea.appendText(f.toString() + "\n");
            sootParagraphField.put(currentParagraph, f.getName());
        });

        codeArea.appendText("\n");

        for (SootMethod m : soot.getMethods())
        {
            int startParagraph = codeArea.getParagraphs().size()-1;
            codeArea.appendText(m.getSubSignature() + "\n");
            if (m.isConcrete()) {
                Body body = Soot.getBodyUnsafe(m);
                if (body != null) {
                    body.getUnits().stream().forEach(u ->
                      codeArea.appendText("    " + u.toString() + "\n"));
                }
            }
            codeArea.appendText("\n");
            int endParagraph = codeArea.getParagraphs().size()-1;

            this.sootMethodParagraphs.put(m.getName(), new IndexRange(startParagraph, endParagraph));
        }

        codeArea.selectionProperty().addListener((observableValue, oldIndexRange, newIndexRange) -> {

            if(sootParagraphField.containsKey(codeArea.getCurrentParagraph()))
            {
                //TODO this the correct way but for now we are doing a direct call
                //codeArea.fireEvent(new FieldSelectEvent(codeArea, Main.getSelectedMainTab(),
                //        this.fullClassName + ":" + sootParagraphField.get(codeArea.getCurrentParagraph()) ));

                Main.getSelectedMainTabController().selectFieldInTaintGraph(this.fullClassName, sootParagraphField.get(codeArea.getCurrentParagraph()));
            }
        });

        //Arrays.stream(soot.getClass().getFields()).forEach(f -> System.out.println(f.getName() + "-->" + f.toString() + "-->" + f.toGenericString()));

        return codeArea;
    }

    public void highlightMethod(String methodName) {
        this.javaCurrentlySelected = doHighlightMethod(javaCodeArea, javaCurrentlySelected, javaMethodParagraphs.get(methodName));
        this.sootCurrentlySelected = doHighlightMethod(sootCodeArea, sootCurrentlySelected, sootMethodParagraphs.get(methodName));
    }

    public IndexRange doHighlightMethod(CodeArea codeArea, IndexRange currentlySelected, IndexRange newRange)
    {
        for (int i = currentlySelected.getStart(); i < currentlySelected.getEnd(); ++i)
        {
            codeArea.setParagraphStyle(i, Collections.EMPTY_LIST);
        }

        if(newRange == null)
            return new IndexRange(0,0);

        codeArea.showParagraphAtTop(newRange.getStart() - 1);

        for (int i = newRange.getStart(); i < newRange.getEnd(); ++i) {
            codeArea.setParagraphStyle(i, Collections.singleton("is-selected"));
        }

        return newRange;
    }
}
