package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class ExtractAndMoveOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation extractedFromOperation;
	private Set<Replacement> replacements;
	
	public ExtractAndMoveOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.extractedOperation = bodyMapper.getOperation2();
		this.extractedFromOperation = bodyMapper.getOperation1();
		this.replacements = bodyMapper.getReplacements();
	}

	public ExtractAndMoveOperationRefactoring(UMLOperation extractedOperation,
			UMLOperation extractedFromOperation) {
		this.extractedOperation = extractedOperation;
		this.extractedFromOperation = extractedFromOperation;
		this.replacements = new LinkedHashSet<Replacement>();
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
	
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_AND_MOVE_OPERATION;
	}
	
	public UMLOperation getExtractedOperation() {
		return extractedOperation;
	}

	public UMLOperation getExtractedFromOperation() {
		return extractedFromOperation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}
}
