package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LangMethodInvocation extends LangExpression {
    private String name;
    private LangASTNode expression;
    private List<LangASTNode> arguments;

    public LangMethodInvocation() {
        super(NodeTypeEnum.METHOD_INVOCATION);
        this.arguments = new ArrayList<>();
    }

    public LangMethodInvocation(PositionInfo positionInfo) {
        super(NodeTypeEnum.METHOD_INVOCATION, positionInfo);
        this.arguments = new ArrayList<>();
    }

    public LangMethodInvocation(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.METHOD_INVOCATION, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.arguments = new ArrayList<>();
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public void addArgument(LangASTNode argument) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>();
        }
        this.arguments.add(argument);
        addChild(argument);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LangASTNode getExpression() {
        return expression;
    }

    public void setExpression(LangASTNode expression) {
        this.expression = expression;
        if (expression != null) {
            addChild(expression);
        }
    }

    public List<LangASTNode> getArguments() {
        return arguments;
    }

    public void setArguments(List<LangASTNode> arguments) {
        this.arguments = arguments;

        for (LangASTNode arg : arguments) {
            addChild(arg);
        }
    }

    @Override
    public String toString() {
        return "LangMethodInvocation{" +
                "name='" + name + '\'' +
                ", expression=" + expression +
                ", arguments=" + arguments +
                '}';
    }
}