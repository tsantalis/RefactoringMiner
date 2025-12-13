package org.refactoringminer.test;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;

public class TestPythonDatasetRefactorings {
	private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";

	@Test
	public void testAllRefactorings() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		BigInteger types = 
					Refactorings.ExtractMethod.getValue()
					.or(Refactorings.InlineMethod.getValue())
					.or(Refactorings.RenameMethod.getValue())
					.or(Refactorings.RenameParameter.getValue())
					.or(Refactorings.ReorderParameter.getValue())
					.or(Refactorings.AddParameter.getValue())
					.or(Refactorings.RemoveParameter.getValue())
					.or(Refactorings.MoveAndRenameClass.getValue())
					.or(Refactorings.RenameClass.getValue())
					.or(Refactorings.ChangeVariableType.getValue())
					.or(Refactorings.RenameVariable.getValue())
					.or(Refactorings.ExtractVariable.getValue())
					.or(Refactorings.InlineVariable.getValue())
					.or(Refactorings.MoveAttribute.getValue())
					.or(Refactorings.PushDownAttribute.getValue())
					.or(Refactorings.PushDownMethod.getValue())
					.or(Refactorings.ExtractSubclass.getValue())
					.or(Refactorings.RenameAttribute.getValue())
					.or(Refactorings.MoveMethod.getValue())
					.or(Refactorings.MoveAndRenameMethod.getValue())
					.or(Refactorings.MoveClass.getValue())
					.or(Refactorings.ExtractClass.getValue())
					.or(Refactorings.ExtractSuperclass.getValue())
					.or(Refactorings.ExtractAndMoveMethod.getValue())
					.or(Refactorings.AddClassAnnotation.getValue())
					.or(Refactorings.MoveCode.getValue())
					.or(Refactorings.AddMethodAnnotation.getValue())
					.or(Refactorings.RemoveMethodAnnotation.getValue())
					.or(Refactorings.SplitConditional.getValue())
					.or(Refactorings.PullUpMethod.getValue())
					.or(Refactorings.ReplaceVariableWithAttribute.getValue())
					.or(Refactorings.ReplaceAttributeWithVariable.getValue())
					.or(Refactorings.LocalizeParameter.getValue())
					.or(Refactorings.ParameterizeAttribute.getValue())
					.or(Refactorings.ParameterizeVariable.getValue())
					.or(Refactorings.EncapsulateAttribute.getValue());
		TestBuilder test = new TestBuilder(detector, REPOS, types);
		RefactoringPopulator.preparePythonRefactorings(test, types);
		test.assertExpectationsWithGitHubAPI(908, 6, 2);
	}
}
