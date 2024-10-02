package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.diff.UMLCommentListDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

/* Created by pourya on 2024-10-02*/
public class CommentMatcher implements TreeMatcher {


    private final UMLCommentListDiff commentListDiff;

    public CommentMatcher(UMLCommentListDiff commonComment) {
        this.commentListDiff = commonComment;
    }

    @Override
    public void match(Tree srcTree, Tree dstTree, ExtendedMultiMappingStore mappingStore) {
        for (Pair<UMLComment, UMLComment> commonComment : commentListDiff.getCommonComments()) {
            Tree srcComment = TreeUtilFunctions.findByLocationInfo(srcTree, commonComment.getLeft().getLocationInfo());
            Tree dstComment = TreeUtilFunctions.findByLocationInfo(dstTree, commonComment.getRight().getLocationInfo());
            if (srcComment != null && dstComment != null) {
                mappingStore.addMapping(srcComment, dstComment);
            }
        }
    }
}
