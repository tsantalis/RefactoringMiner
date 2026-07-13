package gr.uom.java.xmi.diff;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.FunctionType;
import gr.uom.java.xmi.ListCompositeType;
import gr.uom.java.xmi.ModuleContainer;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.Visibility;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.VariableReplacementAnalysis;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.MoveCodeRefactoring.Type;

public abstract class UMLClassBaseDiff extends UMLAbstractClassDiff implements Comparable<UMLClassBaseDiff> {

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
	private Optional<Pair<UMLType, UMLType>> implementedInterfaceBecomesSuperclass;
	private Optional<Pair<UMLType, UMLType>> superclassBecomesImplementedInterface;
	private Optional<Pair<UMLType, UMLType>> commonFunctionType;
	private Optional<UMLJavadocDiff> javadocDiff;
	private Optional<UMLJavadocDiff> packageDeclarationJavadocDiff;
	private Optional<UMLParameterListDiff> primaryConstructorParameterListDiff;
	private Optional<UMLTypeAliasListDiff> typeAliasListDiff;
	private Optional<UMLNamedExportListDiff> namedExportListDiff;
	private Optional<UMLPreprocessorStatementListDiff> preprocessorStatementListDiff;
	private UMLCommentListDiff packageDeclarationCommentListDiff;

	public UMLClassBaseDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
		super(originalClass, nextClass, modelDiff);
		this.visibilityChanged = false;
		this.abstractionChanged = false;
		this.superclassChanged = false;
		this.addedImplementedInterfaces = new ArrayList<UMLType>();
		this.removedImplementedInterfaces = new ArrayList<UMLType>();
		this.implementedInterfaceBecomesSuperclass = Optional.empty();
		this.superclassBecomesImplementedInterface = Optional.empty();
		this.commonFunctionType = Optional.empty();
		this.namedExportListDiff = Optional.empty();
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
		this.preprocessorStatementListDiff = Optional.empty();
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

	public Optional<UMLParameterListDiff> getPrimaryConstructorParameterListDiff() {
		return primaryConstructorParameterListDiff;
	}

	public Optional<UMLTypeAliasListDiff> getTypeAliasListDiff() {
		return typeAliasListDiff;
	}

	public Optional<UMLNamedExportListDiff> getNamedExportListDiff() {
		return namedExportListDiff;
	}

	public UMLCommentListDiff getPackageDeclarationCommentListDiff() {
		if(packageDeclarationCommentListDiff == null)
			packageDeclarationCommentListDiff = new UMLCommentListDiff(getOriginalClass().getPackageDeclarationComments(), getNextClass().getPackageDeclarationComments());
		return packageDeclarationCommentListDiff;
	}

