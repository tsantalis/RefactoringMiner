package extension.ast.node.declaration;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;

public abstract class LangDeclaration extends LangASTNode {
    public LangDeclaration(NodeTypeEnum nodeType) {
        super(nodeType);
    }

    public LangDeclaration(NodeTypeEnum nodeType, PositionInfo positionInfo) {
        super(nodeType, positionInfo);
    }

    public LangDeclaration(NodeTypeEnum nodeType, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(nodeType, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }
}
