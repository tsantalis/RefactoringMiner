package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangAssignment extends LangExpression {
    private String operator;
    private LangASTNode leftSide;
    private LangASTNode rightSide;

    public LangAssignment() {super(NodeTypeEnum.ASSIGNMENT);}

    public LangAssignment(String operator, LangASTNode leftSide, LangASTNode rightSide, PositionInfo positionInfo) {
        super(NodeTypeEnum.ASSIGNMENT, positionInfo);
        this.operator = operator;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        addChild(leftSide);
        addChild(rightSide);
    }

    public LangAssignment(String operator, LangASTNode leftSide, LangASTNode rightSide, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.ASSIGNMENT, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.operator = operator;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        addChild(leftSide);
        addChild(rightSide);
    }


    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LangASTNode getLeftSide() {
        return leftSide;
    }

    public void setLeftSide(LangASTNode leftSide) {
        this.leftSide = leftSide;
    }

    public LangASTNode getRightSide() {
        return rightSide;
    }

    public void setRightSide(LangASTNode rightSide) {
        this.rightSide = rightSide;
    }

    public String toString() {
        return "LangAssignment{" +
                "leftSide=" + leftSide +
                ", operator=" + "'" + operator + "'"+
                ", rightSide=" + rightSide +
                '}';
    }
}
