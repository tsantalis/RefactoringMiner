package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.TryStatementObject;

public class TryWithResourcesRefactoring implements Refactoring {
	private TryStatementObject tryBefore;
	private TryStatementObject tryAfter;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;

	public TryWithResourcesRefactoring(TryStatementObject tryBefore, TryStatementObject tryAfter,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.tryBefore = tryBefore;
		this.tryAfter = tryAfter;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public TryStatementObject getTryBefore() {
		return tryBefore;
	}

	public TryStatementObject getTryAfter() {
		return tryAfter;
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
		ranges.add(tryBefore.codeRange()
				.setDescription("original try block")
				.setCodeElement(tryBefore.toString()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(tryAfter.codeRange()
				.setDescription("try-with-resources block")
				.setCodeElement(tryAfter.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with try-with-resources")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.TRY_WITH_RESOURCES;
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(tryAfter.toString());
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ");
		sb.append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(tryBefore, tryAfter, operationAfter, operationBefore);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TryWithResourcesRefactoring other = (TryWithResourcesRefactoring) obj;
		return Objects.equals(tryBefore, other.tryBefore)
				&& Objects.equals(tryAfter, other.tryAfter)
				&& Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore);
	}
}
