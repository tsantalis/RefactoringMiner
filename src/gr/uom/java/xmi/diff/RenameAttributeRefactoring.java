package gr.uom.java.xmi.diff;

import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class RenameAttributeRefactoring implements Refactoring {
	private VariableDeclaration originalAttribute;
	private VariableDeclaration renamedAttribute;
	private Set<CandidateAttributeRename> attributeRenames;
	private String classNameBefore;
	private String classNameAfter;

	public RenameAttributeRefactoring(VariableDeclaration originalAttribute, VariableDeclaration renamedAttribute,
			String classNameBefore, String classNameAfter, Set<CandidateAttributeRename> attributeRenames) {
		this.originalAttribute = originalAttribute;
		this.renamedAttribute = renamedAttribute;
		this.classNameBefore = classNameBefore;
		this.classNameAfter = classNameAfter;
		this.attributeRenames = attributeRenames;
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalAttribute);
		sb.append(" to ");
		sb.append(renamedAttribute);
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}
}
