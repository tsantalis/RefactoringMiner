// Generated from CSharpParser.g4 by ANTLR 4.13.2
package extension.base.lang.csharp;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class CSharpParser extends CSharpParserBase {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		BYTE_ORDER_MARK=1, SINGLE_LINE_DOC_COMMENT=2, EMPTY_DELIMITED_DOC_COMMENT=3, 
		DELIMITED_DOC_COMMENT=4, SINGLE_LINE_COMMENT=5, DELIMITED_COMMENT=6, WHITESPACES=7, 
		SHARP=8, ABSTRACT=9, ADD=10, ALIAS=11, ARGLIST=12, AS=13, ASCENDING=14, 
		ASYNC=15, AWAIT=16, BASE=17, BOOL=18, BREAK=19, BY=20, BYTE=21, CASE=22, 
		CATCH=23, CHAR=24, CHECKED=25, CLASS=26, CONST=27, CONTINUE=28, DECIMAL=29, 
		DEFAULT=30, DELEGATE=31, DESCENDING=32, DO=33, DOUBLE=34, DYNAMIC=35, 
		ELSE=36, ENUM=37, EQUALS=38, EVENT=39, EXPLICIT=40, EXTERN=41, FALSE=42, 
		FINALLY=43, FIXED=44, FLOAT=45, FOR=46, FOREACH=47, FROM=48, GET=49, GOTO=50, 
		GROUP=51, IF=52, IMPLICIT=53, IN=54, INT=55, INTERFACE=56, INTERNAL=57, 
		INTO=58, IS=59, JOIN=60, LET=61, LOCK=62, LONG=63, NAMEOF=64, NAMESPACE=65, 
		NEW=66, NULL_=67, OBJECT=68, ON=69, OPERATOR=70, ORDERBY=71, OUT=72, OVERRIDE=73, 
		PARAMS=74, PARTIAL=75, PRIVATE=76, PROTECTED=77, PUBLIC=78, READONLY=79, 
		REF=80, REMOVE=81, RETURN=82, SBYTE=83, SEALED=84, SELECT=85, SET=86, 
		SHORT=87, SIZEOF=88, STACKALLOC=89, STATIC=90, STRING=91, STRUCT=92, SWITCH=93, 
		THIS=94, THROW=95, TRUE=96, TRY=97, TYPEOF=98, UINT=99, ULONG=100, UNCHECKED=101, 
		UNMANAGED=102, UNSAFE=103, USHORT=104, USING=105, VAR=106, VIRTUAL=107, 
		VOID=108, VOLATILE=109, WHEN=110, WHERE=111, WHILE=112, YIELD=113, IDENTIFIER=114, 
		LITERAL_ACCESS=115, INTEGER_LITERAL=116, HEX_INTEGER_LITERAL=117, BIN_INTEGER_LITERAL=118, 
		REAL_LITERAL=119, CHARACTER_LITERAL=120, REGULAR_STRING=121, VERBATIUM_STRING=122, 
		INTERPOLATED_REGULAR_STRING_START=123, INTERPOLATED_VERBATIUM_STRING_START=124, 
		OPEN_BRACE=125, CLOSE_BRACE=126, OPEN_BRACKET=127, CLOSE_BRACKET=128, 
		OPEN_PARENS=129, CLOSE_PARENS=130, DOT=131, COMMA=132, COLON=133, SEMICOLON=134, 
		PLUS=135, MINUS=136, STAR=137, DIV=138, PERCENT=139, AMP=140, BITWISE_OR=141, 
		CARET=142, BANG=143, TILDE=144, ASSIGNMENT=145, LT=146, GT=147, INTERR=148, 
		DOUBLE_COLON=149, OP_COALESCING=150, OP_INC=151, OP_DEC=152, OP_AND=153, 
		OP_OR=154, OP_PTR=155, OP_EQ=156, OP_NE=157, OP_LE=158, OP_GE=159, OP_ADD_ASSIGNMENT=160, 
		OP_SUB_ASSIGNMENT=161, OP_MULT_ASSIGNMENT=162, OP_DIV_ASSIGNMENT=163, 
		OP_MOD_ASSIGNMENT=164, OP_AND_ASSIGNMENT=165, OP_OR_ASSIGNMENT=166, OP_XOR_ASSIGNMENT=167, 
		OP_LEFT_SHIFT=168, OP_LEFT_SHIFT_ASSIGNMENT=169, OP_COALESCING_ASSIGNMENT=170, 
		OP_RANGE=171, DOUBLE_CURLY_INSIDE=172, OPEN_BRACE_INSIDE=173, REGULAR_CHAR_INSIDE=174, 
		VERBATIUM_DOUBLE_QUOTE_INSIDE=175, DOUBLE_QUOTE_INSIDE=176, REGULAR_STRING_INSIDE=177, 
		VERBATIUM_INSIDE_STRING=178, CLOSE_BRACE_INSIDE=179, FORMAT_STRING=180, 
		DIRECTIVE_WHITESPACES=181, DIGITS=182, DEFINE=183, UNDEF=184, ELIF=185, 
		ENDIF=186, LINE=187, ERROR=188, WARNING=189, REGION=190, ENDREGION=191, 
		PRAGMA=192, NULLABLE=193, DIRECTIVE_HIDDEN=194, CONDITIONAL_SYMBOL=195, 
		DIRECTIVE_NEW_LINE=196, TEXT=197, DOUBLE_CURLY_CLOSE_INSIDE=198;
	public static final int
		RULE_compilation_unit = 0, RULE_namespace_or_type_name = 1, RULE_type_ = 2, 
		RULE_base_type = 3, RULE_tuple_type = 4, RULE_tuple_element = 5, RULE_simple_type = 6, 
		RULE_numeric_type = 7, RULE_integral_type = 8, RULE_floating_point_type = 9, 
		RULE_class_type = 10, RULE_type_argument_list = 11, RULE_argument_list = 12, 
		RULE_argument = 13, RULE_expression = 14, RULE_non_assignment_expression = 15, 
		RULE_assignment = 16, RULE_assignment_operator = 17, RULE_conditional_expression = 18, 
		RULE_null_coalescing_expression = 19, RULE_conditional_or_expression = 20, 
		RULE_conditional_and_expression = 21, RULE_inclusive_or_expression = 22, 
		RULE_exclusive_or_expression = 23, RULE_and_expression = 24, RULE_equality_expression = 25, 
		RULE_relational_expression = 26, RULE_shift_expression = 27, RULE_additive_expression = 28, 
		RULE_multiplicative_expression = 29, RULE_switch_expression = 30, RULE_switch_expression_arms = 31, 
		RULE_switch_expression_arm = 32, RULE_range_expression = 33, RULE_unary_expression = 34, 
		RULE_cast_expression = 35, RULE_primary_expression = 36, RULE_primary_expression_start = 37, 
		RULE_throwable_expression = 38, RULE_throw_expression = 39, RULE_member_access = 40, 
		RULE_bracket_expression = 41, RULE_indexer_argument = 42, RULE_predefined_type = 43, 
		RULE_expression_list = 44, RULE_object_or_collection_initializer = 45, 
		RULE_object_initializer = 46, RULE_member_initializer_list = 47, RULE_member_initializer = 48, 
		RULE_initializer_value = 49, RULE_collection_initializer = 50, RULE_element_initializer = 51, 
		RULE_anonymous_object_initializer = 52, RULE_member_declarator_list = 53, 
		RULE_member_declarator = 54, RULE_unbound_type_name = 55, RULE_generic_dimension_specifier = 56, 
		RULE_isType = 57, RULE_isTypePatternArms = 58, RULE_isTypePatternArm = 59, 
		RULE_lambda_expression = 60, RULE_anonymous_function_signature = 61, RULE_explicit_anonymous_function_parameter_list = 62, 
		RULE_explicit_anonymous_function_parameter = 63, RULE_implicit_anonymous_function_parameter_list = 64, 
		RULE_anonymous_function_body = 65, RULE_query_expression = 66, RULE_from_clause = 67, 
		RULE_query_body = 68, RULE_query_body_clause = 69, RULE_let_clause = 70, 
		RULE_where_clause = 71, RULE_combined_join_clause = 72, RULE_orderby_clause = 73, 
		RULE_ordering = 74, RULE_select_or_group_clause = 75, RULE_query_continuation = 76, 
		RULE_statement = 77, RULE_declarationStatement = 78, RULE_local_function_declaration = 79, 
		RULE_local_function_header = 80, RULE_local_function_modifiers = 81, RULE_local_function_body = 82, 
		RULE_labeled_Statement = 83, RULE_embedded_statement = 84, RULE_simple_embedded_statement = 85, 
		RULE_block = 86, RULE_local_variable_declaration = 87, RULE_local_variable_type = 88, 
		RULE_local_variable_declarator = 89, RULE_local_variable_initializer = 90, 
		RULE_local_constant_declaration = 91, RULE_if_body = 92, RULE_switch_section = 93, 
		RULE_switch_label = 94, RULE_case_guard = 95, RULE_statement_list = 96, 
		RULE_for_initializer = 97, RULE_for_iterator = 98, RULE_catch_clauses = 99, 
		RULE_specific_catch_clause = 100, RULE_general_catch_clause = 101, RULE_exception_filter = 102, 
		RULE_finally_clause = 103, RULE_resource_acquisition = 104, RULE_namespace_declaration = 105, 
		RULE_qualified_identifier = 106, RULE_namespace_body = 107, RULE_extern_alias_directives = 108, 
		RULE_extern_alias_directive = 109, RULE_using_directives = 110, RULE_using_directive = 111, 
		RULE_namespace_member_declarations = 112, RULE_namespace_member_declaration = 113, 
		RULE_type_declaration = 114, RULE_qualified_alias_member = 115, RULE_type_parameter_list = 116, 
		RULE_type_parameter = 117, RULE_class_base = 118, RULE_interface_type_list = 119, 
		RULE_type_parameter_constraints_clauses = 120, RULE_type_parameter_constraints_clause = 121, 
		RULE_type_parameter_constraints = 122, RULE_primary_constraint = 123, 
		RULE_secondary_constraints = 124, RULE_constructor_constraint = 125, RULE_class_body = 126, 
		RULE_class_member_declarations = 127, RULE_class_member_declaration = 128, 
		RULE_all_member_modifiers = 129, RULE_all_member_modifier = 130, RULE_common_member_declaration = 131, 
		RULE_typed_member_declaration = 132, RULE_constant_declarators = 133, 
		RULE_constant_declarator = 134, RULE_variable_declarators = 135, RULE_variable_declarator = 136, 
		RULE_variable_initializer = 137, RULE_return_type = 138, RULE_member_name = 139, 
		RULE_method_body = 140, RULE_formal_parameter_list = 141, RULE_fixed_parameters = 142, 
		RULE_fixed_parameter = 143, RULE_parameter_modifier = 144, RULE_parameter_array = 145, 
		RULE_accessor_declarations = 146, RULE_get_accessor_declaration = 147, 
		RULE_set_accessor_declaration = 148, RULE_accessor_modifier = 149, RULE_accessor_body = 150, 
		RULE_event_accessor_declarations = 151, RULE_add_accessor_declaration = 152, 
		RULE_remove_accessor_declaration = 153, RULE_overloadable_operator = 154, 
		RULE_conversion_operator_declarator = 155, RULE_constructor_initializer = 156, 
		RULE_body = 157, RULE_struct_interfaces = 158, RULE_struct_body = 159, 
		RULE_struct_member_declaration = 160, RULE_array_type = 161, RULE_rank_specifier = 162, 
		RULE_array_initializer = 163, RULE_variant_type_parameter_list = 164, 
		RULE_variant_type_parameter = 165, RULE_variance_annotation = 166, RULE_interface_base = 167, 
		RULE_interface_body = 168, RULE_interface_member_declaration = 169, RULE_interface_accessors = 170, 
		RULE_enum_base = 171, RULE_enum_body = 172, RULE_enum_member_declaration = 173, 
		RULE_global_attribute_section = 174, RULE_global_attribute_target = 175, 
		RULE_attributes = 176, RULE_attribute_section = 177, RULE_attribute_target = 178, 
		RULE_attribute_list = 179, RULE_attribute = 180, RULE_attribute_argument = 181, 
		RULE_pointer_type = 182, RULE_fixed_pointer_declarators = 183, RULE_fixed_pointer_declarator = 184, 
		RULE_fixed_pointer_initializer = 185, RULE_fixed_size_buffer_declarator = 186, 
		RULE_stackalloc_initializer = 187, RULE_right_arrow = 188, RULE_right_shift = 189, 
		RULE_right_shift_assignment = 190, RULE_literal = 191, RULE_boolean_literal = 192, 
		RULE_string_literal = 193, RULE_interpolated_regular_string = 194, RULE_interpolated_verbatium_string = 195, 
		RULE_interpolated_regular_string_part = 196, RULE_interpolated_verbatium_string_part = 197, 
		RULE_interpolated_string_expression = 198, RULE_keyword = 199, RULE_class_definition = 200, 
		RULE_struct_definition = 201, RULE_interface_definition = 202, RULE_enum_definition = 203, 
		RULE_delegate_definition = 204, RULE_event_declaration = 205, RULE_field_declaration = 206, 
		RULE_property_declaration = 207, RULE_constant_declaration = 208, RULE_indexer_declaration = 209, 
		RULE_destructor_definition = 210, RULE_constructor_declaration = 211, 
		RULE_method_declaration = 212, RULE_method_member_name = 213, RULE_operator_declaration = 214, 
		RULE_arg_declaration = 215, RULE_method_invocation = 216, RULE_object_creation_expression = 217, 
		RULE_identifier = 218;
	private static String[] makeRuleNames() {
		return new String[] {
			"compilation_unit", "namespace_or_type_name", "type_", "base_type", "tuple_type", 
			"tuple_element", "simple_type", "numeric_type", "integral_type", "floating_point_type", 
			"class_type", "type_argument_list", "argument_list", "argument", "expression", 
			"non_assignment_expression", "assignment", "assignment_operator", "conditional_expression", 
			"null_coalescing_expression", "conditional_or_expression", "conditional_and_expression", 
			"inclusive_or_expression", "exclusive_or_expression", "and_expression", 
			"equality_expression", "relational_expression", "shift_expression", "additive_expression", 
			"multiplicative_expression", "switch_expression", "switch_expression_arms", 
			"switch_expression_arm", "range_expression", "unary_expression", "cast_expression", 
			"primary_expression", "primary_expression_start", "throwable_expression", 
			"throw_expression", "member_access", "bracket_expression", "indexer_argument", 
			"predefined_type", "expression_list", "object_or_collection_initializer", 
			"object_initializer", "member_initializer_list", "member_initializer", 
			"initializer_value", "collection_initializer", "element_initializer", 
			"anonymous_object_initializer", "member_declarator_list", "member_declarator", 
			"unbound_type_name", "generic_dimension_specifier", "isType", "isTypePatternArms", 
			"isTypePatternArm", "lambda_expression", "anonymous_function_signature", 
			"explicit_anonymous_function_parameter_list", "explicit_anonymous_function_parameter", 
			"implicit_anonymous_function_parameter_list", "anonymous_function_body", 
			"query_expression", "from_clause", "query_body", "query_body_clause", 
			"let_clause", "where_clause", "combined_join_clause", "orderby_clause", 
			"ordering", "select_or_group_clause", "query_continuation", "statement", 
			"declarationStatement", "local_function_declaration", "local_function_header", 
			"local_function_modifiers", "local_function_body", "labeled_Statement", 
			"embedded_statement", "simple_embedded_statement", "block", "local_variable_declaration", 
			"local_variable_type", "local_variable_declarator", "local_variable_initializer", 
			"local_constant_declaration", "if_body", "switch_section", "switch_label", 
			"case_guard", "statement_list", "for_initializer", "for_iterator", "catch_clauses", 
			"specific_catch_clause", "general_catch_clause", "exception_filter", 
			"finally_clause", "resource_acquisition", "namespace_declaration", "qualified_identifier", 
			"namespace_body", "extern_alias_directives", "extern_alias_directive", 
			"using_directives", "using_directive", "namespace_member_declarations", 
			"namespace_member_declaration", "type_declaration", "qualified_alias_member", 
			"type_parameter_list", "type_parameter", "class_base", "interface_type_list", 
			"type_parameter_constraints_clauses", "type_parameter_constraints_clause", 
			"type_parameter_constraints", "primary_constraint", "secondary_constraints", 
			"constructor_constraint", "class_body", "class_member_declarations", 
			"class_member_declaration", "all_member_modifiers", "all_member_modifier", 
			"common_member_declaration", "typed_member_declaration", "constant_declarators", 
			"constant_declarator", "variable_declarators", "variable_declarator", 
			"variable_initializer", "return_type", "member_name", "method_body", 
			"formal_parameter_list", "fixed_parameters", "fixed_parameter", "parameter_modifier", 
			"parameter_array", "accessor_declarations", "get_accessor_declaration", 
			"set_accessor_declaration", "accessor_modifier", "accessor_body", "event_accessor_declarations", 
			"add_accessor_declaration", "remove_accessor_declaration", "overloadable_operator", 
			"conversion_operator_declarator", "constructor_initializer", "body", 
			"struct_interfaces", "struct_body", "struct_member_declaration", "array_type", 
			"rank_specifier", "array_initializer", "variant_type_parameter_list", 
			"variant_type_parameter", "variance_annotation", "interface_base", "interface_body", 
			"interface_member_declaration", "interface_accessors", "enum_base", "enum_body", 
			"enum_member_declaration", "global_attribute_section", "global_attribute_target", 
			"attributes", "attribute_section", "attribute_target", "attribute_list", 
			"attribute", "attribute_argument", "pointer_type", "fixed_pointer_declarators", 
			"fixed_pointer_declarator", "fixed_pointer_initializer", "fixed_size_buffer_declarator", 
			"stackalloc_initializer", "right_arrow", "right_shift", "right_shift_assignment", 
			"literal", "boolean_literal", "string_literal", "interpolated_regular_string", 
			"interpolated_verbatium_string", "interpolated_regular_string_part", 
			"interpolated_verbatium_string_part", "interpolated_string_expression", 
			"keyword", "class_definition", "struct_definition", "interface_definition", 
			"enum_definition", "delegate_definition", "event_declaration", "field_declaration", 
			"property_declaration", "constant_declaration", "indexer_declaration", 
			"destructor_definition", "constructor_declaration", "method_declaration", 
			"method_member_name", "operator_declaration", "arg_declaration", "method_invocation", 
			"object_creation_expression", "identifier"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'\\u00EF\\u00BB\\u00BF'", null, "'/***/'", null, null, null, null, 
			"'#'", "'abstract'", "'add'", "'alias'", "'__arglist'", "'as'", "'ascending'", 
			"'async'", "'await'", "'base'", "'bool'", "'break'", "'by'", "'byte'", 
			"'case'", "'catch'", "'char'", "'checked'", "'class'", "'const'", "'continue'", 
			"'decimal'", "'default'", "'delegate'", "'descending'", "'do'", "'double'", 
			"'dynamic'", "'else'", "'enum'", "'equals'", "'event'", "'explicit'", 
			"'extern'", "'false'", "'finally'", "'fixed'", "'float'", "'for'", "'foreach'", 
			"'from'", "'get'", "'goto'", "'group'", "'if'", "'implicit'", "'in'", 
			"'int'", "'interface'", "'internal'", "'into'", "'is'", "'join'", "'let'", 
			"'lock'", "'long'", "'nameof'", "'namespace'", "'new'", "'null'", "'object'", 
			"'on'", "'operator'", "'orderby'", "'out'", "'override'", "'params'", 
			"'partial'", "'private'", "'protected'", "'public'", "'readonly'", "'ref'", 
			"'remove'", "'return'", "'sbyte'", "'sealed'", "'select'", "'set'", "'short'", 
			"'sizeof'", "'stackalloc'", "'static'", "'string'", "'struct'", "'switch'", 
			"'this'", "'throw'", "'true'", "'try'", "'typeof'", "'uint'", "'ulong'", 
			"'unchecked'", "'unmanaged'", "'unsafe'", "'ushort'", "'using'", "'var'", 
			"'virtual'", "'void'", "'volatile'", "'when'", "'where'", "'while'", 
			"'yield'", null, null, null, null, null, null, null, null, null, null, 
			null, "'{'", "'}'", "'['", "']'", "'('", "')'", "'.'", "','", "':'", 
			"';'", "'+'", "'-'", "'*'", "'/'", "'%'", "'&'", "'|'", "'^'", "'!'", 
			"'~'", "'='", "'<'", "'>'", "'?'", "'::'", "'??'", "'++'", "'--'", "'&&'", 
			"'||'", "'->'", "'=='", "'!='", "'<='", "'>='", "'+='", "'-='", "'*='", 
			"'/='", "'%='", "'&='", "'|='", "'^='", "'<<'", "'<<='", "'??='", "'..'", 
			"'{{'", null, null, null, null, null, null, null, null, null, null, "'define'", 
			"'undef'", "'elif'", "'endif'", "'line'", null, null, null, null, null, 
			null, "'hidden'", null, null, null, "'}}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "BYTE_ORDER_MARK", "SINGLE_LINE_DOC_COMMENT", "EMPTY_DELIMITED_DOC_COMMENT", 
			"DELIMITED_DOC_COMMENT", "SINGLE_LINE_COMMENT", "DELIMITED_COMMENT", 
			"WHITESPACES", "SHARP", "ABSTRACT", "ADD", "ALIAS", "ARGLIST", "AS", 
			"ASCENDING", "ASYNC", "AWAIT", "BASE", "BOOL", "BREAK", "BY", "BYTE", 
			"CASE", "CATCH", "CHAR", "CHECKED", "CLASS", "CONST", "CONTINUE", "DECIMAL", 
			"DEFAULT", "DELEGATE", "DESCENDING", "DO", "DOUBLE", "DYNAMIC", "ELSE", 
			"ENUM", "EQUALS", "EVENT", "EXPLICIT", "EXTERN", "FALSE", "FINALLY", 
			"FIXED", "FLOAT", "FOR", "FOREACH", "FROM", "GET", "GOTO", "GROUP", "IF", 
			"IMPLICIT", "IN", "INT", "INTERFACE", "INTERNAL", "INTO", "IS", "JOIN", 
			"LET", "LOCK", "LONG", "NAMEOF", "NAMESPACE", "NEW", "NULL_", "OBJECT", 
			"ON", "OPERATOR", "ORDERBY", "OUT", "OVERRIDE", "PARAMS", "PARTIAL", 
			"PRIVATE", "PROTECTED", "PUBLIC", "READONLY", "REF", "REMOVE", "RETURN", 
			"SBYTE", "SEALED", "SELECT", "SET", "SHORT", "SIZEOF", "STACKALLOC", 
			"STATIC", "STRING", "STRUCT", "SWITCH", "THIS", "THROW", "TRUE", "TRY", 
			"TYPEOF", "UINT", "ULONG", "UNCHECKED", "UNMANAGED", "UNSAFE", "USHORT", 
			"USING", "VAR", "VIRTUAL", "VOID", "VOLATILE", "WHEN", "WHERE", "WHILE", 
			"YIELD", "IDENTIFIER", "LITERAL_ACCESS", "INTEGER_LITERAL", "HEX_INTEGER_LITERAL", 
			"BIN_INTEGER_LITERAL", "REAL_LITERAL", "CHARACTER_LITERAL", "REGULAR_STRING", 
			"VERBATIUM_STRING", "INTERPOLATED_REGULAR_STRING_START", "INTERPOLATED_VERBATIUM_STRING_START", 
			"OPEN_BRACE", "CLOSE_BRACE", "OPEN_BRACKET", "CLOSE_BRACKET", "OPEN_PARENS", 
			"CLOSE_PARENS", "DOT", "COMMA", "COLON", "SEMICOLON", "PLUS", "MINUS", 
			"STAR", "DIV", "PERCENT", "AMP", "BITWISE_OR", "CARET", "BANG", "TILDE", 
			"ASSIGNMENT", "LT", "GT", "INTERR", "DOUBLE_COLON", "OP_COALESCING", 
			"OP_INC", "OP_DEC", "OP_AND", "OP_OR", "OP_PTR", "OP_EQ", "OP_NE", "OP_LE", 
			"OP_GE", "OP_ADD_ASSIGNMENT", "OP_SUB_ASSIGNMENT", "OP_MULT_ASSIGNMENT", 
			"OP_DIV_ASSIGNMENT", "OP_MOD_ASSIGNMENT", "OP_AND_ASSIGNMENT", "OP_OR_ASSIGNMENT", 
			"OP_XOR_ASSIGNMENT", "OP_LEFT_SHIFT", "OP_LEFT_SHIFT_ASSIGNMENT", "OP_COALESCING_ASSIGNMENT", 
			"OP_RANGE", "DOUBLE_CURLY_INSIDE", "OPEN_BRACE_INSIDE", "REGULAR_CHAR_INSIDE", 
			"VERBATIUM_DOUBLE_QUOTE_INSIDE", "DOUBLE_QUOTE_INSIDE", "REGULAR_STRING_INSIDE", 
			"VERBATIUM_INSIDE_STRING", "CLOSE_BRACE_INSIDE", "FORMAT_STRING", "DIRECTIVE_WHITESPACES", 
			"DIGITS", "DEFINE", "UNDEF", "ELIF", "ENDIF", "LINE", "ERROR", "WARNING", 
			"REGION", "ENDREGION", "PRAGMA", "NULLABLE", "DIRECTIVE_HIDDEN", "CONDITIONAL_SYMBOL", 
			"DIRECTIVE_NEW_LINE", "TEXT", "DOUBLE_CURLY_CLOSE_INSIDE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "CSharpParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public CSharpParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Compilation_unitContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(CSharpParser.EOF, 0); }
		public TerminalNode BYTE_ORDER_MARK() { return getToken(CSharpParser.BYTE_ORDER_MARK, 0); }
		public Extern_alias_directivesContext extern_alias_directives() {
			return getRuleContext(Extern_alias_directivesContext.class,0);
		}
		public Using_directivesContext using_directives() {
			return getRuleContext(Using_directivesContext.class,0);
		}
		public List<Global_attribute_sectionContext> global_attribute_section() {
			return getRuleContexts(Global_attribute_sectionContext.class);
		}
		public Global_attribute_sectionContext global_attribute_section(int i) {
			return getRuleContext(Global_attribute_sectionContext.class,i);
		}
		public Namespace_member_declarationsContext namespace_member_declarations() {
			return getRuleContext(Namespace_member_declarationsContext.class,0);
		}
		public Compilation_unitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compilation_unit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCompilation_unit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCompilation_unit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCompilation_unit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Compilation_unitContext compilation_unit() throws RecognitionException {
		Compilation_unitContext _localctx = new Compilation_unitContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_compilation_unit);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(439);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==BYTE_ORDER_MARK) {
				{
				setState(438);
				match(BYTE_ORDER_MARK);
				}
			}

			setState(442);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(441);
				extern_alias_directives();
				}
				break;
			}
			setState(445);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==USING) {
				{
				setState(444);
				using_directives();
				}
			}

			setState(450);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(447);
					global_attribute_section();
					}
					} 
				}
				setState(452);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			}
			setState(454);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 216175120790618624L) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & 4611708283706211587L) != 0)) {
				{
				setState(453);
				namespace_member_declarations();
				}
			}

			setState(456);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Namespace_or_type_nameContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public Qualified_alias_memberContext qualified_alias_member() {
			return getRuleContext(Qualified_alias_memberContext.class,0);
		}
		public List<TerminalNode> DOT() { return getTokens(CSharpParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(CSharpParser.DOT, i);
		}
		public List<Type_argument_listContext> type_argument_list() {
			return getRuleContexts(Type_argument_listContext.class);
		}
		public Type_argument_listContext type_argument_list(int i) {
			return getRuleContext(Type_argument_listContext.class,i);
		}
		public Namespace_or_type_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace_or_type_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNamespace_or_type_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNamespace_or_type_name(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNamespace_or_type_name(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Namespace_or_type_nameContext namespace_or_type_name() throws RecognitionException {
		Namespace_or_type_nameContext _localctx = new Namespace_or_type_nameContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_namespace_or_type_name);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(463);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				{
				setState(458);
				identifier();
				setState(460);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
				case 1:
					{
					setState(459);
					type_argument_list();
					}
					break;
				}
				}
				break;
			case 2:
				{
				setState(462);
				qualified_alias_member();
				}
				break;
			}
			setState(472);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(465);
					match(DOT);
					setState(466);
					identifier();
					setState(468);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
					case 1:
						{
						setState(467);
						type_argument_list();
						}
						break;
					}
					}
					} 
				}
				setState(474);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_Context extends ParserRuleContext {
		public Base_typeContext base_type() {
			return getRuleContext(Base_typeContext.class,0);
		}
		public List<TerminalNode> INTERR() { return getTokens(CSharpParser.INTERR); }
		public TerminalNode INTERR(int i) {
			return getToken(CSharpParser.INTERR, i);
		}
		public List<Rank_specifierContext> rank_specifier() {
			return getRuleContexts(Rank_specifierContext.class);
		}
		public Rank_specifierContext rank_specifier(int i) {
			return getRuleContext(Rank_specifierContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(CSharpParser.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(CSharpParser.STAR, i);
		}
		public Type_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_Context type_() throws RecognitionException {
		Type_Context _localctx = new Type_Context(_ctx, getState());
		enterRule(_localctx, 4, RULE_type_);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(475);
			base_type();
			setState(481);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(479);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case INTERR:
						{
						setState(476);
						match(INTERR);
						}
						break;
					case OPEN_BRACKET:
						{
						setState(477);
						rank_specifier();
						}
						break;
					case STAR:
						{
						setState(478);
						match(STAR);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(483);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Base_typeContext extends ParserRuleContext {
		public Simple_typeContext simple_type() {
			return getRuleContext(Simple_typeContext.class,0);
		}
		public Class_typeContext class_type() {
			return getRuleContext(Class_typeContext.class,0);
		}
		public TerminalNode VOID() { return getToken(CSharpParser.VOID, 0); }
		public TerminalNode STAR() { return getToken(CSharpParser.STAR, 0); }
		public Tuple_typeContext tuple_type() {
			return getRuleContext(Tuple_typeContext.class,0);
		}
		public Base_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_base_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterBase_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitBase_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitBase_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Base_typeContext base_type() throws RecognitionException {
		Base_typeContext _localctx = new Base_typeContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_base_type);
		try {
			setState(489);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOL:
			case BYTE:
			case CHAR:
			case DECIMAL:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SBYTE:
			case SHORT:
			case UINT:
			case ULONG:
			case USHORT:
				enterOuterAlt(_localctx, 1);
				{
				setState(484);
				simple_type();
				}
				break;
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BY:
			case DESCENDING:
			case DYNAMIC:
			case EQUALS:
			case FROM:
			case GET:
			case GROUP:
			case INTO:
			case JOIN:
			case LET:
			case NAMEOF:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REMOVE:
			case SELECT:
			case SET:
			case STRING:
			case UNMANAGED:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(485);
				class_type();
				}
				break;
			case VOID:
				enterOuterAlt(_localctx, 3);
				{
				setState(486);
				match(VOID);
				setState(487);
				match(STAR);
				}
				break;
			case OPEN_PARENS:
				enterOuterAlt(_localctx, 4);
				{
				setState(488);
				tuple_type();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tuple_typeContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public List<Tuple_elementContext> tuple_element() {
			return getRuleContexts(Tuple_elementContext.class);
		}
		public Tuple_elementContext tuple_element(int i) {
			return getRuleContext(Tuple_elementContext.class,i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Tuple_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tuple_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterTuple_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitTuple_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitTuple_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tuple_typeContext tuple_type() throws RecognitionException {
		Tuple_typeContext _localctx = new Tuple_typeContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_tuple_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(491);
			match(OPEN_PARENS);
			setState(492);
			tuple_element();
			setState(495); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(493);
				match(COMMA);
				setState(494);
				tuple_element();
				}
				}
				setState(497); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==COMMA );
			setState(499);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tuple_elementContext extends ParserRuleContext {
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Tuple_elementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tuple_element; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterTuple_element(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitTuple_element(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitTuple_element(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tuple_elementContext tuple_element() throws RecognitionException {
		Tuple_elementContext _localctx = new Tuple_elementContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_tuple_element);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(501);
			type_();
			setState(503);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 3750091428249852928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 1904629023639713L) != 0)) {
				{
				setState(502);
				identifier();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Simple_typeContext extends ParserRuleContext {
		public Numeric_typeContext numeric_type() {
			return getRuleContext(Numeric_typeContext.class,0);
		}
		public TerminalNode BOOL() { return getToken(CSharpParser.BOOL, 0); }
		public Simple_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simple_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSimple_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSimple_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSimple_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Simple_typeContext simple_type() throws RecognitionException {
		Simple_typeContext _localctx = new Simple_typeContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_simple_type);
		try {
			setState(507);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BYTE:
			case CHAR:
			case DECIMAL:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SBYTE:
			case SHORT:
			case UINT:
			case ULONG:
			case USHORT:
				enterOuterAlt(_localctx, 1);
				{
				setState(505);
				numeric_type();
				}
				break;
			case BOOL:
				enterOuterAlt(_localctx, 2);
				{
				setState(506);
				match(BOOL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Numeric_typeContext extends ParserRuleContext {
		public Integral_typeContext integral_type() {
			return getRuleContext(Integral_typeContext.class,0);
		}
		public Floating_point_typeContext floating_point_type() {
			return getRuleContext(Floating_point_typeContext.class,0);
		}
		public TerminalNode DECIMAL() { return getToken(CSharpParser.DECIMAL, 0); }
		public Numeric_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numeric_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNumeric_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNumeric_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNumeric_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Numeric_typeContext numeric_type() throws RecognitionException {
		Numeric_typeContext _localctx = new Numeric_typeContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_numeric_type);
		try {
			setState(512);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BYTE:
			case CHAR:
			case INT:
			case LONG:
			case SBYTE:
			case SHORT:
			case UINT:
			case ULONG:
			case USHORT:
				enterOuterAlt(_localctx, 1);
				{
				setState(509);
				integral_type();
				}
				break;
			case DOUBLE:
			case FLOAT:
				enterOuterAlt(_localctx, 2);
				{
				setState(510);
				floating_point_type();
				}
				break;
			case DECIMAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(511);
				match(DECIMAL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Integral_typeContext extends ParserRuleContext {
		public TerminalNode SBYTE() { return getToken(CSharpParser.SBYTE, 0); }
		public TerminalNode BYTE() { return getToken(CSharpParser.BYTE, 0); }
		public TerminalNode SHORT() { return getToken(CSharpParser.SHORT, 0); }
		public TerminalNode USHORT() { return getToken(CSharpParser.USHORT, 0); }
		public TerminalNode INT() { return getToken(CSharpParser.INT, 0); }
		public TerminalNode UINT() { return getToken(CSharpParser.UINT, 0); }
		public TerminalNode LONG() { return getToken(CSharpParser.LONG, 0); }
		public TerminalNode ULONG() { return getToken(CSharpParser.ULONG, 0); }
		public TerminalNode CHAR() { return getToken(CSharpParser.CHAR, 0); }
		public Integral_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_integral_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIntegral_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIntegral_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIntegral_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Integral_typeContext integral_type() throws RecognitionException {
		Integral_typeContext _localctx = new Integral_typeContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_integral_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(514);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & -9187343239816937472L) != 0) || ((((_la - 83)) & ~0x3f) == 0 && ((1L << (_la - 83)) & 2293777L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Floating_point_typeContext extends ParserRuleContext {
		public TerminalNode FLOAT() { return getToken(CSharpParser.FLOAT, 0); }
		public TerminalNode DOUBLE() { return getToken(CSharpParser.DOUBLE, 0); }
		public Floating_point_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floating_point_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFloating_point_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFloating_point_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFloating_point_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Floating_point_typeContext floating_point_type() throws RecognitionException {
		Floating_point_typeContext _localctx = new Floating_point_typeContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_floating_point_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(516);
			_la = _input.LA(1);
			if ( !(_la==DOUBLE || _la==FLOAT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_typeContext extends ParserRuleContext {
		public Namespace_or_type_nameContext namespace_or_type_name() {
			return getRuleContext(Namespace_or_type_nameContext.class,0);
		}
		public TerminalNode OBJECT() { return getToken(CSharpParser.OBJECT, 0); }
		public TerminalNode DYNAMIC() { return getToken(CSharpParser.DYNAMIC, 0); }
		public TerminalNode STRING() { return getToken(CSharpParser.STRING, 0); }
		public Class_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterClass_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitClass_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitClass_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_typeContext class_type() throws RecognitionException {
		Class_typeContext _localctx = new Class_typeContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_class_type);
		try {
			setState(522);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(518);
				namespace_or_type_name();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(519);
				match(OBJECT);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(520);
				match(DYNAMIC);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(521);
				match(STRING);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_argument_listContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(CSharpParser.LT, 0); }
		public List<Type_Context> type_() {
			return getRuleContexts(Type_Context.class);
		}
		public Type_Context type_(int i) {
			return getRuleContext(Type_Context.class,i);
		}
		public TerminalNode GT() { return getToken(CSharpParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Type_argument_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_argument_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_argument_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_argument_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_argument_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_argument_listContext type_argument_list() throws RecognitionException {
		Type_argument_listContext _localctx = new Type_argument_listContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_type_argument_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(524);
			match(LT);
			setState(525);
			type_();
			setState(530);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(526);
				match(COMMA);
				setState(527);
				type_();
				}
				}
				setState(532);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(533);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Argument_listContext extends ParserRuleContext {
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Argument_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterArgument_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitArgument_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitArgument_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Argument_listContext argument_list() throws RecognitionException {
		Argument_listContext _localctx = new Argument_listContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_argument_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(535);
			argument();
			setState(540);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(536);
				match(COMMA);
				setState(537);
				argument();
				}
				}
				setState(542);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentContext extends ParserRuleContext {
		public Token refout;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public TerminalNode VAR() { return getToken(CSharpParser.VAR, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public TerminalNode OUT() { return getToken(CSharpParser.OUT, 0); }
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_argument);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(546);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				{
				setState(543);
				identifier();
				setState(544);
				match(COLON);
				}
				break;
			}
			setState(549);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(548);
				((ArgumentContext)_localctx).refout = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 54)) & ~0x3f) == 0 && ((1L << (_la - 54)) & 67371009L) != 0)) ) {
					((ArgumentContext)_localctx).refout = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			}
			setState(557);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				{
				setState(551);
				expression();
				}
				break;
			case 2:
				{
				setState(554);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
				case 1:
					{
					setState(552);
					match(VAR);
					}
					break;
				case 2:
					{
					setState(553);
					type_();
					}
					break;
				}
				setState(556);
				expression();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public AssignmentContext assignment() {
			return getRuleContext(AssignmentContext.class,0);
		}
		public Non_assignment_expressionContext non_assignment_expression() {
			return getRuleContext(Non_assignment_expressionContext.class,0);
		}
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_expression);
		try {
			setState(563);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(559);
				assignment();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(560);
				non_assignment_expression();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(561);
				match(REF);
				setState(562);
				non_assignment_expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Non_assignment_expressionContext extends ParserRuleContext {
		public Lambda_expressionContext lambda_expression() {
			return getRuleContext(Lambda_expressionContext.class,0);
		}
		public Query_expressionContext query_expression() {
			return getRuleContext(Query_expressionContext.class,0);
		}
		public Conditional_expressionContext conditional_expression() {
			return getRuleContext(Conditional_expressionContext.class,0);
		}
		public Non_assignment_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_non_assignment_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNon_assignment_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNon_assignment_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNon_assignment_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Non_assignment_expressionContext non_assignment_expression() throws RecognitionException {
		Non_assignment_expressionContext _localctx = new Non_assignment_expressionContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_non_assignment_expression);
		try {
			setState(568);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(565);
				lambda_expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(566);
				query_expression();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(567);
				conditional_expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentContext extends ParserRuleContext {
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public Assignment_operatorContext assignment_operator() {
			return getRuleContext(Assignment_operatorContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode OP_COALESCING_ASSIGNMENT() { return getToken(CSharpParser.OP_COALESCING_ASSIGNMENT, 0); }
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_assignment);
		try {
			setState(578);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(570);
				unary_expression();
				setState(571);
				assignment_operator();
				setState(572);
				expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(574);
				unary_expression();
				setState(575);
				match(OP_COALESCING_ASSIGNMENT);
				setState(576);
				throwable_expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Assignment_operatorContext extends ParserRuleContext {
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public TerminalNode OP_ADD_ASSIGNMENT() { return getToken(CSharpParser.OP_ADD_ASSIGNMENT, 0); }
		public TerminalNode OP_SUB_ASSIGNMENT() { return getToken(CSharpParser.OP_SUB_ASSIGNMENT, 0); }
		public TerminalNode OP_MULT_ASSIGNMENT() { return getToken(CSharpParser.OP_MULT_ASSIGNMENT, 0); }
		public TerminalNode OP_DIV_ASSIGNMENT() { return getToken(CSharpParser.OP_DIV_ASSIGNMENT, 0); }
		public TerminalNode OP_MOD_ASSIGNMENT() { return getToken(CSharpParser.OP_MOD_ASSIGNMENT, 0); }
		public TerminalNode OP_AND_ASSIGNMENT() { return getToken(CSharpParser.OP_AND_ASSIGNMENT, 0); }
		public TerminalNode OP_OR_ASSIGNMENT() { return getToken(CSharpParser.OP_OR_ASSIGNMENT, 0); }
		public TerminalNode OP_XOR_ASSIGNMENT() { return getToken(CSharpParser.OP_XOR_ASSIGNMENT, 0); }
		public TerminalNode OP_LEFT_SHIFT_ASSIGNMENT() { return getToken(CSharpParser.OP_LEFT_SHIFT_ASSIGNMENT, 0); }
		public Right_shift_assignmentContext right_shift_assignment() {
			return getRuleContext(Right_shift_assignmentContext.class,0);
		}
		public Assignment_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment_operator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAssignment_operator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAssignment_operator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAssignment_operator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Assignment_operatorContext assignment_operator() throws RecognitionException {
		Assignment_operatorContext _localctx = new Assignment_operatorContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_assignment_operator);
		try {
			setState(591);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ASSIGNMENT:
				enterOuterAlt(_localctx, 1);
				{
				setState(580);
				match(ASSIGNMENT);
				}
				break;
			case OP_ADD_ASSIGNMENT:
				enterOuterAlt(_localctx, 2);
				{
				setState(581);
				match(OP_ADD_ASSIGNMENT);
				}
				break;
			case OP_SUB_ASSIGNMENT:
				enterOuterAlt(_localctx, 3);
				{
				setState(582);
				match(OP_SUB_ASSIGNMENT);
				}
				break;
			case OP_MULT_ASSIGNMENT:
				enterOuterAlt(_localctx, 4);
				{
				setState(583);
				match(OP_MULT_ASSIGNMENT);
				}
				break;
			case OP_DIV_ASSIGNMENT:
				enterOuterAlt(_localctx, 5);
				{
				setState(584);
				match(OP_DIV_ASSIGNMENT);
				}
				break;
			case OP_MOD_ASSIGNMENT:
				enterOuterAlt(_localctx, 6);
				{
				setState(585);
				match(OP_MOD_ASSIGNMENT);
				}
				break;
			case OP_AND_ASSIGNMENT:
				enterOuterAlt(_localctx, 7);
				{
				setState(586);
				match(OP_AND_ASSIGNMENT);
				}
				break;
			case OP_OR_ASSIGNMENT:
				enterOuterAlt(_localctx, 8);
				{
				setState(587);
				match(OP_OR_ASSIGNMENT);
				}
				break;
			case OP_XOR_ASSIGNMENT:
				enterOuterAlt(_localctx, 9);
				{
				setState(588);
				match(OP_XOR_ASSIGNMENT);
				}
				break;
			case OP_LEFT_SHIFT_ASSIGNMENT:
				enterOuterAlt(_localctx, 10);
				{
				setState(589);
				match(OP_LEFT_SHIFT_ASSIGNMENT);
				}
				break;
			case GT:
				enterOuterAlt(_localctx, 11);
				{
				setState(590);
				right_shift_assignment();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Conditional_expressionContext extends ParserRuleContext {
		public Null_coalescing_expressionContext null_coalescing_expression() {
			return getRuleContext(Null_coalescing_expressionContext.class,0);
		}
		public TerminalNode INTERR() { return getToken(CSharpParser.INTERR, 0); }
		public List<Throwable_expressionContext> throwable_expression() {
			return getRuleContexts(Throwable_expressionContext.class);
		}
		public Throwable_expressionContext throwable_expression(int i) {
			return getRuleContext(Throwable_expressionContext.class,i);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Conditional_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditional_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConditional_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConditional_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConditional_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Conditional_expressionContext conditional_expression() throws RecognitionException {
		Conditional_expressionContext _localctx = new Conditional_expressionContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_conditional_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(593);
			null_coalescing_expression();
			setState(599);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				{
				setState(594);
				match(INTERR);
				setState(595);
				throwable_expression();
				setState(596);
				match(COLON);
				setState(597);
				throwable_expression();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Null_coalescing_expressionContext extends ParserRuleContext {
		public Conditional_or_expressionContext conditional_or_expression() {
			return getRuleContext(Conditional_or_expressionContext.class,0);
		}
		public TerminalNode OP_COALESCING() { return getToken(CSharpParser.OP_COALESCING, 0); }
		public Null_coalescing_expressionContext null_coalescing_expression() {
			return getRuleContext(Null_coalescing_expressionContext.class,0);
		}
		public Throw_expressionContext throw_expression() {
			return getRuleContext(Throw_expressionContext.class,0);
		}
		public Null_coalescing_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_null_coalescing_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNull_coalescing_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNull_coalescing_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNull_coalescing_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Null_coalescing_expressionContext null_coalescing_expression() throws RecognitionException {
		Null_coalescing_expressionContext _localctx = new Null_coalescing_expressionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_null_coalescing_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(601);
			conditional_or_expression();
			setState(607);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				{
				setState(602);
				match(OP_COALESCING);
				setState(605);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ADD:
				case ALIAS:
				case ARGLIST:
				case ASCENDING:
				case ASYNC:
				case AWAIT:
				case BASE:
				case BOOL:
				case BY:
				case BYTE:
				case CHAR:
				case CHECKED:
				case DECIMAL:
				case DEFAULT:
				case DELEGATE:
				case DESCENDING:
				case DOUBLE:
				case DYNAMIC:
				case EQUALS:
				case FALSE:
				case FLOAT:
				case FROM:
				case GET:
				case GROUP:
				case INT:
				case INTO:
				case JOIN:
				case LET:
				case LONG:
				case NAMEOF:
				case NEW:
				case NULL_:
				case OBJECT:
				case ON:
				case ORDERBY:
				case PARTIAL:
				case REMOVE:
				case SBYTE:
				case SELECT:
				case SET:
				case SHORT:
				case SIZEOF:
				case STRING:
				case THIS:
				case TRUE:
				case TYPEOF:
				case UINT:
				case ULONG:
				case UNCHECKED:
				case UNMANAGED:
				case USHORT:
				case VAR:
				case WHEN:
				case WHERE:
				case YIELD:
				case IDENTIFIER:
				case LITERAL_ACCESS:
				case INTEGER_LITERAL:
				case HEX_INTEGER_LITERAL:
				case BIN_INTEGER_LITERAL:
				case REAL_LITERAL:
				case CHARACTER_LITERAL:
				case REGULAR_STRING:
				case VERBATIUM_STRING:
				case INTERPOLATED_REGULAR_STRING_START:
				case INTERPOLATED_VERBATIUM_STRING_START:
				case OPEN_PARENS:
				case PLUS:
				case MINUS:
				case STAR:
				case AMP:
				case CARET:
				case BANG:
				case TILDE:
				case OP_INC:
				case OP_DEC:
				case OP_RANGE:
					{
					setState(603);
					null_coalescing_expression();
					}
					break;
				case THROW:
					{
					setState(604);
					throw_expression();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Conditional_or_expressionContext extends ParserRuleContext {
		public List<Conditional_and_expressionContext> conditional_and_expression() {
			return getRuleContexts(Conditional_and_expressionContext.class);
		}
		public Conditional_and_expressionContext conditional_and_expression(int i) {
			return getRuleContext(Conditional_and_expressionContext.class,i);
		}
		public List<TerminalNode> OP_OR() { return getTokens(CSharpParser.OP_OR); }
		public TerminalNode OP_OR(int i) {
			return getToken(CSharpParser.OP_OR, i);
		}
		public Conditional_or_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditional_or_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConditional_or_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConditional_or_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConditional_or_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Conditional_or_expressionContext conditional_or_expression() throws RecognitionException {
		Conditional_or_expressionContext _localctx = new Conditional_or_expressionContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_conditional_or_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(609);
			conditional_and_expression();
			setState(614);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(610);
					match(OP_OR);
					setState(611);
					conditional_and_expression();
					}
					} 
				}
				setState(616);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Conditional_and_expressionContext extends ParserRuleContext {
		public List<Inclusive_or_expressionContext> inclusive_or_expression() {
			return getRuleContexts(Inclusive_or_expressionContext.class);
		}
		public Inclusive_or_expressionContext inclusive_or_expression(int i) {
			return getRuleContext(Inclusive_or_expressionContext.class,i);
		}
		public List<TerminalNode> OP_AND() { return getTokens(CSharpParser.OP_AND); }
		public TerminalNode OP_AND(int i) {
			return getToken(CSharpParser.OP_AND, i);
		}
		public Conditional_and_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditional_and_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConditional_and_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConditional_and_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConditional_and_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Conditional_and_expressionContext conditional_and_expression() throws RecognitionException {
		Conditional_and_expressionContext _localctx = new Conditional_and_expressionContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_conditional_and_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(617);
			inclusive_or_expression();
			setState(622);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(618);
					match(OP_AND);
					setState(619);
					inclusive_or_expression();
					}
					} 
				}
				setState(624);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Inclusive_or_expressionContext extends ParserRuleContext {
		public List<Exclusive_or_expressionContext> exclusive_or_expression() {
			return getRuleContexts(Exclusive_or_expressionContext.class);
		}
		public Exclusive_or_expressionContext exclusive_or_expression(int i) {
			return getRuleContext(Exclusive_or_expressionContext.class,i);
		}
		public List<TerminalNode> BITWISE_OR() { return getTokens(CSharpParser.BITWISE_OR); }
		public TerminalNode BITWISE_OR(int i) {
			return getToken(CSharpParser.BITWISE_OR, i);
		}
		public Inclusive_or_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inclusive_or_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInclusive_or_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInclusive_or_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInclusive_or_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Inclusive_or_expressionContext inclusive_or_expression() throws RecognitionException {
		Inclusive_or_expressionContext _localctx = new Inclusive_or_expressionContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_inclusive_or_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(625);
			exclusive_or_expression();
			setState(630);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,32,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(626);
					match(BITWISE_OR);
					setState(627);
					exclusive_or_expression();
					}
					} 
				}
				setState(632);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,32,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Exclusive_or_expressionContext extends ParserRuleContext {
		public List<And_expressionContext> and_expression() {
			return getRuleContexts(And_expressionContext.class);
		}
		public And_expressionContext and_expression(int i) {
			return getRuleContext(And_expressionContext.class,i);
		}
		public List<TerminalNode> CARET() { return getTokens(CSharpParser.CARET); }
		public TerminalNode CARET(int i) {
			return getToken(CSharpParser.CARET, i);
		}
		public Exclusive_or_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exclusive_or_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExclusive_or_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExclusive_or_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExclusive_or_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Exclusive_or_expressionContext exclusive_or_expression() throws RecognitionException {
		Exclusive_or_expressionContext _localctx = new Exclusive_or_expressionContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_exclusive_or_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(633);
			and_expression();
			setState(638);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(634);
					match(CARET);
					setState(635);
					and_expression();
					}
					} 
				}
				setState(640);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class And_expressionContext extends ParserRuleContext {
		public List<Equality_expressionContext> equality_expression() {
			return getRuleContexts(Equality_expressionContext.class);
		}
		public Equality_expressionContext equality_expression(int i) {
			return getRuleContext(Equality_expressionContext.class,i);
		}
		public List<TerminalNode> AMP() { return getTokens(CSharpParser.AMP); }
		public TerminalNode AMP(int i) {
			return getToken(CSharpParser.AMP, i);
		}
		public And_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAnd_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAnd_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAnd_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final And_expressionContext and_expression() throws RecognitionException {
		And_expressionContext _localctx = new And_expressionContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_and_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(641);
			equality_expression();
			setState(646);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(642);
					match(AMP);
					setState(643);
					equality_expression();
					}
					} 
				}
				setState(648);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Equality_expressionContext extends ParserRuleContext {
		public List<Relational_expressionContext> relational_expression() {
			return getRuleContexts(Relational_expressionContext.class);
		}
		public Relational_expressionContext relational_expression(int i) {
			return getRuleContext(Relational_expressionContext.class,i);
		}
		public List<TerminalNode> OP_EQ() { return getTokens(CSharpParser.OP_EQ); }
		public TerminalNode OP_EQ(int i) {
			return getToken(CSharpParser.OP_EQ, i);
		}
		public List<TerminalNode> OP_NE() { return getTokens(CSharpParser.OP_NE); }
		public TerminalNode OP_NE(int i) {
			return getToken(CSharpParser.OP_NE, i);
		}
		public Equality_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equality_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEquality_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEquality_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEquality_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Equality_expressionContext equality_expression() throws RecognitionException {
		Equality_expressionContext _localctx = new Equality_expressionContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_equality_expression);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(649);
			relational_expression();
			setState(654);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(650);
					_la = _input.LA(1);
					if ( !(_la==OP_EQ || _la==OP_NE) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(651);
					relational_expression();
					}
					} 
				}
				setState(656);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Relational_expressionContext extends ParserRuleContext {
		public List<Shift_expressionContext> shift_expression() {
			return getRuleContexts(Shift_expressionContext.class);
		}
		public Shift_expressionContext shift_expression(int i) {
			return getRuleContext(Shift_expressionContext.class,i);
		}
		public List<TerminalNode> IS() { return getTokens(CSharpParser.IS); }
		public TerminalNode IS(int i) {
			return getToken(CSharpParser.IS, i);
		}
		public List<IsTypeContext> isType() {
			return getRuleContexts(IsTypeContext.class);
		}
		public IsTypeContext isType(int i) {
			return getRuleContext(IsTypeContext.class,i);
		}
		public List<TerminalNode> AS() { return getTokens(CSharpParser.AS); }
		public TerminalNode AS(int i) {
			return getToken(CSharpParser.AS, i);
		}
		public List<Type_Context> type_() {
			return getRuleContexts(Type_Context.class);
		}
		public Type_Context type_(int i) {
			return getRuleContext(Type_Context.class,i);
		}
		public List<TerminalNode> LT() { return getTokens(CSharpParser.LT); }
		public TerminalNode LT(int i) {
			return getToken(CSharpParser.LT, i);
		}
		public List<TerminalNode> GT() { return getTokens(CSharpParser.GT); }
		public TerminalNode GT(int i) {
			return getToken(CSharpParser.GT, i);
		}
		public List<TerminalNode> OP_LE() { return getTokens(CSharpParser.OP_LE); }
		public TerminalNode OP_LE(int i) {
			return getToken(CSharpParser.OP_LE, i);
		}
		public List<TerminalNode> OP_GE() { return getTokens(CSharpParser.OP_GE); }
		public TerminalNode OP_GE(int i) {
			return getToken(CSharpParser.OP_GE, i);
		}
		public Relational_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relational_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterRelational_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitRelational_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitRelational_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Relational_expressionContext relational_expression() throws RecognitionException {
		Relational_expressionContext _localctx = new Relational_expressionContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_relational_expression);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(657);
			shift_expression();
			setState(666);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,37,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(664);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case LT:
					case GT:
					case OP_LE:
					case OP_GE:
						{
						setState(658);
						_la = _input.LA(1);
						if ( !(((((_la - 146)) & ~0x3f) == 0 && ((1L << (_la - 146)) & 12291L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(659);
						shift_expression();
						}
						break;
					case IS:
						{
						setState(660);
						match(IS);
						setState(661);
						isType();
						}
						break;
					case AS:
						{
						setState(662);
						match(AS);
						setState(663);
						type_();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(668);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,37,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Shift_expressionContext extends ParserRuleContext {
		public List<Additive_expressionContext> additive_expression() {
			return getRuleContexts(Additive_expressionContext.class);
		}
		public Additive_expressionContext additive_expression(int i) {
			return getRuleContext(Additive_expressionContext.class,i);
		}
		public List<TerminalNode> OP_LEFT_SHIFT() { return getTokens(CSharpParser.OP_LEFT_SHIFT); }
		public TerminalNode OP_LEFT_SHIFT(int i) {
			return getToken(CSharpParser.OP_LEFT_SHIFT, i);
		}
		public List<Right_shiftContext> right_shift() {
			return getRuleContexts(Right_shiftContext.class);
		}
		public Right_shiftContext right_shift(int i) {
			return getRuleContext(Right_shiftContext.class,i);
		}
		public Shift_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shift_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterShift_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitShift_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitShift_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Shift_expressionContext shift_expression() throws RecognitionException {
		Shift_expressionContext _localctx = new Shift_expressionContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_shift_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(669);
			additive_expression();
			setState(677);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(672);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case OP_LEFT_SHIFT:
						{
						setState(670);
						match(OP_LEFT_SHIFT);
						}
						break;
					case GT:
						{
						setState(671);
						right_shift();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(674);
					additive_expression();
					}
					} 
				}
				setState(679);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Additive_expressionContext extends ParserRuleContext {
		public List<Multiplicative_expressionContext> multiplicative_expression() {
			return getRuleContexts(Multiplicative_expressionContext.class);
		}
		public Multiplicative_expressionContext multiplicative_expression(int i) {
			return getRuleContext(Multiplicative_expressionContext.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(CSharpParser.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(CSharpParser.PLUS, i);
		}
		public List<TerminalNode> MINUS() { return getTokens(CSharpParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(CSharpParser.MINUS, i);
		}
		public Additive_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_additive_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAdditive_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAdditive_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAdditive_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Additive_expressionContext additive_expression() throws RecognitionException {
		Additive_expressionContext _localctx = new Additive_expressionContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_additive_expression);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(680);
			multiplicative_expression();
			setState(685);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,40,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(681);
					_la = _input.LA(1);
					if ( !(_la==PLUS || _la==MINUS) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(682);
					multiplicative_expression();
					}
					} 
				}
				setState(687);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,40,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Multiplicative_expressionContext extends ParserRuleContext {
		public List<Switch_expressionContext> switch_expression() {
			return getRuleContexts(Switch_expressionContext.class);
		}
		public Switch_expressionContext switch_expression(int i) {
			return getRuleContext(Switch_expressionContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(CSharpParser.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(CSharpParser.STAR, i);
		}
		public List<TerminalNode> DIV() { return getTokens(CSharpParser.DIV); }
		public TerminalNode DIV(int i) {
			return getToken(CSharpParser.DIV, i);
		}
		public List<TerminalNode> PERCENT() { return getTokens(CSharpParser.PERCENT); }
		public TerminalNode PERCENT(int i) {
			return getToken(CSharpParser.PERCENT, i);
		}
		public Multiplicative_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiplicative_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMultiplicative_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMultiplicative_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMultiplicative_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Multiplicative_expressionContext multiplicative_expression() throws RecognitionException {
		Multiplicative_expressionContext _localctx = new Multiplicative_expressionContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_multiplicative_expression);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(688);
			switch_expression();
			setState(693);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(689);
					_la = _input.LA(1);
					if ( !(((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & 7L) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(690);
					switch_expression();
					}
					} 
				}
				setState(695);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Switch_expressionContext extends ParserRuleContext {
		public Range_expressionContext range_expression() {
			return getRuleContext(Range_expressionContext.class,0);
		}
		public TerminalNode SWITCH() { return getToken(CSharpParser.SWITCH, 0); }
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Switch_expression_armsContext switch_expression_arms() {
			return getRuleContext(Switch_expression_armsContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(CSharpParser.COMMA, 0); }
		public Switch_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switch_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSwitch_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSwitch_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSwitch_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Switch_expressionContext switch_expression() throws RecognitionException {
		Switch_expressionContext _localctx = new Switch_expressionContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_switch_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(696);
			range_expression();
			setState(706);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
			case 1:
				{
				setState(697);
				match(SWITCH);
				setState(698);
				match(OPEN_BRACE);
				setState(703);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(699);
					switch_expression_arms();
					setState(701);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(700);
						match(COMMA);
						}
					}

					}
				}

				setState(705);
				match(CLOSE_BRACE);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Switch_expression_armsContext extends ParserRuleContext {
		public List<Switch_expression_armContext> switch_expression_arm() {
			return getRuleContexts(Switch_expression_armContext.class);
		}
		public Switch_expression_armContext switch_expression_arm(int i) {
			return getRuleContext(Switch_expression_armContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Switch_expression_armsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switch_expression_arms; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSwitch_expression_arms(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSwitch_expression_arms(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSwitch_expression_arms(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Switch_expression_armsContext switch_expression_arms() throws RecognitionException {
		Switch_expression_armsContext _localctx = new Switch_expression_armsContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_switch_expression_arms);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(708);
			switch_expression_arm();
			setState(713);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,45,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(709);
					match(COMMA);
					setState(710);
					switch_expression_arm();
					}
					} 
				}
				setState(715);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,45,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Switch_expression_armContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public Case_guardContext case_guard() {
			return getRuleContext(Case_guardContext.class,0);
		}
		public Switch_expression_armContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switch_expression_arm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSwitch_expression_arm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSwitch_expression_arm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSwitch_expression_arm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Switch_expression_armContext switch_expression_arm() throws RecognitionException {
		Switch_expression_armContext _localctx = new Switch_expression_armContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_switch_expression_arm);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(716);
			expression();
			setState(718);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHEN) {
				{
				setState(717);
				case_guard();
				}
			}

			setState(720);
			right_arrow();
			setState(721);
			throwable_expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Range_expressionContext extends ParserRuleContext {
		public List<Unary_expressionContext> unary_expression() {
			return getRuleContexts(Unary_expressionContext.class);
		}
		public Unary_expressionContext unary_expression(int i) {
			return getRuleContext(Unary_expressionContext.class,i);
		}
		public TerminalNode OP_RANGE() { return getToken(CSharpParser.OP_RANGE, 0); }
		public Range_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_range_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterRange_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitRange_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitRange_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Range_expressionContext range_expression() throws RecognitionException {
		Range_expressionContext _localctx = new Range_expressionContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_range_expression);
		int _la;
		try {
			setState(731);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,49,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(723);
				unary_expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(725);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161930941L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 12642753L) != 0)) {
					{
					setState(724);
					unary_expression();
					}
				}

				setState(727);
				match(OP_RANGE);
				setState(729);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
				case 1:
					{
					setState(728);
					unary_expression();
					}
					break;
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Unary_expressionContext extends ParserRuleContext {
		public Cast_expressionContext cast_expression() {
			return getRuleContext(Cast_expressionContext.class,0);
		}
		public Primary_expressionContext primary_expression() {
			return getRuleContext(Primary_expressionContext.class,0);
		}
		public TerminalNode PLUS() { return getToken(CSharpParser.PLUS, 0); }
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(CSharpParser.MINUS, 0); }
		public TerminalNode BANG() { return getToken(CSharpParser.BANG, 0); }
		public TerminalNode TILDE() { return getToken(CSharpParser.TILDE, 0); }
		public TerminalNode OP_INC() { return getToken(CSharpParser.OP_INC, 0); }
		public TerminalNode OP_DEC() { return getToken(CSharpParser.OP_DEC, 0); }
		public TerminalNode AWAIT() { return getToken(CSharpParser.AWAIT, 0); }
		public TerminalNode AMP() { return getToken(CSharpParser.AMP, 0); }
		public TerminalNode STAR() { return getToken(CSharpParser.STAR, 0); }
		public TerminalNode CARET() { return getToken(CSharpParser.CARET, 0); }
		public Unary_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unary_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUnary_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUnary_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUnary_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Unary_expressionContext unary_expression() throws RecognitionException {
		Unary_expressionContext _localctx = new Unary_expressionContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_unary_expression);
		try {
			setState(755);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,50,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(733);
				cast_expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(734);
				primary_expression();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(735);
				match(PLUS);
				setState(736);
				unary_expression();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(737);
				match(MINUS);
				setState(738);
				unary_expression();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(739);
				match(BANG);
				setState(740);
				unary_expression();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(741);
				match(TILDE);
				setState(742);
				unary_expression();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(743);
				match(OP_INC);
				setState(744);
				unary_expression();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(745);
				match(OP_DEC);
				setState(746);
				unary_expression();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(747);
				match(AWAIT);
				setState(748);
				unary_expression();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(749);
				match(AMP);
				setState(750);
				unary_expression();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(751);
				match(STAR);
				setState(752);
				unary_expression();
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(753);
				match(CARET);
				setState(754);
				unary_expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Cast_expressionContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public Cast_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cast_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCast_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCast_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCast_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Cast_expressionContext cast_expression() throws RecognitionException {
		Cast_expressionContext _localctx = new Cast_expressionContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_cast_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(757);
			match(OPEN_PARENS);
			setState(758);
			type_();
			setState(759);
			match(CLOSE_PARENS);
			setState(760);
			unary_expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Primary_expressionContext extends ParserRuleContext {
		public Primary_expression_startContext pe;
		public Primary_expression_startContext primary_expression_start() {
			return getRuleContext(Primary_expression_startContext.class,0);
		}
		public List<TerminalNode> BANG() { return getTokens(CSharpParser.BANG); }
		public TerminalNode BANG(int i) {
			return getToken(CSharpParser.BANG, i);
		}
		public List<Bracket_expressionContext> bracket_expression() {
			return getRuleContexts(Bracket_expressionContext.class);
		}
		public Bracket_expressionContext bracket_expression(int i) {
			return getRuleContext(Bracket_expressionContext.class,i);
		}
		public List<Member_accessContext> member_access() {
			return getRuleContexts(Member_accessContext.class);
		}
		public Member_accessContext member_access(int i) {
			return getRuleContext(Member_accessContext.class,i);
		}
		public List<Method_invocationContext> method_invocation() {
			return getRuleContexts(Method_invocationContext.class);
		}
		public Method_invocationContext method_invocation(int i) {
			return getRuleContext(Method_invocationContext.class,i);
		}
		public List<TerminalNode> OP_INC() { return getTokens(CSharpParser.OP_INC); }
		public TerminalNode OP_INC(int i) {
			return getToken(CSharpParser.OP_INC, i);
		}
		public List<TerminalNode> OP_DEC() { return getTokens(CSharpParser.OP_DEC); }
		public TerminalNode OP_DEC(int i) {
			return getToken(CSharpParser.OP_DEC, i);
		}
		public List<TerminalNode> OP_PTR() { return getTokens(CSharpParser.OP_PTR); }
		public TerminalNode OP_PTR(int i) {
			return getToken(CSharpParser.OP_PTR, i);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public Primary_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterPrimary_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitPrimary_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitPrimary_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Primary_expressionContext primary_expression() throws RecognitionException {
		Primary_expressionContext _localctx = new Primary_expressionContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_primary_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(762);
			((Primary_expressionContext)_localctx).pe = primary_expression_start();
			setState(764);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
			case 1:
				{
				setState(763);
				match(BANG);
				}
				break;
			}
			setState(769);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(766);
					bracket_expression();
					}
					} 
				}
				setState(771);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			}
			setState(773);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,53,_ctx) ) {
			case 1:
				{
				setState(772);
				match(BANG);
				}
				break;
			}
			setState(797);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,58,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(781);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case DOT:
					case INTERR:
						{
						setState(775);
						member_access();
						}
						break;
					case OPEN_PARENS:
						{
						setState(776);
						method_invocation();
						}
						break;
					case OP_INC:
						{
						setState(777);
						match(OP_INC);
						}
						break;
					case OP_DEC:
						{
						setState(778);
						match(OP_DEC);
						}
						break;
					case OP_PTR:
						{
						setState(779);
						match(OP_PTR);
						setState(780);
						identifier();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(784);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
					case 1:
						{
						setState(783);
						match(BANG);
						}
						break;
					}
					setState(789);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,56,_ctx);
					while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
						if ( _alt==1 ) {
							{
							{
							setState(786);
							bracket_expression();
							}
							} 
						}
						setState(791);
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,56,_ctx);
					}
					setState(793);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
					case 1:
						{
						setState(792);
						match(BANG);
						}
						break;
					}
					}
					} 
				}
				setState(799);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,58,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Primary_expression_startContext extends ParserRuleContext {
		public Primary_expression_startContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary_expression_start; }
	 
		public Primary_expression_startContext() { }
		public void copyFrom(Primary_expression_startContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LiteralAccessExpressionContext extends Primary_expression_startContext {
		public TerminalNode LITERAL_ACCESS() { return getToken(CSharpParser.LITERAL_ACCESS, 0); }
		public LiteralAccessExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLiteralAccessExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLiteralAccessExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLiteralAccessExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DefaultValueExpressionContext extends Primary_expression_startContext {
		public TerminalNode DEFAULT() { return getToken(CSharpParser.DEFAULT, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public DefaultValueExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterDefaultValueExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitDefaultValueExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitDefaultValueExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BaseAccessExpressionContext extends Primary_expression_startContext {
		public TerminalNode BASE() { return getToken(CSharpParser.BASE, 0); }
		public TerminalNode DOT() { return getToken(CSharpParser.DOT, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public Expression_listContext expression_list() {
			return getRuleContext(Expression_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public Type_argument_listContext type_argument_list() {
			return getRuleContext(Type_argument_listContext.class,0);
		}
		public BaseAccessExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterBaseAccessExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitBaseAccessExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitBaseAccessExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SizeofExpressionContext extends Primary_expression_startContext {
		public TerminalNode SIZEOF() { return getToken(CSharpParser.SIZEOF, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public SizeofExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSizeofExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSizeofExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSizeofExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesisExpressionsContext extends Primary_expression_startContext {
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public ParenthesisExpressionsContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterParenthesisExpressions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitParenthesisExpressions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitParenthesisExpressions(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ThisReferenceExpressionContext extends Primary_expression_startContext {
		public TerminalNode THIS() { return getToken(CSharpParser.THIS, 0); }
		public ThisReferenceExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterThisReferenceExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitThisReferenceExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitThisReferenceExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ObjectCreationExpressionContext extends Primary_expression_startContext {
		public TerminalNode NEW() { return getToken(CSharpParser.NEW, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public Anonymous_object_initializerContext anonymous_object_initializer() {
			return getRuleContext(Anonymous_object_initializerContext.class,0);
		}
		public List<Rank_specifierContext> rank_specifier() {
			return getRuleContexts(Rank_specifierContext.class);
		}
		public Rank_specifierContext rank_specifier(int i) {
			return getRuleContext(Rank_specifierContext.class,i);
		}
		public Array_initializerContext array_initializer() {
			return getRuleContext(Array_initializerContext.class,0);
		}
		public Object_creation_expressionContext object_creation_expression() {
			return getRuleContext(Object_creation_expressionContext.class,0);
		}
		public Object_or_collection_initializerContext object_or_collection_initializer() {
			return getRuleContext(Object_or_collection_initializerContext.class,0);
		}
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public Expression_listContext expression_list() {
			return getRuleContext(Expression_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public ObjectCreationExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterObjectCreationExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitObjectCreationExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitObjectCreationExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AnonymousMethodExpressionContext extends Primary_expression_startContext {
		public TerminalNode DELEGATE() { return getToken(CSharpParser.DELEGATE, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode ASYNC() { return getToken(CSharpParser.ASYNC, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Explicit_anonymous_function_parameter_listContext explicit_anonymous_function_parameter_list() {
			return getRuleContext(Explicit_anonymous_function_parameter_listContext.class,0);
		}
		public AnonymousMethodExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAnonymousMethodExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAnonymousMethodExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAnonymousMethodExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeofExpressionContext extends Primary_expression_startContext {
		public TerminalNode TYPEOF() { return getToken(CSharpParser.TYPEOF, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Unbound_type_nameContext unbound_type_name() {
			return getRuleContext(Unbound_type_nameContext.class,0);
		}
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode VOID() { return getToken(CSharpParser.VOID, 0); }
		public TypeofExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterTypeofExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitTypeofExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitTypeofExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TupleExpressionContext extends Primary_expression_startContext {
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public TupleExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterTupleExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitTupleExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitTupleExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UncheckedExpressionContext extends Primary_expression_startContext {
		public TerminalNode UNCHECKED() { return getToken(CSharpParser.UNCHECKED, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public UncheckedExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUncheckedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUncheckedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUncheckedExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SimpleNameExpressionContext extends Primary_expression_startContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Type_argument_listContext type_argument_list() {
			return getRuleContext(Type_argument_listContext.class,0);
		}
		public SimpleNameExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSimpleNameExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSimpleNameExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSimpleNameExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MemberAccessExpressionContext extends Primary_expression_startContext {
		public Predefined_typeContext predefined_type() {
			return getRuleContext(Predefined_typeContext.class,0);
		}
		public Qualified_alias_memberContext qualified_alias_member() {
			return getRuleContext(Qualified_alias_memberContext.class,0);
		}
		public MemberAccessExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMemberAccessExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMemberAccessExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMemberAccessExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CheckedExpressionContext extends Primary_expression_startContext {
		public TerminalNode CHECKED() { return getToken(CSharpParser.CHECKED, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public CheckedExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCheckedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCheckedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCheckedExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LiteralExpressionContext extends Primary_expression_startContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public LiteralExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLiteralExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLiteralExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NameofExpressionContext extends Primary_expression_startContext {
		public TerminalNode NAMEOF() { return getToken(CSharpParser.NAMEOF, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public List<TerminalNode> DOT() { return getTokens(CSharpParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(CSharpParser.DOT, i);
		}
		public NameofExpressionContext(Primary_expression_startContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNameofExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNameofExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNameofExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Primary_expression_startContext primary_expression_start() throws RecognitionException {
		Primary_expression_startContext _localctx = new Primary_expression_startContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_primary_expression_start);
		int _la;
		try {
			int _alt;
			setState(921);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,74,_ctx) ) {
			case 1:
				_localctx = new LiteralExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(800);
				literal();
				}
				break;
			case 2:
				_localctx = new SimpleNameExpressionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(801);
				identifier();
				setState(803);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,59,_ctx) ) {
				case 1:
					{
					setState(802);
					type_argument_list();
					}
					break;
				}
				}
				break;
			case 3:
				_localctx = new ParenthesisExpressionsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(805);
				match(OPEN_PARENS);
				setState(806);
				expression();
				setState(807);
				match(CLOSE_PARENS);
				}
				break;
			case 4:
				_localctx = new MemberAccessExpressionContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(809);
				predefined_type();
				}
				break;
			case 5:
				_localctx = new MemberAccessExpressionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(810);
				qualified_alias_member();
				}
				break;
			case 6:
				_localctx = new LiteralAccessExpressionContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(811);
				match(LITERAL_ACCESS);
				}
				break;
			case 7:
				_localctx = new ThisReferenceExpressionContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(812);
				match(THIS);
				}
				break;
			case 8:
				_localctx = new BaseAccessExpressionContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(813);
				match(BASE);
				setState(823);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case DOT:
					{
					setState(814);
					match(DOT);
					setState(815);
					identifier();
					setState(817);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
					case 1:
						{
						setState(816);
						type_argument_list();
						}
						break;
					}
					}
					break;
				case OPEN_BRACKET:
					{
					setState(819);
					match(OPEN_BRACKET);
					setState(820);
					expression_list();
					setState(821);
					match(CLOSE_BRACKET);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 9:
				_localctx = new ObjectCreationExpressionContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(825);
				match(NEW);
				setState(854);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ADD:
				case ALIAS:
				case ARGLIST:
				case ASCENDING:
				case ASYNC:
				case AWAIT:
				case BOOL:
				case BY:
				case BYTE:
				case CHAR:
				case DECIMAL:
				case DESCENDING:
				case DOUBLE:
				case DYNAMIC:
				case EQUALS:
				case FLOAT:
				case FROM:
				case GET:
				case GROUP:
				case INT:
				case INTO:
				case JOIN:
				case LET:
				case LONG:
				case NAMEOF:
				case OBJECT:
				case ON:
				case ORDERBY:
				case PARTIAL:
				case REMOVE:
				case SBYTE:
				case SELECT:
				case SET:
				case SHORT:
				case STRING:
				case UINT:
				case ULONG:
				case UNMANAGED:
				case USHORT:
				case VAR:
				case VOID:
				case WHEN:
				case WHERE:
				case YIELD:
				case IDENTIFIER:
				case OPEN_PARENS:
					{
					setState(826);
					type_();
					setState(848);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
					case 1:
						{
						setState(827);
						object_creation_expression();
						}
						break;
					case 2:
						{
						setState(828);
						object_or_collection_initializer();
						}
						break;
					case 3:
						{
						setState(829);
						match(OPEN_BRACKET);
						setState(830);
						expression_list();
						setState(831);
						match(CLOSE_BRACKET);
						setState(835);
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
						while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
							if ( _alt==1 ) {
								{
								{
								setState(832);
								rank_specifier();
								}
								} 
							}
							setState(837);
							_errHandler.sync(this);
							_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
						}
						setState(839);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
						case 1:
							{
							setState(838);
							array_initializer();
							}
							break;
						}
						}
						break;
					case 4:
						{
						setState(842); 
						_errHandler.sync(this);
						_la = _input.LA(1);
						do {
							{
							{
							setState(841);
							rank_specifier();
							}
							}
							setState(844); 
							_errHandler.sync(this);
							_la = _input.LA(1);
						} while ( _la==OPEN_BRACKET );
						setState(846);
						array_initializer();
						}
						break;
					}
					}
					break;
				case OPEN_BRACE:
					{
					setState(850);
					anonymous_object_initializer();
					}
					break;
				case OPEN_BRACKET:
					{
					setState(851);
					rank_specifier();
					setState(852);
					array_initializer();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 10:
				_localctx = new TupleExpressionContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(856);
				match(OPEN_PARENS);
				setState(857);
				argument();
				setState(860); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(858);
					match(COMMA);
					setState(859);
					argument();
					}
					}
					setState(862); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==COMMA );
				setState(864);
				match(CLOSE_PARENS);
				}
				break;
			case 11:
				_localctx = new TypeofExpressionContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(866);
				match(TYPEOF);
				setState(867);
				match(OPEN_PARENS);
				setState(871);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
				case 1:
					{
					setState(868);
					unbound_type_name();
					}
					break;
				case 2:
					{
					setState(869);
					type_();
					}
					break;
				case 3:
					{
					setState(870);
					match(VOID);
					}
					break;
				}
				setState(873);
				match(CLOSE_PARENS);
				}
				break;
			case 12:
				_localctx = new CheckedExpressionContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(874);
				match(CHECKED);
				setState(875);
				match(OPEN_PARENS);
				setState(876);
				expression();
				setState(877);
				match(CLOSE_PARENS);
				}
				break;
			case 13:
				_localctx = new UncheckedExpressionContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(879);
				match(UNCHECKED);
				setState(880);
				match(OPEN_PARENS);
				setState(881);
				expression();
				setState(882);
				match(CLOSE_PARENS);
				}
				break;
			case 14:
				_localctx = new DefaultValueExpressionContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(884);
				match(DEFAULT);
				setState(889);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,69,_ctx) ) {
				case 1:
					{
					setState(885);
					match(OPEN_PARENS);
					setState(886);
					type_();
					setState(887);
					match(CLOSE_PARENS);
					}
					break;
				}
				}
				break;
			case 15:
				_localctx = new AnonymousMethodExpressionContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(892);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ASYNC) {
					{
					setState(891);
					match(ASYNC);
					}
				}

				setState(894);
				match(DELEGATE);
				setState(900);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPEN_PARENS) {
					{
					setState(895);
					match(OPEN_PARENS);
					setState(897);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 7812956763456032119L) != 0) || ((((_la - 75)) & ~0x3f) == 0 && ((1L << (_la - 75)) & 18015337681329505L) != 0)) {
						{
						setState(896);
						explicit_anonymous_function_parameter_list();
						}
					}

					setState(899);
					match(CLOSE_PARENS);
					}
				}

				setState(902);
				block();
				}
				break;
			case 16:
				_localctx = new SizeofExpressionContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(903);
				match(SIZEOF);
				setState(904);
				match(OPEN_PARENS);
				setState(905);
				type_();
				setState(906);
				match(CLOSE_PARENS);
				}
				break;
			case 17:
				_localctx = new NameofExpressionContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(908);
				match(NAMEOF);
				setState(909);
				match(OPEN_PARENS);
				setState(915);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,73,_ctx);
				while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(910);
						identifier();
						setState(911);
						match(DOT);
						}
						} 
					}
					setState(917);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,73,_ctx);
				}
				setState(918);
				identifier();
				setState(919);
				match(CLOSE_PARENS);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Throwable_expressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Throw_expressionContext throw_expression() {
			return getRuleContext(Throw_expressionContext.class,0);
		}
		public Throwable_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_throwable_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterThrowable_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitThrowable_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitThrowable_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Throwable_expressionContext throwable_expression() throws RecognitionException {
		Throwable_expressionContext _localctx = new Throwable_expressionContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_throwable_expression);
		try {
			setState(925);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(923);
				expression();
				}
				break;
			case THROW:
				enterOuterAlt(_localctx, 2);
				{
				setState(924);
				throw_expression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Throw_expressionContext extends ParserRuleContext {
		public TerminalNode THROW() { return getToken(CSharpParser.THROW, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Throw_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_throw_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterThrow_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitThrow_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitThrow_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Throw_expressionContext throw_expression() throws RecognitionException {
		Throw_expressionContext _localctx = new Throw_expressionContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_throw_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(927);
			match(THROW);
			setState(928);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Member_accessContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(CSharpParser.DOT, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode INTERR() { return getToken(CSharpParser.INTERR, 0); }
		public Type_argument_listContext type_argument_list() {
			return getRuleContext(Type_argument_listContext.class,0);
		}
		public Member_accessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_member_access; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMember_access(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMember_access(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMember_access(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Member_accessContext member_access() throws RecognitionException {
		Member_accessContext _localctx = new Member_accessContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_member_access);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(931);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==INTERR) {
				{
				setState(930);
				match(INTERR);
				}
			}

			setState(933);
			match(DOT);
			setState(934);
			identifier();
			setState(936);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,77,_ctx) ) {
			case 1:
				{
				setState(935);
				type_argument_list();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Bracket_expressionContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public List<Indexer_argumentContext> indexer_argument() {
			return getRuleContexts(Indexer_argumentContext.class);
		}
		public Indexer_argumentContext indexer_argument(int i) {
			return getRuleContext(Indexer_argumentContext.class,i);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public TerminalNode INTERR() { return getToken(CSharpParser.INTERR, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Bracket_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bracket_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterBracket_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitBracket_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitBracket_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Bracket_expressionContext bracket_expression() throws RecognitionException {
		Bracket_expressionContext _localctx = new Bracket_expressionContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_bracket_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(939);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==INTERR) {
				{
				setState(938);
				match(INTERR);
				}
			}

			setState(941);
			match(OPEN_BRACKET);
			setState(942);
			indexer_argument();
			setState(947);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(943);
				match(COMMA);
				setState(944);
				indexer_argument();
				}
				}
				setState(949);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(950);
			match(CLOSE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Indexer_argumentContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Indexer_argumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indexer_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIndexer_argument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIndexer_argument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIndexer_argument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Indexer_argumentContext indexer_argument() throws RecognitionException {
		Indexer_argumentContext _localctx = new Indexer_argumentContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_indexer_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(955);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
			case 1:
				{
				setState(952);
				identifier();
				setState(953);
				match(COLON);
				}
				break;
			}
			setState(957);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Predefined_typeContext extends ParserRuleContext {
		public TerminalNode BOOL() { return getToken(CSharpParser.BOOL, 0); }
		public TerminalNode BYTE() { return getToken(CSharpParser.BYTE, 0); }
		public TerminalNode CHAR() { return getToken(CSharpParser.CHAR, 0); }
		public TerminalNode DECIMAL() { return getToken(CSharpParser.DECIMAL, 0); }
		public TerminalNode DOUBLE() { return getToken(CSharpParser.DOUBLE, 0); }
		public TerminalNode FLOAT() { return getToken(CSharpParser.FLOAT, 0); }
		public TerminalNode INT() { return getToken(CSharpParser.INT, 0); }
		public TerminalNode LONG() { return getToken(CSharpParser.LONG, 0); }
		public TerminalNode OBJECT() { return getToken(CSharpParser.OBJECT, 0); }
		public TerminalNode SBYTE() { return getToken(CSharpParser.SBYTE, 0); }
		public TerminalNode SHORT() { return getToken(CSharpParser.SHORT, 0); }
		public TerminalNode STRING() { return getToken(CSharpParser.STRING, 0); }
		public TerminalNode UINT() { return getToken(CSharpParser.UINT, 0); }
		public TerminalNode ULONG() { return getToken(CSharpParser.ULONG, 0); }
		public TerminalNode USHORT() { return getToken(CSharpParser.USHORT, 0); }
		public Predefined_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predefined_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterPredefined_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitPredefined_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitPredefined_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Predefined_typeContext predefined_type() throws RecognitionException {
		Predefined_typeContext _localctx = new Predefined_typeContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_predefined_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(959);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & -9187308037727846400L) != 0) || ((((_la - 68)) & ~0x3f) == 0 && ((1L << (_la - 68)) & 75170873345L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression_listContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Expression_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExpression_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExpression_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExpression_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Expression_listContext expression_list() throws RecognitionException {
		Expression_listContext _localctx = new Expression_listContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_expression_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(961);
			expression();
			setState(966);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(962);
				match(COMMA);
				setState(963);
				expression();
				}
				}
				setState(968);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Object_or_collection_initializerContext extends ParserRuleContext {
		public Object_initializerContext object_initializer() {
			return getRuleContext(Object_initializerContext.class,0);
		}
		public Collection_initializerContext collection_initializer() {
			return getRuleContext(Collection_initializerContext.class,0);
		}
		public Object_or_collection_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_object_or_collection_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterObject_or_collection_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitObject_or_collection_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitObject_or_collection_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Object_or_collection_initializerContext object_or_collection_initializer() throws RecognitionException {
		Object_or_collection_initializerContext _localctx = new Object_or_collection_initializerContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_object_or_collection_initializer);
		try {
			setState(971);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(969);
				object_initializer();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(970);
				collection_initializer();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Object_initializerContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Member_initializer_listContext member_initializer_list() {
			return getRuleContext(Member_initializer_listContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(CSharpParser.COMMA, 0); }
		public Object_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_object_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterObject_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitObject_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitObject_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Object_initializerContext object_initializer() throws RecognitionException {
		Object_initializerContext _localctx = new Object_initializerContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_object_initializer);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(973);
			match(OPEN_BRACE);
			setState(978);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 3750091428249852928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -9221467407831136095L) != 0)) {
				{
				setState(974);
				member_initializer_list();
				setState(976);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(975);
					match(COMMA);
					}
				}

				}
			}

			setState(980);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Member_initializer_listContext extends ParserRuleContext {
		public List<Member_initializerContext> member_initializer() {
			return getRuleContexts(Member_initializerContext.class);
		}
		public Member_initializerContext member_initializer(int i) {
			return getRuleContext(Member_initializerContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Member_initializer_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_member_initializer_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMember_initializer_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMember_initializer_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMember_initializer_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Member_initializer_listContext member_initializer_list() throws RecognitionException {
		Member_initializer_listContext _localctx = new Member_initializer_listContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_member_initializer_list);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(982);
			member_initializer();
			setState(987);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(983);
					match(COMMA);
					setState(984);
					member_initializer();
					}
					} 
				}
				setState(989);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Member_initializerContext extends ParserRuleContext {
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public Initializer_valueContext initializer_value() {
			return getRuleContext(Initializer_valueContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public Member_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_member_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMember_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMember_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMember_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Member_initializerContext member_initializer() throws RecognitionException {
		Member_initializerContext _localctx = new Member_initializerContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_member_initializer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(995);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BY:
			case DESCENDING:
			case DYNAMIC:
			case EQUALS:
			case FROM:
			case GET:
			case GROUP:
			case INTO:
			case JOIN:
			case LET:
			case NAMEOF:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REMOVE:
			case SELECT:
			case SET:
			case UNMANAGED:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
				{
				setState(990);
				identifier();
				}
				break;
			case OPEN_BRACKET:
				{
				setState(991);
				match(OPEN_BRACKET);
				setState(992);
				expression();
				setState(993);
				match(CLOSE_BRACKET);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(997);
			match(ASSIGNMENT);
			setState(998);
			initializer_value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Initializer_valueContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Object_or_collection_initializerContext object_or_collection_initializer() {
			return getRuleContext(Object_or_collection_initializerContext.class,0);
		}
		public Initializer_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_initializer_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInitializer_value(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInitializer_value(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInitializer_value(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Initializer_valueContext initializer_value() throws RecognitionException {
		Initializer_valueContext _localctx = new Initializer_valueContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_initializer_value);
		try {
			setState(1002);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1000);
				expression();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1001);
				object_or_collection_initializer();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Collection_initializerContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public List<Element_initializerContext> element_initializer() {
			return getRuleContexts(Element_initializerContext.class);
		}
		public Element_initializerContext element_initializer(int i) {
			return getRuleContext(Element_initializerContext.class,i);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Collection_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_collection_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCollection_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCollection_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCollection_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Collection_initializerContext collection_initializer() throws RecognitionException {
		Collection_initializerContext _localctx = new Collection_initializerContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_collection_initializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1004);
			match(OPEN_BRACE);
			setState(1005);
			element_initializer();
			setState(1010);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1006);
					match(COMMA);
					setState(1007);
					element_initializer();
					}
					} 
				}
				setState(1012);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
			}
			setState(1014);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1013);
				match(COMMA);
				}
			}

			setState(1016);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Element_initializerContext extends ParserRuleContext {
		public Non_assignment_expressionContext non_assignment_expression() {
			return getRuleContext(Non_assignment_expressionContext.class,0);
		}
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public Expression_listContext expression_list() {
			return getRuleContext(Expression_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Element_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_element_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterElement_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitElement_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitElement_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Element_initializerContext element_initializer() throws RecognitionException {
		Element_initializerContext _localctx = new Element_initializerContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_element_initializer);
		try {
			setState(1023);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1018);
				non_assignment_expression();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1019);
				match(OPEN_BRACE);
				setState(1020);
				expression_list();
				setState(1021);
				match(CLOSE_BRACE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Anonymous_object_initializerContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Member_declarator_listContext member_declarator_list() {
			return getRuleContext(Member_declarator_listContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(CSharpParser.COMMA, 0); }
		public Anonymous_object_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anonymous_object_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAnonymous_object_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAnonymous_object_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAnonymous_object_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Anonymous_object_initializerContext anonymous_object_initializer() throws RecognitionException {
		Anonymous_object_initializerContext _localctx = new Anonymous_object_initializerContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_anonymous_object_initializer);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1025);
			match(OPEN_BRACE);
			setState(1030);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 3417425939254529527L) != 0) || ((((_la - 75)) & ~0x3f) == 0 && ((1L << (_la - 75)) & 19140129564736833L) != 0)) {
				{
				setState(1026);
				member_declarator_list();
				setState(1028);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(1027);
					match(COMMA);
					}
				}

				}
			}

			setState(1032);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Member_declarator_listContext extends ParserRuleContext {
		public List<Member_declaratorContext> member_declarator() {
			return getRuleContexts(Member_declaratorContext.class);
		}
		public Member_declaratorContext member_declarator(int i) {
			return getRuleContext(Member_declaratorContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Member_declarator_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_member_declarator_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMember_declarator_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMember_declarator_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMember_declarator_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Member_declarator_listContext member_declarator_list() throws RecognitionException {
		Member_declarator_listContext _localctx = new Member_declarator_listContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_member_declarator_list);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1034);
			member_declarator();
			setState(1039);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1035);
					match(COMMA);
					setState(1036);
					member_declarator();
					}
					} 
				}
				setState(1041);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Member_declaratorContext extends ParserRuleContext {
		public Primary_expressionContext primary_expression() {
			return getRuleContext(Primary_expressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Member_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_member_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMember_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMember_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMember_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Member_declaratorContext member_declarator() throws RecognitionException {
		Member_declaratorContext _localctx = new Member_declaratorContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_member_declarator);
		try {
			setState(1047);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,94,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1042);
				primary_expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1043);
				identifier();
				setState(1044);
				match(ASSIGNMENT);
				setState(1045);
				expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Unbound_type_nameContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode DOUBLE_COLON() { return getToken(CSharpParser.DOUBLE_COLON, 0); }
		public List<TerminalNode> DOT() { return getTokens(CSharpParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(CSharpParser.DOT, i);
		}
		public List<Generic_dimension_specifierContext> generic_dimension_specifier() {
			return getRuleContexts(Generic_dimension_specifierContext.class);
		}
		public Generic_dimension_specifierContext generic_dimension_specifier(int i) {
			return getRuleContext(Generic_dimension_specifierContext.class,i);
		}
		public Unbound_type_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unbound_type_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUnbound_type_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUnbound_type_name(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUnbound_type_name(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Unbound_type_nameContext unbound_type_name() throws RecognitionException {
		Unbound_type_nameContext _localctx = new Unbound_type_nameContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_unbound_type_name);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1049);
			identifier();
			setState(1058);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CLOSE_PARENS:
			case DOT:
			case LT:
				{
				setState(1051);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1050);
					generic_dimension_specifier();
					}
				}

				}
				break;
			case DOUBLE_COLON:
				{
				setState(1053);
				match(DOUBLE_COLON);
				setState(1054);
				identifier();
				setState(1056);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1055);
					generic_dimension_specifier();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1067);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(1060);
				match(DOT);
				setState(1061);
				identifier();
				setState(1063);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1062);
					generic_dimension_specifier();
					}
				}

				}
				}
				setState(1069);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Generic_dimension_specifierContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(CSharpParser.LT, 0); }
		public TerminalNode GT() { return getToken(CSharpParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Generic_dimension_specifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_generic_dimension_specifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterGeneric_dimension_specifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitGeneric_dimension_specifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitGeneric_dimension_specifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Generic_dimension_specifierContext generic_dimension_specifier() throws RecognitionException {
		Generic_dimension_specifierContext _localctx = new Generic_dimension_specifierContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_generic_dimension_specifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1070);
			match(LT);
			setState(1074);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1071);
				match(COMMA);
				}
				}
				setState(1076);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1077);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IsTypeContext extends ParserRuleContext {
		public Base_typeContext base_type() {
			return getRuleContext(Base_typeContext.class,0);
		}
		public List<Rank_specifierContext> rank_specifier() {
			return getRuleContexts(Rank_specifierContext.class);
		}
		public Rank_specifierContext rank_specifier(int i) {
			return getRuleContext(Rank_specifierContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(CSharpParser.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(CSharpParser.STAR, i);
		}
		public TerminalNode INTERR() { return getToken(CSharpParser.INTERR, 0); }
		public IsTypePatternArmsContext isTypePatternArms() {
			return getRuleContext(IsTypePatternArmsContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public IsTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_isType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIsType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIsType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIsType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IsTypeContext isType() throws RecognitionException {
		IsTypeContext _localctx = new IsTypeContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_isType);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1079);
			base_type();
			setState(1084);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(1082);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case OPEN_BRACKET:
						{
						setState(1080);
						rank_specifier();
						}
						break;
					case STAR:
						{
						setState(1081);
						match(STAR);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(1086);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
			}
			setState(1088);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,103,_ctx) ) {
			case 1:
				{
				setState(1087);
				match(INTERR);
				}
				break;
			}
			setState(1091);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,104,_ctx) ) {
			case 1:
				{
				setState(1090);
				isTypePatternArms();
				}
				break;
			}
			setState(1094);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
			case 1:
				{
				setState(1093);
				identifier();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IsTypePatternArmsContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public List<IsTypePatternArmContext> isTypePatternArm() {
			return getRuleContexts(IsTypePatternArmContext.class);
		}
		public IsTypePatternArmContext isTypePatternArm(int i) {
			return getRuleContext(IsTypePatternArmContext.class,i);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public IsTypePatternArmsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_isTypePatternArms; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIsTypePatternArms(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIsTypePatternArms(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIsTypePatternArms(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IsTypePatternArmsContext isTypePatternArms() throws RecognitionException {
		IsTypePatternArmsContext _localctx = new IsTypePatternArmsContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_isTypePatternArms);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1096);
			match(OPEN_BRACE);
			setState(1097);
			isTypePatternArm();
			setState(1102);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1098);
				match(COMMA);
				setState(1099);
				isTypePatternArm();
				}
				}
				setState(1104);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1105);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IsTypePatternArmContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IsTypePatternArmContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_isTypePatternArm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIsTypePatternArm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIsTypePatternArm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIsTypePatternArm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IsTypePatternArmContext isTypePatternArm() throws RecognitionException {
		IsTypePatternArmContext _localctx = new IsTypePatternArmContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_isTypePatternArm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1107);
			identifier();
			setState(1108);
			match(COLON);
			setState(1109);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Lambda_expressionContext extends ParserRuleContext {
		public Anonymous_function_signatureContext anonymous_function_signature() {
			return getRuleContext(Anonymous_function_signatureContext.class,0);
		}
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Anonymous_function_bodyContext anonymous_function_body() {
			return getRuleContext(Anonymous_function_bodyContext.class,0);
		}
		public TerminalNode ASYNC() { return getToken(CSharpParser.ASYNC, 0); }
		public Lambda_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambda_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLambda_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLambda_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLambda_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Lambda_expressionContext lambda_expression() throws RecognitionException {
		Lambda_expressionContext _localctx = new Lambda_expressionContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_lambda_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1112);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,107,_ctx) ) {
			case 1:
				{
				setState(1111);
				match(ASYNC);
				}
				break;
			}
			setState(1114);
			anonymous_function_signature();
			setState(1115);
			right_arrow();
			setState(1116);
			anonymous_function_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Anonymous_function_signatureContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Explicit_anonymous_function_parameter_listContext explicit_anonymous_function_parameter_list() {
			return getRuleContext(Explicit_anonymous_function_parameter_listContext.class,0);
		}
		public Implicit_anonymous_function_parameter_listContext implicit_anonymous_function_parameter_list() {
			return getRuleContext(Implicit_anonymous_function_parameter_listContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Anonymous_function_signatureContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anonymous_function_signature; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAnonymous_function_signature(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAnonymous_function_signature(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAnonymous_function_signature(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Anonymous_function_signatureContext anonymous_function_signature() throws RecognitionException {
		Anonymous_function_signatureContext _localctx = new Anonymous_function_signatureContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_anonymous_function_signature);
		try {
			setState(1129);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,108,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1118);
				match(OPEN_PARENS);
				setState(1119);
				match(CLOSE_PARENS);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1120);
				match(OPEN_PARENS);
				setState(1121);
				explicit_anonymous_function_parameter_list();
				setState(1122);
				match(CLOSE_PARENS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1124);
				match(OPEN_PARENS);
				setState(1125);
				implicit_anonymous_function_parameter_list();
				setState(1126);
				match(CLOSE_PARENS);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1128);
				identifier();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Explicit_anonymous_function_parameter_listContext extends ParserRuleContext {
		public List<Explicit_anonymous_function_parameterContext> explicit_anonymous_function_parameter() {
			return getRuleContexts(Explicit_anonymous_function_parameterContext.class);
		}
		public Explicit_anonymous_function_parameterContext explicit_anonymous_function_parameter(int i) {
			return getRuleContext(Explicit_anonymous_function_parameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Explicit_anonymous_function_parameter_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_explicit_anonymous_function_parameter_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExplicit_anonymous_function_parameter_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExplicit_anonymous_function_parameter_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExplicit_anonymous_function_parameter_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Explicit_anonymous_function_parameter_listContext explicit_anonymous_function_parameter_list() throws RecognitionException {
		Explicit_anonymous_function_parameter_listContext _localctx = new Explicit_anonymous_function_parameter_listContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_explicit_anonymous_function_parameter_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1131);
			explicit_anonymous_function_parameter();
			setState(1136);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1132);
				match(COMMA);
				setState(1133);
				explicit_anonymous_function_parameter();
				}
				}
				setState(1138);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Explicit_anonymous_function_parameterContext extends ParserRuleContext {
		public Token refout;
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public TerminalNode OUT() { return getToken(CSharpParser.OUT, 0); }
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public Explicit_anonymous_function_parameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_explicit_anonymous_function_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExplicit_anonymous_function_parameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExplicit_anonymous_function_parameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExplicit_anonymous_function_parameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Explicit_anonymous_function_parameterContext explicit_anonymous_function_parameter() throws RecognitionException {
		Explicit_anonymous_function_parameterContext _localctx = new Explicit_anonymous_function_parameterContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_explicit_anonymous_function_parameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1140);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 54)) & ~0x3f) == 0 && ((1L << (_la - 54)) & 67371009L) != 0)) {
				{
				setState(1139);
				((Explicit_anonymous_function_parameterContext)_localctx).refout = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 54)) & ~0x3f) == 0 && ((1L << (_la - 54)) & 67371009L) != 0)) ) {
					((Explicit_anonymous_function_parameterContext)_localctx).refout = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(1142);
			type_();
			setState(1143);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Implicit_anonymous_function_parameter_listContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Implicit_anonymous_function_parameter_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_implicit_anonymous_function_parameter_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterImplicit_anonymous_function_parameter_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitImplicit_anonymous_function_parameter_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitImplicit_anonymous_function_parameter_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Implicit_anonymous_function_parameter_listContext implicit_anonymous_function_parameter_list() throws RecognitionException {
		Implicit_anonymous_function_parameter_listContext _localctx = new Implicit_anonymous_function_parameter_listContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_implicit_anonymous_function_parameter_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1145);
			identifier();
			setState(1150);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1146);
				match(COMMA);
				setState(1147);
				identifier();
				}
				}
				setState(1152);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Anonymous_function_bodyContext extends ParserRuleContext {
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Anonymous_function_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anonymous_function_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAnonymous_function_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAnonymous_function_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAnonymous_function_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Anonymous_function_bodyContext anonymous_function_body() throws RecognitionException {
		Anonymous_function_bodyContext _localctx = new Anonymous_function_bodyContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_anonymous_function_body);
		try {
			setState(1155);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case THROW:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1153);
				throwable_expression();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1154);
				block();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Query_expressionContext extends ParserRuleContext {
		public From_clauseContext from_clause() {
			return getRuleContext(From_clauseContext.class,0);
		}
		public Query_bodyContext query_body() {
			return getRuleContext(Query_bodyContext.class,0);
		}
		public Query_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterQuery_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitQuery_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitQuery_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Query_expressionContext query_expression() throws RecognitionException {
		Query_expressionContext _localctx = new Query_expressionContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_query_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1157);
			from_clause();
			setState(1158);
			query_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class From_clauseContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(CSharpParser.FROM, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public From_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_from_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFrom_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFrom_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFrom_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final From_clauseContext from_clause() throws RecognitionException {
		From_clauseContext _localctx = new From_clauseContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_from_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1160);
			match(FROM);
			setState(1162);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
			case 1:
				{
				setState(1161);
				type_();
				}
				break;
			}
			setState(1164);
			identifier();
			setState(1165);
			match(IN);
			setState(1166);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Query_bodyContext extends ParserRuleContext {
		public Select_or_group_clauseContext select_or_group_clause() {
			return getRuleContext(Select_or_group_clauseContext.class,0);
		}
		public List<Query_body_clauseContext> query_body_clause() {
			return getRuleContexts(Query_body_clauseContext.class);
		}
		public Query_body_clauseContext query_body_clause(int i) {
			return getRuleContext(Query_body_clauseContext.class,i);
		}
		public Query_continuationContext query_continuation() {
			return getRuleContext(Query_continuationContext.class,0);
		}
		public Query_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterQuery_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitQuery_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitQuery_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Query_bodyContext query_body() throws RecognitionException {
		Query_bodyContext _localctx = new Query_bodyContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_query_body);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1171);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 48)) & ~0x3f) == 0 && ((1L << (_la - 48)) & -9223372036846374911L) != 0)) {
				{
				{
				setState(1168);
				query_body_clause();
				}
				}
				setState(1173);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1174);
			select_or_group_clause();
			setState(1176);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,115,_ctx) ) {
			case 1:
				{
				setState(1175);
				query_continuation();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Query_body_clauseContext extends ParserRuleContext {
		public From_clauseContext from_clause() {
			return getRuleContext(From_clauseContext.class,0);
		}
		public Let_clauseContext let_clause() {
			return getRuleContext(Let_clauseContext.class,0);
		}
		public Where_clauseContext where_clause() {
			return getRuleContext(Where_clauseContext.class,0);
		}
		public Combined_join_clauseContext combined_join_clause() {
			return getRuleContext(Combined_join_clauseContext.class,0);
		}
		public Orderby_clauseContext orderby_clause() {
			return getRuleContext(Orderby_clauseContext.class,0);
		}
		public Query_body_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query_body_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterQuery_body_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitQuery_body_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitQuery_body_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Query_body_clauseContext query_body_clause() throws RecognitionException {
		Query_body_clauseContext _localctx = new Query_body_clauseContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_query_body_clause);
		try {
			setState(1183);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FROM:
				enterOuterAlt(_localctx, 1);
				{
				setState(1178);
				from_clause();
				}
				break;
			case LET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1179);
				let_clause();
				}
				break;
			case WHERE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1180);
				where_clause();
				}
				break;
			case JOIN:
				enterOuterAlt(_localctx, 4);
				{
				setState(1181);
				combined_join_clause();
				}
				break;
			case ORDERBY:
				enterOuterAlt(_localctx, 5);
				{
				setState(1182);
				orderby_clause();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Let_clauseContext extends ParserRuleContext {
		public TerminalNode LET() { return getToken(CSharpParser.LET, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Let_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_let_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLet_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLet_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLet_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Let_clauseContext let_clause() throws RecognitionException {
		Let_clauseContext _localctx = new Let_clauseContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_let_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1185);
			match(LET);
			setState(1186);
			identifier();
			setState(1187);
			match(ASSIGNMENT);
			setState(1188);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Where_clauseContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(CSharpParser.WHERE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Where_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_where_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterWhere_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitWhere_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitWhere_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Where_clauseContext where_clause() throws RecognitionException {
		Where_clauseContext _localctx = new Where_clauseContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_where_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1190);
			match(WHERE);
			setState(1191);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Combined_join_clauseContext extends ParserRuleContext {
		public TerminalNode JOIN() { return getToken(CSharpParser.JOIN, 0); }
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode ON() { return getToken(CSharpParser.ON, 0); }
		public TerminalNode EQUALS() { return getToken(CSharpParser.EQUALS, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode INTO() { return getToken(CSharpParser.INTO, 0); }
		public Combined_join_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_combined_join_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCombined_join_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCombined_join_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCombined_join_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Combined_join_clauseContext combined_join_clause() throws RecognitionException {
		Combined_join_clauseContext _localctx = new Combined_join_clauseContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_combined_join_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1193);
			match(JOIN);
			setState(1195);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,117,_ctx) ) {
			case 1:
				{
				setState(1194);
				type_();
				}
				break;
			}
			setState(1197);
			identifier();
			setState(1198);
			match(IN);
			setState(1199);
			expression();
			setState(1200);
			match(ON);
			setState(1201);
			expression();
			setState(1202);
			match(EQUALS);
			setState(1203);
			expression();
			setState(1206);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==INTO) {
				{
				setState(1204);
				match(INTO);
				setState(1205);
				identifier();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Orderby_clauseContext extends ParserRuleContext {
		public TerminalNode ORDERBY() { return getToken(CSharpParser.ORDERBY, 0); }
		public List<OrderingContext> ordering() {
			return getRuleContexts(OrderingContext.class);
		}
		public OrderingContext ordering(int i) {
			return getRuleContext(OrderingContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Orderby_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderby_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterOrderby_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitOrderby_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitOrderby_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Orderby_clauseContext orderby_clause() throws RecognitionException {
		Orderby_clauseContext _localctx = new Orderby_clauseContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_orderby_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1208);
			match(ORDERBY);
			setState(1209);
			ordering();
			setState(1214);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1210);
				match(COMMA);
				setState(1211);
				ordering();
				}
				}
				setState(1216);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrderingContext extends ParserRuleContext {
		public Token dir;
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode ASCENDING() { return getToken(CSharpParser.ASCENDING, 0); }
		public TerminalNode DESCENDING() { return getToken(CSharpParser.DESCENDING, 0); }
		public OrderingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ordering; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterOrdering(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitOrdering(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitOrdering(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderingContext ordering() throws RecognitionException {
		OrderingContext _localctx = new OrderingContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_ordering);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1217);
			expression();
			setState(1219);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASCENDING || _la==DESCENDING) {
				{
				setState(1218);
				((OrderingContext)_localctx).dir = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ASCENDING || _la==DESCENDING) ) {
					((OrderingContext)_localctx).dir = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Select_or_group_clauseContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(CSharpParser.SELECT, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode GROUP() { return getToken(CSharpParser.GROUP, 0); }
		public TerminalNode BY() { return getToken(CSharpParser.BY, 0); }
		public Select_or_group_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_select_or_group_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSelect_or_group_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSelect_or_group_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSelect_or_group_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Select_or_group_clauseContext select_or_group_clause() throws RecognitionException {
		Select_or_group_clauseContext _localctx = new Select_or_group_clauseContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_select_or_group_clause);
		try {
			setState(1228);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SELECT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1221);
				match(SELECT);
				setState(1222);
				expression();
				}
				break;
			case GROUP:
				enterOuterAlt(_localctx, 2);
				{
				setState(1223);
				match(GROUP);
				setState(1224);
				expression();
				setState(1225);
				match(BY);
				setState(1226);
				expression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Query_continuationContext extends ParserRuleContext {
		public TerminalNode INTO() { return getToken(CSharpParser.INTO, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Query_bodyContext query_body() {
			return getRuleContext(Query_bodyContext.class,0);
		}
		public Query_continuationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query_continuation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterQuery_continuation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitQuery_continuation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitQuery_continuation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Query_continuationContext query_continuation() throws RecognitionException {
		Query_continuationContext _localctx = new Query_continuationContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_query_continuation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1230);
			match(INTO);
			setState(1231);
			identifier();
			setState(1232);
			query_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public Labeled_StatementContext labeled_Statement() {
			return getRuleContext(Labeled_StatementContext.class,0);
		}
		public DeclarationStatementContext declarationStatement() {
			return getRuleContext(DeclarationStatementContext.class,0);
		}
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_statement);
		try {
			setState(1237);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,122,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1234);
				labeled_Statement();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1235);
				declarationStatement();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1236);
				embedded_statement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeclarationStatementContext extends ParserRuleContext {
		public Local_variable_declarationContext local_variable_declaration() {
			return getRuleContext(Local_variable_declarationContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Local_constant_declarationContext local_constant_declaration() {
			return getRuleContext(Local_constant_declarationContext.class,0);
		}
		public Local_function_declarationContext local_function_declaration() {
			return getRuleContext(Local_function_declarationContext.class,0);
		}
		public DeclarationStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_declarationStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterDeclarationStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitDeclarationStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitDeclarationStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclarationStatementContext declarationStatement() throws RecognitionException {
		DeclarationStatementContext _localctx = new DeclarationStatementContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_declarationStatement);
		try {
			setState(1246);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,123,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1239);
				local_variable_declaration();
				setState(1240);
				match(SEMICOLON);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1242);
				local_constant_declaration();
				setState(1243);
				match(SEMICOLON);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1245);
				local_function_declaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_function_declarationContext extends ParserRuleContext {
		public Local_function_headerContext local_function_header() {
			return getRuleContext(Local_function_headerContext.class,0);
		}
		public Local_function_bodyContext local_function_body() {
			return getRuleContext(Local_function_bodyContext.class,0);
		}
		public Local_function_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_function_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_function_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_function_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_function_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_function_declarationContext local_function_declaration() throws RecognitionException {
		Local_function_declarationContext _localctx = new Local_function_declarationContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_local_function_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1248);
			local_function_header();
			setState(1249);
			local_function_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_function_headerContext extends ParserRuleContext {
		public Return_typeContext return_type() {
			return getRuleContext(Return_typeContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Local_function_modifiersContext local_function_modifiers() {
			return getRuleContext(Local_function_modifiersContext.class,0);
		}
		public Type_parameter_listContext type_parameter_list() {
			return getRuleContext(Type_parameter_listContext.class,0);
		}
		public Formal_parameter_listContext formal_parameter_list() {
			return getRuleContext(Formal_parameter_listContext.class,0);
		}
		public Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() {
			return getRuleContext(Type_parameter_constraints_clausesContext.class,0);
		}
		public Local_function_headerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_function_header; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_function_header(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_function_header(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_function_header(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_function_headerContext local_function_header() throws RecognitionException {
		Local_function_headerContext _localctx = new Local_function_headerContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_local_function_header);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1252);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,124,_ctx) ) {
			case 1:
				{
				setState(1251);
				local_function_modifiers();
				}
				break;
			}
			setState(1254);
			return_type();
			setState(1255);
			identifier();
			setState(1257);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1256);
				type_parameter_list();
				}
			}

			setState(1259);
			match(OPEN_PARENS);
			setState(1261);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 7812956763456032119L) != 0) || ((((_la - 74)) & ~0x3f) == 0 && ((1L << (_la - 74)) & 45037874618448579L) != 0)) {
				{
				setState(1260);
				formal_parameter_list();
				}
			}

			setState(1263);
			match(CLOSE_PARENS);
			setState(1265);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(1264);
				type_parameter_constraints_clauses();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_function_modifiersContext extends ParserRuleContext {
		public TerminalNode ASYNC() { return getToken(CSharpParser.ASYNC, 0); }
		public TerminalNode UNSAFE() { return getToken(CSharpParser.UNSAFE, 0); }
		public TerminalNode STATIC() { return getToken(CSharpParser.STATIC, 0); }
		public Local_function_modifiersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_function_modifiers; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_function_modifiers(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_function_modifiers(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_function_modifiers(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_function_modifiersContext local_function_modifiers() throws RecognitionException {
		Local_function_modifiersContext _localctx = new Local_function_modifiersContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_local_function_modifiers);
		int _la;
		try {
			setState(1273);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ASYNC:
			case UNSAFE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1267);
				_la = _input.LA(1);
				if ( !(_la==ASYNC || _la==UNSAFE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1269);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(1268);
					match(STATIC);
					}
				}

				}
				break;
			case STATIC:
				enterOuterAlt(_localctx, 2);
				{
				setState(1271);
				match(STATIC);
				setState(1272);
				_la = _input.LA(1);
				if ( !(_la==ASYNC || _la==UNSAFE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_function_bodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Local_function_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_function_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_function_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_function_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_function_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_function_bodyContext local_function_body() throws RecognitionException {
		Local_function_bodyContext _localctx = new Local_function_bodyContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_local_function_body);
		try {
			setState(1280);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1275);
				block();
				}
				break;
			case ASSIGNMENT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1276);
				right_arrow();
				setState(1277);
				throwable_expression();
				setState(1278);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Labeled_StatementContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public Labeled_StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labeled_Statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLabeled_Statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLabeled_Statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLabeled_Statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Labeled_StatementContext labeled_Statement() throws RecognitionException {
		Labeled_StatementContext _localctx = new Labeled_StatementContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_labeled_Statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1282);
			identifier();
			setState(1283);
			match(COLON);
			setState(1284);
			statement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Embedded_statementContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Simple_embedded_statementContext simple_embedded_statement() {
			return getRuleContext(Simple_embedded_statementContext.class,0);
		}
		public Embedded_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_embedded_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEmbedded_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEmbedded_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEmbedded_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Embedded_statementContext embedded_statement() throws RecognitionException {
		Embedded_statementContext _localctx = new Embedded_statementContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_embedded_statement);
		try {
			setState(1288);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1286);
				block();
				}
				break;
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BREAK:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case CONTINUE:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DO:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FIXED:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case GET:
			case GOTO:
			case GROUP:
			case IF:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LOCK:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case RETURN:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case SWITCH:
			case THIS:
			case THROW:
			case TRUE:
			case TRY:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case UNSAFE:
			case USHORT:
			case USING:
			case VAR:
			case WHEN:
			case WHERE:
			case WHILE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case SEMICOLON:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1287);
				simple_embedded_statement();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Simple_embedded_statementContext extends ParserRuleContext {
		public Simple_embedded_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simple_embedded_statement; }
	 
		public Simple_embedded_statementContext() { }
		public void copyFrom(Simple_embedded_statementContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TryStatementContext extends Simple_embedded_statementContext {
		public TerminalNode TRY() { return getToken(CSharpParser.TRY, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Catch_clausesContext catch_clauses() {
			return getRuleContext(Catch_clausesContext.class,0);
		}
		public Finally_clauseContext finally_clause() {
			return getRuleContext(Finally_clauseContext.class,0);
		}
		public TryStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterTryStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitTryStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitTryStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CheckedStatementContext extends Simple_embedded_statementContext {
		public TerminalNode CHECKED() { return getToken(CSharpParser.CHECKED, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public CheckedStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCheckedStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCheckedStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCheckedStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ThrowStatementContext extends Simple_embedded_statementContext {
		public TerminalNode THROW() { return getToken(CSharpParser.THROW, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ThrowStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterThrowStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitThrowStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitThrowStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TheEmptyStatementContext extends Simple_embedded_statementContext {
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public TheEmptyStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterTheEmptyStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitTheEmptyStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitTheEmptyStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UnsafeStatementContext extends Simple_embedded_statementContext {
		public TerminalNode UNSAFE() { return getToken(CSharpParser.UNSAFE, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public UnsafeStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUnsafeStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUnsafeStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUnsafeStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ForStatementContext extends Simple_embedded_statementContext {
		public TerminalNode FOR() { return getToken(CSharpParser.FOR, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public List<TerminalNode> SEMICOLON() { return getTokens(CSharpParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(CSharpParser.SEMICOLON, i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public For_initializerContext for_initializer() {
			return getRuleContext(For_initializerContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public For_iteratorContext for_iterator() {
			return getRuleContext(For_iteratorContext.class,0);
		}
		public ForStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterForStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitForStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitForStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BreakStatementContext extends Simple_embedded_statementContext {
		public TerminalNode BREAK() { return getToken(CSharpParser.BREAK, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public BreakStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterBreakStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitBreakStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitBreakStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IfStatementContext extends Simple_embedded_statementContext {
		public TerminalNode IF() { return getToken(CSharpParser.IF, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public List<If_bodyContext> if_body() {
			return getRuleContexts(If_bodyContext.class);
		}
		public If_bodyContext if_body(int i) {
			return getRuleContext(If_bodyContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(CSharpParser.ELSE, 0); }
		public IfStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIfStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIfStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIfStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ReturnStatementContext extends Simple_embedded_statementContext {
		public TerminalNode RETURN() { return getToken(CSharpParser.RETURN, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ReturnStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterReturnStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitReturnStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitReturnStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class GotoStatementContext extends Simple_embedded_statementContext {
		public TerminalNode GOTO() { return getToken(CSharpParser.GOTO, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode CASE() { return getToken(CSharpParser.CASE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode DEFAULT() { return getToken(CSharpParser.DEFAULT, 0); }
		public GotoStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterGotoStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitGotoStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitGotoStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SwitchStatementContext extends Simple_embedded_statementContext {
		public TerminalNode SWITCH() { return getToken(CSharpParser.SWITCH, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<Switch_sectionContext> switch_section() {
			return getRuleContexts(Switch_sectionContext.class);
		}
		public Switch_sectionContext switch_section(int i) {
			return getRuleContext(Switch_sectionContext.class,i);
		}
		public SwitchStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSwitchStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSwitchStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSwitchStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FixedStatementContext extends Simple_embedded_statementContext {
		public TerminalNode FIXED() { return getToken(CSharpParser.FIXED, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Pointer_typeContext pointer_type() {
			return getRuleContext(Pointer_typeContext.class,0);
		}
		public Fixed_pointer_declaratorsContext fixed_pointer_declarators() {
			return getRuleContext(Fixed_pointer_declaratorsContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public FixedStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFixedStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFixedStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFixedStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhileStatementContext extends Simple_embedded_statementContext {
		public TerminalNode WHILE() { return getToken(CSharpParser.WHILE, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public WhileStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterWhileStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitWhileStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitWhileStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DoStatementContext extends Simple_embedded_statementContext {
		public TerminalNode DO() { return getToken(CSharpParser.DO, 0); }
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public TerminalNode WHILE() { return getToken(CSharpParser.WHILE, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public DoStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterDoStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitDoStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitDoStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ForeachStatementContext extends Simple_embedded_statementContext {
		public TerminalNode FOREACH() { return getToken(CSharpParser.FOREACH, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Local_variable_typeContext local_variable_type() {
			return getRuleContext(Local_variable_typeContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public TerminalNode AWAIT() { return getToken(CSharpParser.AWAIT, 0); }
		public ForeachStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterForeachStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitForeachStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitForeachStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UncheckedStatementContext extends Simple_embedded_statementContext {
		public TerminalNode UNCHECKED() { return getToken(CSharpParser.UNCHECKED, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public UncheckedStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUncheckedStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUncheckedStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUncheckedStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionStatementContext extends Simple_embedded_statementContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public ExpressionStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExpressionStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExpressionStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExpressionStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ContinueStatementContext extends Simple_embedded_statementContext {
		public TerminalNode CONTINUE() { return getToken(CSharpParser.CONTINUE, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public ContinueStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterContinueStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitContinueStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitContinueStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UsingStatementContext extends Simple_embedded_statementContext {
		public TerminalNode USING() { return getToken(CSharpParser.USING, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Resource_acquisitionContext resource_acquisition() {
			return getRuleContext(Resource_acquisitionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public UsingStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUsingStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUsingStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUsingStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LockStatementContext extends Simple_embedded_statementContext {
		public TerminalNode LOCK() { return getToken(CSharpParser.LOCK, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Embedded_statementContext embedded_statement() {
			return getRuleContext(Embedded_statementContext.class,0);
		}
		public LockStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLockStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLockStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLockStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class YieldStatementContext extends Simple_embedded_statementContext {
		public TerminalNode YIELD() { return getToken(CSharpParser.YIELD, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public TerminalNode RETURN() { return getToken(CSharpParser.RETURN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode BREAK() { return getToken(CSharpParser.BREAK, 0); }
		public YieldStatementContext(Simple_embedded_statementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterYieldStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitYieldStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitYieldStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Simple_embedded_statementContext simple_embedded_statement() throws RecognitionException {
		Simple_embedded_statementContext _localctx = new Simple_embedded_statementContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_simple_embedded_statement);
		int _la;
		try {
			setState(1420);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
			case 1:
				_localctx = new TheEmptyStatementContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1290);
				match(SEMICOLON);
				}
				break;
			case 2:
				_localctx = new ExpressionStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1291);
				expression();
				setState(1292);
				match(SEMICOLON);
				}
				break;
			case 3:
				_localctx = new IfStatementContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1294);
				match(IF);
				setState(1295);
				match(OPEN_PARENS);
				setState(1296);
				expression();
				setState(1297);
				match(CLOSE_PARENS);
				setState(1298);
				if_body();
				setState(1301);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,132,_ctx) ) {
				case 1:
					{
					setState(1299);
					match(ELSE);
					setState(1300);
					if_body();
					}
					break;
				}
				}
				break;
			case 4:
				_localctx = new SwitchStatementContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1303);
				match(SWITCH);
				setState(1304);
				match(OPEN_PARENS);
				setState(1305);
				expression();
				setState(1306);
				match(CLOSE_PARENS);
				setState(1307);
				match(OPEN_BRACE);
				setState(1311);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CASE || _la==DEFAULT) {
					{
					{
					setState(1308);
					switch_section();
					}
					}
					setState(1313);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1314);
				match(CLOSE_BRACE);
				}
				break;
			case 5:
				_localctx = new WhileStatementContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1316);
				match(WHILE);
				setState(1317);
				match(OPEN_PARENS);
				setState(1318);
				expression();
				setState(1319);
				match(CLOSE_PARENS);
				setState(1320);
				embedded_statement();
				}
				break;
			case 6:
				_localctx = new DoStatementContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1322);
				match(DO);
				setState(1323);
				embedded_statement();
				setState(1324);
				match(WHILE);
				setState(1325);
				match(OPEN_PARENS);
				setState(1326);
				expression();
				setState(1327);
				match(CLOSE_PARENS);
				setState(1328);
				match(SEMICOLON);
				}
				break;
			case 7:
				_localctx = new ForStatementContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(1330);
				match(FOR);
				setState(1331);
				match(OPEN_PARENS);
				setState(1333);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437194615990526976L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305516992371296445L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(1332);
					for_initializer();
					}
				}

				setState(1335);
				match(SEMICOLON);
				setState(1337);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(1336);
					expression();
					}
				}

				setState(1339);
				match(SEMICOLON);
				setState(1341);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(1340);
					for_iterator();
					}
				}

				setState(1343);
				match(CLOSE_PARENS);
				setState(1344);
				embedded_statement();
				}
				break;
			case 8:
				_localctx = new ForeachStatementContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(1346);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AWAIT) {
					{
					setState(1345);
					match(AWAIT);
					}
				}

				setState(1348);
				match(FOREACH);
				setState(1349);
				match(OPEN_PARENS);
				setState(1350);
				local_variable_type();
				setState(1351);
				identifier();
				setState(1352);
				match(IN);
				setState(1353);
				expression();
				setState(1354);
				match(CLOSE_PARENS);
				setState(1355);
				embedded_statement();
				}
				break;
			case 9:
				_localctx = new BreakStatementContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(1357);
				match(BREAK);
				setState(1358);
				match(SEMICOLON);
				}
				break;
			case 10:
				_localctx = new ContinueStatementContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(1359);
				match(CONTINUE);
				setState(1360);
				match(SEMICOLON);
				}
				break;
			case 11:
				_localctx = new GotoStatementContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(1361);
				match(GOTO);
				setState(1366);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ADD:
				case ALIAS:
				case ARGLIST:
				case ASCENDING:
				case ASYNC:
				case AWAIT:
				case BY:
				case DESCENDING:
				case DYNAMIC:
				case EQUALS:
				case FROM:
				case GET:
				case GROUP:
				case INTO:
				case JOIN:
				case LET:
				case NAMEOF:
				case ON:
				case ORDERBY:
				case PARTIAL:
				case REMOVE:
				case SELECT:
				case SET:
				case UNMANAGED:
				case VAR:
				case WHEN:
				case WHERE:
				case YIELD:
				case IDENTIFIER:
					{
					setState(1362);
					identifier();
					}
					break;
				case CASE:
					{
					setState(1363);
					match(CASE);
					setState(1364);
					expression();
					}
					break;
				case DEFAULT:
					{
					setState(1365);
					match(DEFAULT);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1368);
				match(SEMICOLON);
				}
				break;
			case 12:
				_localctx = new ReturnStatementContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(1369);
				match(RETURN);
				setState(1371);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(1370);
					expression();
					}
				}

				setState(1373);
				match(SEMICOLON);
				}
				break;
			case 13:
				_localctx = new ThrowStatementContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(1374);
				match(THROW);
				setState(1376);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(1375);
					expression();
					}
				}

				setState(1378);
				match(SEMICOLON);
				}
				break;
			case 14:
				_localctx = new TryStatementContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(1379);
				match(TRY);
				setState(1380);
				block();
				setState(1386);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case CATCH:
					{
					setState(1381);
					catch_clauses();
					setState(1383);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==FINALLY) {
						{
						setState(1382);
						finally_clause();
						}
					}

					}
					break;
				case FINALLY:
					{
					setState(1385);
					finally_clause();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 15:
				_localctx = new CheckedStatementContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(1388);
				match(CHECKED);
				setState(1389);
				block();
				}
				break;
			case 16:
				_localctx = new UncheckedStatementContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(1390);
				match(UNCHECKED);
				setState(1391);
				block();
				}
				break;
			case 17:
				_localctx = new LockStatementContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(1392);
				match(LOCK);
				setState(1393);
				match(OPEN_PARENS);
				setState(1394);
				expression();
				setState(1395);
				match(CLOSE_PARENS);
				setState(1396);
				embedded_statement();
				}
				break;
			case 18:
				_localctx = new UsingStatementContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(1398);
				match(USING);
				setState(1399);
				match(OPEN_PARENS);
				setState(1400);
				resource_acquisition();
				setState(1401);
				match(CLOSE_PARENS);
				setState(1402);
				embedded_statement();
				}
				break;
			case 19:
				_localctx = new YieldStatementContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(1404);
				match(YIELD);
				setState(1408);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case RETURN:
					{
					setState(1405);
					match(RETURN);
					setState(1406);
					expression();
					}
					break;
				case BREAK:
					{
					setState(1407);
					match(BREAK);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1410);
				match(SEMICOLON);
				}
				break;
			case 20:
				_localctx = new UnsafeStatementContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(1411);
				match(UNSAFE);
				setState(1412);
				block();
				}
				break;
			case 21:
				_localctx = new FixedStatementContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(1413);
				match(FIXED);
				setState(1414);
				match(OPEN_PARENS);
				setState(1415);
				pointer_type();
				setState(1416);
				fixed_pointer_declarators();
				setState(1417);
				match(CLOSE_PARENS);
				setState(1418);
				embedded_statement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BlockContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Statement_listContext statement_list() {
			return getRuleContext(Statement_listContext.class,0);
		}
		public BlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockContext block() throws RecognitionException {
		BlockContext _localctx = new BlockContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1422);
			match(OPEN_BRACE);
			setState(1424);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -819667982803280896L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 4611642037659175101L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153889L) != 0)) {
				{
				setState(1423);
				statement_list();
				}
			}

			setState(1426);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_variable_declarationContext extends ParserRuleContext {
		public Local_variable_typeContext local_variable_type() {
			return getRuleContext(Local_variable_typeContext.class,0);
		}
		public List<Local_variable_declaratorContext> local_variable_declarator() {
			return getRuleContexts(Local_variable_declaratorContext.class);
		}
		public Local_variable_declaratorContext local_variable_declarator(int i) {
			return getRuleContext(Local_variable_declaratorContext.class,i);
		}
		public TerminalNode USING() { return getToken(CSharpParser.USING, 0); }
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public TerminalNode READONLY() { return getToken(CSharpParser.READONLY, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public TerminalNode FIXED() { return getToken(CSharpParser.FIXED, 0); }
		public Pointer_typeContext pointer_type() {
			return getRuleContext(Pointer_typeContext.class,0);
		}
		public Fixed_pointer_declaratorsContext fixed_pointer_declarators() {
			return getRuleContext(Fixed_pointer_declaratorsContext.class,0);
		}
		public Local_variable_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_variable_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_variable_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_variable_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_variable_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_variable_declarationContext local_variable_declaration() throws RecognitionException {
		Local_variable_declarationContext _localctx = new Local_variable_declarationContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_local_variable_declaration);
		int _la;
		try {
			setState(1449);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case DECIMAL:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case STRING:
			case UINT:
			case ULONG:
			case UNMANAGED:
			case USHORT:
			case USING:
			case VAR:
			case VOID:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case OPEN_PARENS:
				enterOuterAlt(_localctx, 1);
				{
				setState(1432);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,146,_ctx) ) {
				case 1:
					{
					setState(1428);
					match(USING);
					}
					break;
				case 2:
					{
					setState(1429);
					match(REF);
					}
					break;
				case 3:
					{
					setState(1430);
					match(REF);
					setState(1431);
					match(READONLY);
					}
					break;
				}
				setState(1434);
				local_variable_type();
				setState(1435);
				local_variable_declarator();
				setState(1442);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1436);
					match(COMMA);
					setState(1437);
					local_variable_declarator();
					setState(1438);
					if (!( this.IsLocalVariableDeclaration() )) throw new FailedPredicateException(this, " this.IsLocalVariableDeclaration() ");
					}
					}
					setState(1444);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case FIXED:
				enterOuterAlt(_localctx, 2);
				{
				setState(1445);
				match(FIXED);
				setState(1446);
				pointer_type();
				setState(1447);
				fixed_pointer_declarators();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_variable_typeContext extends ParserRuleContext {
		public TerminalNode VAR() { return getToken(CSharpParser.VAR, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public Local_variable_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_variable_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_variable_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_variable_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_variable_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_variable_typeContext local_variable_type() throws RecognitionException {
		Local_variable_typeContext _localctx = new Local_variable_typeContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_local_variable_type);
		try {
			setState(1453);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,149,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1451);
				match(VAR);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1452);
				type_();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_variable_declaratorContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public Local_variable_initializerContext local_variable_initializer() {
			return getRuleContext(Local_variable_initializerContext.class,0);
		}
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public Local_variable_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_variable_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_variable_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_variable_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_variable_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_variable_declaratorContext local_variable_declarator() throws RecognitionException {
		Local_variable_declaratorContext _localctx = new Local_variable_declaratorContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_local_variable_declarator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1455);
			identifier();
			setState(1461);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,151,_ctx) ) {
			case 1:
				{
				setState(1456);
				match(ASSIGNMENT);
				setState(1458);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,150,_ctx) ) {
				case 1:
					{
					setState(1457);
					match(REF);
					}
					break;
				}
				setState(1460);
				local_variable_initializer();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_variable_initializerContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Array_initializerContext array_initializer() {
			return getRuleContext(Array_initializerContext.class,0);
		}
		public Stackalloc_initializerContext stackalloc_initializer() {
			return getRuleContext(Stackalloc_initializerContext.class,0);
		}
		public Local_variable_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_variable_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_variable_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_variable_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_variable_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_variable_initializerContext local_variable_initializer() throws RecognitionException {
		Local_variable_initializerContext _localctx = new Local_variable_initializerContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_local_variable_initializer);
		try {
			setState(1466);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1463);
				expression();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1464);
				array_initializer();
				}
				break;
			case STACKALLOC:
				enterOuterAlt(_localctx, 3);
				{
				setState(1465);
				stackalloc_initializer();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Local_constant_declarationContext extends ParserRuleContext {
		public TerminalNode CONST() { return getToken(CSharpParser.CONST, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public Constant_declaratorsContext constant_declarators() {
			return getRuleContext(Constant_declaratorsContext.class,0);
		}
		public Local_constant_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_local_constant_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLocal_constant_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLocal_constant_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLocal_constant_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Local_constant_declarationContext local_constant_declaration() throws RecognitionException {
		Local_constant_declarationContext _localctx = new Local_constant_declarationContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_local_constant_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1468);
			match(CONST);
			setState(1469);
			type_();
			setState(1470);
			constant_declarators();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class If_bodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Simple_embedded_statementContext simple_embedded_statement() {
			return getRuleContext(Simple_embedded_statementContext.class,0);
		}
		public If_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_if_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIf_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIf_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIf_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final If_bodyContext if_body() throws RecognitionException {
		If_bodyContext _localctx = new If_bodyContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_if_body);
		try {
			setState(1474);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1472);
				block();
				}
				break;
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BREAK:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case CONTINUE:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DO:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FIXED:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case GET:
			case GOTO:
			case GROUP:
			case IF:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LOCK:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case RETURN:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case SWITCH:
			case THIS:
			case THROW:
			case TRUE:
			case TRY:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case UNSAFE:
			case USHORT:
			case USING:
			case VAR:
			case WHEN:
			case WHERE:
			case WHILE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case SEMICOLON:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1473);
				simple_embedded_statement();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Switch_sectionContext extends ParserRuleContext {
		public Statement_listContext statement_list() {
			return getRuleContext(Statement_listContext.class,0);
		}
		public List<Switch_labelContext> switch_label() {
			return getRuleContexts(Switch_labelContext.class);
		}
		public Switch_labelContext switch_label(int i) {
			return getRuleContext(Switch_labelContext.class,i);
		}
		public Switch_sectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switch_section; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSwitch_section(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSwitch_section(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSwitch_section(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Switch_sectionContext switch_section() throws RecognitionException {
		Switch_sectionContext _localctx = new Switch_sectionContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_switch_section);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1477); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1476);
					switch_label();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1479); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,154,_ctx);
			} while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER );
			setState(1481);
			statement_list();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Switch_labelContext extends ParserRuleContext {
		public TerminalNode CASE() { return getToken(CSharpParser.CASE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Case_guardContext case_guard() {
			return getRuleContext(Case_guardContext.class,0);
		}
		public TerminalNode DEFAULT() { return getToken(CSharpParser.DEFAULT, 0); }
		public Switch_labelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switch_label; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSwitch_label(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSwitch_label(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSwitch_label(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Switch_labelContext switch_label() throws RecognitionException {
		Switch_labelContext _localctx = new Switch_labelContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_switch_label);
		int _la;
		try {
			setState(1492);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CASE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1483);
				match(CASE);
				setState(1484);
				expression();
				setState(1486);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHEN) {
					{
					setState(1485);
					case_guard();
					}
				}

				setState(1488);
				match(COLON);
				}
				break;
			case DEFAULT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1490);
				match(DEFAULT);
				setState(1491);
				match(COLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Case_guardContext extends ParserRuleContext {
		public TerminalNode WHEN() { return getToken(CSharpParser.WHEN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Case_guardContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_case_guard; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCase_guard(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCase_guard(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCase_guard(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Case_guardContext case_guard() throws RecognitionException {
		Case_guardContext _localctx = new Case_guardContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_case_guard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1494);
			match(WHEN);
			setState(1495);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Statement_listContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public Statement_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterStatement_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitStatement_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitStatement_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Statement_listContext statement_list() throws RecognitionException {
		Statement_listContext _localctx = new Statement_listContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_statement_list);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1498); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1497);
					statement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1500); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,157,_ctx);
			} while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class For_initializerContext extends ParserRuleContext {
		public Local_variable_declarationContext local_variable_declaration() {
			return getRuleContext(Local_variable_declarationContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public For_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_for_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFor_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFor_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFor_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final For_initializerContext for_initializer() throws RecognitionException {
		For_initializerContext _localctx = new For_initializerContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_for_initializer);
		int _la;
		try {
			setState(1511);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,159,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1502);
				local_variable_declaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1503);
				expression();
				setState(1508);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1504);
					match(COMMA);
					setState(1505);
					expression();
					}
					}
					setState(1510);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class For_iteratorContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public For_iteratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_for_iterator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFor_iterator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFor_iterator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFor_iterator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final For_iteratorContext for_iterator() throws RecognitionException {
		For_iteratorContext _localctx = new For_iteratorContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_for_iterator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1513);
			expression();
			setState(1518);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1514);
				match(COMMA);
				setState(1515);
				expression();
				}
				}
				setState(1520);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Catch_clausesContext extends ParserRuleContext {
		public List<Specific_catch_clauseContext> specific_catch_clause() {
			return getRuleContexts(Specific_catch_clauseContext.class);
		}
		public Specific_catch_clauseContext specific_catch_clause(int i) {
			return getRuleContext(Specific_catch_clauseContext.class,i);
		}
		public General_catch_clauseContext general_catch_clause() {
			return getRuleContext(General_catch_clauseContext.class,0);
		}
		public Catch_clausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_catch_clauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCatch_clauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCatch_clauses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCatch_clauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Catch_clausesContext catch_clauses() throws RecognitionException {
		Catch_clausesContext _localctx = new Catch_clausesContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_catch_clauses);
		int _la;
		try {
			int _alt;
			setState(1532);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,163,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1521);
				specific_catch_clause();
				setState(1525);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,161,_ctx);
				while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1522);
						specific_catch_clause();
						}
						} 
					}
					setState(1527);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,161,_ctx);
				}
				setState(1529);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==CATCH) {
					{
					setState(1528);
					general_catch_clause();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1531);
				general_catch_clause();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Specific_catch_clauseContext extends ParserRuleContext {
		public TerminalNode CATCH() { return getToken(CSharpParser.CATCH, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Class_typeContext class_type() {
			return getRuleContext(Class_typeContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Exception_filterContext exception_filter() {
			return getRuleContext(Exception_filterContext.class,0);
		}
		public Specific_catch_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_specific_catch_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSpecific_catch_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSpecific_catch_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSpecific_catch_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Specific_catch_clauseContext specific_catch_clause() throws RecognitionException {
		Specific_catch_clauseContext _localctx = new Specific_catch_clauseContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_specific_catch_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1534);
			match(CATCH);
			setState(1535);
			match(OPEN_PARENS);
			setState(1536);
			class_type();
			setState(1538);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 3750091428249852928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 1904629023639713L) != 0)) {
				{
				setState(1537);
				identifier();
				}
			}

			setState(1540);
			match(CLOSE_PARENS);
			setState(1542);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHEN) {
				{
				setState(1541);
				exception_filter();
				}
			}

			setState(1544);
			block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class General_catch_clauseContext extends ParserRuleContext {
		public TerminalNode CATCH() { return getToken(CSharpParser.CATCH, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Exception_filterContext exception_filter() {
			return getRuleContext(Exception_filterContext.class,0);
		}
		public General_catch_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_general_catch_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterGeneral_catch_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitGeneral_catch_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitGeneral_catch_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final General_catch_clauseContext general_catch_clause() throws RecognitionException {
		General_catch_clauseContext _localctx = new General_catch_clauseContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_general_catch_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1546);
			match(CATCH);
			setState(1548);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHEN) {
				{
				setState(1547);
				exception_filter();
				}
			}

			setState(1550);
			block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Exception_filterContext extends ParserRuleContext {
		public TerminalNode WHEN() { return getToken(CSharpParser.WHEN, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Exception_filterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exception_filter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterException_filter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitException_filter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitException_filter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Exception_filterContext exception_filter() throws RecognitionException {
		Exception_filterContext _localctx = new Exception_filterContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_exception_filter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1552);
			match(WHEN);
			setState(1553);
			match(OPEN_PARENS);
			setState(1554);
			expression();
			setState(1555);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Finally_clauseContext extends ParserRuleContext {
		public TerminalNode FINALLY() { return getToken(CSharpParser.FINALLY, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Finally_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_finally_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFinally_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFinally_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFinally_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Finally_clauseContext finally_clause() throws RecognitionException {
		Finally_clauseContext _localctx = new Finally_clauseContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_finally_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1557);
			match(FINALLY);
			setState(1558);
			block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Resource_acquisitionContext extends ParserRuleContext {
		public Local_variable_declarationContext local_variable_declaration() {
			return getRuleContext(Local_variable_declarationContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Resource_acquisitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resource_acquisition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterResource_acquisition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitResource_acquisition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitResource_acquisition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Resource_acquisitionContext resource_acquisition() throws RecognitionException {
		Resource_acquisitionContext _localctx = new Resource_acquisitionContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_resource_acquisition);
		try {
			setState(1562);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,167,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1560);
				local_variable_declaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1561);
				expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Namespace_declarationContext extends ParserRuleContext {
		public Qualified_identifierContext qi;
		public TerminalNode NAMESPACE() { return getToken(CSharpParser.NAMESPACE, 0); }
		public Namespace_bodyContext namespace_body() {
			return getRuleContext(Namespace_bodyContext.class,0);
		}
		public Qualified_identifierContext qualified_identifier() {
			return getRuleContext(Qualified_identifierContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Namespace_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNamespace_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNamespace_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNamespace_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Namespace_declarationContext namespace_declaration() throws RecognitionException {
		Namespace_declarationContext _localctx = new Namespace_declarationContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_namespace_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1564);
			match(NAMESPACE);
			setState(1565);
			((Namespace_declarationContext)_localctx).qi = qualified_identifier();
			setState(1566);
			namespace_body();
			setState(1568);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(1567);
				match(SEMICOLON);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Qualified_identifierContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(CSharpParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(CSharpParser.DOT, i);
		}
		public Qualified_identifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualified_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterQualified_identifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitQualified_identifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitQualified_identifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Qualified_identifierContext qualified_identifier() throws RecognitionException {
		Qualified_identifierContext _localctx = new Qualified_identifierContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_qualified_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1570);
			identifier();
			setState(1575);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(1571);
				match(DOT);
				setState(1572);
				identifier();
				}
				}
				setState(1577);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Namespace_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Extern_alias_directivesContext extern_alias_directives() {
			return getRuleContext(Extern_alias_directivesContext.class,0);
		}
		public Using_directivesContext using_directives() {
			return getRuleContext(Using_directivesContext.class,0);
		}
		public Namespace_member_declarationsContext namespace_member_declarations() {
			return getRuleContext(Namespace_member_declarationsContext.class,0);
		}
		public Namespace_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNamespace_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNamespace_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNamespace_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Namespace_bodyContext namespace_body() throws RecognitionException {
		Namespace_bodyContext _localctx = new Namespace_bodyContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_namespace_body);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1578);
			match(OPEN_BRACE);
			setState(1580);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,170,_ctx) ) {
			case 1:
				{
				setState(1579);
				extern_alias_directives();
				}
				break;
			}
			setState(1583);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==USING) {
				{
				setState(1582);
				using_directives();
				}
			}

			setState(1586);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 216175120790618624L) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & 4611708283706211587L) != 0)) {
				{
				setState(1585);
				namespace_member_declarations();
				}
			}

			setState(1588);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Extern_alias_directivesContext extends ParserRuleContext {
		public List<Extern_alias_directiveContext> extern_alias_directive() {
			return getRuleContexts(Extern_alias_directiveContext.class);
		}
		public Extern_alias_directiveContext extern_alias_directive(int i) {
			return getRuleContext(Extern_alias_directiveContext.class,i);
		}
		public Extern_alias_directivesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extern_alias_directives; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExtern_alias_directives(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExtern_alias_directives(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExtern_alias_directives(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Extern_alias_directivesContext extern_alias_directives() throws RecognitionException {
		Extern_alias_directivesContext _localctx = new Extern_alias_directivesContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_extern_alias_directives);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1591); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1590);
					extern_alias_directive();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1593); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,173,_ctx);
			} while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Extern_alias_directiveContext extends ParserRuleContext {
		public TerminalNode EXTERN() { return getToken(CSharpParser.EXTERN, 0); }
		public TerminalNode ALIAS() { return getToken(CSharpParser.ALIAS, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Extern_alias_directiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extern_alias_directive; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterExtern_alias_directive(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitExtern_alias_directive(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitExtern_alias_directive(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Extern_alias_directiveContext extern_alias_directive() throws RecognitionException {
		Extern_alias_directiveContext _localctx = new Extern_alias_directiveContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_extern_alias_directive);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1595);
			match(EXTERN);
			setState(1596);
			match(ALIAS);
			setState(1597);
			identifier();
			setState(1598);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Using_directivesContext extends ParserRuleContext {
		public List<Using_directiveContext> using_directive() {
			return getRuleContexts(Using_directiveContext.class);
		}
		public Using_directiveContext using_directive(int i) {
			return getRuleContext(Using_directiveContext.class,i);
		}
		public Using_directivesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_using_directives; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUsing_directives(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUsing_directives(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUsing_directives(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Using_directivesContext using_directives() throws RecognitionException {
		Using_directivesContext _localctx = new Using_directivesContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_using_directives);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1601); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1600);
				using_directive();
				}
				}
				setState(1603); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==USING );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Using_directiveContext extends ParserRuleContext {
		public Using_directiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_using_directive; }
	 
		public Using_directiveContext() { }
		public void copyFrom(Using_directiveContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UsingAliasDirectiveContext extends Using_directiveContext {
		public TerminalNode USING() { return getToken(CSharpParser.USING, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public Namespace_or_type_nameContext namespace_or_type_name() {
			return getRuleContext(Namespace_or_type_nameContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public UsingAliasDirectiveContext(Using_directiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUsingAliasDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUsingAliasDirective(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUsingAliasDirective(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UsingNamespaceDirectiveContext extends Using_directiveContext {
		public TerminalNode USING() { return getToken(CSharpParser.USING, 0); }
		public Namespace_or_type_nameContext namespace_or_type_name() {
			return getRuleContext(Namespace_or_type_nameContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public UsingNamespaceDirectiveContext(Using_directiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUsingNamespaceDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUsingNamespaceDirective(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUsingNamespaceDirective(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UsingStaticDirectiveContext extends Using_directiveContext {
		public TerminalNode USING() { return getToken(CSharpParser.USING, 0); }
		public TerminalNode STATIC() { return getToken(CSharpParser.STATIC, 0); }
		public Namespace_or_type_nameContext namespace_or_type_name() {
			return getRuleContext(Namespace_or_type_nameContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public UsingStaticDirectiveContext(Using_directiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterUsingStaticDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitUsingStaticDirective(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitUsingStaticDirective(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Using_directiveContext using_directive() throws RecognitionException {
		Using_directiveContext _localctx = new Using_directiveContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_using_directive);
		try {
			setState(1620);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,175,_ctx) ) {
			case 1:
				_localctx = new UsingAliasDirectiveContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1605);
				match(USING);
				setState(1606);
				identifier();
				setState(1607);
				match(ASSIGNMENT);
				setState(1608);
				namespace_or_type_name();
				setState(1609);
				match(SEMICOLON);
				}
				break;
			case 2:
				_localctx = new UsingNamespaceDirectiveContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1611);
				match(USING);
				setState(1612);
				namespace_or_type_name();
				setState(1613);
				match(SEMICOLON);
				}
				break;
			case 3:
				_localctx = new UsingStaticDirectiveContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1615);
				match(USING);
				setState(1616);
				match(STATIC);
				setState(1617);
				namespace_or_type_name();
				setState(1618);
				match(SEMICOLON);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Namespace_member_declarationsContext extends ParserRuleContext {
		public List<Namespace_member_declarationContext> namespace_member_declaration() {
			return getRuleContexts(Namespace_member_declarationContext.class);
		}
		public Namespace_member_declarationContext namespace_member_declaration(int i) {
			return getRuleContext(Namespace_member_declarationContext.class,i);
		}
		public Namespace_member_declarationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace_member_declarations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNamespace_member_declarations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNamespace_member_declarations(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNamespace_member_declarations(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Namespace_member_declarationsContext namespace_member_declarations() throws RecognitionException {
		Namespace_member_declarationsContext _localctx = new Namespace_member_declarationsContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_namespace_member_declarations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1623); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1622);
				namespace_member_declaration();
				}
				}
				setState(1625); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 216175120790618624L) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & 4611708283706211587L) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Namespace_member_declarationContext extends ParserRuleContext {
		public Namespace_declarationContext namespace_declaration() {
			return getRuleContext(Namespace_declarationContext.class,0);
		}
		public Type_declarationContext type_declaration() {
			return getRuleContext(Type_declarationContext.class,0);
		}
		public Namespace_member_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace_member_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterNamespace_member_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitNamespace_member_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitNamespace_member_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Namespace_member_declarationContext namespace_member_declaration() throws RecognitionException {
		Namespace_member_declarationContext _localctx = new Namespace_member_declarationContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_namespace_member_declaration);
		try {
			setState(1629);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NAMESPACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1627);
				namespace_declaration();
				}
				break;
			case ABSTRACT:
			case ASYNC:
			case CLASS:
			case DELEGATE:
			case ENUM:
			case EXTERN:
			case INTERFACE:
			case INTERNAL:
			case NEW:
			case OVERRIDE:
			case PARTIAL:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case READONLY:
			case REF:
			case SEALED:
			case STATIC:
			case STRUCT:
			case UNSAFE:
			case VIRTUAL:
			case VOLATILE:
			case OPEN_BRACKET:
				enterOuterAlt(_localctx, 2);
				{
				setState(1628);
				type_declaration();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_declarationContext extends ParserRuleContext {
		public Class_definitionContext class_definition() {
			return getRuleContext(Class_definitionContext.class,0);
		}
		public Struct_definitionContext struct_definition() {
			return getRuleContext(Struct_definitionContext.class,0);
		}
		public Interface_definitionContext interface_definition() {
			return getRuleContext(Interface_definitionContext.class,0);
		}
		public Enum_definitionContext enum_definition() {
			return getRuleContext(Enum_definitionContext.class,0);
		}
		public Delegate_definitionContext delegate_definition() {
			return getRuleContext(Delegate_definitionContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public All_member_modifiersContext all_member_modifiers() {
			return getRuleContext(All_member_modifiersContext.class,0);
		}
		public Type_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_declarationContext type_declaration() throws RecognitionException {
		Type_declarationContext _localctx = new Type_declarationContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_type_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1632);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1631);
				attributes();
				}
			}

			setState(1635);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,179,_ctx) ) {
			case 1:
				{
				setState(1634);
				all_member_modifiers();
				}
				break;
			}
			setState(1642);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CLASS:
				{
				setState(1637);
				class_definition();
				}
				break;
			case READONLY:
			case REF:
			case STRUCT:
				{
				setState(1638);
				struct_definition();
				}
				break;
			case INTERFACE:
				{
				setState(1639);
				interface_definition();
				}
				break;
			case ENUM:
				{
				setState(1640);
				enum_definition();
				}
				break;
			case DELEGATE:
				{
				setState(1641);
				delegate_definition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Qualified_alias_memberContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode DOUBLE_COLON() { return getToken(CSharpParser.DOUBLE_COLON, 0); }
		public Type_argument_listContext type_argument_list() {
			return getRuleContext(Type_argument_listContext.class,0);
		}
		public Qualified_alias_memberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualified_alias_member; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterQualified_alias_member(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitQualified_alias_member(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitQualified_alias_member(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Qualified_alias_memberContext qualified_alias_member() throws RecognitionException {
		Qualified_alias_memberContext _localctx = new Qualified_alias_memberContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_qualified_alias_member);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1644);
			identifier();
			setState(1645);
			match(DOUBLE_COLON);
			setState(1646);
			identifier();
			setState(1648);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,181,_ctx) ) {
			case 1:
				{
				setState(1647);
				type_argument_list();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_parameter_listContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(CSharpParser.LT, 0); }
		public List<Type_parameterContext> type_parameter() {
			return getRuleContexts(Type_parameterContext.class);
		}
		public Type_parameterContext type_parameter(int i) {
			return getRuleContext(Type_parameterContext.class,i);
		}
		public TerminalNode GT() { return getToken(CSharpParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Type_parameter_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_parameter_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_parameter_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_parameter_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_parameter_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_parameter_listContext type_parameter_list() throws RecognitionException {
		Type_parameter_listContext _localctx = new Type_parameter_listContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_type_parameter_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1650);
			match(LT);
			setState(1651);
			type_parameter();
			setState(1656);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1652);
				match(COMMA);
				setState(1653);
				type_parameter();
				}
				}
				setState(1658);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1659);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_parameterContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Type_parameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_parameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_parameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_parameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_parameterContext type_parameter() throws RecognitionException {
		Type_parameterContext _localctx = new Type_parameterContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_type_parameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1662);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1661);
				attributes();
				}
			}

			setState(1664);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_baseContext extends ParserRuleContext {
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Class_typeContext class_type() {
			return getRuleContext(Class_typeContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public List<Namespace_or_type_nameContext> namespace_or_type_name() {
			return getRuleContexts(Namespace_or_type_nameContext.class);
		}
		public Namespace_or_type_nameContext namespace_or_type_name(int i) {
			return getRuleContext(Namespace_or_type_nameContext.class,i);
		}
		public Class_baseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_base; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterClass_base(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitClass_base(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitClass_base(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_baseContext class_base() throws RecognitionException {
		Class_baseContext _localctx = new Class_baseContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_class_base);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1666);
			match(COLON);
			setState(1667);
			class_type();
			setState(1672);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1668);
				match(COMMA);
				setState(1669);
				namespace_or_type_name();
				}
				}
				setState(1674);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interface_type_listContext extends ParserRuleContext {
		public List<Namespace_or_type_nameContext> namespace_or_type_name() {
			return getRuleContexts(Namespace_or_type_nameContext.class);
		}
		public Namespace_or_type_nameContext namespace_or_type_name(int i) {
			return getRuleContext(Namespace_or_type_nameContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Interface_type_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interface_type_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterface_type_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterface_type_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterface_type_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interface_type_listContext interface_type_list() throws RecognitionException {
		Interface_type_listContext _localctx = new Interface_type_listContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_interface_type_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1675);
			namespace_or_type_name();
			setState(1680);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1676);
				match(COMMA);
				setState(1677);
				namespace_or_type_name();
				}
				}
				setState(1682);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_parameter_constraints_clausesContext extends ParserRuleContext {
		public List<Type_parameter_constraints_clauseContext> type_parameter_constraints_clause() {
			return getRuleContexts(Type_parameter_constraints_clauseContext.class);
		}
		public Type_parameter_constraints_clauseContext type_parameter_constraints_clause(int i) {
			return getRuleContext(Type_parameter_constraints_clauseContext.class,i);
		}
		public Type_parameter_constraints_clausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_parameter_constraints_clauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_parameter_constraints_clauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_parameter_constraints_clauses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_parameter_constraints_clauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() throws RecognitionException {
		Type_parameter_constraints_clausesContext _localctx = new Type_parameter_constraints_clausesContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_type_parameter_constraints_clauses);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1684); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1683);
				type_parameter_constraints_clause();
				}
				}
				setState(1686); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==WHERE );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_parameter_constraints_clauseContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(CSharpParser.WHERE, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Type_parameter_constraintsContext type_parameter_constraints() {
			return getRuleContext(Type_parameter_constraintsContext.class,0);
		}
		public Type_parameter_constraints_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_parameter_constraints_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_parameter_constraints_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_parameter_constraints_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_parameter_constraints_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_parameter_constraints_clauseContext type_parameter_constraints_clause() throws RecognitionException {
		Type_parameter_constraints_clauseContext _localctx = new Type_parameter_constraints_clauseContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_type_parameter_constraints_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1688);
			match(WHERE);
			setState(1689);
			identifier();
			setState(1690);
			match(COLON);
			setState(1691);
			type_parameter_constraints();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_parameter_constraintsContext extends ParserRuleContext {
		public Constructor_constraintContext constructor_constraint() {
			return getRuleContext(Constructor_constraintContext.class,0);
		}
		public Primary_constraintContext primary_constraint() {
			return getRuleContext(Primary_constraintContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Secondary_constraintsContext secondary_constraints() {
			return getRuleContext(Secondary_constraintsContext.class,0);
		}
		public Type_parameter_constraintsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_parameter_constraints; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterType_parameter_constraints(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitType_parameter_constraints(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitType_parameter_constraints(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_parameter_constraintsContext type_parameter_constraints() throws RecognitionException {
		Type_parameter_constraintsContext _localctx = new Type_parameter_constraintsContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_type_parameter_constraints);
		int _la;
		try {
			setState(1703);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NEW:
				enterOuterAlt(_localctx, 1);
				{
				setState(1693);
				constructor_constraint();
				}
				break;
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BY:
			case CLASS:
			case DESCENDING:
			case DYNAMIC:
			case EQUALS:
			case FROM:
			case GET:
			case GROUP:
			case INTO:
			case JOIN:
			case LET:
			case NAMEOF:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REMOVE:
			case SELECT:
			case SET:
			case STRING:
			case STRUCT:
			case UNMANAGED:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(1694);
				primary_constraint();
				setState(1697);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,187,_ctx) ) {
				case 1:
					{
					setState(1695);
					match(COMMA);
					setState(1696);
					secondary_constraints();
					}
					break;
				}
				setState(1701);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(1699);
					match(COMMA);
					setState(1700);
					constructor_constraint();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Primary_constraintContext extends ParserRuleContext {
		public Class_typeContext class_type() {
			return getRuleContext(Class_typeContext.class,0);
		}
		public TerminalNode CLASS() { return getToken(CSharpParser.CLASS, 0); }
		public TerminalNode INTERR() { return getToken(CSharpParser.INTERR, 0); }
		public TerminalNode STRUCT() { return getToken(CSharpParser.STRUCT, 0); }
		public TerminalNode UNMANAGED() { return getToken(CSharpParser.UNMANAGED, 0); }
		public Primary_constraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary_constraint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterPrimary_constraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitPrimary_constraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitPrimary_constraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Primary_constraintContext primary_constraint() throws RecognitionException {
		Primary_constraintContext _localctx = new Primary_constraintContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_primary_constraint);
		int _la;
		try {
			setState(1712);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,191,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1705);
				class_type();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1706);
				match(CLASS);
				setState(1708);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INTERR) {
					{
					setState(1707);
					match(INTERR);
					}
				}

				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1710);
				match(STRUCT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1711);
				match(UNMANAGED);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Secondary_constraintsContext extends ParserRuleContext {
		public List<Namespace_or_type_nameContext> namespace_or_type_name() {
			return getRuleContexts(Namespace_or_type_nameContext.class);
		}
		public Namespace_or_type_nameContext namespace_or_type_name(int i) {
			return getRuleContext(Namespace_or_type_nameContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Secondary_constraintsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_secondary_constraints; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSecondary_constraints(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSecondary_constraints(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSecondary_constraints(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Secondary_constraintsContext secondary_constraints() throws RecognitionException {
		Secondary_constraintsContext _localctx = new Secondary_constraintsContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_secondary_constraints);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1714);
			namespace_or_type_name();
			setState(1719);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,192,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1715);
					match(COMMA);
					setState(1716);
					namespace_or_type_name();
					}
					} 
				}
				setState(1721);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,192,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constructor_constraintContext extends ParserRuleContext {
		public TerminalNode NEW() { return getToken(CSharpParser.NEW, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Constructor_constraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructor_constraint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConstructor_constraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConstructor_constraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConstructor_constraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constructor_constraintContext constructor_constraint() throws RecognitionException {
		Constructor_constraintContext _localctx = new Constructor_constraintContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_constructor_constraint);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1722);
			match(NEW);
			setState(1723);
			match(OPEN_PARENS);
			setState(1724);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Class_member_declarationsContext class_member_declarations() {
			return getRuleContext(Class_member_declarationsContext.class,0);
		}
		public Class_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterClass_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitClass_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitClass_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_bodyContext class_body() throws RecognitionException {
		Class_bodyContext _localctx = new Class_bodyContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_class_body);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1726);
			match(OPEN_BRACE);
			setState(1728);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5212032640031007232L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -9221404082353472843L) != 0) || _la==OPEN_PARENS || _la==TILDE) {
				{
				setState(1727);
				class_member_declarations();
				}
			}

			setState(1730);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_member_declarationsContext extends ParserRuleContext {
		public List<Class_member_declarationContext> class_member_declaration() {
			return getRuleContexts(Class_member_declarationContext.class);
		}
		public Class_member_declarationContext class_member_declaration(int i) {
			return getRuleContext(Class_member_declarationContext.class,i);
		}
		public Class_member_declarationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_member_declarations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterClass_member_declarations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitClass_member_declarations(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitClass_member_declarations(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_member_declarationsContext class_member_declarations() throws RecognitionException {
		Class_member_declarationsContext _localctx = new Class_member_declarationsContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_class_member_declarations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1733); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1732);
				class_member_declaration();
				}
				}
				setState(1735); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & -5212032640031007232L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -9221404082353472843L) != 0) || _la==OPEN_PARENS || _la==TILDE );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_member_declarationContext extends ParserRuleContext {
		public Common_member_declarationContext common_member_declaration() {
			return getRuleContext(Common_member_declarationContext.class,0);
		}
		public Destructor_definitionContext destructor_definition() {
			return getRuleContext(Destructor_definitionContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public All_member_modifiersContext all_member_modifiers() {
			return getRuleContext(All_member_modifiersContext.class,0);
		}
		public Class_member_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_member_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterClass_member_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitClass_member_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitClass_member_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_member_declarationContext class_member_declaration() throws RecognitionException {
		Class_member_declarationContext _localctx = new Class_member_declarationContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_class_member_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1738);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1737);
				attributes();
				}
			}

			setState(1741);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,196,_ctx) ) {
			case 1:
				{
				setState(1740);
				all_member_modifiers();
				}
				break;
			}
			setState(1745);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CLASS:
			case CONST:
			case DECIMAL:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case ENUM:
			case EQUALS:
			case EVENT:
			case EXPLICIT:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case IMPLICIT:
			case INT:
			case INTERFACE:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case READONLY:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case STRING:
			case STRUCT:
			case UINT:
			case ULONG:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case VOID:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case OPEN_PARENS:
				{
				setState(1743);
				common_member_declaration();
				}
				break;
			case TILDE:
				{
				setState(1744);
				destructor_definition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class All_member_modifiersContext extends ParserRuleContext {
		public List<All_member_modifierContext> all_member_modifier() {
			return getRuleContexts(All_member_modifierContext.class);
		}
		public All_member_modifierContext all_member_modifier(int i) {
			return getRuleContext(All_member_modifierContext.class,i);
		}
		public All_member_modifiersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_all_member_modifiers; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAll_member_modifiers(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAll_member_modifiers(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAll_member_modifiers(this);
			else return visitor.visitChildren(this);
		}
	}

	public final All_member_modifiersContext all_member_modifiers() throws RecognitionException {
		All_member_modifiersContext _localctx = new All_member_modifiersContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_all_member_modifiers);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1748); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1747);
					all_member_modifier();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1750); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,198,_ctx);
			} while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class All_member_modifierContext extends ParserRuleContext {
		public TerminalNode NEW() { return getToken(CSharpParser.NEW, 0); }
		public TerminalNode PUBLIC() { return getToken(CSharpParser.PUBLIC, 0); }
		public TerminalNode PROTECTED() { return getToken(CSharpParser.PROTECTED, 0); }
		public TerminalNode INTERNAL() { return getToken(CSharpParser.INTERNAL, 0); }
		public TerminalNode PRIVATE() { return getToken(CSharpParser.PRIVATE, 0); }
		public TerminalNode READONLY() { return getToken(CSharpParser.READONLY, 0); }
		public TerminalNode VOLATILE() { return getToken(CSharpParser.VOLATILE, 0); }
		public TerminalNode VIRTUAL() { return getToken(CSharpParser.VIRTUAL, 0); }
		public TerminalNode SEALED() { return getToken(CSharpParser.SEALED, 0); }
		public TerminalNode OVERRIDE() { return getToken(CSharpParser.OVERRIDE, 0); }
		public TerminalNode ABSTRACT() { return getToken(CSharpParser.ABSTRACT, 0); }
		public TerminalNode STATIC() { return getToken(CSharpParser.STATIC, 0); }
		public TerminalNode UNSAFE() { return getToken(CSharpParser.UNSAFE, 0); }
		public TerminalNode EXTERN() { return getToken(CSharpParser.EXTERN, 0); }
		public TerminalNode PARTIAL() { return getToken(CSharpParser.PARTIAL, 0); }
		public TerminalNode ASYNC() { return getToken(CSharpParser.ASYNC, 0); }
		public All_member_modifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_all_member_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAll_member_modifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAll_member_modifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAll_member_modifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final All_member_modifierContext all_member_modifier() throws RecognitionException {
		All_member_modifierContext _localctx = new All_member_modifierContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_all_member_modifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1752);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 144117387099144704L) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & 11132572286593L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Common_member_declarationContext extends ParserRuleContext {
		public Constant_declarationContext constant_declaration() {
			return getRuleContext(Constant_declarationContext.class,0);
		}
		public Typed_member_declarationContext typed_member_declaration() {
			return getRuleContext(Typed_member_declarationContext.class,0);
		}
		public Event_declarationContext event_declaration() {
			return getRuleContext(Event_declarationContext.class,0);
		}
		public Conversion_operator_declaratorContext conversion_operator_declarator() {
			return getRuleContext(Conversion_operator_declaratorContext.class,0);
		}
		public BodyContext body() {
			return getRuleContext(BodyContext.class,0);
		}
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Constructor_declarationContext constructor_declaration() {
			return getRuleContext(Constructor_declarationContext.class,0);
		}
		public TerminalNode VOID() { return getToken(CSharpParser.VOID, 0); }
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Class_definitionContext class_definition() {
			return getRuleContext(Class_definitionContext.class,0);
		}
		public Struct_definitionContext struct_definition() {
			return getRuleContext(Struct_definitionContext.class,0);
		}
		public Interface_definitionContext interface_definition() {
			return getRuleContext(Interface_definitionContext.class,0);
		}
		public Enum_definitionContext enum_definition() {
			return getRuleContext(Enum_definitionContext.class,0);
		}
		public Delegate_definitionContext delegate_definition() {
			return getRuleContext(Delegate_definitionContext.class,0);
		}
		public Common_member_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_common_member_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterCommon_member_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitCommon_member_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitCommon_member_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Common_member_declarationContext common_member_declaration() throws RecognitionException {
		Common_member_declarationContext _localctx = new Common_member_declarationContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_common_member_declaration);
		try {
			setState(1773);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,200,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1754);
				constant_declaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1755);
				typed_member_declaration();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1756);
				event_declaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1757);
				conversion_operator_declarator();
				setState(1763);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPEN_BRACE:
				case SEMICOLON:
					{
					setState(1758);
					body();
					}
					break;
				case ASSIGNMENT:
					{
					setState(1759);
					right_arrow();
					setState(1760);
					throwable_expression();
					setState(1761);
					match(SEMICOLON);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1765);
				constructor_declaration();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1766);
				match(VOID);
				setState(1767);
				method_declaration();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1768);
				class_definition();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1769);
				struct_definition();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1770);
				interface_definition();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1771);
				enum_definition();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1772);
				delegate_definition();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Typed_member_declarationContext extends ParserRuleContext {
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public Namespace_or_type_nameContext namespace_or_type_name() {
			return getRuleContext(Namespace_or_type_nameContext.class,0);
		}
		public TerminalNode DOT() { return getToken(CSharpParser.DOT, 0); }
		public Indexer_declarationContext indexer_declaration() {
			return getRuleContext(Indexer_declarationContext.class,0);
		}
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Property_declarationContext property_declaration() {
			return getRuleContext(Property_declarationContext.class,0);
		}
		public Operator_declarationContext operator_declaration() {
			return getRuleContext(Operator_declarationContext.class,0);
		}
		public Field_declarationContext field_declaration() {
			return getRuleContext(Field_declarationContext.class,0);
		}
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public TerminalNode READONLY() { return getToken(CSharpParser.READONLY, 0); }
		public Typed_member_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typed_member_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterTyped_member_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitTyped_member_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitTyped_member_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Typed_member_declarationContext typed_member_declaration() throws RecognitionException {
		Typed_member_declarationContext _localctx = new Typed_member_declarationContext(_ctx, getState());
		enterRule(_localctx, 264, RULE_typed_member_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1780);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,201,_ctx) ) {
			case 1:
				{
				setState(1775);
				match(REF);
				}
				break;
			case 2:
				{
				setState(1776);
				match(READONLY);
				setState(1777);
				match(REF);
				}
				break;
			case 3:
				{
				setState(1778);
				match(REF);
				setState(1779);
				match(READONLY);
				}
				break;
			}
			setState(1782);
			type_();
			setState(1792);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,202,_ctx) ) {
			case 1:
				{
				setState(1783);
				namespace_or_type_name();
				setState(1784);
				match(DOT);
				setState(1785);
				indexer_declaration();
				}
				break;
			case 2:
				{
				setState(1787);
				method_declaration();
				}
				break;
			case 3:
				{
				setState(1788);
				property_declaration();
				}
				break;
			case 4:
				{
				setState(1789);
				indexer_declaration();
				}
				break;
			case 5:
				{
				setState(1790);
				operator_declaration();
				}
				break;
			case 6:
				{
				setState(1791);
				field_declaration();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constant_declaratorsContext extends ParserRuleContext {
		public List<Constant_declaratorContext> constant_declarator() {
			return getRuleContexts(Constant_declaratorContext.class);
		}
		public Constant_declaratorContext constant_declarator(int i) {
			return getRuleContext(Constant_declaratorContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Constant_declaratorsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant_declarators; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConstant_declarators(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConstant_declarators(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConstant_declarators(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constant_declaratorsContext constant_declarators() throws RecognitionException {
		Constant_declaratorsContext _localctx = new Constant_declaratorsContext(_ctx, getState());
		enterRule(_localctx, 266, RULE_constant_declarators);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1794);
			constant_declarator();
			setState(1799);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1795);
				match(COMMA);
				setState(1796);
				constant_declarator();
				}
				}
				setState(1801);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constant_declaratorContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Constant_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConstant_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConstant_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConstant_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constant_declaratorContext constant_declarator() throws RecognitionException {
		Constant_declaratorContext _localctx = new Constant_declaratorContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_constant_declarator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1802);
			identifier();
			setState(1803);
			match(ASSIGNMENT);
			setState(1804);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variable_declaratorsContext extends ParserRuleContext {
		public List<Variable_declaratorContext> variable_declarator() {
			return getRuleContexts(Variable_declaratorContext.class);
		}
		public Variable_declaratorContext variable_declarator(int i) {
			return getRuleContext(Variable_declaratorContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Variable_declaratorsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_declarators; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterVariable_declarators(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitVariable_declarators(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitVariable_declarators(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variable_declaratorsContext variable_declarators() throws RecognitionException {
		Variable_declaratorsContext _localctx = new Variable_declaratorsContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_variable_declarators);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1806);
			variable_declarator();
			setState(1811);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1807);
				match(COMMA);
				setState(1808);
				variable_declarator();
				}
				}
				setState(1813);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variable_declaratorContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public Variable_initializerContext variable_initializer() {
			return getRuleContext(Variable_initializerContext.class,0);
		}
		public Variable_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterVariable_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitVariable_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitVariable_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variable_declaratorContext variable_declarator() throws RecognitionException {
		Variable_declaratorContext _localctx = new Variable_declaratorContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_variable_declarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1814);
			identifier();
			setState(1817);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGNMENT) {
				{
				setState(1815);
				match(ASSIGNMENT);
				setState(1816);
				variable_initializer();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variable_initializerContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Array_initializerContext array_initializer() {
			return getRuleContext(Array_initializerContext.class,0);
		}
		public Variable_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterVariable_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitVariable_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitVariable_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variable_initializerContext variable_initializer() throws RecognitionException {
		Variable_initializerContext _localctx = new Variable_initializerContext(_ctx, getState());
		enterRule(_localctx, 274, RULE_variable_initializer);
		try {
			setState(1821);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1819);
				expression();
				}
				break;
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1820);
				array_initializer();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Return_typeContext extends ParserRuleContext {
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode VOID() { return getToken(CSharpParser.VOID, 0); }
		public Return_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterReturn_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitReturn_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitReturn_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Return_typeContext return_type() throws RecognitionException {
		Return_typeContext _localctx = new Return_typeContext(_ctx, getState());
		enterRule(_localctx, 276, RULE_return_type);
		try {
			setState(1825);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,207,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1823);
				type_();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1824);
				match(VOID);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Member_nameContext extends ParserRuleContext {
		public Namespace_or_type_nameContext namespace_or_type_name() {
			return getRuleContext(Namespace_or_type_nameContext.class,0);
		}
		public Member_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_member_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMember_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMember_name(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMember_name(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Member_nameContext member_name() throws RecognitionException {
		Member_nameContext _localctx = new Member_nameContext(_ctx, getState());
		enterRule(_localctx, 278, RULE_member_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1827);
			namespace_or_type_name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_bodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Method_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMethod_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMethod_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMethod_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_bodyContext method_body() throws RecognitionException {
		Method_bodyContext _localctx = new Method_bodyContext(_ctx, getState());
		enterRule(_localctx, 280, RULE_method_body);
		try {
			setState(1831);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1829);
				block();
				}
				break;
			case SEMICOLON:
				enterOuterAlt(_localctx, 2);
				{
				setState(1830);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Formal_parameter_listContext extends ParserRuleContext {
		public Parameter_arrayContext parameter_array() {
			return getRuleContext(Parameter_arrayContext.class,0);
		}
		public Fixed_parametersContext fixed_parameters() {
			return getRuleContext(Fixed_parametersContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(CSharpParser.COMMA, 0); }
		public Formal_parameter_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formal_parameter_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFormal_parameter_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFormal_parameter_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFormal_parameter_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Formal_parameter_listContext formal_parameter_list() throws RecognitionException {
		Formal_parameter_listContext _localctx = new Formal_parameter_listContext(_ctx, getState());
		enterRule(_localctx, 282, RULE_formal_parameter_list);
		int _la;
		try {
			setState(1839);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,210,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1833);
				parameter_array();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1834);
				fixed_parameters();
				setState(1837);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(1835);
					match(COMMA);
					setState(1836);
					parameter_array();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fixed_parametersContext extends ParserRuleContext {
		public List<Fixed_parameterContext> fixed_parameter() {
			return getRuleContexts(Fixed_parameterContext.class);
		}
		public Fixed_parameterContext fixed_parameter(int i) {
			return getRuleContext(Fixed_parameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Fixed_parametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fixed_parameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFixed_parameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFixed_parameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFixed_parameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fixed_parametersContext fixed_parameters() throws RecognitionException {
		Fixed_parametersContext _localctx = new Fixed_parametersContext(_ctx, getState());
		enterRule(_localctx, 284, RULE_fixed_parameters);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1841);
			fixed_parameter();
			setState(1846);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,211,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1842);
					match(COMMA);
					setState(1843);
					fixed_parameter();
					}
					} 
				}
				setState(1848);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,211,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fixed_parameterContext extends ParserRuleContext {
		public Arg_declarationContext arg_declaration() {
			return getRuleContext(Arg_declarationContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Parameter_modifierContext parameter_modifier() {
			return getRuleContext(Parameter_modifierContext.class,0);
		}
		public TerminalNode ARGLIST() { return getToken(CSharpParser.ARGLIST, 0); }
		public Fixed_parameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fixed_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFixed_parameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFixed_parameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFixed_parameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fixed_parameterContext fixed_parameter() throws RecognitionException {
		Fixed_parameterContext _localctx = new Fixed_parameterContext(_ctx, getState());
		enterRule(_localctx, 286, RULE_fixed_parameter);
		int _la;
		try {
			setState(1857);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,214,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1850);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPEN_BRACKET) {
					{
					setState(1849);
					attributes();
					}
				}

				setState(1853);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 54)) & ~0x3f) == 0 && ((1L << (_la - 54)) & 1099578998785L) != 0)) {
					{
					setState(1852);
					parameter_modifier();
					}
				}

				setState(1855);
				arg_declaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1856);
				match(ARGLIST);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Parameter_modifierContext extends ParserRuleContext {
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public TerminalNode OUT() { return getToken(CSharpParser.OUT, 0); }
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public TerminalNode THIS() { return getToken(CSharpParser.THIS, 0); }
		public Parameter_modifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameter_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterParameter_modifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitParameter_modifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitParameter_modifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Parameter_modifierContext parameter_modifier() throws RecognitionException {
		Parameter_modifierContext _localctx = new Parameter_modifierContext(_ctx, getState());
		enterRule(_localctx, 288, RULE_parameter_modifier);
		try {
			setState(1867);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,215,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1859);
				match(REF);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1860);
				match(OUT);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1861);
				match(IN);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1862);
				match(REF);
				setState(1863);
				match(THIS);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1864);
				match(IN);
				setState(1865);
				match(THIS);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1866);
				match(THIS);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Parameter_arrayContext extends ParserRuleContext {
		public TerminalNode PARAMS() { return getToken(CSharpParser.PARAMS, 0); }
		public Array_typeContext array_type() {
			return getRuleContext(Array_typeContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Parameter_arrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameter_array; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterParameter_array(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitParameter_array(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitParameter_array(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Parameter_arrayContext parameter_array() throws RecognitionException {
		Parameter_arrayContext _localctx = new Parameter_arrayContext(_ctx, getState());
		enterRule(_localctx, 290, RULE_parameter_array);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1870);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1869);
				attributes();
				}
			}

			setState(1872);
			match(PARAMS);
			setState(1873);
			array_type();
			setState(1874);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Accessor_declarationsContext extends ParserRuleContext {
		public AttributesContext attrs;
		public Accessor_modifierContext mods;
		public TerminalNode GET() { return getToken(CSharpParser.GET, 0); }
		public Accessor_bodyContext accessor_body() {
			return getRuleContext(Accessor_bodyContext.class,0);
		}
		public TerminalNode SET() { return getToken(CSharpParser.SET, 0); }
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Accessor_modifierContext accessor_modifier() {
			return getRuleContext(Accessor_modifierContext.class,0);
		}
		public Set_accessor_declarationContext set_accessor_declaration() {
			return getRuleContext(Set_accessor_declarationContext.class,0);
		}
		public Get_accessor_declarationContext get_accessor_declaration() {
			return getRuleContext(Get_accessor_declarationContext.class,0);
		}
		public Accessor_declarationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_accessor_declarations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAccessor_declarations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAccessor_declarations(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAccessor_declarations(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Accessor_declarationsContext accessor_declarations() throws RecognitionException {
		Accessor_declarationsContext _localctx = new Accessor_declarationsContext(_ctx, getState());
		enterRule(_localctx, 292, RULE_accessor_declarations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1877);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1876);
				((Accessor_declarationsContext)_localctx).attrs = attributes();
				}
			}

			setState(1880);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 57)) & ~0x3f) == 0 && ((1L << (_la - 57)) & 1572865L) != 0)) {
				{
				setState(1879);
				((Accessor_declarationsContext)_localctx).mods = accessor_modifier();
				}
			}

			setState(1892);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GET:
				{
				setState(1882);
				match(GET);
				setState(1883);
				accessor_body();
				setState(1885);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INTERNAL || ((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & 2251799813686275L) != 0)) {
					{
					setState(1884);
					set_accessor_declaration();
					}
				}

				}
				break;
			case SET:
				{
				setState(1887);
				match(SET);
				setState(1888);
				accessor_body();
				setState(1890);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==GET || _la==INTERNAL || ((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & 2251799813685251L) != 0)) {
					{
					setState(1889);
					get_accessor_declaration();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Get_accessor_declarationContext extends ParserRuleContext {
		public TerminalNode GET() { return getToken(CSharpParser.GET, 0); }
		public Accessor_bodyContext accessor_body() {
			return getRuleContext(Accessor_bodyContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Accessor_modifierContext accessor_modifier() {
			return getRuleContext(Accessor_modifierContext.class,0);
		}
		public Get_accessor_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_get_accessor_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterGet_accessor_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitGet_accessor_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitGet_accessor_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Get_accessor_declarationContext get_accessor_declaration() throws RecognitionException {
		Get_accessor_declarationContext _localctx = new Get_accessor_declarationContext(_ctx, getState());
		enterRule(_localctx, 294, RULE_get_accessor_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1895);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1894);
				attributes();
				}
			}

			setState(1898);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 57)) & ~0x3f) == 0 && ((1L << (_la - 57)) & 1572865L) != 0)) {
				{
				setState(1897);
				accessor_modifier();
				}
			}

			setState(1900);
			match(GET);
			setState(1901);
			accessor_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Set_accessor_declarationContext extends ParserRuleContext {
		public TerminalNode SET() { return getToken(CSharpParser.SET, 0); }
		public Accessor_bodyContext accessor_body() {
			return getRuleContext(Accessor_bodyContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Accessor_modifierContext accessor_modifier() {
			return getRuleContext(Accessor_modifierContext.class,0);
		}
		public Set_accessor_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_set_accessor_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterSet_accessor_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitSet_accessor_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitSet_accessor_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Set_accessor_declarationContext set_accessor_declaration() throws RecognitionException {
		Set_accessor_declarationContext _localctx = new Set_accessor_declarationContext(_ctx, getState());
		enterRule(_localctx, 296, RULE_set_accessor_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1904);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1903);
				attributes();
				}
			}

			setState(1907);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 57)) & ~0x3f) == 0 && ((1L << (_la - 57)) & 1572865L) != 0)) {
				{
				setState(1906);
				accessor_modifier();
				}
			}

			setState(1909);
			match(SET);
			setState(1910);
			accessor_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Accessor_modifierContext extends ParserRuleContext {
		public TerminalNode PROTECTED() { return getToken(CSharpParser.PROTECTED, 0); }
		public TerminalNode INTERNAL() { return getToken(CSharpParser.INTERNAL, 0); }
		public TerminalNode PRIVATE() { return getToken(CSharpParser.PRIVATE, 0); }
		public Accessor_modifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_accessor_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAccessor_modifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAccessor_modifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAccessor_modifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Accessor_modifierContext accessor_modifier() throws RecognitionException {
		Accessor_modifierContext _localctx = new Accessor_modifierContext(_ctx, getState());
		enterRule(_localctx, 298, RULE_accessor_modifier);
		try {
			setState(1919);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,226,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1912);
				match(PROTECTED);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1913);
				match(INTERNAL);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1914);
				match(PRIVATE);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1915);
				match(PROTECTED);
				setState(1916);
				match(INTERNAL);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1917);
				match(INTERNAL);
				setState(1918);
				match(PROTECTED);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Accessor_bodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Accessor_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_accessor_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAccessor_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAccessor_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAccessor_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Accessor_bodyContext accessor_body() throws RecognitionException {
		Accessor_bodyContext _localctx = new Accessor_bodyContext(_ctx, getState());
		enterRule(_localctx, 300, RULE_accessor_body);
		try {
			setState(1923);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1921);
				block();
				}
				break;
			case SEMICOLON:
				enterOuterAlt(_localctx, 2);
				{
				setState(1922);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Event_accessor_declarationsContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(CSharpParser.ADD, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public Remove_accessor_declarationContext remove_accessor_declaration() {
			return getRuleContext(Remove_accessor_declarationContext.class,0);
		}
		public TerminalNode REMOVE() { return getToken(CSharpParser.REMOVE, 0); }
		public Add_accessor_declarationContext add_accessor_declaration() {
			return getRuleContext(Add_accessor_declarationContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Event_accessor_declarationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_event_accessor_declarations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEvent_accessor_declarations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEvent_accessor_declarations(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEvent_accessor_declarations(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Event_accessor_declarationsContext event_accessor_declarations() throws RecognitionException {
		Event_accessor_declarationsContext _localctx = new Event_accessor_declarationsContext(_ctx, getState());
		enterRule(_localctx, 302, RULE_event_accessor_declarations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1926);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1925);
				attributes();
				}
			}

			setState(1936);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
				{
				setState(1928);
				match(ADD);
				setState(1929);
				block();
				setState(1930);
				remove_accessor_declaration();
				}
				break;
			case REMOVE:
				{
				setState(1932);
				match(REMOVE);
				setState(1933);
				block();
				setState(1934);
				add_accessor_declaration();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Add_accessor_declarationContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(CSharpParser.ADD, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Add_accessor_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_add_accessor_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAdd_accessor_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAdd_accessor_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAdd_accessor_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Add_accessor_declarationContext add_accessor_declaration() throws RecognitionException {
		Add_accessor_declarationContext _localctx = new Add_accessor_declarationContext(_ctx, getState());
		enterRule(_localctx, 304, RULE_add_accessor_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1939);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1938);
				attributes();
				}
			}

			setState(1941);
			match(ADD);
			setState(1942);
			block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Remove_accessor_declarationContext extends ParserRuleContext {
		public TerminalNode REMOVE() { return getToken(CSharpParser.REMOVE, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Remove_accessor_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_remove_accessor_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterRemove_accessor_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitRemove_accessor_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitRemove_accessor_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Remove_accessor_declarationContext remove_accessor_declaration() throws RecognitionException {
		Remove_accessor_declarationContext _localctx = new Remove_accessor_declarationContext(_ctx, getState());
		enterRule(_localctx, 306, RULE_remove_accessor_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1945);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(1944);
				attributes();
				}
			}

			setState(1947);
			match(REMOVE);
			setState(1948);
			block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Overloadable_operatorContext extends ParserRuleContext {
		public TerminalNode PLUS() { return getToken(CSharpParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(CSharpParser.MINUS, 0); }
		public TerminalNode BANG() { return getToken(CSharpParser.BANG, 0); }
		public TerminalNode TILDE() { return getToken(CSharpParser.TILDE, 0); }
		public TerminalNode OP_INC() { return getToken(CSharpParser.OP_INC, 0); }
		public TerminalNode OP_DEC() { return getToken(CSharpParser.OP_DEC, 0); }
		public TerminalNode TRUE() { return getToken(CSharpParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(CSharpParser.FALSE, 0); }
		public TerminalNode STAR() { return getToken(CSharpParser.STAR, 0); }
		public TerminalNode DIV() { return getToken(CSharpParser.DIV, 0); }
		public TerminalNode PERCENT() { return getToken(CSharpParser.PERCENT, 0); }
		public TerminalNode AMP() { return getToken(CSharpParser.AMP, 0); }
		public TerminalNode BITWISE_OR() { return getToken(CSharpParser.BITWISE_OR, 0); }
		public TerminalNode CARET() { return getToken(CSharpParser.CARET, 0); }
		public TerminalNode OP_LEFT_SHIFT() { return getToken(CSharpParser.OP_LEFT_SHIFT, 0); }
		public Right_shiftContext right_shift() {
			return getRuleContext(Right_shiftContext.class,0);
		}
		public TerminalNode OP_EQ() { return getToken(CSharpParser.OP_EQ, 0); }
		public TerminalNode OP_NE() { return getToken(CSharpParser.OP_NE, 0); }
		public TerminalNode GT() { return getToken(CSharpParser.GT, 0); }
		public TerminalNode LT() { return getToken(CSharpParser.LT, 0); }
		public TerminalNode OP_GE() { return getToken(CSharpParser.OP_GE, 0); }
		public TerminalNode OP_LE() { return getToken(CSharpParser.OP_LE, 0); }
		public Overloadable_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_overloadable_operator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterOverloadable_operator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitOverloadable_operator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitOverloadable_operator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Overloadable_operatorContext overloadable_operator() throws RecognitionException {
		Overloadable_operatorContext _localctx = new Overloadable_operatorContext(_ctx, getState());
		enterRule(_localctx, 308, RULE_overloadable_operator);
		try {
			setState(1972);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,232,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1950);
				match(PLUS);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1951);
				match(MINUS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1952);
				match(BANG);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1953);
				match(TILDE);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1954);
				match(OP_INC);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1955);
				match(OP_DEC);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1956);
				match(TRUE);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1957);
				match(FALSE);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1958);
				match(STAR);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1959);
				match(DIV);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1960);
				match(PERCENT);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1961);
				match(AMP);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1962);
				match(BITWISE_OR);
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1963);
				match(CARET);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1964);
				match(OP_LEFT_SHIFT);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1965);
				right_shift();
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1966);
				match(OP_EQ);
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(1967);
				match(OP_NE);
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(1968);
				match(GT);
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(1969);
				match(LT);
				}
				break;
			case 21:
				enterOuterAlt(_localctx, 21);
				{
				setState(1970);
				match(OP_GE);
				}
				break;
			case 22:
				enterOuterAlt(_localctx, 22);
				{
				setState(1971);
				match(OP_LE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Conversion_operator_declaratorContext extends ParserRuleContext {
		public TerminalNode OPERATOR() { return getToken(CSharpParser.OPERATOR, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public Arg_declarationContext arg_declaration() {
			return getRuleContext(Arg_declarationContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public TerminalNode IMPLICIT() { return getToken(CSharpParser.IMPLICIT, 0); }
		public TerminalNode EXPLICIT() { return getToken(CSharpParser.EXPLICIT, 0); }
		public Conversion_operator_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conversion_operator_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConversion_operator_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConversion_operator_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConversion_operator_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Conversion_operator_declaratorContext conversion_operator_declarator() throws RecognitionException {
		Conversion_operator_declaratorContext _localctx = new Conversion_operator_declaratorContext(_ctx, getState());
		enterRule(_localctx, 310, RULE_conversion_operator_declarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1974);
			_la = _input.LA(1);
			if ( !(_la==EXPLICIT || _la==IMPLICIT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1975);
			match(OPERATOR);
			setState(1976);
			type_();
			setState(1977);
			match(OPEN_PARENS);
			setState(1978);
			arg_declaration();
			setState(1979);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constructor_initializerContext extends ParserRuleContext {
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public TerminalNode BASE() { return getToken(CSharpParser.BASE, 0); }
		public TerminalNode THIS() { return getToken(CSharpParser.THIS, 0); }
		public Argument_listContext argument_list() {
			return getRuleContext(Argument_listContext.class,0);
		}
		public Constructor_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructor_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConstructor_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConstructor_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConstructor_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constructor_initializerContext constructor_initializer() throws RecognitionException {
		Constructor_initializerContext _localctx = new Constructor_initializerContext(_ctx, getState());
		enterRule(_localctx, 312, RULE_constructor_initializer);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1981);
			match(COLON);
			setState(1982);
			_la = _input.LA(1);
			if ( !(_la==BASE || _la==THIS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1983);
			match(OPEN_PARENS);
			setState(1985);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5419197809667089408L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305514793348041149L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
				{
				setState(1984);
				argument_list();
				}
			}

			setState(1987);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public BodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BodyContext body() throws RecognitionException {
		BodyContext _localctx = new BodyContext(_ctx, getState());
		enterRule(_localctx, 314, RULE_body);
		try {
			setState(1991);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1989);
				block();
				}
				break;
			case SEMICOLON:
				enterOuterAlt(_localctx, 2);
				{
				setState(1990);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_interfacesContext extends ParserRuleContext {
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Interface_type_listContext interface_type_list() {
			return getRuleContext(Interface_type_listContext.class,0);
		}
		public Struct_interfacesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_interfaces; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterStruct_interfaces(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitStruct_interfaces(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitStruct_interfaces(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_interfacesContext struct_interfaces() throws RecognitionException {
		Struct_interfacesContext _localctx = new Struct_interfacesContext(_ctx, getState());
		enterRule(_localctx, 316, RULE_struct_interfaces);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1993);
			match(COLON);
			setState(1994);
			interface_type_list();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<Struct_member_declarationContext> struct_member_declaration() {
			return getRuleContexts(Struct_member_declarationContext.class);
		}
		public Struct_member_declarationContext struct_member_declaration(int i) {
			return getRuleContext(Struct_member_declarationContext.class,i);
		}
		public Struct_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterStruct_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitStruct_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitStruct_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_bodyContext struct_body() throws RecognitionException {
		Struct_bodyContext _localctx = new Struct_bodyContext(_ctx, getState());
		enterRule(_localctx, 318, RULE_struct_body);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1996);
			match(OPEN_BRACE);
			setState(2000);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 9)) & ~0x3f) == 0 && ((1L << (_la - 9)) & 6547061340561119983L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 90075836208545277L) != 0)) {
				{
				{
				setState(1997);
				struct_member_declaration();
				}
				}
				setState(2002);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2003);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_member_declarationContext extends ParserRuleContext {
		public Common_member_declarationContext common_member_declaration() {
			return getRuleContext(Common_member_declarationContext.class,0);
		}
		public TerminalNode FIXED() { return getToken(CSharpParser.FIXED, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public All_member_modifiersContext all_member_modifiers() {
			return getRuleContext(All_member_modifiersContext.class,0);
		}
		public List<Fixed_size_buffer_declaratorContext> fixed_size_buffer_declarator() {
			return getRuleContexts(Fixed_size_buffer_declaratorContext.class);
		}
		public Fixed_size_buffer_declaratorContext fixed_size_buffer_declarator(int i) {
			return getRuleContext(Fixed_size_buffer_declaratorContext.class,i);
		}
		public Struct_member_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_member_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterStruct_member_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitStruct_member_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitStruct_member_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_member_declarationContext struct_member_declaration() throws RecognitionException {
		Struct_member_declarationContext _localctx = new Struct_member_declarationContext(_ctx, getState());
		enterRule(_localctx, 320, RULE_struct_member_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2006);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(2005);
				attributes();
				}
			}

			setState(2009);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,237,_ctx) ) {
			case 1:
				{
				setState(2008);
				all_member_modifiers();
				}
				break;
			}
			setState(2021);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CLASS:
			case CONST:
			case DECIMAL:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case ENUM:
			case EQUALS:
			case EVENT:
			case EXPLICIT:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case IMPLICIT:
			case INT:
			case INTERFACE:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case READONLY:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case STRING:
			case STRUCT:
			case UINT:
			case ULONG:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case VOID:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case OPEN_PARENS:
				{
				setState(2011);
				common_member_declaration();
				}
				break;
			case FIXED:
				{
				setState(2012);
				match(FIXED);
				setState(2013);
				type_();
				setState(2015); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2014);
					fixed_size_buffer_declarator();
					}
					}
					setState(2017); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 3750091428249852928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 1904629023639713L) != 0) );
				setState(2019);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Array_typeContext extends ParserRuleContext {
		public Base_typeContext base_type() {
			return getRuleContext(Base_typeContext.class,0);
		}
		public List<Rank_specifierContext> rank_specifier() {
			return getRuleContexts(Rank_specifierContext.class);
		}
		public Rank_specifierContext rank_specifier(int i) {
			return getRuleContext(Rank_specifierContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(CSharpParser.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(CSharpParser.STAR, i);
		}
		public List<TerminalNode> INTERR() { return getTokens(CSharpParser.INTERR); }
		public TerminalNode INTERR(int i) {
			return getToken(CSharpParser.INTERR, i);
		}
		public Array_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterArray_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitArray_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitArray_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Array_typeContext array_type() throws RecognitionException {
		Array_typeContext _localctx = new Array_typeContext(_ctx, getState());
		enterRule(_localctx, 322, RULE_array_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2023);
			base_type();
			setState(2031); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(2027);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==STAR || _la==INTERR) {
					{
					{
					setState(2024);
					_la = _input.LA(1);
					if ( !(_la==STAR || _la==INTERR) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					}
					setState(2029);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2030);
				rank_specifier();
				}
				}
				setState(2033); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 127)) & ~0x3f) == 0 && ((1L << (_la - 127)) & 2098177L) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Rank_specifierContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Rank_specifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rank_specifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterRank_specifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitRank_specifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitRank_specifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Rank_specifierContext rank_specifier() throws RecognitionException {
		Rank_specifierContext _localctx = new Rank_specifierContext(_ctx, getState());
		enterRule(_localctx, 324, RULE_rank_specifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2035);
			match(OPEN_BRACKET);
			setState(2039);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2036);
				match(COMMA);
				}
				}
				setState(2041);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2042);
			match(CLOSE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Array_initializerContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<Variable_initializerContext> variable_initializer() {
			return getRuleContexts(Variable_initializerContext.class);
		}
		public Variable_initializerContext variable_initializer(int i) {
			return getRuleContext(Variable_initializerContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Array_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterArray_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitArray_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitArray_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Array_initializerContext array_initializer() throws RecognitionException {
		Array_initializerContext _localctx = new Array_initializerContext(_ctx, getState());
		enterRule(_localctx, 326, RULE_array_initializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2044);
			match(OPEN_BRACE);
			setState(2056);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 4611340210375690429L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
				{
				setState(2045);
				variable_initializer();
				setState(2050);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,243,_ctx);
				while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2046);
						match(COMMA);
						setState(2047);
						variable_initializer();
						}
						} 
					}
					setState(2052);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,243,_ctx);
				}
				setState(2054);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(2053);
					match(COMMA);
					}
				}

				}
			}

			setState(2058);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variant_type_parameter_listContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(CSharpParser.LT, 0); }
		public List<Variant_type_parameterContext> variant_type_parameter() {
			return getRuleContexts(Variant_type_parameterContext.class);
		}
		public Variant_type_parameterContext variant_type_parameter(int i) {
			return getRuleContext(Variant_type_parameterContext.class,i);
		}
		public TerminalNode GT() { return getToken(CSharpParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Variant_type_parameter_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variant_type_parameter_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterVariant_type_parameter_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitVariant_type_parameter_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitVariant_type_parameter_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variant_type_parameter_listContext variant_type_parameter_list() throws RecognitionException {
		Variant_type_parameter_listContext _localctx = new Variant_type_parameter_listContext(_ctx, getState());
		enterRule(_localctx, 328, RULE_variant_type_parameter_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2060);
			match(LT);
			setState(2061);
			variant_type_parameter();
			setState(2066);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2062);
				match(COMMA);
				setState(2063);
				variant_type_parameter();
				}
				}
				setState(2068);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2069);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variant_type_parameterContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public Variance_annotationContext variance_annotation() {
			return getRuleContext(Variance_annotationContext.class,0);
		}
		public Variant_type_parameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variant_type_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterVariant_type_parameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitVariant_type_parameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitVariant_type_parameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variant_type_parameterContext variant_type_parameter() throws RecognitionException {
		Variant_type_parameterContext _localctx = new Variant_type_parameterContext(_ctx, getState());
		enterRule(_localctx, 330, RULE_variant_type_parameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2072);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(2071);
				attributes();
				}
			}

			setState(2075);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IN || _la==OUT) {
				{
				setState(2074);
				variance_annotation();
				}
			}

			setState(2077);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variance_annotationContext extends ParserRuleContext {
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public TerminalNode OUT() { return getToken(CSharpParser.OUT, 0); }
		public Variance_annotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variance_annotation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterVariance_annotation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitVariance_annotation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitVariance_annotation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variance_annotationContext variance_annotation() throws RecognitionException {
		Variance_annotationContext _localctx = new Variance_annotationContext(_ctx, getState());
		enterRule(_localctx, 332, RULE_variance_annotation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2079);
			_la = _input.LA(1);
			if ( !(_la==IN || _la==OUT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interface_baseContext extends ParserRuleContext {
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Interface_type_listContext interface_type_list() {
			return getRuleContext(Interface_type_listContext.class,0);
		}
		public Interface_baseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interface_base; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterface_base(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterface_base(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterface_base(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interface_baseContext interface_base() throws RecognitionException {
		Interface_baseContext _localctx = new Interface_baseContext(_ctx, getState());
		enterRule(_localctx, 334, RULE_interface_base);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2081);
			match(COLON);
			setState(2082);
			interface_type_list();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interface_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<Interface_member_declarationContext> interface_member_declaration() {
			return getRuleContexts(Interface_member_declarationContext.class);
		}
		public Interface_member_declarationContext interface_member_declaration(int i) {
			return getRuleContext(Interface_member_declarationContext.class,i);
		}
		public Interface_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interface_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterface_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterface_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterface_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interface_bodyContext interface_body() throws RecognitionException {
		Interface_bodyContext _localctx = new Interface_bodyContext(_ctx, getState());
		enterRule(_localctx, 336, RULE_interface_body);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2084);
			match(OPEN_BRACE);
			setState(2088);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 3273310747417398647L) != 0) || ((((_la - 75)) & ~0x3f) == 0 && ((1L << (_la - 75)) & 22518937577135473L) != 0)) {
				{
				{
				setState(2085);
				interface_member_declaration();
				}
				}
				setState(2090);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2091);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interface_member_declarationContext extends ParserRuleContext {
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode VOID() { return getToken(CSharpParser.VOID, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public TerminalNode EVENT() { return getToken(CSharpParser.EVENT, 0); }
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public TerminalNode NEW() { return getToken(CSharpParser.NEW, 0); }
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public Interface_accessorsContext interface_accessors() {
			return getRuleContext(Interface_accessorsContext.class,0);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public TerminalNode THIS() { return getToken(CSharpParser.THIS, 0); }
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public Formal_parameter_listContext formal_parameter_list() {
			return getRuleContext(Formal_parameter_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public TerminalNode UNSAFE() { return getToken(CSharpParser.UNSAFE, 0); }
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public TerminalNode READONLY() { return getToken(CSharpParser.READONLY, 0); }
		public Type_parameter_listContext type_parameter_list() {
			return getRuleContext(Type_parameter_listContext.class,0);
		}
		public Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() {
			return getRuleContext(Type_parameter_constraints_clausesContext.class,0);
		}
		public Interface_member_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interface_member_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterface_member_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterface_member_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterface_member_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interface_member_declarationContext interface_member_declaration() throws RecognitionException {
		Interface_member_declarationContext _localctx = new Interface_member_declarationContext(_ctx, getState());
		enterRule(_localctx, 338, RULE_interface_member_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2094);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(2093);
				attributes();
				}
			}

			setState(2097);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NEW) {
				{
				setState(2096);
				match(NEW);
				}
			}

			setState(2162);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,262,_ctx) ) {
			case 1:
				{
				setState(2100);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSAFE) {
					{
					setState(2099);
					match(UNSAFE);
					}
				}

				setState(2107);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,253,_ctx) ) {
				case 1:
					{
					setState(2102);
					match(REF);
					}
					break;
				case 2:
					{
					setState(2103);
					match(REF);
					setState(2104);
					match(READONLY);
					}
					break;
				case 3:
					{
					setState(2105);
					match(READONLY);
					setState(2106);
					match(REF);
					}
					break;
				}
				setState(2109);
				type_();
				setState(2137);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,257,_ctx) ) {
				case 1:
					{
					setState(2110);
					identifier();
					setState(2112);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LT) {
						{
						setState(2111);
						type_parameter_list();
						}
					}

					setState(2114);
					match(OPEN_PARENS);
					setState(2116);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 7812956763456032119L) != 0) || ((((_la - 74)) & ~0x3f) == 0 && ((1L << (_la - 74)) & 45037874618448579L) != 0)) {
						{
						setState(2115);
						formal_parameter_list();
						}
					}

					setState(2118);
					match(CLOSE_PARENS);
					setState(2120);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WHERE) {
						{
						setState(2119);
						type_parameter_constraints_clauses();
						}
					}

					setState(2122);
					match(SEMICOLON);
					}
					break;
				case 2:
					{
					setState(2124);
					identifier();
					setState(2125);
					match(OPEN_BRACE);
					setState(2126);
					interface_accessors();
					setState(2127);
					match(CLOSE_BRACE);
					}
					break;
				case 3:
					{
					setState(2129);
					match(THIS);
					setState(2130);
					match(OPEN_BRACKET);
					setState(2131);
					formal_parameter_list();
					setState(2132);
					match(CLOSE_BRACKET);
					setState(2133);
					match(OPEN_BRACE);
					setState(2134);
					interface_accessors();
					setState(2135);
					match(CLOSE_BRACE);
					}
					break;
				}
				}
				break;
			case 2:
				{
				setState(2140);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSAFE) {
					{
					setState(2139);
					match(UNSAFE);
					}
				}

				setState(2142);
				match(VOID);
				setState(2143);
				identifier();
				setState(2145);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(2144);
					type_parameter_list();
					}
				}

				setState(2147);
				match(OPEN_PARENS);
				setState(2149);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 7812956763456032119L) != 0) || ((((_la - 74)) & ~0x3f) == 0 && ((1L << (_la - 74)) & 45037874618448579L) != 0)) {
					{
					setState(2148);
					formal_parameter_list();
					}
				}

				setState(2151);
				match(CLOSE_PARENS);
				setState(2153);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(2152);
					type_parameter_constraints_clauses();
					}
				}

				setState(2155);
				match(SEMICOLON);
				}
				break;
			case 3:
				{
				setState(2157);
				match(EVENT);
				setState(2158);
				type_();
				setState(2159);
				identifier();
				setState(2160);
				match(SEMICOLON);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interface_accessorsContext extends ParserRuleContext {
		public TerminalNode GET() { return getToken(CSharpParser.GET, 0); }
		public List<TerminalNode> SEMICOLON() { return getTokens(CSharpParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(CSharpParser.SEMICOLON, i);
		}
		public TerminalNode SET() { return getToken(CSharpParser.SET, 0); }
		public List<AttributesContext> attributes() {
			return getRuleContexts(AttributesContext.class);
		}
		public AttributesContext attributes(int i) {
			return getRuleContext(AttributesContext.class,i);
		}
		public Interface_accessorsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interface_accessors; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterface_accessors(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterface_accessors(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterface_accessors(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interface_accessorsContext interface_accessors() throws RecognitionException {
		Interface_accessorsContext _localctx = new Interface_accessorsContext(_ctx, getState());
		enterRule(_localctx, 340, RULE_interface_accessors);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2165);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(2164);
				attributes();
				}
			}

			setState(2185);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GET:
				{
				setState(2167);
				match(GET);
				setState(2168);
				match(SEMICOLON);
				setState(2174);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SET || _la==OPEN_BRACKET) {
					{
					setState(2170);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==OPEN_BRACKET) {
						{
						setState(2169);
						attributes();
						}
					}

					setState(2172);
					match(SET);
					setState(2173);
					match(SEMICOLON);
					}
				}

				}
				break;
			case SET:
				{
				setState(2176);
				match(SET);
				setState(2177);
				match(SEMICOLON);
				setState(2183);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==GET || _la==OPEN_BRACKET) {
					{
					setState(2179);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==OPEN_BRACKET) {
						{
						setState(2178);
						attributes();
						}
					}

					setState(2181);
					match(GET);
					setState(2182);
					match(SEMICOLON);
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_baseContext extends ParserRuleContext {
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public Enum_baseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_base; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEnum_base(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEnum_base(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEnum_base(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Enum_baseContext enum_base() throws RecognitionException {
		Enum_baseContext _localctx = new Enum_baseContext(_ctx, getState());
		enterRule(_localctx, 342, RULE_enum_base);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2187);
			match(COLON);
			setState(2188);
			type_();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<Enum_member_declarationContext> enum_member_declaration() {
			return getRuleContexts(Enum_member_declarationContext.class);
		}
		public Enum_member_declarationContext enum_member_declaration(int i) {
			return getRuleContext(Enum_member_declarationContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Enum_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEnum_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEnum_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEnum_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Enum_bodyContext enum_body() throws RecognitionException {
		Enum_bodyContext _localctx = new Enum_bodyContext(_ctx, getState());
		enterRule(_localctx, 344, RULE_enum_body);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2190);
			match(OPEN_BRACE);
			setState(2202);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 3750091428249852928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -9221467407831136095L) != 0)) {
				{
				setState(2191);
				enum_member_declaration();
				setState(2196);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,269,_ctx);
				while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2192);
						match(COMMA);
						setState(2193);
						enum_member_declaration();
						}
						} 
					}
					setState(2198);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,269,_ctx);
				}
				setState(2200);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(2199);
					match(COMMA);
					}
				}

				}
			}

			setState(2204);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_member_declarationContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public AttributesContext attributes() {
			return getRuleContext(AttributesContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Enum_member_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_member_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEnum_member_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEnum_member_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEnum_member_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Enum_member_declarationContext enum_member_declaration() throws RecognitionException {
		Enum_member_declarationContext _localctx = new Enum_member_declarationContext(_ctx, getState());
		enterRule(_localctx, 346, RULE_enum_member_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2207);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_BRACKET) {
				{
				setState(2206);
				attributes();
				}
			}

			setState(2209);
			identifier();
			setState(2212);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGNMENT) {
				{
				setState(2210);
				match(ASSIGNMENT);
				setState(2211);
				expression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Global_attribute_sectionContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public Global_attribute_targetContext global_attribute_target() {
			return getRuleContext(Global_attribute_targetContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Attribute_listContext attribute_list() {
			return getRuleContext(Attribute_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public TerminalNode COMMA() { return getToken(CSharpParser.COMMA, 0); }
		public Global_attribute_sectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_global_attribute_section; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterGlobal_attribute_section(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitGlobal_attribute_section(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitGlobal_attribute_section(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Global_attribute_sectionContext global_attribute_section() throws RecognitionException {
		Global_attribute_sectionContext _localctx = new Global_attribute_sectionContext(_ctx, getState());
		enterRule(_localctx, 348, RULE_global_attribute_section);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2214);
			match(OPEN_BRACKET);
			setState(2215);
			global_attribute_target();
			setState(2216);
			match(COLON);
			setState(2217);
			attribute_list();
			setState(2219);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(2218);
				match(COMMA);
				}
			}

			setState(2221);
			match(CLOSE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Global_attribute_targetContext extends ParserRuleContext {
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Global_attribute_targetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_global_attribute_target; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterGlobal_attribute_target(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitGlobal_attribute_target(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitGlobal_attribute_target(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Global_attribute_targetContext global_attribute_target() throws RecognitionException {
		Global_attribute_targetContext _localctx = new Global_attribute_targetContext(_ctx, getState());
		enterRule(_localctx, 350, RULE_global_attribute_target);
		try {
			setState(2225);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,275,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2223);
				keyword();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2224);
				identifier();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AttributesContext extends ParserRuleContext {
		public List<Attribute_sectionContext> attribute_section() {
			return getRuleContexts(Attribute_sectionContext.class);
		}
		public Attribute_sectionContext attribute_section(int i) {
			return getRuleContext(Attribute_sectionContext.class,i);
		}
		public AttributesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAttributes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAttributes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAttributes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributesContext attributes() throws RecognitionException {
		AttributesContext _localctx = new AttributesContext(_ctx, getState());
		enterRule(_localctx, 352, RULE_attributes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2228); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(2227);
				attribute_section();
				}
				}
				setState(2230); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==OPEN_BRACKET );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Attribute_sectionContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public Attribute_listContext attribute_list() {
			return getRuleContext(Attribute_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public Attribute_targetContext attribute_target() {
			return getRuleContext(Attribute_targetContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public TerminalNode COMMA() { return getToken(CSharpParser.COMMA, 0); }
		public Attribute_sectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribute_section; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAttribute_section(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAttribute_section(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAttribute_section(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Attribute_sectionContext attribute_section() throws RecognitionException {
		Attribute_sectionContext _localctx = new Attribute_sectionContext(_ctx, getState());
		enterRule(_localctx, 354, RULE_attribute_section);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2232);
			match(OPEN_BRACKET);
			setState(2236);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,277,_ctx) ) {
			case 1:
				{
				setState(2233);
				attribute_target();
				setState(2234);
				match(COLON);
				}
				break;
			}
			setState(2238);
			attribute_list();
			setState(2240);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(2239);
				match(COMMA);
				}
			}

			setState(2242);
			match(CLOSE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Attribute_targetContext extends ParserRuleContext {
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Attribute_targetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribute_target; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAttribute_target(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAttribute_target(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAttribute_target(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Attribute_targetContext attribute_target() throws RecognitionException {
		Attribute_targetContext _localctx = new Attribute_targetContext(_ctx, getState());
		enterRule(_localctx, 356, RULE_attribute_target);
		try {
			setState(2246);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,279,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2244);
				keyword();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2245);
				identifier();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Attribute_listContext extends ParserRuleContext {
		public List<AttributeContext> attribute() {
			return getRuleContexts(AttributeContext.class);
		}
		public AttributeContext attribute(int i) {
			return getRuleContext(AttributeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Attribute_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribute_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAttribute_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAttribute_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAttribute_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Attribute_listContext attribute_list() throws RecognitionException {
		Attribute_listContext _localctx = new Attribute_listContext(_ctx, getState());
		enterRule(_localctx, 358, RULE_attribute_list);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2248);
			attribute();
			setState(2253);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,280,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2249);
					match(COMMA);
					setState(2250);
					attribute();
					}
					} 
				}
				setState(2255);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,280,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AttributeContext extends ParserRuleContext {
		public Namespace_or_type_nameContext namespace_or_type_name() {
			return getRuleContext(Namespace_or_type_nameContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public List<Attribute_argumentContext> attribute_argument() {
			return getRuleContexts(Attribute_argumentContext.class);
		}
		public Attribute_argumentContext attribute_argument(int i) {
			return getRuleContext(Attribute_argumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public AttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribute; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAttribute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAttribute(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAttribute(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeContext attribute() throws RecognitionException {
		AttributeContext _localctx = new AttributeContext(_ctx, getState());
		enterRule(_localctx, 360, RULE_attribute);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2256);
			namespace_or_type_name();
			setState(2269);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_PARENS) {
				{
				setState(2257);
				match(OPEN_PARENS);
				setState(2266);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(2258);
					attribute_argument();
					setState(2263);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(2259);
						match(COMMA);
						setState(2260);
						attribute_argument();
						}
						}
						setState(2265);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(2268);
				match(CLOSE_PARENS);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Attribute_argumentContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public Attribute_argumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribute_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterAttribute_argument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitAttribute_argument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitAttribute_argument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Attribute_argumentContext attribute_argument() throws RecognitionException {
		Attribute_argumentContext _localctx = new Attribute_argumentContext(_ctx, getState());
		enterRule(_localctx, 362, RULE_attribute_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2274);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,284,_ctx) ) {
			case 1:
				{
				setState(2271);
				identifier();
				setState(2272);
				match(COLON);
				}
				break;
			}
			setState(2276);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Pointer_typeContext extends ParserRuleContext {
		public TerminalNode STAR() { return getToken(CSharpParser.STAR, 0); }
		public Simple_typeContext simple_type() {
			return getRuleContext(Simple_typeContext.class,0);
		}
		public Class_typeContext class_type() {
			return getRuleContext(Class_typeContext.class,0);
		}
		public List<Rank_specifierContext> rank_specifier() {
			return getRuleContexts(Rank_specifierContext.class);
		}
		public Rank_specifierContext rank_specifier(int i) {
			return getRuleContext(Rank_specifierContext.class,i);
		}
		public List<TerminalNode> INTERR() { return getTokens(CSharpParser.INTERR); }
		public TerminalNode INTERR(int i) {
			return getToken(CSharpParser.INTERR, i);
		}
		public TerminalNode VOID() { return getToken(CSharpParser.VOID, 0); }
		public Pointer_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pointer_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterPointer_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitPointer_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitPointer_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Pointer_typeContext pointer_type() throws RecognitionException {
		Pointer_typeContext _localctx = new Pointer_typeContext(_ctx, getState());
		enterRule(_localctx, 364, RULE_pointer_type);
		int _la;
		try {
			setState(2293);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case DECIMAL:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case STRING:
			case UINT:
			case ULONG:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(2280);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case BOOL:
				case BYTE:
				case CHAR:
				case DECIMAL:
				case DOUBLE:
				case FLOAT:
				case INT:
				case LONG:
				case SBYTE:
				case SHORT:
				case UINT:
				case ULONG:
				case USHORT:
					{
					setState(2278);
					simple_type();
					}
					break;
				case ADD:
				case ALIAS:
				case ARGLIST:
				case ASCENDING:
				case ASYNC:
				case AWAIT:
				case BY:
				case DESCENDING:
				case DYNAMIC:
				case EQUALS:
				case FROM:
				case GET:
				case GROUP:
				case INTO:
				case JOIN:
				case LET:
				case NAMEOF:
				case OBJECT:
				case ON:
				case ORDERBY:
				case PARTIAL:
				case REMOVE:
				case SELECT:
				case SET:
				case STRING:
				case UNMANAGED:
				case VAR:
				case WHEN:
				case WHERE:
				case YIELD:
				case IDENTIFIER:
					{
					setState(2279);
					class_type();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(2286);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==OPEN_BRACKET || _la==INTERR) {
					{
					setState(2284);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case OPEN_BRACKET:
						{
						setState(2282);
						rank_specifier();
						}
						break;
					case INTERR:
						{
						setState(2283);
						match(INTERR);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					setState(2288);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2289);
				match(STAR);
				}
				break;
			case VOID:
				enterOuterAlt(_localctx, 2);
				{
				setState(2291);
				match(VOID);
				setState(2292);
				match(STAR);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fixed_pointer_declaratorsContext extends ParserRuleContext {
		public List<Fixed_pointer_declaratorContext> fixed_pointer_declarator() {
			return getRuleContexts(Fixed_pointer_declaratorContext.class);
		}
		public Fixed_pointer_declaratorContext fixed_pointer_declarator(int i) {
			return getRuleContext(Fixed_pointer_declaratorContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Fixed_pointer_declaratorsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fixed_pointer_declarators; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFixed_pointer_declarators(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFixed_pointer_declarators(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFixed_pointer_declarators(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fixed_pointer_declaratorsContext fixed_pointer_declarators() throws RecognitionException {
		Fixed_pointer_declaratorsContext _localctx = new Fixed_pointer_declaratorsContext(_ctx, getState());
		enterRule(_localctx, 366, RULE_fixed_pointer_declarators);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2295);
			fixed_pointer_declarator();
			setState(2300);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2296);
				match(COMMA);
				setState(2297);
				fixed_pointer_declarator();
				}
				}
				setState(2302);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fixed_pointer_declaratorContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public Fixed_pointer_initializerContext fixed_pointer_initializer() {
			return getRuleContext(Fixed_pointer_initializerContext.class,0);
		}
		public Fixed_pointer_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fixed_pointer_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFixed_pointer_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFixed_pointer_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFixed_pointer_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fixed_pointer_declaratorContext fixed_pointer_declarator() throws RecognitionException {
		Fixed_pointer_declaratorContext _localctx = new Fixed_pointer_declaratorContext(_ctx, getState());
		enterRule(_localctx, 368, RULE_fixed_pointer_declarator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2303);
			identifier();
			setState(2304);
			match(ASSIGNMENT);
			setState(2305);
			fixed_pointer_initializer();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fixed_pointer_initializerContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AMP() { return getToken(CSharpParser.AMP, 0); }
		public Stackalloc_initializerContext stackalloc_initializer() {
			return getRuleContext(Stackalloc_initializerContext.class,0);
		}
		public Fixed_pointer_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fixed_pointer_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFixed_pointer_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFixed_pointer_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFixed_pointer_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fixed_pointer_initializerContext fixed_pointer_initializer() throws RecognitionException {
		Fixed_pointer_initializerContext _localctx = new Fixed_pointer_initializerContext(_ctx, getState());
		enterRule(_localctx, 370, RULE_fixed_pointer_initializer);
		try {
			setState(2312);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(2308);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,290,_ctx) ) {
				case 1:
					{
					setState(2307);
					match(AMP);
					}
					break;
				}
				setState(2310);
				expression();
				}
				break;
			case STACKALLOC:
				enterOuterAlt(_localctx, 2);
				{
				setState(2311);
				stackalloc_initializer();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Fixed_size_buffer_declaratorContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public Fixed_size_buffer_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fixed_size_buffer_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterFixed_size_buffer_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitFixed_size_buffer_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitFixed_size_buffer_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Fixed_size_buffer_declaratorContext fixed_size_buffer_declarator() throws RecognitionException {
		Fixed_size_buffer_declaratorContext _localctx = new Fixed_size_buffer_declaratorContext(_ctx, getState());
		enterRule(_localctx, 372, RULE_fixed_size_buffer_declarator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2314);
			identifier();
			setState(2315);
			match(OPEN_BRACKET);
			setState(2316);
			expression();
			setState(2317);
			match(CLOSE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Stackalloc_initializerContext extends ParserRuleContext {
		public TerminalNode STACKALLOC() { return getToken(CSharpParser.STACKALLOC, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public Stackalloc_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stackalloc_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterStackalloc_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitStackalloc_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitStackalloc_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Stackalloc_initializerContext stackalloc_initializer() throws RecognitionException {
		Stackalloc_initializerContext _localctx = new Stackalloc_initializerContext(_ctx, getState());
		enterRule(_localctx, 374, RULE_stackalloc_initializer);
		int _la;
		try {
			int _alt;
			setState(2348);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,296,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2319);
				match(STACKALLOC);
				setState(2320);
				type_();
				setState(2321);
				match(OPEN_BRACKET);
				setState(2322);
				expression();
				setState(2323);
				match(CLOSE_BRACKET);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2325);
				match(STACKALLOC);
				setState(2327);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 3201253152842599799L) != 0) || ((((_la - 75)) & ~0x3f) == 0 && ((1L << (_la - 75)) & 18015337681329473L) != 0)) {
					{
					setState(2326);
					type_();
					}
				}

				setState(2329);
				match(OPEN_BRACKET);
				setState(2331);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
					{
					setState(2330);
					expression();
					}
				}

				setState(2333);
				match(CLOSE_BRACKET);
				setState(2334);
				match(OPEN_BRACE);
				setState(2335);
				expression();
				setState(2340);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,294,_ctx);
				while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2336);
						match(COMMA);
						setState(2337);
						expression();
						}
						} 
					}
					setState(2342);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,294,_ctx);
				}
				setState(2344);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(2343);
					match(COMMA);
					}
				}

				setState(2346);
				match(CLOSE_BRACE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Right_arrowContext extends ParserRuleContext {
		public Token first;
		public Token second;
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public TerminalNode GT() { return getToken(CSharpParser.GT, 0); }
		public Right_arrowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_right_arrow; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterRight_arrow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitRight_arrow(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitRight_arrow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Right_arrowContext right_arrow() throws RecognitionException {
		Right_arrowContext _localctx = new Right_arrowContext(_ctx, getState());
		enterRule(_localctx, 376, RULE_right_arrow);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2350);
			((Right_arrowContext)_localctx).first = match(ASSIGNMENT);
			setState(2351);
			((Right_arrowContext)_localctx).second = match(GT);
			setState(2352);
			if (!((((Right_arrowContext)_localctx).first!=null?((Right_arrowContext)_localctx).first.getTokenIndex():0) + 1 == (((Right_arrowContext)_localctx).second!=null?((Right_arrowContext)_localctx).second.getTokenIndex():0))) throw new FailedPredicateException(this, "$first.index + 1 == $second.index");
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Right_shiftContext extends ParserRuleContext {
		public Token first;
		public Token second;
		public List<TerminalNode> GT() { return getTokens(CSharpParser.GT); }
		public TerminalNode GT(int i) {
			return getToken(CSharpParser.GT, i);
		}
		public Right_shiftContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_right_shift; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterRight_shift(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitRight_shift(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitRight_shift(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Right_shiftContext right_shift() throws RecognitionException {
		Right_shiftContext _localctx = new Right_shiftContext(_ctx, getState());
		enterRule(_localctx, 378, RULE_right_shift);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2354);
			((Right_shiftContext)_localctx).first = match(GT);
			setState(2355);
			((Right_shiftContext)_localctx).second = match(GT);
			setState(2356);
			if (!((((Right_shiftContext)_localctx).first!=null?((Right_shiftContext)_localctx).first.getTokenIndex():0) + 1 == (((Right_shiftContext)_localctx).second!=null?((Right_shiftContext)_localctx).second.getTokenIndex():0))) throw new FailedPredicateException(this, "$first.index + 1 == $second.index");
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Right_shift_assignmentContext extends ParserRuleContext {
		public Token first;
		public Token second;
		public TerminalNode GT() { return getToken(CSharpParser.GT, 0); }
		public TerminalNode OP_GE() { return getToken(CSharpParser.OP_GE, 0); }
		public Right_shift_assignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_right_shift_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterRight_shift_assignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitRight_shift_assignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitRight_shift_assignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Right_shift_assignmentContext right_shift_assignment() throws RecognitionException {
		Right_shift_assignmentContext _localctx = new Right_shift_assignmentContext(_ctx, getState());
		enterRule(_localctx, 380, RULE_right_shift_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2358);
			((Right_shift_assignmentContext)_localctx).first = match(GT);
			setState(2359);
			((Right_shift_assignmentContext)_localctx).second = match(OP_GE);
			setState(2360);
			if (!((((Right_shift_assignmentContext)_localctx).first!=null?((Right_shift_assignmentContext)_localctx).first.getTokenIndex():0) + 1 == (((Right_shift_assignmentContext)_localctx).second!=null?((Right_shift_assignmentContext)_localctx).second.getTokenIndex():0))) throw new FailedPredicateException(this, "$first.index + 1 == $second.index");
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public Boolean_literalContext boolean_literal() {
			return getRuleContext(Boolean_literalContext.class,0);
		}
		public String_literalContext string_literal() {
			return getRuleContext(String_literalContext.class,0);
		}
		public TerminalNode INTEGER_LITERAL() { return getToken(CSharpParser.INTEGER_LITERAL, 0); }
		public TerminalNode HEX_INTEGER_LITERAL() { return getToken(CSharpParser.HEX_INTEGER_LITERAL, 0); }
		public TerminalNode BIN_INTEGER_LITERAL() { return getToken(CSharpParser.BIN_INTEGER_LITERAL, 0); }
		public TerminalNode REAL_LITERAL() { return getToken(CSharpParser.REAL_LITERAL, 0); }
		public TerminalNode CHARACTER_LITERAL() { return getToken(CSharpParser.CHARACTER_LITERAL, 0); }
		public TerminalNode NULL_() { return getToken(CSharpParser.NULL_, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 382, RULE_literal);
		try {
			setState(2370);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FALSE:
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(2362);
				boolean_literal();
				}
				break;
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
				enterOuterAlt(_localctx, 2);
				{
				setState(2363);
				string_literal();
				}
				break;
			case INTEGER_LITERAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(2364);
				match(INTEGER_LITERAL);
				}
				break;
			case HEX_INTEGER_LITERAL:
				enterOuterAlt(_localctx, 4);
				{
				setState(2365);
				match(HEX_INTEGER_LITERAL);
				}
				break;
			case BIN_INTEGER_LITERAL:
				enterOuterAlt(_localctx, 5);
				{
				setState(2366);
				match(BIN_INTEGER_LITERAL);
				}
				break;
			case REAL_LITERAL:
				enterOuterAlt(_localctx, 6);
				{
				setState(2367);
				match(REAL_LITERAL);
				}
				break;
			case CHARACTER_LITERAL:
				enterOuterAlt(_localctx, 7);
				{
				setState(2368);
				match(CHARACTER_LITERAL);
				}
				break;
			case NULL_:
				enterOuterAlt(_localctx, 8);
				{
				setState(2369);
				match(NULL_);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Boolean_literalContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(CSharpParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(CSharpParser.FALSE, 0); }
		public Boolean_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_boolean_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterBoolean_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitBoolean_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitBoolean_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Boolean_literalContext boolean_literal() throws RecognitionException {
		Boolean_literalContext _localctx = new Boolean_literalContext(_ctx, getState());
		enterRule(_localctx, 384, RULE_boolean_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2372);
			_la = _input.LA(1);
			if ( !(_la==FALSE || _la==TRUE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class String_literalContext extends ParserRuleContext {
		public Interpolated_regular_stringContext interpolated_regular_string() {
			return getRuleContext(Interpolated_regular_stringContext.class,0);
		}
		public Interpolated_verbatium_stringContext interpolated_verbatium_string() {
			return getRuleContext(Interpolated_verbatium_stringContext.class,0);
		}
		public TerminalNode REGULAR_STRING() { return getToken(CSharpParser.REGULAR_STRING, 0); }
		public TerminalNode VERBATIUM_STRING() { return getToken(CSharpParser.VERBATIUM_STRING, 0); }
		public String_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterString_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitString_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitString_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final String_literalContext string_literal() throws RecognitionException {
		String_literalContext _localctx = new String_literalContext(_ctx, getState());
		enterRule(_localctx, 386, RULE_string_literal);
		try {
			setState(2378);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTERPOLATED_REGULAR_STRING_START:
				enterOuterAlt(_localctx, 1);
				{
				setState(2374);
				interpolated_regular_string();
				}
				break;
			case INTERPOLATED_VERBATIUM_STRING_START:
				enterOuterAlt(_localctx, 2);
				{
				setState(2375);
				interpolated_verbatium_string();
				}
				break;
			case REGULAR_STRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(2376);
				match(REGULAR_STRING);
				}
				break;
			case VERBATIUM_STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(2377);
				match(VERBATIUM_STRING);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_regular_stringContext extends ParserRuleContext {
		public TerminalNode INTERPOLATED_REGULAR_STRING_START() { return getToken(CSharpParser.INTERPOLATED_REGULAR_STRING_START, 0); }
		public TerminalNode DOUBLE_QUOTE_INSIDE() { return getToken(CSharpParser.DOUBLE_QUOTE_INSIDE, 0); }
		public List<Interpolated_regular_string_partContext> interpolated_regular_string_part() {
			return getRuleContexts(Interpolated_regular_string_partContext.class);
		}
		public Interpolated_regular_string_partContext interpolated_regular_string_part(int i) {
			return getRuleContext(Interpolated_regular_string_partContext.class,i);
		}
		public Interpolated_regular_stringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_regular_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterpolated_regular_string(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterpolated_regular_string(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterpolated_regular_string(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_regular_stringContext interpolated_regular_string() throws RecognitionException {
		Interpolated_regular_stringContext _localctx = new Interpolated_regular_stringContext(_ctx, getState());
		enterRule(_localctx, 388, RULE_interpolated_regular_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2380);
			match(INTERPOLATED_REGULAR_STRING_START);
			setState(2384);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 329853500975553L) != 0)) {
				{
				{
				setState(2381);
				interpolated_regular_string_part();
				}
				}
				setState(2386);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2387);
			match(DOUBLE_QUOTE_INSIDE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_verbatium_stringContext extends ParserRuleContext {
		public TerminalNode INTERPOLATED_VERBATIUM_STRING_START() { return getToken(CSharpParser.INTERPOLATED_VERBATIUM_STRING_START, 0); }
		public TerminalNode DOUBLE_QUOTE_INSIDE() { return getToken(CSharpParser.DOUBLE_QUOTE_INSIDE, 0); }
		public List<Interpolated_verbatium_string_partContext> interpolated_verbatium_string_part() {
			return getRuleContexts(Interpolated_verbatium_string_partContext.class);
		}
		public Interpolated_verbatium_string_partContext interpolated_verbatium_string_part(int i) {
			return getRuleContext(Interpolated_verbatium_string_partContext.class,i);
		}
		public Interpolated_verbatium_stringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_verbatium_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterpolated_verbatium_string(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterpolated_verbatium_string(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterpolated_verbatium_string(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_verbatium_stringContext interpolated_verbatium_string() throws RecognitionException {
		Interpolated_verbatium_stringContext _localctx = new Interpolated_verbatium_stringContext(_ctx, getState());
		enterRule(_localctx, 390, RULE_interpolated_verbatium_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2389);
			match(INTERPOLATED_VERBATIUM_STRING_START);
			setState(2393);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5437212208176571392L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305497201161996477L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 646512849775041L) != 0)) {
				{
				{
				setState(2390);
				interpolated_verbatium_string_part();
				}
				}
				setState(2395);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2396);
			match(DOUBLE_QUOTE_INSIDE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_regular_string_partContext extends ParserRuleContext {
		public Interpolated_string_expressionContext interpolated_string_expression() {
			return getRuleContext(Interpolated_string_expressionContext.class,0);
		}
		public TerminalNode DOUBLE_CURLY_INSIDE() { return getToken(CSharpParser.DOUBLE_CURLY_INSIDE, 0); }
		public TerminalNode REGULAR_CHAR_INSIDE() { return getToken(CSharpParser.REGULAR_CHAR_INSIDE, 0); }
		public TerminalNode REGULAR_STRING_INSIDE() { return getToken(CSharpParser.REGULAR_STRING_INSIDE, 0); }
		public Interpolated_regular_string_partContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_regular_string_part; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterpolated_regular_string_part(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterpolated_regular_string_part(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterpolated_regular_string_part(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_regular_string_partContext interpolated_regular_string_part() throws RecognitionException {
		Interpolated_regular_string_partContext _localctx = new Interpolated_regular_string_partContext(_ctx, getState());
		enterRule(_localctx, 392, RULE_interpolated_regular_string_part);
		try {
			setState(2402);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(2398);
				interpolated_string_expression();
				}
				break;
			case DOUBLE_CURLY_INSIDE:
				enterOuterAlt(_localctx, 2);
				{
				setState(2399);
				match(DOUBLE_CURLY_INSIDE);
				}
				break;
			case REGULAR_CHAR_INSIDE:
				enterOuterAlt(_localctx, 3);
				{
				setState(2400);
				match(REGULAR_CHAR_INSIDE);
				}
				break;
			case REGULAR_STRING_INSIDE:
				enterOuterAlt(_localctx, 4);
				{
				setState(2401);
				match(REGULAR_STRING_INSIDE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_verbatium_string_partContext extends ParserRuleContext {
		public Interpolated_string_expressionContext interpolated_string_expression() {
			return getRuleContext(Interpolated_string_expressionContext.class,0);
		}
		public TerminalNode DOUBLE_CURLY_INSIDE() { return getToken(CSharpParser.DOUBLE_CURLY_INSIDE, 0); }
		public TerminalNode VERBATIUM_DOUBLE_QUOTE_INSIDE() { return getToken(CSharpParser.VERBATIUM_DOUBLE_QUOTE_INSIDE, 0); }
		public TerminalNode VERBATIUM_INSIDE_STRING() { return getToken(CSharpParser.VERBATIUM_INSIDE_STRING, 0); }
		public Interpolated_verbatium_string_partContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_verbatium_string_part; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterpolated_verbatium_string_part(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterpolated_verbatium_string_part(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterpolated_verbatium_string_part(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_verbatium_string_partContext interpolated_verbatium_string_part() throws RecognitionException {
		Interpolated_verbatium_string_partContext _localctx = new Interpolated_verbatium_string_partContext(_ctx, getState());
		enterRule(_localctx, 394, RULE_interpolated_verbatium_string_part);
		try {
			setState(2408);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
			case ALIAS:
			case ARGLIST:
			case ASCENDING:
			case ASYNC:
			case AWAIT:
			case BASE:
			case BOOL:
			case BY:
			case BYTE:
			case CHAR:
			case CHECKED:
			case DECIMAL:
			case DEFAULT:
			case DELEGATE:
			case DESCENDING:
			case DOUBLE:
			case DYNAMIC:
			case EQUALS:
			case FALSE:
			case FLOAT:
			case FROM:
			case GET:
			case GROUP:
			case INT:
			case INTO:
			case JOIN:
			case LET:
			case LONG:
			case NAMEOF:
			case NEW:
			case NULL_:
			case OBJECT:
			case ON:
			case ORDERBY:
			case PARTIAL:
			case REF:
			case REMOVE:
			case SBYTE:
			case SELECT:
			case SET:
			case SHORT:
			case SIZEOF:
			case STRING:
			case THIS:
			case TRUE:
			case TYPEOF:
			case UINT:
			case ULONG:
			case UNCHECKED:
			case UNMANAGED:
			case USHORT:
			case VAR:
			case WHEN:
			case WHERE:
			case YIELD:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case VERBATIUM_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case INTERPOLATED_VERBATIUM_STRING_START:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case STAR:
			case AMP:
			case CARET:
			case BANG:
			case TILDE:
			case OP_INC:
			case OP_DEC:
			case OP_RANGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(2404);
				interpolated_string_expression();
				}
				break;
			case DOUBLE_CURLY_INSIDE:
				enterOuterAlt(_localctx, 2);
				{
				setState(2405);
				match(DOUBLE_CURLY_INSIDE);
				}
				break;
			case VERBATIUM_DOUBLE_QUOTE_INSIDE:
				enterOuterAlt(_localctx, 3);
				{
				setState(2406);
				match(VERBATIUM_DOUBLE_QUOTE_INSIDE);
				}
				break;
			case VERBATIUM_INSIDE_STRING:
				enterOuterAlt(_localctx, 4);
				{
				setState(2407);
				match(VERBATIUM_INSIDE_STRING);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_string_expressionContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CSharpParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CSharpParser.COMMA, i);
		}
		public TerminalNode COLON() { return getToken(CSharpParser.COLON, 0); }
		public List<TerminalNode> FORMAT_STRING() { return getTokens(CSharpParser.FORMAT_STRING); }
		public TerminalNode FORMAT_STRING(int i) {
			return getToken(CSharpParser.FORMAT_STRING, i);
		}
		public Interpolated_string_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_string_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterpolated_string_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterpolated_string_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterpolated_string_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_string_expressionContext interpolated_string_expression() throws RecognitionException {
		Interpolated_string_expressionContext _localctx = new Interpolated_string_expressionContext(_ctx, getState());
		enterRule(_localctx, 396, RULE_interpolated_string_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2410);
			expression();
			setState(2415);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2411);
				match(COMMA);
				setState(2412);
				expression();
				}
				}
				setState(2417);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2424);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(2418);
				match(COLON);
				setState(2420); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(2419);
					match(FORMAT_STRING);
					}
					}
					setState(2422); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==FORMAT_STRING );
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class KeywordContext extends ParserRuleContext {
		public TerminalNode ABSTRACT() { return getToken(CSharpParser.ABSTRACT, 0); }
		public TerminalNode AS() { return getToken(CSharpParser.AS, 0); }
		public TerminalNode BASE() { return getToken(CSharpParser.BASE, 0); }
		public TerminalNode BOOL() { return getToken(CSharpParser.BOOL, 0); }
		public TerminalNode BREAK() { return getToken(CSharpParser.BREAK, 0); }
		public TerminalNode BYTE() { return getToken(CSharpParser.BYTE, 0); }
		public TerminalNode CASE() { return getToken(CSharpParser.CASE, 0); }
		public TerminalNode CATCH() { return getToken(CSharpParser.CATCH, 0); }
		public TerminalNode CHAR() { return getToken(CSharpParser.CHAR, 0); }
		public TerminalNode CHECKED() { return getToken(CSharpParser.CHECKED, 0); }
		public TerminalNode CLASS() { return getToken(CSharpParser.CLASS, 0); }
		public TerminalNode CONST() { return getToken(CSharpParser.CONST, 0); }
		public TerminalNode CONTINUE() { return getToken(CSharpParser.CONTINUE, 0); }
		public TerminalNode DECIMAL() { return getToken(CSharpParser.DECIMAL, 0); }
		public TerminalNode DEFAULT() { return getToken(CSharpParser.DEFAULT, 0); }
		public TerminalNode DELEGATE() { return getToken(CSharpParser.DELEGATE, 0); }
		public TerminalNode DO() { return getToken(CSharpParser.DO, 0); }
		public TerminalNode DOUBLE() { return getToken(CSharpParser.DOUBLE, 0); }
		public TerminalNode ELSE() { return getToken(CSharpParser.ELSE, 0); }
		public TerminalNode ENUM() { return getToken(CSharpParser.ENUM, 0); }
		public TerminalNode EVENT() { return getToken(CSharpParser.EVENT, 0); }
		public TerminalNode EXPLICIT() { return getToken(CSharpParser.EXPLICIT, 0); }
		public TerminalNode EXTERN() { return getToken(CSharpParser.EXTERN, 0); }
		public TerminalNode FALSE() { return getToken(CSharpParser.FALSE, 0); }
		public TerminalNode FINALLY() { return getToken(CSharpParser.FINALLY, 0); }
		public TerminalNode FIXED() { return getToken(CSharpParser.FIXED, 0); }
		public TerminalNode FLOAT() { return getToken(CSharpParser.FLOAT, 0); }
		public TerminalNode FOR() { return getToken(CSharpParser.FOR, 0); }
		public TerminalNode FOREACH() { return getToken(CSharpParser.FOREACH, 0); }
		public TerminalNode GOTO() { return getToken(CSharpParser.GOTO, 0); }
		public TerminalNode IF() { return getToken(CSharpParser.IF, 0); }
		public TerminalNode IMPLICIT() { return getToken(CSharpParser.IMPLICIT, 0); }
		public TerminalNode IN() { return getToken(CSharpParser.IN, 0); }
		public TerminalNode INT() { return getToken(CSharpParser.INT, 0); }
		public TerminalNode INTERFACE() { return getToken(CSharpParser.INTERFACE, 0); }
		public TerminalNode INTERNAL() { return getToken(CSharpParser.INTERNAL, 0); }
		public TerminalNode IS() { return getToken(CSharpParser.IS, 0); }
		public TerminalNode LOCK() { return getToken(CSharpParser.LOCK, 0); }
		public TerminalNode LONG() { return getToken(CSharpParser.LONG, 0); }
		public TerminalNode NAMESPACE() { return getToken(CSharpParser.NAMESPACE, 0); }
		public TerminalNode NEW() { return getToken(CSharpParser.NEW, 0); }
		public TerminalNode NULL_() { return getToken(CSharpParser.NULL_, 0); }
		public TerminalNode OBJECT() { return getToken(CSharpParser.OBJECT, 0); }
		public TerminalNode OPERATOR() { return getToken(CSharpParser.OPERATOR, 0); }
		public TerminalNode OUT() { return getToken(CSharpParser.OUT, 0); }
		public TerminalNode OVERRIDE() { return getToken(CSharpParser.OVERRIDE, 0); }
		public TerminalNode PARAMS() { return getToken(CSharpParser.PARAMS, 0); }
		public TerminalNode PRIVATE() { return getToken(CSharpParser.PRIVATE, 0); }
		public TerminalNode PROTECTED() { return getToken(CSharpParser.PROTECTED, 0); }
		public TerminalNode PUBLIC() { return getToken(CSharpParser.PUBLIC, 0); }
		public TerminalNode READONLY() { return getToken(CSharpParser.READONLY, 0); }
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public TerminalNode RETURN() { return getToken(CSharpParser.RETURN, 0); }
		public TerminalNode SBYTE() { return getToken(CSharpParser.SBYTE, 0); }
		public TerminalNode SEALED() { return getToken(CSharpParser.SEALED, 0); }
		public TerminalNode SHORT() { return getToken(CSharpParser.SHORT, 0); }
		public TerminalNode SIZEOF() { return getToken(CSharpParser.SIZEOF, 0); }
		public TerminalNode STACKALLOC() { return getToken(CSharpParser.STACKALLOC, 0); }
		public TerminalNode STATIC() { return getToken(CSharpParser.STATIC, 0); }
		public TerminalNode STRING() { return getToken(CSharpParser.STRING, 0); }
		public TerminalNode STRUCT() { return getToken(CSharpParser.STRUCT, 0); }
		public TerminalNode SWITCH() { return getToken(CSharpParser.SWITCH, 0); }
		public TerminalNode THIS() { return getToken(CSharpParser.THIS, 0); }
		public TerminalNode THROW() { return getToken(CSharpParser.THROW, 0); }
		public TerminalNode TRUE() { return getToken(CSharpParser.TRUE, 0); }
		public TerminalNode TRY() { return getToken(CSharpParser.TRY, 0); }
		public TerminalNode TYPEOF() { return getToken(CSharpParser.TYPEOF, 0); }
		public TerminalNode UINT() { return getToken(CSharpParser.UINT, 0); }
		public TerminalNode ULONG() { return getToken(CSharpParser.ULONG, 0); }
		public TerminalNode UNCHECKED() { return getToken(CSharpParser.UNCHECKED, 0); }
		public TerminalNode UNMANAGED() { return getToken(CSharpParser.UNMANAGED, 0); }
		public TerminalNode UNSAFE() { return getToken(CSharpParser.UNSAFE, 0); }
		public TerminalNode USHORT() { return getToken(CSharpParser.USHORT, 0); }
		public TerminalNode USING() { return getToken(CSharpParser.USING, 0); }
		public TerminalNode VIRTUAL() { return getToken(CSharpParser.VIRTUAL, 0); }
		public TerminalNode VOID() { return getToken(CSharpParser.VOID, 0); }
		public TerminalNode VOLATILE() { return getToken(CSharpParser.VOLATILE, 0); }
		public TerminalNode WHILE() { return getToken(CSharpParser.WHILE, 0); }
		public KeywordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyword; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterKeyword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitKeyword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitKeyword(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordContext keyword() throws RecognitionException {
		KeywordContext _localctx = new KeywordContext(_ctx, getState());
		enterRule(_localctx, 398, RULE_keyword);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2426);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & -3750091428249853440L) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & 173722833976239L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_definitionContext extends ParserRuleContext {
		public TerminalNode CLASS() { return getToken(CSharpParser.CLASS, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Class_bodyContext class_body() {
			return getRuleContext(Class_bodyContext.class,0);
		}
		public Type_parameter_listContext type_parameter_list() {
			return getRuleContext(Type_parameter_listContext.class,0);
		}
		public Class_baseContext class_base() {
			return getRuleContext(Class_baseContext.class,0);
		}
		public Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() {
			return getRuleContext(Type_parameter_constraints_clausesContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Class_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterClass_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitClass_definition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitClass_definition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_definitionContext class_definition() throws RecognitionException {
		Class_definitionContext _localctx = new Class_definitionContext(_ctx, getState());
		enterRule(_localctx, 400, RULE_class_definition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2428);
			match(CLASS);
			setState(2429);
			identifier();
			setState(2431);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(2430);
				type_parameter_list();
				}
			}

			setState(2434);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(2433);
				class_base();
				}
			}

			setState(2437);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(2436);
				type_parameter_constraints_clauses();
				}
			}

			setState(2439);
			class_body();
			setState(2441);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(2440);
				match(SEMICOLON);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_definitionContext extends ParserRuleContext {
		public TerminalNode STRUCT() { return getToken(CSharpParser.STRUCT, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Struct_bodyContext struct_body() {
			return getRuleContext(Struct_bodyContext.class,0);
		}
		public Type_parameter_listContext type_parameter_list() {
			return getRuleContext(Type_parameter_listContext.class,0);
		}
		public Struct_interfacesContext struct_interfaces() {
			return getRuleContext(Struct_interfacesContext.class,0);
		}
		public Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() {
			return getRuleContext(Type_parameter_constraints_clausesContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public TerminalNode READONLY() { return getToken(CSharpParser.READONLY, 0); }
		public TerminalNode REF() { return getToken(CSharpParser.REF, 0); }
		public Struct_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterStruct_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitStruct_definition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitStruct_definition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_definitionContext struct_definition() throws RecognitionException {
		Struct_definitionContext _localctx = new Struct_definitionContext(_ctx, getState());
		enterRule(_localctx, 402, RULE_struct_definition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2444);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==READONLY || _la==REF) {
				{
				setState(2443);
				_la = _input.LA(1);
				if ( !(_la==READONLY || _la==REF) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(2446);
			match(STRUCT);
			setState(2447);
			identifier();
			setState(2449);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(2448);
				type_parameter_list();
				}
			}

			setState(2452);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(2451);
				struct_interfaces();
				}
			}

			setState(2455);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(2454);
				type_parameter_constraints_clauses();
				}
			}

			setState(2457);
			struct_body();
			setState(2459);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(2458);
				match(SEMICOLON);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interface_definitionContext extends ParserRuleContext {
		public TerminalNode INTERFACE() { return getToken(CSharpParser.INTERFACE, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Class_bodyContext class_body() {
			return getRuleContext(Class_bodyContext.class,0);
		}
		public Variant_type_parameter_listContext variant_type_parameter_list() {
			return getRuleContext(Variant_type_parameter_listContext.class,0);
		}
		public Interface_baseContext interface_base() {
			return getRuleContext(Interface_baseContext.class,0);
		}
		public Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() {
			return getRuleContext(Type_parameter_constraints_clausesContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Interface_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interface_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterInterface_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitInterface_definition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitInterface_definition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interface_definitionContext interface_definition() throws RecognitionException {
		Interface_definitionContext _localctx = new Interface_definitionContext(_ctx, getState());
		enterRule(_localctx, 404, RULE_interface_definition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2461);
			match(INTERFACE);
			setState(2462);
			identifier();
			setState(2464);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(2463);
				variant_type_parameter_list();
				}
			}

			setState(2467);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(2466);
				interface_base();
				}
			}

			setState(2470);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(2469);
				type_parameter_constraints_clauses();
				}
			}

			setState(2472);
			class_body();
			setState(2474);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(2473);
				match(SEMICOLON);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_definitionContext extends ParserRuleContext {
		public TerminalNode ENUM() { return getToken(CSharpParser.ENUM, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Enum_bodyContext enum_body() {
			return getRuleContext(Enum_bodyContext.class,0);
		}
		public Enum_baseContext enum_base() {
			return getRuleContext(Enum_baseContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Enum_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEnum_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEnum_definition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEnum_definition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Enum_definitionContext enum_definition() throws RecognitionException {
		Enum_definitionContext _localctx = new Enum_definitionContext(_ctx, getState());
		enterRule(_localctx, 406, RULE_enum_definition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2476);
			match(ENUM);
			setState(2477);
			identifier();
			setState(2479);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(2478);
				enum_base();
				}
			}

			setState(2481);
			enum_body();
			setState(2483);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(2482);
				match(SEMICOLON);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Delegate_definitionContext extends ParserRuleContext {
		public TerminalNode DELEGATE() { return getToken(CSharpParser.DELEGATE, 0); }
		public Return_typeContext return_type() {
			return getRuleContext(Return_typeContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Variant_type_parameter_listContext variant_type_parameter_list() {
			return getRuleContext(Variant_type_parameter_listContext.class,0);
		}
		public Formal_parameter_listContext formal_parameter_list() {
			return getRuleContext(Formal_parameter_listContext.class,0);
		}
		public Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() {
			return getRuleContext(Type_parameter_constraints_clausesContext.class,0);
		}
		public Delegate_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_delegate_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterDelegate_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitDelegate_definition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitDelegate_definition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Delegate_definitionContext delegate_definition() throws RecognitionException {
		Delegate_definitionContext _localctx = new Delegate_definitionContext(_ctx, getState());
		enterRule(_localctx, 408, RULE_delegate_definition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2485);
			match(DELEGATE);
			setState(2486);
			return_type();
			setState(2487);
			identifier();
			setState(2489);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(2488);
				variant_type_parameter_list();
				}
			}

			setState(2491);
			match(OPEN_PARENS);
			setState(2493);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 7812956763456032119L) != 0) || ((((_la - 74)) & ~0x3f) == 0 && ((1L << (_la - 74)) & 45037874618448579L) != 0)) {
				{
				setState(2492);
				formal_parameter_list();
				}
			}

			setState(2495);
			match(CLOSE_PARENS);
			setState(2497);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(2496);
				type_parameter_constraints_clauses();
				}
			}

			setState(2499);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Event_declarationContext extends ParserRuleContext {
		public TerminalNode EVENT() { return getToken(CSharpParser.EVENT, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public Variable_declaratorsContext variable_declarators() {
			return getRuleContext(Variable_declaratorsContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Member_nameContext member_name() {
			return getRuleContext(Member_nameContext.class,0);
		}
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public Event_accessor_declarationsContext event_accessor_declarations() {
			return getRuleContext(Event_accessor_declarationsContext.class,0);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Event_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_event_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterEvent_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitEvent_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitEvent_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Event_declarationContext event_declaration() throws RecognitionException {
		Event_declarationContext _localctx = new Event_declarationContext(_ctx, getState());
		enterRule(_localctx, 410, RULE_event_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2501);
			match(EVENT);
			setState(2502);
			type_();
			setState(2511);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,324,_ctx) ) {
			case 1:
				{
				setState(2503);
				variable_declarators();
				setState(2504);
				match(SEMICOLON);
				}
				break;
			case 2:
				{
				setState(2506);
				member_name();
				setState(2507);
				match(OPEN_BRACE);
				setState(2508);
				event_accessor_declarations();
				setState(2509);
				match(CLOSE_BRACE);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Field_declarationContext extends ParserRuleContext {
		public Variable_declaratorsContext variable_declarators() {
			return getRuleContext(Variable_declaratorsContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Field_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterField_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitField_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitField_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Field_declarationContext field_declaration() throws RecognitionException {
		Field_declarationContext _localctx = new Field_declarationContext(_ctx, getState());
		enterRule(_localctx, 412, RULE_field_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2513);
			variable_declarators();
			setState(2514);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Property_declarationContext extends ParserRuleContext {
		public Member_nameContext member_name() {
			return getRuleContext(Member_nameContext.class,0);
		}
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public Accessor_declarationsContext accessor_declarations() {
			return getRuleContext(Accessor_declarationsContext.class,0);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public Variable_initializerContext variable_initializer() {
			return getRuleContext(Variable_initializerContext.class,0);
		}
		public Property_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_property_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterProperty_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitProperty_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitProperty_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Property_declarationContext property_declaration() throws RecognitionException {
		Property_declarationContext _localctx = new Property_declarationContext(_ctx, getState());
		enterRule(_localctx, 414, RULE_property_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2516);
			member_name();
			setState(2530);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				{
				setState(2517);
				match(OPEN_BRACE);
				setState(2518);
				accessor_declarations();
				setState(2519);
				match(CLOSE_BRACE);
				setState(2524);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ASSIGNMENT) {
					{
					setState(2520);
					match(ASSIGNMENT);
					setState(2521);
					variable_initializer();
					setState(2522);
					match(SEMICOLON);
					}
				}

				}
				break;
			case ASSIGNMENT:
				{
				setState(2526);
				right_arrow();
				setState(2527);
				throwable_expression();
				setState(2528);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constant_declarationContext extends ParserRuleContext {
		public TerminalNode CONST() { return getToken(CSharpParser.CONST, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public Constant_declaratorsContext constant_declarators() {
			return getRuleContext(Constant_declaratorsContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Constant_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConstant_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConstant_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConstant_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constant_declarationContext constant_declaration() throws RecognitionException {
		Constant_declarationContext _localctx = new Constant_declarationContext(_ctx, getState());
		enterRule(_localctx, 416, RULE_constant_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2532);
			match(CONST);
			setState(2533);
			type_();
			setState(2534);
			constant_declarators();
			setState(2535);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Indexer_declarationContext extends ParserRuleContext {
		public TerminalNode THIS() { return getToken(CSharpParser.THIS, 0); }
		public TerminalNode OPEN_BRACKET() { return getToken(CSharpParser.OPEN_BRACKET, 0); }
		public Formal_parameter_listContext formal_parameter_list() {
			return getRuleContext(Formal_parameter_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(CSharpParser.CLOSE_BRACKET, 0); }
		public TerminalNode OPEN_BRACE() { return getToken(CSharpParser.OPEN_BRACE, 0); }
		public Accessor_declarationsContext accessor_declarations() {
			return getRuleContext(Accessor_declarationsContext.class,0);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(CSharpParser.CLOSE_BRACE, 0); }
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Indexer_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indexer_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIndexer_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIndexer_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIndexer_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Indexer_declarationContext indexer_declaration() throws RecognitionException {
		Indexer_declarationContext _localctx = new Indexer_declarationContext(_ctx, getState());
		enterRule(_localctx, 418, RULE_indexer_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2537);
			match(THIS);
			setState(2538);
			match(OPEN_BRACKET);
			setState(2539);
			formal_parameter_list();
			setState(2540);
			match(CLOSE_BRACKET);
			setState(2549);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				{
				setState(2541);
				match(OPEN_BRACE);
				setState(2542);
				accessor_declarations();
				setState(2543);
				match(CLOSE_BRACE);
				}
				break;
			case ASSIGNMENT:
				{
				setState(2545);
				right_arrow();
				setState(2546);
				throwable_expression();
				setState(2547);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Destructor_definitionContext extends ParserRuleContext {
		public TerminalNode TILDE() { return getToken(CSharpParser.TILDE, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public BodyContext body() {
			return getRuleContext(BodyContext.class,0);
		}
		public Destructor_definitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_destructor_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterDestructor_definition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitDestructor_definition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitDestructor_definition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Destructor_definitionContext destructor_definition() throws RecognitionException {
		Destructor_definitionContext _localctx = new Destructor_definitionContext(_ctx, getState());
		enterRule(_localctx, 420, RULE_destructor_definition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2551);
			match(TILDE);
			setState(2552);
			identifier();
			setState(2553);
			match(OPEN_PARENS);
			setState(2554);
			match(CLOSE_PARENS);
			setState(2555);
			body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constructor_declarationContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public BodyContext body() {
			return getRuleContext(BodyContext.class,0);
		}
		public Formal_parameter_listContext formal_parameter_list() {
			return getRuleContext(Formal_parameter_listContext.class,0);
		}
		public Constructor_initializerContext constructor_initializer() {
			return getRuleContext(Constructor_initializerContext.class,0);
		}
		public Constructor_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructor_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterConstructor_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitConstructor_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitConstructor_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constructor_declarationContext constructor_declaration() throws RecognitionException {
		Constructor_declarationContext _localctx = new Constructor_declarationContext(_ctx, getState());
		enterRule(_localctx, 422, RULE_constructor_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2557);
			identifier();
			setState(2558);
			match(OPEN_PARENS);
			setState(2560);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 7812956763456032119L) != 0) || ((((_la - 74)) & ~0x3f) == 0 && ((1L << (_la - 74)) & 45037874618448579L) != 0)) {
				{
				setState(2559);
				formal_parameter_list();
				}
			}

			setState(2562);
			match(CLOSE_PARENS);
			setState(2564);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(2563);
				constructor_initializer();
				}
			}

			setState(2566);
			body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_declarationContext extends ParserRuleContext {
		public Method_member_nameContext method_member_name() {
			return getRuleContext(Method_member_nameContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Method_bodyContext method_body() {
			return getRuleContext(Method_bodyContext.class,0);
		}
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public Type_parameter_listContext type_parameter_list() {
			return getRuleContext(Type_parameter_listContext.class,0);
		}
		public Formal_parameter_listContext formal_parameter_list() {
			return getRuleContext(Formal_parameter_listContext.class,0);
		}
		public Type_parameter_constraints_clausesContext type_parameter_constraints_clauses() {
			return getRuleContext(Type_parameter_constraints_clausesContext.class,0);
		}
		public Method_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMethod_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMethod_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMethod_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_declarationContext method_declaration() throws RecognitionException {
		Method_declarationContext _localctx = new Method_declarationContext(_ctx, getState());
		enterRule(_localctx, 424, RULE_method_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2568);
			method_member_name();
			setState(2570);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(2569);
				type_parameter_list();
				}
			}

			setState(2572);
			match(OPEN_PARENS);
			setState(2574);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & 7812956763456032119L) != 0) || ((((_la - 74)) & ~0x3f) == 0 && ((1L << (_la - 74)) & 45037874618448579L) != 0)) {
				{
				setState(2573);
				formal_parameter_list();
				}
			}

			setState(2576);
			match(CLOSE_PARENS);
			setState(2578);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(2577);
				type_parameter_constraints_clauses();
				}
			}

			setState(2585);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
			case SEMICOLON:
				{
				setState(2580);
				method_body();
				}
				break;
			case ASSIGNMENT:
				{
				setState(2581);
				right_arrow();
				setState(2582);
				throwable_expression();
				setState(2583);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_member_nameContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode DOUBLE_COLON() { return getToken(CSharpParser.DOUBLE_COLON, 0); }
		public List<TerminalNode> DOT() { return getTokens(CSharpParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(CSharpParser.DOT, i);
		}
		public List<Type_argument_listContext> type_argument_list() {
			return getRuleContexts(Type_argument_listContext.class);
		}
		public Type_argument_listContext type_argument_list(int i) {
			return getRuleContext(Type_argument_listContext.class,i);
		}
		public Method_member_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_member_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMethod_member_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMethod_member_name(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMethod_member_name(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_member_nameContext method_member_name() throws RecognitionException {
		Method_member_nameContext _localctx = new Method_member_nameContext(_ctx, getState());
		enterRule(_localctx, 426, RULE_method_member_name);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2592);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,334,_ctx) ) {
			case 1:
				{
				setState(2587);
				identifier();
				}
				break;
			case 2:
				{
				setState(2588);
				identifier();
				setState(2589);
				match(DOUBLE_COLON);
				setState(2590);
				identifier();
				}
				break;
			}
			setState(2601);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,336,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2595);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LT) {
						{
						setState(2594);
						type_argument_list();
						}
					}

					setState(2597);
					match(DOT);
					setState(2598);
					identifier();
					}
					} 
				}
				setState(2603);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,336,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Operator_declarationContext extends ParserRuleContext {
		public TerminalNode OPERATOR() { return getToken(CSharpParser.OPERATOR, 0); }
		public Overloadable_operatorContext overloadable_operator() {
			return getRuleContext(Overloadable_operatorContext.class,0);
		}
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public List<Arg_declarationContext> arg_declaration() {
			return getRuleContexts(Arg_declarationContext.class);
		}
		public Arg_declarationContext arg_declaration(int i) {
			return getRuleContext(Arg_declarationContext.class,i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public BodyContext body() {
			return getRuleContext(BodyContext.class,0);
		}
		public Right_arrowContext right_arrow() {
			return getRuleContext(Right_arrowContext.class,0);
		}
		public Throwable_expressionContext throwable_expression() {
			return getRuleContext(Throwable_expressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(CSharpParser.SEMICOLON, 0); }
		public List<TerminalNode> IN() { return getTokens(CSharpParser.IN); }
		public TerminalNode IN(int i) {
			return getToken(CSharpParser.IN, i);
		}
		public TerminalNode COMMA() { return getToken(CSharpParser.COMMA, 0); }
		public Operator_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operator_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterOperator_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitOperator_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitOperator_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Operator_declarationContext operator_declaration() throws RecognitionException {
		Operator_declarationContext _localctx = new Operator_declarationContext(_ctx, getState());
		enterRule(_localctx, 428, RULE_operator_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2604);
			match(OPERATOR);
			setState(2605);
			overloadable_operator();
			setState(2606);
			match(OPEN_PARENS);
			setState(2608);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IN) {
				{
				setState(2607);
				match(IN);
				}
			}

			setState(2610);
			arg_declaration();
			setState(2616);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(2611);
				match(COMMA);
				setState(2613);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IN) {
					{
					setState(2612);
					match(IN);
					}
				}

				setState(2615);
				arg_declaration();
				}
			}

			setState(2618);
			match(CLOSE_PARENS);
			setState(2624);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
			case SEMICOLON:
				{
				setState(2619);
				body();
				}
				break;
			case ASSIGNMENT:
				{
				setState(2620);
				right_arrow();
				setState(2621);
				throwable_expression();
				setState(2622);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Arg_declarationContext extends ParserRuleContext {
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(CSharpParser.ASSIGNMENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Arg_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arg_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterArg_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitArg_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitArg_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Arg_declarationContext arg_declaration() throws RecognitionException {
		Arg_declarationContext _localctx = new Arg_declarationContext(_ctx, getState());
		enterRule(_localctx, 430, RULE_arg_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2626);
			type_();
			setState(2627);
			identifier();
			setState(2630);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGNMENT) {
				{
				setState(2628);
				match(ASSIGNMENT);
				setState(2629);
				expression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_invocationContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Argument_listContext argument_list() {
			return getRuleContext(Argument_listContext.class,0);
		}
		public Method_invocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_invocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterMethod_invocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitMethod_invocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitMethod_invocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_invocationContext method_invocation() throws RecognitionException {
		Method_invocationContext _localctx = new Method_invocationContext(_ctx, getState());
		enterRule(_localctx, 432, RULE_method_invocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2632);
			match(OPEN_PARENS);
			setState(2634);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5419197809667089408L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305514793348041149L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
				{
				setState(2633);
				argument_list();
				}
			}

			setState(2636);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Object_creation_expressionContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(CSharpParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(CSharpParser.CLOSE_PARENS, 0); }
		public Argument_listContext argument_list() {
			return getRuleContext(Argument_listContext.class,0);
		}
		public Object_or_collection_initializerContext object_or_collection_initializer() {
			return getRuleContext(Object_or_collection_initializerContext.class,0);
		}
		public Object_creation_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_object_creation_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterObject_creation_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitObject_creation_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitObject_creation_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Object_creation_expressionContext object_creation_expression() throws RecognitionException {
		Object_creation_expressionContext _localctx = new Object_creation_expressionContext(_ctx, getState());
		enterRule(_localctx, 434, RULE_object_creation_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2638);
			match(OPEN_PARENS);
			setState(2640);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -5419197809667089408L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305514793348041149L) != 0) || ((((_la - 129)) & ~0x3f) == 0 && ((1L << (_la - 129)) & 4398059153857L) != 0)) {
				{
				setState(2639);
				argument_list();
				}
			}

			setState(2642);
			match(CLOSE_PARENS);
			setState(2644);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,344,_ctx) ) {
			case 1:
				{
				setState(2643);
				object_or_collection_initializer();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(CSharpParser.IDENTIFIER, 0); }
		public TerminalNode ADD() { return getToken(CSharpParser.ADD, 0); }
		public TerminalNode ALIAS() { return getToken(CSharpParser.ALIAS, 0); }
		public TerminalNode ARGLIST() { return getToken(CSharpParser.ARGLIST, 0); }
		public TerminalNode ASCENDING() { return getToken(CSharpParser.ASCENDING, 0); }
		public TerminalNode ASYNC() { return getToken(CSharpParser.ASYNC, 0); }
		public TerminalNode AWAIT() { return getToken(CSharpParser.AWAIT, 0); }
		public TerminalNode BY() { return getToken(CSharpParser.BY, 0); }
		public TerminalNode DESCENDING() { return getToken(CSharpParser.DESCENDING, 0); }
		public TerminalNode DYNAMIC() { return getToken(CSharpParser.DYNAMIC, 0); }
		public TerminalNode EQUALS() { return getToken(CSharpParser.EQUALS, 0); }
		public TerminalNode FROM() { return getToken(CSharpParser.FROM, 0); }
		public TerminalNode GET() { return getToken(CSharpParser.GET, 0); }
		public TerminalNode GROUP() { return getToken(CSharpParser.GROUP, 0); }
		public TerminalNode INTO() { return getToken(CSharpParser.INTO, 0); }
		public TerminalNode JOIN() { return getToken(CSharpParser.JOIN, 0); }
		public TerminalNode LET() { return getToken(CSharpParser.LET, 0); }
		public TerminalNode NAMEOF() { return getToken(CSharpParser.NAMEOF, 0); }
		public TerminalNode ON() { return getToken(CSharpParser.ON, 0); }
		public TerminalNode ORDERBY() { return getToken(CSharpParser.ORDERBY, 0); }
		public TerminalNode PARTIAL() { return getToken(CSharpParser.PARTIAL, 0); }
		public TerminalNode REMOVE() { return getToken(CSharpParser.REMOVE, 0); }
		public TerminalNode SELECT() { return getToken(CSharpParser.SELECT, 0); }
		public TerminalNode SET() { return getToken(CSharpParser.SET, 0); }
		public TerminalNode UNMANAGED() { return getToken(CSharpParser.UNMANAGED, 0); }
		public TerminalNode VAR() { return getToken(CSharpParser.VAR, 0); }
		public TerminalNode WHEN() { return getToken(CSharpParser.WHEN, 0); }
		public TerminalNode WHERE() { return getToken(CSharpParser.WHERE, 0); }
		public TerminalNode YIELD() { return getToken(CSharpParser.YIELD, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CSharpParserListener ) ((CSharpParserListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CSharpParserVisitor ) return ((CSharpParserVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 436, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2646);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 3750091428249852928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 1904629023639713L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 87:
			return local_variable_declaration_sempred((Local_variable_declarationContext)_localctx, predIndex);
		case 188:
			return right_arrow_sempred((Right_arrowContext)_localctx, predIndex);
		case 189:
			return right_shift_sempred((Right_shiftContext)_localctx, predIndex);
		case 190:
			return right_shift_assignment_sempred((Right_shift_assignmentContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean local_variable_declaration_sempred(Local_variable_declarationContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return  this.IsLocalVariableDeclaration() ;
		}
		return true;
	}
	private boolean right_arrow_sempred(Right_arrowContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return (((Right_arrowContext)_localctx).first!=null?((Right_arrowContext)_localctx).first.getTokenIndex():0) + 1 == (((Right_arrowContext)_localctx).second!=null?((Right_arrowContext)_localctx).second.getTokenIndex():0);
		}
		return true;
	}
	private boolean right_shift_sempred(Right_shiftContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return (((Right_shiftContext)_localctx).first!=null?((Right_shiftContext)_localctx).first.getTokenIndex():0) + 1 == (((Right_shiftContext)_localctx).second!=null?((Right_shiftContext)_localctx).second.getTokenIndex():0);
		}
		return true;
	}
	private boolean right_shift_assignment_sempred(Right_shift_assignmentContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return (((Right_shift_assignmentContext)_localctx).first!=null?((Right_shift_assignmentContext)_localctx).first.getTokenIndex():0) + 1 == (((Right_shift_assignmentContext)_localctx).second!=null?((Right_shift_assignmentContext)_localctx).second.getTokenIndex():0);
		}
		return true;
	}

	private static final String _serializedATNSegment0 =
		"\u0004\u0001\u00c6\u0a59\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"+
		"\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007"+
		"\u001e\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007"+
		"\"\u0002#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007"+
		"\'\u0002(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007"+
		",\u0002-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u0007"+
		"1\u00022\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u0007"+
		"6\u00027\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007"+
		";\u0002<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007"+
		"@\u0002A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007"+
		"E\u0002F\u0007F\u0002G\u0007G\u0002H\u0007H\u0002I\u0007I\u0002J\u0007"+
		"J\u0002K\u0007K\u0002L\u0007L\u0002M\u0007M\u0002N\u0007N\u0002O\u0007"+
		"O\u0002P\u0007P\u0002Q\u0007Q\u0002R\u0007R\u0002S\u0007S\u0002T\u0007"+
		"T\u0002U\u0007U\u0002V\u0007V\u0002W\u0007W\u0002X\u0007X\u0002Y\u0007"+
		"Y\u0002Z\u0007Z\u0002[\u0007[\u0002\\\u0007\\\u0002]\u0007]\u0002^\u0007"+
		"^\u0002_\u0007_\u0002`\u0007`\u0002a\u0007a\u0002b\u0007b\u0002c\u0007"+
		"c\u0002d\u0007d\u0002e\u0007e\u0002f\u0007f\u0002g\u0007g\u0002h\u0007"+
		"h\u0002i\u0007i\u0002j\u0007j\u0002k\u0007k\u0002l\u0007l\u0002m\u0007"+
		"m\u0002n\u0007n\u0002o\u0007o\u0002p\u0007p\u0002q\u0007q\u0002r\u0007"+
		"r\u0002s\u0007s\u0002t\u0007t\u0002u\u0007u\u0002v\u0007v\u0002w\u0007"+
		"w\u0002x\u0007x\u0002y\u0007y\u0002z\u0007z\u0002{\u0007{\u0002|\u0007"+
		"|\u0002}\u0007}\u0002~\u0007~\u0002\u007f\u0007\u007f\u0002\u0080\u0007"+
		"\u0080\u0002\u0081\u0007\u0081\u0002\u0082\u0007\u0082\u0002\u0083\u0007"+
		"\u0083\u0002\u0084\u0007\u0084\u0002\u0085\u0007\u0085\u0002\u0086\u0007"+
		"\u0086\u0002\u0087\u0007\u0087\u0002\u0088\u0007\u0088\u0002\u0089\u0007"+
		"\u0089\u0002\u008a\u0007\u008a\u0002\u008b\u0007\u008b\u0002\u008c\u0007"+
		"\u008c\u0002\u008d\u0007\u008d\u0002\u008e\u0007\u008e\u0002\u008f\u0007"+
		"\u008f\u0002\u0090\u0007\u0090\u0002\u0091\u0007\u0091\u0002\u0092\u0007"+
		"\u0092\u0002\u0093\u0007\u0093\u0002\u0094\u0007\u0094\u0002\u0095\u0007"+
		"\u0095\u0002\u0096\u0007\u0096\u0002\u0097\u0007\u0097\u0002\u0098\u0007"+
		"\u0098\u0002\u0099\u0007\u0099\u0002\u009a\u0007\u009a\u0002\u009b\u0007"+
		"\u009b\u0002\u009c\u0007\u009c\u0002\u009d\u0007\u009d\u0002\u009e\u0007"+
		"\u009e\u0002\u009f\u0007\u009f\u0002\u00a0\u0007\u00a0\u0002\u00a1\u0007"+
		"\u00a1\u0002\u00a2\u0007\u00a2\u0002\u00a3\u0007\u00a3\u0002\u00a4\u0007"+
		"\u00a4\u0002\u00a5\u0007\u00a5\u0002\u00a6\u0007\u00a6\u0002\u00a7\u0007"+
		"\u00a7\u0002\u00a8\u0007\u00a8\u0002\u00a9\u0007\u00a9\u0002\u00aa\u0007"+
		"\u00aa\u0002\u00ab\u0007\u00ab\u0002\u00ac\u0007\u00ac\u0002\u00ad\u0007"+
		"\u00ad\u0002\u00ae\u0007\u00ae\u0002\u00af\u0007\u00af\u0002\u00b0\u0007"+
		"\u00b0\u0002\u00b1\u0007\u00b1\u0002\u00b2\u0007\u00b2\u0002\u00b3\u0007"+
		"\u00b3\u0002\u00b4\u0007\u00b4\u0002\u00b5\u0007\u00b5\u0002\u00b6\u0007"+
		"\u00b6\u0002\u00b7\u0007\u00b7\u0002\u00b8\u0007\u00b8\u0002\u00b9\u0007"+
		"\u00b9\u0002\u00ba\u0007\u00ba\u0002\u00bb\u0007\u00bb\u0002\u00bc\u0007"+
		"\u00bc\u0002\u00bd\u0007\u00bd\u0002\u00be\u0007\u00be\u0002\u00bf\u0007"+
		"\u00bf\u0002\u00c0\u0007\u00c0\u0002\u00c1\u0007\u00c1\u0002\u00c2\u0007"+
		"\u00c2\u0002\u00c3\u0007\u00c3\u0002\u00c4\u0007\u00c4\u0002\u00c5\u0007"+
		"\u00c5\u0002\u00c6\u0007\u00c6\u0002\u00c7\u0007\u00c7\u0002\u00c8\u0007"+
		"\u00c8\u0002\u00c9\u0007\u00c9\u0002\u00ca\u0007\u00ca\u0002\u00cb\u0007"+
		"\u00cb\u0002\u00cc\u0007\u00cc\u0002\u00cd\u0007\u00cd\u0002\u00ce\u0007"+
		"\u00ce\u0002\u00cf\u0007\u00cf\u0002\u00d0\u0007\u00d0\u0002\u00d1\u0007"+
		"\u00d1\u0002\u00d2\u0007\u00d2\u0002\u00d3\u0007\u00d3\u0002\u00d4\u0007"+
		"\u00d4\u0002\u00d5\u0007\u00d5\u0002\u00d6\u0007\u00d6\u0002\u00d7\u0007"+
		"\u00d7\u0002\u00d8\u0007\u00d8\u0002\u00d9\u0007\u00d9\u0002\u00da\u0007"+
		"\u00da\u0001\u0000\u0003\u0000\u01b8\b\u0000\u0001\u0000\u0003\u0000\u01bb"+
		"\b\u0000\u0001\u0000\u0003\u0000\u01be\b\u0000\u0001\u0000\u0005\u0000"+
		"\u01c1\b\u0000\n\u0000\f\u0000\u01c4\t\u0000\u0001\u0000\u0003\u0000\u01c7"+
		"\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0003\u0001\u01cd"+
		"\b\u0001\u0001\u0001\u0003\u0001\u01d0\b\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0003\u0001\u01d5\b\u0001\u0005\u0001\u01d7\b\u0001\n\u0001"+
		"\f\u0001\u01da\t\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002\u01e0\b\u0002\n\u0002\f\u0002\u01e3\t\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003\u01ea\b\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0004\u0004\u01f0\b\u0004\u000b"+
		"\u0004\f\u0004\u01f1\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0003"+
		"\u0005\u01f8\b\u0005\u0001\u0006\u0001\u0006\u0003\u0006\u01fc\b\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u0201\b\u0007\u0001\b"+
		"\u0001\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0003\n\u020b"+
		"\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u0211"+
		"\b\u000b\n\u000b\f\u000b\u0214\t\u000b\u0001\u000b\u0001\u000b\u0001\f"+
		"\u0001\f\u0001\f\u0005\f\u021b\b\f\n\f\f\f\u021e\t\f\u0001\r\u0001\r\u0001"+
		"\r\u0003\r\u0223\b\r\u0001\r\u0003\r\u0226\b\r\u0001\r\u0001\r\u0001\r"+
		"\u0003\r\u022b\b\r\u0001\r\u0003\r\u022e\b\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0003\u000e\u0234\b\u000e\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0003\u000f\u0239\b\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u0243"+
		"\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0003"+
		"\u0011\u0250\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0003\u0012\u0258\b\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0003\u0013\u025e\b\u0013\u0003\u0013\u0260\b\u0013"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0005\u0014\u0265\b\u0014\n\u0014"+
		"\f\u0014\u0268\t\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0005\u0015"+
		"\u026d\b\u0015\n\u0015\f\u0015\u0270\t\u0015\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0005\u0016\u0275\b\u0016\n\u0016\f\u0016\u0278\t\u0016\u0001\u0017"+
		"\u0001\u0017\u0001\u0017\u0005\u0017\u027d\b\u0017\n\u0017\f\u0017\u0280"+
		"\t\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0005\u0018\u0285\b\u0018"+
		"\n\u0018\f\u0018\u0288\t\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0005"+
		"\u0019\u028d\b\u0019\n\u0019\f\u0019\u0290\t\u0019\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0005\u001a"+
		"\u0299\b\u001a\n\u001a\f\u001a\u029c\t\u001a\u0001\u001b\u0001\u001b\u0001"+
		"\u001b\u0003\u001b\u02a1\b\u001b\u0001\u001b\u0005\u001b\u02a4\b\u001b"+
		"\n\u001b\f\u001b\u02a7\t\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0005"+
		"\u001c\u02ac\b\u001c\n\u001c\f\u001c\u02af\t\u001c\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0005\u001d\u02b4\b\u001d\n\u001d\f\u001d\u02b7\t\u001d\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u02be"+
		"\b\u001e\u0003\u001e\u02c0\b\u001e\u0001\u001e\u0003\u001e\u02c3\b\u001e"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0005\u001f\u02c8\b\u001f\n\u001f"+
		"\f\u001f\u02cb\t\u001f\u0001 \u0001 \u0003 \u02cf\b \u0001 \u0001 \u0001"+
		" \u0001!\u0001!\u0003!\u02d6\b!\u0001!\u0001!\u0003!\u02da\b!\u0003!\u02dc"+
		"\b!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001"+
		"\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001"+
		"\"\u0001\"\u0001\"\u0001\"\u0001\"\u0003\"\u02f4\b\"\u0001#\u0001#\u0001"+
		"#\u0001#\u0001#\u0001$\u0001$\u0003$\u02fd\b$\u0001$\u0005$\u0300\b$\n"+
		"$\f$\u0303\t$\u0001$\u0003$\u0306\b$\u0001$\u0001$\u0001$\u0001$\u0001"+
		"$\u0001$\u0003$\u030e\b$\u0001$\u0003$\u0311\b$\u0001$\u0005$\u0314\b"+
		"$\n$\f$\u0317\t$\u0001$\u0003$\u031a\b$\u0005$\u031c\b$\n$\f$\u031f\t"+
		"$\u0001%\u0001%\u0001%\u0003%\u0324\b%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0003%\u0332\b%\u0001"+
		"%\u0001%\u0001%\u0001%\u0003%\u0338\b%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0005%\u0342\b%\n%\f%\u0345\t%\u0001%\u0003%\u0348"+
		"\b%\u0001%\u0004%\u034b\b%\u000b%\f%\u034c\u0001%\u0001%\u0003%\u0351"+
		"\b%\u0001%\u0001%\u0001%\u0001%\u0003%\u0357\b%\u0001%\u0001%\u0001%\u0001"+
		"%\u0004%\u035d\b%\u000b%\f%\u035e\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0003%\u0368\b%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0003"+
		"%\u037a\b%\u0001%\u0003%\u037d\b%\u0001%\u0001%\u0001%\u0003%\u0382\b"+
		"%\u0001%\u0003%\u0385\b%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0005%\u0392\b%\n%\f%\u0395\t%\u0001%\u0001"+
		"%\u0001%\u0003%\u039a\b%\u0001&\u0001&\u0003&\u039e\b&\u0001\'\u0001\'"+
		"\u0001\'\u0001(\u0003(\u03a4\b(\u0001(\u0001(\u0001(\u0003(\u03a9\b(\u0001"+
		")\u0003)\u03ac\b)\u0001)\u0001)\u0001)\u0001)\u0005)\u03b2\b)\n)\f)\u03b5"+
		"\t)\u0001)\u0001)\u0001*\u0001*\u0001*\u0003*\u03bc\b*\u0001*\u0001*\u0001"+
		"+\u0001+\u0001,\u0001,\u0001,\u0005,\u03c5\b,\n,\f,\u03c8\t,\u0001-\u0001"+
		"-\u0003-\u03cc\b-\u0001.\u0001.\u0001.\u0003.\u03d1\b.\u0003.\u03d3\b"+
		".\u0001.\u0001.\u0001/\u0001/\u0001/\u0005/\u03da\b/\n/\f/\u03dd\t/\u0001"+
		"0\u00010\u00010\u00010\u00010\u00030\u03e4\b0\u00010\u00010\u00010\u0001"+
		"1\u00011\u00031\u03eb\b1\u00012\u00012\u00012\u00012\u00052\u03f1\b2\n"+
		"2\f2\u03f4\t2\u00012\u00032\u03f7\b2\u00012\u00012\u00013\u00013\u0001"+
		"3\u00013\u00013\u00033\u0400\b3\u00014\u00014\u00014\u00034\u0405\b4\u0003"+
		"4\u0407\b4\u00014\u00014\u00015\u00015\u00015\u00055\u040e\b5\n5\f5\u0411"+
		"\t5\u00016\u00016\u00016\u00016\u00016\u00036\u0418\b6\u00017\u00017\u0003"+
		"7\u041c\b7\u00017\u00017\u00017\u00037\u0421\b7\u00037\u0423\b7\u0001"+
		"7\u00017\u00017\u00037\u0428\b7\u00057\u042a\b7\n7\f7\u042d\t7\u00018"+
		"\u00018\u00058\u0431\b8\n8\f8\u0434\t8\u00018\u00018\u00019\u00019\u0001"+
		"9\u00059\u043b\b9\n9\f9\u043e\t9\u00019\u00039\u0441\b9\u00019\u00039"+
		"\u0444\b9\u00019\u00039\u0447\b9\u0001:\u0001:\u0001:\u0001:\u0005:\u044d"+
		"\b:\n:\f:\u0450\t:\u0001:\u0001:\u0001;\u0001;\u0001;\u0001;\u0001<\u0003"+
		"<\u0459\b<\u0001<\u0001<\u0001<\u0001<\u0001=\u0001=\u0001=\u0001=\u0001"+
		"=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0003=\u046a\b=\u0001>\u0001"+
		">\u0001>\u0005>\u046f\b>\n>\f>\u0472\t>\u0001?\u0003?\u0475\b?\u0001?"+
		"\u0001?\u0001?\u0001@\u0001@\u0001@\u0005@\u047d\b@\n@\f@\u0480\t@\u0001"+
		"A\u0001A\u0003A\u0484\bA\u0001B\u0001B\u0001B\u0001C\u0001C\u0003C\u048b"+
		"\bC\u0001C\u0001C\u0001C\u0001C\u0001D\u0005D\u0492\bD\nD\fD\u0495\tD"+
		"\u0001D\u0001D\u0003D\u0499\bD\u0001E\u0001E\u0001E\u0001E\u0001E\u0003"+
		"E\u04a0\bE\u0001F\u0001F\u0001F\u0001F\u0001F\u0001G\u0001G\u0001G\u0001"+
		"H\u0001H\u0003H\u04ac\bH\u0001H\u0001H\u0001H\u0001H\u0001H\u0001H\u0001"+
		"H\u0001H\u0001H\u0003H\u04b7\bH\u0001I\u0001I\u0001I\u0001I\u0005I\u04bd"+
		"\bI\nI\fI\u04c0\tI\u0001J\u0001J\u0003J\u04c4\bJ\u0001K\u0001K\u0001K"+
		"\u0001K\u0001K\u0001K\u0001K\u0003K\u04cd\bK\u0001L\u0001L\u0001L\u0001"+
		"L\u0001M\u0001M\u0001M\u0003M\u04d6\bM\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0003N\u04df\bN\u0001O\u0001O\u0001O\u0001P\u0003P\u04e5"+
		"\bP\u0001P\u0001P\u0001P\u0003P\u04ea\bP\u0001P\u0001P\u0003P\u04ee\b"+
		"P\u0001P\u0001P\u0003P\u04f2\bP\u0001Q\u0001Q\u0003Q\u04f6\bQ\u0001Q\u0001"+
		"Q\u0003Q\u04fa\bQ\u0001R\u0001R\u0001R\u0001R\u0001R\u0003R\u0501\bR\u0001"+
		"S\u0001S\u0001S\u0001S\u0001T\u0001T\u0003T\u0509\bT\u0001U\u0001U\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0003U\u0516"+
		"\bU\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0005U\u051e\bU\nU\fU\u0521"+
		"\tU\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0003"+
		"U\u0536\bU\u0001U\u0001U\u0003U\u053a\bU\u0001U\u0001U\u0003U\u053e\b"+
		"U\u0001U\u0001U\u0001U\u0003U\u0543\bU\u0001U\u0001U\u0001U\u0001U\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001"+
		"U\u0001U\u0001U\u0001U\u0003U\u0557\bU\u0001U\u0001U\u0001U\u0003U\u055c"+
		"\bU\u0001U\u0001U\u0001U\u0003U\u0561\bU\u0001U\u0001U\u0001U\u0001U\u0001"+
		"U\u0003U\u0568\bU\u0001U\u0003U\u056b\bU\u0001U\u0001U\u0001U\u0001U\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0003U\u0581\bU\u0001U\u0001U\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0003U\u058d\bU\u0001"+
		"V\u0001V\u0003V\u0591\bV\u0001V\u0001V\u0001W\u0001W\u0001W\u0001W\u0003"+
		"W\u0599\bW\u0001W\u0001W\u0001W\u0001W\u0001W\u0001W\u0005W\u05a1\bW\n"+
		"W\fW\u05a4\tW\u0001W\u0001W\u0001W\u0001W\u0003W\u05aa\bW\u0001X\u0001"+
		"X\u0003X\u05ae\bX\u0001Y\u0001Y\u0001Y\u0003Y\u05b3\bY\u0001Y\u0003Y\u05b6"+
		"\bY\u0001Z\u0001Z\u0001Z\u0003Z\u05bb\bZ\u0001[\u0001[\u0001[\u0001[\u0001"+
		"\\\u0001\\\u0003\\\u05c3\b\\\u0001]\u0004]\u05c6\b]\u000b]\f]\u05c7\u0001"+
		"]\u0001]\u0001^\u0001^\u0001^\u0003^\u05cf\b^\u0001^\u0001^\u0001^\u0001"+
		"^\u0003^\u05d5\b^\u0001_\u0001_\u0001_\u0001`\u0004`\u05db\b`\u000b`\f"+
		"`\u05dc\u0001a\u0001a\u0001a\u0001a\u0005a\u05e3\ba\na\fa\u05e6\ta\u0003"+
		"a\u05e8\ba\u0001b\u0001b\u0001b\u0005b\u05ed\bb\nb\fb\u05f0\tb\u0001c"+
		"\u0001c\u0005c\u05f4\bc\nc\fc\u05f7\tc\u0001c\u0003c\u05fa\bc\u0001c\u0003"+
		"c\u05fd\bc\u0001d\u0001d\u0001d\u0001d\u0003d\u0603\bd\u0001d\u0001d\u0003"+
		"d\u0607\bd\u0001d\u0001d\u0001e\u0001e\u0003e\u060d\be\u0001e\u0001e\u0001"+
		"f\u0001f\u0001f\u0001f\u0001f\u0001g\u0001g\u0001g\u0001h\u0001h\u0003"+
		"h\u061b\bh\u0001i\u0001i\u0001i\u0001i\u0003i\u0621\bi\u0001j\u0001j\u0001"+
		"j\u0005j\u0626\bj\nj\fj\u0629\tj\u0001k\u0001k\u0003k\u062d\bk\u0001k"+
		"\u0003k\u0630\bk\u0001k\u0003k\u0633\bk\u0001k\u0001k\u0001l\u0004l\u0638"+
		"\bl\u000bl\fl\u0639\u0001m\u0001m\u0001m\u0001m\u0001m\u0001n\u0004n\u0642"+
		"\bn\u000bn\fn\u0643\u0001o\u0001o\u0001o\u0001o\u0001o\u0001o\u0001o\u0001"+
		"o\u0001o\u0001o\u0001o\u0001o\u0001o\u0001o\u0001o\u0003o\u0655\bo\u0001"+
		"p\u0004p\u0658\bp\u000bp\fp\u0659\u0001q\u0001q\u0003q\u065e\bq\u0001"+
		"r\u0003r\u0661\br\u0001r\u0003r\u0664\br\u0001r\u0001r\u0001r\u0001r\u0001"+
		"r\u0003r\u066b\br\u0001s\u0001s\u0001s\u0001s\u0003s\u0671\bs\u0001t\u0001"+
		"t\u0001t\u0001t\u0005t\u0677\bt\nt\ft\u067a\tt\u0001t\u0001t\u0001u\u0003"+
		"u\u067f\bu\u0001u\u0001u\u0001v\u0001v\u0001v\u0001v\u0005v\u0687\bv\n"+
		"v\fv\u068a\tv\u0001w\u0001w\u0001w\u0005w\u068f\bw\nw\fw\u0692\tw\u0001"+
		"x\u0004x\u0695\bx\u000bx\fx\u0696\u0001y\u0001y\u0001y\u0001y\u0001y\u0001"+
		"z\u0001z\u0001z\u0001z\u0003z\u06a2\bz\u0001z\u0001z\u0003z\u06a6\bz\u0003"+
		"z\u06a8\bz\u0001{\u0001{\u0001{\u0003{\u06ad\b{\u0001{\u0001{\u0003{\u06b1"+
		"\b{\u0001|\u0001|\u0001|\u0005|\u06b6\b|\n|\f|\u06b9\t|\u0001}\u0001}"+
		"\u0001}\u0001}\u0001~\u0001~\u0003~\u06c1\b~\u0001~\u0001~\u0001\u007f"+
		"\u0004\u007f\u06c6\b\u007f\u000b\u007f\f\u007f\u06c7\u0001\u0080\u0003"+
		"\u0080\u06cb\b\u0080\u0001\u0080\u0003\u0080\u06ce\b\u0080\u0001\u0080"+
		"\u0001\u0080\u0003\u0080\u06d2\b\u0080\u0001\u0081\u0004\u0081\u06d5\b"+
		"\u0081\u000b\u0081\f\u0081\u06d6\u0001\u0082\u0001\u0082\u0001\u0083\u0001"+
		"\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0001"+
		"\u0083\u0001\u0083\u0003\u0083\u06e4\b\u0083\u0001\u0083\u0001\u0083\u0001"+
		"\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0003"+
		"\u0083\u06ee\b\u0083\u0001\u0084\u0001\u0084\u0001\u0084\u0001\u0084\u0001"+
		"\u0084\u0003\u0084\u06f5\b\u0084\u0001\u0084\u0001\u0084\u0001\u0084\u0001"+
		"\u0084\u0001\u0084\u0001\u0084\u0001\u0084\u0001\u0084\u0001\u0084\u0001"+
		"\u0084\u0003\u0084\u0701\b\u0084\u0001\u0085\u0001\u0085\u0001\u0085\u0005"+
		"\u0085\u0706\b\u0085\n\u0085\f\u0085\u0709\t\u0085\u0001\u0086\u0001\u0086"+
		"\u0001\u0086\u0001\u0086\u0001\u0087\u0001\u0087\u0001\u0087\u0005\u0087"+
		"\u0712\b\u0087\n\u0087\f\u0087\u0715\t\u0087\u0001\u0088\u0001\u0088\u0001"+
		"\u0088\u0003\u0088\u071a\b\u0088\u0001\u0089\u0001\u0089\u0003\u0089\u071e"+
		"\b\u0089\u0001\u008a\u0001\u008a\u0003\u008a\u0722\b\u008a\u0001\u008b"+
		"\u0001\u008b\u0001\u008c\u0001\u008c\u0003\u008c\u0728\b\u008c\u0001\u008d"+
		"\u0001\u008d\u0001\u008d\u0001\u008d\u0003\u008d\u072e\b\u008d\u0003\u008d"+
		"\u0730\b\u008d\u0001\u008e\u0001\u008e\u0001\u008e\u0005\u008e\u0735\b"+
		"\u008e\n\u008e\f\u008e\u0738\t\u008e\u0001\u008f\u0003\u008f\u073b\b\u008f"+
		"\u0001\u008f\u0003\u008f\u073e\b\u008f\u0001\u008f\u0001\u008f\u0003\u008f"+
		"\u0742\b\u008f\u0001\u0090\u0001\u0090\u0001\u0090\u0001\u0090\u0001\u0090"+
		"\u0001\u0090\u0001\u0090\u0001\u0090\u0003\u0090\u074c\b\u0090\u0001\u0091"+
		"\u0003\u0091\u074f\b\u0091\u0001\u0091\u0001\u0091\u0001\u0091\u0001\u0091"+
		"\u0001\u0092\u0003\u0092\u0756\b\u0092\u0001\u0092\u0003\u0092\u0759\b"+
		"\u0092\u0001\u0092\u0001\u0092\u0001\u0092\u0003\u0092\u075e\b\u0092\u0001"+
		"\u0092\u0001\u0092\u0001\u0092\u0003\u0092\u0763\b\u0092\u0003\u0092\u0765"+
		"\b\u0092\u0001\u0093\u0003\u0093\u0768\b\u0093\u0001\u0093\u0003\u0093"+
		"\u076b\b\u0093\u0001\u0093\u0001\u0093\u0001\u0093\u0001\u0094\u0003\u0094"+
		"\u0771\b\u0094\u0001\u0094\u0003\u0094\u0774\b\u0094\u0001\u0094\u0001"+
		"\u0094\u0001\u0094\u0001\u0095\u0001\u0095\u0001\u0095\u0001\u0095\u0001"+
		"\u0095\u0001\u0095\u0001\u0095\u0003\u0095\u0780\b\u0095\u0001\u0096\u0001"+
		"\u0096\u0003\u0096\u0784\b\u0096\u0001\u0097\u0003\u0097\u0787\b\u0097"+
		"\u0001\u0097\u0001\u0097\u0001\u0097\u0001\u0097\u0001\u0097\u0001\u0097"+
		"\u0001\u0097\u0001\u0097\u0003\u0097\u0791\b\u0097\u0001\u0098\u0003\u0098"+
		"\u0794\b\u0098\u0001\u0098\u0001\u0098\u0001\u0098\u0001\u0099\u0003\u0099"+
		"\u079a\b\u0099\u0001\u0099\u0001\u0099\u0001\u0099\u0001\u009a\u0001\u009a"+
		"\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a"+
		"\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a"+
		"\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009a"+
		"\u0001\u009a\u0001\u009a\u0003\u009a\u07b5\b\u009a\u0001\u009b\u0001\u009b"+
		"\u0001\u009b\u0001\u009b\u0001\u009b\u0001\u009b\u0001\u009b\u0001\u009c"+
		"\u0001\u009c\u0001\u009c\u0001\u009c\u0003\u009c\u07c2\b\u009c\u0001\u009c"+
		"\u0001\u009c\u0001\u009d\u0001\u009d\u0003\u009d\u07c8\b\u009d\u0001\u009e"+
		"\u0001\u009e\u0001\u009e\u0001\u009f\u0001\u009f\u0005\u009f\u07cf\b\u009f"+
		"\n\u009f\f\u009f\u07d2\t\u009f\u0001\u009f\u0001\u009f\u0001\u00a0\u0003"+
		"\u00a0\u07d7\b\u00a0\u0001\u00a0\u0003\u00a0\u07da\b\u00a0\u0001\u00a0"+
		"\u0001\u00a0\u0001\u00a0\u0001\u00a0\u0004\u00a0\u07e0\b\u00a0\u000b\u00a0"+
		"\f\u00a0\u07e1\u0001\u00a0\u0001\u00a0\u0003\u00a0\u07e6\b\u00a0\u0001"+
		"\u00a1\u0001\u00a1\u0005\u00a1\u07ea\b\u00a1\n\u00a1\f\u00a1\u07ed\t\u00a1"+
		"\u0001\u00a1\u0004\u00a1\u07f0\b\u00a1\u000b\u00a1\f\u00a1\u07f1\u0001"+
		"\u00a2\u0001\u00a2\u0005\u00a2\u07f6\b\u00a2\n\u00a2\f\u00a2\u07f9\t\u00a2"+
		"\u0001\u00a2\u0001\u00a2\u0001\u00a3\u0001\u00a3\u0001\u00a3\u0001\u00a3"+
		"\u0005\u00a3\u0801\b\u00a3\n\u00a3\f\u00a3\u0804\t\u00a3\u0001\u00a3\u0003"+
		"\u00a3\u0807\b\u00a3\u0003\u00a3\u0809\b\u00a3\u0001\u00a3\u0001\u00a3"+
		"\u0001\u00a4\u0001\u00a4\u0001\u00a4\u0001\u00a4\u0005\u00a4\u0811\b\u00a4"+
		"\n\u00a4\f\u00a4\u0814\t\u00a4\u0001\u00a4\u0001\u00a4\u0001\u00a5\u0003"+
		"\u00a5\u0819\b\u00a5\u0001\u00a5\u0003\u00a5\u081c\b\u00a5\u0001\u00a5"+
		"\u0001\u00a5\u0001\u00a6\u0001\u00a6\u0001\u00a7\u0001\u00a7\u0001\u00a7"+
		"\u0001\u00a8\u0001\u00a8\u0005\u00a8\u0827\b\u00a8\n\u00a8\f\u00a8\u082a"+
		"\t\u00a8\u0001\u00a8\u0001\u00a8\u0001\u00a9\u0003\u00a9\u082f\b\u00a9"+
		"\u0001\u00a9\u0003\u00a9\u0832\b\u00a9\u0001\u00a9\u0003\u00a9\u0835\b"+
		"\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0003"+
		"\u00a9\u083c\b\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0003\u00a9\u0841"+
		"\b\u00a9\u0001\u00a9\u0001\u00a9\u0003\u00a9\u0845\b\u00a9\u0001\u00a9"+
		"\u0001\u00a9\u0003\u00a9\u0849\b\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9"+
		"\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9"+
		"\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9"+
		"\u0003\u00a9\u085a\b\u00a9\u0001\u00a9\u0003\u00a9\u085d\b\u00a9\u0001"+
		"\u00a9\u0001\u00a9\u0001\u00a9\u0003\u00a9\u0862\b\u00a9\u0001\u00a9\u0001"+
		"\u00a9\u0003\u00a9\u0866\b\u00a9\u0001\u00a9\u0001\u00a9\u0003\u00a9\u086a"+
		"\b\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001\u00a9\u0001"+
		"\u00a9\u0001\u00a9\u0003\u00a9\u0873\b\u00a9\u0001\u00aa\u0003\u00aa\u0876"+
		"\b\u00aa\u0001\u00aa\u0001\u00aa\u0001\u00aa\u0003\u00aa\u087b\b\u00aa"+
		"\u0001\u00aa\u0001\u00aa\u0003\u00aa\u087f\b\u00aa\u0001\u00aa\u0001\u00aa"+
		"\u0001\u00aa\u0003\u00aa\u0884\b\u00aa\u0001\u00aa\u0001\u00aa\u0003\u00aa"+
		"\u0888\b\u00aa\u0003\u00aa\u088a\b\u00aa\u0001\u00ab\u0001\u00ab\u0001"+
		"\u00ab\u0001\u00ac\u0001\u00ac\u0001\u00ac\u0001\u00ac\u0005\u00ac\u0893"+
		"\b\u00ac\n\u00ac\f\u00ac\u0896\t\u00ac\u0001\u00ac\u0003\u00ac\u0899\b"+
		"\u00ac\u0003\u00ac\u089b\b\u00ac\u0001\u00ac\u0001\u00ac\u0001\u00ad\u0003"+
		"\u00ad\u08a0\b\u00ad\u0001\u00ad\u0001\u00ad\u0001\u00ad\u0003\u00ad\u08a5"+
		"\b\u00ad\u0001\u00ae\u0001\u00ae\u0001\u00ae\u0001\u00ae\u0001\u00ae\u0003"+
		"\u00ae\u08ac\b\u00ae\u0001\u00ae\u0001\u00ae\u0001\u00af\u0001\u00af\u0003"+
		"\u00af\u08b2\b\u00af\u0001\u00b0\u0004\u00b0\u08b5\b\u00b0\u000b\u00b0"+
		"\f\u00b0\u08b6\u0001\u00b1\u0001\u00b1\u0001\u00b1\u0001\u00b1\u0003\u00b1"+
		"\u08bd\b\u00b1\u0001\u00b1\u0001\u00b1\u0003\u00b1\u08c1\b\u00b1\u0001"+
		"\u00b1\u0001\u00b1\u0001\u00b2\u0001\u00b2\u0003\u00b2\u08c7\b\u00b2\u0001"+
		"\u00b3\u0001\u00b3\u0001\u00b3\u0005\u00b3\u08cc\b\u00b3\n\u00b3\f\u00b3"+
		"\u08cf\t\u00b3\u0001\u00b4\u0001\u00b4\u0001\u00b4\u0001\u00b4\u0001\u00b4"+
		"\u0005\u00b4\u08d6\b\u00b4\n\u00b4\f\u00b4\u08d9\t\u00b4\u0003\u00b4\u08db"+
		"\b\u00b4\u0001\u00b4\u0003\u00b4\u08de\b\u00b4\u0001\u00b5\u0001\u00b5"+
		"\u0001\u00b5\u0003\u00b5\u08e3\b\u00b5\u0001\u00b5\u0001\u00b5\u0001\u00b6"+
		"\u0001\u00b6\u0003\u00b6\u08e9\b\u00b6\u0001\u00b6\u0001\u00b6\u0005\u00b6"+
		"\u08ed\b\u00b6\n\u00b6\f\u00b6\u08f0\t\u00b6\u0001\u00b6\u0001\u00b6\u0001"+
		"\u00b6\u0001\u00b6\u0003\u00b6\u08f6\b\u00b6\u0001\u00b7\u0001\u00b7\u0001"+
		"\u00b7\u0005\u00b7\u08fb\b\u00b7\n\u00b7\f\u00b7\u08fe\t\u00b7\u0001\u00b8"+
		"\u0001\u00b8\u0001\u00b8\u0001\u00b8\u0001\u00b9\u0003\u00b9\u0905\b\u00b9"+
		"\u0001\u00b9\u0001\u00b9\u0003\u00b9\u0909\b\u00b9\u0001\u00ba\u0001\u00ba"+
		"\u0001\u00ba\u0001\u00ba\u0001\u00ba\u0001\u00bb\u0001\u00bb\u0001\u00bb"+
		"\u0001\u00bb\u0001\u00bb\u0001\u00bb\u0001\u00bb\u0001\u00bb\u0003\u00bb"+
		"\u0918\b\u00bb\u0001\u00bb\u0001\u00bb\u0003\u00bb\u091c\b\u00bb\u0001"+
		"\u00bb\u0001\u00bb\u0001\u00bb\u0001\u00bb\u0001\u00bb\u0005\u00bb\u0923"+
		"\b\u00bb\n\u00bb\f\u00bb\u0926\t\u00bb\u0001\u00bb\u0003\u00bb\u0929\b"+
		"\u00bb\u0001\u00bb\u0001\u00bb\u0003\u00bb\u092d\b\u00bb\u0001\u00bc\u0001"+
		"\u00bc\u0001\u00bc\u0001\u00bc\u0001\u00bd\u0001\u00bd\u0001\u00bd\u0001"+
		"\u00bd\u0001\u00be\u0001\u00be\u0001\u00be\u0001\u00be\u0001\u00bf\u0001"+
		"\u00bf\u0001\u00bf\u0001\u00bf\u0001\u00bf\u0001\u00bf\u0001\u00bf\u0001"+
		"\u00bf\u0003\u00bf\u0943\b\u00bf\u0001\u00c0\u0001\u00c0\u0001\u00c1\u0001"+
		"\u00c1\u0001\u00c1\u0001\u00c1\u0003\u00c1\u094b\b\u00c1\u0001\u00c2\u0001"+
		"\u00c2\u0005\u00c2\u094f\b\u00c2\n\u00c2\f\u00c2\u0952\t\u00c2\u0001\u00c2"+
		"\u0001\u00c2\u0001\u00c3\u0001\u00c3\u0005\u00c3\u0958\b\u00c3\n\u00c3"+
		"\f\u00c3\u095b\t\u00c3\u0001\u00c3\u0001\u00c3\u0001\u00c4\u0001\u00c4"+
		"\u0001\u00c4\u0001\u00c4\u0003\u00c4\u0963\b\u00c4\u0001\u00c5\u0001\u00c5"+
		"\u0001\u00c5\u0001\u00c5\u0003\u00c5\u0969\b\u00c5\u0001\u00c6\u0001\u00c6"+
		"\u0001\u00c6\u0005\u00c6\u096e\b\u00c6\n\u00c6\f\u00c6\u0971\t\u00c6\u0001"+
		"\u00c6\u0001\u00c6\u0004\u00c6\u0975\b\u00c6\u000b\u00c6\f\u00c6\u0976"+
		"\u0003\u00c6\u0979\b\u00c6\u0001\u00c7\u0001\u00c7\u0001\u00c8\u0001\u00c8"+
		"\u0001\u00c8\u0003\u00c8\u0980\b\u00c8\u0001\u00c8\u0003\u00c8\u0983\b"+
		"\u00c8\u0001\u00c8\u0003\u00c8\u0986\b\u00c8\u0001\u00c8\u0001\u00c8\u0003"+
		"\u00c8\u098a\b\u00c8\u0001\u00c9\u0003\u00c9\u098d\b\u00c9\u0001\u00c9"+
		"\u0001\u00c9\u0001\u00c9\u0003\u00c9\u0992\b\u00c9\u0001\u00c9\u0003\u00c9"+
		"\u0995\b\u00c9\u0001\u00c9\u0003\u00c9\u0998\b\u00c9\u0001\u00c9\u0001"+
		"\u00c9\u0003\u00c9\u099c\b\u00c9\u0001\u00ca\u0001\u00ca\u0001\u00ca\u0003"+
		"\u00ca\u09a1\b\u00ca\u0001\u00ca\u0003\u00ca\u09a4\b\u00ca\u0001\u00ca"+
		"\u0003\u00ca\u09a7\b\u00ca\u0001\u00ca\u0001\u00ca\u0003\u00ca\u09ab\b"+
		"\u00ca\u0001\u00cb\u0001\u00cb\u0001\u00cb\u0003\u00cb\u09b0\b\u00cb\u0001"+
		"\u00cb\u0001\u00cb\u0003\u00cb\u09b4\b\u00cb\u0001\u00cc\u0001\u00cc\u0001"+
		"\u00cc\u0001\u00cc\u0003\u00cc\u09ba\b\u00cc\u0001\u00cc\u0001\u00cc\u0003"+
		"\u00cc\u09be\b\u00cc\u0001\u00cc\u0001\u00cc\u0003\u00cc\u09c2\b\u00cc"+
		"\u0001\u00cc\u0001\u00cc\u0001\u00cd\u0001\u00cd\u0001\u00cd\u0001\u00cd"+
		"\u0001\u00cd\u0001\u00cd\u0001\u00cd\u0001\u00cd\u0001\u00cd\u0001\u00cd"+
		"\u0003\u00cd\u09d0\b\u00cd\u0001\u00ce\u0001\u00ce\u0001\u00ce\u0001\u00cf"+
		"\u0001\u00cf\u0001\u00cf\u0001\u00cf\u0001\u00cf\u0001\u00cf\u0001\u00cf"+
		"\u0001\u00cf\u0003\u00cf\u09dd\b\u00cf\u0001\u00cf\u0001\u00cf\u0001\u00cf"+
		"\u0001\u00cf\u0003\u00cf\u09e3\b\u00cf\u0001\u00d0\u0001\u00d0\u0001\u00d0"+
		"\u0001\u00d0\u0001\u00d0\u0001\u00d1\u0001\u00d1\u0001\u00d1\u0001\u00d1"+
		"\u0001\u00d1\u0001\u00d1\u0001\u00d1\u0001\u00d1\u0001\u00d1\u0001\u00d1"+
		"\u0001\u00d1\u0001\u00d1\u0003\u00d1\u09f6\b\u00d1\u0001\u00d2\u0001\u00d2"+
		"\u0001\u00d2\u0001\u00d2\u0001\u00d2\u0001\u00d2\u0001\u00d3\u0001\u00d3"+
		"\u0001\u00d3\u0003\u00d3\u0a01\b\u00d3\u0001\u00d3\u0001\u00d3\u0003\u00d3"+
		"\u0a05\b\u00d3\u0001\u00d3\u0001\u00d3\u0001\u00d4\u0001\u00d4\u0003\u00d4"+
		"\u0a0b\b\u00d4\u0001\u00d4\u0001\u00d4\u0003\u00d4\u0a0f\b\u00d4\u0001"+
		"\u00d4\u0001\u00d4\u0003\u00d4\u0a13\b\u00d4\u0001\u00d4\u0001\u00d4\u0001"+
		"\u00d4\u0001\u00d4\u0001\u00d4\u0003\u00d4\u0a1a\b\u00d4\u0001\u00d5\u0001"+
		"\u00d5\u0001\u00d5\u0001\u00d5\u0001\u00d5\u0003\u00d5\u0a21\b\u00d5\u0001"+
		"\u00d5\u0003\u00d5\u0a24\b\u00d5\u0001\u00d5\u0001\u00d5\u0005\u00d5\u0a28"+
		"\b\u00d5\n\u00d5\f\u00d5\u0a2b\t\u00d5\u0001\u00d6\u0001\u00d6\u0001\u00d6"+
		"\u0001\u00d6\u0003\u00d6\u0a31\b\u00d6\u0001\u00d6\u0001\u00d6\u0001\u00d6"+
		"\u0003\u00d6\u0a36\b\u00d6\u0001\u00d6\u0003\u00d6\u0a39\b\u00d6\u0001"+
		"\u00d6\u0001\u00d6\u0001\u00d6\u0001\u00d6\u0001\u00d6\u0001\u00d6\u0003"+
		"\u00d6\u0a41\b\u00d6\u0001\u00d7\u0001\u00d7\u0001\u00d7\u0001\u00d7\u0003"+
		"\u00d7\u0a47\b\u00d7\u0001\u00d8\u0001\u00d8\u0003\u00d8\u0a4b\b\u00d8"+
		"\u0001\u00d8\u0001\u00d8\u0001\u00d9\u0001\u00d9\u0003\u00d9\u0a51\b\u00d9"+
		"\u0001\u00d9\u0001\u00d9\u0003\u00d9\u0a55\b\u00d9\u0001\u00da\u0001\u00da"+
		"\u0001\u00da\u0000\u0000\u00db\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010"+
		"\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPR"+
		"TVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e"+
		"\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6"+
		"\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be"+
		"\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6"+
		"\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec\u00ee"+
		"\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102\u0104\u0106"+
		"\u0108\u010a\u010c\u010e\u0110\u0112\u0114\u0116\u0118\u011a\u011c\u011e"+
		"\u0120\u0122\u0124\u0126\u0128\u012a\u012c\u012e\u0130\u0132\u0134\u0136"+
		"\u0138\u013a\u013c\u013e\u0140\u0142\u0144\u0146\u0148\u014a\u014c\u014e"+
		"\u0150\u0152\u0154\u0156\u0158\u015a\u015c\u015e\u0160\u0162\u0164\u0166"+
		"\u0168\u016a\u016c\u016e\u0170\u0172\u0174\u0176\u0178\u017a\u017c\u017e"+
		"\u0180\u0182\u0184\u0186\u0188\u018a\u018c\u018e\u0190\u0192\u0194\u0196"+
		"\u0198\u019a\u019c\u019e\u01a0\u01a2\u01a4\u01a6\u01a8\u01aa\u01ac\u01ae"+
		"\u01b0\u01b2\u01b4\u0000\u0013\b\u0000\u0015\u0015\u0018\u001877??SSW"+
		"Wcdhh\u0002\u0000\"\"--\u0003\u000066HHPP\u0001\u0000\u009c\u009d\u0002"+
		"\u0000\u0092\u0093\u009e\u009f\u0001\u0000\u0087\u0088\u0001\u0000\u0089"+
		"\u008b\u000e\u0000\u0012\u0012\u0015\u0015\u0018\u0018\u001d\u001d\"\""+
		"--77??DDSSWW[[cdhh\u0002\u0000\u000e\u000e  \u0002\u0000\u000f\u000fg"+
		"g\f\u0000\t\t\u000f\u000f))99BBIIKOTTZZggkkmm\u0002\u0000((55\u0002\u0000"+
		"\u0011\u0011^^\u0002\u0000\u0089\u0089\u0094\u0094\u0002\u000066HH\u0002"+
		"\u0000**``\u0013\u0000\t\t\r\r\u0011\u0013\u0015\u001f!\"$%\'/2249;;>"+
		"?ADFFHJLPRTWikmpp\u0001\u0000OP\u0014\u0000\n\f\u000e\u0010\u0014\u0014"+
		"  ##&&0133::<=@@EEGGKKQQUVffjjnoqr\u0b66\u0000\u01b7\u0001\u0000\u0000"+
		"\u0000\u0002\u01cf\u0001\u0000\u0000\u0000\u0004\u01db\u0001\u0000\u0000"+
		"\u0000\u0006\u01e9\u0001\u0000\u0000\u0000\b\u01eb\u0001\u0000\u0000\u0000"+
		"\n\u01f5\u0001\u0000\u0000\u0000\f\u01fb\u0001\u0000\u0000\u0000\u000e"+
		"\u0200\u0001\u0000\u0000\u0000\u0010\u0202\u0001\u0000\u0000\u0000\u0012"+
		"\u0204\u0001\u0000\u0000\u0000\u0014\u020a\u0001\u0000\u0000\u0000\u0016"+
		"\u020c\u0001\u0000\u0000\u0000\u0018\u0217\u0001\u0000\u0000\u0000\u001a"+
		"\u0222\u0001\u0000\u0000\u0000\u001c\u0233\u0001\u0000\u0000\u0000\u001e"+
		"\u0238\u0001\u0000\u0000\u0000 \u0242\u0001\u0000\u0000\u0000\"\u024f"+
		"\u0001\u0000\u0000\u0000$\u0251\u0001\u0000\u0000\u0000&\u0259\u0001\u0000"+
		"\u0000\u0000(\u0261\u0001\u0000\u0000\u0000*\u0269\u0001\u0000\u0000\u0000"+
		",\u0271\u0001\u0000\u0000\u0000.\u0279\u0001\u0000\u0000\u00000\u0281"+
		"\u0001\u0000\u0000\u00002\u0289\u0001\u0000\u0000\u00004\u0291\u0001\u0000"+
		"\u0000\u00006\u029d\u0001\u0000\u0000\u00008\u02a8\u0001\u0000\u0000\u0000"+
		":\u02b0\u0001\u0000\u0000\u0000<\u02b8\u0001\u0000\u0000\u0000>\u02c4"+
		"\u0001\u0000\u0000\u0000@\u02cc\u0001\u0000\u0000\u0000B\u02db\u0001\u0000"+
		"\u0000\u0000D\u02f3\u0001\u0000\u0000\u0000F\u02f5\u0001\u0000\u0000\u0000"+
		"H\u02fa\u0001\u0000\u0000\u0000J\u0399\u0001\u0000\u0000\u0000L\u039d"+
		"\u0001\u0000\u0000\u0000N\u039f\u0001\u0000\u0000\u0000P\u03a3\u0001\u0000"+
		"\u0000\u0000R\u03ab\u0001\u0000\u0000\u0000T\u03bb\u0001\u0000\u0000\u0000"+
		"V\u03bf\u0001\u0000\u0000\u0000X\u03c1\u0001\u0000\u0000\u0000Z\u03cb"+
		"\u0001\u0000\u0000\u0000\\\u03cd\u0001\u0000\u0000\u0000^\u03d6\u0001"+
		"\u0000\u0000\u0000`\u03e3\u0001\u0000\u0000\u0000b\u03ea\u0001\u0000\u0000"+
		"\u0000d\u03ec\u0001\u0000\u0000\u0000f\u03ff\u0001\u0000\u0000\u0000h"+
		"\u0401\u0001\u0000\u0000\u0000j\u040a\u0001\u0000\u0000\u0000l\u0417\u0001"+
		"\u0000\u0000\u0000n\u0419\u0001\u0000\u0000\u0000p\u042e\u0001\u0000\u0000"+
		"\u0000r\u0437\u0001\u0000\u0000\u0000t\u0448\u0001\u0000\u0000\u0000v"+
		"\u0453\u0001\u0000\u0000\u0000x\u0458\u0001\u0000\u0000\u0000z\u0469\u0001"+
		"\u0000\u0000\u0000|\u046b\u0001\u0000\u0000\u0000~\u0474\u0001\u0000\u0000"+
		"\u0000\u0080\u0479\u0001\u0000\u0000\u0000\u0082\u0483\u0001\u0000\u0000"+
		"\u0000\u0084\u0485\u0001\u0000\u0000\u0000\u0086\u0488\u0001\u0000\u0000"+
		"\u0000\u0088\u0493\u0001\u0000\u0000\u0000\u008a\u049f\u0001\u0000\u0000"+
		"\u0000\u008c\u04a1\u0001\u0000\u0000\u0000\u008e\u04a6\u0001\u0000\u0000"+
		"\u0000\u0090\u04a9\u0001\u0000\u0000\u0000\u0092\u04b8\u0001\u0000\u0000"+
		"\u0000\u0094\u04c1\u0001\u0000\u0000\u0000\u0096\u04cc\u0001\u0000\u0000"+
		"\u0000\u0098\u04ce\u0001\u0000\u0000\u0000\u009a\u04d5\u0001\u0000\u0000"+
		"\u0000\u009c\u04de\u0001\u0000\u0000\u0000\u009e\u04e0\u0001\u0000\u0000"+
		"\u0000\u00a0\u04e4\u0001\u0000\u0000\u0000\u00a2\u04f9\u0001\u0000\u0000"+
		"\u0000\u00a4\u0500\u0001\u0000\u0000\u0000\u00a6\u0502\u0001\u0000\u0000"+
		"\u0000\u00a8\u0508\u0001\u0000\u0000\u0000\u00aa\u058c\u0001\u0000\u0000"+
		"\u0000\u00ac\u058e\u0001\u0000\u0000\u0000\u00ae\u05a9\u0001\u0000\u0000"+
		"\u0000\u00b0\u05ad\u0001\u0000\u0000\u0000\u00b2\u05af\u0001\u0000\u0000"+
		"\u0000\u00b4\u05ba\u0001\u0000\u0000\u0000\u00b6\u05bc\u0001\u0000\u0000"+
		"\u0000\u00b8\u05c2\u0001\u0000\u0000\u0000\u00ba\u05c5\u0001\u0000\u0000"+
		"\u0000\u00bc\u05d4\u0001\u0000\u0000\u0000\u00be\u05d6\u0001\u0000\u0000"+
		"\u0000\u00c0\u05da\u0001\u0000\u0000\u0000\u00c2\u05e7\u0001\u0000\u0000"+
		"\u0000\u00c4\u05e9\u0001\u0000\u0000\u0000\u00c6\u05fc\u0001\u0000\u0000"+
		"\u0000\u00c8\u05fe\u0001\u0000\u0000\u0000\u00ca\u060a\u0001\u0000\u0000"+
		"\u0000\u00cc\u0610\u0001\u0000\u0000\u0000\u00ce\u0615\u0001\u0000\u0000"+
		"\u0000\u00d0\u061a\u0001\u0000\u0000\u0000\u00d2\u061c\u0001\u0000\u0000"+
		"\u0000\u00d4\u0622\u0001\u0000\u0000\u0000\u00d6\u062a\u0001\u0000\u0000"+
		"\u0000\u00d8\u0637\u0001\u0000\u0000\u0000\u00da\u063b\u0001\u0000\u0000"+
		"\u0000\u00dc\u0641\u0001\u0000\u0000\u0000\u00de\u0654\u0001\u0000\u0000"+
		"\u0000\u00e0\u0657\u0001\u0000\u0000\u0000\u00e2\u065d\u0001\u0000\u0000"+
		"\u0000\u00e4\u0660\u0001\u0000\u0000\u0000\u00e6\u066c\u0001\u0000\u0000"+
		"\u0000\u00e8\u0672\u0001\u0000\u0000\u0000\u00ea\u067e\u0001\u0000\u0000"+
		"\u0000\u00ec\u0682\u0001\u0000\u0000\u0000\u00ee\u068b\u0001\u0000\u0000"+
		"\u0000\u00f0\u0694\u0001\u0000\u0000\u0000\u00f2\u0698\u0001\u0000\u0000"+
		"\u0000\u00f4\u06a7\u0001\u0000\u0000\u0000\u00f6\u06b0\u0001\u0000\u0000"+
		"\u0000\u00f8\u06b2\u0001\u0000\u0000\u0000\u00fa\u06ba\u0001\u0000\u0000"+
		"\u0000\u00fc\u06be\u0001\u0000\u0000\u0000\u00fe\u06c5\u0001\u0000\u0000"+
		"\u0000\u0100\u06ca\u0001\u0000\u0000\u0000\u0102\u06d4\u0001\u0000\u0000"+
		"\u0000\u0104\u06d8\u0001\u0000\u0000\u0000\u0106\u06ed\u0001\u0000\u0000"+
		"\u0000\u0108\u06f4\u0001\u0000\u0000\u0000\u010a\u0702\u0001\u0000\u0000"+
		"\u0000\u010c\u070a\u0001\u0000\u0000\u0000\u010e\u070e\u0001\u0000\u0000"+
		"\u0000\u0110\u0716\u0001\u0000\u0000\u0000\u0112\u071d\u0001\u0000\u0000"+
		"\u0000\u0114\u0721\u0001\u0000\u0000\u0000\u0116\u0723\u0001\u0000\u0000"+
		"\u0000\u0118\u0727\u0001\u0000\u0000\u0000\u011a\u072f\u0001\u0000\u0000"+
		"\u0000\u011c\u0731\u0001\u0000\u0000\u0000\u011e\u0741\u0001\u0000\u0000"+
		"\u0000\u0120\u074b\u0001\u0000\u0000\u0000\u0122\u074e\u0001\u0000\u0000"+
		"\u0000\u0124\u0755\u0001\u0000\u0000\u0000\u0126\u0767\u0001\u0000\u0000"+
		"\u0000\u0128\u0770\u0001\u0000\u0000\u0000\u012a\u077f\u0001\u0000\u0000"+
		"\u0000\u012c\u0783\u0001\u0000\u0000\u0000\u012e\u0786\u0001\u0000\u0000"+
		"\u0000\u0130\u0793\u0001\u0000\u0000\u0000\u0132\u0799\u0001\u0000\u0000"+
		"\u0000\u0134\u07b4\u0001\u0000\u0000\u0000\u0136\u07b6\u0001\u0000\u0000"+
		"\u0000\u0138\u07bd\u0001\u0000\u0000\u0000\u013a\u07c7\u0001\u0000\u0000"+
		"\u0000\u013c\u07c9\u0001\u0000\u0000\u0000\u013e\u07cc\u0001\u0000\u0000"+
		"\u0000\u0140\u07d6\u0001\u0000\u0000\u0000\u0142\u07e7\u0001\u0000\u0000"+
		"\u0000\u0144\u07f3\u0001\u0000\u0000\u0000\u0146\u07fc\u0001\u0000\u0000"+
		"\u0000\u0148\u080c\u0001\u0000\u0000\u0000\u014a\u0818\u0001\u0000\u0000"+
		"\u0000\u014c\u081f\u0001\u0000\u0000\u0000\u014e\u0821\u0001\u0000\u0000"+
		"\u0000\u0150\u0824\u0001\u0000\u0000\u0000\u0152\u082e\u0001\u0000\u0000"+
		"\u0000\u0154\u0875\u0001\u0000\u0000\u0000\u0156\u088b\u0001\u0000\u0000"+
		"\u0000\u0158\u088e\u0001\u0000\u0000\u0000\u015a\u089f\u0001\u0000\u0000"+
		"\u0000\u015c\u08a6\u0001\u0000\u0000\u0000\u015e\u08b1\u0001\u0000\u0000"+
		"\u0000\u0160\u08b4\u0001\u0000\u0000\u0000\u0162\u08b8\u0001\u0000\u0000"+
		"\u0000\u0164\u08c6\u0001\u0000\u0000\u0000\u0166\u08c8\u0001\u0000\u0000"+
		"\u0000\u0168\u08d0\u0001\u0000\u0000\u0000\u016a\u08e2\u0001\u0000\u0000"+
		"\u0000\u016c\u08f5\u0001\u0000\u0000\u0000\u016e\u08f7\u0001\u0000\u0000"+
		"\u0000\u0170\u08ff\u0001\u0000\u0000\u0000\u0172\u0908\u0001\u0000\u0000"+
		"\u0000\u0174\u090a\u0001\u0000\u0000\u0000\u0176\u092c\u0001\u0000\u0000"+
		"\u0000\u0178\u092e\u0001\u0000\u0000\u0000\u017a\u0932\u0001\u0000\u0000"+
		"\u0000\u017c\u0936\u0001\u0000\u0000\u0000\u017e\u0942\u0001\u0000\u0000"+
		"\u0000\u0180\u0944\u0001\u0000\u0000\u0000\u0182\u094a\u0001\u0000\u0000"+
		"\u0000\u0184\u094c\u0001\u0000\u0000\u0000\u0186\u0955\u0001\u0000\u0000"+
		"\u0000\u0188\u0962\u0001\u0000\u0000\u0000\u018a\u0968\u0001\u0000\u0000"+
		"\u0000\u018c\u096a\u0001\u0000\u0000\u0000\u018e\u097a\u0001\u0000\u0000"+
		"\u0000\u0190\u097c\u0001\u0000\u0000\u0000\u0192\u098c\u0001\u0000\u0000"+
		"\u0000\u0194\u099d\u0001\u0000\u0000\u0000\u0196\u09ac\u0001\u0000\u0000"+
		"\u0000\u0198\u09b5\u0001\u0000\u0000\u0000\u019a\u09c5\u0001\u0000\u0000"+
		"\u0000\u019c\u09d1\u0001\u0000\u0000\u0000\u019e\u09d4\u0001\u0000\u0000"+
		"\u0000\u01a0\u09e4\u0001\u0000\u0000\u0000\u01a2\u09e9\u0001\u0000\u0000"+
		"\u0000\u01a4\u09f7\u0001\u0000\u0000\u0000\u01a6\u09fd\u0001\u0000\u0000"+
		"\u0000\u01a8\u0a08\u0001\u0000\u0000\u0000\u01aa\u0a20\u0001\u0000\u0000"+
		"\u0000\u01ac\u0a2c\u0001\u0000\u0000\u0000\u01ae\u0a42\u0001\u0000\u0000"+
		"\u0000\u01b0\u0a48\u0001\u0000\u0000\u0000\u01b2\u0a4e\u0001\u0000\u0000"+
		"\u0000\u01b4\u0a56\u0001\u0000\u0000\u0000\u01b6\u01b8\u0005\u0001\u0000"+
		"\u0000\u01b7\u01b6\u0001\u0000\u0000\u0000\u01b7\u01b8\u0001\u0000\u0000"+
		"\u0000\u01b8\u01ba\u0001\u0000\u0000\u0000\u01b9\u01bb\u0003\u00d8l\u0000"+
		"\u01ba\u01b9\u0001\u0000\u0000\u0000\u01ba\u01bb\u0001\u0000\u0000\u0000"+
		"\u01bb\u01bd\u0001\u0000\u0000\u0000\u01bc\u01be\u0003\u00dcn\u0000\u01bd"+
		"\u01bc\u0001\u0000\u0000\u0000\u01bd\u01be\u0001\u0000\u0000\u0000\u01be"+
		"\u01c2\u0001\u0000\u0000\u0000\u01bf\u01c1\u0003\u015c\u00ae\u0000\u01c0"+
		"\u01bf\u0001\u0000\u0000\u0000\u01c1\u01c4\u0001\u0000\u0000\u0000\u01c2"+
		"\u01c0\u0001\u0000\u0000\u0000\u01c2\u01c3\u0001\u0000\u0000\u0000\u01c3"+
		"\u01c6\u0001\u0000\u0000\u0000\u01c4\u01c2\u0001\u0000\u0000\u0000\u01c5"+
		"\u01c7\u0003\u00e0p\u0000\u01c6\u01c5\u0001\u0000\u0000\u0000\u01c6\u01c7"+
		"\u0001\u0000\u0000\u0000\u01c7\u01c8\u0001\u0000\u0000\u0000\u01c8\u01c9"+
		"\u0005\u0000\u0000\u0001\u01c9\u0001\u0001\u0000\u0000\u0000\u01ca\u01cc"+
		"\u0003\u01b4\u00da\u0000\u01cb\u01cd\u0003\u0016\u000b\u0000\u01cc\u01cb"+
		"\u0001\u0000\u0000\u0000\u01cc\u01cd\u0001\u0000\u0000\u0000\u01cd\u01d0"+
		"\u0001\u0000\u0000\u0000\u01ce\u01d0\u0003\u00e6s\u0000\u01cf\u01ca\u0001"+
		"\u0000\u0000\u0000\u01cf\u01ce\u0001\u0000\u0000\u0000\u01d0\u01d8\u0001"+
		"\u0000\u0000\u0000\u01d1\u01d2\u0005\u0083\u0000\u0000\u01d2\u01d4\u0003"+
		"\u01b4\u00da\u0000\u01d3\u01d5\u0003\u0016\u000b\u0000\u01d4\u01d3\u0001"+
		"\u0000\u0000\u0000\u01d4\u01d5\u0001\u0000\u0000\u0000\u01d5\u01d7\u0001"+
		"\u0000\u0000\u0000\u01d6\u01d1\u0001\u0000\u0000\u0000\u01d7\u01da\u0001"+
		"\u0000\u0000\u0000\u01d8\u01d6\u0001\u0000\u0000\u0000\u01d8\u01d9\u0001"+
		"\u0000\u0000\u0000\u01d9\u0003\u0001\u0000\u0000\u0000\u01da\u01d8\u0001"+
		"\u0000\u0000\u0000\u01db\u01e1\u0003\u0006\u0003\u0000\u01dc\u01e0\u0005"+
		"\u0094\u0000\u0000\u01dd\u01e0\u0003\u0144\u00a2\u0000\u01de\u01e0\u0005"+
		"\u0089\u0000\u0000\u01df\u01dc\u0001\u0000\u0000\u0000\u01df\u01dd\u0001"+
		"\u0000\u0000\u0000\u01df\u01de\u0001\u0000\u0000\u0000\u01e0\u01e3\u0001"+
		"\u0000\u0000\u0000\u01e1\u01df\u0001\u0000\u0000\u0000\u01e1\u01e2\u0001"+
		"\u0000\u0000\u0000\u01e2\u0005\u0001\u0000\u0000\u0000\u01e3\u01e1\u0001"+
		"\u0000\u0000\u0000\u01e4\u01ea\u0003\f\u0006\u0000\u01e5\u01ea\u0003\u0014"+
		"\n\u0000\u01e6\u01e7\u0005l\u0000\u0000\u01e7\u01ea\u0005\u0089\u0000"+
		"\u0000\u01e8\u01ea\u0003\b\u0004\u0000\u01e9\u01e4\u0001\u0000\u0000\u0000"+
		"\u01e9\u01e5\u0001\u0000\u0000\u0000\u01e9\u01e6\u0001\u0000\u0000\u0000"+
		"\u01e9\u01e8\u0001\u0000\u0000\u0000\u01ea\u0007\u0001\u0000\u0000\u0000"+
		"\u01eb\u01ec\u0005\u0081\u0000\u0000\u01ec\u01ef\u0003\n\u0005\u0000\u01ed"+
		"\u01ee\u0005\u0084\u0000\u0000\u01ee\u01f0\u0003\n\u0005\u0000\u01ef\u01ed"+
		"\u0001\u0000\u0000\u0000\u01f0\u01f1\u0001\u0000\u0000\u0000\u01f1\u01ef"+
		"\u0001\u0000\u0000\u0000\u01f1\u01f2\u0001\u0000\u0000\u0000\u01f2\u01f3"+
		"\u0001\u0000\u0000\u0000\u01f3\u01f4\u0005\u0082\u0000\u0000\u01f4\t\u0001"+
		"\u0000\u0000\u0000\u01f5\u01f7\u0003\u0004\u0002\u0000\u01f6\u01f8\u0003"+
		"\u01b4\u00da\u0000\u01f7\u01f6\u0001\u0000\u0000\u0000\u01f7\u01f8\u0001"+
		"\u0000\u0000\u0000\u01f8\u000b\u0001\u0000\u0000\u0000\u01f9\u01fc\u0003"+
		"\u000e\u0007\u0000\u01fa\u01fc\u0005\u0012\u0000\u0000\u01fb\u01f9\u0001"+
		"\u0000\u0000\u0000\u01fb\u01fa\u0001\u0000\u0000\u0000\u01fc\r\u0001\u0000"+
		"\u0000\u0000\u01fd\u0201\u0003\u0010\b\u0000\u01fe\u0201\u0003\u0012\t"+
		"\u0000\u01ff\u0201\u0005\u001d\u0000\u0000\u0200\u01fd\u0001\u0000\u0000"+
		"\u0000\u0200\u01fe\u0001\u0000\u0000\u0000\u0200\u01ff\u0001\u0000\u0000"+
		"\u0000\u0201\u000f\u0001\u0000\u0000\u0000\u0202\u0203\u0007\u0000\u0000"+
		"\u0000\u0203\u0011\u0001\u0000\u0000\u0000\u0204\u0205\u0007\u0001\u0000"+
		"\u0000\u0205\u0013\u0001\u0000\u0000\u0000\u0206\u020b\u0003\u0002\u0001"+
		"\u0000\u0207\u020b\u0005D\u0000\u0000\u0208\u020b\u0005#\u0000\u0000\u0209"+
		"\u020b\u0005[\u0000\u0000\u020a\u0206\u0001\u0000\u0000\u0000\u020a\u0207"+
		"\u0001\u0000\u0000\u0000\u020a\u0208\u0001\u0000\u0000\u0000\u020a\u0209"+
		"\u0001\u0000\u0000\u0000\u020b\u0015\u0001\u0000\u0000\u0000\u020c\u020d"+
		"\u0005\u0092\u0000\u0000\u020d\u0212\u0003\u0004\u0002\u0000\u020e\u020f"+
		"\u0005\u0084\u0000\u0000\u020f\u0211\u0003\u0004\u0002\u0000\u0210\u020e"+
		"\u0001\u0000\u0000\u0000\u0211\u0214\u0001\u0000\u0000\u0000\u0212\u0210"+
		"\u0001\u0000\u0000\u0000\u0212\u0213\u0001\u0000\u0000\u0000\u0213\u0215"+
		"\u0001\u0000\u0000\u0000\u0214\u0212\u0001\u0000\u0000\u0000\u0215\u0216"+
		"\u0005\u0093\u0000\u0000\u0216\u0017\u0001\u0000\u0000\u0000\u0217\u021c"+
		"\u0003\u001a\r\u0000\u0218\u0219\u0005\u0084\u0000\u0000\u0219\u021b\u0003"+
		"\u001a\r\u0000\u021a\u0218\u0001\u0000\u0000\u0000\u021b\u021e\u0001\u0000"+
		"\u0000\u0000\u021c\u021a\u0001\u0000\u0000\u0000\u021c\u021d\u0001\u0000"+
		"\u0000\u0000\u021d\u0019\u0001\u0000\u0000\u0000\u021e\u021c\u0001\u0000"+
		"\u0000\u0000\u021f\u0220\u0003\u01b4\u00da\u0000\u0220\u0221\u0005\u0085"+
		"\u0000\u0000\u0221\u0223\u0001\u0000\u0000\u0000\u0222\u021f\u0001\u0000"+
		"\u0000\u0000\u0222\u0223\u0001\u0000\u0000\u0000\u0223\u0225\u0001\u0000"+
		"\u0000\u0000\u0224\u0226\u0007\u0002\u0000\u0000\u0225\u0224\u0001\u0000"+
		"\u0000\u0000\u0225\u0226\u0001\u0000\u0000\u0000\u0226\u022d\u0001\u0000"+
		"\u0000\u0000\u0227\u022e\u0003\u001c\u000e\u0000\u0228\u022b\u0005j\u0000"+
		"\u0000\u0229\u022b\u0003\u0004\u0002\u0000\u022a\u0228\u0001\u0000\u0000"+
		"\u0000\u022a\u0229\u0001\u0000\u0000\u0000\u022b\u022c\u0001\u0000\u0000"+
		"\u0000\u022c\u022e\u0003\u001c\u000e\u0000\u022d\u0227\u0001\u0000\u0000"+
		"\u0000\u022d\u022a\u0001\u0000\u0000\u0000\u022e\u001b\u0001\u0000\u0000"+
		"\u0000\u022f\u0234\u0003 \u0010\u0000\u0230\u0234\u0003\u001e\u000f\u0000"+
		"\u0231\u0232\u0005P\u0000\u0000\u0232\u0234\u0003\u001e\u000f\u0000\u0233"+
		"\u022f\u0001\u0000\u0000\u0000\u0233\u0230\u0001\u0000\u0000\u0000\u0233"+
		"\u0231\u0001\u0000\u0000\u0000\u0234\u001d\u0001\u0000\u0000\u0000\u0235"+
		"\u0239\u0003x<\u0000\u0236\u0239\u0003\u0084B\u0000\u0237\u0239\u0003"+
		"$\u0012\u0000\u0238\u0235\u0001\u0000\u0000\u0000\u0238\u0236\u0001\u0000"+
		"\u0000\u0000\u0238\u0237\u0001\u0000\u0000\u0000\u0239\u001f\u0001\u0000"+
		"\u0000\u0000\u023a\u023b\u0003D\"\u0000\u023b\u023c\u0003\"\u0011\u0000"+
		"\u023c\u023d\u0003\u001c\u000e\u0000\u023d\u0243\u0001\u0000\u0000\u0000"+
		"\u023e\u023f\u0003D\"\u0000\u023f\u0240\u0005\u00aa\u0000\u0000\u0240"+
		"\u0241\u0003L&\u0000\u0241\u0243\u0001\u0000\u0000\u0000\u0242\u023a\u0001"+
		"\u0000\u0000\u0000\u0242\u023e\u0001\u0000\u0000\u0000\u0243!\u0001\u0000"+
		"\u0000\u0000\u0244\u0250\u0005\u0091\u0000\u0000\u0245\u0250\u0005\u00a0"+
		"\u0000\u0000\u0246\u0250\u0005\u00a1\u0000\u0000\u0247\u0250\u0005\u00a2"+
		"\u0000\u0000\u0248\u0250\u0005\u00a3\u0000\u0000\u0249\u0250\u0005\u00a4"+
		"\u0000\u0000\u024a\u0250\u0005\u00a5\u0000\u0000\u024b\u0250\u0005\u00a6"+
		"\u0000\u0000\u024c\u0250\u0005\u00a7\u0000\u0000\u024d\u0250\u0005\u00a9"+
		"\u0000\u0000\u024e\u0250\u0003\u017c\u00be\u0000\u024f\u0244\u0001\u0000"+
		"\u0000\u0000\u024f\u0245\u0001\u0000\u0000\u0000\u024f\u0246\u0001\u0000"+
		"\u0000\u0000\u024f\u0247\u0001\u0000\u0000\u0000\u024f\u0248\u0001\u0000"+
		"\u0000\u0000\u024f\u0249\u0001\u0000\u0000\u0000\u024f\u024a\u0001\u0000"+
		"\u0000\u0000\u024f\u024b\u0001\u0000\u0000\u0000\u024f\u024c\u0001\u0000"+
		"\u0000\u0000\u024f\u024d\u0001\u0000\u0000\u0000\u024f\u024e\u0001\u0000"+
		"\u0000\u0000\u0250#\u0001\u0000\u0000\u0000\u0251\u0257\u0003&\u0013\u0000"+
		"\u0252\u0253\u0005\u0094\u0000\u0000\u0253\u0254\u0003L&\u0000\u0254\u0255"+
		"\u0005\u0085\u0000\u0000\u0255\u0256\u0003L&\u0000\u0256\u0258\u0001\u0000"+
		"\u0000\u0000\u0257\u0252\u0001\u0000\u0000\u0000\u0257\u0258\u0001\u0000"+
		"\u0000\u0000\u0258%\u0001\u0000\u0000\u0000\u0259\u025f\u0003(\u0014\u0000"+
		"\u025a\u025d\u0005\u0096\u0000\u0000\u025b\u025e\u0003&\u0013\u0000\u025c"+
		"\u025e\u0003N\'\u0000\u025d\u025b\u0001\u0000\u0000\u0000\u025d\u025c"+
		"\u0001\u0000\u0000\u0000\u025e\u0260\u0001\u0000\u0000\u0000\u025f\u025a"+
		"\u0001\u0000\u0000\u0000\u025f\u0260\u0001\u0000\u0000\u0000\u0260\'\u0001"+
		"\u0000\u0000\u0000\u0261\u0266\u0003*\u0015\u0000\u0262\u0263\u0005\u009a"+
		"\u0000\u0000\u0263\u0265\u0003*\u0015\u0000\u0264\u0262\u0001\u0000\u0000"+
		"\u0000\u0265\u0268\u0001\u0000\u0000\u0000\u0266\u0264\u0001\u0000\u0000"+
		"\u0000\u0266\u0267\u0001\u0000\u0000\u0000\u0267)\u0001\u0000\u0000\u0000"+
		"\u0268\u0266\u0001\u0000\u0000\u0000\u0269\u026e\u0003,\u0016\u0000\u026a"+
		"\u026b\u0005\u0099\u0000\u0000\u026b\u026d\u0003,\u0016\u0000\u026c\u026a"+
		"\u0001\u0000\u0000\u0000\u026d\u0270\u0001\u0000\u0000\u0000\u026e\u026c"+
		"\u0001\u0000\u0000\u0000\u026e\u026f\u0001\u0000\u0000\u0000\u026f+\u0001"+
		"\u0000\u0000\u0000\u0270\u026e\u0001\u0000\u0000\u0000\u0271\u0276\u0003"+
		".\u0017\u0000\u0272\u0273\u0005\u008d\u0000\u0000\u0273\u0275\u0003.\u0017"+
		"\u0000\u0274\u0272\u0001\u0000\u0000\u0000\u0275\u0278\u0001\u0000\u0000"+
		"\u0000\u0276\u0274\u0001\u0000\u0000\u0000\u0276\u0277\u0001\u0000\u0000"+
		"\u0000\u0277-\u0001\u0000\u0000\u0000\u0278\u0276\u0001\u0000\u0000\u0000"+
		"\u0279\u027e\u00030\u0018\u0000\u027a\u027b\u0005\u008e\u0000\u0000\u027b"+
		"\u027d\u00030\u0018\u0000\u027c\u027a\u0001\u0000\u0000\u0000\u027d\u0280"+
		"\u0001\u0000\u0000\u0000\u027e\u027c\u0001\u0000\u0000\u0000\u027e\u027f"+
		"\u0001\u0000\u0000\u0000\u027f/\u0001\u0000\u0000\u0000\u0280\u027e\u0001"+
		"\u0000\u0000\u0000\u0281\u0286\u00032\u0019\u0000\u0282\u0283\u0005\u008c"+
		"\u0000\u0000\u0283\u0285\u00032\u0019\u0000\u0284\u0282\u0001\u0000\u0000"+
		"\u0000\u0285\u0288\u0001\u0000\u0000\u0000\u0286\u0284\u0001\u0000\u0000"+
		"\u0000\u0286\u0287\u0001\u0000\u0000\u0000\u02871\u0001\u0000\u0000\u0000"+
		"\u0288\u0286\u0001\u0000\u0000\u0000\u0289\u028e\u00034\u001a\u0000\u028a"+
		"\u028b\u0007\u0003\u0000\u0000\u028b\u028d\u00034\u001a\u0000\u028c\u028a"+
		"\u0001\u0000\u0000\u0000\u028d\u0290\u0001\u0000\u0000\u0000\u028e\u028c"+
		"\u0001\u0000\u0000\u0000\u028e\u028f\u0001\u0000\u0000\u0000\u028f3\u0001"+
		"\u0000\u0000\u0000\u0290\u028e\u0001\u0000\u0000\u0000\u0291\u029a\u0003"+
		"6\u001b\u0000\u0292\u0293\u0007\u0004\u0000\u0000\u0293\u0299\u00036\u001b"+
		"\u0000\u0294\u0295\u0005;\u0000\u0000\u0295\u0299\u0003r9\u0000\u0296"+
		"\u0297\u0005\r\u0000\u0000\u0297\u0299\u0003\u0004\u0002\u0000\u0298\u0292"+
		"\u0001\u0000\u0000\u0000\u0298\u0294\u0001\u0000\u0000\u0000\u0298\u0296"+
		"\u0001\u0000\u0000\u0000\u0299\u029c\u0001\u0000\u0000\u0000\u029a\u0298"+
		"\u0001\u0000\u0000\u0000\u029a\u029b\u0001\u0000\u0000\u0000\u029b5\u0001"+
		"\u0000\u0000\u0000\u029c\u029a\u0001\u0000\u0000\u0000\u029d\u02a5\u0003"+
		"8\u001c\u0000\u029e\u02a1\u0005\u00a8\u0000\u0000\u029f\u02a1\u0003\u017a"+
		"\u00bd\u0000\u02a0\u029e\u0001\u0000\u0000\u0000\u02a0\u029f\u0001\u0000"+
		"\u0000\u0000\u02a1\u02a2\u0001\u0000\u0000\u0000\u02a2\u02a4\u00038\u001c"+
		"\u0000\u02a3\u02a0\u0001\u0000\u0000\u0000\u02a4\u02a7\u0001\u0000\u0000"+
		"\u0000\u02a5\u02a3\u0001\u0000\u0000\u0000\u02a5\u02a6\u0001\u0000\u0000"+
		"\u0000\u02a67\u0001\u0000\u0000\u0000\u02a7\u02a5\u0001\u0000\u0000\u0000"+
		"\u02a8\u02ad\u0003:\u001d\u0000\u02a9\u02aa\u0007\u0005\u0000\u0000\u02aa"+
		"\u02ac\u0003:\u001d\u0000\u02ab\u02a9\u0001\u0000\u0000\u0000\u02ac\u02af"+
		"\u0001\u0000\u0000\u0000\u02ad\u02ab\u0001\u0000\u0000\u0000\u02ad\u02ae"+
		"\u0001\u0000\u0000\u0000\u02ae9\u0001\u0000\u0000\u0000\u02af\u02ad\u0001"+
		"\u0000\u0000\u0000\u02b0\u02b5\u0003<\u001e\u0000\u02b1\u02b2\u0007\u0006"+
		"\u0000\u0000\u02b2\u02b4\u0003<\u001e\u0000\u02b3\u02b1\u0001\u0000\u0000"+
		"\u0000\u02b4\u02b7\u0001\u0000\u0000\u0000\u02b5\u02b3\u0001\u0000\u0000"+
		"\u0000\u02b5\u02b6\u0001\u0000\u0000\u0000\u02b6;\u0001\u0000\u0000\u0000"+
		"\u02b7\u02b5\u0001\u0000\u0000\u0000\u02b8\u02c2\u0003B!\u0000\u02b9\u02ba"+
		"\u0005]\u0000\u0000\u02ba\u02bf\u0005}\u0000\u0000\u02bb\u02bd\u0003>"+
		"\u001f\u0000\u02bc\u02be\u0005\u0084\u0000\u0000\u02bd\u02bc\u0001\u0000"+
		"\u0000\u0000\u02bd\u02be\u0001\u0000\u0000\u0000\u02be\u02c0\u0001\u0000"+
		"\u0000\u0000\u02bf\u02bb\u0001\u0000\u0000\u0000\u02bf\u02c0\u0001\u0000"+
		"\u0000\u0000\u02c0\u02c1\u0001\u0000\u0000\u0000\u02c1\u02c3\u0005~\u0000"+
		"\u0000\u02c2\u02b9\u0001\u0000\u0000\u0000\u02c2\u02c3\u0001\u0000\u0000"+
		"\u0000\u02c3=\u0001\u0000\u0000\u0000\u02c4\u02c9\u0003@ \u0000\u02c5"+
		"\u02c6\u0005\u0084\u0000\u0000\u02c6\u02c8\u0003@ \u0000\u02c7\u02c5\u0001"+
		"\u0000\u0000\u0000\u02c8\u02cb\u0001\u0000\u0000\u0000\u02c9\u02c7\u0001"+
		"\u0000\u0000\u0000\u02c9\u02ca\u0001\u0000\u0000\u0000\u02ca?\u0001\u0000"+
		"\u0000\u0000\u02cb\u02c9\u0001\u0000\u0000\u0000\u02cc\u02ce\u0003\u001c"+
		"\u000e\u0000\u02cd\u02cf\u0003\u00be_\u0000\u02ce\u02cd\u0001\u0000\u0000"+
		"\u0000\u02ce\u02cf\u0001\u0000\u0000\u0000\u02cf\u02d0\u0001\u0000\u0000"+
		"\u0000\u02d0\u02d1\u0003\u0178\u00bc\u0000\u02d1\u02d2\u0003L&\u0000\u02d2"+
		"A\u0001\u0000\u0000\u0000\u02d3\u02dc\u0003D\"\u0000\u02d4\u02d6\u0003"+
		"D\"\u0000\u02d5\u02d4\u0001\u0000\u0000\u0000\u02d5\u02d6\u0001\u0000"+
		"\u0000\u0000\u02d6\u02d7\u0001\u0000\u0000\u0000\u02d7\u02d9\u0005\u00ab"+
		"\u0000\u0000\u02d8\u02da\u0003D\"\u0000\u02d9\u02d8\u0001\u0000\u0000"+
		"\u0000\u02d9\u02da\u0001\u0000\u0000\u0000\u02da\u02dc\u0001\u0000\u0000"+
		"\u0000\u02db\u02d3\u0001\u0000\u0000\u0000\u02db\u02d5\u0001\u0000\u0000"+
		"\u0000\u02dcC\u0001\u0000\u0000\u0000\u02dd\u02f4\u0003F#\u0000\u02de"+
		"\u02f4\u0003H$\u0000\u02df\u02e0\u0005\u0087\u0000\u0000\u02e0\u02f4\u0003"+
		"D\"\u0000\u02e1\u02e2\u0005\u0088\u0000\u0000\u02e2\u02f4\u0003D\"\u0000"+
		"\u02e3\u02e4\u0005\u008f\u0000\u0000\u02e4\u02f4\u0003D\"\u0000\u02e5"+
		"\u02e6\u0005\u0090\u0000\u0000\u02e6\u02f4\u0003D\"\u0000\u02e7\u02e8"+
		"\u0005\u0097\u0000\u0000\u02e8\u02f4\u0003D\"\u0000\u02e9\u02ea\u0005"+
		"\u0098\u0000\u0000\u02ea\u02f4\u0003D\"\u0000\u02eb\u02ec\u0005\u0010"+
		"\u0000\u0000\u02ec\u02f4\u0003D\"\u0000\u02ed\u02ee\u0005\u008c\u0000"+
		"\u0000\u02ee\u02f4\u0003D\"\u0000\u02ef\u02f0\u0005\u0089\u0000\u0000"+
		"\u02f0\u02f4\u0003D\"\u0000\u02f1\u02f2\u0005\u008e\u0000\u0000\u02f2"+
		"\u02f4\u0003D\"\u0000\u02f3\u02dd\u0001\u0000\u0000\u0000\u02f3\u02de"+
		"\u0001\u0000\u0000\u0000\u02f3\u02df\u0001\u0000\u0000\u0000\u02f3\u02e1"+
		"\u0001\u0000\u0000\u0000\u02f3\u02e3\u0001\u0000\u0000\u0000\u02f3\u02e5"+
		"\u0001\u0000\u0000\u0000\u02f3\u02e7\u0001\u0000\u0000\u0000\u02f3\u02e9"+
		"\u0001\u0000\u0000\u0000\u02f3\u02eb\u0001\u0000\u0000\u0000\u02f3\u02ed"+
		"\u0001\u0000\u0000\u0000\u02f3\u02ef\u0001\u0000\u0000\u0000\u02f3\u02f1"+
		"\u0001\u0000\u0000\u0000\u02f4E\u0001\u0000\u0000\u0000\u02f5\u02f6\u0005"+
		"\u0081\u0000\u0000\u02f6\u02f7\u0003\u0004\u0002\u0000\u02f7\u02f8\u0005"+
		"\u0082\u0000\u0000\u02f8\u02f9\u0003D\"\u0000\u02f9G\u0001\u0000\u0000"+
		"\u0000\u02fa\u02fc\u0003J%\u0000\u02fb\u02fd\u0005\u008f\u0000\u0000\u02fc"+
		"\u02fb\u0001\u0000\u0000\u0000\u02fc\u02fd\u0001\u0000\u0000\u0000\u02fd"+
		"\u0301\u0001\u0000\u0000\u0000\u02fe\u0300\u0003R)\u0000\u02ff\u02fe\u0001"+
		"\u0000\u0000\u0000\u0300\u0303\u0001\u0000\u0000\u0000\u0301\u02ff\u0001"+
		"\u0000\u0000\u0000\u0301\u0302\u0001\u0000\u0000\u0000\u0302\u0305\u0001"+
		"\u0000\u0000\u0000\u0303\u0301\u0001\u0000\u0000\u0000\u0304\u0306\u0005"+
		"\u008f\u0000\u0000\u0305\u0304\u0001\u0000\u0000\u0000\u0305\u0306\u0001"+
		"\u0000\u0000\u0000\u0306\u031d\u0001\u0000\u0000\u0000\u0307\u030e\u0003"+
		"P(\u0000\u0308\u030e\u0003\u01b0\u00d8\u0000\u0309\u030e\u0005\u0097\u0000"+
		"\u0000\u030a\u030e\u0005\u0098\u0000\u0000\u030b\u030c\u0005\u009b\u0000"+
		"\u0000\u030c\u030e\u0003\u01b4\u00da\u0000\u030d\u0307\u0001\u0000\u0000"+
		"\u0000\u030d\u0308\u0001\u0000\u0000\u0000\u030d\u0309\u0001\u0000\u0000"+
		"\u0000\u030d\u030a\u0001\u0000\u0000\u0000\u030d\u030b\u0001\u0000\u0000"+
		"\u0000\u030e\u0310\u0001\u0000\u0000\u0000\u030f\u0311\u0005\u008f\u0000"+
		"\u0000\u0310\u030f\u0001\u0000\u0000\u0000\u0310\u0311\u0001\u0000\u0000"+
		"\u0000\u0311\u0315\u0001\u0000\u0000\u0000\u0312\u0314\u0003R)\u0000\u0313"+
		"\u0312\u0001\u0000\u0000\u0000\u0314\u0317\u0001\u0000\u0000\u0000\u0315"+
		"\u0313\u0001\u0000\u0000\u0000\u0315\u0316\u0001\u0000\u0000\u0000\u0316"+
		"\u0319\u0001\u0000\u0000\u0000\u0317\u0315\u0001\u0000\u0000\u0000\u0318"+
		"\u031a\u0005\u008f\u0000\u0000\u0319\u0318\u0001\u0000\u0000\u0000\u0319"+
		"\u031a\u0001\u0000\u0000\u0000\u031a\u031c\u0001\u0000\u0000\u0000\u031b"+
		"\u030d\u0001\u0000\u0000\u0000\u031c\u031f\u0001\u0000\u0000\u0000\u031d"+
		"\u031b\u0001\u0000\u0000\u0000\u031d\u031e\u0001\u0000\u0000\u0000\u031e"+
		"I\u0001\u0000\u0000\u0000\u031f\u031d\u0001\u0000\u0000\u0000\u0320\u039a"+
		"\u0003\u017e\u00bf\u0000\u0321\u0323\u0003\u01b4\u00da\u0000\u0322\u0324"+
		"\u0003\u0016\u000b\u0000\u0323\u0322\u0001\u0000\u0000\u0000\u0323\u0324"+
		"\u0001\u0000\u0000\u0000\u0324\u039a\u0001\u0000\u0000\u0000\u0325\u0326"+
		"\u0005\u0081\u0000\u0000\u0326\u0327\u0003\u001c\u000e\u0000\u0327\u0328"+
		"\u0005\u0082\u0000\u0000\u0328\u039a\u0001\u0000\u0000\u0000\u0329\u039a"+
		"\u0003V+\u0000\u032a\u039a\u0003\u00e6s\u0000\u032b\u039a\u0005s\u0000"+
		"\u0000\u032c\u039a\u0005^\u0000\u0000\u032d\u0337\u0005\u0011\u0000\u0000"+
		"\u032e\u032f\u0005\u0083\u0000\u0000\u032f\u0331\u0003\u01b4\u00da\u0000"+
		"\u0330\u0332\u0003\u0016\u000b\u0000\u0331\u0330\u0001\u0000\u0000\u0000"+
		"\u0331\u0332\u0001\u0000\u0000\u0000\u0332\u0338\u0001\u0000\u0000\u0000"+
		"\u0333\u0334\u0005\u007f\u0000\u0000\u0334\u0335\u0003X,\u0000\u0335\u0336"+
		"\u0005\u0080\u0000\u0000\u0336\u0338\u0001\u0000\u0000\u0000\u0337\u032e"+
		"\u0001\u0000\u0000\u0000\u0337\u0333\u0001\u0000\u0000\u0000\u0338\u039a"+
		"\u0001\u0000\u0000\u0000\u0339\u0356\u0005B\u0000\u0000\u033a\u0350\u0003"+
		"\u0004\u0002\u0000\u033b\u0351\u0003\u01b2\u00d9\u0000\u033c\u0351\u0003"+
		"Z-\u0000\u033d\u033e\u0005\u007f\u0000\u0000\u033e\u033f\u0003X,\u0000"+
		"\u033f\u0343\u0005\u0080\u0000\u0000\u0340\u0342\u0003\u0144\u00a2\u0000"+
		"\u0341\u0340\u0001\u0000\u0000\u0000\u0342\u0345\u0001\u0000\u0000\u0000"+
		"\u0343\u0341\u0001\u0000\u0000\u0000\u0343\u0344\u0001\u0000\u0000\u0000"+
		"\u0344\u0347\u0001\u0000\u0000\u0000\u0345\u0343\u0001\u0000\u0000\u0000"+
		"\u0346\u0348\u0003\u0146\u00a3\u0000\u0347\u0346\u0001\u0000\u0000\u0000"+
		"\u0347\u0348\u0001\u0000\u0000\u0000\u0348\u0351\u0001\u0000\u0000\u0000"+
		"\u0349\u034b\u0003\u0144\u00a2\u0000\u034a\u0349\u0001\u0000\u0000\u0000"+
		"\u034b\u034c\u0001\u0000\u0000\u0000\u034c\u034a\u0001\u0000\u0000\u0000"+
		"\u034c\u034d\u0001\u0000\u0000\u0000\u034d\u034e\u0001\u0000\u0000\u0000"+
		"\u034e\u034f\u0003\u0146\u00a3\u0000\u034f\u0351\u0001\u0000\u0000\u0000"+
		"\u0350\u033b\u0001\u0000\u0000\u0000\u0350\u033c\u0001\u0000\u0000\u0000"+
		"\u0350\u033d\u0001\u0000\u0000\u0000\u0350\u034a\u0001\u0000\u0000\u0000"+
		"\u0351\u0357\u0001\u0000\u0000\u0000\u0352\u0357\u0003h4\u0000\u0353\u0354"+
		"\u0003\u0144\u00a2\u0000\u0354\u0355\u0003\u0146\u00a3\u0000\u0355\u0357"+
		"\u0001\u0000\u0000\u0000\u0356\u033a\u0001\u0000\u0000\u0000\u0356\u0352"+
		"\u0001\u0000\u0000\u0000\u0356\u0353\u0001\u0000\u0000\u0000\u0357\u039a"+
		"\u0001\u0000\u0000\u0000\u0358\u0359\u0005\u0081\u0000\u0000\u0359\u035c"+
		"\u0003\u001a\r\u0000\u035a\u035b\u0005\u0084\u0000\u0000\u035b\u035d\u0003"+
		"\u001a\r\u0000\u035c\u035a\u0001\u0000\u0000\u0000\u035d\u035e\u0001\u0000"+
		"\u0000\u0000\u035e\u035c\u0001\u0000\u0000\u0000\u035e\u035f\u0001\u0000"+
		"\u0000\u0000\u035f\u0360\u0001\u0000\u0000\u0000\u0360\u0361\u0005\u0082"+
		"\u0000\u0000\u0361\u039a\u0001\u0000\u0000\u0000\u0362\u0363\u0005b\u0000"+
		"\u0000\u0363\u0367\u0005\u0081\u0000\u0000\u0364\u0368\u0003n7\u0000\u0365"+
		"\u0368\u0003\u0004\u0002\u0000\u0366\u0368\u0005l\u0000\u0000\u0367\u0364"+
		"\u0001\u0000\u0000\u0000\u0367\u0365\u0001\u0000\u0000\u0000\u0367\u0366"+
		"\u0001\u0000\u0000\u0000\u0368\u0369\u0001\u0000\u0000\u0000\u0369\u039a"+
		"\u0005\u0082\u0000\u0000\u036a\u036b\u0005\u0019\u0000\u0000\u036b\u036c"+
		"\u0005\u0081\u0000\u0000\u036c\u036d\u0003\u001c\u000e\u0000\u036d\u036e"+
		"\u0005\u0082\u0000\u0000\u036e\u039a\u0001\u0000\u0000\u0000\u036f\u0370"+
		"\u0005e\u0000\u0000\u0370\u0371\u0005\u0081\u0000\u0000\u0371\u0372\u0003"+
		"\u001c\u000e\u0000\u0372\u0373\u0005\u0082\u0000\u0000\u0373\u039a\u0001"+
		"\u0000\u0000\u0000\u0374\u0379\u0005\u001e\u0000\u0000\u0375\u0376\u0005"+
		"\u0081\u0000\u0000\u0376\u0377\u0003\u0004\u0002\u0000\u0377\u0378\u0005"+
		"\u0082\u0000\u0000\u0378\u037a\u0001\u0000\u0000\u0000\u0379\u0375\u0001"+
		"\u0000\u0000\u0000\u0379\u037a\u0001\u0000\u0000\u0000\u037a\u039a\u0001"+
		"\u0000\u0000\u0000\u037b\u037d\u0005\u000f\u0000\u0000\u037c\u037b\u0001"+
		"\u0000\u0000\u0000\u037c\u037d\u0001\u0000\u0000\u0000\u037d\u037e\u0001"+
		"\u0000\u0000\u0000\u037e\u0384\u0005\u001f\u0000\u0000\u037f\u0381\u0005"+
		"\u0081\u0000\u0000\u0380\u0382\u0003|>\u0000\u0381\u0380\u0001\u0000\u0000"+
		"\u0000\u0381\u0382\u0001\u0000\u0000\u0000\u0382\u0383\u0001\u0000\u0000"+
		"\u0000\u0383\u0385\u0005\u0082\u0000\u0000\u0384\u037f\u0001\u0000\u0000"+
		"\u0000\u0384\u0385\u0001\u0000\u0000\u0000\u0385\u0386\u0001\u0000\u0000"+
		"\u0000\u0386\u039a\u0003\u00acV\u0000\u0387\u0388\u0005X\u0000\u0000\u0388"+
		"\u0389\u0005\u0081\u0000\u0000\u0389\u038a\u0003\u0004\u0002\u0000\u038a"+
		"\u038b\u0005\u0082\u0000\u0000\u038b\u039a\u0001\u0000\u0000\u0000\u038c"+
		"\u038d\u0005@\u0000\u0000\u038d\u0393\u0005\u0081\u0000\u0000\u038e\u038f"+
		"\u0003\u01b4\u00da\u0000\u038f\u0390\u0005\u0083\u0000\u0000\u0390\u0392"+
		"\u0001\u0000\u0000\u0000\u0391\u038e\u0001\u0000\u0000\u0000\u0392\u0395"+
		"\u0001\u0000\u0000\u0000\u0393\u0391\u0001\u0000\u0000\u0000\u0393\u0394"+
		"\u0001\u0000\u0000\u0000\u0394\u0396\u0001\u0000\u0000\u0000\u0395\u0393"+
		"\u0001\u0000\u0000\u0000\u0396\u0397\u0003\u01b4\u00da\u0000\u0397\u0398"+
		"\u0005\u0082\u0000\u0000\u0398\u039a\u0001\u0000\u0000\u0000\u0399\u0320"+
		"\u0001\u0000\u0000\u0000\u0399\u0321\u0001\u0000\u0000\u0000\u0399\u0325"+
		"\u0001\u0000\u0000\u0000\u0399\u0329\u0001\u0000\u0000\u0000\u0399\u032a"+
		"\u0001\u0000\u0000\u0000\u0399\u032b\u0001\u0000\u0000\u0000\u0399\u032c"+
		"\u0001\u0000\u0000\u0000\u0399\u032d\u0001\u0000\u0000\u0000\u0399\u0339"+
		"\u0001\u0000\u0000\u0000\u0399\u0358\u0001\u0000\u0000\u0000\u0399\u0362"+
		"\u0001\u0000\u0000\u0000\u0399\u036a\u0001\u0000\u0000\u0000\u0399\u036f"+
		"\u0001\u0000\u0000\u0000\u0399\u0374\u0001\u0000\u0000\u0000\u0399\u037c"+
		"\u0001\u0000\u0000\u0000\u0399\u0387\u0001\u0000\u0000\u0000\u0399\u038c"+
		"\u0001\u0000\u0000\u0000\u039aK\u0001\u0000\u0000\u0000\u039b\u039e\u0003"+
		"\u001c\u000e\u0000\u039c\u039e\u0003N\'\u0000\u039d\u039b\u0001\u0000"+
		"\u0000\u0000\u039d\u039c\u0001\u0000\u0000\u0000\u039eM\u0001\u0000\u0000"+
		"\u0000\u039f\u03a0\u0005_\u0000\u0000\u03a0\u03a1\u0003\u001c\u000e\u0000"+
		"\u03a1O\u0001\u0000\u0000\u0000\u03a2\u03a4\u0005\u0094\u0000\u0000\u03a3"+
		"\u03a2\u0001\u0000\u0000\u0000\u03a3\u03a4\u0001\u0000\u0000\u0000\u03a4"+
		"\u03a5\u0001\u0000\u0000\u0000\u03a5\u03a6\u0005\u0083\u0000\u0000\u03a6"+
		"\u03a8\u0003\u01b4\u00da\u0000\u03a7\u03a9\u0003\u0016\u000b\u0000\u03a8"+
		"\u03a7\u0001\u0000\u0000\u0000\u03a8\u03a9\u0001\u0000\u0000\u0000\u03a9"+
		"Q\u0001\u0000\u0000\u0000\u03aa\u03ac\u0005\u0094\u0000\u0000\u03ab\u03aa"+
		"\u0001\u0000\u0000\u0000\u03ab\u03ac\u0001\u0000\u0000\u0000\u03ac\u03ad"+
		"\u0001\u0000\u0000\u0000\u03ad\u03ae\u0005\u007f\u0000\u0000\u03ae\u03b3"+
		"\u0003T*\u0000\u03af\u03b0\u0005\u0084\u0000\u0000\u03b0\u03b2\u0003T"+
		"*\u0000\u03b1\u03af\u0001\u0000\u0000\u0000\u03b2\u03b5\u0001\u0000\u0000"+
		"\u0000\u03b3\u03b1\u0001\u0000\u0000\u0000\u03b3\u03b4\u0001\u0000\u0000"+
		"\u0000\u03b4\u03b6\u0001\u0000\u0000\u0000\u03b5\u03b3\u0001\u0000\u0000"+
		"\u0000\u03b6\u03b7\u0005\u0080\u0000\u0000\u03b7S\u0001\u0000\u0000\u0000"+
		"\u03b8\u03b9\u0003\u01b4\u00da\u0000\u03b9\u03ba\u0005\u0085\u0000\u0000"+
		"\u03ba\u03bc\u0001\u0000\u0000\u0000\u03bb\u03b8\u0001\u0000\u0000\u0000"+
		"\u03bb\u03bc\u0001\u0000\u0000\u0000\u03bc\u03bd\u0001\u0000\u0000\u0000"+
		"\u03bd\u03be\u0003\u001c\u000e\u0000\u03beU\u0001\u0000\u0000\u0000\u03bf"+
		"\u03c0\u0007\u0007\u0000\u0000\u03c0W\u0001\u0000\u0000\u0000\u03c1\u03c6"+
		"\u0003\u001c\u000e\u0000\u03c2\u03c3\u0005\u0084\u0000\u0000\u03c3\u03c5"+
		"\u0003\u001c\u000e\u0000\u03c4\u03c2\u0001\u0000\u0000\u0000\u03c5\u03c8"+
		"\u0001\u0000\u0000\u0000\u03c6\u03c4\u0001\u0000\u0000\u0000\u03c6\u03c7"+
		"\u0001\u0000\u0000\u0000\u03c7Y\u0001\u0000\u0000\u0000\u03c8\u03c6\u0001"+
		"\u0000\u0000\u0000\u03c9\u03cc\u0003\\.\u0000\u03ca\u03cc\u0003d2\u0000"+
		"\u03cb\u03c9\u0001\u0000\u0000\u0000\u03cb\u03ca\u0001\u0000\u0000\u0000"+
		"\u03cc[\u0001\u0000\u0000\u0000\u03cd\u03d2\u0005}\u0000\u0000\u03ce\u03d0"+
		"\u0003^/\u0000\u03cf\u03d1\u0005\u0084\u0000\u0000\u03d0\u03cf\u0001\u0000"+
		"\u0000\u0000\u03d0\u03d1\u0001\u0000\u0000\u0000\u03d1\u03d3\u0001\u0000"+
		"\u0000\u0000\u03d2\u03ce\u0001\u0000\u0000\u0000\u03d2\u03d3\u0001\u0000"+
		"\u0000\u0000\u03d3\u03d4\u0001\u0000\u0000\u0000\u03d4\u03d5\u0005~\u0000"+
		"\u0000\u03d5]\u0001\u0000\u0000\u0000\u03d6\u03db\u0003`0\u0000\u03d7"+
		"\u03d8\u0005\u0084\u0000\u0000\u03d8\u03da\u0003`0\u0000\u03d9\u03d7\u0001"+
		"\u0000\u0000\u0000\u03da\u03dd\u0001\u0000\u0000\u0000\u03db\u03d9\u0001"+
		"\u0000\u0000\u0000\u03db\u03dc\u0001\u0000\u0000\u0000\u03dc_\u0001\u0000"+
		"\u0000\u0000\u03dd\u03db\u0001\u0000\u0000\u0000\u03de\u03e4\u0003\u01b4"+
		"\u00da\u0000\u03df\u03e0\u0005\u007f\u0000\u0000\u03e0\u03e1\u0003\u001c"+
		"\u000e\u0000\u03e1\u03e2\u0005\u0080\u0000\u0000\u03e2\u03e4\u0001\u0000"+
		"\u0000\u0000\u03e3\u03de\u0001\u0000\u0000\u0000\u03e3\u03df\u0001\u0000"+
		"\u0000\u0000\u03e4\u03e5\u0001\u0000\u0000\u0000\u03e5\u03e6\u0005\u0091"+
		"\u0000\u0000\u03e6\u03e7\u0003b1\u0000\u03e7a\u0001\u0000\u0000\u0000"+
		"\u03e8\u03eb\u0003\u001c\u000e\u0000\u03e9\u03eb\u0003Z-\u0000\u03ea\u03e8"+
		"\u0001\u0000\u0000\u0000\u03ea\u03e9\u0001\u0000\u0000\u0000\u03ebc\u0001"+
		"\u0000\u0000\u0000\u03ec\u03ed\u0005}\u0000\u0000\u03ed\u03f2\u0003f3"+
		"\u0000\u03ee\u03ef\u0005\u0084\u0000\u0000\u03ef\u03f1\u0003f3\u0000\u03f0"+
		"\u03ee\u0001\u0000\u0000\u0000\u03f1\u03f4\u0001\u0000\u0000\u0000\u03f2"+
		"\u03f0\u0001\u0000\u0000\u0000\u03f2\u03f3\u0001\u0000\u0000\u0000\u03f3"+
		"\u03f6\u0001\u0000\u0000\u0000\u03f4\u03f2\u0001\u0000\u0000\u0000\u03f5"+
		"\u03f7\u0005\u0084\u0000\u0000\u03f6\u03f5\u0001\u0000\u0000\u0000\u03f6"+
		"\u03f7\u0001\u0000\u0000\u0000\u03f7\u03f8\u0001\u0000\u0000\u0000\u03f8"+
		"\u03f9\u0005~\u0000\u0000\u03f9e\u0001\u0000\u0000\u0000\u03fa\u0400\u0003"+
		"\u001e\u000f\u0000\u03fb\u03fc\u0005}\u0000\u0000\u03fc\u03fd\u0003X,"+
		"\u0000\u03fd\u03fe\u0005~\u0000\u0000\u03fe\u0400\u0001\u0000\u0000\u0000"+
		"\u03ff\u03fa\u0001\u0000\u0000\u0000\u03ff\u03fb\u0001\u0000\u0000\u0000"+
		"\u0400g\u0001\u0000\u0000\u0000\u0401\u0406\u0005}\u0000\u0000\u0402\u0404"+
		"\u0003j5\u0000\u0403\u0405\u0005\u0084\u0000\u0000\u0404\u0403\u0001\u0000"+
		"\u0000\u0000\u0404\u0405\u0001\u0000\u0000\u0000\u0405\u0407\u0001\u0000"+
		"\u0000\u0000\u0406\u0402\u0001\u0000\u0000\u0000\u0406\u0407\u0001\u0000"+
		"\u0000\u0000\u0407\u0408\u0001\u0000\u0000\u0000\u0408\u0409\u0005~\u0000"+
		"\u0000\u0409i\u0001\u0000\u0000\u0000\u040a\u040f\u0003l6\u0000\u040b"+
		"\u040c\u0005\u0084\u0000\u0000\u040c\u040e\u0003l6\u0000\u040d\u040b\u0001"+
		"\u0000\u0000\u0000\u040e\u0411\u0001\u0000\u0000\u0000\u040f\u040d\u0001"+
		"\u0000\u0000\u0000\u040f\u0410\u0001\u0000\u0000\u0000\u0410k\u0001\u0000"+
		"\u0000\u0000\u0411\u040f\u0001\u0000\u0000\u0000\u0412\u0418\u0003H$\u0000"+
		"\u0413\u0414\u0003\u01b4\u00da\u0000\u0414\u0415\u0005\u0091\u0000\u0000"+
		"\u0415\u0416\u0003\u001c\u000e\u0000\u0416\u0418\u0001\u0000\u0000\u0000"+
		"\u0417\u0412\u0001\u0000\u0000\u0000\u0417\u0413\u0001\u0000\u0000\u0000"+
		"\u0418m\u0001\u0000\u0000\u0000\u0419\u0422\u0003\u01b4\u00da\u0000\u041a"+
		"\u041c\u0003p8\u0000\u041b\u041a\u0001\u0000\u0000\u0000\u041b\u041c\u0001"+
		"\u0000\u0000\u0000\u041c\u0423\u0001\u0000\u0000\u0000\u041d\u041e\u0005"+
		"\u0095\u0000\u0000\u041e\u0420\u0003\u01b4\u00da\u0000\u041f\u0421\u0003"+
		"p8\u0000\u0420\u041f\u0001\u0000\u0000\u0000\u0420\u0421\u0001\u0000\u0000"+
		"\u0000\u0421\u0423\u0001\u0000\u0000\u0000\u0422\u041b\u0001\u0000\u0000"+
		"\u0000\u0422\u041d\u0001\u0000\u0000\u0000\u0423\u042b\u0001\u0000\u0000"+
		"\u0000\u0424\u0425\u0005\u0083\u0000\u0000\u0425\u0427\u0003\u01b4\u00da"+
		"\u0000\u0426\u0428\u0003p8\u0000\u0427\u0426\u0001\u0000\u0000\u0000\u0427"+
		"\u0428\u0001\u0000\u0000\u0000\u0428\u042a\u0001\u0000\u0000\u0000\u0429"+
		"\u0424\u0001\u0000\u0000\u0000\u042a\u042d\u0001\u0000\u0000\u0000\u042b"+
		"\u0429\u0001\u0000\u0000\u0000\u042b\u042c\u0001\u0000\u0000\u0000\u042c"+
		"o\u0001\u0000\u0000\u0000\u042d\u042b\u0001\u0000\u0000\u0000\u042e\u0432"+
		"\u0005\u0092\u0000\u0000\u042f\u0431\u0005\u0084\u0000\u0000\u0430\u042f"+
		"\u0001\u0000\u0000\u0000\u0431\u0434\u0001\u0000\u0000\u0000\u0432\u0430"+
		"\u0001\u0000\u0000\u0000\u0432\u0433\u0001\u0000\u0000\u0000\u0433\u0435"+
		"\u0001\u0000\u0000\u0000\u0434\u0432\u0001\u0000\u0000\u0000\u0435\u0436"+
		"\u0005\u0093\u0000\u0000\u0436q\u0001\u0000\u0000\u0000\u0437\u043c\u0003"+
		"\u0006\u0003\u0000\u0438\u043b\u0003\u0144\u00a2\u0000\u0439\u043b\u0005"+
		"\u0089\u0000\u0000\u043a\u0438\u0001\u0000\u0000\u0000\u043a\u0439\u0001"+
		"\u0000\u0000\u0000\u043b\u043e\u0001\u0000\u0000\u0000\u043c\u043a\u0001"+
		"\u0000\u0000\u0000\u043c\u043d\u0001\u0000\u0000\u0000\u043d\u0440\u0001"+
		"\u0000\u0000\u0000\u043e\u043c\u0001\u0000\u0000\u0000\u043f\u0441\u0005"+
		"\u0094\u0000\u0000\u0440\u043f\u0001\u0000\u0000\u0000\u0440\u0441\u0001"+
		"\u0000\u0000\u0000\u0441\u0443\u0001\u0000\u0000\u0000\u0442\u0444\u0003"+
		"t:\u0000\u0443\u0442\u0001\u0000\u0000\u0000\u0443\u0444\u0001\u0000\u0000"+
		"\u0000\u0444\u0446\u0001\u0000\u0000\u0000\u0445\u0447\u0003\u01b4\u00da"+
		"\u0000\u0446\u0445\u0001\u0000\u0000\u0000\u0446\u0447\u0001\u0000\u0000"+
		"\u0000\u0447s\u0001\u0000\u0000\u0000\u0448\u0449\u0005}\u0000\u0000\u0449"+
		"\u044e\u0003v;\u0000\u044a\u044b\u0005\u0084\u0000\u0000\u044b\u044d\u0003"+
		"v;\u0000\u044c\u044a\u0001\u0000\u0000\u0000\u044d\u0450\u0001\u0000\u0000"+
		"\u0000\u044e\u044c\u0001\u0000\u0000\u0000\u044e\u044f\u0001\u0000\u0000"+
		"\u0000\u044f\u0451\u0001\u0000\u0000\u0000\u0450\u044e\u0001\u0000\u0000"+
		"\u0000\u0451\u0452\u0005~\u0000\u0000\u0452u\u0001\u0000\u0000\u0000\u0453"+
		"\u0454\u0003\u01b4\u00da\u0000\u0454\u0455\u0005\u0085\u0000\u0000\u0455"+
		"\u0456\u0003\u001c\u000e\u0000\u0456w\u0001\u0000\u0000\u0000\u0457\u0459"+
		"\u0005\u000f\u0000\u0000\u0458\u0457\u0001\u0000\u0000\u0000\u0458\u0459"+
		"\u0001\u0000\u0000\u0000\u0459\u045a\u0001\u0000\u0000\u0000\u045a\u045b"+
		"\u0003z=\u0000\u045b\u045c\u0003\u0178\u00bc\u0000\u045c\u045d\u0003\u0082"+
		"A\u0000\u045dy\u0001\u0000\u0000\u0000\u045e\u045f\u0005\u0081\u0000\u0000"+
		"\u045f\u046a\u0005\u0082\u0000\u0000\u0460\u0461\u0005\u0081\u0000\u0000"+
		"\u0461\u0462\u0003|>\u0000\u0462\u0463\u0005\u0082\u0000\u0000\u0463\u046a"+
		"\u0001\u0000\u0000\u0000\u0464\u0465\u0005\u0081\u0000\u0000\u0465\u0466"+
		"\u0003\u0080@\u0000\u0466\u0467\u0005\u0082\u0000\u0000\u0467\u046a\u0001"+
		"\u0000\u0000\u0000\u0468\u046a\u0003\u01b4\u00da\u0000\u0469\u045e\u0001"+
		"\u0000\u0000\u0000\u0469\u0460\u0001\u0000\u0000\u0000\u0469\u0464\u0001"+
		"\u0000\u0000\u0000\u0469\u0468\u0001\u0000\u0000\u0000\u046a{\u0001\u0000"+
		"\u0000\u0000\u046b\u0470\u0003~?\u0000\u046c\u046d\u0005\u0084\u0000\u0000"+
		"\u046d\u046f\u0003~?\u0000\u046e\u046c\u0001\u0000\u0000\u0000\u046f\u0472"+
		"\u0001\u0000\u0000\u0000\u0470\u046e\u0001\u0000\u0000\u0000\u0470\u0471"+
		"\u0001\u0000\u0000\u0000\u0471}\u0001\u0000\u0000\u0000\u0472\u0470\u0001"+
		"\u0000\u0000\u0000\u0473\u0475\u0007\u0002\u0000\u0000\u0474\u0473\u0001"+
		"\u0000\u0000\u0000\u0474\u0475\u0001\u0000\u0000\u0000\u0475\u0476\u0001"+
		"\u0000\u0000\u0000\u0476\u0477\u0003\u0004\u0002\u0000\u0477\u0478\u0003"+
		"\u01b4\u00da\u0000\u0478\u007f\u0001\u0000\u0000\u0000\u0479\u047e\u0003"+
		"\u01b4\u00da\u0000\u047a\u047b\u0005\u0084\u0000\u0000\u047b\u047d\u0003"+
		"\u01b4\u00da\u0000\u047c\u047a\u0001\u0000\u0000\u0000\u047d\u0480\u0001"+
		"\u0000\u0000\u0000\u047e\u047c\u0001\u0000\u0000\u0000\u047e\u047f\u0001"+
		"\u0000\u0000\u0000\u047f\u0081\u0001\u0000\u0000\u0000\u0480\u047e\u0001"+
		"\u0000\u0000\u0000\u0481\u0484\u0003L&\u0000\u0482\u0484\u0003\u00acV"+
		"\u0000\u0483\u0481\u0001\u0000\u0000\u0000\u0483\u0482\u0001\u0000\u0000"+
		"\u0000\u0484\u0083\u0001\u0000\u0000\u0000\u0485\u0486\u0003\u0086C\u0000"+
		"\u0486\u0487\u0003\u0088D\u0000\u0487\u0085\u0001\u0000\u0000\u0000\u0488"+
		"\u048a\u00050\u0000\u0000\u0489\u048b\u0003\u0004\u0002\u0000\u048a\u0489"+
		"\u0001\u0000\u0000\u0000\u048a\u048b\u0001\u0000\u0000\u0000\u048b\u048c"+
		"\u0001\u0000\u0000\u0000\u048c\u048d\u0003\u01b4\u00da\u0000\u048d\u048e"+
		"\u00056\u0000\u0000\u048e\u048f\u0003\u001c\u000e\u0000\u048f\u0087\u0001"+
		"\u0000\u0000\u0000\u0490\u0492\u0003\u008aE\u0000\u0491\u0490\u0001\u0000"+
		"\u0000\u0000\u0492\u0495\u0001\u0000\u0000\u0000\u0493\u0491\u0001\u0000"+
		"\u0000\u0000\u0493\u0494\u0001\u0000\u0000\u0000\u0494\u0496\u0001\u0000"+
		"\u0000\u0000\u0495\u0493\u0001\u0000\u0000\u0000\u0496\u0498\u0003\u0096"+
		"K\u0000\u0497\u0499\u0003\u0098L\u0000\u0498\u0497\u0001\u0000\u0000\u0000"+
		"\u0498\u0499\u0001\u0000\u0000\u0000\u0499\u0089\u0001\u0000\u0000\u0000"+
		"\u049a\u04a0\u0003\u0086C\u0000\u049b\u04a0\u0003\u008cF\u0000\u049c\u04a0"+
		"\u0003\u008eG\u0000\u049d\u04a0\u0003\u0090H\u0000\u049e\u04a0\u0003\u0092"+
		"I\u0000\u049f\u049a\u0001\u0000\u0000\u0000\u049f\u049b\u0001\u0000\u0000"+
		"\u0000\u049f\u049c\u0001\u0000\u0000\u0000\u049f\u049d\u0001\u0000\u0000"+
		"\u0000\u049f\u049e\u0001\u0000\u0000\u0000\u04a0\u008b\u0001\u0000\u0000"+
		"\u0000\u04a1\u04a2\u0005=\u0000\u0000\u04a2\u04a3\u0003\u01b4\u00da\u0000"+
		"\u04a3\u04a4\u0005\u0091\u0000\u0000\u04a4\u04a5\u0003\u001c\u000e\u0000"+
		"\u04a5\u008d\u0001\u0000\u0000\u0000\u04a6\u04a7\u0005o\u0000\u0000\u04a7"+
		"\u04a8\u0003\u001c\u000e\u0000\u04a8\u008f\u0001\u0000\u0000\u0000\u04a9"+
		"\u04ab\u0005<\u0000\u0000\u04aa\u04ac\u0003\u0004\u0002\u0000\u04ab\u04aa"+
		"\u0001\u0000\u0000\u0000\u04ab\u04ac\u0001\u0000\u0000\u0000\u04ac\u04ad"+
		"\u0001\u0000\u0000\u0000\u04ad\u04ae\u0003\u01b4\u00da\u0000\u04ae\u04af"+
		"\u00056\u0000\u0000\u04af\u04b0\u0003\u001c\u000e\u0000\u04b0\u04b1\u0005"+
		"E\u0000\u0000\u04b1\u04b2\u0003\u001c\u000e\u0000\u04b2\u04b3\u0005&\u0000"+
		"\u0000\u04b3\u04b6\u0003\u001c\u000e\u0000\u04b4\u04b5\u0005:\u0000\u0000"+
		"\u04b5\u04b7\u0003\u01b4\u00da\u0000\u04b6\u04b4\u0001\u0000\u0000\u0000"+
		"\u04b6\u04b7\u0001\u0000\u0000\u0000\u04b7\u0091\u0001\u0000\u0000\u0000"+
		"\u04b8\u04b9\u0005G\u0000\u0000\u04b9\u04be\u0003\u0094J\u0000\u04ba\u04bb"+
		"\u0005\u0084\u0000\u0000\u04bb\u04bd\u0003\u0094J\u0000\u04bc\u04ba\u0001"+
		"\u0000\u0000\u0000\u04bd\u04c0\u0001\u0000\u0000\u0000\u04be\u04bc\u0001"+
		"\u0000\u0000\u0000\u04be\u04bf\u0001\u0000\u0000\u0000\u04bf\u0093\u0001"+
		"\u0000\u0000\u0000\u04c0\u04be\u0001\u0000\u0000\u0000\u04c1\u04c3\u0003"+
		"\u001c\u000e\u0000\u04c2\u04c4\u0007\b\u0000\u0000\u04c3\u04c2\u0001\u0000"+
		"\u0000\u0000\u04c3\u04c4\u0001\u0000\u0000\u0000\u04c4\u0095\u0001\u0000"+
		"\u0000\u0000\u04c5\u04c6\u0005U\u0000\u0000\u04c6\u04cd\u0003\u001c\u000e"+
		"\u0000\u04c7\u04c8\u00053\u0000\u0000\u04c8\u04c9\u0003\u001c\u000e\u0000"+
		"\u04c9\u04ca\u0005\u0014\u0000\u0000\u04ca\u04cb\u0003\u001c\u000e\u0000"+
		"\u04cb\u04cd\u0001\u0000\u0000\u0000\u04cc\u04c5\u0001\u0000\u0000\u0000"+
		"\u04cc\u04c7\u0001\u0000\u0000\u0000\u04cd\u0097\u0001\u0000\u0000\u0000"+
		"\u04ce\u04cf\u0005:\u0000\u0000\u04cf\u04d0\u0003\u01b4\u00da\u0000\u04d0"+
		"\u04d1\u0003\u0088D\u0000\u04d1\u0099\u0001\u0000\u0000\u0000\u04d2\u04d6"+
		"\u0003\u00a6S\u0000\u04d3\u04d6\u0003\u009cN\u0000\u04d4\u04d6\u0003\u00a8"+
		"T\u0000\u04d5\u04d2\u0001\u0000\u0000\u0000\u04d5\u04d3\u0001\u0000\u0000"+
		"\u0000\u04d5\u04d4\u0001\u0000\u0000\u0000\u04d6\u009b\u0001\u0000\u0000"+
		"\u0000\u04d7\u04d8\u0003\u00aeW\u0000\u04d8\u04d9\u0005\u0086\u0000\u0000"+
		"\u04d9\u04df\u0001\u0000\u0000\u0000\u04da\u04db\u0003\u00b6[\u0000\u04db"+
		"\u04dc\u0005\u0086\u0000\u0000\u04dc\u04df\u0001\u0000\u0000\u0000\u04dd"+
		"\u04df\u0003\u009eO\u0000\u04de\u04d7\u0001\u0000\u0000\u0000\u04de\u04da"+
		"\u0001\u0000\u0000\u0000\u04de\u04dd\u0001\u0000\u0000\u0000\u04df\u009d"+
		"\u0001\u0000\u0000\u0000\u04e0\u04e1\u0003\u00a0P\u0000\u04e1\u04e2\u0003"+
		"\u00a4R\u0000\u04e2\u009f\u0001\u0000\u0000\u0000\u04e3\u04e5\u0003\u00a2"+
		"Q\u0000\u04e4\u04e3\u0001\u0000\u0000\u0000\u04e4\u04e5\u0001\u0000\u0000"+
		"\u0000\u04e5\u04e6\u0001\u0000\u0000\u0000\u04e6\u04e7\u0003\u0114\u008a"+
		"\u0000\u04e7\u04e9\u0003\u01b4\u00da\u0000\u04e8\u04ea\u0003\u00e8t\u0000"+
		"\u04e9\u04e8\u0001\u0000\u0000\u0000\u04e9\u04ea\u0001\u0000\u0000\u0000"+
		"\u04ea\u04eb\u0001\u0000\u0000\u0000\u04eb\u04ed\u0005\u0081\u0000\u0000"+
		"\u04ec\u04ee\u0003\u011a\u008d\u0000\u04ed\u04ec\u0001\u0000\u0000\u0000"+
		"\u04ed\u04ee\u0001\u0000\u0000\u0000\u04ee\u04ef\u0001\u0000\u0000\u0000"+
		"\u04ef\u04f1\u0005\u0082\u0000\u0000\u04f0\u04f2\u0003\u00f0x\u0000\u04f1"+
		"\u04f0\u0001\u0000\u0000\u0000\u04f1\u04f2\u0001\u0000\u0000\u0000\u04f2"+
		"\u00a1\u0001\u0000\u0000\u0000\u04f3\u04f5\u0007\t\u0000\u0000\u04f4\u04f6"+
		"\u0005Z\u0000\u0000\u04f5\u04f4\u0001\u0000\u0000\u0000\u04f5\u04f6\u0001"+
		"\u0000\u0000\u0000\u04f6\u04fa\u0001\u0000\u0000\u0000\u04f7\u04f8\u0005"+
		"Z\u0000\u0000\u04f8\u04fa\u0007\t\u0000\u0000\u04f9\u04f3\u0001\u0000"+
		"\u0000\u0000\u04f9\u04f7\u0001\u0000\u0000\u0000\u04fa\u00a3\u0001\u0000"+
		"\u0000\u0000\u04fb\u0501\u0003\u00acV\u0000\u04fc\u04fd\u0003\u0178\u00bc"+
		"\u0000\u04fd\u04fe\u0003L&\u0000\u04fe\u04ff\u0005\u0086\u0000\u0000\u04ff"+
		"\u0501\u0001\u0000\u0000\u0000\u0500\u04fb\u0001\u0000\u0000\u0000\u0500"+
		"\u04fc\u0001\u0000\u0000\u0000\u0501\u00a5\u0001\u0000\u0000\u0000\u0502"+
		"\u0503\u0003\u01b4\u00da\u0000\u0503\u0504\u0005\u0085\u0000\u0000\u0504"+
		"\u0505\u0003\u009aM\u0000\u0505\u00a7\u0001\u0000\u0000\u0000\u0506\u0509"+
		"\u0003\u00acV\u0000\u0507\u0509\u0003\u00aaU\u0000\u0508\u0506\u0001\u0000"+
		"\u0000\u0000\u0508\u0507\u0001\u0000\u0000\u0000\u0509\u00a9\u0001\u0000"+
		"\u0000\u0000\u050a\u058d\u0005\u0086\u0000\u0000\u050b\u050c\u0003\u001c"+
		"\u000e\u0000\u050c\u050d\u0005\u0086\u0000\u0000\u050d\u058d\u0001\u0000"+
		"\u0000\u0000\u050e\u050f\u00054\u0000\u0000\u050f\u0510\u0005\u0081\u0000"+
		"\u0000\u0510\u0511\u0003\u001c\u000e\u0000\u0511\u0512\u0005\u0082\u0000"+
		"\u0000\u0512\u0515\u0003\u00b8\\\u0000\u0513\u0514\u0005$\u0000\u0000"+
		"\u0514\u0516\u0003\u00b8\\\u0000\u0515\u0513\u0001\u0000\u0000\u0000\u0515"+
		"\u0516\u0001\u0000\u0000\u0000\u0516\u058d\u0001\u0000\u0000\u0000\u0517"+
		"\u0518\u0005]\u0000\u0000\u0518\u0519\u0005\u0081\u0000\u0000\u0519\u051a"+
		"\u0003\u001c\u000e\u0000\u051a\u051b\u0005\u0082\u0000\u0000\u051b\u051f"+
		"\u0005}\u0000\u0000\u051c\u051e\u0003\u00ba]\u0000\u051d\u051c\u0001\u0000"+
		"\u0000\u0000\u051e\u0521\u0001\u0000\u0000\u0000\u051f\u051d\u0001\u0000"+
		"\u0000\u0000\u051f\u0520\u0001\u0000\u0000\u0000\u0520\u0522\u0001\u0000"+
		"\u0000\u0000\u0521\u051f\u0001\u0000\u0000\u0000\u0522\u0523\u0005~\u0000"+
		"\u0000\u0523\u058d\u0001\u0000\u0000\u0000\u0524\u0525\u0005p\u0000\u0000"+
		"\u0525\u0526\u0005\u0081\u0000\u0000\u0526\u0527\u0003\u001c\u000e\u0000"+
		"\u0527\u0528\u0005\u0082\u0000\u0000\u0528\u0529\u0003\u00a8T\u0000\u0529"+
		"\u058d\u0001\u0000\u0000\u0000\u052a\u052b\u0005!\u0000\u0000\u052b\u052c"+
		"\u0003\u00a8T\u0000\u052c\u052d\u0005p\u0000\u0000\u052d\u052e\u0005\u0081"+
		"\u0000\u0000\u052e\u052f\u0003\u001c\u000e\u0000\u052f\u0530\u0005\u0082"+
		"\u0000\u0000\u0530\u0531\u0005\u0086\u0000\u0000\u0531\u058d\u0001\u0000"+
		"\u0000\u0000\u0532\u0533\u0005.\u0000\u0000\u0533\u0535\u0005\u0081\u0000"+
		"\u0000\u0534\u0536\u0003\u00c2a\u0000\u0535\u0534\u0001\u0000\u0000\u0000"+
		"\u0535\u0536\u0001\u0000\u0000\u0000\u0536\u0537\u0001\u0000\u0000\u0000"+
		"\u0537\u0539\u0005\u0086\u0000\u0000\u0538\u053a\u0003\u001c\u000e\u0000"+
		"\u0539\u0538\u0001\u0000\u0000\u0000\u0539\u053a\u0001\u0000\u0000\u0000"+
		"\u053a\u053b\u0001\u0000\u0000\u0000\u053b\u053d\u0005\u0086\u0000\u0000"+
		"\u053c\u053e\u0003\u00c4b\u0000\u053d\u053c\u0001\u0000\u0000\u0000\u053d"+
		"\u053e\u0001\u0000\u0000\u0000\u053e\u053f\u0001\u0000\u0000\u0000\u053f"+
		"\u0540\u0005\u0082\u0000\u0000\u0540\u058d\u0003\u00a8T\u0000\u0541\u0543"+
		"\u0005\u0010\u0000\u0000\u0542\u0541\u0001\u0000\u0000\u0000\u0542\u0543"+
		"\u0001\u0000\u0000\u0000\u0543\u0544\u0001\u0000\u0000\u0000\u0544\u0545"+
		"\u0005/\u0000\u0000\u0545\u0546\u0005\u0081\u0000\u0000\u0546\u0547\u0003"+
		"\u00b0X\u0000\u0547\u0548\u0003\u01b4\u00da\u0000\u0548\u0549\u00056\u0000"+
		"\u0000\u0549\u054a\u0003\u001c\u000e\u0000\u054a\u054b\u0005\u0082\u0000"+
		"\u0000\u054b\u054c\u0003\u00a8T\u0000\u054c\u058d\u0001\u0000\u0000\u0000"+
		"\u054d\u054e\u0005\u0013\u0000\u0000\u054e\u058d\u0005\u0086\u0000\u0000"+
		"\u054f\u0550\u0005\u001c\u0000\u0000\u0550\u058d\u0005\u0086\u0000\u0000"+
		"\u0551\u0556\u00052\u0000\u0000\u0552\u0557\u0003\u01b4\u00da\u0000\u0553"+
		"\u0554\u0005\u0016\u0000\u0000\u0554\u0557\u0003\u001c\u000e\u0000\u0555"+
		"\u0557\u0005\u001e\u0000\u0000\u0556\u0552\u0001\u0000\u0000\u0000\u0556"+
		"\u0553\u0001\u0000\u0000\u0000\u0556\u0555\u0001\u0000\u0000\u0000\u0557"+
		"\u0558\u0001\u0000\u0000\u0000\u0558\u058d\u0005\u0086\u0000\u0000\u0559"+
		"\u055b\u0005R\u0000\u0000\u055a\u055c\u0003\u001c\u000e\u0000\u055b\u055a"+
		"\u0001\u0000\u0000\u0000\u055b\u055c\u0001\u0000\u0000\u0000\u055c\u055d"+
		"\u0001\u0000\u0000\u0000\u055d\u058d\u0005\u0086\u0000\u0000\u055e\u0560"+
		"\u0005_\u0000\u0000\u055f\u0561\u0003\u001c\u000e\u0000\u0560\u055f\u0001"+
		"\u0000\u0000\u0000\u0560\u0561\u0001\u0000\u0000\u0000\u0561\u0562\u0001"+
		"\u0000\u0000\u0000\u0562\u058d\u0005\u0086\u0000\u0000\u0563\u0564\u0005"+
		"a\u0000\u0000\u0564\u056a\u0003\u00acV\u0000\u0565\u0567\u0003\u00c6c"+
		"\u0000\u0566\u0568\u0003\u00ceg\u0000\u0567\u0566\u0001\u0000\u0000\u0000"+
		"\u0567\u0568\u0001\u0000\u0000\u0000\u0568\u056b\u0001\u0000\u0000\u0000"+
		"\u0569\u056b\u0003\u00ceg\u0000\u056a\u0565\u0001\u0000\u0000\u0000\u056a"+
		"\u0569\u0001\u0000\u0000\u0000\u056b\u058d\u0001\u0000\u0000\u0000\u056c"+
		"\u056d\u0005\u0019\u0000\u0000\u056d\u058d\u0003\u00acV\u0000\u056e\u056f"+
		"\u0005e\u0000\u0000\u056f\u058d\u0003\u00acV\u0000\u0570\u0571\u0005>"+
		"\u0000\u0000\u0571\u0572\u0005\u0081\u0000\u0000\u0572\u0573\u0003\u001c"+
		"\u000e\u0000\u0573\u0574\u0005\u0082\u0000\u0000\u0574\u0575\u0003\u00a8"+
		"T\u0000\u0575\u058d\u0001\u0000\u0000\u0000\u0576\u0577\u0005i\u0000\u0000"+
		"\u0577\u0578\u0005\u0081\u0000\u0000\u0578\u0579\u0003\u00d0h\u0000\u0579"+
		"\u057a\u0005\u0082\u0000\u0000\u057a\u057b\u0003\u00a8T\u0000\u057b\u058d"+
		"\u0001\u0000\u0000\u0000\u057c\u0580\u0005q\u0000\u0000\u057d\u057e\u0005"+
		"R\u0000\u0000\u057e\u0581\u0003\u001c\u000e\u0000\u057f\u0581\u0005\u0013"+
		"\u0000\u0000\u0580\u057d\u0001\u0000\u0000\u0000\u0580\u057f\u0001\u0000"+
		"\u0000\u0000\u0581\u0582\u0001\u0000\u0000\u0000\u0582\u058d\u0005\u0086"+
		"\u0000\u0000\u0583\u0584\u0005g\u0000\u0000\u0584\u058d\u0003\u00acV\u0000"+
		"\u0585\u0586\u0005,\u0000\u0000\u0586\u0587\u0005\u0081\u0000\u0000\u0587"+
		"\u0588\u0003\u016c\u00b6\u0000\u0588\u0589\u0003\u016e\u00b7\u0000\u0589"+
		"\u058a\u0005\u0082\u0000\u0000\u058a\u058b\u0003\u00a8T\u0000\u058b\u058d"+
		"\u0001\u0000\u0000\u0000\u058c\u050a\u0001\u0000\u0000\u0000\u058c\u050b"+
		"\u0001\u0000\u0000\u0000\u058c\u050e\u0001\u0000\u0000\u0000\u058c\u0517"+
		"\u0001\u0000\u0000\u0000\u058c\u0524\u0001\u0000\u0000\u0000\u058c\u052a"+
		"\u0001\u0000\u0000\u0000\u058c\u0532\u0001\u0000\u0000\u0000\u058c\u0542"+
		"\u0001\u0000\u0000\u0000\u058c\u054d\u0001\u0000\u0000\u0000\u058c\u054f"+
		"\u0001\u0000\u0000\u0000\u058c\u0551\u0001\u0000\u0000\u0000\u058c\u0559"+
		"\u0001\u0000\u0000\u0000\u058c\u055e\u0001\u0000\u0000\u0000\u058c\u0563"+
		"\u0001\u0000\u0000\u0000\u058c\u056c\u0001\u0000\u0000\u0000\u058c\u056e"+
		"\u0001\u0000\u0000\u0000\u058c\u0570\u0001\u0000\u0000\u0000\u058c\u0576"+
		"\u0001\u0000\u0000\u0000\u058c\u057c\u0001\u0000\u0000\u0000\u058c\u0583"+
		"\u0001\u0000\u0000\u0000\u058c\u0585\u0001\u0000\u0000\u0000\u058d\u00ab"+
		"\u0001\u0000\u0000\u0000\u058e\u0590\u0005}\u0000\u0000\u058f\u0591\u0003"+
		"\u00c0`\u0000\u0590\u058f\u0001\u0000\u0000\u0000\u0590\u0591\u0001\u0000"+
		"\u0000\u0000\u0591\u0592\u0001\u0000\u0000\u0000\u0592\u0593\u0005~\u0000"+
		"\u0000\u0593\u00ad\u0001\u0000\u0000\u0000\u0594\u0599\u0005i\u0000\u0000"+
		"\u0595\u0599\u0005P\u0000\u0000\u0596\u0597\u0005P\u0000\u0000\u0597\u0599"+
		"\u0005O\u0000\u0000\u0598\u0594\u0001\u0000\u0000\u0000\u0598\u0595\u0001"+
		"\u0000\u0000\u0000\u0598\u0596\u0001\u0000\u0000\u0000\u0598\u0599\u0001"+
		"\u0000\u0000\u0000\u0599\u059a\u0001\u0000\u0000\u0000\u059a\u059b\u0003"+
		"\u00b0X\u0000\u059b\u05a2\u0003\u00b2Y\u0000\u059c\u059d\u0005\u0084\u0000"+
		"\u0000\u059d\u059e\u0003\u00b2Y\u0000\u059e\u059f\u0004W\u0000\u0000\u059f"+
		"\u05a1\u0001\u0000\u0000\u0000\u05a0\u059c\u0001\u0000\u0000\u0000\u05a1"+
		"\u05a4\u0001\u0000\u0000\u0000\u05a2\u05a0\u0001\u0000\u0000\u0000\u05a2"+
		"\u05a3\u0001\u0000\u0000\u0000\u05a3\u05aa\u0001\u0000\u0000\u0000\u05a4"+
		"\u05a2\u0001\u0000\u0000\u0000\u05a5\u05a6\u0005,\u0000\u0000\u05a6\u05a7"+
		"\u0003\u016c\u00b6\u0000\u05a7\u05a8\u0003\u016e\u00b7\u0000\u05a8\u05aa"+
		"\u0001\u0000\u0000\u0000\u05a9\u0598\u0001\u0000\u0000\u0000\u05a9\u05a5"+
		"\u0001\u0000\u0000\u0000\u05aa\u00af\u0001\u0000\u0000\u0000\u05ab\u05ae"+
		"\u0005j\u0000\u0000\u05ac\u05ae\u0003\u0004\u0002\u0000\u05ad\u05ab\u0001"+
		"\u0000\u0000\u0000\u05ad\u05ac\u0001\u0000\u0000\u0000\u05ae\u00b1\u0001"+
		"\u0000\u0000\u0000\u05af\u05b5\u0003\u01b4\u00da\u0000\u05b0\u05b2\u0005"+
		"\u0091\u0000\u0000\u05b1\u05b3\u0005P\u0000\u0000\u05b2\u05b1\u0001\u0000"+
		"\u0000\u0000\u05b2\u05b3\u0001\u0000\u0000\u0000\u05b3\u05b4\u0001\u0000"+
		"\u0000\u0000\u05b4\u05b6\u0003\u00b4Z\u0000\u05b5\u05b0\u0001\u0000\u0000"+
		"\u0000\u05b5\u05b6\u0001\u0000\u0000\u0000\u05b6\u00b3\u0001\u0000\u0000"+
		"\u0000\u05b7\u05bb\u0003\u001c\u000e\u0000\u05b8\u05bb\u0003\u0146\u00a3"+
		"\u0000\u05b9\u05bb\u0003\u0176\u00bb\u0000\u05ba\u05b7\u0001\u0000\u0000"+
		"\u0000\u05ba\u05b8\u0001\u0000\u0000\u0000\u05ba\u05b9\u0001\u0000\u0000"+
		"\u0000\u05bb\u00b5\u0001\u0000\u0000\u0000\u05bc\u05bd\u0005\u001b\u0000"+
		"\u0000\u05bd\u05be\u0003\u0004\u0002\u0000\u05be\u05bf\u0003\u010a\u0085"+
		"\u0000\u05bf\u00b7\u0001\u0000\u0000\u0000\u05c0\u05c3\u0003\u00acV\u0000"+
		"\u05c1\u05c3\u0003\u00aaU\u0000\u05c2\u05c0\u0001\u0000\u0000\u0000\u05c2"+
		"\u05c1\u0001\u0000\u0000\u0000\u05c3\u00b9\u0001\u0000\u0000\u0000\u05c4"+
		"\u05c6\u0003\u00bc^\u0000\u05c5\u05c4\u0001\u0000\u0000\u0000\u05c6\u05c7"+
		"\u0001\u0000\u0000\u0000\u05c7\u05c5\u0001\u0000\u0000\u0000\u05c7\u05c8"+
		"\u0001\u0000\u0000\u0000\u05c8\u05c9\u0001\u0000\u0000\u0000\u05c9\u05ca"+
		"\u0003\u00c0`\u0000\u05ca\u00bb\u0001\u0000\u0000\u0000\u05cb\u05cc\u0005"+
		"\u0016\u0000\u0000\u05cc\u05ce\u0003\u001c\u000e\u0000\u05cd\u05cf\u0003"+
		"\u00be_\u0000\u05ce\u05cd\u0001\u0000\u0000\u0000\u05ce\u05cf\u0001\u0000"+
		"\u0000\u0000\u05cf\u05d0\u0001\u0000\u0000\u0000\u05d0\u05d1\u0005\u0085"+
		"\u0000\u0000\u05d1\u05d5\u0001\u0000\u0000\u0000\u05d2\u05d3\u0005\u001e"+
		"\u0000\u0000\u05d3\u05d5\u0005\u0085\u0000\u0000\u05d4\u05cb\u0001\u0000"+
		"\u0000\u0000\u05d4\u05d2\u0001\u0000\u0000\u0000\u05d5\u00bd\u0001\u0000"+
		"\u0000\u0000\u05d6\u05d7\u0005n\u0000\u0000\u05d7\u05d8\u0003\u001c\u000e"+
		"\u0000\u05d8\u00bf\u0001\u0000\u0000\u0000\u05d9\u05db\u0003\u009aM\u0000"+
		"\u05da\u05d9\u0001\u0000\u0000\u0000\u05db\u05dc\u0001\u0000\u0000\u0000"+
		"\u05dc\u05da\u0001\u0000\u0000\u0000\u05dc\u05dd\u0001\u0000\u0000\u0000"+
		"\u05dd\u00c1\u0001\u0000\u0000\u0000\u05de\u05e8\u0003\u00aeW\u0000\u05df"+
		"\u05e4\u0003\u001c\u000e\u0000\u05e0\u05e1\u0005\u0084\u0000\u0000\u05e1"+
		"\u05e3\u0003\u001c\u000e\u0000\u05e2\u05e0\u0001\u0000\u0000\u0000\u05e3"+
		"\u05e6\u0001\u0000\u0000\u0000\u05e4\u05e2\u0001\u0000\u0000\u0000\u05e4"+
		"\u05e5\u0001\u0000\u0000\u0000\u05e5\u05e8\u0001\u0000\u0000\u0000\u05e6"+
		"\u05e4\u0001\u0000\u0000\u0000\u05e7\u05de\u0001\u0000\u0000\u0000\u05e7"+
		"\u05df\u0001\u0000\u0000\u0000\u05e8\u00c3\u0001\u0000\u0000\u0000\u05e9"+
		"\u05ee\u0003\u001c\u000e\u0000\u05ea\u05eb\u0005\u0084\u0000\u0000\u05eb"+
		"\u05ed\u0003\u001c\u000e\u0000\u05ec\u05ea\u0001\u0000\u0000\u0000\u05ed"+
		"\u05f0\u0001\u0000\u0000\u0000\u05ee\u05ec\u0001\u0000\u0000\u0000\u05ee"+
		"\u05ef\u0001\u0000\u0000\u0000\u05ef\u00c5\u0001\u0000\u0000\u0000\u05f0"+
		"\u05ee\u0001\u0000\u0000\u0000\u05f1\u05f5\u0003\u00c8d\u0000\u05f2\u05f4"+
		"\u0003\u00c8d\u0000\u05f3\u05f2\u0001\u0000\u0000\u0000\u05f4\u05f7\u0001"+
		"\u0000\u0000\u0000\u05f5\u05f3\u0001\u0000\u0000\u0000\u05f5\u05f6\u0001"+
		"\u0000\u0000\u0000\u05f6\u05f9\u0001\u0000\u0000\u0000\u05f7\u05f5\u0001"+
		"\u0000\u0000\u0000\u05f8\u05fa\u0003\u00cae\u0000\u05f9\u05f8\u0001\u0000"+
		"\u0000\u0000\u05f9\u05fa\u0001\u0000\u0000\u0000\u05fa\u05fd\u0001\u0000"+
		"\u0000\u0000\u05fb\u05fd\u0003\u00cae\u0000\u05fc\u05f1\u0001\u0000\u0000"+
		"\u0000\u05fc\u05fb\u0001\u0000\u0000\u0000\u05fd\u00c7\u0001\u0000\u0000"+
		"\u0000\u05fe\u05ff\u0005\u0017\u0000\u0000\u05ff\u0600\u0005\u0081\u0000"+
		"\u0000\u0600\u0602\u0003\u0014\n\u0000\u0601\u0603\u0003\u01b4\u00da\u0000"+
		"\u0602\u0601\u0001\u0000\u0000\u0000\u0602\u0603\u0001\u0000\u0000\u0000"+
		"\u0603\u0604\u0001\u0000\u0000\u0000\u0604\u0606\u0005\u0082\u0000\u0000"+
		"\u0605\u0607\u0003\u00ccf\u0000\u0606\u0605\u0001\u0000\u0000\u0000\u0606"+
		"\u0607\u0001\u0000\u0000\u0000\u0607\u0608\u0001\u0000\u0000\u0000\u0608"+
		"\u0609\u0003\u00acV\u0000\u0609\u00c9\u0001\u0000\u0000\u0000\u060a\u060c"+
		"\u0005\u0017\u0000\u0000\u060b\u060d\u0003\u00ccf\u0000\u060c\u060b\u0001"+
		"\u0000\u0000\u0000\u060c\u060d\u0001\u0000\u0000\u0000\u060d\u060e\u0001"+
		"\u0000\u0000\u0000\u060e\u060f\u0003\u00acV\u0000\u060f\u00cb\u0001\u0000"+
		"\u0000\u0000\u0610\u0611\u0005n\u0000\u0000\u0611\u0612\u0005\u0081\u0000"+
		"\u0000\u0612\u0613\u0003\u001c\u000e\u0000\u0613\u0614\u0005\u0082\u0000"+
		"\u0000\u0614\u00cd\u0001\u0000\u0000\u0000\u0615\u0616\u0005+\u0000\u0000"+
		"\u0616\u0617\u0003\u00acV\u0000\u0617\u00cf\u0001\u0000\u0000\u0000\u0618"+
		"\u061b\u0003\u00aeW\u0000\u0619\u061b\u0003\u001c\u000e\u0000\u061a\u0618"+
		"\u0001\u0000\u0000\u0000\u061a\u0619\u0001\u0000\u0000\u0000\u061b\u00d1"+
		"\u0001\u0000\u0000\u0000\u061c\u061d\u0005A\u0000\u0000\u061d\u061e\u0003"+
		"\u00d4j\u0000\u061e\u0620\u0003\u00d6k\u0000\u061f\u0621\u0005\u0086\u0000"+
		"\u0000\u0620\u061f\u0001\u0000\u0000\u0000\u0620\u0621\u0001\u0000\u0000"+
		"\u0000\u0621\u00d3\u0001\u0000\u0000\u0000\u0622\u0627\u0003\u01b4\u00da"+
		"\u0000\u0623\u0624\u0005\u0083\u0000\u0000\u0624\u0626\u0003\u01b4\u00da"+
		"\u0000\u0625\u0623\u0001\u0000\u0000\u0000\u0626\u0629\u0001\u0000\u0000"+
		"\u0000\u0627\u0625\u0001\u0000\u0000\u0000\u0627\u0628\u0001\u0000\u0000"+
		"\u0000\u0628\u00d5\u0001\u0000\u0000\u0000\u0629\u0627\u0001\u0000\u0000"+
		"\u0000\u062a\u062c\u0005}\u0000\u0000\u062b\u062d\u0003\u00d8l\u0000\u062c"+
		"\u062b\u0001\u0000\u0000\u0000\u062c\u062d\u0001\u0000\u0000\u0000\u062d"+
		"\u062f\u0001\u0000\u0000\u0000\u062e\u0630\u0003\u00dcn\u0000\u062f\u062e"+
		"\u0001\u0000\u0000\u0000\u062f\u0630\u0001\u0000\u0000\u0000\u0630\u0632"+
		"\u0001\u0000\u0000\u0000\u0631\u0633\u0003\u00e0p\u0000\u0632\u0631\u0001"+
		"\u0000\u0000\u0000\u0632\u0633\u0001\u0000\u0000\u0000\u0633\u0634\u0001"+
		"\u0000\u0000\u0000\u0634\u0635\u0005~\u0000\u0000\u0635\u00d7\u0001\u0000"+
		"\u0000\u0000\u0636\u0638\u0003\u00dam\u0000\u0637\u0636\u0001\u0000\u0000"+
		"\u0000\u0638\u0639\u0001\u0000\u0000\u0000\u0639\u0637\u0001\u0000\u0000"+
		"\u0000\u0639\u063a\u0001\u0000\u0000\u0000\u063a\u00d9\u0001\u0000\u0000"+
		"\u0000\u063b\u063c\u0005)\u0000\u0000\u063c\u063d\u0005\u000b\u0000\u0000"+
		"\u063d\u063e\u0003\u01b4\u00da\u0000\u063e\u063f\u0005\u0086\u0000\u0000"+
		"\u063f\u00db\u0001\u0000\u0000\u0000\u0640\u0642\u0003\u00deo\u0000\u0641"+
		"\u0640\u0001\u0000\u0000\u0000\u0642\u0643\u0001\u0000\u0000\u0000\u0643"+
		"\u0641\u0001\u0000\u0000\u0000\u0643\u0644\u0001\u0000\u0000\u0000\u0644"+
		"\u00dd\u0001\u0000\u0000\u0000\u0645\u0646\u0005i\u0000\u0000\u0646\u0647"+
		"\u0003\u01b4\u00da\u0000\u0647\u0648\u0005\u0091\u0000\u0000\u0648\u0649"+
		"\u0003\u0002\u0001\u0000\u0649\u064a\u0005\u0086\u0000\u0000\u064a\u0655"+
		"\u0001\u0000\u0000\u0000\u064b\u064c\u0005i\u0000\u0000\u064c\u064d\u0003"+
		"\u0002\u0001\u0000\u064d\u064e\u0005\u0086\u0000\u0000\u064e\u0655\u0001"+
		"\u0000\u0000\u0000\u064f\u0650\u0005i\u0000\u0000\u0650\u0651\u0005Z\u0000"+
		"\u0000\u0651\u0652\u0003\u0002\u0001\u0000\u0652\u0653\u0005\u0086\u0000"+
		"\u0000\u0653\u0655\u0001\u0000\u0000\u0000\u0654\u0645\u0001\u0000\u0000"+
		"\u0000\u0654\u064b\u0001\u0000\u0000\u0000\u0654\u064f\u0001\u0000\u0000"+
		"\u0000\u0655\u00df\u0001\u0000\u0000\u0000\u0656\u0658\u0003\u00e2q\u0000"+
		"\u0657\u0656\u0001\u0000\u0000\u0000\u0658\u0659\u0001\u0000\u0000\u0000"+
		"\u0659\u0657\u0001\u0000\u0000\u0000\u0659\u065a\u0001\u0000\u0000\u0000"+
		"\u065a\u00e1\u0001\u0000\u0000\u0000\u065b\u065e\u0003\u00d2i\u0000\u065c"+
		"\u065e\u0003\u00e4r\u0000\u065d\u065b\u0001\u0000\u0000\u0000\u065d\u065c"+
		"\u0001\u0000\u0000\u0000\u065e\u00e3\u0001\u0000\u0000\u0000\u065f\u0661"+
		"\u0003\u0160\u00b0\u0000\u0660\u065f\u0001\u0000\u0000\u0000\u0660\u0661"+
		"\u0001\u0000\u0000\u0000\u0661\u0663\u0001\u0000\u0000\u0000\u0662\u0664"+
		"\u0003\u0102\u0081\u0000\u0663\u0662\u0001\u0000\u0000\u0000\u0663\u0664"+
		"\u0001\u0000\u0000\u0000\u0664\u066a\u0001\u0000\u0000\u0000\u0665\u066b"+
		"\u0003\u0190\u00c8\u0000\u0666\u066b\u0003\u0192\u00c9\u0000\u0667\u066b"+
		"\u0003\u0194\u00ca\u0000\u0668\u066b\u0003\u0196\u00cb\u0000\u0669\u066b"+
		"\u0003\u0198\u00cc\u0000\u066a\u0665\u0001\u0000\u0000\u0000\u066a\u0666"+
		"\u0001\u0000\u0000\u0000\u066a\u0667\u0001\u0000\u0000\u0000\u066a\u0668"+
		"\u0001\u0000\u0000\u0000\u066a\u0669\u0001\u0000\u0000\u0000\u066b\u00e5"+
		"\u0001\u0000\u0000\u0000\u066c\u066d\u0003\u01b4\u00da\u0000\u066d\u066e"+
		"\u0005\u0095\u0000\u0000\u066e\u0670\u0003\u01b4\u00da\u0000\u066f\u0671"+
		"\u0003\u0016\u000b\u0000\u0670\u066f\u0001\u0000\u0000\u0000\u0670\u0671"+
		"\u0001\u0000\u0000\u0000\u0671\u00e7\u0001\u0000\u0000\u0000\u0672\u0673"+
		"\u0005\u0092\u0000\u0000\u0673\u0678\u0003\u00eau\u0000\u0674\u0675\u0005"+
		"\u0084\u0000\u0000\u0675\u0677\u0003\u00eau\u0000\u0676\u0674\u0001\u0000"+
		"\u0000\u0000\u0677\u067a\u0001\u0000\u0000\u0000\u0678\u0676\u0001\u0000"+
		"\u0000\u0000\u0678\u0679\u0001\u0000\u0000\u0000\u0679\u067b\u0001\u0000"+
		"\u0000\u0000\u067a\u0678\u0001\u0000\u0000\u0000\u067b\u067c\u0005\u0093"+
		"\u0000\u0000\u067c\u00e9\u0001\u0000\u0000\u0000\u067d\u067f\u0003\u0160"+
		"\u00b0\u0000\u067e\u067d\u0001\u0000\u0000\u0000\u067e\u067f\u0001\u0000"+
		"\u0000\u0000\u067f\u0680\u0001\u0000\u0000\u0000\u0680\u0681\u0003\u01b4"+
		"\u00da\u0000\u0681\u00eb\u0001\u0000\u0000\u0000\u0682\u0683\u0005\u0085"+
		"\u0000\u0000\u0683\u0688\u0003\u0014\n\u0000\u0684\u0685\u0005\u0084\u0000"+
		"\u0000\u0685\u0687\u0003\u0002\u0001\u0000\u0686\u0684\u0001\u0000\u0000"+
		"\u0000\u0687\u068a\u0001\u0000\u0000\u0000\u0688\u0686\u0001\u0000\u0000"+
		"\u0000\u0688\u0689\u0001\u0000\u0000\u0000\u0689\u00ed\u0001\u0000\u0000"+
		"\u0000\u068a\u0688\u0001\u0000\u0000\u0000\u068b\u0690\u0003\u0002\u0001"+
		"\u0000\u068c\u068d\u0005\u0084\u0000\u0000\u068d\u068f\u0003\u0002\u0001"+
		"\u0000\u068e\u068c\u0001\u0000\u0000\u0000\u068f\u0692\u0001\u0000\u0000"+
		"\u0000\u0690\u068e\u0001\u0000\u0000\u0000\u0690\u0691\u0001\u0000\u0000"+
		"\u0000\u0691\u00ef\u0001\u0000\u0000\u0000\u0692\u0690\u0001\u0000\u0000"+
		"\u0000\u0693\u0695\u0003\u00f2y\u0000\u0694\u0693\u0001\u0000\u0000\u0000"+
		"\u0695\u0696\u0001\u0000\u0000\u0000\u0696\u0694\u0001\u0000\u0000\u0000"+
		"\u0696\u0697\u0001\u0000\u0000\u0000\u0697\u00f1\u0001\u0000\u0000\u0000"+
		"\u0698\u0699\u0005o\u0000\u0000\u0699\u069a\u0003\u01b4\u00da\u0000\u069a"+
		"\u069b\u0005\u0085\u0000\u0000\u069b\u069c\u0003\u00f4z\u0000\u069c\u00f3"+
		"\u0001\u0000\u0000\u0000\u069d\u06a8\u0003\u00fa}\u0000\u069e\u06a1\u0003"+
		"\u00f6{\u0000\u069f\u06a0\u0005\u0084\u0000\u0000\u06a0\u06a2\u0003\u00f8"+
		"|\u0000\u06a1\u069f\u0001\u0000\u0000\u0000\u06a1\u06a2\u0001\u0000\u0000"+
		"\u0000\u06a2\u06a5\u0001\u0000\u0000\u0000\u06a3\u06a4\u0005\u0084\u0000"+
		"\u0000\u06a4\u06a6\u0003\u00fa}\u0000\u06a5\u06a3\u0001\u0000\u0000\u0000"+
		"\u06a5\u06a6\u0001\u0000\u0000\u0000\u06a6\u06a8\u0001\u0000\u0000\u0000"+
		"\u06a7\u069d\u0001\u0000\u0000\u0000\u06a7\u069e\u0001\u0000\u0000\u0000"+
		"\u06a8\u00f5\u0001\u0000\u0000\u0000\u06a9\u06b1\u0003\u0014\n\u0000\u06aa"+
		"\u06ac\u0005\u001a\u0000\u0000\u06ab\u06ad\u0005\u0094\u0000\u0000\u06ac"+
		"\u06ab\u0001\u0000\u0000\u0000\u06ac\u06ad\u0001\u0000\u0000\u0000\u06ad"+
		"\u06b1\u0001\u0000\u0000\u0000\u06ae\u06b1\u0005\\\u0000\u0000\u06af\u06b1"+
		"\u0005f\u0000\u0000\u06b0\u06a9\u0001\u0000\u0000\u0000\u06b0\u06aa\u0001"+
		"\u0000\u0000\u0000\u06b0\u06ae\u0001\u0000\u0000\u0000\u06b0\u06af\u0001"+
		"\u0000\u0000\u0000\u06b1\u00f7\u0001\u0000\u0000\u0000\u06b2\u06b7\u0003"+
		"\u0002\u0001\u0000\u06b3\u06b4\u0005\u0084\u0000\u0000\u06b4\u06b6\u0003"+
		"\u0002\u0001\u0000\u06b5\u06b3\u0001\u0000\u0000\u0000\u06b6\u06b9\u0001"+
		"\u0000\u0000\u0000\u06b7\u06b5\u0001\u0000\u0000\u0000\u06b7\u06b8\u0001"+
		"\u0000\u0000\u0000\u06b8\u00f9\u0001\u0000\u0000\u0000\u06b9\u06b7\u0001"+
		"\u0000\u0000\u0000\u06ba\u06bb\u0005B\u0000\u0000\u06bb\u06bc\u0005\u0081"+
		"\u0000\u0000\u06bc\u06bd\u0005\u0082\u0000\u0000\u06bd\u00fb\u0001\u0000"+
		"\u0000\u0000\u06be\u06c0\u0005}\u0000\u0000\u06bf\u06c1\u0003\u00fe\u007f"+
		"\u0000\u06c0\u06bf\u0001\u0000\u0000\u0000\u06c0\u06c1\u0001\u0000\u0000"+
		"\u0000\u06c1\u06c2\u0001\u0000\u0000\u0000\u06c2\u06c3\u0005~\u0000\u0000"+
		"\u06c3\u00fd\u0001\u0000\u0000\u0000\u06c4\u06c6\u0003\u0100\u0080\u0000"+
		"\u06c5\u06c4\u0001\u0000\u0000\u0000\u06c6\u06c7\u0001\u0000\u0000\u0000"+
		"\u06c7\u06c5\u0001\u0000\u0000\u0000\u06c7\u06c8\u0001\u0000\u0000\u0000"+
		"\u06c8\u00ff\u0001\u0000\u0000\u0000\u06c9\u06cb\u0003\u0160\u00b0\u0000"+
		"\u06ca\u06c9\u0001\u0000\u0000\u0000\u06ca\u06cb\u0001\u0000\u0000\u0000"+
		"\u06cb\u06cd\u0001\u0000\u0000\u0000\u06cc\u06ce\u0003\u0102\u0081\u0000"+
		"\u06cd\u06cc\u0001\u0000\u0000\u0000\u06cd\u06ce\u0001\u0000\u0000\u0000"+
		"\u06ce\u06d1\u0001\u0000\u0000\u0000\u06cf\u06d2\u0003\u0106\u0083\u0000"+
		"\u06d0\u06d2\u0003\u01a4\u00d2\u0000\u06d1\u06cf\u0001\u0000\u0000\u0000"+
		"\u06d1\u06d0\u0001\u0000\u0000\u0000\u06d2\u0101\u0001\u0000\u0000\u0000"+
		"\u06d3\u06d5\u0003\u0104\u0082\u0000\u06d4\u06d3\u0001\u0000\u0000\u0000"+
		"\u06d5\u06d6\u0001\u0000\u0000\u0000\u06d6\u06d4\u0001\u0000\u0000\u0000"+
		"\u06d6\u06d7\u0001\u0000\u0000\u0000\u06d7\u0103\u0001\u0000\u0000\u0000"+
		"\u06d8\u06d9\u0007\n\u0000\u0000\u06d9\u0105\u0001\u0000\u0000\u0000\u06da"+
		"\u06ee\u0003\u01a0\u00d0\u0000\u06db\u06ee\u0003\u0108\u0084\u0000\u06dc"+
		"\u06ee\u0003\u019a\u00cd\u0000\u06dd\u06e3\u0003\u0136\u009b\u0000\u06de"+
		"\u06e4\u0003\u013a\u009d\u0000\u06df\u06e0\u0003\u0178\u00bc\u0000\u06e0"+
		"\u06e1\u0003L&\u0000\u06e1\u06e2\u0005\u0086\u0000\u0000\u06e2\u06e4\u0001"+
		"\u0000\u0000\u0000\u06e3\u06de\u0001\u0000\u0000\u0000\u06e3\u06df\u0001"+
		"\u0000\u0000\u0000\u06e4\u06ee\u0001\u0000\u0000\u0000\u06e5\u06ee\u0003"+
		"\u01a6\u00d3\u0000\u06e6\u06e7\u0005l\u0000\u0000\u06e7\u06ee\u0003\u01a8"+
		"\u00d4\u0000\u06e8\u06ee\u0003\u0190\u00c8\u0000\u06e9\u06ee\u0003\u0192"+
		"\u00c9\u0000\u06ea\u06ee\u0003\u0194\u00ca\u0000\u06eb\u06ee\u0003\u0196"+
		"\u00cb\u0000\u06ec\u06ee\u0003\u0198\u00cc\u0000\u06ed\u06da\u0001\u0000"+
		"\u0000\u0000\u06ed\u06db\u0001\u0000\u0000\u0000\u06ed\u06dc\u0001\u0000"+
		"\u0000\u0000\u06ed\u06dd\u0001\u0000\u0000\u0000\u06ed\u06e5\u0001\u0000"+
		"\u0000\u0000\u06ed\u06e6\u0001\u0000\u0000\u0000\u06ed\u06e8\u0001\u0000"+
		"\u0000\u0000\u06ed\u06e9\u0001\u0000\u0000\u0000\u06ed\u06ea\u0001\u0000"+
		"\u0000\u0000\u06ed\u06eb\u0001\u0000\u0000\u0000\u06ed\u06ec\u0001\u0000"+
		"\u0000\u0000\u06ee\u0107\u0001\u0000\u0000\u0000\u06ef\u06f5\u0005P\u0000"+
		"\u0000\u06f0\u06f1\u0005O\u0000\u0000\u06f1\u06f5\u0005P\u0000\u0000\u06f2"+
		"\u06f3\u0005P\u0000\u0000\u06f3\u06f5\u0005O\u0000\u0000\u06f4\u06ef\u0001"+
		"\u0000\u0000\u0000\u06f4\u06f0\u0001\u0000\u0000\u0000\u06f4\u06f2\u0001"+
		"\u0000\u0000\u0000\u06f4\u06f5\u0001\u0000\u0000\u0000\u06f5\u06f6\u0001"+
		"\u0000\u0000\u0000\u06f6\u0700\u0003\u0004\u0002\u0000\u06f7\u06f8\u0003"+
		"\u0002\u0001\u0000\u06f8\u06f9\u0005\u0083\u0000\u0000\u06f9\u06fa\u0003"+
		"\u01a2\u00d1\u0000\u06fa\u0701\u0001\u0000\u0000\u0000\u06fb\u0701\u0003"+
		"\u01a8\u00d4\u0000\u06fc\u0701\u0003\u019e\u00cf\u0000\u06fd\u0701\u0003"+
		"\u01a2\u00d1\u0000\u06fe\u0701\u0003\u01ac\u00d6\u0000\u06ff\u0701\u0003"+
		"\u019c\u00ce\u0000\u0700\u06f7\u0001\u0000\u0000\u0000\u0700\u06fb\u0001"+
		"\u0000\u0000\u0000\u0700\u06fc\u0001\u0000\u0000\u0000\u0700\u06fd\u0001"+
		"\u0000\u0000\u0000\u0700\u06fe\u0001\u0000\u0000\u0000\u0700\u06ff\u0001"+
		"\u0000\u0000\u0000\u0701\u0109\u0001\u0000\u0000\u0000\u0702\u0707\u0003"+
		"\u010c\u0086\u0000\u0703\u0704\u0005\u0084\u0000\u0000\u0704\u0706\u0003"+
		"\u010c\u0086\u0000\u0705\u0703\u0001\u0000\u0000\u0000\u0706\u0709\u0001"+
		"\u0000\u0000\u0000\u0707\u0705\u0001\u0000\u0000\u0000\u0707\u0708\u0001"+
		"\u0000\u0000\u0000\u0708\u010b\u0001\u0000\u0000\u0000\u0709\u0707\u0001"+
		"\u0000\u0000\u0000\u070a\u070b\u0003\u01b4\u00da\u0000\u070b\u070c\u0005"+
		"\u0091\u0000\u0000\u070c\u070d\u0003\u001c\u000e\u0000\u070d\u010d\u0001"+
		"\u0000\u0000\u0000\u070e\u0713\u0003\u0110\u0088\u0000\u070f\u0710\u0005"+
		"\u0084\u0000\u0000\u0710\u0712\u0003\u0110\u0088\u0000\u0711\u070f\u0001"+
		"\u0000\u0000\u0000\u0712\u0715\u0001\u0000\u0000\u0000\u0713\u0711\u0001"+
		"\u0000\u0000\u0000\u0713\u0714\u0001\u0000\u0000\u0000\u0714\u010f\u0001"+
		"\u0000\u0000\u0000\u0715\u0713\u0001\u0000\u0000\u0000\u0716\u0719\u0003"+
		"\u01b4\u00da\u0000\u0717\u0718\u0005\u0091\u0000\u0000\u0718\u071a\u0003"+
		"\u0112\u0089\u0000\u0719\u0717\u0001\u0000\u0000\u0000\u0719\u071a\u0001"+
		"\u0000\u0000\u0000\u071a\u0111\u0001\u0000\u0000\u0000\u071b\u071e\u0003"+
		"\u001c\u000e\u0000\u071c\u071e\u0003\u0146\u00a3\u0000\u071d\u071b\u0001"+
		"\u0000\u0000\u0000\u071d\u071c\u0001\u0000\u0000\u0000\u071e\u0113\u0001"+
		"\u0000\u0000\u0000\u071f\u0722\u0003\u0004\u0002\u0000\u0720\u0722\u0005"+
		"l\u0000\u0000\u0721\u071f\u0001\u0000\u0000\u0000\u0721\u0720\u0001\u0000"+
		"\u0000\u0000\u0722\u0115\u0001\u0000\u0000\u0000\u0723\u0724\u0003\u0002"+
		"\u0001\u0000\u0724\u0117\u0001\u0000\u0000\u0000\u0725\u0728\u0003\u00ac"+
		"V\u0000\u0726\u0728\u0005\u0086\u0000\u0000\u0727\u0725\u0001\u0000\u0000"+
		"\u0000\u0727\u0726\u0001\u0000\u0000\u0000\u0728\u0119\u0001\u0000\u0000"+
		"\u0000\u0729\u0730\u0003\u0122\u0091\u0000\u072a\u072d\u0003\u011c\u008e"+
		"\u0000\u072b\u072c\u0005\u0084\u0000\u0000\u072c\u072e\u0003\u0122\u0091"+
		"\u0000\u072d\u072b\u0001\u0000\u0000\u0000\u072d\u072e\u0001\u0000\u0000"+
		"\u0000\u072e\u0730\u0001\u0000\u0000\u0000\u072f\u0729\u0001\u0000\u0000"+
		"\u0000\u072f\u072a\u0001\u0000\u0000\u0000\u0730\u011b\u0001\u0000\u0000"+
		"\u0000\u0731\u0736\u0003\u011e\u008f\u0000\u0732\u0733\u0005\u0084\u0000"+
		"\u0000\u0733\u0735\u0003\u011e\u008f\u0000\u0734\u0732\u0001\u0000\u0000"+
		"\u0000\u0735\u0738\u0001\u0000\u0000\u0000\u0736\u0734\u0001\u0000\u0000"+
		"\u0000\u0736\u0737\u0001\u0000\u0000\u0000\u0737\u011d\u0001\u0000\u0000"+
		"\u0000\u0738\u0736\u0001\u0000\u0000\u0000\u0739\u073b\u0003\u0160\u00b0"+
		"\u0000\u073a\u0739\u0001\u0000\u0000\u0000\u073a\u073b\u0001\u0000\u0000"+
		"\u0000\u073b\u073d\u0001\u0000\u0000\u0000\u073c\u073e\u0003\u0120\u0090"+
		"\u0000\u073d\u073c\u0001\u0000\u0000\u0000\u073d\u073e\u0001\u0000\u0000"+
		"\u0000\u073e\u073f\u0001\u0000\u0000\u0000\u073f\u0742\u0003\u01ae\u00d7"+
		"\u0000\u0740\u0742\u0005\f\u0000\u0000\u0741\u073a\u0001\u0000\u0000\u0000"+
		"\u0741\u0740\u0001\u0000\u0000\u0000\u0742\u011f\u0001\u0000\u0000\u0000"+
		"\u0743\u074c\u0005P\u0000\u0000\u0744\u074c\u0005H\u0000\u0000\u0745\u074c"+
		"\u00056\u0000\u0000\u0746\u0747\u0005P\u0000\u0000\u0747\u074c\u0005^"+
		"\u0000\u0000\u0748\u0749\u00056\u0000\u0000\u0749\u074c\u0005^\u0000\u0000"+
		"\u074a\u074c\u0005^\u0000\u0000\u074b\u0743\u0001\u0000\u0000\u0000\u074b"+
		"\u0744\u0001\u0000\u0000\u0000\u074b\u0745\u0001\u0000\u0000\u0000\u074b"+
		"\u0746\u0001\u0000\u0000\u0000\u074b\u0748\u0001\u0000\u0000\u0000\u074b"+
		"\u074a\u0001\u0000\u0000\u0000\u074c\u0121\u0001\u0000\u0000\u0000\u074d"+
		"\u074f\u0003\u0160\u00b0\u0000\u074e\u074d\u0001\u0000\u0000\u0000\u074e"+
		"\u074f\u0001\u0000\u0000\u0000\u074f\u0750\u0001\u0000\u0000\u0000\u0750"+
		"\u0751\u0005J\u0000\u0000\u0751\u0752\u0003\u0142\u00a1\u0000\u0752\u0753"+
		"\u0003\u01b4\u00da\u0000\u0753\u0123\u0001\u0000\u0000\u0000\u0754\u0756"+
		"\u0003\u0160\u00b0\u0000\u0755\u0754\u0001\u0000\u0000\u0000\u0755\u0756"+
		"\u0001\u0000\u0000\u0000\u0756\u0758\u0001\u0000\u0000\u0000\u0757\u0759"+
		"\u0003\u012a\u0095\u0000\u0758\u0757\u0001\u0000\u0000\u0000\u0758\u0759"+
		"\u0001\u0000\u0000\u0000\u0759\u0764\u0001\u0000\u0000\u0000\u075a\u075b"+
		"\u00051\u0000\u0000\u075b\u075d\u0003\u012c\u0096\u0000\u075c\u075e\u0003"+
		"\u0128\u0094\u0000\u075d\u075c\u0001\u0000\u0000\u0000\u075d\u075e\u0001"+
		"\u0000\u0000\u0000\u075e\u0765\u0001\u0000\u0000\u0000\u075f\u0760\u0005"+
		"V\u0000\u0000\u0760\u0762\u0003\u012c\u0096\u0000\u0761\u0763\u0003\u0126"+
		"\u0093\u0000\u0762\u0761\u0001\u0000\u0000\u0000\u0762\u0763\u0001\u0000"+
		"\u0000\u0000\u0763\u0765\u0001\u0000\u0000\u0000\u0764\u075a\u0001\u0000"+
		"\u0000\u0000\u0764\u075f\u0001\u0000\u0000\u0000\u0765\u0125\u0001\u0000"+
		"\u0000\u0000\u0766\u0768\u0003\u0160\u00b0\u0000\u0767\u0766\u0001\u0000"+
		"\u0000\u0000\u0767\u0768\u0001\u0000\u0000\u0000\u0768\u076a\u0001\u0000"+
		"\u0000\u0000\u0769\u076b\u0003\u012a\u0095\u0000\u076a\u0769\u0001\u0000"+
		"\u0000\u0000\u076a\u076b\u0001\u0000\u0000\u0000\u076b\u076c\u0001\u0000"+
		"\u0000\u0000\u076c\u076d\u00051\u0000\u0000\u076d\u076e\u0003\u012c\u0096"+
		"\u0000\u076e\u0127\u0001\u0000\u0000\u0000\u076f\u0771\u0003\u0160\u00b0"+
		"\u0000\u0770\u076f\u0001\u0000\u0000\u0000\u0770\u0771\u0001\u0000\u0000"+
		"\u0000\u0771\u0773\u0001\u0000\u0000\u0000\u0772\u0774\u0003\u012a\u0095"+
		"\u0000\u0773\u0772\u0001\u0000\u0000\u0000\u0773\u0774\u0001\u0000\u0000"+
		"\u0000\u0774\u0775\u0001\u0000\u0000\u0000\u0775\u0776\u0005V\u0000\u0000"+
		"\u0776\u0777\u0003\u012c\u0096\u0000\u0777\u0129\u0001\u0000\u0000\u0000"+
		"\u0778\u0780\u0005M\u0000\u0000\u0779\u0780\u00059\u0000\u0000\u077a\u0780"+
		"\u0005L\u0000\u0000\u077b\u077c\u0005M\u0000\u0000\u077c\u0780\u00059"+
		"\u0000\u0000\u077d\u077e\u00059\u0000\u0000\u077e\u0780\u0005M\u0000\u0000"+
		"\u077f\u0778\u0001\u0000\u0000\u0000\u077f\u0779\u0001\u0000\u0000\u0000"+
		"\u077f\u077a\u0001\u0000\u0000\u0000\u077f\u077b\u0001\u0000\u0000\u0000"+
		"\u077f\u077d\u0001\u0000\u0000\u0000\u0780\u012b\u0001\u0000\u0000\u0000"+
		"\u0781\u0784\u0003\u00acV\u0000\u0782\u0784\u0005\u0086\u0000\u0000\u0783"+
		"\u0781\u0001\u0000\u0000\u0000\u0783\u0782\u0001\u0000\u0000\u0000\u0784"+
		"\u012d\u0001\u0000\u0000\u0000\u0785\u0787\u0003\u0160\u00b0\u0000\u0786"+
		"\u0785\u0001\u0000\u0000\u0000\u0786\u0787\u0001\u0000\u0000\u0000\u0787"+
		"\u0790\u0001\u0000\u0000\u0000\u0788\u0789\u0005\n\u0000\u0000\u0789\u078a"+
		"\u0003\u00acV\u0000\u078a\u078b\u0003\u0132\u0099\u0000\u078b\u0791\u0001"+
		"\u0000\u0000\u0000\u078c\u078d\u0005Q\u0000\u0000\u078d\u078e\u0003\u00ac"+
		"V\u0000\u078e\u078f\u0003\u0130\u0098\u0000\u078f\u0791\u0001\u0000\u0000"+
		"\u0000\u0790\u0788\u0001\u0000\u0000\u0000\u0790\u078c\u0001\u0000\u0000"+
		"\u0000\u0791\u012f\u0001\u0000\u0000\u0000\u0792\u0794\u0003\u0160\u00b0"+
		"\u0000\u0793\u0792\u0001\u0000\u0000\u0000\u0793\u0794\u0001\u0000\u0000"+
		"\u0000\u0794\u0795\u0001\u0000\u0000\u0000\u0795\u0796\u0005\n\u0000\u0000"+
		"\u0796\u0797\u0003\u00acV\u0000\u0797\u0131\u0001\u0000\u0000\u0000\u0798"+
		"\u079a\u0003\u0160\u00b0\u0000\u0799\u0798\u0001\u0000\u0000\u0000\u0799"+
		"\u079a\u0001\u0000\u0000\u0000\u079a\u079b\u0001\u0000\u0000\u0000\u079b"+
		"\u079c\u0005Q\u0000\u0000\u079c\u079d\u0003\u00acV\u0000\u079d\u0133\u0001"+
		"\u0000\u0000\u0000\u079e\u07b5\u0005\u0087\u0000\u0000\u079f\u07b5\u0005"+
		"\u0088\u0000\u0000\u07a0\u07b5\u0005\u008f\u0000\u0000\u07a1\u07b5\u0005"+
		"\u0090\u0000\u0000\u07a2\u07b5\u0005\u0097\u0000\u0000\u07a3\u07b5\u0005"+
		"\u0098\u0000\u0000\u07a4\u07b5\u0005`\u0000\u0000\u07a5\u07b5\u0005*\u0000"+
		"\u0000\u07a6\u07b5\u0005\u0089\u0000\u0000\u07a7\u07b5\u0005\u008a\u0000"+
		"\u0000\u07a8\u07b5\u0005\u008b\u0000\u0000\u07a9\u07b5\u0005\u008c\u0000"+
		"\u0000\u07aa\u07b5\u0005\u008d\u0000\u0000\u07ab\u07b5\u0005\u008e\u0000"+
		"\u0000\u07ac\u07b5\u0005\u00a8\u0000\u0000\u07ad\u07b5\u0003\u017a\u00bd"+
		"\u0000\u07ae\u07b5\u0005\u009c\u0000\u0000\u07af\u07b5\u0005\u009d\u0000"+
		"\u0000\u07b0\u07b5\u0005\u0093\u0000\u0000\u07b1\u07b5\u0005\u0092\u0000"+
		"\u0000\u07b2\u07b5\u0005\u009f\u0000\u0000\u07b3\u07b5\u0005\u009e\u0000"+
		"\u0000\u07b4\u079e\u0001\u0000\u0000\u0000\u07b4\u079f\u0001\u0000\u0000"+
		"\u0000\u07b4\u07a0\u0001\u0000\u0000\u0000\u07b4\u07a1\u0001\u0000\u0000"+
		"\u0000\u07b4\u07a2\u0001\u0000\u0000\u0000\u07b4\u07a3\u0001\u0000\u0000"+
		"\u0000\u07b4\u07a4\u0001\u0000\u0000\u0000\u07b4\u07a5\u0001\u0000\u0000"+
		"\u0000\u07b4\u07a6\u0001\u0000\u0000\u0000\u07b4\u07a7\u0001\u0000\u0000"+
		"\u0000\u07b4\u07a8\u0001\u0000\u0000\u0000\u07b4\u07a9\u0001\u0000\u0000"+
		"\u0000\u07b4\u07aa\u0001\u0000\u0000\u0000\u07b4\u07ab\u0001\u0000\u0000"+
		"\u0000\u07b4\u07ac\u0001\u0000\u0000\u0000\u07b4\u07ad\u0001\u0000\u0000"+
		"\u0000\u07b4\u07ae\u0001\u0000\u0000\u0000\u07b4\u07af\u0001\u0000\u0000"+
		"\u0000\u07b4\u07b0\u0001\u0000\u0000\u0000\u07b4\u07b1\u0001\u0000\u0000"+
		"\u0000\u07b4\u07b2\u0001\u0000\u0000\u0000\u07b4\u07b3\u0001\u0000\u0000"+
		"\u0000\u07b5\u0135\u0001\u0000\u0000\u0000\u07b6\u07b7\u0007\u000b\u0000"+
		"\u0000\u07b7\u07b8\u0005F\u0000\u0000\u07b8\u07b9\u0003\u0004\u0002\u0000"+
		"\u07b9\u07ba\u0005\u0081\u0000\u0000\u07ba\u07bb\u0003\u01ae\u00d7\u0000"+
		"\u07bb\u07bc\u0005\u0082\u0000\u0000\u07bc\u0137\u0001\u0000\u0000\u0000"+
		"\u07bd\u07be\u0005\u0085\u0000\u0000\u07be\u07bf\u0007\f\u0000\u0000\u07bf"+
		"\u07c1\u0005\u0081\u0000\u0000\u07c0\u07c2\u0003\u0018\f\u0000\u07c1\u07c0"+
		"\u0001\u0000\u0000\u0000\u07c1\u07c2\u0001\u0000\u0000\u0000\u07c2\u07c3"+
		"\u0001\u0000\u0000\u0000\u07c3\u07c4\u0005\u0082\u0000\u0000\u07c4\u0139"+
		"\u0001\u0000\u0000\u0000\u07c5\u07c8\u0003\u00acV\u0000\u07c6\u07c8\u0005"+
		"\u0086\u0000\u0000\u07c7\u07c5\u0001\u0000\u0000\u0000\u07c7\u07c6\u0001"+
		"\u0000\u0000\u0000\u07c8\u013b\u0001\u0000\u0000\u0000\u07c9\u07ca\u0005"+
		"\u0085\u0000\u0000\u07ca\u07cb\u0003\u00eew\u0000\u07cb\u013d\u0001\u0000"+
		"\u0000\u0000\u07cc\u07d0\u0005}\u0000\u0000\u07cd\u07cf\u0003\u0140\u00a0"+
		"\u0000\u07ce\u07cd\u0001\u0000\u0000\u0000\u07cf\u07d2\u0001\u0000\u0000"+
		"\u0000\u07d0\u07ce\u0001\u0000\u0000\u0000\u07d0\u07d1\u0001\u0000\u0000"+
		"\u0000\u07d1\u07d3\u0001\u0000\u0000\u0000\u07d2\u07d0\u0001\u0000\u0000"+
		"\u0000\u07d3\u07d4\u0005~\u0000\u0000\u07d4\u013f\u0001\u0000\u0000\u0000"+
		"\u07d5\u07d7\u0003\u0160\u00b0\u0000\u07d6\u07d5\u0001\u0000\u0000\u0000"+
		"\u07d6\u07d7\u0001\u0000\u0000\u0000\u07d7\u07d9\u0001\u0000\u0000\u0000"+
		"\u07d8\u07da\u0003\u0102\u0081\u0000\u07d9\u07d8\u0001\u0000\u0000\u0000"+
		"\u07d9\u07da\u0001\u0000\u0000\u0000\u07da\u07e5\u0001\u0000\u0000\u0000"+
		"\u07db\u07e6\u0003\u0106\u0083\u0000\u07dc\u07dd\u0005,\u0000\u0000\u07dd"+
		"\u07df\u0003\u0004\u0002\u0000\u07de\u07e0\u0003\u0174\u00ba\u0000\u07df"+
		"\u07de\u0001\u0000\u0000\u0000\u07e0\u07e1\u0001\u0000\u0000\u0000\u07e1"+
		"\u07df\u0001\u0000\u0000\u0000\u07e1\u07e2\u0001\u0000\u0000\u0000\u07e2"+
		"\u07e3\u0001\u0000\u0000\u0000\u07e3\u07e4\u0005\u0086\u0000\u0000\u07e4"+
		"\u07e6\u0001\u0000\u0000\u0000\u07e5\u07db\u0001\u0000\u0000\u0000\u07e5"+
		"\u07dc\u0001\u0000\u0000\u0000\u07e6\u0141\u0001\u0000\u0000\u0000\u07e7"+
		"\u07ef\u0003\u0006\u0003\u0000\u07e8\u07ea\u0007\r\u0000\u0000\u07e9\u07e8"+
		"\u0001\u0000\u0000\u0000\u07ea\u07ed\u0001\u0000\u0000\u0000\u07eb\u07e9"+
		"\u0001\u0000\u0000\u0000\u07eb\u07ec\u0001\u0000\u0000\u0000\u07ec\u07ee"+
		"\u0001\u0000\u0000\u0000\u07ed\u07eb\u0001\u0000\u0000\u0000\u07ee\u07f0"+
		"\u0003\u0144\u00a2\u0000\u07ef\u07eb\u0001\u0000\u0000\u0000\u07f0\u07f1"+
		"\u0001\u0000\u0000\u0000\u07f1\u07ef\u0001\u0000\u0000\u0000\u07f1\u07f2"+
		"\u0001\u0000\u0000\u0000\u07f2\u0143\u0001\u0000\u0000\u0000\u07f3\u07f7"+
		"\u0005\u007f\u0000\u0000\u07f4\u07f6\u0005\u0084\u0000\u0000\u07f5\u07f4"+
		"\u0001\u0000\u0000\u0000\u07f6\u07f9\u0001\u0000\u0000\u0000\u07f7\u07f5"+
		"\u0001\u0000\u0000\u0000\u07f7\u07f8\u0001\u0000\u0000\u0000\u07f8\u07fa"+
		"\u0001\u0000\u0000\u0000\u07f9\u07f7\u0001\u0000\u0000\u0000\u07fa\u07fb"+
		"\u0005\u0080\u0000\u0000\u07fb\u0145\u0001\u0000\u0000\u0000\u07fc\u0808"+
		"\u0005}\u0000\u0000\u07fd\u0802\u0003\u0112\u0089\u0000\u07fe\u07ff\u0005"+
		"\u0084\u0000\u0000\u07ff\u0801\u0003\u0112\u0089\u0000\u0800\u07fe\u0001"+
		"\u0000\u0000\u0000\u0801\u0804\u0001\u0000\u0000\u0000\u0802\u0800\u0001"+
		"\u0000\u0000\u0000\u0802\u0803\u0001\u0000\u0000\u0000\u0803\u0806\u0001"+
		"\u0000\u0000\u0000\u0804\u0802\u0001\u0000\u0000\u0000\u0805\u0807\u0005"+
		"\u0084\u0000\u0000\u0806\u0805\u0001\u0000\u0000\u0000\u0806\u0807\u0001"+
		"\u0000\u0000\u0000\u0807\u0809\u0001\u0000\u0000\u0000\u0808\u07fd\u0001"+
		"\u0000\u0000\u0000\u0808\u0809\u0001\u0000\u0000\u0000\u0809\u080a\u0001"+
		"\u0000\u0000\u0000\u080a\u080b\u0005~\u0000\u0000\u080b\u0147\u0001\u0000"+
		"\u0000\u0000\u080c\u080d\u0005\u0092\u0000\u0000\u080d\u0812\u0003\u014a"+
		"\u00a5\u0000\u080e\u080f\u0005\u0084\u0000\u0000\u080f\u0811\u0003\u014a"+
		"\u00a5\u0000\u0810\u080e\u0001\u0000\u0000\u0000\u0811\u0814\u0001\u0000"+
		"\u0000\u0000\u0812\u0810\u0001\u0000\u0000\u0000\u0812\u0813\u0001\u0000"+
		"\u0000\u0000\u0813\u0815\u0001\u0000\u0000\u0000\u0814\u0812\u0001\u0000"+
		"\u0000\u0000\u0815\u0816\u0005\u0093\u0000\u0000\u0816\u0149\u0001\u0000"+
		"\u0000\u0000\u0817\u0819\u0003\u0160\u00b0\u0000\u0818\u0817\u0001\u0000"+
		"\u0000\u0000\u0818\u0819\u0001\u0000\u0000\u0000\u0819\u081b\u0001\u0000"+
		"\u0000\u0000\u081a\u081c\u0003\u014c\u00a6\u0000\u081b\u081a\u0001\u0000"+
		"\u0000\u0000\u081b\u081c\u0001\u0000\u0000\u0000\u081c\u081d\u0001\u0000"+
		"\u0000\u0000\u081d\u081e\u0003\u01b4\u00da\u0000\u081e\u014b\u0001\u0000"+
		"\u0000\u0000\u081f\u0820\u0007\u000e\u0000\u0000\u0820\u014d\u0001\u0000"+
		"\u0000\u0000\u0821\u0822\u0005\u0085\u0000\u0000\u0822\u0823\u0003\u00ee"+
		"w\u0000\u0823\u014f\u0001\u0000\u0000\u0000\u0824\u0828\u0005}\u0000\u0000"+
		"\u0825\u0827\u0003\u0152\u00a9\u0000\u0826\u0825\u0001\u0000\u0000\u0000"+
		"\u0827\u082a\u0001\u0000\u0000\u0000\u0828\u0826\u0001\u0000\u0000\u0000"+
		"\u0828\u0829\u0001\u0000\u0000\u0000\u0829\u082b\u0001\u0000\u0000\u0000"+
		"\u082a\u0828\u0001\u0000\u0000\u0000\u082b\u082c\u0005~\u0000\u0000\u082c"+
		"\u0151\u0001\u0000\u0000\u0000\u082d\u082f\u0003\u0160\u00b0\u0000\u082e"+
		"\u082d\u0001\u0000\u0000\u0000\u082e\u082f\u0001\u0000\u0000\u0000\u082f"+
		"\u0831\u0001\u0000\u0000\u0000\u0830\u0832\u0005B\u0000\u0000\u0831\u0830"+
		"\u0001\u0000\u0000\u0000\u0831\u0832\u0001\u0000\u0000\u0000\u0832\u0872"+
		"\u0001\u0000\u0000\u0000\u0833\u0835\u0005g\u0000\u0000\u0834\u0833\u0001"+
		"\u0000\u0000\u0000\u0834\u0835\u0001\u0000\u0000\u0000\u0835\u083b\u0001"+
		"\u0000\u0000\u0000\u0836\u083c\u0005P\u0000\u0000\u0837\u0838\u0005P\u0000"+
		"\u0000\u0838\u083c\u0005O\u0000\u0000\u0839\u083a\u0005O\u0000\u0000\u083a"+
		"\u083c\u0005P\u0000\u0000\u083b\u0836\u0001\u0000\u0000\u0000\u083b\u0837"+
		"\u0001\u0000\u0000\u0000\u083b\u0839\u0001\u0000\u0000\u0000\u083b\u083c"+
		"\u0001\u0000\u0000\u0000\u083c\u083d\u0001\u0000\u0000\u0000\u083d\u0859"+
		"\u0003\u0004\u0002\u0000\u083e\u0840\u0003\u01b4\u00da\u0000\u083f\u0841"+
		"\u0003\u00e8t\u0000\u0840\u083f\u0001\u0000\u0000\u0000\u0840\u0841\u0001"+
		"\u0000\u0000\u0000\u0841\u0842\u0001\u0000\u0000\u0000\u0842\u0844\u0005"+
		"\u0081\u0000\u0000\u0843\u0845\u0003\u011a\u008d\u0000\u0844\u0843\u0001"+
		"\u0000\u0000\u0000\u0844\u0845\u0001\u0000\u0000\u0000\u0845\u0846\u0001"+
		"\u0000\u0000\u0000\u0846\u0848\u0005\u0082\u0000\u0000\u0847\u0849\u0003"+
		"\u00f0x\u0000\u0848\u0847\u0001\u0000\u0000\u0000\u0848\u0849\u0001\u0000"+
		"\u0000\u0000\u0849\u084a\u0001\u0000\u0000\u0000\u084a\u084b\u0005\u0086"+
		"\u0000\u0000\u084b\u085a\u0001\u0000\u0000\u0000\u084c\u084d\u0003\u01b4"+
		"\u00da\u0000\u084d\u084e\u0005}\u0000\u0000\u084e\u084f\u0003\u0154\u00aa"+
		"\u0000\u084f\u0850\u0005~\u0000\u0000\u0850\u085a\u0001\u0000\u0000\u0000"+
		"\u0851\u0852\u0005^\u0000\u0000\u0852\u0853\u0005\u007f\u0000\u0000\u0853"+
		"\u0854\u0003\u011a\u008d\u0000\u0854\u0855\u0005\u0080\u0000\u0000\u0855"+
		"\u0856\u0005}\u0000\u0000\u0856\u0857\u0003\u0154\u00aa\u0000\u0857\u0858"+
		"\u0005~\u0000\u0000\u0858\u085a\u0001\u0000\u0000\u0000\u0859\u083e\u0001"+
		"\u0000\u0000\u0000\u0859\u084c\u0001\u0000\u0000\u0000\u0859\u0851\u0001"+
		"\u0000\u0000\u0000\u085a\u0873\u0001\u0000\u0000\u0000\u085b\u085d\u0005"+
		"g\u0000\u0000\u085c\u085b\u0001\u0000\u0000\u0000\u085c\u085d\u0001\u0000"+
		"\u0000\u0000\u085d\u085e\u0001\u0000\u0000\u0000\u085e\u085f\u0005l\u0000"+
		"\u0000\u085f\u0861\u0003\u01b4\u00da\u0000\u0860\u0862\u0003\u00e8t\u0000"+
		"\u0861\u0860\u0001\u0000\u0000\u0000\u0861\u0862\u0001\u0000\u0000\u0000"+
		"\u0862\u0863\u0001\u0000\u0000\u0000\u0863\u0865\u0005\u0081\u0000\u0000"+
		"\u0864\u0866\u0003\u011a\u008d\u0000\u0865\u0864\u0001\u0000\u0000\u0000"+
		"\u0865\u0866\u0001\u0000\u0000\u0000\u0866\u0867\u0001\u0000\u0000\u0000"+
		"\u0867\u0869\u0005\u0082\u0000\u0000\u0868\u086a\u0003\u00f0x\u0000\u0869"+
		"\u0868\u0001\u0000\u0000\u0000\u0869\u086a\u0001\u0000\u0000\u0000\u086a"+
		"\u086b\u0001\u0000\u0000\u0000\u086b\u086c\u0005\u0086\u0000\u0000\u086c"+
		"\u0873\u0001\u0000\u0000\u0000\u086d\u086e\u0005\'\u0000\u0000\u086e\u086f"+
		"\u0003\u0004\u0002\u0000\u086f\u0870\u0003\u01b4\u00da\u0000\u0870\u0871"+
		"\u0005\u0086\u0000\u0000\u0871\u0873\u0001\u0000\u0000\u0000\u0872\u0834"+
		"\u0001\u0000\u0000\u0000\u0872\u085c\u0001\u0000\u0000\u0000\u0872\u086d"+
		"\u0001\u0000\u0000\u0000\u0873\u0153\u0001\u0000\u0000\u0000\u0874\u0876"+
		"\u0003\u0160\u00b0\u0000\u0875\u0874\u0001\u0000\u0000\u0000\u0875\u0876"+
		"\u0001\u0000\u0000\u0000\u0876\u0889\u0001\u0000\u0000\u0000\u0877\u0878"+
		"\u00051\u0000\u0000\u0878\u087e\u0005\u0086\u0000\u0000\u0879\u087b\u0003"+
		"\u0160\u00b0\u0000\u087a\u0879\u0001\u0000\u0000\u0000\u087a\u087b\u0001"+
		"\u0000\u0000\u0000\u087b\u087c\u0001\u0000\u0000\u0000\u087c\u087d\u0005"+
		"V\u0000\u0000\u087d\u087f\u0005\u0086\u0000\u0000\u087e\u087a\u0001\u0000"+
		"\u0000\u0000\u087e\u087f\u0001\u0000\u0000\u0000\u087f\u088a\u0001\u0000"+
		"\u0000\u0000\u0880\u0881\u0005V\u0000\u0000\u0881\u0887\u0005\u0086\u0000"+
		"\u0000\u0882\u0884\u0003\u0160\u00b0\u0000\u0883\u0882\u0001\u0000\u0000"+
		"\u0000\u0883\u0884\u0001\u0000\u0000\u0000\u0884\u0885\u0001\u0000\u0000"+
		"\u0000\u0885\u0886\u00051\u0000\u0000\u0886\u0888\u0005\u0086\u0000\u0000"+
		"\u0887\u0883\u0001\u0000\u0000\u0000\u0887\u0888\u0001\u0000\u0000\u0000"+
		"\u0888\u088a\u0001\u0000\u0000\u0000\u0889\u0877\u0001\u0000\u0000\u0000"+
		"\u0889\u0880\u0001\u0000\u0000\u0000\u088a\u0155\u0001\u0000\u0000\u0000"+
		"\u088b\u088c\u0005\u0085\u0000\u0000\u088c\u088d\u0003\u0004\u0002\u0000"+
		"\u088d\u0157\u0001\u0000\u0000\u0000\u088e\u089a\u0005}\u0000\u0000\u088f"+
		"\u0894\u0003\u015a\u00ad\u0000\u0890\u0891\u0005\u0084\u0000\u0000\u0891"+
		"\u0893\u0003\u015a\u00ad\u0000\u0892\u0890\u0001\u0000\u0000\u0000\u0893"+
		"\u0896\u0001\u0000\u0000\u0000\u0894\u0892\u0001\u0000\u0000\u0000\u0894"+
		"\u0895\u0001\u0000\u0000\u0000\u0895\u0898\u0001\u0000\u0000\u0000\u0896"+
		"\u0894\u0001\u0000\u0000\u0000\u0897\u0899\u0005\u0084\u0000\u0000\u0898"+
		"\u0897\u0001\u0000\u0000\u0000\u0898\u0899\u0001\u0000\u0000\u0000\u0899"+
		"\u089b\u0001\u0000\u0000\u0000\u089a\u088f\u0001\u0000\u0000\u0000\u089a"+
		"\u089b\u0001\u0000\u0000\u0000\u089b\u089c\u0001\u0000\u0000\u0000\u089c"+
		"\u089d\u0005~\u0000\u0000\u089d\u0159\u0001\u0000\u0000\u0000\u089e\u08a0"+
		"\u0003\u0160\u00b0\u0000\u089f\u089e\u0001\u0000\u0000\u0000\u089f\u08a0"+
		"\u0001\u0000\u0000\u0000\u08a0\u08a1\u0001\u0000\u0000\u0000\u08a1\u08a4"+
		"\u0003\u01b4\u00da\u0000\u08a2\u08a3\u0005\u0091\u0000\u0000\u08a3\u08a5"+
		"\u0003\u001c\u000e\u0000\u08a4\u08a2\u0001\u0000\u0000\u0000\u08a4\u08a5"+
		"\u0001\u0000\u0000\u0000\u08a5\u015b\u0001\u0000\u0000\u0000\u08a6\u08a7"+
		"\u0005\u007f\u0000\u0000\u08a7\u08a8\u0003\u015e\u00af\u0000\u08a8\u08a9"+
		"\u0005\u0085\u0000\u0000\u08a9\u08ab\u0003\u0166\u00b3\u0000\u08aa\u08ac"+
		"\u0005\u0084\u0000\u0000\u08ab\u08aa\u0001\u0000\u0000\u0000\u08ab\u08ac"+
		"\u0001\u0000\u0000\u0000\u08ac\u08ad\u0001\u0000\u0000\u0000\u08ad\u08ae"+
		"\u0005\u0080\u0000\u0000\u08ae\u015d\u0001\u0000\u0000\u0000\u08af\u08b2"+
		"\u0003\u018e\u00c7\u0000\u08b0\u08b2\u0003\u01b4\u00da\u0000\u08b1\u08af"+
		"\u0001\u0000\u0000\u0000\u08b1\u08b0\u0001\u0000\u0000\u0000\u08b2\u015f"+
		"\u0001\u0000\u0000\u0000\u08b3\u08b5\u0003\u0162\u00b1\u0000\u08b4\u08b3"+
		"\u0001\u0000\u0000\u0000\u08b5\u08b6\u0001\u0000\u0000\u0000\u08b6\u08b4"+
		"\u0001\u0000\u0000\u0000\u08b6\u08b7\u0001\u0000\u0000\u0000\u08b7\u0161"+
		"\u0001\u0000\u0000\u0000\u08b8\u08bc\u0005\u007f\u0000\u0000\u08b9\u08ba"+
		"\u0003\u0164\u00b2\u0000\u08ba\u08bb\u0005\u0085\u0000\u0000\u08bb\u08bd"+
		"\u0001\u0000\u0000\u0000\u08bc\u08b9\u0001\u0000\u0000\u0000\u08bc\u08bd"+
		"\u0001\u0000\u0000\u0000\u08bd\u08be\u0001\u0000\u0000\u0000\u08be\u08c0"+
		"\u0003\u0166\u00b3\u0000\u08bf\u08c1\u0005\u0084\u0000\u0000\u08c0\u08bf"+
		"\u0001\u0000\u0000\u0000\u08c0\u08c1\u0001\u0000\u0000\u0000\u08c1\u08c2"+
		"\u0001\u0000\u0000\u0000\u08c2\u08c3\u0005\u0080\u0000\u0000\u08c3\u0163"+
		"\u0001\u0000\u0000\u0000\u08c4\u08c7\u0003\u018e\u00c7\u0000\u08c5\u08c7"+
		"\u0003\u01b4\u00da\u0000\u08c6\u08c4\u0001\u0000\u0000\u0000\u08c6\u08c5"+
		"\u0001\u0000\u0000\u0000\u08c7\u0165\u0001\u0000\u0000\u0000\u08c8\u08cd"+
		"\u0003\u0168\u00b4\u0000\u08c9\u08ca\u0005\u0084\u0000\u0000\u08ca\u08cc"+
		"\u0003\u0168\u00b4\u0000\u08cb\u08c9\u0001\u0000\u0000\u0000\u08cc\u08cf"+
		"\u0001\u0000\u0000\u0000\u08cd\u08cb\u0001\u0000\u0000\u0000\u08cd\u08ce"+
		"\u0001\u0000\u0000\u0000\u08ce\u0167\u0001\u0000\u0000\u0000\u08cf\u08cd"+
		"\u0001\u0000\u0000\u0000\u08d0\u08dd\u0003\u0002\u0001\u0000\u08d1\u08da"+
		"\u0005\u0081\u0000\u0000\u08d2\u08d7\u0003\u016a\u00b5\u0000\u08d3\u08d4"+
		"\u0005\u0084\u0000\u0000\u08d4\u08d6\u0003\u016a\u00b5\u0000\u08d5\u08d3"+
		"\u0001\u0000\u0000\u0000\u08d6\u08d9\u0001\u0000\u0000\u0000\u08d7\u08d5"+
		"\u0001\u0000\u0000\u0000\u08d7\u08d8\u0001\u0000\u0000\u0000\u08d8\u08db"+
		"\u0001\u0000\u0000\u0000\u08d9\u08d7\u0001\u0000\u0000\u0000\u08da\u08d2"+
		"\u0001\u0000\u0000\u0000\u08da\u08db\u0001\u0000\u0000\u0000\u08db\u08dc"+
		"\u0001\u0000\u0000\u0000\u08dc\u08de\u0005\u0082\u0000\u0000\u08dd\u08d1"+
		"\u0001\u0000\u0000\u0000\u08dd\u08de\u0001\u0000\u0000\u0000\u08de\u0169"+
		"\u0001\u0000\u0000\u0000\u08df\u08e0\u0003\u01b4\u00da\u0000\u08e0\u08e1"+
		"\u0005\u0085\u0000\u0000\u08e1\u08e3\u0001\u0000\u0000\u0000\u08e2\u08df"+
		"\u0001\u0000\u0000\u0000\u08e2\u08e3\u0001\u0000\u0000\u0000\u08e3\u08e4"+
		"\u0001\u0000\u0000\u0000\u08e4\u08e5\u0003\u001c\u000e\u0000\u08e5\u016b"+
		"\u0001\u0000\u0000\u0000\u08e6\u08e9\u0003\f\u0006\u0000\u08e7\u08e9\u0003"+
		"\u0014\n\u0000\u08e8\u08e6\u0001\u0000\u0000\u0000\u08e8\u08e7\u0001\u0000"+
		"\u0000\u0000\u08e9\u08ee\u0001\u0000\u0000\u0000\u08ea\u08ed\u0003\u0144"+
		"\u00a2\u0000\u08eb\u08ed\u0005\u0094\u0000\u0000\u08ec\u08ea\u0001\u0000"+
		"\u0000\u0000\u08ec\u08eb\u0001\u0000\u0000\u0000\u08ed\u08f0\u0001\u0000"+
		"\u0000\u0000\u08ee\u08ec\u0001\u0000\u0000\u0000\u08ee\u08ef\u0001\u0000"+
		"\u0000\u0000\u08ef\u08f1\u0001\u0000\u0000\u0000\u08f0\u08ee\u0001\u0000"+
		"\u0000\u0000\u08f1\u08f2\u0005\u0089\u0000\u0000\u08f2\u08f6\u0001\u0000"+
		"\u0000\u0000\u08f3\u08f4\u0005l\u0000\u0000\u08f4\u08f6\u0005\u0089\u0000"+
		"\u0000\u08f5\u08e8\u0001\u0000\u0000\u0000\u08f5\u08f3\u0001\u0000\u0000"+
		"\u0000\u08f6\u016d\u0001\u0000\u0000\u0000\u08f7\u08fc\u0003\u0170\u00b8"+
		"\u0000\u08f8\u08f9\u0005\u0084\u0000\u0000\u08f9\u08fb\u0003\u0170\u00b8"+
		"\u0000\u08fa\u08f8\u0001\u0000\u0000\u0000\u08fb\u08fe\u0001\u0000\u0000"+
		"\u0000\u08fc\u08fa\u0001\u0000\u0000\u0000\u08fc\u08fd\u0001\u0000\u0000"+
		"\u0000\u08fd\u016f\u0001\u0000\u0000\u0000\u08fe\u08fc\u0001\u0000\u0000"+
		"\u0000\u08ff\u0900\u0003\u01b4\u00da\u0000\u0900\u0901\u0005\u0091\u0000"+
		"\u0000\u0901\u0902\u0003\u0172\u00b9\u0000\u0902\u0171\u0001\u0000\u0000"+
		"\u0000\u0903\u0905\u0005\u008c\u0000\u0000\u0904\u0903\u0001\u0000\u0000"+
		"\u0000\u0904\u0905\u0001\u0000\u0000\u0000\u0905\u0906\u0001\u0000\u0000"+
		"\u0000\u0906\u0909\u0003\u001c\u000e\u0000\u0907\u0909\u0003\u0176\u00bb"+
		"\u0000\u0908\u0904\u0001\u0000\u0000\u0000\u0908\u0907\u0001\u0000\u0000"+
		"\u0000\u0909\u0173\u0001\u0000\u0000\u0000\u090a\u090b\u0003\u01b4\u00da"+
		"\u0000\u090b\u090c\u0005\u007f\u0000\u0000\u090c\u090d\u0003\u001c\u000e"+
		"\u0000\u090d\u090e\u0005\u0080\u0000\u0000\u090e\u0175\u0001\u0000\u0000"+
		"\u0000\u090f\u0910\u0005Y\u0000\u0000\u0910\u0911\u0003\u0004\u0002\u0000"+
		"\u0911\u0912\u0005\u007f\u0000\u0000\u0912\u0913\u0003\u001c\u000e\u0000"+
		"\u0913\u0914\u0005\u0080\u0000\u0000\u0914\u092d\u0001\u0000\u0000\u0000"+
		"\u0915\u0917\u0005Y\u0000\u0000\u0916\u0918\u0003\u0004\u0002\u0000\u0917"+
		"\u0916\u0001\u0000\u0000\u0000\u0917\u0918\u0001\u0000\u0000\u0000\u0918"+
		"\u0919\u0001\u0000\u0000\u0000\u0919\u091b\u0005\u007f\u0000\u0000\u091a"+
		"\u091c\u0003\u001c\u000e\u0000\u091b\u091a\u0001\u0000\u0000\u0000\u091b"+
		"\u091c\u0001\u0000\u0000\u0000\u091c\u091d\u0001\u0000\u0000\u0000\u091d"+
		"\u091e\u0005\u0080\u0000\u0000\u091e\u091f\u0005}\u0000\u0000\u091f\u0924"+
		"\u0003\u001c\u000e\u0000\u0920\u0921\u0005\u0084\u0000\u0000\u0921\u0923"+
		"\u0003\u001c\u000e\u0000\u0922\u0920\u0001\u0000\u0000\u0000\u0923\u0926"+
		"\u0001\u0000\u0000\u0000\u0924\u0922\u0001\u0000\u0000\u0000\u0924\u0925"+
		"\u0001\u0000\u0000\u0000\u0925\u0928\u0001\u0000\u0000\u0000\u0926\u0924"+
		"\u0001\u0000\u0000\u0000\u0927\u0929\u0005\u0084\u0000\u0000\u0928\u0927"+
		"\u0001\u0000\u0000\u0000\u0928\u0929\u0001\u0000\u0000\u0000\u0929\u092a"+
		"\u0001\u0000\u0000\u0000\u092a\u092b\u0005~\u0000\u0000\u092b\u092d\u0001"+
		"\u0000\u0000\u0000\u092c\u090f\u0001\u0000\u0000\u0000\u092c\u0915\u0001"+
		"\u0000\u0000\u0000\u092d\u0177\u0001\u0000\u0000\u0000\u092e\u092f\u0005"+
		"\u0091\u0000\u0000\u092f\u0930\u0005\u0093\u0000\u0000\u0930\u0931\u0004"+
		"\u00bc\u0001\u0001\u0931\u0179\u0001\u0000\u0000\u0000\u0932\u0933\u0005"+
		"\u0093\u0000\u0000\u0933\u0934\u0005\u0093\u0000\u0000\u0934\u0935\u0004"+
		"\u00bd\u0002\u0001\u0935\u017b\u0001\u0000\u0000\u0000\u0936\u0937\u0005"+
		"\u0093\u0000\u0000\u0937\u0938\u0005\u009f\u0000\u0000\u0938\u0939\u0004"+
		"\u00be\u0003\u0001\u0939\u017d\u0001\u0000\u0000\u0000\u093a\u0943\u0003"+
		"\u0180\u00c0\u0000\u093b\u0943\u0003\u0182\u00c1\u0000\u093c\u0943\u0005"+
		"t\u0000\u0000\u093d\u0943\u0005u\u0000\u0000\u093e\u0943\u0005v\u0000"+
		"\u0000\u093f\u0943\u0005w\u0000\u0000\u0940\u0943\u0005x\u0000\u0000\u0941"+
		"\u0943\u0005C\u0000\u0000\u0942\u093a\u0001\u0000\u0000\u0000\u0942\u093b"+
		"\u0001\u0000\u0000\u0000\u0942\u093c\u0001\u0000\u0000\u0000\u0942\u093d"+
		"\u0001\u0000\u0000\u0000\u0942\u093e\u0001\u0000\u0000\u0000\u0942\u093f"+
		"\u0001\u0000\u0000\u0000\u0942\u0940\u0001\u0000\u0000\u0000\u0942\u0941"+
		"\u0001\u0000\u0000\u0000\u0943\u017f\u0001\u0000\u0000\u0000\u0944\u0945"+
		"\u0007\u000f\u0000\u0000\u0945\u0181\u0001\u0000\u0000\u0000\u0946\u094b"+
		"\u0003\u0184\u00c2\u0000\u0947\u094b\u0003\u0186\u00c3\u0000\u0948\u094b"+
		"\u0005y\u0000\u0000\u0949\u094b\u0005z\u0000\u0000\u094a\u0946\u0001\u0000"+
		"\u0000\u0000\u094a\u0947\u0001\u0000\u0000\u0000\u094a\u0948\u0001\u0000"+
		"\u0000\u0000\u094a\u0949\u0001\u0000\u0000\u0000\u094b\u0183\u0001\u0000"+
		"\u0000\u0000\u094c\u0950\u0005{\u0000\u0000\u094d\u094f\u0003\u0188\u00c4"+
		"\u0000\u094e\u094d\u0001\u0000\u0000\u0000\u094f\u0952\u0001\u0000\u0000"+
		"\u0000\u0950\u094e\u0001\u0000\u0000\u0000\u0950\u0951\u0001\u0000\u0000"+
		"\u0000\u0951\u0953\u0001\u0000\u0000\u0000\u0952\u0950\u0001\u0000\u0000"+
		"\u0000\u0953\u0954\u0005\u00b0\u0000\u0000\u0954\u0185\u0001\u0000\u0000"+
		"\u0000\u0955\u0959\u0005|\u0000";
	private static final String _serializedATNSegment1 =
		"\u0000\u0956\u0958\u0003\u018a\u00c5\u0000\u0957\u0956\u0001\u0000\u0000"+
		"\u0000\u0958\u095b\u0001\u0000\u0000\u0000\u0959\u0957\u0001\u0000\u0000"+
		"\u0000\u0959\u095a\u0001\u0000\u0000\u0000\u095a\u095c\u0001\u0000\u0000"+
		"\u0000\u095b\u0959\u0001\u0000\u0000\u0000\u095c\u095d\u0005\u00b0\u0000"+
		"\u0000\u095d\u0187\u0001\u0000\u0000\u0000\u095e\u0963\u0003\u018c\u00c6"+
		"\u0000\u095f\u0963\u0005\u00ac\u0000\u0000\u0960\u0963\u0005\u00ae\u0000"+
		"\u0000\u0961\u0963\u0005\u00b1\u0000\u0000\u0962\u095e\u0001\u0000\u0000"+
		"\u0000\u0962\u095f\u0001\u0000\u0000\u0000\u0962\u0960\u0001\u0000\u0000"+
		"\u0000\u0962\u0961\u0001\u0000\u0000\u0000\u0963\u0189\u0001\u0000\u0000"+
		"\u0000\u0964\u0969\u0003\u018c\u00c6\u0000\u0965\u0969\u0005\u00ac\u0000"+
		"\u0000\u0966\u0969\u0005\u00af\u0000\u0000\u0967\u0969\u0005\u00b2\u0000"+
		"\u0000\u0968\u0964\u0001\u0000\u0000\u0000\u0968\u0965\u0001\u0000\u0000"+
		"\u0000\u0968\u0966\u0001\u0000\u0000\u0000\u0968\u0967\u0001\u0000\u0000"+
		"\u0000\u0969\u018b\u0001\u0000\u0000\u0000\u096a\u096f\u0003\u001c\u000e"+
		"\u0000\u096b\u096c\u0005\u0084\u0000\u0000\u096c\u096e\u0003\u001c\u000e"+
		"\u0000\u096d\u096b\u0001\u0000\u0000\u0000\u096e\u0971\u0001\u0000\u0000"+
		"\u0000\u096f\u096d\u0001\u0000\u0000\u0000\u096f\u0970\u0001\u0000\u0000"+
		"\u0000\u0970\u0978\u0001\u0000\u0000\u0000\u0971\u096f\u0001\u0000\u0000"+
		"\u0000\u0972\u0974\u0005\u0085\u0000\u0000\u0973\u0975\u0005\u00b4\u0000"+
		"\u0000\u0974\u0973\u0001\u0000\u0000\u0000\u0975\u0976\u0001\u0000\u0000"+
		"\u0000\u0976\u0974\u0001\u0000\u0000\u0000\u0976\u0977\u0001\u0000\u0000"+
		"\u0000\u0977\u0979\u0001\u0000\u0000\u0000\u0978\u0972\u0001\u0000\u0000"+
		"\u0000\u0978\u0979\u0001\u0000\u0000\u0000\u0979\u018d\u0001\u0000\u0000"+
		"\u0000\u097a\u097b\u0007\u0010\u0000\u0000\u097b\u018f\u0001\u0000\u0000"+
		"\u0000\u097c\u097d\u0005\u001a\u0000\u0000\u097d\u097f\u0003\u01b4\u00da"+
		"\u0000\u097e\u0980\u0003\u00e8t\u0000\u097f\u097e\u0001\u0000\u0000\u0000"+
		"\u097f\u0980\u0001\u0000\u0000\u0000\u0980\u0982\u0001\u0000\u0000\u0000"+
		"\u0981\u0983\u0003\u00ecv\u0000\u0982\u0981\u0001\u0000\u0000\u0000\u0982"+
		"\u0983\u0001\u0000\u0000\u0000\u0983\u0985\u0001\u0000\u0000\u0000\u0984"+
		"\u0986\u0003\u00f0x\u0000\u0985\u0984\u0001\u0000\u0000\u0000\u0985\u0986"+
		"\u0001\u0000\u0000\u0000\u0986\u0987\u0001\u0000\u0000\u0000\u0987\u0989"+
		"\u0003\u00fc~\u0000\u0988\u098a\u0005\u0086\u0000\u0000\u0989\u0988\u0001"+
		"\u0000\u0000\u0000\u0989\u098a\u0001\u0000\u0000\u0000\u098a\u0191\u0001"+
		"\u0000\u0000\u0000\u098b\u098d\u0007\u0011\u0000\u0000\u098c\u098b\u0001"+
		"\u0000\u0000\u0000\u098c\u098d\u0001\u0000\u0000\u0000\u098d\u098e\u0001"+
		"\u0000\u0000\u0000\u098e\u098f\u0005\\\u0000\u0000\u098f\u0991\u0003\u01b4"+
		"\u00da\u0000\u0990\u0992\u0003\u00e8t\u0000\u0991\u0990\u0001\u0000\u0000"+
		"\u0000\u0991\u0992\u0001\u0000\u0000\u0000\u0992\u0994\u0001\u0000\u0000"+
		"\u0000\u0993\u0995\u0003\u013c\u009e\u0000\u0994\u0993\u0001\u0000\u0000"+
		"\u0000\u0994\u0995\u0001\u0000\u0000\u0000\u0995\u0997\u0001\u0000\u0000"+
		"\u0000\u0996\u0998\u0003\u00f0x\u0000\u0997\u0996\u0001\u0000\u0000\u0000"+
		"\u0997\u0998\u0001\u0000\u0000\u0000\u0998\u0999\u0001\u0000\u0000\u0000"+
		"\u0999\u099b\u0003\u013e\u009f\u0000\u099a\u099c\u0005\u0086\u0000\u0000"+
		"\u099b\u099a\u0001\u0000\u0000\u0000\u099b\u099c\u0001\u0000\u0000\u0000"+
		"\u099c\u0193\u0001\u0000\u0000\u0000\u099d\u099e\u00058\u0000\u0000\u099e"+
		"\u09a0\u0003\u01b4\u00da\u0000\u099f\u09a1\u0003\u0148\u00a4\u0000\u09a0"+
		"\u099f\u0001\u0000\u0000\u0000\u09a0\u09a1\u0001\u0000\u0000\u0000\u09a1"+
		"\u09a3\u0001\u0000\u0000\u0000\u09a2\u09a4\u0003\u014e\u00a7\u0000\u09a3"+
		"\u09a2\u0001\u0000\u0000\u0000\u09a3\u09a4\u0001\u0000\u0000\u0000\u09a4"+
		"\u09a6\u0001\u0000\u0000\u0000\u09a5\u09a7\u0003\u00f0x\u0000\u09a6\u09a5"+
		"\u0001\u0000\u0000\u0000\u09a6\u09a7\u0001\u0000\u0000\u0000\u09a7\u09a8"+
		"\u0001\u0000\u0000\u0000\u09a8\u09aa\u0003\u00fc~\u0000\u09a9\u09ab\u0005"+
		"\u0086\u0000\u0000\u09aa\u09a9\u0001\u0000\u0000\u0000\u09aa\u09ab\u0001"+
		"\u0000\u0000\u0000\u09ab\u0195\u0001\u0000\u0000\u0000\u09ac\u09ad\u0005"+
		"%\u0000\u0000\u09ad\u09af\u0003\u01b4\u00da\u0000\u09ae\u09b0\u0003\u0156"+
		"\u00ab\u0000\u09af\u09ae\u0001\u0000\u0000\u0000\u09af\u09b0\u0001\u0000"+
		"\u0000\u0000\u09b0\u09b1\u0001\u0000\u0000\u0000\u09b1\u09b3\u0003\u0158"+
		"\u00ac\u0000\u09b2\u09b4\u0005\u0086\u0000\u0000\u09b3\u09b2\u0001\u0000"+
		"\u0000\u0000\u09b3\u09b4\u0001\u0000\u0000\u0000\u09b4\u0197\u0001\u0000"+
		"\u0000\u0000\u09b5\u09b6\u0005\u001f\u0000\u0000\u09b6\u09b7\u0003\u0114"+
		"\u008a\u0000\u09b7\u09b9\u0003\u01b4\u00da\u0000\u09b8\u09ba\u0003\u0148"+
		"\u00a4\u0000\u09b9\u09b8\u0001\u0000\u0000\u0000\u09b9\u09ba\u0001\u0000"+
		"\u0000\u0000\u09ba\u09bb\u0001\u0000\u0000\u0000\u09bb\u09bd\u0005\u0081"+
		"\u0000\u0000\u09bc\u09be\u0003\u011a\u008d\u0000\u09bd\u09bc\u0001\u0000"+
		"\u0000\u0000\u09bd\u09be\u0001\u0000\u0000\u0000\u09be\u09bf\u0001\u0000"+
		"\u0000\u0000\u09bf\u09c1\u0005\u0082\u0000\u0000\u09c0\u09c2\u0003\u00f0"+
		"x\u0000\u09c1\u09c0\u0001\u0000\u0000\u0000\u09c1\u09c2\u0001\u0000\u0000"+
		"\u0000\u09c2\u09c3\u0001\u0000\u0000\u0000\u09c3\u09c4\u0005\u0086\u0000"+
		"\u0000\u09c4\u0199\u0001\u0000\u0000\u0000\u09c5\u09c6\u0005\'\u0000\u0000"+
		"\u09c6\u09cf\u0003\u0004\u0002\u0000\u09c7\u09c8\u0003\u010e\u0087\u0000"+
		"\u09c8\u09c9\u0005\u0086\u0000\u0000\u09c9\u09d0\u0001\u0000\u0000\u0000"+
		"\u09ca\u09cb\u0003\u0116\u008b\u0000\u09cb\u09cc\u0005}\u0000\u0000\u09cc"+
		"\u09cd\u0003\u012e\u0097\u0000\u09cd\u09ce\u0005~\u0000\u0000\u09ce\u09d0"+
		"\u0001\u0000\u0000\u0000\u09cf\u09c7\u0001\u0000\u0000\u0000\u09cf\u09ca"+
		"\u0001\u0000\u0000\u0000\u09d0\u019b\u0001\u0000\u0000\u0000\u09d1\u09d2"+
		"\u0003\u010e\u0087\u0000\u09d2\u09d3\u0005\u0086\u0000\u0000\u09d3\u019d"+
		"\u0001\u0000\u0000\u0000\u09d4\u09e2\u0003\u0116\u008b\u0000\u09d5\u09d6"+
		"\u0005}\u0000\u0000\u09d6\u09d7\u0003\u0124\u0092\u0000\u09d7\u09dc\u0005"+
		"~\u0000\u0000\u09d8\u09d9\u0005\u0091\u0000\u0000\u09d9\u09da\u0003\u0112"+
		"\u0089\u0000\u09da\u09db\u0005\u0086\u0000\u0000\u09db\u09dd\u0001\u0000"+
		"\u0000\u0000\u09dc\u09d8\u0001\u0000\u0000\u0000\u09dc\u09dd\u0001\u0000"+
		"\u0000\u0000\u09dd\u09e3\u0001\u0000\u0000\u0000\u09de\u09df\u0003\u0178"+
		"\u00bc\u0000\u09df\u09e0\u0003L&\u0000\u09e0\u09e1\u0005\u0086\u0000\u0000"+
		"\u09e1\u09e3\u0001\u0000\u0000\u0000\u09e2\u09d5\u0001\u0000\u0000\u0000"+
		"\u09e2\u09de\u0001\u0000\u0000\u0000\u09e3\u019f\u0001\u0000\u0000\u0000"+
		"\u09e4\u09e5\u0005\u001b\u0000\u0000\u09e5\u09e6\u0003\u0004\u0002\u0000"+
		"\u09e6\u09e7\u0003\u010a\u0085\u0000\u09e7\u09e8\u0005\u0086\u0000\u0000"+
		"\u09e8\u01a1\u0001\u0000\u0000\u0000\u09e9\u09ea\u0005^\u0000\u0000\u09ea"+
		"\u09eb\u0005\u007f\u0000\u0000\u09eb\u09ec\u0003\u011a\u008d\u0000\u09ec"+
		"\u09f5\u0005\u0080\u0000\u0000\u09ed\u09ee\u0005}\u0000\u0000\u09ee\u09ef"+
		"\u0003\u0124\u0092\u0000\u09ef\u09f0\u0005~\u0000\u0000\u09f0\u09f6\u0001"+
		"\u0000\u0000\u0000\u09f1\u09f2\u0003\u0178\u00bc\u0000\u09f2\u09f3\u0003"+
		"L&\u0000\u09f3\u09f4\u0005\u0086\u0000\u0000\u09f4\u09f6\u0001\u0000\u0000"+
		"\u0000\u09f5\u09ed\u0001\u0000\u0000\u0000\u09f5\u09f1\u0001\u0000\u0000"+
		"\u0000\u09f6\u01a3\u0001\u0000\u0000\u0000\u09f7\u09f8\u0005\u0090\u0000"+
		"\u0000\u09f8\u09f9\u0003\u01b4\u00da\u0000\u09f9\u09fa\u0005\u0081\u0000"+
		"\u0000\u09fa\u09fb\u0005\u0082\u0000\u0000\u09fb\u09fc\u0003\u013a\u009d"+
		"\u0000\u09fc\u01a5\u0001\u0000\u0000\u0000\u09fd\u09fe\u0003\u01b4\u00da"+
		"\u0000\u09fe\u0a00\u0005\u0081\u0000\u0000\u09ff\u0a01\u0003\u011a\u008d"+
		"\u0000\u0a00\u09ff\u0001\u0000\u0000\u0000\u0a00\u0a01\u0001\u0000\u0000"+
		"\u0000\u0a01\u0a02\u0001\u0000\u0000\u0000\u0a02\u0a04\u0005\u0082\u0000"+
		"\u0000\u0a03\u0a05\u0003\u0138\u009c\u0000\u0a04\u0a03\u0001\u0000\u0000"+
		"\u0000\u0a04\u0a05\u0001\u0000\u0000\u0000\u0a05\u0a06\u0001\u0000\u0000"+
		"\u0000\u0a06\u0a07\u0003\u013a\u009d\u0000\u0a07\u01a7\u0001\u0000\u0000"+
		"\u0000\u0a08\u0a0a\u0003\u01aa\u00d5\u0000\u0a09\u0a0b\u0003\u00e8t\u0000"+
		"\u0a0a\u0a09\u0001\u0000\u0000\u0000\u0a0a\u0a0b\u0001\u0000\u0000\u0000"+
		"\u0a0b\u0a0c\u0001\u0000\u0000\u0000\u0a0c\u0a0e\u0005\u0081\u0000\u0000"+
		"\u0a0d\u0a0f\u0003\u011a\u008d\u0000\u0a0e\u0a0d\u0001\u0000\u0000\u0000"+
		"\u0a0e\u0a0f\u0001\u0000\u0000\u0000\u0a0f\u0a10\u0001\u0000\u0000\u0000"+
		"\u0a10\u0a12\u0005\u0082\u0000\u0000\u0a11\u0a13\u0003\u00f0x\u0000\u0a12"+
		"\u0a11\u0001\u0000\u0000\u0000\u0a12\u0a13\u0001\u0000\u0000\u0000\u0a13"+
		"\u0a19\u0001\u0000\u0000\u0000\u0a14\u0a1a\u0003\u0118\u008c\u0000\u0a15"+
		"\u0a16\u0003\u0178\u00bc\u0000\u0a16\u0a17\u0003L&\u0000\u0a17\u0a18\u0005"+
		"\u0086\u0000\u0000\u0a18\u0a1a\u0001\u0000\u0000\u0000\u0a19\u0a14\u0001"+
		"\u0000\u0000\u0000\u0a19\u0a15\u0001\u0000\u0000\u0000\u0a1a\u01a9\u0001"+
		"\u0000\u0000\u0000\u0a1b\u0a21\u0003\u01b4\u00da\u0000\u0a1c\u0a1d\u0003"+
		"\u01b4\u00da\u0000\u0a1d\u0a1e\u0005\u0095\u0000\u0000\u0a1e\u0a1f\u0003"+
		"\u01b4\u00da\u0000\u0a1f\u0a21\u0001\u0000\u0000\u0000\u0a20\u0a1b\u0001"+
		"\u0000\u0000\u0000\u0a20\u0a1c\u0001\u0000\u0000\u0000\u0a21\u0a29\u0001"+
		"\u0000\u0000\u0000\u0a22\u0a24\u0003\u0016\u000b\u0000\u0a23\u0a22\u0001"+
		"\u0000\u0000\u0000\u0a23\u0a24\u0001\u0000\u0000\u0000\u0a24\u0a25\u0001"+
		"\u0000\u0000\u0000\u0a25\u0a26\u0005\u0083\u0000\u0000\u0a26\u0a28\u0003"+
		"\u01b4\u00da\u0000\u0a27\u0a23\u0001\u0000\u0000\u0000\u0a28\u0a2b\u0001"+
		"\u0000\u0000\u0000\u0a29\u0a27\u0001\u0000\u0000\u0000\u0a29\u0a2a\u0001"+
		"\u0000\u0000\u0000\u0a2a\u01ab\u0001\u0000\u0000\u0000\u0a2b\u0a29\u0001"+
		"\u0000\u0000\u0000\u0a2c\u0a2d\u0005F\u0000\u0000\u0a2d\u0a2e\u0003\u0134"+
		"\u009a\u0000\u0a2e\u0a30\u0005\u0081\u0000\u0000\u0a2f\u0a31\u00056\u0000"+
		"\u0000\u0a30\u0a2f\u0001\u0000\u0000\u0000\u0a30\u0a31\u0001\u0000\u0000"+
		"\u0000\u0a31\u0a32\u0001\u0000\u0000\u0000\u0a32\u0a38\u0003\u01ae\u00d7"+
		"\u0000\u0a33\u0a35\u0005\u0084\u0000\u0000\u0a34\u0a36\u00056\u0000\u0000"+
		"\u0a35\u0a34\u0001\u0000\u0000\u0000\u0a35\u0a36\u0001\u0000\u0000\u0000"+
		"\u0a36\u0a37\u0001\u0000\u0000\u0000\u0a37\u0a39\u0003\u01ae\u00d7\u0000"+
		"\u0a38\u0a33\u0001\u0000\u0000\u0000\u0a38\u0a39\u0001\u0000\u0000\u0000"+
		"\u0a39\u0a3a\u0001\u0000\u0000\u0000\u0a3a\u0a40\u0005\u0082\u0000\u0000"+
		"\u0a3b\u0a41\u0003\u013a\u009d\u0000\u0a3c\u0a3d\u0003\u0178\u00bc\u0000"+
		"\u0a3d\u0a3e\u0003L&\u0000\u0a3e\u0a3f\u0005\u0086\u0000\u0000\u0a3f\u0a41"+
		"\u0001\u0000\u0000\u0000\u0a40\u0a3b\u0001\u0000\u0000\u0000\u0a40\u0a3c"+
		"\u0001\u0000\u0000\u0000\u0a41\u01ad\u0001\u0000\u0000\u0000\u0a42\u0a43"+
		"\u0003\u0004\u0002\u0000\u0a43\u0a46\u0003\u01b4\u00da\u0000\u0a44\u0a45"+
		"\u0005\u0091\u0000\u0000\u0a45\u0a47\u0003\u001c\u000e\u0000\u0a46\u0a44"+
		"\u0001\u0000\u0000\u0000\u0a46\u0a47\u0001\u0000\u0000\u0000\u0a47\u01af"+
		"\u0001\u0000\u0000\u0000\u0a48\u0a4a\u0005\u0081\u0000\u0000\u0a49\u0a4b"+
		"\u0003\u0018\f\u0000\u0a4a\u0a49\u0001\u0000\u0000\u0000\u0a4a\u0a4b\u0001"+
		"\u0000\u0000\u0000\u0a4b\u0a4c\u0001\u0000\u0000\u0000\u0a4c\u0a4d\u0005"+
		"\u0082\u0000\u0000\u0a4d\u01b1\u0001\u0000\u0000\u0000\u0a4e\u0a50\u0005"+
		"\u0081\u0000\u0000\u0a4f\u0a51\u0003\u0018\f\u0000\u0a50\u0a4f\u0001\u0000"+
		"\u0000\u0000\u0a50\u0a51\u0001\u0000\u0000\u0000\u0a51\u0a52\u0001\u0000"+
		"\u0000\u0000\u0a52\u0a54\u0005\u0082\u0000\u0000\u0a53\u0a55\u0003Z-\u0000"+
		"\u0a54\u0a53\u0001\u0000\u0000\u0000\u0a54\u0a55\u0001\u0000\u0000\u0000"+
		"\u0a55\u01b3\u0001\u0000\u0000\u0000\u0a56\u0a57\u0007\u0012\u0000\u0000"+
		"\u0a57\u01b5\u0001\u0000\u0000\u0000\u0159\u01b7\u01ba\u01bd\u01c2\u01c6"+
		"\u01cc\u01cf\u01d4\u01d8\u01df\u01e1\u01e9\u01f1\u01f7\u01fb\u0200\u020a"+
		"\u0212\u021c\u0222\u0225\u022a\u022d\u0233\u0238\u0242\u024f\u0257\u025d"+
		"\u025f\u0266\u026e\u0276\u027e\u0286\u028e\u0298\u029a\u02a0\u02a5\u02ad"+
		"\u02b5\u02bd\u02bf\u02c2\u02c9\u02ce\u02d5\u02d9\u02db\u02f3\u02fc\u0301"+
		"\u0305\u030d\u0310\u0315\u0319\u031d\u0323\u0331\u0337\u0343\u0347\u034c"+
		"\u0350\u0356\u035e\u0367\u0379\u037c\u0381\u0384\u0393\u0399\u039d\u03a3"+
		"\u03a8\u03ab\u03b3\u03bb\u03c6\u03cb\u03d0\u03d2\u03db\u03e3\u03ea\u03f2"+
		"\u03f6\u03ff\u0404\u0406\u040f\u0417\u041b\u0420\u0422\u0427\u042b\u0432"+
		"\u043a\u043c\u0440\u0443\u0446\u044e\u0458\u0469\u0470\u0474\u047e\u0483"+
		"\u048a\u0493\u0498\u049f\u04ab\u04b6\u04be\u04c3\u04cc\u04d5\u04de\u04e4"+
		"\u04e9\u04ed\u04f1\u04f5\u04f9\u0500\u0508\u0515\u051f\u0535\u0539\u053d"+
		"\u0542\u0556\u055b\u0560\u0567\u056a\u0580\u058c\u0590\u0598\u05a2\u05a9"+
		"\u05ad\u05b2\u05b5\u05ba\u05c2\u05c7\u05ce\u05d4\u05dc\u05e4\u05e7\u05ee"+
		"\u05f5\u05f9\u05fc\u0602\u0606\u060c\u061a\u0620\u0627\u062c\u062f\u0632"+
		"\u0639\u0643\u0654\u0659\u065d\u0660\u0663\u066a\u0670\u0678\u067e\u0688"+
		"\u0690\u0696\u06a1\u06a5\u06a7\u06ac\u06b0\u06b7\u06c0\u06c7\u06ca\u06cd"+
		"\u06d1\u06d6\u06e3\u06ed\u06f4\u0700\u0707\u0713\u0719\u071d\u0721\u0727"+
		"\u072d\u072f\u0736\u073a\u073d\u0741\u074b\u074e\u0755\u0758\u075d\u0762"+
		"\u0764\u0767\u076a\u0770\u0773\u077f\u0783\u0786\u0790\u0793\u0799\u07b4"+
		"\u07c1\u07c7\u07d0\u07d6\u07d9\u07e1\u07e5\u07eb\u07f1\u07f7\u0802\u0806"+
		"\u0808\u0812\u0818\u081b\u0828\u082e\u0831\u0834\u083b\u0840\u0844\u0848"+
		"\u0859\u085c\u0861\u0865\u0869\u0872\u0875\u087a\u087e\u0883\u0887\u0889"+
		"\u0894\u0898\u089a\u089f\u08a4\u08ab\u08b1\u08b6\u08bc\u08c0\u08c6\u08cd"+
		"\u08d7\u08da\u08dd\u08e2\u08e8\u08ec\u08ee\u08f5\u08fc\u0904\u0908\u0917"+
		"\u091b\u0924\u0928\u092c\u0942\u094a\u0950\u0959\u0962\u0968\u096f\u0976"+
		"\u0978\u097f\u0982\u0985\u0989\u098c\u0991\u0994\u0997\u099b\u09a0\u09a3"+
		"\u09a6\u09aa\u09af\u09b3\u09b9\u09bd\u09c1\u09cf\u09dc\u09e2\u09f5\u0a00"+
		"\u0a04\u0a0a\u0a0e\u0a12\u0a19\u0a20\u0a23\u0a29\u0a30\u0a35\u0a38\u0a40"+
		"\u0a46\u0a4a\u0a50\u0a54";
	public static final String _serializedATN = Utils.join(
		new String[] {
			_serializedATNSegment0,
			_serializedATNSegment1
		},
		""
	);
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}