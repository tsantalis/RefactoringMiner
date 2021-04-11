package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.OperationInvocation;

public class MethodInvocationReplacement extends Replacement {
	private OperationInvocation invokedOperationBefore;
	private OperationInvocation invokedOperationAfter;
	
	public MethodInvocationReplacement(String before, String after,
			OperationInvocation invokedOperationBefore, OperationInvocation invokedOperationAfter,
			ReplacementType type) {
		super(before, after, type);
		this.invokedOperationBefore = invokedOperationBefore;
		this.invokedOperationAfter = invokedOperationAfter;
	}

	public OperationInvocation getInvokedOperationBefore() {
		return invokedOperationBefore;
	}

	public OperationInvocation getInvokedOperationAfter() {
		return invokedOperationAfter;
	}

	public boolean differentExpressionNameAndArguments() {
		return invokedOperationBefore.differentExpressionNameAndArguments(invokedOperationAfter);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((invokedOperationAfter == null) ? 0 : invokedOperationAfter.getLocationInfo().hashCode());
		result = prime * result + ((invokedOperationBefore == null) ? 0 : invokedOperationBefore.getLocationInfo().hashCode());
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
		MethodInvocationReplacement other = (MethodInvocationReplacement) obj;
		if (invokedOperationAfter == null) {
			if (other.invokedOperationAfter != null)
				return false;
		} else if (!invokedOperationAfter.getLocationInfo().equals(other.invokedOperationAfter.getLocationInfo()))
			return false;
		if (invokedOperationBefore == null) {
			if (other.invokedOperationBefore != null)
				return false;
		} else if (!invokedOperationBefore.getLocationInfo().equals(other.invokedOperationBefore.getLocationInfo()))
			return false;
		return true;
	}
}