	public Map<MethodInvocationReplacement, UMLOperationBodyMapper> getConsistentMethodInvocationRenames() {
		return consistentMethodInvocationRenames;
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
		processPrimaryConstructors();
		if(getOriginalClass().getFunctionType().isPresent() && getNextClass().getFunctionType().isPresent()) {
			UMLType type1 = getOriginalClass().getFunctionType().get();
			UMLType type2 = getNextClass().getFunctionType().get();
			if(type1.equals(type2)) {
				this.commonFunctionType = Optional.of(Pair.of(type1, type2));
			}
			else if(type1 instanceof FunctionType functionType1 && type2 instanceof FunctionType functionType2) {
				this.commonFunctionType = Optional.of(Pair.of(functionType1, functionType2));
			}
			else if(type1 instanceof ListCompositeType listType1 && type2 instanceof ListCompositeType listType2) {
				List<UMLType> types1 = listType1.getTypes();
				List<UMLType> types2 = listType2.getTypes();
				if(types1.containsAll(types2) || types2.containsAll(types1)) {
					this.commonFunctionType = Optional.of(Pair.of(type1, type2));
				}
			}
		}
		if(getOriginalClass().getTypeAliasList().size() > 0 && getNextClass().getTypeAliasList().size() > 0) {
			UMLTypeAliasListDiff typeAliasListDiff = new UMLTypeAliasListDiff(getOriginalClass().getTypeAliasList(), getNextClass().getTypeAliasList());
			this.typeAliasListDiff = Optional.of(typeAliasListDiff);
		}
		else {
			this.typeAliasListDiff = Optional.empty();
		}
		processInitializers();
		processModifiers();
		processTypeParameters();
		processEnumConstants();
		processInheritance();
		processOperations();
		createBodyMappers();
		processAnonymousClasses();
		checkForOperationSignatureChanges(this.removedOperations, this.addedOperations);
		if(removedNestedOperations.size() > 0 || addedNestedOperations.size() > 0) {
			checkForOperationSignatureChanges(this.removedNestedOperations, this.addedNestedOperations);
			if(removedNestedOperations.size() == addedOperations.size() && addedNestedOperations.size() == 0) {
				checkForOperationSignatureChanges(this.removedNestedOperations, this.addedOperations);
			}
		}
		processAttributes();
		checkForAttributeChanges();
		checkForInlinedOperations();
		List<UMLOperationBodyMapper> extractedbodyMappers = checkForExtractedOperations();
		checkForExtractedOperationsWithCallsInOtherMappers();
		checkForInlinedOperationsToExtractedOperations(extractedbodyMappers);
		checkForMovedCodeBetweenOperations();
		checkForMovedAnnotations();
		if(originalClass.getContainer().isPresent() && nextClass.getContainer().isPresent()) {
			ModuleContainer container1 = originalClass.getContainer().get();
			ModuleContainer container2 = nextClass.getContainer().get();
			UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(container1, container2, this);
			addOperationBodyMapper(mapper);
			if(container1.getNamedExports().size() > 0 || container2.getNamedExports().size() > 0) {
				UMLNamedExportListDiff diff = new UMLNamedExportListDiff(container1.getNamedExports(), container2.getNamedExports());
				this.namedExportListDiff = Optional.of(diff);
			}
		}
		if(getOriginalClass().getPreprocessorStatements().size() > 0 && getNextClass().getPreprocessorStatements().size() > 0) {
			UMLPreprocessorStatementListDiff diff = new UMLPreprocessorStatementListDiff(getOriginalClass().getPreprocessorStatements(), getNextClass().getPreprocessorStatements());
			this.preprocessorStatementListDiff = Optional.of(diff);
		}
	}

