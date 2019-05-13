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

public class ExtractAndMoveOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation sourceOperationBeforeExtraction;
	private UMLOperation sourceOperationAfterExtraction;
	private OperationInvocation extractedOperationInvocation;
	private Set<Replacement> replacements;
	private Set<AbstractCodeFragment> extractedCodeFragmentsFromSourceOperation;
	private Set<AbstractCodeFragment> extractedCodeFragmentsToExtractedOperation;
	private UMLOperationBodyMapper bodyMapper;
	
	public ExtractAndMoveOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation sourceOperationAfterExtraction, OperationInvocation operationInvocation) {
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
		return RefactoringType.EXTRACT_AND_MOVE_OPERATION;
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

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getSourceOperationBeforeExtraction().getClassName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(getSourceOperationAfterExtraction().getClassName());
		return classNames;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getSourceOperationCodeRangeBeforeExtraction()
				.setDescription("source method declaration before extraction")
				.setCodeElement(sourceOperationBeforeExtraction.toString()));
		//ranges.add(getExtractedCodeRangeFromSourceOperation().setDescription("extracted code from source method declaration"));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getExtractedOperationCodeRange()
				.setDescription("extracted method declaration")
				.setCodeElement(extractedOperation.toString()));
		//ranges.add(getExtractedCodeRangeToExtractedOperation().setDescription("extracted code to extracted method declaration"));
		ranges.add(getSourceOperationCodeRangeAfterExtraction()
				.setDescription("source method declaration after extraction")
				.setCodeElement(sourceOperationAfterExtraction.toString()));
		ranges.add(getExtractedOperationInvocationCodeRange()
				.setDescription("extracted method invocation")
				.setCodeElement(extractedOperationInvocation.actualString()));
		return ranges;
	}
}
