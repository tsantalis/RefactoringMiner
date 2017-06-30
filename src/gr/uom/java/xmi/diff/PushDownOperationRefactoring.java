package gr.uom.java.xmi.diff;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class PushDownOperationRefactoring extends MoveOperationRefactoring {

	public PushDownOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		super(bodyMapper);
	}

	public PushDownOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		super(originalOperation, movedOperation);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.PUSH_DOWN_OPERATION;
	}
}
