package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.diff.*;
import gr.uom.java.xmi.diff.MoveCodeRefactoring.Type;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;

import java.util.*;

import static org.refactoringminer.astDiff.utils.Helpers.findAppends;
import static org.refactoringminer.astDiff.utils.Helpers.findTreeContexts;

/* Created by pourya on 2024-06-04*/
public class UnifiedModelDiffRefactoringsMatcher {
    private final Set<ASTDiff> diffSet;
    private final Map<ASTDiff, OptimizationData> optimizationDataMap;
    private final UMLModelDiff modelDiff;
    private final List<Refactoring> modelDiffRefactorings;

    private final Map<ASTDiff, OptimizationData> newlyGeneratedDiffsOptimizationMap = new LinkedHashMap<>();

    public Map<ASTDiff, OptimizationData> getNewlyGeneratedDiffsOptimizationMap() {
        return newlyGeneratedDiffsOptimizationMap;
    }

    public UnifiedModelDiffRefactoringsMatcher(Set<ASTDiff> diffSet, Map<ASTDiff, OptimizationData> optimizationDataMap, UMLModelDiff modelDiff, List<Refactoring> modelDiffRefactorings) {
        this.diffSet = diffSet;
        this.optimizationDataMap = optimizationDataMap;
        this.modelDiff = modelDiff;
        this.modelDiffRefactorings = modelDiffRefactorings;
        process();
    }

    private void process() {
        processModelDiffCommentsMovedBetweenClasses();
        processModeldiffRefactorings();
        processModeldiffMovedAttributes();
    }

    private void processModelDiffCommentsMovedBetweenClasses() {
        // TODO: Its not the most optimized one.
        //  I am iterating through all the astDiffs, and then I am checking if the srcPath or dstPath is equal to the comment's path.
        //  However since this information comes from modelDiff, there is no other clear alternative.
        for (UMLCommentListDiff commentsMovedBetweenClass : modelDiff.getCommentsMovedBetweenClasses()) {
            for (org.apache.commons.lang3.tuple.Pair<UMLComment, UMLComment> commonComment : commentsMovedBetweenClass.getCommonComments()) {
                UMLComment left = commonComment.getLeft();
                UMLComment right = commonComment.getRight();
                findDiffsAndApplyMatcher(
                        left.getLocationInfo().getFilePath(),
                        right.getLocationInfo().getFilePath(),
                        new CommentMatcher(null, commentsMovedBetweenClass));
                break;
                //We break because the comments always belong to the same pair of files, and the CommentMatcher handles everything
                //Basically, need to take one pair to find the locations of the comments. The For loop is just overkill.

            }
        }
    }

    private void processModeldiffMovedAttributes() {
        for (UMLAttributeDiff umlAttributeDiff : modelDiff.getMovedAttributeDiffList()) {
            UMLAttribute srcAttr = umlAttributeDiff.getRemovedAttribute();
            UMLAttribute dstAttr = umlAttributeDiff.getAddedAttribute();
            String srcPath = srcAttr.getLocationInfo().getFilePath();
            String dstPath = dstAttr.getLocationInfo().getFilePath();
            findDiffsAndApplyMatcher(srcPath, dstPath, new FieldDeclarationByAttrDiffMatcher(umlAttributeDiff));
        }
    }

