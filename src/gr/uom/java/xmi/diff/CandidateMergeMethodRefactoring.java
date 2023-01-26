package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class CandidateMergeMethodRefactoring {
	private Set<VariableDeclarationContainer> mergedMethods = new LinkedHashSet<>();
	private Set<UMLOperationBodyMapper> mappers = new LinkedHashSet<>();
	private VariableDeclarationContainer newMethodAfterMerge;

	public Set<VariableDeclarationContainer> getMergedMethods() {
		return mergedMethods;
	}

	public void addMergedMethod(VariableDeclarationContainer method) {
		mergedMethods.add(method);
	}

	public Set<UMLOperationBodyMapper> getMappers() {
		return mappers;
	}

	public void addMapper(UMLOperationBodyMapper mapper) {
		mappers.add(mapper);
	}

	public VariableDeclarationContainer getNewMethodAfterMerge() {
		return newMethodAfterMerge;
	}

	public void setNewMethodAfterMerge(VariableDeclarationContainer method) {
		this.newMethodAfterMerge = method;
	}

	public boolean equals(CandidateMergeMethodRefactoring candidate) {
		return this.mergedMethods.containsAll(candidate.mergedMethods) &&
				candidate.mergedMethods.containsAll(this.mergedMethods) &&
				this.newMethodAfterMerge.equals(candidate.newMethodAfterMerge);
	}
}
