package extension.ast.node.pattern;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

/**
 * Pattern representing an 'as' binding (e.g., case pattern as name:)
 */
public class LangAsPattern extends LangPattern {
    private final LangASTNode pattern;
    private final LangASTNode target;

    public LangAsPattern(PositionInfo positionInfo, LangASTNode pattern, LangASTNode target) {
        super(NodeTypeEnum.AS_PATTERN, positionInfo);
        this.target = target;
        this.pattern = pattern;
        if (target != null) {
            addChild(target);
        }
        if (pattern != null) {
            addChild(pattern);
        }
    }

    public LangASTNode getPattern() {
        return pattern;
    }

    public LangASTNode getTarget() {
        return target;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangAsPattern{pattern=" + pattern + ", target=" + target + "}";
    }
}
