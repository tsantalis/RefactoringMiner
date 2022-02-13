package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLClass;

public class MergeClassRefactoring implements PackageLevelRefactoring {
	private Set<UMLClass> mergedClasses;
	private UMLClass newClass;
	
	public MergeClassRefactoring(UMLClassMergeDiff mergeDiff) {
		this.mergedClasses = new LinkedHashSet<UMLClass>();
		for(UMLClassRenameDiff renameDiff : mergeDiff.getClassRenameDiffs()) {
			if(newClass == null) {
				newClass = renameDiff.getRenamedClass();
			}
			mergedClasses.add(renameDiff.getOriginalClass());
		}
	}

	public Set<UMLClass> getMergedClasses() {
		return mergedClasses;
	}

	public UMLClass getNewClass() {
		return newClass;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(UMLClass mergedClass : mergedClasses) {
			ranges.add(mergedClass.codeRange()
					.setDescription("original type declaration")
					.setCodeElement(mergedClass.getName()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(newClass.codeRange()
				.setDescription("new type declaration")
				.setCodeElement(newClass.getName()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MERGE_CLASS;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(UMLClass mergedClass : mergedClasses) {
			pairs.add(new ImmutablePair<String, String>(mergedClass.getLocationInfo().getFilePath(), mergedClass.getName()));
		}
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(newClass.getLocationInfo().getFilePath(), newClass.getName()));
		return pairs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(mergedClasses);
		sb.append(" merged to ");
		sb.append(newClass);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mergedClasses == null) ? 0 : mergedClasses.hashCode());
		result = prime * result + ((newClass == null) ? 0 : newClass.hashCode());
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
		MergeClassRefactoring other = (MergeClassRefactoring) obj;
		if (mergedClasses == null) {
			if (other.mergedClasses != null)
				return false;
		} else if (!mergedClasses.equals(other.mergedClasses))
			return false;
		if (newClass == null) {
			if (other.newClass != null)
				return false;
		} else if (!newClass.equals(other.newClass))
			return false;
		return true;
	}

	@Override
	public RenamePattern getRenamePattern() {
		UMLClass renamedClass = newClass;
		UMLClass originalClass = mergedClasses.iterator().next();
		int separatorPos = PrefixSuffixUtils.separatorPosOfCommonSuffix('.', originalClass.getPackageName(), renamedClass.getPackageName());
		if (separatorPos == -1) {
			return new RenamePattern(originalClass.getPackageName(), renamedClass.getPackageName());
		}
		String originalPath = originalClass.getName().substring(0, originalClass.getName().length() - separatorPos);
		String movedPath = renamedClass.getName().substring(0, renamedClass.getName().length() - separatorPos);
		return new RenamePattern(originalPath, movedPath);
	}

	@Override
	public UMLClass getOriginalClass() {
		return mergedClasses.iterator().next();
	}

	@Override
	public UMLClass getMovedClass() {
		return newClass;
	}

	@Override
	public String getOriginalClassName() {
		return mergedClasses.iterator().next().getName();
	}

	@Override
	public String getMovedClassName() {
		return newClass.getName();
	}
}
