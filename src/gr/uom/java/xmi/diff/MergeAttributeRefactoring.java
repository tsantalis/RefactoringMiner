package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class MergeAttributeRefactoring implements Refactoring {
	private Set<VariableDeclaration> mergedAttributes;
	private VariableDeclaration newAttribute;
	private Set<CandidateMergeVariableRefactoring> attributeMerges;
	private String classNameBefore;
	private String classNameAfter;
	
	public MergeAttributeRefactoring(Set<VariableDeclaration> mergedAttributes, VariableDeclaration newAttribute,
			String classNameBefore, String classNameAfter, Set<CandidateMergeVariableRefactoring> attributeMerges) {
		this.mergedAttributes = mergedAttributes;
		this.newAttribute = newAttribute;
		this.classNameBefore = classNameBefore;
		this.classNameAfter = classNameAfter;
		this.attributeMerges = attributeMerges;
	}

	public Set<VariableDeclaration> getMergedAttributes() {
		return mergedAttributes;
	}

	public VariableDeclaration getNewAttribute() {
		return newAttribute;
	}

	public Set<CandidateMergeVariableRefactoring> getAttributeMerges() {
		return attributeMerges;
	}

	public String getClassNameBefore() {
		return classNameBefore;
	}

	public String getClassNameAfter() {
		return classNameAfter;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MERGE_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(mergedAttributes);
		sb.append(" to ");
		sb.append(newAttribute);
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
		result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
		result = prime * result + ((mergedAttributes == null) ? 0 : mergedAttributes.hashCode());
		result = prime * result + ((newAttribute == null) ? 0 : newAttribute.hashCode());
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
		MergeAttributeRefactoring other = (MergeAttributeRefactoring) obj;
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
		if (mergedAttributes == null) {
			if (other.mergedAttributes != null)
				return false;
		} else if (!mergedAttributes.equals(other.mergedAttributes))
			return false;
		if (newAttribute == null) {
			if (other.newAttribute != null)
				return false;
		} else if (!newAttribute.equals(other.newAttribute))
			return false;
		return true;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(VariableDeclaration mergedAttribute : this.mergedAttributes) {
			pairs.add(new ImmutablePair<String, String>(mergedAttribute.getLocationInfo().getFilePath(), getClassNameBefore()));
		}
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getNewAttribute().getLocationInfo().getFilePath(), getClassNameAfter()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(VariableDeclaration mergedAttribute : mergedAttributes) {
			ranges.add(mergedAttribute.codeRange()
					.setDescription("merged attribute declaration")
					.setCodeElement(mergedAttribute.toString()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(newAttribute.codeRange()
				.setDescription("new attribute declaration")
				.setCodeElement(newAttribute.toString()));
		return ranges;
	}
}
