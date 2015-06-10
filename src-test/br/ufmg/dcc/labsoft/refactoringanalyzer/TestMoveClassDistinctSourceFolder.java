package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestMoveClassDistinctSourceFolder {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		test.project("https://github.com/SonarSource/sonarqube.git", "master").atCommit("abbf32571232db81a5343db17a933a9ce6923b44").contains(
			"Move Class org.sonar.core.notification.NotificationDispatcher moved to org.sonar.server.notification.NotificationDispatcher"
		);
		test.assertExpectations();
	}

}
