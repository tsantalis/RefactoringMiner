package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import gr.uom.java.xmi.diff.UMLEnumConstantDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;

import java.util.List;
import java.util.Set;

/* Created by pourya on 2024-05-22*/
public class ClassAttrMatcher extends OptimizationAwareMatcher {
    private final UMLAbstractClassDiff classDiff;

    public ClassAttrMatcher(UMLAbstractClassDiff classDiff) {
        this.classDiff = classDiff;
    }

    public ClassAttrMatcher(OptimizationData optimizationData, UMLAbstractClassDiff classDiff) {
        super(optimizationData);
        this.classDiff = classDiff;
    }
    @Override
    public void matchAndUpdateOptimizationStore(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processClassAttributes(srcTree, dstTree, classDiff, mappingStore);
    }

    private void processClassAttributes(Tree srcTree, Tree dstTree, UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        Set<Pair<UMLAttribute, UMLAttribute>> pairs = classDiff.getCommonAtrributes();
        for (org.apache.commons.lang3.tuple.Pair<UMLAttribute, UMLAttribute> pair : pairs) {
            new FieldDeclarationMatcher(optimizationData, pair.getLeft(), pair.getRight()).match(srcTree,dstTree,mappingStore);
        }
        List<UMLAttributeDiff> attributeDiffList = classDiff.getAttributeDiffList();
        for (UMLAttributeDiff umlAttributeDiff : attributeDiffList) {
            new FieldDeclarationByAttrDiffMatcher(optimizationData, umlAttributeDiff).match(srcTree, dstTree, mappingStore);
        }
        List<UMLEnumConstantDiff> enumConstantDiffList = classDiff.getEnumConstantDiffList();
        for (UMLEnumConstantDiff enumConstantDiff : enumConstantDiffList) {
            processFieldDeclarationByEnumConstantDiff(srcTree,dstTree,enumConstantDiff,mappingStore);
        }
    }
    private void processFieldDeclarationByEnumConstantDiff(Tree srcTree, Tree dstTree, UMLEnumConstantDiff umlEnumConstantDiff, ExtendedMultiMappingStore mappingStore) {
        new FieldDeclarationMatcher(optimizationData, umlEnumConstantDiff.getRemovedEnumConstant(), umlEnumConstantDiff.getAddedEnumConstant()).match(srcTree,dstTree,mappingStore);
        if(umlEnumConstantDiff.getAnonymousClassDiff().isPresent()) {
            UMLAnonymousClassDiff anonymousClassDiff = umlEnumConstantDiff.getAnonymousClassDiff().get();
            new AnonymousClassDiffMatcher(optimizationData, anonymousClassDiff).match(srcTree, dstTree, mappingStore);
        }
    }
}
