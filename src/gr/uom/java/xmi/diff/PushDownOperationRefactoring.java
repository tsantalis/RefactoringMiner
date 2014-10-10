package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

public class PushDownOperationRefactoring extends MoveOperationRefactoring {

	public PushDownOperationRefactoring(UMLOperation originalOperation, UMLOperation movedOperation) {
		super(originalOperation, movedOperation);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Push Down Operation";
	}
}
