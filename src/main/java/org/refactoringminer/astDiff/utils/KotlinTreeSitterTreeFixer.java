package org.refactoringminer.astDiff.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

/**
 * Repairs known tree-sitter-kotlin mis-parses.
 * <p>
 * 1. The comments following a node that is not terminated by a closing token are attached as its trailing children.
 * <pre>
 * import java.util.concurrent.TimeUnit.MILLISECONDS
 *
 * /** A socket connection to a remote peer. *&#47;
 * class Http2Connection
 * </pre>
 * is parsed as {@code import_list(..., import_header(identifier, multiline_comment))}, with the ranges of import_header and import_list
 * extended to the end of the class documentation. The trailing comments are moved after the node in its parent,
 * i.e., {@code source_file(import_list(..., import_header(identifier)), multiline_comment, class_declaration)}.
 * <p>
 * 2. An expression statement starting with a parenthesized expression
 * is attached as call arguments to the end of the previous statement. an expression statement starting with a parenthesized expression
 * is attached as call arguments to the end of the previous statement.
 * <pre>
 * val stream = streams.remove(streamId)
 * (this as Object).notifyAll()
 * </pre>
 * is parsed as {@code val stream = streams.remove(streamId)(this as Object).notifyAll()}.
 * In Kotlin, a newline before the opening parenthesis of a call suffix terminates the statement
 * (unless the newline is enclosed in parentheses or brackets), so the tree is split back into two sibling statements:
 * <pre>
 * statements                                   statements
 *   property_declaration                         property_declaration
 *     call_expression            (E)               call_expression  streams.remove(streamId)
 *       navigation_expression                    call_expression            (E)
 *         call_expression        (C)    ==>        navigation_expression
 *           call_expression  (A)                     parenthesized_expression
 *           call_suffix      (S)                       as_expression
 *             value_arguments                        navigation_suffix
 *               value_argument                     call_suffix
 *                 as_expression
 *         navigation_suffix
 *       call_suffix
 * </pre>
 * <p>
 * 3. A prefix operator is applied to the entire binary expression following it, although it has higher precedence than all binary operators.
 * <pre>
 * !out || a == 0L
 * </pre>
 * is parsed as {@code prefix_expression(!, disjunction_expression(out, ||, equality_expression))}.
 * The unparenthesized binary expressions containing the mis-parsed prefix expression are flattened into their operands and operators,
 * and rebuilt according to the operator precedence, with the prefix operator applied only to the first operand following it, i.e.,
 * {@code disjunction_expression(prefix_expression(!, out), ||, equality_expression)}.
 * Similarly, {@code a != -1 && b}, parsed as {@code equality_expression(a, !=, prefix_expression(-, conjunction_expression(1, &&, b)))},
 * is rebuilt as {@code conjunction_expression(equality_expression(a, !=, prefix_expression(-, 1)), &&, b)}.
 * <p>
 * 4. The opening delimiter of a multi-line comment starting with a line break is replaced with the indentation of the comment.
 * <pre>
 *   /**
 *    * text
 *    *&#47;
 * </pre>
 * is labeled as {@code "  \n   * text\n   *&#47;"}. The label is restored to the source code of the comment.
 * <p>
 * 5. An empty block has no node, as the braces of a block are not included in the tree and only its statements are.
 * <pre>
 * try { a() } catch (e: IOException) { }
 * </pre>
 * is parsed as {@code try_expression(statements, catch_block(simple_identifier, user_type))}. An empty statements node
 * spanning the braces is added to the empty try, catch, and finally blocks, i.e., {@code catch_block(simple_identifier, user_type, statements)}.
 */
