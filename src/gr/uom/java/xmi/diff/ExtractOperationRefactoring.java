package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation extractedFromOperation;
	private String sourceClassName;

	public ExtractOperationRefactoring(UMLOperation extractedOperation, UMLOperation extractedFromOperation, String sourceClassName) {
		this.extractedOperation = extractedOperation;
		this.extractedFromOperation = extractedFromOperation;
		this.sourceClassName = sourceClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedOperation);
		sb.append(" extracted from ");
		sb.append(extractedFromOperation);
		sb.append(" in class ");
		sb.append(sourceClassName);
		return sb.toString();
	}

	public UMLOperation getExtractedOperation() {
		return extractedOperation;
	}

	public UMLOperation getExtractedFromOperation() {
		return extractedFromOperation;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_OPERATION;
	}
	
}
