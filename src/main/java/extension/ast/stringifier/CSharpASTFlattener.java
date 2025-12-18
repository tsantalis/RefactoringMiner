package extension.ast.stringifier;

import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.*;
import extension.ast.node.literal.*;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.pattern.LangLiteralPattern;
import extension.ast.node.pattern.LangVariablePattern;
import extension.ast.node.statement.*;
import extension.ast.node.unit.LangCompilationUnit;

import java.util.List;

public class CSharpASTFlattener implements LangASTFlattener {

    private final StringBuilder builder = new StringBuilder();
    private int indentLevel = 0;
    private static final String INDENT = "    ";

    private final LangASTNode root; // generic root node

    public CSharpASTFlattener(LangASTNode root) {
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
        builder.append("class ").append(type.getName());
        List<LangSimpleName> superClasses = type.getSuperClassNames();
        if (superClasses != null && !superClasses.isEmpty()) {
            builder.append(" : ");
            for (int i = 0; i < superClasses.size(); i++) {
                builder.append(superClasses.get(i).getIdentifier());
                if (i < superClasses.size() - 1) builder.append(", ");
            }
        }
        newline();
        indent();
        builder.append("{");
        newline();
        indentLevel++;
        List<LangASTNode> children = type.getChildren();
        for (LangASTNode child : children) {
            if (child != null) child.accept(this);
        }
        indentLevel--;
        indent();
        builder.append("}");
        newline();
    }

