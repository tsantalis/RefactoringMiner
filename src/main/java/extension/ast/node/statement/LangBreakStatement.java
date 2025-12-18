package extension.ast.node.statement;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangBreakStatement extends LangStatement {

    public LangBreakStatement() {
        super(NodeTypeEnum.BREAK_STATEMENT);
    }

    public LangBreakStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.BREAK_STATEMENT, positionInfo);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangBreakStatement";
    }

}
