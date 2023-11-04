package gr.uom.java.xmi;

public enum Constants {
	JAVA(";\n", "return ", "return;\n", "return true;\n", "return false;\n", "return null;\n", "return this;\n", "=");
	public final String STATEMENT_TERMINATION;
	public final String RETURN_SPACE;
	public final String RETURN_STATEMENT;
	public final String RETURN_TRUE;
	public final String RETURN_FALSE;
	public final String RETURN_NULL;
	public final String RETURN_THIS;
	public final String ASSIGNMENT;
	
	Constants(
			String STATEMENT_TERMINATION,
			String RETURN_SPACE,
			String RETURN_STATEMENT,
			String RETURN_TRUE,
			String RETURN_FALSE,
			String RETURN_NULL,
			String RETURN_THIS,
			String ASSIGNMENT) {
		this.STATEMENT_TERMINATION = STATEMENT_TERMINATION;
		this.RETURN_SPACE = RETURN_SPACE;
		this.RETURN_STATEMENT = RETURN_STATEMENT;
		this.RETURN_TRUE = RETURN_TRUE;
		this.RETURN_FALSE = RETURN_FALSE;
		this.RETURN_NULL = RETURN_NULL;
		this.RETURN_THIS = RETURN_THIS;
		this.ASSIGNMENT = ASSIGNMENT;
	}
}
