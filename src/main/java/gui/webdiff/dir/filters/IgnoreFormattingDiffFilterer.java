package gui.webdiff.dir.filters;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static org.refactoringminer.astDiff.utils.TreeUtilFunctions.isFromType;

public class IgnoreFormattingDiffFilterer implements DiffFilterer {
    @Override
    public List<ASTDiff> filter(List<ASTDiff> diffs) {
        return new ArrayList<>(diffs.stream().filter(this::changesOtherThanCommentUpdates).toList());
    }

    public boolean changesOtherThanCommentUpdates(ASTDiff diff) {
        for(Action action : diff.editScript) {
            boolean validMultiMove = false;
            if(action instanceof MultiMove) {
                validMultiMove = isFromType(action.getNode(), diff.LANG1.LINE_COMMENT, diff.LANG1.TEXT_ELEMENT, diff.LANG2.LINE_COMMENT, diff.LANG2.TEXT_ELEMENT);
            }
            boolean validUpdate = false;
            if(action instanceof Update) {
                validUpdate = isCommentOrJavaDoc(action.getNode(), diff.LANG1, diff.LANG2);
            }
            if(!validUpdate && !validMultiMove) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCommentOrJavaDoc(Tree tree, Constants LANG1, Constants LANG2) {
        return isFromType(tree, LANG1.LINE_COMMENT, LANG1.BLOCK_COMMENT, LANG1.TEXT_ELEMENT,
                LANG2.LINE_COMMENT, LANG2.BLOCK_COMMENT, LANG2.TEXT_ELEMENT);
    }
}



