package org.refactoringminer.astDiff.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

import extension.base.Utf16CharStream;
import extension.base.lang.python.PythonLexer;
import extension.base.lang.python.PythonParser;

/**
 * Generates the AST diff tree of a Python file from the ANTLR parse tree, which is also used to generate the RefactoringMiner model,
 * with the node types and the structure of the tree-sitter-python tree after the gumtree rules (rules.yml), which was used before,
 * so that the AST diff matchers and the diffs remain the same, i.e., the punctuation and some keywords are removed,
 * the comparison, logical and assignment operators are aliased, and the strings are flattened.
 * The comments and the line continuations are placed like the extras of tree-sitter,
 * i.e., in the smallest node containing the tokens before and after them, and the comments before a dedent in the block with the same indentation.
 */
public class PythonTreeGenerator {
	//the anonymous tokens removed by the gumtree rules, and the '=' of default_parameter, and the '*' of wildcard_import
	private static final Set<String> IGNORED = Set.of("(", ")", "{", "}", "[", "]", ".", ":", ",", "def", "for", "in", "if", "with", "return");
	private static final Map<String, String> ALIASED = Map.ofEntries(
			Map.entry("==", "comparison_operator_literal"), Map.entry("<=", "comparison_operator_literal"), Map.entry(">=", "comparison_operator_literal"),
			Map.entry("!=", "comparison_operator_literal"), Map.entry("<", "comparison_operator_literal"), Map.entry(">", "comparison_operator_literal"),
			Map.entry("<>", "comparison_operator_literal"),
			Map.entry("and", "logical_operator_literal"), Map.entry("or", "logical_operator_literal"),
			Map.entry("=", "assignment_operator_literal"), Map.entry("-=", "assignment_operator_literal"), Map.entry("+=", "assignment_operator_literal"),
			Map.entry("*=", "assignment_operator_literal"), Map.entry("/=", "assignment_operator_literal"), Map.entry("//=", "assignment_operator_literal"),
			Map.entry("%=", "assignment_operator_literal"), Map.entry("**=", "assignment_operator_literal"));

	public static TreeContext generate(String content) {
		PythonLexer lexer = new PythonLexer(new Utf16CharStream(content));
		lexer.removeErrorListeners();
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		PythonParser parser = new PythonParser(tokens);
		parser.removeErrorListeners();
		PythonParser.File_inputContext fileInput = parser.file_input();
		return generate(content, tokens, fileInput);
	}

	public static TreeContext generate(String content, CommonTokenStream tokens, PythonParser.File_inputContext fileInput) {
		TreeContext context = new TreeContext();
		if(content.isEmpty()) {
			Tree root = context.createTree(TypeSet.type("empty_root"));
			root.setPos(0);
			root.setLength(0);
			context.setRoot(root);
			return context;
		}
		tokens.fill();
		PythonTreeGenerator generator = new PythonTreeGenerator(content, tokens.getTokens());
		if(hasSyntaxError(fileInput)) {
			//Python 2 print statements are parsed as pass; followed by the printed expressions, with the same offsets
			Map<Integer, Integer> prints = python2Prints(content, tokens.getTokens());
			if(!prints.isEmpty()) {
				char[] chars = content.toCharArray();
				for(Map.Entry<Integer, Integer> print : prints.entrySet()) {
					"pass;".getChars(0, 5, chars, print.getKey());
					if(print.getValue() >= 0) {
						chars[print.getValue()] = ' ';
						chars[print.getValue() + 1] = ' ';
					}
				}
				tokens = new CommonTokenStream(new PythonLexer(new Utf16CharStream(new String(chars))));
				PythonParser parser = new PythonParser(tokens);
				parser.removeErrorListeners();
				fileInput = parser.file_input();
				tokens.fill();
				generator = new PythonTreeGenerator(content, tokens.getTokens());
				generator.prints.putAll(prints);
			}
		}
		Node module = generator.module(fileInput);
		module.computeRange();
		generator.addExtras(module);
		module.computeRange();
		//the module ends at the end of the file
		module.end = content.length();
		if(module.children.isEmpty()) {
			module.start = content.length();
		}
		context.setRoot(generator.toTree(module, context));
		return context;
	}

	//a sequence of nodes without a parent node, i.e., the - and the number of a negative number pattern
	private static final String INLINE = "";

	private static class Node {
		private final String type;
		private int start = -1;
		private int end = -1;
		private Node parent;
		private final List<Node> children = new ArrayList<>();
		//a tree-sitter leaf or flattened node, which has the source code as label
		private boolean leaf;
		//an anonymous token removed by the gumtree rules
		private boolean ignored;

		private Node(String type) {
			this.type = type;
		}

		private Node add(Node child) {
			if(child != null && child.type.equals(INLINE)) {
				//the children of an inline node are added to the parent
				addAll(new ArrayList<>(child.children));
			}
			else if(child != null) {
				child.parent = this;
				children.add(child);
			}
			return this;
		}

		private Node addAll(List<Node> nodes) {
			for(Node node : nodes) {
				add(node);
			}
			return this;
		}

		private void computeRange() {
			if(children.isEmpty()) {
				return;
			}
			int start = Integer.MAX_VALUE;
			int end = Integer.MIN_VALUE;
			for(Node child : children) {
				child.computeRange();
				if(child.start >= 0) {
					start = Math.min(start, child.start);
					end = Math.max(end, child.end);
				}
			}
			if(start != Integer.MAX_VALUE) {
				this.start = start;
				this.end = end;
			}
		}

		private int depth() {
			int depth = 0;
			for(Node node = parent; node != null; node = node.parent) {
				depth++;
			}
			return depth;
		}
	}

	private final String content;
	private final List<Token> tokens;
	private final Node[] tokenOwners;
	private final Map<Node, Integer> blockIndents = new IdentityHashMap<>();
	//the newlines ending the simple statements and decorators, at the end of their line like in tree-sitter, while ANTLR emits them before the next statement
	private final List<Node> newlines = new ArrayList<>();
	//the char offsets of the Python 2 print statements, and of their chevrons (>>) or -1
	private final Map<Integer, Integer> prints = new HashMap<>();

	private PythonTreeGenerator(String content, List<Token> tokens) {
		this.content = content;
		this.tokens = tokens;
		this.tokenOwners = new Node[tokens.size()];
	}

	private void register(TerminalNode terminal, Node owner) {
		int index = terminal.getSymbol().getTokenIndex();
		if(index >= 0) {
			tokenOwners[index] = owner;
		}
	}

	private Node tokenNode(String type, Token token) {
		Node node = new Node(type);
		node.start = token.getStartIndex();
		node.end = token.getStopIndex() + 1;
		node.leaf = true;
		if(token.getTokenIndex() >= 0) {
			tokenOwners[token.getTokenIndex()] = node;
		}
		return node;
	}

	//an anonymous token, removed or aliased by the gumtree rules
	private Node token(TerminalNode terminal) {
		if(terminal == null) {
			return null;
		}
		String text = terminal.getText();
		Node node = tokenNode(ALIASED.getOrDefault(text, text), terminal.getSymbol());
		node.ignored = IGNORED.contains(text);
		return node;
	}

	private Node ignoredToken(TerminalNode terminal) {
		Node node = token(terminal);
		if(node != null) {
			node.ignored = true;
		}
		return node;
	}

	private Node leaf(String type, TerminalNode terminal) {
		return tokenNode(type, terminal.getSymbol());
	}

	//name or name_except_underscore
	private Node identifier(ParseTree name) {
		return leaf("identifier", firstTerminal(name));
	}

	private static TerminalNode firstTerminal(ParseTree tree) {
		while(!(tree instanceof TerminalNode)) {
			tree = tree.getChild(0);
		}
		return (TerminalNode) tree;
	}

	private static Node node(String type, Node... children) {
		Node node = new Node(type);
		for(Node child : children) {
			node.add(child);
		}
		return node;
	}

	private static boolean isToken(ParseTree tree, int type) {
		return tree instanceof TerminalNode terminal && terminal.getSymbol().getType() == type;
	}

	private static boolean isToken(ParseTree tree, String text) {
		return tree instanceof TerminalNode terminal && terminal.getText().equals(text);
	}

	private static List<ParseTree> children(ParserRuleContext ctx) {
		return ctx.children == null ? List.of() : ctx.children;
	}

	//statements

	private Node module(PythonParser.File_inputContext ctx) {
		Node module = new Node("module");
		for(ParseTree child : children(ctx)) {
			if(child instanceof PythonParser.StatementsContext statements) {
				statements(statements, module);
			}
			else if(child instanceof ErrorNode errorNode) {
				module.add(error(errorNode));
			}
		}
		return module;
	}

	private void statements(PythonParser.StatementsContext ctx, Node container) {
		for(ParseTree child : children(ctx)) {
			if(child instanceof PythonParser.StatementContext statement) {
				statement(statement, container);
			}
			else if(child instanceof ErrorNode errorNode) {
				container.add(error(errorNode));
			}
		}
	}

	private void statement(PythonParser.StatementContext ctx, Node container) {
		if(hasError(ctx)) {
			container.add(error(ctx));
		}
		else if(ctx.compound_stmt() != null) {
			container.add(compoundStatement(ctx.compound_stmt()));
		}
		else if(ctx.simple_stmts() != null) {
			simpleStatements(ctx.simple_stmts(), container);
		}
	}

