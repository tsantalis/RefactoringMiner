package org.refactoringminer.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
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

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import org.junit.Assert;
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

	public void assertExpectations(Project project, int expectedTPs, int expectedFPs, int expectedFNs, boolean log) throws Exception {
		c = new Counter();
		cMap = new HashMap<RefactoringType, Counter>();
		commitsCount = 0;
		errorCommitsCount = 0;
		GitService gitService = new GitServiceImpl();
		ExecutorService pool = Executors.newFixedThreadPool(map.size());
		for (ProjectMatcher m : map.values()) {
			String folder = tempDir + "/"
					+ m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
			GitRepository rep = gitService.cloneIfNotExists(project, folder, m.cloneUrl/* , m.branch */);
			if (m.ignoreNonSpecifiedCommits) {
				// It is faster to only look at particular commits
				for (String commitId : m.getCommits()) {
					Runnable r = () -> refactoringDetector.detectAtCommit(rep, commitId, m);
					pool.submit(r);
					System.out.println("Processed " + m.cloneUrl + "\t" + commitId);
				}
			} else {
				// Iterate over each commit
				//refactoringDetector.detectAll(rep, m.branch, m);
			}
		}
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		String mainResultMessage = buildResultMessage(c);
		boolean success = get(FP) == expectedFPs && get(FN) == expectedFNs && get(TP) == expectedTPs;

		if(log) {
			try (FileWriter fw = new FileWriter("log.txt", true);
				 BufferedWriter bw = new BufferedWriter(fw);
				 PrintWriter out = new PrintWriter(bw)) {
				out.println(String.format("Commits: %d  Errors: %d", commitsCount, errorCommitsCount));

				out.println("Total  " + mainResultMessage);
				for (RefactoringType refType : RefactoringType.values()) {
					Counter refTypeCounter = cMap.get(refType);
					if (refTypeCounter != null) {
						out.println(String.format("%-7s", refType.getAbbreviation()) + buildResultMessage(refTypeCounter));
					}
				}

				if (!success || verbose) {
					for (ProjectMatcher m : map.values()) {
						String results = m.printResults();
						if (results.length() > 0)
							out.println(results);
					}
				}
			}
		}
		Assert.assertTrue(mainResultMessage, success);
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

	public class ProjectMatcher extends RefactoringHandler {

		private final String cloneUrl;
		private final String branch;
		private Map<String, CommitMatcher> expected = new HashMap<>();
		private boolean ignoreNonSpecifiedCommits = true;
		private int truePositiveCount = 0;
		private int falsePositiveCount = 0;
		private int falseNegativeCount = 0;
		private int trueNegativeCount = 0;
		private int unknownCount = 0;
		// private int errorsCount = 0;

		private ProjectMatcher(String cloneUrl, String branch) {
			this.cloneUrl = cloneUrl;
			this.branch = branch;
		}

		public ProjectMatcher atNonSpecifiedCommitsContainsNothing() {
			this.ignoreNonSpecifiedCommits = false;
			return this;
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
			//String commitId = curRevision.getId().getName();
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
						this.truePositiveCount++;
						count(TP, expectedRefactoring);
						matcher.truePositive.add(expectedRefactoring);
					}
				}

				// count false positives
				for (Iterator<String> iter = matcher.notExpected.iterator(); iter.hasNext();) {
					String notExpectedRefactoring = iter.next();
					if (refactoringsFound.contains(notExpectedRefactoring)) {
						refactoringsFound.remove(notExpectedRefactoring);
						this.falsePositiveCount++;
						count(FP, notExpectedRefactoring);
					} else {
						this.trueNegativeCount++;
						count(TN, notExpectedRefactoring);
						iter.remove();
					}
				}
				// count false positives when using containsOnly
				if (matcher.ignoreNonSpecified) {
					for (String refactoring : refactoringsFound) {
						matcher.unknown.add(refactoring);
						this.unknownCount++;
						count(UNK, refactoring);
					}
				} else {
					for (String refactoring : refactoringsFound) {
						matcher.notExpected.add(refactoring);
						this.falsePositiveCount++;
						count(FP, refactoring);
					}
				}

				// count false negatives
				for (String expectedButNotFound : matcher.expected) {
					this.falseNegativeCount++;
					count(FN, expectedButNotFound);
				}
			}
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
			e.printStackTrace();
			errorCommitsCount++;
			// System.err.println(" error at commit " + commitId + ": " +
			// e.getMessage());
		}

		private String printResults() {
			// if (verbose || this.falsePositiveCount > 0 ||
			// this.falseNegativeCount > 0 || this.errorsCount > 0) {
			// System.out.println(this.cloneUrl);
			// }
			StringBuilder sb = new StringBuilder();
			String baseUrl = this.cloneUrl.substring(0, this.cloneUrl.length() - 4) + "/commit/";
			for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
				String commitUrl = baseUrl + entry.getKey();
				CommitMatcher matcher = entry.getValue();
				if (matcher.error != null) {
					sb.append("error at " + commitUrl + ": " + matcher.error).append("\n");
				} else {
					if (verbose || !matcher.expected.isEmpty() || !matcher.notExpected.isEmpty()
							|| !matcher.unknown.isEmpty()) {
						if (!matcher.analyzed) {
							sb.append("at not analyzed " + commitUrl).append("\n");
						} else {
							sb.append("at " + commitUrl).append("\n");
						}
					}
					if (verbose && !matcher.truePositive.isEmpty()) {
						sb.append(" true positives").append("\n");
						for (String ref : matcher.truePositive) {
							sb.append("  " + ref).append("\n");
						}
					}
					if (!matcher.notExpected.isEmpty()) {
						sb.append(" false positives").append("\n");
						for (String ref : matcher.notExpected) {
							sb.append("  " + ref).append("\n");
						}
					}
					if (!matcher.expected.isEmpty()) {
						sb.append(" false negatives").append("\n");
						for (String ref : matcher.expected) {
							sb.append("  " + ref).append("\n");
						}
					}
					if (!matcher.unknown.isEmpty()) {
						sb.append(" unknown").append("\n");
						for (String ref : matcher.unknown) {
							sb.append("  " + ref).append("\n");
						}
					}
				}
			}
			String string = sb.toString();
			if(string.endsWith("\n"))
				return string.substring(0, string.length()-1);
			return string;
		}

		// private void countFalseNegatives() {
		// for (Map.Entry<String, CommitMatcher> entry :
		// this.expected.entrySet()) {
		// CommitMatcher matcher = entry.getValue();
		// if (matcher.error == null) {
		// this.falseNegativeCount += matcher.expected.size();
		// }
		// }
		// }

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

			public ProjectMatcher containsNothing() {
				return containsOnly();
			}

			public ProjectMatcher notContains(String... refactorings) {
				for (String refactoring : refactorings) {
					notExpected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
		}
	}
}
