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
    	Constants.setFilePath(diff.getSrcPath());
        for(Action action : diff.editScript) {
            boolean validMultiMove = false;
            if(action instanceof MultiMove) {
                validMultiMove = isFromType(action.getNode(), Constants.get().LINE_COMMENT, Constants.get().TEXT_ELEMENT);
            }
            boolean validUpdate = false;
            if(action instanceof Update) {
                validUpdate = isCommentOrJavaDoc(action.getNode());
            }
            if(!validUpdate && !validMultiMove) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCommentOrJavaDoc(Tree tree) {
        return isFromType(tree, Constants.get().LINE_COMMENT, Constants.get().BLOCK_COMMENT, Constants.get().TEXT_ELEMENT);
    }
}



