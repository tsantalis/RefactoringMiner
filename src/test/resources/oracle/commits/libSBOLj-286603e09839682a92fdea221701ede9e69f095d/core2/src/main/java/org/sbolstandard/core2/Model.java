package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.abstract_classes.Documented;

/**
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 * @version 2.0
 */
public class Model extends TopLevel {
		
	private URI source;
	private URI language;
	private URI framework;
	private URI roles;

	public Model(URI identity, URI persistentIdentity, String version,
			String displayId, String name, String description, URI source,
			URI language, URI framework, URI roles) {
		super(identity, persistentIdentity, version, displayId, name, description);
		this.source = source;
		this.language = language;
		this.framework = framework;
		this.roles = roles;
	}

	public URI getSource() {
		return source;
	}

	public void setSource(URI source) {
		this.source = source;
	}

	public URI getLanguage() {
		return language;
	}

	public void setLanguage(URI language) {
		this.language = language;
	}

	public URI getFramework() {
		return framework;
	}

	public void setFramework(URI framework) {
		this.framework = framework;
	}

	public URI getRoles() {
		return roles;
	}

	public void setRoles(URI roles) {
		this.roles = roles;
	}



}
