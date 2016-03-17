package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public class ProjectCommitCounter {

	private static Logger logger = LoggerFactory.getLogger(ProjectCommitCounter.class);
	private File workingDir;
	private Database db;
	
	public static void main(String[] args) throws Exception {
		final Database db = new Database();
		final String pid = ManagementFactory.getRuntimeMXBean().getName();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.releaseLocks(pid);
                logger.info("Locks released");
            }
        });
		
		ProjectCommitCounter analyzer = new ProjectCommitCounter(new File("tmp"), db);
		ProjectGit project = null;
		while ((project = db.findNonCountedProjectAndLock(pid)) != null) {
			analyzer.countCommitsFromProject(project);
		}
		logger.info("No more projects to fetch");
	}

	public ProjectCommitCounter(File workingDir, Database db) {
		this.workingDir = workingDir;
		this.db = db;
	}

	public void countCommitsFromProject(final ProjectGit project) throws Exception {
		if (project.isAnalyzed() || project.getCommits_count() > 0) {
			logger.info("Project already fetched: {}", project.getCloneUrl());
			return;
		}
		GitService gitService = new GitServiceImpl();
		File projectFile = new File(workingDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl()/*, project.getDefault_branch()*/);
		int count = gitService.countCommits(repo, project.getDefault_branch());
		
		project.setCommits_count(count);
		project.setRunning_pid(null);
		db.update(project);
		logger.info("Project {} has {} commits", project.getCloneUrl(), count);
	}

}
