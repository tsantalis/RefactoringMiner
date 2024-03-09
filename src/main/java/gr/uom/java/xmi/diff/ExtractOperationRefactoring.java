package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.ObjectCreation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class ExtractOperationRefactoring implements Refactoring {
	private UMLOperation extractedOperation;
	private VariableDeclarationContainer sourceOperationBeforeExtraction;
	private VariableDeclarationContainer sourceOperationAfterExtraction;
	private List<AbstractCall> extractedOperationInvocations;
	private Set<Replacement> replacements;
	private Set<AbstractCodeFragment> extractedCodeFragmentsFromSourceOperation;
	private Set<AbstractCodeFragment> extractedCodeFragmentsToExtractedOperation;
	private UMLOperationBodyMapper bodyMapper;
	private Map<String, String> parameterToArgumentMap;
	private List<AbstractCodeMapping> argumentMappings;

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, VariableDeclarationContainer sourceOperationAfterExtraction, List<AbstractCall> operationInvocations) {
		this.bodyMapper = bodyMapper;
		this.extractedOperation = bodyMapper.getOperation2();
		this.sourceOperationBeforeExtraction = bodyMapper.getContainer1();
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.extractedOperationInvocations = operationInvocations;
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragmentsFromSourceOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.extractedCodeFragmentsToExtractedOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.argumentMappings = new ArrayList<AbstractCodeMapping>();
		Optional<Map<String, String>> optionalMap = bodyMapper.getParameterToArgumentMap2();
		this.parameterToArgumentMap = optionalMap.isPresent() ? optionalMap.get() : Collections.emptyMap();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.extractedCodeFragmentsToExtractedOperation.add(mapping.getFragment2());
			createArgumentMappings(mapping);
			checkForMatchingCallChain(mapping);
		}
	}

	public ExtractOperationRefactoring(UMLOperationBodyMapper bodyMapper, UMLOperation extractedOperation,
			VariableDeclarationContainer sourceOperationBeforeExtraction, VariableDeclarationContainer sourceOperationAfterExtraction, List<AbstractCall> operationInvocations) {
		this.bodyMapper = bodyMapper;
		this.extractedOperation = extractedOperation;
		this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
		this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
		this.extractedOperationInvocations = operationInvocations;
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragmentsFromSourceOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.extractedCodeFragmentsToExtractedOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.argumentMappings = new ArrayList<AbstractCodeMapping>();
		Optional<Map<String, String>> optionalMap = bodyMapper.getParameterToArgumentMap2();
		this.parameterToArgumentMap = optionalMap.isPresent() ? optionalMap.get() : Collections.emptyMap();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.extractedCodeFragmentsToExtractedOperation.add(mapping.getFragment2());
			createArgumentMappings(mapping);
			checkForMatchingCallChain(mapping);
		}
	}

	public CompositeStatementObject extractedFromSynchronizedBlock() {
		if(bodyMapper.getParentMapper() != null && extractedOperation.isSynchronized()) {
			for(CompositeStatementObject unmatched : bodyMapper.getParentMapper().getNonMappedInnerNodesT1()) {
				if(unmatched.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT)) {
					int subsumed = 0;
					for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
						if(unmatched.getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo())) {
							subsumed++;
						}
					}
					if(subsumed == bodyMapper.getMappings().size()) {
						return unmatched;
					}
				}
			}
		}
		return null;
	}

	private void checkForMatchingCallChain(AbstractCodeMapping mapping) {
		AbstractCall invocation1 = mapping.getFragment1().invocationCoveringEntireFragment();
		if(invocation1 == null) {
			invocation1 = mapping.getFragment1().assignmentInvocationCoveringEntireStatement();
		}
		if(invocation1 instanceof OperationInvocation && ((OperationInvocation)invocation1).numberOfSubExpressions() > 0) {
			for(AbstractCodeMapping m : this.bodyMapper.getParentMapper().getMappings()) {
				if(m.getFragment2().getLocationInfo().subsumes(this.bodyMapper.getOperationInvocation().getLocationInfo())) {
					AbstractCall invocation2 = m.getFragment2().invocationCoveringEntireFragment();
					if(invocation2 == null) {
						invocation2 = m.getFragment2().assignmentInvocationCoveringEntireStatement();
					}
					if(invocation2 != null && invocation1.equals(invocation2)) {
						LeafMapping leafMapping = new LeafMapping(invocation1, invocation2, this.sourceOperationBeforeExtraction, this.sourceOperationAfterExtraction);
						this.bodyMapper.getParentMapper().getMappings().add(leafMapping);
						break;
					}
				}
			}
		}
	}

	public void updateMapperInfo() {
		this.replacements = bodyMapper.getReplacements();
		this.extractedCodeFragmentsFromSourceOperation.clear();
		this.extractedCodeFragmentsToExtractedOperation.clear();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.extractedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.extractedCodeFragmentsToExtractedOperation.add(mapping.getFragment2());
		}
	}

	private boolean isMappedInParent(AbstractCodeFragment leaf) {
		if(bodyMapper.parentMapperContainsMapping(leaf)) {
			return true;
		}
		else if(leaf.getParent() != null && bodyMapper.parentMapperContainsMapping(leaf.getParent())) {
			return true;
		}
		else if(leaf.getParent() != null && leaf.getParent().getParent() == null) {
			return true;
		}
		if(extractedOperationInvocations.size() == 1) {
			return true;
		}
		return false;
	}

	private boolean isDefaultValue(String argument) {
		return argument.equals("null") || argument.equals("0") || argument.equals("1") || argument.equals("false") || argument.equals("true");
	}

	private void createArgumentMappings(AbstractCodeMapping mapping) {
		boolean argumentMatchFound = false;
		for(AbstractCall call : extractedOperationInvocations) {
			for(String argument : call.arguments()) {
				if(!parameterToArgumentMap.containsKey(argument) && parameterToArgumentMap.containsValue(argument)) {
					Replacement replacementFound = null;
					for(Replacement replacement : mapping.getReplacements()) {
						if(replacement.getBefore().equals(argument) && parameterToArgumentMap.containsKey(replacement.getAfter()) &&
								parameterToArgumentMap.get(replacement.getAfter()).equals(argument)) {
							replacementFound = replacement;
							break;
						}
					}
					if(replacementFound != null) {
						argumentMatchFound = processArgument(mapping, call, argument);
					}
					else if(!isDefaultValue(argument)) {
						argumentMatchFound = processArgument(mapping, call, argument);
					}
				}
			}
		}
		if(!argumentMatchFound) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getBefore().equals(replacement.getAfter()) || replacement.getBefore().equals(JAVA.THIS_DOT + replacement.getAfter()) || replacement.getAfter().equals(JAVA.THIS_DOT + replacement.getBefore())) {
					List<LeafExpression> expressions1 = mapping.getFragment1().findExpression(replacement.getBefore());
					if(expressions1.size() > 0) {
						List<AbstractCodeFragment> leaves = sourceOperationAfterExtraction.getBody().getCompositeStatement().getLeaves();
						for(AbstractCodeFragment leaf : leaves) {
							for(AbstractCall call : extractedOperationInvocations) {
								if(leaf.getLocationInfo().subsumes(call.getLocationInfo()) && isMappedInParent(leaf)) {
									List<LeafExpression> expressions2 = leaf.findExpression(replacement.getAfter());
									if(expressions2.size() == 1) {
										for(LeafExpression expression1 : expressions1) {
											LeafMapping expressionMapping = new LeafMapping(expression1, expressions2.get(0), sourceOperationBeforeExtraction, sourceOperationAfterExtraction);
											argumentMappings.add(expressionMapping);
										}
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean processArgument(AbstractCodeMapping mapping, AbstractCall call, String argument) {
		for(AbstractCodeMapping m : bodyMapper.getMappings()) {
			VariableDeclaration variableDeclaration1 = m.getFragment1().getVariableDeclaration(argument);
			VariableDeclaration variableDeclaration2 = m.getFragment2().getVariableDeclaration(argument);
			if(variableDeclaration1 != null && variableDeclaration2 != null) {
				if(m.getFragment1().equals(mapping.getFragment1()) && m.getFragment2().equals(mapping.getFragment2())) {
					return false;
				}
				if(variableDeclaration1.getStatementsInScopeUsingVariable().contains(mapping.getFragment1()) &&
						variableDeclaration2.getStatementsInScopeUsingVariable().contains(mapping.getFragment2())) {
					return false;
				}
			}
		}
		AbstractCall invocation1 = mapping.getFragment1().invocationCoveringEntireFragment();
		if(invocation1 == null) {
			invocation1 = mapping.getFragment1().assignmentInvocationCoveringEntireStatement();
		}
		AbstractCall invocation2 = mapping.getFragment2().invocationCoveringEntireFragment();
		if(invocation2 == null) {
			invocation2 = mapping.getFragment2().assignmentInvocationCoveringEntireStatement();
		}
		ObjectCreation creation1 = mapping.getFragment1().creationCoveringEntireFragment();
		if(creation1 == null) {
			creation1 = mapping.getFragment1().assignmentCreationCoveringEntireStatement();
		}
		ObjectCreation creation2 = mapping.getFragment2().creationCoveringEntireFragment();
		if(creation2 == null) {
			creation2 = mapping.getFragment2().assignmentCreationCoveringEntireStatement();
		}
		List<LeafExpression> expressions1 = mapping.getFragment1().findExpression(argument);
		if(expressions1.isEmpty() && argument.contains(JAVA.LAMBDA_ARROW)) {
			String actualArgument = argument.substring(argument.indexOf(JAVA.LAMBDA_ARROW) + JAVA.LAMBDA_ARROW.length());
			expressions1 = mapping.getFragment1().findExpression(actualArgument);
		}
		if(expressions1.size() > 0) {
			List<AbstractCodeFragment> leaves = sourceOperationAfterExtraction.getBody().getCompositeStatement().getLeaves();
			for(AbstractCodeFragment leaf : leaves) {
				if(leaf.getLocationInfo().subsumes(call.getLocationInfo()) && isMappedInParent(leaf)) {
					List<LeafExpression> expressions2 = leaf.findExpression(argument);
					if(expressions2.size() == 1) {
						int occurrence = 0;
						for(LeafExpression expression1 : expressions1) {
							boolean equalArgument = false;
							if(invocation1 != null && invocation2 != null && invocation1.arguments().size() == invocation2.arguments().size()) {
								int index = indexInArguments(invocation1, expression1, occurrence);
								if(index != -1) {
									String argument2 = invocation2.arguments().get(index);
									if(argument2.equals(expression1.getString()) || argument2.equals(invocation1.arguments().get(index))) {
										equalArgument = true;
									}
								}
							}
							if(creation1 != null && creation2 != null && creation1.arguments().size() == creation2.arguments().size()) {
								int index = indexInArguments(creation1, expression1, occurrence);
								if(index != -1) {
									String argument2 = creation2.arguments().get(index);
									if(argument2.equals(expression1.getString()) || argument2.equals(creation1.arguments().get(index))) {
										equalArgument = true;
									}
								}
							}
							if(!equalArgument) {
								LeafMapping expressionMapping = new LeafMapping(expression1, expressions2.get(0), sourceOperationBeforeExtraction, sourceOperationAfterExtraction);
								argumentMappings.add(expressionMapping);
							}
							occurrence++;
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	private int indexInArguments(AbstractCall call, LeafExpression expression, int occurrence) {
		int index = 0;
		int matches = 0;
		for(String argument : call.arguments()) {
			if(argument.equals(expression.getString()) || argument.contains(expression.getString())) {
				if(matches == occurrence) {
					return index;
				}
				matches++;
			}
			index++;
		}
		return -1;
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

	public VariableDeclarationContainer getSourceOperationBeforeExtraction() {
		return sourceOperationBeforeExtraction;
	}

	public VariableDeclarationContainer getSourceOperationAfterExtraction() {
		return sourceOperationAfterExtraction;
	}

	public List<AbstractCall> getExtractedOperationInvocations() {
		return extractedOperationInvocations;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public List<AbstractCodeMapping> getArgumentMappings() {
		return argumentMappings;
	}

	public Map<String, String> getParameterToArgumentMap() {
		return parameterToArgumentMap;
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
		for(AbstractCall invocation : extractedOperationInvocations) {
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
		for(AbstractCall invocation : extractedOperationInvocations) {
			ranges.add(invocation.codeRange()
					.setDescription("extracted method invocation")
					.setCodeElement(invocation.actualString()));
		}
		for(AbstractCodeFragment statement : bodyMapper.getNonMappedLeavesT2()) {
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
