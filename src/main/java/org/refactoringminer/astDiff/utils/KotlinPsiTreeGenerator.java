package org.refactoringminer.astDiff.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.TokenType;
import org.jetbrains.kotlin.psi.KtFile;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

/**
 * Generates the AST diff tree of a Kotlin file from the PSI tree of the Kotlin compiler, which is also used to generate the RefactoringMiner model,
 * with the node types and the structure of the tree-sitter-kotlin tree, which was used before, so that the AST diff matchers and the diffs remain the same,
 * while the ranges are the exact ranges of the PSI tree and there are no parsing errors, i.e., PSI DOT_QUALIFIED_EXPRESSION -> [receiver, CALL_EXPRESSION -> [name, VALUE_ARGUMENT_LIST]]
 * becomes call_expression -> [navigation_expression -> [receiver, navigation_suffix -> simple_identifier], call_suffix -> value_arguments].
 * The PSI nodes without a conversion rule are converted with their lowercase PSI type.
 */
public class KotlinPsiTreeGenerator {
	public static TreeContext generate(KtFile ktFile) {
		TreeContext context = new TreeContext();
		KotlinPsiTreeGenerator generator = new KotlinPsiTreeGenerator(context);
		context.setRoot(generator.file(ktFile.getNode()));
		return context;
	}

	private final TreeContext context;

	private KotlinPsiTreeGenerator(TreeContext context) {
		this.context = context;
	}

	//the modifier keywords as tree-sitter modifier -> [modifier leaf], i.e., visibility_modifier -> visibility_modifier [private]
	private static final Map<String, String> MODIFIER_TYPES = Map.ofEntries(
			Map.entry("public", "visibility_modifier"), Map.entry("private", "visibility_modifier"), Map.entry("protected", "visibility_modifier"), Map.entry("internal", "visibility_modifier"),
			Map.entry("override", "member_modifier"), Map.entry("lateinit", "member_modifier"),
			Map.entry("suspend", "function_modifier"), Map.entry("inline", "function_modifier"), Map.entry("operator", "function_modifier"), Map.entry("infix", "function_modifier"),
			Map.entry("tailrec", "function_modifier"), Map.entry("external", "function_modifier"),
			Map.entry("abstract", "inheritance_modifier"), Map.entry("open", "inheritance_modifier"), Map.entry("final", "inheritance_modifier"),
			Map.entry("data", "class_modifier"), Map.entry("enum", "class_modifier"), Map.entry("sealed", "class_modifier"), Map.entry("annotation", "class_modifier"),
			Map.entry("inner", "class_modifier"), Map.entry("value", "class_modifier"),
			Map.entry("const", "property_modifier"),
			Map.entry("vararg", "parameter_modifier"), Map.entry("noinline", "parameter_modifier"), Map.entry("crossinline", "parameter_modifier"),
			Map.entry("expect", "platform_modifier"), Map.entry("actual", "platform_modifier"),
			Map.entry("reified", "reification_modifier"), Map.entry("in", "variance_modifier"), Map.entry("out", "variance_modifier"));
	//the modifiers nested in a modifier of the same type, while the other modifiers are leaves, i.e., property_modifier [const]
	private static final Map<String, String> WRAPPED_MODIFIER_TYPES = Map.of("visibility_modifier", "visibility_modifier", "member_modifier", "member_modifier",
			"function_modifier", "function_modifier", "class_modifier", "class_modifier", "inheritance_modifier", "inherit_modifier", "variance_modifier", "covariance_keyword",
			"parameter_modifier", "parameter_modifier");
	//the binary operators as tree-sitter expression and operator types
	private static final Map<String, String[]> BINARY_OPERATORS = Map.ofEntries(
			Map.entry("==", new String[] {"equality_expression", "comparison_operator"}), Map.entry("!=", new String[] {"equality_expression", "comparison_operator"}),
			Map.entry("===", new String[] {"equality_expression", "comparison_operator"}), Map.entry("!==", new String[] {"equality_expression", "comparison_operator"}),
			Map.entry("<", new String[] {"comparison_expression", "comparison_operator"}), Map.entry(">", new String[] {"comparison_expression", "comparison_operator"}),
			Map.entry("<=", new String[] {"comparison_expression", "<="}), Map.entry(">=", new String[] {"comparison_expression", "comparison_operator"}),
			Map.entry("&&", new String[] {"conjunction_expression", "logical_operator"}), Map.entry("||", new String[] {"disjunction_expression", "logical_operator"}),
			Map.entry("+", new String[] {"additive_expression", "arithmetic_operator"}), Map.entry("-", new String[] {"additive_expression", "arithmetic_operator"}),
			Map.entry("*", new String[] {"multiplicative_expression", "arithmetic_operator"}), Map.entry("/", new String[] {"multiplicative_expression", "arithmetic_operator"}),
			Map.entry("%", new String[] {"multiplicative_expression", "%"}),
			Map.entry("?:", new String[] {"elvis_expression", "elvis_operator"}),
			Map.entry("..", new String[] {"range_expression", "range_creation_operator"}), Map.entry("..<", new String[] {"range_expression", "range_creation_operator"}),
			Map.entry("in", new String[] {"check_expression", "collection_contains"}), Map.entry("!in", new String[] {"check_expression", "collection_not_contains"}));
	private static final Map<String, String> PREFIX_OPERATORS = Map.of("-", "arithmetic_operator", "+", "arithmetic_operator", "++", "increment_operator", "--", "increment_operator");
	private static final Set<String> ASSIGNMENT_OPERATORS = Set.of("=", "+=", "-=", "*=", "/=", "%=");
	private static final Set<String> COMMENTS = Set.of("EOL_COMMENT", "BLOCK_COMMENT", "KDoc");
	private static final Set<String> QUALIFIED_EXPRESSIONS = Set.of("DOT_QUALIFIED_EXPRESSION", "SAFE_ACCESS_EXPRESSION");
	private static final Set<String> DECLARATIONS = Set.of("CLASS", "OBJECT_DECLARATION", "FUN", "PROPERTY", "TYPEALIAS", "CLASS_INITIALIZER", "SECONDARY_CONSTRUCTOR", "ENUM_ENTRY");

	// ---------------------------------------------------------------- tree construction

	private Tree node(String type, int start, int end) {
		Tree t = context.createTree(TypeSet.type(type), "");
		t.setPos(start);
		t.setLength(end - start);
		return t;
	}

	private Tree node(String type, ASTNode n) {
		return node(type, start(n), end(n));
	}

	private Tree leaf(String type, String label, int start, int end) {
		Tree t = context.createTree(TypeSet.type(type), label);
		t.setPos(start);
		t.setLength(end - start);
		return t;
	}

	private Tree leaf(String type, ASTNode n) {
		return leaf(type, n.getText(), start(n), end(n));
	}

	//the soft keywords used as identifiers are nested in the simple_identifier, as in the tree-sitter tree, i.e., simple_identifier -> value [value]
	private static final Map<String, String> SOFT_KEYWORDS = Map.of("value", "value", "get", "get", "set", "set", "data", "class_modifier", "actual", "platform_modifier", "expect", "platform_modifier");

	private Tree simpleIdentifier(ASTNode n) {
		String keyword = SOFT_KEYWORDS.get(n.getText());
		if(keyword != null) {
			Tree t = node("simple_identifier", n);
			add(t, leaf(keyword, n));
			return t;
		}
		return leaf("simple_identifier", n);
	}

	private static void add(Tree parent, Tree child) {
		if(child != null) {
			parent.addChild(child);
			child.setParent(parent);
		}
	}

	private static void addAll(Tree parent, List<Tree> children) {
		for(Tree child : children)
			add(parent, child);
	}

	//the tree-sitter node without PSI counterpart, with the range of its children
	private Tree span(String type, List<Tree> children) {
		Tree t = node(type, children.get(0).getPos(), children.get(children.size() - 1).getEndPos());
		addAll(t, children);
		return t;
	}

	private static int start(ASTNode n) {
		return n.getStartOffset();
	}

	private static int end(ASTNode n) {
		return n.getStartOffset() + n.getTextLength();
	}

