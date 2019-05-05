package gr.uom.java.xmi.decomposition;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableDeclarationReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.SplitVariableReplacement;
import gr.uom.java.xmi.diff.CandidateAttributeRefactoring;
import gr.uom.java.xmi.diff.CandidateMergeVariableRefactoring;
import gr.uom.java.xmi.diff.CandidateSplitVariableRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLParameterDiff;

public class VariableReplacementAnalysis {
	private List<AbstractCodeMapping> mappings;
	private List<StatementObject> nonMappedLeavesT1;
	private List<StatementObject> nonMappedLeavesT2;
	private UMLOperation operation1;
	private UMLOperation operation2;
	private List<UMLOperationBodyMapper> additionalMappers;
	private Set<Refactoring> refactorings;
	private UMLOperation callSiteOperation;
	private UMLOperationDiff operationDiff;
	private Set<RenameVariableRefactoring> variableRenames = new LinkedHashSet<RenameVariableRefactoring>();
	private Set<MergeVariableRefactoring> variableMerges = new LinkedHashSet<MergeVariableRefactoring>();
	private Set<SplitVariableRefactoring> variableSplits = new LinkedHashSet<SplitVariableRefactoring>();
	private Set<CandidateAttributeRefactoring> candidateAttributeRenames = new LinkedHashSet<CandidateAttributeRefactoring>();
	private Set<CandidateMergeVariableRefactoring> candidateAttributeMerges = new LinkedHashSet<CandidateMergeVariableRefactoring>();
	private Set<CandidateSplitVariableRefactoring> candidateAttributeSplits = new LinkedHashSet<CandidateSplitVariableRefactoring>();

	public VariableReplacementAnalysis(UMLOperationBodyMapper mapper, Set<Refactoring> refactorings, UMLOperationDiff operationDiff) {
		this.mappings = mapper.getMappings();
		this.nonMappedLeavesT1 = mapper.getNonMappedLeavesT1();
		this.nonMappedLeavesT2 = mapper.getNonMappedLeavesT2();
		this.operation1 = mapper.getOperation1();
		this.operation2 = mapper.getOperation2();
		this.additionalMappers = mapper.getAdditionalMappers();
		this.refactorings = refactorings;
		this.callSiteOperation = mapper.getCallSiteOperation();
		this.operationDiff = operationDiff;
		findVariableSplits();
		findVariableMerges();
		findConsistentVariableRenames();
		findParametersWrappedInLocalVariables();
	}

