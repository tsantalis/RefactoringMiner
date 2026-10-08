package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.isForEach;
import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.streamAPICalls;
import static gr.uom.java.xmi.decomposition.ReplacementAlgorithm.streamAPIName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.ReplaceLoopWithPipelineRefactoring;
import gr.uom.java.xmi.diff.ReplacePipelineWithLoopRefactoring;

class StreamAPIMatcher {
	private final UMLOperationBodyMapper mapper;

	StreamAPIMatcher(UMLOperationBodyMapper mapper) {
		this.mapper = mapper;
	}

	static Set<AbstractCodeFragment> statementsWithStreamAPICalls(List<AbstractCodeFragment> leaves, Constants LANG) {
		Set<AbstractCodeFragment> streamAPICalls = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeFragment statement : leaves) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = statement.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && (invocation.actualString().contains(LANG.LAMBDA_ARROW) ||
					invocation.actualString().contains(" ->\n") ||
					invocation.actualString().contains(LANG.METHOD_REFERENCE))) {
				for(AbstractCall inv : statement.getMethodInvocations()) {
					if(streamAPIName(inv.getName())) {
						streamAPICalls.add(statement);
						break;
					}
				}
			}
		}
		return streamAPICalls;
	}

	private AbstractCodeFragment containLambdaExpression(List<AbstractCodeFragment> compositeLeaves, AbstractCodeFragment lambdaExpression) {
		for(AbstractCodeFragment leaf : compositeLeaves) {
			for(LambdaExpressionObject lambda : leaf.getLambdas()) {
				if(lambda.getExpression() != null && lambda.getExpression().equals(lambdaExpression)) {
					return leaf;
				}
			}
		}
		return null;
	}

	private boolean nestedLambdaExpressionMatch(List<LambdaExpressionObject> lambdas, AbstractCodeFragment lambdaExpression) {
		for(LambdaExpressionObject lambda : lambdas) {
			if(lambda.getExpression() != null) {
				if(lambda.getExpression().equals(lambdaExpression)) {
					return true;
				}
				else if(nestedLambdaExpressionMatch(lambda.getExpression().getLambdas(), lambdaExpression)) {
					return true;
				}
			}
			else if(lambda.getBody() != null) {
				if(lambda.getBody().getCompositeStatement().getLocationInfo().subsumes(lambdaExpression.getLocationInfo())) {
					boolean foundInNested = false;
					for(LambdaExpressionObject nestedLambda : lambda.getAllLambdas()) {
						if(nestedLambda.getExpression() != null) {
							if(nestedLambda.getExpression().equals(lambdaExpression)) {
								foundInNested = true;
								break;
							}
						}
					}
					if(!foundInNested) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private List<VariableDeclaration> nestedLambdaParameters(List<LambdaExpressionObject> lambdas) {
		List<VariableDeclaration> lambdaParameters = new ArrayList<>();
		for(LambdaExpressionObject lambda : lambdas) {
			lambdaParameters.addAll(lambda.getParameters());
			if(lambda.getExpression() != null) {
				lambdaParameters.addAll(nestedLambdaParameters(lambda.getExpression().getLambdas()));
			}
		}
		return lambdaParameters;
	}

	void processStreamAPIStatements(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			Set<AbstractCodeFragment> streamAPIStatements1, List<CompositeStatementObject> innerNodes2)
			throws RefactoringMinerTimedOutException {
		//match expressions in inner nodes from T2 with leaves from T1
		List<AbstractExpression> expressionsT2 = new ArrayList<AbstractExpression>();
		for(CompositeStatementObject composite : innerNodes2) {
			if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT2.add(expression);
				}
			}
		}
		int numberOfMappings = mapper.getMappings().size();
		mapper.processLeaves(leaves1, expressionsT2, new LinkedHashMap<String, String>(), false);
		
		List<AbstractCodeMapping> mappings = new ArrayList<>(mapper.getMappings());
		if(numberOfMappings == mappings.size()) {
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator2.next();
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
				List<AbstractCodeFragment> compositeLeaves = composite.getLeaves();
				for(AbstractCodeMapping mapping : mappings) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					if(composite.isLoop() &&
							(compositeLeaves.contains(fragment2) || containLambdaExpression(compositeLeaves, fragment2) != null)) {
						AbstractCodeFragment streamAPICallStatement = null;
						List<AbstractCall> streamAPICalls = null;
						for(AbstractCodeFragment leaf1 : streamAPIStatements1) {
							if(leaves1.contains(leaf1)) {
								boolean matchingLambda = nestedLambdaExpressionMatch(leaf1.getLambdas(), fragment1);
								AbstractCall call = leaf1.invocationCoveringEntireFragment();
								boolean findCall = call != null && (call.getName().equals("findFirst") || call.getName().equals("findAny"));
								if(matchingLambda || findCall) {
									streamAPICallStatement = leaf1;
									streamAPICalls = streamAPICalls(leaf1);
									break;
								}
							}
						}
						if(streamAPICallStatement != null && streamAPICalls != null) {
							List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
							List<LeafMapping> leafMappings = new ArrayList<LeafMapping>();
							AbstractCall call1 = fragment1.invocationCoveringEntireFragment();
							AbstractCall call2 = fragment2.invocationCoveringEntireFragment();
							if(call1 != null && call2 != null) {
								LeafMapping leafMapping = new LeafMapping(call1, call2, mapper.getContainer1(), mapper.getContainer2());
								leafMappings.add(leafMapping);
							}
							else {
								call1 = fragment1.creationCoveringEntireFragment();
								call2 = fragment2.creationCoveringEntireFragment();
								if(call1 != null && call2 != null) {
									LeafMapping leafMapping = new LeafMapping(call1, call2, mapper.getContainer1(), mapper.getContainer2());
									leafMappings.add(leafMapping);
								}
								else if(fragment1 instanceof AbstractExpression) {
									List<LeafExpression> leafExpressions = fragment2.findExpression(fragment1.getString());
									for(LeafExpression leafExpression : leafExpressions) {
										LeafMapping leafMapping = new LeafMapping(fragment1, leafExpression, mapper.getContainer1(), mapper.getContainer2());
										leafMappings.add(leafMapping);
									}
								}
							}
							additionallyMatchedStatements1.add(streamAPICallStatement);
							additionallyMatchedStatements2.add(fragment2);
							for(AbstractCall streamAPICall : streamAPICalls) {
								if(isForEach(streamAPICall.getName())) {
									if(!additionallyMatchedStatements2.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(leafExpression, expression, mapper.getContainer1(), mapper.getContainer2());
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements2.add(composite);
												break;
											}
										}
									}
								}
								else if(streamAPICall.getName().equals("stream")) {
									if(!additionallyMatchedStatements2.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(leafExpression, expression, mapper.getContainer1(), mapper.getContainer2());
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements2.add(composite);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(argument);
													for(LeafExpression leafExpression : leafExpressions) {
														LeafMapping leafMapping = new LeafMapping(leafExpression, expression, mapper.getContainer1(), mapper.getContainer2());
														leafMappings.add(leafMapping);
													}
													additionallyMatchedStatements2.add(composite);
													break;
												}
											}
										}
									}
								}
							}
							CompositeReplacement replacement = new CompositeReplacement(streamAPICallStatement.getString(), composite.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
							Set<Replacement> replacements = new LinkedHashSet<>();
							replacements.add(replacement);
							LeafMapping newMapping = mapper.createLeafMapping(streamAPICallStatement, composite, new LinkedHashMap<String, String>(), false, false);
							newMapping.addReplacements(replacements);
							TreeSet<LeafMapping> mappingSet = new TreeSet<>();
							mappingSet.add(newMapping);
							if(!additionallyMatchedStatements2.contains(composite)) {
								additionallyMatchedStatements2.add(composite);
							}
							for(VariableDeclaration lambdaParameter : lambdaParameters) {
								for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
									if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
										Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
										mapper.matchedVariables.add(pair);
									}
									else {
										for(Replacement r : mapping.getReplacements()) {
											if(r.getBefore().equals(lambdaParameter.getVariableName()) && r.getAfter().equals(compositeParameter.getVariableName())) {
												Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
												mapper.matchedVariables.add(pair);
												break;
											}
										}
									}
								}
							}
							ReplacePipelineWithLoopRefactoring ref = new ReplacePipelineWithLoopRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, mapper.getContainer1(), mapper.getContainer2());
							for(LeafMapping leafMapping : leafMappings) {
								ref.addSubExpressionMapping(leafMapping);
							}
							for(AbstractCodeMapping m : mapper.getMappings()) {
								if(composite.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment1().getLocationInfo())) {
									ref.addNestedStatementMapping(m);
								}
							}
							newMapping.addRefactoring(ref);
							mapper.addToMappings(newMapping, mappingSet);
							leaves1.remove(newMapping.getFragment1());
						}
					}
				}
				if(additionallyMatchedStatements2.contains(composite)) {
					innerNodeIterator2.remove();
				}
			}
		}
		for(int i = numberOfMappings; i < mappings.size(); i++) {
			AbstractCodeMapping mapping = mappings.get(i);
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator2.next();
				if(composite.getExpressions().contains(fragment2)) {
					AbstractCodeFragment streamAPICallStatement = null;
					List<AbstractCall> streamAPICalls = null;
					for(AbstractCodeFragment leaf1 : streamAPIStatements1) {
						boolean matchingLambda = nestedLambdaExpressionMatch(leaf1.getLambdas(), fragment1);
						if(leaves1.contains(leaf1) || matchingLambda) {
							streamAPICallStatement = leaf1;
							streamAPICalls = streamAPICalls(leaf1);
							break;
						}
					}
					if(streamAPICallStatement != null && streamAPICalls != null) {
						List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
						additionallyMatchedStatements1.add(streamAPICallStatement);
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						additionallyMatchedStatements2.add(composite);
						for(AbstractCall streamAPICall : streamAPICalls) {
							if(streamAPICall.getName().equals("filter")) {
								for(AbstractCodeFragment leaf2 : leaves2) {
									AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getName().equals("add")) {
										for(String argument : invocation.arguments()) {
											if(streamAPICall.arguments().get(0).startsWith(argument + mapper.LANG1.LAMBDA_ARROW)) {
												additionallyMatchedStatements2.add(leaf2);
												break;
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("removeIf")) {
								for(AbstractCodeFragment leaf2 : leaves2) {
									AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getExpression() != null) {
										if(invocation.getName().equals("next")) {
											for(VariableDeclaration variableDeclaration : leaf2.getVariableDeclarations()) {
												if(streamAPICall.arguments().get(0).startsWith(variableDeclaration.getVariableName() + mapper.LANG1.LAMBDA_ARROW)) {
													additionallyMatchedStatements2.add(leaf2);
													break;
												}
											}
										}
										else if(invocation.getName().equals("remove")) {
											additionallyMatchedStatements2.add(leaf2);
											for(ListIterator<CompositeStatementObject> it = innerNodes2.listIterator(); it.hasNext();) {
												CompositeStatementObject comp = it.next();
												if(comp.getVariableDeclaration(invocation.getExpression()) != null) {
													additionallyMatchedStatements2.add(comp);
													composite = comp;
													break;
												}
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("map")) {
								for(AbstractCodeFragment leaf2 : leaves2) {
									for(AbstractCall invocation2 : leaf2.getMethodInvocations()) {
										for(LambdaExpressionObject lambda : streamAPICallStatement.getLambdas()) {
											if(streamAPICall.getLocationInfo().subsumes(lambda.getLocationInfo())) {
												for(AbstractCall invocation1 : lambda.getAllOperationInvocations()) {
													if(invocation1.getName().equals(invocation2.getName())) {
														additionallyMatchedStatements2.add(leaf2);
													}
												}
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("stream")) {
								for(CompositeStatementObject comp2 : innerNodes2) {
									if(!additionallyMatchedStatements2.contains(comp2)) {
										for(AbstractExpression expression : comp2.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements2.add(comp2);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													additionallyMatchedStatements2.add(comp2);
													break;
												}
											}
										}
									}
								}
							}
							else if(isForEach(streamAPICall.getName())) {
								for(CompositeStatementObject comp2 : innerNodes2) {
									if(!additionallyMatchedStatements2.contains(comp2)) {
										for(AbstractExpression expression : comp2.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements2.add(comp2);
												break;
											}
										}
										if(comp2.isLoop()) {
											List<AbstractCodeFragment> compositeLeaves = comp2.getLeaves();
											for(AbstractCodeMapping m : mappings) {
												if(!m.equals(mapping)) {
													AbstractCodeFragment leaf2 = null;
													if(compositeLeaves.contains(m.getFragment2()) || (leaf2 = containLambdaExpression(compositeLeaves, m.getFragment2())) != null) {
														if(leaf2 != null && composite.getLocationInfo().subsumes(leaf2.getLocationInfo())) {
															additionallyMatchedStatements2.add(comp2);
															additionallyMatchedStatements2.add(leaf2);
															composite = comp2;
															break;
														}
														else if(composite.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
															additionallyMatchedStatements2.add(comp2);
															additionallyMatchedStatements2.add(m.getFragment2());
															composite = comp2;
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
						boolean matchedLoop = additionallyMatchedStatements2.stream().anyMatch(statement -> statement instanceof CompositeStatementObject comp && comp.isLoop());
						if(composite.isLoop() || matchedLoop) {
							CompositeReplacement replacement = new CompositeReplacement(streamAPICallStatement.getString(), composite.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
							Set<Replacement> replacements = new LinkedHashSet<>();
							replacements.add(replacement);
							List<LeafMapping> mappingSet = new ArrayList<>();
							for(AbstractCodeFragment f : additionallyMatchedStatements2) {
								if(f.getVariableDeclarations().size() == 0 || f instanceof CompositeStatementObject) {
									LeafMapping newMapping = mapper.createLeafMapping(streamAPICallStatement, f, new LinkedHashMap<String, String>(), false, false);
									newMapping.addReplacements(replacements);
									mappingSet.add(newMapping);
								}
							}
							for(VariableDeclaration lambdaParameter : lambdaParameters) {
								for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
									if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
										Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
										mapper.matchedVariables.add(pair);
									}
									else {
										for(Replacement r : mapping.getReplacements()) {
											if(r.getBefore().equals(lambdaParameter.getVariableName()) && r.getAfter().equals(compositeParameter.getVariableName())) {
												Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(lambdaParameter, compositeParameter);
												mapper.matchedVariables.add(pair);
												break;
											}
										}
									}
								}
							}
							int count = 0;
							if(mapper.getParentMapper() != null) {
								for(AbstractCodeFragment fragment : additionallyMatchedStatements1) {
									if(mapper.getParentMapper().alreadyMatched1(fragment)) {
										count++;
									}
								}
							}
							boolean allAdditionalMatchedStatementsInParentMapper = count == additionallyMatchedStatements1.size();
							if(!allAdditionalMatchedStatementsInParentMapper) {
								ReplacePipelineWithLoopRefactoring ref = new ReplacePipelineWithLoopRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, mapper.getContainer1(), mapper.getContainer2());
								mappingSet.get(0).addRefactoring(ref);
								for(AbstractCodeMapping m : mapper.getMappings()) {
									if(composite.getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment1().getLocationInfo())) {
										ref.addNestedStatementMapping(m);
									}
								}
							}
							for(LeafMapping newMapping : mappingSet) {
								mapper.addToMappings(newMapping, new TreeSet<>(mappingSet));
								leaves1.remove(newMapping.getFragment1());
							}
							innerNodeIterator2.remove();
						}
					}
				}
			}
		}
	}

	void processStreamAPIStatements(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			List<CompositeStatementObject> innerNodes1, Set<AbstractCodeFragment> streamAPIStatements2)
			throws RefactoringMinerTimedOutException {
		Map<VariableDeclarationContainer, List<AbstractCodeFragment>> map = new LinkedHashMap<VariableDeclarationContainer, List<AbstractCodeFragment>>();
		int mapSize = 0;
		streamAPICallsInExtractedMethods(mapper.getContainer2(), leaves2, streamAPIStatements2, map);
		while(mapSize < map.size()) {
			int i=0;
			int tmpMapSize = map.size();
			for(VariableDeclarationContainer key : new LinkedHashSet<>(map.keySet())) {
				if(i >= mapSize) {
					streamAPICallsInExtractedMethods(key, map.get(key), streamAPIStatements2, map);
				}
				i++;
			}
			mapSize = tmpMapSize;
		}
		List<AbstractCodeFragment> newLeaves2 = new ArrayList<AbstractCodeFragment>();
		for(VariableDeclarationContainer key : map.keySet()) {
			newLeaves2.addAll(map.get(key));
		}
		//match expressions in inner nodes from T1 with leaves from T2
		List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
		for(CompositeStatementObject composite : innerNodes1) {
			if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT1.add(expression);
				}
			}
		}
		int numberOfMappings = mapper.getMappings().size();
		mapper.processLeaves(expressionsT1, leaves2, new LinkedHashMap<String, String>(), false);
		boolean onlyNestedMappings = mapper.getMappings().size() == numberOfMappings;
		mapper.processLeaves(expressionsT1, newLeaves2, new LinkedHashMap<String, String>(), false);
		
		List<AbstractCodeMapping> mappings = new ArrayList<>(mapper.getMappings());
		if(numberOfMappings == mappings.size()) {
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator1.next();
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
				List<AbstractCodeFragment> compositeLeaves = composite.getLeaves();
				for(AbstractCodeMapping mapping : mappings) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					List<VariableDeclaration> declarations1 = fragment1.getVariableDeclarations();
					boolean matchingDeclaration = false;
					if(declarations1.size() > 0 && composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
						for(AbstractExpression expression1 : composite.getExpressions()) {
							if(expression1.getString().equals(declarations1.get(0).getVariableName())) {
								matchingDeclaration = true;
								break;
							}
						}
					}
					if(composite.isLoop() &&
							(compositeLeaves.contains(fragment1) || containLambdaExpression(compositeLeaves, fragment1) != null) || matchingDeclaration) {
						AbstractCodeFragment streamAPICallStatement = null;
						List<AbstractCall> streamAPICalls = null;
						for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
							if(leaves2.contains(leaf2)) {
								boolean matchingLambda = nestedLambdaExpressionMatch(leaf2.getLambdas(), fragment2);
								if(matchingLambda) {
									streamAPICallStatement = leaf2;
									streamAPICalls = streamAPICalls(leaf2);
									break;
								}
							}
							else {
								for(LambdaExpressionObject lambda : leaf2.getLambdas()) {
									if(lambda.getBody() != null) {
										for(AbstractCodeFragment leaf : lambda.getBody().getCompositeStatement().getLeaves()) {
											boolean matchingLambda = nestedLambdaExpressionMatch(leaf.getLambdas(), fragment2);
											if(matchingLambda) {
												streamAPICallStatement = leaf;
												streamAPICalls = streamAPICalls(leaf);
												break;
											}
										}
									}
								}
							}
							List<AbstractCall> tmpStreamAPICalls = streamAPICalls(leaf2);
							if(matchingDeclaration) {
								boolean matchFound = false;
								for(AbstractCall call : tmpStreamAPICalls) {
									if(call.arguments().size() > 0 && call.arguments().get(0).startsWith(composite.getExpressions().get(0).getString() + mapper.LANG2.LAMBDA_ARROW)) {
										matchFound = true;
										break;
									}
									else if(call.arguments().size() > 0 && call.arguments().get(0).contains(mapper.LANG2.LAMBDA_ARROW)) {
										String name = call.arguments().get(0).substring(0, call.arguments().get(0).indexOf(mapper.LANG2.LAMBDA_ARROW));
										if(composite.getExpressions().get(0).getString().toLowerCase().contains(name.toLowerCase())) {
											matchFound = true;
											break;
										}
									}
								}
								if(matchFound) {
									streamAPICallStatement = leaf2;
									streamAPICalls = streamAPICalls(leaf2);
									break;
								}
							}
						}
						if(streamAPICallStatement != null && streamAPICalls != null) {
							List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
							List<LeafMapping> leafMappings = new ArrayList<LeafMapping>();
							AbstractCall call1 = fragment1.invocationCoveringEntireFragment();
							AbstractCall call2 = fragment2.invocationCoveringEntireFragment();
							if(call1 != null && call2 != null) {
								LeafMapping leafMapping = new LeafMapping(call1, call2, mapper.getContainer1(), mapper.getContainer2());
								leafMappings.add(leafMapping);
							}
							else {
								call1 = fragment1.creationCoveringEntireFragment();
								call2 = fragment2.creationCoveringEntireFragment();
								if(call1 != null && call2 != null) {
									LeafMapping leafMapping = new LeafMapping(call1, call2, mapper.getContainer1(), mapper.getContainer2());
									leafMappings.add(leafMapping);
								}
								else if(fragment2 instanceof AbstractExpression) {
									List<LeafExpression> leafExpressions = fragment1.findExpression(fragment2.getString());
									for(LeafExpression leafExpression : leafExpressions) {
										LeafMapping leafMapping = new LeafMapping(leafExpression, fragment2, mapper.getContainer1(), mapper.getContainer2());
										leafMappings.add(leafMapping);
									}
								}
							}
							additionallyMatchedStatements1.add(fragment1);
							additionallyMatchedStatements2.add(streamAPICallStatement);
							for(AbstractCall streamAPICall : streamAPICalls) {
								if(isForEach(streamAPICall.getName())) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(expression, leafExpression, mapper.getContainer1(), mapper.getContainer2());
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements1.add(composite);
												break;
											}
										}
									}
									if(matchingDeclaration) {
										List<LambdaExpressionObject> lambdas = streamAPICallStatement.getLambdas();
										for(LambdaExpressionObject lambda : lambdas) {
											if(lambda.getBody() != null) {
												CompositeStatementObject composite2 = lambda.getBody().getCompositeStatement();
												mapper.processCompositeStatements(composite.getLeaves(), composite2.getLeaves(), composite.getInnerNodes(), composite2.getInnerNodes());
											}
										}
									}
								}
								else if(streamAPICall.getName().equals("map")) {
									for(AbstractCodeFragment leaf1 : leaves1) {
										for(AbstractCall invocation1 : leaf1.getMethodInvocations()) {
											for(LambdaExpressionObject lambda : streamAPICallStatement.getLambdas()) {
												if(streamAPICall.getLocationInfo().subsumes(lambda.getLocationInfo())) {
													for(AbstractCall invocation2 : lambda.getAllOperationInvocations()) {
														if(invocation1.getName().equals(invocation2.getName())) {
															additionallyMatchedStatements1.add(leaf1);
														}
													}
												}
											}
										}
									}
								}
								else if(streamAPICall.getName().equals("stream")) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(streamAPICall.getExpression());
												for(LeafExpression leafExpression : leafExpressions) {
													LeafMapping leafMapping = new LeafMapping(expression, leafExpression, mapper.getContainer1(), mapper.getContainer2());
													leafMappings.add(leafMapping);
												}
												additionallyMatchedStatements1.add(composite);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													List<LeafExpression> leafExpressions = streamAPICallStatement.findExpression(argument);
													for(LeafExpression leafExpression : leafExpressions) {
														LeafMapping leafMapping = new LeafMapping(expression, leafExpression, mapper.getContainer1(), mapper.getContainer2());
														leafMappings.add(leafMapping);
													}
													additionallyMatchedStatements1.add(composite);
													break;
												}
											}
										}
									}
								}
							}
							CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
							Set<Replacement> replacements = new LinkedHashSet<>();
							replacements.add(replacement);
							if(!additionallyMatchedStatements1.contains(composite)) {
								additionallyMatchedStatements1.add(composite);
							}
							List<LeafMapping> mappingSet = new ArrayList<>();
							for(AbstractCodeFragment f : additionallyMatchedStatements1) {
								if(f.getVariableDeclarations().size() == 0 || f instanceof CompositeStatementObject) {
									LeafMapping newMapping = mapper.createLeafMapping(f, streamAPICallStatement, new LinkedHashMap<String, String>(), false, false);
									newMapping.addReplacements(replacements);
									mappingSet.add(newMapping);
								}
							}
							for(VariableDeclaration lambdaParameter : lambdaParameters) {
								for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
									if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
										Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
										mapper.matchedVariables.add(pair);
									}
									else {
										for(Replacement r : mapping.getReplacements()) {
											if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
												Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
												mapper.matchedVariables.add(pair);
												break;
											}
										}
									}
								}
							}
							ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, mapper.getContainer1(), mapper.getContainer2());
							for(LeafMapping leafMapping : leafMappings) {
								ref.addSubExpressionMapping(leafMapping);
							}
							for(AbstractCodeMapping m : mapper.getMappings()) {
								if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
									ref.addNestedStatementMapping(m);
								}
							}
							mappingSet.get(0).addRefactoring(ref);
							for(LeafMapping newMapping : mappingSet) {
								mapper.addToMappings(newMapping, new TreeSet<>(mappingSet));
								leaves2.remove(newMapping.getFragment2());
							}
						}
					}
				}
				if(additionallyMatchedStatements1.contains(composite)) {
					innerNodeIterator1.remove();
				}
			}
		}
		for(int i = numberOfMappings; i < mappings.size(); i++) {
			AbstractCodeMapping mapping = mappings.get(i);
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator1.next();
				if(composite.isLoop() &&
						composite.getLocationInfo().subsumes(fragment1.getLocationInfo()) &&
						onlyNestedMappings) {
					AbstractCodeFragment streamAPICallStatement = null;
					List<AbstractCall> streamAPICalls = null;
					for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
						if(leaves2.contains(leaf2)) {
							streamAPICallStatement = leaf2;
							streamAPICalls = streamAPICalls(leaf2);
							break;
						}
					}
					if(streamAPICallStatement != null && streamAPICalls != null) {
						List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
						additionallyMatchedStatements1.add(composite);
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						additionallyMatchedStatements2.add(streamAPICallStatement);
						for(AbstractCall streamAPICall : streamAPICalls) {
							if(isForEach(streamAPICall.getName())) {
								CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
								Set<Replacement> replacements = new LinkedHashSet<>();
								replacements.add(replacement);
								LeafMapping newMapping = mapper.createLeafMapping(composite, streamAPICallStatement, new LinkedHashMap<String, String>(), false, false);
								newMapping.addReplacements(replacements);
								TreeSet<LeafMapping> mappingSet = new TreeSet<>();
								mappingSet.add(newMapping);
								for(VariableDeclaration lambdaParameter : lambdaParameters) {
									for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
										if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
											Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
											mapper.matchedVariables.add(pair);
										}
										else {
											for(Replacement r : mapping.getReplacements()) {
												if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
													Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
													mapper.matchedVariables.add(pair);
													break;
												}
											}
										}
									}
								}
								ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, mapper.getContainer1(), mapper.getContainer2());
								newMapping.addRefactoring(ref);
								for(AbstractCodeMapping m : mapper.getMappings()) {
									if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
										ref.addNestedStatementMapping(m);
									}
								}
								mapper.addToMappings(newMapping, mappingSet);
								leaves2.remove(newMapping.getFragment2());
								innerNodeIterator1.remove();
							}
						}
					}
				}
				else if(composite.getExpressions().contains(fragment1)) {
					AbstractCodeFragment streamAPICallStatement = null;
					List<AbstractCall> streamAPICalls = null;
					for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
						if(leaves2.contains(leaf2)) {
							boolean matchingLambda = nestedLambdaExpressionMatch(leaf2.getLambdas(), fragment2);
							if(matchingLambda) {
								streamAPICallStatement = leaf2;
								streamAPICalls = streamAPICalls(leaf2);
								break;
							}
						}
						else if(fragment2.equals(leaf2)) {
							streamAPICallStatement = leaf2;
							streamAPICalls = streamAPICalls(leaf2);
							break;
						}
					}
					if(streamAPICallStatement != null && streamAPICalls != null) {
						List<VariableDeclaration> lambdaParameters = nestedLambdaParameters(streamAPICallStatement.getLambdas());
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
						additionallyMatchedStatements1.add(composite);
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						additionallyMatchedStatements2.add(streamAPICallStatement);
						for(AbstractCall streamAPICall : streamAPICalls) {
							if(streamAPICall.getName().equals("filter")) {
								for(AbstractCodeFragment leaf1 : leaves1) {
									AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getName().equals("add")) {
										for(String argument : invocation.arguments()) {
											if(streamAPICall.arguments().get(0).startsWith(argument + mapper.LANG2.LAMBDA_ARROW)) {
												additionallyMatchedStatements1.add(leaf1);
												break;
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("removeIf")) {
								for(AbstractCodeFragment leaf1 : leaves1) {
									AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getExpression() != null) {
										if(invocation.getName().equals("next")) {
											for(VariableDeclaration variableDeclaration : leaf1.getVariableDeclarations()) {
												if(streamAPICall.arguments().get(0).startsWith(variableDeclaration.getVariableName() + mapper.LANG2.LAMBDA_ARROW)) {
													additionallyMatchedStatements1.add(leaf1);
													break;
												}
											}
										}
										else if(invocation.getName().equals("remove")) {
											additionallyMatchedStatements1.add(leaf1);
											for(ListIterator<CompositeStatementObject> it = innerNodes1.listIterator(); it.hasNext();) {
												CompositeStatementObject comp = it.next();
												if(comp.getVariableDeclaration(invocation.getExpression()) != null) {
													additionallyMatchedStatements1.add(comp);
													composite = comp;
													break;
												}
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("stream")) {
								for(CompositeStatementObject comp1 : innerNodes1) {
									if(!additionallyMatchedStatements1.contains(comp1)) {
										for(AbstractExpression expression : comp1.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(comp1);
												break;
											}
											for(String argument : streamAPICall.arguments()) {
												if(expression.getString().equals(argument)) {
													additionallyMatchedStatements1.add(comp1);
													break;
												}
											}
										}
									}
								}
							}
							else if(isForEach(streamAPICall.getName())) {
								for(CompositeStatementObject comp1 : innerNodes1) {
									if(!additionallyMatchedStatements1.contains(comp1)) {
										for(AbstractExpression expression : comp1.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(comp1);
												break;
											}
										}
										if(comp1.isLoop()) {
											List<AbstractCodeFragment> compositeLeaves = comp1.getLeaves();
											for(AbstractCodeMapping m : mappings) {
												if(!m.equals(mapping)) {
													AbstractCodeFragment leaf1 = null;
													if(compositeLeaves.contains(m.getFragment1()) || (leaf1 = containLambdaExpression(compositeLeaves, m.getFragment1())) != null) {
														if(leaf1 != null && composite.getLocationInfo().subsumes(leaf1.getLocationInfo())) {
															additionallyMatchedStatements1.add(comp1);
															additionallyMatchedStatements1.add(leaf1);
															composite = comp1;
															break;
														}
														else if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo())) {
															additionallyMatchedStatements1.add(comp1);
															additionallyMatchedStatements1.add(m.getFragment1());
															composite = comp1;
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
						CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
						Set<Replacement> replacements = new LinkedHashSet<>();
						replacements.add(replacement);
						for(VariableDeclaration lambdaParameter : lambdaParameters) {
							for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
								if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
									Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
									mapper.matchedVariables.add(pair);
								}
								else {
									for(Replacement r : mapping.getReplacements()) {
										if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
											Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
											mapper.matchedVariables.add(pair);
											break;
										}
									}
								}
							}
						}
						AbstractCodeFragment loop = null;
						for(AbstractCodeFragment fragment : additionallyMatchedStatements1) {
							if(fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
									fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
									fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
									fragment.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
								loop = fragment;
								break;
							}
						}
						LeafMapping newMapping = mapper.createLeafMapping(loop != null ? loop : composite, streamAPICallStatement, new LinkedHashMap<String, String>(), false, false);
						newMapping.addReplacements(replacements);
						TreeSet<LeafMapping> mappingSet = new TreeSet<>();
						mappingSet.add(newMapping);
						LeafMapping newMapping2 = null;
						if(loop != null) {
							ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, mapper.getContainer1(), mapper.getContainer2());
							newMapping.addRefactoring(ref);
							for(AbstractCodeMapping m : mapper.getMappings()) {
								if(loop.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPICallStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
									ref.addNestedStatementMapping(m);
								}
							}
							if(!loop.equals(composite) && composite.isLoop()) {
								for(AbstractCodeFragment streamAPIStatement : streamAPIStatements2) {
									if(!streamAPIStatement.equals(streamAPICallStatement)) {
										if(composite.getExpressions().size() > 0 && streamAPIStatement.getString().startsWith(composite.getExpressions().get(0).getString())) {
											List<VariableDeclaration> lambdaParameters2 = nestedLambdaParameters(streamAPIStatement.getLambdas());
											for(VariableDeclaration lambdaParameter : lambdaParameters2) {
												for(VariableDeclaration compositeParameter : composite.getVariableDeclarations()) {
													if(lambdaParameter.getVariableName().equals(compositeParameter.getVariableName())) {
														Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
														mapper.matchedVariables.add(pair);
													}
													else {
														for(Replacement r : mapping.getReplacements()) {
															if(r.getBefore().equals(compositeParameter.getVariableName()) && r.getAfter().equals(lambdaParameter.getVariableName())) {
																Pair<VariableDeclaration, VariableDeclaration> pair = Pair.of(compositeParameter, lambdaParameter);
																mapper.matchedVariables.add(pair);
																break;
															}
														}
													}
												}
											}
											newMapping2 = mapper.createLeafMapping(composite, streamAPIStatement, new LinkedHashMap<String, String>(), false, false);
											Set<AbstractCodeFragment> a1 = new LinkedHashSet<>(additionallyMatchedStatements1);
											a1.remove(loop);
											a1.add(composite);
											Set<AbstractCodeFragment> a2 = new LinkedHashSet<>(additionallyMatchedStatements2);
											a2.remove(streamAPICallStatement);
											a2.add(streamAPIStatement);
											ReplaceLoopWithPipelineRefactoring ref2 = new ReplaceLoopWithPipelineRefactoring(a1, a2, mapper.getContainer1(), mapper.getContainer2());
											newMapping2.addRefactoring(ref2);
											for(AbstractCodeMapping m : mapper.getMappings()) {
												if(composite.getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) && streamAPIStatement.getLocationInfo().subsumes(m.getFragment2().getLocationInfo())) {
													ref2.addNestedStatementMapping(m);
												}
											}
										}
									}
								}
							}
						}
						mapper.addToMappings(newMapping, mappingSet);
						leaves2.remove(newMapping.getFragment2());
						innerNodeIterator1.remove();
						if(newMapping2 != null) {
							mappingSet.add(newMapping2);
							mapper.addToMappings(newMapping2, mappingSet);
							leaves2.remove(newMapping2.getFragment2());
						}
					}
				}
			}
		}
	}

	private void streamAPICallsInExtractedMethods(VariableDeclarationContainer callerOperation, List<AbstractCodeFragment> leaves2, Set<AbstractCodeFragment> streamAPIStatements2, Map<VariableDeclarationContainer, List<AbstractCodeFragment>> map) {
		if(mapper.getClassDiff() != null) {
			for(AbstractCodeFragment leaf2 : leaves2) {
				List<AbstractCall> calls = leaf2.getMethodInvocations();
				for(AbstractCall call : calls) {
					UMLOperation addedOperation = mapper.getClassDiff().matchesOperation(call, mapper.getClassDiff().getAddedOperations(), callerOperation);
					if(addedOperation != null && !map.keySet().contains(addedOperation)) {
						List<AbstractCodeFragment> newLeaves2 = new ArrayList<AbstractCodeFragment>();
						if(!addedOperation.hasEmptyBody() && addedOperation.getBody() != null) {
							Set<AbstractCodeFragment> newStreamAPIStatements2 = statementsWithStreamAPICalls(addedOperation.getBody().getCompositeStatement().getLeaves(), mapper.LANG2);
							for(AbstractCodeFragment streamAPICall : newStreamAPIStatements2) {
								if(streamAPICall.getLambdas().size() > 0) {
									streamAPIStatements2.add(streamAPICall);
									mapper.expandAnonymousAndLambdas(streamAPICall, newLeaves2, new ArrayList<CompositeStatementObject>(), new LinkedHashSet<>(), new LinkedHashSet<>(), mapper.getContainer2().getAnonymousClassList(), mapper.codeFragmentOperationMap2, mapper.getContainer2(), false);
								}
							}
						}
						map.put(addedOperation, newLeaves2);
					}
				}
			}
		}
	}
}
