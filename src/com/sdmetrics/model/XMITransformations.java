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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sdmetrics.math.ExpressionNode;
import com.sdmetrics.math.ExpressionParser;
import com.sdmetrics.math.MappedCollectionsIterator;
import com.sdmetrics.util.SAXHandler;

/**
 * Container and XML parser for XMI transformations.
 */
public class XMITransformations {

	/** The name of the top level XML element in the XMI transformation file. */
	public final static String TL_ELEMENT = "xmitransformations";

	/**
	 * HashMap to store the XMI transformations. Maps the XMI element that
	 * triggers the transformation to a list of candidate XMI transformations
	 * for that XMI element.
	 */
	private HashMap<String, ArrayList<XMITransformation>> transformations = new HashMap<String, ArrayList<XMITransformation>>();
	/** Metamodel on which the XMI transformations are based. */
	private MetaModel metaModel;

	/**
	 * @param metaModel Metamodel on which the XMI transformations are based.
	 */
	public XMITransformations(MetaModel metaModel) {
		this.metaModel = metaModel;
	}

	/**
	 * Gets a SAX handler to parse an XMI transformation file and store the
	 * transformations with this object.
	 * 
	 * @return SAX handler to parse the XMI transformation file
	 */
	public DefaultHandler getSAXParserHandler() {
		return new XMITranformationsParser();
	}

