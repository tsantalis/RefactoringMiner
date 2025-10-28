package org.refactoringminer.test;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.util.GitServiceImpl;

public class TestBuilder {

	private final String tempDir;
	private final Map<String, ProjectMatcher> map;
	private final GitHistoryRefactoringMiner refactoringDetector;
	private boolean verbose;
	private boolean aggregate;
	private int commitsCount;
	private int errorCommitsCount;
	private Counter c;// = new Counter();
	private Map<RefactoringType, Counter> cMap;
	private static final int TP = 0;
	private static final int FP = 1;
	private static final int FN = 2;
	private static final int TN = 3;
	private static final int UNK = 4;

	private BigInteger refactoringFilter;

	public TestBuilder(GitHistoryRefactoringMiner detector, String tempDir) {
		this.map = new HashMap<String, ProjectMatcher>();
		this.refactoringDetector = detector;
		this.tempDir = tempDir;
		this.verbose = false;
		this.aggregate = false;
	}

	public TestBuilder(GitHistoryRefactoringMiner detector, String tempDir, BigInteger refactorings) {
		this(detector, tempDir);

		this.refactoringFilter = refactorings;
	}

	public TestBuilder verbose() {
		this.verbose = true;
		return this;
	}

	public TestBuilder withAggregation() {
		this.aggregate = true;
		return this;
	}

	private static class Counter {
		int[] c = new int[5];
	}

	private void count(int type, String refactoring) {
		c.c[type]++;
		RefactoringType refType = RefactoringType.extractFromDescription(refactoring);
		Counter refTypeCounter = cMap.get(refType);
		if (refTypeCounter == null) {
			refTypeCounter = new Counter();
			cMap.put(refType, refTypeCounter);
		}
		refTypeCounter.c[type]++;
	}

	private int get(int type) {
		return c.c[type];
	}

	private int get(int type, Counter counter) {
		return counter.c[type];
	}

	public TestBuilder() {
		this(new GitHistoryRefactoringMinerImpl(), "tmp");
	}

	public final ProjectMatcher project(String cloneUrl, String branch) {
		ProjectMatcher projectMatcher = this.map.get(cloneUrl);
		if (projectMatcher == null) {
			projectMatcher = new ProjectMatcher(cloneUrl, branch);
			this.map.put(cloneUrl, projectMatcher);
		}
		return projectMatcher;
	}

	public void assertExpectationsWithGitHubAPI(int expectedTPs, int expectedFPs, int expectedFNs) throws Exception {
		c = new Counter();
		cMap = new HashMap<RefactoringType, Counter>();
		commitsCount = 0;
		errorCommitsCount = 0;
		ExecutorService pool = Executors.newWorkStealingPool();
		File rootFolder = new File(tempDir);
		for (ProjectMatcher m : map.values()) {
			if (m.ignoreNonSpecifiedCommits) {
				for (String commitId : m.getCommits()) {
					Runnable r = () -> ((GitHistoryRefactoringMinerImpl)refactoringDetector).detectAtCommitWithGitHubAPI(m.cloneUrl, commitId, rootFolder, m);
					pool.submit(r);
				}
			}
		}
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		System.out.println(String.format("Commits: %d  Errors: %d", commitsCount, errorCommitsCount));

		String mainResultMessage = buildResultMessage(c);
		System.out.println("Total  " + mainResultMessage);
		for (RefactoringType refType : RefactoringType.values()) {
			Counter refTypeCounter = cMap.get(refType);
			if (refTypeCounter != null) {
				System.out
						.println(String.format("%-7s", refType.getAbbreviation()) + buildResultMessage(refTypeCounter));
			}
		}

		boolean success = get(FP) == expectedFPs && get(FN) == expectedFNs && get(TP) == expectedTPs;
		if (!success || verbose) {
			for (ProjectMatcher m : map.values()) {
				m.printResults();
			}
		}
		System.out.println(buildMarkup());
		Assertions.assertTrue(success, mainResultMessage);
	}

	public void assertExpectations(int expectedTPs, int expectedFPs, int expectedFNs) throws Exception {
		c = new Counter();
		cMap = new HashMap<RefactoringType, Counter>();
		commitsCount = 0;
		errorCommitsCount = 0;
		GitService gitService = new GitServiceImpl();
		ExecutorService pool = Executors.newWorkStealingPool();
		for (ProjectMatcher m : map.values()) {
			String folder = tempDir + "/"
					+ m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
			try (Repository rep = gitService.cloneIfNotExists(folder,
					m.cloneUrl/* , m.branch */)) {
				if (m.ignoreNonSpecifiedCommits) {
					// It is faster to only look at particular commits
					for (String commitId : m.getCommits()) {
						Runnable r = () -> refactoringDetector.detectAtCommit(rep, commitId, m);
						pool.submit(r);
					}
				} else {
					// Iterate over each commit
					refactoringDetector.detectAll(rep, m.branch, m);
				}
			}
		}
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		System.out.println(String.format("Commits: %d  Errors: %d", commitsCount, errorCommitsCount));

		String mainResultMessage = buildResultMessage(c);
		System.out.println("Total  " + mainResultMessage);
		for (RefactoringType refType : RefactoringType.values()) {
			Counter refTypeCounter = cMap.get(refType);
			if (refTypeCounter != null) {
				System.out
						.println(String.format("%-7s", refType.getAbbreviation()) + buildResultMessage(refTypeCounter));
			}
		}

		boolean success = get(FP) == expectedFPs && get(FN) == expectedFNs && get(TP) == expectedTPs;
		if (!success || verbose) {
			for (ProjectMatcher m : map.values()) {
				m.printResults();
			}
		}
		System.out.println(buildMarkup());
		Assertions.assertTrue(success, mainResultMessage);
	}

