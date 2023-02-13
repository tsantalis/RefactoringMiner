package gr.uom.java.xmi.diff;

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

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class InlineOperationRefactoring implements Refactoring {
	private UMLOperation inlinedOperation;
	private VariableDeclarationContainer targetOperationAfterInline;
	private VariableDeclarationContainer targetOperationBeforeInline;
	private List<AbstractCall> inlinedOperationInvocations;
	private Set<Replacement> replacements;
	private Set<AbstractCodeFragment> inlinedCodeFragmentsFromInlinedOperation;
	private Set<AbstractCodeFragment> inlinedCodeFragmentsInTargetOperation;
	private UMLOperationBodyMapper bodyMapper;
	private Map<String, String> parameterToArgumentMap;
	private List<AbstractCodeMapping> argumentMappings;
	
	public InlineOperationRefactoring(UMLOperationBodyMapper bodyMapper, VariableDeclarationContainer targetOperationBeforeInline,
			List<AbstractCall> operationInvocations) {
		this.bodyMapper = bodyMapper;
		this.inlinedOperation = bodyMapper.getOperation1();
		this.targetOperationAfterInline = bodyMapper.getContainer2();
		this.targetOperationBeforeInline = targetOperationBeforeInline;
		this.inlinedOperationInvocations = operationInvocations;
		this.replacements = bodyMapper.getReplacements();
		this.inlinedCodeFragmentsFromInlinedOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.inlinedCodeFragmentsInTargetOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.argumentMappings = new ArrayList<AbstractCodeMapping>();
		Optional<Map<String, String>> optionalMap = bodyMapper.getParameterToArgumentMap1();
		this.parameterToArgumentMap = optionalMap.isPresent() ? optionalMap.get() : Collections.emptyMap();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.inlinedCodeFragmentsFromInlinedOperation.add(mapping.getFragment1());
			this.inlinedCodeFragmentsInTargetOperation.add(mapping.getFragment2());
			createArgumentMappings(mapping);
		}
	}

	public void updateMapperInfo() {
		this.replacements = bodyMapper.getReplacements();
		this.inlinedCodeFragmentsFromInlinedOperation.clear();
		this.inlinedCodeFragmentsInTargetOperation.clear();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.inlinedCodeFragmentsFromInlinedOperation.add(mapping.getFragment1());
			this.inlinedCodeFragmentsInTargetOperation.add(mapping.getFragment2());
		}
	}

	private void createArgumentMappings(AbstractCodeMapping mapping) {
		for(AbstractCall call : inlinedOperationInvocations) {
			for(String argument : call.arguments()) {
				if(!parameterToArgumentMap.containsKey(argument)) {
					List<LeafExpression> expressions2 = mapping.getFragment2().findExpression(argument);
					if(expressions2.size() > 0) {
						List<AbstractCodeFragment> leaves = targetOperationBeforeInline.getBody().getCompositeStatement().getLeaves();
						for(AbstractCodeFragment leaf : leaves) {
							if(leaf.getLocationInfo().subsumes(call.getLocationInfo())) {
								List<LeafExpression> expressions1 = leaf.findExpression(argument);
								if(expressions1.size() == 1 && expressions2.size() == 1) {
									LeafMapping expressionMapping = new LeafMapping(expressions1.get(0), expressions2.get(0), targetOperationBeforeInline, targetOperationAfterInline);
									argumentMappings.add(expressionMapping);
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(inlinedOperation);
		if(getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
			sb.append(" inlined to ");
			sb.append(targetOperationAfterInline);
			sb.append(" in class ");
			sb.append(getClassName());
		}
		else if(getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION)) {
			sb.append(" moved from class ");
			sb.append(inlinedOperation.getClassName());
			sb.append(" to class ");
			sb.append(getTargetOperationAfterInline().getClassName());
			sb.append(" & inlined to ");
			sb.append(getTargetOperationAfterInline());
		}
		return sb.toString();
	}

	private String getClassName() {
		return targetOperationAfterInline.getClassName();
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		if (!getTargetOperationBeforeInline().getClassName().equals(getInlinedOperation().getClassName()))
			return RefactoringType.MOVE_AND_INLINE_OPERATION;
		return RefactoringType.INLINE_OPERATION;
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
	}

	public UMLOperation getInlinedOperation() {
		return inlinedOperation;
	}

	public VariableDeclarationContainer getTargetOperationAfterInline() {
		return targetOperationAfterInline;
	}

	public VariableDeclarationContainer getTargetOperationBeforeInline() {
		return targetOperationBeforeInline;
	}

	public List<AbstractCall> getInlinedOperationInvocations() {
		return inlinedOperationInvocations;
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
	 * @return the code range(s) of the invocation(s) to the inlined method inside the target method in the <b>parent</b> commit
	 */
	public Set<CodeRange> getInlinedOperationInvocationCodeRanges() {
		Set<CodeRange> codeRanges = new LinkedHashSet<CodeRange>();
		for(AbstractCall invocation : inlinedOperationInvocations) {
			codeRanges.add(invocation.codeRange());
		}
		return codeRanges;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getInlinedOperation().getLocationInfo().getFilePath(), getInlinedOperation().getClassName()));
		pairs.add(new ImmutablePair<String, String>(getTargetOperationBeforeInline().getLocationInfo().getFilePath(), getTargetOperationBeforeInline().getClassName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getTargetOperationAfterInline().getLocationInfo().getFilePath(), getTargetOperationAfterInline().getClassName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getInlinedOperationCodeRange()
				.setDescription("inlined method declaration")
				.setCodeElement(inlinedOperation.toString()));
		//ranges.add(getInlinedCodeRangeFromInlinedOperation().setDescription("inlined code from inlined method declaration"));
		for(AbstractCodeFragment inlinedCodeFragment : inlinedCodeFragmentsFromInlinedOperation) {
			ranges.add(inlinedCodeFragment.codeRange().setDescription("inlined code from inlined method declaration"));
		}
		ranges.add(getTargetOperationCodeRangeBeforeInline()
				.setDescription("target method declaration before inline")
				.setCodeElement(targetOperationBeforeInline.toString()));
		for(AbstractCall invocation : inlinedOperationInvocations) {
			ranges.add(invocation.codeRange()
					.setDescription("inlined method invocation")
					.setCodeElement(invocation.actualString()));
		}
		for(AbstractCodeFragment statement : bodyMapper.getNonMappedLeavesT1()) {
			ranges.add(statement.codeRange().
					setDescription("deleted statement in inlined method declaration"));
		}
		for(CompositeStatementObject statement : bodyMapper.getNonMappedInnerNodesT1()) {
			ranges.add(statement.codeRange().
					setDescription("deleted statement in inlined method declaration"));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getTargetOperationCodeRangeAfterInline()
				.setDescription("target method declaration after inline")
				.setCodeElement(targetOperationAfterInline.toString()));
		for(AbstractCodeFragment inlinedCodeFragment : inlinedCodeFragmentsInTargetOperation) {
			ranges.add(inlinedCodeFragment.codeRange().setDescription("inlined code in target method declaration"));
		}
		/*
		CodeRange inlinedCodeRangeInTargetOperation = getInlinedCodeRangeInTargetOperation();
		ranges.add(inlinedCodeRangeInTargetOperation.setDescription("inlined code in target method declaration"));
		for(StatementObject statement : bodyMapper.getNonMappedLeavesT2()) {
			if(inlinedCodeRangeInTargetOperation.subsumes(statement.codeRange())) {
				ranges.add(statement.codeRange().
						setDescription("added statement in target method declaration"));
			}
		}
		for(CompositeStatementObject statement : bodyMapper.getNonMappedInnerNodesT2()) {
			if(inlinedCodeRangeInTargetOperation.subsumes(statement.codeRange()) ||
					inlinedCodeRangeInTargetOperation.subsumes(statement.getLeaves())) {
				ranges.add(statement.codeRange().
						setDescription("added statement in target method declaration"));
			}
		}
		*/
		return ranges;
	}
}
