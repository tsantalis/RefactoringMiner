package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class MoveOperationRefactoring implements Refactoring {
	protected UMLOperation originalOperation;
	protected UMLOperation movedOperation;
	private Set<Replacement> replacements;

	public MoveOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.originalOperation = bodyMapper.getOperation1();
		this.movedOperation = bodyMapper.getOperation2();
		this.replacements = bodyMapper.getReplacements();
	}

	public MoveOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		this.originalOperation = originalOperation;
		this.movedOperation = movedOperation;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(originalOperation);
		sb.append(" from class ");
		sb.append(originalOperation.getClassName());
		sb.append(" to ");
		sb.append(movedOperation);
		sb.append(" from class ");
		sb.append(movedOperation.getClassName());
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_OPERATION;
	}

	public UMLOperation getOriginalOperation() {
		return originalOperation;
	}

	public UMLOperation getMovedOperation() {
		return movedOperation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}
}
