package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public abstract class TaskWithProjectLock {

	static final Logger logger = LoggerFactory.getLogger(TaskWithProjectLock.class);
	
	protected Database db;
	protected File workingDir = new File("tmp");

	public TaskWithProjectLock(Database db) {
		this.db = db;
	}

	public final void doTask(final Pid pid) {
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.releaseLocks(pid.toString());
                logger.info("Locks released at shutdown");
            }
        });
		
		try {
			ProjectGit project = null;
			while ((project = this.findNextProject(db, pid)) != null) {
				try {
					this.doTask(db, pid, project);
				} catch (Exception e) {
					// This may be a temporary connection problem with github, so log the erro and move on ...
					logger.warn("Skiping project due to error", e);
				}
				finally {
					db.releaseLocks(pid.toString());
					logger.info("Locks released");
				}
			}
			logger.info("No more projects");
			
		} catch (Exception e) {
			logger.error("Fatal error", e);
		}
	}
	
	protected abstract void doTask(Database db, Pid pid, ProjectGit project) throws Exception;

	protected abstract ProjectGit findNextProject(Database db, Pid pid) throws Exception;

	protected void initWorkingDir(String[] args, Pid pid) throws IOException {
		if (args.length > 0) {
			workingDir = new File(args[0]);
		}
		if (!workingDir.exists()) {
			workingDir.mkdir();
		}
		String logFilePath = addFileLogger(workingDir, logger, pid.toString());
		logger.info("Log file: " + logFilePath);
	}

	private String addFileLogger(File workingDir, final Logger logger,  final String pid) throws IOException {
		// setting up a FileAppender dynamically...
		String logFilePath = workingDir.getPath() + "/rd." + pid + ".log";
		PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %m%n");
		FileAppender appender = new FileAppender(layout, logFilePath, true);
		org.apache.log4j.Logger.getRootLogger().addAppender(appender);
		return logFilePath;
	}
}
