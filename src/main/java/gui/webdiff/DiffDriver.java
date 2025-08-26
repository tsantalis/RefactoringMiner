package gui.webdiff;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.perforce.p4java.Log;
import org.refactoringminer.exceptions.NetworkException;
import gui.webdiff.export.WebExporter;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.MappingExportModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.refactoringminer.astDiff.utils.ExportUtils.getFileNameFromSrcDiff;

/* Created by pourya on 2024-02-09*/
public class DiffDriver {
    @Parameter(names = {"-u", "--url"}, description = "URL of the commit/PR, or the Perforce Server" , order = 0)
    String url;
    @Parameter(names = {"-s", "--src"}, description = "Source directory", order = 1)
    Path src;
    @Parameter(names = {"-d", "--dst"}, description = "Destination directory", order = 2)
    Path dst;
    @Parameter(names = {"-r", "--repo"}, description = "Repository path (locally cloned repo)", order = 3)
    String repo;
    @Parameter(names = {"-c", "--commit"}, description = "Commit ID for locally cloned repo, or Perforce ChangeList number", order = 4)
    String commit;
    @Parameter(names = {"-h", "--help"}, description = "Help", help = true)
    boolean help;
    @Parameter(names = {"-e", "--export"}, description = "Export WebDiff/Mappings/Actions into files")
    boolean export = false;
    @Parameter(names = {"--out", "-o"}, description = "Output dir for the exported files")
    String exportPath = "exported";
    @Parameter(names = {"-pun", "--perforce-user-name"}, description = "Perforce user name")
    String perforceUserName = null;
    @Parameter(names = {"-pup", "--perforce-password"}, description = "Perforce password")
    String perforcePassword = null;
    @Parameter(names = {"--no-browser", "-nb"}, description = "Not open the diff in the browser")
    boolean no_browser = false;

    private static final String HELP_MSG = """
You can run the diff with the following options:
    --url <commit-url>                           \t\t      Run the diff with a GitHub commit url
    --url <pr-url>                               \t\t      Run the diff with a GitHub PullRequest url
    --src <folder1> --dst <folder2>            \t\t              Run the diff with two local directories
    --repo <repo-folder-path> --commit <commitID>  \t\t      Run the diff with a locally cloned GitHub repo

To export the mappings/actions, add --export to the end of the command.
""";

    public ProjectASTDiff getProjectASTDiff() throws Exception {
        RunMode runMode = RunMode.getRunMode(this);
        if (runMode == null) return null;
        ProjectASTDiff projectASTDiff;
        projectASTDiff = runMode.getProjectASTDIFF(this);
        return projectASTDiff;
    }
    public void execute(String[] args) throws Exception {
        JCommander jCommander = JCommander.newBuilder()
                .addObject(this)
                .build();
        jCommander.parse(args);
        help = RunMode.getRunMode(this) == null;
        if (help) {
            System.out.println(HELP_MSG);
//            jCommander.usage();
            return;
        }
        try {
            ProjectASTDiff projectASTDiff = getProjectASTDiff();
            if (export){
                export(projectASTDiff, exportPath);
            }
            else {
                WebDiff webDiff = new WebDiff(projectASTDiff);
                if (no_browser) {
                    webDiff.run();
                }
                else
                    webDiff.openInBrowser();
            }
		}
		catch (NetworkException e) {
			throw e;
		}
		catch (Exception e) {
            if (isConnectionIssue(e))
				throw new NetworkException();
			else
            {
                System.out.println(HELP_MSG);
                throw e;
            }
        }
    }

    private static boolean isConnectionIssue(Throwable e) {
        while (e != null) {
            if (e instanceof java.net.ConnectException ||
                e instanceof java.nio.channels.UnresolvedAddressException ||
                (e.getMessage() != null && (
                        e.getMessage().contains("api.github.com") ||
                        e.getMessage().contains("Failed to connect") ||
                        e.getMessage().contains("Connection refused") ||
                        e.getMessage().contains("Network is unreachable")
                ))) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    public static void export(ProjectASTDiff projectASTDiff, String exportDestination) throws IOException {
        //if export dir doesn't end with a slash, add it
        if (!exportDestination.endsWith(File.separator)) {
            exportDestination += File.separator;
        }
        //Export the webdiff
        WebDiff webDiff = new WebDiff(projectASTDiff);
        webDiff.run();
        WebExporter webExporter = new WebExporter(webDiff);
        webExporter.export(exportDestination);
        webDiff.terminate();

        //Export JSON files containing mappings/actions (unnecessary for now)

        //Make JSON directory
        String jsonPaths = exportDestination + File.separator + "jsons"; //TODO must be variable
        File jsonPathsDir = new File(jsonPaths);
        if (!jsonPathsDir.exists()) {
            jsonPathsDir.mkdirs();
        }

        //Export mappings/actions
        for (ASTDiff astDiff : projectASTDiff.getDiffSet()) {
            String fileNameFromSrcDiff = getFileNameFromSrcDiff(astDiff.getSrcPath());
            int lastIndex = fileNameFromSrcDiff.lastIndexOf(".json");
            if (lastIndex != -1) {
                fileNameFromSrcDiff = fileNameFromSrcDiff.substring(0, lastIndex) + fileNameFromSrcDiff.substring(lastIndex + 5);
            }
            MappingExportModel.exportToFile(new File(jsonPaths, fileNameFromSrcDiff + "_mappings.json"), astDiff.getAllMappings());
            MappingExportModel.exportActions(new File(jsonPaths,fileNameFromSrcDiff + "_actions.txt"), astDiff);
        }
    }
}

