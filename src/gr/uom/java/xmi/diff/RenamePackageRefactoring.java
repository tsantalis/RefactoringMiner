package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class RenamePackageRefactoring implements Refactoring {

	private List<MoveClassRefactoring> moveClassRefactorings;
	private RenamePattern pattern;
	
	public RenamePackageRefactoring(RenamePattern pattern) {
		this.moveClassRefactorings = new ArrayList<MoveClassRefactoring>();
		this.pattern = pattern;
	}

	public RenamePackageRefactoring(MoveClassRefactoring moveClassRefactoring) {
		this.moveClassRefactorings = new ArrayList<MoveClassRefactoring>();
		this.moveClassRefactorings.add(moveClassRefactoring);
		this.pattern = moveClassRefactoring.getRenamePattern();
	}

	public void addMoveClassRefactoring(MoveClassRefactoring moveClassRefactoring) {
		moveClassRefactorings.add(moveClassRefactoring);
	}

	public RenamePattern getPattern() {
		return pattern;
	}

	public List<MoveClassRefactoring> getMoveClassRefactorings() {
		return moveClassRefactorings;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_PACKAGE;
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
		for(MoveClassRefactoring ref : moveClassRefactorings) {
			pairs.add(new ImmutablePair<String, String>(ref.getOriginalClass().getLocationInfo().getFilePath(), ref.getOriginalClassName()));
		}
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(MoveClassRefactoring ref : moveClassRefactorings) {
			pairs.add(new ImmutablePair<String, String>(ref.getMovedClass().getLocationInfo().getFilePath(), ref.getMovedClassName()));
		}
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(MoveClassRefactoring ref : moveClassRefactorings) {
			ranges.add(ref.getOriginalClass().codeRange()
					.setDescription("original type declaration")
					.setCodeElement(ref.getOriginalClass().getName()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(MoveClassRefactoring ref : moveClassRefactorings) {
			ranges.add(ref.getMovedClass().codeRange()
					.setDescription("moved type declaration")
					.setCodeElement(ref.getMovedClass().getName()));
		}
		return ranges;
	}
}
