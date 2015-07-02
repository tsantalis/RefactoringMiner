package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestMoveClass {

	@Test
	public void testMoveInDistinctSourceFolders() throws Exception {
		TestBuilder test = new TestBuilder();
		test.project("https://github.com/SonarSource/sonarqube.git", "master").atCommit("abbf32571232db81a5343db17a933a9ce6923b44").contains(
			"Move Class org.sonar.core.notification.NotificationDispatcher moved to org.sonar.server.notification.NotificationDispatcher"
		);
		test.assertExpectations();
	}

	@Test
	public void testMovePathButKeepSamePackage() throws Exception {
		TestBuilder test = new TestBuilder();
		test.project("https://github.com/dropwizard/metrics.git", "master").atCommit("9ce7346cc0d243d29619ae4b867ee8e694844f8d").contains(
			"Move Class Folder io.dropwizard.metrics.influxdb.utils.InfluxDbWriteObjectSerializerTest moved from metrics-influxdb/src/test/java/com/codehale to metrics-influxdb/src/test/java/io/dropwizard",
			"Move Class Folder io.dropwizard.metrics.influxdb.InfluxDbReporterTest moved from metrics-influxdb/src/test/java/com/codehale to metrics-influxdb/src/test/java/io/dropwizard"
		);
		test.assertExpectations();
	}

	@Test
	public void testMoveRootClassWithInnerClasses() throws Exception {
		TestBuilder test = new TestBuilder();
		test.project("https://github.com/hierynomus/sshj.git", "master").atCommit("e334525da503d04a978eb9482ab8c7aec02a0b69").containsNothing();
		//Move Class net.schmizz.sshj.userauth.GssApiTest.TestAuthConfiguration moved to com.hierynomus.sshj.userauth.GssApiTest.TestAuthConfiguration
		test.assertExpectations();
	}

	@Test
	public void testStrangeMove() throws Exception {
		TestBuilder test = new TestBuilder();
		test.project("https://github.com/liferay/liferay-portal.git", "master").atCommit("ed3204fae38bbe9b5f99029ebbf14f162ccea2f1").contains(
			"Move Class com.liferay.exportimport.LayoutSetPrototypePropagationTest moved to com.liferay.export.import.LayoutSetPrototypePropagationTest"
		);
		test.assertExpectations();
	}

}
