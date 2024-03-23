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
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.ObjectCreation;

public class ReplaceGenericWithDiamondRefactoring implements Refactoring {
	private AbstractCodeFragment statementBefore;
	private AbstractCodeFragment statementAfter;
	private AbstractCall creationBefore;
	private AbstractCall creationAfter;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	
	public ReplaceGenericWithDiamondRefactoring(AbstractCodeFragment statementBefore, AbstractCodeFragment statementAfter,
			AbstractCall creationBefore, AbstractCall creationAfter,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.statementBefore = statementBefore;
		this.statementAfter = statementAfter;
		this.creationBefore = creationBefore;
		this.creationAfter = creationAfter;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
	}

	public AbstractCall getCreationBefore() {
		return creationBefore;
	}

	public AbstractCall getCreationAfter() {
		return creationAfter;
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
		ranges.add(creationBefore.codeRange()
				.setDescription("original creation")
				.setCodeElement(creationBefore.toString()));
		String elementType = operationBefore.getElementType();
		ranges.add(operationBefore.codeRange()
				.setDescription("original " + elementType + " declaration")
				.setCodeElement(operationBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(creationAfter.codeRange()
				.setDescription("creation with diamond operator")
				.setCodeElement(creationAfter.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with diamond operator")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.REPLACE_GENERIC_WITH_DIAMOND;
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
		return Objects.hash(statementAfter, statementBefore, operationAfter, operationBefore);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReplaceGenericWithDiamondRefactoring other = (ReplaceGenericWithDiamondRefactoring) obj;
		return Objects.equals(statementAfter, other.statementAfter)
				&& Objects.equals(statementBefore, other.statementBefore)
				&& Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractType(creationBefore));
		sb.append(" with ");
		sb.append(extractType(creationAfter));
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	private static String extractType(AbstractCall c) {
		if(c instanceof ObjectCreation) {
			ObjectCreation creation = (ObjectCreation)c;
			return creation.getType().toString();
		}
		else {
			String s = c.actualString();
			if(s.startsWith("new ")) {
				s = s.substring(4);
			}
			if(s.contains("(")) {
				s = s.substring(0, s.indexOf("("));
			}
			return s;
		}
	}
}
