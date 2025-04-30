package gui.webdiff;

import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.perforce.PerforceHistoryRefactoringMinerImpl;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public enum RunMode{
    URL,
    PR,
    DIR,
    CLONED,
    PERFORCE_CL;
    private static final int timeout = 1000;
    public static RunMode getRunMode(DiffRunner runner) {
        if (runner.commit != null &&
                runner.perforceUserName != null &&
                runner.perforcePassword != null &&
                runner.url != null
        ) {
            return PERFORCE_CL;
        }
        if (runner.url != null)
        {
            if (URLHelper.isPR(runner.url)) return PR;
            else return URL;
        }
        else if (runner.src != null && runner.dst != null) return DIR;
        else if (runner.repo != null && runner.commit != null) return CLONED;
        else {
            throw new RuntimeException("Invalid mode");
        }
    }

    public ProjectASTDiff getProjectASTDIFF(DiffRunner runner) throws Exception {
        return switch (this) {
            case URL -> new GitHistoryRefactoringMinerImpl().diffAtCommit(
                    URLHelper.getRepo(runner.url),
                    URLHelper.getCommit(runner.url),
                    timeout);
            case PR -> new GitHistoryRefactoringMinerImpl().diffAtPullRequest(
                    URLHelper.getRepo(runner.url),
                    URLHelper.getPullRequestID(runner.url),
                    timeout);
            case DIR -> new GitHistoryRefactoringMinerImpl().diffAtDirectories(
                    runner.src.toAbsolutePath().normalize(),
                    runner.dst.toAbsolutePath().normalize());
            case CLONED -> new GitHistoryRefactoringMinerImpl().diffAtCommit(
                    new GitServiceImpl().openRepository(runner.repo), runner.commit);
            case PERFORCE_CL -> new PerforceHistoryRefactoringMinerImpl().diffAtChangeList(
                    runner.url,
                    runner.perforceUserName,
                    runner.perforcePassword,
                    Integer.parseInt(runner.commit));
        };
    }
}
