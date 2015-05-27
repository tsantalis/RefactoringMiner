package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

public class MoveOperationRefactoring implements Refactoring {
	protected UMLOperation originalOperation;
	protected UMLOperation movedOperation;

	public MoveOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		this.originalOperation = originalOperation;
		this.movedOperation = movedOperation;
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
	
}
