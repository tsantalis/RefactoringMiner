// Generated from CSharpParser.g4 by ANTLR 4.13.2
package extension.base.lang.csharp;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CSharpParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CSharpParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CSharpParser#compilation_unit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilation_unit(CSharpParser.Compilation_unitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#namespace_or_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_or_type_name(CSharpParser.Namespace_or_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_(CSharpParser.Type_Context ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#base_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBase_type(CSharpParser.Base_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#tuple_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple_type(CSharpParser.Tuple_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#tuple_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple_element(CSharpParser.Tuple_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#simple_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_type(CSharpParser.Simple_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#numeric_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_type(CSharpParser.Numeric_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#integral_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegral_type(CSharpParser.Integral_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#floating_point_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloating_point_type(CSharpParser.Floating_point_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#class_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type(CSharpParser.Class_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_argument_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_argument_list(CSharpParser.Type_argument_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#argument_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument_list(CSharpParser.Argument_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(CSharpParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(CSharpParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#non_assignment_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_assignment_expression(CSharpParser.Non_assignment_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(CSharpParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#assignment_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_operator(CSharpParser.Assignment_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#conditional_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_expression(CSharpParser.Conditional_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#null_coalescing_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull_coalescing_expression(CSharpParser.Null_coalescing_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#conditional_or_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_or_expression(CSharpParser.Conditional_or_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#conditional_and_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_and_expression(CSharpParser.Conditional_and_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#inclusive_or_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclusive_or_expression(CSharpParser.Inclusive_or_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#exclusive_or_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusive_or_expression(CSharpParser.Exclusive_or_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#and_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnd_expression(CSharpParser.And_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#equality_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquality_expression(CSharpParser.Equality_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#relational_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_expression(CSharpParser.Relational_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#shift_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShift_expression(CSharpParser.Shift_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#additive_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditive_expression(CSharpParser.Additive_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#multiplicative_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicative_expression(CSharpParser.Multiplicative_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#switch_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_expression(CSharpParser.Switch_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#switch_expression_arms}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_expression_arms(CSharpParser.Switch_expression_armsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#switch_expression_arm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_expression_arm(CSharpParser.Switch_expression_armContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#range_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_expression(CSharpParser.Range_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#unary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_expression(CSharpParser.Unary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#cast_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast_expression(CSharpParser.Cast_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#primary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_expression(CSharpParser.Primary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code literalExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExpression(CSharpParser.LiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleNameExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleNameExpression(CSharpParser.SimpleNameExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesisExpressions}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesisExpressions(CSharpParser.ParenthesisExpressionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code memberAccessExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberAccessExpression(CSharpParser.MemberAccessExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code literalAccessExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralAccessExpression(CSharpParser.LiteralAccessExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code thisReferenceExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisReferenceExpression(CSharpParser.ThisReferenceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code baseAccessExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseAccessExpression(CSharpParser.BaseAccessExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code objectCreationExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectCreationExpression(CSharpParser.ObjectCreationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tupleExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleExpression(CSharpParser.TupleExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeofExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeofExpression(CSharpParser.TypeofExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code checkedExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheckedExpression(CSharpParser.CheckedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code uncheckedExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUncheckedExpression(CSharpParser.UncheckedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code defaultValueExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValueExpression(CSharpParser.DefaultValueExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code anonymousMethodExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymousMethodExpression(CSharpParser.AnonymousMethodExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sizeofExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSizeofExpression(CSharpParser.SizeofExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nameofExpression}
	 * labeled alternative in {@link CSharpParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameofExpression(CSharpParser.NameofExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#throwable_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowable_expression(CSharpParser.Throwable_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#throw_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_expression(CSharpParser.Throw_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#member_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMember_access(CSharpParser.Member_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#bracket_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracket_expression(CSharpParser.Bracket_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#indexer_argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexer_argument(CSharpParser.Indexer_argumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#predefined_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredefined_type(CSharpParser.Predefined_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#expression_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_list(CSharpParser.Expression_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#object_or_collection_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_or_collection_initializer(CSharpParser.Object_or_collection_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#object_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_initializer(CSharpParser.Object_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#member_initializer_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMember_initializer_list(CSharpParser.Member_initializer_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#member_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMember_initializer(CSharpParser.Member_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#initializer_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer_value(CSharpParser.Initializer_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#collection_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollection_initializer(CSharpParser.Collection_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#element_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElement_initializer(CSharpParser.Element_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#anonymous_object_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymous_object_initializer(CSharpParser.Anonymous_object_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#member_declarator_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMember_declarator_list(CSharpParser.Member_declarator_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#member_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMember_declarator(CSharpParser.Member_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#unbound_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnbound_type_name(CSharpParser.Unbound_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#generic_dimension_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_dimension_specifier(CSharpParser.Generic_dimension_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#isType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsType(CSharpParser.IsTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#isTypePatternArms}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsTypePatternArms(CSharpParser.IsTypePatternArmsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#isTypePatternArm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsTypePatternArm(CSharpParser.IsTypePatternArmContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#lambda_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_expression(CSharpParser.Lambda_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#anonymous_function_signature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymous_function_signature(CSharpParser.Anonymous_function_signatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#explicit_anonymous_function_parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicit_anonymous_function_parameter_list(CSharpParser.Explicit_anonymous_function_parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#explicit_anonymous_function_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicit_anonymous_function_parameter(CSharpParser.Explicit_anonymous_function_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#implicit_anonymous_function_parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImplicit_anonymous_function_parameter_list(CSharpParser.Implicit_anonymous_function_parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#anonymous_function_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymous_function_body(CSharpParser.Anonymous_function_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#query_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_expression(CSharpParser.Query_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#from_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_clause(CSharpParser.From_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#query_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_body(CSharpParser.Query_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#query_body_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_body_clause(CSharpParser.Query_body_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#let_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLet_clause(CSharpParser.Let_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#where_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhere_clause(CSharpParser.Where_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#combined_join_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCombined_join_clause(CSharpParser.Combined_join_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#orderby_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderby_clause(CSharpParser.Orderby_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#ordering}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrdering(CSharpParser.OrderingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#select_or_group_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_or_group_clause(CSharpParser.Select_or_group_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#query_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_continuation(CSharpParser.Query_continuationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(CSharpParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#declarationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarationStatement(CSharpParser.DeclarationStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_function_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_function_declaration(CSharpParser.Local_function_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_function_header}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_function_header(CSharpParser.Local_function_headerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_function_modifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_function_modifiers(CSharpParser.Local_function_modifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_function_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_function_body(CSharpParser.Local_function_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#labeled_Statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabeled_Statement(CSharpParser.Labeled_StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmbedded_statement(CSharpParser.Embedded_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code theEmptyStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTheEmptyStatement(CSharpParser.TheEmptyStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code expressionStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(CSharpParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ifStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(CSharpParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code switchStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchStatement(CSharpParser.SwitchStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code whileStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(CSharpParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code doStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoStatement(CSharpParser.DoStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code forStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(CSharpParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code foreachStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeachStatement(CSharpParser.ForeachStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code breakStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStatement(CSharpParser.BreakStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code continueStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(CSharpParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code gotoStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGotoStatement(CSharpParser.GotoStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(CSharpParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code throwStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowStatement(CSharpParser.ThrowStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tryStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryStatement(CSharpParser.TryStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code checkedStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheckedStatement(CSharpParser.CheckedStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code uncheckedStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUncheckedStatement(CSharpParser.UncheckedStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lockStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLockStatement(CSharpParser.LockStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code usingStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsingStatement(CSharpParser.UsingStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code yieldStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYieldStatement(CSharpParser.YieldStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unsafeStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsafeStatement(CSharpParser.UnsafeStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code fixedStatement}
	 * labeled alternative in {@link CSharpParser#simple_embedded_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFixedStatement(CSharpParser.FixedStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(CSharpParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_variable_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_variable_declaration(CSharpParser.Local_variable_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_variable_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_variable_type(CSharpParser.Local_variable_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_variable_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_variable_declarator(CSharpParser.Local_variable_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_variable_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_variable_initializer(CSharpParser.Local_variable_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#local_constant_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_constant_declaration(CSharpParser.Local_constant_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#if_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_body(CSharpParser.If_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#switch_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_section(CSharpParser.Switch_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#switch_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_label(CSharpParser.Switch_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#case_guard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_guard(CSharpParser.Case_guardContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#statement_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement_list(CSharpParser.Statement_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#for_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_initializer(CSharpParser.For_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#for_iterator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_iterator(CSharpParser.For_iteratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#catch_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatch_clauses(CSharpParser.Catch_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#specific_catch_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecific_catch_clause(CSharpParser.Specific_catch_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#general_catch_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_catch_clause(CSharpParser.General_catch_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#exception_filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_filter(CSharpParser.Exception_filterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#finally_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinally_clause(CSharpParser.Finally_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#resource_acquisition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource_acquisition(CSharpParser.Resource_acquisitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#namespace_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_declaration(CSharpParser.Namespace_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#qualified_identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualified_identifier(CSharpParser.Qualified_identifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#namespace_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_body(CSharpParser.Namespace_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#extern_alias_directives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtern_alias_directives(CSharpParser.Extern_alias_directivesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#extern_alias_directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtern_alias_directive(CSharpParser.Extern_alias_directiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#using_directives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_directives(CSharpParser.Using_directivesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code usingAliasDirective}
	 * labeled alternative in {@link CSharpParser#using_directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsingAliasDirective(CSharpParser.UsingAliasDirectiveContext ctx);
	/**
	 * Visit a parse tree produced by the {@code usingNamespaceDirective}
	 * labeled alternative in {@link CSharpParser#using_directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsingNamespaceDirective(CSharpParser.UsingNamespaceDirectiveContext ctx);
	/**
	 * Visit a parse tree produced by the {@code usingStaticDirective}
	 * labeled alternative in {@link CSharpParser#using_directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsingStaticDirective(CSharpParser.UsingStaticDirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#namespace_member_declarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_member_declarations(CSharpParser.Namespace_member_declarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#namespace_member_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_member_declaration(CSharpParser.Namespace_member_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_declaration(CSharpParser.Type_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#qualified_alias_member}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualified_alias_member(CSharpParser.Qualified_alias_memberContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_parameter_list(CSharpParser.Type_parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_parameter(CSharpParser.Type_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#class_base}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_base(CSharpParser.Class_baseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interface_type_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_type_list(CSharpParser.Interface_type_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_parameter_constraints_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_parameter_constraints_clauses(CSharpParser.Type_parameter_constraints_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_parameter_constraints_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_parameter_constraints_clause(CSharpParser.Type_parameter_constraints_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#type_parameter_constraints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_parameter_constraints(CSharpParser.Type_parameter_constraintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#primary_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_constraint(CSharpParser.Primary_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#secondary_constraints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecondary_constraints(CSharpParser.Secondary_constraintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#constructor_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_constraint(CSharpParser.Constructor_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#class_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_body(CSharpParser.Class_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#class_member_declarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_member_declarations(CSharpParser.Class_member_declarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#class_member_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_member_declaration(CSharpParser.Class_member_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#all_member_modifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_member_modifiers(CSharpParser.All_member_modifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#all_member_modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_member_modifier(CSharpParser.All_member_modifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#common_member_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommon_member_declaration(CSharpParser.Common_member_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#typed_member_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTyped_member_declaration(CSharpParser.Typed_member_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#constant_declarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant_declarators(CSharpParser.Constant_declaratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#constant_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant_declarator(CSharpParser.Constant_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#variable_declarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declarators(CSharpParser.Variable_declaratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#variable_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declarator(CSharpParser.Variable_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#variable_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_initializer(CSharpParser.Variable_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#return_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_type(CSharpParser.Return_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#member_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMember_name(CSharpParser.Member_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#method_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_body(CSharpParser.Method_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#formal_parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormal_parameter_list(CSharpParser.Formal_parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#fixed_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFixed_parameters(CSharpParser.Fixed_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#fixed_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFixed_parameter(CSharpParser.Fixed_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#parameter_modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_modifier(CSharpParser.Parameter_modifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#parameter_array}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_array(CSharpParser.Parameter_arrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#accessor_declarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccessor_declarations(CSharpParser.Accessor_declarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#get_accessor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGet_accessor_declaration(CSharpParser.Get_accessor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#set_accessor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_accessor_declaration(CSharpParser.Set_accessor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#accessor_modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccessor_modifier(CSharpParser.Accessor_modifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#accessor_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccessor_body(CSharpParser.Accessor_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#event_accessor_declarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_accessor_declarations(CSharpParser.Event_accessor_declarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#add_accessor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_accessor_declaration(CSharpParser.Add_accessor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#remove_accessor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemove_accessor_declaration(CSharpParser.Remove_accessor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#overloadable_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverloadable_operator(CSharpParser.Overloadable_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#conversion_operator_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConversion_operator_declarator(CSharpParser.Conversion_operator_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#constructor_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_initializer(CSharpParser.Constructor_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBody(CSharpParser.BodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#struct_interfaces}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_interfaces(CSharpParser.Struct_interfacesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#struct_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_body(CSharpParser.Struct_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#struct_member_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_member_declaration(CSharpParser.Struct_member_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#array_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_type(CSharpParser.Array_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#rank_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRank_specifier(CSharpParser.Rank_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#array_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_initializer(CSharpParser.Array_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#variant_type_parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariant_type_parameter_list(CSharpParser.Variant_type_parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#variant_type_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariant_type_parameter(CSharpParser.Variant_type_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#variance_annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariance_annotation(CSharpParser.Variance_annotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interface_base}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_base(CSharpParser.Interface_baseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interface_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_body(CSharpParser.Interface_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interface_member_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_member_declaration(CSharpParser.Interface_member_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interface_accessors}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_accessors(CSharpParser.Interface_accessorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#enum_base}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_base(CSharpParser.Enum_baseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#enum_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_body(CSharpParser.Enum_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#enum_member_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_member_declaration(CSharpParser.Enum_member_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#global_attribute_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_attribute_section(CSharpParser.Global_attribute_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#global_attribute_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_attribute_target(CSharpParser.Global_attribute_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#attributes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributes(CSharpParser.AttributesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#attribute_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_section(CSharpParser.Attribute_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#attribute_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_target(CSharpParser.Attribute_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#attribute_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_list(CSharpParser.Attribute_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute(CSharpParser.AttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#attribute_argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_argument(CSharpParser.Attribute_argumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#pointer_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_type(CSharpParser.Pointer_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#fixed_pointer_declarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFixed_pointer_declarators(CSharpParser.Fixed_pointer_declaratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#fixed_pointer_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFixed_pointer_declarator(CSharpParser.Fixed_pointer_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#fixed_pointer_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFixed_pointer_initializer(CSharpParser.Fixed_pointer_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#fixed_size_buffer_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFixed_size_buffer_declarator(CSharpParser.Fixed_size_buffer_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#stackalloc_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStackalloc_initializer(CSharpParser.Stackalloc_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#right_arrow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRight_arrow(CSharpParser.Right_arrowContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#right_shift}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRight_shift(CSharpParser.Right_shiftContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#right_shift_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRight_shift_assignment(CSharpParser.Right_shift_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(CSharpParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#boolean_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolean_literal(CSharpParser.Boolean_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#string_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_literal(CSharpParser.String_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interpolated_regular_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_regular_string(CSharpParser.Interpolated_regular_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interpolated_verbatium_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_verbatium_string(CSharpParser.Interpolated_verbatium_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interpolated_regular_string_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_regular_string_part(CSharpParser.Interpolated_regular_string_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interpolated_verbatium_string_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_verbatium_string_part(CSharpParser.Interpolated_verbatium_string_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interpolated_string_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_string_expression(CSharpParser.Interpolated_string_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(CSharpParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#class_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_definition(CSharpParser.Class_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#struct_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_definition(CSharpParser.Struct_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#interface_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_definition(CSharpParser.Interface_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#enum_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_definition(CSharpParser.Enum_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#delegate_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelegate_definition(CSharpParser.Delegate_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#event_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_declaration(CSharpParser.Event_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#field_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_declaration(CSharpParser.Field_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#property_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProperty_declaration(CSharpParser.Property_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#constant_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant_declaration(CSharpParser.Constant_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#indexer_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexer_declaration(CSharpParser.Indexer_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#destructor_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDestructor_definition(CSharpParser.Destructor_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#constructor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_declaration(CSharpParser.Constructor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#method_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_declaration(CSharpParser.Method_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#method_member_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_member_name(CSharpParser.Method_member_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#operator_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_declaration(CSharpParser.Operator_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#arg_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArg_declaration(CSharpParser.Arg_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#method_invocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_invocation(CSharpParser.Method_invocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#object_creation_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_creation_expression(CSharpParser.Object_creation_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CSharpParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(CSharpParser.IdentifierContext ctx);
}