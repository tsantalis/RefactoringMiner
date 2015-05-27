package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

public class PushDownOperationRefactoring extends MoveOperationRefactoring {

	public PushDownOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		super(originalOperation, movedOperation);
		// TODO Auto-generated constructor stub
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.PUSH_DOWN_OPERATION;
	}
}