	private void findParametersWrappedInLocalVariables() {
		for(StatementObject statement : nonMappedLeavesT2) {
			for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
				AbstractExpression initializer = declaration.getInitializer();
				if(initializer != null) {
					for(String key : initializer.getCreationMap().keySet()) {
						ObjectCreation creation = initializer.getCreationMap().get(key);
						for(String argument : creation.arguments) {
							SimpleEntry<VariableDeclaration, UMLOperation> v2 = getVariableDeclaration2(new Replacement("", argument, ReplacementType.VARIABLE_NAME));
							SimpleEntry<VariableDeclaration, UMLOperation> v1 = getVariableDeclaration1(new Replacement(declaration.getVariableName(), "", ReplacementType.VARIABLE_NAME));
							if(v2 != null && v1 != null) {
								RenameVariableRefactoring ref = new RenameVariableRefactoring(v1.getKey(), v2.getKey(), v1.getValue(), v2.getValue(), new ArrayList<AbstractCodeMapping>());
								if(!existsConflictingExtractVariableRefactoring(ref) && !existsConflictingMergeVariableRefactoring(ref) && !existsConflictingSplitVariableRefactoring(ref)) {
									variableRenames.add(ref);
								}
							}
						}
					}
				}
			}
		}
	}

	public Set<RenameVariableRefactoring> getVariableRenames() {
		return variableRenames;
	}

	public Set<MergeVariableRefactoring> getVariableMerges() {
		return variableMerges;
	}

	public Set<SplitVariableRefactoring> getVariableSplits() {
		return variableSplits;
	}

	public Set<CandidateAttributeRefactoring> getCandidateAttributeRenames() {
		return candidateAttributeRenames;
	}

	public Set<CandidateMergeVariableRefactoring> getCandidateAttributeMerges() {
		return candidateAttributeMerges;
	}

	public Set<CandidateSplitVariableRefactoring> getCandidateAttributeSplits() {
		return candidateAttributeSplits;
	}

	private void findVariableSplits() {
		Map<SplitVariableReplacement, List<AbstractCodeMapping>> splitMap = new LinkedHashMap<SplitVariableReplacement, List<AbstractCodeMapping>>();
		Map<String, Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>>> variableInvocationExpressionMap = new LinkedHashMap<String, Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement instanceof SplitVariableReplacement) {
					SplitVariableReplacement split = (SplitVariableReplacement)replacement;
					if(splitMap.containsKey(split)) {
						splitMap.get(split).add(mapping);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
						mappings.add(mapping);
						splitMap.put(split, mappings);
					}
				}
				else if(replacement instanceof VariableReplacementWithMethodInvocation) {
					VariableReplacementWithMethodInvocation variableReplacement = (VariableReplacementWithMethodInvocation)replacement;
					processVariableReplacementWithMethodInvocation(variableReplacement, mapping, variableInvocationExpressionMap);
				}
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
					for(StatementObject statement : nonMappedLeavesT1) {
						VariableDeclaration variableDeclaration = statement.getVariableDeclaration(replacement.getBefore());
						if(variableDeclaration != null) {
							AbstractExpression initializer = variableDeclaration.getInitializer();
							if(initializer != null) {
								OperationInvocation invocation = initializer.invocationCoveringEntireFragment();
								if(invocation != null) {
									VariableReplacementWithMethodInvocation variableReplacement = new VariableReplacementWithMethodInvocation(initializer.getString(), replacement.getAfter(), invocation, Direction.INVOCATION_TO_VARIABLE);
									processVariableReplacementWithMethodInvocation(variableReplacement, mapping, variableInvocationExpressionMap);
								}
							}
						}
					}
				}
			}
		}
		for(String key : variableInvocationExpressionMap.keySet()) {
			Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(key);
			List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
			Set<String> splitVariables = new LinkedHashSet<String>();
			for(VariableReplacementWithMethodInvocation replacement : map.keySet()) {
				if(!PrefixSuffixUtils.normalize(key).equals(PrefixSuffixUtils.normalize(replacement.getAfter()))) {
					splitVariables.add(replacement.getAfter());
					mappings.addAll(map.get(replacement));
				}
			}
			if(splitVariables.size() > 0) {
				SplitVariableReplacement split = new SplitVariableReplacement(key, splitVariables);
				splitMap.put(split, mappings);
			}
		}
		for(SplitVariableReplacement split : splitMap.keySet()) {
			Set<VariableDeclaration> splitVariables = new LinkedHashSet<VariableDeclaration>();
			Set<UMLOperation> splitVariableOperations = new LinkedHashSet<UMLOperation>();
			for(String variableName : split.getSplitVariables()) {
				SimpleEntry<VariableDeclaration,UMLOperation> declaration = getVariableDeclaration2(split, variableName);
				if(declaration != null) {
					splitVariables.add(declaration.getKey());
					splitVariableOperations.add(declaration.getValue());
				}
			}
			SimpleEntry<VariableDeclaration,UMLOperation> oldVariable = getVariableDeclaration1(split);
			if(splitVariables.size() > 1 && splitVariables.size() == split.getSplitVariables().size() && oldVariable != null) {
				UMLOperation operationAfer = splitVariableOperations.iterator().next();
				SplitVariableRefactoring refactoring = new SplitVariableRefactoring(oldVariable.getKey(), splitVariables, oldVariable.getValue(), operationAfer, splitMap.get(split));
				//if(!existsConflictingInlineVariableRefactoring(refactoring)) {
					variableSplits.add(refactoring);
				//}
			}
			else {
				CandidateSplitVariableRefactoring candidate = new CandidateSplitVariableRefactoring(split.getBefore(), split.getSplitVariables(), operation1, operation2, splitMap.get(split));
				candidateAttributeSplits.add(candidate);
			}
		}
	}

	private void processVariableReplacementWithMethodInvocation(
			VariableReplacementWithMethodInvocation variableReplacement, AbstractCodeMapping mapping,
			Map<String, Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>>> variableInvocationExpressionMap) {
		String expression = variableReplacement.getInvokedOperation().getExpression();
		if(expression != null && variableReplacement.getDirection().equals(Direction.INVOCATION_TO_VARIABLE)) {
			if(variableInvocationExpressionMap.containsKey(expression)) {
				Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(expression);
				if(map.containsKey(variableReplacement)) {
					map.get(variableReplacement).add(mapping);
				}
				else {
					List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
					mappings.add(mapping);
					map.put(variableReplacement, mappings);
				}
			}
			else {
				List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
				mappings.add(mapping);
				Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>> map = new LinkedHashMap<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>>();
				map.put(variableReplacement, mappings);
				variableInvocationExpressionMap.put(expression, map);
			}
		}
	}

	private void findVariableMerges() {
		Map<MergeVariableReplacement, List<AbstractCodeMapping>> mergeMap = new LinkedHashMap<MergeVariableReplacement, List<AbstractCodeMapping>>();
		Map<String, Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>>> variableInvocationExpressionMap = new LinkedHashMap<String, Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement instanceof MergeVariableReplacement) {
					MergeVariableReplacement merge = (MergeVariableReplacement)replacement;
					if(mergeMap.containsKey(merge)) {
						mergeMap.get(merge).add(mapping);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
						mappings.add(mapping);
						mergeMap.put(merge, mappings);
					}
				}
				else if(replacement instanceof VariableReplacementWithMethodInvocation) {
					VariableReplacementWithMethodInvocation variableReplacement = (VariableReplacementWithMethodInvocation)replacement;
					String expression = variableReplacement.getInvokedOperation().getExpression();
					if(expression != null && variableReplacement.getDirection().equals(Direction.VARIABLE_TO_INVOCATION)) {
						if(variableInvocationExpressionMap.containsKey(expression)) {
							Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(expression);
							if(map.containsKey(variableReplacement)) {
								map.get(variableReplacement).add(mapping);
							}
							else {
								List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
								mappings.add(mapping);
								map.put(variableReplacement, mappings);
							}
						}
						else {
							List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
							mappings.add(mapping);
							Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>> map = new LinkedHashMap<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>>();
							map.put(variableReplacement, mappings);
							variableInvocationExpressionMap.put(expression, map);
						}
					}
				}
			}
		}
		for(String key : variableInvocationExpressionMap.keySet()) {
			Map<VariableReplacementWithMethodInvocation, List<AbstractCodeMapping>> map = variableInvocationExpressionMap.get(key);
			List<AbstractCodeMapping> mappings = new ArrayList<AbstractCodeMapping>();
			Set<String> mergedVariables = new LinkedHashSet<String>();
			for(VariableReplacementWithMethodInvocation replacement : map.keySet()) {
				if(!PrefixSuffixUtils.normalize(key).equals(PrefixSuffixUtils.normalize(replacement.getBefore()))) {
					mergedVariables.add(replacement.getBefore());
					mappings.addAll(map.get(replacement));
				}
			}
			if(mergedVariables.size() > 0) {
				MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
				mergeMap.put(merge, mappings);
			}
		}
		for(MergeVariableReplacement merge : mergeMap.keySet()) {
			Set<VariableDeclaration> mergedVariables = new LinkedHashSet<VariableDeclaration>();
			Set<UMLOperation> mergedVariableOperations = new LinkedHashSet<UMLOperation>();
			for(String variableName : merge.getMergedVariables()) {
				SimpleEntry<VariableDeclaration,UMLOperation> declaration = getVariableDeclaration1(merge, variableName);
				if(declaration != null) {
					mergedVariables.add(declaration.getKey());
					mergedVariableOperations.add(declaration.getValue());
				}
			}
			SimpleEntry<VariableDeclaration,UMLOperation> newVariable = getVariableDeclaration2(merge);
			if(mergedVariables.size() > 1 && mergedVariables.size() == merge.getMergedVariables().size() && newVariable != null) {
				UMLOperation operationBefore = mergedVariableOperations.iterator().next();
				MergeVariableRefactoring refactoring = new MergeVariableRefactoring(mergedVariables, newVariable.getKey(), operationBefore, newVariable.getValue(), mergeMap.get(merge));
				if(!existsConflictingInlineVariableRefactoring(refactoring)) {
					variableMerges.add(refactoring);
				}
			}
			else {
				CandidateMergeVariableRefactoring candidate = new CandidateMergeVariableRefactoring(merge.getMergedVariables(), merge.getAfter(), operation1, operation2, mergeMap.get(merge));
				candidateAttributeMerges.add(candidate);
			}
		}
	}

	private void findConsistentVariableRenames() {
		Map<Replacement, List<AbstractCodeMapping>> variableDeclarationReplacementOccurrenceMap = getVariableDeclarationReplacementOccurrenceMap();
		Set<Replacement> allConsistentVariableDeclarationRenames = allConsistentRenames(variableDeclarationReplacementOccurrenceMap);
		Map<Replacement, List<AbstractCodeMapping>> replacementOccurrenceMap = getReplacementOccurrenceMap(ReplacementType.VARIABLE_NAME);
		Set<Replacement> allConsistentRenames = allConsistentRenames(replacementOccurrenceMap);
		Map<Replacement, List<AbstractCodeMapping>> finalConsistentRenames = new LinkedHashMap<Replacement, List<AbstractCodeMapping>>();
		for(Replacement replacement : allConsistentRenames) {
			SimpleEntry<VariableDeclaration, UMLOperation> v1 = getVariableDeclaration1(replacement);
			SimpleEntry<VariableDeclaration, UMLOperation> v2 = getVariableDeclaration2(replacement);
			List<AbstractCodeMapping> list = replacementOccurrenceMap.get(replacement);
			if((list.size() > 1 && v1 != null && v2 != null && consistencyCheck(v1.getKey(), v2.getKey())) ||
					potentialParameterRename(replacement) ||
					v1 == null || v2 == null ||
					(list.size() == 1 && replacementInLocalVariableDeclaration(replacement))) {
				finalConsistentRenames.put(replacement, list);
			}
			if(v1 != null && !v1.getKey().isParameter() && v2 != null && v2.getKey().isParameter() && consistencyCheck(v1.getKey(), v2.getKey()) &&
					!operation1.getParameterNameList().contains(v2.getKey().getVariableName())) {
				finalConsistentRenames.put(replacement, list);
			}
		}
		for(Replacement replacement : allConsistentVariableDeclarationRenames) {
			VariableDeclarationReplacement vdReplacement = (VariableDeclarationReplacement)replacement;
			Replacement variableNameReplacement = vdReplacement.getVariableNameReplacement();
			List<AbstractCodeMapping> list = variableDeclarationReplacementOccurrenceMap.get(vdReplacement);
			if((list.size() > 1 && consistencyCheck(vdReplacement.getVariableDeclaration1(), vdReplacement.getVariableDeclaration2())) ||
					(list.size() == 1 && replacementInLocalVariableDeclaration(variableNameReplacement))) {
				finalConsistentRenames.put(variableNameReplacement, list);
			}
		}
		for(Replacement replacement : finalConsistentRenames.keySet()) {
			SimpleEntry<VariableDeclaration, UMLOperation> v1 = getVariableDeclaration1(replacement);
			SimpleEntry<VariableDeclaration, UMLOperation> v2 = getVariableDeclaration2(replacement);
			if(v1 != null && v2 != null) {
				RenameVariableRefactoring ref = new RenameVariableRefactoring(v1.getKey(), v2.getKey(), v1.getValue(), v2.getValue(), finalConsistentRenames.get(replacement));
				if(!existsConflictingExtractVariableRefactoring(ref) && !existsConflictingMergeVariableRefactoring(ref) && !existsConflictingSplitVariableRefactoring(ref)) {
					variableRenames.add(ref);
				}
			}
			else if(!PrefixSuffixUtils.normalize(replacement.getBefore()).equals(PrefixSuffixUtils.normalize(replacement.getAfter())) &&
					(!operation1.getAllVariables().contains(replacement.getAfter()) || cyclicRename(finalConsistentRenames.keySet(), replacement)) &&
					(!operation2.getAllVariables().contains(replacement.getBefore()) || cyclicRename(finalConsistentRenames.keySet(), replacement))) {
				CandidateAttributeRefactoring candidate = new CandidateAttributeRefactoring(
						replacement.getBefore(), replacement.getAfter(), operation1, operation2,
						replacementOccurrenceMap.get(replacement));
				if(v1 != null)
					candidate.setOriginalVariableDeclaration(v1.getKey());
				if(v2 != null)
					candidate.setRenamedVariableDeclaration(v2.getKey());
				this.candidateAttributeRenames.add(candidate);
			}
		}
	}

	private Map<Replacement, List<AbstractCodeMapping>> getReplacementOccurrenceMap(ReplacementType type) {
		Map<Replacement, List<AbstractCodeMapping>> map = new LinkedHashMap<Replacement, List<AbstractCodeMapping>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(type) && !returnVariableMapping(mapping, replacement) &&
						!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
						replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
					if(map.containsKey(replacement)) {
						map.get(replacement).add(mapping);
					}
					else {
						List<AbstractCodeMapping> list = new ArrayList<AbstractCodeMapping>();
						list.add(mapping);
						map.put(replacement, list);
					}
				}
			}
		}
		return map;
	}

	private Map<Replacement, List<AbstractCodeMapping>> getVariableDeclarationReplacementOccurrenceMap() {
		Map<Replacement, List<AbstractCodeMapping>> map = new LinkedHashMap<Replacement, List<AbstractCodeMapping>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) && !returnVariableMapping(mapping, replacement) &&
						!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
						replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
					SimpleEntry<VariableDeclaration, UMLOperation> v1 = getVariableDeclaration1(replacement);
					SimpleEntry<VariableDeclaration, UMLOperation> v2 = getVariableDeclaration2(replacement);
					if(v1 != null && v2 != null) {
						VariableDeclarationReplacement r = new VariableDeclarationReplacement(v1.getKey(), v2.getKey());
						if(map.containsKey(r)) {
							map.get(r).add(mapping);
						}
						else {
							List<AbstractCodeMapping> list = new ArrayList<AbstractCodeMapping>();
							list.add(mapping);
							map.put(r, list);
						}
					}
				}
			}
		}
		if(operationDiff != null) {
			List<UMLParameterDiff> allParameterDiffs = new ArrayList<UMLParameterDiff>();
			for(UMLParameterDiff parameterDiff : operationDiff.getParameterDiffList()) {
				if(parameterDiff.isNameChanged()) {
					allParameterDiffs.add(parameterDiff);
				}
			}
			List<UMLParameterDiff> matchedParameterDiffs = new ArrayList<UMLParameterDiff>();
			for(UMLParameterDiff parameterDiff : allParameterDiffs) {
				for(Replacement replacement : map.keySet()) {
					VariableDeclarationReplacement vdR = (VariableDeclarationReplacement)replacement;
					if(parameterDiff.getRemovedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration1()) &&
							parameterDiff.getAddedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration2())) {
						matchedParameterDiffs.add(parameterDiff);
						break;
					}
				}
			}
			Set<VariableDeclarationReplacement> keysToBeRemoved = new LinkedHashSet<VariableDeclarationReplacement>();
			for(UMLParameterDiff parameterDiff : matchedParameterDiffs) {
				for(Replacement replacement : map.keySet()) {
					VariableDeclarationReplacement vdR = (VariableDeclarationReplacement)replacement;
					if(parameterDiff.getRemovedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration1()) &&
							!parameterDiff.getAddedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration2())) {
						keysToBeRemoved.add(vdR);
					}
					else if(!parameterDiff.getRemovedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration1()) &&
							parameterDiff.getAddedParameter().getVariableDeclaration().equals(vdR.getVariableDeclaration2())) {
						keysToBeRemoved.add(vdR);
					}
				}
			}
			for(VariableDeclarationReplacement key : keysToBeRemoved) {
				map.remove(key);
			}
		}
		return map;
	}

	private static boolean returnVariableMapping(AbstractCodeMapping mapping, Replacement replacement) {
		return mapping.getFragment1().getString().equals("return " + replacement.getBefore() + ";\n") &&
				mapping.getFragment2().getString().equals("return " + replacement.getAfter() + ";\n");
	}

	private boolean containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(Set<Replacement> replacements) {
		for(Replacement replacement : replacements) {
			if(replacement instanceof MethodInvocationReplacement) {
				MethodInvocationReplacement r = (MethodInvocationReplacement)replacement;
				if(r.differentExpressionNameAndArguments())
					return true;
			}
		}
		return false;
	}

	private boolean replacementNotInsideMethodSignatureOfAnonymousClass(AbstractCodeMapping mapping, Replacement replacement) {
		AbstractCodeFragment fragment1 = mapping.getFragment1();
		AbstractCodeFragment fragment2 = mapping.getFragment2();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = fragment1.getAnonymousClassDeclarations();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = fragment2.getAnonymousClassDeclarations();
		if(anonymousClassDeclarations1.size() > 0 && anonymousClassDeclarations2.size() > 0) {
			boolean replacementBeforeNotFoundInMethodSignature = false;
			String[] lines1 = fragment1.getString().split("\\n");
			for(String line : lines1) {
				line = prepareLine(line);
				if(!Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches() &&
						ReplacementUtil.contains(line, replacement.getBefore())) {
					replacementBeforeNotFoundInMethodSignature = true;
					break;
				}
			}
			boolean replacementAfterNotFoundInMethodSignature = false;
			String[] lines2 = fragment2.getString().split("\\n");
			for(String line : lines2) {
				line = prepareLine(line);
				if(!Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches() &&
						ReplacementUtil.contains(line, replacement.getAfter())) {
					replacementAfterNotFoundInMethodSignature = true;
					break;
				}
			}
			return replacementBeforeNotFoundInMethodSignature && replacementAfterNotFoundInMethodSignature;
		}
		return true;
	}

	public static String prepareLine(String line) {
		line = line.trim();
		if(line.startsWith("@Nullable")) {
			line = line.substring(9, line.length());
			line = line.trim();
		}
		if(line.startsWith("@Override")) {
			line = line.substring(9, line.length());
			line = line.trim();
		}
		if(line.contains("throws ")) {
			line = line.substring(0, line.indexOf("throws "));
		}
		return line;
	}

	private static boolean cyclicRename(Set<Replacement> finalConsistentRenames, Replacement replacement) {
		for(Replacement r : finalConsistentRenames) {
			if(replacement.getAfter().equals(r.getBefore()))
				return true;
			if(replacement.getBefore().equals(r.getAfter()))
				return true;
		}
		return false;
	}

	private Set<Replacement> allConsistentRenames(Map<Replacement, List<AbstractCodeMapping>> replacementOccurrenceMap) {
		Set<Replacement> renames = replacementOccurrenceMap.keySet();
		Set<Replacement> allConsistentRenames = new LinkedHashSet<Replacement>();
		Set<Replacement> allInconsistentRenames = new LinkedHashSet<Replacement>();
		ConsistentReplacementDetector.updateRenames(allConsistentRenames, allInconsistentRenames, renames);
		allConsistentRenames.removeAll(allInconsistentRenames);
		return allConsistentRenames;
	}

	private boolean replacementInLocalVariableDeclaration(Replacement replacement) {
		VariableDeclaration v1 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v1 = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
				break;
			}
		}
		VariableDeclaration v2 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v2 = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				break;
			}
		}
		return v1 != null && v2 != null &&
				v1.equalVariableDeclarationType(v2) &&
				!containsVariableDeclarationWithName(operation1.getAllVariableDeclarations(), replacement.getAfter()) &&
				!containsVariableDeclarationWithName(operation2.getAllVariableDeclarations(), replacement.getBefore()) &&
				consistencyCheck(v1, v2);
	}

	private boolean consistencyCheck(VariableDeclaration v1, VariableDeclaration v2) {
		return !variableAppearsInExtractedMethod(v1, v2) &&
				!inconsistentVariableMapping(v1, v2);
	}

	private boolean inconsistentVariableMapping(VariableDeclaration v1, VariableDeclaration v2) {
		if(v1 != null && v2 != null) {
			for(AbstractCodeMapping mapping : mappings) {
				List<VariableDeclaration> variableDeclarations1 = mapping.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mapping.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.contains(v1) &&
						variableDeclarations2.size() > 0 &&
						!variableDeclarations2.contains(v2)) {
					return true;
				}
				if(variableDeclarations2.contains(v2) &&
						variableDeclarations1.size() > 0 &&
						!variableDeclarations1.contains(v1)) {
					return true;
				}
				if(mapping.isExact() && (bothFragmentsUseVariable(v1, mapping) || bothFragmentsUseVariable(v2, mapping))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean bothFragmentsUseVariable(VariableDeclaration v1, AbstractCodeMapping mapping) {
		return mapping.getFragment1().getVariables().contains(v1.getVariableName()) &&
				mapping.getFragment2().getVariables().contains(v1.getVariableName());
	}

	private static boolean containsVariableDeclarationWithName(List<VariableDeclaration> variableDeclarations, String variableName) {
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return true;
			}
		}
		return false;
	}

	private SimpleEntry<VariableDeclaration, UMLOperation> getVariableDeclaration1(Replacement replacement) {
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				VariableDeclaration vd = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
				if(vd != null) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation1);
				}
			}
		}
		for(UMLParameter parameter : operation1.getParameters()) {
			VariableDeclaration vd = parameter.getVariableDeclaration();
			if(vd != null && vd.getVariableName().equals(replacement.getBefore())) {
				return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation1);
			}
		}
		if(callSiteOperation != null) {
			for(UMLParameter parameter : callSiteOperation.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(replacement.getBefore())) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, UMLOperation> getVariableDeclaration1(MergeVariableReplacement replacement, String variableName) {
		for(AbstractCodeMapping mapping : mappings) {
			Set<String> foundMergedVariables = new LinkedHashSet<String>();
			for(Replacement r : mapping.getReplacements()) {
				if(replacement.getMergedVariables().contains(r.getBefore())) {
					foundMergedVariables.add(r.getBefore());
				}
			}
			if(mapping.getReplacements().contains(replacement) || foundMergedVariables.equals(replacement.getMergedVariables())) {
				VariableDeclaration vd = mapping.getFragment1().searchVariableDeclaration(variableName);
				if(vd != null) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation1);
				}
			}
		}
		for(UMLParameter parameter : operation1.getParameters()) {
			VariableDeclaration vd = parameter.getVariableDeclaration();
			if(vd != null && vd.getVariableName().equals(variableName)) {
				return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation1);
			}
		}
		if(callSiteOperation != null) {
			for(UMLParameter parameter : callSiteOperation.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(variableName)) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, UMLOperation> getVariableDeclaration2(Replacement replacement) {
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				VariableDeclaration vd = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				if(vd != null) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation2);
				}
			}
		}
		for(UMLParameter parameter : operation2.getParameters()) {
			VariableDeclaration vd = parameter.getVariableDeclaration();
			if(vd != null && vd.getVariableName().equals(replacement.getAfter())) {
				return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation2);
			}
		}
		if(callSiteOperation != null) {
			for(UMLParameter parameter : callSiteOperation.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(replacement.getAfter())) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, UMLOperation> getVariableDeclaration2(SplitVariableReplacement replacement, String variableName) {
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				Set<String> foundSplitVariables = new LinkedHashSet<String>();
				for(Replacement r : mapping.getReplacements()) {
					if(replacement.getSplitVariables().contains(r.getAfter())) {
						foundSplitVariables.add(r.getAfter());
					}
				}
				if(mapping.getReplacements().contains(replacement) || foundSplitVariables.equals(replacement.getSplitVariables())) {
					VariableDeclaration vd = mapping.getFragment2().searchVariableDeclaration(variableName);
					if(vd != null) {
						return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation2);
					}
				}
			}
		}
		for(UMLParameter parameter : operation2.getParameters()) {
			VariableDeclaration vd = parameter.getVariableDeclaration();
			if(vd != null && vd.getVariableName().equals(variableName)) {
				return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation2);
			}
		}
		if(callSiteOperation != null) {
			for(UMLParameter parameter : callSiteOperation.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(variableName)) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, callSiteOperation);
				}
			}
		}
		return null;
	}

	private SimpleEntry<VariableDeclaration, UMLOperation> getVariableDeclaration2(MergeVariableReplacement replacement) {
		for(AbstractCodeMapping mapping : mappings) {
			Set<String> foundMergedVariables = new LinkedHashSet<String>();
			for(Replacement r : mapping.getReplacements()) {
				if(replacement.getMergedVariables().contains(r.getBefore())) {
					foundMergedVariables.add(r.getBefore());
				}
			}
			if(mapping.getReplacements().contains(replacement) || foundMergedVariables.equals(replacement.getMergedVariables())) {
				VariableDeclaration vd = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				if(vd != null) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation2);
				}
			}
		}
		for(UMLParameter parameter : operation2.getParameters()) {
			VariableDeclaration vd = parameter.getVariableDeclaration();
			if(vd != null && vd.getVariableName().equals(replacement.getAfter())) {
				return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, operation2);
			}
		}
		if(callSiteOperation != null) {
			for(UMLParameter parameter : callSiteOperation.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(replacement.getAfter())) {
					return new SimpleEntry<VariableDeclaration, UMLOperation>(vd, callSiteOperation);
				}
			}
		}
		return null;
	}

	private boolean variableAppearsInExtractedMethod(VariableDeclaration v1, VariableDeclaration v2) {
		if(v1 != null) {
			for(UMLOperationBodyMapper mapper : additionalMappers) {
				for(AbstractCodeMapping mapping : mapper.getMappings()) {
					if(mapping.getFragment1().getVariableDeclarations().contains(v1)) {
						if(v2 != null && v2.getInitializer() != null) {
							UMLOperation extractedMethod = mapper.getOperation2();
							Map<String, OperationInvocation> methodInvocationMap = v2.getInitializer().getMethodInvocationMap();
							for(OperationInvocation invocation : methodInvocationMap.values()) {
								if(invocation.matchesOperation(extractedMethod, operation2.variableTypeMap(), null)) {
									return false;
								}
								else {
									//check if the extracted method is called in the initializer of a variable used in the initializer of v2
									List<String> initializerVariables = v2.getInitializer().getVariables();
									for(String variable : initializerVariables) {
										for(VariableDeclaration declaration : operation2.getAllVariableDeclarations()) {
											if(declaration.getVariableName().equals(variable) && declaration.getInitializer() != null) {
												Map<String, OperationInvocation> methodInvocationMap2 = declaration.getInitializer().getMethodInvocationMap();
												for(OperationInvocation invocation2 : methodInvocationMap2.values()) {
													if(invocation2.matchesOperation(extractedMethod, operation2.variableTypeMap(), null)) {
														return false;
													}
												}
											}
										}
									}
								}
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean existsConflictingExtractVariableRefactoring(RenameVariableRefactoring ref) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariableRef = (ExtractVariableRefactoring)refactoring;
				if(extractVariableRef.getVariableDeclaration().equals(ref.getRenamedVariable()) &&
						extractVariableRef.getOperation().equals(ref.getOperationAfter())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean existsConflictingInlineVariableRefactoring(MergeVariableRefactoring ref) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof InlineVariableRefactoring) {
				InlineVariableRefactoring inline = (InlineVariableRefactoring)refactoring;
				if(ref.getMergedVariables().contains(inline.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean existsConflictingMergeVariableRefactoring(RenameVariableRefactoring ref) {
		for(MergeVariableRefactoring merge : variableMerges) {
			if(merge.getOperationBefore().equals(ref.getOperationBefore()) &&
					merge.getOperationAfter().equals(ref.getOperationAfter()) &&
					merge.getMergedVariables().contains(ref.getOriginalVariable()) &&
					merge.getNewVariable().equals(ref.getRenamedVariable())) {
				return true;
			}
		}
		return false;
	}

	private boolean existsConflictingSplitVariableRefactoring(RenameVariableRefactoring ref) {
		for(SplitVariableRefactoring split : variableSplits) {
			if(split.getOperationBefore().equals(ref.getOperationBefore()) &&
					split.getOperationAfter().equals(ref.getOperationAfter()) &&
					split.getSplitVariables().contains(ref.getRenamedVariable()) &&
					split.getOldVariable().equals(ref.getOriginalVariable())) {
				return true;
			}
		}
		return false;
	}

	private boolean potentialParameterRename(Replacement replacement) {
		int index1 = operation1.getParameterNameList().indexOf(replacement.getBefore());
		if(index1 == -1 && callSiteOperation != null) {
			index1 = callSiteOperation.getParameterNameList().indexOf(replacement.getBefore());
		}
		int index2 = operation2.getParameterNameList().indexOf(replacement.getAfter());
		if(index2 == -1 && callSiteOperation != null) {
			index2 = callSiteOperation.getParameterNameList().indexOf(replacement.getAfter());
		}
		return index1 >= 0 && index1 == index2;
	}
}
