package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.VariableReferenceExtractor;

public class UMLAttributeDiff implements UMLDocumentationDiffProvider {
	private UMLAttribute removedAttribute;
	private UMLAttribute addedAttribute;
	private boolean visibilityChanged;
	private boolean typeChanged;
	private boolean qualifiedTypeChanged;
	private boolean renamed;
	private boolean staticChanged;
	private boolean finalChanged;
	private boolean volatileChanged;
	private boolean transientChanged;
	private boolean initializerChanged;
	private List<UMLOperationBodyMapper> operationBodyMapperList;
	private UMLAnnotationListDiff annotationListDiff;
	private UMLOperation addedGetter;
	private UMLOperation addedSetter;
	private UMLOperationBodyMapper mapper;
	private UMLAbstractClassDiff classDiff;
	private UMLModelDiff modelDiff;
	private Optional<UMLJavadocDiff> javadocDiff = Optional.empty();
	private UMLCommentListDiff commentListDiff;

	public UMLAttributeDiff(UMLAttribute removedAttribute, UMLAttribute addedAttribute, UMLAbstractClassDiff classDiff, UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		this.modelDiff = modelDiff;
		init(removedAttribute, addedAttribute, classDiff.getOperationBodyMapperList());
		this.addedGetter = findMethod(classDiff.getAddedOperations(), addedAttribute, getterCondition(addedAttribute));
		if(this.addedGetter != null && !removedAttribute.getName().equals(addedAttribute.getName())) {
			UMLOperation removedGetter = findMethod(classDiff.getRemovedOperations(), removedAttribute, getterCondition(removedAttribute));
			if(removedGetter != null) {
				this.addedGetter = null;
			}
		}
		this.addedSetter = findMethod(classDiff.getAddedOperations(), addedAttribute, setterCondition(addedAttribute));
		if(this.addedSetter != null && !removedAttribute.getName().equals(addedAttribute.getName())) {
			UMLOperation removedSetter = findMethod(classDiff.getRemovedOperations(), removedAttribute, setterCondition(removedAttribute));
			if(removedSetter != null) {
				this.addedSetter = null;
			}
		}
	}

	private Function<UMLOperation, Boolean> getterCondition(UMLAttribute attribute) {
		return (UMLOperation operation) -> {
			UMLParameter returnParameter = operation.getReturnParameter();
			if(returnParameter != null) {
				return operation.isGetter() && (returnParameter.getType().equals(attribute.getType()) ||
						returnParameter.getType().getClassType().equalsIgnoreCase(attribute.getType().getClassType()));
			}
			return false;
		};
	}

	private Function<UMLOperation, Boolean> setterCondition(UMLAttribute attribute) {
		return (UMLOperation operation) -> operation.isSetter() && (operation.getParameterTypeList().get(0).equals(attribute.getType()) ||
				operation.getParameterTypeList().get(0).getClassType().equalsIgnoreCase(attribute.getType().getClassType()));
	}

	private UMLOperation findMethod(List<UMLOperation> operations, UMLAttribute attribute, Function<UMLOperation, Boolean> condition) {
		for(UMLOperation operation : operations) {
			if(!operation.isConstructor() && !operation.hasOverrideAnnotation() && condition.apply(operation)) {
				List<String> variables = operation.getAllVariables();
				if(variables.contains(attribute.getName()) || variables.contains(JAVA.THIS_DOT + attribute.getName())) {
					return operation;
				}
			}
		}
		return null;
	}

	public UMLAttributeDiff(UMLAttribute removedAttribute, UMLAttribute addedAttribute, List<UMLOperationBodyMapper> operationBodyMapperList) throws RefactoringMinerTimedOutException {
		init(removedAttribute, addedAttribute, operationBodyMapperList);
	}

