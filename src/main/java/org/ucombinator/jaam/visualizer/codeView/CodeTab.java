package org.ucombinator.jaam.visualizer.codeView;

import com.strobel.decompiler.languages.EntityType;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Role;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.Collections;
import java.util.HashMap;

public class CodeTab extends Tab{

    private CompilationUnit unit;
    private StackPane stackPane;
    private VirtualizedScrollPane scrollPane;
    private CodeArea codeArea;
    public final String shortClassName;
    public final String fullClassName;
    private HashMap<String, IndexRange> methodParagraphs;
    private IndexRange currentlySelected;


    public CodeTab(CompilationUnit unit, String shortClassName, String fullClassName) {
        super(shortClassName);
        this.unit = unit;
        this.methodParagraphs = new HashMap<>();

        generateCodeArea(unit);

        this.setContent(this.stackPane);

        this.shortClassName = shortClassName;
        this.fullClassName = fullClassName;

        this.setId(this.fullClassName);

        this.currentlySelected = new IndexRange(0,0); // Empty range
    }

    private String generateCodeArea(CompilationUnit unit)
    {
        assert unit != null;
        this.codeArea = new CodeArea();
        this.codeArea.setEditable(false);

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .subscribe(change -> {
                    codeArea.setStyleSpans(0, CodeHighlighter.computeHighlighting(codeArea.getText()));
                });


        AstNodeCollection<TypeDeclaration> types = unit.getTypes();
        assert types.size() == 1;
        TypeDeclaration typeDeclaration = types.firstOrNullObject();

        /*
        for (AstNode i : typeDeclaration.getChildren()) {

            if(i.getRole() == Roles.TYPE_MEMBER)
                continue;

            System.out.println("JUAN " + i + "\t\t" + i.getRole() + "\t\t" + i.getText());
        }
        */

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
                this.methodParagraphs.put(entity.getName(), new IndexRange(startParagraph, endParagraph));
            }
        }

        codeArea.setMaxHeight(Double.MAX_VALUE);

        scrollPane = new VirtualizedScrollPane(codeArea);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        this.stackPane = new StackPane(scrollPane);

        this.stackPane.setMaxWidth(Double.MAX_VALUE);
        this.stackPane.setMaxHeight(Double.MAX_VALUE);

        return classDeclaration.toString();
    }

    public void highlightMethod(String methodName) {

        for(int i = currentlySelected.getStart(); i < currentlySelected.getEnd(); ++i)
        {
            codeArea.setParagraphStyle(i, Collections.EMPTY_LIST);
        }
        this.currentlySelected = new IndexRange(0,0);

        IndexRange range = methodParagraphs.get(methodName);
        if (range == null) {
            System.out.println("Didn't find method " + methodName);
            return;
        }

        codeArea.showParagraphAtTop(range.getStart() - 1);

        for (int i = range.getStart(); i < range.getEnd(); ++i) {
            codeArea.setParagraphStyle(i, Collections.singleton("is-selected"));
        }

        this.currentlySelected = range;
    }
}
