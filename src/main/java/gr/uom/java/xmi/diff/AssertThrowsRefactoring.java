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
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;

public class AssertThrowsRefactoring implements Refactoring, LeafMappingProvider {
	private Set<AbstractCodeMapping> assertThrowsMappings;
	private AbstractCall assertThrowsCall;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private List<LeafMapping> subExpressionMappings;

	public AssertThrowsRefactoring(Set<AbstractCodeMapping> assertThrowsMappings, AbstractCall assertThrowsCall,
			VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		this.assertThrowsMappings = assertThrowsMappings;
		this.assertThrowsCall = assertThrowsCall;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.subExpressionMappings = new ArrayList<LeafMapping>();
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

	public Set<AbstractCodeMapping> getAssertThrowsMappings() {
		return assertThrowsMappings;
	}

	public AbstractCall getAssertThrowsCall() {
		return assertThrowsCall;
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
		for(AbstractCodeMapping mapping : assertThrowsMappings) {
			AbstractCodeFragment fragment = mapping.getFragment1();
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
		for(AbstractCodeMapping mapping : assertThrowsMappings) {
			AbstractCodeFragment fragment = mapping.getFragment2();
			ranges.add(fragment.codeRange()
					.setDescription("assertThrows code")
					.setCodeElement(fragment.getString()));
		}
		String elementType = operationAfter.getElementType();
		ranges.add(operationAfter.codeRange()
				.setDescription(elementType + " declaration with introduced assertThrows")
				.setCodeElement(operationAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.ASSERT_THROWS;
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
		String string = assertThrowsCall.actualString();
		sb.append(string.contains("\n") ? string.substring(0, string.indexOf("\n")) : string);
		String elementType = operationAfter.getElementType();
		sb.append(" in " + elementType + " ");
		sb.append(operationAfter);
		sb.append(" from class ");
		sb.append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(assertThrowsCall, assertThrowsMappings, operationAfter, operationBefore);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssertThrowsRefactoring other = (AssertThrowsRefactoring) obj;
		return Objects.equals(assertThrowsCall, other.assertThrowsCall)
				&& Objects.equals(assertThrowsMappings, other.assertThrowsMappings)
				&& Objects.equals(operationAfter, other.operationAfter)
				&& Objects.equals(operationBefore, other.operationBefore);
	}
}
