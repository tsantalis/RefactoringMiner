package org.refactoringminer.astDiff.utils;

import org.refactoringminer.util.PathFileUtils;

public class Constants {
	public static boolean isCrossLanguage(Constants LANG1, Constants LANG2) {
		return !LANG1.TYPE_DECLARATION.equals(LANG2.TYPE_DECLARATION);
	}

	public Constants(String filePath) {
		if(PathFileUtils.isPythonFile(filePath)) {
			CLASS_BLOCK = "block";
			METHOD_DECLARATION = "function_definition";
			SIMPLE_NAME = "identifier";
			IMPORT_DECLARATION = "import_statement";
			TYPE_DECLARATION = "class_definition";
			TYPE_DECLARATION_KIND = "class";
			BLOCK_COMMENT = "string"; // this is a text-block comment style, Python does not support Java-like multi-line comment
			LINE_COMMENT = "line_comment"; // TODO validate when Python comments get supported
			EXPRESSION_STATEMENT = "expression_statement";
			TRY_STATEMENT = "try_statement";
			CATCH_CLAUSE = "except_clause";
			IF_STATEMENT = "if_statement";
			WHILE_STATEMENT = "while_statement";
			FOR_STATEMENT = "for_statement";
			ENHANCED_FOR_STATEMENT = "for_statement";
			PACKAGE_DECLARATION = "";
			FIELD_DECLARATION = "";
			MODIFIER = "";
			INITIALIZER = "";
			CONSTRUCTOR_INVOCATION = "";
			ENUM_DECLARATION = "class_definition";
			ANNOTATION_TYPE_DECLARATION = "class_definition";
			RECORD_DECLARATION = "class_definition";
			PREFIX_EXPRESSION = "prefix_expression";
			METHOD_INVOCATION_ARGUMENTS = "argument_list";
			STRING_LITERAL = "string";
			BOOLEAN_LITERAL = "boolean_literal"; // True, False labels //TODO introduce List of values
			METHOD_INVOCATION = "call";
		}
		else if(PathFileUtils.isKotlinFile(filePath)) {
			CLASS_BLOCK = "type_body";
			METHOD_DECLARATION = "function_declaration";
			SIMPLE_NAME = "simple_identifier";
			IMPORT_DECLARATION = "import_header";
			TYPE_DECLARATION = "class_declaration";
			TYPE_DECLARATION_KIND = "type_keyword";
			BLOCK_COMMENT = "multiline_comment";
			LINE_COMMENT = "line_comment";
			EXPRESSION_STATEMENT = "expression_statement"; // TODO update value
			TRY_STATEMENT = "try_expression";
			CATCH_CLAUSE = "catch_block";
			IF_STATEMENT = "if_expression";
			WHILE_STATEMENT = "while_statement";
			FOR_STATEMENT = "for_statement";
			ENHANCED_FOR_STATEMENT = "for_statement";
			PACKAGE_DECLARATION = "package_header";
			FIELD_DECLARATION = "property_declaration";
			MODIFIER = "visibility_modifier";
			INITIALIZER = "anonymous_initializer";
			CONSTRUCTOR_INVOCATION = "constructor_invocation";
			ENUM_DECLARATION = "class_declaration";
			ANNOTATION_TYPE_DECLARATION = "class_declaration";
			RECORD_DECLARATION = "class_declaration";
			PREFIX_EXPRESSION = "prefix_expression";
			METHOD_INVOCATION_ARGUMENTS = "value_arguments";
			STRING_LITERAL = "string_literal";
			BOOLEAN_LITERAL = "boolean_literal";
			METHOD_INVOCATION = "call_expression";
		}
		else {
			// Java values as default
			CLASS_BLOCK = "";
			METHOD_DECLARATION = "MethodDeclaration";
			SIMPLE_NAME = "SimpleName";
			IMPORT_DECLARATION = "ImportDeclaration";
			TYPE_DECLARATION = "TypeDeclaration";
			TYPE_DECLARATION_KIND = "TYPE_DECLARATION_KIND";
			BLOCK_COMMENT = "BlockComment";
			LINE_COMMENT = "LineComment";
			EXPRESSION_STATEMENT = "ExpressionStatement";
			TRY_STATEMENT = "TryStatement";
			CATCH_CLAUSE = "CatchClause";
			IF_STATEMENT = "IfStatement";
			WHILE_STATEMENT = "WhileStatement";
			FOR_STATEMENT = "ForStatement";
			ENHANCED_FOR_STATEMENT = "EnhancedForStatement";
			PACKAGE_DECLARATION = "PackageDeclaration";
			FIELD_DECLARATION = "FieldDeclaration";
			MODIFIER = "Modifier";
			INITIALIZER = "Initializer";
			CONSTRUCTOR_INVOCATION = "ConstructorInvocation";
			ENUM_DECLARATION = "EnumDeclaration";
			ANNOTATION_TYPE_DECLARATION = "AnnotationTypeDeclaration";
			RECORD_DECLARATION = "RecordDeclaration";
			PREFIX_EXPRESSION = "PrefixExpression";
			METHOD_INVOCATION_ARGUMENTS = "METHOD_INVOCATION_ARGUMENTS";
			STRING_LITERAL = "StringLiteral";
			BOOLEAN_LITERAL = "BooleanLiteral";
			METHOD_INVOCATION = "MethodInvocation";
		}
	}

