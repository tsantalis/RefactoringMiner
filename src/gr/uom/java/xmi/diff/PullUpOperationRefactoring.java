package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

public class PullUpOperationRefactoring extends MoveOperationRefactoring {

	public PullUpOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		super(originalOperation, movedOperation);
	}

	@Override
	public String getName() {
		return "Pull Up Operation";
	}
}
