package gr.uom.java.xmi;

public enum Constants {
	JAVA(";\n", "return ", "return;\n");
	public final String STATEMENT_TERMINATION;
	public final String RETURN_SPACE;
	public final String RETURN_STATEMENT;
	
	Constants(
			String STATEMENT_TERMINATION,
			String RETURN_SPACE,
			String RETURN_STATEMENT) {
		this.STATEMENT_TERMINATION = STATEMENT_TERMINATION;
		this.RETURN_SPACE = RETURN_SPACE;
		this.RETURN_STATEMENT = RETURN_STATEMENT;
	}
}
