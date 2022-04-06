package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAbstractClass;

public class AddClassModifierRefactoring implements Refactoring {
	private String modifier;
	private UMLAbstractClass classBefore;
	private UMLAbstractClass classAfter;

	public AddClassModifierRefactoring(String modifier, UMLAbstractClass classBefore, UMLAbstractClass classAfter) {
		this.modifier = modifier;
		this.classBefore = classBefore;
		this.classAfter = classAfter;
	}

	public String getModifier() {
		return modifier;
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
				.setDescription("class declaration with added modifier")
				.setCodeElement(classAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.ADD_CLASS_MODIFIER;
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
		sb.append(modifier);
		sb.append(" in class ");
		sb.append(classAfter.getName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classAfter == null) ? 0 : classAfter.hashCode());
		result = prime * result + ((classBefore == null) ? 0 : classBefore.hashCode());
		result = prime * result + ((modifier == null) ? 0 : modifier.hashCode());
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
		AddClassModifierRefactoring other = (AddClassModifierRefactoring) obj;
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
		if (modifier == null) {
			if (other.modifier != null)
				return false;
		} else if (!modifier.equals(other.modifier))
			return false;
		return true;
	}
}
