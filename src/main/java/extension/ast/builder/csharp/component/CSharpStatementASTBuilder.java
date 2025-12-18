package extension.ast.builder.csharp.component;

import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.statement.LangBlock;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.csharp.CSharpParserBaseVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic C# statement-related AST building.
 */
public class CSharpStatementASTBuilder extends CSharpParserBaseVisitor<LangASTNode> {

    protected final CSharpASTBuilder mainBuilder;

    public CSharpStatementASTBuilder(CSharpASTBuilder mainBuilder) {
        this.mainBuilder = mainBuilder;
    }

    @Override
    public LangASTNode visitBlock(CSharpParser.BlockContext ctx) {
        List<LangASTNode> statements = new ArrayList<>();
        CSharpParser.Statement_listContext listCtx = ctx.statement_list();
        if (listCtx != null) {
            for (CSharpParser.StatementContext st : listCtx.statement()) {
                LangASTNode s = mainBuilder.visit(st);
                if (s != null) statements.add(s);
            }
        }
        return LangASTNodeFactory.createBlock(ctx, statements);
    }

    @Override
    public LangASTNode visitReturnStatement(CSharpParser.ReturnStatementContext ctx) {
        LangASTNode expr = ctx.expression() != null ? mainBuilder.visit(ctx.expression()) : null;
        return LangASTNodeFactory.createReturnStatement(expr, ctx);
    }

    @Override
    public LangASTNode visitExpressionStatement(CSharpParser.ExpressionStatementContext ctx) {
        LangASTNode expr = mainBuilder.visit(ctx.expression());
        if (expr == null) return null;
        return LangASTNodeFactory.createExpressionStatement(expr, ctx);
    }

    @Override
    public LangASTNode visitIfStatement(CSharpParser.IfStatementContext ctx) {
        LangASTNode cond = mainBuilder.visit(ctx.expression());
        LangASTNode thenNode = ctx.if_body().isEmpty() ? null : mainBuilder.visit(ctx.if_body(0));
        LangBlock thenBlock = (thenNode instanceof LangBlock) ? (LangBlock) thenNode : LangASTNodeFactory.createBlock(ctx.if_body(0), thenNode == null ? null : List.of(thenNode));
        LangASTNode elseNode = null;
        if (ctx.if_body().size() > 1) {
            elseNode = mainBuilder.visit(ctx.if_body(1));
        }
        LangBlock elseBlock = (elseNode instanceof LangBlock) ? (LangBlock) elseNode : (elseNode != null ? LangASTNodeFactory.createBlock(ctx.if_body(1), List.of(elseNode)) : null);
        return LangASTNodeFactory.createIfStatement(cond, thenBlock, elseBlock, ctx);
    }

    @Override
    public LangASTNode visitWhileStatement(CSharpParser.WhileStatementContext ctx) {
        LangASTNode cond = mainBuilder.visit(ctx.expression());
        LangASTNode bodyNode = mainBuilder.visit(ctx.embedded_statement());
        LangBlock bodyBlock = (bodyNode instanceof LangBlock) ? (LangBlock) bodyNode : LangASTNodeFactory.createBlock(ctx.embedded_statement(), bodyNode == null ? null : List.of(bodyNode));
        return LangASTNodeFactory.createWhileStatement(cond, bodyBlock, null, ctx);
    }

    @Override
    public LangASTNode visitForStatement(CSharpParser.ForStatementContext ctx) {
        LangASTNode condition = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            org.antlr.v4.runtime.tree.ParseTree ch = ctx.getChild(i);
            if (ch instanceof CSharpParser.ExpressionContext) {
                condition = mainBuilder.visit(ch);
                break;
            }
        }
        LangASTNode bodyNode = mainBuilder.visit(ctx.embedded_statement());
        LangBlock bodyBlock = (bodyNode instanceof LangBlock) ? (LangBlock) bodyNode : LangASTNodeFactory.createBlock(ctx.embedded_statement(), bodyNode == null ? null : List.of(bodyNode));
        return LangASTNodeFactory.createForStatement(new ArrayList<>(), condition, new ArrayList<>(), bodyBlock, null, ctx);
    }
}
