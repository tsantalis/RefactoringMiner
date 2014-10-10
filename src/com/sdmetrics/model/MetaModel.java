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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sdmetrics.util.SAXHandler;

/**
 * Represents an SDMetrics metamodel. The metamodel defines the available model
 * element types and their attributes.
 * <p>
 * The class parses the metamodel definition file, manages the set of
 * {@link MetaModelElement} instances that constitute the metamodel, and
 * provides access to them.
 */
public class MetaModel implements Iterable<MetaModelElement> {

	/** The name of the top level XML element in the metamodel definition file. */
	public final static String TL_ELEMENT = "sdmetricsmetamodel";
	/** The name of the base element type. */
	public final static String BASE_ELEMENT = "sdmetricsbase";

	/**
	 * Directory of the element types that constitute this metamodel. Allows
	 * lookup of the element types by their name.
	 */
	private Map<String, MetaModelElement> elementMap = new LinkedHashMap<String, MetaModelElement>();

	/**
	 * Retrieves an iterator over the element types in this metamodel. Types are
	 * returned in the order in which they were added/defined in the metamodel
	 * definition file.
	 * 
	 * @return Metamodel element iterator
	 */
	@Override
	public Iterator<MetaModelElement> iterator() {
		return elementMap.values().iterator();
	}

	/**
	 * Gets a SAX handler to populate this metamodel with the contents of a
	 * metamodel definition file.
	 * 
	 * @return SAX handler to parse the metamodel definition file
	 */
	public DefaultHandler getSAXParserHandler() {
		return new MetaModelParser();
	}

	/**
	 * Retrieves a metamodel element by its type name.
	 * 
	 * @param typeName The type name of the metamodel element.
	 * @return The metamodel element with that name or <code>null</code> if
	 *         there is no element of that name.
	 */
	public MetaModelElement getType(String typeName) {
		return elementMap.get(typeName);
	}

	/**
	 * Gets the number of element types defined by this metamodel.
	 * 
	 * @return The number of types in this metamodel.
	 */
	int getNumberOfTypes() {
		return elementMap.size();
	}

	/**
	 * SAX handler to parse a metamodel definition file.
	 */
	private class MetaModelParser extends SAXHandler {
		// XML element and attribute names of the metamodel definition
		private final static String ELEM_MODELELEMENT = "modelelement";
		private final static String ATTR_NAME = "name";
		private final static String ATTR_PARENT = "parent";
		private final static String ELEM_ATTRIBUTE = "attribute";
		private final static String ATTR_TYPE = "type";
		private final static String ATTR_MULTIPLICITY = "multiplicity";

		private MetaModelElement currentMME = null;
		private String currentAttributeName = null;

		/** Clear the metamodel at the beginning of the XML document. */
		@Override
		public void startDocument() {
			elementMap.clear();
		}

		/**
		 * Process an XML element in the metamodel definition file. Extracts and
		 * stores the metamodel element or attribute information.
		 * 
		 * @throws SAXException The XML file contains something other than model
		 *         element and attribute definitions.
		 */
		@Override
		public void startElement(String uri, String local, String raw,
				Attributes attrs) throws SAXException {
			if (TL_ELEMENT.equals(raw)) {
				checkVersion(attrs, null);
			} else if (ELEM_MODELELEMENT.equals(raw)) {
				handleMetaModelElement(attrs);
			} else if (ELEM_ATTRIBUTE.equals(raw)) {
				handleAttributeDefinition(attrs);
			} else {
				reportError("Unexpected XML element <" + raw + ">.");
			}
		}

		/**
		 * Handles a metamodel element definition.
		 * 
		 * @param attrs XML attributes of the element
		 * @throws SAXException metamodel element definition is invalid
		 */
		private void handleMetaModelElement(Attributes attrs)
				throws SAXException {
			String typeName = attrs.getValue(ATTR_NAME);
			if (typeName == null)
				reportError("Model element is missing \"" + ATTR_NAME
						+ "\" attribute.");
			String parentName = attrs.getValue(ATTR_PARENT);
			if (parentName == null)
				parentName = BASE_ELEMENT;
			MetaModelElement parentElement = elementMap.get(parentName);
			if (parentElement == null && !BASE_ELEMENT.equals(typeName)) {
				if (BASE_ELEMENT.equals(parentName))
					reportError("The first metamodel element to be defined must be named \""
							+ BASE_ELEMENT + "\".");
				reportError("Unknown parent type \"" + parentName
						+ "\" for model element \"" + typeName + "\".");
			}

			currentMME = new MetaModelElement(typeName, parentElement);
			elementMap.put(typeName, currentMME);
			currentAttributeName = null;
		}

		/**
		 * Handles a metamodel attribute definition.
		 * 
		 * @param attrs XML attributes of the metamodel attribute
		 * @throws SAXException metamodel attribute definition is invalid
		 */
		private void handleAttributeDefinition(Attributes attrs)
				throws SAXException {
			if (currentMME == null)
				reportError("Attribute definition outside model element definition.");

			currentAttributeName = attrs.getValue(ATTR_NAME);
			if (currentAttributeName == null)
				reportError("Attribute without a name for model element \""
						+ currentMME.getName() + "\".");

			boolean isRefAttribute = "ref".equals(attrs.getValue(ATTR_TYPE));
			boolean isSetAttribute = "many".equals(attrs
					.getValue(ATTR_MULTIPLICITY));
			currentMME.addAttribute(currentAttributeName, isRefAttribute,
					isSetAttribute);
		}

		/** Registers end of metamodel element/attribute definitions. */
		@Override
		public void endElement(String uri, String local, String raw) {
			if (ELEM_MODELELEMENT.equals(raw))
				currentMME = null;
			else if (ELEM_ATTRIBUTE.equals(raw))
				currentAttributeName = null;
		}

		/**
		 * Adds description text to the current attribute of the current element
		 * type.
		 */
		@Override
		public void characters(char ch[], int start, int length) {
			if (currentMME != null && currentAttributeName != null) {
				currentMME.addAttributeDescription(currentAttributeName,
						new String(ch, start, length));
			}
		}
	}
}
