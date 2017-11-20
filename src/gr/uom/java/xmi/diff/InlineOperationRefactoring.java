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
	private UMLOperation targetOperationAfterInline;
	private UMLOperation targetOperationBeforeInline;
	private Set<Replacement> replacements;
	
	public InlineOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation targetOperationBeforeInline) {
		this.inlinedOperation = bodyMapper.getOperation1();
		this.targetOperationAfterInline = bodyMapper.getOperation2();
		this.targetOperationBeforeInline = targetOperationBeforeInline;
		this.replacements = bodyMapper.getReplacements();
	}

	public InlineOperationRefactoring(UMLOperation inlinedOperation, UMLOperation targetOperationAfterInline, UMLOperation targetOperationBeforeInline) {
		this.inlinedOperation = inlinedOperation;
		this.targetOperationAfterInline = targetOperationAfterInline;
		this.targetOperationBeforeInline = targetOperationBeforeInline;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(inlinedOperation);
		sb.append(" inlined to ");
		sb.append(targetOperationAfterInline);
		sb.append(" in class ");
		sb.append(getClassName());
		return sb.toString();
	}

	private String getClassName() {
		return targetOperationAfterInline.getClassName();
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

	public UMLOperation getTargetOperationAfterInline() {
		return targetOperationAfterInline;
	}

	public UMLOperation getTargetOperationBeforeInline() {
		return targetOperationBeforeInline;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}
}
