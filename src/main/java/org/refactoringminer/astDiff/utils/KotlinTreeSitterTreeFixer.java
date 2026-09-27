package org.refactoringminer.astDiff.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;

/**
 * Repairs a known tree-sitter-kotlin mis-parse, where an expression statement starting with a parenthesized expression
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
 */
public class KotlinTreeSitterTreeFixer {
	private static final String STATEMENTS = "statements";
	private static final String CALL_EXPRESSION = "call_expression";
	private static final String CALL_SUFFIX = "call_suffix";
	private static final String VALUE_ARGUMENTS = "value_arguments";
	private static final String VALUE_ARGUMENT = "value_argument";
	private static final String PARENTHESIZED_EXPRESSION = "parenthesized_expression";
	private static final Set<String> COMMENTS = Set.of("line_comment", "multiline_comment");
	// expressions that can have the mis-parsed call as their leftmost operand, i.e., (x as Object).notifyAll() or (a) + b
	private static final Set<String> LEFT_OPERAND_EXPRESSIONS = Set.of(CALL_EXPRESSION, "navigation_expression", "indexing_expression",
			"postfix_expression", "as_expression", "additive_expression", "multiplicative_expression", "comparison_expression",
			"equality_expression", "conjunction_expression", "disjunction_expression", "elvis_expression", "infix_expression",
			"range_expression", "check_expression");
	// nodes that can have the mis-parsed call as their rightmost part, i.e., val x = foo or x = foo or return foo
	private static final Set<String> RIGHT_OPERAND_NODES = Set.of("property_declaration", "assignment", "jump_expression", "prefix_expression",
			"additive_expression", "multiplicative_expression", "comparison_expression", "equality_expression", "conjunction_expression",
			"disjunction_expression", "elvis_expression", "infix_expression", "range_expression", "check_expression", "as_expression");

	public static void fix(TreeContext context, String sourceCode) {
		Tree call;
		while ((call = findMisparsedCall(context.getRoot(), sourceCode)) != null) {
			split(context, call);
		}
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