    // AST node type labels
    public final String ASSIGNMENT = "Assignment";
    public final String METHOD_INVOCATION;
    public final String METHOD_DECLARATION;
    public final String ANNOTATION_TYPE_MEMBER_DECLARATION = "AnnotationTypeMemberDeclaration";
    public final String TRY_STATEMENT;
    public final String CATCH_CLAUSE;
    public final String BLOCK = "Block";
    public final String VARIABLE_DECLARATION_FRAGMENT = "VariableDeclarationFragment";
    public final String FIELD_DECLARATION;
    public final String ACCESS_MODIFIER = "AccessModifier";
    public final String PACKAGE_DECLARATION;
    public final String MODULE_DECLARATION = "ModuleDeclaration";
    public final String ANONYMOUS_CLASS_DECLARATION = "AnonymousClassDeclaration";
    public final String LABELED_STATEMENT = "LabeledStatement";
    public final String SIMPLE_NAME;
    public final String VARIABLE_DECLARATION_STATEMENT = "VariableDeclarationStatement";
    public final String EXPRESSION_STATEMENT;
    public final String MODIFIER;
    public final String IMPORT_DECLARATION;
    public final String PRIMITIVE_TYPE = "PrimitiveType";
    public final String TYPE_DECLARATION;
    public final String ENUM_DECLARATION;
    public final String RECORD_DECLARATION;
    public final String ANNOTATION_TYPE_DECLARATION;
    public final String ENUM_CONSTANT_DECLARATION = "EnumConstantDeclaration";
    public final String TYPE_DECLARATION_STATEMENT = "TypeDeclarationStatement";
    public final String RECORD_COMPONENT = "SingleVariableDeclaration";
    public final String SINGLE_VARIABLE_DECLARATION = "SingleVariableDeclaration";

    // AST node property labels
    public final String ASSIGNMENT_OPERATOR = "ASSIGNMENT_OPERATOR";
    public final String TYPE_DECLARATION_KIND;

    // Keyword labels
    public final String TRANSIENT = "transient";
    public final String VOLATILE = "volatile";
    public final String SYNCHRONIZED = "synchronized";
    public final String ABSTRACT = "abstract";
    public final String NATIVE = "native";
    public final String FINAL = "final";
    public final String STATIC = "static";
    public final String SEALED = "sealed";
    public final String DEFAULT = "default";
    public final String STRICTFP = "strictfp";
    public final String INLINE = "inline";
    public final String OVERRIDE = "override";
    public final String SUSPEND = "suspend";
    public final String EQUAL_OPERATOR = "=";
    public final String ANNOTATION = "annotation";
    public final String ENUM = "enum";
    public final String OPEN = "open";
    public final String OPERATOR = "operator";
    public final String DATA = "data";
    public final String INTERNAL = "internal";
    public final String PRIVATE = "private";
    public final String COMPANION = "companion";
    public final String INFIX = "infix";
    public final String INNER = "inner";

