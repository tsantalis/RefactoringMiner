package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestPerformance {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		
		// -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=myrecording.jfr
		test.project("https://github.com/elastic/elasticsearch.git", "master").atCommit("0cec37f3c3f191a775ebcef833a00d57d1f0fe94").containsOnly();
		
		test.assertExpectations();
	}

}