    private void processModeldiffRefactorings() {
        for(Refactoring refactoring : modelDiffRefactorings) {
            if (refactoring instanceof MoveOperationRefactoring)
            {
                MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
//                if (isExtractedMethodRef(moveOperationRefactoring.getBodyMapper().getOperation2())) continue;
                String srcPath = moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath();
                String dstPath = moveOperationRefactoring.getMovedOperation().getLocationInfo().getFilePath();
                findDiffsAndApplyMatcher(srcPath, dstPath, new MethodMatcher(moveOperationRefactoring.getBodyMapper(), true));
                if (moveOperationRefactoring.getOriginalOperation().getJavadoc() == null && moveOperationRefactoring.getMovedOperation().getJavadoc() != null) {
                    UMLClass originalClass = modelDiff.getRemovedClass(moveOperationRefactoring.getOriginalOperation().getClassName());
                    if (originalClass != null && originalClass.getJavadoc() != null) {
                        findDiffsAndApplyMatcher(srcPath, dstPath, new JavaDocMatcher(originalClass.getJavadoc(), moveOperationRefactoring.getMovedOperation().getJavadoc(), moveOperationRefactoring.getBodyMapper().getJavadocDiff()));
                    }
                }
            }
            else if (refactoring instanceof MoveAttributeRefactoring)
            {
                MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;
                String srcPath = moveAttributeRefactoring.getOriginalAttribute().getLocationInfo().getFilePath();
                String dstPath = moveAttributeRefactoring.getMovedAttribute().getLocationInfo().getFilePath();
                findDiffsAndApplyMatcher(srcPath, dstPath, 
                new FieldDeclarationMatcher(moveAttributeRefactoring.getOriginalAttribute(), moveAttributeRefactoring.getMovedAttribute(),
                		(moveAttributeRefactoring.getOriginalAttribute().getJavadoc() != null && moveAttributeRefactoring.getMovedAttribute().getJavadoc() != null) ?
                                Optional.of(new UMLJavadocDiff(moveAttributeRefactoring.getOriginalAttribute().getJavadoc(), moveAttributeRefactoring.getMovedAttribute().getJavadoc()))
                                /* TODO : Replace with movedAttr.getJavaDocDiff() */
                                :
                                Optional.empty(),
                                new UMLCommentListDiff(moveAttributeRefactoring.getOriginalAttribute().getComments(), moveAttributeRefactoring.getMovedAttribute().getComments())));
            }
            else if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION) ||
            		refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
            {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                String srcPath = extractOperationRefactoring.getBodyMapper().getContainer1().getLocationInfo().getFilePath();
                String dstPath = extractOperationRefactoring.getBodyMapper().getContainer2().getLocationInfo().getFilePath();
                findDiffsAndApplyMatcher(srcPath, dstPath, new BodyMapperMatcher(extractOperationRefactoring.getBodyMapper(), true));
            }
            else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_AND_INLINE_OPERATION)) {
                InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                String srcPath = inlineOperationRefactoring.getBodyMapper().getContainer1().getLocationInfo().getFilePath();
                String dstPath = inlineOperationRefactoring.getBodyMapper().getContainer2().getLocationInfo().getFilePath();
                findDiffsAndApplyMatcher(srcPath, dstPath, new BodyMapperMatcher(inlineOperationRefactoring.getBodyMapper(), true));
            }
            else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_CODE)) {
            	MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
            	if (moveCodeRefactoring.getMoveType().equals(Type.MOVE_BETWEEN_FILES)) {
            		String srcPath = moveCodeRefactoring.getBodyMapper().getContainer1().getLocationInfo().getFilePath();
                    String dstPath = moveCodeRefactoring.getBodyMapper().getContainer2().getLocationInfo().getFilePath();
                    findDiffsAndApplyMatcher(srcPath, dstPath, new BodyMapperMatcher(moveCodeRefactoring.getBodyMapper(), true));
            	}
            }
        }
    }

    private void findDiffsAndApplyMatcher(String srcPath, String dstPath, OptimizationAwareMatcher matcher) {
        Collection<ASTDiff> diffs = findAppends(diffSet, srcPath, dstPath, false);
        if (diffs.isEmpty()) {
            //This means that the there is no astDiff associated with the refactoring,
            //It could be a case of move from a deleted file to an added file which neither of them have an equivalent file in the other side.
            //In this case, we should create a new ASTDiff and apply the matcher on it.
            Collection<ASTDiff> appends = findAppends(newlyGeneratedDiffsOptimizationMap.keySet(), srcPath, dstPath, true);
            if (appends.isEmpty()) {
                Pair<TreeContext, TreeContext> treeContextPair = findTreeContexts(modelDiff, srcPath, dstPath);
                Tree srcTree = treeContextPair.first.getRoot();
                Tree dstTree = treeContextPair.second.getRoot();
                ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTree, dstTree);
                ASTDiff diff = new ASTDiff(srcPath, dstPath,
                    treeContextPair.first, treeContextPair.second,
                    mappingStore);
                newlyGeneratedDiffsOptimizationMap.putIfAbsent(diff, new OptimizationData(new ArrayList<>(), new ExtendedMultiMappingStore(srcTree,dstTree)));
                setOptimizationAndMatch(matcher, diff, newlyGeneratedDiffsOptimizationMap.get(diff), srcTree, dstTree);
                }
            else {
                for (ASTDiff append : appends) {
                    setOptimizationAndMatch(matcher, append, newlyGeneratedDiffsOptimizationMap.get(append), append.src.getRoot(),  append.dst.getRoot());
                }
            }
        }
        for (ASTDiff diff : diffs) {
            Tree srcTotalTree = modelDiff.getParentModel().getTreeContextMap().get(srcPath).getRoot();
            Tree dstTotalTree = modelDiff.getChildModel().getTreeContextMap().get(dstPath).getRoot();
            OptimizationData optimizationData = optimizationDataMap.get(diff);
            setOptimizationAndMatch(matcher, diff, optimizationData, srcTotalTree, dstTotalTree);
        }
    }

    private static void setOptimizationAndMatch(OptimizationAwareMatcher matcher, ASTDiff diff, OptimizationData optimizationData, Tree srcTotalTree, Tree dstTotalTree) {
        matcher.setOptimizationData(optimizationData);
        matcher.match(srcTotalTree, dstTotalTree, diff.getAllMappings());
    }


}
