package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;

import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refdetector.GitHistoryRefactoringDetector;
import br.ufmg.dcc.labsoft.refdetector.GitService;
import br.ufmg.dcc.labsoft.refdetector.GitServiceImpl;
import br.ufmg.dcc.labsoft.refdetector.GitHistoryRefactoringDetectorImpl;

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

		GitHistoryRefactoringDetector detector = new GitHistoryRefactoringDetectorImpl();
		detector.detectAll(repo, project.getDefault_branch(), new AnalyzeProjectsHandler(db, project));
		repo.close();
	}
}
