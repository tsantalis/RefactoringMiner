package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestExtractAndInlineMethod {

	@Test
	public void testChangeSignature() throws Exception {
		TestBuilder test = new TestBuilder();
		// https://github.com/checkstyle/checkstyle/commit/d282d5b8db9eba5943d1cb0269315744d5344a47
		// * Inline Method	private validate(details Details, rcurly DetailAST, lcurly DetailAST) : void inlined to public visitToken(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.blocks.RightCurlyCheck
		// * Extract Method	private validate(details Details, bracePolicy RightCurlyOption, shouldStartLine boolean, targetSourceLine String) : String extracted from public visitToken(ast DetailAST) : void in class com.puppycrawl.tools.checkstyle.checks.blocks.RightCurlyCheck
		test.project("https://github.com/checkstyle/checkstyle.git", "master").atCommit("d282d5b8db9eba5943d1cb0269315744d5344a47").containsNothing();
		test.assertExpectations();
	}
	
}
