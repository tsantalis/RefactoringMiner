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

    /**
     * The statements to execute if the pattern matches.
     */
    private LangBlock body;

    public LangCaseStatement(PositionInfo positionInfo, LangASTNode pattern, LangBlock body) {
        super(NodeTypeEnum.CASE_STATEMENT, positionInfo);
        this.pattern = pattern;
        this.body = body;
    }

    public LangASTNode getPattern() {
        return pattern;
    }

    public LangBlock getBody() {
        return body;
    }

    public void setPattern(LangASTNode pattern) {
        this.pattern = pattern;
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