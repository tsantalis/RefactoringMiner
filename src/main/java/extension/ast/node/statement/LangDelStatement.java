package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LangDelStatement extends LangStatement {

    List<LangASTNode> targets = new ArrayList<>();

    public LangDelStatement() {
        super(NodeTypeEnum.DEL_STATEMENT);
    }

    public LangDelStatement(PositionInfo positionInfo){
        super(NodeTypeEnum.DEL_STATEMENT, positionInfo);
    }

    public LangDelStatement(PositionInfo positionInfo, List<LangASTNode> targets){
        super(NodeTypeEnum.DEL_STATEMENT, positionInfo);
        this.targets = targets;
    }


    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public List<LangASTNode> getTargets() {
        return targets;
    }

    public void setTargets(List<LangASTNode> targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        return "LangDelStatement{" +
                "targets=" + targets +
                '}';
    }
}
