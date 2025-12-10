package extension.ast.node.statement;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;


public class LangNonLocalStatement extends LangStatement {

    private List<LangSimpleName> names = new ArrayList<>();

    public LangNonLocalStatement() {
        super(NodeTypeEnum.NON_LOCAL_STATEMENT);
    }

    public LangNonLocalStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.NON_LOCAL_STATEMENT, positionInfo);
    }

    public LangNonLocalStatement(PositionInfo positionInfo, List<LangSimpleName> nonlocalNames) {
        super(NodeTypeEnum.NON_LOCAL_STATEMENT, positionInfo);
        this.names = nonlocalNames;
        for (LangSimpleName name : nonlocalNames) {
            addChild(name);
        }
    }

    public void addName(LangSimpleName name) {
        this.names.add(name);
        addChild(name);
    }

    public List<LangSimpleName> getNames() {
        return new ArrayList<>(names);
    }

    public void setNames(List<LangSimpleName> names) {
        this.names = names;
        for (LangSimpleName name : names) {
            addChild(name);
        }
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangNonLocalStatement{" +
                "names=" + names +
                '}';
    }
}