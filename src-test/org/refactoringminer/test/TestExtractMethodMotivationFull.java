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

public class TestExtractMethodMotivationFull {

	@Test
	public void run() throws Exception {
		/* testes que funcionam
		// deprecated no javadoc agora funciona
		test("https://github.com/antlr/antlr4.git", "a9ca2efae56815dc464189b055ffe9da23766f7f", "org.antlr.v4.runtime.tree.Trees#getDescendants(ParseTree)", "EM: Replace method keeping backward compatibility");
		
		// funciona enable recursion
		//test("https://github.com/liferay/liferay-plugins.git", "7c7ecf4cffda166938efd0ae34830e2979c25c73", "com.liferay.sync.hook.listeners.ResourcePermissionModelListener#updateSyncDLObject(SyncDLObject)", "EM: Enable recursion");
		
		*/
		
		
		// não funciona porque há reuso dentro de proprio metodo de origem (e que poderia até ser considerado duplication)
		//test("https://github.com/wildfly/wildfly.git", "d7675fb0b19d3d22978e79954f441eeefd74a3b2", "org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor#handleExcludeMethods(EJBComponentDescription, ExcludeListMetaData)", "EM: Extract reusable method");
		//test("https://github.com/wildfly/wildfly.git", "d7675fb0b19d3d22978e79954f441eeefd74a3b2", "org.jboss.as.ejb3.deployment.processors.merging.MethodPermissionsMergingProcessor#handleMethodPermissions(EJBComponentDescription, MethodPermissionsMetaData)", "EM: Extract reusable method");
		// reuso dentro do proprio metodo
		//test("https://github.com/apache/hive.git", "0fa45e4a562fc2586b1ef06a88e9c186a0835316", "org.apache.hadoop.hive.metastore.hbase.HBaseImport#copyOneFunction(String, String)", "EM: Extract reusable method");

		// não funciona porque o origins esta incompleto
		//test("https://github.com/bitcoinj/bitcoinj.git", "2fd96c777164dd812e8b5a4294b162889601df1d", "org.bitcoinj.core.Utils#newSha256Digest()", "EM: Remove duplication");
		
		// não funciona pois ele considera que adicionou três linhas de código
		//test("https://github.com/apache/camel.git", "14a7dd79148f9306dcd2f748b56fd6550e9406ab", "org.apache.camel.maven.packaging.PackageDataFormatMojo#readClassFromCamelResource(File, StringBuilder, BuildContext)", "EM: Decompose method to improve readability");
		//test("https://github.com/apache/camel.git", "14a7dd79148f9306dcd2f748b56fd6550e9406ab", "org.apache.camel.maven.packaging.PackageLanguageMojo#readClassFromCamelResource(File, StringBuilder, BuildContext)", "EM: Decompose method to improve readability");
		
		
		// O metodo do qual foi extraído foi renomeado e não foi reportado rename? muito estranho que tem várias instâncias sendo reportadas
		//test("https://github.com/apache/hive.git", "0fa45e4a562fc2586b1ef06a88e9c186a0835316", "org.apache.hadoop.hive.metastore.hbase.TestHBaseImport#setupObjectStore(RawStore, String[], String[], String[], String[], String[], int)", "EM: Extract reusable method");
		// fromMethodAfter is null porque adicionou um parametro no método 
//		test("https://github.com/apache/cassandra.git", "9a3fa887cfa03c082f249d1d4003d87c14ba5d24", "org.apache.cassandra.dht.BootStrapper#getSpecifiedTokens(TokenMetadata, Collection)", "EM: Decompose method to improve readability");
//		// fromMethodAfter is null
//		test("https://github.com/neo4j/neo4j.git", "dc199688d69416da58b370ca2aa728e935fc8e0d", "org.neo4j.kernel.impl.api.state.TxState#getSortedIndexUpdates(IndexDescriptor)", "EM: Extract reusable method");
//		test("https://github.com/orfjackal/retrolambda.git", "46b0d84de9c309bca48a99e572e6611693ed5236", "net.orfjackal.retrolambda.files.ClassSaver#saveResource(Path, byte[])", "EM: Extract reusable method");
		
		// Invalid_Char_In_String
		//test("https://github.com/apache/hive.git", "102b23b16bf26cbf439009b4b95542490a082710", "org.apache.hive.beeline.Commands#handleMultiLineCmd(String)", "EM: Decompose method to improve readability");
		
		// Checkout conflict with files
		//test("https://github.com/osmandapp/Osmand.git", "e95aa8ab32a0334b9c941799060fd601297d05e4", "net.osmand.plus.activities.FavoritesListFragment#showItemPopupOptionsMenu(FavouritePoint, Activity, View)", "EM: Extract reusable method");
		//test("https://github.com/osmandapp/Osmand.git", "e95aa8ab32a0334b9c941799060fd601297d05e4", "net.osmand.plus.activities.FavoritesTreeFragment#showItemPopupOptionsMenu(FavouritePoint, View)", "EM: Extract reusable method");
		
		// npe pau ao resolver bindings
//		test("https://github.com/crate/crate.git", "c7b6a7aa878aabd6400d2df0490e1eb2b810c8f9", "io.crate.planner.consumer.ConsumingPlanner#plan(AnalyzedRelation, ConsumerContext)", "EM: Introduce alternative method signature");
		
//		// duplicate entity key (realmente tem duas vezes a classe AbstractClientCacheProxy.java)
//		test("https://github.com/hazelcast/hazelcast.git", "679d38d4316c16ccba4982d7f3ba13c147a451cb", "com.hazelcast.client.cache.impl.AbstractClientCacheProxy#getFromNearCache(Data, boolean)", "EM: Decompose method to improve readability");

//		// criou o método e comentou que era para fazer override. O método foi adicionado em uma interface tb e o javadoc contem o termo extension point
//		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeAddressMessage(byte[], int)", "EM: Enable overriding");
//		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeBlock(byte[], int)", "EM: Enable overriding");
//		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeInventoryMessage(byte[], int)", "EM: Enable overriding");
//		test("https://github.com/bitcoinj/bitcoinj.git", "12602650ce99f34cb530fc24266c23e39733b0bb", "org.bitcoinj.core.BitcoinSerializer#makeTransaction(byte[], int, int, byte[])", "EM: Enable overriding");
//		
//		// criou o método e comentou que era para fazer override, mas não fez ainda. O método é protected e existe o termo override no javadoc
//		test("https://github.com/Netflix/eureka.git", "f6212a7e474f812f31ddbce6d4f7a7a0d498b751", "com.netflix.discovery.DiscoveryClient#onRemoteStatusChanged(InstanceStatus, InstanceStatus)", "EM: Enable overriding");
		
		// não marcou como deprecated, mas a assinatura do novo método é a mesma
//		test("https://github.com/GoClipse/goclipse.git", "851ab757698304e9d8d4ae24ab75be619ddae31a", "melnorme.lang.tooling.ast.SourceRange#contains(int)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/GoClipse/goclipse.git", "851ab757698304e9d8d4ae24ab75be619ddae31a", "melnorme.lang.tooling.ast.SourceRange#contains(SourceRange)", "EM: Replace method keeping backward compatibility");
//		// O deprecated está no método extraído, não deu para entender porque: http://aserg.labsoft.dcc.ufmg.br/refactoring-explorer/commits/1159637
//		test("https://github.com/JetBrains/MPS.git", "61b5decd4a4e5e6bbdea99eb76f136ca10915b73", "jetbrains.mps.nodeEditor.cellProviders.AbstractCellListHandler#startInsertMode(EditorContext, EditorCell, boolean)", "EM: Replace method keeping backward compatibility");

		// método foi sobrescrito, mas usuário não mencionou.
//		test("https://github.com/neo4j/neo4j.git", "b83e6a535cbca21d5ea764b0c49bfca8a9ff9db4", "org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReader#query(Query)", "EM: Remove duplication");

		// missing object
//		test("https://github.com/amplab/tachyon.git", "ed966510ccf8441115614e2258aea61df0ea55f5", "tachyon.worker.block.meta.StorageDir#reserveSpace(long)", "EM: Facilitate extension");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#getMaxRequestsPerConnection(HostDistance)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#getNewConnectionThreshold(HostDistance)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#setMaxRequestsPerConnection(HostDistance, int)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/datastax/java-driver.git", "1edac0e92080e7c5e971b2d56c8753bf44ea8a6c", "com.datastax.driver.core.PoolingOptions#setNewConnectionThreshold(HostDistance, int)", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/gradle/gradle.git", "79c66ceab11dae0b9fd1dade7bb4120028738705", "org.gradle.platform.base.binary.BaseBinarySpec#getInputs()", "EM: Replace method keeping backward compatibility");
//		test("https://github.com/mongodb/morphia.git", "70a25d4afdc435e9cad4460b2a20b7aabdd21e35", "org.mongodb.morphia.TestMapping#performBasicMappingTest()", "EM: Extract reusable method");
//		test("https://github.com/restlet/restlet-framework-java.git", "7ffe37983e2f09637b0c84d526a2f824de652de4", "org.restlet.ext.apispark.internal.conversion.swagger.v2_0.Swagger2Reader#fillRepresentation(Model, String, Contract)", "EM: Extract reusable method", "EM: Facilitate extension");
//		test("https://github.com/spotify/helios.git", "dd8753cfb0f67db4dde6c5254e2df3104b635dae", "com.spotify.helios.master.ZooKeeperMasterModel#getDeploymentGroup(ZooKeeperClient, String)", "EM: Introduce alternative method signature");
//		test("https://github.com/spring-projects/spring-data-rest.git", "b7cba6a700d8c5e456cdeffe9c5bf54563eab7d3", "org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#setupMockMvc()", "EM: Enable overriding");
//		test("https://github.com/VoltDB/voltdb.git", "deb8e5ca64fcf633edbd89523af472da813b6772", "org.voltdb.regressionsuites.TestApproxCountDistinctSuite#getNormalValue(Random, double, long, long)", "EM: Extract reusable method");
//		test("https://github.com/VoltDB/voltdb.git", "e58c9c3eef4c6e44b21a97cfbd2862bb2eb4627a", "org.voltdb.sqlparser.symtab.SymbolTableAssert#hasSize(int)", "EM: Improve testability", "EM: Introduce alternative method signature");
		
		
		
		
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
				e.printStackTrace();
			}
		}
	}
	
	private void assertExtractMethodMotivation(SDModel sdModel, String method, Iterable<String> motivations) {
		final RefactoringMotivationClassifier classifier = new RefactoringMotivationClassifier(sdModel);
		SDMethod extractedMethod = sdModel.after().find(SDMethod.class, method);
		assert extractedMethod != null;
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
