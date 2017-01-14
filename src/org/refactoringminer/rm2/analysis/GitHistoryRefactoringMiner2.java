package org.refactoringminer.rm2.analysis;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm2.model.SDModel;

public class GitHistoryRefactoringMiner2 implements GitHistoryRefactoringMiner {

    private RefactoringDetectorConfig config;
    
    public GitHistoryRefactoringMiner2() {
        this(new RefactoringDetectorConfigImpl());
    }
    
	public GitHistoryRefactoringMiner2(RefactoringDetectorConfig config) {
        this.config = config;
    }

    private final class HandlerAdpater extends StructuralDiffHandler {
		private final RefactoringHandler handler;

		private HandlerAdpater(RefactoringHandler handler) {
			this.handler = handler;
		}

		@Override
		public boolean skipCommit(String commitId) {
			return handler.skipCommit(commitId);
		}

		@Override
		public void handle(RevCommit commitData, SDModel sdModel) {
			handler.handle(commitData, sdModel.getRefactorings());
		}

		@Override
		public void handleException(String commitId, Exception e) {
		    handler.handleException(commitId, e);
		}
		
		@Override
		public void onFinish(int commitsCount, int errorCommitsCount) {
			handler.onFinish(0, commitsCount, errorCommitsCount);
		}
	}

	@Override
	public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
	    GitHistoryStructuralDiffAnalyzer sda = new GitHistoryStructuralDiffAnalyzer(config);
	    sda.detectAll(repository, branch, new HandlerAdpater(handler));
	}

	@Override
	public void fetchAndDetectNew(Repository repository, RefactoringHandler handler) throws Exception {
	    GitHistoryStructuralDiffAnalyzer sda = new GitHistoryStructuralDiffAnalyzer(config);
	    sda.fetchAndDetectNew(repository, new HandlerAdpater(handler));
	}

	@Override
	public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler) {
	    GitHistoryStructuralDiffAnalyzer sda = new GitHistoryStructuralDiffAnalyzer(config);
	    sda.detectAtCommit(repository, commitId, new HandlerAdpater(handler));
	}

}