	private void init(UMLAttribute removedAttribute, UMLAttribute addedAttribute,
			List<UMLOperationBodyMapper> operationBodyMapperList) throws RefactoringMinerTimedOutException {
		this.removedAttribute = removedAttribute;
		this.addedAttribute = addedAttribute;
		this.operationBodyMapperList = operationBodyMapperList;
		if(!removedAttribute.getName().equals(addedAttribute.getName()))
			renamed = true;
		if(!removedAttribute.getVisibility().equals(addedAttribute.getVisibility()))
			visibilityChanged = true;
		if(!removedAttribute.getType().equals(addedAttribute.getType()))
			typeChanged = true;
		else if(!removedAttribute.getType().equalsQualified(addedAttribute.getType()))
			qualifiedTypeChanged = true;
		if(!removedAttribute.getType().toString().equals(addedAttribute.getType().toString()))
			typeChanged = true;
		if(removedAttribute.isStatic() != addedAttribute.isStatic())
			staticChanged = true;
		if(removedAttribute.isFinal() != addedAttribute.isFinal())
			finalChanged = true;
		if(removedAttribute.isVolatile() != addedAttribute.isVolatile())
			volatileChanged = true;
		if(removedAttribute.isTransient() != addedAttribute.isTransient())
			transientChanged = true;
		this.annotationListDiff = new UMLAnnotationListDiff(removedAttribute.getAnnotations(), addedAttribute.getAnnotations());
		AbstractExpression initializer1 = removedAttribute.getVariableDeclaration().getInitializer();
		AbstractExpression initializer2 = addedAttribute.getVariableDeclaration().getInitializer();
		if(removedAttribute.getJavadoc() != null && addedAttribute.getJavadoc() != null) {
			UMLJavadocDiff diff = new UMLJavadocDiff(removedAttribute.getJavadoc(), addedAttribute.getJavadoc());
			this.javadocDiff = Optional.of(diff);
		}
		this.commentListDiff = new UMLCommentListDiff(removedAttribute.getComments(), addedAttribute.getComments());
		if(initializer1 != null && initializer2 != null) {
			if(!initializer1.getExpression().equals(initializer2.getExpression())) {
				initializerChanged = true;
			}
			this.mapper = new UMLOperationBodyMapper(removedAttribute, addedAttribute, classDiff, modelDiff);
		}
		else if(initializer1 == null && initializer2 != null) {
			initializerChanged = true;
			for(UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
				if(operationBodyMapper.getContainer1().isConstructor() && operationBodyMapper.getContainer2().isConstructor()) {
					for(AbstractCodeFragment fragment1 : operationBodyMapper.getNonMappedLeavesT1()) {
						String fragment = fragment1.getString();
						if((fragment.startsWith(addedAttribute.getName() + "=") ||
								fragment.startsWith("this." + addedAttribute.getName() + "=")) &&
								fragment.endsWith(";\n")) {
							String variableInitializer = fragment.substring(fragment.indexOf("=")+1, fragment.lastIndexOf(";\n"));
							List<LeafExpression> leafExpressions1 = fragment1.findExpression(variableInitializer);
							if(leafExpressions1.size() == 1) {
								this.mapper = new UMLOperationBodyMapper(fragment1, initializer2, operationBodyMapper.getContainer1(), operationBodyMapper.getContainer2(), classDiff, modelDiff);
							}
						}
					}
				}
			}
		}
		else if(initializer1 != null && initializer2 == null) {
			initializerChanged = true;
			for(UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
				if(operationBodyMapper.getContainer1().isConstructor() && operationBodyMapper.getContainer2().isConstructor()) {
					for(AbstractCodeFragment fragment2 : operationBodyMapper.getNonMappedLeavesT2()) {
						String fragment = fragment2.getString();
						if((fragment.startsWith(removedAttribute.getName() + "=") ||
								fragment.startsWith("this." + removedAttribute.getName() + "=")) &&
								fragment.endsWith(";\n")) {
							String variableInitializer = fragment.substring(fragment.indexOf("=")+1, fragment.lastIndexOf(";\n"));
							List<LeafExpression> leafExpressions2 = fragment2.findExpression(variableInitializer);
							if(leafExpressions2.size() == 1) {
								this.mapper = new UMLOperationBodyMapper(initializer1, fragment2, operationBodyMapper.getContainer1(), operationBodyMapper.getContainer2(), classDiff, modelDiff);
							}
						}
					}
				}
			}
		}
	}

	public UMLAttribute getRemovedAttribute() {
		return removedAttribute;
	}

	public UMLAttribute getAddedAttribute() {
		return addedAttribute;
	}

	public boolean isRenamed() {
		return renamed;
	}

