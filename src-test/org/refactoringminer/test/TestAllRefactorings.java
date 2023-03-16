package org.refactoringminer.test;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;

import org.junit.Test;

import java.util.List;
import java.util.Map;
interface TestExpectation {
	void toHave(Map<RefactoringType, Integer> refactorings);
	void toHave(List<String> refactorings);
}
public class TestAllRefactorings {

	@Test
	public void testAllRefactorings() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		TestBuilder test = new TestBuilder(detector, "tmp1", Refactorings.All.getValue());
		RefactoringPopulator.feedRefactoringsInstances(Refactorings.All.getValue(), Systems.FSE.getValue(), test);
		test.assertExpectations(11563, 23, 275);
	}
}
