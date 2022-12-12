package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;

public class MergeConditionalRefactoring implements Refactoring {
	private Set<AbstractCodeFragment> mergedConditionals;
	private AbstractCodeFragment newConditional;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	
	public MergeConditionalRefactoring(Set<AbstractCodeFragment> mergedConditionals,
			AbstractCodeFragment newConditional, VariableDeclarationContainer operationBefore,
			VariableDeclarationContainer operationAfter) {
		this.mergedConditionals = mergedConditionals;
		this.newConditional = newConditional;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public Set<AbstractCodeFragment> getMergedConditionals() {
		return mergedConditionals;
	}

	public AbstractCodeFragment getNewConditional() {
		return newConditional;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(AbstractCodeFragment mergedConditional : mergedConditionals) {
			ranges.add(mergedConditional.codeRange()
					.setDescription("merged conditional")
					.setCodeElement(mergedConditional.toString()));
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
		ranges.add(newConditional.codeRange()
				.setDescription("new conditional")
				.setCodeElement(newConditional.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with merged conditional")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MERGE_CONDITIONAL;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append("[");
		int i = 0;
		for(AbstractCodeFragment splitConditional : mergedConditionals) {
			String conditionalString = splitConditional.getString();
			String newConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
			sb.append(newConditional);
			if(i < mergedConditionals.size()-1) {
				sb.append(", ");
			}
			i++;
		}
		sb.append("]");
		sb.append(" to ");
		String conditionalString = newConditional.getString();
		String oldConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
		sb.append(oldConditional);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(mergedConditionals, newConditional, operationAfter, operationBefore);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MergeConditionalRefactoring other = (MergeConditionalRefactoring) obj;
		return Objects.equals(mergedConditionals, other.mergedConditionals)
				&& Objects.equals(newConditional, other.newConditional)
				&& Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore);
	}
}