	private String buildMarkup() {
		StringBuilder sb = new StringBuilder();
		sb.append("| Refactoring Type | TP | FP | FN | Precision | Recall |").append("\n");
		sb.append("|:-----------------------|-----------:|--------:|--------:|--------:|--------:|").append("\n");
		for (RefactoringType refType : RefactoringType.values()) {
			Counter refTypeCounter = cMap.get(refType);
			if (refTypeCounter != null) {
				sb.append("|" + refType.getDisplayName() + buildResultMessageMarkup(refTypeCounter)).append("\n");
			}
		}
		sb.append("|Total" + buildResultMessageMarkup(c));
		return sb.toString();
	}

	private String buildResultMessageMarkup(Counter c) {
		double precision = ((double) get(TP, c) / (get(TP, c) + get(FP, c)));
		double recall = ((double) get(TP, c)) / (get(TP, c) + get(FN, c));
		String mainResultMessage = String.format(
				"|%2d  | %2d  | %2d  | %.3f  | %.3f|", get(TP, c), get(FP, c),
				get(FN, c), precision, recall);
		return mainResultMessage;
	}

	private String buildResultMessage(Counter c) {
		double precision = ((double) get(TP, c) / (get(TP, c) + get(FP, c)));
		double recall = ((double) get(TP, c)) / (get(TP, c) + get(FN, c));
		String mainResultMessage = String.format(
				"TP: %2d  FP: %2d  FN: %2d  TN: %2d  Unk.: %2d  Prec.: %.3f  Recall: %.3f", get(TP, c), get(FP, c),
				get(FN, c), get(TN, c), get(UNK, c), precision, recall);
		return mainResultMessage;
	}

	private List<String> normalize(String refactoring) {
		RefactoringType refType = RefactoringType.extractFromDescription(refactoring);
		refactoring = normalizeSingle(refactoring);
		if (aggregate) {
			refactoring = refType.aggregate(refactoring);
		} else {
			int begin = refactoring.indexOf("from classes [");
			if (begin != -1) {
				int end = refactoring.lastIndexOf(']');
				String types = refactoring.substring(begin + "from classes [".length(), end);
				String[] typesArray = types.split(", ");
				List<String> refactorings = new ArrayList<String>();
				for (String type : typesArray) {
					refactorings.add(refactoring.substring(0, begin) + "from class " + type);
				}
				return refactorings;
			}
		}
		return Collections.singletonList(refactoring);
	}

