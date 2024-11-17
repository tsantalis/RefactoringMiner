package org.sbolstandard.core2;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;

import org.sbolstandard.core2.Documented;
import org.sbolstandard.core2.ComponentInstantiation;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class SequenceAnnotation extends Documented {

	private int start;
	private int end;
	private Orientation orientation;
	private ComponentInstantiation subComponentInstantiation;
	private Collection<SequenceAnnotation> precededAnnotations;
	
	
	/**
	 * 
	 * @param identity an identity for the sequence annotation
	 * @param displayId a display ID for the sequence annotation
	 */
	public SequenceAnnotation(URI identity, String displayId) {
		super(identity, displayId);
		precededAnnotations = new HashSet<SequenceAnnotation>();
	}
	
	/**
	 * 
	 * @param identity an identity for the sequence annotation
	 * @param displayId a display ID for the sequence annotation
	 * @param start a starting position for the sequence annotation
	 * @param end an ending position for the sequence annotation
	 */
	public SequenceAnnotation(URI identity, String displayId, int start, int end) {
		this(identity, displayId);
		this.start = start;
		this.end = end;
	}

	/**
	 * 
	 * @return the sequence annotation's starting position
	 */
	public int getStart() {
		return start;
	}

	/**
	 * 
	 * @return the sequence annotation's ending position
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * 
	 * @return the sequence annotation's orientation
	 */
	public Orientation getOrientation() {
		return orientation;
	}

	/**
	 * 
	 * @param orientation an orientation for the sequence annotation
	 */
	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}
	
	/**
	 * 
	 * @return the sequence annotation's subcomponent instantiation
	 */
	public ComponentInstantiation getSubComponentInstantiation() {
		return subComponentInstantiation;
	}
	
	/**
	 * 
	 * @param subComponentInstantiation a subcomponent instantiation for the sequence annotation
	 */
	public void setSubComponentInstantiation(ComponentInstantiation subComponentInstantiation) {
		this.subComponentInstantiation = subComponentInstantiation;
	}

	/**
	 * 
	 * @return a collection of sequence annotations preceded by this sequence annotation
	 */
	public Collection<SequenceAnnotation> getPrecededAnnotations() {
		return precededAnnotations;
	}

	/**
	 * 
	 * @param precededAnnotation a preceded sequence annotation for this sequence annotation
	 */
	public void addPrecededAnnotation(SequenceAnnotation precededAnnotation) {
		precededAnnotations.add(precededAnnotation);
	}
	
	
	
}
