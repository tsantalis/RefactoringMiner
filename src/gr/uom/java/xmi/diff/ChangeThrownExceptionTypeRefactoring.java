package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;

public class ChangeThrownExceptionTypeRefactoring implements Refactoring {
	private Set<UMLType> originalTypes;
	private Set<UMLType> changedTypes;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;
	
	public ChangeThrownExceptionTypeRefactoring(Set<UMLType> originalTypes, Set<UMLType> changedTypes,
			UMLOperation operationBefore, UMLOperation operationAfter) {
		this.originalTypes = originalTypes;
		this.changedTypes = changedTypes;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public Set<UMLType> getOriginalTypes() {
		return originalTypes;
	}

	public Set<UMLType> getChangedTypes() {
		return changedTypes;
	}

	public UMLOperation getOperationBefore() {
		return operationBefore;
	}

	public UMLOperation getOperationAfter() {
		return operationAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(UMLType originalType : originalTypes) {
			ranges.add(originalType.codeRange()
					.setDescription("original exception type")
					.setCodeElement(originalType.toString()));
		}
		ranges.add(operationBefore.codeRange()
				.setDescription("original method declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(UMLType changedType : changedTypes) {
			ranges.add(changedType.codeRange()
					.setDescription("changed exception type")
					.setCodeElement(changedType.toString()));
		}
		ranges.add(operationAfter.codeRange()
				.setDescription("method declaration with changed thrown exception type")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.CHANGE_THROWN_EXCEPTION_TYPE;
	}

	@Override
	public String getName() {
		return getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalTypes.size() == 1 ? originalTypes.iterator().next() : originalTypes);
		sb.append(" to ");
		sb.append(changedTypes.size() == 1 ? changedTypes.iterator().next() : changedTypes);
		sb.append(" in method ");
		sb.append(operationAfter.toString());
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedTypes == null) ? 0 : changedTypes.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((originalTypes == null) ? 0 : originalTypes.hashCode());
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
		ChangeThrownExceptionTypeRefactoring other = (ChangeThrownExceptionTypeRefactoring) obj;
		if (changedTypes == null) {
			if (other.changedTypes != null)
				return false;
		} else if (!changedTypes.equals(other.changedTypes))
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
		if (originalTypes == null) {
			if (other.originalTypes != null)
				return false;
		} else if (!originalTypes.equals(other.originalTypes))
			return false;
		return true;
	}
}
