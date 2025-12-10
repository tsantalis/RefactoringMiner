package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangIndexAccess extends LangExpression {

    private LangASTNode target;
    private LangASTNode index;

    public LangIndexAccess(LangASTNode target, LangASTNode index) {
        super(NodeTypeEnum.INDEX_ACCESS);
        this.target = target;
        this.index = index;
    }

    public LangIndexAccess(PositionInfo positionInfo, LangASTNode target, LangASTNode index) {
        super(NodeTypeEnum.INDEX_ACCESS, positionInfo);
        this.target = target;
        this.index = index;
    }

    public LangIndexAccess(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn, LangASTNode target, LangASTNode index) {
        super(NodeTypeEnum.INDEX_ACCESS, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.target = target;
        this.index = index;
    }

    public LangASTNode getTarget() {
        return target;
    }

    public void setTarget(LangASTNode target) {
        this.target = target;
    }

    public LangASTNode getIndex() {
        return index;
    }

    public void setIndex(LangASTNode index) {
        this.index = index;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangIndexAccess{" +
                "target=" + target +
                ", index=" + index +
                '}';
    }
}
