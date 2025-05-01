package gui.webdiff;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
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

    public ProjectASTDiff getProjectASTDiff() {
        RunMode runMode = RunMode.getRunMode(this);
        if (runMode == null) return null;
        ProjectASTDiff projectASTDiff;
        try {
            projectASTDiff = runMode.getProjectASTDIFF(this);
            projectASTDiff.setMetaInfo(runMode.getDiffMetaInfo(this));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return projectASTDiff;
    }
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
        } catch (Exception e) {
	        System.out.println(HELP_MSG);
            throw new RuntimeException(e);
        }
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

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSrc(Path src) {
        this.src = src;
    }

    public void setDst(Path dst) {
        this.dst = dst;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public void setPerforceUserName(String perforceUserName) {
        this.perforceUserName = perforceUserName;
    }

    public void setPerforcePassword(String perforcePassword) {
        this.perforcePassword = perforcePassword;
    }

    public void setNo_browser(boolean no_browser) {
        this.no_browser = no_browser;
    }
}

