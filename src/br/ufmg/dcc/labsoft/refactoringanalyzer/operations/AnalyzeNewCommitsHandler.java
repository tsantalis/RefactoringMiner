package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.util.Date;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

class AnalyzeNewCommitsHandler extends AnalyzeProjectsHandler {

	AnalyzeNewCommitsHandler(Database db, ProjectGit project) {
		super(db, project);
	}

	@Override
	public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
		project.setLast_update(new Date());
		db.update(project);
	}

}