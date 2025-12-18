package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangYieldStatement extends LangStatement {

    private LangASTNode expression;

    public LangYieldStatement() {
        super(NodeTypeEnum.YIELD_STATEMENT);
    }

    public LangYieldStatement(PositionInfo positionInfo){
        super(NodeTypeEnum.YIELD_STATEMENT, positionInfo);
    }

    public LangYieldStatement(PositionInfo positionInfo, LangASTNode expression){
        super(NodeTypeEnum.YIELD_STATEMENT, positionInfo);
        this.expression = expression;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public LangASTNode getExpression() {
        return expression;
    }

    public void setExpression(LangASTNode expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "LangYieldStatement{" +
                "expression=" + expression +
                '}';
    }
}
