package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.refactoringminer.astDiff.actions.ASTDiff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ProjectASTDiffer
{
	private final static Logger logger = LoggerFactory.getLogger(ProjectASTDiffer.class);
	private final boolean CHECK_COMMENTS = false;
	private final UMLModelDiff modelDiff;
	private List<AbstractCodeMapping> lastStepMappings;
	private ExtendedMultiMappingStore optimizationMappingStore;
	private ExtendedMultiMappingStore finalOptimization;
	private List<Refactoring> modelDiffRefactorings;
	private final Set<ASTDiff> diffSet = new LinkedHashSet<>();

	public ProjectASTDiffer(UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
		this.modelDiff = modelDiff;
		diff();
	}

	public Set<ASTDiff> getDiffSet() {
		return diffSet;
	}

	private void diff() throws RefactoringMinerTimedOutException {
		long start = System.currentTimeMillis();
		this.modelDiffRefactorings = modelDiff.getRefactorings();
		long finish = System.currentTimeMillis();
		logger.info("ModelDiff.getRefactorings() execution time: " + (finish - start)/ 1000 + " seconds");
		long diff_execution_started = System.currentTimeMillis();
		makeASTDiff(modelDiff.getCommonClassDiffList(),false);
		makeASTDiff(modelDiff.getClassRenameDiffList(),false);
		makeASTDiff(modelDiff.getClassMoveDiffList(),false);
		makeASTDiff(modelDiff.getInnerClassMoveDiffList(),true);
		makeASTDiff(getExtraDiffs(),true);
		long diff_execution_finished =  System.currentTimeMillis();
		logger.info("Diff execution: " + (diff_execution_finished - diff_execution_started)/ 1000 + " seconds");

		computeAllEditScripts();
	}

	private List<? extends UMLAbstractClassDiff> getExtraDiffs() {
		List<UMLAbstractClassDiff> extraDiffs = new ArrayList<>();
		for (Refactoring modelDiffRefactoring : modelDiffRefactorings) {
			if ((modelDiffRefactoring.getRefactoringType() == RefactoringType.REPLACE_ANONYMOUS_WITH_CLASS))
			{
				ReplaceAnonymousWithClassRefactoring replaceAnonymousWithClassRefactoring = (ReplaceAnonymousWithClassRefactoring) modelDiffRefactoring;
				extraDiffs.add(replaceAnonymousWithClassRefactoring.getDiff());
			}
		}
		return extraDiffs;
	}
	private void computeAllEditScripts() {
		long editScript_start = System.currentTimeMillis();
		for (ASTDiff diff : diffSet) {
			diff.computeEditScript(modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap());
		}
		long editScript_end = System.currentTimeMillis();
		logger.info("EditScript execution: " + (editScript_end - editScript_start)/ 1000 + " seconds");
	}

	private void makeASTDiff(List<? extends UMLAbstractClassDiff> umlClassBaseDiffList, boolean mergeFlag){
		for (UMLAbstractClassDiff classDiff : umlClassBaseDiffList) {
			ASTDiff append = findAppend(classDiff);
			boolean decision = (append != null) || mergeFlag;
			ASTDiff classASTDiff = process(classDiff, findTreeContexts(classDiff), decision);
			if (append != null)
				append.getAllMappings().mergeMappings(classASTDiff.getAllMappings());
			else {
				diffSet.add(classASTDiff);
			}
		}
	}

	private ASTDiff findAppend(UMLAbstractClassDiff classBaseDiff) {
		for (ASTDiff existing : diffSet) {
			if (existing.getSrcPath().equals(classBaseDiff.getOriginalClass().getSourceFile()))
				return existing;
			else if (existing.getDstPath().equals(classBaseDiff.getNextClass().getSourceFile()))
				return existing;
		}
		return null;
	}

	private Pair<TreeContext, TreeContext> findTreeContexts(UMLAbstractClassDiff classDiff) {
		return new Pair<>(modelDiff.getParentModel().getTreeContextMap().get(classDiff.getOriginalClass().getSourceFile()),
				modelDiff.getChildModel().getTreeContextMap().get(classDiff.getNextClass().getSourceFile()));
	}

	private ASTDiff process(UMLAbstractClassDiff classDiff, Pair<TreeContext, TreeContext> treeContextPair,boolean mergeFlag){
		TreeContext srcTreeContext = treeContextPair.first;
		TreeContext dstTreeContext = treeContextPair.second;
		Tree srcTree = srcTreeContext.getRoot();
		Tree dstTree = dstTreeContext.getRoot();
		ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		this.lastStepMappings = new ArrayList<>();
		this.optimizationMappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		this.finalOptimization = new ExtendedMultiMappingStore(srcTree,dstTree);
		if (!mergeFlag) {
			mappingStore.addMapping(srcTree, dstTree);
			processPackageDeclaration(srcTree,dstTree,classDiff,mappingStore);
		}
		boolean isBaseDiff = classDiff instanceof UMLClassBaseDiff;
		if (isBaseDiff) {
			UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
			processImports(srcTree, dstTree, (baseClassDiff).getImportDiffList(), mappingStore);
			processRefactorings(srcTree,dstTree,getClassDiffRefactorings(baseClassDiff),mappingStore);
			processClassDeclarationMapping(srcTree,dstTree, baseClassDiff,mappingStore);
		}
		processClassAttributes(srcTree,dstTree,classDiff,mappingStore);
		processEnumConstants(srcTree,dstTree,classDiff.getCommonEnumConstants(),mappingStore);
		processAllMethods(srcTree,dstTree,classDiff.getOperationBodyMapperList(),mappingStore);
		processModelDiffRefactorings(srcTree,dstTree,classDiff,mappingStore);
		processMovedAttributes(srcTree,dstTree,classDiff,mappingStore);
		processLastStepMappings(srcTree,dstTree,mappingStore);

		//if (CHECK_COMMENTS) addAndProcessComments(treeContextPair.first, treeContextPair.second,mappingStore);
		return new ASTDiff(classDiff.getOriginalClass().getLocationInfo().getFilePath(),
				classDiff.getNextClass().getLocationInfo().getFilePath(), treeContextPair.first, treeContextPair.second, mappingStore);
	}

	private List<Refactoring> getClassDiffRefactorings(UMLClassBaseDiff classDiff) {
		List<Refactoring> classDiffRefactorings = new ArrayList<>();
		for (Refactoring modelDiffRefactoring : modelDiffRefactorings) {
			Set<ImmutablePair<String, String>> involvedClassesBeforeRefactoring = modelDiffRefactoring.getInvolvedClassesBeforeRefactoring();
			Set<ImmutablePair<String, String>> involvedClassesAfterRefactoring = modelDiffRefactoring.getInvolvedClassesAfterRefactoring();
			if (involvedClassesBeforeRefactoring.size() > 1 || involvedClassesAfterRefactoring.size() > 1) continue;
			//TODO: Must extend the logic to work for cases with more than one involving classes such as ExtractAndMoveMethodRefactoring
			UMLClass umlClassBefore = classDiff.getOriginalClass();
			UMLClass umlClassAfter = classDiff.getNextClass();
			ImmutablePair<String, String> refactoringClassBefore = involvedClassesBeforeRefactoring.iterator().next();
			ImmutablePair<String, String> refactoringClassAfter = involvedClassesAfterRefactoring.iterator().next();
			if (umlClassBefore.getLocationInfo().getFilePath().equals(refactoringClassBefore.getLeft())
					&& umlClassAfter.getLocationInfo().getFilePath().equals(refactoringClassAfter.getLeft())) {
				String refactoringClassNameBefore = refactoringClassBefore.getRight();
				String refactoringClassNameAfter = refactoringClassAfter.getRight();
				// Relied on || in order to ascertain at least one class involves (handling move to anonymous and vice versa)
				boolean isNameMatching = refactoringClassNameBefore.equals(umlClassBefore.getName()) || refactoringClassNameAfter.equals(umlClassAfter.getName());
//				boolean isReplaceWithAnonymousWithClassWhileContainsSameName = false;
//				if (modelDiffRefactoring.getRefactoringType().equals(RefactoringType.REPLACE_ANONYMOUS_WITH_CLASS) ||
//						modelDiffRefactoring.getRefactoringType().equals(RefactoringType.REPLACE_ANONYMOUS_WITH_LAMBDA))
//					if (refactoringClassNameBefore.contains(umlClassBefore.getName()) &&  refactoringClassNameAfter.contains(umlClassAfter.getName()))
//						isReplaceWithAnonymousWithClassWhileContainsSameName = true;
				if (isNameMatching)
					classDiffRefactorings.add(modelDiffRefactoring);
			}

		}
		return classDiffRefactorings;
	}

	private void processMovedAttributes(Tree srcTree, Tree dstTree, UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		List<UMLAttributeDiff> movedAttributeDiffList = modelDiff.getMovedAttributeDiffList();
		for (UMLAttributeDiff umlAttributeDiff : movedAttributeDiffList) {
			UMLAttribute srcAttr = umlAttributeDiff.getRemovedAttribute();
			UMLAttribute dstAttr = umlAttributeDiff.getAddedAttribute();
			String srcAttrPath = srcAttr.getLocationInfo().getFilePath();
			String dstAttrPath = dstAttr.getLocationInfo().getFilePath();
			if (classDiff.getOriginalClass().getLocationInfo().getFilePath().equals(srcAttrPath) &&
					classDiff.getNextClass().getLocationInfo().getFilePath().equals(dstAttrPath)) {
				Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcAttrPath).getRoot();
				Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstAttrPath).getRoot();
				processFieldDeclarationByAttrDiff(srcTotalTree,dstTotalTree,umlAttributeDiff,mappingStore);
			}
		}
	}

	private void processLastStepMappings(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
		ExtendedMultiMappingStore lastStepMappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		for (AbstractCodeMapping lastStepMapping : lastStepMappings) {
			if (lastStepMapping.getFragment1().getLocationInfo().getFilePath().equals(lastStepMapping.getFragment2().getLocationInfo().getFilePath())) {
				Tree srcExp = TreeUtilFunctions.findByLocationInfo(srcTree, lastStepMapping.getFragment1().getLocationInfo());
				Tree dstExp = TreeUtilFunctions.findByLocationInfo(dstTree, lastStepMapping.getFragment2().getLocationInfo());
				new LeafMatcher().match(srcExp, dstExp, lastStepMappingStore);
			}
		}
		mappingStore.replaceWithOptimizedMappings(lastStepMappingStore);
		prepareOptimization(mappingStore);
		mappingStore.replaceWithOptimizedMappings(optimizationMappingStore);
		mappingStore.replaceWithOptimizedMappings(finalOptimization);
	}

	private void prepareOptimization(ExtendedMultiMappingStore mappingStore) {
		for (Mapping mapping : optimizationMappingStore) {
			Set<Tree> dsts = mappingStore.getDsts(mapping.first);
			if (dsts == null) continue;
			if (dsts.contains(mapping.second))
				optimizationMappingStore.removeMapping(mapping.first,mapping.second);
		}
	}

	private void processEnumConstants(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<UMLEnumConstant, UMLEnumConstant>> commonEnumConstants, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<UMLEnumConstant, UMLEnumConstant> commonEnumConstant : commonEnumConstants) {
			LocationInfo locationInfo1 = commonEnumConstant.getLeft().getLocationInfo();
			LocationInfo locationInfo2 = commonEnumConstant.getRight().getLocationInfo();
			Tree srcEnumConstant = TreeUtilFunctions.findByLocationInfo(srcTree,locationInfo1);
			Tree dstEnumConstant = TreeUtilFunctions.findByLocationInfo(dstTree,locationInfo2);
			new LeafMatcher().match(srcEnumConstant,dstEnumConstant,mappingStore);
		}
	}

	private void processModelDiffRefactorings(Tree srcTree, Tree dstTree, UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		for(Refactoring refactoring : modelDiffRefactorings) {
			List<String> beforeRefactoringClasses = refactoring.getInvolvedClassesBeforeRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
			List<String> afterRefactoringClasses = refactoring.getInvolvedClassesAfterRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
			if (refactoring instanceof MoveOperationRefactoring) {
				if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
						beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
					MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
					String srcPath = moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
					String dstPath = moveOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
					Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
					Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
					processMethod(srcTotalTree, dstTotalTree, moveOperationRefactoring.getBodyMapper(), mappingStore);
				}
			} else if (refactoring instanceof MoveAttributeRefactoring) {
				if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
						beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
					MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;
					String srcPath = moveAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
					String dstPath = moveAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
					Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
					Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
					processFieldDeclaration(srcTotalTree, dstTotalTree, moveAttributeRefactoring.getOriginalAttribute(), moveAttributeRefactoring.getMovedAttribute(), mappingStore);
				}
			} else if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION)) {
				ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
				UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
				String srcPath = bodyMapper.getOperation1().getLocationInfo().getFilePath();
				String dstPath = bodyMapper.getOperation2().getLocationInfo().getFilePath();
				Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
				Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
				processBodyMapper(srcTotalTree,dstTotalTree, bodyMapper,mappingStore, true);
			}

		}
	}

    /*
	private void addAndProcessComments(TreeContext firstTC, TreeContext secondTC, MultiMappingStore mappingStore) {
		Pair<List<Tree>, List<Tree>> addedCommentsPair = addComments(firstTC,secondTC);
		processComments(addedCommentsPair,mappingStore);
	}
	*/

	private void processAllMethods(Tree srcTree, Tree dstTree, List<UMLOperationBodyMapper> operationBodyMapperList, ExtendedMultiMappingStore mappingStore) {
		for(UMLOperationBodyMapper umlOperationBodyMapper : new ArrayList<>(operationBodyMapperList))
			processMethod(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
	}

	private void processMethod(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
		Tree srcOperationNode;
		Tree dstOperationNode;
		if (umlOperationBodyMapper.getOperation1() != null & umlOperationBodyMapper.getOperation2() != null) {
			srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getOperation1().getLocationInfo());
			dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getOperation2().getLocationInfo());
			processJavaDocs(srcOperationNode, dstOperationNode, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc(), mappingStore);
			mappingStore.addMapping(srcOperationNode, dstOperationNode);
		} else {
			//Static Initializers
			srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo());
			dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo());
			mappingStore.addMapping(srcOperationNode, dstOperationNode);
			if (umlOperationBodyMapper.getContainer1() instanceof UMLInitializer &&  umlOperationBodyMapper.getContainer2() instanceof UMLInitializer)
				if (((UMLInitializer)umlOperationBodyMapper.getContainer1()).isStatic() && ((UMLInitializer)umlOperationBodyMapper.getContainer2()).isStatic())
					mappingStore.addMapping(srcOperationNode.getChild(0),dstOperationNode.getChild(0));
		}
		processMethodSignature(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
		processBodyMapper(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore, false);
		processOperationDiff(srcOperationNode,dstOperationNode,umlOperationBodyMapper,mappingStore);
		processMethodParameters(srcOperationNode,dstOperationNode,umlOperationBodyMapper.getMatchedVariables(),mappingStore);
	}

	private void processMethodParameters(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
			VariableDeclaration leftVarDecl = matchedPair.getLeft();
			VariableDeclaration rightVarDecl = matchedPair.getRight();
			Tree leftTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftVarDecl.getLocationInfo());
			Tree rightTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightVarDecl.getLocationInfo());
			if (leftVarDecl.isParameter() && rightVarDecl.isParameter()) {
				if (rightTree.isIsomorphicTo(leftTree))
					mappingStore.addMappingRecursively(leftTree, rightTree);
				else {
					new LeafMatcher().match(leftTree,rightTree,mappingStore);
					mappingStore.addMapping(leftTree,rightTree);
				}
			}
		}
	}

	private void processBodyMapper(Tree srcTree, Tree dstTree, UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
		if (bodyMapper.getAnonymousClassDiffs() != null) {
			for (UMLAnonymousClassDiff anonymousClassDiff : bodyMapper.getAnonymousClassDiffs()) {
				processClassAttributes(srcTree,dstTree,anonymousClassDiff,mappingStore);
				for (UMLOperationBodyMapper umlOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
					processMethod(srcTree,dstTree,umlOperationBodyMapper,mappingStore);
				}
			}
		}
		Set<AbstractCodeMapping> mappingSet = bodyMapper.getMappings();
		ArrayList<AbstractCodeMapping> mappings = new ArrayList<>(mappingSet);
//		mappings.add(new LeafMapping(
//				bodyMapper.getNonMappedLeavesT1().get(0)
//				,bodyMapper.getNonMappedLeavesT2().get(0),null,null));
		for (AbstractCodeMapping abstractCodeMapping : mappings) {
			if (abstractCodeMapping instanceof LeafMapping)
				processLeafMapping(srcTree,dstTree,abstractCodeMapping,mappingStore, isPartOfExtractedMethod);
			else if (abstractCodeMapping instanceof CompositeStatementObjectMapping)
				processCompositeMapping(srcTree,dstTree,abstractCodeMapping,mappingStore);
		}
	}

	private void processCompositeMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
		Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,compositeStatementObjectMapping.getFragment1().getLocationInfo());
		Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,compositeStatementObjectMapping.getFragment2().getLocationInfo());
		// TODO: 8/2/2022 Need to rethink regarding the logic asap, with this logic , we might see a huge drop in performance
		//if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
		//{
		//	mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
		//}
		//else
		{
			mappingStore.addMapping(srcStatementNode,dstStatementNode);
			if ((srcStatementNode.getType().name.equals(Constants.TRY_STATEMENT) && dstStatementNode.getType().name.equals(Constants.TRY_STATEMENT)) ||
					(srcStatementNode.getType().name.equals(Constants.CATCH_CLAUSE) && dstStatementNode.getType().name.equals(Constants.CATCH_CLAUSE))) {
				matchBlocks(srcStatementNode, dstStatementNode, mappingStore);
				new CompositeMatcher(abstractCodeMapping).match(srcStatementNode,dstStatementNode,mappingStore);
			} else if (!srcStatementNode.getType().name.equals(Constants.BLOCK) && !dstStatementNode.getType().name.equals(Constants.BLOCK)) {
				new CompositeMatcher(abstractCodeMapping).match(srcStatementNode, dstStatementNode, mappingStore);
			}
		}
	}

	private void processLeafMapping(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore, boolean isPartOfExtractedMethod) {
		LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
		Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo());
		Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo());
		if (srcStatementNode == null || dstStatementNode == null) {
			System.err.println("Tree not found for " + abstractCodeMapping);
			return;
		}
		if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
			mappingStore.addMapping(srcStatementNode, dstStatementNode);

		if (abstractCodeMapping.getFragment1() instanceof AbstractExpression || abstractCodeMapping.getFragment2() instanceof AbstractExpression) {
			lastStepMappings.add(abstractCodeMapping);
		} else {
			new LeafMatcher().match(srcStatementNode,dstStatementNode,mappingStore);
			additionallyMatchedStatements(srcTree, dstTree, srcStatementNode, dstStatementNode, abstractCodeMapping, mappingStore);
		}
		optimizeVariableDeclarations(abstractCodeMapping);
		if (!isPartOfExtractedMethod && srcStatementNode.getType().name.equals(Constants.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(Constants.RETURN_STATEMENT)) {
			finalOptimization.addMapping(srcStatementNode,dstStatementNode);
		}
	}

	private void optimizeVariableDeclarations(AbstractCodeMapping abstractCodeMapping) {
		List<VariableDeclaration> variableDeclarations1 = abstractCodeMapping.getFragment1().getVariableDeclarations();
		List<VariableDeclaration> variableDeclarations2 = abstractCodeMapping.getFragment2().getVariableDeclarations();
		if (variableDeclarations1.size() == 1 && variableDeclarations2.size() == 0){
			if (variableDeclarations1.get(0).getInitializer() != null)
				if (abstractCodeMapping.getFragment2().toString().contains(variableDeclarations1.get(0).getInitializer().toString()))
					lastStepMappings.add(new LeafMapping(variableDeclarations1.get(0).getInitializer(), abstractCodeMapping.getFragment2(),null,null));
		}
		if (variableDeclarations1.size() == 0 && variableDeclarations2.size() == 1){
			if (variableDeclarations2.get(0).getInitializer() != null)
				if (abstractCodeMapping.getFragment1().toString().contains(variableDeclarations2.get(0).getInitializer().toString()))
					lastStepMappings.add(new LeafMapping(abstractCodeMapping.getFragment1(),variableDeclarations2.get(0).getInitializer(),null,null));
		}
	}

	private static void additionallyMatchedStatements(Tree srcTree, Tree dstTree, Tree srcStatementNode, Tree dstStatementNode, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		if (abstractCodeMapping != null) {
			for (Replacement replacement : abstractCodeMapping.getReplacements()) {
				if (replacement instanceof CompositeReplacement) {
					CompositeReplacement compositeReplacement = (CompositeReplacement) replacement;
					if (compositeReplacement.getAdditionallyMatchedStatements1().size() > 0) {
						for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements1()) {
							Tree srcAdditionalTree = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeFragment.getLocationInfo());
							new LeafMatcher().match(srcAdditionalTree, dstStatementNode, mappingStore);
						}
					} else if (compositeReplacement.getAdditionallyMatchedStatements2().size() > 0) {
						for (AbstractCodeFragment abstractCodeFragment : compositeReplacement.getAdditionallyMatchedStatements2()) {
							Tree dstAdditionalTree = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeFragment.getLocationInfo());
							new LeafMatcher().match(srcStatementNode, dstAdditionalTree, mappingStore);
						}
					} else {

					}

				}
			}
		}
	}

	public void processLeafMatcherForExtractedOrInlinedVariables(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		LeafMapping leafMapping = (LeafMapping) abstractCodeMapping;
		Tree srcStatementNode = TreeUtilFunctions.findByLocationInfo(srcTree,leafMapping.getFragment1().getLocationInfo());
		Tree dstStatementNode = TreeUtilFunctions.findByLocationInfo(dstTree,leafMapping.getFragment2().getLocationInfo());
		new LeafMatcher().match(srcStatementNode,dstStatementNode,mappingStore);
	}

	private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
			Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
			Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
			if (srcClassAnnotationTree.isIsoStructuralTo(dstClassAnnotationTree))
				mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
		}
	}

	private void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
		String searchingType = Constants.BLOCK;
		Pair<Tree, Tree> matched = matchBasedOnType(srcStatementNode,dstStatementNode, searchingType);
		if (matched != null)
			mappingStore.addMapping(matched.first,matched.second);
	}

	private void processComments(Pair<List<Tree>, List<Tree>> addedCommentsPair, ExtendedMultiMappingStore mappingStore) {
		List<Tree> srcComments = addedCommentsPair.first;
		List<Tree> dstComments = addedCommentsPair.second;
		Map<Tree,List<Tree>> candidates = new HashMap<>();
		for (Tree srcComment : srcComments) {
			List<Tree> candidateList = new ArrayList<>();
			for (Tree dstComment : dstComments) {
				if (srcComment.getMetrics().hash == dstComment.getMetrics().hash)
					candidateList.add(dstComment);
			}
			if (!candidateList.isEmpty())
				candidates.put(srcComment,candidateList);
		}
		for (Map.Entry<Tree,List<Tree>> entry : candidates.entrySet()) {
			Tree srcTree = entry.getKey();
			List<Tree> matches = entry.getValue();
			if (matches.size() == 1) {
				mappingStore.addMappingRecursively(srcTree, matches.get(0));
			} else {
				//TODO: ignore at the moment
			}
		}
	}

	/*
    private Pair<List<Tree> , List<Tree>> addComments(TreeContext first, TreeContext second) {
        CommentVisitor firstCommentVisitor = new CommentVisitor(first);
        firstCommentVisitor.addCommentToProperSubtree();
        CommentVisitor secondCommentVisitor = new CommentVisitor(second);
        secondCommentVisitor.addCommentToProperSubtree();
        return new Pair<>(firstCommentVisitor.getComments(),secondCommentVisitor.getComments());
    }
    */
	private void processJavaDocs(Tree srcTree, Tree dstTree, UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc, ExtendedMultiMappingStore mappingStore) {
		if (srcUMLJavaDoc != null && dstUMLJavaDoc != null) {
			Tree srcJavaDocNode = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLJavaDoc.getLocationInfo());
			Tree dstJavaDocNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLJavaDoc.getLocationInfo());
			if (srcUMLJavaDoc.equalText(dstUMLJavaDoc)) {
				mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
			} else {
				new BasicTreeMatcher().match(srcJavaDocNode,dstJavaDocNode,mappingStore);
			}
		}
	}

	private void processOperationDiff(Tree srcTree, Tree dstTree, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
		UMLOperationDiff umlOperationDiff = umlOperationBodyMapper.getOperationSignatureDiff().isPresent() ? umlOperationBodyMapper.getOperationSignatureDiff().get() : null;
		if (umlOperationDiff == null) return;
		UMLTypeParameterListDiff umlTypeParameterListDiff = umlOperationDiff.getTypeParameterListDiff();
		for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : umlTypeParameterListDiff.getCommonTypeParameters()) {
			Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTree, commonTypeParamSet.getLeft().getLocationInfo());
			Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTree, commonTypeParamSet.getRight().getLocationInfo());
			mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
		}
		for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation>  umlAnnotationUMLAnnotationPair : umlOperationDiff.getAnnotationListDiff().getCommonAnnotations()) {
			Tree srcClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree , umlAnnotationUMLAnnotationPair.getLeft().getLocationInfo());
			Tree dstClassAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, umlAnnotationUMLAnnotationPair.getRight().getLocationInfo());
			mappingStore.addMappingRecursively(srcClassAnnotationTree,dstClassAnnotationTree);
		}
		Set<org.apache.commons.lang3.tuple.Pair<UMLType, UMLType>> commonExceptionTypes = umlOperationDiff.getCommonExceptionTypes();
		if (commonExceptionTypes != null) {
			for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> matchedException : commonExceptionTypes) {
				Tree srcExceptionNode =TreeUtilFunctions.findByLocationInfo(srcTree, matchedException.getLeft().getLocationInfo());
				Tree dstExceptionNode =TreeUtilFunctions.findByLocationInfo(dstTree, matchedException.getRight().getLocationInfo());
				mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
			}
		}
		if (umlOperationDiff.getRemovedOperation().getReturnParameter() != null && umlOperationDiff.getAddedOperation().getReturnParameter() != null ) {
			LocationInfo srcLocationInfo = umlOperationDiff.getRemovedOperation().getReturnParameter().getType().getLocationInfo();
			LocationInfo dstLocationInfo = umlOperationDiff.getAddedOperation().getReturnParameter().getType().getLocationInfo();
			Tree srcNode =TreeUtilFunctions.findByLocationInfo(srcTree, srcLocationInfo);
			Tree dstNode =TreeUtilFunctions.findByLocationInfo(dstTree, dstLocationInfo);
			if (srcNode.isIsoStructuralTo(dstNode))
				mappingStore.addMappingRecursively(srcNode,dstNode);
		}
	}

	private void processRefactorings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, ExtendedMultiMappingStore mappingStore){
		for (Refactoring refactoring : refactoringList) {
			if (refactoring instanceof ReplaceLoopWithPipelineRefactoring) {
				//TODO: Kinda completed but assertations must be removed
				ReplaceLoopWithPipelineRefactoring replaceLoopWithPipelineRefactoring = (ReplaceLoopWithPipelineRefactoring) refactoring;
				assert replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().size() == 1;
				AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().iterator().next();
				List<LambdaExpressionObject> lambdas = next.getLambdas();
				AbstractCodeFragment enhancedFor = null;
				for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore()) {
					if (abstractCodeFragment instanceof CompositeStatementObject && ((CompositeStatementObject)abstractCodeFragment).isLoop()) {
						enhancedFor = abstractCodeFragment;
						break;
					}
				}
				assert enhancedFor != null;
				for (LambdaExpressionObject lambda : lambdas) {
					for (VariableDeclaration parameter : lambda.getParameters()) {
						String variableName = parameter.getVariableName();
						VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
						if (variableDeclaration != null) {
							Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
							Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,parameter.getLocationInfo());
							new LeafMatcher().match(srcNode,dstNode,mappingStore);
						}
					}
				}
				Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,enhancedFor.getLocationInfo());
				Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,next.getLocationInfo());
				mappingStore.addMapping(srcSt,dstSt);
			} else if (refactoring instanceof ReplacePipelineWithLoopRefactoring) {
				//TODO : ongoing problem
				ReplacePipelineWithLoopRefactoring replaceLoopWithPipelineRefactoring = (ReplacePipelineWithLoopRefactoring) refactoring;
				assert replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().size() == 1;
				AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().iterator().next();
				List<LambdaExpressionObject> lambdas = next.getLambdas();
				AbstractCodeFragment enhancedFor = null;
				for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter()) {
					if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
						enhancedFor = abstractCodeFragment;
						break;
					}
				}
				assert enhancedFor != null;
				for (LambdaExpressionObject lambda : lambdas) {
					for (VariableDeclaration parameter : lambda.getParameters()) {
						String variableName = parameter.getVariableName();
						VariableDeclaration variableDeclaration = enhancedFor.getVariableDeclaration(variableName);
						if (variableDeclaration == null)
							continue;
						Tree srcNode = TreeUtilFunctions.findByLocationInfo(srcTree,parameter.getLocationInfo());
						Tree dstNode = TreeUtilFunctions.findByLocationInfo(dstTree,variableDeclaration.getLocationInfo());
						new LeafMatcher().match(srcNode,dstNode,mappingStore);
					}
				}
				Tree srcSt = TreeUtilFunctions.findByLocationInfo(srcTree,next.getLocationInfo());
				Tree dstSt = TreeUtilFunctions.findByLocationInfo(dstTree,enhancedFor.getLocationInfo());
				mappingStore.addMapping(srcSt,dstSt);
			} else if (refactoring instanceof MergeOperationRefactoring) {
				MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
				for(UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
					processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, false);
				}
			} else if (refactoring instanceof SplitOperationRefactoring) {
				SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
				for(UMLOperationBodyMapper bodyMapper : splitOperationRefactoring.getMappers()) {
					processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, false);
				}
			} else if (refactoring instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
				UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
				processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, true);
				for(AbstractCodeMapping expressionMapping : extractOperationRefactoring.getArgumentMappings()) {
					lastStepMappings.add(expressionMapping);
				}
			} else if (refactoring instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
				UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
				processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, false);
				for(AbstractCodeMapping expressionMapping : inlineOperationRefactoring.getArgumentMappings()) {
					lastStepMappings.add(expressionMapping);
				}
			} else if (refactoring instanceof MoveCodeRefactoring) {
				MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
				UMLOperationBodyMapper bodyMapper = moveCodeRefactoring.getBodyMapper();
				processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, false);
			} else if (refactoring instanceof ParameterizeTestRefactoring) {
				ParameterizeTestRefactoring parameterizeTestRefactoring = (ParameterizeTestRefactoring) refactoring;
				UMLOperationBodyMapper bodyMapper = parameterizeTestRefactoring.getBodyMapper();
				processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, false);
			} else if (refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
				for(LeafMapping mapping : extractVariableRefactoring.getSubExpressionMappings()) {
					processLeafMatcherForExtractedOrInlinedVariables(srcTree,dstTree,mapping,mappingStore);
				}
			} else if (refactoring instanceof InlineVariableRefactoring) {
				InlineVariableRefactoring inlineVariableRefactoring = (InlineVariableRefactoring) refactoring;
				for(LeafMapping mapping : inlineVariableRefactoring.getSubExpressionMappings()) {
					processLeafMatcherForExtractedOrInlinedVariables(srcTree,dstTree,mapping,mappingStore);
				}
			} else if (refactoring instanceof ReplaceAttributeRefactoring) {
				//TODO:
				ReplaceAttributeRefactoring replaceAttributeRefactoring = (ReplaceAttributeRefactoring) refactoring;
			} else if (refactoring instanceof InlineAttributeRefactoring) {
				InlineAttributeRefactoring inlineAttributeRefactoring = (InlineAttributeRefactoring) refactoring;
				Tree srcAttrDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, inlineAttributeRefactoring.getVariableDeclaration().getLocationInfo());
				for (AbstractCodeMapping reference : inlineAttributeRefactoring.getReferences()) {
					Tree dstStatementTree = TreeUtilFunctions.findByLocationInfo(dstTree,reference.getFragment2().getLocationInfo());
					new LeafMatcher().match(srcAttrDeclaration,dstStatementTree,mappingStore);
				}
			} else if (refactoring instanceof ExtractAttributeRefactoring) {
				ExtractAttributeRefactoring extractAttributeRefactoring = (ExtractAttributeRefactoring) refactoring;
				Tree dstAttrDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree, extractAttributeRefactoring.getVariableDeclaration().getLocationInfo());
				for (AbstractCodeMapping reference : extractAttributeRefactoring.getReferences()) {
					Tree srcStatementTree = TreeUtilFunctions.findByLocationInfo(srcTree,reference.getFragment1().getLocationInfo());
					new LeafMatcher().match(srcStatementTree,dstAttrDeclaration,mappingStore);
				}
				for (UMLAnonymousClassDiff umlAnonymousClassDiff : extractAttributeRefactoring.getAnonymousClassDiffList()) {
					processAnonymousClassDiff(srcTree,dstTree,umlAnonymousClassDiff,mappingStore);
				}
			} else if (refactoring instanceof MergeVariableRefactoring) {
				MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
				Set<VariableDeclaration> mergedVariables = mergeVariableRefactoring.getMergedVariables();
				VariableDeclaration newVariable = mergeVariableRefactoring.getNewVariable();
				Tree dstVariableType =TreeUtilFunctions.findByLocationInfo(dstTree,newVariable.getType().getLocationInfo());
				Tree dstVariableDeclaration =TreeUtilFunctions.findByLocationInfo(dstTree,newVariable.getLocationInfo());
				List<Tree> dstChildrenList = dstVariableDeclaration.getChildren();
				Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
				for (VariableDeclaration variableDeclaration : mergedVariables) {
					Tree srcVariableDeclaration =TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getLocationInfo());
					Tree srcVariableType =TreeUtilFunctions.findByLocationInfo(srcTree,variableDeclaration.getType().getLocationInfo());
					List<Tree> srcChildrenList = srcVariableDeclaration.getChildren();
					Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
//					mappingStore.addMapping(srcVariableDeclaration,dstVariableDeclaration);
//					mappingStore.addMapping(srcVariableType,dstVariableType.getChild(0));
					mappingStore.addMapping(srcVarName,dstVarName);
				}
			} else if (refactoring instanceof SplitConditionalRefactoring) {
				SplitConditionalRefactoring splitConditionalRefactoring = (SplitConditionalRefactoring) refactoring;
				for(LeafMapping leafMapping : splitConditionalRefactoring.getSubExpressionMappings()) {
					lastStepMappings.add(leafMapping);
				}
				/*
				Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,splitConditionalRefactoring.getOriginalConditional().getLocationInfo());
				Set<AbstractCodeFragment> splitConditionals = splitConditionalRefactoring.getSplitConditionals();
				for (AbstractCodeFragment splitConditional : splitConditionals) {
					Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,splitConditional.getLocationInfo());
					new GeneralMatcher(splitConditionalRefactoring.getOriginalConditional(), splitConditional).
							match(srcSubTree,dstSubTree,mappingStore);
				}
				*/
			} else if (refactoring instanceof MergeConditionalRefactoring) {
				MergeConditionalRefactoring mergeConditionalRefactoring = (MergeConditionalRefactoring) refactoring;
				for(LeafMapping leafMapping : mergeConditionalRefactoring.getSubExpressionMappings()) {
					lastStepMappings.add(leafMapping);
				}
				/*
				Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,mergeConditionalRefactoring.getNewConditional().getLocationInfo());
				Set<AbstractCodeFragment> mergedConditionals = mergeConditionalRefactoring.getMergedConditionals();
				for (AbstractCodeFragment eachMerged : mergedConditionals) {
					Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,eachMerged.getLocationInfo());
					new GeneralMatcher(eachMerged, mergeConditionalRefactoring.getNewConditional())
							.match(srcSubTree,dstSubTree,mappingStore);
				}
				*/
			} else if (refactoring instanceof MergeCatchRefactoring) {
				MergeCatchRefactoring mergeCatchRefactoring = (MergeCatchRefactoring) refactoring;
				Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,mergeCatchRefactoring.getNewCatchBlock().getLocationInfo());
				for (AbstractCodeFragment eachMerged : mergeCatchRefactoring.getMergedCatchBlocks()) {
					Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,eachMerged.getLocationInfo());
					new GeneralMatcher(eachMerged, mergeCatchRefactoring.getNewCatchBlock())
							.match(srcSubTree,dstSubTree,mappingStore);
				}
			} else if (refactoring instanceof RenameVariableRefactoring) {
				RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
				VariableDeclaration originalVariable = renameVariableRefactoring.getOriginalVariable();
				VariableDeclaration renamedVariable = renameVariableRefactoring.getRenamedVariable();
				Tree srcInput = TreeUtilFunctions.findByLocationInfo(srcTree,originalVariable.getLocationInfo());
				Tree dstInput = TreeUtilFunctions.findByLocationInfo(dstTree, renamedVariable.getLocationInfo());
				Tree srcType, dstType, srcName, dstName;

				if (srcInput == null || dstInput == null) continue;
				boolean eligible = true;
				switch (renameVariableRefactoring.getRefactoringType()) {
					case REPLACE_VARIABLE_WITH_ATTRIBUTE:
						srcInput = TreeUtilFunctions.getParentUntilType(srcInput,Constants.VARIABLE_DECLARATION_STATEMENT);
						dstInput = TreeUtilFunctions.getParentUntilType(dstInput,Constants.FIELD_DECLARATION);
						//TODO: need more cases to generalize the logic
						break;
					case RENAME_PARAMETER:
						eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
						break;
					case RENAME_VARIABLE:
						Set<AbstractCodeMapping> references = renameVariableRefactoring.getReferences();
						for (AbstractCodeMapping abstractCodeMapping : references) {
							if (abstractCodeMapping instanceof LeafMapping)
								findVariablesAndMatch(srcTree,dstTree,abstractCodeMapping,renameVariableRefactoring.getOriginalVariable().getVariableName(),renameVariableRefactoring.getRenamedVariable().getVariableName(),mappingStore);
						}
						eligible = false;
						break;
					case PARAMETERIZE_VARIABLE:
						eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
						if (!eligible)
							break;
						srcType = TreeUtilFunctions.findByLocationInfo(srcTree, originalVariable.getType().getLocationInfo());
						dstType = TreeUtilFunctions.findByLocationInfo(dstTree, renamedVariable.getType().getLocationInfo());
						new LeafMatcher().match(srcType,dstType,mappingStore);
						if (srcInput.getChildren().size() > 0) {
							srcName = srcInput.getChild(0);
							if (srcName.getType().name.equals(Constants.SIMPLE_NAME)) {
								for (Tree dstChild : dstInput.getChildren()) {
									if (dstChild.getType().name.equals(Constants.SIMPLE_NAME)) {
										mappingStore.addMapping(srcName,dstChild);
										break;
									}
								}
							}
						}
						eligible = false;
						break;
					case LOCALIZE_PARAMETER:
						eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
						if (!eligible)
							break;
						srcType = TreeUtilFunctions.findByLocationInfo(srcTree, originalVariable.getType().getLocationInfo());
						dstType = TreeUtilFunctions.findByLocationInfo(dstTree, renamedVariable.getType().getLocationInfo());
						new LeafMatcher().match(srcType,dstType,mappingStore);
						if (dstInput.getChildren().size() > 0) {
							dstName = dstInput.getChild(0);
							if (dstName.getType().name.equals(Constants.SIMPLE_NAME)) {
								for (Tree srcChild : srcInput.getChildren()) {
									if (srcChild.getType().name.equals(Constants.SIMPLE_NAME)) {
										mappingStore.addMapping(srcChild,dstName);
										break;
									}
								}
							}
						}
						eligible = false;
						break;
					default:
						eligible = false;
				}
				if (eligible) new LeafMatcher().match(
						srcInput,
						dstInput,
						mappingStore);
			}
		}
	}

	private void findVariablesAndMatch(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, String originalVariableName, String renamedVariableName, ExtendedMultiMappingStore mappingStore) {
		Tree srcStatement = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeMapping.getFragment1().getLocationInfo());
		Tree dstStatement = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeMapping.getFragment2().getLocationInfo());
		Tree srcName = TreeUtilFunctions.findVariable(srcStatement ,originalVariableName);
		Tree dstName = TreeUtilFunctions.findVariable(dstStatement ,renamedVariableName);
		if (srcName != null & dstName != null) {
			optimizationMappingStore.addMapping(srcName,dstName);
		}

	}

	private void processClassImplementedInterfaces(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		List<UMLType> srcImplementedInterfaces = classDiff.getOriginalClass().getImplementedInterfaces();
		List<UMLType> dstImplementedInterfaces = classDiff.getNextClass().getImplementedInterfaces();
		List<UMLType> removedOnes = classDiff.getRemovedImplementedInterfaces();
		for (UMLType srcUmlType : srcImplementedInterfaces) {
			if (!removedOnes.contains(srcUmlType)) {
				Tree srcInterfaceTree =TreeUtilFunctions.findByLocationInfo(srcTree,srcUmlType.getLocationInfo());
				for (UMLType dstUmlType : dstImplementedInterfaces) {
					if (dstUmlType.getClassType().equals(srcUmlType.getClassType())) {
						Tree dstInterfaceTree =TreeUtilFunctions.findByLocationInfo(dstTree,dstUmlType.getLocationInfo());
						mappingStore.addMappingRecursively(srcInterfaceTree, dstInterfaceTree);
						break;
					}
				}
			}
		}
	}

	private void processClassAttributes(Tree srcTree, Tree dstTree, UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		Set<org.apache.commons.lang3.tuple.Pair<UMLAttribute, UMLAttribute>> pairs = classDiff.getCommonAtrributes();
		for (org.apache.commons.lang3.tuple.Pair<UMLAttribute, UMLAttribute> pair : pairs) {
			processFieldDeclaration(srcTree,dstTree,pair.getLeft(),pair.getRight(),mappingStore);
		}
		List<UMLAttributeDiff> attributeDiffList = classDiff.getAttributeDiffList();
		for (UMLAttributeDiff umlAttributeDiff : attributeDiffList) {
			processFieldDeclarationByAttrDiff(srcTree,dstTree,umlAttributeDiff,mappingStore);
		}
		List<UMLEnumConstantDiff> enumConstantDiffList = classDiff.getEnumConstantDiffList();
		for (UMLEnumConstantDiff enumConstantDiff : enumConstantDiffList) {
			processFieldDeclarationByEnumConstantDiff(srcTree,dstTree,enumConstantDiff,mappingStore);
		}
	}

	private void processFieldDeclarationByAttrDiff(Tree srcTree, Tree dstTree, UMLAttributeDiff umlAttributeDiff, ExtendedMultiMappingStore mappingStore) {
		processFieldDeclaration(srcTree,dstTree,umlAttributeDiff.getRemovedAttribute(),umlAttributeDiff.getAddedAttribute(),mappingStore);
		if (umlAttributeDiff.getInitializerMapper().isPresent()) {
			UMLOperationBodyMapper umlOperationBodyMapper = umlAttributeDiff.getInitializerMapper().get();
			processMethod(srcTree, dstTree, umlOperationBodyMapper, mappingStore);
		}
	}

	private void processFieldDeclarationByEnumConstantDiff(Tree srcTree, Tree dstTree, UMLEnumConstantDiff umlEnumConstantDiff, ExtendedMultiMappingStore mappingStore) {
		processFieldDeclaration(srcTree,dstTree,umlEnumConstantDiff.getRemovedEnumConstant(),umlEnumConstantDiff.getAddedEnumConstant(),mappingStore);
		if(umlEnumConstantDiff.getAnonymousClassDiff().isPresent()) {
			UMLAnonymousClassDiff anonymousClassDiff = umlEnumConstantDiff.getAnonymousClassDiff().get();
			processAnonymousClassDiff(srcTree, dstTree, anonymousClassDiff, mappingStore);
		}
	}

	private void processAnonymousClassDiff(Tree srcTree, Tree dstTree, UMLAnonymousClassDiff anonymousClassDiff, ExtendedMultiMappingStore mappingStore) {
		List<UMLOperationBodyMapper> operationBodyMapperList = anonymousClassDiff.getOperationBodyMapperList();
		for (UMLOperationBodyMapper umlOperationBodyMapper : operationBodyMapperList) {
			processMethod(srcTree, dstTree,umlOperationBodyMapper, mappingStore);
		}
	}

	private List<Pair<UMLAttribute, UMLAttribute>> findMatchedAttributesPair(UMLClassBaseDiff classDiff) {
		List<Pair<UMLAttribute,UMLAttribute>> pairs = new ArrayList<>();
		List<UMLAttribute> srcAttributes = classDiff.getOriginalClass().getAttributes();
		List<UMLAttribute> dstAttributes = classDiff.getNextClass().getAttributes();
		List<UMLAttribute> removedOnes = classDiff.getRemovedAttributes();
		for (UMLAttribute srcUmlType : srcAttributes) {
			if (!removedOnes.contains(srcUmlType)) {
				for (UMLAttribute dstUmlType : dstAttributes) {
					if (dstUmlType.getName().equals(srcUmlType.getName())) {
						pairs.add(new Pair<>(srcUmlType,dstUmlType));
						break;
					}
				}
			}
		}
		return pairs;
	}

	private void processFieldDeclaration(Tree srcTree, Tree dstTree, UMLAttribute srcUMLAttribute,UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {

		Tree srcAttr = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLAttribute.getLocationInfo());
		Tree dstAttr = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLAttribute.getLocationInfo());
		Tree srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.FIELD_DECLARATION);
		Tree dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.FIELD_DECLARATION);
		if (srcFieldDeclaration == null) {
			srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.ENUM_CONSTANT_DECLARATION);
		}
		if (dstFieldDeclaration == null) {
			dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.ENUM_CONSTANT_DECLARATION);
		}
		if (srcFieldDeclaration.getMetrics().hash == dstFieldDeclaration.getMetrics().hash
//				|| srcFieldDeclaration.isIsoStructuralTo(dstFieldDeclaration))
		) {
			// TODO: 8/3/2022 isoStructural can't be a good idea here, i.e anonymous class
			mappingStore.addMappingRecursively(srcFieldDeclaration,dstFieldDeclaration);
			return;
		}
		if (srcAttr.getMetrics().hash == dstAttr.getMetrics().hash) {
			mappingStore.addMappingRecursively(srcAttr,dstAttr);
		}

		mappingStore.addMapping(srcFieldDeclaration,dstFieldDeclaration);
		matchFieldAllModifiers(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute,dstUMLAttribute,mappingStore);
		if (srcUMLAttribute.getType().getLocationInfo() == null || dstUMLAttribute.getType().getLocationInfo() == null) {
			if (srcUMLAttribute instanceof UMLEnumConstant && dstUMLAttribute instanceof UMLEnumConstant) {
				//TODO: JavaDocs are mapped as well.
				new LeafMatcher().match(srcAttr,dstAttr,mappingStore);
				return;
			}
		}
		Tree srcType = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getType().getLocationInfo());
		Tree dstType = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getType().getLocationInfo());
		if (srcType.isIsoStructuralTo(dstType)) mappingStore.addMappingRecursively(srcType,dstType);
		Tree srcVarDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getVariableDeclaration().getLocationInfo());
		Tree dstVarDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getVariableDeclaration().getLocationInfo());
		mappingStore.addMapping(srcVarDeclaration,dstVarDeclaration);
		new LeafMatcher().match(srcVarDeclaration,dstVarDeclaration,mappingStore);
		processJavaDocs(srcTree,dstTree,srcUMLAttribute.getJavadoc(),dstUMLAttribute.getJavadoc(),mappingStore);
		mappingStore.addMapping(srcVarDeclaration.getChild(0),dstVarDeclaration.getChild(0));
	}

	private void matchFieldAllModifiers(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {
		//Pair<Tree, Tree> attributeAccessModifierPair = findAttributeAccessModifierPair(srcFieldDeclaration, dstFieldDeclaration, srcUMLAttribute, dstUMLAttribute);
		//if (attributeAccessModifierPair.first != null && attributeAccessModifierPair.second != null)
		//	mappingStore.addMapping(attributeAccessModifierPair.first, attributeAccessModifierPair.second);

//		if (srcUMLAttribute.getVisibility().equals(dstUMLAttribute.getVisibility()))
		matchModifiersForField(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute.getVisibility().toString(),dstUMLAttribute.getVisibility().toString(),mappingStore);
		if (srcUMLAttribute.isFinal() && dstUMLAttribute.isFinal())
			matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.FINAL,mappingStore);
		if (srcUMLAttribute.isVolatile() && dstUMLAttribute.isVolatile())
			matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.VOLATILE,mappingStore);
		if (srcUMLAttribute.isStatic() && dstUMLAttribute.isStatic())
			matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.STATIC,mappingStore);
		if (srcUMLAttribute.isTransient() && dstUMLAttribute.isTransient())
			matchModifierForField(srcFieldDeclaration,dstFieldDeclaration,Constants.TRANSIENT,mappingStore);
	}

	private void matchModifiersForField(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String srcModifier, String dstModifier, ExtendedMultiMappingStore mappingStore) {
		Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, srcModifier);
		Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, dstModifier);
		if (srcModifierTree != null && dstModifierTree != null)
			mappingStore.addMapping(srcModifierTree,dstModifierTree);
	}

	private void matchModifierForField(Tree srcFieldDeclaration, Tree dstFieldDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
		Tree srcModifierTree = findAttributeModifierByLabel(srcFieldDeclaration, modifier);
		Tree dstModifierTree = findAttributeModifierByLabel(dstFieldDeclaration, modifier);
		if (srcModifierTree != null && dstModifierTree != null)
			mappingStore.addMapping(srcModifierTree,dstModifierTree);
	}

	private Pair<Tree, Tree> findAttributeAccessModifierPair(Tree srcFieldDeclaration, Tree dstFieldDeclaration, UMLAttribute srcUMLAttribute, UMLAttribute dstUMLAttribute) {
		Tree srcAccessModifier = findAttributeAccessModifier(srcFieldDeclaration);
		Tree dstAccessModifier = findAttributeAccessModifier(dstFieldDeclaration);
		return new Pair<>(srcAccessModifier,dstAccessModifier);
	}

	private Tree findAttributeAccessModifier(Tree anyFieldDeclaration) {
		if (anyFieldDeclaration.getChildren().size() > 0) {
			for (Tree child : anyFieldDeclaration.getChildren()) {
				if (child.getType().name.equals(Constants.ACCESS_MODIFIER))
					return child;
				if (child.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT))
					break;
			}
		}
		return null;
	}

	private Tree findAttributeModifierByLabel(Tree anyFieldDeclaration,String label) {
		if (anyFieldDeclaration.getChildren().size() > 0) {
			for (Tree child : anyFieldDeclaration.getChildren()) {
				if (child.getLabel().equals(label))
					return child;
				if (child.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT))
					break;
			}
		}
		return null;
	}

	private Tree findAttributeTreeByType(Tree anyFieldDeclaration,String type) {
		if (anyFieldDeclaration.getChildren().size() > 0) {
			for (Tree child : anyFieldDeclaration.getChildren()) {
				if (child.getType().name.equals(type))
					return child;
				if (child.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT))
					break;
			}
		}
		return null;
	}

	private void processSuperClass(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		UMLType srcParentUML = classDiff.getOldSuperclass();
		UMLType dstParentUML = classDiff.getNewSuperclass();
		if (srcParentUML != null && dstParentUML != null) {
			Tree srcParentClassTree =TreeUtilFunctions.findByLocationInfo(srcTree, srcParentUML.getLocationInfo());
			Tree dstParentClassTree =TreeUtilFunctions.findByLocationInfo(dstTree, dstParentUML.getLocationInfo());
			if (srcParentClassTree != null && dstParentClassTree != null)
				if (srcParentClassTree.isIsoStructuralTo(dstParentClassTree))
					mappingStore.addMappingRecursively(srcParentClassTree,dstParentClassTree);
		}
	}

	private void processPackageDeclaration(Tree srcTree, Tree dstTree, UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		//TODO: In current implementation, I assumed that these two package-statements are matched since they both belong to the same class
		//TODO : Question: Can single file have multiple package declaration? if yes, I have to use list of pairs
		Tree srcPackageDeclaration = findPackageDeclaration(srcTree);
		Tree dstPackageDeclaration = findPackageDeclaration(dstTree);
		if (srcPackageDeclaration != null && dstPackageDeclaration != null)
			mappingStore.addMappingRecursively(srcPackageDeclaration,dstPackageDeclaration);
	}

	private Tree findPackageDeclaration(Tree inputTree) {
		String searchingType = Constants.PACKAGE_DECLARATION;
		if (!inputTree.getChildren().isEmpty()) {
			List<Tree> children = inputTree.getChildren();
			for(Tree child: children) {
				if (child.getType().name.equals(searchingType))
					return child;
			}
		}
		return null;
	}

	private void processImports(Tree srcTree, Tree dstTree, UMLImportListDiff importDiffList, ExtendedMultiMappingStore mappingStore) {
		if (importDiffList == null) return;
		Set<UMLImport> commonImports = importDiffList.getCommonImports();
		if (commonImports.isEmpty())
			return;
		String searchingType = Constants.IMPORT_DECLARATION;
		List<Tree> srcChildren = srcTree.getChildren();
		List<Tree> dstChildren = dstTree.getChildren();

		for(UMLImport label : commonImports){
			Tree srcImportStatement = findImportByTypeAndLabel(srcChildren,searchingType,label);
			Tree dstImportStatement = findImportByTypeAndLabel(dstChildren,searchingType,label);
			if (srcImportStatement != null && dstImportStatement != null)
				mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
		}
		//Grouped Imports
		for (Map.Entry<Set<UMLImport>, UMLImport> setUMLImportEntry : importDiffList.getGroupedImports().entrySet()) {
			Set<UMLImport> srcImportSet = setUMLImportEntry.getKey();
			UMLImport dstImport = setUMLImportEntry.getValue();
			Tree dstImportStatement = findImportByTypeAndLabel(dstChildren,searchingType,dstImport);
			for (UMLImport srcUMLImport : srcImportSet) {
				Tree srcImportStatement = findImportByTypeAndLabel(srcChildren,searchingType,srcUMLImport);
				mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
			}
		}
		//UnGrouped Imports
		for (Map.Entry<UMLImport, Set<UMLImport>> umlImportSetEntry : importDiffList.getUnGroupedImports().entrySet()) {
			UMLImport srcImport = umlImportSetEntry.getKey();
			Set<UMLImport> dstImportSet = umlImportSetEntry.getValue();
			Tree srcImportStatement = findImportByTypeAndLabel(srcChildren,searchingType,srcImport);
			for (UMLImport dstUMLImport : dstImportSet) {
				Tree dstImportStatement = findImportByTypeAndLabel(dstChildren,searchingType,dstUMLImport);
				mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
			}
		}
	}

	private Tree findImportByTypeAndLabel(List<Tree> inputTree, String searchingType, UMLImport label) {
		for (Tree srcStatement: inputTree) {
			if (srcStatement.getType().name.equals(searchingType)) {
				if (srcStatement.getChild(0).getLabel().equals(label.getName())) //TODO getChild 0 will cause a lot of problem
					if (label.isOnDemand()) {
						if (srcStatement.getChild(0).getEndPos() + 3 == srcStatement.getEndPos()) {
							return srcStatement;
						}
					} else {
						return srcStatement;
					}
			}
		}
		return null;
	}

	private void processMethodSignature(Tree srcOperationNode, Tree dstOperationNode, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
		List<String> searchingTypes = new ArrayList<>();
		//searchingTypes.add(Constants.ACCESS_MODIFIER);
		searchingTypes.add(Constants.SIMPLE_NAME);
		searchingTypes.add(Constants.PRIMITIVE_TYPE);
		searchingTypes.add(Constants.BLOCK);
		for (String type : searchingTypes) {
			Pair<Tree,Tree> matched = matchBasedOnType(srcOperationNode,dstOperationNode,type);
			if (matched != null)
				mappingStore.addMapping(matched.first,matched.second);
		}
		if (umlOperationBodyMapper.getOperation1() != null && umlOperationBodyMapper.getOperation2() != null) {
			if (umlOperationBodyMapper.getOperation1().isStatic() && umlOperationBodyMapper.getOperation2().isStatic())
				matchModifier(srcOperationNode, dstOperationNode, Constants.STATIC, mappingStore);
			if (umlOperationBodyMapper.getOperation1().isFinal() && umlOperationBodyMapper.getOperation2().isFinal())
				matchModifier(srcOperationNode, dstOperationNode, Constants.FINAL, mappingStore);
			if (umlOperationBodyMapper.getOperation1().isAbstract() && umlOperationBodyMapper.getOperation2().isAbstract())
				matchModifier(srcOperationNode, dstOperationNode, Constants.ABSTRACT, mappingStore);
			if (umlOperationBodyMapper.getOperation1().isSynchronized() && umlOperationBodyMapper.getOperation2().isSynchronized())
				matchModifier(srcOperationNode, dstOperationNode, Constants.SYNCHRONIZED, mappingStore);
			if (umlOperationBodyMapper.getOperation1().isNative() && umlOperationBodyMapper.getOperation2().isNative())
				matchModifier(srcOperationNode, dstOperationNode, Constants.NATIVE, mappingStore);
			String v1 = umlOperationBodyMapper.getOperation1().getVisibility().toString();
			String v2 = umlOperationBodyMapper.getOperation2().getVisibility().toString();
			Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcOperationNode, Constants.MODIFIER, v1);
			Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstOperationNode, Constants.MODIFIER, v2);
			if (tree1 != null && tree2 != null)
				mappingStore.addMappingRecursively(tree1,tree2);
		}
	}

	private Pair<Tree, Tree> matchBasedOnType(Tree srcOperationNode, Tree dstOperationNode, String searchingType) {
		Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode,searchingType);
		Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode,searchingType);
		if (srcModifier != null && dstModifier != null)
			return new Pair<>(srcModifier, dstModifier);
		return null;
	}

	private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		String AST_type = Constants.TYPE_DECLARATION;
		if (classDiff.getOriginalClass().isEnum()) AST_type = Constants.ENUM_DECLARATION;
		Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),AST_type);
		Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),AST_type);
		if (srcTypeDeclaration.getParent() != null && dstTypeDeclaration.getParent() != null) {
			if (
					srcTypeDeclaration.getParent().getType().name.equals(Constants.TYPE_DECLARATION_STATEMENT)
							&&
							dstTypeDeclaration.getParent().getType().name.equals(Constants.TYPE_DECLARATION_STATEMENT)
			)
				mappingStore.addMapping(srcTypeDeclaration.getParent(),dstTypeDeclaration.getParent());
		}

		mappingStore.addMapping(srcTypeDeclaration,dstTypeDeclaration);

		String v1 = classDiff.getOriginalClass().getVisibility().toString();
		String v2 = classDiff.getNextClass().getVisibility().toString();
		Tree tree1 = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration, Constants.MODIFIER, v1);
		Tree tree2 = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration, Constants.MODIFIER, v2);
		if (tree1 != null && tree2 != null)
			mappingStore.addMappingRecursively(tree1,tree2);

		List<String> searchingTypes = new ArrayList<>();
		searchingTypes.add(Constants.SIMPLE_NAME);
		searchingTypes.add(Constants.TYPE_DECLARATION_KIND);
		for (String type : searchingTypes) {
			Pair<Tree,Tree> matched = matchBasedOnType(srcTypeDeclaration,dstTypeDeclaration,type);
			if (matched != null)
				mappingStore.addMapping(matched.first,matched.second);
		}
		if (classDiff.getOriginalClass().isStatic() && classDiff.getNextClass().isStatic())
			matchModifier(srcTypeDeclaration,dstTypeDeclaration,Constants.STATIC,mappingStore);
		if (classDiff.getOriginalClass().isFinal() && classDiff.getNextClass().isFinal())
			matchModifier(srcTypeDeclaration,dstTypeDeclaration,Constants.FINAL,mappingStore);
		if (classDiff.getOriginalClass().isAbstract() && classDiff.getNextClass().isAbstract())
			matchModifier(srcTypeDeclaration,dstTypeDeclaration,Constants.ABSTRACT,mappingStore);

		for (org.apache.commons.lang3.tuple.Pair<UMLTypeParameter, UMLTypeParameter> commonTypeParamSet : classDiff.getTypeParameterDiffList().getCommonTypeParameters()) {
			Tree srcTypeParam = TreeUtilFunctions.findByLocationInfo(srcTypeDeclaration, commonTypeParamSet.getLeft().getLocationInfo());
			Tree dstTypeParam = TreeUtilFunctions.findByLocationInfo(dstTypeDeclaration, commonTypeParamSet.getRight().getLocationInfo());
			mappingStore.addMappingRecursively(srcTypeParam,dstTypeParam);
		}
		processSuperClass(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
		processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
		processJavaDocs(srcTypeDeclaration,dstTypeDeclaration,classDiff.getOriginalClass().getJavadoc(),classDiff.getNextClass().getJavadoc(),mappingStore);
		processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);

	}

	private void matchModifier(Tree srcTypeDeclaration, Tree dstTypeDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
		// TODO: 8/3/2022 Search should be limited to the method signature before the method name
		String type = Constants.MODIFIER;
		Tree srcTree = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration,type,modifier);
		Tree dstTree = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration,type,modifier);
		if (srcTree != null && dstTree != null){
			mappingStore.addMapping(srcTree,dstTree);
		}
	}
}
