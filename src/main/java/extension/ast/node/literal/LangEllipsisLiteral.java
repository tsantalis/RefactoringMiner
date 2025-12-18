package extension.ast.node.literal;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangEllipsisLiteral extends LangLiteral {

    private String ellipsis = "...";

    public LangEllipsisLiteral() {
        super(NodeTypeEnum.ELLIPSIS_LITERAL);
    }

    public LangEllipsisLiteral(PositionInfo positionInfo) {
        super(NodeTypeEnum.ELLIPSIS_LITERAL, positionInfo);
    }

    public LangEllipsisLiteral(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.ELLIPSIS_LITERAL, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public String getEllipsis() {
        return ellipsis;
    }

    public void setEllipsis(String ellipsis) {
        this.ellipsis = ellipsis;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangEllipsisLiteral{" +
                "ellipsis='" + ellipsis + '\'' +
                '}';
    }
}
