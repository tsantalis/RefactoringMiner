package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public class AnalyzeProjects {

	public static void main(String[] args) {
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
		
		try {
			File workingDir = new File("tmp");
			if (args.length > 0) {
				workingDir = new File(args[0]);
			}
			if (!workingDir.exists()) {
				workingDir.mkdir();
			}
			// setting up a FileAppender dynamically...
			String logFilePath = workingDir.getPath() + "/rd." + pid + ".log";
			logger.info("Log file: " + logFilePath);
			
			PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %m%n");
			FileAppender appender = new FileAppender(layout, logFilePath, true);
			org.apache.log4j.Logger.getRootLogger().addAppender(appender);
			
			GitProjectAnalyzer analyzer = new GitProjectAnalyzer(workingDir, db);
			ProjectGit project = null;
			while ((project = db.findNonAnalyzedProjectAndLock(pid)) != null) {
				analyzer.analyzedProject(project);
			}
			logger.info("No more projects to analyze");
			
		} catch (Exception e) {
			logger.error("Error", e);
		}
	}

}
