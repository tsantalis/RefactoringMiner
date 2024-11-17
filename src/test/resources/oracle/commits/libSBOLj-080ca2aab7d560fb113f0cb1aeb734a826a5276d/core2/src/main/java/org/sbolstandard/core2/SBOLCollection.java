package org.sbolstandard.core2;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class SBOLCollection extends Documented {

	private Collection<Identified> elements;

	/**
	 * 
	 * @param identity an identity for the SBOL collection
	 * @param displayId a display ID for the SBOL collection
	 */
	public SBOLCollection(URI identity, String displayId) {
		super(identity, displayId);
		elements = new HashSet<Identified>();
	}
	
	/**
	 * 
	 * @return a collection of the SBOL collection's elements
	 */
	public Collection<Identified> getElements() {
		return elements;
	}
	
	/**
	 * 
	 * @param element an identified element for the SBOL collection
	 */
	public void addElement(Identified element) {
		elements.add(element);
	}

}
