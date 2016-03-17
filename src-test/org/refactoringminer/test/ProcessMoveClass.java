package org.refactoringminer.test;

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
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.GitServiceImpl;

public class ProcessMoveClass {

    private final String tempDir = "c:/Users/danilofs/tmp";

    @Test
    public void testOne() throws Exception {
      
//     processTp("https://github.com/ratpack/ratpack.git", "2581441eda268c45306423dd4c515514d98a14a0", 
//    "Move Class ratpack.jackson.JacksonModule moved to ratpack.jackson.guice.JacksonModule");
//    
//        processTp("https://github.com/novoda/android-demos.git", "5cdabae35f0642e9fe243afe12e4c16b3378a150", 
//            "Move Class com.novoda.Base64DecoderException moved to com.novoda.demo.encryption.Base64DecoderException",
//            "Move Class com.novoda.Base64 moved to com.novoda.demo.encryption.Base64");
        
       // missing object
   processTp("https://github.com/spring-projects/spring-data-neo4j.git", "ef2a0d63393484975854fc08ad0fd3abc7dd76b0", 
   "Move Class org.springframework.data.neo4j.examples.friends.Person moved to org.springframework.data.neo4j.examples.friends.domain.Person",
   "Move Class org.springframework.data.neo4j.examples.friends.Friendship moved to org.springframework.data.neo4j.examples.friends.domain.Friendship",
   "Move Class org.springframework.data.neo4j.examples.friends.FriendContext moved to org.springframework.data.neo4j.examples.friends.context.FriendContext");


    }
    
    @Test
    public void testAll2() throws Exception {
        
        processFp("https://github.com/gradle/gradle.git", "36e73a93fcf2ed285721c88117c176d4a90e0327", 
            "Extract Method private createResolveContext() : DefaultJavaSourceSetResolveContext extracted from public getFiles() : Set<File> in class org.gradle.language.java.plugins.JavaLanguagePlugin.DependencyResolvingClasspath");

          processFp("https://github.com/GoClipse/goclipse.git", "e7b7d9ef3156abf5b47fb0b97210abbaca495a2e", 
            "Rename Method public handlePropertyChangeEvent(event PropertyChangeEvent) : void renamed to public handleTextPresentationPropertyChangeEvent(event PropertyChangeEvent) : void in class melnorme.lang.ide.ui.text.SimpleLangSourceViewerConfiguration");
          processTp("https://github.com/JetBrains/MPS.git", "2bcd05a827ead109a56cb1f79a83dcd332f42888", 
            "Inline Method public getLanguage(id SLanguageId, langName String, version int) : SLanguage inlined to public getLanguage(id SLanguageId, langName String) : SLanguage in class jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory");

          processTp("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", 
            "Extract Method private createEventLogTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
            "Extract Method private createDefaultMCCTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
            "Extract Method private createOpenCellIDTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
            "Extract Method private createCellTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
            "Extract Method private createLocationTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
            "Extract Method private createSilentSmsTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper",
            "Extract Method private createCellSignalTable(database SQLiteDatabase) : void extracted from public onCreate(database SQLiteDatabase) : void in class com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper");

          processTp("https://github.com/crashub/crash.git", "2801269c7e47bd6e243612654a74cee809d20959", 
            "Extract Method private convertPemKeyPair(pemKeyPair PEMKeyPair) : KeyPair extracted from public loadKeys() : Iterable<KeyPair> in class org.crsh.auth.FilePublicKeyProvider");


          processFp("https://github.com/springfox/springfox.git", "1bf09d2da6e6123d84ad872aea71c71f98646dfc", 
            "Extract Method private getResourceDescription(requestMappings List<RequestMappingContext>, context DocumentationContext) : String extracted from public scan(context DocumentationContext) : ApiListingReferenceScanResult in class springfox.documentation.spring.web.scanners.ApiListingReferenceScanner");
          processTp("https://github.com/vaadin/vaadin.git", "b0d5315e8ba95d099f93dc2d16339033a6525b59", 
            "Inline Method private remove() : void inlined to public testColExpandRatioIsForgotten() : void in class com.vaadin.ui.GridLayoutExpandRatioTest");

          processTp("https://github.com/glyptodon/guacamole-client.git", "ebb483320d971ff4d9e947309668f5da1fcd3d23", 
            "Move Attribute private EXPIRED_PASSWORD : CredentialsInfo from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private CONFIRM_NEW_PASSWORD : Field from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private CONFIRM_NEW_PASSWORD_PARAMETER : String from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private NEW_PASSWORD : Field from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private NEW_PASSWORD_PARAMETER : String from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private logger : Logger from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService");

          processTp("https://github.com/JetBrains/intellij-community.git", "7655200f58293e5a30bf8b3cbb29ebadae374564", 
            "Extract Method private checkRemap() : void extracted from public getOffset() : int in class com.intellij.debugger.engine.RemappedSourcePosition",
            "Extract Method private checkRemap() : void extracted from public getLine() : int in class com.intellij.debugger.engine.RemappedSourcePosition");

          processTp("https://github.com/oblac/jodd.git", "722ef9156896248ef3fbe83adde0f6ff8f46856a", 
            "Extract Method protected resolveFormEncoding() : String extracted from protected formBuffer() : Buffer in class jodd.http.HttpBase");

          processTp("https://github.com/jeeeyul/eclipse-themes.git", "72f61ec9b85a740fd09d10ad711e275d2ec2e564", 
            "Move Class net.jeeeyul.eclipse.themes.test.e4app.TestView moved to net.jeeeyul.eclipse.themes.test.e4app.views.TestView",
            "Move Class net.jeeeyul.eclipse.themes.test.e4app.SplashHandler moved to net.jeeeyul.eclipse.themes.test.e4app.handlers.SplashHandler",
            "Move Class net.jeeeyul.eclipse.themes.test.e4app.AboutHandler moved to net.jeeeyul.eclipse.themes.test.e4app.handlers.AboutHandler");


          processFp("https://github.com/crate/crate.git", "aa868fea198beecc42b01fee91414091ae38da25", 
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeProcessExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeOsExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeOsCpuExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeSysExpression : NodeSysExpression from class io.crate.metadata.GlobalReferenceResolver to class io.crate.operation.collect.MapSideDataCollectOperation");
          processTp("https://github.com/realm/realm-java.git", "6cf596df183b3c3a38ed5dd9bb3b0100c6548ebb", 
            "Extract Method private showStatus(txt String) : void extracted from private showStatus(realm Realm) : void in class io.realm.examples.realmmigrationexample.MigrationExampleActivity");

          processTp("https://github.com/JetBrains/intellij-community.git", "7a4dab88185553bd09e827839fdf52e870ef7088", 
            "Extract Method private getDocumentationText(sourceEditorText String) : String extracted from public testImagesInsideJavadocJar() : void in class com.intellij.codeInsight.JavaExternalDocumentationTest",
            "Extract Method private getDataFile(name String) : File extracted from private getJarFile(name String) : VirtualFile in class com.intellij.codeInsight.JavaExternalDocumentationTest");

          processTp("https://github.com/fabric8io/fabric8.git", "8127b21a220ca677c4e59961d019e7753da7ea6e", 
            "Extract Method protected getProbe(prefix String) : Probe extracted from protected getLivenessProbe() : Probe in class io.fabric8.maven.JsonMojo");

          processTp("https://github.com/puniverse/quasar.git", "c22d40fab8dfe4c5cad9ba582caf0855ff64b324", 
            "Extract Method protected failedSubscribe(s Subscription) : void extracted from public onSubscribe(s Subscription) : void in class co.paralleluniverse.strands.channels.reactivestreams.ChannelSubscriber");

          processTp("https://github.com/gradle/gradle.git", "04bcfe98dbe7b05e508559930c21379ece845732", 
            "Extract Interface org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ResolvedArtifactsContainer from classes [org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.ResolvedArtifactResults]");

          processTp("https://github.com/rstudio/rstudio.git", "cb49e436b9d7ee55f2531ebc2ef1863f5c9ba9fe", 
            "Extract Method protected setMaxHeight(maxHeight int) : void extracted from protected wrapMenuBar(menuBar ToolbarMenuBar) : Widget in class org.rstudio.core.client.widget.ScrollableToolbarPopupMenu");


          processFp("https://github.com/crate/crate.git", "e79d6f76e789e7c626ae177e51617aa2175f1678", 
            "Move Attribute private networkService : NetworkService from class io.crate.operation.reference.sys.node.NodeNetworkTCPExpression.TCPPacketsExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private networkService : NetworkService from class io.crate.operation.reference.sys.node.NodeNetworkTCPExpression.TCPConnectionsExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private jvmService : JvmService from class io.crate.operation.reference.sys.node.NodeHeapExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private osService : OsService from class io.crate.operation.reference.sys.node.NodeMemoryExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private osService : OsService from class io.crate.operation.reference.sys.node.NodeLoadExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeProcessExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeOsExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeOsCpuExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeSysExpression : NodeSysExpression from class io.crate.metadata.GlobalReferenceResolver to class io.crate.operation.collect.MapSideDataCollectOperation");
          processTp("https://github.com/SonarSource/sonarqube.git", "abbf32571232db81a5343db17a933a9ce6923b44", 
            "Move Class org.sonar.server.notifications.email.EmailNotificationChannel moved to org.sonar.server.notification.email.EmailNotificationChannel",
            "Move Class org.sonar.server.notifications.email.AlertsEmailTemplate moved to org.sonar.server.notification.email.AlertsEmailTemplate",
            "Move Class org.sonar.server.notifications.NotificationService moved to org.sonar.server.notification.NotificationService",
            "Move Class org.sonar.server.notifications.NotificationCenter moved to org.sonar.server.notification.NotificationCenter",
            "Move Class org.sonar.server.notifications.email.EmailNotificationChannelTest moved to org.sonar.server.notification.email.EmailNotificationChannelTest",
            "Move Class org.sonar.server.notifications.email.AlertsEmailTemplateTest moved to org.sonar.server.notification.email.AlertsEmailTemplateTest",
            "Move Class org.sonar.server.notifications.NotificationTest moved to org.sonar.server.notification.NotificationTest",
            "Move Class org.sonar.server.notifications.NotificationServiceTest moved to org.sonar.server.notification.NotificationServiceTest",
            "Move Class org.sonar.server.notifications.NotificationCenterTest moved to org.sonar.server.notification.NotificationCenterTest");

          processTp("https://github.com/JetBrains/intellij-community.git", "7ed3f273ab0caf0337c22f0b721d51829bb0c877", 
            "Extract Method private addCoursesFromStepic(result List<CourseInfo>, pageNumber int) : boolean extracted from public getCourses() : List<CourseInfo> in class com.jetbrains.edu.stepic.EduStepicConnector");


          processFp("https://github.com/SonarSource/sonarqube.git", "83b87b277e2d86d6b8c4bac05352b5cbedb49966", 
            "Move Method private getFileQualifier(reportComponent Component) : String from class org.sonar.server.computation.step.PersistComponentsAndSnapshotsStep.PersisComponentExecutor to private getFileQualifier(reportComponent Component) : String from class org.sonar.server.computation.step.PersistComponentsAndSnapshotsStep",
            "Move Method private updateComponent(existingComponent ComponentDto, newComponent ComponentDto) : boolean from class org.sonar.server.computation.step.PersistComponentsAndSnapshotsStep.PersisComponentExecutor to private updateComponent(existingComponent ComponentDto, newComponent ComponentDto) : boolean from class org.sonar.server.computation.step.PersistComponentsAndSnapshotsStep",
            "Move Class org.sonar.server.computation.step.PersistComponentsAndSnapshotsStep.PersisComponentExecutor.PersistedComponent moved to org.sonar.server.computation.step.PersistComponentsAndSnapshotsStep.PersistedComponent");

          processFp("https://github.com/SonarSource/sonarqube.git", "1b02908ab2ab3993c51e6df524f5d6bacf1e091d", 
            "Extract Method private findByDate(index int, date Date) : Period extracted from private resolve(index int, property String) : Period in class org.sonar.server.computation.step.FeedPeriodsStep.PeriodResolver");

          processFp("https://github.com/SonarSource/sonarqube.git", "bdf8ed8afa37620923b040c1d1246c9fb8b17d08", 
            "Move Class org.sonar.server.computation.period.PeriodsRepository.PeriodResolver moved to org.sonar.server.computation.step.FeedPeriodsStep.PeriodResolver");
          processTp("https://github.com/Athou/commafeed.git", "18a7bd1fd1a83b3b8d1b245e32f78c0b4443b7a7", 
            "Extract Method private fetch(url String) : byte[] extracted from public fetch(feed Feed) : byte[] in class com.commafeed.backend.favicon.DefaultFaviconFetcher");

          processTp("https://github.com/SonarSource/sonarqube.git", "4a2247c24efee48de53ca07302b6810ab7205621", 
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

          processTp("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "4ef35268bb96bb78b2dc698fa68e7ce763cde32e", 
            "Pull Up Method public setColumn(column Integer) : void from class org.broadleafcommerce.openadmin.dto.BasicFieldMetadata to public setColumn(column Integer) : void from class org.broadleafcommerce.openadmin.dto.FieldMetadata",
            "Pull Up Method public getColumn() : Integer from class org.broadleafcommerce.openadmin.dto.BasicFieldMetadata to public getColumn() : Integer from class org.broadleafcommerce.openadmin.dto.FieldMetadata",
            "Pull Up Attribute private column : Integer from class org.broadleafcommerce.openadmin.dto.BasicFieldMetadata to class org.broadleafcommerce.openadmin.dto.FieldMetadata");

          processTp("https://github.com/dreamhead/moco.git", "55ffa2f3353c5dc77fe6b790e5e045b76a07a772", 
            "Pull Up Method protected onRequestAttached(matcher RequestMatcher) : HttpResponseSetting from class com.github.dreamhead.moco.internal.ActualHttpServer to protected onRequestAttached(matcher RequestMatcher) : HttpResponseSetting from class com.github.dreamhead.moco.internal.HttpConfiguration",
            "Pull Up Method public redirectTo(url String) : HttpResponseSetting from class com.github.dreamhead.moco.internal.ActualHttpServer to public redirectTo(url String) : HttpResponseSetting from class com.github.dreamhead.moco.internal.HttpConfiguration");

          processTp("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", 
            "Extract Method public setMaxRequestsPerConnection(distance HostDistance, newMaxRequests int) : PoolingOptions extracted from public setMaxSimultaneousRequestsPerHostThreshold(distance HostDistance, newMaxRequests int) : PoolingOptions in class com.datastax.driver.core.PoolingOptions",
            "Extract Method public getMaxRequestsPerConnection(distance HostDistance) : int extracted from public getMaxSimultaneousRequestsPerHostThreshold(distance HostDistance) : int in class com.datastax.driver.core.PoolingOptions",
            "Extract Method public setNewConnectionThreshold(distance HostDistance, newValue int) : PoolingOptions extracted from public setMaxSimultaneousRequestsPerConnectionThreshold(distance HostDistance, newMaxSimultaneousRequests int) : PoolingOptions in class com.datastax.driver.core.PoolingOptions",
            "Extract Method public getNewConnectionThreshold(distance HostDistance) : int extracted from public getMaxSimultaneousRequestsPerConnectionThreshold(distance HostDistance) : int in class com.datastax.driver.core.PoolingOptions");

          processTp("https://github.com/JetBrains/intellij-community.git", "cc0eaf7faa408a04b68e2b5820f3ebcc75420b5b", 
            "Extract Method private canBinaryExpressionBeUnboxed(lhs PsiExpression, rhs PsiExpression) : boolean extracted from private canBeUnboxed(expression PsiExpression) : boolean in class com.siyeh.ig.migration.UnnecessaryBoxingInspection.UnnecessaryBoxingVisitor");


          processFp("https://github.com/datastax/java-driver.git", "79fbfa467e956d339686d4fb77cfab3564cb9f7a", 
            "Move Method protected Result(kind Kind) from class com.datastax.driver.core.Responses to protected Result(kind Kind) from class com.datastax.driver.core.Responses.Result",
            "Extract Method package appendValue(value Object, codecRegistry CodecRegistry, sb StringBuilder, variables List<Object>) : StringBuilder extracted from package appendValue(value Object, sb StringBuilder, variables List<ByteBuffer>) : StringBuilder in class com.datastax.driver.core.querybuilder.Utils",
            "Move Class com.datastax.driver.core.AuthSuccess moved to com.datastax.driver.core.Responses.AuthSuccess",
            "Move Class com.datastax.driver.core.AuthChallenge moved to com.datastax.driver.core.Responses.AuthChallenge",
            "Move Class com.datastax.driver.core.Event moved to com.datastax.driver.core.Responses.Event",
            "Move Class com.datastax.driver.core.Prepared moved to com.datastax.driver.core.Responses.Result.Prepared",
            "Move Class com.datastax.driver.core.Responses.SetKeyspace moved to com.datastax.driver.core.Responses.Result.SetKeyspace",
            "Move Class com.datastax.driver.core.Responses.Void moved to com.datastax.driver.core.Responses.Result.Void",
            "Move Attribute public kind : Kind from class com.datastax.driver.core.Responses to class com.datastax.driver.core.Responses.Result");
          processTp("https://github.com/JetBrains/intellij-community.git", "04397f41107bd6de41b31d45a4e8e2ed65628bbe", 
            "Inline Method private checkForTestRoots(srcModule Module, testFolders Set<VirtualFile>, processed Set<Module>) : void inlined to protected checkForTestRoots(srcModule Module, testFolders Set<VirtualFile>) : void in class com.intellij.testIntegration.createTest.CreateTestAction");


          processFp("https://github.com/crate/crate.git", "b683ae9b6a64f0ec31be74ad2546aba71e3fd41e", 
            "Move Attribute private networkService : NetworkService from class io.crate.operation.reference.sys.node.NodeNetworkTCPExpression.TCPPacketsExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private networkService : NetworkService from class io.crate.operation.reference.sys.node.NodeNetworkTCPExpression.TCPConnectionsExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private jvmService : JvmService from class io.crate.operation.reference.sys.node.NodeHeapExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private osService : OsService from class io.crate.operation.reference.sys.node.NodeMemoryExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private osService : OsService from class io.crate.operation.reference.sys.node.NodeLoadExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeProcessExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeOsExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeService : NodeService from class io.crate.operation.reference.sys.node.NodeOsCpuExpression to class io.crate.operation.reference.sys.node.NodeSysExpression",
            "Move Attribute private nodeSysExpression : NodeSysExpression from class io.crate.metadata.GlobalReferenceResolver to class io.crate.operation.collect.MapSideDataCollectOperation");

          processFp("https://github.com/JetBrains/intellij-plugins.git", "8194d3e78d7b801852e18cd47e367f4a20e977e4", 
            "Extract Method private forLoopParts_2_1_0_2_0(b PsiBuilder, l int) : boolean extracted from private forLoopParts_0(b PsiBuilder, l int) : boolean in class com.jetbrains.lang.dart.DartParser",
            "Extract Method private forLoopParts_2_1_0(b PsiBuilder, l int) : boolean extracted from private forLoopParts_0(b PsiBuilder, l int) : boolean in class com.jetbrains.lang.dart.DartParser",
            "Extract Method private forLoopParts_1_1_0_2_0(b PsiBuilder, l int) : boolean extracted from private forLoopParts_0(b PsiBuilder, l int) : boolean in class com.jetbrains.lang.dart.DartParser",
            "Extract Method private forLoopParts_1_1_0(b PsiBuilder, l int) : boolean extracted from private forLoopParts_0(b PsiBuilder, l int) : boolean in class com.jetbrains.lang.dart.DartParser");
          processTp("https://github.com/raphw/byte-buddy.git", "372f4ae6cebcd664e3b43cade356d1df233f6467", 
            "Move Attribute package ARRAY_MODIFIERS : int from class net.bytebuddy.description.type.TypeDescription.ArrayProjection to class net.bytebuddy.description.type.TypeDescription");
          processFp("https://github.com/raphw/byte-buddy.git", "372f4ae6cebcd664e3b43cade356d1df233f6467", 
            "Extract Interface net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken from classes [net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.ForTypeVariable.Formal, net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.ForParameterizedType.Nested]",
            "Extract Interface net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.Resolution from classes [net.bytebuddy.pool.TypePool.AbstractBase.ArrayTypeResolution]",
            "Move Class net.bytebuddy.pool.TokenizedGenericType moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.TokenizedGenericType",
            "Move Class net.bytebuddy.pool.LazyTypeList moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.LazyTypeList",
            "Move Class net.bytebuddy.pool.LazyMethodDescription moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.LazyMethodDescription",
            "Move Class net.bytebuddy.pool.LazyFieldDescription moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.LazyFieldDescription",
            "Move Class net.bytebuddy.pool.LazyPackageDescription moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.LazyPackageDescription",
            "Move Class net.bytebuddy.pool.LazyAnnotationDescription moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.LazyAnnotationDescription",
            "Move Class net.bytebuddy.pool.ForParameterizedType moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.ForParameterizedType",
            "Move Class net.bytebuddy.pool.ForLowerBoundWildcard moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.ForLowerBoundWildcard",
            "Move Class net.bytebuddy.pool.ForArray moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.ForArray",
            "Move Class net.bytebuddy.pool.ForTypeVariable moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.ForTypeVariable",
            "Move Class net.bytebuddy.pool.ForRawType moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.ForRawType",
            "Move Class net.bytebuddy.pool.GenericTypeToken.Resolution.ForField moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.Resolution.ForField",
            "Move Class net.bytebuddy.pool.GenericTypeToken.Resolution.ForMethod moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.Resolution.ForMethod",
            "Move Class net.bytebuddy.pool.GenericTypeToken.Resolution.ForType moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.Resolution.ForType",
            "Move Class net.bytebuddy.pool.MethodToken moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.MethodToken",
            "Move Class net.bytebuddy.pool.FieldToken moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.FieldToken",
            "Move Class net.bytebuddy.pool.AnnotationToken moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.AnnotationToken",
            "Move Class net.bytebuddy.pool.LazyTypeDescription.DeclaredInMethod moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod",
            "Move Class net.bytebuddy.pool.LazyTypeDescription.DeclaredInType moved to net.bytebuddy.pool.TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType",
            "Move Class net.bytebuddy.pool.LazyTypeDescription moved to net.bytebuddy.pool.TypePool.LazyTypeDescription",
            "Move Class net.bytebuddy.pool.TypeExtractor moved to net.bytebuddy.pool.TypePool.Default.TypeExtractor",
            "Move Class net.bytebuddy.pool.GenericTypeExtractor moved to net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor",
            "Move Class net.bytebuddy.pool.GenericTypeRegistrant moved to net.bytebuddy.pool.TypePool.Default.GenericTypeRegistrant",
            "Move Class net.bytebuddy.pool.ParameterBag moved to net.bytebuddy.pool.TypePool.Default.ParameterBag",
            "Move Class net.bytebuddy.pool.Default.ForArrayType moved to net.bytebuddy.pool.TypePool.Default.ComponentTypeLocator.ForArrayType",
            "Move Class net.bytebuddy.pool.Default.ForAnnotationProperty moved to net.bytebuddy.pool.TypePool.Default.ComponentTypeLocator.ForAnnotationProperty",
            "Move Class net.bytebuddy.dynamic.Default moved to net.bytebuddy.dynamic.DynamicType.Default",
            "Move Class net.bytebuddy.dynamic.Unloaded moved to net.bytebuddy.dynamic.DynamicType.Unloaded",
            "Move Class net.bytebuddy.dynamic.Loaded moved to net.bytebuddy.dynamic.DynamicType.Loaded",
            "Move Class net.bytebuddy.dynamic.AbstractBase moved to net.bytebuddy.dynamic.DynamicType.Builder.AbstractBase",
            "Move Class net.bytebuddy.dynamic.FieldAnnotationTarget moved to net.bytebuddy.dynamic.DynamicType.Builder.FieldAnnotationTarget",
            "Move Class net.bytebuddy.description.type.generic.LazyProjection.OfLegacyVmMethodParameter moved to net.bytebuddy.description.type.generic.GenericTypeDescription.LazyProjection.OfLegacyVmMethodParameter",
            "Move Class net.bytebuddy.description.type.generic.LazyProjection.OfLegacyVmConstructorParameter moved to net.bytebuddy.description.type.generic.GenericTypeDescription.LazyProjection.OfLegacyVmConstructorParameter",
            "Move Class net.bytebuddy.description.type.generic.LazyProjection.OfLoadedParameter moved to net.bytebuddy.description.type.generic.GenericTypeDescription.LazyProjection.OfLoadedParameter",
            "Move Class net.bytebuddy.description.type.generic.LazyProjection.OfLoadedFieldType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.LazyProjection.OfLoadedFieldType",
            "Move Class net.bytebuddy.description.type.generic.LazyProjection.OfLoadedReturnType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.LazyProjection.OfLoadedReturnType",
            "Move Class net.bytebuddy.description.type.generic.LazyProjection.OfLoadedSuperType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.LazyProjection.OfLoadedSuperType",
            "Move Class net.bytebuddy.description.type.generic.ForTypeVariable.Latent moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForTypeVariable.Latent",
            "Move Class net.bytebuddy.description.type.generic.Visitor moved to net.bytebuddy.description.type.generic.GenericTypeDescription.Visitor",
            "Move Class net.bytebuddy.description.type.generic.ForGenericArray.OfLoadedType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForGenericArray.OfLoadedType",
            "Move Class net.bytebuddy.description.type.generic.ForGenericArray.Latent moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForGenericArray.Latent",
            "Move Class net.bytebuddy.description.type.generic.ForWildcardType.OfLoadedType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForWildcardType.OfLoadedType",
            "Move Class net.bytebuddy.description.type.generic.ForWildcardType.Latent moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForWildcardType.Latent",
            "Move Class net.bytebuddy.description.type.generic.ForParameterizedType.OfLoadedType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForParameterizedType.OfLoadedType",
            "Move Class net.bytebuddy.description.type.generic.ForParameterizedType.Latent moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForParameterizedType.Latent",
            "Move Class net.bytebuddy.description.type.generic.ForTypeVariable.OfLoadedType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForTypeVariable.OfLoadedType",
            "Move Class net.bytebuddy.dynamic.TargetType.ForType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.Visitor.Substitutor.ForRawType.TypeVariableProxy.ForType",
            "Move Class net.bytebuddy.dynamic.scaffold.InstrumentedType.Simple moved to net.bytebuddy.dynamic.scaffold.InstrumentedType.TypeInitializer.Simple",
            "Move Class net.bytebuddy.dynamic.scaffold.AbstractBase moved to net.bytebuddy.dynamic.scaffold.InstrumentedType.AbstractBase",
            "Move Class net.bytebuddy.dynamic.scaffold.TypeWriter.FieldPool.Simple moved to net.bytebuddy.dynamic.scaffold.TypeWriter.FieldPool.Entry.Simple",
            "Move Class net.bytebuddy.dynamic.scaffold.AbstractDefiningEntry moved to net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry.AbstractDefiningEntry",
            "Move Class net.bytebuddy.dynamic.scaffold.ForImplementation moved to net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry.ForImplementation",
            "Move Class net.bytebuddy.dynamic.scaffold.ForAbstractMethod moved to net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry.ForAbstractMethod",
            "Move Class net.bytebuddy.dynamic.scaffold.ForAnnotationDefaultValue moved to net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue",
            "Move Class net.bytebuddy.dynamic.scaffold.Default moved to net.bytebuddy.dynamic.scaffold.TypeWriter.Default",
            "Move Class net.bytebuddy.dynamic.scaffold.Default.ValidatingClassVisitor.ValidatingMethodVisitor moved to net.bytebuddy.dynamic.scaffold.TypeWriter.Default.ValidatingClassVisitor.ValidatingMethodVisitor",
            "Move Class net.bytebuddy.dynamic.scaffold.ForInlining moved to net.bytebuddy.dynamic.scaffold.TypeWriter.Default.ForInlining",
            "Move Class net.bytebuddy.dynamic.scaffold.ForCreation moved to net.bytebuddy.dynamic.scaffold.TypeWriter.Default.ForCreation",
            "Move Class net.bytebuddy.implementation.FieldAccessor.Factory moved to net.bytebuddy.implementation.FieldAccessor.FieldLocator.Factory",
            "Move Class net.bytebuddy.implementation.ForGivenType moved to net.bytebuddy.implementation.FieldAccessor.FieldLocator.ForGivenType",
            "Move Class net.bytebuddy.implementation.AssignerConfigurable moved to net.bytebuddy.implementation.FieldAccessor.AssignerConfigurable",
            "Move Class net.bytebuddy.implementation.OwnerTypeLocatable moved to net.bytebuddy.implementation.FieldAccessor.OwnerTypeLocatable",
            "Move Class net.bytebuddy.implementation.FieldDefinable moved to net.bytebuddy.implementation.FieldAccessor.FieldDefinable",
            "Move Class net.bytebuddy.implementation.ForUnnamedField moved to net.bytebuddy.implementation.FieldAccessor.ForUnnamedField",
            "Move Class net.bytebuddy.implementation.ForNamedField moved to net.bytebuddy.implementation.FieldAccessor.ForNamedField",
            "Move Class net.bytebuddy.implementation.ForNamedField.FieldDefiner moved to net.bytebuddy.implementation.FieldAccessor.ForNamedField.PreparationHandler.FieldDefiner",
            "Move Class net.bytebuddy.implementation.Appender moved to net.bytebuddy.implementation.FieldAccessor.Appender",
            "Move Class net.bytebuddy.pool.TypePool.Simple moved to net.bytebuddy.pool.TypePool.CacheProvider.Simple",
            "Move Class net.bytebuddy.pool.AbstractBase moved to net.bytebuddy.pool.TypePool.AbstractBase",
            "Move Class net.bytebuddy.pool.Default moved to net.bytebuddy.pool.TypePool.Default",
            "Extract Superclass net.bytebuddy.description.type.generic.GenericTypeDescription.Visitor.Substitutor.ForRawType.TypeVariableProxy.ForMethod from classes [net.bytebuddy.pool.TypePool.LazyTypeDescription.GenericTypeToken.Resolution.ForMethod.Tokenized]");

          processFp("https://github.com/eclipse/jetty.project.git", "b18adb525f1784714a3b0548df1b9dfde0eda44a", 
            "Move Class org.eclipse.jetty.util.thread.SpinLock.Lock moved to org.eclipse.jetty.util.thread.Locker.Lock");
          processTp("https://github.com/JetBrains/intellij-community.git", "138911ce88b05039242b8d1b2bb5b7a59008f5ee", 
            "Extract Method public getHTMLEditorKit(noGapsBetweenParagraphs boolean) : HTMLEditorKit extracted from public getHTMLEditorKit() : HTMLEditorKit in class com.intellij.util.ui.UIUtil");


          processFp("https://github.com/raphw/byte-buddy.git", "5477f5eb12650e7874af6ac7c08d008415366ae4", 
            "Move Class net.bytebuddy.description.type.generic.LazyProjection moved to net.bytebuddy.description.type.generic.GenericTypeDescription.LazyProjection",
            "Move Class net.bytebuddy.description.type.generic.ForTypeVariable moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForTypeVariable",
            "Move Class net.bytebuddy.description.type.generic.ForParameterizedType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForParameterizedType",
            "Move Class net.bytebuddy.description.type.generic.ForWildcardType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForWildcardType",
            "Move Class net.bytebuddy.description.type.generic.ForGenericArray moved to net.bytebuddy.description.type.generic.GenericTypeDescription.ForGenericArray",
            "Move Class net.bytebuddy.description.type.generic.Visitor.Substitutor.ForRawType.ForMethod moved to net.bytebuddy.description.type.generic.GenericTypeDescription.Visitor.Substitutor.ForRawType.TypeVariableProxy.ForMethod",
            "Move Class net.bytebuddy.description.type.generic.Visitor.Substitutor.ForRawType.ForType moved to net.bytebuddy.description.type.generic.GenericTypeDescription.Visitor.Substitutor.ForRawType.TypeVariableProxy.ForType",
            "Move Class net.bytebuddy.description.type.generic.Visitor moved to net.bytebuddy.description.type.generic.GenericTypeDescription.Visitor");

          processFp("https://github.com/orientechnologies/orientdb.git", "31f0b5a8f3f0f2cfc7e33c810116a904381ee98c", 
            "Extract Method private calculateMatch(estimatedRootEntries Map<String,Long>, matchContext MatchContext, aliasClasses Map<String,String>, aliasFilters Map<String,OWhereClause>) : OResultSet extracted from public execute(iArgs Map<Object,Object>) : Object in class com.orientechnologies.orient.core.sql.parser.OMatchStatement");

          processFp("https://github.com/elastic/elasticsearch.git", "761326283f498361cc14582acffd8c2a15c92428", 
            "Extract Method protected maybeUpgrade3xSegments(store Store) : void extracted from public InternalEngine(engineConfig EngineConfig) in class org.elasticsearch.index.engine.InternalEngine");
          processTp("https://github.com/SonarSource/sonarqube.git", "c55a8c3761e9aae9f375d312c14b1bbb9ee9c0fa", 
            "Move Method private createComponentDto(reportComponent Component, component Component) : ComponentDto from class org.sonar.server.computation.step.PersistComponentsStep to private createComponentDto(reportComponent Component, component Component) : ComponentDto from class org.sonar.server.computation.step.PersistComponentsStep.PersisComponent");


          processFp("https://github.com/JetBrains/intellij-community.git", "71fda71e1d0b755cc3f06473c99f6e69810e7677", 
            "Move Class git4idea.rebase.MyCopyProvider moved to git4idea.rebase.GitRebaseEditor.MyCopyProvider",
            "Move Class git4idea.rebase.MoveUpDownActionListener moved to git4idea.rebase.GitRebaseEditor.MoveUpDownActionListener",
            "Move Class git4idea.rebase.MyDiffAction moved to git4idea.rebase.GitRebaseEditor.MyDiffAction");

          processFp("https://github.com/liferay/liferay-portal.git", "ba749a31ef4781a4d4176d2528bf8879c11b8720", 
            "Extract Method public create(field String, value String, occur String) : BooleanClause<Query> extracted from public create(searchContext SearchContext, field String, value String, occur String) : BooleanClause<Query> in class com.liferay.portal.kernel.search.generic.BooleanClauseFactoryImpl",
            "Extract Method public create(query Query, occur String) : BooleanClause<Query> extracted from public create(searchContext SearchContext, query Query, occur String) : BooleanClause<Query> in class com.liferay.portal.kernel.search.generic.BooleanClauseFactoryImpl");
          processTp("https://github.com/glyptodon/guacamole-client.git", "ce1f3d07976de31aed8f8189ec5e1a6453f4b580", 
            "Move Attribute private EXPIRED_PASSWORD : CredentialsInfo from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private CONFIRM_NEW_PASSWORD : Field from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private CONFIRM_NEW_PASSWORD_PARAMETER : String from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private NEW_PASSWORD : Field from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private NEW_PASSWORD_PARAMETER : String from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService",
            "Move Attribute private logger : Logger from class org.glyptodon.guacamole.auth.jdbc.user.UserContextService to class org.glyptodon.guacamole.auth.jdbc.user.UserService");

          processTp("https://github.com/puniverse/quasar.git", "56d4b999e8be70be237049708f019c278c356e71", 
            "Inline Method public popMethod(catchAll boolean) : void inlined to public popMethod() : void in class co.paralleluniverse.fibers.Stack",
            "Inline Method public pushMethod(entry int, numSlots int, method String, sourceLine int) : void inlined to public pushMethod(entry int, numSlots int) : void in class co.paralleluniverse.fibers.Stack",
            "Inline Method public checkInstrumentation(exc boolean) : boolean inlined to public checkInstrumentation() : boolean in class co.paralleluniverse.fibers.Fiber",
            "Inline Method package verifySuspend(current Fiber, exc boolean) : Fiber inlined to package verifySuspend(current Fiber) : Fiber in class co.paralleluniverse.fibers.Fiber");


          processFp("https://github.com/google/guava.git", "cc3b0f8dc48497a2911dfe31f60fe186b3fed8d4",
              // strange bug: Move Method package getBestComparator() : Comparator<byte[] from class com.google.common.primitives.UnsignedBytes to package getBestComparator() : Comparator<byte[] from class com.google.common.primitives.UnsignedBytes.LexicographicalComparatorHolder
            "Move Method package getBestComparator() : Comparator from class com.google.common.primitives.UnsignedBytes to package getBestComparator() : Comparator from class com.google.common.primitives.UnsignedBytes.LexicographicalComparatorHolder",
            "Move Class com.google.common.collect.ComputingMapAdapter moved to com.google.common.collect.MapMaker.ComputingMapAdapter",
            "Move Class com.google.common.collect.NullComputingConcurrentMap moved to com.google.common.collect.MapMaker.NullComputingConcurrentMap",
            "Move Class com.google.common.collect.NullConcurrentMap moved to com.google.common.collect.MapMaker.NullConcurrentMap",
            "Move Class com.google.common.base.NotPredicate.ContainsPatternFromStringPredicate moved to com.google.common.base.Predicates.ContainsPatternFromStringPredicate",
            "Move Class com.google.common.base.NotPredicate.ContainsPatternPredicate moved to com.google.common.base.Predicates.ContainsPatternPredicate",
            "Move Class com.google.common.base.NotPredicate.AssignableFromPredicate moved to com.google.common.base.Predicates.AssignableFromPredicate",
            "Move Class com.google.common.base.NotPredicate.InstanceOfPredicate moved to com.google.common.base.Predicates.InstanceOfPredicate",
            "Move Class com.google.common.base.NotPredicate.CompositionPredicate moved to com.google.common.base.Predicates.CompositionPredicate",
            "Move Class com.google.common.base.NotPredicate.InPredicate moved to com.google.common.base.Predicates.InPredicate",
            "Move Class com.google.common.base.NotPredicate.IsEqualToPredicate moved to com.google.common.base.Predicates.IsEqualToPredicate",
            "Move Class com.google.common.base.NotPredicate.OrPredicate moved to com.google.common.base.Predicates.OrPredicate",
            "Move Class com.google.common.base.NotPredicate.AndPredicate moved to com.google.common.base.Predicates.AndPredicate");
          processTp("https://github.com/hibernate/hibernate-orm.git", "2b89553db5081fe4e55b7b34d636d0ea2acf71c5", 
            "Extract Method private categorizeAnnotatedClass(annotatedClass Class, attributeConverterManager AttributeConverterManager) : void extracted from public AnnotationMetadataSourceProcessorImpl(sources MetadataSources, rootMetadataBuildingContext MetadataBuildingContextRootImpl, jandexView IndexView) in class org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl");

          processTp("https://github.com/apache/cassandra.git", "446e2537895c15b404a74107069a12f3fc404b15", 
            "Move Class org.apache.cassandra.hadoop.BulkRecordWriter.NullOutputHandler moved to org.apache.cassandra.hadoop.cql3.CqlBulkRecordWriter.NullOutputHandler",
            "Move Class org.apache.cassandra.hadoop.AbstractColumnFamilyInputFormat.SplitCallable moved to org.apache.cassandra.hadoop.cql3.CqlInputFormat.SplitCallable");

          processTp("https://github.com/amplab/tachyon.git", "0ba343846f21649e29ffc600f30a7f3e463fb24c", 
            "Extract Superclass tachyon.worker.block.meta.BlockMetaBase from classes [tachyon.worker.block.meta.BlockMeta, tachyon.worker.block.meta.TempBlockMeta]");

          processTp("https://github.com/raphw/byte-buddy.git", "f1dfb66a368760e77094ac1e3860b332cf0e4eb5", 
            "Pull Up Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.Explicit to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase",
            "Pull Up Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.ForLoadedExecutable to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase");
          processFp("https://github.com/raphw/byte-buddy.git", "f1dfb66a368760e77094ac1e3860b332cf0e4eb5", 
            "Move Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.ForLoadedExecutable.OfLegacyVmConstructor to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase",
            "Move Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.ForLoadedExecutable.OfLegacyVmMethod to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase");

          processFp("https://github.com/realm/realm-java.git", "09168d60ea878aab4fe3c3592c803ee451c3c044", 
            "Extract Method private validateAgainstExistingConfigurations(newConfiguration RealmConfiguration) : void extracted from private createAndValidate(config RealmConfiguration, validateSchema boolean, autoRefresh boolean) : Realm in class io.realm.Realm");
          processTp("https://github.com/kuujo/copycat.git", "19a49f8f36b2f6d82534dc13504d672e41a3a8d1", 
            "Pull Up Attribute protected transition : boolean from class net.kuujo.copycat.raft.state.ActiveState to class net.kuujo.copycat.raft.state.PassiveState",
            "Pull Up Method private applyIndex(globalIndex long) : void from class net.kuujo.copycat.raft.state.ActiveState to private applyIndex(globalIndex long) : void from class net.kuujo.copycat.raft.state.PassiveState",
            "Pull Up Method private applyCommits(commitIndex long) : CompletableFuture<Void> from class net.kuujo.copycat.raft.state.ActiveState to private applyCommits(commitIndex long) : CompletableFuture<Void> from class net.kuujo.copycat.raft.state.PassiveState",
            "Pull Up Method private doAppendEntries(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private doAppendEntries(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState",
            "Pull Up Method private doCheckPreviousEntry(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private doCheckPreviousEntry(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState",
            "Pull Up Method private handleAppend(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private handleAppend(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState");

          processTp("https://github.com/elastic/elasticsearch.git", "ff9041dc486adf0a8dec41f80bbfbdd49f97016a", 
            "Extract Method protected buildFQuery(builder XContentBuilder, params Params) : void extracted from protected doXContent(builder XContentBuilder, params Params) : void in class org.elasticsearch.index.query.QueryFilterBuilder");

          processTp("https://github.com/JetBrains/intellij-community.git", "484038e916dc40bf87eca10c77889d79eca96c4d", 
            "Extract Method public removeNodes(paths Collection<TreePath>) : void extracted from public removeNode(nodePath TreePath) : void in class com.intellij.compiler.options.AnnotationProcessorsPanel.MyTreeModel");

          processTp("https://github.com/SonarSource/sonarqube.git", "7668c875dfa7240b1ec08eb60b42107bae1b4cd3", 
            "Move Method private createComponentDto(reportComponent Component, component Component) : ComponentDto from class org.sonar.server.computation.step.PersistComponentsStep to private createComponentDto(reportComponent Component, component Component) : ComponentDto from class org.sonar.server.computation.step.PersistComponentsStep.PersisComponent");

          processTp("https://github.com/SonarSource/sonarqube.git", "091ec857d24bfe139d2a5ce143ffc9b32b21cd7c", 
            "Move Class org.sonar.core.component.SnapshotQueryTest moved to org.sonar.core.component.db.SnapshotQueryTest",
            "Move Class org.sonar.core.component.SnapshotQuery moved to org.sonar.core.component.db.SnapshotQuery");


          processFp("https://github.com/excilys/androidannotations.git", "1012b2d56c4794fa32d5147ba0022570a15a03fe", 
            "Move Method private narrow(toNarrow JClass) : JClass from class org.androidannotations.holder.EFragmentHolder to public narrow(toNarrow JClass) : JClass from class org.androidannotations.holder.BaseGeneratedClassHolder");

          processFp("https://github.com/liferay/liferay-portal.git", "1855f570b62d1176e729229996c603d2a74abe01", 
            "Move Class com.liferay.lar.lifecycle.ExportImportLifecycleEventTest moved to com.liferay.lar.lifecycle.ExportImportLifecycleEventTest",
            "Move Class com.liferay.lar.PortletDataContextZipWriterTest moved to com.liferay.lar.PortletDataContextZipWriterTest",
            "Move Class com.liferay.lar.PortletDataContextReferencesTest moved to com.liferay.lar.PortletDataContextReferencesTest",
            "Move Class com.liferay.lar.PermissionExportImportTest moved to com.liferay.lar.PermissionExportImportTest",
            "Move Class com.liferay.lar.LayoutSetPrototypePropagationTest moved to com.liferay.lar.LayoutSetPrototypePropagationTest",
            "Move Class com.liferay.lar.LayoutPrototypePropagationTest moved to com.liferay.lar.LayoutPrototypePropagationTest",
            "Move Class com.liferay.lar.LayoutExportImportTest moved to com.liferay.lar.LayoutExportImportTest",
            "Move Class com.liferay.lar.ExportImportHelperUtilTest moved to com.liferay.lar.ExportImportHelperUtilTest",
            "Move Class com.liferay.lar.ExportImportDateUtilTest moved to com.liferay.lar.ExportImportDateUtilTest",
            "Move Class com.liferay.lar.BasePrototypePropagationTestCase moved to com.liferay.lar.BasePrototypePropagationTestCase");
          processTp("https://github.com/JetBrains/intellij-community.git", "219d6ddfd1db62c11efb57e0216436874e087834", 
            "Extract Method private addAdditionalLoggingHandler(loggingHandler LoggingHandlerBase) : void extracted from public addAdditionalLog(presentableName String) : LoggingHandler in class com.intellij.remoteServer.impl.runtime.log.DeploymentLogManagerImpl",
            "Extract Superclass com.intellij.remoteServer.impl.runtime.log.LoggingHandlerBase from classes [com.intellij.remoteServer.impl.runtime.log.LoggingHandlerImpl]",
            "Extract Superclass com.intellij.remoteServer.agent.util.log.LogPipeBase from classes [com.intellij.remoteServer.agent.util.log.LogPipe]");

          processTp("https://github.com/HubSpot/Singularity.git", "45ada13b852af85e1ae0491267a0239d9bdf6f3f", 
            "Pull Up Attribute protected validator : SingularityValidator from class com.hubspot.singularity.resources.RequestResource to class com.hubspot.singularity.resources.AbstractRequestResource",
            "Pull Up Attribute protected validator : SingularityValidator from class com.hubspot.singularity.resources.DeployResource to class com.hubspot.singularity.resources.AbstractRequestResource");

          processTp("https://github.com/liferay/liferay-portal.git", "59fd9e696cec5f2ed44c27422bbc426b11647321", 
            "Extract Method public addDependency(project Project, configurationName String, group String, name String, version String, classifier String, transitive boolean) : Dependency extracted from public addDependency(project Project, configurationName String, group String, name String, version String, transitive boolean) : Dependency in class com.liferay.gradle.util.GradleUtil");


          processFp("https://github.com/open-keychain/open-keychain.git", "d16b09b2a6be41319b993c27e69b85067a7f1c46", 
            "Move Class org.sufficientlysecure.keychain.util.TestingUtils moved to org.sufficientlysecure.keychain.util.TestingUtils",
            "Move Class org.sufficientlysecure.keychain.util.ParcelableFileCacheTest moved to org.sufficientlysecure.keychain.util.ParcelableFileCacheTest",
            "Move Class org.sufficientlysecure.keychain.util.Iso7816TLVTest moved to org.sufficientlysecure.keychain.util.Iso7816TLVTest",
            "Move Class org.sufficientlysecure.keychain.support.TestDataUtil moved to org.sufficientlysecure.keychain.support.TestDataUtil",
            "Move Class org.sufficientlysecure.keychain.support.ProviderHelperStub moved to org.sufficientlysecure.keychain.support.ProviderHelperStub",
            "Move Class org.sufficientlysecure.keychain.support.KeyringTestingHelper moved to org.sufficientlysecure.keychain.support.KeyringTestingHelper",
            "Move Class org.sufficientlysecure.keychain.operations.CertifyOperationTest moved to org.sufficientlysecure.keychain.operations.CertifyOperationTest",
            "Move Class org.sufficientlysecure.keychain.operations.ExportTest moved to org.sufficientlysecure.keychain.operations.ExportTest",
            "Move Class org.sufficientlysecure.keychain.operations.PromoteKeyOperationTest moved to org.sufficientlysecure.keychain.operations.PromoteKeyOperationTest",
            "Move Class org.sufficientlysecure.keychain.pgp.KeyRingTest moved to org.sufficientlysecure.keychain.pgp.KeyRingTest",
            "Move Class org.sufficientlysecure.keychain.pgp.PgpEncryptDecryptTest moved to org.sufficientlysecure.keychain.pgp.PgpEncryptDecryptTest",
            "Move Class org.sufficientlysecure.keychain.pgp.PgpKeyOperationTest moved to org.sufficientlysecure.keychain.pgp.PgpKeyOperationTest",
            "Move Class org.sufficientlysecure.keychain.pgp.UncachedKeyringCanonicalizeTest moved to org.sufficientlysecure.keychain.pgp.UncachedKeyringCanonicalizeTest",
            "Move Class org.sufficientlysecure.keychain.pgp.UncachedKeyringMergeTest moved to org.sufficientlysecure.keychain.pgp.UncachedKeyringMergeTest",
            "Move Class org.sufficientlysecure.keychain.pgp.UncachedKeyringTest moved to org.sufficientlysecure.keychain.pgp.UncachedKeyringTest",
            "Move Class org.sufficientlysecure.keychain.provider.ProviderHelperKeyringTest moved to org.sufficientlysecure.keychain.provider.ProviderHelperKeyringTest",
            "Move Class org.sufficientlysecure.keychain.provider.ProviderHelperSaveTest moved to org.sufficientlysecure.keychain.provider.ProviderHelperSaveTest",
            "Move Class org.sufficientlysecure.keychain.support.KeyringBuilder moved to org.sufficientlysecure.keychain.support.KeyringBuilder");
          processTp("https://github.com/FasterXML/jackson-databind.git", "44dea1f292933192ea5287d9b3e14a7daaef3c0f", 
            "Move Class com.fasterxml.jackson.failing.TestExternalTypeId222.Issue222BeanB moved to com.fasterxml.jackson.databind.jsontype.TestExternalId.Issue222BeanB",
            "Move Class com.fasterxml.jackson.failing.TestExternalTypeId222.Issue222Bean moved to com.fasterxml.jackson.databind.jsontype.TestExternalId.Issue222Bean");

          processTp("https://github.com/amplab/tachyon.git", "6d10621465c0e6ae81ad8d240d70a55c72caeea6", 
            "Push Down Attribute private mBlockSize : long from class tachyon.worker.block.meta.BlockMetaBase to class tachyon.worker.block.meta.BlockMeta",
            "Push Down Method public getBlockSize() : long from class tachyon.worker.block.meta.BlockMetaBase to public getBlockSize() : long from class tachyon.worker.block.meta.BlockMeta");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "96a2c3410f3c71d3ab20857036422f1d64c3a6d3", 
            "Extract Method private cleanupProximityLocked() : void extracted from private cleanupProximity() : void in class com.android.server.power.PowerManagerService");


          processFp("https://github.com/kuujo/copycat.git", "7fed82baa16f03aba6938da14eb81de4d18c3b76", 
            "Extract Method protected applyEntry(entry Entry) : CompletableFuture<?> extracted from protected getReplicas() : List<MemberState> in class net.kuujo.copycat.raft.state.FollowerState");

          processFp("https://github.com/SonarSource/sonarqube.git", "6abce5190a0a399f0b7e0af09cda6e86b2188284", 
            "Extract Method private checkQualityGateStatusChange(project Component, metric Metric, rawStatus QualityGateStatus) : void extracted from private executeForProject(project Component) : void in class org.sonar.server.computation.step.QualityGateEventsStep");
          processTp("https://github.com/elastic/elasticsearch.git", "f77804dad35c13d9ff96456e85737883cf7ddd99", 
            "Move Class org.elasticsearch.index.merge.policy.VersionFieldUpgraderTest moved to org.elasticsearch.index.shard.VersionFieldUpgraderTest",
            "Move Class org.elasticsearch.index.merge.policy.VersionFieldUpgrader moved to org.elasticsearch.index.shard.VersionFieldUpgrader",
            "Move Class org.elasticsearch.index.merge.policy.FilterDocValuesProducer moved to org.elasticsearch.index.shard.FilterDocValuesProducer",
            "Move Class org.elasticsearch.index.merge.policy.ElasticsearchMergePolicy moved to org.elasticsearch.index.shard.ElasticsearchMergePolicy");

          processTp("https://github.com/elastic/elasticsearch.git", "c928852d4ab7d8c744063979208709ed4429b8e9", 
            "Move Method public newFilter(parseContext QueryParseContext, fieldPattern String, queryName String) : Query from class org.elasticsearch.index.query.ExistsQueryParser to public newFilter(parseContext QueryParseContext, fieldPattern String, queryName String) : Query from class org.elasticsearch.index.query.ExistsQueryBuilder");


          processFp("https://github.com/orientechnologies/orientdb.git", "a6558ca8350bdccb1a216596c1414424abd776e8", 
            "Extract Method private computePath(leftDistances Map<ORID,ORID>, rightDistances Map<ORID,ORID>, neighbor ORID) : List<ORID> extracted from public execute(iThis Object, iCurrentRecord OIdentifiable, iCurrentResult Object, iParams Object[], iContext OCommandContext) : List<ORID> in class com.orientechnologies.orient.graph.sql.functions.OSQLFunctionShortestPath");
          processTp("https://github.com/checkstyle/checkstyle.git", "5a9b7249e3d092a78ac8e7d48aeeb62bf1c44e20", 
            "Extract Method private processField(ast DetailAST, parentType int) : void extracted from private processIDENT(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.RequireThisCheck");

          processTp("https://github.com/JetBrains/intellij-community.git", "a97341973c3b683d62d1422e5404ed5c7ccf45f8", 
            "Extract Method private setNewName(newText String) : PsiElement extracted from public bindToElement(element PsiElement) : PsiElement in class org.jetbrains.plugins.javaFX.fxml.refs.FxmlReferencesContributor.MyJavaClassReferenceProvider.JavaClassReferenceWrapper",
            "Extract Method private setNewName(newText String) : PsiElement extracted from public handleElementRename(newElementName String) : PsiElement in class org.jetbrains.plugins.javaFX.fxml.refs.FxmlReferencesContributor.MyJavaClassReferenceProvider.JavaClassReferenceWrapper");


          processFp("https://github.com/codefollower/Lealone.git", "11dd351f6500cc9626dfa4f0438917de4dcb7fe3", 
            "Extract Method protected parseCreateTable(temp boolean, globalTemp boolean, persistIndexes boolean) : CreateTable extracted from protected parseCreate() : Prepared in class org.lealone.command.Parser");
          processTp("https://github.com/datastax/java-driver.git", "3a0603f8f778be3219a5a0f3a7845cda65f1e172", 
            "Extract Method public values(names List<String>, values List<Object>) : Insert extracted from public values(names String[], values Object[]) : Insert in class com.datastax.driver.core.querybuilder.Insert");

          processTp("https://github.com/JetBrains/intellij-community.git", "d71154ed21e2d5c65bb0ddb000bcb04ca5735048", 
            "Extract Method public canonicalizePath(url String, baseUrl Url, baseUrlIsFile boolean) : String extracted from protected canonicalizeUrl(url String, baseUrl Url, trimFileScheme boolean, sourceIndex int, baseUrlIsFile boolean) : Url in class org.jetbrains.debugger.sourcemap.SourceResolver");

          processTp("https://github.com/undertow-io/undertow.git", "d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4", 
            "Move Method private isOperator(op String) : boolean from class io.undertow.server.handlers.builder.HandlerParser to private isOperator(op String) : boolean from class io.undertow.server.handlers.builder.PredicatedHandlersParser",
            "Move Method private isOperator(op String) : boolean from class io.undertow.predicate.PredicateParser to private isOperator(op String) : boolean from class io.undertow.server.handlers.builder.PredicatedHandlersParser",
            "Move Class io.undertow.util.PredicateTokeniser.Token moved to io.undertow.server.handlers.builder.PredicatedHandlersParser.Token",
            "Extract Method public addPredicatedHandler(predicate Predicate, handlerWrapper HandlerWrapper, elseBranch HandlerWrapper) : PredicatesHandler extracted from public addPredicatedHandler(predicate Predicate, handlerWrapper HandlerWrapper) : PredicatesHandler in class io.undertow.predicate.PredicatesHandler");
          processFp("https://github.com/undertow-io/undertow.git", "d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4", 
            "Extract Method public tokenize(string String) : Deque<Token> extracted from public parse(contents String, classLoader ClassLoader) : List<PredicatedHandler> in class io.undertow.server.handlers.builder.PredicatedHandlersParser",
            "Extract Method package parse(string String, tokens Deque<Token>) : Node extracted from public parse(contents String, classLoader ClassLoader) : List<PredicatedHandler> in class io.undertow.server.handlers.builder.PredicatedHandlersParser");
          processTp("https://github.com/Netflix/eureka.git", "f6212a7e474f812f31ddbce6d4f7a7a0d498b751", 
            "Extract Method protected onRemoteStatusChanged(oldStatus InstanceStatus, newStatus InstanceStatus) : void extracted from private updateInstanceRemoteStatus() : void in class com.netflix.discovery.DiscoveryClient");


          processFp("https://github.com/crate/crate.git", "00e669301ab09e76735468446f3d4b83011ae679", 
            "Inline Method private resolveCustomSchemas(metaData MetaData) : Map<String,SchemaInfo> inlined to public clusterChanged(event ClusterChangedEvent) : void in class io.crate.metadata.ReferenceInfos");

          processFp("https://github.com/crate/crate.git", "d4e5501a48257a6d03ba874db8b0ad8b8588006c", 
            "Inline Method private resolveCustomSchemas(metaData MetaData) : Map<String,SchemaInfo> inlined to public clusterChanged(event ClusterChangedEvent) : void in class io.crate.metadata.ReferenceInfos");
          processTp("https://github.com/eclipse/vert.x.git", "718782014519034b28f6d3182fd9d340b7b31a74", 
            "Push Down Attribute protected connectionMap : Map<Channel,C> from class io.vertx.core.net.impl.VertxHandler to class io.vertx.core.http.impl.VertxHttpHandler");

          processTp("https://github.com/orientechnologies/orientdb.git", "b40adc25008b6f608ee3eb3422c8884fff987337", 
            "Extract Method public serializeValue(listener OAbstractCommandResultListener, result Object) : void extracted from protected command() : void in class com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary",
            "Extract Method public serializeValue(listener OAbstractCommandResultListener, result Object) : void extracted from private indexGet() : void in class com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary",
            "Extract Method protected readSynchResult(network OChannelBinaryAsynchClient, database ODatabaseDocument) : Object extracted from public command(iCommand OCommandRequestText) : Object in class com.orientechnologies.orient.client.remote.OStorageRemote");

          processTp("https://github.com/SonarSource/sonarqube.git", "06fa57185cba7304c69a7d6c792a15f2632e3e02", 
            "Move Class org.sonar.server.computation.measure.Measure.QualityGateStatus moved to org.sonar.server.computation.measure.QualityGateStatus");


          processFp("https://github.com/JetBrains/intellij-community.git", "edab2a9bf365da7508b6a22d70357c55cb441225", 
            "Extract Method package shouldNotifySmartPointers(virtualFile VirtualFile) : boolean extracted from public updateFinished(document Document) : void in class com.intellij.psi.impl.PsiDocumentManagerBase",
            "Extract Method package shouldNotifySmartPointers(virtualFile VirtualFile) : boolean extracted from public updateStarted(document Document) : void in class com.intellij.psi.impl.PsiDocumentManagerBase");

          processFp("https://github.com/wordpress-mobile/WordPress-Android.git", "9eaf95842954e90dbb01618e932ce1560ba1f5f6", 
            "Move Class org.wordpress.android.editor.ApplicationTest moved to org.wordpress.android.editor.ApplicationTest");
          processTp("https://github.com/JetBrains/intellij-community.git", "3972b9b3d4e03bdb5e62dfa663e3e0a1871e3c9f", 
            "Move Class com.intellij.psi.codeStyle.autodetect.NewLineBlocksIterator moved to com.intellij.psi.formatter.common.NewLineBlocksIterator");


          processFp("https://github.com/JetBrains/intellij-community.git", "3b1c3bad64e0e7cfabb5ca5d2be7b3aca0b7c197", 
            "Move Class A moved to A",
            "Move Class EventManager moved to EventManager",
            "Move Class aaa.bbb.Margin moved to aaa.bbb.Margin",
            "Move Class Test moved to Test",
            "Move Class Doable moved to Doable",
            "Move Class ChangeFileTypeRequest moved to ChangeFileTypeRequest",
            "Move Class Percolation moved to Percolation");

          processFp("https://github.com/eclipse/vert.x.git", "c53e67f865e6322d00009f1373df3f87978c6c98", 
            "Extract Method private createConnAndHandle(ch Channel, msg Object, shake WebSocketServerHandshaker) : void extracted from private handshake(request FullHttpRequest, ch Channel, ctx ChannelHandlerContext) : void in class io.vertx.core.http.impl.HttpServerImpl.ServerHandler",
            "Extract Method private createConnAndHandle(ch Channel, msg Object, shake WebSocketServerHandshaker) : void extracted from private handleHttp(conn ServerConnection, ch Channel, msg Object) : void in class io.vertx.core.http.impl.HttpServerImpl.ServerHandler");
          processTp("https://github.com/droolsjbpm/drools.git", "1bf2875e9d73e2d1cd3b58200d5300485f890ff5", 
            "Push Down Attribute private evaluatingActionQueue : AtomicBoolean from class org.drools.core.impl.StatefulKnowledgeSessionImpl to class org.drools.reteoo.common.ReteWorkingMemory",
            "Move Method public notifyHalt() : void from class org.drools.reteoo.common.ReteAgenda to public notifyHalt() : void from class org.drools.core.phreak.SynchronizedBypassPropagationList",
            "Move Method public notifyHalt() : void from class org.drools.core.common.DefaultAgenda to public notifyHalt() : void from class org.drools.core.phreak.SynchronizedBypassPropagationList",
            "Extract Method protected initPriorityQueue(kBase InternalKnowledgeBase) : BinaryHeapQueue extracted from public AgendaGroupQueueImpl(name String, kBase InternalKnowledgeBase) in class org.drools.core.common.AgendaGroupQueueImpl",
            "Extract Method private internalAddEntry(entry PropagationEntry) : void extracted from public addEntry(entry PropagationEntry) : void in class org.drools.core.phreak.SynchronizedPropagationList");
          processFp("https://github.com/droolsjbpm/drools.git", "1bf2875e9d73e2d1cd3b58200d5300485f890ff5", 
            "Extract Method private fire(wm InternalWorkingMemory, filter AgendaFilter, fireCount int, fireLimit int, agenda InternalAgenda) : int extracted from public evaluateNetworkAndFire(wm InternalWorkingMemory, filter AgendaFilter, fireCount int, fireLimit int) : int in class org.drools.core.phreak.RuleExecutor",
            "Extract Method private haltRuleFiring(fireCount int, fireLimit int, localFireCount int, agenda InternalAgenda) : boolean extracted from private fire(wm InternalWorkingMemory, filter AgendaFilter, fireCount int, fireLimit int, outerStack LinkedList<StackEntry>, agenda InternalAgenda) : int in class org.drools.core.phreak.RuleExecutor",
            "Extract Method private evalQueryNode(liaNode LeftInputAdapterNode, pmem PathMemory, node NetworkNode, bit long, nodeMem Memory, smems SegmentMemory[], smemIndex int, trgTuples LeftTupleSets, wm InternalWorkingMemory, stack LinkedList<StackEntry>, srcTuples LeftTupleSets, sink LeftTupleSinkNode, stagedLeftTuples LeftTupleSets) : boolean extracted from public evaluateNetwork(pmem PathMemory, outerStack LinkedList<StackEntry>, executor RuleExecutor, wm InternalWorkingMemory) : void in class org.drools.core.phreak.RuleNetworkEvaluator",
            "Extract Method public flush(workingMemory InternalWorkingMemory, currentHead PropagationEntry) : void extracted from private internalFlush() : void in class org.drools.core.phreak.SynchronizedPropagationList",
            "Extract Method public takeAll() : PropagationEntry extracted from private internalFlush() : void in class org.drools.core.phreak.SynchronizedPropagationList");
          processTp("https://github.com/spring-projects/spring-data-rest.git", "b7cba6a700d8c5e456cdeffe9c5bf54563eab7d3", 
            "Extract Method protected setupMockMvc() : void extracted from public setUp() : void in class org.springframework.data.rest.webmvc.AbstractWebIntegrationTests");

          processTp("https://github.com/datastax/java-driver.git", "d5134b15fe6545ec8ab5c2256006cd6fe19eac92", 
            "Extract Method package getPreparedQuery(type QueryType, columns Set<ColumnMapper<?>>, options Option[]) : PreparedStatement extracted from package getPreparedQuery(type QueryType, options Option[]) : PreparedStatement in class com.datastax.driver.mapping.Mapper");

          processTp("https://github.com/VoltDB/voltdb.git", "669e0722324965e3c99f29685517ac24d4ff2848", 
            "Extract Method public getClient(timeout long, scheme ClientAuthHashScheme, useAdmin boolean) : Client extracted from public getClient(timeout long, scheme ClientAuthHashScheme) : Client in class org.voltdb.regressionsuites.RegressionSuite",
            "Extract Method private getListenerAddress(hostId int, useAdmin boolean) : String extracted from public getListenerAddress(hostId int) : String in class org.voltdb.regressionsuites.LocalCluster",
            "Extract Method private runPausedMode(isAdmin boolean) : void extracted from public testPausedMode() : void in class org.voltdb.TestClientInterface",
            "Extract Method public makeStoredProcAdHocPlannerWork(replySiteId long, sql String, userParams Object[], singlePartition boolean, context CatalogContext, completionHandler AsyncCompilerWorkCompletionHandler, isAdmin boolean) : AdHocPlannerWork extracted from public makeStoredProcAdHocPlannerWork(replySiteId long, sql String, userParams Object[], singlePartition boolean, context CatalogContext, completionHandler AsyncCompilerWorkCompletionHandler) : AdHocPlannerWork in class org.voltdb.compiler.AdHocPlannerWork",
            "Extract Method public mockStatementBatch(replySiteId long, sql String, extractedValues Object[], paramTypes VoltType[], userParams Object[], partitionParamIndex int, catalogHash byte[], readOnly boolean, isAdmin boolean) : AdHocPlannedStmtBatch extracted from public mockStatementBatch(replySiteId long, sql String, extractedValues Object[], paramTypes VoltType[], userParams Object[], partitionParamIndex int, catalogHash byte[]) : AdHocPlannedStmtBatch in class org.voltdb.compiler.AdHocPlannedStmtBatch");

          processTp("https://github.com/SonarSource/sonarqube.git", "5ff305abb3068e420d8e54a796591d75acc8b8be", 
            "Extract Interface org.sonar.api.utils.ProjectTempFolder from classes [org.sonar.api.utils.internal.DefaultTempFolder, org.sonar.api.utils.internal.JUnitTempFolder]");

          processTp("https://github.com/VoltDB/voltdb.git", "e9efc045fbc6fa893c66a03b72b7eedb388cf96c", 
            "Extract Method public setMpUniqueIdListener(listener DurableMpUniqueIdListener) : void extracted from public setMpDRGateway(mpGateway PartitionDRGateway) : void in class org.voltdb.iv2.SpScheduler");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "658a918eebcbdeb4f920c2947ca8d0e79ad86d89", 
            "Extract Method private initTickerView() : void extracted from protected makeStatusBarView() : PhoneStatusBarView in class com.android.systemui.statusbar.phone.PhoneStatusBar");

          processTp("https://github.com/wordpress-mobile/WordPress-Android.git", "2252ed3754bff8c39db48d172ac76ac5a4e15359", 
            "Inline Method private shouldShowTagToolbar() : boolean inlined to public onCreateView(inflater LayoutInflater, container ViewGroup, savedInstanceState Bundle) : View in class org.wordpress.android.ui.reader.ReaderPostListFragment");


          processFp("https://github.com/amplab/tachyon.git", "6d453d1a046ec7ed1b60610adfc8c006173792c8", 
            "Inline Method public getWorkerPort() : int inlined to public getWorkerAddress() : NetAddress in class tachyon.master.LocalTachyonCluster");
          processTp("https://github.com/spring-projects/spring-boot.git", "20d39f7af2165c67d5221f556f58820c992d2cc6", 
            "Extract Method private getFullKey(path String, key String) : String extracted from private flatten(properties Properties, input Map<String,Object>, path String) : void in class org.springframework.boot.cloudfoundry.VcapApplicationListener");


          processFp("https://github.com/infinispan/infinispan.git", "c58fea6e4a521a0b75b967915b80e643fbf40424", 
            "Extract Method public invokeRemotelyAsync(recipients Collection<Address>, rpc ReplicableCommand, options RpcOptions) : CompletableFuture<Map<Address,Response>> extracted from public invokeRemotely(recipients Collection<Address>, rpc ReplicableCommand, options RpcOptions) : Map<Address,Response> in class org.infinispan.remoting.rpc.RpcManagerImpl");

          processFp("https://github.com/infinispan/infinispan.git", "ecb07a95b98bd3671b8ef6636c4ccf11d972a0dd", 
            "Extract Method public invokeRemotely(rpcCommands Map<Address,ReplicableCommand>, mode ResponseMode, timeout long, responseFilter ResponseFilter, deliverOrder DeliverOrder, anycast boolean) : Map<Address,Response> extracted from public invokeRemotely(rpcCommands Map<Address,ReplicableCommand>, mode ResponseMode, timeout long, usePriorityQueue boolean, responseFilter ResponseFilter, totalOrder boolean, anycast boolean) : Map<Address,Response> in class org.infinispan.remoting.transport.jgroups.JGroupsTransport",
            "Extract Method public invokeRemotelyAsync(recipients Collection<Address>, rpcCommand ReplicableCommand, mode ResponseMode, timeout long, responseFilter ResponseFilter, deliverOrder DeliverOrder, anycast boolean) : CompletableFuture<Map<Address,Response>> extracted from public invokeRemotely(recipients Collection<Address>, rpcCommand ReplicableCommand, mode ResponseMode, timeout long, responseFilter ResponseFilter, deliverOrder DeliverOrder, anycast boolean) : Map<Address,Response> in class org.infinispan.remoting.transport.jgroups.JGroupsTransport",
            "Extract Method private processCalls(command ReplicableCommand, broadcast boolean, timeout long, filter RspFilter, dests List<Address>, mode ResponseMode, deliverOrder DeliverOrder, marshaller Marshaller) : RspListFuture extracted from public invokeRemoteCommands(recipients List<Address>, command ReplicableCommand, mode ResponseMode, timeout long, filter RspFilter, deliverOrder DeliverOrder, ignoreLeavers boolean) : RspList<Object> in class org.infinispan.remoting.transport.jgroups.CommandAwareRpcDispatcher",
            "Extract Method private processSingleCall(command ReplicableCommand, timeout long, destination Address, mode ResponseMode, deliverOrder DeliverOrder, marshaller Marshaller) : SingleResponseFuture extracted from public invokeRemoteCommands(recipients List<Address>, command ReplicableCommand, mode ResponseMode, timeout long, filter RspFilter, deliverOrder DeliverOrder, ignoreLeavers boolean) : RspList<Object> in class org.infinispan.remoting.transport.jgroups.CommandAwareRpcDispatcher");
          processTp("https://github.com/fabric8io/fabric8.git", "9e61a71540da58c3208fd2c7737f793c3f81e5ae", 
            "Extract Method public createGogsWebhook(kubernetes KubernetesClient, log Log, gogsUser String, gogsPwd String, repoName String, webhookUrl String, webhookSecret String) : boolean extracted from public execute() : void in class io.fabric8.maven.CreateGogsWebhook");

          processTp("https://github.com/spring-projects/spring-boot.git", "1cfc6f64f64353bc5530a8ce8cdacfc3eba3e7b2", 
            "Extract Method private addEntityScanBeanPostProcessor(registry BeanDefinitionRegistry, packagesToScan Set<String>) : void extracted from public registerBeanDefinitions(importingClassMetadata AnnotationMetadata, registry BeanDefinitionRegistry) : void in class org.springframework.boot.orm.jpa.EntityScanRegistrar");

          processTp("https://github.com/facebook/buck.git", "1c7c03dd9e6d5810ad22d37ecae59722c219ac35", 
            "Move Class com.facebook.buck.cli.UninstallEventTest moved to com.facebook.buck.event.UninstallEventTest",
            "Move Class com.facebook.buck.cli.TestDevice moved to com.facebook.buck.android.TestDevice",
            "Move Class com.facebook.buck.cli.StartActivityEventTest moved to com.facebook.buck.event.StartActivityEventTest",
            "Move Class com.facebook.buck.cli.InstallEventTest moved to com.facebook.buck.event.InstallEventTest",
            "Move Class com.facebook.buck.cli.UninstallEvent moved to com.facebook.buck.event.UninstallEvent",
            "Move Class com.facebook.buck.cli.StartActivityEvent moved to com.facebook.buck.event.StartActivityEvent",
            "Move Class com.facebook.buck.cli.InstallEvent moved to com.facebook.buck.event.InstallEvent",
            "Move Class com.facebook.buck.cli.AdbHelper.CommandFailedException moved to com.facebook.buck.android.AdbHelper.CommandFailedException",
            "Move Class com.facebook.buck.cli.AdbHelper.ErrorParsingReceiver moved to com.facebook.buck.android.AdbHelper.ErrorParsingReceiver",
            "Move Class com.facebook.buck.cli.AdbHelper.AdbCallable moved to com.facebook.buck.android.AdbHelper.AdbCallable");

          processTp("https://github.com/Netflix/eureka.git", "1cacbe2ad700275bc575234ff2b32ee0d6493817", 
            "Extract Method protected fireEvent(event DiscoveryEvent) : void extracted from protected onRemoteStatusChanged(oldStatus InstanceStatus, newStatus InstanceStatus) : void in class com.netflix.discovery.DiscoveryClient");
          processFp("https://github.com/Netflix/eureka.git", "1cacbe2ad700275bc575234ff2b32ee0d6493817", 
            "Extract Superclass com.netflix.discovery.DiscoveryEvent from classes [com.netflix.discovery.StatusChangeEvent]");

          processFp("https://github.com/apache/drill.git", "68c933c75f3832abd5a56d31937c45d3087cee47", 
            "Move Attribute private offsetsField : MaterializedField from class org.apache.drill.exec.vector.complex.BaseRepeatedValueVector to class org.apache.drill.exec.vector.$");
          processTp("https://github.com/google/closure-compiler.git", "ba5e6d44526a2491a7004423ca2ad780c6992c46", 
            "Inline Method private getRawTypeFromJSType(t JSType) : RawNominalType inlined to private visitOtherPropertyDeclaration(getProp Node) : void in class com.google.javascript.jscomp.GlobalTypeInfo.ProcessScope");

          processTp("https://github.com/Netflix/eureka.git", "457a7f637ddb226acf477cae0b04c8ff16ec9a50", 
            "Extract Superclass com.netflix.discovery.BaseDiscoveryClientTester from classes [com.netflix.discovery.AbstractDiscoveryClientTester]");

          processTp("https://github.com/linkedin/rest.li.git", "bd0d3bf75d31a8b5db34b8b66dfb28e5e1f492de", 
            "Extract Method protected extendUnionBaseClass(unionClass JDefinedClass) : void extracted from protected generateUnion(unionClass JDefinedClass, unionSpec UnionTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator",
            "Extract Method protected extendRecordBaseClass(templateClass JDefinedClass) : void extracted from protected generateRecord(templateClass JDefinedClass, recordSpec RecordTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator",
            "Extract Method protected extendWrappingMapBaseClass(valueJClass JClass, mapClass JDefinedClass) : void extracted from protected generateMap(mapClass JDefinedClass, mapSpec MapTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator",
            "Extract Method protected extendWrappingArrayBaseClass(itemJClass JClass, arrayClass JDefinedClass) : void extracted from protected generateArray(arrayClass JDefinedClass, arrayDataTemplateSpec ArrayTemplateSpec) : void in class com.linkedin.pegasus.generator.JavaDataTemplateGenerator");


          processFp("https://github.com/square/otto.git", "936f692ffee63760aa2ace2d189b3fa1aa15e543", 
            "Extract Method private loadAnnotatedMethods(listenerClass Class<?>, producerMethods Map<Class<?>,Method>, subscriberMethods Map<Class<?>,Set<Method>>) : void extracted from private loadAnnotatedMethods(listenerClass Class<?>) : void in class com.squareup.otto.AnnotatedHandlerFinder");
          processTp("https://github.com/codinguser/gnucash-android.git", "bba4af3f52064b5a2de2c9a57f9d34ba67dcdd8c", 
            "Pull Up Method public getAllTransactionsCount() : long from class org.gnucash.android.db.TransactionsDbAdapter to public getRecordsCount() : long from class org.gnucash.android.db.DatabaseAdapter");


          processFp("https://github.com/fabric8io/fabric8.git", "a6ed950385f68748fee604757d78ed3c1d8a2a00", 
            "Extract Method protected createRoutes(kubernetes KubernetesClient, collection Collection<HasMetadata>) : void extracted from public execute() : void in class io.fabric8.maven.ApplyMojo",
            "Inline Method protected createRoutes(kubernetes KubernetesClient, list List<HasMetadata>) : void inlined to public execute() : void in class io.fabric8.maven.ApplyMojo");
          processTp("https://github.com/eclipse/vert.x.git", "32a8c9086040fd6d6fa11a214570ee4f75a4301f", 
            "Inline Method private handleHttp(conn ServerConnection, ch Channel, msg Object) : void inlined to protected doMessageReceived(conn ServerConnection, ctx ChannelHandlerContext, msg Object) : void in class io.vertx.core.http.impl.HttpServerImpl.ServerHandler");
          processFp("https://github.com/eclipse/vert.x.git", "32a8c9086040fd6d6fa11a214570ee4f75a4301f", 
            "Inline Method private handleExpectWebsockets(conn ServerConnection, ctx ChannelHandlerContext, msg Object) : void inlined to protected doMessageReceived(conn ServerConnection, ctx ChannelHandlerContext, msg Object) : void in class io.vertx.core.http.impl.HttpServerImpl.ServerHandler");
          processTp("https://github.com/crate/crate.git", "72b5348307d86b1a118e546c24d97f1ac1895bdb", 
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

          processTp("https://github.com/JetBrains/intellij-community.git", "5f18bed8da4dda4fa516907ecbbe28f712e944f7", 
            "Move Class com.intellij.util.ui.components.JBPanel moved to com.intellij.ui.components.JBPanel");

          processTp("https://github.com/orientechnologies/orientdb.git", "1089957b645bde069d3864563bbf1f7c7da8045c", 
            "Extract Method protected rewriteLinksInDocument(document ODocument, rewrite OIndex<OIdentifiable>) : void extracted from private rewriteLinksInDocument(document ODocument) : void in class com.orientechnologies.orient.core.db.tool.ODatabaseImport");

          processTp("https://github.com/belaban/JGroups.git", "f1533756133dec84ce8218202585ac85904da7c9", 
            "Extract Method public isInMembersList(sender IpAddress) : boolean extracted from public authenticate(token AuthToken, msg Message) : boolean in class org.jgroups.auth.FixedMembershipToken");

          processTp("https://github.com/hierynomus/sshj.git", "7c26ac669a4e17ca1d2319a5049a56424fd33104", 
            "Move Class nl.javadude.sshj.connection.channel.ChannelCloseEofTest moved to com.hierynomus.sshj.connection.channel.ChannelCloseEofTest");
          processFp("https://github.com/hierynomus/sshj.git", "7c26ac669a4e17ca1d2319a5049a56424fd33104", 
            "Extract Superclass com.hierynomus.sshj.SshIntegrationTestBase from classes [com.hierynomus.sshj.connection.channel.ChannelCloseEofTest]");
          processTp("https://github.com/processing/processing.git", "d7f781da42e54824c17875a6036d3448672637f5", 
            "Move Attribute protected ERROR_PUSHMATRIX_UNDERFLOW : String from class processing.core.PConstants to class processing.core.PGraphics",
            "Move Attribute protected ERROR_PUSHMATRIX_OVERFLOW : String from class processing.core.PConstants to class processing.core.PGraphics",
            "Move Attribute protected ERROR_BACKGROUND_IMAGE_FORMAT : String from class processing.core.PConstants to class processing.core.PGraphics",
            "Move Attribute protected ERROR_BACKGROUND_IMAGE_SIZE : String from class processing.core.PConstants to class processing.core.PGraphics",
            "Move Attribute protected ERROR_TEXTFONT_NULL_PFONT : String from class processing.core.PConstants to class processing.core.PGraphics");

          processTp("https://github.com/nutzam/nutz.git", "6599c748ef35d38085703cf3bd41b9b5b6af5f32", 
            "Extract Method public from(dao Dao, obj Object, filter FieldFilter, ignoreNull boolean, ignoreZero boolean, ignoreDate boolean, ignoreId boolean, ignoreName boolean, ignorePk boolean) : Cnd extracted from public from(dao Dao, obj Object, filter FieldFilter) : Cnd in class org.nutz.dao.Cnd");

          processTp("https://github.com/infinispan/infinispan.git", "e3b0d87b3ca0fd27cec39937cb3dc3a05b0cfc4e", 
            "Extract Method protected waitForCacheToStabilize(cache Cache<Object,Object>, cacheConfig Configuration) : void extracted from public perform(ctx InvocationContext) : Object in class org.infinispan.commands.CreateCacheCommand");

          processTp("https://github.com/crate/crate.git", "c7b6a7aa878aabd6400d2df0490e1eb2b810c8f9", 
            "Extract Method public plan(relation AnalyzedRelation, consumerContext ConsumerContext) : PlannedAnalyzedRelation extracted from public plan(rootRelation AnalyzedRelation, plannerContext Context) : Plan in class io.crate.planner.consumer.ConsumingPlanner");

          processTp("https://github.com/BuildCraft/BuildCraft.git", "6abc40ed4850d74ee6c155f5a28f8b34881a0284", 
            "Extract Method private initTemplate() : void extracted from public updateEntity() : void in class buildcraft.builders.TileFiller",
            "Extract Method private initTemplate() : void extracted from public initialize() : void in class buildcraft.builders.TileFiller");

          processTp("https://github.com/apache/cassandra.git", "ec52e77ecde749e7c5a483b26cbd8041f2a5a33c", 
            "Extract Method public submitBackground(cfs ColumnFamilyStore, autoFill boolean) : List<Future<?>> extracted from public submitBackground(cfs ColumnFamilyStore) : List<Future<?>> in class org.apache.cassandra.db.compaction.CompactionManager");


          processFp("https://github.com/VoltDB/voltdb.git", "aa5d07f189a9e92cf6d6c28d14d0fb54f0203688", 
            "Extract Method private subTestScalarSubqueryWithNonIntegerType(client Client) : void extracted from public testSubSelects_Simple() : void in class org.voltdb.regressionsuites.TestSubQueriesSuite",
            "Extract Method private subTestScalarSubqueryWithParentOrderByOrGroupBy(client Client) : void extracted from public testSubSelects_Simple() : void in class org.voltdb.regressionsuites.TestSubQueriesSuite");
          processTp("https://github.com/robovm/robovm.git", "bf5ee44b3b576e01ab09cae9f50300417b01dc07", 
            "Extract Method public has(key CFString) : boolean extracted from public isContainingFloatingPointPixels() : boolean in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key CFString) : boolean extracted from public getColorModel() : CGImagePropertyColorModel in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key NSString) : boolean extracted from public getServices() : NSArray<CBMutableService> in class org.robovm.apple.corebluetooth.CBPeripheralManagerRestoredState",
            "Extract Method public has(key NSString) : boolean extracted from public getServiceUUIDs() : NSArray<CBUUID> in class org.robovm.apple.corebluetooth.CBAdvertisementData",
            "Extract Method public has(key NSString) : boolean extracted from public getLocalName() : String in class org.robovm.apple.corebluetooth.CBAdvertisementData",
            "Extract Method public has(key CFString) : boolean extracted from public getFileSize() : long in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key CFString) : boolean extracted from public getDPIHeight() : long in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key CFString) : boolean extracted from public getDPIWidth() : long in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key CFString) : boolean extracted from public getPixelWidth() : long in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key CFString) : boolean extracted from public getDepth() : int in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key CFString) : boolean extracted from public isIndexed() : boolean in class org.robovm.apple.imageio.CGImageProperties",
            "Extract Method public has(key CFString) : boolean extracted from public hasAlphaChannel() : boolean in class org.robovm.apple.imageio.CGImageProperties");


          processFp("https://github.com/orientechnologies/orientdb.git", "243c0ff59b3d3c3b029f477a0fa4330d1fe94682", 
            "Extract Method private jj_3R_428() : boolean extracted from private jj_3R_415() : boolean in class com.orientechnologies.orient.core.sql.parser.OrientSql");
          processTp("https://github.com/GoClipse/goclipse.git", "851ab757698304e9d8d4ae24ab75be619ddae31a", 
            "Extract Method public contains(otherOffset int) : boolean extracted from public inclusiveContains(otherOffset int) : boolean in class melnorme.lang.tooling.ast.SourceRange",
            "Extract Method public contains(other SourceRange) : boolean extracted from public inclusiveContains(other SourceRange) : boolean in class melnorme.lang.tooling.ast.SourceRange");

          processTp("https://github.com/VoltDB/voltdb.git", "c9b2006381301c99b66c50c4b31f329caac06137", 
            "Extract Method private open(forWrite boolean, truncate boolean) : void extracted from public open(forWrite boolean) : void in class org.voltdb.utils.PBDRegularSegment",
            "Extract Method private open(forWrite boolean, truncate boolean) : void extracted from public open(forWrite boolean) : void in class org.voltdb.utils.PBDMMapSegment");

          processTp("https://github.com/facebook/buck.git", "0f8a0af934f09deef1b58e961ffe789c7299bcc1", 
            "Move Class com.facebook.buck.cxx.AbstractCxxPreprocessorInput.ConflictingHeadersException moved to com.facebook.buck.cxx.AbstractCxxHeaders.ConflictingHeadersException",
            "Move Method private addAllEntriesToIncludeMap(destination Map<Path,SourcePath>, source Map<Path,SourcePath>) : void from class com.facebook.buck.cxx.AbstractCxxPreprocessorInput to public addAllEntriesToIncludeMap(destination Map<Path,SourcePath>, source Map<Path,SourcePath>) : void from class com.facebook.buck.cxx.AbstractCxxHeaders");

          processTp("https://github.com/facebook/buck.git", "f26d234e8d3458f34454583c22e3bd5f4b2a5da8", 
            "Extract Method public getDevices() : List<IDevice> extracted from public adbCall(adbCallable AdbCallable) : boolean in class com.facebook.buck.android.AdbHelper");


          processFp("https://github.com/spring-projects/spring-framework.git", "8743b6bb304e4af8a1d58b256139690d5a95c516", 
            "Extract Method private searchWithFindSemantics(element AnnotatedElement, annotationName String, processor Processor<T>, visited Set<AnnotatedElement>, metaDepth int) : T extracted from private searchWithFindSemantics(element AnnotatedElement, annotationName String, searchOnInterfaces boolean, searchOnSuperclasses boolean, searchOnMethodsInInterfaces boolean, searchOnMethodsInSuperclasses boolean, processor Processor<T>, visited Set<AnnotatedElement>, metaDepth int) : T in class org.springframework.core.annotation.AnnotatedElementUtils",
            "Inline Method private searchWithFindSemantics(element AnnotatedElement, annotationName String, searchOnInterfaces boolean, searchOnSuperclasses boolean, searchOnMethodsInInterfaces boolean, searchOnMethodsInSuperclasses boolean, processor Processor<T>) : T inlined to private searchWithFindSemantics(element AnnotatedElement, annotationName String, processor Processor<T>) : T in class org.springframework.core.annotation.AnnotatedElementUtils");
          processTp("https://github.com/google/closure-compiler.git", "ea96643364e91125f560e9508a5cbcdb776bde64", 
            "Extract Method private parseFormalParameterList(inTypeExpression boolean) : FormalParameterListTree extracted from private parseFormalParameterList() : FormalParameterListTree in class com.google.javascript.jscomp.parsing.parser.Parser");

          processTp("https://github.com/killbill/killbill.git", "66901e86e8bea2b999ed9f33e013fa5ed21507c7", 
            "Inline Method private sanityOnPaymentInfoPlugin(paymentInfoPlugin PaymentTransactionInfoPlugin) : void inlined to private doOperation() : OperationResult in class org.killbill.billing.payment.core.sm.payments.PaymentOperation");

          processTp("https://github.com/real-logic/Aeron.git", "35893c115ba23bd62a7036a33390420f074ce660", 
            "Inline Method private verifySenderNotifiedOfNewPublication() : void inlined to public shouldNotTimeoutPublicationOnKeepAlive() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifySenderNotifiedOfNewPublication() : void inlined to public shouldTimeoutPublication() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifySenderNotifiedOfNewPublication() : void inlined to public shouldBeAbleToAddSinglePublication() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyNeverSucceeds() : void inlined to public shouldErrorOnAddSubscriptionWithInvalidUri() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyNeverSucceeds() : void inlined to public shouldErrorOnRemoveChannelOnUnknownStreamId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyNeverSucceeds() : void inlined to public shouldErrorOnRemoveChannelOnUnknownSessionId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyExceptionLogged() : void inlined to public shouldErrorOnAddSubscriptionWithInvalidUri() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyExceptionLogged() : void inlined to public shouldErrorOnRemoveChannelOnUnknownStreamId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyExceptionLogged() : void inlined to public shouldErrorOnRemoveChannelOnUnknownSessionId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyPublicationClosed(times VerificationMode) : void inlined to public shouldErrorOnRemoveChannelOnUnknownStreamId() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyReceiverSubscribes() : void inlined to public shouldNotTimeoutSubscriptionOnKeepAlive() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest",
            "Inline Method private verifyReceiverSubscribes() : void inlined to public shouldTimeoutSubscription() : void in class uk.co.real_logic.aeron.driver.DriverConductorTest");

          processTp("https://github.com/bumptech/glide.git", "0d4b27952751de0caab01774048c3e0ec74824ce", 
            "Extract Method package clearCallbacksAndListener() : void extracted from private checkCurrentDimens() : void in class com.bumptech.glide.request.target.ViewTarget.SizeDeterminer");

          processTp("https://github.com/eclipse/jetty.project.git", "1f3be625e62f44d929c01f6574678eea05754474", 
            "Extract Method public gatherScannables() : void extracted from public configureScanner() : void in class org.eclipse.jetty.maven.plugin.JettyRunMojo");

          processTp("https://github.com/brianfrankcooper/YCSB.git", "0b024834549c53512ef18bce89f60ef9225d4819", 
            "Extract Method private throttle(currTimeMillis long) : void extracted from public run() : void in class com.yahoo.ycsb.ClientThread");

          processTp("https://github.com/netty/netty.git", "303cb535239a6f07cbe24a033ef965e2f55758eb", 
            "Inline Method private principal(certs Certificate[]) : Principal inlined to public getLocalPrincipal() : Principal in class io.netty.handler.ssl.OpenSslEngine.OpenSslSession",
            "Inline Method private principal(certs Certificate[]) : Principal inlined to public getPeerPrincipal() : Principal in class io.netty.handler.ssl.OpenSslEngine.OpenSslSession");

          processTp("https://github.com/spring-projects/spring-boot.git", "b47634176fa48ad925f79886c6aaca225cb9af64", 
            "Extract Method private findAll(predicate Predicate<String>) : Iterable<Metric<?>> extracted from public findAll() : Iterable<Metric<?>> in class org.springframework.boot.actuate.metrics.buffer.BufferMetricReader",
            "Extract Method private findAll(predicate Predicate<String>) : Iterable<Metric<?>> extracted from public findAll(prefix String) : Iterable<Metric<?>> in class org.springframework.boot.actuate.metrics.buffer.BufferMetricReader");

          processTp("https://github.com/eucalyptus/eucalyptus.git", "5a38d0bca0e48853c3f7c00a0f098bada64797df", 
            "Move Class com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricCacheKey moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricQueue.AbsoluteMetricCacheKey",
            "Move Class com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricLoadCacheKey moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricQueue.AbsoluteMetricLoadCacheKey",
            "Move Class com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricCache moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricQueue.AbsoluteMetricCache",
            "Move Class com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHistory moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricHistory",
            "Move Class com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHelper moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricHelper");
          processFp("https://github.com/eucalyptus/eucalyptus.git", "5a38d0bca0e48853c3f7c00a0f098bada64797df", 
            "Move Class com.eucalyptus.cluster.callback.CloudWatchHelper.DefaultInstanceInfoProvider moved to com.eucalyptus.cluster.callback.cloudwatch.CloudWatchHelper.DefaultInstanceInfoProvider",
            "Move Class com.eucalyptus.cluster.callback.CloudWatchHelper.InstanceInfoProvider moved to com.eucalyptus.cluster.callback.cloudwatch.CloudWatchHelper.InstanceInfoProvider",
            "Move Class com.eucalyptus.cluster.callback.CloudWatchHelper.EC2DiskMetricCache moved to com.eucalyptus.cluster.callback.cloudwatch.CloudWatchHelper.EC2DiskMetricCache",
            "Move Class com.eucalyptus.cluster.callback.CloudWatchHelper.EC2DiskMetricCacheValue moved to com.eucalyptus.cluster.callback.cloudwatch.CloudWatchHelper.EC2DiskMetricCacheValue",
            "Move Class com.eucalyptus.cluster.callback.CloudWatchHelper.EC2DiskMetricCacheKey moved to com.eucalyptus.cluster.callback.cloudwatch.CloudWatchHelper.EC2DiskMetricCacheKey",
            "Move Class com.eucalyptus.cluster.callback.CloudWatchHelper.DiskReadWriteMetricTypeCache moved to com.eucalyptus.cluster.callback.cloudwatch.CloudWatchHelper.DiskReadWriteMetricTypeCache");
          processTp("https://github.com/FasterXML/jackson-databind.git", "da29a040ebae664274b28117b157044af0f525fa", 
            "Inline Method private _writeCloseableValue(gen JsonGenerator, value Object, cfg SerializationConfig) : void inlined to public writeValue(gen JsonGenerator, value Object) : void in class com.fasterxml.jackson.databind.ObjectWriter");

          processTp("https://github.com/spring-projects/spring-boot.git", "cb98ee25ff52bf97faebe3f45cdef0ced9b4416e", 
            "Extract Method private load(config Class<?>, environment String[]) : void extracted from public overrideMessageCodesFormat() : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfigurationTests",
            "Extract Method private load(config Class<?>, environment String[]) : void extracted from public overrideDateFormat() : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfigurationTests",
            "Extract Method private load(config Class<?>, environment String[]) : void extracted from public overrideLocale() : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfigurationTests");

          processTp("https://github.com/graphhopper/graphhopper.git", "7f80425b6a0af9bdfef12c8a873676e39e0a04a6", 
            "Move Method private stringHashCode(str String) : int from class com.graphhopper.storage.GraphHopperStorage to private stringHashCode(str String) : int from class com.graphhopper.storage.BaseGraph",
            "Move Method private nextGeoRef(arrayLength int) : int from class com.graphhopper.storage.GraphHopperStorage to private nextGeoRef(arrayLength int) : int from class com.graphhopper.storage.BaseGraph",
            "Move Method private ensureGeometry(bytePos long, byteLength int) : void from class com.graphhopper.storage.GraphHopperStorage to private ensureGeometry(bytePos long, byteLength int) : void from class com.graphhopper.storage.BaseGraph",
            "Move Method private isTestingEnabled() : boolean from class com.graphhopper.storage.GraphHopperStorage to private isTestingEnabled() : boolean from class com.graphhopper.storage.BaseGraph",
            "Move Method private getRemovedNodes() : GHBitSet from class com.graphhopper.storage.GraphHopperStorage to package getRemovedNodes() : GHBitSet from class com.graphhopper.storage.BaseGraph",
            "Move Method private inPlaceNodeRemove(removeNodeCount int) : void from class com.graphhopper.storage.GraphHopperStorage to package inPlaceNodeRemove(removeNodeCount int) : void from class com.graphhopper.storage.BaseGraph",
            "Move Method private trimToSize() : void from class com.graphhopper.storage.GraphHopperStorage to protected trimToSize() : void from class com.graphhopper.storage.BaseGraph",
            "Move Method private invalidateEdge(edgePointer long) : void from class com.graphhopper.storage.GraphHopperStorage to private invalidateEdge(edgePointer long) : void from class com.graphhopper.storage.BaseGraph",
            "Move Method package internalEdgeDisconnect(edgeToRemove int, edgeToUpdatePointer long, baseNode int, adjNode int) : long from class com.graphhopper.storage.GraphHopperStorage to package internalEdgeDisconnect(edgeToRemove int, edgeToUpdatePointer long, baseNode int, adjNode int) : long from class com.graphhopper.storage.BaseGraph",
            "Move Method protected createSingleEdge(edgeId int, nodeId int) : SingleEdge from class com.graphhopper.storage.GraphHopperStorage to protected createSingleEdge(edgeId int, nodeId int) : SingleEdge from class com.graphhopper.storage.BaseGraph",
            "Move Method private getOtherNode(nodeThis int, edgePointer long) : int from class com.graphhopper.storage.GraphHopperStorage to private getOtherNode(nodeThis int, edgePointer long) : int from class com.graphhopper.storage.BaseGraph",
            "Move Method public getDebugInfo(node int, area int) : String from class com.graphhopper.storage.GraphHopperStorage to public getDebugInfo(node int, area int) : String from class com.graphhopper.storage.BaseGraph",
            "Move Method protected getLinkPosInEdgeArea(nodeThis int, nodeOther int, edgePointer long) : long from class com.graphhopper.storage.GraphHopperStorage to protected getLinkPosInEdgeArea(nodeThis int, nodeOther int, edgePointer long) : long from class com.graphhopper.storage.BaseGraph",
            "Move Method private writeEdge(edge int, nodeThis int, nodeOther int, nextEdge int, nextEdgeOther int) : long from class com.graphhopper.storage.GraphHopperStorage to private writeEdge(edge int, nodeThis int, nodeOther int, nextEdge int, nextEdgeOther int) : long from class com.graphhopper.storage.BaseGraph",
            "Move Method private connectNewEdge(fromNode int, newOrExistingEdge int) : void from class com.graphhopper.storage.GraphHopperStorage to private connectNewEdge(fromNode int, newOrExistingEdge int) : void from class com.graphhopper.storage.BaseGraph",
            "Move Method private nextEdge() : int from class com.graphhopper.storage.GraphHopperStorage to private nextEdge() : int from class com.graphhopper.storage.BaseGraph",
            "Move Method package setEdgeCount(cnt int) : void from class com.graphhopper.storage.GraphHopperStorage to package setEdgeCount(cnt int) : void from class com.graphhopper.storage.BaseGraph",
            "Move Method package internalEdgeAdd(fromNodeId int, toNodeId int) : int from class com.graphhopper.storage.GraphHopperStorage to package internalEdgeAdd(fromNodeId int, toNodeId int) : int from class com.graphhopper.storage.BaseGraph",
            "Move Method package copyProperties(from EdgeIteratorState, to EdgeIteratorState) : EdgeIteratorState from class com.graphhopper.storage.GraphHopperStorage to package copyProperties(from EdgeIteratorState, to EdgeIteratorState) : EdgeIteratorState from class com.graphhopper.storage.BaseGraph",
            "Move Method package ensureNodeIndex(nodeIndex int) : void from class com.graphhopper.storage.GraphHopperStorage to package ensureNodeIndex(nodeIndex int) : void from class com.graphhopper.storage.BaseGraph",
            "Move Method private getDist(pointer long) : double from class com.graphhopper.storage.GraphHopperStorage to private getDist(pointer long) : double from class com.graphhopper.storage.BaseGraph",
            "Move Method private distToInt(distance double) : int from class com.graphhopper.storage.GraphHopperStorage to private distToInt(distance double) : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected initNodeAndEdgeEntrySize() : void from class com.graphhopper.storage.GraphHopperStorage to protected initNodeAndEdgeEntrySize() : void from class com.graphhopper.storage.BaseGraph",
            "Move Method protected nextNodeEntryIndex(sizeInBytes int) : int from class com.graphhopper.storage.GraphHopperStorage to protected nextNodeEntryIndex(sizeInBytes int) : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected nextEdgeEntryIndex(sizeInBytes int) : int from class com.graphhopper.storage.GraphHopperStorage to protected nextEdgeEntryIndex(sizeInBytes int) : int from class com.graphhopper.storage.BaseGraph",
            "Move Method package checkInit() : void from class com.graphhopper.storage.GraphHopperStorage to package checkInit() : void from class com.graphhopper.storage.BaseGraph",
            "Move Method protected loadNodesHeader() : int from class com.graphhopper.storage.GraphHopperStorage to protected loadNodesHeader() : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected setNodesHeader() : int from class com.graphhopper.storage.GraphHopperStorage to protected setNodesHeader() : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected loadEdgesHeader() : int from class com.graphhopper.storage.GraphHopperStorage to protected loadEdgesHeader() : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected setEdgesHeader() : int from class com.graphhopper.storage.GraphHopperStorage to protected setEdgesHeader() : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected loadWayGeometryHeader() : int from class com.graphhopper.storage.GraphHopperStorage to protected loadWayGeometryHeader() : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected setWayGeometryHeader() : int from class com.graphhopper.storage.GraphHopperStorage to protected setWayGeometryHeader() : int from class com.graphhopper.storage.BaseGraph",
            "Move Method protected initStorage() : void from class com.graphhopper.storage.GraphHopperStorage to package initStorage() : void from class com.graphhopper.storage.BaseGraph",
            "Move Method private initNodeRefs(oldCapacity long, newCapacity long) : void from class com.graphhopper.storage.GraphHopperStorage to package initNodeRefs(oldCapacity long, newCapacity long) : void from class com.graphhopper.storage.BaseGraph",
            "Move Method private ensureEdgeIndex(edgeIndex int) : void from class com.graphhopper.storage.GraphHopperStorage to private ensureEdgeIndex(edgeIndex int) : void from class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected nodeCount : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected nodes : DataAccess from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected nodeEntryBytes : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected N_ADDITIONAL : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected N_ELE : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected N_LON : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected N_LAT : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected N_EDGE_REF : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected edgeCount : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected edges : DataAccess from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private initialized : boolean from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected edgeEntryBytes : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private NO_NODE : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private MAX_EDGES : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private INT_DIST_FACTOR : double from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_NODEA : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_NODEB : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_LINKA : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_LINKB : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_DIST : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_FLAGS : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_GEO : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_NAME : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute protected E_ADDITIONAL : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute package bounds : BBox from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private removedNodes : GHBitSet from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private edgeEntryIndex : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private nodeEntryIndex : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute package nodeAccess : NodeAccess from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute package extStorage : GraphExtension from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute package wayGeometry : DataAccess from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute private maxGeoRef : int from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute package nameIndex : NameIndex from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute package flagsSizeIsLong : boolean from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph",
            "Move Attribute package bitUtil : BitUtil from class com.graphhopper.storage.GraphHopperStorage to class com.graphhopper.storage.BaseGraph");


          processFp("https://github.com/apache/tomcat.git", "89c3cb56a2c44421290eaee7ffe06be323631fb2", 
            "Inline Method private getAuthContext(authConfig ServerAuthConfig, authContextId String, authProperties Map, authContext ServerAuthContext) : ServerAuthContext inlined to public authenticate(request Request, response HttpServletResponse) : boolean in class org.apache.catalina.authenticator.jaspic.JaspicAuthenticator");
          processTp("https://github.com/geoserver/geoserver.git", "182f4d1174036417aad9d6db908ceaf64234fd5f", 
            "Pull Up Method public post(task ImportTask, data ImportData) : void from class org.geoserver.importer.transform.VectorTransformChain to public post(task ImportTask, data ImportData) : void from class org.geoserver.importer.transform.TransformChain",
            "Pull Up Method public pre(item ImportTask, data ImportData) : void from class org.geoserver.importer.transform.VectorTransformChain to public pre(item ImportTask, data ImportData) : void from class org.geoserver.importer.transform.TransformChain");

          processTp("https://github.com/restlet/restlet-framework-java.git", "e4de9513d0a06d381b4521211cf688b77379c39d", 
            "Move Method private computeSectionName(apiDeclarationPath String) : String from class org.restlet.ext.apispark.internal.conversion.swagger.v1_2.SwaggerReader to public computeSectionName(apiDeclarationPath String) : String from class org.restlet.ext.apispark.internal.conversion.swagger.v1_2.SwaggerUtils");


          processFp("https://github.com/datastax/java-driver.git", "4cf08b5552a7bcb8d4b158d03e9eae569875e511", 
            "Extract Method package appendName(name String, sb StringBuilder) : StringBuilder extracted from package appendName(name Object, sb StringBuilder) : StringBuilder in class com.datastax.driver.core.querybuilder.Utils",
            "Inline Method package appendName(name Object, sb StringBuilder, raw boolean) : StringBuilder inlined to package appendName(name Object, sb StringBuilder) : StringBuilder in class com.datastax.driver.core.querybuilder.Utils",
            "Inline Method package appendName(name String, sb StringBuilder, raw boolean) : StringBuilder inlined to package appendName(name Object, sb StringBuilder) : StringBuilder in class com.datastax.driver.core.querybuilder.Utils",
            "Inline Method package joinAndAppendNames(sb StringBuilder, separator String, values List<?>, raw boolean) : StringBuilder inlined to package joinAndAppendNames(sb StringBuilder, separator String, values List<?>) : StringBuilder in class com.datastax.driver.core.querybuilder.Utils");

          processFp("https://github.com/hierynomus/sshj.git", "e334525da503d04a978eb9482ab8c7aec02a0b69", 
            "Move Class net.schmizz.sshj.userauth.GssApiTest.TestAuthConfiguration moved to com.hierynomus.sshj.userauth.GssApiTest.TestAuthConfiguration");
          processTp("https://github.com/bennidi/mbassador.git", "40e41d11d7847d660bba6691859b0506514bd0ac", 
            "Move Class net.engio.mbassy.ConditionalHandlers.ConditionalMessageListener moved to net.engio.mbassy.ConditionalHandlerTest.ConditionalMessageListener",
            "Move Class net.engio.mbassy.ConditionalHandlers.TestEvent moved to net.engio.mbassy.ConditionalHandlerTest.TestEvent");

          processTp("https://github.com/Graylog2/graylog2-server.git", "2ef067fc70055fc4d55c75937303414ddcf07e0e", 
            "Extract Superclass integration.BaseRestTestHelper from classes [integration.BaseRestTest]",
            "Move Class integration.BaseRestTest.KeysPresentMatcher moved to integration.BaseRestTestHelper.KeysPresentMatcher");

          processTp("https://github.com/hazelcast/hazelcast.git", "e66e49cd4a9dd8027204f712f780170a5c129f5b", 
            "Move Class com.hazelcast.spi.ServiceInfo moved to com.hazelcast.spi.impl.servicemanager.ServiceInfo");


          processFp("https://github.com/Graylog2/graylog2-server.git", "4b1bbc0307d37146d33fca250ac854f1522359e6", 
            "Move Class integration.RequiredVersionRule.IgnoreStatement moved to integration.IgnoreStatement");
          processTp("https://github.com/open-keychain/open-keychain.git", "49d544d558e9c7f1106b5923204b1fbec2696cf7", 
            "Move Class org.sufficientlysecure.keychain.util.orbot.OrbotHelper moved to org.sufficientlysecure.keychain.util.tor.OrbotHelper");


          processFp("https://github.com/JetBrains/MPS.git", "0572e944622735a5583dba40b8b4af3adc160868", 
            "Extract Method protected replaceNonAsciiSymbolsWithUnicodeSymbols(s String, ctx TextGenContext) : String extracted from protected replaceNonAsciiSymbolsWithUnicodeSymbols(s String, textGen SNodeTextGen) : String in class jetbrains.mps.baseLanguage.textGen.StringTextGen",
            "Extract Method protected getPackageAndShortName(classifierRef SReference, ctx TextGenContext) : _2<String,String> extracted from protected getPackageAndShortName(classifierRef SReference, textGen SNodeTextGen) : _2<String,String> in class jetbrains.mps.baseLanguage.textGen.BaseLanguageTextGen");

          processFp("https://github.com/google/j2objc.git", "d8172732456b8be61bc12b74322ecbe9a958f301", 
            "Pull Up Method private needsCompanionClassDeclaration() : boolean from class com.google.devtools.j2objc.gen.TypeDeclarationGenerator to private hasStaticAccessorMethods() : boolean from class com.google.devtools.j2objc.gen.TypeGenerator");
          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "15fd4f9caea01e53725086e290d3b35ec4bd4cd9", 
            "Extract Method protected reset(animateTransition boolean) : void extracted from public reset() : void in class com.android.keyguard.KeyguardAbsKeyInputView");


          processFp("https://github.com/spring-projects/spring-data-jpa.git", "43cbfb7dc8d7c1d557777357e8606bd188ce4616", 
            "Move Class org.springframework.data.jpa.repository.augment.JpaSoftDeleteQueryAugmentor.PropertyChangeEnsuringBeanWrapper moved to org.springframework.data.jpa.repository.augment.PropertyChangeEnsuringBeanWrapper");
          processTp("https://github.com/Netflix/eureka.git", "5103ace802b2819438318dd53b5b07512aae0d25", 
            "Extract Method public fillUpRegistryOfServer(serverIdx int, count int, instanceTemplate InstanceInfo) : void extracted from public fillUpRegistry(count int, instanceTemplate InstanceInfo) : void in class com.netflix.eureka2.integration.EurekaDeploymentClients");


          processFp("https://github.com/open-keychain/open-keychain.git", "b7834b432697af8026ae403b5841fedde1697a6f", 
            "Inline Method public modifySecretKeyRing(wsKR CanonicalizedSecretKeyRing, cryptoInput CryptoInputParcel, saveParcel SaveKeyringParcel, log OperationLog, indent int) : PgpEditKeyResult inlined to public modifySecretKeyRing(wsKR CanonicalizedSecretKeyRing, cryptoInput CryptoInputParcel, saveParcel SaveKeyringParcel) : PgpEditKeyResult in class org.sufficientlysecure.keychain.pgp.PgpKeyOperation");

          processFp("https://github.com/petrnohejl/Android-Templates-And-Utilities.git", "d3d5c74917f7974f7f910067ce3b60d5dccf5fb6", 
            "Move Method public onCreate(savedInstanceState Bundle) : void from class com.example.fragment.ExampleFragment to public onCreate(savedInstanceState Bundle) : void from class com.example.fragment.ListingFragment");
          processTp("https://github.com/AsyncHttpClient/async-http-client.git", "f01d8610b9ceebc1de59d42f569b8af3efbe0a0f", 
            "Extract Method package signatureBaseString(method String, uri Uri, oauthTimestamp long, nonce String, formParams List<Param>, queryParams List<Param>) : StringBuilder extracted from public calculateSignature(method String, uri Uri, oauthTimestamp long, nonce String, formParams List<Param>, queryParams List<Param>) : String in class org.asynchttpclient.oauth.OAuthSignatureCalculator");

          processTp("https://github.com/square/okhttp.git", "c753d2e41ba667f9b5a31451a16ecbaecdc65d80", 
            "Move Class com.squareup.okhttp.internal.spdy.Variant moved to com.squareup.okhttp.internal.framed.Variant",
            "Move Class com.squareup.okhttp.internal.spdy.Spdy3 moved to com.squareup.okhttp.internal.framed.Spdy3",
            "Move Class com.squareup.okhttp.internal.spdy.Settings moved to com.squareup.okhttp.internal.framed.Settings",
            "Move Class com.squareup.okhttp.internal.spdy.PushObserver moved to com.squareup.okhttp.internal.framed.PushObserver",
            "Move Class com.squareup.okhttp.internal.spdy.Ping moved to com.squareup.okhttp.internal.framed.Ping",
            "Move Class com.squareup.okhttp.internal.spdy.HuffmanTest moved to com.squareup.okhttp.internal.framed.HuffmanTest",
            "Move Class com.squareup.okhttp.internal.spdy.Http2Test moved to com.squareup.okhttp.internal.framed.Http2Test",
            "Move Class com.squareup.okhttp.internal.spdy.Http2FrameLoggerTest moved to com.squareup.okhttp.internal.framed.Http2FrameLoggerTest",
            "Move Class com.squareup.okhttp.internal.spdy.Http2ConnectionTest moved to com.squareup.okhttp.internal.framed.Http2ConnectionTest",
            "Move Class com.squareup.okhttp.internal.spdy.HpackTest moved to com.squareup.okhttp.internal.framed.HpackTest",
            "Move Class com.squareup.okhttp.internal.spdy.BaseTestHandler moved to com.squareup.okhttp.internal.framed.BaseTestHandler",
            "Move Class com.squareup.okhttp.internal.spdy.hpackjson.Story moved to com.squareup.okhttp.internal.framed.hpackjson.Story",
            "Move Class com.squareup.okhttp.internal.spdy.hpackjson.HpackJsonUtil moved to com.squareup.okhttp.internal.framed.hpackjson.HpackJsonUtil",
            "Move Class com.squareup.okhttp.internal.spdy.hpackjson.Case moved to com.squareup.okhttp.internal.framed.hpackjson.Case",
            "Move Class com.squareup.okhttp.internal.spdy.HpackRoundTripTest moved to com.squareup.okhttp.internal.framed.HpackRoundTripTest",
            "Move Class com.squareup.okhttp.internal.spdy.HpackDecodeTestBase moved to com.squareup.okhttp.internal.framed.HpackDecodeTestBase",
            "Move Class com.squareup.okhttp.internal.spdy.HpackDecodeInteropTest moved to com.squareup.okhttp.internal.framed.HpackDecodeInteropTest",
            "Move Class com.squareup.okhttp.internal.spdy.MockSpdyPeer moved to com.squareup.okhttp.internal.framed.MockSpdyPeer",
            "Move Class com.squareup.okhttp.internal.spdy.SettingsTest moved to com.squareup.okhttp.internal.framed.SettingsTest",
            "Move Class com.squareup.okhttp.internal.spdy.Spdy3ConnectionTest moved to com.squareup.okhttp.internal.framed.Spdy3ConnectionTest",
            "Move Class com.squareup.okhttp.internal.spdy.Spdy3Test moved to com.squareup.okhttp.internal.framed.Spdy3Test",
            "Move Class com.squareup.okhttp.internal.spdy.FrameReader moved to com.squareup.okhttp.internal.framed.FrameReader",
            "Move Class com.squareup.okhttp.internal.spdy.FrameWriter moved to com.squareup.okhttp.internal.framed.FrameWriter",
            "Move Class com.squareup.okhttp.internal.spdy.Header moved to com.squareup.okhttp.internal.framed.Header",
            "Move Class com.squareup.okhttp.internal.spdy.Hpack moved to com.squareup.okhttp.internal.framed.Hpack",
            "Move Class com.squareup.okhttp.internal.spdy.Http2 moved to com.squareup.okhttp.internal.framed.Http2",
            "Move Class com.squareup.okhttp.internal.spdy.Huffman moved to com.squareup.okhttp.internal.framed.Huffman",
            "Move Class com.squareup.okhttp.internal.spdy.IncomingStreamHandler moved to com.squareup.okhttp.internal.framed.IncomingStreamHandler",
            "Move Class com.squareup.okhttp.internal.spdy.NameValueBlockReader moved to com.squareup.okhttp.internal.framed.NameValueBlockReader");

          processTp("https://github.com/rstudio/rstudio.git", "9a581e07cb6381d70f3fd9bb2055e810e2a682a9", 
            "Extract Method private getBoolean(key String) : boolean extracted from public init(widget AceEditorWidget, position Position) : void in class org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOptionsPopupPanel",
            "Extract Method private has(key String) : boolean extracted from public init(widget AceEditorWidget, position Position) : void in class org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOptionsPopupPanel");

          processTp("https://github.com/spring-projects/spring-integration.git", "247232bdde24b81814a82100743f77d881aaf06b", 
            "Extract Method private handleInputStreamMessage(sourceFileInputStream InputStream, originalFile File, tempFile File, resultFile File) : File extracted from private handleFileMessage(sourceFile File, tempFile File, resultFile File) : File in class org.springframework.integration.file.FileWritingMessageHandler");

          processTp("https://github.com/open-keychain/open-keychain.git", "c11fef6e7c80681ce69e5fdc7f4796b0b7a18e2b", 
            "Extract Method protected cryptoOperation(cryptoInput CryptoInputParcel, showProgress boolean) : void extracted from protected cryptoOperation(cryptoInput CryptoInputParcel) : void in class org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment",
            "Extract Method public displayInputFragment(showOpenDialog boolean) : void extracted from private handleActions(savedInstanceState Bundle, intent Intent) : void in class org.sufficientlysecure.keychain.ui.DecryptFilesActivity");

          processTp("https://github.com/apache/pig.git", "7a1659c12d76b510809dea1dea1f5100bcf4cd60", 
            "Extract Method private initialize() : void extracted from public launchPig(physicalPlan PhysicalPlan, grpName String, pigContext PigContext) : PigStats in class org.apache.pig.backend.hadoop.executionengine.spark.SparkLauncher");


          processFp("https://github.com/square/retrofit.git", "f431d3fd3d36d849ab15af16b28cd9d0dda8f5f5", 
            "Move Class retrofit.SimpleXmlConverterTest.MyObject moved to retrofit.MyObject");
          processTp("https://github.com/infinispan/infinispan.git", "ce4f6292d6350a2c6b82d995352fdf6d07042c9c", 
            "Extract Method protected assertNoTransactions(cacheName String) : void extracted from protected assertNoTransactions() : void in class org.infinispan.test.MultipleCacheManagersTest",
            "Extract Method protected eventually(message String, ec Condition, timeout long, pollInterval long, unit TimeUnit) : void extracted from protected eventually(ec Condition, timeout long, pollInterval long, unit TimeUnit) : void in class org.infinispan.test.AbstractInfinispanTest",
            "Extract Method protected eventually(message String, ec Condition, timeoutMillis long, loops int) : void extracted from protected eventually(ec Condition, timeoutMillis long, loops int) : void in class org.infinispan.test.AbstractInfinispanTest",
            "Extract Method public throwRollbackExceptionIfAny() : void extracted from public commit() : void in class org.infinispan.transaction.tm.DummyTransaction",
            "Extract Method private createRollbackRpcOptions() : RpcOptions extracted from public visitRollbackCommand(ctx TxInvocationContext, command RollbackCommand) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
            "Extract Method private replayRemoteTransactionIfNeeded(ctx RemoteTxInvocationContext, topologyId int) : void extracted from public visitCommitCommand(ctx TxInvocationContext, command CommitCommand) : Object in class org.infinispan.interceptors.TxInterceptor",
            "Extract Method private verifyRemoteTransaction(ctx RemoteTxInvocationContext, command AbstractTransactionBoundaryCommand) : void extracted from private invokeNextInterceptorAndVerifyTransaction(ctx TxInvocationContext, command AbstractTransactionBoundaryCommand) : Object in class org.infinispan.interceptors.TxInterceptor",
            "Move Attribute private configuration : Configuration from class org.infinispan.tx.TransactionXaAdapterTmIntegrationTest to class org.infinispan.partitionhandling.impl.PartitionHandlingManagerImpl",
            "Inline Method private sendCommitCommand(ctx TxInvocationContext, command CommitCommand) : void inlined to public visitCommitCommand(ctx TxInvocationContext, command CommitCommand) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
            "Inline Method protected lockAndWrap(ctx InvocationContext, key Object, ice InternalCacheEntry, command FlagAffectedCommand) : void inlined to private remoteGet(ctx InvocationContext, key Object, isWrite boolean, command FlagAffectedCommand) : InternalCacheEntry in class org.infinispan.interceptors.distribution.TxDistributionInterceptor",
            "Inline Method protected lockAndWrap(ctx InvocationContext, key Object, ice InternalCacheEntry, command FlagAffectedCommand) : void inlined to private localGet(ctx InvocationContext, key Object, isWrite boolean, command FlagAffectedCommand, isGetCacheEntry boolean) : Object in class org.infinispan.interceptors.distribution.TxDistributionInterceptor");


          processFp("https://github.com/eclipse/jetty.project.git", "1a572c3236073d7df7d002873e42a2f98358bc7a", 
            "Extract Method public size() : int extracted from public getPatterns() : String[] in class org.eclipse.jetty.webapp.ClasspathPattern");
          processTp("https://github.com/grails/grails-core.git", "480537e0f8aaf50a7648bf445b33230aa32a9b44", 
            "Extract Method public weaveMixinsIntoClass(classNode ClassNode, values ListExpression, applicationClassNode ClassNode) : void extracted from public weaveMixinsIntoClass(classNode ClassNode, values ListExpression) : void in class org.grails.compiler.injection.test.TestMixinTransformation",
            "Extract Method public weaveTestMixins(classNode ClassNode, values ListExpression, applicationClassNode ClassNode) : void extracted from public weaveTestMixins(classNode ClassNode, values ListExpression) : void in class org.grails.compiler.injection.test.TestMixinTransformation");
          processFp("https://github.com/grails/grails-core.git", "480537e0f8aaf50a7648bf445b33230aa32a9b44", 
            "Extract Method protected weaveMixinIntoClass(classNode ClassNode, mixinClassNode ClassNode, junit3MethodHandler Junit3TestFixtureMethodHandler, applicationClassNode ClassNode) : void extracted from protected weaveMixinIntoClass(classNode ClassNode, mixinClassNode ClassNode, junit3MethodHandler Junit3TestFixtureMethodHandler) : void in class org.grails.compiler.injection.test.TestMixinTransformation");
          processTp("https://github.com/jfinal/jfinal.git", "881baed894540031bd55e402933bcad28b74ca88", 
            "Extract Method private validateLongValue(value String, errorKey String, errorMessage String) : void extracted from protected validateLong(field String, errorKey String, errorMessage String) : void in class com.jfinal.validate.Validator",
            "Extract Method private validateLongValue(value String, min long, max long, errorKey String, errorMessage String) : void extracted from protected validateLong(field String, min long, max long, errorKey String, errorMessage String) : void in class com.jfinal.validate.Validator",
            "Extract Method private validateIntegerValue(value String, min int, max int, errorKey String, errorMessage String) : void extracted from protected validateInteger(field String, min int, max int, errorKey String, errorMessage String) : void in class com.jfinal.validate.Validator");


          processFp("https://github.com/datastax/java-driver.git", "39a3ab29f67786fbf320b599e62b959370cec171", 
            "Move Class com.datastax.driver.core.SpeculativeExecutionTest.SortingLoadBalancingPolicy moved to com.datastax.driver.core.SortingLoadBalancingPolicy");
          processTp("https://github.com/SonarSource/sonarqube.git", "021bf45623b748e70f20d956e86d595191241786", 
            "Extract Method package getPluginMetrics() : List<Metric> extracted from public start() : void in class org.sonar.server.startup.RegisterMetrics");
          processFp("https://github.com/SonarSource/sonarqube.git", "021bf45623b748e70f20d956e86d595191241786", 
            "Extract Method package register(metrics Iterable<Metric>) : void extracted from public start() : void in class org.sonar.server.startup.RegisterMetrics");
          processTp("https://github.com/wordpress-mobile/WordPress-Android.git", "f8d5cf01f123a1d0a65857aa2db0571fe9cd1911", 
            "Extract Method private getIconImageURL(size int, iconUrl String, blogUrl String) : String extracted from public getIconImageURL(size int) : String in class org.wordpress.android.models.Blog");

          processTp("https://github.com/neo4j/neo4j.git", "b83e6a535cbca21d5ea764b0c49bfca8a9ff9db4", 
            "Extract Method protected query(query Query) : PrimitiveLongIterator extracted from public scan() : PrimitiveLongIterator in class org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReader",
            "Extract Method protected query(query Query) : PrimitiveLongIterator extracted from public lookup(value Object) : PrimitiveLongIterator in class org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReader",
            "Extract Superclass org.neo4j.kernel.api.impl.index.AbstractLuceneIndexAccessorReaderTest from classes [org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReaderTest, org.neo4j.kernel.api.impl.index.LuceneUniqueIndexAccessorReaderTest]");

          processTp("https://github.com/baasbox/baasbox.git", "d949fe9079a82ee31aa91244aa67baaf56b7e28f", 
            "Extract Method public execMultiLineCommands(db ODatabaseRecordTx, log boolean, stopOnException boolean, commands String[]) : void extracted from public execMultiLineCommands(db ODatabaseRecordTx, log boolean, commands String[]) : void in class com.baasbox.db.DbHelper");

          processTp("https://github.com/wordpress-mobile/WordPress-Android.git", "ab298886b59f4ad0235cd6d5764854189eb59eb6", 
            "Extract Method public openPostInReaderOrInAppWebview(ctx Context, remoteBlogID String, remoteItemID String, itemType String, itemURL String) : void extracted from public openPostInReaderOrInAppWebview(ctx Context, post PostModel) : void in class org.wordpress.android.ui.stats.StatsUtils");
          processFp("https://github.com/wordpress-mobile/WordPress-Android.git", "ab298886b59f4ad0235cd6d5764854189eb59eb6", 
            "Move Class org.wordpress.android.ui.stats.StatsSinglePostDetailsActivity.RestBatchCallListener moved to org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity.RestBatchCallListener",
            "Move Class org.wordpress.android.ui.stats.StatsSinglePostDetailsActivity.MonthsAndYearsListAdapter moved to org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity.MonthsAndYearsListAdapter",
            "Move Class org.wordpress.android.ui.stats.StatsSinglePostDetailsActivity.RecentWeeksListAdapter moved to org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity.RecentWeeksListAdapter");
          processTp("https://github.com/jberkel/sms-backup-plus.git", "c265bde2ace252bc1e1c65c6af93520e5994edd2", 
            "Extract Method public getTokenForLogging() : String extracted from public toString() : String in class com.zegoggles.smssync.auth.OAuth2Token");


          processFp("https://github.com/fabric8io/fabric8.git", "d24eb2afb5457f162d1d772344c010d32e037082", 
            "Move Class io.fabric8.repo.git.GerritProjectInfoDTO.WebLinkInfo moved to io.fabric8.gerrit.WebLinkInfo");
          processTp("https://github.com/fabric8io/fabric8.git", "07807aed847e1d0589c094461544e54a2677cbf5", 
            "Pull Up Attribute protected combineDependencies : boolean from class io.fabric8.maven.JsonMojo to class io.fabric8.maven.AbstractFabric8Mojo",
            "Pull Up Attribute private DEFAULT_CONFIG_FILE_NAME : String from class io.fabric8.maven.ApplyMojo to class io.fabric8.maven.AbstractFabric8Mojo",
            "Pull Up Method private hasKubernetesJson(f File) : boolean from class io.fabric8.maven.ApplyMojo to package hasKubernetesJson(f File) : boolean from class io.fabric8.maven.AbstractFabric8Mojo",
            "Pull Up Method protected isKubernetesJsonArtifact(classifier String, type String) : boolean from class io.fabric8.maven.JsonMojo to package isKubernetesJsonArtifact(classifier String, type String) : boolean from class io.fabric8.maven.AbstractFabric8Mojo",
            "Pull Up Method private getDependencies() : Set<File> from class io.fabric8.maven.ApplyMojo to package getDependencies() : Set<File> from class io.fabric8.maven.AbstractFabric8Mojo");

          processTp("https://github.com/processing/processing.git", "8707194f003444a9fb8e00bffa2893ef0c2492c6", 
            "Inline Method private setFrameCentered() : void inlined to public placeWindow(location int[], editorLocation int[]) : void in class processing.opengl.PSurfaceJOGL");

          processTp("https://github.com/google/closure-compiler.git", "5a853a60f93e09c446d458673bc7a2f6bb26742c", 
            "Move Class com.google.javascript.jscomp.parsing.TypeDeclarationsIRFactory moved to com.google.javascript.jscomp.JsdocToEs6TypedConverter.TypeDeclarationsIRFactory");

          processTp("https://github.com/rackerlabs/blueflood.git", "c76e6e1f27a6697b3b88ad4ed710441b801afb3b", 
            "Move Attribute private sendResponseTimer : Timer from class com.rackspacecloud.blueflood.inputs.handlers.HttpMetricsIngestionHandler to class com.rackspacecloud.blueflood.http.DefaultHandler",
            "Move Method public sendResponse(channel ChannelHandlerContext, request HttpRequest, messageBody String, status HttpResponseStatus) : void from class com.rackspacecloud.blueflood.inputs.handlers.HttpMetricsIngestionHandler to public sendResponse(channel ChannelHandlerContext, request HttpRequest, messageBody String, status HttpResponseStatus) : void from class com.rackspacecloud.blueflood.http.DefaultHandler");


          processFp("https://github.com/apache/zookeeper.git", "697d77ff65b746cc05049e73007cef5ba9fe7e5d", 
            "Extract Superclass org.apache.zookeeper.server.ZooKeeperThread from classes [org.apache.zookeeper.server.quorum.FastLeaderElection.Messenger.WorkerReceiver, org.apache.zookeeper.server.quorum.FastLeaderElection.Messenger.WorkerSender]");
          processTp("https://github.com/amplab/tachyon.git", "ed966510ccf8441115614e2258aea61df0ea55f5", 
            "Extract Method private reserveSpace(size long) : void extracted from public addBlockMeta(block BlockMeta) : Optional<BlockMeta> in class tachyon.worker.block.meta.StorageDir");

          processTp("https://github.com/k9mail/k-9.git", "23c49d834d3859fc76a604da32d1789d2e863303", 
            "Extract Method private setNotificationContent(context Context, message Message, sender CharSequence, subject CharSequence, builder Builder, accountDescr String) : Builder extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController",
            "Extract Method private buildNotificationNavigationStack(context Context, account Account, message LocalMessage, newMessages int, unreadCount int, allRefs ArrayList<MessageReference>) : TaskStackBuilder extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController");
          processFp("https://github.com/k9mail/k-9.git", "23c49d834d3859fc76a604da32d1789d2e863303", 
            "Extract Method private addWearActions(builder Builder, totalMsgCount int, msgCount int, account Account, allRefs ArrayList<MessageReference>, messages List<? extends Message>, notificationID int) : void extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController");
          processTp("https://github.com/antlr/antlr4.git", "a9ca2efae56815dc464189b055ffe9da23766f7f", 
            "Extract Method public getAmbuityParserInterpreter(g Grammar, originalParser Parser, tokens TokenStream) : ParserInterpreter extracted from public getAllPossibleParseTrees(g Grammar, originalParser Parser, tokens TokenStream, decision int, alts BitSet, startIndex int, stopIndex int, startRuleIndex int) : List<ParserRuleContext> in class org.antlr.v4.tool.GrammarParserInterpreter",
            "Extract Method public getDescendants(t ParseTree) : List<ParseTree> extracted from public descendants(t ParseTree) : List<ParseTree> in class org.antlr.v4.runtime.tree.Trees");

          processTp("https://github.com/gradle/gradle.git", "f841d8dda2bf461f595755f85c3eba786783702d", 
            "Inline Method private adaptResult(result BuildResult, startTime long, endTime long) : AbstractOperationResult inlined to private adaptResult(source BuildOperationInternal) : AbstractOperationResult in class org.gradle.tooling.internal.provider.runner.ClientForwardingBuildListener");

          processTp("https://github.com/eclipse/jetty.project.git", "13b63c194b010201c439932ece2f1bc628ebf287", 
            "Move Attribute private __propertyPattern : Pattern from class org.eclipse.jetty.xml.XmlConfiguration to class org.eclipse.jetty.start.Props");


          processFp("https://github.com/JetBrains/intellij-plugins.git", "92d56f6dc1661c00f619fd695689d10f451499d7", 
            "Move Class com.jetbrains.lang.dart.fixes.DartServerFixIntention.DartLookupExpression moved to com.jetbrains.lang.dart.fixes.DartQuickFix.DartLookupExpression",
            "Move Class com.jetbrains.lang.dart.fixes.DartServerFixIntention.SuggestionInfo moved to com.jetbrains.lang.dart.fixes.DartQuickFix.SuggestionInfo");

          processFp("https://github.com/SonarSource/sonarqube.git", "b0c8aeece75f1dcd7069fe4f496eeba2a7832fc4", 
            "Move Attribute private LOG : Logger from class org.sonar.batch.scan.ProjectSettings to class org.sonar.batch.scan.ProjectScanContainer",
            "Move Attribute private LOG : Logger from class org.sonar.batch.scan.ModuleSettings to class org.sonar.batch.scan.ProjectScanContainer");
          processTp("https://github.com/SonarSource/sonarqube.git", "0eaa5217883cfeca688aad1d462192c194741827", 
            "Move Attribute private userWriter : UserJsonWriter from class org.sonar.server.issue.ws.IssueJsonWriter to class org.sonar.server.issue.InternalRubyIssueService");
          processFp("https://github.com/SonarSource/sonarqube.git", "0eaa5217883cfeca688aad1d462192c194741827", 
            "Move Attribute package userWriter : UserJsonWriter from class org.sonar.server.issue.ws.IssueJsonWriter to class org.sonar.server.issue.InternalRubyIssueServiceTest");
          processTp("https://github.com/spring-projects/spring-framework.git", "ece12f9d370108549fffac105e4bcb7faeaaf124", 
            "Extract Method private assertMissingTextAttribute(attributes Map<String,Object>) : void extracted from public synthesizeAnnotationFromMapWithNullAttributeValue() : void in class org.springframework.core.annotation.AnnotationUtilsTests",
            "Extract Method private assertMissingTextAttribute(attributes Map<String,Object>) : void extracted from public synthesizeAnnotationFromMapWithMissingAttributeValue() : void in class org.springframework.core.annotation.AnnotationUtilsTests");

          processTp("https://github.com/infinispan/infinispan.git", "03573a655bcbb77f7a76d8e22d851cc22796b4f8", 
            "Extract Method protected shouldInvoke(event Event<K,V>) : boolean extracted from protected shouldInvoke(event CacheEntryEvent<K,V>, isLocalNodePrimaryOwner boolean) : CacheEntryEvent<K,V> in class org.infinispan.notifications.cachelistener.CacheNotifierImpl.BaseCacheEntryListenerInvocation");


          processFp("https://github.com/infinispan/infinispan.git", "8228dfc611e83ed9c9840a009ea52ceae765548d", 
            "Extract Method protected shouldInvoke(event Event<K,V>) : boolean extracted from protected shouldInvoke(event CacheEntryEvent<K,V>, isLocalNodePrimaryOwner boolean) : CacheEntryEvent<K,V> in class org.infinispan.notifications.cachelistener.CacheNotifierImpl.BaseCacheEntryListenerInvocation");
          processTp("https://github.com/JetBrains/MPS.git", "797fb7fc1415ac0ebe9a8262677dfa4462ed6cb4", 
            "Extract Method private doAppendNode(node SNode) : void extracted from public appendNode(node SNode) : void in class jetbrains.mps.text.impl.TextGenSupport");

          processTp("https://github.com/wordpress-mobile/WordPress-Android.git", "4bfe164cc8b4556b98df18098b162e0a84038b32", 
            "Extract Method private trackLastVisibleTab(position int) : void extracted from protected onResume() : void in class org.wordpress.android.ui.main.WPMainActivity");

          processTp("https://github.com/apache/camel.git", "5e08a9e8e93a2f117b5fbec9c6d54500d8e99a4d", 
            "Extract Method public copyAttachments(that Message) : void extracted from public copyFrom(that Message) : void in class org.apache.camel.impl.MessageSupport");


          processFp("https://github.com/orientechnologies/orientdb.git", "3842d7440a60781d7c96a6cf1e395937afa3ea17", 
            "Inline Method private jj_3R_39() : boolean inlined to private jj_3R_405() : boolean in class com.orientechnologies.orient.core.sql.parser.OrientSql",
            "Inline Method private jj_3R_39() : boolean inlined to private jj_3R_238() : boolean in class com.orientechnologies.orient.core.sql.parser.OrientSql",
            "Inline Method private jj_3R_39() : boolean inlined to private jj_3R_332() : boolean in class com.orientechnologies.orient.core.sql.parser.OrientSql");

          processFp("https://github.com/kairosdb/kairosdb.git", "328f06da65ea4c0f9d0b1d315a96903c5dec2514", 
            "Move Class org.kairosdb.core.http.rest.json.JsonMetricParser.NewMetric moved to org.kairosdb.core.http.rest.json.DataPointsParser.NewMetric",
            "Move Class org.kairosdb.core.http.rest.json.JsonMetricParser.SubContext moved to org.kairosdb.core.http.rest.json.DataPointsParser.SubContext");

          processFp("https://github.com/VoltDB/voltdb.git", "4be519de41980ba3434aeec9fb90c92ecb712e6f", 
            "Extract Method private subTestScalarSubqueryWithNonIntegerType(client Client) : void extracted from public testSubSelects_Simple() : void in class org.voltdb.regressionsuites.TestSubQueriesSuite",
            "Extract Method private subTestScalarSubqueryWithParentOrderByOrGroupBy(client Client) : void extracted from public testSubSelects_Simple() : void in class org.voltdb.regressionsuites.TestSubQueriesSuite");

          processFp("https://github.com/structr/structr.git", "814d6fd466366c804f3edc179b91f6515413386f", 
            "Inline Method private getPropertiesForView(type Class, view String) : Map<String,Object> inlined to public doGet(sortKey PropertyKey, sortDescending boolean, pageSize int, page int, offsetId String) : Result in class org.structr.rest.resource.SchemaTypeResource");
          processTp("https://github.com/rstudio/rstudio.git", "4983f83d1bedb7b737fc56d409c1c06b04e34e4e", 
            "Extract Method private setValue(value boolean, force boolean) : void extracted from public setValue(value boolean) : void in class org.rstudio.core.client.widget.ThemedCheckBox");

          processTp("https://github.com/liferay/liferay-plugins.git", "7c7ecf4cffda166938efd0ae34830e2979c25c73", 
            "Extract Method protected updateSyncDLObject(syncDLObject SyncDLObject) : void extracted from public onAfterUpdate(resourcePermission ResourcePermission) : void in class com.liferay.sync.hook.listeners.ResourcePermissionModelListener");

          processTp("https://github.com/VoltDB/voltdb.git", "a896b8bf8f7067e41291eb7771deed76b3621fa0", 
            "Move Attribute private m_framework : Framework from class org.voltdb.importer.ImportProcessor.BundleWrapper to class org.voltdb.importer.ImportProcessor");

          processTp("https://github.com/mockito/mockito.git", "2d036ecf1d7170b4ec7346579a1ef8904109530a", 
            "Extract Method private allMockedTypes(features MockFeatures<T>) : Class<?>[] extracted from public generateMockClass(features MockFeatures<T>) : Class<? extends T> in class org.mockito.internal.creation.bytebuddy.MockBytecodeGenerator");

          processTp("https://github.com/apache/tomcat.git", "40f00732b9652350ac11830367fd32db67987fc7", 
            "Move Attribute private certificateKeystoreType : String from class org.apache.tomcat.util.net.SSLHostConfig to class org.apache.tomcat.util.net.SSLHostConfigCertificate",
            "Move Attribute private certificateKeystoreProvider : String from class org.apache.tomcat.util.net.SSLHostConfig to class org.apache.tomcat.util.net.SSLHostConfigCertificate",
            "Move Attribute private certificateKeystoreFile : String from class org.apache.tomcat.util.net.SSLHostConfig to class org.apache.tomcat.util.net.SSLHostConfigCertificate",
            "Move Attribute private certificateKeystorePassword : String from class org.apache.tomcat.util.net.SSLHostConfig to class org.apache.tomcat.util.net.SSLHostConfigCertificate",
            "Move Attribute private certificateKeyAlias : String from class org.apache.tomcat.util.net.SSLHostConfig to class org.apache.tomcat.util.net.SSLHostConfigCertificate",
            "Move Method public getCertificateKeystorePassword() : String from class org.apache.tomcat.util.net.SSLHostConfig to public getCertificateKeystorePassword() : String from class org.apache.tomcat.util.net.SSLHostConfigCertificate",
            "Move Method public getCertificateKeystoreFile() : String from class org.apache.tomcat.util.net.SSLHostConfig to public getCertificateKeystoreFile() : String from class org.apache.tomcat.util.net.SSLHostConfigCertificate",
            "Move Method public getCertificateKeyAlias() : String from class org.apache.tomcat.util.net.SSLHostConfig to public getCertificateKeyAlias() : String from class org.apache.tomcat.util.net.SSLHostConfigCertificate");
          processFp("https://github.com/apache/tomcat.git", "40f00732b9652350ac11830367fd32db67987fc7", 
            "Inline Method public getCertificateKeystoreProvider() : String inlined to public getTruststoreProvider() : String in class org.apache.tomcat.util.net.SSLHostConfig");

          processFp("https://github.com/facebook/buck.git", "6a1e0c38740ab80edd9f88db28b6b06eca5b22f4", 
            "Extract Method private getFinalWorkingDirectory(context ExecutionContext) : File extracted from public getDescription(context ExecutionContext) : String in class com.facebook.buck.shell.ShellStep");
          processTp("https://github.com/facebook/buck.git", "7e104c3ed4b80ec8e9b72356396f879d1067cc40", 
            "Extract Method private createBuckFiles(buckFilesData Map<Path,SortedSet<Prebuilt>>) : void extracted from public resolve(mavenCoords String[]) : void in class com.facebook.buck.maven.Resolver",
            "Extract Method private downloadArtifact(artifactToDownload Artifact, repoSys RepositorySystem, session RepositorySystemSession, buckFiles Map<Path,SortedSet<Prebuilt>>, graph MutableDirectedGraph<Artifact>) : void extracted from public resolve(mavenCoords String[]) : void in class com.facebook.buck.maven.Resolver");

          processTp("https://github.com/processing/processing.git", "acf67c8cb58d13827e14bbeeec11a66f9277015f", 
            "Inline Method protected runSketchEDT(args String[], constructedSketch PApplet) : void inlined to public runSketch(args String[], constructedSketch PApplet) : void in class processing.core.PApplet");

          processTp("https://github.com/apache/hive.git", "e2dd54ab180b577b08cf6b0e69310ac81fc99fd3", 
            "Extract Method private foldExprFull(desc ExprNodeDesc, constants Map<ColumnInfo,ExprNodeDesc>, cppCtx ConstantPropagateProcCtx, op Operator<? extends Serializable>, tag int, propagate boolean) : ExprNodeDesc extracted from private foldExpr(desc ExprNodeDesc, constants Map<ColumnInfo,ExprNodeDesc>, cppCtx ConstantPropagateProcCtx, op Operator<? extends Serializable>, tag int, propagate boolean) : ExprNodeDesc in class org.apache.hadoop.hive.ql.optimizer.ConstantPropagateProcFactory");
          processFp("https://github.com/apache/hive.git", "e2dd54ab180b577b08cf6b0e69310ac81fc99fd3", 
            "Extract Method private foldExprShortcut(desc ExprNodeDesc, constants Map<ColumnInfo,ExprNodeDesc>, cppCtx ConstantPropagateProcCtx, op Operator<? extends Serializable>, tag int, propagate boolean) : ExprNodeDesc extracted from private foldExpr(desc ExprNodeDesc, constants Map<ColumnInfo,ExprNodeDesc>, cppCtx ConstantPropagateProcCtx, op Operator<? extends Serializable>, tag int, propagate boolean) : ExprNodeDesc in class org.apache.hadoop.hive.ql.optimizer.ConstantPropagateProcFactory");
          processTp("https://github.com/opentripplanner/OpenTripPlanner.git", "e32f161fc023d1ee153c49df312ae10b06941465", 
            "Move Class org.opentripplanner.analyst.qbroker.User moved to org.opentripplanner.analyst.broker.User",
            "Move Class org.opentripplanner.analyst.qbroker.QueueType moved to org.opentripplanner.analyst.broker.QueueType",
            "Move Class org.opentripplanner.analyst.qbroker.QueuePath moved to org.opentripplanner.analyst.broker.QueuePath",
            "Move Class org.opentripplanner.analyst.qbroker.CircularList moved to org.opentripplanner.analyst.broker.CircularList",
            "Move Class org.opentripplanner.analyst.qbroker.BrokerMain moved to org.opentripplanner.analyst.broker.BrokerMain",
            "Move Class org.opentripplanner.analyst.qbroker.BrokerHttpHandler moved to org.opentripplanner.analyst.broker.BrokerHttpHandler");

          processTp("https://github.com/ratpack/ratpack.git", "da6167af3bdbf7663af6c20fb603aba27dd5e174", 
            "Extract Method private post(responseStatus HttpResponseStatus, lastContentFuture ChannelFuture) : void extracted from private post(responseStatus HttpResponseStatus) : void in class ratpack.server.internal.DefaultResponseTransmitter");

          processTp("https://github.com/brettwooldridge/HikariCP.git", "cd8c4d578a609bdd6395d3a8c49bfd19ed700dea", 
            "Move Class com.zaxxer.hikari.util.NanosecondClockSource moved to com.zaxxer.hikari.util.ClockSource.NanosecondClockSource",
            "Move Class com.zaxxer.hikari.util.MillisecondClockSource moved to com.zaxxer.hikari.util.ClockSource.MillisecondClockSource");

          processTp("https://github.com/brettwooldridge/HikariCP.git", "1571049ec04b1e7e6f082ed5ec071584e7200c12", 
            "Move Class com.zaxxer.hikari.util.IConcurrentBagEntry moved to com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry",
            "Move Class com.zaxxer.hikari.util.IBagStateListener moved to com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener");

          processTp("https://github.com/scobal/seyren.git", "5fb36a321af7df470d4c845cb18da8f85be31c38", 
            "Extract Method private evaluateTemplate(check Check, subscription Subscription, alerts List<Alert>, templateContent String) : String extracted from public createBody(check Check, subscription Subscription, alerts List<Alert>) : String in class com.seyren.core.util.velocity.VelocityEmailHelper");

          processTp("https://github.com/SlimeKnights/TinkersConstruct.git", "71820e573134be3fad3935035249cd77c4412f4e", 
            "Move Class tconstruct.library.modifiers.RecipeMatch moved to tconstruct.library.mantle.RecipeMatch");


          processFp("https://github.com/JetBrains/intellij-community.git", "e3993ff8c50ef4459389326bca048fd1ed3a73a4", 
            "Inline Method private calcEffectivePlatformCp(platformCp Collection<File>, chunkSdkVersion int, options List<String>, compilingTool JavaCompilingTool) : Collection<File> inlined to private compileJava(context CompileContext, chunk ModuleChunk, files Collection<File>, classpath Collection<File>, platformCp Collection<File>, sourcePath Collection<File>, diagnosticSink DiagnosticOutputConsumer, outputSink OutputFileConsumer, compilingTool JavaCompilingTool) : boolean in class org.jetbrains.jps.incremental.java.JavaBuilder");
          processTp("https://github.com/open-keychain/open-keychain.git", "de50b3becb31c367f867382ff9cd898ba1628350", 
            "Extract Method public isOrbotInRequiredState(middleButton int, middleButtonRunnable Runnable, fragmentActivity FragmentActivity) : boolean extracted from public isOrbotInRequiredState(middleButton int, middleButtonRunnable Runnable, proxyPrefs ProxyPrefs, fragmentActivity FragmentActivity) : boolean in class org.sufficientlysecure.keychain.util.orbot.OrbotHelper");

          processTp("https://github.com/osmandapp/Osmand.git", "c45b9e6615181b7d8f4d7b5b1cc141169081c02c", 
            "Extract Method private addPreviousToActionPoints(lastProjection Location, routeNodes List<Location>, DISTANCE_ACTION double, prevFinishPoint int, routePoint int, loc Location) : void extracted from private calculateActionPoints(topLatitude double, leftLongitude double, bottomLatitude double, rightLongitude double, lastProjection Location, routeNodes List<Location>, cd int, it Iterator<RouteDirectionInfo>, zoom int) : void in class net.osmand.plus.views.RouteLayer");

          processTp("https://github.com/apache/drill.git", "ffae1691c0cd526ed1095fbabbc0855d016790d7", 
            "Extract Method protected convertToDrel(relNode RelNode) : DrillRel extracted from protected convertToDrel(relNode RelNode, validatedRowType RelDataType) : DrillRel in class org.apache.drill.exec.planner.sql.handlers.DefaultSqlHandler",
            "Extract Method protected validateAndConvert(sqlNode SqlNode) : ConvertedRelNode extracted from public getPlan(sqlNode SqlNode) : PhysicalPlan in class org.apache.drill.exec.planner.sql.handlers.DefaultSqlHandler");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "5d1a70a4d32ac4c96a32535c68c69b20288d8968", 
            "Extract Method public killProcessGroup(uid int, pid int) : void extracted from private crashApplication(r ProcessRecord, crashInfo CrashInfo) : void in class com.android.server.am.ActivityManagerService");

          processTp("https://github.com/codefollower/Lealone.git", "7a2e0ae5f6172cbe34f9bc4a5cde666314ff75dd", 
            "Extract Method package setPassword(user User, session Session, password Expression) : void extracted from public update() : int in class org.lealone.command.ddl.CreateUser",
            "Extract Method package setSaltAndHash(user User, session Session, salt Expression, hash Expression) : void extracted from public update() : int in class org.lealone.command.ddl.CreateUser");

          processTp("https://github.com/joel-costigliola/assertj-core.git", "b36ab386559d04db114db8edd87c8d4cbf850c12", 
            "Extract Superclass org.assertj.core.api.StrictAssertions from classes [org.assertj.core.api.Assertions]");


          processFp("https://github.com/phishman3579/java-algorithms-implementation.git", "171019f2d04bab1dc9676997cd193c7c78306ccf", 
            "Extract Method private testMapEntrySet(map Map<K,V>, keyType Class<T>, data Integer[]) : boolean extracted from private addInOrderAndRemoveInOrder(map Map<K,V>, keyType Type, name String, data T[], invalid T) : boolean in class com.jwetherell.algorithms.data_structures.test.common.JavaMapTest");
          processTp("https://github.com/languagetool-org/languagetool.git", "bec15926deb49d2b3f7b979d4cfc819947a434ec", 
            "Move Attribute public VIDMINKY_MAP : Map<String,String> from class org.languagetool.tagging.uk.UkrainianTagger to class org.languagetool.tagging.uk.PosTagHelper");
          processFp("https://github.com/languagetool-org/languagetool.git", "bec15926deb49d2b3f7b979d4cfc819947a434ec", 
            "Inline Method private hasRequiredPosTag(posTagsToFind Collection<String>, tokenReadings AnalyzedTokenReadings) : boolean inlined to public match(text AnalyzedSentence) : RuleMatch[] in class org.languagetool.rules.uk.TokenAgreementRule");
          processTp("https://github.com/phishman3579/java-algorithms-implementation.git", "4ffcb5a65e6d24c58ef75a5cd7692e875619548d", 
            "Extract Method private collectGarbage() : void extracted from public main(args String[]) : void in class com.jwetherell.algorithms.sorts.timing.SortsTiming");

          processTp("https://github.com/phishman3579/java-algorithms-implementation.git", "f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29", 
            "Extract Method private putOutTheGarbage() : void extracted from public main(args String[]) : void in class com.jwetherell.algorithms.sorts.timing.SortsTiming");

          processTp("https://github.com/droolsjbpm/drools.git", "c8e09e2056c54ead97bce4386a25b222154223b1", 
            "Extract Method public instantiateObject(className String, args Object[]) : Object extracted from public instantiateObject(className String) : Object in class org.drools.core.util.ClassUtils",
            "Extract Method public loadClass(className String, classLoader ClassLoader) : Class<?> extracted from public instantiateObject(className String, classLoader ClassLoader) : Object in class org.drools.core.util.ClassUtils",
            "Extract Interface org.drools.core.util.ByteArrayClassLoader from classes [org.drools.core.base.ClassFieldAccessorCache.DefaultByteArrayClassLoader]");

          processTp("https://github.com/querydsl/querydsl.git", "09b9d989658ef5bf9333c081c92b57a7611ad207", 
            "Extract Superclass com.querydsl.sql.types.AbstractJSR310DateTimeTypeTest from classes [com.querydsl.sql.types.JSR310InstantTypeTest, com.querydsl.sql.types.JSR310LocalDateTimeTypeTest, com.querydsl.sql.types.JSR310LocalDateTypeTest, com.querydsl.sql.types.JSR310LocalTimeTypeTest, com.querydsl.sql.types.JSR310OffsetDateTimeTypeTest, com.querydsl.sql.types.JSR310OffsetTimeTypeTest, com.querydsl.sql.types.JSR310ZonedDateTimeTypeTest]");

          processTp("https://github.com/spring-projects/spring-framework.git", "dd4bc630c3de70204081ab196945d6b55ab03beb", 
            "Move Class org.springframework.aop.interceptor.AsyncExecutionInterceptor.CompletableFutureDelegate moved to org.springframework.aop.interceptor.AsyncExecutionAspectSupport.CompletableFutureDelegate",
            "Pull Up Attribute private completableFuturePresent : boolean from class org.springframework.aop.interceptor.AsyncExecutionInterceptor to class org.springframework.aop.interceptor.AsyncExecutionAspectSupport");

          processTp("https://github.com/k9mail/k-9.git", "9d44f0e06232661259681d64002dd53c7c43099d", 
            "Extract Method private handleSendFailure(account Account, localStore Store, localFolder Folder, message Message, exception Exception, permanentFailure boolean) : void extracted from public sendPendingMessagesSynchronous(account Account) : void in class com.fsck.k9.controller.MessagingController");
          processFp("https://github.com/k9mail/k-9.git", "9d44f0e06232661259681d64002dd53c7c43099d", 
            "Extract Method private notifySynchronizeMailboxFailed(account Account, localFolder Folder, exception Exception) : void extracted from public sendPendingMessagesSynchronous(account Account) : void in class com.fsck.k9.controller.MessagingController");
          processTp("https://github.com/languagetool-org/languagetool.git", "01cddc5afb590b4d36cb784637a8ea8aa31d3561", 
            "Extract Method private getMotherTonguePanel(cons GridBagConstraints) : JPanel extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
            "Extract Method private getTreeButtonPanel() : JPanel extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
            "Extract Method private getMouseAdapter() : MouseAdapter extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
            "Extract Method private getTreeModel(rootNode DefaultMutableTreeNode) : DefaultTreeModel extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog",
            "Extract Method private createNonOfficeElements(cons GridBagConstraints, portPanel JPanel) : void extracted from public show(rules List<Rule>) : void in class org.languagetool.gui.ConfigurationDialog");

          processTp("https://github.com/neo4j/neo4j.git", "74d2cc420e5590ba3bc0ffcc15b30b76a9cbef0b", 
            "Extract Method private availability() : Availability extracted from private availability(millis long) : Availability in class org.neo4j.kernel.AvailabilityGuard");

          processTp("https://github.com/wicketstuff/core.git", "8ea46f48063c38473c12ca7c114106ca910b6e74", 
            "Extract Method private testRenderedTab() : void extracted from public renderSimpleTab() : void in class org.wicketstuff.foundation.tab.FoundationTabTest");


          processFp("https://github.com/checkstyle/checkstyle.git", "8a49fd50ee5dbadc22b7d557b9ef8a6e212b0f78", 
            "Move Class InputRedundantImportCheck_UnnamedPackage moved to InputRedundantImportCheck_UnnamedPackage");
          processTp("https://github.com/mongodb/morphia.git", "70a25d4afdc435e9cad4460b2a20b7aabdd21e35", 
            "Extract Method private performBasicMappingTest() : void extracted from public testBasicMapping() : void in class org.mongodb.morphia.TestMapping");


          processFp("https://github.com/orientechnologies/orientdb.git", "df627b33960e37bfbf837d438d32748f0110c0f7", 
            "Extract Method public QueryStatement() : OStatement extracted from public Statement() : OStatement in class com.orientechnologies.orient.core.sql.parser.OrientSql");
          processTp("https://github.com/spring-projects/spring-framework.git", "31a5434ea433bdec2283797bf9415c02bb2f41c1", 
            "Extract Method protected addDefaultHeaders(headers HttpHeaders, t T, contentType MediaType) : void extracted from public write(t T, contentType MediaType, outputMessage HttpOutputMessage) : void in class org.springframework.http.converter.AbstractHttpMessageConverter");

          processTp("https://github.com/Graylog2/graylog2-server.git", "72acc2126611f0bff9b672de18b9b2f8dacdc03a", 
            "Extract Interface org.graylog2.bootstrap.CliCommand from classes [org.graylog2.bootstrap.CmdLineTool, org.graylog2.bootstrap.commands.ShowVersion]",
            "Move Class org.graylog2.UI moved to org.graylog2.shared.UI",
            "Move Class org.graylog2.bootstrap.commands.journal.JournalTruncate moved to org.graylog2.commands.journal.JournalTruncate",
            "Move Class org.graylog2.bootstrap.commands.journal.JournalShow moved to org.graylog2.commands.journal.JournalShow",
            "Move Class org.graylog2.bootstrap.commands.journal.JournalDecode moved to org.graylog2.commands.journal.JournalDecode",
            "Move Class org.graylog2.bootstrap.commands.journal.AbstractJournalCommand moved to org.graylog2.commands.journal.AbstractJournalCommand",
            "Move Class org.graylog2.bootstrap.commands.Server moved to org.graylog2.commands.Server",
            "Move Class org.graylog2.bootstrap.commands.Radio moved to org.graylog2.radio.commands.Radio");

          processTp("https://github.com/neo4j/neo4j.git", "001de307492df8f84ad15f6aaa0bd1e748d4ce27", 
            "Move Class org.neo4j.kernel.Recovery moved to org.neo4j.kernel.recovery.Recovery");


          processFp("https://github.com/dropwizard/metrics.git", "9ce7346cc0d243d29619ae4b867ee8e694844f8d", 
            "Move Class io.dropwizard.metrics.influxdb.utils.InfluxDbWriteObjectSerializerTest moved to io.dropwizard.metrics.influxdb.utils.InfluxDbWriteObjectSerializerTest",
            "Move Class io.dropwizard.metrics.influxdb.InfluxDbReporterTest moved to io.dropwizard.metrics.influxdb.InfluxDbReporterTest");

          processFp("https://github.com/google/guava.git", "6640609a10c02ad8708cff784417a47a9b4006a4", 
            "Inline Method public subMap(fromKey K, fromInclusive boolean, toKey K, toInclusive boolean) : ImmutableSortedMap<K,V> inlined to public subMap(fromKey K, toKey K) : ImmutableSortedMap<K,V> in class com.google.common.collect.ImmutableSortedMap",
            "Inline Method private copyOfEnumMap(original EnumMap<K,? extends V>) : ImmutableMap<K,V> inlined to public copyOf(map Map<? extends K,? extends V>) : ImmutableMap<K,V> in class com.google.common.collect.ImmutableMap",
            "Extract Method package fromEntryList(entries Collection<? extends Entry<? extends K,? extends V>>) : ImmutableMap<K,V> extracted from public copyOf(entries Iterable<? extends Entry<? extends K,? extends V>>) : ImmutableMap<K,V> in class com.google.common.collect.ImmutableMap");
          processTp("https://github.com/wordpress-mobile/WordPress-Android.git", "3b95d10985776fb7b710089ff71074fd2bf860ee", 
            "Extract Method private getBlogsForCurrentView() : List<Map<String,Object>> extracted from protected doInBackground(params Void[]) : SiteList in class org.wordpress.android.ui.main.SitePickerAdapter.LoadSitesTask");

          processTp("https://github.com/neo4j/neo4j.git", "77fab3caea4495798a248035f0e928f745c7c2db", 
            "Inline Method public releaseAllExclusive() : void inlined to public releaseAll() : void in class org.neo4j.kernel.impl.locking.community.CommunityLockClient",
            "Inline Method public releaseAllShared() : void inlined to public releaseAll() : void in class org.neo4j.kernel.impl.locking.community.CommunityLockClient");
          processFp("https://github.com/neo4j/neo4j.git", "77fab3caea4495798a248035f0e928f745c7c2db", 
            "Inline Method private incrementAndRemoveAlreadyTakenLocks(takenLocks Map<Long,AtomicInteger>, resourceIds long[]) : long[] inlined to public acquireExclusive(resourceType ResourceType, resourceId long) : void in class org.neo4j.kernel.ha.lock.SlaveLocksClient",
            "Inline Method private incrementAndRemoveAlreadyTakenLocks(takenLocks Map<Long,AtomicInteger>, resourceIds long[]) : long[] inlined to public acquireShared(resourceType ResourceType, resourceId long) : void in class org.neo4j.kernel.ha.lock.SlaveLocksClient");
          processTp("https://github.com/mrniko/redisson.git", "186357ac6c2da1a5a12c0287a08408ac5ec6683b", 
            "Extract Method public createClient(host String, port int, timeout int) : RedisClient extracted from public createClient(host String, port int) : RedisClient in class org.redisson.connection.MasterSlaveConnectionManager");

          processTp("https://github.com/VoltDB/voltdb.git", "ebb1c2c364e888d4a0f47abe691cb2bad4eb4e75", 
            "Extract Method private isIndexOptimalForMinMax(matchingCase MatViewIndexMacthingGroupby, singleUniqueMinMaxAggExpr AbstractExpression, indexedColRefs List<ColumnRef>, indexedExprs List<AbstractExpression>, srcColumnArray List<Column>) : boolean extracted from private findBestMatchIndexForMatviewMinOrMax(matviewinfo MaterializedViewInfo, srcTable Table, groupbyExprs List<AbstractExpression>, singleUniqueMinMaxAggExpr AbstractExpression) : Index in class org.voltdb.compiler.DDLCompiler",
            "Extract Method private isGroupbyMatchingIndex(matchingCase MatViewIndexMacthingGroupby, groupbyColRefs List<ColumnRef>, groupbyExprs List<AbstractExpression>, indexedColRefs List<ColumnRef>, indexedExprs List<AbstractExpression>, srcColumnArray List<Column>) : boolean extracted from private findBestMatchIndexForMatviewMinOrMax(matviewinfo MaterializedViewInfo, srcTable Table, groupbyExprs List<AbstractExpression>, singleUniqueMinMaxAggExpr AbstractExpression) : Index in class org.voltdb.compiler.DDLCompiler");

          processTp("https://github.com/hazelcast/hazelcast.git", "4d05a3b1168441216dcaea8282c39338285182af", 
            "Extract Superclass com.hazelcast.spi.impl.SimpleExecutionCallback from classes [com.hazelcast.cache.impl.operation.CacheCreateConfigOperation.CacheConfigCreateCallback, com.hazelcast.client.impl.client.MultiTargetClientRequest.SingleTargetCallback, com.hazelcast.client.impl.protocol.task.AbstractMultiTargetMessageTask.SingleTargetCallback, com.hazelcast.partition.impl.MigrationRequestOperation.MigrationCallback]",
            "Move Class com.hazelcast.spi.impl.operationservice.impl.InvocationFuture.ExecutorCallbackAdapter moved to com.hazelcast.spi.InvocationBuilder.ExecutorCallbackAdapter");


          processFp("https://github.com/CyanogenMod/android_packages_apps_Trebuchet.git", "cae95210719c7d520afcf1b5c6f088d776c974d2", 
            // strange bug: Inline Method private checkItemPlacement(occupied HashMap<Long,ItemInfo[][], item ItemInfo, deleteOnInvalidPlacement AtomicBoolean) : boolean inlined to private loadWorkspace() : boolean in class com.android.launcher3.LauncherModel.LoaderTask
              "Inline Method private checkItemPlacement(occupied HashMap, item ItemInfo, deleteOnInvalidPlacement AtomicBoolean) : boolean inlined to private loadWorkspace() : boolean in class com.android.launcher3.LauncherModel.LoaderTask");
          processTp("https://github.com/openhab/openhab.git", "a9b1e5d67421ed98b49ae25c3bbd6e27a0ab1590", 
            "Extract Method private bail(txt String) : void extracted from public processData() : Msg in class org.openhab.binding.insteonplm.internal.message.MsgFactory",
            "Extract Method private processBindingConfiguration() : void extracted from public updated(config Dictionary<String,?>) : void in class org.openhab.binding.insteonplm.InsteonPLMActiveBinding");


          processFp("https://github.com/apache/hive.git", "1ff7f9efc6f95c079f82ce50e32706040977783f", 
            "Extract Method public executeLDAPQuery(ctx DirContext, query String, rootDN String) : List<String> extracted from public Authenticate(user String, password String) : void in class org.apache.hive.service.auth.LdapAuthenticationProviderImpl");

          processFp("https://github.com/apache/hive.git", "9f76caeb0f254a7d3a09131e4ff3028a203a321a", 
            "Extract Method public executeLDAPQuery(ctx DirContext, query String, rootDN String) : List<String> extracted from public Authenticate(user String, password String) : void in class org.apache.hive.service.auth.LdapAuthenticationProviderImpl");
          processTp("https://github.com/apache/drill.git", "711992f22ae6d6dfc43bdb4c01bf8f921d175b38", 
            "Extract Method private nextRowInternally() : boolean extracted from public next() : boolean in class org.apache.drill.jdbc.impl.DrillCursor");


          processFp("https://github.com/eucalyptus/eucalyptus.git", "6a666a178b59856bd851c7856840990e5e135626", 
            "Inline Method private getPolicyDescription(policyName String, policyTypeName String, pathToAttributeJson String) : PolicyDescription inlined to private getSamplePolicyDescription42() : List<PolicyDescription> in class com.eucalyptus.loadbalancing.LoadBalancerPolicies");
          processTp("https://github.com/mongodb/mongo-java-driver.git", "8c5a20d786e66ee4c4b0d743f0f80bf681c419be", 
            "Move Class com.mongodb.JsonPoweredTestHelper moved to util.JsonPoweredTestHelper");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "f166866cd68efa963534c5bc7fc9ca38e4aa2838", 
            "Inline Method public is24HourFormatLocale(context Context) : boolean inlined to public is24HourFormat(context Context, userHandle int) : boolean in class android.text.format.DateFormat");


          processFp("https://github.com/gabrielemariotti/changeloglib.git", "9f812d9b9a55016399f47b50a88fbec2d5235d4f", 
            "Inline Method private setupNavDrawer() : void inlined to public onCreate(savedInstanceState Bundle) : void in class it.gmariotti.changelog.demo.MainActivity");

          processFp("https://github.com/google/closure-compiler.git", "8e198470aaf696652e0173d76cbc388f83c3a994", 
            "Inline Method private copyStaticMember(staticMember Node, superclassNameNode Node, subclassNameNode Node, insertionPoint Node) : void inlined to public visit(t NodeTraversal, n Node, parent Node) : void in class com.google.javascript.jscomp.Es6ToEs3ClassSideInheritance",
            "Extract Method private copyStaticMethod(staticMember Node, superclassNameNode Node, subclassNameNode Node, insertionPoint Node) : void extracted from public visit(t NodeTraversal, n Node, parent Node) : void in class com.google.javascript.jscomp.Es6ToEs3ClassSideInheritance");

          processFp("https://github.com/amplab/tachyon.git", "58c7aca35377424fe933fe64f69a7cabac6826bd", 
            "Move Class tachyon.underfs.UnderFileSystemCluster moved to tachyon.underfs.UnderFileSystemCluster",
            "Move Class tachyon.master.LocalTachyonMaster moved to tachyon.master.LocalTachyonMaster",
            "Move Class tachyon.master.LocalTachyonClusterMultiMaster moved to tachyon.master.LocalTachyonClusterMultiMaster",
            "Move Class tachyon.master.LocalTachyonCluster moved to tachyon.master.LocalTachyonCluster",
            "Move Class tachyon.master.ClientPool moved to tachyon.master.ClientPool",
            "Move Class tachyon.LocalFilesystemCluster moved to tachyon.LocalFilesystemCluster");
          processTp("https://github.com/facebook/presto.git", "b7f4914d81a7a618acf2eba52af1093fc23cfba9", 
            "Extract Method private tryGetLookupSource() : void extracted from public getOutput() : Page in class com.facebook.presto.operator.LookupJoinOperator",
            "Extract Method private tryGetLookupSource() : void extracted from public needsInput() : boolean in class com.facebook.presto.operator.LookupJoinOperator");


          processFp("https://github.com/eucalyptus/eucalyptus.git", "8a0ce146b718f904568d7c8e6eb4a4865bfe8289", 
            "Extract Method private matchResources(auth Authorization, region String, resourceAccountNumber String, resourceType String, resource String) : boolean extracted from private matchResources(auth Authorization, resourceAccountNumber String, resource String) : boolean in class com.eucalyptus.auth.policy.PolicyEngineImpl");
          processTp("https://github.com/spring-projects/spring-framework.git", "e083683f4fe9206609201bb39a60bbd8ee0c8a0f", 
            "Extract Superclass org.springframework.web.socket.server.support.AbstractHandshakeHandler from classes [org.springframework.web.socket.server.support.DefaultHandshakeHandler]");


          processFp("https://github.com/amplab/tachyon.git", "a9db8681431028a7e057db93d936294b0cdcb41c", 
            "Move Class tachyon.underfs.UnderFileSystemCluster moved to tachyon.underfs.UnderFileSystemCluster",
            "Move Class tachyon.LocalFilesystemCluster moved to tachyon.LocalFilesystemCluster",
            "Move Class tachyon.master.LocalTachyonMaster moved to tachyon.master.LocalTachyonMaster",
            "Move Class tachyon.master.LocalTachyonClusterMultiMaster moved to tachyon.master.LocalTachyonClusterMultiMaster",
            "Move Class tachyon.master.LocalTachyonCluster moved to tachyon.master.LocalTachyonCluster",
            "Move Class tachyon.master.ClientPool moved to tachyon.master.ClientPool");
          processTp("https://github.com/spring-projects/spring-boot.git", "becced5f0b7bac8200df7a5706b568687b517b90", 
            "Extract Method private createPreparedEvent(propName String, propValue String) : SpringApplicationEvent extracted from public overridePidFileWithSpring() : void in class org.springframework.boot.actuate.system.ApplicationPidFileWriterTests",
            "Extract Method private createEnvironmentPreparedEvent(propName String, propValue String) : SpringApplicationEvent extracted from public differentEventTypes() : void in class org.springframework.boot.actuate.system.ApplicationPidFileWriterTests");

          processTp("https://github.com/JetBrains/intellij-community.git", "6540dde58190f642e59ca10516f84eb85f855373", 
            "Move Method public averageAmongMedians(time long[], part int) : long from class com.intellij.testFramework.PlatformTestUtil to public averageAmongMedians(time long[], part int) : long from class com.intellij.util.ArrayUtil");

          processTp("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "0b93231025f51c7ec62fd8588985c5dc807854e4", 
            "Extract Method protected doSomething(virtualFile VirtualFile, module Module, project Project, title String, withProgress boolean) : boolean extracted from protected doSomething(virtualFile VirtualFile, module Module, project Project, title String) : boolean in class com.goide.actions.fmt.GoExternalToolsAction");

          processTp("https://github.com/apache/cassandra.git", "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", 
            "Extract Method private generateFakeEndpoints(tmd TokenMetadata, numOldNodes int, numVNodes int) : void extracted from private generateFakeEndpoints(numOldNodes int) : void in class org.apache.cassandra.dht.BootStrapperTest",
            "Extract Method public getRandomToken(r Random) : LongToken extracted from public getRandomToken() : LongToken in class org.apache.cassandra.dht.Murmur3Partitioner",
            "Extract Method private getSpecifiedTokens(metadata TokenMetadata, initialTokens Collection<String>) : Collection<Token> extracted from public getBootstrapTokens(metadata TokenMetadata) : Collection<Token> in class org.apache.cassandra.dht.BootStrapper");

          processTp("https://github.com/infinispan/infinispan.git", "4184c577f4bbc57f3ac13639557cfd99cdaca3e7", 
            "Move Attribute private stopped : boolean from class org.infinispan.persistence.async.State to class org.infinispan.persistence.async.AsyncCacheWriter");


          processFp("https://github.com/hazelcast/hazelcast.git", "19913abb1c18466955c06b25a902a3bfe5d26317", 
            "Extract Method private readFromCookie() : HazelcastHttpSession extracted from private readSessionFromLocal() : HazelcastHttpSession in class com.hazelcast.web.WebFilter.RequestWrapper");
          processTp("https://github.com/mongodb/morphia.git", "5db323b99f7064af8780f2c35f245461cf55cc8e", 
            "Extract Method private performBasicMappingTest() : void extracted from public testBasicMapping() : void in class org.mongodb.morphia.TestMapping");

          processTp("https://github.com/crate/crate.git", "5373a852a7e45715e0a6771b7cd56592994c07dd", 
            "Push Down Method public plan() : Plan from class io.crate.planner.node.dql.ESDQLPlanNode to public plan() : Plan from class io.crate.planner.node.dql.ESGetNode");


          processFp("https://github.com/hazelcast/hazelcast.git", "57e44159d9246d8de59b1d6d0776b26cbe844c71", 
            "Extract Method private readFromCookie() : HazelcastHttpSession extracted from private readSessionFromLocal() : HazelcastHttpSession in class com.hazelcast.web.WebFilter.RequestWrapper");
          processTp("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "b8929ccb4057c74ac64679216487a4abcd3ae1c3", 
            "Extract Method protected isAvailableInModule(module Module) : boolean extracted from protected setupConfigurationFromContext(configuration GoTestRunConfigurationBase, context ConfigurationContext, sourceElement Ref) : boolean in class com.goide.runconfig.testing.GoTestRunConfigurationProducerBase",
            "Move Attribute private GO_CHECK_GITHUB_IMPORT_PATH : Pattern from class com.goide.runconfig.testing.frameworks.gocheck.GocheckRunConfiguration to class com.goide.runconfig.GoRunUtil",
            "Move Attribute private GO_CHECK_IMPORT_PATH : Pattern from class com.goide.runconfig.testing.frameworks.gocheck.GocheckRunConfiguration to class com.goide.runconfig.GoRunUtil");


          processFp("https://github.com/addthis/hydra.git", "57de5a29793ce828583bfa22fb04d706aa3c0857", 
            "Move Class com.addthis.hydra.task.source.TaskDataSource moved to com.addthis.hydra.task.source.TaskDataSource",
            "Move Class com.addthis.hydra.task.output.WritableRootPaths moved to com.addthis.hydra.task.output.WritableRootPaths",
            "Move Class com.addthis.hydra.job.store.SpawnDataStoreKeys moved to com.addthis.hydra.job.store.SpawnDataStoreKeys",
            "Move Class com.addthis.hydra.job.store.SpawnDataStore moved to com.addthis.hydra.job.store.SpawnDataStore",
            "Move Class com.addthis.hydra.job.JobConfigExpander moved to com.addthis.hydra.job.JobConfigExpander");
          processTp("https://github.com/WhisperSystems/TextSecure.git", "99528dcc3b4a82b5e52a87d3e7aed5c6479028c7", 
            "Inline Method private getSynchronousRecipient(context Context, recipientId long) : Recipient inlined to package getRecipient(context Context, recipientId long, asynchronous boolean) : Recipient in class org.thoughtcrime.securesms.recipients.RecipientProvider",
            "Move Class org.thoughtcrime.securesms.contacts.ContactPhotoFactory.ExpandingLayerDrawable moved to org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto.ExpandingLayerDrawable");

          processTp("https://github.com/neo4j/neo4j.git", "d3533c1a0716ca114d294b3ea183504c9725698f", 
            "Extract Method private createNewThread(group Group, job Runnable, metadata Map<String,String>) : Thread extracted from public schedule(group Group, job Runnable, metadata Map<String,String>) : JobHandle in class org.neo4j.kernel.impl.util.Neo4jJobScheduler");

          processTp("https://github.com/springfox/springfox.git", "e70b04810eb1e73f60e3d8b3980b8271ce473565", 
            "Move Class springfox.documentation.schema.property.provider.ModelPropertiesProvider moved to springfox.documentation.schema.property.ModelPropertiesProvider",
            "Move Class springfox.documentation.schema.property.provider.DefaultModelPropertiesProvider moved to springfox.documentation.schema.property.DefaultModelPropertiesProvider");

          processTp("https://github.com/apache/hive.git", "abe6cd5d4614eb2ae3a78d85196f4d786d5886bd", 
            "Move Attribute private uniqueCounter : int from class org.apache.hadoop.hive.ql.optimizer.calcite.translator.HiveOpConverterPostProc to class org.apache.hadoop.hive.ql.optimizer.calcite.translator.HiveOpConverter");

          processTp("https://github.com/apache/drill.git", "8815eb7d947be6d2a0281c15a3a60d8ba040db95", 
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.store.parquet.ParquetReaderUtility",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.planner.sql.handlers.SqlHandlerUtil",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.planner.sql.handlers.ShowTablesHandler",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.planner.sql.handlers.DescribeTableHandler",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.planner.sql.handlers.CreateTableHandler",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.planner.sql.SchemaUtilites",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.planner.sql.DrillSqlWorker",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.expr.fn.impl.AggregateErrorFunctions",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.client.PrintingResultsListener",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.exec.vector.complex.impl.$",
            "Move Attribute private logger : Logger from class org.apache.drill.common.exceptions.UserException to class org.apache.drill.common.exceptions.TestUserException");

          processTp("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "3d5e343df6a39ce3b41624b90974d83e9899541e", 
            "Extract Method public processResolveVariants(processor GoScopeProcessor) : void extracted from public resolveInner() : PsiElement in class com.goide.psi.impl.GoVarReference");


          processFp("https://github.com/spring-projects/spring-integration.git", "f3d525a5e81e6794789967f77e887cf91315ce32", 
            "Extract Superclass org.springframework.integration.store.AbstractBatchingMessageGroupStore from classes [org.springframework.integration.store.AbstractMessageGroupStore, org.springframework.integration.mongodb.store.AbstractConfigurableMongoDbMessageStore]");
          processTp("https://github.com/spring-projects/spring-integration.git", "ec5230abc7500734d7b78a176c291378e100a927", 
            "Inline Method private doClose() : void inlined to public close() : void in class org.springframework.integration.ip.tcp.connection.CachingClientConnectionFactory.CachedConnection");
          processFp("https://github.com/spring-projects/spring-integration.git", "ec5230abc7500734d7b78a176c291378e100a927", 
            "Inline Method protected doWrite(message Message<?>) : void inlined to public handleMessageInternal(message Message<?>) : void in class org.springframework.integration.ip.tcp.TcpSendingMessageHandler");
          processTp("https://github.com/apache/hive.git", "5f78f9ef1e6c798849d34cc66721e6c1d9709b6f", 
            "Extract Method package generateSplitsInfo(conf Configuration, numSplits int) : List<OrcSplit> extracted from package generateSplitsInfo(conf Configuration) : List<OrcSplit> in class org.apache.hadoop.hive.ql.io.orc.OrcInputFormat");

          processTp("https://github.com/wildfly/wildfly.git", "37d842bfed9779e662321a5ee43c36b058386843", 
            "Extract Method public executeReloadAndWaitForCompletion(client ModelControllerClient, timeout int) : void extracted from public executeReloadAndWaitForCompletion(client ModelControllerClient) : void in class org.jboss.as.test.shared.ServerReload");

          processTp("https://github.com/Netflix/zuul.git", "b25d3f32ed2e2da86f5c746098686445c2e2a314", 
            "Extract Method private putFilter(sName String, filter ZuulFilter, lastModified long) : void extracted from public putFilter(file File) : boolean in class com.netflix.zuul.FilterLoader");


          processFp("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "02833b8110cd8c8704194ed765ab243cf65ede42", 
            "Extract Method protected buildThreadIdString() : String extracted from protected getTempDirectory(baseDirectory String) : String in class org.broadleafcommerce.common.file.service.BroadleafFileServiceImpl");
          processTp("https://github.com/jersey/jersey.git", "fab1516773d50bf86d9cc37e2f6db13496f0ecae", 
            "Extract Method public close() : void extracted from public hasNext() : boolean in class org.glassfish.jersey.server.internal.scanning.JarFileScanner",
            "Extract Method private init() : void extracted from public reset() : void in class org.glassfish.jersey.server.internal.scanning.FilesScanner",
            "Extract Method private init() : void extracted from public FilesScanner(fileNames String[], recursive boolean) in class org.glassfish.jersey.server.internal.scanning.FilesScanner");


          processFp("https://github.com/gradle/gradle.git", "b7da315f5fb3ba92de1952000119bffd003b731f", 
            "Move Class org.gradle.model.internal.core.DomainObjectSetBackedModelMap.ToName moved to org.gradle.model.internal.core.DomainObjectCollectionBackedModelMap.ToName",
            "Move Class org.gradle.model.internal.core.DomainObjectSetBackedModelMap.HasNamePredicate moved to org.gradle.model.internal.core.DomainObjectCollectionBackedModelMap.HasNamePredicate");

          processFp("https://github.com/wildfly/wildfly.git", "aa4b5e86084dfadbe3f6fe251f7c7240db9ef759", 
            "Move Class org.jboss.as.messaging.test.TransformerUtils moved to org.jboss.as.messaging.test.TransformerUtils",
            "Move Class org.jboss.as.messaging.test.SubsystemDescriptionsUnitTestCase moved to org.jboss.as.messaging.test.SubsystemDescriptionsUnitTestCase",
            "Move Class org.jboss.as.messaging.test.Subsystem12ParsingUnitTestCase moved to org.jboss.as.messaging.test.Subsystem12ParsingUnitTestCase",
            "Move Class org.jboss.as.messaging.test.Subsystem11ParsingUnitTestCase moved to org.jboss.as.messaging.test.Subsystem11ParsingUnitTestCase",
            "Move Class org.jboss.as.messaging.test.ModelFixers moved to org.jboss.as.messaging.test.ModelFixers",
            "Move Class org.jboss.as.messaging.SecurityRoleRemove moved to org.jboss.as.messaging.SecurityRoleRemove",
            "Move Class org.jboss.as.messaging.SecurityRoleReadAttributeHandler moved to org.jboss.as.messaging.SecurityRoleReadAttributeHandler",
            "Move Class org.jboss.as.messaging.SecurityRoleDefinition moved to org.jboss.as.messaging.SecurityRoleDefinition",
            "Move Class org.jboss.as.messaging.SecurityRoleAttributeHandler moved to org.jboss.as.messaging.SecurityRoleAttributeHandler",
            "Move Class org.jboss.as.messaging.SecurityRoleAdd moved to org.jboss.as.messaging.SecurityRoleAdd",
            "Move Class org.jboss.as.messaging.RemoteTransportDefinition moved to org.jboss.as.messaging.RemoteTransportDefinition",
            "Move Class org.jboss.as.messaging.QueueService moved to org.jboss.as.messaging.QueueService",
            "Move Class org.jboss.as.messaging.QueueRemove moved to org.jboss.as.messaging.QueueRemove",
            "Move Class org.jboss.as.messaging.QueueReadAttributeHandler moved to org.jboss.as.messaging.QueueReadAttributeHandler",
            "Move Class org.jboss.as.messaging.QueueDefinition moved to org.jboss.as.messaging.QueueDefinition",
            "Move Class org.jboss.as.messaging.QueueControlHandler moved to org.jboss.as.messaging.QueueControlHandler",
            "Move Class org.jboss.as.messaging.QueueConfigurationWriteHandler moved to org.jboss.as.messaging.QueueConfigurationWriteHandler",
            "Move Class org.jboss.as.messaging.QueueAdd moved to org.jboss.as.messaging.QueueAdd",
            "Move Class org.jboss.as.messaging.PathDefinition moved to org.jboss.as.messaging.PathDefinition",
            "Move Class org.jboss.as.messaging.OperationValidator moved to org.jboss.as.messaging.OperationValidator",
            "Move Class org.jboss.as.messaging.OperationDefinitionHelper moved to org.jboss.as.messaging.OperationDefinitionHelper",
            "Move Class org.jboss.as.messaging.MessagingXMLWriter moved to org.jboss.as.messaging.MessagingXMLWriter",
            "Move Class org.jboss.as.messaging.MessagingTransformers moved to org.jboss.as.messaging.MessagingTransformers",
            "Move Class org.jboss.as.messaging.MessagingSubsystemRootResourceDefinition moved to org.jboss.as.messaging.MessagingSubsystemRootResourceDefinition",
            "Move Class org.jboss.as.messaging.MessagingSubsystemParser moved to org.jboss.as.messaging.MessagingSubsystemParser",
            "Move Class org.jboss.as.messaging.MessagingSubsystemAdd moved to org.jboss.as.messaging.MessagingSubsystemAdd",
            "Move Class org.jboss.as.messaging.MessagingServices moved to org.jboss.as.messaging.MessagingServices",
            "Move Class org.jboss.as.messaging.MessagingExtension moved to org.jboss.as.messaging.MessagingExtension",
            "Move Class org.jboss.as.messaging.Messaging30SubsystemParser moved to org.jboss.as.messaging.Messaging30SubsystemParser",
            "Move Class org.jboss.as.messaging.Messaging20SubsystemParser moved to org.jboss.as.messaging.Messaging20SubsystemParser",
            "Move Class org.jboss.as.messaging.Messaging14SubsystemParser moved to org.jboss.as.messaging.Messaging14SubsystemParser",
            "Move Class org.jboss.as.messaging.Messaging13SubsystemParser moved to org.jboss.as.messaging.Messaging13SubsystemParser",
            "Move Class org.jboss.as.messaging.Messaging12SubsystemParser moved to org.jboss.as.messaging.Messaging12SubsystemParser",
            "Move Class org.jboss.as.messaging.ManagementUtil moved to org.jboss.as.messaging.ManagementUtil",
            "Move Class org.jboss.as.messaging.JGroupsChannelLocator moved to org.jboss.as.messaging.JGroupsChannelLocator",
            "Move Class org.jboss.as.messaging.InVMTransportDefinition moved to org.jboss.as.messaging.InVMTransportDefinition",
            "Move Class org.jboss.as.messaging.HornetQService moved to org.jboss.as.messaging.HornetQService",
            "Move Class org.jboss.as.messaging.HornetQServerResourceDefinition moved to org.jboss.as.messaging.HornetQServerResourceDefinition",
            "Move Class org.jboss.as.messaging.HornetQServerResource moved to org.jboss.as.messaging.HornetQServerResource",
            "Move Class org.jboss.as.messaging.HornetQServerRemove moved to org.jboss.as.messaging.HornetQServerRemove",
            "Move Class org.jboss.as.messaging.HornetQServerControlWriteHandler moved to org.jboss.as.messaging.HornetQServerControlWriteHandler",
            "Move Class org.jboss.as.messaging.HornetQServerControlHandler moved to org.jboss.as.messaging.HornetQServerControlHandler",
            "Move Class org.jboss.as.messaging.HornetQServerAdd moved to org.jboss.as.messaging.HornetQServerAdd",
            "Move Class org.jboss.as.messaging.HornetQSecurityManagerAS7 moved to org.jboss.as.messaging.HornetQSecurityManagerAS7",
            "Move Class org.jboss.as.messaging.HornetQReloadRequiredHandlers moved to org.jboss.as.messaging.HornetQReloadRequiredHandlers",
            "Move Class org.jboss.as.messaging.HornetQDefaultCredentials moved to org.jboss.as.messaging.HornetQDefaultCredentials",
            "Move Class org.jboss.as.messaging.HornetQActivationService moved to org.jboss.as.messaging.HornetQActivationService",
            "Move Class org.jboss.as.messaging.HTTPUpgradeService moved to org.jboss.as.messaging.HTTPUpgradeService",
            "Move Class org.jboss.as.messaging.HTTPConnectorDefinition moved to org.jboss.as.messaging.HTTPConnectorDefinition",
            "Move Class org.jboss.as.messaging.HTTPAcceptorRemove moved to org.jboss.as.messaging.HTTPAcceptorRemove",
            "Move Class org.jboss.as.messaging.HTTPAcceptorDefinition moved to org.jboss.as.messaging.HTTPAcceptorDefinition",
            "Move Class org.jboss.as.messaging.HTTPAcceptorAdd moved to org.jboss.as.messaging.HTTPAcceptorAdd",
            "Move Class org.jboss.as.messaging.GroupingHandlerWriteAttributeHandler moved to org.jboss.as.messaging.GroupingHandlerWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.BroadcastGroupWriteAttributeHandler moved to org.jboss.as.messaging.BroadcastGroupWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.BroadcastGroupRemove moved to org.jboss.as.messaging.BroadcastGroupRemove",
            "Move Class org.jboss.as.messaging.BroadcastGroupDefinition moved to org.jboss.as.messaging.BroadcastGroupDefinition",
            "Move Class org.jboss.as.messaging.BroadcastGroupControlHandler moved to org.jboss.as.messaging.BroadcastGroupControlHandler",
            "Move Class org.jboss.as.messaging.BroadcastGroupAdd moved to org.jboss.as.messaging.BroadcastGroupAdd",
            "Move Class org.jboss.as.messaging.BridgeWriteAttributeHandler moved to org.jboss.as.messaging.BridgeWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.BridgeRemove moved to org.jboss.as.messaging.BridgeRemove",
            "Move Class org.jboss.as.messaging.BridgeDefinition moved to org.jboss.as.messaging.BridgeDefinition",
            "Move Class org.jboss.as.messaging.BridgeControlHandler moved to org.jboss.as.messaging.BridgeControlHandler",
            "Move Class org.jboss.as.messaging.BridgeAdd moved to org.jboss.as.messaging.BridgeAdd",
            "Move Class org.jboss.as.messaging.BinderServiceUtil moved to org.jboss.as.messaging.BinderServiceUtil",
            "Move Class org.jboss.as.messaging.AttributeMarshallers moved to org.jboss.as.messaging.AttributeMarshallers",
            "Move Class org.jboss.as.messaging.AbstractHornetQComponentControlHandler moved to org.jboss.as.messaging.AbstractHornetQComponentControlHandler",
            "Move Class org.jboss.as.messaging.AbstractQueueControlHandler moved to org.jboss.as.messaging.AbstractQueueControlHandler",
            "Move Class org.jboss.as.messaging.AbstractTransportDefinition moved to org.jboss.as.messaging.AbstractTransportDefinition",
            "Move Class org.jboss.as.messaging.AcceptorControlHandler moved to org.jboss.as.messaging.AcceptorControlHandler",
            "Move Class org.jboss.as.messaging.AddressControlHandler moved to org.jboss.as.messaging.AddressControlHandler",
            "Move Class org.jboss.as.messaging.AddressSettingAdd moved to org.jboss.as.messaging.AddressSettingAdd",
            "Move Class org.jboss.as.messaging.AddressSettingDefinition moved to org.jboss.as.messaging.AddressSettingDefinition",
            "Move Class org.jboss.as.messaging.AddressSettingRemove moved to org.jboss.as.messaging.AddressSettingRemove",
            "Move Class org.jboss.as.messaging.AddressSettingsResolveHandler moved to org.jboss.as.messaging.AddressSettingsResolveHandler",
            "Move Class org.jboss.as.messaging.AddressSettingsValidator moved to org.jboss.as.messaging.AddressSettingsValidator",
            "Move Class org.jboss.as.messaging.AddressSettingsWriteHandler moved to org.jboss.as.messaging.AddressSettingsWriteHandler",
            "Move Class org.jboss.as.messaging.AlternativeAttributeCheckHandler moved to org.jboss.as.messaging.AlternativeAttributeCheckHandler",
            "Move Class org.jboss.as.messaging.ClusterConnectionAdd moved to org.jboss.as.messaging.ClusterConnectionAdd",
            "Move Class org.jboss.as.messaging.ClusterConnectionControlHandler moved to org.jboss.as.messaging.ClusterConnectionControlHandler",
            "Move Class org.jboss.as.messaging.ClusterConnectionDefinition moved to org.jboss.as.messaging.ClusterConnectionDefinition",
            "Move Class org.jboss.as.messaging.ClusterConnectionRemove moved to org.jboss.as.messaging.ClusterConnectionRemove",
            "Move Class org.jboss.as.messaging.ClusterConnectionWriteAttributeHandler moved to org.jboss.as.messaging.ClusterConnectionWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.CommonAttributes moved to org.jboss.as.messaging.CommonAttributes",
            "Move Class org.jboss.as.messaging.ConnectorServiceDefinition moved to org.jboss.as.messaging.ConnectorServiceDefinition",
            "Move Class org.jboss.as.messaging.ConnectorServiceParamDefinition moved to org.jboss.as.messaging.ConnectorServiceParamDefinition",
            "Move Class org.jboss.as.messaging.CoreAddressDefinition moved to org.jboss.as.messaging.CoreAddressDefinition",
            "Move Class org.jboss.as.messaging.CoreAddressResource moved to org.jboss.as.messaging.CoreAddressResource",
            "Move Class org.jboss.as.messaging.DeprecatedAttributeWriteHandler moved to org.jboss.as.messaging.DeprecatedAttributeWriteHandler",
            "Move Class org.jboss.as.messaging.DiscoveryGroupAdd moved to org.jboss.as.messaging.DiscoveryGroupAdd",
            "Move Class org.jboss.as.messaging.DiscoveryGroupDefinition moved to org.jboss.as.messaging.DiscoveryGroupDefinition",
            "Move Class org.jboss.as.messaging.DiscoveryGroupRemove moved to org.jboss.as.messaging.DiscoveryGroupRemove",
            "Move Class org.jboss.as.messaging.DiscoveryGroupWriteAttributeHandler moved to org.jboss.as.messaging.DiscoveryGroupWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.DivertAdd moved to org.jboss.as.messaging.DivertAdd",
            "Move Class org.jboss.as.messaging.DivertConfigurationWriteHandler moved to org.jboss.as.messaging.DivertConfigurationWriteHandler",
            "Move Class org.jboss.as.messaging.DivertDefinition moved to org.jboss.as.messaging.DivertDefinition",
            "Move Class org.jboss.as.messaging.DivertRemove moved to org.jboss.as.messaging.DivertRemove",
            "Move Class org.jboss.as.messaging.GenericTransportDefinition moved to org.jboss.as.messaging.GenericTransportDefinition",
            "Move Class org.jboss.as.messaging.GroupBindingService moved to org.jboss.as.messaging.GroupBindingService",
            "Move Class org.jboss.as.messaging.GroupingHandlerAdd moved to org.jboss.as.messaging.GroupingHandlerAdd",
            "Move Class org.jboss.as.messaging.GroupingHandlerDefinition moved to org.jboss.as.messaging.GroupingHandlerDefinition",
            "Move Class org.jboss.as.messaging.GroupingHandlerRemove moved to org.jboss.as.messaging.GroupingHandlerRemove",
            "Move Class org.jboss.as.messaging.SecurityRoleResource moved to org.jboss.as.messaging.SecurityRoleResource",
            "Move Class org.jboss.as.messaging.SecuritySettingAdd moved to org.jboss.as.messaging.SecuritySettingAdd",
            "Move Class org.jboss.as.messaging.SecuritySettingDefinition moved to org.jboss.as.messaging.SecuritySettingDefinition",
            "Move Class org.jboss.as.messaging.SecuritySettingRemove moved to org.jboss.as.messaging.SecuritySettingRemove",
            "Move Class org.jboss.as.messaging.TransportConfigOperationHandlers moved to org.jboss.as.messaging.TransportConfigOperationHandlers",
            "Move Class org.jboss.as.messaging.TransportParamDefinition moved to org.jboss.as.messaging.TransportParamDefinition",
            "Move Class org.jboss.as.messaging.deployment.CDIDeploymentProcessor moved to org.jboss.as.messaging.deployment.CDIDeploymentProcessor",
            "Move Class org.jboss.as.messaging.deployment.DefaultJMSConnectionFactoryBindingProcessor moved to org.jboss.as.messaging.deployment.DefaultJMSConnectionFactoryBindingProcessor",
            "Move Class org.jboss.as.messaging.deployment.DefaultJMSConnectionFactoryResourceReferenceProcessor moved to org.jboss.as.messaging.deployment.DefaultJMSConnectionFactoryResourceReferenceProcessor",
            "Move Class org.jboss.as.messaging.deployment.JMSCDIExtension moved to org.jboss.as.messaging.deployment.JMSCDIExtension",
            "Move Class org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionAnnotationProcessor moved to org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionAnnotationProcessor",
            "Move Class org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionDescriptorProcessor moved to org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionDescriptorProcessor",
            "Move Class org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionInjectionSource moved to org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionInjectionSource",
            "Move Class org.jboss.as.messaging.deployment.JMSContextProducer moved to org.jboss.as.messaging.deployment.JMSContextProducer",
            "Move Class org.jboss.as.messaging.deployment.JMSDestinationDefinitionAnnotationProcessor moved to org.jboss.as.messaging.deployment.JMSDestinationDefinitionAnnotationProcessor",
            "Move Class org.jboss.as.messaging.deployment.JMSDestinationDefinitionDescriptorProcessor moved to org.jboss.as.messaging.deployment.JMSDestinationDefinitionDescriptorProcessor",
            "Move Class org.jboss.as.messaging.deployment.JMSDestinationDefinitionInjectionSource moved to org.jboss.as.messaging.deployment.JMSDestinationDefinitionInjectionSource",
            "Move Class org.jboss.as.messaging.deployment.JmsDestination moved to org.jboss.as.messaging.deployment.JmsDestination",
            "Move Class org.jboss.as.messaging.deployment.MessagingAttachments moved to org.jboss.as.messaging.deployment.MessagingAttachments",
            "Move Class org.jboss.as.messaging.deployment.MessagingDependencyProcessor moved to org.jboss.as.messaging.deployment.MessagingDependencyProcessor",
            "Move Class org.jboss.as.messaging.deployment.MessagingDeploymentParser_1_0 moved to org.jboss.as.messaging.deployment.MessagingDeploymentParser_1_0",
            "Move Class org.jboss.as.messaging.deployment.MessagingJMSDestinationManagedReferenceFactory moved to org.jboss.as.messaging.deployment.MessagingJMSDestinationManagedReferenceFactory",
            "Move Class org.jboss.as.messaging.deployment.MessagingXmlInstallDeploymentUnitProcessor moved to org.jboss.as.messaging.deployment.MessagingXmlInstallDeploymentUnitProcessor",
            "Move Class org.jboss.as.messaging.deployment.MessagingXmlParsingDeploymentUnitProcessor moved to org.jboss.as.messaging.deployment.MessagingXmlParsingDeploymentUnitProcessor",
            "Move Class org.jboss.as.messaging.deployment.ParseResult moved to org.jboss.as.messaging.deployment.ParseResult",
            "Move Class org.jboss.as.messaging.jms.AS7BindingRegistry moved to org.jboss.as.messaging.jms.AS7BindingRegistry",
            "Move Class org.jboss.as.messaging.jms.AS7RecoveryRegistry moved to org.jboss.as.messaging.jms.AS7RecoveryRegistry",
            "Move Class org.jboss.as.messaging.jms.AbstractJMSRuntimeHandler moved to org.jboss.as.messaging.jms.AbstractJMSRuntimeHandler",
            "Move Class org.jboss.as.messaging.jms.AbstractUpdateJndiHandler moved to org.jboss.as.messaging.jms.AbstractUpdateJndiHandler",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryAdd moved to org.jboss.as.messaging.jms.ConnectionFactoryAdd",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryAttribute moved to org.jboss.as.messaging.jms.ConnectionFactoryAttribute",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryAttributes moved to org.jboss.as.messaging.jms.ConnectionFactoryAttributes",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryDefinition moved to org.jboss.as.messaging.jms.ConnectionFactoryDefinition",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryReadAttributeHandler moved to org.jboss.as.messaging.jms.ConnectionFactoryReadAttributeHandler",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryRemove moved to org.jboss.as.messaging.jms.ConnectionFactoryRemove",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryService moved to org.jboss.as.messaging.jms.ConnectionFactoryService",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryUpdateJndiHandler moved to org.jboss.as.messaging.jms.ConnectionFactoryUpdateJndiHandler",
            "Move Class org.jboss.as.messaging.jms.ConnectionFactoryWriteAttributeHandler moved to org.jboss.as.messaging.jms.ConnectionFactoryWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.jms.JMSManagementHelper moved to org.jboss.as.messaging.jms.JMSManagementHelper",
            "Move Class org.jboss.as.messaging.jms.JMSQueueAdd moved to org.jboss.as.messaging.jms.JMSQueueAdd",
            "Move Class org.jboss.as.messaging.jms.JMSQueueConfigurationRuntimeHandler moved to org.jboss.as.messaging.jms.JMSQueueConfigurationRuntimeHandler",
            "Move Class org.jboss.as.messaging.jms.JMSQueueConfigurationWriteHandler moved to org.jboss.as.messaging.jms.JMSQueueConfigurationWriteHandler",
            "Move Class org.jboss.as.messaging.jms.JMSQueueControlHandler moved to org.jboss.as.messaging.jms.JMSQueueControlHandler",
            "Move Class org.jboss.as.messaging.jms.JMSQueueDefinition moved to org.jboss.as.messaging.jms.JMSQueueDefinition",
            "Move Class org.jboss.as.messaging.jms.JMSQueueReadAttributeHandler moved to org.jboss.as.messaging.jms.JMSQueueReadAttributeHandler",
            "Move Class org.jboss.as.messaging.jms.JMSQueueRemove moved to org.jboss.as.messaging.jms.JMSQueueRemove",
            "Move Class org.jboss.as.messaging.jms.JMSQueueService moved to org.jboss.as.messaging.jms.JMSQueueService",
            "Move Class org.jboss.as.messaging.jms.JMSQueueUpdateJndiHandler moved to org.jboss.as.messaging.jms.JMSQueueUpdateJndiHandler",
            "Move Class org.jboss.as.messaging.jms.JMSServerControlHandler moved to org.jboss.as.messaging.jms.JMSServerControlHandler",
            "Move Class org.jboss.as.messaging.jms.JMSService moved to org.jboss.as.messaging.jms.JMSService",
            "Move Class org.jboss.as.messaging.jms.JMSServices moved to org.jboss.as.messaging.jms.JMSServices",
            "Move Class org.jboss.as.messaging.jms.JMSTopicAdd moved to org.jboss.as.messaging.jms.JMSTopicAdd",
            "Move Class org.jboss.as.messaging.jms.JMSTopicConfigurationRuntimeHandler moved to org.jboss.as.messaging.jms.JMSTopicConfigurationRuntimeHandler",
            "Move Class org.jboss.as.messaging.jms.JMSTopicConfigurationWriteHandler moved to org.jboss.as.messaging.jms.JMSTopicConfigurationWriteHandler",
            "Move Class org.jboss.as.messaging.jms.JMSTopicControlHandler moved to org.jboss.as.messaging.jms.JMSTopicControlHandler",
            "Move Class org.jboss.as.messaging.jms.JMSTopicDefinition moved to org.jboss.as.messaging.jms.JMSTopicDefinition",
            "Move Class org.jboss.as.messaging.jms.JMSTopicReadAttributeHandler moved to org.jboss.as.messaging.jms.JMSTopicReadAttributeHandler",
            "Move Class org.jboss.as.messaging.jms.JMSTopicRemove moved to org.jboss.as.messaging.jms.JMSTopicRemove",
            "Move Class org.jboss.as.messaging.jms.JMSTopicService moved to org.jboss.as.messaging.jms.JMSTopicService",
            "Move Class org.jboss.as.messaging.jms.JMSTopicUpdateJndiHandler moved to org.jboss.as.messaging.jms.JMSTopicUpdateJndiHandler",
            "Move Class org.jboss.as.messaging.jms.PooledConnectionFactoryAdd moved to org.jboss.as.messaging.jms.PooledConnectionFactoryAdd",
            "Move Class org.jboss.as.messaging.jms.PooledConnectionFactoryConfigProperties moved to org.jboss.as.messaging.jms.PooledConnectionFactoryConfigProperties",
            "Move Class org.jboss.as.messaging.jms.PooledConnectionFactoryConfigurationRuntimeHandler moved to org.jboss.as.messaging.jms.PooledConnectionFactoryConfigurationRuntimeHandler",
            "Move Class org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition moved to org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition",
            "Move Class org.jboss.as.messaging.jms.PooledConnectionFactoryRemove moved to org.jboss.as.messaging.jms.PooledConnectionFactoryRemove",
            "Move Class org.jboss.as.messaging.jms.PooledConnectionFactoryService moved to org.jboss.as.messaging.jms.PooledConnectionFactoryService",
            "Move Class org.jboss.as.messaging.jms.PooledConnectionFactoryWriteAttributeHandler moved to org.jboss.as.messaging.jms.PooledConnectionFactoryWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.jms.TransactionManagerLocator moved to org.jboss.as.messaging.jms.TransactionManagerLocator",
            "Move Class org.jboss.as.messaging.jms.Validators moved to org.jboss.as.messaging.jms.Validators",
            "Move Class org.jboss.as.messaging.jms.bridge.InfiniteOrPositiveValidators moved to org.jboss.as.messaging.jms.bridge.InfiniteOrPositiveValidators",
            "Move Class org.jboss.as.messaging.jms.bridge.JMSBridgeAdd moved to org.jboss.as.messaging.jms.bridge.JMSBridgeAdd",
            "Move Class org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition moved to org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition",
            "Move Class org.jboss.as.messaging.jms.bridge.JMSBridgeHandler moved to org.jboss.as.messaging.jms.bridge.JMSBridgeHandler",
            "Move Class org.jboss.as.messaging.jms.bridge.JMSBridgeRemove moved to org.jboss.as.messaging.jms.bridge.JMSBridgeRemove",
            "Move Class org.jboss.as.messaging.jms.bridge.JMSBridgeService moved to org.jboss.as.messaging.jms.bridge.JMSBridgeService",
            "Move Class org.jboss.as.messaging.jms.bridge.JMSBridgeWriteAttributeHandler moved to org.jboss.as.messaging.jms.bridge.JMSBridgeWriteAttributeHandler",
            "Move Class org.jboss.as.messaging.jms.cli.ConnectionFactoryHandlerProvider moved to org.jboss.as.messaging.jms.cli.ConnectionFactoryHandlerProvider",
            "Move Class org.jboss.as.messaging.jms.cli.CreateJMSResourceHandler moved to org.jboss.as.messaging.jms.cli.CreateJMSResourceHandler",
            "Move Class org.jboss.as.messaging.jms.cli.CreateJMSResourceHandlerProvider moved to org.jboss.as.messaging.jms.cli.CreateJMSResourceHandlerProvider",
            "Move Class org.jboss.as.messaging.jms.cli.DeleteJMSResourceHandler moved to org.jboss.as.messaging.jms.cli.DeleteJMSResourceHandler",
            "Move Class org.jboss.as.messaging.jms.cli.DeleteJMSResourceHandlerProvider moved to org.jboss.as.messaging.jms.cli.DeleteJMSResourceHandlerProvider",
            "Move Class org.jboss.as.messaging.jms.cli.JMSQueueHandlerProvider moved to org.jboss.as.messaging.jms.cli.JMSQueueHandlerProvider",
            "Move Class org.jboss.as.messaging.jms.cli.JMSTopicHandlerProvider moved to org.jboss.as.messaging.jms.cli.JMSTopicHandlerProvider",
            "Move Class org.jboss.as.messaging.logging.MessagingLogger moved to org.jboss.as.messaging.logging.MessagingLogger",
            "Move Class org.jboss.as.messaging.MessagingPathsTestCase moved to org.jboss.as.messaging.MessagingPathsTestCase",
            "Move Class org.jboss.as.messaging.jms.test.AttributesTestBase moved to org.jboss.as.messaging.jms.test.AttributesTestBase",
            "Move Class org.jboss.as.messaging.jms.test.ConfigurationAttributesTestCase moved to org.jboss.as.messaging.jms.test.ConfigurationAttributesTestCase",
            "Move Class org.jboss.as.messaging.jms.test.PooledConnectionFactoryAttributesTestCase moved to org.jboss.as.messaging.jms.test.PooledConnectionFactoryAttributesTestCase",
            "Move Class org.jboss.as.messaging.test.AS7BindingRegistryTestCase moved to org.jboss.as.messaging.test.AS7BindingRegistryTestCase",
            "Move Class org.jboss.as.messaging.test.JMSBridge13ParsingUnitTestCase moved to org.jboss.as.messaging.test.JMSBridge13ParsingUnitTestCase",
            "Move Class org.jboss.as.messaging.test.MessagingDependencies moved to org.jboss.as.messaging.test.MessagingDependencies",
            "Move Class org.jboss.as.messaging.test.MessagingSubsystem13TestCase moved to org.jboss.as.messaging.test.MessagingSubsystem13TestCase",
            "Move Class org.jboss.as.messaging.test.MessagingSubsystem14TestCase moved to org.jboss.as.messaging.test.MessagingSubsystem14TestCase",
            "Move Class org.jboss.as.messaging.test.MessagingSubsystem15TestCase moved to org.jboss.as.messaging.test.MessagingSubsystem15TestCase",
            "Move Class org.jboss.as.messaging.test.MessagingSubsystem20TestCase moved to org.jboss.as.messaging.test.MessagingSubsystem20TestCase",
            "Move Class org.jboss.as.messaging.test.MessagingSubsystem30TestCase moved to org.jboss.as.messaging.test.MessagingSubsystem30TestCase");
          processTp("https://github.com/jankotek/MapDB.git", "32dd05fc13b53873bf18c589622b55d12e3883c7", 
            "Pull Up Method protected longStackValParityGet(value long) : long from class org.mapdb.StoreDirect to protected longParitySet(value long) : long from class org.mapdb.Store",
            "Pull Up Method protected longStackValParitySet(value long) : long from class org.mapdb.StoreDirect to protected longParitySet(value long) : long from class org.mapdb.Store",
            "Extract Method private insertOrUpdate(recid long, out DataOutputByteArray, isInsert boolean) : void extracted from protected update2(recid long, out DataOutputByteArray) : void in class org.mapdb.StoreAppend");

          processTp("https://github.com/bitcoinj/bitcoinj.git", "2fd96c777164dd812e8b5a4294b162889601df1d", 
            "Extract Method public newSha256Digest() : MessageDigest extracted from public sha256hash160(input byte[]) : byte[] in class org.bitcoinj.core.Utils");


          processFp("https://github.com/bitcoinj/bitcoinj.git", "faf92971dd634a60c344782139f46525696abf22", 
            "Extract Method public twiceOf(contents byte[]) : Sha256Hash extracted from public createDouble(contents byte[]) : Sha256Hash in class org.bitcoinj.core.Sha256Hash",
            "Extract Method public of(contents byte[]) : Sha256Hash extracted from public create(contents byte[]) : Sha256Hash in class org.bitcoinj.core.Sha256Hash");
          processTp("https://github.com/bitcoinj/bitcoinj.git", "a6601066ddc72ef8e71c46c5a51e1252ea0a1af5", 
            "Move Attribute private digest : MessageDigest from class org.bitcoinj.core.Utils to class org.bitcoinj.core.Sha256Hash");

          processTp("https://github.com/bitcoinj/bitcoinj.git", "1d96e1ad1dca6e2151603e10515bb04f0c2730fc", 
            "Extract Method public updatedChannel(channel StoredServerChannel) : void extracted from public closeChannel(channel StoredServerChannel) : void in class org.bitcoinj.protocols.channels.StoredPaymentChannelServerStates",
            "Extract Method package updatedChannel(channel StoredClientChannel) : void extracted from package removeChannel(channel StoredClientChannel) : void in class org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates",
            "Extract Method package updatedChannel(channel StoredClientChannel) : void extracted from private putChannel(channel StoredClientChannel, updateWallet boolean) : void in class org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates");

          processTp("https://github.com/structr/structr.git", "6c59050b8b03adf6d8043f3fb7add0496f447edf", 
            "Extract Method private getSchemaProperties(schemaNode SchemaNode) : List<SchemaProperty> extracted from private getPropertiesForView(type Class, view String, schemaNode SchemaNode) : Map<String,Object> in class org.structr.rest.resource.SchemaTypeResource");


          processFp("https://github.com/VoltDB/voltdb.git", "9533c537d519a60164c7ad64d5b3ab9f8e234262", 
            "Extract Method public doInserts(client Client, ratio int) : void extracted from private runTest() : void in class exportbenchmark.ExportBenchmark");

          processFp("https://github.com/wildfly/wildfly.git", "63170d10ecb30ce2725d1db3b5450ec380a0fddc", 
            "Move Class org.jboss.as.messaging.HornetQServerControlWriteHandler.MessageCounterEnabledHandler moved to org.jboss.as.messaging.HornetQServerResourceDefinition.MessageCounterEnabledHandler",
            "Move Class org.jboss.as.messaging.HornetQServerControlWriteHandler.ClusteredAttributeHandlers moved to org.jboss.as.messaging.HornetQServerResourceDefinition.ClusteredAttributeHandlers");
          processTp("https://github.com/facebook/buck.git", "cfea606b129dbfc5703eb279d4803185afc99c58", 
            "Extract Method public getPathToJSBundleDir(target BuildTarget) : Path extracted from protected ReactNativeBundle(ruleParams BuildRuleParams, resolver SourcePathResolver, entryPath SourcePath, isDevMode boolean, bundleName String, jsPackager SourcePath, platform ReactNativePlatform, depsFinder ReactNativeDeps) in class com.facebook.buck.js.ReactNativeBundle");

          processTp("https://github.com/apache/cassandra.git", "f797bfa4da53315b49f8d97b784047f33ba1bf5f", 
            "Move Class org.apache.cassandra.cql3.CrcCheckChanceTest moved to org.apache.cassandra.cql3.validation.miscellaneous.CrcCheckChanceTest",
            "Move Class org.apache.cassandra.cql3.SSTableMetadataTrackingTest moved to org.apache.cassandra.cql3.validation.miscellaneous.SSTableMetadataTrackingTest",
            "Move Class org.apache.cassandra.cql3.TypeTest moved to org.apache.cassandra.cql3.validation.entities.TypeTest",
            "Extract Method protected assertInvalidThrowMessage(errorMessage String, exception Class<? extends Throwable>, query String, values Object[]) : void extracted from protected assertInvalidMessage(errorMessage String, query String, values Object[]) : void in class org.apache.cassandra.cql3.CQLTester",
            "Extract Method private makeCasRequest(options BatchQueryOptions, state QueryState) : Pair<CQL3CasRequest,Set<ColumnDefinition>> extracted from private executeWithConditions(options BatchQueryOptions, state QueryState) : ResultMessage in class org.apache.cassandra.cql3.statements.BatchStatement",
            "Extract Method private executeInternalWithoutCondition(queryState QueryState, options QueryOptions) : ResultMessage extracted from public executeInternal(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.BatchStatement",
            "Extract Method private makeCasRequest(queryState QueryState, options QueryOptions) : CQL3CasRequest extracted from public executeWithCondition(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.ModificationStatement",
            "Extract Method public executeInternalWithoutCondition(queryState QueryState, options QueryOptions) : ResultMessage extracted from public executeInternal(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.ModificationStatement",
            "Extract Method private fromUnixTimestamp(timestamp long, nanos long) : long extracted from private fromUnixTimestamp(timestamp long) : long in class org.apache.cassandra.utils.UUIDGen",
            "Extract Method protected createTableName() : String extracted from protected createTable(query String) : String in class org.apache.cassandra.cql3.CQLTester");

          processTp("https://github.com/facebook/buck.git", "d49765899cb9df6781fff9773ffc244b5167351c", 
            "Extract Method private getTestPathPredicate(enableStringWhitelisting boolean, whitelistedStringDirs ImmutableSet<Path>, locales ImmutableSet<String>) : Predicate<Path> extracted from public testFilterStrings() : void in class com.facebook.buck.android.FilterResourcesStepTest",
            "Extract Method private getTestPathPredicate(enableStringWhitelisting boolean, whitelistedStringDirs ImmutableSet<Path>, locales ImmutableSet<String>) : Predicate<Path> extracted from public testFilterLocales() : void in class com.facebook.buck.android.FilterResourcesStepTest");

          processTp("https://github.com/facebook/buck.git", "84b7b3974ae8171a4de2f804eb94fcd1d6cd6647", 
            "Move Class com.facebook.buck.java.ReportGenerator moved to com.facebook.buck.java.coverage.ReportGenerator");

          processTp("https://github.com/spring-projects/spring-data-neo4j.git", "071588a418dbc743e0f7dbfe218cd8a6c0f97421", 
            "Move Class org.springframework.data.neo4j.repository.support.GraphRepositoryFactoryTest moved to org.springframework.data.neo4j.repositories.support.GraphRepositoryFactoryTest",
            "Move Class org.springframework.data.neo4j.integration.web.service.UserServiceImpl moved to org.springframework.data.neo4j.web.service.UserServiceImpl",
            "Move Class org.springframework.data.neo4j.integration.web.service.UserService moved to org.springframework.data.neo4j.web.service.UserService",
            "Move Class org.springframework.data.neo4j.integration.web.repo.UserRepository moved to org.springframework.data.neo4j.web.repo.UserRepository",
            "Move Class org.springframework.data.neo4j.integration.web.repo.GenreRepository moved to org.springframework.data.neo4j.web.repo.GenreRepository",
            "Move Class org.springframework.data.neo4j.integration.web.domain.User moved to org.springframework.data.neo4j.web.domain.User",
            "Move Class org.springframework.data.neo4j.integration.web.domain.Genre moved to org.springframework.data.neo4j.web.domain.Genre",
            "Move Class org.springframework.data.neo4j.integration.web.domain.Cinema moved to org.springframework.data.neo4j.web.domain.Cinema",
            "Move Class org.springframework.data.neo4j.integration.web.controller.UserController moved to org.springframework.data.neo4j.web.controller.UserController",
            "Move Class org.springframework.data.neo4j.integration.web.context.WebPersistenceContext moved to org.springframework.data.neo4j.web.context.WebPersistenceContext",
            "Move Class org.springframework.data.neo4j.integration.web.context.WebAppContext moved to org.springframework.data.neo4j.web.context.WebAppContext",
            "Move Class org.springframework.data.neo4j.integration.web.WebIntegrationTest moved to org.springframework.data.neo4j.web.WebIntegrationTest",
            "Move Class org.springframework.data.neo4j.integration.transactions.service.WrapperService moved to org.springframework.data.neo4j.transactions.service.WrapperService",
            "Move Class org.springframework.data.neo4j.integration.transactions.service.BusinessService moved to org.springframework.data.neo4j.transactions.service.BusinessService",
            "Move Class org.springframework.data.neo4j.integration.transactions.TransactionBoundaryTest moved to org.springframework.data.neo4j.transactions.TransactionBoundaryTest",
            "Move Class org.springframework.data.neo4j.integration.transactions.ApplicationConfig moved to org.springframework.data.neo4j.transactions.ApplicationConfig",
            "Move Class org.springframework.data.neo4j.integration.template.context.DataManipulationEventConfiguration moved to org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration",
            "Move Class org.springframework.data.neo4j.integration.template.TestNeo4jEventListener moved to org.springframework.data.neo4j.template.TestNeo4jEventListener",
            "Move Class org.springframework.data.neo4j.integration.template.TemplateApplicationEventTest moved to org.springframework.data.neo4j.template.TemplateApplicationEventTest",
            "Move Class org.springframework.data.neo4j.integration.template.Neo4jTemplateTest moved to org.springframework.data.neo4j.template.Neo4jTemplateTest",
            "Move Class org.springframework.data.neo4j.integration.template.ExceptionTranslationTest moved to org.springframework.data.neo4j.template.ExceptionTranslationTest",
            "Move Class org.springframework.data.neo4j.integration.repositories.repo.UserRepository moved to org.springframework.data.neo4j.repositories.repo.UserRepository",
            "Move Class org.springframework.data.neo4j.integration.repositories.repo.PersistenceContextInTheSamePackage moved to org.springframework.data.neo4j.repositories.repo.PersistenceContextInTheSamePackage",
            "Move Class org.springframework.data.neo4j.integration.repositories.repo.MovieRepository moved to org.springframework.data.neo4j.repositories.repo.MovieRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.Person moved to org.springframework.data.neo4j.examples.movies.domain.Person",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.Movie moved to org.springframework.data.neo4j.examples.movies.domain.Movie",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.Genre moved to org.springframework.data.neo4j.examples.movies.domain.Genre",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.Cinema moved to org.springframework.data.neo4j.examples.movies.domain.Cinema",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.Actor moved to org.springframework.data.neo4j.examples.movies.domain.Actor",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.AbstractEntity moved to org.springframework.data.neo4j.examples.movies.domain.AbstractEntity",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.AbstractAnnotatedEntity moved to org.springframework.data.neo4j.examples.movies.domain.AbstractAnnotatedEntity",
            "Move Class org.springframework.data.neo4j.integration.movies.TransactionIntegrationTest moved to org.springframework.data.neo4j.transactions.TransactionIntegrationTest",
            "Move Class org.springframework.data.neo4j.integration.movies.QueryIntegrationTest moved to org.springframework.data.neo4j.queries.QueryIntegrationTest",
            "Move Class org.springframework.data.neo4j.integration.movies.DerivedQueryTest moved to org.springframework.data.neo4j.queries.DerivedQueryTest",
            "Move Class org.springframework.data.neo4j.integration.jsr303.service.AdultService moved to org.springframework.data.neo4j.examples.jsr303.service.AdultService",
            "Move Class org.springframework.data.neo4j.integration.jsr303.repo.AdultRepository moved to org.springframework.data.neo4j.examples.jsr303.repo.AdultRepository",
            "Move Class org.springframework.data.neo4j.integration.extensions.CustomGraphRepository moved to org.springframework.data.neo4j.extensions.CustomGraphRepository",
            "Move Class org.springframework.data.neo4j.integration.extensions.CustomGraphRepositoryImpl moved to org.springframework.data.neo4j.extensions.CustomGraphRepositoryImpl",
            "Move Class org.springframework.data.neo4j.integration.extensions.CustomGraphRepositoryTest moved to org.springframework.data.neo4j.extensions.CustomGraphRepositoryTest",
            "Move Class org.springframework.data.neo4j.integration.extensions.CustomPersistenceContext moved to org.springframework.data.neo4j.extensions.CustomPersistenceContext",
            "Move Class org.springframework.data.neo4j.integration.extensions.UserRepository moved to org.springframework.data.neo4j.extensions.UserRepository",
            "Move Class org.springframework.data.neo4j.integration.extensions.domain.User moved to org.springframework.data.neo4j.extensions.domain.User",
            "Move Class org.springframework.data.neo4j.integration.helloworld.GalaxyServiceTest moved to org.springframework.data.neo4j.examples.galaxy.GalaxyServiceTest",
            "Move Class org.springframework.data.neo4j.integration.helloworld.service.GalaxyService moved to org.springframework.data.neo4j.examples.galaxy.service.GalaxyService",
            "Move Class org.springframework.data.neo4j.integration.jsr303.JSR303Test moved to org.springframework.data.neo4j.examples.jsr303.JSR303Test",
            "Move Class org.springframework.data.neo4j.integration.jsr303.WebConfiguration moved to org.springframework.data.neo4j.examples.jsr303.WebConfiguration",
            "Move Class org.springframework.data.neo4j.integration.jsr303.controller.AdultController moved to org.springframework.data.neo4j.examples.jsr303.controller.AdultController",
            "Move Class org.springframework.data.neo4j.integration.jsr303.domain.Adult moved to org.springframework.data.neo4j.examples.jsr303.domain.Adult",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.Rating moved to org.springframework.data.neo4j.examples.movies.domain.Rating",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.ReleasedMovie moved to org.springframework.data.neo4j.examples.movies.domain.ReleasedMovie",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.TempMovie moved to org.springframework.data.neo4j.examples.movies.domain.TempMovie",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.User moved to org.springframework.data.neo4j.examples.movies.domain.User",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.EntityWrappingQueryResult moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.RichUserQueryResult moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.RichUserQueryResult",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.UserQueryResult moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResult",
            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.UserQueryResultInterface moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResultInterface",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.AbstractAnnotatedEntityRepository moved to org.springframework.data.neo4j.examples.movies.repo.AbstractAnnotatedEntityRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.AbstractEntityRepository moved to org.springframework.data.neo4j.examples.movies.repo.AbstractEntityRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.ActorRepository moved to org.springframework.data.neo4j.examples.movies.repo.ActorRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.CinemaRepository moved to org.springframework.data.neo4j.examples.movies.repo.CinemaRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.GenreRepository moved to org.springframework.data.neo4j.examples.movies.repo.GenreRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.RatingRepository moved to org.springframework.data.neo4j.examples.movies.repo.RatingRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.TempMovieRepository moved to org.springframework.data.neo4j.examples.movies.repo.TempMovieRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.UnmanagedUserPojo moved to org.springframework.data.neo4j.examples.movies.repo.UnmanagedUserPojo",
            "Move Class org.springframework.data.neo4j.integration.movies.repo.UserRepository moved to org.springframework.data.neo4j.examples.movies.repo.UserRepository",
            "Move Class org.springframework.data.neo4j.integration.movies.service.UserService moved to org.springframework.data.neo4j.examples.movies.service.UserService",
            "Move Class org.springframework.data.neo4j.integration.movies.service.UserServiceImpl moved to org.springframework.data.neo4j.examples.movies.service.UserServiceImpl",
            "Move Class org.springframework.data.neo4j.integration.repositories.ProgrammaticRepositoryTest moved to org.springframework.data.neo4j.repositories.ProgrammaticRepositoryTest",
            "Move Class org.springframework.data.neo4j.integration.repositories.RepoScanningTest moved to org.springframework.data.neo4j.repositories.RepoScanningTest",
            "Move Class org.springframework.data.neo4j.integration.repositories.RepositoryDefinitionTest moved to org.springframework.data.neo4j.repositories.RepositoryDefinitionTest",
            "Move Class org.springframework.data.neo4j.integration.repositories.domain.Movie moved to org.springframework.data.neo4j.repositories.domain.Movie",
            "Move Class org.springframework.data.neo4j.integration.repositories.domain.User moved to org.springframework.data.neo4j.repositories.domain.User");

          processTp("https://github.com/rackerlabs/blueflood.git", "fce2d1f07c14bbac286e16ec666fd4bf26abd43d", 
            "Move Method public sendResponse(channel ChannelHandlerContext, request HttpRequest, messageBody String, status HttpResponseStatus) : void from class com.rackspacecloud.blueflood.inputs.handlers.HttpMetricsIngestionHandler to public sendResponse(channel ChannelHandlerContext, request HttpRequest, messageBody String, status HttpResponseStatus) : void from class com.rackspacecloud.blueflood.http.DefaultHandler",
            "Move Attribute private sendResponseTimer : Timer from class com.rackspacecloud.blueflood.inputs.handlers.HttpMetricsIngestionHandler to class com.rackspacecloud.blueflood.http.DefaultHandler");


          processFp("https://github.com/antlr/antlr4.git", "46202d98a260fb865e6dde6732deb92bcb793c80", 
            "Move Class org.antlr.v4.test.runtime.python3.TestSets moved to org.antlr.v4.test.runtime.python3.TestSets",
            "Move Class org.antlr.v4.test.runtime.python3.TestSemPredEvalParser moved to org.antlr.v4.test.runtime.python3.TestSemPredEvalParser",
            "Move Class org.antlr.v4.test.runtime.python3.TestSemPredEvalLexer moved to org.antlr.v4.test.runtime.python3.TestSemPredEvalLexer",
            "Move Class org.antlr.v4.test.runtime.python3.TestPerformance moved to org.antlr.v4.test.runtime.python3.TestPerformance",
            "Move Class org.antlr.v4.test.runtime.python3.TestParserExec moved to org.antlr.v4.test.runtime.python3.TestParserExec",
            "Move Class org.antlr.v4.test.runtime.python3.TestParserErrors moved to org.antlr.v4.test.runtime.python3.TestParserErrors",
            "Move Class org.antlr.v4.test.runtime.python3.TestParseTrees moved to org.antlr.v4.test.runtime.python3.TestParseTrees",
            "Move Class org.antlr.v4.test.runtime.python3.TestListeners moved to org.antlr.v4.test.runtime.python3.TestListeners",
            "Move Class org.antlr.v4.test.runtime.python3.TestLexerExec moved to org.antlr.v4.test.runtime.python3.TestLexerExec",
            "Move Class org.antlr.v4.test.runtime.python3.TestLexerErrors moved to org.antlr.v4.test.runtime.python3.TestLexerErrors",
            "Move Class org.antlr.v4.test.runtime.python3.TestLeftRecursion moved to org.antlr.v4.test.runtime.python3.TestLeftRecursion",
            "Move Class org.antlr.v4.test.runtime.python3.TestFullContextParsing moved to org.antlr.v4.test.runtime.python3.TestFullContextParsing",
            "Move Class org.antlr.v4.test.runtime.python3.TestCompositeParsers moved to org.antlr.v4.test.runtime.python3.TestCompositeParsers",
            "Move Class org.antlr.v4.test.runtime.python3.TestCompositeLexers moved to org.antlr.v4.test.runtime.python3.TestCompositeLexers",
            "Move Class org.antlr.v4.test.runtime.python2.TestSets moved to org.antlr.v4.test.runtime.python2.TestSets",
            "Move Class org.antlr.v4.test.runtime.python2.TestSemPredEvalParser moved to org.antlr.v4.test.runtime.python2.TestSemPredEvalParser",
            "Move Class org.antlr.v4.test.runtime.python2.TestSemPredEvalLexer moved to org.antlr.v4.test.runtime.python2.TestSemPredEvalLexer",
            "Move Class org.antlr.v4.test.runtime.python2.TestPerformance moved to org.antlr.v4.test.runtime.python2.TestPerformance",
            "Move Class org.antlr.v4.test.runtime.python2.TestParserExec moved to org.antlr.v4.test.runtime.python2.TestParserExec",
            "Move Class org.antlr.v4.test.runtime.python2.TestParserErrors moved to org.antlr.v4.test.runtime.python2.TestParserErrors",
            "Move Class org.antlr.v4.test.runtime.python2.TestParseTrees moved to org.antlr.v4.test.runtime.python2.TestParseTrees",
            "Move Class org.antlr.v4.test.runtime.python2.TestListeners moved to org.antlr.v4.test.runtime.python2.TestListeners",
            "Move Class org.antlr.v4.test.runtime.java.TestParserExec moved to org.antlr.v4.test.runtime.java.TestParserExec",
            "Move Class org.antlr.v4.test.runtime.java.TestParserErrors moved to org.antlr.v4.test.runtime.java.TestParserErrors",
            "Move Class org.antlr.v4.test.runtime.java.TestParseTrees moved to org.antlr.v4.test.runtime.java.TestParseTrees",
            "Move Class org.antlr.v4.test.runtime.java.TestListeners moved to org.antlr.v4.test.runtime.java.TestListeners",
            "Move Class org.antlr.v4.test.runtime.java.TestLexerExec moved to org.antlr.v4.test.runtime.java.TestLexerExec",
            "Move Class org.antlr.v4.test.runtime.java.TestLexerErrors moved to org.antlr.v4.test.runtime.java.TestLexerErrors",
            "Move Class org.antlr.v4.test.runtime.java.TestLeftRecursion moved to org.antlr.v4.test.runtime.java.TestLeftRecursion",
            "Move Class org.antlr.v4.test.runtime.java.TestFullContextParsing moved to org.antlr.v4.test.runtime.java.TestFullContextParsing",
            "Move Class org.antlr.v4.test.runtime.java.TestCompositeParsers moved to org.antlr.v4.test.runtime.java.TestCompositeParsers",
            "Move Class org.antlr.v4.test.runtime.java.TestCompositeLexers moved to org.antlr.v4.test.runtime.java.TestCompositeLexers",
            "Move Class org.antlr.v4.test.runtime.csharp.TestSets moved to org.antlr.v4.test.runtime.csharp.TestSets",
            "Move Class org.antlr.v4.test.runtime.csharp.TestSemPredEvalParser moved to org.antlr.v4.test.runtime.csharp.TestSemPredEvalParser",
            "Move Class org.antlr.v4.test.runtime.csharp.TestCompositeLexers moved to org.antlr.v4.test.runtime.csharp.TestCompositeLexers",
            "Move Class org.antlr.v4.test.runtime.csharp.TestCompositeParsers moved to org.antlr.v4.test.runtime.csharp.TestCompositeParsers",
            "Move Class org.antlr.v4.test.runtime.csharp.TestFullContextParsing moved to org.antlr.v4.test.runtime.csharp.TestFullContextParsing",
            "Move Class org.antlr.v4.test.runtime.csharp.TestLeftRecursion moved to org.antlr.v4.test.runtime.csharp.TestLeftRecursion",
            "Move Class org.antlr.v4.test.runtime.csharp.TestLexerErrors moved to org.antlr.v4.test.runtime.csharp.TestLexerErrors",
            "Move Class org.antlr.v4.test.runtime.csharp.TestLexerExec moved to org.antlr.v4.test.runtime.csharp.TestLexerExec",
            "Move Class org.antlr.v4.test.runtime.csharp.TestListeners moved to org.antlr.v4.test.runtime.csharp.TestListeners",
            "Move Class org.antlr.v4.test.runtime.csharp.TestParseTrees moved to org.antlr.v4.test.runtime.csharp.TestParseTrees",
            "Move Class org.antlr.v4.test.runtime.csharp.TestParserErrors moved to org.antlr.v4.test.runtime.csharp.TestParserErrors",
            "Move Class org.antlr.v4.test.runtime.csharp.TestParserExec moved to org.antlr.v4.test.runtime.csharp.TestParserExec",
            "Move Class org.antlr.v4.test.runtime.csharp.TestPerformance moved to org.antlr.v4.test.runtime.csharp.TestPerformance",
            "Move Class org.antlr.v4.test.runtime.csharp.TestSemPredEvalLexer moved to org.antlr.v4.test.runtime.csharp.TestSemPredEvalLexer",
            "Move Class org.antlr.v4.test.runtime.java.TestPerformance moved to org.antlr.v4.test.runtime.java.TestPerformance",
            "Move Class org.antlr.v4.test.runtime.java.TestSemPredEvalLexer moved to org.antlr.v4.test.runtime.java.TestSemPredEvalLexer",
            "Move Class org.antlr.v4.test.runtime.java.TestSemPredEvalParser moved to org.antlr.v4.test.runtime.java.TestSemPredEvalParser",
            "Move Class org.antlr.v4.test.runtime.java.TestSets moved to org.antlr.v4.test.runtime.java.TestSets",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestCompositeLexers moved to org.antlr.v4.test.runtime.javascript.node.TestCompositeLexers",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestCompositeParsers moved to org.antlr.v4.test.runtime.javascript.node.TestCompositeParsers",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestFullContextParsing moved to org.antlr.v4.test.runtime.javascript.node.TestFullContextParsing",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestLeftRecursion moved to org.antlr.v4.test.runtime.javascript.node.TestLeftRecursion",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestLexerErrors moved to org.antlr.v4.test.runtime.javascript.node.TestLexerErrors",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestLexerExec moved to org.antlr.v4.test.runtime.javascript.node.TestLexerExec",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestListeners moved to org.antlr.v4.test.runtime.javascript.node.TestListeners",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestParseTrees moved to org.antlr.v4.test.runtime.javascript.node.TestParseTrees",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestParserErrors moved to org.antlr.v4.test.runtime.javascript.node.TestParserErrors",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestParserExec moved to org.antlr.v4.test.runtime.javascript.node.TestParserExec",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestPerformance moved to org.antlr.v4.test.runtime.javascript.node.TestPerformance",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestSemPredEvalLexer moved to org.antlr.v4.test.runtime.javascript.node.TestSemPredEvalLexer",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestSemPredEvalParser moved to org.antlr.v4.test.runtime.javascript.node.TestSemPredEvalParser",
            "Move Class org.antlr.v4.test.runtime.javascript.node.TestSets moved to org.antlr.v4.test.runtime.javascript.node.TestSets",
            "Move Class org.antlr.v4.test.runtime.python2.TestCompositeLexers moved to org.antlr.v4.test.runtime.python2.TestCompositeLexers",
            "Move Class org.antlr.v4.test.runtime.python2.TestCompositeParsers moved to org.antlr.v4.test.runtime.python2.TestCompositeParsers",
            "Move Class org.antlr.v4.test.runtime.python2.TestFullContextParsing moved to org.antlr.v4.test.runtime.python2.TestFullContextParsing",
            "Move Class org.antlr.v4.test.runtime.python2.TestLeftRecursion moved to org.antlr.v4.test.runtime.python2.TestLeftRecursion",
            "Move Class org.antlr.v4.test.runtime.python2.TestLexerErrors moved to org.antlr.v4.test.runtime.python2.TestLexerErrors",
            "Move Class org.antlr.v4.test.runtime.python2.TestLexerExec moved to org.antlr.v4.test.runtime.python2.TestLexerExec");

          processFp("https://github.com/petrnohejl/Android-Templates-And-Utilities.git", "cc73e7599c5012160cfd95de011506851ecd5993", 
            "Move Method public onOptionsItemSelected(item MenuItem) : boolean from class com.example.fragment.ListingFragment to public onOptionsItemSelected(item MenuItem) : boolean from class com.example.activity.ExampleActivity",
            "Move Method public onOptionsItemSelected(item MenuItem) : boolean from class com.example.fragment.SimpleFragment to public onOptionsItemSelected(item MenuItem) : boolean from class com.example.activity.ExampleActivity",
            "Move Method public onPause() : void from class com.example.fragment.SimpleFragment to public onPause() : void from class com.example.fragment.ExampleFragment",
            "Move Method private showOffline() : void from class com.example.fragment.ListingFragment to private showOffline() : void from class com.example.fragment.ExampleFragment",
            "Move Method private showOffline() : void from class com.example.fragment.SimpleFragment to private showOffline() : void from class com.example.fragment.ExampleFragment",
            "Move Method private showProgress() : void from class com.example.fragment.ListingFragment to private showProgress() : void from class com.example.fragment.ExampleFragment",
            "Move Method private showProgress() : void from class com.example.fragment.SimpleFragment to private showProgress() : void from class com.example.fragment.ExampleFragment",
            "Move Method private showContent() : void from class com.example.fragment.ListingFragment to private showContent() : void from class com.example.fragment.ExampleFragment",
            "Move Method public onPause() : void from class com.example.fragment.ListingFragment to public onPause() : void from class com.example.fragment.ExampleFragment",
            "Move Method public onDestroy() : void from class com.example.fragment.SimpleFragment to public onDestroy() : void from class com.example.fragment.ExampleFragment",
            "Move Method public onDestroy() : void from class com.example.fragment.ListingFragment to public onDestroy() : void from class com.example.fragment.ExampleFragment",
            "Move Method private showLazyLoadingProgress(visible boolean) : void from class com.example.fragment.ListingFragment to private showLazyLoadingProgress(visible boolean) : void from class com.example.fragment.ExampleFragment",
            "Move Method private showContent() : void from class com.example.fragment.SimpleFragment to private showContent() : void from class com.example.fragment.ExampleFragment",
            "Move Attribute private LAZY_LOADING_TAKE : int from class com.example.fragment.ListingFragment to class com.example.fragment.ExampleFragment",
            "Move Attribute private LAZY_LOADING_OFFSET : int from class com.example.fragment.ListingFragment to class com.example.fragment.ExampleFragment",
            "Move Attribute private mLazyLoading : boolean from class com.example.fragment.ListingFragment to class com.example.fragment.ExampleFragment",
            "Move Attribute private mViewState : ViewState from class com.example.fragment.SimpleFragment to class com.example.fragment.ExampleFragment",
            "Move Attribute private mViewState : ViewState from class com.example.fragment.ListingFragment to class com.example.fragment.ExampleFragment",
            "Move Attribute private mFooterView : View from class com.example.fragment.ListingFragment to class com.example.fragment.ExampleFragment",
            "Move Attribute private ARGUMENT_PRODUCT_ID : String from class com.example.fragment.ExampleFragment to class com.example.fragment.SimpleFragment");
          processTp("https://github.com/apache/giraph.git", "add1d4f07c925b8a9044cb3aa5bb4abdeaf49fc7", 
            "Extract Method private registerSerializer(kryo HadoopKryo, className String, serializer Serializer) : void extracted from private createKryo() : HadoopKryo in class org.apache.giraph.writable.kryo.HadoopKryo");

          processTp("https://github.com/phishman3579/java-algorithms-implementation.git", "ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9", 
            "Extract Method private runTest(testable Testable, unsorted Integer[], sorted Integer[], results double[], count int) : int extracted from public main(args String[]) : void in class com.jwetherell.algorithms.sorts.timing.SortsTiming");
          processFp("https://github.com/phishman3579/java-algorithms-implementation.git", "ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9", 
            "Inline Method private testBTree() : boolean inlined to private runTests() : boolean in class com.jwetherell.algorithms.data_structures.timing.DataStructuresTiming",
            "Inline Method private testAVLTree() : boolean inlined to private runTests() : boolean in class com.jwetherell.algorithms.data_structures.timing.DataStructuresTiming",
            "Extract Method private runTests(testable Testable, tests int, unsorteds Integer[][], sorteds Integer, strings String[]) : boolean extracted from private runTests() : boolean in class com.jwetherell.algorithms.data_structures.timing.DataStructuresTiming");
          processTp("https://github.com/gwtproject/gwt.git", "22fb2c9c6974bd1fe0f6ff684f52e6cfbed1a387", 
            "Extract Method private rescueMembersAndInstantiateSuperInterfaces(type JDeclaredType) : void extracted from public visit(type JInterfaceType, ctx Context) : boolean in class com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer.RescueVisitor");
          processFp("https://github.com/gwtproject/gwt.git", "22fb2c9c6974bd1fe0f6ff684f52e6cfbed1a387", 
            "Inline Method private rescueArgumentsIfParametersCanBeRead(call JMethodCall, method JMethod) : boolean inlined to public visit(call JMethodCall, ctx Context) : boolean in class com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer.RescueVisitor",
            "Extract Method private rescue(type JReferenceType, isInstantiated boolean) : void extracted from private rescue(type JReferenceType, isReferenced boolean, isInstantiated boolean) : void in class com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer.RescueVisitor",
            "Extract Method private maybeRescueJsTypeArray(type JType) : void extracted from private rescue(type JReferenceType, isReferenced boolean, isInstantiated boolean) : void in class com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer.RescueVisitor");
          processTp("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "abba5d83602c7ae23901bd579ba9fbb7dc36adc0", 
            "Extract Superclass org.broadleafcommerce.openadmin.dto.override.MetadataOverride from classes [org.broadleafcommerce.openadmin.dto.override.FieldMetadataOverride]");

          processTp("https://github.com/apache/drill.git", "00aa01fb90f3210d1e3027d7f759fb1085b814bd", 
            "Extract Method public setSessionOption(drillClient DrillClient, option String, value String) : void extracted from public setControls(drillClient DrillClient, controls String) : void in class org.apache.drill.exec.testing.ControlsInjectionUtil",
            "Inline Method private assertCancelled(controls String, testQuery String, listener WaitUntilCompleteListener) : void inlined to private assertCancelledWithoutException(controls String, listener WaitUntilCompleteListener, query String) : void in class org.apache.drill.exec.server.TestDrillbitResilience");

          processTp("https://github.com/aws/aws-sdk-java.git", "4baf0a4de8d03022df48d696d210cc8b3117d38a", 
            "Extract Method private pause(delay long) : void extracted from private pauseExponentially(retries int) : void in class com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper",
            "Inline Method private killServer() : void inlined to public cleanUp() : void in class com.amazonaws.util.EC2MetadataUtilsTest");

          processTp("https://github.com/apache/drill.git", "3f0d9221d3f96c20db10e868cc33c2e972318ba6", 
            "Extract Method public createNewWrapperCurrent(batchRecordCount int) : SelectionVector4 extracted from public createNewWrapperCurrent() : SelectionVector4 in class org.apache.drill.exec.record.selection.SelectionVector4");

          processTp("https://github.com/mockito/mockito.git", "7f20e63a7252f33c888085134d16ee8bf45f183f", 
            "Move Class org.mockito.internal.util.text.ValuePrinter moved to org.mockito.internal.matchers.text.ValuePrinter",
            "Move Class org.mockito.internal.util.text.HamcrestPrinter moved to org.mockito.internal.matchers.text.HamcrestPrinter",
            "Move Class org.mockito.internal.util.text.ArrayIterator moved to org.mockito.internal.matchers.text.ArrayIterator",
            "Move Class org.mockito.internal.matchers.MatchersPrinter moved to org.mockito.internal.matchers.text.MatchersPrinter",
            "Extract Superclass org.mockito.MockitoMatcher from classes [org.mockito.internal.matchers.LocalizedMatcher]");

          processTp("https://github.com/libgdx/libgdx.git", "2bd1557bc293cb8c2348374771aad832befbe26f", 
            "Inline Method public setCheckBoxRight(right boolean) : void inlined to public CheckBox(text String, style CheckBoxStyle) in class com.badlogic.gdx.scenes.scene2d.ui.CheckBox");

          processTp("https://github.com/SonarSource/sonarqube.git", "0d9fcaa4415ee996e423a97cfe0d965586ca59a5", 
            "Extract Method private doStop(swallowException boolean) : void extracted from public stop() : void in class org.sonar.batch.bootstrapper.Batch");

          processTp("https://github.com/apache/drill.git", "c1b847acdc8cb90a1498b236b3bb5c81ca75c044", 
            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.TableNameLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.TableNameLoader",
            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.DatabaseLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.DatabaseLoader",
            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.TableLoaderLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.TableLoaderLoader",
            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.TableLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.TableLoader",
            "Pull Up Attribute protected MINIDFS_STORAGE_PLUGIN_NAME : String from class org.apache.drill.exec.impersonation.TestImpersonationDisabledWithMiniDFS to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
            "Pull Up Attribute protected MINIDFS_STORAGE_PLUGIN_NAME : String from class org.apache.drill.exec.impersonation.TestImpersonationMetadata to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
            "Pull Up Attribute protected MINIDFS_STORAGE_PLUGIN_NAME : String from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
            "Pull Up Attribute protected org1Users : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
            "Pull Up Attribute protected org1Groups : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
            "Pull Up Attribute protected org2Users : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
            "Pull Up Attribute protected org2Groups : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
            "Extract Method private createTestData() : void extracted from public addMiniDfsBasedStorageAndGenerateTestData() : void in class org.apache.drill.exec.impersonation.TestImpersonationQueries");
          processFp("https://github.com/apache/drill.git", "c1b847acdc8cb90a1498b236b3bb5c81ca75c044", 
            "Extract Method private createTestData() : void extracted from public addMiniDfsBasedStorage() : void in class org.apache.drill.exec.impersonation.TestImpersonationDisabledWithMiniDFS");

          processFp("https://github.com/google/j2objc.git", "45cafde754d9f99a74f3ae723233e351d0f78642", 
            "Move Method public visit(node LambdaExpression) : boolean from class com.google.devtools.j2objc.translate.Rewriter to public visit(node LambdaExpression) : boolean from class com.google.devtools.j2objc.translate.OuterReferenceResolver");
          processTp("https://github.com/facebook/buck.git", "be292763b8c4cb09988023d6081b0a2d57b4c778", 
            "Move Attribute private PATH_TO_URL : Function<Path,URL> from class com.facebook.buck.util.ClassLoaderCacheTest to class com.facebook.buck.java.JarBackedJavac",
            "Move Attribute private PATH_TO_URL : Function<Path,URL> from class com.facebook.buck.util.ClassLoaderCache to class com.facebook.buck.java.JarBackedJavac");

          processTp("https://github.com/facebook/facebook-android-sdk.git", "e813a0be86c87366157a0201e6c61662cadee586", 
            "Extract Method private getAndroidIdViaReflection(context Context) : AttributionIdentifiers extracted from private getAndroidId(context Context) : AttributionIdentifiers in class com.facebook.internal.AttributionIdentifiers",
            "Move Class com.facebook.samples.switchuser.UserInfoCache moved to com.example.switchuser.UserInfoCache",
            "Move Class com.facebook.samples.switchuser.UserInfo moved to com.example.switchuser.UserInfo",
            "Move Class com.facebook.samples.switchuser.Slot moved to com.example.switchuser.Slot",
            "Move Class com.facebook.samples.switchuser.SettingsFragment moved to com.example.switchuser.SettingsFragment",
            "Move Class com.facebook.samples.switchuser.ProfileFragment moved to com.example.switchuser.ProfileFragment",
            "Move Class com.facebook.samples.switchuser.MainActivity moved to com.example.switchuser.MainActivity",
            "Move Class com.facebook.scrumptious.usersettings.UserSettingsFragment moved to com.example.scrumptious.usersettings.UserSettingsFragment",
            "Move Class com.facebook.scrumptious.BaseListElement moved to com.example.scrumptious.BaseListElement",
            "Move Class com.facebook.samples.rps.usersettings.UserSettingsFragment moved to com.example.rps.usersettings.UserSettingsFragment",
            "Move Class com.facebook.samples.rps.RpsGameUtils moved to com.example.rps.RpsGameUtils",
            "Move Class com.facebook.samples.rps.RpsFragment moved to com.example.rps.RpsFragment",
            "Move Class com.facebook.samples.rps.OpenGraphConsts moved to com.example.rps.OpenGraphConsts",
            "Move Class com.facebook.samples.rps.MainActivity moved to com.example.rps.MainActivity",
            "Move Class com.facebook.samples.rps.ContentFragment moved to com.example.rps.ContentFragment",
            "Move Class com.facebook.samples.rps.CommonObjects moved to com.example.rps.CommonObjects",
            "Move Class com.facebook.iconicus.GameController moved to com.example.iconicus.GameController",
            "Move Class com.facebook.iconicus.GameBoard moved to com.example.iconicus.GameBoard",
            "Move Class com.facebook.samples.hellofacebook.HelloFacebookSampleActivity moved to com.example.hellofacebook.HelloFacebookSampleActivity",
            "Move Class com.facebook.samples.hellofacebook.HelloFacebookBroadcastReceiver moved to com.example.hellofacebook.HelloFacebookBroadcastReceiver",
            "Move Class com.facebook.scrumptious.FullListView moved to com.example.scrumptious.FullListView",
            "Move Class com.facebook.scrumptious.MainActivity moved to com.example.scrumptious.MainActivity",
            "Move Class com.facebook.scrumptious.PickerActivity moved to com.example.scrumptious.PickerActivity",
            "Move Class com.facebook.scrumptious.ScrumptiousApplication moved to com.example.scrumptious.ScrumptiousApplication",
            "Move Class com.facebook.scrumptious.SelectionFragment moved to com.example.scrumptious.SelectionFragment",
            "Move Class com.facebook.scrumptious.SplashFragment moved to com.example.scrumptious.SplashFragment",
            "Move Class com.facebook.scrumptious.picker.FriendPickerFragment moved to com.example.scrumptious.picker.FriendPickerFragment",
            "Move Class com.facebook.scrumptious.picker.GraphObjectAdapter moved to com.example.scrumptious.picker.GraphObjectAdapter",
            "Move Class com.facebook.scrumptious.picker.GraphObjectCursor moved to com.example.scrumptious.picker.GraphObjectCursor",
            "Move Class com.facebook.scrumptious.picker.GraphObjectPagingLoader moved to com.example.scrumptious.picker.GraphObjectPagingLoader",
            "Move Class com.facebook.scrumptious.picker.PickerFragment moved to com.example.scrumptious.picker.PickerFragment",
            "Move Class com.facebook.scrumptious.picker.PlacePickerFragment moved to com.example.scrumptious.picker.PlacePickerFragment");
          processFp("https://github.com/facebook/facebook-android-sdk.git", "e813a0be86c87366157a0201e6c61662cadee586", 
            "Extract Method private getAndroidIdViaService(context Context) : AttributionIdentifiers extracted from private getAndroidId(context Context) : AttributionIdentifiers in class com.facebook.internal.AttributionIdentifiers");
          processTp("https://github.com/fabric8io/fabric8.git", "e068eb7f484f24dee285d29b8a910d9019592020", 
            "Extract Method private getHTTPGetAction(prefix String, properties Properties) : HTTPGetAction extracted from protected getProbe(prefix String) : Probe in class io.fabric8.maven.JsonMojo");


          processFp("https://github.com/crate/crate.git", "f9c7870eb2070bf0eb7d362b89c9e8a92a71ac43", 
            "Inline Method private getRouting(observer ClusterStateObserver, whereClause WhereClause, preference String, currentRetry int, retry boolean) : Routing inlined to public getRouting(whereClause WhereClause, preference String) : Routing in class io.crate.metadata.doc.DocTableInfo");

          processFp("https://github.com/hazelcast/hazelcast.git", "0c74a45e5561953be5bbc962b212b1baa5d13445", 
            "Extract Method private executeLocal(serviceName String, event Object, registration EventRegistration, orderKey int) : void extracted from public publishEvent(serviceName String, registrations Collection<EventRegistration>, event Object, orderKey int) : void in class com.hazelcast.spi.impl.eventservice.impl.EventServiceImpl");

          processFp("https://github.com/gradle/gradle.git", "276d5b7cc419b3d6e3fc7bc4381e763e4df25fda", 
            "Move Class org.gradle.model.internal.core.DomainObjectSetBackedModelMap.ToName moved to org.gradle.model.internal.core.DomainObjectCollectionBackedModelMap.ToName",
            "Move Class org.gradle.model.internal.core.DomainObjectSetBackedModelMap.HasNamePredicate moved to org.gradle.model.internal.core.DomainObjectCollectionBackedModelMap.HasNamePredicate");
          processTp("https://github.com/killbill/killbill.git", "4b5b74b6467a28fb9b7712f8091e4aa61c2d64b6", 
            "Push Down Attribute private pluginControlledPaymentAutomatonRunner : PluginRoutingPaymentAutomatonRunner from class org.killbill.billing.payment.core.janitor.CompletionTaskBase to class org.killbill.billing.payment.core.janitor.IncompletePaymentAttemptTask",
            "Extract Method public updatePaymentAndTransactionIfNeeded(payment PaymentModelDao, paymentTransaction PaymentTransactionModelDao, paymentTransactionInfoPlugin PaymentTransactionInfoPlugin, internalTenantContext InternalTenantContext) : boolean extracted from public doIteration(paymentTransaction PaymentTransactionModelDao) : void in class org.killbill.billing.payment.core.janitor.IncompletePaymentTransactionTask");

          processTp("https://github.com/redsolution/xabber-android.git", "faaf826e901f43d1b46105b18e655eb120f3ffef", 
            "Extract Interface com.xabber.android.ui.ContactAdder from classes [com.xabber.android.ui.ContactAddFragment]");

          processTp("https://github.com/deeplearning4j/deeplearning4j.git", "d4992887291cc0a7eeda87ad547fa9e1e7fda41c", 
            "Extract Method public output(x INDArray, test boolean) : INDArray extracted from public output(x INDArray) : INDArray in class org.deeplearning4j.nn.layers.OutputLayer");

          processTp("https://github.com/cbeust/testng.git", "b5cf7a0252c8b0465c4dbd906717f7a12e26e6f8", 
            "Move Class test.testng234.PolymorphicFailureTest moved to test.inheritance.testng234.PolymorphicFailureTest",
            "Move Class test.testng234.ParentTest moved to test.inheritance.testng234.ParentTest",
            "Move Class test.testng234.ChildTest moved to test.inheritance.testng234.ChildTest");

          processTp("https://github.com/JetBrains/intellij-community.git", "8d7a26edd1fedb9505b4f2b4fe57b2d2958b4dd9", 
            "Inline Method private writeContentToFile(revision byte[]) : void inlined to private write(revision byte[]) : void in class com.intellij.openapi.vcs.history.FileHistoryPanelImpl.MyGetVersionAction");

          processTp("https://github.com/AntennaPod/AntennaPod.git", "c64217e2b485f3c6b997a55b1ef910c8b72779d3", 
            "Extract Method public addQueueItem(context Context, performAutoDownload boolean, itemIds long[]) : Future<?> extracted from public addQueueItem(context Context, itemIds long[]) : Future<?> in class de.danoeh.antennapod.core.storage.DBWriter");

          processTp("https://github.com/datastax/java-driver.git", "9de5f0d408f861455716b8410fd53f62b360787d", 
            "Extract Method protected query(session Session) : ResultSet extracted from protected query() : ResultSet in class com.datastax.driver.core.policies.AbstractRetryPolicyIntegrationTest",
            "Extract Method private retry(retryCurrent boolean, newConsistencyLevel ConsistencyLevel, connection Connection, response Response) : void extracted from private retry(retryCurrent boolean, newConsistencyLevel ConsistencyLevel) : void in class com.datastax.driver.core.RequestHandler.SpeculativeExecution",
            "Extract Method package sendRequest(reportNoMoreHosts boolean) : boolean extracted from package sendRequest() : void in class com.datastax.driver.core.RequestHandler.SpeculativeExecution");

          processTp("https://github.com/apache/hive.git", "7eb3567e7880511b76b8b65e8eb7d373927f2fb6", 
            "Extract Method private unionTester(ws Schema, rs Schema, record Record) : ResultPair extracted from private unionTester(s Schema, record Record) : ResultPair in class org.apache.hadoop.hive.serde2.avro.TestAvroDeserializer");


          processFp("https://github.com/ratpack/ratpack.git", "23986bbc3e94ce5d77f772be721f9512c258e476", 
            "Move Class ratpack.jackson.internal.JsonRenderer moved to ratpack.jackson.internal.JsonRenderer",
            "Move Class ratpack.jackson.internal.JsonParser moved to ratpack.jackson.internal.JsonParser",
            "Move Class ratpack.jackson.internal.JsonNoOptParser moved to ratpack.jackson.internal.JsonNoOptParser",
            "Move Class ratpack.jackson.internal.DefaultJsonRender moved to ratpack.jackson.internal.DefaultJsonRender",
            "Move Class ratpack.jackson.internal.DefaultJsonParseOpts moved to ratpack.jackson.internal.DefaultJsonParseOpts",
            "Move Class ratpack.jackson.JsonRender moved to ratpack.jackson.JsonRender",
            "Move Class ratpack.jackson.JsonParseOpts moved to ratpack.jackson.JsonParseOpts",
            "Move Class ratpack.jackson.Jackson moved to ratpack.jackson.Jackson");

          processFp("https://github.com/greenrobot/greenDAO.git", "f935cc5f17d1e47c8597ee09161d524c80c64125", 
            "Move Class de.greenrobot.daogenerator.gentest.TestDaoGenerator moved to de.greenrobot.daogenerator.gentest.TestDaoGenerator");
          processTp("https://github.com/ratpack/ratpack.git", "2581441eda268c45306423dd4c515514d98a14a0", 
            "Move Class ratpack.jackson.JacksonModule moved to ratpack.jackson.guice.JacksonModule");


          processFp("https://github.com/VoltDB/voltdb.git", "7792be981681432dec07640ab316995485bf07ee", 
            "Move Class org.voltdb.sqlparser.symtab.ValueAssert moved to org.voltdb.sqlparser.symtab.ValueAssert",
            "Move Class org.voltdb.sqlparser.symtab.TypeAssert moved to org.voltdb.sqlparser.symtab.TypeAssert",
            "Move Class org.voltdb.sqlparser.symtab.TopAssert moved to org.voltdb.sqlparser.symtab.TopAssert",
            "Move Class org.voltdb.sqlparser.symtab.TableAssert moved to org.voltdb.sqlparser.symtab.TableAssert",
            "Move Class org.voltdb.sqlparser.symtab.SymbolTableAssert moved to org.voltdb.sqlparser.symtab.SymbolTableAssert",
            "Move Class org.voltdb.sqlparser.symtab.ColumnAssert moved to org.voltdb.sqlparser.symtab.ColumnAssert",
            "Move Class org.voltdb.sqlparser.symtab.CatalogAdapterAssert moved to org.voltdb.sqlparser.symtab.CatalogAdapterAssert",
            "Move Class org.voltdb.sqlparser.matchers.TypeAssert moved to org.voltdb.sqlparser.matchers.TypeAssert",
            "Move Class org.voltdb.sqlparser.matchers.SymbolTableAssert moved to org.voltdb.sqlparser.matchers.SymbolTableAssert",
            "Move Class org.voltdb.sqlparser.grammar.SelectQueryAssert moved to org.voltdb.sqlparser.grammar.SelectQueryAssert",
            "Move Class org.voltdb.sqlparser.grammar.ProjectionAssert moved to org.voltdb.sqlparser.grammar.ProjectionAssert");
          processTp("https://github.com/apache/zookeeper.git", "3fd77b419673ce6ec41e06cdc27558b1d8f4ca06", 
            "Pull Up Method private cleanupWriterSocket(pwriter PrintWriter) : void from class org.apache.zookeeper.server.NettyServerCnxn to public cleanupWriterSocket(pwriter PrintWriter) : void from class org.apache.zookeeper.server.ServerCnxn",
            "Pull Up Method private cleanupWriterSocket(pwriter PrintWriter) : void from class org.apache.zookeeper.server.NIOServerCnxn to public cleanupWriterSocket(pwriter PrintWriter) : void from class org.apache.zookeeper.server.ServerCnxn");


          processFp("https://github.com/siacs/Conversations.git", "6e9be182b17e50d545372a402e977b1740ce2e29", 
            "Extract Method private sendFileMessage(message Message) : void extracted from private resendMessage(message Message) : void in class eu.siacs.conversations.services.XmppConnectionService",
            "Extract Method private sendFileMessage(message Message) : void extracted from public sendMessage(message Message) : void in class eu.siacs.conversations.services.XmppConnectionService");
          processTp("https://github.com/QuantumBadger/RedReader.git", "51b8b0e1ad4be1b137d67774eab28dc0ef52cb0a", 
            "Pull Up Method public onSharedPreferenceChanged(prefs SharedPreferences, key String) : void from class org.quantumbadger.redreader.activities.MainActivity to protected onSharedPreferenceChangedInner(prefs SharedPreferences, key String) : void from class org.quantumbadger.redreader.activities.RefreshableActivity");
          processFp("https://github.com/QuantumBadger/RedReader.git", "51b8b0e1ad4be1b137d67774eab28dc0ef52cb0a", 
            "Move Method protected onDestroy() : void from class org.quantumbadger.redreader.activities.PostListingActivity to protected onDestroy() : void from class org.quantumbadger.redreader.activities.BaseActivity",
            "Move Method protected onDestroy() : void from class org.quantumbadger.redreader.activities.MoreCommentsListingActivity to protected onDestroy() : void from class org.quantumbadger.redreader.activities.BaseActivity",
            "Move Method protected onDestroy() : void from class org.quantumbadger.redreader.activities.MainActivity to protected onDestroy() : void from class org.quantumbadger.redreader.activities.BaseActivity",
            "Move Method protected onDestroy() : void from class org.quantumbadger.redreader.activities.CommentListingActivity to protected onDestroy() : void from class org.quantumbadger.redreader.activities.BaseActivity");
          processTp("https://github.com/VoltDB/voltdb.git", "e58c9c3eef4c6e44b21a97cfbd2862bb2eb4627a", 
            "Extract Method public hasSize(size int) : SymbolTableAssert extracted from public isEmpty() : SymbolTableAssert in class org.voltdb.sqlparser.symtab.SymbolTableAssert");

          processTp("https://github.com/koush/AndroidAsync.git", "1bc7905b07821f840068089343e6b77a8686d1ab", 
            "Extract Method private terminate() : void extracted from protected report(e Exception) : void in class com.koushikdutta.async.http.AsyncHttpResponseImpl");

          processTp("https://github.com/gradle/gradle.git", "0e7345a9c10863dca9217ad902b825db50fed01f", 
            "Extract Method private getConfiguration() : Configuration extracted from package getFileCollection() : FileCollection in class org.gradle.play.plugins.PlayPluginConfigurations.PlayConfiguration");

          processTp("https://github.com/apache/hive.git", "102b23b16bf26cbf439009b4b95542490a082710", 
            "Move Method private isSourceCMD(cmd String) : boolean from class org.apache.hive.beeline.BeeLine to private isSourceCMD(cmd String) : boolean from class org.apache.hive.beeline.Commands",
            "Move Method private getFirstCmd(cmd String, length int) : String from class org.apache.hive.beeline.BeeLine to private getFirstCmd(cmd String, length int) : String from class org.apache.hive.beeline.Commands",
            "Extract Method public handleMultiLineCmd(line String) : String extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands",
            "Extract Method private executeInternal(sql String, call boolean) : boolean extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands");
          processFp("https://github.com/apache/hive.git", "102b23b16bf26cbf439009b4b95542490a082710", 
            "Extract Method private sourceFile(cmd String) : boolean extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands");
          processTp("https://github.com/gradle/gradle.git", "79c66ceab11dae0b9fd1dade7bb4120028738705", 
            "Extract Method public getInputs() : Set<LanguageSourceSet> extracted from public getAllSources() : Set<LanguageSourceSet> in class org.gradle.platform.base.binary.BaseBinarySpec");


          processFp("https://github.com/gradle/gradle.git", "c56a3ca0c581f8653a3ebe38f463878f26813b37", 
            "Move Class org.gradle.model.internal.core.DomainObjectSetBackedModelMap.ToName moved to org.gradle.model.internal.core.DomainObjectCollectionBackedModelMap.ToName",
            "Move Class org.gradle.model.internal.core.DomainObjectSetBackedModelMap.HasNamePredicate moved to org.gradle.model.internal.core.DomainObjectCollectionBackedModelMap.HasNamePredicate");

          processFp("https://github.com/JetBrains/intellij-community.git", "deb0741b48f9945ff067eac07118fe2b4463ad2e", 
            "Move Class com.intellij.openapi.roots.impl.PackageDirectoryCache moved to com.intellij.openapi.roots.impl.PackageDirectoryCache");
          processTp("https://github.com/spring-projects/spring-data-jpa.git", "c13f3469e1d64ec97b11f0509e45f9c3fa8ff88a", 
            "Extract Method protected getCountQuery(spec Specification<T>, mode QueryMode) : TypedQuery<Long> extracted from protected getCountQuery(spec Specification<T>) : TypedQuery<Long> in class org.springframework.data.jpa.repository.support.SimpleJpaRepository");

          processTp("https://github.com/facebook/presto.git", "364f50274d4b4b83d40930c0d2c4d0e57fb34589", 
            "Extract Superclass com.facebook.presto.PartitionedPagePartitionFunction from classes [com.facebook.presto.HashPagePartitionFunction]");

          processTp("https://github.com/apache/hive.git", "d69e5cb21c04d9eede314aaa9ad059fc603fb025", 
            "Move Method public setTag(tag byte) : void from class org.apache.hadoop.hive.ql.exec.SparkHashTableSinkOperator to public setTag(tag byte) : void from class org.apache.hadoop.hive.ql.plan.SparkHashTableSinkDesc",
            "Move Attribute private tag : byte from class org.apache.hadoop.hive.ql.exec.SparkHashTableSinkOperator to class org.apache.hadoop.hive.ql.plan.SparkHashTableSinkDesc");

          processTp("https://github.com/spring-projects/spring-boot.git", "1e464da2480568014a87dd0bac6febe63a76c889", 
            "Inline Method private addStaticIndexHtmlViewControllers(registry ViewControllerRegistry) : void inlined to public addViewControllers(registry ViewControllerRegistry) : void in class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter",
            "Move Method public setResourceLoader(resourceLoader ResourceLoader) : void from class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.FaviconConfiguration to public setResourceLoader(resourceLoader ResourceLoader) : void from class org.springframework.boot.autoconfigure.web.ResourceProperties",
            "Move Attribute private resourceLoader : ResourceLoader from class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter.FaviconConfiguration to class org.springframework.boot.autoconfigure.web.ResourceProperties",
            "Move Attribute private STATIC_INDEX_HTML_RESOURCES : String[] from class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration to class org.springframework.boot.autoconfigure.web.ResourceProperties",
            "Move Attribute private RESOURCE_LOCATIONS : String[] from class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration to class org.springframework.boot.autoconfigure.web.ResourceProperties",
            "Move Attribute private CLASSPATH_RESOURCE_LOCATIONS : String[] from class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration to class org.springframework.boot.autoconfigure.web.ResourceProperties",
            "Move Attribute private SERVLET_RESOURCE_LOCATIONS : String[] from class org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration to class org.springframework.boot.autoconfigure.web.ResourceProperties");
          processFp("https://github.com/spring-projects/spring-boot.git", "1e464da2480568014a87dd0bac6febe63a76c889", 
            "Move Attribute private managementServerProperties : ManagementServerProperties from class org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration to class org.springframework.boot.actuate.autoconfigure.EndpointWebMvcChildContextConfiguration");
          processTp("https://github.com/droolsjbpm/jbpm.git", "a739d16d301f0e89ab0b9dfa56b4585bbad6b793", 
            "Extract Method private createUser(id String) : User extracted from public testCompleteWithComments() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testActivateFromIncorrectStatus() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testNominateToUser() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testNominateWithIncorrectUser() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testNominateOnOtherThanCreated() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testRemoveNotInRecipientList() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testForwardFromReservedWithIncorrectUser() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testForwardFromReserved() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testDelegateFromReservedWithIncorrectUser() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testDelegateFromReserved() : void in class org.jbpm.services.task.LifeCycleBaseTest",
            "Extract Method private createUser(id String) : User extracted from public testDelegateFromReady() : void in class org.jbpm.services.task.LifeCycleBaseTest");

          processTp("https://github.com/droolsjbpm/jbpm.git", "83cfa21578e63956bca0715eefee2860c3b6d39a", 
            "Extract Method private testTaskWithExpectedDescription(task Task, expectedDescription String) : void extracted from public testTaskWithVariables() : void in class org.jbpm.services.task.wih.HTWorkItemHandlerBaseTest",
            "Extract Method private prepareWorkItemWithTaskVariables(taskDescriptionParam String) : WorkItemImpl extracted from public testTaskWithVariables() : void in class org.jbpm.services.task.wih.HTWorkItemHandlerBaseTest");

          processTp("https://github.com/Graylog2/graylog2-server.git", "f05e86c4d31987ff2f30330745c3eb605de4c4dc", 
            "Move Attribute package COMPARATOR : Comparator<IndexRange> from class org.graylog2.indexer.ranges.MongoIndexRangeService to class org.graylog2.indexer.ranges.IndexRange");


          processFp("https://github.com/Graylog2/graylog2-server.git", "904f8e2a49f8ded1b16ab52e37588592e02da71c", 
            "Inline Method private updateCollection(ranges List<IndexRange>) : void inlined to public execute() : void in class org.graylog2.indexer.ranges.RebuildIndexRangesJob");
          processTp("https://github.com/Graylog2/graylog2-server.git", "767171c90110c4c5781e8f6d19ece1fba0d492e9", 
            "Move Method public testTimestampStatsOfIndexWithNonExistingIndex() : void from class org.graylog2.indexer.searches.SearchesTest to public testTimestampStatsOfIndexWithNonExistingIndex() : void from class org.graylog2.indexer.ranges.EsIndexRangeServiceTest",
            "Move Method public testTimestampStatsOfIndexWithEmptyIndex() : void from class org.graylog2.indexer.searches.SearchesTest to public testTimestampStatsOfIndexWithEmptyIndex() : void from class org.graylog2.indexer.ranges.EsIndexRangeServiceTest",
            "Move Method public testTimestampStatsOfIndex() : void from class org.graylog2.indexer.searches.SearchesTest to public testTimestampStatsOfIndex() : void from class org.graylog2.indexer.ranges.EsIndexRangeServiceTest",
            "Move Method public timestampStatsOfIndex(index String) : TimestampStats from class org.graylog2.indexer.searches.Searches to protected timestampStatsOfIndex(index String) : TimestampStats from class org.graylog2.indexer.ranges.EsIndexRangeService");


          processFp("https://github.com/JetBrains/intellij-community.git", "87fe269ef2d531e1d1288aae62a9cd1d8ec713d7", 
            "Extract Interface org.jetbrains.jps.builders.ModuleInducedTargetType from classes [org.jetbrains.jps.builders.ModuleBasedBuildTargetType]");
          processTp("https://github.com/apache/cassandra.git", "35668435090eb47cf8c5e704243510b6cee35a7b", 
            "Extract Method private onDropFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onDropAggregate(ksName String, aggregateName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber",
            "Extract Method private onDropFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onDropFunction(ksName String, functionName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber",
            "Extract Method private onCreateFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onCreateAggregate(ksName String, aggregateName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber",
            "Extract Method private onCreateFunctionInternal(ksName String, functionName String, argTypes List<AbstractType<?>>) : void extracted from public onCreateFunction(ksName String, functionName String, argTypes List<AbstractType<?>>) : void in class org.apache.cassandra.cql3.QueryProcessor.MigrationSubscriber");

          processTp("https://github.com/gradle/gradle.git", "527ac38334000e105daacb2aca25cb345d77c01e", 
            "Move Attribute private stopped : AtomicBoolean from class org.gradle.play.internal.run.PlayApplicationDeploymentHandle to class org.gradle.play.internal.run.PlayApplicationRunnerToken");

          processTp("https://github.com/Activiti/Activiti.git", "53036cece662f9c796d2a187b0077059c3d9088a", 
            "Pull Up Attribute protected notExclusive : boolean from class org.activiti.bpmn.model.Gateway to class org.activiti.bpmn.model.FlowNode",
            "Pull Up Attribute protected notExclusive : boolean from class org.activiti.bpmn.model.Activity to class org.activiti.bpmn.model.FlowNode",
            "Pull Up Attribute protected asynchronous : boolean from class org.activiti.bpmn.model.Gateway to class org.activiti.bpmn.model.FlowNode",
            "Pull Up Attribute protected asynchronous : boolean from class org.activiti.bpmn.model.Activity to class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.Gateway to public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.Activity to public setNotExclusive(notExclusive boolean) : void from class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public isNotExclusive() : boolean from class org.activiti.bpmn.model.Gateway to public isNotExclusive() : boolean from class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public isNotExclusive() : boolean from class org.activiti.bpmn.model.Activity to public isNotExclusive() : boolean from class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.Gateway to public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.Activity to public setAsynchronous(asynchronous boolean) : void from class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public isAsynchronous() : boolean from class org.activiti.bpmn.model.Gateway to public isAsynchronous() : boolean from class org.activiti.bpmn.model.FlowNode",
            "Pull Up Method public isAsynchronous() : boolean from class org.activiti.bpmn.model.Activity to public isAsynchronous() : boolean from class org.activiti.bpmn.model.FlowNode");

          processTp("https://github.com/FasterXML/jackson-databind.git", "cfe88fe3fbcc6b02ca55cee7b1f4ab13e249edea", 
            "Extract Method protected classForName(name String) : Class<?> extracted from public findClass(className String) : Class<?> in class com.fasterxml.jackson.databind.type.TypeFactory",
            "Extract Method protected classForName(name String, initialize boolean, loader ClassLoader) : Class<?> extracted from public findClass(className String) : Class<?> in class com.fasterxml.jackson.databind.type.TypeFactory");


          processFp("https://github.com/JetBrains/intellij-community.git", "b8253a915e372b7cac5deba76127c5ae32c03ac6", 
            "Move Class net.sf.cglib.core.AbstractClassGenerator moved to net.sf.cglib.core.AbstractClassGenerator",
            "Move Class com.intellij.util.InstanceofCheckerGenerator moved to com.intellij.util.InstanceofCheckerGenerator");

          processFp("https://github.com/wordpress-mobile/WordPress-Android.git", "e9882c60cf234a2fe5592d9ba13d6f6b2c289741", 
            "Move Class org.wordpress.android.models.NotificationSetting.WordPressCom moved to org.wordpress.android.models.NotificationsSettings.WordPressCom",
            "Move Class org.wordpress.android.models.NotificationSetting.Other moved to org.wordpress.android.models.NotificationsSettings.Other",
            "Move Class org.wordpress.android.models.NotificationSetting.Site moved to org.wordpress.android.models.NotificationsSettings.Site");
          processTp("https://github.com/wordpress-mobile/WordPress-Android.git", "9dc3cbd59a20f03406f295a4a8f3b8676dbc939e", 
            "Move Class org.wordpress.android.ui.prefs.NotificationsSettingsFragment moved to org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment",
            "Move Class org.wordpress.android.ui.prefs.NotificationsSettingsActivity moved to org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity",
            "Move Class org.wordpress.android.ui.prefs.NotificationsPreference moved to org.wordpress.android.ui.prefs.notifications.NotificationsPreference");

          processTp("https://github.com/rstudio/rstudio.git", "229d1b60c03a3f8375451c68a6911660a3993777", 
            "Move Method public checkForExistingApp(account RSConnectAccount, appName String, onValidated OperationWithInput<Boolean>) : void from class org.rstudio.studio.client.rsconnect.RSConnect to private checkForExistingApp(account RSConnectAccount, appName String, onValidated OperationWithInput<Boolean>) : void from class org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy",
            "Inline Method private fireValidatedRSconnectPublish(result RSConnectPublishResult, launchBrowser boolean) : void inlined to public fireRSConnectPublishEvent(result RSConnectPublishResult, launchBrowser boolean) : void in class org.rstudio.studio.client.rsconnect.RSConnect",
            "Extract Method private isUpdate() : boolean extracted from public getResult() : RSConnectPublishResult in class org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy");


          processFp("https://github.com/Atmosphere/atmosphere.git", "7ab0e6becfc4acf27783092945eb67d9fdb0474d", 
            "Inline Method protected needInjection() : void inlined to public configure(config AtmosphereConfig) : void in class org.atmosphere.config.managed.ServiceInterceptor");

          processFp("https://github.com/neo4j/neo4j.git", "373ee8f7b7e2aa6684ca44e08f23f0995e68aeb4", 
            "Move Class org.neo4j.kernel.ha.UniqueConstraintStressIT.Operation moved to org.neo4j.kernel.ha.PropertyConstraintsStressIT.Operation");
          processTp("https://github.com/neo4j/neo4j.git", "8d9bedbf96b14beb027ebc1338bc6d5750e1feb5", 
            "Extract Superclass org.neo4j.kernel.api.constraints.PropertyConstraint from classes [org.neo4j.kernel.api.constraints.UniquenessConstraint]");

          processTp("https://github.com/neo4j/neo4j.git", "d1a6ae2a16ba1d53b1de02eea8745d67c6a1a005", 
            "Extract Method private fileSelection() : File extracted from public actionPerformed(e ActionEvent) : void in class org.neo4j.desktop.ui.BrowseForDatabaseActionListener");


          processFp("https://github.com/crate/crate.git", "43aee5ef2bbe8a6a8091a775a58345143f17edd6", 
            "Move Class io.crate.planner.node.ExecutionNode.ExecutionNodeFactory moved to io.crate.planner.node.ExecutionPhase.ExecutionNodeFactory");

          processFp("https://github.com/JetBrains/intellij-community.git", "526af6f1c1206b7f278459757f4b4b22b18069ea", 
            "Inline Method private convertToJavaLambda(expression PsiExpression, streamApiMethodName String) : PsiExpression inlined to public convertToStream(expression PsiMethodCallExpression, method PsiMethod, force boolean) : PsiExpression in class com.intellij.codeInspection.java18StreamApi.PseudoLambdaReplaceTemplate");
          processTp("https://github.com/katzer/cordova-plugin-local-notifications.git", "51f498a96b2fa1822e392027982c20e950535fd1", 
            "Extract Method public handleEndTag(xml XmlPullParser) : void extracted from public parse(xml XmlResourceParser) : void in class org.apache.cordova.ConfigXmlParser",
            "Extract Method public handleStartTag(xml XmlPullParser) : void extracted from public parse(xml XmlResourceParser) : void in class org.apache.cordova.ConfigXmlParser");
          processFp("https://github.com/katzer/cordova-plugin-local-notifications.git", "51f498a96b2fa1822e392027982c20e950535fd1", 
            "Move Attribute private cordova : CordovaInterface from class org.apache.cordova.NativeToJsMessageQueue to class org.apache.cordova.NativeToJsMessageQueue.LoadUrlBridgeMode",
            "Move Attribute private cordova : CordovaInterface from class org.apache.cordova.CordovaWebView to class org.apache.cordova.NativeToJsMessageQueue.LoadUrlBridgeMode");

          processFp("https://github.com/apache/hive.git", "cdd1c7bf775d788fc94eee6d33e8d630158195f1", 
            "Move Method public setIndex(index Index) : void from class org.apache.hadoop.hive.ql.session.LineageState to public setIndex(depMap Index) : void from class org.apache.hadoop.hive.ql.hooks.HookContext");

          processFp("https://github.com/apache/hive.git", "0dece6f37d43ae6ecbd0ad496ab18bcdd505a395", 
            "Move Method public setIndex(index Index) : void from class org.apache.hadoop.hive.ql.session.LineageState to public setIndex(depMap Index) : void from class org.apache.hadoop.hive.ql.hooks.HookContext");
          processTp("https://github.com/JetBrains/MPS.git", "fe6653db5fb9f1a25d5ee30e4d5d54ccdaba65fa", 
            "Inline Method private createManyCells() : EditorCell_Collection inlined to public createCell() : EditorCell in class jetbrains.mps.lang.editor.cellProviders.SingleRoleCellProvider");

          processTp("https://github.com/jboss-developer/jboss-eap-quickstarts.git", "983e0e0e22ab5bd2c6ea44235518057ea45dcca9", 
            "Move Class org.jboss.as.quickstarts.poh5helloworld.HelloWorld moved to org.jboss.as.quickstarts.html5rest.HelloWorld",
            "Move Class org.jboss.as.quickstarts.poh5helloworld.HelloService moved to org.jboss.as.quickstarts.html5rest.HelloService");

          processTp("https://github.com/square/wire.git", "85a690e3cdbbb8447342eefdf690e22ad1b33e02", 
            "Extract Method private fieldInitializer(type ProtoTypeName, value Object) : CodeBlock extracted from private defaultValue(field WireField) : CodeBlock in class com.squareup.wire.java.TypeWriter");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "3f2a0e7629d032712ab38ab1431cd0a9d91d4db6", 
            "Move Attribute private IDMAP_HASH_VERSION : byte from class com.android.server.pm.PackageManagerService to class android.content.pm.ThemeUtils");

          processTp("https://github.com/realm/realm-java.git", "9b5b10a0c254017a48651771029f4dfc0a61bcfa", 
            "Move Attribute package CASTING_TYPES : Map<String,String> from class io.realm.processor.RealmProxyClassGenerator to class io.realm.processor.Constants",
            "Move Attribute package JAVA_TO_COLUMN_TYPES : Map<String,String> from class io.realm.processor.RealmProxyClassGenerator to class io.realm.processor.Constants",
            "Move Attribute package NULLABLE_JAVA_TYPES : Map<String,String> from class io.realm.processor.RealmProxyClassGenerator to class io.realm.processor.Constants",
            "Move Attribute package JAVA_TO_REALM_TYPES : Map<String,String> from class io.realm.processor.RealmProxyClassGenerator to class io.realm.processor.Constants");

          processTp("https://github.com/orientechnologies/orientdb.git", "0a1ff849ec7709be8553383fe9c2c7301980dde0", 
            "Pull Up Method public getDatabase() : ODatabaseDocumentInternal from class com.orientechnologies.orient.core.sql.parser.OStatement to public getDatabase() : ODatabaseDocumentInternal from class com.orientechnologies.orient.core.sql.parser.SimpleNode");


          processFp("https://github.com/JetBrains/intellij-community.git", "cff1ba8ca168413ceed389a5934b43cf72bf32f8", 
            "Move Method protected useNonBlockingRead() : boolean from class org.jetbrains.idea.svn.commandLine.TerminalProcessHandler to protected useNonBlockingRead() : boolean from class git4idea.commands.GitTextHandler.MyOSProcessHandler");

          processFp("https://github.com/spring-projects/spring-boot.git", "36d36f97bd95dbb3bab1fdfd40a28975e919571d", 
            "Extract Method private getRandomValue(type String) : Object extracted from public getProperty(name String) : Object in class org.springframework.boot.context.config.RandomValuePropertySource");
          processTp("https://github.com/slapperwan/gh4a.git", "b8fffb706258db4c4d2f608d8e8dad9312e2230d", 
            "Move Method private writeCssInclude(builder StringBuilder, cssType String) : void from class com.gh4a.utils.StringUtils to protected writeCssInclude(builder StringBuilder, cssType String) : void from class com.gh4a.activities.WebViewerActivity",
            "Move Method private writeScriptInclude(builder StringBuilder, scriptName String) : void from class com.gh4a.utils.StringUtils to protected writeScriptInclude(builder StringBuilder, scriptName String) : void from class com.gh4a.activities.WebViewerActivity",
            "Move Method public highlightImage(imageUrl String) : String from class com.gh4a.utils.StringUtils to private highlightImage(imageUrl String) : String from class com.gh4a.activities.FileViewerActivity",
            "Extract Method private isExtensionIn(filename String, extensions List<String>) : boolean extracted from public isImage(filename String) : boolean in class com.gh4a.utils.FileUtils",
            "Move Attribute private SKIP_PRETTIFY_EXT : List<String> from class com.gh4a.Constants to class com.gh4a.activities.WebViewerActivity");

          processTp("https://github.com/hazelcast/hazelcast.git", "c00275e7f85c8a9af5785f66cc0f75dc027b6cb6", 
            "Push Down Method protected getConnection() : HazelcastConnection from class com.hazelcast.jca.AbstractDeploymentTest to protected getConnection() : HazelcastConnection from class com.hazelcast.jca.XATransactionTest");
          processFp("https://github.com/hazelcast/hazelcast.git", "c00275e7f85c8a9af5785f66cc0f75dc027b6cb6", 
            "Push Down Attribute private connectionFactory : HazelcastConnectionFactory from class com.hazelcast.jca.AbstractDeploymentTest to class com.hazelcast.jca.XATransactionTest");
          processTp("https://github.com/deeplearning4j/deeplearning4j.git", "3325f5ccd23f8016fa28a24f878b54f1918546ed", 
            "Extract Method public loadGoogleModel(modelFile File, binary boolean, lineBreaks boolean) : Word2Vec extracted from public loadGoogleModel(modelFile File, binary boolean) : Word2Vec in class org.deeplearning4j.models.embeddings.loader.WordVectorSerializer");

          processTp("https://github.com/gradle/gradle.git", "ba1da95200d080aef6251f13ced0ca67dff282be", 
            "Move Class org.gradle.tooling.tests.TestExecutionException moved to org.gradle.tooling.test.TestExecutionException");

          processTp("https://github.com/gradle/gradle.git", "b1fb1192daa1647b0bd525600dd41063765eca70", 
            "Push Down Method public setTasks(taskNames List<String>) : void from class org.gradle.testkit.functional.GradleRunner to public setTasks(taskNames List<String>) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Method public getTasks() : List<String> from class org.gradle.testkit.functional.GradleRunner to public getTasks() : List<String> from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Method public setArguments(arguments List<String>) : void from class org.gradle.testkit.functional.GradleRunner to public setArguments(arguments List<String>) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Method public getArguments() : List<String> from class org.gradle.testkit.functional.GradleRunner to public getArguments() : List<String> from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Method public setWorkingDir(workingDirectory File) : void from class org.gradle.testkit.functional.GradleRunner to public setWorkingDir(workingDirectory File) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Method public getWorkingDir() : File from class org.gradle.testkit.functional.GradleRunner to public getWorkingDir() : File from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Method public setGradleUserHomeDir(gradleUserHomeDir File) : void from class org.gradle.testkit.functional.GradleRunner to public setGradleUserHomeDir(gradleUserHomeDir File) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Method public getGradleUserHomeDir() : File from class org.gradle.testkit.functional.GradleRunner to public getGradleUserHomeDir() : File from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Attribute private taskNames : List<String> from class org.gradle.testkit.functional.GradleRunner to class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Attribute private arguments : List<String> from class org.gradle.testkit.functional.GradleRunner to class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Attribute private workingDirectory : File from class org.gradle.testkit.functional.GradleRunner to class org.gradle.testkit.functional.internal.DefaultGradleRunner",
            "Push Down Attribute private gradleUserHomeDir : File from class org.gradle.testkit.functional.GradleRunner to class org.gradle.testkit.functional.internal.DefaultGradleRunner");

          processTp("https://github.com/orientechnologies/orientdb.git", "f50f234b24e6ada29c82ce57830118508bf55d51", 
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.ReadWriteDiskCacheTest moved to com.orientechnologies.orient.core.storage.cache.local.ReadWriteDiskCacheTest",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.HashLRUListTest moved to com.orientechnologies.orient.core.storage.cache.local.HashLRUListTest",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.SynchronizedLRUList moved to com.orientechnologies.orient.core.storage.cache.local.SynchronizedLRUList",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.PageGroup moved to com.orientechnologies.orient.core.storage.cache.local.PageGroup",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.ConcurrentLRUList moved to com.orientechnologies.orient.core.storage.cache.local.ConcurrentLRUList",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.HashLRUList moved to com.orientechnologies.orient.core.storage.cache.local.HashLRUList",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.LRUEntry moved to com.orientechnologies.orient.core.storage.cache.local.LRUEntry",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.LRUList moved to com.orientechnologies.orient.core.storage.cache.local.LRUList",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OBlockedPageException moved to com.orientechnologies.orient.core.storage.cache.OBlockedPageException",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OCachePointer moved to com.orientechnologies.orient.core.storage.cache.OCachePointer",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OPageDataVerificationError moved to com.orientechnologies.orient.core.storage.cache.OPageDataVerificationError",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OReadCache moved to com.orientechnologies.orient.core.storage.cache.OReadCache");
          processFp("https://github.com/orientechnologies/orientdb.git", "f50f234b24e6ada29c82ce57830118508bf55d51", 
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.LowSpaceEventsPublisherFactory moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.LowSpaceEventsPublisherFactory",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.FlushThreadFactory moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.FlushThreadFactory",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.RemoveFilePagesTask moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.RemoveFilePagesTask",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.FileFlushTask moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.FileFlushTask",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.PeriodicalFuzzyCheckpointTask moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.PeriodicalFuzzyCheckpointTask",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.PeriodicFlushTask moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.PeriodicFlushTask",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.PagedKey moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.PagedKey",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.O2QCache.PinnedPage moved to com.orientechnologies.orient.core.storage.cache.local.O2QCache.PinnedPage",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.O2QCache.PageKey moved to com.orientechnologies.orient.core.storage.cache.local.O2QCache.PageKey",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.O2QCache.UpdateCacheResult moved to com.orientechnologies.orient.core.storage.cache.local.O2QCache.UpdateCacheResult",
            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OWOWCache.NameFileIdEntry moved to com.orientechnologies.orient.core.storage.cache.local.OWOWCache.NameFileIdEntry");
          processTp("https://github.com/crate/crate.git", "d5f10a4958f5e870680be906689d92d1efb42480", 
            "Extract Method public add(info ReferenceInfo, partitionBy boolean) : Builder extracted from public add(column String, type DataType, path List<String>, columnPolicy ColumnPolicy, indexType IndexType, partitionBy boolean) : Builder in class io.crate.metadata.table.TestingTableInfo.Builder");

          processTp("https://github.com/real-logic/Aeron.git", "4b762c2c70f06b0c5d2cd85866424c46478c827b", 
            "Move Attribute private flowControl : FlowControl from class uk.co.real_logic.aeron.driver.cmd.NewPublicationCmd to class uk.co.real_logic.aeron.driver.NetworkPublication");

          processTp("https://github.com/infinispan/infinispan.git", "35b6c869546a7968b6fd2f640add6eea87e03c22", 
            "Move Class org.infinispan.query.dsl.embedded.impl.EmbeddedQuery.ReverseFilterResultComparator moved to org.infinispan.query.dsl.embedded.impl.BaseEmbeddedQuery.ReverseFilterResultComparator");
          processFp("https://github.com/infinispan/infinispan.git", "35b6c869546a7968b6fd2f640add6eea87e03c22", 
            "Move Attribute private booleanFilterNormalizer : BooleanFilterNormalizer from class org.infinispan.objectfilter.impl.BaseMatcher to class org.infinispan.query.remote.QueryFacadeImpl",
            "Move Attribute private booleanFilterNormalizer : BooleanFilterNormalizer from class org.infinispan.objectfilter.impl.BaseMatcher to class org.infinispan.query.dsl.embedded.impl.QueryEngine",
            "Move Attribute protected projection : String[] from class org.infinispan.query.dsl.embedded.impl.EmbeddedQuery to class org.infinispan.query.dsl.impl.BaseQuery",
            "Move Attribute private booleanFilterNormalizer : BooleanFilterNormalizer from class org.infinispan.objectfilter.impl.BaseMatcher to class org.infinispan.objectfilter.impl.FilterRegistry");
          processTp("https://github.com/google/guava.git", "5bab9e837cf273250aa26702204f139fdcfd9e7a", 
            "Inline Method private checkForConcurrentModification() : void inlined to public remove() : void in class com.google.common.collect.HashBiMap.Itr",
            "Inline Method private checkForConcurrentModification() : void inlined to public hasNext() : boolean in class com.google.common.collect.HashBiMap.Itr");

          processTp("https://github.com/hibernate/hibernate-orm.git", "2f1b67b03f6c48aa189d7478e16ed0dcf8d50af8", 
            "Pull Up Attribute protected MUTABLE_NON_VERSIONED : CacheDataDescription from class org.hibernate.test.cache.infinispan.entity.EntityRegionImplTestCase to class org.hibernate.test.cache.infinispan.AbstractEntityCollectionRegionTestCase",
            "Pull Up Attribute protected MUTABLE_NON_VERSIONED : CacheDataDescription from class org.hibernate.test.cache.infinispan.collection.CollectionRegionImplTestCase to class org.hibernate.test.cache.infinispan.AbstractEntityCollectionRegionTestCase");

          processTp("https://github.com/google/truth.git", "1768840bf1e69892fd2a23776817f620edfed536", 
            "Move Class com.google.common.truth.ListTest.Bar moved to com.google.common.truth.IterableTest.Bar",
            "Move Class com.google.common.truth.ListTest.Foo moved to com.google.common.truth.IterableTest.Foo");

          processTp("https://github.com/google/truth.git", "200f1577d238a6d3fbcf99cb2a2585b2071214a6", 
            "Extract Method public isOrdered(comparator Comparator<? super T>) : void extracted from public isPartiallyOrdered(comparator Comparator<? super T>) : void in class com.google.common.truth.IterableSubject",
            "Extract Method public isOrdered() : void extracted from public isPartiallyOrdered() : void in class com.google.common.truth.IterableSubject");

          processTp("https://github.com/wildfly/wildfly.git", "c0f8a7f2b4341601df63c5470f41f157dbd83781", 
            "Extract Method private standaloneCollect(cli CLI, protocol String, host String, port int) : void extracted from public main(args String[]) : void in class org.jboss.as.jdr.CommandLineMain");

          processTp("https://github.com/hibernate/hibernate-orm.git", "44a02e5efc39c6953ca6dd631669d91293ab67f6", 
            "Move Class org.hibernate.test.bytecode.enhancement.entity.customer.User moved to org.hibernate.test.bytecode.enhancement.association.User",
            "Move Class org.hibernate.test.bytecode.enhancement.entity.customer.Group moved to org.hibernate.test.bytecode.enhancement.association.Group",
            "Move Class org.hibernate.test.bytecode.enhancement.entity.SimpleEntity moved to org.hibernate.test.bytecode.enhancement.dirty.SimpleEntity",
            "Move Class org.hibernate.test.bytecode.enhancement.entity.MyEntity moved to org.hibernate.test.bytecode.enhancement.basic.MyEntity",
            "Move Class org.hibernate.test.bytecode.enhancement.entity.Country moved to org.hibernate.test.bytecode.enhancement.dirty.Country",
            "Move Class org.hibernate.test.bytecode.enhancement.entity.Address moved to org.hibernate.test.bytecode.enhancement.dirty.Address");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "4587c32ab8a1c8e2169e4f93491a8c927216a6ab", 
            "Extract Method private startAsync() : void extracted from public start() : void in class com.android.systemui.usb.StorageNotification");

          processTp("https://github.com/VoltDB/voltdb.git", "05bd8ecda456e0901ef7375b9ff7b120ae668eca", 
            "Move Class exportbenchmark.SocketExporter moved to exportbenchmark2.exporter.exportbenchmark.SocketExporter",
            "Move Class exportbenchmark.NoOpExporter moved to exportbenchmark2.exporter.exportbenchmark.NoOpExporter",
            "Move Class exportbenchmark.procedures.TruncateTables moved to exportbenchmark2.db.exportbenchmark.procedures.TruncateTables",
            "Move Class exportbenchmark.procedures.SampleRecord moved to exportbenchmark2.db.exportbenchmark.procedures.SampleRecord",
            "Move Class exportbenchmark.procedures.InsertExport5 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport5",
            "Move Class exportbenchmark.procedures.InsertExport10 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport10",
            "Move Class exportbenchmark.procedures.InsertExport1 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport1",
            "Move Class exportbenchmark.procedures.InsertExport0 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport0",
            "Move Class exportbenchmark.procedures.InsertExport moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport",
            "Move Class exportbenchmark.Connect2Server moved to exportbenchmark2.client.exportbenchmark.Connect2Server");
          processFp("https://github.com/VoltDB/voltdb.git", "05bd8ecda456e0901ef7375b9ff7b120ae668eca", 
            "Move Class exportbenchmark.ExportBenchmark.ExportCallback moved to exportbenchmark2.client.exportbenchmark.ExportBenchmark.ExportCallback",
            "Move Class exportbenchmark.ExportBenchmark.ExportBenchConfig moved to exportbenchmark2.client.exportbenchmark.ExportBenchmark.ExportBenchConfig");

          processFp("https://github.com/spotify/helios.git", "14d2a88726358a86900d891aff80c6c0ca69a8d6", 
            "Extract Method private getUndeployOperations(client ZooKeeperClient, host String, jobId JobId, token String) : List<ZooKeeperOperation> extracted from public rollingUpdate(deploymentGroupName String, jobId JobId) : void in class com.spotify.helios.master.ZooKeeperMasterModel");
          processTp("https://github.com/abarisain/dmix.git", "885771d57c97bd2dd48951e8aeaaa87ceb87532b", 
            "Inline Method package processIntent(action String, mpd MPD) : void inlined to protected onHandleIntent(intent Intent) : void in class com.namelessdev.mpdroid.widgets.WidgetHelperService");

          processTp("https://github.com/cucumber/cucumber-jvm.git", "0e815f3e1339f91960c7c64ab395de6dd8ff9eec", 
            "Move Class cucumber.runtime.java.ObjectFactory moved to cucumber.api.java.ObjectFactory");

          processTp("https://github.com/apache/drill.git", "b2bbd9941be6b132a83d27c0ae02c935e1dec5dd", 
            "Extract Method private allocateBytes(size long) : void extracted from public allocateNew(valueCount int) : void in class org.apache.drill.exec.vector.BitVector",
            "Extract Method private allocateBytes(size long) : void extracted from public allocateNewSafe() : boolean in class org.apache.drill.exec.vector.BitVector",
            "Extract Method private allocateBytes(size long) : void extracted from public allocateNew(valueCount int) : void in class org.apache.drill.exec.vector.$",
            "Extract Method private allocateBytes(size long) : void extracted from public allocateNewSafe() : boolean in class org.apache.drill.exec.vector.$");
          processFp("https://github.com/apache/drill.git", "b2bbd9941be6b132a83d27c0ae02c935e1dec5dd", 
            "Inline Method private fillEmpties(index int) : void inlined to public setValueCount(valueCount int) : void in class org.apache.drill.exec.vector.$.Mutator");
          processTp("https://github.com/gradle/gradle.git", "36ccb0f5c6771ff4be87a282560c090447520b66", 
            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ResolvedLocalComponentsResultGraphVisitor moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.projectresult.ResolvedLocalComponentsResultGraphVisitor",
            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ResolvedConfigurationDependencyGraphVisitor moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.ResolvedConfigurationDependencyGraphVisitor",
            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ResolutionResultDependencyGraphVisitor moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ResolutionResultDependencyGraphVisitor",
            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.DependencyArtifactSet",
            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ConfigurationArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.ConfigurationArtifactSet",
            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.ArtifactSet",
            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.AbstractArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.AbstractArtifactSet");

          processTp("https://github.com/WhisperSystems/TextSecure.git", "fa62b9bde224341e0c2d43c0694fc10c4df7336f", 
            "Inline Method private initializeResources() : void inlined to private initialize() : void in class org.thoughtcrime.securesms.components.emoji.EmojiToggle",
            "Inline Method private init() : void inlined to public EmojiDrawer(context Context, attrs AttributeSet, defStyle int) in class org.thoughtcrime.securesms.components.emoji.EmojiDrawer");


          processFp("https://github.com/CyanogenMod/android_frameworks_base.git", "1d4e84e9045274fa23c3825ea79e4f7721f39e26", 
            "Move Attribute private mVisualizerEnabled : boolean from class com.android.systemui.statusbar.BackDropView to class com.android.systemui.statusbar.phone.PhoneStatusBar");
          processTp("https://github.com/spring-projects/spring-data-mongodb.git", "3224fa8ce7e0079d6ad507e17534cdf01f758876", 
            "Inline Method private processTypeHintForNestedDocuments(source Object, info TypeInformation<?>) : TypeInformation<?> inlined to private getTypeHintForEntity(source Object, entity MongoPersistentEntity<?>) : TypeInformation<?> in class org.springframework.data.mongodb.core.convert.UpdateMapper");

          processTp("https://github.com/gradle/gradle.git", "f394599bf1423be0be2d5822ed7f1271d2841225", 
            "Move Class org.gradle.jvm.plugins.JarBinaryRules moved to org.gradle.jvm.internal.JarBinaryRules");

          processTp("https://github.com/spring-projects/spring-data-jpa.git", "36d1b0717bc5836bba39985caadc2df5f2533ac4", 
            "Move Class org.springframework.data.jpa.repository.augment.JpaSoftDeleteQueryAugmentor.PropertyChangeEnsuringBeanWrapper moved to org.springframework.data.jpa.repository.augment.PropertyChangeEnsuringBeanWrapper");

          processTp("https://github.com/google/j2objc.git", "fa3e6fa02dadc675f0d487a15cd842b3ac4a0c11", 
            "Extract Method private getOperatorFunctionModifier(expr Expression) : String extracted from private rewriteBoxedPrefixOrPostfix(node TreeNode, operand Expression, funcName String) : void in class com.google.devtools.j2objc.translate.Autoboxer");

          processTp("https://github.com/google/closure-compiler.git", "b9a17665b158955ad28ef7f50cc0a8585460f053", 
            "Inline Method private createUntaggedTemplateLiteral(n Node) : void inlined to package visitTemplateLiteral(t NodeTraversal, n Node) : void in class com.google.javascript.jscomp.Es6TemplateLiterals");


          processFp("https://github.com/HubSpot/Singularity.git", "f06f7ab4b898a97e8af8a47c0164205c96992d05", 
            "Inline Method private addContinuation(existingHandler SingularityS3DownloaderAsyncHandler) : boolean inlined to private addDownloadRequest() : boolean in class com.hubspot.singularity.s3downloader.server.SingularityS3DownloaderCoordinator.DownloadJoiner",
            "Inline Method private completeContinuations() : void inlined to public run() : void in class com.hubspot.singularity.s3downloader.server.SingularityS3DownloaderAsyncHandler");
          processTp("https://github.com/deeplearning4j/deeplearning4j.git", "91cdfa1ffd937a4cb01cdc0052874ef7831955e2", 
            "Extract Method private getNewScore(oldParameters INDArray) : double extracted from public optimize(initialStep double, parameters INDArray, gradients INDArray) : double in class org.deeplearning4j.optimize.solvers.BackTrackLineSearch",
            "Move Method public testBackTrackLine() : void from class org.deeplearning4j.plot.ListenerTest to public testBackTrackLine() : void from class org.deeplearning4j.optimize.solver.BackTrackLineSearchTest");

          processTp("https://github.com/spring-projects/spring-framework.git", "ef0eb01f93d6c485cf37692fd193833a6821272a", 
            "Extract Method protected checkRequest(request HttpServletRequest) : void extracted from protected checkAndPrepare(request HttpServletRequest, response HttpServletResponse, cacheControl CacheControl) : void in class org.springframework.web.servlet.support.WebContentGenerator");


          processFp("https://github.com/geoserver/geoserver.git", "8f9a784bdfd0456bcb2621bf92050154d9313df7", 
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterRed() : void in class org.geoserver.wcs.GetCoverageTest",
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterGreen() : void in class org.geoserver.wcs.GetCoverageTest");
          processTp("https://github.com/apache/hive.git", "b8d2140fe4faccadcf1a6343ec8cd0cc58c315f9", 
            "Extract Method private doFirstFetchIfNeeded() : void extracted from private joinFinalLeftData() : void in class org.apache.hadoop.hive.ql.exec.CommonMergeJoinOperator",
            "Extract Method private doFirstFetchIfNeeded() : void extracted from public process(row Object, tag int) : void in class org.apache.hadoop.hive.ql.exec.CommonMergeJoinOperator");

          processTp("https://github.com/tomahawk-player/tomahawk-android.git", "56c273ee11296288cb15320c3de781b94a1e8eb4", 
            "Push Down Attribute private mQueryTimeStamps : ConcurrentHashMap<Query,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.UserCollection",
            "Push Down Attribute private mArtistTimeStamps : ConcurrentHashMap<Artist,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.UserCollection",
            "Push Down Attribute private mAlbumTimeStamps : ConcurrentHashMap<Album,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.UserCollection",
            "Pull Up Attribute protected mAlbums : Set<Album> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.Collection",
            "Pull Up Attribute protected mArtists : Set<Artist> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.Collection",
            "Pull Up Attribute protected mAlbumArtists : Set<Artist> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.Collection",
            "Pull Up Attribute protected mQueries : Set<Query> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.Collection",
            "Pull Up Attribute protected mAlbumTracks : ConcurrentHashMap<Album,Set<Query>> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.Collection",
            "Pull Up Attribute protected mArtistAlbums : ConcurrentHashMap<Artist,Set<Album>> from class org.tomahawk.libtomahawk.collection.NativeCollection to class org.tomahawk.libtomahawk.collection.Collection",
            "Push Down Method public getAlbumTimeStamps() : ConcurrentHashMap<Album,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to public getAlbumTimeStamps() : ConcurrentHashMap<Album,Long> from class org.tomahawk.libtomahawk.collection.UserCollection",
            "Push Down Method public getArtistTimeStamps() : ConcurrentHashMap<Artist,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to public getArtistTimeStamps() : ConcurrentHashMap<Artist,Long> from class org.tomahawk.libtomahawk.collection.UserCollection",
            "Push Down Method public addQuery(query Query, addedTimeStamp long) : void from class org.tomahawk.libtomahawk.collection.NativeCollection to public addQuery(query Query, addedTimeStamp long) : void from class org.tomahawk.libtomahawk.collection.UserCollection",
            "Push Down Method public getQueryTimeStamps() : ConcurrentHashMap<Query,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to public getQueryTimeStamps() : ConcurrentHashMap<Query,Long> from class org.tomahawk.libtomahawk.collection.UserCollection",
            "Pull Up Method public wipe() : void from class org.tomahawk.libtomahawk.collection.NativeCollection to public wipe() : void from class org.tomahawk.libtomahawk.collection.Collection");

          processTp("https://github.com/nutzam/nutz.git", "de7efe40dad0f4bb900c4fffa80ed377745532b3", 
            "Extract Method public migration(dao Dao, klass Class<?>, add boolean, del boolean, tableName Object) : void extracted from public migration(dao Dao, klass Class<?>, add boolean, del boolean) : void in class org.nutz.dao.util.Daos");


          processFp("https://github.com/gocd/gocd.git", "670673d0f9ee6090ec95aecd655291df8cbf9845", 
            "Move Attribute private systemEnvironment : SystemEnvironment from class com.thoughtworks.go.server.config.WeakSSLConfigTest to class com.thoughtworks.go.server.config.GoSSLConfigTest",
            "Move Method public setUp() : void from class com.thoughtworks.go.server.config.WeakSSLConfigTest to public setUp() : void from class com.thoughtworks.go.server.config.GoSSLConfigTest");

          processFp("https://github.com/realm/realm-java.git", "37dbfb6de6a4d075e154e19b0af2239149fe766d", 
            "Move Class io.realm.Realm.DebugRealmObjectQueryCallback moved to io.realm.RealmObject.DebugRealmObjectQueryCallback",
            "Move Class io.realm.Realm.DebugRealmResultsQueryCallback moved to io.realm.RealmResults.DebugRealmResultsQueryCallback");

          processFp("https://github.com/JetBrains/intellij-plugins.git", "afde6f7ef85a429b27c319ca63ccf7065a3e39c9", 
            "Inline Method private shiftReferences(references FileReference[], shift int) : FileReference[] inlined to private getPackageReferences(contextFile VirtualFile, packagesFolder VirtualFile, relPathFromPackagesFolderToReferencedFile String, startOffset int) : PsiReference[] in class com.jetbrains.lang.dart.psi.impl.DartUriElementBase");
          processTp("https://github.com/amplab/tachyon.git", "b0938501f1014cf663e33b44ed5bb9b24d19a358", 
            "Extract Method private getBlockOutStream(filename String, isLocalWrite boolean) : BlockOutStream extracted from public enableLocalWriteTest() : void in class tachyon.client.BlockOutStreamIntegrationTest",
            "Extract Method private getBlockOutStream(filename String, isLocalWrite boolean) : BlockOutStream extracted from public disableLocalWriteTest() : void in class tachyon.client.BlockOutStreamIntegrationTest");

          processTp("https://github.com/apache/cassandra.git", "f283ed29814403bde6350a2598cdd6e2c8b983d5", 
            "Inline Method public submitBackground(cfs ColumnFamilyStore, autoFill boolean) : List<Future<?>> inlined to public submitBackground(cfs ColumnFamilyStore) : List<Future<?>> in class org.apache.cassandra.db.compaction.CompactionManager");

          processTp("https://github.com/orfjackal/retrolambda.git", "46b0d84de9c309bca48a99e572e6611693ed5236", 
            "Extract Method public saveResource(relativePath Path, content byte[]) : void extracted from public save(bytecode byte[]) : void in class net.orfjackal.retrolambda.files.ClassSaver");

          processTp("https://github.com/JetBrains/MPS.git", "7b5622d41537315710b6fd57b2739a3a64698375", 
            "Extract Method private getTreePath(components List<String>, escapePathSep boolean) : TreePath extracted from private stringToPath(pathString String) : TreePath in class jetbrains.mps.ide.ui.tree.MPSTree");

          processTp("https://github.com/Jasig/cas.git", "7fb0d1ce3b6583013e81ac05eb9afb15d20eab7f", 
            "Move Attribute private NTLMSSP_SIGNATURE : Byte[] from class org.jasig.cas.support.spnego.util.SpnegoConstants to class org.jasig.cas.support.spnego.authentication.principal.SpnegoCredential");

          processTp("https://github.com/facebook/facebook-android-sdk.git", "19d1936c3b07d97d88646aeae30de747715e3248", 
            "Extract Method private getErrorMessage(extras Bundle) : String extracted from private handleResultOk(request Request, data Intent) : Result in class com.facebook.login.KatanaProxyLoginMethodHandler",
            "Extract Method private getError(extras Bundle) : String extracted from private handleResultOk(request Request, data Intent) : Result in class com.facebook.login.KatanaProxyLoginMethodHandler",
            "Extract Method public sdkInitialize(applicationContext Context, callback InitializeCallback) : void extracted from public sdkInitialize(applicationContext Context) : void in class com.facebook.FacebookSdk",
            "Extract Method public sdkInitialize(applicationContext Context, callbackRequestCodeOffset int, callback InitializeCallback) : void extracted from public sdkInitialize(applicationContext Context, callbackRequestCodeOffset int) : void in class com.facebook.FacebookSdk",
            "Move Attribute private CAPTION_PARAM : String from class com.facebook.share.internal.ShareInternalUtility to class com.facebook.GraphRequest",
            "Move Attribute private PICTURE_PARAM : String from class com.facebook.share.internal.ShareInternalUtility to class com.facebook.GraphRequest");

          processTp("https://github.com/gwtproject/gwt.git", "e0dda9f61b7c409944c4734edf75b108e0288f59", 
            "Move Class com.google.gwt.core.client.impl.Md5Digest moved to java.security.MessageDigest.Md5Digest");

          processTp("https://github.com/WhisperSystems/TextSecure.git", "f0b2cc559026871c1b4d8e008666afb590553004", 
            "Extract Method private craftIntent(context Context, intentAction String, extras Bundle) : PendingIntent extracted from public getMarkAsReadIntent(context Context, masterSecret MasterSecret) : PendingIntent in class org.thoughtcrime.securesms.notifications.NotificationState");

          processTp("https://github.com/JetBrains/intellij-community.git", "ce5f9ff96e2718e4014655f819314ac2ac4bd8bf", 
            "Move Method private getLiveIndicator(base Icon) : Icon from class com.intellij.execution.ui.RunContentManagerImpl to public getLiveIndicator(base Icon) : Icon from class com.intellij.execution.runners.ExecutionUtil");

          processTp("https://github.com/bitcoinj/bitcoinj.git", "95bfa40630e34f6f369e0055d9f37f49bca60247", 
            "Extract Method public getUTXOs(outPoints List<TransactionOutPoint>, includeMempool boolean) : ListenableFuture<UTXOsMessage> extracted from public getUTXOs(outPoints List<TransactionOutPoint>) : ListenableFuture<UTXOsMessage> in class org.bitcoinj.core.Peer");

          processTp("https://github.com/WhisperSystems/TextSecure.git", "c4a37e38aba926c2bef27e4fc00e3a4848ce46bd", 
            "Extract Method public setMedia(slide Slide, masterSecret MasterSecret) : void extracted from public setMedia(slide Slide) : void in class org.thoughtcrime.securesms.mms.AttachmentManager");

          processTp("https://github.com/apache/hive.git", "f664789737d516ac664462732664121acc111a1e", 
            "Extract Method private dumpConfig(conf Configuration, sb StringBuilder) : void extracted from private dumpEnvironent() : String in class org.apache.hive.hcatalog.templeton.AppConfig");

          processTp("https://github.com/checkstyle/checkstyle.git", "a07cae0aca9f9072256b3a5fd05779e8d69b9748", 
            "Inline Method private leaveLiteralTry() : void inlined to public leaveToken(literalTry DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedTryDepthCheck",
            "Inline Method private visitLiteralTry(literalTry DetailAST) : void inlined to public visitToken(literalTry DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedTryDepthCheck",
            "Inline Method private leaveLiteralIf(literalIf DetailAST) : void inlined to public leaveToken(literalIf DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedIfDepthCheck",
            "Inline Method private visitLiteralIf(literalIf DetailAST) : void inlined to public visitToken(literalIf DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedIfDepthCheck");

          processTp("https://github.com/amplab/tachyon.git", "9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825", 
            "Extract Method private getCandidateDirInTier(tier StorageTier, blockSize long) : StorageDir extracted from public allocateBlock(userId long, blockId long, blockSize long, location BlockStoreLocation) : TempBlockMeta in class tachyon.worker.block.allocator.MaxFreeAllocator");

          processTp("https://github.com/reactor/reactor.git", "669b96c8aa4ed5134617932118de563bd4c34066", 
            "Move Class reactor.core.alloc.ReferenceCountingAllocator moved to reactor.alloc.ReferenceCountingAllocator",
            "Move Class reactor.core.alloc.Reference moved to reactor.alloc.Reference",
            "Move Class reactor.core.alloc.RecyclableString moved to reactor.alloc.RecyclableString",
            "Move Class reactor.core.alloc.RecyclableNumber moved to reactor.alloc.RecyclableNumber",
            "Move Class reactor.core.alloc.Recyclable moved to reactor.core.support.Recyclable",
            "Move Class reactor.core.alloc.Allocator moved to reactor.alloc.Allocator",
            "Move Class reactor.core.alloc.AbstractReference moved to reactor.alloc.AbstractReference",
            "Move Class reactor.bus.alloc.EventAllocatorTests moved to reactor.alloc.EventAllocatorTests",
            "Move Class reactor.bus.alloc.EventFactorySupplier moved to reactor.alloc.EventFactorySupplier",
            "Move Class reactor.bus.alloc.EventAllocator moved to reactor.alloc.EventAllocator");

          processTp("https://github.com/crate/crate.git", "563d281b61e9f8748858e911eaa810e981f1e953", 
            "Extract Method private getCustomRoutingCol() : ColumnIdent extracted from private getRoutingCol() : ColumnIdent in class io.crate.metadata.doc.DocIndexMetaData");

          processTp("https://github.com/siacs/Conversations.git", "bdc9f9a44f337ab595a3570833dc6a0558df904c", 
            "Extract Method private getIdentityKeyCursor(account Account, name String, own boolean, fingerprint String) : Cursor extracted from private getIdentityKeyCursor(account Account, name String, own boolean) : Cursor in class eu.siacs.conversations.persistance.DatabaseBackend");

          processTp("https://github.com/openhab/openhab.git", "cf1efb6d27a4037cdbe5a780afa6053859a60d4a", 
            "Extract Method private initializeGeneralGlobals() : void extracted from private initializeSciptGlobals() : void in class org.openhab.core.jsr223.internal.engine.scriptmanager.Script",
            "Extract Method private initializeNashornGlobals() : void extracted from private initializeSciptGlobals() : void in class org.openhab.core.jsr223.internal.engine.scriptmanager.Script");

          processTp("https://github.com/novoda/android-demos.git", "5cdabae35f0642e9fe243afe12e4c16b3378a150", 
            "Move Class com.novoda.Base64DecoderException moved to com.novoda.demo.encryption.Base64DecoderException",
            "Move Class com.novoda.Base64 moved to com.novoda.demo.encryption.Base64");

          processTp("https://github.com/netty/netty.git", "8a16081a9322b4a4062baaf32edc6b6b8b4afa88", 
            "Inline Method private cancelPendingStreams() : void inlined to public close() : void in class io.netty.handler.codec.http2.StreamBufferingEncoder");

          processTp("https://github.com/apache/camel.git", "9f319029ecc031cf8bf1756ab8a0e9e4e52c2902", 
            "Extract Interface org.apache.camel.model.OtherAttributesAware from classes [org.apache.camel.model.ProcessorDefinition]");

          processTp("https://github.com/spotify/helios.git", "da39bfeb9c370abe2d86e6e327fade252434090d", 
            "Extract Method package run0(client HeliosClient, out PrintStream, json boolean, name String, full boolean) : int extracted from package run(options Namespace, client HeliosClient, out PrintStream, json boolean, stdin BufferedReader) : int in class com.spotify.helios.cli.command.DeploymentGroupStatusCommand");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "910397f2390d6821a006991ed6035c76cbc74897", 
            "Extract Method protected getBoltPointsArrayResource() : int extracted from private loadBoltPoints(res Resources) : float[] in class com.android.systemui.BatteryMeterView.NormalBatteryMeterDrawable",
            "Extract Method public internalStoreStatsHistoryInFile(stats BatteryStats, fname String) : void extracted from public storeStatsHistoryInFile(fname String) : void in class com.android.internal.os.BatteryStatsHelper",
            "Extract Method private queryProperty(id int, fromDock boolean) : long extracted from private queryProperty(id int) : long in class android.os.BatteryManager",
            "Extract Interface com.android.systemui.statusbar.policy.BatteryStateRegistar from classes [com.android.systemui.statusbar.policy.BatteryController]");
          processFp("https://github.com/CyanogenMod/android_frameworks_base.git", "910397f2390d6821a006991ed6035c76cbc74897", 
            "Extract Method protected setKernelWakelockUpdateVersion(kernelWakelockUpdateVersion int) : void extracted from private parseProcWakelocks(wlBuffer byte[], len int, wakeup_sources boolean) : Map<String,KernelWakelockStats> in class com.android.internal.os.BatteryStatsImpl",
            "Extract Method protected getKernelWakelockUpdateVersion() : int extracted from public updateKernelWakelocksLocked() : void in class com.android.internal.os.BatteryStatsImpl",
            "Extract Method protected getKernelWakelockUpdateVersion() : int extracted from private parseProcWakelocks(wlBuffer byte[], len int, wakeup_sources boolean) : Map<String,KernelWakelockStats> in class com.android.internal.os.BatteryStatsImpl",
            "Move Class com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback moved to com.android.systemui.statusbar.policy.BatteryStateRegistar.BatteryStateChangeCallback");

          processFp("https://github.com/geoserver/geoserver.git", "72fcd3fb2dfefa2fcc7cef10734d705955f36fed", 
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterRed() : void in class org.geoserver.wcs.GetCoverageTest",
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterGreen() : void in class org.geoserver.wcs.GetCoverageTest");

          processFp("https://github.com/geoserver/geoserver.git", "12d52307e8c478a9bdaf72ed2b672689b00af999", 
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterRed() : void in class org.geoserver.wcs.GetCoverageTest",
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterGreen() : void in class org.geoserver.wcs.GetCoverageTest");

          processFp("https://github.com/geoserver/geoserver.git", "5816636872a927dd28151e001f61e1b7d9cd8b3e", 
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterRed() : void in class org.geoserver.wcs.GetCoverageTest",
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterGreen() : void in class org.geoserver.wcs.GetCoverageTest");

          processFp("https://github.com/geoserver/geoserver.git", "cb2a7bfcae3e125add90a87cd3246146b1f62868", 
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterRed() : void in class org.geoserver.wcs.GetCoverageTest",
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterGreen() : void in class org.geoserver.wcs.GetCoverageTest");
          processTp("https://github.com/thymeleaf/thymeleaf.git", "aed371dac5e1248880e869930c636994c3d0f8dc", 
            "Extract Method private fillUpOverflow() : void extracted from public read(cbuf char[], off int, len int) : int in class org.thymeleaf.templateparser.markup.ThymeleafMarkupTemplateReader",
            "Extract Method private processReadBuffer(buffer char[], off int, len int) : int extracted from public read(cbuf char[], off int, len int) : int in class org.thymeleaf.templateparser.markup.ThymeleafMarkupTemplateReader");

          processTp("https://github.com/HubSpot/Singularity.git", "944aea445051891280a8ab7fbbd514c19646f1ab", 
            "Extract Method protected launchTask(request SingularityRequest, deploy SingularityDeploy, launchTime long, updateTime long, instanceNo int, initialTaskState TaskState) : SingularityTask extracted from protected launchTask(request SingularityRequest, deploy SingularityDeploy, launchTime long, instanceNo int, initialTaskState TaskState) : SingularityTask in class com.hubspot.singularity.SingularitySchedulerTestBase");


          processFp("https://github.com/geoserver/geoserver.git", "60048960c25b4737332e4cb846dd4d3c113232dc", 
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterRed() : void in class org.geoserver.wcs.GetCoverageTest",
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterGreen() : void in class org.geoserver.wcs.GetCoverageTest");

          processFp("https://github.com/geoserver/geoserver.git", "43cb5e5776dba38af153de4dba1ffefac183f26f", 
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterRed() : void in class org.geoserver.wcs.GetCoverageTest",
            "Extract Method package readTiff(response MockHttpServletResponse) : RenderedImage extracted from public testRasterFilterGreen() : void in class org.geoserver.wcs.GetCoverageTest");
          processTp("https://github.com/google/auto.git", "8fc60d81fe0e46e7e5c96e71d4a93fcadc6bde4f", 
            "Extract Method private process(validElements ImmutableSetMultimap<Class<? extends Annotation>,Element>) : void extracted from public process(annotations Set<? extends TypeElement>, roundEnv RoundEnvironment) : boolean in class com.google.auto.common.BasicAnnotationProcessor",
            "Extract Method private validElements(deferredElements ImmutableMap<String,Optional<? extends Element>>, roundEnv RoundEnvironment) : ImmutableSetMultimap<Class<? extends Annotation>,Element> extracted from public process(annotations Set<? extends TypeElement>, roundEnv RoundEnvironment) : boolean in class com.google.auto.common.BasicAnnotationProcessor",
            "Extract Method private deferredElements() : ImmutableMap<String,Optional<? extends Element>> extracted from public process(annotations Set<? extends TypeElement>, roundEnv RoundEnvironment) : boolean in class com.google.auto.common.BasicAnnotationProcessor");


          processFp("https://github.com/google/auto.git", "ce5d9cec0a3ed51f866a9ad4768e4a4d26c13c4e", 
            "Extract Method private parseUnaryExpression() : ExpressionNode extracted from private parseExpression() : ExpressionNode in class com.google.auto.value.processor.escapevelocity.Parser");
          processTp("https://github.com/google/auto.git", "8967e7c33c59e1336e1e3b4671293ced5697fca6", 
            "Extract Method private doTestMissingClass(tempDir File) : void extracted from public testMissingClass() : void in class com.google.auto.value.processor.AutoAnnotationCompilationTest");

          processTp("https://github.com/thymeleaf/thymeleaf.git", "378ba37750a9cb1b19a6db434dfa59308f721ea6", 
            "Extract Method private matchOverflow(structure char[]) : boolean extracted from public read(cbuf char[], off int, len int) : int in class org.thymeleaf.templateparser.reader.BlockAwareReader");


          processFp("https://github.com/checkstyle/checkstyle.git", "b405880b6e79f143c3ba3651fe6c8b68324fca51", 
            "Inline Method private visitExpr(ast DetailAST) : void inlined to public visitToken(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.OneStatementPerLineCheck");

          processFp("https://github.com/gradle/gradle.git", "6089d60cf392e6218c46aa3d2d3005b92fdfefcd", 
            "Extract Interface org.gradle.api.authentication.AuthenticationInternal from classes [org.gradle.api.authentication.BasicAuthentication]");
          processTp("https://github.com/hazelcast/hazelcast.git", "69dd55c93fc99c5f7a1e2c21f10e671e311be49e", 
            "Move Attribute public UTF_8 : Charset from class com.hazelcast.client.impl.protocol.util.UnsafeBuffer to class com.hazelcast.nio.Bits",
            "Move Attribute public UTF_8 : Charset from class com.hazelcast.client.impl.protocol.util.SafeBuffer to class com.hazelcast.nio.Bits",
            "Move Class com.hazelcast.client.protocol.Int2ObjectHashMapTest moved to com.hazelcast.util.collection.Int2ObjectHashMapTest",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap moved to com.hazelcast.util.collection.Int2ObjectHashMap");
          processFp("https://github.com/hazelcast/hazelcast.git", "69dd55c93fc99c5f7a1e2c21f10e671e311be49e", 
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.Supplier moved to com.hazelcast.util.function.Supplier",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.EntryIterator moved to com.hazelcast.util.collection.Int2ObjectHashMap.EntryIterator",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.KeyIterator moved to com.hazelcast.util.collection.Int2ObjectHashMap.KeyIterator",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.ValueIterator moved to com.hazelcast.util.collection.Int2ObjectHashMap.ValueIterator",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.AbstractIterator moved to com.hazelcast.util.collection.Int2ObjectHashMap.AbstractIterator",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.EntrySet moved to com.hazelcast.util.collection.Int2ObjectHashMap.EntrySet",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.ValueCollection moved to com.hazelcast.util.collection.Int2ObjectHashMap.ValueCollection",
            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap.KeySet moved to com.hazelcast.util.collection.Int2ObjectHashMap.KeySet");
          processTp("https://github.com/siacs/Conversations.git", "925801c14e7500313069b2bc04abd066798a881c", 
            "Move Method private setupTrustManager(connection HttpsURLConnection, interactive boolean) : void from class eu.siacs.conversations.http.HttpConnection to public setupTrustManager(connection HttpsURLConnection, interactive boolean) : void from class eu.siacs.conversations.http.HttpConnectionManager");

          processTp("https://github.com/JetBrains/intellij-community.git", "e1625136ba12907696ef4c6e922ce073293f3a2c", 
            "Extract Method private addAnnotationProcessorOption(compilerArg String, optionsMap Map<String,String>) : void extracted from private getAnnotationProcessorOptionsFromCompilerConfig(compilerConfig Element) : Map<String,String> in class org.jetbrains.idea.maven.project.MavenProject");

          processTp("https://github.com/apache/camel.git", "cb0935e3af05b333b5c85a4fb3b1846836218f11", 
            "Extract Method private storeCamelContextInQuartzContext() : SchedulerContext extracted from private createAndInitScheduler() : void in class org.apache.camel.component.quartz2.QuartzComponent");

          processTp("https://github.com/square/javapoet.git", "5a37c2aa596377cb4c9b6f916614407fd0a7d3db", 
            "Extract Superclass com.squareup.javapoet.AbstractTypesTest from classes [com.squareup.javapoet.TypesTest]",
            "Move Class com.squareup.javapoet.TypesTest.Parameterized moved to com.squareup.javapoet.AbstractTypesTest.Parameterized");

          processTp("https://github.com/spotify/helios.git", "dd8753cfb0f67db4dde6c5254e2df3104b635dae", 
            "Extract Method private getDeploymentGroup(client ZooKeeperClient, name String) : DeploymentGroup extracted from public getDeploymentGroup(name String) : DeploymentGroup in class com.spotify.helios.master.ZooKeeperMasterModel");


          processFp("https://github.com/checkstyle/checkstyle.git", "12fc9a2b54118418712e3817bed82d52b8701244", 
            "Move Class com.puppycrawl.tools.checkstyle.naming.AbstractClassName.NonAbstractInnerClass moved to com.puppycrawl.tools.checkstyle.naming.AbstractClassOther.NonAbstractInnerClass");
          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "76331570e68446c17e4ff5287f5b7b2b6b472895", 
            "Extract Method public clearFailedUnlockAttempts(clearFingers boolean) : void extracted from public clearFailedUnlockAttempts() : void in class com.android.keyguard.KeyguardUpdateMonitor");

          processTp("https://github.com/github/android.git", "a7401e5091c06c68fae499ea1972b40143c66fa9", 
            "Extract Method private onUserLoggedIn(uri Uri) : void extracted from protected onNewIntent(intent Intent) : void in class com.github.mobile.accounts.LoginActivity");

          processTp("https://github.com/amplab/tachyon.git", "5b184ac783784c1ca4baf1437888c79bd9460763", 
            "Inline Method private freeSpace(bytesToBeAvailable long, location BlockStoreLocation) : EvictionPlan inlined to public freeSpaceWithView(bytesToBeAvailable long, location BlockStoreLocation, view BlockMetadataManagerView) : EvictionPlan in class tachyon.worker.block.evictor.LRUEvictor");

          processTp("https://github.com/QuantumBadger/RedReader.git", "2b2bb6c734d106cdd1c0f4691607be2fe11d7ebb", 
            "Move Attribute public UI_THREAD_HANDLER : Handler from class org.quantumbadger.redreader.common.General to class org.quantumbadger.redreader.common.AndroidApi");

          processTp("https://github.com/liferay/liferay-plugins.git", "78b54757c0d234db671526aed9b3288a85048e22", 
            "Move Class com.liferay.portal.util.MimeTypesImpl moved to com.liferay.tika.util.MimeTypesImpl",
            "Move Class com.liferay.portal.metadata.XugglerRawMetadataProcessor moved to com.liferay.tika.metadata.XugglerRawMetadataProcessor",
            "Move Class com.liferay.portal.metadata.XMPDM moved to com.liferay.tika.metadata.XMPDM",
            "Move Class com.liferay.portal.metadata.TikaRawMetadataProcessor moved to com.liferay.tika.metadata.TikaRawMetadataProcessor",
            "Move Class com.liferay.portal.metadata.BaseRawMetadataProcessor moved to com.liferay.tika.metadata.BaseRawMetadataProcessor");

          processTp("https://github.com/deeplearning4j/deeplearning4j.git", "3d080545362794ac5ab63a6cf1bdfb523a0d92a5", 
            "Extract Method public readCaffeModel(is InputStream, sizeLimitMb int) : NetParameter extracted from public readCaffeModel(caffeModelPath String, sizeLimitMb int) : NetParameter in class org.deeplearning4j.translate.CaffeModelToJavaClass");

          processTp("https://github.com/checkstyle/checkstyle.git", "febbc986cb25ed460ea601c0a68c7d2597f89ee4", 
            "Move Class com.google.checkstyle.test.chapter5naming.rule521packageNames.PackageNameInputBad moved to com.google.checkstyle.test.chapter5naming.rule521packageNamesCamelCase.PackageNameInputBad");

          processTp("https://github.com/hazelcast/hazelcast.git", "679d38d4316c16ccba4982d7f3ba13c147a451cb", 
            "Extract Method protected getFromNearCache(keyData Data, async boolean) : Object extracted from protected getInternal(key K, expiryPolicy ExpiryPolicy, async boolean) : Object in class com.hazelcast.client.cache.impl.AbstractClientCacheProxy");

          processTp("https://github.com/cgeo/cgeo.git", "7e7e4f54801af4e49ebddb934d0c6ff33a2c2160", 
            "Move Class cgeo.geocaching.connector.TerraCachingConnector moved to cgeo.geocaching.connector.tc.TerraCachingConnector");


          processFp("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "67b7d887c2658c122645dad7ce3eb2b3405f59bd", 
            "Extract Method private executeDelveRequest(payload String) : String extracted from private sendCommand(command String, arguments String) : String in class com.goide.debugger.delve.dlv.Delve",
            "Extract Method public sendCommand(command DelveCommand, callback DelveEventCallback) : void extracted from private runDelve(delvePath String, goFilePath String, workingDirectory String) : void in class com.goide.debugger.delve.dlv.Delve");
          processTp("https://github.com/SimonVT/schematic.git", "c1a9dd63aca8bf488f9a671aa6281538540397f8", 
            "Extract Method private printNotifyInsert(writer JavaWriter, uri UriContract) : void extracted from public write(filer Filer) : void in class net.simonvt.schematic.compiler.ContentProviderWriter");


          processFp("https://github.com/raphw/byte-buddy.git", "e8508791c53a3442d34245a93bd269bb9cdb735e", 
            "Move Method public equals(other Object) : boolean from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForLowerBound to public equals(other Object) : boolean from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.ForSignature.OfType.SuperTypeRegistrant",
            "Move Method public equals(other Object) : boolean from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForUpperBound to public equals(other Object) : boolean from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.ForSignature.OfType.SuperTypeRegistrant",
            "Move Method public equals(other Object) : boolean from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForDirectBound to public equals(other Object) : boolean from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.ForSignature.OfType.SuperTypeRegistrant",
            "Move Method public hashCode() : int from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForUpperBound to public hashCode() : int from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.ForSignature.OfType.SuperTypeRegistrant",
            "Move Method public hashCode() : int from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForDirectBound to public hashCode() : int from class net.bytebuddy.pool.TypePool.Default.GenericTypeExtractor.ForSignature.OfType.SuperTypeRegistrant");

          processFp("https://github.com/checkstyle/checkstyle.git", "e13f8a423f7380dbafdf0fcd8ac8d98a46345017", 
            "Move Class com.puppycrawl.tools.checkstyle.checks.coding.InputPackageDeclaration moved to com.puppycrawl.tools.checkstyle.coding.InputPackageDeclaration");

          processFp("https://github.com/JetBrains/intellij-community.git", "36e95f77f7fdffcaaded41cf0af79edbc3658fb2", 
            "Inline Method private processLine(buffer char[], token StringBuilder, n int) : void inlined to protected readAvailableBlocking() : boolean in class com.intellij.util.io.BaseOutputReader",
            "Inline Method private processLine(buffer char[], token StringBuilder, n int) : void inlined to protected readAvailableNonBlocking() : boolean in class com.intellij.util.io.BaseOutputReader");
          processTp("https://github.com/neo4j/neo4j.git", "5fa74fbb4a307571e3807c1201b8b05d3d60a99b", 
            "Extract Method private createCountsTracker(pageCache PageCache) : CountsTracker extracted from public shouldRotateCountsStoreWhenRotatingLog() : void in class org.neo4j.kernel.impl.store.counts.CountsRotationTest",
            "Extract Method private createCountsTracker(pageCache PageCache) : CountsTracker extracted from public shouldRotateCountsStoreWhenClosingTheDatabase() : void in class org.neo4j.kernel.impl.store.counts.CountsRotationTest",
            "Extract Method private createCountsTracker(pageCache PageCache) : CountsTracker extracted from public shouldCreateEmptyCountsTrackerStoreWhenCreatingDatabase() : void in class org.neo4j.kernel.impl.store.counts.CountsRotationTest",
            "Extract Method private createCountsTracker() : CountsTracker extracted from public shouldCreateACountStoreWhenDBContainsDenseNodes() : void in class org.neo4j.kernel.impl.store.counts.CountsComputerTest",
            "Extract Method private createCountsTracker() : CountsTracker extracted from public shouldCreateACountsStoreWhenThereAreNodesAndRelationshipsInTheDB() : void in class org.neo4j.kernel.impl.store.counts.CountsComputerTest",
            "Extract Method private createCountsTracker() : CountsTracker extracted from public shouldCreateACountsStoreWhenThereAreUnusedRelationshipRecordsInTheDB() : void in class org.neo4j.kernel.impl.store.counts.CountsComputerTest",
            "Extract Method private createCountsTracker() : CountsTracker extracted from public shouldCreateACountsStoreWhenThereAreUnusedNodeRecordsInTheDB() : void in class org.neo4j.kernel.impl.store.counts.CountsComputerTest",
            "Extract Method private createCountsTracker() : CountsTracker extracted from public shouldCreateACountsStoreWhenThereAreNodesInTheDB() : void in class org.neo4j.kernel.impl.store.counts.CountsComputerTest",
            "Extract Method private createCountsTracker() : CountsTracker extracted from public shouldCreateAnEmptyCountsStoreFromAnEmptyDatabase() : void in class org.neo4j.kernel.impl.store.counts.CountsComputerTest");


          processFp("https://github.com/structr/structr.git", "c4d9d3ed64d28ab0656937d96f2d1d538eceb9c7", 
            "Extract Method public getRelationshipById(uuid String) : RelationshipInterface extracted from public get(uuid String) : GraphObject in class org.structr.core.app.StructrApp",
            "Extract Method public getNodeById(uuid String) : NodeInterface extracted from public get(uuid String) : GraphObject in class org.structr.core.app.StructrApp");
          processTp("https://github.com/jline/jline2.git", "80d3ffb5aafa90992385c17e8338c2cc5def3cec", 
            "Extract Method public readCharacter(checkForAltKeyCombo boolean, allowed char[]) : int extracted from public readCharacter(allowed char[]) : int in class jline.console.ConsoleReader",
            "Extract Method public readCharacter(checkForAltKeyCombo boolean) : int extracted from public readCharacter() : int in class jline.console.ConsoleReader");

          processTp("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "9687048f76519fc89b4151cbe2841bbba61a401d", 
            "Extract Method protected getEntityForm(info DynamicEntityFormInfo, dynamicFormOverride EntityForm) : EntityForm extracted from protected getBlankDynamicFieldTemplateForm(info DynamicEntityFormInfo, dynamicFormOverride EntityForm) : EntityForm in class org.broadleafcommerce.openadmin.web.controller.AdminAbstractController");

          processTp("https://github.com/Netflix/feign.git", "b2b4085348de32f10903970dded99fdf0376a43c", 
            "Extract Method public configKey(targetType Class, method Method) : String extracted from public configKey(method Method) : String in class feign.Feign",
            "Extract Method private headersFromAnnotation(targetType Class<?>, data MethodMetadata) : void extracted from public parseAndValidatateMetadata(method Method) : MethodMetadata in class feign.Contract.Default",
            "Extract Method protected parseAndValidateMetadata(targetType Class<?>, method Method) : MethodMetadata extracted from public parseAndValidatateMetadata(method Method) : MethodMetadata in class feign.Contract.BaseContract");

          processTp("https://github.com/aws/aws-sdk-java.git", "14593c6379445f260baeb5287f618758da6d9952", 
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


          processFp("https://github.com/dropwizard/dropwizard.git", "cdecd49957e8a45dbfc7837c19b9c038880723b7", 
            "Move Attribute private clazz : Class<T> from class io.dropwizard.auth.AuthValueFactoryProvider.Binder to class io.dropwizard.auth.AuthValueFactoryProvider.PrincipalClassProvider");
          processTp("https://github.com/structr/structr.git", "15afd616cba5fb3d432d11a6de0d4f7805b202db", 
            "Extract Method package handleObject(nodeFactory NodeFactory, relFactory RelationshipFactory, key String, value Object, includeHiddenAndDeleted boolean, publicOnly boolean, level int) : Object extracted from public execute(query String, parameters Map<String,Object>, includeHiddenAndDeleted boolean, publicOnly boolean) : List<GraphObject> in class org.structr.core.graph.CypherQueryCommand");


          processFp("https://github.com/skylot/jadx.git", "bd4d4f49fff46492601bc7fe663c52089873218a", 
            "Inline Method private indexClass(cls JavaClass) : void inlined to private loadData() : void in class jadx.gui.ui.SearchDialog");
          processTp("https://github.com/hazelcast/hazelcast.git", "76d7f5e3fe4eb41b383c1d884bc1217b9fa7192e", 
            "Extract Method protected createAddress(host String, port int) : Address extracted from private createAddresses(count int) : Address[] in class com.hazelcast.test.TestHazelcastInstanceFactory",
            "Extract Method protected shutdownSelectors() : void extracted from public shutdown() : void in class com.hazelcast.client.connection.nio.ClientConnectionManagerImpl",
            "Extract Method protected startSelectors() : void extracted from public start() : void in class com.hazelcast.client.connection.nio.ClientConnectionManagerImpl",
            "Extract Method protected initializeSelectors(client HazelcastClientInstanceImpl) : void extracted from public ClientConnectionManagerImpl(client HazelcastClientInstanceImpl, addressTranslator AddressTranslator) in class com.hazelcast.client.connection.nio.ClientConnectionManagerImpl");

          processTp("https://github.com/geometer/FBReaderJ.git", "42e0649f82779ecd48bff6448924fc7dc2534554", 
            "Extract Method private allTopLevelNodes() : List<MenuNode> extracted from public topLevelNodes() : List<MenuNode> in class org.geometerplus.android.fbreader.MenuData");

          processTp("https://github.com/spring-projects/spring-security.git", "64938ebcfc2fc8cd9ccd6cf31dbcd8cdd0660aca", 
            "Extract Method public createExpressionMessageMetadataSource(matcherToExpression LinkedHashMap<MessageMatcher<?>,String>, handler SecurityExpressionHandler<Message<Object>>) : MessageSecurityMetadataSource extracted from public createExpressionMessageMetadataSource(matcherToExpression LinkedHashMap<MessageMatcher<?>,String>) : MessageSecurityMetadataSource in class org.springframework.security.messaging.access.expression.ExpressionBasedMessageSecurityMetadataSourceFactory");

          processTp("https://github.com/plutext/docx4j.git", "e29924b33ec0c0298ba4fc3f7a8c218c8e6cfa0c", 
            "Move Class org.apache.poi.util.TempFileCreationStrategy moved to org.docx4j.org.apache.poi.util.TempFileCreationStrategy",
            "Move Class org.apache.poi.util.TempFile moved to org.docx4j.org.apache.poi.util.TempFile",
            "Move Class org.apache.poi.util.StringUtil moved to org.docx4j.org.apache.poi.util.StringUtil",
            "Move Class org.apache.poi.util.ShortField moved to org.docx4j.org.apache.poi.util.ShortField",
            "Move Class org.apache.poi.util.SAXHelper moved to org.docx4j.org.apache.poi.util.SAXHelper",
            "Move Class org.apache.poi.util.RecordFormatException moved to org.docx4j.org.apache.poi.util.RecordFormatException",
            "Move Class org.apache.poi.util.PngUtils moved to org.docx4j.org.apache.poi.util.PngUtils",
            "Move Class org.apache.poi.util.LongField moved to org.docx4j.org.apache.poi.util.LongField",
            "Move Class org.apache.poi.util.LittleEndianOutputStream moved to org.docx4j.org.apache.poi.util.LittleEndianOutputStream",
            "Move Class org.apache.poi.util.LittleEndianOutput moved to org.docx4j.org.apache.poi.util.LittleEndianOutput",
            "Move Class org.apache.poi.util.LittleEndianInputStream moved to org.docx4j.org.apache.poi.util.LittleEndianInputStream",
            "Move Class org.apache.poi.util.LittleEndianInput moved to org.docx4j.org.apache.poi.util.LittleEndianInput",
            "Move Class org.apache.poi.util.LittleEndianConsts moved to org.docx4j.org.apache.poi.util.LittleEndianConsts",
            "Move Class org.apache.poi.util.LittleEndianByteArrayOutputStream moved to org.docx4j.org.apache.poi.util.LittleEndianByteArrayOutputStream",
            "Move Class org.apache.poi.util.LittleEndian moved to org.docx4j.org.apache.poi.util.LittleEndian",
            "Move Class org.apache.poi.util.IntegerField moved to org.docx4j.org.apache.poi.util.IntegerField",
            "Move Class org.apache.poi.util.IntList moved to org.docx4j.org.apache.poi.util.IntList",
            "Move Class org.apache.poi.util.IOUtils moved to org.docx4j.org.apache.poi.util.IOUtils",
            "Move Class org.apache.poi.util.HexRead moved to org.docx4j.org.apache.poi.util.HexRead",
            "Move Class org.apache.poi.util.HexDump moved to org.docx4j.org.apache.poi.util.HexDump",
            "Move Class org.apache.poi.util.FixedField moved to org.docx4j.org.apache.poi.util.FixedField",
            "Move Class org.apache.poi.util.DocumentHelper moved to org.docx4j.org.apache.poi.util.DocumentHelper",
            "Move Class org.apache.poi.util.DelayableLittleEndianOutput moved to org.docx4j.org.apache.poi.util.DelayableLittleEndianOutput",
            "Move Class org.apache.poi.util.CodePageUtil moved to org.docx4j.org.apache.poi.util.CodePageUtil",
            "Move Class org.apache.poi.util.CloseIgnoringInputStream moved to org.docx4j.org.apache.poi.util.CloseIgnoringInputStream",
            "Move Class org.apache.poi.util.ByteField moved to org.docx4j.org.apache.poi.util.ByteField",
            "Move Class org.apache.poi.util.BoundedInputStream moved to org.docx4j.org.apache.poi.util.BoundedInputStream",
            "Move Class org.apache.poi.util.BitFieldFactory moved to org.docx4j.org.apache.poi.util.BitFieldFactory",
            "Move Class org.apache.poi.util.BitField moved to org.docx4j.org.apache.poi.util.BitField",
            "Move Class org.apache.poi.poifs.storage.SmallDocumentBlockList moved to org.docx4j.org.apache.poi.poifs.storage.SmallDocumentBlockList",
            "Move Class org.apache.poi.poifs.storage.SmallBlockTableWriter moved to org.docx4j.org.apache.poi.poifs.storage.SmallBlockTableWriter",
            "Move Class org.apache.poi.poifs.storage.RawDataBlockList moved to org.docx4j.org.apache.poi.poifs.storage.RawDataBlockList",
            "Move Class org.apache.poi.poifs.storage.RawDataBlock moved to org.docx4j.org.apache.poi.poifs.storage.RawDataBlock",
            "Move Class org.apache.poi.poifs.storage.PropertyBlock moved to org.docx4j.org.apache.poi.poifs.storage.PropertyBlock",
            "Move Class org.apache.poi.poifs.storage.ListManagedBlock moved to org.docx4j.org.apache.poi.poifs.storage.ListManagedBlock",
            "Move Class org.apache.poi.poifs.storage.HeaderBlockWriter moved to org.docx4j.org.apache.poi.poifs.storage.HeaderBlockWriter",
            "Move Class org.apache.poi.poifs.storage.HeaderBlockConstants moved to org.docx4j.org.apache.poi.poifs.storage.HeaderBlockConstants",
            "Move Class org.apache.poi.poifs.storage.HeaderBlock moved to org.docx4j.org.apache.poi.poifs.storage.HeaderBlock",
            "Move Class org.apache.poi.poifs.storage.DocumentBlock moved to org.docx4j.org.apache.poi.poifs.storage.DocumentBlock",
            "Move Class org.apache.poi.poifs.storage.DataInputBlock moved to org.docx4j.org.apache.poi.poifs.storage.DataInputBlock",
            "Move Class org.apache.poi.poifs.storage.BlockWritable moved to org.docx4j.org.apache.poi.poifs.storage.BlockWritable",
            "Move Class org.apache.poi.poifs.storage.BlockListImpl moved to org.docx4j.org.apache.poi.poifs.storage.BlockListImpl",
            "Move Class org.apache.poi.poifs.storage.BlockList moved to org.docx4j.org.apache.poi.poifs.storage.BlockList",
            "Move Class org.apache.poi.poifs.storage.BlockAllocationTableWriter moved to org.docx4j.org.apache.poi.poifs.storage.BlockAllocationTableWriter",
            "Move Class org.apache.poi.poifs.storage.BlockAllocationTableReader moved to org.docx4j.org.apache.poi.poifs.storage.BlockAllocationTableReader",
            "Move Class org.apache.poi.poifs.storage.BigBlock moved to org.docx4j.org.apache.poi.poifs.storage.BigBlock",
            "Move Class org.apache.poi.poifs.storage.BATBlock.BATBlockAndIndex moved to org.docx4j.org.apache.poi.poifs.storage.BATBlock.BATBlockAndIndex",
            "Move Class org.apache.poi.poifs.property.RootProperty moved to org.docx4j.org.apache.poi.poifs.property.RootProperty",
            "Move Class org.apache.poi.poifs.property.PropertyTableBase moved to org.docx4j.org.apache.poi.poifs.property.PropertyTableBase",
            "Move Class org.apache.poi.poifs.property.PropertyTable moved to org.docx4j.org.apache.poi.poifs.property.PropertyTable",
            "Move Class org.apache.poi.poifs.property.PropertyFactory moved to org.docx4j.org.apache.poi.poifs.property.PropertyFactory",
            "Move Class org.apache.poi.poifs.property.PropertyConstants moved to org.docx4j.org.apache.poi.poifs.property.PropertyConstants",
            "Move Class org.apache.poi.poifs.property.Property moved to org.docx4j.org.apache.poi.poifs.property.Property",
            "Move Class org.apache.poi.poifs.property.Parent moved to org.docx4j.org.apache.poi.poifs.property.Parent",
            "Move Class org.apache.poi.poifs.property.NPropertyTable moved to org.docx4j.org.apache.poi.poifs.property.NPropertyTable",
            "Move Class org.apache.poi.poifs.property.DocumentProperty moved to org.docx4j.org.apache.poi.poifs.property.DocumentProperty",
            "Move Class org.apache.poi.poifs.property.DirectoryProperty moved to org.docx4j.org.apache.poi.poifs.property.DirectoryProperty",
            "Move Class org.apache.poi.poifs.property.Child moved to org.docx4j.org.apache.poi.poifs.property.Child",
            "Move Class org.apache.poi.poifs.nio.FileBackedDataSource moved to org.docx4j.org.apache.poi.poifs.nio.FileBackedDataSource",
            "Move Class org.apache.poi.poifs.nio.DataSource moved to org.docx4j.org.apache.poi.poifs.nio.DataSource",
            "Move Class org.apache.poi.poifs.nio.ByteArrayBackedDataSource moved to org.docx4j.org.apache.poi.poifs.nio.ByteArrayBackedDataSource",
            "Move Class org.apache.poi.poifs.filesystem.POIFSWriterListener moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSWriterListener",
            "Move Class org.apache.poi.poifs.filesystem.POIFSWriterEvent moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSWriterEvent",
            "Move Class org.apache.poi.poifs.filesystem.POIFSFileSystem moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSFileSystem",
            "Move Class org.apache.poi.poifs.filesystem.POIFSDocumentPath moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSDocumentPath",
            "Move Class org.apache.poi.poifs.filesystem.Ole10NativeException moved to org.docx4j.org.apache.poi.poifs.filesystem.Ole10NativeException",
            "Move Class org.apache.poi.poifs.filesystem.Ole10Native moved to org.docx4j.org.apache.poi.poifs.filesystem.Ole10Native",
            "Move Class org.apache.poi.poifs.filesystem.OfficeXmlFileException moved to org.docx4j.org.apache.poi.poifs.filesystem.OfficeXmlFileException",
            "Move Class org.apache.poi.poifs.filesystem.OPOIFSFileSystem moved to org.docx4j.org.apache.poi.poifs.filesystem.OPOIFSFileSystem",
            "Move Class org.apache.poi.poifs.filesystem.OPOIFSDocument moved to org.docx4j.org.apache.poi.poifs.filesystem.OPOIFSDocument",
            "Move Class org.apache.poi.poifs.filesystem.ODocumentInputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.ODocumentInputStream",
            "Move Class org.apache.poi.poifs.filesystem.NotOLE2FileException moved to org.docx4j.org.apache.poi.poifs.filesystem.NotOLE2FileException",
            "Move Class org.apache.poi.poifs.filesystem.NPOIFSStream moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSStream",
            "Move Class org.apache.poi.poifs.filesystem.NPOIFSMiniStore moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSMiniStore",
            "Move Class org.apache.poi.poifs.filesystem.NPOIFSFileSystem moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSFileSystem",
            "Move Class org.apache.poi.poifs.filesystem.NPOIFSDocument moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSDocument",
            "Move Class org.apache.poi.poifs.filesystem.NDocumentOutputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.NDocumentOutputStream",
            "Move Class org.apache.poi.poifs.filesystem.NDocumentInputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.NDocumentInputStream",
            "Move Class org.apache.poi.poifs.filesystem.FilteringDirectoryNode moved to org.docx4j.org.apache.poi.poifs.filesystem.FilteringDirectoryNode",
            "Move Class org.apache.poi.poifs.filesystem.EntryUtils moved to org.docx4j.org.apache.poi.poifs.filesystem.EntryUtils",
            "Move Class org.apache.poi.poifs.filesystem.EntryNode moved to org.docx4j.org.apache.poi.poifs.filesystem.EntryNode",
            "Move Class org.apache.poi.poifs.filesystem.Entry moved to org.docx4j.org.apache.poi.poifs.filesystem.Entry",
            "Move Class org.apache.poi.poifs.filesystem.DocumentOutputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentOutputStream",
            "Move Class org.apache.poi.poifs.filesystem.DocumentNode moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentNode",
            "Move Class org.apache.poi.poifs.filesystem.DocumentInputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentInputStream",
            "Move Class org.apache.poi.poifs.filesystem.DocumentEntry moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentEntry",
            "Move Class org.apache.poi.poifs.filesystem.DocumentDescriptor moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentDescriptor",
            "Move Class org.apache.poi.poifs.filesystem.DirectoryNode moved to org.docx4j.org.apache.poi.poifs.filesystem.DirectoryNode",
            "Move Class org.apache.poi.hpsf.VersionedStream moved to org.docx4j.org.apache.poi.hpsf.VersionedStream",
            "Move Class org.apache.poi.hpsf.Vector moved to org.docx4j.org.apache.poi.hpsf.Vector",
            "Move Class org.apache.poi.hpsf.VariantTypeException moved to org.docx4j.org.apache.poi.hpsf.VariantTypeException",
            "Move Class org.apache.poi.hpsf.VariantSupport moved to org.docx4j.org.apache.poi.hpsf.VariantSupport",
            "Move Class org.apache.poi.hpsf.VariantBool moved to org.docx4j.org.apache.poi.hpsf.VariantBool",
            "Move Class org.apache.poi.hpsf.Variant moved to org.docx4j.org.apache.poi.hpsf.Variant",
            "Move Class org.apache.poi.hpsf.Util moved to org.docx4j.org.apache.poi.hpsf.Util",
            "Move Class org.apache.poi.hpsf.UnsupportedVariantTypeException moved to org.docx4j.org.apache.poi.hpsf.UnsupportedVariantTypeException",
            "Move Class org.apache.poi.hpsf.UnicodeString moved to org.docx4j.org.apache.poi.hpsf.UnicodeString",
            "Move Class org.apache.poi.hpsf.UnexpectedPropertySetTypeException moved to org.docx4j.org.apache.poi.hpsf.UnexpectedPropertySetTypeException",
            "Move Class org.apache.poi.hpsf.TypedPropertyValue moved to org.docx4j.org.apache.poi.hpsf.TypedPropertyValue",
            "Move Class org.apache.poi.hpsf.TypeWriter moved to org.docx4j.org.apache.poi.hpsf.TypeWriter",
            "Move Class org.apache.poi.hpsf.SummaryInformation moved to org.docx4j.org.apache.poi.hpsf.SummaryInformation",
            "Move Class org.apache.poi.hpsf.SpecialPropertySet moved to org.docx4j.org.apache.poi.hpsf.SpecialPropertySet",
            "Move Class org.apache.poi.hpsf.Section moved to org.docx4j.org.apache.poi.hpsf.Section",
            "Move Class org.apache.poi.hpsf.ReadingNotSupportedException moved to org.docx4j.org.apache.poi.hpsf.ReadingNotSupportedException",
            "Move Class org.apache.poi.hpsf.PropertySetFactory moved to org.docx4j.org.apache.poi.hpsf.PropertySetFactory",
            "Move Class org.apache.poi.hpsf.PropertySet moved to org.docx4j.org.apache.poi.hpsf.PropertySet",
            "Move Class org.apache.poi.hpsf.Property moved to org.docx4j.org.apache.poi.hpsf.Property",
            "Move Class org.apache.poi.hpsf.NoSingleSectionException moved to org.docx4j.org.apache.poi.hpsf.NoSingleSectionException",
            "Move Class org.apache.poi.hpsf.NoPropertySetStreamException moved to org.docx4j.org.apache.poi.hpsf.NoPropertySetStreamException",
            "Move Class org.apache.poi.hpsf.NoFormatIDException moved to org.docx4j.org.apache.poi.hpsf.NoFormatIDException",
            "Move Class org.apache.poi.hpsf.MutableSection moved to org.docx4j.org.apache.poi.hpsf.MutableSection",
            "Move Class org.apache.poi.hpsf.MutablePropertySet moved to org.docx4j.org.apache.poi.hpsf.MutablePropertySet",
            "Move Class org.apache.poi.hpsf.CustomProperty moved to org.docx4j.org.apache.poi.hpsf.CustomProperty",
            "Move Class org.apache.poi.hpsf.CustomProperties moved to org.docx4j.org.apache.poi.hpsf.CustomProperties",
            "Move Class org.apache.poi.hpsf.Currency moved to org.docx4j.org.apache.poi.hpsf.Currency",
            "Move Class org.apache.poi.hpsf.CodePageString moved to org.docx4j.org.apache.poi.hpsf.CodePageString",
            "Move Class org.apache.poi.hpsf.ClipboardData moved to org.docx4j.org.apache.poi.hpsf.ClipboardData",
            "Move Class org.apache.poi.hpsf.ClassID moved to org.docx4j.org.apache.poi.hpsf.ClassID",
            "Move Class org.apache.poi.hpsf.Blob moved to org.docx4j.org.apache.poi.hpsf.Blob",
            "Move Class org.apache.poi.hpsf.Array moved to org.docx4j.org.apache.poi.hpsf.Array",
            "Move Class org.apache.poi.UnsupportedFileFormatException moved to org.docx4j.org.apache.poi.UnsupportedFileFormatException",
            "Move Class org.apache.poi.OldFileFormatException moved to org.docx4j.org.apache.poi.OldFileFormatException",
            "Move Class org.apache.poi.EncryptedDocumentException moved to org.docx4j.org.apache.poi.EncryptedDocumentException",
            "Move Class org.apache.poi.EmptyFileException moved to org.docx4j.org.apache.poi.EmptyFileException",
            "Move Class org.apache.poi.hpsf.Date moved to org.docx4j.org.apache.poi.hpsf.Date",
            "Move Class org.apache.poi.hpsf.Decimal moved to org.docx4j.org.apache.poi.hpsf.Decimal",
            "Move Class org.apache.poi.hpsf.DocumentSummaryInformation moved to org.docx4j.org.apache.poi.hpsf.DocumentSummaryInformation",
            "Move Class org.apache.poi.hpsf.Filetime moved to org.docx4j.org.apache.poi.hpsf.Filetime",
            "Move Class org.apache.poi.hpsf.GUID moved to org.docx4j.org.apache.poi.hpsf.GUID",
            "Move Class org.apache.poi.hpsf.HPSFException moved to org.docx4j.org.apache.poi.hpsf.HPSFException",
            "Move Class org.apache.poi.hpsf.HPSFRuntimeException moved to org.docx4j.org.apache.poi.hpsf.HPSFRuntimeException",
            "Move Class org.apache.poi.hpsf.IllegalPropertySetDataException moved to org.docx4j.org.apache.poi.hpsf.IllegalPropertySetDataException",
            "Move Class org.apache.poi.hpsf.IndirectPropertyName moved to org.docx4j.org.apache.poi.hpsf.IndirectPropertyName",
            "Move Class org.apache.poi.hpsf.MarkUnsupportedException moved to org.docx4j.org.apache.poi.hpsf.MarkUnsupportedException",
            "Move Class org.apache.poi.hpsf.MissingSectionException moved to org.docx4j.org.apache.poi.hpsf.MissingSectionException",
            "Move Class org.apache.poi.hpsf.MutableProperty moved to org.docx4j.org.apache.poi.hpsf.MutableProperty",
            "Move Class org.apache.poi.hpsf.WritingNotSupportedException moved to org.docx4j.org.apache.poi.hpsf.WritingNotSupportedException",
            "Move Class org.apache.poi.hpsf.wellknown.PropertyIDMap moved to org.docx4j.org.apache.poi.hpsf.wellknown.PropertyIDMap",
            "Move Class org.apache.poi.hpsf.wellknown.SectionIDMap moved to org.docx4j.org.apache.poi.hpsf.wellknown.SectionIDMap",
            "Move Class org.apache.poi.hssf.OldExcelFormatException moved to org.docx4j.org.apache.poi.hssf.OldExcelFormatException",
            "Move Class org.apache.poi.poifs.common.POIFSBigBlockSize moved to org.docx4j.org.apache.poi.poifs.common.POIFSBigBlockSize",
            "Move Class org.apache.poi.poifs.common.POIFSConstants moved to org.docx4j.org.apache.poi.poifs.common.POIFSConstants",
            "Move Class org.apache.poi.poifs.crypt.ChunkedCipherInputStream moved to org.docx4j.org.apache.poi.poifs.crypt.ChunkedCipherInputStream",
            "Move Class org.apache.poi.poifs.crypt.ChunkedCipherOutputStream moved to org.docx4j.org.apache.poi.poifs.crypt.ChunkedCipherOutputStream",
            "Move Class org.apache.poi.poifs.crypt.CryptoFunctions moved to org.docx4j.org.apache.poi.poifs.crypt.CryptoFunctions",
            "Move Class org.apache.poi.poifs.crypt.DataSpaceMapUtils moved to org.docx4j.org.apache.poi.poifs.crypt.DataSpaceMapUtils",
            "Move Class org.apache.poi.poifs.crypt.Decryptor moved to org.docx4j.org.apache.poi.poifs.crypt.Decryptor",
            "Move Class org.apache.poi.poifs.crypt.EncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionHeader",
            "Move Class org.apache.poi.poifs.crypt.EncryptionInfo moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionInfo",
            "Move Class org.apache.poi.poifs.crypt.EncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionInfoBuilder",
            "Move Class org.apache.poi.poifs.crypt.EncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionVerifier",
            "Move Class org.apache.poi.poifs.crypt.Encryptor moved to org.docx4j.org.apache.poi.poifs.crypt.Encryptor",
            "Move Class org.apache.poi.poifs.crypt.agile.AgileDecryptor moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileDecryptor",
            "Move Class org.apache.poi.poifs.crypt.agile.AgileEncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileEncryptionHeader",
            "Move Class org.apache.poi.poifs.crypt.agile.AgileEncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileEncryptionVerifier",
            "Move Class org.apache.poi.poifs.crypt.agile.AgileEncryptor moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileEncryptor",
            "Move Class org.apache.poi.poifs.crypt.agile.EncryptionDocument moved to org.docx4j.org.apache.poi.poifs.crypt.agile.EncryptionDocument",
            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Decryptor moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Decryptor",
            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionHeader",
            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionInfoBuilder",
            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionVerifier",
            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Encryptor moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Encryptor",
            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIDecryptor moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIDecryptor",
            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionHeader",
            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionInfoBuilder",
            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionVerifier",
            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptor moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptor",
            "Move Class org.apache.poi.poifs.crypt.standard.EncryptionRecord moved to org.docx4j.org.apache.poi.poifs.crypt.standard.EncryptionRecord",
            "Move Class org.apache.poi.poifs.crypt.standard.StandardDecryptor moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardDecryptor",
            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptionHeader",
            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptionInfoBuilder",
            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptionVerifier",
            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptor moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptor",
            "Move Class org.apache.poi.poifs.dev.POIFSLister moved to org.docx4j.org.apache.poi.poifs.dev.POIFSLister",
            "Move Class org.apache.poi.poifs.dev.POIFSViewEngine moved to org.docx4j.org.apache.poi.poifs.dev.POIFSViewEngine",
            "Move Class org.apache.poi.poifs.dev.POIFSViewable moved to org.docx4j.org.apache.poi.poifs.dev.POIFSViewable",
            "Move Class org.apache.poi.poifs.dev.POIFSViewer moved to org.docx4j.org.apache.poi.poifs.dev.POIFSViewer",
            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReader moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReader",
            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent",
            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReaderListener moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReaderListener",
            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReaderRegistry moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReaderRegistry",
            "Move Class org.apache.poi.poifs.filesystem.BATManaged moved to org.docx4j.org.apache.poi.poifs.filesystem.BATManaged",
            "Move Class org.apache.poi.poifs.filesystem.BlockStore moved to org.docx4j.org.apache.poi.poifs.filesystem.BlockStore",
            "Move Class org.apache.poi.poifs.filesystem.DirectoryEntry moved to org.docx4j.org.apache.poi.poifs.filesystem.DirectoryEntry",
            "Extract Method public save(outStream OutputStream, flags int, password String) : void extracted from public save(outStream OutputStream, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage",
            "Extract Method public save(outFile File, flags int, password String) : void extracted from public save(outStream OutputStream, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage",
            "Extract Method public save(outFile File, flags int, password String) : void extracted from public save(outFile File, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage");

          processTp("https://github.com/springfox/springfox.git", "2307ff3a4ca367aaf64088b7b1e1bbf212c9bf3a", 
            "Move Class springfox.documentation.spring.web.RelativePathProvider moved to springfox.documentation.spring.web.paths.RelativePathProvider",
            "Move Class springfox.documentation.spring.web.Paths moved to springfox.documentation.spring.web.paths.Paths",
            "Move Class springfox.documentation.spring.web.AbstractPathProvider moved to springfox.documentation.spring.web.paths.AbstractPathProvider");

          processTp("https://github.com/wordpress-mobile/WordPress-Android.git", "1b21ba4bcea986988d4bbd578e3bb9a20ec69606", 
            "Extract Method private privacyStringForValue(value int) : String extracted from public onPreferenceChange(preference Preference, newValue Object) : boolean in class org.wordpress.android.ui.prefs.SiteSettingsFragment");
          processFp("https://github.com/wordpress-mobile/WordPress-Android.git", "1b21ba4bcea986988d4bbd578e3bb9a20ec69606", 
            "Inline Method private getLanguageString(languagueCode String) : String inlined to public onPreferenceChange(preference Preference, newValue Object) : boolean in class org.wordpress.android.ui.prefs.SiteSettingsFragment");
          processTp("https://github.com/neo4j/neo4j.git", "a26b61201cd86c9a8773b418d9c84b446e95a601", 
            "Move Method public arrayAsCollection(arrayValue Object) : Collection<Object> from class org.neo4j.kernel.impl.util.IoPrimitiveUtils to public arrayAsCollection(arrayValue Object) : Collection<Object> from class org.neo4j.graphdb.Neo4jMatchers");

          processTp("https://github.com/robovm/robovm.git", "7837d0baf1aef45340eec699516a8c3a22aeb553", 
            "Extract Method private signFrameworks(appDir File, getTaskAllow boolean) : void extracted from protected prepareLaunch(appDir File) : void in class org.robovm.compiler.target.ios.IOSTarget");

          processTp("https://github.com/JetBrains/intellij-community.git", "af618666043f21b3db7e6a1be2aa225ae0f432b4", 
            "Extract Method private isNavigationBlocked(renderer GutterIconRenderer, project Project) : boolean extracted from public mouseReleased(e MouseEvent) : void in class com.intellij.openapi.editor.impl.EditorGutterComponentImpl");

          processTp("https://github.com/droolsjbpm/drools.git", "7ffc62aa554f5884064b81ee80078e35e3833006", 
            "Extract Method protected addInterceptor(interceptor Interceptor, store boolean) : void extracted from public addInterceptor(interceptor Interceptor) : void in class org.drools.persistence.SingleSessionCommandService");

          processTp("https://github.com/Activiti/Activiti.git", "a70ca1d9ad2ea07b19c5e1f9540c809d7a12d3fb", 
            "Extract Method protected flushPersistentObjects(persistentObjectClass Class<? extends PersistentObject>, persistentObjectsToInsert List<PersistentObject>) : void extracted from protected flushInserts() : void in class org.activiti.engine.impl.db.DbSqlSession");

          processTp("https://github.com/spring-projects/spring-boot.git", "84937551787072a4befac29fb48436b3187ac4c6", 
            "Move Class org.springframework.boot.cli.compiler.grape.SettingsXmlRepositorySystemSessionAutoConfiguration.SpringBootSecDispatcher moved to org.springframework.boot.cli.compiler.MavenSettingsReader.SpringBootSecDispatcher");

          processTp("https://github.com/Activiti/Activiti.git", "ca7d0c3b33a0863bed04c77932b9ef6b1317f34e", 
            "Move Class org.activiti.engine.impl.persistence.entity.UserEntityTest moved to org.activiti.engine.test.api.identity.UserEntityTest");

          processTp("https://github.com/JetBrains/intellij-community.git", "106d1d51754f454fa673976665e41f463316e084", 
            "Extract Method private dummy(builder PsiBuilder) : void extracted from public parseTypeParameter(builder PsiBuilder) : Marker in class com.intellij.lang.java.parser.ReferenceParser");

          processTp("https://github.com/facebook/buck.git", "8184a32a019b2ed956e8f24c18cb49a266af47bf", 
            "Extract Method private generateSingleCopyFilesBuildPhase(target PBXNativeTarget, destinationSpec CopyFilePhaseDestinationSpec, targetNodes Iterable<TargetNode<?>>) : void extracted from private generateCopyFilesBuildPhases(target PBXNativeTarget, copiedNodes Iterable<TargetNode<?>>) : void in class com.facebook.buck.apple.ProjectGenerator");

          processTp("https://github.com/linkedin/rest.li.git", "ec5ea36faa3dd74585bb339beabdba6149ed63be", 
            "Extract Method private buildErrorResponse(result RestLiServiceException, errorResponseFormat ErrorResponseFormat) : ErrorResponse extracted from public buildErrorResponse(result RestLiServiceException) : ErrorResponse in class com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder");
          processFp("https://github.com/linkedin/rest.li.git", "ec5ea36faa3dd74585bb339beabdba6149ed63be", 
            "Extract Method private createEntityResponse(entityTemplate RecordTemplate, routingResult RoutingResult) : EntityResponse<RecordTemplate> extracted from public buildRestLiResponseData(request RestRequest, routingResult RoutingResult, result Object, headers Map<String,String>) : AugmentedRestLiResponseData in class com.linkedin.restli.internal.server.methods.response.BatchGetResponseBuilder");

          processFp("https://github.com/ReactiveX/RxJava.git", "0420193e7c075c4fa7986fe03e96fe4a92952cde", 
            "Move Method public onStart() : void from class rx.internal.operators.OperatorMerge.MergeSubscriber to public onStart() : void from class rx.internal.operators.OperatorMerge.InnerSubscriber",
            "Move Method private OperatorMerge() from class rx.internal.operators.OperatorMerge to private OperatorMerge(delayErrors boolean, maxConcurrent int) from class rx.internal.operators.OperatorMerge",
            "Extract Method public requestMore(n long) : void extracted from private handleScalarSynchronousObservableWithoutRequestLimits(t ScalarSynchronousObservable<? extends T>) : void in class rx.internal.operators.OperatorMerge.MergeSubscriber",
            "Extract Method public requestMore(n long) : void extracted from private handleNewSource(t Observable<? extends T>) : void in class rx.internal.operators.OperatorMerge.MergeSubscriber",
            "Extract Method protected emitScalar(subscriber InnerSubscriber<T>, value T, r long) : void extracted from private handleScalarSynchronousObservableWithRequestLimits(t ScalarSynchronousObservable<? extends T>) : void in class rx.internal.operators.OperatorMerge.MergeSubscriber");
          processTp("https://github.com/apache/hive.git", "999e0e3616525d77cf46c5865f9981b5a6b5609a", 
            "Extract Method private hepPlan(basePlan RelNode, followPlanChanges boolean, mdProvider RelMetadataProvider, order HepMatchOrder, rules RelOptRule[]) : RelNode extracted from private hepPlan(basePlan RelNode, followPlanChanges boolean, mdProvider RelMetadataProvider, rules RelOptRule[]) : RelNode in class org.apache.hadoop.hive.ql.parse.CalcitePlanner.CalcitePlannerAction");


          processFp("https://github.com/infinispan/infinispan.git", "4445ee17e8aca49d47bee4a495d8bc0a9cbee3ad", 
            "Extract Interface org.infinispan.server.infinispan.spi.CacheContainer from classes [org.jboss.as.clustering.infinispan.DefaultCacheContainer]");
          processTp("https://github.com/infinispan/infinispan.git", "8f446b6ddf540e1b1fefca34dd10f45ba7256095", 
            "Move Class org.jboss.as.clustering.jgroups.ProtocolConfiguration moved to org.infinispan.server.jgroups.spi.ProtocolConfiguration",
            "Move Class org.jboss.as.clustering.jgroups.SaslClientCallbackHandler moved to org.infinispan.server.jgroups.security.SaslClientCallbackHandler",
            "Move Class org.jboss.as.clustering.jgroups.RealmAuthorizationCallbackHandler moved to org.infinispan.server.jgroups.security.RealmAuthorizationCallbackHandler",
            "Move Class org.jboss.as.clustering.jgroups.TopologyAddressGenerator moved to org.infinispan.server.jgroups.TopologyAddressGenerator",
            "Move Class org.jboss.as.clustering.jgroups.ProtocolDefaults moved to org.infinispan.server.jgroups.ProtocolDefaults",
            "Move Class org.jboss.as.clustering.jgroups.ManagedSocketFactory moved to org.infinispan.server.jgroups.ManagedSocketFactory",
            "Move Class org.jboss.as.clustering.jgroups.LogFactory moved to org.infinispan.server.jgroups.LogFactory",
            "Move Class org.jboss.as.clustering.jgroups.ChannelFactory moved to org.infinispan.server.jgroups.ChannelFactory",
            "Move Class org.jboss.as.clustering.jgroups.subsystem.ClusteringSubsystemTest moved to org.infinispan.server.commons.subsystem.ClusteringSubsystemTest",
            "Move Class org.jboss.as.clustering.jgroups.ServiceContainerHelper moved to org.infinispan.server.commons.msc.ServiceContainerHelper",
            "Move Class org.jboss.as.clustering.concurrent.ManagedScheduledExecutorService moved to org.infinispan.server.commons.concurrent.ManagedScheduledExecutorService",
            "Move Class org.jboss.as.clustering.concurrent.ManagedExecutorService moved to org.infinispan.server.commons.concurrent.ManagedExecutorService",
            "Move Class org.jboss.as.clustering.jgroups.ProtocolStackConfiguration moved to org.infinispan.server.jgroups.spi.ProtocolStackConfiguration",
            "Move Class org.jboss.as.clustering.jgroups.RelayConfiguration moved to org.infinispan.server.jgroups.spi.RelayConfiguration",
            "Move Class org.jboss.as.clustering.jgroups.RemoteSiteConfiguration moved to org.infinispan.server.jgroups.spi.RemoteSiteConfiguration",
            "Move Class org.jboss.as.clustering.jgroups.SaslConfiguration moved to org.infinispan.server.jgroups.spi.SaslConfiguration",
            "Move Class org.jboss.as.clustering.jgroups.TransportConfiguration moved to org.infinispan.server.jgroups.spi.TransportConfiguration",
            "Move Class org.jboss.as.clustering.jgroups.subsystem.ExportNativeConfiguration moved to org.infinispan.server.jgroups.subsystem.ExportNativeConfiguration",
            "Move Class org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension moved to org.infinispan.server.jgroups.subsystem.JGroupsExtension",
            "Move Class org.jboss.as.clustering.jgroups.subsystem.ModelKeys moved to org.infinispan.server.jgroups.subsystem.ModelKeys",
            "Move Class org.jboss.as.clustering.jgroups.subsystem.RelayResourceDefinition moved to org.infinispan.server.jgroups.subsystem.RelayResourceDefinition",
            "Move Class org.jboss.as.clustering.jgroups.subsystem.SaslResourceDefinition moved to org.infinispan.server.jgroups.subsystem.SaslResourceDefinition",
            "Move Class org.jboss.as.clustering.jgroups.ManagedSocketFactoryTest moved to org.infinispan.server.jgroups.ManagedSocketFactoryTest",
            "Move Class org.jboss.as.clustering.jgroups.subsystem.OperationSequencesTestCase moved to org.infinispan.server.jgroups.subsystem.OperationSequencesTestCase");

          processTp("https://github.com/JetBrains/intellij-community.git", "9f7de200c9aef900596b09327a52d33241a68d9c", 
            "Extract Method private dummy(builder PsiBuilder) : void extracted from public parseTypeParameter(builder PsiBuilder) : Marker in class com.intellij.lang.java.parser.ReferenceParser");

          processTp("https://github.com/JetBrains/intellij-community.git", "a9379ee529ed87e28c0736c3c6657dcd6a0680e4", 
            "Extract Method private toCanonicalPath(path String, separatorChar char, removeLastSlash boolean, resolveSymlinksIfNecessary boolean) : String extracted from private toCanonicalPath(path String, separatorChar char, removeLastSlash boolean) : String in class com.intellij.openapi.util.io.FileUtil");
          processFp("https://github.com/JetBrains/intellij-community.git", "a9379ee529ed87e28c0736c3c6657dcd6a0680e4", 
            "Extract Method private processDots(result StringBuilder, dots int, start int, resolveSymlinksIfNecessary boolean) : boolean extracted from private processDots(result StringBuilder, dots int, start int) : void in class com.intellij.openapi.util.io.FileUtil");
          processTp("https://github.com/spring-projects/spring-security.git", "08b1b56e2cd5ad72126f4bbeb15a47d9b104dfff", 
            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextServletOutputStream moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextServletOutputStream",
            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextPrintWriter moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextPrintWriter");

          processTp("https://github.com/spring-projects/spring-security.git", "fcc9a34356817d93c24b5ccf3107ec234a28b136", 
            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextServletOutputStream moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextServletOutputStream",
            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextPrintWriter moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextPrintWriter");

          processTp("https://github.com/zeromq/jeromq.git", "02d3fa171d02c9d82c7bdcaeb739f47d0c0006a0", 
            "Inline Method private makeFdPair() : void inlined to public Signaler() in class zmq.Signaler");


          processFp("https://github.com/eucalyptus/eucalyptus.git", "c8613623d8b7c786de54ce484c4ef0dc59c8f7ad", 
            "Inline Method private isValidServoRequest(instanceId String, remoteHost String) : void inlined to public describeLoadBalancersByServo(request DescribeLoadBalancersByServoType) : DescribeLoadBalancersByServoResponseType in class com.eucalyptus.loadbalancing.backend.LoadBalancingBackendService");
          processTp("https://github.com/antlr/antlr4.git", "b395127e733b33c27f344695ebf155ecf5edeeab", 
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
          processFp("https://github.com/antlr/antlr4.git", "b395127e733b33c27f344695ebf155ecf5edeeab", 
            "Inline Method public toStringTree(t Tree, nodeTextProvider TreeTextProvider) : String inlined to public toStringTree(t Tree, ruleNames List<String>) : String in class org.antlr.v4.runtime.tree.Trees");

          processFp("https://github.com/raphw/byte-buddy.git", "adc14c0ed9168c2f0d954e55d698aace8287d910", 
            "Extract Superclass net.bytebuddy.description.type.AbstractTypeListTest from classes [net.bytebuddy.pool.TypePoolLazyTypeListTest]",
            "Extract Superclass net.bytebuddy.description.method.AbstractMethodListTest from classes [net.bytebuddy.pool.TypePoolLazyMethodListTest]",
            "Extract Superclass net.bytebuddy.description.field.AbstractFieldListTest from classes [net.bytebuddy.pool.TypePoolLazyFieldListTest]",
            "Extract Superclass net.bytebuddy.description.annotation.AbstractAnnotationListTest from classes [net.bytebuddy.pool.TypePoolLazyAnnotationListTest]");
          processTp("https://github.com/AdoptOpenJDK/jitwatch.git", "3b1f4e56fea289860b31ef83ccfe96a3a003cc8b", 
            "Extract Method private visitTagEliminateAllocation(tag Tag, parseDictionary IParseDictionary) : void extracted from public visitTag(tag Tag, parseDictionary IParseDictionary) : void in class org.adoptopenjdk.jitwatch.model.bytecode.BytecodeAnnotationBuilder",
            "Extract Method private visitTagParse(tag Tag, parseDictionary IParseDictionary) : void extracted from public visitTag(tag Tag, parseDictionary IParseDictionary) : void in class org.adoptopenjdk.jitwatch.model.bytecode.BytecodeAnnotationBuilder");

          processTp("https://github.com/facebook/presto.git", "cb2deceea993128c22710b0f64f1b755c9d176f7", 
            "Move Method private flipComparison(type Type) : Type from class com.facebook.presto.sql.planner.RelationPlanner to public flipComparison(type Type) : Type from class com.facebook.presto.sql.ExpressionUtils");

          processTp("https://github.com/facebook/buck.git", "52cfd39ecba349c4d8e2c46eac76ed4d75b7ebae", 
            "Extract Method private createSymLinkSdks(sdks Iterable<String>, root Path, version String) : void extracted from private createSymLinkIosSdks(root Path, version String) : void in class com.facebook.buck.apple.AppleSdkDiscoveryTest");


          processFp("https://github.com/facebook/buck.git", "26288d076d16438a5dea45dff6fbc06671a394e2", 
            "Extract Method private assertExpandsTo(rule BuildRule, buildRuleResolver BuildRuleResolver, expectedClasspath String) : void extracted from public shouldIncludeTransitiveDependencies() : void in class com.facebook.buck.rules.macros.ClasspathMacroExpanderTest",
            "Extract Method private assertExpandsTo(rule BuildRule, buildRuleResolver BuildRuleResolver, expectedClasspath String) : void extracted from public shouldIncludeARuleIfNothingIsGiven() : void in class com.facebook.buck.rules.macros.ClasspathMacroExpanderTest");

          processFp("https://github.com/couchbase/couchbase-lite-android.git", "f3ff31223c72880803a44e2bbbe0fb06ec926402", 
            "Extract Method private getAttachmentsStub(name String) : Map<String,Map<String,Object>> extracted from public testAttachments() : void in class com.couchbase.lite.AttachmentsTest");
          processTp("https://github.com/vaadin/vaadin.git", "0f9d0b0bf1cd5fb58f47f22bd6d52a9fac31c530", 
            "Extract Method protected createGrid(container PersonContainer) : Grid extracted from protected setup(request VaadinRequest) : void in class com.vaadin.tests.components.grid.GridEditorUI",
            "Extract Method private getVisibleFrozenColumnCount() : int extracted from private updateFrozenColumns() : void in class com.vaadin.client.widgets.Grid");


          processFp("https://github.com/vaadin/vaadin.git", "b5365d5cca1a25efdb0b80855d28d20da71111d1", 
            "Extract Method protected getTrueString(locale Locale) : String extracted from public convertToPresentation(value Boolean, targetType Class<? extends String>, locale Locale) : String in class com.vaadin.data.util.converter.StringToBooleanConverter",
            "Extract Method protected getFalseString(locale Locale) : String extracted from public convertToPresentation(value Boolean, targetType Class<? extends String>, locale Locale) : String in class com.vaadin.data.util.converter.StringToBooleanConverter");
          processTp("https://github.com/JetBrains/intellij-community.git", "7c59f2a4f9b03a9e48ca15554291a03477aa19c1", 
            "Extract Method public addJarsToRoots(jarPaths List<String>, libraryName String, module Module, location PsiElement) : void extracted from public addJarToRoots(libPath String, module Module, location PsiElement) : void in class com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix");

          processTp("https://github.com/plutext/docx4j.git", "1ba361438ab4d7f6a0305428ba40ba62e2e6ff3c", 
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.ObjectFactory moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.ObjectFactory",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTVbaSuppData moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTVbaSuppData",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTMcds moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTMcds",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTMcd moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTMcd",
            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTDocEvents moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTDocEvents");


          processFp("https://github.com/apache/camel.git", "8e07c18c3537049962ccea7b075061c5560f03ec", 
            "Extract Superclass org.apache.camel.component.undertow.BaseUndertowTest from classes [org.apache.camel.component.undertow.UndertowSharedPortTest]");
          processTp("https://github.com/JetBrains/intellij-community.git", "10f769a60c7c7b73982e978959d381df487bbe2d", 
            "Move Method private getJUnit4JarPaths() : List<String> from class com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix to public getJUnit4JarPaths() : List<String> from class com.intellij.openapi.projectRoots.ex.JavaSdkUtil",
            "Extract Method public getLibraryPaths() : List<String> extracted from public getLibraryPath() : String in class com.intellij.execution.junit.JUnit4Framework");


          processFp("https://github.com/spring-projects/spring-loaded.git", "d2fc21dc1dd8806429dde964bcfd6c2417a423bd", 
            "Extract Method private couldBeReloadable(slashedName String) : CouldBeReloadableDecision extracted from private couldBeReloadable(slashedName String) : boolean in class org.springsource.loaded.TypeRegistry");
          processTp("https://github.com/JetBrains/intellij-community.git", "619a6012da868d0d42d9628460f2264effe9bdba", 
            "Extract Method private fillWithScopeExpansion(elements Set<Object>, pattern String) : boolean extracted from public computeInReadAction(indicator ProgressIndicator) : void in class com.intellij.ide.util.gotoByName.ChooseByNameBase.CalcElementsThread");

          processTp("https://github.com/processing/processing.git", "d403a0b2322a74dde824094d67b7997c1c371883", 
            "Move Class processing.app.contrib.UpdateStatusPanel moved to processing.app.contrib.UpdateContributionTab.UpdateStatusPanel",
            "Move Class processing.app.contrib.UpdateContribListingPanel moved to processing.app.contrib.UpdateContributionTab.UpdateContribListingPanel");


          processFp("https://github.com/facebook/swift.git", "0ca5daea2e7fd5b5616f30cc0ba3cabaa2fd7f7a", 
            "Inline Method private export(typeElement TypeElement) : void inlined to public process(annotations Set<? extends TypeElement>, roundEnv RoundEnvironment) : boolean in class com.facebook.swift.javadoc.JavaDocProcessor");
          processTp("https://github.com/JetBrains/intellij-community.git", "e1f0dbc2f09541fc64ce88ee22d8f8f4648004fe", 
            "Extract Method public resolveAndDownloadImpl(project Project, coord String, attachJavaDoc boolean, attachSources boolean, copyTo String, repositories List<MavenRepositoryInfo>) : List<OrderRoot> extracted from public resolveAndDownload(project Project, coord String, attachJavaDoc boolean, attachSources boolean, copyTo String, repositories List<MavenRepositoryInfo>) : NewLibraryConfiguration in class org.jetbrains.idea.maven.utils.library.RepositoryAttachHandler");

          processTp("https://github.com/apache/hive.git", "240097b78b70172e1cf9bc37876a566ddfb9e115", 
            "Move Method public getAcidEventFields() : List<String> from class org.apache.hadoop.hive.ql.io.orc.OrcRecordUpdater to package getAcidEventFields() : List<String> from class org.apache.hadoop.hive.ql.io.orc.RecordReaderFactory");

          processTp("https://github.com/jMonkeyEngine/jmonkeyengine.git", "5989711f7315abe4c3da0f1516a3eb3a81da6716", 
            "Extract Method protected movePanel(xoffset int, yoffset int) : void extracted from public mouseDragged(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel",
            "Extract Method protected saveLocation() : void extracted from public mousePressed(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel");
          processFp("https://github.com/jMonkeyEngine/jmonkeyengine.git", "5989711f7315abe4c3da0f1516a3eb3a81da6716", 
            "Extract Method protected removeSelectedConnection(selectedItem Selectable) : void extracted from protected removeSelectedConnection() : void in class com.jme3.gde.materialdefinition.editor.Diagram");
          processTp("https://github.com/facebook/presto.git", "8b1f5ce432bd6f579c646705d79ff0c4690495ae", 
            "Extract Method public checkArrayIndex(index long) : void extracted from public readBlockAndCheckIndex(array Slice, index long) : Block in class com.facebook.presto.operator.scalar.ArraySubscriptOperator");

          processTp("https://github.com/hazelcast/hazelcast.git", "30c4ae09745d6062077925a54f27205b7401d8df", 
            "Extract Method private renderHeap() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderSwap() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderPhysicalMemory() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderProcessors() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderProxy() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderClient() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderConnection() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderEvents() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderCluster() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderThread() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderGc() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderNativeMemory() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderExecutors() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method private renderOperationService() : void extracted from public toString() : String in class com.hazelcast.internal.monitors.HealthMonitor.HealthMetrics",
            "Extract Method package getConnectionAddress() : Object extracted from public close(t Throwable) : void in class com.hazelcast.nio.tcp.TcpIpConnection");
          processFp("https://github.com/hazelcast/hazelcast.git", "30c4ae09745d6062077925a54f27205b7401d8df", 
            "Move Class com.hazelcast.internal.metrics.impl.GaugeImplTest.SomeObject moved to com.hazelcast.internal.metrics.impl.DoubleGaugeImplTest.SomeObject",
            "Extract Interface com.hazelcast.internal.metrics.ProbeFunction from classes [com.hazelcast.internal.metrics.DoubleProbeFunction, com.hazelcast.internal.metrics.LongProbeFunction, com.hazelcast.internal.metrics.impl.FieldProbe, com.hazelcast.internal.metrics.impl.MethodProbe]");
          processTp("https://github.com/jersey/jersey.git", "d57b1401f874f96a53f1ec1c0f8a6089ae66a4ce", 
            "Extract Method public _testOldFashionedResourceParamProvided(target WebTarget) : void extracted from public testOldFashionedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Method public _testParamValidatedResourceNoParam(target WebTarget) : void extracted from public testParamValidatedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Method public _testParamValidatedResourceParamProvided(target WebTarget) : void extracted from public testParamValidatedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Method public _testFieldValidatedResourceNoParam(target WebTarget) : void extracted from public testFieldValidatedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Method public _testFieldValidatedResourceParamProvided(target WebTarget) : void extracted from public testFieldValidatedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Method public _testPropertyValidatedResourceNoParam(target WebTarget) : void extracted from public testPropertyValidatedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Method public _testPropertyValidatedResourceParamProvided(target WebTarget) : void extracted from public testPropertyValidatedResourceParamProvided() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Method public _testOldFashionedResourceNoParam(target WebTarget) : void extracted from public testOldFashionedResourceNoParam() : void in class org.glassfish.jersey.tests.cdi.bv.BaseValidationTest",
            "Extract Superclass org.glassfish.jersey.ext.cdi1x.internal.GenericHk2LocatorManager from classes [org.glassfish.jersey.ext.cdi1x.servlet.internal.ServletHk2LocatorManager]");

          processTp("https://github.com/deeplearning4j/deeplearning4j.git", "c50064efb325b0c94cc62788b4c8935b7c39ac40", 
            "Extract Method private getOptimizer(oa OptimizationAlgorithm, conf NeuralNetConfiguration, m Model) : ConvexOptimizer extracted from public testSphereFnOptHelper(oa OptimizationAlgorithm, numLineSearchIter int, nDimensions int) : void in class org.deeplearning4j.optimize.solver.TestOptimizers");

          processTp("https://github.com/JetBrains/intellij-community.git", "6ad1dcbfef36821a71cbffa301c58d1c3ffe8d62", 
            "Move Method protected createMainModule(moduleType ModuleType) : Module from class com.intellij.testFramework.LightPlatformTestCase to public createMainModule(project Project) : Module from class com.intellij.testFramework.LightProjectDescriptor");

          processTp("https://github.com/apache/cassandra.git", "4fcd7d4d366d001cf5f1f7d926c608c902e3f0af", 
            "Move Class org.apache.cassandra.locator.DynamicEndpointSnitchTest.ScoreUpdater moved to org.apache.cassandra.locator.DynamicEndpointSnitchLongTest.ScoreUpdater");

          processTp("https://github.com/opentripplanner/OpenTripPlanner.git", "334dbc7cf3432e7c17b0ed98801e61b0b591b408", 
            "Move Class org.opentripplanner.analyst.cluster.AnalystWorker.WorkerIdDefiner moved to org.opentripplanner.analyst.cluster.WorkerIdDefiner");

          processTp("https://github.com/JetBrains/intellij-community.git", "97811cf971f7ccf6a5fc5e90a491db2f58d49da1", 
            "Move Class org.jetbrains.jps.cmdline.BuildMain.MyLoggerFactory moved to org.jetbrains.jps.cmdline.LogSetup.MyLoggerFactory");

          processTp("https://github.com/Graylog2/graylog2-server.git", "2d98ae165ea43e9a1ac6a905d6094f077abb2e55", 
            "Extract Method private dispatchMessage(msg Message) : void extracted from public onEvent(event MessageEvent) : void in class org.graylog2.shared.buffers.processors.ProcessBufferProcessor",
            "Extract Method private postProcessMessage(raw RawMessage, codec Codec, inputIdOnCurrentNode String, baseMetricName String, message Message, decodeTime long) : Message extracted from private processMessage(raw RawMessage) : Message in class org.graylog2.shared.buffers.processors.DecodingProcessor");

          processTp("https://github.com/VoltDB/voltdb.git", "7527cfc746dc20ddb78002c7b3a65d55026a334e", 
            "Move Class org.voltdb.importer.ChannelChangeNotifier.CallbacksRef moved to org.voltdb.importer.ChannelDistributer.CallbacksRef");

          processTp("https://github.com/NLPchina/ansj_seg.git", "913704e835169255530c7408cad11ce9a714d4ec", 
            "Move Class org.ansj.app.crf.pojo.TempFeature moved to org.ansj.app.crf.CrfppModelParser.TempFeature");

          processTp("https://github.com/cgeo/cgeo.git", "c142b8ca3e9f9467931987ee16805cf53e6bc5d2", 
            "Extract Method private getWaymarkingConnector() : IConnector extracted from public testCanHandle() : void in class cgeo.geocaching.connector.WaymarkingConnectorTest",
            "Extract Method private getWaymarkingConnector() : IConnector extracted from public testGetGeocodeFromUrl() : void in class cgeo.geocaching.connector.WaymarkingConnectorTest");

          processTp("https://github.com/netty/netty.git", "9f422ed0f44516bea8116ed7730203e4eb316252", 
            "Extract Method private resetCtx() : void extracted from public windowUpdateAndFlushShouldTriggerWrite() : void in class io.netty.handler.codec.http2.DefaultHttp2RemoteFlowControllerTest",
            "Extract Method private initConnectionAndController() : void extracted from public setup() : void in class io.netty.handler.codec.http2.DefaultHttp2RemoteFlowControllerTest",
            "Move Attribute private ctx : ChannelHandlerContext from class io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder.FlowControlledBase to class io.netty.microbench.http2.NoopHttp2RemoteFlowController");

          processTp("https://github.com/loopj/android-async-http.git", "af7e9e4bcd90504d6a665dbb21635eb1733fe025", 
            "Move Attribute private TAG : WeakReference<Object> from class com.loopj.android.http.RequestHandle to class com.loopj.android.http.AsyncHttpResponseHandler");

          processTp("https://github.com/liferay/liferay-plugins.git", "d99695841fa675ea9150602b1132f037093e867d", 
            "Extract Method protected getGetOAuthRequest(portletRequest PortletRequest, portletResponse PortletResponse) : OAuthRequest extracted from protected remoteRender(renderRequest RenderRequest, renderResponse RenderResponse) : void in class com.liferay.marketplace.store.portlet.RemoteMVCPortlet");

          processTp("https://github.com/plutext/docx4j.git", "59b8e89e61432d1d8f25cb003b62b3ac004d1b6f", 
            "Extract Method private setProtectionPassword(password String, hashAlgo HashAlgorithm) : void extracted from public setEnforcementEditValue(editValue STDocProtect, password String, hashAlgo HashAlgorithm) : void in class org.docx4j.openpackaging.parts.WordprocessingML.DocumentSettingsPart");

          processTp("https://github.com/MovingBlocks/Terasology.git", "543a9808a85619dbe5acc2373cb4fe5344442de7", 
            "Move Method public isFullscreen() : boolean from class org.terasology.engine.TerasologyEngine to public isFullscreen() : boolean from class org.terasology.engine.subsystem.lwjgl.LwjglDisplayDevice",
            "Inline Method private initTimer(context Context) : void inlined to public preInitialise(context Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglTimer",
            "Inline Method private initOpenAL(context Context) : void inlined to public initialise(rootContext Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglAudio",
            "Move Class org.terasology.engine.subsystem.ThreadManagerSubsystem moved to org.terasology.engine.subsystem.common.ThreadManagerSubsystem",
            "Move Class org.terasology.engine.subsystem.ThreadManager moved to org.terasology.engine.subsystem.common.ThreadManager",
            "Move Attribute private time : EngineTime from class org.terasology.engine.TerasologyEngine to class org.terasology.engine.subsystem.lwjgl.LwjglTimer",
            "Move Attribute private time : EngineTime from class org.terasology.engine.TerasologyEngine to class org.terasology.engine.subsystem.headless.HeadlessTimer");

          processTp("https://github.com/datastax/java-driver.git", "14abb6919a99a0d6d500198dd2e30c83b1bb6709", 
            "Extract Method private validateParameters() : void extracted from public prepare(manager MappingManager, ps PreparedStatement) : void in class com.datastax.driver.mapping.MethodMapper");

          processTp("https://github.com/BuildCraft/BuildCraft.git", "a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0", 
            "Push Down Attribute private side : ForgeDirection from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdRequest",
            "Push Down Attribute private index : BlockIndex from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdRequest",
            "Push Down Attribute public side : ForgeDirection from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdBlock",
            "Push Down Attribute public index : BlockIndex from class buildcraft.api.robots.ResourceId to class buildcraft.api.robots.ResourceIdBlock",
            "Extract Method private getAvailableRequests(station DockingStation) : Collection<StackRequest> extracted from private getOrderFromRequestingStation(station DockingStation, take boolean) : StackRequest in class buildcraft.robotics.ai.AIRobotSearchStackRequest",
            "Push Down Method public equals(obj Object) : boolean from class buildcraft.api.robots.ResourceId to public equals(obj Object) : boolean from class buildcraft.api.robots.ResourceIdBlock");
          processFp("https://github.com/BuildCraft/BuildCraft.git", "a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0", 
            "Extract Method private releaseCurrentRequest() : void extracted from public delegateAIEnded(ai AIRobot) : void in class buildcraft.robotics.boards.BoardRobotDelivery");

          processFp("https://github.com/jMonkeyEngine/jmonkeyengine.git", "7f2c7c5d356acc350e705168e972112bfb151e83", 
            "Move Method public getAngularFactor() : Vector3f from class com.jme3.bullet.objects.PhysicsRigidBody to public getAngularFactor(store Vector3f) : Vector3f from class com.jme3.bullet.objects.PhysicsRigidBody");
          processTp("https://github.com/apache/camel.git", "ab1d1dd78fe53edb50c4ede447e4ac5a55ee2ac9", 
            "Extract Method public createExchange(message Message, endpoint Endpoint, keyFormatStrategy KeyFormatStrategy) : Exchange extracted from public createExchange(message Message, endpoint Endpoint) : Exchange in class org.apache.camel.component.sjms.jms.JmsMessageHelper");


          processFp("https://github.com/SonarSource/sonarqube.git", "a080f6f35834dc47d80474b5b0cc5fa5f50ec1fc", 
            "Extract Interface org.sonar.server.issue.filter.IssueFilterWsAction from classes [org.sonar.server.issue.filter.AppAction, org.sonar.server.issue.filter.FavoritesAction, org.sonar.server.issue.filter.ShowAction]");

          processFp("https://github.com/cwensel/cascading.git", "13058185bf30976b96f31f8ca006e9c3635e3549", 
            "Inline Method private getJobStatusClient() : RunningJob inlined to protected captureChildDetailInternal() : boolean in class cascading.stats.hadoop.HadoopNodeStats");
          processTp("https://github.com/xetorthio/jedis.git", "d4b4aecbc69bbd04ba87c4e32a52cff3d129906a", 
            "Extract Method private poolInactive() : boolean extracted from public getNumWaiters() : int in class redis.clients.util.Pool",
            "Extract Method private poolInactive() : boolean extracted from public getNumIdle() : int in class redis.clients.util.Pool",
            "Extract Method private poolInactive() : boolean extracted from public getNumActive() : int in class redis.clients.util.Pool");


          processFp("https://github.com/sarxos/webcam-capture.git", "36e1138fc4b46513c32b31fab0227378d495531d", 
            "Move Class com.github.sarxos.webcam.MultipointMotionDetectionExample moved to MultipointMotionDetectionExample");
          processTp("https://github.com/neo4j/neo4j.git", "dc199688d69416da58b370ca2aa728e935fc8e0d", 
            "Extract Method private getSortedIndexUpdates(descriptor IndexDescriptor) : TreeMap<DefinedProperty,DiffSets<Long>> extracted from private getIndexUpdatesForPrefix(descriptor IndexDescriptor, prefix String) : ReadableDiffSets<Long> in class org.neo4j.kernel.impl.api.state.TxState");

          processTp("https://github.com/ignatov/intellij-erlang.git", "3855f0ca82795f7481b34342c7d9e5644a1d42c3", 
            "Inline Method private getModuleFileName() : String inlined to public resolve() : PsiElement in class org.intellij.erlang.psi.impl.ErlangFunctionReferenceImpl");

          processTp("https://github.com/apache/camel.git", "14a7dd79148f9306dcd2f748b56fd6550e9406ab", 
            "Extract Method private readClassFromCamelResource(file File, buffer StringBuilder, buildContext BuildContext) : String extracted from public prepareLanguage(log Log, project MavenProject, projectHelper MavenProjectHelper, languageOutDir File, schemaOutDir File, buildContext BuildContext) : void in class org.apache.camel.maven.packaging.PackageLanguageMojo",
            "Extract Method private readClassFromCamelResource(file File, buffer StringBuilder, buildContext BuildContext) : String extracted from public prepareDataFormat(log Log, project MavenProject, projectHelper MavenProjectHelper, dataFormatOutDir File, schemaOutDir File, buildContext BuildContext) : void in class org.apache.camel.maven.packaging.PackageDataFormatMojo");


          processFp("https://github.com/crate/crate.git", "7c1f74b47290a155c28498a1cb79720c4e0e1e95", 
            "Extract Method protected processRequestItems(shardId ShardId, request ShardUpsertRequest, extractorContextUpdate SymbolToFieldExtractorContext, implContextInsert SymbolToInputContext, killed AtomicBoolean) : ShardUpsertResponse extracted from protected shardOperationOnPrimary(clusterState ClusterState, shardRequest PrimaryOperationRequest) : PrimaryResponse<ShardUpsertResponse,ShardUpsertRequest> in class io.crate.executor.transport.TransportShardUpsertAction",
            "Extract Method protected processRequestItems(shardId ShardId, request SymbolBasedShardUpsertRequest, killed AtomicBoolean) : ShardUpsertResponse extracted from protected shardOperationOnPrimary(clusterState ClusterState, shardRequest PrimaryOperationRequest) : PrimaryResponse<ShardUpsertResponse,SymbolBasedShardUpsertRequest> in class io.crate.executor.transport.SymbolBasedTransportShardUpsertAction");
          processTp("https://github.com/siacs/Conversations.git", "e6cb12dfe414497b4317820497985c110cb81864", 
            "Extract Method public getItemViewType(message Message) : int extracted from public getItemViewType(position int) : int in class eu.siacs.conversations.ui.adapter.MessageAdapter");

          processTp("https://github.com/JetBrains/intellij-community.git", "1b70adbfd49e00194c4c1170ef65e8114d7a2e46", 
            "Extract Method private getFieldInitializerNullness(expression PsiExpression) : Nullness extracted from private calcInherentNullability() : Nullness in class com.intellij.codeInspection.dataFlow.value.DfaVariableValue");

          processTp("https://github.com/facebook/presto.git", "484b7cb0d20ec8f7c3b0d9eaf9e3f93468cec88c", 
            "Move Class com.facebook.presto.split.TestJmxSplitManager moved to com.facebook.presto.connector.jmx.TestJmxSplitManager");

          processTp("https://github.com/jline/jline2.git", "1eb3b624b288a4b1a054420d3efb05b8f1d28517", 
            "Move Method public wcwidth(cs CharSequence) : int from class jline.console.WCWidth to package wcwidth(str CharSequence, pos int) : int from class jline.console.ConsoleReader");
          processFp("https://github.com/jline/jline2.git", "1eb3b624b288a4b1a054420d3efb05b8f1d28517", 
            "Inline Method private print(buff char) : void inlined to private drawBuffer(clear int) : void in class jline.console.ConsoleReader",
            "Extract Method private rawPrint(c char, num int) : void extracted from private drawBuffer(clear int) : void in class jline.console.ConsoleReader");
          processTp("https://github.com/greenrobot/greenDAO.git", "d6d9dd4365387816fda6987a4ad9b679c27e72a3", 
            "Move Class de.greenrobot.dao.PropertyConverter moved to de.greenrobot.dao.converter.PropertyConverter");

          processTp("https://github.com/spotify/helios.git", "3ffd70929c08be5cf14f156189e8050969caa87e", 
            "Extract Method private isRolloutTimedOut(deploymentGroup DeploymentGroup, client ZooKeeperClient) : boolean extracted from private rollingUpdateAwaitRunning(deploymentGroup DeploymentGroup, host String) : RollingUpdateTaskResult in class com.spotify.helios.master.ZooKeeperMasterModel");

          processTp("https://github.com/apache/cassandra.git", "b70f7ea0ce27b5defa0a7773d448732364e7aee0", 
            "Extract Method private listSnapshots() : List<File> extracted from public getSnapshotDetails() : Map<String,Pair<Long,Long>> in class org.apache.cassandra.db.Directories");


          processFp("https://github.com/neo4j/neo4j.git", "7973485502c707739756a9e712b655f84cb6b492", 
            "Move Method public nodeDelete(state KernelStatement, nodeId long) : void from class org.neo4j.kernel.impl.api.StateHandlingStatementOperations to public nodeDelete(state KernelStatement, node NodeItem) : void from class org.neo4j.kernel.impl.api.StateHandlingStatementOperations");

          processFp("https://github.com/infinispan/infinispan.git", "33b2b3025f98ec6ca1960101e797e340a4a35b31", 
            "Extract Method private assertIterationActiveOnlyOnServer(index int) : void extracted from public testIterationRouting() : void in class org.infinispan.client.hotrod.impl.iteration.MultiServerDistRemoteIteratorTest");
          
          // missing object
//          processTp("https://github.com/spring-projects/spring-data-neo4j.git", "ef2a0d63393484975854fc08ad0fd3abc7dd76b0", 
//            "Move Class org.springframework.data.neo4j.examples.friends.Person moved to org.springframework.data.neo4j.examples.friends.domain.Person",
//            "Move Class org.springframework.data.neo4j.examples.friends.Friendship moved to org.springframework.data.neo4j.examples.friends.domain.Friendship",
//            "Move Class org.springframework.data.neo4j.examples.friends.FriendContext moved to org.springframework.data.neo4j.examples.friends.context.FriendContext");


          processFp("https://github.com/impetus-opensource/Kundera.git", "656eb28fc2d417497cee2e9e3de1a35b5d2a277a", 
            "Extract Method private appendValue(value Object, isString boolean) : void extracted from private appendValueClause(alias String, expr Expression) : void in class com.impetus.kundera.persistence.CriteriaQueryTranslator.QueryBuilder");
          processTp("https://github.com/neo4j/neo4j.git", "4beba7bbdf927486a5cbf298a0fb2be50905a590", 
            "Move Class org.neo4j.kernel.impl.store.UniquePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.UniquePropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.RelationshipPropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.RelationshipPropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.PropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.PropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.NodePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.NodePropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.MandatoryRelationshipPropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.MandatoryRelationshipPropertyConstraintRule",
            "Move Class org.neo4j.kernel.impl.store.MandatoryNodePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.MandatoryNodePropertyConstraintRule");


          processFp("https://github.com/apache/nutch.git", "b017487ac9e04ed2f0c2473dff8bfd9a924535df", 
            "Extract Method public dump(outputDir File, segmentRootDir File, mimeTypes String[], mimeTypeStats boolean) : void extracted from public main(args String[]) : void in class org.apache.nutch.tools.FileDumper");

          processFp("https://github.com/encog/encog-java-core.git", "f7a6d89a22b8d0fcdc78dc6bb606abb06c5ef017", 
            "Move Method public calculateError(ideal double[], actual double[], error double[]) : void from class org.encog.neural.error.LinearErrorFunction to public calculateError(af ActivationFunction, b double[], a double[], ideal double[], actual double[], error double[], derivShift double, significance double) : void from class org.encog.neural.error.CrossEntropyErrorFunction");
          processTp("https://github.com/jersey/jersey.git", "ee5aa50af6b4586fbe92cab718abfae8113a81aa", 
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringRequestResourceTest moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringRequestResourceTest",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringRequestResource moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringRequestResource",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringAnnotationConfig moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringAnnotationConfig",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.NorwegianGoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.NorwegianGoodbyeService",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.JerseyConfig moved to org.glassfish.jersey.examples.hello.spring.annotations.JerseyConfig",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.GreetingService moved to org.glassfish.jersey.examples.hello.spring.annotations.GreetingService",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.GoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.GoodbyeService",
            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.EnglishGoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.EnglishGoodbyeService");


          processFp("https://github.com/JetBrains/intellij-plugins.git", "1277949a1b52cc16c2f4fad1acb48556377b7f30", 
            "Extract Method public findRegion(refPsiFile PsiFile, refOffset int, refLength int) : PluginNavigationRegion extracted from public resolve(reference DartReference, incompleteCode boolean) : List<? extends PsiElement> in class com.jetbrains.lang.dart.resolve.DartResolver");
          processTp("https://github.com/spotify/helios.git", "687bda5a3ea1b5daae2764653843d318c77f4590", 
            "Move Attribute public KAFKA_TOPIC : String from class com.spotify.helios.agent.TaskHistoryWriter to class com.spotify.helios.common.descriptors.TaskStatusEvent");

          processTp("https://github.com/apache/hive.git", "c53c6f45988db869d56abe3b1d831ff775f4fa73", 
            "Extract Method private statsForOneColumnFromProtoBuf(partitionColumnStats ColumnStatistics, proto ColumnStats) : ColumnStatisticsObj extracted from package deserializeStatsForOneColumn(partitionColumnStats ColumnStatistics, bytes byte[]) : ColumnStatisticsObj in class org.apache.hadoop.hive.metastore.hbase.HBaseUtils",
            "Extract Method private protoBufStatsForOneColumn(partitionColumnStats ColumnStatistics, colStats ColumnStatisticsObj) : ColumnStats extracted from package serializeStatsForOneColumn(partitionColumnStats ColumnStatistics, colStats ColumnStatisticsObj) : byte[] in class org.apache.hadoop.hive.metastore.hbase.HBaseUtils");

          processTp("https://github.com/Atmosphere/atmosphere.git", "69c229b7611ff8c6a20ff2d4da917a68c1cde64a", 
            "Move Method private getInheritedPrivateFields(type Class<?>) : Set<Field> from class org.atmosphere.inject.InjectableObjectFactory to public getInheritedPrivateFields(type Class<?>) : Set<Field> from class org.atmosphere.util.Utils");


          processFp("https://github.com/liquibase/liquibase.git", "96abb29ca3eadad9529e0c29445ff986b32383b0", 
            "Move Method public escapeObjectName(catalogName String, schemaName String, objectName String, objectType Class<? extends DatabaseObject>) : String from class liquibase.database.AbstractJdbcDatabase to public escapeObjectName(objectName ObjectName, objectType Class<? extends DatabaseObject>) : String from class liquibase.database.core.db2.DB2Database");

          processFp("https://github.com/couchbase/couchbase-lite-android.git", "a705c993ab86f39e85b86e3fec9f14d40d51d66d", 
            "Extract Method private getAttachmentsStub(name String) : Map<String,Map<String,Object>> extracted from public testAttachments() : void in class com.couchbase.lite.AttachmentsTest");
          processTp("https://github.com/querydsl/querydsl.git", "e1aa31cff985e2a0c2babf4da96dc0a538d5e514", 
            "Move Method private escapeLiteral(str String) : String from class com.querydsl.jpa.JPQLSerializer to private escapeLiteral(str String) : String from class com.querydsl.jpa.JPQLTemplates");


          processFp("https://github.com/JetBrains/intellij-plugins.git", "2ee27cda004847f287e32b4b04c0163c69c35c76", 
            "Pull Up Attribute protected KNOWN_TO_FAIL : Set<String> from class com.jetbrains.lang.dart.dart_style.DartStyleStrictTest to class com.jetbrains.lang.dart.dart_style.DartStyleTest",
            "Pull Up Attribute protected KNOWN_TO_FAIL : Set<String> from class com.jetbrains.lang.dart.dart_style.DartStyleLenientTest to class com.jetbrains.lang.dart.dart_style.DartStyleTest");

          processFp("https://github.com/liferay/liferay-plugins.git", "82838b1bab855fdfe7e6b1380fea038222b317c0", 
            "Inline Method protected rotateImage(file File) : void inlined to protected addImageFile(userId long, file File) : JSONObject in class com.liferay.asset.entry.set.service.impl.AssetEntrySetLocalServiceImpl");
          processTp("https://github.com/JetBrains/intellij-community.git", "6ff3fe00d7ffe04dbe0904b8bad98285b6988d6d", 
            "Pull Up Method public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List<String> from class org.jetbrains.idea.maven.server.Maven32ServerEmbedderImpl to public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List<String> from class org.jetbrains.idea.maven.server.Maven3ServerEmbedder",
            "Pull Up Method public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List<String> from class org.jetbrains.idea.maven.server.Maven30ServerEmbedderImpl to public retrieveAvailableVersions(groupId String, artifactId String, remoteRepositoryUrl String) : List<String> from class org.jetbrains.idea.maven.server.Maven3ServerEmbedder",
            "Extract Method public customizeComponents() : void extracted from public customize(workspaceMap MavenWorkspaceMap, failOnUnresolvedDependency boolean, console MavenServerConsole, indicator MavenServerProgressIndicator, alwaysUpdateSnapshots boolean) : void in class org.jetbrains.idea.maven.server.Maven32ServerEmbedderImpl",
            "Extract Method public customizeComponents() : void extracted from public customize(workspaceMap MavenWorkspaceMap, failOnUnresolvedDependency boolean, console MavenServerConsole, indicator MavenServerProgressIndicator, alwaysUpdateSnapshots boolean) : void in class org.jetbrains.idea.maven.server.Maven30ServerEmbedderImpl");

          processTp("https://github.com/apache/cassandra.git", "2b0a8f6bdac621badabcb9921c077260f2470c26", 
            "Extract Method public deleteRowAt(metadata CFMetaData, timestamp long, localDeletionTime int, key Object, clusteringValues Object[]) : Mutation extracted from public deleteRow(metadata CFMetaData, timestamp long, key Object, clusteringValues Object[]) : Mutation in class org.apache.cassandra.db.RowUpdateBuilder");

          processTp("https://github.com/JetBrains/intellij-community.git", "33b0ac3a029845f9c20f7f5967c03b31b24f3b4b", 
            "Pull Up Method private iterateRecursively(root VirtualFile, processor ContentIterator, indicator ProgressIndicator, visitedRoots Set<VirtualFile>, projectFileIndex ProjectFileIndex) : void from class com.intellij.util.indexing.FileBasedIndexImpl to public iterateRecursively(root VirtualFile, processor ContentIterator, indicator ProgressIndicator, visitedRoots Set<VirtualFile>, projectFileIndex ProjectFileIndex) : void from class com.intellij.util.indexing.FileBasedIndex");

          processTp("https://github.com/dropwizard/metrics.git", "4c6ab3d77cc67c7a91155d884077520dcf1509c6", 
            "Extract Method private closeGraphiteConnection() : void extracted from public report(gauges SortedMap<String,Gauge>, counters SortedMap<String,Counter>, histograms SortedMap<String,Histogram>, meters SortedMap<String,Meter>, timers SortedMap<String,Timer>) : void in class com.codahale.metrics.graphite.GraphiteReporter");

          processTp("https://github.com/neo4j/neo4j.git", "4712de476aabe69cd762233c9641dd3cf9f8361b", 
            "Extract Superclass org.neo4j.graphalgo.impl.centrality.EigenvectorCentralityBase from classes [org.neo4j.graphalgo.impl.centrality.EigenvectorCentralityArnoldi, org.neo4j.graphalgo.impl.centrality.EigenvectorCentralityPower]");


          processFp("https://github.com/facebook/presto.git", "3d479c692eccc3518a253dd252baf96088addc7f", 
            "Move Method public longAccessor(type Type, field Integer, row Slice) : Long from class com.facebook.presto.operator.scalar.RowFieldReference to public longAccessor(type Type, field Integer, row Block) : Long from class com.facebook.presto.operator.scalar.RowFieldReference");
          processTp("https://github.com/gradle/gradle.git", "681dc6346ce3cf5be5c5985faad120a18949cee0", 
            "Extract Method private createPlatformToolProvider(targetPlatform NativePlatformInternal) : PlatformToolProvider extracted from public select(targetPlatform NativePlatformInternal) : PlatformToolProvider in class org.gradle.nativeplatform.toolchain.internal.gcc.AbstractGccCompatibleToolChain");

          processTp("https://github.com/linkedin/rest.li.git", "f61db44ca4a862f1a84450643d92f85449016cfa", 
            "Inline Method public generate(schema DataSchema) : ClassTemplateSpec inlined to private generateRecord(schema RecordDataSchema) : RecordTemplateSpec in class com.linkedin.pegasus.generator.TemplateSpecGenerator");

          processTp("https://github.com/jenkinsci/workflow-plugin.git", "d0e374ce8ecb687b4dc046d1edea9e52da17706f", 
            "Move Attribute package SCRIPT : String from class org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory to class org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject",
            "Inline Method private setBranch(property BranchJobProperty, branch Branch, project WorkflowJob) : void inlined to public setBranch(project WorkflowJob, branch Branch) : WorkflowJob in class org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory");

          processTp("https://github.com/JetBrains/intellij-community.git", "9fbf6b852bd9766060307aff960fb597d55e24d3", 
            "Extract Method public clear() : void extracted from public close() : void in class com.intellij.util.io.CachingEnumerator");

          processTp("https://github.com/robovm/robovm.git", "1ef86e69d5a108c2b4d836b0634ebdea912cfe00", 
            "Move Class org.robovm.compiler.plugin.lambda2.LambdaPlugin moved to org.robovm.compiler.plugin.lambda.LambdaPlugin",
            "Move Class org.robovm.compiler.plugin.lambda2.LambdaClassGenerator moved to org.robovm.compiler.plugin.lambda.LambdaClassGenerator",
            "Move Class org.robovm.compiler.plugin.lambda2.LambdaClass moved to org.robovm.compiler.plugin.lambda.LambdaClass");


          processFp("https://github.com/crate/crate.git", "2aebccdad738432eb0fb2c7279fad8e00df17e3e", 
            "Extract Method protected processRequestItems(shardId ShardId, request ShardUpsertRequest, extractorContextUpdate SymbolToFieldExtractorContext, implContextInsert SymbolToInputContext, killed AtomicBoolean) : ShardUpsertResponse extracted from protected shardOperationOnPrimary(clusterState ClusterState, shardRequest PrimaryOperationRequest) : PrimaryResponse<ShardUpsertResponse,ShardUpsertRequest> in class io.crate.executor.transport.TransportShardUpsertAction",
            "Extract Method protected processRequestItems(shardId ShardId, request SymbolBasedShardUpsertRequest, killed AtomicBoolean) : ShardUpsertResponse extracted from protected shardOperationOnPrimary(clusterState ClusterState, shardRequest PrimaryOperationRequest) : PrimaryResponse<ShardUpsertResponse,SymbolBasedShardUpsertRequest> in class io.crate.executor.transport.SymbolBasedTransportShardUpsertAction");
          processTp("https://github.com/apache/cassandra.git", "5790b4a44ba85e7e8ece64613d9e6a1b737a6cde", 
            "Extract Method protected decompose(dataType DataType, protocolVersion int, value Object) : ByteBuffer extracted from protected decompose(protocolVersion int, value Object) : ByteBuffer in class org.apache.cassandra.cql3.functions.UDFunction",
            "Extract Method protected compose(argDataTypes DataType[], protocolVersion int, argIndex int, value ByteBuffer) : Object extracted from protected compose(protocolVersion int, argIndex int, value ByteBuffer) : Object in class org.apache.cassandra.cql3.functions.UDFunction");

          processTp("https://github.com/facebook/buck.git", "a1525ac9a0bb8f727167a8be94c81a3415128ef4", 
            "Extract Method private getAllPathsWork(workingDir Path) : ImmutableBiMap<Path,Path> extracted from private getAllPaths(workingDir Optional<Path>) : ImmutableBiMap<Path,Path> in class com.facebook.buck.cxx.DebugPathSanitizer");

          processTp("https://github.com/PhilJay/MPAndroidChart.git", "3514aaedf9624222c985cb3abb12df2d9b514b12", 
            "Move Class com.github.mikephil.charting.utils.Highlight moved to com.github.mikephil.charting.highlight.Highlight");

          processTp("https://github.com/RoboBinding/RoboBinding.git", "b6565814805dfb2d989be25c11d4fb4cf8fb1d84", 
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.SubItemPresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.SubItemPresentationModelExample",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.SubItem moved to org.robobinding.codegen.presentationmodel.nestedIPM.SubItem",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.PresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.PresentationModelExample",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.ItemPresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.ItemPresentationModelExample",
            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.Item moved to org.robobinding.codegen.presentationmodel.nestedIPM.Item");
          processFp("https://github.com/RoboBinding/RoboBinding.git", "b6565814805dfb2d989be25c11d4fb4cf8fb1d84", 
            "Extract Method private removeItemPresentationModelFactoryMethod(dataSetProperties Collection<DataSetPropertyInfoImpl>) : void extracted from private processDataSetProperties() : void in class org.robobinding.codegen.presentationmodel.processor.PresentationModelInfoBuilder");
          processTp("https://github.com/facebook/buck.git", "db024f5ec3e9611ddf8103bdc4c3817c704f7b27", 
            "Extract Method public getTargetsAndDependencies(params CommandRunnerParams, argumentsFormattedAsBuildTargets List<String>, showTransitive boolean, showTests boolean, enableProfiling boolean) : Multimap<BuildTarget,BuildTarget> extracted from public runWithoutHelp(params CommandRunnerParams) : int in class com.facebook.buck.cli.AuditDependenciesCommand");


          processFp("https://github.com/wildfly/wildfly.git", "0f54e0da7e91da5d226709f50c5a40b9ec8bfd89", 
            "Inline Method private startDataSource(dataSourceService AbstractDataSourceService, jndiName String, moduleDescription EEModuleDescription, context ResolutionContext, serviceTarget ServiceTarget, valueSourceServiceBuilder ServiceBuilder, injector Injector<ManagedReferenceFactory>, securityEnabled boolean) : void inlined to public getResourceValue(context ResolutionContext, serviceBuilder ServiceBuilder<?>, phaseContext DeploymentPhaseContext, injector Injector<ManagedReferenceFactory>) : void in class org.jboss.as.connector.deployers.datasource.DataSourceDefinitionInjectionSource");
          processTp("https://github.com/JetBrains/intellij-plugins.git", "0df7cb00757fe0d4fac8d8b0d5fc46af95feb238", 
            "Extract Method public findPsiFile(project Project, path String) : PsiFile extracted from private getElementForNavigationTarget(project Project, target PluginNavigationTarget) : PsiElement in class com.jetbrains.lang.dart.resolve.DartResolver");

          processTp("https://github.com/JetBrains/intellij-community.git", "7dd55014f9840ce03867bb175cf37a4c151dc806", 
            "Extract Method private createConfigurable(ep ConfigurableEP<T>, log boolean) : T extracted from public wrapConfigurable(ep ConfigurableEP<T>) : T in class com.intellij.openapi.options.ex.ConfigurableWrapper");

          processTp("https://github.com/google/guava.git", "31fc19200207ccadc45328037d8a2a62b617c029", 
            "Extract Method public tryParse(string String, radix int) : Long extracted from public tryParse(string String) : Long in class com.google.common.primitives.Longs",
            "Move Attribute private asciiDigits : byte[] from class com.google.common.primitives.Ints to class com.google.common.primitives.Longs",
            "Move Method private digit(c char) : int from class com.google.common.primitives.Ints to private digit(c char) : int from class com.google.common.primitives.Longs");
          processFp("https://github.com/google/guava.git", "31fc19200207ccadc45328037d8a2a62b617c029", 
            "Extract Method public tryParse(string String, radix int) : Integer extracted from package tryParse(string String, radix int) : Integer in class com.google.common.primitives.Ints");
          processTp("https://github.com/apache/hive.git", "92e98858e742bbb669ccbf790a71a618c581df21", 
            "Extract Method public use(ctx ParserRuleContext, sql String) : Integer extracted from public use(ctx Use_stmtContext) : Integer in class org.apache.hive.hplsql.Stmt");

          processTp("https://github.com/JetBrains/intellij-community.git", "6905d569a1e39d0d7b1ec5ceee4f0bbe60b85947", 
            "Extract Interface com.jetbrains.edu.courseFormat.Named from classes [com.jetbrains.edu.courseFormat.Lesson, com.jetbrains.edu.courseFormat.Task]");

          processTp("https://github.com/apache/cassandra.git", "573a1d115b86abbe3fb53ff930464d7d8fd95600", 
            "Extract Method package getDroppedMessagesLogs() : List<String> extracted from private logDroppedMessages() : void in class org.apache.cassandra.net.MessagingService",
            "Extract Method public incrementDroppedMessages(verb Verb, isCrossNodeTimeout boolean) : void extracted from public incrementDroppedMessages(verb Verb) : void in class org.apache.cassandra.net.MessagingService");


          processFp("https://github.com/google/closure-compiler.git", "7a3b2e18587b2a8fd3e0b759c92d317dd7098ec9", 
            "Extract Method private addTypeWarning(messageId String) : void extracted from private parseAnnotation(token JsDocToken, extendedTypes List<ExtendedTypeInfo>) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addTypeWarning(messageId String, messageArg String) : void extracted from private parseFieldTypeList(token JsDocToken) : Node in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String) : void extracted from private parseIdGeneratorTag(token JsDocToken) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String) : void extracted from private parseModifiesTag(token JsDocToken) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String) : void extracted from private parseSuppressTag(token JsDocToken) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String) : void extracted from private parseAnnotation(token JsDocToken, extendedTypes List<ExtendedTypeInfo>) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String) : void extracted from private parseHelperLoop(token JsDocToken, extendedTypes List<ExtendedTypeInfo>) : boolean in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String, messageArg String) : void extracted from private parseIdGeneratorTag(token JsDocToken) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String, messageArg String) : void extracted from private parseModifiesTag(token JsDocToken) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String, messageArg String) : void extracted from private parseSuppressTag(token JsDocToken) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String, messageArg String) : void extracted from private parseAnnotation(token JsDocToken, extendedTypes List<ExtendedTypeInfo>) : JsDocToken in class com.google.javascript.jscomp.parsing.JsDocInfoParser",
            "Extract Method private addParserWarning(messageId String, messageArg String) : void extracted from private parseHelperLoop(token JsDocToken, extendedTypes List<ExtendedTypeInfo>) : boolean in class com.google.javascript.jscomp.parsing.JsDocInfoParser");

          processFp("https://github.com/CyanogenMod/android_packages_apps_Trebuchet.git", "9431c89974fb4c119da5e53bcdef5c5f8b390652", 
            "Inline Method public isGelIntegrationEnabled() : boolean inlined to protected hasCustomContentToLeft() : boolean in class com.android.launcher3.Launcher",
            "Extract Method public getCustomContentMode() : CustomContentMode extracted from protected hasCustomContentToLeft() : boolean in class com.android.launcher3.Launcher");
          processTp("https://github.com/checkstyle/checkstyle.git", "2f7481ee4e20ae785298c31ec2f979752dd7eb03", 
            "Extract Method private checkInterfaceModifiers(ast DetailAST) : void extracted from public visitToken(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.modifier.RedundantModifierCheck");


          processFp("https://github.com/JetBrains/intellij-community.git", "fdb5f14a54196ca286d90d5cf6194dc1661e4666", 
            "Extract Superclass com.intellij.util.containers.JBIterator from classes [com.intellij.util.containers.TreeTraverser.TracingIt]");
          processTp("https://github.com/neo4j/neo4j.git", "021d17c8234904dcb1d54596662352395927fe7b", 
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


          processFp("https://github.com/apache/camel.git", "b170152df80611118a7aad628747be15ea7fe07c", 
            "Extract Method protected doWrite(event String) : void extracted from public write(event SplunkEvent) : void in class org.apache.camel.component.splunk.support.SplunkDataWriter");
          processTp("https://github.com/facebook/presto.git", "11048642b1e6b0e35efefab9e4e693b09c8753f5", 
            "Move Method private formatDomain(table TableHandle, column ColumnHandle, domain Domain) : String from class com.facebook.presto.sql.planner.PlanPrinter to private formatDomain(table TableHandle, column ColumnHandle, domain Domain) : String from class com.facebook.presto.sql.planner.PlanPrinter.Visitor",
            "Move Method private printConstraint(indent int, table TableHandle, column ColumnHandle, constraint TupleDomain<ColumnHandle>) : void from class com.facebook.presto.sql.planner.PlanPrinter to private printConstraint(indent int, table TableHandle, column ColumnHandle, constraint TupleDomain<ColumnHandle>) : void from class com.facebook.presto.sql.planner.PlanPrinter.Visitor");
          processFp("https://github.com/facebook/presto.git", "11048642b1e6b0e35efefab9e4e693b09c8753f5", 
            "Move Method public createPageSink(tableHandle InsertTableHandle) : ConnectorPageSink from class com.facebook.presto.split.PageSinkManager to public createPageSink(session Session, tableHandle OutputTableHandle) : ConnectorPageSink from class com.facebook.presto.split.PageSinkManager",
            "Move Method public createPageSink(tableHandle OutputTableHandle) : ConnectorPageSink from class com.facebook.presto.split.PageSinkManager to public createPageSink(session Session, tableHandle OutputTableHandle) : ConnectorPageSink from class com.facebook.presto.split.PageSinkManager",
            "Move Method public getIndex(indexHandle IndexHandle, lookupSchema List<ColumnHandle>, outputSchema List<ColumnHandle>) : ConnectorIndex from class com.facebook.presto.index.IndexManager to public getIndex(session Session, indexHandle IndexHandle, lookupSchema List<ColumnHandle>, outputSchema List<ColumnHandle>) : ConnectorIndex from class com.facebook.presto.index.IndexManager");

          processFp("https://github.com/processing/processing.git", "7a21fb27dbae2ab4b32f41d37066c7108f173178", 
            "Inline Method package arrayCheck(array int[], size int, requested int) : int[] inlined to package sort(tessGeo TessGeometry) : void in class processing.opengl.PGraphicsOpenGL.DepthSorter",
            "Extract Method package sortByMinZ(leftTid int, rightTid int, triangleIndices int[], minZBuffer float[]) : void extracted from package sort(tessGeo TessGeometry) : void in class processing.opengl.PGraphicsOpenGL.DepthSorter");

          processFp("https://github.com/droolsjbpm/jbpm.git", "ef9b6ba2b2464587b1d77cbe0a7497d64e260979", 
            "Inline Method private unsupported() : T inlined to public execute(command Command<T>) : T in class org.jbpm.services.ejb.impl.TaskServiceEJBImpl");
          processTp("https://github.com/skylot/jadx.git", "2d8d4164830631d3125575f055b417c5addaa22f", 
            "Extract Method public getDefinitionPosition(javaNode JavaNode) : CodePosition extracted from public getDefinitionPosition(line int, offset int) : CodePosition in class jadx.api.JavaClass",
            "Extract Method public getJavaNodeAtPosition(line int, offset int) : JavaNode extracted from public getDefinitionPosition(line int, offset int) : CodePosition in class jadx.api.JavaClass");
          processFp("https://github.com/skylot/jadx.git", "2d8d4164830631d3125575f055b417c5addaa22f", 
            "Move Method public hashCode() : int from class jadx.core.codegen.CodeWriter to public hashCode() : int from class jadx.api.JavaField");
          processTp("https://github.com/wildfly/wildfly.git", "d7675fb0b19d3d22978e79954f441eeefd74a3b2", 
            "Extract Method private handleExcludeMethods(componentDescription EJBComponentDescription, excludeList ExcludeListMetaData) : void extracted from protected handleDeploymentDescriptor(deploymentUnit DeploymentUnit, deploymentReflectionIndex DeploymentReflectionIndex, componentClass Class<?>, componentDescription EJBComponentDescription) : void in class org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor",
            "Extract Method private handleMethodPermissions(componentDescription EJBComponentDescription, methodPermissions MethodPermissionsMetaData) : void extracted from protected handleDeploymentDescriptor(deploymentUnit DeploymentUnit, deploymentReflectionIndex DeploymentReflectionIndex, componentClass Class<?>, componentDescription EJBComponentDescription) : void in class org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor");

          processTp("https://github.com/neo4j/neo4j.git", "03ece4f24163204d8a3948eb53576f1feaa86a61", 
            "Move Attribute private fileLock : FileLock from class org.neo4j.kernel.impl.store.standard.StoreOpenCloseCycle to class org.neo4j.io.pagecache.impl.SingleFilePageSwapper",
            "Move Attribute private fileLock : FileLock from class org.neo4j.kernel.impl.store.CommonAbstractStore to class org.neo4j.io.pagecache.impl.SingleFilePageSwapper");
          processFp("https://github.com/neo4j/neo4j.git", "03ece4f24163204d8a3948eb53576f1feaa86a61", 
            "Inline Method private checkLastCommittedTxIdInLogAndNeoStore(txId long) : void inlined to public shouldContainTransactionsThatHappenDuringBackupProcess() : void in class org.neo4j.backup.BackupServiceIT");
          processTp("https://github.com/openhab/openhab.git", "f25fa3ae35e4a60a2b7f79a88f14d46ce6cebf55", 
              // strange bug: Extract Method private initTimeMap() : Map<String,Integer> extracted from public parameters() : Collection<Object[] in class org.openhab.core.library.types.DateTimeTypeTest
            "Extract Method private initTimeMap() : Map<String,Integer> extracted from public parameters() : Collection in class org.openhab.core.library.types.DateTimeTypeTest");

          processTp("https://github.com/selendroid/selendroid.git", "e4a309c160285708f917ea23238573da3b677f7f", 
            "Extract Method protected toByteArray(image BufferedImage) : byte[] extracted from public takeScreenshot() : byte[] in class io.selendroid.standalone.android.impl.AbstractDevice");

          processTp("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", 
            "Extract Method public formatCSV(writer Writer, header boolean, delimiter char, nullString String) : void extracted from public formatCSV(writer Writer, delimiter char, nullString String) : void in class org.jooq.impl.ResultImpl",
            "Extract Method public formatCSV(stream OutputStream, header boolean, delimiter char, nullString String) : void extracted from public formatCSV(stream OutputStream, delimiter char, nullString String) : void in class org.jooq.impl.ResultImpl",
            "Extract Method public formatCSV(header boolean, delimiter char, nullString String) : String extracted from public formatCSV(delimiter char, nullString String) : String in class org.jooq.impl.ResultImpl",
            "Extract Method public formatCSV(writer Writer, header boolean, delimiter char) : void extracted from public formatCSV(writer Writer, delimiter char) : void in class org.jooq.impl.ResultImpl",
            "Extract Method public formatCSV(header boolean, delimiter char) : String extracted from public formatCSV(delimiter char) : String in class org.jooq.impl.ResultImpl",
            "Extract Method public formatCSV(writer Writer, header boolean) : void extracted from public formatCSV(writer Writer) : void in class org.jooq.impl.ResultImpl",
            "Extract Method public formatCSV(stream OutputStream, header boolean) : void extracted from public formatCSV(stream OutputStream) : void in class org.jooq.impl.ResultImpl",
            "Extract Method public formatCSV(header boolean) : String extracted from public formatCSV() : String in class org.jooq.impl.ResultImpl");

          processTp("https://github.com/eclipse/vert.x.git", "0ef66582ffaba9a8df1cad846880df2074d34505", 
            "Extract Method private init() : void extracted from public TCPSSLOptions() in class io.vertx.core.net.TCPSSLOptions",
            "Extract Method private init() : void extracted from public NetServerOptions() in class io.vertx.core.net.NetServerOptions",
            "Extract Method private init() : void extracted from public NetClientOptions() in class io.vertx.core.net.NetClientOptions",
            "Extract Method private init() : void extracted from public ClientOptionsBase() in class io.vertx.core.net.ClientOptionsBase",
            "Extract Method private init() : void extracted from public HttpServerOptions() in class io.vertx.core.http.HttpServerOptions",
            "Extract Method private init() : void extracted from public HttpClientOptions() in class io.vertx.core.http.HttpClientOptions");

          processTp("https://github.com/hazelcast/hazelcast.git", "f1e26fa73074a89680a2e1756d85eb80ad87c3bf", 
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

          processTp("https://github.com/apache/hive.git", "4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6", 
            "Extract Method private dedupWithTableInfo() : void extracted from private writeObject(oos ObjectOutputStream) : void in class org.apache.hive.hcatalog.mapreduce.PartInfo",
            "Extract Method private restoreLocalInfoFromTableInfo() : void extracted from package setTableInfo(thatTableInfo HCatTableInfo) : void in class org.apache.hive.hcatalog.mapreduce.PartInfo");
          processFp("https://github.com/apache/hive.git", "4ccc0c37aabbd90ecaa36fcc491e2270e7e9bea6", 
            "Extract Method private restoreLocalInfoFromTableInfo() : void extracted from private writeObject(oos ObjectOutputStream) : void in class org.apache.hive.hcatalog.mapreduce.PartInfo");
          processTp("https://github.com/apache/giraph.git", "03ade425dd5a65d3a713d5e7d85aa7605956fbd2", 
            "Move Attribute private edgeStore : EdgeStore<I,V,E> from class org.apache.giraph.comm.ServerData to class org.apache.giraph.partition.SimplePartitionStore");


          processFp("https://github.com/apache/hive.git", "92e9772068123ca1d2ed95435d28473b1ebb54fe", 
            "Extract Method private PartitionDescConstructorHelper(part Partition, tblDesc TableDesc, setInputFileFormat boolean) : void extracted from public PartitionDesc(part Partition, tblDesc TableDesc) in class org.apache.hadoop.hive.ql.plan.PartitionDesc",
            "Extract Method private PartitionDescConstructorHelper(part Partition, tblDesc TableDesc, setInputFileFormat boolean) : void extracted from public PartitionDesc(part Partition) in class org.apache.hadoop.hive.ql.plan.PartitionDesc");
          processTp("https://github.com/VoltDB/voltdb.git", "c1359c843bd03a694f846c8140e24ed646bbb913", 
            "Extract Method private createSchema(config Configuration, ddl String, sitesPerHost int, hostCount int, replication int) : void extracted from public testCreateDropIndexonView() : void in class org.voltdb.TestAdhocCreateDropIndex",
            "Extract Method private createSchema(config Configuration, ddl String, sitesPerHost int, hostCount int, replication int) : void extracted from public testBasicCreateIndex() : void in class org.voltdb.TestAdhocCreateDropIndex");


          processFp("https://github.com/cwensel/cascading.git", "47ead800f9bbc26abd1b63016824ee72ff036eda", 
            "Inline Method private getJobStatusClient() : RunningJob inlined to protected captureChildDetailInternal() : boolean in class cascading.stats.hadoop.HadoopNodeStats");
          processTp("https://github.com/google/closure-compiler.git", "545a7d027b4c55c116dc52d9cd8121fbb09777f0", 
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.UnknownType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.UnionType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.TemplatizedType",
            "Extract Method package isSubtype(typeA ObjectType, typeB RecordType, implicitImplCache ImplCache) : boolean extracted from package isSubtype(typeA ObjectType, typeB RecordType) : boolean in class com.google.javascript.rhino.jstype.RecordType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.RecordType",
            "Extract Method protected isSubtype(other JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(other JSType) : boolean in class com.google.javascript.rhino.jstype.ArrowType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.EnumElementType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.EnumType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.FunctionType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.NoObjectType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.NoResolvedType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.NoType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.PrototypeObjectType",
            "Extract Method protected isSubtype(that JSType, implicitImplCache ImplCache) : boolean extracted from public isSubtype(that JSType) : boolean in class com.google.javascript.rhino.jstype.ProxyObjectType");
          processFp("https://github.com/google/closure-compiler.git", "545a7d027b4c55c116dc52d9cd8121fbb09777f0", 
            "Extract Method protected implicitMatch(rightType JSType, leftType JSType, implicitImplCache ImplCache) : boolean extracted from package isSubtypeHelper(thisType JSType, thatType JSType) : boolean in class com.google.javascript.rhino.jstype.JSType",
            "Extract Method protected checkObjectImplicitMatch(rightType ObjectType, leftType FunctionType, implicitImplCache ImplCache) : boolean extracted from package isSubtypeHelper(thisType JSType, thatType JSType) : boolean in class com.google.javascript.rhino.jstype.JSType");

          processFp("https://github.com/liferay/liferay-plugins.git", "ec37db551ad9295470e4c6d1fbe0e72cd09fa512", 
            "Extract Method protected checkSyncDLObjects(syncDLObjects List<SyncDLObject>, repositoryId long) : List<SyncDLObject> extracted from public getSyncDLObjectUpdate(companyId long, repositoryId long, parentFolderId long, lastAccessTime long) : SyncDLObjectUpdate in class com.liferay.sync.service.impl.SyncDLObjectServiceImpl",
            "Inline Method protected checkSyncDLObjects(syncDLObjects List<SyncDLObject>, companyId long, repositoryId long) : List<SyncDLObject> inlined to public getSyncDLObjectUpdate(repositoryId long, parentFolderId long, lastAccessTime long) : SyncDLObjectUpdate in class com.liferay.sync.service.impl.SyncDLObjectServiceImpl");

          processFp("https://github.com/liferay/liferay-plugins.git", "6dd5d03a55fcfbaaa2719987947116ffd06f7cda", 
            "Extract Method protected checkSyncDLObjects(syncDLObjects List<SyncDLObject>, repositoryId long) : List<SyncDLObject> extracted from public getSyncDLObjectUpdate(companyId long, repositoryId long, parentFolderId long, lastAccessTime long) : SyncDLObjectUpdate in class com.liferay.sync.service.impl.SyncDLObjectServiceImpl",
            "Inline Method protected checkSyncDLObjects(syncDLObjects List<SyncDLObject>, companyId long, repositoryId long) : List<SyncDLObject> inlined to public getSyncDLObjectUpdate(repositoryId long, parentFolderId long, lastAccessTime long) : SyncDLObjectUpdate in class com.liferay.sync.service.impl.SyncDLObjectServiceImpl");

          processFp("https://github.com/hazelcast/hazelcast.git", "bef95d303c1a0eb13a4eef30ebe1511724c1d4b2", 
            "Extract Method public stop() : void extracted from public shutdown() : void in class com.hazelcast.nio.tcp.TcpIpConnectionManager");
          processTp("https://github.com/hazelcast/hazelcast.git", "204bf49cba03fe5d581a35ff82dd22587a681f46", 
            "Extract Method private createConfig() : Config extracted from private testWaitNotifyService_whenNodeSplitFromCluster(action SplitAction) : void in class com.hazelcast.spi.impl.operationservice.impl.InvocationNetworkSplitTest",
            "Extract Method private createConfig() : Config extracted from private testWaitingInvocations_whenNodeSplitFromCluster(splitAction SplitAction) : void in class com.hazelcast.spi.impl.operationservice.impl.InvocationNetworkSplitTest");
          processFp("https://github.com/hazelcast/hazelcast.git", "204bf49cba03fe5d581a35ff82dd22587a681f46", 
            "Inline Method private sendHearBeatIfRequired(now long, member MemberImpl) : void inlined to private heartBeaterSlave(now long, clockJump long) : void in class com.hazelcast.cluster.impl.ClusterServiceImpl",
            "Inline Method private sendHearBeatIfRequired(now long, member MemberImpl) : void inlined to private heartBeaterMaster(now long, clockJump long) : void in class com.hazelcast.cluster.impl.ClusterServiceImpl");
          processTp("https://github.com/hazelcast/hazelcast.git", "e84e96ff5c2bdc48cea7f75fd794506159c4e1f7", 
            "Move Attribute public DATA_FULL_NAME : String from class com.hazelcast.client.protocol.generator.CodecModel to class com.hazelcast.client.protocol.generator.CodeGenerationUtils",
            "Move Method public convertTypeToCSharp(type String) : String from class com.hazelcast.client.protocol.generator.CodecModel.ParameterModel to public convertTypeToCSharp(type String) : String from class com.hazelcast.client.protocol.generator.CodeGenerationUtils",
            "Extract Method private createCodecModel(methodElement ExecutableElement, lang Lang) : CodecModel extracted from public generateCodec(methodElement ExecutableElement, lang Lang) : void in class com.hazelcast.client.protocol.generator.CodecCodeGenerator");

          processTp("https://github.com/spring-projects/spring-roo.git", "0bb4cca1105fc6eb86e7c4b75bfff3dbbd55f0c8", 
            "Pull Up Method public setGenericDefinition(definition String) : void from class org.springframework.roo.classpath.details.MethodMetadataBuilder to public setGenericDefinition(genericDefinition String) : void from class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder",
            "Pull Up Method public getGenericDefinition() : String from class org.springframework.roo.classpath.details.MethodMetadataBuilder to public getGenericDefinition() : String from class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder",
            "Pull Up Attribute private genericDefinition : String from class org.springframework.roo.classpath.details.MethodMetadataBuilder to class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder");

          processTp("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", 
            "Extract Method protected parseTransactions(transactionsOffset int) : void extracted from protected parseTransactions() : void in class org.bitcoinj.core.Block",
            "Extract Method public makeTransaction(payloadBytes byte[], offset int, length int, hash byte[]) : Transaction extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer",
            "Extract Method public makeInventoryMessage(payloadBytes byte[], length int) : InventoryMessage extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer",
            "Extract Method public makeBlock(payloadBytes byte[], length int) : Block extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer",
            "Extract Method public makeAddressMessage(payloadBytes byte[], length int) : AddressMessage extracted from private makeMessage(command String, length int, payloadBytes byte[], hash byte[], checksum byte[]) : Message in class org.bitcoinj.core.BitcoinSerializer");


          processFp("https://github.com/ignatov/intellij-erlang.git", "c0ceabc5e9e47c628f041b72c1ca3dafb876f3b4", 
            "Move Class org.intellij.erlang.compilation.ErlangCompilationTestCase.ErlangModuleTextBuilder.ParseTransformBuilder moved to org.intellij.erlang.compilation.ErlangModuleTextGenerator.ParseTransformBuilder");
          processTp("https://github.com/alibaba/druid.git", "87f3f8144b7a6cb57b6e21cd3753d09ecde0d88f", 
            "Extract Method protected printJoinType(joinType JoinType) : void extracted from public visit(x SQLJoinTableSource) : boolean in class com.alibaba.druid.sql.visitor.SQLASTOutputVisitor");

          processTp("https://github.com/eclipse/jetty.project.git", "837d1a74bb7d694220644a2539c4440ce55462cf", 
            "Extract Method private testTransparentProxyWithQuery(proxyToContext String, prefix String, target String) : void extracted from public testTransparentProxyWithQuery() : void in class org.eclipse.jetty.proxy.ProxyServletTest");

          processTp("https://github.com/clojure/clojure.git", "309c03055b06525c275b278542c881019424760e", 
            "Extract Method package sigTag(argcount int, v Var) : Object extracted from public InvokeExpr(source String, line int, column int, tag Symbol, fexpr Expr, args IPersistentVector, tailPosition boolean) in class clojure.lang.Compiler.InvokeExpr");

          processTp("https://github.com/osmandapp/Osmand.git", "e95aa8ab32a0334b9c941799060fd601297d05e4", 
            "Extract Method public showItemPopupOptionsMenu(point FavouritePoint, view View) : void extracted from public onChildClick(parent ExpandableListView, v View, groupPosition int, childPosition int, id long) : boolean in class net.osmand.plus.activities.FavoritesTreeFragment",
            "Extract Method public showItemPopupOptionsMenu(point FavouritePoint, activity Activity, view View) : void extracted from public onListItemClick(l ListView, v View, position int, id long) : void in class net.osmand.plus.activities.FavoritesListFragment");

          processTp("https://github.com/facebook/buck.git", "6ed4cf9e83fe24fc6ab6fc9ebede016c777c9725", 
            "Inline Method public sanitize(workingDir Optional<Path>, contents String, expandPaths boolean) : String inlined to public sanitize(workingDir Optional<Path>, contents String) : String in class com.facebook.buck.cxx.DebugPathSanitizer");

          processTp("https://github.com/facebook/buck.git", "ecd0ad5ab99b8d14f28881cf4f49ec01f2221776", 
            "Extract Method private computeRuleCompilerFlags(source CxxSource) : ImmutableList<String> extracted from public createPreprocessAndCompileBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType, strategy CxxPreprocessMode) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory",
            "Extract Method private computeRuleFlags(source CxxSource) : ImmutableList<String> extracted from public createPreprocessBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory",
            "Extract Method private computePlatformCompilerFlags(pic PicType, source CxxSource) : ImmutableList<String> extracted from public createPreprocessAndCompileBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType, strategy CxxPreprocessMode) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory",
            "Extract Method private computePlatformFlags(pic PicType, source CxxSource) : ImmutableList<String> extracted from public createPreprocessBuildRule(resolver BuildRuleResolver, name String, source CxxSource, pic PicType) : CxxPreprocessAndCompile in class com.facebook.buck.cxx.CxxSourceRuleFactory");

          processTp("https://github.com/bitcoinj/bitcoinj.git", "7744a00629514b9539acac05596d64af878fe747", 
            "Inline Method private testCachedParsing(lazy boolean) : void inlined to public testCachedParsing() : void in class org.bitcoinj.core.BitcoinSerializerTest");
          processFp("https://github.com/bitcoinj/bitcoinj.git", "7744a00629514b9539acac05596d64af878fe747", 
            "Push Down Method protected maybeParse() : void from class org.bitcoinj.core.Message to protected parse() : void from class org.bitcoinj.core.ListMessage");
          processTp("https://github.com/addthis/hydra.git", "7fea4c9d5ee97d4a61ad985cadc9c5c0ab2db780", 
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerTest moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerTest",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerTaskSizer moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerTaskSizer",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerConfig moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerConfig",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.HostScore moved to com.addthis.hydra.job.spawn.balancer.HostScore",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.HostAndScore moved to com.addthis.hydra.job.spawn.balancer.HostAndScore",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.JobTaskItem moved to com.addthis.hydra.job.spawn.balancer.JobTaskItem",
            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancer");

          processTp("https://github.com/wildfly/wildfly.git", "4aa2e8746b5492bbc1cf2b36af956cf3b01e40f5", 
            "Move Class org.wildfly.clustering.spi.GroupServiceNameFactory moved to org.wildfly.clustering.service.GroupServiceNameFactory");


          processFp("https://github.com/gradle/gradle.git", "681190842248310e1bcc700a4a9316cffe9659e7", 
            "Extract Interface org.gradle.deployment.internal.RunnerToken from classes [org.gradle.play.internal.run.PlayApplicationRunnerToken]");
          processTp("https://github.com/hibernate/hibernate-orm.git", "0b6ea757e34a63b1421b77ed5fbb61398377aab1", 
            "Move Class org.hibernate.jpa.test.callbacks.EntityWithLazyProperty moved to org.hibernate.jpa.test.instrument.domain.EntityWithLazyProperty");


          processFp("https://github.com/glyptodon/guacamole-client.git", "05c50e9f633ec015d84039f2de4ed7097a27817f", 
            "Inline Method protected createConnectedTunnel(context UserContext, id String, info GuacamoleClientInformation) : GuacamoleTunnel inlined to public createTunnel(request TunnelRequest) : GuacamoleTunnel in class org.glyptodon.guacamole.net.basic.TunnelRequestService");

          processFp("https://github.com/crate/crate.git", "c0ab870a6a65d9384e636a1b9560605ecc2eec54", 
            "Extract Method protected processRequestItems(shardId ShardId, request ShardUpsertRequest, extractorContextUpdate SymbolToFieldExtractorContext, implContextInsert SymbolToInputContext, killed AtomicBoolean) : ShardUpsertResponse extracted from protected shardOperationOnPrimary(clusterState ClusterState, shardRequest PrimaryOperationRequest) : PrimaryResponse<ShardUpsertResponse,ShardUpsertRequest> in class io.crate.executor.transport.TransportShardUpsertAction",
            "Extract Method protected processRequestItems(shardId ShardId, request SymbolBasedShardUpsertRequest, killed AtomicBoolean) : ShardUpsertResponse extracted from protected shardOperationOnPrimary(clusterState ClusterState, shardRequest PrimaryOperationRequest) : PrimaryResponse<ShardUpsertResponse,SymbolBasedShardUpsertRequest> in class io.crate.executor.transport.SymbolBasedTransportShardUpsertAction");
          processTp("https://github.com/ReactiveX/RxJava.git", "8ad226067434cd39ce493b336bd0659778625959", 
            "Extract Method private awaitForComplete(latch CountDownLatch, subscription Subscription) : void extracted from private blockForSingle(observable Observable<? extends T>) : T in class rx.observables.BlockingObservable",
            "Extract Method private awaitForComplete(latch CountDownLatch, subscription Subscription) : void extracted from public forEach(onNext Action1<? super T>) : void in class rx.observables.BlockingObservable");
          processFp("https://github.com/ReactiveX/RxJava.git", "8ad226067434cd39ce493b336bd0659778625959", 
            "Extract Method private getInterruptedExceptionOrNull() : InterruptedException extracted from package assertUnsubscribeIsInvoked(method String, blockingAction Action1<BlockingObservable<Void>>) : void in class rx.observables.BlockingObservableTest.InterruptionTests");
          processTp("https://github.com/apache/cassandra.git", "3bdcaa336a6e6a9727c333b433bb9f5d3afc0fb1", 
            "Move Class org.apache.cassandra.AbstractReadCommandBuilder moved to org.apache.cassandra.db.AbstractReadCommandBuilder",
            "Extract Method public dumpMemtable() : void extracted from public truncateBlocking() : void in class org.apache.cassandra.db.ColumnFamilyStore");


          processFp("https://github.com/raphw/byte-buddy.git", "46d7561a878626cee19f5f8471b73ec70bafcb61", 
            "Move Class net.bytebuddy.dynamic.scaffold.MethodGraph.Factory.Default.Key moved to net.bytebuddy.dynamic.scaffold.MethodGraph.Compiler.Default.Key");
          processTp("https://github.com/cwensel/cascading.git", "f9d3171f5020da5c359cdda28ef05172e858c464", 
            "Move Method private getPrefix() : String from class cascading.stats.tez.TezNodeStats to private getPrefix() : String from class cascading.stats.CascadingStats",
            "Move Method protected logWarn(message String, arguments Object[]) : void from class cascading.stats.tez.TezNodeStats to protected logWarn(message String, arguments Object[]) : void from class cascading.stats.CascadingStats",
            "Move Method protected logDebug(message String, arguments Object[]) : void from class cascading.stats.tez.TezNodeStats to protected logDebug(message String, arguments Object[]) : void from class cascading.stats.CascadingStats",
            "Move Method protected logInfo(message String, arguments Object[]) : void from class cascading.stats.tez.TezNodeStats to protected logInfo(message String, arguments Object[]) : void from class cascading.stats.CascadingStats",
            "Move Attribute private prefixID : String from class cascading.stats.tez.TezNodeStats to class cascading.stats.CascadingStats");
          processFp("https://github.com/cwensel/cascading.git", "f9d3171f5020da5c359cdda28ef05172e858c464", 
            "Inline Method private getJobStatusClient() : RunningJob inlined to protected captureChildDetailInternal() : boolean in class cascading.stats.hadoop.HadoopNodeStats");
          processTp("https://github.com/google/j2objc.git", "d05d92de40542e85f9f26712d976e710be82914e", 
            "Move Class com.google.devtools.j2objc.translate.LambdaExpressionTest moved to com.google.devtools.j2objc.ast.LambdaExpressionTest");

          processTp("https://github.com/netty/netty.git", "9d347ffb91f34933edb7b1124f6b70c3fc52e220", 
            "Extract Method private expand() : void extracted from public append(c char) : AppendableCharSequence in class io.netty.util.internal.AppendableCharSequence");

          processTp("https://github.com/restlet/restlet-framework-java.git", "7ffe37983e2f09637b0c84d526a2f824de652de4", 
            "Extract Method private fillRepresentation(model Model, name String, contract Contract) : void extracted from private fillRepresentations(swagger Swagger, contract Contract) : void in class org.restlet.ext.apispark.internal.conversion.swagger.v2_0.Swagger2Reader");

          processTp("https://github.com/JetBrains/MPS.git", "ce4b0e22659c16ae83d421f9621fd3e922750764", 
            "Extract Method protected renameModels(oldName String, newName String) : void extracted from public rename(newName String) : void in class jetbrains.mps.project.AbstractModule");

          processTp("https://github.com/VoltDB/voltdb.git", "deb8e5ca64fcf633edbd89523af472da813b6772", 
            "Extract Method private getNormalValue(r Random, magnitude double, min long, max long) : long extracted from private fillTable(client Client, tbl String) : void in class org.voltdb.regressionsuites.TestApproxCountDistinctSuite");

          processTp("https://github.com/netty/netty.git", "d31fa31cdcc5ea2fa96116e3b1265baa180df58a", 
            "Inline Method private comparator(ignoreCase boolean) : Comparator<CharSequence> inlined to public contains(name CharSequence, value CharSequence, ignoreCase boolean) : boolean in class io.netty.handler.codec.DefaultTextHeaders");
          processFp("https://github.com/netty/netty.git", "d31fa31cdcc5ea2fa96116e3b1265baa180df58a", 
            "Inline Method public contains(name T, value T, keyComparator Comparator<? super T>, valueComparator Comparator<? super T>) : boolean inlined to public contains(name T, value T, valueComparator Comparator<? super T>) : boolean in class io.netty.handler.codec.DefaultHeaders",
            "Inline Method private convertName(name T) : T inlined to public addObject(name T, value Object) : Headers<T> in class io.netty.handler.codec.DefaultHeaders");
          processTp("https://github.com/VoltDB/voltdb.git", "e2de877a29217a50afbd142454a330e423d86045", 
            "Pull Up Method private findAllAggPlanNodes(node AbstractPlanNode) : List<AbstractPlanNode> from class org.voltdb.planner.TestPlansApproxCountDistinct to protected findAllAggPlanNodes(fragment AbstractPlanNode) : List<AbstractPlanNode> from class org.voltdb.planner.PlannerTestCase");

          processTp("https://github.com/JetBrains/intellij-community.git", "2b76aa336d696bbbbb205e6b6998e07ae5eb4261", 
            "Move Class org.jetbrains.plugins.groovy.util.ResolveProfiler moved to com.intellij.util.profiling.ResolveProfiler");

          processTp("https://github.com/apache/drill.git", "f8197cfe1bc3671aa6878ef9d1869b2fe8e57331", 
            "Extract Interface org.apache.drill.exec.ops.OptimizerRulesContext from classes [org.apache.drill.exec.ops.QueryContext]",
            "Extract Interface org.apache.drill.exec.expr.fn.FunctionLookupContext from classes [org.apache.drill.exec.expr.fn.FunctionImplementationRegistry]");

          processTp("https://github.com/geoserver/geoserver.git", "07c26a3a1dd6fcc2494c2d755ee5a2753e0df87c", 
            "Move Class org.geoserver.wfs.xml.PropertyRule moved to org.geoserver.util.PropertyRule");

          processTp("https://github.com/brettwooldridge/HikariCP.git", "e19c6874431dc2c3046436c2ac249a0ab2ef3457", 
            "Extract Method private closeOpenStatements() : void extracted from public close() : void in class com.zaxxer.hikari.proxy.ConnectionProxy");

          processTp("https://github.com/jOOQ/jOOQ.git", "58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357", 
            "Extract Method private millis(temporal Temporal) : long extracted from public from(from Object) : U in class org.jooq.tools.Convert.ConvertAll");

          processTp("https://github.com/spring-projects/spring-integration.git", "4cca684f368d3ff719c62d3fa4cac3cdb7828bff", 
            "Move Class org.springframework.integration.codec.CompositeCodecTests moved to org.springframework.integration.codec.kryo.CompositeCodecTests");


          processFp("https://github.com/facebook/buck.git", "b41da953b6a643c8bfefee32321e30cf8de636c4", 
            "Extract Method private partsToValues(stringParts String[]) : ImmutableList<Integer> extracted from public compare(a String, b String) : int in class com.facebook.buck.util.VersionStringComparator");
          processTp("https://github.com/facebook/buck.git", "6c93f15f502f39dff99ecb01b56dcad7dddb0f0d", 
            "Extract Method package getEnumerator(rType RType) : ResourceIdEnumerator extracted from public addIntResourceIfNotPresent(rType RType, name String) : void in class com.facebook.buck.android.aapt.AaptResourceCollector");

          processTp("https://github.com/apache/hive.git", "0fa45e4a562fc2586b1ef06a88e9c186a0835316", 
            "Extract Method private copyOneFunction(dbName String, funcName String) : void extracted from private copyFunctions() : void in class org.apache.hadoop.hive.metastore.hbase.HBaseImport",
            "Extract Method private setupObjectStore(rdbms RawStore, roles String[], dbNames String[], tokenIds String[], tokens String[], masterKeys String[], now int) : void extracted from public doImport() : void in class org.apache.hadoop.hive.metastore.hbase.TestHBaseImport");

          processTp("https://github.com/JetBrains/intellij-plugins.git", "83b3092c1ee11b70489732f9e69b8e01c2a966f0", 
            "Extract Method private getShortErrorMessage(methodName String, filePath String, error RequestError) : String extracted from private logError(methodName String, filePath String, error RequestError) : void in class com.jetbrains.lang.dart.analyzer.DartAnalysisServerService");

          processTp("https://github.com/gwtproject/gwt.git", "892d1760c8e4c76c369cd5ec1eaed215d3a22c8a", 
            "Extract Method public startRow(rowValue T) : TableRowBuilder extracted from public startRow() : TableRowBuilder in class com.google.gwt.user.cellview.client.AbstractCellTableBuilder");


          processFp("https://github.com/raphw/byte-buddy.git", "f3faf1d452fe53e7de0f32e46a84f29361178324", 
            "Move Class net.bytebuddy.dynamic.scaffold.MethodGraph.Compiler.Default.Key.Store.Entry.ForMethod.Node moved to net.bytebuddy.dynamic.scaffold.MethodGraph.Compiler.Default.Key.Store.Entry.Resolved.Node");
          processTp("https://github.com/VoltDB/voltdb.git", "d47e58f9bbce9a816378e8a7930c1de67a864c29", 
            "Extract Method public callProcedure(ic ImportContext, procCallback ProcedureCallback, proc String, fieldList Object) : boolean extracted from public callProcedure(ic ImportContext, proc String, fieldList Object) : boolean in class org.voltdb.ImportHandler");

          processTp("https://github.com/raphw/byte-buddy.git", "fd000ca2e78fce2f8aa11e6a81e4f23c2f1348e6", 
            "Extract Method private invokeMethod(methodToken Token) : SpecialMethodInvocation extracted from public invokeSuper(methodToken Token) : SpecialMethodInvocation in class net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget");

          processTp("https://github.com/bitfireAT/davdroid.git", "5b7947034a656c463ca477e198f7728cccc9e4c1", 
            "Move Method package recurrenceSetsToAndroidString(dates List<? extends DateListProperty>) : String from class at.bitfire.davdroid.resource.LocalCalendar to public recurrenceSetsToAndroidString(dates List<? extends DateListProperty>, allDay boolean) : String from class at.bitfire.davdroid.DateUtils");

          processTp("https://github.com/MovingBlocks/Terasology.git", "dbd2d5048ae5e30fec98ddd969b6c1e91183fb65", 
            "Move Attribute private localPlayer : LocalPlayer from class org.terasology.world.chunks.remoteChunkProvider.RemoteChunkProvider.ReadyChunkRelevanceComparator to class org.terasology.world.chunks.remoteChunkProvider.RemoteChunkProvider",
            "Move Attribute private localPlayer : LocalPlayer from class org.terasology.world.chunks.remoteChunkProvider.RemoteChunkProvider.ChunkTaskRelevanceComparator to class org.terasology.world.chunks.remoteChunkProvider.RemoteChunkProvider");

          processTp("https://github.com/droolsjbpm/jbpm.git", "3815f293ba9338f423315d93a373608c95002b15", 
            "Extract Superclass org.jbpm.process.audit.JPAService from classes [org.jbpm.process.audit.JPAAuditLogService]");
          processFp("https://github.com/droolsjbpm/jbpm.git", "3815f293ba9338f423315d93a373608c95002b15", 
            "Extract Method private getOrderByListId(field OrderBy) : String extracted from public language(language String) : TaskQueryBuilder in class org.jbpm.services.task.impl.TaskQueryBuilderImpl",
            "Inline Method private resetGroup() : void inlined to public clear() : void in class org.jbpm.query.jpa.data.QueryWhere",
            "Move Method private joinTransaction(em EntityManager) : Object from class org.jbpm.process.audit.JPAAuditLogService to protected joinTransaction(em EntityManager) : Object from class org.jbpm.executor.impl.jpa.ExecutorJPAAuditService",
            "Move Method private getEntityManager() : EntityManager from class org.jbpm.process.audit.JPAAuditLogService to protected getEntityManager() : EntityManager from class org.jbpm.executor.impl.jpa.ExecutorJPAAuditService");

          processFp("https://github.com/checkstyle/checkstyle.git", "d282d5b8db9eba5943d1cb0269315744d5344a47", 
            "Extract Method private validate(details Details, bracePolicy RightCurlyOption, shouldStartLine boolean, targetSourceLine String) : String extracted from public visitToken(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.blocks.RightCurlyCheck",
            "Inline Method private validate(details Details, rcurly DetailAST, lcurly DetailAST) : void inlined to public visitToken(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.blocks.RightCurlyCheck");
          processTp("https://github.com/processing/processing.git", "f36b736cf1206dd1af794d6fb4cee967a3553b1f", 
            "Extract Method private createDepthAndStencilBuffer(multisample boolean, depthBits int, stencilBits int, packed boolean) : void extracted from private createFBOLayer() : void in class processing.opengl.PGL");

          processTp("https://github.com/infinispan/infinispan.git", "043030723632627b0908dca6b24dae91d3dfd938", 
            "Extract Method private rehashAwareIteration(complete AtomicBoolean, consumer Consumer<R>, supplier IteratorSupplier<R>, iteratorParallelDistribute boolean) : void extracted from package remoteIterator() : Iterator<R> in class org.infinispan.stream.impl.DistributedCacheStream",
            "Extract Method private ignoreRehashIteration(consumer Consumer<R>, supplier IteratorSupplier<R>, iteratorParallelDistribute boolean) : void extracted from package remoteIterator() : Iterator<R> in class org.infinispan.stream.impl.DistributedCacheStream");

          processTp("https://github.com/MovingBlocks/Terasology.git", "8f63cc5c8edb8e740026447bc4827f8e8e6c34b1", 
            "Extract Method private ensurePreviewUnloaded() : boolean extracted from public onClosed() : void in class org.terasology.rendering.nui.layers.mainMenu.PreviewWorldScreen");

          processTp("https://github.com/jersey/jersey.git", "d94ca2b27c9e8a5fa9fe19483d58d2f2ef024606", 
            "Move Class org.glassfish.jersey.client.HttpUrlConnector moved to org.glassfish.jersey.client.internal.HttpUrlConnector");

          processTp("https://github.com/dropwizard/metrics.git", "2331fe19ea88a22de32f15375de8118226eaa1e6", 
            "Extract Method private registerMetricsForModel(resourceModel ResourceModel) : void extracted from public onEvent(event ApplicationEvent) : void in class io.dropwizard.metrics.jersey2.InstrumentedResourceMethodApplicationListener");


          processFp("https://github.com/dropwizard/metrics.git", "7ba7631d5f1e473aad1063499492008369663869", 
            "Move Class com.codahale.metrics.jersey2.resources.InstrumentedSubResource moved to io.dropwizard.metrics.jersey2.resources.InstrumentedSubResource");
          processTp("https://github.com/gradle/gradle.git", "3a7ccf5a252077332b9505acb22f190745f726f7", 
            "Move Method private registerOrFindDeploymentHandle(deploymentRegistry DeploymentRegistry, deploymentId String) : PlayApplicationDeploymentHandle from class org.gradle.play.plugins.PlayApplicationPlugin.Rules to private registerOrFindDeploymentHandle(deploymentId String) : PlayApplicationDeploymentHandle from class org.gradle.play.tasks.PlayRun");

          processTp("https://github.com/JoanZapata/android-iconify.git", "eb500cca282e39d01a9882e1d0a83186da6d1a26", 
            "Extract Method private copy(inputStream InputStream, outputFile File) : void extracted from package resourceToFile(context Context, resourceName String) : File in class com.joanzapata.android.iconify.Utils");

          processTp("https://github.com/apache/cassandra.git", "e37d577b6cfc2d3e11252cef87ab9ebba72e1d52", 
            "Extract Method public assertUdfsEnabled(language String) : void extracted from public create(name FunctionName, argNames List<ColumnIdentifier>, argTypes List<AbstractType<?>>, returnType AbstractType<?>, calledOnNullInput boolean, language String, body String) : UDFunction in class org.apache.cassandra.cql3.functions.UDFunction");

          processTp("https://github.com/apache/pig.git", "92dce401344a28ff966ad4cf3dd969a676852315", 
            "Extract Method public depthFirstSearchForFile(statusArray FileStatus[], fileSystem FileSystem, filter PathFilter) : Path extracted from public depthFirstSearchForFile(statusArray FileStatus[], fileSystem FileSystem) : Path in class org.apache.pig.impl.util.Utils");

          processTp("https://github.com/linkedin/rest.li.git", "54fa890a6af4ccf564fb481d3e1b6ad4d084de9e", 
            "Extract Method public addResponseCompressionHeaders(responseCompressionOverride CompressionOption, req RestRequest) : RestRequest extracted from public onRestRequest(req RestRequest, requestContext RequestContext, wireAttrs Map<String,String>, nextFilter NextFilter<RestRequest,RestResponse>) : void in class com.linkedin.r2.filter.compression.ClientCompressionFilter",
            "Move Method public testEncodingGeneration(encoding EncodingType[], acceptEncoding String) : void from class com.linkedin.restli.examples.TestCompressionServer to public testEncodingGeneration(encoding EncodingType[], acceptEncoding String) : void from class com.linkedin.r2.filter.compression.TestClientCompressionFilter",
            "Move Method public contentEncodingGeneratorDataProvider() : Object[][] from class com.linkedin.restli.examples.TestCompressionServer to public contentEncodingGeneratorDataProvider() : Object[][] from class com.linkedin.r2.filter.compression.TestClientCompressionFilter",
            "Move Method public shouldCompressRequest(entityLength int, requestCompressionOverride CompressionOption) : boolean from class com.linkedin.r2.filter.CompressionConfig to private shouldCompressRequest(entityLength int, requestCompressionOverride CompressionOption) : boolean from class com.linkedin.r2.filter.compression.ClientCompressionFilter");

          processTp("https://github.com/square/mortar.git", "72dda3404820a82d53f1a16bb2ed9ad95f745d3c", 
            "Move Method public isDestroyedGetsSet() : void from class mortar.ObjectGraphServiceTest to public isDestroyedGetsSet() : void from class mortar.MortarScopeTest",
            "Move Method public isDestroyedStartsFalse() : void from class mortar.ObjectGraphServiceTest to public isDestroyedStartsFalse() : void from class mortar.MortarScopeTest",
            "Move Method public rootDestroyIsIdempotent() : void from class mortar.ObjectGraphServiceTest to public rootDestroyIsIdempotent() : void from class mortar.MortarScopeTest",
            "Move Method public destroyIsIdempotent() : void from class mortar.ObjectGraphServiceTest to public destroyIsIdempotent() : void from class mortar.MortarScopeTest",
            "Move Method public cannotFindChildFromDestroyed() : void from class mortar.ObjectGraphServiceTest to public cannotFindChildFromDestroyed() : void from class mortar.MortarScopeTest",
            "Move Method public cannotRegisterOnDestroyed() : void from class mortar.ObjectGraphServiceTest to public cannotRegisterOnDestroyed() : void from class mortar.MortarScopeTest");

          processTp("https://github.com/apache/cassandra.git", "1a2c1bcdc7267abec9b19d77726aedbb045d79a8", 
            "Extract Method public minorWasTriggered(keyspace String, cf String) : boolean extracted from public testTriggerMinorCompaction() : void in class org.apache.cassandra.db.compaction.CompactionsCQLTest");

          processTp("https://github.com/spring-projects/spring-hateoas.git", "8bdc57ba8975d851fe91edc908761aacea624766", 
            "Extract Method private assertCanWrite(converter GenericHttpMessageConverter<Object>, type Class<?>, expected boolean) : void extracted from public canWriteTypeIfAssignableToConfiguredType() : void in class org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverterUnitTest",
            "Extract Method private assertCanRead(converter GenericHttpMessageConverter<Object>, type Class<?>, expected boolean) : void extracted from public canReadTypeIfAssignableToConfiguredType() : void in class org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverterUnitTest");

          processTp("https://github.com/JetBrains/intellij-community.git", "6b90205e9f7bbd1570f600d3812fd3efa1a0597e", 
            "Move Class com.intellij.execution.console.RunIdeConsoleAction.IDE moved to com.intellij.ide.script.IDE");

          processTp("https://github.com/JetBrains/intellij-community.git", "d12e1c16d1c73142334e689eb01f20abaeba84b0", 
            "Move Attribute public ORIGINAL_DOCUMENT : Key<Document> from class com.intellij.diff.actions.DocumentFragmentContent to class com.intellij.openapi.command.undo.UndoManager");


          processFp("https://github.com/neo4j/neo4j.git", "716173c5eb0629661bfb44c8e18741b62c82340a", 
            "Inline Method protected getFileChannel() : StoreChannel inlined to private calculateHighestIdInUseByLookingAtFileSize() : long in class org.neo4j.kernel.impl.store.CommonAbstractStore",
            "Inline Method protected closeFileChannel() : void inlined to public close() : void in class org.neo4j.kernel.impl.store.CommonAbstractStore");
          processTp("https://github.com/gradle/gradle.git", "c41466b6fd11ef4edc40cb9fd42dc13cf4f6fde1", 
            "Inline Method public resolveMetaDataArtifact(artifact ModuleComponentArtifactMetaData, result ResourceAwareResolveResult) : LocallyAvailableExternalResource inlined to public resolveArtifact(artifact ModuleComponentArtifactMetaData, result ResourceAwareResolveResult) : LocallyAvailableExternalResource in class org.gradle.api.internal.artifacts.repositories.resolver.DefaultExternalResourceArtifactResolver");

          processTp("https://github.com/addthis/hydra.git", "664923815b5aeeba2025bfe1dc5a0cd1a02a80e2", 
            "Extract Method public updateHits(state DataTreeNodeUpdater, path TreeDataParent) : boolean extracted from public updateChildData(state DataTreeNodeUpdater, path TreeDataParent) : void in class com.addthis.hydra.data.tree.concurrent.ConcurrentTreeNode");

          processTp("https://github.com/HdrHistogram/HdrHistogram.git", "0e65ac4da70c6ca5c67bb8418e67db914218042f", 
            "Extract Method private getIntervalHistogram() : EncodableHistogram extracted from public run() : void in class org.HdrHistogram.HistogramLogProcessor");

          processTp("https://github.com/JoanZapata/android-iconify.git", "b08f28a10d050beaba6250e9e9c46efe13d9caaa", 
            "Move Class android.widget.IconToggleButton moved to com.joanzapata.android.iconify.views.IconToggleButton",
            "Move Class android.widget.IconTextView moved to com.joanzapata.android.iconify.views.IconTextView",
            "Move Class android.widget.IconButton moved to com.joanzapata.android.iconify.views.IconButton");
          processFp("https://github.com/JoanZapata/android-iconify.git", "b08f28a10d050beaba6250e9e9c46efe13d9caaa", 
            "Inline Method public addIcons(icon T, textViews TextView) : void inlined to public addIcons(textViews TextView) : void in class com.joanzapata.android.iconify.Iconify");
          processTp("https://github.com/hibernate/hibernate-orm.git", "7ccbd4693288dbdbc2e6844aa0877640d63fbd04", 
            "Move Class org.hibernate.test.annotations.enumerated.LastNumberType moved to org.hibernate.test.annotations.enumerated.custom_types.LastNumberType",
            "Move Class org.hibernate.test.annotations.enumerated.FirstLetterType moved to org.hibernate.test.annotations.enumerated.custom_types.FirstLetterType");

          processTp("https://github.com/github/android.git", "c97659888126e43e95f0d52d22188bfe194a8439", 
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

          processTp("https://github.com/JetBrains/intellij-community.git", "98ecc4cfd927f388645f6a6bc492c80868b1a11d", 
            "Extract Method private getFileNamesToCreate() : Set<String> extracted from private createPropertiesFiles() : List<PsiFile> in class com.intellij.lang.properties.create.CreateResourceBundleDialogComponent");

          processTp("https://github.com/neo4j/neo4j.git", "e0072aac53b3b88de787e7ca653c7e17f9499018", 
            "Extract Method private unpackBytesHeader() : int extracted from public unpackBytes() : byte[] in class org.neo4j.packstream.PackStream.Unpacker");

          processTp("https://github.com/JetBrains/intellij-community.git", "61215911ef28ca783c5106d7c01e74cf3000a866", 
            "Extract Method public format(path String, pegRevision SVNRevision) : String extracted from public put(parameters List<String>, path String, pegRevision SVNRevision) : void in class org.jetbrains.idea.svn.commandLine.CommandUtil");

          processTp("https://github.com/gradle/gradle.git", "44aab6242f8c93059612c953af950eb1870e0774", 
            "Extract Interface org.gradle.internal.file.RelativeFilePathResolver from classes [org.gradle.api.internal.file.FileResolver]");

          processTp("https://github.com/Netflix/genie.git", "b77de40c0f3dd43a16f2491558594a61682271fc", 
            "Move Class com.netflix.genie.server.startup.TestGenieApplication moved to com.netflix.genie.web.startup.TestGenieApplication",
            "Move Class com.netflix.genie.server.health.TestHealthCheck moved to com.netflix.genie.web.health.TestHealthCheck",
            "Move Class com.netflix.genie.server.startup.GenieApplication moved to com.netflix.genie.web.startup.GenieApplication",
            "Move Class com.netflix.genie.server.health.HealthCheck moved to com.netflix.genie.web.health.HealthCheck");

          processTp("https://github.com/geoserver/geoserver.git", "e78cda0fcf23de3973b659bc54f58a4e9b1f3bd3", 
            "Extract Superclass org.geoserver.ogr.core.ToolConfiguration from classes [org.geoserver.wfs.response.OgrConfiguration]",
            "Extract Superclass org.geoserver.ogr.core.Format from classes [org.geoserver.wfs.response.OgrFormat]",
            "Extract Superclass org.geoserver.ogr.core.AbstractToolWrapper from classes [org.geoserver.wfs.response.OGRWrapper]",
            "Extract Superclass org.geoserver.ogr.core.AbstractToolConfigurator from classes [org.geoserver.wfs.response.Ogr2OgrConfigurator]");

          processTp("https://github.com/wildfly/wildfly.git", "bf35b533f067b51d4c373c5e5124d88525db99f3", 
            "Move Class org.wildfly.extension.batch.jberet.deployment.BatchEnvironmentProcessor.JobXmlFilter moved to org.wildfly.extension.batch.jberet.deployment.WildFlyJobXmlResolver.JobXmlFilter");


          processFp("https://github.com/spring-projects/spring-batch.git", "03503247053848164858e40255c9750503e908fb", 
            "Inline Method public doDestroy() : void inlined to public destroy() : void in class test.jdbc.datasource.DataSourceInitializer",
            "Inline Method private initialize() : void inlined to public afterPropertiesSet() : void in class test.jdbc.datasource.DataSourceInitializer");
          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "153611deab149accd8aeaf03fd102c0b069bd322", 
            "Extract Method public of(cells Cell[][], row int, column int, size byte) : Cell extracted from public of(row int, column int, size byte) : Cell in class com.android.internal.widget.LockPatternView.Cell",
            "Extract Method public stringToPattern(string String, size byte) : List<LockPatternView.Cell> extracted from public stringToPattern(string String) : List<LockPatternView.Cell> in class com.android.internal.widget.LockPatternUtils");

          processTp("https://github.com/JetBrains/MPS.git", "61b5decd4a4e5e6bbdea99eb76f136ca10915b73", 
            "Extract Method public startInsertMode(editorContext EditorContext, anchorCell EditorCell, insertBefore boolean) : void extracted from public insertNewChild(editorContext EditorContext, anchorCell EditorCell, insertBefore boolean) : void in class jetbrains.mps.nodeEditor.cellProviders.AbstractCellListHandler");

          processTp("https://github.com/jayway/rest-assured.git", "7cac88b9a28efc05bdc60e585e3291862ffc9da1", 
            "Move Method public supportsOverridingKeystore() : void from class com.jayway.restassured.itest.java.SpecificationBuilderITest to public supportsOverridingKeystore() : void from class com.jayway.restassured.itest.java.SSLITest",
            "Move Method public supportsSpecifyingKeystore() : void from class com.jayway.restassured.itest.java.SpecificationBuilderITest to public supportsSpecifyingKeystore() : void from class com.jayway.restassured.itest.java.SSLITest");


          processFp("https://github.com/apache/cassandra.git", "0dd50a6cdc81ec9ff1367238876d476affcf60e2", 
            "Extract Method public findCorrectRange(t Token) : boolean extracted from public add(partition UnfilteredRowIterator) : void in class org.apache.cassandra.repair.Validator");

          processFp("https://github.com/spring-projects/spring-batch.git", "dcc9da8273f2fe25fbc9aaa001b101f296f4ddcd", 
            "Inline Method public doDestroy() : void inlined to public destroy() : void in class test.jdbc.datasource.DataSourceInitializer",
            "Inline Method private initialize() : void inlined to public afterPropertiesSet() : void in class test.jdbc.datasource.DataSourceInitializer");
          processTp("https://github.com/ignatov/intellij-erlang.git", "e3b84c8753a21b1b15cfc9aa90b5e0c56d290f41", 
            "Extract Method private addSourceRoot(module Module, sourceRootName String, rootType JpsModuleSourceRootType<?>) : VirtualFile extracted from private addSourceRoot(module Module, sourceRootName String, isTestSourceRoot boolean) : VirtualFile in class org.intellij.erlang.compilation.ErlangCompilationTestBase",
            "Extract Method private collectFiles(module Module, onlyTestModules boolean, filesCollector Processor<VirtualFile>) : void extracted from private addErlangModules(module Module, onlyTestModules boolean, erlangModules Collection<ErlangFile>) : Collection<ErlangFile> in class org.intellij.erlang.utils.ErlangModulesUtil");

          processTp("https://github.com/apache/hive.git", "8398fbf3dd0937a0a4a3d540977a95f97425f566", 
            "Extract Method public closeSparkSession() : void extracted from public close() : void in class org.apache.hadoop.hive.ql.session.SessionState");

          processTp("https://github.com/xetorthio/jedis.git", "6c3dde45e8cbd0c1fa73072fad7610275afc6240", 
            "Move Class redis.clients.jedis.SentinelCommands moved to redis.clients.jedis.commands.SentinelCommands",
            "Move Class redis.clients.jedis.ScriptingCommandsPipeline moved to redis.clients.jedis.commands.ScriptingCommandsPipeline",
            "Move Class redis.clients.jedis.Commands moved to redis.clients.jedis.commands.Commands",
            "Move Class redis.clients.jedis.ClusterPipeline moved to redis.clients.jedis.commands.ClusterPipeline",
            "Move Class redis.clients.jedis.ClusterCommands moved to redis.clients.jedis.commands.ClusterCommands",
            "Move Class redis.clients.jedis.BinaryScriptingCommandsPipeline moved to redis.clients.jedis.commands.BinaryScriptingCommandsPipeline",
            "Move Class redis.clients.jedis.BinaryScriptingCommands moved to redis.clients.jedis.commands.BinaryScriptingCommands",
            "Move Class redis.clients.jedis.BinaryRedisPipeline moved to redis.clients.jedis.commands.BinaryRedisPipeline",
            "Move Class redis.clients.jedis.BinaryJedisCommands moved to redis.clients.jedis.commands.BinaryJedisCommands",
            "Move Class redis.clients.jedis.BinaryJedisClusterCommands moved to redis.clients.jedis.commands.BinaryJedisClusterCommands",
            "Move Class redis.clients.jedis.BasicRedisPipeline moved to redis.clients.jedis.commands.BasicRedisPipeline",
            "Move Class redis.clients.jedis.BasicCommands moved to redis.clients.jedis.commands.BasicCommands",
            "Move Class redis.clients.jedis.AdvancedJedisCommands moved to redis.clients.jedis.commands.AdvancedJedisCommands",
            "Move Class redis.clients.jedis.AdvancedBinaryJedisCommands moved to redis.clients.jedis.commands.AdvancedBinaryJedisCommands",
            "Move Class redis.clients.jedis.JedisClusterBinaryScriptingCommands moved to redis.clients.jedis.commands.JedisClusterBinaryScriptingCommands",
            "Move Class redis.clients.jedis.JedisClusterCommands moved to redis.clients.jedis.commands.JedisClusterCommands",
            "Move Class redis.clients.jedis.JedisClusterScriptingCommands moved to redis.clients.jedis.commands.JedisClusterScriptingCommands",
            "Move Class redis.clients.jedis.JedisCommands moved to redis.clients.jedis.commands.JedisCommands",
            "Move Class redis.clients.jedis.MultiKeyBinaryCommands moved to redis.clients.jedis.commands.MultiKeyBinaryCommands",
            "Move Class redis.clients.jedis.MultiKeyBinaryJedisClusterCommands moved to redis.clients.jedis.commands.MultiKeyBinaryJedisClusterCommands",
            "Move Class redis.clients.jedis.MultiKeyBinaryRedisPipeline moved to redis.clients.jedis.commands.MultiKeyBinaryRedisPipeline",
            "Move Class redis.clients.jedis.MultiKeyCommands moved to redis.clients.jedis.commands.MultiKeyCommands",
            "Move Class redis.clients.jedis.MultiKeyCommandsPipeline moved to redis.clients.jedis.commands.MultiKeyCommandsPipeline",
            "Move Class redis.clients.jedis.MultiKeyJedisClusterCommands moved to redis.clients.jedis.commands.MultiKeyJedisClusterCommands",
            "Move Class redis.clients.jedis.ProtocolCommand moved to redis.clients.jedis.commands.ProtocolCommand",
            "Move Class redis.clients.jedis.ScriptingCommands moved to redis.clients.jedis.commands.ScriptingCommands");

          processTp("https://github.com/spotify/helios.git", "cc02c00b8a92ef34d1a8bcdf44a45fb01a8dea6c", 
            "Extract Method protected createJobRawOutput(job Job) : String extracted from protected createJob(job Job) : JobId in class com.spotify.helios.system.SystemTestBase");

          processTp("https://github.com/CyanogenMod/android_frameworks_base.git", "f1b8ae1c44e6ba46118c2f66eae1725259acdccc", 
            "Extract Method public of(cells Cell[][], row int, column int, size byte) : Cell extracted from public of(row int, column int, size byte) : Cell in class com.android.internal.widget.LockPatternView.Cell",
            "Extract Method public stringToPattern(string String, size byte) : List<LockPatternView.Cell> extracted from public stringToPattern(string String) : List<LockPatternView.Cell> in class com.android.internal.widget.LockPatternUtils");


          processFp("https://github.com/apache/cassandra.git", "fa6205c909656b09165da4b5ca469328a6450917", 
            "Extract Method private tempCacheFiles(cfId UUID) : Pair<File,File> extracted from public saveCache() : void in class org.apache.cassandra.cache.AutoSavingCache.Writer");
          processTp("https://github.com/spring-projects/spring-framework.git", "fffdd1e9e9dc887c3e8973147904d47d9fffbb47", 
            "Extract Method private assertExistsAndReturn(content String) : Object extracted from public exists(content String) : void in class org.springframework.test.util.JsonPathExpectationsHelper");

          processTp("https://github.com/liferay/liferay-plugins.git", "720b0d2064ecc4403809e794075e9fe8cfa857f1", 
            "Extract Method protected validate(titleMap Map<Locale,String>, startTimeJCalendar Calendar, endTimeJCalendar Calendar, untilJCalendar Calendar) : void extracted from protected validate(titleMap Map<Locale,String>, startTimeJCalendar Calendar, endTimeJCalendar Calendar) : void in class com.liferay.calendar.service.impl.CalendarBookingLocalServiceImpl");


          processFp("https://github.com/eclipse/jetty.project.git", "aac9568a30e133dad5633dd22f0a43f58346513f", 
            "Extract Method private toDetail(deflater Deflater) : String extracted from protected decompress(input byte[]) : ByteAccumulator in class org.eclipse.jetty.websocket.common.extensions.compress.CompressExtension",
            "Extract Method private toDetail(inflater Inflater) : String extracted from protected decompress(input byte[]) : ByteAccumulator in class org.eclipse.jetty.websocket.common.extensions.compress.CompressExtension");
          processTp("https://github.com/facebook/buck.git", "8d14e557e01cc607dd2db66c29d106ef01aa81f7", 
            "Extract Method public get(buildTarget BuildTarget, eventBus Optional<BuckEventBus>) : TargetNode<?> extracted from public get(buildTarget BuildTarget) : TargetNode<?> in class com.facebook.buck.parser.Parser.CachedState");

          processTp("https://github.com/facebook/buck.git", "89973a5e4f188040c5fcf87fb5a3e9167329d175", 
            "Extract Method private installAppleBundleForSimulator(params CommandRunnerParams, appleBundle AppleBundle, projectFilesystem ProjectFilesystem, processExecutor ProcessExecutor) : InstallResult extracted from private installAppleBundle(params CommandRunnerParams, appleBundle AppleBundle, projectFilesystem ProjectFilesystem, processExecutor ProcessExecutor) : InstallResult in class com.facebook.buck.cli.InstallCommand");

          processTp("https://github.com/VoltDB/voltdb.git", "cfc54e8afa7ee7d5376525a84559e90b21487ccf", 
            "Extract Method private resetLeader() : void extracted from public run() : void in class org.voltdb.importclient.kafka.KafkaStreamImporter.TopicPartitionFetcher");
          processFp("https://github.com/VoltDB/voltdb.git", "cfc54e8afa7ee7d5376525a84559e90b21487ccf", 
            "Extract Method private resetLeader() : void extracted from public getLastOffset(whichTime long) : long in class org.voltdb.importclient.kafka.KafkaStreamImporter.TopicPartitionFetcher");
    }
//    
//    @Test
//	public void testAll() throws Exception {
//
//        
//        
//        process("https://github.com/jeeeyul/eclipse-themes.git", "72f61ec9b85a740fd09d10ad711e275d2ec2e564",
//            "Move Class net.jeeeyul.eclipse.themes.test.e4app.TestView moved to net.jeeeyul.eclipse.themes.test.e4app.views.TestView",
//            "Move Class net.jeeeyul.eclipse.themes.test.e4app.SplashHandler moved to net.jeeeyul.eclipse.themes.test.e4app.handlers.SplashHandler",
//            "Move Class net.jeeeyul.eclipse.themes.test.e4app.AboutHandler moved to net.jeeeyul.eclipse.themes.test.e4app.handlers.AboutHandler");
//
//          process("https://github.com/SonarSource/sonarqube.git", "abbf32571232db81a5343db17a933a9ce6923b44",
//            "Move Class org.sonar.server.notifications.email.EmailNotificationChannel moved to org.sonar.server.notification.email.EmailNotificationChannel",
//            "Move Class org.sonar.server.notifications.email.AlertsEmailTemplate moved to org.sonar.server.notification.email.AlertsEmailTemplate",
//            "Move Class org.sonar.server.notifications.NotificationService moved to org.sonar.server.notification.NotificationService",
//            "Move Class org.sonar.server.notifications.NotificationCenter moved to org.sonar.server.notification.NotificationCenter",
//            "Move Class org.sonar.server.notifications.email.EmailNotificationChannelTest moved to org.sonar.server.notification.email.EmailNotificationChannelTest",
//            "Move Class org.sonar.server.notifications.email.AlertsEmailTemplateTest moved to org.sonar.server.notification.email.AlertsEmailTemplateTest",
//            "Move Class org.sonar.server.notifications.NotificationTest moved to org.sonar.server.notification.NotificationTest",
//            "Move Class org.sonar.server.notifications.NotificationServiceTest moved to org.sonar.server.notification.NotificationServiceTest",
//            "Move Class org.sonar.server.notifications.NotificationCenterTest moved to org.sonar.server.notification.NotificationCenterTest");
//
//          process("https://github.com/SonarSource/sonarqube.git", "4a2247c24efee48de53ca07302b6810ab7205621",
//            "Move Class org.sonar.server.custommeasure.ws.DeleteAction moved to org.sonar.server.measure.custom.ws.DeleteAction",
//            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsModule moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsModule",
//            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsAction moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsAction",
//            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWs moved to org.sonar.server.measure.custom.ws.CustomMeasuresWs",
//            "Move Class org.sonar.server.custommeasure.persistence.CustomMeasureDao moved to org.sonar.server.measure.custom.persistence.CustomMeasureDao",
//            "Move Class org.sonar.server.custommeasure.ws.DeleteActionTest moved to org.sonar.server.measure.custom.ws.DeleteActionTest",
//            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsTest moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsTest",
//            "Move Class org.sonar.server.custommeasure.ws.CustomMeasuresWsModuleTest moved to org.sonar.server.measure.custom.ws.CustomMeasuresWsModuleTest",
//            "Move Class org.sonar.server.custommeasure.persistence.CustomMeasureTesting moved to org.sonar.server.measure.custom.persistence.CustomMeasureTesting",
//            "Move Class org.sonar.server.custommeasure.persistence.CustomMeasureDaoTest moved to org.sonar.server.measure.custom.persistence.CustomMeasureDaoTest");
//
//          process("https://github.com/apache/cassandra.git", "446e2537895c15b404a74107069a12f3fc404b15",
//            "Move Class org.apache.cassandra.hadoop.BulkRecordWriter.NullOutputHandler moved to org.apache.cassandra.hadoop.cql3.CqlBulkRecordWriter.NullOutputHandler",
//            "Move Class org.apache.cassandra.hadoop.AbstractColumnFamilyInputFormat.SplitCallable moved to org.apache.cassandra.hadoop.cql3.CqlInputFormat.SplitCallable");
//
//          process("https://github.com/SonarSource/sonarqube.git", "091ec857d24bfe139d2a5ce143ffc9b32b21cd7c",
//            "Move Class org.sonar.core.component.SnapshotQueryTest moved to org.sonar.core.component.db.SnapshotQueryTest",
//            "Move Class org.sonar.core.component.SnapshotQuery moved to org.sonar.core.component.db.SnapshotQuery");
//
//          process("https://github.com/FasterXML/jackson-databind.git", "44dea1f292933192ea5287d9b3e14a7daaef3c0f",
//            "Move Class com.fasterxml.jackson.failing.TestExternalTypeId222.Issue222BeanB moved to com.fasterxml.jackson.databind.jsontype.TestExternalId.Issue222BeanB",
//            "Move Class com.fasterxml.jackson.failing.TestExternalTypeId222.Issue222Bean moved to com.fasterxml.jackson.databind.jsontype.TestExternalId.Issue222Bean");
//
//          process("https://github.com/elastic/elasticsearch.git", "f77804dad35c13d9ff96456e85737883cf7ddd99",
//            "Move Class org.elasticsearch.index.merge.policy.VersionFieldUpgraderTest moved to org.elasticsearch.index.shard.VersionFieldUpgraderTest",
//            "Move Class org.elasticsearch.index.merge.policy.VersionFieldUpgrader moved to org.elasticsearch.index.shard.VersionFieldUpgrader",
//            "Move Class org.elasticsearch.index.merge.policy.FilterDocValuesProducer moved to org.elasticsearch.index.shard.FilterDocValuesProducer",
//            "Move Class org.elasticsearch.index.merge.policy.ElasticsearchMergePolicy moved to org.elasticsearch.index.shard.ElasticsearchMergePolicy");
//
//          process("https://github.com/undertow-io/undertow.git", "d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4",
//            "Move Method private isOperator(op String) : boolean from class io.undertow.server.handlers.builder.HandlerParser to private isOperator(op String) : boolean from class io.undertow.server.handlers.builder.PredicatedHandlersParser",
//            "Move Method private isOperator(op String) : boolean from class io.undertow.predicate.PredicateParser to private isOperator(op String) : boolean from class io.undertow.server.handlers.builder.PredicatedHandlersParser",
//            "Move Class io.undertow.util.PredicateTokeniser.Token moved to io.undertow.server.handlers.builder.PredicatedHandlersParser.Token",
//            "Extract Method public addPredicatedHandler(predicate Predicate, handlerWrapper HandlerWrapper, elseBranch HandlerWrapper) : PredicatesHandler extracted from public addPredicatedHandler(predicate Predicate, handlerWrapper HandlerWrapper) : PredicatesHandler in class io.undertow.predicate.PredicatesHandler");
//
//          process("https://github.com/SonarSource/sonarqube.git", "06fa57185cba7304c69a7d6c792a15f2632e3e02",
//            "Move Class org.sonar.server.computation.measure.Measure.QualityGateStatus moved to org.sonar.server.computation.measure.QualityGateStatus");
//
//          process("https://github.com/JetBrains/intellij-community.git", "3972b9b3d4e03bdb5e62dfa663e3e0a1871e3c9f",
//            "Move Class com.intellij.psi.codeStyle.autodetect.NewLineBlocksIterator moved to com.intellij.psi.formatter.common.NewLineBlocksIterator");
//
//          process("https://github.com/facebook/buck.git", "1c7c03dd9e6d5810ad22d37ecae59722c219ac35",
//            "Move Class com.facebook.buck.cli.UninstallEventTest moved to com.facebook.buck.event.UninstallEventTest",
//            "Move Class com.facebook.buck.cli.TestDevice moved to com.facebook.buck.android.TestDevice",
//            "Move Class com.facebook.buck.cli.StartActivityEventTest moved to com.facebook.buck.event.StartActivityEventTest",
//            "Move Class com.facebook.buck.cli.InstallEventTest moved to com.facebook.buck.event.InstallEventTest",
//            "Move Class com.facebook.buck.cli.UninstallEvent moved to com.facebook.buck.event.UninstallEvent",
//            "Move Class com.facebook.buck.cli.StartActivityEvent moved to com.facebook.buck.event.StartActivityEvent",
//            "Move Class com.facebook.buck.cli.InstallEvent moved to com.facebook.buck.event.InstallEvent",
//            "Move Class com.facebook.buck.cli.AdbHelper.CommandFailedException moved to com.facebook.buck.android.AdbHelper.CommandFailedException",
//            "Move Class com.facebook.buck.cli.AdbHelper.ErrorParsingReceiver moved to com.facebook.buck.android.AdbHelper.ErrorParsingReceiver",
//            "Move Class com.facebook.buck.cli.AdbHelper.AdbCallable moved to com.facebook.buck.android.AdbHelper.AdbCallable");
//
//          process("https://github.com/JetBrains/intellij-community.git", "5f18bed8da4dda4fa516907ecbbe28f712e944f7",
//            "Move Class com.intellij.util.ui.components.JBPanel moved to com.intellij.ui.components.JBPanel");
//
//          process("https://github.com/hierynomus/sshj.git", "7c26ac669a4e17ca1d2319a5049a56424fd33104",
//            "Move Class nl.javadude.sshj.connection.channel.ChannelCloseEofTest moved to com.hierynomus.sshj.connection.channel.ChannelCloseEofTest");
//
//          process("https://github.com/facebook/buck.git", "0f8a0af934f09deef1b58e961ffe789c7299bcc1",
//            "Move Class com.facebook.buck.cxx.AbstractCxxPreprocessorInput.ConflictingHeadersException moved to com.facebook.buck.cxx.AbstractCxxHeaders.ConflictingHeadersException",
//            "Move Method private addAllEntriesToIncludeMap(destination Map<Path,SourcePath>, source Map<Path,SourcePath>) : void from class com.facebook.buck.cxx.AbstractCxxPreprocessorInput to public addAllEntriesToIncludeMap(destination Map<Path,SourcePath>, source Map<Path,SourcePath>) : void from class com.facebook.buck.cxx.AbstractCxxHeaders");
//
//          process("https://github.com/eucalyptus/eucalyptus.git", "5a38d0bca0e48853c3f7c00a0f098bada64797df",
//            "Move Class com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricCacheKey moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricQueue.AbsoluteMetricCacheKey",
//            "Move Class com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricLoadCacheKey moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricQueue.AbsoluteMetricLoadCacheKey",
//            "Move Class com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricCache moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricQueue.AbsoluteMetricCache",
//            "Move Class com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHistory moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricHistory",
//            "Move Class com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHelper moved to com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricHelper");
//
//          process("https://github.com/bennidi/mbassador.git", "40e41d11d7847d660bba6691859b0506514bd0ac",
//            "Move Class net.engio.mbassy.ConditionalHandlers.ConditionalMessageListener moved to net.engio.mbassy.ConditionalHandlerTest.ConditionalMessageListener",
//            "Move Class net.engio.mbassy.ConditionalHandlers.TestEvent moved to net.engio.mbassy.ConditionalHandlerTest.TestEvent");
//
//          process("https://github.com/Graylog2/graylog2-server.git", "2ef067fc70055fc4d55c75937303414ddcf07e0e",
//            "Extract Superclass integration.BaseRestTestHelper from classes [integration.BaseRestTest]",
//            "Move Class integration.BaseRestTest.KeysPresentMatcher moved to integration.BaseRestTestHelper.KeysPresentMatcher");
//
//          process("https://github.com/hazelcast/hazelcast.git", "e66e49cd4a9dd8027204f712f780170a5c129f5b",
//            "Move Class com.hazelcast.spi.ServiceInfo moved to com.hazelcast.spi.impl.servicemanager.ServiceInfo");
//
//          process("https://github.com/open-keychain/open-keychain.git", "49d544d558e9c7f1106b5923204b1fbec2696cf7",
//            "Move Class org.sufficientlysecure.keychain.util.orbot.OrbotHelper moved to org.sufficientlysecure.keychain.util.tor.OrbotHelper");
//
//          process("https://github.com/square/okhttp.git", "c753d2e41ba667f9b5a31451a16ecbaecdc65d80",
//            "Move Class com.squareup.okhttp.internal.spdy.Variant moved to com.squareup.okhttp.internal.framed.Variant",
//            "Move Class com.squareup.okhttp.internal.spdy.Spdy3 moved to com.squareup.okhttp.internal.framed.Spdy3",
//            "Move Class com.squareup.okhttp.internal.spdy.Settings moved to com.squareup.okhttp.internal.framed.Settings",
//            "Move Class com.squareup.okhttp.internal.spdy.PushObserver moved to com.squareup.okhttp.internal.framed.PushObserver",
//            "Move Class com.squareup.okhttp.internal.spdy.Ping moved to com.squareup.okhttp.internal.framed.Ping",
//            "Move Class com.squareup.okhttp.internal.spdy.HuffmanTest moved to com.squareup.okhttp.internal.framed.HuffmanTest",
//            "Move Class com.squareup.okhttp.internal.spdy.Http2Test moved to com.squareup.okhttp.internal.framed.Http2Test",
//            "Move Class com.squareup.okhttp.internal.spdy.Http2FrameLoggerTest moved to com.squareup.okhttp.internal.framed.Http2FrameLoggerTest",
//            "Move Class com.squareup.okhttp.internal.spdy.Http2ConnectionTest moved to com.squareup.okhttp.internal.framed.Http2ConnectionTest",
//            "Move Class com.squareup.okhttp.internal.spdy.HpackTest moved to com.squareup.okhttp.internal.framed.HpackTest",
//            "Move Class com.squareup.okhttp.internal.spdy.BaseTestHandler moved to com.squareup.okhttp.internal.framed.BaseTestHandler",
//            "Move Class com.squareup.okhttp.internal.spdy.hpackjson.Story moved to com.squareup.okhttp.internal.framed.hpackjson.Story",
//            "Move Class com.squareup.okhttp.internal.spdy.hpackjson.HpackJsonUtil moved to com.squareup.okhttp.internal.framed.hpackjson.HpackJsonUtil",
//            "Move Class com.squareup.okhttp.internal.spdy.hpackjson.Case moved to com.squareup.okhttp.internal.framed.hpackjson.Case",
//            "Move Class com.squareup.okhttp.internal.spdy.HpackRoundTripTest moved to com.squareup.okhttp.internal.framed.HpackRoundTripTest",
//            "Move Class com.squareup.okhttp.internal.spdy.HpackDecodeTestBase moved to com.squareup.okhttp.internal.framed.HpackDecodeTestBase",
//            "Move Class com.squareup.okhttp.internal.spdy.HpackDecodeInteropTest moved to com.squareup.okhttp.internal.framed.HpackDecodeInteropTest",
//            "Move Class com.squareup.okhttp.internal.spdy.MockSpdyPeer moved to com.squareup.okhttp.internal.framed.MockSpdyPeer",
//            "Move Class com.squareup.okhttp.internal.spdy.SettingsTest moved to com.squareup.okhttp.internal.framed.SettingsTest",
//            "Move Class com.squareup.okhttp.internal.spdy.Spdy3ConnectionTest moved to com.squareup.okhttp.internal.framed.Spdy3ConnectionTest",
//            "Move Class com.squareup.okhttp.internal.spdy.Spdy3Test moved to com.squareup.okhttp.internal.framed.Spdy3Test",
//            "Move Class com.squareup.okhttp.internal.spdy.FrameReader moved to com.squareup.okhttp.internal.framed.FrameReader",
//            "Move Class com.squareup.okhttp.internal.spdy.FrameWriter moved to com.squareup.okhttp.internal.framed.FrameWriter",
//            "Move Class com.squareup.okhttp.internal.spdy.Header moved to com.squareup.okhttp.internal.framed.Header",
//            "Move Class com.squareup.okhttp.internal.spdy.Hpack moved to com.squareup.okhttp.internal.framed.Hpack",
//            "Move Class com.squareup.okhttp.internal.spdy.Http2 moved to com.squareup.okhttp.internal.framed.Http2",
//            "Move Class com.squareup.okhttp.internal.spdy.Huffman moved to com.squareup.okhttp.internal.framed.Huffman",
//            "Move Class com.squareup.okhttp.internal.spdy.IncomingStreamHandler moved to com.squareup.okhttp.internal.framed.IncomingStreamHandler",
//            "Move Class com.squareup.okhttp.internal.spdy.NameValueBlockReader moved to com.squareup.okhttp.internal.framed.NameValueBlockReader");
//
//          process("https://github.com/google/closure-compiler.git", "5a853a60f93e09c446d458673bc7a2f6bb26742c",
//            "Move Class com.google.javascript.jscomp.parsing.TypeDeclarationsIRFactory moved to com.google.javascript.jscomp.JsdocToEs6TypedConverter.TypeDeclarationsIRFactory");
//
//          process("https://github.com/opentripplanner/OpenTripPlanner.git", "e32f161fc023d1ee153c49df312ae10b06941465",
//            "Move Class org.opentripplanner.analyst.qbroker.User moved to org.opentripplanner.analyst.broker.User",
//            "Move Class org.opentripplanner.analyst.qbroker.QueueType moved to org.opentripplanner.analyst.broker.QueueType",
//            "Move Class org.opentripplanner.analyst.qbroker.QueuePath moved to org.opentripplanner.analyst.broker.QueuePath",
//            "Move Class org.opentripplanner.analyst.qbroker.CircularList moved to org.opentripplanner.analyst.broker.CircularList",
//            "Move Class org.opentripplanner.analyst.qbroker.BrokerMain moved to org.opentripplanner.analyst.broker.BrokerMain",
//            "Move Class org.opentripplanner.analyst.qbroker.BrokerHttpHandler moved to org.opentripplanner.analyst.broker.BrokerHttpHandler");
//
//          process("https://github.com/brettwooldridge/HikariCP.git", "cd8c4d578a609bdd6395d3a8c49bfd19ed700dea",
//            "Move Class com.zaxxer.hikari.util.NanosecondClockSource moved to com.zaxxer.hikari.util.ClockSource.NanosecondClockSource",
//            "Move Class com.zaxxer.hikari.util.MillisecondClockSource moved to com.zaxxer.hikari.util.ClockSource.MillisecondClockSource");
//
//          process("https://github.com/brettwooldridge/HikariCP.git", "1571049ec04b1e7e6f082ed5ec071584e7200c12",
//            "Move Class com.zaxxer.hikari.util.IConcurrentBagEntry moved to com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry",
//            "Move Class com.zaxxer.hikari.util.IBagStateListener moved to com.zaxxer.hikari.util.ConcurrentBag.IBagStateListener");
//
//          process("https://github.com/SlimeKnights/TinkersConstruct.git", "71820e573134be3fad3935035249cd77c4412f4e",
//            "Move Class tconstruct.library.modifiers.RecipeMatch moved to tconstruct.library.mantle.RecipeMatch");
//
//          process("https://github.com/spring-projects/spring-framework.git", "dd4bc630c3de70204081ab196945d6b55ab03beb",
//            "Move Class org.springframework.aop.interceptor.AsyncExecutionInterceptor.CompletableFutureDelegate moved to org.springframework.aop.interceptor.AsyncExecutionAspectSupport.CompletableFutureDelegate",
//            "Pull Up Attribute private completableFuturePresent : boolean from class org.springframework.aop.interceptor.AsyncExecutionInterceptor to class org.springframework.aop.interceptor.AsyncExecutionAspectSupport");
//
//          process("https://github.com/Graylog2/graylog2-server.git", "72acc2126611f0bff9b672de18b9b2f8dacdc03a",
//            "Extract Interface org.graylog2.bootstrap.CliCommand from classes [org.graylog2.bootstrap.CmdLineTool, org.graylog2.bootstrap.commands.ShowVersion]",
//            "Move Class org.graylog2.UI moved to org.graylog2.shared.UI",
//            "Move Class org.graylog2.bootstrap.commands.journal.JournalTruncate moved to org.graylog2.commands.journal.JournalTruncate",
//            "Move Class org.graylog2.bootstrap.commands.journal.JournalShow moved to org.graylog2.commands.journal.JournalShow",
//            "Move Class org.graylog2.bootstrap.commands.journal.JournalDecode moved to org.graylog2.commands.journal.JournalDecode",
//            "Move Class org.graylog2.bootstrap.commands.journal.AbstractJournalCommand moved to org.graylog2.commands.journal.AbstractJournalCommand",
//            "Move Class org.graylog2.bootstrap.commands.Server moved to org.graylog2.commands.Server",
//            "Move Class org.graylog2.bootstrap.commands.Radio moved to org.graylog2.radio.commands.Radio");
//
//          process("https://github.com/neo4j/neo4j.git", "001de307492df8f84ad15f6aaa0bd1e748d4ce27",
//            "Move Class org.neo4j.kernel.Recovery moved to org.neo4j.kernel.recovery.Recovery");
//
//          process("https://github.com/hazelcast/hazelcast.git", "4d05a3b1168441216dcaea8282c39338285182af",
//            "Extract Superclass com.hazelcast.spi.impl.SimpleExecutionCallback from classes [com.hazelcast.cache.impl.operation.CacheCreateConfigOperation.CacheConfigCreateCallback, com.hazelcast.client.impl.client.MultiTargetClientRequest.SingleTargetCallback, com.hazelcast.client.impl.protocol.task.AbstractMultiTargetMessageTask.SingleTargetCallback, com.hazelcast.partition.impl.MigrationRequestOperation.MigrationCallback]",
//            "Move Class com.hazelcast.spi.impl.operationservice.impl.InvocationFuture.ExecutorCallbackAdapter moved to com.hazelcast.spi.InvocationBuilder.ExecutorCallbackAdapter");
//
//          process("https://github.com/mongodb/mongo-java-driver.git", "8c5a20d786e66ee4c4b0d743f0f80bf681c419be",
//            "Move Class com.mongodb.JsonPoweredTestHelper moved to util.JsonPoweredTestHelper");
//
//          process("https://github.com/WhisperSystems/TextSecure.git", "99528dcc3b4a82b5e52a87d3e7aed5c6479028c7",
//            "Inline Method private getSynchronousRecipient(context Context, recipientId long) : Recipient inlined to package getRecipient(context Context, recipientId long, asynchronous boolean) : Recipient in class org.thoughtcrime.securesms.recipients.RecipientProvider",
//            "Move Class org.thoughtcrime.securesms.contacts.ContactPhotoFactory.ExpandingLayerDrawable moved to org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto.ExpandingLayerDrawable");
//
//          process("https://github.com/springfox/springfox.git", "e70b04810eb1e73f60e3d8b3980b8271ce473565",
//            "Move Class springfox.documentation.schema.property.provider.ModelPropertiesProvider moved to springfox.documentation.schema.property.ModelPropertiesProvider",
//            "Move Class springfox.documentation.schema.property.provider.DefaultModelPropertiesProvider moved to springfox.documentation.schema.property.DefaultModelPropertiesProvider");
//
//          process("https://github.com/apache/cassandra.git", "f797bfa4da53315b49f8d97b784047f33ba1bf5f",
//            "Move Class org.apache.cassandra.cql3.CrcCheckChanceTest moved to org.apache.cassandra.cql3.validation.miscellaneous.CrcCheckChanceTest",
//            "Move Class org.apache.cassandra.cql3.SSTableMetadataTrackingTest moved to org.apache.cassandra.cql3.validation.miscellaneous.SSTableMetadataTrackingTest",
//            "Move Class org.apache.cassandra.cql3.TypeTest moved to org.apache.cassandra.cql3.validation.entities.TypeTest",
//            "Extract Method protected assertInvalidThrowMessage(errorMessage String, exception Class<? extends Throwable>, query String, values Object[]) : void extracted from protected assertInvalidMessage(errorMessage String, query String, values Object[]) : void in class org.apache.cassandra.cql3.CQLTester",
//            "Extract Method private makeCasRequest(options BatchQueryOptions, state QueryState) : Pair<CQL3CasRequest,Set<ColumnDefinition>> extracted from private executeWithConditions(options BatchQueryOptions, state QueryState) : ResultMessage in class org.apache.cassandra.cql3.statements.BatchStatement",
//            "Extract Method private executeInternalWithoutCondition(queryState QueryState, options QueryOptions) : ResultMessage extracted from public executeInternal(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.BatchStatement",
//            "Extract Method private makeCasRequest(queryState QueryState, options QueryOptions) : CQL3CasRequest extracted from public executeWithCondition(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.ModificationStatement",
//            "Extract Method public executeInternalWithoutCondition(queryState QueryState, options QueryOptions) : ResultMessage extracted from public executeInternal(queryState QueryState, options QueryOptions) : ResultMessage in class org.apache.cassandra.cql3.statements.ModificationStatement",
//            "Extract Method private fromUnixTimestamp(timestamp long, nanos long) : long extracted from private fromUnixTimestamp(timestamp long) : long in class org.apache.cassandra.utils.UUIDGen",
//            "Extract Method protected createTableName() : String extracted from protected createTable(query String) : String in class org.apache.cassandra.cql3.CQLTester");
//
//          process("https://github.com/facebook/buck.git", "84b7b3974ae8171a4de2f804eb94fcd1d6cd6647",
//            "Move Class com.facebook.buck.java.ReportGenerator moved to com.facebook.buck.java.coverage.ReportGenerator");
//
//          process("https://github.com/spring-projects/spring-data-neo4j.git", "071588a418dbc743e0f7dbfe218cd8a6c0f97421",
//            "Move Class org.springframework.data.neo4j.repository.support.GraphRepositoryFactoryTest moved to org.springframework.data.neo4j.repositories.support.GraphRepositoryFactoryTest",
//            "Move Class org.springframework.data.neo4j.integration.web.service.UserServiceImpl moved to org.springframework.data.neo4j.web.service.UserServiceImpl",
//            "Move Class org.springframework.data.neo4j.integration.web.service.UserService moved to org.springframework.data.neo4j.web.service.UserService",
//            "Move Class org.springframework.data.neo4j.integration.web.repo.UserRepository moved to org.springframework.data.neo4j.web.repo.UserRepository",
//            "Move Class org.springframework.data.neo4j.integration.web.repo.GenreRepository moved to org.springframework.data.neo4j.web.repo.GenreRepository",
//            "Move Class org.springframework.data.neo4j.integration.web.domain.User moved to org.springframework.data.neo4j.web.domain.User",
//            "Move Class org.springframework.data.neo4j.integration.web.domain.Genre moved to org.springframework.data.neo4j.web.domain.Genre",
//            "Move Class org.springframework.data.neo4j.integration.web.domain.Cinema moved to org.springframework.data.neo4j.web.domain.Cinema",
//            "Move Class org.springframework.data.neo4j.integration.web.controller.UserController moved to org.springframework.data.neo4j.web.controller.UserController",
//            "Move Class org.springframework.data.neo4j.integration.web.context.WebPersistenceContext moved to org.springframework.data.neo4j.web.context.WebPersistenceContext",
//            "Move Class org.springframework.data.neo4j.integration.web.context.WebAppContext moved to org.springframework.data.neo4j.web.context.WebAppContext",
//            "Move Class org.springframework.data.neo4j.integration.web.WebIntegrationTest moved to org.springframework.data.neo4j.web.WebIntegrationTest",
//            "Move Class org.springframework.data.neo4j.integration.transactions.service.WrapperService moved to org.springframework.data.neo4j.transactions.service.WrapperService",
//            "Move Class org.springframework.data.neo4j.integration.transactions.service.BusinessService moved to org.springframework.data.neo4j.transactions.service.BusinessService",
//            "Move Class org.springframework.data.neo4j.integration.transactions.TransactionBoundaryTest moved to org.springframework.data.neo4j.transactions.TransactionBoundaryTest",
//            "Move Class org.springframework.data.neo4j.integration.transactions.ApplicationConfig moved to org.springframework.data.neo4j.transactions.ApplicationConfig",
//            "Move Class org.springframework.data.neo4j.integration.template.context.DataManipulationEventConfiguration moved to org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration",
//            "Move Class org.springframework.data.neo4j.integration.template.TestNeo4jEventListener moved to org.springframework.data.neo4j.template.TestNeo4jEventListener",
//            "Move Class org.springframework.data.neo4j.integration.template.TemplateApplicationEventTest moved to org.springframework.data.neo4j.template.TemplateApplicationEventTest",
//            "Move Class org.springframework.data.neo4j.integration.template.Neo4jTemplateTest moved to org.springframework.data.neo4j.template.Neo4jTemplateTest",
//            "Move Class org.springframework.data.neo4j.integration.template.ExceptionTranslationTest moved to org.springframework.data.neo4j.template.ExceptionTranslationTest",
//            "Move Class org.springframework.data.neo4j.integration.repositories.repo.UserRepository moved to org.springframework.data.neo4j.repositories.repo.UserRepository",
//            "Move Class org.springframework.data.neo4j.integration.repositories.repo.PersistenceContextInTheSamePackage moved to org.springframework.data.neo4j.repositories.repo.PersistenceContextInTheSamePackage",
//            "Move Class org.springframework.data.neo4j.integration.repositories.repo.MovieRepository moved to org.springframework.data.neo4j.repositories.repo.MovieRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.Person moved to org.springframework.data.neo4j.examples.movies.domain.Person",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.Movie moved to org.springframework.data.neo4j.examples.movies.domain.Movie",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.Genre moved to org.springframework.data.neo4j.examples.movies.domain.Genre",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.Cinema moved to org.springframework.data.neo4j.examples.movies.domain.Cinema",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.Actor moved to org.springframework.data.neo4j.examples.movies.domain.Actor",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.AbstractEntity moved to org.springframework.data.neo4j.examples.movies.domain.AbstractEntity",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.AbstractAnnotatedEntity moved to org.springframework.data.neo4j.examples.movies.domain.AbstractAnnotatedEntity",
//            "Move Class org.springframework.data.neo4j.integration.movies.TransactionIntegrationTest moved to org.springframework.data.neo4j.transactions.TransactionIntegrationTest",
//            "Move Class org.springframework.data.neo4j.integration.movies.QueryIntegrationTest moved to org.springframework.data.neo4j.queries.QueryIntegrationTest",
//            "Move Class org.springframework.data.neo4j.integration.movies.DerivedQueryTest moved to org.springframework.data.neo4j.queries.DerivedQueryTest",
//            "Move Class org.springframework.data.neo4j.integration.jsr303.service.AdultService moved to org.springframework.data.neo4j.examples.jsr303.service.AdultService",
//            "Move Class org.springframework.data.neo4j.integration.jsr303.repo.AdultRepository moved to org.springframework.data.neo4j.examples.jsr303.repo.AdultRepository",
//            "Move Class org.springframework.data.neo4j.integration.extensions.CustomGraphRepository moved to org.springframework.data.neo4j.extensions.CustomGraphRepository",
//            "Move Class org.springframework.data.neo4j.integration.extensions.CustomGraphRepositoryImpl moved to org.springframework.data.neo4j.extensions.CustomGraphRepositoryImpl",
//            "Move Class org.springframework.data.neo4j.integration.extensions.CustomGraphRepositoryTest moved to org.springframework.data.neo4j.extensions.CustomGraphRepositoryTest",
//            "Move Class org.springframework.data.neo4j.integration.extensions.CustomPersistenceContext moved to org.springframework.data.neo4j.extensions.CustomPersistenceContext",
//            "Move Class org.springframework.data.neo4j.integration.extensions.UserRepository moved to org.springframework.data.neo4j.extensions.UserRepository",
//            "Move Class org.springframework.data.neo4j.integration.extensions.domain.User moved to org.springframework.data.neo4j.extensions.domain.User",
//            "Move Class org.springframework.data.neo4j.integration.helloworld.GalaxyServiceTest moved to org.springframework.data.neo4j.examples.galaxy.GalaxyServiceTest",
//            "Move Class org.springframework.data.neo4j.integration.helloworld.service.GalaxyService moved to org.springframework.data.neo4j.examples.galaxy.service.GalaxyService",
//            "Move Class org.springframework.data.neo4j.integration.jsr303.JSR303Test moved to org.springframework.data.neo4j.examples.jsr303.JSR303Test",
//            "Move Class org.springframework.data.neo4j.integration.jsr303.WebConfiguration moved to org.springframework.data.neo4j.examples.jsr303.WebConfiguration",
//            "Move Class org.springframework.data.neo4j.integration.jsr303.controller.AdultController moved to org.springframework.data.neo4j.examples.jsr303.controller.AdultController",
//            "Move Class org.springframework.data.neo4j.integration.jsr303.domain.Adult moved to org.springframework.data.neo4j.examples.jsr303.domain.Adult",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.Rating moved to org.springframework.data.neo4j.examples.movies.domain.Rating",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.ReleasedMovie moved to org.springframework.data.neo4j.examples.movies.domain.ReleasedMovie",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.TempMovie moved to org.springframework.data.neo4j.examples.movies.domain.TempMovie",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.User moved to org.springframework.data.neo4j.examples.movies.domain.User",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.EntityWrappingQueryResult moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.RichUserQueryResult moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.RichUserQueryResult",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.UserQueryResult moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResult",
//            "Move Class org.springframework.data.neo4j.integration.movies.domain.queryresult.UserQueryResultInterface moved to org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResultInterface",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.AbstractAnnotatedEntityRepository moved to org.springframework.data.neo4j.examples.movies.repo.AbstractAnnotatedEntityRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.AbstractEntityRepository moved to org.springframework.data.neo4j.examples.movies.repo.AbstractEntityRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.ActorRepository moved to org.springframework.data.neo4j.examples.movies.repo.ActorRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.CinemaRepository moved to org.springframework.data.neo4j.examples.movies.repo.CinemaRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.GenreRepository moved to org.springframework.data.neo4j.examples.movies.repo.GenreRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.RatingRepository moved to org.springframework.data.neo4j.examples.movies.repo.RatingRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.TempMovieRepository moved to org.springframework.data.neo4j.examples.movies.repo.TempMovieRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.UnmanagedUserPojo moved to org.springframework.data.neo4j.examples.movies.repo.UnmanagedUserPojo",
//            "Move Class org.springframework.data.neo4j.integration.movies.repo.UserRepository moved to org.springframework.data.neo4j.examples.movies.repo.UserRepository",
//            "Move Class org.springframework.data.neo4j.integration.movies.service.UserService moved to org.springframework.data.neo4j.examples.movies.service.UserService",
//            "Move Class org.springframework.data.neo4j.integration.movies.service.UserServiceImpl moved to org.springframework.data.neo4j.examples.movies.service.UserServiceImpl",
//            "Move Class org.springframework.data.neo4j.integration.repositories.ProgrammaticRepositoryTest moved to org.springframework.data.neo4j.repositories.ProgrammaticRepositoryTest",
//            "Move Class org.springframework.data.neo4j.integration.repositories.RepoScanningTest moved to org.springframework.data.neo4j.repositories.RepoScanningTest",
//            "Move Class org.springframework.data.neo4j.integration.repositories.RepositoryDefinitionTest moved to org.springframework.data.neo4j.repositories.RepositoryDefinitionTest",
//            "Move Class org.springframework.data.neo4j.integration.repositories.domain.Movie moved to org.springframework.data.neo4j.repositories.domain.Movie",
//            "Move Class org.springframework.data.neo4j.integration.repositories.domain.User moved to org.springframework.data.neo4j.repositories.domain.User");
//
//          process("https://github.com/mockito/mockito.git", "7f20e63a7252f33c888085134d16ee8bf45f183f",
//            "Move Class org.mockito.internal.util.text.ValuePrinter moved to org.mockito.internal.matchers.text.ValuePrinter",
//            "Move Class org.mockito.internal.util.text.HamcrestPrinter moved to org.mockito.internal.matchers.text.HamcrestPrinter",
//            "Move Class org.mockito.internal.util.text.ArrayIterator moved to org.mockito.internal.matchers.text.ArrayIterator",
//            "Move Class org.mockito.internal.matchers.MatchersPrinter moved to org.mockito.internal.matchers.text.MatchersPrinter",
//            "Extract Superclass org.mockito.MockitoMatcher from classes [org.mockito.internal.matchers.LocalizedMatcher]");
//
//          process("https://github.com/apache/drill.git", "c1b847acdc8cb90a1498b236b3bb5c81ca75c044",
//            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.TableNameLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.TableNameLoader",
//            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.DatabaseLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.DatabaseLoader",
//            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.TableLoaderLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.TableLoaderLoader",
//            "Move Class org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.TableLoader moved to org.apache.drill.exec.store.hive.DrillHiveMetaStoreClient.NonCloseableHiveClientWithCaching.TableLoader",
//            "Pull Up Attribute protected MINIDFS_STORAGE_PLUGIN_NAME : String from class org.apache.drill.exec.impersonation.TestImpersonationDisabledWithMiniDFS to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
//            "Pull Up Attribute protected MINIDFS_STORAGE_PLUGIN_NAME : String from class org.apache.drill.exec.impersonation.TestImpersonationMetadata to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
//            "Pull Up Attribute protected MINIDFS_STORAGE_PLUGIN_NAME : String from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
//            "Pull Up Attribute protected org1Users : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
//            "Pull Up Attribute protected org1Groups : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
//            "Pull Up Attribute protected org2Users : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
//            "Pull Up Attribute protected org2Groups : String[] from class org.apache.drill.exec.impersonation.TestImpersonationQueries to class org.apache.drill.exec.impersonation.BaseTestImpersonation",
//            "Extract Method private createTestData() : void extracted from public addMiniDfsBasedStorageAndGenerateTestData() : void in class org.apache.drill.exec.impersonation.TestImpersonationQueries");
//
//          process("https://github.com/facebook/facebook-android-sdk.git", "e813a0be86c87366157a0201e6c61662cadee586",
//            "Extract Method private getAndroidIdViaReflection(context Context) : AttributionIdentifiers extracted from private getAndroidId(context Context) : AttributionIdentifiers in class com.facebook.internal.AttributionIdentifiers",
//            "Move Class com.facebook.samples.switchuser.UserInfoCache moved to com.example.switchuser.UserInfoCache",
//            "Move Class com.facebook.samples.switchuser.UserInfo moved to com.example.switchuser.UserInfo",
//            "Move Class com.facebook.samples.switchuser.Slot moved to com.example.switchuser.Slot",
//            "Move Class com.facebook.samples.switchuser.SettingsFragment moved to com.example.switchuser.SettingsFragment",
//            "Move Class com.facebook.samples.switchuser.ProfileFragment moved to com.example.switchuser.ProfileFragment",
//            "Move Class com.facebook.samples.switchuser.MainActivity moved to com.example.switchuser.MainActivity",
//            "Move Class com.facebook.scrumptious.usersettings.UserSettingsFragment moved to com.example.scrumptious.usersettings.UserSettingsFragment",
//            "Move Class com.facebook.scrumptious.BaseListElement moved to com.example.scrumptious.BaseListElement",
//            "Move Class com.facebook.samples.rps.usersettings.UserSettingsFragment moved to com.example.rps.usersettings.UserSettingsFragment",
//            "Move Class com.facebook.samples.rps.RpsGameUtils moved to com.example.rps.RpsGameUtils",
//            "Move Class com.facebook.samples.rps.RpsFragment moved to com.example.rps.RpsFragment",
//            "Move Class com.facebook.samples.rps.OpenGraphConsts moved to com.example.rps.OpenGraphConsts",
//            "Move Class com.facebook.samples.rps.MainActivity moved to com.example.rps.MainActivity",
//            "Move Class com.facebook.samples.rps.ContentFragment moved to com.example.rps.ContentFragment",
//            "Move Class com.facebook.samples.rps.CommonObjects moved to com.example.rps.CommonObjects",
//            "Move Class com.facebook.iconicus.GameController moved to com.example.iconicus.GameController",
//            "Move Class com.facebook.iconicus.GameBoard moved to com.example.iconicus.GameBoard",
//            "Move Class com.facebook.samples.hellofacebook.HelloFacebookSampleActivity moved to com.example.hellofacebook.HelloFacebookSampleActivity",
//            "Move Class com.facebook.samples.hellofacebook.HelloFacebookBroadcastReceiver moved to com.example.hellofacebook.HelloFacebookBroadcastReceiver",
//            "Move Class com.facebook.scrumptious.FullListView moved to com.example.scrumptious.FullListView",
//            "Move Class com.facebook.scrumptious.MainActivity moved to com.example.scrumptious.MainActivity",
//            "Move Class com.facebook.scrumptious.PickerActivity moved to com.example.scrumptious.PickerActivity",
//            "Move Class com.facebook.scrumptious.ScrumptiousApplication moved to com.example.scrumptious.ScrumptiousApplication",
//            "Move Class com.facebook.scrumptious.SelectionFragment moved to com.example.scrumptious.SelectionFragment",
//            "Move Class com.facebook.scrumptious.SplashFragment moved to com.example.scrumptious.SplashFragment",
//            "Move Class com.facebook.scrumptious.picker.FriendPickerFragment moved to com.example.scrumptious.picker.FriendPickerFragment",
//            "Move Class com.facebook.scrumptious.picker.GraphObjectAdapter moved to com.example.scrumptious.picker.GraphObjectAdapter",
//            "Move Class com.facebook.scrumptious.picker.GraphObjectCursor moved to com.example.scrumptious.picker.GraphObjectCursor",
//            "Move Class com.facebook.scrumptious.picker.GraphObjectPagingLoader moved to com.example.scrumptious.picker.GraphObjectPagingLoader",
//            "Move Class com.facebook.scrumptious.picker.PickerFragment moved to com.example.scrumptious.picker.PickerFragment",
//            "Move Class com.facebook.scrumptious.picker.PlacePickerFragment moved to com.example.scrumptious.picker.PlacePickerFragment");
//
//          process("https://github.com/cbeust/testng.git", "b5cf7a0252c8b0465c4dbd906717f7a12e26e6f8",
//            "Move Class test.testng234.PolymorphicFailureTest moved to test.inheritance.testng234.PolymorphicFailureTest",
//            "Move Class test.testng234.ParentTest moved to test.inheritance.testng234.ParentTest",
//            "Move Class test.testng234.ChildTest moved to test.inheritance.testng234.ChildTest");
//
//          process("https://github.com/ratpack/ratpack.git", "2581441eda268c45306423dd4c515514d98a14a0",
//            "Move Class ratpack.jackson.JacksonModule moved to ratpack.jackson.guice.JacksonModule");
//
//          process("https://github.com/wordpress-mobile/WordPress-Android.git", "9dc3cbd59a20f03406f295a4a8f3b8676dbc939e",
//            "Move Class org.wordpress.android.ui.prefs.NotificationsSettingsFragment moved to org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment",
//            "Move Class org.wordpress.android.ui.prefs.NotificationsSettingsActivity moved to org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity",
//            "Move Class org.wordpress.android.ui.prefs.NotificationsPreference moved to org.wordpress.android.ui.prefs.notifications.NotificationsPreference");
//
//          process("https://github.com/jboss-developer/jboss-eap-quickstarts.git", "983e0e0e22ab5bd2c6ea44235518057ea45dcca9",
//            "Move Class org.jboss.as.quickstarts.poh5helloworld.HelloWorld moved to org.jboss.as.quickstarts.html5rest.HelloWorld",
//            "Move Class org.jboss.as.quickstarts.poh5helloworld.HelloService moved to org.jboss.as.quickstarts.html5rest.HelloService");
//
//          process("https://github.com/gradle/gradle.git", "ba1da95200d080aef6251f13ced0ca67dff282be",
//            "Move Class org.gradle.tooling.tests.TestExecutionException moved to org.gradle.tooling.test.TestExecutionException");
//
//          process("https://github.com/orientechnologies/orientdb.git", "f50f234b24e6ada29c82ce57830118508bf55d51",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.ReadWriteDiskCacheTest moved to com.orientechnologies.orient.core.storage.cache.local.ReadWriteDiskCacheTest",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.HashLRUListTest moved to com.orientechnologies.orient.core.storage.cache.local.HashLRUListTest",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.SynchronizedLRUList moved to com.orientechnologies.orient.core.storage.cache.local.SynchronizedLRUList",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.PageGroup moved to com.orientechnologies.orient.core.storage.cache.local.PageGroup",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.ConcurrentLRUList moved to com.orientechnologies.orient.core.storage.cache.local.ConcurrentLRUList",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.HashLRUList moved to com.orientechnologies.orient.core.storage.cache.local.HashLRUList",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.LRUEntry moved to com.orientechnologies.orient.core.storage.cache.local.LRUEntry",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.LRUList moved to com.orientechnologies.orient.core.storage.cache.local.LRUList",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OBlockedPageException moved to com.orientechnologies.orient.core.storage.cache.OBlockedPageException",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OCachePointer moved to com.orientechnologies.orient.core.storage.cache.OCachePointer",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OPageDataVerificationError moved to com.orientechnologies.orient.core.storage.cache.OPageDataVerificationError",
//            "Move Class com.orientechnologies.orient.core.index.hashindex.local.cache.OReadCache moved to com.orientechnologies.orient.core.storage.cache.OReadCache");
//
//          process("https://github.com/infinispan/infinispan.git", "35b6c869546a7968b6fd2f640add6eea87e03c22",
//            "Move Class org.infinispan.query.dsl.embedded.impl.EmbeddedQuery.ReverseFilterResultComparator moved to org.infinispan.query.dsl.embedded.impl.BaseEmbeddedQuery.ReverseFilterResultComparator");
//
//          process("https://github.com/google/truth.git", "1768840bf1e69892fd2a23776817f620edfed536",
//            "Move Class com.google.common.truth.ListTest.Bar moved to com.google.common.truth.IterableTest.Bar",
//            "Move Class com.google.common.truth.ListTest.Foo moved to com.google.common.truth.IterableTest.Foo");
//
//          process("https://github.com/hibernate/hibernate-orm.git", "44a02e5efc39c6953ca6dd631669d91293ab67f6",
//            "Move Class org.hibernate.test.bytecode.enhancement.entity.customer.User moved to org.hibernate.test.bytecode.enhancement.association.User",
//            "Move Class org.hibernate.test.bytecode.enhancement.entity.customer.Group moved to org.hibernate.test.bytecode.enhancement.association.Group",
//            "Move Class org.hibernate.test.bytecode.enhancement.entity.SimpleEntity moved to org.hibernate.test.bytecode.enhancement.dirty.SimpleEntity",
//            "Move Class org.hibernate.test.bytecode.enhancement.entity.MyEntity moved to org.hibernate.test.bytecode.enhancement.basic.MyEntity",
//            "Move Class org.hibernate.test.bytecode.enhancement.entity.Country moved to org.hibernate.test.bytecode.enhancement.dirty.Country",
//            "Move Class org.hibernate.test.bytecode.enhancement.entity.Address moved to org.hibernate.test.bytecode.enhancement.dirty.Address");
//
//          process("https://github.com/VoltDB/voltdb.git", "05bd8ecda456e0901ef7375b9ff7b120ae668eca",
//            "Move Class exportbenchmark.SocketExporter moved to exportbenchmark2.exporter.exportbenchmark.SocketExporter",
//            "Move Class exportbenchmark.NoOpExporter moved to exportbenchmark2.exporter.exportbenchmark.NoOpExporter",
//            "Move Class exportbenchmark.procedures.TruncateTables moved to exportbenchmark2.db.exportbenchmark.procedures.TruncateTables",
//            "Move Class exportbenchmark.procedures.SampleRecord moved to exportbenchmark2.db.exportbenchmark.procedures.SampleRecord",
//            "Move Class exportbenchmark.procedures.InsertExport5 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport5",
//            "Move Class exportbenchmark.procedures.InsertExport10 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport10",
//            "Move Class exportbenchmark.procedures.InsertExport1 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport1",
//            "Move Class exportbenchmark.procedures.InsertExport0 moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport0",
//            "Move Class exportbenchmark.procedures.InsertExport moved to exportbenchmark2.db.exportbenchmark.procedures.InsertExport",
//            "Move Class exportbenchmark.Connect2Server moved to exportbenchmark2.client.exportbenchmark.Connect2Server");
//
//          process("https://github.com/cucumber/cucumber-jvm.git", "0e815f3e1339f91960c7c64ab395de6dd8ff9eec",
//            "Move Class cucumber.runtime.java.ObjectFactory moved to cucumber.api.java.ObjectFactory");
//
//          process("https://github.com/gradle/gradle.git", "36ccb0f5c6771ff4be87a282560c090447520b66",
//            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ResolvedLocalComponentsResultGraphVisitor moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.projectresult.ResolvedLocalComponentsResultGraphVisitor",
//            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ResolvedConfigurationDependencyGraphVisitor moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.ResolvedConfigurationDependencyGraphVisitor",
//            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ResolutionResultDependencyGraphVisitor moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ResolutionResultDependencyGraphVisitor",
//            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.DependencyArtifactSet",
//            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ConfigurationArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.ConfigurationArtifactSet",
//            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.ArtifactSet",
//            "Move Class org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.AbstractArtifactSet moved to org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult.AbstractArtifactSet");
//
//          process("https://github.com/gradle/gradle.git", "f394599bf1423be0be2d5822ed7f1271d2841225",
//            "Move Class org.gradle.jvm.plugins.JarBinaryRules moved to org.gradle.jvm.internal.JarBinaryRules");
//
//          process("https://github.com/spring-projects/spring-data-jpa.git", "36d1b0717bc5836bba39985caadc2df5f2533ac4",
//            "Move Class org.springframework.data.jpa.repository.augment.JpaSoftDeleteQueryAugmentor.PropertyChangeEnsuringBeanWrapper moved to org.springframework.data.jpa.repository.augment.PropertyChangeEnsuringBeanWrapper");
//
//          process("https://github.com/gwtproject/gwt.git", "e0dda9f61b7c409944c4734edf75b108e0288f59",
//            "Move Class com.google.gwt.core.client.impl.Md5Digest moved to java.security.MessageDigest.Md5Digest");
//
//          process("https://github.com/reactor/reactor.git", "669b96c8aa4ed5134617932118de563bd4c34066",
//            "Move Class reactor.core.alloc.ReferenceCountingAllocator moved to reactor.alloc.ReferenceCountingAllocator",
//            "Move Class reactor.core.alloc.Reference moved to reactor.alloc.Reference",
//            "Move Class reactor.core.alloc.RecyclableString moved to reactor.alloc.RecyclableString",
//            "Move Class reactor.core.alloc.RecyclableNumber moved to reactor.alloc.RecyclableNumber",
//            "Move Class reactor.core.alloc.Recyclable moved to reactor.core.support.Recyclable",
//            "Move Class reactor.core.alloc.Allocator moved to reactor.alloc.Allocator",
//            "Move Class reactor.core.alloc.AbstractReference moved to reactor.alloc.AbstractReference",
//            "Move Class reactor.bus.alloc.EventAllocatorTests moved to reactor.alloc.EventAllocatorTests",
//            "Move Class reactor.bus.alloc.EventFactorySupplier moved to reactor.alloc.EventFactorySupplier",
//            "Move Class reactor.bus.alloc.EventAllocator moved to reactor.alloc.EventAllocator");
//
//          process("https://github.com/novoda/android-demos.git", "5cdabae35f0642e9fe243afe12e4c16b3378a150",
//            "Move Class com.novoda.Base64DecoderException moved to com.novoda.demo.encryption.Base64DecoderException",
//            "Move Class com.novoda.Base64 moved to com.novoda.demo.encryption.Base64");
//
//          process("https://github.com/hazelcast/hazelcast.git", "69dd55c93fc99c5f7a1e2c21f10e671e311be49e",
//            "Move Attribute public UTF_8 : Charset from class com.hazelcast.client.impl.protocol.util.UnsafeBuffer to class com.hazelcast.nio.Bits",
//            "Move Attribute public UTF_8 : Charset from class com.hazelcast.client.impl.protocol.util.SafeBuffer to class com.hazelcast.nio.Bits",
//            "Move Class com.hazelcast.client.protocol.Int2ObjectHashMapTest moved to com.hazelcast.util.collection.Int2ObjectHashMapTest",
//            "Move Class com.hazelcast.client.impl.protocol.util.Int2ObjectHashMap moved to com.hazelcast.util.collection.Int2ObjectHashMap");
//
//          process("https://github.com/square/javapoet.git", "5a37c2aa596377cb4c9b6f916614407fd0a7d3db",
//            "Extract Superclass com.squareup.javapoet.AbstractTypesTest from classes [com.squareup.javapoet.TypesTest]",
//            "Move Class com.squareup.javapoet.TypesTest.Parameterized moved to com.squareup.javapoet.AbstractTypesTest.Parameterized");
//
//          process("https://github.com/liferay/liferay-plugins.git", "78b54757c0d234db671526aed9b3288a85048e22",
//            "Move Class com.liferay.portal.util.MimeTypesImpl moved to com.liferay.tika.util.MimeTypesImpl",
//            "Move Class com.liferay.portal.metadata.XugglerRawMetadataProcessor moved to com.liferay.tika.metadata.XugglerRawMetadataProcessor",
//            "Move Class com.liferay.portal.metadata.XMPDM moved to com.liferay.tika.metadata.XMPDM",
//            "Move Class com.liferay.portal.metadata.TikaRawMetadataProcessor moved to com.liferay.tika.metadata.TikaRawMetadataProcessor",
//            "Move Class com.liferay.portal.metadata.BaseRawMetadataProcessor moved to com.liferay.tika.metadata.BaseRawMetadataProcessor");
//
//          process("https://github.com/checkstyle/checkstyle.git", "febbc986cb25ed460ea601c0a68c7d2597f89ee4",
//            "Move Class com.google.checkstyle.test.chapter5naming.rule521packageNames.PackageNameInputBad moved to com.google.checkstyle.test.chapter5naming.rule521packageNamesCamelCase.PackageNameInputBad");
//
//          process("https://github.com/cgeo/cgeo.git", "7e7e4f54801af4e49ebddb934d0c6ff33a2c2160",
//            "Move Class cgeo.geocaching.connector.TerraCachingConnector moved to cgeo.geocaching.connector.tc.TerraCachingConnector");
//
//          process("https://github.com/aws/aws-sdk-java.git", "14593c6379445f260baeb5287f618758da6d9952",
//            "Move Class com.amazonaws.service.codecommit.model.transform.UpdateRepositoryNameRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.UpdateRepositoryNameRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.UpdateRepositoryDescriptionRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.UpdateRepositoryDescriptionRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.UpdateDefaultBranchRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.UpdateDefaultBranchRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryNameIdPairJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryNameIdPairJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryNameIdPairJsonMarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryNameIdPairJsonMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryMetadataJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryMetadataJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.RepositoryMetadataJsonMarshaller moved to com.amazonaws.services.codecommit.model.transform.RepositoryMetadataJsonMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.ListRepositoriesResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.ListRepositoriesResultJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.ListRepositoriesRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.ListRepositoriesRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.ListBranchesResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.ListBranchesResultJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.ListBranchesRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.ListBranchesRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.GetRepositoryResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.GetRepositoryResultJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.GetRepositoryRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.GetRepositoryRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.GetBranchResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.GetBranchResultJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.GetBranchRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.GetBranchRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.DeleteRepositoryResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.DeleteRepositoryResultJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.DeleteRepositoryRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.DeleteRepositoryRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.CreateRepositoryResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.CreateRepositoryResultJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.CreateRepositoryRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.CreateRepositoryRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.CreateBranchRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.CreateBranchRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.BranchInfoJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.BranchInfoJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.BranchInfoJsonMarshaller moved to com.amazonaws.services.codecommit.model.transform.BranchInfoJsonMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.BatchGetRepositoriesResultJsonUnmarshaller moved to com.amazonaws.services.codecommit.model.transform.BatchGetRepositoriesResultJsonUnmarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.transform.BatchGetRepositoriesRequestMarshaller moved to com.amazonaws.services.codecommit.model.transform.BatchGetRepositoriesRequestMarshaller",
//            "Move Class com.amazonaws.service.codecommit.model.GetBranchResult moved to com.amazonaws.services.codecommit.model.GetBranchResult",
//            "Move Class com.amazonaws.service.codecommit.model.GetBranchRequest moved to com.amazonaws.services.codecommit.model.GetBranchRequest",
//            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyUnavailableException moved to com.amazonaws.services.codecommit.model.EncryptionKeyUnavailableException",
//            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyNotFoundException moved to com.amazonaws.services.codecommit.model.EncryptionKeyNotFoundException",
//            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyDisabledException moved to com.amazonaws.services.codecommit.model.EncryptionKeyDisabledException",
//            "Move Class com.amazonaws.service.codecommit.model.EncryptionKeyAccessDeniedException moved to com.amazonaws.services.codecommit.model.EncryptionKeyAccessDeniedException",
//            "Move Class com.amazonaws.service.codecommit.model.EncryptionIntegrityChecksFailedException moved to com.amazonaws.services.codecommit.model.EncryptionIntegrityChecksFailedException",
//            "Move Class com.amazonaws.service.codecommit.model.DeleteRepositoryResult moved to com.amazonaws.services.codecommit.model.DeleteRepositoryResult",
//            "Move Class com.amazonaws.service.codecommit.model.DeleteRepositoryRequest moved to com.amazonaws.services.codecommit.model.DeleteRepositoryRequest",
//            "Move Class com.amazonaws.service.codecommit.model.CreateRepositoryResult moved to com.amazonaws.services.codecommit.model.CreateRepositoryResult",
//            "Move Class com.amazonaws.service.codecommit.model.CreateRepositoryRequest moved to com.amazonaws.services.codecommit.model.CreateRepositoryRequest",
//            "Move Class com.amazonaws.service.codecommit.model.CreateBranchRequest moved to com.amazonaws.services.codecommit.model.CreateBranchRequest",
//            "Move Class com.amazonaws.service.codecommit.AWSCodeCommit moved to com.amazonaws.services.codecommit.AWSCodeCommit",
//            "Move Class com.amazonaws.service.codecommit.AWSCodeCommitAsync moved to com.amazonaws.services.codecommit.AWSCodeCommitAsync",
//            "Move Class com.amazonaws.service.codecommit.AWSCodeCommitAsyncClient moved to com.amazonaws.services.codecommit.AWSCodeCommitAsyncClient",
//            "Move Class com.amazonaws.service.codecommit.AWSCodeCommitClient moved to com.amazonaws.services.codecommit.AWSCodeCommitClient",
//            "Move Class com.amazonaws.service.codecommit.model.BatchGetRepositoriesRequest moved to com.amazonaws.services.codecommit.model.BatchGetRepositoriesRequest",
//            "Move Class com.amazonaws.service.codecommit.model.BatchGetRepositoriesResult moved to com.amazonaws.services.codecommit.model.BatchGetRepositoriesResult",
//            "Move Class com.amazonaws.service.codecommit.model.BranchDoesNotExistException moved to com.amazonaws.services.codecommit.model.BranchDoesNotExistException",
//            "Move Class com.amazonaws.service.codecommit.model.BranchInfo moved to com.amazonaws.services.codecommit.model.BranchInfo",
//            "Move Class com.amazonaws.service.codecommit.model.BranchNameExistsException moved to com.amazonaws.services.codecommit.model.BranchNameExistsException",
//            "Move Class com.amazonaws.service.codecommit.model.BranchNameRequiredException moved to com.amazonaws.services.codecommit.model.BranchNameRequiredException",
//            "Move Class com.amazonaws.service.codecommit.model.CommitDoesNotExistException moved to com.amazonaws.services.codecommit.model.CommitDoesNotExistException",
//            "Move Class com.amazonaws.service.codecommit.model.CommitIdRequiredException moved to com.amazonaws.services.codecommit.model.CommitIdRequiredException",
//            "Move Class com.amazonaws.service.codecommit.model.GetRepositoryRequest moved to com.amazonaws.services.codecommit.model.GetRepositoryRequest",
//            "Move Class com.amazonaws.service.codecommit.model.GetRepositoryResult moved to com.amazonaws.services.codecommit.model.GetRepositoryResult",
//            "Move Class com.amazonaws.service.codecommit.model.InvalidBranchNameException moved to com.amazonaws.services.codecommit.model.InvalidBranchNameException",
//            "Move Class com.amazonaws.service.codecommit.model.InvalidCommitIdException moved to com.amazonaws.services.codecommit.model.InvalidCommitIdException",
//            "Move Class com.amazonaws.service.codecommit.model.InvalidContinuationTokenException moved to com.amazonaws.services.codecommit.model.InvalidContinuationTokenException",
//            "Move Class com.amazonaws.service.codecommit.model.InvalidOrderException moved to com.amazonaws.services.codecommit.model.InvalidOrderException",
//            "Move Class com.amazonaws.service.codecommit.model.InvalidRepositoryDescriptionException moved to com.amazonaws.services.codecommit.model.InvalidRepositoryDescriptionException",
//            "Move Class com.amazonaws.service.codecommit.model.InvalidRepositoryNameException moved to com.amazonaws.services.codecommit.model.InvalidRepositoryNameException",
//            "Move Class com.amazonaws.service.codecommit.model.InvalidSortByException moved to com.amazonaws.services.codecommit.model.InvalidSortByException",
//            "Move Class com.amazonaws.service.codecommit.model.ListBranchesRequest moved to com.amazonaws.services.codecommit.model.ListBranchesRequest",
//            "Move Class com.amazonaws.service.codecommit.model.ListBranchesResult moved to com.amazonaws.services.codecommit.model.ListBranchesResult",
//            "Move Class com.amazonaws.service.codecommit.model.ListRepositoriesRequest moved to com.amazonaws.services.codecommit.model.ListRepositoriesRequest",
//            "Move Class com.amazonaws.service.codecommit.model.ListRepositoriesResult moved to com.amazonaws.services.codecommit.model.ListRepositoriesResult",
//            "Move Class com.amazonaws.service.codecommit.model.MaximumRepositoryNamesExceededException moved to com.amazonaws.services.codecommit.model.MaximumRepositoryNamesExceededException",
//            "Move Class com.amazonaws.service.codecommit.model.RepositoryDoesNotExistException moved to com.amazonaws.services.codecommit.model.RepositoryDoesNotExistException",
//            "Move Class com.amazonaws.service.codecommit.model.RepositoryLimitExceededException moved to com.amazonaws.services.codecommit.model.RepositoryLimitExceededException",
//            "Move Class com.amazonaws.service.codecommit.model.RepositoryMetadata moved to com.amazonaws.services.codecommit.model.RepositoryMetadata",
//            "Move Class com.amazonaws.service.codecommit.model.RepositoryNameExistsException moved to com.amazonaws.services.codecommit.model.RepositoryNameExistsException",
//            "Move Class com.amazonaws.service.codecommit.model.RepositoryNameIdPair moved to com.amazonaws.services.codecommit.model.RepositoryNameIdPair",
//            "Move Class com.amazonaws.service.codecommit.model.RepositoryNameRequiredException moved to com.amazonaws.services.codecommit.model.RepositoryNameRequiredException",
//            "Move Class com.amazonaws.service.codecommit.model.RepositoryNamesRequiredException moved to com.amazonaws.services.codecommit.model.RepositoryNamesRequiredException",
//            "Move Class com.amazonaws.service.codecommit.model.UpdateDefaultBranchRequest moved to com.amazonaws.services.codecommit.model.UpdateDefaultBranchRequest",
//            "Move Class com.amazonaws.service.codecommit.model.UpdateRepositoryDescriptionRequest moved to com.amazonaws.services.codecommit.model.UpdateRepositoryDescriptionRequest",
//            "Move Class com.amazonaws.service.codecommit.model.UpdateRepositoryNameRequest moved to com.amazonaws.services.codecommit.model.UpdateRepositoryNameRequest");
//
//          process("https://github.com/plutext/docx4j.git", "e29924b33ec0c0298ba4fc3f7a8c218c8e6cfa0c",
//            "Move Class org.apache.poi.util.TempFileCreationStrategy moved to org.docx4j.org.apache.poi.util.TempFileCreationStrategy",
//            "Move Class org.apache.poi.util.TempFile moved to org.docx4j.org.apache.poi.util.TempFile",
//            "Move Class org.apache.poi.util.StringUtil moved to org.docx4j.org.apache.poi.util.StringUtil",
//            "Move Class org.apache.poi.util.ShortField moved to org.docx4j.org.apache.poi.util.ShortField",
//            "Move Class org.apache.poi.util.SAXHelper moved to org.docx4j.org.apache.poi.util.SAXHelper",
//            "Move Class org.apache.poi.util.RecordFormatException moved to org.docx4j.org.apache.poi.util.RecordFormatException",
//            "Move Class org.apache.poi.util.PngUtils moved to org.docx4j.org.apache.poi.util.PngUtils",
//            "Move Class org.apache.poi.util.LongField moved to org.docx4j.org.apache.poi.util.LongField",
//            "Move Class org.apache.poi.util.LittleEndianOutputStream moved to org.docx4j.org.apache.poi.util.LittleEndianOutputStream",
//            "Move Class org.apache.poi.util.LittleEndianOutput moved to org.docx4j.org.apache.poi.util.LittleEndianOutput",
//            "Move Class org.apache.poi.util.LittleEndianInputStream moved to org.docx4j.org.apache.poi.util.LittleEndianInputStream",
//            "Move Class org.apache.poi.util.LittleEndianInput moved to org.docx4j.org.apache.poi.util.LittleEndianInput",
//            "Move Class org.apache.poi.util.LittleEndianConsts moved to org.docx4j.org.apache.poi.util.LittleEndianConsts",
//            "Move Class org.apache.poi.util.LittleEndianByteArrayOutputStream moved to org.docx4j.org.apache.poi.util.LittleEndianByteArrayOutputStream",
//            "Move Class org.apache.poi.util.LittleEndian moved to org.docx4j.org.apache.poi.util.LittleEndian",
//            "Move Class org.apache.poi.util.IntegerField moved to org.docx4j.org.apache.poi.util.IntegerField",
//            "Move Class org.apache.poi.util.IntList moved to org.docx4j.org.apache.poi.util.IntList",
//            "Move Class org.apache.poi.util.IOUtils moved to org.docx4j.org.apache.poi.util.IOUtils",
//            "Move Class org.apache.poi.util.HexRead moved to org.docx4j.org.apache.poi.util.HexRead",
//            "Move Class org.apache.poi.util.HexDump moved to org.docx4j.org.apache.poi.util.HexDump",
//            "Move Class org.apache.poi.util.FixedField moved to org.docx4j.org.apache.poi.util.FixedField",
//            "Move Class org.apache.poi.util.DocumentHelper moved to org.docx4j.org.apache.poi.util.DocumentHelper",
//            "Move Class org.apache.poi.util.DelayableLittleEndianOutput moved to org.docx4j.org.apache.poi.util.DelayableLittleEndianOutput",
//            "Move Class org.apache.poi.util.CodePageUtil moved to org.docx4j.org.apache.poi.util.CodePageUtil",
//            "Move Class org.apache.poi.util.CloseIgnoringInputStream moved to org.docx4j.org.apache.poi.util.CloseIgnoringInputStream",
//            "Move Class org.apache.poi.util.ByteField moved to org.docx4j.org.apache.poi.util.ByteField",
//            "Move Class org.apache.poi.util.BoundedInputStream moved to org.docx4j.org.apache.poi.util.BoundedInputStream",
//            "Move Class org.apache.poi.util.BitFieldFactory moved to org.docx4j.org.apache.poi.util.BitFieldFactory",
//            "Move Class org.apache.poi.util.BitField moved to org.docx4j.org.apache.poi.util.BitField",
//            "Move Class org.apache.poi.poifs.storage.SmallDocumentBlockList moved to org.docx4j.org.apache.poi.poifs.storage.SmallDocumentBlockList",
//            "Move Class org.apache.poi.poifs.storage.SmallBlockTableWriter moved to org.docx4j.org.apache.poi.poifs.storage.SmallBlockTableWriter",
//            "Move Class org.apache.poi.poifs.storage.RawDataBlockList moved to org.docx4j.org.apache.poi.poifs.storage.RawDataBlockList",
//            "Move Class org.apache.poi.poifs.storage.RawDataBlock moved to org.docx4j.org.apache.poi.poifs.storage.RawDataBlock",
//            "Move Class org.apache.poi.poifs.storage.PropertyBlock moved to org.docx4j.org.apache.poi.poifs.storage.PropertyBlock",
//            "Move Class org.apache.poi.poifs.storage.ListManagedBlock moved to org.docx4j.org.apache.poi.poifs.storage.ListManagedBlock",
//            "Move Class org.apache.poi.poifs.storage.HeaderBlockWriter moved to org.docx4j.org.apache.poi.poifs.storage.HeaderBlockWriter",
//            "Move Class org.apache.poi.poifs.storage.HeaderBlockConstants moved to org.docx4j.org.apache.poi.poifs.storage.HeaderBlockConstants",
//            "Move Class org.apache.poi.poifs.storage.HeaderBlock moved to org.docx4j.org.apache.poi.poifs.storage.HeaderBlock",
//            "Move Class org.apache.poi.poifs.storage.DocumentBlock moved to org.docx4j.org.apache.poi.poifs.storage.DocumentBlock",
//            "Move Class org.apache.poi.poifs.storage.DataInputBlock moved to org.docx4j.org.apache.poi.poifs.storage.DataInputBlock",
//            "Move Class org.apache.poi.poifs.storage.BlockWritable moved to org.docx4j.org.apache.poi.poifs.storage.BlockWritable",
//            "Move Class org.apache.poi.poifs.storage.BlockListImpl moved to org.docx4j.org.apache.poi.poifs.storage.BlockListImpl",
//            "Move Class org.apache.poi.poifs.storage.BlockList moved to org.docx4j.org.apache.poi.poifs.storage.BlockList",
//            "Move Class org.apache.poi.poifs.storage.BlockAllocationTableWriter moved to org.docx4j.org.apache.poi.poifs.storage.BlockAllocationTableWriter",
//            "Move Class org.apache.poi.poifs.storage.BlockAllocationTableReader moved to org.docx4j.org.apache.poi.poifs.storage.BlockAllocationTableReader",
//            "Move Class org.apache.poi.poifs.storage.BigBlock moved to org.docx4j.org.apache.poi.poifs.storage.BigBlock",
//            "Move Class org.apache.poi.poifs.storage.BATBlock.BATBlockAndIndex moved to org.docx4j.org.apache.poi.poifs.storage.BATBlock.BATBlockAndIndex",
//            "Move Class org.apache.poi.poifs.property.RootProperty moved to org.docx4j.org.apache.poi.poifs.property.RootProperty",
//            "Move Class org.apache.poi.poifs.property.PropertyTableBase moved to org.docx4j.org.apache.poi.poifs.property.PropertyTableBase",
//            "Move Class org.apache.poi.poifs.property.PropertyTable moved to org.docx4j.org.apache.poi.poifs.property.PropertyTable",
//            "Move Class org.apache.poi.poifs.property.PropertyFactory moved to org.docx4j.org.apache.poi.poifs.property.PropertyFactory",
//            "Move Class org.apache.poi.poifs.property.PropertyConstants moved to org.docx4j.org.apache.poi.poifs.property.PropertyConstants",
//            "Move Class org.apache.poi.poifs.property.Property moved to org.docx4j.org.apache.poi.poifs.property.Property",
//            "Move Class org.apache.poi.poifs.property.Parent moved to org.docx4j.org.apache.poi.poifs.property.Parent",
//            "Move Class org.apache.poi.poifs.property.NPropertyTable moved to org.docx4j.org.apache.poi.poifs.property.NPropertyTable",
//            "Move Class org.apache.poi.poifs.property.DocumentProperty moved to org.docx4j.org.apache.poi.poifs.property.DocumentProperty",
//            "Move Class org.apache.poi.poifs.property.DirectoryProperty moved to org.docx4j.org.apache.poi.poifs.property.DirectoryProperty",
//            "Move Class org.apache.poi.poifs.property.Child moved to org.docx4j.org.apache.poi.poifs.property.Child",
//            "Move Class org.apache.poi.poifs.nio.FileBackedDataSource moved to org.docx4j.org.apache.poi.poifs.nio.FileBackedDataSource",
//            "Move Class org.apache.poi.poifs.nio.DataSource moved to org.docx4j.org.apache.poi.poifs.nio.DataSource",
//            "Move Class org.apache.poi.poifs.nio.ByteArrayBackedDataSource moved to org.docx4j.org.apache.poi.poifs.nio.ByteArrayBackedDataSource",
//            "Move Class org.apache.poi.poifs.filesystem.POIFSWriterListener moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSWriterListener",
//            "Move Class org.apache.poi.poifs.filesystem.POIFSWriterEvent moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSWriterEvent",
//            "Move Class org.apache.poi.poifs.filesystem.POIFSFileSystem moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSFileSystem",
//            "Move Class org.apache.poi.poifs.filesystem.POIFSDocumentPath moved to org.docx4j.org.apache.poi.poifs.filesystem.POIFSDocumentPath",
//            "Move Class org.apache.poi.poifs.filesystem.Ole10NativeException moved to org.docx4j.org.apache.poi.poifs.filesystem.Ole10NativeException",
//            "Move Class org.apache.poi.poifs.filesystem.Ole10Native moved to org.docx4j.org.apache.poi.poifs.filesystem.Ole10Native",
//            "Move Class org.apache.poi.poifs.filesystem.OfficeXmlFileException moved to org.docx4j.org.apache.poi.poifs.filesystem.OfficeXmlFileException",
//            "Move Class org.apache.poi.poifs.filesystem.OPOIFSFileSystem moved to org.docx4j.org.apache.poi.poifs.filesystem.OPOIFSFileSystem",
//            "Move Class org.apache.poi.poifs.filesystem.OPOIFSDocument moved to org.docx4j.org.apache.poi.poifs.filesystem.OPOIFSDocument",
//            "Move Class org.apache.poi.poifs.filesystem.ODocumentInputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.ODocumentInputStream",
//            "Move Class org.apache.poi.poifs.filesystem.NotOLE2FileException moved to org.docx4j.org.apache.poi.poifs.filesystem.NotOLE2FileException",
//            "Move Class org.apache.poi.poifs.filesystem.NPOIFSStream moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSStream",
//            "Move Class org.apache.poi.poifs.filesystem.NPOIFSMiniStore moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSMiniStore",
//            "Move Class org.apache.poi.poifs.filesystem.NPOIFSFileSystem moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSFileSystem",
//            "Move Class org.apache.poi.poifs.filesystem.NPOIFSDocument moved to org.docx4j.org.apache.poi.poifs.filesystem.NPOIFSDocument",
//            "Move Class org.apache.poi.poifs.filesystem.NDocumentOutputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.NDocumentOutputStream",
//            "Move Class org.apache.poi.poifs.filesystem.NDocumentInputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.NDocumentInputStream",
//            "Move Class org.apache.poi.poifs.filesystem.FilteringDirectoryNode moved to org.docx4j.org.apache.poi.poifs.filesystem.FilteringDirectoryNode",
//            "Move Class org.apache.poi.poifs.filesystem.EntryUtils moved to org.docx4j.org.apache.poi.poifs.filesystem.EntryUtils",
//            "Move Class org.apache.poi.poifs.filesystem.EntryNode moved to org.docx4j.org.apache.poi.poifs.filesystem.EntryNode",
//            "Move Class org.apache.poi.poifs.filesystem.Entry moved to org.docx4j.org.apache.poi.poifs.filesystem.Entry",
//            "Move Class org.apache.poi.poifs.filesystem.DocumentOutputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentOutputStream",
//            "Move Class org.apache.poi.poifs.filesystem.DocumentNode moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentNode",
//            "Move Class org.apache.poi.poifs.filesystem.DocumentInputStream moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentInputStream",
//            "Move Class org.apache.poi.poifs.filesystem.DocumentEntry moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentEntry",
//            "Move Class org.apache.poi.poifs.filesystem.DocumentDescriptor moved to org.docx4j.org.apache.poi.poifs.filesystem.DocumentDescriptor",
//            "Move Class org.apache.poi.poifs.filesystem.DirectoryNode moved to org.docx4j.org.apache.poi.poifs.filesystem.DirectoryNode",
//            "Move Class org.apache.poi.hpsf.VersionedStream moved to org.docx4j.org.apache.poi.hpsf.VersionedStream",
//            "Move Class org.apache.poi.hpsf.Vector moved to org.docx4j.org.apache.poi.hpsf.Vector",
//            "Move Class org.apache.poi.hpsf.VariantTypeException moved to org.docx4j.org.apache.poi.hpsf.VariantTypeException",
//            "Move Class org.apache.poi.hpsf.VariantSupport moved to org.docx4j.org.apache.poi.hpsf.VariantSupport",
//            "Move Class org.apache.poi.hpsf.VariantBool moved to org.docx4j.org.apache.poi.hpsf.VariantBool",
//            "Move Class org.apache.poi.hpsf.Variant moved to org.docx4j.org.apache.poi.hpsf.Variant",
//            "Move Class org.apache.poi.hpsf.Util moved to org.docx4j.org.apache.poi.hpsf.Util",
//            "Move Class org.apache.poi.hpsf.UnsupportedVariantTypeException moved to org.docx4j.org.apache.poi.hpsf.UnsupportedVariantTypeException",
//            "Move Class org.apache.poi.hpsf.UnicodeString moved to org.docx4j.org.apache.poi.hpsf.UnicodeString",
//            "Move Class org.apache.poi.hpsf.UnexpectedPropertySetTypeException moved to org.docx4j.org.apache.poi.hpsf.UnexpectedPropertySetTypeException",
//            "Move Class org.apache.poi.hpsf.TypedPropertyValue moved to org.docx4j.org.apache.poi.hpsf.TypedPropertyValue",
//            "Move Class org.apache.poi.hpsf.TypeWriter moved to org.docx4j.org.apache.poi.hpsf.TypeWriter",
//            "Move Class org.apache.poi.hpsf.SummaryInformation moved to org.docx4j.org.apache.poi.hpsf.SummaryInformation",
//            "Move Class org.apache.poi.hpsf.SpecialPropertySet moved to org.docx4j.org.apache.poi.hpsf.SpecialPropertySet",
//            "Move Class org.apache.poi.hpsf.Section moved to org.docx4j.org.apache.poi.hpsf.Section",
//            "Move Class org.apache.poi.hpsf.ReadingNotSupportedException moved to org.docx4j.org.apache.poi.hpsf.ReadingNotSupportedException",
//            "Move Class org.apache.poi.hpsf.PropertySetFactory moved to org.docx4j.org.apache.poi.hpsf.PropertySetFactory",
//            "Move Class org.apache.poi.hpsf.PropertySet moved to org.docx4j.org.apache.poi.hpsf.PropertySet",
//            "Move Class org.apache.poi.hpsf.Property moved to org.docx4j.org.apache.poi.hpsf.Property",
//            "Move Class org.apache.poi.hpsf.NoSingleSectionException moved to org.docx4j.org.apache.poi.hpsf.NoSingleSectionException",
//            "Move Class org.apache.poi.hpsf.NoPropertySetStreamException moved to org.docx4j.org.apache.poi.hpsf.NoPropertySetStreamException",
//            "Move Class org.apache.poi.hpsf.NoFormatIDException moved to org.docx4j.org.apache.poi.hpsf.NoFormatIDException",
//            "Move Class org.apache.poi.hpsf.MutableSection moved to org.docx4j.org.apache.poi.hpsf.MutableSection",
//            "Move Class org.apache.poi.hpsf.MutablePropertySet moved to org.docx4j.org.apache.poi.hpsf.MutablePropertySet",
//            "Move Class org.apache.poi.hpsf.CustomProperty moved to org.docx4j.org.apache.poi.hpsf.CustomProperty",
//            "Move Class org.apache.poi.hpsf.CustomProperties moved to org.docx4j.org.apache.poi.hpsf.CustomProperties",
//            "Move Class org.apache.poi.hpsf.Currency moved to org.docx4j.org.apache.poi.hpsf.Currency",
//            "Move Class org.apache.poi.hpsf.CodePageString moved to org.docx4j.org.apache.poi.hpsf.CodePageString",
//            "Move Class org.apache.poi.hpsf.ClipboardData moved to org.docx4j.org.apache.poi.hpsf.ClipboardData",
//            "Move Class org.apache.poi.hpsf.ClassID moved to org.docx4j.org.apache.poi.hpsf.ClassID",
//            "Move Class org.apache.poi.hpsf.Blob moved to org.docx4j.org.apache.poi.hpsf.Blob",
//            "Move Class org.apache.poi.hpsf.Array moved to org.docx4j.org.apache.poi.hpsf.Array",
//            "Move Class org.apache.poi.UnsupportedFileFormatException moved to org.docx4j.org.apache.poi.UnsupportedFileFormatException",
//            "Move Class org.apache.poi.OldFileFormatException moved to org.docx4j.org.apache.poi.OldFileFormatException",
//            "Move Class org.apache.poi.EncryptedDocumentException moved to org.docx4j.org.apache.poi.EncryptedDocumentException",
//            "Move Class org.apache.poi.EmptyFileException moved to org.docx4j.org.apache.poi.EmptyFileException",
//            "Move Class org.apache.poi.hpsf.Date moved to org.docx4j.org.apache.poi.hpsf.Date",
//            "Move Class org.apache.poi.hpsf.Decimal moved to org.docx4j.org.apache.poi.hpsf.Decimal",
//            "Move Class org.apache.poi.hpsf.DocumentSummaryInformation moved to org.docx4j.org.apache.poi.hpsf.DocumentSummaryInformation",
//            "Move Class org.apache.poi.hpsf.Filetime moved to org.docx4j.org.apache.poi.hpsf.Filetime",
//            "Move Class org.apache.poi.hpsf.GUID moved to org.docx4j.org.apache.poi.hpsf.GUID",
//            "Move Class org.apache.poi.hpsf.HPSFException moved to org.docx4j.org.apache.poi.hpsf.HPSFException",
//            "Move Class org.apache.poi.hpsf.HPSFRuntimeException moved to org.docx4j.org.apache.poi.hpsf.HPSFRuntimeException",
//            "Move Class org.apache.poi.hpsf.IllegalPropertySetDataException moved to org.docx4j.org.apache.poi.hpsf.IllegalPropertySetDataException",
//            "Move Class org.apache.poi.hpsf.IndirectPropertyName moved to org.docx4j.org.apache.poi.hpsf.IndirectPropertyName",
//            "Move Class org.apache.poi.hpsf.MarkUnsupportedException moved to org.docx4j.org.apache.poi.hpsf.MarkUnsupportedException",
//            "Move Class org.apache.poi.hpsf.MissingSectionException moved to org.docx4j.org.apache.poi.hpsf.MissingSectionException",
//            "Move Class org.apache.poi.hpsf.MutableProperty moved to org.docx4j.org.apache.poi.hpsf.MutableProperty",
//            "Move Class org.apache.poi.hpsf.WritingNotSupportedException moved to org.docx4j.org.apache.poi.hpsf.WritingNotSupportedException",
//            "Move Class org.apache.poi.hpsf.wellknown.PropertyIDMap moved to org.docx4j.org.apache.poi.hpsf.wellknown.PropertyIDMap",
//            "Move Class org.apache.poi.hpsf.wellknown.SectionIDMap moved to org.docx4j.org.apache.poi.hpsf.wellknown.SectionIDMap",
//            "Move Class org.apache.poi.hssf.OldExcelFormatException moved to org.docx4j.org.apache.poi.hssf.OldExcelFormatException",
//            "Move Class org.apache.poi.poifs.common.POIFSBigBlockSize moved to org.docx4j.org.apache.poi.poifs.common.POIFSBigBlockSize",
//            "Move Class org.apache.poi.poifs.common.POIFSConstants moved to org.docx4j.org.apache.poi.poifs.common.POIFSConstants",
//            "Move Class org.apache.poi.poifs.crypt.ChunkedCipherInputStream moved to org.docx4j.org.apache.poi.poifs.crypt.ChunkedCipherInputStream",
//            "Move Class org.apache.poi.poifs.crypt.ChunkedCipherOutputStream moved to org.docx4j.org.apache.poi.poifs.crypt.ChunkedCipherOutputStream",
//            "Move Class org.apache.poi.poifs.crypt.CryptoFunctions moved to org.docx4j.org.apache.poi.poifs.crypt.CryptoFunctions",
//            "Move Class org.apache.poi.poifs.crypt.DataSpaceMapUtils moved to org.docx4j.org.apache.poi.poifs.crypt.DataSpaceMapUtils",
//            "Move Class org.apache.poi.poifs.crypt.Decryptor moved to org.docx4j.org.apache.poi.poifs.crypt.Decryptor",
//            "Move Class org.apache.poi.poifs.crypt.EncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionHeader",
//            "Move Class org.apache.poi.poifs.crypt.EncryptionInfo moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionInfo",
//            "Move Class org.apache.poi.poifs.crypt.EncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionInfoBuilder",
//            "Move Class org.apache.poi.poifs.crypt.EncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.EncryptionVerifier",
//            "Move Class org.apache.poi.poifs.crypt.Encryptor moved to org.docx4j.org.apache.poi.poifs.crypt.Encryptor",
//            "Move Class org.apache.poi.poifs.crypt.agile.AgileDecryptor moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileDecryptor",
//            "Move Class org.apache.poi.poifs.crypt.agile.AgileEncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileEncryptionHeader",
//            "Move Class org.apache.poi.poifs.crypt.agile.AgileEncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileEncryptionVerifier",
//            "Move Class org.apache.poi.poifs.crypt.agile.AgileEncryptor moved to org.docx4j.org.apache.poi.poifs.crypt.agile.AgileEncryptor",
//            "Move Class org.apache.poi.poifs.crypt.agile.EncryptionDocument moved to org.docx4j.org.apache.poi.poifs.crypt.agile.EncryptionDocument",
//            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Decryptor moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Decryptor",
//            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionHeader",
//            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionInfoBuilder",
//            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4EncryptionVerifier",
//            "Move Class org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Encryptor moved to org.docx4j.org.apache.poi.poifs.crypt.binaryrc4.BinaryRC4Encryptor",
//            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIDecryptor moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIDecryptor",
//            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionHeader",
//            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionInfoBuilder",
//            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptionVerifier",
//            "Move Class org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptor moved to org.docx4j.org.apache.poi.poifs.crypt.cryptoapi.CryptoAPIEncryptor",
//            "Move Class org.apache.poi.poifs.crypt.standard.EncryptionRecord moved to org.docx4j.org.apache.poi.poifs.crypt.standard.EncryptionRecord",
//            "Move Class org.apache.poi.poifs.crypt.standard.StandardDecryptor moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardDecryptor",
//            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptionHeader moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptionHeader",
//            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptionInfoBuilder moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptionInfoBuilder",
//            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptionVerifier moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptionVerifier",
//            "Move Class org.apache.poi.poifs.crypt.standard.StandardEncryptor moved to org.docx4j.org.apache.poi.poifs.crypt.standard.StandardEncryptor",
//            "Move Class org.apache.poi.poifs.dev.POIFSLister moved to org.docx4j.org.apache.poi.poifs.dev.POIFSLister",
//            "Move Class org.apache.poi.poifs.dev.POIFSViewEngine moved to org.docx4j.org.apache.poi.poifs.dev.POIFSViewEngine",
//            "Move Class org.apache.poi.poifs.dev.POIFSViewable moved to org.docx4j.org.apache.poi.poifs.dev.POIFSViewable",
//            "Move Class org.apache.poi.poifs.dev.POIFSViewer moved to org.docx4j.org.apache.poi.poifs.dev.POIFSViewer",
//            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReader moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReader",
//            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent",
//            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReaderListener moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReaderListener",
//            "Move Class org.apache.poi.poifs.eventfilesystem.POIFSReaderRegistry moved to org.docx4j.org.apache.poi.poifs.eventfilesystem.POIFSReaderRegistry",
//            "Move Class org.apache.poi.poifs.filesystem.BATManaged moved to org.docx4j.org.apache.poi.poifs.filesystem.BATManaged",
//            "Move Class org.apache.poi.poifs.filesystem.BlockStore moved to org.docx4j.org.apache.poi.poifs.filesystem.BlockStore",
//            "Move Class org.apache.poi.poifs.filesystem.DirectoryEntry moved to org.docx4j.org.apache.poi.poifs.filesystem.DirectoryEntry",
//            "Extract Method public save(outStream OutputStream, flags int, password String) : void extracted from public save(outStream OutputStream, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage",
//            "Extract Method public save(outFile File, flags int, password String) : void extracted from public save(outStream OutputStream, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage",
//            "Extract Method public save(outFile File, flags int, password String) : void extracted from public save(outFile File, flags int) : void in class org.docx4j.openpackaging.packages.OpcPackage");
//
//          process("https://github.com/springfox/springfox.git", "2307ff3a4ca367aaf64088b7b1e1bbf212c9bf3a",
//            "Move Class springfox.documentation.spring.web.RelativePathProvider moved to springfox.documentation.spring.web.paths.RelativePathProvider",
//            "Move Class springfox.documentation.spring.web.Paths moved to springfox.documentation.spring.web.paths.Paths",
//            "Move Class springfox.documentation.spring.web.AbstractPathProvider moved to springfox.documentation.spring.web.paths.AbstractPathProvider");
//
//          process("https://github.com/spring-projects/spring-boot.git", "84937551787072a4befac29fb48436b3187ac4c6",
//            "Move Class org.springframework.boot.cli.compiler.grape.SettingsXmlRepositorySystemSessionAutoConfiguration.SpringBootSecDispatcher moved to org.springframework.boot.cli.compiler.MavenSettingsReader.SpringBootSecDispatcher");
//
//          process("https://github.com/Activiti/Activiti.git", "ca7d0c3b33a0863bed04c77932b9ef6b1317f34e",
//            "Move Class org.activiti.engine.impl.persistence.entity.UserEntityTest moved to org.activiti.engine.test.api.identity.UserEntityTest");
//
//          process("https://github.com/infinispan/infinispan.git", "8f446b6ddf540e1b1fefca34dd10f45ba7256095",
//            "Move Class org.jboss.as.clustering.jgroups.ProtocolConfiguration moved to org.infinispan.server.jgroups.spi.ProtocolConfiguration",
//            "Move Class org.jboss.as.clustering.jgroups.SaslClientCallbackHandler moved to org.infinispan.server.jgroups.security.SaslClientCallbackHandler",
//            "Move Class org.jboss.as.clustering.jgroups.RealmAuthorizationCallbackHandler moved to org.infinispan.server.jgroups.security.RealmAuthorizationCallbackHandler",
//            "Move Class org.jboss.as.clustering.jgroups.TopologyAddressGenerator moved to org.infinispan.server.jgroups.TopologyAddressGenerator",
//            "Move Class org.jboss.as.clustering.jgroups.ProtocolDefaults moved to org.infinispan.server.jgroups.ProtocolDefaults",
//            "Move Class org.jboss.as.clustering.jgroups.ManagedSocketFactory moved to org.infinispan.server.jgroups.ManagedSocketFactory",
//            "Move Class org.jboss.as.clustering.jgroups.LogFactory moved to org.infinispan.server.jgroups.LogFactory",
//            "Move Class org.jboss.as.clustering.jgroups.ChannelFactory moved to org.infinispan.server.jgroups.ChannelFactory",
//            "Move Class org.jboss.as.clustering.jgroups.subsystem.ClusteringSubsystemTest moved to org.infinispan.server.commons.subsystem.ClusteringSubsystemTest",
//            "Move Class org.jboss.as.clustering.jgroups.ServiceContainerHelper moved to org.infinispan.server.commons.msc.ServiceContainerHelper",
//            "Move Class org.jboss.as.clustering.concurrent.ManagedScheduledExecutorService moved to org.infinispan.server.commons.concurrent.ManagedScheduledExecutorService",
//            "Move Class org.jboss.as.clustering.concurrent.ManagedExecutorService moved to org.infinispan.server.commons.concurrent.ManagedExecutorService",
//            "Move Class org.jboss.as.clustering.jgroups.ProtocolStackConfiguration moved to org.infinispan.server.jgroups.spi.ProtocolStackConfiguration",
//            "Move Class org.jboss.as.clustering.jgroups.RelayConfiguration moved to org.infinispan.server.jgroups.spi.RelayConfiguration",
//            "Move Class org.jboss.as.clustering.jgroups.RemoteSiteConfiguration moved to org.infinispan.server.jgroups.spi.RemoteSiteConfiguration",
//            "Move Class org.jboss.as.clustering.jgroups.SaslConfiguration moved to org.infinispan.server.jgroups.spi.SaslConfiguration",
//            "Move Class org.jboss.as.clustering.jgroups.TransportConfiguration moved to org.infinispan.server.jgroups.spi.TransportConfiguration",
//            "Move Class org.jboss.as.clustering.jgroups.subsystem.ExportNativeConfiguration moved to org.infinispan.server.jgroups.subsystem.ExportNativeConfiguration",
//            "Move Class org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension moved to org.infinispan.server.jgroups.subsystem.JGroupsExtension",
//            "Move Class org.jboss.as.clustering.jgroups.subsystem.ModelKeys moved to org.infinispan.server.jgroups.subsystem.ModelKeys",
//            "Move Class org.jboss.as.clustering.jgroups.subsystem.RelayResourceDefinition moved to org.infinispan.server.jgroups.subsystem.RelayResourceDefinition",
//            "Move Class org.jboss.as.clustering.jgroups.subsystem.SaslResourceDefinition moved to org.infinispan.server.jgroups.subsystem.SaslResourceDefinition",
//            "Move Class org.jboss.as.clustering.jgroups.ManagedSocketFactoryTest moved to org.infinispan.server.jgroups.ManagedSocketFactoryTest",
//            "Move Class org.jboss.as.clustering.jgroups.subsystem.OperationSequencesTestCase moved to org.infinispan.server.jgroups.subsystem.OperationSequencesTestCase");
//
//          process("https://github.com/spring-projects/spring-security.git", "08b1b56e2cd5ad72126f4bbeb15a47d9b104dfff",
//            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextServletOutputStream moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextServletOutputStream",
//            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextPrintWriter moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextPrintWriter");
//
//          process("https://github.com/spring-projects/spring-security.git", "fcc9a34356817d93c24b5ccf3107ec234a28b136",
//            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextServletOutputStream moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextServletOutputStream",
//            "Move Class org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper.SaveContextPrintWriter moved to org.springframework.security.web.context.OnCommittedResponseWrapper.SaveContextPrintWriter");
//
//          process("https://github.com/antlr/antlr4.git", "b395127e733b33c27f344695ebf155ecf5edeeab",
//            "Move Class org.antlr.v4.runtime.tree.gui.TreeViewer moved to org.antlr.v4.gui.TreeViewer",
//            "Move Class org.antlr.v4.runtime.tree.gui.TreeTextProvider moved to org.antlr.v4.gui.TreeTextProvider",
//            "Move Class org.antlr.v4.runtime.tree.gui.TreePostScriptGenerator moved to org.antlr.v4.gui.TreePostScriptGenerator",
//            "Move Class org.antlr.v4.runtime.tree.gui.TreeLayoutAdaptor moved to org.antlr.v4.gui.TreeLayoutAdaptor",
//            "Move Class org.antlr.v4.runtime.misc.TestRig moved to org.antlr.v4.gui.TestRig",
//            "Move Class org.antlr.v4.runtime.tree.gui.SystemFontMetrics moved to org.antlr.v4.gui.SystemFontMetrics",
//            "Move Class org.antlr.v4.runtime.tree.gui.PostScriptDocument moved to org.antlr.v4.gui.PostScriptDocument",
//            "Move Class org.antlr.v4.runtime.misc.JFileChooserConfirmOverwrite moved to org.antlr.v4.gui.JFileChooserConfirmOverwrite",
//            "Move Class org.antlr.v4.runtime.misc.GraphicsSupport moved to org.antlr.v4.gui.GraphicsSupport",
//            "Move Class org.antlr.v4.runtime.tree.gui.BasicFontMetrics moved to org.antlr.v4.gui.BasicFontMetrics");
//
//          process("https://github.com/plutext/docx4j.git", "1ba361438ab4d7f6a0305428ba40ba62e2e6ff3c",
//            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.ObjectFactory moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.ObjectFactory",
//            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTVbaSuppData moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTVbaSuppData",
//            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTMcds moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTMcds",
//            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTMcd moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTMcd",
//            "Move Class org.docx4j.schemas.microsoft.com.office.word_2006.wordml.CTDocEvents moved to org.docx4j.com.microsoft.schemas.office.word.x2006.wordml.CTDocEvents");
//
//          process("https://github.com/processing/processing.git", "d403a0b2322a74dde824094d67b7997c1c371883",
//            "Move Class processing.app.contrib.UpdateStatusPanel moved to processing.app.contrib.UpdateContributionTab.UpdateStatusPanel",
//            "Move Class processing.app.contrib.UpdateContribListingPanel moved to processing.app.contrib.UpdateContributionTab.UpdateContribListingPanel");
//
//          process("https://github.com/apache/cassandra.git", "4fcd7d4d366d001cf5f1f7d926c608c902e3f0af",
//            "Move Class org.apache.cassandra.locator.DynamicEndpointSnitchTest.ScoreUpdater moved to org.apache.cassandra.locator.DynamicEndpointSnitchLongTest.ScoreUpdater");
//
//          process("https://github.com/opentripplanner/OpenTripPlanner.git", "334dbc7cf3432e7c17b0ed98801e61b0b591b408",
//            "Move Class org.opentripplanner.analyst.cluster.AnalystWorker.WorkerIdDefiner moved to org.opentripplanner.analyst.cluster.WorkerIdDefiner");
//
//          process("https://github.com/JetBrains/intellij-community.git", "97811cf971f7ccf6a5fc5e90a491db2f58d49da1",
//            "Move Class org.jetbrains.jps.cmdline.BuildMain.MyLoggerFactory moved to org.jetbrains.jps.cmdline.LogSetup.MyLoggerFactory");
//
//          process("https://github.com/VoltDB/voltdb.git", "7527cfc746dc20ddb78002c7b3a65d55026a334e",
//            "Move Class org.voltdb.importer.ChannelChangeNotifier.CallbacksRef moved to org.voltdb.importer.ChannelDistributer.CallbacksRef");
//
//          process("https://github.com/NLPchina/ansj_seg.git", "913704e835169255530c7408cad11ce9a714d4ec",
//            "Move Class org.ansj.app.crf.pojo.TempFeature moved to org.ansj.app.crf.CrfppModelParser.TempFeature");
//
//          process("https://github.com/MovingBlocks/Terasology.git", "543a9808a85619dbe5acc2373cb4fe5344442de7",
//            "Move Method public isFullscreen() : boolean from class org.terasology.engine.TerasologyEngine to public isFullscreen() : boolean from class org.terasology.engine.subsystem.lwjgl.LwjglDisplayDevice",
//            "Inline Method private initTimer(context Context) : void inlined to public preInitialise(context Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglTimer",
//            "Inline Method private initOpenAL(context Context) : void inlined to public initialise(rootContext Context) : void in class org.terasology.engine.subsystem.lwjgl.LwjglAudio",
//            "Move Class org.terasology.engine.subsystem.ThreadManagerSubsystem moved to org.terasology.engine.subsystem.common.ThreadManagerSubsystem",
//            "Move Class org.terasology.engine.subsystem.ThreadManager moved to org.terasology.engine.subsystem.common.ThreadManager",
//            "Move Attribute private time : EngineTime from class org.terasology.engine.TerasologyEngine to class org.terasology.engine.subsystem.lwjgl.LwjglTimer",
//            "Move Attribute private time : EngineTime from class org.terasology.engine.TerasologyEngine to class org.terasology.engine.subsystem.headless.HeadlessTimer");
//
//          process("https://github.com/facebook/presto.git", "484b7cb0d20ec8f7c3b0d9eaf9e3f93468cec88c",
//            "Move Class com.facebook.presto.split.TestJmxSplitManager moved to com.facebook.presto.connector.jmx.TestJmxSplitManager");
//
//          process("https://github.com/greenrobot/greenDAO.git", "d6d9dd4365387816fda6987a4ad9b679c27e72a3",
//            "Move Class de.greenrobot.dao.PropertyConverter moved to de.greenrobot.dao.converter.PropertyConverter");
//
//          // missing object exception
////          process("https://github.com/spring-projects/spring-data-neo4j.git", "ef2a0d63393484975854fc08ad0fd3abc7dd76b0",
////            "Move Class org.springframework.data.neo4j.examples.friends.Person moved to org.springframework.data.neo4j.examples.friends.domain.Person",
////            "Move Class org.springframework.data.neo4j.examples.friends.Friendship moved to org.springframework.data.neo4j.examples.friends.domain.Friendship",
////            "Move Class org.springframework.data.neo4j.examples.friends.FriendContext moved to org.springframework.data.neo4j.examples.friends.context.FriendContext");
//
//          process("https://github.com/neo4j/neo4j.git", "4beba7bbdf927486a5cbf298a0fb2be50905a590",
//            "Move Class org.neo4j.kernel.impl.store.UniquePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.UniquePropertyConstraintRule",
//            "Move Class org.neo4j.kernel.impl.store.RelationshipPropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.RelationshipPropertyConstraintRule",
//            "Move Class org.neo4j.kernel.impl.store.PropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.PropertyConstraintRule",
//            "Move Class org.neo4j.kernel.impl.store.NodePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.NodePropertyConstraintRule",
//            "Move Class org.neo4j.kernel.impl.store.MandatoryRelationshipPropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.MandatoryRelationshipPropertyConstraintRule",
//            "Move Class org.neo4j.kernel.impl.store.MandatoryNodePropertyConstraintRule moved to org.neo4j.kernel.impl.store.record.MandatoryNodePropertyConstraintRule");
//
//          process("https://github.com/jersey/jersey.git", "ee5aa50af6b4586fbe92cab718abfae8113a81aa",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringRequestResourceTest moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringRequestResourceTest",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringRequestResource moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringRequestResource",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.SpringAnnotationConfig moved to org.glassfish.jersey.examples.hello.spring.annotations.SpringAnnotationConfig",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.NorwegianGoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.NorwegianGoodbyeService",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.JerseyConfig moved to org.glassfish.jersey.examples.hello.spring.annotations.JerseyConfig",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.GreetingService moved to org.glassfish.jersey.examples.hello.spring.annotations.GreetingService",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.GoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.GoodbyeService",
//            "Move Class org.glassfish.jersey.examples.hello.spring.annotations.annotations.EnglishGoodbyeService moved to org.glassfish.jersey.examples.hello.spring.annotations.EnglishGoodbyeService");
//
//          process("https://github.com/robovm/robovm.git", "1ef86e69d5a108c2b4d836b0634ebdea912cfe00",
//            "Move Class org.robovm.compiler.plugin.lambda2.LambdaPlugin moved to org.robovm.compiler.plugin.lambda.LambdaPlugin",
//            "Move Class org.robovm.compiler.plugin.lambda2.LambdaClassGenerator moved to org.robovm.compiler.plugin.lambda.LambdaClassGenerator",
//            "Move Class org.robovm.compiler.plugin.lambda2.LambdaClass moved to org.robovm.compiler.plugin.lambda.LambdaClass");
//
//          process("https://github.com/PhilJay/MPAndroidChart.git", "3514aaedf9624222c985cb3abb12df2d9b514b12",
//            "Move Class com.github.mikephil.charting.utils.Highlight moved to com.github.mikephil.charting.highlight.Highlight");
//
//          process("https://github.com/RoboBinding/RoboBinding.git", "b6565814805dfb2d989be25c11d4fb4cf8fb1d84",
//            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.SubItemPresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.SubItemPresentationModelExample",
//            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.SubItem moved to org.robobinding.codegen.presentationmodel.nestedIPM.SubItem",
//            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.PresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.PresentationModelExample",
//            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.ItemPresentationModelExample moved to org.robobinding.codegen.presentationmodel.nestedIPM.ItemPresentationModelExample",
//            "Move Class org.robobinding.codegen.presentationmodel.nestedIPMexample.Item moved to org.robobinding.codegen.presentationmodel.nestedIPM.Item");
//
//          process("https://github.com/neo4j/neo4j.git", "021d17c8234904dcb1d54596662352395927fe7b",
//            "Move Method public nodesGetAllCursor(statement StoreStatement) : Cursor<NodeItem> from class org.neo4j.kernel.impl.api.store.DiskLayer to public nodesGetAllCursor() : Cursor<NodeItem> from class org.neo4j.kernel.impl.api.store.StoreStatement",
//            "Move Method private directionOf(nodeId long, relationshipId long, startNode long, endNode long) : Direction from class org.neo4j.kernel.impl.api.store.DiskLayer to private directionOf(nodeId long, relationshipId long, startNode long, endNode long) : Direction from class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Move Method private countByFirstPrevPointer(nodeId long, relationshipId long) : long from class org.neo4j.kernel.impl.api.store.DiskLayer to private countByFirstPrevPointer(relationshipId long) : long from class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Move Method private nodeDegreeByDirection(nodeId long, group RelationshipGroupRecord, direction Direction) : long from class org.neo4j.kernel.impl.api.store.DiskLayer to private nodeDegreeByDirection(group RelationshipGroupRecord, direction Direction) : long from class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Extract Method private assertHasRelationships(node long) : void extracted from private deleteNode1(node long, prop1 DefinedProperty, prop2 DefinedProperty, prop3 DefinedProperty) : void in class org.neo4j.kernel.impl.store.TestNeoStore",
//            "Extract Method private assertHasRelationships(node long) : void extracted from private deleteNode2(node long, prop1 DefinedProperty, prop2 DefinedProperty, prop3 DefinedProperty) : void in class org.neo4j.kernel.impl.store.TestNeoStore",
//            "Move Class org.neo4j.kernel.impl.api.store.DiskLayer.AllStoreIdIterator moved to org.neo4j.kernel.impl.api.store.StoreStatement.AllStoreIdIterator",
//            "Move Attribute package GET_LABEL : ToIntFunction<LabelItem> from class org.neo4j.kernel.api.cursor.LabelItem to class org.neo4j.kernel.api.cursor.NodeItem",
//            "Move Attribute private labelCursor : InstanceCache<StoreLabelCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Move Attribute private singleLabelCursor : InstanceCache<StoreSingleLabelCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Move Attribute private nodeRelationshipCursor : InstanceCache<StoreNodeRelationshipCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Move Attribute private singlePropertyCursor : InstanceCache<StoreSinglePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Move Attribute private allPropertyCursor : InstanceCache<StorePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractNodeCursor",
//            "Move Attribute private singlePropertyCursor : InstanceCache<StoreSinglePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractRelationshipCursor",
//            "Move Attribute private allPropertyCursor : InstanceCache<StorePropertyCursor> from class org.neo4j.kernel.impl.api.store.StoreStatement to class org.neo4j.kernel.impl.api.store.StoreAbstractRelationshipCursor");
//
//          process("https://github.com/hazelcast/hazelcast.git", "f1e26fa73074a89680a2e1756d85eb80ad87c3bf",
//            "Move Class com.hazelcast.query.Predicates.InstanceOfPredicate moved to com.hazelcast.query.impl.predicates.InstanceOfPredicate",
//            "Move Class com.hazelcast.query.Predicates.BetweenPredicate moved to com.hazelcast.query.impl.predicates.BetweenPredicate",
//            "Move Class com.hazelcast.query.Predicates.NotPredicate moved to com.hazelcast.query.impl.predicates.NotPredicate",
//            "Move Class com.hazelcast.query.Predicates.InPredicate moved to com.hazelcast.query.impl.predicates.InPredicate",
//            "Move Class com.hazelcast.query.Predicates.RegexPredicate moved to com.hazelcast.query.impl.predicates.RegexPredicate",
//            "Move Class com.hazelcast.query.Predicates.LikePredicate moved to com.hazelcast.query.impl.predicates.LikePredicate",
//            "Move Class com.hazelcast.query.Predicates.ILikePredicate moved to com.hazelcast.query.impl.predicates.ILikePredicate",
//            "Move Class com.hazelcast.query.Predicates.AndPredicate moved to com.hazelcast.query.impl.predicates.AndPredicate",
//            "Move Class com.hazelcast.query.Predicates.OrPredicate moved to com.hazelcast.query.impl.predicates.OrPredicate",
//            "Move Class com.hazelcast.query.Predicates.GreaterLessPredicate moved to com.hazelcast.query.impl.predicates.GreaterLessPredicate",
//            "Move Class com.hazelcast.query.Predicates.NotEqualPredicate moved to com.hazelcast.query.impl.predicates.NotEqualPredicate",
//            "Move Class com.hazelcast.query.Predicates.EqualPredicate moved to com.hazelcast.query.impl.predicates.EqualPredicate",
//            "Move Class com.hazelcast.query.Predicates.AbstractPredicate moved to com.hazelcast.query.impl.predicates.AbstractPredicate");
//
//          process("https://github.com/addthis/hydra.git", "7fea4c9d5ee97d4a61ad985cadc9c5c0ab2db780",
//            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerTest moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerTest",
//            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerTaskSizer moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerTaskSizer",
//            "Move Class com.addthis.hydra.job.spawn.SpawnBalancerConfig moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancerConfig",
//            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.HostScore moved to com.addthis.hydra.job.spawn.balancer.HostScore",
//            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.HostAndScore moved to com.addthis.hydra.job.spawn.balancer.HostAndScore",
//            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer.JobTaskItem moved to com.addthis.hydra.job.spawn.balancer.JobTaskItem",
//            "Move Class com.addthis.hydra.job.spawn.SpawnBalancer moved to com.addthis.hydra.job.spawn.balancer.SpawnBalancer");
//
//          process("https://github.com/wildfly/wildfly.git", "4aa2e8746b5492bbc1cf2b36af956cf3b01e40f5",
//            "Move Class org.wildfly.clustering.spi.GroupServiceNameFactory moved to org.wildfly.clustering.service.GroupServiceNameFactory");
//
//          process("https://github.com/hibernate/hibernate-orm.git", "0b6ea757e34a63b1421b77ed5fbb61398377aab1",
//            "Move Class org.hibernate.jpa.test.callbacks.EntityWithLazyProperty moved to org.hibernate.jpa.test.instrument.domain.EntityWithLazyProperty");
//
//          process("https://github.com/apache/cassandra.git", "3bdcaa336a6e6a9727c333b433bb9f5d3afc0fb1",
//            "Move Class org.apache.cassandra.AbstractReadCommandBuilder moved to org.apache.cassandra.db.AbstractReadCommandBuilder",
//            "Extract Method public dumpMemtable() : void extracted from public truncateBlocking() : void in class org.apache.cassandra.db.ColumnFamilyStore");
//
//          process("https://github.com/google/j2objc.git", "d05d92de40542e85f9f26712d976e710be82914e",
//            "Move Class com.google.devtools.j2objc.translate.LambdaExpressionTest moved to com.google.devtools.j2objc.ast.LambdaExpressionTest");
//
//          process("https://github.com/JetBrains/intellij-community.git", "2b76aa336d696bbbbb205e6b6998e07ae5eb4261",
//            "Move Class org.jetbrains.plugins.groovy.util.ResolveProfiler moved to com.intellij.util.profiling.ResolveProfiler");
//
//          process("https://github.com/geoserver/geoserver.git", "07c26a3a1dd6fcc2494c2d755ee5a2753e0df87c",
//            "Move Class org.geoserver.wfs.xml.PropertyRule moved to org.geoserver.util.PropertyRule");
//
//          process("https://github.com/spring-projects/spring-integration.git", "4cca684f368d3ff719c62d3fa4cac3cdb7828bff",
//            "Move Class org.springframework.integration.codec.CompositeCodecTests moved to org.springframework.integration.codec.kryo.CompositeCodecTests");
//
//          process("https://github.com/jersey/jersey.git", "d94ca2b27c9e8a5fa9fe19483d58d2f2ef024606",
//            "Move Class org.glassfish.jersey.client.HttpUrlConnector moved to org.glassfish.jersey.client.internal.HttpUrlConnector");
//
//          process("https://github.com/JetBrains/intellij-community.git", "6b90205e9f7bbd1570f600d3812fd3efa1a0597e",
//            "Move Class com.intellij.execution.console.RunIdeConsoleAction.IDE moved to com.intellij.ide.script.IDE");
//
//          process("https://github.com/JoanZapata/android-iconify.git", "b08f28a10d050beaba6250e9e9c46efe13d9caaa",
//            "Move Class android.widget.IconToggleButton moved to com.joanzapata.android.iconify.views.IconToggleButton",
//            "Move Class android.widget.IconTextView moved to com.joanzapata.android.iconify.views.IconTextView",
//            "Move Class android.widget.IconButton moved to com.joanzapata.android.iconify.views.IconButton");
//
//          process("https://github.com/hibernate/hibernate-orm.git", "7ccbd4693288dbdbc2e6844aa0877640d63fbd04",
//            "Move Class org.hibernate.test.annotations.enumerated.LastNumberType moved to org.hibernate.test.annotations.enumerated.custom_types.LastNumberType",
//            "Move Class org.hibernate.test.annotations.enumerated.FirstLetterType moved to org.hibernate.test.annotations.enumerated.custom_types.FirstLetterType");
//
//          process("https://github.com/github/android.git", "c97659888126e43e95f0d52d22188bfe194a8439",
//            "Move Class com.github.mobile.ui.user.IconAndViewTextManagerTest moved to com.github.pockethub.ui.user.IconAndViewTextManagerTest",
//            "Move Class com.github.mobile.util.TypefaceUtils moved to com.github.pockethub.util.TypefaceUtils",
//            "Move Class com.github.mobile.util.ToastUtils moved to com.github.pockethub.util.ToastUtils",
//            "Move Class com.github.mobile.util.TimeUtils moved to com.github.pockethub.util.TimeUtils",
//            "Move Class com.github.mobile.util.SourceEditor moved to com.github.pockethub.util.SourceEditor",
//            "Move Class com.github.mobile.util.ShareUtils moved to com.github.pockethub.util.ShareUtils",
//            "Move Class com.github.mobile.util.ServiceUtils moved to com.github.pockethub.util.ServiceUtils",
//            "Move Class com.github.mobile.util.PreferenceUtils moved to com.github.pockethub.util.PreferenceUtils",
//            "Move Class com.github.mobile.util.MarkdownUtils moved to com.github.pockethub.util.MarkdownUtils",
//            "Move Class com.github.mobile.util.ImageUtils moved to com.github.pockethub.util.ImageUtils",
//            "Move Class com.github.mobile.util.HttpImageGetter moved to com.github.pockethub.util.HttpImageGetter",
//            "Move Class com.github.mobile.util.HtmlUtils moved to com.github.pockethub.util.HtmlUtils",
//            "Move Class com.github.mobile.util.GravatarUtils moved to com.github.pockethub.util.GravatarUtils",
//            "Move Class com.github.mobile.util.AvatarLoader moved to com.github.pockethub.util.AvatarLoader",
//            "Move Class com.github.mobile.ui.user.UserViewActivity moved to com.github.pockethub.ui.user.UserViewActivity",
//            "Move Class com.github.mobile.ui.user.UserReceivedNewsFragment moved to com.github.pockethub.ui.user.UserReceivedNewsFragment",
//            "Move Class com.github.mobile.ui.user.UserPagerAdapter moved to com.github.pockethub.ui.user.UserPagerAdapter",
//            "Move Class com.github.mobile.ui.user.UserNewsFragment moved to com.github.pockethub.ui.user.UserNewsFragment",
//            "Move Class com.github.mobile.ui.user.UserListAdapter moved to com.github.pockethub.ui.user.UserListAdapter",
//            "Move Class com.github.mobile.ui.user.UserFollowingFragment moved to com.github.pockethub.ui.user.UserFollowingFragment",
//            "Move Class com.github.mobile.ui.user.UserFollowersFragment moved to com.github.pockethub.ui.user.UserFollowersFragment",
//            "Move Class com.github.mobile.ui.user.UserCreatedNewsFragment moved to com.github.pockethub.ui.user.UserCreatedNewsFragment",
//            "Move Class com.github.mobile.ui.user.UriLauncherActivity moved to com.github.pockethub.ui.user.UriLauncherActivity",
//            "Move Class com.github.mobile.ui.user.PagedUserFragment moved to com.github.pockethub.ui.user.PagedUserFragment",
//            "Move Class com.github.mobile.ui.user.OrganizationSelectionProvider moved to com.github.pockethub.ui.user.OrganizationSelectionProvider",
//            "Move Class com.github.mobile.ui.user.OrganizationSelectionListener moved to com.github.pockethub.ui.user.OrganizationSelectionListener",
//            "Move Class com.github.mobile.ui.user.OrganizationNewsFragment moved to com.github.pockethub.ui.user.OrganizationNewsFragment",
//            "Move Class com.github.mobile.ui.user.NewsListAdapter moved to com.github.pockethub.ui.user.NewsListAdapter",
//            "Move Class com.github.mobile.ui.user.MyFollowingFragment moved to com.github.pockethub.ui.user.MyFollowingFragment",
//            "Move Class com.github.mobile.ui.user.MyFollowersFragment moved to com.github.pockethub.ui.user.MyFollowersFragment",
//            "Move Class com.github.mobile.ui.user.MembersFragment moved to com.github.pockethub.ui.user.MembersFragment",
//            "Move Class com.github.mobile.ui.user.IconAndViewTextManager moved to com.github.pockethub.ui.user.IconAndViewTextManager",
//            "Move Class com.github.mobile.ui.user.HomePagerFragment moved to com.github.pockethub.ui.user.HomePagerFragment",
//            "Move Class com.github.mobile.ui.user.HomePagerAdapter moved to com.github.pockethub.ui.user.HomePagerAdapter",
//            "Move Class com.github.mobile.ui.user.FollowingFragment moved to com.github.pockethub.ui.user.FollowingFragment",
//            "Move Class com.github.mobile.ui.user.FollowersFragment moved to com.github.pockethub.ui.user.FollowersFragment",
//            "Move Class com.github.mobile.ui.user.EventPager moved to com.github.pockethub.ui.user.EventPager",
//            "Move Class com.github.mobile.ui.search.SearchUserListFragment moved to com.github.pockethub.ui.search.SearchUserListFragment",
//            "Move Class com.github.mobile.ui.search.SearchUserListAdapter moved to com.github.pockethub.ui.search.SearchUserListAdapter",
//            "Move Class com.github.mobile.ui.search.SearchRepositoryListFragment moved to com.github.pockethub.ui.search.SearchRepositoryListFragment",
//            "Move Class com.github.mobile.ui.search.SearchRepositoryListAdapter moved to com.github.pockethub.ui.search.SearchRepositoryListAdapter",
//            "Move Class com.github.mobile.ui.search.SearchPagerAdapter moved to com.github.pockethub.ui.search.SearchPagerAdapter",
//            "Move Class com.github.mobile.ui.search.SearchActivity moved to com.github.pockethub.ui.search.SearchActivity",
//            "Move Class com.github.mobile.ui.search.RepositorySearchSuggestionsProvider moved to com.github.pockethub.ui.search.RepositorySearchSuggestionsProvider",
//            "Move Class com.github.mobile.ui.roboactivities.RoboSupportFragment moved to com.github.pockethub.ui.roboactivities.RoboSupportFragment",
//            "Move Class com.github.mobile.ui.roboactivities.RoboActionBarActivity moved to com.github.pockethub.ui.roboactivities.RoboActionBarActivity",
//            "Move Class com.github.mobile.ui.roboactivities.RoboActionBarAccountAuthenticatorActivity moved to com.github.pockethub.ui.roboactivities.RoboActionBarAccountAuthenticatorActivity",
//            "Move Class com.github.mobile.ui.roboactivities.ActionBarAccountAuthenticatorActivity moved to com.github.pockethub.ui.roboactivities.ActionBarAccountAuthenticatorActivity",
//            "Move Class com.github.mobile.ui.repo.UserRepositoryListFragment moved to com.github.pockethub.ui.repo.UserRepositoryListFragment",
//            "Move Class com.github.mobile.ui.repo.UserRepositoryListAdapter moved to com.github.pockethub.ui.repo.UserRepositoryListAdapter",
//            "Move Class com.github.mobile.ui.repo.RepositoryViewActivity moved to com.github.pockethub.ui.repo.RepositoryViewActivity",
//            "Move Class com.github.mobile.ui.repo.RepositoryPagerAdapter moved to com.github.pockethub.ui.repo.RepositoryPagerAdapter",
//            "Move Class com.github.mobile.ui.repo.RepositoryNewsFragment moved to com.github.pockethub.ui.repo.RepositoryNewsFragment",
//            "Move Class com.github.mobile.ui.repo.RepositoryListFragment moved to com.github.pockethub.ui.repo.RepositoryListFragment",
//            "Move Class com.github.mobile.ui.repo.RepositoryListAdapter moved to com.github.pockethub.ui.repo.RepositoryListAdapter",
//            "Move Class com.github.mobile.ui.repo.RepositoryContributorsFragment moved to com.github.pockethub.ui.repo.RepositoryContributorsFragment",
//            "Move Class com.github.mobile.ui.repo.RepositoryContributorsActivity moved to com.github.pockethub.ui.repo.RepositoryContributorsActivity",
//            "Move Class com.github.mobile.ui.repo.RecentRepositories moved to com.github.pockethub.ui.repo.RecentRepositories",
//            "Move Class com.github.mobile.ui.repo.OrganizationLoader moved to com.github.pockethub.ui.repo.OrganizationLoader",
//            "Move Class com.github.mobile.ui.repo.DefaultRepositoryListAdapter moved to com.github.pockethub.ui.repo.DefaultRepositoryListAdapter",
//            "Move Class com.github.mobile.ui.repo.ContributorListAdapter moved to com.github.pockethub.ui.repo.ContributorListAdapter",
//            "Move Class com.github.mobile.ui.ref.RefDialogFragment moved to com.github.pockethub.ui.ref.RefDialogFragment",
//            "Move Class com.github.mobile.ui.ref.RefDialog moved to com.github.pockethub.ui.ref.RefDialog",
//            "Move Class com.github.mobile.ui.ref.CodeTreeAdapter moved to com.github.pockethub.ui.ref.CodeTreeAdapter",
//            "Move Class com.github.mobile.ui.ref.BranchFileViewActivity moved to com.github.pockethub.ui.ref.BranchFileViewActivity",
//            "Move Class com.github.mobile.ui.issue.SearchIssueListFragment moved to com.github.pockethub.ui.issue.SearchIssueListFragment",
//            "Move Class com.github.mobile.ui.issue.SearchIssueListAdapter moved to com.github.pockethub.ui.issue.SearchIssueListAdapter",
//            "Move Class com.github.mobile.ui.issue.RepositoryIssueListAdapter moved to com.github.pockethub.ui.issue.RepositoryIssueListAdapter",
//            "Move Class com.github.mobile.ui.issue.MilestoneDialogFragment moved to com.github.pockethub.ui.issue.MilestoneDialogFragment",
//            "Move Class com.github.mobile.ui.issue.MilestoneDialog moved to com.github.pockethub.ui.issue.MilestoneDialog",
//            "Move Class com.github.mobile.ui.issue.LabelsDialogFragment moved to com.github.pockethub.ui.issue.LabelsDialogFragment",
//            "Move Class com.github.mobile.ui.issue.LabelsDialog moved to com.github.pockethub.ui.issue.LabelsDialog",
//            "Move Class com.github.mobile.ui.issue.LabelDrawableSpan moved to com.github.pockethub.ui.issue.LabelDrawableSpan",
//            "Move Class com.github.mobile.ui.issue.IssuesViewActivity moved to com.github.pockethub.ui.issue.IssuesViewActivity",
//            "Move Class com.github.mobile.ui.issue.IssuesPagerAdapter moved to com.github.pockethub.ui.issue.IssuesPagerAdapter",
//            "Move Class com.github.mobile.ui.issue.IssuesFragment moved to com.github.pockethub.ui.issue.IssuesFragment",
//            "Move Class com.github.mobile.ui.issue.IssueSearchSuggestionsProvider moved to com.github.pockethub.ui.issue.IssueSearchSuggestionsProvider",
//            "Move Class com.github.mobile.ui.issue.IssueSearchActivity moved to com.github.pockethub.ui.issue.IssueSearchActivity",
//            "Move Class com.github.mobile.ui.issue.IssueListAdapter moved to com.github.pockethub.ui.issue.IssueListAdapter",
//            "Move Class com.github.mobile.ui.issue.IssueFragment moved to com.github.pockethub.ui.issue.IssueFragment",
//            "Move Class com.github.mobile.ui.issue.IssueDashboardPagerFragment moved to com.github.pockethub.ui.issue.IssueDashboardPagerFragment",
//            "Move Class com.github.mobile.ui.issue.IssueDashboardPagerAdapter moved to com.github.pockethub.ui.issue.IssueDashboardPagerAdapter",
//            "Move Class com.github.mobile.ui.issue.IssueBrowseActivity moved to com.github.pockethub.ui.issue.IssueBrowseActivity",
//            "Move Class com.github.mobile.ui.issue.FiltersViewFragment moved to com.github.pockethub.ui.issue.FiltersViewFragment",
//            "Move Class com.github.mobile.ui.issue.FiltersViewActivity moved to com.github.pockethub.ui.issue.FiltersViewActivity",
//            "Move Class com.github.mobile.ui.issue.FilterListFragment moved to com.github.pockethub.ui.issue.FilterListFragment",
//            "Move Class com.github.mobile.ui.issue.FilterListAdapter moved to com.github.pockethub.ui.issue.FilterListAdapter",
//            "Move Class com.github.mobile.ui.issue.EditStateTask moved to com.github.pockethub.ui.issue.EditStateTask",
//            "Move Class com.github.mobile.ui.issue.EditMilestoneTask moved to com.github.pockethub.ui.issue.EditMilestoneTask",
//            "Move Class com.github.mobile.ui.issue.EditLabelsTask moved to com.github.pockethub.ui.issue.EditLabelsTask",
//            "Move Class com.github.mobile.ui.issue.EditIssuesFilterActivity moved to com.github.pockethub.ui.issue.EditIssuesFilterActivity",
//            "Move Class com.github.mobile.ui.issue.EditIssueTask moved to com.github.pockethub.ui.issue.EditIssueTask",
//            "Move Class com.github.mobile.ui.issue.EditIssueActivity moved to com.github.pockethub.ui.issue.EditIssueActivity",
//            "Move Class com.github.mobile.ui.issue.EditCommentTask moved to com.github.pockethub.ui.issue.EditCommentTask",
//            "Move Class com.github.mobile.ui.issue.EditCommentActivity moved to com.github.pockethub.ui.issue.EditCommentActivity",
//            "Move Class com.github.mobile.ui.issue.EditAssigneeTask moved to com.github.pockethub.ui.issue.EditAssigneeTask",
//            "Move Class com.github.mobile.ui.issue.DeleteCommentTask moved to com.github.pockethub.ui.issue.DeleteCommentTask",
//            "Move Class com.github.mobile.ui.issue.DashboardIssueListAdapter moved to com.github.pockethub.ui.issue.DashboardIssueListAdapter",
//            "Move Class com.github.mobile.ui.issue.DashboardIssueFragment moved to com.github.pockethub.ui.issue.DashboardIssueFragment",
//            "Move Class com.github.mobile.ui.issue.CreateIssueTask moved to com.github.pockethub.ui.issue.CreateIssueTask",
//            "Move Class com.github.mobile.ui.issue.CreateCommentTask moved to com.github.pockethub.ui.issue.CreateCommentTask",
//            "Move Class com.github.mobile.ui.issue.CreateCommentActivity moved to com.github.pockethub.ui.issue.CreateCommentActivity",
//            "Move Class com.github.mobile.ui.issue.AssigneeDialogFragment moved to com.github.pockethub.ui.issue.AssigneeDialogFragment",
//            "Move Class com.github.mobile.ui.issue.AssigneeDialog moved to com.github.pockethub.ui.issue.AssigneeDialog",
//            "Move Class com.github.mobile.ui.gist.StarredGistsFragment moved to com.github.pockethub.ui.gist.StarredGistsFragment",
//            "Move Class com.github.mobile.ui.gist.RandomGistTask moved to com.github.pockethub.ui.gist.RandomGistTask",
//            "Move Class com.github.mobile.ui.gist.PublicGistsFragment moved to com.github.pockethub.ui.gist.PublicGistsFragment",
//            "Move Class com.github.mobile.core.repo.UnstarRepositoryTask moved to com.github.pockethub.core.repo.UnstarRepositoryTask",
//            "Move Class com.github.mobile.core.repo.StarredRepositoryTask moved to com.github.pockethub.core.repo.StarredRepositoryTask",
//            "Move Class com.github.mobile.core.repo.StarRepositoryTask moved to com.github.pockethub.core.repo.StarRepositoryTask",
//            "Move Class com.github.mobile.core.repo.RepositoryUtils moved to com.github.pockethub.core.repo.RepositoryUtils",
//            "Move Class com.github.mobile.core.repo.RepositoryUriMatcher moved to com.github.pockethub.core.repo.RepositoryUriMatcher",
//            "Move Class com.github.mobile.core.repo.RepositoryEventMatcher moved to com.github.pockethub.core.repo.RepositoryEventMatcher",
//            "Move Class com.github.mobile.core.repo.RefreshRepositoryTask moved to com.github.pockethub.core.repo.RefreshRepositoryTask",
//            "Move Class com.github.mobile.core.repo.ForkRepositoryTask moved to com.github.pockethub.core.repo.ForkRepositoryTask",
//            "Move Class com.github.mobile.core.repo.DeleteRepositoryTask moved to com.github.pockethub.core.repo.DeleteRepositoryTask",
//            "Move Class com.github.mobile.core.ref.RefUtils moved to com.github.pockethub.core.ref.RefUtils",
//            "Move Class com.github.mobile.core.issue.RefreshIssueTask moved to com.github.pockethub.core.issue.RefreshIssueTask",
//            "Move Class com.github.mobile.core.issue.IssueUtils moved to com.github.pockethub.core.issue.IssueUtils",
//            "Move Class com.github.mobile.core.issue.IssueUriMatcher moved to com.github.pockethub.core.issue.IssueUriMatcher",
//            "Move Class com.github.mobile.core.issue.IssueStore moved to com.github.pockethub.core.issue.IssueStore",
//            "Move Class com.github.mobile.core.issue.IssuePager moved to com.github.pockethub.core.issue.IssuePager",
//            "Move Class com.github.mobile.core.issue.IssueFilter moved to com.github.pockethub.core.issue.IssueFilter",
//            "Move Class com.github.mobile.core.issue.IssueEventMatcher moved to com.github.pockethub.core.issue.IssueEventMatcher",
//            "Move Class com.github.mobile.core.issue.FullIssue moved to com.github.pockethub.core.issue.FullIssue",
//            "Move Class com.github.mobile.core.gist.UnstarGistTask moved to com.github.pockethub.core.gist.UnstarGistTask",
//            "Move Class com.github.mobile.core.gist.StarGistTask moved to com.github.pockethub.core.gist.StarGistTask",
//            "Move Class com.github.mobile.core.gist.RefreshGistTask moved to com.github.pockethub.core.gist.RefreshGistTask",
//            "Move Class com.github.mobile.core.gist.GistUriMatcher moved to com.github.pockethub.core.gist.GistUriMatcher",
//            "Move Class com.github.mobile.core.gist.GistStore moved to com.github.pockethub.core.gist.GistStore",
//            "Move Class com.github.mobile.core.gist.GistPager moved to com.github.pockethub.core.gist.GistPager",
//            "Move Class com.github.mobile.core.gist.GistEventMatcher moved to com.github.pockethub.core.gist.GistEventMatcher",
//            "Move Class com.github.mobile.core.gist.FullGist moved to com.github.pockethub.core.gist.FullGist",
//            "Move Class com.github.mobile.core.commit.RefreshCommitTask moved to com.github.pockethub.core.commit.RefreshCommitTask",
//            "Move Class com.github.mobile.core.commit.FullCommitFile moved to com.github.pockethub.core.commit.FullCommitFile",
//            "Move Class com.github.mobile.core.commit.FullCommit moved to com.github.pockethub.core.commit.FullCommit",
//            "Move Class com.github.mobile.core.commit.CommitUtils moved to com.github.pockethub.core.commit.CommitUtils",
//            "Move Class com.github.mobile.core.commit.CommitUriMatcher moved to com.github.pockethub.core.commit.CommitUriMatcher",
//            "Move Class com.github.mobile.core.commit.CommitStore moved to com.github.pockethub.core.commit.CommitStore",
//            "Move Class com.github.mobile.core.commit.CommitPager moved to com.github.pockethub.core.commit.CommitPager",
//            "Move Class com.github.mobile.core.commit.CommitMatch moved to com.github.pockethub.core.commit.CommitMatch",
//            "Move Class com.github.mobile.core.commit.CommitCompareTask moved to com.github.pockethub.core.commit.CommitCompareTask",
//            "Move Class com.github.mobile.core.code.RefreshTreeTask moved to com.github.pockethub.core.code.RefreshTreeTask",
//            "Move Class com.github.mobile.core.code.RefreshBlobTask moved to com.github.pockethub.core.code.RefreshBlobTask",
//            "Move Class com.github.mobile.core.code.FullTree moved to com.github.pockethub.core.code.FullTree",
//            "Move Class com.github.mobile.core.UrlMatcher moved to com.github.pockethub.core.UrlMatcher",
//            "Move Class com.github.mobile.core.ResourcePager moved to com.github.pockethub.core.ResourcePager",
//            "Move Class com.github.mobile.core.OnLoadListener moved to com.github.pockethub.core.OnLoadListener",
//            "Move Class com.github.mobile.core.ItemStore moved to com.github.pockethub.core.ItemStore",
//            "Move Class com.github.mobile.api.GitHubClientV2 moved to com.github.pockethub.api.GitHubClientV2",
//            "Move Class com.github.mobile.accounts.TwoFactorAuthException moved to com.github.pockethub.accounts.TwoFactorAuthException",
//            "Move Class com.github.mobile.accounts.TwoFactorAuthClient moved to com.github.pockethub.accounts.TwoFactorAuthClient",
//            "Move Class com.github.mobile.accounts.TwoFactorAuthActivity moved to com.github.pockethub.accounts.TwoFactorAuthActivity",
//            "Move Class com.github.mobile.accounts.ScopeBase moved to com.github.pockethub.accounts.ScopeBase",
//            "Move Class com.github.mobile.accounts.LoginWebViewActivity moved to com.github.pockethub.accounts.LoginWebViewActivity",
//            "Move Class com.github.mobile.tests.repo.SearchActivityTest moved to com.github.pockethub.tests.repo.SearchActivityTest",
//            "Move Class com.github.mobile.tests.repo.RepositoryUriMatcherTest moved to com.github.pockethub.tests.repo.RepositoryUriMatcherTest",
//            "Move Class com.github.mobile.tests.repo.RepositoryEventMatcherTest moved to com.github.pockethub.tests.repo.RepositoryEventMatcherTest",
//            "Move Class com.github.mobile.tests.repo.RecentRepositoriesTest moved to com.github.pockethub.tests.repo.RecentRepositoriesTest",
//            "Move Class com.github.mobile.tests.ref.RefUtilsTest moved to com.github.pockethub.tests.ref.RefUtilsTest",
//            "Move Class com.github.mobile.tests.issue.IssueUriMatcherTest moved to com.github.pockethub.tests.issue.IssueUriMatcherTest",
//            "Move Class com.github.mobile.tests.issue.IssueStoreTest moved to com.github.pockethub.tests.issue.IssueStoreTest",
//            "Move Class com.github.mobile.tests.issue.IssueFilterTest moved to com.github.pockethub.tests.issue.IssueFilterTest",
//            "Move Class com.github.mobile.tests.issue.EditIssuesFilterActivityTest moved to com.github.pockethub.tests.issue.EditIssuesFilterActivityTest",
//            "Move Class com.github.mobile.tests.issue.EditIssueActivityTest moved to com.github.pockethub.tests.issue.EditIssueActivityTest",
//            "Move Class com.github.mobile.tests.issue.CreateCommentActivityTest moved to com.github.pockethub.tests.issue.CreateCommentActivityTest",
//            "Move Class com.github.mobile.tests.gist.GistUriMatcherTest moved to com.github.pockethub.tests.gist.GistUriMatcherTest",
//            "Move Class com.github.mobile.tests.ActivityTest moved to com.github.pockethub.tests.ActivityTest",
//            "Move Class com.github.mobile.tests.FiltersViewActivityTest moved to com.github.pockethub.tests.FiltersViewActivityTest",
//            "Move Class com.github.mobile.tests.NewsEventTextTest moved to com.github.pockethub.tests.NewsEventTextTest",
//            "Move Class com.github.mobile.tests.commit.CommitUriMatcherTest moved to com.github.pockethub.tests.commit.CommitUriMatcherTest",
//            "Move Class com.github.mobile.tests.commit.CommitUtilsTest moved to com.github.pockethub.tests.commit.CommitUtilsTest",
//            "Move Class com.github.mobile.tests.commit.CreateCommentActivityTest moved to com.github.pockethub.tests.commit.CreateCommentActivityTest",
//            "Move Class com.github.mobile.tests.commit.DiffStylerTest moved to com.github.pockethub.tests.commit.DiffStylerTest",
//            "Move Class com.github.mobile.tests.commit.FullCommitTest moved to com.github.pockethub.tests.commit.FullCommitTest",
//            "Move Class com.github.mobile.tests.gist.CreateCommentActivityTest moved to com.github.pockethub.tests.gist.CreateCommentActivityTest",
//            "Move Class com.github.mobile.tests.gist.CreateGistActivityTest moved to com.github.pockethub.tests.gist.CreateGistActivityTest",
//            "Move Class com.github.mobile.tests.gist.GistFilesViewActivityTest moved to com.github.pockethub.tests.gist.GistFilesViewActivityTest",
//            "Move Class com.github.mobile.tests.gist.GistStoreTest moved to com.github.pockethub.tests.gist.GistStoreTest",
//            "Move Class com.github.mobile.tests.user.LoginActivityTest moved to com.github.pockethub.tests.user.LoginActivityTest",
//            "Move Class com.github.mobile.tests.user.UserComparatorTest moved to com.github.pockethub.tests.user.UserComparatorTest",
//            "Move Class com.github.mobile.tests.user.UserUriMatcherTest moved to com.github.pockethub.tests.user.UserUriMatcherTest",
//            "Move Class com.github.mobile.tests.util.HtmlUtilsTest moved to com.github.pockethub.tests.util.HtmlUtilsTest",
//            "Move Class com.github.mobile.DefaultClient moved to com.github.pockethub.DefaultClient",
//            "Move Class com.github.mobile.GitHubModule moved to com.github.pockethub.GitHubModule",
//            "Move Class com.github.mobile.Intents moved to com.github.pockethub.Intents",
//            "Move Class com.github.mobile.RequestCodes moved to com.github.pockethub.RequestCodes",
//            "Move Class com.github.mobile.RequestFuture moved to com.github.pockethub.RequestFuture",
//            "Move Class com.github.mobile.RequestReader moved to com.github.pockethub.RequestReader",
//            "Move Class com.github.mobile.RequestWriter moved to com.github.pockethub.RequestWriter",
//            "Move Class com.github.mobile.ResultCodes moved to com.github.pockethub.ResultCodes",
//            "Move Class com.github.mobile.ServicesModule moved to com.github.pockethub.ServicesModule",
//            "Move Class com.github.mobile.ThrowableLoader moved to com.github.pockethub.ThrowableLoader",
//            "Move Class com.github.mobile.accounts.AccountAuthenticator moved to com.github.pockethub.accounts.AccountAuthenticator",
//            "Move Class com.github.mobile.accounts.AccountAuthenticatorService moved to com.github.pockethub.accounts.AccountAuthenticatorService",
//            "Move Class com.github.mobile.accounts.AccountClient moved to com.github.pockethub.accounts.AccountClient",
//            "Move Class com.github.mobile.accounts.AccountConstants moved to com.github.pockethub.accounts.AccountConstants",
//            "Move Class com.github.mobile.accounts.AccountScope moved to com.github.pockethub.accounts.AccountScope",
//            "Move Class com.github.mobile.accounts.AccountUtils moved to com.github.pockethub.accounts.AccountUtils",
//            "Move Class com.github.mobile.accounts.AuthenticatedUserLoader moved to com.github.pockethub.accounts.AuthenticatedUserLoader",
//            "Move Class com.github.mobile.accounts.AuthenticatedUserTask moved to com.github.pockethub.accounts.AuthenticatedUserTask",
//            "Move Class com.github.mobile.accounts.GitHubAccount moved to com.github.pockethub.accounts.GitHubAccount",
//            "Move Class com.github.mobile.accounts.LoginActivity moved to com.github.pockethub.accounts.LoginActivity",
//            "Move Class com.github.mobile.core.search.SearchUser moved to com.github.pockethub.core.search.SearchUser",
//            "Move Class com.github.mobile.core.search.SearchUserService moved to com.github.pockethub.core.search.SearchUserService",
//            "Move Class com.github.mobile.core.user.FollowUserTask moved to com.github.pockethub.core.user.FollowUserTask",
//            "Move Class com.github.mobile.core.user.FollowingUserTask moved to com.github.pockethub.core.user.FollowingUserTask",
//            "Move Class com.github.mobile.core.user.RefreshUserTask moved to com.github.pockethub.core.user.RefreshUserTask",
//            "Move Class com.github.mobile.core.user.UnfollowUserTask moved to com.github.pockethub.core.user.UnfollowUserTask",
//            "Move Class com.github.mobile.core.user.UserComparator moved to com.github.pockethub.core.user.UserComparator",
//            "Move Class com.github.mobile.core.user.UserEventMatcher moved to com.github.pockethub.core.user.UserEventMatcher",
//            "Move Class com.github.mobile.core.user.UserPager moved to com.github.pockethub.core.user.UserPager",
//            "Move Class com.github.mobile.core.user.UserUriMatcher moved to com.github.pockethub.core.user.UserUriMatcher",
//            "Move Class com.github.mobile.model.App moved to com.github.pockethub.model.App",
//            "Move Class com.github.mobile.model.Authorization moved to com.github.pockethub.model.Authorization",
//            "Move Class com.github.mobile.persistence.AccountDataManager moved to com.github.pockethub.persistence.AccountDataManager",
//            "Move Class com.github.mobile.persistence.CacheHelper moved to com.github.pockethub.persistence.CacheHelper",
//            "Move Class com.github.mobile.persistence.DatabaseCache moved to com.github.pockethub.persistence.DatabaseCache",
//            "Move Class com.github.mobile.persistence.OrganizationRepositories moved to com.github.pockethub.persistence.OrganizationRepositories",
//            "Move Class com.github.mobile.persistence.Organizations moved to com.github.pockethub.persistence.Organizations",
//            "Move Class com.github.mobile.persistence.PersistableResource moved to com.github.pockethub.persistence.PersistableResource",
//            "Move Class com.github.mobile.sync.ContentProviderAdapter moved to com.github.pockethub.sync.ContentProviderAdapter",
//            "Move Class com.github.mobile.sync.SyncAdapter moved to com.github.pockethub.sync.SyncAdapter",
//            "Move Class com.github.mobile.sync.SyncAdapterService moved to com.github.pockethub.sync.SyncAdapterService",
//            "Move Class com.github.mobile.sync.SyncCampaign moved to com.github.pockethub.sync.SyncCampaign",
//            "Move Class com.github.mobile.ui.BaseActivity moved to com.github.pockethub.ui.BaseActivity",
//            "Move Class com.github.mobile.ui.CheckableRelativeLayout moved to com.github.pockethub.ui.CheckableRelativeLayout",
//            "Move Class com.github.mobile.ui.ConfirmDialogFragment moved to com.github.pockethub.ui.ConfirmDialogFragment",
//            "Move Class com.github.mobile.ui.DialogFragment moved to com.github.pockethub.ui.DialogFragment",
//            "Move Class com.github.mobile.ui.DialogFragmentActivity moved to com.github.pockethub.ui.DialogFragmentActivity",
//            "Move Class com.github.mobile.ui.DialogFragmentHelper moved to com.github.pockethub.ui.DialogFragmentHelper",
//            "Move Class com.github.mobile.ui.DialogResultListener moved to com.github.pockethub.ui.DialogResultListener",
//            "Move Class com.github.mobile.ui.FragmentPagerAdapter moved to com.github.pockethub.ui.FragmentPagerAdapter",
//            "Move Class com.github.mobile.ui.FragmentProvider moved to com.github.pockethub.ui.FragmentProvider",
//            "Move Class com.github.mobile.ui.FragmentStatePagerAdapter moved to com.github.pockethub.ui.FragmentStatePagerAdapter",
//            "Move Class com.github.mobile.ui.HeaderFooterListAdapter moved to com.github.pockethub.ui.HeaderFooterListAdapter",
//            "Move Class com.github.mobile.ui.ItemListFragment moved to com.github.pockethub.ui.ItemListFragment",
//            "Move Class com.github.mobile.ui.LightAlertDialog moved to com.github.pockethub.ui.LightAlertDialog",
//            "Move Class com.github.mobile.ui.LightProgressDialog moved to com.github.pockethub.ui.LightProgressDialog",
//            "Move Class com.github.mobile.ui.MainActivity moved to com.github.pockethub.ui.MainActivity",
//            "Move Class com.github.mobile.ui.MarkdownLoader moved to com.github.pockethub.ui.MarkdownLoader",
//            "Move Class com.github.mobile.ui.NavigationDrawerAdapter moved to com.github.pockethub.ui.NavigationDrawerAdapter",
//            "Move Class com.github.mobile.ui.NavigationDrawerFragment moved to com.github.pockethub.ui.NavigationDrawerFragment",
//            "Move Class com.github.mobile.ui.NavigationDrawerObject moved to com.github.pockethub.ui.NavigationDrawerObject",
//            "Move Class com.github.mobile.ui.NewsFragment moved to com.github.pockethub.ui.NewsFragment",
//            "Move Class com.github.mobile.ui.PagedItemFragment moved to com.github.pockethub.ui.PagedItemFragment",
//            "Move Class com.github.mobile.ui.PagerActivity moved to com.github.pockethub.ui.PagerActivity",
//            "Move Class com.github.mobile.ui.PagerFragment moved to com.github.pockethub.ui.PagerFragment",
//            "Move Class com.github.mobile.ui.PatchedScrollingViewBehavior moved to com.github.pockethub.ui.PatchedScrollingViewBehavior",
//            "Move Class com.github.mobile.ui.ProgressDialogTask moved to com.github.pockethub.ui.ProgressDialogTask",
//            "Move Class com.github.mobile.ui.ResourceLoadingIndicator moved to com.github.pockethub.ui.ResourceLoadingIndicator",
//            "Move Class com.github.mobile.ui.SelectableLinkMovementMethod moved to com.github.pockethub.ui.SelectableLinkMovementMethod",
//            "Move Class com.github.mobile.ui.SingleChoiceDialogFragment moved to com.github.pockethub.ui.SingleChoiceDialogFragment",
//            "Move Class com.github.mobile.ui.StyledText moved to com.github.pockethub.ui.StyledText",
//            "Move Class com.github.mobile.ui.TabPagerActivity moved to com.github.pockethub.ui.TabPagerActivity",
//            "Move Class com.github.mobile.ui.TabPagerFragment moved to com.github.pockethub.ui.TabPagerFragment",
//            "Move Class com.github.mobile.ui.TextWatcherAdapter moved to com.github.pockethub.ui.TextWatcherAdapter",
//            "Move Class com.github.mobile.ui.ViewPager moved to com.github.pockethub.ui.ViewPager",
//            "Move Class com.github.mobile.ui.WebView moved to com.github.pockethub.ui.WebView",
//            "Move Class com.github.mobile.ui.code.RepositoryCodeFragment moved to com.github.pockethub.ui.code.RepositoryCodeFragment",
//            "Move Class com.github.mobile.ui.comment.CommentListAdapter moved to com.github.pockethub.ui.comment.CommentListAdapter",
//            "Move Class com.github.mobile.ui.comment.CommentPreviewPagerAdapter moved to com.github.pockethub.ui.comment.CommentPreviewPagerAdapter",
//            "Move Class com.github.mobile.ui.comment.CreateCommentActivity moved to com.github.pockethub.ui.comment.CreateCommentActivity",
//            "Move Class com.github.mobile.ui.comment.DeleteCommentListener moved to com.github.pockethub.ui.comment.DeleteCommentListener",
//            "Move Class com.github.mobile.ui.comment.EditCommentListener moved to com.github.pockethub.ui.comment.EditCommentListener",
//            "Move Class com.github.mobile.ui.comment.RawCommentFragment moved to com.github.pockethub.ui.comment.RawCommentFragment",
//            "Move Class com.github.mobile.ui.comment.RenderedCommentFragment moved to com.github.pockethub.ui.comment.RenderedCommentFragment",
//            "Move Class com.github.mobile.ui.commit.CommitCompareListFragment moved to com.github.pockethub.ui.commit.CommitCompareListFragment",
//            "Move Class com.github.mobile.ui.commit.CommitCompareViewActivity moved to com.github.pockethub.ui.commit.CommitCompareViewActivity",
//            "Move Class com.github.mobile.ui.commit.CommitDiffListFragment moved to com.github.pockethub.ui.commit.CommitDiffListFragment",
//            "Move Class com.github.mobile.ui.commit.CommitFileComparator moved to com.github.pockethub.ui.commit.CommitFileComparator",
//            "Move Class com.github.mobile.ui.commit.CommitFileListAdapter moved to com.github.pockethub.ui.commit.CommitFileListAdapter",
//            "Move Class com.github.mobile.ui.commit.CommitFileViewActivity moved to com.github.pockethub.ui.commit.CommitFileViewActivity",
//            "Move Class com.github.mobile.ui.commit.CommitListAdapter moved to com.github.pockethub.ui.commit.CommitListAdapter",
//            "Move Class com.github.mobile.ui.commit.CommitListFragment moved to com.github.pockethub.ui.commit.CommitListFragment",
//            "Move Class com.github.mobile.ui.commit.CommitPagerAdapter moved to com.github.pockethub.ui.commit.CommitPagerAdapter",
//            "Move Class com.github.mobile.ui.commit.CommitViewActivity moved to com.github.pockethub.ui.commit.CommitViewActivity",
//            "Move Class com.github.mobile.ui.commit.CreateCommentActivity moved to com.github.pockethub.ui.commit.CreateCommentActivity",
//            "Move Class com.github.mobile.ui.commit.CreateCommentTask moved to com.github.pockethub.ui.commit.CreateCommentTask",
//            "Move Class com.github.mobile.ui.commit.DiffStyler moved to com.github.pockethub.ui.commit.DiffStyler",
//            "Move Class com.github.mobile.ui.gist.CreateCommentActivity moved to com.github.pockethub.ui.gist.CreateCommentActivity",
//            "Move Class com.github.mobile.ui.gist.CreateCommentTask moved to com.github.pockethub.ui.gist.CreateCommentTask",
//            "Move Class com.github.mobile.ui.gist.CreateGistActivity moved to com.github.pockethub.ui.gist.CreateGistActivity",
//            "Move Class com.github.mobile.ui.gist.CreateGistTask moved to com.github.pockethub.ui.gist.CreateGistTask",
//            "Move Class com.github.mobile.ui.gist.DeleteCommentTask moved to com.github.pockethub.ui.gist.DeleteCommentTask",
//            "Move Class com.github.mobile.ui.gist.DeleteGistTask moved to com.github.pockethub.ui.gist.DeleteGistTask",
//            "Move Class com.github.mobile.ui.gist.EditCommentActivity moved to com.github.pockethub.ui.gist.EditCommentActivity",
//            "Move Class com.github.mobile.ui.gist.EditCommentTask moved to com.github.pockethub.ui.gist.EditCommentTask",
//            "Move Class com.github.mobile.ui.gist.GistFileFragment moved to com.github.pockethub.ui.gist.GistFileFragment",
//            "Move Class com.github.mobile.ui.gist.GistFilesPagerAdapter moved to com.github.pockethub.ui.gist.GistFilesPagerAdapter",
//            "Move Class com.github.mobile.ui.gist.GistFilesViewActivity moved to com.github.pockethub.ui.gist.GistFilesViewActivity",
//            "Move Class com.github.mobile.ui.gist.GistFragment moved to com.github.pockethub.ui.gist.GistFragment",
//            "Move Class com.github.mobile.ui.gist.GistListAdapter moved to com.github.pockethub.ui.gist.GistListAdapter",
//            "Move Class com.github.mobile.ui.gist.GistQueriesPagerAdapter moved to com.github.pockethub.ui.gist.GistQueriesPagerAdapter",
//            "Move Class com.github.mobile.ui.gist.GistsFragment moved to com.github.pockethub.ui.gist.GistsFragment",
//            "Move Class com.github.mobile.ui.gist.GistsPagerAdapter moved to com.github.pockethub.ui.gist.GistsPagerAdapter",
//            "Move Class com.github.mobile.ui.gist.GistsPagerFragment moved to com.github.pockethub.ui.gist.GistsPagerFragment",
//            "Move Class com.github.mobile.ui.gist.GistsViewActivity moved to com.github.pockethub.ui.gist.GistsViewActivity",
//            "Move Class com.github.mobile.ui.gist.MyGistsFragment moved to com.github.pockethub.ui.gist.MyGistsFragment");
//
//          process("https://github.com/Netflix/genie.git", "b77de40c0f3dd43a16f2491558594a61682271fc",
//            "Move Class com.netflix.genie.server.startup.TestGenieApplication moved to com.netflix.genie.web.startup.TestGenieApplication",
//            "Move Class com.netflix.genie.server.health.TestHealthCheck moved to com.netflix.genie.web.health.TestHealthCheck",
//            "Move Class com.netflix.genie.server.startup.GenieApplication moved to com.netflix.genie.web.startup.GenieApplication",
//            "Move Class com.netflix.genie.server.health.HealthCheck moved to com.netflix.genie.web.health.HealthCheck");
//
//          process("https://github.com/wildfly/wildfly.git", "bf35b533f067b51d4c373c5e5124d88525db99f3",
//            "Move Class org.wildfly.extension.batch.jberet.deployment.BatchEnvironmentProcessor.JobXmlFilter moved to org.wildfly.extension.batch.jberet.deployment.WildFlyJobXmlResolver.JobXmlFilter");
//
//          process("https://github.com/xetorthio/jedis.git", "6c3dde45e8cbd0c1fa73072fad7610275afc6240",
//            "Move Class redis.clients.jedis.SentinelCommands moved to redis.clients.jedis.commands.SentinelCommands",
//            "Move Class redis.clients.jedis.ScriptingCommandsPipeline moved to redis.clients.jedis.commands.ScriptingCommandsPipeline",
//            "Move Class redis.clients.jedis.Commands moved to redis.clients.jedis.commands.Commands",
//            "Move Class redis.clients.jedis.ClusterPipeline moved to redis.clients.jedis.commands.ClusterPipeline",
//            "Move Class redis.clients.jedis.ClusterCommands moved to redis.clients.jedis.commands.ClusterCommands",
//            "Move Class redis.clients.jedis.BinaryScriptingCommandsPipeline moved to redis.clients.jedis.commands.BinaryScriptingCommandsPipeline",
//            "Move Class redis.clients.jedis.BinaryScriptingCommands moved to redis.clients.jedis.commands.BinaryScriptingCommands",
//            "Move Class redis.clients.jedis.BinaryRedisPipeline moved to redis.clients.jedis.commands.BinaryRedisPipeline",
//            "Move Class redis.clients.jedis.BinaryJedisCommands moved to redis.clients.jedis.commands.BinaryJedisCommands",
//            "Move Class redis.clients.jedis.BinaryJedisClusterCommands moved to redis.clients.jedis.commands.BinaryJedisClusterCommands",
//            "Move Class redis.clients.jedis.BasicRedisPipeline moved to redis.clients.jedis.commands.BasicRedisPipeline",
//            "Move Class redis.clients.jedis.BasicCommands moved to redis.clients.jedis.commands.BasicCommands",
//            "Move Class redis.clients.jedis.AdvancedJedisCommands moved to redis.clients.jedis.commands.AdvancedJedisCommands",
//            "Move Class redis.clients.jedis.AdvancedBinaryJedisCommands moved to redis.clients.jedis.commands.AdvancedBinaryJedisCommands",
//            "Move Class redis.clients.jedis.JedisClusterBinaryScriptingCommands moved to redis.clients.jedis.commands.JedisClusterBinaryScriptingCommands",
//            "Move Class redis.clients.jedis.JedisClusterCommands moved to redis.clients.jedis.commands.JedisClusterCommands",
//            "Move Class redis.clients.jedis.JedisClusterScriptingCommands moved to redis.clients.jedis.commands.JedisClusterScriptingCommands",
//            "Move Class redis.clients.jedis.JedisCommands moved to redis.clients.jedis.commands.JedisCommands",
//            "Move Class redis.clients.jedis.MultiKeyBinaryCommands moved to redis.clients.jedis.commands.MultiKeyBinaryCommands",
//            "Move Class redis.clients.jedis.MultiKeyBinaryJedisClusterCommands moved to redis.clients.jedis.commands.MultiKeyBinaryJedisClusterCommands",
//            "Move Class redis.clients.jedis.MultiKeyBinaryRedisPipeline moved to redis.clients.jedis.commands.MultiKeyBinaryRedisPipeline",
//            "Move Class redis.clients.jedis.MultiKeyCommands moved to redis.clients.jedis.commands.MultiKeyCommands",
//            "Move Class redis.clients.jedis.MultiKeyCommandsPipeline moved to redis.clients.jedis.commands.MultiKeyCommandsPipeline",
//            "Move Class redis.clients.jedis.MultiKeyJedisClusterCommands moved to redis.clients.jedis.commands.MultiKeyJedisClusterCommands",
//            "Move Class redis.clients.jedis.ProtocolCommand moved to redis.clients.jedis.commands.ProtocolCommand",
//            "Move Class redis.clients.jedis.ScriptingCommands moved to redis.clients.jedis.commands.ScriptingCommands");
//		
//	}

    public void processTp(String cloneUrl, String commitId, String ... refactorings) {
        process(true, cloneUrl, commitId, refactorings);
    }

    public void processFp(String cloneUrl, String commitId, String ... refactorings) {
        process(false, cloneUrl, commitId, refactorings);
    }

    public void process(String cloneUrl, String commitId, String ... refactorings) {
        process(true, cloneUrl, commitId, refactorings);
    }
    
	public void process(boolean tp, String cloneUrl, String commitId, String ... refactorings) {
//        if (tp) {
//            return;
//        }
	    
	    GitService gitService = new GitServiceImpl();
        String folder = tempDir + "/" + cloneUrl.substring(cloneUrl.lastIndexOf('/') + 1, cloneUrl.lastIndexOf('.'));
        //File projectFolder = new File(folder);
        
        Set<String> mcrs = new HashSet<String>();
        Set<String> result = new HashSet<String>();
        for (String refactoring : refactorings) {
            RefactoringType refType = extractRefactoringType(refactoring);
            if (refType == RefactoringType.MOVE_CLASS/* && tp*/) {
                String from = getParent(RefactoringType.MOVE_CLASS.getGroup(refactoring, 1));
                String to = getParent(RefactoringType.MOVE_CLASS.getGroup(refactoring, 2));
                boolean fromIsPackage = isPackageName(from);
                boolean toIsPackage = isPackageName(to);
                if (fromIsPackage && toIsPackage && !from.equals(to)) {
                    mcrs.add(normalize(refactoring));
                } else {
                    result.add(normalize(refactoring));
                }
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
                    if (fromIsPackage && toIsPackage && !existsBefore && !existsAfter) {
                        // rename
                        result.add("Rename Package " + from + " to " + to);
                    } else {
//                        System.out.println(String.format("not rename %b %b %b %b", fromIsPackage, toIsPackage, existsBefore, existsAfter));
                        result.add(refactoring);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println(String.format("insert into nrefactoring(cloneUrl, commit, refactoringType, description, truePositive) values "));
        for (Iterator<String> iter = result.iterator(); iter.hasNext();) {
            String refactoring = iter.next();
            //                System.out.println(refactoring);
            RefactoringType refType = extractRefactoringType(refactoring);
//            System.out.println(String.format("insert into nrefactoring(project, commit, refType, description, tp) values ('%s', '%s', '%s', '%s', '%d');", cloneUrl, commitId, refType.getDisplayName(), refactoring, tp ? 1 : 0));
            System.out.print(String.format("  ('%s', '%s', '%s', '%s', '%d')", cloneUrl, commitId, refType.getDisplayName(), refactoring, tp ? 1 : 0));
            System.out.println(iter.hasNext() ? "," : ";");
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
