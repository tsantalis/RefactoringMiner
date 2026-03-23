package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

public class LangTemplateExpressionPart extends LangExpression {

    private LangASTNode expression;
    private LangASTNode formatExpression;
    private boolean isDebug;
    private String conversion;

    public LangTemplateExpressionPart() {
        super(NodeTypeEnum.TEMPLATE_EXPRESSION_PART);
    }

    public LangTemplateExpressionPart(PositionInfo positionInfo) {
        super(NodeTypeEnum.TEMPLATE_EXPRESSION_PART, positionInfo);
    }

    public LangTemplateExpressionPart(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.TEMPLATE_EXPRESSION_PART, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public LangTemplateExpressionPart(PositionInfo positionInfo, LangASTNode expression, LangASTNode formatExpression) {
        super(NodeTypeEnum.TEMPLATE_EXPRESSION_PART, positionInfo);
        this.expression = expression;
        this.formatExpression = formatExpression;
    }


    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangTemplateExpressionPart{" +
                "expression=" + expression +
                ", format=" + formatExpression +
                ", isDebug=" + isDebug +
                ", conversion='" + conversion + '\'' +
                '}';
    }

    public LangASTNode getExpression() {
        return expression;
    }

    public void setExpression(LangASTNode expression) {
        this.expression = expression;
    }

    public LangASTNode getFormatExpression() {
        return formatExpression;
    }

    public void setFormatExpression(LangASTNode format) {
        this.formatExpression = format;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public String getConversion() {
        return conversion;
    }

    public void setConversion(String conversion) {
        this.conversion = conversion;
    }
}