	private void simpleStatements(PythonParser.Simple_stmtsContext ctx, Node container) {
		List<ParseTree> children = children(ctx);
		for(int i = 0; i < children.size(); i++) {
			ParseTree child = children.get(i);
			if(child instanceof PythonParser.Simple_stmtContext simpleStatement && simpleStatement.pass_stmt() != null
					&& prints.containsKey(simpleStatement.getStart().getStartIndex())) {
				//pass; expressions of a Python 2 print statement
				int start = simpleStatement.getStart().getStartIndex();
				Node print = new Node("print_statement");
				Node keyword = new Node("print");
				keyword.start = start;
				keyword.end = start + 5;
				keyword.leaf = true;
				print.add(keyword);
				tokenOwners[simpleStatement.getStart().getTokenIndex()] = keyword;
				tokenOwners[((TerminalNode) children.get(i + 1)).getSymbol().getTokenIndex()] = keyword;
				i++;
				List<Node> elements = new ArrayList<>();
				if(i + 1 < children.size() && children.get(i + 1) instanceof PythonParser.Simple_stmtContext printed && printed.star_expressions() != null) {
					elements = elements(printed.star_expressions());
					i++;
				}
				int chevron = prints.get(start);
				if(chevron >= 0 && !elements.isEmpty()) {
					Node operator = new Node(">>");
					operator.start = chevron;
					operator.end = chevron + 2;
					operator.leaf = true;
					print.add(node("chevron", operator, elements.remove(0)));
				}
				print.addAll(elements);
				container.add(print);
			}
			else if(child instanceof PythonParser.Simple_stmtContext simpleStatement) {
				container.add(simpleStatement(simpleStatement));
			}
			else if(isToken(child, PythonParser.NEWLINE)) {
				newline(ctx.getStop().getTokenIndex(), container);
			}
			else if(child instanceof TerminalNode terminal) {
				container.add(token(terminal));
			}
		}
	}

	//a newline anchor owned by the container, at the end of the line of the previous token
	private void newline(int newlineTokenIndex, Node owner) {
		int index = newlineTokenIndex - 1;
		while(index >= 0 && (tokens.get(index).getChannel() != Token.DEFAULT_CHANNEL || tokens.get(index).getType() == PythonParser.NEWLINE)) {
			index--;
		}
		int position = lineEnd(index >= 0 ? tokens.get(index).getStopIndex() + 1 : 0);
		Node newline = new Node("newline");
		newline.start = position;
		newline.end = position;
		newline.parent = owner;
		newlines.add(newline);
	}

