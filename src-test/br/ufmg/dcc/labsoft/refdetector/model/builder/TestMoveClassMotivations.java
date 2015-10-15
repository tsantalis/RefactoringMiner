package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;

import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;
import br.ufmg.dcc.labsoft.refdetector.model.builder.RefactoringMotivationClassifier.Motivation;

public class TestMoveClassMotivations {

	private GitService gitService = new GitServiceImpl();
	private GitHistoryStructuralDiffAnalyzer analyzer = new GitHistoryStructuralDiffAnalyzer();

	@Test
	public void testIntroduceSubpackage() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
			    "C:\\tmp\\WordPress-Android",
			    "https://github.com/wordpress-mobile/WordPress-Android.git");
		
		analyzer.detectAtCommit(repo, "9dc3cbd59a20f03406f295a4a8f3b8676dbc939e", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveClassMotivation(sdModel, "org.wordpress.android.ui.prefs.notifications.NotificationsPreference", Motivation.MC_INTRODUCE_SUBPACKAGE);
				assertMoveClassMotivation(sdModel, "org.wordpress.android.ui.prefs.notifications.NotificationsSettingsActivity", Motivation.MC_INTRODUCE_SUBPACKAGE);
				assertMoveClassMotivation(sdModel, "org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment", Motivation.MC_INTRODUCE_SUBPACKAGE);
			}
		});
	}

	@Test
	public void testRenamePackage() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\gradle",
				"https://github.com/gradle/gradle.git");
		
		analyzer.detectAtCommit(repo, "ba1da95200d080aef6251f13ced0ca67dff282be", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveClassMotivation(sdModel, "org.gradle.tooling.test.TestExecutionException", Motivation.MC_RENAME_PACKAGE);
			}
		});
	}

	@Test
	public void testConvertToTopLevelType() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\hydra",
				"https://github.com/addthis/hydra.git");
		
		analyzer.detectAtCommit(repo, "7fea4c9d5ee97d4a61ad985cadc9c5c0ab2db780", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveClassMotivation(sdModel, "com.addthis.hydra.job.spawn.balancer.HostAndScore", Motivation.MC_CONVERT_TO_TOP_LEVEL);
				assertMoveClassMotivation(sdModel, "com.addthis.hydra.job.spawn.balancer.HostScore", Motivation.MC_CONVERT_TO_TOP_LEVEL);
				assertMoveClassMotivation(sdModel, "com.addthis.hydra.job.spawn.balancer.JobTaskItem", Motivation.MC_CONVERT_TO_TOP_LEVEL);
			}
		});
	}

	@Test
	public void testConvertToInnerClass() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\HikariCP",
				"https://github.com/brettwooldridge/HikariCP.git");
		
		analyzer.detectAtCommit(repo, "cd8c4d578a609bdd6395d3a8c49bfd19ed700dea", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveClassMotivation(sdModel, "com.zaxxer.hikari.util.ClockSource.MillisecondClockSource", Motivation.MC_CONVERT_TO_INNER);
				assertMoveClassMotivation(sdModel, "com.zaxxer.hikari.util.ClockSource.NanosecondClockSource", Motivation.MC_CONVERT_TO_INNER);
			}
		});
	}
	
	@Test
	public void testRemoveFromDeprecatedContainer() throws Exception {
		Repository repo = gitService.cloneIfNotExists(
				"C:\\tmp\\cassandra",
				"https://github.com/apache/cassandra.git");
		
		analyzer.detectAtCommit(repo, "446e2537895c15b404a74107069a12f3fc404b15", new StructuralDiffHandler() {
			@Override
			public void handle(RevCommit commitData, SDModel sdModel) {
				assertMoveClassMotivation(sdModel, "org.apache.cassandra.hadoop.cql3.CqlInputFormat.SplitCallable", Motivation.MC_REMOVE_FROM_DEPRECATED_CONTAINER);
				assertMoveClassMotivation(sdModel, "org.apache.cassandra.hadoop.cql3.CqlBulkRecordWriter.NullOutputHandler", Motivation.MC_REMOVE_FROM_DEPRECATED_CONTAINER);
			}
		});
	}
	
	private void assertMoveClassMotivation(SDModel sdModel, String movedClass, Motivation ... motivations) {
		final RefactoringMotivationClassifier classifier = new RefactoringMotivationClassifier(sdModel);
		SDType movedType = sdModel.after().find(SDType.class, movedClass);
		Set<Motivation> tags = classifier.classifyMoveClass(movedType);
		Motivation[] actual = tags.toArray(new Motivation[tags.size()]);
		Arrays.sort(motivations);
		Arrays.sort(actual);
		Assert.assertEquals(Arrays.toString(motivations), Arrays.toString(actual));
	}
}
