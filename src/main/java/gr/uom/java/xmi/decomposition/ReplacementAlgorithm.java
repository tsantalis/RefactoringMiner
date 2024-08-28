package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.Constants.JAVA;
import static gr.uom.java.xmi.decomposition.OperationInvocation.PRIMITIVE_WRAPPER_CLASS_MAP;
import static gr.uom.java.xmi.decomposition.StringBasedHeuristics.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper.ReplacementInfo;
import gr.uom.java.xmi.decomposition.replacement.ClassInstanceCreationWithMethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.InitializerReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationWithClassInstanceCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.ObjectCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.InvertConditionRefactoring;
import gr.uom.java.xmi.diff.ReplaceAnonymousWithLambdaRefactoring;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.TryWithResourcesRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;

public class ReplacementAlgorithm {
	private static final int MAXIMUM_NUMBER_OF_COMPARED_STRINGS = 100;

	private static String stringEndingWithDotVariable(Set<String> set, String variable) {
		for(String s : set) {
			if(s.endsWith("."+variable)) {
				String prefix = s.substring(0, s.indexOf("."+variable));
				if(set.contains(prefix)) {
					return s;
				}
			}
		}
		return null;
	}

	protected static Set<Replacement> findReplacementsWithExactMatching(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo, boolean equalNumberOfAssertions, UMLOperationBodyMapper operationBodyMapper) throws RefactoringMinerTimedOutException {
		VariableDeclarationContainer container1 = operationBodyMapper.getContainer1();
		VariableDeclarationContainer container2 = operationBodyMapper.getContainer2();
		UMLOperationBodyMapper parentMapper = operationBodyMapper.getParentMapper();
		Set<AbstractCodeMapping> mappings = operationBodyMapper.getMappings();
		UMLAbstractClassDiff classDiff = operationBodyMapper.getClassDiff();
		UMLModelDiff modelDiff = operationBodyMapper.getModelDiff();
		Optional<UMLOperationDiff> operationSignatureDiff = operationBodyMapper.getOperationSignatureDiff();
		
		List<VariableDeclaration> variableDeclarations1 = new ArrayList<VariableDeclaration>(statement1.getVariableDeclarations());
		List<VariableDeclaration> variableDeclarations2 = new ArrayList<VariableDeclaration>(statement2.getVariableDeclarations());
		VariableDeclaration variableDeclarationWithArrayInitializer1 = declarationWithArrayInitializer(variableDeclarations1);
		VariableDeclaration variableDeclarationWithArrayInitializer2 = declarationWithArrayInitializer(variableDeclarations2);
		if(parentMapper != null && variableDeclarations1.size() > 0) {
			for(Refactoring r : parentMapper.getRefactoringsAfterPostProcessing()) {
				if(r instanceof InlineVariableRefactoring) {
					InlineVariableRefactoring inline = (InlineVariableRefactoring)r;
					if(!inline.isInsideExtractedOrInlinedMethod() && inline.getVariableDeclaration().equals(variableDeclarations1.get(0))) {
						return null;
					}
				}
			}
		}
		AbstractCall invocationCoveringTheEntireStatement1 = statement1.invocationCoveringEntireFragment();
		AbstractCall invocationCoveringTheEntireStatement2 = statement2.invocationCoveringEntireFragment();
		AbstractCall assignmentInvocationCoveringTheEntireStatement1 = invocationCoveringTheEntireStatement1 == null ? statement1.assignmentInvocationCoveringEntireStatement() : invocationCoveringTheEntireStatement1;
		AbstractCall assignmentInvocationCoveringTheEntireStatement2 = invocationCoveringTheEntireStatement2 == null ? statement2.assignmentInvocationCoveringEntireStatement() : invocationCoveringTheEntireStatement2;
		ObjectCreation creationCoveringTheEntireStatement1 = statement1.creationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement2 = statement2.creationCoveringEntireFragment();
		AbstractCall assignmentCreationCoveringTheEntireStatement1 = creationCoveringTheEntireStatement1 == null ? statement1.assignmentCreationCoveringEntireStatement() : creationCoveringTheEntireStatement1;
		AbstractCall assignmentCreationCoveringTheEntireStatement2 = creationCoveringTheEntireStatement2 == null ? statement2.assignmentCreationCoveringEntireStatement() : creationCoveringTheEntireStatement2;
		Map<String, List<AbstractCall>> methodInvocationMap1 = convertToMap(statement1.getMethodInvocations());
		Map<String, List<AbstractCall>> methodInvocationMap2 = convertToMap(statement2.getMethodInvocations());
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = statement1.getAnonymousClassDeclarations();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = statement2.getAnonymousClassDeclarations();
		List<LambdaExpressionObject> lambdas1 = statement1.getLambdas();
		List<LambdaExpressionObject> lambdas2 = statement2.getLambdas();
		if(assignmentInvocationCoveringTheEntireStatement1 instanceof OperationInvocation && assignmentInvocationCoveringTheEntireStatement2 instanceof OperationInvocation) {
			OperationInvocation inv1 = (OperationInvocation)assignmentInvocationCoveringTheEntireStatement1;
			OperationInvocation inv2 = (OperationInvocation)assignmentInvocationCoveringTheEntireStatement2;
			if(inv1.numberOfSubExpressions() == inv2.numberOfSubExpressions() && inv1.numberOfSubExpressions() > 3) {
				Set<String> callChainIntersection = inv1.callChainIntersection(inv2);
				double ratio = (double)callChainIntersection.size()/(double)inv1.numberOfSubExpressions();
				if(ratio >= 0.6) {
					processAnonymousAndLambdas(statement1, statement2, parameterToArgumentMap, replacementInfo,
							assignmentInvocationCoveringTheEntireStatement1 != null ? assignmentInvocationCoveringTheEntireStatement1 : assignmentCreationCoveringTheEntireStatement1,
							assignmentInvocationCoveringTheEntireStatement2 != null ? assignmentInvocationCoveringTheEntireStatement2 : assignmentCreationCoveringTheEntireStatement2,
							methodInvocationMap1, methodInvocationMap2,	anonymousClassDeclarations1, anonymousClassDeclarations2, lambdas1, lambdas2, operationBodyMapper);
					return replacementInfo.getReplacements();
				}
			}
		}
		Set<String> variables1 = convertToStringSet(statement1.getVariables());
		Set<String> variables2 = convertToStringSet(statement2.getVariables());
		Set<String> variableIntersection = new LinkedHashSet<String>(variables1);
		variableIntersection.retainAll(variables2);
		// ignore the variables in the intersection that also appear with "this." prefix in the sets of variables
		// ignore the variables in the intersection that also appear with "OtherVar." prefix in the sets of variables
		// ignore the variables in the intersection that are static fields
		// ignore the variables in the intersection that one of them is a variable declaration and the other is not
		// ignore the variables in the intersection that one of them is part of a method invocation, but the same method invocation does not appear on the other side
		Set<String> variablesToBeRemovedFromTheIntersection = new LinkedHashSet<String>();
		for(String variable : variableIntersection) {
			if(!variable.startsWith(JAVA.THIS_DOT) && !variableIntersection.contains(JAVA.THIS_DOT+variable) &&
					(variables1.contains(JAVA.THIS_DOT+variable) || variables2.contains(JAVA.THIS_DOT+variable))) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			String s1 = stringEndingWithDotVariable(variables1, variable);
			String s2 = stringEndingWithDotVariable(variables2, variable);
			if(!variable.contains(".") && stringEndingWithDotVariable(variableIntersection, variable) == null && (s1 != null || s2 != null)) {
				if(s1 != null && !variables2.contains(s1))
					variablesToBeRemovedFromTheIntersection.add(variable);
				else if(s2 != null && !variables1.contains(s2))
					variablesToBeRemovedFromTheIntersection.add(variable);
			}
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
					invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				if(!invocationCoveringTheEntireStatement1.arguments().contains(variable) &&
						invocationCoveringTheEntireStatement2.arguments().contains(variable)) {
					for(String argument : invocationCoveringTheEntireStatement1.arguments()) {
						String argumentNoWhiteSpace = argument.replaceAll("\\s","");
						if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
								!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) &&
								!nonMatchedStatementUsesVariableInArgument(replacementInfo.getStatements1(), variable, argument)) {
							variablesToBeRemovedFromTheIntersection.add(variable);
						}
					}
				}
				else if(invocationCoveringTheEntireStatement1.arguments().contains(variable) &&
						!invocationCoveringTheEntireStatement2.arguments().contains(variable)) {
					for(String argument : invocationCoveringTheEntireStatement2.arguments()) {
						String argumentNoWhiteSpace = argument.replaceAll("\\s","");
						if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
								!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) &&
								!nonMatchedStatementUsesVariableInArgument(replacementInfo.getStatements2(), variable, argument)) {
							variablesToBeRemovedFromTheIntersection.add(variable);
						}
					}
				}
			}
			if(variable.toUpperCase().equals(variable) && !ReplacementUtil.sameCharsBeforeAfter(statement1.getString(), statement2.getString(), variable)) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			boolean foundInDeclaration1 = false;
			boolean foundInInitializer1 = false;
			for(VariableDeclaration declaration : variableDeclarations1) {
				if(declaration.getVariableName().equals(variable)) {
					foundInDeclaration1 = true;
					AbstractExpression initializer = declaration.getInitializer();
					if(initializer != null && initializer.getString().endsWith("." + declaration.getVariableName())) {
						variablesToBeRemovedFromTheIntersection.add(variable);
					}
					break;
				}
				AbstractExpression initializer = declaration.getInitializer();
				if(initializer != null && initializer.getString().endsWith("." + variable)) {
					foundInInitializer1 = true;
				}
			}
			boolean foundInDeclaration2 = false;
			boolean foundInInitializer2 = false;
			for(VariableDeclaration declaration : variableDeclarations2) {
				if(declaration.getVariableName().equals(variable)) {
					foundInDeclaration2 = true;
					AbstractExpression initializer = declaration.getInitializer();
					if(initializer != null && initializer.getString().endsWith("." + declaration.getVariableName())) {
						variablesToBeRemovedFromTheIntersection.add(variable);
					}
					break;
				}
				AbstractExpression initializer = declaration.getInitializer();
				if(initializer != null && initializer.getString().endsWith("." + variable)) {
					foundInInitializer2 = true;
				}
			}
			if(foundInDeclaration1 != foundInDeclaration2 || foundInInitializer1 != foundInInitializer2) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			else if(!variable.contains(".")) {
				boolean foundInInvocation1 = false;
				for(String key : methodInvocationMap1.keySet()) {
					if(key.startsWith(variable + ".")) {
						foundInInvocation1 = true;
						break;
					}
				}
				boolean foundInInvocation2 = false;
				for(String key : methodInvocationMap2.keySet()) {
					if(key.startsWith(variable + ".")) {
						foundInInvocation2 = true;
						break;
					}
				}
				boolean sameCoverageInvocations = invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
						invocationCoveringTheEntireStatement1.getCoverage().equals(invocationCoveringTheEntireStatement2.getCoverage());
				boolean sameCoverageCreations = creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
						creationCoveringTheEntireStatement1.getCoverage().equals(creationCoveringTheEntireStatement2.getCoverage());
				if((sameCoverageInvocations || sameCoverageCreations) && foundInInvocation1 != foundInInvocation2) {
					variablesToBeRemovedFromTheIntersection.add(variable);
				}
				else if(methodInvocationMap1.isEmpty() && !methodInvocationMap2.isEmpty() && foundInInvocation1 != foundInInvocation2) {
					variablesToBeRemovedFromTheIntersection.add(variable);
				}
				else if(!methodInvocationMap1.isEmpty() && methodInvocationMap2.isEmpty() && foundInInvocation1 != foundInInvocation2) {
					variablesToBeRemovedFromTheIntersection.add(variable);
				}
			}
		}
		variableIntersection.removeAll(variablesToBeRemovedFromTheIntersection);
		// remove common variables from the two sets
		variables1.removeAll(variableIntersection);
		variables2.removeAll(variableIntersection);
		
		// replace variables with the corresponding arguments
		replaceVariablesWithArguments(variables1, parameterToArgumentMap);
		replaceVariablesWithArguments(variables2, parameterToArgumentMap);
		
		Set<String> methodInvocations1 = new LinkedHashSet<String>(methodInvocationMap1.keySet());
		Set<String> methodInvocations2 = new LinkedHashSet<String>(methodInvocationMap2.keySet());
		
		Map<String, List<AbstractCall>> creationMap1 = convertToMap(statement1.getCreations());
		Map<String, List<AbstractCall>> creationMap2 = convertToMap(statement2.getCreations());
		Set<String> creations1 = new LinkedHashSet<String>(creationMap1.keySet());
		Set<String> creations2 = new LinkedHashSet<String>(creationMap2.keySet());
		
		Set<String> arguments1 = convertToStringSet(statement1.getArguments());
		Set<String> arguments2 = convertToStringSet(statement2.getArguments());
		removeCommonElements(arguments1, arguments2);
		
		if(!argumentsWithIdenticalMethodCalls(arguments1, arguments2, variables1, variables2)) {
			boolean argsAreMethodCalls = false;
			if(arguments1.size() == arguments2.size()) {
				Iterator<String> it1 = arguments1.iterator();
				Iterator<String> it2 = arguments2.iterator();
				for(int i=0; i<arguments1.size(); i++) {
					String arg1 = it1.next();
					String arg2 = it2.next();
					if(methodInvocationMap1.containsKey(arg1) && methodInvocationMap2.containsKey(arg2)) {
						List<? extends AbstractCall> calls1 = methodInvocationMap1.get(arg1);
						List<? extends AbstractCall> calls2 = methodInvocationMap2.get(arg2);
						if(calls1.get(0).getName().equals(calls2.get(0).getName())) {
							argsAreMethodCalls = true;
							break;
						}
					}
					else {
						String matchingKey1 = null;
						for(String key1 : methodInvocationMap1.keySet()) {
							if(arg1.contains(key1)) {
								matchingKey1 = key1;
								break;
							}
						}
						String matchingKey2 = null;
						for(String key2 : methodInvocationMap2.keySet()) {
							if(arg2.contains(key2)) {
								matchingKey2 = key2;
								break;
							}
						}
						if(matchingKey1 != null && matchingKey2 != null) {
							List<? extends AbstractCall> calls1 = methodInvocationMap1.get(matchingKey1);
							List<? extends AbstractCall> calls2 = methodInvocationMap2.get(matchingKey2);
							if(calls1.get(0).getName().equals(calls2.get(0).getName())) {
								argsAreMethodCalls = true;
								break;
							}
						}
					}
				}
			}
			for(String arg1 : arguments1) {
				if(methodInvocationMap1.containsKey(arg1) && (arg1.contains(JAVA.METHOD_REFERENCE) || arg1.contains(JAVA.LAMBDA_ARROW))) {
					argsAreMethodCalls = true;
				}
			}
			if(!argsAreMethodCalls) {
				findReplacements(arguments1, variables2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_VARIABLE, container1, container2, classDiff);
			}
		}
		
		Map<String, String> map = new LinkedHashMap<String, String>();
		Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<Replacement>();
		Set<Replacement> replacementsToBeAdded = new LinkedHashSet<Replacement>();
		for(Replacement r : replacementInfo.getReplacements()) {
			map.put(r.getBefore(), r.getAfter());
			if(methodInvocationMap1.containsKey(r.getBefore())) {
				Replacement replacement = new VariableReplacementWithMethodInvocation(r.getBefore(), r.getAfter(), methodInvocationMap1.get(r.getBefore()).get(0), Direction.INVOCATION_TO_VARIABLE);
				replacementsToBeAdded.add(replacement);
				replacementsToBeRemoved.add(r);
			}
		}
		replacementInfo.getReplacements().removeAll(replacementsToBeRemoved);
		replacementInfo.getReplacements().addAll(replacementsToBeAdded);
		
		// replace variables with the corresponding arguments in method invocations
		replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, parameterToArgumentMap);
		replaceVariablesWithArguments(methodInvocationMap2, methodInvocations2, parameterToArgumentMap);
		
		replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, map);
		
		//remove methodInvocation covering the entire statement
		boolean variableReturn2 = statement2.getVariables().size() > 0 && statement2.getString().equals(JAVA.RETURN_SPACE + statement2.getVariables().get(0).getString() + JAVA.STATEMENT_TERMINATION);
		boolean returnMatch2 = false;
		if(variableReturn2 && invocationCoveringTheEntireStatement1 != null) {
			for(AbstractCodeFragment f2 : replacementInfo.getStatements2()) {
				if(f2.getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration2 = f2.getVariableDeclarations().get(0);
					if(declaration2.getVariableName().equals(statement2.getVariables().get(0).getString()) &&
							declaration2.getInitializer() != null) {
						if(declaration2.getInitializer().getString().equals(invocationCoveringTheEntireStatement1.actualString())) {
							returnMatch2 = true;
							break;
						}
					}
				}
				AbstractCall assignmentInvocation2 = f2.assignmentInvocationCoveringEntireStatement();
				if(assignmentInvocation2 != null) {
					if(assignmentInvocation2.actualString().equals(invocationCoveringTheEntireStatement1.actualString())) {
						returnMatch2 = true;
						break;
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && !returnMatch2) {
			for(String methodInvocation1 : methodInvocationMap1.keySet()) {
				for(AbstractCall call : methodInvocationMap1.get(methodInvocation1)) {
					if(invocationCoveringTheEntireStatement1.getLocationInfo().equals(call.getLocationInfo())) {
						methodInvocations1.remove(methodInvocation1);
					}
				}
			}
		}
		boolean variableReturn1 = statement1.getVariables().size() > 0 && statement1.getString().equals(JAVA.RETURN_SPACE + statement1.getVariables().get(0).getString() + JAVA.STATEMENT_TERMINATION);
		boolean returnMatch1 = false;
		if(variableReturn1 && invocationCoveringTheEntireStatement2 != null) {
			for(AbstractCodeFragment f1 : replacementInfo.getStatements1()) {
				if(f1.getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration1 = f1.getVariableDeclarations().get(0);
					if(declaration1.getVariableName().equals(statement1.getVariables().get(0).getString()) &&
							declaration1.getInitializer() != null) {
						if(declaration1.getInitializer().getString().equals(invocationCoveringTheEntireStatement2.actualString())) {
							returnMatch1 = true;
							break;
						}
					}
				}
				AbstractCall assignmentInvocation1 = f1.assignmentInvocationCoveringEntireStatement();
				if(assignmentInvocation1 != null) {
					if(assignmentInvocation1.actualString().equals(invocationCoveringTheEntireStatement2.actualString())) {
						returnMatch1 = true;
						break;
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement2 != null && !returnMatch1) {
			for(String methodInvocation2 : methodInvocationMap2.keySet()) {
				for(AbstractCall call : methodInvocationMap2.get(methodInvocation2)) {
					if(invocationCoveringTheEntireStatement2.getLocationInfo().equals(call.getLocationInfo())) {
						methodInvocations2.remove(methodInvocation2);
					}
				}
			}
		}
		Set<String> methodInvocationIntersection = new LinkedHashSet<String>(methodInvocations1);
		methodInvocationIntersection.retainAll(methodInvocations2);
		Set<String> methodInvocationsToBeRemovedFromTheIntersection = new LinkedHashSet<String>();
		for(String methodInvocation : methodInvocationIntersection) {
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
					invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				if(!invocationCoveringTheEntireStatement1.arguments().contains(methodInvocation) &&
						invocationCoveringTheEntireStatement2.arguments().contains(methodInvocation)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
				else if(invocationCoveringTheEntireStatement1.arguments().contains(methodInvocation) &&
						!invocationCoveringTheEntireStatement2.arguments().contains(methodInvocation)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
			}
			for(String key : methodInvocationMap1.keySet()) {
				if(key.startsWith(methodInvocation + ".") && !methodInvocationMap2.containsKey(key)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
			}
			for(String key : methodInvocationMap2.keySet()) {
				if(key.startsWith(methodInvocation + ".") && !methodInvocationMap1.containsKey(key)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
			}
		}
		methodInvocationIntersection.removeAll(methodInvocationsToBeRemovedFromTheIntersection);
		// remove common methodInvocations from the two sets
		methodInvocations1.removeAll(methodInvocationIntersection);
		methodInvocations2.removeAll(methodInvocationIntersection);
		
		Set<String> variablesAndMethodInvocations1 = new LinkedHashSet<String>();
		//variablesAndMethodInvocations1.addAll(methodInvocations1);
		//variablesAndMethodInvocations1.addAll(variables1);
		
		Set<String> variablesAndMethodInvocations2 = new LinkedHashSet<String>();
		variablesAndMethodInvocations2.addAll(methodInvocations2);
		variablesAndMethodInvocations2.addAll(variables2);
		
		Set<String> types1 = new LinkedHashSet<String>(statement1.getTypes());
		for(AnonymousClassDeclarationObject anonymous1 : statement1.getAnonymousClassDeclarations()) {
			types1.addAll(anonymous1.getTypes());
		}
		Set<String> types2 = new LinkedHashSet<String>(statement2.getTypes());
		for(AnonymousClassDeclarationObject anonymous2 : statement2.getAnonymousClassDeclarations()) {
			types2.addAll(anonymous2.getTypes());
		}
		removeCommonTypes(types1, types2, statement1.getTypes(), statement2.getTypes());
		
		// replace variables with the corresponding arguments in object creations
		replaceVariablesWithArguments(creationMap1, creations1, parameterToArgumentMap);
		replaceVariablesWithArguments(creationMap2, creations2, parameterToArgumentMap);
		
		replaceVariablesWithArguments(creationMap1, creations1, map);
		
		//remove objectCreation covering the entire statement
		if(variableReturn2 && creationCoveringTheEntireStatement1 != null) {
			for(AbstractCodeFragment f2 : replacementInfo.getStatements2()) {
				if(f2.getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration2 = f2.getVariableDeclarations().get(0);
					if(declaration2.getVariableName().equals(statement2.getVariables().get(0).getString()) &&
							declaration2.getInitializer() != null) {
						if(declaration2.getInitializer().getString().equals(creationCoveringTheEntireStatement1.actualString())) {
							returnMatch2 = true;
							break;
						}
					}
				}
				AbstractCall assignmentCreation2 = f2.assignmentCreationCoveringEntireStatement();
				if(assignmentCreation2 != null) {
					if(assignmentCreation2.actualString().equals(creationCoveringTheEntireStatement1.actualString())) {
						returnMatch2 = true;
						break;
					}
				}
			}
		}
		if(!returnMatch2) {
			for(String objectCreation1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(objectCreation1)) {
					if(creationCoveringTheEntireStatement1 != null && 
							creationCoveringTheEntireStatement1.getLocationInfo().equals(creation1.getLocationInfo())) {
						creations1.remove(objectCreation1);
					}
					if(((ObjectCreation)creation1).getAnonymousClassDeclaration() != null) {
						creations1.remove(objectCreation1);
					}
				}
			}
		}
		if(variableReturn1 && creationCoveringTheEntireStatement2 != null) {
			for(AbstractCodeFragment f1 : replacementInfo.getStatements1()) {
				if(f1.getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration1 = f1.getVariableDeclarations().get(0);
					if(declaration1.getVariableName().equals(statement1.getVariables().get(0).getString()) &&
							declaration1.getInitializer() != null) {
						if(declaration1.getInitializer().getString().equals(creationCoveringTheEntireStatement2.actualString())) {
							returnMatch1 = true;
							break;
						}
					}
				}
				AbstractCall assignmentCreation1 = f1.assignmentCreationCoveringEntireStatement();
				if(assignmentCreation1 != null) {
					if(assignmentCreation1.actualString().equals(creationCoveringTheEntireStatement2.actualString())) {
						returnMatch1 = true;
						break;
					}
				}
			}
		}
		if(!returnMatch1) {
			for(String objectCreation2 : creationMap2.keySet()) {
				for(AbstractCall creation2 : creationMap2.get(objectCreation2)) {
					if(creationCoveringTheEntireStatement2 != null &&
							creationCoveringTheEntireStatement2.getLocationInfo().equals(creation2.getLocationInfo())) {
						creations2.remove(objectCreation2);
					}
					if(((ObjectCreation)creation2).getAnonymousClassDeclaration() != null) {
						creations2.remove(objectCreation2);
					}
				}
			}
		}
		Set<String> creationIntersection = new LinkedHashSet<String>(creations1);
		creationIntersection.retainAll(creations2);
		// remove common creations from the two sets
		creations1.removeAll(creationIntersection);
		creations2.removeAll(creationIntersection);
		
		Set<String> stringLiterals1 = convertToStringSet(statement1.getStringLiterals());
		Set<String> stringLiterals2 = convertToStringSet(statement2.getStringLiterals());
		removeCommonElements(stringLiterals1, stringLiterals2);
		
		Set<String> charLiterals1 = convertToStringSet(statement1.getCharLiterals());
		Set<String> charLiterals2 = convertToStringSet(statement2.getCharLiterals());
		removeCommonElements(charLiterals1, charLiterals2);
		
		Set<String> typeLiterals1 = convertToStringSet(statement1.getTypeLiterals());
		Set<String> typeLiterals2 = convertToStringSet(statement2.getTypeLiterals());
		removeCommonElements(typeLiterals1, typeLiterals2);
		
		Set<String> numberLiterals1 = convertToStringSet(statement1.getNumberLiterals());
		Set<String> numberLiterals2 = convertToStringSet(statement2.getNumberLiterals());
		removeCommonElements(numberLiterals1, numberLiterals2);
		
		Set<String> booleanLiterals1 = convertToStringSet(statement1.getBooleanLiterals());
		Set<String> booleanLiterals2 = convertToStringSet(statement2.getBooleanLiterals());
		removeCommonElements(booleanLiterals1, booleanLiterals2);
		
		Set<String> infixOperators1 = new LinkedHashSet<String>(statement1.getInfixOperators());
		Set<String> infixOperators2 = new LinkedHashSet<String>(statement2.getInfixOperators());
		removeCommonElements(infixOperators1, infixOperators2);
		
		Set<String> arrayAccesses1 = convertToStringSet(statement1.getArrayAccesses());
		Set<String> arrayAccesses2 = convertToStringSet(statement2.getArrayAccesses());
		removeCommonElements(arrayAccesses1, arrayAccesses2);
		
		Set<String> prefixExpressions1 = convertToStringSet(statement1.getPrefixExpressions());
		Set<String> prefixExpressions2 = convertToStringSet(statement2.getPrefixExpressions());
		removeCommonElements(prefixExpressions1, prefixExpressions2);
		
		Set<String> parenthesizedExpressions1 = convertToStringSet(statement1.getParenthesizedExpressions());
		Set<String> parenthesizedExpressions2 = convertToStringSet(statement2.getParenthesizedExpressions());
		removeCommonElements(parenthesizedExpressions1, parenthesizedExpressions2);
		
		Set<String> castExpressions1 = convertToStringSet(statement1.getCastExpressions());
		Set<String> castExpressions2 = convertToStringSet(statement2.getCastExpressions());
		removeCommonElements(castExpressions1, castExpressions2);
		
		//perform type replacements
		findReplacements(types1, types2, replacementInfo, ReplacementType.TYPE, container1, container2, classDiff);
		
		if(statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType())) {
			Set<String> infixExpressions1 = convertToStringSet(statement1.getInfixExpressions());
			String infixExpressionCoveringTheEntireFragment = statement1.infixExpressionCoveringTheEntireFragment();
			if(infixExpressionCoveringTheEntireFragment != null) {
				boolean skip = false;
				if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) || statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT)) {
					if(infixExpressionCoveringTheEntireFragment.contains(" || ") || infixExpressionCoveringTheEntireFragment.contains(" && ")) {
						skip = true;
					}
				}
				if(!skip) {
					infixExpressions1.remove(infixExpressionCoveringTheEntireFragment);
				}
			}
			Set<String> infixExpressions2 = convertToStringSet(statement2.getInfixExpressions());
			Set<String> tmpVariables2 = new LinkedHashSet<>(variables2);
			Set<String> variablesToBeRemoved = new LinkedHashSet<String>();
			for(String infix : infixExpressions2) {
				for(String variable : tmpVariables2) {
					if(ReplacementUtil.contains(infix, variable) &&
							(infix.contains(variable + " > ") || infix.contains(variable + " < ") ||
									infix.contains(variable + " >= ") || infix.contains(variable + " <= ") ||
									infix.contains(variable + " != ") || infix.contains(variable + " == "))) {
						variablesToBeRemoved.add(variable);
					}
				}
			}
			tmpVariables2.removeAll(variablesToBeRemoved);
			infixExpressions2.remove(statement2.infixExpressionCoveringTheEntireFragment());
			removeCommonElements(infixExpressions1, infixExpressions2);
			
			if(infixExpressions1.size() != infixExpressions2.size()) {
				List<String> infixExpressions1AsList = new ArrayList<>(infixExpressions1);
				Collections.reverse(infixExpressions1AsList);
				Set<String> reverseInfixExpressions1 = new LinkedHashSet<String>(infixExpressions1AsList);
				findReplacements(reverseInfixExpressions1, tmpVariables2, replacementInfo, ReplacementType.INFIX_EXPRESSION, container1, container2, classDiff);
			}
		}
		//perform operator replacements
		findReplacements(infixOperators1, infixOperators2, replacementInfo, ReplacementType.INFIX_OPERATOR, container1, container2, classDiff);
		
		//apply existing replacements on method invocations
		for(String methodInvocation1 : methodInvocations1) {
			String temp = new String(methodInvocation1);
			for(Replacement replacement : replacementInfo.getReplacements()) {
				temp = ReplacementUtil.performReplacement(temp, replacement.getBefore(), replacement.getAfter());
			}
			if(!temp.equals(methodInvocation1)) {
				variablesAndMethodInvocations1.add(temp);
				methodInvocationMap1.put(temp, methodInvocationMap1.get(methodInvocation1));
			}
		}
		//add updated method invocation to the original list of invocations
		methodInvocations1.addAll(variablesAndMethodInvocations1);
		variablesAndMethodInvocations1.addAll(methodInvocations1);
		variablesAndMethodInvocations1.addAll(variables1);
		//perform creation replacements
		findReplacements(creations1, creations2, replacementInfo, ReplacementType.CLASS_INSTANCE_CREATION, container1, container2, classDiff);

		findReplacements(variables1, creations2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CLASS_INSTANCE_CREATION, container1, container2, classDiff);
		findReplacements(creations1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CLASS_INSTANCE_CREATION, container1, container2, classDiff);
		if(statement1.getString().startsWith(JAVA.THROW_SPACE) && statement2.getString().startsWith(JAVA.THROW_SPACE) && creationCoveringTheEntireStatement2 != null && creations2.isEmpty()) {
			findReplacements(variables1, Set.of(creationCoveringTheEntireStatement2.actualString()), replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CLASS_INSTANCE_CREATION, container1, container2, classDiff);
		}
		if(charLiterals1.size() > 0 && charLiterals2.isEmpty() && stringLiterals2.size() > 0) {
			findReplacements(charLiterals1, stringLiterals2, replacementInfo, ReplacementType.STRING_LITERAL_REPLACED_WITH_CHAR_LITERAL, container1, container2, classDiff);
		}
		if(charLiterals2.size() > 0 && charLiterals1.isEmpty() && stringLiterals1.size() > 0) {
			findReplacements(stringLiterals1, charLiterals2, replacementInfo, ReplacementType.STRING_LITERAL_REPLACED_WITH_CHAR_LITERAL, container1, container2, classDiff);
		}
		if(variablesAndMethodInvocations1.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS || variablesAndMethodInvocations2.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS) {
			return null;
		}
		if (replacementInfo.getRawDistance() > 0) {
			for(String s1 : variablesAndMethodInvocations1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				int minDistance = replacementInfo.getRawDistance();
				for(String s2 : variablesAndMethodInvocations2) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), minDistance);
					boolean multipleInstances = ReplacementUtil.countInstances(temp, s2) > 1;
					boolean typeContainsVariableName = false;
					if(variableDeclarations1.size() > 0 && !s1.equals(s2)) {
						VariableDeclaration variableDeclaration = variableDeclarations1.get(0);
						UMLType variableType = variableDeclaration.getType();
						if(variableType != null) {
							String typeTolowerCase = variableType.toString().toLowerCase();
							if(typeTolowerCase.contains(variableDeclaration.getVariableName().toLowerCase()) &&	typeTolowerCase.contains(s2.toLowerCase())) {
								typeContainsVariableName = true;
							}
							if(!typeContainsVariableName && statement1.getString().contains(s1 + JAVA.ASSIGNMENT) && statement2.getString().contains(s2 + JAVA.ASSIGNMENT)) {
								String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variableType.toString());
								String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(s2);
								int commonTokens = 0;
								for(String token1 : tokens1) {
									for(String token2 : tokens2) {
										if(token1.toLowerCase().equals(token2.toLowerCase()) || 
												token1.toLowerCase().startsWith(token2.toLowerCase()) ||
												token2.toLowerCase().startsWith(token1.toLowerCase())) {
											commonTokens++;
										}
									}
								}
								if(commonTokens > 1) {
									typeContainsVariableName = true;
								}
							}
						}
					}
					if(distanceRaw == -1 && (multipleInstances || typeContainsVariableName)) {
						distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					}
					boolean allowReplacementIncreasingDistance = (multipleInstances && Math.abs(s1.length() - s2.length()) == Math.abs(distanceRaw - minDistance) && !s1.equals(s2)) || typeContainsVariableName;
					if(distanceRaw >= 0 && (distanceRaw < replacementInfo.getRawDistance() || allowReplacementIncreasingDistance)) {
						minDistance = distanceRaw;
						Replacement replacement = null;
						if(variables1.contains(s1) && variables2.contains(s2) && variablesStartWithSameCase(s1, s2, replacementInfo)) {
							replacement = new Replacement(s1, s2, ReplacementType.VARIABLE_NAME);
							if(s1.startsWith("(") && s2.startsWith("(") && s1.contains(")") && s2.contains(")")) {
								String prefix1 = s1.substring(0, s1.indexOf(")")+1);
								String prefix2 = s2.substring(0, s2.indexOf(")")+1);
								if(prefix1.equals(prefix2)) {
									String suffix1 = s1.substring(prefix1.length(), s1.length());
									String suffix2 = s2.substring(prefix2.length(), s2.length());
									replacement = new Replacement(suffix1, suffix2, ReplacementType.VARIABLE_NAME);
								}
							}
							VariableDeclaration v1 = statement1.searchVariableDeclaration(s1);
							if(v1 == null && container1 != null) {
								for(VariableDeclaration declaration : container1.getParameterDeclarationList()) {
									if(declaration.getVariableName().equals(s1)) {
										v1 = declaration;
										break;
									}
								}
							}
							VariableDeclaration v2 = statement2.searchVariableDeclaration(s2);
							if(v2 == null && container2 != null) {
								for(VariableDeclaration declaration : container2.getParameterDeclarationList()) {
									if(declaration.getVariableName().equals(s2)) {
										v2 = declaration;
										break;
									}
								}
							}
							if((inconsistentVariableMappingCount(statement1, statement2, v1, v2, mappings) > 1 || mappingsForStatementsInScope(statement1, statement2, v1, v2, mappings) == 0) &&
									!existsVariableDeclarationForV2InitializedWithV1(v1, v2, replacementInfo) && !existsVariableDeclarationForV1InitializedWithV2(v1, v2, replacementInfo) &&
									!isExtractedVariable(v2, mappings) && !containsRightHandSideReplacementWithAppendChange(statement1, statement2, replacementInfo, replacement) &&
									container2 != null && container2.loopWithVariables(v1.getVariableName(), v2.getVariableName()) == null) {
								replacement = null;
							}
						}
						else if(variables1.contains(s1) && methodInvocations2.contains(s2)) {
							AbstractCall invokedOperationAfter = methodInvocationMap2.get(s2).get(0);
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationAfter, Direction.VARIABLE_TO_INVOCATION);
						}
						else if(methodInvocations1.contains(s1) && methodInvocations2.contains(s2)) {
							AbstractCall invokedOperationBefore = methodInvocationMap1.get(s1).get(0);
							AbstractCall invokedOperationAfter = methodInvocationMap2.get(s2).get(0);
							if(invokedOperationBefore.compatibleExpression(invokedOperationAfter)) {
								if(invokedOperationBefore.identicalExpression(invokedOperationAfter) && invokedOperationBefore.equalArguments(invokedOperationAfter)) {
									replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION_NAME);
								}
								else {
									replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION);
								}
							}
						}
						else if(methodInvocations1.contains(s1) && variables2.contains(s2)) {
							AbstractCall invokedOperationBefore = methodInvocationMap1.get(s1).get(0);
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationBefore, Direction.INVOCATION_TO_VARIABLE);
						}
						if(replacement != null) {
							double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
							replacementMap.put(distancenormalized, replacement);
						}
						if(distanceRaw == 0 && !replacementInfo.getReplacements().isEmpty()) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
							invocationCoveringTheEntireStatement1.methodNameContainsArgumentName() &&
							invocationCoveringTheEntireStatement2.methodNameContainsArgumentName() &&
							replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
						for(Replacement r : replacementMap.values()) {
							if(!replacement.equals(r) && r.getType().equals(ReplacementType.VARIABLE_NAME) &&
									invocationCoveringTheEntireStatement1.getName().toLowerCase().endsWith(r.getBefore().toLowerCase()) &&
									invocationCoveringTheEntireStatement2.getName().toLowerCase().endsWith(r.getAfter().toLowerCase())) {
								replacement = r;
								break;
							}
						}
					}
					replacementInfo.addReplacement(replacement);
					replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
					if(replacementMap.firstEntry().getKey() == 0) {
						break;
					}
				}
			}
		}
		
		if(replacementInfo.getReplacements().size() > 0 && replacementInfo.getReplacements(ReplacementType.VARIABLE_NAME).size() == 0) {
			boolean atLeastOneUpperCaseVariable1 = false;
			for(String variable1 : variables1) {
				if(Character.isUpperCase(variable1.charAt(0))) {
					boolean foundInReplacement = false;
					for(Replacement r : replacementInfo.getReplacements()) {
						if(r.getBefore().contains(variable1) && !r.getBefore().equals(variable1)) {
							foundInReplacement = true;
						}
					}
					if(!foundInReplacement) {
						atLeastOneUpperCaseVariable1 = true;
					}
				}
			}
			boolean atLeastOneUpperCaseVariable2 = false;
			for(String variable2 : variables2) {
				if(Character.isUpperCase(variable2.charAt(0))) {
					boolean foundInReplacement = false;
					for(Replacement r : replacementInfo.getReplacements()) {
						if(r.getAfter().contains(variable2) && !r.getAfter().equals(variable2)) {
							foundInReplacement = true;
						}
					}
					if(!foundInReplacement) {
						atLeastOneUpperCaseVariable2 = true;
					}
				}
			}
			if(atLeastOneUpperCaseVariable1 != atLeastOneUpperCaseVariable2) {
				findReplacements(variables1, variables2, replacementInfo, ReplacementType.VARIABLE_NAME, container1, container2, classDiff);
			}
		}
		findReplacements(parenthesizedExpressions1, parenthesizedExpressions2, replacementInfo, ReplacementType.PARENTHESIZED_EXPRESSION, container1, container2, classDiff);
		
		//perform literal replacements
		findReplacements(stringLiterals1, stringLiterals2, replacementInfo, ReplacementType.STRING_LITERAL, container1, container2, classDiff);
		findReplacements(charLiterals1, charLiterals2, replacementInfo, ReplacementType.CHAR_LITERAL, container1, container2, classDiff);
		findReplacements(numberLiterals1, numberLiterals2, replacementInfo, ReplacementType.NUMBER_LITERAL, container1, container2, classDiff);
		if(!statement1.containsInitializerOfVariableDeclaration(numberLiterals1) && !statement2.containsInitializerOfVariableDeclaration(variables2) &&
				(!statement1.getString().endsWith("=0;\n") || (statement1.getString().endsWith("=0;\n") && statement2.getString().endsWith(".length;\n")))) {
			findReplacements(numberLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NUMBER_LITERAL, container1, container2, classDiff);
		}
		findReplacements(variables1, arrayAccesses2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS, container1, container2, classDiff);
		findReplacements(arrayAccesses1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS, container1, container2, classDiff);
		
		findReplacements(methodInvocations1, arrayAccesses2, replacementInfo, ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION, container1, container2, classDiff);
		findReplacements(arrayAccesses1, methodInvocations2, replacementInfo, ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION, container1, container2, classDiff);
		
		findReplacements(variables1, prefixExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION, container1, container2, classDiff);
		findReplacements(prefixExpressions1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION, container1, container2, classDiff);
		if(prefixExpressions1.size() == 1 && prefixExpressions1.iterator().next().startsWith("!") && booleanLiterals1.isEmpty()) {
			findReplacements(prefixExpressions1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_PREFIX_EXPRESSION, container1, container2, classDiff);
		}
		if(prefixExpressions2.size() == 1 && prefixExpressions2.iterator().next().startsWith("!") && booleanLiterals2.isEmpty()) {
			findReplacements(booleanLiterals1, prefixExpressions2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_PREFIX_EXPRESSION, container1, container2, classDiff);
		}
		if(statement2.getThisExpressions().size() > 0 && !statement2.getString().equals(JAVA.RETURN_THIS)) {
			findReplacements(variables1, Set.of("this"), replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_THIS_EXPRESSION, container1, container2, classDiff);
		}
		if(statement1.getThisExpressions().size() > 0 && !statement1.getString().equals(JAVA.RETURN_THIS)) {
			findReplacements(Set.of("this"), variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_THIS_EXPRESSION, container1, container2, classDiff);
		}
		if(!container1.isGetter() && !container2.isGetter()) {
			findReplacements(stringLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL, container1, container2, classDiff);
			findReplacements(variables1, stringLiterals2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL, container1, container2, classDiff);
		}
		findReplacements(parenthesizedExpressions1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PARENTHESIZED_EXPRESSION, container1, container2, classDiff);
		findReplacements(variables1, parenthesizedExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PARENTHESIZED_EXPRESSION, container1, container2, classDiff);
		findReplacements(methodInvocations1, stringLiterals2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_STRING_LITERAL, container1, container2, classDiff);
		if(stringLiterals1.size() > 0 && methodInvocationMap2.size() > 0) {
			for(String stringLiteral1 : stringLiterals1) {
				List<LeafExpression> leafExpressions1 = statement1.findExpression(stringLiteral1);
				String value1 = stringLiteral1.substring(1, stringLiteral1.length()-1);
				if(!value1.isEmpty()) {
					for(AbstractCall call2 : statement2.getMethodInvocations()) {
						String expression = call2.getExpression();
						if(expression != null && methodInvocations2.contains(call2.actualString())) {
							if(expression.startsWith("\"") && expression.endsWith("\"")) {
								String value2 = expression.substring(1, expression.length()-1);
								List<LeafExpression> leafExpressions2 = statement2.findExpression(expression);
								if(!value2.isEmpty() && (value1.contains(value2) || value2.contains(value1))) {
									Set<String> set1 = Set.of(stringLiteral1);
									Set<String> set2 = Set.of(call2.actualString());
									findReplacements(set1, set2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_STRING_LITERAL, container1, container2, classDiff);
									if(leafExpressions1.size() == leafExpressions2.size()) {
										for(int i=0; i<leafExpressions1.size(); i++) {
											LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), container1, container2);
											replacementInfo.addSubExpressionMapping(leafMapping);
										}
									}
									break;
								}
								else if(value1.endsWith(".") && value2.contains(value1.substring(0, value1.length()-1))) {
									Set<String> set1 = Set.of(stringLiteral1);
									Set<String> set2 = Set.of(call2.actualString());
									findReplacements(set1, set2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_STRING_LITERAL, container1, container2, classDiff);
									if(leafExpressions1.size() == leafExpressions2.size()) {
										for(int i=0; i<leafExpressions1.size(); i++) {
											LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), container1, container2);
											replacementInfo.addSubExpressionMapping(leafMapping);
										}
									}
									break;
								}
							}
						}
					}
				}
			}
		}
		if((statement1.getNullLiterals().isEmpty() && !statement2.getNullLiterals().isEmpty()) ||
				bothContainNullInDifferentIndexes(invocationCoveringTheEntireStatement1 != null ? invocationCoveringTheEntireStatement1 : creationCoveringTheEntireStatement1,
						invocationCoveringTheEntireStatement2 != null ? invocationCoveringTheEntireStatement2 : creationCoveringTheEntireStatement2)) {
			Set<String> nullLiterals2 = Set.of("null");
			for(String parameter : parameterToArgumentMap.keySet()) { 
				String argument = parameterToArgumentMap.get(parameter); 
				if(!parameter.equals(argument) && variables1.contains(parameter)) {
					variables1.add(argument);
				}
			}
			findReplacements(variables1, nullLiterals2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL, container1, container2, classDiff);
			if(invocationCoveringTheEntireStatement1 != null) {
				String expression = invocationCoveringTheEntireStatement1.getExpression();
				if(expression != null && expression.equals("Optional") && invocationCoveringTheEntireStatement1.getName().equals("empty") &&
						invocationCoveringTheEntireStatement1.arguments().size() == 0) {
					Set<String> invocations1 = new LinkedHashSet<String>();
					invocations1.add(invocationCoveringTheEntireStatement1.actualString());
					findReplacements(invocations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY, container1, container2, classDiff);
				}
			}
			if(methodInvocations1.contains("Optional.empty()")) {
				findReplacements(methodInvocations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY, container1, container2, classDiff);
			}
			if(!creations1.isEmpty()) {
				findReplacements(creations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CREATION, container1, container2, classDiff);
			}
			if(!stringLiterals1.isEmpty()) {
				findReplacements(stringLiterals1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_STRING_LITERAL, container1, container2, classDiff);
			}
			if(!typeLiterals1.isEmpty()) {
				findReplacements(typeLiterals1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_TYPE_LITERAL, container1, container2, classDiff);
			}
		}
		if((!statement1.getNullLiterals().isEmpty() && statement2.getNullLiterals().isEmpty()) ||
				bothContainNullInDifferentIndexes(invocationCoveringTheEntireStatement1 != null ? invocationCoveringTheEntireStatement1 : creationCoveringTheEntireStatement1,
						invocationCoveringTheEntireStatement2 != null ? invocationCoveringTheEntireStatement2 : creationCoveringTheEntireStatement2)) {
			Set<String> nullLiterals1 = Set.of("null");
			for(String parameter : parameterToArgumentMap.keySet()) { 
				String argument = parameterToArgumentMap.get(parameter); 
				if(!parameter.equals(argument) && variables2.contains(parameter)) {
					variables2.add(argument);
				}
			}
			findReplacements(nullLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL, container1, container2, classDiff);
			if(invocationCoveringTheEntireStatement2 != null) {
				String expression = invocationCoveringTheEntireStatement2.getExpression();
				if(expression != null && expression.equals("Optional") && invocationCoveringTheEntireStatement2.getName().equals("empty") &&
						invocationCoveringTheEntireStatement2.arguments().size() == 0) {
					Set<String> invocations2 = new LinkedHashSet<String>();
					invocations2.add(invocationCoveringTheEntireStatement2.actualString());
					findReplacements(nullLiterals1, invocations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY, container1, container2, classDiff);
				}
			}
			if(methodInvocations2.contains("Optional.empty()")) {
				findReplacements(nullLiterals1, methodInvocations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY, container1, container2, classDiff);
			}
			if(!creations2.isEmpty()) {
				findReplacements(nullLiterals1, creations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CREATION, container1, container2, classDiff);
			}
			if(!stringLiterals2.isEmpty()) {
				findReplacements(nullLiterals1, stringLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_STRING_LITERAL, container1, container2, classDiff);
			}
			if(!typeLiterals2.isEmpty()) {
				findReplacements(nullLiterals1, typeLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_TYPE_LITERAL, container1, container2, classDiff);
			}
			if(parentMapper == null && variableDeclarations1.size() > 0 && variableDeclarations2.size() > 0 &&
					variableDeclarations1.get(0).getType() != null && variableDeclarations2.get(0).getType() != null &&
					variableDeclarations1.get(0).getType().equals(variableDeclarations2.get(0).getType()) &&
					statement1.getString().endsWith("=null;\n") && invocationCoveringTheEntireStatement2 != null) {
				findReplacements(nullLiterals1, Set.of(invocationCoveringTheEntireStatement2.actualString()), replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_NULL_LITERAL, container1, container2, classDiff);
			}
		}

		if(statement1.getTernaryOperatorExpressions().isEmpty() && !statement2.getTernaryOperatorExpressions().isEmpty()) {
			if(!statement1.getNullLiterals().isEmpty()) {
				Set<String> nullLiterals1 = new LinkedHashSet<String>();
				nullLiterals1.add("null");
				Set<String> ternaryExpressions2 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
					ternaryExpressions2.add(ternary.getExpression());	
				}
				findReplacements(nullLiterals1, ternaryExpressions2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			}
			if(methodInvocations1.size() > methodInvocations2.size() && !containsMethodSignatureOfAnonymousClass(statement1.getString())) {
				Set<String> ternaryExpressions2 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
					ternaryExpressions2.add(ternary.getExpression());	
				}
				findReplacements(methodInvocations1, ternaryExpressions2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			}
			Set<String> ternaryExpressions2 = new LinkedHashSet<String>();
			Set<String> tmpVariables1 = new LinkedHashSet<String>();
			for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
				List<LeafExpression> thenVariables = ternary.getThenExpression().getVariables();
				List<LeafExpression> elseVariables = ternary.getElseExpression().getVariables();
				if((thenVariables.size() > 0 && ternary.getThenExpression().getExpression().equals(thenVariables.get(0).getString())) ||
						(elseVariables.size() > 0 && ternary.getElseExpression().getString().equals(elseVariables.get(0).getString()))) {
					ternaryExpressions2.add(ternary.getExpression());
					tmpVariables1.addAll(convertToStringSet(ternary.getCondition().getVariables()));
					tmpVariables1.addAll(convertToStringSet(ternary.getThenExpression().getVariables()));
					tmpVariables1.addAll(convertToStringSet(ternary.getElseExpression().getVariables()));
				}
			}
			findReplacements(tmpVariables1, ternaryExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			if(variableDeclarations1.size() > 0 && variableDeclarations2.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString()) &&
					variableDeclarations2.get(0).getInitializer() != null && variableDeclarations2.get(0).getInitializer().getString().equals(statement2.getTernaryOperatorExpressions().get(0).getString()) &&
					variableDeclarations1.get(0).getInitializer() != null && variables1.contains(variableDeclarations1.get(0).getInitializer().getString())) {
				findReplacements(variables1, ternaryExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			}
		}
		else if(!statement1.getTernaryOperatorExpressions().isEmpty() && statement2.getTernaryOperatorExpressions().isEmpty()) {
			if(!statement2.getNullLiterals().isEmpty()) {
				Set<String> nullLiterals2 = new LinkedHashSet<String>();
				nullLiterals2.add("null");
				Set<String> ternaryExpressions1 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
					ternaryExpressions1.add(ternary.getExpression());	
				}
				findReplacements(ternaryExpressions1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			}
			if(methodInvocations2.size() > methodInvocations1.size() && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
				Set<String> ternaryExpressions1 = new LinkedHashSet<String>();
				for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
					ternaryExpressions1.add(ternary.getExpression());	
				}
				findReplacements(ternaryExpressions1, methodInvocations2, replacementInfo, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			}
			Set<String> ternaryExpressions1 = new LinkedHashSet<String>();
			Set<String> tmpVariables2 = new LinkedHashSet<String>();
			for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
				List<LeafExpression> thenVariables = ternary.getThenExpression().getVariables();
				List<LeafExpression> elseVariables = ternary.getElseExpression().getVariables();
				if((thenVariables.size() > 0 && ternary.getThenExpression().getExpression().equals(thenVariables.get(0).getString())) ||
						(elseVariables.size() > 0 && ternary.getElseExpression().getString().equals(elseVariables.get(0).getString()))) {
					ternaryExpressions1.add(ternary.getExpression());
					tmpVariables2.addAll(convertToStringSet(ternary.getCondition().getVariables()));
					tmpVariables2.addAll(convertToStringSet(thenVariables));
					tmpVariables2.addAll(convertToStringSet(elseVariables));
				}
			}
			findReplacements(ternaryExpressions1, tmpVariables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			if(variableDeclarations1.size() > 0 && variableDeclarations2.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString()) &&
					variableDeclarations1.get(0).getInitializer() != null && variableDeclarations1.get(0).getInitializer().getString().equals(statement1.getTernaryOperatorExpressions().get(0).getString()) &&
					variableDeclarations2.get(0).getInitializer() != null && variables2.contains(variableDeclarations2.get(0).getInitializer().getString())) {
				findReplacements(ternaryExpressions1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_CONDITIONAL_EXPRESSION, container1, container2, classDiff);
			}
		}
		if(!statement1.getString().endsWith("=true;\n") && !statement1.getString().endsWith("=false;\n")) {
			findReplacements(booleanLiterals1, arguments2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_ARGUMENT, container1, container2, classDiff);
			findReplacements(booleanLiterals1, variables2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE, container1, container2, classDiff);
			if(booleanLiterals1.isEmpty() && statement1.getBooleanLiterals().size() != statement2.getBooleanLiterals().size()) {
				Set<String> literals1 = convertToStringSet(statement1.getBooleanLiterals());
				Set<String> literals2 = convertToStringSet(statement2.getBooleanLiterals());
				if(literals1.equals(literals2) ||
						matchingArgument(literals1, arguments2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
						matchingArgument(literals1, arguments2, creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2)) {
					findReplacements(literals1, arguments2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_ARGUMENT, container1, container2, classDiff);
				}
				if(literals1.equals(literals2) ||
						matchingArgument(literals1, variables2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
						matchingArgument(literals1, variables2, creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2)) {
					findReplacements(literals1, variables2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE, container1, container2, classDiff);
				}
			}
		}
		if(!statement2.getString().endsWith("=true;\n") && !statement2.getString().endsWith("=false;\n")) {
			if(statement1.getBooleanLiterals().size() != statement2.getBooleanLiterals().size()) {
				Set<String> literals1 = convertToStringSet(statement1.getBooleanLiterals());
				Set<String> literals2 = convertToStringSet(statement2.getBooleanLiterals());
				if(literals1.equals(literals2) ||
						matchingArgument(arguments1, literals2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
						matchingArgument(arguments1, literals2, creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2)) {
					findReplacements(arguments1, literals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_ARGUMENT, container1, container2, classDiff);
				}
				if(literals1.equals(literals2) ||
						matchingArgument(variables1, literals2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
						matchingArgument(variables1, literals2, creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2)) {
					findReplacements(variables1, literals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE, container1, container2, classDiff);
				}
			}
		}
		if((statement2.getString().endsWith("true;\n") && statement1.getString().endsWith("Boolean.TRUE;\n")) ||
				(statement2.getString().endsWith("false;\n") && statement1.getString().endsWith("Boolean.FALSE;\n"))) {
			findReplacements(variables1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE, container1, container2, classDiff);
		}
		if((statement1.getString().endsWith("true;\n") && statement2.getString().endsWith("Boolean.TRUE;\n")) ||
				(statement1.getString().endsWith("false;\n") && statement2.getString().endsWith("Boolean.FALSE;\n"))) {
			findReplacements(booleanLiterals1, variables2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE, container1, container2, classDiff);
		}
		if((statement1.getString().endsWith("true;\n") && statement2.getString().endsWith("false;\n")) ||
				(statement1.getString().endsWith("false;\n") && statement2.getString().endsWith("true;\n"))) {
			findReplacements(booleanLiterals1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_LITERAL, container1, container2, classDiff);
		}
		if(statement1.getString().contains(" != null") || statement1.getString().contains(" == null")) {
			for(String key : methodInvocationMap2.keySet()) {
				List<? extends AbstractCall> calls2 = methodInvocationMap2.get(key);
				for(AbstractCall call : calls2) {
					if(call.getName().equals("isPresent") && call.getExpression() != null) {
						String checkIfNull = call.getExpression() + " == null";
						String checkIfNotNull = call.getExpression() + " != null";
						if(statement1.getString().contains(checkIfNotNull)) {
							Set<String> set1 = Set.of(checkIfNotNull);
							Set<String> set2 = Set.of(call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_PRESENT_CHECK, container1, container2, classDiff);
						}
						else if(statement1.getString().contains(checkIfNull)) {
							Set<String> set1 = Set.of(checkIfNull);
							Set<String> set2 = Set.of("!" + call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_PRESENT_CHECK, container1, container2, classDiff);
						}
					}
					else if(call.getName().equals("isEmpty") && call.getExpression() != null) {
						String checkIfNull = call.getExpression() + " == null";
						String checkIfNotNull = call.getExpression() + " != null";
						if(statement1.getString().contains(checkIfNotNull)) {
							Set<String> set1 = Set.of(checkIfNotNull);
							Set<String> set2 = Set.of("!" + call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_EMPTY_CHECK, container1, container2, classDiff);
						}
						else if(statement1.getString().contains(checkIfNull)) {
							Set<String> set1 = Set.of(checkIfNull);
							Set<String> set2 = Set.of(call.actualString());
							findReplacements(set1, set2, replacementInfo, ReplacementType.NULL_LITERAL_CHECK_REPLACED_WITH_OPTIONAL_IS_EMPTY_CHECK, container1, container2, classDiff);
						}
					}
				}
			}
		}
		if(variableDeclarations1.toString().equals(variableDeclarations2.toString()) && castExpressions1.size() == castExpressions2.size() && castExpressions1.size() > 0) {
			findReplacements(castExpressions1, castExpressions2, replacementInfo, ReplacementType.CAST_EXPRESSION, container1, container2, classDiff);
		}
		if(!argumentsWithIdenticalMethodCalls(arguments1, arguments2, methodInvocations1, methodInvocations2)) {
			findReplacements(arguments1, methodInvocations2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_METHOD_INVOCATION, container1, container2, classDiff);
			findReplacements(methodInvocations1, arguments2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_METHOD_INVOCATION, container1, container2, classDiff);
			boolean anonymousArgument1 = false;
			if(arguments1.size() == 1 && containsMethodSignatureOfAnonymousClass(arguments1.iterator().next())) {
				anonymousArgument1 = true;
			}
			boolean anonymousArgument2 = false;
			if(arguments2.size() == 1 && containsMethodSignatureOfAnonymousClass(arguments2.iterator().next())) {
				anonymousArgument2 = true;
			}
			if(!anonymousArgument1 && !anonymousArgument2) {
				findReplacements(arguments1, arguments2, replacementInfo, ReplacementType.ARGUMENT, container1, container2, classDiff);
			}
		}
		if(parentMapper != null && statement1.getParent() != null && statement2.getParent() != null &&
				statement1.getParent().getLocationInfo().getCodeElementType().equals(statement2.getParent().getLocationInfo().getCodeElementType())) {
			if(statement1.getString().equals(JAVA.RETURN_STATEMENT) && statement2.getString().equals(JAVA.RETURN_NULL)) {
				return replacementInfo.getReplacements();
			}
			else if(statement1.getString().equals(JAVA.RETURN_NULL) && statement2.getString().equals(JAVA.RETURN_STATEMENT)) {
				return replacementInfo.getReplacements();
			}
			if(statement1.getString().equals(JAVA.RETURN_FALSE) && statement2.getString().equals(JAVA.RETURN_NULL)) {
				return replacementInfo.getReplacements();
			}
			else if(statement1.getString().equals(JAVA.RETURN_NULL) && statement2.getString().equals(JAVA.RETURN_FALSE)) {
				return replacementInfo.getReplacements();
			}
		}
		else if(parentMapper == null && statement1.getParent() != null && statement2.getParent() != null &&
				statement1.getParent().getLocationInfo().getCodeElementType().equals(statement2.getParent().getLocationInfo().getCodeElementType())) {
			if(container1 instanceof UMLOperation && container2 instanceof UMLOperation) {
				UMLParameter returnParameter1 = ((UMLOperation)container1).getReturnParameter();
				UMLParameter returnParameter2 = ((UMLOperation)container2).getReturnParameter();
				if(returnParameter1 != null && returnParameter2 != null) {
					UMLType returnType1 = returnParameter1.getType();
					UMLType returnType2 = returnParameter2.getType();
					if(returnType1.getClassType().equals("void") && returnType2.getClassType().equals("boolean")) {
						if(statement1.getString().equals(JAVA.RETURN_STATEMENT) && statement2.getString().equals(JAVA.RETURN_FALSE)) {
							return replacementInfo.getReplacements();
						}
						if(statement1.getString().equals(JAVA.RETURN_STATEMENT) && statement2.getString().equals(JAVA.RETURN_TRUE)) {
							return replacementInfo.getReplacements();
						}
					}
					else if(returnType1.getClassType().equals("boolean") && returnType2.getClassType().equals("void")) {
						if(statement2.getString().equals(JAVA.RETURN_STATEMENT) && statement1.getString().equals(JAVA.RETURN_FALSE)) {
							return replacementInfo.getReplacements();
						}
						if(statement2.getString().equals(JAVA.RETURN_STATEMENT) && statement1.getString().equals(JAVA.RETURN_TRUE)) {
							return replacementInfo.getReplacements();
						}
					}
					if(returnType1.getClassType().equals("void") && !returnType2.getClassType().equals("void")) {
						if(statement1.getString().equals(JAVA.RETURN_STATEMENT) && statement2.getVariables().size() > 0 && statement2.getString().equals(JAVA.RETURN_SPACE + statement2.getVariables().get(0).getString() + JAVA.STATEMENT_TERMINATION)) {
							VariableDeclaration variableDeclaration2 = container2.getVariableDeclaration(statement2.getVariables().get(0).getString());
							if(variableDeclaration2 != null && variableDeclaration2.getType() != null && variableDeclaration2.getType().equals(returnType2)) {
								return replacementInfo.getReplacements();
							}
						}
					}
					else if(!returnType1.getClassType().equals("void") && returnType2.getClassType().equals("void")) {
						if(statement2.getString().equals(JAVA.RETURN_STATEMENT) && statement1.getVariables().size() > 0 && statement1.getString().equals(JAVA.RETURN_SPACE + statement1.getVariables().get(0).getString() + JAVA.STATEMENT_TERMINATION)) {
							VariableDeclaration variableDeclaration1 = container1.getVariableDeclaration(statement1.getVariables().get(0).getString());
							if(variableDeclaration1 != null && variableDeclaration1.getType() != null && variableDeclaration1.getType().equals(returnType1)) {
								return replacementInfo.getReplacements();
							}
						}
					}
					//match break with already matched return
					if(statement1.getString().equals(JAVA.BREAK_STATEMENT)) {
						Set<AbstractCodeMapping> mappingsToBeAdded = new LinkedHashSet<>();
						for(AbstractCodeMapping mapping : mappings) {
							AbstractCodeFragment fragment2 = mapping.getFragment2();
							if(fragment2.getParent() != null && fragment2.getString().startsWith(JAVA.RETURN_SPACE)) {
								CompositeStatementObject parent1 = statement1.getParent();
								CompositeStatementObject parent2 = fragment2.getParent();
								String signature1 = parent1.getSignature();
								String signature2 = parent2.getSignature();
								if(signature1.equals(signature2)) {
									LeafMapping leafMapping = operationBodyMapper.createLeafMapping(statement1, fragment2, parameterToArgumentMap, equalNumberOfAssertions);
									mappingsToBeAdded.add(leafMapping);
									break;
								}
							}
						}
						for(AbstractCodeMapping mapping : mappingsToBeAdded) {
							operationBodyMapper.addMapping(mapping);
						}
					}
				}
			}
		}
		
		String s1 = operationBodyMapper.preprocessInput1(statement1, statement2);
		String s2 = operationBodyMapper.preprocessInput2(statement1, statement2);
		replacementsToBeRemoved = new LinkedHashSet<Replacement>();
		replacementsToBeAdded = new LinkedHashSet<Replacement>();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			s1 = ReplacementUtil.performReplacement(s1, s2, replacement.getBefore(), replacement.getAfter());
			//find method invocation replacements within method invocation replacements, the boolean value indicates if the remaining part of the original replacement is identical or not
			Map<Replacement, Boolean> nestedReplacementMap = replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), methodInvocations1, methodInvocations2, methodInvocationMap1, methodInvocationMap2);
			if(!nestedReplacementMap.isEmpty()) {
				if(!nestedReplacementMap.values().contains(false)) {
					replacementsToBeRemoved.add(replacement);
				}
				replacementsToBeAdded.addAll(nestedReplacementMap.keySet());
			}
			//find variable-to-method-invocation replacements within method invocation replacements, the boolean value indicates if the remaining part of the original replacement is identical or not
			if(nestedReplacementMap.isEmpty()) {
				nestedReplacementMap.putAll(replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1, methodInvocations2, methodInvocationMap2, Direction.VARIABLE_TO_INVOCATION));
				nestedReplacementMap.putAll(replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), methodInvocations1, variables2, methodInvocationMap1, Direction.INVOCATION_TO_VARIABLE));
			}
			if(!nestedReplacementMap.isEmpty()) {
				if(!nestedReplacementMap.values().contains(false)) {
					replacementsToBeRemoved.add(replacement);
				}
				replacementsToBeAdded.addAll(nestedReplacementMap.keySet());
			}
			boolean methodInvocationReplacementWithDifferentNumberOfArguments = false;
			if(replacement instanceof MethodInvocationReplacement) {
				MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
				AbstractCall invokedOperationBefore = methodInvocationReplacement.getInvokedOperationBefore();
				AbstractCall invokedOperationAfter = methodInvocationReplacement.getInvokedOperationAfter();
				if(invokedOperationBefore.arguments().size() != invokedOperationAfter.arguments().size()) {
					methodInvocationReplacementWithDifferentNumberOfArguments = true;
				}
			}
			if(!methodInvocationReplacementWithDifferentNumberOfArguments) {
				Set<Replacement> r = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1, variables2, operationBodyMapper.getParameterToArgumentMap1());
				if(!r.isEmpty()) {
					replacementsToBeRemoved.add(replacement);
					replacementsToBeAdded.addAll(r);
				}
				Set<Replacement> r2 = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), stringLiterals1, variables2, operationBodyMapper.getParameterToArgumentMap1());
				if(!r2.isEmpty()) {
					replacementsToBeRemoved.add(replacement);
					replacementsToBeAdded.addAll(r2);
				}
			}
		}
		replacementInfo.removeReplacements(replacementsToBeRemoved);
		replacementInfo.addReplacements(replacementsToBeAdded);
		boolean isEqualWithReplacement = s1.equals(s2) || (s1 + JAVA.STATEMENT_TERMINATION).equals(s2) || (s2 + JAVA.STATEMENT_TERMINATION).equals(s1) || ("final " + s1 + JAVA.STATEMENT_TERMINATION).equals(s2) || ("final " + s2 + JAVA.STATEMENT_TERMINATION).equals(s1) || replacementInfo.getArgumentizedString1().equals(replacementInfo.getArgumentizedString2()) || equalAfterParenthesisElimination(s1, s2) ||
				differOnlyInCastExpressionOrPrefixOperatorOrInfixOperand(s1, s2, methodInvocationMap1, methodInvocationMap2, statement1, statement2, variableDeclarations1, variableDeclarations2, replacementInfo, operationBodyMapper) ||
				differOnlyInFinalModifier(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) || differOnlyInThis(s1, s2) || differOnlyInThrow(s1, s2) || matchAsLambdaExpressionArgument(s1, s2, parameterToArgumentMap, replacementInfo, statement1, container2, operationBodyMapper) || differOnlyInDefaultInitializer(s1, s2, variableDeclarations1, variableDeclarations2) ||
				oneIsVariableDeclarationTheOtherIsVariableAssignment(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) || identicalVariableDeclarationsWithDifferentNames(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) ||
				oneIsVariableDeclarationTheOtherIsReturnStatement(s1, s2, variableDeclarations1, variableDeclarations2) || oneIsVariableDeclarationTheOtherIsReturnStatement(statement1.getString(), statement2.getString(), variableDeclarations1, variableDeclarations2) ||
				(invocationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null && creationCoveringTheEntireStatement1 == null && creationCoveringTheEntireStatement2 == null && wrapInMethodCall(s1, s2, methodInvocationMap1, replacementInfo)) ||
				(containsValidOperatorReplacements(replacementInfo) && (equalAfterInfixExpressionExpansion(s1, s2, replacementInfo, statement1.getInfixExpressions()) || commonConditional(s1, s2, parameterToArgumentMap, replacementInfo, statement1, statement2, operationBodyMapper))) ||
				equalAfterArgumentMerge(s1, s2, replacementInfo) ||
				equalAfterNewArgumentAdditions(s1, s2, replacementInfo, container1, container2, operationSignatureDiff, classDiff) ||
				(validStatementForConcatComparison(statement1, statement2) && commonConcat(s1, s2, parameterToArgumentMap, replacementInfo, statement1, statement2, operationBodyMapper));
		if(isEqualWithReplacement) {
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
				List<Replacement> typeReplacements = replacementInfo.getReplacements(ReplacementType.TYPE);
				for(Replacement typeReplacement : typeReplacements) {
					if(invocationCoveringTheEntireStatement1.getName().contains(typeReplacement.getBefore()) && invocationCoveringTheEntireStatement2.getName().contains(typeReplacement.getAfter())) {
						if(invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2) && invocationCoveringTheEntireStatement1.equalArguments(invocationCoveringTheEntireStatement2)) {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
									invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
							replacementInfo.addReplacement(replacement);
						}
						else {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
									invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
						}
						break;
					}
				}
				if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) &&
						invocationCoveringTheEntireStatement1.staticInvokerExpressionReplaced(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements()) &&
						invocationCoveringTheEntireStatement1.allArgumentsReplaced(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
					replacementInfo.addReplacement(replacement);
				}
				for(Replacement r : replacementInfo.getReplacements()) {
					if(r instanceof VariableReplacementWithMethodInvocation) {
						VariableReplacementWithMethodInvocation replacement = (VariableReplacementWithMethodInvocation)r;
						AbstractCall call = replacement.getInvokedOperation();
						if(call.getName().equals("of") || call.getName().equals("asList")) {
							if(replacement.getDirection().equals(Direction.VARIABLE_TO_INVOCATION)) {
								for(String argument2 : call.arguments()) {
									for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
										AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
										if(invocation1 != null && invocation1.getExpression() != null && invocation1.getExpression().equals(replacement.getBefore())) {
											boolean argumentMatched = false;
											for(String argument1 : invocation1.arguments()) {
												if(argument1.equals(argument2)) {
													List<LeafExpression> leafExpressions1 = fragment1.findExpression(argument1);
													List<LeafExpression> leafExpressions2 = statement2.findExpression(argument2);
													if(leafExpressions1.size() == 1 && leafExpressions2.size() == 1) {
														LeafMapping mapping = operationBodyMapper.createLeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), parameterToArgumentMap, equalNumberOfAssertions);
														operationBodyMapper.addMapping(mapping);
													}
													argumentMatched = true;
													break;
												}
											}
											if(argumentMatched) {
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
			if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
					!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
					!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
					!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
					!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				return null;
			}
			if(variableAssignmentWithEverythingReplaced(statement1, statement2, replacementInfo, operationBodyMapper)) {
				return null;
			}
			if(classInstanceCreationWithEverythingReplaced(statement1, statement2, replacementInfo, parameterToArgumentMap)) {
				return null;
			}
			if(operatorExpressionWithEverythingReplaced(statement1, statement2, replacementInfo, parameterToArgumentMap)) {
				return null;
			}
			if(thisConstructorCallWithEverythingReplaced(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, replacementInfo)) {
				return null;
			}
			if(invocationWithEverythingReplaced(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, replacementInfo)) {
				return null;
			}
			if(statement1.getString().startsWith(JAVA.RETURN_SPACE) && statement2.getString().startsWith(JAVA.RETURN_SPACE) && statement1.getParent() != null && statement2.getParent() != null &&
					!statement1.getParent().getLocationInfo().getCodeElementType().equals(statement2.getParent().getLocationInfo().getCodeElementType())) {
				if(!(statement1.isLastStatementInParentBlock() && statement1.getParent() instanceof TryStatementObject && statement2.isLastStatement()) &&
						!(statement2.isLastStatementInParentBlock() && statement2.getParent() instanceof TryStatementObject && statement1.isLastStatement())) {
					return null;
				}
			}
			if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty()) {
				Set<Replacement> replacementsInsideAnonymous = new LinkedHashSet<Replacement>();
				for(Replacement replacement : replacementInfo.getReplacements()) {
					if(replacement instanceof MethodInvocationReplacement) {
						MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
						for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
							for(int j=0; j<anonymousClassDeclarations2.size(); j++) {
								AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
								AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(j);
								if(anonymousClassDeclaration1.getMethodInvocations().contains(methodInvocationReplacement.getInvokedOperationBefore()) &&
										anonymousClassDeclaration2.getMethodInvocations().contains(methodInvocationReplacement.getInvokedOperationAfter())) {
									replacementsInsideAnonymous.add(replacement);
									break;
								}
							}
							if(replacementsInsideAnonymous.contains(replacement)) {
								break;
							}
						}
					}
				}
				for(Replacement replacement : replacementsInsideAnonymous) {
					equalAfterNewArgumentAdditions(replacement.getBefore(), replacement.getAfter(), replacementInfo, container1, container2, operationSignatureDiff, classDiff);
				}
			}
			processAnonymousAndLambdas(statement1, statement2, parameterToArgumentMap, replacementInfo,
					assignmentInvocationCoveringTheEntireStatement1 != null ? assignmentInvocationCoveringTheEntireStatement1 : assignmentCreationCoveringTheEntireStatement1,
					assignmentInvocationCoveringTheEntireStatement2 != null ? assignmentInvocationCoveringTheEntireStatement2 : assignmentCreationCoveringTheEntireStatement2,
					methodInvocationMap1, methodInvocationMap2,	anonymousClassDeclarations1, anonymousClassDeclarations2, lambdas1, lambdas2, operationBodyMapper);
			if(s1.equals(s2) && replacementInfo.containsOnlyReplacement(ReplacementType.INFIX_OPERATOR) && containsValidOperatorReplacements(replacementInfo)) {
				List<Replacement> operatorReplacements = replacementInfo.getReplacements(ReplacementType.INFIX_OPERATOR);
				boolean booleanOperatorReversed = false;
				for(Replacement r : operatorReplacements) {
					if(r.getBefore().equals("&&") && r.getAfter().equals("||")) {
						booleanOperatorReversed = true;
					}
					else if(r.getBefore().equals("||") && r.getAfter().equals("&&")) {
						booleanOperatorReversed = true;
					}
					else if(r.getBefore().equals("==") && r.getAfter().equals("!=")) {
						booleanOperatorReversed = true;
					}
					else if(r.getBefore().equals("!=") && r.getAfter().equals("==")) {
						booleanOperatorReversed = true;
					}
				}
				if(booleanOperatorReversed) {
					if(statement1 instanceof AbstractExpression) {
						CompositeStatementObject owner = ((AbstractExpression)statement1).getOwner();
						if(owner != null) {
							InvertConditionRefactoring invert = new InvertConditionRefactoring(owner, statement2, container1, container2);
							operationBodyMapper.getRefactoringsAfterPostProcessing().add(invert);
						}
					}
					else {
						InvertConditionRefactoring invert = new InvertConditionRefactoring(statement1, statement2, container1, container2);
						operationBodyMapper.getRefactoringsAfterPostProcessing().add(invert);
					}
				}
			}
			return replacementInfo.getReplacements();
		}
		Set<Replacement> replacements = processAnonymousAndLambdas(statement1, statement2, parameterToArgumentMap, replacementInfo,
				assignmentInvocationCoveringTheEntireStatement1 != null ? assignmentInvocationCoveringTheEntireStatement1 : assignmentCreationCoveringTheEntireStatement1,
				assignmentInvocationCoveringTheEntireStatement2 != null ? assignmentInvocationCoveringTheEntireStatement2 : assignmentCreationCoveringTheEntireStatement2,
				methodInvocationMap1, methodInvocationMap2,	anonymousClassDeclarations1, anonymousClassDeclarations2, lambdas1, lambdas2, operationBodyMapper);
		if(replacements != null) {
			return replacements;
		}
		//match if with switch
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
			CompositeStatementObject if1 = (CompositeStatementObject)statement1;
			CompositeStatementObject switch2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions1 = if1.getExpressions();
			List<AbstractExpression> expressions2 = switch2.getExpressions();
			Set<CompositeStatementObject> elseIfChain = new LinkedHashSet<CompositeStatementObject>();
			elseIfChain.add(if1);
			CompositeStatementObject current = if1;
			while(current != null) {
				current = getElseIfParent(current);
				if(current != null) {
					elseIfChain.add(current);
				}
			}
			current = if1;
			while(current != null) {
				current = getElseIfBranch(current);
				if(current != null) {
					elseIfChain.add(current);
				}
			}
			Set<StatementObject> switchCases = new LinkedHashSet<StatementObject>(); 
			for(AbstractStatement statement : switch2.getStatements()) {
				if(statement.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_CASE) && !statement.getString().equals("default:")) {
					switchCases.add((StatementObject)statement);
				}
			}
			List<LeafExpression> switchLeafExpression = switch2.findExpression(switch2.getExpressions().get(0).getString());
			Set<LeafMapping> caseLeafMappings = new LinkedHashSet<LeafMapping>();
			Set<LeafMapping> switchExpressionLeafMappings = new LinkedHashSet<LeafMapping>(); 
			for(StatementObject switchCase : switchCases) {
				String string = switchCase.getString();
				if(string.startsWith("case ")) {
					String caseExpression = string.substring(5, string.length()-1);
					List<LeafExpression> leafExpressions2 = switchCase.findExpression(caseExpression);
					for(CompositeStatementObject ifStatement : elseIfChain) {
						List<LeafExpression> leafExpressions1 = ifStatement.findExpression(caseExpression);
						if(leafExpressions1.size() > 0 && leafExpressions2.size() == leafExpressions1.size()) {
							for(int i=0; i<leafExpressions1.size(); i++) {
								LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), container1, container2);
								caseLeafMappings.add(leafMapping);
							}
						}
						if(switchLeafExpression.size() == 1) {
							leafExpressions1 = ifStatement.findExpression(switch2.getExpressions().get(0).getString());
							if(leafExpressions1.size() == 1) {
								for(int i=0; i<leafExpressions1.size(); i++) {
									LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), switchLeafExpression.get(i), container1, container2);
									switchExpressionLeafMappings.add(leafMapping);
								}
							}
						}
					}
				}
			}
			if(caseLeafMappings.size() == Math.min(elseIfChain.size(), switchCases.size()) ||
					switchExpressionLeafMappings.size() == Math.min(elseIfChain.size(), switchCases.size())) {
				for(LeafMapping leafMapping : caseLeafMappings) {
					replacementInfo.addSubExpressionMapping(leafMapping);
				}
				for(LeafMapping leafMapping : switchExpressionLeafMappings) {
					replacementInfo.addSubExpressionMapping(leafMapping);
				}
				return replacementInfo.getReplacements();
			}
			if(expressions1.size() == 1 && expressions2.size() == 1 && expressions1.get(0).getString().equals(expressions2.get(0).getString())) {
				return replacementInfo.getReplacements();
			}
		}
		//match switch with if
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			CompositeStatementObject switch1 = (CompositeStatementObject)statement1;
			CompositeStatementObject if2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions1 = switch1.getExpressions();
			List<AbstractExpression> expressions2 = if2.getExpressions();
			if(expressions1.size() == 1 && expressions2.size() == 1 && expressions1.get(0).getString().equals(expressions2.get(0).getString())) {
				return replacementInfo.getReplacements();
			}
		}
		//match traditional for with enhanced for
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for1 = (CompositeStatementObject)statement1;
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions2 = for2.getExpressions();
			AbstractExpression enhancedForExpression = expressions2.get(expressions2.size()-1);
			VariableDeclaration inlinedVariableDeclaration = null;
			for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
				for(VariableDeclaration variableDeclaration : fragment1.getVariableDeclarations()) {
					if(variableDeclaration.getInitializer() != null && variableDeclaration.getInitializer().getString().equals(enhancedForExpression.getString())) {
						inlinedVariableDeclaration = variableDeclaration;
						break;
					}
				}
				if(fragment1.getVariableDeclarations().toString().equals(for2.getVariableDeclarations().toString())) {
					for(int i=0; i<fragment1.getVariableDeclarations().size(); i++) {
						//VariableDeclaration declaration1 = fragment1.getVariableDeclarations().get(i);
						VariableDeclaration declaration2 = for2.getVariableDeclarations().get(i);
						LeafMapping leafMapping = new LeafMapping(fragment1, declaration2.asLeafExpression(), container1, container2);
						replacementInfo.addSubExpressionMapping(leafMapping);
					}
				}
			}
			//search for previous variable declaration mappings having these for loops in their scope
			String renamedVariable = null;
			for(AbstractCodeMapping previousMapping : mappings) {
				if(previousMapping.getFragment1().getVariableDeclarations().size() > 0 && previousMapping.getFragment2().getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration1 = previousMapping.getFragment1().getVariableDeclarations().get(0);
					VariableDeclaration declaration2 = previousMapping.getFragment2().getVariableDeclarations().get(0);
					if(declaration1.getScope().subsumes(for1.getLocationInfo()) && declaration2.getScope().subsumes(for2.getLocationInfo())) {
						if(declaration2.getVariableName().equals(enhancedForExpression.getString())) {
							renamedVariable = declaration1.getVariableName();
							break;
						}
					}
				}
			}
			for(AbstractExpression expression1 : for1.getExpressions()) {
				if(expression1.getString().contains(enhancedForExpression.getString() + ".length") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".size()") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".iterator()") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".listIterator()") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".descendingIterator()")) {
					return replacementInfo.getReplacements();
				}
				if(renamedVariable != null) {
					if(expression1.getString().contains(renamedVariable + ".length") ||
							expression1.getString().contains(renamedVariable + ".size()") ||
							expression1.getString().contains(renamedVariable + ".iterator()") ||
							expression1.getString().contains(renamedVariable + ".listIterator()") ||
							expression1.getString().contains(renamedVariable + ".descendingIterator()")) {
						return replacementInfo.getReplacements();
					}
				}
				if(inlinedVariableDeclaration != null) {
					if(expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".length") ||
							expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".size()") ||
							expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".iterator()") ||
							expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".listIterator()") ||
							expression1.getString().contains(inlinedVariableDeclaration.getVariableName() + ".descendingIterator()")) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		if(statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) &&
				statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for1 = (CompositeStatementObject)statement1;
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions1 = for1.getExpressions();
			AbstractExpression enhancedForExpression = expressions1.get(expressions1.size()-1);
			//search for previous variable declaration mappings having these for loops in their scope
			String renamedVariable = null;
			for(AbstractCodeMapping previousMapping : mappings) {
				if(previousMapping.getFragment1().getVariableDeclarations().size() > 0 && previousMapping.getFragment2().getVariableDeclarations().size() > 0) {
					VariableDeclaration declaration1 = previousMapping.getFragment1().getVariableDeclarations().get(0);
					VariableDeclaration declaration2 = previousMapping.getFragment2().getVariableDeclarations().get(0);
					if(declaration1.getScope().subsumes(for1.getLocationInfo()) && declaration2.getScope().subsumes(for2.getLocationInfo())) {
						if(declaration1.getVariableName().equals(enhancedForExpression.getString())) {
							renamedVariable = declaration2.getVariableName();
							break;
						}
					}
				}
			}
			for(AbstractExpression expression2 : for2.getExpressions()) {
				if(expression2.getString().contains(enhancedForExpression.getString() + ".length") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".size()") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".iterator()") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".listIterator()") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".descendingIterator()")) {
					return replacementInfo.getReplacements();
				}
				if(renamedVariable != null) {
					if(expression2.getString().contains(renamedVariable + ".length") ||
							expression2.getString().contains(renamedVariable + ".size()") ||
							expression2.getString().contains(renamedVariable + ".iterator()") ||
							expression2.getString().contains(renamedVariable + ".listIterator()") ||
							expression2.getString().contains(renamedVariable + ".descendingIterator()")) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//match while with enhanced for
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
			List<AbstractExpression> expressions2 = for2.getExpressions();
			AbstractExpression enhancedForExpression = expressions2.get(expressions2.size()-1);
			VariableDeclaration iteratorDeclaration1 = null;
			for(AbstractCodeFragment codeFragment : replacementInfo.getStatements1()) {
				AbstractCall invocation = codeFragment.invocationCoveringEntireFragment();
				if(invocation != null && invocation.getExpression() != null) {
					if(invocation.getExpression().equals(enhancedForExpression.getString())) {
						List<VariableDeclaration> variableDeclarations = codeFragment.getVariableDeclarations();
						if(variableDeclarations.size() == 1 && codeFragment.getLocationInfo().before(statement1.getLocationInfo())) {
							boolean iteratorDeclarationFound = false;
							for(String key : methodInvocationMap1.keySet()) {
								for(AbstractCall call : methodInvocationMap1.get(key)) {
									if(call.getExpression() != null && call.getExpression().equals(variableDeclarations.get(0).getVariableName())) {
										iteratorDeclarationFound = true;
										break;
									}
								}
							}
							if(iteratorDeclarationFound) {
								iteratorDeclaration1 = variableDeclarations.get(0);
								additionallyMatchedStatements1.add(codeFragment);
							}
						}
					}
					else if(iteratorDeclaration1 != null && invocation.getExpression().equals(iteratorDeclaration1.getVariableName())) {
						List<VariableDeclaration> variableDeclarations = codeFragment.getVariableDeclarations();
						if(variableDeclarations.size() == 1 && variableDeclarations2.size() == 1 && statement1.getLocationInfo().subsumes(codeFragment.getLocationInfo())) {
							//check if variable name and type are the same with the enhanced-for parameter
							VariableDeclaration v1 = variableDeclarations.get(0);
							VariableDeclaration v2 = variableDeclarations2.get(0);
							if(v1.getVariableName().equals(v2.getVariableName()) && v1.getType() != null && v1.getType().equals(v2.getType())) {
								additionallyMatchedStatements1.add(codeFragment);
							}
						}
					}
				}
			}
			if(additionallyMatchedStatements1.size() > 0) {
				Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
				replacementInfo.addReplacement(composite);
				return replacementInfo.getReplacements();
			}
		}
		//match try-with-resources with regular try
		if(statement1 instanceof TryStatementObject && statement2 instanceof TryStatementObject) {
			TryStatementObject try1 = (TryStatementObject)statement1;
			TryStatementObject try2 = (TryStatementObject)statement2;
			if(!try1.isTryWithResources() && try2.isTryWithResources()) {
				List<AbstractStatement> tryStatements1 = try1.getStatements();
				List<AbstractStatement> tryStatements2 = try2.getStatements();
				List<AbstractCodeFragment> matchedChildStatements1 = new ArrayList<>();
				List<AbstractCodeFragment> matchedChildStatements2 = new ArrayList<>();
				for(AbstractCodeMapping mapping : mappings) {
					if(tryStatements1.contains(mapping.getFragment1()) && tryStatements2.contains(mapping.getFragment2())) {
						matchedChildStatements1.add(mapping.getFragment1());
						matchedChildStatements2.add(mapping.getFragment2());
					}
				}
				if(matchedChildStatements1.size() > 0 && matchedChildStatements2.size() > 0) {
					List<AbstractStatement> unmatchedStatementsTry1 = new ArrayList<>();
					for(AbstractStatement tryStatement1 : tryStatements1) {
						if(!matchedChildStatements1.contains(tryStatement1)) {
							unmatchedStatementsTry1.add(tryStatement1);
						}
					}
					List<AbstractExpression> unmatchedExpressionsTry2 = new ArrayList<>();
					for(AbstractExpression tryExpression2 : try2.getExpressions()) {
						unmatchedExpressionsTry2.add(tryExpression2);
					}
					operationBodyMapper.processLeaves(unmatchedStatementsTry1, unmatchedExpressionsTry2, parameterToArgumentMap, false);
					Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
					for(CompositeStatementObject catchClause1 : try1.getCatchClauses()) {
						additionallyMatchedStatements1.add(catchClause1);
					}
					Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
					replacementInfo.addReplacement(composite);
					TryWithResourcesRefactoring ref = new TryWithResourcesRefactoring(try1, try2, container1, container2);
					operationBodyMapper.getRefactoringsAfterPostProcessing().add(ref);
					return replacementInfo.getReplacements();
				}
			}
			else if(try1.isTryWithResources() && try2.isTryWithResources()) {
				if((creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) ||
						(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null)) {
					List<AbstractStatement> tryStatements1 = try1.getStatements();
					List<AbstractStatement> tryStatements2 = try2.getStatements();
					List<AbstractCodeFragment> matchedChildStatements1 = new ArrayList<>();
					List<AbstractCodeFragment> matchedChildStatements2 = new ArrayList<>();
					for(AbstractCodeMapping mapping : mappings) {
						if(tryStatements1.contains(mapping.getFragment1()) && tryStatements2.contains(mapping.getFragment2())) {
							matchedChildStatements1.add(mapping.getFragment1());
							matchedChildStatements2.add(mapping.getFragment2());
						}
					}
					if(matchedChildStatements1.size() > 0 && matchedChildStatements1.size() == matchedChildStatements2.size() &&
							(tryStatements1.size() == matchedChildStatements1.size() || tryStatements2.size() == matchedChildStatements2.size())) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation is identical
		if(assignmentInvocationCoveringTheEntireStatement1 != null && assignmentInvocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(invocation1.identical(assignmentInvocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, replacementInfo.getLambdaMappers()) &&
							!assignmentInvocationCoveringTheEntireStatement1.arguments().contains(key1)) {
						if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
								!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
								!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
							return null;
						}
						String expression1 = assignmentInvocationCoveringTheEntireStatement1.getExpression();
						if(expression1 == null || !expression1.contains(key1)) {
							return replacementInfo.getReplacements();
						}
					}
					else if(invocation1.identicalName(assignmentInvocationCoveringTheEntireStatement2) && invocation1.equalArguments(assignmentInvocationCoveringTheEntireStatement2) &&
							(!assignmentInvocationCoveringTheEntireStatement1.arguments().contains(key1) || statement2 instanceof AbstractExpression) && assignmentInvocationCoveringTheEntireStatement2.getExpression() != null) {
						boolean expressionMatched = false;
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<AbstractCodeFragment>();
						Map<VariableDeclaration, AbstractCodeFragment> variableDeclarationsInUnmatchedStatements2 = new LinkedHashMap<VariableDeclaration, AbstractCodeFragment>();
						for(AbstractCodeFragment codeFragment : replacementInfo.getStatements2()) {
							for(VariableDeclaration variableDeclaration : codeFragment.getVariableDeclarations()) {
								variableDeclarationsInUnmatchedStatements2.put(variableDeclaration, codeFragment);
							}
						}
						for(AbstractCodeFragment codeFragment : replacementInfo.getStatements2()) {
							VariableDeclaration variableDeclaration = codeFragment.getVariableDeclaration(assignmentInvocationCoveringTheEntireStatement2.getExpression());
							AbstractCall invocationCoveringEntireCodeFragment = codeFragment.invocationCoveringEntireFragment();
							if(variableDeclaration != null && variableDeclaration.getInitializer() != null) {
								String initializer = variableDeclaration.getInitializer().getString();
								if(invocation1.getExpression() != null && invocation1.getExpression().equals(initializer)) {
									Replacement r = new Replacement(invocation1.getExpression(), variableDeclaration.getVariableName(), ReplacementType.VARIABLE_REPLACED_WITH_EXPRESSION_OF_METHOD_INVOCATION);
									replacementInfo.getReplacements().add(r);
									additionallyMatchedStatements2.add(codeFragment);
									expressionMatched = true;
								}
								else if(invocation1.getExpression() != null) {
									String temp = initializer;
									Set<VariableDeclaration> matchingDeclarations = new LinkedHashSet<>();
									for(VariableDeclaration decl : variableDeclarationsInUnmatchedStatements2.keySet()) {
										if(temp.contains(decl.getVariableName() + ".") && decl.getInitializer() != null) {
											temp = ReplacementUtil.performReplacement(temp, decl.getVariableName(), decl.getInitializer().getString());
											matchingDeclarations.add(decl);
										}
									}
									if(invocation1.getExpression().equals(temp)) {
										expressionMatched = true;
										additionallyMatchedStatements2.add(codeFragment);
										for(VariableDeclaration decl : matchingDeclarations) {
											additionallyMatchedStatements2.add(variableDeclarationsInUnmatchedStatements2.get(decl));
										}
									}
									else if(invocation1.getExpression().startsWith(temp + ".")) {
										additionallyMatchedStatements2.add(codeFragment);
										for(VariableDeclaration decl : matchingDeclarations) {
											additionallyMatchedStatements2.add(variableDeclarationsInUnmatchedStatements2.get(decl));
										}
										for(AbstractCodeFragment codeFragment2 : replacementInfo.getStatements2()) {
											AbstractCall invocationCoveringEntireCodeFragment2 = codeFragment2.invocationCoveringEntireFragment();
											if(invocationCoveringEntireCodeFragment2 != null) {
												String extendedTemp = temp + "." + invocationCoveringEntireCodeFragment2.actualString().substring(
														invocationCoveringEntireCodeFragment2.getExpression() != null ? invocationCoveringEntireCodeFragment2.getExpression().length()+1 : 0);
												if(invocation1.getExpression().startsWith(extendedTemp)) {
													additionallyMatchedStatements2.add(codeFragment2);
													temp = extendedTemp;
												}
											}
										}
										if(invocation1.getExpression().equals(temp)) {
											expressionMatched = true;
										}
									}
								}
							}
							if(invocationCoveringEntireCodeFragment != null && assignmentInvocationCoveringTheEntireStatement1.identicalName(invocationCoveringEntireCodeFragment) &&
									assignmentInvocationCoveringTheEntireStatement1.equalArguments(invocationCoveringEntireCodeFragment)) {
								additionallyMatchedStatements2.add(codeFragment);
							}
						}
						if(classDiff != null) {
							boolean removedAttributeMatched = false;
							for(UMLAttribute removedAttribute : classDiff.getRemovedAttributes()) {
								if(removedAttribute.getName().equals(assignmentInvocationCoveringTheEntireStatement1.getExpression())) {
									removedAttributeMatched = true;
									break;
								}
							}
							boolean addedAttributeMatched = false;
							for(UMLAttribute addedAttribute : classDiff.getAddedAttributes()) {
								if(addedAttribute.getName().equals(assignmentInvocationCoveringTheEntireStatement2.getExpression())) {
									addedAttributeMatched = true;
									break;
								}
							}
							if(removedAttributeMatched && addedAttributeMatched) {
								expressionMatched = true;
								Replacement r = new Replacement(assignmentInvocationCoveringTheEntireStatement1.getExpression(), assignmentInvocationCoveringTheEntireStatement2.getExpression(), ReplacementType.VARIABLE_NAME);
								replacementInfo.addReplacement(r);
							}
						}
						for(AbstractCodeMapping mapping : mappings) {
							for(Replacement r : mapping.getReplacements()) {
								if(r.getBefore().equals(assignmentInvocationCoveringTheEntireStatement1.getExpression()) &&
										r.getAfter().equals(assignmentInvocationCoveringTheEntireStatement2.getExpression())) {
									expressionMatched = true;
									replacementInfo.addReplacement(r);
									break;
								}
							}
							if(expressionMatched) {
								break;
							}
						}
						if(expressionMatched) {
							if(additionallyMatchedStatements2.size() > 0) {
								Replacement r = new CompositeReplacement(statement1.getString(), statement2.getString(), new LinkedHashSet<AbstractCodeFragment>(), additionallyMatchedStatements2);
								replacementInfo.getReplacements().add(r);
							}
							return replacementInfo.getReplacements();
						}
					}
				}
			}
		}
		//method invocation is identical with a difference in the expression call chain
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1 instanceof OperationInvocation && invocationCoveringTheEntireStatement2 instanceof OperationInvocation) {
			if(((OperationInvocation)invocationCoveringTheEntireStatement1).identicalWithExpressionCallChainDifference((OperationInvocation)invocationCoveringTheEntireStatement2)) {
				List<? extends AbstractCall> invokedOperationsBefore = methodInvocationMap1.get(invocationCoveringTheEntireStatement1.getExpression());
				List<? extends AbstractCall> invokedOperationsAfter = methodInvocationMap2.get(invocationCoveringTheEntireStatement2.getExpression());
				if(invokedOperationsBefore != null && invokedOperationsBefore.size() > 0 && invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
					AbstractCall invokedOperationBefore = invokedOperationsBefore.get(0);
					AbstractCall invokedOperationAfter = invokedOperationsAfter.get(0);
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION_EXPRESSION);
					replacementInfo.addReplacement(replacement);
					boolean skipCompositeReplacementCheck = false;
					if(variableDeclarations1.size() > 0 && variableDeclarations1.get(0).getType() != null && invokedOperationBefore.actualString().startsWith(variableDeclarations1.get(0).getType().getClassType()) &&
							variableDeclarations2.size() > 0 && variableDeclarations2.get(0).getType() != null && invokedOperationAfter.actualString().startsWith(variableDeclarations2.get(0).getType().getClassType())) {
						skipCompositeReplacementCheck = true;
					}
					if(!skipCompositeReplacementCheck) {
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = additionallyMatchedStatements(variableDeclarations2, replacementInfo.getStatements1());
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = additionallyMatchedStatements(variableDeclarations1, replacementInfo.getStatements2());
						if(additionallyMatchedStatements1.size() > 0 || additionallyMatchedStatements2.size() > 0) {
							Replacement r = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
							replacementInfo.getReplacements().add(r);
						}
					}
					return replacementInfo.getReplacements();
				}
				else if(invokedOperationsBefore != null && invokedOperationsBefore.size() > 0) {
					AbstractCall invokedOperationBefore = invokedOperationsBefore.get(0);
					Replacement replacement = new VariableReplacementWithMethodInvocation(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationBefore, Direction.INVOCATION_TO_VARIABLE);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
				else if(invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
					AbstractCall invokedOperationAfter = invokedOperationsAfter.get(0);
					Replacement replacement = new VariableReplacementWithMethodInvocation(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationAfter, Direction.VARIABLE_TO_INVOCATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
				if(((OperationInvocation)invocationCoveringTheEntireStatement1).numberOfSubExpressions() == ((OperationInvocation)invocationCoveringTheEntireStatement2).numberOfSubExpressions() &&
						invocationCoveringTheEntireStatement1.getExpression().contains(".") == invocationCoveringTheEntireStatement2.getExpression().contains(".")) {
					return replacementInfo.getReplacements();
				}
			}
			String expression1 = invocationCoveringTheEntireStatement1.getExpression();
			String expression2 = invocationCoveringTheEntireStatement2.getExpression();
			boolean staticVSNonStatic = (expression1 == null && expression2 != null && container1 != null && container1.getClassName().endsWith("." + expression2)) ||
					(expression1 != null && expression2 == null && container2 != null && container2.getClassName().endsWith("." + expression1));
			if(!staticVSNonStatic && modelDiff != null) {
				for(UMLClass addedClass : modelDiff.getAddedClasses()) {
					if((expression1 == null && expression2 != null && container1 != null && addedClass.getName().endsWith("." + expression2)) ||
							(expression1 != null && expression2 == null && container2 != null && addedClass.getName().endsWith("." + expression1))) {
						staticVSNonStatic = true;
						break;
					}
				}
			}
			boolean additionalCaller = invocationCoveringTheEntireStatement1.actualString().endsWith("." + invocationCoveringTheEntireStatement2.actualString()) ||
					invocationCoveringTheEntireStatement2.actualString().endsWith("." + invocationCoveringTheEntireStatement1.actualString()) ||
					s2.endsWith("." + s1) || s1.endsWith("." + s2);
			if((invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) || invocationCoveringTheEntireStatement1.compatibleName(invocationCoveringTheEntireStatement2)) &&
					(staticVSNonStatic || additionalCaller) && invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.getLambdaMappers())) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation is identical if arguments are replaced
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) &&
				invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ) {
			for(String key : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key)) {
					if(invocation2.arguments().size() > 0 && invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocation2, replacementInfo.getReplacements(), replacementInfo.getLambdaMappers())) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation is identical if expression and one arguments are swapped
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalWithExpressionArgumentSwap(invocationCoveringTheEntireStatement2)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_EXPRESSION_ARGUMENT_SWAPPED);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation is identical if arguments are wrapped or concatenated
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) &&
				invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ) {
			for(String key : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key)) {
					if(invocationCoveringTheEntireStatement1.identicalOrWrappedArguments(invocation2)) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT_WRAPPED);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					if(invocationCoveringTheEntireStatement1.identicalOrConcatenatedArguments(invocation2)) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT_CONCATENATED);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation has been renamed but the expression and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null && statement1.getClass().equals(statement2.getClass()) &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, replacementInfo.getLambdaMappers(),
						matchPairOfRemovedAddedOperationsWithIdenticalBody(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, operationBodyMapper), argumentsWithVariableDeclarationMapping(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, mappings))) {
			boolean variableDeclarationMatch = true;
			if(variableDeclarations1.size() > 0  && variableDeclarations2.size() > 0 && !variableDeclarations1.toString().equals(variableDeclarations2.toString()) && !invocationCoveringTheEntireStatement1.arguments().equals(invocationCoveringTheEntireStatement2.arguments()) &&
					!argumentsWithVariableDeclarationMapping(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, mappings)) {
				variableDeclarationMatch = false;
			}
			if(variableDeclarationMatch) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null && statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType()) && creationMap1.size() == 0 && creationMap2.size() == 0 &&
				methodInvocationMap1.size() == methodInvocationMap2.size() && methodInvocationMap1.size() == 1 && methodInvocations1.size() == methodInvocations2.size() && methodInvocations1.size() == 1) {
			AbstractCall invocation1 = methodInvocationMap1.get(methodInvocations1.iterator().next()).get(0);
			AbstractCall invocation2 = methodInvocationMap2.get(methodInvocations2.iterator().next()).get(0);
			if(invocation1.renamedWithIdenticalExpressionAndArguments(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, replacementInfo.getLambdaMappers(), matchPairOfRemovedAddedOperationsWithIdenticalBody(invocation1, invocation2, operationBodyMapper), argumentsWithVariableDeclarationMapping(invocation1, invocation2, mappings))) {
				Replacement replacement = new MethodInvocationReplacement(invocation1.getName(),
						invocation2.getName(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_NAME);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation has been renamed and the expression is different but the arguments are identical, and the variable declarations are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString()) &&
				invocationCoveringTheEntireStatement1.variableDeclarationInitializersRenamedWithIdenticalArguments(invocationCoveringTheEntireStatement2)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed but the expressions are null and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalArgumentsAndNoExpression(invocationCoveringTheEntireStatement2, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, replacementInfo.getLambdaMappers())) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed (one name contains the other), one expression is null, but the other is not null, and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithDifferentExpressionAndIdenticalArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
			boolean variableDeclarationInitializer1 =  invocationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean variableDeclarationInitializer2 =  invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean logStatement1 = invocationCoveringTheEntireStatement1.isLog();
			boolean logStatement2 = invocationCoveringTheEntireStatement2.isLog();
			boolean setMatch = (invocationCoveringTheEntireStatement1.getName().equals("set") && !invocationCoveringTheEntireStatement2.getName().equals("set")) ||
					(!invocationCoveringTheEntireStatement1.getName().equals("set") && invocationCoveringTheEntireStatement2.getName().equals("set"));
			boolean getMatch = (invocationCoveringTheEntireStatement1.getName().equals("get") && !invocationCoveringTheEntireStatement2.getName().equals("get")) ||
					(!invocationCoveringTheEntireStatement1.getName().equals("get") && invocationCoveringTheEntireStatement2.getName().equals("get"));
			boolean addMatch = (replacementInfo.containsReplacement(ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION) || replacementInfo.containsReplacement(ReplacementType.METHOD_INVOCATION)) &&
					((invocationCoveringTheEntireStatement1.getName().equals("add") && !invocationCoveringTheEntireStatement2.getName().equals("add")) ||
					(!invocationCoveringTheEntireStatement1.getName().equals("add") && invocationCoveringTheEntireStatement2.getName().equals("add")));
			boolean callToAddedOperation = false;
			boolean callToDeletedOperation = false;
			if(classDiff != null) {
				callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), container2) != null;
				callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), container1) != null;
			}
			if(variableDeclarationInitializer1 == variableDeclarationInitializer2 && logStatement1 == logStatement2 && !setMatch && !getMatch && !addMatch && callToAddedOperation == callToDeletedOperation) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation has been renamed (one name contains the other), both expressions are null, and one contains all the arguments of the other
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithNoExpressionAndArgumentIntersection(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap,
						classDiff != null ? classDiff.matchesPairOfRemovedAddedOperations(invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, container1, container2) : false)) {
			boolean callToAddedOperation = false;
			boolean callToDeletedOperation = false;
			if(classDiff != null && !invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				UMLOperation matchedAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), container2);
				if(matchedAddedOperation != null) {
					callToAddedOperation = true;
					for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
						if(removedOperation.getName().equals(matchedAddedOperation.getName())) {
							callToAddedOperation = false;
							break;
						}
					}
				}
				UMLOperation matchedRemovedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), container1);
				if(matchedRemovedOperation != null) {
					callToDeletedOperation = true;
					for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
						if(addedOperation.getName().equals(matchedRemovedOperation.getName())) {
							callToDeletedOperation = false;
							break;
						}
					}
				}
			}
			if(callToAddedOperation == callToDeletedOperation) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//method invocation has been renamed and arguments changed, but the expressions are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndDifferentArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, replacementInfo.getLambdaMappers())) {
			ReplacementType type = invocationCoveringTheEntireStatement1.getName().equals(invocationCoveringTheEntireStatement2.getName()) ? ReplacementType.METHOD_INVOCATION_ARGUMENT : ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, type);
			replacementInfo.addReplacement(replacement);
			if(invocationCoveringTheEntireStatement2.arguments().size() > 0) {
				for(String argument2 : invocationCoveringTheEntireStatement2.arguments()) {
					LeafExpression matchingVariable = null;
					for(LeafExpression variable : statement2.getVariables()) {
						if(argument2.startsWith(variable.getString())) {
							matchingVariable = variable;
							break;
						}
					}
					if(matchingVariable != null) {
						for(AbstractCodeFragment codeFragment : replacementInfo.getStatements2()) {
							if(codeFragment.getString().startsWith(matchingVariable.getString() + "." + "add")) {
								for(String argument1 : invocationCoveringTheEntireStatement1.arguments()) {
									List<LeafExpression> leafExpressions2 = codeFragment.findExpression(argument1);
									if(leafExpressions2.size() == 1) {
										List<LeafExpression> leafExpressions1 = statement1.findExpression(argument1);
										if(leafExpressions1.size() == 1) {
											LeafMapping leafMapping = operationBodyMapper.createLeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), parameterToArgumentMap, equalNumberOfAssertions);
											operationBodyMapper.addMapping(leafMapping);
										}
									}
								}
							}
						}
					}
				}
			}
			return replacementInfo.getReplacements();
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			boolean variableDeclarationMatchedWithNonVariableDeclaration = false;
			if(invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL)) {
				if(invocationCoveringTheEntireStatement1 != null) {
					if(!invocationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL)) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
					else if(!variableDeclarations1.get(0).getVariableName().equals(variableDeclarations2.get(0).getVariableName()) &&
							!variableDeclarations1.get(0).equalType(variableDeclarations2.get(0))) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
				}
				else if(creationCoveringTheEntireStatement1 != null) {
					if(!creationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL)) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
					else if(!variableDeclarations1.get(0).getVariableName().equals(variableDeclarations2.get(0).getVariableName()) &&
							!variableDeclarations1.get(0).equalType(variableDeclarations2.get(0))) {
						variableDeclarationMatchedWithNonVariableDeclaration = true;
					}
				}
			}
			for(String methodInvocation1 : methodInvocations1) {
				for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
					if(operationInvocation1.renamedWithIdenticalExpressionAndDifferentArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, replacementInfo.getLambdaMappers()) &&
							!isExpressionOfAnotherMethodInvocation(operationInvocation1, methodInvocationMap1) &&
							!variableDeclarationMatchedWithNonVariableDeclaration) {
						ReplacementType type = operationInvocation1.getName().equals(invocationCoveringTheEntireStatement2.getName()) ? ReplacementType.METHOD_INVOCATION_ARGUMENT : ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, type);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation has only changes in the arguments (different number of arguments)
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			if(invocationCoveringTheEntireStatement1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				return replacementInfo.getReplacements();
			}
			else if(invocationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(invocationCoveringTheEntireStatement1.inlinedStatementBecomesAdditionalArgument(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.getStatements1())) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation1 : methodInvocations1) {
				for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
					if(operationInvocation1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						return replacementInfo.getReplacements();
					}
					else if(operationInvocation1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					else if(operationInvocation1.inlinedStatementBecomesAdditionalArgument(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.getStatements1())) {
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.getName().equals("add") && invocationCoveringTheEntireStatement2.getName().equals("setWidget") &&
				invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression() != null &&
				invocationCoveringTheEntireStatement1.argumentIntersection(invocationCoveringTheEntireStatement2).size() > 0) {
			if(invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			for(Replacement r : replacementInfo.getReplacements()) {
				if(invocationCoveringTheEntireStatement1.getExpression().equals(r.getBefore()) && invocationCoveringTheEntireStatement2.getExpression().equals(r.getAfter())) {
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null && modelDiff != null &&
				invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression() != null &&
				invocationCoveringTheEntireStatement1.getExpression().toLowerCase().contains("log") && invocationCoveringTheEntireStatement2.getExpression().toLowerCase().contains("log")) {
			//check for added inner class
			UMLClass addedInnerClass = null;
			for(UMLClass c : modelDiff.getAddedClasses()) {
				if(c.getName().startsWith(container1.getClassName() + ".") && c.getName().startsWith(container2.getClassName() + ".")) {
					addedInnerClass = c;
					break;
				}
			}
			if(addedInnerClass != null) {
				for(UMLOperation operation : addedInnerClass.getOperations()) {
					if(invocationCoveringTheEntireStatement2.matchesOperation(operation, container2, classDiff, modelDiff)) {
						String[] tokens = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(invocationCoveringTheEntireStatement2.getName());
						boolean match = false;
						for(String argument1 : invocationCoveringTheEntireStatement1.arguments()) {
							String arg = argument1.toLowerCase();
							int matchingTokens = 0;
							for(String token : tokens) {
								if(arg.contains(token.toLowerCase())) {
									matchingTokens++;
								}
							}
							int matchingArguments = 0;
							for(String arg2 : invocationCoveringTheEntireStatement2.arguments()) {
								if(argument1.contains(arg2) || invocationCoveringTheEntireStatement1.arguments().contains(arg2)) {
									matchingArguments++;
								}
								else if(arg2.contains(".")) {
									String prefix = arg2.substring(0, arg2.indexOf("."));
									if(argument1.contains(prefix) || invocationCoveringTheEntireStatement1.arguments().contains(prefix)) {
										matchingArguments++;
									}
								}
							}
							if(tokens.length == matchingTokens && matchingArguments == invocationCoveringTheEntireStatement2.arguments().size()) {
								match = true;
								break;
							}
						}
						if(match) {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
									invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
							return replacementInfo.getReplacements();
						}
					}
				}
			}
			else {
				String[] tokens = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(invocationCoveringTheEntireStatement2.getName());
				boolean match = false;
				for(String argument1 : invocationCoveringTheEntireStatement1.arguments()) {
					String arg = argument1.toLowerCase();
					int matchingTokens = 0;
					for(String token : tokens) {
						if(arg.contains(token.toLowerCase())) {
							matchingTokens++;
						}
					}
					int matchingArguments = 0;
					for(String arg2 : invocationCoveringTheEntireStatement2.arguments()) {
						if(argument1.contains(arg2) || invocationCoveringTheEntireStatement1.arguments().contains(arg2)) {
							matchingArguments++;
						}
						else if(arg2.contains(".")) {
							String prefix = arg2.substring(0, arg2.indexOf("."));
							if(argument1.contains(prefix) || invocationCoveringTheEntireStatement1.arguments().contains(prefix)) {
								matchingArguments++;
							}
						}
					}
					if(tokens.length == matchingTokens && matchingArguments == invocationCoveringTheEntireStatement2.arguments().size()) {
						match = true;
						break;
					}
				}
				if(match) {
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		//check if the argument of the method call in the first statement is returned in the second statement
		Replacement r;
		if(invocationCoveringTheEntireStatement1 != null && (r = invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
			return replacementInfo.getReplacements();
		}
		else if(invocationCoveringTheEntireStatement1 != null && (r = invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(statement2.getString())) != null) {
			replacementInfo.addReplacement(r);
			addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
			return replacementInfo.getReplacements();
		}
		for(String methodInvocation1 : methodInvocations1) {
			for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
				if(statement1.getString().endsWith(methodInvocation1 + JAVA.STATEMENT_TERMINATION) && (r = operationInvocation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
					if(operationInvocation1.makeReplacementForReturnedArgument(statement2.getString()) != null) {
						replacementInfo.addReplacement(r);
						addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the second statement is returned in the first statement
		if(invocationCoveringTheEntireStatement2 != null && (r = invocationCoveringTheEntireStatement2.makeReplacementForWrappedCall(replacementInfo.getArgumentizedString1())) != null) {
			replacementInfo.addReplacement(r);
			addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
			return replacementInfo.getReplacements();
		}
		if(invocationCoveringTheEntireStatement2 != null && (r = invocationCoveringTheEntireStatement2.makeReplacementForWrappedLambda(replacementInfo.getArgumentizedString1())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String methodInvocation2 : methodInvocations2) {
			for(AbstractCall operationInvocation2 : methodInvocationMap2.get(methodInvocation2)) {
				if(statement2.getString().endsWith(methodInvocation2 + JAVA.STATEMENT_TERMINATION) && (r = operationInvocation2.makeReplacementForWrappedCall(replacementInfo.getArgumentizedString1())) != null) {
					if(operationInvocation2.makeReplacementForWrappedCall(statement1.getString()) != null) {
						replacementInfo.addReplacement(r);
						addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the second statement is the right hand side of an assignment in the first statement
		if(invocationCoveringTheEntireStatement2 != null &&
				(r = invocationCoveringTheEntireStatement2.makeReplacementForAssignedArgument(replacementInfo.getArgumentizedString1())) != null &&
				(methodInvocationMap1.containsKey(r.getBefore()) || methodInvocationMap1.containsKey(r.getAfter()))) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		//check if the expression of the method call in the second statement is the right hand side of an assignment in the first statement
		if(invocationCoveringTheEntireStatement2 != null && variableDeclarations1.toString().equals(variableDeclarations2.toString()) && variableDeclarations1.size() > 0 &&
				(r = invocationCoveringTheEntireStatement2.makeReplacementForAssignedExpression(replacementInfo.getArgumentizedString1())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		//check if the method call in the second statement is the expression (or sub-expression) of the method invocation in the first statement
		if(invocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + JAVA.STATEMENT_TERMINATION)) {
						if(methodInvocationMap2.keySet().contains(invocation1.getExpression())) {
							Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
									invocationCoveringTheEntireStatement2.actualString(), invocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
							if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
									invocationCoveringTheEntireStatement2.arguments().contains(invocation1.getExpression()) &&
									!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
									!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
								return null;
							}
							return replacementInfo.getReplacements();
						}
						if(invocation1 instanceof OperationInvocation) {
							Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<AbstractCodeFragment>();
							for(String subExpression1 : ((OperationInvocation)invocation1).getSubExpressions()) {
								if(methodInvocationMap2.keySet().contains(subExpression1)) {
									AbstractCall subOperationInvocation1 = null;
									for(String key : methodInvocationMap1.keySet()) {
										if(key.endsWith(subExpression1)) {
											subOperationInvocation1 = methodInvocationMap1.get(key).get(0);
											break;
										}
									}
									if(additionallyMatchedStatements2.size() > 0) {
										CompositeReplacement composite = new CompositeReplacement(subExpression1,
												invocationCoveringTheEntireStatement2.actualString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
										replacementInfo.addReplacement(composite);
									}
									Replacement replacement = new MethodInvocationReplacement(subExpression1,
											invocationCoveringTheEntireStatement2.actualString(), subOperationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
									replacementInfo.addReplacement(replacement);
									return replacementInfo.getReplacements();
								}
								for(AbstractCodeFragment codeFragment : replacementInfo.getStatements2()) {
									AbstractCall invocationCoveringEntireCodeFragment = codeFragment.invocationCoveringEntireFragment();
									if(invocationCoveringEntireCodeFragment != null && subExpression1.equals(invocationCoveringEntireCodeFragment.actualString())) {
										additionallyMatchedStatements2.add(codeFragment);
									}
								}
							}
						}
					}
				}
			}
		}
		//check if the method call in the first statement is the expression (or sub-expression) of the method invocation in the second statement
		if(invocationCoveringTheEntireStatement1 != null) {
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(statement2.getString().endsWith(key2 + JAVA.STATEMENT_TERMINATION)) {
						if(methodInvocationMap1.keySet().contains(invocation2.getExpression())) {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
									invocation2.actualString(), invocationCoveringTheEntireStatement1, invocation2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
							if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
									invocationCoveringTheEntireStatement1.arguments().contains(invocation2.getExpression()) &&
									!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
									!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
								return null;
							}
							return replacementInfo.getReplacements();
						}
						if(invocation2 instanceof OperationInvocation) {
							Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
							for(String subExpression2 : ((OperationInvocation)invocation2).getSubExpressions()) {
								if(methodInvocationMap1.keySet().contains(subExpression2)) {
									AbstractCall subOperationInvocation2 = null;
									for(String key : methodInvocationMap2.keySet()) {
										if(key.endsWith(subExpression2)) {
											subOperationInvocation2 = methodInvocationMap2.get(key).get(0);
											break;
										}
									}
									if(additionallyMatchedStatements1.size() > 0) {
										CompositeReplacement composite = new CompositeReplacement(invocationCoveringTheEntireStatement1.actualString(),
												subExpression2, additionallyMatchedStatements1, new LinkedHashSet<>());
										replacementInfo.addReplacement(composite);
									}
									Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
											subExpression2, invocationCoveringTheEntireStatement1, subOperationInvocation2, ReplacementType.METHOD_INVOCATION);
									replacementInfo.addReplacement(replacement);
									return replacementInfo.getReplacements();
								}
								for(AbstractCodeFragment codeFragment : replacementInfo.getStatements1()) {
									AbstractCall invocationCoveringEntireCodeFragment = codeFragment.invocationCoveringEntireFragment();
									if(invocationCoveringEntireCodeFragment != null && subExpression2.equals(invocationCoveringEntireCodeFragment.actualString())) {
										additionallyMatchedStatements1.add(codeFragment);
									}
								}
							}
						}
					}
				}
			}
		}
		//check if the class instance creation in the first statement is the expression of the method invocation in the second statement
		if(creationCoveringTheEntireStatement1 != null) {
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(statement2.getString().endsWith(key2 + JAVA.STATEMENT_TERMINATION) && invocation2.getExpression() != null &&
							invocation2.getExpression().startsWith(creationCoveringTheEntireStatement1.actualString())) {
						Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
								invocation2.getName(), creationCoveringTheEntireStatement1, invocation2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the class instance creation in the second statement is the class instance creation of the first statement
		if(creationCoveringTheEntireStatement2 != null) {
			for(String key1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + JAVA.STATEMENT_TERMINATION) &&
							creationCoveringTheEntireStatement2.arguments().contains(creation1.actualString())) {
						if(variableDeclarations1.size() > 0) {
							VariableDeclaration declaration1 = variableDeclarations1.get(0);
							for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
								for(AbstractCall fragmentCreation1 : fragment1.getCreations()) {
									if(fragmentCreation1.arguments().contains(declaration1.getVariableName()) &&
											creationCoveringTheEntireStatement2.identicalName(fragmentCreation1)) {
										Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
										additionallyMatchedStatements1.add(fragment1);
										Replacement replacement = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
						Replacement replacement = new ObjectCreationReplacement(creation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)creation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//builder call chain in the first statement is replaced with class instance creation in the second statement
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement1.getName().equals("build")) {
			if(creationCoveringTheEntireStatement2 != null) {
				int commonArguments = 0;
				for(String key1 : methodInvocationMap1.keySet()) {
					if(invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
						for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
							Set<String> argumentIntersection = invocation1.argumentIntersection(creationCoveringTheEntireStatement2);
							commonArguments += argumentIntersection.size();
						}
					}
				}
				if(commonArguments > 0) {
					Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
							creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement2 != null) {
				int commonArguments = 0;
				Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<AbstractCodeFragment>(); 
				for(String key1 : methodInvocationMap1.keySet()) {
					if(invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
						for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
							if(invocation1.equalArguments(invocationCoveringTheEntireStatement2)) {
								commonArguments += invocation1.arguments().size();
							}
							else {
								Set<String> argumentIntersection = invocation1.argumentIntersection(invocationCoveringTheEntireStatement2);
								int threshold = Math.max(invocation1.arguments().size(), invocationCoveringTheEntireStatement2.arguments().size())/2;
								if(argumentIntersection.size() > threshold) {
									commonArguments += argumentIntersection.size();
								}
							}
							for(AbstractCodeFragment codeFragment : replacementInfo.getStatements2()) { 
								AbstractCall invocation = codeFragment.invocationCoveringEntireFragment(); 
								if(invocation != null) { 
									if(invocation.identical(invocation1, replacementInfo.getReplacements(), parameterToArgumentMap, replacementInfo.getLambdaMappers())) { 
										additionallyMatchedStatements2.add(codeFragment); 
									} 
									if((invocation.getExpression() != null && invocation.getExpression().equals(invocation1.actualString())) ||
											invocation.callChainIntersection(invocation1).size() > 0) {
										additionallyMatchedStatements2.add(codeFragment); 
									} 
								} 
							}
						}
					}
				}
				if(commonArguments > 0) {
					if(additionallyMatchedStatements2.size() > 0) { 
						Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), new LinkedHashSet<AbstractCodeFragment>(), additionallyMatchedStatements2); 
						replacementInfo.addReplacement(composite); 
					}
					else {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(),
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
					}
					return replacementInfo.getReplacements();
				}
			}
		}
		//class instance creation in the first statement is replaced with builder call chain in the second statement
		if(invocationCoveringTheEntireStatement2 != null && invocationCoveringTheEntireStatement2.getName().equals("build")) {
			if(creationCoveringTheEntireStatement1 != null) {
				int commonArguments = 0;
				for(String key2 : methodInvocationMap2.keySet()) {
					if(invocationCoveringTheEntireStatement2.actualString().startsWith(key2)) {
						for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
							Set<String> argumentIntersection = invocation2.argumentIntersection(creationCoveringTheEntireStatement1);
							commonArguments += argumentIntersection.size();
						}
					}
				}
				if(commonArguments > 0) {
					Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
							invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement1 != null) {
				int commonArguments = 0;
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<AbstractCodeFragment>();
				for(String key2 : methodInvocationMap2.keySet()) {
					if(invocationCoveringTheEntireStatement2.actualString().startsWith(key2)) {
						for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
							if(invocation2.equalArguments(invocationCoveringTheEntireStatement1)) {
								commonArguments += invocation2.arguments().size();
							}
							else {
								Set<String> argumentIntersection = invocation2.argumentIntersection(invocationCoveringTheEntireStatement1);
								int threshold = Math.max(invocation2.arguments().size(), invocationCoveringTheEntireStatement1.arguments().size())/2;
								if(argumentIntersection.size() > threshold) {
									commonArguments += argumentIntersection.size();
								}
							}
							for(AbstractCodeFragment codeFragment : replacementInfo.getStatements1()) {
								AbstractCall invocation = codeFragment.invocationCoveringEntireFragment();
								if(invocation != null) {
									if(invocation.identical(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, replacementInfo.getLambdaMappers())) {
										additionallyMatchedStatements1.add(codeFragment);
									}
									if((invocation.getExpression() != null && invocation.getExpression().equals(invocation2.actualString())) ||
											invocation.callChainIntersection(invocation2).size() > 0) {
										additionallyMatchedStatements1.add(codeFragment);
									}
								}
							}
						}
					}
				}
				if(commonArguments > 0) {
					if(additionallyMatchedStatements1.size() > 0) {
						Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<AbstractCodeFragment>());
						replacementInfo.addReplacement(composite);
					}
					else {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(),
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
					}
					return replacementInfo.getReplacements();
				}
			}
		}
		//object creation is identical
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.identical(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, replacementInfo.getLambdaMappers())) {
			boolean identicalArrayInitializer = true;
			if(creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray()) {
				identicalArrayInitializer = creationCoveringTheEntireStatement1.identicalArrayInitializer(creationCoveringTheEntireStatement2);
			}
			if(identicalArrayInitializer) {
				String anonymousClassDeclaration1 = creationCoveringTheEntireStatement1.getAnonymousClassDeclaration();
				String anonymousClassDeclaration2 = creationCoveringTheEntireStatement2.getAnonymousClassDeclaration();
				if(anonymousClassDeclaration1 != null && anonymousClassDeclaration2 != null && !anonymousClassDeclaration1.equals(anonymousClassDeclaration2)) {
					Replacement replacement = new Replacement(anonymousClassDeclaration1, anonymousClassDeclaration2, ReplacementType.ANONYMOUS_CLASS_DECLARATION);
					replacementInfo.addReplacement(replacement);
				}
				return replacementInfo.getReplacements();
			}
		}
		//object creation has identical arguments, but different type
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.arguments().size() > 0 && creationCoveringTheEntireStatement1.equalArguments(creationCoveringTheEntireStatement2)) {
			Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.getName(),
					creationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.arguments().size() > 0 && creationCoveringTheEntireStatement1.getType().equalsWithSubType(creationCoveringTheEntireStatement2.getType()) &&
				creationCoveringTheEntireStatement1.getType().getClassType().endsWith("Exception") &&
				creationCoveringTheEntireStatement2.getType().getClassType().endsWith("Exception")) {
			Set<String> argumentIntersection = creationCoveringTheEntireStatement1.argumentIntersection(creationCoveringTheEntireStatement2);
			if(argumentIntersection.size() > 0 && argumentIntersection.size() == Math.min(creationCoveringTheEntireStatement1.arguments().size(), creationCoveringTheEntireStatement2.arguments().size())) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement1.getType().equals(creationCoveringTheEntireStatement2.getType())) {
				CompositeStatementObject parent1 = statement1.getParent();
				CompositeStatementObject parent2 = statement2.getParent();
				while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent1 = parent1.getParent(); 
				}
				while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
					parent2 = parent2.getParent(); 
				}
				if(parent1 != null && parent2 != null && parent1.getString().equals(parent2.getString())) {
					Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
							creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		//object creation has only changes in the arguments
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
			if(creationCoveringTheEntireStatement1.identicalWithMergedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement1.reorderedArguments(creationCoveringTheEntireStatement2)) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement1.inlinedStatementBecomesAdditionalArgument(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.getStatements1())) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.actualString(),
						creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//check if the argument lists are identical after replacements
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.identicalName(creationCoveringTheEntireStatement2) &&
				creationCoveringTheEntireStatement1.identicalExpression(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) &&
				!creationCoveringTheEntireStatement1.allArgumentsReplaced(creationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
			if(creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains("[") && s2.contains("[") &&
					s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).equals(s2.substring(s2.indexOf("[")+1, s2.lastIndexOf("]"))) &&
					s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).length() > 0) {
				return replacementInfo.getReplacements();
			}
			if(!creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains("(") && s2.contains("(") &&
					s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).equals(s2.substring(s2.indexOf("(")+1, s2.lastIndexOf(")"))) &&
					s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).length() > 0) {
				return replacementInfo.getReplacements();
			}
		}
		//check if array creation is replaced with data structure creation
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
			VariableDeclaration v1 = variableDeclarations1.get(0);
			VariableDeclaration v2 = variableDeclarations2.get(0);
			String initializer1 = v1.getInitializer() != null ? v1.getInitializer().getString() : null;
			String initializer2 = v2.getInitializer() != null ? v2.getInitializer().getString() : null;
			UMLType v1Type = v1.getType();
			UMLType v2Type = v2.getType();
			if(v1Type != null && v2Type != null) {
				if(v1Type.getArrayDimension() == 1 && v2Type.containsTypeArgument(v1Type.getClassType()) &&
						creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() &&
						initializer1 != null && initializer2 != null &&
						initializer1.substring(initializer1.indexOf("[")+1, initializer1.lastIndexOf("]")).equals(initializer2.substring(initializer2.indexOf("(")+1, initializer2.lastIndexOf(")")))) {
					r = new ObjectCreationReplacement(initializer1, initializer2,
							creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
				if(v2Type.getArrayDimension() == 1 && v1Type.containsTypeArgument(v2Type.getClassType()) &&
						!creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() &&
						initializer1 != null && initializer2 != null &&
						initializer1.substring(initializer1.indexOf("(")+1, initializer1.lastIndexOf(")")).equals(initializer2.substring(initializer2.indexOf("[")+1, initializer2.lastIndexOf("]")))) {
					r = new ObjectCreationReplacement(initializer1, initializer2,
							creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
		}
		if(!creations1.isEmpty() && creationCoveringTheEntireStatement2 != null) {
			for(String creation1 : creations1) {
				for(AbstractCall objectCreation1 : creationMap1.get(creation1)) {
					if(objectCreation1.identicalWithMergedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						return replacementInfo.getReplacements();
					}
					else if(objectCreation1.reorderedArguments(creationCoveringTheEntireStatement2)) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					else if(objectCreation1.identicalWithDifferentNumberOfArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					else if(objectCreation1.inlinedStatementBecomesAdditionalArgument(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.getStatements1())) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.actualString(),
								creationCoveringTheEntireStatement2.actualString(), (ObjectCreation)objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					//check if the argument lists are identical after replacements
					if(objectCreation1.identicalName(creationCoveringTheEntireStatement2) &&
							objectCreation1.identicalExpression(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						if(((ObjectCreation)objectCreation1).isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains("[") && s2.contains("[") &&
								s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).equals(s2.substring(s2.indexOf("[")+1, s2.lastIndexOf("]"))) &&
								s1.substring(s1.indexOf("[")+1, s1.lastIndexOf("]")).length() > 0) {
							return replacementInfo.getReplacements();
						}
						if(!((ObjectCreation)objectCreation1).isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains("(") && s2.contains("(") &&
								s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).equals(s2.substring(s2.indexOf("(")+1, s2.lastIndexOf(")"))) &&
								s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")")).length() > 0) {
							return replacementInfo.getReplacements();
						}
					}
				}
			}
		}
		if(creationCoveringTheEntireStatement1 != null && (r = creationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String creation1 : creations1) {
			for(AbstractCall objectCreation1 : creationMap1.get(creation1)) {
				if(statement1.getString().endsWith(creation1 + JAVA.STATEMENT_TERMINATION) && (r = objectCreation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
		}
		if(variableDeclarationWithArrayInitializer1 != null && invocationCoveringTheEntireStatement2 != null && !(invocationCoveringTheEntireStatement2 instanceof MethodReference) && variableDeclarations2.isEmpty() &&
				!containsMethodSignatureOfAnonymousClass(statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
			String args1 = s1.substring(s1.indexOf(JAVA.OPEN_ARRAY_INITIALIZER)+1, s1.lastIndexOf(JAVA.CLOSE_ARRAY_INITIALIZER));
			String args2 = s2.substring(s2.indexOf("(")+1, s2.lastIndexOf(")"));
			if(args1.equals(args2)) {
				r = new Replacement(args1, args2, ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		if(variableDeclarationWithArrayInitializer2 != null && invocationCoveringTheEntireStatement1 != null && !(invocationCoveringTheEntireStatement1 instanceof MethodReference) && variableDeclarations1.isEmpty() &&
				!containsMethodSignatureOfAnonymousClass(statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
			String args1 = s1.substring(s1.indexOf("(")+1, s1.lastIndexOf(")"));
			String args2 = s2.substring(s2.indexOf(JAVA.OPEN_ARRAY_INITIALIZER)+1, s2.lastIndexOf(JAVA.CLOSE_ARRAY_INITIALIZER));
			if(args1.equals(args2)) {
				r = new Replacement(args1, args2, ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		List<TernaryOperatorExpression> ternaryOperatorExpressions1 = statement1.getTernaryOperatorExpressions();
		List<TernaryOperatorExpression> ternaryOperatorExpressions2 = statement2.getTernaryOperatorExpressions();
		if(ternaryOperatorExpressions1.isEmpty() && ternaryOperatorExpressions2.size() == 1) {
			TernaryOperatorExpression ternary = ternaryOperatorExpressions2.get(0);
			for(String creation : creationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(creation, parameterToArgumentMap)) != null) {
					replacementInfo.addReplacement(r);
					addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
					return replacementInfo.getReplacements();
				}
			}
			for(String methodInvocation : methodInvocationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(methodInvocation, parameterToArgumentMap)) != null) {
					replacementInfo.addReplacement(r);
					addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement1 != null && (r = ternary.makeReplacementWithTernaryOnTheRight(invocationCoveringTheEntireStatement1.actualString(), parameterToArgumentMap)) != null) {
				replacementInfo.addReplacement(r);
				addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement1 != null && (r = ternary.makeReplacementWithTernaryOnTheRight(creationCoveringTheEntireStatement1.actualString(), parameterToArgumentMap)) != null) {
				replacementInfo.addReplacement(r);
				addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
				return replacementInfo.getReplacements();
			}
			for(String creation2 : creations2) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(creation2, parameterToArgumentMap)) != null) {
					for(AbstractCall c2 : creationMap2.get(creation2)) {
						for(String creation1 : creations1) {
							for(AbstractCall c1 : creationMap1.get(creation1)) {
								if(((ObjectCreation)c1).getType().compatibleTypes(((ObjectCreation)c2).getType()) && c1.equalArguments(c2)) {
									replacementInfo.addReplacement(r);
									addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		if(ternaryOperatorExpressions1.size() == 1 && ternaryOperatorExpressions2.isEmpty()) {
			TernaryOperatorExpression ternary = ternaryOperatorExpressions1.get(0);
			for(String creation : creationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(creation, parameterToArgumentMap)) != null) {
					replacementInfo.addReplacement(r);
					addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
					return replacementInfo.getReplacements();
				}
			}
			for(String methodInvocation : methodInvocationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(methodInvocation, parameterToArgumentMap)) != null) {
					replacementInfo.addReplacement(r);
					addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement2 != null && (r = ternary.makeReplacementWithTernaryOnTheLeft(invocationCoveringTheEntireStatement2.actualString(), parameterToArgumentMap)) != null) {
				replacementInfo.addReplacement(r);
				addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement2 != null && (r = ternary.makeReplacementWithTernaryOnTheLeft(creationCoveringTheEntireStatement2.actualString(), parameterToArgumentMap)) != null) {
				replacementInfo.addReplacement(r);
				addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
				return replacementInfo.getReplacements();
			}
			for(String creation1 : creations1) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(creation1, parameterToArgumentMap)) != null) {
					for(AbstractCall c1 : creationMap1.get(creation1)) {
						for(String creation2 : creations2) {
							for(AbstractCall c2 : creationMap2.get(creation2)) {
								if(((ObjectCreation)c1).getType().compatibleTypes(((ObjectCreation)c2).getType()) && c1.equalArguments(c2)) {
									replacementInfo.addReplacement(r);
									addLeafMappings(statement1, statement2, r, replacementInfo, container1, container2);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement2 != null && statement2.getString().equals(invocationCoveringTheEntireStatement2.actualString() + JAVA.STATEMENT_TERMINATION) &&
				invocationCoveringTheEntireStatement2.arguments().size() == 1 && statement1.getString().endsWith(JAVA.ASSIGNMENT + invocationCoveringTheEntireStatement2.arguments().get(0) + JAVA.STATEMENT_TERMINATION) &&
				invocationCoveringTheEntireStatement2.expressionIsNullOrThis() && invocationCoveringTheEntireStatement2.getName().startsWith("set")) {
			String prefix1 = statement1.getString().substring(0, statement1.getString().lastIndexOf(JAVA.ASSIGNMENT));
			if(variables1.contains(prefix1)) {
				String before = prefix1 + JAVA.ASSIGNMENT + invocationCoveringTheEntireStatement2.arguments().get(0);
				String after = invocationCoveringTheEntireStatement2.actualString();
				r = new Replacement(before, after, ReplacementType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER_METHOD_INVOCATION);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				(variableDeclarations1.size() == variableDeclarations2.size() || (variableDeclarations1.size() > 0 && statement2.getString().startsWith(JAVA.RETURN_SPACE)))) {
			if(invocationCoveringTheEntireStatement2.getName().equals("of") && variableDeclarations1.size() > 0) {
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				for(String argument2 : invocationCoveringTheEntireStatement2.arguments()) {
					for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
						AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
						if(invocation1 != null && invocation1.getExpression() != null && invocation1.getExpression().equals(variableDeclarations1.get(0).getVariableName())) {
							boolean argumentMatched = false;
							for(String argument1 : invocation1.arguments()) {
								if(argument1.equals(argument2)) {
									List<LeafExpression> leafExpressions1 = fragment1.findExpression(argument1);
									List<LeafExpression> leafExpressions2 = statement2.findExpression(argument2);
									if(leafExpressions1.size() == 1 && leafExpressions2.size() == 1) {
										LeafMapping mapping = operationBodyMapper.createLeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), parameterToArgumentMap, equalNumberOfAssertions);
										operationBodyMapper.addMapping(mapping);
										additionallyMatchedStatements1.add(fragment1);
									}
									argumentMatched = true;
									break;
								}
							}
							if(argumentMatched) {
								break;
							}
						}
					}
				}
				if(additionallyMatchedStatements1.size() > 0) {
					return replacementInfo.getReplacements();
				}
			}
			if(creationCoveringTheEntireStatement1.equalArguments(invocationCoveringTheEntireStatement2) && creationCoveringTheEntireStatement1.arguments().size() > 0) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(invocationCoveringTheEntireStatement2.arguments().size() == 1 && invocationCoveringTheEntireStatement2.arguments().contains(creationCoveringTheEntireStatement1.actualString())) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_WRAPPED_IN_METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(creationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null) {
			for(String key1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + JAVA.STATEMENT_TERMINATION)) {
						for(String key2 : methodInvocationMap2.keySet()) {
							for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
								if(statement2.getString().endsWith(key2 + JAVA.STATEMENT_TERMINATION)) {
									if(invocation2.getName().equals("of")) {
										String assignedVariable = null;
										if(assignmentCreationCoveringTheEntireStatement1 != null) {
											assignedVariable = statement1.getString().substring(0, statement1.getString().indexOf(JAVA.ASSIGNMENT));
										}
										Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
										for(String argument2 : invocation2.arguments()) {
											for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
												AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
												if(invocation1 != null && invocation1.getExpression() != null && invocation1.getExpression().equals(assignedVariable)) {
													boolean argumentMatched = false;
													for(String argument1 : invocation1.arguments()) {
														if(argument1.equals(argument2)) {
															List<LeafExpression> leafExpressions1 = fragment1.findExpression(argument1);
															List<LeafExpression> leafExpressions2 = statement2.findExpression(argument2);
															if(leafExpressions1.size() == 1 && leafExpressions2.size() == 1) {
																LeafMapping mapping = operationBodyMapper.createLeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), parameterToArgumentMap, equalNumberOfAssertions);
																operationBodyMapper.addMapping(mapping);
																additionallyMatchedStatements1.add(fragment1);
															}
															argumentMatched = true;
															break;
														}
													}
													if(argumentMatched) {
														break;
													}
												}
											}
										}
										if(additionallyMatchedStatements1.size() > 0) {
											return replacementInfo.getReplacements();
										}
									}
									if(creation1.equalArguments(invocation2) && creation1.arguments().size() > 0) {
										Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.getName(),
												invocation2.getName(), (ObjectCreation)creation1, invocation2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
									else if(invocation2.arguments().size() == 1 && invocation2.arguments().contains(creation1.actualString())) {
										Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.getName(),
												invocation2.getName(), (ObjectCreation)creation1, invocation2, ReplacementType.CLASS_INSTANCE_CREATION_WRAPPED_IN_METHOD_INVOCATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() == variableDeclarations2.size()) {
			if(invocationCoveringTheEntireStatement1.equalArguments(creationCoveringTheEntireStatement2) && invocationCoveringTheEntireStatement1.arguments().size() > 0) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement2.arguments().size() == 1 && creationCoveringTheEntireStatement2.arguments().contains(invocationCoveringTheEntireStatement1.actualString())) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(invocationCoveringTheEntireStatement1 == null && creationCoveringTheEntireStatement2 == null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + JAVA.STATEMENT_TERMINATION)) {
						for(String key2 : creationMap2.keySet()) {
							for(AbstractCall creation2 : creationMap2.get(key2)) {
								if(statement2.getString().endsWith(key2 + JAVA.STATEMENT_TERMINATION)) {
									if(invocation1.equalArguments(creation2) && invocation1.arguments().size() > 0) {
										Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocation1.getName(),
												creation2.getName(), invocation1, (ObjectCreation)creation2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
									else if(creation2.arguments().size() == 1 && creation2.arguments().contains(invocation1.actualString())) {
										Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocation1.getName(),
												creation2.getName(), invocation1, (ObjectCreation)creation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_CLASS_INSTANCE_CREATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			if(creationCoveringTheEntireStatement1.arguments().size() > 1 && creationCoveringTheEntireStatement1.argumentIntersection(invocationCoveringTheEntireStatement2).size() > 0 &&
					creationCoveringTheEntireStatement1.getCoverage().equals(invocationCoveringTheEntireStatement2.getCoverage()) &&
					creationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.getLambdaMappers())) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
			if(invocationCoveringTheEntireStatement1.arguments().size() > 1 && invocationCoveringTheEntireStatement1.argumentIntersection(creationCoveringTheEntireStatement2).size() > 0 &&
					invocationCoveringTheEntireStatement1.getCoverage().equals(creationCoveringTheEntireStatement2.getCoverage()) &&
					invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), replacementInfo.getLambdaMappers())) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 instanceof OperationInvocation && invocationCoveringTheEntireStatement2 instanceof MethodReference) {
			if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_METHOD_REFERENCE);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(invocationCoveringTheEntireStatement1 instanceof MethodReference && invocationCoveringTheEntireStatement2 instanceof OperationInvocation) {
			if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_METHOD_REFERENCE);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(classDiff != null && statement1.assignmentInvocationCoveringEntireStatement() != null && statement2.assignmentInvocationCoveringEntireStatement() != null) {
			String assignedVariable1 = statement1.getString().substring(0, statement1.getString().indexOf(JAVA.ASSIGNMENT));
			String assignedVariable2 = statement2.getString().substring(0, statement2.getString().indexOf(JAVA.ASSIGNMENT));
			if(assignedVariable1.equals(assignedVariable2)) {
				for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
					if(assignmentInvocationCoveringTheEntireStatement1.matchesOperation(removedOperation, container1, classDiff, modelDiff)) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if replacement is wrapped in creation within the first statement
		if(replacementInfo.getReplacements().size() == 1 && creations1.size() > 0) {
			for(String objectCreation1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(objectCreation1)) {
					for(Replacement replacement : replacementInfo.getReplacements()) {
						if(creation1.arguments().contains(replacement.getBefore())) {
							String creationAfterReplacement = ReplacementUtil.performArgumentReplacement(creation1.actualString(), replacement.getBefore(), replacement.getAfter());
							String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), creationAfterReplacement, replacement.getAfter());
							int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), replacementInfo.getRawDistance());
							if(distanceRaw == 0) {
								if(replacement instanceof MethodInvocationReplacement) {
									MethodInvocationReplacement methodInvocationReplacement = (MethodInvocationReplacement)replacement;
									AbstractCall invokedOperationAfter = methodInvocationReplacement.getInvokedOperationAfter();
									r = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.actualString(), invokedOperationAfter.actualString(),
											(ObjectCreation)creation1, invokedOperationAfter, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
									replacementInfo.addReplacement(r);
									return replacementInfo.getReplacements();
								}
								else if(replacement instanceof VariableReplacementWithMethodInvocation) {
									VariableReplacementWithMethodInvocation methodInvocationReplacement = (VariableReplacementWithMethodInvocation)replacement;
									if(methodInvocationReplacement.getDirection().equals(Direction.VARIABLE_TO_INVOCATION)) {
										AbstractCall invokedOperationAfter = methodInvocationReplacement.getInvokedOperation();
										r = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.actualString(), invokedOperationAfter.actualString(),
												(ObjectCreation)creation1, invokedOperationAfter, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
										replacementInfo.addReplacement(r);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(variableDeclarations1.size() > 0  && variableDeclarations2.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString()) &&
				invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null && parentMapper == null) {
			int variableDeclarationsInOtherStatements1 = 0;
			for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
				for(VariableDeclaration variableDeclaration : fragment1.getVariableDeclarations()) {
					if(variableDeclarations1.get(0).getScope().overlaps(variableDeclaration.getScope())) {
						variableDeclarationsInOtherStatements1++;
					}
				}
			}
			int variableDeclarationsInOtherStatements2 = 0;
			for(AbstractCodeFragment fragment2 : replacementInfo.getStatements2()) {
				for(VariableDeclaration variableDeclaration : fragment2.getVariableDeclarations()) {
					if(variableDeclarations2.get(0).getScope().overlaps(variableDeclaration.getScope())) {
						variableDeclarationsInOtherStatements2++;
					}
				}
			}
			if(variableDeclarationsInOtherStatements1 == 0 && variableDeclarationsInOtherStatements2 == 0) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(), invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			boolean variableDeclarationInitializer1 =  invocationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean variableDeclarationInitializer2 =  invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			if(variableDeclarationInitializer1 && variableDeclarationInitializer2) {
				if(variableDeclarations1.toString().equals(variableDeclarations2.toString()) && variableDeclarations1.size() > 0) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					boolean statementIsExtracted = false;
					if(classDiff != null) {
						UMLOperation addedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), container2);
						statementIsExtracted = checkIfStatementIsExtracted(statement1, statement2, addedOperation, operationBodyMapper);
						callToAddedOperation = addedOperation != null;
						callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), container1) != null;
					}
					if(callToAddedOperation != callToDeletedOperation && !statementIsExtracted) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
				else if(variableDeclarations1.get(0).getVariableName().equals(variableDeclarations2.get(0).getVariableName()) && replacementInfo.getReplacements(ReplacementType.TYPE).size() > 0) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					if(classDiff != null) {
						boolean superCall2 = invocationCoveringTheEntireStatement2.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression().equals("super");
						if(!superCall2) {
							callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getNextClass().getOperations(), container2) != null;
						}
						boolean superCall1 = invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement1.getExpression().equals("super");
						if(!superCall1) {
							callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getOriginalClass().getOperations(), container1) != null;
						}
					}
					if(callToAddedOperation != callToDeletedOperation) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		else if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			boolean variableDeclarationInitializer1 =  creationCoveringTheEntireStatement1.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			boolean variableDeclarationInitializer2 =  invocationCoveringTheEntireStatement2.getCoverage().equals(StatementCoverageType.VARIABLE_DECLARATION_INITIALIZER_CALL);
			if(variableDeclarationInitializer1 && variableDeclarationInitializer2) {
				if(variableDeclarations1.toString().equals(variableDeclarations2.toString()) && variableDeclarations1.size() > 0) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					if(classDiff != null) {
						boolean superCall2 = invocationCoveringTheEntireStatement2.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression().equals("super");
						if(!superCall2) {
							callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getNextClass().getOperations(), container2) != null;
						}
					}
					if(callToAddedOperation != callToDeletedOperation) {
						Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		else if(invocationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null && methodInvocationMap2.size() > 0) {
			if(variableDeclarations1.toString().equals(variableDeclarations2.toString()) && variableDeclarations1.size() > 0 &&
					variableDeclarations1.get(0).getInitializer() != null && variableDeclarations2.get(0).getInitializer() != null) {
				boolean callToAddedOperation = false;
				boolean statementIsExtracted = false;
				if(classDiff != null) {
					for(String key : methodInvocationMap2.keySet()) {
						AbstractCall call = methodInvocationMap2.get(key).get(0);
						UMLOperation addedOperation = classDiff.matchesOperation(call, classDiff.getAddedOperations(), container2);
						statementIsExtracted = checkIfStatementIsExtracted(statement1, statement2, addedOperation, operationBodyMapper);
						callToAddedOperation = addedOperation != null;
						if(callToAddedOperation) {
							break;
						}
					}
				}
				if(callToAddedOperation && !statementIsExtracted) {
					Replacement replacement = new InitializerReplacement(variableDeclarations1.get(0).getInitializer(), variableDeclarations2.get(0).getInitializer());
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
		}
		boolean variableReturnQualified1 = false;
		if(variableReturn1 && statement1.getVariables().get(0).getString().contains(".")) {
			variableReturnQualified1 = true;
		}
		boolean variableReturnAsLastStatement1 = variableReturn1 && statement1.isLastStatement();
		boolean variableReturnQualified2 = false;
		if(variableReturn2 && statement2.getVariables().get(0).getString().contains(".")) {
			variableReturnQualified2 = true;
		}
		boolean variableReturnAsLastStatement2 = variableReturn2 && statement2.isLastStatement();
		boolean numberLiteralReturn1 = statement1.getNumberLiterals().size() > 0 && statement1.getString().equals(JAVA.RETURN_SPACE + statement1.getNumberLiterals().get(0).getString() + JAVA.STATEMENT_TERMINATION) && statement1.isLastStatementInParentBlock();
		boolean numberLiteralReturn2 = statement2.getNumberLiterals().size() > 0 && statement2.getString().equals(JAVA.RETURN_SPACE + statement2.getNumberLiterals().get(0).getString() + JAVA.STATEMENT_TERMINATION) && statement2.isLastStatementInParentBlock();
		boolean lastStatement = (statement1.isLastStatement() && statement2.isLastStatement()) || 
				lastStatementInParentBlockWithSameParentType(statement1, statement2);
		if(parentMapper == null && !variableReturnAsLastStatement1 && !variableReturnAsLastStatement2 && !numberLiteralReturn1 && !numberLiteralReturn2 && statement1.getString().startsWith(JAVA.RETURN_SPACE) && statement2.getString().startsWith(JAVA.RETURN_SPACE) && lastStatement &&
				variableReturnQualified1 == variableReturnQualified2 && container1 instanceof UMLOperation && container2 instanceof UMLOperation && operationBodyMapper.getOperation1().equalSignature(operationBodyMapper.getOperation2()) && statement1.getLambdas().size() == statement2.getLambdas().size()) {
			boolean callToAddedOperation = false;
			boolean callToDeletedOperation = false;
			boolean isMovedMethod = !container1.getClassName().equals(container2.getClassName());
			if(classDiff != null) {
				if(!container1.getClassName().equals(container2.getClassName()) && modelDiff != null) {
					boolean pushDown = false;
					UMLClassBaseDiff container2Diff = modelDiff.getUMLClassDiff(container2.getClassName());
					if(container2Diff != null && container2Diff.getSuperclass() != null) {
						UMLType superclass = container2Diff.getSuperclass();
						if(container1.getClassName().endsWith("." + superclass.getClassType())) {
							pushDown = true;
						}
					}
					boolean pullUp = false;
					UMLClassBaseDiff container1Diff = modelDiff.getUMLClassDiff(container1.getClassName());
					if(container1Diff != null && container1Diff.getSuperclass() != null) {
						UMLType superclass = container1Diff.getSuperclass();
						if(container2.getClassName().endsWith("." + superclass.getClassType())) {
							pullUp = true;
						}
					}
					if(pullUp || pushDown) {
						isMovedMethod = false;
					}
				}
				if(invocationCoveringTheEntireStatement2 != null) {
					boolean superCall2 = invocationCoveringTheEntireStatement2.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression().equals("super");
					if(!superCall2) {
						UMLOperation addedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getNextClass().getOperations(), container2);
						callToAddedOperation = addedOperation != null && !addedOperation.equals(container2);
						if(callToAddedOperation == false) {
							if(invocationCoveringTheEntireStatement2.getExpression() != null) {
								List<AbstractCall> methodInvocations = methodInvocationMap2.get(invocationCoveringTheEntireStatement2.getExpression());
								if(methodInvocations != null) {
									for(AbstractCall invocation : methodInvocations) {
										if(classDiff.matchesOperation(invocation, classDiff.getNextClass().getOperations(), container2) != null) {
											callToAddedOperation = true;
											break;
										}
									}
								}
							}
						}
					}
				}
				if(invocationCoveringTheEntireStatement1 != null) {
					boolean superCall1 = invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement1.getExpression().equals("super");
					if(!superCall1) {
						UMLOperation removedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getOriginalClass().getOperations(), container1);
						callToDeletedOperation = removedOperation != null && !removedOperation.equals(container1);
						if(callToDeletedOperation == false) {
							if(invocationCoveringTheEntireStatement1.getExpression() != null) {
								List<AbstractCall> methodInvocations = methodInvocationMap1.get(invocationCoveringTheEntireStatement1.getExpression());
								if(methodInvocations != null) {
									for(AbstractCall invocation : methodInvocations) {
										if(classDiff.matchesOperation(invocation, classDiff.getOriginalClass().getOperations(), container1) != null) {
											callToDeletedOperation = true;
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			if(callToAddedOperation == callToDeletedOperation && !isMovedMethod) {
				if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
					ReplacementType replacementType = !invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ? ReplacementType.METHOD_INVOCATION_NAME : ReplacementType.METHOD_INVOCATION;
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, replacementType);
					replacementInfo.addReplacement(replacement);
				}
				else if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
					Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.actualString(),
							creationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION);
					replacementInfo.addReplacement(replacement);
				}
				else if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
					Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.actualString(),
							invocationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION);
					replacementInfo.addReplacement(replacement);
				}
				else if(methodInvocationMap1.size() > 0 && invocationCoveringTheEntireStatement2 != null) {
					AbstractCall invocation1 = null;
					for(String key : methodInvocationMap1.keySet()) {
						List<AbstractCall> calls = methodInvocationMap1.get(key);
						for(AbstractCall call : calls) {
							if(statement1.getString().endsWith(call.actualString() + JAVA.STATEMENT_TERMINATION)) {
								invocation1 = call;
								break;
							}
						}
						if(invocation1 != null) {
							break;
						}
					}
					if(invocation1 != null) {
						ReplacementType replacementType = !invocation1.identicalName(invocationCoveringTheEntireStatement2) ? ReplacementType.METHOD_INVOCATION_NAME : ReplacementType.METHOD_INVOCATION;
						Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocation1, invocationCoveringTheEntireStatement2, replacementType);
						replacementInfo.addReplacement(replacement);
					}
				}
				else if(methodInvocationMap2.size() > 0 && invocationCoveringTheEntireStatement1 != null) {
					AbstractCall invocation2 = null;
					for(String key : methodInvocationMap2.keySet()) {
						List<AbstractCall> calls = methodInvocationMap2.get(key);
						for(AbstractCall call : calls) {
							if(statement2.getString().endsWith(call.actualString() + JAVA.STATEMENT_TERMINATION)) {
								invocation2 = call;
								break;
							}
						}
						if(invocation2 != null) {
							break;
						}
					}
					if(invocation2 != null) {
						ReplacementType replacementType = !invocationCoveringTheEntireStatement1.identicalName(invocation2) ? ReplacementType.METHOD_INVOCATION_NAME : ReplacementType.METHOD_INVOCATION;
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocation2.actualString(), invocationCoveringTheEntireStatement1, invocation2, replacementType);
						replacementInfo.addReplacement(replacement);
					}
				}
				else if(invocationCoveringTheEntireStatement1 != null && variableReturn2) {
					VariableReplacementWithMethodInvocation replacement = new VariableReplacementWithMethodInvocation(invocationCoveringTheEntireStatement1.actualString(), statement2.getVariables().get(0).getString(), invocationCoveringTheEntireStatement1, Direction.INVOCATION_TO_VARIABLE);
					replacementInfo.addReplacement(replacement);
				}
				else if(invocationCoveringTheEntireStatement2 != null && variableReturn1) {
					VariableReplacementWithMethodInvocation replacement = new VariableReplacementWithMethodInvocation(statement1.getVariables().get(0).getString(), invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement2, Direction.VARIABLE_TO_INVOCATION);
					replacementInfo.addReplacement(replacement);
				}
				else if(creationCoveringTheEntireStatement1 != null && variableReturn2) {
					VariableReplacementWithMethodInvocation replacement = new VariableReplacementWithMethodInvocation(creationCoveringTheEntireStatement1.actualString(), statement2.getVariables().get(0).getString(), creationCoveringTheEntireStatement1, Direction.INVOCATION_TO_VARIABLE);
					replacementInfo.addReplacement(replacement);
				}
				else if(creationCoveringTheEntireStatement2 != null && variableReturn1) {
					VariableReplacementWithMethodInvocation replacement = new VariableReplacementWithMethodInvocation(statement1.getVariables().get(0).getString(), creationCoveringTheEntireStatement2.actualString(), creationCoveringTheEntireStatement2, Direction.VARIABLE_TO_INVOCATION);
					replacementInfo.addReplacement(replacement);
				}
				if(lambdas1.size() > 0 && lambdas2.size() > 0) {
					boolean lambdaWithBody1 = lambdas1.get(0).getBody() != null;
					boolean lambdaWithBody2 = lambdas2.get(0).getBody() != null;
					if(lambdaWithBody1 != lambdaWithBody2) {
						Replacement replacement = new Replacement(lambdas1.get(0).toString(), lambdas2.get(0).toString(), ReplacementType.LAMBDA_WITH_BODY_REPLACED_LAMBDA_WITH_EXPRESSION);
						replacementInfo.addReplacement(replacement);
					}
				}
				if(booleanLiterals1.size() == 1 && statement2.getInfixExpressions().size() == 1) {
					boolean returnBoolean1 = statement1.getString().equals(JAVA.RETURN_SPACE + statement1.getBooleanLiterals().get(0).getString() + JAVA.STATEMENT_TERMINATION);
					boolean returnInfix2 = statement2.getString().equals(JAVA.RETURN_SPACE + statement2.getInfixExpressions().get(0).getString() + JAVA.STATEMENT_TERMINATION);
					if(returnBoolean1 && returnInfix2) {
						Replacement replacement = new Replacement(statement1.getBooleanLiterals().get(0).getString(), statement2.getInfixExpressions().get(0).getString(), ReplacementType.BOOLEAN_REPLACED_WITH_INFIX_EXPRESSION);
						replacementInfo.addReplacement(replacement);
					}
				}
				if(booleanLiterals2.size() == 1 && statement1.getInfixExpressions().size() == 1) {
					boolean returnBoolean2 = statement2.getString().equals(JAVA.RETURN_SPACE + statement2.getBooleanLiterals().get(0).getString() + JAVA.STATEMENT_TERMINATION);
					boolean returnInfix1 = statement1.getString().equals(JAVA.RETURN_SPACE + statement1.getInfixExpressions().get(0).getString() + JAVA.STATEMENT_TERMINATION);
					if(returnBoolean2 && returnInfix1) {
						Replacement replacement = new Replacement(statement1.getInfixExpressions().get(0).getString(), statement2.getBooleanLiterals().get(0).getString(), ReplacementType.BOOLEAN_REPLACED_WITH_INFIX_EXPRESSION);
						replacementInfo.addReplacement(replacement);
					}
				}
				return replacementInfo.getReplacements();
			}
		}
		return null;
	}

	private static boolean wrapInMethodCall(String s1, String s2, Map<String, List<AbstractCall>> methodInvocationMap1, ReplacementInfo info) {
		for(String key : methodInvocationMap1.keySet()) {
			List<AbstractCall> calls = methodInvocationMap1.get(key);
			AbstractCall call = calls.get(0);
			if(call.arguments().size() == 1) {
				String argument = new String(call.arguments().get(0));
				for(Replacement replacement : info.getReplacements()) {
					argument = ReplacementUtil.performReplacement(argument, replacement.getBefore(), replacement.getAfter());
				}
				if(s2.contains(argument)) {
					StringBuilder callBuilder = new StringBuilder();
					if(call.getExpression() != null) {
						callBuilder.append(call.getExpression()).append(".");
					}
					callBuilder.append(call.getName());
					callBuilder.append("(");
					callBuilder.append(argument);
					callBuilder.append(")");
					String updatedS2 = s2.replace(argument, callBuilder.toString());
					if(updatedS2.equals(s1)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean existsVariableDeclarationForV2InitializedWithV1(VariableDeclaration v1, VariableDeclaration v2, ReplacementInfo info) {
		for(AbstractCodeFragment fragment2 : info.getStatements2()) {
			if(fragment2.getVariableDeclarations().contains(v2)) {
				AbstractExpression initializer = v2.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v1.getVariableName()) || variable.getString().equals(JAVA.THIS_DOT + v1.getVariableName())) {
							return true;
						}
					}
				}
				
			}
			if(fragment2.getString().equals(v2.getVariableName() + JAVA.ASSIGNMENT + v1.getVariableName() + JAVA.STATEMENT_TERMINATION)) {
				return true;
			}
			VariableDeclaration v1DeclarationInFragment2 = fragment2.getVariableDeclaration(v1.getVariableName());
			if(v1DeclarationInFragment2 != null) {
				AbstractExpression initializer = v1DeclarationInFragment2.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v2.getVariableName())) {
							return true;
						}
					}
				}
			}
			if(fragment2.getString().equals(v1.getVariableName() + JAVA.ASSIGNMENT + v2.getVariableName() + JAVA.STATEMENT_TERMINATION)) {
				return true;
			}
		}
		return false;
	}

	private static boolean existsVariableDeclarationForV1InitializedWithV2(VariableDeclaration v1, VariableDeclaration v2, ReplacementInfo info) {
		for(AbstractCodeFragment fragment1 : info.getStatements1()) {
			if(fragment1.getVariableDeclarations().contains(v1)) {
				AbstractExpression initializer = v1.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v2.getVariableName()) || variable.getString().equals(JAVA.THIS_DOT + v2.getVariableName())) {
							return true;
						}
					}
				}
				
			}
			if(fragment1.getString().equals(v1.getVariableName() + JAVA.ASSIGNMENT + v2.getVariableName() + JAVA.STATEMENT_TERMINATION)) {
				return true;
			}
			VariableDeclaration v2DeclarationInFragment1 = fragment1.getVariableDeclaration(v2.getVariableName());
			if(v2DeclarationInFragment1 != null) {
				AbstractExpression initializer = v2DeclarationInFragment1.getInitializer();
				if(initializer != null) {
					for(LeafExpression variable : initializer.getVariables()) {
						if(variable.getString().equals(v1.getVariableName())) {
							return true;
						}
					}
				}
			}
			if(fragment1.getString().equals(v2.getVariableName() + JAVA.ASSIGNMENT + v1.getVariableName() + JAVA.STATEMENT_TERMINATION)) {
				return true;
			}
		}
		return false;
	}

	private static int mappingsForStatementsInScope(AbstractCodeFragment statement1, AbstractCodeFragment statement2, VariableDeclaration v1, VariableDeclaration v2, Set<AbstractCodeMapping> mappings) {
		boolean increment = v1 != null && v2 != null && statement1.getString().startsWith(v1.getVariableName() + "+=") && statement2.getString().startsWith(v2.getVariableName() + "+=");
		boolean decrement = v1 != null && v2 != null && statement1.getString().startsWith(v1.getVariableName() + "-=") && statement2.getString().startsWith(v2.getVariableName() + "-=");
		if(v1 != null && v2 != null && (increment || decrement) && !mappings.isEmpty()) {
			int count = 0;
			Set<AbstractCodeFragment> statementsInScope1 = v1.getScope().getStatementsInScopeUsingVariable();
			Set<AbstractCodeFragment> statementsInScope2 = v2.getScope().getStatementsInScopeUsingVariable();
			for(AbstractCodeMapping mapping : mappings) {
				if(statementsInScope1.contains(mapping.getFragment1()) && statementsInScope2.contains(mapping.getFragment2())) {
					count++;
				}
				if(mapping.getFragment1().getVariableDeclarations().contains(v1) && mapping.getFragment2().getVariableDeclarations().contains(v2)) {
					count++;
				}
			}
			return count;
		}
		return -1;
	}

	private static int inconsistentVariableMappingCount(AbstractCodeFragment statement1, AbstractCodeFragment statement2, VariableDeclaration v1, VariableDeclaration v2, Set<AbstractCodeMapping> mappings) {
		int count = 0;
		if(v1 != null && v2 != null) {
			boolean variableDeclarationMismatch = false;
			for(AbstractCodeMapping mapping : mappings) {
				List<VariableDeclaration> variableDeclarations1 = mapping.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mapping.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.contains(v1) &&
						variableDeclarations2.size() > 0 &&
						!variableDeclarations2.contains(v2)) {
					variableDeclarationMismatch = true;
					count++;
				}
				if(variableDeclarations2.contains(v2) &&
						variableDeclarations1.size() > 0 &&
						!variableDeclarations1.contains(v1)) {
					variableDeclarationMismatch = true;
					count++;
				}
				if(mapping.isExact()) {
					boolean containsMapping = true;
					if(statement1 instanceof CompositeStatementObject && statement2 instanceof CompositeStatementObject &&
							statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
						CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
						CompositeStatementObject comp2 = (CompositeStatementObject)statement2;
						containsMapping = comp1.contains(mapping.getFragment1()) && comp2.contains(mapping.getFragment2());
					}
					if(containsMapping) {
						if(VariableReplacementAnalysis.bothFragmentsUseVariable(v1, mapping)) {
							VariableDeclaration otherV1 = mapping.getFragment1().getVariableDeclaration(v1.getVariableName());
							if(otherV1 != null) {
								VariableScope otherV1Scope = otherV1.getScope();
								VariableScope v1Scope = v1.getScope();
								if(otherV1Scope.overlaps(v1Scope)) {
									count++;
								}
							}
							else {
								count++;
							}
						}
						if(VariableReplacementAnalysis.bothFragmentsUseVariable(v2, mapping)) {
							VariableDeclaration otherV2 = mapping.getFragment2().getVariableDeclaration(v2.getVariableName());
							if(otherV2 != null) {
								VariableScope otherV2Scope = otherV2.getScope();
								VariableScope v2Scope = v2.getScope();
								if(otherV2Scope.overlaps(v2Scope)) {
									count++;
								}
							}
							else {
								count++;
							}
						}
					}
				}
				else if(variableDeclarationMismatch && !variableDeclarations1.contains(v1) && !variableDeclarations2.contains(v2)) {
					for(Replacement r : mapping.getReplacements()) {
						if(r.getBefore().equals(v1.getVariableName()) && !r.getAfter().equals(v2.getVariableName())) {
							count++;
						}
						else if(!r.getBefore().equals(v1.getVariableName()) && r.getAfter().equals(v2.getVariableName())) {
							count++;
						}
					}
				}
			}
		}
		return count;
	}

	private static void removeCommonElements(Set<String> strings1, Set<String> strings2) {
		Set<String> intersection = new LinkedHashSet<String>(strings1);
		intersection.retainAll(strings2);
		strings1.removeAll(intersection);
		strings2.removeAll(intersection);
	}

	private static void removeCommonTypes(Set<String> strings1, Set<String> strings2, List<String> types1, List<String> types2) {
		if(types1.size() == types2.size()) {
			Set<String> removeFromIntersection = new LinkedHashSet<String>();
			for(int i=0; i<types1.size(); i++) {
				String type1 = types1.get(i);
				String type2 = types2.get(i);
				if(!type1.equals(type2)) {
					removeFromIntersection.add(type1);
					removeFromIntersection.add(type2);
				}
			}
			Set<String> intersection = new LinkedHashSet<String>(strings1);
			intersection.retainAll(strings2);
			intersection.removeAll(removeFromIntersection);
			strings1.removeAll(intersection);
			strings2.removeAll(intersection);
		}
		else {
			removeCommonElements(strings1, strings2);
		}
	}

	private static Map<String, List<AbstractCall>> convertToMap(List<AbstractCall> calls) {
		Map<String, List<AbstractCall>> methodInvocationMap = new LinkedHashMap<>();
		for(AbstractCall invocation : calls) {
			String methodInvocation = invocation.getString();
			if(methodInvocationMap.containsKey(methodInvocation)) {
				methodInvocationMap.get(methodInvocation).add(invocation);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(invocation);
				methodInvocationMap.put(methodInvocation, list);
			}
		}
		return methodInvocationMap;
	}

	private static Set<String> convertToStringSet(List<? extends LeafExpression> expressions) {
		Set<String> set = new LinkedHashSet<>();
		for(LeafExpression expression : expressions) {
			set.add(expression.getString());
		}
		return set;
	}

	private static void replaceVariablesWithArguments(Set<String> variables, Map<String, String> parameterToArgumentMap) {
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(variables.contains(parameter)) {
				if(!StringDistance.isNumeric(argument)) {
					variables.add(argument);
				}
				if(argument.contains("(") && argument.contains(")")) {
					int indexOfOpeningParenthesis = argument.indexOf("(");
					int indexOfClosingParenthesis = argument.lastIndexOf(")");
					boolean openingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(argument, indexOfOpeningParenthesis);
					boolean closingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(argument, indexOfClosingParenthesis);
					boolean openingParenthesisInsideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(argument, indexOfOpeningParenthesis);
					boolean closingParenthesisIndideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(argument, indexOfClosingParenthesis);
					if(indexOfOpeningParenthesis < indexOfClosingParenthesis &&
							!openingParenthesisInsideSingleQuotes && !closingParenthesisInsideSingleQuotes &&
							!openingParenthesisInsideDoubleQuotes && !closingParenthesisIndideDoubleQuotes) {
						String arguments = argument.substring(indexOfOpeningParenthesis+1, indexOfClosingParenthesis);
						if(!arguments.isEmpty() && !arguments.contains(",") && !arguments.contains("(") && !arguments.contains(")") && !StringDistance.isNumeric(arguments)) {
							variables.add(arguments);
						}
					}
				}
			}
		}
	}

	private static void replaceVariablesWithArguments(Map<String, List<AbstractCall>> callMap,
			Set<String> calls, Map<String, String> parameterToArgumentMap) {
		if(isCallChain(callMap.values())) {
			for(String parameter : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameter);
				if(!parameter.equals(argument)) {
					Set<String> toBeAdded = new LinkedHashSet<String>();
					for(String call : calls) {
						String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
						if(!call.equals(afterReplacement)) {
							toBeAdded.add(afterReplacement);
							List<? extends AbstractCall> oldCalls = callMap.get(call);
							List<AbstractCall> newCalls = new ArrayList<AbstractCall>();
							for(AbstractCall oldCall : oldCalls) {
								AbstractCall newCall = oldCall.update(parameter, argument);
								newCalls.add(newCall);
							}
							callMap.put(afterReplacement, newCalls);
						}
					}
					calls.addAll(toBeAdded);
				}
			}
		}
		else {
			Set<String> finalNewCalls = new LinkedHashSet<String>();
			for(String parameter : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameter);
				if(!parameter.equals(argument)) {
					Set<String> toBeAdded = new LinkedHashSet<String>();
					for(String call : calls) {
						String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
						if(!call.equals(afterReplacement)) {
							toBeAdded.add(afterReplacement);
							List<? extends AbstractCall> oldCalls = callMap.get(call);
							List<AbstractCall> newCalls = new ArrayList<AbstractCall>();
							for(AbstractCall oldCall : oldCalls) {
								AbstractCall newCall = oldCall.update(parameter, argument);
								newCalls.add(newCall);
							}
							callMap.put(afterReplacement, newCalls);
						}
					}
					finalNewCalls.addAll(toBeAdded);
				}
			}
			calls.addAll(finalNewCalls);
		}
	}

	private static boolean isCallChain(Collection<List<AbstractCall>> calls) {
		if(calls.size() > 1) {
			AbstractCall previous = null;
			AbstractCall current = null;
			int chainLength = 0;
			for(List<? extends AbstractCall> list : calls) {
				previous = current;
				current = list.get(0);
				if(current != null && previous != null) {
					if(previous.getExpression() != null && previous.getExpression().equals(current.actualString())) {
						chainLength++;
					}
					else {
						return false;
					}
				}
			}
			if(chainLength == calls.size()-1) {
				return true;
			}
		}
		return false;
	}

	private static void findReplacements(Set<String> strings1, Set<String> strings2, ReplacementInfo replacementInfo, ReplacementType type,
			VariableDeclarationContainer container1, VariableDeclarationContainer container2, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		if(strings1.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS || strings2.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS ||
				strings1.size()*strings2.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS*10 ||
				strings2.size() > 10*strings1.size() || strings1.size() > 10*strings2.size()) {
			return;
		}
		TreeMap<Double, Set<Replacement>> globalReplacementMap = new TreeMap<Double, Set<Replacement>>();
		TreeMap<Double, Set<Replacement>> replacementCache = new TreeMap<Double, Set<Replacement>>();
		if(strings1.size() <= strings2.size()) {
			for(String s1 : strings1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				for(String s2 : strings2) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
					boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
					if(containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
							container1 != null && container2 != null &&
							container1.getVariableDeclaration(s1) == null && container2.getVariableDeclaration(s2) == null &&
							classDiff != null && !classDiff.getOriginalClass().containsAttributeWithName(s1) && !classDiff.getNextClass().containsAttributeWithName(s2)) {
						continue;
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
						Replacement replacement = new Replacement(s1, s2, type);
						double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
						replacementMap.put(distancenormalized, replacement);
						if(replacementCache.containsKey(distancenormalized)) {
							replacementCache.get(distancenormalized).add(replacement);
						}
						else {
							Set<Replacement> r = new LinkedHashSet<Replacement>();
							r.add(replacement);
							replacementCache.put(distancenormalized, r);
						}
						if(distanceRaw == 0) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Double distancenormalized = replacementMap.firstEntry().getKey();
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(globalReplacementMap.containsKey(distancenormalized)) {
						globalReplacementMap.get(distancenormalized).add(replacement);
					}
					else {
						Set<Replacement> r = new LinkedHashSet<Replacement>();
						r.add(replacement);
						globalReplacementMap.put(distancenormalized, r);
					}
					if(distancenormalized == 0) {
						break;
					}
				}
			}
		}
		else {
			for(String s2 : strings2) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				for(String s1 : strings1) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
					boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
					if(containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
							container1 != null && container2 != null &&
							container1.getVariableDeclaration(s1) == null && container2.getVariableDeclaration(s2) == null &&
							classDiff != null && !classDiff.getOriginalClass().containsAttributeWithName(s1) && !classDiff.getNextClass().containsAttributeWithName(s2)) {
						continue;
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
						Replacement replacement = new Replacement(s1, s2, type);
						double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
						replacementMap.put(distancenormalized, replacement);
						if(replacementCache.containsKey(distancenormalized)) {
							replacementCache.get(distancenormalized).add(replacement);
						}
						else {
							Set<Replacement> r = new LinkedHashSet<Replacement>();
							r.add(replacement);
							replacementCache.put(distancenormalized, r);
						}
						if(distanceRaw == 0) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Double distancenormalized = replacementMap.firstEntry().getKey();
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(globalReplacementMap.containsKey(distancenormalized)) {
						globalReplacementMap.get(distancenormalized).add(replacement);
					}
					else {
						Set<Replacement> r = new LinkedHashSet<Replacement>();
						r.add(replacement);
						globalReplacementMap.put(distancenormalized, r);
					}
					if(replacementMap.firstEntry().getKey() == 0) {
						break;
					}
				}
			}
		}
		if(!globalReplacementMap.isEmpty()) {
			Double distancenormalized = globalReplacementMap.firstEntry().getKey();
			if(distancenormalized == 0) {
				Set<Replacement> replacements = globalReplacementMap.firstEntry().getValue();
				for(Replacement replacement : replacements) {
					replacementInfo.addReplacement(replacement);
					replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
				}
			}
			else {
				Set<Replacement> conflictingReplacements = conflictingReplacements(globalReplacementMap, replacementCache);
				Set<String> processedBefores = new LinkedHashSet<String>();
				for(Set<Replacement> replacements : globalReplacementMap.values()) {
					for(Replacement replacement : replacements) {
						if(!conflictingReplacements.contains(replacement)) {
							if(!processedBefores.contains(replacement.getBefore())) {
								replacementInfo.addReplacement(replacement);
								replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
								processedBefores.add(replacement.getBefore());
							}
							else {
								//find the next best match for replacement.getAfter() from the replacement cache
								for(Set<Replacement> replacements2 : replacementCache.values()) {
									boolean found = false;
									for(Replacement replacement2 : replacements2) {
										if(replacement2.getAfter().equals(replacement.getAfter()) && !replacement2.equals(replacement)) {
											replacementInfo.addReplacement(replacement2);
											replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement2.getBefore(), replacement2.getAfter()));
											processedBefores.add(replacement2.getBefore());
											found = true;
											break;
										}
									}
									if(found) {
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

	private static Set<Replacement> conflictingReplacements(TreeMap<Double, Set<Replacement>> globalReplacementMap, TreeMap<Double, Set<Replacement>> replacementCache) {
		Map<String, Set<Replacement>> map = new LinkedHashMap<String, Set<Replacement>>();
		for(Map.Entry<Double, Set<Replacement>> entry : globalReplacementMap.entrySet()) {
			Set<Replacement> replacements = entry.getValue();
			Double score = entry.getKey();
			for(Replacement replacement : replacements) {
				String after = replacement.getAfter();
				if(map.containsKey(after)) {
					map.get(after).add(replacement);
				}
				else {
					Set<Replacement> set = new LinkedHashSet<Replacement>();
					set.add(replacement);
					map.put(after, set);
				}
			}
			if(replacementCache.containsKey(score)) {
				Set<Replacement> replacements2 = replacementCache.get(score);
				if(replacements2.size() > 1) {
					for(Replacement replacement : replacements2) {
						String after = replacement.getAfter();
						if(map.containsKey(after)) {
							map.get(after).add(replacement);
						}
					}
				}
			}
		}
		Set<Replacement> conflictingReplacements = new LinkedHashSet<Replacement>();
		for(String key : map.keySet()) {
			Set<Replacement> replacements = map.get(key);
			if(replacements.size() > 1) {
				conflictingReplacements.add(replacements.iterator().next());
			}
		}
		return conflictingReplacements;
	}

	private static Set<Replacement> variableReplacementWithinMethodInvocations(String s1, String s2, Set<String> variables1, Set<String> variables2, Optional<Map<String, String>> parameterToArgumentMap1) {
		Set<Replacement> tempReplacements = new LinkedHashSet<Replacement>();
		for(String variable1 : variables1) {
			String originalVariable1 = variable1;
			if(parameterToArgumentMap1.isPresent() && parameterToArgumentMap1.get().containsKey(variable1) && !parameterToArgumentMap1.get().get(variable1).equals(variable1)) {
				variable1 = parameterToArgumentMap1.get().get(variable1);
			}
			if((ReplacementUtil.contains(s1, variable1) || s1.endsWith(variable1)) && !s1.equals(variable1) && !s1.equals(JAVA.THIS_DOT + variable1) && !s1.equals("_" + variable1)) {
				int startIndex1 = s1.indexOf(variable1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + variable1.length(), s1.length());
				for(String variable2 : variables2) {
					if(variable2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
						variable2 = variable2.substring(0, variable2.indexOf(substringAfterIndex1));
					}
					if((ReplacementUtil.contains(s2, variable2) || s2.endsWith(variable2)) && !s2.equals(variable2)) {
						int startIndex2 = s2.indexOf(variable2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + variable2.length(), s2.length());
						boolean suffixMatch = substringAfterIndex1.equals(substringAfterIndex2) && !substringAfterIndex1.isEmpty();
						if(!suffixMatch && !tempReplacements.isEmpty() && !substringAfterIndex1.isEmpty()) {
							for(Replacement r : tempReplacements) {
								String tmp1 = substringAfterIndex1.replace(r.getBefore(), r.getAfter());
								if(tmp1.equals(substringAfterIndex2)) {
									suffixMatch = true;
									break;
								}
							}
						}
						boolean prefixMatch = substringBeforeIndex1.equals(substringBeforeIndex2) && !substringBeforeIndex1.isEmpty();
						if(!prefixMatch && !tempReplacements.isEmpty() && !substringBeforeIndex1.isEmpty()) {
							for(Replacement r : tempReplacements) {
								String tmp1 = substringBeforeIndex1.replace(r.getBefore(), r.getAfter());
								if(tmp1.equals(substringBeforeIndex2)) {
									prefixMatch = true;
									break;
								}
							}
						}
						if(prefixMatch || suffixMatch) {
							Replacement r = new Replacement(originalVariable1, variable2, ReplacementType.VARIABLE_NAME);
							tempReplacements.add(r);
						}
					}
				}
			}
		}
		String tmp1 = new String(s1);
		Set<Replacement> finalReplacements = new LinkedHashSet<Replacement>();
		for(Replacement replacement : tempReplacements) {
			tmp1 = ReplacementUtil.performReplacement(tmp1, s2, replacement.getBefore(), replacement.getAfter());
			finalReplacements.add(replacement);
			if(tmp1.equals(s2)) {
				return finalReplacements;
			}
		}
		if(tmp1.equals(s2)) {
			return finalReplacements;
		}
		else {
			return Collections.emptySet();
		}
	}

	private static Map<Replacement, Boolean> replacementsWithinMethodInvocations(String s1, String s2, Set<String> set1, Set<String> set2, Map<String, List<AbstractCall>> methodInvocationMap1, Map<String, List<AbstractCall>> methodInvocationMap2) {
		Map<Replacement, Boolean> replacements = new LinkedHashMap<Replacement, Boolean>();
		for(String element1 : set1) {
			if(s1.contains(element1) && !s1.equals(element1) && !s1.equals(JAVA.THIS_DOT + element1) && !s1.equals("_" + element1)) {
				int startIndex1 = s1.indexOf(element1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + element1.length(), s1.length());
				for(String element2 : set2) {
					if(element2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
						element2 = element2.substring(0, element2.indexOf(substringAfterIndex1));
					}
					if(s2.contains(element2) && !s2.equals(element2) && !element1.equals(element2)) {
						int startIndex2 = s2.indexOf(element2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + element2.length(), s2.length());
						List<? extends AbstractCall> methodInvocationList1 = methodInvocationMap1.get(element1);
						List<? extends AbstractCall> methodInvocationList2 = methodInvocationMap2.get(element2);
						if(substringBeforeIndex1.equals(substringBeforeIndex2) && !substringAfterIndex1.isEmpty() && !substringAfterIndex2.isEmpty() && methodInvocationList1 != null && methodInvocationList2 != null) {
							boolean skip = false;
							if(substringAfterIndex1.length() > substringAfterIndex2.length()) {
								skip = s2.contains(substringAfterIndex1);
								if(substringAfterIndex1.startsWith(".") && !substringAfterIndex1.startsWith(".<")) {
									skip = s2.contains(substringAfterIndex1.substring(1));
								}
								else if(substringAfterIndex1.startsWith(".<") && substringAfterIndex1.contains(">")) {
									skip = s2.contains(substringAfterIndex1.substring(substringAfterIndex1.indexOf(">")+1));
								}
							}
							else if(substringAfterIndex1.length() < substringAfterIndex2.length()) {
								skip = s1.contains(substringAfterIndex2);
								if(substringAfterIndex2.startsWith(".") && !substringAfterIndex2.startsWith(".<")) {
									skip = s1.contains(substringAfterIndex2.substring(1));
								}
								else if(substringAfterIndex2.startsWith(".<") && substringAfterIndex2.contains(">")) {
									skip = s1.contains(substringAfterIndex2.substring(substringAfterIndex2.indexOf(">")+1));
								}
							}
							if(!skip) {
								for(Replacement old : replacements.keySet()) {
									if(old.getBefore().equals(element1) && !old.getAfter().equals(element2)) {
										skip = true;
										break;
									}
									else if(!old.getBefore().equals(element1) && old.getAfter().equals(element2)) {
										skip = true;
										break;
									}
								}
								if(!skip) {
									Replacement r = new MethodInvocationReplacement(element1, element2, methodInvocationList1.get(0), methodInvocationList2.get(0), ReplacementType.METHOD_INVOCATION);
									replacements.put(r, substringAfterIndex1.equals(substringAfterIndex2));
								}
							}
						}
						else if(substringAfterIndex1.equals(substringAfterIndex2) && !substringBeforeIndex1.isEmpty() && !substringBeforeIndex2.isEmpty() && methodInvocationList1 != null && methodInvocationList2 != null) {
							boolean skip = false;
							if(substringBeforeIndex1.length() > substringBeforeIndex2.length()) {
								skip = s2.contains(substringBeforeIndex1);
							}
							else if(substringBeforeIndex1.length() < substringBeforeIndex2.length()) {
								skip = s1.contains(substringBeforeIndex2);
							}
							if(!skip) {
								for(Replacement old : replacements.keySet()) {
									if(old.getBefore().equals(element1) && !old.getAfter().equals(element2)) {
										skip = true;
										break;
									}
									else if(!old.getBefore().equals(element1) && old.getAfter().equals(element2)) {
										skip = true;
										break;
									}
								}
								if(!skip) {
									Replacement r = new MethodInvocationReplacement(element1, element2, methodInvocationList1.get(0), methodInvocationList2.get(0), ReplacementType.METHOD_INVOCATION);
									replacements.put(r, substringBeforeIndex1.equals(substringBeforeIndex2));
								}
							}
						}
					}
				}
			}
		}
		//remove subsumed replacements
		if(replacements.values().contains(true) && replacements.values().contains(false)) {
			Replacement trueReplacement = null;
			for(Replacement key : replacements.keySet()) {
				if(replacements.get(key) == true) {
					trueReplacement = key;
					break;
				}
			}
			Set<Replacement> toBeRemoved = new LinkedHashSet<Replacement>();
			for(Replacement key : replacements.keySet()) {
				if(replacements.get(key) == false) {
					if(key.getBefore().equals(trueReplacement.getBefore()) && trueReplacement.getAfter().contains(key.getAfter())) {
						toBeRemoved.add(key);
					}
					else if(key.getAfter().equals(trueReplacement.getAfter()) && trueReplacement.getBefore().contains(key.getBefore())) {
						toBeRemoved.add(key);
					}
				}
			}
			for(Replacement r : toBeRemoved) {
				replacements.remove(r);
			}
		}
		return replacements;
	}

	private static Map<Replacement, Boolean> replacementsWithinMethodInvocations(String s1, String s2, Set<String> set1, Set<String> set2, Map<String, List<AbstractCall>> methodInvocationMap, Direction direction) {
		Map<Replacement, Boolean> replacements = new LinkedHashMap<Replacement, Boolean>();
		for(String element1 : set1) {
			if(s1.contains(element1) && !s1.equals(element1) && !s1.equals(JAVA.THIS_DOT + element1) && !s1.equals("_" + element1)) {
				int startIndex1 = s1.indexOf(element1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + element1.length(), s1.length());
				for(String element2 : set2) {
					if(element2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
						element2 = element2.substring(0, element2.indexOf(substringAfterIndex1));
					}
					if(s2.contains(element2) && !s2.equals(element2) && !element1.equals(element2)) {
						int startIndex2 = s2.indexOf(element2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + element2.length(), s2.length());
						List<? extends AbstractCall> methodInvocationList = null;
						if(direction.equals(Direction.VARIABLE_TO_INVOCATION))
							methodInvocationList = methodInvocationMap.get(element2);
						else if(direction.equals(Direction.INVOCATION_TO_VARIABLE))
							methodInvocationList = methodInvocationMap.get(element1);
						if(substringBeforeIndex1.equals(substringBeforeIndex2) && !substringAfterIndex1.isEmpty() && !substringAfterIndex2.isEmpty() && methodInvocationList != null) {
							boolean skip = false;
							if(substringAfterIndex1.length() > substringAfterIndex2.length()) {
								skip = s2.contains(substringAfterIndex1);
								if(substringAfterIndex1.startsWith(".") && !substringAfterIndex1.startsWith(".<")) {
									skip = s2.contains(substringAfterIndex1.substring(1));
								}
								else if(substringAfterIndex1.startsWith(".<") && substringAfterIndex1.contains(">")) {
									skip = s2.contains(substringAfterIndex1.substring(substringAfterIndex1.indexOf(">")+1));
								}
							}
							else if(substringAfterIndex1.length() < substringAfterIndex2.length()) {
								skip = s1.contains(substringAfterIndex2);
								if(substringAfterIndex2.startsWith(".") && !substringAfterIndex2.startsWith(".<")) {
									skip = s1.contains(substringAfterIndex2.substring(1));
								}
								else if(substringAfterIndex2.startsWith(".<") && substringAfterIndex2.contains(">")) {
									skip = s1.contains(substringAfterIndex2.substring(substringAfterIndex2.indexOf(">")+1));
								}
							}
							if(!skip) {
								Replacement r = new VariableReplacementWithMethodInvocation(element1, element2, methodInvocationList.get(0), direction);
								replacements.put(r, substringAfterIndex1.equals(substringAfterIndex2));
							}
						}
						else if(substringAfterIndex1.equals(substringAfterIndex2) && !substringBeforeIndex1.isEmpty() && !substringBeforeIndex2.isEmpty() && methodInvocationList != null) {
							boolean skip = false;
							if(substringBeforeIndex1.length() > substringBeforeIndex2.length()) {
								skip = s2.contains(substringBeforeIndex1);
							}
							else if(substringBeforeIndex1.length() < substringBeforeIndex2.length()) {
								skip = s1.contains(substringBeforeIndex2);
							}
							if(!skip) {
								Replacement r = new VariableReplacementWithMethodInvocation(element1, element2, methodInvocationList.get(0), direction);
								replacements.put(r, substringBeforeIndex1.equals(substringBeforeIndex2));
							}
						}
					}
				}
			}
		}
		//remove subsumed replacements
		if(replacements.values().contains(true) && replacements.values().contains(false)) {
			Replacement trueReplacement = null;
			for(Replacement key : replacements.keySet()) {
				if(replacements.get(key) == true) {
					trueReplacement = key;
					break;
				}
			}
			Set<Replacement> toBeRemoved = new LinkedHashSet<Replacement>();
			for(Replacement key : replacements.keySet()) {
				if(replacements.get(key) == false) {
					if(key.getBefore().equals(trueReplacement.getBefore()) && trueReplacement.getAfter().contains(key.getAfter())) {
						toBeRemoved.add(key);
					}
					else if(key.getAfter().equals(trueReplacement.getAfter()) && trueReplacement.getBefore().contains(key.getBefore())) {
						toBeRemoved.add(key);
					}
				}
			}
			for(Replacement r : toBeRemoved) {
				replacements.remove(r);
			}
		}
		return replacements;
	}

	private static boolean variablesStartWithSameCase(String s1, String s2, ReplacementInfo replacementInfo) {
		if(s1.length() > 0 && s2.length() > 0) {
			if((s1.contains(".") || s2.contains(".")) && !s1.contains("."+s2) && !s2.contains("."+s1) && (replacementInfo.getArgumentizedString1().equals(JAVA.RETURN_SPACE + s1 + JAVA.STATEMENT_TERMINATION) ||
					replacementInfo.getArgumentizedString2().equals(JAVA.RETURN_SPACE + s2 + JAVA.STATEMENT_TERMINATION))) {
				return false;
			}
			else if(PRIMITIVE_WRAPPER_CLASS_MAP.values().contains(s1) != PRIMITIVE_WRAPPER_CLASS_MAP.values().contains(s2) &&
					Character.isUpperCase(s1.charAt(0)) != Character.isUpperCase(s2.charAt(0))) {
				return false;
			}
		}
		return true;
	}

	private static boolean nonMatchedStatementUsesVariableInArgument(List<? extends AbstractCodeFragment> statements, String variable, String otherArgument) {
		for(AbstractCodeFragment statement : statements) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation != null) {
				for(String argument : invocation.arguments()) {
					String argumentNoWhiteSpace = argument.replaceAll("\\s","");
					if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
							!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) && !argument.equals(otherArgument)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean containsRightHandSideReplacementWithAppendChange(AbstractCodeFragment statement1, AbstractCodeFragment statement2, ReplacementInfo info, Replacement candidateReplacement) {
		for(Replacement r : info.getReplacements()) {
			if(statement1.getString().endsWith(JAVA.ASSIGNMENT + r.getBefore() + JAVA.STATEMENT_TERMINATION) &&
					statement2.getString().endsWith(JAVA.ASSIGNMENT + r.getAfter() + JAVA.STATEMENT_TERMINATION) &&
					(r.getAfter().startsWith(r.getBefore()) ||
					r.getBefore().startsWith(r.getAfter()))) {
				return true;
			}
		}
		if(statement1.getString().startsWith(candidateReplacement.getBefore() + JAVA.ASSIGNMENT) &&
				statement2.getString().startsWith(candidateReplacement.getAfter() + JAVA.ASSIGNMENT) &&
				statement1.getString().endsWith(JAVA.STATEMENT_TERMINATION) && statement2.getString().endsWith(JAVA.STATEMENT_TERMINATION)) {
			String suffix1 = statement1.getString().substring(statement1.getString().indexOf(JAVA.ASSIGNMENT) + 1, statement1.getString().lastIndexOf(JAVA.STATEMENT_TERMINATION));
			String suffix2 = statement2.getString().substring(statement2.getString().indexOf(JAVA.ASSIGNMENT) + 1, statement2.getString().lastIndexOf(JAVA.STATEMENT_TERMINATION));
			if(suffix1.startsWith(suffix2) || suffix2.startsWith(suffix1)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isExtractedVariable(VariableDeclaration v2, Set<AbstractCodeMapping> mappings) {
		for(AbstractCodeMapping mapping : mappings) {
			for(Refactoring ref : mapping.getRefactorings()) {
				if(ref instanceof ExtractVariableRefactoring) {
					ExtractVariableRefactoring extract = (ExtractVariableRefactoring)ref;
					if(extract.getVariableDeclaration().equals(v2)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean argumentsWithVariableDeclarationMapping(AbstractCall invocation1, AbstractCall invocation2, Set<AbstractCodeMapping> mappings) {
		if(invocation1.arguments().size() == invocation2.arguments().size()) {
			int differentArguments = 0;
			int matchedArguments = 0;
			for(int i=0; i<invocation1.arguments().size(); i++) {
				String argument1 = invocation1.arguments().get(i);
				String argument2 = invocation2.arguments().get(i);
				if(!argument1.equals(argument2)) {
					differentArguments++;
					for(AbstractCodeMapping mapping : mappings) {
						if(mapping.getFragment1().getVariableDeclaration(argument1) != null && mapping.getFragment2().getVariableDeclaration(argument2) != null) {
							matchedArguments++;
							break;
						}
					}
				}
			}
			return differentArguments == matchedArguments && matchedArguments > 0;
		}
		return false;
	}

	private static boolean lastStatementInParentBlockWithSameParentType(AbstractCodeFragment statement1, AbstractCodeFragment statement2) {
		if(statement1.isLastStatementInParentBlock() && statement2.isLastStatementInParentBlock()) {
			CompositeStatementObject parent1 = statement1.getParent();
			CompositeStatementObject parent2 = statement2.getParent();
			while(parent1 != null && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
				parent1 = parent1.getParent(); 
			}
			while(parent2 != null && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK)) { 
				parent2 = parent2.getParent(); 
			}
			if(parent1 != null && parent2 != null && parent1.getLocationInfo().getCodeElementType().equals(parent2.getLocationInfo().getCodeElementType())) {
				// check if one is simple if and the other if-else-if
				boolean isBlock1 = statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK);
				boolean isWithinIfBranch1 = isBlock1 ? isIfBranch(statement1, statement1.getParent()) : isIfBranch(statement1.getParent(), statement1.getParent().getParent());
				if(!isWithinIfBranch1 && parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && parent1.getStatements().size() == 1 && parent1.getStatements().contains(statement1)) {
					isWithinIfBranch1 = true;
				}
				boolean isWithinElseBranch1 = isBlock1 ? isElseBranch(statement1, statement1.getParent()) : isElseBranch(statement1.getParent(), statement1.getParent().getParent());
				boolean isWithinElseIfBranch1 = false;
				CompositeStatementObject grandGrandParent1 = statement1.getParent().getParent().getParent();
				if(grandGrandParent1 != null) {
					isWithinElseIfBranch1 = isBlock1 ? isElseIfBranch(statement1.getParent(), statement1.getParent().getParent()) : isElseIfBranch(statement1.getParent().getParent(), grandGrandParent1);
				}
				boolean hasElseIfBranch1 = hasElseIfBranch(parent1);
				//boolean hasElseBranch1 = hasElseBranch(parent1);
				
				boolean isBlock2 = statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK);
				boolean isWithinIfBranch2 = isBlock2 ? isIfBranch(statement2, statement2.getParent()) : isIfBranch(statement2.getParent(), statement2.getParent().getParent());
				if(!isWithinIfBranch2 && parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && parent2.getStatements().size() == 1 && parent2.getStatements().contains(statement2)) {
					isWithinIfBranch2 = true;
				}
				boolean isWithinElseBranch2 = isBlock2 ? isElseBranch(statement2, statement2.getParent()) : isElseBranch(statement2.getParent(), statement2.getParent().getParent());
				boolean isWithinElseIfBranch2 = false;
				CompositeStatementObject grandGrandParent2 = statement2.getParent().getParent().getParent();
				if(grandGrandParent2 != null) {
					isWithinElseIfBranch2 = isBlock2 ? isElseIfBranch(statement2.getParent(), statement2.getParent().getParent()) : isElseIfBranch(statement2.getParent().getParent(), grandGrandParent2);
				}
				boolean hasElseIfBranch2 = hasElseIfBranch(parent2);
				//boolean hasElseBranch2 = hasElseBranch(parent2);
				if(isWithinIfBranch1 == isWithinIfBranch2 && isWithinElseBranch1 == isWithinElseBranch2 && isWithinElseIfBranch1 == isWithinElseIfBranch2 &&
						hasElseIfBranch1 == hasElseIfBranch2 /*&& hasElseBranch1 == hasElseBranch2*/) {
					return true;
				}
			}
		}
		return false;
	}

	private static void addLeafMappings(AbstractCodeFragment statement1, AbstractCodeFragment statement2, Replacement r, ReplacementInfo replacementInfo, VariableDeclarationContainer container1, VariableDeclarationContainer container2) {
		List<LeafExpression> leafExpressions1 = statement1.findExpression(r.getBefore());
		List<LeafExpression> leafExpressions2 = statement2.findExpression(r.getAfter());
		if(statement1.getVariables().containsAll(leafExpressions1) && statement2.getVariables().containsAll(leafExpressions2)) {
			return;
		}
		if(leafExpressions1.size() == leafExpressions2.size()) {
			for(int i=0; i<leafExpressions1.size(); i++) {
				LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), container1, container2);
				replacementInfo.addSubExpressionMapping(leafMapping);
			}
		}
	}

	private static boolean checkIfStatementIsExtracted(AbstractCodeFragment statement1, AbstractCodeFragment statement2, UMLOperation addedOperation, UMLOperationBodyMapper operationBodyMapper) {
		boolean nested = operationBodyMapper.isNested();
		UMLAbstractClassDiff classDiff = operationBodyMapper.getClassDiff();
		if(classDiff != null) { 
			AbstractCall invocationCoveringTheEntireStatement2 = statement1.invocationCoveringEntireFragment();
			if(invocationCoveringTheEntireStatement2 != null) {
				if(addedOperation != null && addedOperation.getBody() != null) {
					for(AbstractCodeFragment fragment : addedOperation.getBody().getCompositeStatement().getLeaves()) {
						if(fragment.getString().equals(statement1.getString())) {
							return true;
						}
						if(fragment.getVariableDeclarations().size() > 0 && fragment.getVariableDeclarations().toString().equals(statement1.getVariableDeclarations().toString())) {
							return true;
						}
						if(statement1.getVariableDeclarations().size() > 0 && statement1.getVariableDeclarations().get(0).getInitializer() != null &&
								fragment.getString().equals(JAVA.RETURN_SPACE + statement1.getVariableDeclarations().get(0).getInitializer().getString() + JAVA.STATEMENT_TERMINATION) &&
								(!fragment.getParent().equals(addedOperation.getBody().getCompositeStatement()) || nested)) {
							return true;
						}
						for(AnonymousClassDeclarationObject anonymous : fragment.getAnonymousClassDeclarations()) {
							UMLAnonymousClass anonymousClass = addedOperation.findAnonymousClass(anonymous);
							if(anonymousClass != null) {
								for(UMLOperation anonymousOperation : anonymousClass.getOperations()) {
									for(AbstractCodeFragment anonymousFragment : anonymousOperation.getBody().getCompositeStatement().getLeaves()) {
										if(anonymousFragment.getString().equals(statement1.getString())) {
											return true;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static boolean bothContainNullInDifferentIndexes(AbstractCall call1, AbstractCall call2) {
		if(call1 != null && call2 != null && call1.arguments().contains("null") && call2.arguments().contains("null")) {
			int index1 = call1.arguments().indexOf("null");
			int index2 = call2.arguments().indexOf("null");
			return index1 != index2;
		}
		return false;
	}

	private static boolean matchingArgument(Set<String> variables1, Set<String> literals2, AbstractCall call1, AbstractCall call2) {
		if(call1 != null && call2 != null && call1.arguments().size() == call2.arguments().size()) {
			for(int i=0; i<call1.arguments().size(); i++) {
				String arg1 = call1.arguments().get(i);
				String arg2 = call2.arguments().get(i);
				if(variables1.contains(arg1) && literals2.contains(arg2)) {
					return true;
				}
			}
		}
		return false;
	}

	private static Set<AbstractCodeFragment> additionallyMatchedStatements(List<VariableDeclaration> variableDeclarations, List<? extends AbstractCodeFragment> unmatchedStatements) {
		Set<AbstractCodeFragment> additionallyMatchedStatements = new LinkedHashSet<AbstractCodeFragment>();
		if(!variableDeclarations.isEmpty()) {
			for(AbstractCodeFragment codeFragment : unmatchedStatements) {
				for(VariableDeclaration variableDeclaration : codeFragment.getVariableDeclarations()) {
					if(variableDeclaration.getVariableName().equals(variableDeclarations.get(0).getVariableName()) &&
							variableDeclaration.equalType(variableDeclarations.get(0))) {
						additionallyMatchedStatements.add(codeFragment);
						break;
					}
				}
			}
		}
		return additionallyMatchedStatements;
	}

	private static boolean matchAsLambdaExpressionArgument(String s1, String s2, Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo, AbstractCodeFragment statement1, VariableDeclarationContainer container2, UMLOperationBodyMapper operationBodyMapper) {
		UMLOperationBodyMapper parentMapper = operationBodyMapper.getParentMapper();
		if(parentMapper != null && s2.contains(JAVA.LAMBDA_ARROW)) {
			for(String parameterName : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameterName);
				if(!parameterName.equals(argument) && !argument.isEmpty() && s2.contains(argument)) {
					for(VariableDeclaration parameter : container2.getParameterDeclarationList()) {
						if(parameterName.equals(parameter.getVariableName())) {
							String lambdaArrow = "()" + JAVA.LAMBDA_ARROW;
							String supplierGet = ".get()";
							UMLType parameterType = parameter.getType();
							if(parameterType != null && parameterType.getClassType().equals("Supplier") && s2.contains(supplierGet) && s2.contains(lambdaArrow)) {
								String tmp = s2.replace(supplierGet, "");
								tmp = tmp.replace(lambdaArrow, "");
								if(s1.equals(tmp)) {
									for(AbstractCodeFragment nonMappedLeafT2 : parentMapper.getNonMappedLeavesT2()) {
										List<AbstractCall> methodInvocations = nonMappedLeafT2.getMethodInvocations();
										if(methodInvocations.contains(operationBodyMapper.getOperationInvocation())) {
											List<LambdaExpressionObject> lambdas = nonMappedLeafT2.getLambdas();
											for(LambdaExpressionObject lambda : lambdas) {
												AbstractExpression lambdaExpression = lambda.getExpression();
												if(lambdaExpression != null && lambda.toString().equals(argument)) {
													List<VariableDeclaration> variableDeclarations = statement1.getVariableDeclarations();
													for(VariableDeclaration variableDeclaration : variableDeclarations) {
														AbstractExpression initializer = variableDeclaration.getInitializer();
														if(initializer != null && initializer.getString().equals(lambdaExpression.getString())) {
															LeafMapping mapping = operationBodyMapper.createLeafMapping(initializer, lambdaExpression, parameterToArgumentMap, false);
															operationBodyMapper.addMapping(mapping);
															String before = argument.substring(lambdaArrow.length());
															String after = parameterName + supplierGet;
															Replacement r = new Replacement(before, after, ReplacementType.LAMBDA_EXPRESSION_ARGUMENT);
															replacementInfo.addReplacement(r);
															return true;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static boolean isExpressionOfAnotherMethodInvocation(AbstractCall invocation, Map<String, List<AbstractCall>> invocationMap) {
		for(String key : invocationMap.keySet()) {
			List<? extends AbstractCall> invocations = invocationMap.get(key);
			for(AbstractCall call : invocations) {
				if(!call.equals(invocation) && call.getExpression() != null && call.getExpression().equals(invocation.actualString())) {
					for(String argument : call.arguments()) {
						if(invocationMap.containsKey(argument)) {
							List<? extends AbstractCall> argumentInvocations = invocationMap.get(argument);
							for(AbstractCall argumentCall : argumentInvocations) {
								if(argumentCall.identicalName(invocation) && argumentCall.equalArguments(invocation)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static boolean matchPairOfRemovedAddedOperationsWithIdenticalBody(AbstractCall call1, AbstractCall call2, UMLOperationBodyMapper operationBodyMapper) {
		VariableDeclarationContainer container1 = operationBodyMapper.getContainer1();
		VariableDeclarationContainer container2 = operationBodyMapper.getContainer2();
		UMLAbstractClassDiff classDiff = operationBodyMapper.getClassDiff();
		UMLModelDiff modelDiff = operationBodyMapper.getModelDiff();
		if(classDiff != null) {
			for(UMLOperation removedOperation : classDiff.getRemovedOperations()) {
				if(call1.matchesOperation(removedOperation, container1, classDiff, modelDiff)) {
					for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
						if(removedOperation.getBodyHashCode() == addedOperation.getBodyHashCode()) {
							if(call2.matchesOperation(addedOperation, container2, classDiff, modelDiff)) {
								return true;
							}
						}
					}
				}
			}
			for(UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
				if(mapper.getContainer1().getBodyHashCode() == mapper.getContainer2().getBodyHashCode() &&
						call1.matchesOperation(mapper.getContainer1(), container1, classDiff, modelDiff) &&
						call2.matchesOperation(mapper.getContainer2(), container2, classDiff, modelDiff)) {
					return true;
				}
			}
		}
		return false;
	}

	private static Set<Replacement> processAnonymousAndLambdas(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo,
			AbstractCall invocationCoveringTheEntireStatement1,
			AbstractCall invocationCoveringTheEntireStatement2,
			Map<String, List<AbstractCall>> methodInvocationMap1,
			Map<String, List<AbstractCall>> methodInvocationMap2,
			List<AnonymousClassDeclarationObject> anonymousClassDeclarations1,
			List<AnonymousClassDeclarationObject> anonymousClassDeclarations2,
			List<LambdaExpressionObject> lambdas1,
			List<LambdaExpressionObject> lambdas2,
			UMLOperationBodyMapper operationBodyMapper)
			throws RefactoringMinerTimedOutException {
		VariableDeclarationContainer container1 = operationBodyMapper.getContainer1();
		VariableDeclarationContainer container2 = operationBodyMapper.getContainer2();
		UMLOperationBodyMapper parentMapper = operationBodyMapper.getParentMapper();
		List<UMLOperationBodyMapper> lambdaMappers = replacementInfo.getLambdaMappers();
		boolean replacementAdded = false;
		if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty() && container1 != null && container2 != null) {
			if(anonymousClassDeclarations1.size() == anonymousClassDeclarations2.size()) {
				for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
					AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
					AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(i);
					UMLAnonymousClass anonymousClass1 = operationBodyMapper.findAnonymousClass1(anonymousClassDeclaration1);
					UMLAnonymousClass anonymousClass2 = operationBodyMapper.findAnonymousClass2(anonymousClassDeclaration2);
					boolean anonymousClassDiffFound = false;
					for(UMLAnonymousClassDiff anonymousClassDiff : operationBodyMapper.getAnonymousClassDiffs()) {
						if(anonymousClassDiff.getOriginalClass().equals(anonymousClass1) || anonymousClassDiff.getNextClass().equals(anonymousClass2)) {
							anonymousClassDiffFound = true;
							break;
						}
					}
					if(!anonymousClassDiffFound) {
						replacementAdded = processAnonymous(statement1, statement2, parameterToArgumentMap, replacementInfo,
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2,
								replacementAdded, anonymousClassDeclaration1, anonymousClassDeclaration2, lambdaMappers, operationBodyMapper);
					}
				}
			}
			else {
				for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
					for(int j=0; j<anonymousClassDeclarations2.size(); j++) {
						AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
						AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(j);
						replacementAdded = processAnonymous(statement1, statement2, parameterToArgumentMap, replacementInfo,
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2,
								replacementAdded, anonymousClassDeclaration1, anonymousClassDeclaration2, lambdaMappers, operationBodyMapper);
					}
				}
			}
		}
		else if(anonymousClassDeclarations1.size() == 0 && anonymousClassDeclarations2.size() == 1 && container2 != null && parentMapper == null) {
			AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(0);
			UMLAnonymousClass anonymousClass2 = operationBodyMapper.findAnonymousClass2(anonymousClassDeclaration2);
			if(anonymousClass2.getOperations().size() == 1) {
				UMLOperation anonymousClass2Operation = anonymousClass2.getOperations().get(0);
				if(anonymousClass2Operation.getBody() != null) {
					List<AbstractStatement> statements = anonymousClass2Operation.getBody().getCompositeStatement().getStatements();
					if(statements.size() == 1) {
						AbstractStatement statement = statements.get(0);
						AbstractCall invocation2 = statement.invocationCoveringEntireFragment();
						if(invocation2 != null) {
							for(String key1 : methodInvocationMap1.keySet()) {
								for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
									if(invocation1.identical(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, Collections.emptyList())) {
										Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
												invocation2.actualString(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_ANONYMOUS_CLASS_DECLARATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		else if(anonymousClassDeclarations1.size() == 1 && anonymousClassDeclarations2.size() == 0 && container1 != null && parentMapper == null) {
			AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(0);
			UMLAnonymousClass anonymousClass1 = operationBodyMapper.findAnonymousClass1(anonymousClassDeclaration1);
			if(anonymousClass1.getOperations().size() == 1) {
				UMLOperation anonymousClass1Operation = anonymousClass1.getOperations().get(0);
				if(anonymousClass1Operation.getBody() != null) {
					List<AbstractCodeFragment> statements = anonymousClass1Operation.getBody().getCompositeStatement().getLeaves();
					for(AbstractCodeFragment statement : statements) {
						AbstractCall invocation1 = statement.invocationCoveringEntireFragment();
						if(invocation1 != null) {
							for(String key2 : methodInvocationMap2.keySet()) {
								for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
									if(invocation1.identical(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, Collections.emptyList()) ||
											invocation1.identicalWithInlinedStatements(invocation2, replacementInfo.getReplacements(), parameterToArgumentMap, statements)) {
										Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
												invocation2.actualString(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_ANONYMOUS_CLASS_DECLARATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(!lambdas1.isEmpty() && !lambdas2.isEmpty()) {
			if(lambdas1.size() == lambdas2.size()) {
				for(int i=0; i<lambdas1.size(); i++) {
					LambdaExpressionObject lambda1 = lambdas1.get(i);
					LambdaExpressionObject lambda2 = lambdas2.get(i);
					processLambdas(lambda1, lambda2, replacementInfo, operationBodyMapper);
				}
			}
			else {
				for(int i=0; i<lambdas1.size(); i++) {
					for(int j=0; j<lambdas2.size(); j++) {
						LambdaExpressionObject lambda1 = lambdas1.get(i);
						LambdaExpressionObject lambda2 = lambdas2.get(j);
						processLambdas(lambda1, lambda2, replacementInfo, operationBodyMapper);
					}
				}
			}
		}
		if(anonymousClassDeclarations1.size() >= 1 && container1 != null && lambdas2.size() >= 1) {
			for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
				AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
				UMLAnonymousClass anonymousClass1 = operationBodyMapper.findAnonymousClass1(anonymousClassDeclaration1);
				if(anonymousClass1.getOperations().size() == 1) {
					UMLOperation anonymousClass1Operation = anonymousClass1.getOperations().get(0);
					for(int j=0; j<lambdas2.size(); j++) {
						LambdaExpressionObject lambda2 = lambdas2.get(j);
						UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(anonymousClass1Operation, lambda2, operationBodyMapper);
						int mappings = mapper.mappingsWithoutBlocks();
						if(mappings > 0) {
							int nonMappedElementsT1 = mapper.nonMappedElementsT1();
							int nonMappedElementsT2 = mapper.nonMappedElementsT2();
							if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
									nonMappedElementsT1 == 0 || nonMappedElementsT2 == 0) {
								operationBodyMapper.addAllMappings(mapper.getMappings());
								operationBodyMapper.getNonMappedInnerNodesT1().addAll(mapper.getNonMappedInnerNodesT1());
								operationBodyMapper.getNonMappedInnerNodesT2().addAll(mapper.getNonMappedInnerNodesT2());
								operationBodyMapper.getNonMappedLeavesT1().addAll(mapper.getNonMappedLeavesT1());
								operationBodyMapper.getNonMappedLeavesT2().addAll(mapper.getNonMappedLeavesT2());
								if(operationBodyMapper.getContainer1() != null && operationBodyMapper.getContainer2() != null) {
									ReplaceAnonymousWithLambdaRefactoring ref = new ReplaceAnonymousWithLambdaRefactoring(anonymousClass1, lambda2, container1, container2);
									operationBodyMapper.getRefactoringsAfterPostProcessing().add(ref);
									operationBodyMapper.getRefactoringsAfterPostProcessing().addAll(mapper.getRefactorings());
								}
								Replacement replacement = new Replacement(anonymousClassDeclaration1.toString(), lambda2.toString(), ReplacementType.ANONYMOUS_CLASS_DECLARATION_REPLACED_WITH_LAMBDA);
								replacementInfo.addReplacement(replacement);
								replacementAdded = true;
								replacementInfo.addLambdaMapper(mapper);
							}
						}
					}
				}
			}
		}
		if(replacementAdded) {
			return replacementInfo.getReplacements();
		}
		if(lambdaMappers.size() > 0 && lambdaMappers.size() == lambdas1.size() && lambdaMappers.size() == lambdas2.size()) {
			return replacementInfo.getReplacements();
		}
		return null;
	}

	protected static void processLambdas(LambdaExpressionObject lambda1, LambdaExpressionObject lambda2,
			ReplacementInfo replacementInfo, UMLOperationBodyMapper operationBodyMapper) throws RefactoringMinerTimedOutException {
		UMLAbstractClassDiff classDiff = operationBodyMapper.getClassDiff();
		UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(lambda1, lambda2, operationBodyMapper);
		int mappings = mapper.mappingsWithoutBlocks();
		if(mappings > 0) {
			int nonMappedElementsT1 = mapper.nonMappedElementsT1();
			int nonMappedElementsT2 = mapper.nonMappedElementsT2();
			List<AbstractCall> invocations1 = streamAPICalls(mapper.getNonMappedLeavesT1());
			List<AbstractCall> invocations2 = streamAPICalls(mapper.getNonMappedLeavesT2());
			if(invocations1.size() > 0 && invocations2.size() == 0) {
				nonMappedElementsT2 = nonMappedElementsT2 - ignoredNonMappedElements(invocations1, mapper.getNonMappedLeavesT2(), mapper.getNonMappedInnerNodesT2());
			}
			else if(invocations1.size() == 0 && invocations2.size() > 0) {
				nonMappedElementsT1 = nonMappedElementsT1 - ignoredNonMappedElements(invocations2, mapper.getNonMappedLeavesT1(), mapper.getNonMappedInnerNodesT1());
			}
			if((mappings >= nonMappedElementsT1 && mappings >= nonMappedElementsT2) ||
					nonMappedElementsT1 == 0 || nonMappedElementsT2 == 0 ||
					(classDiff != null && (classDiff.isPartOfMethodExtracted(lambda1, lambda2) || classDiff.isPartOfMethodInlined(lambda1, lambda2)))) {
				operationBodyMapper.addAllMappings(mapper.getMappings());
				operationBodyMapper.getNonMappedInnerNodesT1().addAll(mapper.getNonMappedInnerNodesT1());
				operationBodyMapper.getNonMappedInnerNodesT2().addAll(mapper.getNonMappedInnerNodesT2());
				operationBodyMapper.getNonMappedLeavesT1().addAll(mapper.getNonMappedLeavesT1());
				operationBodyMapper.getNonMappedLeavesT2().addAll(mapper.getNonMappedLeavesT2());
				if(operationBodyMapper.getContainer1() != null && operationBodyMapper.getContainer2() != null) {
					operationBodyMapper.getRefactoringsAfterPostProcessing().addAll(mapper.getRefactorings());
				}
				replacementInfo.addLambdaMapper(mapper);
			}
		}
	}

	protected static boolean streamAPIName(String name) {
		return name.equals("stream") || name.equals("filter") || name.equals("forEach") || name.equals("collect") || name.equals("map") || name.equals("removeIf");
	}

	protected static List<AbstractCall> streamAPICalls(AbstractCodeFragment leaf) {
		List<AbstractCodeFragment> list = new ArrayList<>();
		list.add(leaf);
		return streamAPICalls(list);
	}

	private static List<AbstractCall> streamAPICalls(List<AbstractCodeFragment> leaves) {
		List<AbstractCall> streamAPICalls = new ArrayList<AbstractCall>();
		for(AbstractCodeFragment statement : leaves) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = statement.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && (invocation.actualString().contains(JAVA.LAMBDA_ARROW) ||
					invocation.actualString().contains(JAVA.METHOD_REFERENCE))) {
				for(AbstractCall inv : statement.getMethodInvocations()) {
					if(streamAPIName(inv.getName())) {
						streamAPICalls.add(inv);
					}
				}
			}
		}
		return streamAPICalls;
	}

	private static int ignoredNonMappedElements(List<AbstractCall> invocations, List<AbstractCodeFragment> nonMappedLeaves, List<CompositeStatementObject> nonMappedInnerNodes) {
		int counter = 0;
		for(AbstractCall inv : invocations) {
			if(inv.getName().equals("forEach")) {
				for(CompositeStatementObject comp : nonMappedInnerNodes) {
					if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
						counter++;
					}
				}
				for(AbstractCodeFragment statement : nonMappedLeaves) {
					for(AbstractCall statementInvocation : statement.getMethodInvocations()) {
						if(statementInvocation.getName().equals("iterator") || statementInvocation.getName().equals("next")) {
							counter++;
						}
					}
					for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
						if(declaration.getInitializer() != null) {
							for(LeafExpression numberLiteral : declaration.getInitializer().getNumberLiterals()) {
								if(numberLiteral.getString().equals(declaration.getInitializer().getExpression())) {
									counter++;
									break;
								}
							}
						}
					}
				}
			}
			else if(inv.getName().equals("filter")) {
				for(CompositeStatementObject comp : nonMappedInnerNodes) {
					if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						counter++;
					}
				}
			}
		}
		return counter;
	}

	private static boolean processAnonymous(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo,
			AbstractCall invocationCoveringTheEntireStatement1, AbstractCall invocationCoveringTheEntireStatement2,
			boolean replacementAdded, AnonymousClassDeclarationObject anonymousClassDeclaration1,
			AnonymousClassDeclarationObject anonymousClassDeclaration2, List<UMLOperationBodyMapper> lambdaMappers,
			UMLOperationBodyMapper operationBodyMapper) throws RefactoringMinerTimedOutException {
		VariableDeclarationContainer container1 = operationBodyMapper.getContainer1();
		VariableDeclarationContainer container2 = operationBodyMapper.getContainer2();
		UMLAbstractClassDiff classDiff = operationBodyMapper.getClassDiff();
		UMLModelDiff modelDiff = operationBodyMapper.getModelDiff();
		UMLOperationBodyMapper parentMapper = operationBodyMapper.getParentMapper();
		String statementWithoutAnonymous1 = statementWithoutAnonymous(statement1, anonymousClassDeclaration1, container1);
		String statementWithoutAnonymous2 = statementWithoutAnonymous(statement2, anonymousClassDeclaration2, container2);
		if(replacementInfo.getRawDistance() == 0 || statementWithoutAnonymous1.equals(statementWithoutAnonymous2) || anonymousClassDeclaration1.toString().equals(anonymousClassDeclaration2.toString()) ||
				identicalAfterVariableAndTypeReplacements(statementWithoutAnonymous1, statementWithoutAnonymous2, replacementInfo.getReplacements()) ||
				extractedToVariable(statementWithoutAnonymous1, statementWithoutAnonymous2, statement1, statement2, replacementInfo) ||
				(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				(onlyDifferentInvoker(statementWithoutAnonymous1, statementWithoutAnonymous2, invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2) ||
				invocationCoveringTheEntireStatement1.identical(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap, lambdaMappers) ||
				invocationCoveringTheEntireStatement1.identicalWithOnlyChangesInAnonymousClassArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) ||
				invocationCoveringTheEntireStatement1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) ||
				invocationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap) ||
				invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2()) != null ||
				(invocationCoveringTheEntireStatement1 instanceof ObjectCreation && invocationCoveringTheEntireStatement2 instanceof ObjectCreation && invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2))))) {
			UMLAnonymousClass anonymousClass1 = operationBodyMapper.findAnonymousClass1(anonymousClassDeclaration1);
			UMLAnonymousClass anonymousClass2 = operationBodyMapper.findAnonymousClass2(anonymousClassDeclaration2);
			UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(anonymousClass1, anonymousClass2, classDiff, modelDiff);
			anonymousClassDiff.process();
			List<UMLOperationBodyMapper> matchedOperationMappers = anonymousClassDiff.getOperationBodyMapperList();
			if(matchedOperationMappers.size() > 0) {
				for(UMLOperationBodyMapper mapper : matchedOperationMappers) {
					operationBodyMapper.addAllMappings(mapper.getMappings());
					operationBodyMapper.getNonMappedInnerNodesT1().addAll(mapper.getNonMappedInnerNodesT1());
					operationBodyMapper.getNonMappedInnerNodesT2().addAll(mapper.getNonMappedInnerNodesT2());
					operationBodyMapper.getNonMappedLeavesT1().addAll(mapper.getNonMappedLeavesT1());
					operationBodyMapper.getNonMappedLeavesT2().addAll(mapper.getNonMappedLeavesT2());
				}
				List<Refactoring> anonymousClassDiffRefactorings = anonymousClassDiff.getRefactorings();
				for(Refactoring r : anonymousClassDiffRefactorings) {
					if(r instanceof ExtractOperationRefactoring) {
						UMLOperationBodyMapper childMapper = ((ExtractOperationRefactoring)r).getBodyMapper();
						operationBodyMapper.addAllMappings(childMapper.getMappings());
					}
				}
				operationBodyMapper.getRefactoringsAfterPostProcessing().addAll(anonymousClassDiffRefactorings);
				operationBodyMapper.getAnonymousClassDiffs().add(anonymousClassDiff);
				if(parentMapper != null && statement1 instanceof AbstractExpression && statement2 instanceof AbstractExpression) {
					parentMapper.getAnonymousClassDiffs().add(anonymousClassDiff);
				}
				if(classDiff != null && classDiff.getRemovedAnonymousClasses().contains(anonymousClass1)) {
					classDiff.getRemovedAnonymousClasses().remove(anonymousClass1);
				}
				if(classDiff != null && classDiff.getAddedAnonymousClasses().contains(anonymousClass2)) {
					classDiff.getAddedAnonymousClasses().remove(anonymousClass2);
				}
				if(!anonymousClassDeclaration1.toString().equals(anonymousClassDeclaration2.toString())) {
					Replacement replacement = new Replacement(anonymousClassDeclaration1.toString(), anonymousClassDeclaration2.toString(), ReplacementType.ANONYMOUS_CLASS_DECLARATION);
					replacementInfo.addReplacement(replacement);
				}
				replacementAdded = true;
			}
		}
		return replacementAdded;
	}
}
