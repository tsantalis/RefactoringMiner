package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MergePackageRefactoring implements Refactoring {
	private Set<String> mergedPackages;
	private String newPackage;
	private Set<RenamePackageRefactoring> renamePackageRefactorings;

	public MergePackageRefactoring(Set<RenamePackageRefactoring> renamePackageRefactorings) {
		this.renamePackageRefactorings = renamePackageRefactorings;
		this.mergedPackages = new TreeSet<String>();
		for(RenamePackageRefactoring refactoring : renamePackageRefactorings) {
			RenamePattern pattern = refactoring.getPattern();
			if(newPackage == null) {
				this.newPackage = pattern.getAfter();
			}
			mergedPackages.add(pattern.getBefore());
		}
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
		return RefactoringType.MERGE_PACKAGE;
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
		Set<String> mergedPaths = new LinkedHashSet<String>();
		for(String mergePackage : mergedPackages) {
			String mergePath = mergePackage.endsWith(".") ? mergePackage.substring(0, mergePackage.length()-1) : mergePackage;
			mergedPaths.add(mergePath);
		}
		sb.append(mergedPaths);
		sb.append(" to ");
		String newPath = newPackage.endsWith(".") ? newPackage.substring(0, newPackage.length()-1) : newPackage;
		sb.append(newPath);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mergedPackages == null) ? 0 : mergedPackages.hashCode());
		result = prime * result + ((newPackage == null) ? 0 : newPackage.hashCode());
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
		MergePackageRefactoring other = (MergePackageRefactoring) obj;
		if (mergedPackages == null) {
			if (other.mergedPackages != null)
				return false;
		} else if (!mergedPackages.equals(other.mergedPackages))
			return false;
		if (newPackage == null) {
			if (other.newPackage != null)
				return false;
		} else if (!newPackage.equals(other.newPackage))
			return false;
		return true;
	}
}
