package org.refactoringminer.test;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.rm2.analysis.GitHistoryStructuralDiffAnalyzer;
import org.refactoringminer.rm2.analysis.RefactoringMotivationClassifier;
import org.refactoringminer.rm2.analysis.StructuralDiffHandler;
import org.refactoringminer.rm2.analysis.RefactoringMotivationClassifier.Motivation;
import org.refactoringminer.rm2.model.SDMethod;
import org.refactoringminer.rm2.model.SDModel;
import org.refactoringminer.util.GitServiceImpl;

public class TestExtractMethodMotivations {

	private GitService gitService = new GitServiceImpl();
	private GitHistoryStructuralDiffAnalyzer analyzer = new GitHistoryStructuralDiffAnalyzer();

	@Test
	public void testExtractReusableMethod() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
			    "C:\\tmp\\jmonkeyengine",
			    "https://github.com/jMonkeyEngine/jmonkeyengine.git");
		
		analyzer.detectAtCommit(repo, "5989711f7315abe4c3da0f1516a3eb3a81da6716", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "com.jme3.gde.materialdefinition.editor.DraggablePanel#movePanel(int, int)", Motivation.EM_REUSE);
			}
		});
	}

	@Test
	public void testRemoveDuplication() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\neo4j",
				"https://github.com/neo4j/neo4j.git");
		
		analyzer.detectAtCommit(repo, "b83e6a535cbca21d5ea764b0c49bfca8a9ff9db4", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "org.neo4j.kernel.api.impl.index.LuceneIndexAccessorReader#query(Query)",
						Motivation.EM_REUSE, Motivation.EM_REMOVE_DUPLICATION, Motivation.EM_ENABLE_OVERRIDING);
			}
		});
	}

	@Test
	public void testImproveTestability() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\JGroups",
				"https://github.com/belaban/JGroups.git");
		
		analyzer.detectAtCommit(repo, "f1533756133dec84ce8218202585ac85904da7c9", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "org.jgroups.auth.FixedMembershipToken#isInMembersList(IpAddress)", 
						Motivation.EM_IMPROVE_TESTABILITY);
			}
		});
	}
	
	@Test
	public void testIntroduceAlternativeMethodSignature() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\realm-java",
				"https://github.com/realm/realm-java.git");
		
		analyzer.detectAtCommit(repo, "6cf596df183b3c3a38ed5dd9bb3b0100c6548ebb", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "io.realm.examples.realmmigrationexample.MigrationExampleActivity#showStatus(String)", 
						Motivation.EM_INTRODUCE_ALTERNATIVE_SIGNATURE);
			}
		});
	}

	@Test
	public void testReplaceMethodPreservingBackwardCompatibility() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
			    "C:\\tmp\\truth",
			    "https://github.com/google/truth.git");
		
		analyzer.detectAtCommit(repo, "200f1577d238a6d3fbcf99cb2a2585b2071214a6", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "com.google.common.truth.IterableSubject#isOrdered()", 
						Motivation.EM_PRESERVE_BACKWARD_COMPATIBILITY);
				assertExtractMethodMotivation(sdModel, "com.google.common.truth.IterableSubject#isOrdered(Comparator)", 
						Motivation.EM_PRESERVE_BACKWARD_COMPATIBILITY);
			}
		});
	}

//	@Test
//	public void testEnableOverriding() throws Exception {
//		Repository repo = gitService.cloneIfNotExists(
//				"C:\\tmp\\bitcoinj",
//				"https://github.com/bitcoinj/bitcoinj.git");
//		
//		analyzer.detectAtCommit(repo, "12602650ce99f34cb530fc24266c23e39733b0bb", new StructuralDiffHandler() {
//			@Override
//			public void handle(RevCommit commitData, SDModel sdModel) {
//				assertExtractMethodMotivation(sdModel, "org.bitcoinj.core.BitcoinSerializer#makeAddressMessage(byte[], int)", Motivation.EM_ENABLE_OVERRIDING);
//			}
//		});
//	}

	@Test
	public void testEnableOverriding() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\quasar",
				"https://github.com/puniverse/quasar.git");
		
		analyzer.detectAtCommit(repo, "c22d40fab8dfe4c5cad9ba582caf0855ff64b324", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "co.paralleluniverse.strands.channels.reactivestreams.ChannelSubscriber#failedSubscribe(Subscription)", 
						Motivation.EM_ENABLE_OVERRIDING);
			}
		});
	}
	
	@Test
	public void testEnableRecursion() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\liferay-plugins",
				"https://github.com/liferay/liferay-plugins.git");
		
		analyzer.detectAtCommit(repo, "7c7ecf4cffda166938efd0ae34830e2979c25c73", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "com.liferay.sync.hook.listeners.ResourcePermissionModelListener#updateSyncDLObject(SyncDLObject)", 
						Motivation.EM_ENABLE_RECURSION);
			}
		});
	}
	
	private void assertExtractMethodMotivation(SDModel sdModel, String method, Motivation ... motivations) {
		final RefactoringMotivationClassifier classifier = new RefactoringMotivationClassifier(sdModel);
		SDMethod extractedMethod = sdModel.after().findByName(SDMethod.class, method);
		Set<Motivation> tags = classifier.classifyExtractMethod(extractedMethod);
		Motivation[] actual = tags.toArray(new Motivation[tags.size()]);
		Arrays.sort(motivations);
		Arrays.sort(actual);
		Assert.assertEquals(Arrays.toString(motivations), Arrays.toString(actual));
	}
}
