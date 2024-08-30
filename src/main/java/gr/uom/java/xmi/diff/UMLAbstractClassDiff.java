package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.SplitVariableReplacement;

public abstract class UMLAbstractClassDiff {
	protected List<UMLOperation> addedOperations;
	protected List<UMLOperation> removedOperations;
	protected List<UMLInitializer> addedInitializers;
	protected List<UMLInitializer> removedInitializers;
	protected List<UMLAttribute> addedAttributes;
	protected List<UMLAttribute> removedAttributes;
	protected List<UMLOperationBodyMapper> operationBodyMapperList;
	protected List<UMLAttributeDiff> attributeDiffList;
	protected List<UMLEnumConstantDiff> enumConstantDiffList;
	protected Set<Pair<UMLAttribute, UMLAttribute>> commonAtrributes;
	protected Set<Pair<UMLEnumConstant, UMLEnumConstant>> commonEnumConstants;
	protected UMLAbstractClass originalClass;
	protected UMLAbstractClass nextClass;
	protected List<UMLEnumConstant> addedEnumConstants;
	protected List<UMLEnumConstant> removedEnumConstants;
	protected List<UMLAnonymousClass> addedAnonymousClasses;
	protected List<UMLAnonymousClass> removedAnonymousClasses;
	protected Set<CandidateMergeMethodRefactoring> candidateMethodMerges = new LinkedHashSet<CandidateMergeMethodRefactoring>();
	protected Set<CandidateSplitMethodRefactoring> candidateMethodSplits = new LinkedHashSet<CandidateSplitMethodRefactoring>();
	private Set<CandidateAttributeRefactoring> candidateAttributeRenames = new LinkedHashSet<CandidateAttributeRefactoring>();
	private Set<CandidateMergeVariableRefactoring> candidateAttributeMerges = new LinkedHashSet<CandidateMergeVariableRefactoring>();
	private Set<CandidateSplitVariableRefactoring> candidateAttributeSplits = new LinkedHashSet<CandidateSplitVariableRefactoring>();
	private Map<Replacement, Set<CandidateAttributeRefactoring>> renameMap = new LinkedHashMap<Replacement, Set<CandidateAttributeRefactoring>>();
	private Map<MergeVariableReplacement, Set<CandidateMergeVariableRefactoring>> mergeMap = new LinkedHashMap<MergeVariableReplacement, Set<CandidateMergeVariableRefactoring>>();
	private Map<SplitVariableReplacement, Set<CandidateSplitVariableRefactoring>> splitMap = new LinkedHashMap<SplitVariableReplacement, Set<CandidateSplitVariableRefactoring>>();
	protected List<Refactoring> refactorings;
	protected UMLModelDiff modelDiff;
	protected UMLAnnotationListDiff annotationListDiff;
	private UMLImplementedInterfaceListDiff interfaceListDiff;
	private UMLCommentListDiff commentListDiff;
	private static final List<String> collectionAPINames = List.of("get", "add", "contains", "put", "putAll", "addAll", "equals");
	
	public UMLAbstractClassDiff(UMLAbstractClass originalClass, UMLAbstractClass nextClass, UMLModelDiff modelDiff) {
		this.addedOperations = new ArrayList<UMLOperation>();
		this.removedOperations = new ArrayList<UMLOperation>();
		this.addedInitializers = new ArrayList<UMLInitializer>();
		this.removedInitializers = new ArrayList<UMLInitializer>();
		this.addedAttributes = new ArrayList<UMLAttribute>();
		this.removedAttributes = new ArrayList<UMLAttribute>();
		this.addedEnumConstants = new ArrayList<UMLEnumConstant>();
		this.removedEnumConstants = new ArrayList<UMLEnumConstant>();
		this.addedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.removedAnonymousClasses = new ArrayList<UMLAnonymousClass>();
		this.operationBodyMapperList = new ArrayList<UMLOperationBodyMapper>();
		this.attributeDiffList = new ArrayList<UMLAttributeDiff>();
		this.enumConstantDiffList = new ArrayList<UMLEnumConstantDiff>();
		this.commonAtrributes = new LinkedHashSet<Pair<UMLAttribute, UMLAttribute>>();
		this.commonEnumConstants = new LinkedHashSet<Pair<UMLEnumConstant, UMLEnumConstant>>();
		this.refactorings = new ArrayList<Refactoring>();
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.modelDiff = modelDiff;
		this.interfaceListDiff = new UMLImplementedInterfaceListDiff(originalClass.getImplementedInterfaces(), nextClass.getImplementedInterfaces());
		processAnnotations();
	}

	public List<UMLOperation> getAddedOperations() {
		return addedOperations;
	}

	public List<UMLOperation> getRemovedOperations() {
		return removedOperations;
	}

	public List<UMLInitializer> getAddedInitializers() {
		return addedInitializers;
	}

	public List<UMLInitializer> getRemovedInitializers() {
		return removedInitializers;
	}

	public List<UMLEnumConstant> getAddedEnumConstants() {
		return addedEnumConstants;
	}

	public List<UMLEnumConstant> getRemovedEnumConstants() {
		return removedEnumConstants;
	}

	public List<UMLAttribute> getAddedAttributes() {
		return addedAttributes;
	}

	public List<UMLAttribute> getRemovedAttributes() {
		return removedAttributes;
	}

	public List<UMLAnonymousClass> getAddedAnonymousClasses() {
		return addedAnonymousClasses;
	}

	public List<UMLAnonymousClass> getRemovedAnonymousClasses() {
		return removedAnonymousClasses;
	}

