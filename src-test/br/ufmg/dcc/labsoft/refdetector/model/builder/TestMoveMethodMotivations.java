package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;

import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.builder.RefactoringMotivationClassifier.Motivation;

public class TestMoveMethodMotivations {

	private GitService gitService = new GitServiceImpl();
	private GitHistoryStructuralDiffAnalyzer analyzer = new GitHistoryStructuralDiffAnalyzer();

	@Test
	public void testReuse() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
			    "C:\\tmp\\intellij-community",
			    "https://github.com/JetBrains/intellij-community.git");
		
		analyzer.detectAtCommit(repo, "ce5f9ff96e2718e4014655f819314ac2ac4bd8bf", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveMethodMotivation(sdModel, "com.intellij.execution.runners.ExecutionUtil#getLiveIndicator(Icon)", Motivation.MM_REUSE);
			}
		});
	}

	@Test
	public void testEnableOverriding() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\intellij-community",
				"https://github.com/JetBrains/intellij-community.git");
		
		analyzer.detectAtCommit(repo, "6ad1dcbfef36821a71cbffa301c58d1c3ffe8d62", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveMethodMotivation(sdModel, "com.intellij.testFramework.LightProjectDescriptor#createMainModule(Project)", Motivation.MM_ENABLE_OVERRIDING);
			}
		});
	}

	@Test
	public void testRemoveDuplication() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\blueflood",
				"https://github.com/rackerlabs/blueflood.git");
		
		analyzer.detectAtCommit(repo, "c76e6e1f27a6697b3b88ad4ed710441b801afb3b", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveMethodMotivation(sdModel, "com.rackspacecloud.blueflood.http.DefaultHandler#sendResponse(ChannelHandlerContext, HttpRequest, String, HttpResponseStatus)", Motivation.MM_REMOVE_DUPLICATION);
			}
		});
	}

	private void assertMoveMethodMotivation(SDModel sdModel, String movedMethodKey, Motivation ... motivations) {
		final RefactoringMotivationClassifier classifier = new RefactoringMotivationClassifier(sdModel);
		SDMethod moved = sdModel.after().find(SDMethod.class, movedMethodKey);
		assert moved != null;
		Set<Motivation> tags = classifier.classifyMoveMethod(moved);
		Motivation[] actual = tags.toArray(new Motivation[tags.size()]);
		Arrays.sort(motivations);
		Arrays.sort(actual);
		Assert.assertEquals(Arrays.toString(motivations), Arrays.toString(actual));
	}
}
