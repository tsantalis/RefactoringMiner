package extension.ast.builder.python;

import extension.ast.builder.LangASTBuilder;
import extension.ast.builder.python.component.PyCompilationUnitASTBuilder;
import extension.ast.builder.python.component.PyDeclarationASTBuilder;
import extension.ast.builder.python.component.PyExpressionASTBuilder;
import extension.ast.builder.python.component.PyStatementASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.python.PythonParser;
import extension.base.lang.python.PythonParserBaseVisitor;

public class PyASTBuilder extends PythonParserBaseVisitor<LangASTNode> implements LangASTBuilder<PythonParser.File_inputContext> {

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

    @Override public LangASTNode build(PythonParser.File_inputContext ctx) {return visitFile_input(ctx);}

    // CompilationUnit related methods
    @Override public LangASTNode visitFile_input(PythonParser.File_inputContext ctx) {return compilationUnitBuilder.visitFile_input(ctx);}
    @Override public LangASTNode visitImport_stmt(PythonParser.Import_stmtContext ctx) {return compilationUnitBuilder.visitImport_stmt(ctx);}

    // Declaration related methods
    @Override public LangASTNode visitClass_def(PythonParser.Class_defContext ctx) {return declarationBuilder.visitClass_def(ctx);}
    @Override public LangASTNode visitFunction_def(PythonParser.Function_defContext ctx) {return declarationBuilder.visitFunction_def(ctx);}
    @Override public LangASTNode visitDecorators(PythonParser.DecoratorsContext ctx) {return declarationBuilder.visitDecorators(ctx);}

