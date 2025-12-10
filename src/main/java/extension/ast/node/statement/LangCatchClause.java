package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a catch clause in a try statement.
 */
public class LangCatchClause extends LangStatement {
    private List<LangASTNode> exceptionTypes = new ArrayList<>();
    private LangSimpleName exceptionVariable;
    private LangASTNode body;

    public LangCatchClause() {
        super(NodeTypeEnum.CATCH_CLAUSE);
    }

    public LangCatchClause(PositionInfo positionInfo) {
        super(NodeTypeEnum.CATCH_CLAUSE, positionInfo);
    }

    public LangCatchClause(int startLine, int startChar, int endLine, int endChar,
                           int startColumn, int endColumn) {
        super(NodeTypeEnum.CATCH_CLAUSE, startLine, startChar, endLine, endChar,
                startColumn, endColumn);
    }

    public void addExceptionType(LangASTNode exceptionType) {
        this.exceptionTypes.add(exceptionType);
        addChild(exceptionType);
    }

    public void setExceptionVariable(LangSimpleName exceptionVariable) {
        this.exceptionVariable = exceptionVariable;
        addChild(exceptionVariable);
    }

    public void setBody(LangASTNode body) {
        this.body = body;
        addChild(body);
    }

    public List<LangASTNode> getExceptionTypes() {
        return new ArrayList<>(exceptionTypes);
    }

    public LangSimpleName getExceptionVariable() {
        return exceptionVariable;
    }

    public LangASTNode getBody() {
        return body;
    }

    public void setExceptionTypes(List<LangASTNode> exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangCatchClause{" +
                "exceptionTypes=" + exceptionTypes +
                ", exceptionVariable=" + exceptionVariable +
                ", body=" + body +
                '}';
    }
}