package org.sbolstandard.core2;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 * @version 2.0
 */
public class SequenceComponent { //extends Component {

	private Collection<URI> sequenceTypes;
	private Sequence sequence;
	private Collection<StructuralAnnotation> sequenceAnnotations;
	
	/**
	 * 
	 * @param identity an identity for the sequence component
	 * @param displayId a display ID for the sequence component
	 * @param type a type for the sequence component
	 */
	public SequenceComponent(URI identity, String displayId, URI type) {
		//super(identity, displayId, type);
		this.sequenceTypes = new HashSet<URI>();
		this.sequenceAnnotations = new ArrayList<StructuralAnnotation>();		
	}

	/**
	 * @deprecated Creating an empty Sequence object is not recommended. 
	 * See {@link #SequenceComponent(URI, String, URI)}
	 */
	public SequenceComponent() {
		super();		
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
	 * DNA sequence which this DnaComponent object represents.
	 * @return 1 {@link DnaSequence} specifying the DNA sequence of this DnaComponent
	 * @see DnaSequence
	 * @deprecated As of release 2.0, replaced by {@link #getSequence()}
	 */
	public Sequence getDnaSequence() {
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
	 * DNA sequence which this DnaComponent object represents.
	 * @param dnaSequence specify the DnaSequence of this DnaComponent
	 * @deprecated As of release 2.0, replaced by {@link #setSequence(Sequence)}
	 */
	public void setDnaSequence(Sequence value) {		
		this.sequence = value;
	}
	
	/**
	 * 
	 * @return a collection of the sequence component's sequence annotations
	 */
	public Collection<StructuralAnnotation> getSequenceAnnotations() {
		return sequenceAnnotations;
	}
	
	/**
	 * Positions and directions of <code>SequenceFeature</code>[s] that describe
	 * the DNA sequence.
	 * @return 0 or more <code>SequenceAnnotation</code>[s] that describe the 
	 * DNA composition
	 * @see #addAnnotation
	 * @deprecated As of release 2.0, replaced by {@link #getSequenceAnnotations()}
	 */
	public ArrayList<StructuralAnnotation> getAnnotations() {		
		return (ArrayList<StructuralAnnotation>) this.sequenceAnnotations;
	}	
	
	/**
	 * 
	 * @param sequenceAnnotation a sequence annotation for the sequence component
	 */
	public void addSequenceAnnotation(StructuralAnnotation sequenceAnnotation) {
		sequenceAnnotations.add(sequenceAnnotation);
	}
	
	/**
     * New position and direction of a <code>SequenceFeature</code> that
     * describes the DNA sequence.
     * The DnaComponent could be left un-annotated, but that condition is not a very useful to users.
     * @param annotation a <code>SequenceAnnotation</code> that describes the DNA composition
     * @deprecated As of release 2.0, replaced by {@link #addSequenceAnnotation(StructuralAnnotation)}
     */
	public void addAnnotation(StructuralAnnotation annotation) {
		sequenceAnnotations.add(annotation);
	}
	
	/**
	 * @param annotation
	 * @deprecated
	 */
	public void removeAnnotation(StructuralAnnotation annotation) {
		sequenceAnnotations.remove(annotation);		
	}
		
    /**
     * Sequence Ontology vocabulary provides a defined term for types of DNA
     * components.
     * TO DO: implement use of SO within libSBOLj.
     * @return a Sequence Ontology (SO) vocabulary term to describe the type of DnaComponent.
     * @deprecated As of release 2.0, replaced by {@link #getSequenceTypes()}
     */
	public List<URI> getTypes() {
		return (List<URI>) sequenceTypes;
	}
	
	/**
	 * Sequence Ontology vocabulary provides a defined term for types of DNA
	 * components.
	 *
	 * @param type Sequence Ontology URI specifying the type of the DnaComponent
	 * @deprecated As of release 2.0, replaced by {@link #addSequenceType(URI)}
	 */
	public void addType(URI type) {
		sequenceTypes.add(type);
	}

//	@Override
//	public <T extends Throwable> void accept(SBOLVisitor<T> visitor) throws T {
//		
//		
//	}

//	//@XmlAccessorType(XmlAccessType.NONE)
//	public static class DnaSequenceWrapper extends WrappedValue<Sequence> {
//		//@XmlElement(name = "DnaSequence", required = true)
//		//@Override
//		public Sequence getValue() {
//			return super.getValue();
//		}
//
//		//@Override
//		public void setValue(Sequence value) {
//			super.setValue(value);
//		}
//	}
//	
//	//@XmlAccessorType(XmlAccessType.PROPERTY)
//	//@XmlType(name = "")
//	public static class URIWrapper extends WrappedValue<URI> {
//		//@XmlAttribute(name = "resource", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#", required = true)
//		//@XmlJavaTypeAdapter(URIAdapter.class)
//		//@Override
//		public URI getValue() {
//			return super.getValue();
//		}
//		
//		//@Override
//		public void setValue(URI value) {
//			super.setValue(value);
//		}
//	}
//	
//		//@XmlAccessorType(XmlAccessType.NONE)
//	public static class SequenceAnnotationWrapper extends WrappedValue<SequenceAnnotation> {
//		//@XmlElement(name = "SequenceAnnotation")
//		//@Override
//		public SequenceAnnotation getValue() {
//			return super.getValue();
//		}
//
//		//@Override
//		public void setValue(SequenceAnnotation value) {
//			super.setValue(value);
//		}
//	}
	
}
