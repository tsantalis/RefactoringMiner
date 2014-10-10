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

import java.util.Arrays;
import java.util.HashSet;

/** Represents a node in the operator tree for a math expression. */
public class ExpressionNode {

	// math functions
	/** Math function ln - natural logarithm */
	public final static String FCT_LN = "ln";
	/** Math function exp - e to the power of */
	public final static String FCT_EXP = "exp";
	/** Math function sqrt - square root */
	public final static String FCT_SQRT = "sqrt";
	/** Math function abs - absolute value */
	public final static String FCT_ABS = "abs";
	/** Math function floor - largest integer smaller than */
	public final static String FCT_FLOOR = "floor";
	/** Math function ceil - smallest integer larger than */
	public final static String FCT_CEIL = "ceil";
	/** Math function round - next closest integer */
	public final static String FCT_ROUND = "round";

	// string functions
	/** String function to test if a string starts with a capital letter. */
	public final static String FCT_STARTS_WITH_CAPITAL = "startswithcapital";
	/** String function to test if a string starts with a lower case letter. */
	public final static String FCT_STARTS_WITH_LOWERCASE = "startswithlowercase";
	/** String function to test if a string uses only lower case letters. */
	public final static String FCT_ISLOWER = "islowercase";
	/** String function to turn a string to lower case letters. */
	public final static String FCT_TOLOWER = "tolowercase";
	/** String function to obtain the length of a string. */
	public final static String FCT_LENGTH = "length";

	// set functions
	/** Set function cardinality of a set. */
	public final static String FCT_SIZE = "size";
	/**
	 * Set function cardinality of a set (ignore duplicates for multiset).
	 */
	public final static String FCT_FLATSIZE = "flatsize";
	/** Set function to test if a multiset contains any duplicates. */
	public final static String FCT_ISUNIQUE = "isunique";

	// model element functions
	/** Return the metamodel type name of a model element. */
	public final static String FCT_TYPEOF = "typeof";
	/** Return the fully qualified name of a model element. */
	public final static String FCT_QUALIFIEDNAME = "qualifiedname";

	/** The set of all function names. */
	private static final HashSet<String> FUNCTIONNAMES = new HashSet<String>(
			Arrays.asList(FCT_LN, FCT_EXP, FCT_SQRT, FCT_ABS, FCT_FLOOR,
					FCT_CEIL, FCT_ROUND, FCT_STARTS_WITH_CAPITAL,
					FCT_STARTS_WITH_LOWERCASE, FCT_ISLOWER, FCT_TOLOWER,
					FCT_LENGTH, FCT_SIZE, FCT_FLATSIZE, FCT_ISUNIQUE,
					FCT_TYPEOF, FCT_QUALIFIEDNAME));

	// string relations
	/** String relation to test if a string starts with a given prefix. */
	public final static String REL_STARTSWITH = "startswith";
	/** String relation to test if a string ends with a given suffix. */
	public final static String REL_ENDSWITH = "endswith";
	/** String relation to test if a string is included in a word list. */
	public final static String REL_WORDONLIST = "onlist";

	// model element operators
	/** Operator upto. */
	public final static String OPR_UPTO = "upto";
	/** Operator topmost. */
	public final static String OPR_TOPMOST = "topmost";

	/**
	 * Tests if a string is the name of a function.
	 * 
	 * @param s String to test.
	 * @return <code>true</code> if s is a function, else <code>false</code>.
	 */
	static boolean isFunction(String s) {
		return FUNCTIONNAMES.contains(s);
	}

	/**
	 * Tests if a string is the name of a relation.
	 * 
	 * @param s String to test.
	 * @return <code>true</code> if s is a relation, else <code>false</code>.
	 */
	static boolean isRelation(String s) {
		return s.equals(REL_STARTSWITH) || s.equals(REL_ENDSWITH)
				|| s.equals(REL_WORDONLIST);
	}

	/** Left operator tree. */
	private ExpressionNode left;
	/** Right operator tree. */
	private ExpressionNode right;
	/** Type of the node: operator, identifier, constant etc. */
	private NodeType type;
	/** Value of the node. The operator, the identifier, the constant etc. */
	private String value;

	/**
	 * Creates a new node representing an identifier.
	 * 
	 * @param identifier Name of the identifier
	 */
	public ExpressionNode(String identifier) {
		this(NodeType.IDENTIFIER, identifier);
	}

	/**
	 * Creates a new node of a given type and value, without child nodes.
	 * 
	 * @param type Type of the node
	 * @param value Value of the node
	 */
	ExpressionNode(NodeType type, String value) {
		this(type, value, null, null);
	}

	/**
	 * Creates a new node of a given type and value, and with child nodes.
	 * 
	 * @param type Type of the node
	 * @param value Value of the node
	 * @param leftOp left operand
	 * @param rightOp right operand
	 */
	ExpressionNode(NodeType type, String value, ExpressionNode leftOp,
			ExpressionNode rightOp) {
		setValue(type, value, leftOp, rightOp);
	}

	/**
	 * Retrieves the value of this node. The meaning of the value depends on the
	 * node type:
	 * <ul>
	 * <li>For operations, the value is a string indicating the operation: +,-,
	 * etc or a function or relation name.
	 * <li>For numbers, the value is the string representation of the number.
	 * <li>For string constants, the value is the constant string (without the
	 * single quotes).
	 * <li>For identifiers, the value is the identifier.
	 * </ul>
	 * 
	 * @return value of the node
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Retrieves the node representing the left hand side of a binary operation,
	 * or the operand of a unary operation.
	 * 
	 * @return the left node
	 */
	public ExpressionNode getLeftNode() {
		return left;
	}

	/**
	 * Retrieves the node representing the right hand side of a binary
	 * operation.
	 * 
	 * @return the right node
	 */
	public ExpressionNode getRightNode() {
		return right;
	}

	/**
	 * Tests whether this node represents an operation.
	 * 
	 * @return <code>true</code> if this node represents an operation.
	 */
	public boolean isOperation() {
		return type == NodeType.OPERATION;
	}

	/**
	 * Tests whether this node represents a number constant.
	 * 
	 * @return <code>true</code> if this node represents a number constant.
	 */
	public boolean isNumberConstant() {
		return type == NodeType.NUMBER;
	}

	/**
	 * Tests whether this node represents a string constant.
	 * 
	 * @return <code>true</code> if this node represents a string constant.
	 */
	public boolean isStringConstant() {
		return type == NodeType.STRING;
	}

	/**
	 * Tests whether this node represents an identifier.
	 * 
	 * @return <code>true</code> if this node represents an identifier.
	 */
	public boolean isIdentifier() {
		return type == NodeType.IDENTIFIER;
	}

	/**
	 * Sets type, value, and children of this node.
	 * 
	 * @param type New type of the node
	 * @param value New value of the node
	 * @param leftOp New left subnode
	 * @param rightOp New right subnode
	 */
	void setValue(NodeType type, String value, ExpressionNode leftOp,
			ExpressionNode rightOp) {
		this.type = type;
		this.value = value;
		left = leftOp;
		right = rightOp;
	}
}

/**
 * The node types of the expression operator tree.
 */
enum NodeType {
	/** A node representing a unary or binary operation (+, -, exp() etc.) */
	OPERATION,
	/** A node representing a number constant. */
	NUMBER,
	/** A node representing a string constant. */
	STRING,
	/** A node representing the name of identifier. */
	IDENTIFIER,
}
