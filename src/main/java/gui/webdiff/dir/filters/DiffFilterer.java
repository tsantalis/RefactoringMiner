package gui.webdiff.dir.filters;

import org.refactoringminer.astDiff.models.ASTDiff;

import java.util.List;

public interface DiffFilterer {
    List<ASTDiff> filter(List<ASTDiff> diffs);
}

