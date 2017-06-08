package org.refactoringminer.test;

import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;
import org.junit.Test;

public class TestAllRefactorings {

	@Test
	public void testAllRefactorings() throws Exception {

		int refactoring = Refactorings.All.getValue();

		TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), "tmp1", refactoring);

		RefactoringPopulator.feedRefactoringsInstances(refactoring, Systems.FSE.getValue(), test);

		RefactoringPopulator.printRefDiffResults(refactoring);
		test.assertExpectations();
	}
}
