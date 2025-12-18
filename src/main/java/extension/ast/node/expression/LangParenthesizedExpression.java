package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangParenthesizedExpression extends LangExpression {

    private LangASTNode parenthesizedExpression;

    public LangParenthesizedExpression(LangASTNode parenthesizedExpression) {
        super(NodeTypeEnum.PARENTHESIZED_EXPRESSION);
        this.parenthesizedExpression = parenthesizedExpression;
    }

    public LangParenthesizedExpression(PositionInfo positionInfo, LangASTNode parenthesizedExpression) {
        super(NodeTypeEnum.PARENTHESIZED_EXPRESSION, positionInfo);
        this.parenthesizedExpression = parenthesizedExpression;
    }

    public LangParenthesizedExpression(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn, LangASTNode parenthesizedExpression) {
        super(NodeTypeEnum.PARENTHESIZED_EXPRESSION, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.parenthesizedExpression = parenthesizedExpression;
    }

    public LangASTNode getParenthesizedExpression() {
        return parenthesizedExpression;
    }

    public void setParenthesizedExpression(LangASTNode parenthesizedExpression) {
        this.parenthesizedExpression = parenthesizedExpression;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangParenthesizedExpression{" +
                "parenthesizedExpression=" + (parenthesizedExpression != null ? parenthesizedExpression : "null") +
                '}';
    }
}
