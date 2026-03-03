package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;

/**
 * Represents a case within a switch/match statement in the AST.
 * For Python, this corresponds to a 'case' block or the 'case _' (default).
 */
public class LangCaseStatement extends LangStatement {
    /**
     * The pattern for this case (can be null or a special value for 'default'/underscore).
     */
    private LangASTNode pattern;
    private LangASTNode guard;
    private LangBlock body;

    public LangCaseStatement(PositionInfo positionInfo, LangASTNode pattern, LangBlock body) {
        super(NodeTypeEnum.CASE_STATEMENT, positionInfo);
        this.pattern = pattern;
        this.body = body;
    }

    public LangCaseStatement(PositionInfo positionInfo, LangASTNode pattern, LangASTNode guard, LangBlock body) {
        super(NodeTypeEnum.CASE_STATEMENT, positionInfo);
        this.pattern = pattern;
        this.guard = guard;
        this.body = body;
    }

    public LangASTNode getPattern() {
        return pattern;
    }

    public LangASTNode getGuard() {
        return guard;
    }

    public LangBlock getBody() {
        return body;
    }

    public void setPattern(LangASTNode pattern) {
        this.pattern = pattern;
    }

    public void setGuard(LangASTNode guard) {
        this.guard = guard;
    }

    public void setBody(LangBlock body) {
        this.body = body;
    }

    @Override
    public void accept(extension.ast.visitor.LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangCaseStatement{" +
                "pattern=" + pattern +
                ", body=" + body +
                '}';
    }
}