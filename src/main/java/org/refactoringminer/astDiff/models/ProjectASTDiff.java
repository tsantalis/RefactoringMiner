package org.refactoringminer.astDiff.models;

import com.github.gumtreediff.tree.TreeContext;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectASTDiff {
	private final Set<ASTDiff> diffSet = new LinkedHashSet<>();
	private final Set<ASTDiff> moveDiffSet = new LinkedHashSet<>();
	private final Map<String, String> fileContentsBefore;
	private final Map<String, String> fileContentsAfter;
	private List<Refactoring> refactorings;
	private Map<String, TreeContext> parentContextMap;
	private Map<String, TreeContext> childContextMap;
	private UMLModelDiff modelDiff;

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
	public void addMoveASTDiff(Set<ASTDiff> diffs) {
		moveDiffSet.addAll(diffs);
	}

	public void setRefactorings(List<Refactoring> refactorings) {this.refactorings = refactorings;}

	public List<Refactoring> getRefactorings() { return refactorings;}

	public void setModelDiff(UMLModelDiff modelDiff) {
		this.modelDiff = modelDiff;
	}

	public UMLModelDiff getModelDiff() {
		return modelDiff;
	}

	public void setParentContextMap(Map<String, TreeContext> treeContextMap) {
		this.parentContextMap = treeContextMap;
	}

	public void setChildContextMap(Map<String, TreeContext> treeContextMap) {
		this.childContextMap = treeContextMap;
	}

	public Map<String, TreeContext> getParentContextMap() {
		return parentContextMap;
	}

	public Map<String, TreeContext> getChildContextMap() {
		return childContextMap;
	}
}
