package br.ufmg.dcc.labsoft.refactoringanalyzer;

public interface GitService {

	void cloneIfNotExists(String folder, String cloneUrl);

}
