package org.refactoringminer.astDiff.utils;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClass;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClassMethod;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstClassProp;
import com.caoccao.javet.swc4j.ast.clazz.Swc4jAstFunction;
import com.caoccao.javet.swc4j.ast.enums.Swc4jAstMethodKind;
import com.caoccao.javet.swc4j.ast.enums.Swc4jAstVarDeclKind;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstArrowExpr;
import com.caoccao.javet.swc4j.ast.expr.Swc4jAstIdent;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAst;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstFnDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstTsModuleDecl;
import com.caoccao.javet.swc4j.ast.stmt.Swc4jAstVarDecl;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsKeywordType;
import com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypeOperator;
import com.caoccao.javet.swc4j.comments.Swc4jComment;
import com.caoccao.javet.swc4j.comments.Swc4jComments;
import com.caoccao.javet.swc4j.enums.Swc4jMediaType;
import com.caoccao.javet.swc4j.enums.Swc4jParseMode;
import com.caoccao.javet.swc4j.options.Swc4jParseOptions;
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

/**
 * Generates the AST diff tree of a JavaScript or TypeScript file from the swc4j AST, which is also used to generate the RefactoringMiner model,
 * with the node types and the structure of the tree-sitter-javascript (.js) and tree-sitter-typescript (.ts) or tsx (.tsx, .jsx) trees, which were used before,
 * so that the AST diff matchers and the diffs remain the same.
 * The keywords and the punctuation, which are leaves of the tree-sitter trees, are the tokens between the children of each node,
 * and the comments are placed like the extras of tree-sitter, i.e., in the smallest node containing the tokens before and after them.
 */
