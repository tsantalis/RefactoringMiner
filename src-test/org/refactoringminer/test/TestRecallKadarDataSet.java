package org.refactoringminer.test;

import static org.junit.Assert.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import org.junit.Test;

public class TestRecallKadarDataSet {

	@Test
	public void kadarDatasetRecallTest() throws Exception {
		TestBuilder test;

		test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), "kadar");

		test.project("https://github.com/MatinMan/RefactoringDatasets.git", "junit")
				.atCommit("00e584db35fdb44b58eccaff7dc5ec6b0da7547a").containsOnly(
						"Extract Method	private addMultipleFailureException(mfe MultipleFailureException) : void extracted from public addFailure(targetException Throwable) : void in class org.junit.internal.runners.model.EachTestNotifier",
						"Extract Method	private runNotIgnored(method FrameworkMethod, eachNotifier EachTestNotifier) : void extracted from protected runChild(method FrameworkMethod, notifier RunNotifier) : void in class org.junit.runners.BlockJUnit4ClassRunner",
						"Extract Method	private runIgnored(eachNotifier EachTestNotifier) : void extracted from protected runChild(method FrameworkMethod, notifier RunNotifier) : void in class org.junit.runners.BlockJUnit4ClassRunner",
						"Extract Method addTestsFromTestCase(theClass final Clss<?>) : void extracted from public TestSuite(final Class<? extends TestCase> theClass) in class in class junit.src.main.java.junit.framework.TestSuite",
						"Rename Method	public testsThatAreBothIncludedAndExcludedAreIncluded() : void renamed to public testsThatAreBothIncludedAndExcludedAreExcluded() : void in class org.junit.tests.experimental.categories.CategoryTest",
						"Rename Method	public saffSqueezeExample() : void renamed to public filterSingleMethodFromOldTestClass() : void in class org.junit.tests.experimental.max.MaxStarterTest",
						"Inline Method	private ruleFields() : List<FrameworkField> inlined to private validateFields(errors List<Throwable>) : void in class org.junit.runners.BlockJUnit4ClassRunner");
	
		test.assertExpectations();
	}
	
}