	/**
	 * Remove generics type information.
	 */
	private static String normalizeSingle(String refactoring) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < refactoring.length(); i++) {
			char c = refactoring.charAt(i);
			if (c == '\t') {
				c = ' ';
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public class ProjectMatcher implements RefactoringHandler {

		private final String cloneUrl;
		private final String branch;
		private Map<String, CommitMatcher> expected = new HashMap<>();
		private boolean ignoreNonSpecifiedCommits = true;

		private ProjectMatcher(String cloneUrl, String branch) {
			this.cloneUrl = cloneUrl;
			this.branch = branch;
		}

		public CommitMatcher atCommit(String commitId) {
			CommitMatcher m = expected.get(commitId);
			if (m == null) {
				m = new CommitMatcher();
				expected.put(commitId, m);
			}
			return m;
		}

		public Set<String> getCommits() {
			return expected.keySet();
		}

		@Override
		public boolean skipCommit(String commitId) {
			if (this.ignoreNonSpecifiedCommits) {
				return !this.expected.containsKey(commitId);
			}
			return false;
		}

		@Override
		public void handle(String commitId, List<Refactoring> refactorings) {
			refactorings= filterRefactoring(refactorings);
			CommitMatcher matcher;
			commitsCount++;
			if (expected.containsKey(commitId)) {
				matcher = expected.get(commitId);
			} else if (!this.ignoreNonSpecifiedCommits) {
				matcher = this.atCommit(commitId);
				matcher.containsOnly();
			} else {
				// ignore this commit
				matcher = null;
			}
			if (matcher != null) {
				matcher.analyzed = true;
				Set<String> refactoringsFound = new HashSet<String>();
				for (Refactoring refactoring : refactorings) {
					refactoringsFound.addAll(normalize(refactoring.toString()));
				}
				// count true positives
				for (Iterator<String> iter = matcher.expected.iterator(); iter.hasNext();) {
					String expectedRefactoring = iter.next();
					if (refactoringsFound.contains(expectedRefactoring)) {
						iter.remove();
						refactoringsFound.remove(expectedRefactoring);
						count(TP, expectedRefactoring);
						matcher.truePositive.add(expectedRefactoring);
					}
					else if(countAsFN(expectedRefactoring)) {
						count(FN, expectedRefactoring);
					}
				}

				// count false positives
				for (Iterator<String> iter = matcher.notExpected.iterator(); iter.hasNext();) {
					String notExpectedRefactoring = iter.next();
					if (refactoringsFound.contains(notExpectedRefactoring)) {
						refactoringsFound.remove(notExpectedRefactoring);
						count(FP, notExpectedRefactoring);
					} else {
						count(TN, notExpectedRefactoring);
						iter.remove();
					}
				}
				// count false positives when using containsOnly
				if (matcher.ignoreNonSpecified) {
					for (String refactoring : refactoringsFound) {
						matcher.unknown.add(refactoring);
						count(UNK, refactoring);
					}
				} else {
					for (String refactoring : refactoringsFound) {
						matcher.notExpected.add(refactoring);
						count(FP, refactoring);
					}
				}
			}
		}

		private boolean countAsFN(String expectedRefactoring) {
			RefactoringType refType = RefactoringType.extractFromDescription(expectedRefactoring);
			BigInteger value = Enum.valueOf(Refactorings.class, refType.getDisplayName().replace(" ", "")).getValue();
			if (value.and(refactoringFilter).compareTo(BigInteger.ZERO) == 1) {
				return true;
			}
			return false;
		}

		private List<Refactoring> filterRefactoring(List<Refactoring> refactorings) {
			List<Refactoring> filteredRefactorings = new ArrayList<>();

			for (Refactoring refactoring : refactorings) {
				BigInteger value = Enum.valueOf(Refactorings.class, refactoring.getName().replace(" ", "")).getValue();
				if (value.and(refactoringFilter).compareTo(BigInteger.ZERO) == 1) {
					filteredRefactorings.add(refactoring);
				}
			}
			
			return filteredRefactorings;
		}

		@Override
		public void handleException(String commitId, Exception e) {
			if (expected.containsKey(commitId)) {
				CommitMatcher matcher = expected.get(commitId);
				matcher.error = e.toString();
			}
			errorCommitsCount++;
		}

		private void printResults() {
			String baseUrl = this.cloneUrl.substring(0, this.cloneUrl.length() - 4) + "/commit/";
			for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
				String commitUrl = baseUrl + entry.getKey();
				CommitMatcher matcher = entry.getValue();
				if (matcher.error != null) {
					System.out.println("error at " + commitUrl + ": " + matcher.error);
				} else {
					if (verbose || !matcher.expected.isEmpty() || !matcher.notExpected.isEmpty()
							|| !matcher.unknown.isEmpty()) {
						if (!matcher.analyzed) {
							System.out.println("at not analyzed " + commitUrl);
						} else {
							System.out.println("at " + commitUrl);
						}
					}
					if (verbose && !matcher.truePositive.isEmpty()) {
						System.out.println(" true positives");
						for (String ref : matcher.truePositive) {
							System.out.println("  " + ref);
						}
					}
					if (!matcher.notExpected.isEmpty()) {
						System.out.println(" false positives");
						for (String ref : matcher.notExpected) {
							System.out.println("  " + ref);
						}
					}
					if (!matcher.expected.isEmpty()) {
						System.out.println(" false negatives");
						for (String ref : matcher.expected) {
							System.out.println("  " + ref);
						}
					}
					if (!matcher.unknown.isEmpty()) {
						System.out.println(" unknown");
						for (String ref : matcher.unknown) {
							System.out.println("  " + ref);
						}
					}
				}
			}
		}

		public class CommitMatcher {
			private Set<String> expected = new HashSet<String>();
			private Set<String> notExpected = new HashSet<String>();
			private Set<String> truePositive = new HashSet<String>();
			private Set<String> unknown = new HashSet<String>();
			private boolean ignoreNonSpecified = true;
			private boolean analyzed = false;
			private String error = null;

			private CommitMatcher() {
			}

			public ProjectMatcher contains(String... refactorings) {
				for (String refactoring : refactorings) {
					expected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}

			public ProjectMatcher containsOnly(String... refactorings) {
				this.ignoreNonSpecified = false;
				this.expected = new HashSet<String>();
				this.notExpected = new HashSet<String>();
				for (String refactoring : refactorings) {
					expected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
		}
	}
}
