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

public class MergeOperationRefactoring implements Refactoring {
	private Set<VariableDeclarationContainer> mergedMethods;
	private Set<UMLOperationBodyMapper> mappers;
	private VariableDeclarationContainer newMethodAfterMerge;
	private String classNameBefore;
	private String classNameAfter;

	public MergeOperationRefactoring(Set<VariableDeclarationContainer> mergedMethods,
			VariableDeclarationContainer newMethodAfterMerge,
			String classNameBefore, String classNameAfter, Set<UMLOperationBodyMapper> mappers) {
		this.mergedMethods = mergedMethods;
		this.newMethodAfterMerge = newMethodAfterMerge;
		this.classNameBefore = classNameBefore;
		this.classNameAfter = classNameAfter;
		this.mappers = mappers;
	}

	public Set<VariableDeclarationContainer> getMergedMethods() {
		return mergedMethods;
	}

	public Set<UMLOperationBodyMapper> getMappers() {
		return mappers;
	}

	public VariableDeclarationContainer getNewMethodAfterMerge() {
		return newMethodAfterMerge;
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
		for(VariableDeclarationContainer mergedMethod : getMergedMethods()) {
			ranges.add(mergedMethod.codeRange()
					.setDescription("merged method declaration")
					.setCodeElement(mergedMethod.toString()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(newMethodAfterMerge.codeRange()
				.setDescription("new method declaration")
				.setCodeElement(newMethodAfterMerge.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MERGE_OPERATION;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(VariableDeclarationContainer mergedMethod : this.mergedMethods) {
			pairs.add(new ImmutablePair<String, String>(mergedMethod.getLocationInfo().getFilePath(), getClassNameBefore()));
		}
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getNewMethodAfterMerge().getLocationInfo().getFilePath(), getClassNameAfter()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(getMergedMethods());
		sb.append(" to ");
		sb.append(newMethodAfterMerge);
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(classNameAfter, classNameBefore, mergedMethods, newMethodAfterMerge);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MergeOperationRefactoring other = (MergeOperationRefactoring) obj;
		return Objects.equals(classNameAfter, other.classNameAfter)
				&& Objects.equals(classNameBefore, other.classNameBefore)
				&& Objects.equals(mergedMethods, other.mergedMethods)
				&& Objects.equals(newMethodAfterMerge, other.newMethodAfterMerge);
	}
}
