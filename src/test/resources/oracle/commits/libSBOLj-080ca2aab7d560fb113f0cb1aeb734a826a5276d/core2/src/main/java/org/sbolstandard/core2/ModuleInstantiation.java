package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.Instantiation;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class ModuleInstantiation extends Instantiation {

	private Module instantiatedModule;
	
	/**
	 * @param identity an identity for the module instantiation
	 * @param displayId a display ID for the module instantiation
	 * @param instantiatedModule the module to be instantiated
	 */
	public ModuleInstantiation(URI identity, String displayId, Module instantiatedModule) {
		super(identity, displayId);
		this.instantiatedModule = instantiatedModule;
	}

	/**
	 * 
	 * @return the instantiated module
	 */
	public Module getInstantiatedModule() {
		return instantiatedModule;
	}

}
