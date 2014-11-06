package br.ufmg.dcc.labsoft.refactoringanalyzer;
import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.UMLModel;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Receives two source folders as arguments and detects refactoring operations performed between the given versions of the code.
 */
public class TestInvocationCountOneRevision {

	public static void main(String[] args) throws FileNotFoundException {
		runProject("../junit");
		//runProject("test/bindings");
	}

	private static void runProject(String folder) throws FileNotFoundException {
		
		ASTReader2 reader = new ASTReader2(new File(folder));
		UMLModel model = reader.getUmlModel();
		
		final MethodInvocationInfoSummary s = new MethodInvocationInfoSummary();
		s.analyzeCurrent(model);
		
		s.print(System.out);
		System.out.println("Terminou.");
    }

}
