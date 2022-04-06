package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAbstractClass;

public class ChangeClassAccessModifierRefactoring implements Refactoring {
	private String originalAccessModifier;
	private String changedAccessModifier;
	private UMLAbstractClass classBefore;
	private UMLAbstractClass classAfter;

	public ChangeClassAccessModifierRefactoring(String originalAccessModifier, String changedAccessModifier,
			UMLAbstractClass classBefore, UMLAbstractClass classAfter) {
		this.originalAccessModifier = originalAccessModifier;
		this.changedAccessModifier = changedAccessModifier;
		this.classBefore = classBefore;
		this.classAfter = classAfter;
	}

	public String getOriginalAccessModifier() {
		return originalAccessModifier;
	}

	public String getChangedAccessModifier() {
		return changedAccessModifier;
	}

	public UMLAbstractClass getClassBefore() {
		return classBefore;
	}

	public UMLAbstractClass getClassAfter() {
		return classAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(classBefore.codeRange()
				.setDescription("original class declaration")
				.setCodeElement(classBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(classAfter.codeRange()
				.setDescription("class declaration with changed access modifier")
				.setCodeElement(classAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.CHANGE_CLASS_ACCESS_MODIFIER;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getClassBefore().getLocationInfo().getFilePath(), getClassBefore().getName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getClassAfter().getLocationInfo().getFilePath(), getClassAfter().getName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalAccessModifier);
		sb.append(" to ");
		sb.append(changedAccessModifier);
		sb.append(" in class ");
		sb.append(classAfter.getName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedAccessModifier == null) ? 0 : changedAccessModifier.hashCode());
		result = prime * result + ((classAfter == null) ? 0 : classAfter.hashCode());
		result = prime * result + ((classBefore == null) ? 0 : classBefore.hashCode());
		result = prime * result + ((originalAccessModifier == null) ? 0 : originalAccessModifier.hashCode());
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
		ChangeClassAccessModifierRefactoring other = (ChangeClassAccessModifierRefactoring) obj;
		if (changedAccessModifier == null) {
			if (other.changedAccessModifier != null)
				return false;
		} else if (!changedAccessModifier.equals(other.changedAccessModifier))
			return false;
		if (classAfter == null) {
			if (other.classAfter != null)
				return false;
		} else if (!classAfter.equals(other.classAfter))
			return false;
		if (classBefore == null) {
			if (other.classBefore != null)
				return false;
		} else if (!classBefore.equals(other.classBefore))
			return false;
		if (originalAccessModifier == null) {
			if (other.originalAccessModifier != null)
				return false;
		} else if (!originalAccessModifier.equals(other.originalAccessModifier))
			return false;
		return true;
	}
}
