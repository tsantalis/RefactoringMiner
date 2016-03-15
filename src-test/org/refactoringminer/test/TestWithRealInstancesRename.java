package org.refactoringminer.test;

import org.junit.Test;

public class TestWithRealInstancesRename {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();

		// Thiago
		test.project("https://github.com/k9mail/k-9.git", "master").atCommit("186379cbd05bcd5b22ea912ec4846d6bb44298bc").contains("Rename Method protected isReadyForPullUp() : boolean renamed to protected isReadyForPullEnd() : boolean in class com.handmark.pulltorefresh.library.PullToRefreshWebView");
		test.project("https://github.com/junit-team/junit.git", "master").atCommit("5d9e0022d3f6db1367d530579de6332b9c8802e3").contains("Rename Method package c() : int renamed to package gamma() : int in class org.junit.internal.MethodSorterTest.Dummy");
		test.project("https://github.com/junit-team/junit.git", "master").atCommit("1fcd990f60518c941a44bae2596392021614a20b").contains("Rename Method public childBlock(method FrameworkMethod) : Statement renamed to public methodBlock(method FrameworkMethod) : Statement in class org.junit.tests.experimental.theories.extendingwithstubs.StubbedTheories");
		test.project("https://github.com/voldemort/voldemort.git", "master").atCommit("d06fcec37fb3f3d45ffbd3fe15eafce77ba64069").notContains("Rename Method public addAllStealMasterPartitions(values Iterable<? extends java.lang.Integer>) : Builder renamed to public clearRebalancePartitionInfo() : Builder in class voldemort.client.protocol.pb.InitiateRebalanceNodeRequest.Builder");
		test.project("https://github.com/thinkaurelius/titan.git", "titan05").atCommit("c7712a21c768cdd9893d0f6b03b12fef720e5a2b").contains("Rename Method public writeObjectData(buffer WriteBuffer, attribute boolean[]) : void renamed to public write(buffer WriteBuffer, attribute boolean[]) : void in class com.thinkaurelius.titan.graphdb.database.serialize.attribute.BooleanArraySerializer");
		  // Esse parece estar errado
		  test.project("https://github.com/voldemort/voldemort.git", "master").atCommit("d24f295f897528431bcdf914e9b8919987252298").contains("Rename Method public setStoreToRODir(index int, value ROStoreVersionDirMap) : Builder renamed to public setStealerRoStoreToDir(index int, value ROStoreVersionDirMap) : Builder in class voldemort.client.protocol.pb.InitiateRebalanceNodeRequest.Builder");
		test.project("https://github.com/junit-team/junit.git", "master").atCommit("0a2601045f101930d0ca7f31c53b416fafbbe7a7").contains("Rename Method public micros(nanos long) : long renamed to public toMicros(nanos long) : long in class org.junit.rules.TimeWatcher");
		  // Método apenas com um return statement
		  test.project("https://github.com/loopj/android-async-http.git", "master").atCommit("e8b4797212cb58a626c2264bc3e35fa35b0f3027").notContains("Rename Method public getIp() : String renamed to public getAcceptLanguage() : String in class com.loopj.android.http.sample.util.SampleJSON");
		  // É realmente um rename, mas com um único statement
		  test.project("https://github.com/netty/netty.git", "master").atCommit("392623749eaadbbb4169103480971fac0b635784").contains("Rename Method protected firstOut() : ChannelBufferHolder<Object> renamed to protected newOutboundBuffer() : ChannelBufferHolder<?> in class io.netty.channel.socket.oio.OioServerSocketChannel");
		test.project("https://github.com/voldemort/voldemort.git", "master").atCommit("d06fcec37fb3f3d45ffbd3fe15eafce77ba64069").notContains("Rename Method public addDeletePartitions(value int) : Builder renamed to public clearRebalancePartitionInfo() : Builder in class voldemort.client.protocol.pb.InitiateRebalanceNodeRequest.Builder");

		// Renato
		test.project("https://github.com/netty/netty.git", "master").atCommit("5e93d206ffeb637f9b4885643860ff63a8412dc6").contains("Rename Method public canHandleInbound() : boolean renamed to public hasInboundByteBuffer() : boolean in class io.netty.channel.DefaultChannelHandlerContext");
		test.project("https://github.com/netty/netty.git", "master").atCommit("fdb66b629403a5bfca1fc6c14f8e743cdbe08898").contains("Rename Method public persistValue(ID int) : boolean renamed to public isPersistValue(ID int) : boolean in class io.netty.handler.codec.spdy.DefaultSpdySettingsFrame");
		test.project("https://github.com/netty/netty.git", "master").atCommit("c8fa42beaf29fe0b65a8fbb8a031f9db46e14c10").contains("Rename Method public wrapMedium(values int) : ChannelBuffer renamed to public copyMedium(values int) : ChannelBuffer in class io.netty.buffer.ChannelBuffers");
		test.project("https://github.com/clojure/clojure.git", "master").atCommit("cdd429f0d51b754ed0d2f4ab4cd9b90d320a3c0e").contains("Rename Method public get(i int) : Object renamed to public nth(i int) : Object in class clojure.lang.PersistentArrayList");
		test.project("https://github.com/netty/netty.git", "master").atCommit("c8fa42beaf29fe0b65a8fbb8a031f9db46e14c10").contains("Rename Method public wrapBoolean(value boolean) : ChannelBuffer renamed to public copyBoolean(value boolean) : ChannelBuffer in class io.netty.buffer.ChannelBuffers");
		test.project("https://github.com/thinkaurelius/titan.git", "titan05").atCommit("923dbcd39c67bda3f91580a6ceab68f64fb554de").contains("Rename Method private getVertexIDType(vertexid long) : VertexIDType renamed to private getUserVertexIDType(vertexid long) : VertexIDType in class com.thinkaurelius.titan.graphdb.idmanagement.IDManager");
		test.project("https://github.com/netty/netty.git", "master").atCommit("9da01417b25bca25af70f27246ab961166cd85bb").notContains("Rename Method public count() : int renamed to public isFirst() : boolean in class io.netty.handler.timeout.IdleStateEvent");
		test.project("https://github.com/voldemort/voldemort.git", "master").atCommit("f085f188403b8083b0b05fbcf3d2b329bac2ace1").notContains("Rename Method public RebalanceClient(cluster Cluster, config RebalanceClientConfig) renamed to public RebalanceClient(bootstrapUrl String, maxParallelRebalancing int, config AdminClientConfig) in class voldemort.client.rebalance.RebalanceClient");
		test.project("https://github.com/thinkaurelius/titan.git", "titan05").atCommit("6ef5e7d8b390cfc63a8dedc0dbbccb67517b7242").contains("Rename Method public getStartTime() : long renamed to public getStartTimeMicro() : long in class com.thinkaurelius.titan.diskstorage.log.ReadMarker");
		test.project("https://github.com/junit-team/junit.git", "master").atCommit("04f4f3197084ad9ad7c050ce1a40d6f4421662f4").contains("Rename Method protected useReoadingTestSuiteLoader() : boolean renamed to protected useReloadingTestSuiteLoader() : boolean in class junit.runner.BaseTestRunner");
		
		test.assertExpectations();
	}

}