	private Node simpleStatement(PythonParser.Simple_stmtContext ctx) {
		ParseTree child = ctx.getChild(0);
		if(child instanceof PythonParser.AssignmentContext assignment) {
			return node("expression_statement", assignment(assignment));
		}
		if(child instanceof PythonParser.Star_expressionsContext expressions) {
			//a tuple without parentheses is not an expression_list
			return new Node("expression_statement").addAll(elements(expressions));
		}
		if(child instanceof PythonParser.Return_stmtContext returnStatement) {
			Node node = node("return_statement", token(returnStatement.getToken(PythonParser.RETURN, 0)));
			if(returnStatement.star_expressions() != null) {
				node.add(expressions(returnStatement.star_expressions()));
			}
			return node;
		}
		if(child instanceof PythonParser.Import_stmtContext importStatement) {
			return importStatement(importStatement);
		}
		if(child instanceof PythonParser.Raise_stmtContext raiseStatement) {
			Node node = new Node("raise_statement");
			for(ParseTree raiseChild : children(raiseStatement)) {
				node.add(raiseChild instanceof TerminalNode terminal ? token(terminal) : expression((ParserRuleContext) raiseChild));
			}
			return node;
		}
		if(child instanceof PythonParser.Pass_stmtContext || child instanceof PythonParser.Break_stmtContext || child instanceof PythonParser.Continue_stmtContext) {
			String keyword = child.getText();
			return node(keyword + "_statement", token((TerminalNode) child.getChild(0)));
		}
		if(child instanceof PythonParser.Del_stmtContext deleteStatement) {
			return node("delete_statement", token(deleteStatement.getToken(PythonParser.DEL, 0)), deleteTargets(deleteStatement.del_targets()));
		}
		if(child instanceof PythonParser.Yield_stmtContext yieldStatement) {
			return node("expression_statement", expression(yieldStatement.yield_expr()));
		}
		if(child instanceof PythonParser.Assert_stmtContext || child instanceof PythonParser.Global_stmtContext || child instanceof PythonParser.Nonlocal_stmtContext) {
			String type = child instanceof PythonParser.Assert_stmtContext ? "assert_statement" : child instanceof PythonParser.Global_stmtContext ? "global_statement" : "nonlocal_statement";
			Node node = new Node(type);
			for(ParseTree statementChild : children((ParserRuleContext) child)) {
				if(statementChild instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else if(statementChild instanceof PythonParser.NameContext name) {
					node.add(identifier(name));
				}
				else {
					node.add(expression((ParserRuleContext) statementChild));
				}
			}
			return node;
		}
		if(child instanceof PythonParser.Type_aliasContext typeAlias) {
			Node node = new Node("type_alias_statement");
			for(ParseTree aliasChild : children(typeAlias)) {
				if(aliasChild instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else if(aliasChild instanceof PythonParser.NameContext name) {
					node.add(node("type", identifier(name)));
				}
				else if(aliasChild instanceof PythonParser.Type_paramsContext typeParams) {
					//type Alias[T] is a generic_type
					Node type = node.children.get(node.children.size() - 1);
					Node name = type.children.remove(0);
					type.add(node("generic_type", name, typeParameters(typeParams)));
				}
				else {
					node.add(type((ParserRuleContext) aliasChild));
				}
			}
			return node;
		}
		return error(ctx);
	}

	private Node assignment(PythonParser.AssignmentContext ctx) {
		List<ParseTree> children = children(ctx);
		if(ctx.augassign() != null) {
			TerminalNode operator = (TerminalNode) ctx.augassign().getChild(0);
			return node("assignment", singleTarget(ctx.single_target()), token(operator), annotatedRightHandSide(ctx.annotated_rhs()));
		}
		if(ctx.getToken(PythonParser.COLON, 0) != null) {
			//annotated assignment
			Node node = new Node("assignment");
			for(ParseTree child : children) {
				if(child instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else if(child instanceof PythonParser.NameContext name) {
					node.add(identifier(name));
				}
				else if(child instanceof PythonParser.Single_targetContext target) {
					node.add(singleTarget(target));
				}
				else if(child instanceof PythonParser.Single_subscript_attribute_targetContext target) {
					node.add(targetPrimary(target));
				}
				else if(child instanceof PythonParser.ExpressionContext expression) {
					node.add(type(expression));
				}
				else if(child instanceof PythonParser.Annotated_rhsContext rightHandSide) {
					node.add(annotatedRightHandSide(rightHandSide));
				}
			}
			return node;
		}
		//chained assignment a = b = c is assignment(a, =, assignment(b, =, c))
		List<Node> targets = new ArrayList<>();
		List<Node> operators = new ArrayList<>();
		Node rightHandSide = null;
		for(ParseTree child : children) {
			if(child instanceof PythonParser.Star_targetsContext targets_) {
				targets.add(targets(targets_));
			}
			else if(isToken(child, PythonParser.EQUAL)) {
				operators.add(token((TerminalNode) child));
			}
			else if(child instanceof PythonParser.Annotated_rhsContext annotatedRightHandSide) {
				rightHandSide = annotatedRightHandSide(annotatedRightHandSide);
			}
			else if(isToken(child, PythonParser.TYPE_COMMENT)) {
				rightHandSide = node("assignment", rightHandSide, leaf("comment", (TerminalNode) child));
			}
		}
		Node node = rightHandSide;
		for(int i = targets.size() - 1; i >= 0; i--) {
			node = node("assignment", targets.get(i), operators.get(i), node);
		}
		return node;
	}

	private Node annotatedRightHandSide(PythonParser.Annotated_rhsContext ctx) {
		if(ctx.yield_expr() != null) {
			return expression(ctx.yield_expr());
		}
		return expressions(ctx.star_expressions());
	}

	private Node importStatement(PythonParser.Import_stmtContext ctx) {
		if(ctx.import_name() != null) {
			PythonParser.Import_nameContext importName = ctx.import_name();
			Node node = node("import_statement", token(importName.IMPORT()));
			for(ParseTree child : children(importName.dotted_as_names())) {
				if(child instanceof PythonParser.Dotted_as_nameContext dottedAsName) {
					Node dottedName = dottedName(dottedAsName.dotted_name());
					node.add(dottedAsName.AS() != null ? node("aliased_import", dottedName, token(dottedAsName.AS()), identifier(dottedAsName.name())) : dottedName);
				}
				else {
					node.add(token((TerminalNode) child));
				}
			}
			return node;
		}
		PythonParser.Import_fromContext importFrom = ctx.import_from();
		boolean future = importFrom.dotted_name() != null && importFrom.dotted_name().getText().equals("__future__") && importFrom.getChild(1) instanceof PythonParser.Dotted_nameContext;
		Node node = new Node(future ? "future_import_statement" : "import_from_statement");
		Node relativeImport = null;
		for(ParseTree child : children(importFrom)) {
			if(child instanceof PythonParser.Dotted_nameContext dottedName) {
				if(future) {
					node.add(tokenNode("__future__", firstTerminal(dottedName).getSymbol()));
				}
				else if(relativeImport != null) {
					relativeImport.add(dottedName(dottedName));
				}
				else {
					node.add(dottedName(dottedName));
				}
			}
			else if(child instanceof PythonParser.Import_from_targetsContext targets) {
				for(ParseTree targetChild : children(targets)) {
					if(isToken(targetChild, PythonParser.STAR)) {
						node.add(node("wildcard_import", ignoredToken((TerminalNode) targetChild)));
					}
					else if(targetChild instanceof PythonParser.Import_from_as_namesContext names) {
						for(ParseTree nameChild : children(names)) {
							if(nameChild instanceof PythonParser.Import_from_as_nameContext name) {
								Node dottedName = node("dotted_name", identifier(name.name(0)));
								node.add(name.AS() != null ? node("aliased_import", dottedName, token(name.AS()), identifier(name.name(1))) : dottedName);
							}
							else {
								node.add(token((TerminalNode) nameChild));
							}
						}
					}
					else {
						node.add(token((TerminalNode) targetChild));
					}
				}
			}
			else if(isToken(child, PythonParser.DOT) || isToken(child, PythonParser.ELLIPSIS)) {
				if(relativeImport == null) {
					relativeImport = node("relative_import", new Node("import_prefix"));
					node.add(relativeImport);
				}
				relativeImport.children.get(0).add(ignoredToken((TerminalNode) child));
			}
			else if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
		}
		return node;
	}

	private Node dottedName(PythonParser.Dotted_nameContext ctx) {
		Node node = new Node("dotted_name");
		dottedName(ctx, node);
		return node;
	}

	private void dottedName(PythonParser.Dotted_nameContext ctx, Node node) {
		for(ParseTree child : children(ctx)) {
			if(child instanceof PythonParser.Dotted_nameContext dottedName) {
				dottedName(dottedName, node);
			}
			else if(child instanceof PythonParser.NameContext name) {
				node.add(identifier(name));
			}
			else {
				node.add(token((TerminalNode) child));
			}
		}
	}

	private Node compoundStatement(PythonParser.Compound_stmtContext ctx) {
		ParseTree child = ctx.getChild(0);
		if(child instanceof PythonParser.Function_defContext functionDefinition) {
			return decorated(functionDefinition.decorators(), functionDefinition(functionDefinition.function_def_raw()));
		}
		if(child instanceof PythonParser.Class_defContext classDefinition) {
			return decorated(classDefinition.decorators(), classDefinition(classDefinition.class_def_raw()));
		}
		if(child instanceof PythonParser.If_stmtContext ifStatement) {
			Node node = new Node("if_statement");
			conditional(ifStatement, node);
			return node;
		}
		if(child instanceof PythonParser.While_stmtContext whileStatement) {
			return clause("while_statement", whileStatement);
		}
		if(child instanceof PythonParser.For_stmtContext forStatement) {
			Node node = new Node("for_statement");
			for(ParseTree forChild : children(forStatement)) {
				if(forChild instanceof TerminalNode terminal) {
					node.add(terminal.getSymbol().getType() == PythonParser.TYPE_COMMENT ? leaf("comment", terminal) : token(terminal));
				}
				else if(forChild instanceof PythonParser.Star_targetsContext targets) {
					node.add(targets(targets));
				}
				else if(forChild instanceof PythonParser.Star_expressionsContext expressions) {
					node.add(expressions(expressions));
				}
				else if(forChild instanceof PythonParser.BlockContext block) {
					node.add(block(block, node));
				}
				else if(forChild instanceof PythonParser.Else_blockContext elseBlock) {
					node.add(clause("else_clause", elseBlock));
				}
			}
			return node;
		}
		if(child instanceof PythonParser.With_stmtContext withStatement) {
			//the with items and their parentheses are in the with_clause
			Node node = new Node("with_statement");
			Node withClause = new Node("with_clause");
			for(ParseTree withChild : children(withStatement)) {
				if(withChild instanceof PythonParser.With_itemContext withItem) {
					withClause.add(withItem(withItem));
				}
				else if(withChild instanceof PythonParser.BlockContext block) {
					node.add(block(block, node));
				}
				else if(isToken(withChild, PythonParser.TYPE_COMMENT)) {
					node.add(leaf("comment", (TerminalNode) withChild));
				}
				else if(isToken(withChild, PythonParser.LPAR) || isToken(withChild, PythonParser.RPAR) || isToken(withChild, PythonParser.COMMA)) {
					withClause.add(token((TerminalNode) withChild));
				}
				else {
					node.add(token((TerminalNode) withChild));
				}
				if(withClause.parent == null && !withClause.children.isEmpty()) {
					node.add(withClause);
				}
			}
			return node;
		}
		if(child instanceof PythonParser.Try_stmtContext tryStatement) {
			Node node = new Node("try_statement");
			for(ParseTree tryChild : children(tryStatement)) {
				if(tryChild instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else if(tryChild instanceof PythonParser.BlockContext block) {
					node.add(block(block, node));
				}
				else if(tryChild instanceof PythonParser.Except_blockContext exceptBlock) {
					node.add(exceptClause("except_clause", exceptBlock));
				}
				else if(tryChild instanceof PythonParser.Except_star_blockContext exceptStarBlock) {
					node.add(exceptClause("except_clause", exceptStarBlock));
				}
				else if(tryChild instanceof PythonParser.Else_blockContext elseBlock) {
					node.add(clause("else_clause", elseBlock));
				}
				else if(tryChild instanceof PythonParser.Finally_blockContext finallyBlock) {
					node.add(clause("finally_clause", finallyBlock));
				}
			}
			return node;
		}
		if(child instanceof PythonParser.Match_stmtContext matchStatement) {
			return matchStatement(matchStatement);
		}
		return error(ctx);
	}

	private Node decorated(PythonParser.DecoratorsContext decorators, Node definition) {
		if(decorators == null) {
			return definition;
		}
		Node node = new Node("decorated_definition");
		Node decorator = null;
		for(ParseTree child : children(decorators)) {
			if(isToken(child, PythonParser.AT)) {
				decorator = node("decorator", token((TerminalNode) child));
				node.add(decorator);
			}
			else if(child instanceof PythonParser.Named_expressionContext expression) {
				decorator.add(expression(expression));
			}
			else if(isToken(child, PythonParser.NEWLINE)) {
				newline(((TerminalNode) child).getSymbol().getTokenIndex(), decorator);
			}
		}
		node.add(definition);
		return node;
	}

	private Node functionDefinition(PythonParser.Function_def_rawContext ctx) {
		Node node = new Node("function_definition");
		Node parameters = null;
		for(ParseTree child : children(ctx)) {
			if(isToken(child, PythonParser.LPAR)) {
				parameters = node("parameters", token((TerminalNode) child));
				node.add(parameters);
			}
			else if(isToken(child, PythonParser.RPAR)) {
				parameters.add(token((TerminalNode) child));
			}
			else if(child instanceof PythonParser.ParamsContext params) {
				parameters(params.parameters(), parameters);
			}
			else if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
			else if(child instanceof PythonParser.NameContext name) {
				node.add(identifier(name));
			}
			else if(child instanceof PythonParser.Type_paramsContext typeParams) {
				node.add(typeParameters(typeParams));
			}
			else if(child instanceof PythonParser.ExpressionContext expression) {
				node.add(type(expression));
			}
			else if(child instanceof PythonParser.Func_type_commentContext typeComment) {
				for(ParseTree commentChild : children(typeComment)) {
					if(isToken(commentChild, PythonParser.TYPE_COMMENT)) {
						node.add(leaf("comment", (TerminalNode) commentChild));
					}
					else {
						register((TerminalNode) commentChild, node);
					}
				}
			}
			else if(child instanceof PythonParser.BlockContext block) {
				node.add(block(block, node));
			}
		}
		return node;
	}

	private Node classDefinition(PythonParser.Class_def_rawContext ctx) {
		Node node = new Node("class_definition");
		Node argumentList = null;
		for(ParseTree child : children(ctx)) {
			if(isToken(child, PythonParser.LPAR)) {
				argumentList = node("argument_list", token((TerminalNode) child));
				node.add(argumentList);
			}
			else if(isToken(child, PythonParser.RPAR)) {
				argumentList.add(token((TerminalNode) child));
			}
			else if(child instanceof PythonParser.ArgumentsContext arguments) {
				argumentList.addAll(arguments(arguments));
			}
			else if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
			else if(child instanceof PythonParser.NameContext name) {
				node.add(identifier(name));
			}
			else if(child instanceof PythonParser.Type_paramsContext typeParams) {
				node.add(typeParameters(typeParams));
			}
			else if(child instanceof PythonParser.BlockContext block) {
				node.add(block(block, node));
			}
		}
		return node;
	}

	//if_stmt and elif_stmt, whose elif and else clauses are nested in the clause before them, like in the grammar and in the model,
	//where an elif is an if nested in the else of the previous clause, i.e., if_statement(..., elif_clause(..., elif_clause(...), else_clause(...)))
	private void conditional(ParserRuleContext ctx, Node clause) {
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				clause.add(token(terminal));
			}
			else if(child instanceof PythonParser.Named_expressionContext condition) {
				clause.add(expression(condition));
			}
			else if(child instanceof PythonParser.BlockContext block) {
				clause.add(block(block, clause));
			}
			else if(child instanceof PythonParser.Elif_stmtContext elifStatement) {
				Node elifClause = new Node("elif_clause");
				clause.add(elifClause);
				conditional(elifStatement, elifClause);
			}
			else if(child instanceof PythonParser.Else_blockContext elseBlock) {
				clause.add(clause("else_clause", elseBlock));
			}
		}
	}

	//while_stmt, else_block and finally_block
	private Node clause(String type, ParserRuleContext ctx) {
		Node node = new Node(type);
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
			else if(child instanceof PythonParser.BlockContext block) {
				node.add(block(block, node));
			}
			else if(child instanceof PythonParser.Else_blockContext elseBlock) {
				node.add(clause("else_clause", elseBlock));
			}
			else {
				node.add(expression((ParserRuleContext) child));
			}
		}
		return node;
	}

	private Node exceptClause(String type, ParserRuleContext ctx) {
		Node node = new Node(type);
		Node value = null;
		for(ParseTree child : children(ctx)) {
			if(isToken(child, PythonParser.AS)) {
				//except E as e is an as_pattern
				Node asPattern = node("as_pattern", value, token((TerminalNode) child));
				node.children.remove(value);
				node.add(asPattern);
				value = asPattern;
			}
			else if(child instanceof PythonParser.NameContext name) {
				value.add(node("as_pattern_target", identifier(name)));
			}
			else if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
			else if(child instanceof PythonParser.BlockContext block) {
				node.add(block(block, node));
			}
			else if(child instanceof PythonParser.ExpressionsContext expressions) {
				node.addAll(elements(expressions));
			}
			else {
				value = expression((ParserRuleContext) child);
				node.add(value);
			}
		}
		return node;
	}

	private Node withItem(PythonParser.With_itemContext ctx) {
		Node value = expression(ctx.expression());
		if(ctx.AS() != null) {
			value = node("as_pattern", value, token(ctx.AS()), node("as_pattern_target", targetExpression(ctx.star_target())));
		}
		return node("with_item", value);
	}

	private Node block(PythonParser.BlockContext ctx, Node owner) {
		Node block = new Node("block");
		if(ctx.simple_stmts() != null) {
			simpleStatements(ctx.simple_stmts(), block);
			return block;
		}
		for(ParseTree child : children(ctx)) {
			if(child instanceof PythonParser.StatementsContext statements) {
				statements(statements, block);
			}
			else if(isToken(child, PythonParser.NEWLINE) || isToken(child, PythonParser.INDENT)) {
				//the newline and the indent before the block are in the compound statement
				register((TerminalNode) child, owner);
			}
		}
		return block;
	}

	private Node matchStatement(PythonParser.Match_stmtContext ctx) {
		Node node = new Node("match_statement");
		Node block = new Node("block");
		for(ParseTree child : children(ctx)) {
			if(child instanceof PythonParser.Subject_exprContext subject) {
				for(ParseTree subjectChild : children(subject)) {
					if(subjectChild instanceof TerminalNode terminal) {
						node.add(token(terminal));
					}
					else if(subjectChild instanceof PythonParser.Star_named_expressionsContext expressions) {
						node.addAll(elements(expressions));
					}
					else {
						node.add(expression((ParserRuleContext) subjectChild));
					}
				}
			}
			else if(child instanceof PythonParser.Case_blockContext caseBlock) {
				if(block.parent == null) {
					node.add(block);
				}
				block.add(caseClause(caseBlock));
			}
			else if(isToken(child, PythonParser.NEWLINE) || isToken(child, PythonParser.INDENT)) {
				//the indent is in the block of the match statement
				register((TerminalNode) child, block);
			}
			else if(isToken(child, PythonParser.COLON)) {
				node.add(token((TerminalNode) child));
				//the block starts with the indent at the end of the line
				Node indent = new Node("indent");
				indent.start = indent.end = lineEnd(((TerminalNode) child).getSymbol().getStopIndex() + 1);
				indent.ignored = true;
				block.add(indent);
				newlines.add(indent);
			}
			else if(child instanceof TerminalNode terminal && terminal.getSymbol().getType() != PythonParser.DEDENT) {
				node.add(token(terminal));
			}
		}
		return node;
	}

	private Node caseClause(PythonParser.Case_blockContext ctx) {
		Node node = new Node("case_clause");
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
			else if(child instanceof PythonParser.PatternsContext patterns) {
				if(patterns.open_sequence_pattern() != null) {
					for(Node pattern : sequencePatterns(patterns.open_sequence_pattern())) {
						node.add(pattern.ignored ? pattern : node("case_pattern", pattern));
					}
				}
				else {
					node.add(casePattern(patterns.pattern()));
				}
			}
			else if(child instanceof PythonParser.GuardContext guard) {
				node.add(node("if_clause", token(guard.IF()), expression(guard.named_expression())));
			}
			else if(child instanceof PythonParser.BlockContext block) {
				node.add(block(block, node));
			}
		}
		return node;
	}

