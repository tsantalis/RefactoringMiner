package extension.ast.builder.python;

import extension.ast.builder.LangASTBuilder;
import extension.ast.builder.python.component.PyCompilationUnitASTBuilder;
import extension.ast.builder.python.component.PyDeclarationASTBuilder;
import extension.ast.builder.python.component.PyExpressionASTBuilder;
import extension.ast.builder.python.component.PyStatementASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.python.Python3Parser;
import extension.base.lang.python.Python3ParserBaseVisitor;

/**
 * Βuilder class to traverse the ANTLR parse tree
 * and build the custom AST.
 */
public class PyASTBuilder extends Python3ParserBaseVisitor<LangASTNode> implements LangASTBuilder<Python3Parser.File_inputContext> {

    private final PyCompilationUnitASTBuilder compilationUnitBuilder;
    private final PyDeclarationASTBuilder declarationBuilder;
    private final PyExpressionASTBuilder expressionBuilder;
    private final PyStatementASTBuilder statementBuilder;

    public PyASTBuilder() {
        this.compilationUnitBuilder = new PyCompilationUnitASTBuilder(this);
        this.declarationBuilder = new PyDeclarationASTBuilder(this);
        this.expressionBuilder = new PyExpressionASTBuilder(this);
        this.statementBuilder = new PyStatementASTBuilder(this);
    }

    public LangASTNode build(Python3Parser.File_inputContext ctx) { return visitFile_input(ctx); }

    /** CompilationUnit related methods **/
    @Override public LangASTNode visitFile_input(Python3Parser.File_inputContext ctx) { return compilationUnitBuilder.visitFile_input(ctx); }
    @Override public LangASTNode visitImport_stmt(Python3Parser.Import_stmtContext ctx) { return compilationUnitBuilder.visitImport_stmt(ctx); }

    /** Declaration related methods **/
    @Override public LangASTNode visitClassdef(Python3Parser.ClassdefContext ctx) { return declarationBuilder.visitClassdef(ctx); }
    @Override public LangASTNode visitFuncdef(Python3Parser.FuncdefContext ctx) { return declarationBuilder.visitFuncdef(ctx); }
    @Override public LangASTNode visitDecorated(Python3Parser.DecoratedContext ctx) { return declarationBuilder.visitDecorated(ctx); }
    @Override public LangASTNode visitAsync_funcdef(Python3Parser.Async_funcdefContext ctx) { return declarationBuilder.visitAsync_funcdef(ctx); }

    /** Expression-related methods **/
    @Override public LangASTNode visitAtom(Python3Parser.AtomContext ctx) { return expressionBuilder.visitAtom(ctx); }
    @Override public LangASTNode visitAtom_expr(Python3Parser.Atom_exprContext ctx) { return expressionBuilder.visitAtom_expr(ctx); }
    @Override public LangASTNode visitExpr_stmt(Python3Parser.Expr_stmtContext ctx) { return expressionBuilder.visitExpr_stmt(ctx); }
    @Override public LangASTNode visitExpr(Python3Parser.ExprContext ctx) { return expressionBuilder.visitExpr(ctx); }
    @Override public LangASTNode visitComparison(Python3Parser.ComparisonContext ctx){ return expressionBuilder.visitComparison(ctx); }
    @Override public LangASTNode visitPattern(Python3Parser.PatternContext ctx){ return expressionBuilder.visitPattern(ctx); }
    @Override public LangASTNode visitStar_expr(Python3Parser.Star_exprContext ctx) { return expressionBuilder.visitStar_expr(ctx); }
    @Override public LangASTNode visitTestlist_star_expr(Python3Parser.Testlist_star_exprContext ctx) { return expressionBuilder.visitTestlist_star_expr(ctx); }
    @Override public LangASTNode visitTrailer(Python3Parser.TrailerContext ctx) { return expressionBuilder.visitTrailer(ctx); }
    @Override public LangASTNode visitSubscriptlist(Python3Parser.SubscriptlistContext ctx) { return expressionBuilder.visitSubscriptlist(ctx); }
    @Override public LangASTNode visitSubscript_(Python3Parser.Subscript_Context ctx) { return expressionBuilder.visitSubscript_(ctx); }
    @Override public LangASTNode visitTestlist_comp(Python3Parser.Testlist_compContext ctx) {return expressionBuilder.visitTestlist_comp(ctx);}
    @Override public LangASTNode visitDictorsetmaker(Python3Parser.DictorsetmakerContext ctx) {return expressionBuilder.visitDictorsetmaker(ctx);}