public class KotlinTreeSitterTreeFixer {
	private static final String STATEMENTS = "statements";
	private static final String CALL_EXPRESSION = "call_expression";
	private static final String CALL_SUFFIX = "call_suffix";
	private static final String VALUE_ARGUMENTS = "value_arguments";
	private static final String VALUE_ARGUMENT = "value_argument";
	private static final String PARENTHESIZED_EXPRESSION = "parenthesized_expression";
	private static final String MULTILINE_COMMENT = "multiline_comment";
	private static final Set<String> COMMENTS = Set.of("line_comment", MULTILINE_COMMENT);
	// nodes without a closing token, which absorb the comments preceding the next sibling, i.e., the documentation of the first declaration
	private static final Set<String> TRAILING_COMMENT_OWNERS = Set.of("package_header", "import_header", "import_list", "when_entry");
	// expressions that can have the mis-parsed call as their leftmost operand, i.e., (x as Object).notifyAll() or (a) + b
	private static final Set<String> LEFT_OPERAND_EXPRESSIONS = Set.of(CALL_EXPRESSION, "navigation_expression", "indexing_expression",
			"postfix_expression", "as_expression", "additive_expression", "multiplicative_expression", "comparison_expression",
			"equality_expression", "conjunction_expression", "disjunction_expression", "elvis_expression", "infix_expression",
			"range_expression", "check_expression");
	// nodes that can have the mis-parsed call as their rightmost part, i.e., val x = foo or x = foo or return foo
	private static final Set<String> RIGHT_OPERAND_NODES = Set.of("property_declaration", "assignment", "jump_expression", "prefix_expression",
			"additive_expression", "multiplicative_expression", "comparison_expression", "equality_expression", "conjunction_expression",
			"disjunction_expression", "elvis_expression", "infix_expression", "range_expression", "check_expression", "as_expression");

	private static final String TRY_EXPRESSION = "try_expression";
	private static final String CATCH_BLOCK = "catch_block";
	private static final String FINALLY_BLOCK = "finally_block";
	private static final String PREFIX_EXPRESSION = "prefix_expression";
	private static final Set<String> PREFIX_OPERATORS = Set.of("!", "-", "+", "++", "--");
	// binary expressions with lower precedence than the prefix operators, from the highest to the lowest precedence
	private static final List<String> BINARY_EXPRESSIONS = List.of("as_expression", "multiplicative_expression", "additive_expression",
			"range_expression", "infix_expression", "elvis_expression", "check_expression", "comparison_expression", "equality_expression",
			"conjunction_expression", "disjunction_expression");

	public static void fix(TreeContext context, String sourceCode) {
		restoreCommentDelimiters(context, sourceCode);
		moveTrailingComments(context);
		Tree call;
		while ((call = findMisparsedCall(context.getRoot(), sourceCode)) != null) {
			split(context, call);
		}
		fixPrefixExpressions(context);
		addEmptyBlockStatements(context, sourceCode);
	}

	private static void fixPrefixExpressions(TreeContext context) {
		boolean changed = false;
		Tree prefix;
		// post-order, so that in !!a && b the inner prefix expression is fixed before the outer one
		while ((prefix = findMisparsedPrefixExpression(context.getRoot())) != null) {
			// the region of binary expressions containing the prefix expression as their rightmost part, i.e., a != -1 && b
			// is parsed as equality_expression(a, !=, prefix_expression(-, conjunction_expression(1, &&, b)))
			Tree top = prefix;
			while (top.getParent() != null && isBinaryExpression(top.getParent()) && top.getParent().getChild(2) == top) {
				top = top.getParent();
			}
			Tree parent = top.getParent();
			int index = parent.getChildPosition(top);
			List<Tree> operands = new ArrayList<>();
			List<Tree> operators = new ArrayList<>();
			flatten(top, operands, operators);
			Tree root = rebuild(operands, operators, new int[] {0}, 0);
			// top is reused inside root, so its parent must not be reset as in replaceChild
			parent.getChildren().set(index, root);
			root.setParent(parent);
			changed = true;
		}
		if (changed)
			resetMetrics(context.getRoot());
	}

	private static Tree findMisparsedPrefixExpression(Tree root) {
		for (Tree t : root.postOrder()) {
			if (isMisparsedPrefixExpression(t))
				return t;
		}
		return null;
	}

	/**
	 * prefix_expression(op, binary(left, ...)) where binary has lower precedence than op
	 */
	private static boolean isMisparsedPrefixExpression(Tree t) {
		if (!t.getType().name.equals(PREFIX_EXPRESSION) || t.getParent() == null || t.getChildren().size() != 2)
			return false;
		Tree operator = t.getChild(0);
		return operator.isLeaf() && PREFIX_OPERATORS.contains(operator.getLabel()) && isBinaryExpression(t.getChild(1));
	}

	private static boolean isBinaryExpression(Tree t) {
		return BINARY_EXPRESSIONS.contains(t.getType().name) && t.getChildren().size() == 3;
	}

