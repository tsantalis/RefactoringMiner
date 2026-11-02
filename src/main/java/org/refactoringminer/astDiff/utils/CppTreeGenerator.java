package org.refactoringminer.astDiff.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gr.uom.java.xmi.CppFileProcessor;
import gr.uom.java.xmi.decomposition.CppVisitor;
import gr.uom.java.xmi.CppLanguage;

import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTInitializerList;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorElseStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorEndifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfdefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorFunctionStyleMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroExpansion;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemHolder;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTAliasDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCapture;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConversionName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDecltypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeleteExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDesignatedInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDesignator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTExplicitTemplateInstantiation;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFieldDesignator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFieldReference;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTIfStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTInitCapture;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLambdaExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLinkageSpecification;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNameSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTOperatorName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTPackExpansionExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTPointerToMember;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTReferenceOperator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeConstructorExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTStaticAssertDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTStructuredBindingDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateSpecialization;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTypeId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDirective;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

/**
 * Generates the AST diff tree of a C or C++ file from the Eclipse CDT AST, which is also used to generate the RefactoringMiner model,
 * with the node types and the structure of the tree-sitter-cpp trees, which were used before, so that the AST diff matchers and the diffs remain the same.
 * The keywords and the punctuation, which are leaves of the tree-sitter trees, are the tokens between the children of each node.
 * The comments and the preprocessor directives are placed like the extras of tree-sitter, i.e., in the smallest node containing the tokens before and after them,
 * and the declarations and statements between the conditional directives are grouped in preproc_if, preproc_ifdef, preproc_elif and preproc_else nodes.
 * The code that CDT does not parse, i.e., inactive preprocessor branches and code with problems, and the code containing macro expansions,
 * which tree-sitter parses without expanding the macros, is parsed again with CDT without macros.
 * When it parses code again, the names that tree-sitter parses as templates (f<T>(x)) or as called functions (f(x);) are declared before the code,
 * so that CDT does not parse them as comparisons or declarations, the macros that are not valid without their definitions (T absl_nonnull x) are empty,
 * and the return type of the macros with a body (TEST(A, B) {...}) is int, whose offsets are not in the tree.
 * The ambiguities that CDT resolves with the names it knows remain, i.e., T x(y); is a declaration of a variable with an argument list,
 * which tree-sitter parses as a declaration of a function with a parameter.
 */
public class CppTreeGenerator {
	private static final String INLINE = "";
	private static final String[] PUNCTUATION = {"...", "<=>", "<<=", ">>=", "->*", "::", "->", "++", "--", "<<", ">>", "<=", ">=", "==", "!=", "&&", "||",
			"+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", ".*", "##"};
	private static final Set<String> KEYWORDS = Set.of("alignas", "alignof", "asm", "auto", "bool", "break", "case", "catch", "char", "char8_t", "char16_t", "char32_t",
			"class", "concept", "const", "consteval", "constexpr", "constinit", "const_cast", "continue", "co_await", "co_return", "co_yield", "decltype", "default", "delete",
			"do", "double", "dynamic_cast", "else", "enum", "explicit", "export", "extern", "false", "final", "float", "for", "friend", "goto", "if", "inline", "int", "long",
			"mutable", "namespace", "new", "noexcept", "nullptr", "operator", "override", "private", "protected", "public", "register", "reinterpret_cast", "requires", "return",
			"short", "signed", "sizeof", "static", "static_assert", "static_cast", "struct", "switch", "template", "this", "thread_local", "throw", "true", "try", "typedef",
			"typeid", "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "wchar_t", "while", "__restrict", "__restrict__", "_Atomic", "restrict",
			"__attribute__", "__declspec", "__asm__", "__asm", "__inline", "__inline__", "__forceinline", "offsetof");
	private static final Set<String> PRIMITIVE_TYPES = Set.of("bool", "char", "int", "float", "double", "void", "size_t", "ssize_t", "ptrdiff_t", "intptr_t", "uintptr_t",
			"charptr_t", "nullptr_t", "max_align_t", "int8_t", "int16_t", "int32_t", "int64_t", "uint8_t", "uint16_t", "uint32_t", "uint64_t",
			"char8_t", "char16_t", "char32_t", "char64_t");
	private static final Set<String> SIZED_MODIFIERS = Set.of("signed", "unsigned", "long", "short");
	private static final Set<String> STORAGE_CLASSES = Set.of("static", "extern", "inline", "thread_local", "register", "__inline", "__inline__", "__forceinline");
	private static final Set<String> TYPE_QUALIFIERS = Set.of("const", "volatile", "constexpr", "constinit", "consteval", "mutable", "restrict", "__restrict", "__restrict__",
			"_Atomic", "__extension__", "noexcept");

	public static TreeContext generate(String content, IASTTranslationUnit translationUnit) {
		TreeContext context = new TreeContext();
		if(content.isEmpty()) {
			Tree root = context.createTree(TypeSet.type("empty_root"));
			root.setPos(0);
			root.setLength(0);
			context.setRoot(root);
			return context;
		}
		CppTreeGenerator generator = new CppTreeGenerator(content);
		Node root = generator.translationUnit(translationUnit);
		generator.addDirectives(root);
		generator.groupConditionals(root);
		generator.ungroupedDirectives(root);
		generator.addComments(root);
		context.setRoot(generator.toTree(root, context));
		return context;
	}

	//the names used as types in the code between the offsets, i.e., ns::T and A in ns::T<A> x = e;, and whether they are templates,
	//which the model declares before parsing the file again, like the code parsed again for the tree
	public static Map<String, Boolean> templateTypes(String content, List<int[]> ranges) {
		CppTreeGenerator generator = new CppTreeGenerator(content);
		Map<String, Boolean> names = new LinkedHashMap<>();
		for(int[] range : ranges) {
			Set<String> types = new LinkedHashSet<>();
			for(Map.Entry<String, Boolean> entry : generator.templateNames(range[0], range[1], types).entrySet()) {
				if(entry.getValue()) {
					names.put(entry.getKey(), true);
				}
			}
			for(String type : types) {
				names.putIfAbsent(type, false);
			}
		}
		return names;
	}

	private static class Node {
		private final String type;
		private int start = -1;
		private int end = -1;
		private Node parent;
		private final List<Node> children = new ArrayList<>();
		private boolean leaf;

		private Node(String type) {
			this.type = type;
		}

		private Node add(Node child) {
			if(child == null) {
				return this;
			}
			if(child.type.equals(INLINE)) {
				for(Node grandChild : new ArrayList<>(child.children)) {
					add(grandChild);
				}
			}
			else {
				child.parent = this;
				children.add(child);
			}
			return this;
		}

		private int depth() {
			int depth = 0;
			for(Node node = parent; node != null; node = node.parent) {
				depth++;
			}
			return depth;
		}

		private Node first() {
			return children.isEmpty() ? null : children.get(0);
		}

		private Node last() {
			return children.isEmpty() ? null : children.get(children.size() - 1);
		}

		@Override
		public String toString() {
			return type + "[" + start + "," + end + ")";
		}
	}

	//a CDT translation unit with the offset of its file content in the file, which is not 0 for the code parsed again
	private static class Source {
		private final IASTTranslationUnit translationUnit;
		private final int delta;
		private final boolean reparsed;
		//the names declared as functions in the code parsed again
		private Set<String> functions = Set.of();
		//the offsets and lengths of the text inserted in the code parsed again, i.e., int in int TEST(A, B) {...}
		private List<int[]> insertions = List.of();
		//the start and end offsets in the file of the template arguments of the member templates, which are int in the code parsed again
		private Map<Integer, Integer> maskedArguments = Map.of();
		//the start and end offsets of the macro expansions
		private final int[][] expansions;

		private Source(IASTTranslationUnit translationUnit, int delta, boolean reparsed) {
			this.translationUnit = translationUnit;
			this.delta = delta;
			this.reparsed = reparsed;
			List<int[]> list = new ArrayList<>();
			for(IASTPreprocessorMacroExpansion expansion : translationUnit.getMacroExpansions()) {
				IASTFileLocation location = expansion.getFileLocation();
				if(location != null && expansion.isPartOfTranslationUnitFile()) {
					list.add(new int[] {offset(location.getNodeOffset()), offset(location.getNodeOffset() + location.getNodeLength())});
				}
			}
			this.expansions = list.toArray(new int[0][]);
		}

		//the offset in the file of the offset in the parsed code
		private int offset(int offset) {
			int shift = 0;
			for(int[] insertion : insertions) {
				if(offset >= insertion[0] + insertion[1]) {
					shift += insertion[1];
				}
				else if(offset > insertion[0]) {
					shift += offset - insertion[0];
				}
			}
			return offset - shift + delta;
		}

		private boolean expands(int start, int end) {
			for(int[] expansion : expansions) {
				if(expansion[0] < end && expansion[1] > start) {
					return true;
				}
			}
			return false;
		}
	}

	private static final String INSERTED_TYPE = "int ";
	//the function whose arguments are parsed again, which is declared so that CDT does not parse its call as a declaration
	private static final String CALL = "__rm_call__";
	private static final String INSERTED_CLASS = "__rm_type__";

	private enum Context {TOP, CLASS, BLOCK}

	private enum NameKind {IDENTIFIER, TYPE, FIELD, NAMESPACE, LABEL}

	private final String content;
	private final int length;
	//the start and end offsets of the comments
	private final TreeMap<Integer, Integer> comments = new TreeMap<>();
	//the start and end offsets of the preprocessor directives, without the line break
	private final TreeMap<Integer, Integer> directives = new TreeMap<>();
	//the start of the #if, #ifdef or #ifndef of each #endif
	private final Map<Integer, Integer> conditionalStarts = new HashMap<>();
	//the comments and the directives
	private final TreeMap<Integer, Integer> extras = new TreeMap<>();
	private Source source;
	//the macros of the file expanded to nothing, i.e., LLAMA_API in #define LLAMA_API, which the code parsed again does not know
	private Set<String> emptyMacros = Set.of();

	private CppTreeGenerator(String content) {
		this.content = content;
		this.length = content.length();
		scan();
	}

	//lexical scan

	private boolean isLineBreak(int offset) {
		return offset < length && (content.charAt(offset) == '\n' || content.charAt(offset) == '\r' && offset + 1 < length && content.charAt(offset + 1) == '\n');
	}

	private int afterLineBreak(int offset) {
		return content.charAt(offset) == '\r' ? offset + 2 : offset + 1;
	}

	private void scan() {
		int offset = 0;
		boolean lineStart = true;
		while(offset < length) {
			char character = content.charAt(offset);
			if(character == '\n') {
				lineStart = true;
				offset++;
			}
			else if(character == ' ' || character == '\t' || character == '\r' || character == '\f' || character == 0x0B || character == '﻿') {
				offset++;
			}
			else if(character == '\\' && isLineBreak(offset + 1)) {
				offset = afterLineBreak(offset + 1);
			}
			else if(content.startsWith("//", offset) || content.startsWith("/*", offset)) {
				int end = commentEnd(offset);
				comments.put(offset, end);
				offset = end;
			}
			else if(character == '#' && lineStart) {
				int end = directiveEnd(offset);
				directives.put(offset, end);
				offset = end;
				lineStart = false;
			}
			else {
				lineStart = false;
				offset = tokenEnd(offset, length);
			}
		}
		extras.putAll(comments);
		extras.putAll(directives);
	}

	private int commentEnd(int offset) {
		if(content.startsWith("/*", offset)) {
			int end = content.indexOf("*/", offset + 2);
			return end < 0 ? length : end + 2;
		}
		int end = offset + 2;
		while(end < length && content.charAt(end) != '\n') {
			if(content.charAt(end) == '\\' && end + 1 < length) {
				end += isLineBreak(end + 1) ? (content.charAt(end + 1) == '\r' ? 3 : 2) : 2;
			}
			else {
				end++;
			}
		}
		return Math.min(end, length);
	}

	//the end of the directive line, before the line break, with the comments in it
	private int directiveEnd(int offset) {
		int end = offset + 1;
		while(end < length) {
			char character = content.charAt(end);
			if(character == '\n') {
				break;
			}
			if(character == '\\' && isLineBreak(end + 1)) {
				end = afterLineBreak(end + 1);
			}
			else if(content.startsWith("//", end) || content.startsWith("/*", end)) {
				int commentEnd = commentEnd(end);
				comments.put(end, commentEnd);
				end = commentEnd;
			}
			else if(character == '"') {
				int close = end + 1;
				while(close < length && content.charAt(close) != '"' && content.charAt(close) != '\n') {
					close += content.charAt(close) == '\\' ? 2 : 1;
				}
				end = close < length && content.charAt(close) == '"' ? close + 1 : end + 1;
			}
			else {
				end++;
			}
		}
		//the carriage return of the line break
		if(end > offset && end < length && content.charAt(end - 1) == '\r') {
			end--;
		}
		return Math.min(end, length);
	}

	//tokens

	private int skip(int offset, int limit) {
		offset = Math.max(offset, 0);
		while(offset < limit) {
			char character = content.charAt(offset);
			Integer extraEnd = extras.get(offset);
			if(extraEnd != null) {
				offset = extraEnd;
			}
			else if(character == '\\' && isLineBreak(offset + 1)) {
				offset = afterLineBreak(offset + 1);
			}
			else if(Character.isWhitespace(character) || character == '﻿' || character == ' ') {
				offset++;
			}
			else {
				break;
			}
		}
		return offset;
	}

	private static boolean isIdentifierStart(char character) {
		return Character.isLetter(character) || character == '_' || character == '$' || character > 0x7F && !Character.isWhitespace(character);
	}

	private static boolean isIdentifierPart(char character) {
		return Character.isLetterOrDigit(character) || character == '_' || character == '$' || character > 0x7F && !Character.isWhitespace(character);
	}

	//the length of the string or character literal prefix at the offset, or -1
	private int literalPrefix(int offset, int limit) {
		for(String prefix : new String[] {"u8R\"", "LR\"", "uR\"", "UR\"", "R\"", "u8\"", "u8'", "L\"", "L'", "u\"", "u'", "U\"", "U'", "\"", "'"}) {
			if(content.startsWith(prefix, offset) && offset + prefix.length() <= limit) {
				return prefix.length() - 1;
			}
		}
		return -1;
	}

	//the end of the token starting at the offset
	private int tokenEnd(int offset, int limit) {
		char character = content.charAt(offset);
		int prefix = literalPrefix(offset, limit);
		if(prefix >= 0) {
			return literalEnd(offset, offset + prefix, limit);
		}
		if(isIdentifierStart(character)) {
			int end = offset + 1;
			while(end < limit && isIdentifierPart(content.charAt(end))) {
				end++;
			}
			return end;
		}
		if(Character.isDigit(character) || character == '.' && offset + 1 < limit && Character.isDigit(content.charAt(offset + 1))) {
			int end = offset + 1;
			while(end < limit) {
				char c = content.charAt(end);
				if((c == '+' || c == '-') && "eEpP".indexOf(content.charAt(end - 1)) >= 0 && !content.startsWith("0x", offset) && !content.startsWith("0X", offset)
						|| (c == '+' || c == '-') && "pP".indexOf(content.charAt(end - 1)) >= 0) {
					end++;
				}
				else if(Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '\'' && end + 1 < limit && Character.isLetterOrDigit(content.charAt(end + 1))) {
					end++;
				}
				else {
					break;
				}
			}
			return end;
		}
		for(String punctuation : PUNCTUATION) {
			if(content.startsWith(punctuation, offset) && offset + punctuation.length() <= limit) {
				return offset + punctuation.length();
			}
		}
		return offset + 1;
	}

	//the end of the string or character literal whose quote is at the offset
	private int literalEnd(int start, int quote, int limit) {
		char delimiter = content.charAt(quote);
		if(quote > start && content.charAt(quote - 1) == 'R') {
			int open = content.indexOf('(', quote);
			if(open > 0) {
				String close = ")" + content.substring(quote + 1, open) + "\"";
				int end = content.indexOf(close, open);
				if(end >= 0) {
					return Math.min(end + close.length(), Math.max(limit, end + close.length()));
				}
			}
		}
		int end = quote + 1;
		while(end < length) {
			char character = content.charAt(end);
			if(character == delimiter) {
				return end + 1;
			}
			if(character == '\n') {
				return end;
			}
			end += character == '\\' && end + 1 < length ? 2 : 1;
		}
		return end;
	}

