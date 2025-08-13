package org.refactoringminer.test;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;

public class TestNewDatasetRefactorings {
	private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";

	@Test
	public void testAllRefactorings() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		BigInteger types = 
					Refactorings.ExtractMethod.getValue()
				.or(Refactorings.RenameClass.getValue())
				.or(Refactorings.RenameMethod.getValue())
				.or(Refactorings.RenameAttribute.getValue())
				.or(Refactorings.RenameVariable.getValue())
				.or(Refactorings.MoveClass.getValue())
				.or(Refactorings.MoveAndRenameClass.getValue())
				.or(Refactorings.MoveMethod.getValue())
				.or(Refactorings.MoveAndRenameMethod.getValue())
				.or(Refactorings.MoveAttribute.getValue())
				.or(Refactorings.MoveAndRenameAttribute.getValue())
				.or(Refactorings.PullUpMethod.getValue())
				.or(Refactorings.PushDownMethod.getValue())
				.or(Refactorings.PullUpAttribute.getValue())
				.or(Refactorings.PushDownAttribute.getValue())
				.or(Refactorings.ExtractAndMoveMethod.getValue())
				.or(Refactorings.ExtractClass.getValue())
				.or(Refactorings.ExtractSuperclass.getValue())
				.or(Refactorings.ExtractSubclass.getValue())
				.or(Refactorings.ExtractInterface.getValue())
				.or(Refactorings.ExtractVariable.getValue())
				.or(Refactorings.ExtractAttribute.getValue())
				.or(Refactorings.InlineMethod.getValue())
				.or(Refactorings.MoveAndInlineMethod.getValue())
				.or(Refactorings.InlineVariable.getValue())
				.or(Refactorings.ChangeTypeDeclarationKind.getValue())
				.or(Refactorings.ChangeReturnType.getValue())
				.or(Refactorings.ChangeAttributeType.getValue())
				.or(Refactorings.ChangeVariableType.getValue())
				.or(Refactorings.SplitMethod.getValue())
				.or(Refactorings.MergeMethod.getValue())
				.or(Refactorings.ReplaceAnonymousWithClass.getValue())
				.or(Refactorings.ReplacePipelineWithLoop.getValue());
		TestBuilder test = new TestBuilder(detector, REPOS, types);
		RefactoringPopulator.feedTSERefactoringInstances(test);
		test.assertExpectationsWithGitHubAPI(3211, 134, 231);
	}
}
