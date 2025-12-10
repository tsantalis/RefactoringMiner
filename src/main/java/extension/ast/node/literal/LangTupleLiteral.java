package extension.ast.node.literal;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LangTupleLiteral extends LangLiteral {
    private final List<LangASTNode> elements = new ArrayList<>();

    public LangTupleLiteral(PositionInfo positionInfo) {
        super(NodeTypeEnum.TUPLE_LITERAL, positionInfo);
    }

    public LangTupleLiteral(PositionInfo positionInfo, List<LangASTNode> elements) {
        super(NodeTypeEnum.TUPLE_LITERAL, positionInfo);
        if (elements != null) {
            for (LangASTNode element : elements) {
                addElement(element);
            }
        }
    }

    public void addElement(LangASTNode element) {
        if (element != null) {
            elements.add(element);
            addChild(element);
        }
    }

    public List<LangASTNode> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangTupleLiteral{" +
                "elements=" + elements +
                '}';
    }
}