package org.sbolstandard.core2.abstract_classes;

import java.net.URI;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 * @version 2.0
 */
public abstract class Documented extends Identified {

	private String displayId;
	private String name;
	private String description;
	
	/**
	 * 
	 * @param identity an identity for the documented object
	 * @param displayID a display ID for the documented object
	 */
	public Documented(URI identity, URI persistentIdentity, String version,
				String displayId, String name, String description) {
		super(identity, persistentIdentity, version);
		this.displayId = displayId;
		this.name = name;
		this.description = description;
	}

	/**
	 * 
	 * @return the documented object's display ID
	 */
	public String getDisplayId() {
		return displayId;
	}

	/**
	 * 
	 * @return the documented object's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param name a name for the documented object
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return the documented object's description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 
	 * @param description a description for the documented object
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Created for backward compatibility to 1.1. 
	 * @param value
	 * @deprecated
	 */
	public void setDisplayId(String value) {
		this.displayId = value;
	}

}
