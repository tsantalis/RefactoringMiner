package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class VariableDeclarationReplacement extends Replacement {

	private VariableDeclaration v1;
	private VariableDeclaration v2;
	private VariableDeclarationContainer operation1;
	private VariableDeclarationContainer operation2;
	
	public VariableDeclarationReplacement(VariableDeclaration v1, VariableDeclaration v2,
			VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
		super(v1.toString() + " | " + v1.getScope(), v2.toString() + " | " + v2.getScope(), ReplacementType.VARIABLE_DECLARATION);
		this.v1 = v1;
		this.v2 = v2;
		this.operation1 = operation1;
		this.operation2 = operation2;
	}

	public VariableDeclaration getVariableDeclaration1() {
		return v1;
	}

	public VariableDeclaration getVariableDeclaration2() {
		return v2;
	}

	public VariableDeclarationContainer getOperation1() {
		return operation1;
	}

	public VariableDeclarationContainer getOperation2() {
		return operation2;
	}

	public boolean identicalTypeAndInitializer() {
		AbstractExpression initializer1 = v1.getInitializer();
		AbstractExpression initializer2 = v2.getInitializer();
		if(initializer1 != null && initializer2 != null) {
			return v1.equalType(v2) && initializer1.getExpression().equals(initializer2.getExpression());
		}
		return false;
	}

	public Replacement getVariableNameReplacement() {
		return new Replacement(v1.getVariableName(), v2.getVariableName(), ReplacementType.VARIABLE_NAME);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
		result = prime * result + ((v2 == null) ? 0 : v2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableDeclarationReplacement other = (VariableDeclarationReplacement) obj;
		if (v1 == null) {
			if (other.v1 != null)
				return false;
		} else if (!v1.equals(other.v1))
			return false;
		if (v2 == null) {
			if (other.v2 != null)
				return false;
		} else if (!v2.equals(other.v2))
			return false;
		return true;
	}
}
