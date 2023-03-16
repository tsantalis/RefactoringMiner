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
	public void testAllRefactoringsWithinOneCommit_StringList() {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		TestBuilder test = new TestBuilder(detector, "tmp1", Refactorings.All.getValue());
		test.expect("https://github.com/checkstyle/checkstyle.git", "master" , "0a1a4c6e94c9b3b73b21b323f14ae7b7337b1b44")
				.toHave(List.of("Rename Method private markFinalVariableCandidateAsAssignInIfBlock(ast DetailAST) : void renamed to private markFinalVariableCandidateAsAssignedInIfBlock(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck",
								"Merge Method [private isInIfBlock(node DetailAST) : boolean, private isInElseBlock(node DetailAST) : boolean] to private isInSpecificCodeBlock(node DetailAST, blockType int) : boolean in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck",
								"Rename Method private markFinalVariableCandidateAsAssignOutsideIfOrElseBlock(ast DetailAST) : void renamed to private markFinalVariableCandidateAsAssignedOutsideIfOrElseBlock(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck",
								"Rename Method private markFinalVariableCandidateAsAssignInElseBlock(ast DetailAST) : void renamed to private markFinalVariableCandidateAsAssignedInElseBlock(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.FinalLocalVariableCheck"));
	}

	@Test
	public void testAllRefactorings() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		TestBuilder test = new TestBuilder(detector, "tmp1", Refactorings.All.getValue());
		RefactoringPopulator.feedRefactoringsInstances(Refactorings.All.getValue(), Systems.FSE.getValue(), test);
		test.assertExpectations(11563, 23, 275);
	}

	@Test
	public void testAllRefactoringsWithinOneCommit_RefactoringTypeMap() {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		TestBuilder test = new TestBuilder(detector, "tmp1", Refactorings.All.getValue());
		test.expect("https://github.com/checkstyle/checkstyle.git", "master" , "0a1a4c6e94c9b3b73b21b323f14ae7b7337b1b44")
				.toHave(Map.of(
						RefactoringType.MERGE_OPERATION, 1,
						RefactoringType.RENAME_METHOD, 3));
	}
}