	//the tokens between the offsets as leaves
	private List<Node> tokens(int start, int end) {
		List<Node> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			tokens.add(token(offset, tokenEnd));
			offset = skip(tokenEnd, end);
		}
		return tokens;
	}

	//a token as a leaf, with its text as type, except for the identifiers, numbers, booleans and literals
	private Node token(int start, int end) {
		String text = content.substring(start, end);
		char first = text.charAt(0);
		if(literalPrefix(start, end) >= 0) {
			return text.endsWith("'") && !text.startsWith("R") ? charLiteral(start, end) : stringLiteral(start, end);
		}
		if(isIdentifierStart(first) && !KEYWORDS.contains(text)) {
			return leaf("identifier", start, end);
		}
		if(Character.isDigit(first) || first == '.' && text.length() > 1 && Character.isDigit(text.charAt(1))) {
			return leaf("number_literal", start, end);
		}
		if(text.equals("true") || text.equals("false")) {
			return leaf("boolean_literal", start, end);
		}
		return leaf(text, start, end);
	}

	//the offset of the first token with the text, or -1
	private int find(String text, int start, int end) {
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			if(content.startsWith(text, offset) && tokenEnd - offset == text.length()) {
				return offset;
			}
			offset = skip(tokenEnd, end);
		}
		return -1;
	}

	//the offset of the last token with the text before the end, or -1
	private int findLast(String text, int start, int end) {
		int found = -1;
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			if(content.startsWith(text, offset) && tokenEnd - offset == text.length()) {
				found = offset;
			}
			offset = skip(tokenEnd, end);
		}
		return found;
	}

	//the end of the last token before the offset
	private int previousTokenEnd(int start, int offset) {
		int end = start;
		int position = skip(start, offset);
		while(position < offset) {
			int tokenEnd = Math.min(tokenEnd(position, offset), offset);
			end = tokenEnd;
			position = skip(tokenEnd, offset);
		}
		return end;
	}

	private int nextTokenStart(int offset) {
		return skip(offset, length);
	}

	private String tokenAt(int offset) {
		if(offset < 0 || offset >= length) {
			return "";
		}
		return content.substring(offset, Math.min(tokenEnd(offset, length), length));
	}

	//nodes

	private Node leaf(String type, int start, int end) {
		Node node = new Node(type);
		node.start = start;
		node.end = end;
		node.leaf = true;
		return node;
	}

	private static Node inline(List<Node> nodes) {
		Node node = new Node(INLINE);
		for(Node child : nodes) {
			if(child != null) {
				if(child.type.equals(INLINE)) {
					node.children.addAll(child.children);
				}
				else {
					node.children.add(child);
				}
			}
		}
		for(Node child : node.children) {
			if(node.start < 0 || child.start < node.start) {
				node.start = child.start;
			}
			node.end = Math.max(node.end, child.end);
		}
		return node;
	}

	private static Node inline(Node... nodes) {
		return inline(Arrays.asList(nodes));
	}

	//a node with the range, the children and the tokens between them
	private Node node(String type, int start, int end, List<Node> children) {
		Node node = new Node(type);
		List<Node> sorted = new ArrayList<>();
		for(Node child : children) {
			if(child == null) {
				continue;
			}
			if(child.type.equals(INLINE)) {
				for(Node grandChild : child.children) {
					sorted.add(grandChild);
				}
			}
			else {
				sorted.add(child);
			}
		}
		sorted.sort(Comparator.comparingInt((Node n) -> n.start).thenComparingInt(n -> n.end));
		//an offset that was not found
		if(start < 0) {
			start = sorted.isEmpty() ? Math.max(end, 0) : sorted.get(0).start;
		}
		if(end < start) {
			end = sorted.isEmpty() ? start : Math.max(start, sorted.get(sorted.size() - 1).end);
		}
		node.start = start;
		node.end = end;
		int offset = start;
		for(Node child : sorted) {
			if(child.start > offset) {
				for(Node token : tokens(offset, child.start)) {
					node.add(token);
				}
			}
			node.add(child);
			offset = Math.max(offset, child.end);
		}
		if(end > offset) {
			for(Node token : tokens(offset, end)) {
				node.add(token);
			}
		}
		//the attributes in the tokens, i.e., [[noreturn]] in [[noreturn]] static void f() {...}, since [[ only starts an attribute
		List<Node> withAttributes = attributeTokens(node.children);
		if(withAttributes.size() < node.children.size()) {
			node.children.clear();
			for(Node child : withAttributes) {
				node.add(child);
			}
			//the attributes of a declaration, which CDT excludes from the location of a declaration statement,
			//i.e., [[maybe_unused]] in if(x) [[maybe_unused]] auto y = f();
			for(int i = node.children.size() - 2; i >= 0; i--) {
				Node attribute = node.children.get(i);
				Node declaration = node.children.get(i + 1);
				if(attribute.type.equals("attribute_declaration") && declaration.type.equals("declaration")) {
					node.children.remove(i);
					declaration.children.add(0, attribute);
					attribute.parent = declaration;
					declaration.start = attribute.start;
				}
			}
		}
		return node;
	}

	private Node node(String type, int start, int end, Node... children) {
		return node(type, start, end, Arrays.asList(children));
	}

	private Node node(String type, IASTNode ast, List<Node> children) {
		return node(type, start(ast), end(ast), children);
	}

	private Node node(String type, IASTNode ast, Node... children) {
		return node(type, start(ast), end(ast), Arrays.asList(children));
	}

	private Node wrap(String type, Node child) {
		Node node = new Node(type);
		node.start = child.start;
		node.end = child.end;
		node.add(child);
		return node;
	}

	private int start(IASTNode ast) {
		IASTFileLocation location = ast.getFileLocation();
		return location == null ? -1 : source.offset(location.getNodeOffset());
	}

	private int end(IASTNode ast) {
		IASTFileLocation location = ast.getFileLocation();
		return location == null ? -1 : source.offset(location.getNodeOffset() + location.getNodeLength());
	}

	private boolean hasLocation(IASTNode ast) {
		return ast != null && ast.getFileLocation() != null && ast.isPartOfTranslationUnitFile();
	}

	private String text(IASTNode ast) {
		return content.substring(start(ast), end(ast));
	}

	//wraps the token children with the texts in a node of the type, i.e., const in type_qualifier
	private static void wrapTokens(Node node, Set<String> texts, String type) {
		for(int i = 0; i < node.children.size(); i++) {
			Node child = node.children.get(i);
			if(child.leaf && texts.contains(child.type)) {
				Node wrapper = new Node(type);
				wrapper.start = child.start;
				wrapper.end = child.end;
				wrapper.parent = node;
				child.parent = wrapper;
				wrapper.children.add(child);
				node.children.set(i, wrapper);
			}
		}
	}

	private static void renameTokens(Node node, String text, String type) {
		for(int i = 0; i < node.children.size(); i++) {
			Node child = node.children.get(i);
			if(child.leaf && child.type.equals(text)) {
				Node renamed = new Node(type);
				renamed.start = child.start;
				renamed.end = child.end;
				renamed.leaf = true;
				renamed.parent = node;
				node.children.set(i, renamed);
			}
		}
	}

	//the children between the offsets in a new node of the type
	private static Node group(Node node, String type, int start, int end) {
		Node group = new Node(type);
		group.start = start;
		group.end = end;
		List<Node> children = new ArrayList<>();
		int index = -1;
		for(Node child : node.children) {
			if(child.start >= start && child.end <= end) {
				if(index < 0) {
					index = children.size();
					children.add(group);
				}
				group.children.add(child);
				child.parent = group;
			}
			else {
				children.add(child);
			}
		}
		if(index < 0) {
			int position = 0;
			while(position < children.size() && children.get(position).start < start) {
				position++;
			}
			children.add(position, group);
		}
		node.children.clear();
		node.children.addAll(children);
		group.parent = node;
		return group;
	}

	//the modifier tokens of declarations as tree-sitter nodes
	private static void wrapModifiers(Node node) {
		wrapTokens(node, STORAGE_CLASSES, "storage_class_specifier");
		wrapTokens(node, TYPE_QUALIFIERS, "type_qualifier");
		wrapTokens(node, Set.of("explicit"), "explicit_function_specifier");
	}

	//error

	private Node error(int start, int end) {
		return node("ERROR", start, end, new ArrayList<>());
	}

	//code parsed again

	//whether the node or its code has a problem or contains a macro expansion, so that it must be parsed again
	private boolean reparse(IASTNode ast) {
		if(ast instanceof IASTProblemHolder || ast instanceof IASTProblem) {
			return true;
		}
		if(!hasLocation(ast)) {
			return false;
		}
		if(source.reparsed) {
			return false;
		}
		return source.expands(start(ast), end(ast));
	}

	private boolean problem(IASTNode ast) {
		if(ast instanceof IASTProblemHolder || ast instanceof IASTProblem) {
			return true;
		}
		for(IASTNode child : ast.getChildren()) {
			if(problem(child)) {
				return true;
			}
		}
		return false;
	}

	//the name of the class whose members are parsed again, so that its constructors are parsed
	private String className = "__rm_class__";
	//whether the next statement is converted without parsing it again
	private boolean direct;
	private static final String BLOCK_PREFIX = "void __rm_function__() {\n";

	//the declarations, members or statements parsed again from the code between the offsets without macros
	private List<Node> reparse(int start, int end, Context context) {
		return reparse(start, end, context, Set.of());
	}

	private List<Node> reparse(int start, int end, Context context, Set<String> functions) {
		return reparse(start, end, context, functions, Map.of());
	}

	private List<Node> reparse(int start, int end, Context context, Set<String> functions, Map<Integer, String> extraInsertions) {
		List<Node> nodes = new ArrayList<>();
		if(start >= end) {
			return nodes;
		}
		String prefix = templatePreamble(start, end, functions) + (context == Context.CLASS ? "struct " + className + " {\n" : context == Context.BLOCK ? BLOCK_PREFIX : "");
		String suffix = context == Context.CLASS ? "\n};" : context == Context.BLOCK ? "\n}" : "";
		//the text inserted in the code, which CDT does not parse otherwise
		TreeMap<Integer, String> inserted = new TreeMap<>();
		if(context == Context.TOP) {
			//a function definition without return type, i.e., TEST(A, B) {...} of a macro, has the type int
			for(int declarationStart : untypedFunctionDefinitions(start, end)) {
				inserted.put(declarationStart, INSERTED_TYPE);
			}
		}
		//return {.x = 1}; and a = {.x = 1}; are parsed with a type before the designated initializers
		for(int brace : designatedInitializerLists(start, end)) {
			inserted.put(brace, INSERTED_CLASS);
		}
		inserted.putAll(extraInsertions);
		Map<Integer, Integer> masked = new HashMap<>();
		StringBuilder builder = new StringBuilder(rewrite(start, end, inserted, masked));
		for(int offset : inserted.descendingKeySet()) {
			builder.insert(offset - start, inserted.get(offset));
		}
		List<int[]> insertions = new ArrayList<>();
		int shift = 0;
		for(var entry : inserted.entrySet()) {
			insertions.add(new int[] {prefix.length() + entry.getKey() - start + shift, entry.getValue().length()});
			shift += entry.getValue().length();
		}
		String code = prefix + builder + suffix;
		IASTTranslationUnit translationUnit;
		try {
			translationUnit = CppLanguage.getDefault().getASTTranslationUnit(FileContent.create("__rm_reparsed__.cpp", code.toCharArray()), new ScannerInfo(),
					IncludeFileContentProvider.getEmptyFilesProvider(), EmptyCIndex.INSTANCE, GPPLanguage.OPTION_IS_SOURCE_UNIT, new DefaultLogService());
		}
		catch(Exception e) {
			nodes.add(error(start, end));
			return nodes;
		}
		Source previous = source;
		source = new Source(translationUnit, start - prefix.length(), true);
		source.functions = functions;
		source.insertions = insertions;
		source.maskedArguments = masked;
		try {
			IASTDeclaration[] declarations = withoutPreamble(translationUnit.getDeclarations(false), start);
			if(context == Context.TOP) {
				for(IASTDeclaration declaration : declarations) {
					nodes.add(declaration(declaration, Context.TOP));
				}
				nodes.addAll(inactive(translationUnit, start, end, Context.TOP, declarations));
			}
			else if(declarations.length == 1 && declarations[0] instanceof IASTSimpleDeclaration simple && simple.getDeclSpecifier() instanceof ICPPASTCompositeTypeSpecifier composite) {
				IASTDeclaration[] members = composite.getDeclarations(false);
				//the members in the code, which are grouped like the members of the file, i.e., the problems of DEFINE_PROP(T, x, T{}, E) that CDT split at T{}
				List<IASTDeclaration> membersInCode = new ArrayList<>();
				for(IASTDeclaration member : members) {
					if(start(member) >= start) {
						membersInCode.add(member);
					}
				}
				nodes.addAll(declarations(membersInCode.toArray(new IASTDeclaration[0]), Context.CLASS));
				nodes.addAll(inactive(translationUnit, start, end, Context.CLASS, members));
			}
			else if(declarations.length == 1 && declarations[0] instanceof IASTFunctionDefinition function && function.getBody() instanceof IASTCompoundStatement body) {
				IASTStatement[] statements = body.getStatements();
				for(IASTStatement statement : statements) {
					nodes.add(statement(statement));
				}
				nodes.addAll(inactive(translationUnit, start, end, Context.BLOCK, statements));
			}
			else {
				nodes.clear();
				nodes.add(error(start, end));
			}
		}
		catch(RuntimeException e) {
			nodes.clear();
			nodes.add(error(start, end));
		}
		finally {
			source = previous;
		}
		//the code outside the parsed nodes, i.e., the closing brace of the prefix that closed early
		//the empty problem that CDT gives at the end of the code without a closing brace
		nodes.removeIf(n -> n == null || n.start < start || n.end > end || n.type.equals("ERROR") && n.start == n.end);
		absorbMacros(nodes, start);
		return nodes;
	}

	//the starts of the declarations between the offsets that are a name with parameters followed by a body, i.e., TEST(A, B) {...}
	private List<Integer> untypedFunctionDefinitions(int start, int end) {
		List<Integer> starts = new ArrayList<>();
		List<int[]> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			tokens.add(new int[] {offset, tokenEnd});
			offset = skip(tokenEnd, end);
		}
		int depth = 0;
		for(int i = 0; i < tokens.size(); i++) {
			String token = tokenText(tokens.get(i));
			String previous = i > 0 ? tokenText(tokens.get(i - 1)) : ";";
			if(depth == 0 && (previous.equals(";") || previous.equals("}")) && isIdentifierStart(token.charAt(0)) && !KEYWORDS.contains(token)
					&& i + 1 < tokens.size() && tokenText(tokens.get(i + 1)).equals("(")) {
				int close = -1;
				int nesting = 0;
				for(int k = i + 1; k < tokens.size(); k++) {
					String text = tokenText(tokens.get(k));
					if(text.equals("(")) {
						nesting++;
					}
					else if(text.equals(")") && --nesting == 0) {
						close = k;
						break;
					}
				}
				if(close > 0 && close + 1 < tokens.size() && tokenText(tokens.get(close + 1)).equals("{")) {
					starts.add(tokens.get(i)[0]);
				}
			}
			if(token.equals("{")) {
				depth++;
			}
			else if(token.equals("}")) {
				depth--;
			}
		}
		return starts;
	}

	//the opening braces of the designated initializers after return and =
	private List<Integer> designatedInitializerLists(int start, int end) {
		List<Integer> braces = new ArrayList<>();
		List<int[]> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			tokens.add(new int[] {offset, tokenEnd});
			offset = skip(tokenEnd, end);
		}
		for(int i = 1; i + 2 < tokens.size(); i++) {
			String previous = tokenText(tokens.get(i - 1));
			if(tokenText(tokens.get(i)).equals("{") && (previous.equals("return") || previous.equals("=")) && tokenText(tokens.get(i + 1)).equals(".")
					&& isIdentifierStart(tokenText(tokens.get(i + 2)).charAt(0))) {
				braces.add(tokens.get(i)[0]);
			}
		}
		return braces;
	}

	//whether the code between the offsets is a call followed by a semicolon, i.e., TRACEPOINT_SEMAPHORE(a, b); of a macro
	private boolean isCallStatement(int start, int end) {
		int offset = skip(start, end);
		if(offset >= end || !isIdentifierStart(content.charAt(offset))) {
			return false;
		}
		int position = offset;
		while(position < end) {
			int tokenEnd = Math.min(tokenEnd(position, end), end);
			String token = content.substring(position, tokenEnd);
			if(token.equals("(")) {
				//a call followed by other calls and member accesses, without braces outside the parentheses
				int depth = 0;
				int last = -1;
				int current = position;
				while(current < end) {
					int currentEnd = Math.min(tokenEnd(current, end), end);
					String text = content.substring(current, currentEnd);
					if(text.equals("(") || text.equals("[")) {
						depth++;
					}
					else if(text.equals(")") || text.equals("]")) {
						depth--;
					}
					else if(depth == 0 && (text.equals("{") || text.equals("}") || text.equals("=") || text.equals(";") && skip(currentEnd, end) < end)) {
						return false;
					}
					last = current;
					current = skip(currentEnd, end);
				}
				return last >= 0 && content.charAt(last) == ';';
			}
			if(!isIdentifierStart(token.charAt(0)) && !token.equals("::")) {
				return false;
			}
			position = skip(tokenEnd, end);
		}
		return false;
	}

	//the start of the qualified name that ends at the end, or -1
	private int qualifiedNameStart(int start, int end) {
		List<int[]> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			tokens.add(new int[] {offset, tokenEnd});
			offset = skip(tokenEnd, end);
		}
		int i = tokens.size() - 1;
		if(i < 0 || !isIdentifierStart(tokenText(tokens.get(i)).charAt(0)) || KEYWORDS.contains(tokenText(tokens.get(i)))) {
			return -1;
		}
		while(i >= 2 && tokenText(tokens.get(i - 1)).equals("::") && isIdentifierStart(tokenText(tokens.get(i - 2)).charAt(0))) {
			i -= 2;
		}
		if(i >= 1 && (tokenText(tokens.get(i - 1)).equals(".") || tokenText(tokens.get(i - 1)).equals("->"))) {
			return -1;
		}
		return tokens.get(i)[0];
	}

	//the argument list between the offsets parsed again as the arguments of a call
	private Node reparseArgumentList(int start, int end) {
		Map<Integer, String> insertions = new HashMap<>();
		insertions.put(start, CALL);
		insertions.put(end, ";");
		List<Node> nodes = reparse(start, end, Context.BLOCK, Set.of(CALL), insertions);
		if(nodes.size() == 1 && nodes.get(0).type.equals("expression_statement") && !nodes.get(0).children.isEmpty()) {
			Node call = nodes.get(0).first();
			if(call.type.equals("call_expression") && call.last() != null && call.last().type.equals("argument_list")) {
				return call.last();
			}
		}
		return null;
	}

	//the declarations parsed again, or the call of a macro at the top level as an expression statement like in tree-sitter
	private List<Node> reparseDeclaration(int start, int end, Context context) {
		Node usingEnum = usingEnum(start, end);
		if(usingEnum != null) {
			return List.of(usingEnum);
		}
		int close = macroCallWithBody(start, end);
		if(context == Context.TOP && close > 0) {
			//TEST_CASE("x", "[y]") {...} of a macro is a call followed by a compound statement
			return reparse(start, end, Context.BLOCK, Set.of(), Map.of(close + 1, ";"));
		}
		if(context == Context.TOP && isCallStatement(start, end)) {
			List<Node> nodes = reparse(start, end, Context.BLOCK);
			if(failed(nodes)) {
				Node call = macroCallStatement(start, end);
				if(call != null) {
					return List.of(call);
				}
			}
			return nodes;
		}
		return reparse(start, end, context);
	}

	//using enum A::B; which CDT does not parse
	private Node usingEnum(int start, int end) {
		int using = skip(start, end);
		if(!tokenAt(using).equals("using")) {
			return null;
		}
		int keyword = skip(using + 5, end);
		if(!tokenAt(keyword).equals("enum")) {
			return null;
		}
		int semicolon = findLast(";", keyword, end);
		if(semicolon < 0 || skip(semicolon + 1, end) < end) {
			return null;
		}
		int nameStart = skip(keyword + 4, semicolon);
		int nameEnd = previousTokenEnd(nameStart, semicolon);
		List<int[]> parts = new ArrayList<>();
		int offset = nameStart;
		while(offset < nameEnd) {
			int separator = find("::", offset, nameEnd);
			int partEnd = separator < 0 ? nameEnd : previousTokenEnd(offset, separator);
			parts.add(new int[] {skip(offset, nameEnd), partEnd});
			offset = separator < 0 ? nameEnd : separator + 2;
		}
		if(parts.isEmpty()) {
			return null;
		}
		Node name = leaf("identifier", parts.get(parts.size() - 1)[0], parts.get(parts.size() - 1)[1]);
		for(int i = parts.size() - 2; i >= 0; i--) {
			name = node("qualified_identifier", parts.get(i)[0], name.end, leaf("namespace_identifier", parts.get(i)[0], parts.get(i)[1]), name);
		}
		return node("using_declaration", using, semicolon + 1, name);
	}

	//the offset of the closing parenthesis of a macro call with literal arguments followed by a body that is the code between the offsets, or -1
	private int macroCallWithBody(int start, int end) {
		int offset = skip(start, end);
		if(offset >= end || !isIdentifierStart(content.charAt(offset)) || KEYWORDS.contains(tokenAt(offset))) {
			return -1;
		}
		int open = skip(offset + tokenAt(offset).length(), end);
		if(!tokenAt(open).equals("(")) {
			return -1;
		}
		int close = matching(open, end);
		int brace = skip(close + 1, end);
		if(close >= end || !tokenAt(brace).equals("{") || matching(brace, end) + 1 != previousTokenEnd(brace, end)) {
			return -1;
		}
		for(Node token : tokens(open + 1, close)) {
			if(token.type.equals("string_literal") || token.type.equals("number_literal") || token.type.equals("char_literal") || token.type.equals("raw_string_literal")) {
				return close;
			}
		}
		return -1;
	}

	//a macro call whose arguments are not expressions, i.e., ASSIGN_OR_RETURN(auto x, f()); with each argument parsed again as an expression or as a parameter
	private Node macroCallStatement(int start, int end) {
		return macroCallStatement(start, end, false);
	}

	//the call may have no semicolon when it is a member of a class, i.e., DEFINE_PROP(bool, decorate, true, EFFECT) expanded to a field and methods
	private Node macroCallStatement(int start, int end, boolean optionalSemicolon) {
		int nameStart = skip(start, end);
		if(nameStart >= end || !isIdentifierStart(content.charAt(nameStart))) {
			return null;
		}
		int nameEnd = nameStart + tokenAt(nameStart).length();
		int open = skip(nameEnd, end);
		if(!tokenAt(open).equals("(")) {
			return null;
		}
		int close = matching(open, end);
		int semicolon = skip(close + 1, end);
		boolean withoutSemicolon = optionalSemicolon && close < end && semicolon >= end;
		if(close >= end || !withoutSemicolon && (!tokenAt(semicolon).equals(";") || skip(semicolon + 1, end) < end)) {
			return null;
		}
		List<Node> arguments = new ArrayList<>();
		int argumentStart = open + 1;
		int depth = 0;
		int offset = skip(open + 1, close);
		while(offset <= close) {
			String token = offset < close ? tokenAt(offset) : ",";
			if(token.equals("(") || token.equals("[") || token.equals("{")) {
				depth++;
			}
			else if(token.equals(")") || token.equals("]") || token.equals("}")) {
				depth--;
			}
			if(depth == 0 && token.equals(",") || offset == close) {
				int argumentEnd = previousTokenEnd(argumentStart, Math.min(offset, close));
				int first = skip(argumentStart, argumentEnd);
				if(first < argumentEnd) {
					arguments.add(macroArgument(first, argumentEnd));
				}
				argumentStart = offset + 1;
				if(offset == close) {
					break;
				}
			}
			offset = skip(offset + token.length(), close + 1);
			if(offset > close) {
				offset = close;
			}
		}
		Node call = node("call_expression", nameStart, close + 1, leaf("identifier", nameStart, nameEnd), node("argument_list", open, close + 1, arguments));
		return node("expression_statement", nameStart, withoutSemicolon ? close + 1 : semicolon + 1, call);
	}

	//the argument of a macro call as an expression or as a parameter declaration
	//the declaration whose type is the call of a macro, like the macro_type_specifier of tree-sitter, i.e., typedef GTEST_REMOVE_REFERENCE_AND_CONST_(Container) RawContainer;
	private Node macroTypeDeclaration(int start, int end, Context context) {
		List<Node> children = new ArrayList<>();
		int offset = skip(start, end);
		boolean typedef = tokenAt(offset).equals("typedef");
		if(typedef) {
			children.add(leaf("typedef", offset, offset + "typedef".length()));
			offset = skip(offset + "typedef".length(), end);
		}
		if(offset >= end || !isIdentifierStart(content.charAt(offset))) {
			return null;
		}
		int nameEnd = offset + tokenAt(offset).length();
		int open = skip(nameEnd, end);
		if(open >= end || !tokenAt(open).equals("(")) {
			return null;
		}
		int close = matching(open, end);
		int declarator = skip(close + 1, end);
		if(close >= end || declarator >= end || !isIdentifierStart(content.charAt(declarator))) {
			return null;
		}
		//the specifiers after the parameters of a constructor are not names, i.e., S(S&&) noexcept;
		if(KEYWORDS.contains(tokenAt(declarator)) || KEYWORDS.contains(tokenAt(offset))) {
			return null;
		}
		int declaratorEnd = declarator + tokenAt(declarator).length();
		int semicolon = skip(declaratorEnd, end);
		if(semicolon >= end || !tokenAt(semicolon).equals(";") || skip(semicolon + 1, end) < end) {
			return null;
		}
		List<Node> type = new ArrayList<>();
		for(Node token : tokens(open + 1, close)) {
			type.add(token.type.equals("identifier") ? leaf("type_identifier", token.start, token.end) : token);
		}
		if(type.isEmpty()) {
			return null;
		}
		Node descriptor = node("type_descriptor", type.get(0).start, type.get(type.size() - 1).end, type);
		children.add(node("macro_type_specifier", offset, close + 1, leaf("identifier", offset, nameEnd), leaf("(", open, open + 1), descriptor, leaf(")", close, close + 1)));
		String name = typedef ? "type_identifier" : context == Context.CLASS ? "field_identifier" : "identifier";
		children.add(leaf(name, declarator, declaratorEnd));
		children.add(leaf(";", semicolon, semicolon + 1));
		String kind = typedef ? "type_definition" : context == Context.CLASS ? "field_declaration" : "declaration";
		return node(kind, children.get(0).start, semicolon + 1, children);
	}

	private Node macroArgument(int start, int end) {
		Map<Integer, String> callInsertions = new HashMap<>();
		callInsertions.put(start, CALL + "(");
		callInsertions.put(end, ");");
		List<Node> statements = reparse(start, end, Context.BLOCK, Set.of(CALL), callInsertions);
		if(statements.size() == 1 && statements.get(0).type.equals("expression_statement") && !statements.get(0).children.isEmpty()
				&& statements.get(0).first().type.equals("call_expression")) {
			Node call = statements.get(0).first();
			Node arguments = call.last();
			if(arguments != null && arguments.type.equals("argument_list") && arguments.children.size() == 1 && !arguments.first().type.equals("ERROR")) {
				Node argument = arguments.first();
				argument.parent = null;
				return argument;
			}
		}
		Map<Integer, String> insertions = new HashMap<>();
		insertions.put(start, "void __rm_parameter__(");
		insertions.put(end, ");");
		List<Node> nodes = reparse(start, end, Context.TOP, Set.of(), insertions);
		if(nodes.size() == 1 && nodes.get(0).type.equals("declaration")) {
			for(Node descendant : descendants(nodes.get(0))) {
				if(descendant.type.endsWith("parameter_declaration") && descendant.start >= start && descendant.end <= end) {
					//the macros before the parameter, i.e., LLAMA_API
					List<Node> tokens = tokens(start, descendant.start);
					descendant.children.addAll(0, tokens);
					for(Node token : tokens) {
						token.parent = descendant;
					}
					descendant.start = start;
					descendant.parent = null;
					return descendant;
				}
			}
		}
		return error(start, end);
	}

	private static List<Node> descendants(Node node) {
		List<Node> nodes = new ArrayList<>();
		for(Node child : node.children) {
			nodes.add(child);
			nodes.addAll(descendants(child));
		}
		return nodes;
	}

	//the macros before a declaration, i.e., LLAMA_API in LLAMA_API int f();, are its first children
	private void absorbMacros(List<Node> nodes, int start) {
		int previousEnd = start;
		for(Node node : nodes) {
			if(node.type.equals("declaration") || node.type.equals("function_definition") || node.type.equals("field_declaration")) {
				int from = previousEnd;
				List<Node> tokens = tokens(from, node.start);
				if(!tokens.isEmpty() && tokens.get(0).type.equals("identifier") && tokens.stream().noneMatch(t -> t.type.equals(";") || t.type.equals("}") || t.type.equals("{"))) {
					node.children.addAll(0, tokens);
					for(Node token : tokens) {
						token.parent = node;
					}
					node.start = tokens.get(0).start;
				}
			}
			previousEnd = Math.max(previousEnd, node.end);
		}
	}

	//the declarations of the code parsed again, without the declarations of the preamble
	private IASTDeclaration[] withoutPreamble(IASTDeclaration[] declarations, int start) {
		List<IASTDeclaration> result = new ArrayList<>();
		for(IASTDeclaration declaration : declarations) {
			if(declaration.getFileLocation() != null && end(declaration) > start) {
				result.add(declaration);
			}
		}
		return result.toArray(new IASTDeclaration[0]);
	}

	//the names followed by template arguments, i.e., std::max<T>(x), which tree-sitter parses as templates, but CDT as comparisons when it does not know them
	private static final Set<String> TEMPLATE_FOLLOWERS = Set.of("(", "::", "{", ")", ",", ">", ";", "[", "*", "&", "&&", "...", "=", "]", "}");

	//the declarations of the template names between the offsets
	private String templatePreamble(int start, int end, Set<String> functions) {
		Set<String> types = new LinkedHashSet<>();
		Map<String, Boolean> templates = templateNames(start, end, types);
		Set<String> annotations = annotations(start, end);
		if(templates.isEmpty() && functions.isEmpty() && types.isEmpty() && annotations.isEmpty()) {
			return "";
		}
		StringBuilder preamble = new StringBuilder();
		for(String annotation : annotations) {
			preamble.append("#define ").append(annotation).append('\n');
		}
		for(String type : types) {
			if(templates.containsKey(type)) {
				continue;
			}
			String[] parts = type.split("::");
			int namespaces = 0;
			for(int i = 0; i < parts.length - 1; i++) {
				if(!parts[i].isEmpty()) {
					preamble.append("namespace ").append(parts[i]).append(" { ");
					namespaces++;
				}
			}
			preamble.append("struct ").append(parts[parts.length - 1]).append("; ");
			preamble.append("} ".repeat(namespaces));
		}
		for(String function : functions) {
			String[] parts = function.split("::");
			int namespaces = 0;
			for(int i = 0; i < parts.length - 1; i++) {
				if(!parts[i].isEmpty()) {
					preamble.append("namespace ").append(parts[i]).append(" { ");
					namespaces++;
				}
			}
			preamble.append("int ").append(parts[parts.length - 1]).append("(...); ");
			preamble.append("} ".repeat(namespaces));
		}
		for(var entry : templates.entrySet()) {
			String[] parts = entry.getKey().split("::");
			int namespaces = 0;
			for(int i = 0; i < parts.length - 1; i++) {
				if(!parts[i].isEmpty()) {
					preamble.append("namespace ").append(parts[i]).append(" { ");
					namespaces++;
				}
			}
			String name = parts[parts.length - 1];
			preamble.append(entry.getValue() ? "template<class... __rm_t__> struct " + name + "; " : "template<class... __rm_t__> int " + name + "(...); ");
			preamble.append("} ".repeat(namespaces));
		}
		preamble.append('\n');
		return preamble.toString();
	}

	//the qualified names followed by template arguments, and whether they are types
	private static final Set<String> TYPE_KEYWORDS = Set.of("void", "bool", "char", "int", "float", "double", "long", "short", "unsigned", "signed", "auto", "const",
			"struct", "class", "enum", "union", "static", "inline", "extern", "virtual", "explicit", "constexpr", "typename");

	//the macros that tree-sitter parses as identifiers in declarations, i.e., absl_nonnull in T absl_nonnull x and LLAMA_API in LLAMA_API int f(),
	//which are not valid without the macro definitions
	private Set<String> annotations(int start, int end) {
		Set<String> annotations = new LinkedHashSet<>();
		List<int[]> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			tokens.add(new int[] {offset, tokenEnd});
			offset = skip(tokenEnd, end);
		}
		for(int i = 0; i + 1 < tokens.size(); i++) {
			String token = tokenText(tokens.get(i));
			if(!isPlainIdentifier(token)) {
				continue;
			}
			//a macro of the file expanded to nothing, i.e., LLAMA_API in DEPRECATED(LLAMA_API llama_token f(), "...")
			if(emptyMacros.contains(token)) {
				annotations.add(token);
				continue;
			}
			String previous = i > 0 ? tokenText(tokens.get(i - 1)) : ";";
			String next = tokenText(tokens.get(i + 1));
			String afterNext = i + 2 < tokens.size() ? tokenText(tokens.get(i + 2)) : "";
			//a type, the macro and the declarator, i.e., T absl_nonnull x, where the type does not follow an empty macro of the file, i.e., llama_token in LLAMA_API llama_token f(),
			//and does not end with the parameters of a template, i.e., > in template <typename T> R f()
			boolean afterType = (isPlainIdentifier(previous) && !emptyMacros.contains(previous) || previous.equals(">") && !closesTemplateParameters(tokens, i - 1)
					|| previous.equals("*") || previous.equals("&") || previous.equals("&&"))
					&& isPlainIdentifier(next) && (afterNext.equals(",") || afterNext.equals(")") || afterNext.equals("=") || afterNext.equals(";")
					|| afterNext.equals("(") || afterNext.equals("[") || afterNext.equals("{"));
			//the macro at the start of a declaration, i.e., LLAMA_API int f()
			boolean beforeType = (previous.equals(";") || previous.equals("}") || previous.equals("{") || previous.equals("(") || previous.equals(":") && i > 1 && isAccessSpecifier(tokenText(tokens.get(i - 2))))
					&& (TYPE_KEYWORDS.contains(next) || isPlainIdentifier(next) && (isPlainIdentifier(afterNext) || afterNext.equals("*") || afterNext.equals("&")));
			//a macro between if and its condition, i.e., if UNLIKELY (x)
			boolean afterIf = (previous.equals("if") || previous.equals("while")) && next.equals("(");
			//a macro after the name of a class, i.e., class X FINAL {
			String beforePrevious = i > 1 ? tokenText(tokens.get(i - 2)) : "";
			boolean afterClassName = isPlainIdentifier(previous) && (beforePrevious.equals("class") || beforePrevious.equals("struct") || beforePrevious.equals("union"))
					&& (next.equals("{") || next.equals(":"));
			if(afterType || beforeType || afterIf || afterClassName) {
				annotations.add(token);
			}
			//a macro after the parameters of a function, i.e., f() ABSL_LOCKS_EXCLUDED(mutex) {...}
			else if((previous.equals(")") || previous.equals("const") || previous.equals("override") || previous.equals("noexcept") || previous.equals("final"))
					&& (next.equals("(") || next.equals("{") || next.equals(";"))) {
				int close = i;
				if(next.equals("(")) {
					close = -1;
					int depth = 0;
					for(int k = i + 1; k < tokens.size(); k++) {
						String text = tokenText(tokens.get(k));
						if(text.equals("(")) {
							depth++;
						}
						else if(text.equals(")") && --depth == 0) {
							close = k;
							break;
						}
					}
				}
				String after = close > 0 && close + 1 < tokens.size() ? tokenText(tokens.get(close + 1)) : "";
				if(close > 0 && (after.equals("{") || after.equals(";") || after.equals("=") || after.equals(":") || after.equals("const") || after.equals("override") || isPlainIdentifier(after))
						&& token.chars().anyMatch(Character::isUpperCase) && token.equals(token.toUpperCase())) {
					annotations.add(next.equals("(") ? token + "(...)" : token);
				}
			}
			//a macro with arguments before a declaration, i.e., ABSL_DEPRECATED("...") static int f();
			else if(next.equals("(") && (previous.equals(";") || previous.equals("}") || previous.equals("{") || previous.equals(":") && i > 1 && isAccessSpecifier(tokenText(tokens.get(i - 2))))) {
				int close = -1;
				int depth = 0;
				for(int k = i + 1; k < tokens.size(); k++) {
					String text = tokenText(tokens.get(k));
					if(text.equals("(")) {
						depth++;
					}
					else if(text.equals(")") && --depth == 0) {
						close = k;
						break;
					}
				}
				if(close > 0 && close + 2 < tokens.size()) {
					String first = tokenText(tokens.get(close + 1));
					String second = tokenText(tokens.get(close + 2));
					if(TYPE_KEYWORDS.contains(first) && !first.equals("const") || isPlainIdentifier(first) && (isPlainIdentifier(second) || second.equals("*") || second.equals("&") || second.equals("::") || second.equals("<"))) {
						annotations.add(token + "(...)");
					}
				}
			}
		}
		return annotations;
	}

	//whether the > at the index closes the parameters of a template, i.e., template <typename T>
	private boolean closesTemplateParameters(List<int[]> tokens, int index) {
		int depth = 0;
		for(int k = index; k >= 0; k--) {
			String text = tokenText(tokens.get(k));
			if(text.equals(">")) {
				depth++;
			}
			else if(text.equals(">>")) {
				depth += 2;
			}
			else if(text.equals("<") && --depth == 0) {
				return k > 0 && tokenText(tokens.get(k - 1)).equals("template");
			}
			else if(text.equals(";") || text.equals("{") || text.equals("}")) {
				return false;
			}
		}
		return false;
	}

	private static boolean isAccessSpecifier(String token) {
		return token.equals("public") || token.equals("private") || token.equals("protected");
	}

	private static boolean isPlainIdentifier(String token) {
		return !token.isEmpty() && isIdentifierStart(token.charAt(0)) && !KEYWORDS.contains(token) && !PRIMITIVE_TYPES.contains(token)
				&& token.indexOf('"') < 0 && token.indexOf('\'') < 0;
	}

	private Map<String, Boolean> templateNames(int start, int end) {
		return templateNames(start, end, new LinkedHashSet<>());
	}

	//the qualified names followed by template arguments, and whether they are types, with the names of the types in the template arguments
	private Map<String, Boolean> templateNames(int start, int end, Set<String> types) {
		Map<String, Boolean> templates = new LinkedHashMap<>();
		List<int[]> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			tokens.add(new int[] {offset, tokenEnd});
			offset = skip(tokenEnd, end);
		}
		for(int i = 0; i < tokens.size(); i++) {
			String token = tokenText(tokens.get(i));
			String previous = i > 0 ? tokenText(tokens.get(i - 1)) : "";
			if(!isIdentifierStart(token.charAt(0)) || KEYWORDS.contains(token) || previous.equals("::")) {
				continue;
			}
			boolean member = previous.equals(".") || previous.equals("->") || previous.equals("template");
			StringBuilder name = new StringBuilder(token);
			if(previous.equals("::") && i > 1) {
				name.insert(0, "::");
			}
			int j = i + 1;
			while(j + 1 < tokens.size() && tokenText(tokens.get(j)).equals("::") && isIdentifierStart(tokenText(tokens.get(j + 1)).charAt(0))) {
				name.append("::").append(tokenText(tokens.get(j + 1)));
				j += 2;
			}
			if(j < tokens.size() && tokenText(tokens.get(j)).equals("<")) {
				int close = matchingAngleBracket(tokens, j);
				if(close > 0) {
					String follower = close + 1 < tokens.size() ? tokenText(tokens.get(close + 1)) : ";";
					if(TEMPLATE_FOLLOWERS.contains(follower) || isIdentifierStart(follower.charAt(0))) {
						boolean type = !follower.equals("(");
						String key = name.toString();
						if(!PRIMITIVE_TYPES.contains(key) && !member) {
							templates.merge(key, type, (a, b) -> a || b);
						}
						if(!member || follower.equals("(")) {
							argumentTypes(tokens, j, close, types);
						}
					}
				}
			}
		}
		return templates;
	}

	//the qualified names that are template arguments, i.e., T and ns::T in f<T, ns::T*>
	private void argumentTypes(List<int[]> tokens, int open, int close, Set<String> types) {
		int depth = 0;
		for(int i = open; i < close; i++) {
			String token = tokenText(tokens.get(i));
			if(token.equals("<") || token.equals("(")) {
				depth++;
				continue;
			}
			if(token.equals(">") || token.equals(")")) {
				depth--;
				continue;
			}
			String previous = tokenText(tokens.get(i - 1));
			if(depth != 1 || !isIdentifierStart(token.charAt(0)) || KEYWORDS.contains(token) || PRIMITIVE_TYPES.contains(token)
					|| !(previous.equals("<") || previous.equals(",") || previous.equals("const") || previous.equals("::") && i >= 2 && tokenText(tokens.get(i - 2)).equals("<"))) {
				continue;
			}
			StringBuilder name = new StringBuilder(previous.equals("::") ? "::" + token : token);
			int j = i + 1;
			while(j + 1 < close + 1 && tokenText(tokens.get(j)).equals("::") && isIdentifierStart(tokenText(tokens.get(j + 1)).charAt(0))) {
				name.append("::").append(tokenText(tokens.get(j + 1)));
				j += 2;
			}
			String follower = j <= close ? tokenText(tokens.get(j)) : ">";
			if(follower.equals(",") || follower.equals(">") || follower.equals("*") || follower.equals("&") || follower.equals("&&") || follower.equals("...") || follower.equals(">>")) {
				types.add(name.toString());
			}
			i = j - 1;
		}
	}

	private String tokenText(int[] token) {
		return content.substring(token[0], token[1]);
	}

	//the index of the closing angle bracket of the template arguments, or -1
	private int matchingAngleBracket(List<int[]> tokens, int open) {
		int depth = 0;
		int nesting = 0;
		for(int i = open; i < tokens.size(); i++) {
			String token = tokenText(tokens.get(i));
			switch(token) {
				case "<" -> {
					if(nesting == 0) {
						depth++;
					}
				}
				case ">" -> {
					if(nesting == 0 && --depth == 0) {
						return i;
					}
				}
				case ">>" -> {
					if(nesting == 0) {
						depth -= 2;
						if(depth <= 0) {
							return i;
						}
					}
				}
				case "(", "[" -> nesting++;
				case ")", "]" -> {
					if(--nesting < 0) {
						return -1;
					}
				}
				case ";", "{", "}" -> {
					return -1;
				}
				case "&&", "||" -> {
					String next = i + 1 < tokens.size() ? tokenText(tokens.get(i + 1)) : "";
					if(nesting == 0 && !next.equals(">") && !next.equals(",") && !next.equals("...")) {
						return -1;
					}
				}
				default -> {
				}
			}
		}
		return -1;
	}

	private boolean hasNestedStatement(IASTStatement statement) {
		for(IASTNode child : statement.getChildren()) {
			if(child instanceof IASTStatement) {
				return true;
			}
		}
		return false;
	}

	//the statement converted without parsing it again, with its nested statements converted as usual
	private Node statementWithoutReparse(IASTStatement statement) {
		boolean previous = direct;
		direct = true;
		try {
			return statement(statement);
		}
		finally {
			direct = previous;
		}
	}

	//the statement parsed again, or the nodes parsed again in an error node
	private Node single(List<Node> nodes, IASTStatement statement) {
		//a declaration that CDT split after the call of the macro of its type
		if(nodes.size() > 1 || failed(nodes)) {
			Node macroTypeDeclaration = macroTypeDeclaration(start(statement), end(statement), Context.BLOCK);
			if(macroTypeDeclaration != null) {
				return macroTypeDeclaration;
			}
		}
		if(nodes.size() == 1) {
			return nodes.get(0);
		}
		if(nodes.isEmpty()) {
			return error(start(statement), end(statement));
		}
		return inline(nodes);
	}

	//the unknown names that CDT parsed as the types of declarations, i.e., f(x); and if(f(x)), which tree-sitter parses as calls
	private Set<String> misparsedCalls(IASTNode ast, boolean root, Set<String> names) {
		if(!root && ast instanceof IASTStatement) {
			return names;
		}
		if(ast instanceof IASTSimpleDeclaration declaration && declaration.getDeclSpecifier() instanceof IASTNamedTypeSpecifier named
				&& declaration.getDeclarators().length == 1 && hasLocation(named.getName())) {
			IASTDeclarator declarator = declaration.getDeclarators()[0];
			IASTName name = named.getName();
			if(declarator.getNestedDeclarator() != null && declarator.getPointerOperators().length == 0 && !(name instanceof ICPPASTTemplateId)
					&& !(name instanceof ICPPASTQualifiedName qualified && qualified.getLastName() instanceof ICPPASTTemplateId)
					&& tokenAt(nextTokenStart(end(name))).equals("(") && unknown(name) && named.getStorageClass() == IASTDeclSpecifier.sc_unspecified
					&& !named.isConst()) {
				names.add(text(name).replaceAll("\\s+", ""));
			}
			//a declaration without initializer in a condition, i.e., if (a && b)
			IASTNode parent = declaration.getParent();
			if((parent instanceof IASTIfStatement || parent instanceof IASTWhileStatement || parent instanceof IASTSwitchStatement || parent instanceof IASTForStatement)
					&& declarator.getInitializer() == null && !(name instanceof ICPPASTTemplateId) && unknown(name)) {
				names.add(text(name).replaceAll("\\s+", ""));
			}
		}
		for(IASTNode child : ast.getChildren()) {
			misparsedCalls(child, false, names);
		}
		return names;
	}

	private static boolean isType(IASTName name) {
		try {
			return name.resolveBinding() instanceof IType;
		}
		catch(RuntimeException e) {
			return false;
		}
	}

	//whether CDT does not know the name, or knows it only from the declarations that the model added before parsing the file, which tree-sitter does not know
	private static boolean unknown(IASTName name) {
		try {
			IBinding binding = name.resolveBinding();
			return binding instanceof IProblemBinding || CppFileProcessor.declaredOnlyBeforeFile(name.getTranslationUnit(), binding);
		}
		catch(RuntimeException e) {
			return true;
		}
	}

	//whether CDT parsed template arguments as comparisons in the node, without the nested statements
	private boolean misparsedTemplate(IASTNode ast, boolean root) {
		if(!root && (ast instanceof IASTStatement || ast instanceof ICPPASTConstructorChainInitializer)) {
			return false;
		}
		if(ast instanceof IASTBinaryExpression binary && binary.getOperator() == IASTBinaryExpression.op_lessThan
				&& binary.getOperand1() instanceof IASTFieldReference field && hasLocation(field.getFieldName())) {
			//a member template, i.e., x.f<T>(y)
			List<int[]> tokens = new ArrayList<>();
			int offset = start(field.getFieldName());
			int limit = Math.min(length, lineEnd(end(binary)) + 200);
			while(offset < limit && tokens.size() < 300) {
				int tokenEnd = Math.min(tokenEnd(offset, limit), limit);
				tokens.add(new int[] {offset, tokenEnd});
				offset = skip(tokenEnd, limit);
			}
			if(tokens.size() > 1 && tokenText(tokens.get(1)).equals("<")) {
				int close = matchingAngleBracket(tokens, 1);
				if(close > 0 && close + 1 < tokens.size() && tokenText(tokens.get(close + 1)).equals("(")) {
					return true;
				}
			}
		}
		if(ast instanceof IASTBinaryExpression binary && binary.getOperator() == IASTBinaryExpression.op_lessThan && hasLocation(binary.getOperand1())) {
			//the qualified name before the angle bracket
			int operandEnd = end(binary.getOperand1());
			int nameStart = qualifiedNameStart(start(binary.getOperand1()), operandEnd);
			if(nameStart >= 0 && (!templateNames(nameStart, end(binary)).isEmpty() || !templateNames(nameStart, Math.min(length, lineEnd(operandEnd) + 200)).isEmpty())) {
				return true;
			}
		}
		for(IASTNode child : ast.getChildren()) {
			if(misparsedTemplate(child, false)) {
				return true;
			}
		}
		return false;
	}

	//the code between the offsets without the macro definitions, and with string literals instead of the macros between string literals
	private String rewrite(int start, int end, TreeMap<Integer, String> inserted, Map<Integer, Integer> masked) {
		StringBuilder code = new StringBuilder(withoutMacroDefinitions(start, end));
		List<int[]> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = Math.min(tokenEnd(offset, end), end);
			tokens.add(new int[] {offset, tokenEnd});
			offset = skip(tokenEnd, end);
		}
		for(int i = 0; i < tokens.size(); i++) {
			int[] token = tokens.get(i);
			String text = tokenText(token);
			if(!isIdentifierStart(text.charAt(0)) || KEYWORDS.contains(text)) {
				continue;
			}
			//the template arguments of a member template, i.e., T in x.f<T>(y), which CDT parses as a comparison when it does not know the type of x
			String previous = i > 0 ? tokenText(tokens.get(i - 1)) : "";
			if((previous.equals(".") || previous.equals("->") || previous.equals("template")) && i + 1 < tokens.size() && tokenText(tokens.get(i + 1)).equals("<")) {
				int close = matchingAngleBracket(tokens, i + 1);
				if(close > 0 && close + 1 < tokens.size() && tokenText(tokens.get(close + 1)).equals("(")) {
					int argumentsStart = tokens.get(i + 1)[1];
					int argumentsEnd = tokenText(tokens.get(close)).equals(">>") ? tokens.get(close)[0] + 1 : tokens.get(close)[0];
					if(argumentsEnd > argumentsStart) {
						String type = "int";
						int length = argumentsEnd - argumentsStart;
						for(int k = argumentsStart; k < argumentsEnd; k++) {
							int index = k - argumentsStart;
							code.setCharAt(k - start, index < type.length() ? type.charAt(index) : content.charAt(k) == '\n' ? '\n' : ' ');
						}
						if(length < type.length()) {
							inserted.put(argumentsEnd, type.substring(length));
						}
						masked.put(argumentsStart, argumentsEnd);
					}
					i = close;
					continue;
				}
			}
			//a macro between string literals
			boolean stringBefore = i > 0 && isString(tokens.get(i - 1)) && tokens.get(i - 1)[1] < token[0] && !(i > 1 && tokenText(tokens.get(i - 2)).equals("operator"));
			boolean stringAfter = i + 1 < tokens.size() && isString(tokens.get(i + 1));
			if((stringBefore || stringAfter) && text.length() >= 2 && literalPrefix(token[0], token[1]) < 0) {
				code.setCharAt(token[0] - start, '"');
				for(int k = token[0] + 1; k < token[1] - 1; k++) {
					code.setCharAt(k - start, 'x');
				}
				code.setCharAt(token[1] - 1 - start, '"');
			}
		}
		return code.toString();
	}

	private boolean isString(int[] token) {
		int prefix = literalPrefix(token[0], token[1]);
		return prefix >= 0 && content.charAt(token[0] + prefix) == '"';
	}

	//the code between the offsets with spaces instead of the macro definitions, so that no macro is expanded
	private String withoutMacroDefinitions(int start, int end) {
		StringBuilder code = new StringBuilder(content.substring(start, end));
		for(var entry : directives.subMap(start, end).entrySet()) {
			int nameStart = skip(entry.getKey() + 1, entry.getValue());
			if(content.startsWith("define", nameStart) || content.startsWith("undef", nameStart)) {
				for(int i = entry.getKey(); i < Math.min(entry.getValue(), end); i++) {
					char character = content.charAt(i);
					if(character != '\n' && character != '\r') {
						code.setCharAt(i - start, ' ');
					}
				}
			}
		}
		return code.toString();
	}

	//the nodes of the inactive preprocessor branches between the offsets, outside the nodes of the active code
	private List<Node> inactive(IASTTranslationUnit translationUnit, int start, int end, Context context, IASTNode[] activeNodes) {
		List<Node> nodes = new ArrayList<>();
		for(int[] region : inactiveRegions(translationUnit)) {
			if(region[0] >= start && region[1] <= end) {
				boolean overlaps = false;
				for(IASTNode active : activeNodes) {
					if(hasLocation(active) && start(active) < region[1] && end(active) > region[0]) {
						overlaps = true;
						break;
					}
				}
				if(!overlaps) {
					nodes.addAll(reparse(region[0], region[1], context));
				}
			}
		}
		return nodes;
	}

	//the start and end offsets of the code in the outermost inactive preprocessor branches
	private List<int[]> inactiveRegions(IASTTranslationUnit translationUnit) {
		List<int[]> regions = new ArrayList<>();
		int depth = 0;
		int inactiveDepth = -1;
		int regionStart = -1;
		for(IASTPreprocessorStatement statement : translationUnit.getAllPreprocessorStatements()) {
			if(!CppFileProcessor.isPartOfFile(statement) || statement.getFileLocation() == null) {
				continue;
			}
			int statementStart = start(statement);
			if(statementStart < 0 || statementStart >= length) {
				//the preamble of the code parsed again
				continue;
			}
			int lineEnd = lineEnd(end(statement));
			if(statement instanceof IASTPreprocessorIfStatement || statement instanceof IASTPreprocessorIfdefStatement || statement instanceof IASTPreprocessorIfndefStatement) {
				depth++;
				if(inactiveDepth < 0 && !taken(statement)) {
					inactiveDepth = depth;
					regionStart = lineEnd;
				}
			}
			else if(statement instanceof IASTPreprocessorElifStatement || statement instanceof IASTPreprocessorElseStatement) {
				if(inactiveDepth == depth) {
					regions.add(new int[] {regionStart, statementStart});
					inactiveDepth = -1;
				}
				if(inactiveDepth < 0 && !taken(statement)) {
					inactiveDepth = depth;
					regionStart = lineEnd;
				}
			}
			else if(statement instanceof IASTPreprocessorEndifStatement) {
				if(inactiveDepth == depth) {
					regions.add(new int[] {regionStart, statementStart});
					inactiveDepth = -1;
				}
				depth--;
			}
		}
		return regions;
	}

	private static boolean taken(IASTPreprocessorStatement statement) {
		if(statement instanceof IASTPreprocessorIfStatement s) {
			return s.taken();
		}
		if(statement instanceof IASTPreprocessorIfdefStatement s) {
			return s.taken();
		}
		if(statement instanceof IASTPreprocessorIfndefStatement s) {
			return s.taken();
		}
		if(statement instanceof IASTPreprocessorElifStatement s) {
			return s.taken();
		}
		if(statement instanceof IASTPreprocessorElseStatement s) {
			return s.taken();
		}
		return true;
	}

	private int lineEnd(int offset) {
		int end = offset;
		while(end < length && content.charAt(end) != '\n') {
			end++;
		}
		return Math.min(end + 1, length);
	}

	//translation unit and declarations

	private Node translationUnit(IASTTranslationUnit translationUnit) {
		source = new Source(translationUnit, 0, false);
		emptyMacros = new HashSet<>();
		for(IASTPreprocessorMacroDefinition definition : translationUnit.getMacroDefinitions()) {
			if(definition.isActive() && !(definition instanceof IASTPreprocessorFunctionStyleMacroDefinition) && definition.getExpansion().isBlank()) {
				emptyMacros.add(definition.getName().toString());
			}
		}
		List<Node> children = new ArrayList<>();
		IASTDeclaration[] declarations = activeDeclarations(translationUnit.getDeclarations(false));
		children.addAll(declarations(declarations, Context.TOP));
		children.addAll(inactive(translationUnit, 0, length, Context.TOP, declarations));
		int start = nextTokenStart(0);
		Integer extra = extras.ceilingKey(0);
		if(extra != null && extra < start) {
			start = extra;
		}
		Node root = node("translation_unit", Math.min(start, length), length, children);
		emptyStatements(root);
		return root;
	}

	//the declarations, with the code of overlapping declarations, i.e., the declarations of the expansion of TEST(A, B) {...}, parsed again
	private List<Node> declarations(IASTDeclaration[] declarations, Context context) {
		List<Node> nodes = new ArrayList<>();
		IASTDeclaration[] sorted = declarations.clone();
		Arrays.sort(sorted, Comparator.comparingInt(this::start));
		int i = 0;
		while(i < sorted.length) {
			int start = start(sorted[i]);
			int end = end(sorted[i]);
			boolean truncated = false;
			//a function definition whose body ends before its closing brace because of a problem
			if(!source.reparsed && sorted[i] instanceof IASTFunctionDefinition function && function.getBody() != null && hasLocation(function.getBody())) {
				int open = start(function.getBody());
				if(open >= 0 && open < length && content.charAt(open) == '{') {
					int close = matching(open, length);
					if(close + 1 > end && content.charAt(close) == '}') {
						end = close + 1;
						truncated = true;
					}
				}
			}
			int j = i + 1;
			while(j < sorted.length && (start(sorted[j]) < end
					//the problem declarations of a construct that CDT split, i.e., BENCHMARK(f)->Args({1, 2});
					|| sorted[j] instanceof IASTProblemHolder && sorted[j - 1] instanceof IASTProblemHolder
					&& !content.substring(start, end).stripTrailing().endsWith(";")
					//a macro before a declaration, i.e., LLAMA_API int f();
					|| !source.reparsed && sorted[j - 1] instanceof IASTProblemHolder && !content.substring(start, end).stripTrailing().endsWith(";")
					&& !content.substring(start, end).stripTrailing().endsWith("}"))) {
				end = Math.max(end, end(sorted[j]));
				j++;
			}
			if(j == i + 1 && !truncated) {
				nodes.add(declarationOrReparsed(sorted[i], context));
			}
			else if(source.reparsed) {
				Node call = context == Context.CLASS ? macroCallStatement(start, end, true) : null;
				if(call == null) {
					call = macroTypeDeclaration(start, end, context);
				}
				nodes.add(call != null ? call : error(start, end));
			}
			else {
				nodes.addAll(reparseDeclaration(start, end, context));
			}
			i = j;
		}
		if(!source.reparsed) {
			absorbEmptyMacros(nodes);
		}
		return nodes;
	}

	//whether the offset is in a comment or a preprocessor directive, i.e., X in #if X
	private boolean inExtra(int offset) {
		Map.Entry<Integer, Integer> extra = extras.floorEntry(offset);
		return extra != null && extra.getValue() > offset;
	}

	//the macros expanded to nothing before a declaration, i.e., LLAMA_API in LLAMA_API int f();, which are not in the location of the declaration, are its first children,
	//and so are the calls of these macros, i.e., ABSL_DEPRECATED("...") in ABSL_DEPRECATED("...") static int f();
	private void absorbEmptyMacros(List<Node> nodes) {
		int previousEnd = 0;
		for(Node node : nodes) {
			absorbEmptyMacros(node, previousEnd);
			previousEnd = Math.max(previousEnd, node.end);
		}
	}

	//the macros expanded to nothing between the offset and a declaration become its first children
	private void absorbEmptyMacros(Node node, int previousEnd) {
		if(node.type.equals("declaration") || node.type.equals("function_definition") || node.type.equals("field_declaration")) {
			boolean absorbed = true;
			while(absorbed) {
				absorbed = false;
				for(int[] expansion : source.expansions) {
					if(expansion[0] >= previousEnd && expansion[1] <= node.start && skip(expansion[1], node.start) == node.start && !inExtra(expansion[0])) {
						List<Node> tokens = tokens(expansion[0], expansion[1]);
						Node macro = tokens.size() == 1 && tokens.get(0).type.equals("identifier") ? tokens.get(0) : macroCall(expansion[0], expansion[1]);
						if(macro != null) {
							node.children.add(0, macro);
							macro.parent = node;
							node.start = expansion[0];
							absorbed = true;
						}
						break;
					}
				}
			}
		}
	}

	//the call of a macro between the offsets as a call with its arguments parsed again, i.e., ABSL_DEPRECATED("...")
	private Node macroCall(int start, int end) {
		int nameEnd = start + tokenAt(start).length();
		int open = skip(nameEnd, end);
		if(!isIdentifierStart(content.charAt(start)) || open >= end || !tokenAt(open).equals("(") || matching(open, end) != end - 1) {
			return null;
		}
		Node arguments = reparseArgumentList(open, end);
		if(arguments == null || failed(List.of(arguments))) {
			arguments = node("argument_list", open, end, new ArrayList<>());
		}
		return node("call_expression", start, end, leaf("identifier", start, nameEnd), arguments);
	}

	private IASTDeclaration[] activeDeclarations(IASTDeclaration[] declarations) {
		List<IASTDeclaration> active = new ArrayList<>();
		for(IASTDeclaration declaration : declarations) {
			if(declaration.isActive() && hasLocation(declaration) && CppFileProcessor.isPartOfFile(declaration) && end(declaration) > start(declaration)) {
				active.add(declaration);
			}
		}
		return active.toArray(new IASTDeclaration[0]);
	}

	private Node declarationOrReparsed(IASTDeclaration declaration, Context context) {
		//NAME(...) name; is not valid C++ unless NAME is a macro, which CDT reads as a type with a parenthesized declarator
		Node macroTypeDeclaration = declaration instanceof IASTSimpleDeclaration || declaration instanceof IASTProblemDeclaration ? macroTypeDeclaration(start(declaration), end(declaration), context) : null;
		if(macroTypeDeclaration != null) {
			return macroTypeDeclaration;
		}
		if(!source.reparsed && !(declaration instanceof ICPPASTNamespaceDefinition) && !(declaration instanceof ICPPASTLinkageSpecification)
				&& !isClassDefinition(declaration) && misparsedTemplate(declaration, true)) {
			return orDeclaration(reparseDeclaration(start(declaration), end(declaration), context), declaration, context);
		}
		if(reparse(declaration) && !(declaration instanceof ICPPASTNamespaceDefinition) && !(declaration instanceof ICPPASTLinkageSpecification)
				&& !isClassDefinition(declaration) && !isFunctionWithMacroFreeHeader(declaration)) {
			if(source.reparsed) {
				Node call = context == Context.CLASS ? macroCallStatement(start(declaration), end(declaration), true) : null;
				if(call == null) {
					call = macroTypeDeclaration(start(declaration), end(declaration), context);
				}
				return call != null ? call : error(start(declaration), end(declaration));
			}
			return orDeclaration(macroFunctionDefinition(reparseDeclaration(start(declaration), end(declaration), context), declaration), declaration, context);
		}
		return declaration(declaration, context);
	}

	//the call of a macro followed by a body is a function definition when the macro is expanded to one,
	//i.e., MATCHER_P(IsEven, n, "") {...} of the gmock stub, unlike TEST_CASE("x", "[y]") {...} of a macro that is not known
	private List<Node> macroFunctionDefinition(List<Node> nodes, IASTDeclaration declaration) {
		IASTDeclaration definition = declaration instanceof ICPPASTTemplateDeclaration template ? template.getDeclaration() : declaration;
		if(!(definition instanceof IASTFunctionDefinition) || nodes.size() != 2) {
			return nodes;
		}
		Node statement = nodes.get(0);
		Node body = nodes.get(1);
		if(statement.type.equals("expression_statement") && statement.children.size() == 1 && statement.first().type.equals("call_expression")
				&& body.type.equals("compound_statement") && statement.start == start(declaration) && body.end == end(declaration)) {
			return List.of(node("function_definition", statement.start, body.end, statement.first(), body));
		}
		return nodes;
	}

	//the nodes parsed again, or the declaration of CDT when they have errors and CDT parsed it, i.e., with the macros of a header that is not available
	private Node orDeclaration(List<Node> nodes, IASTDeclaration declaration, Context context) {
		if(failed(nodes) && !problem(declaration)) {
			return declaration(declaration, context);
		}
		return inline(nodes);
	}

	private static boolean failed(List<Node> nodes) {
		if(nodes.isEmpty()) {
			return true;
		}
		for(Node node : nodes) {
			if(node.type.equals("ERROR")) {
				return true;
			}
		}
		return false;
	}

	private boolean isClassDefinition(IASTDeclaration declaration) {
		if(declaration instanceof IASTSimpleDeclaration simple && simple.getDeclSpecifier() instanceof IASTCompositeTypeSpecifier composite
				&& simple.getDeclarators().length == 0 && !(declaration instanceof IASTProblemHolder)) {
			IASTFileLocation location = composite.getFileLocation();
			int bodyStart = find("{", start(composite), end(composite));
			return location != null && bodyStart >= 0 && !source.expands(start(declaration), bodyStart + 1)
					&& !source.expands(end(composite) - 1, end(declaration));
		}
		return false;
	}

	//a function definition whose macros are only in its body
	private boolean isFunctionWithMacroFreeHeader(IASTDeclaration declaration) {
		if(declaration instanceof IASTFunctionDefinition function && function.getBody() != null && hasLocation(function.getBody()) && !problem(function.getDeclarator())) {
			IASTDeclSpecifier declSpecifier = function.getDeclSpecifier();
			if(declSpecifier != null && problem(declSpecifier)) {
				return false;
			}
			return !source.expands(start(declaration), start(function.getBody()) + 1) && !source.expands(end(declaration) - 1, end(declaration));
		}
		return false;
	}

	private Node declaration(IASTDeclaration declaration, Context context) {
		if(declaration instanceof IASTProblemHolder) {
			return source.reparsed ? error(start(declaration), end(declaration)) : inline(reparseDeclaration(start(declaration), end(declaration), context));
		}
		if(declaration instanceof ICPPASTTemplateDeclaration template) {
			return templateDeclaration(template);
		}
		if(declaration instanceof ICPPASTTemplateSpecialization specialization) {
			int open = find("<", start(declaration), end(declaration));
			int close = find(">", open, end(declaration));
			Node parameters = node("template_parameter_list", open, close + 1, new ArrayList<>());
			Node inner = declarationOrReparsed(specialization.getDeclaration(), Context.TOP);
			absorbEmptyMacros(inner, close + 1);
			return node("template_declaration", declaration, parameters, inner);
		}
		if(declaration instanceof IASTFunctionDefinition function) {
			return functionDefinition(function, context);
		}
		if(declaration instanceof IASTSimpleDeclaration simple) {
			return simpleDeclaration(simple, context);
		}
		if(declaration instanceof ICPPASTNamespaceDefinition namespace) {
			return namespaceDefinition(namespace);
		}
		if(declaration instanceof ICPPASTLinkageSpecification linkage) {
			return linkageSpecification(linkage);
		}
		if(declaration instanceof ICPPASTVisibilityLabel label) {
			int colon = findLast(":", start(label), end(label));
			Node specifier = node("access_specifier", start(label), colon < 0 ? end(label) : previousTokenEnd(start(label), colon), new ArrayList<>());
			return inline(specifier, colon < 0 ? null : leaf(":", colon, colon + 1));
		}
		if(declaration instanceof ICPPASTUsingDirective using) {
			return node("using_declaration", using, name(using.getQualifiedName(), NameKind.IDENTIFIER));
		}
		if(declaration instanceof ICPPASTUsingDeclaration using) {
			//a member without using is a macro expanded to a field in the code parsed again, i.e., DIAGNOSTIC_FIELD; with #define DIAGNOSTIC_FIELD int last_error
			if(!tokenAt(start(using)).equals("using")) {
				return node("field_declaration", using, name(using.getName(), NameKind.TYPE));
			}
			return node("using_declaration", using, name(using.getName(), NameKind.IDENTIFIER));
		}
		if(declaration instanceof ICPPASTAliasDeclaration alias) {
			return node("alias_declaration", alias, name(alias.getAlias(), NameKind.TYPE), typeDescriptor(alias.getMappingTypeId()));
		}
		if(declaration instanceof ICPPASTStaticAssertDeclaration staticAssert) {
			return node("static_assert_declaration", staticAssert, expression(staticAssert.getCondition()),
					staticAssert.getMessage() == null ? null : expression(staticAssert.getMessage()));
		}
		if(declaration instanceof ICPPASTExplicitTemplateInstantiation instantiation) {
			Node inner = declaration(instantiation.getDeclaration(), Context.TOP);
			Node node = node("template_instantiation", instantiation, inner.type.equals("declaration") ? inline(inner.children) : inner);
			wrapModifiers(node);
			return node;
		}
		return generic(declaration);
	}

	private Node templateDeclaration(ICPPASTTemplateDeclaration template) {
		List<Node> children = new ArrayList<>();
		ICPPASTTemplateParameter[] parameters = template.getTemplateParameters();
		int open = find("<", start(template), end(template));
		int close = parameters.length == 0 ? find(">", open, end(template)) : templateParametersEnd(parameters[parameters.length - 1], end(template));
		List<Node> parameterNodes = new ArrayList<>();
		for(ICPPASTTemplateParameter parameter : parameters) {
			parameterNodes.add(templateParameter(parameter));
		}
		children.add(node("template_parameter_list", open, close + 1, parameterNodes));
		Node declaration = declarationOrReparsed(template.getDeclaration(), Context.TOP);
		//the macros expanded to nothing after the template parameters belong to the declaration, like the location of the declaration in the model,
		//i.e., GOOGLE_PROTOBUF_ATTRIBUTE_NOINLINE in template<> GOOGLE_PROTOBUF_ATTRIBUTE_NOINLINE T* Arena::CreateMaybeMessage<T>(Arena* arena) {...}
		absorbEmptyMacros(declaration, close + 1);
		children.add(declaration);
		return node("template_declaration", template, children);
	}

	private int templateParametersEnd(ICPPASTTemplateParameter last, int limit) {
		int close = find(">", end(last), limit);
		return close < 0 ? end(last) : close;
	}

	private Node templateParameter(ICPPASTTemplateParameter parameter) {
		if(parameter instanceof ICPPASTSimpleTypeTemplateParameter simple) {
			String type = simple.isParameterPack() ? "variadic_type_parameter_declaration" : simple.getDefaultType() != null ? "optional_type_parameter_declaration" : "type_parameter_declaration";
			List<Node> children = new ArrayList<>();
			if(simple.getName() != null && hasLocation(simple.getName()) && end(simple.getName()) > start(simple.getName())) {
				children.add(name(simple.getName(), NameKind.TYPE));
			}
			if(simple.getDefaultType() != null) {
				Node defaultType = typeDescriptor(simple.getDefaultType());
				children.add(defaultType.children.size() == 1 ? defaultType.first() : defaultType);
			}
			return node(type, simple, children);
		}
		if(parameter instanceof ICPPASTParameterDeclaration declaration) {
			return parameterDeclaration(declaration);
		}
		if(parameter instanceof ICPPASTTemplatedTypeTemplateParameter templated) {
			List<Node> children = new ArrayList<>();
			ICPPASTTemplateParameter[] parameters = templated.getTemplateParameters();
			int open = find("<", start(templated), end(templated));
			int close = parameters.length == 0 ? find(">", open, end(templated)) : templateParametersEnd(parameters[parameters.length - 1], end(templated));
			List<Node> parameterNodes = new ArrayList<>();
			for(ICPPASTTemplateParameter p : parameters) {
				parameterNodes.add(templateParameter(p));
			}
			children.add(node("template_parameter_list", open, close + 1, parameterNodes));
			int classStart = nextTokenStart(close + 1);
			List<Node> nameNodes = new ArrayList<>();
			if(templated.getName() != null && hasLocation(templated.getName()) && end(templated.getName()) > start(templated.getName())) {
				nameNodes.add(name(templated.getName(), NameKind.TYPE));
			}
			children.add(node("type_parameter_declaration", classStart, end(templated), nameNodes));
			return node("template_template_parameter_declaration", templated, children);
		}
		return generic(parameter);
	}

	private Node namespaceDefinition(ICPPASTNamespaceDefinition namespace) {
		List<Node> children = new ArrayList<>();
		//namespace A::B { } is a namespace definition in another one with the same range
		List<IASTName> names = new ArrayList<>();
		ICPPASTNamespaceDefinition innermost = namespace;
		names.add(namespace.getName());
		while(innermost.getDeclarations(true).length == 1 && innermost.getDeclarations(true)[0] instanceof ICPPASTNamespaceDefinition nested
				&& start(nested) == start(namespace) && end(nested) == end(namespace)) {
			innermost = nested;
			names.add(nested.getName());
		}
		int open = -1;
		IASTName last = names.get(names.size() - 1);
		if(last != null && hasLocation(last) && end(last) > start(last)) {
			Node name = leaf("namespace_identifier", start(last), end(last));
			for(int i = names.size() - 2; i >= 0; i--) {
				name = node("nested_namespace_specifier", start(names.get(i)), name.end, leaf("namespace_identifier", start(names.get(i)), end(names.get(i))), name);
			}
			children.add(name);
			open = find("{", end(last), end(namespace));
		}
		else {
			open = find("{", start(namespace), end(namespace));
		}
		children.add(declarationList(open, end(namespace), innermost.getDeclarations(false), namespace.getTranslationUnit()));
		return node("namespace_definition", namespace, children);
	}

	private Node namespaceName(IASTName name) {
		String text = text(name);
		if(text.contains("::")) {
			List<int[]> parts = new ArrayList<>();
			int offset = start(name);
			int end = end(name);
			while(offset < end) {
				int separator = find("::", offset, end);
				int partEnd = separator < 0 ? end : previousTokenEnd(offset, separator);
				parts.add(new int[] {skip(offset, end), partEnd});
				offset = separator < 0 ? end : separator + 2;
			}
			Node node = leaf("namespace_identifier", parts.get(parts.size() - 1)[0], parts.get(parts.size() - 1)[1]);
			for(int i = parts.size() - 2; i >= 0; i--) {
				node = node("nested_namespace_specifier", parts.get(i)[0], node.end, leaf("namespace_identifier", parts.get(i)[0], parts.get(i)[1]), node);
			}
			return node;
		}
		return leaf("namespace_identifier", start(name), end(name));
	}

	private Node declarationList(int open, int end, IASTDeclaration[] declarations, IASTTranslationUnit translationUnit) {
		List<Node> children = new ArrayList<>();
		IASTDeclaration[] active = activeDeclarations(declarations);
		children.addAll(declarations(active, Context.TOP));
		children.addAll(inactive(source.translationUnit, open + 1, end - 1, Context.TOP, active));
		Node node = node("declaration_list", open, end, children);
		emptyStatements(node);
		return node;
	}

	private Node linkageSpecification(ICPPASTLinkageSpecification linkage) {
		int literal = find("\"", start(linkage), end(linkage));
		int literalStart = skip(start(linkage) + "extern".length(), end(linkage));
		Node string = stringLiteral(literalStart, tokenEnd(literalStart, end(linkage)));
		int open = find("{", string.end, end(linkage));
		IASTDeclaration[] declarations = linkage.getDeclarations(false);
		if(open >= 0 && (declarations.length == 0 || open < start(declarations[0]))) {
			return node("linkage_specification", linkage, string, declarationList(open, end(linkage), declarations, linkage.getTranslationUnit()));
		}
		List<Node> children = new ArrayList<>();
		children.add(string);
		children.addAll(declarations(activeDeclarations(declarations), Context.TOP));
		return node("linkage_specification", linkage, children);
	}

	private Node functionDefinition(IASTFunctionDefinition function, Context context) {
		List<Node> children = new ArrayList<>();
		IASTDeclSpecifier declSpecifier = function.getDeclSpecifier();
		boolean hasType = hasType(declSpecifier);
		boolean friend = declSpecifier instanceof ICPPASTDeclSpecifier cpp && cpp.isFriend();
		children.add(declSpecifier(declSpecifier));
		NameKind kind = context == Context.CLASS && hasType && !friend ? NameKind.FIELD : NameKind.IDENTIFIER;
		IASTDeclarator declarator = function.getDeclarator();
		Node declaratorNode = declarator(declarator, kind, false);
		children.add(declaratorNode);
		int bodyStart = declaratorNode.end;
		if(function instanceof ICPPASTFunctionDefinition cpp) {
			ICPPASTConstructorChainInitializer[] initializers = cpp.getMemberInitializers();
			if(initializers.length > 0) {
				List<Node> initializerNodes = new ArrayList<>();
				for(ICPPASTConstructorChainInitializer initializer : initializers) {
					initializerNodes.add(fieldInitializer(initializer));
				}
				int colon = find(":", declaratorNode.end, start(initializers[0]));
				Node last = initializerNodes.get(initializerNodes.size() - 1);
				children.add(node("field_initializer_list", colon < 0 ? initializerNodes.get(0).start : colon, last.end, initializerNodes));
				bodyStart = last.end;
			}
			if(cpp.isDefaulted() || cpp.isDeleted()) {
				int equals = find("=", bodyStart, end(function));
				children.add(node(cpp.isDefaulted() ? "default_method_clause" : "delete_method_clause", equals, end(function), new ArrayList<>()));
			}
		}
		if(function instanceof ICPPASTFunctionWithTryBlock tryBlock) {
			List<Node> tryChildren = new ArrayList<>();
			tryChildren.add(statement(function.getBody()));
			for(ICPPASTCatchHandler handler : tryBlock.getCatchHandlers()) {
				tryChildren.add(catchClause(handler));
			}
			int tryStart = find("try", bodyStart, start(function.getBody()));
			children.add(node("try_statement", tryStart, end(function), tryChildren));
		}
		else if(function.getBody() != null && hasLocation(function.getBody())) {
			children.add(statement(function.getBody()));
		}
		Node node = node("function_definition", function, children);
		//the body without its closing brace, which ends after the function definition of CDT
		if(!children.isEmpty()) {
			node.end = Math.max(node.end, children.get(children.size() - 1).end);
		}
		wrapModifiers(node);
		if(friend) {
			Node friendToken = null;
			for(Node child : node.children) {
				if(child.type.equals("friend")) {
					friendToken = child;
				}
			}
			if(friendToken != null) {
				node.children.remove(friendToken);
				node.start = node.first().start;
				return node("friend_declaration", function, friendToken, node);
			}
		}
		return node;
	}

	private Node fieldInitializer(ICPPASTConstructorChainInitializer initializer) {
		Node name = name(initializer.getMemberInitializerId(), NameKind.FIELD);
		Node value = null;
		if(initializer.getInitializer() instanceof ICPPASTConstructorInitializer constructor && hasLocation(constructor) && misparsedTemplate(constructor, true)) {
			//the arguments of a member initializer, i.e., x(f<T>(y)), which CDT parses as comparisons even when it knows f, are parsed again as the arguments of a call
			value = reparseArgumentList(start(constructor), end(constructor));
		}
		if(value == null) {
			value = initializer(initializer.getInitializer());
		}
		return node("field_initializer", initializer, name, value);
	}

	private boolean hasType(IASTDeclSpecifier declSpecifier) {
		if(declSpecifier instanceof IASTSimpleDeclSpecifier simple) {
			return simple.getType() != IASTSimpleDeclSpecifier.t_unspecified || simple.isLong() || simple.isShort() || simple.isUnsigned() || simple.isSigned() || simple.isLongLong();
		}
		return declSpecifier != null;
	}

	private Node simpleDeclaration(IASTSimpleDeclaration declaration, Context context) {
		IASTDeclSpecifier declSpecifier = declaration.getDeclSpecifier();
		IASTDeclarator[] declarators = declaration.getDeclarators();
		boolean typedef = declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_typedef;
		boolean friend = declSpecifier instanceof ICPPASTDeclSpecifier cpp && cpp.isFriend();
		if(declaration instanceof ICPPASTStructuredBindingDeclaration binding) {
			return structuredBinding(binding);
		}
		Node specifiers = declSpecifier(declSpecifier);
		if(declarators.length == 0 && !typedef && !friend) {
			//class A {...}; is a class_specifier followed by a semicolon, except for the members
			if(context == Context.CLASS) {
				if(specifiers == null || specifiers.children.isEmpty()) {
					//a semicolon after a member function definition
					return leaf(";", start(declaration), end(declaration));
				}
				Node node = node("field_declaration", declaration, specifiers);
				wrapModifiers(node);
				return node;
			}
			Node node = node(INLINE, declaration, specifiers);
			wrapModifiers(node);
			return inline(node.children);
		}
		boolean hasType = hasType(declSpecifier);
		if(!hasType && !typedef && !friend && declarators.length == 1 && declarators[0] instanceof ICPPASTFunctionDeclarator function && function.isPureVirtual()) {
			//virtual ~A() = 0; is a function definition with a pure_virtual_clause, whose 0 is not a node
			Node declaratorNode = declarator(function, NameKind.IDENTIFIER, false);
			int equals = find("=", declaratorNode.end, end(declaration));
			Node clause = new Node("pure_virtual_clause");
			clause.start = equals;
			clause.end = end(declaration);
			clause.add(leaf("=", equals, equals + 1));
			int semicolon = findLast(";", equals, end(declaration));
			if(semicolon >= 0) {
				clause.add(leaf(";", semicolon, semicolon + 1));
			}
			Node node = node("function_definition", declaration, specifiers, declaratorNode, clause);
			wrapModifiers(node);
			return node;
		}
		List<Node> children = new ArrayList<>();
		children.add(specifiers);
		NameKind kind = typedef ? NameKind.TYPE : context == Context.CLASS && hasType && !friend ? NameKind.FIELD : NameKind.IDENTIFIER;
		boolean field = context == Context.CLASS && hasType && !friend && !typedef;
		for(IASTDeclarator declarator : declarators) {
			if(field) {
				children.add(fieldDeclarator(declarator));
			}
			else {
				children.add(declarator(declarator, kind, true));
			}
		}
		String type = typedef ? "type_definition" : field ? "field_declaration" : "declaration";
		Node node = node(type, declaration, children);
		wrapModifiers(node);
		if(friend) {
			Node friendToken = null;
			for(Node child : node.children) {
				if(child.type.equals("friend")) {
					friendToken = child;
				}
			}
			if(friendToken != null) {
				node.children.remove(friendToken);
				node.start = node.first().start;
				if(declarators.length == 0) {
					//friend class X; has the keyword and the name of the class specifier
					List<Node> children1 = new ArrayList<>();
					children1.add(friendToken);
					for(Node child : node.children) {
						if(child.type.endsWith("_specifier") && !child.type.equals("access_specifier") && !child.children.isEmpty() && child.last().type.endsWith("identifier") && child.children.size() == 2) {
							children1.addAll(child.children);
						}
						else {
							children1.add(child);
						}
					}
					return node("friend_declaration", declaration, children1);
				}
				return node("friend_declaration", declaration, friendToken, node);
			}
		}
		return node;
	}

	//a field declarator, whose initializer and bit field size are children of the field declaration
	private Node fieldDeclarator(IASTDeclarator declarator) {
		Node node = declarator(declarator, NameKind.FIELD, false);
		List<Node> nodes = new ArrayList<>();
		nodes.add(node);
		if(declarator instanceof IASTFieldDeclarator field && field.getBitFieldSize() != null) {
			Node size = expression(field.getBitFieldSize());
			int colon = findLast(":", node.end, size.start);
			nodes.add(node("bitfield_clause", colon, size.end, size));
		}
		IASTInitializer initializer = declarator.getInitializer();
		if(initializer != null && hasLocation(initializer)) {
			if(initializer instanceof IASTEqualsInitializer equals) {
				nodes.add(initializerClause(equals.getInitializerClause()));
			}
			else {
				nodes.add(initializer(initializer));
			}
		}
		return inline(nodes);
	}

	private Node structuredBinding(ICPPASTStructuredBindingDeclaration binding) {
		List<Node> names = new ArrayList<>();
		for(IASTName name : binding.getNames()) {
			names.add(leaf("identifier", start(name), end(name)));
		}
		int open = find("[", end(binding.getDeclSpecifier()), start(binding.getNames()[0]));
		int close = find("]", end(binding.getNames()[binding.getNames().length - 1]), end(binding));
		Node declarator = node("structured_binding_declarator", open, close + 1, names);
		if(binding.getRefQualifier() != null) {
			int reference = findLast(binding.getRefQualifier() == ICPPASTFunctionDeclarator.RefQualifier.RVALUE ? "&&" : "&", end(binding.getDeclSpecifier()), open);
			declarator = node("reference_declarator", reference, declarator.end, declarator);
		}
		IASTInitializer initializer = binding.getInitializer();
		if(initializer != null && hasLocation(initializer)) {
			declarator = initDeclarator(declarator, initializer);
		}
		Node node = node("declaration", binding, declSpecifier(binding.getDeclSpecifier()), declarator);
		wrapModifiers(node);
		return node;
	}

	//the semicolons between the declarations are empty expression statements
	private static void emptyStatements(Node node) {
		for(int i = 0; i < node.children.size(); i++) {
			Node child = node.children.get(i);
			if(child.leaf && child.type.equals(";")) {
				Node previous = i > 0 ? node.children.get(i - 1) : null;
				//the semicolon after a class specifier ends its declaration
				if(previous != null && (previous.type.endsWith("_specifier") && !previous.type.equals("access_specifier"))) {
					continue;
				}
				Node statement = new Node("expression_statement");
				statement.start = child.start;
				statement.end = child.end;
				statement.parent = node;
				child.parent = statement;
				statement.children.add(child);
				node.children.set(i, statement);
			}
		}
	}

	//declaration specifiers

	private Node declSpecifier(IASTDeclSpecifier declSpecifier) {
		if(declSpecifier == null || !hasLocation(declSpecifier)) {
			return null;
		}
		int start = start(declSpecifier);
		int end = end(declSpecifier);
		List<Node> nodes = new ArrayList<>();
		Node type = null;
		if(declSpecifier instanceof ICPPASTNamedTypeSpecifier named) {
			type = typeName(named.getName());
			if(type != null && named.isTypename()) {
				int typename = find("typename", start, type.start);
				if(typename >= 0) {
					type = node("dependent_type", typename, type.end, type);
				}
			}
		}
		else if(declSpecifier instanceof IASTNamedTypeSpecifier named) {
			type = typeName(named.getName());
		}
		else if(declSpecifier instanceof IASTElaboratedTypeSpecifier elaborated) {
			String keyword = switch(elaborated.getKind()) {
				case IASTElaboratedTypeSpecifier.k_struct -> "struct";
				case IASTElaboratedTypeSpecifier.k_union -> "union";
				case IASTElaboratedTypeSpecifier.k_enum -> "enum";
				default -> "class";
			};
			Node name = typeName(elaborated.getName());
			int keywordStart = name == null ? find(keyword, start, end) : findLast(keyword, start, name.start);
			type = name == null ? node(keyword + "_specifier", keywordStart < 0 ? start : keywordStart, end, new ArrayList<>()) :
				node(keyword + "_specifier", keywordStart < 0 ? name.start : keywordStart, name.end, name);
		}
		else if(declSpecifier instanceof ICPPASTCompositeTypeSpecifier composite) {
			type = classSpecifier(composite);
		}
		else if(declSpecifier instanceof IASTEnumerationSpecifier enumeration) {
			type = enumSpecifier(enumeration);
		}
		else if(declSpecifier instanceof IASTSimpleDeclSpecifier simple) {
			nodes.addAll(simpleType(simple, start, end));
		}
		if(type != null) {
			nodes.add(type);
			for(Node token : tokens(start, type.start)) {
				nodes.add(token);
			}
			for(Node token : tokens(type.end, end)) {
				nodes.add(token);
			}
		}
		return inline(attributeTokens(nodes));
	}

	//the tokens of a simple type with the modifiers
	private List<Node> simpleType(IASTSimpleDeclSpecifier simple, int start, int end) {
		List<Node> nodes = new ArrayList<>();
		List<Node> tokens = tokens(start, end);
		List<Node> sized = new ArrayList<>();
		for(int i = 0; i < tokens.size(); i++) {
			Node token = tokens.get(i);
			String text = content.substring(token.start, token.end);
			if(text.equals("decltype") && i + 1 < tokens.size() && tokens.get(i + 1).type.equals("(")) {
				int close = matching(tokens.get(i + 1).start, end);
				Node expression;
				if(simple.getType() == IASTSimpleDeclSpecifier.t_decltype_auto) {
					expression = leaf("auto", skip(tokens.get(i + 1).end, close), previousTokenEnd(tokens.get(i + 1).end, close));
					Node decltype = node("decltype", token.start, close + 1, expression);
					nodes.add(wrap("placeholder_type_specifier", decltype));
				}
				else {
					expression = simple.getDeclTypeExpression() == null ? null : expression(simple.getDeclTypeExpression());
					nodes.add(node("decltype", token.start, close + 1, expression));
				}
				while(i + 1 < tokens.size() && tokens.get(i + 1).start <= close) {
					i++;
				}
			}
			else if(text.equals("auto")) {
				nodes.add(wrap("placeholder_type_specifier", token));
			}
			else if(SIZED_MODIFIERS.contains(text)) {
				sized.add(token);
				nodes.add(null);
			}
			else if(PRIMITIVE_TYPES.contains(text) || text.equals("wchar_t") || text.equals("__int128")) {
				Node primitive = leaf("primitive_type", token.start, token.end);
				if(!sized.isEmpty()) {
					sized.add(primitive);
					nodes.add(null);
				}
				else {
					nodes.add(primitive);
				}
			}
			else {
				nodes.add(token);
			}
		}
		if(!sized.isEmpty()) {
			Node node = node("sized_type_specifier", sized.get(0).start, sized.get(sized.size() - 1).end, sized);
			int index = nodes.indexOf(null);
			nodes.removeIf(n -> n == null);
			nodes.add(index, node);
		}
		return nodes;
	}

	private int matching(int open, int limit) {
		String openText = content.substring(open, open + 1);
		String closeText = openText.equals("(") ? ")" : openText.equals("[") ? "]" : openText.equals("{") ? "}" : ">";
		int depth = 0;
		int offset = open;
		while(offset < limit) {
			int tokenEnd = Math.min(tokenEnd(offset, limit), limit);
			String token = content.substring(offset, tokenEnd);
			if(token.equals(openText)) {
				depth++;
			}
			else if(token.equals(closeText)) {
				depth--;
				if(depth == 0) {
					return offset;
				}
			}
			offset = skip(tokenEnd, limit);
		}
		return limit - 1;
	}

	//the attribute_declaration nodes of the [[ ]] tokens
	private List<Node> attributeTokens(List<Node> nodes) {
		List<Node> result = new ArrayList<>();
		for(int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			if(node != null && node.leaf && node.type.equals("[") && i + 1 < nodes.size() && nodes.get(i + 1).type.equals("[") && nodes.get(i + 1).start == node.end) {
				int close = matching(node.start, length);
				result.add(attributeDeclaration(node.start, close + 1));
				while(i + 1 < nodes.size() && nodes.get(i + 1).start < close + 1) {
					i++;
				}
			}
			else {
				result.add(node);
			}
		}
		return result;
	}

	//[[ attribute, ... ]]
	private Node attributeDeclaration(int start, int end) {
		Node node = new Node("attribute_declaration");
		node.start = start;
		node.end = end;
		node.add(leaf("[[", start, start + 2));
		int offset = skip(start + 2, end - 2);
		while(offset < end - 2) {
			int attributeEnd = offset;
			int depth = 0;
			int position = offset;
			while(position < end - 2) {
				int tokenEnd = Math.min(tokenEnd(position, end - 2), end - 2);
				String token = content.substring(position, tokenEnd);
				if(token.equals("(")) {
					depth++;
				}
				else if(token.equals(")")) {
					depth--;
				}
				else if(token.equals(",") && depth == 0) {
					break;
				}
				attributeEnd = tokenEnd;
				position = skip(tokenEnd, end - 2);
			}
			List<Node> children = new ArrayList<>();
			int open = find("(", offset, attributeEnd);
			int nameEnd = open < 0 ? attributeEnd : previousTokenEnd(offset, open);
			int separator = find("::", offset, nameEnd);
			if(separator >= 0) {
				children.add(leaf("identifier", offset, previousTokenEnd(offset, separator)));
				children.add(leaf("identifier", skip(separator + 2, nameEnd), nameEnd));
			}
			else {
				children.add(leaf("identifier", offset, nameEnd));
			}
			if(open >= 0) {
				children.add(node("argument_list", open, attributeEnd, tokens(open + 1, attributeEnd - 1)));
			}
			node.add(node("attribute", offset, attributeEnd, children));
			if(position < end - 2) {
				node.add(leaf(",", position, position + 1));
				offset = skip(position + 1, end - 2);
			}
			else {
				offset = end - 2;
			}
		}
		node.add(leaf("]]", end - 2, end));
		return node;
	}

	private Node classSpecifier(ICPPASTCompositeTypeSpecifier composite) {
		String keyword = switch(composite.getKey()) {
			case IASTCompositeTypeSpecifier.k_struct -> "struct";
			case IASTCompositeTypeSpecifier.k_union -> "union";
			default -> "class";
		};
		int end = end(composite);
		int keywordStart = find(keyword, start(composite), end);
		List<Node> children = new ArrayList<>();
		IASTName name = composite.getName();
		int afterName = keywordStart + keyword.length();
		if(name != null && hasLocation(name) && end(name) > start(name)) {
			Node nameNode = typeName(name);
			children.add(nameNode);
			afterName = nameNode.end;
			for(Node token : tokens(keywordStart + keyword.length(), nameNode.start)) {
				children.add(token);
			}
		}
		int open = find("{", afterName, end);
		ICPPASTBaseSpecifier[] bases = composite.getBaseSpecifiers();
		if(bases.length > 0) {
			int colon = find(":", afterName, start(bases[0]));
			List<Node> baseNodes = new ArrayList<>();
			for(ICPPASTBaseSpecifier base : bases) {
				baseNodes.add(baseSpecifier(base));
			}
			Node clause = node("base_class_clause", colon, end(bases[bases.length - 1]), baseNodes);
			children.add(clause);
		}
		if(open >= 0) {
			children.add(fieldDeclarationList(composite, open, end));
		}
		Node node = node(keyword + "_specifier", keywordStart, end, attributeTokens(children));
		wrapTokens(node, Set.of("final", "override"), "virtual_specifier");
		return node;
	}

	private Node baseSpecifier(ICPPASTBaseSpecifier base) {
		List<Node> nodes = new ArrayList<>();
		ICPPASTNameSpecifier specifier = base.getNameSpecifier();
		Node name = specifier instanceof IASTName name1 ? typeName(name1) : generic(specifier);
		nodes.add(name);
		for(Node token : tokens(start(base), name.start)) {
			nodes.add(token);
		}
		for(Node token : tokens(name.end, end(base))) {
			nodes.add(token);
		}
		Node node = node(INLINE, start(base), end(base), nodes);
		wrapTokens(node, Set.of("public", "private", "protected"), "access_specifier");
		return inline(node.children);
	}

	private Node fieldDeclarationList(ICPPASTCompositeTypeSpecifier composite, int open, int end) {
		String previousClassName = className;
		IASTName name = composite.getName();
		if(name != null && hasLocation(name) && end(name) > start(name)) {
			IASTName last = name instanceof ICPPASTQualifiedName qualified ? qualified.getLastName() : name;
			className = last instanceof ICPPASTTemplateId template ? template.getTemplateName().toString() : last.toString();
		}
		try {
			return fieldDeclarationList(composite.getDeclarations(false), open, end);
		}
		finally {
			className = previousClassName;
		}
	}

	private Node fieldDeclarationList(IASTDeclaration[] declarations, int open, int end) {
		List<Node> children = new ArrayList<>();
		IASTDeclaration[] members = activeDeclarations(declarations);
		children.addAll(declarations(members, Context.CLASS));
		children.addAll(inactive(source.translationUnit, open + 1, end - 1, Context.CLASS, members));
		return node("field_declaration_list", open, end, children);
	}

	private Node enumSpecifier(IASTEnumerationSpecifier enumeration) {
		int start = start(enumeration);
		int end = end(enumeration);
		int keywordStart = find("enum", start, end);
		List<Node> children = new ArrayList<>();
		IASTName name = enumeration.getName();
		int after = keywordStart + 4;
		if(name != null && hasLocation(name) && end(name) > start(name)) {
			Node nameNode = typeName(name);
			children.add(nameNode);
			after = nameNode.end;
		}
		if(enumeration instanceof ICPPASTEnumerationSpecifier cpp && cpp.getBaseType() != null && hasLocation(cpp.getBaseType())) {
			Node base = declSpecifier(cpp.getBaseType());
			children.add(base);
			after = end(cpp.getBaseType());
		}
		int open = find("{", after, end);
		if(open >= 0) {
			List<Node> enumerators = new ArrayList<>();
			for(IASTEnumerationSpecifier.IASTEnumerator enumerator : enumeration.getEnumerators()) {
				if(!hasLocation(enumerator)) {
					continue;
				}
				List<Node> enumeratorChildren = new ArrayList<>();
				enumeratorChildren.add(leaf("identifier", start(enumerator.getName()), end(enumerator.getName())));
				if(enumerator.getValue() != null) {
					enumeratorChildren.add(expression(enumerator.getValue()));
				}
				enumerators.add(node("enumerator", enumerator, enumeratorChildren));
			}
			children.add(node("enumerator_list", open, end, enumerators));
		}
		return node("enum_specifier", keywordStart, end, children);
	}

	//declarators

	private Node declarator(IASTDeclarator declarator, NameKind kind, boolean withInitializer) {
		Node node = declaratorCore(declarator, kind, false);
		if(withInitializer) {
			IASTInitializer initializer = declarator.getInitializer();
			if(initializer != null && hasLocation(initializer)) {
				node = initDeclarator(node, initializer);
			}
		}
		return node;
	}

	private Node initDeclarator(Node declarator, IASTInitializer initializer) {
		Node value;
		if(initializer instanceof IASTEqualsInitializer equals) {
			value = initializerClause(equals.getInitializerClause());
		}
		else {
			value = initializer(initializer);
		}
		return node("init_declarator", declarator.start, end(initializer), declarator, value);
	}

	//the declarator without the initializer
	private Node declaratorCore(IASTDeclarator declarator, NameKind kind, boolean abstractDeclarator) {
		String prefix = abstractDeclarator ? "abstract_" : "";
		Node core = null;
		IASTDeclarator nested = declarator.getNestedDeclarator();
		IASTName name = declarator.getName();
		if(nested != null && hasLocation(nested)) {
			Node inner = declaratorCore(nested, kind, abstractDeclarator);
			int open = findLast("(", start(declarator), inner == null ? start(nested) : inner.start);
			int close = find(")", inner == null ? end(nested) : inner.end, end(declarator));
			if(open < 0) {
				open = start(nested);
			}
			core = node(prefix + "parenthesized_declarator", open, close < 0 ? end(nested) : close + 1, inner);
		}
		else if(name != null && hasLocation(name) && end(name) > start(name)) {
			core = declaratorName(name, kind);
		}
		if(declarator instanceof ICPPASTDeclarator cpp && cpp.declaresParameterPack()) {
			int ellipsis = find("...", start(declarator), core == null ? end(declarator) : core.start);
			if(ellipsis >= 0) {
				core = core == null ? null : node("variadic_declarator", ellipsis, core.end, core);
			}
		}
		if(declarator instanceof IASTStandardFunctionDeclarator function) {
			core = functionDeclarator(function, core, abstractDeclarator);
		}
		else if(declarator instanceof IASTArrayDeclarator array) {
			for(IASTArrayModifier modifier : array.getArrayModifiers()) {
				if(!hasLocation(modifier)) {
					continue;
				}
				IASTExpression size = modifier.getConstantExpression();
				core = node(prefix + "array_declarator", core == null ? start(modifier) : core.start, end(modifier), core, size == null ? null : expression(size));
				wrapModifiers(core);
			}
		}
		IASTPointerOperator[] operators = declarator.getPointerOperators();
		for(int i = operators.length - 1; i >= 0; i--) {
			IASTPointerOperator operator = operators[i];
			if(!hasLocation(operator)) {
				continue;
			}
			String type = operator instanceof ICPPASTReferenceOperator ? prefix + "reference_declarator" : prefix + "pointer_declarator";
			Node scope = null;
			if(operator instanceof ICPPASTPointerToMember member) {
				scope = typeName(member.getName());
			}
			core = node(type, start(operator), core == null ? end(operator) : core.end, scope, core);
			renameTokens(core, "__restrict", "ms_restrict_modifier");
			wrapTokens(core, Set.of("ms_restrict_modifier"), "ms_pointer_modifier");
			wrapModifiers(core);
		}
		return core;
	}

	private Node declaratorName(IASTName name, NameKind kind) {
		return name(name, kind);
	}

	private Node functionDeclarator(IASTStandardFunctionDeclarator function, Node core, boolean abstractDeclarator) {
		IASTParameterDeclaration[] parameters = function.getParameters();
		int searchStart = core == null ? start(function) : core.end;
		int open = find("(", searchStart, end(function));
		if(open < 0) {
			open = searchStart;
		}
		int close = matching(open, end(function));
		List<Node> parameterNodes = new ArrayList<>();
		for(IASTParameterDeclaration parameter : parameters) {
			if(hasLocation(parameter)) {
				parameterNodes.add(parameterDeclaration(parameter));
			}
		}
		Node parameterList = node("parameter_list", open, close + 1, parameterNodes);
		List<Node> children = new ArrayList<>();
		children.add(core);
		children.add(parameterList);
		int end = close + 1;
		if(function instanceof ICPPASTFunctionDeclarator cpp) {
			int limit = end(function);
			if(cpp.isPureVirtual()) {
				int equals = findLast("=", close + 1, limit);
				if(equals >= 0) {
					limit = equals;
				}
			}
			IASTTypeId trailing = cpp.getTrailingReturnType();
			if(trailing != null && hasLocation(trailing)) {
				int arrow = findLast("->", close + 1, start(trailing));
				Node returnType = typeDescriptor(trailing);
				children.add(node("trailing_return_type", arrow, returnType.end, returnType));
			}
			IASTExpression noexceptExpression = cpp.getNoexceptExpression();
			int noexceptStart = find("noexcept", close + 1, limit);
			if(noexceptStart >= 0) {
				if(noexceptExpression != null && hasLocation(noexceptExpression) && noexceptExpression != ICPPASTFunctionDeclarator.NOEXCEPT_DEFAULT) {
					int noexceptClose = find(")", end(noexceptExpression), limit);
					children.add(node("noexcept", noexceptStart, noexceptClose + 1, expression(noexceptExpression)));
				}
				else {
					children.add(wrap("noexcept", leaf("noexcept", noexceptStart, noexceptStart + 8)));
				}
			}
			int throwStart = find("throw", close + 1, limit);
			if(throwStart >= 0) {
				int throwOpen = find("(", throwStart, limit);
				int throwClose = matching(throwOpen, limit);
				List<Node> types = new ArrayList<>();
				for(IASTTypeId type : cpp.getExceptionSpecification()) {
					if(hasLocation(type)) {
						types.add(typeDescriptor(type));
					}
				}
				children.add(node("throw_specifier", throwStart, throwClose + 1, types));
			}
			end = suffixEnd(close + 1, limit, trailing != null && hasLocation(trailing) ? end(trailing) : -1);
		}
		if(core != null && core.type.equals("operator_cast")) {
			//operator T() const is an operator_cast with an abstract function declarator
			children.remove(core);
			Node declarator = node("abstract_function_declarator", open, end, children);
			wrapTokens(declarator, Set.of("const", "volatile", "mutable", "constexpr"), "type_qualifier");
			wrapTokens(declarator, Set.of("&", "&&"), "ref_qualifier");
			wrapTokens(declarator, Set.of("override", "final"), "virtual_specifier");
			List<Node> castChildren = new ArrayList<>(core.children);
			castChildren.add(declarator);
			return node("operator_cast", core.start, end, castChildren);
		}
		Node node = node(abstractDeclarator ? "abstract_function_declarator" : "function_declarator", core == null ? open : core.start, end, children);
		wrapTokens(node, Set.of("const", "volatile", "mutable", "constexpr"), "type_qualifier");
		wrapTokens(node, Set.of("&", "&&"), "ref_qualifier");
		wrapTokens(node, Set.of("override", "final"), "virtual_specifier");
		return node;
	}

	private static final Set<String> FUNCTION_SUFFIXES = Set.of("const", "volatile", "&", "&&", "override", "final", "mutable", "constexpr", "consteval");

	//the end of the qualifiers and specifiers after the parameters of a function declarator
	private int suffixEnd(int offset, int limit, int trailingEnd) {
		int end = offset;
		int position = skip(offset, limit);
		while(position < limit) {
			int tokenEnd = Math.min(tokenEnd(position, limit), limit);
			String token = content.substring(position, tokenEnd);
			if(FUNCTION_SUFFIXES.contains(token)) {
				end = tokenEnd;
			}
			else if(token.equals("noexcept") || token.equals("throw")) {
				end = tokenEnd;
				int next = skip(tokenEnd, limit);
				if(next < limit && content.charAt(next) == '(') {
					end = matching(next, limit) + 1;
					tokenEnd = end;
				}
			}
			else if(token.equals("->") && trailingEnd > position) {
				end = trailingEnd;
				tokenEnd = trailingEnd;
			}
			else if(token.equals("[") && content.startsWith("[[", position)) {
				end = matching(position, limit) + 1;
				tokenEnd = end;
			}
			else {
				break;
			}
			position = skip(tokenEnd, limit);
		}
		return end;
	}

	private Node parameterDeclaration(IASTParameterDeclaration parameter) {
		IASTDeclarator declarator = parameter.getDeclarator();
		List<Node> children = new ArrayList<>();
		children.add(declSpecifier(parameter.getDeclSpecifier()));
		String type = "parameter_declaration";
		if(declarator != null && hasLocation(declarator)) {
			boolean pack = declarator instanceof ICPPASTDeclarator cpp && cpp.declaresParameterPack();
			boolean named = declarator.getName() != null && end(declarator.getName()) > start(declarator.getName()) || declarator.getNestedDeclarator() != null;
			Node core = declaratorCore(declarator, NameKind.IDENTIFIER, !named);
			if(core != null) {
				children.add(core);
			}
			IASTInitializer initializer = declarator.getInitializer();
			if(initializer != null && hasLocation(initializer)) {
				type = "optional_parameter_declaration";
				if(initializer instanceof IASTEqualsInitializer equals) {
					children.add(initializerClause(equals.getInitializerClause()));
				}
				else {
					children.add(initializer(initializer));
				}
			}
			else if(pack) {
				type = "variadic_parameter_declaration";
			}
		}
		Node node = node(type, parameter, children);
		wrapModifiers(node);
		return node;
	}

	//names

	private Node typeName(IASTName name) {
		return name(name, NameKind.TYPE);
	}

	private Node name(IASTName name, NameKind kind) {
		if(name == null || !hasLocation(name) || end(name) <= start(name)) {
			return null;
		}
		if(name instanceof ICPPASTQualifiedName qualified) {
			return qualifiedName(qualified, kind);
		}
		if(name instanceof ICPPASTTemplateId template) {
			return templateId(template, kind);
		}
		if(name instanceof ICPPASTOperatorName) {
			return operatorName(start(name), end(name));
		}
		if(name instanceof ICPPASTConversionName conversion) {
			int keyword = start(name);
			Node type = typeDescriptor(conversion.getTypeId());
			return node("operator_cast", keyword, type.end, inline(type.children));
		}
		int start = start(name);
		int end = end(name);
		String text = content.substring(start, end);
		if(text.startsWith("~")) {
			int identifier = skip(start + 1, end);
			return node("destructor_name", start, end, leaf("identifier", identifier, end));
		}
		return leaf(nameType(text, kind), start, end);
	}

	private static String nameType(String text, NameKind kind) {
		return switch(kind) {
			case TYPE -> PRIMITIVE_TYPES.contains(text) ? "primitive_type" : "type_identifier";
			case FIELD -> "field_identifier";
			case NAMESPACE -> "namespace_identifier";
			case LABEL -> "statement_identifier";
			default -> "identifier";
		};
	}

	private Node operatorName(int start, int end) {
		List<Node> tokens = tokens(start, end);
		List<Node> children = new ArrayList<>();
		for(int i = 0; i < tokens.size(); i++) {
			Node token = tokens.get(i);
			if((token.type.equals("(") || token.type.equals("[")) && i + 1 < tokens.size() && tokens.get(i + 1).type.equals(token.type.equals("(") ? ")" : "]")) {
				children.add(leaf(token.type + tokens.get(i + 1).type, token.start, tokens.get(i + 1).end));
				i++;
			}
			else {
				children.add(token);
			}
		}
		Node node = new Node("operator_name");
		node.start = start;
		node.end = end;
		for(Node child : children) {
			node.add(child);
		}
		return node;
	}

	private Node qualifiedName(ICPPASTQualifiedName qualified, NameKind kind) {
		ICPPASTNameSpecifier[] qualifier = qualified.getQualifier();
		Node node = name(qualified.getLastName(), kind == NameKind.NAMESPACE ? NameKind.NAMESPACE : kind);
		if(node == null) {
			return leaf(nameType("", kind), start(qualified), end(qualified));
		}
		if(node.type.equals("primitive_type")) {
			node = leaf("type_identifier", node.start, node.end);
		}
		for(int i = qualifier.length - 1; i >= 0; i--) {
			Node scope = scope(qualifier[i]);
			node = node("qualified_identifier", scope.start, node.end, scope, node);
		}
		if(qualified.isFullyQualified()) {
			int separator = find("::", start(qualified), node.start);
			if(separator >= 0) {
				node = node("qualified_identifier", separator, node.end, node);
			}
		}
		return node;
	}

	private Node scope(ICPPASTNameSpecifier specifier) {
		if(specifier instanceof ICPPASTTemplateId template) {
			return templateId(template, NameKind.TYPE);
		}
		if(specifier instanceof ICPPASTDecltypeSpecifier decltype) {
			return node("decltype", specifier, expression(decltype.getDecltypeExpression()));
		}
		return leaf("namespace_identifier", start(specifier), end(specifier));
	}

	private Node templateId(ICPPASTTemplateId template, NameKind kind) {
		IASTName templateName = template.getTemplateName();
		String type = switch(kind) {
			case TYPE, NAMESPACE -> "template_type";
			case FIELD -> "template_method";
			default -> "template_function";
		};
		Node name = name(templateName, kind == NameKind.NAMESPACE ? NameKind.TYPE : kind);
		if(name == null) {
			return leaf(nameType("", kind), start(template), end(template));
		}
		if(name.type.equals("primitive_type")) {
			name = leaf("type_identifier", name.start, name.end);
		}
		int open = find("<", name.end, end(template));
		List<Node> arguments = new ArrayList<>();
		Integer maskedEnd = source.maskedArguments.get(open + 1);
		if(maskedEnd != null) {
			arguments.addAll(templateArguments(open + 1, maskedEnd));
		}
		else {
			for(IASTNode argument : template.getTemplateArguments()) {
				if(hasLocation(argument)) {
					arguments.add(templateArgument(argument));
				}
			}
		}
		Node list = node("template_argument_list", open, end(template), arguments);
		return node(type, template, name, list);
	}

	//the template arguments between the offsets, parsed again in the arguments of a template type
	private List<Node> templateArguments(int start, int end) {
		List<Node> nodes = new ArrayList<>();
		String prefix = templatePreamble(start, end, Set.of()) + "template<class... __rm_t__> struct __rm_arguments__; __rm_arguments__<";
		String code = prefix + withoutMacroDefinitions(start, end) + "> __rm_variable__;";
		Source previous = source;
		try {
			IASTTranslationUnit translationUnit = CppLanguage.getDefault().getASTTranslationUnit(FileContent.create("__rm_arguments__.cpp", code.toCharArray()), new ScannerInfo(),
					IncludeFileContentProvider.getEmptyFilesProvider(), EmptyCIndex.INSTANCE, GPPLanguage.OPTION_IS_SOURCE_UNIT, new DefaultLogService());
			source = new Source(translationUnit, start - prefix.length(), true);
			IASTDeclaration[] declarations = translationUnit.getDeclarations(false);
			IASTDeclaration last = declarations.length == 0 ? null : declarations[declarations.length - 1];
			if(last instanceof IASTSimpleDeclaration declaration && declaration.getDeclSpecifier() instanceof IASTNamedTypeSpecifier named
					&& named.getName() instanceof ICPPASTTemplateId template) {
				for(IASTNode argument : template.getTemplateArguments()) {
					if(hasLocation(argument)) {
						nodes.add(templateArgument(argument));
					}
				}
			}
		}
		catch(Exception e) {
			nodes.clear();
		}
		finally {
			source = previous;
		}
		if(nodes.isEmpty()) {
			nodes.addAll(tokens(start, end));
		}
		return nodes;
	}

	private Node templateArgument(IASTNode argument) {
		if(argument instanceof IASTTypeId typeId) {
			return typeDescriptor(typeId);
		}
		if(argument instanceof IASTIdExpression id && !text(id).equals("NULL") && (unknown(id.getName()) || isType(id.getName())
				|| id.getName() instanceof ICPPASTQualifiedName qualified && qualified.getLastName() instanceof ICPPASTTemplateId)) {
			//an unknown name is a type like in tree-sitter
			Node type = typeName(id.getName());
			return type == null ? expression(id) : node("type_descriptor", type.start, type.end, type);
		}
		if(argument instanceof IASTExpression expression) {
			return expression(expression);
		}
		return generic(argument);
	}

	//types

	private Node typeDescriptor(IASTTypeId typeId) {
		List<Node> children = new ArrayList<>();
		children.add(declSpecifier(typeId.getDeclSpecifier()));
		IASTDeclarator declarator = typeId.getAbstractDeclarator();
		if(declarator != null && hasLocation(declarator)) {
			Node core = declaratorCore(declarator, NameKind.IDENTIFIER, true);
			if(core != null) {
				children.add(core);
			}
		}
		boolean pack = declarator instanceof ICPPASTDeclarator cpp && cpp.declaresParameterPack()
				|| typeId instanceof ICPPASTTypeId cppTypeId && cppTypeId.isPackExpansion();
		int end = end(typeId);
		int childrenEnd = -1;
		for(Node child : children) {
			if(child != null && child.type.equals(INLINE)) {
				for(Node grandChild : child.children) {
					childrenEnd = Math.max(childrenEnd, grandChild.end);
				}
			}
			else if(child != null) {
				childrenEnd = Math.max(childrenEnd, child.end);
			}
		}
		//the range of the type ids in template arguments may include the closing angle bracket
		if(childrenEnd > 0 && childrenEnd < end && !pack) {
			end = childrenEnd;
		}
		Node node = node("type_descriptor", start(typeId), end, children);
		wrapModifiers(node);
		if(pack) {
			int ellipsis = findLast("...", node.start, node.end);
			if(ellipsis >= 0) {
				node.children.removeIf(c -> c.type.equals("..."));
				node.end = previousTokenEnd(node.start, ellipsis);
				return node("parameter_pack_expansion", node.start, ellipsis + 3, node);
			}
		}
		return node;
	}

	//initializers

	private Node initializer(IASTInitializer initializer) {
		if(initializer instanceof IASTEqualsInitializer equals) {
			return initializerClause(equals.getInitializerClause());
		}
		if(initializer instanceof ICPPASTConstructorInitializer constructor) {
			List<Node> arguments = new ArrayList<>();
			for(IASTInitializerClause argument : constructor.getArguments()) {
				arguments.add(initializerClause(argument));
			}
			return node("argument_list", constructor, arguments);
		}
		if(initializer instanceof IASTInitializerList list) {
			return initializerList(list);
		}
		if(initializer instanceof ICPPASTDesignatedInitializer designated) {
			return designatedInitializer(designated);
		}
		return generic(initializer);
	}

	private Node initializerClause(IASTInitializerClause clause) {
		if(clause instanceof IASTExpression expression) {
			return expression(expression);
		}
		if(clause instanceof IASTInitializer initializer) {
			return initializer(initializer);
		}
		return generic(clause);
	}

	private Node initializerList(IASTInitializerList list) {
		List<Node> clauses = new ArrayList<>();
		for(IASTInitializerClause clause : list.getClauses()) {
			if(hasLocation(clause)) {
				clauses.add(initializerClause(clause));
			}
		}
		return node("initializer_list", list, clauses);
	}

	private Node designatedInitializer(ICPPASTDesignatedInitializer designated) {
		List<Node> children = new ArrayList<>();
		for(ICPPASTDesignator designator : designated.getDesignators()) {
			if(designator instanceof ICPPASTFieldDesignator field) {
				children.add(node("field_designator", designator, leaf("field_identifier", start(field.getName()), end(field.getName()))));
			}
			else {
				children.add(generic(designator));
			}
		}
		children.add(initializerClause(designated.getOperand()));
		return node("initializer_pair", designated, children);
	}

	//statements

	private Node statement(IASTStatement statement) {
		if(statement == null || !hasLocation(statement)) {
			return null;
		}
		if(direct) {
			//the statement whose code parsed again has errors
			direct = false;
			return statementKind(statement);
		}
		//NAME(...) name; is not valid C++ unless NAME is a macro, which CDT reads as a type with a parenthesized declarator
		Node macroTypeDeclaration = statement instanceof IASTDeclarationStatement ? macroTypeDeclaration(start(statement), end(statement), Context.BLOCK) : null;
		if(macroTypeDeclaration != null) {
			return macroTypeDeclaration;
		}
		Set<String> functions = statement instanceof IASTCompoundStatement ? new LinkedHashSet<>() : misparsedCalls(statement, true, new LinkedHashSet<>());
		functions.removeAll(source.functions);
		if(!functions.isEmpty()) {
			Set<String> allFunctions = new LinkedHashSet<>(source.functions);
			allFunctions.addAll(functions);
			return single(reparse(start(statement), end(statement), Context.BLOCK, allFunctions), statement);
		}
		if((reparse(statement) || !source.reparsed && misparsedTemplate(statement, true)) && !(statement instanceof IASTCompoundStatement)) {
			if(source.reparsed) {
				//the call of a macro in a function parsed again, i.e., TF_ASSERT_OK_AND_ASSIGN(auto x, f()); in TEST(A, B) {...}
				Node call = macroCallStatement(start(statement), end(statement));
				return call != null ? call : error(start(statement), end(statement));
			}
			if(statement instanceof IASTDeclarationStatement declarationStatement && isFunctionWithMacroFreeHeader(declarationStatement.getDeclaration())) {
				return declaration(declarationStatement.getDeclaration(), Context.BLOCK);
			}
			List<Node> nodes = reparse(start(statement), end(statement), Context.BLOCK);
			//a statement with a body that CDT parsed is converted when the code parsed again has errors, so that its body is converted
			if(failed(nodes) && !problem(statement) && hasNestedStatement(statement)) {
				return statementWithoutReparse(statement);
			}
			if(failed(nodes)) {
				Node call = macroCallStatement(start(statement), end(statement));
				if(call != null) {
					return call;
				}
			}
			return single(nodes, statement);
		}
		return statementKind(statement);
	}

	private Node statementKind(IASTStatement statement) {
		if(statement instanceof IASTCompoundStatement compound) {
			return compoundStatement(compound);
		}
		if(statement instanceof IASTExpressionStatement expressionStatement) {
			IASTExpression expression = expressionStatement.getExpression();
			if(expression instanceof IASTUnaryExpression unary && unary.getOperator() == IASTUnaryExpression.op_throw) {
				return node("throw_statement", statement, unary.getOperand() == null ? null : expression(unary.getOperand()));
			}
			return node("expression_statement", statement, expression(expression));
		}
		if(statement instanceof IASTDeclarationStatement declarationStatement) {
			return declaration(declarationStatement.getDeclaration(), Context.BLOCK);
		}
		if(statement instanceof IASTIfStatement ifStatement) {
			return ifStatement(ifStatement);
		}
		if(statement instanceof IASTReturnStatement returnStatement) {
			IASTInitializerClause argument = returnStatement.getReturnArgument();
			return node("return_statement", statement, argument == null ? null : initializerClause(argument));
		}
		if(statement instanceof IASTNullStatement) {
			return node("expression_statement", statement);
		}
		if(statement instanceof ICPPASTRangeBasedForStatement rangeFor) {
			return rangeBasedFor(rangeFor);
		}
		if(statement instanceof IASTForStatement forStatement) {
			return forStatement(forStatement);
		}
		if(statement instanceof IASTWhileStatement whileStatement) {
			IASTNode condition = whileStatement instanceof ICPPASTWhileStatement cpp && cpp.getConditionDeclaration() != null ? cpp.getConditionDeclaration() : whileStatement.getCondition();
			Node body = statement(whileStatement.getBody());
			return node("while_statement", statement, conditionClause(start(statement), condition, null, body == null ? end(statement) : body.start), body);
		}
		if(statement instanceof IASTDoStatement doStatement) {
			Node body = statement(doStatement.getBody());
			Node condition = expression(doStatement.getCondition());
			int open = findLast("(", body.end, condition.start);
			int close = find(")", condition.end, end(statement));
			Node parenthesized = node("parenthesized_expression", open, close + 1, condition);
			return node("do_statement", statement, body, parenthesized);
		}
		if(statement instanceof IASTSwitchStatement switchStatement) {
			IASTNode controller = switchStatement instanceof ICPPASTSwitchStatement cpp && cpp.getControllerDeclaration() != null ? cpp.getControllerDeclaration() : switchStatement.getControllerExpression();
			IASTStatement initializer = switchStatement instanceof ICPPASTSwitchStatement cpp ? cpp.getInitializerStatement() : null;
			Node body = switchStatement.getBody() instanceof IASTCompoundStatement compound ? compoundStatement(compound) : statement(switchStatement.getBody());
			return node("switch_statement", statement, conditionClause(start(statement), controller, initializer, body == null ? end(statement) : body.start), body);
		}
		if(statement instanceof IASTCaseStatement caseStatement) {
			return node("case_statement", statement, expression(caseStatement.getExpression()));
		}
		if(statement instanceof IASTDefaultStatement) {
			return node("case_statement", statement);
		}
		if(statement instanceof IASTBreakStatement) {
			return node("break_statement", statement);
		}
		if(statement instanceof IASTContinueStatement) {
			return node("continue_statement", statement);
		}
		if(statement instanceof IASTGotoStatement gotoStatement) {
			return node("goto_statement", statement, name(gotoStatement.getName(), NameKind.LABEL));
		}
		if(statement instanceof IASTLabelStatement label) {
			return node("labeled_statement", statement, name(label.getName(), NameKind.LABEL), statement(label.getNestedStatement()));
		}
		if(statement instanceof ICPPASTTryBlockStatement tryStatement) {
			List<Node> children = new ArrayList<>();
			children.add(statement(tryStatement.getTryBody()));
			for(ICPPASTCatchHandler handler : tryStatement.getCatchHandlers()) {
				children.add(catchClause(handler));
			}
			return node("try_statement", statement, children);
		}
		return generic(statement);
	}

	private Node catchClause(ICPPASTCatchHandler handler) {
		Node body = statement(handler.getCatchBody());
		int open = find("(", start(handler), body.start);
		int close = findLast(")", open, body.start);
		List<Node> parameters = new ArrayList<>();
		if(handler.getDeclaration() instanceof IASTSimpleDeclaration declaration && hasLocation(declaration)) {
			List<Node> children = new ArrayList<>();
			children.add(declSpecifier(declaration.getDeclSpecifier()));
			for(IASTDeclarator declarator : declaration.getDeclarators()) {
				if(hasLocation(declarator) && end(declarator) > start(declarator)) {
					boolean named = declarator.getName() != null && end(declarator.getName()) > start(declarator.getName());
					children.add(declaratorCore(declarator, NameKind.IDENTIFIER, !named));
				}
			}
			Node parameter = node("parameter_declaration", declaration, children);
			wrapModifiers(parameter);
			parameters.add(parameter);
		}
		return node("catch_clause", handler, node("parameter_list", open, close + 1, parameters), body);
	}

	private Node compoundStatement(IASTCompoundStatement compound) {
		List<Node> children = new ArrayList<>();
		IASTStatement[] statements = compound.getStatements();
		List<IASTStatement> active = new ArrayList<>();
		for(IASTStatement statement : statements) {
			if(hasLocation(statement) && end(statement) > start(statement)) {
				active.add(statement);
			}
		}
		for(int i = 0; i < active.size(); i++) {
			IASTStatement statement = active.get(i);
			//a declaration whose type is the call of a macro, which CDT split after the call, i.e., typedef GTEST_REMOVE_REFERENCE_AND_CONST_(Container) RawContainer;
			if(statement instanceof IASTDeclarationStatement && !content.substring(start(statement), end(statement)).stripTrailing().endsWith(";")) {
				int k = i + 1;
				while(k < active.size() && !content.substring(start(statement), end(active.get(k))).stripTrailing().endsWith(";")) {
					k++;
				}
				Node macroTypeDeclaration = k < active.size() ? macroTypeDeclaration(start(statement), end(active.get(k)), Context.BLOCK) : null;
				if(macroTypeDeclaration != null) {
					children.add(macroTypeDeclaration);
					i = k;
					continue;
				}
			}
			//a macro expansion or a problem followed by the semicolon or the rest of the statement, i.e., CHECK(x); where the macro ends with a semicolon,
			//or ASSIGN_OR_RETURN(auto x, f({{a, b}}, c)); that CDT split at the argument it cannot parse, like the problem statement of the model
			int end = end(statement);
			int j = i + 1;
			if(reparse(statement) && !(statement instanceof IASTCompoundStatement)) {
				while(j < active.size() && (CppVisitor.unbalancedParentheses(content.substring(start(statement), end))
						|| !content.substring(start(statement), end).stripTrailing().endsWith(";") && !content.substring(start(statement), end).stripTrailing().endsWith("}"))
						&& (active.get(j) instanceof IASTNullStatement || active.get(j) instanceof IASTProblemHolder)) {
					end = end(active.get(j));
					j++;
				}
			}
			if(j > i + 1) {
				List<Node> nodes = reparse(start(statement), end, Context.BLOCK);
				Node call = failed(nodes) ? macroCallStatement(start(statement), end) : null;
				children.add(call != null ? call : single(nodes, statement));
				i = j - 1;
			}
			else {
				children.add(statement(statement));
			}
		}
		children.addAll(inactive(source.translationUnit, start(compound) + 1, end(compound) - 1, Context.BLOCK, active.toArray(new IASTNode[0])));
		//the body without its closing brace, which CDT gives as empty at the opening brace, i.e., TEST(A, B) { in an inactive branch
		if(end(compound) == start(compound) && content.startsWith("{", start(compound))) {
			return node("compound_statement", start(compound), start(compound) + 1, children);
		}
		return node("compound_statement", compound, children);
	}

	private Node ifStatement(IASTIfStatement ifStatement) {
		List<Node> children = new ArrayList<>();
		Node then = statement(ifStatement.getThenClause());
		IASTNode condition = ifStatement instanceof ICPPASTIfStatement cpp && cpp.getConditionDeclaration() != null ? cpp.getConditionDeclaration() : ifStatement.getConditionExpression();
		IASTStatement initializer = ifStatement instanceof ICPPASTIfStatement cpp ? cpp.getInitializerStatement() : null;
		children.add(conditionClause(start(ifStatement), condition, initializer, then == null ? end(ifStatement) : then.start));
		children.add(then);
		IASTStatement elseClause = ifStatement.getElseClause();
		if(elseClause != null && hasLocation(elseClause)) {
			Node elseNode = statement(elseClause);
			int elseStart = findLast("else", then.end, elseNode.start);
			children.add(node("else_clause", elseStart, elseNode.end, elseNode));
		}
		return node("if_statement", ifStatement, children);
	}

	private Node conditionClause(int searchStart, IASTNode condition, IASTStatement initializer, int limit) {
		int open = find("(", searchStart, limit);
		int close = findLast(")", open, limit);
		List<Node> children = new ArrayList<>();
		if(initializer != null && hasLocation(initializer)) {
			Node init = statement(initializer);
			children.add(wrap("init_statement", init));
		}
		if(condition instanceof IASTExpression expression) {
			children.add(expression(expression));
		}
		else if(condition instanceof IASTSimpleDeclaration declaration) {
			children.add(conditionDeclaration(declaration));
		}
		else if(condition != null) {
			children.add(generic(condition));
		}
		return node("condition_clause", open, close + 1, children);
	}

	//a declaration in a condition, whose declarator and initializer are in an init_declarator like in the other declarations,
	//where the variable of the model is located, i.e., int should_continue = index < 2 in for(int index = 0; int should_continue = index < 2; ++index)
	private Node conditionDeclaration(IASTSimpleDeclaration declaration) {
		List<Node> children = new ArrayList<>();
		children.add(declSpecifier(declaration.getDeclSpecifier()));
		for(IASTDeclarator declarator : declaration.getDeclarators()) {
			Node core = declaratorCore(declarator, NameKind.IDENTIFIER, false);
			IASTInitializer initializer = declarator.getInitializer();
			if(initializer != null && hasLocation(initializer)) {
				Node value = initializer(initializer);
				children.add(node("init_declarator", core.start, value.end, core, value));
			}
			else {
				children.add(core);
			}
		}
		Node node = node("declaration", declaration, children);
		wrapModifiers(node);
		return node;
	}

	private Node forStatement(IASTForStatement forStatement) {
		List<Node> children = new ArrayList<>();
		IASTStatement initializer = forStatement.getInitializerStatement();
		if(initializer instanceof IASTDeclarationStatement declarationStatement) {
			children.add(declaration(declarationStatement.getDeclaration(), Context.BLOCK));
		}
		else if(initializer instanceof IASTExpressionStatement expressionStatement) {
			children.add(expression(expressionStatement.getExpression()));
		}
		IASTNode condition = forStatement instanceof ICPPASTForStatement cpp && cpp.getConditionDeclaration() != null ? cpp.getConditionDeclaration() : forStatement.getConditionExpression();
		if(condition instanceof IASTExpression expression) {
			children.add(expression(expression));
		}
		else if(condition instanceof IASTSimpleDeclaration declaration) {
			children.add(conditionDeclaration(declaration));
		}
		if(forStatement.getIterationExpression() != null) {
			children.add(expression(forStatement.getIterationExpression()));
		}
		children.add(statement(forStatement.getBody()));
		return node("for_statement", forStatement, children);
	}

	private Node rangeBasedFor(ICPPASTRangeBasedForStatement rangeFor) {
		List<Node> children = new ArrayList<>();
		IASTDeclaration declaration = rangeFor.getDeclaration();
		if(declaration instanceof ICPPASTStructuredBindingDeclaration binding) {
			Node bindingNode = structuredBinding(binding);
			children.addAll(bindingNode.children);
		}
		else if(declaration instanceof IASTSimpleDeclaration simple) {
			children.add(declSpecifier(simple.getDeclSpecifier()));
			for(IASTDeclarator declarator : simple.getDeclarators()) {
				children.add(declaratorCore(declarator, NameKind.IDENTIFIER, false));
			}
		}
		children.add(initializerClause(rangeFor.getInitializerClause()));
		children.add(statement(rangeFor.getBody()));
		Node node = node("for_range_loop", rangeFor, children);
		wrapModifiers(node);
		return node;
	}

	//expressions

	private Node expression(IASTExpression expression) {
		if(expression == null || !hasLocation(expression)) {
			return null;
		}
		if(expression instanceof IASTProblemHolder) {
			return error(start(expression), end(expression));
		}
		if(expression instanceof IASTIdExpression id) {
			IASTName name = id.getName();
			if(text(name).equals("NULL")) {
				return wrap("null", leaf("NULL", start(name), end(name)));
			}
			return name(name, NameKind.IDENTIFIER);
		}
		if(expression instanceof IASTLiteralExpression literal) {
			return literal(literal);
		}
		if(expression instanceof IASTBinaryExpression binary) {
			Node left = expression(binary.getOperand1());
			Node right = initializerClause(binary.getInitOperand2());
			String type = isAssignment(binary.getOperator()) ? "assignment_expression" : "binary_expression";
			return node(type, expression, left, right);
		}
		if(expression instanceof IASTUnaryExpression unary) {
			return unaryExpression(unary);
		}
		if(expression instanceof IASTFunctionCallExpression call) {
			Node function = expression(call.getFunctionNameExpression());
			if(function != null && function.type.equals("identifier") && PRIMITIVE_TYPES.contains(content.substring(function.start, function.end))) {
				function = leaf("primitive_type", function.start, function.end);
			}
			int open = find("(", function == null ? start(call) : function.end, end(call));
			List<Node> arguments = new ArrayList<>();
			for(IASTInitializerClause argument : call.getArguments()) {
				if(hasLocation(argument)) {
					arguments.add(initializerClause(argument));
				}
			}
			return node("call_expression", expression, function, node("argument_list", open, end(call), arguments));
		}
		if(expression instanceof IASTFieldReference field) {
			Node owner = expression(field.getFieldOwner());
			Node name = name(field.getFieldName(), NameKind.FIELD);
			if(field instanceof ICPPASTFieldReference cpp && cpp.isTemplate()) {
				int template = findLast("template", owner.end, name.start);
				if(template >= 0) {
					name = node("dependent_name", template, name.end, name);
				}
			}
			return node("field_expression", expression, owner, name);
		}
		if(expression instanceof IASTArraySubscriptExpression subscript) {
			Node array = expression(subscript.getArrayExpression());
			Node argument = initializerClause(subscript.getArgument());
			int open = find("[", array.end, end(subscript));
			return node("subscript_expression", expression, array, node("subscript_argument_list", open, end(subscript), argument));
		}
		if(expression instanceof IASTConditionalExpression conditional) {
			return node("conditional_expression", expression, expression(conditional.getLogicalConditionExpression()),
					expression(conditional.getPositiveResultExpression()), expression(conditional.getNegativeResultExpression()));
		}
		if(expression instanceof IASTExpressionList list) {
			IASTExpression[] expressions = list.getExpressions();
			Node node = expression(expressions[expressions.length - 1]);
			for(int i = expressions.length - 2; i >= 0; i--) {
				Node left = expression(expressions[i]);
				node = node("comma_expression", left.start, node.end, left, node);
			}
			return node;
		}
		if(expression instanceof IASTCastExpression cast) {
			return castExpression(cast);
		}
		if(expression instanceof IASTTypeIdExpression typeIdExpression) {
			String keyword = switch(typeIdExpression.getOperator()) {
				case IASTTypeIdExpression.op_sizeof, IASTTypeIdExpression.op_sizeofParameterPack -> "sizeof";
				case IASTTypeIdExpression.op_alignof -> "alignof";
				default -> null;
			};
			if(typeIdExpression.getOperator() == IASTTypeIdExpression.op_typeid) {
				//typeid(T) is a call
				int open = find("(", start(expression), end(expression));
				IASTTypeId typeId = typeIdExpression.getTypeId();
				Node argument = typeId.getDeclSpecifier() instanceof IASTNamedTypeSpecifier named && (typeId.getAbstractDeclarator() == null || end(typeId.getAbstractDeclarator()) <= start(typeId.getAbstractDeclarator()))
						? name(named.getName(), NameKind.IDENTIFIER) : typeDescriptor(typeId);
				return node("call_expression", expression, leaf("identifier", start(expression), start(expression) + 6), node("argument_list", open, end(expression), argument));
			}
			IASTTypeId sizeofType = typeIdExpression.getTypeId();
			if(typeIdExpression.getOperator() == IASTTypeIdExpression.op_sizeof && sizeofType.getDeclSpecifier() instanceof IASTNamedTypeSpecifier named
					&& (sizeofType.getAbstractDeclarator() == null || end(sizeofType.getAbstractDeclarator()) <= start(sizeofType.getAbstractDeclarator()))
					&& named.getStorageClass() == IASTDeclSpecifier.sc_unspecified && !named.isConst() && !named.isVolatile() && unknown(named.getName())
					&& !(named.getName() instanceof ICPPASTQualifiedName) && !(named.getName() instanceof ICPPASTTemplateId) && !PRIMITIVE_TYPES.contains(text(named.getName()))
					&& !(named instanceof ICPPASTNamedTypeSpecifier cppNamed && cppNamed.isTypename())) {
				//sizeof(X) of an unknown X is a parenthesized expression like in tree-sitter
				int open = find("(", start(expression), end(expression));
				return node("sizeof_expression", expression, node("parenthesized_expression", open, end(expression), name(named.getName(), NameKind.IDENTIFIER)));
			}
			Node type = typeDescriptor(typeIdExpression.getTypeId());
			if(typeIdExpression.getOperator() == IASTTypeIdExpression.op_sizeofParameterPack) {
				return node("sizeof_expression", expression, leaf("identifier", type.start, type.end));
			}
			if(keyword == null) {
				return generic(expression);
			}
			return node(keyword + "_expression", expression, type);
		}
		if(expression instanceof ICPPASTNewExpression newExpression) {
			return newExpression(newExpression);
		}
		if(expression instanceof ICPPASTDeleteExpression deleteExpression) {
			return node("delete_expression", expression, expression(deleteExpression.getOperand()));
		}
		if(expression instanceof ICPPASTLambdaExpression lambda) {
			return lambdaExpression(lambda);
		}
		if(expression instanceof ICPPASTSimpleTypeConstructorExpression constructor) {
			Node type = declSpecifier(constructor.getDeclSpecifier());
			IASTInitializer initializer = constructor.getInitializer();
			Node value = initializer(initializer);
			if(hasLocation(constructor.getDeclSpecifier()) && start(constructor.getDeclSpecifier()) == end(constructor.getDeclSpecifier())) {
				//the type inserted before the designated initializers
				return value;
			}
			if(value != null && value.type.equals("initializer_list")) {
				return node("compound_literal_expression", expression, type, value);
			}
			if(constructor.getDeclSpecifier() instanceof IASTNamedTypeSpecifier named) {
				type = name(named.getName(), NameKind.IDENTIFIER);
			}
			return node("call_expression", expression, type, value);
		}
		if(expression instanceof IASTTypeIdInitializerExpression compound) {
			Node type = typeDescriptor(compound.getTypeId());
			return node("compound_literal_expression", expression, type, initializer(compound.getInitializer()));
		}
		if(expression instanceof ICPPASTPackExpansionExpression pack) {
			return node("parameter_pack_expansion", expression, expression(pack.getPattern()));
		}
		return generic(expression);
	}

	private static boolean isAssignment(int operator) {
		return switch(operator) {
			case IASTBinaryExpression.op_assign, IASTBinaryExpression.op_multiplyAssign, IASTBinaryExpression.op_divideAssign, IASTBinaryExpression.op_moduloAssign,
					IASTBinaryExpression.op_plusAssign, IASTBinaryExpression.op_minusAssign, IASTBinaryExpression.op_shiftLeftAssign,
					IASTBinaryExpression.op_shiftRightAssign, IASTBinaryExpression.op_binaryAndAssign, IASTBinaryExpression.op_binaryXorAssign,
					IASTBinaryExpression.op_binaryOrAssign -> true;
			default -> false;
		};
	}

	private Node unaryExpression(IASTUnaryExpression unary) {
		Node operand = unary.getOperand() == null ? null : expression(unary.getOperand());
		return switch(unary.getOperator()) {
			case IASTUnaryExpression.op_star, IASTUnaryExpression.op_amper -> node("pointer_expression", unary, operand);
			case IASTUnaryExpression.op_prefixIncr, IASTUnaryExpression.op_prefixDecr, IASTUnaryExpression.op_postFixIncr, IASTUnaryExpression.op_postFixDecr ->
				node("update_expression", unary, operand);
			case IASTUnaryExpression.op_bracketedPrimary -> node("parenthesized_expression", unary, operand);
			case IASTUnaryExpression.op_sizeof -> node("sizeof_expression", unary, operand);
			case IASTUnaryExpression.op_alignOf -> node("alignof_expression", unary, operand);
			case IASTUnaryExpression.op_throw -> node("throw_expression", unary, operand);
			case IASTUnaryExpression.op_sizeofParameterPack -> node("sizeof_expression", unary, operand);
			case IASTUnaryExpression.op_noexcept -> node("noexcept", unary, operand);
			case IASTUnaryExpression.op_typeid -> node("call_expression", unary, leaf("identifier", start(unary), start(unary) + 6), node("argument_list", find("(", start(unary), end(unary)), end(unary), operand));
			case IASTUnaryExpression.op_minus, IASTUnaryExpression.op_plus -> operand != null && operand.type.equals("number_literal") && operand.start == start(unary) + 1 ?
				leaf("number_literal", start(unary), operand.end) : node("unary_expression", unary, operand);
			default -> node("unary_expression", unary, operand);
		};
	}

	private Node castExpression(IASTCastExpression cast) {
		Node type = typeDescriptor(cast.getTypeId());
		Node operand = expression(cast.getOperand());
		if(cast.getOperator() == IASTCastExpression.op_cast) {
			return node("cast_expression", cast, type, operand);
		}
		//static_cast<T>(x) is a call of a template function
		int keywordStart = start(cast);
		int keywordEnd = tokenEnd(keywordStart, end(cast));
		int open = find("<", keywordEnd, type.start);
		int close = find(">", type.end, end(cast));
		Node arguments = node("template_argument_list", open, close + 1, type);
		Node function = node("template_function", keywordStart, close + 1, leaf("identifier", keywordStart, keywordEnd), arguments);
		int parenthesis = find("(", close + 1, end(cast));
		return node("call_expression", cast, function, node("argument_list", parenthesis, end(cast), operand));
	}

	private Node newExpression(ICPPASTNewExpression newExpression) {
		List<Node> children = new ArrayList<>();
		IASTInitializerClause[] placement = newExpression.getPlacementArguments();
		int newStart = find("new", start(newExpression), end(newExpression));
		if(placement != null && placement.length > 0) {
			List<Node> arguments = new ArrayList<>();
			for(IASTInitializerClause argument : placement) {
				arguments.add(initializerClause(argument));
			}
			int open = find("(", newStart + 3, start(placement[0]));
			int close = find(")", end(placement[placement.length - 1]), end(newExpression));
			children.add(node("argument_list", open, close + 1, arguments));
		}
		IASTTypeId typeId = newExpression.getTypeId();
		Node type = typeDescriptor(typeId);
		List<Node> typeChildren = new ArrayList<>(type.children);
		for(Node child : typeChildren) {
			if(child.type.equals("abstract_array_declarator")) {
				children.add(newDeclarator(child));
			}
			else {
				children.add(child);
			}
		}
		IASTInitializer initializer = newExpression.getInitializer();
		if(initializer != null && hasLocation(initializer)) {
			children.add(initializer(initializer));
		}
		return node("new_expression", newExpression, children);
	}

	private Node newDeclarator(Node arrayDeclarator) {
		Node node = new Node("new_declarator");
		node.start = arrayDeclarator.start;
		node.end = arrayDeclarator.end;
		for(Node child : arrayDeclarator.children) {
			node.add(child.type.equals("abstract_array_declarator") ? newDeclarator(child) : child);
		}
		return node;
	}

	private Node lambdaExpression(ICPPASTLambdaExpression lambda) {
		List<Node> children = new ArrayList<>();
		int open = start(lambda);
		int close = matching(open, end(lambda));
		List<Node> captures = new ArrayList<>();
		ICPPASTCapture[] captureNodes = lambda.getCaptures();
		if(lambda.getCaptureDefault() != ICPPASTLambdaExpression.CaptureDefault.UNSPECIFIED) {
			int defaultStart = skip(open + 1, close);
			captures.add(node("lambda_default_capture", defaultStart, defaultStart + 1, new ArrayList<>()));
		}
		for(ICPPASTCapture capture : captureNodes) {
			if(!hasLocation(capture)) {
				continue;
			}
			if(capture instanceof ICPPASTInitCapture init) {
				IASTDeclarator declarator = init.getDeclarator();
				List<Node> initChildren = new ArrayList<>();
				initChildren.add(leaf("identifier", start(declarator.getName()), end(declarator.getName())));
				if(declarator.getInitializer() != null) {
					initChildren.add(initializer(declarator.getInitializer()));
				}
				captures.add(node("lambda_capture_initializer", capture, initChildren));
			}
			else if(capture.capturesThisPointer()) {
				captures.add(inline(tokens(start(capture), end(capture))));
			}
			else {
				List<Node> captureChildren = new ArrayList<>();
				IASTName name = capture.getIdentifier();
				captureChildren.add(leaf("identifier", start(name), end(name)));
				captures.add(inline(node(INLINE, capture, captureChildren).children));
			}
		}
		children.add(node("lambda_capture_specifier", open, close + 1, captures));
		ICPPASTFunctionDeclarator declarator = lambda.getDeclarator();
		if(declarator != null && hasLocation(declarator) && end(declarator) > start(declarator)) {
			Node function = functionDeclarator(declarator, null, true);
			children.add(function);
		}
		children.add(statement(lambda.getBody()));
		return node("lambda_expression", lambda, children);
	}

	//literals

	private Node literal(IASTLiteralExpression literal) {
		int start = start(literal);
		int end = end(literal);
		//__FILE__ and __LINE__ are expanded by CDT
		String text = content.substring(start, end);
		if(!text.isEmpty() && isIdentifierStart(text.charAt(0)) && literalPrefix(start, end) < 0 && !KEYWORDS.contains(text) && text.chars().allMatch(c -> isIdentifierPart((char) c))) {
			return leaf("identifier", start, end);
		}
		return switch(literal.getKind()) {
			case IASTLiteralExpression.lk_integer_constant, IASTLiteralExpression.lk_float_constant -> numberLiteral(start, end);
			case IASTLiteralExpression.lk_char_constant -> charLiteral(start, end);
			case IASTLiteralExpression.lk_string_literal -> strings(start, end);
			case IASTLiteralExpression.lk_this -> leaf("this", start, end);
			case IASTLiteralExpression.lk_true, IASTLiteralExpression.lk_false -> leaf("boolean_literal", start, end);
			case IASTLiteralExpression.lk_nullptr -> wrap("null", leaf("nullptr", start, end));
			default -> leaf("number_literal", start, end);
		};
	}

	private Node numberLiteral(int start, int end) {
		String text = content.substring(start, end);
		int suffix = text.indexOf('_');
		if(suffix > 0 && !text.startsWith("0x") && !text.startsWith("0X")) {
			return node("user_defined_literal", start, end, leaf("number_literal", start, start + suffix), leaf("literal_suffix", start + suffix, end));
		}
		return leaf("number_literal", start, end);
	}

	//one string literal or concatenated strings
	private Node strings(int start, int end) {
		List<Node> tokens = tokens(start, end);
		if(tokens.size() == 1) {
			return tokens.get(0);
		}
		return node("concatenated_string", start, end, tokens);
	}

	private Node stringLiteral(int start, int end) {
		int quote = content.indexOf('"', start);
		if(quote < 0 || quote >= end) {
			return leaf("string_literal", start, end);
		}
		if(quote > start && content.charAt(quote - 1) == 'R') {
			return rawStringLiteral(start, quote, end);
		}
		Node node = new Node("string_literal");
		node.start = start;
		node.end = end;
		node.add(leaf(content.substring(start, quote + 1), start, quote + 1));
		int contentEnd = end - 1;
		if(contentEnd <= quote || content.charAt(contentEnd) != '"') {
			contentEnd = end;
		}
		int offset = quote + 1;
		int pieceStart = offset;
		while(offset < contentEnd) {
			if(content.charAt(offset) == '\\') {
				if(offset > pieceStart) {
					node.add(leaf("string_content", pieceStart, offset));
				}
				int escapeEnd = escapeEnd(offset, contentEnd);
				node.add(leaf("escape_sequence", offset, escapeEnd));
				offset = escapeEnd;
				pieceStart = offset;
			}
			else {
				offset++;
			}
		}
		if(contentEnd > pieceStart) {
			node.add(leaf("string_content", pieceStart, contentEnd));
		}
		if(contentEnd < end) {
			node.add(leaf("\"", contentEnd, end));
		}
		return node;
	}

	private Node rawStringLiteral(int start, int quote, int end) {
		Node node = new Node("raw_string_literal");
		node.start = start;
		node.end = end;
		node.add(leaf(content.substring(start, quote + 1), start, quote + 1));
		int open = content.indexOf('(', quote);
		if(open < 0 || open >= end) {
			return node;
		}
		String delimiter = content.substring(quote + 1, open);
		if(!delimiter.isEmpty()) {
			node.add(leaf("raw_string_delimiter", quote + 1, open));
		}
		node.add(leaf("(", open, open + 1));
		int close = end - 2 - delimiter.length();
		if(close > open + 1) {
			node.add(leaf("raw_string_content", open + 1, close));
		}
		node.add(leaf(")", close, close + 1));
		if(!delimiter.isEmpty()) {
			node.add(leaf("raw_string_delimiter", close + 1, close + 1 + delimiter.length()));
		}
		node.add(leaf("\"", end - 1, end));
		return node;
	}

	private int escapeEnd(int offset, int limit) {
		if(offset + 1 >= limit) {
			return limit;
		}
		char next = content.charAt(offset + 1);
		int end = offset + 2;
		if(next == 'x') {
			while(end < limit && end < offset + 6 && Character.digit(content.charAt(end), 16) >= 0) {
				end++;
			}
		}
		else if(next == 'u' || next == 'U') {
			int digits = next == 'u' ? 4 : 8;
			while(end < limit && end < offset + 2 + digits && Character.digit(content.charAt(end), 16) >= 0) {
				end++;
			}
		}
		else if(Character.isDigit(next)) {
			while(end < limit && end < offset + 4 && Character.isDigit(content.charAt(end))) {
				end++;
			}
		}
		else if(next == '\r' && end < limit && content.charAt(end) == '\n') {
			end++;
		}
		return end;
	}

	private Node charLiteral(int start, int end) {
		int quote = content.indexOf('\'', start);
		Node node = new Node("char_literal");
		node.start = start;
		node.end = end;
		node.add(leaf(content.substring(start, quote + 1), start, quote + 1));
		int contentEnd = end - 1;
		int offset = quote + 1;
		while(offset < contentEnd) {
			if(content.charAt(offset) == '\\') {
				int escapeEnd = escapeEnd(offset, contentEnd);
				node.add(leaf("escape_sequence", offset, escapeEnd));
				offset = escapeEnd;
			}
			else {
				int next = offset + Character.charCount(content.codePointAt(offset));
				node.add(leaf("character", offset, next));
				offset = next;
			}
		}
		node.add(leaf("'", contentEnd, end));
		return node;
	}

	//any other node, with its children and tokens
	private Node generic(IASTNode ast) {
		if(ast == null || !hasLocation(ast)) {
			return null;
		}
		List<Node> children = new ArrayList<>();
		for(IASTNode child : ast.getChildren()) {
			if(hasLocation(child)) {
				if(child instanceof IASTExpression expression) {
					children.add(expression(expression));
				}
				else if(child instanceof IASTStatement statement) {
					children.add(statement(statement));
				}
				else if(child instanceof IASTName name) {
					children.add(name(name, NameKind.IDENTIFIER));
				}
				else if(child instanceof IASTTypeId typeId) {
					children.add(typeDescriptor(typeId));
				}
				else {
					children.add(generic(child));
				}
			}
		}
		String type = ast.getClass().getSimpleName().replace("CPPAST", "").replace("CAST", "");
		return node(type, ast, children);
	}

	//preprocessor directives

	private void addDirectives(Node root) {
		List<Node> leaves = new ArrayList<>();
		collectLeaves(root, leaves);
		Deque<Integer> open = new ArrayDeque<>();
		for(var entry : directives.entrySet()) {
			Node directive = directive(entry.getKey(), entry.getValue());
			if(directive.type.equals("#if") || directive.type.equals("#ifdef") || directive.type.equals("#ifndef")) {
				open.push(directive.start);
			}
			else if(directive.type.equals("#endif") && !open.isEmpty()) {
				conditionalStarts.put(directive.start, open.pop());
			}
			insertExtra(root, leaves, directive);
		}
	}

	private Node directive(int start, int end) {
		int nameStart = skip(start + 1, end);
		int nameEnd = nameStart;
		while(nameEnd < end && Character.isLetter(content.charAt(nameEnd))) {
			nameEnd++;
		}
		String name = content.substring(nameStart, nameEnd);
		int lineEnd = end < length && content.charAt(end) == '\r' ? end + 1 : end;
		int nodeEnd = lineEnd < length && content.charAt(lineEnd) == '\n' ? lineEnd + 1 : lineEnd;
		Node keyword = leaf("#" + name, start, nameEnd);
		switch(name) {
			case "include", "include_next", "import" -> {
				int pathStart = skip(nameEnd, end);
				int pathEnd = previousTokenEnd(pathStart, end);
				Node path;
				if(pathStart < end && content.charAt(pathStart) == '<') {
					int close = content.indexOf('>', pathStart);
					path = leaf("system_lib_string", pathStart, close < 0 || close >= end ? pathEnd : close + 1);
				}
				else if(pathStart < end && content.charAt(pathStart) == '"') {
					path = stringLiteral(pathStart, tokenEnd(pathStart, end));
				}
				else {
					path = node("identifier", pathStart, pathEnd, new ArrayList<>());
					path.leaf = true;
				}
				Node node = new Node("preproc_include");
				node.start = start;
				node.end = nodeEnd;
				node.add(keyword);
				node.add(path);
				return node;
			}
			case "define" -> {
				int identifierStart = skip(nameEnd, end);
				int identifierEnd = identifierStart;
				while(identifierEnd < end && isIdentifierPart(content.charAt(identifierEnd))) {
					identifierEnd++;
				}
				Node node = new Node("preproc_def");
				node.start = start;
				node.end = nodeEnd;
				node.add(keyword);
				node.add(leaf("identifier", identifierStart, identifierEnd));
				int argumentStart = identifierEnd;
				if(identifierEnd < end && content.charAt(identifierEnd) == '(') {
					node = new Node("preproc_function_def");
					node.start = start;
					node.end = nodeEnd;
					node.add(keyword);
					node.add(leaf("identifier", identifierStart, identifierEnd));
					int close = content.indexOf(')', identifierEnd);
					if(close < 0 || close >= end) {
						close = end - 1;
					}
					Node parameters = new Node("preproc_params");
					parameters.start = identifierEnd;
					parameters.end = close + 1;
					for(Node token : tokens(identifierEnd, close + 1)) {
						parameters.add(token);
					}
					node.add(parameters);
					argumentStart = close + 1;
				}
				Node argument = preprocArgument(argumentStart, lineEnd);
				if(argument != null) {
					node.add(argument);
				}
				return node;
			}
			case "if", "elif" -> {
				Node node = new Node(name.equals("if") ? "#if" : "#elif");
				node.start = start;
				node.end = lineEnd < length ? lineEnd + 1 : lineEnd;
				node.add(keyword);
				int conditionStart = skip(nameEnd, end);
				int conditionEnd = previousTokenEnd(conditionStart, end);
				if(conditionStart < conditionEnd) {
					node.add(new PreprocessorExpression(conditionStart, conditionEnd).parse());
				}
				if(lineEnd < length) {
					node.add(leaf("\n", lineEnd, lineEnd + 1));
				}
				return node;
			}
			case "ifdef", "ifndef", "elifdef", "elifndef" -> {
				Node node = new Node("#" + name);
				node.start = start;
				int identifierStart = skip(nameEnd, end);
				int identifierEnd = previousTokenEnd(identifierStart, end);
				node.end = Math.max(identifierEnd, nameEnd);
				node.add(keyword);
				if(identifierStart < identifierEnd) {
					node.add(leaf("identifier", identifierStart, identifierEnd));
				}
				return node;
			}
			case "else", "endif" -> {
				Node node = new Node("#" + name);
				node.start = start;
				node.end = nameEnd;
				node.add(keyword);
				return node;
			}
			default -> {
				Node node = new Node("preproc_call");
				node.start = start;
				node.end = nodeEnd;
				node.add(leaf("preproc_directive", start, nameEnd));
				Node argument = preprocArgument(nameEnd, lineEnd);
				if(argument != null) {
					node.add(argument);
				}
				return node;
			}
		}
	}

	//the text after the directive name until the end of the line or a comment, which is a comment node like in the model,
	//i.e., warning(disable : 4127) in #pragma warning(disable : 4127) // C4127: conditional expression is constant
	private Node preprocArgument(int start, int end) {
		int offset = start;
		while(offset < end && (content.charAt(offset) == ' ' || content.charAt(offset) == '\t' || content.charAt(offset) == '\\' && isLineBreak(offset + 1))) {
			offset = content.charAt(offset) == '\\' ? afterLineBreak(offset + 1) : offset + 1;
		}
		if(offset >= end) {
			return null;
		}
		int argumentEnd = offset;
		while(argumentEnd < end) {
			char c = content.charAt(argumentEnd);
			if(content.startsWith("/*", argumentEnd) || content.startsWith("//", argumentEnd)) {
				break;
			}
			if(c == '"' || c == '\'') {
				//the literal up to its closing quote, i.e., "http://example.com" in #define URL "http://example.com"
				for(argumentEnd++; argumentEnd < end && content.charAt(argumentEnd) != c; argumentEnd++) {
					if(content.charAt(argumentEnd) == '\\') {
						argumentEnd++;
					}
				}
			}
			argumentEnd++;
		}
		argumentEnd = Math.min(argumentEnd, end);
		while(argumentEnd > offset && Character.isWhitespace(content.charAt(argumentEnd - 1))) {
			argumentEnd--;
		}
		if(argumentEnd == offset) {
			return null;
		}
		return leaf("preproc_arg", offset, argumentEnd);
	}

	//the expression of #if and #elif
	private class PreprocessorExpression {
		private final List<Node> tokens;
		private int position;

		private PreprocessorExpression(int start, int end) {
			this.tokens = tokens(start, end);
		}

		private Node parse() {
			Node node = conditional();
			while(position < tokens.size()) {
				Node error = tokens.get(position++);
				node = node("ERROR", node.start, error.end, node, error);
			}
			return node;
		}

		private Node peek() {
			return position < tokens.size() ? tokens.get(position) : null;
		}

		private boolean at(String type) {
			Node token = peek();
			return token != null && token.type.equals(type);
		}

		private Node conditional() {
			Node condition = binary(0);
			if(at("?")) {
				position++;
				Node consequence = conditional();
				if(at(":")) {
					position++;
				}
				Node alternative = conditional();
				return node("conditional_expression", condition.start, alternative.end, condition, consequence, alternative);
			}
			return condition;
		}

		private static final String[][] LEVELS = {{"||"}, {"&&"}, {"|"}, {"^"}, {"&"}, {"==", "!="}, {"<", ">", "<=", ">="}, {"<<", ">>"}, {"+", "-"}, {"*", "/", "%"}};

		private Node binary(int level) {
			if(level == LEVELS.length) {
				return unary();
			}
			Node left = binary(level + 1);
			while(peek() != null && Arrays.asList(LEVELS[level]).contains(peek().type)) {
				position++;
				Node right = binary(level + 1);
				left = node("binary_expression", left.start, right.end, left, right);
			}
			return left;
		}

		private Node unary() {
			Node token = peek();
			if(token == null) {
				return error(tokens.isEmpty() ? 0 : tokens.get(tokens.size() - 1).end, tokens.isEmpty() ? 0 : tokens.get(tokens.size() - 1).end);
			}
			if(token.type.equals("!") || token.type.equals("~") || token.type.equals("-") || token.type.equals("+")) {
				position++;
				Node operand = unary();
				return node("unary_expression", token.start, operand.end, operand);
			}
			if(token.type.equals("(")) {
				position++;
				Node inner = conditional();
				int end = inner.end;
				if(at(")")) {
					end = peek().end;
					position++;
				}
				return node("parenthesized_expression", token.start, end, inner);
			}
			position++;
			if(token.type.equals("identifier") && content.substring(token.start, token.end).equals("defined")) {
				if(at("(")) {
					position++;
					Node identifier = peek();
					position++;
					int end = identifier == null ? token.end : identifier.end;
					if(at(")")) {
						end = peek().end;
						position++;
					}
					return node("preproc_defined", token.start, end, leaf("defined", token.start, token.end), identifier);
				}
				Node identifier = peek();
				position++;
				return node("preproc_defined", token.start, identifier == null ? token.end : identifier.end, leaf("defined", token.start, token.end), identifier);
			}
			if(token.type.equals("identifier") && at("(")) {
				Node open = peek();
				int depth = 0;
				int end = open.end;
				List<Node> arguments = new ArrayList<>();
				while(position < tokens.size()) {
					Node next = tokens.get(position++);
					if(next.type.equals("(")) {
						depth++;
					}
					else if(next.type.equals(")")) {
						depth--;
					}
					else if(!next.type.equals(",")) {
						arguments.add(next);
					}
					end = next.end;
					if(depth == 0) {
						break;
					}
				}
				return node("call_expression", token.start, end, token, node("argument_list", open.start, end, arguments));
			}
			return token;
		}
	}

	//the extra in the smallest node containing the leaves before and after it
	private void insertExtra(Node root, List<Node> leaves, Node extra) {
		int start = extra.start;
		int low = 0;
		int high = leaves.size();
		while(low < high) {
			int middle = (low + high) / 2;
			if(leaves.get(middle).start < start) {
				low = middle + 1;
			}
			else {
				high = middle;
			}
		}
		Node previous = low > 0 ? leaves.get(low - 1) : null;
		Node next = low < leaves.size() ? leaves.get(low) : null;
		Node parent;
		if(previous == null || next == null) {
			parent = root;
		}
		else {
			parent = commonAncestor(previous, next);
		}
		while(parent.leaf && parent.parent != null) {
			parent = parent.parent;
		}
		//a comment in a preprocessor directive
		boolean descended = true;
		while(descended) {
			descended = false;
			for(Node child : parent.children) {
				if(!child.leaf && child.start <= extra.start && child.end >= extra.end && child != extra) {
					parent = child;
					descended = true;
					break;
				}
			}
		}
		int position = 0;
		while(position < parent.children.size() && parent.children.get(position).start < start) {
			position++;
		}
		extra.parent = parent;
		parent.children.add(position, extra);
	}

	//the conditional directives and the nodes between them in preproc_if, preproc_ifdef, preproc_elif and preproc_else nodes
	private void groupConditionals(Node node) {
		for(Node child : new ArrayList<>(node.children)) {
			groupConditionals(child);
		}
		List<Node> children = node.children;
		boolean grouped = true;
		while(grouped) {
			grouped = false;
			//the innermost conditional, without conditional directives between its directives,
			//whose #if is a sibling of its #endif, i.e., not the #ifdef of #ifdef X\nextern "C" {\n#endif
			for(int i = 0; i < children.size() && !grouped; i++) {
				if(children.get(i).type.equals("#endif") && conditionalStarts.containsKey(children.get(i).start)) {
					int start = conditionalStarts.get(children.get(i).start);
					for(int open = i - 1; open >= 0; open--) {
						if(children.get(open).start == start && children.get(open).type.startsWith("#if")) {
							group(node, open, i);
							grouped = true;
							break;
						}
					}
				}
			}
		}
	}

	//the directives without their conditional, after the grouping, which can move them into the conditionals of the parent nodes
	private void ungroupedDirectives(Node node) {
		List<Node> children = node.children;
		for(Node child : children) {
			ungroupedDirectives(child);
		}
		for(int i = 0; i < children.size(); i++) {
			Node child = children.get(i);
			if(child.type.startsWith("#") && !child.leaf) {
				Node call = new Node("preproc_call");
				call.start = child.start;
				call.end = child.end;
				call.parent = node;
				Node keyword = child.first();
				call.add(leaf("preproc_directive", keyword.start, keyword.end));
				for(int k = 1; k < child.children.size(); k++) {
					Node argument = child.children.get(k);
					if(!argument.type.equals("\n")) {
						call.add(argument);
					}
				}
				children.set(i, call);
			}
		}
	}

	private void group(Node node, int open, int close) {
		List<Node> children = node.children;
		List<Node> branch = new ArrayList<>(children.subList(open, close + 1));
		Node endif = branch.get(branch.size() - 1).first();
		Node opening = branch.get(0);
		String type = opening.type.equals("#if") ? "preproc_if" : "preproc_ifdef";
		Node group = new Node(type);
		group.start = opening.start;
		group.end = endif.end;
		group.parent = node;
		for(Node child : opening.children) {
			group.add(child);
		}
		//the branches between the alternative directives
		List<Node> current = new ArrayList<>();
		Node alternative = null;
		Node target = group;
		List<Node> alternatives = new ArrayList<>();
		for(int i = 1; i < branch.size() - 1; i++) {
			Node child = branch.get(i);
			if(child.type.equals("#elif") || child.type.equals("#else") || child.type.equals("#elifdef") || child.type.equals("#elifndef")) {
				Node next = new Node(child.type.equals("#else") ? "preproc_else" : child.type.equals("#elif") ? "preproc_elif" : "preproc_elifdef");
				next.start = child.start;
				next.end = child.end;
				for(Node grandChild : child.children) {
					next.add(grandChild);
				}
				alternatives.add(next);
			}
			else {
				if(alternatives.isEmpty()) {
					group.add(child);
				}
				else {
					alternatives.get(alternatives.size() - 1).add(child);
				}
			}
		}
		//each alternative is a child of the previous one, and ends with its last child
		for(int i = alternatives.size() - 1; i >= 0; i--) {
			Node current1 = alternatives.get(i);
			if(i < alternatives.size() - 1) {
				current1.add(alternatives.get(i + 1));
			}
			Node last = current1.last();
			if(last != null) {
				current1.end = Math.max(current1.end, last.end);
			}
		}
		if(!alternatives.isEmpty()) {
			group.add(alternatives.get(0));
		}
		group.add(endif);
		for(int i = close; i >= open; i--) {
			children.remove(i);
		}
		children.add(open, group);
	}

	//comments

	private void addComments(Node root) {
		List<Node> leaves = new ArrayList<>();
		collectLeaves(root, leaves);
		leaves.sort(Comparator.comparingInt(n -> n.start));
		for(var entry : comments.entrySet()) {
			Node comment = leaf("comment", entry.getKey(), entry.getValue());
			insertExtra(root, leaves, comment);
		}
	}

	private static void collectLeaves(Node node, List<Node> leaves) {
		if(node.children.isEmpty()) {
			leaves.add(node);
		}
		for(Node child : node.children) {
			collectLeaves(child, leaves);
		}
	}

	private static Node commonAncestor(Node a, Node b) {
		int depthA = a.depth();
		int depthB = b.depth();
		while(depthA > depthB) {
			a = a.parent;
			depthA--;
		}
		while(depthB > depthA) {
			b = b.parent;
			depthB--;
		}
		while(a != b) {
			a = a.parent;
			b = b.parent;
		}
		return a;
	}

	private Tree toTree(Node node, TreeContext context) {
		boolean leaf = node.children.isEmpty();
		int start = Math.max(Math.min(node.start, length), 0);
		int end = Math.max(Math.min(node.end, length), start);
		Tree tree = leaf ? context.createTree(TypeSet.type(node.type), content.substring(start, end)) : context.createTree(TypeSet.type(node.type));
		tree.setPos(start);
		tree.setLength(end - start);
		for(Node child : node.children) {
			tree.addChild(toTree(child, context));
		}
		return tree;
	}

}
