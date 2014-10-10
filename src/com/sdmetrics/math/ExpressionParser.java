/*
 * SDMetrics Open Core for UML design measurement
 * Copyright (c) 2002-2011 Juergen Wuest
 * To contact the author, see <http://www.sdmetrics.com/Contact.html>.
 * 
 * This file is part of the SDMetrics Open Core.
 * 
 * SDMetrics Open Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
    
 * SDMetrics Open Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SDMetrics Open Core.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.sdmetrics.math;

/** Parses metric, set, and condition expressions. */
public class ExpressionParser {

	/** Special token: opening parenthesis. */
	private static final ExpressionNode OPENINGPARENTHESIS = new ExpressionNode(
			null, "(");
	/** Special token: closing parenthesis. */
	private static final ExpressionNode CLOSINGPARENTHESIS = new ExpressionNode(
			null, ")");
	/** Special token: end of expression. */
	private static final ExpressionNode ENDNODE = new ExpressionNode(null, "");

	/** The expression to parse. */
	private String expression;
	/** Position in the expression string to process next. */
	private int currentPos;
	/** Node representation of the current parse position. */
	private ExpressionNode currentNode;

	/** Error message in case of syntax errors. */
	private String errorMessage;

	/**
	 * Parses an expression and returns its operator tree.
	 * 
	 * @param expr The expression to parse.
	 * @return Root node of the operator tree for the expression, or
	 *         <code>null</code> if the expression could not be parsed.
	 */
	public ExpressionNode parseExpression(String expr) {
		expression = expr;
		currentPos = 0;
		errorMessage = "";
		ExpressionNode result = null;
		try {
			result = parseExpression();
			if (currentPos < expression.length() || currentNode != ENDNODE) {
				throw new ParserException(
						"Unexpected token past end of expression: "
								+ currentNode.getValue(), currentPos);
			}
		} catch (ParserException ex) {
			errorMessage = ex.getMessage();
			result = null;
		}
		return result;
	}

	/**
	 * Retrieves a string describing the syntax error that occurred during
	 * expression parsing.
	 * 
	 * @return Description of the parse error.
	 */
	public String getErrorInfo() {
		return errorMessage;
	}

	/**
	 * Exception thrown by the expression parser on syntax errors.
	 */
	static class ParserException extends RuntimeException {
		private static final long serialVersionUID = 4537409434255097180L;

		/**
		 * @param message Description of the syntax error.
		 * @param pos Index of the location of the error in the expression
		 *        string.
		 */
		ParserException(String message, int pos) {
			super("Parse error at position " + pos + ": " + message);
		}
	}

	/**
	 * Top level parser function to parse an arbitrary expression.
	 * 
	 * @return Root node of the operator tree for the expression.
	 */
	private ExpressionNode parseExpression() {
		return uptoExpression();
	}

