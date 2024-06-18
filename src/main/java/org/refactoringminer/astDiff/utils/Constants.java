package org.refactoringminer.astDiff.utils;

public class Constants {

	// AST node type labels
	public static final String ASSIGNMENT = "Assignment";
	public static final String METHOD_INVOCATION = "MethodInvocation";
	public static final String METHOD_DECLARATION = "MethodDeclaration";
	public static final String ANNOTATION_TYPE_MEMBER_DECLARATION = "AnnotationTypeMemberDeclaration";
	public static final String TRY_STATEMENT = "TryStatement";
	public static final String CATCH_CLAUSE = "CatchClause";
	public static final String BLOCK = "Block";
	public static final String VARIABLE_DECLARATION_FRAGMENT = "VariableDeclarationFragment";
	public static final String FIELD_DECLARATION = "FieldDeclaration";
	public static final String ACCESS_MODIFIER = "AccessModifier";
	public static final String PACKAGE_DECLARATION = "PackageDeclaration";
	public static final String ANONYMOUS_CLASS_DECLARATION = "AnonymousClassDeclaration";
	public static final String LABELED_STATEMENT = "LabeledStatement";
	public static final String SIMPLE_NAME = "SimpleName";
	public static final String VARIABLE_DECLARATION_STATEMENT = "VariableDeclarationStatement";
	public static final String EXPRESSION_STATEMENT = "ExpressionStatement";
	public static final String MODIFIER = "Modifier";
	public static final String IMPORT_DECLARATION = "ImportDeclaration";
	public static final String PRIMITIVE_TYPE = "PrimitiveType";
	public static final String TYPE_DECLARATION = "TypeDeclaration";
	public static final String ENUM_DECLARATION = "EnumDeclaration";
	public static final String RECORD_DECLARATION = "RecordDeclaration";
	public static final String ANNOTATION_TYPE_DECLARATION = "AnnotationTypeDeclaration";
	public static final String ENUM_CONSTANT_DECLARATION = "EnumConstantDeclaration";
	public static final String TYPE_DECLARATION_STATEMENT = "TypeDeclarationStatement";
	public static final String RECORD_COMPONENT = "SingleVariableDeclaration";

	// AST node property labels
	public static final String ASSIGNMENT_OPERATOR = "ASSIGNMENT_OPERATOR";
	public static final String TYPE_DECLARATION_KIND = "TYPE_DECLARATION_KIND";

	// Keyword labels
	public static final String TRANSIENT = "transient";
	public static final String VOLATILE = "volatile";
	public static final String SYNCHRONIZED = "synchronized";
	public static final String ABSTRACT = "abstract";
	public static final String NATIVE = "native";
	public static final String FINAL = "final";
	public static final String STATIC = "static";
	public static final String DEFAULT = "default";
	public static final String EQUAL_OPERATOR = "=";

	public static final String METHOD_INVOCATION_ARGUMENTS = "METHOD_INVOCATION_ARGUMENTS";
	public static final String METHOD_INVOCATION_RECEIVER = "METHOD_INVOCATION_RECEIVER";

	public static final String ASSERT_STATEMENT = "AssertStatement";
	public static final String BREAK_STATEMENT = "BreakStatement";
	public static final String CONSTRUCTOR_INVOCATION = "ConstructorInvocation";
	public static final String CONTINUE_STATEMENT = "ContinueStatement";
	public static final String DO_STATEMENT = "DoStatement";
	public static final String EMPTY_STATEMENT = "EmptyStatement";
	public static final String ENHANCED_FOR_STATEMENT = "EnhancedForStatement";
	public static final String FOR_STATEMENT = "ForStatement";
	public static final String IF_STATEMENT = "IfStatement";
	public static final String RETURN_STATEMENT = "ReturnStatement";
	public static final String SUPER_CONSTRUCTOR_INVOCATION = "SuperConstructorInvocation";
	public static final String SWITCH_CASE = "SwitchCase";
	public static final String SWITCH_STATEMENT = "SwitchStatement";
	public static final String SYNCHRONIZED_STATEMENT = "SynchronizedStatement";
	public static final String THROW_STATEMENT = "ThrowStatement";
	public static final String WHILE_STATEMENT = "WhileStatement";
	public static final String CONDITIONAL_EXPRESSION = "ConditionalExpression";

	public static final String INFIX_EXPRESSION = "InfixExpression";
	public static final String INFIX_EXPRESSION_OPERATOR = "INFIX_EXPRESSION_OPERATOR";

	public static final String STRING_LITERAL = "StringLiteral";
	public static final String NUMBER_LITERAL = "NumberLiteral";
	public static final String BOOLEAN_LITERAL = "BooleanLiteral";

	public static final String SINGLE_MEMBER_ANNOTATION = "SingleMemberAnnotation";
	public static final String MARKER_ANNOTATION = "MarkerAnnotation";

	public static final String COMPILATION_UNIT = "CompilationUnit";

	public static final String JAVA_DOC = "Javadoc";
	public static final String TEXT_ELEMENT = "TextElement";
	public static final String TAG_ELEMENT = "TagElement";

	public static final String SIMPLE_TYPE = "SimpleType";
	public static final String EXPRESSION_METHOD_REFERENCE = "ExpressionMethodReference";
	public static final String PREFIX_EXPRESSION = "PrefixExpression";
	public static final String INITIALIZER = "Initializer";
	public static final String QUALIFIED_NAME = "QualifiedName";
	public static final String CLASS_INSTANCE_CREATION = "ClassInstanceCreation";
}
