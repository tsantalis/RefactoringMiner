package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class RenamePackageRefactoring implements Refactoring {

	private List<PackageLevelRefactoring> moveClassRefactorings;
	private RenamePattern pattern;
	
	public RenamePackageRefactoring(RenamePattern pattern) {
		this.moveClassRefactorings = new ArrayList<PackageLevelRefactoring>();
		this.pattern = pattern;
	}

	public RenamePackageRefactoring(PackageLevelRefactoring moveClassRefactoring) {
		this.moveClassRefactorings = new ArrayList<PackageLevelRefactoring>();
		this.moveClassRefactorings.add(moveClassRefactoring);
		this.pattern = moveClassRefactoring.getRenamePattern();
	}

	public void addMoveClassRefactoring(PackageLevelRefactoring moveClassRefactoring) {
		moveClassRefactorings.add(moveClassRefactoring);
	}

	public RenamePattern getPattern() {
		return pattern;
	}

	public List<PackageLevelRefactoring> getMoveClassRefactorings() {
		return moveClassRefactorings;
	}

	public RefactoringType getRefactoringType() {
		String originalPath = pattern.getBefore().endsWith(".") ? pattern.getBefore().substring(0, pattern.getBefore().length()-1) : pattern.getBefore();
		String movedPath = pattern.getAfter().endsWith(".") ? pattern.getAfter().substring(0, pattern.getAfter().length()-1) : pattern.getAfter();
		if(originalPath.contains(".") && movedPath.contains(".")) {
			String prefix1 = originalPath.substring(0, originalPath.lastIndexOf("."));
			String prefix2 = movedPath.substring(0, movedPath.lastIndexOf("."));
			if(prefix1.equals(prefix2)) {
				return RefactoringType.RENAME_PACKAGE;
			}
		}
		else if(!originalPath.contains(".") && !movedPath.contains(".")) {
			return RefactoringType.RENAME_PACKAGE;
		}
		return RefactoringType.MOVE_PACKAGE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		String originalPath = pattern.getBefore().endsWith(".") ? pattern.getBefore().substring(0, pattern.getBefore().length()-1) : pattern.getBefore();
		sb.append(originalPath);
		sb.append(" to ");
		String movedPath = pattern.getAfter().endsWith(".") ? pattern.getAfter().substring(0, pattern.getAfter().length()-1) : pattern.getAfter();
		sb.append(movedPath);
		return sb.toString();
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(PackageLevelRefactoring ref : moveClassRefactorings) {
			pairs.add(new ImmutablePair<String, String>(ref.getOriginalClass().getLocationInfo().getFilePath(), ref.getOriginalClassName()));
		}
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(PackageLevelRefactoring ref : moveClassRefactorings) {
			pairs.add(new ImmutablePair<String, String>(ref.getMovedClass().getLocationInfo().getFilePath(), ref.getMovedClassName()));
		}
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(PackageLevelRefactoring ref : moveClassRefactorings) {
			ranges.add(ref.getOriginalClass().codeRange()
					.setDescription("original type declaration")
					.setCodeElement(ref.getOriginalClass().getName()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(PackageLevelRefactoring ref : moveClassRefactorings) {
			ranges.add(ref.getMovedClass().codeRange()
					.setDescription("moved type declaration")
					.setCodeElement(ref.getMovedClass().getName()));
		}
		return ranges;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
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
		RenamePackageRefactoring other = (RenamePackageRefactoring) obj;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		return true;
	}
}
