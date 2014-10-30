package br.ufmg.dcc.labsoft.refactoringanalyzer;
import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.MethodInvocationInfo;
import gr.uom.java.xmi.MethodInvocationInfo.MethodInfo;

import java.io.File;

/**
 * Receives two source folders as arguments and detects refactoring operations performed between the given versions of the code.
 */
public class TestInvocationCount {

	public static void main(String[] args) {
		String version0SrcFolder = "test/v1";
		String version1SrcFolder = "test/v2";
		
		MethodInvocationInfo model1 = new ASTReader2(new File(version1SrcFolder)).getMethodInvocationInfo();
		printMethods(model1);
	}

	private static void printMethods(MethodInvocationInfo map) {
		System.out.println("Internal invocations: " + map.getInternalInvocations());
		System.out.println("External invocations: " + map.getExternalInvocations());
		for (MethodInfo methodInfo : map.getMethodInfoCollection()) {
			System.out.println(methodInfo.getBindingKey() + " " + methodInfo.getCount());
		}
	}

}
