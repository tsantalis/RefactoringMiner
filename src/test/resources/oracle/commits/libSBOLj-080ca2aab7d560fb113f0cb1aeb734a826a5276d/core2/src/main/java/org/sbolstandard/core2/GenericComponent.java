package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.Component;

public class GenericComponent extends Component {

	/**
	 * 
	 * @param identity identity for the generic component
	 * @param displayId display ID for the generic component
	 * @param type type for the generic component
	 */
	public GenericComponent(URI identity, String displayId, URI type) {
		super(identity, displayId, type);
	}

}
