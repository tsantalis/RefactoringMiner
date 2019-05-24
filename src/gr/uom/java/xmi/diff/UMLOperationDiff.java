package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.VariableScope;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.refactoringminer.api.Refactoring;

public class UMLOperationDiff {
	private UMLOperation removedOperation;
	private UMLOperation addedOperation;
	private List<UMLParameter> addedParameters;
	private List<UMLParameter> removedParameters;
	private List<UMLParameterDiff> parameterDiffList;
	private boolean visibilityChanged;
	private boolean abstractionChanged;
	private boolean returnTypeChanged;
	private boolean operationRenamed;
	private Set<AbstractCodeMapping> mappings = new LinkedHashSet<AbstractCodeMapping>();
	
	public UMLOperationDiff(UMLOperation removedOperation, UMLOperation addedOperation) {
		this.removedOperation = removedOperation;
		this.addedOperation = addedOperation;
		this.addedParameters = new ArrayList<UMLParameter>();
		this.removedParameters = new ArrayList<UMLParameter>();
		this.parameterDiffList = new ArrayList<UMLParameterDiff>();
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
		if(!removedOperation.equalReturnParameter(addedOperation))
			returnTypeChanged = true;
		List<SimpleEntry<UMLParameter, UMLParameter>> matchedParameters = updateAddedRemovedParameters(removedOperation, addedOperation);
		int matchedParameterCount = matchedParameters.size()/2;
		//first round match parameters with the same name
		for(Iterator<UMLParameter> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
			UMLParameter removedParameter = removedParameterIterator.next();
			for(Iterator<UMLParameter> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
				UMLParameter addedParameter = addedParameterIterator.next();
				if(removedParameter.getName().equals(addedParameter.getName())) {
					UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter);
					parameterDiffList.add(parameterDiff);
					addedParameterIterator.remove();
					removedParameterIterator.remove();
					break;
				}
			}
		}
		//second round match parameters with the same type
		for(Iterator<UMLParameter> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
			UMLParameter removedParameter = removedParameterIterator.next();
			for(Iterator<UMLParameter> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
				UMLParameter addedParameter = addedParameterIterator.next();
				if(removedParameter.getType().equalsQualified(addedParameter.getType()) &&
						!existsAnotherAddedParameterWithTheSameType(addedParameter)) {
					UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter);
					parameterDiffList.add(parameterDiff);
					addedParameterIterator.remove();
					removedParameterIterator.remove();
					break;
				}
			}
		}
		//third round match parameters with different type and name
		List<UMLParameter> removedParametersWithoutReturnType = removedOperation.getParametersWithoutReturnType();
		List<UMLParameter> addedParametersWithoutReturnType = addedOperation.getParametersWithoutReturnType();
		if(matchedParameterCount == removedParametersWithoutReturnType.size()-1 && matchedParameterCount == addedParametersWithoutReturnType.size()-1) {
			for(Iterator<UMLParameter> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
				UMLParameter removedParameter = removedParameterIterator.next();
				int indexOfRemovedParameter = removedParametersWithoutReturnType.indexOf(removedParameter);
				for(Iterator<UMLParameter> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
					UMLParameter addedParameter = addedParameterIterator.next();
					int indexOfAddedParameter = addedParametersWithoutReturnType.indexOf(addedParameter);
					if(indexOfRemovedParameter == indexOfAddedParameter) {
						UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter);
						parameterDiffList.add(parameterDiff);
						addedParameterIterator.remove();
						removedParameterIterator.remove();
						break;
					}
				}
			}
		}
	}
	public UMLOperationDiff(UMLOperation removedOperation, UMLOperation addedOperation, Set<AbstractCodeMapping> mappings) {
		this(removedOperation, addedOperation);
		this.mappings = mappings;
	}

	private boolean existsAnotherAddedParameterWithTheSameType(UMLParameter parameter) {
		if(removedOperation.hasTwoParametersWithTheSameType() && addedOperation.hasTwoParametersWithTheSameType()) {
			return false;
		}
		for(UMLParameter addedParameter : addedParameters) {
			if(!addedParameter.getName().equals(parameter.getName()) &&
					addedParameter.getType().equalsQualified(parameter.getType())) {
				return true;
			}
		}
		return false;
	}

	private List<SimpleEntry<UMLParameter, UMLParameter>> updateAddedRemovedParameters(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<SimpleEntry<UMLParameter, UMLParameter>> matchedParameters = new ArrayList<SimpleEntry<UMLParameter, UMLParameter>>();
		for(UMLParameter parameter1 : removedOperation.getParameters()) {
			if(!parameter1.getKind().equals("return")) {
				boolean found = false;
				for(UMLParameter parameter2 : addedOperation.getParameters()) {
					if(parameter1.equalsIncludingName(parameter2)) {
						matchedParameters.add(new SimpleEntry<UMLParameter, UMLParameter>(parameter1, parameter2));
						found = true;
						break;
					}
				}
				if(!found) {
					this.removedParameters.add(parameter1);
				}
			}
		}
		for(UMLParameter parameter1 : addedOperation.getParameters()) {
			if(!parameter1.getKind().equals("return")) {
				boolean found = false;
				for(UMLParameter parameter2 : removedOperation.getParameters()) {
					if(parameter1.equalsIncludingName(parameter2)) {
						matchedParameters.add(new SimpleEntry<UMLParameter, UMLParameter>(parameter2, parameter1));
						found = true;
						break;
					}
				}
				if(!found) {
					this.addedParameters.add(parameter1);
				}
			}
		}
		return matchedParameters;
	}

	public List<UMLParameterDiff> getParameterDiffList() {
		return parameterDiffList;
	}

	public UMLOperation getRemovedOperation() {
		return removedOperation;
	}

	public UMLOperation getAddedOperation() {
		return addedOperation;
	}

	public List<UMLParameter> getAddedParameters() {
		return addedParameters;
	}

	public List<UMLParameter> getRemovedParameters() {
		return removedParameters;
	}

	public boolean isOperationRenamed() {
		return operationRenamed;
	}

	public boolean isEmpty() {
		return addedParameters.isEmpty() && removedParameters.isEmpty() && parameterDiffList.isEmpty() &&
		!visibilityChanged && !abstractionChanged && !returnTypeChanged && !operationRenamed;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append("\t").append(removedOperation).append("\n");
		if(operationRenamed)
			sb.append("\t").append("renamed from " + removedOperation.getName() + " to " + addedOperation.getName()).append("\n");
		if(visibilityChanged)
			sb.append("\t").append("visibility changed from " + removedOperation.getVisibility() + " to " + addedOperation.getVisibility()).append("\n");
		if(abstractionChanged)
			sb.append("\t").append("abstraction changed from " + (removedOperation.isAbstract() ? "abstract" : "concrete") + " to " +
					(addedOperation.isAbstract() ? "abstract" : "concrete")).append("\n");
		if(returnTypeChanged)
			sb.append("\t").append("return type changed from " + removedOperation.getReturnParameter() + " to " + addedOperation.getReturnParameter()).append("\n");
		for(UMLParameter umlParameter : removedParameters) {
			sb.append("\t").append("parameter " + umlParameter + " removed").append("\n");
		}
		for(UMLParameter umlParameter : addedParameters) {
			sb.append("\t").append("parameter " + umlParameter + " added").append("\n");
		}
		for(UMLParameterDiff parameterDiff : parameterDiffList) {
			sb.append(parameterDiff);
		}
		return sb.toString();
	}

	public Set<Refactoring> getRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		if(returnTypeChanged) {
			UMLParameter removedOperationReturnParameter = removedOperation.getReturnParameter();
			UMLParameter addedOperationReturnParameter = addedOperation.getReturnParameter();
			if(removedOperationReturnParameter != null && addedOperationReturnParameter != null) {
				ChangeReturnTypeRefactoring refactoring = new ChangeReturnTypeRefactoring(removedOperationReturnParameter.getType(), addedOperationReturnParameter.getType(), removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
		}
		for(UMLParameterDiff parameterDiff : getParameterDiffList()) {
			VariableDeclaration originalVariable = parameterDiff.getRemovedParameter().getVariableDeclaration();
			VariableDeclaration newVariable = parameterDiff.getAddedParameter().getVariableDeclaration();
			Set<AbstractCodeMapping> references = findReferences(originalVariable, newVariable);
			if(parameterDiff.isNameChanged()) {
				RenameVariableRefactoring refactoring = new RenameVariableRefactoring(originalVariable, newVariable, removedOperation, addedOperation, references);
				refactorings.add(refactoring);
			}
			if(parameterDiff.isTypeChanged()) {
				ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(originalVariable, newVariable, removedOperation, addedOperation, references);
				refactorings.add(refactoring);
			}
		}
		return refactorings;
	}
	
	private Set<AbstractCodeMapping> findReferences(VariableDeclaration declaration1, VariableDeclaration declaration2) {
		Set<AbstractCodeMapping> references = new LinkedHashSet<AbstractCodeMapping>();
		VariableScope scope1 = declaration1.getScope();
		VariableScope scope2 = declaration2.getScope();
		for(AbstractCodeMapping mapping : mappings) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			if(scope1.subsumes(fragment1.getLocationInfo()) && scope2.subsumes(fragment2.getLocationInfo()) &&
					fragment1.getVariables().contains(declaration1.getVariableName()) &&
					fragment2.getVariables().contains(declaration2.getVariableName())) {
				references.add(mapping);
			}
		}
		return references;
	}
}
