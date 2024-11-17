package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.ComponentInstantiation;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class Participation extends Identified {

	private URI role;
	private ComponentInstantiation participant;
	
	/**
	 * 
	 * @param identity an identity for the participation
	 * @param role a role for the participation
	 * @param participant a participant component instantiation for the participation
	 */
	public Participation(URI identity, URI role, ComponentInstantiation participant) {
		super(identity);
		this.role = role;
		this.participant = participant;
	}

	/**
	 * @return the participation's role
	 */
	public URI getRole() {
		return role;
	}

	/**
	 * 
	 * @return the participation's participant component instantiation
	 */
	public ComponentInstantiation getParticipant() {
		return participant;
	}
	
}
