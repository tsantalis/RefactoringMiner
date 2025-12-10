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

public class PyASTFlattener implements LangASTFlattener {

    private final StringBuilder builder = new StringBuilder();

    private final LangASTNode root; // generic root node

    public PyASTFlattener(LangASTNode root) {
        this.root = root;
    }

    public static String flattenNode(LangASTNode node) {
        return new PyASTFlattener(node).getResult();
    }

    @Override
    public String getResult() {
        return builder.toString();
    }


    @Override
    public void visit(LangCompilationUnit unit) {
        List<LangASTNode> children = unit.getChildren();
        for (LangASTNode child : children) {
            child.accept(this);
        }
    }


    @Override
    public void visit(LangTypeDeclaration type) {
        List<LangSimpleName> superClasses = type.getSuperClassNames();
        builder.append("class ").append(type.getName());
        if (!superClasses.isEmpty()) {
            builder.append("(");
        }
        for(LangSimpleName superClass : superClasses) {
            builder.append(superClass.getIdentifier()).append(", ");
        }
        if (!superClasses.isEmpty()) {
            builder.append(")");
        }
        builder.append(":");
        List<LangASTNode> children = type.getChildren();
        for (LangASTNode child : children) {
            child.accept(this);
        }
    }

    @Override
    public void visit(LangMethodDeclaration method) {
        if (method.isAsync()) {
            builder.append("async ");
        }
        builder.append("def ").append(method.getName()).append("(");
        List<LangSingleVariableDeclaration> parameters = method.getParameters();
        for (LangSingleVariableDeclaration param : parameters) {
            if (param.isVarArgs()) builder.append("*");
            if (param.isKwArgs()) builder.append("**");
            param.accept(this);
            if (parameters.indexOf(param) < parameters.size() - 1){
                builder.append(", ");
            }
        }
        builder.append("):");
        for (LangASTNode stmt : method.getBody().getStatements()) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(LangSingleVariableDeclaration var) {
        var.getLangSimpleName().accept(this);
    }

    @Override
    public void visit(LangBlock block) {
        if (block.getStatements().isEmpty()) {
            return;
        }
        for (LangASTNode stmt : block.getStatements()) {
            if (stmt != null) {
                stmt.accept(this);
            }
        }
    }

    @Override
    public void visit(LangReturnStatement stmt) {
        builder.append("return");

        if (stmt.getExpression() != null) {
            builder.append(" ");
            stmt.getExpression().accept(this);
        }
        builder.append("\n");
    }

    @Override
    public void visit(LangInfixExpression expr) {
        expr.getLeft().accept(this);
        builder.append(" "); 
        builder.append(expr.getOperator().getJavaSymbol());
        builder.append(" ");
        expr.getRight().accept(this);
    }

    @Override
    public void visit(LangMethodInvocation langMethodInvocation) {
        // Append the name of the function/method
        langMethodInvocation.getExpression().accept(this);

        // Append the argument list
        List<LangASTNode> arguments = langMethodInvocation.getArguments();
        if (arguments != null) {
            builder.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                arguments.get(i).accept(this);
                if (i != arguments.size() - 1) {
                    builder.append(",");
                }
            }
            builder.append(")");
        }
    }

    @Override
    public void visit(LangSimpleName langSimpleName) {
        builder.append(langSimpleName.getIdentifier());
    }

    @Override
    public void visit(LangIfStatement langIfStatement) {
        builder.append("if ");
        langIfStatement.getCondition().accept(this);
        builder.append(":");
        langIfStatement.getBody().accept(this);

        LangASTNode elseBody = langIfStatement.getElseBody();

        while (elseBody instanceof LangIfStatement elifStatement) {
            builder.append("elif ");
            elifStatement.getCondition().accept(this);
            builder.append(":");
            elifStatement.getBody().accept(this);
            elseBody = elifStatement.getElseBody();
        }

        if (elseBody != null) {
            builder.append("else:");
            elseBody.accept(this);
        }
    }


    @Override
    public void visit(LangWhileStatement langWhileStatement) {
        builder.append("while ");
        langWhileStatement.getCondition().accept(this);
        builder.append(":");
        langWhileStatement.getBody().accept(this);
    }

    @Override
    public void visit(LangForStatement langForStatement) {
        builder.append("for ");
        langForStatement.getCondition().accept(this);
        builder.append(" in ");

        // Visit the iterable/collection
        for (LangASTNode initializer : langForStatement.getInitializers()) {
            initializer.accept(this);
        }

        builder.append(":");
        langForStatement.getBody().accept(this);
    }


    @Override
    public void visit(LangExpressionStatement langExpressionStatement) {
        langExpressionStatement.getExpression().accept(this);
        builder.append("\n");
    }

    @Override
    public void visit(LangAssignment langAssignment) {
        langAssignment.getLeftSide().accept(this);
        builder.append("=");
        langAssignment.getRightSide().accept(this);
    }

