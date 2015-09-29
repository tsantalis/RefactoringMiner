package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestExtractMethod2 {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		
		// -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=myrecording.jfr
		test.project("https://github.com/eclipse/vert.x.git", "master").atCommit("c53e67f865e6322d00009f1373df3f87978c6c98").containsOnly(
			//"Extract Method	private createConnAndHandle(ch Channel, msg Object, shake WebSocketServerHandshaker) : void extracted from private handleHttp(conn ServerConnection, ch Channel, msg Object) : void in class io.vertx.core.http.impl.HttpServerImpl.ServerHandler"
		);
		
		test.assertExpectations();
	}

}
