package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.ComponentInstantiation;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class PortMap extends Identified {

	private Port port;
	private ComponentInstantiation componentInstantiation;
	
	/**
	 * 
	 * @param identity an identity for the port map
	 * @param displayId a display ID for the port map
	 * @param componentInstantiation a component instantiation for the port map
	 */
	public PortMap(URI identity, Port port, ComponentInstantiation componentInstantiation) {
		super(identity);
		this.port = port;
		this.componentInstantiation = componentInstantiation;
	}

	/**
	 * @return the port map's port
	 */
	public Port getPort() {
		return port;
	}

	/**
	 * @return the port map's component instantiation
	 */
	public ComponentInstantiation getComponentInstantiation() {
		return componentInstantiation;
	}

}

