package br.ufmg.dcc.labsoft.refdetector.model.builder;

import org.junit.Test;

import br.ufmg.dcc.labsoft.refactoringanalyzer.TestBuilder;

public class TestRefDetector2 {

	@Test
	public void testExtractMethod() throws Exception {
		TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");

		test.project("https://github.com/jMonkeyEngine/jmonkeyengine.git", "master")
		.atCommit("5989711f7315abe4c3da0f1516a3eb3a81da6716").contains(
		  "Extract Method protected movePanel(xoffset int, yoffset int) : void extracted from public mouseDragged(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel",
		  "Extract Method protected saveLocation() : void extracted from public mousePressed(e MouseEvent) : void in class com.jme3.gde.materialdefinition.editor.DraggablePanel"
		)
		.atCommit("5989711f7315abe4c3da0f1516a3eb3a81da6716").notContains(
		  "Extract Method protected removeSelectedConnection(selectedItem Selectable) : void extracted from protected removeSelectedConnection() : void in class com.jme3.gde.materialdefinition.editor.Diagram");

		test.project("https://github.com/Graylog2/graylog2-server.git", "master")
		.atCommit("2d98ae165ea43e9a1ac6a905d6094f077abb2e55").contains(
		  "Extract Method private dispatchMessage(msg Message) : void extracted from public onEvent(event MessageEvent) : void in class org.graylog2.shared.buffers.processors.ProcessBufferProcessor",
		  "Extract Method private postProcessMessage(raw RawMessage, codec Codec, inputIdOnCurrentNode String, baseMetricName String, message Message, decodeTime long) : Message extracted from private processMessage(raw RawMessage) : Message in class org.graylog2.shared.buffers.processors.DecodingProcessor")
		.atCommit("2d98ae165ea43e9a1ac6a905d6094f077abb2e55").notContains();

		test.project("https://github.com/aws/aws-sdk-java.git", "master")
		.atCommit("4baf0a4de8d03022df48d696d210cc8b3117d38a").contains(
		  "Extract Method private pause(delay long) : void extracted from private pauseExponentially(retries int) : void in class com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper",
		  "Inline Method private killServer() : void inlined to public cleanUp() : void in class com.amazonaws.util.EC2MetadataUtilsTest")
		.atCommit("4baf0a4de8d03022df48d696d210cc8b3117d38a").notContains();

		test.project("https://github.com/grails/grails-core.git", "master")
		.atCommit("480537e0f8aaf50a7648bf445b33230aa32a9b44").contains(
		  "Extract Method public weaveMixinsIntoClass(classNode ClassNode, values ListExpression, applicationClassNode ClassNode) : void extracted from public weaveMixinsIntoClass(classNode ClassNode, values ListExpression) : void in class org.grails.compiler.injection.test.TestMixinTransformation",
		  "Extract Method public weaveTestMixins(classNode ClassNode, values ListExpression, applicationClassNode ClassNode) : void extracted from public weaveTestMixins(classNode ClassNode, values ListExpression) : void in class org.grails.compiler.injection.test.TestMixinTransformation")
		.atCommit("480537e0f8aaf50a7648bf445b33230aa32a9b44").notContains(
		  "Extract Method protected weaveMixinIntoClass(classNode ClassNode, mixinClassNode ClassNode, junit3MethodHandler Junit3TestFixtureMethodHandler, applicationClassNode ClassNode) : void extracted from protected weaveMixinIntoClass(classNode ClassNode, mixinClassNode ClassNode, junit3MethodHandler Junit3TestFixtureMethodHandler) : void in class org.grails.compiler.injection.test.TestMixinTransformation");

		test.project("https://github.com/apache/cassandra.git", "master")
		.atCommit("b70f7ea0ce27b5defa0a7773d448732364e7aee0").contains(
	      "Extract Method private listSnapshots() : List<File> extracted from public getSnapshotDetails() : Map<String,Pair<Long,Long>> in class org.apache.cassandra.db.Directories")
		.atCommit("b70f7ea0ce27b5defa0a7773d448732364e7aee0").notContains();

		test.project("https://github.com/apache/cassandra.git", "master")
		.atCommit("ec52e77ecde749e7c5a483b26cbd8041f2a5a33c").contains(
		  "Extract Method public submitBackground(cfs ColumnFamilyStore, autoFill boolean) : List<Future<?>> extracted from public submitBackground(cfs ColumnFamilyStore) : List<Future<?>> in class org.apache.cassandra.db.compaction.CompactionManager")
		.atCommit("ec52e77ecde749e7c5a483b26cbd8041f2a5a33c").notContains();

		test.project("https://github.com/orientechnologies/orientdb.git", "master")
		.atCommit("31f0b5a8f3f0f2cfc7e33c810116a904381ee98c").contains()
		.atCommit("31f0b5a8f3f0f2cfc7e33c810116a904381ee98c").notContains(
		  "Extract Method private calculateMatch(estimatedRootEntries Map<String,Long>, matchContext MatchContext, aliasClasses Map<String,String>, aliasFilters Map<String,OWhereClause>) : OResultSet extracted from public execute(iArgs Map<Object,Object>) : Object in class com.orientechnologies.orient.core.sql.parser.OMatchStatement");

		test.project("https://github.com/apache/hive.git", "master")
		.atCommit("5f78f9ef1e6c798849d34cc66721e6c1d9709b6f").contains(
		  "Extract Method package generateSplitsInfo(conf Configuration, numSplits int) : List<OrcSplit> extracted from package generateSplitsInfo(conf Configuration) : List<OrcSplit> in class org.apache.hadoop.hive.ql.io.orc.OrcInputFormat")
		.atCommit("5f78f9ef1e6c798849d34cc66721e6c1d9709b6f").notContains();

		test.project("https://github.com/bitcoinj/bitcoinj.git", "master")
		.atCommit("2fd96c777164dd812e8b5a4294b162889601df1d").contains(
		  "Extract Method public newSha256Digest() : MessageDigest extracted from public sha256hash160(input byte[]) : byte[] in class org.bitcoinj.core.Utils")
		.atCommit("2fd96c777164dd812e8b5a4294b162889601df1d").notContains();
		
		// NullPointerException
//		test.project("https://github.com/crate/crate.git", "master") 
//        .atCommit("c7b6a7aa878aabd6400d2df0490e1eb2b810c8f9").contains(
//          "Extract Method public plan(relation AnalyzedRelation, consumerContext ConsumerContext) : PlannedAnalyzedRelation extracted from public plan(rootRelation AnalyzedRelation, plannerContext Context) : Plan in class io.crate.planner.consumer.ConsumingPlanner");

        test.project("https://github.com/k9mail/k-9.git", "master") 
        .atCommit("23c49d834d3859fc76a604da32d1789d2e863303").contains(
          "Extract Method private setNotificationContent(context Context, message Message, sender CharSequence, subject CharSequence, builder Builder, accountDescr String) : Builder extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController",
          "Extract Method private buildNotificationNavigationStack(context Context, account Account, message LocalMessage, newMessages int, unreadCount int, allRefs ArrayList<MessageReference>) : TaskStackBuilder extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController")
        .atCommit("23c49d834d3859fc76a604da32d1789d2e863303").notContains(
         // isn't it correct?
            "Extract Method private addWearActions(builder Builder, totalMsgCount int, msgCount int, account Account, allRefs ArrayList<MessageReference>, messages List<? extends Message>, notificationID int) : void extracted from private notifyAccountWithDataLocked(context Context, account Account, message LocalMessage, data NotificationData) : void in class com.fsck.k9.controller.MessagingController");

        test.project("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "master") 
        .atCommit("9687048f76519fc89b4151cbe2841bbba61a401d").contains(
          "Extract Method protected getEntityForm(info DynamicEntityFormInfo, dynamicFormOverride EntityForm) : EntityForm extracted from protected getBlankDynamicFieldTemplateForm(info DynamicEntityFormInfo, dynamicFormOverride EntityForm) : EntityForm in class org.broadleafcommerce.openadmin.web.controller.AdminAbstractController");

        test.project("https://github.com/jOOQ/jOOQ.git", "master") 
        .atCommit("227254cf769f3e821ed1b2ef2d88c4ec6b20adea").contains(
            // 4 fn due to similarity < 0.5
          "Extract Method public formatCSV(writer Writer, header boolean, delimiter char, nullString String) : void extracted from public formatCSV(writer Writer, delimiter char, nullString String) : void in class org.jooq.impl.ResultImpl",
          "Extract Method public formatCSV(stream OutputStream, header boolean, delimiter char, nullString String) : void extracted from public formatCSV(stream OutputStream, delimiter char, nullString String) : void in class org.jooq.impl.ResultImpl",
          "Extract Method public formatCSV(header boolean, delimiter char, nullString String) : String extracted from public formatCSV(delimiter char, nullString String) : String in class org.jooq.impl.ResultImpl",
          "Extract Method public formatCSV(writer Writer, header boolean, delimiter char) : void extracted from public formatCSV(writer Writer, delimiter char) : void in class org.jooq.impl.ResultImpl",
          "Extract Method public formatCSV(header boolean, delimiter char) : String extracted from public formatCSV(delimiter char) : String in class org.jooq.impl.ResultImpl",
          "Extract Method public formatCSV(writer Writer, header boolean) : void extracted from public formatCSV(writer Writer) : void in class org.jooq.impl.ResultImpl",
          "Extract Method public formatCSV(stream OutputStream, header boolean) : void extracted from public formatCSV(stream OutputStream) : void in class org.jooq.impl.ResultImpl",
          "Extract Method public formatCSV(header boolean) : String extracted from public formatCSV() : String in class org.jooq.impl.ResultImpl");

        test.project("https://github.com/springfox/springfox.git", "master") 
        .atCommit("1bf09d2da6e6123d84ad872aea71c71f98646dfc").notContains(
          "Extract Method private getResourceDescription(requestMappings List<RequestMappingContext>, context DocumentationContext) : String extracted from public scan(context DocumentationContext) : ApiListingReferenceScanResult in class springfox.documentation.spring.web.scanners.ApiListingReferenceScanner");

        test.project("https://github.com/apache/pig.git", "master") 
        .atCommit("7a1659c12d76b510809dea1dea1f5100bcf4cd60").contains(
          "Extract Method private initialize() : void extracted from public launchPig(physicalPlan PhysicalPlan, grpName String, pigContext PigContext) : PigStats in class org.apache.pig.backend.hadoop.executionengine.spark.SparkLauncher");
		
		test.assertExpectations();
	}
	
	@Test
	public void testMoveClass() throws Exception {
	    TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");
	    
	    test.project("https://github.com/ignatov/intellij-erlang.git", "master")
	    .atCommit("c0ceabc5e9e47c628f041b72c1ca3dafb876f3b4").contains()
	    .atCommit("c0ceabc5e9e47c628f041b72c1ca3dafb876f3b4").notContains(
	        // isn't it correct?
	      "Move Class org.intellij.erlang.compilation.ErlangCompilationTestCase.ErlangModuleTextBuilder.ParseTransformBuilder moved to org.intellij.erlang.compilation.ErlangModuleTextGenerator.ParseTransformBuilder");

	    // missing object
//	    test.project("https://github.com/spring-projects/spring-data-jpa.git", "master")
//	    .atCommit("36d1b0717bc5836bba39985caadc2df5f2533ac4").contains(
//	      "Move Class org.springframework.data.jpa.repository.augment.JpaSoftDeleteQueryAugmentor.PropertyChangeEnsuringBeanWrapper moved to org.springframework.data.jpa.repository.augment.PropertyChangeEnsuringBeanWrapper")
//	    .atCommit("36d1b0717bc5836bba39985caadc2df5f2533ac4").notContains();

	    test.project("https://github.com/greenrobot/greenDAO.git", "master")
	    .atCommit("f935cc5f17d1e47c8597ee09161d524c80c64125").contains()
	    .atCommit("f935cc5f17d1e47c8597ee09161d524c80c64125").notContains(
	      "Move Class de.greenrobot.daogenerator.gentest.TestDaoGenerator moved to de.greenrobot.daogenerator.gentest.TestDaoGenerator");

	    test.project("https://github.com/SonarSource/sonarqube.git", "master")
	    .atCommit("091ec857d24bfe139d2a5ce143ffc9b32b21cd7c").contains(
	      "Move Class org.sonar.core.component.SnapshotQueryTest moved to org.sonar.core.component.db.SnapshotQueryTest",
	      "Move Class org.sonar.core.component.SnapshotQuery moved to org.sonar.core.component.db.SnapshotQuery")
	    .atCommit("091ec857d24bfe139d2a5ce143ffc9b32b21cd7c").notContains();

	    test.project("https://github.com/checkstyle/checkstyle.git", "master")
	    .atCommit("8a49fd50ee5dbadc22b7d557b9ef8a6e212b0f78").contains()
	    .atCommit("8a49fd50ee5dbadc22b7d557b9ef8a6e212b0f78").notContains(
	      "Move Class InputRedundantImportCheck_UnnamedPackage moved to InputRedundantImportCheck_UnnamedPackage");
	    
	    test.assertExpectations();
	}
	
	@Test
	public void testMoveMethod() throws Exception {
	    TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");
	    
	    test.project("https://github.com/apache/hive.git", "master") 
	    .atCommit("0dece6f37d43ae6ecbd0ad496ab18bcdd505a395").notContains(
	      "Move Method public setIndex(index Index) : void from class org.apache.hadoop.hive.ql.session.LineageState to public setIndex(depMap Index) : void from class org.apache.hadoop.hive.ql.hooks.HookContext");

	    test.project("https://github.com/google/j2objc.git", "master") 
	    .atCommit("45cafde754d9f99a74f3ae723233e351d0f78642").notContains(
	      // Duplicate entity key: com.google.devtools.j2objc.ast.LambdaExpression#(LambdaExpression) 
	        "Move Method public visit(node LambdaExpression) : boolean from class com.google.devtools.j2objc.translate.Rewriter to public visit(node LambdaExpression) : boolean from class com.google.devtools.j2objc.translate.OuterReferenceResolver");

	    test.project("https://github.com/petrnohejl/Android-Templates-And-Utilities.git", "master") 
	    .atCommit("d3d5c74917f7974f7f910067ce3b60d5dccf5fb6").notContains(
	      // Duplicate entity key: com.example.fragment.ExampleFragment
	        "Move Method public onCreate(savedInstanceState Bundle) : void from class com.example.fragment.ExampleFragment to public onCreate(savedInstanceState Bundle) : void from class com.example.fragment.ListingFragment");

	    test.project("https://github.com/jMonkeyEngine/jmonkeyengine.git", "master") 
	    .atCommit("7f2c7c5d356acc350e705168e972112bfb151e83").notContains(
	      "Move Method public getAngularFactor() : Vector3f from class com.jme3.bullet.objects.PhysicsRigidBody to public getAngularFactor(store Vector3f) : Vector3f from class com.jme3.bullet.objects.PhysicsRigidBody");

	    test.project("https://github.com/apache/hive.git", "master") 
	    .atCommit("240097b78b70172e1cf9bc37876a566ddfb9e115").contains(
	      "Move Method public getAcidEventFields() : List<String> from class org.apache.hadoop.hive.ql.io.orc.OrcRecordUpdater to package getAcidEventFields() : List<String> from class org.apache.hadoop.hive.ql.io.orc.RecordReaderFactory");

	    test.project("https://github.com/apache/hive.git", "master") 
	    .atCommit("102b23b16bf26cbf439009b4b95542490a082710").contains(
	      "Move Method private isSourceCMD(cmd String) : boolean from class org.apache.hive.beeline.BeeLine to private isSourceCMD(cmd String) : boolean from class org.apache.hive.beeline.Commands",
	      "Move Method private getFirstCmd(cmd String, length int) : String from class org.apache.hive.beeline.BeeLine to private getFirstCmd(cmd String, length int) : String from class org.apache.hive.beeline.Commands",
	      "Extract Method public handleMultiLineCmd(line String) : String extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands",
	      "Extract Method private executeInternal(sql String, call boolean) : boolean extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands")
	    .atCommit("102b23b16bf26cbf439009b4b95542490a082710").notContains(
	      "Extract Method private sourceFile(cmd String) : boolean extracted from private execute(line String, call boolean, entireLineAsCommand boolean) : boolean in class org.apache.hive.beeline.Commands");

	    test.project("https://github.com/hazelcast/hazelcast.git", "master") 
	    .atCommit("e84e96ff5c2bdc48cea7f75fd794506159c4e1f7").contains(
	      "Move Attribute public DATA_FULL_NAME : String from class com.hazelcast.client.protocol.generator.CodecModel to class com.hazelcast.client.protocol.generator.CodeGenerationUtils",
	      "Move Method public convertTypeToCSharp(type String) : String from class com.hazelcast.client.protocol.generator.CodecModel.ParameterModel to public convertTypeToCSharp(type String) : String from class com.hazelcast.client.protocol.generator.CodeGenerationUtils",
	      "Extract Method private createCodecModel(methodElement ExecutableElement, lang Lang) : CodecModel extracted from public generateCodec(methodElement ExecutableElement, lang Lang) : void in class com.hazelcast.client.protocol.generator.CodecCodeGenerator");

	    test.project("https://github.com/cwensel/cascading.git", "master") 
	    .atCommit("f9d3171f5020da5c359cdda28ef05172e858c464").contains(
	      // Duplicate entity key: cascading.stats.hadoop.HadoopNodeStats
	      "Move Method private getPrefix() : String from class cascading.stats.tez.TezNodeStats to private getPrefix() : String from class cascading.stats.CascadingStats",
	      "Move Method protected logWarn(message String, arguments Object) : void from class cascading.stats.tez.TezNodeStats to protected logWarn(message String, arguments Object) : void from class cascading.stats.CascadingStats",
	      "Move Method protected logDebug(message String, arguments Object) : void from class cascading.stats.tez.TezNodeStats to protected logDebug(message String, arguments Object) : void from class cascading.stats.CascadingStats",
	      "Move Method protected logInfo(message String, arguments Object) : void from class cascading.stats.tez.TezNodeStats to protected logInfo(message String, arguments Object) : void from class cascading.stats.CascadingStats",
	      "Move Attribute private prefixID : String from class cascading.stats.tez.TezNodeStats to class cascading.stats.CascadingStats")
	    .atCommit("f9d3171f5020da5c359cdda28ef05172e858c464").notContains(
	      "Inline Method private getJobStatusClient() : RunningJob inlined to protected captureChildDetailInternal() : boolean in class cascading.stats.hadoop.HadoopNodeStats");

	    test.project("https://github.com/Graylog2/graylog2-server.git", "master") 
	    .atCommit("767171c90110c4c5781e8f6d19ece1fba0d492e9").contains(
	      "Move Method public testTimestampStatsOfIndexWithNonExistingIndex() : void from class org.graylog2.indexer.searches.SearchesTest to public testTimestampStatsOfIndexWithNonExistingIndex() : void from class org.graylog2.indexer.ranges.EsIndexRangeServiceTest",
	      "Move Method public testTimestampStatsOfIndexWithEmptyIndex() : void from class org.graylog2.indexer.searches.SearchesTest to public testTimestampStatsOfIndexWithEmptyIndex() : void from class org.graylog2.indexer.ranges.EsIndexRangeServiceTest",
	      "Move Method public testTimestampStatsOfIndex() : void from class org.graylog2.indexer.searches.SearchesTest to public testTimestampStatsOfIndex() : void from class org.graylog2.indexer.ranges.EsIndexRangeServiceTest",
	      "Move Method public timestampStatsOfIndex(index String) : TimestampStats from class org.graylog2.indexer.searches.Searches to protected timestampStatsOfIndex(index String) : TimestampStats from class org.graylog2.indexer.ranges.EsIndexRangeService");
	    
	    test.assertExpectations();
	}
	
	@Test
    public void testInlineMethod() throws Exception {
        TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");
        
        test.project("https://github.com/droolsjbpm/jbpm.git", "master") 
        .atCommit("3815f293ba9338f423315d93a373608c95002b15").contains(
          "Extract Superclass org.jbpm.process.audit.JPAService from classes [org.jbpm.process.audit.JPAAuditLogService]")
        .atCommit("3815f293ba9338f423315d93a373608c95002b15").notContains(
          "Extract Method private getOrderByListId(field OrderBy) : String extracted from public language(language String) : TaskQueryBuilder in class org.jbpm.services.task.impl.TaskQueryBuilderImpl",
          "Inline Method private resetGroup() : void inlined to public clear() : void in class org.jbpm.query.jpa.data.QueryWhere",
          "Move Method private joinTransaction(em EntityManager) : Object from class org.jbpm.process.audit.JPAAuditLogService to protected joinTransaction(em EntityManager) : Object from class org.jbpm.executor.impl.jpa.ExecutorJPAAuditService",
          "Move Method private getEntityManager() : EntityManager from class org.jbpm.process.audit.JPAAuditLogService to protected getEntityManager() : EntityManager from class org.jbpm.executor.impl.jpa.ExecutorJPAAuditService");

        test.project("https://github.com/HubSpot/Singularity.git", "master") 
        .atCommit("f06f7ab4b898a97e8af8a47c0164205c96992d05").notContains(
          "Inline Method private addContinuation(existingHandler SingularityS3DownloaderAsyncHandler) : boolean inlined to private addDownloadRequest() : boolean in class com.hubspot.singularity.s3downloader.server.SingularityS3DownloaderCoordinator.DownloadJoiner",
          "Inline Method private completeContinuations() : void inlined to public run() : void in class com.hubspot.singularity.s3downloader.server.SingularityS3DownloaderAsyncHandler");

        // slow
//        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
//        .atCommit("8d7a26edd1fedb9505b4f2b4fe57b2d2958b4dd9").contains(
//          "Inline Method private writeContentToFile(revision byte[]) : void inlined to private write(revision byte[]) : void in class com.intellij.openapi.vcs.history.FileHistoryPanelImpl.MyGetVersionAction");

        // missing object
//        test.project("https://github.com/wordpress-mobile/WordPress-Android.git", "master") 
//        .atCommit("2252ed3754bff8c39db48d172ac76ac5a4e15359").contains(
//          "Inline Method private shouldShowTagToolbar() : boolean inlined to public onCreateView(inflater LayoutInflater, container ViewGroup, savedInstanceState Bundle) : View in class org.wordpress.android.ui.reader.ReaderPostListFragment");

        // duplicate entity key com.google.common.collect.ImmutableBiMapTest
//        test.project("https://github.com/google/guava.git", "master") 
//        .atCommit("6640609a10c02ad8708cff784417a47a9b4006a4").notContains(
//          "Inline Method public subMap(fromKey K, fromInclusive boolean, toKey K, toInclusive boolean) : ImmutableSortedMap<K,V> inlined to public subMap(fromKey K, toKey K) : ImmutableSortedMap<K,V> in class com.google.common.collect.ImmutableSortedMap",
//          "Inline Method private copyOfEnumMap(original EnumMap<K,? extends V>) : ImmutableMap<K,V> inlined to public copyOf(map Map<? extends K,? extends V>) : ImmutableMap<K,V> in class com.google.common.collect.ImmutableMap",
//          "Extract Method package fromEntryList(entries Collection<? extends Entry<? extends K,? extends V>>) : ImmutableMap<K,V> extracted from public copyOf(entries Iterable<? extends Entry<? extends K,? extends V>>) : ImmutableMap<K,V> in class com.google.common.collect.ImmutableMap");

        test.project("https://github.com/ignatov/intellij-erlang.git", "master") 
        .atCommit("3855f0ca82795f7481b34342c7d9e5644a1d42c3").contains(
          // fn similarity threshold 0.35714285714285715
          "Inline Method private getModuleFileName() : String inlined to public resolve() : PsiElement in class org.intellij.erlang.psi.impl.ErlangFunctionReferenceImpl");

        // slow
//        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
//        .atCommit("e3993ff8c50ef4459389326bca048fd1ed3a73a4").notContains(
//          "Inline Method private calcEffectivePlatformCp(platformCp Collection<File>, chunkSdkVersion int, options List<String>, compilingTool JavaCompilingTool) : Collection<File> inlined to private compileJava(context CompileContext, chunk ModuleChunk, files Collection<File>, classpath Collection<File>, platformCp Collection<File>, sourcePath Collection<File>, diagnosticSink DiagnosticOutputConsumer, outputSink OutputFileConsumer, compilingTool JavaCompilingTool) : boolean in class org.jetbrains.jps.incremental.java.JavaBuilder");

        test.project("https://github.com/Atmosphere/atmosphere.git", "master") 
        .atCommit("7ab0e6becfc4acf27783092945eb67d9fdb0474d").notContains(
          "Inline Method protected needInjection() : void inlined to public configure(config AtmosphereConfig) : void in class org.atmosphere.config.managed.ServiceInterceptor");

        // duplicate entity key .c
//        test.project("https://github.com/JetBrains/intellij-community.git", "master") 
//        .atCommit("526af6f1c1206b7f278459757f4b4b22b18069ea").notContains(
//          "Inline Method private convertToJavaLambda(expression PsiExpression, streamApiMethodName String) : PsiExpression inlined to public convertToStream(expression PsiMethodCallExpression, method PsiMethod, force boolean) : PsiExpression in class com.intellij.codeInspection.java18StreamApi.PseudoLambdaReplaceTemplate");

        test.project("https://github.com/checkstyle/checkstyle.git", "master") 
        .atCommit("a07cae0aca9f9072256b3a5fd05779e8d69b9748").contains(
          "Inline Method private leaveLiteralTry() : void inlined to public leaveToken(literalTry DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedTryDepthCheck",
          "Inline Method private visitLiteralTry(literalTry DetailAST) : void inlined to public visitToken(literalTry DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedTryDepthCheck",
          "Inline Method private leaveLiteralIf(literalIf DetailAST) : void inlined to public leaveToken(literalIf DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedIfDepthCheck",
          "Inline Method private visitLiteralIf(literalIf DetailAST) : void inlined to public visitToken(literalIf DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.coding.NestedIfDepthCheck");
        
        test.assertExpectations();
    }
	
	// -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=myrecording.jfr
	@Test
	public void testPullUpMethod() throws Exception {
	    TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");
	    
	    // org.eclipse.jgit.errors.MissingObjectException: Missing unknown 4ef35268bb96bb78b2dc698fa68e7ce763cde32e
//	    test.project("https://github.com/BroadleafCommerce/BroadleafCommerce.git", "master") 
//	    .atCommit("4ef35268bb96bb78b2dc698fa68e7ce763cde32e").contains(
//	      "Pull Up Method public setColumn(column Integer) : void from class org.broadleafcommerce.openadmin.dto.BasicFieldMetadata to public setColumn(column Integer) : void from class org.broadleafcommerce.openadmin.dto.FieldMetadata",
//	      "Pull Up Method public getColumn() : Integer from class org.broadleafcommerce.openadmin.dto.BasicFieldMetadata to public getColumn() : Integer from class org.broadleafcommerce.openadmin.dto.FieldMetadata");

	    // Duplicate entity key: org.quantumbadger.redreader.activities.MainActivity#onSortSelected(Sort)
//	    test.project("https://github.com/QuantumBadger/RedReader.git", "master") 
//	    .atCommit("51b8b0e1ad4be1b137d67774eab28dc0ef52cb0a").contains(
//	      "Pull Up Method public onSharedPreferenceChanged(prefs SharedPreferences, key String) : void from class org.quantumbadger.redreader.activities.MainActivity to protected onSharedPreferenceChangedInner(prefs SharedPreferences, key String) : void from class org.quantumbadger.redreader.activities.RefreshableActivity");

	    test.project("https://github.com/google/j2objc.git", "master") 
	    .atCommit("d8172732456b8be61bc12b74322ecbe9a958f301").notContains(
	      "Pull Up Method private needsCompanionClassDeclaration() : boolean from class com.google.devtools.j2objc.gen.TypeDeclarationGenerator to private hasStaticAccessorMethods() : boolean from class com.google.devtools.j2objc.gen.TypeGenerator");

	    test.project("https://github.com/raphw/byte-buddy.git", "master") 
	    .atCommit("f1dfb66a368760e77094ac1e3860b332cf0e4eb5").contains(
	      "Pull Up Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.Explicit to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase");
	      // same ref
	      //"Pull Up Method protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.ForLoadedExecutable to protected wrap(values List<ParameterDescription>) : ParameterList from class net.bytebuddy.description.method.ParameterList.AbstractBase");

	    test.project("https://github.com/fabric8io/fabric8.git", "master") 
	    .atCommit("07807aed847e1d0589c094461544e54a2677cbf5").contains(
	      "Pull Up Method private hasKubernetesJson(f File) : boolean from class io.fabric8.maven.ApplyMojo to package hasKubernetesJson(f File) : boolean from class io.fabric8.maven.AbstractFabric8Mojo",
	      "Pull Up Method protected isKubernetesJsonArtifact(classifier String, type String) : boolean from class io.fabric8.maven.JsonMojo to package isKubernetesJsonArtifact(classifier String, type String) : boolean from class io.fabric8.maven.AbstractFabric8Mojo",
	      "Pull Up Method private getDependencies() : Set<File> from class io.fabric8.maven.ApplyMojo to package getDependencies() : Set<File> from class io.fabric8.maven.AbstractFabric8Mojo");

	    test.project("https://github.com/spring-projects/spring-roo.git", "master") 
	    .atCommit("0bb4cca1105fc6eb86e7c4b75bfff3dbbd55f0c8").contains(
	      "Pull Up Method public setGenericDefinition(definition String) : void from class org.springframework.roo.classpath.details.MethodMetadataBuilder to public setGenericDefinition(genericDefinition String) : void from class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder",
	      "Pull Up Method public getGenericDefinition() : String from class org.springframework.roo.classpath.details.MethodMetadataBuilder to public getGenericDefinition() : String from class org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder");

	    // org.eclipse.jgit.errors.MissingObjectException: Missing unknown e2de877a29217a50afbd142454a330e423d86045
//	    test.project("https://github.com/VoltDB/voltdb.git", "master") 
//	    .atCommit("e2de877a29217a50afbd142454a330e423d86045").contains(
//	      "Pull Up Method private findAllAggPlanNodes(node AbstractPlanNode) : List<AbstractPlanNode> from class org.voltdb.planner.TestPlansApproxCountDistinct to protected findAllAggPlanNodes(fragment AbstractPlanNode) : List<AbstractPlanNode> from class org.voltdb.planner.PlannerTestCase");

	    // slow
	    test.project("https://github.com/JetBrains/intellij-community.git", "master") 
	    .atCommit("33b0ac3a029845f9c20f7f5967c03b31b24f3b4b").contains(
	      "Pull Up Method private iterateRecursively(root VirtualFile, processor ContentIterator, indicator ProgressIndicator, visitedRoots Set<VirtualFile>, projectFileIndex ProjectFileIndex) : void from class com.intellij.util.indexing.FileBasedIndexImpl to public iterateRecursively(root VirtualFile, processor ContentIterator, indicator ProgressIndicator, visitedRoots Set<VirtualFile>, projectFileIndex ProjectFileIndex) : void from class com.intellij.util.indexing.FileBasedIndex");

	    // org.eclipse.jgit.errors.MissingObjectException: Missing unknown 72b5348307d86b1a118e546c24d97f1ac1895bdb
//	    test.project("https://github.com/crate/crate.git", "master") 
//	    .atCommit("72b5348307d86b1a118e546c24d97f1ac1895bdb").contains(
//	      "Pull Up Method public downstreamExecutionNodeId(downstreamExecutionNodeId int) : void from class io.crate.planner.node.dql.MergeNode to public downstreamExecutionNodeId(downstreamExecutionNodeId int) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Method public downstreamNodes(nodes Set<String>) : void from class io.crate.planner.node.dql.join.NestedLoopNode to public downstreamNodes(downStreamNodes Set<String>) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Method public downstreamNodes(nodes Set<String>) : void from class io.crate.planner.node.dql.MergeNode to public downstreamNodes(downStreamNodes Set<String>) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Method public downstreamNodes(downStreamNodes List<String>) : void from class io.crate.planner.node.dql.CollectNode to public downstreamNodes(downStreamNodes List<String>) : void from class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Attribute private downstreamExecutionNodeId : int from class io.crate.planner.node.dql.join.NestedLoopNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Attribute private downstreamExecutionNodeId : int from class io.crate.planner.node.dql.MergeNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Attribute private downstreamExecutionNodeId : int from class io.crate.planner.node.dql.CollectNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Attribute private downstreamNodes : List<String> from class io.crate.planner.node.dql.join.NestedLoopNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Attribute private downstreamNodes : List<String> from class io.crate.planner.node.dql.MergeNode to class io.crate.planner.node.dql.AbstractDQLPlanNode",
//	      "Pull Up Attribute private downstreamNodes : List<String> from class io.crate.planner.node.dql.CollectNode to class io.crate.planner.node.dql.AbstractDQLPlanNode");

	    test.project("https://github.com/kuujo/copycat.git", "master") 
	    .atCommit("19a49f8f36b2f6d82534dc13504d672e41a3a8d1").contains(
	      "Pull Up Method private applyIndex(globalIndex long) : void from class net.kuujo.copycat.raft.state.ActiveState to private applyIndex(globalIndex long) : void from class net.kuujo.copycat.raft.state.PassiveState",
	      "Pull Up Method private applyCommits(commitIndex long) : CompletableFuture<Void> from class net.kuujo.copycat.raft.state.ActiveState to private applyCommits(commitIndex long) : CompletableFuture<Void> from class net.kuujo.copycat.raft.state.PassiveState",
	      "Pull Up Method private doAppendEntries(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private doAppendEntries(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState",
	      "Pull Up Method private doCheckPreviousEntry(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private doCheckPreviousEntry(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState",
	      "Pull Up Method private handleAppend(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.ActiveState to private handleAppend(request AppendRequest) : AppendResponse from class net.kuujo.copycat.raft.state.PassiveState");

	    test.assertExpectations();
	}
	
	@Test
	public void testPushDownMethod() throws Exception {
	    TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");
	    
	    test.project("https://github.com/hazelcast/hazelcast.git", "master") 
	    .atCommit("c00275e7f85c8a9af5785f66cc0f75dc027b6cb6").contains(
	      "Push Down Method protected getConnection() : HazelcastConnection from class com.hazelcast.jca.AbstractDeploymentTest to protected getConnection() : HazelcastConnection from class com.hazelcast.jca.XATransactionTest");

	    test.project("https://github.com/crate/crate.git", "master") 
	    .atCommit("5373a852a7e45715e0a6771b7cd56592994c07dd").contains(
	      "Push Down Method public plan() : Plan from class io.crate.planner.node.dql.ESDQLPlanNode to public plan() : Plan from class io.crate.planner.node.dql.ESGetNode");

	    test.project("https://github.com/BuildCraft/BuildCraft.git", "master") 
	    .atCommit("a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0").contains(
	      "Push Down Method public equals(obj Object) : boolean from class buildcraft.api.robots.ResourceId to public equals(obj Object) : boolean from class buildcraft.api.robots.ResourceIdBlock");

	    // push down implementation and set method to abstract
	    test.project("https://github.com/gradle/gradle.git", "master") 
	    .atCommit("b1fb1192daa1647b0bd525600dd41063765eca70").contains(
	      "Push Down Method public setTasks(taskNames List<String>) : void from class org.gradle.testkit.functional.GradleRunner to public setTasks(taskNames List<String>) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
	      "Push Down Method public getTasks() : List<String> from class org.gradle.testkit.functional.GradleRunner to public getTasks() : List<String> from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
	      "Push Down Method public setArguments(arguments List<String>) : void from class org.gradle.testkit.functional.GradleRunner to public setArguments(arguments List<String>) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
	      "Push Down Method public getArguments() : List<String> from class org.gradle.testkit.functional.GradleRunner to public getArguments() : List<String> from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
	      "Push Down Method public setWorkingDir(workingDirectory File) : void from class org.gradle.testkit.functional.GradleRunner to public setWorkingDir(workingDirectory File) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
	      "Push Down Method public getWorkingDir() : File from class org.gradle.testkit.functional.GradleRunner to public getWorkingDir() : File from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
	      "Push Down Method public setGradleUserHomeDir(gradleUserHomeDir File) : void from class org.gradle.testkit.functional.GradleRunner to public setGradleUserHomeDir(gradleUserHomeDir File) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
	      "Push Down Method public getGradleUserHomeDir() : File from class org.gradle.testkit.functional.GradleRunner to public getGradleUserHomeDir() : File from class org.gradle.testkit.functional.internal.DefaultGradleRunner");

	    test.project("https://github.com/bitcoinj/bitcoinj.git", "master") 
	    .atCommit("7744a00629514b9539acac05596d64af878fe747").notContains(
	      "Push Down Method protected maybeParse() : void from class org.bitcoinj.core.Message to protected parse() : void from class org.bitcoinj.core.ListMessage");

	    test.project("https://github.com/tomahawk-player/tomahawk-android.git", "master") 
	    .atCommit("56c273ee11296288cb15320c3de781b94a1e8eb4").contains(
	      "Push Down Method public getAlbumTimeStamps() : ConcurrentHashMap<Album,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to public getAlbumTimeStamps() : ConcurrentHashMap<Album,Long> from class org.tomahawk.libtomahawk.collection.UserCollection",
	      "Push Down Method public getArtistTimeStamps() : ConcurrentHashMap<Artist,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to public getArtistTimeStamps() : ConcurrentHashMap<Artist,Long> from class org.tomahawk.libtomahawk.collection.UserCollection",
	      "Push Down Method public addQuery(query Query, addedTimeStamp long) : void from class org.tomahawk.libtomahawk.collection.NativeCollection to public addQuery(query Query, addedTimeStamp long) : void from class org.tomahawk.libtomahawk.collection.UserCollection",
	      "Push Down Method public getQueryTimeStamps() : ConcurrentHashMap<Query,Long> from class org.tomahawk.libtomahawk.collection.NativeCollection to public getQueryTimeStamps() : ConcurrentHashMap<Query,Long> from class org.tomahawk.libtomahawk.collection.UserCollection");

	    // org.eclipse.jgit.errors.MissingObjectException: Missing unknown 6d10621465c0e6ae81ad8d240d70a55c72caeea6
//	    test.project("https://github.com/amplab/tachyon.git", "master") 
//	    .atCommit("6d10621465c0e6ae81ad8d240d70a55c72caeea6").contains(
//	      "Push Down Method public getBlockSize() : long from class tachyon.worker.block.meta.BlockMetaBase to public getBlockSize() : long from class tachyon.worker.block.meta.BlockMeta");
	    
	    test.assertExpectations();
	}

	@Test
    public void testTemp() throws Exception {
        TestBuilder test = new TestBuilder(new GitHistoryRefactoringDetector2(), "c:/Users/danilofs/tmp");
        
        
        // push down implementation and set method to abstract
        test.project("https://github.com/gradle/gradle.git", "master") 
        .atCommit("b1fb1192daa1647b0bd525600dd41063765eca70").containsOnly(
          "Push Down Method public setTasks(taskNames List<String>) : void from class org.gradle.testkit.functional.GradleRunner to public setTasks(taskNames List<String>) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
          "Push Down Method public getTasks() : List<String> from class org.gradle.testkit.functional.GradleRunner to public getTasks() : List<String> from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
          "Push Down Method public setArguments(arguments List<String>) : void from class org.gradle.testkit.functional.GradleRunner to public setArguments(arguments List<String>) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
          "Push Down Method public getArguments() : List<String> from class org.gradle.testkit.functional.GradleRunner to public getArguments() : List<String> from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
          "Push Down Method public setWorkingDir(workingDirectory File) : void from class org.gradle.testkit.functional.GradleRunner to public setWorkingDir(workingDirectory File) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
          "Push Down Method public getWorkingDir() : File from class org.gradle.testkit.functional.GradleRunner to public getWorkingDir() : File from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
          "Push Down Method public setGradleUserHomeDir(gradleUserHomeDir File) : void from class org.gradle.testkit.functional.GradleRunner to public setGradleUserHomeDir(gradleUserHomeDir File) : void from class org.gradle.testkit.functional.internal.DefaultGradleRunner",
          "Push Down Method public getGradleUserHomeDir() : File from class org.gradle.testkit.functional.GradleRunner to public getGradleUserHomeDir() : File from class org.gradle.testkit.functional.internal.DefaultGradleRunner");

        test.assertExpectations();
    }

}
