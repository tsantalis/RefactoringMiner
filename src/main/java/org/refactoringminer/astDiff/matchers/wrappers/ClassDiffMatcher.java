package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.List;

/* Created by pourya on 2024-06-06*/
public class ClassDiffMatcher extends OptimizationAwareMatcher{
    private final UMLAbstractClassDiff classDiff;
    private final boolean mergeFlag;
    private final List<Refactoring> modelDiffRefactorings;
    private final Constants LANG1;
    private final Constants LANG2;

    public ClassDiffMatcher(UMLAbstractClassDiff classDiff, boolean mergeFlag, List<Refactoring> modelDiffRefactorings, Constants LANG1, Constants LANG2) {
        this.classDiff = classDiff;
        this.mergeFlag = mergeFlag;
        this.modelDiffRefactorings = modelDiffRefactorings;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    public ClassDiffMatcher(OptimizationData optimizationData, UMLAbstractClassDiff classDiff, boolean mergeFlag, List<Refactoring> modelDiffRefactorings, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.classDiff = classDiff;
        this.mergeFlag = mergeFlag;
        this.modelDiffRefactorings = modelDiffRefactorings;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    void matchAndUpdateOptimizationStore(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        classDiffMatcher(classDiff, mergeFlag, mappingStore, src, dst, optimizationData);
    }

    private void classDiffMatcher(UMLAbstractClassDiff classDiff, boolean mergeFlag, ExtendedMultiMappingStore mappingStore, Tree srcTree, Tree dstTree, OptimizationData optimizationData) {
        if (!mergeFlag) {
            mappingStore.addMapping(srcTree, dstTree);
            new PackageDeclarationMatcher(LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        }
        boolean isBaseDiff = classDiff instanceof UMLClassBaseDiff;
        if (isBaseDiff) {
            UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
            new ImportMatcher(baseClassDiff.getImportDiffList(), LANG1, LANG2).match(srcTree, dstTree, mappingStore);
            new ClassDeclarationMatcher(optimizationData, baseClassDiff, LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        }
        new ClassAttrMatcher(optimizationData, classDiff, LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        new EnumConstantsMatcher(optimizationData, classDiff.getCommonEnumConstants(), LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        for(UMLOperationBodyMapper umlOperationBodyMapper : classDiff.getOperationBodyMapperList())
            new MethodMatcher(optimizationData, umlOperationBodyMapper, LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        new CommentMatcher(optimizationData, classDiff.getCommentListDiff(), LANG1, LANG2).match(srcTree, dstTree, mappingStore);

        if (isBaseDiff){
            UMLClassBaseDiff baseClassDiff = (UMLClassBaseDiff) classDiff;
            new RefactoringMatcher(optimizationData, modelDiffRefactorings, baseClassDiff).match(srcTree, dstTree, mappingStore);
        }
    }
}
