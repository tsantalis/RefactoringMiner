package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.List;

/* Created by pourya on 2024-05-22*/
public class AnonymousClassDiffMatcher extends OptimizationAwareMatcher {

    private final UMLAnonymousClassDiff anonymousClassDiff;
    private final Constants LANG1;
    private final Constants LANG2;

    public AnonymousClassDiffMatcher(UMLAnonymousClassDiff anonymousClassDiff, Constants LANG1, Constants LANG2) {
        this.anonymousClassDiff = anonymousClassDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    public AnonymousClassDiffMatcher(OptimizationData optimizationData, UMLAnonymousClassDiff anonymousClassDiff, Constants LANG1, Constants LANG2) {
        super(optimizationData);
        this.anonymousClassDiff = anonymousClassDiff;
        this.LANG1 = LANG1;
        this.LANG2 = LANG2;
    }

    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processAnonymousClassDiff(srcTree, dstTree, anonymousClassDiff, mappingStore);
    }

    private void processAnonymousClassDiff(Tree srcTree, Tree dstTree, UMLAnonymousClassDiff anonymousClassDiff, ExtendedMultiMappingStore mappingStore) {
        List<UMLOperationBodyMapper> operationBodyMapperList = anonymousClassDiff.getOperationBodyMapperList();
        for (UMLOperationBodyMapper umlOperationBodyMapper : operationBodyMapperList) {
            new MethodMatcher(optimizationData, umlOperationBodyMapper, LANG1, LANG2).match(srcTree,dstTree,mappingStore);
        }
    }
}
