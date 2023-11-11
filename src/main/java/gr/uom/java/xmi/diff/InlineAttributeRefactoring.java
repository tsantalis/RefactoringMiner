package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;

public class InlineAttributeRefactoring implements Refactoring, ReferenceBasedRefactoring, LeafMappingProvider {
	private UMLAttribute attributeDeclaration;
	private UMLAbstractClass originalClass;
	private UMLAbstractClass nextClass;
	private Set<AbstractCodeMapping> references;
	private List<LeafMapping> subExpressionMappings;
	private boolean insideExtractedOrInlinedMethod;

	public InlineAttributeRefactoring(UMLAttribute variableDeclaration, UMLAbstractClass originalClass, UMLAbstractClass nextClass,
			boolean insideExtractedOrInlinedMethod) {
		this.attributeDeclaration = variableDeclaration;
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.references = new LinkedHashSet<AbstractCodeMapping>();
		this.subExpressionMappings = new ArrayList<LeafMapping>();
		this.insideExtractedOrInlinedMethod = insideExtractedOrInlinedMethod;
	}

	public void addReference(AbstractCodeMapping mapping) {
		references.add(mapping);
	}

	public void addReferences(Set<AbstractCodeMapping> mappings) {
		references.addAll(mappings);
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

	public RefactoringType getRefactoringType() {
		return RefactoringType.INLINE_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLAttribute getVariableDeclaration() {
		return attributeDeclaration;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return references;
	}

	public List<LeafMapping> getSubExpressionMappings() {
		return subExpressionMappings;
	}

	public boolean isInsideExtractedOrInlinedMethod() {
		return insideExtractedOrInlinedMethod;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(attributeDeclaration);
		sb.append(" in class ");
		sb.append(nextClass.getName());
		return sb.toString();
	}

	/**
	 * @return the code range of the inlined variable declaration in the <b>parent</b> commit
	 */
	public CodeRange getInlinedVariableDeclarationCodeRange() {
		return attributeDeclaration.codeRange();
	}

	public UMLAbstractClass getOriginalClass() {
		return originalClass;
	}

	public UMLAbstractClass getNextClass() {
		return nextClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeDeclaration.getVariableDeclaration() == null) ? 0 : attributeDeclaration.getVariableDeclaration().hashCode());
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
		InlineAttributeRefactoring other = (InlineAttributeRefactoring) obj;
		if (attributeDeclaration == null) {
			if (other.attributeDeclaration != null)
				return false;
		} else if (!attributeDeclaration.getVariableDeclaration().equals(other.attributeDeclaration.getVariableDeclaration()))
			return false;
		return true;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(attributeDeclaration.codeRange()
				.setDescription("inlined attribute declaration")
				.setCodeElement(attributeDeclaration.toString()));
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment1().codeRange().setDescription("statement with the name of the inlined attribute"));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment2().codeRange().setDescription("statement with the initializer of the inlined attribute"));
		}
		return ranges;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getNextClass().getLocationInfo().getFilePath(), getNextClass().getName()));
		return pairs;
	}
}
