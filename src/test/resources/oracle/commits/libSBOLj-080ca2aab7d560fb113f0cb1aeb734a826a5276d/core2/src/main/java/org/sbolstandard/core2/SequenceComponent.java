package org.sbolstandard.core2;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;

import org.sbolstandard.core2.Component;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class SequenceComponent extends Component {

	private Collection<URI> sequenceTypes;
	private Sequence sequence;
	private Collection<SequenceAnnotation> sequenceAnnotations;
	
	/**
	 * 
	 * @param identity an identity for the sequence component
	 * @param displayId a display ID for the sequence component
	 * @param type a type for the sequence component
	 */
	public SequenceComponent(URI identity, String displayId, URI type) {
		super(identity, displayId, type);
		this.sequenceTypes = new HashSet<URI>();
		this.sequenceAnnotations = new ArrayList<SequenceAnnotation>();
	}

	/**
	 * 
	 * @return a collection of the sequence component's sequence types
	 */
	public Collection<URI> getSequenceTypes() {
		return sequenceTypes;
	}
	
	/**
	 * 
	 * @param sequenceType a sequence type for the sequence component
	 */
	public void addSequenceType(URI sequenceType) {
		sequenceTypes.add(sequenceType);
	}

	/**
	 * 
	 * @return the sequence component's sequence
	 */
	public Sequence getSequence() {
		return sequence;
	}

	/**
	 * 
	 * @param sequence a sequence for the sequence component
	 */
	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}
	
	/**
	 * 
	 * @return a collection of the sequence component's sequence annotations
	 */
	public Collection<SequenceAnnotation> getSequenceAnnotations() {
		return sequenceAnnotations;
	}
	
	/**
	 * 
	 * @param sequenceAnnotation a sequence annotation for the sequence component
	 */
	public void addSequenceAnnotation(SequenceAnnotation sequenceAnnotation) {
		sequenceAnnotations.add(sequenceAnnotation);
	}
	
}
