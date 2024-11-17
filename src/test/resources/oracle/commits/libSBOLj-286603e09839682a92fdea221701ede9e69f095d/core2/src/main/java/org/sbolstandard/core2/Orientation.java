package org.sbolstandard.core2;

/**
 * 
 * @author Ernst Oberortner
 * @author Nicholas Roehner
 * @version 2.0
 */
public enum Orientation {
	inline, 
	reverseComplement,
	
	/**
	 * Represents <code>+</code> strand which is 5' to 3'.
	 * @deprecated As of release 2.0, replaced by {@link #inline}}
	 */
	POSITIVE("+"), 
	
	/**
	 * Represents <code>-</code> strand which is 3' to 5'.
	 * @deprecated As of release 2.0, replaced by {@link #reverseComplement}}
	 */
	NEGATIVE("-");
	
	Orientation() {
		
	}
	
	private String symbol;
	
	private Orientation(String symbol) {
		this.symbol = symbol;
	}
	
	/**
	 * Returns the symbol (inline or reverseComplement) for this strand type. 
	 */
	public String getSymbol() {
		return symbol;
	}
	
	@Override
	public String toString() {
		return symbol;
	}
	
}
