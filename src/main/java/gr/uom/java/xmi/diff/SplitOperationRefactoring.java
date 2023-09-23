package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class SplitOperationRefactoring implements Refactoring {
	private Set<VariableDeclarationContainer> splitMethods;
	private Set<UMLOperationBodyMapper> mappers;
	private VariableDeclarationContainer originalMethodBeforeSplit;
	private String classNameBefore;
	private String classNameAfter;

	public SplitOperationRefactoring(VariableDeclarationContainer originalMethodBeforeSplit,
			Set<VariableDeclarationContainer> splitMethods, String classNameBefore, String classNameAfter, Set<UMLOperationBodyMapper> mappers) {
		this.originalMethodBeforeSplit = originalMethodBeforeSplit;
		this.splitMethods = splitMethods;
		this.classNameBefore = classNameBefore;
		this.classNameAfter = classNameAfter;
		this.mappers = mappers;
	}

	public Set<VariableDeclarationContainer> getSplitMethods() {
		return splitMethods;
	}

	public Set<UMLOperationBodyMapper> getMappers() {
		return mappers;
	}

	public VariableDeclarationContainer getOriginalMethodBeforeSplit() {
		return originalMethodBeforeSplit;
	}

	public String getClassNameBefore() {
		return classNameBefore;
	}

	public String getClassNameAfter() {
		return classNameAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalMethodBeforeSplit.codeRange()
				.setDescription("original method declaration")
				.setCodeElement(originalMethodBeforeSplit.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(VariableDeclarationContainer splitMethod : getSplitMethods()) {
			ranges.add(splitMethod.codeRange()
					.setDescription("split method declaration")
					.setCodeElement(splitMethod.toString()));
		}
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.SPLIT_OPERATION;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalMethodBeforeSplit().getLocationInfo().getFilePath(), getClassNameBefore()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(VariableDeclarationContainer splitMethod : this.splitMethods) {
			pairs.add(new ImmutablePair<String, String>(splitMethod.getLocationInfo().getFilePath(), getClassNameAfter()));
		}
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalMethodBeforeSplit);
		sb.append(" to ");
		sb.append(getSplitMethods());
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(classNameAfter, classNameBefore, originalMethodBeforeSplit, splitMethods);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SplitOperationRefactoring other = (SplitOperationRefactoring) obj;
		return Objects.equals(classNameAfter, other.classNameAfter)
				&& Objects.equals(classNameBefore, other.classNameBefore)
				&& Objects.equals(originalMethodBeforeSplit, other.originalMethodBeforeSplit)
				&& Objects.equals(splitMethods, other.splitMethods);
	}
}
