package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;

import gr.uom.java.xmi.diff.ReplaceAnonymousWithClassRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.actions.editscript.SimplifiedExtendedChawatheScriptGenerator;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.moved.AllSubTreesMovedASTDiffGenerator;
import org.refactoringminer.astDiff.moved.MovedASTDiffGenerator;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.matchers.wrappers.*;
import org.refactoringminer.astDiff.matchers.vanilla.MissingIdenticalSubtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.refactoringminer.astDiff.utils.Helpers.findAppends;
import static org.refactoringminer.astDiff.utils.Helpers.findTreeContexts;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ProjectASTDiffer
{
	private final static Logger logger = LoggerFactory.getLogger(ProjectASTDiffer.class);
	private final UMLModelDiff modelDiff;
	private List<Refactoring> modelDiffRefactorings;
	private final ProjectASTDiff projectASTDiff;
	private final MovedASTDiffGenerator movedDeclarationGenerator;
	private final Map<ASTDiff, OptimizationData> optimizationDataMap = new HashMap<>();

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
		//Process the ModelDiffRefactorings once at the end
		UnifiedModelDiffRefactoringsMatcher unifiedModelDiffRefactoringsMatcher = new UnifiedModelDiffRefactoringsMatcher(projectASTDiff.getDiffSet(), optimizationDataMap, modelDiff, modelDiffRefactorings);
		processAllOptimizations(unifiedModelDiffRefactoringsMatcher.getNewlyGeneratedDiffsOptimizationMap());
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
			new MissingIdenticalSubtree().match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
		}
		long diff_execution_finished =  System.currentTimeMillis();
		logger.info("Diff execution: " + (diff_execution_finished - diff_execution_started)/ 1000 + " seconds");
		long movedDiff_execution_started =  System.currentTimeMillis();
		computeAllEditScripts();
		projectASTDiff.addMoveASTDiff(unifiedModelDiffRefactoringsMatcher.getNewlyGeneratedDiffsOptimizationMap().keySet());
		projectASTDiff.addMoveASTDiff(movedDeclarationGenerator.make());
		long movedDiff_execution_finished =  System.currentTimeMillis();
		logger.info("MovedDiff execution: " + (movedDiff_execution_finished - movedDiff_execution_started)/ 1000 + " seconds");
		computeMovedDiffsEditScripts();
	}

	private void processAllOptimizations(Map<ASTDiff, OptimizationData> newlyGeneratedDiffMap) {
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
			new ASTDiffMappingOptimizer(optimizationDataMap.get(diff), diff, modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap()).
					match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
		}
		for (Map.Entry<ASTDiff, OptimizationData> astDiffOptimizationDataEntry : newlyGeneratedDiffMap.entrySet()) {
			ASTDiff diff = astDiffOptimizationDataEntry.getKey();
			OptimizationData optimizationData = astDiffOptimizationDataEntry.getValue();
			new ASTDiffMappingOptimizer(optimizationData, diff, modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap()).
					match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
		}
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
			if (modelDiffRefactoring.getRefactoringType() == RefactoringType.REPLACE_ANONYMOUS_WITH_CLASS)
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
			diff.computeEditScript(modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap(), new SimplifiedExtendedChawatheScriptGenerator());
		}
		long editScript_end = System.currentTimeMillis();
		logger.info("EditScript execution: " + (editScript_end - editScript_start)/ 1000 + " seconds");
	}

	private void computeMovedDiffsEditScripts() {
		for (ASTDiff diff : projectASTDiff.getMoveDiffSet()) {
			Tree srcRoot = diff.src.getRoot();
			Tree dstRoot = diff.dst.getRoot();
			diff.getAllMappings().addMapping(srcRoot, dstRoot); //This helps Chawathe to generate the editscript properly, however the mapping is actually incorrect
			diff.computeEditScript(null, null, new SimplifiedExtendedChawatheScriptGenerator());
			diff.getAllMappings().removeMapping(srcRoot, dstRoot); //Removes the mapping that was added to help Chawathe
		}
	}

	private void makeASTDiff(List<? extends UMLAbstractClassDiff> umlClassBaseDiffList, boolean mergeFlag){
		for (UMLAbstractClassDiff classDiff : umlClassBaseDiffList) {
			Collection<ASTDiff> appends = findAppends(projectASTDiff.getDiffSet(), classDiff.getOriginalClass().getSourceFile(), classDiff.getNextClass().getSourceFile());
			boolean decision = (!appends.isEmpty()) || mergeFlag;
			ASTDiff classASTDiff = process(classDiff, findTreeContexts(modelDiff, classDiff), decision);
			if (!appends.isEmpty()) {
				for (ASTDiff append : appends) {
					append.getAllMappings().mergeMappings(classASTDiff.getAllMappings());
				}
			}
			else {
				projectASTDiff.addASTDiff(classASTDiff);
			}
		}
	}

	private ASTDiff process(UMLAbstractClassDiff classDiff, Pair<TreeContext, TreeContext> treeContextPair,boolean mergeFlag){
		Tree srcTree = treeContextPair.first.getRoot();
		Tree dstTree = treeContextPair.second.getRoot();
		ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		ASTDiff astDiff = new ASTDiff(classDiff.getOriginalClass().getLocationInfo().getFilePath(),
				classDiff.getNextClass().getLocationInfo().getFilePath(),
				treeContextPair.first,
				treeContextPair.second,
				mappingStore);
		optimizationDataMap.putIfAbsent(astDiff,
			new OptimizationData(new ArrayList<>(),
			new ExtendedMultiMappingStore(srcTree,dstTree)));
		OptimizationData optimizationData = optimizationDataMap.get(astDiff);
		new ClassDiffMatcher(optimizationData, classDiff, mergeFlag, modelDiffRefactorings).match(srcTree, dstTree, mappingStore);
		return astDiff;
	}
}