    // Statement-related methods
    @Override public LangASTNode visitBlock(PythonParser.BlockContext ctx) {return statementBuilder.visitBlock(ctx);}
    @Override public LangASTNode visitStatement(PythonParser.StatementContext ctx) {return statementBuilder.visitStatement(ctx);}
    @Override public LangASTNode visitSimple_stmts(PythonParser.Simple_stmtsContext ctx) {return statementBuilder.visitSimple_stmts(ctx);}
    @Override public LangASTNode visitExcept_block(PythonParser.Except_blockContext ctx) {return statementBuilder.visitExcept_block(ctx);}
    @Override public LangASTNode visitExcept_star_block(PythonParser.Except_star_blockContext ctx) {return statementBuilder.visitExcept_star_block(ctx);}
    @Override public LangASTNode visitFinally_block(PythonParser.Finally_blockContext ctx) {return statementBuilder.visitFinally_block(ctx);}
    @Override public LangASTNode visitCase_block(PythonParser.Case_blockContext ctx) {return statementBuilder.visitCase_block(ctx);}
    @Override public LangASTNode visitWith_item(PythonParser.With_itemContext ctx) {return statementBuilder.visitWith_item(ctx);}
    @Override public LangASTNode visitType_alias(PythonParser.Type_aliasContext ctx) {return statementBuilder.visitType_alias(ctx);}
    @Override public LangASTNode visitSimple_stmt(PythonParser.Simple_stmtContext ctx) {return statementBuilder.visitSimple_stmt(ctx);}
    @Override public LangASTNode visitType_params(PythonParser.Type_paramsContext ctx) {return declarationBuilder.visitType_params(ctx);}
    @Override public LangASTNode visitType_param(PythonParser.Type_paramContext ctx) {return declarationBuilder.visitType_param(ctx);}
    @Override public LangASTNode visitAs_pattern(PythonParser.As_patternContext ctx) {return statementBuilder.visitAs_pattern(ctx);}
    @Override public LangASTNode visitOr_pattern(PythonParser.Or_patternContext ctx) {return statementBuilder.visitOr_pattern(ctx);}
    @Override public LangASTNode visitClosed_pattern(PythonParser.Closed_patternContext ctx) {return statementBuilder.visitClosed_pattern(ctx);}
    @Override public LangASTNode visitSequence_pattern(PythonParser.Sequence_patternContext ctx) {return statementBuilder.visitSequence_pattern(ctx);}
    @Override public LangASTNode visitMaybe_star_pattern(PythonParser.Maybe_star_patternContext ctx) {return statementBuilder.visitMaybe_star_pattern(ctx);}
    @Override public LangASTNode visitStar_pattern(PythonParser.Star_patternContext ctx) {return statementBuilder.visitStar_pattern(ctx);}
    @Override public LangASTNode visitMapping_pattern(PythonParser.Mapping_patternContext ctx) {return statementBuilder.visitMapping_pattern(ctx);}
    @Override public LangASTNode visitClass_pattern(PythonParser.Class_patternContext ctx) {return statementBuilder.visitClass_pattern(ctx);}
    @Override public LangASTNode visitLiteral_pattern(PythonParser.Literal_patternContext ctx) {return statementBuilder.visitLiteral_pattern(ctx);}
    @Override public LangASTNode visitCapture_pattern(PythonParser.Capture_patternContext ctx) {return statementBuilder.visitCapture_pattern(ctx);}
    @Override public LangASTNode visitWildcard_pattern(PythonParser.Wildcard_patternContext ctx) {return statementBuilder.visitWildcard_pattern(ctx);}
    @Override public LangASTNode visitPatterns(PythonParser.PatternsContext ctx) {return statementBuilder.visitPatterns(ctx);}
    @Override public LangASTNode visitPattern(PythonParser.PatternContext ctx) {return statementBuilder.visitPattern(ctx);}
    @Override public LangASTNode visitWith_stmt(PythonParser.With_stmtContext ctx) {return statementBuilder.visitWith_stmt(ctx);}
    @Override public LangASTNode visitMatch_stmt(PythonParser.Match_stmtContext ctx) {return statementBuilder.visitMatch_stmt(ctx);}
    @Override public LangASTNode visitRaise_stmt(PythonParser.Raise_stmtContext ctx) {return statementBuilder.visitRaise_stmt(ctx);}
    @Override public LangASTNode visitAssert_stmt(PythonParser.Assert_stmtContext ctx) {return statementBuilder.visitAssert_stmt(ctx);}
    @Override public LangASTNode visitDel_stmt(PythonParser.Del_stmtContext ctx) {return statementBuilder.visitDel_stmt(ctx);}
    @Override public LangASTNode visitPass_stmt(PythonParser.Pass_stmtContext ctx) {return statementBuilder.visitPass_stmt(ctx);}
    @Override public LangASTNode visitYield_stmt(PythonParser.Yield_stmtContext ctx) {return statementBuilder.visitYield_stmt(ctx);}
    @Override public LangASTNode visitGlobal_stmt(PythonParser.Global_stmtContext ctx) {return statementBuilder.visitGlobal_stmt(ctx);}
    @Override public LangASTNode visitNonlocal_stmt(PythonParser.Nonlocal_stmtContext ctx) {return statementBuilder.visitNonlocal_stmt(ctx);}
    @Override public LangASTNode visitIf_stmt(PythonParser.If_stmtContext ctx) {return statementBuilder.visitIf_stmt(ctx);}
    @Override public LangASTNode visitElif_stmt(PythonParser.Elif_stmtContext ctx) {return statementBuilder.visitElif_stmt(ctx);}
    @Override public LangASTNode visitElse_block(PythonParser.Else_blockContext ctx) {return statementBuilder.visitElse_block(ctx);}
    @Override public LangASTNode visitReturn_stmt(PythonParser.Return_stmtContext ctx) {return statementBuilder.visitReturn_stmt(ctx);}
    @Override public LangASTNode visitFor_stmt(PythonParser.For_stmtContext ctx) {return statementBuilder.visitFor_stmt(ctx);}
    @Override public LangASTNode visitWhile_stmt(PythonParser.While_stmtContext ctx) {return statementBuilder.visitWhile_stmt(ctx);}
    @Override public LangASTNode visitTry_stmt(PythonParser.Try_stmtContext ctx) {return statementBuilder.visitTry_stmt(ctx);}
    @Override public LangASTNode visitCompound_stmt(PythonParser.Compound_stmtContext ctx) {return statementBuilder.visitCompound_stmt(ctx);}
    @Override public LangASTNode visitSubject_expr(PythonParser.Subject_exprContext ctx) {return statementBuilder.visitSubject_expr(ctx);}
    @Override public LangASTNode visitOpen_sequence_pattern(PythonParser.Open_sequence_patternContext ctx) {return statementBuilder.visitOpen_sequence_pattern(ctx);}
    @Override public LangASTNode visitPattern_capture_target(PythonParser.Pattern_capture_targetContext ctx) {return statementBuilder.visitPattern_capture_target(ctx);}
    @Override public LangASTNode visitValue_pattern(PythonParser.Value_patternContext ctx) {return statementBuilder.visitValue_pattern(ctx);}
    @Override public LangASTNode visitDel_target(PythonParser.Del_targetContext ctx) {return statementBuilder.visitDel_target(ctx);}
    @Override public LangASTNode visitDel_t_atom(PythonParser.Del_t_atomContext ctx) {return statementBuilder.visitDel_t_atom(ctx);}

