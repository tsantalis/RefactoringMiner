package br.ufmg.dcc.labsoft.refactoringanalyzer;

import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public interface GitService {

	Repository cloneIfNotExists(String folder, String cloneUrl/*, String branch*/) throws Exception;

	int countCommits(Repository repository, String branch) throws Exception;

	void checkout(Repository repository, String commitId) throws Exception;

	RevWalk fetchAndCreateNewRevsWalk(Repository repository) throws Exception;

	RevWalk fetchAndCreateNewRevsWalk(Repository repository, String branch) throws Exception;

	RevWalk createAllRevsWalk(Repository repository) throws Exception;

	RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception;

	void fileTreeDiff(Repository repository, RevCommit currentCommit, List<String> filesBefore, List<String> filesCurrent, Map<String, String> renamedFilesHint) throws Exception;
}
