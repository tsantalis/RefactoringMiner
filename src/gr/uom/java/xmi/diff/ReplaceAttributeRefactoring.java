package gr.uom.java.xmi.diff;

import java.util.Set;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;

public class ReplaceAttributeRefactoring extends MoveAttributeRefactoring {
private Set<CandidateAttributeRefactoring> attributeRenames;
	
	public ReplaceAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute,
			Set<CandidateAttributeRefactoring> attributeRenames) {
		super(originalAttribute, movedAttribute);
		this.attributeRenames = attributeRenames;
	}

	public Set<CandidateAttributeRefactoring> getAttributeRenames() {
		return attributeRenames;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalAttribute);
		sb.append(" from class ");
		sb.append(getSourceClassName());
		sb.append(" with ");
		sb.append(movedAttribute);
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
}
