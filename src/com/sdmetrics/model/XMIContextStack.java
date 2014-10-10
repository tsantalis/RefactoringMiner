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

/**
 * Manages the stack of model elements being processed during parsing of the XMI
 * file.
 */
class XMIContextStack {

	/**
	 * Class to store parser context information for a model element on the
	 * stack. The context stack holds instances of this class.
	 */
	private static class Context {
		/** The model element of the parse context. */
		ModelElement modelElement;
		/** Nesting level of the XML element of that model element. */
		int nestingLevel;
		/** The XMI transformation to use for the model element. */
		XMITransformation transformation;
		/** The currently active XMI trigger. */
		XMITrigger activeTrigger;
	}

	/** Model element stack to keep track of the parser state. */
	private ArrayList<Context> stack;
	/** Index of the top element. */
	private int topIndex;
	/** Top element on the stack. */
	private Context top;

	/** Create a new, empty stack. */
	XMIContextStack() {
		stack = new ArrayList<Context>();
		// keep a default element at the bottom of the stack with reasonable
		// values to return when the stack is otherwise empty.
		top = new Context();
		stack.add(top);
		topIndex = 0;
	}

	/**
	 * Pushes a new model element on top of the stack.
	 * 
	 * @param element the model element
	 * @param trans the XMI transformation applicable to this model element.
	 * @param nesting Nesting level of the XML element that represents the model
	 *        element.
	 */
	void push(ModelElement element, XMITransformation trans, int nesting) {
		topIndex++;
		if (topIndex >= stack.size()) {
			top = new Context();
			stack.add(top);
		} else {
			top = stack.get(topIndex);
		}

		top.modelElement = element;
		top.transformation = trans;
		top.nestingLevel = nesting;
	}

	/**
	 * Removes the model element on top of the stack and returns it.
	 * 
	 * @return The model element removed from the top of the stack.
	 */
	ModelElement pop() {
		ModelElement result = top.modelElement;
		topIndex--;
		top = stack.get(topIndex);
		return result;
	}

	/**
	 * Checks if the stack is empty.
	 * 
	 * @return <code>true</code> if the stack is empty.
	 */
	boolean isEmpty() {
		return topIndex == 0; // the bottom element does not count
	}

	/**
	 * Gets the model element on top of the context stack.
	 * 
	 * @return Model element on top of the stack
	 */
	ModelElement getModelElement() {
		return top.modelElement;
	}

	/**
	 * Gets the XML nesting level for the model element on top of the stack.
	 * 
	 * @return Nesting level for the top element.
	 */
	int getNestingLevel() {
		return top.nestingLevel;
	}

	/**
	 * Retrieves the XMI transformations applicable to the model element on top
	 * of the stack.
	 * 
	 * @return XMI transformation for the top element.
	 */
	XMITransformation getXMITransformation() {
		return top.transformation;
	}

	/**
	 * Tests if the current parse context accepts new model elements. This is
	 * the case if the stack is empty, or the model element on top of the stack
	 * can have sub-elements.
	 * 
	 * @return <code>true</code> if new elements are accepted.
	 */
	boolean isAcceptingNewElements() {
		if (top.transformation == null)
			return true;
		return top.transformation.getXMIRecurse();
	}

	/**
	 * Sets the active trigger for the XMI transformation on top of the stack.
	 * 
	 * @param trigger active trigger for the top XMI transformation
	 */
	void setActiveTrigger(XMITrigger trigger) {
		top.activeTrigger = trigger;
	}

	/**
	 * Gets the active trigger.
	 * 
	 * @return The active trigger for the top XMI transformation
	 */
	XMITrigger getActiveTrigger() {
		return top.activeTrigger;
	}

	/**
	 * Checks if the active trigger is of the specified type.
	 * 
	 * @param type trigger type to compare the active trigger to
	 * @return <code>true</code> if the active trigger is of the specified type,
	 *         <code>false</code> if it is of a different type or there is no
	 *         current active trigger.
	 */
	boolean activeTriggerTypeEquals(XMITrigger.TriggerType type) {
		if (top.activeTrigger == null)
			return false;
		return top.activeTrigger.type == type;
	}
}
