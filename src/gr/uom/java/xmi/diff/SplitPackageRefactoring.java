package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class SplitPackageRefactoring implements Refactoring {
	private String originalPackage;
	private Set<String> splitPackages;
	private Set<RenamePackageRefactoring> renamePackageRefactorings;
	
	public SplitPackageRefactoring(Set<RenamePackageRefactoring> renamePackageRefactorings) {
		this.renamePackageRefactorings = renamePackageRefactorings;
		this.splitPackages = new TreeSet<String>();
		for(RenamePackageRefactoring refactoring : renamePackageRefactorings) {
			RenamePattern pattern = refactoring.getPattern();
			if(originalPackage == null) {
				this.originalPackage = pattern.getBefore();
			}
			splitPackages.add(pattern.getAfter());
		}
	}

	public String getOriginalPackage() {
		return originalPackage;
	}

	public Set<String> getSplitPackages() {
		return splitPackages;
	}

	public Set<RenamePackageRefactoring> getRenamePackageRefactorings() {
		return renamePackageRefactorings;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(RenamePackageRefactoring renamePackage : renamePackageRefactorings) {
			for(PackageLevelRefactoring ref : renamePackage.getMoveClassRefactorings()) {
				ranges.add(ref.getOriginalClass().codeRange()
						.setDescription("original type declaration")
						.setCodeElement(ref.getOriginalClass().getName()));
			}
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(RenamePackageRefactoring renamePackage : renamePackageRefactorings) {
			for(PackageLevelRefactoring ref : renamePackage.getMoveClassRefactorings()) {
				ranges.add(ref.getMovedClass().codeRange()
						.setDescription("moved type declaration")
						.setCodeElement(ref.getMovedClass().getName()));
			}
		}
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.SPLIT_PACKAGE;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(RenamePackageRefactoring renamePackage : renamePackageRefactorings) {
			for(PackageLevelRefactoring ref : renamePackage.getMoveClassRefactorings()) {
				pairs.add(new ImmutablePair<String, String>(ref.getOriginalClass().getLocationInfo().getFilePath(), ref.getOriginalClassName()));
			}
		}
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(RenamePackageRefactoring renamePackage : renamePackageRefactorings) {
			for(PackageLevelRefactoring ref : renamePackage.getMoveClassRefactorings()) {
				pairs.add(new ImmutablePair<String, String>(ref.getMovedClass().getLocationInfo().getFilePath(), ref.getMovedClassName()));
			}
		}
		return pairs;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		String originalPath = originalPackage.endsWith(".") ? originalPackage.substring(0, originalPackage.length()-1) : originalPackage;
		sb.append(originalPath);
		sb.append(" to ");
		Set<String> splitPaths = new LinkedHashSet<String>();
		for(String splitPackage : splitPackages) {
			String splitPath = splitPackage.endsWith(".") ? splitPackage.substring(0, splitPackage.length()-1) : splitPackage;
			splitPaths.add(splitPath);
		}
		sb.append(splitPaths);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((originalPackage == null) ? 0 : originalPackage.hashCode());
		result = prime * result + ((splitPackages == null) ? 0 : splitPackages.hashCode());
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
		SplitPackageRefactoring other = (SplitPackageRefactoring) obj;
		if (originalPackage == null) {
			if (other.originalPackage != null)
				return false;
		} else if (!originalPackage.equals(other.originalPackage))
			return false;
		if (splitPackages == null) {
			if (other.splitPackages != null)
				return false;
		} else if (!splitPackages.equals(other.splitPackages))
			return false;
		return true;
	}
}
