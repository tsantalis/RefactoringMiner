package org.sbolstandard.core2.abstract_classes;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.sbolstandard.core2.Annotation;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 * @version 2.0
 */
public abstract class Identified extends SBOLVisitable{
	
	private URI identity;
	private URI persistentIdentity;
	private String version;
	private Timestamp timeStamp;
	private List<Annotation> annotations;
	
	public Identified(URI identity, URI persistentIdentity, String version) {
		this.identity = identity;
		this.persistentIdentity = persistentIdentity;
		this.version = version;
		this.timeStamp = new Timestamp(Calendar.getInstance().getTime().getTime());
		this.annotations = new ArrayList<Annotation>();
	}

	public URI getIdentity() {
		return identity;
	}

	public void setIdentity(URI identity) {
		this.identity = identity;
	}

	public URI getPersistentIdentity() {
		return persistentIdentity;
	}

	public void setPersistentIdentity(URI persistentIdentity) {
		this.persistentIdentity = persistentIdentity;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}
	
	
//	public Identified() {
//		
//	}

//	private URI identity;
//	private String annotation;
//
//	/**
//	 * 
//	 * @param identity an identity for the identified object
//	 */
//	public Identified(URI identity) {
//		this.identity = identity;
//	}
//	
//	public Identified() {		
//	}
//
//	/**
//	 * 
//	 * @return the identified object's identity
//	 */
//	public URI getIdentity() {
//		return identity;
//	}
//	
//	/**
//	 * @return
//	 * @deprecated As of release 2.0, replaced by {@link #getIdentity()}
//	 */
//	public URI getURI() {
//		return identity;
//	}
//
//	/**
//	 * 
//	 * @return the identified object's annotation
//	 */
//	public String getAnnotation() {
//		return annotation;
//	}
//
//	/**
//	 * 
//	 * @param annotation an annotation for the identified object
//	 */
//	public void setAnnotation(String annotation) {
//		this.annotation = annotation;
//	}
//	
//	/**
//	 * 
//	 * Sets the identifier for this object.	 
//	 * @param value
//	 * @deprecated As of release 2.0, URI can only be set when an Identified instance is created.
//	 */
//	public void setURI(URI value) {
//		this.identity = value;
//	}
	
	
	
}
