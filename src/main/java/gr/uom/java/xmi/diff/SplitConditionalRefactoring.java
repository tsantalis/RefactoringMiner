package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LeafMapping;

public class SplitConditionalRefactoring extends AbstractRefactoring implements MethodLevelRefactoring, LeafMappingProvider {
	private AbstractCodeFragment originalConditional;
	private Set<AbstractCodeFragment> splitConditionals;
	private VariableDeclarationContainer operationBefore;
	private VariableDeclarationContainer operationAfter;
	private List<LeafMapping> subExpressionMappings;

	public SplitConditionalRefactoring(AbstractCodeFragment originalConditional,
			Set<AbstractCodeFragment> splitConditionals, VariableDeclarationContainer operationBefore,
			VariableDeclarationContainer operationAfter) {
		this.originalConditional = originalConditional;
		this.splitConditionals = splitConditionals;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.subExpressionMappings = new ArrayList<LeafMapping>();
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return operationBefore;
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return operationAfter;
	}

	public Optional<String> getTemplateParameterBefore() {
		String conditionalString = originalConditional.getString();
		String oldConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
		return Optional.of(oldConditional);
	}

	public String getTemplateParameterAfter() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		int i = 0;
		for(AbstractCodeFragment splitConditional : splitConditionals) {
			String conditionalString = splitConditional.getString();
			String newConditional = (conditionalString.contains("\n") ? conditionalString.substring(0, conditionalString.indexOf("\n")) : conditionalString);
			sb.append(newConditional);
			if(i < splitConditionals.size()-1) {
				sb.append(", ");
			}
			i++;
		}
		sb.append("]");
		return sb.toString();
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