public class TypeScriptTreeGenerator {
	private static final String INLINE = "";
	//the punctuation tokens, the longest first
	private static final String[] PUNCTUATION = {"${", "</", "/>", ">>>=", "...", "===", "!==", "**=", "<<=", ">>=", ">>>", "&&=", "||=", "??=", "=>", "==", "!=", "<=", ">=",
			"&&", "||", "??", "?.", "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "**", "<<", ">>"};
	private static final Set<String> ACCESSIBILITY = Set.of("public", "private", "protected");

	public static TreeContext generate(String filePath, String content) {
		try {
			Swc4jMediaType mediaType;
			if(filePath.endsWith(".tsx"))
				mediaType = Swc4jMediaType.Tsx;
			else if(filePath.endsWith(".ts"))
				mediaType = Swc4jMediaType.TypeScript;
			else if(filePath.endsWith(".jsx"))
				mediaType = Swc4jMediaType.Jsx;
			else if(filePath.endsWith("spec.js") || content.contains("</div>") || content.contains("</ul>") || content.contains("</Route>") || content.contains("</Provider>") || content.contains("/>"))
				mediaType = Swc4jMediaType.Jsx;
			else
				mediaType = Swc4jMediaType.JavaScript;
			URL specifier = Path.of(filePath).toUri().toURL();
			Swc4jParseOptions options = new Swc4jParseOptions().setSpecifier(specifier).setMediaType(mediaType)
					.setCaptureAst(true).setCaptureComments(true).setParseMode(Swc4jParseMode.Module);
			Swc4jParseOutput output = new Swc4j().parse(content, options);
			return generate(filePath, content, output.getProgram(), output.getComments());
		}
		catch(Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static TreeContext generate(String filePath, String content, ISwc4jAst program, Swc4jComments comments) {
		TreeContext context = new TreeContext();
		if(content.isEmpty()) {
			Tree root = context.createTree(TypeSet.type("empty_root"));
			root.setPos(0);
			root.setLength(0);
			context.setRoot(root);
			return context;
		}
		TypeScriptTreeGenerator generator = new TypeScriptTreeGenerator(content, !filePath.endsWith(".js"), comments);
		Node root = generator.program(program);
		generator.addComments(root);
		root.computeRange();
		root.start = root.children.isEmpty() ? content.length() : root.first().start;
		root.end = content.length();
		context.setRoot(generator.toTree(root, context));
		return context;
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

		private void computeRange() {
			for(Node child : children) {
				child.computeRange();
			}
			if(start < 0 && !children.isEmpty()) {
				start = children.get(0).start;
				end = children.get(children.size() - 1).end;
			}
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
	}

	private final String content;
	//tree-sitter-typescript grammar, i.e., not .js files
	private final boolean typescript;
	//the start and end offsets of the comments
	private final TreeMap<Integer, Integer> comments = new TreeMap<>();

	private TypeScriptTreeGenerator(String content, boolean typescript, Swc4jComments swc4jComments) {
		this.content = content;
		this.typescript = typescript;
		if(swc4jComments != null) {
			for(List<Swc4jComment> list : swc4jComments.getLeading().values()) {
				for(Swc4jComment comment : list) {
					comments.put(comment.getSpan().getStart(), comment.getSpan().getEnd());
				}
			}
			for(List<Swc4jComment> list : swc4jComments.getTrailing().values()) {
				for(Swc4jComment comment : list) {
					comments.put(comment.getSpan().getStart(), comment.getSpan().getEnd());
				}
			}
		}
	}

	//tokens

	private int skip(int offset, int limit) {
		offset = Math.max(offset, 0);
		while(offset < limit) {
			char character = content.charAt(offset);
			Integer commentEnd = comments.get(offset);
			if(commentEnd != null) {
				offset = commentEnd;
			}
			else if(Character.isWhitespace(character) || character == ' ' || character == '﻿' || character == ' ' || character == ' ') {
				offset++;
			}
			else {
				break;
			}
		}
		return offset;
	}

	private static boolean isIdentifierStart(char character) {
		return Character.isLetter(character) || character == '_' || character == '$' || character == '#';
	}

	private static boolean isIdentifierPart(char character) {
		return Character.isLetterOrDigit(character) || character == '_' || character == '$';
	}

	//the end of the token starting at the offset
	private int tokenEnd(int offset, int limit) {
		char character = content.charAt(offset);
		if(content.startsWith("${", offset)) {
			return offset + 2;
		}
		if(isIdentifierStart(character) || Character.isDigit(character)) {
			int end = offset + 1;
			while(end < limit && isIdentifierPart(content.charAt(end))) {
				end++;
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

	//the tokens between the offsets as leaves
	private List<Node> tokens(int start, int end) {
		List<Node> tokens = new ArrayList<>();
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = tokenEnd(offset, end);
			tokens.add(leaf(content.substring(offset, tokenEnd), offset, tokenEnd));
			offset = skip(tokenEnd, end);
		}
		return tokens;
	}

	//the offset of the first token with the text, or -1
	private int find(String text, int start, int end) {
		int offset = skip(start, end);
		while(offset < end) {
			int tokenEnd = tokenEnd(offset, end);
			if(content.substring(offset, tokenEnd).equals(text)) {
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
			int tokenEnd = tokenEnd(offset, end);
			if(content.substring(offset, tokenEnd).equals(text)) {
				found = offset;
			}
			offset = skip(tokenEnd, end);
		}
		return found;
	}

	private int nextTokenStart(int offset) {
		return skip(offset, content.length());
	}

	private Node leaf(String type, int start, int end) {
		Node node = new Node(type);
		node.start = start;
		node.end = end;
		node.leaf = true;
		return node;
	}

	private Node leaf(String type, ISwc4jAst ast) {
		return leaf(type, start(ast), end(ast));
	}

	private static int start(ISwc4jAst ast) {
		return ast.getSpan().getStart();
	}

	private static int end(ISwc4jAst ast) {
		return ast.getSpan().getEnd();
	}

	private static Node inline(List<Node> nodes) {
		Node node = new Node(INLINE);
		for(Node child : nodes) {
			if(child != null) {
				node.children.add(child);
			}
		}
		return node;
	}

	//a node with the range, the children and the tokens between them
	private Node node(String type, int start, int end, List<Node> children) {
		Node node = new Node(type);
		node.start = start;
		node.end = end;
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
		return node;
	}

	private Node node(String type, ISwc4jAst ast, List<Node> children) {
		return node(type, start(ast), end(ast), children);
	}

	private Node node(String type, ISwc4jAst ast) {
		return node(type, ast, convertChildren(ast));
	}

	private List<Node> convertChildren(ISwc4jAst ast) {
		List<Node> children = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			children.add(convert(child));
		}
		return children;
	}

	private List<Node> convert(List<? extends ISwc4jAst> asts) {
		List<Node> nodes = new ArrayList<>();
		for(ISwc4jAst ast : asts) {
			nodes.add(convert(ast));
		}
		return nodes;
	}

	private static String type(ISwc4jAst ast) {
		return ast.getType().name();
	}

	private static String parentType(ISwc4jAst ast) {
		return ast.getParent() == null ? "" : type(ast.getParent());
	}

	//wraps the token children with the texts in a node of the type, i.e., public in accessibility_modifier
	private static void wrapTokens(Node node, Set<String> texts, String type) {
		for(int i = 0; i < node.children.size(); i++) {
			Node child = node.children.get(i);
			if(child.leaf && texts.contains(child.type) && child.children.isEmpty()) {
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

	//program and statements

	private Node program(ISwc4jAst program) {
		List<Node> children = convertChildren(program);
		int start = content.startsWith("#!") ? 0 : nextTokenStart(0);
		Node node = node("program", start, content.length(), children);
		if(content.startsWith("#!")) {
			int end = 0;
			while(end < content.length() && content.charAt(end) != '\n' && content.charAt(end) != '\r') {
				end++;
			}
			//replace the tokens of the hashbang line
			List<Node> kept = new ArrayList<>();
			for(Node child : node.children) {
				if(child.end <= end) {
					continue;
				}
				kept.add(child);
			}
			node.children.clear();
			node.add(leaf("hash_bang_line", 0, end));
			for(Node child : kept) {
				node.add(child);
			}
		}
		emptyStatements(node);
		node.start = node.children.isEmpty() ? content.length() : node.children.get(0).start;
		return node;
	}

	//the semicolons between the statements
	private static void emptyStatements(Node node) {
		for(int i = 0; i < node.children.size(); i++) {
			Node child = node.children.get(i);
			if(child.leaf && child.type.equals(";")) {
				Node statement = new Node("empty_statement");
				statement.start = child.start;
				statement.end = child.end;
				statement.parent = node;
				child.parent = statement;
				statement.children.add(child);
				node.children.set(i, statement);
			}
		}
	}

	private Node convert(ISwc4jAst ast) {
		String type = type(ast);
		switch(type) {
		case "ExprStmt":
			return node("expression_statement", ast);
		case "VarDecl":
			return variableDeclaration((Swc4jAstVarDecl) ast);
		case "VarDeclarator":
			return node("variable_declarator", ast);
		case "BindingIdent":
			return bindingIdentifier(ast);
		case "Ident":
			return identifier((Swc4jAstIdent) ast);
		case "IdentName":
			return leaf(parentType(ast).equals("TsQualifiedName") ? "type_identifier" : "property_identifier", ast);
		case "PrivateName":
			return leaf("private_property_identifier", ast);
		case "BlockStmt":
		case "TsModuleBlock":
			return node("statement_block", ast);
		case "ReturnStmt":
			return node("return_statement", ast);
		case "ThrowStmt":
			return node("throw_statement", ast);
		case "BreakStmt":
			return node("break_statement", ast);
		case "ContinueStmt":
			return node("continue_statement", ast);
		case "LabeledStmt":
			return node("labeled_statement", ast);
		case "EmptyStmt":
			return node("empty_statement", ast);
		case "DebuggerStmt":
			return node("debugger_statement", ast);
		case "IfStmt":
			return ifStatement(ast);
		case "ForStmt":
			return forStatement(ast);
		case "ForInStmt":
		case "ForOfStmt":
			return forInStatement(ast);
		case "WhileStmt":
			return parenthesizedFirst("while_statement", ast);
		case "DoWhileStmt":
			return doStatement(ast);
		case "WithStmt":
			return parenthesizedFirst("with_statement", ast);
		case "SwitchStmt":
			return switchStatement(ast);
		case "SwitchCase":
			return node(content.startsWith("default", start(ast)) ? "switch_default" : "switch_case", ast);
		case "TryStmt":
			return tryStatement(ast);
		case "CatchClause":
			return node("catch_clause", ast);
		case "FnDecl":
			return functionDeclaration((Swc4jAstFnDecl) ast);
		case "FnExpr":
			return functionExpression(ast);
		case "Function":
			return function((Swc4jAstFunction) ast);
		case "Param":
			return parameter(ast);
		case "ArrowExpr":
			return arrowFunction((Swc4jAstArrowExpr) ast);
		case "ClassDecl":
		case "ClassExpr":
			return classDeclaration(ast);
		case "Class":
			return classInline((Swc4jAstClass) ast);
		case "ClassMethod":
		case "PrivateMethod":
			return method(ast);
		case "Constructor":
			return method(ast);
		case "ClassProp":
		case "PrivateProp":
		case "AutoAccessor":
			return fieldDefinition(ast);
		case "StaticBlock":
			return node("class_static_block", ast);
		case "Decorator":
			return node("decorator", ast);
		case "TsParamProp":
			return parameterProperty(ast);
		case "ObjectLit":
			return node("object", ast);
		case "KeyValueProp":
			return node("pair", ast);
		case "MethodProp":
		case "GetterProp":
		case "SetterProp":
			return method(ast);
		case "ComputedPropName":
			return node(parentType(ast).equals("MemberExpr") || parentType(ast).equals("SuperPropExpr") ? INLINE : "computed_property_name", ast);
		case "SpreadElement":
			return node("spread_element", ast);
		case "ExprOrSpread":
			return exprOrSpread(ast);
		case "ObjectPat":
			return pattern("object_pattern", ast);
		case "ArrayPat":
			return pattern("array_pattern", ast);
		case "AssignPat":
			return node("assignment_pattern", ast);
		case "AssignPatProp":
			return assignPatProp(ast);
		case "KeyValuePatProp":
			return node("pair_pattern", ast);
		case "RestPat":
			return restPattern(ast);
		case "MemberExpr":
			return memberExpression(ast);
		case "SuperPropExpr":
			return memberExpression(ast);
		case "OptChainExpr":
			return node(INLINE, ast);
		case "CallExpr":
		case "OptCall":
			return callExpression(ast);
		case "NewExpr":
			return newExpression(ast);
		case "TaggedTpl":
			return node("call_expression", ast);
		case "Super":
			return leaf("super", ast);
		case "Import":
			return node("import", ast);
		case "ThisExpr":
			return leaf("this", ast);
		case "MetaPropExpr":
			return node("meta_property", ast, List.of());
		case "AwaitExpr":
			return node("await_expression", ast);
		case "YieldExpr":
			return node("yield_expression", ast);
		case "ArrayLit":
			return node("array", ast);
		case "ParenExpr":
			return node("parenthesized_expression", ast);
		case "SeqExpr":
			return node("sequence_expression", ast);
		case "BinExpr":
			return node("binary_expression", ast);
		case "AssignExpr":
			return assignment(ast);
		case "UnaryExpr":
			return node("unary_expression", ast);
		case "UpdateExpr":
			return node("update_expression", ast);
		case "CondExpr":
			return node("ternary_expression", ast);
		case "Str":
			return string(start(ast), end(ast));
		case "Number":
		case "BigInt":
			return leaf("number", ast);
		case "Bool":
			return leaf(content.startsWith("true", start(ast)) ? "true" : "false", ast);
		case "Null":
			return leaf("null", ast);
		case "Regex":
			return regex(ast);
		case "Tpl":
			return template(ast);
		case "TplElement":
			return null;
		case "TsAsExpr":
			return node("as_expression", ast);
		case "TsConstAssertion":
			return node("as_expression", ast);
		case "TsSatisfiesExpr":
			return node("satisfies_expression", ast);
		case "TsNonNullExpr":
			return node("non_null_expression", ast);
		case "TsTypeAssertion":
			return typeAssertion(ast);
		case "TsInstantiation":
			return node("instantiation_expression", ast);
		case "ImportDecl":
			return importStatement(ast);
		case "ImportDefaultSpecifier":
			return node(INLINE, ast);
		case "ImportNamedSpecifier":
			return node("import_specifier", ast);
		case "ImportStarAsSpecifier":
			return node("namespace_import", ast);
		case "ExportDecl":
		case "ExportDefaultDecl":
			return exportDeclaration(ast);
		case "ExportDefaultExpr":
		case "ExportAll":
		case "TsExportAssignment":
		case "TsNamespaceExportDecl":
			return node("export_statement", ast);
		case "NamedExport":
			return namedExport(ast);
		case "ExportNamedSpecifier":
			return node("export_specifier", ast);
		case "ExportNamespaceSpecifier":
			return node("namespace_export", ast);
		case "ExportDefaultSpecifier":
			return node(INLINE, ast);
		case "TsImportEqualsDecl":
			return importAlias(ast);
		case "TsExternalModuleRef":
			return node(INLINE, ast);
		case "JsxElement":
			return jsxElement(ast);
		case "JsxFragment":
			return node("jsx_element", ast);
		case "JsxOpeningElement":
			return jsxOpeningElement(ast);
		case "JsxClosingElement":
			return node("jsx_closing_element", ast);
		case "JsxOpeningFragment":
			return node("jsx_opening_element", ast);
		case "JsxClosingFragment":
			return node("jsx_closing_element", ast);
		case "JsxAttr":
			return node("jsx_attribute", ast);
		case "JsxExprContainer":
			return node("jsx_expression", ast);
		case "JsxEmptyExpr":
			return null;
		case "JsxText":
			return jsxText(ast);
		case "JsxMemberExpr":
			return node("member_expression", ast);
		case "JsxNamespacedName":
			return node("jsx_namespace_name", ast, ast.getChildNodes().stream().map(c -> leaf("identifier", c)).toList());
		case "JsxSpreadChild":
			return jsxSpreadChild(ast);
		//types
		case "TsTypeAnn":
			return typeAnnotation(ast);
		case "TsKeywordType":
			return keywordType((Swc4jAstTsKeywordType) ast);
		case "TsThisType":
			return leaf(parentType(ast).equals("TsTypePredicate") ? "this" : "this_type", ast);
		case "TsTypeRef":
			return typeReference(ast);
		case "TsQualifiedName":
			return qualifiedName(ast);
		case "TsArrayType":
			return node("array_type", ast);
		case "TsUnionType":
			return unionType("union_type", ast);
		case "TsIntersectionType":
			return unionType("intersection_type", ast);
		case "TsFnType":
			return functionType("function_type", ast);
		case "TsConstructorType":
			return functionType("constructor_type", ast);
		case "TsTypeLit":
			return node("object_type", ast);
		case "TsInterfaceBody":
			return node("interface_body", ast);
		case "TsPropertySignature":
			return memberSignature("property_signature", ast);
		case "TsMethodSignature":
			return functionType("method_signature", ast);
		case "TsGetterSignature":
		case "TsSetterSignature":
			return functionType("method_signature", ast);
		case "TsCallSignatureDecl":
			return functionType("call_signature", ast);
		case "TsConstructSignatureDecl":
			return functionType("construct_signature", ast);
		case "TsIndexSignature":
			return memberSignature("index_signature", ast);
		case "TsLitType":
			if(ast.getChildNodes().size() == 1 && type(ast.getChildNodes().get(0)).equals("TsTplLitType")) {
				return convert(ast.getChildNodes().get(0));
			}
			return node("literal_type", ast);
		case "TsTplLitType":
			return template(ast);
		case "TsTupleType":
			return node("tuple_type", ast);
		case "TsTupleElement":
			return tupleElement(ast);
		case "TsOptionalType":
			return node("optional_type", ast);
		case "TsRestType":
			return restType(ast);
		case "TsTypeOperator":
			return typeOperator((Swc4jAstTsTypeOperator) ast);
		case "TsIndexedAccessType":
			return node("lookup_type", ast);
		case "TsTypeQuery":
			return node("type_query", ast);
		case "TsImportType":
			return importType(ast);
		case "TsParenthesizedType":
			return node("parenthesized_type", ast);
		case "TsConditionalType":
			return node("conditional_type", ast);
		case "TsInferType":
			return node("infer_type", ast, ast.getChildNodes().stream().map(c -> type(c).equals("TsTypeParam") ? inferParameter(c) : convert(c)).toList());
		case "TsMappedType":
			return mappedType(ast);
		case "TsTypePredicate":
			return typePredicate(ast);
		case "TsTypeParamDecl":
			return node("type_parameters", ast);
		case "TsTypeParam":
			return typeParameter(ast);
		case "TsTypeParamInstantiation":
			return node("type_arguments", ast);
		case "TsExprWithTypeArgs":
			return expressionWithTypeArguments(ast);
		case "TsInterfaceDecl":
			return ambient(ast, interfaceDeclaration(ast));
		case "TsTypeAliasDecl":
			return ambient(ast, node("type_alias_declaration", ast));
		case "TsEnumDecl":
			return ambient(ast, enumDeclaration(ast));
		case "TsEnumMember":
			return enumMember(ast);
		case "TsModuleDecl":
			return moduleDeclaration((Swc4jAstTsModuleDecl) ast);
		case "TsNamespaceDecl":
			return node("internal_module", ast);
		case "UsingDecl":
			return node("lexical_declaration", ast);
		default:
			return node("SWC:" + type, ast);
		}
	}

	private Node identifier(Swc4jAstIdent ident) {
		String parent = parentType(ident);
		String type = "identifier";
		switch(parent) {
		case "TsTypeRef":
		case "TsInterfaceDecl":
		case "TsTypeAliasDecl":
		case "TsTypeParam":
		case "TsExprWithTypeArgs":
			type = "type_identifier";
			break;
		case "ClassDecl":
		case "ClassExpr":
			type = typescript ? "type_identifier" : "identifier";
			break;
		case "TsIndexSignature":
			type = "identifier";
			break;
		case "LabeledStmt":
		case "BreakStmt":
		case "ContinueStmt":
			type = "statement_identifier";
			break;
		case "ObjectLit":
			type = "shorthand_property_identifier";
			break;
		case "TsEnumMember":
		case "TsPropertySignature":
		case "TsMethodSignature":
		case "TsGetterSignature":
		case "TsSetterSignature":
		case "JsxAttr":
			type = "property_identifier";
			break;
		case "ExportNamedSpecifier":
			if(ident.getSym().equals("default") && start(ident) > start(ident.getParent())) {
				type = "default";
			}
			break;
		default:
			if(ident.getSym().equals("undefined") && !parent.equals("BindingIdent")) {
				type = "undefined";
			}
			else if(ident.getSym().equals("this") && parent.equals("BindingIdent")) {
				type = "this";
			}
		}
		int start = start(ident);
		int end = end(ident);
		if(content.startsWith(ident.getSym(), start)) {
			end = start + ident.getSym().length();
		}
		return leaf(type, start, end);
	}

	private Node variableDeclaration(Swc4jAstVarDecl ast) {
		String type = ast.getKind() == Swc4jAstVarDeclKind.Var ? "variable_declaration" : "lexical_declaration";
		String parent = parentType(ast);
		if(parent.equals("ForInStmt") || parent.equals("ForOfStmt")) {
			//for (const x of y) has the kind and the pattern without declaration
			List<Node> children = new ArrayList<>();
			for(ISwc4jAst declarator : ast.getDecls()) {
				children.addAll(convertChildren(declarator));
			}
			return node(INLINE, ast, children);
		}
		int end = end(ast);
		if(parent.equals("ForStmt")) {
			//the declaration in for (let i = 0; ...) ends with the semicolon
			int semicolon = find(";", end, content.length());
			if(semicolon >= 0) {
				end = semicolon + 1;
			}
		}
		if(ast.isDeclare()) {
			int kind = find(ast.getKind().name().toLowerCase(), start(ast), end);
			Node declaration = node(type, kind, end, convertChildren(ast));
			return node("ambient_declaration", start(ast), end, List.of(declaration));
		}
		return node(type, start(ast), end, convertChildren(ast));
	}

	//declare interface, type and enum
	private Node ambient(ISwc4jAst ast, Node declaration) {
		if(declaration.first() != null && declaration.first().type.equals("declare")) {
			Node declare = declaration.children.remove(0);
			declaration.start = declaration.first().start;
			return node("ambient_declaration", start(ast), end(ast), List.of(declare, declaration));
		}
		return declaration;
	}

	private Node bindingIdentifier(ISwc4jAst ast) {
		//the identifier and its type annotation
		return node(INLINE, ast);
	}

	private Node ifStatement(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		ISwc4jAst test = children.get(0);
		ISwc4jAst consequent = children.get(1);
		List<Node> nodes = new ArrayList<>();
		nodes.add(parenthesized(test, start(ast) + 2));
		nodes.add(convert(consequent));
		if(children.size() > 2) {
			ISwc4jAst alternate = children.get(2);
			int elseStart = findLast("else", end(consequent), start(alternate));
			nodes.add(node("else_clause", elseStart, end(alternate), List.of(convert(alternate))));
		}
		return node("if_statement", ast, nodes);
	}

	//a parenthesized_expression with the parentheses around the expression
	private Node parenthesized(ISwc4jAst expression, int from) {
		int open = findLast("(", from, start(expression));
		int close = find(")", end(expression), content.length());
		return node("parenthesized_expression", open, close + 1, List.of(convert(expression)));
	}

	private Node parenthesizedFirst(String type, ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		List<Node> nodes = new ArrayList<>();
		nodes.add(parenthesized(children.get(0), start(ast)));
		for(int i = 1; i < children.size(); i++) {
			nodes.add(convert(children.get(i)));
		}
		return node(type, ast, nodes);
	}

	private Node doStatement(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		return node("do_statement", ast, List.of(convert(children.get(0)), parenthesized(children.get(1), end(children.get(0)))));
	}

	private Node switchStatement(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		Node discriminant = parenthesized(children.get(0), start(ast));
		int open = find("{", discriminant.end, end(ast));
		List<Node> cases = convert(children.subList(1, children.size()));
		return node("switch_statement", ast, List.of(discriminant, node("switch_body", open, end(ast), cases)));
	}

	private Node forStatement(ISwc4jAst ast) {
		List<Node> children = convertChildren(ast);
		Node node = node("for_statement", ast, children);
		//an empty initializer or condition is an empty_statement
		int open = find("(", start(ast), end(ast));
		List<Node> result = new ArrayList<>();
		int semicolons = 0;
		for(Node child : node.children) {
			if(child.type.equals(";") && child.parent == node) {
				semicolons++;
				Node previous = result.isEmpty() ? null : result.get(result.size() - 1);
				boolean empty = previous == null || previous.type.equals("(") || previous.type.equals(";") || previous.type.equals("empty_statement")
						|| previous.type.endsWith("_declaration");
				if(empty && !(previous != null && previous.type.endsWith("_declaration") && semicolons == 1)) {
					Node statement = new Node("empty_statement");
					statement.start = child.start;
					statement.end = child.end;
					statement.add(child);
					statement.parent = node;
					result.add(statement);
					continue;
				}
			}
			result.add(child);
		}
		node.children.clear();
		for(Node child : result) {
			node.add(child);
		}
		return node;
	}

	private Node tryStatement(ISwc4jAst ast) {
		Node node = node("try_statement", ast);
		Node finallyKeyword = null;
		for(Node child : node.children) {
			if(child.type.equals("finally") && child.leaf) {
				finallyKeyword = child;
			}
		}
		if(finallyKeyword != null) {
			group(node, "finally_clause", finallyKeyword.start, node.last().end);
		}
		return node;
	}

	private Node forInStatement(ISwc4jAst ast) {
		return node("for_in_statement", ast);
	}

	//functions

	private Node functionDeclaration(Swc4jAstFnDecl ast) {
		Swc4jAstFunction function = ast.getFunction();
		String type = function.getBody().isEmpty() ? "function_signature" : function.isGenerator() ? "generator_function_declaration" : "function_declaration";
		Node node = node(type, ast, convertChildren(ast));
		if(ast.isDeclare()) {
			Node declare = node.children.remove(0);
			node.start = node.first().start;
			return node("ambient_declaration", start(ast), end(ast), List.of(declare, node));
		}
		return node;
	}

	private Node functionExpression(ISwc4jAst ast) {
		Swc4jAstFunction function = (Swc4jAstFunction) ast.getChildNodes().stream().filter(c -> c instanceof Swc4jAstFunction).findFirst().get();
		boolean declaration = parentType(ast).equals("ExportDefaultDecl") && ast.getChildNodes().stream().anyMatch(c -> type(c).equals("Ident"));
		if(declaration) {
			return node(function.isGenerator() ? "generator_function_declaration" : "function_declaration", ast);
		}
		return node(function.isGenerator() ? "generator_function" : "function_expression", ast);
	}

	//the parts of a function, with the parameters in formal_parameters
	private Node function(Swc4jAstFunction function) {
		List<Node> children = new ArrayList<>();
		int parametersStart = start(function);
		for(ISwc4jAst child : function.getChildNodes()) {
			if(type(child).equals("Param")) {
				continue;
			}
			if(type(child).equals("TsTypeParamDecl") || type(child).equals("Decorator")) {
				parametersStart = Math.max(parametersStart, end(child));
			}
			children.add(convert(child));
		}
		ISwc4jAst parent = function.getParent();
		if(parent != null) {
			for(ISwc4jAst sibling : parent.getChildNodes()) {
				if(sibling != function && end(sibling) <= start(function) + 1000000 && start(sibling) >= start(function) && end(sibling) <= end(function)) {
					parametersStart = Math.max(parametersStart, end(sibling));
				}
				else if(sibling != function && start(sibling) < start(function)) {
					//the key of a method
				}
			}
		}
		int parametersEnd = function.getReturnType().map(TypeScriptTreeGenerator::start).orElse(function.getBody().map(TypeScriptTreeGenerator::start).orElse(end(function)));
		children.add(formalParameters(function.getParams(), parametersStart, parametersEnd));
		return inline(children);
	}

	private Node formalParameters(List<? extends ISwc4jAst> parameters, int from, int to) {
		int open = find("(", from, to);
		int close = findLast(")", open, to);
		List<Node> nodes = new ArrayList<>();
		for(ISwc4jAst parameter : parameters) {
			String type = type(parameter);
			nodes.add(typescript && !type.equals("Param") && !type.equals("TsParamProp") ? typedParameter(parameter, parameter, List.of()) : convert(parameter));
		}
		return node("formal_parameters", open, close + 1, nodes);
	}

	private Node parameter(ISwc4jAst ast) {
		List<ISwc4jAst> children = ast.getChildNodes();
		ISwc4jAst pattern = children.get(children.size() - 1);
		if(!typescript) {
			return node(INLINE, ast);
		}
		return typedParameter(ast, pattern, convert(children.subList(0, children.size() - 1)));
	}

	//required_parameter or optional_parameter with the pattern, its type annotation and its default value
	private Node typedParameter(ISwc4jAst ast, ISwc4jAst pattern, List<Node> decorators) {
		List<Node> children = new ArrayList<>(decorators);
		String type = "required_parameter";
		if(type(pattern).equals("AssignPat")) {
			for(ISwc4jAst child : pattern.getChildNodes()) {
				children.add(start(child) == start(pattern) ? patternWithType(child) : convert(child));
			}
		}
		else {
			children.add(patternWithType(pattern));
			if(type(pattern).equals("BindingIdent") && ((com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent) pattern).getId().isOptional()) {
				type = "optional_parameter";
			}
		}
		int start = start(ast);
		for(Node child : children) {
			if(child != null && child.start >= 0) {
				start = Math.min(start, child.start);
			}
		}
		return node(type, start, end(ast), children);
	}

	//a pattern followed by its type annotation, i.e., {y}: P
	private Node patternWithType(ISwc4jAst pattern) {
		String type = type(pattern);
		if(type.equals("BindingIdent")) {
			return node(INLINE, pattern);
		}
		if(type.equals("ObjectPat") || type.equals("ArrayPat") || type.equals("RestPat")) {
			return pattern(type.equals("ObjectPat") ? "object_pattern" : type.equals("ArrayPat") ? "array_pattern" : "rest_pattern", pattern);
		}
		return convert(pattern);
	}

	//object_pattern, array_pattern and rest_pattern followed by their type annotation
	private Node pattern(String type, ISwc4jAst ast) {
		List<Node> children = new ArrayList<>();
		Node typeAnnotation = null;
		for(ISwc4jAst child : ast.getChildNodes()) {
			if(type(child).equals("TsTypeAnn")) {
				typeAnnotation = convert(child);
			}
			else {
				children.add(convert(child));
			}
		}
		int start = start(ast);
		if(type.equals("rest_pattern") && !content.startsWith("...", start)) {
			//the span of a rest parameter without type may start after the ...
			int dots = start - 1;
			while(dots > 0 && Character.isWhitespace(content.charAt(dots))) {
				dots--;
			}
			if(dots >= 2 && content.startsWith("...", dots - 2)) {
				start = dots - 2;
			}
		}
		if(type.equals("rest_pattern") && typeAnnotation == null) {
			return node(type, start, end(ast), children);
		}
		int end = typeAnnotation == null ? end(ast) : typeAnnotation.start;
		if(type.equals("rest_pattern")) {
			Node rest = node(type, start, lastEnd(children, end), children);
			return typeAnnotation == null ? rest : inline(List.of(rest, typeAnnotation));
		}
		int close = findLast(type.equals("object_pattern") ? "}" : "]", start(ast), end);
		Node node = node(type, start(ast), close + 1, children);
		return typeAnnotation == null ? node : inline(List.of(node, typeAnnotation));
	}

	private static int lastEnd(List<Node> nodes, int limit) {
		int end = -1;
		for(Node node : nodes) {
			if(node != null && node.end <= limit) {
				end = Math.max(end, node.end);
			}
		}
		return end;
	}

	private Node restPattern(ISwc4jAst ast) {
		if(typescript && parentType(ast).equals("Param")) {
			return pattern("rest_pattern", ast);
		}
		return pattern("rest_pattern", ast);
	}

	private Node assignPatProp(ISwc4jAst ast) {
		List<ISwc4jAst> children = ast.getChildNodes();
		ISwc4jAst key = children.get(0);
		Node identifier = leaf("shorthand_property_identifier_pattern", start(key), start(key) + ((com.caoccao.javet.swc4j.ast.pat.Swc4jAstBindingIdent) key).getId().getSym().length());
		if(children.size() == 1) {
			return identifier;
		}
		List<Node> nodes = new ArrayList<>();
		nodes.add(identifier);
		nodes.add(convert(children.get(1)));
		return node("object_assignment_pattern", ast, nodes);
	}

	private Node arrowFunction(Swc4jAstArrowExpr ast) {
		List<Node> children = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			if(!ast.getParams().contains(child)) {
				children.add(convert(child));
			}
		}
		int bodyOrArrow = findLast("=>", start(ast), start(ast.getBody()));
		int parametersStart = ast.getTypeParams().map(TypeScriptTreeGenerator::end).orElse(start(ast));
		int parametersEnd = ast.getReturnType().map(TypeScriptTreeGenerator::start).orElse(bodyOrArrow);
		int open = find("(", parametersStart, parametersEnd);
		if(open < 0 && ast.getParams().size() == 1) {
			//x => x
			children.add(convertParameterWithoutParentheses(ast.getParams().get(0)));
		}
		else {
			int close = findLast(")", open, parametersEnd);
			List<Node> parameters = new ArrayList<>();
			for(ISwc4jAst parameter : ast.getParams()) {
				parameters.add(typescript ? typedParameter(parameter, parameter, List.of()) : convert(parameter));
			}
			children.add(node("formal_parameters", open, close + 1, parameters));
		}
		return node("arrow_function", ast, children);
	}

	private Node convertParameterWithoutParentheses(ISwc4jAst parameter) {
		return node(INLINE, parameter);
	}

	//classes

	private Node classDeclaration(ISwc4jAst ast) {
		Swc4jAstClass clazz = (Swc4jAstClass) ast.getChildNodes().stream().filter(c -> c instanceof Swc4jAstClass).findFirst().get();
		boolean declaration = type(ast).equals("ClassDecl") || parentType(ast).equals("ExportDefaultDecl") && ast.getChildNodes().stream().anyMatch(c -> type(c).equals("Ident"));
		String type = !declaration ? "class" : clazz.isAbstract() ? "abstract_class_declaration" : "class_declaration";
		Node node = node(type, ast);
		if(type(ast).equals("ClassDecl") && node.first() != null && node.first().type.equals("declare")) {
			Node declare = node.children.remove(0);
			node.start = node.first().start;
			return node("ambient_declaration", start(ast), end(ast), List.of(declare, node));
		}
		return node;
	}

	private Node classInline(Swc4jAstClass clazz) {
		List<Node> children = new ArrayList<>();
		List<Node> heritage = new ArrayList<>();
		List<Node> members = new ArrayList<>();
		for(ISwc4jAst child : clazz.getChildNodes()) {
			Node node = convert(child);
			if(clazz.getBody().contains(child)) {
				members.add(node);
			}
			else if(clazz.getImplements().contains(child) || clazz.getSuperClass().map(s -> s == child).orElse(false)
					|| clazz.getSuperTypeParams().map(s -> s == child).orElse(false)) {
				heritage.add(node);
			}
			else {
				children.add(node);
			}
		}
		int bodyStart;
		int heritageEnd = lastEnd(heritage, end(clazz));
		int from = Math.max(heritageEnd, clazz.getTypeParams().map(TypeScriptTreeGenerator::end).orElse(start(clazz)));
		for(Node child : children) {
			if(child.type.equals("type_identifier") || child.type.equals("identifier")) {
				from = Math.max(from, child.end);
			}
		}
		bodyStart = find("{", from, end(clazz));
		if(!heritage.isEmpty()) {
			int nameEnd = start(clazz);
			for(Node child : children) {
				if(child.type.equals("type_identifier") || child.type.equals("identifier") || child.type.equals("type_parameters")) {
					nameEnd = Math.max(nameEnd, child.end);
				}
			}
			int heritageFrom = Math.max(nameEnd, clazz.getTypeParams().map(TypeScriptTreeGenerator::end).orElse(start(clazz)));
			int extendsStart = find("extends", heritageFrom, bodyStart);
			int implementsStart = find("implements", heritageFrom, bodyStart);
			int heritageStart = extendsStart >= 0 ? extendsStart : implementsStart;
			if(typescript) {
				List<Node> clauses = new ArrayList<>();
				List<Node> extendsNodes = new ArrayList<>();
				List<Node> implementsNodes = new ArrayList<>();
				for(Node node : heritage) {
					if(implementsStart >= 0 && node.start > implementsStart) {
						implementsNodes.add(node);
					}
					else {
						extendsNodes.add(node);
					}
				}
				if(extendsStart >= 0) {
					clauses.add(node("extends_clause", extendsStart, lastEnd(extendsNodes, bodyStart), extendsNodes));
				}
				if(implementsStart >= 0) {
					clauses.add(node("implements_clause", implementsStart, lastEnd(implementsNodes, bodyStart), implementsNodes));
				}
				children.add(node("class_heritage", heritageStart, heritageEnd, clauses));
			}
			else {
				children.add(node("class_heritage", heritageStart, heritageEnd, heritage));
			}
		}
		children.add(node("class_body", bodyStart, end(clazz), members));
		return inline(children);
	}

	private Node method(ISwc4jAst ast) {
		String type = "method_definition";
		if(ast instanceof Swc4jAstClassMethod method && method.getFunction().getBody().isEmpty()) {
			type = method.isAbstract() ? "abstract_method_signature" : "method_signature";
		}
		else if(type(ast).equals("Constructor") && ast.getChildNodes().stream().noneMatch(c -> type(c).equals("BlockStmt"))) {
			type = "method_signature";
		}
		List<Node> children = new ArrayList<>();
		List<ISwc4jAst> parameters = new ArrayList<>();
		ISwc4jAst key = null;
		for(ISwc4jAst child : ast.getChildNodes()) {
			String childType = type(child);
			if(type(ast).equals("Constructor") && (childType.equals("Param") || childType.equals("TsParamProp"))) {
				parameters.add(child);
			}
			else if((type(ast).equals("SetterProp")) && !childType.equals("BlockStmt") && start(child) > start(ast) + 3 && key != null) {
				parameters.add(child);
			}
			else {
				if(key == null && isPropertyName(childType)) {
					key = child;
				}
				children.add(convert(child));
			}
		}
		if(type(ast).equals("Constructor") || type(ast).equals("SetterProp") || type(ast).equals("GetterProp")) {
			int from = key != null ? end(key) : start(ast);
			int to = ast.getChildNodes().stream().filter(c -> type(c).equals("BlockStmt") || type(c).equals("TsTypeAnn")).mapToInt(TypeScriptTreeGenerator::start).min().orElse(end(ast));
			children.add(formalParameters(parameters, from, to));
		}
		int end = end(ast);
		if(!type.equals("method_definition") && content.charAt(end - 1) == ';') {
			end = trimSeparator(start(ast), end);
		}
		List<Node> decorators = new ArrayList<>();
		int start = start(ast);
		if(typescript && type.equals("method_definition") && parentType(ast).equals("Class")) {
			//the decorators of a method are in the class body
			List<Node> flattened = new ArrayList<>();
			for(Node child : children) {
				if(child != null && child.type.equals(INLINE)) {
					flattened.addAll(child.children);
				}
				else if(child != null) {
					flattened.add(child);
				}
			}
			children = flattened;
			for(Node child : new ArrayList<>(children)) {
				if(child.type.equals("decorator")) {
					children.remove(child);
					decorators.add(child);
					start = Math.max(start, child.end);
				}
			}
			if(!decorators.isEmpty()) {
				start = skip(start, end);
			}
		}
		Node node = node(type, start, end, children);
		wrapTokens(node, ACCESSIBILITY, "accessibility_modifier");
		wrapTokens(node, Set.of("override"), "override_modifier");
		if(!decorators.isEmpty()) {
			decorators.add(node);
			return inline(decorators);
		}
		return node;
	}

	private static boolean isPropertyName(String type) {
		return type.equals("IdentName") || type.equals("PrivateName") || type.equals("ComputedPropName") || type.equals("Str") || type.equals("Number") || type.equals("BigInt") || type.equals("Ident");
	}

	//the end before a trailing ; or ,
	private int trimSeparator(int start, int end) {
		int separator = end - 1;
		while(separator > start && Character.isWhitespace(content.charAt(separator))) {
			separator--;
		}
		if(content.charAt(separator) == ';' || content.charAt(separator) == ',') {
			int trimmed = separator;
			while(trimmed > start && Character.isWhitespace(content.charAt(trimmed - 1))) {
				trimmed--;
			}
			return trimmed;
		}
		return end;
	}

	private Node fieldDefinition(ISwc4jAst ast) {
		int end = trimSeparator(start(ast), end(ast));
		Node node = node(typescript ? "public_field_definition" : "field_definition", start(ast), end, convertChildren(ast));
		wrapTokens(node, ACCESSIBILITY, "accessibility_modifier");
		wrapTokens(node, Set.of("override"), "override_modifier");
		return node;
	}

	private Node parameterProperty(ISwc4jAst ast) {
		List<ISwc4jAst> children = ast.getChildNodes();
		ISwc4jAst pattern = children.get(children.size() - 1);
		Node node = typedParameter(ast, pattern, convert(children.subList(0, children.size() - 1)));
		wrapTokens(node, ACCESSIBILITY, "accessibility_modifier");
		wrapTokens(node, Set.of("override"), "override_modifier");
		return node;
	}

	//expressions

	private Node exprOrSpread(ISwc4jAst ast) {
		List<Node> children = convertChildren(ast);
		if(nextTokenStart(start(ast)) < children.get(0).start && content.startsWith("...", start(ast))) {
			return node("spread_element", ast, children);
		}
		return children.get(0);
	}

	private Node memberExpression(ISwc4jAst ast) {
		List<Node> children = convertChildren(ast);
		boolean computed = ast.getChildNodes().stream().anyMatch(c -> type(c).equals("ComputedPropName"));
		Node node = node(computed ? "subscript_expression" : "member_expression", ast, children);
		if(typescript) {
			wrapTokens(node, Set.of("?."), "optional_chain");
		}
		else {
			renameTokens(node, "?.", "optional_chain");
		}
		return node;
	}

	private Node callExpression(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		List<Node> nodes = new ArrayList<>();
		List<ISwc4jAst> arguments = new ArrayList<>();
		int calleeEnd = start(ast);
		for(ISwc4jAst child : children) {
			if(type(child).equals("ExprOrSpread")) {
				arguments.add(child);
			}
			else {
				nodes.add(convert(child));
				calleeEnd = Math.max(calleeEnd, end(child));
			}
		}
		int open = find("(", calleeEnd, end(ast));
		nodes.add(node("arguments", open, end(ast), convert(arguments)));
		return node("call_expression", ast, nodes);
	}

	private Node newExpression(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		List<Node> nodes = new ArrayList<>();
		List<ISwc4jAst> arguments = new ArrayList<>();
		int calleeEnd = start(ast);
		for(ISwc4jAst child : children) {
			if(type(child).equals("ExprOrSpread")) {
				arguments.add(child);
			}
			else {
				nodes.add(convert(child));
				calleeEnd = Math.max(calleeEnd, end(child));
			}
		}
		int open = find("(", calleeEnd, end(ast));
		if(open >= 0) {
			nodes.add(node("arguments", open, end(ast), convert(arguments)));
		}
		return node("new_expression", ast, nodes);
	}

	private Node assignment(ISwc4jAst ast) {
		List<Node> children = convertChildren(ast);
		Node node = node("assignment_expression", ast, children);
		for(Node child : node.children) {
			if(child.leaf && child.type.endsWith("=") && !child.type.equals("=") && child.parent == node && child.start >= node.first().end) {
				return node("augmented_assignment_expression", ast, children);
			}
		}
		return node;
	}

	//literals

	private Node string(int start, int end) {
		char quote = content.charAt(start);
		List<Node> children = new ArrayList<>();
		children.add(leaf(String.valueOf(quote), start, start + 1));
		int fragmentStart = -1;
		int i = start + 1;
		int contentEnd = end - 1;
		while(i < contentEnd) {
			if(content.charAt(i) == '\\') {
				if(fragmentStart >= 0) {
					children.add(leaf("string_fragment", fragmentStart, i));
					fragmentStart = -1;
				}
				int escapeEnd = escapeEnd(i, contentEnd);
				children.add(leaf("escape_sequence", i, escapeEnd));
				i = escapeEnd;
			}
			else {
				if(fragmentStart < 0) {
					fragmentStart = i;
				}
				i++;
			}
		}
		if(fragmentStart >= 0) {
			children.add(leaf("string_fragment", fragmentStart, contentEnd));
		}
		children.add(leaf(String.valueOf(quote), end - 1, end));
		Node node = new Node("string");
		node.start = start;
		node.end = end;
		for(Node child : children) {
			node.add(child);
		}
		return node;
	}

	private int escapeEnd(int start, int limit) {
		int i = start + 1;
		if(i >= limit) {
			return limit;
		}
		char character = content.charAt(i);
		if(character == 'x') {
			return Math.min(i + 3, limit);
		}
		if(character == 'u') {
			if(i + 1 < limit && content.charAt(i + 1) == '{') {
				int close = content.indexOf('}', i);
				return close < 0 ? limit : Math.min(close + 1, limit);
			}
			return Math.min(i + 5, limit);
		}
		if(character >= '0' && character <= '7') {
			int end = i + 1;
			while(end < limit && end < i + 3 && content.charAt(end) >= '0' && content.charAt(end) <= '7') {
				end++;
			}
			return end;
		}
		if(character == '\r' && i + 1 < limit && content.charAt(i + 1) == '\n') {
			return i + 2;
		}
		return i + 1;
	}

	private Node regex(ISwc4jAst ast) {
		int start = start(ast);
		int end = end(ast);
		int close = content.lastIndexOf('/', end - 1);
		List<Node> children = new ArrayList<>();
		children.add(leaf("/", start, start + 1));
		children.add(leaf("regex_pattern", start + 1, close));
		children.add(leaf("/", close, close + 1));
		if(close + 1 < end) {
			children.add(leaf("regex_flags", close + 1, end));
		}
		Node node = new Node("regex");
		node.start = start;
		node.end = end;
		for(Node child : children) {
			node.add(child);
		}
		return node;
	}

	//template_string and template_literal_type with the fragments, escape sequences and substitutions
	private Node template(ISwc4jAst ast) {
		int start = start(ast);
		int end = end(ast);
		List<ISwc4jAst> expressions = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			if(!type(child).equals("TplElement")) {
				expressions.add(child);
			}
		}
		expressions.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		Node node = new Node(type(ast).equals("TsTplLitType") ? "template_literal_type" : "template_string");
		node.start = start;
		node.end = end;
		node.add(leaf("`", start, start + 1));
		int i = start + 1;
		int index = 0;
		int fragmentStart = -1;
		while(i < end - 1) {
			char character = content.charAt(i);
			if(character == '$' && i + 1 < end && content.charAt(i + 1) == '{' && index < expressions.size()) {
				if(fragmentStart >= 0) {
					node.add(leaf("string_fragment", fragmentStart, i));
					fragmentStart = -1;
				}
				ISwc4jAst expression = expressions.get(index++);
				int close = find("}", end(expression), end);
				Node substitution = node(type(ast).equals("TsTplLitType") ? "template_type" : "template_substitution", i, close + 1, List.of(convert(expression)));
				node.add(substitution);
				i = close + 1;
			}
			else if(character == '\\') {
				if(fragmentStart >= 0) {
					node.add(leaf("string_fragment", fragmentStart, i));
					fragmentStart = -1;
				}
				int escapeEnd = escapeEnd(i, end - 1);
				node.add(leaf("escape_sequence", i, escapeEnd));
				i = escapeEnd;
			}
			else {
				if(fragmentStart < 0) {
					fragmentStart = i;
				}
				i++;
			}
		}
		if(fragmentStart >= 0) {
			node.add(leaf("string_fragment", fragmentStart, end - 1));
		}
		node.add(leaf("`", end - 1, end));
		return node;
	}

	//modules

	private Node importStatement(ISwc4jAst ast) {
		List<Node> specifiers = new ArrayList<>();
		List<Node> others = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			String type = type(child);
			if(type.endsWith("Specifier")) {
				specifiers.add(convert(child));
			}
			else if(type.equals("ObjectLit")) {
				int with = findLast("with", start(ast), start(child));
				others.add(node("import_attribute", with, end(child), List.of(convert(child))));
			}
			else {
				others.add(convert(child));
			}
		}
		if(!specifiers.isEmpty()) {
			specifiers.sort(Comparator.comparingInt(n -> n.start));
			int from = find("from", lastEnd(specifiers, end(ast)), end(ast));
			int clauseStart = specifiers.get(0).start;
			int clauseEnd = from;
			List<Node> named = new ArrayList<>();
			List<Node> clause = new ArrayList<>();
			for(Node specifier : specifiers) {
				if(specifier.type.equals("import_specifier")) {
					named.add(specifier);
				}
				else {
					clause.add(specifier);
				}
			}
			int open = find("{", start(ast), from);
			if(open >= 0) {
				int close = findLast("}", open, from);
				clause.add(node("named_imports", open, close + 1, named));
				clauseStart = Math.min(clauseStart, open);
			}
			clauseEnd = lastEnd(clause, from);
			others.add(node("import_clause", clauseStart, clauseEnd, clause));
		}
		return node("import_statement", ast, others);
	}

	//the decorators of an exported class are before export
	private Node exportDeclaration(ISwc4jAst ast) {
		List<Node> children = convertChildren(ast);
		List<Node> decorators = new ArrayList<>();
		for(Node child : children) {
			for(Node grandChild : new ArrayList<>(child.children)) {
				if(grandChild.type.equals("decorator") && grandChild.start < start(ast)) {
					child.children.remove(grandChild);
					decorators.add(grandChild);
				}
			}
			if(!decorators.isEmpty() && !child.children.isEmpty()) {
				child.start = child.first().start;
			}
		}
		children.addAll(decorators);
		int start = decorators.isEmpty() ? start(ast) : Math.min(start(ast), decorators.get(0).start);
		return node("export_statement", start, end(ast), children);
	}

	private Node namedExport(ISwc4jAst ast) {
		List<Node> specifiers = new ArrayList<>();
		List<Node> others = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			if(type(child).equals("ExportNamedSpecifier")) {
				specifiers.add(convert(child));
			}
			else {
				others.add(convert(child));
			}
		}
		int open = find("{", start(ast), end(ast));
		if(open >= 0) {
			int close = find("}", Math.max(open, lastEnd(specifiers, end(ast))), end(ast));
			others.add(node("export_clause", open, close + 1, specifiers));
		}
		else {
			others.addAll(specifiers);
		}
		return node("export_statement", ast, others);
	}

	private Node importAlias(ISwc4jAst ast) {
		Node node = node("import_alias", ast);
		for(Node child : node.children) {
			if(child.type.equals("require")) {
				//import x = require('y')
				int equals = -1;
				for(Node c : node.children) {
					if(c.type.equals("=")) {
						equals = c.start;
					}
				}
				Node name = node.children.get(1);
				Node clause = group(node, "import_require_clause", name.start, findLast(")", child.end, node.end) + 1);
				Node statement = new Node("import_statement");
				statement.start = node.start;
				statement.end = node.end;
				for(Node c : new ArrayList<>(node.children)) {
					statement.add(c);
				}
				return statement;
			}
		}
		return node;
	}

	//JSX

	private Node jsxElement(ISwc4jAst ast) {
		List<ISwc4jAst> children = ast.getChildNodes();
		if(children.size() == 1 && type(children.get(0)).equals("JsxOpeningElement") && content.startsWith("/>", end(ast) - 2)) {
			return jsxOpeningElement(children.get(0));
		}
		return node("jsx_element", ast);
	}

	private Node jsxOpeningElement(ISwc4jAst ast) {
		boolean selfClosing = content.startsWith("/>", end(ast) - 2);
		List<Node> children = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			if(type(child).equals("SpreadElement")) {
				int open = findLast("{", start(ast), start(child));
				int close = find("}", end(child), end(ast));
				children.add(node("jsx_expression", open, close + 1, List.of(node("spread_element", child))));
			}
			else {
				children.add(convert(child));
			}
		}
		return node(selfClosing ? "jsx_self_closing_element" : "jsx_opening_element", ast, children);
	}

	private Node jsxSpreadChild(ISwc4jAst ast) {
		ISwc4jAst expression = ast.getChildNodes().get(0);
		int dots = findLast("...", start(ast), start(expression));
		return node("jsx_expression", ast, List.of(node("spread_element", dots, end(expression), List.of(convert(expression)))));
	}

	//the text without the whitespace-only parts, and the html character references
	private Node jsxText(ISwc4jAst ast) {
		List<Node> nodes = new ArrayList<>();
		int start = start(ast);
		int end = end(ast);
		int textStart = start;
		int i = start;
		while(i < end) {
			int referenceEnd = content.charAt(i) == '&' ? characterReferenceEnd(i, end) : -1;
			if(referenceEnd > 0) {
				addJsxText(nodes, textStart, i);
				nodes.add(leaf("html_character_reference", i, referenceEnd));
				i = referenceEnd;
				textStart = i;
			}
			else {
				i++;
			}
		}
		addJsxText(nodes, textStart, end);
		return inline(nodes);
	}

	private void addJsxText(List<Node> nodes, int start, int end) {
		if(!content.substring(start, end).isBlank()) {
			nodes.add(leaf("jsx_text", start, end));
		}
	}

	private int characterReferenceEnd(int start, int end) {
		int i = start + 1;
		while(i < end && i < start + 33 && (Character.isLetterOrDigit(content.charAt(i)) || content.charAt(i) == '#')) {
			i++;
		}
		return i > start + 1 && i < end && content.charAt(i) == ';' ? i + 1 : -1;
	}

	//types

	private Node typeAnnotation(ISwc4jAst ast) {
		boolean predicate = ast.getChildNodes().stream().anyMatch(c -> type(c).equals("TsTypePredicate"));
		boolean asserts = ast.getChildNodes().stream().anyMatch(c -> c instanceof com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypePredicate p && p.isAsserts());
		if(asserts) {
			return node("asserts_annotation", ast);
		}
		String parent = parentType(ast);
		if(parent.equals("TsTypePredicate") || parent.equals("TsMappedType")
				|| parent.equals("BindingIdent") && parentType(ast.getParent()).equals("TsIndexSignature")) {
			return node(INLINE, ast);
		}
		return node(predicate ? "type_predicate_annotation" : "type_annotation", ast);
	}

	private Node keywordType(Swc4jAstTsKeywordType ast) {
		String text = content.substring(start(ast), end(ast));
		if(text.equals("undefined") || text.equals("null")) {
			return node("literal_type", start(ast), end(ast), List.of(leaf(text, ast)));
		}
		return node("predefined_type", ast, List.of());
	}

	private Node typeReference(ISwc4jAst ast) {
		List<Node> children = convertChildren(ast);
		boolean typeArguments = ast.getChildNodes().stream().anyMatch(c -> type(c).equals("TsTypeParamInstantiation"));
		if(!typeArguments) {
			return children.get(0);
		}
		return node("generic_type", ast, children);
	}

	private Node qualifiedName(ISwc4jAst ast) {
		//A.B is nested_type_identifier, A.B.C has a nested_identifier
		if(parentType(ast).equals("TsTypeQuery")) {
			return qualifiedNameAsMember(ast, "member_expression");
		}
		boolean type = parentType(ast).equals("TsTypeRef") || parentType(ast).equals("TsExprWithTypeArgs");
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		ISwc4jAst left = children.get(0);
		ISwc4jAst right = children.get(1);
		Node leftNode = type(left).equals("TsQualifiedName") ? qualifiedNameAsMember(left, type ? "nested_identifier" : "member_expression") : leaf("identifier", left);
		Node rightNode = leaf(type ? "type_identifier" : "property_identifier", right);
		return node(type ? "nested_type_identifier" : "nested_identifier", ast, List.of(leftNode, rightNode));
	}

	private Node qualifiedNameAsMember(ISwc4jAst ast, String typeName) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		ISwc4jAst left = children.get(0);
		Node leftNode = type(left).equals("TsQualifiedName") ? qualifiedNameAsMember(left, "member_expression") : leaf("identifier", left);
		return node(typeName, ast, List.of(leftNode, leaf("property_identifier", children.get(1))));
	}

	private Node unionType(String type, ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		Node left = null;
		int start = start(ast);
		for(ISwc4jAst child : children) {
			Node right = convert(child);
			if(left == null) {
				if(nextTokenStart(start) < right.start) {
					//a leading |
					left = node(type, start, right.end, List.of(right));
				}
				else {
					left = right;
				}
			}
			else {
				left = node(type, left.start, right.end, List.of(left, right));
			}
		}
		return left;
	}

	private Node functionType(String type, ISwc4jAst ast) {
		List<Node> children = new ArrayList<>();
		List<ISwc4jAst> parameters = new ArrayList<>();
		int from = start(ast);
		int to = end(ast);
		for(ISwc4jAst child : ast.getChildNodes()) {
			String childType = type(child);
			if(childType.equals("BindingIdent") || childType.endsWith("Pat") || childType.equals("Param")) {
				parameters.add(child);
			}
			else {
				//the return type of a function type is not a type_annotation
				Node node = childType.equals("TsTypeAnn") && type.endsWith("_type") ? node(INLINE, child) : convert(child);
				children.add(node);
				if(childType.equals("TsTypeParamDecl") || start(child) < nextParenthesis(ast)) {
					from = Math.max(from, end(child));
				}
				else if(childType.equals("TsTypeAnn") || start(child) > nextParenthesis(ast)) {
					to = Math.min(to, start(child));
				}
			}
		}
		int arrow = -1;
		if(type.endsWith("_type")) {
			int parametersEnd = from;
			for(ISwc4jAst parameter : parameters) {
				parametersEnd = Math.max(parametersEnd, end(parameter));
			}
			int close = find(")", parametersEnd, end(ast));
			arrow = find("=>", close, end(ast));
			to = Math.min(to, arrow);
		}
		List<Node> converted = new ArrayList<>();
		for(ISwc4jAst parameter : parameters) {
			converted.add(typescript ? typedParameter(parameter, parameter, List.of()) : convert(parameter));
		}
		int open = find("(", from, to);
		int close = findLast(")", open, to);
		children.add(node("formal_parameters", open, close + 1, converted));
		int end = type.endsWith("_type") ? end(ast) : trimSeparator(start(ast), end(ast));
		return node(type, start(ast), end, children);
	}

	private static int returnTypeStart(ISwc4jAst ast) {
		return ast.getChildNodes().stream().filter(c -> type(c).equals("TsTypeAnn")).mapToInt(TypeScriptTreeGenerator::start).max().orElse(end(ast));
	}

	private int nextParenthesis(ISwc4jAst ast) {
		int open = find("(", start(ast), end(ast));
		return open < 0 ? end(ast) : open;
	}

	private Node memberSignature(String type, ISwc4jAst ast) {
		int end = trimSeparator(start(ast), end(ast));
		return node(type, start(ast), end, convertChildren(ast));
	}

	private Node tupleElement(ISwc4jAst ast) {
		List<Node> children = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			if(type(child).equals("RestPat")) {
				//the span of a rest label includes the colon
				ISwc4jAst binding = child.getChildNodes().get(0);
				children.add(node("rest_pattern", start(child), end(binding), List.of(convert(binding))));
			}
			else {
				children.add(convert(child));
			}
		}
		if(ast.getChildNodes().size() > 1) {
			//a labeled element is a parameter with a type annotation
			children.sort(Comparator.comparingInt(n -> n.start));
			Node label = children.get(0);
			Node type = children.get(children.size() - 1);
			int colon = find(":", label.end, type.start);
			List<Node> labelNodes = new ArrayList<>(children.subList(0, children.size() - 1));
			labelNodes.add(node("type_annotation", colon, type.end, List.of(type)));
			boolean optional = content.substring(label.start, colon).contains("?");
			return node(optional ? "optional_parameter" : "required_parameter", ast, labelNodes);
		}
		return children.get(0);
	}

	private Node typeOperator(Swc4jAstTsTypeOperator ast) {
		String operator = content.substring(start(ast), find(" ", start(ast), end(ast)) < 0 ? start(ast) + 5 : start(ast) + 5).trim();
		if(content.startsWith("keyof", start(ast))) {
			return node("index_type_query", ast);
		}
		if(content.startsWith("readonly", start(ast))) {
			return node("readonly_type", ast);
		}
		if(content.startsWith("unique", start(ast))) {
			//unique symbol is a predefined_type with two unique symbol tokens
			Node node = node("predefined_type", ast, List.of());
			List<Node> tokens = new ArrayList<>(node.children);
			node.children.clear();
			for(Node token : tokens) {
				Node renamed = leaf("unique symbol", token.start, token.end);
				node.add(renamed);
			}
			return node;
		}
		return node("type_operator", ast);
	}

	//{ [K in keyof T]: T[K] } is an object_type with an index_signature
	private Node mappedType(ISwc4jAst ast) {
		com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsMappedType mapped = (com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsMappedType) ast;
		ISwc4jAst typeParam = mapped.getTypeParam();
		List<Node> clause = new ArrayList<>();
		int clauseEnd = end(typeParam);
		for(ISwc4jAst child : typeParam.getChildNodes()) {
			clause.add(type(child).equals("Ident") ? leaf("type_identifier", child) : convert(child));
		}
		if(mapped.getNameType().isPresent()) {
			clause.add(convert(mapped.getNameType().get()));
			clauseEnd = end(mapped.getNameType().get());
		}
		int open = findLast("[", start(ast), start(typeParam));
		int close = find("]", clauseEnd, end(ast));
		List<Node> signature = new ArrayList<>();
		signature.add(node("mapped_type_clause", start(typeParam), clauseEnd, clause));
		int signatureEnd = close + 1;
		if(mapped.getTypeAnn().isPresent()) {
			ISwc4jAst typeAnn = mapped.getTypeAnn().get();
			int colon = findLast(":", close + 1, start(typeAnn));
			int tokenStart = skip(close + 1, colon);
			String token = content.substring(tokenStart, colon + 1).replaceAll("\\s", "");
			String annotationType = token.equals("?:") ? "opting_type_annotation" : token.equals("+?:") ? "adding_type_annotation" : token.equals("-?:") ? "omitting_type_annotation" : "type_annotation";
			Node annotation = new Node(annotationType);
			annotation.start = tokenStart;
			annotation.end = end(typeAnn);
			annotation.add(leaf(token, tokenStart, colon + 1));
			annotation.add(convert(typeAnn));
			signature.add(annotation);
			signatureEnd = annotation.end;
		}
		int signatureStart = skip(start(ast) + 1, open + 1);
		Node indexSignature = node("index_signature", signatureStart, signatureEnd, signature);
		return node("object_type", start(ast), end(ast), List.of(indexSignature));
	}

	private Node inferParameter(ISwc4jAst typeParam) {
		if(typeParam.getChildNodes().size() == 1) {
			return leaf("type_identifier", typeParam.getChildNodes().get(0));
		}
		return typeParameter(typeParam);
	}

	//...rest: T[] in a tuple is a parameter with a rest_pattern
	private Node restType(ISwc4jAst ast) {
		String text = content.substring(start(ast), end(ast));
		ISwc4jAst type = ast.getChildNodes().get(0);
		int colon = find(":", start(ast) + 3, start(type));
		if(colon >= 0 && parentType(ast).equals("TsTupleElement")) {
			int nameStart = skip(start(ast) + 3, colon);
			int nameEnd = tokenEnd(nameStart, colon);
			Node rest = node("rest_pattern", start(ast), nameEnd, List.of(leaf("identifier", nameStart, nameEnd)));
			return node("required_parameter", ast, List.of(rest, node("type_annotation", colon, end(type), List.of(convert(type)))));
		}
		return node("rest_type", ast);
	}

	//x is T, asserts x is T and asserts x
	private Node typePredicate(ISwc4jAst ast) {
		com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypePredicate predicate = (com.caoccao.javet.swc4j.ast.ts.Swc4jAstTsTypePredicate) ast;
		if(!predicate.isAsserts()) {
			return node("type_predicate", ast);
		}
		ISwc4jAst name = predicate.getParamName();
		Node nameNode = type(name).equals("TsThisType") ? leaf("this", name) : leaf("identifier", name);
		if(predicate.getTypeAnn().isEmpty()) {
			return node("asserts", ast, List.of(nameNode));
		}
		Node typePredicate = node("type_predicate", start(name), end(ast), List.of(nameNode, convert(predicate.getTypeAnn().get())));
		return node("asserts", ast, List.of(typePredicate));
	}

	private Node typeAssertion(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		ISwc4jAst type = children.get(0);
		int close = find(">", end(type), end(ast));
		return node("type_assertion", ast, List.of(node("type_arguments", start(ast), close + 1, List.of(convert(type))), convert(children.get(1))));
	}

	private Node typeParameter(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		List<Node> nodes = new ArrayList<>();
		ISwc4jAst name = children.get(0);
		nodes.add(leaf("type_identifier", name));
		int previousEnd = end(name);
		for(int i = 1; i < children.size(); i++) {
			ISwc4jAst child = children.get(i);
			int extendsStart = find("extends", previousEnd, start(child));
			int equalsStart = find("=", previousEnd, start(child));
			if(extendsStart >= 0) {
				nodes.add(node("constraint", extendsStart, end(child), List.of(convert(child))));
			}
			else if(equalsStart >= 0) {
				nodes.add(node("default_type", equalsStart, end(child), List.of(convert(child))));
			}
			else {
				nodes.add(convert(child));
			}
			previousEnd = end(child);
		}
		return node("type_parameter", ast, nodes);
	}

	private Node expressionWithTypeArguments(ISwc4jAst ast) {
		List<Node> children = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			//React.Component in extends of interfaces and implements of classes is a type
			children.add(type(child).equals("MemberExpr") ? memberAsNestedType(child, true) : convert(child));
		}
		String parent = parentType(ast);
		boolean hasTypeArguments = ast.getChildNodes().stream().anyMatch(c -> type(c).equals("TsTypeParamInstantiation"));
		if(parent.equals("Class") && !hasTypeArguments) {
			return children.get(0);
		}
		if(hasTypeArguments) {
			return node("generic_type", ast, children);
		}
		return children.get(0);
	}

	private Node interfaceDeclaration(ISwc4jAst ast) {
		List<Node> children = new ArrayList<>();
		List<Node> extendsTypes = new ArrayList<>();
		for(ISwc4jAst child : ast.getChildNodes()) {
			if(type(child).equals("TsExprWithTypeArgs")) {
				extendsTypes.add(convert(child));
			}
			else {
				children.add(convert(child));
			}
		}
		if(!extendsTypes.isEmpty()) {
			extendsTypes.sort(Comparator.comparingInt(n -> n.start));
			int extendsStart = findLast("extends", start(ast), extendsTypes.get(0).start);
			children.add(node("extends_type_clause", extendsStart, lastEnd(extendsTypes, end(ast)), extendsTypes));
		}
		return node("interface_declaration", ast, children);
	}

	//import('./m').X is a member_expression of a call_expression
	private Node importType(ISwc4jAst ast) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		ISwc4jAst argument = children.get(0);
		int open = findLast("(", start(ast), start(argument));
		int close = find(")", end(argument), end(ast));
		Node call = node("call_expression", start(ast), close + 1,
				List.of(node("import", start(ast), start(ast) + 6, List.of()), node("arguments", open, close + 1, List.of(convert(argument)))));
		Node node = call;
		for(int i = 1; i < children.size(); i++) {
			ISwc4jAst child = children.get(i);
			if(type(child).equals("TsTypeParamInstantiation")) {
				node = node("generic_type", node.start, end(child), List.of(node, convert(child)));
			}
			else {
				node = qualifiedImport(node, child);
			}
		}
		return node;
	}

