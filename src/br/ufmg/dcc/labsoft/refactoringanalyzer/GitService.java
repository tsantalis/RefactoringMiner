package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.eclipse.jgit.lib.Repository;

public interface GitService {

	Repository cloneIfNotExists(String folder, String cloneUrl, String branch) throws Exception;

}
