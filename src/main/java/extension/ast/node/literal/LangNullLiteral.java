package extension.ast.node.literal;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;


public class LangNullLiteral extends LangLiteral {

    public LangNullLiteral() {
        super(NodeTypeEnum.NULL_LITERAL);
    }

    public LangNullLiteral(PositionInfo positionInfo) {
        super(NodeTypeEnum.NULL_LITERAL, positionInfo);
    }

    public LangNullLiteral(int startLine, int startChar, int endLine, int endChar,
                           int startColumn, int endColumn) {
        super(NodeTypeEnum.NULL_LITERAL, startLine, startChar, endLine, endChar,
                startColumn, endColumn);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Returns true since this is a null literal.
     */
    public boolean isNull() {
        return true;
    }

    /**
     * Returns the null value.
     */
    public Object getValue() {
        return null;
    }

    @Override
    public String toString() {
        return "LangNullLiteral{}";
    }
}