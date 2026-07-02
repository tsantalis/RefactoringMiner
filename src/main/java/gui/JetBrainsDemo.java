package gui;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.refactoringminer.astDiff.matchers.ProjectASTDiffer;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gui.webdiff.WebDiff;

public class JetBrainsDemo {
	private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";
	private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";

	// Notes about the demos: Each demo reserves port 6789. Terminate the running process, comment out the previous demo, and execute the next demo
	public static void main(String[] args) throws Exception {
		demo1(); // This demo includes moved code between different files with overlapping refactorings
		//demo2(); // This demo includes Java code being extracted and moved to a Kotlin file
		//demo3(); // This demo includes entire test files migrated from Java to Kotlin
		//demo4(); // This demo includes nested Extract Method refactorings along with migration of traditional for loop and if statements to Java Streams API
		//demo5(); // This demo includes file migrated from Java to Kotlin (constructor code moved to init, method code moved to field get() custom accessor)
	}

	private static void demo1() throws Exception {
		String url = "https://github.com/JabRef/jabref/pull/10847";
		String repo = URLHelper.getRepo(url);
		String PR = URLHelper.getPRID(url);
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtPullRequest(repo, Integer.parseInt(PR), 10000000);
		new WebDiff(projectASTDiff).openInBrowser();
	}

	private static void demo2() {
		String url = "https://github.com/JetBrains/intellij-community/commit/2f6c8c057c950b8eaaea5a48c478c593aa977720";
		String repo = URLHelper.getRepo(url);
		String commit = URLHelper.getCommit(url);
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(repo, commit, new File(REPOS));
		new WebDiff(projectASTDiff).openInBrowser();
	}
	
	private static void demo3() {
		String url = "https://github.com/square/okhttp/commit/34bb12533b56eacd7b03c13b87dede4204d48629";
		String repo = URLHelper.getRepo(url);
		String commit = URLHelper.getCommit(url);
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(repo, commit, new File(REPOS));
		new WebDiff(projectASTDiff).openInBrowser();
	}

	@SuppressWarnings("deprecation")
	private static void demo4() throws Exception {
		List<String> fileNames = List.of("FetchAndMergeEntry", "PdfMergeMetadataImporter");
		List<String> paths = List.of("src/main/java/org/jabref/gui/mergeentries/FetchAndMergeEntry.java",
				"src/main/java/org/jabref/logic/importer/fileformat/PdfMergeMetadataImporter.java");
		
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		for(int i=0; i<fileNames.size(); i++) {
			String fileName = fileNames.get(i);
			String path = paths.get(i);
			String contentsV1 = FileUtils.readFileToString(new File(EXPECTED_PATH + fileName + "-v1.txt"));
			String contentsV2 = FileUtils.readFileToString(new File(EXPECTED_PATH + fileName + "-v2.txt"));
			fileContentsBefore.put(path, contentsV1);
			fileContentsCurrent.put(path, contentsV2);
		}
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModelForASTDiff(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModelForASTDiff(fileContentsCurrent, new LinkedHashSet<String>());
		
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
		ProjectASTDiff projectASTDiff = differ.getProjectASTDiff();
		projectASTDiff.setMetaInfo(new DiffMetaInfo("v1 -> v2", ""));
		new WebDiff(projectASTDiff).openInBrowser();
	}
	
	private static void demo5() {
		String url = "https://github.com/carlphilipp/chicago-commutes/commit/3638be60c8bd144b968f044c0ded218e19697d69";
		String repo = URLHelper.getRepo(url);
		String commit = URLHelper.getCommit(url);
		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(repo, commit, new File(REPOS));
		new WebDiff(projectASTDiff).openInBrowser();
	}
}
