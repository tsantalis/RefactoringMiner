package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLClass;

public class CollapseHierarchyRefactoring implements Refactoring {
	private UMLClass collapsedClass;
	private UMLClass superclassAfterCollapse;

	public CollapseHierarchyRefactoring(UMLClass collapsedClass, UMLClass superclassAfterCollapse) {
		this.collapsedClass = collapsedClass;
		this.superclassAfterCollapse = superclassAfterCollapse;
	}

	private UMLClass getCollapsedClass() {
		return collapsedClass;
	}

	private UMLClass getSuperclassAfterCollapse() {
		return superclassAfterCollapse;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(collapsedClass.codeRange()
				.setDescription("collapsed class")
				.setCodeElement(collapsedClass.getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(superclassAfterCollapse.codeRange()
				.setDescription("superclass after collapse")
				.setCodeElement(superclassAfterCollapse.getName()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.COLLAPSE_HIERARCHY;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getCollapsedClass().getLocationInfo().getFilePath(), getCollapsedClass().getName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getSuperclassAfterCollapse().getLocationInfo().getFilePath(), getSuperclassAfterCollapse().getName()));
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(collapsedClass);
		sb.append(" to ");
		sb.append(superclassAfterCollapse);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collapsedClass == null) ? 0 : collapsedClass.hashCode());
		result = prime * result + ((superclassAfterCollapse == null) ? 0 : superclassAfterCollapse.hashCode());
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
		CollapseHierarchyRefactoring other = (CollapseHierarchyRefactoring) obj;
		if (collapsedClass == null) {
			if (other.collapsedClass != null)
				return false;
		} else if (!collapsedClass.equals(other.collapsedClass))
			return false;
		if (superclassAfterCollapse == null) {
			if (other.superclassAfterCollapse != null)
				return false;
		} else if (!superclassAfterCollapse.equals(other.superclassAfterCollapse))
			return false;
		return true;
	}
}
