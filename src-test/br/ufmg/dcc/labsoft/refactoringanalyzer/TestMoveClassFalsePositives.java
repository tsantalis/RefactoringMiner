package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestMoveClassFalsePositives {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();

		test.project("https://github.com/google/guava.git", "master").atCommit("cc3b0f8dc48497a2911dfe31f60fe186b3fed8d4").containsNothing();
		
		test.assertExpectations();
	}

}
