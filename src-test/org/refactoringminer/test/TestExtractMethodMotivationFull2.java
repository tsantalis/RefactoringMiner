package org.refactoringminer.test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.refactoringminer.model.SDMethod;
import org.refactoringminer.model.SDModel;
import org.refactoringminer.model.builder.GitHistoryStructuralDiffAnalyzer;
import org.refactoringminer.model.builder.RefactoringMotivationClassifier;
import org.refactoringminer.model.builder.StructuralDiffHandler;
import org.refactoringminer.model.builder.RefactoringMotivationClassifier.Motivation;

import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;

public class TestExtractMethodMotivationFull2 {

	@Test
	public void run() throws Exception {


		test("https://github.com/Activiti/Activiti.git", "a70ca1d9ad2ea07b19c5e1f9540c809d7a12d3fb", "org.activiti.engine.impl.db.DbSqlSession#flushPersistentObjects(Class, List)", "EM: Remove duplication");
		test("https://github.com/amplab/tachyon.git", "b0938501f1014cf663e33b44ed5bb9b24d19a358", "tachyon.client.BlockOutStreamIntegrationTest#getBlockOutStream(String, boolean)", "EM: Remove duplication");
		
//		test("https://github.com/amplab/tachyon.git", "ed966510ccf8441115614e2258aea61df0ea55f5", "tachyon.worker.block.meta.StorageDir#reserveSpace(long)", "EM: Facilitate extension");
		test("https://github.com/AntennaPod/AntennaPod.git", "c64217e2b485f3c6b997a55b1ef910c8b72779d3", "de.danoeh.antennapod.core.storage.DBWriter#addQueueItem(Context, boolean, long[])", "EM: Introduce alternative method signature");
		test("https://github.com/antlr/antlr4.git", "a9ca2efae56815dc464189b055ffe9da23766f7f", "org.antlr.v4.runtime.tree.Trees#getDescendants(ParseTree)", "EM: Replace method keeping backward compatibility");
		test("https://github.com/antlr/antlr4.git", "a9ca2efae56815dc464189b055ffe9da23766f7f", "org.antlr.v4.tool.GrammarParserInterpreter#getAmbuityParserInterpreter(Grammar, Parser, TokenStream)", "EM: Extract reusable method");
		test("https://github.com/apache/camel.git", "14a7dd79148f9306dcd2f748b56fd6550e9406ab", "org.apache.camel.maven.packaging.PackageDataFormatMojo#readClassFromCamelResource(File, StringBuilder, BuildContext)", "EM: Decompose method to improve readability");
		test("https://github.com/apache/camel.git", "14a7dd79148f9306dcd2f748b56fd6550e9406ab", "org.apache.camel.maven.packaging.PackageLanguageMojo#readClassFromCamelResource(File, StringBuilder, BuildContext)", "EM: Decompose method to improve readability");
		test("https://github.com/apache/cassandra.git", "e37d577b6cfc2d3e11252cef87ab9ebba72e1d52", "org.apache.cassandra.cql3.functions.UDFunction#assertUdfsEnabled(String)", "EM: Remove duplication");
		test("https://github.com/apache/cassandra.git", "b70f7ea0ce27b5defa0a7773d448732364e7aee0", "org.apache.cassandra.db.Directories#listSnapshots()", "EM: Extract reusable method");
//		test("https://github.com/apache/cassandra.git", "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", "org.apache.cassandra.dht.BootStrapper#getSpecifiedTokens(TokenMetadata, Collection)", "EM: Decompose method to improve readability");
		test("https://github.com/apache/cassandra.git", "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", "org.apache.cassandra.dht.BootStrapperTest#generateFakeEndpoints(TokenMetadata, int, int)", "EM: Improve testability", "EM: Introduce alternative method signature");
		test("https://github.com/apache/cassandra.git", "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", "org.apache.cassandra.dht.Murmur3Partitioner#getRandomToken(Random)", "EM: Improve testability", "EM: Introduce alternative method signature");
//		test("https://github.com/apache/hive.git", "0fa45e4a562fc2586b1ef06a88e9c186a0835316", "org.apache.hadoop.hive.metastore.hbase.HBaseImport#copyOneFunction(String, String)", "EM: Extract reusable method");
		test("https://github.com/apache/hive.git", "0fa45e4a562fc2586b1ef06a88e9c186a0835316", "org.apache.hadoop.hive.metastore.hbase.TestHBaseImport#setupObjectStore(RawStore, String[], String[], String[], String[], String[], int)", "EM: Extract reusable method");
//		test("https://github.com/apache/hive.git", "102b23b16bf26cbf439009b4b95542490a082710", "org.apache.hive.beeline.Commands#executeInternal(String, boolean)", "EM: Decompose method to improve readability", "EM: Extract reusable method");
		test("https://github.com/apache/hive.git", "102b23b16bf26cbf439009b4b95542490a082710", "org.apache.hive.beeline.Commands#handleMultiLineCmd(String)", "EM: Decompose method to improve readability");
		test("https://github.com/apache/hive.git", "5f78f9ef1e6c798849d34cc66721e6c1d9709b6f", "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat#generateSplitsInfo(Configuration, int)", "EM: Introduce alternative method signature");
		test("https://github.com/apache/hive.git", "e2dd54ab180b577b08cf6b0e69310ac81fc99fd3", "org.apache.hadoop.hive.ql.optimizer.ConstantPropagateProcFactory#foldExprFull(ExprNodeDesc, Map, ConstantPropagateProcCtx, Operator, int, boolean)", "EM: Decompose method to improve readability", "EM: Facilitate extension");
		test("https://github.com/baasbox/baasbox.git", "d949fe9079a82ee31aa91244aa67baaf56b7e28f", "com.baasbox.db.DbHelper#execMultiLineCommands(ODatabaseRecordTx, boolean, boolean, String[])", "EM: Introduce alternative method signature");
		test("https://github.com/belaban/JGroups.git", "f1533756133dec84ce8218202585ac85904da7c9", "org.jgroups.auth.FixedMembershipToken#isInMembersList(IpAddress)", "EM: Improve testability");
		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeAddressMessage(byte[], int)", "EM: Enable overriding");
		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeBlock(byte[], int)", "EM: Enable overriding");
		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeInventoryMessage(byte[], int)", "EM: Enable overriding");
		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeTransaction(byte[], int, int, byte[])", "EM: Enable overriding");
		test("https://github.com/bitcoinj/bitcoinj.git", "2fd96c777164dd812e8b5a4294b162889601df1d", "org.bitcoinj.core.Utils#newSha256Digest()", "EM: Remove duplication");
		test("https://github.com/BuildCraft/BuildCraft.git", "a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0", "buildcraft.robotics.ai.AIRobotSearchStackRequest#getAvailableRequests(DockingStation)", "EM: Decompose method to improve readability", "EM: Facilitate extension");
		test("https://github.com/checkstyle/checkstyle.git", "2f7481ee4e20ae785298c31ec2f979752dd7eb03", "com.puppycrawl.tools.checkstyle.checks.modifier.RedundantModifierCheck#checkInterfaceModifiers(DetailAST)", "EM: Decompose method to improve readability");
		test("https://github.com/checkstyle/checkstyle.git", "5a9b7249e3d092a78ac8e7d48aeeb62bf1c44e20", "com.puppycrawl.tools.checkstyle.checks.coding.RequireThisCheck#processField(DetailAST, int)", "EM: Decompose method to improve readability");
		test("https://github.com/crate/crate.git", "563d281b61e9f8748858e911eaa810e981f1e953", "io.crate.metadata.doc.DocIndexMetaData#getCustomRoutingCol()", "EM: Extract reusable method");
//		test("https://github.com/crate/crate.git", "c7b6a7aa878aabd6400d2df0490e1eb2b810c8f9", "io.crate.planner.consumer.ConsumingPlanner#plan(AnalyzedRelation, ConsumerContext)", "EM: Introduce alternative method signature");
		test("https://github.com/CyanogenMod/android_frameworks_base.git", "153611deab149accd8aeaf03fd102c0b069bd322", "com.android.internal.widget.LockPatternUtils#stringToPattern(String, byte)", "EM: Introduce alternative method signature");
		test("https://github.com/CyanogenMod/android_frameworks_base.git", "153611deab149accd8aeaf03fd102c0b069bd322", "com.android.internal.widget.LockPatternView.Cell#of(Cell[][], int, int, byte)", "EM: Introduce alternative method signature");
		test("https://github.com/CyanogenMod/android_frameworks_base.git", "4587c32ab8a1c8e2169e4f93491a8c927216a6ab", "com.android.systemui.usb.StorageNotification#startAsync()", "EM: Facilitate extension");
		test("https://github.com/CyanogenMod/android_frameworks_base.git", "5d1a70a4d32ac4c96a32535c68c69b20288d8968", "com.android.server.am.ActivityManagerService#killProcessGroup(int, int)", "EM: Extract reusable method");
		test("https://github.com/CyanogenMod/android_frameworks_base.git", "15fd4f9caea01e53725086e290d3b35ec4bd4cd9", "com.android.keyguard.KeyguardAbsKeyInputView#reset(boolean)", "EM: Introduce alternative method signature");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#getMaxRequestsPerConnection(HostDistance)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#getNewConnectionThreshold(HostDistance)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#setMaxRequestsPerConnection(HostDistance, int)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#setNewConnectionThreshold(HostDistance, int)", "EM: Replace method keeping backward compatibility");
		test("https://github.com/deeplearning4j/deeplearning4j.git", "91cdfa1ffd937a4cb01cdc0052874ef7831955e2", "org.deeplearning4j.optimize.solvers.BackTrackLineSearch#getNewScore(INDArray)", "EM: Remove duplication");
		test("https://github.com/deeplearning4j/deeplearning4j.git", "3325f5ccd23f8016fa28a24f878b54f1918546ed", "org.deeplearning4j.models.embeddings.loader.WordVectorSerializer#loadGoogleModel(File, boolean, boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/droolsjbpm/drools.git", "7ffc62aa554f5884064b81ee80078e35e3833006", "org.drools.persistence.SingleSessionCommandService#addInterceptor(Interceptor, boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/droolsjbpm/jbpm.git", "83cfa21578e63956bca0715eefee2860c3b6d39a", "org.jbpm.services.task.wih.HTWorkItemHandlerBaseTest#prepareWorkItemWithTaskVariables(String)", "EM: Extract reusable method");
		test("https://github.com/droolsjbpm/jbpm.git", "83cfa21578e63956bca0715eefee2860c3b6d39a", "org.jbpm.services.task.wih.HTWorkItemHandlerBaseTest#testTaskWithExpectedDescription(Task, String)", "EM: Extract reusable method");
		test("https://github.com/droolsjbpm/jbpm.git", "a739d16d301f0e89ab0b9dfa56b4585bbad6b793", "org.jbpm.services.task.LifeCycleBaseTest#createUser(String)", "EM: Remove duplication");
		test("https://github.com/eclipse/jetty.project.git", "837d1a74bb7d694220644a2539c4440ce55462cf", "org.eclipse.jetty.proxy.ProxyServletTest#testTransparentProxyWithQuery(String, String, String)", "EM: Extract reusable method");
		test("https://github.com/fabric8io/fabric8.git", "9e61a71540da58c3208fd2c7737f793c3f81e5ae", "io.fabric8.maven.CreateGogsWebhook#createGogsWebhook(KubernetesClient, Log, String, String, String, String, String)", "EM: Extract reusable method");
		test("https://github.com/facebook/buck.git", "89973a5e4f188040c5fcf87fb5a3e9167329d175", "com.facebook.buck.cli.InstallCommand#installAppleBundleForSimulator(CommandRunnerParams, AppleBundle, ProjectFilesystem, ProcessExecutor)", "EM: Facilitate extension");
		test("https://github.com/facebook/buck.git", "8d14e557e01cc607dd2db66c29d106ef01aa81f7", "com.facebook.buck.parser.Parser.CachedState#get(BuildTarget, Optional)", "EM: Introduce alternative method signature");
		test("https://github.com/facebook/buck.git", "6c93f15f502f39dff99ecb01b56dcad7dddb0f0d", "com.facebook.buck.android.aapt.AaptResourceCollector#getEnumerator(RType)", "EM: Extract reusable method");
		test("https://github.com/facebook/buck.git", "52cfd39ecba349c4d8e2c46eac76ed4d75b7ebae", "com.facebook.buck.apple.AppleSdkDiscoveryTest#createSymLinkSdks(Iterable, Path, String)", "EM: Extract reusable method");
		test("https://github.com/facebook/buck.git", "f26d234e8d3458f34454583c22e3bd5f4b2a5da8", "com.facebook.buck.android.AdbHelper#getDevices()", "EM: Improve testability");
		test("https://github.com/facebook/presto.git", "8b1f5ce432bd6f579c646705d79ff0c4690495ae", "com.facebook.presto.operator.scalar.ArraySubscriptOperator#checkArrayIndex(long)", "EM: Extract reusable method");
		test("https://github.com/FasterXML/jackson-databind.git", "cfe88fe3fbcc6b02ca55cee7b1f4ab13e249edea", "com.fasterxml.jackson.databind.type.TypeFactory#classForName(String)", "EM: Improve testability");
		test("https://github.com/FasterXML/jackson-databind.git", "cfe88fe3fbcc6b02ca55cee7b1f4ab13e249edea", "com.fasterxml.jackson.databind.type.TypeFactory#classForName(String, boolean, ClassLoader)", "EM: Improve testability");
		test("https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git", "0b93231025f51c7ec62fd8588985c5dc807854e4", "com.goide.actions.fmt.GoExternalToolsAction#doSomething(VirtualFile, Module, Project, String, boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/GoClipse/goclipse.git", "851ab757698304e9d8d4ae24ab75be619ddae31a", "melnorme.lang.tooling.ast.SourceRange#contains(int)", "EM: Replace method keeping backward compatibility");
		test("https://github.com/GoClipse/goclipse.git", "851ab757698304e9d8d4ae24ab75be619ddae31a", "melnorme.lang.tooling.ast.SourceRange#contains(SourceRange)", "EM: Replace method keeping backward compatibility");
		test("https://github.com/google/closure-compiler.git", "ea96643364e91125f560e9508a5cbcdb776bde64", "com.google.javascript.jscomp.parsing.parser.Parser#parseFormalParameterList(boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/google/j2objc.git", "fa3e6fa02dadc675f0d487a15cd842b3ac4a0c11", "com.google.devtools.j2objc.translate.Autoboxer#getOperatorFunctionModifier(Expression)", "EM: Remove duplication");
		test("https://github.com/google/truth.git", "200f1577d238a6d3fbcf99cb2a2585b2071214a6", "com.google.common.truth.IterableSubject#isOrdered()", "EM: Replace method keeping backward compatibility");
		test("https://github.com/google/truth.git", "200f1577d238a6d3fbcf99cb2a2585b2071214a6", "com.google.common.truth.IterableSubject#isOrdered(Comparator)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/gradle/gradle.git", "79c66ceab11dae0b9fd1dade7bb4120028738705", "org.gradle.platform.base.binary.BaseBinarySpec#getInputs()", "EM: Replace method keeping backward compatibility");
		test("https://github.com/grails/grails-core.git", "480537e0f8aaf50a7648bf445b33230aa32a9b44", "org.grails.compiler.injection.test.TestMixinTransformation#weaveMixinsIntoClass(ClassNode, ListExpression, ClassNode)", "EM: Introduce alternative method signature");
		test("https://github.com/grails/grails-core.git", "480537e0f8aaf50a7648bf445b33230aa32a9b44", "org.grails.compiler.injection.test.TestMixinTransformation#weaveTestMixins(ClassNode, ListExpression, ClassNode)", "EM: Introduce alternative method signature");
		test("https://github.com/gwtproject/gwt.git", "22fb2c9c6974bd1fe0f6ff684f52e6cfbed1a387", "com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer.RescueVisitor#rescueMembersAndInstantiateSuperInterfaces(JDeclaredType)", "EM: Remove duplication");
//		test("https://github.com/hazelcast/hazelcast.git", "679d38d4316c16ccba4982d7f3ba13c147a451cb", "com.hazelcast.client.cache.impl.AbstractClientCacheProxy#getFromNearCache(Data, boolean)", "EM: Decompose method to improve readability");
		test("https://github.com/JetBrains/intellij-community.git", "7dd55014f9840ce03867bb175cf37a4c151dc806", "com.intellij.openapi.options.ex.ConfigurableWrapper#createConfigurable(ConfigurableEP, boolean)", "EM: Decompose method to improve readability");
		test("https://github.com/JetBrains/intellij-community.git", "1b70adbfd49e00194c4c1170ef65e8114d7a2e46", "com.intellij.codeInspection.dataFlow.value.DfaVariableValue#getFieldInitializerNullness(PsiExpression)", "EM: Decompose method to improve readability", "EM: Facilitate extension");
		test("https://github.com/JetBrains/intellij-community.git", "10f769a60c7c7b73982e978959d381df487bbe2d", "com.intellij.execution.junit.JUnit4Framework#getLibraryPaths()", "EM: Extract reusable method");
		test("https://github.com/JetBrains/intellij-community.git", "106d1d51754f454fa673976665e41f463316e084", "com.intellij.lang.java.parser.ReferenceParser#dummy(PsiBuilder)", "EM: Extract reusable method");
		test("https://github.com/JetBrains/intellij-community.git", "e1625136ba12907696ef4c6e922ce073293f3a2c", "org.jetbrains.idea.maven.project.MavenProject#addAnnotationProcessorOption(String, Map)", "EM: Extract reusable method");
		test("https://github.com/JetBrains/intellij-community.git", "d71154ed21e2d5c65bb0ddb000bcb04ca5735048", "org.jetbrains.debugger.sourcemap.SourceResolver#canonicalizePath(String, Url, boolean)", "EM: Extract reusable method");
		test("https://github.com/JetBrains/intellij-community.git", "a97341973c3b683d62d1422e5404ed5c7ccf45f8", "org.jetbrains.plugins.javaFX.fxml.refs.FxmlReferencesContributor.MyJavaClassReferenceProvider.JavaClassReferenceWrapper#setNewName(String)", "EM: Extract reusable method", "EM: Facilitate extension");
		test("https://github.com/JetBrains/intellij-community.git", "cc0eaf7faa408a04b68e2b5820f3ebcc75420b5b", "com.siyeh.ig.migration.UnnecessaryBoxingInspection.UnnecessaryBoxingVisitor#canBinaryExpressionBeUnboxed(PsiExpression, PsiExpression)", "EM: Extract reusable method");
		test("https://github.com/JetBrains/intellij-community.git", "7ed3f273ab0caf0337c22f0b721d51829bb0c877", "com.jetbrains.edu.stepic.EduStepicConnector#addCoursesFromStepic(List, int)", "EM: Extract reusable method", "EM: Facilitate extension");
		test("https://github.com/JetBrains/intellij-plugins.git", "0df7cb00757fe0d4fac8d8b0d5fc46af95feb238", "com.jetbrains.lang.dart.resolve.DartResolver#findPsiFile(Project, String)", "EM: Decompose method to improve readability");
		test("https://github.com/JetBrains/MPS.git", "61b5decd4a4e5e6bbdea99eb76f136ca10915b73", "jetbrains.mps.nodeEditor.cellProviders.AbstractCellListHandler#startInsertMode(EditorContext, EditorCell, boolean)", "EM: Replace method keeping backward compatibility");
		test("https://github.com/JetBrains/MPS.git", "797fb7fc1415ac0ebe9a8262677dfa4462ed6cb4", "jetbrains.mps.text.impl.TextGenSupport#doAppendNode(SNode)", "EM: Facilitate extension");
		test("https://github.com/jfinal/jfinal.git", "881baed894540031bd55e402933bcad28b74ca88", "com.jfinal.validate.Validator#validateIntegerValue(String, int, int, String, String)", "EM: Extract reusable method");
		test("https://github.com/jfinal/jfinal.git", "881baed894540031bd55e402933bcad28b74ca88", "com.jfinal.validate.Validator#validateLongValue(String, long, long, String, String)", "EM: Extract reusable method");
		test("https://github.com/jfinal/jfinal.git", "881baed894540031bd55e402933bcad28b74ca88", "com.jfinal.validate.Validator#validateLongValue(String, String, String)", "EM: Extract reusable method");
		test("https://github.com/jMonkeyEngine/jmonkeyengine.git", "5989711f7315abe4c3da0f1516a3eb3a81da6716", "com.jme3.gde.materialdefinition.editor.DraggablePanel#movePanel(int, int)", "EM: Extract reusable method", "EM: Facilitate extension");
		test("https://github.com/jMonkeyEngine/jmonkeyengine.git", "5989711f7315abe4c3da0f1516a3eb3a81da6716", "com.jme3.gde.materialdefinition.editor.DraggablePanel#saveLocation()", "EM: Extract reusable method", "EM: Facilitate extension");
		test("https://github.com/jOOQ/jOOQ.git", "58a4e74d28073e7c6f15d1f225ac1c2fd9aa4357", "org.jooq.tools.Convert.ConvertAll#millis(Temporal)", "EM: Remove duplication");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(boolean, char)", "EM: Introduce alternative method signature");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(boolean, char, String)", "EM: Introduce alternative method signature");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(OutputStream, boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(OutputStream, boolean, char, String)", "EM: Introduce alternative method signature");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(Writer, boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(Writer, boolean, char)", "EM: Introduce alternative method signature");
		test("https://github.com/jOOQ/jOOQ.git", "227254cf769f3e821ed1b2ef2d88c4ec6b20adea", "org.jooq.impl.ResultImpl#formatCSV(Writer, boolean, char, String)", "EM: Introduce alternative method signature");
		test("https://github.com/k9mail/k-9.git", "9d44f0e06232661259681d64002dd53c7c43099d", "com.fsck.k9.controller.MessagingController#handleSendFailure(Account, Store, Folder, Message, Exception, boolean)", "EM: Extract reusable method");
//		test("https://github.com/k9mail/k-9.git", "23c49d834d3859fc76a604da32d1789d2e863303", "com.fsck.k9.controller.MessagingController#buildNotificationNavigationStack(Context, Account, LocalMessage, int, int, ArrayList)", "EM: Decompose method to improve readability");
//		test("https://github.com/k9mail/k-9.git", "23c49d834d3859fc76a604da32d1789d2e863303", "com.fsck.k9.controller.MessagingController#setNotificationContent(Context, Message, CharSequence, CharSequence, Builder, String)", "EM: Decompose method to improve readability", "EM: Extract reusable method");
		test("https://github.com/languagetool-org/languagetool.git", "01cddc5afb590b4d36cb784637a8ea8aa31d3561", "org.languagetool.gui.ConfigurationDialog#createNonOfficeElements(GridBagConstraints, JPanel)", "EM: Decompose method to improve readability");
		test("https://github.com/languagetool-org/languagetool.git", "01cddc5afb590b4d36cb784637a8ea8aa31d3561", "org.languagetool.gui.ConfigurationDialog#getMotherTonguePanel(GridBagConstraints)", "EM: Decompose method to improve readability");
		test("https://github.com/languagetool-org/languagetool.git", "01cddc5afb590b4d36cb784637a8ea8aa31d3561", "org.languagetool.gui.ConfigurationDialog#getMouseAdapter()", "EM: Decompose method to improve readability");
		test("https://github.com/languagetool-org/languagetool.git", "01cddc5afb590b4d36cb784637a8ea8aa31d3561", "org.languagetool.gui.ConfigurationDialog#getTreeButtonPanel()", "EM: Decompose method to improve readability");
		test("https://github.com/languagetool-org/languagetool.git", "01cddc5afb590b4d36cb784637a8ea8aa31d3561", "org.languagetool.gui.ConfigurationDialog#getTreeModel(DefaultMutableTreeNode)", "EM: Decompose method to improve readability");
		test("https://github.com/liferay/liferay-plugins.git", "7c7ecf4cffda166938efd0ae34830e2979c25c73", "com.liferay.sync.hook.listeners.ResourcePermissionModelListener#updateSyncDLObject(SyncDLObject)", "EM: Enable recursion");
		test("https://github.com/linkedin/rest.li.git", "54fa890a6af4ccf564fb481d3e1b6ad4d084de9e", "com.linkedin.r2.filter.compression.ClientCompressionFilter#addResponseCompressionHeaders(CompressionOption, RestRequest)", "EM: Decompose method to improve readability", "EM: Facilitate extension");
		test("https://github.com/mockito/mockito.git", "2d036ecf1d7170b4ec7346579a1ef8904109530a", "org.mockito.internal.creation.bytebuddy.MockBytecodeGenerator#allMockedTypes(MockFeatures)", "EM: Decompose method to improve readability");
//		test("https://github.com/mongodb/morphia.git", "70a25d4afdc435e9cad4460b2a20b7aabdd21e35", "org.mongodb.morphia.TestMapping#performBasicMappingTest()", "EM: Extract reusable method");
		test("https://github.com/mrniko/redisson.git", "186357ac6c2da1a5a12c0287a08408ac5ec6683b", "org.redisson.connection.MasterSlaveConnectionManager#createClient(String, int, int)", "EM: Introduce alternative method signature");
//		test("https://github.com/neo4j/neo4j.git", "dc199688d69416da58b370ca2aa728e935fc8e0d", "org.neo4j.kernel.impl.api.state.TxState#getSortedIndexUpdates(IndexDescriptor)", "EM: Extract reusable method");
		test("https://github.com/neo4j/neo4j.git", "d3533c1a0716ca114d294b3ea183504c9725698f", "org.neo4j.kernel.impl.util.Neo4jJobScheduler#createNewThread(Group, Runnable, Map)", "EM: Extract reusable method");
		test("https://github.com/neo4j/neo4j.git", "b83e6a535cbca21d5ea764b0c49bfca8a9ff9db4", "org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReader#query(Query)", "EM: Remove duplication");
		test("https://github.com/Netflix/eureka.git", "5103ace802b2819438318dd53b5b07512aae0d25", "com.netflix.eureka2.integration.EurekaDeploymentClients#fillUpRegistryOfServer(int, int, InstanceInfo)", "EM: Introduce alternative method signature");
		test("https://github.com/Netflix/eureka.git", "f6212a7e474f812f31ddbce6d4f7a7a0d498b751", "com.netflix.discovery.DiscoveryClient#onRemoteStatusChanged(InstanceStatus, InstanceStatus)", "EM: Enable overriding");
		test("https://github.com/Netflix/zuul.git", "b25d3f32ed2e2da86f5c746098686445c2e2a314", "com.netflix.zuul.FilterLoader#putFilter(String, ZuulFilter, long)", "EM: Extract reusable method");
		test("https://github.com/oblac/jodd.git", "722ef9156896248ef3fbe83adde0f6ff8f46856a", "jodd.http.HttpBase#resolveFormEncoding()", "EM: Extract reusable method");
		test("https://github.com/open-keychain/open-keychain.git", "c11fef6e7c80681ce69e5fdc7f4796b0b7a18e2b", "org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment#cryptoOperation(CryptoInputParcel, boolean)", "EM: Introduce alternative method signature");
		test("https://github.com/openhab/openhab.git", "a9b1e5d67421ed98b49ae25c3bbd6e27a0ab1590", "org.openhab.binding.insteonplm.InsteonPLMActiveBinding#processBindingConfiguration()", "EM: Decompose method to improve readability");
		test("https://github.com/openhab/openhab.git", "a9b1e5d67421ed98b49ae25c3bbd6e27a0ab1590", "org.openhab.binding.insteonplm.internal.message.MsgFactory#bail(String)", "EM: Extract reusable method");
//		test("https://github.com/orfjackal/retrolambda.git", "46b0d84de9c309bca48a99e572e6611693ed5236", "net.orfjackal.retrolambda.files.ClassSaver#saveResource(Path, byte[])", "EM: Extract reusable method");
		test("https://github.com/orientechnologies/orientdb.git", "1089957b645bde069d3864563bbf1f7c7da8045c", "com.orientechnologies.orient.core.db.tool.ODatabaseImport#rewriteLinksInDocument(ODocument, OIndex)", "EM: Improve testability", "EM: Introduce alternative method signature");
		test("https://github.com/osmandapp/Osmand.git", "e95aa8ab32a0334b9c941799060fd601297d05e4", "net.osmand.plus.activities.FavoritesListFragment#showItemPopupOptionsMenu(FavouritePoint, Activity, View)", "EM: Extract reusable method");
		test("https://github.com/osmandapp/Osmand.git", "e95aa8ab32a0334b9c941799060fd601297d05e4", "net.osmand.plus.activities.FavoritesTreeFragment#showItemPopupOptionsMenu(FavouritePoint, View)", "EM: Extract reusable method");
		test("https://github.com/phishman3579/java-algorithms-implementation.git", "f2385a56e6aa040ea4ff18a23ce5b63a4eeacf29", "com.jwetherell.algorithms.sorts.timing.SortsTiming#putOutTheGarbage()", "EM: Facilitate extension");
		test("https://github.com/puniverse/quasar.git", "c22d40fab8dfe4c5cad9ba582caf0855ff64b324", "co.paralleluniverse.strands.channels.reactivestreams.ChannelSubscriber#failedSubscribe(Subscription)", "EM: Enable overriding");
		test("https://github.com/ratpack/ratpack.git", "da6167af3bdbf7663af6c20fb603aba27dd5e174", "ratpack.server.internal.DefaultResponseTransmitter#post(HttpResponseStatus, ChannelFuture)", "EM: Introduce alternative method signature");
		test("https://github.com/realm/realm-java.git", "6cf596df183b3c3a38ed5dd9bb3b0100c6548ebb", "io.realm.examples.realmmigrationexample.MigrationExampleActivity#showStatus(String)", "EM: Introduce alternative method signature");
//		test("https://github.com/restlet/restlet-framework-java.git", "7ffe37983e2f09637b0c84d526a2f824de652de4", "org.restlet.ext.apispark.internal.conversion.swagger.v2_0.Swagger2Reader#fillRepresentation(Model, String, Contract)", "EM: Extract reusable method", "EM: Facilitate extension");
		test("https://github.com/robovm/robovm.git", "7837d0baf1aef45340eec699516a8c3a22aeb553", "org.robovm.compiler.target.ios.IOSTarget#signFrameworks(File, boolean)", "EM: Extract reusable method");
		test("https://github.com/rstudio/rstudio.git", "229d1b60c03a3f8375451c68a6911660a3993777", "org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy#isUpdate()", "EM: Extract reusable method");
		test("https://github.com/scobal/seyren.git", "5fb36a321af7df470d4c845cb18da8f85be31c38", "com.seyren.core.util.velocity.VelocityEmailHelper#evaluateTemplate(Check, Subscription, List, String)", "EM: Extract reusable method");
		test("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", "com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper#createCellSignalTable(SQLiteDatabase)", "EM: Decompose method to improve readability");
		test("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", "com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper#createCellTable(SQLiteDatabase)", "EM: Decompose method to improve readability");
		test("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", "com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper#createDefaultMCCTable(SQLiteDatabase)", "EM: Decompose method to improve readability");
		test("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", "com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper#createEventLogTable(SQLiteDatabase)", "EM: Decompose method to improve readability");
		test("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", "com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper#createLocationTable(SQLiteDatabase)", "EM: Decompose method to improve readability");
		test("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", "com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper#createOpenCellIDTable(SQLiteDatabase)", "EM: Decompose method to improve readability");
		test("https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git", "e235f884f2e0bc258da77b9c80492ad33386fa86", "com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter.DbHelper#createSilentSmsTable(SQLiteDatabase)", "EM: Decompose method to improve readability");
		test("https://github.com/siacs/Conversations.git", "e6cb12dfe414497b4317820497985c110cb81864", "eu.siacs.conversations.ui.adapter.MessageAdapter#getItemViewType(Message)", "EM: Introduce alternative method signature");
		test("https://github.com/spotify/helios.git", "cc02c00b8a92ef34d1a8bcdf44a45fb01a8dea6c", "com.spotify.helios.system.SystemTestBase#createJobRawOutput(Job)", "EM: Extract reusable method");
//		test("https://github.com/spotify/helios.git", "dd8753cfb0f67db4dde6c5254e2df3104b635dae", "com.spotify.helios.master.ZooKeeperMasterModel#getDeploymentGroup(ZooKeeperClient, String)", "EM: Introduce alternative method signature");
		test("https://github.com/spring-projects/spring-boot.git", "becced5f0b7bac8200df7a5706b568687b517b90", "org.springframework.boot.actuate.system.ApplicationPidFileWriterTests#createEnvironmentPreparedEvent(String, String)", "EM: Decompose method to improve readability");
		test("https://github.com/spring-projects/spring-boot.git", "becced5f0b7bac8200df7a5706b568687b517b90", "org.springframework.boot.actuate.system.ApplicationPidFileWriterTests#createPreparedEvent(String, String)", "EM: Decompose method to improve readability", "EM: Extract reusable method");
		test("https://github.com/spring-projects/spring-boot.git", "b47634176fa48ad925f79886c6aaca225cb9af64", "org.springframework.boot.actuate.metrics.buffer.BufferMetricReader#findAll(Predicate)", "EM: Remove duplication");
		test("https://github.com/spring-projects/spring-boot.git", "1cfc6f64f64353bc5530a8ce8cdacfc3eba3e7b2", "org.springframework.boot.orm.jpa.EntityScanRegistrar#addEntityScanBeanPostProcessor(BeanDefinitionRegistry, Set)", "EM: Decompose method to improve readability", "EM: Facilitate extension");
		test("https://github.com/spring-projects/spring-boot.git", "20d39f7af2165c67d5221f556f58820c992d2cc6", "org.springframework.boot.cloudfoundry.VcapApplicationListener#getFullKey(String, String)", "EM: Decompose method to improve readability");
//		test("https://github.com/spring-projects/spring-data-rest.git", "b7cba6a700d8c5e456cdeffe9c5bf54563eab7d3", "org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#setupMockMvc()", "EM: Enable overriding");
		test("https://github.com/spring-projects/spring-framework.git", "ece12f9d370108549fffac105e4bcb7faeaaf124", "org.springframework.core.annotation.AnnotationUtilsTests#assertMissingTextAttribute(Map)", "EM: Remove duplication");
		test("https://github.com/structr/structr.git", "15afd616cba5fb3d432d11a6de0d4f7805b202db", "org.structr.core.graph.CypherQueryCommand#handleObject(NodeFactory, RelationshipFactory, String, Object, boolean, boolean, int)", "EM: Enable recursion", "EM: Facilitate extension");
		test("https://github.com/structr/structr.git", "6c59050b8b03adf6d8043f3fb7add0496f447edf", "org.structr.rest.resource.SchemaTypeResource#getSchemaProperties(SchemaNode)", "EM: Extract reusable method");
//		test("https://github.com/VoltDB/voltdb.git", "deb8e5ca64fcf633edbd89523af472da813b6772", "org.voltdb.regressionsuites.TestApproxCountDistinctSuite#getNormalValue(Random, double, long, long)", "EM: Extract reusable method");
//		test("https://github.com/VoltDB/voltdb.git", "e58c9c3eef4c6e44b21a97cfbd2862bb2eb4627a", "org.voltdb.sqlparser.symtab.SymbolTableAssert#hasSize(int)", "EM: Improve testability", "EM: Introduce alternative method signature");
		test("https://github.com/WhisperSystems/TextSecure.git", "f0b2cc559026871c1b4d8e008666afb590553004", "org.thoughtcrime.securesms.notifications.NotificationState#craftIntent(Context, String, Bundle)", "EM: Extract reusable method");
		test("https://github.com/wicketstuff/core.git", "8ea46f48063c38473c12ca7c114106ca910b6e74", "org.wicketstuff.foundation.tab.FoundationTabTest#testRenderedTab()", "EM: Extract reusable method");
		test("https://github.com/wildfly/wildfly.git", "d7675fb0b19d3d22978e79954f441eeefd74a3b2", "org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor#handleExcludeMethods(EJBComponentDescription, ExcludeListMetaData)", "EM: Extract reusable method");
		test("https://github.com/wildfly/wildfly.git", "d7675fb0b19d3d22978e79954f441eeefd74a3b2", "org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor#handleMethodPermissions(EJBComponentDescription, MethodPermissionsMetaData)", "EM: Extract reusable method");
		test("https://github.com/wildfly/wildfly.git", "37d842bfed9779e662321a5ee43c36b058386843", "org.jboss.as.test.shared.ServerReload#executeReloadAndWaitForCompletion(ModelControllerClient, int)", "EM: Introduce alternative method signature");
		test("https://github.com/wordpress-mobile/WordPress-Android.git", "4bfe164cc8b4556b98df18098b162e0a84038b32", "org.wordpress.android.ui.main.WPMainActivity#trackLastVisibleTab(int)", "EM: Extract reusable method");
		test("https://github.com/xetorthio/jedis.git", "d4b4aecbc69bbd04ba87c4e32a52cff3d129906a", "redis.clients.util.Pool#poolInactive()", "EM: Decompose method to improve readability", "EM: Remove duplication");
//*/

