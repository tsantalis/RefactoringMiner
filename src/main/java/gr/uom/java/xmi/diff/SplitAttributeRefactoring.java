package gr.uom.java.xmi.diff;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.UMLAttribute;

public class SplitAttributeRefactoring implements Refactoring {
	private UMLAttribute oldAttribute;
	private Set<UMLAttribute> splitAttributes;
	private Set<CandidateSplitVariableRefactoring> attributeSplits;
	private String classNameBefore;
	private String classNameAfter;

	public SplitAttributeRefactoring(UMLAttribute oldAttribute, Set<UMLAttribute> splitAttributes,
									 String classNameBefore, String classNameAfter, Set<CandidateSplitVariableRefactoring> attributeSplits) {
		this.oldAttribute = oldAttribute;
		this.splitAttributes = splitAttributes;
		this.classNameBefore = classNameBefore;
		this.classNameAfter = classNameAfter;
		this.attributeSplits = attributeSplits;
	}

	public UMLAttribute getOldAttribute() {
		return oldAttribute;
	}

	public Set<UMLAttribute> getSplitAttributes() {
		return splitAttributes;
	}

	public Set<CandidateSplitVariableRefactoring> getAttributeSplits() {
		return attributeSplits;
	}

	public String getClassNameBefore() {
		return classNameBefore;
	}

	public String getClassNameAfter() {
		return classNameAfter;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.SPLIT_ATTRIBUTE;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(oldAttribute.getVariableDeclaration());
		sb.append(" to ");
		sb.append(getSplitVariables());
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
		result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
		result = prime * result + ((oldAttribute == null || oldAttribute.getVariableDeclaration() == null) ? 0 : oldAttribute.getVariableDeclaration().hashCode());
		Set<VariableDeclaration> splitVariables = getSplitVariables();
		result = prime * result + ((splitVariables.isEmpty()) ? 0 : splitVariables.hashCode());
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
		SplitAttributeRefactoring other = (SplitAttributeRefactoring) obj;
		if (classNameAfter == null) {
			if (other.classNameAfter != null)
				return false;
		} else if (!classNameAfter.equals(other.classNameAfter))
			return false;
		if (classNameBefore == null) {
			if (other.classNameBefore != null)
				return false;
		} else if (!classNameBefore.equals(other.classNameBefore))
			return false;
		if (oldAttribute == null) {
			if (other.oldAttribute != null)
				return false;
		} else if (oldAttribute.getVariableDeclaration() == null) {
			if (other.oldAttribute.getVariableDeclaration() != null)
				return false;
		} else if (!oldAttribute.getVariableDeclaration().equals(other.oldAttribute.getVariableDeclaration()))
			return false;
		if (splitAttributes == null) {
			if (other.splitAttributes != null)
				return false;
		}
		if (!this.getSplitVariables().equals(other.getSplitVariables()))
				return false;
		return true;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOldAttribute().getLocationInfo().getFilePath(), getClassNameBefore()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(UMLAttribute splitAttribute : this.splitAttributes) {
			pairs.add(new ImmutablePair<String, String>(splitAttribute.getLocationInfo().getFilePath(), getClassNameAfter()));
		}
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(oldAttribute.getVariableDeclaration().codeRange()
				.setDescription("original attribute declaration")
				.setCodeElement(oldAttribute.getVariableDeclaration().toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(VariableDeclaration splitVariableDeclaration : getSplitVariables()) {
			ranges.add(splitVariableDeclaration.codeRange()
					.setDescription("split attribute declaration")
					.setCodeElement(splitVariableDeclaration.toString()));
		}
		return ranges;
	}

	public Set<VariableDeclaration> getSplitVariables() {
		return splitAttributes.stream().map(UMLAttribute::getVariableDeclaration).collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
