package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLClass;

public class SplitClassRefactoring implements PackageLevelRefactoring {
	private Set<UMLClass> splitClasses;
	private UMLClass originalClass;
	
	public SplitClassRefactoring(UMLClassSplitDiff mergeDiff) {
		this.splitClasses = new LinkedHashSet<UMLClass>();
		for(UMLClassRenameDiff renameDiff : mergeDiff.getClassRenameDiffs()) {
			if(originalClass == null) {
				originalClass = renameDiff.getOriginalClass();
			}
			splitClasses.add(renameDiff.getRenamedClass());
		}
	}

	public Set<UMLClass> getSplitClasses() {
		return splitClasses;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.SPLIT_CLASS;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(originalClass.getLocationInfo().getFilePath(), originalClass.getName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(UMLClass splitClass : splitClasses) {
			pairs.add(new ImmutablePair<String, String>(splitClass.getLocationInfo().getFilePath(), splitClass.getName()));
		}
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalClass.codeRange()
				.setDescription("original type declaration")
				.setCodeElement(originalClass.getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(UMLClass splitClass : splitClasses) {
			ranges.add(splitClass.codeRange()
					.setDescription("split type declaration")
					.setCodeElement(splitClass.getName()));
		}
		return ranges;
	}

	@Override
	public RenamePattern getRenamePattern() {
		UMLClass renamedClass = splitClasses.iterator().next();
		UMLClass originalClass = this.originalClass;
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
		return originalClass;
	}

	@Override
	public UMLClass getMovedClass() {
		return splitClasses.iterator().next();
	}

	@Override
	public String getOriginalClassName() {
		return originalClass.getName();
	}

	@Override
	public String getMovedClassName() {
		return splitClasses.iterator().next().getName();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalClass);
		sb.append(" split to ");
		sb.append(splitClasses);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((splitClasses == null) ? 0 : splitClasses.hashCode());
		result = prime * result + ((originalClass == null) ? 0 : originalClass.hashCode());
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
		SplitClassRefactoring other = (SplitClassRefactoring) obj;
		if (splitClasses == null) {
			if (other.splitClasses != null)
				return false;
		} else if (!splitClasses.equals(other.splitClasses))
			return false;
		if (originalClass == null) {
			if (other.originalClass != null)
				return false;
		} else if (!originalClass.equals(other.originalClass))
			return false;
		return true;
	}
}
