package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LangBlock extends LangStatement {
    private List<LangASTNode> statements = new ArrayList<>();

    public LangBlock() {super(NodeTypeEnum.BLOCK);}

    public LangBlock(PositionInfo positionInfo) {
        super(NodeTypeEnum.BLOCK, positionInfo);
    }

    public LangBlock(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.BLOCK, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public void addStatement(LangASTNode statement) {
        statements.add(statement);
        addChild(statement);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public List<LangASTNode> getStatements() {
        return statements;
    }

    public void setStatements(List<LangASTNode> statements) {
        this.statements = statements;
    }

    public String toString() {
        return "LangBlock{" +
                "statements=" + statements +
                '}';
    }
}
