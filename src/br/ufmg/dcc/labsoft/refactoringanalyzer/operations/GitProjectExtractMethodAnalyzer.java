package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.GitService;
import br.ufmg.dcc.labsoft.refactoringanalyzer.GitServiceImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.MethodInvocationInfoSummary;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringDetectorImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringHandler;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RevisionGit;

public class GitProjectExtractMethodAnalyzer {

	Logger logger = LoggerFactory.getLogger(GitProjectExtractMethodAnalyzer.class);
	
	private File workingDir = new File("tmp");
	private Database db = new Database();

	public static void main(String[] args) throws Exception {
		String[] projects = new String[] {
			"https://github.com/nathanmarz/storm.git",
			"https://github.com/github/android.git",
			"https://github.com/loopj/android-async-http.git",
			"https://github.com/square/picasso.git",
			"https://github.com/clojure/clojure.git",
			"https://github.com/facebook/facebook-android-sdk.git",
			"https://github.com/junit-team/junit.git",
			"https://github.com/perwendel/spark.git",
			"https://github.com/yui/yuicompressor.git",
			"https://github.com/jhy/jsoup.git"
		};
		GitProjectExtractMethodAnalyzer analyzer = new GitProjectExtractMethodAnalyzer();
		for (String cloneUrl : projects) {
			analyzer.analyzeProject(cloneUrl);
		}
	}

	public void analyzeProject(String cloneUrl) throws Exception {
		final ProjectGit project = db.getProjectByCloneUrl(cloneUrl);
		if (project == null) {
			throw new IllegalArgumentException("Project not found in database: " + cloneUrl);
		}

		GitService gitService = new GitServiceImpl();
		File projectFile = new File(workingDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), cloneUrl, project.getDefault_branch());

		File projectFolder = repo.getDirectory().getParentFile();
		ASTParser parser = RefactoringDetectorImpl.buildAstParser(projectFolder, true);
		UMLModel currentUMLModel = new ASTReader2(projectFolder, parser, true).getUmlModel();
		final MethodInvocationInfoSummary s = new MethodInvocationInfoSummary();
		s.analyzeCurrent(currentUMLModel);

		logger.info("Project {}", cloneUrl);
		RefactoringDetectorImpl detector = new RefactoringDetectorImpl(true);
		List<RevisionGit> revisions = db.findRevisionsByProjectAndExtractMethod(project);
		for (final RevisionGit rev : revisions) {
			detector.detectOne(parser, repo, rev.getIdCommit(), rev.getIdCommitParent(), new RefactoringHandler() {
				@Override
				public void handleDiff(RevCommit prevRevision, UMLModel prevModel, RevCommit curRevision, UMLModel curModel, List<Refactoring> refactorings) {
					s.analyzeRevision(rev.getIdCommit(), curModel, refactorings);
					logger.info("Revision {} analyzed", rev.getIdCommit());
				}
			});
		}
		repo.close();

		File outputFile = new File(projectFolder.getName() + ".csv");
		PrintStream out = new PrintStream(outputFile);
		s.print(out);
		out.close();
		logger.info("Output file written at {}", outputFile.toString());
	}

}
