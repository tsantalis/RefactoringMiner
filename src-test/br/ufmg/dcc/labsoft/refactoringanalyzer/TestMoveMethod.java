package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestMoveMethod {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		
		test.project("https://github.com/voldemort/voldemort.git", "master").atCommit("0cc38b8ce750c9940eefe9b3274c4fb2f9e5437e").contains(
			"Move Method public getAddedInTarget(current Set<T>, target Set<T>) : Set<T> from class voldemort.utils.RebalanceUtils to public getAddedInTarget(current Set<T>, target Set<T>) : Set<T> from class voldemort.utils.Utils"
		);
		test.project("https://github.com/jMonkeyEngine/jmonkeyengine.git", "master").atCommit("7f2c7c5d356acc350e705168e972112bfb151e83").containsNothing();
		
		test.assertExpectations();
	}

}