    @Override
    public void visit(LangBooleanLiteral langBooleanLiteral) {
        builder.append(langBooleanLiteral.getValue());
    }

    @Override
    public void visit(LangNumberLiteral langNumberLiteral) {
        builder.append(langNumberLiteral.getValue());
    }

    @Override
    public void visit(LangStringLiteral langStringLiteral) {
        builder.append("\"").append(langStringLiteral.getValue()).append("\"");
    }

    @Override
    public void visit(LangListLiteral langListLiteral) {
        builder.append("[");
        for (LangASTNode expr : langListLiteral.getElements()) {
            expr.accept(this);
            if (expr != langListLiteral.getElements().get(langListLiteral.getElements().size() - 1)) {
                builder.append(", ");
            }
        }
        builder.append("]");
    }

    @Override
    public void visit(LangFieldAccess langFieldAccess) {
        langFieldAccess.getExpression().accept(this);
        builder.append(".");
        langFieldAccess.getName().accept(this);
    }

    @Override
    public void visit(LangDictionaryLiteral langDictionaryLiteral) {
        builder.append("{");

        // Get the list of key-value pairs (entries)
        List<LangDictionaryLiteral.Entry> entries = langDictionaryLiteral.getEntries();

        // Iterate over the entries
        for (int i = 0; i < entries.size(); i++) {
            LangDictionaryLiteral.Entry entry = entries.get(i);

            // Flatten the key and append to the builder
            entry.getKey().accept(this);

            // Append the colon separator
            builder.append(": ");

            // Flatten the value and append to the builder
            entry.getValue().accept(this);

            // Append a comma for all entries except the last one
            if (i < entries.size() - 1) {
                builder.append(", ");
            }
        }

        // End the dictionary with a closing brace
        builder.append("}");
    }

    @Override
    public void visit(LangTupleLiteral langTupleLiteral) {
        builder.append("(");
        List<LangASTNode> elements = langTupleLiteral.getElements();
        int i = 0;
        for (LangASTNode expr : elements) {
            expr.accept(this);
            // Append a comma for all entries except the last one
            if (i < elements.size() - 1) {
                builder.append(", ");
            }
            i++;
        }
        builder.append(")");
    }

    @Override
    public void visit(LangImportStatement langImportStatement) {
        StringBuilder importBuilder = new StringBuilder();

        // Handle relative imports (dots for relative levels, only for "from" imports)
        int relativeLevel = langImportStatement.getRelativeLevel();
        if (relativeLevel > 0) {
            importBuilder.append(".".repeat(relativeLevel));
        }

        // Check for 'from' style imports
        if (langImportStatement.isFromImport()) {
            if (langImportStatement.getModuleName() != null && !langImportStatement.getModuleName().isEmpty()) {
                importBuilder.append("from ").append(langImportStatement.getModuleName()).append(" ");
            } else if (relativeLevel > 0) {
                importBuilder.append("from ");
            }

            // Add 'import'
            importBuilder.append("import ");

            // Handle wildcard imports
            if (langImportStatement.isWildcardImport()) {
                importBuilder.append("*");
            } else {
                // Flatten list of specific imports
                List<LangImportStatement.LangImportItem> imports = langImportStatement.getImports();
                for (int i = 0; i < imports.size(); i++) {
                    LangImportStatement.LangImportItem item = imports.get(i);

                    // Add the imported name
                    importBuilder.append(item.getName());

                    // If there's an alias, append it
                    if (item.getAlias() != null && !item.getAlias().isEmpty()) {
                        importBuilder.append(" as ").append(item.getAlias());
                    }

                    // Add a comma if this isn't the last item
                    if (i < imports.size() - 1) {
                        importBuilder.append(", ");
                    }
                }
            }
        } else {
            // Handle basic 'import module' statements
            importBuilder.append("import ");

            // Flatten list of modules being imported
            List<LangImportStatement.LangImportItem> imports = langImportStatement.getImports();
            for (int i = 0; i < imports.size(); i++) {
                LangImportStatement.LangImportItem item = imports.get(i);

                importBuilder.append(item.getName());

                // If there's an alias, append it
                if (item.getAlias() != null && !item.getAlias().isEmpty()) {
                    importBuilder.append(" as ").append(item.getAlias());
                }

                // Add a comma if this isn't the last item
                if (i < imports.size() - 1) {
                    importBuilder.append(", ");
                }
            }
        }

        importBuilder.append("\n");
        builder.append(importBuilder);
    }

    @Override
    public void visit(LangImportStatement.LangImportItem langImportItem) {
    	
    }

    @Override
    public void visit(LangPrefixExpression langPrefixExpression) {
        builder.append(langPrefixExpression.getOperator().getSymbol());
        langPrefixExpression.getOperand().accept(this);
    }

