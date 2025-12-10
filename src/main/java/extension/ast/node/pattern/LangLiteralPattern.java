package extension.ast.node.pattern;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

/**
 * Pattern representing a literal value (e.g., case 3:)
 */
public class LangLiteralPattern extends LangPattern {
    private final Object value;

    public LangLiteralPattern(PositionInfo positionInfo, Object value) {
        super(NodeTypeEnum.LITERAL_PATTERN, positionInfo);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangLiteralPattern{value=" + value + "}";
    }
}