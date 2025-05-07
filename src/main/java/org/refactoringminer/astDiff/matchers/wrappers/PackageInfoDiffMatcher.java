package org.refactoringminer.astDiff.matchers.wrappers;

import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLPackageInfoDiff;

public class PackageInfoDiffMatcher extends OptimizationAwareMatcher {
    private final UMLPackageInfoDiff packageInfoDiff;
    private final boolean mergeFlag;

    public PackageInfoDiffMatcher(OptimizationData optimizationData, UMLPackageInfoDiff packageInfoDiff, boolean mergeFlag) {
        super(optimizationData);
        this.packageInfoDiff = packageInfoDiff;
        this.mergeFlag = mergeFlag;
    }

	@Override
    void matchAndUpdateOptimizationStore(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        packageInfoDiffMatcher(packageInfoDiff, mergeFlag, mappingStore, src, dst, optimizationData);
    }

    private void packageInfoDiffMatcher(UMLPackageInfoDiff packageInfoDiff, boolean mergeFlag, ExtendedMultiMappingStore mappingStore, Tree srcTree, Tree dstTree, OptimizationData optimizationData) {
        if (!mergeFlag) {
            mappingStore.addMapping(srcTree, dstTree);
            new PackageDeclarationMatcher().match(srcTree, dstTree, mappingStore);
        }
        new ImportMatcher(packageInfoDiff.getImportDiffList()).match(srcTree, dstTree, mappingStore);
        new CommentMatcher(optimizationData, packageInfoDiff.getCommentListDiff()).match(srcTree, dstTree, mappingStore);
        if (packageInfoDiff.getPackageDeclarationJavadocDiff().isPresent()) {
            new JavaDocMatcher(optimizationData, packageInfoDiff.getOriginalPackageInfo().getPackageDoc(), packageInfoDiff.getNextPackageInfo().getPackageDoc(), packageInfoDiff.getPackageDeclarationJavadocDiff())
            .match(srcTree, dstTree, mappingStore);
        }
        UMLAnnotationListDiff annotationListDiff = packageInfoDiff.getAnnotationListDiff();
        for (org.apache.commons.lang3.tuple.Pair<UMLAnnotation, UMLAnnotation> umlAnnotationUMLAnnotationPair : annotationListDiff.getCommonAnnotations()) {
            processLocationInfoProvidersRecursively(srcTree, dstTree, mappingStore, umlAnnotationUMLAnnotationPair.getLeft(), umlAnnotationUMLAnnotationPair.getRight());
        }
    }

    private static void processLocationInfoProvidersRecursively(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore, LocationInfoProvider left, LocationInfoProvider right) {
        Tree srcSubTree = TreeUtilFunctions.findByLocationInfo(srcTree, left.getLocationInfo());
        Tree dstSubTree = TreeUtilFunctions.findByLocationInfo(dstTree, right.getLocationInfo());
        if (srcSubTree == null || dstSubTree == null) return;
        if (srcSubTree.isIsoStructuralTo(dstSubTree)) {
            mappingStore.addMappingRecursively(srcSubTree,dstSubTree);
        }
        else {
            new LeafMatcher().match(srcSubTree,dstSubTree,mappingStore);
        }
    }
}
