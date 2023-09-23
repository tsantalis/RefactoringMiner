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
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class MergeCatchRefactoring implements Refactoring {
	private Set<AbstractCodeFragment> mergedCatchBlocks;
	private AbstractCodeFragment newCatchBlock;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	
	public MergeCatchRefactoring(Set<AbstractCodeFragment> mergedCatchBlocks, AbstractCodeFragment newCatchBlock,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.mergedCatchBlocks = mergedCatchBlocks;
		this.newCatchBlock = newCatchBlock;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public Set<AbstractCodeFragment> getMergedCatchBlocks() {
		return mergedCatchBlocks;
	}

	public AbstractCodeFragment getNewCatchBlock() {
		return newCatchBlock;
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
		for(AbstractCodeFragment mergedConditional : mergedCatchBlocks) {
			ranges.add(mergedConditional.codeRange()
					.setDescription("merged catch block")
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
		ranges.add(newCatchBlock.codeRange()
				.setDescription("new catch block")
				.setCodeElement(newCatchBlock.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with merged catch blocks")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MERGE_CATCH;
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
		for(AbstractCodeFragment mergedCatch : mergedCatchBlocks) {
			String catchString = mergedCatch.getString();
			VariableDeclaration exceptionDeclaration = mergedCatch.getVariableDeclarations().get(0);
			catchString = catchString.replace(exceptionDeclaration.getVariableName() + ")", exceptionDeclaration.getType() + " " +  exceptionDeclaration.getVariableName() + ")");
			sb.append(catchString);
			if(i < mergedCatchBlocks.size()-1) {
				sb.append(", ");
			}
			i++;
		}
		sb.append("]");
		sb.append(" to ");
		String catchString = newCatchBlock.getString();
		VariableDeclaration exceptionDeclaration = newCatchBlock.getVariableDeclarations().get(0);
		catchString = catchString.replace(exceptionDeclaration.getVariableName() + ")", exceptionDeclaration.getType() + " " +  exceptionDeclaration.getVariableName() + ")");
		sb.append(catchString);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(mergedCatchBlocks, newCatchBlock, operationAfter, operationBefore);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MergeCatchRefactoring other = (MergeCatchRefactoring) obj;
		return Objects.equals(mergedCatchBlocks, other.mergedCatchBlocks)
				&& Objects.equals(newCatchBlock, other.newCatchBlock)
				&& Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore);
	}
}
