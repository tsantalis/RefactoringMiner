package gr.uom.java.xmi.diff;

import static gr.uom.java.xmi.decomposition.Visitor.stringify;
import static gr.uom.java.xmi.Constants.JAVA;
import static gr.uom.java.xmi.decomposition.Visitor.METHOD_SIGNATURE_PATTERN;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.Visibility;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;

public abstract class UMLClassBaseDiff extends UMLAbstractClassDiff implements Comparable<UMLClassBaseDiff> {

	public static final double BUILDER_STATEMENT_RATIO_THRESHOLD = 0.7;
	private static final int MAXIMUM_NUMBER_OF_COMPARED_METHODS = 20;
	public static final double MAX_OPERATION_NAME_DISTANCE = 0.4;
	private boolean visibilityChanged;
	private Visibility oldVisibility;
	private Visibility newVisibility;
	private boolean abstractionChanged;
	private boolean oldAbstraction;
	private boolean newAbstraction;
	private boolean staticChanged;
	private boolean finalChanged;
	private boolean superclassChanged;
	private UMLType oldSuperclass;
	private UMLType newSuperclass;
	private List<UMLType> addedImplementedInterfaces;
	private List<UMLType> removedImplementedInterfaces;
	private UMLImportListDiff importDiffList;
	private UMLTypeParameterListDiff typeParameterDiffList;
	private Map<MethodInvocationReplacement, UMLOperationBodyMapper> consistentMethodInvocationRenamesInModel;
	private Map<MethodInvocationReplacement, UMLOperationBodyMapper> consistentMethodInvocationRenames;
	private Set<UMLOperationBodyMapper> potentialCodeMoveBetweenSetUpTearDownMethods = new LinkedHashSet<>();
	private Set<UMLOperationBodyMapper> movedMethodsInDifferentPositionWithinFile = new LinkedHashSet<>();
	private Optional<Pair<UMLType, UMLType>> implementedInterfaceBecomesSuperclass;
	private Optional<Pair<UMLType, UMLType>> superclassBecomesImplementedInterface;
	private Optional<UMLJavadocDiff> javadocDiff;
	private Optional<UMLJavadocDiff> packageDeclarationJavadocDiff;
	private UMLCommentListDiff packageDeclarationCommentListDiff;

