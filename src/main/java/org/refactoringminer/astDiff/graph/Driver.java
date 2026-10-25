package org.refactoringminer.astDiff.graph;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.kohsuke.github.GHRepository;
import gr.uom.java.xmi.RepositoryStateFiles;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class Driver {

    // A folder keeping what a graph reads from GitHub, the files of the changes and the unchanged files they use, so
    // that a revision read once is read again with no GitHub access
    public static final String CACHE_PROPERTY = "narrator.cache";

    public static Graph<Node, Edge> getPullRequestGraph(String url) throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();

        String repo = URLHelper.getRepo(url);
        int PR = Integer.parseInt(URLHelper.getPRID(url));

        GHRepository repository = miner.getGitHubRepository(repo);
        RepositoryStateFiles PRFiles = new RepositoryStateFiles(repository, repository.getPullRequest(PR).getHead().getSha());

        ProjectASTDiff projectASTDiff = miner.diffAtPullRequest(repo, PR, 1000);
        return getGraph(projectASTDiff, PRFiles);
    }

    public static Graph<Node, Edge> getCommitGraph(String url) throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();

        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        File cacheFolder = cacheFolder();
        RepositoryStateFiles commitFiles = new RepositoryStateFiles(() -> miner.getGitHubRepository(repo), repoName(repo),
                commit, cacheFolder);

        ProjectASTDiff projectASTDiff = cacheFolder == null ? miner.diffAtCommit(repo, commit, 1000)
                : miner.diffAtCommitWithGitHubAPI(repo, commit, cacheFolder);
        return getGraph(projectASTDiff, commitFiles);
    }

    public static Graph<Node, Edge> getCompareGraph(String url) throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();

        String repo = URLHelper.getRepo(url);
        Pair<String, String> range = URLHelper.getCommitPairFromGitHubCompareURL(url);

        File cacheFolder = cacheFolder();
        RepositoryStateFiles rangeFiles = new RepositoryStateFiles(() -> miner.getGitHubRepository(repo), repoName(repo),
                range.getRight(), cacheFolder);

        ProjectASTDiff projectASTDiff = miner.diffAtGitHubCompare(repo, range.getLeft(), range.getRight(), cacheFolder);
        return getGraph(projectASTDiff, rangeFiles);
    }

    private static File cacheFolder() {
        String cache = System.getProperty(CACHE_PROPERTY);
        return cache == null ? null : new File(cache);
    }

    private static String repoName(String repo) {
        return repo.substring(repo.lastIndexOf('/') + 1, repo.lastIndexOf('.'));
    }

    private static Graph<Node, Edge> getGraph(ProjectASTDiff projectASTDiff, RepositoryStateFiles repositoryFiles) {
        HunkNetwork network = new HunkNetwork(projectASTDiff.getModelDiff(), projectASTDiff.getFileContentsBefore(),
                projectASTDiff.getFileContentsAfter(), repositoryFiles);

        Set<ASTDiff> authoritativeDiffs = new LinkedHashSet<>(projectASTDiff.getDiffSet());
        Set<ASTDiff> diffSet = new LinkedHashSet<>(authoritativeDiffs);
        diffSet.addAll(projectASTDiff.getMoveDiffSet());
        for (ASTDiff diff : diffSet) {
            network.importDiff(diff, authoritativeDiffs.contains(diff));
        }

        network.importFiles();

        network.process();

        return network.getGraph();
    }
}