package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModelSet;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;

public class RefactoringMatcher extends RefactoringHandler {

	private static final String TMP_DIR = "tmp";
	private final String cloneUrl;
	private final String branch;
	Map<String, Set<String>> expected = new HashMap<>();
	boolean ignoreNonSpecifiedCommits = true;
	private int truePositiveCount = 0;
	private int falsePositiveCount = 0;
	private int falseNegativeCount = 0;
	private Map<String, Set<String>> falsePositives = new HashMap<>();

	private RefactoringMatcher(String cloneUrl, String branch) {
		this.cloneUrl = cloneUrl;
		this.branch = branch;
	}

	public RefactoringMatcher containsOnly() {
		this.ignoreNonSpecifiedCommits = false;
		return this;
	}

	public RefactoringMatcher contains() {
		this.ignoreNonSpecifiedCommits = true;
		return this;
	}

	public RefactoringMatcher atCommit(String commitId, String ... refactorings) {
		Set<String> set = new HashSet<>();
		for (String refactoring : refactorings) {
			set.add(refactoring);
		}
		expected.put(commitId, set);
		return this;
	}

	@Override
	public boolean skipRevision(RevCommit curRevision) {
		if (this.ignoreNonSpecifiedCommits) {
			String commitId = curRevision.getId().getName();
			return !this.expected.containsKey(commitId);
		}
		return false;
	}

	@Override
	public void handleDiff(RevCommit prevRevision, UMLModelSet prevModel, RevCommit curRevision, UMLModelSet curModel, List<Refactoring> refactorings) {
		String commitId = curRevision.getId().getName();
		Set<String> expectedRefs;
		if (expected.containsKey(commitId)) {
			expectedRefs = expected.get(commitId);
		}
		else {
			expectedRefs = Collections.emptySet();
		}
		for (Refactoring refactoring : refactorings) {
			String refFound = refactoring.toString();
			if (expectedRefs.contains(refFound)) {
				expectedRefs.remove(refFound);
				this.addTruePositive(commitId, refFound);
			} else {
				this.addFalsePositive(commitId, refFound);
			}
		}
	}

	private void addTruePositive(String commitId, String refFound) {
		this.truePositiveCount++;
	}

	private void addFalsePositive(String commitId, String refFound) {
		Set<String> found = this.falsePositives.get(commitId);
		if (found == null) {
			found = new HashSet<String>();
			this.falsePositives.put(commitId, found);
		}
		found.add(refFound);
		this.falsePositiveCount++;
	}

	private void addFalseNegative(String commitId, String refExpected) {
		this.falseNegativeCount++;
	}

	private void countFalseNegatives() {
		for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
			for (String ref : entry.getValue()) {
				this.addFalseNegative(entry.getKey(), ref);
			}
		}
	}
	
	private void printResults() {
		if (this.falsePositiveCount > 0 || this.falseNegativeCount > 0) {
			System.out.println(this.cloneUrl);
		}
		if (this.falsePositiveCount > 0) {
			System.out.println(" false positives");
			print(this.falsePositives);
		}
		if (this.falseNegativeCount > 0) {
			System.out.println(" false negatives");
			print(this.expected);
		}
	}

	private void print(Map<String, Set<String>> map) {
		for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				System.out.println("  at commit " + entry.getKey());
			}
			for (String ref : entry.getValue()) {
				System.out.println("   " + ref);
			}
		}
	}

	public static RefactoringMatcher project(String cloneUrl, String branch) {
		return new RefactoringMatcher(cloneUrl, branch);
	}

	public static void assertThat(RefactoringMatcher ... matchers) throws Exception {
		int tp = 0;
		int fp = 0;
		int fn = 0;
		GitService gitService = new GitServiceImpl();
		RefactoringDetectorImpl refactoringDetector = new RefactoringDetectorImpl();
		for (RefactoringMatcher m : matchers) {
			String folder = TMP_DIR + "/" + m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
			Repository rep = gitService.cloneIfNotExists(folder, m.cloneUrl, m.branch);
			refactoringDetector.detectAll(rep, m);
			m.countFalseNegatives();
			tp += m.truePositiveCount;
			fp += m.falsePositiveCount;
			fn += m.falseNegativeCount;
		}
		boolean success = fp == 0 && fn == 0;
		double precision = ((double) tp) / (tp + fp);
		double recall = ((double) tp) / (tp + fn);
		String mainResultMessage = String.format("Precision: %.3f  Recall: %.3f", precision, recall);
		
		System.out.println(mainResultMessage);
		if (!success) {
			for (RefactoringMatcher m : matchers) {
				m.printResults();
			}
		}
		Assert.assertTrue(mainResultMessage, success);
	}

}