	private static int precedence(Tree binaryExpression) {
		return BINARY_EXPRESSIONS.size() - BINARY_EXPRESSIONS.indexOf(binaryExpression.getType().name);
	}

	/**
	 * collects the operands and operators of the binary expressions in source code order,
	 * applying the mis-parsed prefix operators only to the first operand following them
	 */
	private static void flatten(Tree t, List<Tree> operands, List<Tree> operators) {
		if (isBinaryExpression(t)) {
			flatten(t.getChild(0), operands, operators);
			// the binary expression is reused for its operator
			operators.add(t);
			flatten(t.getChild(2), operands, operators);
		}
		else if (isMisparsedPrefixExpression(t)) {
			int index = operands.size();
			flatten(t.getChild(1), operands, operators);
			Tree operand = operands.get(index);
			setChildren(t, t.getChild(0), operand);
			t.setLength(operand.getEndPos() - t.getPos());
			operands.set(index, t);
		}
		else {
			operands.add(t);
		}
	}

	/**
	 * precedence climbing, where all binary operators are left-associative
	 */
	private static Tree rebuild(List<Tree> operands, List<Tree> operators, int[] next, int minPrecedence) {
		Tree left = operands.get(next[0]);
		while (next[0] < operators.size() && precedence(operators.get(next[0])) >= minPrecedence) {
			Tree binary = operators.get(next[0]++);
			Tree right = rebuild(operands, operators, next, precedence(binary) + 1);
			setChildren(binary, left, binary.getChild(1), right);
			binary.setPos(left.getPos());
			binary.setLength(right.getEndPos() - left.getPos());
			left = binary;
		}
		return left;
	}

	private static void setChildren(Tree parent, Tree... children) {
		List<Tree> list = new ArrayList<>(List.of(children));
		parent.setChildren(list);
		for (Tree child : list) {
			child.setParent(parent);
		}
	}

	private static void addEmptyBlockStatements(TreeContext context, String sourceCode) {
		boolean changed = false;
		List<Tree> nodes = new ArrayList<>();
		context.getRoot().preOrder().forEach(nodes::add);
		for (Tree t : nodes) {
			String type = t.getType().name;
			if (type.equals(TRY_EXPRESSION)) {
				// the try block is the first child, followed by the catch and finally blocks
				Tree first = t.getChildren().isEmpty() ? null : t.getChild(0);
				if (first == null || first.getType().name.equals(STATEMENTS))
					continue;
				changed |= addEmptyStatements(context, t, 0, t.getPos() + "try".length(), sourceCode);
			}
			else if (type.equals(CATCH_BLOCK)) {
				// the catch block is the last child, following the exception parameter
				if (t.getChildren().isEmpty() || hasChild(t, STATEMENTS))
					continue;
				int index = t.getChildren().size();
				while (index > 0 && COMMENTS.contains(t.getChild(index - 1).getType().name))
					index--;
				if (index == 0)
					continue;
				changed |= addEmptyStatements(context, t, index, t.getChild(index - 1).getEndPos(), sourceCode);
			}
			else if (type.equals(FINALLY_BLOCK)) {
				if (hasChild(t, STATEMENTS))
					continue;
				changed |= addEmptyStatements(context, t, 0, t.getPos() + "finally".length(), sourceCode);
			}
		}
		if (changed)
			resetMetrics(context.getRoot());
	}

	/**
	 * inserts an empty statements node for the braces of the empty block following the start position,
	 * moving the comments inside the braces to the statements node
	 */
	private static boolean addEmptyStatements(TreeContext context, Tree parent, int index, int start, String sourceCode) {
		int open = start;
		// the closing parenthesis of the catch parameter
		while (open < sourceCode.length() && (Character.isWhitespace(sourceCode.charAt(open)) || sourceCode.charAt(open) == ')'))
			open++;
		if (open >= sourceCode.length() || sourceCode.charAt(open) != '{')
			return false;
		int close = skipWhitespaceAndComments(sourceCode, open + 1);
		if (close >= sourceCode.length() || sourceCode.charAt(close) != '}')
			return false;
		Tree statements = context.createTree(TypeSet.type(STATEMENTS));
		statements.setPos(open);
		statements.setLength(close + 1 - open);
		List<Tree> comments = new ArrayList<>();
		for (Tree child : parent.getChildren()) {
			if (COMMENTS.contains(child.getType().name) && child.getPos() > open && child.getPos() < close)
				comments.add(child);
		}
		parent.getChildren().removeAll(comments);
		for (Tree comment : comments) {
			statements.addChild(comment);
			comment.setParent(statements);
		}
		parent.insertChild(statements, Math.min(index, parent.getChildren().size()));
		statements.setParent(parent);
		return true;
	}

