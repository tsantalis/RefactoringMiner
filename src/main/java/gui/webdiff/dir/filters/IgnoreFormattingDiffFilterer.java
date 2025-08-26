package gui.webdiff.dir.filters;

import com.github.gumtreediff.actions.model.*;
import org.refactoringminer.astDiff.actions.model.MoveIn;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;

import java.util.List;

public class IgnoreFormattingDiffFilterer implements DiffFilterer {
    @Override
    public List<ASTDiff> filter(List<ASTDiff> diffs) {
        return diffs.stream().filter(this::isPureFormatting).toList();
    }

    private boolean isPureFormatting(ASTDiff diff) {
        for (Action action : diff.editScript) {
            if (action instanceof Addition || action instanceof Delete || action instanceof Update)
                return false;
            if (action instanceof MoveIn || action instanceof MoveOut)
                return false;
            if (action instanceof MultiMove || action instanceof Move){
                //TODO:
            }
            //TODO
        }
        return true;
    }
}

