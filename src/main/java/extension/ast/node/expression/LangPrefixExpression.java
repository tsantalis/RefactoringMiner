package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.OperatorEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangPrefixExpression extends LangExpression {
    private LangASTNode operand;
    private OperatorEnum operator;

    public LangPrefixExpression() {
        super(NodeTypeEnum.PREFIX_EXPRESSION);
    }

    public LangPrefixExpression(PositionInfo positionInfo) {
        super(NodeTypeEnum.PREFIX_EXPRESSION, positionInfo);
    }

    public LangPrefixExpression(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.PREFIX_EXPRESSION, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public void setOperand(LangASTNode operand) {
        this.operand = operand;
        addChild(operand);
    }

    public void setOperator(OperatorEnum operator) {
        this.operator = operator;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public LangASTNode getOperand() {
        return operand;
    }

    public OperatorEnum getOperator() {
        return operator;
    }

    public boolean isPlusAssign(){
        return OperatorEnum.PLUS_ASSIGN.equals(operator);
    }

    public boolean isMinusAssign(){
        return OperatorEnum.MINUS_ASSIGN.equals(operator);
    }

    public boolean isIncrement() {
        return OperatorEnum.INCREMENT.equals(operator);
    }

    public boolean isDecrement() {
        return OperatorEnum.DECREMENT.equals(operator);
    }

    @Override
    public String toString() {
        return "LangPrefixExpression{" +
                "operand=" + operand +
                ", operator=" + operator +
                '}';
    }
}