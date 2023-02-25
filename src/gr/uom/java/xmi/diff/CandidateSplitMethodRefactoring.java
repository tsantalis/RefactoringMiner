package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class CandidateSplitMethodRefactoring {
	private Set<VariableDeclarationContainer> splitMethods = new LinkedHashSet<>();
	private Set<UMLOperationBodyMapper> mappers = new LinkedHashSet<>();
	private VariableDeclarationContainer originalMethodBeforeSplit;

	public Set<VariableDeclarationContainer> getSplitMethods() {
		return splitMethods;
	}

	public void addSplitMethod(VariableDeclarationContainer method) {
		splitMethods.add(method);
	}

	public Set<UMLOperationBodyMapper> getMappers() {
		return mappers;
	}

	public void addMapper(UMLOperationBodyMapper mapper) {
		mappers.add(mapper);
	}

	public VariableDeclarationContainer getOriginalMethodBeforeSplit() {
		return originalMethodBeforeSplit;
	}

	public void setOriginalMethodBeforeSplit(VariableDeclarationContainer method) {
		this.originalMethodBeforeSplit = method;
	}

	public boolean equals(CandidateSplitMethodRefactoring candidate) {
		return this.splitMethods.containsAll(candidate.splitMethods) &&
				candidate.splitMethods.containsAll(this.splitMethods) &&
				this.originalMethodBeforeSplit.equals(candidate.originalMethodBeforeSplit);
	}
}