	/**
	 * Parses an "upto" expression (operators upto, topmost).
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode uptoExpression() {
		ExpressionNode result = orExpression();
		while (currentNode.isOperation()
				&& (isCurrentNodeValue(ExpressionNode.OPR_UPTO) || isCurrentNodeValue(ExpressionNode.OPR_TOPMOST))) {
			String operator = currentNode.getValue();
			result = handleRightHandSide(result, operator, orExpression());
		}
		return result;
	}

	private boolean isCurrentNodeValue(String value) {
		return value.equals(currentNode.getValue());
	}

	private ExpressionNode handleRightHandSide(ExpressionNode lhs,
			String operator, ExpressionNode rhs) {
		if (rhs == null)
			throw new ParserException("Missing right hand side for operator "
					+ operator, currentPos);
		return new ExpressionNode(NodeType.OPERATION, operator, lhs, rhs);
	}

	/**
	 * Parses an "or" expression.
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode orExpression() {
		ExpressionNode result = andExpression();
		while (currentNode.isOperation() && isCurrentNodeValue("|")) {
			result = handleRightHandSide(result, "|", andExpression());
		}
		return result;
	}

	/**
	 * Parse an "and" expression.
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode andExpression() {
		ExpressionNode result = equalExpression();
		while (currentNode.isOperation() && isCurrentNodeValue("&")) {
			result = handleRightHandSide(result, "&", equalExpression());
		}
		return result;
	}

	/**
	 * Parses an "equal" expression (operators = and !=).
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode equalExpression() {
		ExpressionNode result = relExpression();
		if (currentNode.isOperation()
				&& (isCurrentNodeValue("=") || isCurrentNodeValue("!="))) {
			String operator = currentNode.getValue();
			result = handleRightHandSide(result, operator, relExpression());
		}
		return result;
	}

	/**
	 * Parses a relational expression (comparators &gt;, &ge; etc and relations
	 * "startswith", "endswith", "onlist").
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode relExpression() {
		ExpressionNode result = addExpression();
		if (currentNode.isOperation()
				&& (isCurrentNodeValue("<") || isCurrentNodeValue(">")
						|| isCurrentNodeValue("<=") || isCurrentNodeValue(">=") || ExpressionNode
						.isRelation(currentNode.getValue()))) {
			String operator = currentNode.getValue();
			result = handleRightHandSide(result, operator, addExpression());
		}
		return result;
	}

	/**
	 * Parses an additive expression (binary operators + and -).
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode addExpression() {
		ExpressionNode result = multExpression();
		while (currentNode.isOperation()
				&& (isCurrentNodeValue("+") || isCurrentNodeValue("-"))) {
			String operator = currentNode.getValue();
			result = handleRightHandSide(result, operator, multExpression());
		}
		return result;
	}

	/**
	 * Parses a multiplicative expression (operators *, /).
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode multExpression() {
		ExpressionNode result = powExpression();
		while (currentNode.isOperation()
				&& (isCurrentNodeValue("/") || isCurrentNodeValue("*"))) {
			String operator = currentNode.getValue();
			result = handleRightHandSide(result, operator, powExpression());
		}
		return result;
	}

	/**
	 * Parses a power expression term (operators ^, -&gt;).
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode powExpression() {
		ExpressionNode result = dotExpression();
		while (currentNode.isOperation()
				&& (isCurrentNodeValue("^") || isCurrentNodeValue("->"))) {
			String operator = currentNode.getValue();
			result = handleRightHandSide(result, operator, dotExpression());
		}
		return result;
	}

	/**
	 * Parses a dot expression "a.b"
	 * 
	 * @return Root node of the corresponding operator tree.
	 */
	private ExpressionNode dotExpression() {
		ExpressionNode result = unaryExpression();
		while (currentNode.isOperation() && (isCurrentNodeValue("."))) {
			result = handleRightHandSide(result, ".", unaryExpression());
		}
		return result;
	}

	/**
	 * Parses a unary expression (constants, prefixes +/-/!, function calls,
	 * parenthesis).
	 * 
	 * @return Node representing the unary expression.
	 */
	private ExpressionNode unaryExpression() {

		nextToken();
		String currentValue = currentNode.getValue();

		if (currentNode.isNumberConstant() || currentNode.isStringConstant()
				|| currentNode.isIdentifier()) {
			ExpressionNode constant = currentNode;
			nextToken();
			return constant;
		} else if (currentNode.isOperation()
				&& (ExpressionNode.isFunction(currentValue))
				|| "-".equals(currentValue) || "+".equals(currentValue)
				|| "!".equals(currentValue)) {
			ExpressionNode unary = currentNode;
			unary.setValue(NodeType.OPERATION, currentValue, unaryExpression(),
					null);
			return unary;
		} else if (currentNode == OPENINGPARENTHESIS) {
			int startIndex = currentPos;
			ExpressionNode temp = parseExpression();

			if (currentNode == CLOSINGPARENTHESIS) {
				nextToken();
				return temp;
			}
			throw new ParserException(
					"Missing closing parenthesis opened at position "
							+ startIndex + ".", currentPos);
		} else if (currentNode == ENDNODE) {
			throw new ParserException("Unexpected end of expression.",
					currentPos);
		} else {
			throw new ParserException("Unexpected token: " + currentValue,
					currentPos);
		}
	}

