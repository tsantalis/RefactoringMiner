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

import static com.sdmetrics.model.XMITrigger.TriggerType.ATTRVAL;
import static com.sdmetrics.model.XMITrigger.TriggerType.CATTRVAL;
import static com.sdmetrics.model.XMITrigger.TriggerType.CONSTANT;
import static com.sdmetrics.model.XMITrigger.TriggerType.CTEXT;
import static com.sdmetrics.model.XMITrigger.TriggerType.GCATTRVAL;
import static com.sdmetrics.model.XMITrigger.TriggerType.REFLIST;
import static com.sdmetrics.model.XMITrigger.TriggerType.XMI2ASSOC;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.sdmetrics.util.SAXHandler;

/**
 * Reads an XMI source file, processing it as specified by an XMI transformation
 * file. The UML model elements retrieved from the file are stored in a
 * {@link Model}.
 * <p>
 * 
 * In the XMI, design information for one model element is spread across a
 * number of XML elements in the XML tree. Therefore, a DOM XML parser would be
 * a reasonable choice to use for processing an XMI file. DOM parsers read the
 * entire tree and store it in memory. However, XMI files can become large, and
 * we usually only need a fraction of the information contained in an XMI file,
 * so this is not economical.
 * <p>
 * Therefore, a deliberate choice was made to use a SAX XML parser. The SAX
 * parser reads one XML element at the time, and we need to keep track of the
 * context in which an XML element is embedded. This is done using a stack
 * holding the model elements to process, and the state at which processing is
 * for each model element on the stack.
 */

public class XMIReader extends SAXHandler {

	/**
	 * Message handler for progress messages of the XMI parser. Reading a large
	 * XMI file can take some time (several tens of seconds). Therefore, the XMI
	 * reader continuously emits progress messages. You can use these messages
	 * to convey to the user that the application is still alive.
	 */
	public interface ProgressMessageHandler {
		/**
		 * Report a progress message.
		 * 
		 * @param msg Message to display.
		 */
		public void reportXMIProgress(String msg);
	}

	/** The model to store the extracted model elements. */
	private Model model;
	/** Object that knows the available XMI transformations. */
	private XMITransformations transformations;

	/** Current nesting level in the XML tree */
	private int nestingLevel;
	/** Stack of model element being processed. */
	private XMIContextStack stack;
	/** Running id for artificial XMI IDs that the reader creates. */
	private int artificialIDCounter;
	/**
	 * Temporarily stores links via the "linkbackattr" attribute. These links
	 * can be registered with the model elements only after the entire XMI file
	 * has been read and all model elements are known.
	 */
	private List<DeferredRelation> deferredRelationsList;

	/** An entry on the deferred relations list. */
	private static class DeferredRelation {
		ModelElement sourceElement;
		String targetXMIID;
		String linkBackAttr;
	}

	/** Collects text between XML element tags for "ctext" trigger. */
	private StringBuilder ctextBuffer = new StringBuilder();

	/**
	 * Hash map holding all model elements. Key is the XMI ID, value the model
	 * element. Used to collect the model elements while parsing the XMI file,
	 * and cross-referencing them by their XMI ID.
	 */
	private HashMap<String, ModelElement> xmiIDMap;

	/**
	 * Cache of string values used for attributes values of model elements. The
	 * attribute value strings in a UML model contain a fair amount of
	 * redundancy, XML parsers however will return distinct string objects for
	 * equals string. Therefore, we cache those string values during parsing and
	 * replace duplicates with the cached version.
	 */
	private HashMap<String, String> stringCache;

	/** Count of the number of model elements extracted from the XMI file. */
	private int elementsExtracted;

	/** Object to report progress messages to during parsing. */
	private ProgressMessageHandler messageHandler;

	/**
	 * @param trans The XMI transformations to use.
	 * @param model The model that will contain the extracted model elements.
	 */
	public XMIReader(XMITransformations trans, Model model) {
		this.model = model;
		this.transformations = trans;
	}