    /** Statement-related methods **/
    @Override public LangASTNode visitBlock(Python3Parser.BlockContext ctx) { return statementBuilder.visitBlock(ctx); }
    @Override public LangASTNode visitStmt(Python3Parser.StmtContext ctx) { return statementBuilder.visitStmt(ctx); }
    @Override public LangASTNode visitSimple_stmt(Python3Parser.Simple_stmtContext ctx) { return statementBuilder.visitSimple_stmt(ctx); }
    @Override public LangASTNode visitIf_stmt(Python3Parser.If_stmtContext ctx) { return statementBuilder.visitIf_stmt(ctx); }
    @Override public LangASTNode visitTest(Python3Parser.TestContext ctx) { return statementBuilder.visitTest(ctx); }
    @Override public LangASTNode visitOr_test(Python3Parser.Or_testContext ctx) { return statementBuilder.visitOr_test(ctx); }
    @Override public LangASTNode visitAnd_test(Python3Parser.And_testContext ctx) { return statementBuilder.visitAnd_test(ctx); }
    @Override public LangASTNode visitNot_test(Python3Parser.Not_testContext ctx) { return statementBuilder.visitNot_test(ctx); }
    @Override public LangASTNode visitReturn_stmt(Python3Parser.Return_stmtContext ctx) { return statementBuilder.visitReturn_stmt(ctx); }
    @Override public LangASTNode visitFor_stmt(Python3Parser.For_stmtContext ctx) { return statementBuilder.visitFor_stmt(ctx); }
    @Override public LangASTNode visitWhile_stmt(Python3Parser.While_stmtContext ctx) { return statementBuilder.visitWhile_stmt(ctx); }
    @Override public LangASTNode visitBreak_stmt(Python3Parser.Break_stmtContext ctx) { return statementBuilder.visitBreak_stmt(ctx); }
    @Override public LangASTNode visitContinue_stmt(Python3Parser.Continue_stmtContext ctx) { return statementBuilder.visitContinue_stmt(ctx); }
    @Override public LangASTNode visitTry_stmt(Python3Parser.Try_stmtContext ctx) { return statementBuilder.visitTry_stmt(ctx); }
    @Override public LangASTNode visitExcept_clause(Python3Parser.Except_clauseContext ctx){ return statementBuilder.visitExcept_clause(ctx); }
    @Override public LangASTNode visitRaise_stmt(Python3Parser.Raise_stmtContext ctx) { return statementBuilder.visitRaise_stmt(ctx); }
    @Override public LangASTNode visitWith_stmt(Python3Parser.With_stmtContext ctx) { return statementBuilder.visitWith_stmt(ctx); }
    @Override public LangASTNode visitWith_item(Python3Parser.With_itemContext ctx) { return statementBuilder.visitWith_item(ctx); }
    @Override public LangASTNode visitAssert_stmt(Python3Parser.Assert_stmtContext ctx) { return statementBuilder.visitAssert_stmt(ctx); }
    @Override public LangASTNode visitNonlocal_stmt(Python3Parser.Nonlocal_stmtContext ctx) { return statementBuilder.visitNonlocal_stmt(ctx); }
    @Override public LangASTNode visitGlobal_stmt(Python3Parser.Global_stmtContext ctx) { return statementBuilder.visitGlobal_stmt(ctx); }
    @Override public LangASTNode visitPass_stmt(Python3Parser.Pass_stmtContext ctx) { return statementBuilder.visitPass_stmt(ctx); }
    @Override public LangASTNode visitDel_stmt(Python3Parser.Del_stmtContext ctx) { return statementBuilder.visitDel_stmt(ctx); }
    @Override public LangASTNode visitYield_stmt(Python3Parser.Yield_stmtContext ctx) { return statementBuilder.visitYield_stmt(ctx); }
    @Override public LangASTNode visitAsync_stmt(Python3Parser.Async_stmtContext ctx) { return statementBuilder.visitAsync_stmt(ctx); }
    @Override public LangASTNode visitMatch_stmt(Python3Parser.Match_stmtContext ctx) { return statementBuilder.visitMatch_stmt(ctx); }
    @Override public LangASTNode visitLambdef(Python3Parser.LambdefContext ctx) { return statementBuilder.visitLambdadef(ctx); }
    @Override public LangASTNode visitVfpdef(Python3Parser.VfpdefContext ctx) { return statementBuilder.visitVfpdef(ctx); }
}
