package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;

public class ChangeReturnTypeRefactoring implements Refactoring {
	private UMLType originalType;
	private UMLType changedType;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;

	public ChangeReturnTypeRefactoring(UMLType originalType, UMLType changedType,
			UMLOperation operationBefore, UMLOperation operationAfter) {
		this.originalType = originalType;
		this.changedType = changedType;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.CHANGE_RETURN_TYPE;
	}

	public String getName() {
		return getRefactoringType().getDisplayName();
	}

	public UMLType getOriginalType() {
		return originalType;
	}

	public UMLType getChangedType() {
		return changedType;
	}

	public UMLOperation getOperationBefore() {
		return operationBefore;
	}

	public UMLOperation getOperationAfter() {
		return operationAfter;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalType);
		sb.append(" to ");
		sb.append(changedType);
		sb.append(" in method ");
		sb.append(operationAfter);
		sb.append(" in class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedType == null) ? 0 : changedType.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((originalType == null) ? 0 : originalType.hashCode());
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
		ChangeReturnTypeRefactoring other = (ChangeReturnTypeRefactoring) obj;
		if (changedType == null) {
			if (other.changedType != null)
				return false;
		} else if (!changedType.equals(other.changedType))
			return false;
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
		if (originalType == null) {
			if (other.originalType != null)
				return false;
		} else if (!originalType.equals(other.originalType))
			return false;
		return true;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(operationBefore.getClassName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(operationAfter.getClassName());
		return classNames;
	}


	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalType.codeRange()
				.setDescription("original return type")
				.setCodeElement(originalType.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(changedType.codeRange()
				.setDescription("changed return type")
				.setCodeElement(changedType.toString()));
		return ranges;
	}
}