	/**
	 * Registers a message handler for the XMI reader to report progress to.
	 * 
	 * @param handler The message handler.
	 */
	public void setProgressMessageHandler(ProgressMessageHandler handler) {
		messageHandler = handler;
	}

	/**
	 * Gets the number of elements that have been extracted.
	 * 
	 * @return number of elements extracted from the XMI file
	 */
	public int getNumberOfElements() {
		return elementsExtracted;
	}

	// SAX Parser callbacks

	/** Prepare parser for new XMI file. */
	@Override
	public void startDocument() {
		stack = new XMIContextStack();
		deferredRelationsList = new LinkedList<DeferredRelation>();
		xmiIDMap = new HashMap<String, ModelElement>(2048);
		stringCache = new HashMap<String, String>(2048);
		elementsExtracted = 0;
		nestingLevel = 0;
		artificialIDCounter = 0;
	}

	/**
	 * Processes the opening tag of an XML element in the XMI source file.
	 * Depending on the parser state, this either creates a new model element,
	 * or adds information from a child or grandchild node of the XMI tree to
	 * the current model element.
	 * 
	 * @throws SAXException An error occurred during the evaluation of a
	 *         conditional XMI transformation.
	 */
	@Override
	public void startElement(String uri, String local, String raw,
			Attributes attrs) throws SAXException {
		nestingLevel++;

		if (stack.isAcceptingNewElements()) {
			// Check if this is the beginning of a new model element
			if (checkAndProcessNewElement(raw, attrs))
				return;
		}

		if (stack.isEmpty()) {
			return;
		} else if (nestingLevel == stack.getNestingLevel() + 1) {
			processXMLFirstLevelChild(raw, attrs);
		} else if (nestingLevel == stack.getNestingLevel() + 2) {
			processXMLSecondLevelChild(attrs);
		}
	}

	/**
	 * Processes text between XML element tags.
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		if (stack.activeTriggerTypeEquals(CTEXT)) {
			ctextBuffer.append(ch, start, length);
		}
	}

	/**
	 * Processes the closing tag of an XML element. If the end of the definition
	 * of the model element on the top of the stack has been reached, add this
	 * element to the {@link Model}.
	 */
	@Override
	public void endElement(String uri, String local, String raw) {
		// Check for end of XML tag relevant to "ctext" triggers
		if (nestingLevel == stack.getNestingLevel() + 1) {
			if (stack.activeTriggerTypeEquals(CTEXT)) {
				// add the character data to the current model element
				ModelElement element = stack.getModelElement();
				setModelAttributeValue(element, stack.getActiveTrigger().name,
						ctextBuffer.toString());
				ctextBuffer.setLength(0);
			}
			// any currently active trigger is expired by now.
			stack.setActiveTrigger(null);
		}

		// Check if a model element has completed and add that
		// element to the model
		if (nestingLevel == stack.getNestingLevel()) {
			ModelElement element = stack.pop();

			// if element is its own "owner" (Visio XMI exporter), delete its
			// context info
			String contextID = element
					.getPlainAttribute(MetaModelElement.CONTEXT);
			if (contextID.equals(element.getXMIID())) {
				contextID = "";
			}

			// if element has no context info, insert id of owner (element below
			// on the stack)
			if (contextID.length() == 0) {
				ModelElement parent = stack.getModelElement();
				if (parent != null)
					contextID = parent.getXMIID();
				setModelAttributeValue(element, MetaModelElement.CONTEXT,
						contextID);
			}

			// now we can add the element to the model
			addModelElement(element.getXMIID(), element);
		}

		nestingLevel--;
	}

