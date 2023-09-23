package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;

public class ReplaceLoopWithPipelineRefactoring implements Refactoring {
	private Set<AbstractCodeFragment> codeFragmentsBefore;
	private Set<AbstractCodeFragment> codeFragmentsAfter;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;

	public ReplaceLoopWithPipelineRefactoring(Set<AbstractCodeFragment> codeFragmentsBefore,
			Set<AbstractCodeFragment> codeFragmentsAfter, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.codeFragmentsBefore = codeFragmentsBefore;
		this.codeFragmentsAfter = codeFragmentsAfter;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}
	
	public Set<AbstractCodeFragment> getCodeFragmentsBefore() {
		return codeFragmentsBefore;
	}

	public Set<AbstractCodeFragment> getCodeFragmentsAfter() {
		return codeFragmentsAfter;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		for(AbstractCodeFragment fragment : codeFragmentsBefore) {
			if(fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
					fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
					fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
					fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
				sb.append(fragment.getString());
				break;
			}
		}
		sb.append(" with ");
		String pipeline = codeFragmentsAfter.iterator().next().getString();
		sb.append(pipeline.contains("\n") ? pipeline.substring(0, pipeline.indexOf("\n")) : pipeline);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ");
		sb.append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(AbstractCodeFragment fragment : codeFragmentsBefore) {
			ranges.add(fragment.codeRange()
					.setDescription("original code")
					.setCodeElement(fragment.getString()));
		}
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(AbstractCodeFragment fragment : codeFragmentsAfter) {
			ranges.add(fragment.codeRange()
					.setDescription("pipeline code")
					.setCodeElement(fragment.getString()));
		}
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with introduced pipeline")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.REPLACE_LOOP_WITH_PIPELINE;
	}

	@Override
	public String getName() {
		return getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
		return pairs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((codeFragmentsAfter == null) ? 0 : codeFragmentsAfter.hashCode());
		result = prime * result + ((codeFragmentsBefore == null) ? 0 : codeFragmentsBefore.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReplaceLoopWithPipelineRefactoring other = (ReplaceLoopWithPipelineRefactoring) obj;
		if (codeFragmentsAfter == null) {
			if (other.codeFragmentsAfter != null)
				return false;
		} else if (!codeFragmentsAfter.equals(other.codeFragmentsAfter))
			return false;
		if (codeFragmentsBefore == null) {
			if (other.codeFragmentsBefore != null)
				return false;
		} else if (!codeFragmentsBefore.equals(other.codeFragmentsBefore))
			return false;
		if (operationAfter == null) {
			if (other.operationAfter != null)
				return false;
		} else if (!operationAfter.equals(other.operationAfter))
			return false;
		if (operationBefore == null) {
			if (other.operationBefore != null)
				return false;
		} else if (!operationBefore.equals(other.operationBefore))
			return false;
		return true;
	}
}
