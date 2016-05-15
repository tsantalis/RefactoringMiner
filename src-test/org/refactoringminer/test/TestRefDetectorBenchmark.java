package org.refactoringminer.test;

import org.junit.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class TestRefDetectorBenchmark {

	@Test
	public void test() throws Exception {
//	  TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");
		  TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), "tmp").verbose();

		  test.project("https://github.com/linkedin/rest.li.git", "master") 
      .atCommit("54fa890a6af4ccf564fb481d3e1b6ad4d084de9e").contains(
        "Extract Method public addResponseCompressionHeaders(responseCompressionOverride CompressionOption, req RestRequest) : RestRequest extracted from public onRestRequest(req RestRequest, requestContext RequestContext, wireAttrs Map<String,String>, nextFilter NextFilter<RestRequest,RestResponse>) : void in class com.linkedin.r2.filter.compression.ClientCompressionFilter",
        "Move Method public testEncodingGeneration(encoding EncodingType[], acceptEncoding String) : void from class com.linkedin.restli.examples.TestCompressionServer to public testEncodingGeneration(encoding EncodingType[], acceptEncoding String) : void from class com.linkedin.r2.filter.compression.TestClientCompressionFilter",
        "Move Method public contentEncodingGeneratorDataProvider() : Object[][] from class com.linkedin.restli.examples.TestCompressionServer to public contentEncodingGeneratorDataProvider() : Object[][] from class com.linkedin.r2.filter.compression.TestClientCompressionFilter",
        "Move Method public shouldCompressRequest(entityLength int, requestCompressionOverride CompressionOption) : boolean from class com.linkedin.r2.filter.CompressionConfig to private shouldCompressRequest(entityLength int, requestCompressionOverride CompressionOption) : boolean from class com.linkedin.r2.filter.compression.ClientCompressionFilter");

		  test.project("https://github.com/droolsjbpm/jbpm.git", "master") 
      .atCommit("3815f293ba9338f423315d93a373608c95002b15").contains(
        "Extract Superclass org.jbpm.process.audit.JPAService from classes [org.jbpm.process.audit.JPAAuditLogService]")
      .atCommit("3815f293ba9338f423315d93a373608c95002b15").notContains(
        "Extract Method private getOrderByListId(field OrderBy) : String extracted from public language(language String) : TaskQueryBuilder in class org.jbpm.services.task.impl.TaskQueryBuilderImpl",
        "Inline Method private resetGroup() : void inlined to public clear() : void in class org.jbpm.query.jpa.data.QueryWhere",
        "Move Method private joinTransaction(em EntityManager) : Object from class org.jbpm.process.audit.JPAAuditLogService to protected joinTransaction(em EntityManager) : Object from class org.jbpm.executor.impl.jpa.ExecutorJPAAuditService",
        "Move Method private getEntityManager() : EntityManager from class org.jbpm.process.audit.JPAAuditLogService to protected getEntityManager() : EntityManager from class org.jbpm.executor.impl.jpa.ExecutorJPAAuditService");

		  test.project("https://github.com/gradle/gradle.git", "master") 
      .atCommit("44aab6242f8c93059612c953af950eb1870e0774").contains(
        "Extract Interface org.gradle.internal.file.RelativeFilePathResolver from classes [org.gradle.api.internal.file.FileResolver]");

		  test.project("https://github.com/jenkinsci/workflow-plugin.git", "master") 
      .atCommit("d0e374ce8ecb687b4dc046d1edea9e52da17706f").contains(
        "Move Attribute package SCRIPT : String from class org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory to class org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject",
        "Inline Method private setBranch(property BranchJobProperty, branch Branch, project WorkflowJob) : void inlined to public setBranch(project WorkflowJob, branch Branch) : WorkflowJob in class org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory");

		  test.project("https://github.com/spring-projects/spring-roo.git", "master") 
      .atCommit("0bb4cca1105fc6eb86e7c4b75bfff3dbbd55f0c8").contains(
        "Pull Up Method public setGenericDefinition(definition String) : void from class org.springframework.roo.classpath.details.MethodMetadataBuilder to public setGenericDefinition(genericDefinition String) : void from class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder",
        "Pull Up Method public getGenericDefinition() : String from class org.springframework.roo.classpath.details.MethodMetadataBuilder to public getGenericDefinition() : String from class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder",
        "Pull Up Attribute private genericDefinition : String from class org.springframework.roo.classpath.details.MethodMetadataBuilder to class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder");

		  test.project("https://github.com/BuildCraft/BuildCraft.git", "master") 
      .atCommit("a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0").contains(
        "Push Down Attribute private side : ForgeDirection from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdRequest",
        "Push Down Attribute private index : BlockIndex from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdRequest",
        "Push Down Attribute public side : ForgeDirection from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdBlock",
        "Push Down Attribute public index : BlockIndex from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdBlock",
        "Extract Method private getAvailableRequests(station DockingStation) : Collection<StackRequest> extracted from private getOrderFromRequestingStation(station DockingStation, take boolean) : StackRequest in class buildcraft.robotics.ai.AIRobotSearchStackRequest",
        "Push Down Method public equals(obj Object) : boolean from class buildcraft.api.robots.ResourceId to public equals(obj Object) : boolean from class buildcraft.api.robots.ResourceIdBlock")
      .atCommit("a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0").notContains(
        "Extract Method private releaseCurrentRequest() : void extracted from public delegateAIEnded(ai AIRobot) : void in class buildcraft.robotics.boards.BoardRobotDelivery");

		  test.project("https://github.com/droolsjbpm/drools.git", "master") 
      .atCommit("1bf2875e9d73e2d1cd3b58200d5300485f890ff5").contains(
        "Push Down Attribute private evaluatingActionQueue : AtomicBoolean from class org.drools.core.impl.StatefulKnowledgeSessionImpl to class org.drools.reteoo.common.ReteWorkingMemory",
        "Move Method public notifyHalt() : void from class org.drools.reteoo.common.ReteAgenda to public notifyHalt() : void from class org.drools.core.phreak.SynchronizedBypassPropagationList",
        "Move Method public notifyHalt() : void from class org.drools.core.common.DefaultAgenda to public notifyHalt() : void from class org.drools.core.phreak.SynchronizedBypassPropagationList",
        "Extract Method protected initPriorityQueue(kBase InternalKnowledgeBase) : BinaryHeapQueue extracted from public AgendaGroupQueueImpl(name String, kBase InternalKnowledgeBase) in class org.drools.core.common.AgendaGroupQueueImpl",
        "Extract Method private internalAddEntry(entry PropagationEntry) : void extracted from public addEntry(entry PropagationEntry) : void in class org.drools.core.phreak.SynchronizedPropagationList")
      .atCommit("1bf2875e9d73e2d1cd3b58200d5300485f890ff5").notContains(
        "Extract Method private fire(wm InternalWorkingMemory, filter AgendaFilter, fireCount int, fireLimit int, agenda InternalAgenda) : int extracted from public evaluateNetworkAndFire(wm InternalWorkingMemory, filter AgendaFilter, fireCount int, fireLimit int) : int in class org.drools.core.phreak.RuleExecutor",
        "Extract Method private haltRuleFiring(fireCount int, fireLimit int, localFireCount int, agenda InternalAgenda) : boolean extracted from private fire(wm InternalWorkingMemory, filter AgendaFilter, fireCount int, fireLimit int, outerStack LinkedList<StackEntry>, agenda InternalAgenda) : int in class org.drools.core.phreak.RuleExecutor",
        "Extract Method private evalQueryNode(liaNode LeftInputAdapterNode, pmem PathMemory, node NetworkNode, bit long, nodeMem Memory, smems SegmentMemory[], smemIndex int, trgTuples LeftTupleSets, wm InternalWorkingMemory, stack LinkedList<StackEntry>, srcTuples LeftTupleSets, sink LeftTupleSinkNode, stagedLeftTuples LeftTupleSets) : boolean extracted from public evaluateNetwork(pmem PathMemory, outerStack LinkedList<StackEntry>, executor RuleExecutor, wm InternalWorkingMemory) : void in class org.drools.core.phreak.RuleNetworkEvaluator",
        "Extract Method public flush(workingMemory InternalWorkingMemory, currentHead PropagationEntry) : void extracted from private internalFlush() : void in class org.drools.core.phreak.SynchronizedPropagationList",
        "Extract Method public takeAll() : PropagationEntry extracted from private internalFlush() : void in class org.drools.core.phreak.SynchronizedPropagationList");

		  test.project("https://github.com/jersey/jersey.git", "master") 
      .atCommit("d94ca2b27c9e8a5fa9fe19483d58d2f2ef024606").contains(
        "Move Class org.glassfish.jersey.client.HttpUrlConnector moved to org.glassfish.jersey.client.internal.HttpUrlConnector");

		  test.project("https://github.com/cwensel/cascading.git", "master") 
      .atCommit("f9d3171f5020da5c359cdda28ef05172e858c464").contains(
        "Move Method private getPrefix() : String from class cascading.stats.tez.TezNodeStats to private getPrefix() : String from class cascading.stats.CascadingStats",
        "Move Method protected logWarn(message String, arguments Object[]) : void from class cascading.stats.tez.TezNodeStats to protected logWarn(message String, arguments Object[]) : void from class cascading.stats.CascadingStats",
        "Move Method protected logDebug(message String, arguments Object[]) : void from class cascading.stats.tez.TezNodeStats to protected logDebug(message String, arguments Object[]) : void from class cascading.stats.CascadingStats",
        "Move Method protected logInfo(message String, arguments Object[]) : void from class cascading.stats.tez.TezNodeStats to protected logInfo(message String, arguments Object[]) : void from class cascading.stats.CascadingStats",
        "Move Attribute private prefixID : String from class cascading.stats.tez.TezNodeStats to class cascading.stats.CascadingStats")
      .atCommit("f9d3171f5020da5c359cdda28ef05172e858c464").notContains(
        "Inline Method private getJobStatusClient() : RunningJob inlined to protected captureChildDetailInternal() : boolean in class cascading.stats.hadoop.HadoopNodeStats");

		  test.project("https://github.com/crate/crate.git", "master") 
      .atCommit("72b5348307d86b1a118e546c24d97f1ac1895bdb").contains(
        "Pull Up Method public downstreamExecutionNodeId(downstreamExecutionNodeId int) : void from class io.crate.planner.node.dql.MergeNode to public downstreamExecutionNodeId(downstreamExecutionNodeId int) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Method public downstreamNodes(nodes Set<String>) : void from class io.crate.planner.node.dql.join.NestedLoopNode to public downstreamNodes(downStreamNodes Set<String>) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Method public downstreamNodes(nodes Set<String>) : void from class io.crate.planner.node.dql.MergeNode to public downstreamNodes(downStreamNodes Set<String>) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Method public downstreamNodes(downStreamNodes List<String>) : void from class io.crate.planner.node.dql.CollectNode to public downstreamNodes(downStreamNodes List<String>) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Attribute private downstreamExecutionNodeId : int from class io.crate.planner.node.dql.join.NestedLoopNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Attribute private downstreamExecutionNodeId : int from class io.crate.planner.node.dql.MergeNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Attribute private downstreamExecutionNodeId : int from class io.crate.planner.node.dql.CollectNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Attribute private downstreamNodes : List<String> from class io.crate.planner.node.dql.join.NestedLoopNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Attribute private downstreamNodes : List<String> from class io.crate.planner.node.dql.MergeNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Pull Up Attribute private downstreamNodes : List<String> from class io.crate.planner.node.dql.CollectNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
        "Move Attribute private rightMergeNode : MergeNode from class io.crate.planner.node.dql.join.NestedLoop to class io.crate.planner.node.dql.join.NestedLoopNode",
        "Move Attribute private leftMergeNode : MergeNode from class io.crate.planner.node.dql.join.NestedLoop to class io.crate.planner.node.dql.join.NestedLoopNode");
	    
		  test.assertExpectations();
	}
	
}
