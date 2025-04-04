package gr.uom.java.xmi.diff;

import java.util.LinkedHashSet;
import java.util.Set;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.VariableReferenceExtractor;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class UMLParameterDiff {
	private VariableDeclaration removedParameter;
	private VariableDeclaration addedParameter;
	private VariableDeclarationContainer removedOperation;
	private VariableDeclarationContainer addedOperation;
	private boolean typeChanged;
	private boolean qualifiedTypeChanged;
	private boolean nameChanged;
	private boolean varArgsChanged;
	private boolean finalChanged;
	private Set<AbstractCodeMapping> mappings;
	private UMLAnnotationListDiff annotationListDiff;
	private Set<Refactoring> mapperRefactorings;
	private UMLAbstractClassDiff classDiff;
	private UMLModelDiff modelDiff;

	public UMLParameterDiff(UMLParameter removedParameter, UMLParameter addedParameter,
			VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation,
			Set<AbstractCodeMapping> mappings, Set<Refactoring> mapperRefactorings, UMLAbstractClassDiff classDiff) {
		this.mappings = mappings;
		this.mapperRefactorings = mapperRefactorings;
		this.removedParameter = removedParameter.getVariableDeclaration();
		this.addedParameter = addedParameter.getVariableDeclaration();
		this.removedOperation = removedOperation;
		this.addedOperation = addedOperation;
		this.typeChanged = false;
		this.nameChanged = false;
		if(removedParameter.getType() != null && addedParameter.getType() != null && !removedParameter.getType().equals(addedParameter.getType()))
			typeChanged = true;
		else if(removedParameter.getType() != null && addedParameter.getType() != null && !removedParameter.getType().equalsQualified(addedParameter.getType()))
			qualifiedTypeChanged = true;
		if(!typeChanged && !qualifiedTypeChanged) {
			if(removedParameter.isVarargs() && !addedParameter.isVarargs()) {
				varArgsChanged = true;
			}
			if(!removedParameter.isVarargs() && addedParameter.isVarargs()) {
				varArgsChanged = true;
			}
		}
		if(removedParameter.getType() != null && addedParameter.getType() != null && !removedParameter.getType().toString().equals(addedParameter.getType().toString()))
			typeChanged = true;
		if(!removedParameter.getName().equals(addedParameter.getName()))
			nameChanged = true;
		if(removedParameter.isFinal() != addedParameter.isFinal())
			finalChanged = true;
		this.annotationListDiff = new UMLAnnotationListDiff(removedParameter.getAnnotations(), addedParameter.getAnnotations());
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
	}

	public UMLParameterDiff(VariableDeclaration removedParameter, VariableDeclaration addedParameter,
			VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation,
			Set<AbstractCodeMapping> mappings, Set<Refactoring> mapperRefactorings, UMLAbstractClassDiff classDiff) {
		this.mappings = mappings;
		this.mapperRefactorings = mapperRefactorings;
		this.removedParameter = removedParameter.getVariableDeclaration();
		this.addedParameter = addedParameter.getVariableDeclaration();
		this.removedOperation = removedOperation;
		this.addedOperation = addedOperation;
		this.typeChanged = false;
		this.nameChanged = false;
		if(removedParameter.getType() != null && addedParameter.getType() != null && !removedParameter.getType().equals(addedParameter.getType()))
			typeChanged = true;
		else if(removedParameter.getType() != null && addedParameter.getType() != null && !removedParameter.getType().equalsQualified(addedParameter.getType()))
			qualifiedTypeChanged = true;
		if(!typeChanged && !qualifiedTypeChanged) {
			UMLParameter removedUMLParameter = null;
			for(UMLParameter parameter : removedOperation.getParametersWithoutReturnType()) {
				if(parameter.getVariableDeclaration().equals(removedParameter)) {
					removedUMLParameter = parameter;
					break;
				}
			}
			UMLParameter addedUMLParameter = null;
			for(UMLParameter parameter : addedOperation.getParametersWithoutReturnType()) {
				if(parameter.getVariableDeclaration().equals(addedParameter)) {
					addedUMLParameter = parameter;
					break;
				}
			}
			if(removedUMLParameter != null && addedUMLParameter != null && removedUMLParameter.isVarargs() && !addedUMLParameter.isVarargs()) {
				varArgsChanged = true;
			}
			if(removedUMLParameter != null && addedUMLParameter != null && !removedUMLParameter.isVarargs() && addedUMLParameter.isVarargs()) {
				varArgsChanged = true;
			}
		}
		if(removedParameter.getType() != null && addedParameter.getType() != null && !removedParameter.getType().toString().equals(addedParameter.getType().toString()))
			typeChanged = true;
		if(!removedParameter.getVariableName().equals(addedParameter.getVariableName()))
			nameChanged = true;
		if(removedParameter.isFinal() != addedParameter.isFinal())
			finalChanged = true;
		this.annotationListDiff = new UMLAnnotationListDiff(removedParameter.getAnnotations(), addedParameter.getAnnotations());
		this.classDiff = classDiff;
		this.modelDiff = classDiff != null ? classDiff.getModelDiff() : null;
	}

	public VariableDeclaration getRemovedParameter() {
		return removedParameter;
	}

	public VariableDeclaration getAddedParameter() {
		return addedParameter;
	}

	public boolean isTypeChanged() {
		return typeChanged;
	}

	public boolean isQualifiedTypeChanged() {
		return qualifiedTypeChanged;
	}

	public boolean isVarArgsChanged() {
		return varArgsChanged;
	}

	public boolean isNameChanged() {
		return nameChanged;
	}

	public boolean isEmpty() {
		return !nameChanged && !typeChanged && !qualifiedTypeChanged && !varArgsChanged && !finalChanged && annotationListDiff.isEmpty();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(typeChanged || nameChanged || qualifiedTypeChanged)
			sb.append("\t\t").append("parameter ").append(removedParameter).append(":").append("\n");
		if(typeChanged || qualifiedTypeChanged)
			sb.append("\t\t").append("type changed from " + removedParameter.getType() + " to " + addedParameter.getType()).append("\n");
		if(nameChanged)
			sb.append("\t\t").append("name changed from " + removedParameter.getVariableName() + " to " + addedParameter.getVariableName()).append("\n");
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
		VariableDeclaration originalVariable = getRemovedParameter().getVariableDeclaration();
		VariableDeclaration newVariable = getAddedParameter().getVariableDeclaration();
		Set<AbstractCodeMapping> references = VariableReferenceExtractor.findReferences(originalVariable, newVariable, mappings, classDiff, modelDiff);
		RenameVariableRefactoring renameRefactoring = null;
		if(isNameChanged()) {
			renameRefactoring = new RenameVariableRefactoring(originalVariable, newVariable, removedOperation, addedOperation, references, false);
			if(!inconsistentReplacement(originalVariable, newVariable)) {
				refactorings.add(renameRefactoring);
			}
			else if(!mapperRefactorings.contains(renameRefactoring)) {
				RemoveParameterRefactoring removeParameter = new RemoveParameterRefactoring(removedParameter, removedOperation, addedOperation);
				AddParameterRefactoring addParameter = new AddParameterRefactoring(addedParameter, removedOperation, addedOperation);
				refactorings.add(removeParameter);
				refactorings.add(addParameter);
			}
		}
		if((isTypeChanged() || isQualifiedTypeChanged() || isVarArgsChanged()) && !inconsistentReplacement(originalVariable, newVariable)) {
			ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(originalVariable, newVariable, removedOperation, addedOperation, references, false);
			if(renameRefactoring != null) {
				refactoring.addRelatedRefactoring(renameRefactoring);
			}
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddVariableAnnotationRefactoring refactoring = new AddVariableAnnotationRefactoring(annotation, originalVariable, newVariable, removedOperation, addedOperation, false);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveVariableAnnotationRefactoring refactoring = new RemoveVariableAnnotationRefactoring(annotation, originalVariable, newVariable, removedOperation, addedOperation, false);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
			ModifyVariableAnnotationRefactoring refactoring = new ModifyVariableAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), originalVariable, newVariable, removedOperation, addedOperation, false);
			refactorings.add(refactoring);
		}
		if(finalChanged) {
			if(newVariable.isFinal()) {
				AddVariableModifierRefactoring ref = new AddVariableModifierRefactoring("final", originalVariable, newVariable, removedOperation, addedOperation, false);
				refactorings.add(ref);
			}
			else if(originalVariable.isFinal()) {
				RemoveVariableModifierRefactoring ref = new RemoveVariableModifierRefactoring("final", originalVariable, newVariable, removedOperation, addedOperation, false);
				refactorings.add(ref);
			}
		}
		return refactorings;
	}

	private boolean inconsistentReplacement(VariableDeclaration originalVariable, VariableDeclaration newVariable) {
		if(removedOperation.isStatic() || addedOperation.isStatic()) {
			for(AbstractCodeMapping mapping : mappings) {
				for(Replacement replacement : mapping.getReplacements()) {
					if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
						if(replacement.getBefore().equals(originalVariable.getVariableName()) && !replacement.getAfter().equals(newVariable.getVariableName())) {
							return true;
						}
						else if(!replacement.getBefore().equals(originalVariable.getVariableName()) && replacement.getAfter().equals(newVariable.getVariableName())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
