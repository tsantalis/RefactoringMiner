package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

public class CandidateAttributeRename {
	private String originalVariableName;
	private String renamedVariableName;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;

	public CandidateAttributeRename(
			String originalVariableName,
			String renamedVariableName,
			UMLOperation operationBefore,
			UMLOperation operationAfter) {
		this.originalVariableName = originalVariableName;
		this.renamedVariableName = renamedVariableName;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public String getOriginalVariableName() {
		return originalVariableName;
	}

	public String getRenamedVariableName() {
		return renamedVariableName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Rename Attribute").append("\t");
		sb.append(originalVariableName);
		sb.append(" to ");
		sb.append(renamedVariableName);
		sb.append(" in method ");
		sb.append(operationAfter);
		sb.append(" in class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((originalVariableName == null) ? 0 : originalVariableName.hashCode());
		result = prime * result + ((renamedVariableName == null) ? 0 : renamedVariableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CandidateAttributeRename other = (CandidateAttributeRename) obj;
		if (operationAfter == null) {
			if (other.operationAfter != null)
				return false;
		} else if (!operationAfter.equals(other.operationAfter))
			return false;
		if (operationBefore == null) {
			if (other.operationBefore != null)
				return false;
		} else if (!operationBefore.equals(other.operationBefore))
			return false;
		if (originalVariableName == null) {
			if (other.originalVariableName != null)
				return false;
		} else if (!originalVariableName.equals(other.originalVariableName))
			return false;
		if (renamedVariableName == null) {
			if (other.renamedVariableName != null)
				return false;
		} else if (!renamedVariableName.equals(other.renamedVariableName))
			return false;
		return true;
	}

}