	/**
	 * Performs the post-processing after the entire XMI file has been read.
	 * Process the elements on the deferredRelations list, and cross-references
	 * all model element links.
	 */
	@Override
	public void endDocument() {
		// Now that all model elements are known, we can process deferred
		// relations
		for (DeferredRelation dRel : deferredRelationsList) {
			ModelElement target = xmiIDMap.get(dRel.targetXMIID);
			if (target == null)
				continue; // referenced element does not exist

			// set attribute of target element to point back to the source
			if (target.getType().hasAttribute(dRel.linkBackAttr))
				setModelAttributeValue(target, dRel.linkBackAttr,
						dRel.sourceElement.getXMIID());
		}

		// The string cache and deferred relations list have served their
		// purpose and can be gc'ed
		stringCache = null;
		deferredRelationsList = null;

		// Now we can do the cross-referencing of model elements
		doCrossreferencing();
	}

	/**
	 * Checks if an XML element in the XMI file defines a new model element, and
	 * process it accordingly.
	 * 
	 * @param xmlElement Name of the XML element.
	 * @param attrs The attributes of the XML element.
	 * @return <code>true</code> if a new model element was created, else
	 *         <code>false</code>.
	 * @throws SAXException An error occurred evaluating the condition
	 *         expression of a conditional XMI transformation.
	 */
	private boolean checkAndProcessNewElement(String xmlElement,
			Attributes attrs) throws SAXException {
		XMITransformation trans = transformations.getTransformation(xmlElement,
				attrs);

		if (trans == null) {
			// Check if current element has a "xmi2assoc" trigger where
			// xmlElement=trigger.attr,
			// For example:
			// XMI Source: <ownedAttribute xmi:id='xmi.43' name='attr1'
			// type='xmi.2001'/>
			// Candidate Trigger: <trigger name="properties" type="xmi2assoc"
			// attr="ownedAttribute" src="UML:Property"/>
			XMITransformation currentTrans = stack.getXMITransformation();
			if (currentTrans == null)
				return false;
			for (XMITrigger trigger : currentTrans.getTriggerList()) {
				if (trigger.type == XMI2ASSOC
						&& trigger.attr.equals(xmlElement)) {
					String xmiPattern = null;
					// if xmi:type or xsi:type is specified in the XML file, use
					// it, else use trigger.src
					int xmiTypeIndex = findAttributeIndex(attrs, "xmi:type",
							"xsi:type");
					if (xmiTypeIndex < 0) {
						if (trigger.src == null)
							return false;
						xmiPattern = trigger.src;
					} else
						xmiPattern = attrs.getValue(xmiTypeIndex);
					trans = transformations
							.getTransformation(xmiPattern, attrs);
					break;
				}
			}
		}

		if (trans == null)
			return false;

		if (trans.requiresXMIID()) {
			// Test if XML element has an XMI id specified.
			// If not, return false to ignore the element.
			if (findAttributeIndex(attrs, "xmi.id", "xmi:id") < 0)
				return false;
		} else {
			// Test if XML element has an XMI idref specified. If so, ignore the
			// element.
			if (findAttributeIndex(attrs, "xmi.idref", "xmi:idref") >= 0)
				return false;
		}

		processNewElement(trans, xmlElement, attrs);
		return true;
	}

	/**
	 * Finds the index of an XML attribute that can be specified using one of
	 * two alternative names.
	 * 
	 * @param attrs SAX attributes of an XML element
	 * @param attr1 First name used for the attribute.
	 * @param attr2 Alternative name used for the attribute.
	 * @return Index of the attribute, or -1 if the attribute is not contained
	 *         under either name
	 */
	private int findAttributeIndex(Attributes attrs, String attr1, String attr2) {
		int idindex = attrs.getIndex(attr1);
		return (idindex < 0) ? attrs.getIndex(attr2) : idindex;
	}

	/**
	 * Processes a new model element in the XMI file. Creates a new
	 * {@link ModelElement}, processes the XML elements, and pushes the new
	 * model element on the context stack.
	 * 
	 * @param trans XMI transformations to apply for the model element.
	 * @param xmlElement The name of the XML element in the XMI file.
	 * @param attrs The XML attributes of the XML element.
	 */
	private void processNewElement(XMITransformation trans, String xmlElement,
			Attributes attrs) {
		ModelElement element = new ModelElement(trans.getType());
		processAttrTriggers(element, trans, attrs);

		// if element has no XMI ID yet, add one
		String xmiID = element.getPlainAttribute(MetaModelElement.ID);
		if (xmiID.length() == 0) {
			xmiID = "SDMetricsID." + (artificialIDCounter++);
			setModelAttributeValue(element, MetaModelElement.ID, xmiID);
		}

		processRelationTriggers(xmlElement, xmiID);

		stack.push(element, trans, nestingLevel);
	}