	/**
	 * Returns the XMI transformation for a particular XMI element. If
	 * conditional XMI transformations exist for the element, and the element
	 * matches at least one of them, an arbitrary matching conditional condition
	 * is returned. If there are no matching conditional XMI transformations, an
	 * unconditional XMI transformation is returned, if one exists.
	 * 
	 * @param xmlElement The name of the XMI element.
	 * @param attrs The XML attributes of the element.
	 * @return The XMI transformation for the XMI element, or <code>null</code>
	 *         if no matching transformation was found.
	 * @throws SAXException An error occurred evaluating the condition of a
	 *         conditional transformation.
	 */
	XMITransformation getTransformation(String xmlElement, Attributes attrs)
			throws SAXException {
		ArrayList<XMITransformation> candidates = transformations
				.get(xmlElement);
		if (candidates == null)
			return null;

		// Find an XMI transformation with matching condition
		for (XMITransformation trans : candidates) {
			try {
				if (eval_boolean_expr(trans.getConditionExpression(), attrs))
					return trans;
			} catch (Exception e) {
				throw new SAXException(
						"Error evaluating condition for XMI transformation \""
								+ trans.getXMIPattern() + "\" in line "
								+ trans.getLineNumber()
								+ " of the XMI transformation file: "
								+ e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Evaluates a condition expression for a conditional XMI transformation.
	 * 
	 * @param node Operator tree of the condition expression.
	 * @param attr Attributes of the candidate XML element.
	 * @return <code>true</code> if the attribute values of the XML element
	 *         fulfill the condition, else <code>false</code>.
	 * @throws IllegalArgumentException The condition expression could not be
	 *         evaluated.
	 */
	private boolean eval_boolean_expr(ExpressionNode node, Attributes attr) {
		if (node == null)
			return true; // no expression is a match
		String operator = node.getValue();
		if ("!".equals(operator)) // handle negation
			return !eval_boolean_expr(node.getLeftNode(), attr);
		if ("&".equals(operator)) { // handle logical "and"
			if (eval_boolean_expr(node.getLeftNode(), attr))
				return eval_boolean_expr(node.getRightNode(), attr);
			return false;
		}
		if ("|".equals(operator)) { // handle logical "or"
			if (!eval_boolean_expr(node.getLeftNode(), attr))
				return eval_boolean_expr(node.getRightNode(), attr);
			return true;
		}

		// comparisons =, !=
		if ("=".equals(operator) || "!=".equals(operator)) {
			String lhs = eval_expr(node.getLeftNode(), attr);
			String rhs = eval_expr(node.getRightNode(), attr);
			if ("=".equals(operator))
				return lhs.equals(rhs);
			return !lhs.equals(rhs);
		}

		throw new IllegalArgumentException("Illegal boolean operation: "
				+ operator);
	}

	/**
	 * Evaluates identifiers and constants of an XMI transformation condition.
	 * 
	 * @param node Operator tree node containing the identifier/constant.
	 * @param attr Attributes of the candidate XML element.
	 * @return Value of the identifier or constant.
	 */
	private String eval_expr(ExpressionNode node, Attributes attr) {

		if (node.isNumberConstant() || node.isStringConstant())
			return node.getValue();

		// treat node as identifier, get value of the XML attribute of that name
		int attrIndex = attr.getIndex(node.getValue());
		if (attrIndex >= 0)
			return attr.getValue(attrIndex);

		// Attribute not set - return an empty string. This is designed to
		// ensure that comparisons attr='' or attr!='' can be used to test if an
		// attribute is set or not
		return "";
	}

	/**
	 * SAX handler to parse an XMI transformation file.
	 */
	class XMITranformationsParser extends SAXHandler {

		private final static String ELEM_TRANSFORMATION = "xmitransformation";
		private final static String ELEM_TRIGGER = "trigger";

		/** Value of the "requirexmiid" attribute of the top level element. */
		private boolean globalRequireID = true;

		/**
		 * Expression parser for condition expressions of conditional
		 * transformations.
		 */
		private ExpressionParser exprParser = new ExpressionParser();

		/** The XMI transformation currently being read. */
		private XMITransformation currentTrans;

		/**
		 * Processes a new XMI transformation or trigger.
		 * 
		 * @throws SAXException Illegal XML elements or attributes were
		 *         specified.
		 */
		@Override
		public void startElement(String uri, String local, String raw,
				Attributes attrs) throws SAXException {
			if (TL_ELEMENT.equals(raw)) {
				checkVersion(attrs, null);
				// if not explicitly set to false, XMI IDs are required by
				// default
				globalRequireID = !("false".equals(attrs
						.getValue("requirexmiid")));
			} else if (ELEM_TRANSFORMATION.equals(raw)) {
				processTransformation(attrs);
			} else if (ELEM_TRIGGER.equals(raw)) {
				processTrigger(attrs);
			} else {
				reportError("Unexpected XML element <" + raw + ">.");
			}
		}

		private void processTransformation(Attributes attrs)
				throws SAXException {

			if (currentTrans != null)
				reportError("XMI transformations must not be nested.");

			// check model element attribute
			String typeName = attrs.getValue("modelelement");
			if (typeName == null)
				reportError("XMI transformation is missing the \"modelelement\" attribute.");

			MetaModelElement type = metaModel.getType(typeName);
			if (type == null)
				reportError("Unknown metamodel element type \"" + typeName
						+ "\".");

			// check recurse attribute
			boolean recurse = "true".equals(attrs.getValue("recurse"));

			// check requirexmiid attribute
			boolean requireID;
			String reqIDValue = attrs.getValue("requirexmiid");
			if (globalRequireID) // use default value "true"
				requireID = !"false".equals(reqIDValue);
			else
				// use default value "false"
				requireID = "true".equals(reqIDValue);

			// parse condition expression, if any.
			ExpressionNode expr = null;
			String condition = attrs.getValue("condition");
			if (condition != null) {
				expr = exprParser.parseExpression(condition);
				if (expr == null)
					reportError("Invalid condition expression \"" + condition
							+ "\": " + exprParser.getErrorInfo());
			}

			currentTrans = new XMITransformation(type,
					attrs.getValue("xmipattern"), recurse, requireID, expr);
			currentTrans.setLineNumber(locator.getLineNumber());
		}

		private void processTrigger(Attributes attrs) throws SAXException {
			if (currentTrans == null)
				reportError("Trigger definition outside of an XMI transformation definition.");

			// check trigger name
			String name = attrs.getValue("name");
			if (name == null)
				reportError("Trigger is missing the \"name\" attribute.");

			// check trigger type
			String typeName = attrs.getValue("type");
			if (typeName == null)
				reportError("Trigger is missing the \"type\" attribute.");

			if (XMITrigger.TriggerType.REFLIST.toString().equals(typeName)) {
				if (metaModel.getType(name) == null)
					reportError("Unknown metamodel element type \"" + name
							+ "\" for reflist attribute \"attr\".");
			} else {
				MetaModelElement type = currentTrans.getType();
				if (!type.hasAttribute(name))
					reportError("Attribute \"attr\": Unknown metamodel attribute \""
							+ name
							+ "\" for elements of type \""
							+ type.getName() + "\".");
			}

			// add the new trigger to the current XMITransformation
			try {
				XMITrigger trigger = new XMITrigger(name, typeName,
						attrs.getValue("src"), attrs.getValue("attr"),
						attrs.getValue("linkbackattr"));
				currentTrans.addTrigger(trigger);
			} catch (IllegalArgumentException ex) {
				reportError(ex.getMessage());
			}
		}

		/**
		 * Adds each new transformation after it has completed parsing.
		 */
		@Override
		public void endElement(String uri, String local, String raw) {
			if (ELEM_TRANSFORMATION.equals(raw)) {
				// get list of XMI transformations for the current XMI pattern
				ArrayList<XMITransformation> transList = transformations
						.get(currentTrans.getXMIPattern());
				// new XMI pattern, create its lists first
				if (transList == null) {
					transList = new ArrayList<XMITransformation>();
					transformations
							.put(currentTrans.getXMIPattern(), transList);
				}

				// add conditional transformation at the beginning of the list,
				// unconditional transformations at the end (to be checked
				// last).
				if (currentTrans.getConditionExpression() == null)
					transList.add(currentTrans);
				else
					transList.add(0, currentTrans);
				currentTrans = null;
			}
		}

		/**
		 * Calculates trigger inheritance after all transformations have been
		 * read.
		 */
		@Override
		public void endDocument() {
			Set<XMITransformation> processedTransformations = new HashSet<XMITransformation>();
			Iterator<XMITransformation> it = getXMITransIterator();
			while (it.hasNext())
				insertInheritedTransformations(it.next(),
						processedTransformations);
		}

		/**
		 * Recursively adds the triggers for inherited metamodel element
		 * attributes to an XMI transformation.
		 * 
		 * @param trans The XMI transformation to process.
		 * @param processedTransformations The set of transformations already
		 *        processed
		 */
		private void insertInheritedTransformations(XMITransformation trans,
				Set<XMITransformation> processedTransformations) {
			// make sure each XMI transformation is processed only once
			if (processedTransformations.contains(trans))
				return;
			processedTransformations.add(trans);

			// Find the XMI transformation for the parent metamodel element type
			MetaModelElement parentType = trans.getType().getParent();
			if (parentType == null)
				return;
			XMITransformation parentTrans = findXMITransformation(parentType);
			if (parentTrans == null)
				return;

			// recursively calculate inherited triggers for parent first
			insertInheritedTransformations(parentTrans,
					processedTransformations);

			// Find all of the parent's triggers the child does not yet have
			List<XMITrigger> missingTriggers = new ArrayList<XMITrigger>();
			for (XMITrigger trigger : parentTrans.getTriggerList())
				if (!trans.hasTrigger(trigger.name))
					missingTriggers.add(trigger);

			// Add the missing triggers to the child XMITransformation
			for (XMITrigger trg : missingTriggers)
				trans.addTrigger(trg);
		}

		/**
		 * Finds an XMI transformation for a metamodel element type. If there
		 * are multiple XMI transformations for the type, the method arbitrarily
		 * chooses one to return.
		 * 
		 * @param type Element type of interest
		 * @return An XMI transformation for the given type, or
		 *         <code>null</code> if none was found.
		 */
		private XMITransformation findXMITransformation(MetaModelElement type) {
			Iterator<XMITransformation> it = getXMITransIterator();
			while (it.hasNext()) {
				XMITransformation trans = it.next();
				if (type == trans.getType())
					return trans;
			}
			return null;
		}

		/**
		 * Returns an iterator that yields all XMITransformation objects read so
		 * far.
		 * 
		 * @return Iterator over all XMITransformation objects
		 */
		private Iterator<XMITransformation> getXMITransIterator() {
			return new MappedCollectionsIterator<XMITransformation>(
					transformations);
		}
	}
}
