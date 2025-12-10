package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangReturnStatement extends LangStatement {
    private LangASTNode expression;

    public LangReturnStatement() {super(NodeTypeEnum.RETURN_STATEMENT);}

    public LangReturnStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.RETURN_STATEMENT, positionInfo);
    }

    public LangReturnStatement(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.RETURN_STATEMENT, startLine, startChar, endLine, endChar,  startColumn, endColumn);
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

    public String toString() {
        return "LangReturnStatement{" +
                "expression=" + expression +
                '}';
    }
}
