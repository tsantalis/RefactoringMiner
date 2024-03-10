package gui.webdiff;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.nio.file.Path;

/* Created by pourya on 2024-02-09*/
public class WebDiffRunner {
    private static final int timeout = 1000;
    @Parameter(names = {"-u", "--url"}, description = "URL of the commit/PR" , order = 0)
    String url;
    @Parameter(names = {"-s", "--src"}, description = "Source directory", order = 1)
    Path src;
    @Parameter(names = {"-d", "--dst"}, description = "Destination directory", order = 2)
    Path dst;
    @Parameter(names = {"-r", "--repo"}, description = "Repository path (locally cloned repo)", order = 3)
    String repo;
    @Parameter(names = {"-c", "--commit"}, description = "Commit ID for locally cloned repo", order = 4)
    String commit;
    @Parameter(names = {"-h", "--help"}, description = "Help", help = true)
    boolean help;

    public void execute(String[] args) {
        JCommander jCommander = JCommander.newBuilder()
                .addObject(this)
                .build();
        jCommander.parse(args);

        if (help) {
            jCommander.usage();
            return;
        }
        RunMode runMode = RunMode.getRunMode(this);
        try {
            ProjectASTDiff projectASTDiff = runMode.getProjectASTDIFF(this);
            new WebDiff(projectASTDiff).run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    enum RunMode{
        URL,
        PR,
        DIR,
        CLONED;
        public static RunMode getRunMode(WebDiffRunner runner) {
            if (runner.url != null)
            {
                if (URLHelper.isPR(runner.url)) return PR;
                else return URL;
            }
            else if (runner.src != null && runner.dst != null) return DIR;
            else if (runner.repo != null && runner.commit != null) return CLONED;
            else throw new RuntimeException("Invalid mode");
        }

        public ProjectASTDiff getProjectASTDIFF(WebDiffRunner runner) throws Exception {
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
            };
        }
    }

}
