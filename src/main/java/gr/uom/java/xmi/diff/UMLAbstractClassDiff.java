package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.decomposition.Visitor.METHOD_SIGNATURE_PATTERN;
import static gr.uom.java.xmi.decomposition.Visitor.stringify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.ASTNode;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PathFileUtils;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.JavaFileProcessor;
import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLClassMatcher;
import gr.uom.java.xmi.UMLClassMatcher.MatchResult;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.MethodReference;
import gr.uom.java.xmi.decomposition.ObjectCreation;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.StringBasedHeuristics;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.SplitVariableReplacement;

public abstract class UMLAbstractClassDiff {
	protected List<UMLOperation> addedOperations;
	protected List<UMLOperation> removedOperations;
	protected List<UMLOperation> addedNestedOperations;
	protected List<UMLOperation> removedNestedOperations;
	protected List<UMLClass> removedNestedClasses;
	protected List<UMLClass> addedNestedClasses;
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
	private UMLTypeListDiff interfaceListDiff;
	private UMLTypeListDiff permittedTypeListDiff;
	private UMLCommentListDiff commentListDiff;
	private List<UMLClassDiff> nestedClassDiffList;
	private static final List<String> collectionAPINames = List.of("get", "add", "contains", "put", "putAll", "addAll", "equals");
	public static final double BUILDER_STATEMENT_RATIO_THRESHOLD = 0.7;
	private static final int MAXIMUM_NUMBER_OF_COMPARED_METHODS = 20;
	public static final double MAX_OPERATION_NAME_DISTANCE = 0.4;
	public final Constants LANG1;
	public final Constants LANG2;
	private Map<MethodInvocationReplacement, UMLOperationBodyMapper> consistentMethodInvocationRenamesInModel;
	protected Map<MethodInvocationReplacement, UMLOperationBodyMapper> consistentMethodInvocationRenames;
	protected Set<UMLOperationBodyMapper> potentialCodeMoveBetweenSetUpTearDownMethods = new LinkedHashSet<>();
	private Set<UMLOperationBodyMapper> movedMethodsInDifferentPositionWithinFile = new LinkedHashSet<>();
	protected Set<Pair<UMLOperationBodyMapper, UMLOperationBodyMapper>> calledBy = new LinkedHashSet<>();
	private Set<UMLOperationBodyMapper> extractMethodCandidates = new LinkedHashSet<>();
	private int removedOperationDelegates;
	
	public UMLAbstractClassDiff(UMLAbstractClass originalClass, UMLAbstractClass nextClass, UMLModelDiff modelDiff) {
		this.LANG1 = PathFileUtils.getLang(originalClass.getLocationInfo().getFilePath());
		this.LANG2 = PathFileUtils.getLang(nextClass.getLocationInfo().getFilePath());
		this.addedOperations = new ArrayList<UMLOperation>();
		this.removedOperations = new ArrayList<UMLOperation>();
		this.addedNestedOperations = new ArrayList<UMLOperation>();
		this.removedNestedOperations = new ArrayList<UMLOperation>();
		this.addedNestedClasses = new ArrayList<UMLClass>();
		this.removedNestedClasses = new ArrayList<UMLClass>();
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
		this.nestedClassDiffList = new ArrayList<UMLClassDiff>();
		this.refactorings = new ArrayList<Refactoring>();
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.modelDiff = modelDiff;
		this.interfaceListDiff = new UMLTypeListDiff(originalClass.getImplementedInterfaces(), nextClass.getImplementedInterfaces());
		this.permittedTypeListDiff = new UMLTypeListDiff(originalClass.getPermittedTypes(), nextClass.getPermittedTypes());
		this.consistentMethodInvocationRenamesInModel = findConsistentMethodInvocationRenamesInModelDiff();
		processAnnotations();
	}

	public List<UMLOperation> getAddedNestedOperations() {
		return addedNestedOperations;
	}

	public List<UMLOperation> getAddedNestedOperationsRecursively() {
		List<UMLOperation> all = new ArrayList<>(addedNestedOperations);
		for(UMLOperation op : addedNestedOperations) {
			all.addAll(op.getNestedOperations());
		}
		return all;
	}

	public List<UMLOperation> getRemovedNestedOperations() {
		return removedNestedOperations;
	}

	public List<UMLOperation> getRemovedNestedOperationsRecursively() {
		List<UMLOperation> all = new ArrayList<>(removedNestedOperations);
		for(UMLOperation op : removedNestedOperations) {
			all.addAll(op.getNestedOperations());
		}
		return all;
	}

	public List<UMLOperation> getAddedOperations() {
		return addedOperations;
	}

	public List<UMLClass> getRemovedNestedClasses() {
		return removedNestedClasses;
	}

	public List<UMLClass> getAddedNestedClasses() {
		return addedNestedClasses;
	}

	public List<UMLOperation> getAddedAndExtractedOperations() {
		List<UMLOperation> operations = new ArrayList<UMLOperation>(addedOperations);
		for(Refactoring r : refactorings) {
			if(r instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
				VariableDeclarationContainer extractedOperation = extract.getExtractedOperation();
				if(!operations.contains(extractedOperation) && extractedOperation instanceof UMLOperation op) {
					operations.add(op);
				}
			}
		}
		return operations;
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

	public List<UMLOperationBodyMapper> getOperationBodyMapperListIncludingNestedMappersInAnonymousClassDiffs() {
		ArrayList<UMLOperationBodyMapper> mappers = new ArrayList<>(operationBodyMapperList);
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			for(UMLAnonymousClassDiff anonymousDiff : mapper.getAnonymousClassDiffs()) {
				for(UMLOperationBodyMapper nestedMapper : anonymousDiff.getOperationBodyMapperList()) {
					if(nestedMapper.nonMappedElementsT1() > 0) {
						mappers.add(nestedMapper);
					}
				}
			}
		}
		return mappers;
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

	public List<UMLClassDiff> getNestedClassDiffList() {
		return nestedClassDiffList;
	}

	public void reportAddedAnonymousClass(UMLAnonymousClass umlClass) {
		this.addedAnonymousClasses.add(umlClass);
	}

	public void reportRemovedAnonymousClass(UMLAnonymousClass umlClass) {
		this.removedAnonymousClasses.add(umlClass);
	}

	public void addOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper) throws RefactoringMinerTimedOutException {
		this.operationBodyMapperList.add(operationBodyMapper);
		if(operationBodyMapper.getOperation1() != null && operationBodyMapper.getOperation2() != null) {
			if(operationBodyMapper.getOperation1().getNestedOperations().size() > 0 || operationBodyMapper.getOperation2().getNestedOperations().size() > 0) {
				processNestedOperations(operationBodyMapper.getOperation1(), operationBodyMapper.getOperation2());
			}
			if(operationBodyMapper.getOperation1().getNestedClasses().size() > 0 || operationBodyMapper.getOperation2().getNestedClasses().size() > 0) {
				processNestedClasses(operationBodyMapper.getOperation1(), operationBodyMapper.getOperation2());
			}
		}
	}

	private void processNestedClasses(UMLOperation operation1, UMLOperation operation2) throws RefactoringMinerTimedOutException {
		if(operation1.getNestedClasses().size() == operation2.getNestedClasses().size() && operation1.getNestedClasses().toString().equals(operation2.getNestedClasses().toString())) {
			for(int i=0; i<operation1.getNestedClasses().size(); i++) {
				UMLClass class1 = operation1.getNestedClasses().get(i);
				UMLClass class2 = operation2.getNestedClasses().get(i);
				UMLClassDiff classDiff = new UMLClassDiff(class1, class2, modelDiff);
				classDiff.process();
				this.nestedClassDiffList.add(classDiff);
			}
		}
		else if(operation1.getNestedClasses().size() <= operation2.getNestedClasses().size()) {
			this.removedNestedClasses.addAll(operation1.getNestedClasses());
			this.addedNestedClasses.addAll(operation2.getNestedClasses());
			for(UMLClass class1 : operation1.getNestedClasses()) {
				List<UMLClassDiff> nestedClassDiffs = new ArrayList<>();
				for(UMLClass class2 : operation2.getNestedClasses()) {
					if(class1.getName().equals(class2.getName()) || class1.getNonQualifiedName().equals(class2.getNonQualifiedName())) {
						UMLClassDiff classDiff = new UMLClassDiff(class1, class2, modelDiff);
						nestedClassDiffs.add(classDiff);
					}
				}
				if(nestedClassDiffs.size() == 1) {
					nestedClassDiffs.get(0).process();
					this.nestedClassDiffList.add(nestedClassDiffs.get(0));
					this.removedNestedClasses.remove(class1);
					this.addedNestedClasses.remove(nestedClassDiffs.get(0).getNextClass());
				}
				else {
					for(UMLClassDiff nested : nestedClassDiffs) {
						processNested(nested);
					}
				}
			}
		}
		else if(operation1.getNestedClasses().size() > operation2.getNestedClasses().size()) {
			this.removedNestedClasses.addAll(operation1.getNestedClasses());
			this.addedNestedClasses.addAll(operation2.getNestedClasses());
			for(UMLClass class2 : operation2.getNestedClasses()) {
				List<UMLClassDiff> nestedClassDiffs = new ArrayList<>();
				for(UMLClass class1 : operation1.getNestedClasses()) {
					if(class1.getName().equals(class2.getName()) || class1.getNonQualifiedName().equals(class2.getNonQualifiedName())) {
						UMLClassDiff classDiff = new UMLClassDiff(class1, class2, modelDiff);
						nestedClassDiffs.add(classDiff);
					}
				}
				if(nestedClassDiffs.size() == 1) {
					nestedClassDiffs.get(0).process();
					this.nestedClassDiffList.add(nestedClassDiffs.get(0));
					this.removedNestedClasses.remove(nestedClassDiffs.get(0).getOriginalClass());
					this.addedNestedClasses.remove(class2);
				}
				else {
					for(UMLClassDiff nested : nestedClassDiffs) {
						processNested(nested);
					}
				}
			}
		}
	}