	private static int skipWhitespaceAndComments(String sourceCode, int i) {
		while (i < sourceCode.length()) {
			if (Character.isWhitespace(sourceCode.charAt(i))) {
				i++;
			}
			else if (sourceCode.startsWith("//", i)) {
				int end = sourceCode.indexOf('\n', i);
				i = end < 0 ? sourceCode.length() : end;
			}
			else if (sourceCode.startsWith("/*", i)) {
				int end = sourceCode.indexOf("*/", i + 2);
				i = end < 0 ? sourceCode.length() : end + 2;
			}
			else {
				break;
			}
		}
		return i;
	}

	private static boolean hasChild(Tree t, String type) {
		for (Tree child : t.getChildren()) {
			if (child.getType().name.equals(type))
				return true;
		}
		return false;
	}

	private static void restoreCommentDelimiters(TreeContext context, String sourceCode) {
		for (Tree t : context.getRoot().preOrder()) {
			if (!t.getType().name.equals(MULTILINE_COMMENT) || t.getPos() >= sourceCode.length())
				continue;
			String label = t.getLabel();
			if (sourceCode.startsWith(label, t.getPos()))
				continue;
			// the positions are in characters, while the lengths are in UTF-8 bytes, so the label is restored from the start of the comment
			String text = label.replaceFirst("^ +", "");
			for (String delimiter : new String[] {"/**", "/*"}) {
				if (sourceCode.startsWith(delimiter, t.getPos()) && sourceCode.startsWith(text, t.getPos() + delimiter.length())) {
					t.setLabel(delimiter + text);
					break;
				}
			}
		}
	}

	private static void moveTrailingComments(TreeContext context) {
		boolean changed = false;
		// post-order, so that the comments moved from the last import_header to import_list are moved again to source_file
		List<Tree> nodes = new ArrayList<>();
		context.getRoot().postOrder().forEach(nodes::add);
		for (Tree t : nodes) {
			if (!TRAILING_COMMENT_OWNERS.contains(t.getType().name) || t.getParent() == null)
				continue;
			int first = t.getChildren().size();
			while (first > 1 && COMMENTS.contains(t.getChild(first - 1).getType().name))
				first--;
			if (first == t.getChildren().size())
				continue;
			List<Tree> comments = new ArrayList<>(t.getChildren().subList(first, t.getChildren().size()));
			t.getChildren().removeAll(comments);
			t.setLength(t.getChild(first - 1).getEndPos() - t.getPos());
			Tree parent = t.getParent();
			int index = parent.getChildPosition(t) + 1;
			for (Tree comment : comments) {
				parent.insertChild(comment, index++);
			}
			changed = true;
		}
		if (changed)
			resetMetrics(context.getRoot());
	}

	private static Tree findMisparsedCall(Tree root, String sourceCode) {
		for (Tree t : root.preOrder()) {
			if (isMisparsedCall(t, sourceCode) && findStatement(leftOperandChainTop(t)) != null) {
				return t;
			}
		}
		return null;
	}

