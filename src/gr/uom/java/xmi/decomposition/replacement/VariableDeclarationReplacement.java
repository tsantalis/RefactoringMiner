package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class VariableDeclarationReplacement extends Replacement {

	private VariableDeclaration v1;
	private VariableDeclaration v2;
	
	public VariableDeclarationReplacement(VariableDeclaration v1, VariableDeclaration v2) {
		super(v1.toString() + " | " + v1.getScope(), v2.toString() + " | " + v2.getScope(), ReplacementType.VARIABLE_DECLARATION);
		this.v1 = v1;
		this.v2 = v2;
	}

	public VariableDeclaration getVariableDeclaration1() {
		return v1;
	}

	public VariableDeclaration getVariableDeclaration2() {
		return v2;
	}

	public Replacement getVariableNameReplacement() {
		return new Replacement(v1.getVariableName(), v2.getVariableName(), ReplacementType.VARIABLE_NAME);
	}
}
