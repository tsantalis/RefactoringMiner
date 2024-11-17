package org.sbolstandard.core2;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class SequenceOntology {

	// the map that contains all SO terms 
	// will get initialized at the first access
	
	// EO: does the SO have a 1-1 key-value mapping?
	
	private static Map<String, URI> so = null;
	
	private static final String URI_PREFIX = "http://purl.obolibrary.org/obo/";
	
	public static String getTerm(URI uri) {

		// if the SO terms have not been loaded,
		// then load them now
		if(null == so) {
			loadSO();
		}
		

		// EO: is there a more efficient solution?
		// the answer will depend on the answer to the question above
		if(so.containsValue(uri) && null != uri) {
			// here, we need to iterate over the SO terms
			for(String term : so.keySet()) {
				// if the URI of the current SO term matches the provided URI, 
				// then we return the term
				if(so.get(term).toString().equalsIgnoreCase(uri.toString())) {
					return term;
				}
			}
		}
		
		return null;
	}

	public static URI getURI(String term) {
		// if the SO terms have not been loaded,
		// then load them now
		if(null == so) {
			loadSO();
		}
		
		if(null != term) {
			return so.get(term.toUpperCase());
		}
		
		return null;
	}
	
	private static void loadSO() {

		// this needs to be enhanced, of course
		so = new HashMap<String, URI>();

		// types
		so.put("DNA", URI.create(URI_PREFIX + "SO:0000352"));		
		so.put("PROTEIN", URI.create(URI_PREFIX + "SO:0001217"));
		so.put("COMPLEX", URI.create(URI_PREFIX + "SO:0001784"));
		so.put("GENE", URI.create(URI_PREFIX + "SO:0000704"));
		so.put("SMALL MOLECULE", URI.create(URI_PREFIX + "SO:0001854"));
		
		
		// sequence types
		so.put("PROMOTER", URI.create(URI_PREFIX + "SO:0000167"));
		so.put("RBS", URI.create(URI_PREFIX + "SO:0000139"));
		so.put("TRANSCRIPT", URI.create(URI_PREFIX + "SO:0000673"));
		so.put("CDS", URI.create(URI_PREFIX + "SO:0000673"));
		so.put("TERMINATOR", URI.create(URI_PREFIX + "SO:0000673"));
		
	}

}
