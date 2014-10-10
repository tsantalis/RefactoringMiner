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

/** Stores information for one trigger of an XMITransformation. */
class XMITrigger {

	/** Enumerates the types of triggers. */
	static enum TriggerType {
		/** Trigger "constant". */
		CONSTANT(true, false),
		/** Trigger "attrval". */
		ATTRVAL(true, false),
		/** Trigger "cattrval". */
		CATTRVAL(true, true),
		/** Trigger "ctext". */
		CTEXT(false, true),
		/** Trigger "gcattrval". */
		GCATTRVAL(true, true),
		/** Trigger "ignore". */
		IGNORE(false, false),
		/** Trigger "xmi2assoc". */
		XMI2ASSOC(true, false),
		/** Deprecated trigger "reflist". */
		REFLIST(true, true);

		/** Indicates if the trigger require the "attr" attribute to be set. */
		final boolean requiresAttr;
		/** Indicates if the trigger require the "src" attribute to be set. */
		final boolean requiresSrc;

		private TriggerType(boolean requiresAttr, boolean requiresSrc) {
			this.requiresAttr = requiresAttr;
			this.requiresSrc = requiresSrc;
		}

		/**
		 * Sets trigger names to lower case for output.
		 */
		@Override
		public String toString() {
			return name().toLowerCase();
		}

	}

	/** Value of the name attribute of this trigger. */
	String name;

	/** The type of trigger (attrval, ctext, ...) */
	TriggerType type;

	/** The XMI element that holds the information for this trigger. */
	String src;

	/** The relevant attribute of the XMI element that holds the information. */
	String attr;

	/** Name of an attribute storing back links to a referencing element. */
	String linkback;

	/**
	 * Create a new trigger.
	 * 
	 * @param attributeName The name of the metamodel element attribute for
	 *        which this trigger is defined.
	 * @param triggerType The type of trigger.
	 * @param srcElement The XMI element that holds the information for this
	 *        trigger.
	 * @param srcAttribute The relevant attribute of the XMI element that holds
	 *        the information.
	 * @param linkBackAttribute For cross-reference attributes: the link back
	 *        attribute.
	 * @throws IllegalArgumentException Unknown trigger kind or required
	 *         attributes were missing.
	 */
	XMITrigger(String attributeName, String triggerType, String srcElement,
			String srcAttribute, String linkBackAttribute) {
		name = attributeName;
		src = srcElement;
		attr = srcAttribute;
		linkback = linkBackAttribute;

		type = null;
		try {
			type = TriggerType.valueOf(triggerType.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown trigger type '"
					+ triggerType + "'.");
		}

		if (type.requiresAttr && srcAttribute == null)
			throw new IllegalArgumentException(
					"Attribute 'attr' must be specified for triggers of type '"
							+ triggerType + "'.");

		if (type.requiresSrc && srcElement == null)
			throw new IllegalArgumentException(
					"Attribute 'src' must be specified for triggers of type '"
							+ triggerType + "'.");
	}
}
