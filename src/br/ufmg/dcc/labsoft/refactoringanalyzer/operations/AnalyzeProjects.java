package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public class AnalyzeProjects {

	public static void main(String[] args) throws Exception {
		final Logger logger = LoggerFactory.getLogger(AnalyzeProjects.class);
		final Database db = new Database();
		final String pid = ManagementFactory.getRuntimeMXBean().getName();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.releaseLocks(pid);
                logger.info("Locks released");
            }
        });
		
		GitProjectAnalyzer analyzer = new GitProjectAnalyzer(new File("tmp"), db);
		ProjectGit project = null;
		while ((project = db.findNonAnalyzedProjectAndLock(pid)) != null) {
			analyzer.analyzedProject(project);
		}
		logger.info("No more projects to analyze");
	}

}
