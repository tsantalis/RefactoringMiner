package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.Documented;

/**
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 */
public class Model extends Documented {

	private URI source;
	private URI language;
	private URI framework;
	private URI role;
	
	/**
	 * 
	 * @param identity an identity for the model
	 * @param displayId a displayId for the model
	 * @param source a source for the model
	 * @param language a language for the model
	 * @param framework a framework for the model
	 * @param role a role for the model
	 */
	public Model(URI identity, String displayId, URI source, URI language, URI framework, 
			URI role) {
		super(identity, displayId);
		this.source = source;
		this.language = language;
		this.framework = framework;
		this.role = role;
	}

	/**
	 * @return the model's source
	 */
	public URI getSource() {
		return source;
	}

	/**
	 * @return the model's language
	 */
	public URI getLanguage() {
		return language;
	}

	/**
	 * @return the model's framework
	 */
	public URI getFramework() {
		return framework;
	}

	/**
	 * @return the model's role
	 */
	public URI getRole() {
		return role;
	}

}
