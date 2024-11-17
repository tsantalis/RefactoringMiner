package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.Documented;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class Context extends Documented {

	private URI type;
	
	/**
	 * 
	 * @param identity an identity for the context
	 * @param displayId a display ID for the context
	 * @param type a type for the context
	 */
	
	public Context(URI identity, String displayId, URI type) {
		super(identity, displayId);
		this.type = type;		
	}

	/**
	 * @return the context's type
	 */
	public URI getType() {
		return type;
	}

}
