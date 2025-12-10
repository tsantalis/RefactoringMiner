// Generated from CSharpPreprocessorParser.g4 by ANTLR 4.13.2
package extension.base.lang.csharp;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CSharpPreprocessorParser}.
 */
public interface CSharpPreprocessorParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by the {@code preprocessorDeclaration}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void enterPreprocessorDeclaration(CSharpPreprocessorParser.PreprocessorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preprocessorDeclaration}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void exitPreprocessorDeclaration(CSharpPreprocessorParser.PreprocessorDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code preprocessorConditional}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void enterPreprocessorConditional(CSharpPreprocessorParser.PreprocessorConditionalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preprocessorConditional}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void exitPreprocessorConditional(CSharpPreprocessorParser.PreprocessorConditionalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code preprocessorLine}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void enterPreprocessorLine(CSharpPreprocessorParser.PreprocessorLineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preprocessorLine}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void exitPreprocessorLine(CSharpPreprocessorParser.PreprocessorLineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code preprocessorDiagnostic}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void enterPreprocessorDiagnostic(CSharpPreprocessorParser.PreprocessorDiagnosticContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preprocessorDiagnostic}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void exitPreprocessorDiagnostic(CSharpPreprocessorParser.PreprocessorDiagnosticContext ctx);
	/**
	 * Enter a parse tree produced by the {@code preprocessorRegion}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void enterPreprocessorRegion(CSharpPreprocessorParser.PreprocessorRegionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preprocessorRegion}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void exitPreprocessorRegion(CSharpPreprocessorParser.PreprocessorRegionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code preprocessorPragma}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void enterPreprocessorPragma(CSharpPreprocessorParser.PreprocessorPragmaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preprocessorPragma}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void exitPreprocessorPragma(CSharpPreprocessorParser.PreprocessorPragmaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code preprocessorNullable}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void enterPreprocessorNullable(CSharpPreprocessorParser.PreprocessorNullableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preprocessorNullable}
	 * labeled alternative in {@link CSharpPreprocessorParser#preprocessor_directive}.
	 * @param ctx the parse tree
	 */
	void exitPreprocessorNullable(CSharpPreprocessorParser.PreprocessorNullableContext ctx);
	/**
	 * Enter a parse tree produced by {@link CSharpPreprocessorParser#directive_new_line_or_sharp}.
	 * @param ctx the parse tree
	 */
	void enterDirective_new_line_or_sharp(CSharpPreprocessorParser.Directive_new_line_or_sharpContext ctx);
	/**
	 * Exit a parse tree produced by {@link CSharpPreprocessorParser#directive_new_line_or_sharp}.
	 * @param ctx the parse tree
	 */
	void exitDirective_new_line_or_sharp(CSharpPreprocessorParser.Directive_new_line_or_sharpContext ctx);
	/**
	 * Enter a parse tree produced by .
	 * @param ctx the parse tree
	 */
	void enterPreprocessor_expression(CSharpPreprocessorParser.Preprocessor_expressionContext ctx);
	/**
	 * Exit a parse tree produced by .
	 * @param ctx the parse tree
	 */
	void exitPreprocessor_expression(CSharpPreprocessorParser.Preprocessor_expressionContext ctx);
}