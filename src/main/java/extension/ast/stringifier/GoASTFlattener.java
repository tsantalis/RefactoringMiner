package extension.ast.stringifier;

import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.*;
import extension.ast.node.literal.*;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.pattern.LangAsPattern;
import extension.ast.node.pattern.LangLiteralPattern;
import extension.ast.node.pattern.LangVariablePattern;
import extension.ast.node.statement.*;
import extension.ast.node.unit.LangCompilationUnit;

import java.util.List;

public class GoASTFlattener implements LangASTFlattener {

    private final StringBuilder builder = new StringBuilder();
    private int indentLevel = 0;
    private static final String INDENT = "\t";

    private final LangASTNode root; // generic root node

    public GoASTFlattener(LangASTNode root) {
        this.root = root;
    }

    private void indent() {
        builder.append(INDENT.repeat(Math.max(0, indentLevel)));
    }

    private void newline() {
        builder.append("\n");
    }

    @Override
    public String getResult() {
        return builder.toString();
    }

    @Override
    public void visit(LangCompilationUnit unit) {
        List<LangASTNode> children = unit.getChildren();
        for (LangASTNode child : children) {
            if (child != null) child.accept(this);
        }
    }

    @Override
    public void visit(LangTypeDeclaration type) {
        indent();
        builder.append("type ").append(type.getName());
        builder.append(type.isInterface() ? " interface {" : " struct {");
        newline();
        indentLevel++;
        for (LangSingleVariableDeclaration field : type.getFields()) {
            field.accept(this);
        }
        List<LangMethodDeclaration> methods = type.getMethods();
        for (LangMethodDeclaration method : methods) {
            method.accept(this);
        }
        indentLevel--;
        indent();
        builder.append("}");
        newline();
    }