	/**
	 * Consumes a token, leave information about it in {@link #currentType} and
	 * {@link #currentValue} variables.
	 */
	private void nextToken() {
		while (currentPos < expression.length()
				&& Character.isSpaceChar(expression.charAt(currentPos)))
			currentPos++;

		if (currentPos >= expression.length()) {
			currentNode = ENDNODE;
		} else if (expression.charAt(currentPos) == '+'
				|| expression.charAt(currentPos) == '-'
				|| expression.charAt(currentPos) == '*'
				|| expression.charAt(currentPos) == '^'
				|| expression.charAt(currentPos) == '/'
				|| expression.charAt(currentPos) == '='
				|| expression.charAt(currentPos) == '|'
				|| expression.charAt(currentPos) == '!'
				|| expression.charAt(currentPos) == '<'
				|| expression.charAt(currentPos) == '>'
				|| expression.charAt(currentPos) == '.'
				|| expression.charAt(currentPos) == '&') {
			handleSymbolicOperatorToken();
		} else if (expression.charAt(currentPos) == '(') {
			currentPos++;
			currentNode = OPENINGPARENTHESIS;
		} else if (expression.charAt(currentPos) == ')') {
			currentPos++;
			currentNode = CLOSINGPARENTHESIS;
		} else if (expression.charAt(currentPos) == '\'') {
			currentNode = new ExpressionNode(NodeType.STRING,
					getStringConstant());
		} else if (Character.isDigit(expression.charAt(currentPos))) {
			currentNode = new ExpressionNode(NodeType.NUMBER,
					getNumberConstant());
		} else {
			handleIdentifier();
		}
	}

	private void handleSymbolicOperatorToken() {
		String currentValue = String.valueOf(expression.charAt(currentPos));
		currentPos++;
		if ("&".equals(currentValue)) {
			// In case some XML do not translate &amp; and &lt;in quoted
			// text, we accept &amp; as "and" operator and &lt; as "less
			// than" operator
			if (currentPos + 3 < expression.length()) {
				if ("amp;".equals(expression.substring(currentPos,
						currentPos + 4))) {
					currentPos = currentPos + 4; // skip "amp;" part
				} else if (expression.substring(currentPos, currentPos + 3)
						.equals("lt;")) {
					currentPos = currentPos + 3;
					currentValue = "<"; // skip "lt;" part
				}
			}
		}
		// check >=, <= !=
		if (currentPos < expression.length()
				&& expression.charAt(currentPos) == '=') {
			if ("<".equals(currentValue) || ">".equals(currentValue)
					|| "!".equals(currentValue)) {
				currentValue += expression.charAt(currentPos);
				currentPos++;
			}
		} else if (currentPos < expression.length()
				&& expression.charAt(currentPos) == '>') {
			// check ->
			if ("-".equals(currentValue)) {
				currentValue += expression.charAt(currentPos);
				currentPos++;
			}
		}
		currentNode = new ExpressionNode(NodeType.OPERATION, currentValue);
	}

