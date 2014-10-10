package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
		if(!removedOperation.getParameters().equals(addedOperation.getParameters())) {
			if(!removedOperation.equalReturnParameter(addedOperation))
				returnTypeChanged = true;
			for(UMLParameter parameter1 : removedOperation.getParameters()) {
				if(!parameter1.getKind().equals("return")) {
					boolean found = false;
					for(UMLParameter parameter2 : addedOperation.getParameters()) {
						if(parameter1.equalsIncludingName(parameter2)) {
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
							found = true;
							break;
						}
					}
					if(!found) {
						this.addedParameters.add(parameter1);
					}
				}
			}
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
		}
	}

	public UMLOperation getRemovedOperation() {
		return removedOperation;
	}

	public UMLOperation getAddedOperation() {
		return addedOperation;
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
}
