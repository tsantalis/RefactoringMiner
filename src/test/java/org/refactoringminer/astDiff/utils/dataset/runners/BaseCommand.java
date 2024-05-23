package org.refactoringminer.astDiff.utils.dataset.runners;

import com.beust.jcommander.Parameter;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.astDiff.utils.dataset.DiffDataSet;

// Base class for common parameters
abstract class BaseCommand {
    @Parameter(names = {"-d", "--dataset"}, description = "Path to the dataset", converter = DiffDataSet.Converter.class)
    DiffDataSet diffDataSet  = DiffDataSet.Misc;

    @Parameter(names = {"-u", "--url"}, description = "URL of the repository")
    String url;

    @Parameter(names = {"-r", "--repo"}, description = "Repository name")
    String repo;

    @Parameter(names = {"-c", "--commit"}, description = "Commit ID")
    String commit;

    @Parameter(names = {"-p", "--problematic"},
            description = "Mark the added files as problematic",
            required = false)
    boolean problematic = false;

    @Parameter(names = {"-h", "--help"}, description =  "Display help information", help = true)
    boolean help;
    abstract void postValidationExecution();
    public void execute(){
        validate();
        printArgs();
        postValidationExecution();
    }

    private void printArgs() {
        System.out.println("Arguments:");
        System.out.println("Dataset: " + diffDataSet);
        if (url != null) {
            System.out.println("URL: " + url);
            repo = URLHelper.getRepo(url);
            commit = URLHelper.getCommit(url);
        }
        System.out.println("Repository: " + repo);
        System.out.println("Commit: " + commit);
    }

    private void validate() {
        if (repo == null || commit == null)
            if (url == null)
                throw new IllegalArgumentException("URL/ Repo&Commit  is required");

    }
}