	//patterns of the match statement, as tree-sitter case_pattern nodes

	private Node casePattern(ParserRuleContext ctx) {
		return node("case_pattern", pattern(ctx));
	}

	private Node pattern(ParserRuleContext ctx) {
		if(ctx instanceof PythonParser.Capture_patternContext capture) {
			return node("dotted_name", identifier(capture));
		}
		if(ctx instanceof PythonParser.PatternContext || ctx instanceof PythonParser.Closed_patternContext || ctx instanceof PythonParser.Maybe_star_patternContext
				|| ctx instanceof PythonParser.Value_patternContext) {
			return pattern((ParserRuleContext) ctx.getChild(0));
		}
		if(ctx instanceof PythonParser.As_patternContext asPattern) {
			return node("as_pattern", casePattern(asPattern.or_pattern()), token(asPattern.AS()), identifier(asPattern.pattern_capture_target()));
		}
		if(ctx instanceof PythonParser.Or_patternContext orPattern) {
			if(orPattern.closed_pattern().size() == 1) {
				return pattern(orPattern.closed_pattern(0));
			}
			Node node = new Node("union_pattern");
			for(ParseTree child : children(orPattern)) {
				node.add(child instanceof TerminalNode terminal ? token(terminal) : pattern((ParserRuleContext) child));
			}
			return node;
		}
		if(ctx instanceof PythonParser.Pattern_capture_targetContext) {
			return identifier(ctx);
		}
		if(ctx instanceof PythonParser.Wildcard_patternContext wildcard) {
			return tokenNode("_", ((TerminalNode) wildcard.getChild(0)).getSymbol());
		}
		if(ctx instanceof PythonParser.AttrContext || ctx instanceof PythonParser.Name_or_attrContext) {
			Node node = new Node("dotted_name");
			for(ParseTree child : children(ctx)) {
				node.add(child instanceof TerminalNode terminal ? token(terminal) : identifier(child));
			}
			return node;
		}
		if(ctx instanceof PythonParser.Literal_patternContext || ctx instanceof PythonParser.Literal_exprContext) {
			ParseTree child = ctx.getChild(0);
			if(child instanceof PythonParser.StringsContext strings) {
				return strings(strings);
			}
			if(child instanceof TerminalNode terminal) {
				return literal(terminal);
			}
			return pattern((ParserRuleContext) child);
		}
		if(ctx instanceof PythonParser.Signed_numberContext signedNumber) {
			Node number = number(signedNumber.NUMBER());
			return signedNumber.MINUS() != null ? node(INLINE, token(signedNumber.MINUS()), number) : number;
		}
		if(ctx instanceof PythonParser.Complex_numberContext complexNumber) {
			Node node = new Node("complex_pattern");
			for(ParseTree child : children(complexNumber)) {
				node.add(child instanceof TerminalNode terminal ? token(terminal) : pattern((ParserRuleContext) child));
			}
			return node;
		}
		if(ctx instanceof PythonParser.Signed_real_numberContext signedRealNumber) {
			Node number = number(signedRealNumber.real_number().NUMBER());
			return signedRealNumber.MINUS() != null ? node(INLINE, token(signedRealNumber.MINUS()), number) : number;
		}
		if(ctx instanceof PythonParser.Imaginary_numberContext imaginaryNumber) {
			return number(imaginaryNumber.NUMBER());
		}
		if(ctx instanceof PythonParser.Group_patternContext group) {
			return node("tuple_pattern", token(group.LPAR()), casePattern(group.pattern()), token(group.RPAR()));
		}
		if(ctx instanceof PythonParser.Sequence_patternContext sequence) {
			Node node = new Node(sequence.LSQB() != null ? "list_pattern" : "tuple_pattern");
			for(ParseTree child : children(sequence)) {
				if(child instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else {
					for(Node pattern : sequencePatterns((ParserRuleContext) child)) {
						node.add(pattern.ignored ? pattern : node("case_pattern", pattern));
					}
				}
			}
			return node;
		}
		if(ctx instanceof PythonParser.Star_patternContext starPattern) {
			TerminalNode name = firstTerminal(starPattern.name());
			return node("splat_pattern", token(starPattern.STAR()), name.getText().equals("_") ? tokenNode("_", name.getSymbol()) : identifier(name));
		}
		if(ctx instanceof PythonParser.Mapping_patternContext mapping) {
			Node node = new Node("dict_pattern");
			mappingPattern(mapping, node);
			return node;
		}
		if(ctx instanceof PythonParser.Class_patternContext classPattern) {
			Node node = node("class_pattern", pattern(classPattern.name_or_attr()));
			for(int i = 1; i < classPattern.getChildCount(); i++) {
				ParseTree child = classPattern.getChild(i);
				if(child instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else {
					for(ParseTree patternChild : children((ParserRuleContext) child)) {
						if(patternChild instanceof TerminalNode terminal) {
							node.add(token(terminal));
						}
						else if(patternChild instanceof PythonParser.Keyword_patternContext keywordPattern) {
							node.add(node("case_pattern", node("keyword_pattern", identifier(keywordPattern.name()), token(keywordPattern.EQUAL()), pattern(keywordPattern.pattern()))));
						}
						else {
							node.add(casePattern((ParserRuleContext) patternChild));
						}
					}
				}
			}
			return node;
		}
		return error(ctx);
	}

	private List<Node> sequencePatterns(ParserRuleContext ctx) {
		List<Node> patterns = new ArrayList<>();
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				patterns.add(token(terminal));
			}
			else if(child instanceof PythonParser.Maybe_sequence_patternContext || child instanceof PythonParser.Open_sequence_patternContext) {
				patterns.addAll(sequencePatterns((ParserRuleContext) child));
			}
			else {
				patterns.add(pattern((ParserRuleContext) child));
			}
		}
		return patterns;
	}

