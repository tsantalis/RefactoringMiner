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
import gr.uom.java.xmi.decomposition.LeafMapping;

public class ReplaceConditionalWithTernaryRefactoring implements Refactoring, LeafMappingProvider {
	private AbstractCodeFragment originalConditional;
	private AbstractCodeFragment ternaryConditional;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private List<LeafMapping> subExpressionMappings;
	
	public ReplaceConditionalWithTernaryRefactoring(AbstractCodeFragment originalConditional, AbstractCodeFragment ternaryConditional,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.originalConditional = originalConditional;
		this.ternaryConditional = ternaryConditional;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.subExpressionMappings = new ArrayList<LeafMapping>();
	}

	public AbstractCodeFragment getOriginalConditional() {
		return originalConditional;
	}

	public AbstractCodeFragment getTernaryConditional() {
		return ternaryConditional;
	}

	public VariableDeclarationContainer getOperationBefore() {
		return operationBefore;
	}

	public VariableDeclarationContainer getOperationAfter() {
		return operationAfter;
	}

	public void addSubExpressionMapping(LeafMapping newLeafMapping) {
		boolean alreadyPresent = false; 
		for(LeafMapping oldLeafMapping : subExpressionMappings) { 
			if(oldLeafMapping.getFragment1().getLocationInfo().equals(newLeafMapping.getFragment1().getLocationInfo()) && 
					oldLeafMapping.getFragment2().getLocationInfo().equals(newLeafMapping.getFragment2().getLocationInfo())) { 
				alreadyPresent = true; 
				break; 
			} 
		} 
		if(!alreadyPresent) { 
			subExpressionMappings.add(newLeafMapping); 
		}
	}

	public List<LeafMapping> getSubExpressionMappings() {
		return subExpressionMappings;
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
		ranges.add(ternaryConditional.codeRange()
				.setDescription("ternary conditional")
				.setCodeElement(ternaryConditional.toString()));
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with ternary conditional")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.REPLACE_CONDITIONAL_WITH_TERNARY;
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
		conditionalString = ternaryConditional.getString();
		String newConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
		sb.append(newConditional);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(ternaryConditional, operationAfter, operationBefore, originalConditional);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReplaceConditionalWithTernaryRefactoring other = (ReplaceConditionalWithTernaryRefactoring) obj;
		return Objects.equals(ternaryConditional, other.ternaryConditional)
				&& Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore)
				&& Objects.equals(originalConditional, other.originalConditional);
	}
}
