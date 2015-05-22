package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;

import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.GitService;
import br.ufmg.dcc.labsoft.refactoringanalyzer.GitServiceImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringDetector;
import br.ufmg.dcc.labsoft.refactoringanalyzer.RefactoringDetectorImpl;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public class AnalyzeProjects extends TaskWithProjectLock {

	private static Logger logger = LoggerFactory.getLogger(AnalyzeProjects.class);
	static Pid pid = new Pid();
	
	public static void main(String[] args) {
		try {
			AnalyzeProjects task = new AnalyzeProjects(args);
			task.doTask(pid);
		} catch (Exception e) {
			logger.error("Fatal error", e);
		}
	}

	public AnalyzeProjects(String[] args) throws Exception {
		super(new Database());
		initWorkingDir(args, pid);
	}

	@Override
	protected ProjectGit findNextProject(Database db, Pid pid) throws Exception {
		return db.findNonAnalyzedProjectAndLock(pid.toString());
	}

	@Override
	protected void doTask(Database db, Pid pid, ProjectGit project) throws Exception {
		this.analyzeProject(db, project);
	}

	public void analyzeProject(final Database db, final ProjectGit project) throws Exception {
		GitService gitService = new GitServiceImpl();
		File projectFile = new File(workingDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl()/*, project.getDefault_branch()*/);

		RefactoringDetector detector = new RefactoringDetectorImpl();
		detector.detectAll(repo, project.getDefault_branch(), new AnalyzeProjectsHandler(db, project));
		repo.close();
	}
}
