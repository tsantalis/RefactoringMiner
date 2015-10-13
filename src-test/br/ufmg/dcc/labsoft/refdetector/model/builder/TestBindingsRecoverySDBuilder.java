package br.ufmg.dcc.labsoft.refdetector.model.builder;

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

public class TestBindingsRecoverySDBuilder {

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
	public void testImproveTestability() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\JGroups",
				"https://github.com/belaban/JGroups.git");
		
		analyzer.detectAtCommit(repo, "f1533756133dec84ce8218202585ac85904da7c9", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "org.jgroups.auth.FixedMembershipToken#isInMembersList(IpAddress)", Motivation.EM_IMPROVE_TESTABILITY);
			}
		});
	}
	
	@Test
	public void testReplaceMethod() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
			    "C:\\tmp\\truth",
			    "https://github.com/google/truth.git");
		
		analyzer.detectAtCommit(repo, "200f1577d238a6d3fbcf99cb2a2585b2071214a6", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertExtractMethodMotivation(sdModel, "com.google.common.truth.IterableSubject#isOrdered()", Motivation.EM_PRESERVE_BACKWARD_COMPATIBILITY);
				assertExtractMethodMotivation(sdModel, "com.google.common.truth.IterableSubject#isOrdered(Comparator<? super T>)", Motivation.EM_PRESERVE_BACKWARD_COMPATIBILITY);
			}
		});
	}
	
	private void assertExtractMethodMotivation(SDModel sdModel, String method, Motivation ... motivations) {
		final RefactoringMotivationClassifier classifier = new RefactoringMotivationClassifier();
		SDMethod extractedMethod = sdModel.setAfter().find(SDMethod.class, method);
		Set<Motivation> tags = classifier.classifyExtractMethod(extractedMethod);
		Motivation[] actual = tags.toArray(new Motivation[tags.size()]);
		Assert.assertArrayEquals(motivations, actual);
	}
}
