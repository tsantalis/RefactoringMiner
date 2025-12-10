package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LangThrowStatement extends LangStatement {

    private List<LangASTNode> expressions = new ArrayList<>();
    private LangASTNode fromExpression; // For Python's "raise ... from ..." syntax

    public LangThrowStatement() {
        super(NodeTypeEnum.THROW_STATEMENT);
    }

    public LangThrowStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.THROW_STATEMENT, positionInfo);
    }

    public LangThrowStatement(PositionInfo positionInfo, List<LangASTNode> expressions) {
        super(NodeTypeEnum.THROW_STATEMENT, positionInfo);
        if (expressions != null) {
            this.expressions = new ArrayList<>(expressions);
            for (LangASTNode expr : expressions) {
                addChild(expr);
            }
        }
    }

    /**
     * Constructor for Python-style 'raise exception from cause' syntax
     */
    public LangThrowStatement(PositionInfo positionInfo, LangASTNode exception, LangASTNode fromExpr) {
        super(NodeTypeEnum.THROW_STATEMENT, positionInfo);
        if (exception != null) {
            this.expressions.add(exception);
            addChild(exception);
        }
        this.fromExpression = fromExpr;
        if (fromExpr != null) {
            addChild(fromExpr);
        }
    }

    /**
     * Add an expression to the throw statement
     */
    public void addExpression(LangASTNode expression) {
        if (expression != null) {
            this.expressions.add(expression);
            addChild(expression);
        }
    }

    /**
     * Set the "from" expression for Python's raise syntax
     */
    public void setFromExpression(LangASTNode fromExpression) {
        this.fromExpression = fromExpression;
        if (fromExpression != null) {
            addChild(fromExpression);
        }
    }

    public List<LangASTNode> getExpressions() {
        return new ArrayList<>(expressions);
    }

    public LangASTNode getFromExpression() {
        return fromExpression;
    }

    public boolean hasFromExpression() {
        return fromExpression != null;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangThrowStatement{" +
                "expressions=" + expressions +
                ", fromExpression=" + fromExpression +
                '}';
    }
}
