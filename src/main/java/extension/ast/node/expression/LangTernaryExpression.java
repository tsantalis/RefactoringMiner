package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangTernaryExpression extends LangExpression {

    // LangASTNode for ternary expression
    // Example: x if x > 0 else -x
    // Condition: x > 0
    // Then: x
    // Else: -x

    private LangASTNode thenExpression;
    private LangASTNode condition;
    private LangASTNode elseExpression;

    public LangTernaryExpression() {
        super(NodeTypeEnum.TERNARY_EXPRESSION);
    }

    public LangTernaryExpression(PositionInfo positionInfo) {
        super(NodeTypeEnum.TERNARY_EXPRESSION, positionInfo);
    }

    public LangTernaryExpression(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.TERNARY_EXPRESSION, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public LangTernaryExpression(PositionInfo positionInfo, LangASTNode condition, LangASTNode thenExpression, LangASTNode elseExpression) {
        super(NodeTypeEnum.TERNARY_EXPRESSION, positionInfo);
        this.condition = condition;
        this.thenExpression = thenExpression;
        this.elseExpression = elseExpression;
    }

    public LangASTNode getCondition() {
        return condition;
    }

    public void setCondition(LangASTNode condition) {
        this.condition = condition;
    }

    public LangASTNode getThenExpression() {
        return thenExpression;
    }

    public void setThenExpression(LangASTNode thenExpression) {
        this.thenExpression = thenExpression;
    }

    public LangASTNode getElseExpression() {
        return elseExpression;
    }

    public void setElseExpression(LangASTNode elseExpression) {
        this.elseExpression = elseExpression;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangTernaryExpression{" +
                "thenExpression=" + thenExpression +
                ", condition=" + condition +
                ", elseExpression=" + elseExpression +
                '}';
    }
}