    @Override
    public void visit(LangMethodDeclaration method) {
        indent();
        builder.append("func ");
        if (method.getReceiverType() != null) {
            builder.append("(").append(method.getReceiverType()).append(") ");
        }
        builder.append(method.getName()).append("(");
        List<LangSingleVariableDeclaration> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).accept(this);
            if (i < parameters.size() - 1) builder.append(", ");
        }
        builder.append(")");
        List<String> returnTypes = method.getReturnTypeAnnotations();
        if (!returnTypes.isEmpty()) {
            builder.append(" (").append(String.join(", ", returnTypes)).append(")");
        }
        builder.append(" {");
        newline();
        indentLevel++;
        if (method.getBody() != null) {
            for (LangASTNode stmt : method.getBody().getStatements()) {
                if (stmt != null) stmt.accept(this);
            }
        }
        indentLevel--;
        indent();
        builder.append("}");
        newline();
    }

    @Override
    public void visit(LangSingleVariableDeclaration var) {
        indent();
        if (var.getLangSimpleName() != null) {
            var.getLangSimpleName().accept(this);
        }
        if (var.getTypeAnnotationText() != null) {
            builder.append(" ").append(var.getTypeAnnotationText());
        }
        newline();
    }

    @Override
    public void visit(LangBlock block) {
        if (block.getStatements().isEmpty()) {
            return;
        }
        for (LangASTNode stmt : block.getStatements()) {
            if (stmt != null) stmt.accept(this);
        }
    }

    @Override
    public void visit(LangReturnStatement stmt) {
        indent();
        builder.append("return");
        if (stmt.getExpression() != null) {
            builder.append(" ");
            stmt.getExpression().accept(this);
        }
        newline();
    }

    @Override
    public void visit(LangInfixExpression expr) {
        expr.getLeft().accept(this);
        builder.append(" ").append(expr.getOperator().getSymbol()).append(" ");
        expr.getRight().accept(this);
    }

    @Override
    public void visit(LangMethodInvocation call) {
        call.getExpression().accept(this);
        List<LangASTNode> arguments = call.getArguments();
        builder.append("(");
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                arguments.get(i).accept(this);
                if (i != arguments.size() - 1) builder.append(", ");
            }
        }
        builder.append(")");
    }

    @Override
    public void visit(LangSimpleName name) {
        builder.append(name.getIdentifier());
    }

    @Override
    public void visit(LangIfStatement stmt) {
        indent();
        builder.append("if ");
        stmt.getCondition().accept(this);
        builder.append(" {");
        newline();
        indentLevel++;
        stmt.getBody().accept(this);
        indentLevel--;
        indent();
        builder.append("}");
        LangASTNode elseBody = stmt.getElseBody();
        while (elseBody instanceof LangIfStatement elif) {
            builder.append(" else if ");
            elif.getCondition().accept(this);
            builder.append(" {");
            newline();
            indentLevel++;
            elif.getBody().accept(this);
            indentLevel--;
            indent();
            builder.append("}");
            elseBody = elif.getElseBody();
        }
        if (elseBody != null) {
            builder.append(" else {");
            newline();
            indentLevel++;
            elseBody.accept(this);
            indentLevel--;
            indent();
            builder.append("}");
        }
        newline();
    }

    @Override
    public void visit(LangWhileStatement stmt) {
        indent();
        builder.append("for ");
        stmt.getCondition().accept(this);
        builder.append(" {");
        newline();
        indentLevel++;
        stmt.getBody().accept(this);
        indentLevel--;
        indent();
        builder.append("}");
        newline();
    }

    @Override
    public void visit(LangForStatement stmt) {
        indent();
        builder.append("for ");
        if (stmt.getCondition() != null) {
            stmt.getCondition().accept(this);
        } else {
            builder.append("range ");
            List<? extends LangASTNode> inits = stmt.getInitializers();
            for (int i = 0; i < inits.size(); i++) {
                inits.get(i).accept(this);
                if (i < inits.size() - 1) builder.append(", ");
            }
        }
        builder.append(" {");
        newline();
        indentLevel++;
        stmt.getBody().accept(this);
        indentLevel--;
        indent();
        builder.append("}");
        newline();
    }

    @Override
    public void visit(LangExpressionStatement stmt) {
        indent();
        if (stmt.getExpression() != null) {
            stmt.getExpression().accept(this);
        }
        newline();
    }

    @Override
    public void visit(LangAssignment assign) {
        assign.getLeftSide().accept(this);
        builder.append(" = ");
        assign.getRightSide().accept(this);
    }

    @Override
    public void visit(LangBooleanLiteral lit) {
        builder.append(lit.getValue());
    }

    @Override
    public void visit(LangNumberLiteral lit) {
        builder.append(lit.getValue());
    }

    @Override
    public void visit(LangStringLiteral lit) {
        builder.append("\"").append(lit.getValue()).append("\"");
    }

    @Override
    public void visit(LangListLiteral lit) {
        builder.append("[]{");
        List<LangASTNode> elems = lit.getElements();
        for (int i = 0; i < elems.size(); i++) {
            elems.get(i).accept(this);
            if (i < elems.size() - 1) builder.append(", ");
        }
        builder.append("}");
    }

    @Override
    public void visit(LangFieldAccess access) {
        access.getExpression().accept(this);
        builder.append(".");
        access.getName().accept(this);
    }

    @Override
    public void visit(LangDictionaryLiteral dict) {
        builder.append("map{");
        List<LangDictionaryLiteral.Entry> entries = dict.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LangDictionaryLiteral.Entry e = entries.get(i);
            e.getKey().accept(this);
            builder.append(": ");
            e.getValue().accept(this);
            if (i < entries.size() - 1) builder.append(", ");
        }
        builder.append("}");
    }

    @Override
    public void visit(LangTupleLiteral tuple) {
        builder.append("(");
        List<LangASTNode> elems = tuple.getElements();
        for (int i = 0; i < elems.size(); i++) {
            elems.get(i).accept(this);
            if (i < elems.size() - 1) builder.append(", ");
        }
        builder.append(")");
    }

    @Override
    public void visit(LangImportStatement imp) {
        List<LangImportStatement.LangImportItem> imports = imp.getImports();
        if (imports != null && !imports.isEmpty()) {
            for (int i = 0; i < imports.size(); i++) {
                LangImportStatement.LangImportItem item = imports.get(i);
                builder.append("import ");
                if (item.getAlias() != null && !item.getAlias().isEmpty()) {
                    builder.append(item.getAlias()).append(" ");
                }
                builder.append("\"").append(item.getName()).append("\"\n");
            }
        } else if (imp.getModuleName() != null) {
            builder.append("import \"").append(imp.getModuleName()).append("\"\n");
        }
    }

    @Override
    public void visit(LangImportStatement.LangImportItem langImportItem) {

    }

    @Override
    public void visit(LangTemplateStringExpression langTemplateStringExpression) {

    }

    @Override
    public void visit(LangTemplateExpressionPart langTemplateExpressionPart) {

    }

    @Override
    public void visit(LangPrefixExpression expr) {
        builder.append(expr.getOperator().getSymbol());
        expr.getOperand().accept(this);
    }

    @Override
    public void visit(LangPostfixExpression expr) {
        expr.getOperand().accept(this);
        builder.append(expr.getOperator().getSymbol());
    }

    @Override
    public void visit(LangNullLiteral lit) {
        builder.append("nil");
    }

    @Override
    public void visit(LangTryStatement stmt) {
        // Go has no try/catch (uses multi-value returns + `error`): rendered generically so a
        // stable hash still exists if this is ever reached via a shared code path.
        indent();
        builder.append("try {");
        newline();
        indentLevel++;
        stmt.getBody().accept(this);
        indentLevel--;
        indent();
        builder.append("}");
        for (LangCatchClause c : stmt.getCatchClauses()) {
            newline();
            c.accept(this);
        }
        if (stmt.getFinallyBlock() != null) {
            indent();
            builder.append("finally {");
            newline();
            indentLevel++;
            stmt.getFinallyBlock().accept(this);
            indentLevel--;
            indent();
            builder.append("}");
        }
        newline();
    }

    @Override
    public void visit(LangCatchClause c) {
        indent();
        builder.append("catch (");
        if (!c.getExceptionTypes().isEmpty()) {
            for (int i = 0; i < c.getExceptionTypes().size(); i++) {
                c.getExceptionTypes().get(i).accept(this);
                if (i < c.getExceptionTypes().size() - 1) builder.append(" | ");
            }
        }
        if (c.getExceptionVariable() != null) {
            if (!c.getExceptionTypes().isEmpty()) builder.append(" ");
            c.getExceptionVariable().accept(this);
        }
        builder.append(") {");
        newline();
        indentLevel++;
        c.getBody().accept(this);
        indentLevel--;
        indent();
        builder.append("}");
        newline();
    }

    @Override
    public void visit(LangBreakStatement s) {
        indent();
        builder.append("break");
        newline();
    }

    @Override
    public void visit(LangContinueStatement s) {
        indent();
        builder.append("continue");
        newline();
    }

    @Override
    public void visit(LangDelStatement s) {
        // Go has no statement-level "delete a name" construct - the closest thing, delete(m, key)
        // for maps, is an ordinary builtin call (LangMethodInvocation), not this node type.
        // GoASTBuilder never produces a LangDelStatement.
    }

    @Override
    public void visit(LangGlobalStatement s) {
        // Go has no "declare this name as referring to package scope" statement - every
        // identifier's scope is resolved lexically. GoASTBuilder never produces this node type.
    }

    @Override
    public void visit(LangPassStatement s) {
        // Go has no no-op placeholder statement - an empty block ({}) already means "do nothing".
        // GoASTBuilder never produces this node type.
    }

    @Override
    public void visit(LangYieldStatement stmt) {
        // Go has no generator/yield construct - concurrent producer/consumer patterns are built
        // from goroutines and channels instead, a structurally different mechanism (not yet
        // modeled by any shared LangASTNode type). GoASTBuilder never produces this node type.
    }

    @Override
    public void visit(LangAnnotation a) {
        // Go has no annotation/decorator syntax
    }

    @Override
    public void visit(LangAssertStatement s) {
        builder.append("assert(");
        if (s.getExpression() != null) {
            s.getExpression().accept(this);
        } else {
            builder.append("true");
        }
        if (s.getMessage() != null) {
            builder.append(", ");
            s.getMessage().accept(this);
        }
        builder.append(")");
    }

    @Override
    public void visit(LangThrowStatement s) {
        builder.append("panic(");
        List<LangASTNode> exprs = s.getExpressions();
        for (int i = 0; i < exprs.size(); i++) {
            exprs.get(i).accept(this);
            if (i < exprs.size() - 1) builder.append(", ");
        }
        builder.append(")");
    }

    @Override
    public void visit(LangWithContextItem item) {
        // Go has no scoped context-manager construct - the nearest idiom, `defer f.Close()`, runs
        // at function-exit rather than block-exit and is a plain statement, not a context object.
        // GoASTBuilder never produces this node type.
    }

    @Override
    public void visit(LangWithStatement s) {
        // See visit(LangWithContextItem) above - Go's `defer`/`go`/`select` don't fit this node's
        // shape and would need their own dedicated node types in a later step. GoASTBuilder never
        // produces this node type.
    }

    @Override
    public void visit(LangNonLocalStatement s) {
        // Go closures capture enclosing variables by reference natively, with no keyword needed
        // to opt into it. GoASTBuilder never produces this node type.
    }

    @Override
    public void visit(LangAsyncStatement s) {
        // Go's concurrency model is goroutines (`go f()`), started with an ordinary statement
        // rather than a function/statement modifier - not a fit for this node's shape. GoASTBuilder
        // never produces this node type.
    }

    @Override
    public void visit(LangAwaitExpression e) {
        // Go has no async/await - a blocking call is written the same as any other call, and
        // concurrency is opted into explicitly via `go`/channels instead. GoASTBuilder never
        // produces this node type; the fallback (render the inner expression, dropping the
        // wrapper) exists only so a stable hash still results if this is ever reached via a
        // shared code path.
        e.getExpression().accept(this);
    }

    @Override
    public void visit(LangLambdaExpression e) {
        builder.append("func(");
        for (int i = 0; i < e.getParameters().size(); i++) {
            e.getParameters().get(i).accept(this);
            if (i < e.getParameters().size() - 1) builder.append(", ");
        }
        builder.append(") { ");
        e.getBody().accept(this);
        builder.append(" }");
    }

    @Override
    public void visit(LangSwitchStatement s) {
        indent();
        builder.append("switch ");
        s.getExpression().accept(this);
        builder.append(" {");
        newline();
        for (LangCaseStatement cs : s.getCases()) {
            cs.accept(this);
        }
        indent();
        builder.append("}");
        newline();
    }

    @Override
    public void visit(LangCaseStatement c) {
        indent();
        builder.append("case ");
        if (c.getPattern() != null) c.getPattern().accept(this);
        builder.append(":");
        newline();
        c.getBody().accept(this);
    }

    @Override
    public void visit(LangVariablePattern p) {
        builder.append(p.getVariableName());
    }

    @Override
    public void visit(LangAsPattern p) {
    }

    @Override
    public void visit(LangLiteralPattern p) {
        builder.append(p.getValue());
    }

    @Override
    public void visit(LangComment c) {
        builder.append(c.getContent());
    }

    @Override
    public void visit(LangTernaryExpression langTernaryExpression) {

    }

    @Override
    public void visit(LangIndexAccess langIndexAccess) {

    }

    @Override
    public void visit(LangSliceExpression langSliceExpression) {

    }

    @Override
    public void visit(LangEllipsisLiteral langEllipsisLiteral) {

    }

    @Override
    public void visit(LangComprehensionExpression langComprehensionExpression) {
        // Go has no comprehension syntax - list/map/set construction is always an explicit for
        // loop. GoASTBuilder never produces this node type.
    }

    @Override
    public void visit(LangComprehensionExpression.LangComprehensionClause langComprehensionClause) {
        // See visit(LangComprehensionExpression) above. GoASTBuilder never produces this node type.
    }

    @Override
    public void visit(LangParenthesizedExpression langParenthesizedExpression) {

    }
}