	public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
		return operationBodyMapperList;
	}

	public List<UMLAttributeDiff> getAttributeDiffList() {
		return attributeDiffList;
	}

	public Set<Pair<UMLAttribute, UMLAttribute>> getCommonAtrributes() {
		return commonAtrributes;
	}

	public List<UMLEnumConstantDiff> getEnumConstantDiffList() {
		return enumConstantDiffList;
	}

	public Set<Pair<UMLEnumConstant, UMLEnumConstant>> getCommonEnumConstants() {
		return commonEnumConstants;
	}

	public void reportAddedAnonymousClass(UMLAnonymousClass umlClass) {
		this.addedAnonymousClasses.add(umlClass);
	}

	public void reportRemovedAnonymousClass(UMLAnonymousClass umlClass) {
		this.removedAnonymousClasses.add(umlClass);
	}

	public void addOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper) {
		this.operationBodyMapperList.add(operationBodyMapper);
	}

	public UMLModelDiff getModelDiff() {
		return modelDiff;
	}

	protected void processAnnotations() {
		this.annotationListDiff = new UMLAnnotationListDiff(originalClass.getAnnotations(), nextClass.getAnnotations());
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddClassAnnotationRefactoring refactoring = new AddClassAnnotationRefactoring(annotation, originalClass, nextClass);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveClassAnnotationRefactoring refactoring = new RemoveClassAnnotationRefactoring(annotation, originalClass, nextClass);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffs()) {
			ModifyClassAnnotationRefactoring refactoring = new ModifyClassAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), originalClass, nextClass);
			refactorings.add(refactoring);
		}
	}

	public UMLAnnotationListDiff getAnnotationListDiff() {
		return annotationListDiff;
	}

	public UMLCommentListDiff getCommentListDiff() {
		if(commentListDiff == null)
			commentListDiff = new UMLCommentListDiff(originalClass.getComments(), nextClass.getComments());
		return commentListDiff;
	}

	public UMLImplementedInterfaceListDiff getInterfaceListDiff() {
		return interfaceListDiff;
	}

	public void findInterfaceChanges(String nameBefore, String nameAfter) {
		interfaceListDiff.findInterfaceChanges(nameBefore, nameAfter);
	}

	public void findInterfaceChanges(UMLType typeBefore, UMLType typeAfter) {
		interfaceListDiff.findInterfaceChanges(typeBefore, typeAfter);
	}

	public boolean hasBothAddedAndRemovedInterfaces() {
		if(interfaceListDiff != null) {
			return interfaceListDiff.getAddedInterfaces().size() > 0 && interfaceListDiff.getRemovedInterfaces().size() > 0;
		}
		return false;
	}

	protected boolean mapperListContainsOperation(UMLOperation operation1, UMLOperation operation2) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getContainer1().equals(operation1) || mapper.getContainer2().equals(operation2))
				return true;
		}
		return false;
	}

	protected boolean containsMapperForOperation1(UMLOperation operation) {
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if(mapper.getContainer1().equals(operation)) {
				return true;
			}
		}
		return false;
	}

	protected boolean containsMapperForOperation2(UMLOperation operation) {
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if(mapper.getContainer2().equals(operation)) {
				return true;
			}
		}
		return false;
	}

	public UMLOperation matchesOperation(AbstractCall invocation, List<UMLOperation> operations, VariableDeclarationContainer callerOperation) {
		for(UMLOperation operation : operations) {
			if(invocation.matchesOperation(operation, callerOperation, this, modelDiff))
				return operation;
		}
		return null;
	}

	public Set<Replacement> getReplacementsOfType(ReplacementType type) {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			replacements.addAll(mapper.getReplacementsOfType(type));
		}
		for(UMLAttributeDiff diff : getAttributeDiffList()) {
			if(diff.getInitializerMapper().isPresent()) {
				UMLOperationBodyMapper initializerMapper = diff.getInitializerMapper().get();
				replacements.addAll(initializerMapper.getReplacementsOfType(type));
			}
		}
		return replacements;
	}

	public abstract Optional<UMLJavadocDiff> getJavadocDiff();

	public abstract Optional<UMLJavadocDiff> getPackageDeclarationJavadocDiff();

	public abstract UMLCommentListDiff getPackageDeclarationCommentListDiff();

	public abstract void process() throws RefactoringMinerTimedOutException;
	
	protected abstract void checkForAttributeChanges() throws RefactoringMinerTimedOutException;

	protected abstract void createBodyMappers() throws RefactoringMinerTimedOutException;

	protected boolean isPartOfMethodMovedToAddedMethod(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation, UMLOperationBodyMapper operationBodyMapper) {
		if(removedOperations.size() != addedOperations.size()) {
			if(removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation() && addedOperation.getName().contains(removedOperation.getName())) {
				List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
				List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
				Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
				movedInvocations.removeAll(addedOperationInvocations);
				if(movedInvocations.size() > 0) {
					for(UMLOperation insertedOperation : addedOperations) {
						if(!insertedOperation.equals(addedOperation)) {
							Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(movedInvocations);
							intersection.retainAll(insertedOperation.getAllOperationInvocations());
							for(Iterator<AbstractCall> operationInvocationIterator = intersection.iterator(); operationInvocationIterator.hasNext();) {
								AbstractCall invocation = operationInvocationIterator.next();
								boolean lambdaGet = invocation.getName().equals("get") && invocation.arguments().size() == 0;
								boolean collectionGet = invocation.getName().startsWith("get") && invocation.arguments().size() == 1;
								if(!lambdaGet && (collectionAPINames.contains(invocation.getName()) || collectionGet)) {
									operationInvocationIterator.remove();
								}
							}
							List<AbstractCall> unmatchedCalls = new ArrayList<AbstractCall>(movedInvocations);
							unmatchedCalls.removeAll(insertedOperation.getAllOperationInvocations());
							if(movedInvocations.containsAll(intersection) && intersection.size() > 0 && intersection.size() >= unmatchedCalls.size()) {
								for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
									unmatchedCalls.removeAll(composite.getMethodInvocations());
								}
								for(AbstractCodeFragment fragment : operationBodyMapper.getNonMappedLeavesT1()) {
									unmatchedCalls.removeAll(fragment.getMethodInvocations());
								}
								if(unmatchedCalls.isEmpty()) {
									CandidateSplitMethodRefactoring newCandidate = new CandidateSplitMethodRefactoring();
									newCandidate.addSplitMethod(addedOperation);
									newCandidate.addSplitMethod(insertedOperation);
									newCandidate.setOriginalMethodBeforeSplit(removedOperation);
									boolean alreadyInCandidates = false;
									for(CandidateSplitMethodRefactoring oldCandidate : candidateMethodSplits) {
										if(newCandidate.equals(oldCandidate)) {
											alreadyInCandidates = true;
											break;
										}
									}
									if(!alreadyInCandidates) {
										candidateMethodSplits.add(newCandidate);
									}
									return true;
								}
							}
						}
					}
				}
			}
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				List<AbstractCall> invocationsCalledInOperation1 = mapper.getContainer1().getAllOperationInvocations();
				List<AbstractCall> invocationsCalledInOperation2 = mapper.getContainer2().getAllOperationInvocations();
				Set<AbstractCall> invocationsCalledOnlyInOperation1 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation1);
				Set<AbstractCall> invocationsCalledOnlyInOperation2 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation2);
				invocationsCalledOnlyInOperation1.removeAll(invocationsCalledInOperation2);
				invocationsCalledOnlyInOperation2.removeAll(invocationsCalledInOperation1);
				boolean removedOperationCalledInContainer1 = false;
				for(AbstractCall invocation : invocationsCalledOnlyInOperation1) {
					if(invocation.matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff)) {
						removedOperationCalledInContainer1 = true;
						break;
					}
				}
				boolean addedOperationCalledInContainer2 = false;
				for(AbstractCall invocation : invocationsCalledOnlyInOperation2) {
					if(invocation.matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
						addedOperationCalledInContainer2 = true;
						break;
					}
				}
				if(removedOperationCalledInContainer1 && addedOperationCalledInContainer2) {
					List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
					List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
					Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
					movedInvocations.removeAll(addedOperationInvocations);
					if(movedInvocations.size() > 1) {
						for(UMLOperation insertedOperation : addedOperations) {
							if(!insertedOperation.equals(addedOperation)) {
								Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(movedInvocations);
								intersection.retainAll(insertedOperation.getAllOperationInvocations());
								for(Iterator<AbstractCall> operationInvocationIterator = intersection.iterator(); operationInvocationIterator.hasNext();) {
									AbstractCall invocation = operationInvocationIterator.next();
									boolean lambdaGet = invocation.getName().equals("get") && invocation.arguments().size() == 0;
									boolean collectionGet = invocation.getName().startsWith("get") && invocation.arguments().size() == 1;
									if(!lambdaGet && (collectionAPINames.contains(invocation.getName()) || collectionGet)) {
										operationInvocationIterator.remove();
									}
								}
								List<AbstractCall> unmatchedCalls = new ArrayList<AbstractCall>(movedInvocations);
								unmatchedCalls.removeAll(insertedOperation.getAllOperationInvocations());
								if(movedInvocations.containsAll(intersection) && intersection.size() > 1 && intersection.size() >= unmatchedCalls.size()) {
									for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
										unmatchedCalls.removeAll(composite.getMethodInvocations());
									}
									for(AbstractCodeFragment fragment : operationBodyMapper.getNonMappedLeavesT1()) {
										unmatchedCalls.removeAll(fragment.getMethodInvocations());
									}
									boolean callsInsertedOperation = false;
									for(AbstractCodeFragment fragment : operationBodyMapper.getNonMappedLeavesT2()) {
										for(AbstractCall call : fragment.getMethodInvocations()) {
											if(call.matchesOperation(insertedOperation, operationBodyMapper.getContainer2(), this, modelDiff)) {
												callsInsertedOperation = true;
												break;
											}
										}
										if(callsInsertedOperation) {
											break;
										}
									}
									if(!callsInsertedOperation) {
										for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
											for(AbstractCall call : composite.getMethodInvocations()) {
												if(call.matchesOperation(insertedOperation, operationBodyMapper.getContainer2(), this, modelDiff)) {
													callsInsertedOperation = true;
													break;
												}
											}
											if(callsInsertedOperation) {
												break;
											}
										}
									}
									if(unmatchedCalls.isEmpty() && !callsInsertedOperation) {
										CandidateSplitMethodRefactoring newCandidate = new CandidateSplitMethodRefactoring();
										newCandidate.addSplitMethod(addedOperation);
										newCandidate.addSplitMethod(insertedOperation);
										newCandidate.setOriginalMethodBeforeSplit(removedOperation);
										boolean alreadyInCandidates = false;
										for(CandidateSplitMethodRefactoring oldCandidate : candidateMethodSplits) {
											if(newCandidate.equals(oldCandidate)) {
												alreadyInCandidates = true;
												break;
											}
										}
										if(!alreadyInCandidates) {
											candidateMethodSplits.add(newCandidate);
										}
										return true;
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

	protected boolean isPartOfMethodMovedFromDeletedMethod(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation, UMLOperationBodyMapper operationBodyMapper) {
		if(removedOperations.size() != addedOperations.size()) {
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				List<AbstractCall> invocationsCalledInOperation1 = mapper.getContainer1().getAllOperationInvocations();
				List<AbstractCall> invocationsCalledInOperation2 = mapper.getContainer2().getAllOperationInvocations();
				Set<AbstractCall> invocationsCalledOnlyInOperation1 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation1);
				Set<AbstractCall> invocationsCalledOnlyInOperation2 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation2);
				invocationsCalledOnlyInOperation1.removeAll(invocationsCalledInOperation2);
				invocationsCalledOnlyInOperation2.removeAll(invocationsCalledInOperation1);
				boolean removedOperationCalledInContainer1 = false;
				for(AbstractCall invocation : invocationsCalledOnlyInOperation1) {
					if(invocation.matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff)) {
						removedOperationCalledInContainer1 = true;
						break;
					}
				}
				boolean addedOperationCalledInContainer2 = false;
				for(AbstractCall invocation : invocationsCalledOnlyInOperation2) {
					if(invocation.matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
						addedOperationCalledInContainer2 = true;
						break;
					}
				}
				if(removedOperationCalledInContainer1 && addedOperationCalledInContainer2) {
					List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
					List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
					Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
					boolean identicalOperationInvocations = removedOperationInvocations.equals(addedOperationInvocations);
					if(!identicalOperationInvocations) {
						movedInvocations.removeAll(removedOperationInvocations);
					}
					if(movedInvocations.size() > 1) {
						for(UMLOperation deletedOperation : removedOperations) {
							if(!deletedOperation.equals(removedOperation)) {
								Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(movedInvocations);
								intersection.retainAll(deletedOperation.getAllOperationInvocations());
								for(Iterator<AbstractCall> operationInvocationIterator = intersection.iterator(); operationInvocationIterator.hasNext();) {
									AbstractCall invocation = operationInvocationIterator.next();
									boolean lambdaGet = invocation.getName().equals("get") && invocation.arguments().size() == 0;
									boolean collectionGet = invocation.getName().startsWith("get") && invocation.arguments().size() == 1;
									if(!lambdaGet && (collectionAPINames.contains(invocation.getName()) || collectionGet)) {
										operationInvocationIterator.remove();
									}
								}
								List<AbstractCall> unmatchedCalls = new ArrayList<AbstractCall>(movedInvocations);
								unmatchedCalls.removeAll(deletedOperation.getAllOperationInvocations());
								if(movedInvocations.containsAll(intersection) && intersection.size() > 1 && intersection.size() >= unmatchedCalls.size()) {
									for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
										unmatchedCalls.removeAll(composite.getMethodInvocations());
									}
									for(AbstractCodeFragment fragment : operationBodyMapper.getNonMappedLeavesT2()) {
										unmatchedCalls.removeAll(fragment.getMethodInvocations());
									}
									boolean callsDeletedOperation = false;
									for(AbstractCodeFragment fragment : operationBodyMapper.getNonMappedLeavesT1()) {
										for(AbstractCall call : fragment.getMethodInvocations()) {
											if(call.matchesOperation(deletedOperation, operationBodyMapper.getContainer1(), this, modelDiff)) {
												callsDeletedOperation = true;
												break;
											}
										}
										if(callsDeletedOperation) {
											break;
										}
									}
									if(!callsDeletedOperation) {
										for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
											for(AbstractCall call : composite.getMethodInvocations()) {
												if(call.matchesOperation(deletedOperation, operationBodyMapper.getContainer1(), this, modelDiff)) {
													callsDeletedOperation = true;
													break;
												}
											}
											if(callsDeletedOperation) {
												break;
											}
										}
									}
									if(unmatchedCalls.isEmpty() && !callsDeletedOperation) {
										boolean addedOperationHasAdditionalParameters = false;
										if(identicalOperationInvocations) {
											//check if addedOperation has additional parameters
											List<UMLType> addedOperationParameterTypes = addedOperation.getParameterTypeList();
											List<UMLType> removedOperationParameterTypes = removedOperation.getParameterTypeList();
											List<UMLType> deletedOperationParameterTypes = deletedOperation.getParameterTypeList();
											if(addedOperationParameterTypes.containsAll(removedOperationParameterTypes) &&
													addedOperationParameterTypes.containsAll(deletedOperationParameterTypes) &&
													addedOperationParameterTypes.size() > removedOperationParameterTypes.size() &&
													addedOperationParameterTypes.size() > deletedOperationParameterTypes.size()) {
												addedOperationHasAdditionalParameters = true;
											}
										}
										if(!identicalOperationInvocations || addedOperationHasAdditionalParameters) {
											CandidateMergeMethodRefactoring newCandidate = new CandidateMergeMethodRefactoring();
											newCandidate.addMergedMethod(removedOperation);
											newCandidate.addMergedMethod(deletedOperation);
											newCandidate.setNewMethodAfterMerge(addedOperation);
											boolean alreadyInCandidates = false;
											for(CandidateMergeMethodRefactoring oldCandidate : candidateMethodMerges) {
												if(newCandidate.equals(oldCandidate)) {
													alreadyInCandidates = true;
													break;
												}
											}
											if(!alreadyInCandidates) {
												candidateMethodMerges.add(newCandidate);
											}
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

	protected boolean isPartOfMethodMovedFromExistingMethod(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			List<AbstractCall> invocationsCalledInOperation1 = mapper.getContainer1().getAllOperationInvocations();
			List<AbstractCall> invocationsCalledInOperation2 = mapper.getContainer2().getAllOperationInvocations();
			Set<AbstractCall> invocationsCalledOnlyInOperation1 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation1);
			Set<AbstractCall> invocationsCalledOnlyInOperation2 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation1.removeAll(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation2.removeAll(invocationsCalledInOperation1);
			for(AbstractCall invocation : invocationsCalledOnlyInOperation2) {
				if(invocation.matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
					List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
					List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
					Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
					movedInvocations.removeAll(removedOperationInvocations);
					movedInvocations.retainAll(invocationsCalledOnlyInOperation1);
					Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
					intersection.retainAll(removedOperationInvocations);
					int chainedCalls = 0;
					AbstractCall previous = null;
					for(AbstractCall inv : intersection) {
						if(previous != null && previous.getExpression() != null && previous.getExpression().equals(inv.actualString())) {
							chainedCalls++;
						}
						previous = inv;
					}
					if(movedInvocations.size() > 1 && intersection.size() - chainedCalls > 1) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean isPartOfMethodMovedToExistingMethod(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			List<AbstractCall> invocationsCalledInOperation1 = mapper.getContainer1().getAllOperationInvocations();
			List<AbstractCall> invocationsCalledInOperation2 = mapper.getContainer2().getAllOperationInvocations();
			Set<AbstractCall> invocationsCalledOnlyInOperation1 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation1);
			Set<AbstractCall> invocationsCalledOnlyInOperation2 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation1.removeAll(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation2.removeAll(invocationsCalledInOperation1);
			for(AbstractCall invocation : invocationsCalledOnlyInOperation1) {
				if(invocation.matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff)) {
					List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
					List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
					Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
					movedInvocations.removeAll(addedOperationInvocations);
					movedInvocations.retainAll(invocationsCalledOnlyInOperation2);
					Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
					intersection.retainAll(addedOperationInvocations);
					int chainedCalls = 0;
					AbstractCall previous = null;
					for(AbstractCall inv : intersection) {
						if(previous != null && previous.getExpression() != null && previous.getExpression().equals(inv.actualString())) {
							chainedCalls++;
						}
						previous = inv;
					}
					int renamedCalls = 0;
					for(AbstractCall inv : addedOperationInvocations) {
						if(!intersection.contains(inv)) {
							for(Refactoring ref : refactorings) {
								if(ref instanceof RenameOperationRefactoring) {
									RenameOperationRefactoring rename = (RenameOperationRefactoring)ref;
									if(inv.matchesOperation(rename.getRenamedOperation(), addedOperation, this, modelDiff)) {
										renamedCalls++;
										break;
									}
								}
							}
						}
					}
					if(movedInvocations.size() > 1 && intersection.size() + renamedCalls - chainedCalls > 1) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isPartOfMethodExtracted(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation) {
		List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromRemovedOperation = new LinkedHashSet<AbstractCall>(removedOperationInvocations).size() - intersection.size();
		
		Set<String> removedOperationVariableDeclarationNames = getVariableDeclarationNamesInMethodBody(removedOperation);
		Set<String> addedOperationVariableDeclarationNames = getVariableDeclarationNamesInMethodBody(addedOperation);
		Set<String> variableDeclarationIntersection = new LinkedHashSet<String>(removedOperationVariableDeclarationNames);
		variableDeclarationIntersection.retainAll(addedOperationVariableDeclarationNames);
		int numberOfVariableDeclarationsMissingFromRemovedOperation = removedOperationVariableDeclarationNames.size() - variableDeclarationIntersection.size();
		
		Set<AbstractCall> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<AbstractCall>();
		Set<String> variableDeclarationsInMethodsCalledByAddedOperation = new LinkedHashSet<String>();
		Set<AbstractCall> matchedOperationInvocations = new LinkedHashSet<AbstractCall>();
		for(AbstractCall addedOperationInvocation : addedOperationInvocations) {
			if(!intersection.contains(addedOperationInvocation)) {
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						if(addedOperationInvocation.matchesOperation(operation, addedOperation, this, modelDiff)) {
							//addedOperation calls another added method
							operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
							variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
							matchedOperationInvocations.add(addedOperationInvocation);
						}
					}
				}
			}
		}
		//this is to support the Extract & Move Method scenario
		if(modelDiff != null) {
			for(AbstractCall addedOperationInvocation : addedOperationInvocations) {
				String expression = addedOperationInvocation.getExpression();
				if(expression != null && !expression.equals("this") &&
						!intersection.contains(addedOperationInvocation) && !matchedOperationInvocations.contains(addedOperationInvocation)) {
					UMLOperation operation = modelDiff.findOperationInAddedClasses(addedOperationInvocation, addedOperation, this);
					if(operation != null) {
						operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
						variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
					}
				}
			}
		}
		Set<AbstractCall> newIntersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);
		
		Set<String> newVariableDeclarationIntersection = new LinkedHashSet<String>(removedOperationVariableDeclarationNames);
		newVariableDeclarationIntersection.retainAll(variableDeclarationsInMethodsCalledByAddedOperation);
		
		Set<AbstractCall> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<AbstractCall> operationInvocationIterator = removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			AbstractCall invocation = operationInvocationIterator.next();
			if(invocation.getName().startsWith("get") || invocation.getName().equals("add") || invocation.getName().equals("contains")) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		
		int numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations = newVariableDeclarationIntersection.size();
		int numberOfVariableDeclarationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfVariableDeclarationsMissingFromRemovedOperation - numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations;
		
		return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
				numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size() ||
				numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations > numberOfVariableDeclarationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getNextClassName() {
		return nextClass.getName();
	}

	public UMLAbstractClass getOriginalClass() {
		return originalClass;
	}

	public UMLAbstractClass getNextClass() {
		return nextClass;
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

	public List<Refactoring> getRefactorings() throws RefactoringMinerTimedOutException {
		List<Refactoring> originalRefactorings = new ArrayList<Refactoring>(this.refactorings);
		List<Refactoring> refactorings = new ArrayList<Refactoring>(this.refactorings);
		if(!originalClass.getTypeDeclarationKind().equals(nextClass.getTypeDeclarationKind())) {
			boolean anonymousToClass = originalClass.getTypeDeclarationKind().endsWith("class") && nextClass.getTypeDeclarationKind().endsWith("class");
			if(!anonymousToClass) {
				ChangeTypeDeclarationKindRefactoring ref = new ChangeTypeDeclarationKindRefactoring(originalClass.getTypeDeclarationKind(), nextClass.getTypeDeclarationKind(), originalClass, nextClass);
				refactorings.add(ref);
			}
		}
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			processMapperRefactorings(mapper, refactorings);
		}
		refactorings.addAll(inferAttributeMergesAndSplits(renameMap, refactorings));
		for(MergeVariableReplacement merge : mergeMap.keySet()) {
			Set<UMLAttribute> mergedAttributes = new LinkedHashSet<UMLAttribute>();
			Set<VariableDeclaration> mergedVariables = new LinkedHashSet<VariableDeclaration>();
			for(String mergedVariable : merge.getMergedVariables()) {
				UMLAttribute a1 = findAttributeInOriginalClass(mergedVariable);
				if(a1 != null) {
					mergedAttributes.add(a1);
					mergedVariables.add(a1.getVariableDeclaration());
				}
			}
			UMLAttribute a2 = findAttributeInNextClass(merge.getAfter());
			Set<CandidateMergeVariableRefactoring> set = mergeMap.get(merge);
			for(CandidateMergeVariableRefactoring candidate : set) {
				if(mergedVariables.size() > 1 && mergedVariables.size() == merge.getMergedVariables().size() && a2 != null &&
						mergedAttributes.size() > 0 && mergedAttributes.iterator().next().getClassName().equals(a2.getClassName())) {
					MergeAttributeRefactoring ref = new MergeAttributeRefactoring(mergedAttributes, a2, getOriginalClassName(), getNextClassName(), set);
					if(!refactorings.contains(ref)) {
						refactorings.add(ref);
						break;//it's not necessary to repeat the same process for all candidates in the set
					}
				}
				else {
					candidate.setMergedAttributes(mergedAttributes);
					candidate.setNewAttribute(a2);
					candidateAttributeMerges.add(candidate);
				}
			}
		}
		for(SplitVariableReplacement split : splitMap.keySet()) {
			Set<UMLAttribute> splitAttributes = new LinkedHashSet<UMLAttribute>();
			Set<VariableDeclaration> splitVariables = new LinkedHashSet<VariableDeclaration>();
			for(String splitVariable : split.getSplitVariables()) {
				UMLAttribute a2 = findAttributeInNextClass(splitVariable);
				if(a2 != null) {
					splitAttributes.add(a2);
					splitVariables.add(a2.getVariableDeclaration());
				}
			}
			UMLAttribute a1 = findAttributeInOriginalClass(split.getBefore());
			Set<CandidateSplitVariableRefactoring> set = splitMap.get(split);
			for(CandidateSplitVariableRefactoring candidate : set) {
				if(splitVariables.size() > 1 && splitVariables.size() == split.getSplitVariables().size() && a1 != null && findAttributeInNextClass(split.getBefore()) == null) {
					SplitAttributeRefactoring ref = new SplitAttributeRefactoring(a1, splitAttributes, getOriginalClassName(), getNextClassName(), set);
					if(!refactorings.contains(ref)) {
						refactorings.add(ref);
						break;//it's not necessary to repeat the same process for all candidates in the set
					}
				}
				else {
					candidate.setSplitAttributes(splitAttributes);
					candidate.setOldAttribute(a1);
					candidateAttributeSplits.add(candidate);
				}
			}
		}
		Set<Replacement> renames = renameMap.keySet();
		Set<Replacement> allConsistentRenames = new LinkedHashSet<Replacement>();
		Set<Replacement> allInconsistentRenames = new LinkedHashSet<Replacement>();
		Map<String, Set<String>> aliasedAttributesInOriginalClass = originalClass.aliasedAttributes();
		Map<String, Set<String>> aliasedAttributesInNextClass = nextClass.aliasedAttributes();
		ConsistentReplacementDetector.updateRenames(allConsistentRenames, allInconsistentRenames, renames,
				aliasedAttributesInOriginalClass, aliasedAttributesInNextClass, renameMap);
		allConsistentRenames.removeAll(allInconsistentRenames);
		for(Replacement pattern : allConsistentRenames) {
			UMLAttribute a1 = findAttributeInOriginalClass(pattern.getBefore());
			UMLAttribute a2 = findAttributeInNextClass(pattern.getAfter());
			Set<CandidateAttributeRefactoring> set = renameMap.get(pattern);
			for(CandidateAttributeRefactoring candidate : set) {
				if(candidate.getOriginalVariableDeclaration() == null && candidate.getRenamedVariableDeclaration() == null) {
					if(a1 != null && a2 != null) {
						if((!originalClass.containsAttributeWithName(pattern.getAfter()) || cyclicRename(renameMap, pattern)) &&
								(!nextClass.containsAttributeWithName(pattern.getBefore()) || cyclicRename(renameMap, pattern)) &&
								!inconsistentAttributeRename(pattern, aliasedAttributesInOriginalClass, aliasedAttributesInNextClass) &&
								!attributeMerged(a1, a2, refactorings) && !attributeSplit(a1, a2, refactorings) && !attributeWithConflictingRename(a1, a2, originalRefactorings)) {
							if(a1 instanceof UMLEnumConstant && a2 instanceof UMLEnumConstant) {
								UMLEnumConstantDiff enumConstantDiff = new UMLEnumConstantDiff((UMLEnumConstant)a1, (UMLEnumConstant)a2, this, modelDiff);
								if(!enumConstantDiffList.contains(enumConstantDiff)) {
									enumConstantDiffList.add(enumConstantDiff);
								}
								Set<Refactoring> enumConstantDiffRefactorings = enumConstantDiff.getRefactorings(set);
								if(!refactorings.containsAll(enumConstantDiffRefactorings)) {
									refactorings.addAll(enumConstantDiffRefactorings);
									break;//it's not necessary to repeat the same process for all candidates in the set
								}
							}
							else {
								//avoid infinite loop if the attributes are referenced within their own initializers
								boolean a1ReferencedInItsOwnInitializer = false;
								if(a1.getVariableDeclaration().getInitializer() != null && a1.getVariableDeclaration().getInitializer().getAnonymousClassDeclarations().size() > 0) {
									int subsumeCount = 0;
									for(AbstractCodeMapping reference : candidate.getReferences()) {
										if(a1.getVariableDeclaration().getInitializer().getLocationInfo().subsumes(reference.getFragment1().getLocationInfo())) {
											subsumeCount++;
										}
									}
									if(subsumeCount == candidate.getReferences().size()) {
										a1ReferencedInItsOwnInitializer = true;
									}
								}
								boolean a2ReferencedInItsOwnInitializer = false;
								if(a2.getVariableDeclaration().getInitializer() != null && a2.getVariableDeclaration().getInitializer().getAnonymousClassDeclarations().size() > 0) {
									int subsumeCount = 0;
									for(AbstractCodeMapping reference : candidate.getReferences()) {
										if(a2.getVariableDeclaration().getInitializer().getLocationInfo().subsumes(reference.getFragment2().getLocationInfo())) {
											subsumeCount++;
										}
									}
									if(subsumeCount == candidate.getReferences().size()) {
										a2ReferencedInItsOwnInitializer = true;
									}
								}
								if(!a1ReferencedInItsOwnInitializer && !a2ReferencedInItsOwnInitializer) {
									UMLAttributeDiff attributeDiff = new UMLAttributeDiff(a1, a2, this, modelDiff);
									if(!attributeDiffList.contains(attributeDiff)) {
										attributeDiffList.add(attributeDiff);
									}
									Set<Refactoring> attributeDiffRefactorings = attributeDiff.getRefactorings(set);
									if(!refactorings.containsAll(attributeDiffRefactorings)) {
										refactorings.addAll(attributeDiffRefactorings);
										break;//it's not necessary to repeat the same process for all candidates in the set
									}
								}
							}
						}
					}
					else {
						candidate.setOriginalAttribute(a1);
						candidate.setRenamedAttribute(a2);
						if(a1 != null)
							candidate.setOriginalVariableDeclaration(a1.getVariableDeclaration());
						if(a2 != null)
							candidate.setRenamedVariableDeclaration(a2.getVariableDeclaration());
						candidateAttributeRenames.add(candidate);
					}
				}
				else if(candidate.getOriginalVariableDeclaration() != null) {
					if(a2 != null) {
						RenameVariableRefactoring ref = new RenameVariableRefactoring(
								candidate.getOriginalVariableDeclaration(), a2.getVariableDeclaration(),
								candidate.getOperationBefore(), candidate.getOperationAfter(), candidate.getReferences(), false);
						if(!refactorings.contains(ref)) {
							refactorings.add(ref);
							if(!candidate.getOriginalVariableDeclaration().equalType(a2.getVariableDeclaration()) ||
									!candidate.getOriginalVariableDeclaration().equalQualifiedType(a2.getVariableDeclaration())) {
								ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(candidate.getOriginalVariableDeclaration(), a2.getVariableDeclaration(),
										candidate.getOperationBefore(), candidate.getOperationAfter(), candidate.getReferences(), false);
								refactoring.addRelatedRefactoring(ref);
								refactorings.add(refactoring);
							}
						}
					}
					else {
						//field is declared in a superclass or outer class
						candidateAttributeRenames.add(candidate);
					}
				}
				else if(candidate.getRenamedVariableDeclaration() != null) {
					if(a1 != null) {
						RenameVariableRefactoring ref = new RenameVariableRefactoring(
								a1.getVariableDeclaration(), candidate.getRenamedVariableDeclaration(),
								candidate.getOperationBefore(), candidate.getOperationAfter(), candidate.getReferences(), false);
						if(!refactorings.contains(ref)) {
							refactorings.add(ref);
							List<Refactoring> refactoringsToBeRemoved = new ArrayList<>();
							for(Refactoring r : refactorings) {
								if(r instanceof InlineAttributeRefactoring) {
									InlineAttributeRefactoring inline = (InlineAttributeRefactoring)r;
									if(inline.getVariableDeclaration().equals(a1)) {
										for(LeafMapping leafMapping : inline.getSubExpressionMappings()) {
											if(candidate.getRenamedVariableDeclaration().getVariableName().equals(leafMapping.getFragment2().getString())) {
												refactoringsToBeRemoved.add(r);
												break;
											}
										}
									}
								}
							}
							refactorings.removeAll(refactoringsToBeRemoved);
						}
					}
					else {
						//field is declared in a superclass or outer class
						candidateAttributeRenames.add(candidate);
					}
				}
			}
		}
		return refactorings;
	}

	public List<Refactoring> getRefactoringsBeforePostProcessing() {
		return refactorings;
	}

	protected void processMapperRefactorings(UMLOperationBodyMapper mapper, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
		Set<Refactoring> refactorings2 = new LinkedHashSet<>();
		refactorings2.addAll(mapper.getRefactoringsAfterPostProcessing());
		refactorings2.addAll(mapper.getRefactorings());
		for(Refactoring newRefactoring : refactorings2) {
			if(refactorings.contains(newRefactoring)) {
				boolean inlineExtractAttributeFound = false;
				for(Refactoring refactoring : refactorings) {
					if(refactoring.equals(newRefactoring) && refactoring instanceof ExtractAttributeRefactoring) {
						ExtractAttributeRefactoring newExtractVariableRefactoring = (ExtractAttributeRefactoring)newRefactoring;
						Set<AbstractCodeMapping> newReferences = newExtractVariableRefactoring.getReferences();
						ExtractAttributeRefactoring oldExtractVariableRefactoring = (ExtractAttributeRefactoring)refactoring;
						oldExtractVariableRefactoring.addReferences(newReferences);
						for(LeafMapping newLeafMapping : newExtractVariableRefactoring.getSubExpressionMappings()) {
							oldExtractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
						}
						inlineExtractAttributeFound = true;
						break;
					}
					if(refactoring.equals(newRefactoring) && refactoring instanceof ExtractVariableRefactoring) {
						ExtractVariableRefactoring newExtractVariableRefactoring = (ExtractVariableRefactoring)newRefactoring;
						Set<AbstractCodeMapping> newReferences = newExtractVariableRefactoring.getReferences();
						Set<AbstractCodeFragment> newUnmatchedStatementReferences = newExtractVariableRefactoring.getUnmatchedStatementReferences();
						ExtractVariableRefactoring oldExtractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
						oldExtractVariableRefactoring.addReferences(newReferences);
						oldExtractVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
						for(LeafMapping newLeafMapping : newExtractVariableRefactoring.getSubExpressionMappings()) {
							oldExtractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
						}
						inlineExtractAttributeFound = true;
						break;
					}
					if(refactoring.equals(newRefactoring) && refactoring instanceof InlineAttributeRefactoring) {
						InlineAttributeRefactoring newInlineVariableRefactoring = (InlineAttributeRefactoring)newRefactoring;
						Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
						InlineAttributeRefactoring oldInlineVariableRefactoring = (InlineAttributeRefactoring)refactoring;
						oldInlineVariableRefactoring.addReferences(newReferences);
						for(LeafMapping newLeafMapping : newInlineVariableRefactoring.getSubExpressionMappings()) {
							oldInlineVariableRefactoring.addSubExpressionMapping(newLeafMapping);
						}
						inlineExtractAttributeFound = true;
						break;
					}
					if(refactoring.equals(newRefactoring) && refactoring instanceof InlineVariableRefactoring) {
						InlineVariableRefactoring newInlineVariableRefactoring = (InlineVariableRefactoring)newRefactoring;
						Set<AbstractCodeMapping> newReferences = newInlineVariableRefactoring.getReferences();
						Set<AbstractCodeFragment> newUnmatchedStatementReferences = newInlineVariableRefactoring.getUnmatchedStatementReferences();
						InlineVariableRefactoring oldInlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
						oldInlineVariableRefactoring.addReferences(newReferences);
						oldInlineVariableRefactoring.addUnmatchedStatementReferences(newUnmatchedStatementReferences);
						for(LeafMapping newLeafMapping : newInlineVariableRefactoring.getSubExpressionMappings()) {
							oldInlineVariableRefactoring.addSubExpressionMapping(newLeafMapping);
						}
						inlineExtractAttributeFound = true;
						break;
					}
				}
				//special handling for replacing rename variable refactorings having statement mapping information
				if(!inlineExtractAttributeFound) {
					int index = refactorings.indexOf(newRefactoring);
					refactorings.remove(index);
					refactorings.add(index, newRefactoring);
				}
			}
			else {
				refactorings.add(newRefactoring);
			}
		}
		for(CandidateAttributeRefactoring candidate : mapper.getCandidateAttributeRenames()) {
			if(!multipleExtractedMethodInvocationsWithDifferentAttributesAsArguments(candidate, refactorings)) {
				String before = PrefixSuffixUtils.normalize(candidate.getOriginalVariableName());
				String after = PrefixSuffixUtils.normalize(candidate.getRenamedVariableName());
				if(before.contains(".") && after.contains(".")) {
					String prefix1 = before.substring(0, before.lastIndexOf(".") + 1);
					String prefix2 = after.substring(0, after.lastIndexOf(".") + 1);
					if(prefix1.equals(prefix2)) {
						before = before.substring(prefix1.length(), before.length());
						after = after.substring(prefix2.length(), after.length());
					}
				}
				Replacement renamePattern = new Replacement(before, after, ReplacementType.VARIABLE_NAME);
				if(renameMap.containsKey(renamePattern)) {
					renameMap.get(renamePattern).add(candidate);
				}
				else {
					Set<CandidateAttributeRefactoring> set = new LinkedHashSet<CandidateAttributeRefactoring>();
					set.add(candidate);
					renameMap.put(renamePattern, set);
				}
			}
		}
		for(CandidateMergeVariableRefactoring candidate : mapper.getCandidateAttributeMerges()) {
			int movedAttributes = movedAttributeCount(candidate);
			if(movedAttributes != candidate.getMergedVariables().size()) {
				Set<String> before = new LinkedHashSet<String>();
				for(String mergedVariable : candidate.getMergedVariables()) {
					before.add(PrefixSuffixUtils.normalize(mergedVariable));
				}
				String after = PrefixSuffixUtils.normalize(candidate.getNewVariable());
				MergeVariableReplacement merge = new MergeVariableReplacement(before, after);
				processMerge(mergeMap, merge, candidate);
			}
		}
		for(CandidateSplitVariableRefactoring candidate : mapper.getCandidateAttributeSplits()) {
			Set<String> after = new LinkedHashSet<String>();
			for(String splitVariable : candidate.getSplitVariables()) {
				after.add(PrefixSuffixUtils.normalize(splitVariable));
			}
			String before = PrefixSuffixUtils.normalize(candidate.getOldVariable());
			SplitVariableReplacement split = new SplitVariableReplacement(before, after);
			processSplit(splitMap, split, candidate);
		}
	}

	public int movedAttributeCount(CandidateMergeVariableRefactoring candidate) {
		UMLAttribute addedAttribute = null;
		for(UMLAttribute attribute : addedAttributes) {
			if(attribute.getName().equals(PrefixSuffixUtils.normalize(candidate.getNewVariable()))) {
				addedAttribute = attribute;
				break;
			}
		}
		int movedAttributes = 0;
		if(addedAttribute != null) {
			UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff(addedAttribute.getType());
			if(classDiff != null) {
				for(String mergedVariable : candidate.getMergedVariables()) {
					UMLAttribute removedAttribute = null;
					for(UMLAttribute attribute : removedAttributes) {
						if(attribute.getName().equals(PrefixSuffixUtils.normalize(mergedVariable))) {
							removedAttribute = attribute;
							break;
						}
					}
					if(removedAttribute != null) {
						for(UMLAttribute attribute : classDiff.getAddedAttributes()) {
							if(attribute.getName().equals(removedAttribute.getName()) && attribute.getType().equals(removedAttribute.getType())) {
								movedAttributes++;
								break;
							}
						}
					}
				}
			}
		}
		return movedAttributes;
	}

	private boolean multipleExtractedMethodInvocationsWithDifferentAttributesAsArguments(CandidateAttributeRefactoring candidate, List<Refactoring> refactorings) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractRefactoring = (ExtractOperationRefactoring)refactoring;
				if(extractRefactoring.getExtractedOperation().equals(candidate.getOperationAfter())) {
					List<AbstractCall> extractedInvocations = extractRefactoring.getExtractedOperationInvocations();
					if(extractedInvocations.size() > 1) {
						Set<VariableDeclaration> attributesMatchedWithArguments = new LinkedHashSet<VariableDeclaration>();
						Set<String> attributeNamesMatchedWithArguments = new LinkedHashSet<String>();
						for(AbstractCall extractedInvocation : extractedInvocations) {
							for(String argument : extractedInvocation.arguments()) {
								for(UMLAttribute attribute : originalClass.getAttributes()) {
									if(attribute.getName().equals(argument)) {
										attributesMatchedWithArguments.add(attribute.getVariableDeclaration());
										attributeNamesMatchedWithArguments.add(attribute.getName());
										break;
									}
								}
							}
						}
						if((attributeNamesMatchedWithArguments.contains(candidate.getOriginalVariableName()) ||
								attributeNamesMatchedWithArguments.contains(candidate.getRenamedVariableName())) &&
								attributesMatchedWithArguments.size() > 1) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private Set<Refactoring> inferAttributeMergesAndSplits(Map<Replacement, Set<CandidateAttributeRefactoring>> map, List<Refactoring> refactorings) {
		Set<Refactoring> newRefactorings = new LinkedHashSet<Refactoring>();
		for(Replacement replacement : map.keySet()) {
			Set<CandidateAttributeRefactoring> candidates = map.get(replacement);
			for(CandidateAttributeRefactoring candidate : candidates) {
				String originalAttributeName = PrefixSuffixUtils.normalize(candidate.getOriginalVariableName());
				String renamedAttributeName = PrefixSuffixUtils.normalize(candidate.getRenamedVariableName());
				UMLOperationBodyMapper candidateMapper = null;
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(mapper.getMappings().containsAll(candidate.getReferences())) {
						candidateMapper = mapper;
						break;
					}
					for(UMLOperationBodyMapper nestedMapper : mapper.getChildMappers()) {
						if(nestedMapper.getMappings().containsAll(candidate.getReferences())) {
							candidateMapper = nestedMapper;
							break;
						}
					}
				}
				if(candidateMapper != null) {
					for(Refactoring refactoring : refactorings) {
						if(refactoring instanceof MergeVariableRefactoring) {
							MergeVariableRefactoring merge = (MergeVariableRefactoring)refactoring;
							Set<String> nonMatchingVariableNames = new LinkedHashSet<String>();
							String matchingVariableName = null;
							for(VariableDeclaration variableDeclaration : merge.getMergedVariables()) {
								if(originalAttributeName.equals(variableDeclaration.getVariableName())) {
									matchingVariableName = variableDeclaration.getVariableName();
								}
								else {
									for(AbstractCodeFragment statement : candidateMapper.getNonMappedLeavesT1()) {
										if(statement.getString().startsWith(variableDeclaration.getVariableName() + JAVA.ASSIGNMENT) ||
												statement.getString().startsWith(JAVA.THIS_DOT + variableDeclaration.getVariableName() + JAVA.ASSIGNMENT)) {
											nonMatchingVariableNames.add(variableDeclaration.getVariableName());
											break;
										}
									}
								}
							}
							if(matchingVariableName != null && renamedAttributeName.equals(merge.getNewVariable().getVariableName()) && nonMatchingVariableNames.size() > 0) {
								Set<UMLAttribute> mergedAttributes = new LinkedHashSet<UMLAttribute>();
								Set<VariableDeclaration> mergedVariables = new LinkedHashSet<VariableDeclaration>();
								Set<String> allMatchingVariables = new LinkedHashSet<String>();
								if(merge.getMergedVariables().iterator().next().getVariableName().equals(matchingVariableName)) {
									allMatchingVariables.add(matchingVariableName);
									allMatchingVariables.addAll(nonMatchingVariableNames);
								}
								else {
									allMatchingVariables.addAll(nonMatchingVariableNames);
									allMatchingVariables.add(matchingVariableName);
								}
								for(String mergedVariable : allMatchingVariables) {
									UMLAttribute a1 = findAttributeInOriginalClass(mergedVariable);
									if(a1 != null) {
										mergedAttributes.add(a1);
										mergedVariables.add(a1.getVariableDeclaration());
									}
								}
								UMLAttribute a2 = findAttributeInNextClass(renamedAttributeName);
								if(mergedVariables.size() > 1 && mergedVariables.size() == merge.getMergedVariables().size() && a2 != null) {
									MergeAttributeRefactoring ref = new MergeAttributeRefactoring(mergedAttributes, a2, getOriginalClassName(), getNextClassName(), new LinkedHashSet<CandidateMergeVariableRefactoring>());
									if(!refactorings.contains(ref)) {
										newRefactorings.add(ref);
									}
								}
							}
						}
						else if(refactoring instanceof SplitVariableRefactoring) {
							SplitVariableRefactoring split = (SplitVariableRefactoring)refactoring;
							Set<String> nonMatchingVariableNames = new LinkedHashSet<String>();
							String matchingVariableName = null;
							for(VariableDeclaration variableDeclaration : split.getSplitVariables()) {
								if(renamedAttributeName.equals(variableDeclaration.getVariableName())) {
									matchingVariableName = variableDeclaration.getVariableName();
								}
								else {
									for(AbstractCodeFragment statement : candidateMapper.getNonMappedLeavesT2()) {
										if(statement.getString().startsWith(variableDeclaration.getVariableName() + JAVA.ASSIGNMENT) ||
												statement.getString().startsWith(JAVA.THIS_DOT + variableDeclaration.getVariableName() + JAVA.ASSIGNMENT)) {
											nonMatchingVariableNames.add(variableDeclaration.getVariableName());
											break;
										}
									}
								}
							}
							if(matchingVariableName != null && originalAttributeName.equals(split.getOldVariable().getVariableName()) && nonMatchingVariableNames.size() > 0) {
								Set<UMLAttribute> splitAttributes = new LinkedHashSet<UMLAttribute>();
								Set<VariableDeclaration> splitVariables = new LinkedHashSet<VariableDeclaration>();
								Set<String> allMatchingVariables = new LinkedHashSet<String>();
								if(split.getSplitVariables().iterator().next().getVariableName().equals(matchingVariableName)) {
									allMatchingVariables.add(matchingVariableName);
									allMatchingVariables.addAll(nonMatchingVariableNames);
								}
								else {
									allMatchingVariables.addAll(nonMatchingVariableNames);
									allMatchingVariables.add(matchingVariableName);
								}
								for(String splitVariable : allMatchingVariables) {
									UMLAttribute a2 = findAttributeInNextClass(splitVariable);
									if(a2 != null) {
										splitAttributes.add(a2);
										splitVariables.add(a2.getVariableDeclaration());
									}
								}
								UMLAttribute a1 = findAttributeInOriginalClass(originalAttributeName);
								if(splitVariables.size() > 1 && splitVariables.size() == split.getSplitVariables().size() && a1 != null && findAttributeInNextClass(originalAttributeName) == null) {
									SplitAttributeRefactoring ref = new SplitAttributeRefactoring(a1, splitAttributes, getOriginalClassName(), getNextClassName(), new LinkedHashSet<CandidateSplitVariableRefactoring>());
									if(!refactorings.contains(ref)) {
										newRefactorings.add(ref);
									}
								}
							}
						}
					}
				}
			}
		}
		return newRefactorings;
	}

	private boolean attributeWithConflictingRename(UMLAttribute a1, UMLAttribute a2, List<Refactoring> refactorings) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof RenameAttributeRefactoring) {
				RenameAttributeRefactoring rename = (RenameAttributeRefactoring)refactoring;
				if(rename.getOriginalAttribute().equals(a1) && !rename.getRenamedAttribute().equals(a2)) {
					return true;
				}
				else if(!rename.getOriginalAttribute().equals(a1) && rename.getRenamedAttribute().equals(a2)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean attributeMerged(UMLAttribute a1, UMLAttribute a2, List<Refactoring> refactorings) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof MergeAttributeRefactoring) {
				MergeAttributeRefactoring merge = (MergeAttributeRefactoring)refactoring;
				if(merge.getMergedVariables().contains(a1.getVariableDeclaration()) && merge.getNewAttribute().getVariableDeclaration().equals(a2.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean attributeSplit(UMLAttribute a1, UMLAttribute a2, List<Refactoring> refactorings) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof SplitAttributeRefactoring) {
				SplitAttributeRefactoring split = (SplitAttributeRefactoring)refactoring;
				if(split.getSplitVariables().contains(a2.getVariableDeclaration()) && split.getOldAttribute().getVariableDeclaration().equals(a1.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private void processMerge(Map<MergeVariableReplacement, Set<CandidateMergeVariableRefactoring>> mergeMap, MergeVariableReplacement newMerge, CandidateMergeVariableRefactoring candidate) {
		MergeVariableReplacement mergeToBeRemoved = null;
		for(MergeVariableReplacement merge : mergeMap.keySet()) {
			if(merge.subsumes(newMerge)) {
				mergeMap.get(merge).add(candidate);
				return;
			}
			else if(merge.equal(newMerge)) {
				mergeMap.get(merge).add(candidate);
				return;
			}
			else if(merge.commonAfter(newMerge)) {
				mergeToBeRemoved = merge;
				Set<String> mergedVariables = new LinkedHashSet<String>();
				mergedVariables.addAll(merge.getMergedVariables());
				mergedVariables.addAll(newMerge.getMergedVariables());
				MergeVariableReplacement replacement = new MergeVariableReplacement(mergedVariables, merge.getAfter());
				Set<CandidateMergeVariableRefactoring> candidates = mergeMap.get(mergeToBeRemoved);
				candidates.add(candidate);
				mergeMap.put(replacement, candidates);
				break;
			}
			else if(newMerge.subsumes(merge)) {
				mergeToBeRemoved = merge;
				Set<CandidateMergeVariableRefactoring> candidates = mergeMap.get(mergeToBeRemoved);
				candidates.add(candidate);
				mergeMap.put(newMerge, candidates);
				break;
			}
		}
		if(mergeToBeRemoved != null) {
			mergeMap.remove(mergeToBeRemoved);
			return;
		}
		Set<CandidateMergeVariableRefactoring> set = new LinkedHashSet<CandidateMergeVariableRefactoring>();
		set.add(candidate);
		mergeMap.put(newMerge, set);
	}

	private void processSplit(Map<SplitVariableReplacement, Set<CandidateSplitVariableRefactoring>> splitMap, SplitVariableReplacement newSplit, CandidateSplitVariableRefactoring candidate) {
		SplitVariableReplacement splitToBeRemoved = null;
		for(SplitVariableReplacement split : splitMap.keySet()) {
			if(split.subsumes(newSplit)) {
				splitMap.get(split).add(candidate);
				return;
			}
			else if(split.equal(newSplit)) {
				splitMap.get(split).add(candidate);
				return;
			}
			else if(split.commonBefore(newSplit)) {
				splitToBeRemoved = split;
				Set<String> splitVariables = new LinkedHashSet<String>();
				splitVariables.addAll(split.getSplitVariables());
				splitVariables.addAll(newSplit.getSplitVariables());
				SplitVariableReplacement replacement = new SplitVariableReplacement(split.getBefore(), splitVariables);
				Set<CandidateSplitVariableRefactoring> candidates = splitMap.get(splitToBeRemoved);
				candidates.add(candidate);
				splitMap.put(replacement, candidates);
				break;
			}
			else if(newSplit.subsumes(split)) {
				splitToBeRemoved = split;
				Set<CandidateSplitVariableRefactoring> candidates = splitMap.get(splitToBeRemoved);
				candidates.add(candidate);
				splitMap.put(newSplit, candidates);
				break;
			}
		}
		if(splitToBeRemoved != null) {
			splitMap.remove(splitToBeRemoved);
			return;
		}
		Set<CandidateSplitVariableRefactoring> set = new LinkedHashSet<CandidateSplitVariableRefactoring>();
		set.add(candidate);
		splitMap.put(newSplit, set);
	}

	public UMLAttribute findAttributeInOriginalClass(String attributeName) {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			if(attribute.getName().equals(attributeName)) {
				return attribute;
			}
		}
		for(UMLEnumConstant enumConstant : originalClass.getEnumConstants()) {
			if(enumConstant.getName().equals(attributeName) && removedEnumConstants.contains(enumConstant)) {
				return enumConstant;
			}
		}
		if(modelDiff != null && !originalClass.isTopLevel()) {
			//search attribute declaration in parent class
			for(UMLClassDiff diff : modelDiff.getCommonClassDiffList()) {
				if(originalClass.getName().startsWith(diff.getOriginalClassName()) && !originalClass.getName().equals(diff.getOriginalClassName())) {
					UMLAttribute attribute = diff.findAttributeInOriginalClass(attributeName);
					if(attribute != null) {
						return attribute;
					}
				}
			}
		}
		return null;
	}

	public UMLAttribute findAttributeInNextClass(String attributeName) {
		for(UMLAttribute attribute : nextClass.getAttributes()) {
			if(attribute.getName().equals(attributeName)) {
				return attribute;
			}
		}
		for(UMLEnumConstant enumConstant : nextClass.getEnumConstants()) {
			if(enumConstant.getName().equals(attributeName) && addedEnumConstants.contains(enumConstant)) {
				return enumConstant;
			}
		}
		if(modelDiff != null && !nextClass.isTopLevel()) {
			//search attribute declaration in parent class
			for(UMLClassDiff diff : modelDiff.getCommonClassDiffList()) {
				if(nextClass.getName().startsWith(diff.getNextClassName()) && !nextClass.getName().equals(diff.getNextClassName())) {
					UMLAttribute attribute = diff.findAttributeInNextClass(attributeName);
					if(attribute != null) {
						return attribute;
					}
				}
			}
		}
		return null;
	}

	private boolean inconsistentAttributeRename(Replacement pattern, Map<String, Set<String>> aliasedAttributesInOriginalClass, Map<String, Set<String>> aliasedAttributesInNextClass) {
		for(String key : aliasedAttributesInOriginalClass.keySet()) {
			if(aliasedAttributesInOriginalClass.get(key).contains(pattern.getBefore())) {
				return false;
			}
		}
		for(String key : aliasedAttributesInNextClass.keySet()) {
			if(aliasedAttributesInNextClass.get(key).contains(pattern.getAfter())) {
				return false;
			}
		}
		int counter = 0;
		int allCases = 0;
		for(UMLOperationBodyMapper mapper : this.operationBodyMapperList) {
			List<String> allVariables1 = mapper.getContainer1().getAllVariables();
			List<String> allVariables2 = mapper.getContainer2().getAllVariables();
			for(UMLOperationBodyMapper nestedMapper : mapper.getChildMappers()) {
				allVariables1.addAll(nestedMapper.getContainer1().getAllVariables());
				allVariables2.addAll(nestedMapper.getContainer2().getAllVariables());
			}
			boolean variables1contains = (allVariables1.contains(pattern.getBefore()) &&
					!mapper.getContainer1().getParameterNameList().contains(pattern.getBefore())) ||
					allVariables1.contains(JAVA.THIS_DOT+pattern.getBefore());
			boolean variables2Contains = (allVariables2.contains(pattern.getAfter()) &&
					!mapper.getContainer2().getParameterNameList().contains(pattern.getAfter())) ||
					allVariables2.contains(JAVA.THIS_DOT+pattern.getAfter());
			if(variables1contains && !variables2Contains) {
				boolean skip = false;
				for(AbstractCodeMapping mapping : mapper.getMappings()) {
					for(AbstractCall call : mapping.getFragment2().getMethodInvocations()) {
						for(UMLOperation addedOperation : addedOperations) {
							if(call.matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
								List<String> addedOperationVariables = addedOperation.getAllVariables();
								if(addedOperationVariables.contains(pattern.getAfter())) {
									skip = true;
									break;
								}
							}
						}
						if(skip)
							break;
					}
					if(skip)
						break;
				}
				if(!skip) {
					counter++;
				}
			}
			if(variables2Contains && !variables1contains) {
				boolean skip = false;
				for(AbstractCodeMapping mapping : mapper.getMappings()) {
					for(AbstractCall call : mapping.getFragment1().getMethodInvocations()) {
						for(UMLOperation removedOperation : removedOperations) {
							if(call.matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff)) {
								List<String> removedOperationVariables = removedOperation.getAllVariables();
								if(removedOperationVariables.contains(pattern.getBefore())) {
									skip = true;
									break;
								}
							}
						}
						if(skip)
							break;
					}
					if(skip)
						break;
				}
				if(!skip) {
					counter++;
				}
			}
			if(variables1contains || variables2Contains) {
				allCases++;
			}
		}
		double percentage = (double)counter/(double)allCases;
		if(percentage > 0.5)
			return true;
		return false;
	}

	private static boolean cyclicRename(Map<Replacement, Set<CandidateAttributeRefactoring>> renames, Replacement rename) {
		for(Replacement r : renames.keySet()) {
			if((rename.getAfter().equals(r.getBefore()) || rename.getBefore().equals(r.getAfter())) &&
					(totalOccurrences(renames.get(rename)) > 1 || totalOccurrences(renames.get(r)) > 1))
			return true;
		}
		return false;
	}

	private static int totalOccurrences(Set<CandidateAttributeRefactoring> candidates) {
		int totalCount = 0;
		for(CandidateAttributeRefactoring candidate : candidates) {
			totalCount += candidate.getOccurrences();
		}
		return totalCount;
	}

	public boolean containsExtractOperationRefactoring(VariableDeclarationContainer sourceOperationBeforeExtraction, UMLOperation extractedOperation) {
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractRef = (ExtractOperationRefactoring)ref;
				if(extractRef.getSourceOperationBeforeExtraction().equals(sourceOperationBeforeExtraction) &&
						extractRef.getExtractedOperation().equalSignature(extractedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsInlineOperationRefactoring(UMLOperation inlinedOperation, VariableDeclarationContainer targetOperationAfterInline) {
		for(Refactoring ref : refactorings) {
			if(ref instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring inlineRef = (InlineOperationRefactoring)ref;
				if(inlineRef.getTargetOperationAfterInline().equals(targetOperationAfterInline) &&
						inlineRef.getInlinedOperation().equalSignature(inlinedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<String> getVariableDeclarationNamesInMethodBody(VariableDeclarationContainer operation) {
		if(operation.getBody() != null) {
			Set<String> keySet = new LinkedHashSet<>(operation.variableDeclarationMap().keySet());
			keySet.removeAll(operation.getParameterNameList());
			return keySet;
		}
		return Collections.emptySet();
	}

	public boolean isPartOfMethodInlined(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation) {
		List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
		List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		Set<AbstractCall> intersection = new LinkedHashSet<AbstractCall>(removedOperationInvocations);
		intersection.retainAll(addedOperationInvocations);
		int numberOfInvocationsMissingFromAddedOperation = new LinkedHashSet<AbstractCall>(addedOperationInvocations).size() - intersection.size();
		
		Set<AbstractCall> operationInvocationsInMethodsCalledByRemovedOperation = new LinkedHashSet<AbstractCall>();
		for(AbstractCall removedOperationInvocation : removedOperationInvocations) {
			if(!intersection.contains(removedOperationInvocation)) {
				for(UMLOperation operation : removedOperations) {
					if(!operation.equals(removedOperation) && operation.getBody() != null) {
						if(removedOperationInvocation.matchesOperation(operation, removedOperation, this, modelDiff)) {
							//removedOperation calls another removed method
							operationInvocationsInMethodsCalledByRemovedOperation.addAll(operation.getAllOperationInvocations());
						}
					}
				}
			}
		}
		Set<AbstractCall> newIntersection = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
		newIntersection.retainAll(operationInvocationsInMethodsCalledByRemovedOperation);
		
		Set<AbstractCall> addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
		addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
		addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
		for(Iterator<AbstractCall> operationInvocationIterator = addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.iterator(); operationInvocationIterator.hasNext();) {
			AbstractCall invocation = operationInvocationIterator.next();
			if(invocation.getName().startsWith("get") || invocation.getName().equals("add") || invocation.getName().equals("contains")) {
				operationInvocationIterator.remove();
			}
		}
		
		int numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations = numberOfInvocationsMissingFromAddedOperation - numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations;
		return numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations ||
				numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > addedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
	}

	public boolean matchesCandidateAttributeRename(UMLOperation addedOperation) {
		String setPrefix = "set";
		String getPrefix = "get";
		for(Replacement r : renameMap.keySet()) {
			if(addedOperation.isGetter()) {
				if(addedOperation.getName().equals(r.getAfter()) || addedOperation.getName().toLowerCase().equals(getPrefix + r.getAfter().toLowerCase())) {
					return true;
				}
			}
			else if(addedOperation.isSetter()) {
				if(addedOperation.getName().toLowerCase().equals(setPrefix + r.getAfter().toLowerCase()) || addedOperation.getParameterNameList().get(0).equals(r.getAfter())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean matchesPairOfRemovedAddedOperations(AbstractCall call1, AbstractCall call2, VariableDeclarationContainer container1, VariableDeclarationContainer container2) {
		boolean foundInRemoved = false;
		if(call1 instanceof OperationInvocation) {
			OperationInvocation inv1 = (OperationInvocation)call1;
			for(UMLOperation removedOperation : removedOperations) {
				if(inv1.matchesOperation(removedOperation, container1, this, modelDiff)) {
					foundInRemoved = true;
					break;
				}
			}
		}
		boolean foundInAdded = false;
		if(call2 instanceof OperationInvocation) {
			OperationInvocation inv2 = (OperationInvocation)call2;
			for(UMLOperation addedOperation : addedOperations) {
				if(inv2.matchesOperation(addedOperation, container2, this, modelDiff)) {
					foundInAdded = true;
					break;
				}
			}
		}
		return foundInRemoved && foundInAdded;
	}
}
