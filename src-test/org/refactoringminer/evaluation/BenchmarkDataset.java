package org.refactoringminer.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.utils.RefactoringSet;

public class BenchmarkDataset {

    List<RefactoringSet> data = new ArrayList<>();

    public BenchmarkDataset() {
        at("https://github.com/linkedin/rest.li.git", "54fa890")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.HttpClientFactory.buildAcceptEncodingSchemaNames()", "com.linkedin.r2.transport.http.client.HttpClientFactory.buildAcceptEncodingSchemas()")
            .add(RefactoringType.MOVE_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.testEncodingGeneration(EncodingType[],String)", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.testEncodingGeneration(EncodingType[],String)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.HttpClientFactory.getCompressionConfig(String,String)", "com.linkedin.r2.transport.http.client.HttpClientFactory.getRequestCompressionConfig(String,EncodingType)")
            .add(RefactoringType.EXTRACT_OPERATION, "com.linkedin.r2.filter.compression.ClientCompressionFilter.onRestRequest(RestRequest,RequestContext,Map,NextFilter)", "com.linkedin.r2.filter.compression.ClientCompressionFilter.addResponseCompressionHeaders(CompressionOption,RestRequest)")
            .add(RefactoringType.MOVE_OPERATION, "com.linkedin.r2.filter.CompressionConfig.shouldCompressRequest(int,CompressionOption)", "com.linkedin.r2.filter.compression.ClientCompressionFilter.shouldCompressRequest(int,CompressionOption)")
            .add(RefactoringType.MOVE_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.contentEncodingGeneratorDataProvider()", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.contentEncodingGeneratorDataProvider()")
            .add(RefactoringType.INLINE_OPERATION, "com.linkedin.restli.examples.RestLiIntTestServer.createServer(Engine,int,String,boolean,int,List,List)", "com.linkedin.restli.examples.RestLiIntTestServer.createServer(Engine,int,String,boolean,int)")
            .add(RefactoringType.INLINE_OPERATION, "com.linkedin.restli.examples.RestLiIntTestServer.createServer(Engine,int,String,boolean,int,List,List)", "com.linkedin.restli.examples.RestLiIntegrationTest.init(List,List)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.filter.compression.TestClientCompressionFilter.provideRequestData()", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.provideRequestCompressionData()")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.filter.compression.TestClientCompressionFilter.testCompressionOperations(String,String[],boolean)", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.testResponseCompressionRules(CompressionConfig,CompressionOption,String,String)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.filter.compression.ClientCompressionFilter.shouldCompressResponse(String)", "com.linkedin.r2.filter.compression.ClientCompressionFilter.shouldCompressResponseForOperation(String)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.TestHttpClientFactory.testGetCompressionConfig(String,int,CompressionConfig)", "com.linkedin.r2.transport.http.client.TestHttpClientFactory.testGetRequestCompressionConfig(String,int,CompressionConfig)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.HttpClientFactory.getRequestContentEncodingName(List)", "com.linkedin.r2.transport.http.client.HttpClientFactory.getRequestContentEncoding(List)");
        at("https://github.com/droolsjbpm/jbpm.git", "3815f29")
            .add(RefactoringType.EXTRACT_SUPERCLASS, "org.jbpm.process.audit.JPAAuditLogService", "org.jbpm.process.audit.JPAService")
            .add(RefactoringType.MOVE_OPERATION, "org.jbpm.process.audit.JPAAuditLogService.convertListToInterfaceList(List,Class)", "org.jbpm.query.jpa.impl.QueryCriteriaUtil.convertListToInterfaceList(List,Class)")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.query.jpa.data.QueryWhere.startGroup()", "org.jbpm.query.jpa.data.QueryWhere.newGroup()")
            .add(RefactoringType.RENAME_CLASS, "org.jbpm.services.task.commands.TaskQueryDataCommand", "org.jbpm.services.task.commands.TaskQueryWhereCommand")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.initiator(String[])", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.createdBy(String[])")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.query.jpa.data.QueryWhere.addAppropriateParam(String,T[])", "org.jbpm.query.jpa.data.QueryWhere.addParameter(String,T[])")
            .add(RefactoringType.MOVE_OPERATION, "org.jbpm.services.task.commands.TaskQueryDataCommand.execute(Context)", "org.jbpm.services.task.commands.TaskQueryWhereCommand.execute(Context)")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.taskOwner(String[])", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.actualOwner(String[])")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.orderBy(OrderBy)", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.getOrderByListId(OrderBy)")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.buildQuery()", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.build()")
            .add(RefactoringType.RENAME_CLASS, "org.jbpm.query.jpa.data.QueryWhere.ParameterType", "org.jbpm.query.jpa.data.QueryWhere.QueryCriteriaType");
            
        at("https://github.com/gradle/gradle.git", "44aab62")
            .add(RefactoringType.EXTRACT_INTERFACE, "org.gradle.api.internal.file.FileResolver", "org.gradle.internal.file.RelativeFilePathResolver");
        at("https://github.com/jenkinsci/workflow-plugin.git", "d0e374c")
            .add(RefactoringType.MOVE_ATTRIBUTE, "org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory.SCRIPT", "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject.SCRIPT")
            .add(RefactoringType.INLINE_OPERATION, "org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory.setBranch(BranchJobProperty,Branch,WorkflowJob)", "org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory.setBranch(WorkflowJob,Branch)");
        at("https://github.com/spring-projects/spring-roo.git", "0bb4cca")
            .add(RefactoringType.PULL_UP_ATTRIBUTE, "org.springframework.roo.classpath.details.MethodMetadataBuilder.genericDefinition", "org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder.genericDefinition")
            .add(RefactoringType.PULL_UP_OPERATION, "org.springframework.roo.classpath.details.MethodMetadataBuilder.getGenericDefinition()", "org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder.getGenericDefinition()")
            .add(RefactoringType.PULL_UP_OPERATION, "org.springframework.roo.classpath.details.MethodMetadataBuilder.setGenericDefinition(String)", "org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder.setGenericDefinition(String)");
        at("https://github.com/BuildCraft/BuildCraft.git", "a5cdd8c")
            .add(RefactoringType.PUSH_DOWN_OPERATION, "buildcraft.api.robots.ResourceId.equals(Object)", "buildcraft.api.robots.ResourceIdRequest.equals(Object)")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.side", "buildcraft.api.robots.ResourceIdRequest.side")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.side", "buildcraft.api.robots.ResourceIdBlock.side")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.robotics.TileRequester.provideItemsForRequest(int,ItemStack)", "buildcraft.robotics.TileRequester.offerItem(int,ItemStack)")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.index", "buildcraft.api.robots.ResourceIdRequest.index")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.index", "buildcraft.api.robots.ResourceIdBlock.index")
            .add(RefactoringType.EXTRACT_OPERATION, "buildcraft.robotics.ai.AIRobotSearchStackRequest.getOrderFromRequestingStation(DockingStation,boolean)", "buildcraft.robotics.ai.AIRobotSearchStackRequest.getAvailableRequests(DockingStation)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.robotics.TileRequester.getNumberOfRequests()", "buildcraft.robotics.TileRequester.getRequestsCount()")
            .add(RefactoringType.PUSH_DOWN_OPERATION, "buildcraft.api.robots.ResourceId.equals(Object)", "buildcraft.api.robots.ResourceIdBlock.equals(Object)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.builders.TileBuilder.getAvailableRequest(int)", "buildcraft.builders.TileBuilder.getRequest(int)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.builders.TileBuilder.provideItemsForRequest(int,ItemStack)", "buildcraft.builders.TileBuilder.offerItem(int,ItemStack)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.builders.TileBuilder.getNumberOfRequests()", "buildcraft.builders.TileBuilder.getRequestsCount()");
        at("https://github.com/droolsjbpm/drools.git", "1bf2875")
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.phreak.PhreakTimerNode.TimerAction.requiresImmediateFlushingIfNotFiring()", "org.drools.core.phreak.PhreakTimerNode.TimerAction.requiresImmediateFlushing()")
            .add(RefactoringType.MOVE_OPERATION, "org.drools.reteoo.common.ReteAgenda.notifyHalt()", "org.drools.core.phreak.SynchronizedBypassPropagationList.notifyHalt()")
            .add(RefactoringType.MOVE_OPERATION, "org.drools.core.common.DefaultAgenda.notifyHalt()", "org.drools.core.phreak.SynchronizedBypassPropagationList.notifyHalt()")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.phreak.SynchronizedPropagationList.addEntry(PropagationEntry)", "org.drools.core.phreak.SynchronizedPropagationList.internalAddEntry(PropagationEntry)")
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.phreak.PropagationEntry.AbstractPropagationEntry.requiresImmediateFlushingIfNotFiring()", "org.drools.core.phreak.PropagationEntry.AbstractPropagationEntry.requiresImmediateFlushing()")
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.phreak.RuleExecutor.isHighestSalience(RuleAgendaItem)", "org.drools.core.phreak.RuleExecutor.isHigherSalience(RuleAgendaItem)")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.common.AgendaGroupQueueImpl.(String,InternalKnowledgeBase)", "org.drools.core.common.AgendaGroupQueueImpl.initPriorityQueue(InternalKnowledgeBase)")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "org.drools.core.impl.StatefulKnowledgeSessionImpl.evaluatingActionQueue", "org.drools.reteoo.common.ReteWorkingMemory.evaluatingActionQueue");
        at("https://github.com/jersey/jersey.git", "d94ca2b")
            .add(RefactoringType.MOVE_CLASS, "org.glassfish.jersey.client.HttpUrlConnector", "org.glassfish.jersey.client.internal.HttpUrlConnector")
            .add(RefactoringType.EXTRACT_OPERATION, "org.glassfish.jersey.client.HttpUrlConnector._apply(ClientRequest)", "org.glassfish.jersey.client.internal.HttpUrlConnector.secureConnection(Client,HttpURLConnection)");
        at("https://github.com/undertow-io/undertow.git", "d5b2bb8")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.coerceToType(String,Token,Class,ExchangeAttributeParser)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.coerceToType(String,Token,Class,ExchangeAttributeParser)")
            .add(RefactoringType.EXTRACT_OPERATION, "io.undertow.predicate.PredicatesHandler.addPredicatedHandler(Predicate,HandlerWrapper)", "io.undertow.predicate.PredicatesHandler.addPredicatedHandler(Predicate,HandlerWrapper,HandlerWrapper)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.collapseOutput(Object,Deque)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.collapseOutput(Node,Deque)")
            .add(RefactoringType.MOVE_CLASS, "io.undertow.util.PredicateTokeniser.Token", "io.undertow.server.handlers.builder.PredicatedHandlersParser.Token")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.isSpecialChar(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isSpecialChar(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.util.PredicateTokeniser.tokenize(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.tokenize(String)")
            .add(RefactoringType.EXTRACT_OPERATION, "io.undertow.predicate.PredicateParser.parse(String,ClassLoader)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.parsePredicate(String,ClassLoader)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.readArrayType(String,Deque,Token,PredicateBuilder,ExchangeAttributeParser,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.readArrayType(String,Deque,String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.coerceToType(String,Token,Class,ExchangeAttributeParser)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.coerceToType(String,Token,Class,ExchangeAttributeParser)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.parse(String,Deque,Map,ExchangeAttributeParser)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.parse(String,Deque,boolean)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.isSpecialChar(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isSpecialChar(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.isOperator(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isOperator(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.handleSingleArrayValue(String,PredicateBuilder,Deque,Token,ExchangeAttributeParser,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.handleSingleArrayValue(String,Token,Deque,String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.precedence(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.precedence(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.handleSingleArrayValue(String,HandlerBuilder,Deque,Token,ExchangeAttributeParser,String,Token)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.handleSingleArrayValue(String,Token,Deque,String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.readArrayType(String,Deque,Token,PredicateBuilder,ExchangeAttributeParser,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.readArrayType(String,String,ArrayNode,ExchangeAttributeParser,Class)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.isOperator(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isOperator(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.util.PredicateTokeniser.error(String,int,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.error(String,int,String)")
            .add(RefactoringType.EXTRACT_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.parse(String,ClassLoader)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.parseHandler(String,ClassLoader)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.tokenize(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.tokenize(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.precedence(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.precedence(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.readArrayType(String,Deque,Token,HandlerBuilder,ExchangeAttributeParser,String,Token)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.readArrayType(String,String,ArrayNode,ExchangeAttributeParser,Class)");
        at("https://github.com/kuujo/copycat.git", "19a49f8")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.applyCommits(long)", "net.kuujo.copycat.raft.state.PassiveState.applyCommits(long)")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.doCheckPreviousEntry(AppendRequest)", "net.kuujo.copycat.raft.state.PassiveState.doCheckPreviousEntry(AppendRequest)")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.doAppendEntries(AppendRequest)", "net.kuujo.copycat.raft.state.PassiveState.doAppendEntries(AppendRequest)")
            .add(RefactoringType.PULL_UP_ATTRIBUTE, "net.kuujo.copycat.raft.state.ActiveState.transition", "net.kuujo.copycat.raft.state.PassiveState.transition")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.handleAppend(AppendRequest)", "net.kuujo.copycat.raft.state.PassiveState.handleAppend(AppendRequest)")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.applyIndex(long)", "net.kuujo.copycat.raft.state.PassiveState.applyIndex(long)");
    }

    public RefactoringSet[] all() {
        return data.toArray(new RefactoringSet[data.size()]);
    }

    private RefactoringSet at(String cloneUrl, String commit) {
        RefactoringSet rs = new RefactoringSet(cloneUrl, commit);
        data.add(rs);
        return rs;
    }

    public void printSourceCode() {
        for (RefactoringSet set : data) {
            set.printSourceCode(System.out);
        }
    }

}
