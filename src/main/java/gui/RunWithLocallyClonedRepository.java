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
        String url = "https://github.com/JetBrains/intellij-community/commit/7ed3f273ab0caf0337c22f0b721d51829bb0c877";
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        GitService gitService = new GitServiceImpl();
        String projectName = repo.substring(repo.lastIndexOf("/") + 1, repo.length() - 4);
        String pathToClonedRepository = "tmp/" + projectName;
        Repository repository = gitService.cloneIfNotExists(pathToClonedRepository, repo);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repository, commit);
        new WebDiff(projectASTDiff).run();
    }
}