	private void handleIdentifier() {
		String currentValue = getIdentifier();
		NodeType currentType = NodeType.OPERATION;
		if (currentValue.startsWith("fct_")) {
			// In earlier versions, all functions had a prefix (e.g. fct_sqrt()
			// instead of sqrt()). We support this prefix for backwards
			// compatibility.
			String trail = currentValue.substring(4);
			if (ExpressionNode.isFunction(trail))
				currentValue = trail;
		}
		if (ExpressionNode.isFunction(currentValue)
				|| ExpressionNode.isRelation(currentValue)
				|| currentValue.equals(ExpressionNode.OPR_UPTO)
				|| currentValue.equals(ExpressionNode.OPR_TOPMOST))
			currentType = NodeType.OPERATION;
		else if ("and".equals(currentValue)) {
			currentValue = "&";
		} else if ("or".equals(currentValue)) {
			currentValue = "|";
		} else if ("not".equals(currentValue)) {
			currentValue = "!";
		} else if ("lt".equals(currentValue)) {
			currentValue = "<";
		} else if ("gt".equals(currentValue)) {
			currentValue = ">";
		} else if ("le".equals(currentValue)) {
			currentValue = "<=";
		} else if ("ge".equals(currentValue)) {
			currentValue = ">=";
		} else if ("in".equals(currentValue)) {
			currentValue = "->";
		} else
			currentType = NodeType.IDENTIFIER;

		currentNode = new ExpressionNode(currentType, currentValue);
	}

	/**
	 * Parses a number constant in the expression string.
	 * 
	 * @return The number constant, as a string.
	 */
	private String getNumberConstant() {
		int startIndex = currentPos;

		while (currentPos < expression.length()
				&& Character.isDigit(expression.charAt(currentPos))) {
			currentPos++;
		}
		if (currentPos < expression.length()
				&& expression.charAt(currentPos) == '.') {
			currentPos++;
			int fractionStart = currentPos;
			while (currentPos < expression.length()
					&& Character.isDigit(expression.charAt(currentPos))) {
				currentPos++;
			}
			if (fractionStart == currentPos)
				throw new ParserException(
						"Missing fraction part of number constant.", currentPos);
		}
		if (currentPos < expression.length()
				&& (expression.charAt(currentPos) == 'e' || expression
						.charAt(currentPos) == 'E')) {
			currentPos++;
			if (currentPos < expression.length()
					&& (expression.charAt(currentPos) == '+' || expression
							.charAt(currentPos) == '-')) {
				currentPos++;
			}
			int exponentStart = currentPos;
			while (currentPos < expression.length()
					&& Character.isDigit(expression.charAt(currentPos))) {
				currentPos++;
			}
			if (exponentStart == currentPos)
				throw new ParserException(
						"Missing exponent of scientific notation number constant.",
						currentPos);
		}

		return expression.substring(startIndex, currentPos);
	}

	/**
	 * Parses an identifier in the expression string.
	 * 
	 * @return the identifier.
	 */
	private String getIdentifier() {
		int startIndex = currentPos;
		while (currentPos < expression.length()
				&& isIdentifierCharacter(expression.charAt(currentPos))) {
			currentPos++;
		}
		if (startIndex == currentPos)
			throw new ParserException("Unexpected character: "
					+ expression.charAt(currentPos), startIndex + 1);
		return expression.substring(startIndex, currentPos);
	}

	/**
	 * Tests if a character is a valid identifier character.
	 * 
	 * @param c Character to test.
	 * @return <code>true</code> if the character is a letter, digit, or
	 *         underscore.
	 */
	static public boolean isIdentifierCharacter(char c) {
		return Character.isLetter(c) || Character.isDigit(c) || c == '_';
	}

	/**
	 * Parses a string constant in the expression string. String constants are
	 * enclosed by apostrophes.
	 * 
	 * @return The string constant without the enclosing apostrophes.
	 */
	private String getStringConstant() {

		currentPos++; // skip leading apostrophe
		int startIndex = currentPos;
		while (currentPos < expression.length()
				&& expression.charAt(currentPos) != '\'') {
			currentPos++;
		}
		if (currentPos == expression.length()) {
			throw new ParserException(
					"Missing closing apostrophe of string constant starting at position "
							+ startIndex + ".", currentPos);
		}
		currentPos++; // skip the closing apostrophe

		return expression.substring(startIndex, currentPos - 1);
	}
}
