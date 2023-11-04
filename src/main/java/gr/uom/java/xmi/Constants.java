package gr.uom.java.xmi;

public enum Constants {
	JAVA(";\n", "return ");
	public final String STATEMENT_TERMINATION;
	public final String RETURN;
	
	Constants(String STATEMENT_TERMINATION,
			String RETURN) {
		this.STATEMENT_TERMINATION = STATEMENT_TERMINATION;
		this.RETURN = RETURN;
	}
}
