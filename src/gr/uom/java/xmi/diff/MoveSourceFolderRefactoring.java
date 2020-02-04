package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
		String originalPath = pattern.getBefore().endsWith("/") ? pattern.getBefore().substring(0, pattern.getBefore().length()-1) : pattern.getBefore();
		sb.append(originalPath);
		sb.append(" to ");
		String movedPath = pattern.getAfter().endsWith("/") ? pattern.getAfter().substring(0, pattern.getAfter().length()-1) : pattern.getAfter();
		sb.append(movedPath);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_SOURCE_FOLDER;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		for(MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
			classNames.add(ref.getOriginalClassName());
		}
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		for(MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
			classNames.add(ref.getMovedClassName());
		}
		return classNames;
	}

	public Set<String> getInvolvedFilesBeforeRefactoring() {
		return movedClassesToAnotherSourceFolder.stream().map(x -> x.getOriginalClass().getLocationInfo().getFilePath()).collect(Collectors.toSet());
	}

	public Set<String> getInvolvedFilesAfterRefactoring() {
		return movedClassesToAnotherSourceFolder.stream().map(x -> x.getMovedClass().getLocationInfo().getFilePath()).collect(Collectors.toSet());
	}


	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
			ranges.add(ref.getOriginalClass().codeRange()
					.setDescription("original type declaration")
					.setCodeElement(ref.getOriginalClass().getName()));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
			ranges.add(ref.getMovedClass().codeRange()
					.setDescription("moved type declaration")
					.setCodeElement(ref.getMovedClass().getName()));
		}
		return ranges;
	}
}