	private Node qualifiedImport(Node object, ISwc4jAst name) {
		if(type(name).equals("TsQualifiedName")) {
			List<ISwc4jAst> children = new ArrayList<>(name.getChildNodes());
			children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
			Node left = qualifiedImport(object, children.get(0));
			return node("member_expression", object.start, end(children.get(1)), List.of(left, leaf("property_identifier", children.get(1))));
		}
		return node("member_expression", object.start, end(name), List.of(object, leaf("property_identifier", name)));
	}

	private Node enumDeclaration(ISwc4jAst ast) {
		Node node = node("enum_declaration", ast);
		int open = -1;
		for(Node child : node.children) {
			if(child.type.equals("{") && open < 0) {
				open = child.start;
			}
		}
		group(node, "enum_body", open, end(ast));
		return node;
	}

	private Node memberAsNestedType(ISwc4jAst ast, boolean type) {
		List<ISwc4jAst> children = new ArrayList<>(ast.getChildNodes());
		children.sort(Comparator.comparingInt(TypeScriptTreeGenerator::start));
		ISwc4jAst object = children.get(0);
		Node objectNode = type(object).equals("MemberExpr") ? memberAsNestedType(object, false) : leaf("identifier", object);
		if(type) {
			if(objectNode.type.equals("member_expression")) {
				objectNode = node("nested_identifier", objectNode.start, objectNode.end, new ArrayList<>(objectNode.children));
			}
			return node("nested_type_identifier", ast, List.of(objectNode, leaf("type_identifier", children.get(1))));
		}
		return node("member_expression", ast, List.of(objectNode, leaf("property_identifier", children.get(1))));
	}

