package org.sbolstandard.core2;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sbolstandard.core2.abstract_classes.Documented;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 * @version 2.0
 */

public class Component extends TopLevel {

	private URI type;
	private URI roles;
	private List<StructuralInstantiation> structuralInstantiations;
	private List<Structure> structure;
	private List<StructuralAnnotation> structuralAnnotations;
	private List<StructuralConstraint> structuralConstraints;
	
	public Component(URI identity, URI persistentIdentity, String version,
			String displayId, String name, String description,
			URI type, URI roles) {
		super(identity, persistentIdentity, version, displayId, name, description);
		this.type = type;
		this.roles = roles;
		this.structuralInstantiations = new ArrayList<StructuralInstantiation>(); 
		this.structure = new ArrayList<Structure>();
		this.structuralAnnotations = new ArrayList<StructuralAnnotation>();
		this.structuralConstraints = new ArrayList<StructuralConstraint>();
	}

	public URI getType() {
		return type;
	}

	public void setType(URI type) {
		this.type = type;
	}

	public URI getRoles() {
		return roles;
	}

	public void setRoles(URI roles) {
		this.roles = roles;
	}

	public List<StructuralInstantiation> getStructuralInstantiations() {
		return structuralInstantiations;
	}

	public void setStructuralInstantiations(
			List<StructuralInstantiation> structuralInstantiations) {
		this.structuralInstantiations = structuralInstantiations;
	}

	public List<Structure> getStructure() {
		return structure;
	}

	public void setStructure(List<Structure> structure) {
		this.structure = structure;
	}

	public List<StructuralAnnotation> getStructuralAnnotations() {
		return structuralAnnotations;
	}

	public void setStructuralAnnotations(
			List<StructuralAnnotation> structuralAnnotations) {
		this.structuralAnnotations = structuralAnnotations;
	}

	public List<StructuralConstraint> getStructuralConstraints() {
		return structuralConstraints;
	}

	public void setStructuralConstraints(
			List<StructuralConstraint> structuralConstraints) {
		this.structuralConstraints = structuralConstraints;
	}

	
//	private URI type;
//	private Collection<ComponentInstantiation> subComponentInstantiations;
//	private Collection<Port> ports;
//	
//	/**
//	 * 
//	 * @param identity an identity for the component
//	 * @param displayId a display ID for the component
//	 * @param type a type for the component
//	 */
//	public Component(URI identity, String displayId, URI type) {
//		super(identity, displayId);
//		this.type = type;
//	}
//	

//	/**
//	 * 
//	 * @return the component's type
//	 */
//	public URI getType() {
//		return type;
//	}
//	
//	/**
//	 * 
//	 * @return a collection of the component's subcomponent instantiations
//	 */
//	public Collection<ComponentInstantiation> getSubComponentInstantiations() {
//		return subComponentInstantiations;
//	}
//
//	/**
//	 * 
//	 * @param subComponentInstantiation a subcomponent instantiation for the component
//	 */
//	public void addSubComponentInstantiation(ComponentInstantiation subComponentInstantiation) {
//		subComponentInstantiations.add(subComponentInstantiation);
//	}
//	
//	/**
//	 * 
//	 * @return a collection of the component's ports
//	 */
//	public Collection<Port> getPorts() {
//		return ports;
//	}
//	
//	/**
//	 * 
//	 * @param port a port for the component
//	 */
//	public void addPort(Port port) {
//		ports.add(port);
//	}

}
