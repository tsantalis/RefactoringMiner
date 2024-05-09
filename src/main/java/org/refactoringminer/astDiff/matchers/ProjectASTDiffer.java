package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.actions.model.Action;
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
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.actions.ExtendedOnlyRootsClassifier;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;
import org.refactoringminer.astDiff.actions.SimplifiedExtendedChawatheScriptGenerator;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
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
	private final UMLModelDiff modelDiff;
	private List<AbstractCodeMapping> lastStepMappings;
	private ExtendedMultiMappingStore optimizationMappingStore;
	private List<Refactoring> modelDiffRefactorings;
	private final ProjectASTDiff projectASTDiff;

	public ProjectASTDiffer(UMLModelDiff modelDiff, Map<String, String> fileContentsBefore, Map<String, String> fileContentsAfter) throws RefactoringMinerTimedOutException {
		this.modelDiff = modelDiff;
		this.projectASTDiff = new ProjectASTDiff(fileContentsBefore, fileContentsAfter);
		diff();
	}

	public ProjectASTDiff getProjectASTDiff() {
		return projectASTDiff;
	}

	//kept for backward compatibility, but getProjectASTDiff() should be used instead
	public Set<ASTDiff> getDiffSet() {
		return projectASTDiff.getDiffSet();
	}

	private void diff() throws RefactoringMinerTimedOutException {
		long start = System.currentTimeMillis();
		this.modelDiffRefactorings = modelDiff.getRefactorings();
		long finish = System.currentTimeMillis();
		logger.info("ModelDiff.getRefactorings() execution time: " + (finish - start)/ 1000 + " seconds");
		projectASTDiff.setRefactorings(this.modelDiffRefactorings);
		projectASTDiff.setModelDiff(modelDiff);
		projectASTDiff.setParentContextMap(modelDiff.getParentModel().getTreeContextMap());
		projectASTDiff.setChildContextMap(modelDiff.getChildModel().getTreeContextMap());
		long diff_execution_started = System.currentTimeMillis();
		makeASTDiff(modelDiff.getCommonClassDiffList(),false);
		makeASTDiff(withCorrectOrder(modelDiff.getClassRenameDiffList()),false);
		makeASTDiff(withCorrectOrder(modelDiff.getClassMoveDiffList()),false);
		makeASTDiff(modelDiff.getInnerClassMoveDiffList(),true);
		makeASTDiff(getExtraDiffs(),true);
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
			processOptimization(diff, optimizationDataMap.get(diff.getSrcPath()));
		}
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
			new MissingIdenticalSubtree().match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
		}
		long diff_execution_finished =  System.currentTimeMillis();
		logger.info("Diff execution: " + (diff_execution_finished - diff_execution_started)/ 1000 + " seconds");
		computeAllEditScripts();
		createASTDiffsForMovedDeclarations();
	}

	private void createASTDiffsForMovedDeclarations() {
		Map<Pair<String, String>, List<Mapping>> filePairMappings = new LinkedHashMap<>();
        for(ASTDiff diff : projectASTDiff.getDiffSet()) {
        	Map<Tree, List<Mapping>> methodDeclarationMappings = new LinkedHashMap<Tree, List<Mapping>>();
            Map<Tree, List<Mapping>> fieldDeclarationMappings = new LinkedHashMap<Tree, List<Mapping>>();
            Map<Tree, List<Mapping>> typeDeclarationMappings = new LinkedHashMap<Tree, List<Mapping>>();
            Map<Tree, Action> actionMap = new LinkedHashMap<Tree, Action>();
        	ExtendedOnlyRootsClassifier classifier = (ExtendedOnlyRootsClassifier) diff.createRootNodesClassifier();
        	Map<Tree, Action> map = classifier.getSrcMoveOutTreeMap();
        	for(Tree src : map.keySet()) {
        		Tree methodRoot = TreeUtilFunctions.getParentUntilType(src, Constants.METHOD_DECLARATION);
        		if(methodRoot != null) {
        			if(methodDeclarationMappings.containsKey(methodRoot)) {
        				methodDeclarationMappings.get(methodRoot).addAll(getMappingForLeft(diff, src));
        			}
        			else {
        				List<Mapping> mappings = new ArrayList<Mapping>();
        				mappings.addAll(getMappingForLeft(diff, src));
        				methodDeclarationMappings.put(methodRoot, mappings);
        				if(map.containsKey(methodRoot)) {
        					actionMap.put(methodRoot, map.get(methodRoot));
        				}
        			}
        		}
        		Tree fieldRoot = TreeUtilFunctions.getParentUntilType(src, Constants.FIELD_DECLARATION);
        		if(fieldRoot != null) {
        			if(methodDeclarationMappings.containsKey(fieldRoot)) {
        				fieldDeclarationMappings.get(fieldRoot).addAll(getMappingForLeft(diff, src));
        			}
        			else {
        				List<Mapping> mappings = new ArrayList<Mapping>();
        				mappings.addAll(getMappingForLeft(diff, src));
        				fieldDeclarationMappings.put(fieldRoot, mappings);
        				if(map.containsKey(fieldRoot)) {
        					actionMap.put(fieldRoot, map.get(fieldRoot));
        				}
        			}
        		}
        		if(src.getType().name.equals(Constants.TYPE_DECLARATION) ||
        				src.getType().name.equals(Constants.ENUM_DECLARATION) ||
        				src.getType().name.equals(Constants.RECORD_DECLARATION)) {
        			actionMap.put(src, map.get(src));
        			List<Mapping> mappings = new ArrayList<Mapping>();
    				mappings.addAll(getMappingForLeft(diff, src));
    				typeDeclarationMappings.put(src, mappings);
        		}
        	}
        	//group the mappings based on the pair of src and dst files.
        	String srcPath = diff.getSrcPath();
        	for(Tree key : methodDeclarationMappings.keySet()) {
        		if(actionMap.containsKey(key)) {
        			Action action = actionMap.get(key);
        			if(action instanceof MoveOut) {
        				MoveOut moveOut = (MoveOut)action;
        				String dstPath = moveOut.getDstFile();
        				Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
        				if(filePairMappings.containsKey(pair)) {
        					filePairMappings.get(pair).addAll(methodDeclarationMappings.get(key));
        				}
        				else {
        					List<Mapping> mappings = new ArrayList<Mapping>();
        					mappings.addAll(methodDeclarationMappings.get(key));
        					filePairMappings.put(pair, mappings);
        				}
        			}
        		}
        	}
        	for(Tree key : fieldDeclarationMappings.keySet()) {
        		if(actionMap.containsKey(key)) {
        			Action action = actionMap.get(key);
        			if(action instanceof MoveOut) {
        				MoveOut moveOut = (MoveOut)action;
        				String dstPath = moveOut.getDstFile();
        				Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
        				if(filePairMappings.containsKey(pair)) {
        					filePairMappings.get(pair).addAll(fieldDeclarationMappings.get(key));
        				}
        				else {
        					List<Mapping> mappings = new ArrayList<Mapping>();
        					mappings.addAll(fieldDeclarationMappings.get(key));
        					filePairMappings.put(pair, mappings);
        				}
        			}
            	}
        	}
        	for(Tree key : typeDeclarationMappings.keySet()) {
        		if(actionMap.containsKey(key)) {
        			Action action = actionMap.get(key);
        			if(action instanceof MoveOut) {
        				MoveOut moveOut = (MoveOut)action;
        				String dstPath = moveOut.getDstFile();
        				Pair<String, String> pair = new Pair<String, String>(srcPath, dstPath);
        				if(filePairMappings.containsKey(pair)) {
        					filePairMappings.get(pair).addAll(typeDeclarationMappings.get(key));
        				}
        				else {
        					List<Mapping> mappings = new ArrayList<Mapping>();
        					mappings.addAll(typeDeclarationMappings.get(key));
        					filePairMappings.put(pair, mappings);
        				}
        			}
            	}
        	}
        }
        for(Pair<String, String> pair : filePairMappings.keySet()) {
        	Pair<TreeContext, TreeContext> treeContextPairs = findTreeContexts(pair.first, pair.second);
        	List<Mapping> mappings = filePairMappings.get(pair);
        	if(mappings.size() > 0) {
	        	Tree leftRoot = TreeUtilFunctions.getFinalRoot(mappings.get(0).first);
	        	Tree rightRoot = TreeUtilFunctions.getFinalRoot(mappings.get(0).second);
	        	ExtendedMultiMappingStore store = new ExtendedMultiMappingStore(leftRoot, rightRoot);
	        	for(Mapping m : mappings) {
	        		store.addMappingRecursively(m.first, m.second);
	        	}
	        	ASTDiff diff = new ASTDiff(pair.first, pair.second, treeContextPairs.first, treeContextPairs.second, store, new SimplifiedExtendedChawatheScriptGenerator().computeActions(store, null, null));
	        	projectASTDiff.addMoveASTDiff(diff);
        	}
        }
	}

    private List<Mapping> getMappingForLeft(ASTDiff diff, Tree left) {
    	List<Mapping> matchingMappings = new ArrayList<Mapping>();
    	for(Mapping mapping : diff.getAllMappings()) {
    		if(mapping.first.equals(left)) {
    			matchingMappings.add(mapping);
    		}
    	}
    	return matchingMappings;
    }

	private List<? extends UMLAbstractClassDiff> withCorrectOrder(List<? extends UMLAbstractClassDiff> umlDiffs) {
		ArrayList<UMLAbstractClassDiff> result = new ArrayList<>(umlDiffs);
		Set<UMLAbstractClassDiff> seen = new HashSet<>();
		for (UMLAbstractClassDiff umlDiff : umlDiffs) {
			UMLAbstractClassDiff found = findDiffWith(result, umlDiff.getOriginalClassName(), umlDiff.getNextClassName());
			if (found != null && !seen.contains(found))
			{
				seen.add(found);
				result.remove(found);
				result.add(0, found);
			}
		}
		return result;
	}

	private UMLAbstractClassDiff findDiffWith(ArrayList<? extends UMLAbstractClassDiff> result, String originalClassName, String nextClassName) {
		for (UMLAbstractClassDiff umlAbstractClassDiff : result) {
			if (umlAbstractClassDiff.getOriginalClassName().equals(originalClassName)
				&&
				umlAbstractClassDiff.getNextClassName().equals(nextClassName))
				return umlAbstractClassDiff;
		}
		return null;
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
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
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
				projectASTDiff.addASTDiff(classASTDiff);
			}
		}
	}

	private ASTDiff findAppend(UMLAbstractClassDiff classBaseDiff) {
		for (ASTDiff existing : projectASTDiff.getDiffSet()) {
			if (existing.getSrcPath().equals(classBaseDiff.getOriginalClass().getSourceFile()))
				return existing;
			else if (existing.getDstPath().equals(classBaseDiff.getNextClass().getSourceFile()))
				return existing;
		}
		return null;
	}

	private Pair<TreeContext, TreeContext> findTreeContexts(String srcPath, String dstPath) {
		return new Pair<>(modelDiff.getParentModel().getTreeContextMap().get(srcPath),
				modelDiff.getChildModel().getTreeContextMap().get(dstPath));
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

		String key = classDiff.getOriginalClass().getLocationInfo().getFilePath();
		if (optimizationDataMap.containsKey(key)) {
			this.lastStepMappings = optimizationDataMap.get(key).lastStepMappings;
			this.optimizationMappingStore = optimizationDataMap.get(key).optimizationMappingStore;
		}
		else {
			this.lastStepMappings = new ArrayList<>();
			this.optimizationMappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		}
		if (!mergeFlag) {
			mappingStore.addMapping(srcTree, dstTree);
			processPackageDeclaration(srcTree,dstTree,mappingStore);
		}
		boolean isBaseDiff = classDiff instanceof UMLClassBaseDiff;
		if (isBaseDiff) {
			UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
			processImports(srcTree, dstTree, (baseClassDiff).getImportDiffList(), mappingStore);
			processClassDeclarationMapping(srcTree,dstTree, baseClassDiff,mappingStore);
		}
		processClassAttributes(srcTree,dstTree,classDiff,mappingStore);
		processEnumConstants(srcTree,dstTree,classDiff.getCommonEnumConstants(),mappingStore);
		processAllMethods(srcTree,dstTree,classDiff.getOperationBodyMapperList(),mappingStore);

		processModelDiffRefactorings(classDiff,mappingStore);
		processMovedAttributes(classDiff,mappingStore);

		if (isBaseDiff){
			UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
			processRefactorings(srcTree,dstTree,getClassDiffRefactorings(baseClassDiff),mappingStore);
		}
		optimizationDataMap.put(key, new OptimizationData(lastStepMappings, optimizationMappingStore));
		return new ASTDiff(classDiff.getOriginalClass().getLocationInfo().getFilePath(),
				classDiff.getNextClass().getLocationInfo().getFilePath(), treeContextPair.first, treeContextPair.second, mappingStore);
	}

	private List<Refactoring> getClassDiffRefactorings(UMLClassBaseDiff classDiff) {
		List<Refactoring> classDiffRefactorings = new ArrayList<>();
		for (Refactoring modelDiffRefactoring : modelDiffRefactorings) {
			Set<ImmutablePair<String, String>> involvedClassesBeforeRefactoring = modelDiffRefactoring.getInvolvedClassesBeforeRefactoring();
			Set<ImmutablePair<String, String>> involvedClassesAfterRefactoring = modelDiffRefactoring.getInvolvedClassesAfterRefactoring();
			if (involvedClassesBeforeRefactoring.size() > 1 || involvedClassesAfterRefactoring.size() > 1) continue; //Must extend the logic to work for cases with more than one involving classes such as ExtractAndMoveMethodRefactoring
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
				boolean bothInAnonymous = refactoringClassNameBefore.contains(umlClassBefore.getName()) && refactoringClassNameAfter.contains(umlClassAfter.getName());
				if (isNameMatching || bothInAnonymous)
					classDiffRefactorings.add(modelDiffRefactoring);
			}

		}
		return classDiffRefactorings;
	}

	private void processMovedAttributes(UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
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
	private static void processOptimization(ASTDiff input, OptimizationData optimizationData) {
		Tree srcTree = input.src.getRoot();
		Tree dstTree = input.dst.getRoot();
		ExtendedMultiMappingStore lastStepMappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		for (AbstractCodeMapping lastStepMapping : optimizationData.lastStepMappings) {
//			if (lastStepMapping.getFragment1().getLocationInfo().getFilePath().equals(lastStepMapping.getFragment2().getLocationInfo().getFilePath())) {
			if (lastStepMapping.getFragment1().getLocationInfo().getFilePath().equals(input.getSrcPath()) && lastStepMapping.getFragment2().getLocationInfo().getFilePath().equals(input.getDstPath())) {
				Tree srcExp = TreeUtilFunctions.findByLocationInfo(srcTree, lastStepMapping.getFragment1().getLocationInfo());
				Tree dstExp = TreeUtilFunctions.findByLocationInfo(dstTree, lastStepMapping.getFragment2().getLocationInfo());
				if (srcExp == null || dstExp == null) continue;
				if (srcExp.isLeaf() && dstExp.isLeaf())
				{
					if (srcExp.getType().name.equals(Constants.STRING_LITERAL) &&
						dstExp.getType().name.equals(Constants.STRING_LITERAL)) {
						input.getAllMappings().addMapping(srcExp, dstExp);
						continue;
					}
				}
				if (needToOverride(input, srcExp, dstExp))
					new LeafMatcher().match(srcExp, dstExp, lastStepMappingStore);
				else
					new LeafMatcher().match(srcExp,dstExp,input.getAllMappings());
			}
		}
		ExtendedMultiMappingStore allMappings = input.getAllMappings();
		allMappings.replaceWithOptimizedMappings(lastStepMappingStore);
		allMappings.replaceWithOptimizedMappings(optimizationData.optimizationMappingStore);
	}

	private static boolean needToOverride(ASTDiff input, Tree srcExp, Tree dstExp) {
		if (!srcExp.isIsomorphicTo(dstExp)) return true;
		ExtendedMultiMappingStore allMappings = input.getAllMappings();
		Set<Tree> dsts = allMappings.getDsts(srcExp);
		if (dsts != null)
		{
			for (Tree dst : dsts)
				if (!srcExp.isIsomorphicTo(dst))
					return true;
			return false;
		}
		Set<Tree> srcs = allMappings.getSrcs(dstExp);
		if (srcs != null) {
			for (Tree src : srcs) {
				if (!src.isIsomorphicTo(dstExp)) {
					return true;
				}
				return false;
			}
		}
		return true;
	}

	private static boolean isTestRelated(Tree srcTree) {
		Tree p1 = TreeUtilFunctions.getParentUntilType(srcTree, Constants.METHOD_DECLARATION);
		if (p1 == null) return false;
		for (Tree child : p1.getChildren()) {
			if (child.getType().name.equals(Constants.MARKER_ANNOTATION))
			{
				if (!child.getChildren().isEmpty())
				{
					Tree c0 = child.getChild(0);
					if (c0.getLabel().equals("Test") || c0.getLabel().equals("ParameterizedTest")) return true;
				}
			}
		}
		return false;
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

	private void processModelDiffRefactorings(UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		for(Refactoring refactoring : modelDiffRefactorings) {
			List<String> beforeRefactoringClasses = refactoring.getInvolvedClassesBeforeRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
			List<String> afterRefactoringClasses = refactoring.getInvolvedClassesAfterRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
			if (refactoring instanceof MoveOperationRefactoring) {
				if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
						beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
					MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
					if (isExtractedMethodRef(moveOperationRefactoring.getBodyMapper().getOperation2())) return;
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
				if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
						beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
					ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
					findTreeFromMapperAndProcessBodyMapper(extractOperationRefactoring.getBodyMapper(), mappingStore);
				}
			}
			else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION)) {
				if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
						beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
					InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
					findTreeFromMapperAndProcessBodyMapper(inlineOperationRefactoring.getBodyMapper(), mappingStore);
				}
			}

		}
	}

	private void findTreeFromMapperAndProcessBodyMapper(UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore) {
		String srcPath = bodyMapper.getOperation1().getLocationInfo().getFilePath();
		String dstPath = bodyMapper.getOperation2().getLocationInfo().getFilePath();
		Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
		Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
		processBodyMapper(srcTotalTree, dstTotalTree, bodyMapper, mappingStore, true);
	}

	private boolean isExtractedMethodRef(UMLOperation operation2) {
		for (Refactoring modelDiffRefactoring : modelDiffRefactorings) {
			if (modelDiffRefactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
			{
				if (((ExtractOperationRefactoring) modelDiffRefactoring).getExtractedOperation().equals(operation2)) return true;
			}
		}
		return false;
	}
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
			if (srcOperationNode == null || !(srcOperationNode.getType().name.equals(Constants.METHOD_DECLARATION) || srcOperationNode.getType().name.equals(Constants.ANNOTATION_TYPE_MEMBER_DECLARATION))) return;
			if (dstOperationNode == null || !(dstOperationNode.getType().name.equals(Constants.METHOD_DECLARATION) || dstOperationNode.getType().name.equals(Constants.ANNOTATION_TYPE_MEMBER_DECLARATION))) return;
			processJavaDocs(srcOperationNode, dstOperationNode, umlOperationBodyMapper.getOperation1().getJavadoc(), umlOperationBodyMapper.getOperation2().getJavadoc(), mappingStore);
			mappingStore.addMapping(srcOperationNode, dstOperationNode);
		} else {
			//Static Initializers
			srcOperationNode = TreeUtilFunctions.findByLocationInfo(srcTree, umlOperationBodyMapper.getContainer1().getLocationInfo());
			dstOperationNode = TreeUtilFunctions.findByLocationInfo(dstTree, umlOperationBodyMapper.getContainer2().getLocationInfo());
			if (srcOperationNode != null & dstOperationNode != null) {
				if (srcOperationNode.getType().name.equals(Constants.INITIALIZER) && dstOperationNode.getType().name.equals(Constants.INITIALIZER)) {
					mappingStore.addMapping(srcOperationNode, dstOperationNode);
					//static keyword
					if (umlOperationBodyMapper.getContainer1() instanceof UMLInitializer && umlOperationBodyMapper.getContainer2() instanceof UMLInitializer)
						if (((UMLInitializer) umlOperationBodyMapper.getContainer1()).isStatic() && ((UMLInitializer) umlOperationBodyMapper.getContainer2()).isStatic()) {
							Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode, Constants.MODIFIER);
							Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode, Constants.MODIFIER);
							if (srcModifier != null && dstModifier != null)
								mappingStore.addMapping(srcModifier, dstModifier);
						}
					//Javadoc
					Tree srcJavadoc = TreeUtilFunctions.findChildByType(srcOperationNode, Constants.JAVA_DOC);
					Tree dstJavadoc = TreeUtilFunctions.findChildByType(dstOperationNode, Constants.JAVA_DOC);
					if (srcJavadoc != null && dstJavadoc != null)
						new BasicTreeMatcher().match(srcJavadoc, dstJavadoc, mappingStore);
				}
			}
		}
		if (srcOperationNode != null && dstOperationNode != null) {
			processMethodSignature(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
			processBodyMapper(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore, false);
			processOperationDiff(srcOperationNode, dstOperationNode, umlOperationBodyMapper, mappingStore);
			processMethodParameters(srcOperationNode, dstOperationNode, umlOperationBodyMapper.getMatchedVariables(), mappingStore);
		}
	}

	private void processMethodParameters(Tree srcTree, Tree dstTree, Set<org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration>> matchedVariables, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<VariableDeclaration, VariableDeclaration> matchedPair: matchedVariables) {
			VariableDeclaration leftVarDecl = matchedPair.getLeft();
			VariableDeclaration rightVarDecl = matchedPair.getRight();
			Tree leftTree =  TreeUtilFunctions.findByLocationInfo(srcTree,leftVarDecl.getLocationInfo());
			Tree rightTree = TreeUtilFunctions.findByLocationInfo(dstTree,rightVarDecl.getLocationInfo());
			if (leftTree == null || rightTree == null) return;
			if (leftVarDecl.isParameter() && rightVarDecl.isParameter()) {
				if (TreeUtilFunctions.isIsomorphicTo(rightTree, leftTree))
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
		//if (srcStatementNode.getMetrics().hash == dstStatementNode.getMetrics().hash)
		//{
		//	mappingStore.addMappingRecursively(srcStatementNode, dstStatementNode);
		//}
		//else
		{
			if (srcStatementNode == null || dstStatementNode == null)
				return;
			if (srcStatementNode.getType().name.equals(dstStatementNode.getType().name))
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

		boolean _abstractExp = abstractCodeMapping.getFragment1() instanceof AbstractExpression || abstractCodeMapping.getFragment2() instanceof AbstractExpression;
		boolean _leafExp = abstractCodeMapping.getFragment1() instanceof LeafExpression || abstractCodeMapping.getFragment2() instanceof LeafExpression;
		boolean _abstractExpWithNonCompositeOwner = _abstractExp;
		if (_abstractExp){
			if (abstractCodeMapping.getFragment1() instanceof AbstractExpression)
				if (((AbstractExpression)abstractCodeMapping.getFragment1()).getOwner() != null
						&& ((AbstractExpression)abstractCodeMapping.getFragment1()).getOwner().getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT))
					_abstractExpWithNonCompositeOwner = false;
			if (abstractCodeMapping.getFragment2() instanceof AbstractExpression)
				if (((AbstractExpression)abstractCodeMapping.getFragment2()).getOwner() != null
						&& ((AbstractExpression)abstractCodeMapping.getFragment2()).getOwner().getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT))
					_abstractExpWithNonCompositeOwner = false;
		}
		if (_abstractExpWithNonCompositeOwner || _leafExp) {
			lastStepMappings.add(abstractCodeMapping);
		} else {
			new LeafMatcher().match(srcStatementNode,dstStatementNode,mappingStore);
			additionallyMatchedStatements(srcTree, dstTree, srcStatementNode, dstStatementNode, abstractCodeMapping, mappingStore);
		}
		optimizeVariableDeclarations(abstractCodeMapping);
		if (!isPartOfExtractedMethod && srcStatementNode.getType().name.equals(Constants.RETURN_STATEMENT) && dstStatementNode.getType().name.equals(Constants.RETURN_STATEMENT)) {
			optimizationMappingStore.addMapping(srcStatementNode,dstStatementNode);
		}
		if (abstractCodeMapping.getRefactorings().size() > 0) {
			leafMappingRefactoringAwareness(dstTree, abstractCodeMapping, mappingStore);
		}

	}

	private static void leafMappingRefactoringAwareness(Tree dstTree, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		for (Refactoring refactoring : abstractCodeMapping.getRefactorings()) {
			if (refactoring instanceof ExtractVariableRefactoring)
			{
				ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
				for (AbstractCodeMapping reference : extractVariableRefactoring.getReferences()) {
					for (LeafExpression variable : reference.getFragment2().getVariables()) {
						if (variable.getString().equals(extractVariableRefactoring.getVariableDeclaration().getVariableName())) {
							Tree referenceNode = TreeUtilFunctions.findByLocationInfo(dstTree, variable.getLocationInfo());
							if (referenceNode != null)
							{
								if (referenceNode.getChildren().size() > 0){
									referenceNode = referenceNode.getChild(0);
								}
								if (mappingStore.isDstMapped(referenceNode) && !mappingStore.isDstMultiMapped(referenceNode)) {
									Tree tempSrc = mappingStore.getSrcs(referenceNode).iterator().next();
									mappingStore.removeMapping(tempSrc, referenceNode);
								}
							}
						}
					}
				}
			}
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
					}
				}
			}
		}
	}

	private void processClassAnnotations(Tree srcTree, Tree dstTree, UMLAnnotationListDiff annotationListDiff, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations())
			processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, umlAnnotationUMLAnnotationPair.getLeft(), umlAnnotationUMLAnnotationPair.getRight());
	}

	private void matchBlocks(Tree srcStatementNode, Tree dstStatementNode, ExtendedMultiMappingStore mappingStore) {
		String searchingType = Constants.BLOCK;
		Pair<Tree, Tree> matched = matchBasedOnType(srcStatementNode,dstStatementNode, searchingType);
		if (matched != null)
			mappingStore.addMapping(matched.first,matched.second);
	}

	private void processJavaDocs(Tree srcTree, Tree dstTree, UMLJavadoc srcUMLJavaDoc, UMLJavadoc dstUMLJavaDoc, ExtendedMultiMappingStore mappingStore) {
		if (srcUMLJavaDoc != null && dstUMLJavaDoc != null) {
			Tree srcJavaDocNode = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLJavaDoc.getLocationInfo());
			Tree dstJavaDocNode = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLJavaDoc.getLocationInfo());
			if (srcJavaDocNode == null || dstJavaDocNode == null) return;
			if (srcUMLJavaDoc.equalText(dstUMLJavaDoc) && srcJavaDocNode.isIsoStructuralTo(dstJavaDocNode)) {
				mappingStore.addMappingRecursively(srcJavaDocNode,dstJavaDocNode);
			} else {
				new BasicTreeMatcher().match(srcJavaDocNode,dstJavaDocNode,mappingStore);
				mappingStore.addMapping(srcJavaDocNode,dstJavaDocNode);
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
		AbstractMap.SimpleEntry<Set<UMLType>, Set<UMLType>> changedExceptionTypes = umlOperationDiff.getChangedExceptionTypes();
		if (changedExceptionTypes != null){
			Iterator<UMLType> keys = changedExceptionTypes.getKey().iterator();
			Iterator<UMLType> values = changedExceptionTypes.getValue().iterator();
			if (keys.hasNext() && values.hasNext())
			{
				Tree srcExceptionNode =TreeUtilFunctions.findByLocationInfo(srcTree, keys.next().getLocationInfo());
				Tree dstExceptionNode =TreeUtilFunctions.findByLocationInfo(dstTree, values.next().getLocationInfo());
				mappingStore.addMappingRecursively(srcExceptionNode,dstExceptionNode);
			}
		}
		if (umlOperationDiff.getRemovedOperation().getReturnParameter() != null && umlOperationDiff.getAddedOperation().getReturnParameter() != null ) {
			LocationInfo srcLocationInfo = umlOperationDiff.getRemovedOperation().getReturnParameter().getType().getLocationInfo();
			LocationInfo dstLocationInfo = umlOperationDiff.getAddedOperation().getReturnParameter().getType().getLocationInfo();
			Tree srcNode =TreeUtilFunctions.findByLocationInfo(srcTree, srcLocationInfo);
			Tree dstNode =TreeUtilFunctions.findByLocationInfo(dstTree, dstLocationInfo);
			if (srcNode == null || dstNode == null) return;
			if (srcNode.isIsoStructuralTo(dstNode))
				mappingStore.addMappingRecursively(srcNode,dstNode);
			else {
				new LeafMatcher().match(srcNode,dstNode,mappingStore);
			}
		}
	}

	private static boolean multipleInstancesWithSameDescription(List<Refactoring> refactoringList, Refactoring refactoring) {
		int count = 0;
		for (Refactoring r : refactoringList) {
			if(r.toString().equals(refactoring.toString()))
				count++;
		}
		return count > 1;
	}

	private void processRefactorings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, ExtendedMultiMappingStore mappingStore){
		for (Refactoring refactoring : refactoringList) {
			if (refactoring instanceof ReplaceLoopWithPipelineRefactoring) {
				ReplaceLoopWithPipelineRefactoring replaceLoopWithPipelineRefactoring = (ReplaceLoopWithPipelineRefactoring) refactoring;
				if (replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().size() != 1) continue;
				AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter().iterator().next();
				List<LambdaExpressionObject> lambdas = next.getLambdas();
				AbstractCodeFragment enhancedFor = null;
				for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore()) {
					if (abstractCodeFragment instanceof CompositeStatementObject && ((CompositeStatementObject)abstractCodeFragment).isLoop()) {
						enhancedFor = abstractCodeFragment;
						break;
					}
				}
				if (enhancedFor == null) continue;
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
				if (srcSt != null && dstSt != null)
					if (srcSt.getType().name.equals(dstSt.getType().name))
						mappingStore.addMapping(srcSt,dstSt);
			} else if (refactoring instanceof ReplacePipelineWithLoopRefactoring) {
				ReplacePipelineWithLoopRefactoring replaceLoopWithPipelineRefactoring = (ReplacePipelineWithLoopRefactoring) refactoring;
				if (replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().size() != 1) continue;
				AbstractCodeFragment next = replaceLoopWithPipelineRefactoring.getCodeFragmentsBefore().iterator().next();
				List<LambdaExpressionObject> lambdas = next.getLambdas();
				AbstractCodeFragment enhancedFor = null;
				for (AbstractCodeFragment abstractCodeFragment : replaceLoopWithPipelineRefactoring.getCodeFragmentsAfter()) {
					if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
						enhancedFor = abstractCodeFragment;
						break;
					}
				}
				if( enhancedFor == null) continue;
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
				if (dstSt != null && srcSt != null && srcSt.getType().name.equals(dstSt.getType().name))
					mappingStore.addMapping(srcSt, dstSt);
			} else if (refactoring instanceof MergeOperationRefactoring) {
				MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
				for(UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
					processMethod(srcTree,dstTree,bodyMapper,mappingStore);
				}
			} else if (refactoring instanceof SplitOperationRefactoring) {
				SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
				for(UMLOperationBodyMapper bodyMapper : splitOperationRefactoring.getMappers()) {
					processMethod(srcTree,dstTree,bodyMapper,mappingStore);
				}
			} else if (refactoring instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
				UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
				processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, true);
				processArgumentMappings(srcTree, dstTree, refactoringList, refactoring, extractOperationRefactoring.getArgumentMappings());
			} else if (refactoring instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
				UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
				processBodyMapper(srcTree,dstTree,bodyMapper,mappingStore, false);
				processArgumentMappings(srcTree, dstTree, refactoringList, refactoring, inlineOperationRefactoring.getArgumentMappings());
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
				lastStepMappings.addAll(extractVariableRefactoring.getSubExpressionMappings());
			} else if (refactoring instanceof InlineVariableRefactoring) {
				InlineVariableRefactoring inlineVariableRefactoring = (InlineVariableRefactoring) refactoring;
				lastStepMappings.addAll(inlineVariableRefactoring.getSubExpressionMappings());
			} else if (refactoring instanceof AssertThrowsRefactoring) {
				AssertThrowsRefactoring assertThrowsRefactoring = (AssertThrowsRefactoring) refactoring;
				lastStepMappings.addAll(assertThrowsRefactoring.getSubExpressionMappings());
			} else if (refactoring instanceof InlineAttributeRefactoring) {
				InlineAttributeRefactoring inlineAttributeRefactoring = (InlineAttributeRefactoring) refactoring;
				//Tree srcAttrDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, inlineAttributeRefactoring.getVariableDeclaration().getLocationInfo());
				//for (AbstractCodeMapping reference : inlineAttributeRefactoring.getReferences()) {
				//	Tree dstStatementTree = TreeUtilFunctions.findByLocationInfo(dstTree,reference.getFragment2().getLocationInfo());
				//	new LeafMatcher().match(srcAttrDeclaration,dstStatementTree,mappingStore);
				//}
				lastStepMappings.addAll(inlineAttributeRefactoring.getSubExpressionMappings());
			} else if (refactoring instanceof ExtractAttributeRefactoring) {
				ExtractAttributeRefactoring extractAttributeRefactoring = (ExtractAttributeRefactoring) refactoring;
				//Tree dstAttrDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree, extractAttributeRefactoring.getVariableDeclaration().getLocationInfo());
				//for (AbstractCodeMapping reference : extractAttributeRefactoring.getReferences()) {
				//	Tree srcStatementTree = TreeUtilFunctions.findByLocationInfo(srcTree,reference.getFragment1().getLocationInfo());
				//	new LeafMatcher().match(srcStatementTree,dstAttrDeclaration,mappingStore);
				//}
				lastStepMappings.addAll(extractAttributeRefactoring.getSubExpressionMappings());
				for (UMLAnonymousClassDiff umlAnonymousClassDiff : extractAttributeRefactoring.getAnonymousClassDiffList()) {
					processAnonymousClassDiff(srcTree,dstTree,umlAnonymousClassDiff,mappingStore);
				}
			} else if (refactoring instanceof MergeVariableRefactoring) {
				MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
				Set<VariableDeclaration> mergedVariables = mergeVariableRefactoring.getMergedVariables();
				VariableDeclaration newVariable = mergeVariableRefactoring.getNewVariable();
				Tree dstVariableDeclaration =TreeUtilFunctions.findByLocationInfo(dstTree,newVariable.getLocationInfo());

				if (dstVariableDeclaration != null) {
					List<Tree> dstChildrenList = dstVariableDeclaration.getChildren();
					Tree dstVarName = dstChildrenList.get(dstChildrenList.size() - 1);
					for (VariableDeclaration variableDeclaration : mergedVariables) {
						Tree srcVariableDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree, variableDeclaration.getLocationInfo());
						if (srcVariableDeclaration != null) {
							List<Tree> srcChildrenList = srcVariableDeclaration.getChildren();
							Tree srcVarName = srcChildrenList.get(srcChildrenList.size() - 1);
							mappingStore.addMapping(srcVarName, dstVarName);
						}
					}
				}
			} else if (refactoring instanceof SplitConditionalRefactoring) {
				SplitConditionalRefactoring splitConditionalRefactoring = (SplitConditionalRefactoring) refactoring;
				lastStepMappings.addAll(splitConditionalRefactoring.getSubExpressionMappings());
			} else if (refactoring instanceof MergeConditionalRefactoring) {
				MergeConditionalRefactoring mergeConditionalRefactoring = (MergeConditionalRefactoring) refactoring;
				lastStepMappings.addAll(mergeConditionalRefactoring.getSubExpressionMappings());
			} else if (refactoring instanceof ReplaceGenericWithDiamondRefactoring) {
				ReplaceGenericWithDiamondRefactoring replaceGenericWithDiamondRefactoring = (ReplaceGenericWithDiamondRefactoring) refactoring;
				lastStepMappings.addAll(replaceGenericWithDiamondRefactoring.getSubExpressionMappings());
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
				if (srcInput == null || dstInput == null) continue;
				boolean eligible = true;
				switch (renameVariableRefactoring.getRefactoringType()) {
					case REPLACE_VARIABLE_WITH_ATTRIBUTE:
						srcInput = TreeUtilFunctions.getParentUntilType(srcInput,Constants.VARIABLE_DECLARATION_STATEMENT);
						dstInput = TreeUtilFunctions.getParentUntilType(dstInput,Constants.FIELD_DECLARATION);
						if (mappingStore.isSrcMapped(srcInput) || mappingStore.isDstMapped(dstInput))
							continue;
						break;
					case REPLACE_ATTRIBUTE_WITH_VARIABLE:
						srcInput = TreeUtilFunctions.getParentUntilType(srcInput,Constants.FIELD_DECLARATION);
						dstInput = TreeUtilFunctions.getParentUntilType(dstInput,Constants.VARIABLE_DECLARATION_STATEMENT);
						if (mappingStore.isSrcMapped(srcInput) || mappingStore.isDstMapped(dstInput))
							continue;
						break;
					case RENAME_PARAMETER:
						eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
						break;
					case RENAME_VARIABLE:
						Set<AbstractCodeMapping> references = renameVariableRefactoring.getReferences();
						for (AbstractCodeMapping abstractCodeMapping : references) {
							if (((RenameVariableRefactoring) refactoring).isInsideExtractedOrInlinedMethod() &&
									multipleInstancesWithSameDescription(refactoringList,((RenameVariableRefactoring) refactoring).getOperationBefore(),((RenameVariableRefactoring) refactoring).getOperationAfter())) {
								eligible = false;
							}
							if (eligible) {
								if (abstractCodeMapping instanceof LeafMapping) {
									findVariablesAndMatch(srcTree, dstTree, abstractCodeMapping, renameVariableRefactoring.getOriginalVariable().getVariableName(), renameVariableRefactoring.getRenamedVariable().getVariableName());
								}
							}
						}
						eligible = false;
						break;
					case PARAMETERIZE_VARIABLE:
						eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
						if (!eligible)
							break;
						while (!TreeUtilFunctions.isStatement(srcInput.getType().name)) {
							if (srcInput.getType() == null) break;
							srcInput = srcInput.getParent();
						}
						if (TreeUtilFunctions.isStatement(srcInput.getType().name)){
							new LeafMatcher().match(srcInput,dstInput,mappingStore);
						}
						eligible = false;
						break;
					case LOCALIZE_PARAMETER:
						eligible = !renameVariableRefactoring.isInsideExtractedOrInlinedMethod();
						if (!eligible)
							break;
						while (!TreeUtilFunctions.isStatement(dstInput.getType().name)) {
							if (dstInput.getType() == null) break;
							dstInput = dstInput.getParent();
						}
						if (TreeUtilFunctions.isStatement(dstInput.getType().name)){
							new LeafMatcher().match(srcInput,dstInput,mappingStore);
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
			else if (refactoring instanceof ModifyClassAnnotationRefactoring)
			{
				ModifyClassAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyClassAnnotationRefactoring) refactoring;
				if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
					processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
			}
			else if (refactoring instanceof ModifyMethodAnnotationRefactoring)
			{
				ModifyMethodAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) refactoring;
				if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
					processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
			}
			else if (refactoring instanceof ModifyAttributeAnnotationRefactoring)
			{
				ModifyAttributeAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyAttributeAnnotationRefactoring) refactoring;
				if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
					processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
			}
			else if (refactoring instanceof ModifyVariableAnnotationRefactoring)
			{
				ModifyVariableAnnotationRefactoring modifiedAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
				if (modifiedAnnotationRefactoring.getAnnotationBefore().getTypeName().equals(modifiedAnnotationRefactoring.getAnnotationAfter().getTypeName()))
					processModifiedAnnotation(srcTree, dstTree, mappingStore, modifiedAnnotationRefactoring.getAnnotationBefore(), modifiedAnnotationRefactoring.getAnnotationAfter());
			}
			else if (refactoring instanceof RenameAttributeRefactoring) {
				RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) refactoring;
				for (AbstractCodeMapping reference : renameAttributeRefactoring.getReferences()) {
					List<LeafMapping> subExpressionMappings = reference.getSubExpressionMappings();
					for (LeafMapping subExpressionMapping : subExpressionMappings) {
						Tree srcSimpleName = TreeUtilFunctions.findByLocationInfo(srcTree, subExpressionMapping.getFragment1().getLocationInfo(), Constants.SIMPLE_NAME);
						Tree dstSimpleName = TreeUtilFunctions.findByLocationInfo(dstTree, subExpressionMapping.getFragment2().getLocationInfo(), Constants.SIMPLE_NAME);
						if (srcSimpleName != null && dstSimpleName != null)
							optimizationMappingStore.addMapping(srcSimpleName,dstSimpleName);
					}
				}
			}
			else if (refactoring instanceof InvertConditionRefactoring) {
				InvertConditionRefactoring invertConditionRefactoring = (InvertConditionRefactoring) refactoring;
				Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree,invertConditionRefactoring.getOriginalConditional().getLocationInfo());
				Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree,invertConditionRefactoring.getInvertedConditional().getLocationInfo());