    public final String METHOD_INVOCATION_ARGUMENTS;
    public final String METHOD_INVOCATION_RECEIVER = "METHOD_INVOCATION_RECEIVER";

    public final String ASSERT_STATEMENT = "AssertStatement";
    public final String BREAK_STATEMENT = "BreakStatement";
    public final String CONSTRUCTOR_INVOCATION;
    public final String CONTINUE_STATEMENT = "ContinueStatement";
    public final String DO_STATEMENT = "DoStatement";
    public final String EMPTY_STATEMENT = "EmptyStatement";
    public final String ENHANCED_FOR_STATEMENT;
    public final String FOR_STATEMENT;
    public final String IF_STATEMENT;
    public final String RETURN_STATEMENT = "ReturnStatement";
    public final String SUPER_CONSTRUCTOR_INVOCATION = "SuperConstructorInvocation";
    public final String SWITCH_CASE = "SwitchCase";
    public final String SWITCH_STATEMENT = "SwitchStatement";
    public final String SYNCHRONIZED_STATEMENT = "SynchronizedStatement";
    public final String THROW_STATEMENT = "ThrowStatement";
    public final String WHILE_STATEMENT;
    public final String CONDITIONAL_EXPRESSION = "ConditionalExpression";
    public final String CAST_EXPRESSION = "CastExpression";

    public final String INFIX_EXPRESSION = "InfixExpression";
    public final String LAMBDA_EXPRESSION = "LambdaExpression";
    public final String INFIX_EXPRESSION_OPERATOR = "INFIX_EXPRESSION_OPERATOR";

    public final String STRING_LITERAL;
    public final String NUMBER_LITERAL = "NumberLiteral";
    public final String BOOLEAN_LITERAL;

    public final String SINGLE_MEMBER_ANNOTATION = "SingleMemberAnnotation"; //@type(Expression), for instance
    public final String MARKER_ANNOTATION = "MarkerAnnotation"; //@Deprecated for instance
    public final String NORMAL_ANNOTATION = "NormalAnnotation"; //@Author("John Doe", "") for instance

    public final String COMPILATION_UNIT = "CompilationUnit";

    public final String JAVA_DOC = "Javadoc";
    public final String TEXT_ELEMENT = "TextElement";
    public final String TAG_ELEMENT = "TagElement";
    public final String TAG_NAME = "TAG_NAME";

    public final String SIMPLE_TYPE = "SimpleType";
    public final String EXPRESSION_METHOD_REFERENCE = "ExpressionMethodReference";
    public final String PREFIX_EXPRESSION;
    public final String INITIALIZER;
    public final String QUALIFIED_NAME = "QualifiedName";
    public final String CLASS_INSTANCE_CREATION = "ClassInstanceCreation";

    public final String LINE_COMMENT;
    public final String BLOCK_COMMENT;

    public final String TYPE_INHERITANCE_KEYWORD = "TYPE_INHERITANCE_KEYWORD";
    public final String PERMITS_KEYWORD = "PERMITS_KEYWORD";
    public final String THROWS_KEYWORD = "THROWS_KEYWORD";


