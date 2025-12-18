package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.OperatorEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangInfixExpression extends LangExpression {
    private LangASTNode left;
    private OperatorEnum operator;
    private LangASTNode right;

    public LangInfixExpression() {super(NodeTypeEnum.INFIX_EXPRESSION);}

    public LangInfixExpression(LangASTNode left, OperatorEnum operator, LangASTNode right, PositionInfo positionInfo) {
        super(NodeTypeEnum.INFIX_EXPRESSION, positionInfo);
        this.left = left;
        this.operator = operator;
        this.right = right;
        if (left != null) addChild(left);
        if (right != null) addChild(right);
    }

    public LangInfixExpression(LangASTNode left, OperatorEnum operator, LangASTNode right, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.INFIX_EXPRESSION, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.left = left;
        this.operator = operator;
        this.right = right;
        if (left != null) addChild(left);
        if (right != null) addChild(right);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public LangASTNode getLeft() {
        return left;
    }

    public void setLeft(LangASTNode left) {
        this.left = left;
    }

    public OperatorEnum getOperator() {
        return operator;
    }

    public void setOperator(OperatorEnum operator) {
        this.operator = operator;
    }

    public LangASTNode getRight() {
        return right;
    }

    public void setRight(LangASTNode right) {
        this.right = right;
    }

    public String toString() {
        return "LangInfixExpression{" +
                "left=" + left +
                ", operator='" + operator + '\'' +
                ", right=" + right +
                '}';
    }
}