package org.refactoringminer.astDiff.matchers;

public class Constants {
	// PSI node type labels
	public static final String ASSIGNMENT = "PsiAssignmentExpression";
	public static final String METHOD_INVOCATION = "PsiMethodCallExpression";
	public static final String METHOD_DECLARATION = "PsiMethod";
	public static final String TRY_STATEMENT = "PsiTryStatement";
	public static final String CATCH_CLAUSE = "PsiCatchSection";
	public static final String BLOCK = "PsiBlockStatement";
	/**
	 * 	VARIABLE_DECLARATION_FRAGMENT does not exist in PSI
	 * 	The following methods should be adapted in the intellij-psi branch
	 * 	ProjectASTDiffer#findAttributeAccessModifier(Tree)
	 * 	ProjectASTDiffer#findAttributeModifierByLabel(Tree, String)
	 *  ProjectASTDiffer#findAttributeTreeByType(Tree, String)
	 */
	public static final String VARIABLE_DECLARATION_FRAGMENT = "PsiLocalVariable";
	public static final String FIELD_DECLARATION = "PsiField";
	public static final String ACCESS_MODIFIER = "PsiModifier";
	public static final String PACKAGE_DECLARATION = "PsiPackageStatement";
	public static final String ANONYMOUS_CLASS_DECLARATION = "PsiAnonymousClass";
	public static final String LABELED_STATEMENT = "PsiLabeledStatement";
	public static final String SIMPLE_NAME = "PsiIdentifier";
	public static final String VARIABLE_DECLARATION_STATEMENT = "PsiLocalVariable";
	public static final String EXPRESSION_STATEMENT = "PsiExpressionStatement";
	public static final String MODIFIER = "PsiModifier";
	public static final String IMPORT_DECLARATION = "PsiImportStatement";
	public static final String PRIMITIVE_TYPE = "PsiPrimitiveType";
	/**
	 * 	See {@link com.intellij.psi.PsiClass#isInterface}.
	 */
	public static final String TYPE_DECLARATION = "PsiClass";
	/**
	 * 	See {@link com.intellij.psi.PsiClass#isEnum}.
	 */
	public static final String ENUM_DECLARATION = "PsiClass";
	public static final String ENUM_CONSTANT_DECLARATION = "PsiEnumConstant";

	// PSI node property labels
	/**
	 *  AssignmentOperator in JDT:
	 *  <pre>{@code
	 *     = ASSIGN
	 *     += PLUS_ASSIGN
	 *     -= MINUS_ASSIGN
	 *     *= TIMES_ASSIGN
	 *     /= DIVIDE_ASSIGN
	 *     &= BIT_AND_ASSIGN
	 *     |= BIT_OR_ASSIGN
	 *     ^= BIT_XOR_ASSIGN
	 *     %= REMAINDER_ASSIGN
	 *     <<= LEFT_SHIFT_ASSIGN
	 *     >>= RIGHT_SHIFT_SIGNED_ASSIGN
	 *     >>>= RIGHT_SHIFT_UNSIGNED_ASSIGN
	 *  }</pre>
	 * 	See {@link com.intellij.psi.JavaTokenType#EQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#PLUSEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#MINUSEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#ASTERISKEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#DIVEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#ANDEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#OREQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#XOREQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#PERCEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#LTLTEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#GTGTEQ}.
	 * 	See {@link com.intellij.psi.JavaTokenType#GTGTGTEQ}.
	 */
	public static final String ASSIGNMENT_OPERATOR = "PsiJavaToken";
	/**
	 *  TYPE_DECLARATION_KIND does not exist as a property in PSI
	 * 	See {@link com.intellij.psi.PsiClass#isEnum}.
	 * 	See {@link com.intellij.psi.PsiClass#isInterface}.
	 * 	See {@link com.intellij.psi.PsiClass#isAnnotationType()}.
	 */
	public static final String TYPE_DECLARATION_KIND = "TYPE_DECLARATION_KIND";

	// Keyword labels
	public static final String TRANSIENT = "transient";
	public static final String VOLATILE = "volatile";
	public static final String SYNCHRONIZED = "synchronized";
	public static final String ABSTRACT = "abstract";
	public static final String FINAL = "final";
	public static final String STATIC = "static";
	public static final String EQUAL_OPERATOR = "=";

}
