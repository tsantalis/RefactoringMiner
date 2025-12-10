package extension.ast.node.pattern;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

/**
 * Pattern representing a variable binding (e.g., case x:)
 */
public class LangVariablePattern extends LangPattern {
    private final String variableName;

    public LangVariablePattern(PositionInfo positionInfo, String variableName) {
        super(NodeTypeEnum.VARIABLE_PATTERN, positionInfo);
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangVariablePattern{variableName='" + variableName + "'}";
    }
}