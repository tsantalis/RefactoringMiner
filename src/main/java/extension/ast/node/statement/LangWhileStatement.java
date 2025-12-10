package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangWhileStatement extends LangStatement {
    private LangASTNode condition;
    private LangASTNode body;
    private LangASTNode elseBody;

    public LangWhileStatement(){super(NodeTypeEnum.WHILE_STATEMENT);}

    public LangWhileStatement(LangASTNode condition, LangASTNode body, LangASTNode elseBody, PositionInfo positionInfo) {
        super(NodeTypeEnum.WHILE_STATEMENT, positionInfo);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;

        if (condition != null) addChild(condition);
        if (body != null) addChild(body);
        if (elseBody != null) addChild(elseBody);
    }

    public LangWhileStatement(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn, LangASTNode condition, LangASTNode body, LangASTNode elseBody) {
        super(NodeTypeEnum.WHILE_STATEMENT, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;

        if (condition != null) addChild(condition);
        if (body != null) addChild(body);
        if (elseBody != null) addChild(elseBody);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public LangASTNode getCondition() {
        return condition;
    }

    public void setCondition(LangASTNode condition) {
        this.condition = condition;
    }

    public LangASTNode getBody() {
        return body;
    }

    public void setBody(LangASTNode body) {
        this.body = body;
    }

    public LangASTNode getElseBody() {
        return elseBody;
    }

    public void setElseBody(LangASTNode elseBody) {
        this.elseBody = elseBody;
    }

    public String toString() {
        return "LangWhileStatement{" +
                "condition=" + condition +
                ", body=" + body +
                ", elseBody=" + elseBody +
                '}';
    }

}