package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObjectMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class MappingOptimizer {
	private UMLAbstractClassDiff classDiff;
	
	public MappingOptimizer(UMLAbstractClassDiff classDiff) {
		this.classDiff = classDiff;
	}

	public void optimizeDuplicateMappingsForMoveCode(List<UMLOperationBodyMapper> moveCodeMappers, List<Refactoring> refactorings) {
		if(moveCodeMappers.size() > 1) {
			Map<AbstractCodeFragment, List<AbstractCodeMapping>> oneToManyMappings = new HashMap<>();
			Map<AbstractCodeFragment, List<UMLOperationBodyMapper>> oneToManyMappers = new HashMap<>();
			for(UMLOperationBodyMapper moveCodeMapper : moveCodeMappers) {
				for(AbstractCodeMapping mapping : moveCodeMapper.getMappings()) {
					AbstractCodeFragment fragmentContainingExpression = null;
					if(oneToManyMappings.containsKey(mapping.getFragment2())) {
						oneToManyMappings.get(mapping.getFragment2()).add(mapping);
						oneToManyMappers.get(mapping.getFragment2()).add(moveCodeMapper);
					}
					else if(mapping.getFragment2() instanceof AbstractExpression &&
							(fragmentContainingExpression = findFragmentContainingExpression(oneToManyMappings.keySet(), (AbstractExpression)mapping.getFragment2())) != null) {
						oneToManyMappings.get(fragmentContainingExpression).add(mapping);
						oneToManyMappers.get(fragmentContainingExpression).add(moveCodeMapper);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<>();
						List<UMLOperationBodyMapper> mappers = new ArrayList<>();
						mappings.add(mapping);
						mappers.add(moveCodeMapper);
						oneToManyMappings.put(mapping.getFragment2(), mappings);
						oneToManyMappers.put(mapping.getFragment2(), mappers);
					}
				}
			}
			optimizeDuplicateMappings(oneToManyMappings, oneToManyMappers, refactorings);
		}
	}

	private AbstractCodeFragment findFragmentContainingExpression(Set<AbstractCodeFragment> fragments, AbstractExpression expression) {
		for(AbstractCodeFragment fragment : fragments) {
			if(fragment instanceof CompositeStatementObject) {
				CompositeStatementObject comp = (CompositeStatementObject)fragment;
				if(comp.getExpressions().contains(expression)) {
					return fragment;
				}
			}
		}
		return null;
	}

	public void optimizeDuplicateMappingsForInline(UMLOperationBodyMapper parentMapper, Collection<Refactoring> refactorings) {
		if(parentMapper.getChildMappers().size() > 0) {
			Map<AbstractCodeFragment, List<AbstractCodeMapping>> oneToManyMappings = new HashMap<>();
			Map<AbstractCodeFragment, List<UMLOperationBodyMapper>> oneToManyMappers = new HashMap<>();
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				for(AbstractCodeMapping mapping : childMapper.getMappings()) {
					AbstractCodeFragment fragmentContainingExpression = null;
					if(oneToManyMappings.containsKey(mapping.getFragment2())) {
						oneToManyMappings.get(mapping.getFragment2()).add(mapping);
						oneToManyMappers.get(mapping.getFragment2()).add(childMapper);
					}
					else if(mapping.getFragment2() instanceof AbstractExpression &&
							(fragmentContainingExpression = findFragmentContainingExpression(oneToManyMappings.keySet(), (AbstractExpression)mapping.getFragment2())) != null) {
						oneToManyMappings.get(fragmentContainingExpression).add(mapping);
						oneToManyMappers.get(fragmentContainingExpression).add(childMapper);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<>();
						List<UMLOperationBodyMapper> mappers = new ArrayList<>();
						mappings.add(mapping);
						mappers.add(childMapper);
						oneToManyMappings.put(mapping.getFragment2(), mappings);
						oneToManyMappers.put(mapping.getFragment2(), mappers);
					}
				}
			}
			for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
				if(oneToManyMappings.containsKey(mapping.getFragment2())) {
					oneToManyMappings.get(mapping.getFragment2()).add(mapping);
					oneToManyMappers.get(mapping.getFragment2()).add(parentMapper);
				}
				else {
					List<AbstractCodeMapping> mappings = new ArrayList<>();
					List<UMLOperationBodyMapper> mappers = new ArrayList<>();
					mappings.add(mapping);
					mappers.add(parentMapper);
					oneToManyMappings.put(mapping.getFragment2(), mappings);
					oneToManyMappers.put(mapping.getFragment2(), mappers);
				}
			}
			optimizeDuplicateMappings(oneToManyMappings, oneToManyMappers, refactorings);
		}
	}

	private boolean subexpressionOverlap(List<AbstractCodeMapping> mappings, AbstractCodeMapping newMapping) {
		for(AbstractCodeMapping previousMapping : mappings) {
			AbstractCodeFragment previousFragment2 = previousMapping.getFragment2();
			AbstractCodeFragment newFragment2 = newMapping.getFragment2();
			if(previousFragment2.getString().startsWith(JAVA.RETURN_SPACE) && previousFragment2.getString().endsWith(JAVA.STATEMENT_TERMINATION) &&
					newFragment2.getString().startsWith(JAVA.RETURN_SPACE) && newFragment2.getString().endsWith(JAVA.STATEMENT_TERMINATION)) {
				String previousReturnExpression = previousFragment2.getString().substring(JAVA.RETURN_SPACE.length(), previousFragment2.getString().length()-JAVA.STATEMENT_TERMINATION.length());
				String newReturnExpression = newFragment2.getString().substring(JAVA.RETURN_SPACE.length(), newFragment2.getString().length()-JAVA.STATEMENT_TERMINATION.length());
				if(previousReturnExpression.contains("(" + newReturnExpression + ")") || newReturnExpression.contains("(" + previousReturnExpression + ")")) {
					return true;
				}
			}
		}
		return false;
	}

	public void optimizeDuplicateMappingsForExtract(UMLOperationBodyMapper parentMapper, Collection<Refactoring> refactorings) {
		if(parentMapper.getChildMappers().size() > 0) {
			Map<AbstractCodeFragment, List<AbstractCodeMapping>> oneToManyMappings = new HashMap<>();
			Map<AbstractCodeFragment, List<UMLOperationBodyMapper>> oneToManyMappers = new HashMap<>();
			for(UMLOperationBodyMapper childMapper : parentMapper.getChildMappers()) {
				for(AbstractCodeMapping mapping : childMapper.getMappings()) {
					AbstractCodeFragment fragmentContainingExpression = null;
					if(oneToManyMappings.containsKey(mapping.getFragment1())) {
						if(!subexpressionOverlap(oneToManyMappings.get(mapping.getFragment1()), mapping)) {
							oneToManyMappings.get(mapping.getFragment1()).add(mapping);
							oneToManyMappers.get(mapping.getFragment1()).add(childMapper);
						}
					}
					else if(mapping.getFragment1() instanceof AbstractExpression &&
							(fragmentContainingExpression = findFragmentContainingExpression(oneToManyMappings.keySet(), (AbstractExpression)mapping.getFragment1())) != null) {
						oneToManyMappings.get(fragmentContainingExpression).add(mapping);
						oneToManyMappers.get(fragmentContainingExpression).add(childMapper);
					}
					else {
						List<AbstractCodeMapping> mappings = new ArrayList<>();
						List<UMLOperationBodyMapper> mappers = new ArrayList<>();
						mappings.add(mapping);
						mappers.add(childMapper);
						oneToManyMappings.put(mapping.getFragment1(), mappings);
						oneToManyMappers.put(mapping.getFragment1(), mappers);
					}
				}
			}
			for(AbstractCodeMapping mapping : parentMapper.getMappings()) {
				if(oneToManyMappings.containsKey(mapping.getFragment1())) {
					oneToManyMappings.get(mapping.getFragment1()).add(mapping);
					oneToManyMappers.get(mapping.getFragment1()).add(parentMapper);
				}
				else {
					List<AbstractCodeMapping> mappings = new ArrayList<>();
					List<UMLOperationBodyMapper> mappers = new ArrayList<>();
					mappings.add(mapping);
					mappers.add(parentMapper);
					oneToManyMappings.put(mapping.getFragment1(), mappings);
					oneToManyMappers.put(mapping.getFragment1(), mappers);
				}
			}
			optimizeDuplicateMappings(oneToManyMappings, oneToManyMappers, refactorings);
		}
	}

	private void optimizeDuplicateMappings(Map<AbstractCodeFragment, List<AbstractCodeMapping>> oneToManyMappings,
			Map<AbstractCodeFragment, List<UMLOperationBodyMapper>> oneToManyMappers, Collection<Refactoring> refactorings) {
		for(Iterator<AbstractCodeFragment> it = oneToManyMappers.keySet().iterator(); it.hasNext();) {
			AbstractCodeFragment fragment = it.next();
			if(oneToManyMappings.get(fragment).size() == 1) {
				oneToManyMappings.remove(fragment);
			}
		}
		//sort oneToManyMappings keys to put first composite statements, then blocks, then leaf statements
		TreeSet<AbstractCodeFragment> sortedKeys = new TreeSet<>(new CodeFragmentComparator());
		sortedKeys.addAll(oneToManyMappings.keySet());
		Set<UMLOperationBodyMapper> updatedMappers = new LinkedHashSet<>();
		for(AbstractCodeFragment fragment : sortedKeys) {
			List<AbstractCodeMapping> mappings = oneToManyMappings.get(fragment);
			List<UMLOperationBodyMapper> mappers = oneToManyMappers.get(fragment);
			boolean identicalExtractedMethod = false;
			int exactMappers = 0;
			for(UMLOperationBodyMapper mapper : mappers) {
				if(mapper.mappingsWithoutBlocks() == mapper.exactMatches()) {
					exactMappers++;
				}
				if(mapper.getParentMapper() != null && mapper.getParentMapper().getExtractedStatements().keySet().contains(mapper.getContainer2()) &&
						mapper.getContainer1().getBody() != null && mapper.getContainer2().getBody() != null &&
						mapper.getContainer1().getBody().stringRepresentation().equals(mapper.getContainer2().getBody().stringRepresentation())) {
					identicalExtractedMethod = true;
					break;
				}
			}
			if(identicalExtractedMethod && exactMappers == mappers.size()) {
				continue;
			}
			Iterator<AbstractCodeMapping> mappingIterator = mappings.iterator();
			Iterator<UMLOperationBodyMapper> mapperIterator = mappers.iterator();
			List<Boolean> callsExtractedInlinedMethod = new ArrayList<>();
			List<Boolean> parentMappingFound = new ArrayList<>();
			List<Boolean> parentIsContainerBody = new ArrayList<>();
			List<Boolean> nestedMapper = new ArrayList<>();
			List<Boolean> identical = new ArrayList<>();
			List<Integer> identicalStatementsForCompositeMappings = new ArrayList<>();
			List<Integer> exactMappingsNestedUnderCompositeExcludingBlocks = new ArrayList<>();
			List<Integer> nonMappedNodes = new ArrayList<>();
			List<Integer> replacementTypeCount = new ArrayList<>();
			List<Boolean> replacementCoversEntireStatement = new ArrayList<>();
			List<Boolean> extractInlineOverlappingRefactoring = new ArrayList<>();
			List<UMLOperationBodyMapper> parentMappers = new ArrayList<>();
			List<Double> editDistances = new ArrayList<>();
			//check if mappings are the same references
			Set<AbstractCodeMapping> mappingsAsSet = new LinkedHashSet<>();
			mappingsAsSet.addAll(mappings);
			Set<UMLOperationBodyMapper> mappersAsSet = new LinkedHashSet<>();
			mappersAsSet.addAll(mappers);
			if(mappingsAsSet.size() == 1 || (mappersAsSet.size() == 1 && mappersAsSet.iterator().next().getParentMapper() == null)) {
				continue;
			}
			while(mappingIterator.hasNext()) {
				AbstractCodeMapping mapping = mappingIterator.next();
				UMLOperationBodyMapper mapper = mapperIterator.next();
				if(mapping instanceof CompositeStatementObjectMapping) {
					CompositeStatementObject comp1 = (CompositeStatementObject)mapping.getFragment1();
					CompositeStatementObject comp2 = (CompositeStatementObject)mapping.getFragment2();
					List<String> stringRepresentation1 = comp1.stringRepresentation();
					List<String> stringRepresentation2 = comp2.stringRepresentation();
					int minSize = Math.min(stringRepresentation1.size(), stringRepresentation2.size());
					int identicalStatements = 0;
					for(int i=0; i<minSize; i++) {
						if(stringRepresentation1.get(i).equals(stringRepresentation2.get(i)) &&
								!stringRepresentation1.get(i).equals(JAVA.OPEN_BLOCK) && !stringRepresentation1.get(i).equals(JAVA.CLOSE_BLOCK)) {
							identicalStatements++;
						}
					}
					identicalStatementsForCompositeMappings.add(identicalStatements);
					exactMappingsNestedUnderCompositeExcludingBlocks.add(mapper.exactMappingsNestedUnderCompositeExcludingBlocks((CompositeStatementObjectMapping)mapping));
				}
				else {
					identicalStatementsForCompositeMappings.add(0);
					exactMappingsNestedUnderCompositeExcludingBlocks.add(0);
				}
				callsExtractedInlinedMethod.add(mapper.containsExtractedOrInlinedOperationInvocation(mapping));
				parentMappingFound.add(mapper.containsParentMapping(mapping));
				parentIsContainerBody.add(mapper.parentIsContainerBody(mapping));
				nestedMapper.add(mapper.isNested());
				identical.add(mapping.getFragment1().getString().equals(mapping.getFragment2().getString()));
				if(mapper.getParentMapper() != null) {
					if(mapper.getContainer1().equals(mapper.getParentMapper().getContainer1()) && !mapper.getContainer2().equals(mapper.getParentMapper().getContainer2())) {
						//extract method scenario
						nonMappedNodes.add(mapper.nonMappedElementsT2());
					}
					else if(!mapper.getContainer1().equals(mapper.getParentMapper().getContainer1()) && mapper.getContainer2().equals(mapper.getParentMapper().getContainer2())) {
						//inline method scenario
						nonMappedNodes.add(mapper.nonMappedElementsT1());
					}
				}
				else {
					nonMappedNodes.add(0);
				}
				replacementTypeCount.add(mapper.getReplacementTypesExcludingParameterToArgumentMaps(mapping).size());
				boolean replacementFound = false;
				for(Replacement r : mapping.getReplacements()) {
					if((r.getBefore().equals(mapping.getFragment1().getString()) || (r.getBefore() + JAVA.STATEMENT_TERMINATION).equals(mapping.getFragment1().getString())) &&
							(r.getAfter().equals(mapping.getFragment2().getString()) || (r.getAfter() + JAVA.STATEMENT_TERMINATION).equals(mapping.getFragment2().getString()))) {
						replacementFound = true;
						break;
					}
				}
				replacementCoversEntireStatement.add(replacementFound);
				boolean containsExtractInlineVariableRefactoring = false;
				for(Refactoring r : mapping.getRefactorings()) {
					if(r instanceof ExtractVariableRefactoring || r instanceof InlineVariableRefactoring) {
						containsExtractInlineVariableRefactoring = true;
						break;
					}
				}
				if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable() || containsExtractInlineVariableRefactoring) {
					extractInlineOverlappingRefactoring.add(true);
				}
				else {
					extractInlineOverlappingRefactoring.add(false);
				}
				parentMappers.add(mapper.getParentMapper());
				editDistances.add(mapping.editDistance());
			}
			Set<Integer> indicesToBeRemoved = new LinkedHashSet<>();
			if(callsExtractedInlinedMethod.contains(true) && callsExtractedInlinedMethod.contains(false)) {
				for(int i=0; i<callsExtractedInlinedMethod.size(); i++) {
					if(callsExtractedInlinedMethod.get(i) == true && !callToExtractedInlinedMethodIsArgument(mappings.get(i), mappers.get(callsExtractedInlinedMethod.indexOf(false)))) {
						indicesToBeRemoved.add(i);
					}
				}
				if(matchingParentMappers(parentMappers, mappings) > 1) {
					if(parentMappingFound.contains(true)) {
						for(int i=0; i<parentMappingFound.size(); i++) {
							if(parentMappingFound.get(i) == false) {
								indicesToBeRemoved.add(i);
							}
						}
						determineIndicesToBeRemoved(nestedMapper, identical, exactMappingsNestedUnderCompositeExcludingBlocks, replacementTypeCount, replacementCoversEntireStatement, extractInlineOverlappingRefactoring, indicesToBeRemoved, editDistances);
					}
				}
			}
			else if(parentMappingFound.contains(true)) {
				boolean anonymousClassDeclarationMatch = false;
				boolean splitConditional = false;
				boolean splitDeclaration = false;
				for(int i=0; i<parentMappingFound.size(); i++) {
					if(parentMappingFound.get(i) == false) {
						//check if composite mapping in index i has more identical statements
						boolean skip = false;
						if(!identicalStatementsForCompositeMappings.isEmpty()) {
							int indexOfTrueParentMapping = parentMappingFound.indexOf(true);
							if(identicalStatementsForCompositeMappings.get(i) > identicalStatementsForCompositeMappings.get(indexOfTrueParentMapping)) {
								skip = true;
							}
							if(mappings.get(i).getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
									mappings.get(i).getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
								String condition = mappings.get(i).getFragment2().getString().substring(3, mappings.get(i).getFragment2().getString().length()-1);
								String conditionOfTrueParentMapping = mappings.get(indexOfTrueParentMapping).getFragment2().getString().substring(3, mappings.get(indexOfTrueParentMapping).getFragment2().getString().length()-1);
								if(mappings.get(i).getFragment1().getString().contains(condition) && mappings.get(indexOfTrueParentMapping).getFragment1().getString().contains(conditionOfTrueParentMapping)) {
									splitConditional = true;
								}
							}
						}
						if(parentIsContainerBody.get(i) == true && editDistances.get(i).equals(editDistances.get(parentMappingFound.indexOf(true))) &&
								!mappings.get(i).getFragment1().getString().startsWith(JAVA.RETURN_SPACE) && !mappings.get(i).getFragment2().getString().startsWith(JAVA.RETURN_SPACE)) {
							skip = true;
						}
						if(parentIsContainerBody.get(i) == true && mappings.get(i).getFragment1().getAnonymousClassDeclarations().size() > 0 && mappings.get(i).getFragment2().getAnonymousClassDeclarations().size() > 0) {
							skip = true;
							anonymousClassDeclarationMatch = true;
						}
						List<VariableDeclaration> fragment2VariableDeclarations = mappings.get(i).getFragment2().getVariableDeclarations();
						if(parentIsContainerBody.get(i) == true && fragment2VariableDeclarations.size() > 0 &&
								mappings.get(parentMappingFound.indexOf(true)).getFragment2().getString().startsWith(fragment2VariableDeclarations.get(0).getVariableName() + JAVA.ASSIGNMENT)) {
							skip = true;
							splitDeclaration = true;
						}
						if(!skip) {
							indicesToBeRemoved.add(i);
						}
					}
				}
				if(!anonymousClassDeclarationMatch && !splitConditional && !splitDeclaration)
					determineIndicesToBeRemoved(nestedMapper, identical, exactMappingsNestedUnderCompositeExcludingBlocks, replacementTypeCount, replacementCoversEntireStatement, extractInlineOverlappingRefactoring, indicesToBeRemoved, editDistances);
			}
			else if(parentIsContainerBody.contains(true)) {
				boolean splitConditional = false;
				boolean splitDeclaration = false;
				for(int i=0; i<parentIsContainerBody.size(); i++) {
					if(parentIsContainerBody.get(i) == false && !nestedMapper.get(parentIsContainerBody.indexOf(true))) {
						//check if composite mapping in index i has more identical statements
						boolean skip = false;
						if(!identicalStatementsForCompositeMappings.isEmpty()) {
							int indexOfTrueParentIsContainerBody = parentIsContainerBody.indexOf(true);
							if(identicalStatementsForCompositeMappings.get(i) > identicalStatementsForCompositeMappings.get(indexOfTrueParentIsContainerBody)) {
								skip = true;
							}
							if(mappings.get(i).getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
									mappings.get(i).getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
									mappings.get(indexOfTrueParentIsContainerBody).getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
								String condition = mappings.get(i).getFragment2().getString().substring(3, mappings.get(i).getFragment2().getString().length()-1);
								String conditionOfTrueParentIsContainerBody = mappings.get(indexOfTrueParentIsContainerBody).getFragment2().getString().substring(3, mappings.get(indexOfTrueParentIsContainerBody).getFragment2().getString().length()-1);
								if(mappings.get(i).getFragment1().getString().contains(condition) && mappings.get(indexOfTrueParentIsContainerBody).getFragment1().getString().contains(conditionOfTrueParentIsContainerBody)) {
									splitConditional = true;
								}
							}
						}
						List<VariableDeclaration> fragment2VariableDeclarations = mappings.get(parentIsContainerBody.indexOf(true)).getFragment2().getVariableDeclarations();
						if(fragment2VariableDeclarations.size() > 0 &&
								mappings.get(i).getFragment2().getString().startsWith(fragment2VariableDeclarations.get(0).getVariableName() + JAVA.ASSIGNMENT)) {
							skip = true;
							splitDeclaration = true;
						}
						if(!skip) {
							indicesToBeRemoved.add(i);
						}
					}
				}
				if(!splitConditional && !splitDeclaration)
					determineIndicesToBeRemoved(nestedMapper, identical, exactMappingsNestedUnderCompositeExcludingBlocks, replacementTypeCount, replacementCoversEntireStatement, extractInlineOverlappingRefactoring, indicesToBeRemoved, editDistances);
			}
			else {
				determineIndicesToBeRemoved(nestedMapper, identical, exactMappingsNestedUnderCompositeExcludingBlocks, replacementTypeCount, replacementCoversEntireStatement, extractInlineOverlappingRefactoring, indicesToBeRemoved, editDistances);
			}
			if(indicesToBeRemoved.isEmpty() && matchingParentMappers(parentMappers, mappings) == parentMappers.size()) {
				int minimum = nonMappedNodes.get(0);
				for(int i=1; i<nonMappedNodes.size(); i++) {
					if(nonMappedNodes.get(i) < minimum) {
						minimum = nonMappedNodes.get(i);
					}
				}
				for(int i=0; i<nonMappedNodes.size(); i++) {
					if(nonMappedNodes.get(i) > minimum) {
						indicesToBeRemoved.add(i);
					}
				}
			}
			mappingIterator = mappings.iterator();
			mapperIterator = mappers.iterator();
			int index = 0;
			boolean atLeastOneMappingCallsExtractedOrInlinedMethodWithVariableDeclarationOrThrow = 
					atLeastOneMappingCallsExtractedOrInlinedMethodWithVariableDeclarationOrThrow(mappings, mappers);
			while(mappingIterator.hasNext()) {
				AbstractCodeMapping mapping = mappingIterator.next();
				UMLOperationBodyMapper mapper = mapperIterator.next();
				if(indicesToBeRemoved.contains(index)) {
					if(!atLeastOneMappingCallsExtractedOrInlinedMethodWithVariableDeclarationOrThrow) {
						mapper.removeMapping(mapping);
						for(LeafMapping leafMapping : mapping.getSubExpressionMappings()) {
							mapper.removeMapping(leafMapping);
						}
						if(mapping instanceof LeafMapping) {
							if(!mapper.getNonMappedLeavesT1().contains(mapping.getFragment1())) {
								mapper.getNonMappedLeavesT1().add(mapping.getFragment1());
							}
							if(!mapper.getNonMappedLeavesT2().contains(mapping.getFragment2())) {
								mapper.getNonMappedLeavesT2().add(mapping.getFragment2());
							}
						}
						else if(mapping instanceof CompositeStatementObjectMapping) {
							if(!mapper.getNonMappedInnerNodesT1().contains(mapping.getFragment1())) {
								mapper.getNonMappedInnerNodesT1().add((CompositeStatementObject) mapping.getFragment1());
							}
							if(!mapper.getNonMappedInnerNodesT2().contains(mapping.getFragment2())) {
								mapper.getNonMappedInnerNodesT2().add((CompositeStatementObject) mapping.getFragment2());
							}
						}
						//remove refactorings based on mapping
						Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<Refactoring>();
						Set<Refactoring> refactoringsAfterPostProcessing = mapper.getRefactoringsAfterPostProcessing();
						for(Refactoring r : refactoringsAfterPostProcessing) {
							if(r instanceof ReferenceBasedRefactoring) {
								ReferenceBasedRefactoring referenceBased = (ReferenceBasedRefactoring)r;
								Set<AbstractCodeMapping> references = referenceBased.getReferences();
								if(references.contains(mapping)) {
									refactoringsToBeRemoved.add(r);
								}
							}
							else if(r instanceof InvertConditionRefactoring) {
								InvertConditionRefactoring invert = (InvertConditionRefactoring)r;
								if(mapping.getFragment1().equals(invert.getOriginalConditional()) || mapping.getFragment2().equals(invert.getInvertedConditional())) {
									refactoringsToBeRemoved.add(r);
								}
							}
							else if(r instanceof ReplaceConditionalWithTernaryRefactoring) {
								ReplaceConditionalWithTernaryRefactoring replace = (ReplaceConditionalWithTernaryRefactoring)r;
								if(mapping.getFragment1().equals(replace.getOriginalConditional()) || mapping.getFragment2().equals(replace.getTernaryConditional())) {
									boolean ternaryCallsExtractedMethod = false;
									for(UMLOperationBodyMapper childMapper : mapper.getChildMappers()) {
										List<AbstractCall> calls = mapping.getFragment2().getMethodInvocations();
										for(AbstractCall call : calls) {
											if(call.matchesOperation(childMapper.getContainer2(), mapper.getContainer2(), classDiff, mapper.getModelDiff())) {
												ternaryCallsExtractedMethod = true;
												break;
											}
										}
									}
									if(!ternaryCallsExtractedMethod) {
										refactoringsToBeRemoved.add(r);
									}
								}
							}
						}
						refactoringsAfterPostProcessing.removeAll(refactoringsToBeRemoved);
						updatedMappers.add(mapper);
					}
				}
				index++;
			}
		}
		Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<>();
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring refactoring = (ExtractOperationRefactoring)ref;
				if(updatedMappers.contains(refactoring.getBodyMapper())) {
					if(refactoring.getBodyMapper().getMappings().size() == 0) {
						refactoringsToBeRemoved.add(refactoring);
					}
					else {
						refactoring.updateMapperInfo();
					}
				}
			}
			else if(ref instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring refactoring = (InlineOperationRefactoring)ref;
				if(updatedMappers.contains(refactoring.getBodyMapper())) {
					if(refactoring.getBodyMapper().getMappings().size() == 0) {
						refactoringsToBeRemoved.add(refactoring);
					}
					else {
						refactoring.updateMapperInfo();
					}
				}
			}
			else if(ref instanceof MoveCodeRefactoring) {
				MoveCodeRefactoring refactoring = (MoveCodeRefactoring)ref;
				if(updatedMappers.contains(refactoring.getBodyMapper())) {
					if(refactoring.getBodyMapper().getMappings().size() == 0) {
						refactoringsToBeRemoved.add(refactoring);
					}
					else {
						refactoring.updateMapperInfo();
					}
				}
			}
		}
		refactorings.removeAll(refactoringsToBeRemoved);
	}

	private boolean callToExtractedInlinedMethodIsArgument(AbstractCodeMapping mapping, UMLOperationBodyMapper mapper) {
		if(mapper.getOperationInvocation() != null) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			for(AbstractCall call : fragment1.getCreations()) {
				if(call.arguments().contains(mapper.getOperationInvocation().actualString())) {
					return true;
				}
			}
			for(AbstractCall call : fragment1.getMethodInvocations()) {
				if(call.arguments().contains(mapper.getOperationInvocation().actualString())) {
					return true;
				}
			}
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			for(AbstractCall call : fragment2.getCreations()) {
				if(call.arguments().contains(mapper.getOperationInvocation().actualString())) {
					return true;
				}
			}
			for(AbstractCall call : fragment2.getMethodInvocations()) {
				if(call.arguments().contains(mapper.getOperationInvocation().actualString())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean atLeastOneMappingCallsExtractedOrInlinedMethodWithVariableDeclarationOrThrow(List<AbstractCodeMapping> mappings, List<UMLOperationBodyMapper> mappers) {
		Set<AbstractCall> operationInvocations = new LinkedHashSet<>();
		for(UMLOperationBodyMapper mapper : mappers) {
			if(mapper.getOperationInvocation() != null) {
				operationInvocations.add(mapper.getOperationInvocation());
			}
		}
		int matches = 0;
		boolean identicalMapping = false;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
				identicalMapping = true;
			}
			for(AbstractCall operationInvocation : operationInvocations) {
				if(callsExtractedOrInlinedMethodWithVariableDeclarationOrThrow(mapping, operationInvocation)) {
					matches++;
				}
			}
		}
		if(matches == operationInvocations.size() && !identicalMapping) {
			return true;
		}
		return false;
	}

	private boolean callsExtractedOrInlinedMethodWithVariableDeclarationOrThrow(AbstractCodeMapping mapping, AbstractCall operationInvocation) {
		if(operationInvocation != null) {
			if(stringBasedInvocationMatch(mapping.getFragment1(), operationInvocation)) {
				return true;
			}
			if(stringBasedInvocationMatch(mapping.getFragment2(), operationInvocation)) {
				return true;
			}
		}
		return false;
	}

	private boolean stringBasedInvocationMatch(AbstractCodeFragment callFragment, AbstractCall operationInvocation) {
		AbstractCall invocation = callFragment.invocationCoveringEntireFragment();
		if(invocation == null) {
			invocation = callFragment.fieldAssignmentInvocationCoveringEntireStatement(classDiff);
			if(invocation != null && invocation.actualString().equals(operationInvocation.actualString())) {
				return true;
			}
		}
		if(invocation == null && callFragment.getVariableDeclarations().size() > 0) {
			for(AbstractCall call : callFragment.getMethodInvocations()) {
				if(call.actualString().equals(operationInvocation.actualString())) {
					return true;
				}
			}
		}
		if(invocation != null && invocation.actualString().equals(operationInvocation.actualString())) {
			if(invocation.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL)) {
				return true;
			}
			String expression = invocation.getExpression();
			if(expression != null && !expression.equals("this")) {
				return true;
			}
		}
		if(invocation != null) {
			for(String argument : invocation.arguments()) {
				if(argument.contains(operationInvocation.actualString())) {
					return true;
				}
			}
			if(invocation.getExpression() != null && invocation.getExpression().equals(operationInvocation.actualString())) {
				return true;
			}
		}
		AbstractCall creation = callFragment.creationCoveringEntireFragment();
		if(creation != null && creation.actualString().contains(operationInvocation.actualString())) {
			return true;
		}
		return false;
	}

	private int matchingParentMappers(List<UMLOperationBodyMapper> parentMappers, List<AbstractCodeMapping> mappings) {
		int matchingParentMappers = 1;
		for(int i=1; i<parentMappers.size(); i++) {
			if(parentMappers.get(i) != null && parentMappers.get(i).equals(parentMappers.get(i-1))) {
				boolean identicalMapping = mappings.get(i).getFragment1().equals(mappings.get(i-1).getFragment1()) &&
						mappings.get(i).getFragment2().equals(mappings.get(i-1).getFragment2());
				if(!identicalMapping)
					matchingParentMappers++;
			}
		}
		return matchingParentMappers;
	}

	private void determineIndicesToBeRemoved(List<Boolean> nestedMapper, List<Boolean> identical,
			List<Integer> exactMappingsNestedUnderCompositeExcludingBlocks,
			List<Integer> replacementTypeCount, List<Boolean> replacementCoversEntireStatement,
			List<Boolean> extractInlineOverlappingRefactoring, Set<Integer> indicesToBeRemoved, List<Double> editDistances) {
		if(indicesToBeRemoved.isEmpty()) {
			if(nestedMapper.contains(false)) {
				double editDistanceFalseNestedMapper = editDistances.get(nestedMapper.indexOf(false));
				for(int i=0; i<nestedMapper.size(); i++) {
					if(nestedMapper.get(i) == true && identical.get(i) == false && editDistances.get(i) > editDistanceFalseNestedMapper) {
						indicesToBeRemoved.add(i);
					}
				}
			}
			if(identical.contains(true)) {
				for(int i=0; i<identical.size(); i++) {
					if(identical.get(i) == false) {
						indicesToBeRemoved.add(i);
					}
				}
				if(indicesToBeRemoved.isEmpty()) {
					int zeroCount = 0;
					for(int i=0; i<exactMappingsNestedUnderCompositeExcludingBlocks.size(); i++) {
						if(exactMappingsNestedUnderCompositeExcludingBlocks.get(i) == 0) {
							zeroCount++;
						}
					}
					if(zeroCount == 1) {
						for(int i=0; i<exactMappingsNestedUnderCompositeExcludingBlocks.size(); i++) {
							if(exactMappingsNestedUnderCompositeExcludingBlocks.get(i) == 0) {
								indicesToBeRemoved.add(i);
							}
						}
					}
				}
			}
			else {
				boolean allReplacementsCoverEntireStatement = false;
				if(replacementCoversEntireStatement.contains(false)) {
					for(int i=0; i<replacementCoversEntireStatement.size(); i++) {
						if(replacementCoversEntireStatement.get(i) == true) {
							indicesToBeRemoved.add(i);
						}
					}
				}
				else {
					allReplacementsCoverEntireStatement = true;
				}
				if(!allReplacementsCoverEntireStatement) {
					int minimum = replacementTypeCount.get(0);
					for(int i=1; i<replacementTypeCount.size(); i++) {
						if(replacementTypeCount.get(i) < minimum) {
							minimum = replacementTypeCount.get(i);
						}
					}
					for(int i=0; i<replacementTypeCount.size(); i++) {
						if(replacementTypeCount.get(i) > minimum && !extractInlineOverlappingRefactoring.get(i) == true) {
							indicesToBeRemoved.add(i);
						}
					}
				}
				if(indicesToBeRemoved.isEmpty()) {
					double minimumEditDistance = editDistances.get(0);
					for(int i=1; i<editDistances.size(); i++) {
						if(editDistances.get(i) < minimumEditDistance) {
							minimumEditDistance = editDistances.get(i);
						}
					}
					for(int i=0; i<editDistances.size(); i++) {
						if(editDistances.get(i) > minimumEditDistance) {
							indicesToBeRemoved.add(i);
						}
					}
				}
			}
		}
	}
}