	private Node enumMember(ISwc4jAst ast) {
		List<Node> children = convertChildren(ast);
		if(ast.getChildNodes().size() == 1) {
			return children.get(0);
		}
		return node("enum_assignment", ast, children);
	}

	private Node moduleDeclaration(Swc4jAstTsModuleDecl ast) {
		List<Node> children = convertChildren(ast);
		String type = ast.isGlobal() ? INLINE : ast.isNamespace() ? "internal_module" : "module";
		int start = start(ast);
		if(ast.isDeclare()) {
			int keyword = find(ast.isGlobal() ? "global" : ast.isNamespace() ? "namespace" : "module", start, end(ast));
			if(ast.isGlobal()) {
				List<Node> global = new ArrayList<>();
				for(Node child : children) {
					global.add(child.type.equals("identifier") && content.startsWith("global", child.start) ? leaf("global", child.start, child.end) : child);
				}
				return node("ambient_declaration", ast, global);
			}
			int end = ast.getBody().isPresent() ? end(ast) : trimSeparator(start, end(ast));
			Node module = node(type, keyword, end, children);
			return node("ambient_declaration", start, end, List.of(module));
		}
		return node(type.isEmpty() ? "module" : type, ast, children);
	}

	//comments

	private static final Set<String> SEMICOLON_TERMINATED = Set.of("expression_statement", "lexical_declaration", "variable_declaration", "return_statement",
			"throw_statement", "break_statement", "continue_statement", "debugger_statement", "do_statement", "import_statement", "export_statement",
			"type_alias_declaration", "import_alias", "statement_block");