    @Override
    public void visit(LangPostfixExpression langPostFixExpression) {
        langPostFixExpression.getOperand().accept(this);
        builder.append(langPostFixExpression.getOperator().getSymbol());
    }

    @Override
    public void visit(LangNullLiteral langNullLiteral) {
        builder.append("None");
    }

    @Override
    public void visit(LangTryStatement langTryStatement) {
        builder.append("try:");
        langTryStatement.getBody().accept(this);
        for (LangCatchClause catchClause : langTryStatement.getCatchClauses()) {
            catchClause.accept(this);
        }
        if (langTryStatement.getFinallyBlock() != null) {
            builder.append("finally:");
            langTryStatement.getFinallyBlock().accept(this);
        }
    }

    @Override
    public void visit(LangCatchClause langCatchClause) {
        builder.append("except ");
        if (langCatchClause.getExceptionVariable() != null) {
            langCatchClause.getExceptionVariable().accept(this);
        }
        if (!langCatchClause.getExceptionTypes().isEmpty()) {
            langCatchClause.getExceptionTypes().forEach(type -> type.accept(this));
        }
        builder.append(":");
        langCatchClause.getBody().accept(this);
    }

    @Override
    public void visit(LangBreakStatement langBreakStatement) {
        builder.append("break");
        builder.append("\n");
    }

    @Override
    public void visit(LangContinueStatement langContinueStatement) {
        builder.append("continue");
        builder.append("\n");
    }

    @Override
    public void visit(LangDelStatement langDelStatement) {
        builder.append("del ");
        for (LangASTNode expr : langDelStatement.getTargets()){
            expr.accept(this);
        }
        builder.append("\n");
    }

    @Override
    public void visit(LangGlobalStatement langGlobalStatement) {
        builder.append("global ");
        langGlobalStatement.getVariableNames().forEach(name -> builder.append(name).append(", "));
        builder.append("\n");
    }

    @Override
    public void visit(LangPassStatement langPassStatement) {
        builder.append("pass");
        builder.append("\n");
    }


    @Override
    public void visit(LangYieldStatement stmt) {
        builder.append("yield");

        if (stmt.getExpression() != null) {
            builder.append(" ");
            stmt.getExpression().accept(this);
        }
        builder.append("\n");
    }

