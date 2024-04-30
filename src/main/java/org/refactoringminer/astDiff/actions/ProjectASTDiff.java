package org.refactoringminer.astDiff.actions;

import org.refactoringminer.api.Refactoring;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectASTDiff {
	private final Set<ASTDiff> diffSet = new LinkedHashSet<>();
	private final Set<ASTDiff> moveDiffSet = new LinkedHashSet<>();
	private Map<String, String> fileContentsBefore;
	private Map<String, String> fileContentsAfter;
	private List<Refactoring> refactorings;

	public ProjectASTDiff(Map<String, String> fileContentsBefore, Map<String, String> fileContentsAfter) {
		this.fileContentsBefore = fileContentsBefore;
		this.fileContentsAfter = fileContentsAfter;
	}

	public Map<String, String> getFileContentsBefore() {
		return fileContentsBefore;
	}

	public Map<String, String> getFileContentsAfter() {
		return fileContentsAfter;
	}

	public Set<ASTDiff> getDiffSet() {
		return diffSet;
	}

	public Set<ASTDiff> getMoveDiffSet() {
		return moveDiffSet;
	}

	public void addASTDiff(ASTDiff diff) {
		diffSet.add(diff);
	}

	public void addMoveASTDiff(ASTDiff diff) {
		moveDiffSet.add(diff);
	}

	public void setRefactorings(List<Refactoring> refactorings) {this.refactorings = refactorings;}

	public List<Refactoring> getRefactorings() { return refactorings;}
}