	/**
	 * Processes triggers "attrval", "xmi2assoc" and "constant" for a new model
	 * element. These triggers can be determined from the attributes of the XML
	 * element that defines the model element.
	 * 
	 * @param element The new model element.
	 * @param trans XMI transformation to use for the new model element.
	 * @param attrs XML attributes defining the model element.
	 */
	private void processAttrTriggers(ModelElement element,
			XMITransformation trans, Attributes attrs) {
		MetaModelElement type = trans.getType();

		for (XMITrigger trigger : trans.getTriggerList()) {
			if (trigger.type == ATTRVAL || trigger.type == XMI2ASSOC) {
				// Retrieve model element attribute from an XML attribute in the
				// XMI file. For example:
				// XMI Source: <ownedMember xmi:id='xmi.42' visibility='public'
				// classifier='xmi.43 xmi.44'/>
				// Triggers: <trigger name="classifiers" type="xmi2assoc"
				// attr="classifier" />
				// <trigger name="visible" type="attrval" attr="visibility" />
				String xmlAttrValue = attrs.getValue(trigger.attr);
				if (xmlAttrValue != null) {
					if (type.isSetAttribute(trigger.name)) {
						// tokenize the XML attribute values and add each one
						StringTokenizer t = new StringTokenizer(xmlAttrValue);
						while (t.hasMoreTokens()) {
							String nextID = t.nextToken();
							setModelElementAttribute(element, nextID, trigger);
						}
					} else {
						setModelElementAttribute(element, xmlAttrValue, trigger);
					}
				}
			} else if (trigger.type == CONSTANT) {
				// Insert the constant defined in the XMI trigger as attribute
				// value
				setModelAttributeValue(element, trigger.name, trigger.attr);
			}
		}
	}

	/**
	 * Sets a cross-reference attribute value and registers the "linkbackattr"
	 * on the "deferred relations" list, if defined.
	 * 
	 * @param sourceElement The source element of the cross-reference.
	 * @param targetXMIID XMI ID of the target element.
	 * @param trigger The trigger that produced the target element XMIID.
	 */
	private void setModelElementAttribute(ModelElement sourceElement,
			String targetXMIID, XMITrigger trigger) {
		setModelAttributeValue(sourceElement, trigger.name, targetXMIID);
		if (trigger.linkback != null) {
			DeferredRelation dRel = new DeferredRelation();
			dRel.sourceElement = sourceElement;
			dRel.targetXMIID = targetXMIID;
			dRel.linkBackAttr = trigger.linkback;
			deferredRelationsList.add(dRel);
		}
	}

	/**
	 * Processes "cattrval", "gcattrval", and "xmi2assoc" triggers of the owner
	 * of a new model element.
	 * 
	 * @param xmlElement Name of the XML element defining the new model element.
	 * @param xmiID XMI ID of the new model element.
	 */
	private void processRelationTriggers(String xmlElement, String xmiID) {
		// Process any matching "cattrval" or "xmi2assoc" triggers of the
		// current model element
		if ((nestingLevel == stack.getNestingLevel() + 1) && !stack.isEmpty()) {
			for (XMITrigger trigger : stack.getXMITransformation()
					.getTriggerList()) {
				if ((trigger.type == CATTRVAL && trigger.src.equals(xmlElement))
						|| (trigger.type == XMI2ASSOC && trigger.attr
								.equals(xmlElement))) {
					// Add relation to the new element from the owner
					// model element on the stack
					setModelElementAttribute(stack.getModelElement(), xmiID,
							trigger);
				}
			}
		}

		// process active "gcattr" trigger, if any
		if ((nestingLevel == stack.getNestingLevel() + 2)
				&& stack.activeTriggerTypeEquals(GCATTRVAL)) {
			processGCATTRElement(xmiID);
		}
	}

