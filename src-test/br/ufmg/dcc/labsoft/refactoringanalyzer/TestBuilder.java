package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.diff.Refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;

import br.ufmg.dcc.labsoft.refdetector.GitHistoryRefactoringDetector;
import br.ufmg.dcc.labsoft.refdetector.GitHistoryRefactoringDetectorImpl;
import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;
import br.ufmg.dcc.labsoft.refdetector.RefactoringHandler;


public class TestBuilder {

	private final String tempDir;
	private final Map<String, ProjectMatcher> map;
	private final GitHistoryRefactoringDetector refactoringDetector;
	private final boolean verbose;
	private int commitsCount;
	
	public TestBuilder(GitHistoryRefactoringDetector detector, String tempDir) {
		this.map = new HashMap<String, ProjectMatcher>();
		this.refactoringDetector = detector;
		this.tempDir = tempDir;
		this.verbose = true;
	}

	public TestBuilder() {
		this(new GitHistoryRefactoringDetectorImpl(), "tmp");
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
		int unknown = 0;
		commitsCount = 0;
		GitService gitService = new GitServiceImpl();
		
		for (ProjectMatcher m : map.values()) {
			String folder = tempDir + "/" + m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
			try (Repository rep = gitService.cloneIfNotExists(folder, m.cloneUrl/*, m.branch*/)) {
    			if (m.ignoreNonSpecifiedCommits) {
    				// It is faster to only look at particular commits
    				for (String commitId : m.getCommits()) {
    			        refactoringDetector.detectAtCommit(rep, commitId, m);
    				}
    			} else {
    				// Iterate over each commit
    				refactoringDetector.detectAll(rep, m.branch, m);
    			}
			}
			m.countFalseNegatives();
			tp += m.truePositiveCount;
			fp += m.falsePositiveCount;
			fn += m.falseNegativeCount;
			unknown += m.unknownCount;
		}
		boolean success = fp == 0 && fn == 0 && tp > 0;
		double precision = ((double) tp) / (tp + fp);
		double recall = ((double) tp) / (tp + fn);
		System.out.println(String.format("Commits: %d, TP: %d  FP: %d  FN: %d  Unknown: %d", commitsCount, tp, fp, fn, unknown));
		String mainResultMessage = String.format("Precision: %.3f  Recall: %.3f", precision, recall);
		
		System.out.println(mainResultMessage);
		if (!success || verbose) {
			for (ProjectMatcher m : map.values()) {
				m.printResults();
			}
		}
		Assert.assertTrue(mainResultMessage, success);
	}

	private static List<String> normalize(String refactoring) {
	    // Decompose Extract Superclass/Interface 
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
	    return Collections.singletonList(normalizeSingle(refactoring));
	}
	
	/**
	 * Remove generics type information.
	 */
	private static String normalizeSingle(String refactoring) {
	    StringBuilder sb = new StringBuilder();
	    int openGenerics = 0;
	    for (int i = 0; i < refactoring.length(); i++) {
	        char c = refactoring.charAt(i);
	        if (c == '<') {
	            openGenerics++;
	        }
	        if (c == '\t') {
                c = ' ';
            }
	        if (openGenerics == 0) {
	            sb.append(c);
	        }
	        if (c == '>') {
                openGenerics--;
            }
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
		private int unknownCount = 0;
		private int errorsCount = 0;

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
		public void handle(RevCommit curRevision, List<Refactoring> refactorings) {
			CommitMatcher matcher;
			commitsCount++;
			String commitId = curRevision.getId().getName();
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
					refactoringsFound.addAll(normalize(refactoring.toString()));
				}
				// count true positives
				for (Iterator<String> iter = matcher.expected.iterator(); iter.hasNext();) {
					String expectedRefactoring = iter.next();
					if (refactoringsFound.contains(expectedRefactoring)) {
						iter.remove();
						refactoringsFound.remove(expectedRefactoring);
						this.truePositiveCount++;
						matcher.truePositive.add(expectedRefactoring);
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
				if (matcher.ignoreNonSpecified) {
				    for (String refactoring : refactoringsFound) {
                        matcher.unknown.add(refactoring);
                        this.unknownCount++;
                    }
				}
				else {
					for (String refactoring : refactoringsFound) {
						matcher.notExpected.add(refactoring);
						this.falsePositiveCount++;
					}
				}
			}
		}

		@Override
		public void handleException(String commitId, Exception e) {
		    if (expected.containsKey(commitId)) {
		        CommitMatcher matcher = expected.get(commitId);
		        matcher.error = e.toString();
            }
		    errorsCount++;
		    //System.err.println(" error at commit " + commitId + ": " + e.getMessage());
		}

		private void printResults() {
//			if (verbose || this.falsePositiveCount > 0 || this.falseNegativeCount > 0 || this.errorsCount > 0) {
//				System.out.println(this.cloneUrl);
//			}
			String baseUrl = this.cloneUrl.substring(0, this.cloneUrl.length() - 4) + "/commit/";
			for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
			    String commitUrl = baseUrl + entry.getKey();
				CommitMatcher matcher = entry.getValue();
				if (matcher.error != null) {
				    System.out.println("error at " + commitUrl + ": " + matcher.error);
				} else {
				    if (verbose || !matcher.expected.isEmpty() || !matcher.notExpected.isEmpty()) {
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
				    if (verbose && !matcher.unknown.isEmpty()) {
                        System.out.println(" unknown");
                        for (String ref : matcher.unknown) {
                            System.out.println("  " + ref);
                        }
                    }
				}
			}
		}

		private void countFalseNegatives() {
			for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
				CommitMatcher matcher = entry.getValue();
				if (matcher.error == null) {
                    this.falseNegativeCount += matcher.expected.size();
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
			public ProjectMatcher contains(String ... refactorings) {
				for (String refactoring : refactorings) {
					expected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
			public ProjectMatcher containsOnly(String ... refactorings) {
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
			public ProjectMatcher notContains(String ... refactorings) {
				for (String refactoring : refactorings) {
					notExpected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
		}
	}
}


