package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;

import java.util.List;

/**
 * Represents a switch (match) statement in the AST, similar to JDT's SwitchStatement.
 * For Python, this corresponds to the 'match' statement.
 */
public class LangSwitchStatement extends LangStatement {
    /**
     * The expression being matched (e.g., the value after 'match' in Python).
     */
    private LangASTNode expression;

    /**
     * The list of case blocks (each a LangCaseStatement).
     */
    private List<LangCaseStatement> cases;

    public LangSwitchStatement(PositionInfo positionInfo, LangASTNode expression, List<LangCaseStatement> cases) {
        super(NodeTypeEnum.SWITCH_STATEMENT, positionInfo);
        this.expression = expression;
        this.cases = cases;
    }

    public LangASTNode getExpression() {
        return expression;
    }

    public List<LangCaseStatement> getCases() {
        return cases;
    }

    @Override
    public void accept(extension.ast.visitor.LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public void setExpression(LangASTNode expression) {
        this.expression = expression;
    }

    public void setCases(List<LangCaseStatement> cases) {
        this.cases = cases;
    }

    @Override
    public String toString() {
        return "LangSwitchStatement{" +
                "expression=" + expression +
                ", cases=" + cases +
                '}';
    }
}