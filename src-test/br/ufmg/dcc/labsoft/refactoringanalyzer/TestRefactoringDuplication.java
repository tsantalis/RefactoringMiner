package br.ufmg.dcc.labsoft.refactoringanalyzer;

import static br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringMatcher.assertThat;
import static br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringMatcher.project;

import org.junit.Test;

public class TestRefactoringDuplication {

	@Test
	public void test() throws Exception {
		assertThat(
			project("https://github.com/commonsguy/cw-omnibus.git", "master").contains()
			.atCommit("ffafecd870f56f9e221a362fe6a14c2cdc48d20e")
		);
	}

}
