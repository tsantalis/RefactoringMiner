package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;

/* Created by pourya on 2024-05-22*/
public class FieldDeclarationByAttrDiffMatcher extends OptimizationAwareMatcher {
    private final UMLAttributeDiff umlAttrDiff;
    private final Constants LANG1;
    private final Constants LANG2;

    public FieldDeclarationByAttrDiffMatcher(UMLAttributeDiff umlAttrDiff, Constants LANG1, Constants LANG2) {
        this.umlAttrDiff = umlAttrDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    public FieldDeclarationByAttrDiffMatcher(OptimizationData optimizationData, UMLAttributeDiff umlAttrDiff, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.umlAttrDiff = umlAttrDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processFieldDeclarationByAttrDiff(srcTree, dstTree, umlAttrDiff, mappingStore);
    }
    private void processFieldDeclarationByAttrDiff(Tree srcTree, Tree dstTree, UMLAttributeDiff umlAttributeDiff, ExtendedMultiMappingStore mappingStore) {
        new FieldDeclarationMatcher(optimizationData, umlAttributeDiff.getRemovedAttribute(), umlAttributeDiff.getAddedAttribute(), umlAttributeDiff.getJavadocDiff(), umlAttributeDiff.getCommentListDiff(), LANG1, LANG2).match(srcTree,dstTree,mappingStore);
        if (umlAttributeDiff.getInitializerMapper().isPresent()) {
            UMLOperationBodyMapper umlOperationBodyMapper = umlAttributeDiff.getInitializerMapper().get();
            new MethodMatcher(optimizationData, umlOperationBodyMapper, LANG1, LANG2).match(srcTree, dstTree, mappingStore);
            //TODO: if its a moved process its refactoring
        }
    }
}
