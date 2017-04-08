package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveSourceFolderRefactoring implements Refactoring {
	private List<MovedClassToAnotherSourceFolder> movedClassesToAnotherSourceFolder;
	private RenamePattern pattern;
	
	public MoveSourceFolderRefactoring(RenamePattern pattern) {
		this.movedClassesToAnotherSourceFolder = new ArrayList<MovedClassToAnotherSourceFolder>();
		this.pattern = pattern;
	}

	public MoveSourceFolderRefactoring(MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder) {
		this.movedClassesToAnotherSourceFolder = new ArrayList<MovedClassToAnotherSourceFolder>();
		this.movedClassesToAnotherSourceFolder.add(movedClassToAnotherSourceFolder);
		this.pattern = movedClassToAnotherSourceFolder.getRenamePattern();
	}

	public void addMovedClassToAnotherSourceFolder(MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder) {
		movedClassesToAnotherSourceFolder.add(movedClassToAnotherSourceFolder);
	}

	public List<MovedClassToAnotherSourceFolder> getMovedClassesToAnotherSourceFolder() {
		return movedClassesToAnotherSourceFolder;
	}

	public RenamePattern getPattern() {
		return pattern;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		String originalPath = pattern.getOriginalPath().endsWith("/") ? pattern.getOriginalPath().substring(0, pattern.getOriginalPath().length()-1) : pattern.getOriginalPath();
		sb.append(originalPath);
		sb.append(" to ");
		String movedPath = pattern.getMovedPath().endsWith("/") ? pattern.getMovedPath().substring(0, pattern.getMovedPath().length()-1) : pattern.getMovedPath();
		sb.append(movedPath);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_SOURCE_FOLDER;
	}
	
}
