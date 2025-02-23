package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.VariableReferenceExtractor;

import java.util.AbstractMap.SimpleEntry;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;

public class UMLOperationDiff {
	private VariableDeclarationContainer removedOperation;
	private VariableDeclarationContainer addedOperation;
	private List<VariableDeclaration> addedParameters = new ArrayList<VariableDeclaration>();
	private List<VariableDeclaration> removedParameters = new ArrayList<VariableDeclaration>();
	private List<UMLParameterDiff> parameterDiffList = new ArrayList<UMLParameterDiff>();
	private boolean visibilityChanged;
	private boolean abstractionChanged;
	private boolean finalChanged;
	private boolean staticChanged;
	private boolean synchronizedChanged;
	private boolean returnTypeChanged;
	private boolean qualifiedReturnTypeChanged;
	private boolean operationRenamed;
	private boolean parametersReordered;
	private Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
	private Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables = new LinkedHashSet<>();
	private Set<Refactoring> refactorings = new LinkedHashSet<>();
	private UMLAbstractClassDiff classDiff;
	private UMLModelDiff modelDiff;
	private UMLAnnotationListDiff annotationListDiff;
	private UMLTypeParameterListDiff typeParameterListDiff;
	private List<UMLType> addedExceptionTypes = new ArrayList<UMLType>();
	private List<UMLType> removedExceptionTypes = new ArrayList<UMLType>();
	private Set<Pair<UMLType, UMLType>> commonExceptionTypes = new LinkedHashSet<Pair<UMLType, UMLType>>();
	private SimpleEntry<Set<UMLType>, Set<UMLType>> changedExceptionTypes;
	
	public UMLOperationDiff(UMLOperation removedOperation, UMLOperation addedOperation, UMLAbstractClassDiff classDiff) {
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		process(removedOperation, addedOperation);
	}
	
	public UMLOperationDiff(LambdaExpressionObject removedLambda, LambdaExpressionObject addedLambda, UMLAbstractClassDiff classDiff) {
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		process(removedLambda, addedLambda);
	}

	private void process(LambdaExpressionObject removedLambda, LambdaExpressionObject addedLambda) {
		this.removedOperation = removedLambda;
		this.addedOperation = addedLambda;
		processParameters(removedLambda, addedLambda);
	}

