package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class InlineOperationRefactoring implements Refactoring {
	private UMLOperation inlinedOperation;
	private UMLOperation inlinedToOperation;
	private String sourceClassName;
	private Set<Replacement> replacements;
	
	public InlineOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		this.inlinedOperation = bodyMapper.getOperation1();
		this.inlinedToOperation = bodyMapper.getOperation2();
		this.sourceClassName = bodyMapper.getOperation2().getClassName();
		this.replacements = bodyMapper.getReplacements();
	}

	public InlineOperationRefactoring(UMLOperation inlinedOperation, UMLOperation inlinedToOperation, String sourceClassName) {
		this.inlinedOperation = inlinedOperation;
		this.inlinedToOperation = inlinedToOperation;
		this.sourceClassName = sourceClassName;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(inlinedOperation);
		sb.append(" inlined to ");
		sb.append(inlinedToOperation);
		sb.append(" in class ");
		sb.append(sourceClassName);
		return sb.toString();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.INLINE_OPERATION;
	}

	public UMLOperation getInlinedOperation() {
		return inlinedOperation;
	}

	public UMLOperation getInlinedToOperation() {
		return inlinedToOperation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}
}
