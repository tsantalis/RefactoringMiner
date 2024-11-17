package org.sbolstandard.core2;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zhen Zhang
 * @version 2.0
 *
 */
public class SequenceOntology {

	// the map that contains all SO terms 
	// will get initialized at the first access
	
	// EO: does the SO have a 1-1 key-value mapping?
	
	private static Map<String, URI> so = null;
	
	private static final String URI_PREFIX = "http://purl.obolibrary.org/obo/";
	
	/**
	 * Namespace of the Sequence Ontology (<a href="http://purl.obolibrary.org/obo/">http://purl.obolibrary.org/obo/</a>).
	 */
	public static final URI NAMESPACE = URI.create("http://purl.obolibrary.org/obo/");
	
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
	
	/**
	 * Creates a new URI from the Sequence Ontology namespace with the given local name. For example, the function call
	 * <value>term("SO_0000001")</value> will return the URI <value>http://purl.obolibrary.org/obo/SO_0000001</value>
	 */
	public static final URI type(String localName) {
		return NAMESPACE.resolve(localName);
	}
	
		/**
	 * A regulatory_region composed of the TSS(s) and binding sites for TF_complexes of the basal transcription
	 * machinery (<a href="http://purl.obolibrary.org/obo/SO_0000167">SO_0000167</a>).
	 */
	public static final URI PROMOTER = type("SO_0000167");

	/**
	 * A regulatory element of an operon to which activators or repressors bind, thereby effecting translation of genes
	 * in that operon (<a href="http://purl.obolibrary.org/obo/SO_0000057">SO_0000057</a>).
	 */
	public static final URI OPERATOR = type("SO_0000057");

	/**
	 * A contiguous sequence which begins with, and includes, a start codon, and ends with, and includes, a stop codon
	 * (<a href="http://purl.obolibrary.org/obo/SO_0000316">SO_0000316</a>).
	 */
	public static final URI CDS = type("SO_0000316");

	/**
	 * A region at the 5' end of a mature transcript (preceding the initiation codon) that is not translated into a
	 * protein (<a href="http://purl.obolibrary.org/obo/SO_0000204">SO_0000204</a>).
	 */
	public static final URI FIVE_PRIME_UTR = type("SO_0000204");

	/**
	 * The sequence of DNA located either at the end of the transcript that causes RNA polymerase to terminate
	 * transcription (<a href="http://purl.obolibrary.org/obo/SO_0000141">SO_0000141</a>).
	 */
	public static final URI TERMINATOR = type("SO_0000141");

	/**
	 * A transcriptional cis regulatory region that, when located between a CM and a gene's promoter, prevents the CRM
	 * from modulating that genes expression (<a href="http://purl.obolibrary.org/obo/SO_0000627">SO_0000627</a>)
	 */
	public static final URI INSULATOR = type("SO_0000627");

	/**
	 * The origin of replication; starting site for duplication of a nucleic acid molecule to give two identical copies
	 * (<a href="http://purl.obolibrary.org/obo/SO_0000296">SO_0000296</a>).
	 */
	public static final URI ORIGIN_OF_REPLICATION = type("SO_0000296");

	/**
	 * Non-covalent primer binding site for initiation of replication, transcription, or reverse transcription (<a
	 * href="http://purl.obolibrary.org/obo/SO_0005850">SO_0005850<a/>)
	 */
	public static final URI PRIMER_BINDING_SITE = type("SO_0005850");

	/**
	 * Represents a region of a DNA molecule which is a nucleotide region (usually a palindrome) that is recognized by a
	 * restriction enzyme (<a href="http://purl.obolibrary.org/obo/SO_0001687">SO_0001687</a>).
	 */
	public static final URI RESTRICTION_ENZYME_RECOGNITION_SITE = type("SO_0001687");

}