	public void checkForMovedAnnotations() {
		if(this.getAnnotationListDiff().getAddedAnnotations().size() > 0) {
			List<UMLAnnotation> addedAnnotationsToBeRemoved = new ArrayList<>();
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				if(mapper.getOperationSignatureDiff().isPresent()) {
					UMLAnnotationListDiff annotationListDiff = mapper.getOperationSignatureDiff().get().getAnnotationListDiff();
					if(annotationListDiff.getRemovedAnnotations().size() > 0) {
						for(UMLAnnotation removedAnnotation : annotationListDiff.getRemovedAnnotations()) {
							for(UMLAnnotation addedAnnotation : this.getAnnotationListDiff().getAddedAnnotations()) {
								if(removedAnnotation.equals(addedAnnotation)) {
									MoveAnnotationRefactoring moveAnnotation = new MoveAnnotationRefactoring(removedAnnotation, addedAnnotation, mapper.getOperation1(), nextClass);
									refactorings.add(moveAnnotation);
									addedAnnotationsToBeRemoved.add(addedAnnotation);
									break;
								}
							}
						}
					}
				}
			}
			this.getAnnotationListDiff().getAddedAnnotations().removeAll(addedAnnotationsToBeRemoved);
			Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<>();
			for(Refactoring r : refactorings) {
				if(r instanceof AddClassAnnotationRefactoring addClassAnnotation && addedAnnotationsToBeRemoved.contains(addClassAnnotation.getAnnotation())) {
					refactoringsToBeRemoved.add(addClassAnnotation);
				}
			}
			this.refactorings.removeAll(refactoringsToBeRemoved);
		}
		else if(this.getAnnotationListDiff().getRemovedAnnotations().size() > 0) {
			List<UMLAnnotation> removedAnnotationsToBeRemoved = new ArrayList<>();
			for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
				if(mapper.getOperationSignatureDiff().isPresent()) {
					UMLAnnotationListDiff annotationListDiff = mapper.getOperationSignatureDiff().get().getAnnotationListDiff();
					if(annotationListDiff.getAddedAnnotations().size() > 0) {
						for(UMLAnnotation addedAnnotation : annotationListDiff.getAddedAnnotations()) {
							for(UMLAnnotation removedAnnotation : this.getAnnotationListDiff().getRemovedAnnotations()) {
								if(removedAnnotation.equals(addedAnnotation)) {
									MoveAnnotationRefactoring moveAnnotation = new MoveAnnotationRefactoring(removedAnnotation, addedAnnotation, originalClass, mapper.getOperation2());
									refactorings.add(moveAnnotation);
									removedAnnotationsToBeRemoved.add(removedAnnotation);
									break;
								}
							}
						}
					}
				}
			}
			this.getAnnotationListDiff().getRemovedAnnotations().removeAll(removedAnnotationsToBeRemoved);
			Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<>();
			for(Refactoring r : refactorings) {
				if(r instanceof RemoveClassAnnotationRefactoring removeClassAnnotation && removedAnnotationsToBeRemoved.contains(removeClassAnnotation.getAnnotation())) {
					refactoringsToBeRemoved.add(removeClassAnnotation);
				}
			}
			this.refactorings.removeAll(refactoringsToBeRemoved);
		}
	}

	private void processPrimaryConstructors() throws RefactoringMinerTimedOutException {
		if(getOriginalClass().getPrimaryConstructor().isPresent() && getNextClass().getPrimaryConstructor().isPresent()) {
			List<AbstractExpression> superCallEntries1 = getOriginalClass().getSuperTypeCallEntries();
			List<AbstractExpression> superCallEntries2 = getNextClass().getSuperTypeCallEntries();
			if(superCallEntries1.size() == superCallEntries2.size()) {
				for(int i=0; i<superCallEntries1.size(); i++) {
					AbstractExpression expr1 = superCallEntries1.get(i);
					AbstractExpression expr2 = superCallEntries2.get(i);
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(expr1, expr2, getOriginalClass().getPrimaryConstructor().get(), getNextClass().getPrimaryConstructor().get(), this, modelDiff);
					Set<Refactoring> refactorings2 = new LinkedHashSet<>();
					VariableReplacementAnalysis analysis = new VariableReplacementAnalysis(mapper, refactorings2, this, modelDiff, new LinkedHashSet<>());
					refactorings.addAll(analysis.getVariableRenames());
					refactorings.addAll(refactorings2);
				}
			}
			UMLParameterListDiff parameterListDiff = new UMLParameterListDiff(getOriginalClass().getPrimaryConstructor().get(), getNextClass().getPrimaryConstructor().get(), Collections.emptySet(), Collections.emptySet(), this);
			this.primaryConstructorParameterListDiff = Optional.of(parameterListDiff);
			for(VariableDeclaration addedParameter : parameterListDiff.getAddedParameters()) {
				if(addedParameter.isAttribute() && getOriginalClass().getPrimaryConstructor().get().getParameters().size() == getNextClass().getPrimaryConstructor().get().getParameters().size()) {
					continue;
				}
				Refactoring r = new AddParameterRefactoring(addedParameter, getOriginalClass().getPrimaryConstructor().get(), getNextClass().getPrimaryConstructor().get());
				this.refactorings.add(r);
			}
			for(VariableDeclaration removedParameter : parameterListDiff.getRemovedParameters()) {
				if(removedParameter.isAttribute() && getOriginalClass().getPrimaryConstructor().get().getParameters().size() == getNextClass().getPrimaryConstructor().get().getParameters().size()) {
					continue;
				}
				Refactoring r = new RemoveParameterRefactoring(removedParameter, getOriginalClass().getPrimaryConstructor().get(), getNextClass().getPrimaryConstructor().get());
				this.refactorings.add(r);
			}
		}
		else {
			this.primaryConstructorParameterListDiff = Optional.empty();
		}
	}

	private void checkForMovedCodeBetweenOperations() throws RefactoringMinerTimedOutException {
		//find setUp and tearDown methods
		Set<UMLOperationBodyMapper> setUpMappers = new LinkedHashSet<>();
		Set<UMLOperationBodyMapper> tearDownMappers = new LinkedHashSet<>();
		Set<UMLOperationBodyMapper> constructorMappers = new LinkedHashSet<>();
		List<UMLOperationBodyMapper> moveCodeMappers = new ArrayList<>();
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getContainer1() instanceof ModuleContainer && mapper.getContainer2() instanceof ModuleContainer) {
				continue;
			}
			if(mapper.getContainer1().hasSetUpAnnotation() && mapper.getContainer2().hasSetUpAnnotation()) {
				setUpMappers.add(mapper);
			}
			if(mapper.getContainer1().getName().equals("setUp") && mapper.getContainer2().getName().equals("setUp")) {
				setUpMappers.add(mapper);
			}
			if(mapper.getContainer1().getName().equals("prepare") && mapper.getContainer2().getName().equals("prepare")) {
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
								MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
								if(!moveCodeMappers.contains(moveCodeMapper))
									moveCodeMappers.add(moveCodeMapper);
								refactorings.add(ref);
							}
						}
					}
				}
			}
			if(mapper.getLambdaMappers().size() > 0 && mapper.nonMappedElementsT2() > 0 && mapper.getContainer1().getDefaultExpression() == null && mapper.getContainer2().getDefaultExpression() == null) {
				for(UMLOperationBodyMapper lambdaMapper : mapper.getLambdaMappers()) {
					if(lambdaMapper.nonMappedElementsT1() > 0) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(lambdaMapper, mapper, this);
						int invalidMappings = 0;
						for(AbstractCodeMapping mapping : moveCodeMapper.getMappings()) {
							if(mapper.alreadyMatched2(mapping.getFragment2()) || lambdaMapper.getNonMappedLeavesT2().contains(mapping.getFragment2()) ||
									lambdaMapper.getNonMappedInnerNodesT2().contains(mapping.getFragment2())) {
								invalidMappings++;
							}
							else if(mapping.getFragment1() instanceof LeafExpression || mapping.getFragment1() instanceof AbstractExpression ||
									mapping.getFragment2() instanceof LeafExpression || mapping.getFragment2() instanceof AbstractExpression) {
								invalidMappings++;
							}
						}
						if(moveCodeMapper.getMappings().size() > invalidMappings) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
							int extractedStatementCount = 0;
							for(Refactoring r : refactorings) {
								if(r instanceof ExtractOperationRefactoring) {
									ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
									for(AbstractCodeMapping mapping : moveCodeMapper.getMappings()) {
										for(AbstractCodeMapping m : extract.getBodyMapper().getMappings()) {
											if(m.getFragment1().equals(mapping.getFragment1())) {
												extractedStatementCount++;
												break;
											}
										}
									}
								}
							}
							boolean skip = false;
							if(extractedStatementCount == moveCodeMapper.getMappings().size() && extractedStatementCount > 0) {
								skip = true;
							}
							if(!skip) {
								if(!moveCodeMappers.contains(moveCodeMapper))
									moveCodeMappers.add(moveCodeMapper);
								refactorings.add(ref);
							}
						}
					}
					//support move code from deleted lambda to LambdaMapper
					if(lambdaMapper.nonMappedElementsT2() > 0) {
						for(AbstractCodeFragment fragment1 : mapper.getNonMappedLeavesT1()) {
							if(fragment1.getLambdas().size() > 0) {
								for(LambdaExpressionObject lambda : fragment1.getLambdas()) {
									if(lambda.getBody() != null) {
										UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(lambda, lambdaMapper, this);
										int invalidMappings = 0;
										for(AbstractCodeMapping mapping : moveCodeMapper.getMappings()) {
											if(lambdaMapper.alreadyMatched1(mapping.getFragment1()) || lambdaMapper.alreadyMatched2(mapping.getFragment2())) {
												invalidMappings++;
											}
											else if(mapping.getFragment1() instanceof LeafExpression || mapping.getFragment1() instanceof AbstractExpression ||
													mapping.getFragment2() instanceof LeafExpression || mapping.getFragment2() instanceof AbstractExpression) {
												invalidMappings++;
											}
										}
										if(moveCodeMapper.getMappings().size() > invalidMappings) {
											MoveCodeRefactoring ref = new MoveCodeRefactoring(lambdaMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
											ref.setLambdaMapper(lambdaMapper);
											if(!moveCodeMappers.contains(moveCodeMapper))
												moveCodeMappers.add(moveCodeMapper);
											refactorings.add(ref);
											refactorings.addAll(moveCodeMapper.getRefactoringsAfterPostProcessing());
										}
									}
								}
							}
						}
					}
					//support move code from LambdaMapper to added lambda
					if(lambdaMapper.nonMappedElementsT1() > 0) {
						for(AbstractCodeFragment fragment2 : mapper.getNonMappedLeavesT2()) {
							if(fragment2.getLambdas().size() > 0) {
								for(LambdaExpressionObject lambda : fragment2.getLambdas()) {
									if(lambda.getBody() != null) {
										UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(lambdaMapper, lambda, this);
										int invalidMappings = 0;
										for(AbstractCodeMapping mapping : moveCodeMapper.getMappings()) {
											if(lambdaMapper.alreadyMatched1(mapping.getFragment1()) || lambdaMapper.alreadyMatched2(mapping.getFragment2())) {
												invalidMappings++;
											}
											else if(mapping.getFragment1() instanceof LeafExpression || mapping.getFragment1() instanceof AbstractExpression ||
													mapping.getFragment2() instanceof LeafExpression || mapping.getFragment2() instanceof AbstractExpression) {
												invalidMappings++;
											}
										}
										if(moveCodeMapper.getMappings().size() > invalidMappings) {
											MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), lambdaMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
											ref.setLambdaMapper(lambdaMapper);
											if(!moveCodeMappers.contains(moveCodeMapper))
												moveCodeMappers.add(moveCodeMapper);
											refactorings.add(ref);
											refactorings.addAll(moveCodeMapper.getRefactoringsAfterPostProcessing());
										}
									}
								}
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
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							if(!moveCodeMappers.contains(mapper)) {
								moveCodeMappers.add(mapper);
							}
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
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							if(!moveCodeMappers.contains(mapper)) {
								moveCodeMappers.add(mapper);
							}
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
								MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
								if(!moveCodeMappers.contains(moveCodeMapper))
									moveCodeMappers.add(moveCodeMapper);
								moveCodeMapper.computeRefactoringsWithinBody();
								constructorMapper.addChildMapper(moveCodeMapper);
								refactorings.add(ref);
							}
						}
						if(mapper.nonMappedElementsT2() > 0) {
							UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(constructorMapper, mapper, this);
							if(moveCodeMapper.getExactMatchesWithoutLoggingStatements().size() > 0 && !mappingFoundInExtractedMethod(moveCodeMapper.getMappings())) {
								MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
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
										MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
										if(!moveCodeMappers.contains(moveCodeMapper))
											moveCodeMappers.add(moveCodeMapper);
										refactoringsToBeAdded.add(ref);
									}
								}
								if(mapper.nonMappedElementsT2() > 0) {
									UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(constructorMapper, mapper, this);
									if(moveCodeMapper.getExactMatchesWithoutLoggingStatements().size() > 0 && !mappingFoundInExtractedMethod(moveCodeMapper.getMappings())) {
										MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
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
					removedOperation.getName().equals("setUp") || removedOperation.getName().equals("tearDown") || removedOperation.getName().equals("prepare")) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(mapper.nonMappedElementsT2() > 0) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(removedOperation, mapper, this);
						if(moveCodeMapper.getMappings().size() > 0) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_FROM_REMOVED);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							moveCodeMapper.computeRefactoringsWithinBody();
							refactorings.add(ref);
							refactorings.addAll(moveCodeMapper.getRefactoringsAfterPostProcessing());
						}
					}
				}
			}
			else if(removedOperation.isConstructor()) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(mapper.nonMappedElementsT2() > 0) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(removedOperation, mapper, this);
						if(moveCodeMapper.mappingsWithoutBlocks() > 2) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_FROM_REMOVED);
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
					addedOperation.getName().equals("setUp") || addedOperation.getName().equals("tearDown") || addedOperation.getName().equals("prepare")) {
				for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
					if(mapper.nonMappedElementsT1() > 0) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, addedOperation, this);
						if(moveCodeMapper.getMappings().size() > 0) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_TO_ADDED);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							moveCodeMapper.computeRefactoringsWithinBody();
							refactorings.add(ref);
							refactorings.addAll(moveCodeMapper.getRefactoringsAfterPostProcessing());
						}
					}
				}
			}
		}
		//move from deprecated method to new one
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.nonMappedElementsT1() > 0 && mapper.getContainer2().hasDeprecatedAnnotation() && !mapper.getContainer1().hasDeprecatedAnnotation()) {
				for(UMLOperation addedOperation : addedOperations) {
					if(addedOperation.getName().equals(mapper.getContainer2().getName())) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(mapper, addedOperation, this);
						if(moveCodeMapper.getMappings().size() > 0 && !mappingFoundInExtractedMethod(moveCodeMapper.getMappings())) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_TO_ADDED);
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
				MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
				if(!moveCodeMappers.contains(moveCodeMapper))
					moveCodeMappers.add(moveCodeMapper);
				refactorings.add(ref);
			}
		}
		for(Pair<UMLOperationBodyMapper, UMLOperationBodyMapper> pair : calledBy) {
			UMLOperationBodyMapper called = pair.getLeft();
			UMLOperationBodyMapper caller = pair.getRight();
			if(!called.getContainer1().getParameterTypeList().equals(called.getContainer2().getParameterTypeList())) {
				UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(caller, called, this);
				if(moveCodeMapper.mappingsWithoutBlocks() > 1 || moveCodeMapper.exactMatches() > 0) {
					MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_BETWEEN_EXISTING);
					if(!moveCodeMappers.contains(moveCodeMapper))
						moveCodeMappers.add(moveCodeMapper);
					if(!moveCodeMappers.contains(caller))
						moveCodeMappers.add(caller);
					refactorings.add(ref);
				}
			}
		}
		if(LANG1.equals(Constants.JAVA) && LANG2.equals(Constants.KOTLIN)) {
			for(UMLOperation removedOperation : removedOperations) {
				if(removedOperation.isConstructor()) {
					for(UMLInitializer addedInitializer : addedInitializers) {
						UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(removedOperation, addedInitializer, this);
						if(moveCodeMapper.mappingsWithoutBlocks() > 1 || moveCodeMapper.exactMatches() > 0) {
							MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_FROM_REMOVED_TO_ADDED);
							if(!moveCodeMappers.contains(moveCodeMapper))
								moveCodeMappers.add(moveCodeMapper);
							refactorings.add(ref);
						}
					}
				}
				else {
					for(UMLAttribute addedAttribute : addedAttributes) {
						if(addedAttribute.getCustomGetter().isPresent()) {
							UMLOperationBodyMapper moveCodeMapper = new UMLOperationBodyMapper(removedOperation, addedAttribute.getCustomGetter().get(), this);
							if(moveCodeMapper.mappingsWithoutBlocks() > 1 || moveCodeMapper.exactMatches() > 0) {
								MoveCodeRefactoring ref = new MoveCodeRefactoring(moveCodeMapper.getContainer1(), moveCodeMapper.getContainer2(), moveCodeMapper, Type.MOVE_FROM_REMOVED_TO_ADDED);
								if(!moveCodeMappers.contains(moveCodeMapper))
									moveCodeMappers.add(moveCodeMapper);
								refactorings.add(ref);
							}
						}
					}
				}
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
					if((mappings >= nonMappedElementsT1 && mappings >= nonMappedElementsT2) ||
							isPartOfMethodExtracted(initializer1, initializer2) || isPartOfMethodInlined(initializer1, initializer2)) {
						addOperationBodyMapper(mapper);
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
								addOperationBodyMapper(mapper);
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
								addOperationBodyMapper(mapper);
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
    					refactorings.addAll(enumConstantDiff.getRefactorings());
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
    					refactorings.addAll(enumConstantDiff.getRefactorings());
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
			boolean skip = false;
			if(operationWithTheSameSignature != null && originalClass.isInterface() && !operation.getName().equals(operationWithTheSameSignature.getName()) ) {
				for(UMLOperation operation1 : originalClass.getOperations()) {
					if(operation1.equalSignatureRelaxedReturnType(operationWithTheSameSignature)) {
						skip = true;
						break;
					}
				}
			}
			if(operationWithTheSameSignature == null) {
				this.removedOperations.add(operation);
			}
			else if(!mapperListContainsOperation(operation, operationWithTheSameSignature) && !skip) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature, this);
				this.addOperationBodyMapper(mapper);
			}
		}
		for(UMLOperation operation : nextClass.getOperations()) {
			UMLOperation operationWithTheSameSignature = originalClass.operationWithTheSameSignatureIgnoringChangedTypes(operation);
			boolean skip = false;
			if(operationWithTheSameSignature != null && nextClass.isInterface() && !operation.getName().equals(operationWithTheSameSignature.getName()) ) {
				for(UMLOperation operation2 : nextClass.getOperations()) {
					if(operation2.equalSignatureRelaxedReturnType(operationWithTheSameSignature)) {
						skip = true;
						break;
					}
				}
			}
			if(operationWithTheSameSignature == null) {
				this.addedOperations.add(operation);
			}
			else if(!mapperListContainsOperation(operationWithTheSameSignature, operation) && !skip) {
				UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operationWithTheSameSignature, operation, this);
				this.addOperationBodyMapper(mapper);
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

	public Optional<Pair<UMLType, UMLType>> getCommonFunctionType() {
		return commonFunctionType;
	}

	public Optional<UMLPreprocessorStatementListDiff> getPreprocessorStatementListDiff() {
		return preprocessorStatementListDiff;
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

	private void checkForInlinedOperationsToExtractedOperations(List<UMLOperationBodyMapper> extractedBodyMappers) throws RefactoringMinerTimedOutException {
		List<UMLOperation> operationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperationBodyMapper> inlinedOperationMappers = new ArrayList<UMLOperationBodyMapper>();
		boolean inlinedToExtracted = false;
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
					inlinedToExtracted = true;
					//check if the Javadoc of the inlined method matches the Javadoc of the method calling the extracted one
					if(removedOperation.getJavadoc() != null && mapper.getParentMapper().getOperation2() != null && mapper.getParentMapper().getOperation2().getJavadoc() != null &&
							removedOperation.getJavadoc().getText().equals(mapper.getParentMapper().getOperation2().getJavadoc().getText())) {
						UMLJavadocDiff diff = new UMLJavadocDiff(removedOperation.getJavadoc(), mapper.getParentMapper().getOperation2().getJavadoc());
						mapper.getParentMapper().updateJavadocDiff(diff);
					}
					//check if the Javadoc of the method from which the extracted method was extracted matches the Javadoc of the extracted method
					if(mapper.getParentMapper().getOperation1() != null && mapper.getParentMapper().getOperation1().getJavadoc() != null &&
							mapper.getOperation2() != null && mapper.getOperation2().getJavadoc() != null &&
							mapper.getParentMapper().getOperation1().getJavadoc().getText().equals(mapper.getOperation2().getJavadoc().getText())) {
						UMLJavadocDiff diff = new UMLJavadocDiff(mapper.getParentMapper().getOperation1().getJavadoc(), mapper.getOperation2().getJavadoc());
						mapper.updateJavadocDiff(diff);
					}
				}
			}
		}
		if(inlinedToExtracted) {
			MappingOptimizer optimizer = new MappingOptimizer(this);
			for(UMLOperationBodyMapper mapper : extractedBodyMappers) {
				int size = refactorings.size();
				optimizer.optimizeDuplicateMappingsForInline(mapper, refactorings);
				if(refactorings.size() < size) {
					//refactoring has been removed
					for(UMLOperationBodyMapper operationBodyMapper : inlinedOperationMappers) {
						operationsToBeRemoved.remove(operationBodyMapper.getContainer1());
					}
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
		List<UMLOperation> allAddedOperations = new ArrayList<>(addedOperations);
		allAddedOperations.addAll(getAddedNestedOperations());
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if((!mapper.getNonMappedLeavesT1().isEmpty() || !mapper.getNonMappedInnerNodesT1().isEmpty()) && mapper.getChildMappers().size() == 0) {
				ExtractOperationDetection detection = new ExtractOperationDetection(mapper, allAddedOperations, this, modelDiff, true);
				List<UMLOperation> sortedAddedOperations = detection.getAddedOperationsSortedByCalls();
				for(UMLOperation addedOperation : sortedAddedOperations) {
					List<ExtractOperationRefactoring> refs = detection.check(addedOperation);
					for(ExtractOperationRefactoring refactoring : refs) {
						UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
						boolean objectCreation = false;
						for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
							AbstractCall call1 = mapping.getFragment1().creationCoveringEntireFragment();
							AbstractCall call2 = mapping.getFragment2().creationCoveringEntireFragment();
							if(call1 != null && call2 != null && call1.identicalName(call2)) {
								objectCreation = true;
								break;
							}
						}
						boolean duplicate = false;
						for(Refactoring r : refactorings) {
							if(r instanceof ExtractOperationRefactoring) {
								ExtractOperationRefactoring ex = (ExtractOperationRefactoring)r;
								if(ex.getBodyMapper().getMappings().equals(refactoring.getBodyMapper().getMappings())) {
									duplicate = true;
								}
							}
						}
						if(!duplicate && (operationBodyMapper.exactMatches() > 1 || objectCreation)) {
							refactorings.add(refactoring);
							extractedOperationMappers.add(operationBodyMapper);
							mapper.addChildMapper(operationBodyMapper);
							parentMappersToBeOptimized.add(mapper);
							operationsToBeRemoved.add(addedOperation);
						}
					}
				}
				//check if container2 calls another mapper
				if(mapper.getMappings().size() == 0) {
					for(UMLOperationBodyMapper mapper2 : getOperationBodyMapperList()) {
						if(!mapper.equals(mapper2) && containCallToOperation(mapper2.getContainer2(), mapper.getContainer2())) {
							ExtractOperationDetection detection2 = new ExtractOperationDetection(mapper, List.of(mapper2.getOperation2()), this, modelDiff, false);
							List<ExtractOperationRefactoring> refs = detection2.check(mapper2.getOperation2());
							for(ExtractOperationRefactoring refactoring : refs) {
								UMLOperationBodyMapper operationBodyMapper = refactoring.getBodyMapper();
								if(operationBodyMapper.exactMatches() > 1) {
									refactorings.add(refactoring);
									extractedOperationMappers.add(operationBodyMapper);
									mapper.addChildMapper(operationBodyMapper);
									parentMappersToBeOptimized.add(mapper);
									//operationsToBeRemoved.add(addedOperation);
								}
							}
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

	private List<UMLJavadoc> findUnmatchedJavadocsInMethodBodiesBefore() {
		List<UMLJavadoc> list = new ArrayList<UMLJavadoc>();
		for(UMLOperationBodyMapper mapper : operationBodyMapperList) {
			if(mapper.getCommentListDiff() != null) {
				for(UMLComment comment : mapper.getCommentListDiff().getDeletedComments()) {
					if(comment.getJavaDoc().isPresent()) {
						list.add(comment.getJavaDoc().get());
					}
				}
			}
		}
		return list;
	}

	protected boolean testAnnotationMismatch(UMLOperation op1, UMLOperation op2) {
		if(op1.hasTestAnnotation() && !op2.hasTestAnnotation() && !op2.hasParameterizedTestAnnotation()) {
			for(UMLOperation operation : nextClass.getOperations()) {
				if(!operation.equals(op2) && operation.getName().equals(op1.getName()) && (operation.hasTestAnnotation() || operation.hasParameterizedTestAnnotation())) {
					return true;
				}
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
						if(originalOperation.getJavadoc() == null && nextOperation.getJavadoc() != null) {
							List<UMLJavadoc> list = findUnmatchedJavadocsInMethodBodiesBefore();
							for(UMLJavadoc doc : list) {
								if(doc.getText().equals(nextOperation.getJavadoc().getText())) {
									UMLJavadocDiff diff = new UMLJavadocDiff(doc, nextOperation.getJavadoc());
									operationBodyMapper.updateJavadocDiff(diff);
									break;
								}
							}
						}
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
				else if(!typeParameterDiffList.isEmpty() && removedOperation.equalsIgnoringParentClassTypeParameterChange(addedOperation, typeParameterDiffList) &&
						!differentParameterNames(removedOperation, addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
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
				else if(removedOperation.equalsIgnoringTypeParameters(addedOperation) && !differentParameterNames(removedOperation, addedOperation) &&
						Math.abs(removedOperations.size() - addedOperations.size()) <= 1 && !testAnnotationMismatch(removedOperation, addedOperation)) {
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
					List<AbstractCodeMapping> exactMatchListWithoutMatchesInNestedContainers = operationBodyMapper.getExactMatchesWithoutMatchesInNestedContainers();
					int exactMatchesWithoutMatchesInNestedContainers = exactMatchListWithoutMatchesInNestedContainers.size();
					boolean skip = operationBodyMapper.getMappings().size() == 1 && exactMatchesWithoutMatchesInNestedContainers == 1 && exactMatchListWithoutMatchesInNestedContainers.get(0).getFragment1().throwsNewException();
					if(!skip) {
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
		}
		if(!originalOperationStringRepresentation.equals(nextOperationStringRepresentation) && originalOperationStringRepresentation.size() > 2) {
			for(UMLOperation addedOperation : addedOperations) {
				if(addedOperation.stringRepresentation().equals(originalOperationStringRepresentation) && !containCallToOperation(addedOperation, nextOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(originalOperation, addedOperation, this);
					List<AbstractCodeMapping> exactMatchListWithoutMatchesInNestedContainers = operationBodyMapper.getExactMatchesWithoutMatchesInNestedContainers();
					int exactMatchesWithoutMatchesInNestedContainers = exactMatchListWithoutMatchesInNestedContainers.size();
					boolean skip = operationBodyMapper.getMappings().size() == 1 && exactMatchesWithoutMatchesInNestedContainers == 1 && exactMatchListWithoutMatchesInNestedContainers.get(0).getFragment1().throwsNewException();
					if(!skip) {
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
