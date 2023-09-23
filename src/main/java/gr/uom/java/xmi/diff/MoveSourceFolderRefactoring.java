package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class MoveSourceFolderRefactoring implements Refactoring {
	private List<MovedClassToAnotherSourceFolder> movedClassesToAnotherSourceFolder = new ArrayList<MovedClassToAnotherSourceFolder>();
	private Map<String, String> identicalFilePaths = new HashMap<String, String>();
	private RenamePattern pattern;
	
	public MoveSourceFolderRefactoring(RenamePattern pattern) {
		this.pattern = pattern;
	}

	public MoveSourceFolderRefactoring(MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder) {
		this.movedClassesToAnotherSourceFolder.add(movedClassToAnotherSourceFolder);
		this.pattern = movedClassToAnotherSourceFolder.getRenamePattern();
	}

	public void putIdenticalFilePaths(String filePathBefore, String filePathAfter) {
		identicalFilePaths.put(filePathBefore, filePathAfter);
	}

	public void addMovedClassToAnotherSourceFolder(MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder) {
		movedClassesToAnotherSourceFolder.add(movedClassToAnotherSourceFolder);
	}

	public List<MovedClassToAnotherSourceFolder> getMovedClassesToAnotherSourceFolder() {
		return movedClassesToAnotherSourceFolder;
	}

	public Map<String, String> getIdenticalFilePaths() {
		return identicalFilePaths;
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

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
			pairs.add(new ImmutablePair<String, String>(ref.getOriginalClass().getLocationInfo().getFilePath(), ref.getOriginalClassName()));
		}
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		for(MovedClassToAnotherSourceFolder ref : movedClassesToAnotherSourceFolder) {
			pairs.add(new ImmutablePair<String, String>(ref.getMovedClass().getLocationInfo().getFilePath(), ref.getMovedClassName()));
		}
		return pairs;
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