	private void processNested(UMLClassDiff nested) throws RefactoringMinerTimedOutException {
		if(nested.getOriginalClass().getParentStatement().isPresent() && nested.getNextClass().getParentStatement().isPresent()) {
			AbstractStatement statement1 = nested.getOriginalClass().getParentStatement().get();
			AbstractStatement statement2 = nested.getNextClass().getParentStatement().get();
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				for(AbstractCodeMapping mapping : mapper.getMappings()) {
					if(mapping.getFragment1().equals(statement1) && mapping.getFragment2().equals(statement2)) {
						nested.process();
						this.nestedClassDiffList.add(nested);
						this.removedNestedClasses.remove(nested.getOriginalClass());
						this.addedNestedClasses.remove(nested.getNextClass());
						return;
					}
				}
			}
		}
	}

	private void processNestedOperations(UMLOperation operation1, UMLOperation operation2) throws RefactoringMinerTimedOutException {
		for(UMLOperation operation : operation1.getNestedOperations()) {
			UMLOperation operationWithTheSameSignature = operation2.nestedOperationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(operationWithTheSameSignature == null) {
				this.removedNestedOperations.add(operation);
			}
			else if(!mapperListContainsOperation(operation, operationWithTheSameSignature)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature, this);
				this.addOperationBodyMapper(mapper);
			}
		}
		for(UMLOperation operation : operation2.getNestedOperations()) {
			UMLOperation operationWithTheSameSignature = operation1.nestedOperationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(operationWithTheSameSignature == null) {
				this.addedNestedOperations.add(operation);
			}
			else if(!mapperListContainsOperation(operationWithTheSameSignature, operation)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operationWithTheSameSignature, operation, this);
				this.addOperationBodyMapper(mapper);
			}
		}
	}

	public UMLModelDiff getModelDiff() {
		return modelDiff;
	}

	public Set<String> commonPackagesInQualifiedName() {
		List<String> packages1 = Arrays.asList(originalClass.getName().split("\\."));
		List<String> packages2 = Arrays.asList(nextClass.getName().split("\\."));
		Set<String> intersection = new LinkedHashSet<>(packages1);
		intersection.retainAll(packages2);
		return intersection;
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
			commentListDiff = new UMLCommentListDiff(originalClass.getComments(), nextClass.getComments(), this);
		return commentListDiff;
	}

	public UMLTypeListDiff getInterfaceListDiff() {
		return interfaceListDiff;
	}

	public void findInterfaceChanges(String nameBefore, String nameAfter) {
		interfaceListDiff.findTypeChanges(nameBefore, nameAfter);
	}

	public void findInterfaceChanges(UMLType typeBefore, UMLType typeAfter) {
		interfaceListDiff.findTypeChanges(typeBefore, typeAfter);
	}

	public boolean hasBothAddedAndRemovedInterfaces() {
		if(interfaceListDiff != null) {
			return interfaceListDiff.getAddedTypes().size() > 0 && interfaceListDiff.getRemovedTypes().size() > 0;
		}
		return false;
	}

	public UMLTypeListDiff getPermittedTypeListDiff() {
		return permittedTypeListDiff;
	}

	public void findPermittedTypeChanges(String nameBefore, String nameAfter) {
		permittedTypeListDiff.findTypeChanges(nameBefore, nameAfter);
	}

	public void findPermittedTypeChanges(UMLType typeBefore, UMLType typeAfter) {
		permittedTypeListDiff.findTypeChanges(typeBefore, typeAfter);
	}

	public boolean hasBothAddedAndRemovedPermittedTypes() {
		if(permittedTypeListDiff != null) {
			return permittedTypeListDiff.getAddedTypes().size() > 0 && permittedTypeListDiff.getRemovedTypes().size() > 0;
		}
		return false;
	}

	protected boolean mapperListContainsMapper(UMLOperation operation1, UMLOperation operation2) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getContainer1().equals(operation1) && mapper.getContainer2().equals(operation2))
				return true;
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

	public List<UMLOperation> matchesAllOperation(AbstractCall invocation, List<UMLOperation> operations, VariableDeclarationContainer callerOperation) {
		List<UMLOperation> matches = new ArrayList<>();
		for(UMLOperation operation : operations) {
			if(invocation.matchesOperation(operation, callerOperation, this, modelDiff))
				matches.add(operation);
		}
		return matches;
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
			if((removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation() && addedOperation.getName().contains(removedOperation.getName())) ||
					(removedOperation.getJavadoc() != null && removedOperation.getJavadoc().refersToModifiedClass(modelDiff) && addedOperation.getJavadoc() != null && addedOperation.getJavadoc().refersToModifiedClass(modelDiff))) {
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
				else if(mapper.getContainer1().isConstructor() && mapper.getContainer2().isConstructor()) {
					for(AbstractCall invocation : invocationsCalledOnlyInOperation2) {
						for(UMLOperation insertedOperation : addedOperations) {
							if(!insertedOperation.equals(addedOperation)) {
								if(invocation.matchesOperation(insertedOperation, mapper.getContainer2(), this, modelDiff)) {
									List<String> insertedOperationRepresentation = insertedOperation.stringRepresentation();
									List<String> removedOperationRepresentation = removedOperation.stringRepresentation();
									Set<String> set2 = new LinkedHashSet<String>(insertedOperationRepresentation);
									set2.remove("{");
									set2.remove("}");
									Set<String> set1 = new LinkedHashSet<String>(removedOperationRepresentation);
									set1.remove("{");
									set1.remove("}");
									Set<String> intersection = new LinkedHashSet<String>(set1);
									intersection.retainAll(set2);
									if(intersection.size() > 1) {
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

	protected boolean isPartOfMethodMovedFromDeletedMethod(VariableDeclarationContainer removedOperation, VariableDeclarationContainer addedOperation, UMLOperationBodyMapper operationBodyMapper, Set<UMLOperationBodyMapper> mapperSet) {
		if(removedOperations.size() != addedOperations.size()) {
			if((addedOperation.getName().contains(removedOperation.getName()) || removedOperation.getName().contains(addedOperation.getName())) && !addedOperation.hasParameterizedTestAnnotation()) {
				//check if a mapper in the mapperSet calls removedOperation
				boolean callsRemovedOperation = false;
				for(UMLOperationBodyMapper mapper : mapperSet) {
					for(AbstractCodeFragment fragment : mapper.getNonMappedLeavesT1()) {
						for(AbstractCall call : fragment.getMethodInvocations()) {
							if(call.matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff)) {
								callsRemovedOperation = true;
								break;
							}
						}
						if(callsRemovedOperation) {
							break;
						}
					}
					if(!callsRemovedOperation) {
						for(CompositeStatementObject composite : mapper.getNonMappedInnerNodesT1()) {
							for(AbstractCall call : composite.getMethodInvocations()) {
								if(call.matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff)) {
									callsRemovedOperation = true;
									break;
								}
							}
							if(callsRemovedOperation) {
								break;
							}
						}
					}
				}
				List<AbstractCall> removedOperationInvocations = removedOperation.getAllOperationInvocations();
				List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
				Set<AbstractCall> movedInvocations = new LinkedHashSet<AbstractCall>(addedOperationInvocations);
				movedInvocations.removeAll(removedOperationInvocations);
				if(movedInvocations.size() > 0) {
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
							if(movedInvocations.containsAll(intersection) && intersection.size() > 0 && intersection.size() >= unmatchedCalls.size()) {
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
								if(unmatchedCalls.isEmpty() && !callsDeletedOperation && !callsRemovedOperation) {
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
			if(mapper.getMappings().isEmpty()) {
				continue;
			}
			List<AbstractCall> invocationsCalledInOperation1 = mapper.getContainer1().getAllOperationInvocations();
			List<AbstractCall> invocationsCalledInOperation2 = mapper.getContainer2().getAllOperationInvocations();
			Set<AbstractCall> invocationsCalledOnlyInOperation1 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation1);
			Set<AbstractCall> invocationsCalledOnlyInOperation2 = new LinkedHashSet<AbstractCall>(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation1.removeAll(invocationsCalledInOperation2);
			invocationsCalledOnlyInOperation2.removeAll(invocationsCalledInOperation1);
			for(AbstractCall invocation : invocationsCalledOnlyInOperation1) {
				if(invocation.matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff)) {
					boolean sameCallInFragment2 = false;
					for(AbstractCodeMapping mapping : mapper.getMappings()) {
						if(mapping.getFragment1().getLocationInfo().subsumes(invocation.getLocationInfo())) {
							for(AbstractCall call : mapping.getFragment2().getMethodInvocations()) {
								if(call.identicalName(invocation) && call.identicalExpression(invocation)) {
									sameCallInFragment2 = true;
									break;
								}
							}
							if(sameCallInFragment2) {
								break;
							}
						}
					}
					if(sameCallInFragment2) {
						continue;
					}
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
		removedOperationInvocations.addAll(removedOperation.getAllCreations());
		List<AbstractCall> addedOperationInvocations = addedOperation.getAllOperationInvocations();
		addedOperationInvocations.addAll(addedOperation.getAllCreations());
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
							operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllCreations());
							variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
							matchedOperationInvocations.add(addedOperationInvocation);
						}
					}
				}
			}
		}
		//this is to support the Extract & Move Method scenario
		Set<UMLOperation> matchingAddedOperations = new LinkedHashSet<>();
		if(modelDiff != null) {
			for(AbstractCall addedOperationInvocation : addedOperationInvocations) {
				String expression = addedOperationInvocation.getExpression();
				if(expression != null && !expression.equals(LANG2.THIS) &&
						!intersection.contains(addedOperationInvocation) && !matchedOperationInvocations.contains(addedOperationInvocation)) {
					UMLOperation operation = modelDiff.findOperationInAddedClasses(addedOperationInvocation, addedOperation, this);
					if(operation != null) {
						operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
						operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllCreations());
						variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
					}
				}
				if(addedOperation.getAnonymousClassContainer().isPresent()) {
					//the extraction might be in the class containing the anonymous class
					UMLAnonymousClass anonymous = addedOperation.getAnonymousClassContainer().get();
					String outerClassName = anonymous.getPackageName();
					UMLClassBaseDiff baseDiff = modelDiff.getUMLClassDiff(outerClassName);
					UMLAbstractClass outerClass = modelDiff.findClassInChildModel(outerClassName);
					if(baseDiff != null) {
						for(UMLOperation operation : baseDiff.getAddedOperations()) {
							if(addedOperationInvocation.matchesOperation(operation, addedOperation, this, modelDiff)) {
								operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
								operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllCreations());
								variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
								matchingAddedOperations.add(operation);
								break;
							}
						}
					}
					else if(outerClass != null) {
						for(UMLOperation operation : outerClass.getOperations()) {
							if(addedOperationInvocation.matchesOperation(operation, addedOperation, this, modelDiff)) {
								operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
								operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllCreations());
								variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(operation));
								matchingAddedOperations.add(operation);
								break;
							}
							for(UMLOperation nestedOperation : operation.getNestedOperations()) {
								if(addedOperationInvocation.matchesOperation(nestedOperation, addedOperation, this, modelDiff)) {
									operationInvocationsInMethodsCalledByAddedOperation.addAll(nestedOperation.getAllOperationInvocations());
									operationInvocationsInMethodsCalledByAddedOperation.addAll(nestedOperation.getAllCreations());
									variableDeclarationsInMethodsCalledByAddedOperation.addAll(getVariableDeclarationNamesInMethodBody(nestedOperation));
									matchingAddedOperations.add(nestedOperation);
									break;
								}
							}
						}
					}
				}
			}
		}
		if(LANG1.equals(Constants.TYPESCRIPT) && LANG2.equals(Constants.TYPESCRIPT) && matchingAddedOperations.size() > 0) {
			return true;
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
			if(invocation.getName().startsWith("get") || invocation.getName().equals("add") || invocation.getName().equals("contains") || invocation instanceof ObjectCreation) {
				operationInvocationIterator.remove();
			}
		}
		int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
		int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
		
		int numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations = newVariableDeclarationIntersection.size();
		int numberOfVariableDeclarationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfVariableDeclarationsMissingFromRemovedOperation - numberOfVariableDeclarationsInRemovedOperationFoundInOtherAddedOperations;
		
		//check for indirect method invocations
		List<String> variableReferences = addedOperation.getAllVariables();
		for(UMLAttribute attribute : nextClass.getAttributes()) {
			if(variableReferences.contains(attribute.getName()) && attribute.getVariableDeclaration().getInitializer() != null) {
				List<AbstractCall> invocations = attribute.getVariableDeclaration().getInitializer().getAllOperationInvocations();
				int matches = 0;
				for(AbstractCall invocation : invocations) {
					for(UMLOperation op : addedOperations) {
						if(op.getName().equals(invocation.getName())) {
							matches++;
							break;
						}
					}
				}
				if(matches > 0 && invocations.size() == matches) {
					return true;
				}
			}
		}
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
				if(a1 == null) {
					a1 = findAttributeInOriginalClass(PrefixSuffixUtils.normalize(mergedVariable, LANG1));
				}
				if(a1 != null) {
					mergedAttributes.add(a1);
					mergedVariables.add(a1.getVariableDeclaration());
				}
			}
			String after = merge.getAfter();
			if(merge.getAfter().contains("[")) {
				after = merge.getAfter().substring(0, merge.getAfter().indexOf("["));
			}
			UMLAttribute a2 = findAttributeInNextClass(after);
			if(a2 == null) {
				a2 = findAttributeInNextClass(PrefixSuffixUtils.normalize(after, LANG2));
			}
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
			int renamedVariables = 0;
			for(String splitVariable : split.getSplitVariables()) {
				UMLAttribute a2 = findAttributeInNextClass(splitVariable);
				if(a2 != null) {
					splitAttributes.add(a2);
					splitVariables.add(a2.getVariableDeclaration());
				}
				for(Replacement r : renameMap.keySet()) {
					if(r.getAfter().equals(splitVariable)) {
						renamedVariables++;
						break;
					}
				}
			}
			if(renamedVariables == splitVariables.size()) {
				continue;
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
			String before = pattern.getBefore();
			String after = pattern.getAfter();
			if(after.contains(".") && after.startsWith(LANG2.THIS_DOT)) {
				after = after.substring(after.lastIndexOf(".") + 1, after.length());
			}
			if(before.contains(".") && before.startsWith(LANG1.THIS_DOT)) {
				before = before.substring(before.lastIndexOf(".") + 1, before.length());
			}
			UMLAttribute a1 = findAttributeInOriginalClass(before);
			UMLAttribute a2 = findAttributeInNextClass(after);
			Set<CandidateAttributeRefactoring> set = renameMap.get(pattern);
			for(CandidateAttributeRefactoring candidate : set) {
				if(candidate.getOriginalVariableDeclaration() == null && candidate.getRenamedVariableDeclaration() == null) {
					if(a1 != null && a2 != null) {
						if((!originalClass.containsAttributeWithName(after) || cyclicRename(renameMap, pattern)) &&
								(!nextClass.containsAttributeWithName(before) || cyclicRename(renameMap, pattern)) &&
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
									RemoveParameterRefactoring remove = null;
									AddParameterRefactoring add = null;
									for(Refactoring r : originalRefactorings) {
										if(r instanceof RemoveParameterRefactoring ref && ref.getParameter().equals(a1.getVariableDeclaration())) {
											remove = ref;
										}
										else if(r instanceof AddParameterRefactoring ref && ref.getParameter().equals(a2.getVariableDeclaration())) {
											add = ref;
										}
									}
									if(remove != null && add != null) {
										refactorings.remove(remove);
										refactorings.remove(add);
										RenameVariableRefactoring rename = new RenameVariableRefactoring(remove.getParameter(), add.getParameter(), remove.getOperationBefore(), remove.getOperationAfter(), candidate.getReferences(), false);
										refactorings.add(rename);
									}
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
							boolean variableRemainsWithTheSameType = false;
							if(ref.getRefactoringType().equals(RefactoringType.REPLACE_VARIABLE_WITH_ATTRIBUTE)) {
								VariableDeclaration declaration = candidate.getOperationAfter().getVariableDeclaration(candidate.getOriginalVariableDeclaration().getVariableName());
								if(declaration != null && declaration.getType().equals(candidate.getOriginalVariableDeclaration().getType())) {
									variableRemainsWithTheSameType = true;
								}
							}
							if(!variableRemainsWithTheSameType) {
								if(!candidate.getOriginalVariableDeclaration().equalType(a2.getVariableDeclaration()) ||
										!candidate.getOriginalVariableDeclaration().equalQualifiedType(a2.getVariableDeclaration())) {
									ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(candidate.getOriginalVariableDeclaration(), a2.getVariableDeclaration(),
											candidate.getOperationBefore(), candidate.getOperationAfter(), candidate.getReferences(), false);
									refactoring.addRelatedRefactoring(ref);
									refactorings.add(refactoring);
								}
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
						boolean betterParameterMatchFound = false;
						if(modelDiff != null) {
							for(Refactoring r : modelDiff.getDetectedRefactorings()) {
								if(r instanceof ReplaceAnonymousWithClassRefactoring anonymous) {
									for(VariableDeclarationContainer container : anonymous.getAnonymousClass().getParentContainers()) {
										if(container.equals(a1) && candidate.getRenamedVariableDeclaration().isParameter()) {
											for(VariableDeclaration parameter : candidate.getOperationAfter().getParameterDeclarationList()) {
												if(!parameter.equals(candidate.getRenamedVariableDeclaration()) && parameter.getType().getClassType().equals(anonymous.getAddedClass().getNonQualifiedName())) {
													betterParameterMatchFound = true;
													break;
												}
											}
										}
										if(container.equals(a1)) {
											for(AbstractCodeMapping mapping : candidate.getReferences()) {
												AbstractCall call = mapping.getFragment2().invocationCoveringEntireFragment();
												if(call != null) {
													for(String arg : call.arguments()) {
														if(!arg.equals(candidate.getRenamedVariableDeclaration().getVariableName()) &&
																arg.contains(anonymous.getAddedClass().getNonQualifiedName())) {
															betterParameterMatchFound = true;
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
						RenameVariableRefactoring ref = new RenameVariableRefactoring(
								a1.getVariableDeclaration(), candidate.getRenamedVariableDeclaration(),
								candidate.getOperationBefore(), candidate.getOperationAfter(), candidate.getReferences(), false);
						if(!refactorings.contains(ref) && !betterParameterMatchFound) {
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
				String before = PrefixSuffixUtils.normalize(candidate.getOriginalVariableName(), LANG1);
				String after = PrefixSuffixUtils.normalize(candidate.getRenamedVariableName(), LANG2);
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
					before.add(PrefixSuffixUtils.normalize(mergedVariable, LANG1));
				}
				String after = PrefixSuffixUtils.normalize(candidate.getNewVariable(), LANG2);
				MergeVariableReplacement merge = new MergeVariableReplacement(before, after);
				processMerge(mergeMap, merge, candidate);
			}
		}
		for(CandidateSplitVariableRefactoring candidate : mapper.getCandidateAttributeSplits()) {
			Set<String> after = new LinkedHashSet<String>();
			for(String splitVariable : candidate.getSplitVariables()) {
				after.add(PrefixSuffixUtils.normalize(splitVariable, LANG2));
			}
			String before = PrefixSuffixUtils.normalize(candidate.getOldVariable(), LANG1);
			SplitVariableReplacement split = new SplitVariableReplacement(before, after);
			processSplit(splitMap, split, candidate);
		}
	}

	public int movedAttributeCount(CandidateMergeVariableRefactoring candidate) {
		UMLAttribute addedAttribute = null;
		for(UMLAttribute attribute : addedAttributes) {
			if(attribute.getName().equals(PrefixSuffixUtils.normalize(candidate.getNewVariable(), LANG2))) {
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
						if(attribute.getName().equals(PrefixSuffixUtils.normalize(mergedVariable, LANG1))) {
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
				String originalAttributeName = PrefixSuffixUtils.normalize(candidate.getOriginalVariableName(), LANG1);
				String renamedAttributeName = PrefixSuffixUtils.normalize(candidate.getRenamedVariableName(), LANG2);
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
										if(statement.getString().startsWith(variableDeclaration.getVariableName() + LANG1.ASSIGNMENT) ||
												statement.getString().startsWith(LANG1.THIS_DOT + variableDeclaration.getVariableName() + LANG1.ASSIGNMENT)) {
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
										if(statement.getString().startsWith(variableDeclaration.getVariableName() + LANG2.ASSIGNMENT) ||
												statement.getString().startsWith(LANG2.THIS_DOT + variableDeclaration.getVariableName() + LANG2.ASSIGNMENT)) {
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
					break;
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
					break;
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
		String topLevelClassName = null;
		if(!originalClass.isTopLevel() && !nextClass.isTopLevel()) {
			String originalTopLevelClass = originalClass.getPackageName();
			String nextTopLevelClass = nextClass.getPackageName();
			if(originalTopLevelClass.equals(nextTopLevelClass) && originalTopLevelClass.contains(".")) {
				topLevelClassName = originalTopLevelClass.substring(originalTopLevelClass.lastIndexOf(".") + 1, originalTopLevelClass.length());
			}
		}
		for(UMLOperationBodyMapper mapper : this.operationBodyMapperList) {
			List<String> allVariables1 = mapper.getContainer1().getAllVariables();
			List<String> allVariables2 = mapper.getContainer2().getAllVariables();
			for(UMLOperationBodyMapper nestedMapper : mapper.getChildMappers()) {
				allVariables1.addAll(nestedMapper.getContainer1().getAllVariables());
				allVariables2.addAll(nestedMapper.getContainer2().getAllVariables());
			}
			boolean variables1contains = (allVariables1.contains(pattern.getBefore()) &&
					!mapper.getParameterNameList1().contains(pattern.getBefore())) ||
					allVariables1.contains(LANG1.THIS_DOT+pattern.getBefore());
			if(!variables1contains && topLevelClassName != null) {
				variables1contains = allVariables1.contains(topLevelClassName + "." + LANG1.THIS_DOT+pattern.getBefore());
			}
			boolean variables2Contains = (allVariables2.contains(pattern.getAfter()) &&
					!mapper.getParameterNameList2().contains(pattern.getAfter())) ||
					allVariables2.contains(LANG2.THIS_DOT+pattern.getAfter());
			if(!variables2Contains && topLevelClassName != null) {
				variables2Contains = allVariables2.contains(topLevelClassName + "." + LANG2.THIS_DOT+pattern.getAfter());
			}
			if(variables1contains && !variables2Contains) {
				boolean skip = false;
				for(AbstractCodeMapping mapping : mapper.getMappings()) {
					for(AbstractCall call : mapping.getFragment2().getMethodInvocations()) {
						for(UMLOperation addedOperation : addedOperations) {
							if(call.matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
								List<String> addedOperationVariables = addedOperation.getAllVariables();
								if(addedOperationVariables.contains(pattern.getAfter()) || addedOperationVariables.contains(LANG2.THIS_DOT + pattern.getAfter())) {
									skip = true;
									break;
								}
							}
						}
						for(UMLOperationBodyMapper mapper2 : operationBodyMapperList) {
							if(!mapper.equals(mapper2)) {
								if(call.matchesOperation(mapper2.getContainer2(), mapper.getContainer2(), this, modelDiff)) {
									List<String> addedOperationVariables = mapper2.getContainer2().getAllVariables();
									if(addedOperationVariables.contains(pattern.getAfter()) || addedOperationVariables.contains(LANG2.THIS_DOT + pattern.getAfter())) {
										skip = true;
										break;
									}
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
								if(removedOperationVariables.contains(pattern.getBefore()) || removedOperationVariables.contains(LANG1.THIS_DOT + pattern.getBefore())) {
									skip = true;
									break;
								}
							}
						}
						for(UMLOperationBodyMapper mapper2 : operationBodyMapperList) {
							if(!mapper.equals(mapper2)) {
								if(call.matchesOperation(mapper2.getContainer1(), mapper.getContainer1(), this, modelDiff)) {
									List<String> removedOperationVariables = mapper2.getContainer1().getAllVariables();
									if(removedOperationVariables.contains(pattern.getBefore()) || removedOperationVariables.contains(LANG1.THIS_DOT + pattern.getBefore())) {
										skip = true;
										break;
									}
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

	private static UMLClass findEnumDeclaration(UMLModel model, String enumClassLiteral) {
		UMLClass enumClassDeclaration = null;
		for (UMLClass aClass : model.getClassList()) {
			if (aClass.getName().contains(enumClassLiteral)) {
				enumClassDeclaration = aClass;
			}
		}
		return enumClassDeclaration;
	}

	private static String getFirstParameterType(UMLOperation operation) {
		return operation.getParametersWithoutReturnType().get(0).getType().getClassType();
	}

	private Map<Integer, Integer> matchParamsWithRemovedStatements(List<List<String>> parameterValues, List<String> parameterNames, List<AbstractCodeFragment> nonMappedLeavesT1) {
		Map<Integer, Integer> matchingTestParameters = new LinkedHashMap<>();
		for(AbstractCodeFragment fragment : nonMappedLeavesT1) {
			if(fragment instanceof StatementObject && fragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.VARIABLE_DECLARATION_STATEMENT)) {
				StatementObject statement = (StatementObject)fragment;
				List<VariableDeclaration> declarations = statement.getVariableDeclarations();
				if(declarations.size() > 0 && declarations.get(0).getInitializer() != null) {
					String variableInitialValue = declarations.get(0).getInitializer().getString();
					for(int parameterIndex=0; parameterIndex<parameterValues.size(); parameterIndex++) {
						for(String value : parameterValues.get(parameterIndex)) {
							if(variableInitialValue.contains(sanitizeStringLiteral(value))) {
								int previousValue = matchingTestParameters.getOrDefault(parameterIndex, 0);
								matchingTestParameters.put(parameterIndex, previousValue + 1);
							}
						}
					}
				}
			}
		}
		return matchingTestParameters;
	}

	public static Map<Integer, Integer> matchParamsWithReplacements(List<List<String>> testParameters, List<String> parameterNames, Set<Replacement> replacements) {
		Map<Integer, Integer> matchingTestParameters = new LinkedHashMap<>();
		for(Replacement r : replacements) {
			if(parameterNames.contains(r.getAfter())) {
				String before = r.getBefore();
				matchParamsWithReplacement(before, testParameters, matchingTestParameters);
			}
			if(r instanceof MethodInvocationReplacement) {
				MethodInvocationReplacement m = (MethodInvocationReplacement)r;
				AbstractCall invocationBefore = m.getInvokedOperationBefore();
				AbstractCall invocationAfter = m.getInvokedOperationAfter();
				if(invocationBefore.arguments().size() == invocationAfter.arguments().size()) {
					for(int i=0; i<invocationBefore.arguments().size(); i++) {
						String argumentBefore = invocationBefore.arguments().get(i);
						String argumentAfter = invocationAfter.arguments().get(i);
						if(parameterNames.contains(argumentAfter)) {
							matchParamsWithReplacement(argumentBefore, testParameters, matchingTestParameters);
						}
					}
				}
				else if(invocationBefore.identicalName(invocationAfter)) {
					int min = Math.min(invocationBefore.arguments().size(), invocationAfter.arguments().size());
					for(int i=0; i<min; i++) {
						String argumentBefore = invocationBefore.arguments().get(i);
						String argumentAfter = invocationAfter.arguments().get(i);
						if(parameterNames.contains(argumentAfter)) {
							matchParamsWithReplacement(argumentBefore, testParameters, matchingTestParameters);
						}
					}
				}
			}
		}
		return matchingTestParameters;
	}

	private static void matchParamsWithReplacement(String before, List<List<String>> testParameters, Map<Integer, Integer> matchingTestParameters) {
		String paramsWithoutDoubleQuotes = sanitizeStringLiteral(before);
		for (int parameterRow = 0; parameterRow < testParameters.size(); parameterRow++) {
			if (testParameters.get(parameterRow).contains(paramsWithoutDoubleQuotes) ||
					testParameters.get(parameterRow).contains(before)) {
				Integer previousValue = matchingTestParameters.getOrDefault(parameterRow, 0);
				matchingTestParameters.put(parameterRow, previousValue + 1);
			}
			else if (paramsWithoutDoubleQuotes.contains(".")) {
				for(String s : testParameters.get(parameterRow)) {
					if(paramsWithoutDoubleQuotes.endsWith("." + s)) {
						Integer previousValue = matchingTestParameters.getOrDefault(parameterRow, 0);
						matchingTestParameters.put(parameterRow, previousValue + 1);
					}
				}
			}
		}
	}

	public static String sanitizeStringLiteral(String expression) {
		if (expression.startsWith("\"") && expression.endsWith("\"")) {
			return expression.substring(1, expression.length() - 1);
		} else if (expression.endsWith(".class")) {
			return expression.substring(0, expression.lastIndexOf(".class"));
		} else if (expression.contains(".")) {
			//return expression.substring(expression.lastIndexOf('.') + 1);
		}
		return expression;
	}

	public boolean containsExtractOperationRefactoring(VariableDeclarationContainer sourceOperationBeforeExtraction, UMLOperation extractedOperation) {
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractRef = (ExtractOperationRefactoring)ref;
				if(extractRef.getSourceOperationBeforeExtraction().equals(sourceOperationBeforeExtraction) &&
						extractRef.getExtractedOperation() instanceof UMLOperation op && op.equalSignature(extractedOperation)) {
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

	private int computeAbsoluteDifferenceInPositionWithinClass(UMLOperation removedOperation, UMLOperation addedOperation) {
		int index1 = originalClass.getOperations().indexOf(removedOperation);
		int index2 = nextClass.getOperations().indexOf(addedOperation);
		for(CandidateSplitMethodRefactoring candidate : candidateMethodSplits) {
			int splitMethodsBefore = 0;
			for(VariableDeclarationContainer splitMethod : candidate.getSplitMethods()) {
				int index = nextClass.getOperations().indexOf(splitMethod);
				if(index != -1 && index < index2) {
					splitMethodsBefore++;
				}
			}
			if(splitMethodsBefore == candidate.getSplitMethods().size()) {
				index2 -= (splitMethodsBefore-1);
			}
		}
		return Math.abs(index1-index2);
	}

	private boolean parameterTypeChanges(List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		if(removedOperations.size() == addedOperations.size()) {
			int count = 0;
			for(int i=0; i<removedOperations.size(); i++) {
				UMLOperation removedOperation = removedOperations.get(i);
				UMLOperation addedOperation = addedOperations.get(i);
				boolean migration = removedOperation.getName().toLowerCase().equals("test" + addedOperation.getName().toLowerCase());
				if((removedOperation.getName().equals(addedOperation.getName()) || migration) &&
						removedOperation.getParameters().size() == addedOperation.getParameters().size() &&
						removedOperation.isAbstract() == addedOperation.isAbstract()) {
					count++;
				}
			}
			return count == removedOperations.size();
		}
		return false;
	}

	private List<Pair<UMLOperation, UMLOperation>> operationAlignment(List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		List<Pair<UMLOperation, UMLOperation>> pairs = new ArrayList<Pair<UMLOperation,UMLOperation>>();
		if(originalClass.isTestClass() || nextClass.isTestClass()) {
			return pairs;
		}
		if(removedOperations.size() <= addedOperations.size()) {
			for(UMLOperation removedOperation : removedOperations) {
				List<UMLOperation> matchingOperations = new ArrayList<UMLOperation>();
				for(UMLOperation addedOperation : addedOperations) {
					if(matchCondition(removedOperation, addedOperation)) {
						if(removedOperation.isConstructor() && addedOperation.isConstructor()) {
							if(removedOperation.equalParameterTypes(addedOperation))
								matchingOperations.add(addedOperation);
						}
						else {
							matchingOperations.add(addedOperation);
						}
					}
				}
				if(matchingOperations.size() == 0) {
					for(UMLOperation addedOperation : addedOperations) {
						if(matchConditionDifferentNumberOfParameters(removedOperation, addedOperation)) {
							matchingOperations.add(addedOperation);
						}
					}
				}
				if(matchingOperations.size() == 1) {
					pairs.add(Pair.of(removedOperation, matchingOperations.get(0)));
				}
			}
		}
		else {
			for(UMLOperation addedOperation : addedOperations) {
				List<UMLOperation> matchingOperations = new ArrayList<UMLOperation>();
				for(UMLOperation removedOperation : removedOperations) {
					if(matchCondition(removedOperation, addedOperation)) {
						if(removedOperation.getBodyHashCode() == addedOperation.getBodyHashCode() && matchingOperations.size() > 0 && matchingOperations.get(0).delegatesTo(removedOperation, this, modelDiff) != null)
							continue;
						if(removedOperation.isConstructor() && addedOperation.isConstructor()) {
							if(removedOperation.equalParameterTypes(addedOperation))
								matchingOperations.add(removedOperation);
						}
						else {
							matchingOperations.add(removedOperation);
						}
					}
				}
				if(matchingOperations.size() == 0) {
					for(UMLOperation removedOperation : removedOperations) {
						if(matchConditionDifferentNumberOfParameters(removedOperation, addedOperation)) {
							matchingOperations.add(removedOperation);
						}
					}
				}
				if(matchingOperations.size() == 1) {
					pairs.add(Pair.of(matchingOperations.get(0), addedOperation));
				}
			}
		}
		return pairs;
	}

	private boolean matchCondition(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<String> removedOperationParameterNameList = removedOperation.getParameterNameList();
		List<String> addedOperationParameterNameList = addedOperation.getParameterNameList();
		if((removedOperation.getName().equals(addedOperation.getName()) &&
				removedOperation.getParameters().size() == addedOperation.getParameters().size() &&
				removedOperation.isAbstract() == addedOperation.isAbstract()) ||
				(removedOperation.getBodyHashCode() == addedOperation.getBodyHashCode() && removedOperation.getBodyHashCode() != 0)) {
			if(removedOperation.getParameterTypeList().equals(addedOperation.getParameterTypeList()) &&
					removedOperationParameterNameList.equals(addedOperationParameterNameList)) {
				return true;
			}
			int found = 0;
			if(removedOperationParameterNameList.size() == addedOperationParameterNameList.size()) {
				for(int i=0; i<removedOperationParameterNameList.size(); i++) {
					String removedOperationParameterName = removedOperationParameterNameList.get(i);
					int index = addedOperationParameterNameList.indexOf(removedOperationParameterName);
					if(index != -1) {
						found++;
					}
				}
			}
			return found >= removedOperationParameterNameList.size()-1;
		}
		if((removedOperation.getName().contains(addedOperation.getName()) || addedOperation.getName().contains(removedOperation.getName())) &&
				removedOperationParameterNameList.size() == addedOperationParameterNameList.size() &&
				removedOperation.isAbstract() == addedOperation.isAbstract()) {
			int count = 0;
			for(int i=0; i<removedOperationParameterNameList.size(); i++) {
				String removedOperationParameterName = removedOperationParameterNameList.get(i);
				String addedOperationParameterName = addedOperationParameterNameList.get(i);
				if(removedOperationParameterName.equals(addedOperationParameterName)) {
					count++;
				}
			}
			return count >= removedOperationParameterNameList.size()-1;
		}
		return false;
	}

	private boolean matchConditionDifferentNumberOfParameters(UMLOperation removedOperation, UMLOperation addedOperation) {
		if((removedOperation.getName().contains(addedOperation.getName()) || addedOperation.getName().contains(removedOperation.getName())) &&
				!removedOperation.isConstructor() && !addedOperation.isConstructor() &&
				removedOperation.getParameters().size() != addedOperation.getParameters().size() &&
				removedOperation.isAbstract() == addedOperation.isAbstract()) {
			List<String> removedOperationParameterNameList = removedOperation.getParameterNameList();
			List<String> addedOperationParameterNameList = addedOperation.getParameterNameList();
			int notFound = 0;
			int found = 0;
			int maxDifference = 0;
			if(removedOperationParameterNameList.size() < addedOperationParameterNameList.size()) {
				for(int i=0; i<removedOperationParameterNameList.size(); i++) {
					String removedOperationParameterName = removedOperationParameterNameList.get(i);
					int index = addedOperationParameterNameList.indexOf(removedOperationParameterName);
					if(index != -1) {
						found++;
						int difference = Math.abs(i-index);
						if(difference > maxDifference) {
							maxDifference = difference;
						}
					}
					else {
						notFound++;
					}
				}
			}
			else if(removedOperationParameterNameList.size() > addedOperationParameterNameList.size()) {
				for(int i=0; i<addedOperationParameterNameList.size(); i++) {
					String addedOperationParameterName = addedOperationParameterNameList.get(i);
					int index = removedOperationParameterNameList.indexOf(addedOperationParameterName);
					if(index != -1) {
						found++;
						int difference = Math.abs(i-index);
						if(difference > maxDifference) {
							maxDifference = difference;
						}
					}
					else {
						notFound++;
					}
				}
			}
			return notFound == 0 &&
					found == Math.max(removedOperationParameterNameList.size(), addedOperationParameterNameList.size()) - maxDifference &&
					maxDifference == Math.abs(removedOperationParameterNameList.size() - addedOperationParameterNameList.size());
		}
		return false;
	}

	private boolean conflictingPairs(List<Pair<UMLOperation, UMLOperation>> pairs) {
		Map<UMLOperation, Integer> removedOperationCountMap = new LinkedHashMap<>();
		Map<UMLOperation, Integer> addedOperationCountMap = new LinkedHashMap<>();
		for(Pair<UMLOperation, UMLOperation> p : pairs) {
			if(p.getLeft().getName().equals(p.getRight().getName())) {
				continue;
			}
			UMLOperation removedOperation = p.getLeft();
			if(removedOperationCountMap.containsKey(removedOperation)) {
				removedOperationCountMap.put(removedOperation, removedOperationCountMap.get(removedOperation) + 1);
			}
			else {
				removedOperationCountMap.put(removedOperation, 1);
			}
			UMLOperation addedOperation = p.getRight();
			if(addedOperationCountMap.containsKey(addedOperation)) {
				addedOperationCountMap.put(addedOperation, addedOperationCountMap.get(addedOperation) + 1);
			}
			else {
				addedOperationCountMap.put(addedOperation, 1);
			}
		}
		for(Integer count : removedOperationCountMap.values()) {
			if(count > 1) {
				return true;
			}
		}
		for(Integer count : addedOperationCountMap.values()) {
			if(count > 1) {
				return true;
			}
		}
		return false;
	}

	protected void checkForOperationSignatureChanges(List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) throws RefactoringMinerTimedOutException {
		for(UMLOperation op1 : removedOperations) {
			for(UMLOperation op2 : removedOperations) {
				if(!op1.equals(op2) && op1.delegatesTo(op2, this, modelDiff) != null) {
					removedOperationDelegates++;
					break;
				}
			}
		}
		boolean junit3Migration = nextClass.importsType("org.junit.Test") != originalClass.importsType("org.junit.Test");
		if(parameterTypeChanges(removedOperations, addedOperations)) {
			this.consistentMethodInvocationRenames = new HashMap<MethodInvocationReplacement, UMLOperationBodyMapper>();
			Set<VariableDeclarationContainer> removedOperationsToBeRemoved = new LinkedHashSet<>();
			Set<VariableDeclarationContainer> addedOperationsToBeRemoved = new LinkedHashSet<>();
			for(int i=0; i<removedOperations.size(); i++) {
				UMLOperation removedOperation = removedOperations.get(i);
				UMLOperation addedOperation = addedOperations.get(i);
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				updateMapperSet(mapperSet, removedOperation, addedOperation, 0);
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = mapperSet.first();
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
					for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
						if(containCallToOperation(bestMapper.getContainer1(), mapper.getContainer1()) && containCallToOperation(bestMapper.getContainer2(), mapper.getContainer2())) {
							Pair<UMLOperationBodyMapper, UMLOperationBodyMapper> pair = Pair.of(bestMapper, mapper);
							calledBy.add(pair);
						}
					}
					this.addOperationBodyMapper(bestMapper);
				}
			}
			removedOperations.removeAll(removedOperationsToBeRemoved);
			addedOperations.removeAll(addedOperationsToBeRemoved);
		}
		List<Pair<UMLOperation, UMLOperation>> pairs = operationAlignment(removedOperations, addedOperations);
		if(pairs.size() == Math.min(removedOperations.size(), addedOperations.size()) && !conflictingPairs(pairs)) {
			consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
			Set<VariableDeclarationContainer> removedOperationsToBeRemoved = new LinkedHashSet<>();
			Set<VariableDeclarationContainer> addedOperationsToBeRemoved = new LinkedHashSet<>();
			for(Pair<UMLOperation, UMLOperation> p : pairs) {
				UMLOperation removedOperation = p.getLeft();
				UMLOperation addedOperation = p.getRight();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				int maxDifferenceInPosition;
				if(removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
					maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
				}
				else {
					maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
				}
				updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
				if(!mapperSet.isEmpty()) {
					UMLOperationBodyMapper bestMapper = mapperSet.first();
					if(bestMapper.getOperation1().equalSignature(bestMapper.getOperation2()) || (!containsMapperForOperation1(bestMapper.getOperation1()) && !containsMapperForOperation2(bestMapper.getOperation2()))) {
						removedOperationsToBeRemoved.add(removedOperation);
						addedOperationsToBeRemoved.add(addedOperation);
						if(!removedOperation.getName().equals(addedOperation.getName()) &&
								!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
							Set<MethodInvocationReplacement> callReferences = getCallReferences(removedOperation, addedOperation);
							RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper, callReferences);
							refactorings.add(rename);
						}
						for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
							if(containCallToOperation(bestMapper.getContainer1(), mapper.getContainer1()) && containCallToOperation(bestMapper.getContainer2(), mapper.getContainer2())) {
								Pair<UMLOperationBodyMapper, UMLOperationBodyMapper> pair = Pair.of(bestMapper, mapper);
								calledBy.add(pair);
							}
						}
						this.addOperationBodyMapper(bestMapper);
						consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
					}
					else {
						removedOperationsToBeRemoved.add(removedOperation);
						addedOperationsToBeRemoved.add(addedOperation);
						ExtractOperationRefactoring extract = new ExtractOperationRefactoring(bestMapper, bestMapper.getContainer2(), Collections.emptyList());
						this.refactorings.add(extract);
					}
				}
				else if(removedOperation.equalSignature(addedOperation) && originalClass.isInterface() == nextClass.isInterface() &&
						removedOperation.hasEmptyBody() == addedOperation.hasEmptyBody()) {
					UMLOperationBodyMapper bestMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
					for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
						if(containCallToOperation(bestMapper.getContainer1(), mapper.getContainer1()) && containCallToOperation(bestMapper.getContainer2(), mapper.getContainer2())) {
							Pair<UMLOperationBodyMapper, UMLOperationBodyMapper> pair = Pair.of(bestMapper, mapper);
							calledBy.add(pair);
						}
					}
					this.addOperationBodyMapper(bestMapper);
					consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
				}
				else if(removedOperation.getComments().size() > 0 && addedOperation.getComments().size() > 0 && removedOperation.identicalComments(addedOperation)) {
					UMLOperationBodyMapper bestMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
					if(!removedOperation.getName().equals(addedOperation.getName()) &&
							!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
						Set<MethodInvocationReplacement> callReferences = getCallReferences(removedOperation, addedOperation);
						RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper, callReferences);
						refactorings.add(rename);
					}
					for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
						if(containCallToOperation(bestMapper.getContainer1(), mapper.getContainer1()) && containCallToOperation(bestMapper.getContainer2(), mapper.getContainer2())) {
							Pair<UMLOperationBodyMapper, UMLOperationBodyMapper> pair = Pair.of(bestMapper, mapper);
							calledBy.add(pair);
						}
					}
					this.addOperationBodyMapper(bestMapper);
					consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
				}
			}
			removedOperations.removeAll(removedOperationsToBeRemoved);
			addedOperations.removeAll(addedOperationsToBeRemoved);
			return;
		}
		consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
		int initialNumberOfRemovedOperations = removedOperations.size();
		int initialNumberOfAddedOperations = addedOperations.size();
		if(removedOperations.size() <= addedOperations.size()) {
			Set<VariableDeclarationContainer> removedOperationsToBeRemoved = new LinkedHashSet<>();
			for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
				UMLOperation removedOperation = removedOperationIterator.next();
				if(isCommentedOut(removedOperation)) {
					continue;
				}
				double removedOperationBuilderStatementRatio = removedOperation.builderStatementRatio();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
					if((removedOperation.hasTestAnnotation() && addedOperation.getAnnotations().isEmpty()) ||
							(addedOperation.hasTestAnnotation() && removedOperation.getAnnotations().isEmpty())) {
						// exclude case of JUnit 3 to JUnit 4 migration
						if(!junit3Migration)
							continue;
					}
					if(!containsMapperForOperation1(removedOperation) && !containsMapperForOperation2(addedOperation) && !existingMapperDelegatesToAddedOperation(addedOperation) &&
							removedOperationBuilderStatementRatio < BUILDER_STATEMENT_RATIO_THRESHOLD && addedOperation.builderStatementRatio() < BUILDER_STATEMENT_RATIO_THRESHOLD) {
						int maxDifferenceInPosition;
						if(removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
							maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
						}
						else if(removedOperation.hasTestAnnotation() && addedOperation.hasParameterizedTestAnnotation()) {
							maxDifferenceInPosition = initialNumberOfRemovedOperations + initialNumberOfAddedOperations;
						}
						else {
							maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
						}
						updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
						List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
						for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
							updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
						}
						if(initialNumberOfRemovedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && initialNumberOfAddedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && mapperSet.size() > 0 &&
								removedOperation.getName().equals(addedOperation.getName())) {
							break;
						}
					}
				}
				if(!mapperSet.isEmpty()) {
					boolean firstMapperWithIdenticalMethodName = false;
					UMLOperationBodyMapper firstMapper = mapperSet.first();
					if(firstMapper.getContainer1().getName().equals(firstMapper.getContainer2().getName())) {
						firstMapperWithIdenticalMethodName = true;
					}
					boolean matchingMergeCandidateFound = false;
					boolean matchingSplitCandidateFound = false;
					//infer split method
					Set<UMLOperationBodyMapper> exactMappers = new LinkedHashSet<>();
					CandidateSplitMethodRefactoring newCandidate = new CandidateSplitMethodRefactoring();
					newCandidate.setOriginalMethodBeforeSplit(removedOperation);
					for(UMLOperationBodyMapper mapper : mapperSet) {
						if(mapper.allMappingsAreIdentical() && !mapper.getContainer2().isGetter() && !mapper.getContainer2().isSetter()) {
							exactMappers.add(mapper);
							newCandidate.addSplitMethod(mapper.getContainer2());
						}
					}
					if(exactMappers.size() < mapperSet.size() && exactMappers.size() > 1) {
						candidateMethodSplits.add(newCandidate);
					}
					Map<AbstractCodeFragment, Set<UMLOperationBodyMapper>> uniqueAndCommonMappings = new LinkedHashMap<>();
					Map<Integer, Set<UMLOperationBodyMapper>> mappingCounter = new LinkedHashMap<>();
					Set<UMLOperationBodyMapper> mappersWithZeroAddedStatements = new LinkedHashSet<>();
					Set<UMLOperationBodyMapper> mappersWithParameterizedTest = new LinkedHashSet<>();
					for(UMLOperationBodyMapper mapper : mapperSet) {
						if(!mapper.getContainer1().hasParameterizedTestAnnotation() && mapper.getContainer2().hasParameterizedTestAnnotation()) {
							mappersWithParameterizedTest.add(mapper);
						}
						for(AbstractCodeMapping mapping : mapper.getMappings()) {
							if(uniqueAndCommonMappings.containsKey(mapping.getFragment1())) {
								uniqueAndCommonMappings.get(mapping.getFragment1()).add(mapper);
							}
							else {
								Set<UMLOperationBodyMapper> mappers = new LinkedHashSet<>();
								mappers.add(mapper);
								uniqueAndCommonMappings.put(mapping.getFragment1(), mappers);
							}
						}
						if(mapper.getNonMappedInnerNodesT2().size() == 0 && mapper.getNonMappedLeavesT2().size() == 0) {
							mappersWithZeroAddedStatements.add(mapper);
						}
						int mappingCount = mapper.getMappings().size();
						if(mappingCounter.containsKey(mappingCount)) {
							mappingCounter.get(mappingCount).add(mapper);
						}
						else {
							Set<UMLOperationBodyMapper> mappers = new LinkedHashSet<>();
							mappers.add(mapper);
							mappingCounter.put(mappingCount, mappers);
						}
					}
					int commonMappingCount = 0;
					int uniqueMappingCount = 0;
					for(AbstractCodeFragment fragment : uniqueAndCommonMappings.keySet()) {
						if(uniqueAndCommonMappings.get(fragment).size() == mapperSet.size()) {
							commonMappingCount++;
						}
						else if(uniqueAndCommonMappings.get(fragment).size() == 1) {
							uniqueMappingCount++;
						}
					}
					if((initialNumberOfAddedOperations <= 2 || initialNumberOfRemovedOperations <= 2 || mappersWithParameterizedTest.size() == mapperSet.size()) && commonMappingCount > 0 && uniqueMappingCount > 0 && (mappingCounter.size() == 1 || mappersWithZeroAddedStatements.size() == mapperSet.size())) {
						for(UMLOperationBodyMapper mapper : mapperSet) {
							if(!mapper.getContainer1().getName().equals(mapper.getContainer2().getName()) && !mapper.getContainer2().isGetter() && !mapper.getContainer2().isSetter()) {
								newCandidate.addSplitMethod(mapper.getContainer2());
							}
						}
						boolean oneMapperCallsTheOther = false;
						for(VariableDeclarationContainer containerI : newCandidate.getSplitMethods()) {
							for(VariableDeclarationContainer containerJ : newCandidate.getSplitMethods()) {
								if(!containerI.equals(containerJ)) {
									for(AbstractCall call : containerI.getAllOperationInvocations()) {
										if(call.matchesOperation(containerJ, containerI, this, modelDiff)) {
											oneMapperCallsTheOther = true;
											break;
										}
									}
								}
								if(oneMapperCallsTheOther) {
									break;
								}
							}
							if(oneMapperCallsTheOther) {
								break;
							}
						}
						if(newCandidate.getSplitMethods().size() > 1 && !oneMapperCallsTheOther) {
							candidateMethodSplits.add(newCandidate);
						}
					}
					if(!firstMapperWithIdenticalMethodName) {
						for(CandidateMergeMethodRefactoring candidate : candidateMethodMerges) {
							Set<VariableDeclarationContainer> methodsWithMapper = new LinkedHashSet<>();
							int perfectMappers = 0;
							for(UMLOperationBodyMapper mapper : mapperSet) {
								if(candidate.getMergedMethods().contains(mapper.getContainer1()) && candidate.getNewMethodAfterMerge().equals(mapper.getContainer2())) {
									candidate.addMapper(mapper);
									if(isPerfectMapper(mapper)) {
										perfectMappers++;
									}
									methodsWithMapper.add(mapper.getContainer1());
									matchingMergeCandidateFound = true;
								}
							}
							if(perfectMappers > 0 && perfectMappers < mapperSet.size()) {
								matchingMergeCandidateFound = false;
							}
							if(matchingMergeCandidateFound && candidate.getMappers().size() < candidate.getMergedMethods().size()) {
								for(VariableDeclarationContainer deletedOperation : candidate.getMergedMethods()) {
									if(!methodsWithMapper.contains(deletedOperation)) {
										UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper((UMLOperation)deletedOperation, (UMLOperation)candidate.getNewMethodAfterMerge(), this);
										candidate.addMapper(operationBodyMapper);
									}
								}
							}
						}
						for(CandidateSplitMethodRefactoring candidate : candidateMethodSplits) {
							Set<VariableDeclarationContainer> methodsWithMapper = new LinkedHashSet<>();
							int perfectMappers = 0;
							for(UMLOperationBodyMapper mapper : mapperSet) {
								if(candidate.getSplitMethods().contains(mapper.getContainer2()) && candidate.getOriginalMethodBeforeSplit().equals(mapper.getContainer1())) {
									candidate.addMapper(mapper);
									if(isPerfectMapper(mapper)) {
										perfectMappers++;
									}
									methodsWithMapper.add(mapper.getContainer2());
									matchingSplitCandidateFound = true;
								}
							}
							if(perfectMappers > 0 && perfectMappers < mapperSet.size()) {
								matchingSplitCandidateFound = false;
							}
							if(matchingSplitCandidateFound && candidate.getMappers().size() < candidate.getSplitMethods().size()) {
								for(VariableDeclarationContainer addedOperation : candidate.getSplitMethods()) {
									if(!methodsWithMapper.contains(addedOperation)) {
										UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, (UMLOperation)addedOperation, this);
										candidate.addMapper(operationBodyMapper);
									}
								}
							}
						}
					}
					if(!matchingMergeCandidateFound && !matchingSplitCandidateFound) {
						UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
						int size = mapperSet.size();
						betterMatchForAddedOperation(mapperSet, bestMapper);
						if(mapperSet.size() > size) {
							bestMapper = findBestMapper(mapperSet);
						}
						if(bestMapper != null && !modelDiffContainsConflictingMoveOperationRefactoring(bestMapper) && !potentialExtractFixture(bestMapper) && !conflictWithExtractMethodCandidate(bestMapper)) {
							removedOperation = bestMapper.getOperation1();
							UMLOperation addedOperation = bestMapper.getOperation2();
							addedOperations.remove(addedOperation);
							removedOperationIterator.remove();
							if(!removedOperation.getName().equals(addedOperation.getName()) &&
									!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
								Set<MethodInvocationReplacement> callReferences = getCallReferences(removedOperation, addedOperation);
								RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper, callReferences);
								refactorings.add(rename);
							}
							for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
								if(containCallToOperation(bestMapper.getContainer1(), mapper.getContainer1()) && containCallToOperation(bestMapper.getContainer2(), mapper.getContainer2())) {
									Pair<UMLOperationBodyMapper, UMLOperationBodyMapper> pair = Pair.of(bestMapper, mapper);
									calledBy.add(pair);
								}
							}
							this.addOperationBodyMapper(bestMapper);
							consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
							processNestedTypeDeclarationStatements(removedOperation, addedOperation);
						}
					}
					else {
						Set<VariableDeclarationContainer> addedOperationsToBeRemoved = new LinkedHashSet<>();
						for(CandidateMergeMethodRefactoring candidate : candidateMethodMerges) {
							addedOperationsToBeRemoved.add(candidate.getNewMethodAfterMerge());
							removedOperationsToBeRemoved.addAll(candidate.getMergedMethods());
							MergeOperationRefactoring merge = new MergeOperationRefactoring(candidate.getMergedMethods(), candidate.getNewMethodAfterMerge(), getOriginalClassName(), getNextClassName(), candidate.getMappers());
							refactorings.add(merge);
							for(UMLOperationBodyMapper mapper : merge.getMappers()) {
								mapper.computeRefactoringsWithinBody();
								refactorings.addAll(mapper.getRefactoringsAfterPostProcessing());
								getCandidateAttributeRenames().addAll(mapper.getCandidateAttributeRenames());
							}
						}
						//group split based on original method
						Map<VariableDeclarationContainer, List<CandidateSplitMethodRefactoring>> map = new HashMap<>();
						for(CandidateSplitMethodRefactoring candidate : candidateMethodSplits) {
							if(map.containsKey(candidate.getOriginalMethodBeforeSplit())) {
								map.get(candidate.getOriginalMethodBeforeSplit()).add(candidate);
							}
							else {
								List<CandidateSplitMethodRefactoring> list = new ArrayList<>();
								list.add(candidate);
								map.put(candidate.getOriginalMethodBeforeSplit(), list);
							}
						}
						for(VariableDeclarationContainer key : map.keySet()) {
							Set<VariableDeclarationContainer> splitMethods = new LinkedHashSet<>();
							Set<UMLOperationBodyMapper> splitMappers = new LinkedHashSet<>();
							for(CandidateSplitMethodRefactoring candidate : map.get(key)) {
								addedOperationsToBeRemoved.addAll(candidate.getSplitMethods());
								removedOperationsToBeRemoved.add(candidate.getOriginalMethodBeforeSplit());
								splitMethods.addAll(candidate.getSplitMethods());
								splitMappers.addAll(candidate.getMappers());
							}
							SplitOperationRefactoring split = new SplitOperationRefactoring(key, splitMethods, getOriginalClassName(), getNextClassName(), splitMappers);
							refactorings.add(split);
							for(UMLOperationBodyMapper mapper : split.getMappers()) {
								mapper.computeRefactoringsWithinBody();
								refactorings.addAll(mapper.getRefactoringsAfterPostProcessing());
								getCandidateAttributeRenames().addAll(mapper.getCandidateAttributeRenames());
							}
						}
						if(candidateMethodMerges.size() > 0 || candidateMethodSplits.size() > 0) {
							removedOperationIterator.remove();
						}
						addedOperations.removeAll(addedOperationsToBeRemoved);
					}
				}
			}
			removedOperations.removeAll(removedOperationsToBeRemoved);
		}
		else {
			Set<VariableDeclarationContainer> addedOperationsToBeRemoved = new LinkedHashSet<>();
			for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
				UMLOperation addedOperation = addedOperationIterator.next();
				double addedOperationBuilderStatementRatio = addedOperation.builderStatementRatio();
				TreeSet<UMLOperationBodyMapper> mapperSet = new TreeSet<UMLOperationBodyMapper>();
				for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
					UMLOperation removedOperation = removedOperationIterator.next();
					if((removedOperation.hasTestAnnotation() && addedOperation.getAnnotations().isEmpty()) ||
							(addedOperation.hasTestAnnotation() && removedOperation.getAnnotations().isEmpty())) {
						// exclude case of JUnit 3 to JUnit 4 migration
						if(!junit3Migration)
							continue;
					}
					if(!containsMapperForOperation1(removedOperation) && !containsMapperForOperation2(addedOperation) &&
							removedOperation.builderStatementRatio() < BUILDER_STATEMENT_RATIO_THRESHOLD && addedOperationBuilderStatementRatio < BUILDER_STATEMENT_RATIO_THRESHOLD) {
						int maxDifferenceInPosition;
						if(removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
							maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
						}
						else if(removedOperation.hasTestAnnotation() && addedOperation.hasParameterizedTestAnnotation()) {
							maxDifferenceInPosition = initialNumberOfRemovedOperations + initialNumberOfAddedOperations;
						}
						else {
							maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
						}
						updateMapperSet(mapperSet, removedOperation, addedOperation, maxDifferenceInPosition);
						List<UMLOperation> operationsInsideAnonymousClass = addedOperation.getOperationsInsideAnonymousClass(this.addedAnonymousClasses);
						for(UMLOperation operationInsideAnonymousClass : operationsInsideAnonymousClass) {
							updateMapperSet(mapperSet, removedOperation, operationInsideAnonymousClass, addedOperation, maxDifferenceInPosition);
						}
						if(initialNumberOfRemovedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && initialNumberOfAddedOperations >= MAXIMUM_NUMBER_OF_COMPARED_METHODS && mapperSet.size() > 0 &&
								removedOperation.getName().equals(addedOperation.getName())) {
							break;
						}
					}
				}
				if(!mapperSet.isEmpty()) {
					boolean firstMapperWithIdenticalMethodName = false;
					UMLOperationBodyMapper firstMapper = mapperSet.first();
					if(firstMapper.getContainer1().getName().equals(firstMapper.getContainer2().getName())) {
						firstMapperWithIdenticalMethodName = true;
					}
					boolean matchingMergeCandidateFound = false;
					boolean matchingSplitCandidateFound = false;
					if(!firstMapperWithIdenticalMethodName) {
						for(CandidateMergeMethodRefactoring candidate : candidateMethodMerges) {
							Set<VariableDeclarationContainer> methodsWithMapper = new LinkedHashSet<>();
							int perfectMappers = 0;
							for(UMLOperationBodyMapper mapper : mapperSet) {
								if(candidate.getMergedMethods().contains(mapper.getContainer1()) && candidate.getNewMethodAfterMerge().equals(mapper.getContainer2())) {
									candidate.addMapper(mapper);
									if(isPerfectMapper(mapper)) {
										perfectMappers++;
									}
									methodsWithMapper.add(mapper.getContainer1());
									matchingMergeCandidateFound = true;
								}
							}
							if(perfectMappers > 0 && perfectMappers < mapperSet.size()) {
								matchingMergeCandidateFound = false;
							}
							if(matchingMergeCandidateFound && candidate.getMappers().size() < candidate.getMergedMethods().size()) {
								for(VariableDeclarationContainer removedOperation : candidate.getMergedMethods()) {
									if(!methodsWithMapper.contains(removedOperation)) {
										UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper((UMLOperation)removedOperation, addedOperation, this);
										candidate.addMapper(operationBodyMapper);
									}
								}
							}
						}
						for(CandidateSplitMethodRefactoring candidate : candidateMethodSplits) {
							Set<VariableDeclarationContainer> methodsWithMapper = new LinkedHashSet<>();
							int perfectMappers = 0;
							for(UMLOperationBodyMapper mapper : mapperSet) {
								if(candidate.getSplitMethods().contains(mapper.getContainer2()) && candidate.getOriginalMethodBeforeSplit().equals(mapper.getContainer1())) {
									candidate.addMapper(mapper);
									if(isPerfectMapper(mapper)) {
										perfectMappers++;
									}
									methodsWithMapper.add(mapper.getContainer2());
									matchingSplitCandidateFound = true;
								}
							}
							if(perfectMappers > 0 && perfectMappers < mapperSet.size()) {
								matchingSplitCandidateFound = false;
							}
							if(matchingSplitCandidateFound && candidate.getMappers().size() < candidate.getSplitMethods().size()) {
								for(VariableDeclarationContainer insertedOperation : candidate.getSplitMethods()) {
									if(!methodsWithMapper.contains(insertedOperation)) {
										UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper((UMLOperation)candidate.getOriginalMethodBeforeSplit(), (UMLOperation)insertedOperation, this);
										candidate.addMapper(operationBodyMapper);
									}
								}
							}
						}
					}
					if(!matchingMergeCandidateFound && !matchingSplitCandidateFound) {
						List<List<String>> parameterValues = getParameterValues(addedOperation);
						if(addedOperation.hasParameterizedTestAnnotation() && !parameterValues.isEmpty() && !firstMapper.getContainer1().hasParameterizedTestAnnotation()) {
							Set<UMLOperationBodyMapper> filteredMapperSet = new LinkedHashSet<UMLOperationBodyMapper>();
							List<String> parameterNames = addedOperation.getParameterNameList();
							int overallMaxMatchingTestParameters = -1;
							Map<Integer, Integer> overallMatchingTestParameters = new LinkedHashMap<Integer, Integer>();
							boolean internalParameterizeTest = false;
							Set<String> commonTokensInName = null;
							for(UMLOperationBodyMapper mapper : mapperSet) {
								Set<String> commonTokens = new LinkedHashSet<>();
								String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(mapper.getContainer1().getName());
								String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(mapper.getContainer2().getName());
								boolean commonTokenCheck = false;
								for(String token1 : tokens1) {
									for(String token2 : tokens2) {
										if(token1.equals(token2) || token1.startsWith(token2)) {
											commonTokens.add(token2);
										}
									}
								}
								if(commonTokensInName == null) {
									commonTokensInName = commonTokens;
									if(Arrays.equals(tokens1, tokens2)) {
										commonTokenCheck = true;
									}
								}
								else if(commonTokens.size() > commonTokensInName.size()) {
									commonTokensInName = commonTokens;
									filteredMapperSet.clear();
								}
								else if(commonTokensInName.equals(commonTokens)) {
									commonTokenCheck = true;
								}
								if(mapper.getInternalParameterizeTestMultiMappings().size() > 0) {
									internalParameterizeTest = true;
								}
								Map<Integer, Integer> matchingTestParameters = matchParamsWithReplacements(parameterValues, parameterNames, mapper.getReplacements());
								if (matchingTestParameters.isEmpty()) {
									matchingTestParameters = matchParamsWithRemovedStatements(parameterValues, parameterNames, mapper.getNonMappedLeavesT1());
								}
								int max = matchingTestParameters.isEmpty() ? 0 : Collections.max(matchingTestParameters.values());
								if(max >= 1 && (overallMaxMatchingTestParameters == -1 || max >= overallMaxMatchingTestParameters)) {
									if(max > overallMaxMatchingTestParameters) {
										overallMaxMatchingTestParameters = max;
									}
									int exactMatchCount = 0;
									for(Map.Entry<Integer, Integer> entry  : matchingTestParameters.entrySet()) {
										if(overallMatchingTestParameters.containsKey(entry.getKey()) &&
												overallMatchingTestParameters.get(entry.getKey()).equals(entry.getValue())) {
											exactMatchCount++;
										}
									}
									boolean containsAll = exactMatchCount == matchingTestParameters.size() && exactMatchCount > 0;
									int sizeBefore = overallMatchingTestParameters.size();
									overallMatchingTestParameters.putAll(matchingTestParameters);
									if(internalParameterizeTest) {
										if(overallMatchingTestParameters.size() > sizeBefore) {
											filteredMapperSet.add(mapper);
										}
										else if(overallMatchingTestParameters.size() == sizeBefore && !containsAll) {
											filteredMapperSet.add(mapper);
										}
									}
									else {
										filteredMapperSet.add(mapper);
									}
								}
								else if(commonTokenCheck) {
									filteredMapperSet.add(mapper);
								}
							}
							//cluster mappers based on number of mappings and number of total replacements
							Set<UMLOperationBodyMapper> filteredMapperSet2 = new LinkedHashSet<UMLOperationBodyMapper>();
							int maxMappings = -1;
							int minReplacements = Integer.MAX_VALUE;
							for(UMLOperationBodyMapper mapper : filteredMapperSet) {
								int mappings = mapper.countMappingsForInternalParameterizedTest();
								if(mappings > maxMappings) {
									maxMappings = mappings;
								}
								int replacements = mapper.countReplacementsForInternalParameterizedTest();
								if(replacements < minReplacements) {
									minReplacements = replacements;
								}
								if(mappings == maxMappings && replacements == minReplacements) {
									filteredMapperSet2.add(mapper);
								}
								else if(mappings < maxMappings && replacements >= minReplacements && mapper.getOperation1().getName().contains(mapper.getOperation2().getName())) {
									filteredMapperSet2.add(mapper);
								}
							}
							for(UMLOperationBodyMapper mapper : filteredMapperSet2) {
								ParameterizeTestRefactoring refactoring = new ParameterizeTestRefactoring(mapper);
								refactorings.add(refactoring);
								mapper.computeRefactoringsWithinBody();
								refactorings.addAll(mapper.getRefactoringsAfterPostProcessing());
								UMLOperation removedOperation = mapper.getOperation1();
								removedOperations.remove(removedOperation);
								//check for JUnit migration from @Parameterized.Parameters to @ParameterizedTest
								for(UMLOperation removed : removedOperations) {
									if(removed.hasParametersAnnotation()) {
										List<List<LeafExpression>> parameters2 = getParameterValuesAsLeafExpressions(addedOperation);
										List<List<LeafExpression>> parameters1 = getParameterValuesAsLeafExpressions(removed);
										if(parameters1.size() == parameters2.size()) {
											for(int i=0; i<parameters1.size(); i++) {
												List<LeafExpression> expressions1 = parameters1.get(i);
												List<LeafExpression> expressions2 = parameters2.get(i);
												if(expressions1.size() == expressions2.size()) {
													for(int j=0; j<expressions1.size(); j++) {
														LeafExpression expression1 = expressions1.get(j);
														LeafExpression expression2 = expressions2.get(j);
														if(expression1.getString().equals(expression2.getString())) {
															LeafMapping leafMapping = new LeafMapping(expression1, expression2, removed, addedOperation);
															mapper.addMapping(leafMapping);
														}
													}
												}
											}
										}
									}
								}
							}
							if(overallMaxMatchingTestParameters > -1) {
								addedOperationIterator.remove();
							}
						}
						else {
							UMLOperationBodyMapper bestMapper = findBestMapper(mapperSet);
							int mapperSetSize = mapperSet.size();
							if(bestMapper != null) {
								//check for consistent method renames in modelDiff
								for(MethodInvocationReplacement replacement : consistentMethodInvocationRenamesInModel.keySet()) {
									UMLOperationBodyMapper mapper = consistentMethodInvocationRenamesInModel.get(replacement);
									if(replacement.getInvokedOperationBefore().matchesOperation(bestMapper.getContainer1(), mapper.getContainer1(), mapper.getClassDiff(), modelDiff)) {
										for(Iterator<UMLOperation> addedOperationIterator2 = addedOperations.iterator(); addedOperationIterator2.hasNext();) {
											UMLOperation addedOperation2 = addedOperationIterator2.next();
											if(replacement.getInvokedOperationAfter().matchesOperation(addedOperation2, mapper.getContainer2(), mapper.getClassDiff(), modelDiff)) {
												int maxDifferenceInPosition;
												if(bestMapper.getContainer1().hasTestAnnotation() && addedOperation2.hasTestAnnotation()) {
													maxDifferenceInPosition = Math.abs(removedOperations.size() - addedOperations.size());
												}
												else if(bestMapper.getContainer1().hasTestAnnotation() && addedOperation2.hasParameterizedTestAnnotation()) {
													maxDifferenceInPosition = initialNumberOfRemovedOperations + initialNumberOfAddedOperations;
												}
												else {
													maxDifferenceInPosition = Math.max(removedOperations.size(), addedOperations.size());
												}
												updateMapperSet(mapperSet, bestMapper.getOperation1(), addedOperation2, maxDifferenceInPosition);
												break;
											}
										}
										break;
									}
								}
							}
							if(mapperSet.size() > mapperSetSize) {
								bestMapper = findBestMapper(mapperSet);
							}
							if(bestMapper != null && !modelDiffContainsConflictingMoveOperationRefactoring(bestMapper) && !potentialExtractFixture(bestMapper) && !conflictWithExtractMethodCandidate(bestMapper)) {
								UMLOperation removedOperation = bestMapper.getOperation1();
								addedOperation = bestMapper.getOperation2();
								if(mapperSet.size() > mapperSetSize) {
									addedOperationsToBeRemoved.add(addedOperation);
								}
								else {
									addedOperationIterator.remove();
								}
								removedOperations.remove(removedOperation);
								if(!removedOperation.getName().equals(addedOperation.getName()) &&
										!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
									Set<MethodInvocationReplacement> callReferences = getCallReferences(removedOperation, addedOperation);
									RenameOperationRefactoring rename = new RenameOperationRefactoring(bestMapper, callReferences);
									refactorings.add(rename);
								}
								this.addOperationBodyMapper(bestMapper);
								consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
								processNestedTypeDeclarationStatements(removedOperation, addedOperation);
							}
						}
					}
					else {
						Set<VariableDeclarationContainer> removedOperationsToBeRemoved = new LinkedHashSet<>();
						for(CandidateMergeMethodRefactoring candidate : candidateMethodMerges) {
							addedOperationsToBeRemoved.add(candidate.getNewMethodAfterMerge());
							removedOperationsToBeRemoved.addAll(candidate.getMergedMethods());
							MergeOperationRefactoring merge = new MergeOperationRefactoring(candidate.getMergedMethods(), candidate.getNewMethodAfterMerge(), getOriginalClassName(), getNextClassName(), candidate.getMappers());
							refactorings.add(merge);
							for(UMLOperationBodyMapper mapper : merge.getMappers()) {
								mapper.computeRefactoringsWithinBody();
								refactorings.addAll(mapper.getRefactoringsAfterPostProcessing());
							}
						}
						for(CandidateSplitMethodRefactoring candidate : candidateMethodSplits) {
							addedOperationsToBeRemoved.addAll(candidate.getSplitMethods());
							removedOperationsToBeRemoved.add(candidate.getOriginalMethodBeforeSplit());
							SplitOperationRefactoring split = new SplitOperationRefactoring(candidate.getOriginalMethodBeforeSplit(), candidate.getSplitMethods(), getOriginalClassName(), getNextClassName(), candidate.getMappers());
							refactorings.add(split);
							for(UMLOperationBodyMapper mapper : split.getMappers()) {
								mapper.computeRefactoringsWithinBody();
								refactorings.addAll(mapper.getRefactoringsAfterPostProcessing());
							}
						}
						if(candidateMethodMerges.size() > 0 || candidateMethodSplits.size() > 0) {
							addedOperationIterator.remove();
						}
						removedOperations.removeAll(removedOperationsToBeRemoved);
					}
				}
			}
			addedOperations.removeAll(addedOperationsToBeRemoved);
		}
		//infer signature changes for delegate methods calling methods in the operationDiffList
		for(Iterator<UMLOperation> removedOperationIterator = removedOperations.iterator(); removedOperationIterator.hasNext();) {
			UMLOperation removedOperation = removedOperationIterator.next();
			AbstractCall removedOperationInvocation = removedOperation.isDelegate();
			if(removedOperationInvocation != null) {
				for(Iterator<UMLOperation> addedOperationIterator = addedOperations.iterator(); addedOperationIterator.hasNext();) {
					UMLOperation addedOperation = addedOperationIterator.next();
					AbstractCall addedOperationInvocation = addedOperation.isDelegate();
					if(addedOperationInvocation != null) {
						for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
							if(removedOperationInvocation.matchesOperation(mapper.getContainer1(), removedOperation, this, modelDiff) &&
									addedOperationInvocation.matchesOperation(mapper.getContainer2(), addedOperation, this, modelDiff) &&
									removedOperation.getParameterTypeList().equals(addedOperation.getParameterTypeList())) {
								addedOperationIterator.remove();
								removedOperationIterator.remove();

								UMLOperationDiff operationSignatureDiff = new UMLOperationDiff(removedOperation, addedOperation, this);
								refactorings.addAll(operationSignatureDiff.getRefactorings());
								if(!removedOperation.getName().equals(addedOperation.getName()) &&
										!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
									RenameOperationRefactoring rename = new RenameOperationRefactoring(removedOperation, addedOperation);
									refactorings.add(rename);
								}
								break;
							}
						}
					}
				}
			}
		}
	}

	private void processNestedTypeDeclarationStatements(UMLOperation removedOperation, UMLOperation addedOperation) {
		if(modelDiff != null) {
			List<UMLClass> nestedRemovedTypeDeclarations = new ArrayList<>();
			for(UMLClass removedClass : modelDiff.getRemovedClasses()) {
				if(removedOperation.getLocationInfo().subsumes(removedClass.getLocationInfo())) {
					nestedRemovedTypeDeclarations.add(removedClass);
				}
			}
			List<UMLClass> nestedAddedTypeDeclarations = new ArrayList<>();
			for(UMLClass addedClass : modelDiff.getAddedClasses()) {
				if(addedOperation.getLocationInfo().subsumes(addedClass.getLocationInfo())) {
					nestedAddedTypeDeclarations.add(addedClass);
				}
			}
			List<UMLClassMoveDiff> possiblyIncorrect = new ArrayList<>();
			for(UMLClassMoveDiff moveDiff : modelDiff.getClassMoveDiffList()) {
				UMLClass removedClass = moveDiff.getOriginalClass();
				UMLClass addedClass = moveDiff.getNextClass();
				if(removedOperation.getLocationInfo().subsumes(removedClass.getLocationInfo()) &&
						!addedOperation.getLocationInfo().subsumes(addedClass.getLocationInfo())) {
					nestedRemovedTypeDeclarations.add(removedClass);
					possiblyIncorrect.add(moveDiff);
				}
				if(addedOperation.getLocationInfo().subsumes(addedClass.getLocationInfo()) &&
						!removedOperation.getLocationInfo().subsumes(removedClass.getLocationInfo())) {
					nestedAddedTypeDeclarations.add(addedClass);
					possiblyIncorrect.add(moveDiff);
				}
			}
			if(nestedRemovedTypeDeclarations.size() == nestedAddedTypeDeclarations.size()) {
				for(int i=0; i<nestedRemovedTypeDeclarations.size(); i++) {
					UMLClass removedClass = nestedRemovedTypeDeclarations.get(i);
					UMLClass addedClass = nestedAddedTypeDeclarations.get(i);
					if(removedClass.getNonQualifiedName().equals(addedClass.getNonQualifiedName())) {
						MatchResult matchResult = new UMLClassMatcher.Move().match(removedClass, addedClass);
						UMLClassMoveDiff newMoveDiff = new UMLClassMoveDiff(removedClass, addedClass, modelDiff, matchResult);
						for(UMLClassMoveDiff oldMoveDiff : possiblyIncorrect) {
							if(oldMoveDiff.getOriginalClass().equals(removedClass) || oldMoveDiff.getNextClass().equals(addedClass)) {
								modelDiff.getClassMoveDiffList().remove(oldMoveDiff);
							}
						}
						modelDiff.getClassMoveDiffList().add(newMoveDiff);
					}
				}
			}
		}
	}

	private boolean conflictWithExtractMethodCandidate(UMLOperationBodyMapper mapper) {
		for(UMLOperationBodyMapper candidate : extractMethodCandidates) {
			if(candidate.getContainer2().equals(mapper.getContainer2())) {
				return true;
			}
		}
		return false;
	}

	private void betterMatchForAddedOperation(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperationBodyMapper mapper)
			throws RefactoringMinerTimedOutException {
				if(mapper != null && !mapper.getContainer1().getName().toLowerCase().contains(mapper.getContainer2().getName().toLowerCase())) {
					List<String> representation2 = mapper.getContainer2().stringRepresentation();
					for(UMLOperation removedOperation : removedOperations) {
						if(!removedOperation.equals(mapper.getContainer1()) &&
								removedOperation.getName().toLowerCase().contains(mapper.getContainer2().getName().toLowerCase())) {
							List<String> representation1 = removedOperation.stringRepresentation();
							if(representation1.containsAll(representation2)) {
								UMLOperationBodyMapper newMapper = new UMLOperationBodyMapper(removedOperation, mapper.getOperation2(), this);
								mapperSet.add(newMapper);
							}
						}
					}
				}
			}

	private boolean potentialExtractFixture(UMLOperationBodyMapper mapper) {
		if(mapper.getContainer2().hasTearDownAnnotation() || mapper.getContainer2().hasSetUpAnnotation()) {
			if(!mapper.getContainer1().hasTearDownAnnotation() && !mapper.getContainer1().hasSetUpAnnotation()) {
				for(UMLOperationBodyMapper bodyMapper : operationBodyMapperList) {
					for(AbstractCall call : bodyMapper.getContainer1().getAllOperationInvocations()) {
						if(call.matchesOperation(mapper.getContainer1(), bodyMapper.getContainer1(), this, modelDiff)) {
							if(isPartOfMethodInlined(bodyMapper.getContainer1(), bodyMapper.getContainer2())) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean modelDiffContainsConflictingMoveOperationRefactoring(UMLOperationBodyMapper mapper) {
		if(modelDiff != null) {
			return modelDiff.refactoringListContainsAnotherMoveRefactoringWithTheSameRemovedOperation(mapper);
		}
		return false;
	}

	private boolean isPerfectMapper(UMLOperationBodyMapper mapper) {
		int nonMappedLeavesT1 = mapper.getNonMappedLeavesT1().size();
		int nonMappedLeavesT2 = mapper.getNonMappedLeavesT2().size();
		int nonMappedInnerNodesT1 = mapper.getNonMappedInnerNodesT1().size();
		int nonMappedInnerNodesT2 = mapper.getNonMappedInnerNodesT2().size();
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			if(mapping.isIdenticalWithInlinedVariable() || mapping.isIdenticalWithExtractedVariable()) {
				for(Refactoring r : mapping.getRefactorings()) {
					if(r instanceof InlineVariableRefactoring) {
						InlineVariableRefactoring inline = (InlineVariableRefactoring)r;
						for(AbstractCodeFragment fragment : mapper.getNonMappedLeavesT1()) {
							if(fragment.getLocationInfo().subsumes(inline.getVariableDeclaration().getLocationInfo())) {
								nonMappedLeavesT1--;
								break;
							}
						}
					}
					if(r instanceof ExtractVariableRefactoring) {
						ExtractVariableRefactoring extract = (ExtractVariableRefactoring)r;
						for(AbstractCodeFragment fragment : mapper.getNonMappedLeavesT2()) {
							if(fragment.getLocationInfo().subsumes(extract.getVariableDeclaration().getLocationInfo())) {
								nonMappedLeavesT2--;
								break;
							}
						}
					}
				}
			}
		}
		return mapper.getMappings().size() > 0 && nonMappedLeavesT1 <= 0 && nonMappedLeavesT2 <= 0 && nonMappedInnerNodesT1 == 0 && nonMappedInnerNodesT2 == 0;
	}

	public List<List<LeafExpression>> getParameterValuesAsLeafExpressions(UMLOperation addedOperation) {
		List<List<LeafExpression>> parameterValues = new ArrayList<>();
		for(UMLAnnotation annotation : addedOperation.getAnnotations()) {
			try {
				SourceAnnotation sourceAnnotation = generateSourceAnnotation(annotation, addedOperation);
				List<List<LeafExpression>> testParameters = sourceAnnotation.getTestParameterLeafExpressions();
				parameterValues.addAll(testParameters);
			} catch (IllegalArgumentException ignored) {/* Do nothing */}
		}
		return parameterValues;
	}

	public List<List<String>> getParameterValues(UMLOperation addedOperation) {
		List<List<String>> parameterValues = new ArrayList<>();
		for(UMLAnnotation annotation : addedOperation.getAnnotations()) {
			try {
				SourceAnnotation sourceAnnotation = generateSourceAnnotation(annotation, addedOperation);
				List<List<String>> testParameters = sourceAnnotation.getTestParameters();
				parameterValues.addAll(testParameters);
			} catch (IllegalArgumentException ignored) {/* Do nothing */}
		}
		return parameterValues;
	}

	private SourceAnnotation generateSourceAnnotation(UMLAnnotation annotation, UMLOperation addedOperation) {
		UMLAbstractClass inputDeclaration = nextClass;
		if(annotation.getTypeName().equals("EnumSource") && modelDiff != null) {
			String enumClassLiteral = null;
			if (annotation.isMarkerAnnotation()) {
				enumClassLiteral = SourceAnnotation.sanitizeLiteral(getFirstParameterType(addedOperation));
			} else {
				AbstractExpression value = annotation.isSingleMemberAnnotation() ? annotation.getValue() : annotation.getMemberValuePairs().get("value");
				List<LeafExpression> typeLiterals = value.getTypeLiterals();
				if(typeLiterals.size() > 0)
					enumClassLiteral = SourceAnnotation.sanitizeLiteral(typeLiterals.get(0).getString());
			}
			if(enumClassLiteral != null) {
				UMLClass enumClassDeclaration = findEnumDeclaration(modelDiff.getChildModel(), enumClassLiteral);
				if(enumClassDeclaration != null) {
					inputDeclaration = enumClassDeclaration;
				}
			}
		}
		SourceAnnotation sourceAnnotation = SourceAnnotation.create(annotation, addedOperation, inputDeclaration);
		return sourceAnnotation;
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
		if(addedOperation.isGetter()) {
			for(UMLAttribute attr : addedAttributes) {
				if(addedOperation.getName().equals(attr.getName()) || addedOperation.getName().toLowerCase().equals(getPrefix + attr.getName().toLowerCase())) {
					return true;
				}
			}
		}
		else if(addedOperation.isSetter()) {
			for(UMLAttribute attr : addedAttributes) {
				if(addedOperation.getName().toLowerCase().equals(setPrefix + attr.getName().toLowerCase()) || addedOperation.getParameterNameList().get(0).equals(attr.getName())) {
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

	private boolean isCommentedOut(UMLOperation removedOperation) {
		List<UMLComment> nextClassComments = nextClass.getComments();
		for(UMLComment nextClassComment : nextClassComments) {
			String comment = nextClassComment.getFullText();
			boolean commentedOut = false;
			Scanner scanner = new Scanner(comment);
			int openCurlyBrackets = 0;
			int closeCurlyBrackets = 0;
			String methodSignature = null;
			int bodyStartOffset = -1;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				line = line.trim();
				if(line.startsWith("/*")) {
					line = line.substring(2);
				}
				if(line.endsWith("*/")) {
					line = line.substring(0, line.length()-2);
				}
				if(line.startsWith("//")) {
					line = line.substring(2);
				}
				if(line.startsWith("*")) {
					line = line.substring(1);
				}
				line = line.trim();
				if(METHOD_SIGNATURE_PATTERN.matcher(line).matches()) {
					//method signature starts
					methodSignature = line;
					openCurlyBrackets = 0;
					closeCurlyBrackets = 0;
				}
				for(int i=0; i<line.length(); i++) {
					if(line.charAt(i) == '{') {
						if(methodSignature != null && openCurlyBrackets == 0) {
							int indexOfSignature = comment.indexOf(methodSignature);
							String commentSubString = comment.substring(indexOfSignature, comment.length());
							int indexOfOpenCurlyBracket = commentSubString.indexOf(LANG2.OPEN_BLOCK);
							bodyStartOffset = indexOfSignature + indexOfOpenCurlyBracket;
						}
						openCurlyBrackets++;
					}
					else if(line.charAt(i) == '}') {
						closeCurlyBrackets++;
					}
				}
				if(openCurlyBrackets > 0 && openCurlyBrackets == closeCurlyBrackets) {
					//method ends
					if(methodSignature != null) {
						int indexOfSignature = comment.indexOf(methodSignature);
						String commentSubString = comment.substring(indexOfSignature, comment.length());
						int indexOfCloseCurlyBracket = -1;
						int occurrences = 0;
						for(int i=0; i<commentSubString.length(); i++) {
							if(commentSubString.charAt(i) == '}') {
								indexOfCloseCurlyBracket = i;
								occurrences++;
								if(occurrences == closeCurlyBrackets) {
									break;
								}
							}
						}
						int bodyEndOffset = indexOfSignature + indexOfCloseCurlyBracket;
						if(bodyStartOffset >= 0 && comment.charAt(bodyStartOffset) == '{' &&
								bodyEndOffset >= 0 && comment.charAt(bodyEndOffset) == '}') {
							// +1 to include the closing curly bracket
							String methodBody = comment.substring(bodyStartOffset, bodyEndOffset + 1);
							if(PathFileUtils.isJavaFile(nextClassComment.getLocationInfo().getFilePath())) {
								ASTNode methodBodyBlock = JavaFileProcessor.processBlock(methodBody);
								if(methodBodyBlock != null) {
									int methodBodyHashCode = stringify(methodBodyBlock).hashCode();
									if(methodBodyHashCode == removedOperation.getBodyHashCode()) {
										commentedOut = true;
										break;
									}
								}
							}
						}
					}
				}
			}
			scanner.close();
			if(commentedOut) {
				return true;
			}
		}
		return false;
	}

	private Set<MethodInvocationReplacement> getCallReferences(UMLOperation removedOperation, UMLOperation addedOperation) {
		Set<MethodInvocationReplacement> callReferences = new LinkedHashSet<MethodInvocationReplacement>();
		for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
			UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
			if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff) &&
					replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
				callReferences.add(replacement);
			}
		}
		return callReferences;
	}

	private Map<MethodInvocationReplacement, UMLOperationBodyMapper> findConsistentMethodInvocationRenamesInModelDiff() {
		Map<MethodInvocationReplacement, UMLOperationBodyMapper> map = new HashMap<MethodInvocationReplacement, UMLOperationBodyMapper>();
		if(modelDiff != null) {
			Set<MethodInvocationReplacement> allConsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
			Set<MethodInvocationReplacement> allInconsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
			for(UMLClassDiff classDiff : modelDiff.getCommonClassDiffList()) {
				for(UMLOperationBodyMapper bodyMapper : classDiff.getOperationBodyMapperList()) {
					Set<MethodInvocationReplacement> methodInvocationRenames = bodyMapper.getMethodInvocationRenameReplacements();
					for(MethodInvocationReplacement replacement : methodInvocationRenames) {
						map.put(replacement, bodyMapper);
					}
					ConsistentReplacementDetector.updateRenames(allConsistentMethodInvocationRenames, allInconsistentMethodInvocationRenames,
							methodInvocationRenames);
				}
			}
			//allConsistentMethodInvocationRenames.removeAll(allInconsistentMethodInvocationRenames);
			map.keySet().removeAll(allInconsistentMethodInvocationRenames);
		}
		return map;
	}

	private Map<MethodInvocationReplacement, UMLOperationBodyMapper> findConsistentMethodInvocationRenames() {
		Map<MethodInvocationReplacement, UMLOperationBodyMapper> map = new HashMap<MethodInvocationReplacement, UMLOperationBodyMapper>();
		Set<MethodInvocationReplacement> allConsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		Set<MethodInvocationReplacement> allInconsistentMethodInvocationRenames = new LinkedHashSet<MethodInvocationReplacement>();
		for(UMLOperationBodyMapper bodyMapper : operationBodyMapperList) {
			Set<MethodInvocationReplacement> methodInvocationRenames = bodyMapper.getMethodInvocationRenameReplacements();
			for(MethodInvocationReplacement replacement : methodInvocationRenames) {
				map.put(replacement, bodyMapper);
				//check for nested method calls
				AbstractCall invokedOperationBefore = replacement.getInvokedOperationBefore();
				AbstractCall invokedOperationAfter = replacement.getInvokedOperationAfter();
				if(invokedOperationBefore.arguments().size() == invokedOperationAfter.arguments().size() && invokedOperationBefore.arguments().size() > 0) {
					for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
						if(mapping.getFragment1().getLocationInfo().subsumes(invokedOperationBefore.getLocationInfo()) &&
								mapping.getFragment2().getLocationInfo().subsumes(invokedOperationAfter.getLocationInfo())) {
							List<AbstractCall> invocationsBefore = mapping.getFragment1().getMethodInvocations();
							List<AbstractCall> invocationsAfter = mapping.getFragment2().getMethodInvocations();
							for(int i=0; i<invokedOperationBefore.arguments().size(); i++) {
								String argumentBefore = invokedOperationBefore.arguments().get(i);
								String argumentAfter = invokedOperationAfter.arguments().get(i);
								AbstractCall invocationBefore = null;
								for(AbstractCall inv : invocationsBefore) {
									if(inv.actualString().equals(argumentBefore)) {
										invocationBefore = inv;
										break;
									}
								}
								AbstractCall invocationAfter = null;
								for(AbstractCall inv : invocationsAfter) {
									if(inv.actualString().equals(argumentAfter)) {
										invocationAfter = inv;
										break;
									}
								}
								if(invocationBefore != null && invocationAfter != null) {
									MethodInvocationReplacement nested = new MethodInvocationReplacement(argumentBefore, argumentAfter, invocationBefore, invocationAfter, ReplacementType.METHOD_INVOCATION);
									map.put(nested, bodyMapper);
								}
							}
						}
					}
				}
			}
			ConsistentReplacementDetector.updateRenames(allConsistentMethodInvocationRenames, allInconsistentMethodInvocationRenames,
					methodInvocationRenames);
		}
		//allConsistentMethodInvocationRenames.removeAll(allInconsistentMethodInvocationRenames);
		map.keySet().removeAll(allInconsistentMethodInvocationRenames);
		return map;
	}

	private boolean delegatesToAnotherRemovedOperation(UMLOperation removedOperation) {
		for(UMLOperation removedOperation2 : removedOperations) {
			if(!removedOperation.equals(removedOperation2)) {
				AbstractCall call = removedOperation.delegatesTo(removedOperation2, this, modelDiff);
				if(call != null) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean literalIntersectionOrFormatting(List<LeafExpression> literals1, List<LeafExpression> literals2) {
		Set<String> strippedLiterals1 = StringBasedHeuristics.convertToStringSet(literals1);
		Set<String> intersection = new LinkedHashSet<>(strippedLiterals1);
		Set<String> strippedLiterals2 = StringBasedHeuristics.convertToStringSet(literals2);
		intersection.retainAll(strippedLiterals2);
		if(literals1.size() == literals2.size() && literals1.size() > 0 && intersection.size() == literals1.size()) {
			return true;
		}
		else {
			StringBuilder sb1 = new StringBuilder();
			for(LeafExpression expression : literals1) {
				String withoutQuotes = expression.getString();
				if(withoutQuotes.startsWith("\""))
					withoutQuotes = withoutQuotes.substring(1);
				if(withoutQuotes.endsWith("\""))
					withoutQuotes = withoutQuotes.substring(0, withoutQuotes.length()-1);
				sb1.append(withoutQuotes);
			}
			StringBuilder sb2 = new StringBuilder();
			for(LeafExpression expression : literals2) {
				String withoutQuotes = expression.getString();
				if(withoutQuotes.startsWith("\""))
					withoutQuotes = withoutQuotes.substring(1);
				if(withoutQuotes.endsWith("\""))
					withoutQuotes = withoutQuotes.substring(0, withoutQuotes.length()-1);
				sb2.append(withoutQuotes);
			}
			if(sb1.toString().equals(sb2.toString())) {
				return true;
			}
		}
		return false;
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation addedOperation, int differenceInPosition) throws RefactoringMinerTimedOutException {
				boolean mapperWithZeroNonMappedStatementsOrIdenticalMethodName = false;
				for(UMLOperationBodyMapper mapper : mapperSet) {
					if(mapper.getContainer1().getBody() != null && mapper.getContainer2().getBody() != null &&
							mapper.getContainer1().getBodyHashCode() == mapper.getContainer2().getBodyHashCode()) {
						return;
					}
					if(mapper.getContainer1().getName().equals(mapper.getContainer2().getName())) {
						mapperWithZeroNonMappedStatementsOrIdenticalMethodName = true;
					}
					int sum = mapper.getNonMappedLeavesT1().size() + mapper.getNonMappedLeavesT2().size() + mapper.getNonMappedInnerNodesT1().size() + mapper.getNonMappedInnerNodesT2().size();
					if(sum == 0) {
						mapperWithZeroNonMappedStatementsOrIdenticalMethodName = true;
					}
				}
				UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
				List<AbstractCodeMapping> totalMappings = new ArrayList<AbstractCodeMapping>(operationBodyMapper.getMappings());
				for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
					Set<Replacement> replacements = mapping.getReplacements();
					for(UMLAttribute attribute : originalClass.getAttributes()) {
						if(attribute.getVariableDeclaration().getInitializer() != null) {
							for(Replacement r : replacements) {
								if(r.getBefore().equals(attribute.getName()) && attribute.getVariableDeclaration().getInitializer().getString().contains(r.getAfter())) {
									mapping.setIdenticalWithInlinedVariable(true);
									break;
								}
							}
						}
					}
				}
				int mappings = operationBodyMapper.mappingsWithoutBlocks();
				if(mappings > 0 || (delegatesToAnotherRemovedOperation(removedOperation) && addedOperation.getBody() != null && (addedOperation.stringRepresentation().size() > 3 || addedOperation.getAllLambdas().size() > 0)) || (removedOperation.getName().equals(addedOperation.getName()) && removedOperation.getBody() != null && addedOperation.getBody() != null) ||
						removedOperation.equalSignatureForAbstractMethods(addedOperation)) {
					boolean zeroNonMapped = operationBodyMapper.getNonMappedLeavesT1().size() == 0 && operationBodyMapper.getNonMappedLeavesT2().size() == 0 &&
							operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.getNonMappedInnerNodesT2().size() == 0 &&
							removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation();
					boolean containsAnonymousClassDiff = operationBodyMapper.getAnonymousClassDiffs().size() > 0 &&
							!removedOperation.hasTestAnnotation() && !addedOperation.hasTestAnnotation();
					int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
					List<LeafExpression> literals1 = removedOperation.getAllStringLiterals();
					List<LeafExpression> literals2 = addedOperation.getAllStringLiterals();
					if(exactMappings(operationBodyMapper) || (operationBodyMapper.allMappingsHaveSameDepthAndIndex() && !removedOperation.hasTestAnnotation() && !addedOperation.hasTestAnnotation())) {
						mapperSet.add(operationBodyMapper);
					}
					else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
							(relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) || zeroNonMapped || containsAnonymousClassDiff || operationsBeforeOrAfterMatch(removedOperation, addedOperation)) &&
							(compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition) || operationsBeforeAndAfterMatch(removedOperation, addedOperation)) &&
							removedOperation.testMethodCheck(addedOperation)) {
						isPartOfMethodMovedFromDeletedMethod(removedOperation, addedOperation, operationBodyMapper, mapperSet);
						isPartOfMethodMovedToAddedMethod(removedOperation, addedOperation, operationBodyMapper);
						mapperSet.add(operationBodyMapper);
					}
					else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
							mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
							relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) &&
							(isPartOfMethodExtracted(removedOperation, addedOperation) || isPartOfMethodMovedToExistingMethod(removedOperation, addedOperation) ||
									(operationBodyMapper.exactMatches() > 0 && !mapperWithZeroNonMappedStatementsOrIdenticalMethodName && isPartOfMethodMovedToAddedMethod(removedOperation, addedOperation, operationBodyMapper))) &&
							removedOperation.testMethodCheck(addedOperation)) {
						mapperSet.add(operationBodyMapper);
					}
					else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
							mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
							relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) &&
							(isPartOfMethodInlined(removedOperation, addedOperation) || isPartOfMethodMovedFromExistingMethod(removedOperation, addedOperation) ||
									(operationBodyMapper.exactMatches() > 0 && !mapperWithZeroNonMappedStatementsOrIdenticalMethodName && isPartOfMethodMovedFromDeletedMethod(removedOperation, addedOperation, operationBodyMapper, mapperSet))) &&
							removedOperation.testMethodCheck(addedOperation)) {
						mapperSet.add(operationBodyMapper);
					}
					else if(absoluteDifferenceInPosition == 0 && !removedOperation.isConstructor() && !addedOperation.isConstructor() &&
							removedOperation.getName().equals(addedOperation.getName()) && operationBodyMapper.exactMatches() > 0) {
						mapperSet.add(operationBodyMapper);
					}
					else if(mappings == 0 && removedOperation.hasTestAnnotation() && addedOperation.hasParameterizedTestAnnotation() && literalIntersectionOrFormatting(literals1, literals2)) {
						Set<AbstractCodeFragment> nonMappedLeavesT1ToBeRemoved = new LinkedHashSet<>();
						Set<AbstractCodeFragment> nonMappedLeavesT2ToBeRemoved = new LinkedHashSet<>();
						for(AbstractCodeFragment fragment1 : operationBodyMapper.getNonMappedLeavesT1()) {
							List<LeafExpression> stringLiterals1 = fragment1.getStringLiterals();
							Set<String> lit1 = StringBasedHeuristics.convertToStringSet(stringLiterals1);
							for(AbstractCodeFragment fragment2 : operationBodyMapper.getNonMappedLeavesT2()) {
								List<LeafExpression> stringLiterals2 = fragment2.getStringLiterals();
								Set<String> lit2 = StringBasedHeuristics.convertToStringSet(stringLiterals2);
								Set<String> intersection = new LinkedHashSet<>(lit1);
								intersection.retainAll(lit2);
								if(intersection.size() > 0) {
									LeafMapping mapping = new LeafMapping(fragment1, fragment2,
											operationBodyMapper.getContainer1(),
											operationBodyMapper.getContainer2());
									operationBodyMapper.addMapping(mapping);
									nonMappedLeavesT1ToBeRemoved.add(fragment1);
									nonMappedLeavesT2ToBeRemoved.add(fragment2);
									if(stringLiterals1.size() > stringLiterals2.size()) {
										for(int i=0; i<stringLiterals2.size(); i++) {
											List<LeafExpression> leafExpressions1 = fragment1.findExpression(stringLiterals2.get(i).getString());
											if(leafExpressions1.size() == 1) {
												LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(0), stringLiterals2.get(i),
														operationBodyMapper.getContainer1(),
														operationBodyMapper.getContainer2());
												operationBodyMapper.addMapping(leafMapping);
											}
											else if(leafExpressions1.size() == 0) {
												String stripped2 = stringLiterals2.get(i).getString();
												if(stripped2.startsWith("\""))
													stripped2 = stripped2.substring(1);
												if(stripped2.endsWith("\""))
													stripped2 = stripped2.substring(0, stripped2.length()-1);
												stripped2 = stripped2.strip();
												for(int j=0; j<stringLiterals1.size(); j++) {
													String stripped1 = stringLiterals1.get(j).getString();
													if(stripped1.startsWith("\""))
														stripped1 = stripped1.substring(1);
													if(stripped1.endsWith("\""))
														stripped1 = stripped1.substring(0, stripped1.length()-1);
													stripped1 = stripped1.strip();
													if(stripped1.equals(stripped2)) {
														LeafMapping leafMapping = new LeafMapping(stringLiterals1.get(j), stringLiterals2.get(i),
																operationBodyMapper.getContainer1(),
																operationBodyMapper.getContainer2());
														operationBodyMapper.addMapping(leafMapping);
													}
												}
											}
										}
									}
								}
								else if(intersection.size() == 0) {
									StringBuilder sb1 = new StringBuilder();
									for(LeafExpression expression : stringLiterals1) {
										boolean alreadyMapped = false;
										for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
											if(mapping.getFragment1().equals(expression)) {
												alreadyMapped = true;
												break;
											}
										}
										if(!alreadyMapped) {
											String withoutQuotes = expression.getString();
											if(withoutQuotes.startsWith("\""))
												withoutQuotes = withoutQuotes.substring(1);
											if(withoutQuotes.endsWith("\""))
												withoutQuotes = withoutQuotes.substring(0, withoutQuotes.length()-1);
											sb1.append(withoutQuotes);
										}
									}
									StringBuilder sb2 = new StringBuilder();
									for(LeafExpression expression : stringLiterals2) {
										String withoutQuotes = expression.getString();
										if(withoutQuotes.startsWith("\""))
											withoutQuotes = withoutQuotes.substring(1);
										if(withoutQuotes.endsWith("\""))
											withoutQuotes = withoutQuotes.substring(0, withoutQuotes.length()-1);
										sb2.append(withoutQuotes);
									}
									if(sb1.toString().equals(sb2.toString())) {
										//make all combinations, formatting change
										for(LeafExpression expression1 : stringLiterals1) {
											boolean alreadyMapped = false;
											for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
												if(mapping.getFragment1().equals(expression1)) {
													alreadyMapped = true;
													break;
												}
											}
											if(!alreadyMapped) {
												for(LeafExpression expression2 : stringLiterals2) {
													LeafMapping leafMapping = new LeafMapping(expression1, expression2,
															operationBodyMapper.getContainer1(),
															operationBodyMapper.getContainer2());
													operationBodyMapper.addMapping(leafMapping);
												}
											}
										}
									}
								}
							}
						}
						if(operationBodyMapper.getMappings().size() > 0) {
							operationBodyMapper.getNonMappedLeavesT1().removeAll(nonMappedLeavesT1ToBeRemoved);
							operationBodyMapper.getNonMappedLeavesT2().removeAll(nonMappedLeavesT2ToBeRemoved);
							mapperSet.add(operationBodyMapper);
						}
					}
					// in Python only one constructor is allowed
					else if((LANG1.equals(Constants.PYTHON) || LANG2.equals(Constants.PYTHON)) && removedOperation.isConstructor() && addedOperation.isConstructor()) {
						mapperSet.add(operationBodyMapper);
					}
					else {
						String originalClassName = getOriginalClassName();
						String nextClassName = getNextClassName();
						if(modelDiff != null && originalClassName.contains(".") && nextClassName.contains(".")) {
							UMLAbstractClass originalOuterClass = modelDiff.findClassInParentModel(getOriginalClass().getSourceFolder(), originalClassName.substring(0, originalClassName.lastIndexOf(".")));
							UMLAbstractClass nextOuterClass = modelDiff.findClassInChildModel(getNextClass().getSourceFolder(), nextClassName.substring(0, nextClassName.lastIndexOf(".")));
							if(originalOuterClass != null && nextOuterClass != null && !originalOuterClass.equals(this.originalClass) && !nextOuterClass.equals(this.nextClass)) {
								UMLOperation op1 = originalOuterClass.operationWithTheSameSignature(removedOperation);
								UMLOperation op2 = nextOuterClass.operationWithTheSameSignature(addedOperation);
								if(op1 != null && op2 != null) {
									mapperSet.add(operationBodyMapper);
								}
							}
						}
						if((removedOperation.hasSetUpAnnotation() || removedOperation.getName().equals("setUp")) && (addedOperation.hasSetUpAnnotation() || addedOperation.getName().equals("setUp"))) {
							potentialCodeMoveBetweenSetUpTearDownMethods.add(operationBodyMapper);
						}
						else if((removedOperation.hasTearDownAnnotation() || removedOperation.getName().equals("tearDown")) && (addedOperation.hasTearDownAnnotation() || addedOperation.getName().equals("tearDown"))) {
							potentialCodeMoveBetweenSetUpTearDownMethods.add(operationBodyMapper);
						}
						else if(allStatementsMappedOrParameterized(operationBodyMapper)) {
							movedMethodsInDifferentPositionWithinFile.add(operationBodyMapper);
						}
						boolean found = false;
						for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
							UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
							if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff) &&
									replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
								mapperSet.add(operationBodyMapper);
								found = true;
								break;
							}
						}
						if(!found) {
							for(MethodInvocationReplacement replacement : consistentMethodInvocationRenamesInModel.keySet()) {
								UMLOperationBodyMapper mapper = consistentMethodInvocationRenamesInModel.get(replacement);
								if(mapper.nonMappedElementsT1() <= 1 && mapper.nonMappedElementsT2() <= 1 &&
										replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff) &&
										replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
									boolean skip = false;
									if(mapper.getOperation1().equalSignature(operationBodyMapper.getOperation1()) && mapper.getOperation2().equalSignature(operationBodyMapper.getOperation2())) {
										skip = true;
									}
									if(!skip) {
										mapperSet.add(operationBodyMapper);
										break;
									}
								}
							}
						}
					}
				}
				else {
					boolean found = false;
					for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
						UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
						if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff) &&
								replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
							mapperSet.add(operationBodyMapper);
							found = true;
							break;
						}
					}
					if(!found) {
						for(MethodInvocationReplacement replacement : consistentMethodInvocationRenamesInModel.keySet()) {
							UMLOperationBodyMapper mapper = consistentMethodInvocationRenamesInModel.get(replacement);
							if(mapper.nonMappedElementsT1() == 0 && mapper.nonMappedElementsT2() == 0 &&
									replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff) &&
									replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
								mapperSet.add(operationBodyMapper);
								break;
							}
						}
					}
					if(matchingGetterSetterWithSameRenamePattern(removedOperation, addedOperation) && computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation) <= differenceInPosition) {
						mapperSet.add(operationBodyMapper);
					}
					for(UMLOperationBodyMapper m : movedMethodsInDifferentPositionWithinFile) {
						int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(List.of(m.getContainer2()));
						if(nonMappedElementsT2CallingAddedOperation > 0) {
							mapperSet.add(operationBodyMapper);
						}
					}
					for(UMLOperation operation : addedOperations) {
						int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(List.of(operation));
						boolean nameMatch = operationBodyMapper.getContainer1().getName().contains(operationBodyMapper.getContainer2().getName()) ||
								operationBodyMapper.getContainer2().getName().contains(operationBodyMapper.getContainer1().getName());
						if(nameMatch && operationBodyMapper.nonMappedElementsT2() == 1 && nonMappedElementsT2CallingAddedOperation == 1 &&
								operationBodyMapper.getContainer2().stringRepresentation().size() == 3) {
							boolean callToNewClass = false;
							List<AbstractCodeFragment> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
							if(nonMappedLeavesT2.size() > 0) {
								AbstractCodeFragment nonMappedLeafT2 = nonMappedLeavesT2.get(0);
								AbstractCall call = nonMappedLeafT2.invocationCoveringEntireFragment();
								if(call == null) {
									call = nonMappedLeafT2.assignmentInvocationCoveringEntireStatement();
								}
								if(call != null && call.getExpression() != null) {
									String expression = call.getExpression();
									for(UMLAttribute attribute : nextClass.getAttributes()) {
										if(attribute.getName().equals(expression) && !originalClass.containsAttributeWithName(expression)) {
											if(modelDiff != null && (modelDiff.findClassInChildModel(attribute.getType().getClassType()) != null || modelDiff.partialModel())) {
												callToNewClass = true;
												break;
											}
										}
									}
								}
							}
							if(!callToNewClass) {
								mapperSet.add(operationBodyMapper);
							}
						}
					}
				}
				if(totalMappings.size() > 0) {
					int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
					if(singleUnmatchedStatementCallsAddedOperation(operationBodyMapper) &&
							relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) &&
							compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
						mapperSet.add(operationBodyMapper);
					}
				}
				if (mappings == 0 && isPartOfMethodExtracted(removedOperation, addedOperation) && removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation()) {
					int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
					if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
							mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
							relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) &&
							removedOperation.testMethodCheck(addedOperation)) {
						mapperSet.add(operationBodyMapper);
					}
				}
			}

	private boolean relativePositionCheck(int differenceInPosition, int absoluteDifferenceInPosition) {
		return absoluteDifferenceInPosition <= differenceInPosition || (removedOperations.size() == addedOperations.size() && removedOperations.size() == 1 && !originalClass.isTestClass() && !nextClass.isTestClass()) ||
				(removedOperations.size() - removedOperationDelegates == addedOperations.size() && removedOperations.size() - removedOperationDelegates == 1 && !originalClass.isTestClass() && !nextClass.isTestClass());
	}

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation operationInsideAnonymousClass, UMLOperation addedOperation, int differenceInPosition) throws RefactoringMinerTimedOutException {
				UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, operationInsideAnonymousClass, this);
				int mappings = operationBodyMapper.mappingsWithoutBlocks();
				if(mappings > 0) {
					int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
					if(exactMappings(operationBodyMapper)) {
						mapperSet.add(operationBodyMapper);
					}
					else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
							relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) &&
							compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
						mapperSet.add(operationBodyMapper);
					}
					else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
							mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
							relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) &&
							(isPartOfMethodExtracted(removedOperation, addedOperation) || isPartOfMethodMovedToExistingMethod(removedOperation, addedOperation))) {
						mapperSet.add(operationBodyMapper);
					}
					else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
							mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
							relativePositionCheck(differenceInPosition, absoluteDifferenceInPosition) &&
							(isPartOfMethodInlined(removedOperation, addedOperation) || isPartOfMethodMovedFromExistingMethod(removedOperation, addedOperation))) {
						mapperSet.add(operationBodyMapper);
					}
					else {
						for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
							UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
							if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff) &&
									replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
								mapperSet.add(operationBodyMapper);
								break;
							}
						}
					}
				}
			}

	private boolean allStatementsMappedOrParameterized(UMLOperationBodyMapper operationBodyMapper) {
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0 && operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0) {
			return true;
		}
		int nonMappedStatements1ThatCanBeIgnored = 0;
		int nonMappedStatements1 = 0;
		for(AbstractCodeFragment fragment1 : operationBodyMapper.getNonMappedLeavesT1()) {
			if(fragment1.countableStatement()) {
				for(String parameterName : operationBodyMapper.getParameterNameList2()) {
					if(fragment1.getVariableDeclaration(parameterName) != null) {
						nonMappedStatements1ThatCanBeIgnored++;
						break;
					}
				}
				nonMappedStatements1++;
			}
		}
		int nonMappedStatements2ThatCanBeIgnored = 0;
		int nonMappedStatements2 = 0;
		for(CompositeStatementObject composite2 : operationBodyMapper.getNonMappedInnerNodesT2()) {
			if(composite2.countableStatement()) {
				for(String parameterName : operationBodyMapper.getParameterNameList2()) {
					boolean matchFound = false;
					for(LeafExpression leafExpression : composite2.getVariables()) {
						if(leafExpression.getString().equals(parameterName)) {
							nonMappedStatements2ThatCanBeIgnored++;
							matchFound = true;
							break;
						}
					}
					if(matchFound) {
						break;
					}
				}
				nonMappedStatements2++;
			}
		}
		for(AbstractCodeFragment fragment2 : operationBodyMapper.getNonMappedLeavesT2()) {
			if(fragment2.countableStatement()) {
				for(String parameterName : operationBodyMapper.getParameterNameList2()) {
					boolean matchFound = false;
					for(LeafExpression leafExpression : fragment2.getVariables()) {
						if(leafExpression.getString().equals(parameterName)) {
							nonMappedStatements2ThatCanBeIgnored++;
							matchFound = true;
							break;
						}
					}
					if(matchFound) {
						break;
					}
				}
				nonMappedStatements2++;
			}
		}
		if(operationBodyMapper.exactMatches() > 0 && nonMappedStatements1 == nonMappedStatements1ThatCanBeIgnored && nonMappedStatements2 == nonMappedStatements2ThatCanBeIgnored) {
			return true;
		}
		return false;
	}

	private boolean matchingGetterSetterWithSameRenamePattern(UMLOperation removedOperation, UMLOperation addedOperation) {
		String setPrefix = "set";
		String getPrefix = "get";
		if(removedOperation.getName().startsWith(setPrefix) && addedOperation.getName().startsWith(setPrefix)) {
			String removedOperationSuffix = removedOperation.getName().substring(setPrefix.length());
			String addedOperationSuffix = addedOperation.getName().substring(setPrefix.length());
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				if(mapper.getContainer1().getName().startsWith(getPrefix) && mapper.getContainer2().getName().startsWith(getPrefix)) {
					String container1Suffix = mapper.getContainer1().getName().substring(getPrefix.length());
					String container2Suffix = mapper.getContainer2().getName().substring(getPrefix.length());
					if(container1Suffix.equals(removedOperationSuffix) && container2Suffix.equals(addedOperationSuffix)) {
						return true;
					}
				}
			}
		}
		else if(removedOperation.getName().startsWith(getPrefix) && addedOperation.getName().startsWith(getPrefix)) {
			String removedOperationSuffix = removedOperation.getName().substring(getPrefix.length());
			String addedOperationSuffix = addedOperation.getName().substring(getPrefix.length());
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				if(mapper.getContainer1().getName().startsWith(setPrefix) && mapper.getContainer2().getName().startsWith(setPrefix)) {
					String container1Suffix = mapper.getContainer1().getName().substring(setPrefix.length());
					String container2Suffix = mapper.getContainer2().getName().substring(setPrefix.length());
					if(container1Suffix.equals(removedOperationSuffix) && container2Suffix.equals(addedOperationSuffix)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean exactMappings(UMLOperationBodyMapper operationBodyMapper) {
		if(operationBodyMapper.allMappingsAreExactMatches()) {
			if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() == 0)
				return true;
			else if(operationBodyMapper.nonMappedElementsT1() > 0 && operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.nonMappedElementsT2() == 0) {
				int countableStatements = 0;
				int parameterizedVariableDeclarationStatements = 0;
				VariableDeclarationContainer addedOperation = operationBodyMapper.getContainer2();
				List<String> nonMappedLeavesT1 = new ArrayList<String>();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT1()) {
					if(statement.countableStatement()) {
						nonMappedLeavesT1.add(statement.getString());
						for(String parameterName : addedOperation.getParameterNameList()) {
							if(statement.getVariableDeclaration(parameterName) != null) {
								parameterizedVariableDeclarationStatements++;
								break;
							}
						}
						countableStatements++;
					}
				}
				int nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation = 0;
				for(UMLOperation operation : addedOperations) {
					if(!operation.equals(addedOperation) && operation.getBody() != null) {
						for(AbstractCodeFragment statement : operation.getBody().getCompositeStatement().getLeaves()) {
							if(nonMappedLeavesT1.contains(statement.getString())) {
								nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation++;
							}
						}
					}
				}
				return (countableStatements == parameterizedVariableDeclarationStatements || countableStatements == nonMappedLeavesExactlyMatchedInTheBodyOfAddedOperation + parameterizedVariableDeclarationStatements) && countableStatements > 0;
			}
			else if(operationBodyMapper.nonMappedElementsT1() == 0 && operationBodyMapper.nonMappedElementsT2() > 0 && operationBodyMapper.getNonMappedInnerNodesT2().size() == 0) {
				int countableStatements = 0;
				int parameterizedVariableDeclarationStatements = 0;
				VariableDeclarationContainer removedOperation = operationBodyMapper.getContainer1();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT2()) {
					if(statement.countableStatement()) {
						for(String parameterName : removedOperation.getParameterNameList()) {
							if(statement.getVariableDeclaration(parameterName) != null) {
								parameterizedVariableDeclarationStatements++;
								break;
							}
						}
						countableStatements++;
					}
				}
				return countableStatements == parameterizedVariableDeclarationStatements && countableStatements > 0;
			}
			else if((operationBodyMapper.nonMappedElementsT1() == 1 || operationBodyMapper.nonMappedElementsT2() == 1) &&
					operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.getNonMappedInnerNodesT2().size() == 0) {
				AbstractCodeFragment statementUsingParameterAsInvoker1 = null;
				VariableDeclarationContainer removedOperation = operationBodyMapper.getContainer1();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT1()) {
					if(statement.countableStatement()) {
						for(String parameterName : removedOperation.getParameterNameList()) {
							AbstractCall invocation = statement.invocationCoveringEntireFragment();
							if(invocation != null && invocation.getExpression() != null && invocation.getExpression().equals(parameterName)) {
								statementUsingParameterAsInvoker1 = statement;
								break;
							}
						}
					}
				}
				AbstractCodeFragment statementUsingParameterAsInvoker2 = null;
				VariableDeclarationContainer addedOperation = operationBodyMapper.getContainer2();
				for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT2()) {
					if(statement.countableStatement()) {
						for(String parameterName : addedOperation.getParameterNameList()) {
							AbstractCall invocation = statement.invocationCoveringEntireFragment();
							if(invocation != null && invocation.getExpression() != null && invocation.getExpression().equals(parameterName)) {
								statementUsingParameterAsInvoker2 = statement;
								break;
							}
						}
					}
				}
				if(statementUsingParameterAsInvoker1 != null && statementUsingParameterAsInvoker2 != null) {
					for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
						if(mapping.getFragment1() instanceof CompositeStatementObject && mapping.getFragment2() instanceof CompositeStatementObject) {
							CompositeStatementObject parent1 = (CompositeStatementObject)mapping.getFragment1();
							CompositeStatementObject parent2 = (CompositeStatementObject)mapping.getFragment2();
							if(parent1.getLeaves().contains(statementUsingParameterAsInvoker1) && parent2.getLeaves().contains(statementUsingParameterAsInvoker2)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean mappedElementsMoreThanNonMappedT1AndT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
				List<CompositeReplacement> composites = operationBodyMapper.getCompositeReplacements();
				int additionallyMatchedStatements1 = 0;
				int additionallyMatchedStatements2 = 0;
				for(CompositeReplacement composite : composites) {
					additionallyMatchedStatements1 += composite.getAdditionallyMatchedStatements1().size();
					additionallyMatchedStatements2 += composite.getAdditionallyMatchedStatements2().size();
				}
				mappings += additionallyMatchedStatements1 + additionallyMatchedStatements2;
				int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1() - additionallyMatchedStatements1;
				int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2() - additionallyMatchedStatements2;
				int exactMappings = operationBodyMapper.exactMatches();
				if(operationBodyMapper.getContainer1() instanceof UMLOperation && operationBodyMapper.getContainer2() instanceof UMLOperation) {
					UMLParameter returnParameter1 = ((UMLOperation)operationBodyMapper.getContainer1()).getReturnParameter();
					UMLParameter returnParameter2 = ((UMLOperation)operationBodyMapper.getContainer2()).getReturnParameter();
					if(returnParameter1 != null && returnParameter2 != null) {
						UMLType returnType1 = returnParameter1.getType();
						UMLType returnType2 = returnParameter2.getType();
						boolean returnFound = false;
						if(returnType1.getClassType().equals("void") && !returnType2.getClassType().equals("void")) {
							for(AbstractCodeFragment fragment1 : operationBodyMapper.getNonMappedLeavesT1()) {
								if(fragment1.getString().equals(LANG1.RETURN_STATEMENT)) {
									returnFound = true;
									break;
								}
							}
						}
						if(!returnFound) {
							for(AbstractCodeFragment statement2 : operationBodyMapper.getNonMappedLeavesT2()) {
								if(statement2.getVariables().size() > 0 && statement2.getString().equals(LANG2.RETURN_SPACE + statement2.getVariables().get(0).getString() + LANG2.STATEMENT_TERMINATION)) {
									VariableDeclaration variableDeclaration2 = operationBodyMapper.getContainer2().getVariableDeclaration(statement2.getVariables().get(0).getString());
									if(variableDeclaration2 != null && variableDeclaration2.getType() != null && variableDeclaration2.getType().equals(returnType2)) {
										nonMappedElementsT2--;
									}
								}
							}
						}
					}
				}
				boolean operationsWithSameName = operationBodyMapper.getContainer1().getName().equals(operationBodyMapper.getContainer2().getName()) &&
						!operationBodyMapper.getContainer1().isConstructor() && !operationBodyMapper.getContainer2().isConstructor();
				int inExactWhenEntries = 0;
				for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
					if((mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.WHEN_ENTRY) ||
							mapping.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.WHEN_STATEMENT)) &&
							!mapping.getFragment1().getString().replaceAll("\s", "").equals(mapping.getFragment2().getString().replaceAll("\s", ""))) {
						inExactWhenEntries++;
					}
					if(mapping.isIdenticalWithInlinedVariable() || mapping.isIdenticalWithExtractedVariable()) {
						for(Refactoring r : mapping.getRefactorings()) {
							if(r instanceof InlineVariableRefactoring) {
								nonMappedElementsT1--;
								if(operationsWithSameName)
									mappings++;
							}
							else if(r instanceof ExtractVariableRefactoring) {
								nonMappedElementsT2--;
								if(operationsWithSameName)
									mappings++;
							}
						}
					}
				}
				if(inExactWhenEntries > 0 && inExactWhenEntries == operationBodyMapper.getMappings().size()) {
					return false;
				}
				boolean identicalFixtureAnnotation = operationBodyMapper.getContainer1().identicalTextFixture(operationBodyMapper.getContainer2());
				boolean migrateToExpected = false;
				if(!operationBodyMapper.getContainer1().hasTestAnnotation() && operationBodyMapper.getContainer2().hasTestAnnotation()) {
					AbstractCodeFragment expectedException = null;
					for(UMLAnnotation annotation : operationBodyMapper.getContainer2().getAnnotations()) {
						if(annotation.getTypeName().equals("Test")) {
							for(String key : annotation.getMemberValuePairs().keySet()) {
								if(key.equals("expected")) {
									expectedException = annotation.getMemberValuePairs().get(key);
								}
							}
						}
					}
					if(expectedException == null) {
						for(AbstractCodeFragment fragment2 : operationBodyMapper.getNonMappedLeavesT2()) {
							AbstractCall call = fragment2.invocationCoveringEntireFragment();
							if(call != null && call.getName().equals("expect") && call.arguments().size() == 1) {
								List<LeafExpression> leafExpressions = fragment2.findExpression(call.arguments().get(0));
								if(leafExpressions.size() == 1) {
									expectedException = leafExpressions.get(0);
								}
							}
						}
					}
					if(expectedException != null) {
						for(CompositeStatementObject composite1 : operationBodyMapper.getNonMappedInnerNodesT1()) {
							if(composite1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
								for(VariableDeclaration declaration : composite1.getVariableDeclarations()) {
									if(declaration.getType() != null && expectedException.getString().equals(declaration.getType() + ".class")) {
										migrateToExpected = true;
									}
								}
							}
						}
					}
				}
				return operationBodyMapper.getMappings().size() > 100 ||
						(mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
						(mappings > 0 && identicalFixtureAnnotation) ||
						(mappings > 0 && migrateToExpected) ||
						((operationsWithSameName || mappings > 10) && mappings >= nonMappedElementsT1 && mappings >= nonMappedElementsT2) ||
						(nonMappedElementsT1 == 0 && mappings > Math.floor(nonMappedElementsT2/2.0) && (!operationBodyMapper.involvesTestMethods() || removedOperations.size() == 1 || addedOperations.size() == 1)) ||
						(nonMappedElementsT2 == 0 && mappings > Math.floor(nonMappedElementsT1/2.0) && (!operationBodyMapper.involvesTestMethods() || removedOperations.size() == 1 || addedOperations.size() == 1) && !(this instanceof UMLClassMoveDiff)) ||
						(nonMappedElementsT1 == 0 && exactMappings >= Math.floor(nonMappedElementsT2/2.0) && operationBodyMapper.getContainer1().isConstructor() == operationBodyMapper.getContainer2().isConstructor()) ||
						(nonMappedElementsT1 <= 1 && exactMappings > 1 && exactMappings >= Math.floor(nonMappedElementsT2/2.0)) ||
						(nonMappedElementsT2 <= 1 && exactMappings > 1 && exactMappings >= Math.floor(nonMappedElementsT1/2.0)) ||
						(nonMappedElementsT2 == 0 && exactMappings > 0 && exactMappings >= Math.floor(nonMappedElementsT1/2.0) && operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.getContainer1().isConstructor() == operationBodyMapper.getContainer2().isConstructor()) ||
						(nonMappedElementsT2 == 0 && exactMappings > 0 && operationBodyMapper.getParameterNameList1().size() > 0 &&
							operationBodyMapper.getParameterNameList1().equals(operationBodyMapper.getParameterNameList2()) &&
							operationBodyMapper.getContainer1().getParameterTypeList().equals(operationBodyMapper.getContainer2().getParameterTypeList()) &&
							operationBodyMapper.getContainer1().isConstructor() == operationBodyMapper.getContainer2().isConstructor()) ||
						(mappings == 1 && nonMappedElementsT1 + nonMappedElementsT2 == 1 && operationBodyMapper.getContainer1().getName().equals(operationBodyMapper.getContainer2().getName()));
			}

	private boolean mappedElementsMoreThanNonMappedT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(addedOperations);
		List<VariableDeclarationContainer> addedOperationsCalledByMappedElementsT2 = operationBodyMapper.addedOperationsCalledByMappedElementsT2(addedOperations);
		int mappedElementsT2CallingAddedOperation = addedOperationsCalledByMappedElementsT2.size();
		int nonMappedElementsT2WithoutThoseCallingAddedOperation = nonMappedElementsT2 - nonMappedElementsT2CallingAddedOperation;
		boolean matchFound = false;
		for(AbstractCodeFragment nonMappedLeafT1 : operationBodyMapper.getNonMappedLeavesT1()) {
			if(nonMappedLeafT1.getVariableDeclarations().size() > 0) {
				for(AbstractCodeFragment nonMappedLeafT2 : operationBodyMapper.getNonMappedLeavesT2()) {
					if(nonMappedLeafT1.getVariableDeclarations().toString().equals(nonMappedLeafT2.getVariableDeclarations().toString())) {
						mappings++;
						nonMappedElementsT2--;
						matchFound = true;
						break;
					}
				}
				if(matchFound) {
					break;
				}
			}
		}
		boolean extractedCodeFound = false;
		for(VariableDeclarationContainer addedOperation : addedOperationsCalledByMappedElementsT2) {
			if(addedOperation instanceof UMLOperation) {
				StatementObject returnedStatement = ((UMLOperation)addedOperation).singleReturnStatement();
				if(returnedStatement != null) {
					List<AbstractCall> callsT2 = returnedStatement.getMethodInvocations();
					for(AbstractCodeFragment nonMappedLeafT1 : operationBodyMapper.getNonMappedLeavesT1()) {
						List<AbstractCall> callsT1 = nonMappedLeafT1.getMethodInvocations();
						if(callsT2.size() > 0 && callsT1.containsAll(callsT2)) {
							extractedCodeFound = true;
							break;
						}
					}
				}
			}
		}
		return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation &&
				nonMappedElementsT2CallingAddedOperation + mappedElementsT2CallingAddedOperation >= nonMappedElementsT2WithoutThoseCallingAddedOperation && !operationBodyMapper.isAnonymousCollapse()) ||
				(operationBodyMapper.getMappings().size() > nonMappedElementsT2 && nonMappedElementsT2CallingAddedOperation > 0 &&
				operationBodyMapper.getContainer1().getClassName().equals(operationBodyMapper.getContainer2().getClassName())) ||
				(mappings > 0 && extractedCodeFound);
	}

	private boolean mappedElementsMoreThanNonMappedT1(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1();
		int nonMappedElementsT1CallingRemovedOperation = operationBodyMapper.nonMappedElementsT1CallingRemovedOperation(removedOperations);
		int nonMappedElementsT1WithoutThoseCallingRemovedOperation = nonMappedElementsT1 - nonMappedElementsT1CallingRemovedOperation;
		boolean matchFound = false;
		for(AbstractCodeFragment nonMappedLeafT1 : operationBodyMapper.getNonMappedLeavesT1()) {
			if(nonMappedLeafT1.getVariableDeclarations().size() > 0) {
				for(AbstractCodeFragment nonMappedLeafT2 : operationBodyMapper.getNonMappedLeavesT2()) {
					if(nonMappedLeafT1.getVariableDeclarations().toString().equals(nonMappedLeafT2.getVariableDeclarations().toString())) {
						mappings++;
						nonMappedElementsT1--;
						matchFound = true;
						break;
					}
				}
				if(matchFound) {
					break;
				}
			}
		}
		return mappings > nonMappedElementsT1 || (mappings >= nonMappedElementsT1WithoutThoseCallingRemovedOperation &&
				nonMappedElementsT1CallingRemovedOperation >= nonMappedElementsT1WithoutThoseCallingRemovedOperation);
	}

	private UMLOperationBodyMapper findBestMapper(TreeSet<UMLOperationBodyMapper> mapperSet) {
		List<UMLOperationBodyMapper> mapperList = new ArrayList<UMLOperationBodyMapper>(mapperSet);
		UMLOperationBodyMapper bestMapper = mapperSet.first();
		VariableDeclarationContainer bestMapperOperation1 = bestMapper.getContainer1();
		VariableDeclarationContainer bestMapperOperation2 = bestMapper.getContainer2();
		int bestCommonParameterTypes = bestMapperOperation1.commonParameterTypes(bestMapperOperation2).size();
		VariableDeclarationContainer secondBestMapperOperation1 = mapperList.size() > 1 ?  mapperList.get(1).getContainer1() : null;
		VariableDeclarationContainer secondBestMapperOperation2 = mapperList.size() > 1 ?  mapperList.get(1).getContainer2() : null;
		boolean secondBestMapperEqualSignatureWithCommonParameterTypes = secondBestMapperOperation1 != null && secondBestMapperOperation2 != null ?
				equalSignatureWithCommonParameterTypes(secondBestMapperOperation1, secondBestMapperOperation2) && !secondBestMapperOperation1.isConstructor() && !secondBestMapperOperation2.isConstructor() : false;
		int secondBestCommontParameterTypes = secondBestMapperOperation1 != null && secondBestMapperOperation2 != null ?
				secondBestMapperOperation1.commonParameterTypes(secondBestMapperOperation2).size() : 0;
		boolean identicalBodyWithOperation1OfTheBestMapper = identicalBodyWithAnotherAddedMethod(bestMapper);
		boolean identicalBodyWithOperation2OfTheBestMapper = identicalBodyWithAnotherRemovedMethod(bestMapper);
		boolean secondBestMapperHasIdenticalParameterTypes = secondBestMapperOperation1 != null && secondBestMapperOperation2 != null &&
				secondBestMapperOperation1.getParameterTypeList().equals(secondBestMapperOperation2.getParameterTypeList()) &&
				secondBestMapperOperation1.getParameterTypeList().size() > 0 && secondBestMapperOperation1.getName().equals(secondBestMapperOperation2.getName());
		if(secondBestMapperHasIdenticalParameterTypes) {
			return mapperList.get(1);
		}
		if(equalSignatureWithCommonParameterTypes(bestMapperOperation1, bestMapperOperation2) &&
				!(secondBestMapperEqualSignatureWithCommonParameterTypes && secondBestCommontParameterTypes > bestCommonParameterTypes) &&
				!identicalBodyWithOperation1OfTheBestMapper && !identicalBodyWithOperation2OfTheBestMapper) {
			return bestMapper;
		}
		for(int i=1; i<mapperList.size(); i++) {
			UMLOperationBodyMapper mapper = mapperList.get(i);
			boolean equalSignatureWithCommonParameterTypes = equalSignatureWithCommonParameterTypes(mapper.getContainer1(), mapper.getContainer2());
			if(checkForCalls(mapper) || equalSignatureWithCommonParameterTypes) {
				int commonParameterTypes = mapper.getContainer1().commonParameterTypes(mapper.getContainer2()).size();
				VariableDeclarationContainer operation2 = mapper.getContainer2();
				List<AbstractCall> operationInvocations2 = operation2.getAllOperationInvocations();
				boolean anotherMapperCallsOperation2OfTheBestMapper = false;
				for(AbstractCall invocation : operationInvocations2) {
					if(!(invocation instanceof MethodReference) && invocation.matchesOperation(bestMapper.getContainer2(), operation2, this, modelDiff) && !invocation.matchesOperation(bestMapper.getContainer1(), operation2, this, modelDiff) &&
							(!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, removedOperations) || commonParameterTypes > bestCommonParameterTypes) &&
							(invocation.getExpression() == null || invocation.getExpression().equals(LANG2.THIS))) {
						boolean skip = false;
						for(UMLOperationBodyMapper m : operationBodyMapperList) {
							if(m.getContainer1().getName().equals(operation2.getName()) && !m.getContainer1().stringRepresentation().equals(m.getContainer2().stringRepresentation())) {
								List<AbstractCall> invocations1 = m.getContainer1().getAllOperationInvocations();
								for(AbstractCall inv : invocations1) {
									if(inv.matchesOperation(bestMapper.getContainer1(), m.getContainer1(), this, modelDiff)) {
										skip = true;
									}
								}
							}
						}
						if(!skip) {
							anotherMapperCallsOperation2OfTheBestMapper = true;
							extractMethodCandidates.add(bestMapper);
							break;
						}
					}
				}
				VariableDeclarationContainer operation1 = mapper.getContainer1();
				List<AbstractCall> operationInvocations1 = operation1.getAllOperationInvocations();
				boolean anotherMapperCallsOperation1OfTheBestMapper = false;
				for(AbstractCall invocation : operationInvocations1) {
					if(!(invocation instanceof MethodReference) && invocation.matchesOperation(bestMapper.getContainer1(), operation1, this, modelDiff) && !invocation.matchesOperation(bestMapper.getContainer2(), operation1, this, modelDiff) &&
							!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, addedOperations) &&
							(invocation.getExpression() == null || invocation.getExpression().equals(LANG1.THIS))) {
						boolean skip = false;
						for(UMLOperationBodyMapper m : operationBodyMapperList) {
							if(m.getContainer2().getName().equals(operation1.getName()) && !m.getContainer1().stringRepresentation().equals(m.getContainer2().stringRepresentation())) {
								List<AbstractCall> invocations2 = m.getContainer2().getAllOperationInvocations();
								for(AbstractCall inv : invocations2) {
									if(inv.matchesOperation(bestMapper.getContainer2(), m.getContainer2(), this, modelDiff)) {
										skip = true;
									}
								}
							}
						}
						if(!skip) {
							anotherMapperCallsOperation1OfTheBestMapper = true;
							break;
						}
					}
				}
				if(anotherMapperCallsOperation2OfTheBestMapper || anotherMapperCallsOperation1OfTheBestMapper) {
					return mapper;
				}
			}
		}
		int consistentMethodInvocationRenameMismatchesForBestMapper = mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenames);
		if(consistentMethodInvocationRenameMismatchesForBestMapper > 0 && !exactMappings(bestMapper)) {
			boolean debatable = false;
			for(int i=1; i<mapperList.size(); i++) {
				UMLOperationBodyMapper mapper = mapperList.get(i);
				int consistentMethodInvocationRenameMismatchesForCurrentMapper = mismatchesConsistentMethodInvocationRename(mapper, consistentMethodInvocationRenames);
				if(consistentMethodInvocationRenameMismatchesForCurrentMapper < consistentMethodInvocationRenameMismatchesForBestMapper &&
						mapper.getContainer1().isConstructor() == mapper.getContainer2().isConstructor() &&
						mapper.getContainer1().isGetter() == mapper.getContainer2().isGetter()) {
					return mapper;
				}
				if(consistentMethodInvocationRenameMismatchesForCurrentMapper == consistentMethodInvocationRenameMismatchesForBestMapper) {
					debatable = true;
				}
			}
			if(debatable || mapperSet.size() == 1) {
				return null;
			}
		}
		consistentMethodInvocationRenameMismatchesForBestMapper = mismatchesConsistentMethodInvocationRename(bestMapper, consistentMethodInvocationRenamesInModel);
		if(mapperSet.size() > 1 && consistentMethodInvocationRenameMismatchesForBestMapper > 0 && !exactMappings(bestMapper)) {
			for(int i=1; i<mapperList.size(); i++) {
				UMLOperationBodyMapper mapper = mapperList.get(i);
				int consistentMethodInvocationRenameMismatchesForCurrentMapper = mismatchesConsistentMethodInvocationRename(mapper, consistentMethodInvocationRenamesInModel);
				if(consistentMethodInvocationRenameMismatchesForCurrentMapper < consistentMethodInvocationRenameMismatchesForBestMapper &&
						mapper.getContainer1().isConstructor() == mapper.getContainer2().isConstructor() &&
						mapper.getContainer1().isGetter() == mapper.getContainer2().isGetter() &&
						bestMapper.exactMatches() <= mapper.exactMatches()) {
					return mapper;
				}
			}
		}
		if(identicalBodyWithOperation2OfTheBestMapper || identicalBodyWithOperation1OfTheBestMapper) {
			for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
				if(replacement.getInvokedOperationBefore().getName().equals(bestMapper.getContainer1().getName()) &&
						replacement.getInvokedOperationAfter().getName().equals(bestMapper.getContainer2().getName())) {
					return bestMapper;
				}
			}
			return null;
		}
		return bestMapper;
	}

	private boolean checkForCalls(UMLOperationBodyMapper mapper) {
		if(mapper.getMappings().size() > 1) {
			return true;
		}
		if(mapper.getMappings().size() == 1) {
			AbstractCodeMapping mapping = mapper.getMappings().iterator().next();
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			AbstractCall call1 = fragment1.invocationCoveringEntireFragment();
			AbstractCall call2 = fragment2.invocationCoveringEntireFragment();
			if(call1 != null && call2 != null && call1.identicalExpression(call2) && call1.identicalName(call2)) {
				return true;
			}
			call1 = fragment1.creationCoveringEntireFragment();
			call2 = fragment2.creationCoveringEntireFragment();
			if(call1 != null && call2 != null && call1.identicalExpression(call2) && call1.identicalName(call2)) {
				return true;
			}
		}
		return false;
	}

	private boolean equalSignatureWithCommonParameterTypes(VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
		return operation1.equalReturnParameter(operation2) &&
				operation1.getName().equals(operation2.getName()) &&
				operation1.commonParameterTypes(operation2).size() > 0;
	}

	private boolean identicalBodyWithAnotherAddedMethod(UMLOperationBodyMapper mapper) {
		VariableDeclarationContainer operation1 = mapper.getContainer1();
		List<String> stringRepresentation = operation1.stringRepresentation();
		// 3 corresponds to the opening and closing bracket of a method + a single statement
		if(stringRepresentation.size() > 3) {
			for(UMLOperation addedOperation : addedOperations) {
				if(!mapper.getContainer2().equals(addedOperation)) {
					OperationBody body = addedOperation.getBody();
					if(body != null && body.getBodyHashCode() == operation1.getBodyHashCode()) {
						return true;
					}
					else if(equalSignatureWithCommonParameterTypes(operation1, addedOperation)) {
						List<String> commonStatements = new ArrayList<String>();
						List<String> addedOperationStringRepresentation = addedOperation.stringRepresentation();
						for(String statement : addedOperationStringRepresentation) {
							if(!statement.equals(LANG2.OPEN_BLOCK) && !statement.equals(LANG2.CLOSE_BLOCK) && !statement.equals(LANG2.TRY) && !statement.startsWith("catch(") && !statement.startsWith(LANG2.CASE_SPACE) && !statement.startsWith("default :") &&
									!statement.equals(LANG2.RETURN_TRUE) && !statement.equals(LANG2.RETURN_FALSE) && !statement.equals(LANG2.RETURN_THIS) && !statement.equals(LANG2.RETURN_NULL) && !statement.equals(LANG2.RETURN_STATEMENT)) {
								if(stringRepresentation.contains(statement)) {
									commonStatements.add(statement);
								}
							}
						}
						if(commonStatements.size() > mapper.exactMatches()*2) {
							return true;
						}
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass addedClass : modelDiff.getAddedClasses()) {
					for(UMLOperation addedOperation : addedClass.getOperations()) {
						OperationBody body = addedOperation.getBody();
						List<String> parameterNameList = addedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation1.getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(mapper.getParameterNameList1())) {
							return true;
						}
					}
				}
			}
		}
		else if(stringRepresentation.size() == 3) {
			int counter = 0;
			for(UMLOperation addedOperation : addedOperations) {
				if(!mapper.getContainer2().equals(addedOperation)) {
					OperationBody body = addedOperation.getBody();
					List<String> parameterNameList = addedOperation.getParameterNameList();
					if(body != null && body.getBodyHashCode() == operation1.getBodyHashCode() &&
							parameterNameList.size() > 0 && parameterNameList.equals(mapper.getParameterNameList1())) {
						counter++;
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass addedClass : modelDiff.getAddedClasses()) {
					for(UMLOperation addedOperation : addedClass.getOperations()) {
						OperationBody body = addedOperation.getBody();
						List<String> parameterNameList = addedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation1.getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(mapper.getParameterNameList1())) {
							counter++;
						}
					}
				}
			}
			if(counter == 1 && !existingMapperWithIdenticalMapping(stringRepresentation.get(1))) {
				return true;
			}
		}
		return false;
	}

	private boolean identicalBodyWithAnotherRemovedMethod(UMLOperationBodyMapper mapper) {
		VariableDeclarationContainer operation2 = mapper.getContainer2();
		List<String> stringRepresentation = operation2.stringRepresentation();
		// 3 corresponds to the opening and closing bracket of a method + a single statement
		if(stringRepresentation.size() > 3) {
			for(UMLOperation removedOperation : removedOperations) {
				if(!mapper.getContainer1().equals(removedOperation)) {
					OperationBody body = removedOperation.getBody();
					if(body != null && body.getBodyHashCode() == operation2.getBodyHashCode()) {
						return true;
					}
					else if(equalSignatureWithCommonParameterTypes(removedOperation, operation2)) {
						List<String> commonStatements = new ArrayList<String>();
						List<String> removedOperationStringRepresentation = removedOperation.stringRepresentation();
						for(String statement : removedOperationStringRepresentation) {
							if(!statement.equals(LANG1.OPEN_BLOCK) && !statement.equals(LANG1.CLOSE_BLOCK) && !statement.equals(LANG1.TRY) && !statement.startsWith("catch(") && !statement.startsWith(LANG1.CASE_SPACE) && !statement.startsWith("default :") &&
									!statement.equals(LANG1.RETURN_TRUE) && !statement.equals(LANG1.RETURN_FALSE) && !statement.equals(LANG1.RETURN_THIS) && !statement.equals(LANG1.RETURN_NULL) && !statement.equals(LANG1.RETURN_STATEMENT)) {
								if(stringRepresentation.contains(statement)) {
									commonStatements.add(statement);
								}
							}
						}
						if(commonStatements.size() > mapper.exactMatches()*2) {
							return true;
						}
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass removedClass : modelDiff.getRemovedClasses()) {
					for(UMLOperation removedOperation : removedClass.getOperations()) {
						OperationBody body = removedOperation.getBody();
						List<String> parameterNameList = removedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation2.getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(mapper.getParameterNameList2())) {
							return true;
						}
					}
				}
			}
		}
		else if(stringRepresentation.size() == 3) {
			int counter = 0;
			for(UMLOperation removedOperation : removedOperations) {
				if(!mapper.getContainer1().equals(removedOperation)) {
					OperationBody body = removedOperation.getBody();
					List<String> parameterNameList = removedOperation.getParameterNameList();
					if(body != null && body.getBodyHashCode() == operation2.getBodyHashCode() &&
							parameterNameList.size() > 0 && parameterNameList.equals(mapper.getParameterNameList2())) {
						counter++;
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass removedClass : modelDiff.getRemovedClasses()) {
					for(UMLOperation removedOperation : removedClass.getOperations()) {
						OperationBody body = removedOperation.getBody();
						List<String> parameterNameList = removedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation2.getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(mapper.getParameterNameList2())) {
							counter++;
						}
					}
				}
			}
			if(counter == 1 && !existingMapperWithIdenticalMapping(stringRepresentation.get(1))) {
				return true;
			}
		}
		return false;
	}

	private boolean existingMapperWithIdenticalMapping(String stringRepresentation) {
		for(int i=operationBodyMapperList.size()-1; i>=0; i--) {
			UMLOperationBodyMapper mapper = operationBodyMapperList.get(i);
			for(AbstractCodeMapping mapping : mapper.getExactMatches()) {
				if(mapping.getFragment1().getString().equals(stringRepresentation) ||
						mapping.getFragment2().getString().equals(stringRepresentation)) {
					return true;
				}
			}
		}
		return false;
	}

	private int mismatchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper, Map<MethodInvocationReplacement, UMLOperationBodyMapper> map) {
		int mismatchCount = 0;
		List<UMLType> parameterTypeList1 = mapper.getContainer1().getParameterTypeList();
		List<UMLType> parameterTypeList2 = mapper.getContainer2().getParameterTypeList();
		boolean equalParameterTypes = parameterTypeList1.equals(parameterTypeList2) && parameterTypeList1.size() > 0;
		for(MethodInvocationReplacement rename : map.keySet()) {
			UMLOperationBodyMapper referringMapper = map.get(rename);
			AbstractCall callBefore = rename.getInvokedOperationBefore();
			AbstractCall callAfter = rename.getInvokedOperationAfter();
			if(equalParameterTypes && callBefore.arguments().size() != callAfter.arguments().size()) {
				continue;
			}
			if(callBefore.matchesOperation(mapper.getContainer1(), referringMapper.getContainer1(), this, modelDiff) &&
					!callAfter.matchesOperation(mapper.getContainer2(), referringMapper.getContainer2(), this, modelDiff)) {
				mismatchCount++;
			}
			else if(!callBefore.matchesOperation(mapper.getContainer1(), referringMapper.getContainer1(), this, modelDiff) &&
					callAfter.matchesOperation(mapper.getContainer2(), referringMapper.getContainer2(), this, modelDiff)) {
				mismatchCount++;
			}
		}
		return mismatchCount;
	}

	private boolean operationContainsMethodInvocationWithTheSameNameAndCommonArguments(AbstractCall invocation, List<UMLOperation> operations) {
		for(UMLOperation operation : operations) {
			List<AbstractCall> operationInvocations = operation.getAllOperationInvocations();
			for(AbstractCall operationInvocation : operationInvocations) {
				Set<String> argumentIntersection = new LinkedHashSet<String>(operationInvocation.arguments());
				argumentIntersection.retainAll(invocation.arguments());
				if(operationInvocation.getName().equals(invocation.getName()) && !argumentIntersection.isEmpty()) {
					return true;
				}
				else if(argumentIntersection.size() > 0 && argumentIntersection.size() == invocation.arguments().size() &&
						!operationInvocation.getName().contains(invocation.getName()) && !invocation.getName().contains(operationInvocation.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean singleUnmatchedStatementCallsAddedOperation(UMLOperationBodyMapper operationBodyMapper) {
		List<AbstractCodeFragment> nonMappedLeavesT1 = operationBodyMapper.getNonMappedLeavesT1();
		List<AbstractCodeFragment> nonMappedLeavesT2 = operationBodyMapper.getNonMappedLeavesT2();
		if(nonMappedLeavesT1.size() == 1 && nonMappedLeavesT2.size() == 1) {
			AbstractCodeFragment statementT2 = nonMappedLeavesT2.get(0);
			AbstractCall invocationT2 = statementT2.invocationCoveringEntireFragment();
			if(invocationT2 != null) {
				for(UMLOperation addedOperation : addedOperations) {
					if(invocationT2.matchesOperation(addedOperation, operationBodyMapper.getContainer2(), this, modelDiff)) {
						AbstractCodeFragment statementT1 = nonMappedLeavesT1.get(0);
						AbstractCall invocationT1 = statementT1.invocationCoveringEntireFragment();
						if(invocationT1 != null && addedOperation.getAllOperationInvocations().contains(invocationT1)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean compatibleSignatures(UMLOperation removedOperation, UMLOperation addedOperation, int absoluteDifferenceInPosition) {
		if(addedOperation.compatibleSignature(removedOperation)) {
			return true;
		}
		if(absoluteDifferenceInPosition == 0 || operationsBeforeOrAfterMatch(removedOperation, addedOperation)) {
			if(!gettersWithDifferentReturnType(removedOperation, addedOperation)) {
				if(addedOperation.getParameterTypeList().equals(removedOperation.getParameterTypeList()) || addedOperation.normalizedNameDistance(removedOperation) <= MAX_OPERATION_NAME_DISTANCE) {
					return true;
				}
				else if(addedOperation.isSetter() && removedOperation.isSetter() && sameAttributeIndex(removedOperation, addedOperation)) {
					return true;
				}
				else if(addedOperation.hasTestAnnotation() && removedOperation.hasTestAnnotation()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean gettersWithDifferentReturnType(UMLOperation removedOperation, UMLOperation addedOperation) {
		if(removedOperation.isGetter() && addedOperation.isGetter()) {
			UMLType type1 = removedOperation.getReturnParameter().getType();
			UMLType type2 = addedOperation.getReturnParameter().getType();
			if(!removedOperation.equalReturnParameter(addedOperation) && !type1.compatibleTypes(type2) && !sameAttributeIndex(removedOperation, addedOperation)) {
				return true;
			}
		}
		return false;
	}

	private boolean sameAttributeIndex(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<String> variables1 = removedOperation.getAllVariables();
		int index1 = -1;
		if(variables1.size() > 0) {
			int count = 0;
			for(UMLAttribute attribute : originalClass.getAttributes()) {
				if(attribute.getName().equals(variables1.get(0)) || variables1.get(0).equals(LANG1.THIS_DOT + attribute.getName())) {
					index1 = count;
					break;
				}
				count++;
			}
		}
		List<String> variables2 = addedOperation.getAllVariables();
		int index2 = -1;
		if(variables2.size() > 0) {
			int count = 0;
			for(UMLAttribute attribute : nextClass.getAttributes()) {
				if(attribute.getName().equals(variables2.get(0)) || variables2.get(0).equals(LANG2.THIS_DOT + attribute.getName())) {
					index2 = count;
					break;
				}
				count++;
			}
		}
		boolean sameAttributeIndex = index1 == index2 && index1 != -1;
		return sameAttributeIndex;
	}

	private boolean operationsBeforeAndAfterMatch(UMLOperation removedOperation, UMLOperation addedOperation) {
		UMLOperation operationBefore1 = null;
		UMLOperation operationAfter1 = null;
		List<UMLOperation> originalClassOperations = originalClass.getOperations();
		for(int i=0; i<originalClassOperations.size(); i++) {
			UMLOperation current = originalClassOperations.get(i);
			if(current.equals(removedOperation)) {
				if(i>0) {
					operationBefore1 = originalClassOperations.get(i-1);
				}
				if(i<originalClassOperations.size()-1) {
					operationAfter1 = originalClassOperations.get(i+1);
				}
			}
		}
		
		UMLOperation operationBefore2 = null;
		UMLOperation operationAfter2 = null;
		List<UMLOperation> nextClassOperations = nextClass.getOperations();
		for(int i=0; i<nextClassOperations.size(); i++) {
			UMLOperation current = nextClassOperations.get(i);
			if(current.equals(addedOperation)) {
				if(i>0) {
					operationBefore2 = nextClassOperations.get(i-1);
				}
				if(i<nextClassOperations.size()-1) {
					operationAfter2 = nextClassOperations.get(i+1);
				}
			}
		}
		
		boolean operationsBeforeMatch = false;
		if(operationBefore1 != null && operationBefore2 != null) {
			operationsBeforeMatch = (operationBefore1.equalReturnParameter(operationBefore2) && operationBefore1.getName().equals(operationBefore2.getName()))
					|| (matchingDataProviderAnnotation(removedOperation, operationBefore1) && matchingDataProviderAnnotation(addedOperation, operationBefore2))
					|| mapperListContainsMapper(operationBefore1, operationBefore2);
		}
		else if(operationBefore1 == null && operationBefore2 == null) {
			//both operations are in the first position
			operationsBeforeMatch = true;
		}
		
		boolean operationsAfterMatch = false;
		if(operationAfter1 != null && operationAfter2 != null) {
			operationsAfterMatch = (operationAfter1.equalReturnParameter(operationAfter2) && operationAfter1.getName().equals(operationAfter2.getName()))
					|| (matchingDataProviderAnnotation(removedOperation, operationAfter1) && matchingDataProviderAnnotation(addedOperation, operationAfter2));
		}
		else if(operationAfter1 == null && operationAfter2 == null) {
			//both operations are in the last position
			operationsAfterMatch = true;
		}
		
		return operationsBeforeMatch && operationsAfterMatch;
	}

	private boolean operationsBeforeOrAfterMatch(UMLOperation removedOperation, UMLOperation addedOperation) {
		UMLOperation operationBefore1 = null;
		UMLOperation operationAfter1 = null;
		List<UMLOperation> originalClassOperations = originalClass.getOperations();
		for(int i=0; i<originalClassOperations.size(); i++) {
			UMLOperation current = originalClassOperations.get(i);
			if(current.equals(removedOperation)) {
				if(i>0) {
					operationBefore1 = originalClassOperations.get(i-1);
				}
				if(i<originalClassOperations.size()-1) {
					operationAfter1 = originalClassOperations.get(i+1);
				}
			}
		}
		
		UMLOperation operationBefore2 = null;
		UMLOperation operationAfter2 = null;
		List<UMLOperation> nextClassOperations = nextClass.getOperations();
		for(int i=0; i<nextClassOperations.size(); i++) {
			UMLOperation current = nextClassOperations.get(i);
			if(current.equals(addedOperation)) {
				if(i>0) {
					operationBefore2 = nextClassOperations.get(i-1);
				}
				if(i<nextClassOperations.size()-1) {
					operationAfter2 = nextClassOperations.get(i+1);
				}
			}
		}
		
		boolean operationsBeforeMatch = false;
		if(operationBefore1 != null && operationBefore2 != null) {
			operationsBeforeMatch = (operationBefore1.equalReturnParameter(operationBefore2) && operationBefore1.getName().equals(operationBefore2.getName()))
					|| (matchingDataProviderAnnotation(removedOperation, operationBefore1) && matchingDataProviderAnnotation(addedOperation, operationBefore2));
		}
		else if(operationBefore1 == null && operationBefore2 == null && !removedOperation.isConstructor() && !addedOperation.isConstructor()) {
			//both operations are in the first position
			operationsBeforeMatch = true;
		}
		
		boolean operationsAfterMatch = false;
		if(operationAfter1 != null && operationAfter2 != null) {
			operationsAfterMatch = (operationAfter1.equalReturnParameter(operationAfter2) && operationAfter1.getName().equals(operationAfter2.getName()))
					|| (matchingDataProviderAnnotation(removedOperation, operationAfter1) && matchingDataProviderAnnotation(addedOperation, operationAfter2));
		}
		else if(operationAfter1 == null && operationAfter2 == null) {
			//both operations are in the last position
			operationsAfterMatch = true;
		}
		
		return operationsBeforeMatch || operationsAfterMatch;
	}

	private boolean matchingDataProviderAnnotation(UMLOperation operation1, UMLOperation operation2) {
		UMLAnnotation testAnnotation = null;
		UMLAnnotation dataProviderAnnotation = null;
		if(operation1.hasTestAnnotation() && operation2.hasDataProviderAnnotation()) {
			List<UMLAnnotation> annotations1 = operation1.getAnnotations();
			for(UMLAnnotation annotation1 : annotations1) {
				if(annotation1.getTypeName().equals("Test")) {
					testAnnotation = annotation1;
					break;
				}
			}
			List<UMLAnnotation> annotations2 = operation2.getAnnotations();
			for(UMLAnnotation annotation2 : annotations2) {
				if(annotation2.getTypeName().equals("DataProvider")) {
					dataProviderAnnotation = annotation2;
					break;
				}
			}
		}
		else if(operation2.hasTestAnnotation() && operation1.hasDataProviderAnnotation()) {
			List<UMLAnnotation> annotations2 = operation2.getAnnotations();
			for(UMLAnnotation annotation2 : annotations2) {
				if(annotation2.getTypeName().equals("Test")) {
					testAnnotation = annotation2;
					break;
				}
			}
			List<UMLAnnotation> annotations1 = operation1.getAnnotations();
			for(UMLAnnotation annotation1 : annotations1) {
				if(annotation1.getTypeName().equals("DataProvider")) {
					dataProviderAnnotation = annotation1;
					break;
				}
			}
		}
		if(testAnnotation != null && dataProviderAnnotation != null) {
			Map<String, AbstractExpression> testMemberValuePairs = testAnnotation.getMemberValuePairs();
			if(testMemberValuePairs.containsKey("dataProvider")) {
				Map<String, AbstractExpression> dataProviderMemberValuePairs = dataProviderAnnotation.getMemberValuePairs();
				if(dataProviderMemberValuePairs.containsKey("name")) {
					return testMemberValuePairs.get("dataProvider").getExpression().equals(dataProviderMemberValuePairs.get("name").getExpression());
				}
			}
		}
		return false;
	}

	protected boolean containCallToOperation(VariableDeclarationContainer calledOperation, VariableDeclarationContainer callerOperation) {
		for(AbstractCall invocation : callerOperation.getAllOperationInvocations()) {
			if(invocation.matchesOperation(calledOperation, callerOperation, this, modelDiff)) {
				return true;
			}
		}
		return false;
	}

	private boolean existingMapperDelegatesToAddedOperation(UMLOperation addedOperation) {
		for(UMLOperationBodyMapper mapper : this.operationBodyMapperList) {
			AbstractCall call = mapper.getContainer2().isDelegate();
			if(call != null && call.matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff) &&
					mapper.getContainer1().isDelegate() == null && mapper.getContainer1().stringRepresentation().size() > 4) {
				return true;
			}
		}
		return false;
	}

	protected List<UMLOperationBodyMapper> checkForExtractedOperations() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> extractedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		Set<UMLOperationBodyMapper> nestedFunctionMappers = new LinkedHashSet<>();
		List<UMLOperation> outerClassAddedOperations = new ArrayList<>();
		if(modelDiff != null && getNextClassName().contains(".")) {
			String outerClassName = getNextClassName().substring(0, getNextClassName().lastIndexOf("."));
			UMLClassBaseDiff outerClassDiff = modelDiff.getUMLClassDiff(outerClassName);
			if(outerClassDiff != null) {
				outerClassAddedOperations.addAll(outerClassDiff.getAddedOperations());
			}
		}
		List<UMLOperation> allAddedOperations = new ArrayList<>(addedOperations);
		allAddedOperations.addAll(addedNestedOperations);
		allAddedOperations.addAll(outerClassAddedOperations);
		List<UMLOperationBodyMapper> allMappers = getOperationBodyMapperListIncludingNestedMappersInAnonymousClassDiffs();
		for(UMLOperationBodyMapper mapper : allMappers) {
			ExtractOperationDetection detection = new ExtractOperationDetection(mapper, allAddedOperations, this, modelDiff);
			List<UMLOperation> sortedAddedOperations = detection.getAddedOperationsSortedByCalls();
			for(UMLOperation addedOperation : sortedAddedOperations) {
				List<ExtractOperationRefactoring> refs = detection.check(addedOperation);
				List<ExtractOperationRefactoring> discarded = new ArrayList<>();
				List<ExtractOperationRefactoring> duplicates = new ArrayList<>();
				List<ExtractOperationRefactoring> superSets = new ArrayList<>();
				List<ExtractOperationRefactoring> subSets = new ArrayList<>();
				if(refs.size() > 1) {
					for(ExtractOperationRefactoring refactoring : refs) {
						Set<AbstractCodeMapping> mappings = refactoring.getBodyMapper().getMappings();
						if(mappings.size() == 1) {
							AbstractCodeMapping mapping = mappings.iterator().next();
							if(!mapping.getFragment1().getString().equals(mapping.getFragment2().getString())) {
								AbstractCall call1 = mapping.getFragment1().invocationCoveringEntireFragment();
								AbstractCall call2 = mapping.getFragment2().invocationCoveringEntireFragment();
								if(call1 != null && call2 != null && call1.getName().equals(refactoring.getExtractedOperation().getName()) &&
										call2.getName().equals(refactoring.getExtractedOperation().getName())) {
									discarded.add(refactoring);
								}
							}
						}
					}
				}
				for(ExtractOperationRefactoring refactoring : refs) {
					for(Refactoring r : refactorings) {
						if(r instanceof ExtractOperationRefactoring) {
							ExtractOperationRefactoring ex = (ExtractOperationRefactoring)r;
							if(ex.getBodyMapper().getMappings().equals(refactoring.getBodyMapper().getMappings())) {
								duplicates.add(refactoring);
							}
							else if(ex.toString().equals(refactoring.toString()) &&
									refactoring.getBodyMapper().getMappings().containsAll(ex.getBodyMapper().getMappings())) {
								superSets.add(refactoring);
								subSets.add(ex);
							}
						}
					}
				}
				if(discarded.equals(refs)) {
					discarded.clear();
				}
				for(ExtractOperationRefactoring refactoring : refs) {
					if(!discarded.contains(refactoring) && !duplicates.contains(refactoring)) {
						if(superSets.contains(refactoring)) {
							this.refactorings.removeAll(subSets);
						}
						CompositeStatementObject synchronizedBlock = refactoring.extractedFromSynchronizedBlock();
						if(synchronizedBlock != null) {
							refactoring.getBodyMapper().getParentMapper().getNonMappedInnerNodesT1().remove(synchronizedBlock);
						}
						refactorings.add(refactoring);
						UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
						if(operationBodyMapper.getOperation1() != null &&
								operationBodyMapper.getOperation1().getNestedOperations().size() > 0 &&
								operationBodyMapper.getOperation2() != null &&
								mapper.getOperation2().getNestedOperations().size() < operationBodyMapper.getOperation1().getNestedOperations().size() &&
								addedOperation.getNestedOperations().size() > 0) {
							nestedFunctionMappers.addAll(processNestedOperationsForExtractedMethod(operationBodyMapper.getOperation1(), addedOperation));
						}
						extractedOperationMappers.add(operationBodyMapper);
						mapper.addChildMapper(operationBodyMapper);
						if(mapper.getChildMappers().size() == 1 && mapper.getContainer1().getJavadoc() != null && mapper.getContainer2().getJavadoc() != null) {
							UMLOperationDiff signatureDiff = new UMLOperationDiff(operationBodyMapper);
							mapper.updateJavadocDiff(new UMLJavadocDiff(mapper.getContainer1().getJavadoc(), mapper.getContainer2().getJavadoc(), signatureDiff));
						}
						operationsToBeRemoved.add(addedOperation);
					}
				}
			}
		}
		operationBodyMapperList.addAll(nestedFunctionMappers);
		if(extractedOperationMappers.size() > 0) {
			MappingOptimizer optimizer = new MappingOptimizer(this);
			for(UMLOperationBodyMapper mapper : allMappers) {
				optimizer.optimizeDuplicateMappingsForExtract(mapper, refactorings);
			}
		}
		Set<UMLOperationBodyMapper> zeroMappingMappers = new LinkedHashSet<UMLOperationBodyMapper>();
		for(UMLOperationBodyMapper operationBodyMapper : extractedOperationMappers) {
			if(operationBodyMapper.getMappings().size() == 0) {
				operationsToBeRemoved.remove(operationBodyMapper.getContainer2());
				zeroMappingMappers.add(operationBodyMapper);
			}
			else {
				processMapperRefactorings(operationBodyMapper, refactorings);
			}
		}
		addedOperations.removeAll(operationsToBeRemoved);
		extractedOperationMappers.removeAll(zeroMappingMappers);
		return extractedOperationMappers;
	}

	private Set<UMLOperationBodyMapper> processNestedOperationsForExtractedMethod(UMLOperation operation1, UMLOperation operation2) throws RefactoringMinerTimedOutException {
		Set<UMLOperationBodyMapper> nestedMappers = new LinkedHashSet<>();
		for(UMLOperation operation : operation1.getNestedOperations()) {
			UMLOperation operationWithTheSameSignature = operation2.nestedOperationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(removedOperations.contains(operation) && operationWithTheSameSignature != null && !mapperListContainsOperation(operation, operationWithTheSameSignature)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature, this);
				nestedMappers.add(mapper);
			}
		}
		return nestedMappers;
	}
}
