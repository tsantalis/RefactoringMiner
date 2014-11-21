package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;

import org.eclipse.jgit.lib.Repository;

import br.ufmg.dcc.labsoft.refactoringanalyzer.GitService;
import br.ufmg.dcc.labsoft.refactoringanalyzer.GitServiceImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringDetector;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringDetectorImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public class GitProjectAnalyzer {

	private File workingDir = new File("tmp");
	private Database db = new Database();

	public static void main(String[] args) throws Exception {
		new GitProjectAnalyzer().analyzeProject(args[0]);
	}

	public void analyzeProject(String cloneUrl) throws Exception {
		ProjectGit project = db.getProjectByCloneUrl(cloneUrl);
		if (project == null) {
			throw new IllegalArgumentException("Project not found in database: " + cloneUrl);
		}
		if (project.isAnalyzed()) {
			System.out.println("Project already analyzed: " + cloneUrl);
		}
		
		GitService gitService = new GitServiceImpl();
		File projectFile = new File(workingDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), cloneUrl, project.getDefault_branch());
		
		RefactoringDetector detector = new RefactoringDetectorImpl();
		// TODO
	}

}
