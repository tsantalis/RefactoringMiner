package org.refactoringminer.test;

import java.util.LinkedList;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch.Diff;

import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.junit.Test;
import org.refactoringminer.rm2.analysis.DiffUtils;

public class TestDiff {

	@Test
	public void test1() {
		String s1 = "private static boolean addCoursesFromStepic(List<CourseInfo> result, int pageNumber) throws IOException {\n    final String url = pageNumber == 0 ? \"courses\" : \"courses?page=\" + String.valueOf(pageNumber);\n   }";
		String s2 =                                              "private static void other(List<String> someList, int page) {\n    final String url = pageNumber == 0 ? \"courses\" : \"courses?page=\" + String.valueOf(pageNumber);\n   }";
		
		LinkedList<Diff> diffs = DiffUtils.tokenBasedDiff(s1, s2);

		List<String> deleted = DiffUtils.getDeletedXorInsertedLines(diffs, true, 0.8);
		System.out.println("Deleted lines:");
	    for (String l: deleted) {
	    	System.out.println(l);
	    }
		
	    List<String> inserted = DiffUtils.getDeletedXorInsertedLines(diffs, false, 0.8);
		System.out.println("Inserted lines:");
	    for (String l: inserted) {
	    	System.out.println(l);
	    }
	    
		//System.out.println("<code>" + dmp.diff_prettyHtml(diffs) + "</code>");
	}


	@Test
	public void test2() {
		
		String s1 = "private static boolean addCoursesFromStepic(List<CourseInfo> result, int pageNumber) throws IOException {\n    final String url = pageNumber == 0 ? \"courses\" : \"courses?page=\" + String.valueOf(pageNumber);\n   }";
		String s2 = "private static void other(List<String> someList, int page) {\n    final String url = pageNumber == 0 ? \"courses\" : \"courses?page=\" + String.valueOf(pageNumber);\n   }";
		
		DiffAlgorithm algo = DiffAlgorithm.getAlgorithm(SupportedAlgorithm.MYERS);
		
		EditList r = algo.diff(RawTextComparator.DEFAULT, new RawText(s1.getBytes()), new RawText(s2.getBytes()));
		
	    for (Edit edit: r) {
	    	System.out.println(edit);
	    }
	}
	
}
