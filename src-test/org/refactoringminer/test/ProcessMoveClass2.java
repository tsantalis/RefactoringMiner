package org.refactoringminer.test;

import gr.uom.java.xmi.diff.RefactoringType;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.junit.Test;

import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;

public class ProcessMoveClass2 {

    private final String tempDir = "c:/Users/danilofs/tmp";

    @Test
    public void testOne() throws Exception {
      
////        missing object
      processTp(1149047, "https://github.com/spring-projects/spring-data-neo4j.git", "ef2a0d63393484975854fc08ad0fd3abc7dd76b0", 
        "Move Class org.springframework.data.neo4j.examples.friends.Person moved to org.springframework.data.neo4j.examples.friends.domain.Person",
        "Move Class org.springframework.data.neo4j.examples.friends.Friendship moved to org.springframework.data.neo4j.examples.friends.domain.Friendship",
        "Move Class org.springframework.data.neo4j.examples.friends.FriendContext moved to org.springframework.data.neo4j.examples.friends.context.FriendContext");
    
    }
    
    @Test
    public void testAll() throws Exception {
        
        processTp(1101296, "https://github.com/SonarSource/sonarqube.git", "4a2247c24efee48de53ca07302b6810ab7205621", 
            "Move Class org.sonar.server.custommeasure.ws.DeleteAction moved to org.sonar.server.measure.custom.ws.DeleteAction",
            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsModule moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsModule",
            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsAction moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsAction",
            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWs moved to org.sonar.server.measure.custom.ws.CustomMeasuresWs",
            "Move Class org.sonar.server.custommeasure.persistence.CustomMeasureDao moved to org.sonar.server.measure.custom.persistence.CustomMeasureDao",
            "Move Class org.sonar.server.custommeasure.ws.DeleteActionTest moved to org.sonar.server.measure.custom.ws.DeleteActionTest",
            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsTest moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsTest",
            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsModuleTest moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsModuleTest",
            "Move Class org.sonar.server.custommeasure.persistence.CustomMeasureTesting moved to org.sonar.server.measure.custom.persistence.CustomMeasureTesting",
            "Move Class org.sonar.server.custommeasure.persistence.CustomMeasureDaoTest moved to org.sonar.server.measure.custom.persistence.CustomMeasureDaoTest");

          processTp(1106365, "https://github.com/apache/cassandra.git", "446e2537895c15b404a74107069a12f3fc404b15", 
            "Move Class org.apache.cassandra.hadoop.BulkRecordWriter.NullOutputHandler moved to org.apache.cassandra.hadoop.cql3.CqlBulkRecordWriter.NullOutputHandler",
            "Move Class org.apache.cassandra.hadoop.AbstractColumnFamilyInputFormat.SplitCallable moved to org.apache.cassandra.hadoop.cql3.CqlInputFormat.SplitCallable");

          processTp(1107905, "https://github.com/elastic/elasticsearch.git", "f77804dad35c13d9ff96456e85737883cf7ddd99", 
            "Move Class org.elasticsearch.index.merge.policy.VersionFieldUpgraderTest moved to org.elasticsearch.index.shard.VersionFieldUpgraderTest",
            "Move Class org.elasticsearch.index.merge.policy.VersionFieldUpgrader moved to org.elasticsearch.index.shard.VersionFieldUpgrader",
            "Move Class org.elasticsearch.index.merge.policy.FilterDocValuesProducer moved to org.elasticsearch.index.shard.FilterDocValuesProducer",
            "Move Class org.elasticsearch.index.merge.policy.ElasticsearchMergePolicy moved to org.elasticsearch.index.shard.ElasticsearchMergePolicy");

          processTp(1110272, "https://github.com/JetBrains/intellij-community.git", "3972b9b3d4e03bdb5e62dfa663e3e0a1871e3c9f", 
            "Move Class com.intellij.psi.codeStyle.autodetect.NewLineBlocksIterator moved to com.intellij.psi.formatter.common.NewLineBlocksIterator");

          processTp(1112702, "https://github.com/JetBrains/intellij-community.git", "5f18bed8da4dda4fa516907ecbbe28f712e944f7", 
            "Move Class com.intellij.util.ui.components.JBPanel moved to com.intellij.ui.components.JBPanel");

          processTp(1116627, "https://github.com/hierynomus/sshj.git", "7c26ac669a4e17ca1d2319a5049a56424fd33104", 
            "Move Class nl.javadude.sshj.connection.channel.ChannelCloseEofTest moved to com.hierynomus.sshj.connection.channel.ChannelCloseEofTest");

          processTp(1117602, "https://github.com/bennidi/mbassador.git", "40e41d11d7847d660bba6691859b0506514bd0ac", 
            "Move Class net.engio.mbassy.ConditionalHandlers.ConditionalMessageListener moved to net.engio.mbassy.ConditionalHandlerTest.ConditionalMessageListener",
            "Move Class net.engio.mbassy.ConditionalHandlers.TestEvent moved to net.engio.mbassy.ConditionalHandlerTest.TestEvent");

          processTp(1118362, "https://github.com/hazelcast/hazelcast.git", "e66e49cd4a9dd8027204f712f780170a5c129f5b", 
            "Move Class com.hazelcast.spi.ServiceInfo moved to com.hazelcast.spi.impl.servicemanager.ServiceInfo");

          processTp(1121439, "https://github.com/opentripplanner/OpenTripPlanner.git", "e32f161fc023d1ee153c49df312ae10b06941465", 
            "Move Class org.opentripplanner.analyst.qbroker.User moved to org.opentripplanner.analyst.broker.User",
            "Move Class org.opentripplanner.analyst.qbroker.QueueType moved to org.opentripplanner.analyst.broker.QueueType",
            "Move Class org.opentripplanner.analyst.qbroker.QueuePath moved to org.opentripplanner.analyst.broker.QueuePath",
            "Move Class org.opentripplanner.analyst.qbroker.CircularList moved to org.opentripplanner.analyst.broker.CircularList",
            "Move Class org.opentripplanner.analyst.qbroker.BrokerMain moved to org.opentripplanner.analyst.broker.BrokerMain",
            "Move Class org.opentripplanner.analyst.qbroker.BrokerHttpHandler moved to org.opentripplanner.analyst.broker.BrokerHttpHandler");

          processTp(1120998, "https://github.com/brettwooldridge/HikariCP.git", "cd8c4d578a609bdd6395d3a8c49bfd19ed700dea", 
            "Move Class com.zaxxer.hikari.util.NanosecondClockSource moved to com.zaxxer.hikari.util.ClockSource.NanosecondClockSource",
            "Move Class com.zaxxer.hikari.util.MillisecondClockSource moved to com.zaxxer.hikari.util.ClockSource.MillisecondClockSource");

          processTp(1120836, "https://github.com/SlimeKnights/TinkersConstruct.git", "71820e573134be3fad3935035249cd77c4412f4e", 
            "Move Class tconstruct.library.modifiers.RecipeMatch moved to tconstruct.library.mantle.RecipeMatch");

          processTp(1122348, "https://github.com/mongodb/mongo-java-driver.git", "8c5a20d786e66ee4c4b0d743f0f80bf681c419be", 
            "Move Class com.mongodb.JsonPoweredTestHelper moved to util.JsonPoweredTestHelper");

          processTp(1123712, "https://github.com/facebook/buck.git", "84b7b3974ae8171a4de2f804eb94fcd1d6cd6647", 
            "Move Class com.facebook.buck.java.ReportGenerator moved to com.facebook.buck.java.coverage.ReportGenerator");

          processTp(1126248, "https://github.com/cbeust/testng.git", "b5cf7a0252c8b0465c4dbd906717f7a12e26e6f8", 
            "Move Class test.testng234.PolymorphicFailureTest moved to test.inheritance.testng234.PolymorphicFailureTest",
            "Move Class test.testng234.ParentTest moved to test.inheritance.testng234.ParentTest",
            "Move Class test.testng234.ChildTest moved to test.inheritance.testng234.ChildTest");

          processTp(1126459, "https://github.com/ratpack/ratpack.git", "2581441eda268c45306423dd4c515514d98a14a0", 
            "Move Class ratpack.jackson.JacksonModule moved to ratpack.jackson.guice.JacksonModule");

          processTp(1130125, "https://github.com/wordpress-mobile/WordPress-Android.git", "9dc3cbd59a20f03406f295a4a8f3b8676dbc939e", 
            "Move Class org.wordpress.android.ui.prefs.NotificationsSettingsFragment moved to org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment",
            "Move Class org.wordpress.android.ui.prefs.NotificationsSettingsActivity moved to org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity",
            "Move Class org.wordpress.android.ui.prefs.NotificationsPreference moved to org.wordpress.android.ui.prefs.notifications.NotificationsPreference");

          processTp(1132496, "https://github.com/jboss-developer/jboss-eap-quickstarts.git", "983e0e0e22ab5bd2c6ea44235518057ea45dcca9", 
            "Move Class org.jboss.as.quickstarts.poh5helloworld.HelloWorld moved to org.jboss.as.quickstarts.html5rest.HelloWorld",
            "Move Class org.jboss.as.quickstarts.poh5helloworld.HelloService moved to org.jboss.as.quickstarts.html5rest.HelloService");

          processTp(1134191, "https://github.com/gradle/gradle.git", "ba1da95200d080aef6251f13ced0ca67dff282be", 
            "Move Class org.gradle.tooling.tests.TestExecutionException moved to org.gradle.tooling.test.TestExecutionException");

          processTp(1133238, "https://github.com/google/truth.git", "1768840bf1e69892fd2a23776817f620edfed536", 
            "Move Class com.google.common.truth.ListTest.Bar moved to com.google.common.truth.IterableTest.Bar",
            "Move Class com.google.common.truth.ListTest.Foo moved to com.google.common.truth.IterableTest.Foo");

          processTp(1136374, "https://github.com/novoda/android-demos.git", "5cdabae35f0642e9fe243afe12e4c16b3378a150", 
            "Move Class com.novoda.Base64DecoderException moved to com.novoda.demo.encryption.Base64DecoderException",
            "Move Class com.novoda.Base64 moved to com.novoda.demo.encryption.Base64");

          processTp(1137397, "https://github.com/hazelcast/hazelcast.git", "69dd55c93fc99c5f7a1e2c21f10e671e311be49e", 
            "Move Attribute public UTF_8 : Charset from class com.hazelcast.client.impl.protocol.util.UnsafeBuffer to class com.hazelcast.nio.Bits",
            "Move Attribute public UTF_8 : Charset from class com.hazelcast.client.impl.protocol.util.SafeBuffer to class com.hazelcast.nio.Bits",
            "Move Class com.hazelcast.client.protocol.Int2ObjectHashMapTest moved to com.hazelcast.util.collection.Int2ObjectHashMapTest",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap moved to com.hazelcast.util.collection.Int2ObjectHashMap");

          processTp(1139380, "https://github.com/square/javapoet.git", "5a37c2aa596377cb4c9b6f916614407fd0a7d3db", 
            "Extract Superclass com.squareup.javapoet.AbstractTypesTest from classes [com.squareup.javapoet.TypesTest]",
            "Move Class com.squareup.javapoet.TypesTest.Parameterized moved to com.squareup.javapoet.AbstractTypesTest.Parameterized");

          processTp(1139443, "https://github.com/checkstyle/checkstyle.git", "febbc986cb25ed460ea601c0a68c7d2597f89ee4", 
            "Move Class com.google.checkstyle.test.chapter5naming.rule521packageNames.PackageNameInputBad moved to com.google.checkstyle.test.chapter5naming.rule521packageNamesCamelCase.PackageNameInputBad");

          processTp(1139097, "https://github.com/cgeo/cgeo.git", "7e7e4f54801af4e49ebddb934d0c6ff33a2c2160", 
            "Move Class cgeo.geocaching.connector.TerraCachingConnector moved to cgeo.geocaching.connector.tc.TerraCachingConnector");

          processTp(1140316, "https://github.com/aws/aws-sdk-java.git", "14593c6379445f260baeb5287f618758da6d9952", 
            "Move Class com.amazonaws.service.codecommit.model.transform.UpdateRepositoryNameRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.UpdateRepositoryNameRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.UpdateRepositoryDescriptionRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.UpdateRepositoryDescriptionRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.UpdateDefaultBranchRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.UpdateDefaultBranchRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryNameIdPairJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryNameIdPairJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryNameIdPairJsonMarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryNameIdPairJsonMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryMetadataJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryMetadataJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryMetadataJsonMarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryMetadataJsonMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.ListRepositoriesResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.ListRepositoriesResultJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.ListRepositoriesRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.ListRepositoriesRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.ListBranchesResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.ListBranchesResultJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.ListBranchesRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.ListBranchesRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.GetRepositoryResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.GetRepositoryResultJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.GetRepositoryRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.GetRepositoryRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.GetBranchResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.GetBranchResultJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.GetBranchRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.GetBranchRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.DeleteRepositoryResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.DeleteRepositoryResultJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.DeleteRepositoryRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.DeleteRepositoryRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.CreateRepositoryResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.CreateRepositoryResultJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.CreateRepositoryRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.CreateRepositoryRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.CreateBranchRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.CreateBranchRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.BranchInfoJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.BranchInfoJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.BranchInfoJsonMarshaller moved to com.amazonaws.services.codecommit.model.transform.BranchInfoJsonMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.BatchGetRepositoriesResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.BatchGetRepositoriesResultJsonUnmarshaller",
            "Move Class com.amazonaws.service.codecommit.model.transform.BatchGetRepositoriesRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.BatchGetRepositoriesRequestMarshaller",
            "Move Class com.amazonaws.service.codecommit.model.GetBranchResult moved to com.amazonaws.services.codecommit.model.GetBranchResult",
            "Move Class com.amazonaws.service.codecommit.model.GetBranchRequest moved to com.amazonaws.services.codecommit.model.GetBranchRequest",
            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyUnavailableException moved to com.amazonaws.services.codecommit.model.EncryptionKeyUnavailableException",
            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyNotFoundException moved to com.amazonaws.services.codecommit.model.EncryptionKeyNotFoundException",
            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyDisabledException moved to com.amazonaws.services.codecommit.model.EncryptionKeyDisabledException",
            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyAccessDeniedException moved to com.amazonaws.services.codecommit.model.EncryptionKeyAccessDeniedException",
            "Move Class com.amazonaws.service.codecommit.model.EncryptionIntegrityChecksFailedException moved to com.amazonaws.services.codecommit.model.EncryptionIntegrityChecksFailedException",
            "Move Class com.amazonaws.service.codecommit.model.DeleteRepositoryResult moved to com.amazonaws.services.codecommit.model.DeleteRepositoryResult",
            "Move Class com.amazonaws.service.codecommit.model.DeleteRepositoryRequest moved to com.amazonaws.services.codecommit.model.DeleteRepositoryRequest",
            "Move Class com.amazonaws.service.codecommit.model.CreateRepositoryResult moved to com.amazonaws.services.codecommit.model.CreateRepositoryResult",
            "Move Class com.amazonaws.service.codecommit.model.CreateRepositoryRequest moved to com.amazonaws.services.codecommit.model.CreateRepositoryRequest",
            "Move Class com.amazonaws.service.codecommit.model.CreateBranchRequest moved to com.amazonaws.services.codecommit.model.CreateBranchRequest",
            "Move Class com.amazonaws.service.codecommit.AWSCodeCommit moved to com.amazonaws.services.codecommit.AWSCodeCommit",
            "Move Class com.amazonaws.service.codecommit.AWSCodeCommitAsync moved to com.amazonaws.services.codecommit.AWSCodeCommitAsync",
            "Move Class com.amazonaws.service.codecommit.AWSCodeCommitAsyncClient moved to com.amazonaws.services.codecommit.AWSCodeCommitAsyncClient",
            "Move Class com.amazonaws.service.codecommit.AWSCodeCommitClient moved to com.amazonaws.services.codecommit.AWSCodeCommitClient",
            "Move Class com.amazonaws.service.codecommit.model.BatchGetRepositoriesRequest moved to com.amazonaws.services.codecommit.model.BatchGetRepositoriesRequest",
            "Move Class com.amazonaws.service.codecommit.model.BatchGetRepositoriesResult moved to com.amazonaws.services.codecommit.model.BatchGetRepositoriesResult",
            "Move Class com.amazonaws.service.codecommit.model.BranchDoesNotExistException moved to com.amazonaws.services.codecommit.model.BranchDoesNotExistException",
            "Move Class com.amazonaws.service.codecommit.model.BranchInfo moved to com.amazonaws.services.codecommit.model.BranchInfo",
            "Move Class com.amazonaws.service.codecommit.model.BranchNameExistsException moved to com.amazonaws.services.codecommit.model.BranchNameExistsException",
            "Move Class com.amazonaws.service.codecommit.model.BranchNameRequiredException moved to com.amazonaws.services.codecommit.model.BranchNameRequiredException",
            "Move Class com.amazonaws.service.codecommit.model.CommitDoesNotExistException moved to com.amazonaws.services.codecommit.model.CommitDoesNotExistException",
            "Move Class com.amazonaws.service.codecommit.model.CommitIdRequiredException moved to com.amazonaws.services.codecommit.model.CommitIdRequiredException",
            "Move Class com.amazonaws.service.codecommit.model.GetRepositoryRequest moved to com.amazonaws.services.codecommit.model.GetRepositoryRequest",
            "Move Class com.amazonaws.service.codecommit.model.GetRepositoryResult moved to com.amazonaws.services.codecommit.model.GetRepositoryResult",
            "Move Class com.amazonaws.service.codecommit.model.InvalidBranchNameException moved to com.amazonaws.services.codecommit.model.InvalidBranchNameException",
            "Move Class com.amazonaws.service.codecommit.model.InvalidCommitIdException moved to com.amazonaws.services.codecommit.model.InvalidCommitIdException",
            "Move Class com.amazonaws.service.codecommit.model.InvalidContinuationTokenException moved to com.amazonaws.services.codecommit.model.InvalidContinuationTokenException",
            "Move Class com.amazonaws.service.codecommit.model.InvalidOrderException moved to com.amazonaws.services.codecommit.model.InvalidOrderException",
            "Move Class com.amazonaws.service.codecommit.model.InvalidRepositoryDescriptionException moved to com.amazonaws.services.codecommit.model.InvalidRepositoryDescriptionException",
            "Move Class com.amazonaws.service.codecommit.model.InvalidRepositoryNameException moved to com.amazonaws.services.codecommit.model.InvalidRepositoryNameException",
            "Move Class com.amazonaws.service.codecommit.model.InvalidSortByException moved to com.amazonaws.services.codecommit.model.InvalidSortByException",
            "Move Class com.amazonaws.service.codecommit.model.ListBranchesRequest moved to com.amazonaws.services.codecommit.model.ListBranchesRequest",
            "Move Class com.amazonaws.service.codecommit.model.ListBranchesResult moved to com.amazonaws.services.codecommit.model.ListBranchesResult",
            "Move Class com.amazonaws.service.codecommit.model.ListRepositoriesRequest moved to com.amazonaws.services.codecommit.model.ListRepositoriesRequest",
            "Move Class com.amazonaws.service.codecommit.model.ListRepositoriesResult moved to com.amazonaws.services.codecommit.model.ListRepositoriesResult",
            "Move Class com.amazonaws.service.codecommit.model.MaximumRepositoryNamesExceededException moved to com.amazonaws.services.codecommit.model.MaximumRepositoryNamesExceededException",
            "Move Class com.amazonaws.service.codecommit.model.RepositoryDoesNotExistException moved to com.amazonaws.services.codecommit.model.RepositoryDoesNotExistException",
            "Move Class com.amazonaws.service.codecommit.model.RepositoryLimitExceededException moved to com.amazonaws.services.codecommit.model.RepositoryLimitExceededException",
            "Move Class com.amazonaws.service.codecommit.model.RepositoryMetadata moved to com.amazonaws.services.codecommit.model.RepositoryMetadata",
            "Move Class com.amazonaws.service.codecommit.model.RepositoryNameExistsException moved to com.amazonaws.services.codecommit.model.RepositoryNameExistsException",
            "Move Class com.amazonaws.service.codecommit.model.RepositoryNameIdPair moved to com.amazonaws.services.codecommit.model.RepositoryNameIdPair",
            "Move Class com.amazonaws.service.codecommit.model.RepositoryNameRequiredException moved to com.amazonaws.services.codecommit.model.RepositoryNameRequiredException",
            "Move Class com.amazonaws.service.codecommit.model.RepositoryNamesRequiredException moved to com.amazonaws.services.codecommit.model.RepositoryNamesRequiredException",
            "Move Class com.amazonaws.service.codecommit.model.UpdateDefaultBranchRequest moved to com.amazonaws.services.codecommit.model.UpdateDefaultBranchRequest",
            "Move Class com.amazonaws.service.codecommit.model.UpdateRepositoryDescriptionRequest moved to com.amazonaws.services.codecommit.model.UpdateRepositoryDescriptionRequest",
            "Move Class com.amazonaws.service.codecommit.model.UpdateRepositoryNameRequest moved to com.amazonaws.services.codecommit.model.UpdateRepositoryNameRequest");

          processTp(1140273, "https://github.com/Activiti/Activiti.git", "ca7d0c3b33a0863bed04c77932b9ef6b1317f34e", 
            "Move Class org.activiti.engine.impl.persistence.entity.UserEntityTest moved to org.activiti.engine.test.api.identity.UserEntityTest");

          processTp(1141310, "https://github.com/antlr/antlr4.git", "b395127e733b33c27f344695ebf155ecf5edeeab", 
            "Move Class org.antlr.v4.runtime.tree.gui.TreeViewer moved to org.antlr.v4.gui.TreeViewer",
            "Move Class org.antlr.v4.runtime.tree.gui.TreeTextProvider moved to org.antlr.v4.gui.TreeTextProvider",
            "Move Class org.antlr.v4.runtime.tree.gui.TreePostScriptGenerator moved to org.antlr.v4.gui.TreePostScriptGenerator",
            "Move Class org.antlr.v4.runtime.tree.gui.TreeLayoutAdaptor moved to org.antlr.v4.gui.TreeLayoutAdaptor",
            "Move Class org.antlr.v4.runtime.misc.TestRig moved to org.antlr.v4.gui.TestRig",
            "Move Class org.antlr.v4.runtime.tree.gui.SystemFontMetrics moved to org.antlr.v4.gui.SystemFontMetrics",
            "Move Class org.antlr.v4.runtime.tree.gui.PostScriptDocument moved to org.antlr.v4.gui.PostScriptDocument",
            "Move Class org.antlr.v4.runtime.misc.JFileChooserConfirmOverwrite moved to org.antlr.v4.gui.JFileChooserConfirmOverwrite",
            "Move Class org.antlr.v4.runtime.misc.GraphicsSupport moved to org.antlr.v4.gui.GraphicsSupport",
            "Move Class org.antlr.v4.runtime.tree.gui.BasicFontMetrics moved to org.antlr.v4.gui.BasicFontMetrics");

          processTp(1141185, "https://github.com/plutext/docx4j.git", "1ba361438ab4d7f6a0305428ba40ba62e2e6ff3c", 
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.ObjectFactory moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.ObjectFactory",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTVbaSuppData moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTVbaSuppData",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTMcds moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTMcds",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTMcd moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTMcd",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTDocEvents moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTDocEvents");

          processTp(1142385, "https://github.com/processing/processing.git", "d403a0b2322a74dde824094d67b7997c1c371883", 
            "Move Class processing.app.contrib.UpdateStatusPanel moved to processing.app.contrib.UpdateContributionTab.UpdateStatusPanel",
            "Move Class processing.app.contrib.UpdateContribListingPanel moved to processing.app.contrib.UpdateContributionTab.UpdateContribListingPanel");

          processTp(1143549, "https://github.com/apache/cassandra.git", "4fcd7d4d366d001cf5f1f7d926c608c902e3f0af", 
            "Move Class org.apache.cassandra.locator.DynamicEndpointSnitchTest.ScoreUpdater moved to org.apache.cassandra.locator.DynamicEndpointSnitchLongTest.ScoreUpdater");

          processTp(1143739, "https://github.com/opentripplanner/OpenTripPlanner.git", "334dbc7cf3432e7c17b0ed98801e61b0b591b408", 
            "Move Class org.opentripplanner.analyst.cluster.AnalystWorker.WorkerIdDefiner moved to org.opentripplanner.analyst.cluster.WorkerIdDefiner");

          processTp(1142910, "https://github.com/VoltDB/voltdb.git", "7527cfc746dc20ddb78002c7b3a65d55026a334e", 
            "Move Class org.voltdb.importer.ChannelChangeNotifier.CallbacksRef moved to org.voltdb.importer.ChannelDistributer.CallbacksRef");

          processTp(1145105, "https://github.com/MovingBlocks/Terasology.git", "543a9808a85619dbe5acc2373cb4fe5344442de7", 
            "Move Method public isFullscreen() : boolean from class org.terasology.engine.TerasologyEngine to public isFullscreen() : boolean from class org.terasology.engine.subsystem.lwjgl.LwjglDisplayDevice",
            "Inline Method private initTimer(context Context) : void inlined to public preInitialise(context Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglTimer",
            "Inline Method private initOpenAL(context Context) : void inlined to public initialise(rootContext Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglAudio",
            "Move Class org.terasology.engine.subsystem.ThreadManagerSubsystem moved to org.terasology.engine.subsystem.common.ThreadManagerSubsystem",
            "Move Class org.terasology.engine.subsystem.ThreadManager moved to org.terasology.engine.subsystem.common.ThreadManager",
            "Move Attribute private time : EngineTime from class org.terasology.engine.TerasologyEngine to class org.terasology.engine.subsystem.lwjgl.LwjglTimer",
            "Move Attribute private time : EngineTime from class org.terasology.engine.TerasologyEngine to class org.terasology.engine.subsystem.headless.HeadlessTimer");

          processTp(1144535, "https://github.com/facebook/presto.git", "484b7cb0d20ec8f7c3b0d9eaf9e3f93468cec88c", 
            "Move Class com.facebook.presto.split.TestJmxSplitManager moved to com.facebook.presto.connector.jmx.TestJmxSplitManager");

          processTp(1149047, "https://github.com/spring-projects/spring-data-neo4j.git", "ef2a0d63393484975854fc08ad0fd3abc7dd76b0", 
            "Move Class org.springframework.data.neo4j.examples.friends.Person moved to org.springframework.data.neo4j.examples.friends.domain.Person",
            "Move Class org.springframework.data.neo4j.examples.friends.Friendship moved to org.springframework.data.neo4j.examples.friends.domain.Friendship",
            "Move Class org.springframework.data.neo4j.examples.friends.FriendContext moved to org.springframework.data.neo4j.examples.friends.context.FriendContext");

          processTp(1147092, "https://github.com/neo4j/neo4j.git", "4beba7bbdf927486a5cbf298a0fb2be50905a590", 
            "Move Class org.neo4j.kernel.impl.store.UniquePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.UniquePropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.RelationshipPropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.RelationshipPropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.PropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.PropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.NodePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.NodePropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.MandatoryRelationshipPropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.MandatoryRelationshipPropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.MandatoryNodePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.MandatoryNodePropertyConstraintRule");

          processTp(1147835, "https://github.com/jersey/jersey.git", "ee5aa50af6b4586fbe92cab718abfae8113a81aa", 
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringRequestResourceTest moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringRequestResourceTest",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringRequestResource moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringRequestResource",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringAnnotationConfig moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringAnnotationConfig",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.NorwegianGoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.NorwegianGoodbyeService",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.JerseyConfig moved to org.glassfish.jersey.examples.hello.spring.annotations.JerseyConfig",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.GreetingService moved to org.glassfish.jersey.examples.hello.spring.annotations.GreetingService",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.GoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.GoodbyeService",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.EnglishGoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.EnglishGoodbyeService");

          processTp(1147192, "https://github.com/RoboBinding/RoboBinding.git", "b6565814805dfb2d989be25c11d4fb4cf8fb1d84", 
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.SubItemPresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.SubItemPresentationModelExample",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.SubItem moved to org.robobinding.codegen.presentationmodel.nestedIPM.SubItem",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.PresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.PresentationModelExample",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.ItemPresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.ItemPresentationModelExample",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.Item moved to org.robobinding.codegen.presentationmodel.nestedIPM.Item");

          processTp(1149799, "https://github.com/neo4j/neo4j.git", "021d17c8234904dcb1d54596662352395927fe7b", 
            "Move Method public nodesGetAllCursor(statement StoreStatement) : Cursor<NodeItem> from class org.neo4j.kernel.impl.api.store.DiskLayer to public nodesGetAllCursor() : Cursor<NodeItem> from class org.neo4j.kernel.impl.api.store.StoreStatement",
            "Move Method private directionOf(nodeId long, relationshipId long, startNode long, endNode long) : Direction from class org.neo4j.kernel.impl.api.store.DiskLayer to private directionOf(nodeId long, relationshipId long, startNode long, endNode long) : Direction from class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Move Method private countByFirstPrevPointer(nodeId long, relationshipId long) : long from class org.neo4j.kernel.impl.api.store.DiskLayer to private countByFirstPrevPointer(relationshipId long) : long from class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Move Method private nodeDegreeByDirection(nodeId long, group RelationshipGroupRecord, direction Direction) : long from class org.neo4j.kernel.impl.api.store.DiskLayer to private nodeDegreeByDirection(group RelationshipGroupRecord, direction Direction) : long from class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Extract Method private assertHasRelationships(node long) : void extracted from private deleteNode1(node long, prop1 DefinedProperty, prop2 DefinedProperty, prop3 DefinedProperty) : void in class org.neo4j.kernel.impl.store.TestNeoStore",
            "Extract Method private assertHasRelationships(node long) : void extracted from private deleteNode2(node long, prop1 DefinedProperty, prop2 DefinedProperty, prop3 DefinedProperty) : void in class org.neo4j.kernel.impl.store.TestNeoStore",
            "Move Class org.neo4j.kernel.impl.api.store.DiskLayer.AllStoreIdIterator moved to org.neo4j.kernel.impl.api.store.StoreStatement.AllStoreIdIterator",
            "Move Attribute package GET_LABEL : ToIntFunction<LabelItem> from class org.neo4j.kernel.api.cursor.LabelItem to class org.neo4j.kernel.api.cursor.NodeItem",
            "Move Attribute private labelCursor : InstanceCache<StoreLabelCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Move Attribute private singleLabelCursor : InstanceCache<StoreSingleLabelCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Move Attribute private nodeRelationshipCursor : InstanceCache<StoreNodeRelationshipCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Move Attribute private singlePropertyCursor : InstanceCache<StoreSinglePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Move Attribute private allPropertyCursor : InstanceCache<StorePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
            "Move Attribute private singlePropertyCursor : InstanceCache<StoreSinglePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractRelationshipCursor",
            "Move Attribute private allPropertyCursor : InstanceCache<StorePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractRelationshipCursor");

          processTp(1150594, "https://github.com/hazelcast/hazelcast.git", "f1e26fa73074a89680a2e1756d85eb80ad87c3bf", 
            "Move Class com.hazelcast.query.Predicates.InstanceOfPredicate moved to com.hazelcast.query.impl.predicates.InstanceOfPredicate",
            "Move Class com.hazelcast.query.Predicates.BetweenPredicate moved to com.hazelcast.query.impl.predicates.BetweenPredicate",
            "Move Class com.hazelcast.query.Predicates.NotPredicate moved to com.hazelcast.query.impl.predicates.NotPredicate",
            "Move Class com.hazelcast.query.Predicates.InPredicate moved to com.hazelcast.query.impl.predicates.InPredicate",
            "Move Class com.hazelcast.query.Predicates.RegexPredicate moved to com.hazelcast.query.impl.predicates.RegexPredicate",
            "Move Class com.hazelcast.query.Predicates.LikePredicate moved to com.hazelcast.query.impl.predicates.LikePredicate",
            "Move Class com.hazelcast.query.Predicates.ILikePredicate moved to com.hazelcast.query.impl.predicates.ILikePredicate",
            "Move Class com.hazelcast.query.Predicates.AndPredicate moved to com.hazelcast.query.impl.predicates.AndPredicate",
            "Move Class com.hazelcast.query.Predicates.OrPredicate moved to com.hazelcast.query.impl.predicates.OrPredicate",
            "Move Class com.hazelcast.query.Predicates.GreaterLessPredicate moved to com.hazelcast.query.impl.predicates.GreaterLessPredicate",
            "Move Class com.hazelcast.query.Predicates.NotEqualPredicate moved to com.hazelcast.query.impl.predicates.NotEqualPredicate",
            "Move Class com.hazelcast.query.Predicates.EqualPredicate moved to com.hazelcast.query.impl.predicates.EqualPredicate",
            "Move Class com.hazelcast.query.Predicates.AbstractPredicate moved to com.hazelcast.query.impl.predicates.AbstractPredicate");

          processTp(1152530, "https://github.com/addthis/hydra.git", "7fea4c9d5ee97d4a61ad985cadc9c5c0ab2db780", 
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerTest moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerTest",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerTaskSizer moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerTaskSizer",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerConfig moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerConfig",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.HostScore moved to com.addthis.hydra.job.spawn.balancer.HostScore",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.HostAndScore moved to com.addthis.hydra.job.spawn.balancer.HostAndScore",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.JobTaskItem moved to com.addthis.hydra.job.spawn.balancer.JobTaskItem",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancer");

          processTp(1152358, "https://github.com/hibernate/hibernate-orm.git", "0b6ea757e34a63b1421b77ed5fbb61398377aab1", 
            "Move Class org.hibernate.jpa.test.callbacks.EntityWithLazyProperty moved to org.hibernate.jpa.test.instrument.domain.EntityWithLazyProperty");

          processTp(1153111, "https://github.com/google/j2objc.git", "d05d92de40542e85f9f26712d976e710be82914e", 
            "Move Class com.google.devtools.j2objc.translate.LambdaExpressionTest moved to com.google.devtools.j2objc.ast.LambdaExpressionTest");

          processTp(1155401, "https://github.com/spring-projects/spring-integration.git", "4cca684f368d3ff719c62d3fa4cac3cdb7828bff", 
            "Move Class org.springframework.integration.codec.CompositeCodecTests moved to org.springframework.integration.codec.kryo.CompositeCodecTests");

          processTp(1156317, "https://github.com/jersey/jersey.git", "d94ca2b27c9e8a5fa9fe19483d58d2f2ef024606", 
            "Move Class org.glassfish.jersey.client.HttpUrlConnector moved to org.glassfish.jersey.client.internal.HttpUrlConnector");

          processTp(1156853, "https://github.com/JetBrains/intellij-community.git", "6b90205e9f7bbd1570f600d3812fd3efa1a0597e", 
            "Move Class com.intellij.execution.console.RunIdeConsoleAction.IDE moved to com.intellij.ide.script.IDE");

          processTp(1157300, "https://github.com/github/android.git", "c97659888126e43e95f0d52d22188bfe194a8439", 
            "Move Class com.github.mobile.ui.user.IconAndViewTextManagerTest moved to com.github.pockethub.ui.user.IconAndViewTextManagerTest",
            "Move Class com.github.mobile.util.TypefaceUtils moved to com.github.pockethub.util.TypefaceUtils",
            "Move Class com.github.mobile.util.ToastUtils moved to com.github.pockethub.util.ToastUtils",
            "Move Class com.github.mobile.util.TimeUtils moved to com.github.pockethub.util.TimeUtils",
            "Move Class com.github.mobile.util.SourceEditor moved to com.github.pockethub.util.SourceEditor",
            "Move Class com.github.mobile.util.ShareUtils moved to com.github.pockethub.util.ShareUtils",
            "Move Class com.github.mobile.util.ServiceUtils moved to com.github.pockethub.util.ServiceUtils",
            "Move Class com.github.mobile.util.PreferenceUtils moved to com.github.pockethub.util.PreferenceUtils",
            "Move Class com.github.mobile.util.MarkdownUtils moved to com.github.pockethub.util.MarkdownUtils",
            "Move Class com.github.mobile.util.ImageUtils moved to com.github.pockethub.util.ImageUtils",
            "Move Class com.github.mobile.util.HttpImageGetter moved to com.github.pockethub.util.HttpImageGetter",
            "Move Class com.github.mobile.util.HtmlUtils moved to com.github.pockethub.util.HtmlUtils",
            "Move Class com.github.mobile.util.GravatarUtils moved to com.github.pockethub.util.GravatarUtils",
            "Move Class com.github.mobile.util.AvatarLoader moved to com.github.pockethub.util.AvatarLoader",
            "Move Class com.github.mobile.ui.user.UserViewActivity moved to com.github.pockethub.ui.user.UserViewActivity",
            "Move Class com.github.mobile.ui.user.UserReceivedNewsFragment moved to com.github.pockethub.ui.user.UserReceivedNewsFragment",
            "Move Class com.github.mobile.ui.user.UserPagerAdapter moved to com.github.pockethub.ui.user.UserPagerAdapter",
            "Move Class com.github.mobile.ui.user.UserNewsFragment moved to com.github.pockethub.ui.user.UserNewsFragment",
            "Move Class com.github.mobile.ui.user.UserListAdapter moved to com.github.pockethub.ui.user.UserListAdapter",
            "Move Class com.github.mobile.ui.user.UserFollowingFragment moved to com.github.pockethub.ui.user.UserFollowingFragment",
            "Move Class com.github.mobile.ui.user.UserFollowersFragment moved to com.github.pockethub.ui.user.UserFollowersFragment",
            "Move Class com.github.mobile.ui.user.UserCreatedNewsFragment moved to com.github.pockethub.ui.user.UserCreatedNewsFragment",
            "Move Class com.github.mobile.ui.user.UriLauncherActivity moved to com.github.pockethub.ui.user.UriLauncherActivity",
            "Move Class com.github.mobile.ui.user.PagedUserFragment moved to com.github.pockethub.ui.user.PagedUserFragment",
            "Move Class com.github.mobile.ui.user.OrganizationSelectionProvider moved to com.github.pockethub.ui.user.OrganizationSelectionProvider",
            "Move Class com.github.mobile.ui.user.OrganizationSelectionListener moved to com.github.pockethub.ui.user.OrganizationSelectionListener",
            "Move Class com.github.mobile.ui.user.OrganizationNewsFragment moved to com.github.pockethub.ui.user.OrganizationNewsFragment",
            "Move Class com.github.mobile.ui.user.NewsListAdapter moved to com.github.pockethub.ui.user.NewsListAdapter",
            "Move Class com.github.mobile.ui.user.MyFollowingFragment moved to com.github.pockethub.ui.user.MyFollowingFragment",
            "Move Class com.github.mobile.ui.user.MyFollowersFragment moved to com.github.pockethub.ui.user.MyFollowersFragment",
            "Move Class com.github.mobile.ui.user.MembersFragment moved to com.github.pockethub.ui.user.MembersFragment",
            "Move Class com.github.mobile.ui.user.IconAndViewTextManager moved to com.github.pockethub.ui.user.IconAndViewTextManager",
            "Move Class com.github.mobile.ui.user.HomePagerFragment moved to com.github.pockethub.ui.user.HomePagerFragment",
            "Move Class com.github.mobile.ui.user.HomePagerAdapter moved to com.github.pockethub.ui.user.HomePagerAdapter",
            "Move Class com.github.mobile.ui.user.FollowingFragment moved to com.github.pockethub.ui.user.FollowingFragment",
            "Move Class com.github.mobile.ui.user.FollowersFragment moved to com.github.pockethub.ui.user.FollowersFragment",
            "Move Class com.github.mobile.ui.user.EventPager moved to com.github.pockethub.ui.user.EventPager",
            "Move Class com.github.mobile.ui.search.SearchUserListFragment moved to com.github.pockethub.ui.search.SearchUserListFragment",
            "Move Class com.github.mobile.ui.search.SearchUserListAdapter moved to com.github.pockethub.ui.search.SearchUserListAdapter",
            "Move Class com.github.mobile.ui.search.SearchRepositoryListFragment moved to com.github.pockethub.ui.search.SearchRepositoryListFragment",
            "Move Class com.github.mobile.ui.search.SearchRepositoryListAdapter moved to com.github.pockethub.ui.search.SearchRepositoryListAdapter",
            "Move Class com.github.mobile.ui.search.SearchPagerAdapter moved to com.github.pockethub.ui.search.SearchPagerAdapter",
            "Move Class com.github.mobile.ui.search.SearchActivity moved to com.github.pockethub.ui.search.SearchActivity",
            "Move Class com.github.mobile.ui.search.RepositorySearchSuggestionsProvider moved to com.github.pockethub.ui.search.RepositorySearchSuggestionsProvider",
            "Move Class com.github.mobile.ui.roboactivities.RoboSupportFragment moved to com.github.pockethub.ui.roboactivities.RoboSupportFragment",
            "Move Class com.github.mobile.ui.roboactivities.RoboActionBarActivity moved to com.github.pockethub.ui.roboactivities.RoboActionBarActivity",
            "Move Class com.github.mobile.ui.roboactivities.RoboActionBarAccountAuthenticatorActivity moved to com.github.pockethub.ui.roboactivities.RoboActionBarAccountAuthenticatorActivity",
            "Move Class com.github.mobile.ui.roboactivities.ActionBarAccountAuthenticatorActivity moved to com.github.pockethub.ui.roboactivities.ActionBarAccountAuthenticatorActivity",
            "Move Class com.github.mobile.ui.repo.UserRepositoryListFragment moved to com.github.pockethub.ui.repo.UserRepositoryListFragment",
            "Move Class com.github.mobile.ui.repo.UserRepositoryListAdapter moved to com.github.pockethub.ui.repo.UserRepositoryListAdapter",
            "Move Class com.github.mobile.ui.repo.RepositoryViewActivity moved to com.github.pockethub.ui.repo.RepositoryViewActivity",
            "Move Class com.github.mobile.ui.repo.RepositoryPagerAdapter moved to com.github.pockethub.ui.repo.RepositoryPagerAdapter",
            "Move Class com.github.mobile.ui.repo.RepositoryNewsFragment moved to com.github.pockethub.ui.repo.RepositoryNewsFragment",
            "Move Class com.github.mobile.ui.repo.RepositoryListFragment moved to com.github.pockethub.ui.repo.RepositoryListFragment",
            "Move Class com.github.mobile.ui.repo.RepositoryListAdapter moved to com.github.pockethub.ui.repo.RepositoryListAdapter",
            "Move Class com.github.mobile.ui.repo.RepositoryContributorsFragment moved to com.github.pockethub.ui.repo.RepositoryContributorsFragment",
            "Move Class com.github.mobile.ui.repo.RepositoryContributorsActivity moved to com.github.pockethub.ui.repo.RepositoryContributorsActivity",
            "Move Class com.github.mobile.ui.repo.RecentRepositories moved to com.github.pockethub.ui.repo.RecentRepositories",
            "Move Class com.github.mobile.ui.repo.OrganizationLoader moved to com.github.pockethub.ui.repo.OrganizationLoader",
            "Move Class com.github.mobile.ui.repo.DefaultRepositoryListAdapter moved to com.github.pockethub.ui.repo.DefaultRepositoryListAdapter",
            "Move Class com.github.mobile.ui.repo.ContributorListAdapter moved to com.github.pockethub.ui.repo.ContributorListAdapter",
            "Move Class com.github.mobile.ui.ref.RefDialogFragment moved to com.github.pockethub.ui.ref.RefDialogFragment",
            "Move Class com.github.mobile.ui.ref.RefDialog moved to com.github.pockethub.ui.ref.RefDialog",
            "Move Class com.github.mobile.ui.ref.CodeTreeAdapter moved to com.github.pockethub.ui.ref.CodeTreeAdapter",
            "Move Class com.github.mobile.ui.ref.BranchFileViewActivity moved to com.github.pockethub.ui.ref.BranchFileViewActivity",
            "Move Class com.github.mobile.ui.issue.SearchIssueListFragment moved to com.github.pockethub.ui.issue.SearchIssueListFragment",
            "Move Class com.github.mobile.ui.issue.SearchIssueListAdapter moved to com.github.pockethub.ui.issue.SearchIssueListAdapter",
            "Move Class com.github.mobile.ui.issue.RepositoryIssueListAdapter moved to com.github.pockethub.ui.issue.RepositoryIssueListAdapter",
            "Move Class com.github.mobile.ui.issue.MilestoneDialogFragment moved to com.github.pockethub.ui.issue.MilestoneDialogFragment",
            "Move Class com.github.mobile.ui.issue.MilestoneDialog moved to com.github.pockethub.ui.issue.MilestoneDialog",
            "Move Class com.github.mobile.ui.issue.LabelsDialogFragment moved to com.github.pockethub.ui.issue.LabelsDialogFragment",
            "Move Class com.github.mobile.ui.issue.LabelsDialog moved to com.github.pockethub.ui.issue.LabelsDialog",
            "Move Class com.github.mobile.ui.issue.LabelDrawableSpan moved to com.github.pockethub.ui.issue.LabelDrawableSpan",
            "Move Class com.github.mobile.ui.issue.IssuesViewActivity moved to com.github.pockethub.ui.issue.IssuesViewActivity",
            "Move Class com.github.mobile.ui.issue.IssuesPagerAdapter moved to com.github.pockethub.ui.issue.IssuesPagerAdapter",
            "Move Class com.github.mobile.ui.issue.IssuesFragment moved to com.github.pockethub.ui.issue.IssuesFragment",
            "Move Class com.github.mobile.ui.issue.IssueSearchSuggestionsProvider moved to com.github.pockethub.ui.issue.IssueSearchSuggestionsProvider",
            "Move Class com.github.mobile.ui.issue.IssueSearchActivity moved to com.github.pockethub.ui.issue.IssueSearchActivity",
            "Move Class com.github.mobile.ui.issue.IssueListAdapter moved to com.github.pockethub.ui.issue.IssueListAdapter",
            "Move Class com.github.mobile.ui.issue.IssueFragment moved to com.github.pockethub.ui.issue.IssueFragment",
            "Move Class com.github.mobile.ui.issue.IssueDashboardPagerFragment moved to com.github.pockethub.ui.issue.IssueDashboardPagerFragment",
            "Move Class com.github.mobile.ui.issue.IssueDashboardPagerAdapter moved to com.github.pockethub.ui.issue.IssueDashboardPagerAdapter",
            "Move Class com.github.mobile.ui.issue.IssueBrowseActivity moved to com.github.pockethub.ui.issue.IssueBrowseActivity",
            "Move Class com.github.mobile.ui.issue.FiltersViewFragment moved to com.github.pockethub.ui.issue.FiltersViewFragment",
            "Move Class com.github.mobile.ui.issue.FiltersViewActivity moved to com.github.pockethub.ui.issue.FiltersViewActivity",
            "Move Class com.github.mobile.ui.issue.FilterListFragment moved to com.github.pockethub.ui.issue.FilterListFragment",
            "Move Class com.github.mobile.ui.issue.FilterListAdapter moved to com.github.pockethub.ui.issue.FilterListAdapter",
            "Move Class com.github.mobile.ui.issue.EditStateTask moved to com.github.pockethub.ui.issue.EditStateTask",
            "Move Class com.github.mobile.ui.issue.EditMilestoneTask moved to com.github.pockethub.ui.issue.EditMilestoneTask",
            "Move Class com.github.mobile.ui.issue.EditLabelsTask moved to com.github.pockethub.ui.issue.EditLabelsTask",
            "Move Class com.github.mobile.ui.issue.EditIssuesFilterActivity moved to com.github.pockethub.ui.issue.EditIssuesFilterActivity",
            "Move Class com.github.mobile.ui.issue.EditIssueTask moved to com.github.pockethub.ui.issue.EditIssueTask",
            "Move Class com.github.mobile.ui.issue.EditIssueActivity moved to com.github.pockethub.ui.issue.EditIssueActivity",
            "Move Class com.github.mobile.ui.issue.EditCommentTask moved to com.github.pockethub.ui.issue.EditCommentTask",
            "Move Class com.github.mobile.ui.issue.EditCommentActivity moved to com.github.pockethub.ui.issue.EditCommentActivity",
            "Move Class com.github.mobile.ui.issue.EditAssigneeTask moved to com.github.pockethub.ui.issue.EditAssigneeTask",
            "Move Class com.github.mobile.ui.issue.DeleteCommentTask moved to com.github.pockethub.ui.issue.DeleteCommentTask",
            "Move Class com.github.mobile.ui.issue.DashboardIssueListAdapter moved to com.github.pockethub.ui.issue.DashboardIssueListAdapter",
            "Move Class com.github.mobile.ui.issue.DashboardIssueFragment moved to com.github.pockethub.ui.issue.DashboardIssueFragment",
            "Move Class com.github.mobile.ui.issue.CreateIssueTask moved to com.github.pockethub.ui.issue.CreateIssueTask",
            "Move Class com.github.mobile.ui.issue.CreateCommentTask moved to com.github.pockethub.ui.issue.CreateCommentTask",
            "Move Class com.github.mobile.ui.issue.CreateCommentActivity moved to com.github.pockethub.ui.issue.CreateCommentActivity",
            "Move Class com.github.mobile.ui.issue.AssigneeDialogFragment moved to com.github.pockethub.ui.issue.AssigneeDialogFragment",
            "Move Class com.github.mobile.ui.issue.AssigneeDialog moved to com.github.pockethub.ui.issue.AssigneeDialog",
            "Move Class com.github.mobile.ui.gist.StarredGistsFragment moved to com.github.pockethub.ui.gist.StarredGistsFragment",
            "Move Class com.github.mobile.ui.gist.RandomGistTask moved to com.github.pockethub.ui.gist.RandomGistTask",
            "Move Class com.github.mobile.ui.gist.PublicGistsFragment moved to com.github.pockethub.ui.gist.PublicGistsFragment",
            "Move Class com.github.mobile.core.repo.UnstarRepositoryTask moved to com.github.pockethub.core.repo.UnstarRepositoryTask",
            "Move Class com.github.mobile.core.repo.StarredRepositoryTask moved to com.github.pockethub.core.repo.StarredRepositoryTask",
            "Move Class com.github.mobile.core.repo.StarRepositoryTask moved to com.github.pockethub.core.repo.StarRepositoryTask",
            "Move Class com.github.mobile.core.repo.RepositoryUtils moved to com.github.pockethub.core.repo.RepositoryUtils",
            "Move Class com.github.mobile.core.repo.RepositoryUriMatcher moved to com.github.pockethub.core.repo.RepositoryUriMatcher",
            "Move Class com.github.mobile.core.repo.RepositoryEventMatcher moved to com.github.pockethub.core.repo.RepositoryEventMatcher",
            "Move Class com.github.mobile.core.repo.RefreshRepositoryTask moved to com.github.pockethub.core.repo.RefreshRepositoryTask",
            "Move Class com.github.mobile.core.repo.ForkRepositoryTask moved to com.github.pockethub.core.repo.ForkRepositoryTask",
            "Move Class com.github.mobile.core.repo.DeleteRepositoryTask moved to com.github.pockethub.core.repo.DeleteRepositoryTask",
            "Move Class com.github.mobile.core.ref.RefUtils moved to com.github.pockethub.core.ref.RefUtils",
            "Move Class com.github.mobile.core.issue.RefreshIssueTask moved to com.github.pockethub.core.issue.RefreshIssueTask",
            "Move Class com.github.mobile.core.issue.IssueUtils moved to com.github.pockethub.core.issue.IssueUtils",
            "Move Class com.github.mobile.core.issue.IssueUriMatcher moved to com.github.pockethub.core.issue.IssueUriMatcher",
            "Move Class com.github.mobile.core.issue.IssueStore moved to com.github.pockethub.core.issue.IssueStore",
            "Move Class com.github.mobile.core.issue.IssuePager moved to com.github.pockethub.core.issue.IssuePager",
            "Move Class com.github.mobile.core.issue.IssueFilter moved to com.github.pockethub.core.issue.IssueFilter",
            "Move Class com.github.mobile.core.issue.IssueEventMatcher moved to com.github.pockethub.core.issue.IssueEventMatcher",
            "Move Class com.github.mobile.core.issue.FullIssue moved to com.github.pockethub.core.issue.FullIssue",
            "Move Class com.github.mobile.core.gist.UnstarGistTask moved to com.github.pockethub.core.gist.UnstarGistTask",
            "Move Class com.github.mobile.core.gist.StarGistTask moved to com.github.pockethub.core.gist.StarGistTask",
            "Move Class com.github.mobile.core.gist.RefreshGistTask moved to com.github.pockethub.core.gist.RefreshGistTask",
            "Move Class com.github.mobile.core.gist.GistUriMatcher moved to com.github.pockethub.core.gist.GistUriMatcher",
            "Move Class com.github.mobile.core.gist.GistStore moved to com.github.pockethub.core.gist.GistStore",
            "Move Class com.github.mobile.core.gist.GistPager moved to com.github.pockethub.core.gist.GistPager",
            "Move Class com.github.mobile.core.gist.GistEventMatcher moved to com.github.pockethub.core.gist.GistEventMatcher",
            "Move Class com.github.mobile.core.gist.FullGist moved to com.github.pockethub.core.gist.FullGist",
            "Move Class com.github.mobile.core.commit.RefreshCommitTask moved to com.github.pockethub.core.commit.RefreshCommitTask",
            "Move Class com.github.mobile.core.commit.FullCommitFile moved to com.github.pockethub.core.commit.FullCommitFile",
            "Move Class com.github.mobile.core.commit.FullCommit moved to com.github.pockethub.core.commit.FullCommit",
            "Move Class com.github.mobile.core.commit.CommitUtils moved to com.github.pockethub.core.commit.CommitUtils",
            "Move Class com.github.mobile.core.commit.CommitUriMatcher moved to com.github.pockethub.core.commit.CommitUriMatcher",
            "Move Class com.github.mobile.core.commit.CommitStore moved to com.github.pockethub.core.commit.CommitStore",
            "Move Class com.github.mobile.core.commit.CommitPager moved to com.github.pockethub.core.commit.CommitPager",
            "Move Class com.github.mobile.core.commit.CommitMatch moved to com.github.pockethub.core.commit.CommitMatch",
            "Move Class com.github.mobile.core.commit.CommitCompareTask moved to com.github.pockethub.core.commit.CommitCompareTask",
            "Move Class com.github.mobile.core.code.RefreshTreeTask moved to com.github.pockethub.core.code.RefreshTreeTask",
            "Move Class com.github.mobile.core.code.RefreshBlobTask moved to com.github.pockethub.core.code.RefreshBlobTask",
            "Move Class com.github.mobile.core.code.FullTree moved to com.github.pockethub.core.code.FullTree",
            "Move Class com.github.mobile.core.UrlMatcher moved to com.github.pockethub.core.UrlMatcher",
            "Move Class com.github.mobile.core.ResourcePager moved to com.github.pockethub.core.ResourcePager",
            "Move Class com.github.mobile.core.OnLoadListener moved to com.github.pockethub.core.OnLoadListener",
            "Move Class com.github.mobile.core.ItemStore moved to com.github.pockethub.core.ItemStore",
            "Move Class com.github.mobile.api.GitHubClientV2 moved to com.github.pockethub.api.GitHubClientV2",
            "Move Class com.github.mobile.accounts.TwoFactorAuthException moved to com.github.pockethub.accounts.TwoFactorAuthException",
            "Move Class com.github.mobile.accounts.TwoFactorAuthClient moved to com.github.pockethub.accounts.TwoFactorAuthClient",
            "Move Class com.github.mobile.accounts.TwoFactorAuthActivity moved to com.github.pockethub.accounts.TwoFactorAuthActivity",
            "Move Class com.github.mobile.accounts.ScopeBase moved to com.github.pockethub.accounts.ScopeBase",
            "Move Class com.github.mobile.accounts.LoginWebViewActivity moved to com.github.pockethub.accounts.LoginWebViewActivity",
            "Move Class com.github.mobile.tests.repo.SearchActivityTest moved to com.github.pockethub.tests.repo.SearchActivityTest",
            "Move Class com.github.mobile.tests.repo.RepositoryUriMatcherTest moved to com.github.pockethub.tests.repo.RepositoryUriMatcherTest",
            "Move Class com.github.mobile.tests.repo.RepositoryEventMatcherTest moved to com.github.pockethub.tests.repo.RepositoryEventMatcherTest",
            "Move Class com.github.mobile.tests.repo.RecentRepositoriesTest moved to com.github.pockethub.tests.repo.RecentRepositoriesTest",
            "Move Class com.github.mobile.tests.ref.RefUtilsTest moved to com.github.pockethub.tests.ref.RefUtilsTest",
            "Move Class com.github.mobile.tests.issue.IssueUriMatcherTest moved to com.github.pockethub.tests.issue.IssueUriMatcherTest",
            "Move Class com.github.mobile.tests.issue.IssueStoreTest moved to com.github.pockethub.tests.issue.IssueStoreTest",
            "Move Class com.github.mobile.tests.issue.IssueFilterTest moved to com.github.pockethub.tests.issue.IssueFilterTest",
            "Move Class com.github.mobile.tests.issue.EditIssuesFilterActivityTest moved to com.github.pockethub.tests.issue.EditIssuesFilterActivityTest",
            "Move Class com.github.mobile.tests.issue.EditIssueActivityTest moved to com.github.pockethub.tests.issue.EditIssueActivityTest",
            "Move Class com.github.mobile.tests.issue.CreateCommentActivityTest moved to com.github.pockethub.tests.issue.CreateCommentActivityTest",
            "Move Class com.github.mobile.tests.gist.GistUriMatcherTest moved to com.github.pockethub.tests.gist.GistUriMatcherTest",
            "Move Class com.github.mobile.tests.ActivityTest moved to com.github.pockethub.tests.ActivityTest",
            "Move Class com.github.mobile.tests.FiltersViewActivityTest moved to com.github.pockethub.tests.FiltersViewActivityTest",
            "Move Class com.github.mobile.tests.NewsEventTextTest moved to com.github.pockethub.tests.NewsEventTextTest",
            "Move Class com.github.mobile.tests.commit.CommitUriMatcherTest moved to com.github.pockethub.tests.commit.CommitUriMatcherTest",
            "Move Class com.github.mobile.tests.commit.CommitUtilsTest moved to com.github.pockethub.tests.commit.CommitUtilsTest",
            "Move Class com.github.mobile.tests.commit.CreateCommentActivityTest moved to com.github.pockethub.tests.commit.CreateCommentActivityTest",
            "Move Class com.github.mobile.tests.commit.DiffStylerTest moved to com.github.pockethub.tests.commit.DiffStylerTest",
            "Move Class com.github.mobile.tests.commit.FullCommitTest moved to com.github.pockethub.tests.commit.FullCommitTest",
            "Move Class com.github.mobile.tests.gist.CreateCommentActivityTest moved to com.github.pockethub.tests.gist.CreateCommentActivityTest",
            "Move Class com.github.mobile.tests.gist.CreateGistActivityTest moved to com.github.pockethub.tests.gist.CreateGistActivityTest",
            "Move Class com.github.mobile.tests.gist.GistFilesViewActivityTest moved to com.github.pockethub.tests.gist.GistFilesViewActivityTest",
            "Move Class com.github.mobile.tests.gist.GistStoreTest moved to com.github.pockethub.tests.gist.GistStoreTest",
            "Move Class com.github.mobile.tests.user.LoginActivityTest moved to com.github.pockethub.tests.user.LoginActivityTest",
            "Move Class com.github.mobile.tests.user.UserComparatorTest moved to com.github.pockethub.tests.user.UserComparatorTest",
            "Move Class com.github.mobile.tests.user.UserUriMatcherTest moved to com.github.pockethub.tests.user.UserUriMatcherTest",
            "Move Class com.github.mobile.tests.util.HtmlUtilsTest moved to com.github.pockethub.tests.util.HtmlUtilsTest",
            "Move Class com.github.mobile.DefaultClient moved to com.github.pockethub.DefaultClient",
            "Move Class com.github.mobile.GitHubModule moved to com.github.pockethub.GitHubModule",
            "Move Class com.github.mobile.Intents moved to com.github.pockethub.Intents",
            "Move Class com.github.mobile.RequestCodes moved to com.github.pockethub.RequestCodes",
            "Move Class com.github.mobile.RequestFuture moved to com.github.pockethub.RequestFuture",
            "Move Class com.github.mobile.RequestReader moved to com.github.pockethub.RequestReader",
            "Move Class com.github.mobile.RequestWriter moved to com.github.pockethub.RequestWriter",
            "Move Class com.github.mobile.ResultCodes moved to com.github.pockethub.ResultCodes",
            "Move Class com.github.mobile.ServicesModule moved to com.github.pockethub.ServicesModule",
            "Move Class com.github.mobile.ThrowableLoader moved to com.github.pockethub.ThrowableLoader",
            "Move Class com.github.mobile.accounts.AccountAuthenticator moved to com.github.pockethub.accounts.AccountAuthenticator",
            "Move Class com.github.mobile.accounts.AccountAuthenticatorService moved to com.github.pockethub.accounts.AccountAuthenticatorService",
            "Move Class com.github.mobile.accounts.AccountClient moved to com.github.pockethub.accounts.AccountClient",
            "Move Class com.github.mobile.accounts.AccountConstants moved to com.github.pockethub.accounts.AccountConstants",
            "Move Class com.github.mobile.accounts.AccountScope moved to com.github.pockethub.accounts.AccountScope",
            "Move Class com.github.mobile.accounts.AccountUtils moved to com.github.pockethub.accounts.AccountUtils",
            "Move Class com.github.mobile.accounts.AuthenticatedUserLoader moved to com.github.pockethub.accounts.AuthenticatedUserLoader",
            "Move Class com.github.mobile.accounts.AuthenticatedUserTask moved to com.github.pockethub.accounts.AuthenticatedUserTask",
            "Move Class com.github.mobile.accounts.GitHubAccount moved to com.github.pockethub.accounts.GitHubAccount",
            "Move Class com.github.mobile.accounts.LoginActivity moved to com.github.pockethub.accounts.LoginActivity",
            "Move Class com.github.mobile.core.search.SearchUser moved to com.github.pockethub.core.search.SearchUser",
            "Move Class com.github.mobile.core.search.SearchUserService moved to com.github.pockethub.core.search.SearchUserService",
            "Move Class com.github.mobile.core.user.FollowUserTask moved to com.github.pockethub.core.user.FollowUserTask",
            "Move Class com.github.mobile.core.user.FollowingUserTask moved to com.github.pockethub.core.user.FollowingUserTask",
            "Move Class com.github.mobile.core.user.RefreshUserTask moved to com.github.pockethub.core.user.RefreshUserTask",
            "Move Class com.github.mobile.core.user.UnfollowUserTask moved to com.github.pockethub.core.user.UnfollowUserTask",
            "Move Class com.github.mobile.core.user.UserComparator moved to com.github.pockethub.core.user.UserComparator",
            "Move Class com.github.mobile.core.user.UserEventMatcher moved to com.github.pockethub.core.user.UserEventMatcher",
            "Move Class com.github.mobile.core.user.UserPager moved to com.github.pockethub.core.user.UserPager",
            "Move Class com.github.mobile.core.user.UserUriMatcher moved to com.github.pockethub.core.user.UserUriMatcher",
            "Move Class com.github.mobile.model.App moved to com.github.pockethub.model.App",
            "Move Class com.github.mobile.model.Authorization moved to com.github.pockethub.model.Authorization",
            "Move Class com.github.mobile.persistence.AccountDataManager moved to com.github.pockethub.persistence.AccountDataManager",
            "Move Class com.github.mobile.persistence.CacheHelper moved to com.github.pockethub.persistence.CacheHelper",
            "Move Class com.github.mobile.persistence.DatabaseCache moved to com.github.pockethub.persistence.DatabaseCache",
            "Move Class com.github.mobile.persistence.OrganizationRepositories moved to com.github.pockethub.persistence.OrganizationRepositories",
            "Move Class com.github.mobile.persistence.Organizations moved to com.github.pockethub.persistence.Organizations",
            "Move Class com.github.mobile.persistence.PersistableResource moved to com.github.pockethub.persistence.PersistableResource",
            "Move Class com.github.mobile.sync.ContentProviderAdapter moved to com.github.pockethub.sync.ContentProviderAdapter",
            "Move Class com.github.mobile.sync.SyncAdapter moved to com.github.pockethub.sync.SyncAdapter",
            "Move Class com.github.mobile.sync.SyncAdapterService moved to com.github.pockethub.sync.SyncAdapterService",
            "Move Class com.github.mobile.sync.SyncCampaign moved to com.github.pockethub.sync.SyncCampaign",
            "Move Class com.github.mobile.ui.BaseActivity moved to com.github.pockethub.ui.BaseActivity",
            "Move Class com.github.mobile.ui.CheckableRelativeLayout moved to com.github.pockethub.ui.CheckableRelativeLayout",
            "Move Class com.github.mobile.ui.ConfirmDialogFragment moved to com.github.pockethub.ui.ConfirmDialogFragment",
            "Move Class com.github.mobile.ui.DialogFragment moved to com.github.pockethub.ui.DialogFragment",
            "Move Class com.github.mobile.ui.DialogFragmentActivity moved to com.github.pockethub.ui.DialogFragmentActivity",
            "Move Class com.github.mobile.ui.DialogFragmentHelper moved to com.github.pockethub.ui.DialogFragmentHelper",
            "Move Class com.github.mobile.ui.DialogResultListener moved to com.github.pockethub.ui.DialogResultListener",
            "Move Class com.github.mobile.ui.FragmentPagerAdapter moved to com.github.pockethub.ui.FragmentPagerAdapter",
            "Move Class com.github.mobile.ui.FragmentProvider moved to com.github.pockethub.ui.FragmentProvider",
            "Move Class com.github.mobile.ui.FragmentStatePagerAdapter moved to com.github.pockethub.ui.FragmentStatePagerAdapter",
            "Move Class com.github.mobile.ui.HeaderFooterListAdapter moved to com.github.pockethub.ui.HeaderFooterListAdapter",
            "Move Class com.github.mobile.ui.ItemListFragment moved to com.github.pockethub.ui.ItemListFragment",
            "Move Class com.github.mobile.ui.LightAlertDialog moved to com.github.pockethub.ui.LightAlertDialog",
            "Move Class com.github.mobile.ui.LightProgressDialog moved to com.github.pockethub.ui.LightProgressDialog",
            "Move Class com.github.mobile.ui.MainActivity moved to com.github.pockethub.ui.MainActivity",
            "Move Class com.github.mobile.ui.MarkdownLoader moved to com.github.pockethub.ui.MarkdownLoader",
            "Move Class com.github.mobile.ui.NavigationDrawerAdapter moved to com.github.pockethub.ui.NavigationDrawerAdapter",
            "Move Class com.github.mobile.ui.NavigationDrawerFragment moved to com.github.pockethub.ui.NavigationDrawerFragment",
            "Move Class com.github.mobile.ui.NavigationDrawerObject moved to com.github.pockethub.ui.NavigationDrawerObject",
            "Move Class com.github.mobile.ui.NewsFragment moved to com.github.pockethub.ui.NewsFragment",
            "Move Class com.github.mobile.ui.PagedItemFragment moved to com.github.pockethub.ui.PagedItemFragment",
            "Move Class com.github.mobile.ui.PagerActivity moved to com.github.pockethub.ui.PagerActivity",
            "Move Class com.github.mobile.ui.PagerFragment moved to com.github.pockethub.ui.PagerFragment",
            "Move Class com.github.mobile.ui.PatchedScrollingViewBehavior moved to com.github.pockethub.ui.PatchedScrollingViewBehavior",
            "Move Class com.github.mobile.ui.ProgressDialogTask moved to com.github.pockethub.ui.ProgressDialogTask",
            "Move Class com.github.mobile.ui.ResourceLoadingIndicator moved to com.github.pockethub.ui.ResourceLoadingIndicator",
            "Move Class com.github.mobile.ui.SelectableLinkMovementMethod moved to com.github.pockethub.ui.SelectableLinkMovementMethod",
            "Move Class com.github.mobile.ui.SingleChoiceDialogFragment moved to com.github.pockethub.ui.SingleChoiceDialogFragment",
            "Move Class com.github.mobile.ui.StyledText moved to com.github.pockethub.ui.StyledText",
            "Move Class com.github.mobile.ui.TabPagerActivity moved to com.github.pockethub.ui.TabPagerActivity",
            "Move Class com.github.mobile.ui.TabPagerFragment moved to com.github.pockethub.ui.TabPagerFragment",
            "Move Class com.github.mobile.ui.TextWatcherAdapter moved to com.github.pockethub.ui.TextWatcherAdapter",
            "Move Class com.github.mobile.ui.ViewPager moved to com.github.pockethub.ui.ViewPager",
            "Move Class com.github.mobile.ui.WebView moved to com.github.pockethub.ui.WebView",
            "Move Class com.github.mobile.ui.code.RepositoryCodeFragment moved to com.github.pockethub.ui.code.RepositoryCodeFragment",
            "Move Class com.github.mobile.ui.comment.CommentListAdapter moved to com.github.pockethub.ui.comment.CommentListAdapter",
            "Move Class com.github.mobile.ui.comment.CommentPreviewPagerAdapter moved to com.github.pockethub.ui.comment.CommentPreviewPagerAdapter",
            "Move Class com.github.mobile.ui.comment.CreateCommentActivity moved to com.github.pockethub.ui.comment.CreateCommentActivity",
            "Move Class com.github.mobile.ui.comment.DeleteCommentListener moved to com.github.pockethub.ui.comment.DeleteCommentListener",
            "Move Class com.github.mobile.ui.comment.EditCommentListener moved to com.github.pockethub.ui.comment.EditCommentListener",
            "Move Class com.github.mobile.ui.comment.RawCommentFragment moved to com.github.pockethub.ui.comment.RawCommentFragment",
            "Move Class com.github.mobile.ui.comment.RenderedCommentFragment moved to com.github.pockethub.ui.comment.RenderedCommentFragment",
            "Move Class com.github.mobile.ui.commit.CommitCompareListFragment moved to com.github.pockethub.ui.commit.CommitCompareListFragment",
            "Move Class com.github.mobile.ui.commit.CommitCompareViewActivity moved to com.github.pockethub.ui.commit.CommitCompareViewActivity",
            "Move Class com.github.mobile.ui.commit.CommitDiffListFragment moved to com.github.pockethub.ui.commit.CommitDiffListFragment",
            "Move Class com.github.mobile.ui.commit.CommitFileComparator moved to com.github.pockethub.ui.commit.CommitFileComparator",
            "Move Class com.github.mobile.ui.commit.CommitFileListAdapter moved to com.github.pockethub.ui.commit.CommitFileListAdapter",
            "Move Class com.github.mobile.ui.commit.CommitFileViewActivity moved to com.github.pockethub.ui.commit.CommitFileViewActivity",
            "Move Class com.github.mobile.ui.commit.CommitListAdapter moved to com.github.pockethub.ui.commit.CommitListAdapter",
            "Move Class com.github.mobile.ui.commit.CommitListFragment moved to com.github.pockethub.ui.commit.CommitListFragment",
            "Move Class com.github.mobile.ui.commit.CommitPagerAdapter moved to com.github.pockethub.ui.commit.CommitPagerAdapter",
            "Move Class com.github.mobile.ui.commit.CommitViewActivity moved to com.github.pockethub.ui.commit.CommitViewActivity",
            "Move Class com.github.mobile.ui.commit.CreateCommentActivity moved to com.github.pockethub.ui.commit.CreateCommentActivity",
            "Move Class com.github.mobile.ui.commit.CreateCommentTask moved to com.github.pockethub.ui.commit.CreateCommentTask",
            "Move Class com.github.mobile.ui.commit.DiffStyler moved to com.github.pockethub.ui.commit.DiffStyler",
            "Move Class com.github.mobile.ui.gist.CreateCommentActivity moved to com.github.pockethub.ui.gist.CreateCommentActivity",
            "Move Class com.github.mobile.ui.gist.CreateCommentTask moved to com.github.pockethub.ui.gist.CreateCommentTask",
            "Move Class com.github.mobile.ui.gist.CreateGistActivity moved to com.github.pockethub.ui.gist.CreateGistActivity",
            "Move Class com.github.mobile.ui.gist.CreateGistTask moved to com.github.pockethub.ui.gist.CreateGistTask",
            "Move Class com.github.mobile.ui.gist.DeleteCommentTask moved to com.github.pockethub.ui.gist.DeleteCommentTask",
            "Move Class com.github.mobile.ui.gist.DeleteGistTask moved to com.github.pockethub.ui.gist.DeleteGistTask",
            "Move Class com.github.mobile.ui.gist.EditCommentActivity moved to com.github.pockethub.ui.gist.EditCommentActivity",
            "Move Class com.github.mobile.ui.gist.EditCommentTask moved to com.github.pockethub.ui.gist.EditCommentTask",
            "Move Class com.github.mobile.ui.gist.GistFileFragment moved to com.github.pockethub.ui.gist.GistFileFragment",
            "Move Class com.github.mobile.ui.gist.GistFilesPagerAdapter moved to com.github.pockethub.ui.gist.GistFilesPagerAdapter",
            "Move Class com.github.mobile.ui.gist.GistFilesViewActivity moved to com.github.pockethub.ui.gist.GistFilesViewActivity",
            "Move Class com.github.mobile.ui.gist.GistFragment moved to com.github.pockethub.ui.gist.GistFragment",
            "Move Class com.github.mobile.ui.gist.GistListAdapter moved to com.github.pockethub.ui.gist.GistListAdapter",
            "Move Class com.github.mobile.ui.gist.GistQueriesPagerAdapter moved to com.github.pockethub.ui.gist.GistQueriesPagerAdapter",
            "Move Class com.github.mobile.ui.gist.GistsFragment moved to com.github.pockethub.ui.gist.GistsFragment",
            "Move Class com.github.mobile.ui.gist.GistsPagerAdapter moved to com.github.pockethub.ui.gist.GistsPagerAdapter",
            "Move Class com.github.mobile.ui.gist.GistsPagerFragment moved to com.github.pockethub.ui.gist.GistsPagerFragment",
            "Move Class com.github.mobile.ui.gist.GistsViewActivity moved to com.github.pockethub.ui.gist.GistsViewActivity",
            "Move Class com.github.mobile.ui.gist.MyGistsFragment moved to com.github.pockethub.ui.gist.MyGistsFragment");
    }

    public void processTp(int id, String cloneUrl, String commitId, String ... refactorings) {
        process(id, true, cloneUrl, commitId, refactorings);
    }

    public void processFp(String cloneUrl, String commitId, String ... refactorings) {
        process(0, false, cloneUrl, commitId, refactorings);
    }

    public void process(String cloneUrl, String commitId, String ... refactorings) {
        process(0, true, cloneUrl, commitId, refactorings);
    }
    
	public void process(int id, boolean tp, String cloneUrl, String commitId, String ... refactorings) {
        	    
	    GitService gitService = new GitServiceImpl();
        String folder = tempDir + "/" + cloneUrl.substring(cloneUrl.lastIndexOf('/') + 1, cloneUrl.lastIndexOf('.'));
        //File projectFolder = new File(folder);
        
        Set<String> mcrs = new HashSet<String>();
        Set<String> result = new HashSet<String>();
        for (String refactoring : refactorings) {
            RefactoringType refType = extractRefactoringType(refactoring);
            if (refType == RefactoringType.MOVE_CLASS/* && tp*/) {
//                String from = getParent(RefactoringType.MOVE_CLASS.getGroup(refactoring, 1));
//                String to = getParent(RefactoringType.MOVE_CLASS.getGroup(refactoring, 2));
//                boolean fromIsPackage = isPackageName(from);
//                boolean toIsPackage = isPackageName(to);
//                if (fromIsPackage && toIsPackage && !from.equals(to)) {
                    mcrs.add(normalize(refactoring));
//                } else {
//                    result.add(normalize(refactoring));
//                }
            } else {
                result.add(normalize(refactoring));
            }
        }
        
        if (!mcrs.isEmpty()) {
            try (Repository rep = gitService.cloneIfNotExists(folder, cloneUrl); RevWalk walk = new RevWalk(rep)) {
                RevCommit commit = walk.parseCommit(rep.resolve(commitId));
                RevCommit parent = commit.getParent(0);
                walk.parseCommit(parent);
                
                Set<String> foldersBefore = getFolders(rep, parent);
                Set<String> foldersAfter = getFolders(rep, commit);
                
                for (String refactoring : mcrs) {
                    String from = getParent(RefactoringType.MOVE_CLASS.getGroup(refactoring, 1));
                    String to = getParent(RefactoringType.MOVE_CLASS.getGroup(refactoring, 2));
                    boolean existsBefore = containsPackage(foldersBefore, to);
                    boolean existsAfter = containsPackage(foldersAfter, from);
                    boolean fromIsPackage = isPackageName(from);
                    boolean toIsPackage = isPackageName(to);
                    String fromF = (fromIsPackage ? ("pkg" + (existsAfter ? "~" : "-")) : "cls") + " " + from;
                    String toF = (toIsPackage ? ("pkg" + (existsBefore ? "~" : "+")) : "cls") + " " + to;
                    result.add(String.format("Move Class from %s to %s", fromF, toF));
//                    if (fromIsPackage && toIsPackage && !existsBefore && !existsAfter) {
//                        // rename
////                        result.add("Rename Package " + from + " to " + to);
////                        result.add("Rename Package " + from + " to " + to);
//                    } else {
////                        System.out.println(String.format("not rename %b %b %b %b", fromIsPackage, toIsPackage, existsBefore, existsAfter));
//                        result.add(refactoring);
//                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
//        System.out.println(String.format("insert into nrefactoring(cloneUrl, commit, refactoringType, description, truePositive) values "));
        System.out.println("Commit " + id + " " + cloneUrl.substring(0, cloneUrl.lastIndexOf('.')) + "/commit/" + commitId);
        for (Iterator<String> iter = result.iterator(); iter.hasNext();) {
            String refactoring = iter.next();
            System.out.println("  " + refactoring);
            //                System.out.println(refactoring);
//            RefactoringType refType = extractRefactoringType(refactoring);
//            System.out.print(String.format("  ('%s', '%s', '%s', '%s', '%d')", cloneUrl, commitId, refType.getDisplayName(), refactoring, tp ? 1 : 0));
//            System.out.println(iter.hasNext() ? "," : ";");
        }
    }

	private String getParent(String name) {
	    int lastIndexOf = name.lastIndexOf('.');
        return name.substring(0, Math.max(0, lastIndexOf));
	}

	private String getLeaf(String name) {
	    return name.substring(Math.max(0, name.lastIndexOf('.') + 1));
	}

	private boolean isPackageName(String name) {
	    return name.length() == 0 || Character.isLowerCase(getLeaf(name).charAt(0));
	}

	private boolean containsPackage(Set<String> packages, String name) {
	    String path = '/' + name.replace('.', '/');
	    for (String p : packages) {
	        if (p.endsWith(path)) {
	            return true;
	        }
	    }
	    return false;
	}
	
    private Set<String> getFolders(Repository rep, RevCommit commit) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
        PathSuffixFilter javaFilesFilter = PathSuffixFilter.create(".java");
        Set<String> folders = new HashSet<String>();
        try (TreeWalk treeWalk = new TreeWalk(rep)) {
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(javaFilesFilter);
            while (treeWalk.next()) {
                String path = "/" + treeWalk.getPathString();
                do {
                    path = path.substring(0, path.lastIndexOf("/"));
                    folders.add(path);
                } while (path.lastIndexOf("/") != -1);
            }
        }
        return folders;
    }
    
    private String normalize(String refactoring) {
        RefactoringType refType = extractRefactoringType(refactoring);    
        refactoring = normalizeSingle(refactoring);
        return refType.aggregate(refactoring);
    }
    
    /**
     * Remove generics type information.
     */
    private static String normalizeSingle(String refactoring) {
        StringBuilder sb = new StringBuilder();
        int openGenerics = 0;
        for (int i = 0; i < refactoring.length(); i++) {
            char c = refactoring.charAt(i);
            if (c == '<') {
                openGenerics++;
            }
            if (c == '\t') {
                c = ' ';
            }
            if (openGenerics == 0) {
                sb.append(c);
            }
            if (c == '>') {
                openGenerics--;
            }
        }
        return sb.toString();
    }
    
    private RefactoringType extractRefactoringType(String refactoring) {
        for (RefactoringType refType : RefactoringType.values()) {
            if (refactoring.startsWith(refType.getDisplayName())) {
                return refType;
            }
        }
        throw new RuntimeException("Unknown refactoring type: " + refactoring);
    }
}
