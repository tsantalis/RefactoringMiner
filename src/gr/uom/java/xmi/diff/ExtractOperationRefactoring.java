package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation sourceOperationBeforeExtraction;
	private UMLOperation sourceOperationAfterExtraction;
	private OperationInvocation extractedOperationInvocation;
	private Set<Replacement> replacements;
	private Set<AbstractCodeFragment> extractedCodeFragmentsFromSourceOperation;
	private Set<AbstractCodeFragment> extractedCodeFragmentsToExtractedOperation;
	private UMLOperationBodyMapper bodyMapper;

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation sourceOperationAfterExtraction, OperationInvocation operationInvocation) {
		this.bodyMapper = bodyMapper;
		this.extractedOperation = bodyMapper.getOperation2();
		this.sourceOperationBeforeExtraction = bodyMapper.getOperation1();
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.extractedOperationInvocation = operationInvocation;
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragmentsFromSourceOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.extractedCodeFragmentsToExtractedOperation = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.extractedCodeFragmentsToExtractedOperation.add(mapping.getFragment2());
		}
	}

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation extractedOperation,
			UMLOperation sourceOperationBeforeExtraction, UMLOperation sourceOperationAfterExtraction, OperationInvocation operationInvocation) {
		this.bodyMapper = bodyMapper;
		this.extractedOperation = extractedOperation;
		this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.extractedOperationInvocation = operationInvocation;
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragmentsFromSourceOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.extractedCodeFragmentsToExtractedOperation = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.extractedCodeFragmentsToExtractedOperation.add(mapping.getFragment2());
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(extractedOperation);
		sb.append(" extracted from ");
		sb.append(sourceOperationBeforeExtraction);
		sb.append(" in class ");
		sb.append(getClassName());
		return sb.toString();
	}

	@Override
	public String getRefactoredClass () {
		return getClassName();
	}

	private String getClassName() {
		String sourceClassName = getSourceOperationBeforeExtraction().getClassName();
		String targetClassName = getSourceOperationAfterExtraction().getClassName();
		return sourceClassName.equals(targetClassName) ? sourceClassName : targetClassName;
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
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

	public OperationInvocation getExtractedOperationInvocation() {
		return extractedOperationInvocation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public Set<AbstractCodeFragment> getExtractedCodeFragmentsFromSourceOperation() {
		return extractedCodeFragmentsFromSourceOperation;
	}

	public Set<AbstractCodeFragment> getExtractedCodeFragmentsToExtractedOperation() {
		return extractedCodeFragmentsToExtractedOperation;
	}

	/**
	 * @return the code range of the source method in the <b>parent</b> commit
	 */
	public CodeRange getSourceOperationCodeRangeBeforeExtraction() {
		return sourceOperationBeforeExtraction.codeRange();
	}

	/**
	 * @return the code range of the source method in the <b>child</b> commit
	 */
	public CodeRange getSourceOperationCodeRangeAfterExtraction() {
		return sourceOperationAfterExtraction.codeRange();
	}

	/**
	 * @return the code range of the extracted method in the <b>child</b> commit
	 */
	public CodeRange getExtractedOperationCodeRange() {
		return extractedOperation.codeRange();
	}

	/**
	 * @return the code range of the extracted code fragment from the source method in the <b>parent</b> commit
	 */
	public CodeRange getExtractedCodeRangeFromSourceOperation() {
		return CodeRange.computeRange(extractedCodeFragmentsFromSourceOperation);
	}

	/**
	 * @return the code range of the extracted code fragment to the extracted method in the <b>child</b> commit
	 */
	public CodeRange getExtractedCodeRangeToExtractedOperation() {
		return CodeRange.computeRange(extractedCodeFragmentsToExtractedOperation);
	}

	/**
	 * @return the code range of the invocation to the extracted method inside the source method in the <b>child</b> commit
	 */
	public CodeRange getExtractedOperationInvocationCodeRange() {
		return extractedOperationInvocation.codeRange();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_OPERATION;
	}
}