	/**
	 * call_expression(A, [comments], call_suffix(value_arguments(value_argument(X)))) with a newline between A and the call_suffix
	 */
	private static boolean isMisparsedCall(Tree t, String sourceCode) {
		if (!t.getType().name.equals(CALL_EXPRESSION) || t.getChildren().size() < 2)
			return false;
		for (int i = 1; i < t.getChildren().size() - 1; i++) {
			if (!COMMENTS.contains(t.getChild(i).getType().name))
				return false;
		}
		Tree callee = t.getChild(0);
		Tree suffix = t.getChild(t.getChildren().size() - 1);
		if (!suffix.getType().name.equals(CALL_SUFFIX) || suffix.getChildren().size() != 1)
			return false;
		Tree arguments = suffix.getChild(0);
		if (!arguments.getType().name.equals(VALUE_ARGUMENTS) || arguments.getChildren().size() != 1)
			return false;
		Tree argument = arguments.getChild(0);
		if (!argument.getType().name.equals(VALUE_ARGUMENT) || argument.getChildren().size() != 1)
			return false;
		// excludes named arguments and spread operator
		if (argument.getPos() != argument.getChild(0).getPos())
			return false;
		if (suffix.getPos() > sourceCode.length())
			return false;
		// tree-sitter lengths are in UTF-8 bytes, while positions are in characters, so end positions are unreliable for non-ASCII code.
		// Scan backwards from the opening parenthesis over whitespace and the comments in between, until a newline or code is found
		int commentIndex = t.getChildren().size() - 2;
		int i = suffix.getPos() - 1;
		while (i >= callee.getPos()) {
			char c = sourceCode.charAt(i);
			if (c == '\n')
				return true;
			if (Character.isWhitespace(c)) {
				i--;
			}
			else if (commentIndex > 0) {
				i = t.getChild(commentIndex--).getPos() - 1;
			}
			else {
				return false;
			}
		}
		return false;
	}

	private static Tree leftOperandChainTop(Tree call) {
		Tree top = call;
		while (top.getParent() != null && LEFT_OPERAND_EXPRESSIONS.contains(top.getParent().getType().name) && top.getParent().getChild(0) == top) {
			top = top.getParent();
		}
		return top;
	}

	/**
	 * @return the direct child of a statements node containing chainTop as its rightmost part, or null if the newline is not a statement terminator
	 */
	private static Tree findStatement(Tree chainTop) {
		Tree current = chainTop;
		while (current.getParent() != null && !current.getParent().getType().name.equals(STATEMENTS)) {
			Tree parent = current.getParent();
			if (!RIGHT_OPERAND_NODES.contains(parent.getType().name) || parent.getChild(parent.getChildren().size() - 1) != current)
				return null;
			current = parent;
		}
		return current.getParent() != null ? current : null;
	}

	private static void split(TreeContext context, Tree call) {
		Tree chainTop = leftOperandChainTop(call);
		Tree statement = findStatement(chainTop);
		Tree statements = statement.getParent();
		Tree callee = call.getChild(0);
		List<Tree> comments = new ArrayList<>(call.getChildren().subList(1, call.getChildren().size() - 1));
		Tree arguments = call.getChild(call.getChildren().size() - 1).getChild(0);
		Tree expression = arguments.getChild(0).getChild(0);

		Tree parenthesized = context.createTree(TypeSet.type(PARENTHESIZED_EXPRESSION));
		parenthesized.setPos(arguments.getPos());
		parenthesized.setLength(arguments.getLength());
		parenthesized.addChild(expression);
		expression.setParent(parenthesized);

		// the second statement starts with the parenthesized expression
		Tree secondStatement;
		if (chainTop == call) {
			secondStatement = parenthesized;
		}
		else {
			replaceChild(call.getParent(), call, parenthesized);
			for (Tree t = parenthesized.getParent(); t != chainTop.getParent(); t = t.getParent()) {
				int endPos = t.getEndPos();
				t.setPos(parenthesized.getPos());
				t.setLength(endPos - parenthesized.getPos());
			}
			secondStatement = chainTop;
		}
		// the first statement ends with the callee
		Tree firstStatement;
		if (chainTop == statement) {
			firstStatement = callee;
		}
		else {
			replaceChild(chainTop.getParent(), chainTop, callee);
			for (Tree t = callee.getParent(); t != statements; t = t.getParent()) {
				t.setLength(callee.getEndPos() - t.getPos());
			}
			firstStatement = statement;
		}
		int index = statements.getChildPosition(statement);
		statements.getChildren().remove(index);
		List<Tree> newStatements = new ArrayList<>();
		newStatements.add(firstStatement);
		newStatements.addAll(comments);
		newStatements.add(secondStatement);
		for (Tree t : newStatements) {
			statements.insertChild(t, index++);
		}
		resetMetrics(context.getRoot());
	}

	private static void replaceChild(Tree parent, Tree oldChild, Tree newChild) {
		int index = parent.getChildPosition(oldChild);
		parent.getChildren().set(index, newChild);
		newChild.setParent(parent);
		oldChild.setParent(null);
	}

	private static void resetMetrics(Tree root) {
		for (Tree t : root.preOrder()) {
			t.setMetrics(null);
		}
	}
}
