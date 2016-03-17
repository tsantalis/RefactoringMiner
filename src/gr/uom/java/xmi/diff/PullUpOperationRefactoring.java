package gr.uom.java.xmi.diff;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;

public class PullUpOperationRefactoring extends MoveOperationRefactoring {

	public PullUpOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		super(originalOperation, movedOperation);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.PULL_UP_OPERATION;
	}
	
}
