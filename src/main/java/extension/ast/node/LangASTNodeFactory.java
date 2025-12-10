package extension.ast.node;

import extension.ast.builder.python.PyASTBuilderUtil;
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
import extension.base.lang.python.Python3Parser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LangASTNodeFactory {

    /** Compilation Unit */
    public static LangCompilationUnit createCompilationUnit(Python3Parser.File_inputContext ctx) {
        return new LangCompilationUnit(PositionUtils.getPositionInfo(ctx));
    }

    /** Generic overload for non-Python languages (e.g., C#) */
    public static LangCompilationUnit createCompilationUnit(org.antlr.v4.runtime.ParserRuleContext ctx) {
        return new LangCompilationUnit(PositionUtils.getPositionInfo(ctx));
    }

    /**
     * Creates a basic import statement with position information only
     */
    public static LangImportStatement createImportStatement(ParserRuleContext ctx) {
        return new LangImportStatement(PositionUtils.getPositionInfo(ctx));
    }

    /** Declarations */
    public static LangTypeDeclaration createTypeDeclaration(Python3Parser.ClassdefContext ctx) {
        LangTypeDeclaration type = new LangTypeDeclaration(PositionUtils.getPositionInfo(ctx));
        type.setName(ctx.name().getText());
        return type;
    }

    /** Generic type declaration creation for non-Python languages (e.g., C#) */
    public static LangTypeDeclaration createTypeDeclaration(String name, ParserRuleContext ctx) {
        LangTypeDeclaration type = new LangTypeDeclaration(PositionUtils.getPositionInfo(ctx));
        type.setName(name);
        return type;
    }

    public static LangMethodDeclaration createMethodDeclaration(String name, ParserRuleContext ctx, List<LangSingleVariableDeclaration> langSingleVariableDeclarations, LangBlock body) {
        LangMethodDeclaration method = new LangMethodDeclaration(PositionUtils.getPositionInfo(ctx));
        method.setName(name);
        if (langSingleVariableDeclarations != null) {
            langSingleVariableDeclarations.forEach(method::addParameter);
        }
        if (body != null) {
            method.setBody(body);
        }
        return method;
    }

    public static LangSingleVariableDeclaration createSingleVariableDeclaration(String name, LangASTNode defaultValue, ParserRuleContext ctx) {
        LangSimpleName langSimpleName = createSimpleName(name, ctx);
        LangSingleVariableDeclaration decl = new LangSingleVariableDeclaration(langSimpleName, defaultValue, PositionUtils.getPositionInfo(ctx));
        // Default sane values for non-annotated languages (e.g., C# minimal support)
        decl.setTypeAnnotation(TypeObjectEnum.OBJECT);
        //decl.setParameter(true);
        return decl;
    }

    /** Expressions */
    public static LangSimpleName createSimpleName(String name, ParserRuleContext ctx) {
        return new LangSimpleName(name, PositionUtils.getPositionInfo(ctx));
    }

    public static LangAssignment createAssignment(String operator, LangASTNode left, LangASTNode right, ParserRuleContext ctx) {
        LangAssignment langAssignment = new LangAssignment(operator, left, right, PositionUtils.getPositionInfo(ctx));
        langAssignment.addChild(left);
        langAssignment.addChild(right);
        return langAssignment;
    }

    public static LangInfixExpression createInfixExpression(LangASTNode left, LangASTNode right, String operatorSymbol, ParserRuleContext ctx) {
        OperatorEnum operator = OperatorEnum.fromSymbol(operatorSymbol);
        return new LangInfixExpression(left, operator, right, PositionUtils.getPositionInfo(ctx));
    }

    public static LangMethodInvocation createMethodInvocation(ParserRuleContext ctx) {
        return new LangMethodInvocation(PositionUtils.getPositionInfo(ctx));
    }

    public static LangFieldAccess createFieldAccess(LangASTNode expression, String fieldName, ParserRuleContext ctx) {
        LangSimpleName name = createSimpleName(fieldName, ctx);
        return new LangFieldAccess(expression, name, PositionUtils.getPositionInfo(ctx));
    }

    /** Prefix and Postfix Expressions */
    //TODO
    public static LangPrefixExpression createPrefixExpression(LangASTNode operand, String operatorSymbol, ParserRuleContext ctx) {
        LangPrefixExpression expression = new LangPrefixExpression(PositionUtils.getPositionInfo(ctx));
        expression.setOperand(operand);
        expression.setOperator(OperatorEnum.fromSymbol(operatorSymbol));
        return expression;
    }

    public static LangPostfixExpression createPostfixExpression(LangASTNode operand, String operatorSymbol, ParserRuleContext ctx) {
        LangPostfixExpression expression = new LangPostfixExpression(PositionUtils.getPositionInfo(ctx));
        expression.setOperand(operand);
        expression.setOperator(OperatorEnum.fromSymbol(operatorSymbol));
        return expression;
    }


    /** Statements */
    public static LangBlock createBlock(ParserRuleContext ctx, List<LangASTNode> statements) {
        LangBlock langBlock = new LangBlock(PositionUtils.getPositionInfo(ctx));
        if (statements != null) {
            statements.forEach(langBlock::addStatement);
        }
        return langBlock;
    }

    public static LangIfStatement createIfStatement(LangASTNode condition, LangBlock body, LangASTNode elseBody, ParserRuleContext ctx) {
        return new LangIfStatement(condition, body, elseBody, PositionUtils.getPositionInfo(ctx));
    }

    public static LangForStatement createForStatement(List<LangSingleVariableDeclaration> initializers, LangASTNode condition,
                                                      List<LangASTNode> updates, LangBlock loopBody,
                                                      LangBlock elseBody, ParserRuleContext ctx) {
        return new LangForStatement(initializers, condition, updates, loopBody, elseBody, PositionUtils.getPositionInfo(ctx));
    }

    public static LangWhileStatement createWhileStatement(LangASTNode condition, LangBlock body, LangBlock elseBody, ParserRuleContext ctx) {
        return new LangWhileStatement(condition, body, elseBody, PositionUtils.getPositionInfo(ctx));
    }

    public static LangReturnStatement createReturnStatement(LangASTNode expression, ParserRuleContext ctx) {
        LangReturnStatement langReturnStatement = new LangReturnStatement(PositionUtils.getPositionInfo(ctx));
        if (expression != null) {
            langReturnStatement.setExpression(expression);
        }
        return langReturnStatement;
    }

    public static LangExpressionStatement createExpressionStatement(LangASTNode expression, ParserRuleContext ctx) {
        LangExpressionStatement statement = new LangExpressionStatement(PositionUtils.getPositionInfo(ctx));
        if (expression != null) {
            statement.setExpression(expression);
            statement.addChild(expression);
        }

        return statement;
    }

    public static LangAwaitExpression createAwaitExpression(ParserRuleContext ctx, LangASTNode expression) {
        LangAwaitExpression awaitExpression = new LangAwaitExpression(PositionUtils.getPositionInfo(ctx));
        if (expression != null) {
            awaitExpression.setExpression(expression);
        }
        return awaitExpression;
    }

    public static LangLambdaExpression createLambdaExpression(ParserRuleContext ctx, LangASTNode body, List<LangASTNode> parameters) {
        return new LangLambdaExpression(PositionUtils.getPositionInfo(ctx), parameters, body);
    }

    public static LangTernaryExpression createTernaryExpression(ParserRuleContext ctx, LangASTNode condition, LangASTNode thenExpression, LangASTNode elseExpression) {
        return new LangTernaryExpression(PositionUtils.getPositionInfo(ctx), condition, thenExpression, elseExpression);
    }

    public static LangIndexAccess createIndexAccess(ParserRuleContext ctx, LangASTNode target, LangASTNode index) {
        return new LangIndexAccess(PositionUtils.getPositionInfo(ctx), target, index);
    }

    public static LangSliceExpression createSliceExpression(ParserRuleContext ctx, LangASTNode lower, LangASTNode upper, LangASTNode step) {
        return new LangSliceExpression(PositionUtils.getPositionInfo(ctx), lower, upper, step);
    }

    public static LangParenthesizedExpression createParenthesizedExpression(ParserRuleContext ctx, LangASTNode expression) {
        return new LangParenthesizedExpression(PositionUtils.getPositionInfo(ctx), expression);
    }

    /** Comprehension Expressions */
    public static LangComprehensionExpression createComprehensionExpression(ParserRuleContext ctx,
                                                                            LangComprehensionExpression.LangComprehensionKind kind,
                                                                            LangASTNode expression,
                                                                            LangASTNode keyExpression,
                                                                            LangASTNode valueExpression,
                                                                            List<LangComprehensionExpression.LangComprehensionClause> clauses) {
        return new LangComprehensionExpression(PositionUtils.getPositionInfo(ctx), kind, expression, keyExpression, valueExpression, clauses);
    }

    public static LangComprehensionExpression createListComprehension(ParserRuleContext ctx,
                                                                      LangASTNode expression,
                                                                      List<LangComprehensionExpression.LangComprehensionClause> clauses) {
        return new LangComprehensionExpression(PositionUtils.getPositionInfo(ctx),
                LangComprehensionExpression.LangComprehensionKind.LIST,
                expression, null, null, clauses);
    }

    public static LangComprehensionExpression createSetComprehension(ParserRuleContext ctx,
                                                                     LangASTNode expression,
                                                                     List<LangComprehensionExpression.LangComprehensionClause> clauses) {
        return new LangComprehensionExpression(PositionUtils.getPositionInfo(ctx),
                LangComprehensionExpression.LangComprehensionKind.SET,
                expression, null, null, clauses);
    }

    public static LangComprehensionExpression createDictComprehension(ParserRuleContext ctx,
                                                                      LangASTNode keyExpression,
                                                                      LangASTNode valueExpression,
                                                                      List<LangComprehensionExpression.LangComprehensionClause> clauses) {
        return new LangComprehensionExpression(PositionUtils.getPositionInfo(ctx),
                LangComprehensionExpression.LangComprehensionKind.DICT,
                null, keyExpression, valueExpression, clauses);
    }

    public static LangComprehensionExpression createGeneratorExpression(ParserRuleContext ctx,
                                                                        LangASTNode expression,
                                                                        List<LangComprehensionExpression.LangComprehensionClause> clauses) {
        return new LangComprehensionExpression(PositionUtils.getPositionInfo(ctx),
                LangComprehensionExpression.LangComprehensionKind.GENERATOR,
                expression, null, null, clauses);
    }

    public static LangComprehensionExpression.LangComprehensionClause createComprehensionClause(ParserRuleContext ctx,
                                                                                                boolean isAsync,
                                                                                                List<LangASTNode> targets,
                                                                                                LangExpression iterable,
                                                                                                List<LangASTNode> filters) {
        return new LangComprehensionExpression.LangComprehensionClause(PositionUtils.getPositionInfo(ctx), isAsync, targets, iterable, filters);
    }

    /** Try-Catch-Finally */
    public static LangASTNode createTryStatement(ParserRuleContext ctx, LangBlock tryBlock, List<LangCatchClause> catchClauses, LangBlock elseBlock, LangBlock finallyBlock) {
        return new LangTryStatement(PositionUtils.getPositionInfo(ctx), tryBlock, catchClauses, elseBlock, finallyBlock);
    }

    public static LangTryStatement createTryStatement(LangASTNode body, List<LangCatchClause> catchClauses,
                                                      LangASTNode finallyBlock, ParserRuleContext ctx) {
        LangTryStatement tryStatement = new LangTryStatement(PositionUtils.getPositionInfo(ctx));

        if (body != null) {
            tryStatement.setBody(body);
        }

        if (catchClauses != null) {
            for (LangCatchClause catchClause : catchClauses) {
                tryStatement.addCatchClause(catchClause);
            }
        }

        if (finallyBlock != null) {
            tryStatement.setFinally(finallyBlock);
        }

        return tryStatement;
    }

    public static LangCatchClause createCatchClause(ParserRuleContext ctx) {
        return new LangCatchClause(PositionUtils.getPositionInfo(ctx));
    }

    public static LangCatchClause createCatchClause(ParserRuleContext ctx,
                                                    List<LangASTNode> exceptionTypes,
                                                    LangSimpleName exceptionVariable,
                                                    LangASTNode body) {
        LangCatchClause catchClause = new LangCatchClause(PositionUtils.getPositionInfo(ctx));

        if (exceptionTypes != null) {
            for (LangASTNode exceptionType : exceptionTypes) {
                catchClause.addExceptionType(exceptionType);
            }
        }

        if (exceptionVariable != null) {
            catchClause.setExceptionVariable(exceptionVariable);
        }

        if (body != null) {
            catchClause.setBody(body);
        }

        return catchClause;
    }

    public static LangBreakStatement createBreakStatement(ParserRuleContext ctx) {
        return new LangBreakStatement(PositionUtils.getPositionInfo(ctx));
    }

    public static LangContinueStatement createContinueStatement(ParserRuleContext ctx) {
        return new LangContinueStatement(PositionUtils.getPositionInfo(ctx));
    }

    public static LangGlobalStatement createGlobalStatement(ParserRuleContext ctx, List<String> globalNames) {
        List<LangSimpleName> globalNameList = new ArrayList<>();
        for (String globalName : globalNames) {
            LangSimpleName name = createSimpleName(globalName, ctx);
            globalNameList.add(name);
        }
        return new LangGlobalStatement(PositionUtils.getPositionInfo(ctx), globalNameList);
    }

    public static LangPassStatement createPassStatement(ParserRuleContext ctx) {
        return new LangPassStatement(PositionUtils.getPositionInfo(ctx));
    }

    public static LangDelStatement createDelStatement(ParserRuleContext ctx, List<LangASTNode> targets) {
        return new LangDelStatement(PositionUtils.getPositionInfo(ctx), targets);
    }

    public static LangYieldStatement createYieldStatement(ParserRuleContext ctx, LangASTNode expression) {
        return new LangYieldStatement(PositionUtils.getPositionInfo(ctx), expression);
    }

    public static LangAssertStatement createAssertStatement(ParserRuleContext ctx, LangASTNode expression, LangASTNode message) {
        return new LangAssertStatement(PositionUtils.getPositionInfo(ctx), expression, message);
    }

    public static LangThrowStatement createThrowStatement(ParserRuleContext ctx, LangASTNode exception, LangASTNode fromExpr) {
        return new LangThrowStatement(PositionUtils.getPositionInfo(ctx), exception, fromExpr);
    }

    public static LangASTNode createWithStatement(Python3Parser.With_stmtContext ctx, List<LangASTNode> contextItems, LangBlock body) {
        return new LangWithStatement(PositionUtils.getPositionInfo(ctx), contextItems, body);
    }

    public static LangWithContextItem createWithContextItem(Python3Parser.With_itemContext ctx, LangASTNode expr) {
        return new LangWithContextItem(PositionUtils.getPositionInfo(ctx), expr);
    }

    public static LangWithContextItem createWithContextItem(Python3Parser.With_itemContext ctx, LangASTNode expr, LangASTNode alias) {
        return new LangWithContextItem(PositionUtils.getPositionInfo(ctx), expr, alias);
    }

    public static LangNonLocalStatement createNonlocalStatement(ParserRuleContext ctx, List<String> nonlocalNames) {
        List<LangSimpleName> nonlocalNameList = new ArrayList<>();
        for (String nonlocalName : nonlocalNames) {
            nonlocalNameList.add(createSimpleName(nonlocalName, ctx));
        }
        return new LangNonLocalStatement(PositionUtils.getPositionInfo(ctx), nonlocalNameList);
    }

    public static LangAsyncStatement createAsyncStatement(ParserRuleContext ctx, LangASTNode body) {
        return new LangAsyncStatement(PositionUtils.getPositionInfo(ctx), body);
    }

    public static LangSwitchStatement createSwitchStatement(ParserRuleContext ctx, LangASTNode expression, List<LangCaseStatement> body) {
        return new LangSwitchStatement(PositionUtils.getPositionInfo(ctx), expression, body);
    }

    public static LangCaseStatement createCaseStatement(ParserRuleContext ctx, LangASTNode expression, LangBlock body) {
        return new LangCaseStatement(PositionUtils.getPositionInfo(ctx), expression, body);
    }

    /** Literals */
    public static LangNumberLiteral createNumberLiteral(ParserRuleContext ctx, String value) {
        return new LangNumberLiteral(PositionUtils.getPositionInfo(ctx), value);
    }

    public static LangStringLiteral createStringLiteral(ParserRuleContext ctx, String value) {
        return new LangStringLiteral(PositionUtils.getPositionInfo(ctx), PyASTBuilderUtil.removeQuotes(value));
    }

    public static LangBooleanLiteral createBooleanLiteral(ParserRuleContext ctx, boolean value) {
        return new LangBooleanLiteral(PositionUtils.getPositionInfo(ctx), value);
    }

    public static LangListLiteral createListLiteral(ParserRuleContext ctx, List<LangASTNode> elements) {
        return new LangListLiteral(PositionUtils.getPositionInfo(ctx), elements);
    }

    public static LangTupleLiteral createTupleLiteral(ParserRuleContext ctx, List<LangASTNode> elements) {
        return new LangTupleLiteral(PositionUtils.getPositionInfo(ctx), elements);
    }

    public static LangDictionaryLiteral createDictionaryLiteral(ParserRuleContext ctx) {
        return new LangDictionaryLiteral(PositionUtils.getPositionInfo(ctx));
    }

    public static LangNullLiteral createNullLiteral(ParserRuleContext ctx) {
        return new LangNullLiteral(PositionUtils.getPositionInfo(ctx));
    }

    public static LangEllipsisLiteral createEllipsisLiteral(ParserRuleContext ctx) {
        return new LangEllipsisLiteral(PositionUtils.getPositionInfo(ctx));
    }

    /** Metadata */
    public static LangAnnotation createAnnotation(ParserRuleContext ctx, LangSimpleName name) {
        return new LangAnnotation(name, PositionUtils.getPositionInfo(ctx));
    }

    public static LangAnnotation createAnnotation(ParserRuleContext ctx, LangSimpleName name, List<LangASTNode> arguments) {
        LangAnnotation annotation = new LangAnnotation(name, PositionUtils.getPositionInfo(ctx));
        if (arguments != null) {
            annotation.setArguments(arguments);
        }
        return annotation;
    }

    public static LangAnnotation createAnnotation(ParserRuleContext ctx, LangSimpleName name, Map<String, LangASTNode> memberValuePairs, List<LangASTNode> arguments) {
        LangAnnotation annotation = new LangAnnotation(name, PositionUtils.getPositionInfo(ctx));
        if (arguments != null) {
            annotation.setArguments(arguments);
        }
        if (memberValuePairs != null) {
            for(Map.Entry<String, LangASTNode> entry : memberValuePairs.entrySet()) {
                annotation.addMemberValuePair(entry.getKey(), entry.getValue());
            }
        }
        return annotation;
    }

    public static LangComment createComment(ParserRuleContext ctx, String commentContent, boolean isBlockComment, boolean isDocComment) {
        return new LangComment(commentContent, isBlockComment, isDocComment, PositionUtils.getPositionInfo(ctx));
    }

    /** PATTERN */
    public static LangLiteralPattern createLiteralPattern(ParserRuleContext ctx, Object value) {
        return new LangLiteralPattern(PositionUtils.getPositionInfo(ctx), value);
    }

    public static LangVariablePattern createVariablePattern(ParserRuleContext ctx, String name) {
        return new LangVariablePattern(PositionUtils.getPositionInfo(ctx), name);
    }


}