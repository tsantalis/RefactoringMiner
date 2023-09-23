package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAbstractClass;

public class ChangeTypeDeclarationKindRefactoring implements Refactoring {
	private String originalTypeDeclarationKind;
	private String changedTypeDeclarationKind;
	private UMLAbstractClass classBefore;
	private UMLAbstractClass classAfter;

	public ChangeTypeDeclarationKindRefactoring(String originalTypeDeclarationKind, String changedTypeDeclarationKind,
			UMLAbstractClass classBefore, UMLAbstractClass classAfter) {
		this.originalTypeDeclarationKind = originalTypeDeclarationKind;
		this.changedTypeDeclarationKind = changedTypeDeclarationKind;
		this.classBefore = classBefore;
		this.classAfter = classAfter;
	}

	public String getOriginalTypeDeclarationKind() {
		return originalTypeDeclarationKind;
	}

	public String getChangedTypeDeclarationKind() {
		return changedTypeDeclarationKind;
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
				.setDescription("class declaration with changed type declaration kind")
				.setCodeElement(classAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.CHANGE_TYPE_DECLARATION_KIND;
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
		sb.append(originalTypeDeclarationKind);
		sb.append(" to ");
		sb.append(changedTypeDeclarationKind);
		sb.append(" in type ");
		sb.append(classAfter.getName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedTypeDeclarationKind == null) ? 0 : changedTypeDeclarationKind.hashCode());
		result = prime * result + ((classAfter == null) ? 0 : classAfter.hashCode());
		result = prime * result + ((classBefore == null) ? 0 : classBefore.hashCode());
		result = prime * result + ((originalTypeDeclarationKind == null) ? 0 : originalTypeDeclarationKind.hashCode());
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
		ChangeTypeDeclarationKindRefactoring other = (ChangeTypeDeclarationKindRefactoring) obj;
		if (changedTypeDeclarationKind == null) {
			if (other.changedTypeDeclarationKind != null)
				return false;
		} else if (!changedTypeDeclarationKind.equals(other.changedTypeDeclarationKind))
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
		if (originalTypeDeclarationKind == null) {
			if (other.originalTypeDeclarationKind != null)
				return false;
		} else if (!originalTypeDeclarationKind.equals(other.originalTypeDeclarationKind))
			return false;
		return true;
	}
}
