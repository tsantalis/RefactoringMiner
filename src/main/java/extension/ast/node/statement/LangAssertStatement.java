package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangAssertStatement extends LangStatement {

    private LangASTNode expression;
    private LangASTNode message; // nullable

    public LangAssertStatement() {
        super(NodeTypeEnum.ASSERT_STATEMENT);
    }

    public LangAssertStatement(PositionInfo pos) {
        super(NodeTypeEnum.ASSERT_STATEMENT, pos);
    }

    public LangAssertStatement(PositionInfo positionInfo, LangASTNode expression, LangASTNode message) {
        super(NodeTypeEnum.ASSERT_STATEMENT, positionInfo);
        this.expression = expression;
        this.message = message;
    }

    public LangAssertStatement(NodeTypeEnum nodeType, PositionInfo positionInfo, LangASTNode expression, LangASTNode message) {
        super(nodeType, positionInfo);
        this.expression = expression;
        this.message = message;
    }

    public LangAssertStatement(NodeTypeEnum nodeType, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn, LangASTNode expression, LangASTNode message) {
        super(nodeType, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.expression = expression;
        this.message = message;
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
    }

    public LangASTNode getMessage() {
        return message;
    }

    public void setMessage(LangASTNode message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "LangAssertStatement{" +
                "expression=" + expression +
                ", message=" + message +
                '}';
    }
}
