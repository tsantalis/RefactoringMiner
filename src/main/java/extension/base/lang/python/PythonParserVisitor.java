// Generated from PythonParser.g4 by ANTLR 4.13.2
package extension.base.lang.python;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PythonParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PythonParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PythonParser#file_input}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_input(PythonParser.File_inputContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#interactive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInteractive(PythonParser.InteractiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#eval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEval(PythonParser.EvalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#func_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_type(PythonParser.Func_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatements(PythonParser.StatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(PythonParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#single_compound_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_compound_stmt(PythonParser.Single_compound_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#statement_newline}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement_newline(PythonParser.Statement_newlineContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#simple_stmts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_stmts(PythonParser.Simple_stmtsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#simple_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_stmt(PythonParser.Simple_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#compound_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_stmt(PythonParser.Compound_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(PythonParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#annotated_rhs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotated_rhs(PythonParser.Annotated_rhsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#augassign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAugassign(PythonParser.AugassignContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#return_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_stmt(PythonParser.Return_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#raise_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaise_stmt(PythonParser.Raise_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#pass_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPass_stmt(PythonParser.Pass_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#break_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreak_stmt(PythonParser.Break_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#continue_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinue_stmt(PythonParser.Continue_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#global_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_stmt(PythonParser.Global_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#nonlocal_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonlocal_stmt(PythonParser.Nonlocal_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#del_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDel_stmt(PythonParser.Del_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#yield_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYield_stmt(PythonParser.Yield_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#assert_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssert_stmt(PythonParser.Assert_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#import_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_stmt(PythonParser.Import_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#import_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_name(PythonParser.Import_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#import_from}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_from(PythonParser.Import_fromContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#import_from_targets}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_from_targets(PythonParser.Import_from_targetsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#import_from_as_names}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_from_as_names(PythonParser.Import_from_as_namesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#import_from_as_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_from_as_name(PythonParser.Import_from_as_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#dotted_as_names}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotted_as_names(PythonParser.Dotted_as_namesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#dotted_as_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotted_as_name(PythonParser.Dotted_as_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#dotted_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotted_name(PythonParser.Dotted_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(PythonParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#decorators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecorators(PythonParser.DecoratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#class_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_def(PythonParser.Class_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#class_def_raw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_def_raw(PythonParser.Class_def_rawContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#function_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_def(PythonParser.Function_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#function_def_raw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_def_raw(PythonParser.Function_def_rawContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParams(PythonParser.ParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameters(PythonParser.ParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#slash_no_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlash_no_default(PythonParser.Slash_no_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#slash_with_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlash_with_default(PythonParser.Slash_with_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_etc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_etc(PythonParser.Star_etcContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#kwds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKwds(PythonParser.KwdsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#param_no_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam_no_default(PythonParser.Param_no_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#param_no_default_star_annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam_no_default_star_annotation(PythonParser.Param_no_default_star_annotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#param_with_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam_with_default(PythonParser.Param_with_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#param_maybe_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam_maybe_default(PythonParser.Param_maybe_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(PythonParser.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#param_star_annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam_star_annotation(PythonParser.Param_star_annotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(PythonParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_annotation(PythonParser.Star_annotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#default_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_assignment(PythonParser.Default_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#if_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_stmt(PythonParser.If_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#elif_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElif_stmt(PythonParser.Elif_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#else_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElse_block(PythonParser.Else_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#while_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_stmt(PythonParser.While_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#for_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_stmt(PythonParser.For_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#with_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_stmt(PythonParser.With_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#with_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_item(PythonParser.With_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#try_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTry_stmt(PythonParser.Try_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#except_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExcept_block(PythonParser.Except_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#except_star_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExcept_star_block(PythonParser.Except_star_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#finally_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinally_block(PythonParser.Finally_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#match_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatch_stmt(PythonParser.Match_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#subject_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubject_expr(PythonParser.Subject_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#case_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_block(PythonParser.Case_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#guard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGuard(PythonParser.GuardContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#patterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatterns(PythonParser.PatternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(PythonParser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#as_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAs_pattern(PythonParser.As_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#or_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOr_pattern(PythonParser.Or_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#closed_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosed_pattern(PythonParser.Closed_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#literal_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral_pattern(PythonParser.Literal_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#literal_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral_expr(PythonParser.Literal_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#complex_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplex_number(PythonParser.Complex_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#signed_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned_number(PythonParser.Signed_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#signed_real_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned_real_number(PythonParser.Signed_real_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#real_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReal_number(PythonParser.Real_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#imaginary_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImaginary_number(PythonParser.Imaginary_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#capture_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCapture_pattern(PythonParser.Capture_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#pattern_capture_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern_capture_target(PythonParser.Pattern_capture_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#wildcard_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcard_pattern(PythonParser.Wildcard_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#value_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue_pattern(PythonParser.Value_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttr(PythonParser.AttrContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#name_or_attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName_or_attr(PythonParser.Name_or_attrContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#group_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_pattern(PythonParser.Group_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#sequence_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_pattern(PythonParser.Sequence_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#open_sequence_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_sequence_pattern(PythonParser.Open_sequence_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#maybe_sequence_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaybe_sequence_pattern(PythonParser.Maybe_sequence_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#maybe_star_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaybe_star_pattern(PythonParser.Maybe_star_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_pattern(PythonParser.Star_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#mapping_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapping_pattern(PythonParser.Mapping_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#items_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitItems_pattern(PythonParser.Items_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#key_value_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_value_pattern(PythonParser.Key_value_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#double_star_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDouble_star_pattern(PythonParser.Double_star_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#class_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_pattern(PythonParser.Class_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#positional_patterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositional_patterns(PythonParser.Positional_patternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#keyword_patterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword_patterns(PythonParser.Keyword_patternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#keyword_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword_pattern(PythonParser.Keyword_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_alias(PythonParser.Type_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_params(PythonParser.Type_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_param_seq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_param_seq(PythonParser.Type_param_seqContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_param(PythonParser.Type_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_param_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_param_bound(PythonParser.Type_param_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_param_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_param_default(PythonParser.Type_param_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_param_starred_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_param_starred_default(PythonParser.Type_param_starred_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressions(PythonParser.ExpressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(PythonParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#yield_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYield_expr(PythonParser.Yield_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_expressions(PythonParser.Star_expressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_expression(PythonParser.Star_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_named_expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_named_expressions(PythonParser.Star_named_expressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_named_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_named_expression(PythonParser.Star_named_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#assignment_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_expression(PythonParser.Assignment_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#named_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamed_expression(PythonParser.Named_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#disjunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisjunction(PythonParser.DisjunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#conjunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConjunction(PythonParser.ConjunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#inversion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInversion(PythonParser.InversionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#comparison}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(PythonParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#compare_op_bitwise_or_pair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompare_op_bitwise_or_pair(PythonParser.Compare_op_bitwise_or_pairContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#eq_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEq_bitwise_or(PythonParser.Eq_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#noteq_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNoteq_bitwise_or(PythonParser.Noteq_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lte_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLte_bitwise_or(PythonParser.Lte_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lt_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLt_bitwise_or(PythonParser.Lt_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#gte_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGte_bitwise_or(PythonParser.Gte_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#gt_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGt_bitwise_or(PythonParser.Gt_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#notin_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotin_bitwise_or(PythonParser.Notin_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#in_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIn_bitwise_or(PythonParser.In_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#isnot_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsnot_bitwise_or(PythonParser.Isnot_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#is_bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIs_bitwise_or(PythonParser.Is_bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#bitwise_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwise_or(PythonParser.Bitwise_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#bitwise_xor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwise_xor(PythonParser.Bitwise_xorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#bitwise_and}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwise_and(PythonParser.Bitwise_andContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#shift_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShift_expr(PythonParser.Shift_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#sum}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSum(PythonParser.SumContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTerm(PythonParser.TermContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#factor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFactor(PythonParser.FactorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#power}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPower(PythonParser.PowerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#await_primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAwait_primary(PythonParser.Await_primaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(PythonParser.PrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#slices}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlices(PythonParser.SlicesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#slice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlice(PythonParser.SliceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtom(PythonParser.AtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup(PythonParser.GroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambdef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdef(PythonParser.LambdefContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_params(PythonParser.Lambda_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_parameters(PythonParser.Lambda_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_slash_no_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_slash_no_default(PythonParser.Lambda_slash_no_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_slash_with_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_slash_with_default(PythonParser.Lambda_slash_with_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_star_etc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_star_etc(PythonParser.Lambda_star_etcContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_kwds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_kwds(PythonParser.Lambda_kwdsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_param_no_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_param_no_default(PythonParser.Lambda_param_no_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_param_with_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_param_with_default(PythonParser.Lambda_param_with_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_param_maybe_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_param_maybe_default(PythonParser.Lambda_param_maybe_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#lambda_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_param(PythonParser.Lambda_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#fstring_middle}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFstring_middle(PythonParser.Fstring_middleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#fstring_replacement_field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFstring_replacement_field(PythonParser.Fstring_replacement_fieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#fstring_conversion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFstring_conversion(PythonParser.Fstring_conversionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#fstring_full_format_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFstring_full_format_spec(PythonParser.Fstring_full_format_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#fstring_format_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFstring_format_spec(PythonParser.Fstring_format_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#fstring}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFstring(PythonParser.FstringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#tstring_format_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTstring_format_spec(PythonParser.Tstring_format_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#tstring_full_format_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTstring_full_format_spec(PythonParser.Tstring_full_format_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#tstring_replacement_field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTstring_replacement_field(PythonParser.Tstring_replacement_fieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#tstring_middle}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTstring_middle(PythonParser.Tstring_middleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#tstring}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTstring(PythonParser.TstringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(PythonParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#strings}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStrings(PythonParser.StringsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList(PythonParser.ListContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#tuple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple(PythonParser.TupleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet(PythonParser.SetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#dict}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDict(PythonParser.DictContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#double_starred_kvpairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDouble_starred_kvpairs(PythonParser.Double_starred_kvpairsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#double_starred_kvpair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDouble_starred_kvpair(PythonParser.Double_starred_kvpairContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#kvpair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKvpair(PythonParser.KvpairContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#for_if_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_if_clauses(PythonParser.For_if_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#for_if_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_if_clause(PythonParser.For_if_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#listcomp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListcomp(PythonParser.ListcompContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#setcomp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetcomp(PythonParser.SetcompContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#genexp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenexp(PythonParser.GenexpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#dictcomp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDictcomp(PythonParser.DictcompContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(PythonParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgs(PythonParser.ArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#kwargs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKwargs(PythonParser.KwargsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#starred_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStarred_expression(PythonParser.Starred_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#kwarg_or_starred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKwarg_or_starred(PythonParser.Kwarg_or_starredContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#kwarg_or_double_starred}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKwarg_or_double_starred(PythonParser.Kwarg_or_double_starredContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_targets}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_targets(PythonParser.Star_targetsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_targets_list_seq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_targets_list_seq(PythonParser.Star_targets_list_seqContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_targets_tuple_seq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_targets_tuple_seq(PythonParser.Star_targets_tuple_seqContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_target(PythonParser.Star_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#target_with_star_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#star_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar_atom(PythonParser.Star_atomContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#single_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_target(PythonParser.Single_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#single_subscript_attribute_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_subscript_attribute_target(PythonParser.Single_subscript_attribute_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#t_primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitT_primary(PythonParser.T_primaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#del_targets}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDel_targets(PythonParser.Del_targetsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#del_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDel_target(PythonParser.Del_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#del_t_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDel_t_atom(PythonParser.Del_t_atomContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#type_expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_expressions(PythonParser.Type_expressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#func_type_comment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_type_comment(PythonParser.Func_type_commentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#name_except_underscore}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName_except_underscore(PythonParser.Name_except_underscoreContext ctx);
	/**
	 * Visit a parse tree produced by {@link PythonParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(PythonParser.NameContext ctx);
}