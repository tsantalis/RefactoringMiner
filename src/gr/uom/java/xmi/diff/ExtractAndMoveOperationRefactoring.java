package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

public class ExtractAndMoveOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation extractedFromOperation;
	
	public ExtractAndMoveOperationRefactoring(UMLOperation extractedOperation,
			UMLOperation extractedFromOperation) {
		this.extractedOperation = extractedOperation;
		this.extractedFromOperation = extractedFromOperation;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedOperation);
		sb.append(" extracted from ");
		sb.append(extractedFromOperation);
		sb.append(" in class ");
		sb.append(extractedFromOperation.getClassName());
		sb.append(" & moved to class ");
		sb.append(extractedOperation.getClassName());
		return sb.toString();
	}
	
	@Override
	public String getName() {
		return "Extract & Move Operation";
	}

}
