package org.refactoringminer.astDiff.matchers.wrappers;

import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAbstractModuleDirective;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLModuleDiff;
import gr.uom.java.xmi.diff.UMLModuleDirectiveListDiff;

public class ModuleInfoDiffMatcher extends OptimizationAwareMatcher {
    private final UMLModuleDiff moduleDiff;
    private final boolean mergeFlag;
    private final Constants LANG1;
    private final Constants LANG2;

    public ModuleInfoDiffMatcher(OptimizationData optimizationData, UMLModuleDiff moduleDiff, boolean mergeFlag, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.moduleDiff = moduleDiff;
        this.mergeFlag = mergeFlag;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

	@Override
    void matchAndUpdateOptimizationStore(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        moduleDiffMatcher(moduleDiff, mergeFlag, mappingStore, src, dst, optimizationData);
    }

    private void moduleDiffMatcher(UMLModuleDiff moduleDiff, boolean mergeFlag, ExtendedMultiMappingStore mappingStore, Tree srcTree, Tree dstTree, OptimizationData optimizationData) {
        if (!mergeFlag) {
            mappingStore.addMapping(srcTree, dstTree);
            new ModuleDeclarationMatcher(LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        }
        new CommentMatcher(optimizationData, moduleDiff.getCommentListDiff(), LANG1, LANG2).match(srcTree, dstTree, mappingStore);
        if (moduleDiff.getJavadocDiff().isPresent()) {
            new JavaDocMatcher(optimizationData, moduleDiff.getOriginalModule().getJavadoc(), moduleDiff.getNextModule().getJavadoc(), moduleDiff.getJavadocDiff(), LANG1, LANG2)
            .match(srcTree, dstTree, mappingStore);
        }
        UMLAnnotationListDiff annotationListDiff = moduleDiff.getAnnotationListDiff();
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, umlAnnotationUMLAnnotationPair.getLeft(), umlAnnotationUMLAnnotationPair.getRight());
        }
        UMLModuleDirectiveListDiff moduleDirectiveListDiff = moduleDiff.getDirectiveListDiff();
        for (org.apache.commons.lang3.tuple.Pair<UMLAbstractModuleDirective, UMLAbstractModuleDirective> directivePair : moduleDirectiveListDiff.getCommonDirectives()) {
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, directivePair.getLeft(), directivePair.getRight());
        }
        for (org.apache.commons.lang3.tuple.Pair<UMLAbstractModuleDirective, UMLAbstractModuleDirective> directivePair : moduleDirectiveListDiff.getChangedDirectives()) {
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, directivePair.getLeft(), directivePair.getRight());
        }
    }

    private void processLocationInfoProvidersRecursively(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, LocationInfoProvider left, LocationInfoProvider right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo(), LANG1);
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo(), LANG2);
        if (srcSubTree == null || dstSubTree == null) return;
        if (srcSubTree.isIsoStructuralTo(dstSubTree)) {
            mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
        }
        else {
            new LeafMatcher(LANG1, LANG2).match(srcSubTree,dstSubTree,mappingStore);
        }
    }
}
