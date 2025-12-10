package extension.ast.builder.csharp.component;

import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.csharp.CSharpParserBaseVisitor;

/**
 * Basic C# expression-related AST building.
 */
public class CSharpExpressionASTBuilder extends CSharpParserBaseVisitor<LangASTNode> {

    protected final CSharpASTBuilder mainBuilder;

    public CSharpExpressionASTBuilder(CSharpASTBuilder mainBuilder) {
        this.mainBuilder = mainBuilder;
    }

    @Override
    public LangASTNode visitSimpleNameExpression(CSharpParser.SimpleNameExpressionContext ctx) {
        String name = ctx.identifier() != null ? ctx.identifier().getText() : ctx.getText();
        return LangASTNodeFactory.createSimpleName(name, ctx);
    }

    @Override
    public LangASTNode visitLiteralExpression(CSharpParser.LiteralExpressionContext ctx) {
        String text = ctx.literal().getText();
        if ("null".equals(text)) {
            return LangASTNodeFactory.createNullLiteral(ctx);
        }
        if ("true".equals(text) || "false".equals(text)) {
            return LangASTNodeFactory.createBooleanLiteral(ctx, text.equalsIgnoreCase("true"));
        }
        if (text.startsWith("\"") || text.startsWith("@\"")) {
            return LangASTNodeFactory.createStringLiteral(ctx, text);
        }
        return LangASTNodeFactory.createNumberLiteral(ctx, text);
    }

    @Override
    public LangASTNode visitAssignment(CSharpParser.AssignmentContext ctx) {
        LangASTNode left = mainBuilder.visit(ctx.unary_expression());
        String op;
        if (ctx.assignment_operator() != null) {
            op = ctx.assignment_operator().getText();
        } else {
            op = ctx.OP_COALESCING_ASSIGNMENT() != null ? ctx.OP_COALESCING_ASSIGNMENT().getText() : "=";
        }
        LangASTNode right = ctx.expression() != null ? mainBuilder.visit(ctx.expression()) : (ctx.throwable_expression() != null ? mainBuilder.visit(ctx.throwable_expression()) : null);
        return LangASTNodeFactory.createAssignment(op, left, right, ctx);
    }

    @Override
    public LangASTNode visitAdditive_expression(CSharpParser.Additive_expressionContext ctx) {
        // Left-associative folding of + and - operators
        if (ctx.multiplicative_expression() == null || ctx.multiplicative_expression().size() == 0) {
            return super.visitAdditive_expression(ctx);
        }
        LangASTNode current = mainBuilder.visit(ctx.multiplicative_expression(0));
        for (int i = 1; i < ctx.multiplicative_expression().size(); i++) {
            LangASTNode right = mainBuilder.visit(ctx.multiplicative_expression(i));
            String operatorSymbol = ctx.getChild(2 * i - 1).getText(); // PLUS or MINUS between operands
            current = LangASTNodeFactory.createInfixExpression(current, right, operatorSymbol, ctx);
        }
        return current;
    }

    @Override
    public LangASTNode visitMultiplicative_expression(CSharpParser.Multiplicative_expressionContext ctx) {
        if (ctx.switch_expression() == null || ctx.switch_expression().isEmpty()) {
            return super.visitMultiplicative_expression(ctx);
        }
        LangASTNode current = mainBuilder.visit(ctx.switch_expression(0));
        for (int i = 1; i < ctx.switch_expression().size(); i++) {
            LangASTNode right = mainBuilder.visit(ctx.switch_expression(i));
            String operatorSymbol = ctx.getChild(2 * i - 1).getText(); // *, /, %
            current = LangASTNodeFactory.createInfixExpression(current, right, operatorSymbol, ctx);
        }
        return current;
    }

    @Override
    public LangASTNode visitMember_access(CSharpParser.Member_accessContext ctx) {
        String name = ctx.identifier() != null ? ctx.identifier().getText() : ctx.getText();
        return LangASTNodeFactory.createSimpleName(name, ctx);
    }

    @Override
    public LangASTNode visitMethod_invocation(CSharpParser.Method_invocationContext ctx) {
        var call = LangASTNodeFactory.createMethodInvocation(ctx);
        CSharpParser.Argument_listContext al = ctx.argument_list();
        if (al != null) {
            for (CSharpParser.ArgumentContext a : al.argument()) {
                LangASTNode argExpr = a.expression() != null ? mainBuilder.visit(a.expression()) : null;
                if (argExpr != null) {
                    call.addChild(argExpr);
                }
            }
        }
        return call;
    }
}
