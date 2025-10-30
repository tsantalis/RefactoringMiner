package org.refactoringminer.api;

import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.List;

public interface ModelDiffRefactoringHandler extends RefactoringHandler {

    /**
     * Indicate commits that should be ignored.
     * You may override this method to implement custom logic.
     *
     * @param commitId The SHA key that identifies the commit.
     * @return True to skip the commit, false otherwise.
     */
    default boolean skipCommit(String commitId) {
        return false;
    }

    /**
     * This method is called after each commit is analyzed.
     * You should override this method to do your custom logic with the list of detected refactorings.
     *
     * @param commitId The sha of the analyzed commit.
     * @param refactorings List of refactorings detected in the commit.
     */
    default void handle(String commitId, List<Refactoring> refactorings) {}

    /**
     * This method is called whenever an exception is thrown during the analysis of the given commit.
     * You should override this method to do your custom logic in the case of exceptions (e.g. skip or rethrow).
     *
     * @param commitId The SHA key that identifies the commit.
     * @param e The exception thrown.
     */
    default void handleException(String commitId, Exception e) {
        throw new RuntimeException(e);
    }

    /**
     * This method is called after all commits are analyzed.
     * You may override this method to implement custom logic.
     *
     * @param refactoringsCount Total number of refactorings detected.
     * @param commitsCount Total number of commits analyzed.
     * @param errorCommitsCount Total number of commits not analyzed due to errors.
     */
    default void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {}

    void handleModelDiff(String commitId, List<Refactoring> refactoringsAtRevision, UMLModelDiff modelDiff);
}
