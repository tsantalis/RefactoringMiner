package org.sbolstandard.core2;

import java.net.URI;
import java.util.Collection;

import org.sbolstandard.core2.Documented;
import org.sbolstandard.core2.Participation;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class Interaction extends Documented {

	private URI type;
	private Collection<Participation> participations;
	
	/**
	 * 
	 * @param identity an identity for the interaction
	 * @param displayId a display ID for the interaction
	 * @param type a type for the interaction
	 * @param participations a collection of participations for the interaction
	 */
	public Interaction(URI identity, String displayId, URI type, 
			Collection<Participation> participations) {
		super(identity, displayId);
		this.type = type;
		this.participations = participations;
	}

	/**
	 * @return the interaction's type
	 */
	public URI getType() {
		return type;
	}


	/**
	 * @return the interaction's participations
	 */
	public Collection<Participation> getParticipations() {
		return participations;
	}

}
