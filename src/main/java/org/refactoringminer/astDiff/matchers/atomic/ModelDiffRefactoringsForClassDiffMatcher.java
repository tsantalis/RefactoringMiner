package org.refactoringminer.astDiff.matchers.atomic;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.matchers.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.OptimizationData;

import java.util.List;
import java.util.stream.Collectors;

/* Created by pourya on 2024-05-22*/
public class ModelDiffRefactoringsForClassDiffMatcher extends OptimizationAwareMatcher{
    private final UMLModelDiff modelDiff;
    private final List<Refactoring> modelDiffRefactorings;
    private final UMLAbstractClassDiff classDiff;

    //    private final List<Refactoring> modelDiffRefactorings;
    public ModelDiffRefactoringsForClassDiffMatcher(OptimizationData optimizationData, UMLModelDiff modelDiff, List<Refactoring> modelDiffRefactorings, UMLAbstractClassDiff classDiff) {
        super(optimizationData);
        this.modelDiff = modelDiff;
        this.modelDiffRefactorings = modelDiffRefactorings; //This is just for performance boost
        this.classDiff = classDiff;
    }
    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        processModelDiffRefactorings(classDiff, mappingStore);
        processMovedAttributes(classDiff,mappingStore);
    }
    private void processModelDiffRefactorings(UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        for(Refactoring refactoring : modelDiffRefactorings) {
            List<String> beforeRefactoringClasses = refactoring.getInvolvedClassesBeforeRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
            List<String> afterRefactoringClasses = refactoring.getInvolvedClassesAfterRefactoring().stream().map(ImmutablePair::getRight).collect(Collectors.toList());
            if (refactoring instanceof MoveOperationRefactoring) {
                if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
                        beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    if (isExtractedMethodRef(moveOperationRefactoring.getBodyMapper().getOperation2())) return;
                    String srcPath = moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
                    String dstPath = moveOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
                    Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
                    Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
                    new MethodMatcher(optimizationData, moveOperationRefactoring.getBodyMapper()).match(srcTotalTree, dstTotalTree, mappingStore);
                }
            } else if (refactoring instanceof MoveAttributeRefactoring) {
                if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
                        beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
                    MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;
                    String srcPath = moveAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
                    String dstPath = moveAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
                    Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
                    Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
                    new FieldDeclarationMatcher(moveAttributeRefactoring.getOriginalAttribute(), moveAttributeRefactoring.getMovedAttribute()).match(srcTotalTree, dstTotalTree, mappingStore);
                }
            } else if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION)) {
                if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
                        beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    findTreeFromMapperAndProcessBodyMapper(extractOperationRefactoring.getBodyMapper(), mappingStore);
                }
            }
            else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION)) {
                if (afterRefactoringClasses.contains(classDiff.getNextClass().getName()) ||
                        beforeRefactoringClasses.contains(classDiff.getOriginalClass().getName())) {
                    InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                    findTreeFromMapperAndProcessBodyMapper(inlineOperationRefactoring.getBodyMapper(), mappingStore);
                }
            }

        }
    }
    private void findTreeFromMapperAndProcessBodyMapper(UMLOperationBodyMapper bodyMapper, ExtendedMultiMappingStore mappingStore) {
        String srcPath = bodyMapper.getOperation1().getLocationInfo().getFilePath();
        String dstPath = bodyMapper.getOperation2().getLocationInfo().getFilePath();
        Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
        Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
        new BodyMapperMatcher(optimizationData, bodyMapper, true).match(srcTotalTree, dstTotalTree, mappingStore);
    }

    private boolean isExtractedMethodRef(UMLOperation operation2) {
        for (Refactoring modelDiffRefactoring : modelDiffRefactorings) {
            if (modelDiffRefactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
            {
                if (((ExtractOperationRefactoring) modelDiffRefactoring).getExtractedOperation().equals(operation2)) return true;
            }
        }
        return false;
    }

    private void processMovedAttributes(UMLAbstractClassDiff classDiff, ExtendedMultiMappingStore mappingStore) {
        List<UMLAttributeDiff> movedAttributeDiffList = modelDiff.getMovedAttributeDiffList();
        for (UMLAttributeDiff umlAttributeDiff : movedAttributeDiffList) {
            UMLAttribute srcAttr = umlAttributeDiff.getRemovedAttribute();
            UMLAttribute dstAttr = umlAttributeDiff.getAddedAttribute();
            String srcAttrPath = srcAttr.getLocationInfo().getFilePath();
            String dstAttrPath = dstAttr.getLocationInfo().getFilePath();
            if (classDiff.getOriginalClass().getLocationInfo().getFilePath().equals(srcAttrPath) &&
                    classDiff.getNextClass().getLocationInfo().getFilePath().equals(dstAttrPath)) {
                Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcAttrPath).getRoot();
                Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstAttrPath).getRoot();
                processFieldDeclarationByAttrDiff(srcTotalTree,dstTotalTree,umlAttributeDiff,mappingStore);
            }
        }
    }
    private void processFieldDeclarationByAttrDiff(Tree srcTree, Tree dstTree, UMLAttributeDiff umlAttributeDiff, ExtendedMultiMappingStore mappingStore) {
        new FieldDeclarationMatcher(umlAttributeDiff.getRemovedAttribute(), umlAttributeDiff.getAddedAttribute()).match(srcTree,dstTree,mappingStore);
        if (umlAttributeDiff.getInitializerMapper().isPresent()) {
            UMLOperationBodyMapper umlOperationBodyMapper = umlAttributeDiff.getInitializerMapper().get();
            new MethodMatcher(optimizationData, umlOperationBodyMapper).match(srcTree, dstTree, mappingStore);
        }
    }
}
