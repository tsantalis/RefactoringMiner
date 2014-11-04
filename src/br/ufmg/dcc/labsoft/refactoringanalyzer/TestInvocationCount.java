package br.ufmg.dcc.labsoft.refactoringanalyzer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Receives two source folders as arguments and detects refactoring operations performed between the given versions of the code.
 */
public class TestInvocationCount {

	public static void main(String[] args) throws FileNotFoundException {
		//runProject("tmp/refactoring-toy-example", "https://github.com/danilofes/refactoring-toy-example.git");
		runProject("tmp/junit", "https://github.com/junit-team/junit.git");
		//runProject("tmp/elasticsearch", "https://github.com/elasticsearch/elasticsearch.git");
		//runProject("tmp/guava", "https://github.com/google/guava.git");
	}

	private static void runProject(String folder, String cloneUrl) throws FileNotFoundException {
	    final GitService gitService = new GitServiceImpl();
		gitService.cloneIfNotExists(folder, cloneUrl);
		
		final MethodInvocationInfoSummary s = new MethodInvocationInfoSummary();
		new RefactoringDetectorImpl(folder, new RefactoringHandler() {
			@Override
			public void handleCurrent(UMLModel model) {
				s.analyzeCurrent(model);
			}
			@Override
			public void handleDiff(Revision prevRevision, UMLModel prevModel, Revision curRevision, UMLModel curModel) {
			    s.analyzeRevision(prevRevision, prevModel);
			    s.analyzeRevision(curRevision, curModel);
			}
			@Override
			public void handleRefactoring(Revision revision, Refactoring refactoring) {
			    s.analyzeRefactoring(revision, refactoring);
			}
		}).detectAll();
		
		File outputFile = new File(folder + ".csv");
		s.print(new PrintStream(outputFile));
		System.out.println("Arquivo escrito em: " + outputFile.toString());
    }

}
