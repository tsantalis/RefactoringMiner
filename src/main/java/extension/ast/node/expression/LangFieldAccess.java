package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangFieldAccess extends LangExpression {
    private LangASTNode expression;
    private LangSimpleName name;

    public LangFieldAccess() {
        super(NodeTypeEnum.FIELD_ACCESS);
    }

    public LangFieldAccess(PositionInfo positionInfo) {
        super(NodeTypeEnum.FIELD_ACCESS, positionInfo);
    }

    public LangFieldAccess(LangASTNode expression, LangSimpleName name, PositionInfo positionInfo) {
        super(NodeTypeEnum.FIELD_ACCESS, positionInfo);
        this.expression = expression;
        this.name = name;

        if (expression != null) {
            addChild(expression);
        }
        if (name != null) {
            addChild(name);
        }
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public LangASTNode getExpression() {
        return expression;
    }

    public void setExpression(LangASTNode expression) {
        this.expression = expression;
        if (expression != null) {
            addChild(expression);
        }
    }

    public LangSimpleName getName() {
        return name;
    }

    public void setName(LangSimpleName name) {
        this.name = name;
        if (name != null) {
            addChild(name);
        }
    }


    @Override
    public String toString() {
        return "LangFieldAccess{" +
                "expression=" + expression +
                ", name=" + name +
                '}';
    }
}