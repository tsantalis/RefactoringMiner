package br.ufmg.dcc.labsoft.refdetector;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitHistoryRefactoringDetectorWithMotivationImpl extends  GitHistoryRefactoringDetectorImpl {

	protected List<Refactoring> detectRefactorings(GitService gitService, Repository repository, final RefactoringHandler handler, File projectFolder, RevCommit currentCommit) throws Exception {
		List<Refactoring> refactoringsAtRevision;
		String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<String>();
		List<String> filesCurrent = new ArrayList<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit, filesBefore, filesCurrent, renamedFilesHint);
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring
		if (!filesBefore.isEmpty() && !filesCurrent.isEmpty()) {
			// Checkout and build model for current commit
			gitService.checkout(repository, commitId);
			UMLModel currentUMLModel = createModel(projectFolder, filesCurrent);
			
			// Checkout and build model for parent commit
			String parentCommit = currentCommit.getParent(0).getName();
			gitService.checkout(repository, parentCommit);
			UMLModel parentUMLModel = createModel(projectFolder, filesBefore);
			
			// Diff between currentModel e parentModel
			refactoringsAtRevision = parentUMLModel.diff(currentUMLModel, renamedFilesHint).getRefactorings();
			refactoringsAtRevision = filter(refactoringsAtRevision);
			
		} else {
			//logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
			refactoringsAtRevision = Collections.emptyList();
		}
		handler.handle(currentCommit, refactoringsAtRevision);
		return refactoringsAtRevision;
	}

}
