package extension.base.lang.python;// Generated from C:/Users/popos/Desktop/RefactoringMiner/src/main/antlr/Python3Parser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link Python3Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface Python3ParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link Python3Parser#single_input}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_input(Python3Parser.Single_inputContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#file_input}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_input(Python3Parser.File_inputContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#eval_input}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEval_input(Python3Parser.Eval_inputContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#decorator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecorator(Python3Parser.DecoratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#decorators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecorators(Python3Parser.DecoratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#decorated}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecorated(Python3Parser.DecoratedContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#async_funcdef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsync_funcdef(Python3Parser.Async_funcdefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#funcdef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncdef(Python3Parser.FuncdefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameters(Python3Parser.ParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#typedargslist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypedargslist(Python3Parser.TypedargslistContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#tfpdef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTfpdef(Python3Parser.TfpdefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#varargslist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarargslist(Python3Parser.VarargslistContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#vfpdef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVfpdef(Python3Parser.VfpdefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(Python3Parser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#simple_stmts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_stmts(Python3Parser.Simple_stmtsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#simple_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_stmt(Python3Parser.Simple_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#expr_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_stmt(Python3Parser.Expr_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#annassign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnassign(Python3Parser.AnnassignContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#testlist_star_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTestlist_star_expr(Python3Parser.Testlist_star_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#augassign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAugassign(Python3Parser.AugassignContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#del_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDel_stmt(Python3Parser.Del_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#pass_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPass_stmt(Python3Parser.Pass_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#flow_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlow_stmt(Python3Parser.Flow_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#break_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreak_stmt(Python3Parser.Break_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#continue_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinue_stmt(Python3Parser.Continue_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#return_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_stmt(Python3Parser.Return_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#yield_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYield_stmt(Python3Parser.Yield_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#raise_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaise_stmt(Python3Parser.Raise_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#import_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_stmt(Python3Parser.Import_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#import_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_name(Python3Parser.Import_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#import_from}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_from(Python3Parser.Import_fromContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#import_as_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_as_name(Python3Parser.Import_as_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#dotted_as_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotted_as_name(Python3Parser.Dotted_as_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#import_as_names}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_as_names(Python3Parser.Import_as_namesContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#dotted_as_names}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotted_as_names(Python3Parser.Dotted_as_namesContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#dotted_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotted_name(Python3Parser.Dotted_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#global_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_stmt(Python3Parser.Global_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#nonlocal_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonlocal_stmt(Python3Parser.Nonlocal_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#assert_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssert_stmt(Python3Parser.Assert_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#compound_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_stmt(Python3Parser.Compound_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#async_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsync_stmt(Python3Parser.Async_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#if_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_stmt(Python3Parser.If_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#while_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_stmt(Python3Parser.While_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#for_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_stmt(Python3Parser.For_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#try_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTry_stmt(Python3Parser.Try_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#with_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_stmt(Python3Parser.With_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#with_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_item(Python3Parser.With_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#except_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExcept_clause(Python3Parser.Except_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(Python3Parser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#match_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatch_stmt(Python3Parser.Match_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#subject_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubject_expr(Python3Parser.Subject_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#star_named_expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_named_expressions(Python3Parser.Star_named_expressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#star_named_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_named_expression(Python3Parser.Star_named_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#case_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_block(Python3Parser.Case_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#guard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGuard(Python3Parser.GuardContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#patterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatterns(Python3Parser.PatternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(Python3Parser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#as_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAs_pattern(Python3Parser.As_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#or_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOr_pattern(Python3Parser.Or_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#closed_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosed_pattern(Python3Parser.Closed_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#literal_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral_pattern(Python3Parser.Literal_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#literal_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral_expr(Python3Parser.Literal_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#complex_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplex_number(Python3Parser.Complex_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#signed_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned_number(Python3Parser.Signed_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#signed_real_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned_real_number(Python3Parser.Signed_real_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#real_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReal_number(Python3Parser.Real_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#imaginary_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImaginary_number(Python3Parser.Imaginary_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#capture_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCapture_pattern(Python3Parser.Capture_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#pattern_capture_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern_capture_target(Python3Parser.Pattern_capture_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#wildcard_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcard_pattern(Python3Parser.Wildcard_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#value_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue_pattern(Python3Parser.Value_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttr(Python3Parser.AttrContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#name_or_attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName_or_attr(Python3Parser.Name_or_attrContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#group_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_pattern(Python3Parser.Group_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#sequence_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_pattern(Python3Parser.Sequence_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#open_sequence_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_sequence_pattern(Python3Parser.Open_sequence_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#maybe_sequence_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaybe_sequence_pattern(Python3Parser.Maybe_sequence_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#maybe_star_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaybe_star_pattern(Python3Parser.Maybe_star_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#star_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_pattern(Python3Parser.Star_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#mapping_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapping_pattern(Python3Parser.Mapping_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#items_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitItems_pattern(Python3Parser.Items_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#key_value_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_value_pattern(Python3Parser.Key_value_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#double_star_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDouble_star_pattern(Python3Parser.Double_star_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#class_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_pattern(Python3Parser.Class_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#positional_patterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositional_patterns(Python3Parser.Positional_patternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#keyword_patterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword_patterns(Python3Parser.Keyword_patternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#keyword_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword_pattern(Python3Parser.Keyword_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#test}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTest(Python3Parser.TestContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#test_nocond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTest_nocond(Python3Parser.Test_nocondContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#lambdef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdef(Python3Parser.LambdefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#lambdef_nocond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdef_nocond(Python3Parser.Lambdef_nocondContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#or_test}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOr_test(Python3Parser.Or_testContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#and_test}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnd_test(Python3Parser.And_testContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#not_test}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNot_test(Python3Parser.Not_testContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#comparison}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(Python3Parser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#comp_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComp_op(Python3Parser.Comp_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#star_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_expr(Python3Parser.Star_exprContext ctx);
	/**
	 * Visit a parse tree produced.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(Python3Parser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#atom_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtom_expr(Python3Parser.Atom_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtom(Python3Parser.AtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(Python3Parser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#testlist_comp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTestlist_comp(Python3Parser.Testlist_compContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#trailer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrailer(Python3Parser.TrailerContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#subscriptlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscriptlist(Python3Parser.SubscriptlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#subscript_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript_(Python3Parser.Subscript_Context ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#sliceop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSliceop(Python3Parser.SliceopContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#exprlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprlist(Python3Parser.ExprlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#testlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTestlist(Python3Parser.TestlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#dictorsetmaker}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDictorsetmaker(Python3Parser.DictorsetmakerContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#classdef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassdef(Python3Parser.ClassdefContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#arglist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArglist(Python3Parser.ArglistContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(Python3Parser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#comp_iter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComp_iter(Python3Parser.Comp_iterContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#comp_for}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComp_for(Python3Parser.Comp_forContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#comp_if}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComp_if(Python3Parser.Comp_ifContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#encoding_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEncoding_decl(Python3Parser.Encoding_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#yield_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYield_expr(Python3Parser.Yield_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#yield_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYield_arg(Python3Parser.Yield_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link Python3Parser#strings}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStrings(Python3Parser.StringsContext ctx);
}