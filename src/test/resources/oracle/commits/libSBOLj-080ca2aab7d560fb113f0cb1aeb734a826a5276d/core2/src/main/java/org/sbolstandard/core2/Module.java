package org.sbolstandard.core2;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class Module extends Documented {

	private Collection<ComponentInstantiation> componentInstantiations;
	private Collection<ModuleInstantiation> subModuleInstantiations;
	private Collection<Model> models;
	private Collection<Interaction> interactions;
	private Collection<Context> contexts;
	private Collection<Port> ports;
	
	/**
	 * 
	 * @param identity an identity for the module
	 * @param displayId a display ID for the module
	 */
	public Module(URI identity, String displayId) {
		super(identity, displayId);
		this.componentInstantiations = new HashSet<ComponentInstantiation>();
		this.subModuleInstantiations = new HashSet<ModuleInstantiation>();
		this.contexts = new HashSet<Context>();
		this.models = new HashSet<Model>();
		this.ports = new HashSet<Port>();
		this.interactions = new HashSet<Interaction>();		
	}

	/**
	 * @return a collection of the module's component instantiations
	 */
	public Collection<ComponentInstantiation> getComponentInstantiations() {
		return componentInstantiations;
	}

	/**
	 * @param componentInstantiation a component instantiation for the module
	 */
	public void addComponentInstantiation(ComponentInstantiation componentInstantiation) {
		componentInstantiations.add(componentInstantiation);
	}

	/**
	 * @return a collection of the module's submodule instantiations
	 */
	public Collection<ModuleInstantiation> getSubModuleInstantiations() {
		return subModuleInstantiations;
	}

	/**
	 * @param subModuleInstantiation a submodule instantiation for the module
	 */
	public void addSubModuleInstantiation(ModuleInstantiation subModuleInstantiation) {
		subModuleInstantiations.add(subModuleInstantiation);
	}

	/**
	 * @return a collection of the module's models
	 */
	public Collection<Model> getModels() {
		return models;
	}

	/**
	 * @param model a model for the module
	 */
	public void addModel(Model model) {
		models.add(model);
	}
	
	/**
	 * @return a collection of the module's interactions
	 */
	public Collection<Interaction> getInteractions() {
		return interactions;
	}

	/**
	 * @param interaction an interaction for the module
	 */
	public void addInteraction(Interaction interaction) {
		interactions.add(interaction);
	}
	
	/**
	 * @return a collection of the module's contexts
	 */
	public Collection<Context> getContexts() {
		return contexts;
	}

	/**
	 * @param context a context for the module
	 */
	public void addContext(Context context) {
		contexts.add(context);
	}

	/**
	 * @return a collection of the module's ports
	 */
	public Collection<Port> getPorts() {
		return ports;
	}

	/**
	 * @param port a port for the module
	 */
	public void addPort(Port port) {
		ports.add(port);
	}
	
	

}
