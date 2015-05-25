package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLModelSet;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefactoringDetectorImpl implements RefactoringDetector {

	Logger logger = LoggerFactory.getLogger(RefactoringDetectorImpl.class);
	private boolean analyzeMethodInvocations;
	
	public RefactoringDetectorImpl() {
		this(false);
	}

	public RefactoringDetectorImpl(boolean analyzeMethodInvocations) {
		this.analyzeMethodInvocations = analyzeMethodInvocations;
	}

	private void detect(GitService gitService, Repository repository, final RefactoringHandler handler, Iterator<RevCommit> i) {
		int commitsCount = 0;
		int errorCommitsCount = 0;
		int refactoringsCount = 0;

		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();
		ASTParser parser = buildAstParser(projectFolder, analyzeMethodInvocations);
		
		long time = System.currentTimeMillis();
		while (i.hasNext()) {
			RevCommit currentCommit = i.next();
			try {
				List<Refactoring> refactoringsAtRevision = detectRefactorings(gitService, repository, handler, projectFolder, parser, currentCommit);
				refactoringsCount += refactoringsAtRevision.size();
				
			} catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
				errorCommitsCount++;
			}

			commitsCount++;
			long time2 = System.currentTimeMillis();
			if ((time2 - time) > 20000) {
				time = time2;
				logger.info(String.format("Processing %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
			}
		}

		handler.onFinish(refactoringsCount, commitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
	}

	private List<Refactoring> detectRefactorings(GitService gitService, Repository repository, final RefactoringHandler handler, File projectFolder, ASTParser parser, RevCommit currentCommit) throws Exception {
		List<Refactoring> refactoringsAtRevision;
		String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<String>();
		List<String> filesCurrent = new ArrayList<String>();
		fileTreeDiff(repository, currentCommit, filesBefore, filesCurrent);
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring
		if (!filesBefore.isEmpty() && !filesCurrent.isEmpty()) {
			// Checkout and build model for current commit
			gitService.checkout(repository, commitId);
			UMLModelSet currentUMLModel = createModel(projectFolder, parser, filesCurrent);
			
			// Checkout and build model for parent commit
			String parentCommit = currentCommit.getParent(0).getName();
			gitService.checkout(repository, parentCommit);
			UMLModelSet parentUMLModel = createModel(projectFolder, parser, filesBefore);
			
			// Diff between currentModel e parentModel
			refactoringsAtRevision = parentUMLModel.detectRefactorings(currentUMLModel);
			
		} else {
			//logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
			refactoringsAtRevision = Collections.emptyList();
		}
		handler.handleDiff(currentCommit, refactoringsAtRevision);
		return refactoringsAtRevision;
	}
	
	private void fileTreeDiff(Repository repository, RevCommit current, List<String> javaFilesBefore, List<String> javaFilesCurrent) throws Exception {
        ObjectId oldHead = current.getParent(0).getTree();
        ObjectId head = current.getTree();

        // prepare the two iterators to compute the diff between
		ObjectReader reader = repository.newObjectReader();
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset(reader, oldHead);
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, head);

		// finally get the list of changed files
		List<DiffEntry> diffs = new Git(repository).diff()
		                    .setNewTree(newTreeIter)
		                    .setOldTree(oldTreeIter)
		                    .setShowNameAndStatusOnly(true)
		                    .call();
        for (DiffEntry entry : diffs) {
        	ChangeType changeType = entry.getChangeType();
        	if (changeType != ChangeType.ADD) {
        		String oldPath = entry.getOldPath();
        		if (isJavafile(oldPath)) {
        			javaFilesBefore.add(oldPath);
        		}
        	}
    		if (changeType != ChangeType.DELETE) {
        		String newPath = entry.getNewPath();
        		if (isJavafile(newPath)) {
        			javaFilesCurrent.add(newPath);
        		}
    		}
        }
	}
	
	private boolean isJavafile(String path) {
		return path.endsWith(".java");
	}

	@Override
	public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipRevision(sha1);
			}
		};
		RevWalk walk = gitService.createAllRevsWalk(repository, branch);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	@Override
	public void fetchAndDetectNew(Repository repository, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipRevision(sha1);
			}
		};
		RevWalk walk = gitService.fetchAndCreateNewRevsWalk(repository);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	private UMLModelSet createModel(File projectFolder, ASTParser parser, List<String> files) throws Exception {
		return new UMLModelASTReader(projectFolder, parser, files).getUmlModelSet();
	}

	public void detectOne(ASTParser parser, Repository repository, String commitId, String parentCommitId, RefactoringHandler handler) {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			walk.parseCommit(commit.getParent(0));
			this.detectRefactorings(gitService, repository, handler, projectFolder, parser, commit);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
		} finally {
			walk.dispose();
		}
	}

	public ASTParser buildAstParser(Repository repository) {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		return this.buildAstParser(projectFolder, analyzeMethodInvocations);
	}

	public static ASTParser buildAstParser(File srcFolder, boolean resolveBindings) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		parser.setCompilerOptions(options);
		if (resolveBindings) {
			parser.setResolveBindings(true);
			parser.setBindingsRecovery(true);
			parser.setEnvironment(new String[0], new String[]{srcFolder.getPath()}, null, true);
		} else {
			parser.setResolveBindings(false);
			parser.setEnvironment(new String[0], new String[]{srcFolder.getPath()}, null, false);
		}
		return parser;
	}
}