    @Override
    public void visit(LangAnnotation langAnnotation) {
        builder.append("@").append(langAnnotation.getName());
        if (langAnnotation.getArguments() != null && !langAnnotation.getArguments().isEmpty()) {
            builder.append("(");
            List<LangASTNode> arguments = langAnnotation.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                arguments.get(i).accept(this);
                if (i < arguments.size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append(")");
        }
    }


    @Override
    public void visit(LangAssertStatement langAssertStatement) {
        builder.append("assert ");

        if (langAssertStatement.getExpression() != null) {
            langAssertStatement.getExpression().accept(this);
        } else {
            builder.append("True");
        }

        if (langAssertStatement.getMessage() != null) {
            builder.append(", ");
            langAssertStatement.getMessage().accept(this);
        }
        builder.append("\n");
    }

    @Override
    public void visit(LangThrowStatement langThrowStatement) {
        builder.append("throw ");
        langThrowStatement.getExpressions().forEach(expr -> expr.accept(this));
        builder.append("\n");
    }

    @Override
    public void visit(LangWithContextItem langWithContextItem) {
        if (langWithContextItem.getContextExpression() != null) {
            langWithContextItem.getContextExpression().accept(this);
        }
        if (langWithContextItem.getAlias() != null) {
            builder.append(" as ");
            langWithContextItem.getAlias().accept(this);
        }
    }

    @Override
    public void visit(LangWithStatement langWithStatement) {
        builder.append("with ");
        List<LangASTNode> contextItems = langWithStatement.getContextItems();
        for (int i = 0; i < contextItems.size(); i++) {
            contextItems.get(i).accept(this);
            if (i < contextItems.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(":");
        langWithStatement.getBody().accept(this);
    }

    @Override
    public void visit(LangNonLocalStatement langNonLocalStatement) {
        builder.append("nonlocal ");
        List<LangSimpleName> names = langNonLocalStatement.getNames();
        for (int i = 0; i < names.size(); i++) {
            builder.append(names.get(i).getIdentifier());
            if (i < names.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append("\n");
    }


    @Override
    public void visit(LangAsyncStatement langAsyncStatement) {
        builder.append("async ");
        langAsyncStatement.getBody().accept(this);
    }

    @Override
    public void visit(LangAwaitExpression langAwaitExpression) {
        builder.append("await ");
        langAwaitExpression.getExpression().accept(this);
    }

    @Override
    public void visit(LangLambdaExpression langLambdaExpression) {
        builder.append("lambda ");
        langLambdaExpression.getParameters().forEach(param -> param.accept(this));
        builder.append(":");
        langLambdaExpression.getBody().accept(this);
    }

    @Override
    public void visit(LangSwitchStatement langSwitchStatement) {
        builder.append("match ");
        langSwitchStatement.getExpression().accept(this);
        builder.append(":");
        for (LangCaseStatement caseStatement : langSwitchStatement.getCases()) {
            caseStatement.accept(this);
        }
    }


    @Override
    public void visit(LangCaseStatement langCaseStatement) {
        builder.append("case ");
        if (langCaseStatement.getPattern() != null) {
            langCaseStatement.getPattern().accept(this);
        }
        builder.append(":");
        langCaseStatement.getBody().accept(this);
    }

    @Override
    public void visit(LangVariablePattern langVariablePattern) {
        builder.append(langVariablePattern.getVariableName());
    }

    @Override
    public void visit(LangLiteralPattern langLiteralPattern) {
        builder.append(langLiteralPattern.getValue());
    }

    @Override
    public void visit(LangComment langComment) {
        builder.append(langComment.getContent());
    }

    @Override
    public void visit(LangTernaryExpression langTernaryExpression) {
        langTernaryExpression.getCondition().accept(this);
        builder.append(" if ");
        langTernaryExpression.getThenExpression().accept(this);
        builder.append(" else ");
        langTernaryExpression.getElseExpression().accept(this);
    }

    @Override
    public void visit(LangIndexAccess langIndexAccess) {
        langIndexAccess.getTarget().accept(this);
        builder.append("[");
        langIndexAccess.getIndex().accept(this);
        builder.append("]");
    }

    @Override
    public void visit(LangSliceExpression slice) {
        if (slice.getLower() != null) slice.getLower().accept(this);
        builder.append(":");
        if (slice.getUpper() != null) slice.getUpper().accept(this);
        if (slice.getStep() != null) {
            builder.append(":");
            if (slice.getUpper() == null && slice.getStep() == null){
                builder.append(":");
            }
            slice.getStep().accept(this);
        }
        if (slice.getLower() == null && slice.getUpper() == null && slice.getStep() == null) {
            builder.append(":");
        }
    }

    @Override
    public void visit(LangEllipsisLiteral langEllipsisLiteral) {
        builder.append("...");
    }

    @Override
    public void visit(LangParenthesizedExpression langParenthesizedExpression) {
        builder.append("(");
        if (langParenthesizedExpression.getParenthesizedExpression() != null){
            langParenthesizedExpression.getParenthesizedExpression().accept(this);
        }
        builder.append(")");
    }

    @Override
    public void visit(LangComprehensionExpression langComprehensionExpression) {
        switch (langComprehensionExpression.getKind()) {
            case LIST -> builder.append("[");
            case SET, DICT -> builder.append("{");
            case GENERATOR -> builder.append("(");
        }

        if (LangComprehensionExpression.LangComprehensionKind.DICT.equals(langComprehensionExpression.getKind())) {
            // Dictionary comprehension: {key: value for ...}
            if (langComprehensionExpression.getKeyExpression() != null) {
                langComprehensionExpression.getKeyExpression().accept(this);
                builder.append(": ");
            }
            if (langComprehensionExpression.getValueExpression() != null) {
                langComprehensionExpression.getValueExpression().accept(this);
                builder.append(" ");
            }
        } else {
            // List, set or generator: expr for ...
            if (langComprehensionExpression.getExpression() != null) {
                langComprehensionExpression.getExpression().accept(this);
                builder.append(" ");
            }
        }

        // Handle comprehension clauses
        for (LangComprehensionExpression.LangComprehensionClause clause : langComprehensionExpression.getClauses()) {
            clause.accept(this);
        }

        switch (langComprehensionExpression.getKind()) {
            case LIST -> builder.append("]");
            case SET, DICT -> builder.append("}");
            case GENERATOR -> builder.append(")");
        }
    }

    @Override
    public void visit(LangComprehensionExpression.LangComprehensionClause langComprehensionClause) {
        builder.append("for ");

        if (langComprehensionClause.isAsync()) {
            builder.append("async ");
        }

        // Handle targets
        List<LangASTNode> targets = langComprehensionClause.getTargets();
        if (targets != null && !targets.isEmpty()) {
            for (int i = 0; i < targets.size(); i++) {
                targets.get(i).accept(this);
                if (i < targets.size() - 1) {
                    builder.append(", ");
                }
            }
        }

        builder.append(" in ");

        if (langComprehensionClause.getIterable() != null) {
            langComprehensionClause.getIterable().accept(this);
        }

        List<LangASTNode> filters = langComprehensionClause.getFilters();
        if (filters != null && !filters.isEmpty()) {
            for (LangASTNode filter : filters) {
                builder.append(" if ");
                filter.accept(this);
            }
        }
    }

}
