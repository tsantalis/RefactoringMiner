package gui;

import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.perforce.PerforceHistoryRefactoringMinerImpl;

import gui.webdiff.WebDiff;

public class RunWithPerforceChangeList {
	private static String serverUri = "p4java://fisher.encs.concordia.ca:1666";
	private static String userName = "tsantalis";
	private static String pass = "123456";
	
	public static void main(String[] args) throws Exception {
		ProjectASTDiff projectASTDiff = new PerforceHistoryRefactoringMinerImpl().diffAtChangeList(
				serverUri, userName, pass, 2);
		new WebDiff(projectASTDiff).openInBrowser();
	}
}
