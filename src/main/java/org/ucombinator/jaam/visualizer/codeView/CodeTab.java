package org.ucombinator.jaam.visualizer.codeView;

import com.strobel.decompiler.languages.EntityType;
import com.strobel.decompiler.languages.java.ast.*;
import javafx.scene.Node;
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
    private CodeArea codeArea;
    public final String shortClassName;
    public final String fullClassName;
    private HashMap<String, IndexRange> methodParagraphs;
    private IndexRange currentlySelected;


    public CodeTab(CompilationUnit unit, String shortClassName, String fullClassName) {
        super(shortClassName);
        this.unit = unit;
        this.methodParagraphs = new HashMap<>();

        this.setContent(generateCodeArea(unit));

        this.shortClassName = shortClassName;
        this.fullClassName = fullClassName;

        this.setId(this.fullClassName);

        this.currentlySelected = new IndexRange(0,0); // Empty range
    }

    private Node generateCodeArea(CompilationUnit unit)
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

        for(AstNode i: typeDeclaration.getChildrenByRole(Roles.TYPE_MEMBER))
        {
            EntityDeclaration entity = (EntityDeclaration)i;
            //System.out.println(i.getRole() + " " + i.getClass() + " " + entity.getName() + " " + entity.getEntityType());
            //System.out.println("\t" + entity.getText());

            int startParagraph = codeArea.getParagraphs().size()-1;
            codeArea.appendText(entity.getText() + "\n");
            int endParagraph = codeArea.getParagraphs().size()-1;

            if(entity.getEntityType() == EntityType.METHOD || entity.getEntityType() == EntityType.CONSTRUCTOR)
            {
                this.methodParagraphs.put(entity.getName(), new IndexRange(startParagraph, endParagraph));
            }
        }

        codeArea.setMaxHeight(Double.MAX_VALUE);

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        StackPane stackPane = new StackPane(scrollPane);
        stackPane.setMaxWidth(Double.MAX_VALUE);
        stackPane.setMaxHeight(Double.MAX_VALUE);

        return stackPane;
    }

    public void highlightMethod(String methodName) {

        for (int i = currentlySelected.getStart(); i < currentlySelected.getEnd(); ++i)
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
