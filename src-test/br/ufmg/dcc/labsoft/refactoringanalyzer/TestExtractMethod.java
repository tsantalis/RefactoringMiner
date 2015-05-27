package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.UMLModelSet;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestExtractMethod {

	@Test
	public void testExtractFalse() throws Exception {
		this.assertExpected("test/extract/false0", "test/extract/false1");
	}

	@Test
	public void testExtractDuplication() throws Exception {
		this.assertExpected("test/extract/duplication0", "test/extract/duplication1",
			"Extract Method	public getSum(l List<Integer>) : int extracted from public method1(l List<Integer>) : String in class foo.A",
			"Extract Method	public getSum(l List<Integer>) : int extracted from public method2(l List<Integer>) : String in class foo.A"
		);
	}

	@Test
	public void testInlineDuplication() throws Exception {
		this.assertExpected("test/extract/duplication1", "test/extract/duplication0",
			"Inline Method	public getSum(l List<Integer>) : int inlined to public method1(l List<Integer>) : String in class foo.A",
			"Inline Method	public getSum(l List<Integer>) : int inlined to public method2(l List<Integer>) : String in class foo.A"
		);
	}

	@Test
	public void testExtractOverride() throws Exception {
		this.assertExpected("test/extract/override0", "test/extract/override1",
			"Extract Method	protected getSum(l List<Integer>) : int extracted from public method1(l List<Integer>) : String in class foo.A"
		);
	}

	@Test
	public void testExtractMove() throws Exception {
		this.assertExpected("test/extract/move0", "test/extract/move1",
			"Extract And Move Method	public getSum(l List<Integer>) : int extracted from public method1(l List<Integer>) : String in class foo.A & moved to class foo.B",
			"Extract And Move Method	public getSum(l List<Integer>) : int extracted from public method1B(l List<Integer>) : String in class foo.A & moved to class foo.B",
			"Extract And Move Method	public getSumSquared(l List<Integer>) : int extracted from public method2(l List<Integer>, c C) : String in class foo.A & moved to class foo.C"
		);
	}

	@Test
	public void testExtractPullUp() throws Exception {
		this.assertExpected("test/extract/pullup0", "test/extract/pullup1",
			"Extract And Move Method	public getSum(l List<Integer>) : int extracted from public method1(l List<Integer>) : String in class foo.B & moved to class foo.A"
		);
	}

	private void assertExpected(String version0SrcFolder, String version1SrcFolder, String ... expectedRefactorings) throws Exception {
		UMLModelSet model0 = new ASTReader2(new File(version0SrcFolder)).getUmlModelSet();
		UMLModelSet model1 = new ASTReader2(new File(version1SrcFolder)).getUmlModelSet();
		
		List<Refactoring> actual = model0.detectRefactorings(model1);
		Set<String> expected = new HashSet<>();
		for (String ref : expectedRefactorings) {
			expected.add(ref);
		}
		for (Refactoring ref : actual) {
			if (!expected.contains(ref.toString())) {
				Assert.fail("Refactoring not expected: " + ref.toString());
			} else {
				expected.remove(ref.toString());
			}
		}
		for (String ref : expected) {
			Assert.fail("Refactoring expected but not found: " + ref.toString());
		}
	}

}
