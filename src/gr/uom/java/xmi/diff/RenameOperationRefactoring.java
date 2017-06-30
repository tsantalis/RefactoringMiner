package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class RenameOperationRefactoring implements Refactoring {
	private UMLOperation originalOperation;
	private UMLOperation renamedOperation;
	private Set<Replacement> replacements;
	
	public RenameOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.originalOperation = bodyMapper.getOperation1();
		this.renamedOperation = bodyMapper.getOperation2();
		this.replacements = bodyMapper.getReplacements();
	}

	public RenameOperationRefactoring(UMLOperation originalOperation, UMLOperation renamedOperation) {
		this.originalOperation = originalOperation;
		this.renamedOperation = renamedOperation;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalOperation);
		sb.append(" renamed to ");
		sb.append(renamedOperation);
		sb.append(" in class ").append(originalOperation.getClassName());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_METHOD;
	}

	public UMLOperation getOriginalOperation() {
		return originalOperation;
	}

	public UMLOperation getRenamedOperation() {
		return renamedOperation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}
}
