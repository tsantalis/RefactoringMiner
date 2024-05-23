package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ReplaceAnonymousWithClassRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.AllSubTreesMovedASTDiffGenerator;
import org.refactoringminer.astDiff.MovedASTDiffGenerator;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;
import org.refactoringminer.astDiff.matchers.atomic.*;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.refactoringminer.astDiff.utils.Helpers.findTreeContexts;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ProjectASTDiffer
{
	private final static Logger logger = LoggerFactory.getLogger(ProjectASTDiffer.class);
	private final UMLModelDiff modelDiff;
	private OptimizationData optimizationData;
	private List<Refactoring> modelDiffRefactorings;
	private final ProjectASTDiff projectASTDiff;
	private final MovedASTDiffGenerator movedDeclarationGenerator;
	private final Map<String, OptimizationData> optimizationDataMap = new HashMap<>();

	public ProjectASTDiffer(UMLModelDiff modelDiff, Map<String, String> fileContentsBefore, Map<String, String> fileContentsAfter) throws RefactoringMinerTimedOutException {
		this.modelDiff = modelDiff;
		this.projectASTDiff = new ProjectASTDiff(fileContentsBefore, fileContentsAfter);
//		movedDeclarationGenerator = new MovedDeclarationGenerator(modelDiff, projectASTDiff);
		movedDeclarationGenerator = new AllSubTreesMovedASTDiffGenerator(modelDiff, projectASTDiff);
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
		long movedDiff_execution_started =  System.currentTimeMillis();
		projectASTDiff.addMoveASTDiff(movedDeclarationGenerator.make());
		long movedDiff_execution_finished =  System.currentTimeMillis();
		logger.info("MovedDiff execution: " + (movedDiff_execution_finished - movedDiff_execution_started)/ 1000 + " seconds");


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
			ASTDiff classASTDiff = process(classDiff, findTreeContexts(modelDiff, classDiff), decision);
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

	private ASTDiff process(UMLAbstractClassDiff classDiff, Pair<TreeContext, TreeContext> treeContextPair,boolean mergeFlag){
		TreeContext srcTreeContext = treeContextPair.first;
		TreeContext dstTreeContext = treeContextPair.second;
		Tree srcTree = srcTreeContext.getRoot();
		Tree dstTree = dstTreeContext.getRoot();
		ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);

		String srcFilePath = classDiff.getOriginalClass().getLocationInfo().getFilePath();
		String dstFilePath = classDiff.getNextClass().getLocationInfo().getFilePath();
		optimizationDataMap.putIfAbsent(srcFilePath,
			new OptimizationData(new ArrayList<>(),
			new ExtendedMultiMappingStore(srcTree,dstTree)));
		optimizationData = optimizationDataMap.get(srcFilePath);
		if (!mergeFlag) {
			mappingStore.addMapping(srcTree, dstTree);
			new PackageDeclarationMatcher().match(srcTree, dstTree, mappingStore);
		}
		boolean isBaseDiff = classDiff instanceof UMLClassBaseDiff;
		if (isBaseDiff) {
			UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
			new ImportMatcher(baseClassDiff.getImportDiffList()).match(srcTree, dstTree, mappingStore);
			new ClassDeclarationMatcher(baseClassDiff).match(srcTree, dstTree, mappingStore);
		}
		new ClassAttrMatcher(optimizationData, classDiff).match(srcTree, dstTree, mappingStore);
		new EnumConstantsMatcher(classDiff.getCommonEnumConstants()).match(srcTree,dstTree,mappingStore);
		for(UMLOperationBodyMapper umlOperationBodyMapper : classDiff.getOperationBodyMapperList())
			new MethodMatcher(optimizationData, umlOperationBodyMapper).match(srcTree, dstTree, mappingStore);
		new ModelDiffRefactoringsForClassDiffMatcher(optimizationData, modelDiff, modelDiffRefactorings, classDiff).match(srcTree,dstTree,mappingStore);

		if (isBaseDiff){
			UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
			new RefactoringMatcher(optimizationData, modelDiffRefactorings, baseClassDiff).match(srcTree,dstTree,mappingStore);
		}
		return new ASTDiff(srcFilePath, dstFilePath,
				treeContextPair.first, treeContextPair.second,
				mappingStore);
	}
	private static void processOptimization(ASTDiff input, OptimizationData optimizationData) {
		Tree srcTree = input.src.getRoot();
		Tree dstTree = input.dst.getRoot();
		ExtendedMultiMappingStore lastStepMappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		for (AbstractCodeMapping lastStepMapping : optimizationData.getLastStepMappings()) {
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
		allMappings.replaceWithOptimizedMappings(optimizationData.getSubtreeMappings());
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
}
