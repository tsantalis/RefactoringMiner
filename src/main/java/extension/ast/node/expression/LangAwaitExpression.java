package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangAwaitExpression extends LangExpression {
    private LangASTNode expression;

    public LangAwaitExpression() {
        super(NodeTypeEnum.AWAIT_EXPRESSION);
    }

    public LangAwaitExpression(PositionInfo positionInfo, LangASTNode expression) {
        super(NodeTypeEnum.AWAIT_EXPRESSION, positionInfo);
        this.expression = expression;
    }

    public LangAwaitExpression(PositionInfo positionInfo) {
        super(NodeTypeEnum.AWAIT_EXPRESSION, positionInfo);
    }

    public LangASTNode getExpression() {
        return expression;
    }

    public void setExpression(LangASTNode expression) {
        this.expression = expression;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangAwaitExpression{" +
                "expression=" + expression +
                '}';
    }
}