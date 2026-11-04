package extension.ast.node.expression;

import java.util.ArrayList;
import java.util.List;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangAssignment extends LangExpression {
    private String operator;
    private LangASTNode leftSide;
    private LangASTNode rightSide;
    //the annotation of an annotated assignment, i.e., Final in MAX_COMMENT_CHARS: Final = 60_000
    private String typeAnnotation;
    //the type parameters of a type alias statement, i.e., T in type Pair[T] = tuple[T, T]
    private List<LangASTNode> typeParameters = new ArrayList<>();

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

    //an augmented assignment updates the variable it assigns instead of binding it, i.e., missing += [...], unlike missing = [...], x := 1 or type X = int
    public boolean isAugmented() {
        return operator != null && operator.length() > 1 && operator.endsWith("=") && !operator.equals(":=");
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

    public String getTypeAnnotation() {
        return typeAnnotation;
    }

    public void setTypeAnnotation(String typeAnnotation) {
        this.typeAnnotation = typeAnnotation;
    }

    public List<LangASTNode> getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(List<LangASTNode> typeParameters) {
        this.typeParameters = typeParameters;
    }

    //a PEP 695 type alias statement, i.e., type X = int, built as an assignment with the type operator
    public boolean isTypeAliasStatement() {
        return "type".equals(operator);
    }

    public String toString() {
        return "LangAssignment{" +
                "leftSide=" + leftSide +
                ", operator=" + "'" + operator + "'"+
                ", rightSide=" + rightSide +
                '}';
    }
}
