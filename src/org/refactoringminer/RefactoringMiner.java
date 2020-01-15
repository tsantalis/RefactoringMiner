package org.refactoringminer;

import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public class RefactoringMiner {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw argumentException();
		}

		final String option = args[0];
		if (option.equalsIgnoreCase("-h") || option.equalsIgnoreCase("--h") || option.equalsIgnoreCase("-help")
				|| option.equalsIgnoreCase("--help")) {
			printTips();
			return;
		}

		if (option.equalsIgnoreCase("-a")) {
			detectAll(args);
		} else if (option.equalsIgnoreCase("-bc")) {
			detectBetweenCommits(args);
		} else if (option.equalsIgnoreCase("-bt")) {
			detectBetweenTags(args);
		} else if (option.equalsIgnoreCase("-c")) {
			detectAtCommit(args);
		} else if (option.equalsIgnoreCase("-gc")) {
			detectAtGitHubCommit(args);
		} else if (option.equalsIgnoreCase("-gp")) {
			detectAtGitHubPullRequest(args);
		} else {
			throw argumentException();
		}
	}

	private static void detectAll(String[] args) throws Exception {
		if (args.length > 3) {
			throw argumentException();
		}
		String folder = args[1];
		String branch = null;
		if (args.length == 3) {
			branch = args[2];
		}
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			String gitURL = repo.getConfig().getString("remote", "origin", "url");
			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			StringBuilder sb = new StringBuilder();
			startJSON(sb);
			detector.detectAll(repo, branch, new RefactoringHandler() {
				private int commitCount = 0;
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if(commitCount > 0) {
						sb.append(",").append("\n");
					}
					commitJSON(sb, gitURL, commitId, refactorings);
					commitCount++;
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					//System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
					//		commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
			endJSON(sb);
			System.out.println(sb.toString());
		}
	}

	private static void detectBetweenCommits(String[] args) throws Exception {
		if (!(args.length == 3 || args.length == 4)) {
			throw argumentException();
		}
		String folder = args[1];
		String startCommit = args[2];
		String endCommit = (args.length == 4) ? args[3] : null;
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			String gitURL = repo.getConfig().getString("remote", "origin", "url");
			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			StringBuilder sb = new StringBuilder();
			startJSON(sb);
			detector.detectBetweenCommits(repo, startCommit, endCommit, new RefactoringHandler() {
				private int commitCount = 0;
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if(commitCount > 0) {
						sb.append(",").append("\n");
					}
					commitJSON(sb, gitURL, commitId, refactorings);
					commitCount++;
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					//System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
					//		commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
			endJSON(sb);
			System.out.println(sb.toString());
		}
	}

	private static void detectBetweenTags(String[] args) throws Exception {
		if (!(args.length == 3 || args.length == 4)) {
			throw argumentException();
		}
		String folder = args[1];
		String startTag = args[2];
		String endTag = (args.length == 4) ? args[3] : null;
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			String gitURL = repo.getConfig().getString("remote", "origin", "url");
			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			StringBuilder sb = new StringBuilder();
			startJSON(sb);
			detector.detectBetweenTags(repo, startTag, endTag, new RefactoringHandler() {
				private int commitCount = 0;
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if(commitCount > 0) {
						sb.append(",").append("\n");
					}
					commitJSON(sb, gitURL, commitId, refactorings);
					commitCount++;
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					//System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
					//		commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
			endJSON(sb);
			System.out.println(sb.toString());
		}
	}

	private static void detectAtCommit(String[] args) throws Exception {
		if (args.length != 3) {
			throw argumentException();
		}
		String folder = args[1];
		String commitId = args[2];
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			String gitURL = repo.getConfig().getString("remote", "origin", "url");
			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			StringBuilder sb = new StringBuilder();
			startJSON(sb);
			detector.detectAtCommit(repo, commitId, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					commitJSON(sb, gitURL, commitId, refactorings);
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
			endJSON(sb);
			System.out.println(sb.toString());
		}
	}

	private static void detectAtGitHubCommit(String[] args) throws Exception {
		if (args.length != 4) {
			throw argumentException();
		}
		String gitURL = args[1];
		String commitId = args[2];
		int timeout = Integer.parseInt(args[3]);
		GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
		StringBuilder sb = new StringBuilder();
		startJSON(sb);
		detector.detectAtCommit(gitURL, commitId, new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				commitJSON(sb, gitURL, commitId, refactorings);
			}

			@Override
			public void handleException(String commit, Exception e) {
				System.err.println("Error processing commit " + commit);
				e.printStackTrace(System.err);
			}
		}, timeout);
		endJSON(sb);
		System.out.println(sb.toString());
	}

	private static void detectAtGitHubPullRequest(String[] args) throws Exception {
		if (args.length != 4) {
			throw argumentException();
		}
		String gitURL = args[1];
		int pullId = Integer.parseInt(args[2]);
		int timeout = Integer.parseInt(args[3]);
		GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
		StringBuilder sb = new StringBuilder();
		startJSON(sb);
		detector.detectAtPullRequest(gitURL, pullId, new RefactoringHandler() {
			private int commitCount = 0;
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				if(commitCount > 0) {
					sb.append(",").append("\n");
				}
				commitJSON(sb, gitURL, commitId, refactorings);
				commitCount++;
			}

			@Override
			public void handleException(String commit, Exception e) {
				System.err.println("Error processing commit " + commit);
				e.printStackTrace(System.err);
			}
		}, timeout);
		endJSON(sb);
		System.out.println(sb.toString());
	}

	private static void commitJSON(StringBuilder sb, String cloneURL, String currentCommitId, List<Refactoring> refactoringsAtRevision) {
		sb.append("{").append("\n");
		sb.append("\t").append("\"").append("repository").append("\"").append(": ").append("\"").append(cloneURL).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("sha1").append("\"").append(": ").append("\"").append(currentCommitId).append("\"").append(",").append("\n");
		String url = "https://github.com/" + cloneURL.substring(19, cloneURL.indexOf(".git")) + "/commit/" + currentCommitId;
		sb.append("\t").append("\"").append("url").append("\"").append(": ").append("\"").append(url).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("refactorings").append("\"").append(": ");
		sb.append("[");
		int counter = 0;
		for(Refactoring refactoring : refactoringsAtRevision) {
			sb.append(refactoring.toJSON());
			if(counter < refactoringsAtRevision.size()-1) {
				sb.append(",");
			}
			sb.append("\n");
			counter++;
		}
		sb.append("]").append("\n");
		sb.append("}");
	}

	private static void startJSON(StringBuilder sb) {
		sb.append("{").append("\n");
		sb.append("\"").append("commits").append("\"").append(": ");
		sb.append("[").append("\n");
	}

	private static void endJSON(StringBuilder sb) {
		sb.append("]").append("\n");
		sb.append("}");
	}

	private static void printTips() {
		System.out.println("-h\t\t\t\t\t\t\t\tShow tips");
		System.out.println(
				"-a <git-repo-folder> <branch>\t\t\t\t\tDetect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.");
		System.out.println(
				"-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1>\tDetect refactorings Between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"-bt <git-repo-folder> <start-tag> <end-tag>\t\t\tDetect refactorings Between <start-tag> and <end-tag> for project <git-repo-folder>");
		System.out.println(
				"-c <git-repo-folder> <commit-sha1>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"-gc <git-URL> <commit-sha1> <timeout>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-URL> within the given <timeout> in seconds. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties");
		System.out.println(
				"-gp <git-URL> <pull-request> <timeout>\t\t\t\tDetect refactorings at specified pull request <pull-request> for project <git-URL> within the given <timeout> in seconds for each commit in the pull request. All required information is obtained directly from GitHub using the OAuth token in github-oauth.properties");
	}

	private static IllegalArgumentException argumentException() {
		return new IllegalArgumentException("Type `RefactoringMiner -h` to show usage.");
	}
}