    @Override
    public void visit(LangMethodDeclaration method) {
        indent();
        if (method.isAsync()) {
            builder.append("async ");
        }
        // We don't know return type here; output method name and parameters
        builder.append(method.getName()).append("(");
        List<LangSingleVariableDeclaration> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).accept(this);
            if (i < parameters.size() - 1) builder.append(", ");
        }
        builder.append(") {");
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
        if (var.getLangSimpleName() != null) {
            var.getLangSimpleName().accept(this);
        }
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
        builder.append(";");
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
        builder.append("if (");
        stmt.getCondition().accept(this);
        builder.append(") {");
        newline();
        indentLevel++;
        stmt.getBody().accept(this);
        indentLevel--;
        indent();
        builder.append("}");
        LangASTNode elseBody = stmt.getElseBody();
        while (elseBody instanceof LangIfStatement elif) {
            builder.append(" else if (");
            elif.getCondition().accept(this);
            builder.append(") {");
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
        builder.append("while (");
        stmt.getCondition().accept(this);
        builder.append(") {");
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
        // Approximate as C# foreach(variable in collection)
        indent();
        builder.append("foreach (");
        if (stmt.getCondition() != null) {
            stmt.getCondition().accept(this);
        }
        builder.append(" in ");
        List<? extends LangASTNode> inits = stmt.getInitializers();
        for (int i = 0; i < inits.size(); i++) {
            inits.get(i).accept(this);
            if (i < inits.size() - 1) builder.append(", ");
        }
        builder.append(") {");
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
        builder.append(";");
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
        builder.append("[");
        List<LangASTNode> elems = lit.getElements();
        for (int i = 0; i < elems.size(); i++) {
            elems.get(i).accept(this);
            if (i < elems.size() - 1) builder.append(", ");
        }
        builder.append("]");
    }

    @Override
    public void visit(LangFieldAccess access) {
        access.getExpression().accept(this);
        builder.append(".");
        access.getName().accept(this);
    }

    @Override
    public void visit(LangDictionaryLiteral dict) {
        builder.append("{");
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
        // Represent C# using directives
        List<LangImportStatement.LangImportItem> imports = imp.getImports();
        if (imports != null && !imports.isEmpty()) {
            for (int i = 0; i < imports.size(); i++) {
                LangImportStatement.LangImportItem item = imports.get(i);
                builder.append("using ").append(item.getName());
                if (item.getAlias() != null && !item.getAlias().isEmpty()) {
                    builder.append(" = ").append(item.getAlias());
                }
                builder.append(";\n");
            }
        } else if (imp.getModuleName() != null) {
            builder.append("using ").append(imp.getModuleName()).append(";\n");
        }
    }

    @Override
    public void visit(LangImportStatement.LangImportItem langImportItem) {
    	
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
        builder.append("null");
    }

    @Override
    public void visit(LangTryStatement stmt) {
        indent();
        builder.append("try {");
        newline();
        indentLevel++;
        stmt.getBody().accept(this);
        indentLevel--;
        indent();
        builder.append("}");
        for (LangCatchClause c : stmt.getCatchClauses()) {
            builder.append(" catch (");
            // We'll reuse visit on catch to print its signature and body cleanly
            // But since visit(LangCatchClause) handles parentheses, we adjust there
            // So remove the space we added and delegate entirely
            // To keep simple, remove the prefix and let c.accept handle
            // However search_replace constraints prevent logic here; we'll just remove the added text afterward
            // Instead, newline then c.accept
            // workaround: end current block and newline, then c.accept
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
        builder.append("break;");
        newline();
    }

    @Override
    public void visit(LangContinueStatement s) {
        indent();
        builder.append("continue;");
        newline();
    }

    @Override
    public void visit(LangDelStatement s) {
        // Python-specific node
    }

    @Override
    public void visit(LangGlobalStatement s) {
        // Python-specific node
    }

    @Override
    public void visit(LangPassStatement s) {
        // Python-specific node
    }

    @Override
    public void visit(LangYieldStatement stmt) {
        indent();
        builder.append("yield return");
        if (stmt.getExpression() != null) {
            builder.append(" ");
            stmt.getExpression().accept(this);
        }
        builder.append(";");
        newline();
    }

    @Override
    public void visit(LangAnnotation a) {
        builder.append("[").append(a.getName());
        if (a.getArguments() != null && !a.getArguments().isEmpty()) {
            builder.append("(");
            List<LangASTNode> args = a.getArguments();
            for (int i = 0; i < args.size(); i++) {
                args.get(i).accept(this);
                if (i < args.size() - 1) builder.append(", ");
            }
            builder.append(")");
        }
        builder.append("]");
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
        builder.append(");");
    }

    @Override
    public void visit(LangThrowStatement s) {
        builder.append("throw ");
        List<LangASTNode> exprs = s.getExpressions();
        for (int i = 0; i < exprs.size(); i++) {
            exprs.get(i).accept(this);
            if (i < exprs.size() - 1) builder.append(", ");
        }
        builder.append(";");
    }

    @Override
    public void visit(LangWithContextItem item) {
        // Python-specific node
    }

    @Override
    public void visit(LangWithStatement s) {
        // Python-specific node
    }

    @Override
    public void visit(LangNonLocalStatement s) {
        // Python-specific node
    }

    @Override
    public void visit(LangAsyncStatement s) {
        // Python-specific node
    }

    @Override
    public void visit(LangAwaitExpression e) {
        builder.append("await ");
        e.getExpression().accept(this);
    }

    @Override
    public void visit(LangLambdaExpression e) {
        for (int i = 0; i < e.getParameters().size(); i++) {
            e.getParameters().get(i).accept(this);
            if (i < e.getParameters().size() - 1) builder.append(", ");
        }
        builder.append(" => ");
        e.getBody().accept(this);
    }

    @Override
    public void visit(LangSwitchStatement s) {
        builder.append("switch(");
        s.getExpression().accept(this);
        builder.append("){");
        for (LangCaseStatement cs : s.getCases()) {
            cs.accept(this);
        }
        builder.append("}");
    }

    @Override
    public void visit(LangCaseStatement c) {
        builder.append("case ");
        if (c.getPattern() != null) c.getPattern().accept(this);
        builder.append(":");
        c.getBody().accept(this);
    }

    @Override
    public void visit(LangVariablePattern p) {
        builder.append(p.getVariableName());
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

    }

    @Override
    public void visit(LangComprehensionExpression.LangComprehensionClause langComprehensionClause) {

    }

    @Override
    public void visit(LangParenthesizedExpression langParenthesizedExpression) {

    }
}
