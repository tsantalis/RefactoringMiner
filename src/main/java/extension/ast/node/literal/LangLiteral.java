package extension.ast.node.literal;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;

public abstract class LangLiteral extends LangASTNode {
    public LangLiteral(NodeTypeEnum nodeType) {
        super(nodeType);
    }

    public LangLiteral(NodeTypeEnum nodeType, PositionInfo positionInfo) {
        super(nodeType, positionInfo);
    }

    public LangLiteral(NodeTypeEnum nodeType, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(nodeType, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }
}
