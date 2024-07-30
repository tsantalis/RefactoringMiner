package gui.webdiff;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.refactoringminer.astDiff.utils.ExportUtils.getFileNameFromSrcDiff;

/* Created by pourya on 2024-02-09*/
public class WebDiffRunnerCli {
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
    @Parameter(names = {"-e", "--export"}, description = "Export Mappings/Actions into files")
    boolean export = false;


    private static final String HELP_MSG = """
You can run the diff with the following options:
    --url <commit-url>                           \t\t      Run the diff with a GitHub commit url
    --url <pr-url>                               \t\t      Run the diff with a GitHub PullRequest url
    --src <folder1> --dst <folder2>            \t\t              Run the diff with two local directories
    --repo <repo-folder-path> --commit <commitID>  \t\t      Run the diff with a locally cloned GitHub repo

To export the mappings/actions, add --export to the end of the command.
""";
    public void execute(String[] args) {
        JCommander jCommander = JCommander.newBuilder()
                .addObject(this)
                .build();
        jCommander.parse(args);

        if (help) {
            System.out.println(HELP_MSG);
//            jCommander.usage();
            return;
        }
        RunMode runMode;
        try{
            runMode = RunMode.getRunMode(this);
        }
        catch (Exception e){
            System.out.println(HELP_MSG);
//            jCommander.usage();
            return;
        }
        try {
            ProjectASTDiff projectASTDiff = runMode.getProjectASTDIFF(this);
            if (export){
                export(projectASTDiff);
            }
            else
                new WebDiff(projectASTDiff).run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void export(ProjectASTDiff projectASTDiff) throws IOException {
        for (ASTDiff astDiff : projectASTDiff.getDiffSet()) {
            String fileNameFromSrcDiff = getFileNameFromSrcDiff(astDiff.getSrcPath());
            int lastIndex = fileNameFromSrcDiff.lastIndexOf(".json");
            if (lastIndex != -1) {
                fileNameFromSrcDiff = fileNameFromSrcDiff.substring(0, lastIndex) + fileNameFromSrcDiff.substring(lastIndex + 5);
            }
            MappingExportModel.exportToFile(new File(fileNameFromSrcDiff + "_mappings.json"), astDiff.getAllMappings());
            MappingExportModel.exportActions(new File(fileNameFromSrcDiff + "_actions.txt"), astDiff);
        }
    }

    enum RunMode{
        URL,
        PR,
        DIR,
        CLONED;
        public static RunMode getRunMode(WebDiffRunnerCli runner) {
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

        public ProjectASTDiff getProjectASTDIFF(WebDiffRunnerCli runner) throws Exception {
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
