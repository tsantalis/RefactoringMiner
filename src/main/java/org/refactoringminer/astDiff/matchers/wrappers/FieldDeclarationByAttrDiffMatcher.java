package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;

/* Created by pourya on 2024-05-22*/
public class FieldDeclarationByAttrDiffMatcher extends OptimizationAwareMatcher {
    private final UMLAttributeDiff umlAttrDiff;

    public FieldDeclarationByAttrDiffMatcher(UMLAttributeDiff umlAttrDiff) {
        this.umlAttrDiff = umlAttrDiff;
    }

    public FieldDeclarationByAttrDiffMatcher(OptimizationData optimizationData, UMLAttributeDiff umlAttrDiff) {
        super(optimizationData);
        this.umlAttrDiff = umlAttrDiff;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processFieldDeclarationByAttrDiff(srcTree, dstTree, umlAttrDiff, mappingStore);
    }
    private void processFieldDeclarationByAttrDiff(Tree srcTree, Tree dstTree, UMLAttributeDiff umlAttributeDiff, ExtendedMultiMappingStore mappingStore) {
        new FieldDeclarationMatcher(optimizationData, umlAttributeDiff.getRemovedAttribute(), umlAttributeDiff.getAddedAttribute()).match(srcTree,dstTree,mappingStore);
        if (umlAttributeDiff.getInitializerMapper().isPresent()) {
            UMLOperationBodyMapper umlOperationBodyMapper = umlAttributeDiff.getInitializerMapper().get();
            new MethodMatcher(optimizationData, umlOperationBodyMapper).match(srcTree, dstTree, mappingStore);
        }
    }
}
