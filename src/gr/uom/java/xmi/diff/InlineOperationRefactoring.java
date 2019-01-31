package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class InlineOperationRefactoring implements Refactoring {
	private UMLOperation inlinedOperation;
	private UMLOperation targetOperationAfterInline;
	private UMLOperation targetOperationBeforeInline;
	private OperationInvocation inlinedOperationInvocation;
	private Set<Replacement> replacements;
	private Set<AbstractCodeFragment> inlinedCodeFragmentsFromInlinedOperation;
	private Set<AbstractCodeFragment> inlinedCodeFragmentsInTargetOperation;
	
	public InlineOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation targetOperationBeforeInline,
			OperationInvocation operationInvocation) {
		this.inlinedOperation = bodyMapper.getOperation1();
		this.targetOperationAfterInline = bodyMapper.getOperation2();
		this.targetOperationBeforeInline = targetOperationBeforeInline;
		this.inlinedOperationInvocation = operationInvocation;
		this.replacements = bodyMapper.getReplacements();
		this.inlinedCodeFragmentsFromInlinedOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.inlinedCodeFragmentsInTargetOperation = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.inlinedCodeFragmentsFromInlinedOperation.add(mapping.getFragment1());
			this.inlinedCodeFragmentsInTargetOperation.add(mapping.getFragment2());
		}
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

	public OperationInvocation getInlinedOperationInvocation() {
		return inlinedOperationInvocation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public Set<AbstractCodeFragment> getInlinedCodeFragments() {
		return inlinedCodeFragmentsInTargetOperation;
	}

	/**
	 * @return the code range of the target method in the <b>parent</b> commit
	 */
	public CodeRange getTargetOperationCodeRangeBeforeInline() {
		return targetOperationBeforeInline.codeRange();
	}

	/**
	 * @return the code range of the target method in the <b>child</b> commit
	 */
	public CodeRange getTargetOperationCodeRangeAfterInline() {
		return targetOperationAfterInline.codeRange();
	}

	/**
	 * @return the code range of the inlined method in the <b>parent</b> commit
	 */
	public CodeRange getInlinedOperationCodeRange() {
		return inlinedOperation.codeRange();
	}

	/**
	 * @return the code range of the inlined code fragment from the inlined method in the <b>parent</b> commit
	 */
	public CodeRange getInlinedCodeRangeFromInlinedOperation() {
		return CodeRange.computeRange(inlinedCodeFragmentsFromInlinedOperation);
	}

	/**
	 * @return the code range of the inlined code fragment in the target method in the <b>child</b> commit
	 */
	public CodeRange getInlinedCodeRangeInTargetOperation() {
		return CodeRange.computeRange(inlinedCodeFragmentsInTargetOperation);
	}

	/**
	 * @return the code range of the invocation to the inlined method inside the target method in the <b>parent</b> commit
	 */
	public CodeRange getInlinedOperationInvocationCodeRange() {
		return inlinedOperationInvocation.codeRange();
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getTargetOperationBeforeInline().getClassName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getTargetOperationAfterInline().getClassName());
		return classNames;
	}
}
