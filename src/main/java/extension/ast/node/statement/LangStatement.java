package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;

public abstract class LangStatement extends LangASTNode {

    public LangStatement(NodeTypeEnum nodeType) {
        super(nodeType);
    }

    public LangStatement(NodeTypeEnum nodeType, PositionInfo positionInfo) {
        super(nodeType, positionInfo);
    }

    public LangStatement(NodeTypeEnum nodeType, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(nodeType, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }
}
