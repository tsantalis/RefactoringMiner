package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation extractedFromOperation;
	private String sourceClassName;
	private Set<Replacement> replacements;

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.extractedOperation = bodyMapper.getOperation2();
		this.extractedFromOperation = bodyMapper.getOperation1();
		this.sourceClassName = bodyMapper.getOperation1().getClassName();
		this.replacements = bodyMapper.getReplacements();
	}

	public ExtractOperationRefactoring(UMLOperation extractedOperation, UMLOperation extractedFromOperation, String sourceClassName) {
		this.extractedOperation = extractedOperation;
		this.extractedFromOperation = extractedFromOperation;
		this.sourceClassName = sourceClassName;
		this.replacements = new LinkedHashSet<Replacement>();
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

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_OPERATION;
	}
	
}
