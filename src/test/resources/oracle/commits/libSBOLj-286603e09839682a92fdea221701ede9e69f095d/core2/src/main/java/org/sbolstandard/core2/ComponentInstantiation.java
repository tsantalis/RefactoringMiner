package org.sbolstandard.core2;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.sbolstandard.core2.abstract_classes.Documented;

public class ComponentInstantiation extends Documented {
	
	private AccessType access;
	private List<Component> instantiatedComponent;
	private List<RefersTo> references;
	
	public ComponentInstantiation(URI identity, URI persistentIdentity,
			String version, String displayId, String name, String description,
			AccessType access) {
		super(identity, persistentIdentity, version, displayId, name, description);
		this.access = access;	
		this.instantiatedComponent = new ArrayList<Component>();
		this.references = new ArrayList<RefersTo>();
	}
	
	public AccessType getAccess() {
		return access;
	}

	public void setAccess(AccessType access) {
		this.access = access;
	}

	public List<Component> getInstantiatedComponent() {
		return instantiatedComponent;
	}

	public void setInstantiatedComponent(List<Component> instantiatedComponent) {
		this.instantiatedComponent = instantiatedComponent;
	}

	public List<RefersTo> getReferences() {
		return references;
	}

	public void setReferences(List<RefersTo> references) {
		this.references = references;
	}

	@Override
	public <T extends Throwable> void accept(SBOLVisitor<T> visitor) throws T {
		// TODO Auto-generated method stub
	}
	
//	private Component instantiatedComponent;
//	
//	/**
//	 * 
//	 * @param identity an identity for the component instantiation
//	 * @param displayId a display ID for the component instantiation
//	 * @param instantiatedComponent the component to be instantiated
//	 */
//	public ComponentInstantiation(URI identity, String displayId, 
//			Component instantiatedComponent) {
//		super(identity, displayId);
//		this.instantiatedComponent = instantiatedComponent;
//	}
//	
//	/**
//	 * 
//	 * @return the instantiated component
//	 */
//	public Component getInstantiatedComponent() {
//		return instantiatedComponent;
//	}
	
}
