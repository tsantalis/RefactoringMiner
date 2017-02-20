package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

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
		String originalPath = pattern.getOriginalPath().endsWith(".") ? pattern.getOriginalPath().substring(0, pattern.getOriginalPath().length()-1) : pattern.getOriginalPath();
		sb.append(originalPath);
		sb.append(" to ");
		String movedPath = pattern.getMovedPath().endsWith(".") ? pattern.getMovedPath().substring(0, pattern.getMovedPath().length()-1) : pattern.getMovedPath();
		sb.append(movedPath);
		return sb.toString();
	}
}
