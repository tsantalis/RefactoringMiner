package org.sbolstandard.core2;

import java.net.URI;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public abstract class Identified {
	
	private URI identity;
	private String annotation;

	/**
	 * 
	 * @param identity an identity for the identified object
	 */
	public Identified(URI identity) {
		this.identity = identity;
	}
	
	/**
	 * 
	 * @return the identified object's identity
	 */
	public URI getIdentity() {
		return identity;
	}

	/**
	 * 
	 * @return the identified object's annotation
	 */
	public String getAnnotation() {
		return annotation;
	}

	/**
	 * 
	 * @param annotation an annotation for the identified object
	 */
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	
}
