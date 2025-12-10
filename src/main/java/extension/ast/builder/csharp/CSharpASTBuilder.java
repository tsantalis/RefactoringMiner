package extension.ast.builder.csharp;

import extension.ast.builder.LangASTBuilder;
import extension.ast.builder.csharp.component.CSharpCompilationUnitASTBuilder;
import extension.ast.builder.csharp.component.CSharpDeclarationASTBuilder;
import extension.ast.builder.csharp.component.CSharpExpressionASTBuilder;
import extension.ast.builder.csharp.component.CSharpStatementASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.csharp.CSharpParserBaseVisitor;

/**
 * Builder class to traverse the ANTLR parse tree for C# and build the custom AST.
 */
public class CSharpASTBuilder extends CSharpParserBaseVisitor<LangASTNode> implements LangASTBuilder<CSharpParser.Compilation_unitContext> {

    private final CSharpCompilationUnitASTBuilder compilationUnitBuilder;
    private final CSharpDeclarationASTBuilder declarationBuilder;
    private final CSharpExpressionASTBuilder expressionBuilder;
    private final CSharpStatementASTBuilder statementBuilder;

    public CSharpASTBuilder() {
        this.compilationUnitBuilder = new CSharpCompilationUnitASTBuilder(this);
        this.declarationBuilder = new CSharpDeclarationASTBuilder(this);
        this.expressionBuilder = new CSharpExpressionASTBuilder(this);
        this.statementBuilder = new CSharpStatementASTBuilder(this);
    }

    public LangASTNode build(CSharpParser.Compilation_unitContext ctx) { return visitCompilation_unit(ctx); }

    /** CompilationUnit related methods **/
    @Override
    public LangASTNode visitCompilation_unit(CSharpParser.Compilation_unitContext ctx) {
        return compilationUnitBuilder.visitCompilation_unit(ctx);
    }

    /** Declaration related methods **/
    @Override public LangASTNode visitClass_definition(CSharpParser.Class_definitionContext ctx) { return declarationBuilder.visitClass_definition(ctx); }
    @Override public LangASTNode visitMethod_declaration(CSharpParser.Method_declarationContext ctx) { return declarationBuilder.visitMethod_declaration(ctx); }
    @Override public LangASTNode visitClass_member_declaration(CSharpParser.Class_member_declarationContext ctx) { return declarationBuilder.visitClass_member_declaration(ctx); }
    @Override public LangASTNode visitCommon_member_declaration(CSharpParser.Common_member_declarationContext ctx) { return declarationBuilder.visitCommon_member_declaration(ctx); }

    /** Statement-related methods **/
    @Override public LangASTNode visitBlock(CSharpParser.BlockContext ctx) { return statementBuilder.visitBlock(ctx); }
    @Override public LangASTNode visitReturnStatement(CSharpParser.ReturnStatementContext ctx) { return statementBuilder.visitReturnStatement(ctx); }
    @Override public LangASTNode visitExpressionStatement(CSharpParser.ExpressionStatementContext ctx) { return statementBuilder.visitExpressionStatement(ctx); }
    @Override public LangASTNode visitIfStatement(CSharpParser.IfStatementContext ctx) { return statementBuilder.visitIfStatement(ctx); }
    @Override public LangASTNode visitWhileStatement(CSharpParser.WhileStatementContext ctx) { return statementBuilder.visitWhileStatement(ctx); }
    @Override public LangASTNode visitForStatement(CSharpParser.ForStatementContext ctx) { return statementBuilder.visitForStatement(ctx); }

    /** Expression-related methods **/
    @Override public LangASTNode visitSimpleNameExpression(CSharpParser.SimpleNameExpressionContext ctx) { return expressionBuilder.visitSimpleNameExpression(ctx); }
    @Override public LangASTNode visitLiteralExpression(CSharpParser.LiteralExpressionContext ctx) { return expressionBuilder.visitLiteralExpression(ctx); }
    @Override public LangASTNode visitAssignment(CSharpParser.AssignmentContext ctx) { return expressionBuilder.visitAssignment(ctx); }
    @Override public LangASTNode visitAdditive_expression(CSharpParser.Additive_expressionContext ctx) { return expressionBuilder.visitAdditive_expression(ctx); }
    @Override public LangASTNode visitMultiplicative_expression(CSharpParser.Multiplicative_expressionContext ctx) { return expressionBuilder.visitMultiplicative_expression(ctx); }
    @Override public LangASTNode visitMember_access(CSharpParser.Member_accessContext ctx) { return expressionBuilder.visitMember_access(ctx); }
    @Override public LangASTNode visitMethod_invocation(CSharpParser.Method_invocationContext ctx) { return expressionBuilder.visitMethod_invocation(ctx); }
}
