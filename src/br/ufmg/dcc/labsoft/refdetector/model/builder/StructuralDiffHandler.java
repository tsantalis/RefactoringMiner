package br.ufmg.dcc.labsoft.refdetector.model.builder;

import org.eclipse.jgit.revwalk.RevCommit;

import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public abstract class StructuralDiffHandler {

	/**
	 * Indicate commits that should be ignored.
	 * You may override this method to implement custom logic.
	 *  
	 * @param commitId The SHA key that identifies the commit.
	 * @return True to skip the commit, false otherwise.
	 */
	public boolean skipCommit(String commitId) {
		return false;
	}

	/**
	 * This method is called after each commit is analyzed.
	 * You should override this method to do your custom logic with the computed structural diff model.
	 * 
	 * @param commitData An object (from JGit library) that contains metadata information about the commit such as date, author, etc.
	 * @param sdModel A model that represents the structural diff.
	 */
	public void handle(RevCommit commitData, SDModel sdModel) {}

	/**
     * This method is called whenever an exception is thrown during the analysis of the given commit.
     * You should override this method to do your custom logic in the case of exceptions (e.g. skip or rethrow).
     * 
     * @param commitId The SHA key that identifies the commit.
     * @param e The exception thrown.
     */
    public void handleException(String commitId, Exception e) {
        throw new RuntimeException(e);
    }
	
	/**
	 * This method is called after all commits are analyzed.
	 * You may override this method to implement custom logic.
	 * 
	 * @param commitsCount Total number of commits analyzed.
	 * @param errorCommitsCount Total number of commits not analyzed due to errors.
	 */
	public void onFinish(int commitsCount, int errorCommitsCount) {}
}
