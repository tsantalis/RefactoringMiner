package extension.ast.node.statement;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LangGlobalStatement extends LangStatement {

    List<LangSimpleName> variableNames = new ArrayList<>();

    public LangGlobalStatement() {
        super(NodeTypeEnum.GLOBAL_STATEMENT);
    }

    public LangGlobalStatement(PositionInfo positionInfo){
        super(NodeTypeEnum.GLOBAL_STATEMENT, positionInfo);
    }

    public LangGlobalStatement(PositionInfo positionInfo, List<LangSimpleName> variableNames){
        super(NodeTypeEnum.GLOBAL_STATEMENT, positionInfo);
        this.variableNames = variableNames;
    }

    public void addGlobalName(LangSimpleName name) {
        variableNames.add(name);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public List<LangSimpleName> getVariableNames() {
        return variableNames;
    }

    public void setVariableNames(List<LangSimpleName> variableNames) {
        this.variableNames = variableNames;
    }

    @Override
    public String toString() {
        return "LangGlobalStatement{" +
                "variableNames=" + variableNames +
                '}';
    }
}
