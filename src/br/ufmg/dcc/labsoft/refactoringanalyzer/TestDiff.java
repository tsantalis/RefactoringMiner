package br.ufmg.dcc.labsoft.refactoringanalyzer;
import gr.uom.java.xmi.ASTReader;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.util.List;

/**
 * Receives two source folders as arguments and detects refactoring operations performed between the given versions of the code.
 */
public class TestDiff {

	public static void main(String[] args) {
//		if (args.length != 2) {
//			System.err.println("Missing arguments: v0-folder v1-folder");
//			return;
//		}

//		String version0SrcFolder = "test/diffs/test3/egrefctfowlerV1";
//		String version1SrcFolder = "test/diffs/test3/egrefctfowlerV2";
		String version0SrcFolder = "test/v0";
		String version1SrcFolder = "test/v1";
//		String version0SrcFolder = args[0];
//		String version1SrcFolder = args[1];
		UMLModel model0 = new ASTReader(new File(version0SrcFolder)).getUmlModel();
		UMLModel model1 = new ASTReader(new File(version1SrcFolder)).getUmlModel();
		
		UMLModelDiff modelDiff = model0.diff(model1);
		List<Refactoring> refactorings = modelDiff.getRefactorings();
		for (Refactoring refactoring : refactorings) {
			System.out.println(refactoring.toString());
		}
	}

}