	/**
	 * Checks an XML child element of a model element for pertinent data.
	 * 
	 * @param xmlElement The name of the XML child element to process.
	 * @param attrs The XML attributes of the XML child element.
	 */
	private void processXMLFirstLevelChild(String xmlElement, Attributes attrs) {

		// Go through list of triggers for current model element, checking if
		// one is defined for the current XMI Element.
		for (XMITrigger trigger : stack.getXMITransformation().getTriggerList()) {
			if ((trigger.type == CATTRVAL && trigger.src.equals(xmlElement))
					|| (trigger.type == XMI2ASSOC && trigger.attr
							.equals(xmlElement))) {
				// Process cattr or xmi2assoc trigger, for example:
				// XMI Source: <containedNode xmi:idref='xmi35'/>
				// Trigger: <trigger name="nodes" type="cattrval"
				// src="containedNode" attr="xmi:idref" />
				// or: <trigger name="nodes" type="xmi2assoc"
				// attr="containedNode" />
				int attrIndex;
				if (trigger.type == XMI2ASSOC) {
					attrIndex = findAttributeIndex(attrs, "xmi:idref",
							"xmi.idref");
				} else
					attrIndex = attrs.getIndex(trigger.attr);
				if (attrIndex >= 0) {
					String attrValue = attrs.getValue(attrIndex);
					setModelElementAttribute(stack.getModelElement(),
							attrValue, trigger);
				}
			} else if ((trigger.type == GCATTRVAL || trigger.type == CTEXT || trigger.type == REFLIST)
					&& trigger.src.equals(xmlElement)) {
				// Set current trigger for processing subsequent character text
				// or grandchild XML elements
				stack.setActiveTrigger(trigger);
			}
		}
	}

	/**
	 * Checks an XML grandchild element of a model element for pertinent data.
	 * 
	 * @param attrs The XML attributes of the XML grandchild element.
	 */
	private void processXMLSecondLevelChild(Attributes attrs) {
		if (stack.activeTriggerTypeEquals(GCATTRVAL)) {
			String attrValue = attrs.getValue(stack.getActiveTrigger().attr);
			processGCATTRElement(attrValue);
		} else if (stack.activeTriggerTypeEquals(REFLIST))
			processReflistElement(attrs);
	}

	/**
	 * Completes the processing of the active "gcattrval" trigger.
	 * 
	 * @param attrValue Attribute value extracted by the trigger.
	 */
	private void processGCATTRElement(String attrValue) {
		// Example XMI Source:
		// <UML:Partition' name='mySwimlane' xmi.id='xmi12'>
		// <UML:Partition.contents>
		// <UML:ModelElement xmi.idref='xmi35'/>
		// Trigger: <trigger name="contents" type="gcattrval"
		// src="UML:Partition.contents" attr="xmi.idref"/>
		XMITrigger trigger = stack.getActiveTrigger();
		MetaModelElement type = stack.getModelElement().getType();

		// Insert attribute value of interest.
		if (type.hasAttribute(trigger.name) && attrValue != null) {
			setModelElementAttribute(stack.getModelElement(), attrValue,
					trigger);
		}
		// Done processing. If attribute is single-valued, expire trigger.
		if (!type.isSetAttribute(trigger.name))
			stack.setActiveTrigger(null);
	}

