// Generated from PythonParser.g4 by ANTLR 4.13.2
package extension.base.lang.python;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PythonParser}.
 */
public interface PythonParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PythonParser#file_input}.
	 * @param ctx the parse tree
	 */
	void enterFile_input(PythonParser.File_inputContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#file_input}.
	 * @param ctx the parse tree
	 */
	void exitFile_input(PythonParser.File_inputContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#interactive}.
	 * @param ctx the parse tree
	 */
	void enterInteractive(PythonParser.InteractiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#interactive}.
	 * @param ctx the parse tree
	 */
	void exitInteractive(PythonParser.InteractiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#eval}.
	 * @param ctx the parse tree
	 */
	void enterEval(PythonParser.EvalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#eval}.
	 * @param ctx the parse tree
	 */
	void exitEval(PythonParser.EvalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#func_type}.
	 * @param ctx the parse tree
	 */
	void enterFunc_type(PythonParser.Func_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#func_type}.
	 * @param ctx the parse tree
	 */
	void exitFunc_type(PythonParser.Func_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#statements}.
	 * @param ctx the parse tree
	 */
	void enterStatements(PythonParser.StatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#statements}.
	 * @param ctx the parse tree
	 */
	void exitStatements(PythonParser.StatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(PythonParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(PythonParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#single_compound_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSingle_compound_stmt(PythonParser.Single_compound_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#single_compound_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSingle_compound_stmt(PythonParser.Single_compound_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#statement_newline}.
	 * @param ctx the parse tree
	 */
	void enterStatement_newline(PythonParser.Statement_newlineContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#statement_newline}.
	 * @param ctx the parse tree
	 */
	void exitStatement_newline(PythonParser.Statement_newlineContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#simple_stmts}.
	 * @param ctx the parse tree
	 */
	void enterSimple_stmts(PythonParser.Simple_stmtsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#simple_stmts}.
	 * @param ctx the parse tree
	 */
	void exitSimple_stmts(PythonParser.Simple_stmtsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#simple_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSimple_stmt(PythonParser.Simple_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#simple_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSimple_stmt(PythonParser.Simple_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#compound_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCompound_stmt(PythonParser.Compound_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#compound_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCompound_stmt(PythonParser.Compound_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(PythonParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(PythonParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#annotated_rhs}.
	 * @param ctx the parse tree
	 */
	void enterAnnotated_rhs(PythonParser.Annotated_rhsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#annotated_rhs}.
	 * @param ctx the parse tree
	 */
	void exitAnnotated_rhs(PythonParser.Annotated_rhsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#augassign}.
	 * @param ctx the parse tree
	 */
	void enterAugassign(PythonParser.AugassignContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#augassign}.
	 * @param ctx the parse tree
	 */
	void exitAugassign(PythonParser.AugassignContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#return_stmt}.
	 * @param ctx the parse tree
	 */
	void enterReturn_stmt(PythonParser.Return_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#return_stmt}.
	 * @param ctx the parse tree
	 */
	void exitReturn_stmt(PythonParser.Return_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#raise_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRaise_stmt(PythonParser.Raise_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#raise_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRaise_stmt(PythonParser.Raise_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#pass_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPass_stmt(PythonParser.Pass_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#pass_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPass_stmt(PythonParser.Pass_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#break_stmt}.
	 * @param ctx the parse tree
	 */
	void enterBreak_stmt(PythonParser.Break_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#break_stmt}.
	 * @param ctx the parse tree
	 */
	void exitBreak_stmt(PythonParser.Break_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#continue_stmt}.
	 * @param ctx the parse tree
	 */
	void enterContinue_stmt(PythonParser.Continue_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#continue_stmt}.
	 * @param ctx the parse tree
	 */
	void exitContinue_stmt(PythonParser.Continue_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#global_stmt}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_stmt(PythonParser.Global_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#global_stmt}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_stmt(PythonParser.Global_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#nonlocal_stmt}.
	 * @param ctx the parse tree
	 */
	void enterNonlocal_stmt(PythonParser.Nonlocal_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#nonlocal_stmt}.
	 * @param ctx the parse tree
	 */
	void exitNonlocal_stmt(PythonParser.Nonlocal_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#del_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDel_stmt(PythonParser.Del_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#del_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDel_stmt(PythonParser.Del_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#yield_stmt}.
	 * @param ctx the parse tree
	 */
	void enterYield_stmt(PythonParser.Yield_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#yield_stmt}.
	 * @param ctx the parse tree
	 */
	void exitYield_stmt(PythonParser.Yield_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#assert_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAssert_stmt(PythonParser.Assert_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#assert_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAssert_stmt(PythonParser.Assert_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#import_stmt}.
	 * @param ctx the parse tree
	 */
	void enterImport_stmt(PythonParser.Import_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#import_stmt}.
	 * @param ctx the parse tree
	 */
	void exitImport_stmt(PythonParser.Import_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#import_name}.
	 * @param ctx the parse tree
	 */
	void enterImport_name(PythonParser.Import_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#import_name}.
	 * @param ctx the parse tree
	 */
	void exitImport_name(PythonParser.Import_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#import_from}.
	 * @param ctx the parse tree
	 */
	void enterImport_from(PythonParser.Import_fromContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#import_from}.
	 * @param ctx the parse tree
	 */
	void exitImport_from(PythonParser.Import_fromContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#import_from_targets}.
	 * @param ctx the parse tree
	 */
	void enterImport_from_targets(PythonParser.Import_from_targetsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#import_from_targets}.
	 * @param ctx the parse tree
	 */
	void exitImport_from_targets(PythonParser.Import_from_targetsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#import_from_as_names}.
	 * @param ctx the parse tree
	 */
	void enterImport_from_as_names(PythonParser.Import_from_as_namesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#import_from_as_names}.
	 * @param ctx the parse tree
	 */
	void exitImport_from_as_names(PythonParser.Import_from_as_namesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#import_from_as_name}.
	 * @param ctx the parse tree
	 */
	void enterImport_from_as_name(PythonParser.Import_from_as_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#import_from_as_name}.
	 * @param ctx the parse tree
	 */
	void exitImport_from_as_name(PythonParser.Import_from_as_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#dotted_as_names}.
	 * @param ctx the parse tree
	 */
	void enterDotted_as_names(PythonParser.Dotted_as_namesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#dotted_as_names}.
	 * @param ctx the parse tree
	 */
	void exitDotted_as_names(PythonParser.Dotted_as_namesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#dotted_as_name}.
	 * @param ctx the parse tree
	 */
	void enterDotted_as_name(PythonParser.Dotted_as_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#dotted_as_name}.
	 * @param ctx the parse tree
	 */
	void exitDotted_as_name(PythonParser.Dotted_as_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#dotted_name}.
	 * @param ctx the parse tree
	 */
	void enterDotted_name(PythonParser.Dotted_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#dotted_name}.
	 * @param ctx the parse tree
	 */
	void exitDotted_name(PythonParser.Dotted_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(PythonParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(PythonParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#decorators}.
	 * @param ctx the parse tree
	 */
	void enterDecorators(PythonParser.DecoratorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#decorators}.
	 * @param ctx the parse tree
	 */
	void exitDecorators(PythonParser.DecoratorsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#class_def}.
	 * @param ctx the parse tree
	 */
	void enterClass_def(PythonParser.Class_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#class_def}.
	 * @param ctx the parse tree
	 */
	void exitClass_def(PythonParser.Class_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#class_def_raw}.
	 * @param ctx the parse tree
	 */
	void enterClass_def_raw(PythonParser.Class_def_rawContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#class_def_raw}.
	 * @param ctx the parse tree
	 */
	void exitClass_def_raw(PythonParser.Class_def_rawContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#function_def}.
	 * @param ctx the parse tree
	 */
	void enterFunction_def(PythonParser.Function_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#function_def}.
	 * @param ctx the parse tree
	 */
	void exitFunction_def(PythonParser.Function_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#function_def_raw}.
	 * @param ctx the parse tree
	 */
	void enterFunction_def_raw(PythonParser.Function_def_rawContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#function_def_raw}.
	 * @param ctx the parse tree
	 */
	void exitFunction_def_raw(PythonParser.Function_def_rawContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#params}.
	 * @param ctx the parse tree
	 */
	void enterParams(PythonParser.ParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#params}.
	 * @param ctx the parse tree
	 */
	void exitParams(PythonParser.ParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#parameters}.
	 * @param ctx the parse tree
	 */
	void enterParameters(PythonParser.ParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#parameters}.
	 * @param ctx the parse tree
	 */
	void exitParameters(PythonParser.ParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#slash_no_default}.
	 * @param ctx the parse tree
	 */
	void enterSlash_no_default(PythonParser.Slash_no_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#slash_no_default}.
	 * @param ctx the parse tree
	 */
	void exitSlash_no_default(PythonParser.Slash_no_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#slash_with_default}.
	 * @param ctx the parse tree
	 */
	void enterSlash_with_default(PythonParser.Slash_with_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#slash_with_default}.
	 * @param ctx the parse tree
	 */
	void exitSlash_with_default(PythonParser.Slash_with_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_etc}.
	 * @param ctx the parse tree
	 */
	void enterStar_etc(PythonParser.Star_etcContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_etc}.
	 * @param ctx the parse tree
	 */
	void exitStar_etc(PythonParser.Star_etcContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#kwds}.
	 * @param ctx the parse tree
	 */
	void enterKwds(PythonParser.KwdsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#kwds}.
	 * @param ctx the parse tree
	 */
	void exitKwds(PythonParser.KwdsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#param_no_default}.
	 * @param ctx the parse tree
	 */
	void enterParam_no_default(PythonParser.Param_no_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#param_no_default}.
	 * @param ctx the parse tree
	 */
	void exitParam_no_default(PythonParser.Param_no_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#param_no_default_star_annotation}.
	 * @param ctx the parse tree
	 */
	void enterParam_no_default_star_annotation(PythonParser.Param_no_default_star_annotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#param_no_default_star_annotation}.
	 * @param ctx the parse tree
	 */
	void exitParam_no_default_star_annotation(PythonParser.Param_no_default_star_annotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#param_with_default}.
	 * @param ctx the parse tree
	 */
	void enterParam_with_default(PythonParser.Param_with_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#param_with_default}.
	 * @param ctx the parse tree
	 */
	void exitParam_with_default(PythonParser.Param_with_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#param_maybe_default}.
	 * @param ctx the parse tree
	 */
	void enterParam_maybe_default(PythonParser.Param_maybe_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#param_maybe_default}.
	 * @param ctx the parse tree
	 */
	void exitParam_maybe_default(PythonParser.Param_maybe_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#param}.
	 * @param ctx the parse tree
	 */
	void enterParam(PythonParser.ParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#param}.
	 * @param ctx the parse tree
	 */
	void exitParam(PythonParser.ParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#param_star_annotation}.
	 * @param ctx the parse tree
	 */
	void enterParam_star_annotation(PythonParser.Param_star_annotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#param_star_annotation}.
	 * @param ctx the parse tree
	 */
	void exitParam_star_annotation(PythonParser.Param_star_annotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(PythonParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(PythonParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_annotation}.
	 * @param ctx the parse tree
	 */
	void enterStar_annotation(PythonParser.Star_annotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_annotation}.
	 * @param ctx the parse tree
	 */
	void exitStar_annotation(PythonParser.Star_annotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#default_assignment}.
	 * @param ctx the parse tree
	 */
	void enterDefault_assignment(PythonParser.Default_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#default_assignment}.
	 * @param ctx the parse tree
	 */
	void exitDefault_assignment(PythonParser.Default_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#if_stmt}.
	 * @param ctx the parse tree
	 */
	void enterIf_stmt(PythonParser.If_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#if_stmt}.
	 * @param ctx the parse tree
	 */
	void exitIf_stmt(PythonParser.If_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#elif_stmt}.
	 * @param ctx the parse tree
	 */
	void enterElif_stmt(PythonParser.Elif_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#elif_stmt}.
	 * @param ctx the parse tree
	 */
	void exitElif_stmt(PythonParser.Elif_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#else_block}.
	 * @param ctx the parse tree
	 */
	void enterElse_block(PythonParser.Else_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#else_block}.
	 * @param ctx the parse tree
	 */
	void exitElse_block(PythonParser.Else_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#while_stmt}.
	 * @param ctx the parse tree
	 */
	void enterWhile_stmt(PythonParser.While_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#while_stmt}.
	 * @param ctx the parse tree
	 */
	void exitWhile_stmt(PythonParser.While_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#for_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFor_stmt(PythonParser.For_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#for_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFor_stmt(PythonParser.For_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#with_stmt}.
	 * @param ctx the parse tree
	 */
	void enterWith_stmt(PythonParser.With_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#with_stmt}.
	 * @param ctx the parse tree
	 */
	void exitWith_stmt(PythonParser.With_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#with_item}.
	 * @param ctx the parse tree
	 */
	void enterWith_item(PythonParser.With_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#with_item}.
	 * @param ctx the parse tree
	 */
	void exitWith_item(PythonParser.With_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#try_stmt}.
	 * @param ctx the parse tree
	 */
	void enterTry_stmt(PythonParser.Try_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#try_stmt}.
	 * @param ctx the parse tree
	 */
	void exitTry_stmt(PythonParser.Try_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#except_block}.
	 * @param ctx the parse tree
	 */
	void enterExcept_block(PythonParser.Except_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#except_block}.
	 * @param ctx the parse tree
	 */
	void exitExcept_block(PythonParser.Except_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#except_star_block}.
	 * @param ctx the parse tree
	 */
	void enterExcept_star_block(PythonParser.Except_star_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#except_star_block}.
	 * @param ctx the parse tree
	 */
	void exitExcept_star_block(PythonParser.Except_star_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#finally_block}.
	 * @param ctx the parse tree
	 */
	void enterFinally_block(PythonParser.Finally_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#finally_block}.
	 * @param ctx the parse tree
	 */
	void exitFinally_block(PythonParser.Finally_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#match_stmt}.
	 * @param ctx the parse tree
	 */
	void enterMatch_stmt(PythonParser.Match_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#match_stmt}.
	 * @param ctx the parse tree
	 */
	void exitMatch_stmt(PythonParser.Match_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#subject_expr}.
	 * @param ctx the parse tree
	 */
	void enterSubject_expr(PythonParser.Subject_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#subject_expr}.
	 * @param ctx the parse tree
	 */
	void exitSubject_expr(PythonParser.Subject_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#case_block}.
	 * @param ctx the parse tree
	 */
	void enterCase_block(PythonParser.Case_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#case_block}.
	 * @param ctx the parse tree
	 */
	void exitCase_block(PythonParser.Case_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#guard}.
	 * @param ctx the parse tree
	 */
	void enterGuard(PythonParser.GuardContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#guard}.
	 * @param ctx the parse tree
	 */
	void exitGuard(PythonParser.GuardContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#patterns}.
	 * @param ctx the parse tree
	 */
	void enterPatterns(PythonParser.PatternsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#patterns}.
	 * @param ctx the parse tree
	 */
	void exitPatterns(PythonParser.PatternsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPattern(PythonParser.PatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPattern(PythonParser.PatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#as_pattern}.
	 * @param ctx the parse tree
	 */
	void enterAs_pattern(PythonParser.As_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#as_pattern}.
	 * @param ctx the parse tree
	 */
	void exitAs_pattern(PythonParser.As_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#or_pattern}.
	 * @param ctx the parse tree
	 */
	void enterOr_pattern(PythonParser.Or_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#or_pattern}.
	 * @param ctx the parse tree
	 */
	void exitOr_pattern(PythonParser.Or_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#closed_pattern}.
	 * @param ctx the parse tree
	 */
	void enterClosed_pattern(PythonParser.Closed_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#closed_pattern}.
	 * @param ctx the parse tree
	 */
	void exitClosed_pattern(PythonParser.Closed_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#literal_pattern}.
	 * @param ctx the parse tree
	 */
	void enterLiteral_pattern(PythonParser.Literal_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#literal_pattern}.
	 * @param ctx the parse tree
	 */
	void exitLiteral_pattern(PythonParser.Literal_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#literal_expr}.
	 * @param ctx the parse tree
	 */
	void enterLiteral_expr(PythonParser.Literal_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#literal_expr}.
	 * @param ctx the parse tree
	 */
	void exitLiteral_expr(PythonParser.Literal_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#complex_number}.
	 * @param ctx the parse tree
	 */
	void enterComplex_number(PythonParser.Complex_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#complex_number}.
	 * @param ctx the parse tree
	 */
	void exitComplex_number(PythonParser.Complex_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#signed_number}.
	 * @param ctx the parse tree
	 */
	void enterSigned_number(PythonParser.Signed_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#signed_number}.
	 * @param ctx the parse tree
	 */
	void exitSigned_number(PythonParser.Signed_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#signed_real_number}.
	 * @param ctx the parse tree
	 */
	void enterSigned_real_number(PythonParser.Signed_real_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#signed_real_number}.
	 * @param ctx the parse tree
	 */
	void exitSigned_real_number(PythonParser.Signed_real_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#real_number}.
	 * @param ctx the parse tree
	 */
	void enterReal_number(PythonParser.Real_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#real_number}.
	 * @param ctx the parse tree
	 */
	void exitReal_number(PythonParser.Real_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#imaginary_number}.
	 * @param ctx the parse tree
	 */
	void enterImaginary_number(PythonParser.Imaginary_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#imaginary_number}.
	 * @param ctx the parse tree
	 */
	void exitImaginary_number(PythonParser.Imaginary_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#capture_pattern}.
	 * @param ctx the parse tree
	 */
	void enterCapture_pattern(PythonParser.Capture_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#capture_pattern}.
	 * @param ctx the parse tree
	 */
	void exitCapture_pattern(PythonParser.Capture_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#pattern_capture_target}.
	 * @param ctx the parse tree
	 */
	void enterPattern_capture_target(PythonParser.Pattern_capture_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#pattern_capture_target}.
	 * @param ctx the parse tree
	 */
	void exitPattern_capture_target(PythonParser.Pattern_capture_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#wildcard_pattern}.
	 * @param ctx the parse tree
	 */
	void enterWildcard_pattern(PythonParser.Wildcard_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#wildcard_pattern}.
	 * @param ctx the parse tree
	 */
	void exitWildcard_pattern(PythonParser.Wildcard_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#value_pattern}.
	 * @param ctx the parse tree
	 */
	void enterValue_pattern(PythonParser.Value_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#value_pattern}.
	 * @param ctx the parse tree
	 */
	void exitValue_pattern(PythonParser.Value_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#attr}.
	 * @param ctx the parse tree
	 */
	void enterAttr(PythonParser.AttrContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#attr}.
	 * @param ctx the parse tree
	 */
	void exitAttr(PythonParser.AttrContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#name_or_attr}.
	 * @param ctx the parse tree
	 */
	void enterName_or_attr(PythonParser.Name_or_attrContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#name_or_attr}.
	 * @param ctx the parse tree
	 */
	void exitName_or_attr(PythonParser.Name_or_attrContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#group_pattern}.
	 * @param ctx the parse tree
	 */
	void enterGroup_pattern(PythonParser.Group_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#group_pattern}.
	 * @param ctx the parse tree
	 */
	void exitGroup_pattern(PythonParser.Group_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#sequence_pattern}.
	 * @param ctx the parse tree
	 */
	void enterSequence_pattern(PythonParser.Sequence_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#sequence_pattern}.
	 * @param ctx the parse tree
	 */
	void exitSequence_pattern(PythonParser.Sequence_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#open_sequence_pattern}.
	 * @param ctx the parse tree
	 */
	void enterOpen_sequence_pattern(PythonParser.Open_sequence_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#open_sequence_pattern}.
	 * @param ctx the parse tree
	 */
	void exitOpen_sequence_pattern(PythonParser.Open_sequence_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#maybe_sequence_pattern}.
	 * @param ctx the parse tree
	 */
	void enterMaybe_sequence_pattern(PythonParser.Maybe_sequence_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#maybe_sequence_pattern}.
	 * @param ctx the parse tree
	 */
	void exitMaybe_sequence_pattern(PythonParser.Maybe_sequence_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#maybe_star_pattern}.
	 * @param ctx the parse tree
	 */
	void enterMaybe_star_pattern(PythonParser.Maybe_star_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#maybe_star_pattern}.
	 * @param ctx the parse tree
	 */
	void exitMaybe_star_pattern(PythonParser.Maybe_star_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_pattern}.
	 * @param ctx the parse tree
	 */
	void enterStar_pattern(PythonParser.Star_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_pattern}.
	 * @param ctx the parse tree
	 */
	void exitStar_pattern(PythonParser.Star_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#mapping_pattern}.
	 * @param ctx the parse tree
	 */
	void enterMapping_pattern(PythonParser.Mapping_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#mapping_pattern}.
	 * @param ctx the parse tree
	 */
	void exitMapping_pattern(PythonParser.Mapping_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#items_pattern}.
	 * @param ctx the parse tree
	 */
	void enterItems_pattern(PythonParser.Items_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#items_pattern}.
	 * @param ctx the parse tree
	 */
	void exitItems_pattern(PythonParser.Items_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#key_value_pattern}.
	 * @param ctx the parse tree
	 */
	void enterKey_value_pattern(PythonParser.Key_value_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#key_value_pattern}.
	 * @param ctx the parse tree
	 */
	void exitKey_value_pattern(PythonParser.Key_value_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#double_star_pattern}.
	 * @param ctx the parse tree
	 */
	void enterDouble_star_pattern(PythonParser.Double_star_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#double_star_pattern}.
	 * @param ctx the parse tree
	 */
	void exitDouble_star_pattern(PythonParser.Double_star_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#class_pattern}.
	 * @param ctx the parse tree
	 */
	void enterClass_pattern(PythonParser.Class_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#class_pattern}.
	 * @param ctx the parse tree
	 */
	void exitClass_pattern(PythonParser.Class_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#positional_patterns}.
	 * @param ctx the parse tree
	 */
	void enterPositional_patterns(PythonParser.Positional_patternsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#positional_patterns}.
	 * @param ctx the parse tree
	 */
	void exitPositional_patterns(PythonParser.Positional_patternsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#keyword_patterns}.
	 * @param ctx the parse tree
	 */
	void enterKeyword_patterns(PythonParser.Keyword_patternsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#keyword_patterns}.
	 * @param ctx the parse tree
	 */
	void exitKeyword_patterns(PythonParser.Keyword_patternsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#keyword_pattern}.
	 * @param ctx the parse tree
	 */
	void enterKeyword_pattern(PythonParser.Keyword_patternContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#keyword_pattern}.
	 * @param ctx the parse tree
	 */
	void exitKeyword_pattern(PythonParser.Keyword_patternContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_alias}.
	 * @param ctx the parse tree
	 */
	void enterType_alias(PythonParser.Type_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_alias}.
	 * @param ctx the parse tree
	 */
	void exitType_alias(PythonParser.Type_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_params}.
	 * @param ctx the parse tree
	 */
	void enterType_params(PythonParser.Type_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_params}.
	 * @param ctx the parse tree
	 */
	void exitType_params(PythonParser.Type_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_param_seq}.
	 * @param ctx the parse tree
	 */
	void enterType_param_seq(PythonParser.Type_param_seqContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_param_seq}.
	 * @param ctx the parse tree
	 */
	void exitType_param_seq(PythonParser.Type_param_seqContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_param}.
	 * @param ctx the parse tree
	 */
	void enterType_param(PythonParser.Type_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_param}.
	 * @param ctx the parse tree
	 */
	void exitType_param(PythonParser.Type_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_param_bound}.
	 * @param ctx the parse tree
	 */
	void enterType_param_bound(PythonParser.Type_param_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_param_bound}.
	 * @param ctx the parse tree
	 */
	void exitType_param_bound(PythonParser.Type_param_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_param_default}.
	 * @param ctx the parse tree
	 */
	void enterType_param_default(PythonParser.Type_param_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_param_default}.
	 * @param ctx the parse tree
	 */
	void exitType_param_default(PythonParser.Type_param_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_param_starred_default}.
	 * @param ctx the parse tree
	 */
	void enterType_param_starred_default(PythonParser.Type_param_starred_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_param_starred_default}.
	 * @param ctx the parse tree
	 */
	void exitType_param_starred_default(PythonParser.Type_param_starred_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#expressions}.
	 * @param ctx the parse tree
	 */
	void enterExpressions(PythonParser.ExpressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#expressions}.
	 * @param ctx the parse tree
	 */
	void exitExpressions(PythonParser.ExpressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(PythonParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(PythonParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#yield_expr}.
	 * @param ctx the parse tree
	 */
	void enterYield_expr(PythonParser.Yield_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#yield_expr}.
	 * @param ctx the parse tree
	 */
	void exitYield_expr(PythonParser.Yield_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_expressions}.
	 * @param ctx the parse tree
	 */
	void enterStar_expressions(PythonParser.Star_expressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_expressions}.
	 * @param ctx the parse tree
	 */
	void exitStar_expressions(PythonParser.Star_expressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_expression}.
	 * @param ctx the parse tree
	 */
	void enterStar_expression(PythonParser.Star_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_expression}.
	 * @param ctx the parse tree
	 */
	void exitStar_expression(PythonParser.Star_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_named_expressions}.
	 * @param ctx the parse tree
	 */
	void enterStar_named_expressions(PythonParser.Star_named_expressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_named_expressions}.
	 * @param ctx the parse tree
	 */
	void exitStar_named_expressions(PythonParser.Star_named_expressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_named_expression}.
	 * @param ctx the parse tree
	 */
	void enterStar_named_expression(PythonParser.Star_named_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_named_expression}.
	 * @param ctx the parse tree
	 */
	void exitStar_named_expression(PythonParser.Star_named_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#assignment_expression}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_expression(PythonParser.Assignment_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#assignment_expression}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_expression(PythonParser.Assignment_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#named_expression}.
	 * @param ctx the parse tree
	 */
	void enterNamed_expression(PythonParser.Named_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#named_expression}.
	 * @param ctx the parse tree
	 */
	void exitNamed_expression(PythonParser.Named_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#disjunction}.
	 * @param ctx the parse tree
	 */
	void enterDisjunction(PythonParser.DisjunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#disjunction}.
	 * @param ctx the parse tree
	 */
	void exitDisjunction(PythonParser.DisjunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#conjunction}.
	 * @param ctx the parse tree
	 */
	void enterConjunction(PythonParser.ConjunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#conjunction}.
	 * @param ctx the parse tree
	 */
	void exitConjunction(PythonParser.ConjunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#inversion}.
	 * @param ctx the parse tree
	 */
	void enterInversion(PythonParser.InversionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#inversion}.
	 * @param ctx the parse tree
	 */
	void exitInversion(PythonParser.InversionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#comparison}.
	 * @param ctx the parse tree
	 */
	void enterComparison(PythonParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#comparison}.
	 * @param ctx the parse tree
	 */
	void exitComparison(PythonParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#compare_op_bitwise_or_pair}.
	 * @param ctx the parse tree
	 */
	void enterCompare_op_bitwise_or_pair(PythonParser.Compare_op_bitwise_or_pairContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#compare_op_bitwise_or_pair}.
	 * @param ctx the parse tree
	 */
	void exitCompare_op_bitwise_or_pair(PythonParser.Compare_op_bitwise_or_pairContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#eq_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterEq_bitwise_or(PythonParser.Eq_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#eq_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitEq_bitwise_or(PythonParser.Eq_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#noteq_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterNoteq_bitwise_or(PythonParser.Noteq_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#noteq_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitNoteq_bitwise_or(PythonParser.Noteq_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lte_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterLte_bitwise_or(PythonParser.Lte_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lte_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitLte_bitwise_or(PythonParser.Lte_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lt_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterLt_bitwise_or(PythonParser.Lt_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lt_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitLt_bitwise_or(PythonParser.Lt_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#gte_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterGte_bitwise_or(PythonParser.Gte_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#gte_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitGte_bitwise_or(PythonParser.Gte_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#gt_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterGt_bitwise_or(PythonParser.Gt_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#gt_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitGt_bitwise_or(PythonParser.Gt_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#notin_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterNotin_bitwise_or(PythonParser.Notin_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#notin_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitNotin_bitwise_or(PythonParser.Notin_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#in_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterIn_bitwise_or(PythonParser.In_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#in_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitIn_bitwise_or(PythonParser.In_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#isnot_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterIsnot_bitwise_or(PythonParser.Isnot_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#isnot_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitIsnot_bitwise_or(PythonParser.Isnot_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#is_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterIs_bitwise_or(PythonParser.Is_bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#is_bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitIs_bitwise_or(PythonParser.Is_bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#bitwise_or}.
	 * @param ctx the parse tree
	 */
	void enterBitwise_or(PythonParser.Bitwise_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#bitwise_or}.
	 * @param ctx the parse tree
	 */
	void exitBitwise_or(PythonParser.Bitwise_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#bitwise_xor}.
	 * @param ctx the parse tree
	 */
	void enterBitwise_xor(PythonParser.Bitwise_xorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#bitwise_xor}.
	 * @param ctx the parse tree
	 */
	void exitBitwise_xor(PythonParser.Bitwise_xorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#bitwise_and}.
	 * @param ctx the parse tree
	 */
	void enterBitwise_and(PythonParser.Bitwise_andContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#bitwise_and}.
	 * @param ctx the parse tree
	 */
	void exitBitwise_and(PythonParser.Bitwise_andContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#shift_expr}.
	 * @param ctx the parse tree
	 */
	void enterShift_expr(PythonParser.Shift_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#shift_expr}.
	 * @param ctx the parse tree
	 */
	void exitShift_expr(PythonParser.Shift_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#sum}.
	 * @param ctx the parse tree
	 */
	void enterSum(PythonParser.SumContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#sum}.
	 * @param ctx the parse tree
	 */
	void exitSum(PythonParser.SumContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#term}.
	 * @param ctx the parse tree
	 */
	void enterTerm(PythonParser.TermContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#term}.
	 * @param ctx the parse tree
	 */
	void exitTerm(PythonParser.TermContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#factor}.
	 * @param ctx the parse tree
	 */
	void enterFactor(PythonParser.FactorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#factor}.
	 * @param ctx the parse tree
	 */
	void exitFactor(PythonParser.FactorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#power}.
	 * @param ctx the parse tree
	 */
	void enterPower(PythonParser.PowerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#power}.
	 * @param ctx the parse tree
	 */
	void exitPower(PythonParser.PowerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#await_primary}.
	 * @param ctx the parse tree
	 */
	void enterAwait_primary(PythonParser.Await_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#await_primary}.
	 * @param ctx the parse tree
	 */
	void exitAwait_primary(PythonParser.Await_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(PythonParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(PythonParser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#slices}.
	 * @param ctx the parse tree
	 */
	void enterSlices(PythonParser.SlicesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#slices}.
	 * @param ctx the parse tree
	 */
	void exitSlices(PythonParser.SlicesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#slice}.
	 * @param ctx the parse tree
	 */
	void enterSlice(PythonParser.SliceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#slice}.
	 * @param ctx the parse tree
	 */
	void exitSlice(PythonParser.SliceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(PythonParser.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(PythonParser.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#group}.
	 * @param ctx the parse tree
	 */
	void enterGroup(PythonParser.GroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#group}.
	 * @param ctx the parse tree
	 */
	void exitGroup(PythonParser.GroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambdef}.
	 * @param ctx the parse tree
	 */
	void enterLambdef(PythonParser.LambdefContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambdef}.
	 * @param ctx the parse tree
	 */
	void exitLambdef(PythonParser.LambdefContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_params}.
	 * @param ctx the parse tree
	 */
	void enterLambda_params(PythonParser.Lambda_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_params}.
	 * @param ctx the parse tree
	 */
	void exitLambda_params(PythonParser.Lambda_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLambda_parameters(PythonParser.Lambda_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLambda_parameters(PythonParser.Lambda_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_slash_no_default}.
	 * @param ctx the parse tree
	 */
	void enterLambda_slash_no_default(PythonParser.Lambda_slash_no_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_slash_no_default}.
	 * @param ctx the parse tree
	 */
	void exitLambda_slash_no_default(PythonParser.Lambda_slash_no_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_slash_with_default}.
	 * @param ctx the parse tree
	 */
	void enterLambda_slash_with_default(PythonParser.Lambda_slash_with_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_slash_with_default}.
	 * @param ctx the parse tree
	 */
	void exitLambda_slash_with_default(PythonParser.Lambda_slash_with_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_star_etc}.
	 * @param ctx the parse tree
	 */
	void enterLambda_star_etc(PythonParser.Lambda_star_etcContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_star_etc}.
	 * @param ctx the parse tree
	 */
	void exitLambda_star_etc(PythonParser.Lambda_star_etcContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_kwds}.
	 * @param ctx the parse tree
	 */
	void enterLambda_kwds(PythonParser.Lambda_kwdsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_kwds}.
	 * @param ctx the parse tree
	 */
	void exitLambda_kwds(PythonParser.Lambda_kwdsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_param_no_default}.
	 * @param ctx the parse tree
	 */
	void enterLambda_param_no_default(PythonParser.Lambda_param_no_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_param_no_default}.
	 * @param ctx the parse tree
	 */
	void exitLambda_param_no_default(PythonParser.Lambda_param_no_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_param_with_default}.
	 * @param ctx the parse tree
	 */
	void enterLambda_param_with_default(PythonParser.Lambda_param_with_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_param_with_default}.
	 * @param ctx the parse tree
	 */
	void exitLambda_param_with_default(PythonParser.Lambda_param_with_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_param_maybe_default}.
	 * @param ctx the parse tree
	 */
	void enterLambda_param_maybe_default(PythonParser.Lambda_param_maybe_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_param_maybe_default}.
	 * @param ctx the parse tree
	 */
	void exitLambda_param_maybe_default(PythonParser.Lambda_param_maybe_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#lambda_param}.
	 * @param ctx the parse tree
	 */
	void enterLambda_param(PythonParser.Lambda_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#lambda_param}.
	 * @param ctx the parse tree
	 */
	void exitLambda_param(PythonParser.Lambda_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#fstring_middle}.
	 * @param ctx the parse tree
	 */
	void enterFstring_middle(PythonParser.Fstring_middleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#fstring_middle}.
	 * @param ctx the parse tree
	 */
	void exitFstring_middle(PythonParser.Fstring_middleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#fstring_replacement_field}.
	 * @param ctx the parse tree
	 */
	void enterFstring_replacement_field(PythonParser.Fstring_replacement_fieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#fstring_replacement_field}.
	 * @param ctx the parse tree
	 */
	void exitFstring_replacement_field(PythonParser.Fstring_replacement_fieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#fstring_conversion}.
	 * @param ctx the parse tree
	 */
	void enterFstring_conversion(PythonParser.Fstring_conversionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#fstring_conversion}.
	 * @param ctx the parse tree
	 */
	void exitFstring_conversion(PythonParser.Fstring_conversionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#fstring_full_format_spec}.
	 * @param ctx the parse tree
	 */
	void enterFstring_full_format_spec(PythonParser.Fstring_full_format_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#fstring_full_format_spec}.
	 * @param ctx the parse tree
	 */
	void exitFstring_full_format_spec(PythonParser.Fstring_full_format_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#fstring_format_spec}.
	 * @param ctx the parse tree
	 */
	void enterFstring_format_spec(PythonParser.Fstring_format_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#fstring_format_spec}.
	 * @param ctx the parse tree
	 */
	void exitFstring_format_spec(PythonParser.Fstring_format_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#fstring}.
	 * @param ctx the parse tree
	 */
	void enterFstring(PythonParser.FstringContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#fstring}.
	 * @param ctx the parse tree
	 */
	void exitFstring(PythonParser.FstringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#tstring_format_spec}.
	 * @param ctx the parse tree
	 */
	void enterTstring_format_spec(PythonParser.Tstring_format_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#tstring_format_spec}.
	 * @param ctx the parse tree
	 */
	void exitTstring_format_spec(PythonParser.Tstring_format_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#tstring_full_format_spec}.
	 * @param ctx the parse tree
	 */
	void enterTstring_full_format_spec(PythonParser.Tstring_full_format_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#tstring_full_format_spec}.
	 * @param ctx the parse tree
	 */
	void exitTstring_full_format_spec(PythonParser.Tstring_full_format_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#tstring_replacement_field}.
	 * @param ctx the parse tree
	 */
	void enterTstring_replacement_field(PythonParser.Tstring_replacement_fieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#tstring_replacement_field}.
	 * @param ctx the parse tree
	 */
	void exitTstring_replacement_field(PythonParser.Tstring_replacement_fieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#tstring_middle}.
	 * @param ctx the parse tree
	 */
	void enterTstring_middle(PythonParser.Tstring_middleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#tstring_middle}.
	 * @param ctx the parse tree
	 */
	void exitTstring_middle(PythonParser.Tstring_middleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#tstring}.
	 * @param ctx the parse tree
	 */
	void enterTstring(PythonParser.TstringContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#tstring}.
	 * @param ctx the parse tree
	 */
	void exitTstring(PythonParser.TstringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#string}.
	 * @param ctx the parse tree
	 */
	void enterString(PythonParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#string}.
	 * @param ctx the parse tree
	 */
	void exitString(PythonParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#strings}.
	 * @param ctx the parse tree
	 */
	void enterStrings(PythonParser.StringsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#strings}.
	 * @param ctx the parse tree
	 */
	void exitStrings(PythonParser.StringsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#list}.
	 * @param ctx the parse tree
	 */
	void enterList(PythonParser.ListContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#list}.
	 * @param ctx the parse tree
	 */
	void exitList(PythonParser.ListContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#tuple}.
	 * @param ctx the parse tree
	 */
	void enterTuple(PythonParser.TupleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#tuple}.
	 * @param ctx the parse tree
	 */
	void exitTuple(PythonParser.TupleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#set}.
	 * @param ctx the parse tree
	 */
	void enterSet(PythonParser.SetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#set}.
	 * @param ctx the parse tree
	 */
	void exitSet(PythonParser.SetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#dict}.
	 * @param ctx the parse tree
	 */
	void enterDict(PythonParser.DictContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#dict}.
	 * @param ctx the parse tree
	 */
	void exitDict(PythonParser.DictContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#double_starred_kvpairs}.
	 * @param ctx the parse tree
	 */
	void enterDouble_starred_kvpairs(PythonParser.Double_starred_kvpairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#double_starred_kvpairs}.
	 * @param ctx the parse tree
	 */
	void exitDouble_starred_kvpairs(PythonParser.Double_starred_kvpairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#double_starred_kvpair}.
	 * @param ctx the parse tree
	 */
	void enterDouble_starred_kvpair(PythonParser.Double_starred_kvpairContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#double_starred_kvpair}.
	 * @param ctx the parse tree
	 */
	void exitDouble_starred_kvpair(PythonParser.Double_starred_kvpairContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#kvpair}.
	 * @param ctx the parse tree
	 */
	void enterKvpair(PythonParser.KvpairContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#kvpair}.
	 * @param ctx the parse tree
	 */
	void exitKvpair(PythonParser.KvpairContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#for_if_clauses}.
	 * @param ctx the parse tree
	 */
	void enterFor_if_clauses(PythonParser.For_if_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#for_if_clauses}.
	 * @param ctx the parse tree
	 */
	void exitFor_if_clauses(PythonParser.For_if_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#for_if_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_if_clause(PythonParser.For_if_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#for_if_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_if_clause(PythonParser.For_if_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#listcomp}.
	 * @param ctx the parse tree
	 */
	void enterListcomp(PythonParser.ListcompContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#listcomp}.
	 * @param ctx the parse tree
	 */
	void exitListcomp(PythonParser.ListcompContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#setcomp}.
	 * @param ctx the parse tree
	 */
	void enterSetcomp(PythonParser.SetcompContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#setcomp}.
	 * @param ctx the parse tree
	 */
	void exitSetcomp(PythonParser.SetcompContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#genexp}.
	 * @param ctx the parse tree
	 */
	void enterGenexp(PythonParser.GenexpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#genexp}.
	 * @param ctx the parse tree
	 */
	void exitGenexp(PythonParser.GenexpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#dictcomp}.
	 * @param ctx the parse tree
	 */
	void enterDictcomp(PythonParser.DictcompContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#dictcomp}.
	 * @param ctx the parse tree
	 */
	void exitDictcomp(PythonParser.DictcompContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(PythonParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(PythonParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#args}.
	 * @param ctx the parse tree
	 */
	void enterArgs(PythonParser.ArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#args}.
	 * @param ctx the parse tree
	 */
	void exitArgs(PythonParser.ArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#kwargs}.
	 * @param ctx the parse tree
	 */
	void enterKwargs(PythonParser.KwargsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#kwargs}.
	 * @param ctx the parse tree
	 */
	void exitKwargs(PythonParser.KwargsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#starred_expression}.
	 * @param ctx the parse tree
	 */
	void enterStarred_expression(PythonParser.Starred_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#starred_expression}.
	 * @param ctx the parse tree
	 */
	void exitStarred_expression(PythonParser.Starred_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#kwarg_or_starred}.
	 * @param ctx the parse tree
	 */
	void enterKwarg_or_starred(PythonParser.Kwarg_or_starredContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#kwarg_or_starred}.
	 * @param ctx the parse tree
	 */
	void exitKwarg_or_starred(PythonParser.Kwarg_or_starredContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#kwarg_or_double_starred}.
	 * @param ctx the parse tree
	 */
	void enterKwarg_or_double_starred(PythonParser.Kwarg_or_double_starredContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#kwarg_or_double_starred}.
	 * @param ctx the parse tree
	 */
	void exitKwarg_or_double_starred(PythonParser.Kwarg_or_double_starredContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_targets}.
	 * @param ctx the parse tree
	 */
	void enterStar_targets(PythonParser.Star_targetsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_targets}.
	 * @param ctx the parse tree
	 */
	void exitStar_targets(PythonParser.Star_targetsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_targets_list_seq}.
	 * @param ctx the parse tree
	 */
	void enterStar_targets_list_seq(PythonParser.Star_targets_list_seqContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_targets_list_seq}.
	 * @param ctx the parse tree
	 */
	void exitStar_targets_list_seq(PythonParser.Star_targets_list_seqContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_targets_tuple_seq}.
	 * @param ctx the parse tree
	 */
	void enterStar_targets_tuple_seq(PythonParser.Star_targets_tuple_seqContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_targets_tuple_seq}.
	 * @param ctx the parse tree
	 */
	void exitStar_targets_tuple_seq(PythonParser.Star_targets_tuple_seqContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_target}.
	 * @param ctx the parse tree
	 */
	void enterStar_target(PythonParser.Star_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_target}.
	 * @param ctx the parse tree
	 */
	void exitStar_target(PythonParser.Star_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#target_with_star_atom}.
	 * @param ctx the parse tree
	 */
	void enterTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#target_with_star_atom}.
	 * @param ctx the parse tree
	 */
	void exitTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#star_atom}.
	 * @param ctx the parse tree
	 */
	void enterStar_atom(PythonParser.Star_atomContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#star_atom}.
	 * @param ctx the parse tree
	 */
	void exitStar_atom(PythonParser.Star_atomContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#single_target}.
	 * @param ctx the parse tree
	 */
	void enterSingle_target(PythonParser.Single_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#single_target}.
	 * @param ctx the parse tree
	 */
	void exitSingle_target(PythonParser.Single_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#single_subscript_attribute_target}.
	 * @param ctx the parse tree
	 */
	void enterSingle_subscript_attribute_target(PythonParser.Single_subscript_attribute_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#single_subscript_attribute_target}.
	 * @param ctx the parse tree
	 */
	void exitSingle_subscript_attribute_target(PythonParser.Single_subscript_attribute_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#t_primary}.
	 * @param ctx the parse tree
	 */
	void enterT_primary(PythonParser.T_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#t_primary}.
	 * @param ctx the parse tree
	 */
	void exitT_primary(PythonParser.T_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#del_targets}.
	 * @param ctx the parse tree
	 */
	void enterDel_targets(PythonParser.Del_targetsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#del_targets}.
	 * @param ctx the parse tree
	 */
	void exitDel_targets(PythonParser.Del_targetsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#del_target}.
	 * @param ctx the parse tree
	 */
	void enterDel_target(PythonParser.Del_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#del_target}.
	 * @param ctx the parse tree
	 */
	void exitDel_target(PythonParser.Del_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#del_t_atom}.
	 * @param ctx the parse tree
	 */
	void enterDel_t_atom(PythonParser.Del_t_atomContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#del_t_atom}.
	 * @param ctx the parse tree
	 */
	void exitDel_t_atom(PythonParser.Del_t_atomContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#type_expressions}.
	 * @param ctx the parse tree
	 */
	void enterType_expressions(PythonParser.Type_expressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#type_expressions}.
	 * @param ctx the parse tree
	 */
	void exitType_expressions(PythonParser.Type_expressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#func_type_comment}.
	 * @param ctx the parse tree
	 */
	void enterFunc_type_comment(PythonParser.Func_type_commentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#func_type_comment}.
	 * @param ctx the parse tree
	 */
	void exitFunc_type_comment(PythonParser.Func_type_commentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#name_except_underscore}.
	 * @param ctx the parse tree
	 */
	void enterName_except_underscore(PythonParser.Name_except_underscoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#name_except_underscore}.
	 * @param ctx the parse tree
	 */
	void exitName_except_underscore(PythonParser.Name_except_underscoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link PythonParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(PythonParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PythonParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(PythonParser.NameContext ctx);
}