package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;


public class LangTryStatement extends LangStatement {
    private LangASTNode body;
    private List<LangCatchClause> catchClauses = new ArrayList<>();
    private LangASTNode finallyBlock;

    public LangTryStatement() {
        super(NodeTypeEnum.TRY_STATEMENT);
    }

    public LangTryStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.TRY_STATEMENT, positionInfo);
    }

    public LangTryStatement(int startLine, int startChar, int endLine, int endChar,
                            int startColumn, int endColumn) {
        super(NodeTypeEnum.TRY_STATEMENT, startLine, startChar, endLine, endChar,
                startColumn, endColumn);
    }

    public LangTryStatement(PositionInfo positionInfo, LangBlock tryBlock, List<LangCatchClause> catchClauses, LangBlock elseBlock, LangBlock finallyBlock) {
        super(NodeTypeEnum.TRY_STATEMENT, positionInfo);
        this.body = tryBlock;
        this.catchClauses = catchClauses;
        this.finallyBlock = finallyBlock;

        if (tryBlock != null) addChild(tryBlock);
        if (catchClauses != null) {
            for (LangCatchClause catchClause : catchClauses) {
                if (catchClause != null) addChild(catchClause);
            }
        }
        if (elseBlock != null) addChild(elseBlock);
        if (finallyBlock != null) addChild(finallyBlock);
    }

    public void setBody(LangASTNode body) {
        this.body = body;
        addChild(body);
    }

    public void addCatchClause(LangCatchClause catchClause) {
        this.catchClauses.add(catchClause);
        addChild(catchClause);
    }

    public void setFinally(LangASTNode finallyBlock) {
        this.finallyBlock = finallyBlock;
        addChild(finallyBlock);
    }

    public LangASTNode getBody() {
        return body;
    }

    public List<LangCatchClause> getCatchClauses() {
        return new ArrayList<>(catchClauses);
    }

    public LangASTNode getFinallyBlock() {
        return finallyBlock;
    }

    public boolean hasFinallyBlock() {
        return finallyBlock != null;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangTryStatement{" +
                "body=" + body +
                ", catchClauses=" + catchClauses +
                ", finallyBlock=" + finallyBlock +
                '}';
    }
}