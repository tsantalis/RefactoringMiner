package org.sbolstandard.core2;

import java.net.URI;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
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
	public Documented(URI identity, String displayId) {
		super(identity);
		this.displayId = displayId;
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

}