//				new CompositeMatcher(invertConditionRefactoring.getOriginalConditional(), )
				new GeneralMatcher(
						invertConditionRefactoring.getOriginalConditional(),
						invertConditionRefactoring.getInvertedConditional())
						.match(srcSubTree,dstSubTree,mappingStore);
			}
		}

	}

	private void processArgumentMappings(Tree srcTree, Tree dstTree, List<Refactoring> refactoringList, Refactoring refactoring, List<AbstractCodeMapping> argumentMappings) {
		//skip argument mappings, if the same method is extracted more than once from the original method.
		if(!multipleInstancesWithSameDescription(refactoringList, refactoring)) {
			for(AbstractCodeMapping expressionMapping : argumentMappings) {
				Tree t1 = TreeUtilFunctions.findByLocationInfo(srcTree,expressionMapping.getFragment1().getLocationInfo());
				Tree t2 = TreeUtilFunctions.findByLocationInfo(dstTree,expressionMapping.getFragment2().getLocationInfo());
				new LeafMatcher().match(t1,t2,optimizationMappingStore);
			}
		}
	}

	private boolean multipleInstancesWithSameDescription(List<Refactoring> refactoringList, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
		int counter1 = 0;
		int counter2 = 0;
		for (Refactoring r : refactoringList) {
			if (r.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
			{
				if (((ExtractOperationRefactoring)r).getExtractedOperation().equals(operationAfter))
					counter1++;
			}
			else if (r.getRefactoringType().equals(RefactoringType.INLINE_OPERATION))
			{
				if (((InlineOperationRefactoring)r).getInlinedOperation().equals(operationBefore))
					counter2++;
			}
		}
		return (counter1 > 1 || counter2 > 1);
	}

	private static void processModifiedAnnotation(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, UMLAnnotation annotationBefore, UMLAnnotation annotationAfter) {
		Tree srcAnnotationTree = TreeUtilFunctions.findByLocationInfo(srcTree, annotationBefore.getLocationInfo());
		Tree dstAnnotationTree = TreeUtilFunctions.findByLocationInfo(dstTree, annotationAfter.getLocationInfo());
		if (srcAnnotationTree != null & dstAnnotationTree != null)
			new BasicTreeMatcher().match(srcAnnotationTree,dstAnnotationTree, mappingStore);
		mappingStore.addMapping(srcAnnotationTree,dstAnnotationTree);
	}

	private void findVariablesAndMatch(Tree srcTree, Tree dstTree, AbstractCodeMapping abstractCodeMapping, String originalVariableName, String renamedVariableName) {
		Tree srcStatement = TreeUtilFunctions.findByLocationInfo(srcTree, abstractCodeMapping.getFragment1().getLocationInfo());
		Tree dstStatement = TreeUtilFunctions.findByLocationInfo(dstTree, abstractCodeMapping.getFragment2().getLocationInfo());
		List<Tree> srcRefs = TreeUtilFunctions.findVariable(srcStatement ,originalVariableName);
		List<Tree> dstRefs = TreeUtilFunctions.findVariable(dstStatement ,renamedVariableName);
		if (srcRefs == null || dstRefs == null) return;
		if (srcRefs.size() == 1 && dstRefs.size() == 1)
			optimizationMappingStore.addMapping(srcRefs.get(0),dstRefs.get(0));
		else{
			if (srcRefs.size() == dstRefs.size())
			{
				for (int i = 0; i < srcRefs.size(); i++) {
					optimizationMappingStore.addMapping(srcRefs.get(i),dstRefs.get(i));
				}
			}
		}

	}

	private void processClassImplementedInterfaces(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> commonInterface : classDiff.getInterfaceListDiff().getCommonInterfaces())
			processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, commonInterface.getLeft(), commonInterface.getRight());
		for (org.apache.commons.lang3.tuple.Pair<UMLType, UMLType> changedInterface : classDiff.getInterfaceListDiff().getChangedInterfaces())
			processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, changedInterface.getLeft(), changedInterface.getRight());
	}

	private static void processLocationInfoProvidersRecursively(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, LocationInfoProvider left, LocationInfoProvider right) {
		Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo());
		Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo());
		if (srcSubTree == null || dstSubTree == null) return;
		if (srcSubTree.isIsoStructuralTo(dstSubTree))
			mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
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
	private void processFieldDeclaration(Tree srcTree, Tree dstTree, UMLAttribute srcUMLAttribute,UMLAttribute dstUMLAttribute, ExtendedMultiMappingStore mappingStore) {

		Tree srcAttr = TreeUtilFunctions.findByLocationInfo(srcTree, srcUMLAttribute.getLocationInfo());
		Tree dstAttr = TreeUtilFunctions.findByLocationInfo(dstTree, dstUMLAttribute.getLocationInfo());
		if (srcAttr == null || dstAttr == null) return;
		Tree srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.FIELD_DECLARATION);
		Tree dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.FIELD_DECLARATION);
		if (srcFieldDeclaration == null) {
			srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.ENUM_CONSTANT_DECLARATION);
		}
		if (dstFieldDeclaration == null) {
			dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.ENUM_CONSTANT_DECLARATION);
		}
		//handle Record Components (SingleVariableDeclaration)
		if (srcFieldDeclaration == null) {
			srcFieldDeclaration = TreeUtilFunctions.getParentUntilType(srcAttr, Constants.RECORD_COMPONENT);
		}
		if (dstFieldDeclaration == null) {
			dstFieldDeclaration = TreeUtilFunctions.getParentUntilType(dstAttr, Constants.RECORD_COMPONENT);
		}
		//				|| srcFieldDeclaration.isIsoStructuralTo(dstFieldDeclaration))
		if (srcFieldDeclaration != null && dstFieldDeclaration != null && srcFieldDeclaration.getMetrics().hash == dstFieldDeclaration.getMetrics().hash) {
			//IsoStructural can't be a good idea here, i.e. anonymous class
			mappingStore.addMappingRecursively(srcFieldDeclaration, dstFieldDeclaration);
			return;
		}
		if (srcAttr.getMetrics().hash == dstAttr.getMetrics().hash) {
			mappingStore.addMappingRecursively(srcAttr,dstAttr);
		}

		mappingStore.addMapping(srcFieldDeclaration,dstFieldDeclaration);
		matchFieldAllModifiers(srcFieldDeclaration,dstFieldDeclaration,srcUMLAttribute,dstUMLAttribute,mappingStore);
		matchFieldAnnotations(srcFieldDeclaration, dstFieldDeclaration, mappingStore);
		if (srcUMLAttribute.getType().getLocationInfo() == null || dstUMLAttribute.getType().getLocationInfo() == null) {
			if (srcUMLAttribute instanceof UMLEnumConstant && dstUMLAttribute instanceof UMLEnumConstant) {
				//JavaDocs are mapped as well.
				new LeafMatcher().match(srcAttr,dstAttr,mappingStore);
				return;
			}
		}
		Tree srcType = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getType().getLocationInfo());
		Tree dstType = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getType().getLocationInfo());

		if (srcType != null && dstType != null && srcType.isIsoStructuralTo(dstType)) mappingStore.addMappingRecursively(srcType,dstType);
		else {
			new LeafMatcher().match(srcType,dstType,mappingStore);
		}
		Tree srcVarDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,srcUMLAttribute.getVariableDeclaration().getLocationInfo());
		Tree dstVarDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,dstUMLAttribute.getVariableDeclaration().getLocationInfo());
		mappingStore.addMapping(srcVarDeclaration,dstVarDeclaration);
		new LeafMatcher().match(srcVarDeclaration,dstVarDeclaration,mappingStore);
		processJavaDocs(srcTree,dstTree,srcUMLAttribute.getJavadoc(),dstUMLAttribute.getJavadoc(),mappingStore);
		if (srcVarDeclaration != null && dstVarDeclaration != null)
			mappingStore.addMapping(srcVarDeclaration.getChild(0),dstVarDeclaration.getChild(0));
	}

	private void matchFieldAnnotations(Tree srcFieldDeclaration, Tree dstFieldDeclaration, ExtendedMultiMappingStore mappingStore) {
		Tree srcField = findFirstByType(srcFieldDeclaration, Constants.MARKER_ANNOTATION);
		Tree dstField = findFirstByType(dstFieldDeclaration, Constants.MARKER_ANNOTATION);
		new LeafMatcher().match(srcField, dstField, mappingStore);
	}

	private static Tree findFirstByType(Tree srcFieldDeclaration, String typeName) {
		Tree fieldAnnotation = null;
		for (Tree child : srcFieldDeclaration.getChildren()) {
			if (child.getType().name.equals(typeName)) {
				fieldAnnotation = child;
				break;
			}
		}
		return fieldAnnotation;
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

	private void processSuperClasses(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		UMLType srcParentUML = classDiff.getOldSuperclass();
		UMLType dstParentUML = classDiff.getNewSuperclass();
		if (srcParentUML != null && dstParentUML != null) {
			processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, srcParentUML, dstParentUML);
		}
	}

	private void processPackageDeclaration(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
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
		Set<org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport>> commonImports = importDiffList.getCommonImports();
		String searchingType = Constants.IMPORT_DECLARATION;
		if (!commonImports.isEmpty()) {
			for (org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : commonImports) {
				Tree srcImportStatement = findImportByTypeAndLabel(srcTree, searchingType, pair.getLeft());
				Tree dstImportStatement = findImportByTypeAndLabel(dstTree, searchingType, pair.getRight());
				if (srcImportStatement != null && dstImportStatement != null)
					mappingStore.addMappingRecursively(srcImportStatement, dstImportStatement);
			}
		}
		//Grouped Imports
		for (Map.Entry<Set<UMLImport>, UMLImport> setUMLImportEntry : importDiffList.getGroupedImports().entrySet()) {
			Set<UMLImport> srcImportSet = setUMLImportEntry.getKey();
			UMLImport dstImport = setUMLImportEntry.getValue();
			Tree dstImportStatement = findImportByTypeAndLabel(dstTree,searchingType,dstImport);
			for (UMLImport srcUMLImport : srcImportSet) {
				Tree srcImportStatement = findImportByTypeAndLabel(srcTree,searchingType,srcUMLImport);
				mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
			}
		}
		//UnGrouped Imports
		for (Map.Entry<UMLImport, Set<UMLImport>> umlImportSetEntry : importDiffList.getUnGroupedImports().entrySet()) {
			UMLImport srcImport = umlImportSetEntry.getKey();
			Set<UMLImport> dstImportSet = umlImportSetEntry.getValue();
			Tree srcImportStatement = findImportByTypeAndLabel(srcTree,searchingType,srcImport);
			for (UMLImport dstUMLImport : dstImportSet) {
				Tree dstImportStatement = findImportByTypeAndLabel(dstTree,searchingType,dstUMLImport);
				mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
			}
		}
		//Changed Imports
		for(org.apache.commons.lang3.tuple.Pair<UMLImport, UMLImport> pair : importDiffList.getChangedImports()) {
			Tree srcImportStatement = findImportByTypeAndLabel(srcTree,searchingType,pair.getLeft());
			Tree dstImportStatement = findImportByTypeAndLabel(dstTree,searchingType,pair.getRight());
			mappingStore.addMappingRecursively(srcImportStatement,dstImportStatement);
		}
	}

	private Tree findImportByTypeAndLabel(Tree inputTree, String searchingType, UMLImport label) {
		for (Tree treeNode: inputTree.getChildren()) {
			if (treeNode.getType().name.equals(searchingType)) {
				if (treeNode.getChild(0).getLabel().equals(label.getName()) && treeNode.getPos() == label.getLocationInfo().getStartOffset()) //getChild 0 might be problematic
					if (label.isOnDemand()) {
						if (treeNode.getChild(0).getEndPos() + 3 == treeNode.getEndPos()) {
							return treeNode;
						}
					} else {
						return treeNode;
					}
			}
		}
		return null;
	}

	private void processMethodSignature(Tree srcOperationNode, Tree dstOperationNode, UMLOperationBodyMapper umlOperationBodyMapper, ExtendedMultiMappingStore mappingStore) {
		if (srcOperationNode == null || dstOperationNode == null) return;
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
		if (srcOperationNode == null || dstOperationNode == null) return null;
		Tree srcModifier = TreeUtilFunctions.findChildByType(srcOperationNode,searchingType);
		Tree dstModifier = TreeUtilFunctions.findChildByType(dstOperationNode,searchingType);
		if (srcModifier != null && dstModifier != null)
			return new Pair<>(srcModifier, dstModifier);
		return null;
	}

	private void processClassDeclarationMapping(Tree srcTree, Tree dstTree, UMLClassBaseDiff classDiff, ExtendedMultiMappingStore mappingStore) {
		String AST_type = Constants.TYPE_DECLARATION;
		if (classDiff.getOriginalClass().isEnum()) 
			AST_type = Constants.ENUM_DECLARATION;
		else if (classDiff.getOriginalClass().isAnnotation())
			AST_type = Constants.ANNOTATION_TYPE_DECLARATION;
		else if (classDiff.getOriginalClass().isRecord())
			AST_type = Constants.RECORD_DECLARATION;
		Tree srcTypeDeclaration = TreeUtilFunctions.findByLocationInfo(srcTree,classDiff.getOriginalClass().getLocationInfo(),AST_type);
		Tree dstTypeDeclaration = TreeUtilFunctions.findByLocationInfo(dstTree,classDiff.getNextClass().getLocationInfo(),AST_type);
		if (srcTypeDeclaration == null || dstTypeDeclaration == null) return;
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
		processSuperClasses(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
		processClassImplementedInterfaces(srcTypeDeclaration,dstTypeDeclaration,classDiff,mappingStore);
		processJavaDocs(srcTypeDeclaration,dstTypeDeclaration,classDiff.getOriginalClass().getJavadoc(),classDiff.getNextClass().getJavadoc(),mappingStore);
		processClassAnnotations(srcTypeDeclaration,dstTypeDeclaration,classDiff.getAnnotationListDiff(),mappingStore);

	}

	private void matchModifier(Tree srcTypeDeclaration, Tree dstTypeDeclaration, String modifier, ExtendedMultiMappingStore mappingStore) {
		String type = Constants.MODIFIER;
		Tree srcTree = TreeUtilFunctions.findChildByTypeAndLabel(srcTypeDeclaration,type,modifier);
		Tree dstTree = TreeUtilFunctions.findChildByTypeAndLabel(dstTypeDeclaration,type,modifier);
		if (srcTree != null && dstTree != null){
			mappingStore.addMapping(srcTree,dstTree);
		}
	}
	private final Map<String, OptimizationData> optimizationDataMap = new HashMap<>();

	private static class OptimizationData{
		List<AbstractCodeMapping> lastStepMappings;
		ExtendedMultiMappingStore optimizationMappingStore;
		public OptimizationData(List<AbstractCodeMapping> lastStepMappings, ExtendedMultiMappingStore optimizationMappingStore) {
			this.lastStepMappings = lastStepMappings;
			this.optimizationMappingStore = optimizationMappingStore;
		}
	}
}
