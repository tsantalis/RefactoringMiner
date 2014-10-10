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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Represents a model element of a model. A model element stores the values of
 * its attributes (which includes outgoing references to other model elements),
 * and keeps track of incoming references from other model elements.
 */
public class ModelElement {
	/** Initial size of a hash map that probably will not carry many elements. */
	private static final int INITIAL_FEW_ELEMENTS = 0;
	/** Separator string used to build fully qualified element names. */
	private static final String QUALIFIER_SEPARATOR = ".";

	/** The type of this element. */
	private MetaModelElement type;
	/** The values of the attributes of this model element. */
	private Object[] attributeValues;
	/**
	 * Model elements have a running ID to preserve the order in which the
	 * elements are defined in the XMI source file. Elements with higher IDs
	 * were defined after elements with lower IDs.
	 */
	private int runningID;
	/**
	 * Map of elements that have cross-reference attributes pointing to this
	 * element. Key is the name of the cross-reference attribute, value is the
	 * set of elements pointing to this element with that cross-reference
	 * attribute.
	 */
	private HashMap<String, Collection<ModelElement>> relations;
	/**
	 * Indicates if cross-references to this element should be ignored according
	 * to the element filter settings.
	 */
	private boolean ignoreLinks = false;

	/**
	 * Creates a new element.
	 * 
	 * @param type The element type of the model element.
	 */
	public ModelElement(MetaModelElement type) {
		this.type = type;
		Collection<String> attributeList = type.getAttributeNames();
		// Initialize the attribute values of the element to empty strings/sets
		attributeValues = new Object[attributeList.size()];
		for (String attrName : attributeList)
			if (!type.isSetAttribute(attrName))
				attributeValues[type.getAttributeIndex(attrName)] = "";
	}

	/**
	 * Registers a model element that has a cross-reference attribute pointing
	 * to this model element.
	 * 
	 * @param relationName Name of the cross-reference attribute.
	 * @param source The model element pointing to this element.
	 */
	void addRelation(String relationName, ModelElement source) {
		if (relations == null) {
			relations = new HashMap<String, Collection<ModelElement>>(
					INITIAL_FEW_ELEMENTS);
		}
		Collection<ModelElement> relset = relations.get(relationName);
		if (relset == null) {
			// no relations of relationName yet, install set
			relset = new HashSet<ModelElement>(INITIAL_FEW_ELEMENTS);
			relations.put(relationName, relset);
		}
		relset.add(source);
	}

	/**
	 * Returns the set of model elements that point to this model element via a
	 * specified cross-reference attribute.
	 * 
	 * @param relationName Name of the cross-reference attribute.
	 * @return The set of model elements that point to this model element with
	 *         the specified cross-reference attribute. May be <code>null</code>
	 *         if there are no such referencing model elements.
	 */
	public Collection<ModelElement> getRelations(String relationName) {
		if (relations != null)
			return relations.get(relationName);
		return null;
	}

	/**
	 * Sets the value of an attribute for this model element. If the attribute
	 * is multi-valued, the string will be added to the set.
	 * 
	 * @param attrName Name of the attribute.
	 * @param value Value of the attribute.
	 */
	@SuppressWarnings("unchecked")
	void setAttribute(String attrName, String value) {
		int index = type.getAttributeIndex(attrName);
		if (type.isSetAttribute(attrName)) {
			@SuppressWarnings("rawtypes")
			Collection set = ((Collection) attributeValues[index]);
			if (set == null) {
				set = new HashSet<Object>(INITIAL_FEW_ELEMENTS);
				attributeValues[index] = set;
			}
			set.add(value);
		} else {
			attributeValues[index] = value;
		}
	}

	/**
	 * Sets the target model element of a single-valued cross-reference
	 * attribute.
	 * 
	 * @param attrName Name of the cross-reference attribute.
	 * @param target The referenced model element
	 */
	void setRefAttribute(String attrName, ModelElement target) {
		attributeValues[type.getAttributeIndex(attrName)] = target;
	}

	/**
	 * Sets the value of a multi-valued attribute.
	 * 
	 * @param attrName Name of the multi-valued attribute.
	 * @param set The collection of values for the attribute
	 */
	void setSetAttribute(String attrName, Collection<?> set) {
		attributeValues[type.getAttributeIndex(attrName)] = set;
	}

	/**
	 * Retrieves the value of a single-valued data attribute for this model
	 * element.
	 * 
	 * @param attrName Name of the attribute.
	 * @return Value of the attribute.
	 */
	public String getPlainAttribute(String attrName) {
		return (String) attributeValues[type.getAttributeIndex(attrName)];
	}