		testAll();
		System.out.print("Overall: ");
		pr(tp, fp, fn);
		for (Motivation m : Motivation.values()) {
			int i = m.ordinal();
			if (tpm[i] > 0 || fpm[i] > 0 || fnm[i] > 0) {
				System.out.print(m + ": ");
				pr(tpm[i], fpm[i], fnm[i]);
			}
		}
	}

	private static void pr(int tp, int fp, int fn) {
		double prec = ((double) tp) / (tp + fp);
		double rec = ((double) tp) / (tp + fn);
		System.out.println(String.format("tp: %d, fp: %d, fn: %d, prec: %.2f, rec: %.2f", tp, fp, fn, prec, rec));
	}

	private GitService gitService = new GitServiceImpl();
	private GitHistoryStructuralDiffAnalyzer analyzer = new GitHistoryStructuralDiffAnalyzer();

	private int tp = 0;
	private int fp = 0;
	private int fn = 0;
	private int[] tpm;
	private int[] fpm;
	private int[] fnm;

	private Map<String, List<List<String>>> data = new LinkedHashMap<String, List<List<String>>>();
	
	private void test(String cloneUrl, String commit, final String entity, final String ... motivations) throws Exception {
		String key = cloneUrl + "#" + commit;
		List<List<String>> entry = data.get(key);
		if (entry == null) {
			entry = new LinkedList<List<String>>();
			data.put(key, entry);
		}
		ArrayList<String> ref = new ArrayList<String>(motivations.length + 1);
		ref.add(entity);
		for (String m : motivations) {
			ref.add(m);
		}
		entry.add(ref);
	}
	
	private void testAll() throws Exception {
		tp = 0;
		fp = 0;
		fn = 0;
		tpm = new int[Motivation.values().length];
		fpm = new int[Motivation.values().length];
		fnm = new int[Motivation.values().length];

		for (Map.Entry<String, List<List<String>>> c : data.entrySet()) {
			final String key = c.getKey();
			String cloneUrl = key.substring(0, key.indexOf('#'));
			String commit = key.substring(key.indexOf('#') + 1);
			final List<List<String>> refactorings = c.getValue();

			Repository repo = gitService.cloneIfNotExists(
					"C:\\tmp\\" + cloneUrl.substring(cloneUrl.lastIndexOf("/") + 1, cloneUrl.lastIndexOf(".git")),
					cloneUrl);

			try {
				analyzer.detectAtCommit(repo, commit, new StructuralDiffHandler() {
					@Override
					public void handle(RevCommit commitData, SDModel sdModel) {
						for (List<String> r : refactorings) {
							assertExtractMethodMotivation(sdModel, r.get(0), r.subList(1, r.size()));
						}
					}
				});
			} catch (Exception e) {
				System.out.println("ERROR " + e.getMessage() + " " + key);
			}
		}
	}
	
	private void assertExtractMethodMotivation(SDModel sdModel, String method, Iterable<String> motivations) {
		final RefactoringMotivationClassifier classifier = new RefactoringMotivationClassifier(sdModel);
		SDMethod extractedMethod = sdModel.after().find(SDMethod.class, method);
		Set<Motivation> tags = classifier.classifyExtractMethod(extractedMethod);
		EnumSet<Motivation> expectedMotivations = EnumSet.noneOf(Motivation.class);
		for (String m : motivations) {
			expectedMotivations.add(Motivation.fromName(m));
		}
		EnumSet<Motivation> actualMotivations = EnumSet.noneOf(Motivation.class);
		actualMotivations.addAll(tags);
		String actualMotivationsString = actualMotivations.toString();

		boolean problem = false;
		for (Motivation m : expectedMotivations) {
			if (actualMotivations.contains(m)) {
				tp++;
				tpm[m.ordinal()]++;
				actualMotivations.remove(m);
			} else {
				fn++;
				fnm[m.ordinal()]++;
				problem = true;
			}
		}
		for (Motivation m : actualMotivations) {
			fp++;
			fpm[m.ordinal()]++;
			problem = true;
		}
		if (problem) {
			System.out.println(method);
			System.out.println("Warn: expected: " + expectedMotivations + " actual: " + actualMotivationsString);
		}
	}
}
