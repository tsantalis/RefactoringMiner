package extension.ast.node.literal;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LangListLiteral extends LangLiteral {
    private List<LangASTNode> elements;

    public LangListLiteral() {super(NodeTypeEnum.LIST_LITERAL);}

    public LangListLiteral(PositionInfo positionInfo, List<LangASTNode> elements) {
        super(NodeTypeEnum.LIST_LITERAL, positionInfo);
        this.elements = elements;
    }

    public LangListLiteral(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.LIST_LITERAL, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.elements = new ArrayList<>();
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public void addElement(LangASTNode element) {
        if (this.elements == null) {
            this.elements = new ArrayList<>();
        }
        this.elements.add(element);
        addChild(element);
    }

    public List<LangASTNode> getElements() {
        return elements;
    }

    public void setElements(List<LangASTNode> elements) {
        this.elements = elements;

        // Set parent-child relationships for each element
        for (LangASTNode element : elements) {
            addChild(element);
        }
    }

    public String toString() {
        return "LangListLiteral{" +
                "elements=" + elements +
                '}';
    }

}