    //Python Specific
    public final String ARGUMENT_LIST = "argument_list";
    public final String CLASS_BLOCK;// = "block"; // Pouria: Might be merged with Block, I have no clue about python
    public final String CLASS_KEYWORD = "class";
    public final String TRY_KEYWORD = "try";
    public final String EXCEPT_KEYWORD = "except";
    public final String ATTRIBUTE_EXCEPTION = "attribute";
    public final String FINALLY_CLAUSE = "finally_clause";
    public final String FINALLY_KEYWORD = "finally";
    public final String MODULE = "module"; // This is the root of all trees in Python
    public final String DECORATED_METHOD = "decorated_definition";
    public final String IMPORT_FROM_STATEMENT = "import_from_statement";
    public final String FUTURE_IMPORT_STATEMENT = "future_import_statement";
    public final String RELATIVE_IMPORT = "relative_import";
    public final String FROM_KEYWORD = "from";
    public final String LINE_CONTINUATION = "line_continuation";
    public final String FUTURE = "__future__";
    public final String RELATIVE_IMPORT_DOTTED_NAME = "dotted_name";
    public final String ELSE_IF = "elif_clause";
    public final String ELSE = "else_clause";
    public final String ELIF_KEYWORD = "elif";
    public final String ELSE_KEYWORD = "else";
    public final String WHILE_KEYWORD = "while";
    
    //Kotlin Specific
    public final String FUNCTION_BODY = "function_body";
    public final String SOURCE_FILE = "source_file"; // This is the root of all trees in Kotlin
    public final String TYPE_IDENTIFIER = "type_identifier";
    public final String FUNCTION_KEYWORD = "function_keyword";
    public final String CONSTRUCTOR_KEYWORD = "constructor_keyword";
    public final String FUNCTION_PARAMETERS = "function_value_parameters";
    public final String STATEMENTS = "statements"; // This is a node that wraps each leaf statement in Kotlin
    public final String IMPORT_LIST = "import_list";
    public final String MODIFIERS = "modifiers"; // This is a node that wraps all modifiers
    public final String TYPE_PARAMETERS = "type_parameters";
    public final String INIT_KEYWORD = "initializer_keyword";
    public final String SECONDARY_CONSTRUCTOR = "secondary_constructor";
    public final String PRIMARY_CONSTRUCTOR = "primary_constructor";
    public final String DELEGATION_SPECIFIER = "delegation_specifier";
    public final String AFFECTATION_OPERATOR = "affectation_operator";
    public final String COMPANION_OBJECT = "companion_object";
    public final String OBJECT_DECLARATION = "object_declaration";
    public final String OBJECT_LITERAL = "object_literal";
    public final String WHEN_ENTRY = "when_entry";
    public final String ARROW = "arrow";
    public final String WHEN_EXPRESSION = "when_expression";
    public final String WHEN_SUBJECT = "when_subject";
    public final String COLLECTION_ITERATED = "collection_iterated"; // in keyword in for loop
    public final String PARAMETER_MODIFIERS = "parameter_modifiers";
    public final String PARAMETER = "parameter";
    public final String GETTER = "getter";
    public final String SETTER = "setter";
    public final String PROPERTY_DECLARATION_KEYWORD = "property_declaration_keyword";
    public final String IMPORT_IDENTIFIER = "identifier";
    public final String CONTROL_STRUCTURE_BODY = "control_structure_body";
    public final String CLASS_PARAMETER = "class_parameter";
    public final String USER_TYPE = "user_type";
    public final String ERROR = "ERROR";
    public final String ENUM_ENTRY = "enum_entry";
    public final String JUMP_EXPRESSION = "jump_expression";
    public final String JUMP_KEYWORD = "jump_keyword";
    public final String CALL_SUFFIX = "call_suffix";
    public final String NAVIGATION_EXPRESSION = "navigation_expression";
    public final String NAVIGATION_SUFFIX = "navigation_suffix";
    public final String INTEGER_LITERAL = "integer_literal";
    public final String VARIABLE_DECLARATION = "variable_declaration";
    public final String CLASS_MODIFIER = "class_modifier";
    public final String AS_EXPRESSION = "as_expression";
    public final String ANNOTATED_LAMBDA = "annotated_lambda";
    public final String ARITHMETIC_OPERATOR = "arithmetic_operator";
}
