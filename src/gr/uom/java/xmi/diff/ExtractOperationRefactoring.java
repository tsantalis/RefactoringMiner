package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private UMLOperation sourceOperationBeforeExtraction;
	private UMLOperation sourceOperationAfterExtraction;
	private List<OperationInvocation> extractedOperationInvocations;
	private Set<Replacement> replacements;
	private Set<AbstractCodeFragment> extractedCodeFragmentsFromSourceOperation;
	private Set<AbstractCodeFragment> extractedCodeFragmentsToExtractedOperation;
	private UMLOperationBodyMapper bodyMapper;

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation sourceOperationAfterExtraction, List<OperationInvocation> operationInvocations) {
		this.bodyMapper = bodyMapper;
		this.extractedOperation = bodyMapper.getOperation2();
		this.sourceOperationBeforeExtraction = bodyMapper.getOperation1();
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.extractedOperationInvocations = operationInvocations;
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragmentsFromSourceOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.extractedCodeFragmentsToExtractedOperation = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.extractedCodeFragmentsToExtractedOperation.add(mapping.getFragment2());
		}
	}

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation extractedOperation,
			UMLOperation sourceOperationBeforeExtraction, UMLOperation sourceOperationAfterExtraction, List<OperationInvocation> operationInvocations) {
		this.bodyMapper = bodyMapper;
		this.extractedOperation = extractedOperation;
		this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.extractedOperationInvocations = operationInvocations;
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
		if(getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION)) {
			sb.append(" & moved to class ");
			sb.append(extractedOperation.getClassName());
		}
		return sb.toString();
	}

	private String getClassName() {
		if(getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION)) {
			return getSourceOperationBeforeExtraction().getClassName();
		}
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

	public List<OperationInvocation> getExtractedOperationInvocations() {
		return extractedOperationInvocations;
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
	 * @return the code range(s) of the invocation(s) to the extracted method inside the source method in the <b>child</b> commit
	 */
	public Set<CodeRange> getExtractedOperationInvocationCodeRanges() {
		Set<CodeRange> codeRanges = new LinkedHashSet<CodeRange>();
		for(OperationInvocation invocation : extractedOperationInvocations) {
			codeRanges.add(invocation.codeRange());
		}
		return codeRanges;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		if(!getSourceOperationAfterExtraction().getClassName().equals(getExtractedOperation().getClassName()))
			return RefactoringType.EXTRACT_AND_MOVE_OPERATION;
		return RefactoringType.EXTRACT_OPERATION;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getSourceOperationBeforeExtraction().getLocationInfo().getFilePath(), getSourceOperationBeforeExtraction().getClassName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getSourceOperationAfterExtraction().getLocationInfo().getFilePath(), getSourceOperationAfterExtraction().getClassName()));
		pairs.add(new ImmutablePair<String, String>(getExtractedOperation().getLocationInfo().getFilePath(), getExtractedOperation().getClassName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getSourceOperationCodeRangeBeforeExtraction()
				.setDescription("source method declaration before extraction")
				.setCodeElement(sourceOperationBeforeExtraction.toString()));
		for(AbstractCodeFragment extractedCodeFragment : extractedCodeFragmentsFromSourceOperation) {
			ranges.add(extractedCodeFragment.codeRange().setDescription("extracted code from source method declaration"));
		}
		/*
		CodeRange extractedCodeRangeFromSourceOperation = getExtractedCodeRangeFromSourceOperation();
		ranges.add(extractedCodeRangeFromSourceOperation.setDescription("extracted code from source method declaration"));
		for(StatementObject statement : bodyMapper.getNonMappedLeavesT1()) {
			if(extractedCodeRangeFromSourceOperation.subsumes(statement.codeRange())) {
				ranges.add(statement.codeRange().
						setDescription("deleted statement in source method declaration"));
			}
		}
		for(CompositeStatementObject statement : bodyMapper.getNonMappedInnerNodesT1()) {
			if(extractedCodeRangeFromSourceOperation.subsumes(statement.codeRange()) ||
					extractedCodeRangeFromSourceOperation.subsumes(statement.getLeaves())) {
				ranges.add(statement.codeRange().
						setDescription("deleted statement in source method declaration"));
			}
		}
		*/
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getExtractedOperationCodeRange()
				.setDescription("extracted method declaration")
				.setCodeElement(extractedOperation.toString()));
		//ranges.add(getExtractedCodeRangeToExtractedOperation().setDescription("extracted code to extracted method declaration"));
		for(AbstractCodeFragment extractedCodeFragment : extractedCodeFragmentsToExtractedOperation) {
			ranges.add(extractedCodeFragment.codeRange().setDescription("extracted code to extracted method declaration"));
		}
		ranges.add(getSourceOperationCodeRangeAfterExtraction()
				.setDescription("source method declaration after extraction")
				.setCodeElement(sourceOperationAfterExtraction.toString()));
		for(OperationInvocation invocation : extractedOperationInvocations) {
			ranges.add(invocation.codeRange()
					.setDescription("extracted method invocation")
					.setCodeElement(invocation.actualString()));
		}
		for(StatementObject statement : bodyMapper.getNonMappedLeavesT2()) {
			ranges.add(statement.codeRange().
					setDescription("added statement in extracted method declaration"));
		}
		for(CompositeStatementObject statement : bodyMapper.getNonMappedInnerNodesT2()) {
			ranges.add(statement.codeRange().
					setDescription("added statement in extracted method declaration"));
		}
		return ranges;
	}
}
