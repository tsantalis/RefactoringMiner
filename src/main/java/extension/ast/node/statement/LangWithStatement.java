package extension.ast.node.statement;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class LangWithStatement extends LangStatement {

    private List<LangASTNode> contextItems = new ArrayList<>();
    private LangBlock body;

    public LangWithStatement() {
        super(NodeTypeEnum.WITH_STATEMENT);
    }

    public LangWithStatement(PositionInfo positionInfo) {
        super(NodeTypeEnum.WITH_STATEMENT, positionInfo);
    }

    public LangWithStatement(PositionInfo positionInfo, List<LangASTNode> contextItems, LangBlock body) {
        super(NodeTypeEnum.WITH_STATEMENT, positionInfo);
        this.contextItems = contextItems;
        this.body = body;

        if (contextItems != null) {
            for (LangASTNode item : contextItems) {
                if (item != null) addChild(item);
            }
        }
        if (body != null) addChild(body);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public List<LangASTNode> getContextItems() {
        return contextItems;
    }

    public void setContextItems(List<LangASTNode> contextItems) {
        this.contextItems = contextItems;
    }

    public LangBlock getBody() {
        return body;
    }

    public void setBody(LangBlock body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "LangWithStatement{" +
                "contextItems=" + contextItems +
                ", body=" + body +
                '}';
    }
}
