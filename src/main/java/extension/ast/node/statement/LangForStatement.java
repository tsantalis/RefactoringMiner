package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.visitor.LangASTVisitor;

import java.util.List;

public class LangForStatement extends LangStatement {

    // Variable declarations that initialize the for loop (e.g., "int i = 0" in C-style for loops)
    // In Python, this represents the loop variables (e.g., "i" in "for i in range(10)")
    private List<LangSingleVariableDeclaration> initializers;

    // The condition/iterable expression that controls the loop execution
    // In C-style: the boolean condition (e.g., "i < 10")
    // In Python: the iterable expression (e.g., "range(10)", "items")
    private LangASTNode condition;

    // Update expressions executed at the end of each iteration (e.g., "i++" in C-style for loops)
    // In Python for-each loops, this is typically empty since iteration is handled automatically
    private List<LangASTNode> updates;

    // The main body/block of statements executed in each loop iteration
    // Contains all the statements inside the for loop
    private LangASTNode body;

    // The optional else clause that executes when the loop completes normally (Python-specific)
    // In languages without for-else (like Java/C), this would be null
    // In Python: executes only if the loop wasn't terminated by a break statement
    private LangASTNode elseBody;

    public LangForStatement() {super(NodeTypeEnum.FOR_STATEMENT);}

    public LangForStatement(List<LangSingleVariableDeclaration> initializers, LangASTNode condition, List<LangASTNode> updates,
                            LangASTNode body, LangASTNode elseBody, PositionInfo positionInfo) {
        super(NodeTypeEnum.FOR_STATEMENT, positionInfo);
        this.initializers = initializers;
        this.condition = condition;
        this.updates = updates;
        this.body = body;
        this.elseBody = elseBody;

        if (initializers != null) initializers.forEach(this::addChild);
        if (condition != null) addChild(condition);
        if (updates != null) updates.forEach(this::addChild);
        if (body != null) addChild(body);
        if (elseBody != null) addChild(elseBody);
    }

    public LangForStatement(List<LangSingleVariableDeclaration> initializers, LangASTNode condition, List<LangASTNode> updates,
                            LangASTNode body, LangASTNode elseBody, int startLine, int startChar,
                            int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.FOR_STATEMENT, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.initializers = initializers;
        this.condition = condition;
        this.updates = updates;
        this.body = body;
        this.elseBody = elseBody;

        if (initializers != null) initializers.forEach(this::addChild);
        if (condition != null) addChild(condition);
        if (updates != null) updates.forEach(this::addChild);
        if (body != null) addChild(body);
        if (elseBody != null) addChild(elseBody);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public List<LangSingleVariableDeclaration> getInitializers() {
        return initializers;
    }

    public void setInitializers(List<LangSingleVariableDeclaration> initializers) {
        this.initializers = initializers;
    }

    public LangASTNode getCondition() {
        return condition;
    }

    public void setCondition(LangASTNode condition) {
        this.condition = condition;
    }

    public List<LangASTNode> getUpdates() {
        return updates;
    }

    public void setUpdates(List<LangASTNode> updates) {
        this.updates = updates;
    }

    public LangASTNode getBody() {
        return body;
    }

    public void setBody(LangASTNode body) {
        this.body = body;
    }

    public LangASTNode getElseBody() {
        return elseBody;
    }

    public void setElseBody(LangASTNode elseBody) {
        this.elseBody = elseBody;
    }

    @Override
    public String toString() {
        return "LangForStatement{" +
                "initializers=" + initializers +
                ", condition=" + condition +
                ", updates=" + updates +
                ", body=" + body +
                ", elseBody=" + elseBody +
                '}';
    }
}