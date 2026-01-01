package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class UMLParameterListDiff {
	private List<VariableDeclaration> addedParameters = new ArrayList<VariableDeclaration>();
	private List<VariableDeclaration> removedParameters = new ArrayList<VariableDeclaration>();
	private List<UMLParameterDiff> parameterDiffList = new ArrayList<UMLParameterDiff>();
	private Set<Pair<VariableDeclaration, VariableDeclaration>> commonParameters = new LinkedHashSet<>();
	private boolean parametersReordered;
	private Set<AbstractCodeMapping> mappings;
	private Set<Refactoring> refactorings;
	private UMLAbstractClassDiff classDiff;

	public UMLParameterListDiff(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation,
			Set<AbstractCodeMapping> mappings, Set<Refactoring> refactorings, UMLAbstractClassDiff classDiff) {
		this.mappings = mappings;
		this.refactorings = refactorings;
		this.classDiff = classDiff;
		processParameters(removedOperation, addedOperation);
	}

	private void processParameters(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation) {
		List<SimpleEntry<VariableDeclaration, VariableDeclaration>> matchedParameters = updateAddedRemovedParameters(removedOperation, addedOperation);
		for(SimpleEntry<VariableDeclaration, VariableDeclaration> matchedParameter : matchedParameters) {
			VariableDeclaration parameter1 = matchedParameter.getKey();
			VariableDeclaration parameter2 = matchedParameter.getValue();
			UMLParameterDiff parameterDiff = new UMLParameterDiff(parameter1, parameter2, removedOperation, addedOperation, mappings, refactorings, classDiff);
			if(!parameterDiff.isEmpty()) {
				parameterDiffList.add(parameterDiff);
			}
			else {
				commonParameters.add(Pair.of(parameter1, parameter2));
			}
		}
		int matchedParameterCount = matchedParameters.size()/2;
		List<String> parameterNames1 = new ArrayList<>(removedOperation.getParameterNameList());
		for(VariableDeclaration removedParameter : removedParameters) {
			parameterNames1.remove(removedParameter.getVariableName());
		}
		List<String> parameterNames2 = new ArrayList<>(addedOperation.getParameterNameList());
		for(VariableDeclaration addedParameter : addedParameters) {
			parameterNames2.remove(addedParameter.getVariableName());
		}
		if(matchedParameterCount == parameterNames1.size() && matchedParameterCount == parameterNames2.size() &&
				parameterNames1.size() == parameterNames2.size() && parameterNames1.size() > 1 && !parameterNames1.equals(parameterNames2)) {
			parametersReordered = true;
		}
		//first round match parameters with the same name
		for(Iterator<VariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
			VariableDeclaration removedParameter = removedParameterIterator.next();
			for(Iterator<VariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
				VariableDeclaration addedParameter = addedParameterIterator.next();
				if(removedParameter.getVariableName().equals(addedParameter.getVariableName())) {
					UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings, refactorings, classDiff);
					if(!parameterDiff.isEmpty()) {
						parameterDiffList.add(parameterDiff);
					}
					addedParameterIterator.remove();
					removedParameterIterator.remove();
					break;
				}
			}
		}
		//second round match parameters with the same type
		for(Iterator<VariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
			VariableDeclaration removedParameter = removedParameterIterator.next();
			for(Iterator<VariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
				VariableDeclaration addedParameter = addedParameterIterator.next();
				if(removedParameter.equalQualifiedType(addedParameter)) {
					if(!existsAnotherAddedParameterWithTheSameType(removedOperation, addedOperation, addedParameter)) {
						UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings, refactorings, classDiff);
						if(!parameterDiff.isEmpty()) {
							parameterDiffList.add(parameterDiff);
						}
						addedParameterIterator.remove();
						removedParameterIterator.remove();
						break;
					}
					else if(removedParameters.size() == addedParameters.size()) {
						boolean matched = false;
						for(AbstractCodeMapping mapping : mappings) {
							Set<Replacement> replacements = mapping.getReplacements();
							for(Replacement r : replacements) {
								if(r.getType().equals(ReplacementType.VARIABLE_NAME) && r.getBefore().equals(removedParameter.getVariableName()) && r.getAfter().equals(addedParameter.getVariableName())) {
									UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings, refactorings, classDiff);
									if(!parameterDiff.isEmpty()) {
										parameterDiffList.add(parameterDiff);
									}
									addedParameterIterator.remove();
									removedParameterIterator.remove();
									matched = true;
									break;
								}
							}
							if(matched) {
								break;
							}
						}
						if(matched) {
							break;
						}
					}
				}
			}
		}
		//third round match parameters with different type and name
		List<VariableDeclaration> removedParametersWithoutReturnType = removedOperation.getParameterDeclarationList();
		List<VariableDeclaration> addedParametersWithoutReturnType = addedOperation.getParameterDeclarationList();
		//this condition allows only for one unmatched parameter
		if(matchedParameterCount == removedParametersWithoutReturnType.size()-1 && matchedParameterCount == addedParametersWithoutReturnType.size()-1) {
			for(Iterator<VariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
				VariableDeclaration removedParameter = removedParameterIterator.next();
				int indexOfRemovedParameter = indexOfParameter(removedParametersWithoutReturnType, removedParameter);
				for(Iterator<VariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
					VariableDeclaration addedParameter = addedParameterIterator.next();
					int indexOfAddedParameter = indexOfParameter(addedParametersWithoutReturnType, addedParameter);
					if(indexOfRemovedParameter == indexOfAddedParameter &&
							usedParameters(removedOperation, addedOperation, removedParameter, addedParameter)) {
						UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings, refactorings, classDiff);
						if(!parameterDiff.isEmpty()) {
							parameterDiffList.add(parameterDiff);
						}
						addedParameterIterator.remove();
						removedParameterIterator.remove();
						break;
					}
				}
			}
		}
		else {
			for(Iterator<VariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
				VariableDeclaration removedParameter = removedParameterIterator.next();
				int indexOfRemovedParameter = indexOfParameter(removedParametersWithoutReturnType, removedParameter);
				for(Iterator<VariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
					VariableDeclaration addedParameter = addedParameterIterator.next();
					int indexOfAddedParameter = indexOfParameter(addedParametersWithoutReturnType, addedParameter);
					if(indexOfRemovedParameter == indexOfAddedParameter &&
							commonTokens(removedParameter.getVariableName(), addedParameter.getVariableName())) {
						UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings, refactorings, classDiff);
						if(!parameterDiff.isEmpty()) {
							parameterDiffList.add(parameterDiff);
						}
						addedParameterIterator.remove();
						removedParameterIterator.remove();
						break;
					}
				}
			}
		}
	}

	private static boolean commonTokens(String name1, String name2) {
		String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(name1);
		String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(name2);
		if(tokens1.length == 1 && tokens2.length == 1 && tokens1[0].contains("_") && tokens2[0].contains("_")) {
			tokens1 = name1.split("_");
			tokens2 = name2.split("_");
		}
		int commonTokens = 0;
		for(String token1 : tokens1) {
			for(String token2 : tokens2) {
				if(token1.equals(token2)) {
					commonTokens++;
				}
			}
		}
		if(commonTokens == Math.min(tokens1.length, tokens2.length)) {
			return true;
		}
		return false;
	}

	private boolean usedParameters(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation,
			VariableDeclaration removedParameter, VariableDeclaration addedParameter) {
		List<String> removedOperationVariables = removedOperation.getAllVariables();
		List<String> addedOperationVariables = addedOperation.getAllVariables();
		if(removedOperationVariables.contains(removedParameter.getVariableName()) ==
				addedOperationVariables.contains(addedParameter.getVariableName())) {
			if(!removedOperation.isConstructor() && !addedOperation.isConstructor()) {
				return !removedOperationVariables.contains(addedParameter.getVariableName()) &&
						!addedOperationVariables.contains(removedParameter.getVariableName());
			}
			else {
				return true;
			}
		}
		return false;
	}

	private int indexOfParameter(List<VariableDeclaration> parameters, VariableDeclaration parameter) {
		int index = 0;
		for(VariableDeclaration p : parameters) {
			if(p.equalType(parameter) && p.getVariableName().equals(parameter.getVariableName())) {
				return index;
			}
			index++;
		}
		return -1;
	}

	private boolean existsAnotherAddedParameterWithTheSameType(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation, VariableDeclaration parameter) {
		if(removedOperation.hasTwoParametersWithTheSameType() && addedOperation.hasTwoParametersWithTheSameType()) {
			return false;
		}
		for(VariableDeclaration addedParameter : addedParameters) {
			if(!addedParameter.getVariableName().equals(parameter.getVariableName()) &&
					addedParameter.equalQualifiedType(parameter)) {
				return true;
			}
		}
		return false;
	}

	private List<SimpleEntry<VariableDeclaration, VariableDeclaration>> updateAddedRemovedParameters(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation) {
		List<SimpleEntry<VariableDeclaration, VariableDeclaration>> matchedParameters = new ArrayList<SimpleEntry<VariableDeclaration, VariableDeclaration>>();
		for(VariableDeclaration parameter1 : removedOperation.getParameterDeclarationList()) {
			boolean found = false;
			for(VariableDeclaration parameter2 : addedOperation.getParameterDeclarationList()) {
				if(parameter1.equalType(parameter2) && parameter1.getVariableName().equals(parameter2.getVariableName())) {
					matchedParameters.add(new SimpleEntry<VariableDeclaration, VariableDeclaration>(parameter1, parameter2));
					found = true;
					break;
				}
			}
			if(!found) {
				this.removedParameters.add(parameter1);
			}
		}
		for(VariableDeclaration parameter1 : addedOperation.getParameterDeclarationList()) {
			boolean found = false;
			for(VariableDeclaration parameter2 : removedOperation.getParameterDeclarationList()) {
				if(parameter1.equalType(parameter2) && parameter1.getVariableName().equals(parameter2.getVariableName())) {
					matchedParameters.add(new SimpleEntry<VariableDeclaration, VariableDeclaration>(parameter2, parameter1));
					found = true;
					break;
				}
			}
			if(!found) {
				this.addedParameters.add(parameter1);
			}
		}
		return matchedParameters;
	}

	public List<VariableDeclaration> getAddedParameters() {
		return addedParameters;
	}

	public List<VariableDeclaration> getRemovedParameters() {
		return removedParameters;
	}

	public List<UMLParameterDiff> getParameterDiffList() {
		return parameterDiffList;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getCommonParameters() {
		return commonParameters;
	}

	public boolean isParametersReordered() {
		return parametersReordered;
	}
}
