package extension.ast.node.expression;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

import java.util.List;

public class LangComprehensionExpression extends LangASTNode {


    private LangComprehensionKind kind;     // The type of comprehension (LIST, SET, DICT, or GENERATOR)
    private LangASTNode expression;         // The produced element, e.g. [x * x for x in nums] → expression = x * x
    private LangASTNode keyExpression;      // The key in a dict comp, e.g. {k: v*v for (k, v) in items} → keyExpression = k
    private LangASTNode valueExpression;    // The value in a dict comp, e.g. {k: v*v for (k, v) in items} → valueExpression = v*v

    private List<LangComprehensionClause> clauses;

    public LangComprehensionExpression() {
        super(NodeTypeEnum.COMPREHENSION_EXPRESSION);
    }

    public LangComprehensionExpression(PositionInfo positionInfo) {
        super(NodeTypeEnum.COMPREHENSION_EXPRESSION, positionInfo);
    }

    public LangComprehensionExpression(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.COMPREHENSION_EXPRESSION, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public LangComprehensionExpression(PositionInfo positionInfo, LangComprehensionKind kind, LangASTNode expression, LangASTNode keyExpression, LangASTNode valueExpression, List<LangComprehensionClause> clauses) {
        super(NodeTypeEnum.COMPREHENSION_EXPRESSION, positionInfo);
        this.kind = kind;
        this.expression = expression;
        this.keyExpression = keyExpression;
        this.valueExpression = valueExpression;
        this.clauses = clauses;

        if (expression != null) {
            addChild(expression);
        }
        if (keyExpression != null) {
            addChild(keyExpression);
        }
        if (valueExpression != null) {
            addChild(valueExpression);
        }
        if (clauses != null) {
            for (LangComprehensionClause clause : clauses) {
                if (clause != null) {
                    addChild(clause);
                }
            }
        }
    }

    public LangComprehensionKind getKind() {
        return kind;
    }

    public void setKind(LangComprehensionKind kind) {
        this.kind = kind;
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
    public LangASTNode getKeyExpression() {
        return keyExpression;
    }

    public void setKeyExpression(LangASTNode keyExpression) {
        this.keyExpression = keyExpression;
        if (keyExpression != null) {
            addChild(keyExpression);
        }
    }

    public LangASTNode getValueExpression() {
        return valueExpression;
    }

    public void setValueExpression(LangASTNode valueExpression) {
        this.valueExpression = valueExpression;
        if (valueExpression != null) {
            addChild(valueExpression);
        }
    }

    public List<LangComprehensionClause> getClauses() {
        return clauses;
    }

    public void setClauses(List<LangComprehensionClause> clauses) {
        this.clauses = clauses;
        if (clauses != null) {
            for (LangComprehensionClause clause : clauses) {
                if (clause != null) {
                    addChild(clause);
                }
            }
        }
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangComprehensionExpression{" +
                "kind=" + kind +
                ", expression=" + expression +
                ", keyExpression=" + keyExpression +
                ", valueExpression=" + valueExpression +
                ", clauses=" + clauses +
                '}';
    }

    public enum LangComprehensionKind {
        LIST, SET, DICT, GENERATOR
    }

    public static class LangComprehensionClause extends LangASTNode {
        private boolean isAsync;
        private List<LangASTNode> targets;
        private LangExpression iterable;
        private List<LangASTNode> filters;

        public LangComprehensionClause() {
            super(NodeTypeEnum.COMPREHENSION_CLAUSE);
        }

        public LangComprehensionClause(PositionInfo positionInfo) {
            super(NodeTypeEnum.COMPREHENSION_CLAUSE, positionInfo);
        }

        public LangComprehensionClause(PositionInfo positionInfo, boolean isAsync, List<LangASTNode> targets, LangExpression iterable, List<LangASTNode> filters) {
            super(NodeTypeEnum.COMPREHENSION_CLAUSE, positionInfo);
            this.isAsync = isAsync;     // True if the clause uses 'async for'
            this.targets = targets;     // The target variables in 'for ... in ...', e.g. [a, b for (a, b) in pairs] → targets = (a, b)
            this.iterable = iterable;   // The expression after 'in', e.g. [x for x in range(n)] → iterable = range(n)
            this.filters = filters;     // The 'if' conditions after the for, e.g. [x for x in nums if x > 0] → filters = [x > 0]

            if (targets != null) {
                for (LangASTNode target : targets) {
                    if (target != null) {
                        addChild(target);
                    }
                }
            }
            if (iterable != null) {
                addChild(iterable);
            }
            if (filters != null) {
                for (LangASTNode filter : filters) {
                    if (filter != null) {
                        addChild(filter);
                    }
                }
            }
        }

        public boolean isAsync() {
            return isAsync;
        }

        public void setAsync(boolean async) {
            isAsync = async;
        }

        public List<LangASTNode> getTargets() {
            return targets;
        }

        public void setTargets(List<LangASTNode> targets) {
            this.targets = targets;
            if (targets != null) {
                for (LangASTNode target : targets) {
                    if (target != null) {
                        addChild(target);
                    }
                }
            }
        }

        public LangExpression getIterable() {
            return iterable;
        }

        public void setIterable(LangExpression iterable) {
            this.iterable = iterable;
            if (iterable != null) {
                addChild(iterable);
            }
        }

        public List<LangASTNode> getFilters() {
            return filters;
        }

        public void setFilters(List<LangASTNode> filters) {
            this.filters = filters;
            if (filters != null) {
                for (LangASTNode filter : filters) {
                    if (filter != null) {
                        addChild(filter);
                    }
                }
            }
        }

        @Override
        public void accept(LangASTVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return "LangComprehensionClause{" +
                    "isAsync=" + isAsync +
                    ", targets=" + targets +
                    ", iterable=" + iterable +
                    ", filters=" + filters +
                    '}';
        }
    }
}

