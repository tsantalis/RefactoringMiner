package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Python lambda expression: lambda parameters: expression
 */
public class LangLambdaExpression extends LangExpression {
    private final List<LangASTNode> parameters;
    private LangASTNode body; // The expression after the ':'

    public LangLambdaExpression(PositionInfo positionInfo, List<LangASTNode> parameters, LangASTNode body) {
        super(NodeTypeEnum.LAMBDA_EXPRESSION, positionInfo);
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.body = body;
        // Optionally add children for easy traversal
        if (this.body != null) addChild(this.body);
        for (LangASTNode param : this.parameters) {
            if (param != null) addChild(param);
        }
    }

    public List<LangASTNode> getParameters() {
        return parameters;
    }

    public LangASTNode getBody() {
        return body;
    }

    public void setBody(LangASTNode body) {
        this.body = body;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangLambdaExpression{" +
                "parameters=" + parameters +
                ", body=" + body +
                '}';
    }
}