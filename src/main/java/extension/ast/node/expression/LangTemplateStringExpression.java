package extension.ast.node.expression;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.List;

public class LangTemplateStringExpression extends LangExpression {

    private TemplateStringKind kind;
    private List<LangTemplateExpressionPart> parts;

    public LangTemplateStringExpression() {
        super(NodeTypeEnum.TEMPLATE_STRING_EXPRESSION);
    }

    public LangTemplateStringExpression(PositionInfo positionInfo) {
        super(NodeTypeEnum.TEMPLATE_STRING_EXPRESSION, positionInfo);
    }

    public LangTemplateStringExpression(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.TEMPLATE_STRING_EXPRESSION, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public LangTemplateStringExpression(PositionInfo positionInfo, TemplateStringKind kind, List<LangTemplateExpressionPart> parts) {
        super(NodeTypeEnum.TEMPLATE_STRING_EXPRESSION, positionInfo);
        this.kind = kind;
        this.parts = parts;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangTemplateStringExpression{" +
                "kind=" + kind +
                ", parts=" + parts +
                '}';
    }

    public TemplateStringKind getKind() {
        return kind;
    }

    public void setKind(TemplateStringKind kind) {
        this.kind = kind;
    }

    public List<LangTemplateExpressionPart> getParts() {
        return parts;
    }

    public void setParts(List<LangTemplateExpressionPart> parts) {
        this.parts = parts;
    }

    public enum TemplateStringKind {
        FSTRING, TSTRING
    }
}
