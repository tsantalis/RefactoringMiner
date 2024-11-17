package org.sbolstandard.core2;

import java.net.URI;

import org.sbolstandard.core2.abstract_classes.Identified;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 * @version 2.0
 */
public class Sequence extends Identified {

//	private String elements;
//		
//	/**
//	 * 
//	 * @param identity an identity for the sequence
//	 * @param elements an element string for the sequence
//	 */
//	public Sequence(URI identity, String elements) {
//		super(identity);
//		this.elements = elements;
//	}
//
//	/**
//	 * 
//	 * @deprecated Creating an empty Sequence object is not recommended. See {@link #Sequence(URI, String)} 
//	 * 
//	 */
//	public Sequence() {
//		
//	}
//
//	/**
//	 * 
//	 * @return the sequence's element string
//	 */
//	public String getElements() {
//		return elements;
//	}
//	
//	
//	/**
//	 * Copied directly from libSBOLj.
//	 * @return
//	 */
//	public String getReverseComplementaryNucleotides() {
//		StringBuilder complementary = new StringBuilder(elements.length());
//		for (int i = elements.length() - 1; i >= 0; i--) {
//			char nucleotide = elements.charAt(i);
//			if (nucleotide == 'a')
//				complementary.append('t');
//			else if (nucleotide == 't')
//				complementary.append('a');
//			else if (nucleotide == 'g')
//				complementary.append('c');
//			else if (nucleotide == 'c')
//				complementary.append('g');
//			else if (nucleotide == 'm')
//				complementary.append('k');
//			else if (nucleotide == 'r')
//				complementary.append('y');
//			else if (nucleotide == 'w')
//				complementary.append('w');
//			else if (nucleotide == 's')
//				complementary.append('s');
//			else if (nucleotide == 'y')
//				complementary.append('r');
//			else if (nucleotide == 'k')
//				complementary.append('m');
//			else if (nucleotide == 'v')
//				complementary.append('b');
//			else if (nucleotide == 'h')
//				complementary.append('d');
//			else if (nucleotide == 'd')
//				complementary.append('h');
//			else if (nucleotide == 'b')
//				complementary.append('v');
//			else if (nucleotide == 'n')
//				complementary.append('n');
//		}
//		return complementary.toString();
//	}
//
//		
//	/**
//	 * The sequence of DNA base pairs which are going to be described.
//	 *
//	 *  a.The DNA sequence will use the IUPAC ambiguity recommendation. (See
//	 * http://www.genomatix.de/online_help/help/sequence_formats.html)
//	 * b.Blank lines, spaces, or other symbols must not be included in the
//	 * sequence text.
//	 * c.The sequence text must be in ASCII or UTF-8 encoding. For the alphabets
//	 * used, the two are identical.
//	 *
//	 * @param nucleotides a sequence of [a|c|t|g] letters
//	 * @deprecated As of release 2.0, replaced by {@link #setElements()}
//	 */
//	public void setNucleotides(String value) {
//		this.elements = value;
//	}
//	
//	/**
//	 * The sequence of DNA base pairs which are described.
//	 * @return a string representation of the DNA base-pair sequence
//	 * @deprecated As of release 2.0, replaced by {@link #getElements()}
//	 */
//	public String getNucleotides() {
//		return elements;
//	}

	public Sequence(URI identity, URI persistentIdentity, String version) {
		super(identity, persistentIdentity, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public <T extends Throwable> void accept(SBOLVisitor<T> visitor) throws T {
		// TODO Auto-generated method stub
		
	}
}