	private void mappingPattern(ParserRuleContext ctx, Node node) {
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
			else if(child instanceof PythonParser.Items_patternContext items) {
				mappingPattern(items, node);
			}
			else if(child instanceof PythonParser.Key_value_patternContext keyValue) {
				//the keys are not case patterns
				node.add(pattern((ParserRuleContext) keyValue.getChild(0)));
				node.add(token(keyValue.COLON()));
				node.add(casePattern(keyValue.pattern()));
			}
			else if(child instanceof PythonParser.Double_star_patternContext doubleStar) {
				node.add(node("splat_pattern", token(doubleStar.DOUBLESTAR()), identifier(doubleStar.pattern_capture_target())));
			}
		}
	}

	//parameters

	private void parameters(ParserRuleContext ctx, Node parameters) {
		parameterList(ctx, parameters);
		//attach the splats to their parameters
		List<Node> children = new ArrayList<>(parameters.children);
		parameters.children.clear();
		for(int i = 0; i < children.size(); i++) {
			Node child = children.get(i);
			if((child.type.equals("*") || child.type.equals("**")) && i + 1 < children.size()) {
				Node parameter = children.get(++i);
				String splatType = child.type.equals("*") ? "list_splat_pattern" : "dictionary_splat_pattern";
				if(parameter.type.equals("typed_parameter")) {
					Node name = parameter.children.remove(0);
					Node splat = node(splatType, child, name);
					splat.parent = parameter;
					parameter.children.add(0, splat);
					parameters.add(parameter);
				}
				else {
					parameters.add(node(splatType, child, parameter));
				}
			}
			else {
				parameters.add(child);
			}
		}
	}

	private void parameterList(ParserRuleContext ctx, Node parameters) {
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				int type = terminal.getSymbol().getType();
				if(type == PythonParser.SLASH) {
					parameters.add(node("positional_separator", token(terminal)));
				}
				else if(type == PythonParser.STAR && ctx.getChild(ctx.children.indexOf(child) + 1) instanceof TerminalNode) {
					//* followed by a comma is a keyword separator
					parameters.add(node("keyword_separator", token(terminal)));
				}
				else if(type == PythonParser.STAR || type == PythonParser.DOUBLESTAR) {
					//the splat of the next parameter
					parameters.add(token(terminal));
				}
				else if(type == PythonParser.TYPE_COMMENT) {
					parameters.add(leaf("comment", terminal));
				}
				else {
					parameters.add(token(terminal));
				}
			}
			else if(child instanceof PythonParser.ParamContext || child instanceof PythonParser.Lambda_paramContext || child instanceof PythonParser.Param_star_annotationContext) {
				parameters.add(parameter((ParserRuleContext) child));
			}
			else if(child instanceof PythonParser.Default_assignmentContext defaultAssignment) {
				//param default_assignment
				Node parameter = parameters.children.remove(parameters.children.size() - 1);
				parameters.add(defaultParameter(parameter, defaultAssignment));
			}
			else {
				parameterList((ParserRuleContext) child, parameters);
			}
		}
	}

	private Node parameter(ParserRuleContext ctx) {
		Node name = identifier(ctx.getChild(0));
		if(ctx.getChildCount() > 1) {
			//param: name annotation, or param_star_annotation: name star_annotation
			ParserRuleContext annotation = (ParserRuleContext) ctx.getChild(1);
			return node("typed_parameter", name, ignoredToken((TerminalNode) annotation.getChild(0)), type((ParserRuleContext) annotation.getChild(1)));
		}
		return name;
	}

	private Node defaultParameter(Node parameter, PythonParser.Default_assignmentContext defaultAssignment) {
		if(parameter.type.equals("typed_parameter")) {
			Node node = new Node("typed_default_parameter");
			node.addAll(new ArrayList<>(parameter.children));
			node.add(token(defaultAssignment.EQUAL()));
			node.add(expression(defaultAssignment.expression()));
			return node;
		}
		return node("default_parameter", parameter, ignoredToken(defaultAssignment.EQUAL()), expression(defaultAssignment.expression()));
	}

	//T is type(identifier), T: int is type(constrained_type), *Ts and **P are type(splat_type), and the defaults follow the type
	private Node typeParameters(PythonParser.Type_paramsContext ctx) {
		Node node = node("type_parameter", token(ctx.LSQB()));
		for(ParseTree child : children(ctx.type_param_seq())) {
			if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
				continue;
			}
			PythonParser.Type_paramContext typeParam = (PythonParser.Type_paramContext) child;
			Node type = new Node("type");
			Node name = identifier(typeParam.name());
			if(typeParam.STAR() != null || typeParam.DOUBLESTAR() != null) {
				type.add(node("splat_type", token(typeParam.STAR() != null ? typeParam.STAR() : typeParam.DOUBLESTAR()), name));
			}
			else if(typeParam.type_param_bound() != null) {
				PythonParser.Type_param_boundContext bound = typeParam.type_param_bound();
				type.add(node("constrained_type", node("type", name), ignoredToken(bound.COLON()), type(bound.expression())));
			}
			else {
				type.add(name);
			}
			ParserRuleContext defaultValue = typeParam.type_param_default() != null ? typeParam.type_param_default() : typeParam.type_param_starred_default();
			if(defaultValue != null) {
				type.add(token((TerminalNode) defaultValue.getChild(0)));
				type.add(type((ParserRuleContext) defaultValue.getChild(1)));
			}
			node.add(type);
		}
		node.add(token(ctx.RSQB()));
		return node;
	}

	//type annotations: list[int] is a generic_type, and a | with a generic_type operand is a union_type

	private Node type(ParserRuleContext ctx) {
		return node("type", typeExpression(expression(ctx)));
	}

	private Node typeExpression(Node expression) {
		if(expression.type.equals("subscript") && expression.children.get(0).type.equals("identifier")) {
			Node typeParameter = new Node("type_parameter");
			Node generic = node("generic_type", expression.children.get(0), typeParameter);
			for(int i = 1; i < expression.children.size(); i++) {
				Node child = expression.children.get(i);
				typeParameter.add(child.ignored || child.leaf && !isNamedLeaf(child) ? child : node("type", typeExpression(child)));
			}
			return generic;
		}
		if(expression.type.equals("binary_operator") && expression.children.get(1).type.equals("|")) {
			Node left = typeExpression(expression.children.get(0));
			Node right = typeExpression(expression.children.get(2));
			if(isTypeOnly(left) || isTypeOnly(right)) {
				return node("union_type", node("type", left), expression.children.get(1), node("type", right));
			}
		}
		return expression;
	}

	private static boolean isTypeOnly(Node node) {
		return node.type.equals("generic_type") || node.type.equals("union_type");
	}

	private static boolean isNamedLeaf(Node node) {
		return switch(node.type) {
			case "identifier", "integer", "float", "string", "true", "false", "none", "ellipsis" -> true;
			default -> false;
		};
	}

	//expressions

	private Node expressions(ParserRuleContext ctx) {
		List<Node> elements = elements(ctx);
		if(elements.size() == 1) {
			return elements.get(0);
		}
		return new Node("expression_list").addAll(elements);
	}

	//the elements and the commas of expressions, star_expressions and star_named_expressions
	private List<Node> elements(ParserRuleContext ctx) {
		List<Node> elements = new ArrayList<>();
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				elements.add(token(terminal));
			}
			else if(child instanceof PythonParser.Star_named_expressionsContext expressions) {
				elements.addAll(elements(expressions));
			}
			else {
				elements.add(expression((ParserRuleContext) child));
			}
		}
		return elements;
	}

	private Node expression(ParserRuleContext ctx) {
		if(hasError(ctx)) {
			return error(ctx);
		}
		List<ParseTree> children = children(ctx);
		switch(ctx.getRuleIndex()) {
		case PythonParser.RULE_expression:
			if(children.size() == 1) {
				return expression((ParserRuleContext) children.get(0));
			}
			return node("conditional_expression", expression(ctx.getRuleContext(PythonParser.DisjunctionContext.class, 0)), token((TerminalNode) children.get(1)),
					expression(ctx.getRuleContext(PythonParser.DisjunctionContext.class, 1)), token((TerminalNode) children.get(3)), expression((ParserRuleContext) children.get(4)));
		case PythonParser.RULE_disjunction:
		case PythonParser.RULE_conjunction: {
			Node node = expression((ParserRuleContext) children.get(0));
			for(int i = 1; i < children.size(); i += 2) {
				node = node("boolean_operator", node, token((TerminalNode) children.get(i)), expression((ParserRuleContext) children.get(i + 1)));
			}
			return node;
		}
		case PythonParser.RULE_inversion:
			if(children.size() == 2) {
				return node("not_operator", token((TerminalNode) children.get(0)), expression((ParserRuleContext) children.get(1)));
			}
			return expression((ParserRuleContext) children.get(0));
		case PythonParser.RULE_comparison: {
			if(children.size() == 1) {
				return expression((ParserRuleContext) children.get(0));
			}
			Node node = node("comparison_operator", expression((ParserRuleContext) children.get(0)));
			for(int i = 1; i < children.size(); i++) {
				ParserRuleContext pair = (ParserRuleContext) children.get(i).getChild(0);
				List<ParseTree> pairChildren = children(pair);
				if(pairChildren.size() == 3) {
					//not in, is not
					node.add(node(pairChildren.get(0).getText() + " " + pairChildren.get(1).getText(), token((TerminalNode) pairChildren.get(0)), token((TerminalNode) pairChildren.get(1))));
				}
				else {
					node.add(token((TerminalNode) pairChildren.get(0)));
				}
				node.add(expression((ParserRuleContext) pairChildren.get(pairChildren.size() - 1)));
			}
			return node;
		}
		case PythonParser.RULE_bitwise_or:
		case PythonParser.RULE_bitwise_xor:
		case PythonParser.RULE_bitwise_and:
		case PythonParser.RULE_shift_expr:
		case PythonParser.RULE_sum:
		case PythonParser.RULE_term:
			if(children.size() == 1) {
				return expression((ParserRuleContext) children.get(0));
			}
			return node("binary_operator", expression((ParserRuleContext) children.get(0)), token((TerminalNode) children.get(1)), expression((ParserRuleContext) children.get(2)));
		case PythonParser.RULE_factor:
			if(children.size() == 2) {
				return node("unary_operator", token((TerminalNode) children.get(0)), expression((ParserRuleContext) children.get(1)));
			}
			return expression((ParserRuleContext) children.get(0));
		case PythonParser.RULE_power:
			if(children.size() == 3) {
				return node("binary_operator", expression((ParserRuleContext) children.get(0)), token((TerminalNode) children.get(1)), expression((ParserRuleContext) children.get(2)));
			}
			return expression((ParserRuleContext) children.get(0));
		case PythonParser.RULE_await_primary:
			if(children.size() == 2) {
				return node("await", token((TerminalNode) children.get(0)), expression((ParserRuleContext) children.get(1)));
			}
			return expression((ParserRuleContext) children.get(0));
		case PythonParser.RULE_primary:
		case PythonParser.RULE_t_primary:
			return primary(ctx);
		case PythonParser.RULE_atom:
			return atom(ctx);
		case PythonParser.RULE_name:
		case PythonParser.RULE_name_except_underscore:
			return identifier(ctx);
		case PythonParser.RULE_strings:
			return strings(ctx);
		case PythonParser.RULE_group:
			return node("parenthesized_expression", token((TerminalNode) children.get(0)), expression((ParserRuleContext) children.get(1)), token((TerminalNode) children.get(2)));
		case PythonParser.RULE_tuple:
			return collection("tuple", ctx);
		case PythonParser.RULE_list:
			return collection("list", ctx);
		case PythonParser.RULE_set:
			return collection("set", ctx);
		case PythonParser.RULE_dict:
			return collection("dictionary", ctx);
		case PythonParser.RULE_double_starred_kvpair:
			if(children.size() == 2) {
				return node("dictionary_splat", token((TerminalNode) children.get(0)), expression((ParserRuleContext) children.get(1)));
			}
			return expression((ParserRuleContext) children.get(0));
		case PythonParser.RULE_kvpair:
			return node("pair", expression((ParserRuleContext) children.get(0)), token((TerminalNode) children.get(1)), expression((ParserRuleContext) children.get(2)));
		case PythonParser.RULE_listcomp:
			return collection("list_comprehension", ctx);
		case PythonParser.RULE_setcomp:
			return collection("set_comprehension", ctx);
		case PythonParser.RULE_genexp:
			return collection("generator_expression", ctx);
		case PythonParser.RULE_dictcomp:
			return collection("dictionary_comprehension", ctx);
		case PythonParser.RULE_named_expression:
		case PythonParser.RULE_annotated_rhs:
			return expression((ParserRuleContext) children.get(0));
		case PythonParser.RULE_assignment_expression:
			return node("named_expression", identifier(children.get(0)), token((TerminalNode) children.get(1)), expression((ParserRuleContext) children.get(2)));
		case PythonParser.RULE_star_named_expression:
		case PythonParser.RULE_star_expression:
		case PythonParser.RULE_starred_expression:
			if(children.size() == 2) {
				return node("list_splat", token((TerminalNode) children.get(0)), expression((ParserRuleContext) children.get(1)));
			}
			return expression((ParserRuleContext) children.get(0));
		case PythonParser.RULE_star_expressions:
		case PythonParser.RULE_expressions:
		case PythonParser.RULE_star_named_expressions:
			return expressions(ctx);
		case PythonParser.RULE_lambdef: {
			Node node = new Node("lambda");
			for(ParseTree child : children) {
				if(child instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else if(child instanceof PythonParser.Lambda_paramsContext params) {
					Node parameters = new Node("lambda_parameters");
					parameters(params.lambda_parameters(), parameters);
					node.add(parameters);
				}
				else {
					node.add(expression((ParserRuleContext) child));
				}
			}
			return node;
		}
		case PythonParser.RULE_yield_expr: {
			Node node = new Node("yield");
			for(ParseTree child : children) {
				node.add(child instanceof TerminalNode terminal ? token(terminal) : expression((ParserRuleContext) child));
			}
			return node;
		}
		case PythonParser.RULE_slice: {
			if(children.size() == 1 && !(children.get(0) instanceof TerminalNode)) {
				return expression((ParserRuleContext) children.get(0));
			}
			Node node = new Node("slice");
			for(ParseTree child : children) {
				node.add(child instanceof TerminalNode terminal ? token(terminal) : expression((ParserRuleContext) child));
			}
			return node;
		}
		case PythonParser.RULE_fstring:
		case PythonParser.RULE_tstring:
		case PythonParser.RULE_string:
			return string(ctx);
		default:
			if(children.size() == 1 && children.get(0) instanceof ParserRuleContext child) {
				return expression(child);
			}
			return error(ctx);
		}
	}

	private Node primary(ParserRuleContext ctx) {
		List<ParseTree> children = children(ctx);
		if(children.size() == 1) {
			return expression((ParserRuleContext) children.get(0));
		}
		Node value = expression((ParserRuleContext) children.get(0));
		ParseTree second = children.get(1);
		if(isToken(second, PythonParser.DOT)) {
			return node("attribute", value, token((TerminalNode) second), identifier(children.get(2)));
		}
		if(second instanceof PythonParser.GenexpContext genexp) {
			return node("call", value, expression(genexp));
		}
		if(isToken(second, PythonParser.LPAR)) {
			Node argumentList = node("argument_list", token((TerminalNode) second));
			for(int i = 2; i < children.size(); i++) {
				ParseTree child = children.get(i);
				if(child instanceof PythonParser.ArgumentsContext arguments) {
					argumentList.addAll(arguments(arguments));
				}
				else {
					argumentList.add(token((TerminalNode) child));
				}
			}
			return node("call", value, argumentList);
		}
		//subscript
		Node subscript = node("subscript", value, token((TerminalNode) second));
		for(int i = 2; i < children.size(); i++) {
			ParseTree child = children.get(i);
			if(child instanceof PythonParser.SlicesContext slices) {
				for(ParseTree sliceChild : children(slices)) {
					subscript.add(sliceChild instanceof TerminalNode terminal ? token(terminal) : expression((ParserRuleContext) sliceChild));
				}
			}
			else {
				subscript.add(token((TerminalNode) child));
			}
		}
		return subscript;
	}

	private List<Node> arguments(PythonParser.ArgumentsContext ctx) {
		List<Node> arguments = new ArrayList<>();
		arguments(ctx, arguments);
		return arguments;
	}

	private void arguments(ParserRuleContext ctx, List<Node> arguments) {
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				arguments.add(token(terminal));
			}
			else if(child instanceof PythonParser.ArgsContext || child instanceof PythonParser.KwargsContext) {
				arguments((ParserRuleContext) child, arguments);
			}
			else if(child instanceof PythonParser.Kwarg_or_starredContext || child instanceof PythonParser.Kwarg_or_double_starredContext) {
				ParserRuleContext argument = (ParserRuleContext) child;
				if(argument.getChild(0) instanceof PythonParser.NameContext name) {
					//the name of a keyword argument is not a variable, so it is not matched with the variables, i.e., the value in ids_rank=ids_rank
					arguments.add(node("keyword_argument", leaf("keyword_identifier", firstTerminal(name)), token((TerminalNode) argument.getChild(1)), expression((ParserRuleContext) argument.getChild(2))));
				}
				else if(argument.getChildCount() == 2) {
					arguments.add(node("dictionary_splat", token((TerminalNode) argument.getChild(0)), expression((ParserRuleContext) argument.getChild(1))));
				}
				else {
					arguments.add(expression((ParserRuleContext) argument.getChild(0)));
				}
			}
			else {
				arguments.add(expression((ParserRuleContext) child));
			}
		}
	}

	private Node atom(ParserRuleContext ctx) {
		ParseTree child = ctx.getChild(0);
		if(child instanceof TerminalNode terminal) {
			return literal(terminal);
		}
		return expression((ParserRuleContext) child);
	}

	private Node literal(TerminalNode terminal) {
		switch(terminal.getSymbol().getType()) {
		case PythonParser.TRUE:
			return leaf("true", terminal);
		case PythonParser.FALSE:
			return leaf("false", terminal);
		case PythonParser.NONE:
			return leaf("none", terminal);
		case PythonParser.ELLIPSIS:
			return leaf("ellipsis", terminal);
		case PythonParser.NUMBER:
			return number(terminal);
		default:
			return token(terminal);
		}
	}

	private Node number(TerminalNode terminal) {
		String text = terminal.getText().toLowerCase();
		boolean integer = text.startsWith("0x") || text.startsWith("0o") || text.startsWith("0b") || !(text.contains(".") || text.contains("e"));
		return leaf(integer ? "integer" : "float", terminal);
	}

	private Node strings(ParserRuleContext ctx) {
		List<Node> strings = new ArrayList<>();
		for(ParseTree child : children(ctx)) {
			strings.add(string((ParserRuleContext) child));
		}
		if(strings.size() == 1) {
			return strings.get(0);
		}
		return new Node("concatenated_string").addAll(strings);
	}

	//a flattened string, including the interpolations of f-strings
	private Node string(ParserRuleContext ctx) {
		Node node = new Node("string");
		node.leaf = true;
		node.start = ctx.getStart().getStartIndex();
		node.end = ctx.getStop().getStopIndex() + 1;
		for(int i = ctx.getStart().getTokenIndex(); i <= ctx.getStop().getTokenIndex(); i++) {
			tokenOwners[i] = node;
		}
		return node;
	}

	//tuple, list, set, dict and comprehensions, with their brackets
	private Node collection(String type, ParserRuleContext ctx) {
		Node node = new Node(type);
		for(ParseTree child : children(ctx)) {
			if(child instanceof TerminalNode terminal) {
				node.add(token(terminal));
			}
			else if(child instanceof PythonParser.Star_named_expressionsContext || child instanceof PythonParser.Double_starred_kvpairsContext) {
				for(ParseTree element : children((ParserRuleContext) child)) {
					node.add(element instanceof TerminalNode terminal ? token(terminal) : expression((ParserRuleContext) element));
				}
			}
			else if(child instanceof PythonParser.For_if_clausesContext clauses) {
				for(ParseTree clause : children(clauses)) {
					forIfClause((PythonParser.For_if_clauseContext) clause, node);
				}
			}
			else {
				node.add(expression((ParserRuleContext) child));
			}
		}
		return node;
	}

	private void forIfClause(PythonParser.For_if_clauseContext ctx, Node comprehension) {
		Node forInClause = new Node("for_in_clause");
		comprehension.add(forInClause);
		Node ifClause = null;
		for(ParseTree child : children(ctx)) {
			if(isToken(child, PythonParser.IF)) {
				ifClause = node("if_clause", token((TerminalNode) child));
				comprehension.add(ifClause);
			}
			else if(ifClause != null) {
				ifClause.add(expression((ParserRuleContext) child));
			}
			else if(child instanceof TerminalNode terminal) {
				forInClause.add(token(terminal));
			}
			else if(child instanceof PythonParser.Star_targetsContext targets) {
				forInClause.add(targets(targets));
			}
			else {
				forInClause.add(expression((ParserRuleContext) child));
			}
		}
	}

	//assignment targets as tree-sitter patterns

	private Node targets(PythonParser.Star_targetsContext ctx) {
		List<Node> patterns = new ArrayList<>();
		for(ParseTree child : children(ctx)) {
			patterns.add(child instanceof TerminalNode terminal ? token(terminal) : target((ParserRuleContext) child));
		}
		if(patterns.size() == 1) {
			return patterns.get(0);
		}
		return new Node("pattern_list").addAll(patterns);
	}

	private Node target(ParserRuleContext ctx) {
		List<ParseTree> children = children(ctx);
		if(ctx instanceof PythonParser.Star_targetContext) {
			if(children.size() == 2) {
				return node("list_splat_pattern", token((TerminalNode) children.get(0)), target((ParserRuleContext) children.get(1)));
			}
			return target((ParserRuleContext) children.get(0));
		}
		if(ctx instanceof PythonParser.Target_with_star_atomContext || ctx instanceof PythonParser.Single_subscript_attribute_targetContext) {
			if(children.size() == 1) {
				return target((ParserRuleContext) children.get(0));
			}
			return targetPrimary(ctx);
		}
		if(ctx instanceof PythonParser.Star_atomContext) {
			if(children.size() == 1) {
				return identifier(children.get(0));
			}
			Node node = new Node(isToken(children.get(0), PythonParser.LSQB) ? "list_pattern" : "tuple_pattern");
			for(ParseTree child : children) {
				if(child instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else if(child instanceof PythonParser.Target_with_star_atomContext target) {
					node.add(target(target));
				}
				else {
					for(ParseTree element : children((ParserRuleContext) child)) {
						node.add(element instanceof TerminalNode terminal ? token(terminal) : target((ParserRuleContext) element));
					}
				}
			}
			return node;
		}
		if(ctx instanceof PythonParser.NameContext) {
			return identifier(ctx);
		}
		return expression(ctx);
	}

	//t_primary followed by .name or [slices], as attribute or subscript
	private Node targetPrimary(ParserRuleContext ctx) {
		return primary(ctx);
	}

	private Node singleTarget(PythonParser.Single_targetContext ctx) {
		List<ParseTree> children = children(ctx);
		if(children.size() == 3) {
			return node("parenthesized_expression", token((TerminalNode) children.get(0)), singleTarget((PythonParser.Single_targetContext) children.get(1)), token((TerminalNode) children.get(2)));
		}
		if(children.get(0) instanceof PythonParser.NameContext name) {
			return identifier(name);
		}
		return targetPrimary((ParserRuleContext) children.get(0));
	}

	//a star_target as an expression, i.e., the target of with ... as
	private Node targetExpression(PythonParser.Star_targetContext ctx) {
		Node pattern = target(ctx);
		return patternToExpression(pattern);
	}

	private static Node patternToExpression(Node pattern) {
		String type = switch(pattern.type) {
			case "tuple_pattern" -> "tuple";
			case "list_pattern" -> "list";
			case "list_splat_pattern" -> "list_splat";
			default -> null;
		};
		if(type == null) {
			return pattern;
		}
		Node node = new Node(type);
		for(Node child : new ArrayList<>(pattern.children)) {
			node.add(patternToExpression(child));
		}
		return node;
	}

	private Node deleteTargets(PythonParser.Del_targetsContext ctx) {
		List<Node> elements = new ArrayList<>();
		for(ParseTree child : children(ctx)) {
			elements.add(child instanceof TerminalNode terminal ? token(terminal) : deleteTarget((ParserRuleContext) child));
		}
		if(elements.size() == 1) {
			return elements.get(0);
		}
		return new Node("expression_list").addAll(elements);
	}

	private Node deleteTarget(ParserRuleContext ctx) {
		List<ParseTree> children = children(ctx);
		if(ctx instanceof PythonParser.Del_targetContext) {
			if(children.size() == 1) {
				return deleteTarget((ParserRuleContext) children.get(0));
			}
			return primary(ctx);
		}
		if(ctx instanceof PythonParser.Del_t_atomContext) {
			if(children.size() == 1) {
				return identifier(children.get(0));
			}
			Node node;
			if(isToken(children.get(0), PythonParser.LSQB)) {
				node = new Node("list");
			}
			else if(children.size() == 3 && children.get(1) instanceof PythonParser.Del_targetContext) {
				node = new Node("parenthesized_expression");
			}
			else {
				node = new Node("tuple");
			}
			for(ParseTree child : children) {
				if(child instanceof TerminalNode terminal) {
					node.add(token(terminal));
				}
				else if(child instanceof PythonParser.Del_targetsContext targets) {
					for(ParseTree element : children(targets)) {
						node.add(element instanceof TerminalNode terminal ? token(terminal) : deleteTarget((ParserRuleContext) element));
					}
				}
				else {
					node.add(deleteTarget((ParserRuleContext) child));
				}
			}
			return node;
		}
		return expression(ctx);
	}

	//syntax errors

	private static boolean hasSyntaxError(ParseTree tree) {
		if(tree instanceof ErrorNode) {
			return true;
		}
		if(tree instanceof ParserRuleContext ctx) {
			if(ctx.exception != null) {
				return true;
			}
			for(ParseTree child : children(ctx)) {
				if(hasSyntaxError(child)) {
					return true;
				}
			}
		}
		return false;
	}

	//the print names at the start of a statement followed by an expression other than a parenthesized one, i.e., print x, y and print >>f, x
	private static Map<Integer, Integer> python2Prints(String content, List<Token> tokens) {
		Map<Integer, Integer> prints = new HashMap<>();
		Token previous = null;
		for(int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if(token.getChannel() != Token.DEFAULT_CHANNEL) {
				continue;
			}
			boolean statementStart = previous == null || switch(previous.getType()) {
				case PythonParser.NEWLINE, PythonParser.INDENT, PythonParser.DEDENT, PythonParser.SEMI, PythonParser.COLON -> true;
				default -> false;
			};
			if(statementStart && token.getType() == PythonParser.NAME && token.getText().equals("print")) {
				Token next = null;
				for(int j = i + 1; j < tokens.size() && next == null; j++) {
					if(tokens.get(j).getChannel() == Token.DEFAULT_CHANNEL) {
						next = tokens.get(j);
					}
				}
				if(next != null && startsPrintedExpression(next.getType())) {
					int start = token.getStartIndex();
					int chevron = next.getType() == PythonParser.RIGHTSHIFT ? next.getStartIndex() : -1;
					prints.put(start, chevron);
				}
			}
			previous = token;
		}
		return prints;
	}

	private static boolean startsPrintedExpression(int type) {
		return switch(type) {
			case PythonParser.NAME, PythonParser.NAME_OR_TYPE, PythonParser.NAME_OR_MATCH, PythonParser.NAME_OR_CASE, PythonParser.NAME_OR_WILDCARD,
				PythonParser.STRING, PythonParser.NUMBER, PythonParser.FSTRING_START, PythonParser.TSTRING_START, PythonParser.LBRACE,
				PythonParser.NONE, PythonParser.TRUE, PythonParser.FALSE, PythonParser.NOT, PythonParser.LAMBDA, PythonParser.TILDE, PythonParser.RIGHTSHIFT -> true;
			default -> false;
		};
	}

	private boolean hasError(ParserRuleContext ctx) {
		if(ctx.exception != null) {
			return true;
		}
		for(ParseTree child : children(ctx)) {
			if(child instanceof ErrorNode) {
				return true;
			}
		}
		return false;
	}

	//an ERROR node with the tokens of the erroneous code
	private Node error(ParseTree tree) {
		Node node = new Node("ERROR");
		int start;
		int stop;
		if(tree instanceof ParserRuleContext ctx) {
			start = ctx.getStart().getTokenIndex();
			stop = ctx.getStop() != null ? ctx.getStop().getTokenIndex() : start;
		}
		else {
			start = stop = ((TerminalNode) tree).getSymbol().getTokenIndex();
		}
		for(int i = Math.max(0, start); i <= stop && i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if(token.getChannel() != Token.DEFAULT_CHANNEL || token.getType() == Token.EOF) {
				continue;
			}
			switch(token.getType()) {
			case PythonParser.NEWLINE, PythonParser.INDENT, PythonParser.DEDENT -> tokenOwners[i] = node;
			case PythonParser.NAME, PythonParser.NAME_OR_TYPE, PythonParser.NAME_OR_MATCH, PythonParser.NAME_OR_CASE, PythonParser.NAME_OR_WILDCARD -> node.add(tokenNode("identifier", token));
			case PythonParser.STRING -> node.add(tokenNode("string", token));
			case PythonParser.NUMBER -> {
				String text = token.getText().toLowerCase();
				node.add(tokenNode(text.startsWith("0x") || !(text.contains(".") || text.contains("e")) ? "integer" : "float", token));
			}
			default -> {
				String text = token.getText();
				Node tokenNode = tokenNode(ALIASED.getOrDefault(text, text), token);
				tokenNode.ignored = IGNORED.contains(text);
				node.add(tokenNode);
			}
			}
		}
		return node;
	}

	//comments and line continuations

	private record Anchor(int position, Node owner, boolean dedent) {}

	//the comments and line continuations are placed by position, between the anchors before and after them, which are the tokens, the newlines and the dedents
	private void addExtras(Node root) {
		List<Anchor> anchors = new ArrayList<>();
		List<Node> extras = new ArrayList<>();
		for(int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if(token.getType() == PythonLexer.COMMENT || token.getType() == PythonLexer.EXPLICIT_LINE_JOINING) {
				extras.add(tokenNode(token.getType() == PythonLexer.COMMENT ? "comment" : "line_continuation", token));
			}
			else if(token.getType() == PythonParser.DEDENT) {
				anchors.add(new Anchor(token.getStartIndex(), null, true));
			}
			else if(tokenOwners[i] != null) {
				anchors.add(new Anchor(token.getStartIndex(), tokenOwners[i], false));
			}
		}
		for(Node newline : newlines) {
			anchors.add(new Anchor(newline.start, newline.parent, false));
		}
		anchors.sort((a, b) -> a.position != b.position ? Integer.compare(a.position, b.position) : Boolean.compare(b.dedent, a.dedent));
		Map<Node, List<Node>> parentExtras = new IdentityHashMap<>();
		//the innermost block for the comments before the same anchor
		Map<Integer, Integer> dedentRunDepths = new HashMap<>();
		int anchorIndex = 0;
		for(Node extra : extras) {
			while(anchorIndex < anchors.size() && anchors.get(anchorIndex).position < extra.start) {
				anchorIndex++;
			}
			Node previous = null;
			for(int j = anchorIndex - 1; j >= 0 && previous == null; j--) {
				previous = anchors.get(j).owner;
			}
			Node next = null;
			int nextIndex = anchors.size();
			boolean dedent = false;
			for(int j = anchorIndex; j < anchors.size(); j++) {
				Anchor anchor = anchors.get(j);
				if(anchor.dedent) {
					dedent = true;
				}
				else {
					next = anchor.owner;
					nextIndex = j;
					break;
				}
			}
			Node parent;
			if(previous == null) {
				parent = root;
			}
			else {
				Node common = next == null ? root : commonAncestor(previous, next);
				parent = common;
				if(dedent) {
					//tree-sitter emits the dedents after the comments indented like the block
					int indent = indentation(extra.start);
					List<Node> blocks = new ArrayList<>();
					for(Node node = previous; node != null && node != common; node = node.parent) {
						if(node.type.equals("block")) {
							blocks.add(node);
						}
					}
					int minimum = dedentRunDepths.getOrDefault(nextIndex, 0);
					for(int k = minimum; k < blocks.size(); k++) {
						if(blockIndent(blocks.get(k)) <= indent) {
							parent = blocks.get(k);
							dedentRunDepths.put(nextIndex, k);
							break;
						}
						dedentRunDepths.put(nextIndex, k + 1);
					}
				}
			}
			parentExtras.computeIfAbsent(parent, k -> new ArrayList<>()).add(extra);
		}
		for(Map.Entry<Node, List<Node>> entry : parentExtras.entrySet()) {
			Node parent = entry.getKey();
			List<Node> children = new ArrayList<>(parent.children);
			children.addAll(entry.getValue());
			children.sort((a, b) -> a.start != b.start ? Integer.compare(a.start, b.start) : Integer.compare(a.end, b.end));
			parent.children.clear();
			parent.addAll(children);
		}
	}

	private int lineEnd(int offset) {
		int position = offset;
		while(position < content.length() && content.charAt(position) != '\n' && content.charAt(position) != '\r') {
			position++;
		}
		return position;
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

	//the indentation of the first statement or case clause of the block
	private int blockIndent(Node block) {
		return blockIndents.computeIfAbsent(block, b -> {
			for(Node child : b.children) {
				if(!child.ignored) {
					return indentation(child.start);
				}
			}
			return indentation(b.start);
		});
	}

	//the indentation of the line of the offset, with tabs as 8 spaces like the tree-sitter scanner
	private int indentation(int offset) {
		int lineStart = offset;
		while(lineStart > 0 && content.charAt(lineStart - 1) != '\n' && content.charAt(lineStart - 1) != '\r') {
			lineStart--;
		}
		int indent = 0;
		for(int i = lineStart; i < offset; i++) {
			char character = content.charAt(i);
			if(character == '\t') {
				indent += 8;
			}
			else if(character == '\f') {
				indent = 0;
			}
			else if(character == ' ') {
				indent++;
			}
			else {
				break;
			}
		}
		return indent;
	}

	private Tree toTree(Node node, TreeContext context) {
		Tree tree = node.leaf ? context.createTree(TypeSet.type(node.type), content.substring(node.start, node.end)) : context.createTree(TypeSet.type(node.type));
		tree.setPos(Math.max(node.start, 0));
		tree.setLength(Math.max(node.end - node.start, 0));
		for(Node child : node.children) {
			if(!child.ignored) {
				tree.addChild(toTree(child, context));
			}
		}
		return tree;
	}
}