	/**
	 * Processes an XML element that is a member of a "reflist". Creates a new
	 * model element that is owned by the current model element on the top of
	 * the stack.
	 * <p>
	 * Note: the reflist trigger is deprecated and only supported for backwards
	 * compatibility. New XMITransformation files should use the
	 * "[gc][c]relation" triggers.
	 * 
	 * @param attrs Attributes of the reflist element.
	 * @return <code>true</code> if the XML element was processed successfully,
	 *         <code>false</code> if the XML element was not suitable.
	 */
	private boolean processReflistElement(Attributes attrs) {
		XMITrigger trigger = stack.getActiveTrigger();
		String attrval = attrs.getValue(trigger.attr);
		if (attrval == null)
			return false;

		// Get parent of new model element from top of the stack.
		ModelElement parent = stack.getModelElement();
		String parid = parent.getPlainAttribute(MetaModelElement.ID);

		// Create the new model element.
		MetaModelElement nsetype = model.getMetaModel().getType(trigger.name);
		ModelElement nse = new ModelElement(nsetype);

		// Set the owner of the new model element.
		setModelAttributeValue(nse, MetaModelElement.CONTEXT, parid);

		// Set the cross-reference attribute to the referenced model element.
		setModelAttributeValue(nse, trigger.name, attrval);

		// New model element has no name.
		setModelAttributeValue(nse, MetaModelElement.NAME, "");

		// The XMI ID of the new model element is a concatenation of the
		// referenced model element and the parent element.
		setModelAttributeValue(nse, MetaModelElement.ID, parid + attrval);

		// Add new element to the model and return with success.
		addModelElement(nse.getPlainAttribute(MetaModelElement.ID), nse);
		return true;
	}

	/**
	 * Sets the attribute value of a model element.
	 * <p>
	 * Replaces the string value with a cached copy if the value has been used
	 * before.
	 * 
	 * @param element The model element
	 * @param attributeName Name of the attribute to set
	 * @param value Value for the attribute
	 */
	private void setModelAttributeValue(ModelElement element,
			String attributeName, String value) {
		String cachedValue = stringCache.get(value);
		if (cachedValue == null && value != null) {
			cachedValue = value;
			stringCache.put(value, value);
		}

		element.setAttribute(attributeName, cachedValue);
	}

	/**
	 * Adds a new model element to the model.
	 * 
	 * @param xmiID XMI ID of the model element.
	 * @param element The model element to add.
	 */
	private void addModelElement(String xmiID, ModelElement element) {
		element.setRunningID(elementsExtracted);
		xmiIDMap.put(xmiID, element);
		model.addElement(element);
		elementsExtracted++;

		// issue a progress message every 1000 elements
		if (messageHandler != null) {
			if ((elementsExtracted % 1000) == 0)
				messageHandler
						.reportXMIProgress("Reading UML model. Elements processed: "
								+ elementsExtracted);
		}
	}

	/**
	 * Replaces string-valued XMI IDs of cross-reference attribute values with
	 * references to the respective model element instances. Called after all
	 * model elements have been added.
	 */
	private void doCrossreferencing() {
		if (messageHandler != null)
			messageHandler.reportXMIProgress("Crossreferencing "
					+ elementsExtracted + " model elements");

		for (MetaModelElement type : model.getMetaModel()) {
			for (String attrName : type.getAttributeNames()) {
				if (type.isRefAttribute(attrName)) {
					boolean isMultivalued = type.isSetAttribute(attrName);
					for (ModelElement element : model.getElements(type)) {
						if (isMultivalued) {
							Collection<?> oldStringReferences = element
									.getSetAttribute(attrName);
							if (oldStringReferences.isEmpty())
								continue;
							HashSet<ModelElement> newElementReferences = new HashSet<ModelElement>(
									oldStringReferences.size());
							for (Object xmiID : oldStringReferences) {
								ModelElement referencedElement = xmiIDMap
										.get(xmiID);
								if (referencedElement != null) {
									referencedElement.addRelation(attrName,
											element);
									newElementReferences.add(referencedElement);
								}
							}
							element.setSetAttribute(attrName,
									newElementReferences);
						} else {
							ModelElement referencedElement = xmiIDMap
									.get(element.getPlainAttribute(attrName));
							if (referencedElement != null)
								referencedElement
										.addRelation(attrName, element);
							element.setRefAttribute(attrName, referencedElement);
						}
					}
				}
			}
		}

		// don't need the XMI id mapping anymore
		xmiIDMap = null;
	}
}
