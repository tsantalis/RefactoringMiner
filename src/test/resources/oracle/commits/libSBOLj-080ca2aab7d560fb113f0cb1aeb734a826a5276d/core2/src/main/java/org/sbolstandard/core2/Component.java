package org.sbolstandard.core2;
import java.net.URI;
import java.util.Collection;

import org.sbolstandard.core2.Documented;
import org.sbolstandard.core2.Port;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */

public abstract class Component extends Documented {
	
	private URI type;
	private Collection<ComponentInstantiation> subComponentInstantiations;
	private Collection<Port> ports;
	
	/**
	 * 
	 * @param identity an identity for the component
	 * @param displayId a display ID for the component
	 * @param type a type for the component
	 */
	public Component(URI identity, String displayId, URI type) {
		super(identity, displayId);
		this.type = type;
	}
	
	/**
	 * 
	 * @return the component's type
	 */
	public URI getType() {
		return type;
	}
	
	/**
	 * 
	 * @return a collection of the component's subcomponent instantiations
	 */
	public Collection<ComponentInstantiation> getSubComponentInstantiations() {
		return subComponentInstantiations;
	}

	/**
	 * 
	 * @param subComponentInstantiation a subcomponent instantiation for the component
	 */
	public void addSubComponentInstantiation(ComponentInstantiation subComponentInstantiation) {
		subComponentInstantiations.add(subComponentInstantiation);
	}
	
	/**
	 * 
	 * @return a collection of the component's ports
	 */
	public Collection<Port> getPorts() {
		return ports;
	}
	
	/**
	 * 
	 * @param port a port for the component
	 */
	public void addPort(Port port) {
		ports.add(port);
	}

}