	//the automatic semicolon of a statement without semicolon, after the comments in its line
	private void addAutomaticSemicolons(Node node, List<Node> anchors) {
		for(Node child : node.children) {
			addAutomaticSemicolons(child, anchors);
		}
		//a statement_block ends with an optional automatic semicolon
		boolean block = node.type.equals("statement_block");
		if(SEMICOLON_TERMINATED.contains(node.type) && node.last() != null && (block || !node.last().type.equals(";") && !node.last().type.endsWith("_block")
				&& !node.last().type.endsWith("declaration") && !node.last().type.equals("class_body"))) {
			int position = node.end;
			while(position < content.length()) {
				char character = content.charAt(position);
				Integer commentEnd = comments.get(position);
				if(character == ' ' || character == '\t') {
					position++;
				}
				else if(commentEnd != null && content.substring(position, commentEnd).indexOf('\n') < 0) {
					position = commentEnd;
				}
				else {
					break;
				}
			}
			//the automatic semicolon is before a line break, a closing brace or the end of the file
			boolean inserted = position == content.length() || content.charAt(position) == '\n' || content.charAt(position) == '\r' || content.charAt(position) == '}';
			if(position > node.end && inserted) {
				Node anchor = new Node("automatic_semicolon");
				anchor.start = position;
				anchor.end = position;
				anchor.parent = node;
				anchors.add(anchor);
			}
		}
	}

