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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an element type of the SDMetrics metamodel.
 * <p>
 * An element type has a name, a parent element type, and a list of attributes.
 * Attributes can contain data or references to model elements, and may be
 * single-valued or multi-valued.
 */
public class MetaModelElement {
	/** Name of the attribute with the XMI ID of a model element. */
	final static String ID = "id";
	/** Name of the attribute with the name of a model element. */
	final static String NAME = "name";
	/** Name of the attribute with the owner of a model element. */
	final static String CONTEXT = "context";

	/** Name of this element type. */
	private String typeName;
	/** The parent type of this element type. */
	private MetaModelElement parent;
	/**
	 * The attributes of this element type. Allows lookup of the attributes by
	 * their name.
	 */
	private Map<String, MetaModelElementAttribute> attributes = new LinkedHashMap<String, MetaModelElementAttribute>(
			4);

	/** Stores the definition of a metamodel element attribute. */
	static class MetaModelElementAttribute {
		/** Name of the attribute. */
		String attrName;
		/** Indicates if this attribute references other model elements. */
		boolean isReference;
		/** Indicates if this is a multi-valued attribute. */
		boolean isSet;
		/** Description text of the attribute. */
		String description;
		/** Index of the attribute, for efficient array storage. */
		int index;

		/**
		 * @param name Name of the attribute
		 * @param isRef Indicates if the attribute references other model
		 *        elements
		 * @param isSet Indicates if this is a multi-valued attribute
		 * @param index Index of the attribute
		 */
		MetaModelElementAttribute(String name, boolean isRef, boolean isSet,
				int index) {
			this.attrName = name;
			this.isReference = isRef;
			this.isSet = isSet;
			this.description = "";
			this.index = index;
		}
	}

	/**
	 * Creates a element type.
	 * 
	 * @param name Name of the type.
	 * @param parent The parent of the type.
	 */
	MetaModelElement(String name, MetaModelElement parent) {
		this.typeName = name;
		if (parent != null) {
			// inherit all of the parent's attributes
			this.parent = parent;
			for (MetaModelElementAttribute parentAttribute : parent.attributes
					.values())
				attributes.put(parentAttribute.attrName, parentAttribute);
		}
	}

	/**
	 * Gets the name of this element type.
	 * 
	 * @return Name of the element type.
	 */
	public String getName() {
		return typeName;
	}

	/**
	 * Gets the parent of this element type.
	 * 
	 * @return Parent of the metamodel element, <code>null</code> if this is the
	 *         metamodel base element type.
	 */
	MetaModelElement getParent() {
		return parent;
	}

	/**
	 * Gets the attribute names of the metamodel element. This includes
	 * inherited attributes. The collection maintains the attributes in the
	 * order in which they were defined in the metamodel definition file;
	 * inherited elements are listed first.
	 * 
	 * @return Collection of attribute names
	 */
	public Collection<String> getAttributeNames() {
		return attributes.keySet();
	}

	/**
	 * Tests if this element type has an attribute of a given name.
	 * 
	 * @param name Name of the candidate attribute
	 * @return <code>true</code> if this element type has an attribute of that
	 *         name.
	 */
	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}

	/**
	 * Tests if an attribute is a cross-reference attribute.
	 * 
	 * @param name Name of the attribute to test.
	 * @return <code>true</code> if the attribute is a cross-reference
	 *         attribute, <code>false</code> if it is a data attribute.
	 * @throws IllegalArgumentException Element type has no such attribute.
	 */
	public boolean isRefAttribute(String name) {
		return getAttribute(name).isReference;
	}

	/**
	 * Tests if an attribute is multi-valued.
	 * 
	 * @param name Name of the attribute to test.
	 * @return <code>true</code> if the attribute is multi-valued,
	 *         <code>false</code> if it only stores a single value.
	 * @throws IllegalArgumentException Element type has no such attribute.
	 */
	public boolean isSetAttribute(String name) {
		return getAttribute(name).isSet;
	}

	/**
	 * Gets the description of an attribute.
	 * 
	 * @param name Name of the attribute.
	 * @return Informal description of the attribute.
	 * @throws IllegalArgumentException Element type has no such attribute.
	 */
	public String getAttributeDescription(String name) {
		return getAttribute(name).description;
	}

	/**
	 * Adds an attribute to this element type.
	 * 
	 * @param attrName Name of the attribute.
	 * @param isRef <code>true</code> if this is to be a cross-reference
	 *        attribute, <code>false</code> if it is data-valued.
	 * @param isSet <code>true</code> if this is to be a multi-valued attribute,
	 *        <code>false</code> if it is single-valued.
	 */
	void addAttribute(String attrName, boolean isRef, boolean isSet) {
		MetaModelElementAttribute attrib = new MetaModelElementAttribute(
				attrName, isRef, isSet, attributes.size());
		attributes.put(attrName, attrib);
	}

	/**
	 * Gets the index of an attribute. Each attribute of an element type has a
	 * unique index. Attribute indices go from 0 to N-1, where N is the total
	 * number of attributes of this element type.
	 * 
	 * @param name Name of the attribute
	 * @return Index of the attribute in this element type
	 * @throws IllegalArgumentException Element type has no such attribute.
	 */
	int getAttributeIndex(String name) {
		return getAttribute(name).index;
	}

	/**
	 * Adds text to the description of an attribute.
	 * 
	 * @param name Name of the attribute.
	 * @param description Text to add to the attribute's description.
	 * @throws IllegalArgumentException Element type has no such attribute.
	 */
	void addAttributeDescription(String name, String description) {
		MetaModelElementAttribute attr = getAttribute(name);
		attr.description = attr.description + description;
	}

	/**
	 * Gets the attribute definition for an attribute.
	 * 
	 * @param name Name of the attribute
	 * @return The definition of the attribute
	 * @throws IllegalArgumentException Element type has no such attribute.
	 */
	private MetaModelElementAttribute getAttribute(String name) {
		MetaModelElementAttribute attr = attributes.get(name);
		if (attr == null)
			throw new IllegalArgumentException("No attribute \"" + name
					+ "\" defined for elements of type \"" + typeName + "\".");
		return attr;
	}
}
