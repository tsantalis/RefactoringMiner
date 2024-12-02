package org.refactoringminer.api;

import org.refactoringminer.astDiff.models.ProjectASTDiff;

import com.perforce.p4java.server.IOptionsServer;

public interface PerforceHistoryRefactoringMiner {

	/**
	 * Connects to the Perforce Server using the specified credentials.
	 * 
	 * @param serverUrl The Perforce server url, e.g., p4java://fisher.encs.concordia.ca:1666
	 * @param userName The user name used to connect to the Perforce server
	 * @param password The user password used to connect to the Perforce server
	 * @return Perforce server object that can be used to fetch information.
	 * @throws Exception
	 */
	IOptionsServer connectToPerforceServer(String serverUrl,
            String userName,
            String password) throws Exception;

	/**
	 * Generate the AST diff for the specified Perforce Change List.
	 * 
	 * @param serverUrl The Perforce server url, e.g., p4java://fisher.encs.concordia.ca:1666
	 * @param userName The user name used to connect to the Perforce server
	 * @param password The user password used to connect to the Perforce server
	 * @param changeListNumber The Change List number to generate AST diff for
	 * @return A set of ASTDiff objects. Each ASTDiff corresponds to a pair of Java compilation units.
	 * @throws Exception
	 */
	ProjectASTDiff diffAtChangeList(String serverUrl,
            String userName,
            String password,
            int changeListNumber) throws Exception;

	/**
	 * Detect refactorings performed in the specified Perforce Change List.
	 * 
	 * @param serverUrl The Perforce server url, e.g., p4java://fisher.encs.concordia.ca:1666
	 * @param userName The user name used to connect to the Perforce server
	 * @param password The user password used to connect to the Perforce server
	 * @param changeListNumber The Change List number to generate AST diff for
	 * @param handler A handler object that is responsible to process the detected refactorings.
	 * @throws Exception
	 */
	void detectAtChangeList(String serverUrl,
            String userName,
            String password,
            int changeListNumber,
            RefactoringHandler handler) throws Exception;

	/**
	 * Detect refactorings performed in the specified Perforce Change List.
	 * 
	 * @param serverUrl The Perforce server url, e.g., p4java://fisher.encs.concordia.ca:1666
	 * @param userName The user name used to connect to the Perforce server
	 * @param password The user password used to connect to the Perforce server
	 * @param changeListNumber The Change List number to generate AST diff for
	 * @param handler A handler object that is responsible to process the detected refactorings.
	 * @param timeout A timeout, in seconds. When timeout is reached, the operation stops and returns no refactorings.
	 * @throws Exception
	 */
	void detectAtChangeList(String serverUrl,
            String userName,
            String password,
            int changeListNumber,
            RefactoringHandler handler, int timeout) throws Exception;
}
