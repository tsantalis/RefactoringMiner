package gui.webdiff.dir;

import com.github.gumtreediff.utils.Pair;
import gui.webdiff.dir.filters.DiffFilterer;
import gui.webdiff.tree.TreeViewGenerator;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHPullRequestReviewComment.Side;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryWrapper;
import org.kohsuke.github.PagedIterable;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.tree.DefaultMutableTreeNode;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DirComparator {
    private final static Logger logger = LoggerFactory.getLogger(DirComparator.class);
    private static final String GITHUB_URL = "https://github.com/";
    private List<ASTDiff> diffs;
    private final ProjectASTDiff projectASTDiff;
    private final DefaultMutableTreeNode compressedTree;
    private final List<Pair<String,String>> modifiedFilesName;
    private Set<String> removedFilesName;
    private Set<String> addedFilesName;

    public List<Refactoring> getRefactorings() {
        return projectASTDiff.getRefactorings();
    }

    public List<ASTDiff> getDiffs() {
        return diffs;
    }

    public ProjectASTDiff getProjectASTDiff() {
		return projectASTDiff;
	}

	public int getNumOfDiffs(){
        return diffs.size();
    }

    public DefaultMutableTreeNode getCompressedTree() {
        return compressedTree;
    }

    public Set<String> getRemovedFilesName() {
        return removedFilesName;
    }

    public Set<String> getAddedFilesName() {
        return addedFilesName;
    }

    public List<Pair<String,String>> getModifiedFilesName() {
        return modifiedFilesName;
    }

    public Pair<String,String> getFileContentsPair(int id)
    {
        return new Pair<>(
                projectASTDiff.getFileContentsBefore().get(diffs.get(id).getSrcPath()),
                projectASTDiff.getFileContentsAfter().get(diffs.get(id).getDstPath())
        );
    }

    public int getId(String srcFileName, String dstFileName) {
        int count = 0;
        for (ASTDiff diff : diffs) {
            if(diff.getSrcPath().equals(srcFileName) && diff.getDstPath().equals(dstFileName)) {
                return count;
            }
            count++;
        }
        return -1;
    }

    public DirComparator(ProjectASTDiff projectASTDiff, DiffFilterer diffFilteringOptions)
    {
        this.projectASTDiff = projectASTDiff;
        this.diffs = new ArrayList<>(projectASTDiff.getDiffSet());
        this.diffs.addAll(projectASTDiff.getMoveDiffSet());
        this.diffs = diffFilteringOptions.filter(diffs);

        modifiedFilesName = new ArrayList<>();
        compare();
        TreeViewGenerator treeViewGenerator = new TreeViewGenerator(getModifiedFilesName(), diffs);
        compressedTree = treeViewGenerator.getCompressedTree();
        this.diffs = treeViewGenerator.getOrderedDiffs();
        // it takes some time to fetch all comments
        this.projectASTDiff.getMetaInfo().setComments(fetchPullRequestComments());
    }

    private Map<String, List<PullRequestReviewComment>> fetchPullRequestComments() {
        Map<String, List<PullRequestReviewComment>> commentMap = new ConcurrentHashMap<>();
        DiffMetaInfo info = projectASTDiff.getMetaInfo();

        try {
            String cloneURL = resolveGitHubCloneURL(info);
            if (cloneURL == null) {
                return commentMap;
            }
            GHRepository repository = new GitHistoryRefactoringMinerImpl().getGitHubRepository(cloneURL);
            for (GHPullRequest pullRequest : resolvePullRequestsForComments(repository, info)) {
                appendPullRequestComments(commentMap, repository, pullRequest);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // the URL does not contain a valid pull request identifier
        }
        return commentMap;
    }

    private String resolveGitHubCloneURL(DiffMetaInfo info) {
        if (info.getCloneURL() != null && info.getCloneURL().startsWith(GITHUB_URL)) {
            return info.getCloneURL();
        }
        if (info.hasUrl() && info.getUrl().startsWith(GITHUB_URL)) {
            return URLHelper.getRepo(info.getUrl());
        }
        return null;
    }

    private List<GHPullRequest> resolvePullRequestsForComments(GHRepository repository, DiffMetaInfo info) throws IOException {
        if (info.hasUrl() && URLHelper.hasPullRequestContext(info.getUrl())) {
            return Collections.singletonList(repository.getPullRequest(URLHelper.getPullRequestID(info.getUrl())));
        }
        if (info.getCommitId() == null || info.getCommitId().isEmpty()) {
            return Collections.emptyList();
        }
        GHCommit commit = repository.getCommit(info.getCommitId());
        Map<Integer, GHPullRequest> pullRequestsByNumber = new LinkedHashMap<>();
        for (GHPullRequest pullRequest : commit.listPullRequests()) {
            pullRequestsByNumber.putIfAbsent(pullRequest.getNumber(), pullRequest);
        }
        return new ArrayList<>(pullRequestsByNumber.values());
    }

    private void appendPullRequestComments(Map<String, List<PullRequestReviewComment>> commentMap,
                                           GHRepository repository,
                                           GHPullRequest pullRequest) throws IOException, InterruptedException {
        PagedIterable<GHPullRequestReview> reviews = pullRequest.listReviews();
        List<GHPullRequestReview> reviewList = reviews.toList();
        ExecutorService pool = Executors.newFixedThreadPool(reviewList.isEmpty() ? 1 : reviewList.size());
        for (GHPullRequestReview review : reviewList) {
            Runnable r = () -> {
                try {
                    PagedIterable<GHPullRequestReviewComment> comments = review.listReviewComments();
                    for (GHPullRequestReviewComment comment : comments) {
                        URL url = comment.getUrl();
                        logger.info("Processing PR Review Comment: " + url);
                        String path = comment.getPath();
                        org.apache.commons.lang3.tuple.Pair<Side, Integer> pair = new GHRepositoryWrapper(repository).getGhPullRequestReviewCommentLine(url.toString());
                        Side side = pair.getLeft();
                        int lineNumber = pair.getRight();
                        if (lineNumber != 0) {
                            PullRequestReviewComment prComment = new PullRequestReviewComment(
                                    comment.getUser().getLogin(),
                                    comment.getBody(),
                                    comment.getCreatedAt(),
                                    lineNumber,
                                    comment.getUser().getAvatarUrl(),
                                    side
                            );
                            commentMap.computeIfAbsent(path, k -> Collections.synchronizedList(new ArrayList<>())).add(prComment);
                        }
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            };
            pool.submit(r);
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private void compare() {
        Set<String> beforeFiles = projectASTDiff.getFileContentsBefore().keySet();
        Set<String> afterFiles = projectASTDiff.getFileContentsAfter().keySet();

        removedFilesName = new LinkedHashSet<>(beforeFiles);
        addedFilesName = new LinkedHashSet<>(afterFiles);

        for (ASTDiff diff : diffs) {
            modifiedFilesName.add(new Pair<>(diff.getSrcPath(),diff.getDstPath()));
            removedFilesName.remove(diff.getSrcPath());
            addedFilesName.remove(diff.getDstPath());
        }
        Set<String> removedBackup = new LinkedHashSet<>(removedFilesName);
        removedFilesName.removeAll(addedFilesName);
        addedFilesName.removeAll(removedBackup);
    }

    public boolean isMoveDiff(int id) {
    	ASTDiff diff = getASTDiff(id);
    	return projectASTDiff.getMoveDiffSet().contains(diff);
    }

    public ASTDiff getASTDiff(int id) {
        return diffs.get(id);
    }
}
