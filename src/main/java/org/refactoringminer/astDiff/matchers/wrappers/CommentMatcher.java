package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.diff.UMLCommentListDiff;
import gr.uom.java.xmi.diff.UMLJavadocDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.Optional;

/* Created by pourya on 2024-10-02*/
public class CommentMatcher extends OptimizationAwareMatcher {


    private final UMLCommentListDiff commentListDiff;

    public CommentMatcher(OptimizationData optimizationData, UMLCommentListDiff commentListDiff) {
        super(optimizationData);
        this.commentListDiff = commentListDiff;
    }

    @Override
    void matchAndUpdateOptimizationStore(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        if (commentListDiff != null) {
            for (Pair<UMLComment, UMLComment> commonComment : commentListDiff.getCommonComments()) {
                UMLComment left = commonComment.getLeft();
                UMLComment right = commonComment.getRight();
                if (left.getJavaDoc().isPresent() && right.getJavaDoc().isPresent())
                {
                    UMLJavadoc leftJavadoc = left.getJavaDoc().get();
                    UMLJavadoc rightJavadoc = right.getJavaDoc().get();
                    new JavaDocMatcher(optimizationData, leftJavadoc, rightJavadoc, Optional.of(new UMLJavadocDiff(leftJavadoc, rightJavadoc))).
                            matchAndUpdateOptimizationStore(src, dst, mappingStore);
                }
                else {
                    Tree srcComment = TreeUtilFunctions.findByLocationInfo(src, commonComment.getLeft().getLocationInfo());
                    Tree dstComment = TreeUtilFunctions.findByLocationInfo(dst, commonComment.getRight().getLocationInfo());
                    if (srcComment != null && dstComment != null) {
                        mappingStore.addMapping(srcComment, dstComment);
                    }
                }
            }
        }
    }
}
