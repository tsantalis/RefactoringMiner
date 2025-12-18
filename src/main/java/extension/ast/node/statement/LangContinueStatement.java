package extension.ast.node.statement;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangContinueStatement extends LangStatement {

    public LangContinueStatement() {
        super(NodeTypeEnum.CONTINUE_STATEMENT);
    }

    public LangContinueStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.CONTINUE_STATEMENT, positionInfo);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangContinueStatement";
    }
}