	private void addComments(Node root) {
		List<Node> leaves = new ArrayList<>();
		collectLeaves(root, leaves);
		List<Node> anchors = new ArrayList<>();
		addAutomaticSemicolons(root, anchors);
		leaves.addAll(anchors);
		leaves.sort(Comparator.comparingInt(n -> n.start));
		for(var entry : comments.entrySet()) {
			int start = entry.getKey();
			int end = entry.getValue();
			Node comment = leaf("comment", start, end);
			int index = 0;
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
			index = low;
			Node previous = index > 0 ? leaves.get(index - 1) : null;
			Node next = index < leaves.size() ? leaves.get(index) : null;
			Node parent;
			if(previous == null || next == null) {
				parent = root;
			}
			else {
				parent = commonAncestor(previous.type.equals("automatic_semicolon") ? previous.parent : previous, next.type.equals("automatic_semicolon") ? next.parent : next);
			}
			if(parent.leaf || parent.type.equals("automatic_semicolon")) {
				parent = parent.parent;
			}
			//a comment before the automatic semicolon extends the statement
			for(Node node = parent; node != null && node.end < end; node = node.parent) {
				node.end = end;
			}
			int position = 0;
			while(position < parent.children.size() && parent.children.get(position).start < start) {
				position++;
			}
			comment.parent = parent;
			parent.children.add(position, comment);
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
		Tree tree = leaf ? context.createTree(TypeSet.type(node.type), content.substring(node.start, node.end)) : context.createTree(TypeSet.type(node.type));
		tree.setPos(Math.max(node.start, 0));
		tree.setLength(Math.max(node.end - node.start, 0));
		for(Node child : node.children) {
			tree.addChild(toTree(child, context));
		}
		return tree;
	}
}
