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
package com.sdmetrics.model;

import java.util.ArrayList;
import java.util.List;

import com.sdmetrics.math.ExpressionNode;

/**
 * Stores the XMI pattern and triggers for one XMI transformation.
 */
class XMITransformation {
	/** Name of the XMI element that this XMI transformation handles. */
	private String xmiPattern;
	/** Metamodel element type that this XMI transformation produces. */
	private MetaModelElement type;
	/**
	 * Indicates if the model element produces by this transformation can own
	 * sub-elements.
	 */
	private boolean xmiRecurse = false;
	/**
	 * Indicate if the XMI element must have an explicit XMI id for this
	 * transformation to fire.
	 */
	private boolean requireXMIID = true;
	/** List of triggers stored with this XMI transformation. */
	private ArrayList<XMITrigger> triggers = new ArrayList<XMITrigger>();
	/** Conditional expression for this XMI transformation. */
	private ExpressionNode condition = null;
	/** Line number of the XMI transformation in the definition file. */
	private int lineNumber;

	/**
	 * Creates a new XMI transformation with an empty list of triggers.
	 * 
	 * @param type The metamodel element type that this transformations
	 *        produces.
	 * @param xmiPattern The name of the XMI element that corresponds to the
	 *        metamodel element type.
	 * @param recurse Flag indicating if model elements produced by this
	 *        transformation can own sub-elements.
	 * @param requireXMIID Flag indicating if this transformation requires XMI
	 *        IDs to be present in the XMI file for the transformation to fire
	 * @param condition The condition operator tree for conditional
	 *        transformations. <code>null</code> for unconditional
	 *        transformations.
	 */
	XMITransformation(MetaModelElement type, String xmiPattern,
			boolean recurse, boolean requireXMIID, ExpressionNode condition) {
		this.type = type;
		this.xmiPattern = xmiPattern;
		this.xmiRecurse = recurse;
		this.requireXMIID = requireXMIID;
		this.condition = condition;
	}

	/**
	 * Adds a trigger to the XMI transformation.
	 * 
	 * @param t Trigger to add.
	 */
	void addTrigger(XMITrigger t) {
		triggers.add(t);
	}

	/**
	 * Retrieves the list of triggers of this XMI transformation.
	 * 
	 * @return Random access list of triggers
	 */
	List<XMITrigger> getTriggerList() {
		return triggers;
	}

	/**
	 * Returns the name of the XMI element this XMI transformation handles.
	 * 
	 * @return Name of the XMI element associated with this XMI transformation.
	 */
	String getXMIPattern() {
		return xmiPattern;
	}

	/**
	 * Checks if model elements produced by this transformation can own
	 * sub-elements.
	 * 
	 * @return <code>true</code> if the model element can have sub-elements.
	 */
	boolean getXMIRecurse() {
		return xmiRecurse;
	}

	/**
	 * Checks if the XMI element for this transformation requires an XMI id to
	 * be recognized as design element.
	 * 
	 * @return <code>true</code> if XMI ID is required for recognition as model
	 *         element
	 */
	boolean requiresXMIID() {
		return requireXMIID;
	}

	/**
	 * Returns the operator tree for the condition expression.
	 * 
	 * @return root node of the operator tree, or <code>null</code> if this is
	 *         an unconditional transformation
	 */
	ExpressionNode getConditionExpression() {
		return condition;
	}

	/**
	 * Return the type of elements produced by this XMI transformation.
	 * 
	 * @return metamodel element type of this XMI transformation
	 */
	MetaModelElement getType() {
		return type;
	}

	/**
	 * Sets the line number of this XMI transformation's definition in the XMI
	 * transformation file. Used to pinpoint the user to the error location when
	 * there is a problem during the evaluation of condition expressions.
	 * 
	 * @param line Line number of the transformation
	 */
	void setLineNumber(int line) {
		lineNumber = line;
	}

	/**
	 * Gets the line number of the XMI transformation's definition in the XMI
	 * transformation file.
	 * 
	 * @return Line number of the transformation
	 */
	int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Checks if this XMI transformation has a trigger of a certain name.
	 * 
	 * @param triggerName The name of the trigger to search
	 * @return <code>true</code> if the transformation has a trigger with that
	 *         name
	 */
	boolean hasTrigger(String triggerName) {
		for (XMITrigger trigger : triggers) {
			if (trigger.name.equals(triggerName))
				return true;
		}
		return false;
	}
}