	public UMLClassBaseDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
		super(originalClass, nextClass, modelDiff);
		this.visibilityChanged = false;
		this.abstractionChanged = false;
		this.superclassChanged = false;
		this.addedImplementedInterfaces = new ArrayList<UMLType>();
		this.removedImplementedInterfaces = new ArrayList<UMLType>();
		this.consistentMethodInvocationRenamesInModel = findConsistentMethodInvocationRenamesInModelDiff();
		this.implementedInterfaceBecomesSuperclass = Optional.empty();
		this.superclassBecomesImplementedInterface = Optional.empty();
		if(originalClass.getJavadoc() != null && nextClass.getJavadoc() != null) {
			UMLJavadocDiff diff = new UMLJavadocDiff(originalClass.getJavadoc(), nextClass.getJavadoc());
			this.javadocDiff = Optional.of(diff);
		}
		else {
			this.javadocDiff = Optional.empty();
		}
		if(originalClass.getPackageDeclarationJavadoc() != null && nextClass.getPackageDeclarationJavadoc() != null) {
			UMLJavadocDiff diff = new UMLJavadocDiff(originalClass.getPackageDeclarationJavadoc(), nextClass.getPackageDeclarationJavadoc());
			this.packageDeclarationJavadocDiff = Optional.of(diff);
		}
		else {
			this.packageDeclarationJavadocDiff = Optional.empty();
		}
		processImports();
	}

	public UMLClass getOriginalClass() {
		return (UMLClass) originalClass;
	}

	public UMLClass getNextClass() {
		return (UMLClass) nextClass;
	}

	public Optional<UMLJavadocDiff> getJavadocDiff() {
		return javadocDiff;
	}

	public Optional<UMLJavadocDiff> getPackageDeclarationJavadocDiff() {
		return packageDeclarationJavadocDiff;
	}

	public UMLCommentListDiff getPackageDeclarationCommentListDiff() {
		if(packageDeclarationCommentListDiff == null)
			packageDeclarationCommentListDiff = new UMLCommentListDiff(getOriginalClass().getPackageDeclarationComments(), getNextClass().getPackageDeclarationComments());
		return packageDeclarationCommentListDiff;
	}

	protected void reportAddedOperation(UMLOperation umlOperation) {
		this.addedOperations.add(umlOperation);
	}

	protected void reportRemovedOperation(UMLOperation umlOperation) {
		this.removedOperations.add(umlOperation);
	}

	protected void reportAddedAttribute(UMLAttribute umlAttribute) {
		this.addedAttributes.add(umlAttribute);
	}

	protected void reportRemovedAttribute(UMLAttribute umlAttribute) {
		this.removedAttributes.add(umlAttribute);
	}

	public void process() throws RefactoringMinerTimedOutException {
		processInitializers();
		processModifiers();
		processTypeParameters();
		processEnumConstants();
		processInheritance();
		processOperations();
		createBodyMappers();
		processAnonymousClasses();
		checkForOperationSignatureChanges();
		processAttributes();
		checkForAttributeChanges();
		checkForInlinedOperations();
		List<UMLOperationBodyMapper> extractedbodyMappers = checkForExtractedOperations();
		checkForExtractedOperationsWithCallsInOtherMappers();
		checkForInlinedOperationsToExtractedOperations(extractedbodyMappers);
		checkForMovedCodeBetweenOperations();
	}

	private void checkForMovedCodeBetweenOperations() throws RefactoringMinerTimedOutException {
		//find setUp and tearDown methods
		Set<UMLOperationBodyMapper> setUpMappers = new LinkedHashSet<>();
		Set<UMLOperationBodyMapper> tearDownMappers = new LinkedHashSet<>();
		Set<UMLOperationBodyMapper> constructorMappers = new LinkedHashSet<>();
		List<UMLOperationBodyMapper> moveCodeMappers = new ArrayList<>();
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getContainer1().hasSetUpAnnotation() && mapper.getContainer2().hasSetUpAnnotation()) {
				setUpMappers.add(mapper);
			}
			if(mapper.getContainer1().getName().equals("setUp") && mapper.getContainer2().getName().equals("setUp")) {
				setUpMappers.add(mapper);
			}
			if(mapper.getContainer1().hasTearDownAnnotation() && mapper.getContainer2().hasTearDownAnnotation()) {
				tearDownMappers.add(mapper);
			}
			if(mapper.getContainer1().getName().equals("tearDown") && mapper.getContainer2().getName().equals("tearDown")) {
				tearDownMappers.add(mapper);
			}
			if(mapper.getContainer1().isConstructor() && mapper.getContainer2().isConstructor()) {
				constructorMappers.add(mapper);
			}
			if(mapper.getContainer1().getName().equals("main") && mapper.getContainer2().getName().equals("main")) {
				constructorMappers.add(mapper);
			}
			if(mapper.getAnonymousClassDiffs().size() > 0 && mapper.nonMappedElementsT1() > 0) {
				for(UMLAnonymousClassDiff anonymousClassDiff : mapper.getAnonymousClassDiffs()) {
					for(UMLOperationBodyMapper anonymousMapper : anonymousClassDiff.getOperationBodyMapperList()) {
						if(anonymousMapper.nonMappedElementsT2() > 0) {
							UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, anonymousMapper, this);
							int invalidMappings = 0;
							for(AbstractCodeMapping mapping : moveCodeMapper.getMappings()) {
								if(mapper.alreadyMatched1(mapping.getFragment1()) || anonymousMapper.getNonMappedLeavesT1().contains(mapping.getFragment1()) ||
										anonymousMapper.getNonMappedInnerNodesT1().contains(mapping.getFragment1())) {
									invalidMappings++;
								}
							}
							if(moveCodeMapper.getMappings().size() > invalidMappings) {
								MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
								if(!moveCodeMappers.contains(moveCodeMapper))
									moveCodeMappers.add(moveCodeMapper);
								refactorings.add(ref);
							}
						}
					}
				}
			}
		}
		for(UMLOperationBodyMapper setUpMapper : setUpMappers) {
			if(setUpMapper.nonMappedElementsT2() > 0 || setUpMapper.nonMappedElementsT1() > 0) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(!mapper.equals(setUpMapper)) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, setUpMapper, this);
						if(moveCodeMapper.getMappings().size() > 0) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							refactorings.add(ref);
						}
					}
				}
			}
		}
		for(UMLOperationBodyMapper tearDownMapper : tearDownMappers) {
			if(tearDownMapper.nonMappedElementsT2() > 0 || tearDownMapper.nonMappedElementsT1() > 0) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(!mapper.equals(tearDownMapper)) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, tearDownMapper, this);
						if(moveCodeMapper.getMappings().size() > 0) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							refactorings.add(ref);
						}
					}
				}
			}
		}
		for(UMLOperationBodyMapper constructorMapper : constructorMappers) {
			if(constructorMapper.nonMappedElementsT2() > 0 || constructorMapper.nonMappedElementsT1() > 0) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(!mapper.equals(constructorMapper)) {
						if(mapper.nonMappedElementsT1() > 0) {
							UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, constructorMapper, this);
							if(moveCodeMapper.getExactMatchesWithoutLoggingStatements().size() > 0 && !mappingFoundInExtractedMethod(moveCodeMapper.getMappings())) {
								MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
								if(!moveCodeMappers.contains(moveCodeMapper))
									moveCodeMappers.add(moveCodeMapper);
								refactorings.add(ref);
							}
						}
						if(mapper.nonMappedElementsT2() > 0) {
							UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(constructorMapper, mapper, this);
							if(moveCodeMapper.getExactMatchesWithoutLoggingStatements().size() > 0 && !mappingFoundInExtractedMethod(moveCodeMapper.getMappings())) {
								MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
								if(!moveCodeMappers.contains(moveCodeMapper))
									moveCodeMappers.add(moveCodeMapper);
								refactorings.add(ref);
							}
						}
					}
				}
				Set<Refactoring> refactoringsToBeAdded = new LinkedHashSet<Refactoring>();
				for(Refactoring r : refactorings) {
					if(r instanceof MergeOperationRefactoring) {
						MergeOperationRefactoring merge = (MergeOperationRefactoring)r;
						for(UMLOperationBodyMapper mapper : merge.getMappers()) {
							if(!mapper.equals(constructorMapper)) {
								if(mapper.nonMappedElementsT1() > 0) {
									UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, constructorMapper, this);
									if(moveCodeMapper.getExactMatchesWithoutLoggingStatements().size() > 0 && !mappingFoundInExtractedMethod(moveCodeMapper.getMappings())) {
										MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
										if(!moveCodeMappers.contains(moveCodeMapper))
											moveCodeMappers.add(moveCodeMapper);
										refactoringsToBeAdded.add(ref);
									}
								}
								if(mapper.nonMappedElementsT2() > 0) {
									UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(constructorMapper, mapper, this);
									if(moveCodeMapper.getExactMatchesWithoutLoggingStatements().size() > 0 && !mappingFoundInExtractedMethod(moveCodeMapper.getMappings())) {
										MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
										if(!moveCodeMappers.contains(moveCodeMapper))
											moveCodeMappers.add(moveCodeMapper);
										refactoringsToBeAdded.add(ref);
									}
								}
							}
						}
					}
				}
				refactorings.addAll(refactoringsToBeAdded);
			}
		}
		for(UMLOperation removedOperation : removedOperations) {
			if(removedOperation.hasSetUpAnnotation() || removedOperation.hasTearDownAnnotation() ||
					removedOperation.getName().equals("setUp") || removedOperation.getName().equals("tearDown")) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(mapper.nonMappedElementsT2() > 0) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(removedOperation, mapper, this);
						if(moveCodeMapper.getMappings().size() > 0) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							refactorings.add(ref);
						}
					}
				}
			}
			else if(removedOperation.isConstructor()) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(mapper.nonMappedElementsT2() > 0) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(removedOperation, mapper, this);
						if(moveCodeMapper.mappingsWithoutBlocks() > 2) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							refactorings.add(ref);
						}
					}
				}
			}
		}
		for(UMLOperation addedOperation : addedOperations) {
			if(addedOperation.hasSetUpAnnotation() || addedOperation.hasTearDownAnnotation() ||
					addedOperation.getName().equals("setUp") || addedOperation.getName().equals("tearDown")) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(mapper.nonMappedElementsT1() > 0) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, addedOperation, this);
						if(moveCodeMapper.getMappings().size() > 0) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							refactorings.add(ref);
						}
					}
				}
			}
		}
		for(UMLOperationBodyMapper moveCodeMapper : potentialCodeMoveBetweenSetUpTearDownMethods) {
			if(moveCodeMapper.getMappings().size() > 0) {
				MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper);
				if(!moveCodeMappers.contains(moveCodeMapper))
					moveCodeMappers.add(moveCodeMapper);
				refactorings.add(ref);
			}
		}
		MappingOptimizer optimizer = new MappingOptimizer(this);
		optimizer.optimizeDuplicateMappingsForMoveCode(moveCodeMappers, refactorings);
	}

	private boolean mappingFoundInExtractedMethod(Set<AbstractCodeMapping> mappings) {
		for(Refactoring r : this.refactorings) {
			if(r instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
				for(AbstractCodeMapping newMapping : mappings) {
					for(AbstractCodeMapping oldMapping : extract.getBodyMapper().getMappings()) {
						if(newMapping.getFragment1().equals(oldMapping.getFragment1())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void processTypeParameters() {
		this.typeParameterDiffList = new UMLTypeParameterListDiff(getOriginalClass().getTypeParameters(), getNextClass().getTypeParameters());
	}

	private void processImports() {
		if(originalClass.isTopLevel() && nextClass.isTopLevel()) {
			this.importDiffList = new UMLImportListDiff(originalClass.getImportedTypes(), nextClass.getImportedTypes());
		}
	}

	public boolean hasBothAddedAndRemovedImports() {
		if(importDiffList != null) {
			return importDiffList.getAddedImports().size() > 0 && importDiffList.getRemovedImports().size() > 0;
		}
		return false;
	}

	public void findImportChanges(String nameBefore, String nameAfter) {
		if(importDiffList != null) {
			importDiffList.findImportChanges(nameBefore, nameAfter);
		}
	}

	public void findImportChanges(UMLType typeBefore, UMLType typeAfter) {
		if(importDiffList != null) {
			importDiffList.findImportChanges(typeBefore, typeAfter);
		}
	}

	public UMLImportListDiff getImportDiffList() {
		return importDiffList;
	}

	public UMLTypeParameterListDiff getTypeParameterDiffList() {
		return typeParameterDiffList;
	}

	protected void processInitializers() throws RefactoringMinerTimedOutException {
		List<UMLInitializer> initializers1 = originalClass.getInitializers();
		this.removedInitializers.addAll(initializers1);
		List<UMLInitializer> initializers2 = nextClass.getInitializers();
		this.addedInitializers.addAll(initializers2);
		if(initializers1.size() == initializers2.size()) {
			for(int i=0; i<initializers1.size(); i++) {
				UMLInitializer initializer1 = initializers1.get(i);
				UMLInitializer initializer2 = initializers2.get(i);
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(initializer1, initializer2, this);
				int mappings = mapper.mappingsWithoutBlocks();
				if(mappings > 0) {
					int nonMappedElementsT1 = mapper.nonMappedElementsT1();
					int nonMappedElementsT2 = mapper.nonMappedElementsT2();
					if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
							isPartOfMethodExtracted(initializer1, initializer2) || isPartOfMethodInlined(initializer1, initializer2)) {
						operationBodyMapperList.add(mapper);
						this.removedInitializers.remove(initializer1);
						this.addedInitializers.remove(initializer2);
					}
				}
			}
		}
		else if(initializers1.size() < initializers2.size()) {
			for(UMLInitializer initializer1 : initializers1) {
				for(UMLInitializer initializer2 : initializers2) {
					if(initializer1.isStatic() == initializer2.isStatic()) {
						UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(initializer1, initializer2, this);
						int mappings = mapper.mappingsWithoutBlocks();
						if(mappings > 0) {
							int nonMappedElementsT1 = mapper.nonMappedElementsT1();
							int nonMappedElementsT2 = mapper.nonMappedElementsT2();
							if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
									isPartOfMethodExtracted(initializer1, initializer2) || isPartOfMethodInlined(initializer1, initializer2)) {
								operationBodyMapperList.add(mapper);
								this.removedInitializers.remove(initializer1);
								this.addedInitializers.remove(initializer2);
							}
						}
					}
				}
			}
		}
		else if(initializers1.size() > initializers2.size()) {
			for(UMLInitializer initializer2 : initializers2) {
				for(UMLInitializer initializer1 : initializers1) {
					if(initializer1.isStatic() == initializer2.isStatic()) {
						UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(initializer1, initializer2, this);
						int mappings = mapper.mappingsWithoutBlocks();
						if(mappings > 0) {
							int nonMappedElementsT1 = mapper.nonMappedElementsT1();
							int nonMappedElementsT2 = mapper.nonMappedElementsT2();
							if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
									isPartOfMethodExtracted(initializer1, initializer2) || isPartOfMethodInlined(initializer1, initializer2)) {
								operationBodyMapperList.add(mapper);
								this.removedInitializers.remove(initializer1);
								this.addedInitializers.remove(initializer2);
							}
						}
					}
				}
			}
		}
	}

	private void processModifiers() {
		if(!originalClass.getVisibility().equals(nextClass.getVisibility())) {
			setVisibilityChanged(true);
			setOldVisibility(originalClass.getVisibility());
			setNewVisibility(nextClass.getVisibility());
			ChangeClassAccessModifierRefactoring refactoring = new ChangeClassAccessModifierRefactoring(oldVisibility, newVisibility, originalClass, nextClass);
			refactorings.add(refactoring);
		}
		if(!originalClass.isInterface() && !nextClass.isInterface()) {
			if(originalClass.isAbstract() != nextClass.isAbstract()) {
				setAbstractionChanged(true);
				setOldAbstraction(originalClass.isAbstract());
				setNewAbstraction(nextClass.isAbstract());
				if(nextClass.isAbstract()) {
					AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("abstract", originalClass, nextClass);
					refactorings.add(refactoring);
				}
				else if(originalClass.isAbstract()) {
					RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("abstract", originalClass, nextClass);
					refactorings.add(refactoring);
				}
			}
		}
		if(originalClass.isFinal() != nextClass.isFinal()) {
			finalChanged = true;
			if(nextClass.isFinal()) {
				AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("final", originalClass, nextClass);
				refactorings.add(refactoring);
			}
			else if(originalClass.isFinal()) {
				RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("final", originalClass, nextClass);
				refactorings.add(refactoring);
			}
		}
		if(originalClass.isStatic() != nextClass.isStatic()) {
			staticChanged = true;
			if(nextClass.isStatic()) {
				AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("static", originalClass, nextClass);
				refactorings.add(refactoring);
			}
			else if(originalClass.isStatic()) {
				RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("static", originalClass, nextClass);
				refactorings.add(refactoring);
			}
		}
	}

	public UMLOperationBodyMapper findMapperWithMatchingSignatures(UMLOperation operation1, UMLOperation operation2) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getOperation1() != null && mapper.getOperation1().equalSignature(operation1) && mapper.getOperation2() != null && mapper.getOperation2().equalSignature(operation2)) {
				return mapper;
			}
		}
		return null;
	}

	public Set<MethodInvocationReplacement> findMethodInvocationReplacementWithMatchingSignatures(UMLOperation operation1, UMLOperation operation2) {
		Set<MethodInvocationReplacement> replacements = new LinkedHashSet<MethodInvocationReplacement>();
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			for(AbstractCodeMapping mapping : mapper.getMappings()) {
				if(!mapping.isExact()) {
					List<AbstractCall> methodInvocations1 = new ArrayList<>(mapping.getFragment1().getMethodInvocations());
					List<AbstractCall> methodInvocations2 = new ArrayList<>(mapping.getFragment2().getMethodInvocations());
					Set<AbstractCall> intersection = new LinkedHashSet<>(methodInvocations1);
					intersection.retainAll(methodInvocations2);
					methodInvocations1.removeAll(intersection);
					methodInvocations2.removeAll(intersection);
					if(methodInvocations1.size() == methodInvocations2.size()) {
						AbstractCall matchedCallToOperation1 = null;
						for(AbstractCall call : methodInvocations1) {
							if(call.matchesOperation(operation1, mapper.getContainer1(), this, modelDiff)) {
								matchedCallToOperation1 = call;
								break;
							}
						}
						AbstractCall matchedCallToOperation2 = null;
						if(matchedCallToOperation1 != null) {
							for(AbstractCall call : methodInvocations2) {
								if(call.matchesOperation(operation2, mapper.getContainer2(), this, modelDiff)) {
									matchedCallToOperation2 = call;
									break;
								}
							}
						}
						if(matchedCallToOperation1 != null && matchedCallToOperation2 != null) {
							replacements.add(new MethodInvocationReplacement(matchedCallToOperation1.actualString(), matchedCallToOperation2.actualString(), matchedCallToOperation1, matchedCallToOperation2, ReplacementType.METHOD_INVOCATION));
						}
						else if(matchedCallToOperation1 != null || matchedCallToOperation2 != null) {
							int index = 0;
							if(matchedCallToOperation1 != null) {
								index = methodInvocations1.indexOf(matchedCallToOperation1);
							}
							else if(matchedCallToOperation2 != null) {
								index = methodInvocations2.indexOf(matchedCallToOperation2);
							}
							replacements.add(new MethodInvocationReplacement(methodInvocations1.get(index).actualString(), methodInvocations2.get(index).actualString(), methodInvocations1.get(index), methodInvocations2.get(index), ReplacementType.METHOD_INVOCATION));
						}
					}
				}
			}
		}
		return replacements;
	}

	public UMLOperationBodyMapper findMapperWithMatchingSignature2(UMLOperation operation2) {
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getOperation2() != null && mapper.getOperation2().equalSignature(operation2)) {
				return mapper;
			}
		}
		return null;
	}

	public Set<UMLType> nextClassCommonInterfaces(UMLClassBaseDiff other) {
		Set<UMLType> common = new LinkedHashSet<UMLType>(nextClass.getImplementedInterfaces());
		common.retainAll(other.nextClass.getImplementedInterfaces());
		return common;
	}

	protected void checkForAttributeChanges() throws RefactoringMinerTimedOutException {
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
				UMLAttribute addedAttribute = addedAttributeIterator.next();
				if(removedAttribute.getName().equals(addedAttribute.getName())) {
					UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute, this, modelDiff);
					refactorings.addAll(attributeDiff.getRefactorings());
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
					break;
				}
			}
		}
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			if(removedAttribute.getVariableDeclaration().getInitializer() != null && this.getOriginalClass().uniqueInitializer(removedAttribute)) {
				for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
					UMLAttribute addedAttribute = addedAttributeIterator.next();
					if(addedAttribute.getVariableDeclaration().getInitializer() != null && this.getNextClass().uniqueInitializer(addedAttribute)) {
						if(removedAttribute.getVariableDeclaration().getInitializer().getString().equals(addedAttribute.getVariableDeclaration().getInitializer().getString())) {
							UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute, this, modelDiff);
							refactorings.addAll(attributeDiff.getRefactorings(Collections.emptySet()));
							addedAttributeIterator.remove();
							removedAttributeIterator.remove();
							if(!attributeDiffList.contains(attributeDiff)) {
								attributeDiffList.add(attributeDiff);
							}
							break;
						}
					}
				}
			}
		}
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			if(removedAttribute.getJavadoc() != null && this.getOriginalClass().uniqueJavadoc(removedAttribute)) {
				for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
					UMLAttribute addedAttribute = addedAttributeIterator.next();
					if(addedAttribute.getJavadoc() != null && this.getNextClass().uniqueJavadoc(addedAttribute)) {
						if(removedAttribute.getJavadoc().equalText(addedAttribute.getJavadoc())) {
							UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute, this, modelDiff);
							refactorings.addAll(attributeDiff.getRefactorings(Collections.emptySet()));
							addedAttributeIterator.remove();
							removedAttributeIterator.remove();
							if(!attributeDiffList.contains(attributeDiff)) {
								attributeDiffList.add(attributeDiff);
							}
							break;
						}
					}
				}
			}
		}
	}

	protected void processAnonymousClasses() {
		for(UMLAnonymousClass umlAnonymousClass : originalClass.getAnonymousClassList()) {
    		if(!nextClass.containsAnonymousWithSameAttributesAndOperations(umlAnonymousClass))
    			this.removedAnonymousClasses.add(umlAnonymousClass);
    	}
    	for(UMLAnonymousClass umlAnonymousClass : nextClass.getAnonymousClassList()) {
    		if(!originalClass.containsAnonymousWithSameAttributesAndOperations(umlAnonymousClass))
    			this.addedAnonymousClasses.add(umlAnonymousClass);
    	}
	}

	protected void processEnumConstants() throws RefactoringMinerTimedOutException {
		for(UMLEnumConstant enumConstant : originalClass.getEnumConstants()) {
			UMLEnumConstant matchingEnumConstant = nextClass.containsEnumConstant(enumConstant);
    		if(matchingEnumConstant == null) {
    			this.removedEnumConstants.add(enumConstant);
    		}
    		else {
    			UMLEnumConstantDiff enumConstantDiff = new UMLEnumConstantDiff(enumConstant, matchingEnumConstant, this, modelDiff);
    			if(!enumConstantDiff.isEmpty()) {
	    			refactorings.addAll(enumConstantDiff.getRefactorings());
	    			this.enumConstantDiffList.add(enumConstantDiff);
    			}
    			else {
    				Pair<UMLEnumConstant, UMLEnumConstant> pair = Pair.of(enumConstant, matchingEnumConstant);
    				if(!this.commonEnumConstants.contains(pair)) {
    					this.commonEnumConstants.add(pair);
    					if(enumConstantDiff.getAnonymousClassDiff().isPresent()) {
        					this.enumConstantDiffList.add(enumConstantDiff);
        				}
    				}
    			}
    		}
    	}
    	for(UMLEnumConstant enumConstant : nextClass.getEnumConstants()) {
    		UMLEnumConstant matchingEnumConstant = originalClass.containsEnumConstant(enumConstant);
    		if(matchingEnumConstant == null) {
    			this.addedEnumConstants.add(enumConstant);
    		}
    		else {
    			UMLEnumConstantDiff enumConstantDiff = new UMLEnumConstantDiff(matchingEnumConstant, enumConstant, this, modelDiff);
    			if(!enumConstantDiff.isEmpty()) {
	    			refactorings.addAll(enumConstantDiff.getRefactorings());
					this.enumConstantDiffList.add(enumConstantDiff);
    			}
    			else {
    				Pair<UMLEnumConstant, UMLEnumConstant> pair = Pair.of(matchingEnumConstant, enumConstant);
    				if(!this.commonEnumConstants.contains(pair)) {
    					this.commonEnumConstants.add(pair);
    					if(enumConstantDiff.getAnonymousClassDiff().isPresent()) {
        					this.enumConstantDiffList.add(enumConstantDiff);
        				}
    				}
    			}
    		}
    	}
	}

	protected void processAttributes() throws RefactoringMinerTimedOutException {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
    		UMLAttribute attributeWithTheSameName = nextClass.attributeWithTheSameNameIgnoringChangedType(attribute);
			if(attributeWithTheSameName == null) {
    			this.removedAttributes.add(attribute);
    		}
			else if(!attributeDiffListContainsAttribute(attribute, attributeWithTheSameName)) {
				UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, attributeWithTheSameName, this, modelDiff);
				if(!attributeDiff.isEmpty()) {
					refactorings.addAll(attributeDiff.getRefactorings());
					if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
				}
				else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attribute, attributeWithTheSameName);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
			}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute attributeWithTheSameName = originalClass.attributeWithTheSameNameIgnoringChangedType(attribute);
			if(attributeWithTheSameName == null) {
    			this.addedAttributes.add(attribute);
    		}
			else if(!attributeDiffListContainsAttribute(attributeWithTheSameName, attribute)) {
				UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attributeWithTheSameName, attribute, this, modelDiff);
				if(!attributeDiff.isEmpty()) {
					refactorings.addAll(attributeDiff.getRefactorings());
					if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
				}
				else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attributeWithTheSameName, attribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
			}
    	}
	}

	protected void processOperations() throws RefactoringMinerTimedOutException {
		for(UMLOperation operation : originalClass.getOperations()) {
    		UMLOperation operationWithTheSameSignature = nextClass.operationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(operationWithTheSameSignature == null) {
				this.removedOperations.add(operation);
    		}
			else if(!mapperListContainsOperation(operation, operationWithTheSameSignature)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature, this);
				this.operationBodyMapperList.add(mapper);
			}
    	}
    	for(UMLOperation operation : nextClass.getOperations()) {
    		UMLOperation operationWithTheSameSignature = originalClass.operationWithTheSameSignatureIgnoringChangedTypes(operation);
			if(operationWithTheSameSignature == null) {
				this.addedOperations.add(operation);
    		}
			else if(!mapperListContainsOperation(operationWithTheSameSignature, operation)) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operationWithTheSameSignature, operation, this);
				this.operationBodyMapperList.add(mapper);
			}
    	}
	}

	private boolean attributeDiffListContainsAttribute(UMLAttribute attribute1, UMLAttribute attribute2) {
		for(UMLAttributeDiff diff : attributeDiffList) {
			if(diff.getRemovedAttribute().equals(attribute1) || diff.getAddedAttribute().equals(attribute2))
				return true;
		}
		return false;
	}

	public boolean matches(String className) {
		return this.originalClass.getName().equals(className) ||
				this.nextClass.getName().equals(className);
	}

	public boolean matches(UMLType type) {
		return this.originalClass.getName().endsWith("." + type.getClassType()) ||
				this.nextClass.getName().endsWith("." + type.getClassType());
	}

	//return true if "classMoveDiff" represents the move of a class that is inner to this.originalClass
	public boolean isInnerClassMove(UMLClassBaseDiff classDiff) {
		if(this.originalClass.isInnerClass(classDiff.originalClass) && this.nextClass.isInnerClass(classDiff.nextClass))
			return true;
		return false;
	}

	public boolean nextClassImportsType(String targetClass) {
		return nextClass.importsType(targetClass);
	}

	public boolean originalClassImportsType(String targetClass) {
		return originalClass.importsType(targetClass);
	}

	public List<UMLAttribute> nextClassAttributesOfType(String targetClass) {
		return nextClass.attributesOfType(targetClass);
	}

	public List<UMLAttribute> originalClassAttributesOfType(String targetClass) {
		return originalClass.attributesOfType(targetClass);
	}

	private void reportAddedImplementedInterface(UMLType implementedInterface) {
		this.addedImplementedInterfaces.add(implementedInterface);
	}

	private void reportRemovedImplementedInterface(UMLType implementedInterface) {
		this.removedImplementedInterfaces.add(implementedInterface);
	}

	private void setVisibilityChanged(boolean visibilityChanged) {
		this.visibilityChanged = visibilityChanged;
	}

	private void setOldVisibility(Visibility oldVisibility) {
		this.oldVisibility = oldVisibility;
	}

	private void setNewVisibility(Visibility newVisibility) {
		this.newVisibility = newVisibility;
	}

	private void setAbstractionChanged(boolean abstractionChanged) {
		this.abstractionChanged = abstractionChanged;
	}

	private void setOldAbstraction(boolean oldAbstraction) {
		this.oldAbstraction = oldAbstraction;
	}

	private void setNewAbstraction(boolean newAbstraction) {
		this.newAbstraction = newAbstraction;
	}

	private void setSuperclassChanged(boolean superclassChanged) {
		this.superclassChanged = superclassChanged;
	}

	private void setOldSuperclass(UMLType oldSuperclass) {
		this.oldSuperclass = oldSuperclass;
	}

	private void setNewSuperclass(UMLType newSuperclass) {
		this.newSuperclass = newSuperclass;
	}

	public UMLType getSuperclass() {
		if(!superclassChanged && oldSuperclass != null && newSuperclass != null)
			return oldSuperclass;
		return null;
	}

	public UMLType getOldSuperclass() {
		return oldSuperclass;
	}

	public UMLType getNewSuperclass() {
		return newSuperclass;
	}

	public List<UMLType> getAddedImplementedInterfaces() {
		return addedImplementedInterfaces;
	}

	public List<UMLType> getRemovedImplementedInterfaces() {
		return removedImplementedInterfaces;
	}

	public Optional<Pair<UMLType, UMLType>> getImplementedInterfaceBecomesSuperclass() {
		return implementedInterfaceBecomesSuperclass;
	}

	public Optional<Pair<UMLType, UMLType>> getSuperclassBecomesImplementedInterface() {
		return superclassBecomesImplementedInterface;
	}

	public boolean containsOperationWithTheSameSignatureInOriginalClass(UMLOperation operation) {
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			if(originalOperation.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation))
				return true;
		}
		return false;
	}

	public boolean containsOperationWithTheSameSignatureInNextClass(UMLOperation operation) {
		for(UMLOperation originalOperation : nextClass.getOperations()) {
			if(originalOperation.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation))
				return true;
		}
		return false;
	}

	public boolean containsConcreteOperationWithTheSameSignatureInNextClass(UMLOperation operation) {
		for(UMLOperation originalOperation : nextClass.getOperations()) {
			if(originalOperation.getBody() != null && originalOperation.equalSignatureWithIdenticalNameIgnoringChangedTypes(operation))
				return true;
		}
		return false;
	}

	public UMLOperation containsAddedOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation addedOperation : addedOperations) {
			if(addedOperation.equalSignature(operation))
				return addedOperation;
		}
		return null;
	}

	public UMLOperation containsRemovedOperationWithTheSameSignature(UMLOperation operation) {
		for(UMLOperation removedOperation : removedOperations) {
			if(removedOperation.equalSignature(operation))
				return removedOperation;
		}
		return null;
	}

	public List<UMLOperation> removedOperationWithTheSameName(UMLOperation operation) {
		List<UMLOperation> matchingOperations = new ArrayList<>();
		for(UMLOperation removedOperation : removedOperations) {
			if(removedOperation.getName().equals(operation.getName()))
				matchingOperations.add(removedOperation);
		}
		return matchingOperations;
	}

	public UMLAttribute containsAddedAttributeWithTheSameSignature(UMLAttribute attribute) {
		for(UMLAttribute addedAttribute : addedAttributes) {
			if(addedAttribute.equalsIgnoringChangedVisibility(attribute))
				return addedAttribute;
		}
		return null;
	}

	public UMLAttribute containsRemovedAttributeWithTheSameSignature(UMLAttribute attribute) {
		for(UMLAttribute removedAttribute : removedAttributes) {
			if(removedAttribute.equalsIgnoringChangedVisibility(attribute))
				return removedAttribute;
		}
		return null;
	}

	public UMLAttribute containsRemovedAttributeWithTheSameNameIgnoringChangedType(UMLAttribute attribute) {
		for(UMLAttribute removedAttribute : removedAttributes) {
			if(removedAttribute.equalsIgnoringChangedType(attribute))
				return removedAttribute;
		}
		return null;
	}

	private void processInheritance() {
		if(originalClass.getSuperclass() != null && nextClass.getSuperclass() != null) {
			if(!originalClass.getSuperclass().equals(nextClass.getSuperclass())) {
				setSuperclassChanged(true);
			}
			else if(!originalClass.getSuperclass().equalsQualified(nextClass.getSuperclass())) {
				setSuperclassChanged(true);
			}
			if(!originalClass.getSuperclass().toString().equals(nextClass.getSuperclass().toString())) {
				setSuperclassChanged(true);
			}
			setOldSuperclass(originalClass.getSuperclass());
			setNewSuperclass(nextClass.getSuperclass());
		}
		else if(originalClass.getSuperclass() != null && nextClass.getSuperclass() == null) {
			setSuperclassChanged(true);
			setOldSuperclass(originalClass.getSuperclass());
			setNewSuperclass(nextClass.getSuperclass());
			if(nextClass.getImplementedInterfaces().contains(originalClass.getSuperclass())) {
				int index = nextClass.getImplementedInterfaces().indexOf(originalClass.getSuperclass());
				Pair<UMLType, UMLType> pair = Pair.of(originalClass.getSuperclass(), nextClass.getImplementedInterfaces().get(index));
				superclassBecomesImplementedInterface = Optional.of(pair);
			}
		}
		else if(originalClass.getSuperclass() == null && nextClass.getSuperclass() != null) {
			setSuperclassChanged(true);
			setOldSuperclass(originalClass.getSuperclass());
			setNewSuperclass(nextClass.getSuperclass());
			if(originalClass.getImplementedInterfaces().contains(nextClass.getSuperclass())) {
				int index = originalClass.getImplementedInterfaces().indexOf(nextClass.getSuperclass());
				Pair<UMLType, UMLType> pair = Pair.of(originalClass.getImplementedInterfaces().get(index), nextClass.getSuperclass());
				implementedInterfaceBecomesSuperclass = Optional.of(pair);
			}
		}
		for(UMLType implementedInterface : originalClass.getImplementedInterfaces()) {
			if(!nextClass.getImplementedInterfaces().contains(implementedInterface)) {
				boolean skip = false;
				if(implementedInterfaceBecomesSuperclass.isPresent()) {
					Pair<UMLType, UMLType> pair = implementedInterfaceBecomesSuperclass.get();
					if(pair.getLeft().equals(implementedInterface)) {
						skip = true;
					}
				}
				if(!skip) {
					reportRemovedImplementedInterface(implementedInterface);
				}
			}
		}
		for(UMLType implementedInterface : nextClass.getImplementedInterfaces()) {
			if(!originalClass.getImplementedInterfaces().contains(implementedInterface)) {
				boolean skip = false;
				if(superclassBecomesImplementedInterface.isPresent()) {
					Pair<UMLType, UMLType> pair = superclassBecomesImplementedInterface.get();
					if(pair.getRight().equals(implementedInterface)) {
						skip = true;
					}
				}
				if(!skip) {
					reportAddedImplementedInterface(implementedInterface);
				}
			}
		}
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

	private void checkForOperationSignatureChanges() throws RefactoringMinerTimedOutException {
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
					if(!containsMapperForOperation1(removedOperation) && !containsMapperForOperation2(addedOperation) &&
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
						if(bestMapper != null && !modelDiffContainsConflictingMoveOperationRefactoring(bestMapper)) {
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
							this.addOperationBodyMapper(bestMapper);
							consistentMethodInvocationRenames = findConsistentMethodInvocationRenames();
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
						if(addedOperation.hasParameterizedTestAnnotation() && !firstMapper.getContainer1().hasParameterizedTestAnnotation()) {
							Set<UMLOperationBodyMapper> filteredMapperSet = new LinkedHashSet<UMLOperationBodyMapper>();
							List<List<String>> parameterValues = getParameterValues(addedOperation);
							List<String> parameterNames = addedOperation.getParameterNameList();
							int overallMaxMatchingTestParameters = -1;
							for(UMLOperationBodyMapper mapper : mapperSet) {
								Map<Integer, Integer> matchingTestParameters = matchParamsWithReplacements(parameterValues, parameterNames, mapper.getReplacements());
								if (matchingTestParameters.isEmpty()) {
									matchingTestParameters = matchParamsWithRemovedStatements(parameterValues, parameterNames, mapper.getNonMappedLeavesT1());
								}
								int max = matchingTestParameters.isEmpty() ? 0 : Collections.max(matchingTestParameters.values());
								if(max >= 1 && (overallMaxMatchingTestParameters == -1 || max == overallMaxMatchingTestParameters)) {
									if(max > overallMaxMatchingTestParameters) {
										overallMaxMatchingTestParameters = max;
									}
									filteredMapperSet.add(mapper);
								}
							}
							//cluster mappers based on number of mappings and number of total replacements
							Set<UMLOperationBodyMapper> filteredMapperSet2 = new LinkedHashSet<UMLOperationBodyMapper>();
							int maxMappings = -1;
							int minReplacements = Integer.MAX_VALUE;
							for(UMLOperationBodyMapper mapper : filteredMapperSet) {
								int mappings = mapper.getMappings().size();
								if(mappings > maxMappings) {
									maxMappings = mappings;
								}
								int replacements = mapper.getReplacements().size();
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
								refactorings.addAll(mapper.getRefactoringsAfterPostProcessing());
								UMLOperation removedOperation = mapper.getOperation1();
								removedOperations.remove(removedOperation);
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
							if(bestMapper != null && !modelDiffContainsConflictingMoveOperationRefactoring(bestMapper)) {
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

	private List<List<String>> getParameterValues(UMLOperation addedOperation) {
		List<List<String>> parameterValues = new ArrayList<>();
		for(UMLAnnotation annotation : addedOperation.getAnnotations()) {
			try {
				List<List<String>> testParameters = SourceAnnotation.create(annotation, addedOperation, modelDiff.getChildModel()).getTestParameters();
				parameterValues.addAll(testParameters);
			} catch (IllegalArgumentException ignored) {/* Do nothing */}
		}
		return parameterValues;
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

	private static Map<Integer, Integer> matchParamsWithReplacements(List<List<String>> testParameters, List<String> parameterNames, Set<Replacement> replacements) {
		Map<Integer, Integer> matchingTestParameters = new LinkedHashMap<>();
		for(Replacement r : replacements) {
			if(parameterNames.contains(r.getAfter())) {
				String paramsWithoutDoubleQuotes = sanitizeStringLiteral(r.getBefore());
				for (int parameterRow = 0; parameterRow < testParameters.size(); parameterRow++) {
					if (testParameters.get(parameterRow).contains(paramsWithoutDoubleQuotes)) {
						Integer previousValue = matchingTestParameters.getOrDefault(parameterRow, 0);
						matchingTestParameters.put(parameterRow, previousValue + 1);
					}
				}
			}
		}
		return matchingTestParameters;
	}

	private static String sanitizeStringLiteral(String expression) {
		if (expression.startsWith("\"") && expression.endsWith("\"")) {
			return expression.substring(1, expression.length() - 1);
		} else if (expression.endsWith(".class")) {
			return expression.substring(0, expression.lastIndexOf(".class"));
		} else if (expression.contains(".")) {
			return expression.substring(expression.lastIndexOf('.') + 1);
		}
		return expression;
	}

	private void extractParametersFromCsvFile(List<List<String>> testParameters, String csvFile) {
		try {
			List<String> tests = readCsvFile(csvFile);
			for (String test : tests) {
				List<String> parameters = extractParametersFromCsv(test);
				testParameters.add(parameters);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> readCsvFile(String csvFile) throws IOException {
		List<String> parameters = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(csvFile));
		String line = br.readLine();
		while(line != null) {
			parameters.add(line);
			line = br.readLine();
		}
		br.close();
		return parameters;
	}

	private static List<String> extractParametersFromCsv(String s) {
		List<String> parameters = new ArrayList<>();
		String[] tokens = s.split(",");
		for(String token : tokens) {
			String trimmed = token.trim();
			if(trimmed.startsWith("\"")) {
				trimmed = trimmed.substring(1, trimmed.length());
			}
			if(trimmed.endsWith("\"")) {
				trimmed = trimmed.substring(0, trimmed.length()-1);
			}
			parameters.add(trimmed);
		}
		return parameters;
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
							int indexOfOpenCurlyBracket = commentSubString.indexOf(JAVA.OPEN_BLOCK);
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
							ASTNode methodBodyBlock = UMLModelASTReader.processBlock(methodBody);
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

	private void updateMapperSet(TreeSet<UMLOperationBodyMapper> mapperSet, UMLOperation removedOperation, UMLOperation addedOperation, int differenceInPosition) throws RefactoringMinerTimedOutException {
		boolean mapperWithZeroNonMappedStatementsOrIdenticalMethodName = false;
		for(UMLOperationBodyMapper mapper : mapperSet) {
			if(mapper.getContainer1().getBodyHashCode() == mapper.getContainer2().getBodyHashCode()) {
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
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		if(mappings > 0 || (delegatesToAnotherRemovedOperation(removedOperation) && addedOperation.getBody() != null && addedOperation.stringRepresentation().size() > 3) || (removedOperation.getName().equals(addedOperation.getName()) && removedOperation.getBody() != null && addedOperation.getBody() != null)) {
			boolean zeroNonMapped = operationBodyMapper.getNonMappedLeavesT1().size() == 0 && operationBodyMapper.getNonMappedLeavesT2().size() == 0 &&
					operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.getNonMappedInnerNodesT2().size() == 0 &&
					removedOperation.hasTestAnnotation() && addedOperation.hasTestAnnotation();
			int absoluteDifferenceInPosition = computeAbsoluteDifferenceInPositionWithinClass(removedOperation, addedOperation);
			if(exactMappings(operationBodyMapper) || (operationBodyMapper.allMappingsHaveSameDepthAndIndex() && !removedOperation.hasTestAnnotation() && !addedOperation.hasTestAnnotation())) {
				mapperSet.add(operationBodyMapper);
			}
			else if(mappedElementsMoreThanNonMappedT1AndT2(mappings, operationBodyMapper) &&
					(absoluteDifferenceInPosition <= differenceInPosition || zeroNonMapped) &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition) &&
					removedOperation.testMethodCheck(addedOperation)) {
				isPartOfMethodMovedFromDeletedMethod(removedOperation, addedOperation, operationBodyMapper);
				isPartOfMethodMovedToAddedMethod(removedOperation, addedOperation, operationBodyMapper);
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					(isPartOfMethodExtracted(removedOperation, addedOperation) || isPartOfMethodMovedToExistingMethod(removedOperation, addedOperation) ||
							(operationBodyMapper.exactMatches() > 0 && !mapperWithZeroNonMappedStatementsOrIdenticalMethodName && isPartOfMethodMovedToAddedMethod(removedOperation, addedOperation, operationBodyMapper))) &&
					removedOperation.testMethodCheck(addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					(isPartOfMethodInlined(removedOperation, addedOperation) || isPartOfMethodMovedFromExistingMethod(removedOperation, addedOperation) ||
							(operationBodyMapper.exactMatches() > 0 && !mapperWithZeroNonMappedStatementsOrIdenticalMethodName && isPartOfMethodMovedFromDeletedMethod(removedOperation, addedOperation, operationBodyMapper))) &&
					removedOperation.testMethodCheck(addedOperation)) {
				mapperSet.add(operationBodyMapper);
			}
			else {
				if((removedOperation.hasSetUpAnnotation() || removedOperation.getName().equals("setUp")) && (addedOperation.hasSetUpAnnotation() || addedOperation.getName().equals("setUp"))) {
					potentialCodeMoveBetweenSetUpTearDownMethods.add(operationBodyMapper);
				}
				else if((removedOperation.hasTearDownAnnotation() || removedOperation.getName().equals("tearDown")) && (addedOperation.hasTearDownAnnotation() || addedOperation.getName().equals("tearDown"))) {
					potentialCodeMoveBetweenSetUpTearDownMethods.add(operationBodyMapper);
				}
				else if(allStatementsMappedOrParameterized(operationBodyMapper)) {
					movedMethodsInDifferentPositionWithinFile.add(operationBodyMapper);
				}
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
		else {
			for(MethodInvocationReplacement replacement : consistentMethodInvocationRenames.keySet()) {
				UMLOperationBodyMapper mapper = consistentMethodInvocationRenames.get(replacement);
				if(replacement.getInvokedOperationBefore().matchesOperation(removedOperation, mapper.getContainer1(), this, modelDiff) &&
						replacement.getInvokedOperationAfter().matchesOperation(addedOperation, mapper.getContainer2(), this, modelDiff)) {
					mapperSet.add(operationBodyMapper);
					break;
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
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
		}
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
					absoluteDifferenceInPosition <= differenceInPosition &&
					compatibleSignatures(removedOperation, addedOperation, absoluteDifferenceInPosition)) {
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT2(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
					(isPartOfMethodExtracted(removedOperation, addedOperation) || isPartOfMethodMovedToExistingMethod(removedOperation, addedOperation))) {
				mapperSet.add(operationBodyMapper);
			}
			else if(removedOperation.isConstructor() == addedOperation.isConstructor() &&
					mappedElementsMoreThanNonMappedT1(mappings, operationBodyMapper) &&
					absoluteDifferenceInPosition <= differenceInPosition &&
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
				for(String parameterName : operationBodyMapper.getContainer2().getParameterNameList()) {
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
				for(String parameterName : operationBodyMapper.getContainer2().getParameterNameList()) {
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
				for(String parameterName : operationBodyMapper.getContainer2().getParameterNameList()) {
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
						if(fragment1.getString().equals(JAVA.RETURN_STATEMENT)) {
							returnFound = true;
							break;
						}
					}
				}
				if(!returnFound) {
					for(AbstractCodeFragment statement2 : operationBodyMapper.getNonMappedLeavesT2()) {
						if(statement2.getVariables().size() > 0 && statement2.getString().equals(JAVA.RETURN_SPACE + statement2.getVariables().get(0).getString() + JAVA.STATEMENT_TERMINATION)) {
							VariableDeclaration variableDeclaration2 = operationBodyMapper.getContainer2().getVariableDeclaration(statement2.getVariables().get(0).getString());
							if(variableDeclaration2 != null && variableDeclaration2.getType() != null && variableDeclaration2.getType().equals(returnType2)) {
								nonMappedElementsT2--;
							}
						}
					}
				}
			}
		}
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if(mapping.isIdenticalWithInlinedVariable() || mapping.isIdenticalWithExtractedVariable()) {
				for(Refactoring r : mapping.getRefactorings()) {
					if(r instanceof InlineVariableRefactoring) {
						nonMappedElementsT1--;
					}
					if(r instanceof ExtractVariableRefactoring) {
						nonMappedElementsT2--;
					}
				}
			}
		}
		return (mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
				(nonMappedElementsT1 == 0 && mappings > Math.floor(nonMappedElementsT2/2.0) && !operationBodyMapper.involvesTestMethods()) ||
				(nonMappedElementsT2 == 0 && mappings > Math.floor(nonMappedElementsT1/2.0) && !operationBodyMapper.involvesTestMethods() && !(this instanceof UMLClassMoveDiff)) ||
				(nonMappedElementsT1 == 0 && exactMappings >= Math.floor(nonMappedElementsT2/2.0) && operationBodyMapper.getContainer1().isConstructor() == operationBodyMapper.getContainer2().isConstructor()) ||
				(nonMappedElementsT2 == 0 && exactMappings > 0 && exactMappings >= Math.floor(nonMappedElementsT1/2.0) && operationBodyMapper.getNonMappedInnerNodesT1().size() == 0 && operationBodyMapper.getContainer1().isConstructor() == operationBodyMapper.getContainer2().isConstructor()) ||
				(nonMappedElementsT2 == 0 && exactMappings > 0 && operationBodyMapper.getContainer1().getParameterNameList().size() > 0 &&
					operationBodyMapper.getContainer1().getParameterNameList().equals(operationBodyMapper.getContainer2().getParameterNameList()) &&
					operationBodyMapper.getContainer1().getParameterTypeList().equals(operationBodyMapper.getContainer2().getParameterTypeList()) &&
					operationBodyMapper.getContainer1().isConstructor() == operationBodyMapper.getContainer2().isConstructor()) ||
				(mappings == 1 && nonMappedElementsT1 + nonMappedElementsT2 == 1 && operationBodyMapper.getContainer1().getName().equals(operationBodyMapper.getContainer2().getName()));
	}

	private boolean mappedElementsMoreThanNonMappedT2(int mappings, UMLOperationBodyMapper operationBodyMapper) {
		int nonMappedElementsT2 = operationBodyMapper.nonMappedElementsT2();
		int nonMappedElementsT2CallingAddedOperation = operationBodyMapper.nonMappedElementsT2CallingAddedOperation(addedOperations);
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
		return mappings > nonMappedElementsT2 || (mappings >= nonMappedElementsT2WithoutThoseCallingAddedOperation &&
				nonMappedElementsT2CallingAddedOperation >= nonMappedElementsT2WithoutThoseCallingAddedOperation) ||
				(operationBodyMapper.getMappings().size() > nonMappedElementsT2 && nonMappedElementsT2CallingAddedOperation > 0 &&
						operationBodyMapper.getContainer1().getClassName().equals(operationBodyMapper.getContainer2().getClassName()));
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
		boolean identicalBodyWithOperation1OfTheBestMapper = identicalBodyWithAnotherAddedMethod(bestMapper);
		boolean identicalBodyWithOperation2OfTheBestMapper = identicalBodyWithAnotherRemovedMethod(bestMapper);
		if(equalSignatureWithCommonParameterTypes(bestMapperOperation1, bestMapperOperation2) &&
				!identicalBodyWithOperation1OfTheBestMapper && !identicalBodyWithOperation2OfTheBestMapper) {
			return bestMapper;
		}
		for(int i=1; i<mapperList.size(); i++) {
			UMLOperationBodyMapper mapper = mapperList.get(i);
			if(checkForCalls(mapper)) {
				VariableDeclarationContainer operation2 = mapper.getContainer2();
				List<AbstractCall> operationInvocations2 = operation2.getAllOperationInvocations();
				boolean anotherMapperCallsOperation2OfTheBestMapper = false;
				for(AbstractCall invocation : operationInvocations2) {
					if(invocation.matchesOperation(bestMapper.getContainer2(), operation2, this, modelDiff) && !invocation.matchesOperation(bestMapper.getContainer1(), operation2, this, modelDiff) &&
							!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, removedOperations)) {
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
							break;
						}
					}
				}
				VariableDeclarationContainer operation1 = mapper.getContainer1();
				List<AbstractCall> operationInvocations1 = operation1.getAllOperationInvocations();
				boolean anotherMapperCallsOperation1OfTheBestMapper = false;
				for(AbstractCall invocation : operationInvocations1) {
					if(invocation.matchesOperation(bestMapper.getContainer1(), operation1, this, modelDiff) && !invocation.matchesOperation(bestMapper.getContainer2(), operation1, this, modelDiff) &&
							!operationContainsMethodInvocationWithTheSameNameAndCommonArguments(invocation, addedOperations)) {
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
		int consistentMethodInvocationRenameMismatchesForBestMapper = mismatchesConsistentMethodInvocationRename(bestMapper);
		if(consistentMethodInvocationRenameMismatchesForBestMapper > 0 && !exactMappings(bestMapper)) {
			for(int i=1; i<mapperList.size(); i++) {
				UMLOperationBodyMapper mapper = mapperList.get(i);
				int consistentMethodInvocationRenameMismatchesForCurrentMapper = mismatchesConsistentMethodInvocationRename(mapper);
				if(consistentMethodInvocationRenameMismatchesForCurrentMapper < consistentMethodInvocationRenameMismatchesForBestMapper) {
					return mapper;
				}
			}
			return null;
		}
		if(identicalBodyWithOperation2OfTheBestMapper || identicalBodyWithOperation1OfTheBestMapper) {
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
					if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode()) {
						return true;
					}
					else if(equalSignatureWithCommonParameterTypes(operation1, addedOperation)) {
						List<String> commonStatements = new ArrayList<String>();
						List<String> addedOperationStringRepresentation = addedOperation.stringRepresentation();
						for(String statement : addedOperationStringRepresentation) {
							if(!statement.equals(JAVA.OPEN_BLOCK) && !statement.equals(JAVA.CLOSE_BLOCK) && !statement.equals(JAVA.TRY) && !statement.startsWith("catch(") && !statement.startsWith(JAVA.CASE_SPACE) && !statement.startsWith("default :") &&
									!statement.equals(JAVA.RETURN_TRUE) && !statement.equals(JAVA.RETURN_FALSE) && !statement.equals(JAVA.RETURN_THIS) && !statement.equals(JAVA.RETURN_NULL) && !statement.equals(JAVA.RETURN_STATEMENT)) {
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
						if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation1.getParameterNameList())) {
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
					if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode() &&
							parameterNameList.size() > 0 && parameterNameList.equals(operation1.getParameterNameList())) {
						counter++;
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass addedClass : modelDiff.getAddedClasses()) {
					for(UMLOperation addedOperation : addedClass.getOperations()) {
						OperationBody body = addedOperation.getBody();
						List<String> parameterNameList = addedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation1.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation1.getParameterNameList())) {
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
					if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode()) {
						return true;
					}
					else if(equalSignatureWithCommonParameterTypes(removedOperation, operation2)) {
						List<String> commonStatements = new ArrayList<String>();
						List<String> removedOperationStringRepresentation = removedOperation.stringRepresentation();
						for(String statement : removedOperationStringRepresentation) {
							if(!statement.equals(JAVA.OPEN_BLOCK) && !statement.equals(JAVA.CLOSE_BLOCK) && !statement.equals(JAVA.TRY) && !statement.startsWith("catch(") && !statement.startsWith(JAVA.CASE_SPACE) && !statement.startsWith("default :") &&
									!statement.equals(JAVA.RETURN_TRUE) && !statement.equals(JAVA.RETURN_FALSE) && !statement.equals(JAVA.RETURN_THIS) && !statement.equals(JAVA.RETURN_NULL) && !statement.equals(JAVA.RETURN_STATEMENT)) {
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
						if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation2.getParameterNameList())) {
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
					if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode() &&
							parameterNameList.size() > 0 && parameterNameList.equals(operation2.getParameterNameList())) {
						counter++;
					}
				}
			}
			if(nextClass.hasDeprecatedAnnotation() != originalClass.hasDeprecatedAnnotation()) {
				for(UMLClass removedClass : modelDiff.getRemovedClasses()) {
					for(UMLOperation removedOperation : removedClass.getOperations()) {
						OperationBody body = removedOperation.getBody();
						List<String> parameterNameList = removedOperation.getParameterNameList();
						if(body != null && body.getBodyHashCode() == operation2.getBody().getBodyHashCode() &&
								parameterNameList.size() > 0 && parameterNameList.equals(operation2.getParameterNameList())) {
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

	private int mismatchesConsistentMethodInvocationRename(UMLOperationBodyMapper mapper) {
		int mismatchCount = 0;
		for(MethodInvocationReplacement rename : consistentMethodInvocationRenames.keySet()) {
			UMLOperationBodyMapper referringMapper = consistentMethodInvocationRenames.get(rename);
			AbstractCall callBefore = rename.getInvokedOperationBefore();
			AbstractCall callAfter = rename.getInvokedOperationAfter();
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
		if(absoluteDifferenceInPosition == 0 || operationsBeforeAndAfterMatch(removedOperation, addedOperation)) {
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
				if(attribute.getName().equals(variables1.get(0)) || variables1.get(0).equals(JAVA.THIS_DOT + attribute.getName())) {
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
				if(attribute.getName().equals(variables2.get(0)) || variables2.get(0).equals(JAVA.THIS_DOT + attribute.getName())) {
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
					|| (matchingDataProviderAnnotation(removedOperation, operationBefore1) && matchingDataProviderAnnotation(addedOperation, operationBefore2));
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

	private void checkForInlinedOperationsToExtractedOperations(List<UMLOperationBodyMapper> extractedBodyMappers) throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> inlinedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLOperationBodyMapper mapper : extractedBodyMappers) {
			InlineOperationDetection detection = new InlineOperationDetection(mapper, removedOperations, this, modelDiff);
			List<UMLOperation> sortedRemovedOperations = detection.getRemovedOperationsSortedByCalls();
			for(UMLOperation removedOperation : sortedRemovedOperations) {
				List<InlineOperationRefactoring> refs = detection.check(removedOperation);
				for(InlineOperationRefactoring refactoring : refs) {
					refactorings.add(refactoring);
					UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
					inlinedOperationMappers.add(operationBodyMapper);
					mapper.addChildMapper(operationBodyMapper);
					operationsToBeRemoved.add(removedOperation);
				}
			}
		}
		for(UMLOperationBodyMapper operationBodyMapper : inlinedOperationMappers) {
			processMapperRefactorings(operationBodyMapper, refactorings);
		}
		removedOperations.removeAll(operationsToBeRemoved);
	}

	private void checkForInlinedOperations() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> inlinedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			InlineOperationDetection detection = new InlineOperationDetection(mapper, removedOperations, this, modelDiff);
			List<UMLOperation> sortedRemovedOperations = detection.getRemovedOperationsSortedByCalls();
			for(UMLOperation removedOperation : sortedRemovedOperations) {
				List<InlineOperationRefactoring> refs = detection.check(removedOperation);
				for(InlineOperationRefactoring refactoring : refs) {
					refactorings.add(refactoring);
					UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
					inlinedOperationMappers.add(operationBodyMapper);
					mapper.addChildMapper(operationBodyMapper);
					operationsToBeRemoved.add(removedOperation);
				}
			}
		}
		Set<Refactoring> refactoringsToBeAdded = new LinkedHashSet<Refactoring>();
		for(Refactoring r : refactorings) {
			if(r instanceof MergeOperationRefactoring) {
				MergeOperationRefactoring merge = (MergeOperationRefactoring)r;
				for(UMLOperationBodyMapper mapper : merge.getMappers()) {
					InlineOperationDetection detection = new InlineOperationDetection(mapper, removedOperations, this, modelDiff);
					List<UMLOperation> sortedRemovedOperations = detection.getRemovedOperationsSortedByCalls();
					for(UMLOperation removedOperation : sortedRemovedOperations) {
						List<InlineOperationRefactoring> refs = detection.check(removedOperation);
						for(InlineOperationRefactoring refactoring : refs) {
							refactoringsToBeAdded.add(refactoring);
							UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
							inlinedOperationMappers.add(operationBodyMapper);
							mapper.addChildMapper(operationBodyMapper);
							operationsToBeRemoved.add(removedOperation);
						}
					}
				}
			}
		}
		refactorings.addAll(refactoringsToBeAdded);
		MappingOptimizer optimizer = new MappingOptimizer(this);
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			optimizer.optimizeDuplicateMappingsForInline(mapper, refactorings);
		}
		for(UMLOperationBodyMapper operationBodyMapper : inlinedOperationMappers) {
			processMapperRefactorings(operationBodyMapper, refactorings);
		}
		removedOperations.removeAll(operationsToBeRemoved);
	}

	private List<UMLOperationBodyMapper> checkForExtractedOperationsWithCallsInOtherMappers() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> extractedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		Set<UMLOperationBodyMapper> parentMappersToBeOptimized = new LinkedHashSet<UMLOperationBodyMapper>();
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if((!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty()) && mapper.getChildMappers().size() == 0) {
				ExtractOperationDetection detection = new ExtractOperationDetection(mapper, addedOperations, this, modelDiff, true);
				List<UMLOperation> sortedAddedOperations = detection.getAddedOperationsSortedByCalls();
				for(UMLOperation addedOperation : sortedAddedOperations) {
					List<ExtractOperationRefactoring> refs = detection.check(addedOperation);
					for(ExtractOperationRefactoring refactoring : refs) {
						UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
						if(operationBodyMapper.exactMatches() > 1) {
							refactorings.add(refactoring);
							extractedOperationMappers.add(operationBodyMapper);
							mapper.addChildMapper(operationBodyMapper);
							parentMappersToBeOptimized.add(mapper);
							operationsToBeRemoved.add(addedOperation);
						}
					}
				}
			}
		}
		MappingOptimizer optimizer = new MappingOptimizer(this);
		for(UMLOperationBodyMapper mapper : parentMappersToBeOptimized) {
			optimizer.optimizeDuplicateMappingsForExtract(mapper, refactorings);
		}
		for(UMLOperationBodyMapper operationBodyMapper : extractedOperationMappers) {
			processMapperRefactorings(operationBodyMapper, refactorings);
		}
		addedOperations.removeAll(operationsToBeRemoved);
		return extractedOperationMappers;
	}

	private List<UMLOperationBodyMapper> checkForExtractedOperations() throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> extractedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			ExtractOperationDetection detection = new ExtractOperationDetection(mapper, addedOperations, this, modelDiff);
			List<UMLOperation> sortedAddedOperations = detection.getAddedOperationsSortedByCalls();
			for(UMLOperation addedOperation : sortedAddedOperations) {
				List<ExtractOperationRefactoring> refs = detection.check(addedOperation);
				List<ExtractOperationRefactoring> discarded = new ArrayList<>();
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
				if(discarded.equals(refs)) {
					discarded.clear();
				}
				for(ExtractOperationRefactoring refactoring : refs) {
					if(!discarded.contains(refactoring)) {
						CompositeStatementObject synchronizedBlock = refactoring.extractedFromSynchronizedBlock();
						if(synchronizedBlock != null) {
							refactoring.getBodyMapper().getParentMapper().getNonMappedInnerNodesT1().remove(synchronizedBlock);
						}
						refactorings.add(refactoring);
						UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
						extractedOperationMappers.add(operationBodyMapper);
						mapper.addChildMapper(operationBodyMapper);
						operationsToBeRemoved.add(addedOperation);
					}
				}
			}
		}
		if(extractedOperationMappers.size() > 0) {
			MappingOptimizer optimizer = new MappingOptimizer(this);
			for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
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

	public boolean isEmpty() {
		return addedOperations.isEmpty() && removedOperations.isEmpty() &&
			addedAttributes.isEmpty() && removedAttributes.isEmpty() &&
			addedEnumConstants.isEmpty() && removedEnumConstants.isEmpty() &&
			attributeDiffList.isEmpty() &&
			operationBodyMapperList.isEmpty() && enumConstantDiffList.isEmpty() && annotationListDiff.isEmpty() && typeParameterDiffList.isEmpty() &&
			!visibilityChanged && !abstractionChanged && !finalChanged && !staticChanged && !superclassChanged;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(originalClass.getName()).append(":").append("\n");
		if(visibilityChanged) {
			sb.append("\t").append("visibility changed from " + oldVisibility + " to " + newVisibility).append("\n");
		}
		if(abstractionChanged) {
			sb.append("\t").append("abstraction changed from " + (oldAbstraction ? "abstract" : "concrete") + " to " +
					(newAbstraction ? "abstract" : "concrete")).append("\n");
		}
		Collections.sort(removedOperations);
		for(UMLOperation umlOperation : removedOperations) {
			sb.append("operation " + umlOperation + " removed").append("\n");
		}
		Collections.sort(addedOperations);
		for(UMLOperation umlOperation : addedOperations) {
			sb.append("operation " + umlOperation + " added").append("\n");
		}
		Collections.sort(removedAttributes);
		for(UMLAttribute umlAttribute : removedAttributes) {
			sb.append("attribute " + umlAttribute + " removed").append("\n");
		}
		Collections.sort(addedAttributes);
		for(UMLAttribute umlAttribute : addedAttributes) {
			sb.append("attribute " + umlAttribute + " added").append("\n");
		}
		for(UMLAttributeDiff attributeDiff : attributeDiffList) {
			sb.append(attributeDiff);
		}
		Collections.sort(operationBodyMapperList);
		for(UMLOperationBodyMapper operationBodyMapper : operationBodyMapperList) {
			sb.append(operationBodyMapper).append("\n");
		}
		return sb.toString();
	}

	public int compareTo(UMLClassBaseDiff other) {
		if(!this.originalClass.getName().equals(other.originalClass.getName()))
			return this.originalClass.getName().compareTo(other.originalClass.getName());
		else
			return this.nextClass.getName().compareTo(other.nextClass.getName());
	}

	public boolean samePackage() {
		return originalClass.getPackageName().equals(nextClass.getPackageName());
	}

	protected boolean differentParameterNames(UMLOperation operation1, UMLOperation operation2) {
		if(operation1 != null && operation2 != null && !operation1.getParameterNameList().equals(operation2.getParameterNameList())) {
			int methodsWithIdenticalName1 = 0;
			for(UMLOperation operation : originalClass.getOperations()) {
				if(operation != operation1 && operation.getName().equals(operation1.getName()) && !operation.hasVarargsParameter()) {
					methodsWithIdenticalName1++;
				}
			}
			int methodsWithIdenticalName2 = 0;
			for(UMLOperation operation : nextClass.getOperations()) {
				if(operation != operation2 && operation.getName().equals(operation2.getName()) && !operation.hasVarargsParameter()) {
					methodsWithIdenticalName2++;
				}
			}
			if(methodsWithIdenticalName1 > 0 && methodsWithIdenticalName2 > 0) {
				return true;
			}
		}
		return false;
	}

	private boolean containCallToOperation(VariableDeclarationContainer calledOperation, VariableDeclarationContainer callerOperation) {
		for(AbstractCall invocation : callerOperation.getAllOperationInvocations()) {
			if(invocation.matchesOperation(calledOperation, callerOperation, this, modelDiff)) {
				return true;
			}
		}
		return false;
	}

	protected void createBodyMappers() throws RefactoringMinerTimedOutException {
		List<UMLOperation> removedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperation> addedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			for(UMLOperation nextOperation : nextClass.getOperations()) {
				if(originalOperation.equalsQualified(nextOperation) && !differentParameterNames(originalOperation, nextOperation)) {
					if(getModelDiff() != null) {
						List<UMLOperationBodyMapper> mappers = getModelDiff().findMappersWithMatchingSignature2(nextOperation);
						if(mappers.size() > 0) {
							UMLOperation operation1 = mappers.get(0).getOperation1();
							if(!operation1.equalSignature(originalOperation) &&
									getModelDiff().commonlyImplementedOperations(operation1, nextOperation, this)) {
								if(!removedOperations.contains(originalOperation)) {
									removedOperations.add(originalOperation);
								}
								break;
							}
						}
					}
					boolean matchFound = removedOrAddedOperationWithIdenticalBody(originalOperation, nextOperation, removedOperationsToBeRemoved, addedOperationsToBeRemoved);
					if(!matchFound) {
			    		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(originalOperation, nextOperation, this);
			    		this.addOperationBodyMapper(operationBodyMapper);
					}
				}
			}
		}
		for(UMLOperation operation : originalClass.getOperations()) {
			int index = nextClass.getOperations().indexOf(operation);
			if(!containsMapperForOperation1(operation) && index != -1 && !removedOperations.contains(operation) && !differentParameterNames(operation, nextClass.getOperations().get(index))) {
				int lastIndex = nextClass.getOperations().lastIndexOf(operation);
				int finalIndex = index;
				if(index != lastIndex) {
					if(containsMapperForOperation2(nextClass.getOperations().get(index))) {
						finalIndex = lastIndex;
					}
					else if(!operation.isConstructor()) {
	    				double d1 = operation.getReturnParameter().getType().normalizedNameDistance(nextClass.getOperations().get(index).getReturnParameter().getType());
	    				double d2 = operation.getReturnParameter().getType().normalizedNameDistance(nextClass.getOperations().get(lastIndex).getReturnParameter().getType());
	    				if(d2 < d1) {
	    					finalIndex = lastIndex;
	    				}
					}
				}
				UMLOperation nextOperation = nextClass.getOperations().get(finalIndex);
				boolean matchFound = removedOrAddedOperationWithIdenticalBody(operation, nextOperation, removedOperationsToBeRemoved, addedOperationsToBeRemoved);
				if(!matchFound) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(operation, nextOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
				}
			}
		}
		for(UMLOperation removedOperation : removedOperations) {
			for(UMLOperation addedOperation : addedOperations) {
				if(removedOperation.equalsIgnoringVisibility(addedOperation) && !differentParameterNames(removedOperation, addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
				else if(removedOperation.equalsIgnoringAbstraction(addedOperation) && !differentParameterNames(removedOperation, addedOperation) && !containsMapperForOperation1(removedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
				}
				else if(removedOperation.equalsIgnoringNameCase(addedOperation) && !differentParameterNames(removedOperation, addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					if(!removedOperation.getName().equals(addedOperation.getName()) &&
							!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
						RenameOperationRefactoring rename = new RenameOperationRefactoring(operationBodyMapper, new HashSet<MethodInvocationReplacement>());
						refactorings.add(rename);
					}
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
				else if(removedOperation.equalsIgoringTypeParameters(addedOperation) && !differentParameterNames(removedOperation, addedOperation) &&
						removedOperations.size() == addedOperations.size()) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
				else if(removedOperation.equalSignatureWithIdenticalNameIgnoringChangedTypesToFromObject(addedOperation) && !differentParameterNames(removedOperation, addedOperation) &&
						removedOperations.size() == addedOperations.size() && !mapperListContainsOperation(removedOperation, addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
			}
		}
		removedOperations.removeAll(removedOperationsToBeRemoved);
		addedOperations.removeAll(addedOperationsToBeRemoved);
		for(UMLOperation removedOperation : removedOperations) {
			if(!removedOperation.getAnnotations().isEmpty() && this.getOriginalClass().uniqueAnnotation(removedOperation)) {
				for(UMLOperation addedOperation : addedOperations) {
					if(!addedOperation.getAnnotations().isEmpty() && this.getNextClass().uniqueAnnotation(addedOperation)) {
						if(removedOperation.getAnnotations().equals(addedOperation.getAnnotations()) && removedOperation.hasEmptyBody() && addedOperation.hasEmptyBody()) {
							UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
							this.addOperationBodyMapper(operationBodyMapper);
							UMLOperationDiff operationSignatureDiff = operationBodyMapper.getOperationSignatureDiff().get();
							refactorings.addAll(operationSignatureDiff.getRefactorings());
							if(!removedOperation.getName().equals(addedOperation.getName()) &&
									!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
								RenameOperationRefactoring rename = new RenameOperationRefactoring(removedOperation, addedOperation);
								refactorings.add(rename);
							}
							removedOperationsToBeRemoved.add(removedOperation);
							addedOperationsToBeRemoved.add(addedOperation);
						}
					}
				}
			}
		}
		removedOperations.removeAll(removedOperationsToBeRemoved);
		addedOperations.removeAll(addedOperationsToBeRemoved);
	}

	private boolean removedOrAddedOperationWithIdenticalBody(UMLOperation originalOperation, UMLOperation nextOperation, List<UMLOperation> removedOperationsToBeRemoved, List<UMLOperation> addedOperationsToBeRemoved) throws RefactoringMinerTimedOutException {
		List<String> nextOperationStringRepresentation = nextOperation.stringRepresentation();
		List<String> originalOperationStringRepresentation = originalOperation.stringRepresentation();
		if(!nextOperationStringRepresentation.equals(originalOperationStringRepresentation) && nextOperationStringRepresentation.size() > 2) {
			for(UMLOperation removedOperation : removedOperations) {
				if(removedOperation.stringRepresentation().equals(nextOperationStringRepresentation) && !containCallToOperation(removedOperation, originalOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, nextOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					if(!removedOperation.getName().equals(nextOperation.getName()) &&
							!(removedOperation.isConstructor() && nextOperation.isConstructor())) {
						RenameOperationRefactoring rename = new RenameOperationRefactoring(operationBodyMapper, new HashSet<MethodInvocationReplacement>());
						refactorings.add(rename);
					}
					removedOperationsToBeRemoved.add(removedOperation);
					if(!removedOperations.contains(originalOperation)) {
						removedOperations.add(originalOperation);
					}
					return true;
				}
			}
		}
		if(!originalOperationStringRepresentation.equals(nextOperationStringRepresentation) && originalOperationStringRepresentation.size() > 2) {
			for(UMLOperation addedOperation : addedOperations) {
				if(addedOperation.stringRepresentation().equals(originalOperationStringRepresentation) && !containCallToOperation(addedOperation, nextOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(originalOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					if(!originalOperation.getName().equals(addedOperation.getName()) &&
							!(originalOperation.isConstructor() && addedOperation.isConstructor())) {
						RenameOperationRefactoring rename = new RenameOperationRefactoring(operationBodyMapper, new HashSet<MethodInvocationReplacement>());
						refactorings.add(rename);
					}
					addedOperationsToBeRemoved.add(addedOperation);
					if(!addedOperations.contains(nextOperation)) {
						addedOperations.add(nextOperation);
					}
					return true;
				}
			}
		}
		return false;
	}

	protected boolean containsMapperForOperation1(UMLOperation operation) {
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if(mapper.getOperation1() != null && mapper.getOperation1().equalsQualified(operation)) {
				return true;
			}
		}
		return false;
	}

	protected boolean containsMapperForOperation2(UMLOperation operation) {
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if(mapper.getOperation2() != null && mapper.getOperation2().equalsQualified(operation)) {
				return true;
			}
		}
		return false;
	}
}