	private void process(UMLOperation removedOperation, UMLOperation addedOperation) {
		this.removedOperation = removedOperation;
		this.addedOperation = addedOperation;
		this.visibilityChanged = false;
		this.abstractionChanged = false;
		this.returnTypeChanged = false;
		this.operationRenamed = false;
		if(!removedOperation.getName().equals(addedOperation.getName()))
			operationRenamed = true;
		if(!removedOperation.getVisibility().equals(addedOperation.getVisibility()))
			visibilityChanged = true;
		if(removedOperation.isAbstract() != addedOperation.isAbstract())
			abstractionChanged = true;
		if(removedOperation.isFinal() != addedOperation.isFinal())
			finalChanged = true;
		if(removedOperation.isStatic() != addedOperation.isStatic())
			staticChanged = true;
		if(removedOperation.isSynchronized() != addedOperation.isSynchronized())
			synchronizedChanged = true;
		if(!removedOperation.equalReturnParameter(addedOperation))
			returnTypeChanged = true;
		else if(!removedOperation.equalQualifiedReturnParameter(addedOperation))
			qualifiedReturnTypeChanged = true;
		if(removedOperation.getReturnParameter() != null && addedOperation.getReturnParameter() != null &&
				!removedOperation.getReturnParameter().getType().toString().equals(addedOperation.getReturnParameter().getType().toString()))
			returnTypeChanged = true;
		processThrownExceptionTypes(removedOperation.getThrownExceptionTypes(), addedOperation.getThrownExceptionTypes());
		this.annotationListDiff = new UMLAnnotationListDiff(removedOperation.getAnnotations(), addedOperation.getAnnotations());
		this.typeParameterListDiff = new UMLTypeParameterListDiff(removedOperation.getTypeParameters(), addedOperation.getTypeParameters());
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
				if(removedParameter.equalQualifiedType(addedParameter) &&
						!existsAnotherAddedParameterWithTheSameType(addedParameter)) {
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
		//third round match parameters with different type and name
		List<VariableDeclaration> removedParametersWithoutReturnType = removedOperation.getParameterDeclarationList();
		List<VariableDeclaration> addedParametersWithoutReturnType = addedOperation.getParameterDeclarationList();
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

	public UMLOperationDiff(UMLOperationBodyMapper mapper) {
		this.mappings = mapper.getMappings();
		this.matchedVariables = mapper.getMatchedVariables();
		this.refactorings = mapper.getRefactoringsAfterPostProcessing();
		this.classDiff = mapper.getClassDiff();
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
		if(mapper.getContainer1() instanceof UMLOperation && mapper.getContainer2() instanceof UMLOperation) {
			process(mapper.getOperation1(), mapper.getOperation2());
		}
		else if(mapper.getContainer1() instanceof LambdaExpressionObject && mapper.getContainer2() instanceof LambdaExpressionObject) {
			process((LambdaExpressionObject)mapper.getContainer1(), (LambdaExpressionObject)mapper.getContainer2());
		}
	}

	private void processThrownExceptionTypes(List<UMLType> exceptionTypes1, List<UMLType> exceptionTypes2) {
		Set<UMLType> addedExceptionTypes = new LinkedHashSet<UMLType>();
		Set<UMLType> removedExceptionTypes = new LinkedHashSet<UMLType>();
		for(UMLType exceptionType1 : exceptionTypes1) {
			boolean found = false;
			for(UMLType exceptionType2 : exceptionTypes2) {
				if(exceptionType1.equals(exceptionType2) && exceptionType1.getClassType().equals(exceptionType2.getClassType())) {
					found = true;
					Pair<UMLType, UMLType> pair = Pair.of(exceptionType1, exceptionType2);
					if(!commonExceptionTypes.contains(pair)) {
						if(exceptionType1 instanceof LeafType && exceptionType2 instanceof LeafType && !exceptionType1.toString().equals(exceptionType1.toQualifiedString())) {
							UMLType type1 = ((LeafType)exceptionType1).cloneAsQualified();
							UMLType type2 = ((LeafType)exceptionType2).cloneAsQualified();
							commonExceptionTypes.add(Pair.of(type1, type2));
						}
						else {
							commonExceptionTypes.add(pair);
						}
					}
					else if(exceptionType1 instanceof LeafType && exceptionType2 instanceof LeafType) {
						UMLType type1 = ((LeafType)exceptionType1).cloneAsQualified();
						UMLType type2 = ((LeafType)exceptionType2).cloneAsQualified();
						commonExceptionTypes.add(Pair.of(type1, type2));
					}
					break;
				}
			}
			if(!found) {
				removedExceptionTypes.add(exceptionType1);
			}
		}
		for(UMLType exceptionType2 : exceptionTypes2) {
			boolean found = false;
			for(UMLType exceptionType1 : exceptionTypes1) {
				if(exceptionType1.equals(exceptionType2) && exceptionType1.getClassType().equals(exceptionType2.getClassType())) {
					found = true;
					Pair<UMLType, UMLType> pair = Pair.of(exceptionType1, exceptionType2);
					if(!commonExceptionTypes.contains(pair)) {
						if(exceptionType1 instanceof LeafType && exceptionType2 instanceof LeafType && !exceptionType1.toString().equals(exceptionType1.toQualifiedString())) {
							UMLType type1 = ((LeafType)exceptionType1).cloneAsQualified();
							UMLType type2 = ((LeafType)exceptionType2).cloneAsQualified();
							commonExceptionTypes.add(Pair.of(type1, type2));
						}
						else {
							commonExceptionTypes.add(pair);
						}
					}
					else if(exceptionType1 instanceof LeafType && exceptionType2 instanceof LeafType) {
						UMLType type1 = ((LeafType)exceptionType1).cloneAsQualified();
						UMLType type2 = ((LeafType)exceptionType2).cloneAsQualified();
						commonExceptionTypes.add(Pair.of(type1, type2));
					}
					break;
				}
			}
			if(!found) {
				addedExceptionTypes.add(exceptionType2);
			}
		}
		if(removedExceptionTypes.size() > 0 && addedExceptionTypes.size() == 0) {
			this.removedExceptionTypes.addAll(removedExceptionTypes);
		}
		else if(addedExceptionTypes.size() > 0 && removedExceptionTypes.size() == 0) {
			this.addedExceptionTypes.addAll(addedExceptionTypes);
		}
		else if(removedExceptionTypes.size() > 0 && addedExceptionTypes.size() > 0) {
			this.changedExceptionTypes = new SimpleEntry<Set<UMLType>, Set<UMLType>>(removedExceptionTypes, addedExceptionTypes);
		}
	}

	private boolean existsAnotherAddedParameterWithTheSameType(VariableDeclaration parameter) {
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

	public List<UMLParameterDiff> getParameterDiffList() {
		return parameterDiffList;
	}

	public VariableDeclarationContainer getRemovedOperation() {
		return removedOperation;
	}

	public VariableDeclarationContainer getAddedOperation() {
		return addedOperation;
	}

	public List<VariableDeclaration> getAddedParameters() {
		return addedParameters;
	}

	public List<VariableDeclaration> getRemovedParameters() {
		return removedParameters;
	}

	public boolean isOperationRenamed() {
		return operationRenamed;
	}

	public boolean isVisibilityChanged() {
		return visibilityChanged;
	}

	public boolean isAbstractionChanged() {
		return abstractionChanged;
	}

	public boolean isFinalChanged() {
		return finalChanged;
	}

	public boolean isStaticChanged() {
		return staticChanged;
	}

	public boolean isSynchronizedChanged() {
		return synchronizedChanged;
	}

	public boolean isReturnTypeChanged() {
		return returnTypeChanged;
	}

	public boolean isQualifiedReturnTypeChanged() {
		return qualifiedReturnTypeChanged;
	}

	public boolean isParametersReordered() {
		return parametersReordered;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getMatchedVariables() {
		return matchedVariables;
	}

	public UMLAnnotationListDiff getAnnotationListDiff() {
		return annotationListDiff;
	}

	public UMLTypeParameterListDiff getTypeParameterListDiff() {
		return typeParameterListDiff;
	}

	public List<UMLType> getAddedExceptionTypes() {
		return addedExceptionTypes;
	}

	public List<UMLType> getRemovedExceptionTypes() {
		return removedExceptionTypes;
	}

	public Set<Pair<UMLType, UMLType>> getCommonExceptionTypes() {
		return commonExceptionTypes;
	}

	public SimpleEntry<Set<UMLType>, Set<UMLType>> getChangedExceptionTypes() {
		return changedExceptionTypes;
	}

	public boolean isEmpty() {
		return addedParameters.isEmpty() && removedParameters.isEmpty() && parameterDiffList.isEmpty() &&
		!visibilityChanged && !abstractionChanged && !finalChanged && !staticChanged && !synchronizedChanged && !returnTypeChanged && !operationRenamed && annotationListDiff.isEmpty() && typeParameterListDiff.isEmpty();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append("\t").append(removedOperation).append("\n");
		if(operationRenamed)
			sb.append("\t").append("renamed from " + removedOperation.getName() + " to " + addedOperation.getName()).append("\n");
		if(removedOperation instanceof UMLOperation && addedOperation instanceof UMLOperation) {
			UMLOperation removed = (UMLOperation)removedOperation;
			UMLOperation added = (UMLOperation)addedOperation;
			if(visibilityChanged)
				sb.append("\t").append("visibility changed from " + removed.getVisibility() + " to " + added.getVisibility()).append("\n");
			if(abstractionChanged)
				sb.append("\t").append("abstraction changed from " + (removed.isAbstract() ? "abstract" : "concrete") + " to " +
						(added.isAbstract() ? "abstract" : "concrete")).append("\n");
			if(returnTypeChanged || qualifiedReturnTypeChanged)
				sb.append("\t").append("return type changed from " + removed.getReturnParameter() + " to " + added.getReturnParameter()).append("\n");
		}
		for(VariableDeclaration umlParameter : removedParameters) {
			sb.append("\t").append("parameter " + umlParameter + " removed").append("\n");
		}
		for(VariableDeclaration umlParameter : addedParameters) {
			sb.append("\t").append("parameter " + umlParameter + " added").append("\n");
		}
		for(UMLParameterDiff parameterDiff : parameterDiffList) {
			sb.append(parameterDiff);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			sb.append("\t").append("annotation " + annotation + " removed").append("\n");
		}
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			sb.append("\t").append("annotation " + annotation + " added").append("\n");
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
			sb.append("\t").append("annotation " + annotationDiff.getRemovedAnnotation() + " modified to " + annotationDiff.getAddedAnnotation()).append("\n");
		}
		return sb.toString();
	}

	public Set<Refactoring> getRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		if(returnTypeChanged || qualifiedReturnTypeChanged && removedOperation instanceof UMLOperation && addedOperation instanceof UMLOperation) {
			UMLOperation removed = (UMLOperation)removedOperation;
			UMLOperation added = (UMLOperation)addedOperation;
			UMLParameter removedOperationReturnParameter = removed.getReturnParameter();
			UMLParameter addedOperationReturnParameter = added.getReturnParameter();
			if(removedOperationReturnParameter != null && addedOperationReturnParameter != null) {
				Set<AbstractCodeMapping> references = VariableReferenceExtractor.findReturnReferences(mappings);
				ChangeReturnTypeRefactoring refactoring = new ChangeReturnTypeRefactoring(removedOperationReturnParameter.getType(), addedOperationReturnParameter.getType(),
						removedOperation, addedOperation, references);
				refactorings.add(refactoring);
			}
		}
		for(UMLParameterDiff parameterDiff : getParameterDiffList()) {
			boolean conflictFound = false;
			for(Pair<VariableDeclaration, VariableDeclaration> matchedPair : matchedVariables) {
				if(matchedPair.getLeft().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
						!matchedPair.getRight().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
					conflictFound = true;
					if(matchedPair.getLeft().isParameter() && matchedPair.getRight().isLocalVariable()) {
						Refactoring rename = new RenameVariableRefactoring(matchedPair.getLeft(), matchedPair.getRight(), removedOperation, addedOperation,
								VariableReferenceExtractor.findReferences(matchedPair.getLeft(), matchedPair.getRight(), mappings, classDiff, modelDiff), false);
						refactorings.add(rename);
						Refactoring addParameter = new AddParameterRefactoring(parameterDiff.getAddedParameter(), removedOperation, addedOperation);
						refactorings.add(addParameter);
					}
					break;
				}
				if(matchedPair.getRight().equals(parameterDiff.getAddedParameter().getVariableDeclaration()) &&
						!matchedPair.getLeft().equals(parameterDiff.getRemovedParameter().getVariableDeclaration())) {
					conflictFound = true;
					if(matchedPair.getLeft().isLocalVariable() && matchedPair.getRight().isParameter()) {
						Refactoring rename = new RenameVariableRefactoring(matchedPair.getLeft(), matchedPair.getRight(), removedOperation, addedOperation,
								VariableReferenceExtractor.findReferences(matchedPair.getLeft(), matchedPair.getRight(), mappings, classDiff, modelDiff), false);
						refactorings.add(rename);
						Refactoring removeParameter = new RemoveParameterRefactoring(parameterDiff.getRemovedParameter(), removedOperation, addedOperation);
						refactorings.add(removeParameter);
					}
					break;
				}
			}
			for(Refactoring refactoring : this.refactorings) {
				if(refactoring instanceof RenameVariableRefactoring) {
					RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
					if(rename.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							!rename.getRenamedVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
					else if(!rename.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							rename.getRenamedVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
				}
				else if(refactoring instanceof ChangeVariableTypeRefactoring) {
					ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring)refactoring;
					if(changeType.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							!changeType.getChangedTypeVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
					else if(!changeType.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							changeType.getChangedTypeVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
				}
				else if(refactoring instanceof MergeVariableRefactoring) {
					MergeVariableRefactoring merge = (MergeVariableRefactoring)refactoring;
					if(merge.getMergedVariables().contains(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							merge.getNewVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
				}
			}
			if(!conflictFound) {
				refactorings.addAll(parameterDiff.getRefactorings());
			}
		}
		checkForSplitMergeParameterBasedOnAttributeAssignments(refactorings);
		int exactMappings = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.isExact()) {
				exactMappings++;
			}
		}
		if(removedParameters.isEmpty() || exactMappings > 0 ||
				(mappings.size() > 0 && removedOperation.isConstructor() && addedOperation.isConstructor()) ||
				removedOperation.identicalComments(addedOperation)) {
			for(VariableDeclaration umlParameter : addedParameters) {
				boolean conflictFound = false;
				for(Refactoring refactoring : this.refactorings) {
					if(refactoring instanceof RenameVariableRefactoring) {
						RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
						if(rename.getRenamedVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
					else if(refactoring instanceof ChangeVariableTypeRefactoring) {
						ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring)refactoring;
						if(changeType.getChangedTypeVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
				}
				if(!conflictFound) {
					AddParameterRefactoring refactoring = new AddParameterRefactoring(umlParameter, removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
		}
		if(addedParameters.isEmpty() || exactMappings > 0 ||
				(mappings.size() > 0 && removedOperation.isConstructor() && addedOperation.isConstructor()) ||
				removedOperation.identicalComments(addedOperation)) {
			for(VariableDeclaration umlParameter : removedParameters) {
				boolean conflictFound = false;
				for(Refactoring refactoring : this.refactorings) {
					if(refactoring instanceof RenameVariableRefactoring) {
						RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
						if(rename.getOriginalVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
					else if(refactoring instanceof ChangeVariableTypeRefactoring) {
						ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring)refactoring;
						if(changeType.getOriginalVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
				}
				if(!conflictFound) {
					RemoveParameterRefactoring refactoring = new RemoveParameterRefactoring(umlParameter, removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
		}
		if(parametersReordered) {
			ReorderParameterRefactoring refactoring = new ReorderParameterRefactoring(removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddMethodAnnotationRefactoring refactoring = new AddMethodAnnotationRefactoring(annotation, removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveMethodAnnotationRefactoring refactoring = new RemoveMethodAnnotationRefactoring(annotation, removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
			ModifyMethodAnnotationRefactoring refactoring = new ModifyMethodAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		if(removedOperation instanceof UMLOperation && addedOperation instanceof UMLOperation) {
			UMLOperation removed = (UMLOperation)removedOperation;
			UMLOperation added = (UMLOperation)addedOperation;
			for(UMLType exceptionType : addedExceptionTypes) {
				AddThrownExceptionTypeRefactoring refactoring = new AddThrownExceptionTypeRefactoring(exceptionType, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			for(UMLType exceptionType : removedExceptionTypes) {
				RemoveThrownExceptionTypeRefactoring refactoring = new RemoveThrownExceptionTypeRefactoring(exceptionType, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			if(changedExceptionTypes != null) {
				ChangeThrownExceptionTypeRefactoring refactoring = new ChangeThrownExceptionTypeRefactoring(changedExceptionTypes.getKey(), changedExceptionTypes.getValue(), removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			if(visibilityChanged) {
				ChangeOperationAccessModifierRefactoring refactoring = new ChangeOperationAccessModifierRefactoring(removed.getVisibility(), added.getVisibility(), removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			if(finalChanged) {
				if(added.isFinal()) {
					AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("final", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
				else if(removed.isFinal()) {
					RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("final", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
			if(abstractionChanged) {
				if(added.isAbstract()) {
					AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("abstract", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
				else if(removed.isAbstract()) {
					RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("abstract", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
			if(staticChanged) {
				if(addedOperation.isStatic()) {
					AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("static", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
				else if(removedOperation.isStatic()) {
					RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("static", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
			if(synchronizedChanged) {
				if(added.isSynchronized()) {
					AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("synchronized", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
				else if(removed.isSynchronized()) {
					RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("synchronized", removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
		}
		return refactorings;
	}

	private void checkForSplitMergeParameterBasedOnAttributeAssignments(Set<Refactoring> refactorings) {
		if(classDiff != null) {
			List<AbstractCodeFragment> removedOperationLeaves = removedOperation.getBody() != null ? removedOperation.getBody().getCompositeStatement().getLeaves() : Collections.emptyList();
			Map<VariableDeclaration, AbstractCodeFragment> removedFieldAssignmentMap = new LinkedHashMap<>();
			for(VariableDeclaration removedParameter : removedParameters) {
				for(AbstractCodeFragment leaf : removedOperationLeaves) {
					if(leaf.getString().equals(JAVA.THIS_DOT + removedParameter.getVariableName() + JAVA.ASSIGNMENT + removedParameter.getVariableName() + JAVA.STATEMENT_TERMINATION)) {
						removedFieldAssignmentMap.put(removedParameter.getVariableDeclaration(), leaf);
						break;
					}
				}
			}
			List<AbstractCodeFragment> addedOperationLeaves = addedOperation.getBody() != null ? addedOperation.getBody().getCompositeStatement().getLeaves() : Collections.emptyList();
			Map<VariableDeclaration, AbstractCodeFragment> addedFieldAssignmentMap = new LinkedHashMap<>();
			for(VariableDeclaration addedParameter : addedParameters) {
				for(AbstractCodeFragment leaf : addedOperationLeaves) {
					if(leaf.getString().equals(JAVA.THIS_DOT + addedParameter.getVariableName() + JAVA.ASSIGNMENT + addedParameter.getVariableName() + JAVA.STATEMENT_TERMINATION)) {
						addedFieldAssignmentMap.put(addedParameter.getVariableDeclaration(), leaf);
						break;
					}
				}
			}
			int removedAttributes = 0;
			for(UMLAttribute attribute : classDiff.getRemovedAttributes()) {
				for(VariableDeclaration parameter : removedFieldAssignmentMap.keySet()) {
					if(attribute.getName().equals(parameter.getVariableName()) && attribute.getVariableDeclaration().equalType(parameter)) {
						removedAttributes++;
						break;
					}
				}
			}
			int addedAttributes = 0;
			for(UMLAttribute attribute : classDiff.getAddedAttributes()) {
				for(VariableDeclaration parameter : addedFieldAssignmentMap.keySet()) {
					if(attribute.getName().equals(parameter.getVariableName()) && attribute.getVariableDeclaration().equalType(parameter)) {
						addedAttributes++;
						break;
					}
				}
			}
			if(!removedFieldAssignmentMap.isEmpty() && !addedFieldAssignmentMap.isEmpty() &&
					removedAttributes == removedFieldAssignmentMap.size() && addedAttributes == addedFieldAssignmentMap.size()) {
				Set<AbstractCodeMapping> references = new LinkedHashSet<>();
				for(AbstractCodeMapping mapping : mappings) {
					if(removedFieldAssignmentMap.values().contains(mapping.getFragment1()) || addedFieldAssignmentMap.values().contains(mapping.getFragment2())) {
						references.add(mapping);
					}
				}
				if(removedFieldAssignmentMap.size() == 1 && addedFieldAssignmentMap.size() > 1) {
					SplitVariableRefactoring ref = new SplitVariableRefactoring(removedFieldAssignmentMap.keySet().iterator().next(), addedFieldAssignmentMap.keySet(), removedOperation, addedOperation, references, false);
					refactorings.add(ref);
					cleanUpParameters(removedFieldAssignmentMap, addedFieldAssignmentMap);
				}
				if(removedFieldAssignmentMap.size() > 1 && addedFieldAssignmentMap.size() == 1) {
					MergeVariableRefactoring ref = new MergeVariableRefactoring(removedFieldAssignmentMap.keySet(), addedFieldAssignmentMap.keySet().iterator().next(), removedOperation, addedOperation, references, false);
					refactorings.add(ref);
					cleanUpParameters(removedFieldAssignmentMap, addedFieldAssignmentMap);
				}
			}
		}
	}

	private void cleanUpParameters(Map<VariableDeclaration, AbstractCodeFragment> removedFieldAssignmentMap, Map<VariableDeclaration, AbstractCodeFragment> addedFieldAssignmentMap) {
		for(Iterator<VariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
			VariableDeclaration removedParameter = removedParameterIterator.next();
			if(removedFieldAssignmentMap.keySet().contains(removedParameter.getVariableDeclaration())) {
				removedParameterIterator.remove();
			}
		}
		for(Iterator<VariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
			VariableDeclaration addedParameter = addedParameterIterator.next();
			if(addedFieldAssignmentMap.keySet().contains(addedParameter.getVariableDeclaration())) {
				addedParameterIterator.remove();
			}
		}
	}
}
