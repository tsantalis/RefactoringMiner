package gui;

import gui.webdiff.WebDiff;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public class RunWithLocallyClonedRepository {
    public static void main(String[] args) throws Exception {
        String url = "https://github.com/danilofes/refactoring-toy-example/commit/36287f7c3b09eff78395267a3ac0d7da067863fd";
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        GitService gitService = new GitServiceImpl();
        String projectName = repo.substring(repo.lastIndexOf("/") + 1, repo.length() - 4);
        String pathToClonedRepository = System.getProperty("user.dir") + "/tmp/" + projectName;
        Repository repository = gitService.cloneIfNotExists(pathToClonedRepository, repo);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repository, commit);
        new WebDiff(projectASTDiff).openInBrowser();
    }
}
