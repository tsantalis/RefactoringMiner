package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class ParameterizeTestRefactoring implements Refactoring {
	private UMLOperation removedOperation;
	private UMLOperation parameterizedTestOperation;
	private UMLOperationBodyMapper bodyMapper;
	
	public ParameterizeTestRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.bodyMapper = bodyMapper;
		this.removedOperation = bodyMapper.getOperation1();
		this.parameterizedTestOperation = bodyMapper.getOperation2();
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
	}

	public UMLOperation getRemovedOperation() {
		return removedOperation;
	}

	public UMLOperation getParameterizedTestOperation() {
		return parameterizedTestOperation;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(removedOperation.codeRange()
				.setDescription("removed method declaration")
				.setCodeElement(removedOperation.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(parameterizedTestOperation.codeRange()
				.setDescription("parameterized test method declaration")
				.setCodeElement(parameterizedTestOperation.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.PARAMETERIZE_TEST;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getRemovedOperation().getLocationInfo().getFilePath(), getRemovedOperation().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getParameterizedTestOperation().getLocationInfo().getFilePath(), getParameterizedTestOperation().getClassName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(removedOperation);
		sb.append(" to ");
		sb.append(parameterizedTestOperation);
		sb.append(" in class ");
		sb.append(parameterizedTestOperation.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameterizedTestOperation, removedOperation);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterizeTestRefactoring other = (ParameterizeTestRefactoring) obj;
		return Objects.equals(parameterizedTestOperation, other.parameterizedTestOperation)
				&& Objects.equals(removedOperation, other.removedOperation);
	}
}
