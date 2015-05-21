package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.eclipse.jgit.lib.Repository;

public interface RefactoringDetector {

	void detectAll(Repository repository, String branch, RefactoringHandler handler) throws Exception;

	void fetchAndDetectNew(Repository repository, RefactoringHandler handler) throws Exception;

}
