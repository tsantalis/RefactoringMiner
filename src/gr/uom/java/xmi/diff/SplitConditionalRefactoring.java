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

public class SplitConditionalRefactoring implements Refactoring {
	private AbstractCodeFragment originalConditional;
	private Set<AbstractCodeFragment> splitConditionals;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;

	public SplitConditionalRefactoring(AbstractCodeFragment originalConditional,
			Set<AbstractCodeFragment> splitConditionals, VariableDeclarationContainer operationBefore,
			VariableDeclarationContainer operationAfter) {
		this.originalConditional = originalConditional;
		this.splitConditionals = splitConditionals;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public AbstractCodeFragment getOriginalConditional() {
		return originalConditional;
	}

	public Set<AbstractCodeFragment> getSplitConditionals() {
		return splitConditionals;
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
		ranges.add(originalConditional.codeRange()
				.setDescription("original conditional")
				.setCodeElement(originalConditional.toString()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(AbstractCodeFragment splitConditional : splitConditionals) {
			ranges.add(splitConditional.codeRange()
					.setDescription("split conditional")
					.setCodeElement(splitConditional.toString()));
		}
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with split conditional")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.SPLIT_CONDITIONAL;
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
		String conditionalString = originalConditional.getString();
		String oldConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
		sb.append(oldConditional);
		sb.append(" to ");
		sb.append("[");
		int i = 0;
		for(AbstractCodeFragment splitConditional : splitConditionals) {
			conditionalString = splitConditional.getString();
			String newConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
			sb.append(newConditional);
			if(i < splitConditionals.size()-1) {
				sb.append(", ");
			}
			i++;
		}
		sb.append("]");
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(operationAfter, operationBefore, originalConditional, splitConditionals);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SplitConditionalRefactoring other = (SplitConditionalRefactoring) obj;
		return Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore)
				&& Objects.equals(originalConditional, other.originalConditional)
				&& Objects.equals(splitConditionals, other.splitConditionals);
	}
}
