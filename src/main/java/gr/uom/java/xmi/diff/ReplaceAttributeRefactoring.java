package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public class ReplaceAttributeRefactoring extends MoveAttributeRefactoring implements ReferenceBasedRefactoring {
	private Set<CandidateAttributeRefactoring> attributeRenames;
	
	public ReplaceAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute,
			Set<CandidateAttributeRefactoring> attributeRenames) {
		super(originalAttribute, movedAttribute);
		this.attributeRenames = attributeRenames;
	}

	public Set<AbstractCodeMapping> getReferences() {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		for(CandidateAttributeRefactoring candidate : attributeRenames) {
			references.addAll(candidate.getReferences());
		}
		return references;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalAttribute.toQualifiedString());
		sb.append(" from class ");
		sb.append(getSourceClassName());
		sb.append(" with ");
		sb.append(movedAttribute.toQualifiedString());
		sb.append(" from class ");
		sb.append(getTargetClassName());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.REPLACE_ATTRIBUTE;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(movedAttribute.codeRange()
				.setDescription("replaced attribute declaration")
				.setCodeElement(movedAttribute.toString()));
		return ranges;
	}
}
