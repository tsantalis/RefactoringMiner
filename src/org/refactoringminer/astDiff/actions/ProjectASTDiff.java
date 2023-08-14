package org.refactoringminer.astDiff.actions;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ProjectASTDiff {
	private final Set<ASTDiff> diffSet = new LinkedHashSet<>();
	private Map<String, String> fileContentsBefore;
	private Map<String, String> fileContentsAfter;

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

	public void addASTDiff(ASTDiff diff) {
		diffSet.add(diff);
	}
}
