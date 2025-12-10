package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangSliceExpression extends LangExpression {

    private LangASTNode lower;
    private LangASTNode upper;
    private LangASTNode step;

    public LangSliceExpression() {
        super(NodeTypeEnum.SLICE_EXPRESSION);
    }

    public LangSliceExpression(PositionInfo positionInfo) {
        super(NodeTypeEnum.SLICE_EXPRESSION, positionInfo);
    }

    public LangSliceExpression(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.SLICE_EXPRESSION, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public LangSliceExpression(PositionInfo positionInfo, LangASTNode lower, LangASTNode upper, LangASTNode step) {
        super(NodeTypeEnum.SLICE_EXPRESSION, positionInfo);
        this.lower = lower;
        this.upper = upper;
        this.step = step;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public LangASTNode getLower() {
        return lower;
    }

    public void setLower(LangASTNode lower) {
        this.lower = lower;
    }

    public LangASTNode getUpper() {
        return upper;
    }

    public void setUpper(LangASTNode upper) {
        this.upper = upper;
    }

    public LangASTNode getStep() {
        return step;
    }

    public void setStep(LangASTNode step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "LangSliceExpression{" +
                "lower=" + lower +
                ", upper=" + upper +
                ", step=" + step +
                '}';
    }
}
