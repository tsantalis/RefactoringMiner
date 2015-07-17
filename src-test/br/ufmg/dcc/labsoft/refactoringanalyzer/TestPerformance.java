package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestPerformance {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		
		// -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=myrecording.jfr
		test.project("https://github.com/elastic/elasticsearch.git", "master").atCommit("0cec37f3c3f191a775ebcef833a00d57d1f0fe94").containsOnly(
//			"Extract And Move Method public prepareForIndexRecovery() : void extracted from public recover(indexShouldExists boolean, recoveryState RecoveryState) : void in class org.elasticsearch.index.gateway.IndexShardGateway & moved to class org.elasticsearch.index.shard.IndexShard",
//			"Extract And Move Method public writeTo(out StreamOutput) : void extracted from public writeTo(out StreamOutput) : void in class org.elasticsearch.indices.recovery.RecoveryState.Index & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Extract And Move Method public readFrom(in StreamInput) : void extracted from public readFrom(in StreamInput) : void in class org.elasticsearch.indices.recovery.RecoveryState & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Extract And Move Method public readFrom(in StreamInput) : void extracted from public readFrom(in StreamInput) : void in class org.elasticsearch.indices.recovery.RecoveryState.Start & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Extract And Move Method public readFrom(in StreamInput) : void extracted from public readFrom(in StreamInput) : void in class org.elasticsearch.indices.recovery.RecoveryState.Translog & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Extract And Move Method public writeTo(out StreamOutput) : void extracted from public writeTo(out StreamOutput) : void in class org.elasticsearch.indices.recovery.RecoveryState.Start & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Extract And Move Method public writeTo(out StreamOutput) : void extracted from public writeTo(out StreamOutput) : void in class org.elasticsearch.indices.recovery.RecoveryState.Translog & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Extract And Move Method public prepareForTranslogRecovery() : void extracted from public recover(indexShouldExists boolean, recoveryState RecoveryState) : void in class org.elasticsearch.index.gateway.IndexShardGateway & moved to class org.elasticsearch.index.shard.IndexShard",
//			"Extract And Move Method public readFrom(in StreamInput) : void extracted from public readFrom(in StreamInput) : void in class org.elasticsearch.indices.recovery.RecoveryState.Index & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Extract And Move Method public writeTo(out StreamOutput) : void extracted from public writeTo(out StreamOutput) : void in class org.elasticsearch.indices.recovery.RecoveryState & moved to class org.elasticsearch.indices.recovery.RecoveryState.Timer",
//			"Merge Method public performRecoveryPrepareForTranslog() : void merged to public prepareForTranslogRecovery() : void in class org.elasticsearch.index.shard.IndexShard",
//			"Merge Method package serializeDeserialize() : void merged to public serializeDeserialize() : T in class org.elasticsearch.indices.recovery.RecoveryStateTest.Streamer",
//			"Merge Method public performRecoveryFinalization(withFlush boolean) : void merged to public finalizeRecovery() : void in class org.elasticsearch.index.shard.IndexShard",
			"Extract Method public clusterService(node String) : ClusterService extracted from public clusterService() : ClusterService in class org.elasticsearch.test.InternalTestCluster",
			"Rename Class org.elasticsearch.indices.recovery.ShardRecoveryHandler renamed to org.elasticsearch.indices.recovery.RecoverySourceHandler",
			"Rename Class org.elasticsearch.indices.recovery.SharedFSRecoveryHandler renamed to org.elasticsearch.indices.recovery.SharedFSRecoverySourceHandler"
		);
		
		test.assertExpectations();
	}

}