	//the element type name without the language prefix, i.e., kotlin.FILE -> FILE
	private static String type(ASTNode n) {
		String name = n.getElementType().toString();
		int dot = name.lastIndexOf('.');
		return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1) : name;
	}

	//the children without whitespace and empty nodes
	private static List<ASTNode> kids(ASTNode n) {
		List<ASTNode> kids = new ArrayList<>();
		for(ASTNode c = n.getFirstChildNode(); c != null; c = c.getTreeNext()) {
			if(c.getElementType() != TokenType.WHITE_SPACE && c.getTextLength() > 0)
				kids.add(c);
		}
		return kids;
	}

	private static ASTNode kid(ASTNode n, String type) {
		if(n == null)
			return null;
		for(ASTNode c : kids(n)) {
			if(type(c).equals(type))
				return c;
		}
		return null;
	}

	private static boolean isComment(Tree t) {
		return t.getType().name.equals("line_comment") || t.getType().name.equals("multiline_comment");
	}

	private static boolean hasNewlineBefore(ASTNode n) {
		for(ASTNode p = n.getTreePrev(); p != null && (p.getElementType() == TokenType.WHITE_SPACE || isComment(p) || p.getTextLength() == 0); p = p.getTreePrev()) {
			if(p.getText().contains("\n"))
				return true;
		}
		return false;
	}

	private static boolean isComment(ASTNode n) {
		return COMMENTS.contains(type(n));
	}

	// ---------------------------------------------------------------- file, comments, imports

	private Tree file(ASTNode n) {
		Tree t = node("source_file", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "PACKAGE_DIRECTIVE" -> add(t, packageHeader(c));
				case "IMPORT_LIST" -> {
					Tree list = importList(c);
					ASTNode next = c.getTreeNext();
					while(next != null && (next.getElementType() == TokenType.WHITE_SPACE || next.getTextLength() == 0)) next = next.getTreeNext();
					if(next != null && !isComment(next) && !(DECLARATIONS.contains(type(next)) && !kids(next).isEmpty() && isComment(kids(next).get(0))))
						list.setLength(start(next) - list.getPos());
					add(t, list);
				}
				case "FILE_ANNOTATION_LIST" -> addAll(t, fileAnnotations(c));
				default -> addAll(t, declaration(c));
			}
		}
		return t;
	}

	private Tree comment(ASTNode c) {
		return leaf(type(c).equals("EOL_COMMENT") ? "line_comment" : "multiline_comment", c);
	}

	private Tree packageHeader(ASTNode n) {
		Tree t = node("package_header", n);
		for(ASTNode c : kids(n)) {
			if(type(c).equals("package"))
				add(t, leaf("package", c));
			else if(isComment(c))
				add(t, comment(c));
			else
				add(t, identifier(c));
		}
		return t;
	}

	private Tree importList(ASTNode n) {
		Tree t = node("import_list", n);
		for(ASTNode c : kids(n)) {
			if(type(c).equals("IMPORT_DIRECTIVE"))
				add(t, importHeader(c));
			else if(isComment(c))
				add(t, comment(c));
		}
		return t;
	}

	//import a.b.C, import a.b.*, import a.b.C as D, without the import keyword
	private Tree importHeader(ASTNode n) {
		Tree t = node("import_header", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "import", "DOT" -> {}
				case "MUL" -> add(t, leaf("wildcard_import", c));
				case "IMPORT_ALIAS" -> {
					Tree alias = node("import_alias", c);
					for(ASTNode a : kids(c)) {
						if(type(a).equals("IDENTIFIER"))
							add(alias, leaf("type_identifier", a));
						else if(type(a).equals("as"))
							add(alias, leaf("type_conversion_or_type_alias_keyword", a));
					}
					add(t, alias);
				}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else add(t, identifier(c));
				}
			}
		}
		return t;
	}

	//the dotted names of packages and imports, as identifier -> simple_identifier*
	private Tree identifier(ASTNode n) {
		Tree t = node("identifier", n);
		for(ASTNode name : names(n))
			add(t, simpleIdentifier(name));
		return t;
	}

	private static List<ASTNode> names(ASTNode n) {
		List<ASTNode> names = new ArrayList<>();
		if(type(n).equals("IDENTIFIER")) {
			names.add(n);
			return names;
		}
		for(ASTNode c : kids(n))
			names.addAll(names(c));
		return names;
	}

	private List<Tree> fileAnnotations(ASTNode n) {
		List<Tree> trees = new ArrayList<>();
		for(ASTNode c : kids(n)) {
			if(type(c).equals("ANNOTATION") || type(c).equals("ANNOTATION_ENTRY")) {
				//file_annotation -> [at, file, constructor_invocation | user_type], without the use_site_target of the other annotations
				Tree t = annotation(c, "file_annotation");
				for(int i = 0; i < t.getChildren().size(); i++) {
					Tree child = t.getChild(i);
					if(child.getType().name.equals("use_site_target")) {
						t.getChildren().set(i, child.getChild(0));
						child.getChild(0).setParent(t);
					}
				}
				trees.add(t);
			}
			else if(isComment(c))
				trees.add(comment(c));
		}
		return trees;
	}

	// ---------------------------------------------------------------- declarations

	//the declaration with its leading comments, which are its first children in PSI, while they precede it in the tree-sitter tree
	private List<Tree> declaration(ASTNode n) {
		List<Tree> trees = new ArrayList<>();
		if(isComment(n)) {
			trees.add(comment(n));
			return trees;
		}
		List<ASTNode> kids = kids(n);
		int first = 0;
		if(DECLARATIONS.contains(type(n))) {
			while(first < kids.size() && isComment(kids.get(first))) {
				trees.add(comment(kids.get(first)));
				first++;
			}
		}
		List<ASTNode> rest = kids.subList(first, kids.size());
		switch(type(n)) {
			case "CLASS" -> trees.add(classDeclaration(n, rest));
			case "OBJECT_DECLARATION" -> trees.add(objectDeclaration(n, rest));
			case "FUN" -> trees.add(function(n, rest));
			case "PROPERTY" -> {
				//the accessors follow the property, which ends before them, as in the tree-sitter tree
				List<ASTNode> accessors = new ArrayList<>();
				for(ASTNode c : rest) {
					if(type(c).equals("PROPERTY_ACCESSOR")) accessors.add(c);
				}
				List<ASTNode> declaration = new ArrayList<>(rest);
				declaration.removeAll(accessors);
				while(!declaration.isEmpty() && isComment(declaration.get(declaration.size() - 1))) declaration.remove(declaration.size() - 1);
				Tree property = property(n, declaration);
				if(!accessors.isEmpty() && !declaration.isEmpty()) property.setLength(end(declaration.get(declaration.size() - 1)) - property.getPos());
				trees.add(property);
				//an accessor on the same line as the property is its last child, i.e., val x get() = y
				ASTNode sameLine = !accessors.isEmpty() && !declaration.isEmpty() && !hasNewlineBefore(accessors.get(0)) ? accessors.get(0) : null;
				if(sameLine != null) {
					add(property, accessor(sameLine));
					property.setLength(end(sameLine) - property.getPos());
				}
				//the comments after the declaration, i.e., val x = 1 // comment, follow it
				if(accessors.isEmpty() && !declaration.isEmpty() && isComment(rest.get(rest.size() - 1)))
					property.setLength(end(declaration.get(declaration.size() - 1)) - property.getPos());
				for(ASTNode c : rest) {
					if(c == sameLine) continue;
					if(accessors.contains(c)) trees.add(accessor(c));
					else if(isComment(c) && !accessors.isEmpty() && start(c) > start(accessors.get(0))) trees.add(comment(c));
					else if(isComment(c) && accessors.isEmpty() && !declaration.isEmpty() && start(c) >= end(declaration.get(declaration.size() - 1))) trees.add(comment(c));
				}
			}
			case "TYPEALIAS" -> trees.add(typeAlias(n, rest));
			case "CLASS_INITIALIZER" -> trees.add(initializer(n, rest));
			case "SECONDARY_CONSTRUCTOR" -> trees.add(secondaryConstructor(n, rest));
			case "ENUM_ENTRY" -> trees.add(enumEntry(n, rest));
			case "ANNOTATED_EXPRESSION" -> {
				//the annotations of an assignment are statements preceding it, as in the tree-sitter tree, while they are prefix operators of an expression
				ASTNode inner = null;
				for(ASTNode c : kids(n)) {
					if(!type(c).equals("ANNOTATION_ENTRY") && !isComment(c)) inner = c;
				}
				Tree e = inner != null ? expression(inner) : null;
				if(e != null && e.getType().name.equals("assignment")) {
					for(ASTNode c : kids(n)) {
						if(type(c).equals("ANNOTATION_ENTRY")) trees.add(annotation(c, "class_modifier"));
					}
					trees.add(e);
				}
				else trees.add(expression(n));
			}
			case "LABELED_EXPRESSION" -> {
				//a labeled statement is a label followed by the statement, as in the tree-sitter tree, i.e., outer@ for (...)
				for(ASTNode c : kids(n)) {
					if(type(c).equals("LABEL_QUALIFIER")) trees.add(leaf("label", c));
					else trees.addAll(declaration(c));
				}
			}
			default -> trees.add(expression(n));
		}
		return trees;
	}

	//the node of a declaration starting after its leading comments
	private Tree declarationNode(String type, ASTNode n, List<ASTNode> rest) {
		return node(type, rest.isEmpty() ? start(n) : start(rest.get(0)), end(n));
	}

	private Tree classDeclaration(ASTNode n, List<ASTNode> rest) {
		Tree t = declarationNode("class_declaration", n, rest);
		for(ASTNode c : rest) {
			switch(type(c)) {
				//the fun of a fun interface is a keyword of the class declaration, as in the tree-sitter grammar,
				//and enum is a class_modifier leaf following the other modifiers, i.e., class_declaration -> [modifiers, class_modifier, type_keyword, ...]
				case "MODIFIER_LIST" -> {
					add(t, modifiersWithout(c, "fun", "enum"));
					ASTNode enumKeyword = kids(c).stream().filter(m -> type(m).equals("enum")).findFirst().orElse(null);
					if(enumKeyword != null) add(t, leaf("class_modifier", enumKeyword));
					ASTNode fun = kids(c).stream().filter(m -> type(m).equals("fun")).findFirst().orElse(null);
					if(fun != null) add(t, leaf("fun", fun));
				}
				case "class", "interface" -> add(t, leaf("type_keyword", c));
				case "IDENTIFIER" -> add(t, leaf("type_identifier", c));
				case "TYPE_PARAMETER_LIST" -> add(t, typeParameters(c));
				case "PRIMARY_CONSTRUCTOR" -> add(t, primaryConstructor(c));
				case "SUPER_TYPE_LIST" -> addAll(t, delegationSpecifiers(c));
				case "CLASS_BODY" -> add(t, classBody(c, "type_body"));
				case "COLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else add(t, generic(c));
				}
			}
		}
		return t;
	}

	private Tree objectDeclaration(ASTNode n, List<ASTNode> rest) {
		ASTNode modifiers = kid(n, "MODIFIER_LIST");
		boolean companion = kid(modifiers, "companion") != null;
		Tree t = declarationNode(companion ? "companion_object" : "object_declaration", n, rest);
		for(ASTNode c : rest) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> {
					if(companion) {
						//the companion keyword is a child of the companion object, after the other modifiers
						add(t, modifiersWithout(c, "companion"));
						add(t, leaf("companion", kid(c, "companion")));
					}
					else add(t, modifiers(c));
				}
				case "object" -> add(t, leaf("type_keyword", c));
				case "IDENTIFIER" -> add(t, leaf("type_identifier", c));
				case "SUPER_TYPE_LIST" -> addAll(t, delegationSpecifiers(c));
				case "CLASS_BODY" -> add(t, classBody(c, "type_body"));
				case "COLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else add(t, generic(c));
				}
			}
		}
		return t;
	}

	private Tree classBody(ASTNode n, String type) {
		Tree t = node(type, n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "LBRACE", "RBRACE", "SEMICOLON", "COMMA" -> {}
				default -> addAll(t, declaration(c));
			}
		}
		return t;
	}

	private Tree enumEntry(ASTNode n, List<ASTNode> rest) {
		Tree t = declarationNode("enum_entry", n, rest);
		int last = rest.size() - 1;
		while(last > 0 && (Set.of("COMMA", "SEMICOLON").contains(type(rest.get(last))) || isComment(rest.get(last)))) last--;
		t.setLength(end(rest.get(last)) - t.getPos());
		for(ASTNode c : rest) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "IDENTIFIER" -> add(t, simpleIdentifier(c));
				case "INITIALIZER_LIST" -> {
					for(ASTNode i : kids(c)) {
						ASTNode args = kid(i, "VALUE_ARGUMENT_LIST");
						if(args != null) add(t, valueArguments(args));
					}
				}
				case "CLASS_BODY" -> add(t, classBody(c, "type_body"));
				case "COMMA", "SEMICOLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else add(t, generic(c));
				}
			}
		}
		return t;
	}

	private Tree primaryConstructor(ASTNode n) {
		Tree t = node("primary_constructor", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "constructor" -> add(t, leaf("constructor_keyword", c));
				case "VALUE_PARAMETER_LIST" -> {
					for(ASTNode p : kids(c)) {
						if(type(p).equals("VALUE_PARAMETER")) {
							addAll(t, leadingComments(p));
							add(t, parameter(p, "class_parameter"));
						}
						else if(isComment(p))
							add(t, comment(p));
					}
				}
				default -> {}
			}
		}
		return t;
	}

	private List<Tree> delegationSpecifiers(ASTNode n) {
		List<Tree> trees = new ArrayList<>();
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "SUPER_TYPE_CALL_ENTRY" -> {
					Tree d = node("delegation_specifier", c);
					Tree invocation = node("constructor_invocation", c);
					for(ASTNode e : kids(c)) {
						if(type(e).equals("CONSTRUCTOR_CALLEE")) add(invocation, typeTree(kids(e).get(0)));
						else if(type(e).equals("VALUE_ARGUMENT_LIST")) add(invocation, valueArguments(e));
					}
					add(d, invocation);
					trees.add(d);
				}
				case "SUPER_TYPE_ENTRY" -> {
					Tree d = node("delegation_specifier", c);
					for(ASTNode e : kids(c)) add(d, typeTree(e));
					trees.add(d);
				}
				case "DELEGATED_SUPER_TYPE_ENTRY" -> {
					Tree d = node("delegation_specifier", c);
					Tree delegation = node("explicit_delegation", c);
					for(ASTNode e : kids(c)) {
						switch(type(e)) {
							case "TYPE_REFERENCE" -> add(delegation, typeTree(e));
							case "by" -> add(delegation, leaf("by", e));
							case "DELEGATE_EXPRESSION" -> add(delegation, expression(kids(e).get(0)));
							default -> {}
						}
					}
					add(d, delegation);
					trees.add(d);
				}
				default -> {
					if(isComment(c)) trees.add(comment(c));
				}
			}
		}
		return trees;
	}

	private Tree function(ASTNode n, List<ASTNode> rest) {
		Tree t = declarationNode("function_declaration", n, rest);
		ASTNode eq = kid(n, "EQ");
		for(ASTNode c : rest) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "fun" -> add(t, leaf("function_keyword", c));
				case "TYPE_PARAMETER_LIST" -> add(t, typeParameters(c));
				case "IDENTIFIER" -> add(t, simpleIdentifier(c));
				case "VALUE_PARAMETER_LIST" -> add(t, functionValueParameters(c));
				case "TYPE_REFERENCE" -> add(t, typeTree(c));
				case "TYPE_CONSTRAINT_LIST" -> add(t, generic(c));
				case "BLOCK" -> add(t, blockBody("function_body", c));
				case "EQ", "COLON", "DOT" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else if(eq != null && start(c) > start(eq)) add(t, expressionBody(eq, c));
					else add(t, generic(c));
				}
			}
		}
		return t;
	}

	//the expression body, function_body -> [=, expression]
	private Tree expressionBody(ASTNode eq, ASTNode expression) {
		Tree body = node("function_body", start(eq), end(expression));
		add(body, leaf("affectation_operator", eq));
		add(body, expression(expression));
		return body;
	}

	private Tree functionValueParameters(ASTNode n) {
		Tree t = node("function_value_parameters", n);
		for(ASTNode c : kids(n)) {
			if(type(c).equals("VALUE_PARAMETER")) {
				addAll(t, leadingComments(c));
				//the default value is a sibling of the parameter, as in the tree-sitter tree, i.e., function_value_parameters -> [parameter, =, value]
				ASTNode eq = kid(c, "EQ");
				Tree p = parameter(c, "parameter");
				//the modifiers are a sibling of the parameter, as in the tree-sitter tree, i.e., function_value_parameters -> [parameter_modifiers, parameter]
				if(!p.getChildren().isEmpty() && p.getChild(0).getType().name.equals("parameter_modifiers")) {
					Tree m = p.getChild(0);
					p.getChildren().remove(0);
					p.setPos(p.getChild(0).getPos());
					p.setLength(end(c) - p.getPos());
					add(t, m);
				}
				if(eq != null) {
					int index = 0;
					while(index < p.getChildren().size() && p.getChild(index).getPos() < start(eq)) index++;
					List<Tree> defaults = new ArrayList<>(p.getChildren().subList(index, p.getChildren().size()));
					p.getChildren().removeAll(defaults);
					p.setLength(p.getChildren().get(p.getChildren().size() - 1).getEndPos() - p.getPos());
					add(t, p);
					addAll(t, defaults);
				}
				else add(t, p);
			}
			else if(isComment(c))
				add(t, comment(c));
		}
		return t;
	}

	//the comments preceding a parameter, which are its first children in PSI
	private List<Tree> leadingComments(ASTNode n) {
		List<Tree> trees = new ArrayList<>();
		for(ASTNode c : kids(n)) {
			if(!isComment(c)) break;
			trees.add(comment(c));
		}
		return trees;
	}

	//parameter -> [parameter_modifiers?, simple_identifier, type] or class_parameter -> [modifiers?, binding_pattern_kind?, simple_identifier, type, =, default]
	private Tree parameter(ASTNode n, String type) {
		List<ASTNode> kids = kids(n);
		int first = 0;
		while(first < kids.size() - 1 && isComment(kids.get(first))) first++;
		Tree t = node(type, start(kids.get(first)), end(n));
		for(ASTNode c : kids.subList(first, kids.size())) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, type.equals("parameter") ? parameterModifiers(c) : modifiers(c));
				case "val", "var" -> add(t, bindingPatternKind(c));
				case "IDENTIFIER" -> add(t, simpleIdentifier(c));
				case "TYPE_REFERENCE" -> add(t, typeTree(c));
				case "EQ" -> add(t, leaf("affectation_operator", c));
				case "COLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else add(t, expression(c));
				}
			}
		}
		return t;
	}

	private Tree bindingPatternKind(ASTNode keyword) {
		Tree kind = node("binding_pattern_kind", keyword);
		add(kind, leaf("property_declaration_keyword", keyword));
		return kind;
	}

	private Tree property(ASTNode n, List<ASTNode> rest) {
		Tree t = declarationNode("property_declaration", n, rest);
		//the type follows the colon after the name, while the receiver type of an extension property precedes the name
		ASTNode identifier = kid(n, "IDENTIFIER");
		ASTNode typeReference = null;
		for(ASTNode c : rest) {
			if(type(c).equals("TYPE_REFERENCE") && identifier != null && start(c) > start(identifier)) {
				typeReference = c;
				break;
			}
		}
		for(ASTNode c : rest) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "val", "var" -> add(t, bindingPatternKind(c));
				case "TYPE_PARAMETER_LIST" -> add(t, typeParameters(c));
				case "IDENTIFIER" -> {
					//the variable declaration includes the type, i.e., variable_declaration -> [simple_identifier, user_type]
					Tree v = node("variable_declaration", start(c), typeReference != null ? end(typeReference) : end(c));
					add(v, simpleIdentifier(c));
					if(typeReference != null) add(v, typeTree(typeReference));
					add(t, v);
				}
				case "TYPE_REFERENCE" -> {
					if(c != typeReference) add(t, typeTree(c));
				}
				case "EQ" -> add(t, leaf("affectation_operator", c));
				case "PROPERTY_DELEGATE" -> {
					Tree d = node("property_delegate", c);
					for(ASTNode e : kids(c)) {
						if(type(e).equals("by")) add(d, leaf("delegate_keyword", e));
						else add(d, expression(e));
					}
					add(t, d);
				}
				case "COLON", "DOT", "SEMICOLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else add(t, expression(c));
				}
			}
		}
		return t;
	}

	private Tree accessor(ASTNode n) {
		boolean getter = kid(n, "get") != null;
		Tree t = node(getter ? "getter" : "setter", n);
		ASTNode eq = kid(n, "EQ");
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "get", "set" -> {}
				case "VALUE_PARAMETER_LIST" -> {
					for(ASTNode p : kids(c)) {
						if(type(p).equals("VALUE_PARAMETER")) add(t, parameter(p, "parameter_with_optional_type"));
					}
				}
				case "TYPE_REFERENCE" -> add(t, typeTree(c));
				case "BLOCK" -> add(t, blockBody("function_body", c));
				case "EQ", "LPAR", "RPAR", "COLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else if(eq != null && start(c) > start(eq)) add(t, expressionBody(eq, c));
				}
			}
		}
		return t;
	}

	private Tree typeAlias(ASTNode n, List<ASTNode> rest) {
		Tree t = declarationNode("type_alias", n, rest);
		for(ASTNode c : rest) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "typealias" -> add(t, leaf("typealias_keyword", c));
				case "IDENTIFIER" -> add(t, leaf("type_identifier", c));
				case "TYPE_PARAMETER_LIST" -> add(t, typeParameters(c));
				case "TYPE_REFERENCE" -> add(t, typeTree(c));
				case "EQ" -> add(t, leaf("affectation_operator", c));
				default -> {}
			}
		}
		return t;
	}

	private Tree initializer(ASTNode n, List<ASTNode> rest) {
		Tree t = declarationNode("anonymous_initializer", n, rest);
		for(ASTNode c : rest) {
			switch(type(c)) {
				case "init" -> add(t, leaf("initializer_keyword", c));
				case "BLOCK" -> addAll(t, blockStatements(c));
				default -> {
					if(isComment(c)) add(t, comment(c));
				}
			}
		}
		return t;
	}

	private Tree secondaryConstructor(ASTNode n, List<ASTNode> rest) {
		Tree t = declarationNode("secondary_constructor", n, rest);
		for(ASTNode c : rest) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "constructor" -> add(t, leaf("constructor_keyword", c));
				case "VALUE_PARAMETER_LIST" -> add(t, functionValueParameters(c));
				case "CONSTRUCTOR_DELEGATION_CALL" -> {
					Tree d = node("constructor_delegation_call", c);
					for(ASTNode e : kids(c)) {
						switch(type(e)) {
							case "CONSTRUCTOR_DELEGATION_REFERENCE" -> add(d, leaf(e.getText(), e));
							case "VALUE_ARGUMENT_LIST" -> add(d, valueArguments(e));
							default -> {}
						}
					}
					add(t, d);
				}
				case "BLOCK" -> addAll(t, blockStatements(c));
				default -> {
					if(isComment(c)) add(t, comment(c));
				}
			}
		}
		return t;
	}

	// ---------------------------------------------------------------- modifiers and types

	private Tree modifiers(ASTNode n) {
		Tree t = node("modifiers", n);
		for(ASTNode c : kids(n))
			add(t, modifier(c));
		return t;
	}

	private Tree modifiersWithout(ASTNode n, String... keywords) {
		List<Tree> trees = new ArrayList<>();
		for(ASTNode c : kids(n)) {
			if(!List.of(keywords).contains(type(c))) {
				Tree m = modifier(c);
				if(m != null) trees.add(m);
			}
		}
		return trees.isEmpty() ? null : span("modifiers", trees);
	}

	private Tree parameterModifiers(ASTNode n) {
		Tree t = node("parameter_modifiers", n);
		for(ASTNode c : kids(n))
			add(t, modifier(c));
		return t;
	}

	private Tree modifier(ASTNode c) {
		String type = type(c);
		if(type.equals("ANNOTATION_ENTRY") || type.equals("ANNOTATION"))
			return annotation(c, "class_modifier");
		if(isComment(c))
			return comment(c);
		String modifierType = MODIFIER_TYPES.get(c.getText());
		if(modifierType != null && WRAPPED_MODIFIER_TYPES.containsKey(modifierType)) {
			Tree t = node(modifierType, c);
			//the variance in is an inverter_keyword, while out is a covariance_keyword
			add(t, leaf(c.getText().equals("in") ? "inverter_keyword" : WRAPPED_MODIFIER_TYPES.get(modifierType), c));
			return t;
		}
		if(modifierType != null)
			return leaf(modifierType, c);
		return leaf(c.getText(), c);
	}

	//an annotation, as class_modifier -> [at, user_type] or class_modifier -> [at, constructor_invocation -> [user_type, value_arguments]]
	private Tree annotation(ASTNode n, String type) {
		Tree t = node(type, n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "AT" -> add(t, leaf("at", c));
				case "ANNOTATION_TARGET" -> {
					Tree target = node("use_site_target", start(c), end(c) + 1);
					add(target, leaf(c.getText(), c));
					add(t, target);
				}
				case "CONSTRUCTOR_CALLEE" -> {
					ASTNode args = kid(n, "VALUE_ARGUMENT_LIST");
					if(args != null) {
						Tree invocation = node("constructor_invocation", start(c), end(args));
						add(invocation, typeTree(kids(c).get(0)));
						add(invocation, valueArguments(args));
						add(t, invocation);
					}
					else {
						add(t, typeTree(kids(c).get(0)));
					}
				}
				case "ANNOTATION_ENTRY" -> add(t, annotation(c, "annotation"));
				default -> {}
			}
		}
		return t;
	}

	private Tree typeParameters(ASTNode n) {
		Tree t = node("type_parameters", n);
		for(ASTNode c : kids(n)) {
			if(type(c).equals("TYPE_PARAMETER")) {
				Tree p = node("type_parameter", c);
				for(ASTNode e : kids(c)) {
					switch(type(e)) {
						case "MODIFIER_LIST" -> {
							Tree m = node("type_parameter_modifiers", e);
							for(ASTNode k : kids(e)) add(m, modifier(k));
							add(p, m);
						}
						case "IDENTIFIER" -> add(p, leaf("type_identifier", e));
						case "TYPE_REFERENCE" -> add(p, typeTree(e));
						default -> {}
					}
				}
				add(t, p);
			}
		}
		return t;
	}

	//the types, without the PSI TYPE_REFERENCE wrapper, i.e., user_type -> [type_identifier, type_arguments], nullable_type -> user_type
	//parenthesized_type -> type, for a type in parentheses, i.e., ((T) -> Unit)?
	private Tree parenthesized(ASTNode n, Tree type) {
		ASTNode lpar = kid(n, "LPAR"), rpar = kid(n, "RPAR");
		if(type == null || lpar == null || rpar == null)
			return type;
		Tree t = node("parenthesized_type", start(lpar), end(rpar));
		add(t, type);
		return t;
	}

	private Tree typeTree(ASTNode n) {
		switch(type(n)) {
			case "TYPE_REFERENCE" -> {
				//the annotations and modifiers of the type are skipped
				for(ASTNode c : kids(n)) {
					if(!type(c).equals("MODIFIER_LIST") && !isComment(c) && !type(c).equals("LPAR") && !type(c).equals("RPAR"))
						return parenthesized(n, typeTree(c));
				}
				return null;
			}
			case "USER_TYPE" -> {
				Tree t = node("user_type", n);
				addUserType(t, n);
				return t;
			}
			case "NULLABLE_TYPE" -> {
				Tree t = node("nullable_type", n);
				for(ASTNode c : kids(n)) {
					if(!Set.of("QUEST", "LPAR", "RPAR").contains(type(c)))
						add(t, parenthesized(n, typeTree(c)));
				}
				return t;
			}
			case "FUNCTION_TYPE" -> {
				Tree t = node("function_type", n);
				for(ASTNode c : kids(n)) {
					switch(type(c)) {
						case "FUNCTION_TYPE_RECEIVER" -> add(t, typeTree(kids(c).get(0)));
						case "VALUE_PARAMETER_LIST" -> {
							Tree p = node("function_type_parameters", c);
							for(ASTNode e : kids(c)) {
								if(type(e).equals("VALUE_PARAMETER")) {
									//a named parameter is a parameter -> [simple_identifier, type], i.e., (name: String) -> Unit
									Tree target = kid(e, "IDENTIFIER") != null ? node("parameter", e) : p;
									for(ASTNode x : kids(e)) {
										if(type(x).equals("TYPE_REFERENCE")) add(target, typeTree(x));
										else if(type(x).equals("IDENTIFIER")) add(target, simpleIdentifier(x));
									}
									if(target != p) add(p, target);
								}
							}
							add(t, p);
						}
						case "TYPE_REFERENCE" -> add(t, typeTree(c));
						case "ARROW" -> add(t, leaf("arrow", c));
						default -> {}
					}
				}
				return t;
			}
			default -> {
				return generic(n);
			}
		}
	}

	//the qualified user types are flattened, i.e., USER_TYPE -> [USER_TYPE -> a, b] as user_type -> [type_identifier a, type_identifier b]
	private void addUserType(Tree t, ASTNode n) {
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "USER_TYPE" -> addUserType(t, c);
				case "REFERENCE_EXPRESSION" -> {
					//a soft keyword is nested in the type identifier, as in simpleIdentifier, i.e., com.example.data.Repository
					String keyword = SOFT_KEYWORDS.get(c.getText());
					if(keyword != null) {
						Tree k = node("type_identifier", c);
						add(k, leaf(keyword, c));
						add(t, k);
					}
					else add(t, leaf("type_identifier", c));
				}
				case "TYPE_ARGUMENT_LIST" -> add(t, typeArguments(c));
				default -> {}
			}
		}
	}

	private Tree typeArguments(ASTNode n) {
		Tree t = node("type_arguments", n);
		for(ASTNode c : kids(n)) {
			if(type(c).equals("TYPE_PROJECTION")) {
				Tree p = node("type_projection", c);
				for(ASTNode e : kids(c)) {
					switch(type(e)) {
						case "MODIFIER_LIST" -> {
							Tree m = node("type_projection_modifiers", e);
							for(ASTNode k : kids(e)) add(m, modifier(k));
							add(p, m);
						}
						case "MUL" -> add(p, leaf("arithmetic_operator", e));
						default -> {
							//the annotations of the type are type_modifiers preceding it, i.e., List<@JvmSuppressWildcards T>
							ASTNode annotations = type(e).equals("TYPE_REFERENCE") ? kid(e, "MODIFIER_LIST") : null;
							if(annotations != null) {
								Tree m = node("type_modifiers", annotations);
								for(ASTNode k : kids(annotations)) add(m, modifier(k));
								add(p, m);
							}
							add(p, typeTree(e));
						}
					}
				}
				add(t, p);
			}
		}
		return t;
	}

	// ---------------------------------------------------------------- statements and blocks

	//a block as a body, i.e., function_body -> statements or control_structure_body -> statements
	private Tree blockBody(String type, ASTNode block) {
		Tree t = node(type, block);
		addAll(t, blockStatements(block));
		return t;
	}

	//the statements of a block, from the first to the last statement, without the braces
	//an empty try, catch or finally block is a statements node spanning its braces, with the comments inside them, so that it is matched with an empty Java block
	private List<Tree> tryBlockStatements(ASTNode block) {
		List<ASTNode> inside = kids(block).stream().filter(c -> !type(c).equals("LBRACE") && !type(c).equals("RBRACE")).toList();
		ASTNode lbrace = kid(block, "LBRACE"), rbrace = kid(block, "RBRACE");
		if(lbrace == null || rbrace == null || !inside.stream().allMatch(c -> isComment(c)))
			return blockStatements(block);
		Tree t = node("statements", start(lbrace), end(rbrace));
		for(ASTNode c : inside)
			add(t, comment(c));
		return List.of(t);
	}

	private List<Tree> blockStatements(ASTNode block) {
		List<Tree> trees = new ArrayList<>();
		List<Tree> statements = new ArrayList<>();
		for(ASTNode c : kids(block)) {
			switch(type(c)) {
				case "LBRACE", "RBRACE", "SEMICOLON" -> {}
				default -> statements.addAll(declaration(c));
			}
		}
		//the comments before the first statement are siblings of the statements, while the comments after the last statement are part of them
		int first = 0, last = statements.size() - 1;
		while(first <= last && isComment(statements.get(first))) first++;
		trees.addAll(statements.subList(0, first));
		if(first <= last) {
			Tree span = span("statements", new ArrayList<>(statements.subList(first, last + 1)));
			//the semicolon terminating the last statement is part of the statements
			for(ASTNode c : kids(block)) {
				if(type(c).equals("SEMICOLON") && start(c) == span.getEndPos()) span.setLength(end(c) - span.getPos());
			}
			trees.add(span);
		}
		trees.addAll(statements.subList(last + 1, statements.size()));
		return trees;
	}

	// ---------------------------------------------------------------- expressions

	private Tree expression(ASTNode n) {
		switch(type(n)) {
			case "REFERENCE_EXPRESSION" -> {
				return simpleIdentifier(n);
			}
			case "DOT_QUALIFIED_EXPRESSION", "SAFE_ACCESS_EXPRESSION" -> {
				return qualified(n);
			}
			case "CALL_EXPRESSION" -> {
				return call(n, null);
			}
			case "STRING_TEMPLATE" -> {
				return string(n);
			}
			case "INTEGER_CONSTANT" -> {
				return number(n);
			}
			case "FLOAT_CONSTANT" -> {
				return leaf("real_literal", n);
			}
			case "BOOLEAN_CONSTANT" -> {
				Tree t = node("boolean_literal", n);
				add(t, leaf(n.getText(), n));
				return t;
			}
			case "NULL" -> {
				return leaf("null", n);
			}
			case "CHARACTER_CONSTANT" -> {
				//the quotes are the children, with the escape sequence but without a plain character, i.e., '\n'
				Tree t = node("character_literal", n);
				add(t, leaf("'", "'", start(n), start(n) + 1));
				String text = n.getText();
				if(text.length() > 2 && text.charAt(1) == '\\')
					add(t, leaf("character_escape_seq", text.substring(1, text.length() - 1), start(n) + 1, end(n) - 1));
				add(t, leaf("'", "'", end(n) - 1, end(n)));
				return t;
			}
			case "BINARY_EXPRESSION" -> {
				return binary(n);
			}
			case "PREFIX_EXPRESSION" -> {
				Tree t = node("prefix_expression", n);
				for(ASTNode c : kids(n)) {
					if(type(c).equals("OPERATION_REFERENCE")) add(t, leaf(PREFIX_OPERATORS.getOrDefault(c.getText(), c.getText()), c));
					else add(t, expression(c));
				}
				return t;
			}
			case "POSTFIX_EXPRESSION" -> {
				Tree t = node("postfix_expression", n);
				for(ASTNode c : kids(n)) {
					if(type(c).equals("OPERATION_REFERENCE")) add(t, leaf(c.getText().equals("!!") ? "non-null_assertion_operator" : "increment_operator", c));
					else add(t, expression(c));
				}
				return t;
			}
			case "PARENTHESIZED" -> {
				Tree t = node("parenthesized_expression", n);
				for(ASTNode c : kids(n)) {
					if(!type(c).equals("LPAR") && !type(c).equals("RPAR")) add(t, expression(c));
				}
				return t;
			}
			case "THIS_EXPRESSION", "SUPER_EXPRESSION" -> {
				return thisExpression(n);
			}
			case "ARRAY_ACCESS_EXPRESSION" -> {
				Tree t = node("indexing_expression", n);
				for(ASTNode c : kids(n)) {
					if(type(c).equals("INDICES")) add(t, indexingSuffix(c));
					else add(t, expression(c));
				}
				return t;
			}
			case "IS_EXPRESSION" -> {
				Tree t = node("check_expression", n);
				List<ASTNode> isKids = kids(n);
				for(ASTNode c : isKids) {
					if(type(c).equals("OPERATION_REFERENCE") || (c != isKids.get(0) && (c.getText().equals("is") || c.getText().equals("!is")))) add(t, leaf("type_check_operator", c));
					else if(type(c).equals("TYPE_REFERENCE")) add(t, typeTree(c));
					else add(t, expression(c));
				}
				return t;
			}
			case "BINARY_WITH_TYPE" -> {
				Tree t = node("as_expression", n);
				for(ASTNode c : kids(n)) {
					if(type(c).equals("OPERATION_REFERENCE")) add(t, leaf(c.getText().equals("as?") ? "null_safe_type_conversion_keyword" : "type_conversion_or_type_alias_keyword", c));
					else if(type(c).equals("TYPE_REFERENCE")) add(t, typeTree(c));
					else add(t, expression(c));
				}
				return t;
			}
			case "LAMBDA_EXPRESSION" -> {
				return lambda(n);
			}
			case "IF" -> {
				return ifExpression(n);
			}
			case "WHEN" -> {
				return when(n);
			}
			case "TRY" -> {
				return tryExpression(n);
			}
			case "FOR" -> {
				return forStatement(n);
			}
			case "WHILE", "DO_WHILE" -> {
				return whileStatement(n);
			}
			case "RETURN", "THROW", "BREAK", "CONTINUE" -> {
				return jump(n);
			}
			case "OBJECT_LITERAL" -> {
				Tree t = node("object_literal", n);
				for(ASTNode c : kids(kid(n, "OBJECT_DECLARATION"))) {
					switch(type(c)) {
						case "object" -> add(t, leaf("type_keyword", c));
						case "SUPER_TYPE_LIST" -> addAll(t, delegationSpecifiers(c));
						case "CLASS_BODY" -> add(t, classBody(c, "type_body"));
						default -> {}
					}
				}
				return t;
			}
			case "CALLABLE_REFERENCE_EXPRESSION", "CLASS_LITERAL_EXPRESSION" -> {
				return callableReference(n);
			}
			case "LABELED_EXPRESSION" -> {
				Tree t = node("labeled_expression", n);
				for(ASTNode c : kids(n)) {
					if(type(c).equals("LABEL_QUALIFIER")) add(t, leaf("label", c));
					else add(t, expression(c));
				}
				return t;
			}
			case "ANNOTATED_EXPRESSION" -> {
				//the annotations are prefix operators, as in the tree-sitter tree, i.e., @A @B x is prefix_expression -> [@A, prefix_expression -> [@B, x]]
				List<ASTNode> annotations = new ArrayList<>();
				Tree t = null;
				for(ASTNode c : kids(n)) {
					if(type(c).equals("ANNOTATION_ENTRY")) annotations.add(c);
					else if(!isComment(c)) t = expression(c);
				}
				for(int i = annotations.size() - 1; i >= 0; i--) {
					Tree p = node("prefix_expression", start(annotations.get(i)), t != null ? t.getEndPos() : end(n));
					add(p, annotation(annotations.get(i), "class_modifier"));
					add(p, t);
					t = p;
				}
				return t;
			}
			case "PROPERTY", "FUN", "CLASS", "OBJECT_DECLARATION", "TYPEALIAS" -> {
				List<Tree> trees = declaration(n);
				return trees.get(trees.size() - 1);
			}
			case "DESTRUCTURING_DECLARATION" -> {
				return destructuring(n);
			}
			case "COLLECTION_LITERAL_EXPRESSION" -> {
				Tree t = node("collection_literal", n);
				for(ASTNode c : kids(n)) {
					if(type(c).equals("LBRACKET")) add(t, leaf("[", c));
					else if(type(c).equals("RBRACKET")) add(t, leaf("]", c));
					else if(!type(c).equals("COMMA")) add(t, expression(c));
				}
				return t;
			}
			default -> {
				if(isComment(n)) return comment(n);
				return generic(n);
			}
		}
	}

	//the PSI node without conversion rule, with its lowercase type, i.e., file_annotation_list
	private Tree generic(ASTNode n) {
		if(n.getFirstChildNode() == null)
			return leaf(type(n).toLowerCase(), n);
		Tree t = node(type(n).toLowerCase(), n);
		for(ASTNode c : kids(n))
			add(t, expression(c));
		return t;
	}

	private Tree number(ASTNode n) {
		String text = n.getText();
		boolean hex = text.startsWith("0x") || text.startsWith("0X");
		if(text.endsWith("L") || text.endsWith("l")) {
			//long_literal -> [integer_literal, L]
			Tree t = node("long_literal", n);
			add(t, leaf(hex ? "hex_literal" : "integer_literal", text.substring(0, text.length() - 1), start(n), end(n) - 1));
			add(t, leaf("L", text.substring(text.length() - 1), end(n) - 1, end(n)));
			return t;
		}
		if(hex)
			return leaf("hex_literal", n);
		if(text.startsWith("0b") || text.startsWith("0B"))
			return leaf("bin_literal", n);
		return leaf("integer_literal", n);
	}

	private Tree string(ASTNode n) {
		Tree t = stringEntries(n);
		//the empty string is a leaf, i.e., string_literal [""]
		if(t.getChildren().isEmpty())
			t.setLabel(n.getText());
		return t;
	}

	private Tree stringEntries(ASTNode n) {
		Tree t = node("string_literal", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "OPEN_QUOTE", "CLOSING_QUOTE" -> {}
				case "LITERAL_STRING_TEMPLATE_ENTRY", "ESCAPE_STRING_TEMPLATE_ENTRY" -> {
					//the consecutive literal and escape entries are a single string_content, except for the literal dollar signs, which are separate string contents
					String text = c.getText();
					boolean literal = type(c).equals("LITERAL_STRING_TEMPLATE_ENTRY");
					int from = 0;
					for(int i=0; i<=text.length(); i++) {
						boolean dollar = literal && i < text.length() && text.charAt(i) == '$';
						if(i == text.length() || dollar) {
							if(i > from) appendStringContent(t, text.substring(from, i), start(c) + from, start(c) + i);
							if(dollar) add(t, leaf("string_content", "$", start(c) + i, start(c) + i + 1));
							from = i + 1;
						}
					}
				}
				case "SHORT_STRING_TEMPLATE_ENTRY" -> {
					List<ASTNode> entry = kids(c);
					ASTNode name = entry.get(entry.size() - 1);
					String keyword = SOFT_KEYWORDS.get(name.getText());
					//a soft keyword is nested in the identifier, as in simpleIdentifier, i.e., $value
					if(keyword != null) {
						Tree k = node("interpolated_identifier", name);
						add(k, leaf(keyword, name));
						add(t, k);
					}
					else add(t, leaf("interpolated_identifier", name));
				}
				case "LONG_STRING_TEMPLATE_ENTRY" -> {
					for(ASTNode e : kids(c)) {
						switch(type(e)) {
							case "LONG_TEMPLATE_ENTRY_START" -> add(t, leaf("${", e));
							case "LONG_TEMPLATE_ENTRY_END" -> {}
							default -> {
								Tree i = node("interpolated_expression", e);
								add(i, expression(e));
								add(t, i);
							}
						}
					}
				}
				default -> {}
			}
		}
		return t;
	}

	private void appendStringContent(Tree string, String text, int start, int end) {
		Tree last = string.getChildren().isEmpty() ? null : string.getChild(string.getChildren().size() - 1);
		if(last != null && last.getType().name.equals("string_content") && last.getEndPos() == start && !last.getLabel().equals("$")) {
			last.setLabel(last.getLabel() + text);
			last.setLength(end - last.getPos());
		}
		else add(string, leaf("string_content", text, start, end));
	}

	//the string contents are split at the dollar signs, which are separate string contents, as in the tree-sitter tree, i.e., "a$b" with a literal $
	private void splitDollars(Tree string) {
		List<Tree> children = new ArrayList<>(string.getChildren());
		string.getChildren().clear();
		for(Tree c : children) {
			String label = c.getLabel();
			if(!c.getType().name.equals("string_content") || label.indexOf('$') < 0 || label.equals("$")) {
				add(string, c);
				continue;
			}
			int pos = c.getPos(), from = 0;
			for(int i=0; i<=label.length(); i++) {
				if(i == label.length() || label.charAt(i) == '$') {
					if(i > from) add(string, leaf("string_content", label.substring(from, i), pos + from, pos + i));
					if(i < label.length()) add(string, leaf("string_content", "$", pos + i, pos + i + 1));
					from = i + 1;
				}
			}
		}
	}

	//this_expression -> this, or this_expression -> [this@, type_identifier] for a qualified this
	private Tree thisExpression(ASTNode n) {
		boolean isThis = type(n).equals("THIS_EXPRESSION");
		String keyword = isThis ? "this" : "super";
		Tree t = node(isThis ? "this_expression" : "super_expression", n);
		ASTNode label = kid(n, "LABEL_QUALIFIER");
		if(label != null) {
			add(t, leaf(keyword + "@", keyword + "@", start(n), start(label) + 1));
			add(t, leaf("type_identifier", label.getText().substring(1), start(label) + 1, end(label)));
		}
		else {
			add(t, leaf(isThis ? "this" : "super_expression_keyword", keyword, start(n), start(n) + keyword.length()));
		}
		return t;
	}

	private Tree callableReference(ASTNode n) {
		List<ASTNode> kids = kids(n);
		//with a receiver other than a name, the reference is the navigation_suffix of a navigation_expression, as in the tree-sitter tree,
		//i.e., navigation_expression -> [this, navigation_suffix -> [::, simple_identifier]] for this::x, while Foo::class is a callable_reference
		boolean expressionReceiver = !kids.isEmpty() && !type(kids.get(0)).equals("COLONCOLON") && !type(kids.get(0)).equals("REFERENCE_EXPRESSION");
		Tree t = node(expressionReceiver ? "navigation_expression" : "callable_reference", n);
		Tree suffix = null;
		if(expressionReceiver) {
			ASTNode colons = kid(n, "COLONCOLON");
			if(colons != null) suffix = node("navigation_suffix", start(colons), end(n));
		}
		for(ASTNode c : kids) {
			Tree parent = suffix != null && c != kids.get(0) ? suffix : t;
			switch(type(c)) {
				case "COLONCOLON" -> add(parent, leaf("reference_extraction_operator", c));
				case "class" -> add(parent, leaf("type_keyword", c));
				case "REFERENCE_EXPRESSION" -> add(parent, leaf(c == kids.get(0) ? "type_identifier" : "simple_identifier", c));
				default -> add(parent, expression(c));
			}
		}
		if(suffix != null) add(t, suffix);
		return t;
	}

	private Tree indexingSuffix(ASTNode indices) {
		Tree s = node("indexing_suffix", indices);
		for(ASTNode i : kids(indices)) {
			switch(type(i)) {
				case "LBRACKET" -> add(s, leaf("[", i));
				case "RBRACKET" -> add(s, leaf("]", i));
				case "COMMA" -> {}
				default -> add(s, expression(i));
			}
		}
		return s;
	}

	private Tree binary(ASTNode n) {
		ASTNode operator = kid(n, "OPERATION_REFERENCE");
		String op = operator != null ? operator.getText() : "";
		if(ASSIGNMENT_OPERATORS.contains(op)) {
			//assignment -> [directly_assignable_expression, =, value]
			Tree t = node("assignment", n);
			for(ASTNode c : kids(n)) {
				if(c == operator) add(t, leaf("affectation_operator", c));
				else if(start(c) < start(operator)) add(t, assignable(c));
				else add(t, expression(c));
			}
			return t;
		}
		String[] types = BINARY_OPERATORS.get(op);
		Tree t = node(types != null ? types[0] : "infix_expression", n);
		for(ASTNode c : kids(n)) {
			if(c == operator) add(t, leaf(types != null ? types[1] : "simple_identifier", c));
			else add(t, expression(c));
		}
		return t;
	}

	//the target of an assignment, i.e., directly_assignable_expression -> [this_expression, navigation_suffix] for this.x = y
	private Tree assignable(ASTNode n) {
		Tree t = node("directly_assignable_expression", n);
		if(QUALIFIED_EXPRESSIONS.contains(type(n))) {
			//the navigation suffixes of a chain of property accesses are children of the target, i.e., directly_assignable_expression -> [a, .b, .c] for a.b.c = x
			List<Tree> suffixes = new ArrayList<>();
			ASTNode receiver = n;
			while(QUALIFIED_EXPRESSIONS.contains(type(receiver))) {
				List<ASTNode> kids = kids(receiver);
				ASTNode selector = kids.get(kids.size() - 1);
				if(!type(selector).equals("REFERENCE_EXPRESSION") && receiver != n)
					break;
				suffixes.add(0, navigationSuffix(operator(kids), selector));
				receiver = kids.get(0);
			}
			//a non-null assertion of the receiver is flattened, as in the tree-sitter tree, i.e., directly_assignable_expression -> [a, !!, .b] for a!!.b = x
			if(type(receiver).equals("POSTFIX_EXPRESSION") && kid(receiver, "OPERATION_REFERENCE") != null && kid(receiver, "OPERATION_REFERENCE").getText().equals("!!"))
				addAll(t, expression(receiver).getChildren());
			else
				add(t, expression(receiver));
			addAll(t, suffixes);
		}
		else if(type(n).equals("ARRAY_ACCESS_EXPRESSION")) {
			for(ASTNode c : kids(n)) {
				if(type(c).equals("INDICES")) add(t, indexingSuffix(c));
				else add(t, expression(c));
			}
		}
		else {
			add(t, expression(n));
		}
		return t;
	}

	// ---------------------------------------------------------------- calls and navigation

	//receiver.selector or receiver?.selector, as navigation_expression -> [receiver, navigation_suffix] or call_expression -> [navigation_expression, call_suffix]
	private Tree qualified(ASTNode n) {
		List<ASTNode> kids = kids(n);
		ASTNode selector = kids.get(kids.size() - 1);
		if(type(selector).equals("CALL_EXPRESSION"))
			return call(selector, n);
		Tree t = node("navigation_expression", n);
		addReceiverAndSuffix(t, kids, selector);
		return t;
	}

	//the receiver, the comments before the dot, which are children of the navigation_expression in the tree-sitter tree, and the navigation_suffix
	private void addReceiverAndSuffix(Tree t, List<ASTNode> kids, ASTNode selector) {
		add(t, expression(kids.get(0)));
		for(int i = 1; i < kids.size() - 1; i++) {
			if(isComment(kids.get(i))) add(t, comment(kids.get(i)));
		}
		add(t, navigationSuffix(operator(kids), selector));
	}

	//the dot or the safe call operator of a qualified expression
	private static ASTNode operator(List<ASTNode> kids) {
		for(int i = 1; i < kids.size() - 1; i++) {
			if(!isComment(kids.get(i))) return kids.get(i);
		}
		return kids.get(1);
	}

	//navigation_suffix -> [?., simple_identifier], from the dot to the end of the name
	private Tree navigationSuffix(ASTNode operator, ASTNode name) {
		Tree t = node("navigation_suffix", start(operator), end(name));
		if(type(operator).equals("SAFE_ACCESS"))
			add(t, leaf("null_safe_call_operator", operator));
		if(type(name).equals("REFERENCE_EXPRESSION"))
			add(t, simpleIdentifier(name));
		else
			add(t, expression(name));
		return t;
	}

	//call_expression -> [callee, call_suffix -> [type_arguments?, value_arguments?, annotated_lambda?]], where the callee is a navigation_expression for a qualified call,
	//and a call with both arguments and a trailing lambda is nested in another call_expression, as in the tree-sitter tree, i.e., require(x) { ... }
	private Tree call(ASTNode call, ASTNode qualified) {
		List<ASTNode> kids = kids(call);
		ASTNode callee = kids.get(0);
		ASTNode typeArguments = kid(call, "TYPE_ARGUMENT_LIST");
		ASTNode arguments = kid(call, "VALUE_ARGUMENT_LIST");
		List<ASTNode> lambdas = new ArrayList<>();
		for(ASTNode c : kids) {
			if(type(c).equals("LAMBDA_ARGUMENT")) lambdas.add(c);
		}
		int callStart = qualified != null ? start(qualified) : start(call);
		Tree current;
		if(qualified != null) {
			List<ASTNode> q = kids(qualified);
			current = node("navigation_expression", callStart, end(callee));
			addReceiverAndSuffix(current, q, callee);
		}
		else {
			current = expression(callee);
		}
		ASTNode firstSuffix = typeArguments != null ? typeArguments : arguments;
		if(arguments == null && typeArguments != null && lambdas.size() == 1) {
			//call_expression -> [callee, call_suffix -> [type_arguments, annotated_lambda]], i.e., viewModels<T> { ... }
			ASTNode lambda = lambdas.get(0);
			Tree t = node("call_expression", callStart, end(lambda));
			add(t, current);
			Tree suffix = node("call_suffix", start(typeArguments), end(lambda));
			add(suffix, typeArguments(typeArguments));
			add(suffix, annotatedLambda(lambda));
			add(t, suffix);
			return t;
		}
		if(firstSuffix != null) {
			ASTNode lastSuffix = arguments != null ? arguments : typeArguments;
			int end = lambdas.isEmpty() ? end(call) : end(lastSuffix);
			Tree t = node("call_expression", callStart, end);
			add(t, current);
			Tree suffix = node("call_suffix", start(firstSuffix), end);
			if(typeArguments != null) add(suffix, typeArguments(typeArguments));
			if(arguments != null) add(suffix, valueArguments(arguments));
			add(t, suffix);
			current = t;
		}
		for(ASTNode lambda : lambdas) {
			Tree t = node("call_expression", callStart, end(lambda));
			add(t, current);
			Tree suffix = node("call_suffix", lambda);
			add(suffix, annotatedLambda(lambda));
			add(t, suffix);
			current = t;
		}
		return current;
	}

	private Tree annotatedLambda(ASTNode lambda) {
		Tree annotated = node("annotated_lambda", lambda);
		for(ASTNode l : kids(lambda)) {
			switch(type(l)) {
				case "LAMBDA_EXPRESSION" -> add(annotated, lambda(l));
				case "LABELED_EXPRESSION" -> {
					add(annotated, leaf("label", kid(l, "LABEL_QUALIFIER")));
					ASTNode inner = kid(l, "LAMBDA_EXPRESSION");
					if(inner != null) add(annotated, lambda(inner));
				}
				default -> add(annotated, expression(l));
			}
		}
		return annotated;
	}

	private Tree valueArguments(ASTNode n) {
		Tree t = node("value_arguments", n);
		for(ASTNode c : kids(n)) {
			if(type(c).equals("VALUE_ARGUMENT")) {
				Tree a = node("value_argument", c);
				ASTNode spread = null;
				for(ASTNode e : kids(c)) {
					switch(type(e)) {
						case "VALUE_ARGUMENT_NAME" -> add(a, simpleIdentifier(e));
						case "EQ" -> add(a, leaf("affectation_operator", e));
						case "MUL" -> spread = e;
						default -> {
							if(isComment(e)) add(a, comment(e));
							else if(spread != null) {
								//spread_expression -> [*, expression], i.e., *args
								Tree x = node("spread_expression", start(spread), end(e));
								add(x, leaf("arithmetic_operator", spread));
								add(x, expression(e));
								add(a, x);
								spread = null;
							}
							else add(a, expression(e));
						}
					}
				}
				add(t, a);
			}
			else if(isComment(c)) {
				add(t, comment(c));
			}
		}
		return t;
	}

	//lambda_literal -> [lambda_parameters?, arrow?, statements?]
	private Tree lambda(ASTNode n) {
		ASTNode literal = kid(n, "FUNCTION_LITERAL");
		Tree t = node("lambda_literal", n);
		for(ASTNode c : kids(literal)) {
			switch(type(c)) {
				case "VALUE_PARAMETER_LIST" -> {
					Tree p = node("lambda_parameters", c);
					for(ASTNode e : kids(c)) {
						if(type(e).equals("VALUE_PARAMETER")) {
							ASTNode destructuring = kid(e, "DESTRUCTURING_DECLARATION");
							add(p, destructuring != null ? multiVariableDeclaration(destructuring) : variableDeclaration(e));
						}
					}
					add(t, p);
				}
				case "ARROW" -> add(t, leaf("arrow", c));
				case "BLOCK" -> addAll(t, blockStatements(c));
				default -> {}
			}
		}
		return t;
	}

	//variable_declaration -> [simple_identifier, type?]
	private Tree variableDeclaration(ASTNode n) {
		Tree v = node("variable_declaration", n);
		for(ASTNode x : kids(n)) {
			if(type(x).equals("IDENTIFIER")) add(v, simpleIdentifier(x));
			else if(type(x).equals("TYPE_REFERENCE")) add(v, typeTree(x));
		}
		return v;
	}

	private Tree multiVariableDeclaration(ASTNode n) {
		Tree t = node("multi_variable_declaration", n);
		for(ASTNode c : kids(n)) {
			if(type(c).equals("DESTRUCTURING_DECLARATION_ENTRY"))
				add(t, variableDeclaration(c));
		}
		return t;
	}

	//val (a, b) = x, as property_declaration -> [binding_pattern_kind, multi_variable_declaration, =, expression]
	private Tree destructuring(ASTNode n) {
		Tree t = node("property_declaration", n);
		List<ASTNode> entries = new ArrayList<>();
		for(ASTNode c : kids(n)) {
			if(type(c).equals("DESTRUCTURING_DECLARATION_ENTRY")) entries.add(c);
		}
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "MODIFIER_LIST" -> add(t, modifiers(c));
				case "val", "var" -> add(t, bindingPatternKind(c));
				case "LPAR" -> {
					ASTNode rpar = kid(n, "RPAR");
					Tree m = node("multi_variable_declaration", start(c), rpar != null ? end(rpar) : end(entries.get(entries.size() - 1)));
					for(ASTNode e : entries) add(m, variableDeclaration(e));
					add(t, m);
				}
				case "EQ" -> add(t, leaf("affectation_operator", c));
				case "RPAR", "COMMA", "DESTRUCTURING_DECLARATION_ENTRY" -> {}
				default -> add(t, expression(c));
			}
		}
		return t;
	}

	// ---------------------------------------------------------------- control flow

	//the body of a control structure, control_structure_body -> statements for a block, or control_structure_body -> statement
	private Tree controlBody(ASTNode wrapper) {
		List<ASTNode> kids = kids(wrapper);
		if(kids.isEmpty())
			return null;
		if(kids.size() == 1 && type(kids.get(0)).equals("BLOCK"))
			return blockBody("control_structure_body", kids.get(0));
		Tree t = node("control_structure_body", wrapper);
		for(ASTNode c : kids) add(t, expression(c));
		return t;
	}

	private Tree ifExpression(ASTNode n) {
		Tree t = node("if_expression", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "CONDITION" -> {
					for(ASTNode e : kids(c)) add(t, expression(e));
				}
				case "THEN", "ELSE" -> add(t, controlBody(c));
				case "if", "else", "LPAR", "RPAR", "SEMICOLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
				}
			}
		}
		return t;
	}

	private Tree when(ASTNode n) {
		Tree t = node("when_expression", n);
		ASTNode lpar = kid(n, "LPAR"), rpar = kid(n, "RPAR");
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "when" -> {}
				case "WHEN_ENTRY" -> add(t, whenEntry(c));
				case "LPAR", "RPAR", "LBRACE", "RBRACE" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else {
						//the subject of when, when_subject -> expression, including the parentheses
						Tree s = node("when_subject", lpar != null ? start(lpar) : start(c), rpar != null ? end(rpar) : end(c));
						if(type(c).equals("PROPERTY")) {
							//when (val x = y), as when_subject -> [property_declaration_keyword, variable_declaration, affectation_operator, y] in the tree-sitter tree
							for(Tree d : declaration(c)) {
								if(!d.getType().name.equals("property_declaration")) { add(s, d); continue; }
								for(Tree p : new ArrayList<>(d.getChildren()))
									add(s, p.getType().name.equals("binding_pattern_kind") ? p.getChild(0) : p);
							}
						}
						else
							add(s, expression(c));
						add(t, s);
					}
				}
			}
		}
		return t;
	}

	private Tree whenEntry(ASTNode n) {
		Tree t = node("when_entry", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "WHEN_CONDITION_WITH_EXPRESSION" -> {
					Tree condition = node("when_condition", c);
					for(ASTNode e : kids(c)) add(condition, expression(e));
					add(t, condition);
				}
				case "WHEN_CONDITION_IN_RANGE", "WHEN_CONDITION_IS_PATTERN" -> {
					Tree condition = node("when_condition", c);
					Tree test = node(type(c).equals("WHEN_CONDITION_IS_PATTERN") ? "type_test" : "range_test", c);
					for(ASTNode e : kids(c)) {
						switch(type(e)) {
							case "OPERATION_REFERENCE", "is", "NOT_IS" -> add(test, leaf(type(c).equals("WHEN_CONDITION_IS_PATTERN") ? "type_check_operator" : e.getText(), e));
							case "TYPE_REFERENCE" -> add(test, typeTree(e));
							default -> add(test, expression(e));
						}
					}
					add(condition, test);
					add(t, condition);
				}
				case "else" -> {}
				case "ARROW" -> add(t, leaf("arrow", c));
				case "COMMA", "SEMICOLON" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
					else if(type(c).equals("BLOCK")) add(t, blockBody("control_structure_body", c));
					else {
						Tree body = node("control_structure_body", c);
						add(body, expression(c));
						add(t, body);
					}
				}
			}
		}
		return t;
	}

	private Tree tryExpression(ASTNode n) {
		Tree t = node("try_expression", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "BLOCK" -> addAll(t, tryBlockStatements(c));
				case "CATCH" -> {
					Tree catchBlock = node("catch_block", c);
					for(ASTNode e : kids(c)) {
						switch(type(e)) {
							case "VALUE_PARAMETER_LIST" -> {
								for(ASTNode p : kids(e)) {
									if(type(p).equals("VALUE_PARAMETER")) {
										for(ASTNode x : kids(p)) {
											switch(type(x)) {
												case "IDENTIFIER" -> add(catchBlock, simpleIdentifier(x));
												case "TYPE_REFERENCE" -> add(catchBlock, typeTree(x));
												case "MODIFIER_LIST" -> add(catchBlock, modifiers(x));
												default -> {}
											}
										}
									}
								}
							}
							case "BLOCK" -> addAll(catchBlock, tryBlockStatements(e));
							default -> {}
						}
					}
					add(t, catchBlock);
				}
				case "FINALLY" -> {
					Tree finallyBlock = node("finally_block", c);
					for(ASTNode e : kids(c)) {
						if(type(e).equals("BLOCK")) addAll(finallyBlock, tryBlockStatements(e));
					}
					add(t, finallyBlock);
				}
				default -> {
					if(isComment(c)) add(t, comment(c));
				}
			}
		}
		return t;
	}

	private Tree forStatement(ASTNode n) {
		Tree t = node("for_statement", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "VALUE_PARAMETER" -> {
					ASTNode destructuring = kid(c, "DESTRUCTURING_DECLARATION");
					add(t, destructuring != null ? multiVariableDeclaration(destructuring) : variableDeclaration(c));
				}
				case "LOOP_RANGE" -> {
					for(ASTNode e : kids(c)) add(t, expression(e));
				}
				case "BODY" -> add(t, controlBody(c));
				case "in" -> add(t, leaf("collection_iterated", c));
				case "for", "LPAR", "RPAR" -> {}
				default -> {
					if(isComment(c)) add(t, comment(c));
				}
			}
		}
		return t;
	}

	private Tree whileStatement(ASTNode n) {
		Tree t = node(type(n).equals("WHILE") ? "while_statement" : "do_while_statement", n);
		for(ASTNode c : kids(n)) {
			switch(type(c)) {
				case "do", "while" -> {}
				case "CONDITION" -> {
					for(ASTNode e : kids(c)) add(t, expression(e));
				}
				case "BODY" -> add(t, controlBody(c));
				default -> {
					if(isComment(c)) add(t, comment(c));
				}
			}
		}
		return t;
	}

	//jump_expression -> [jump_keyword, expression?], with the label in the keyword, i.e., return@forEach
	private Tree jump(ASTNode n) {
		Tree t = node("jump_expression", n);
		List<ASTNode> kids = kids(n);
		ASTNode keyword = kids.get(0);
		ASTNode label = kid(n, "LABEL_QUALIFIER");
		if(label != null) {
			add(t, leaf(keyword.getText() + "@", keyword.getText() + "@", start(keyword), start(label) + 1));
			add(t, leaf("label", label.getText().substring(1), start(label) + 1, end(label)));
		}
		else {
			add(t, leaf("jump_keyword", keyword));
		}
		for(ASTNode c : kids.subList(1, kids.size())) {
			if(c != label) add(t, expression(c));
		}
		return t;
	}
}
