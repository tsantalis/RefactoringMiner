package gui;

import org.refactoringminer.astDiff.models.ProjectASTDiff;

import gui.webdiff.DiffDriver;
import gui.webdiff.WebDiff;

public class RunWithPerforceChangeList {
	private static String serverUri = "p4java://fisher.encs.concordia.ca:1666";
	private static String userName = "tsantalis";
	private static String pass = "123456";
	
	public static void main(String[] args) throws Exception {
		DiffDriver diffDriver = new DiffDriver();
		diffDriver.setUrl(serverUri);
		diffDriver.setPerforceUserName(userName);
		diffDriver.setPerforcePassword(pass);
		diffDriver.setCommit("2");
		ProjectASTDiff projectASTDiff = diffDriver.getProjectASTDiff();
		new WebDiff(projectASTDiff).run();
	}
}
