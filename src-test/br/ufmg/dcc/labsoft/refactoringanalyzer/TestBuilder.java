package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModelSet;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;


public class TestBuilder {

	private static final String TMP_DIR = "tmp";
	
	private Map<String, ProjectMatcher> map = new HashMap<String, ProjectMatcher>();
	
	public TestBuilder() {
		map = new HashMap<>();
	}
	
	public final ProjectMatcher project(String cloneUrl, String branch) {
		ProjectMatcher projectMatcher = this.map.get(cloneUrl);
		if (projectMatcher == null) {
			projectMatcher = new ProjectMatcher(cloneUrl, branch);
			this.map.put(cloneUrl, projectMatcher);
		}
		return projectMatcher;
	}

	public void assertExpectations() throws Exception {
		int tp = 0;
		int fp = 0;
		int fn = 0;
		GitService gitService = new GitServiceImpl();
		RefactoringDetectorImpl refactoringDetector = new RefactoringDetectorImpl();
		for (ProjectMatcher m : map.values()) {
			String folder = TMP_DIR + "/" + m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
			if (m.ignoreNonSpecifiedCommits) {
				Repository rep = gitService.cloneIfNotExists(folder, m.cloneUrl/*, m.branch*/);
				// It is faster to only look at particular commits
				ASTParser parser = refactoringDetector.buildAstParser(rep);
				for (String commitId : m.getCommits()) {
					refactoringDetector.detectOne(parser, rep, commitId, null, m);
				}
			} else {
				Repository rep = gitService.cloneIfNotExists(folder, m.cloneUrl/*, m.branch*/);
				// Iterate over each commit
				refactoringDetector.detectAll(rep, m.branch, m);
			}
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
			for (ProjectMatcher m : map.values()) {
				m.printResults();
			}
		}
		Assert.assertTrue(mainResultMessage, success);
	}

	private static String normalize(String refactoring) {
		return refactoring.replace('\t', ' ');
	}
	
	public class ProjectMatcher extends RefactoringHandler {

		private final String cloneUrl;
		private final String branch;
		private Map<String, CommitMatcher> expected = new HashMap<>();
		private boolean ignoreNonSpecifiedCommits = true;
		private int truePositiveCount = 0;
		private int falsePositiveCount = 0;
		private int falseNegativeCount = 0;

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
		public boolean skipRevision(String commitId) {
			if (this.ignoreNonSpecifiedCommits) {
				return !this.expected.containsKey(commitId);
			}
			return false;
		}

		@Override
		public void handleDiff(UMLModelSet prevModel, String commitId, RevCommit curRevision, UMLModelSet curModel, List<Refactoring> refactorings) {
			CommitMatcher matcher;
			if (expected.containsKey(commitId)) {
				matcher = expected.get(commitId);
			}
			else if (!this.ignoreNonSpecifiedCommits) {
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
					refactoringsFound.add(normalize(refactoring.toString()));
				}
				// count true positives
				for (Iterator<String> iter = matcher.expected.iterator(); iter.hasNext();) {
					String expectedRefactoring = iter.next();
					if (refactoringsFound.contains(expectedRefactoring)) {
						iter.remove();
						refactoringsFound.remove(expectedRefactoring);
						this.truePositiveCount++;
					}
				}
				
				// count false positives
				for (Iterator<String> iter = matcher.notExpected.iterator(); iter.hasNext();) {
					String notExpectedRefactoring = iter.next();
					if (refactoringsFound.contains(notExpectedRefactoring)) {
						refactoringsFound.remove(notExpectedRefactoring);
						this.falsePositiveCount++;
					} else {
						iter.remove();
					}
				}
				// count false positives when using containsOnly
				if (!matcher.ignoreNonSpecified) {
					for (String refactoring : refactoringsFound) {
						matcher.notExpected.add(refactoring);
						this.falsePositiveCount++;
					}
				}
			}
		}

		private void printResults() {
			if (this.falsePositiveCount > 0 || this.falseNegativeCount > 0) {
				System.out.println(this.cloneUrl);
			}
			for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
				CommitMatcher matcher = entry.getValue();
				if (!matcher.expected.isEmpty() || !matcher.notExpected.isEmpty()) {
					if (!matcher.analyzed) {
						System.out.println(" at not analyzed commit " + entry.getKey());
					} else {
						System.out.println(" at commit " + entry.getKey());
					}
				}
				if (!matcher.notExpected.isEmpty()) {
					System.out.println("  false positives");
					for (String ref : matcher.notExpected) {
						System.out.println("   " + ref);
					}
				}
				if (!matcher.expected.isEmpty()) {
					System.out.println("  false negatives");
					for (String ref : matcher.expected) {
						System.out.println("   " + ref);
					}
				}
			}
		}

		private void countFalseNegatives() {
			for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
				CommitMatcher matcher = entry.getValue();
				this.falseNegativeCount += matcher.expected.size();
			}
		}

		public class CommitMatcher {
			private Set<String> expected = new HashSet<String>();
			private Set<String> notExpected = new HashSet<String>();
			private boolean ignoreNonSpecified = true;
			private boolean analyzed = false;
			private CommitMatcher() {
			}
			public ProjectMatcher contains(String ... refactorings) {
				for (String refactoring : refactorings) {
					expected.add(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
			public ProjectMatcher containsOnly(String ... refactorings) {
				this.ignoreNonSpecified = false;
				this.expected = new HashSet<String>();
				this.notExpected = new HashSet<String>();
				for (String refactoring : refactorings) {
					expected.add(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
			public ProjectMatcher containsNothing() {
				return containsOnly();
			}
			public ProjectMatcher notContains(String ... refactorings) {
				for (String refactoring : refactorings) {
					notExpected.add(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
		}
	}
}


