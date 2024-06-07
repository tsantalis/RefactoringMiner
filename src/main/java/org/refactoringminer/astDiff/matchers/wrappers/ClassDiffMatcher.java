package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;

import java.util.List;

/* Created by pourya on 2024-06-06*/
public class ClassDiffMatcher extends OptimizationAwareMatcher{
    private final UMLAbstractClassDiff classDiff;
    private final boolean mergeFlag;
    private final List<Refactoring> modelDiffRefactorings;

    public ClassDiffMatcher(UMLAbstractClassDiff classDiff, boolean mergeFlag, List<Refactoring> modelDiffRefactorings) {
        this.classDiff = classDiff;
        this.mergeFlag = mergeFlag;
        this.modelDiffRefactorings = modelDiffRefactorings;
    }

    public ClassDiffMatcher(OptimizationData optimizationData, UMLAbstractClassDiff classDiff, boolean mergeFlag, List<Refactoring> modelDiffRefactorings) {
        super(optimizationData);
        this.classDiff = classDiff;
        this.mergeFlag = mergeFlag;
        this.modelDiffRefactorings = modelDiffRefactorings;
    }

    @Override
    void matchAndUpdateOptimizationStore(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        classDiffMatcher(classDiff, mergeFlag, mappingStore, src, dst, optimizationData);
    }

    private void classDiffMatcher(UMLAbstractClassDiff classDiff, boolean mergeFlag, ExtendedMultiMappingStore mappingStore, Tree srcTree, Tree dstTree, OptimizationData optimizationData) {
        if (!mergeFlag) {
            mappingStore.addMapping(srcTree, dstTree);
            new PackageDeclarationMatcher().match(srcTree, dstTree, mappingStore);
        }
        boolean isBaseDiff = classDiff instanceof UMLClassBaseDiff;
        if (isBaseDiff) {
            UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
            new ImportMatcher(baseClassDiff.getImportDiffList()).match(srcTree, dstTree, mappingStore);
            new ClassDeclarationMatcher(optimizationData, baseClassDiff).match(srcTree, dstTree, mappingStore);
        }
        new ClassAttrMatcher(optimizationData, classDiff).match(srcTree, dstTree, mappingStore);
        new EnumConstantsMatcher(classDiff.getCommonEnumConstants()).match(srcTree, dstTree, mappingStore);
        for(UMLOperationBodyMapper umlOperationBodyMapper : classDiff.getOperationBodyMapperList())
            new MethodMatcher(optimizationData, umlOperationBodyMapper).match(srcTree, dstTree, mappingStore);

        if (isBaseDiff){
            UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
            new RefactoringMatcher(optimizationData, modelDiffRefactorings, baseClassDiff).match(srcTree, dstTree, mappingStore);
        }
    }
}
