package gui.webdiff.dir;

import com.github.gumtreediff.utils.Pair;
import gui.webdiff.tree.TreeViewGenerator;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterable;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import javax.swing.tree.DefaultMutableTreeNode;

import java.io.IOException;
import java.util.*;

public class DirComparator {
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

    public DirComparator(ProjectASTDiff projectASTDiff)
    {
        this.projectASTDiff = projectASTDiff;
        this.diffs = new ArrayList<>(projectASTDiff.getDiffSet());
        this.diffs.addAll(projectASTDiff.getMoveDiffSet());
        modifiedFilesName = new ArrayList<>();
        compare();
        TreeViewGenerator treeViewGenerator = new TreeViewGenerator(getModifiedFilesName(), diffs);
        compressedTree = treeViewGenerator.getCompressedTree();
        this.diffs = treeViewGenerator.getOrderedDiffs();
        // it takes some time to fetch all comments
        //Map<ImmutablePair<String, Integer>, List<PullRequestReviewComment>> commentMap = pullRequestComments();
    }

    private Map<ImmutablePair<String, Integer>, List<PullRequestReviewComment>> pullRequestComments() {
    	Map<ImmutablePair<String, Integer>, List<PullRequestReviewComment>> commentMap = new LinkedHashMap<>();
    	DiffMetaInfo info = projectASTDiff.getMetaInfo();
    	if(info.getUrl().isEmpty()) {
    		return commentMap;
    	}
		try {
			String cloneURL = URLHelper.getRepo(info.getUrl());
	        int pullRequestId = URLHelper.getPullRequestID(info.getUrl());
			GHRepository repository = new GitHistoryRefactoringMinerImpl().getGitHubRepository(cloneURL);
			GHPullRequest pullRequest = repository.getPullRequest(pullRequestId);
	    	PagedIterable<GHPullRequestReview> reviews = pullRequest.listReviews();
			for(GHPullRequestReview review : reviews) {
				PagedIterable<GHPullRequestReviewComment> comments = review.listReviewComments();
				for(GHPullRequestReviewComment comment : comments) {
					ImmutablePair<String, Integer> pair = ImmutablePair.of(comment.getPath(), comment.getOriginalPosition());
					if(commentMap.containsKey(pair)) {
						PullRequestReviewComment prComment = new PullRequestReviewComment(comment.getUser().getLogin(), comment.getBody(), comment.getCreatedAt());
						commentMap.get(pair).add(prComment);
					}
					else {
						List<PullRequestReviewComment> prComments = new ArrayList<>();
						PullRequestReviewComment prComment = new PullRequestReviewComment(comment.getUser().getLogin(), comment.getBody(), comment.getCreatedAt());
						prComments.add(prComment);
						commentMap.put(pair, prComments);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch(NumberFormatException e) {
			//the URL does not correspond to a PR URL
		}
		return commentMap;
    }

    private void compare() {
        Set<String> beforeFiles = projectASTDiff.getFileContentsBefore().keySet();
        Set<String> afterFiles = projectASTDiff.getFileContentsAfter().keySet();

        removedFilesName = new HashSet<>(beforeFiles);
        addedFilesName = new HashSet<>(afterFiles);

        for (ASTDiff diff : diffs) {
            modifiedFilesName.add(new Pair<>(diff.getSrcPath(),diff.getDstPath()));
            removedFilesName.remove(diff.getSrcPath());
            addedFilesName.remove(diff.getDstPath());
        }
        Set<String> removedBackup = new HashSet<>(removedFilesName);
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