	/**
	 * Retrieves the model element referenced by a single-valued cross-reference
	 * attribute.
	 * 
	 * @param attrName Name of the cross-reference attribute.
	 * @return the referenced model element, or <code>null</code> if the
	 *         reference is empty or the reference should be ignored as per
	 *         filter settings.
	 */
	public ModelElement getRefAttribute(String attrName) {
		ModelElement me = (ModelElement) attributeValues[type
				.getAttributeIndex(attrName)];
		if (me != null && me.ignoreLinks)
			return null;
		return me;
	}

	/**
	 * Retrieves the set of values for a multi-valued attribute. For
	 * cross-reference attributes, this is a collection of model elements. For
	 * data attributes, the collection contains strings.
	 * 
	 * @param attrName Name of the multi-valued attribute.
	 * @return Collection of model elements or strings stored by the attribute.
	 */
	public Collection<?> getSetAttribute(String attrName) {
		Collection<?> result = (Collection<?>) attributeValues[type
				.getAttributeIndex(attrName)];
		if (result == null)
			return Collections.EMPTY_SET;
		return result;
	}

	/**
	 * Tests whether cross-references to this element should be ignored
	 * according to the element filter settings.
	 * 
	 * @return <code>true</code> if the cross-references to this element should
	 *         be ignored
	 */
	public boolean getLinksIgnored() {
		return ignoreLinks;
	}

	/**
	 * Marks whether links to this element are to be ignored according to the
	 * filter settings or not.
	 * 
	 * @param ignore <code>true</code> if the cross-references to this element
	 *        are to be ignored, else <code>false</code>.
	 */
	void setLinksIgnored(boolean ignore) {
		ignoreLinks = ignore;
	}

	/**
	 * Returns the metamodel element type of this model element.
	 * 
	 * @return The type of this element
	 * */
	public MetaModelElement getType() {
		return type;
	}

	/**
	 * Sets the running ID of this model element.
	 * 
	 * @param id The running ID
	 */
	void setRunningID(int id) {
		runningID = id;
	}

	/**
	 * Gets the fully qualified name of this model element. This is the path to
	 * the model in the containment hierarchy, with the names of the owner
	 * elements separated by dots.
	 * 
	 * @return Fully qualified name of this model element
	 */
	public String getFullName() {
		StringBuilder sb = new StringBuilder();
		ModelElement currentElement = this;
		while (currentElement != null) {
			String name = currentElement
					.getPlainAttribute(MetaModelElement.NAME);
			sb.insert(0, name);
			currentElement = currentElement.getOwner();
			if (currentElement != null) {
				sb.insert(0, QUALIFIER_SEPARATOR);
			}
		}

		return sb.toString();
	}

	/**
	 * Gets the owner of this model element.
	 * 
	 * @return Owner of the model element, <code>null</code> for root model
	 *         elements.
	 */
	public ModelElement getOwner() {
		return (ModelElement) attributeValues[type
				.getAttributeIndex(MetaModelElement.CONTEXT)];
	}

	/**
	 * Gets the model elements owned by this element.
	 * 
	 * @return A collection of all that this model element owns.
	 */
	public Collection<ModelElement> getOwnedElements() {
		return getRelations(MetaModelElement.CONTEXT);
	}

	/**
	 * Gets the XMI ID of this model element.
	 * 
	 * @return XMI ID of the model element.
	 */
	public String getXMIID() {
		return (String) attributeValues[type
				.getAttributeIndex(MetaModelElement.ID)];
	}

	/**
	 * Gets the name of this model element.
	 * 
	 * @return The unqualified name of the model element
	 */
	public String getName() {
		return (String) attributeValues[type
				.getAttributeIndex(MetaModelElement.NAME)];
	}

	/** Returns the XMI ID of the model element as its string representation. */
	@Override
	public String toString() {
		return String.valueOf(getXMIID());
	}

	/**
	 * Returns a comparator to sort model elements by the order in which they
	 * are defined in the XMI file.
	 * 
	 * @return Model element comparator
	 */
	public static Comparator<ModelElement> getComparator() {
		return new ElementOrderComparator();
	}

	/**
	 * Compares running IDs of the model elements.
	 */
	private static class ElementOrderComparator implements
			Comparator<ModelElement>, Serializable {
		private static final long serialVersionUID = -7690366551032834958L;

		@Override
		public int compare(ModelElement e1, ModelElement e2) {
			int id1 = e1.runningID;
			int id2 = e2.runningID;
			return (id1 < id2) ? -1 : ((id1 == id2) ? 0 : 1);
		}
	}
}
