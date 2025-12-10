package extension.ast.node.statement;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangPassStatement extends LangStatement {

    public LangPassStatement() {
        super(NodeTypeEnum.PASS_STATEMENT);
    }

    public LangPassStatement(PositionInfo positionInfo){
        super(NodeTypeEnum.PASS_STATEMENT, positionInfo);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangPassStatement";
    }
}
