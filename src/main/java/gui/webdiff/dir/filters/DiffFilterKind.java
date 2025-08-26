package gui.webdiff.dir.filters;

import org.refactoringminer.astDiff.models.ASTDiff;

import java.util.List;

public enum DiffFilterKind implements DiffFilterer {
    NO_FILTER(diffs -> diffs),
    IGNORE_FORMATTING(new IgnoreFormattingDiffFilterer()),
    ;


    public final DiffFilterer filterer;
    DiffFilterKind(DiffFilterer filterer) {
        this.filterer = filterer;
    }

    public static DiffFilterKind fromOptions(DiffFilteringOptions options) {
        if (options.ignoreFormatting())
            return IGNORE_FORMATTING;
        return NO_FILTER;
    }

    @Override
    public List<ASTDiff> filter(List<ASTDiff> diffs) {
        return filterer.filter(diffs);
    }
}
