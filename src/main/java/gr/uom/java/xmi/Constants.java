package gr.uom.java.xmi;

public enum Constants {
	JAVA(";\n", "return ", "return;\n", "return true;\n", "return false;\n", "return null;\n", "return this;\n", "=", "break;\n", "continue;\n",
			" -> ", "::", " + ", "throw ", "case ", "try", "{", "}", "{", "}", "this.");
	public final String STATEMENT_TERMINATION;
	public final String RETURN_SPACE;
	public final String RETURN_STATEMENT;
	public final String RETURN_TRUE;
	public final String RETURN_FALSE;
	public final String RETURN_NULL;
	public final String RETURN_THIS;
	public final String ASSIGNMENT;
	public final String BREAK_STATEMENT;
	public final String CONTINUE_STATEMENT;
	public final String LAMBDA_ARROW;
	public final String METHOD_REFERENCE;
	public final String STRING_CONCATENATION;
	public final String THROW_SPACE;
	public final String CASE_SPACE;
	public final String TRY;
	public final String OPEN_BLOCK;
	public final String CLOSE_BLOCK;
	public final String OPEN_ARRAY_INITIALIZER;
	public final String CLOSE_ARRAY_INITIALIZER;
	public final String THIS_DOT;
	
	Constants(
			String STATEMENT_TERMINATION,
			String RETURN_SPACE,
			String RETURN_STATEMENT,
			String RETURN_TRUE,
			String RETURN_FALSE,
			String RETURN_NULL,
			String RETURN_THIS,
			String ASSIGNMENT,
			String BREAK_STATEMENT,
			String CONTINUE_STATEMENT,
			String LAMBDA_ARROW,
			String METHOD_REFERENCE,
			String STRING_CONCATENATION,
			String THROW_SPACE,
			String CASE_SPACE,
			String TRY,
			String OPEN_BLOCK,
			String CLOSE_BLOCK,
			String OPEN_ARRAY_INITIALIZER,
			String CLOSE_ARRAY_INITIALIZER,
			String THIS_DOT) {
		this.STATEMENT_TERMINATION = STATEMENT_TERMINATION;
		this.RETURN_SPACE = RETURN_SPACE;
		this.RETURN_STATEMENT = RETURN_STATEMENT;
		this.RETURN_TRUE = RETURN_TRUE;
		this.RETURN_FALSE = RETURN_FALSE;
		this.RETURN_NULL = RETURN_NULL;
		this.RETURN_THIS = RETURN_THIS;
		this.ASSIGNMENT = ASSIGNMENT;
		this.BREAK_STATEMENT = BREAK_STATEMENT;
		this.CONTINUE_STATEMENT = CONTINUE_STATEMENT;
		this.LAMBDA_ARROW = LAMBDA_ARROW;
		this.METHOD_REFERENCE = METHOD_REFERENCE;
		this.STRING_CONCATENATION = STRING_CONCATENATION;
		this.THROW_SPACE = THROW_SPACE;
		this.CASE_SPACE = CASE_SPACE;
		this.TRY = TRY;
		this.OPEN_BLOCK = OPEN_BLOCK;
		this.CLOSE_BLOCK = CLOSE_BLOCK;
		this.OPEN_ARRAY_INITIALIZER = OPEN_ARRAY_INITIALIZER;
		this.CLOSE_ARRAY_INITIALIZER = CLOSE_ARRAY_INITIALIZER;
		this.THIS_DOT = THIS_DOT;
	}
}