	public boolean isVisibilityChanged() {
		return visibilityChanged;
	}

	public boolean isTypeChanged() {
		return typeChanged;
	}

	public boolean isQualifiedTypeChanged() {
		return qualifiedTypeChanged;
	}

	public Optional<UMLJavadocDiff> getJavadocDiff() {
		return javadocDiff;
	}

	public UMLCommentListDiff getCommentListDiff() {
		return commentListDiff;
	}

	public VariableDeclarationContainer getContainer1() {
		return removedAttribute;
	}

	public VariableDeclarationContainer getContainer2() {
		return addedAttribute;
	}

	public Optional<UMLOperationBodyMapper> getInitializerMapper() {
		return Optional.ofNullable(mapper);
	}

	public Set<AbstractCodeMapping> getInitializerMappings() {
		if(mapper != null)
			return mapper.getMappings();
		return Collections.emptySet();
	}

	public boolean isEmpty() {
		return !visibilityChanged && !staticChanged && !finalChanged && !volatileChanged && !transientChanged && !typeChanged && !renamed && !qualifiedTypeChanged && annotationListDiff.isEmpty() &&
				!initializerChanged;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append("\t").append(removedAttribute).append("\n");
		if(renamed)
			sb.append("\t").append("renamed from " + removedAttribute.getName() + " to " + addedAttribute.getName()).append("\n");
		if(visibilityChanged)
			sb.append("\t").append("visibility changed from " + removedAttribute.getVisibility() + " to " + addedAttribute.getVisibility()).append("\n");
		if(typeChanged || qualifiedTypeChanged)
			sb.append("\t").append("type changed from " + removedAttribute.getType() + " to " + addedAttribute.getType()).append("\n");
		if(staticChanged)
			sb.append("\t").append("modifier changed from " + (removedAttribute.isStatic() ? "static" : "non-static") + " to " +
					(addedAttribute.isStatic() ? "static" : "non-static")).append("\n");
		if(finalChanged)
			sb.append("\t").append("modifier changed from " + (removedAttribute.isFinal() ? "final" : "non-final") + " to " +
					(addedAttribute.isFinal() ? "final" : "non-final")).append("\n");
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

	private Set<Refactoring> getAnnotationRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddAttributeAnnotationRefactoring refactoring = new AddAttributeAnnotationRefactoring(annotation, removedAttribute, addedAttribute);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveAttributeAnnotationRefactoring refactoring = new RemoveAttributeAnnotationRefactoring(annotation, removedAttribute, addedAttribute);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
			ModifyAttributeAnnotationRefactoring refactoring = new ModifyAttributeAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), removedAttribute, addedAttribute);
			refactorings.add(refactoring);
		}
		return refactorings;
	}

	public Set<Refactoring> getRefactorings() throws RefactoringMinerTimedOutException {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		if(changeTypeCondition()) {
			ChangeAttributeTypeRefactoring ref = new ChangeAttributeTypeRefactoring(removedAttribute, addedAttribute,
					VariableReferenceExtractor.findReferences(removedAttribute.getVariableDeclaration(), addedAttribute.getVariableDeclaration(), operationBodyMapperList, classDiff, modelDiff), this);
			refactorings.add(ref);
		}
		refactorings.addAll(getModifierRefactorings());
		refactorings.addAll(getAnnotationRefactorings());
		if(mapper != null) {
			refactorings.addAll(mapper.getRefactorings());
		}
		return refactorings;
	}

	private Set<Refactoring> getModifierRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		if(isVisibilityChanged()) {
			ChangeAttributeAccessModifierRefactoring ref = new ChangeAttributeAccessModifierRefactoring(removedAttribute.getVisibility(), addedAttribute.getVisibility(), removedAttribute, addedAttribute);
			refactorings.add(ref);
		}
		if(encapsulated()) {
			EncapsulateAttributeRefactoring ref = new EncapsulateAttributeRefactoring(removedAttribute, addedAttribute, addedGetter, addedSetter);
			refactorings.add(ref);
		}
		if(finalChanged) {
			if(addedAttribute.isFinal()) {
				AddAttributeModifierRefactoring ref = new AddAttributeModifierRefactoring("final", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
			else if(removedAttribute.isFinal()) {
				RemoveAttributeModifierRefactoring ref = new RemoveAttributeModifierRefactoring("final", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
		}
		if(staticChanged) {
			if(addedAttribute.isStatic()) {
				AddAttributeModifierRefactoring ref = new AddAttributeModifierRefactoring("static", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
			else if(removedAttribute.isStatic()) {
				RemoveAttributeModifierRefactoring ref = new RemoveAttributeModifierRefactoring("static", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
		}
		if(transientChanged) {
			if(addedAttribute.isTransient()) {
				AddAttributeModifierRefactoring ref = new AddAttributeModifierRefactoring("transient", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
			else if(removedAttribute.isTransient()) {
				RemoveAttributeModifierRefactoring ref = new RemoveAttributeModifierRefactoring("transient", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
		}
		if(volatileChanged) {
			if(addedAttribute.isVolatile()) {
				AddAttributeModifierRefactoring ref = new AddAttributeModifierRefactoring("volatile", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
			else if(removedAttribute.isVolatile()) {
				RemoveAttributeModifierRefactoring ref = new RemoveAttributeModifierRefactoring("volatile", removedAttribute, addedAttribute);
				refactorings.add(ref);
			}
		}
		return refactorings;
	}
	
	public Set<Refactoring> getRefactorings(Set<CandidateAttributeRefactoring> set) throws RefactoringMinerTimedOutException {
		Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
		RenameAttributeRefactoring rename = null;
		if(isRenamed()) {
			rename = new RenameAttributeRefactoring(removedAttribute, addedAttribute, set);
			refactorings.add(rename);
		}
		if(changeTypeCondition()) {
			ChangeAttributeTypeRefactoring ref = new ChangeAttributeTypeRefactoring(removedAttribute, addedAttribute,
					VariableReferenceExtractor.findReferences(removedAttribute.getVariableDeclaration(), addedAttribute.getVariableDeclaration(), operationBodyMapperList, classDiff, modelDiff), this);
			refactorings.add(ref);
			if(rename != null) {
				ref.addRelatedRefactoring(rename);
			}
		}
		refactorings.addAll(getModifierRefactorings());
		refactorings.addAll(getAnnotationRefactorings());
		if(mapper != null) {
			refactorings.addAll(mapper.getRefactorings());
		}
		return refactorings;
	}

	public boolean encapsulated() {
		return addedSetter != null || addedGetter != null;
	}

	private boolean changeTypeCondition() {
		return (isTypeChanged() || isQualifiedTypeChanged()) && !enumConstantsDeclaredInTheSameEnumDeclarationType();
	}

	private boolean enumConstantsDeclaredInTheSameEnumDeclarationType() {
		VariableDeclaration removedVariableDeclaration = removedAttribute.getVariableDeclaration();
		VariableDeclaration addedVariableDeclaration = addedAttribute.getVariableDeclaration();
		if(removedVariableDeclaration.isEnumConstant() && addedVariableDeclaration.isEnumConstant() &&
				removedVariableDeclaration.equalType(addedVariableDeclaration)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((removedAttribute == null || removedAttribute.getVariableDeclaration() == null) ? 0 : removedAttribute.getVariableDeclaration().hashCode());
		result = prime * result + ((addedAttribute == null || addedAttribute.getVariableDeclaration() == null) ? 0 : addedAttribute.getVariableDeclaration().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UMLAttributeDiff other = (UMLAttributeDiff)obj;
		if (removedAttribute == null) {
			if (other.removedAttribute != null)
				return false;
		} else if(removedAttribute.getVariableDeclaration() == null) {
			if(other.removedAttribute.getVariableDeclaration() != null)
				return false;
		} else if (!removedAttribute.getVariableDeclaration().equals(other.removedAttribute.getVariableDeclaration()))
			return false;
		if (addedAttribute == null) {
			if (other.addedAttribute != null)
				return false;
		} else if(addedAttribute.getVariableDeclaration() == null) {
			if(other.addedAttribute.getVariableDeclaration() != null)
				return false;
		} else if (!addedAttribute.getVariableDeclaration().equals(other.addedAttribute.getVariableDeclaration()))
			return false;
		return true;
	}
}
