package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestRefactoringDuplication {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		test.project("https://github.com/commonsguy/cw-omnibus.git", "master").atCommit("ffafecd870f56f9e221a362fe6a14c2cdc48d20e").containsNothing();
		test.assertExpectations();
	}

}