    // Expression-related methods
    @Override public LangASTNode visitPrimary(PythonParser.PrimaryContext ctx) {return expressionBuilder.visitPrimary(ctx);}
    @Override public LangASTNode visitAtom(PythonParser.AtomContext ctx) {return expressionBuilder.visitAtom(ctx);}
    @Override public LangASTNode visitAssignment(PythonParser.AssignmentContext ctx) {return expressionBuilder.visitAssignment(ctx);}
    @Override public LangASTNode visitExpression(PythonParser.ExpressionContext ctx) {return expressionBuilder.visitExpression(ctx);}
    @Override public LangASTNode visitDisjunction(PythonParser.DisjunctionContext ctx) {return expressionBuilder.visitDisjunction(ctx);}
    @Override public LangASTNode visitConjunction(PythonParser.ConjunctionContext ctx) {return expressionBuilder.visitConjunction(ctx);}
    @Override public LangASTNode visitInversion(PythonParser.InversionContext ctx) {return expressionBuilder.visitInversion(ctx);}
    @Override public LangASTNode visitComparison(PythonParser.ComparisonContext ctx) {return expressionBuilder.visitComparison(ctx);}
    @Override public LangASTNode visitAwait_primary(PythonParser.Await_primaryContext ctx) {return expressionBuilder.visitAwait_primary(ctx);}
    @Override public LangASTNode visitBitwise_or(PythonParser.Bitwise_orContext ctx) {return expressionBuilder.visitBitwise_or(ctx);}
    @Override public LangASTNode visitBitwise_xor(PythonParser.Bitwise_xorContext ctx) {return expressionBuilder.visitBitwise_xor(ctx);}
    @Override public LangASTNode visitBitwise_and(PythonParser.Bitwise_andContext ctx) {return expressionBuilder.visitBitwise_and(ctx);}
    @Override public LangASTNode visitShift_expr(PythonParser.Shift_exprContext ctx) {return expressionBuilder.visitShift_expr(ctx);}
    @Override public LangASTNode visitSum(PythonParser.SumContext ctx) {return expressionBuilder.visitSum(ctx);}
    @Override public LangASTNode visitTerm(PythonParser.TermContext ctx) {return expressionBuilder.visitTerm(ctx);}
    @Override public LangASTNode visitFactor(PythonParser.FactorContext ctx) {return expressionBuilder.visitFactor(ctx);}
    @Override public LangASTNode visitPower(PythonParser.PowerContext ctx) {return expressionBuilder.visitPower(ctx);}
    @Override public LangASTNode visitNamed_expression(PythonParser.Named_expressionContext ctx) {return expressionBuilder.visitNamed_expression(ctx);}
    @Override public LangASTNode visitAssignment_expression(PythonParser.Assignment_expressionContext ctx) {return expressionBuilder.visitAssignment_expression(ctx);}
    @Override public LangASTNode visitList(PythonParser.ListContext ctx) {return expressionBuilder.visitList(ctx);}
    @Override public LangASTNode visitTuple(PythonParser.TupleContext ctx) {return expressionBuilder.visitTuple(ctx);}
    @Override public LangASTNode visitSet(PythonParser.SetContext ctx) {return expressionBuilder.visitSet(ctx);}
    @Override public LangASTNode visitDict(PythonParser.DictContext ctx) {return expressionBuilder.visitDict(ctx);}
    @Override public LangASTNode visitListcomp(PythonParser.ListcompContext ctx) {return expressionBuilder.visitListcomp(ctx);}
    @Override public LangASTNode visitSetcomp(PythonParser.SetcompContext ctx) {return expressionBuilder.visitSetcomp(ctx);}
    @Override public LangASTNode visitGenexp(PythonParser.GenexpContext ctx) {return expressionBuilder.visitGenexp(ctx);}
    @Override public LangASTNode visitDictcomp(PythonParser.DictcompContext ctx) {return expressionBuilder.visitDictcomp(ctx);}
    @Override public LangASTNode visitLambdef(PythonParser.LambdefContext ctx) {return expressionBuilder.visitLambdef(ctx);}
    @Override public LangASTNode visitLambda_parameters(PythonParser.Lambda_parametersContext ctx) {return null;} // Handled within visitLambdef
    @Override public LangASTNode visitStrings(PythonParser.StringsContext ctx) {return expressionBuilder.visitStrings(ctx);}
    @Override public LangASTNode visitFstring(PythonParser.FstringContext ctx) {return expressionBuilder.visitFstring(ctx);}
    @Override public LangASTNode visitFstring_middle(PythonParser.Fstring_middleContext ctx) {return expressionBuilder.visitFstring_middle(ctx);}
    @Override public LangASTNode visitFstring_replacement_field(PythonParser.Fstring_replacement_fieldContext ctx) {return expressionBuilder.visitFstring_replacement_field(ctx);}
    @Override public LangASTNode visitFstring_full_format_spec(PythonParser.Fstring_full_format_specContext ctx) {return expressionBuilder.visitFstring_full_format_spec(ctx);}
    @Override public LangASTNode visitFstring_format_spec(PythonParser.Fstring_format_specContext ctx) {return expressionBuilder.visitFstring_format_spec(ctx);}
    @Override public LangASTNode visitTstring(PythonParser.TstringContext ctx) {return expressionBuilder.visitTstring(ctx);}
    @Override public LangASTNode visitTstring_middle(PythonParser.Tstring_middleContext ctx) {return expressionBuilder.visitTstring_middle(ctx);}
    @Override public LangASTNode visitTstring_replacement_field(PythonParser.Tstring_replacement_fieldContext ctx) {return expressionBuilder.visitTstring_replacement_field(ctx);}
    @Override public LangASTNode visitTstring_full_format_spec(PythonParser.Tstring_full_format_specContext ctx) {return expressionBuilder.visitTstring_full_format_spec(ctx);}
    @Override public LangASTNode visitTstring_format_spec(PythonParser.Tstring_format_specContext ctx) {return expressionBuilder.visitTstring_format_spec(ctx);}
    @Override public LangASTNode visitYield_expr(PythonParser.Yield_exprContext ctx) {return expressionBuilder.visitYield_expr(ctx);}
    @Override public LangASTNode visitStar_named_expression(PythonParser.Star_named_expressionContext ctx) {return expressionBuilder.visitStar_named_expression(ctx);}
    @Override public LangASTNode visitStar_expressions(PythonParser.Star_expressionsContext ctx) {return expressionBuilder.visitStar_expressions(ctx);}
    @Override public LangASTNode visitStar_expression(PythonParser.Star_expressionContext ctx) {return expressionBuilder.visitStar_expression(ctx);}
    @Override public LangASTNode visitStarred_expression(PythonParser.Starred_expressionContext ctx) {return expressionBuilder.visitStarred_expression(ctx);}
    @Override public LangASTNode visitKwarg_or_starred(PythonParser.Kwarg_or_starredContext ctx) {return expressionBuilder.visitKwarg_or_starred(ctx);}
    @Override public LangASTNode visitKwarg_or_double_starred(PythonParser.Kwarg_or_double_starredContext ctx) {return expressionBuilder.visitKwarg_or_double_starred(ctx);}
    @Override public LangASTNode visitStar_targets(PythonParser.Star_targetsContext ctx) {return expressionBuilder.visitStar_targets(ctx);}
    @Override public LangASTNode visitStar_target(PythonParser.Star_targetContext ctx) {return expressionBuilder.visitStar_target(ctx);}
    @Override public LangASTNode visitSingle_target(PythonParser.Single_targetContext ctx) {return expressionBuilder.visitSingle_target(ctx);}
    @Override public LangASTNode visitSingle_subscript_attribute_target(PythonParser.Single_subscript_attribute_targetContext ctx) {return expressionBuilder.visitSingle_subscript_attribute_target(ctx);}
    @Override public LangASTNode visitTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx) {return expressionBuilder.visitTarget_with_star_atom(ctx);}
    @Override public LangASTNode visitStar_atom(PythonParser.Star_atomContext ctx) {return expressionBuilder.visitStar_atom(ctx);}
    @Override public LangASTNode visitT_primary(PythonParser.T_primaryContext ctx) {return expressionBuilder.visitT_primary(ctx);}
    @Override public LangASTNode visitGroup(PythonParser.GroupContext ctx) {return expressionBuilder.visitGroup(ctx);}
    @Override public LangASTNode visitSlices(PythonParser.SlicesContext ctx) {return expressionBuilder.visitSlices(ctx);}
    @Override public LangASTNode visitSlice(PythonParser.SliceContext ctx) {return expressionBuilder.visitSlice(ctx);}
    @Override public LangASTNode visitString(PythonParser.StringContext ctx) {return expressionBuilder.visitString(ctx);}
    @Override public LangASTNode visitName(PythonParser.NameContext ctx) {return expressionBuilder.visitName(ctx);}
    @Override public LangASTNode visitAttr(PythonParser.AttrContext ctx) {return expressionBuilder.visitAttr(ctx);}
    @Override public LangASTNode visitName_or_attr(PythonParser.Name_or_attrContext ctx) {return expressionBuilder.visitName_or_attr(ctx);}
    @Override public LangASTNode visitLiteral_expr(PythonParser.Literal_exprContext ctx) {return expressionBuilder.visitLiteral_expr(ctx);}
}
