package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.Documented;
import org.sbolstandard.core2.ComponentInstantiation;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class Port extends Documented {

	private URI directionality;
	private ComponentInstantiation componentInstantiation;
	
	/**
	 * 
	 * @param identity an identity for the port
	 * @param displayId a display ID for the port
	 * @param componentInstantiation a component instantiation for the port
	 */
	public Port(URI identity, String displayId, ComponentInstantiation componentInstantiation) {
		super(identity, displayId);
		this.componentInstantiation = componentInstantiation;
	}
	
	/**
	 * @return the port's component instantiation
	 */
	public ComponentInstantiation getComponentInstantiation() {
		return componentInstantiation;
	}

	/**
	 * @return the port's directionality
	 */
	public URI getDirectionality() {
		return directionality;
	}

	/**
	 * @param directionality a directionality for the port
	 */
	public void setDirectionality(URI directionality) {
		this.directionality = directionality;
	}

}
