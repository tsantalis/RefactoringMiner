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
	private UMLOperation sourceOperationBeforeExtraction;
	private UMLOperation sourceOperationAfterExtraction;
	private Set<Replacement> replacements;
	
	public ExtractAndMoveOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation sourceOperationAfterExtraction) {
		this.extractedOperation = bodyMapper.getOperation2();
		this.sourceOperationBeforeExtraction = bodyMapper.getOperation1();
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.replacements = bodyMapper.getReplacements();
	}

	public ExtractAndMoveOperationRefactoring(UMLOperation extractedOperation,
			UMLOperation sourceOperationBeforeExtraction, UMLOperation sourceOperationAfterExtraction) {
		this.extractedOperation = extractedOperation;
		this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedOperation);
		sb.append(" extracted from ");
		sb.append(sourceOperationBeforeExtraction);
		sb.append(" in class ");
		sb.append(sourceOperationBeforeExtraction.getClassName());
		sb.append(" & moved to class ");
		sb.append(extractedOperation.getClassName());
		return sb.toString();
	}

	@Override
	public String getRefactoredClass () {
		return sourceOperationBeforeExtraction.getClassName();
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

	public UMLOperation getSourceOperationBeforeExtraction() {
		return sourceOperationBeforeExtraction;
	}

	public UMLOperation getSourceOperationAfterExtraction() {
		return sourceOperationAfterExtraction;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}
}
