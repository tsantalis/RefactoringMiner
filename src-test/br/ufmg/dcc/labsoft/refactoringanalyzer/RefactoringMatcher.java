package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

public class RefactoringMatcher extends RefactoringHandler {

	Map<String, Set<String>> expected = new HashMap<>();

	@Override
	public void handleDiff(Revision prevRevision, UMLModel prevModel, Revision curRevision, UMLModel curModel, List<Refactoring> refactorings) {
		String commitId = curRevision.getId();
		if (expected.containsKey(commitId)) {
			Set<String> refs = expected.get(commitId);
			for (Refactoring refactoring : refactorings) {
				String refFound = refactoring.toString();
				if (refs.contains(refFound)) {
					refs.remove(refFound);
				} else {
					Assert.fail(String.format("Refactoring was not expected at revision %s: %s", commitId, refFound));
				}
			}
		} else {
			for (Refactoring refactoring : refactorings) {
				String refFound = refactoring.toString();
				Assert.fail(String.format("No refactoring was expected at revision %s, but found: %s", commitId, refFound));
			}
		}
		super.handleDiff(prevRevision, prevModel, curRevision, curModel, refactorings);
	}
	
	public RefactoringMatcher expectAtCommit(String commitId, String ... refactorings) {
		Set<String> set = new HashSet<>();
		for (String refactoring : refactorings) {
			set.add(refactoring);
		}
		expected.put(commitId, set);
		return this;
	}

	public void checkFalseNegatives() {
		for (Map.Entry<String, Set<String>> entry : expected.entrySet()) {
			for (String ref : entry.getValue()) {
				Assert.fail(String.format("Refactoring was expected at revision %s: %s", entry.getKey(), ref));
			}
		}
	}
}
