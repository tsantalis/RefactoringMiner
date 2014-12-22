package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.UMLModelSet;
import gr.uom.java.xmi.diff.Refactoring;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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

	@Override
	public void detectAll(Repository repository, RefactoringHandler handler) {
		int commitsCount = 0;
		int mergeCommitsCount = 0;
		int errorCommitsCount = 0;
		int refactoringsCount = 0;
		RevCommit currentCommit = null;
		RevCommit parentCommit = null;
		UMLModelSet currentUMLModel = null;
		UMLModelSet parentUMLModel = null;

		File metadataFolder = repository.getDirectory();
		Git git = new Git(repository);
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();
		ASTParser parser = this.buildAstParser(projectFolder, analyzeMethodInvocations);
		
		RevWalk walk = new RevWalk(repository);
		try {
			
			//ObjectId startPoint = repository.resolve("FETCH_HEAD");
			//logger.info("Project: {}, start: {}", projectName, startPoint.getName());
			walk.markStart(walk.parseCommit(repository.resolve(Constants.HEAD)));
			Iterator<RevCommit> i = walk.iterator();
			long time = System.currentTimeMillis();
			while (i.hasNext()) {
				currentCommit = i.next();
				if (currentCommit.getParentCount() == 1 && !handler.skipRevision(currentCommit)) {
					try {
						// Ganho de performance - Aproveita a UML Model que ja se encontra em memorioa da comparacao anterior
						if (parentCommit != null && currentCommit.getId().equals(parentCommit.getId())) {
							currentUMLModel = parentUMLModel;
						} else {
							// Faz checkout e gera UML model da revisao current
							checkoutCommand(git, currentCommit);
							currentUMLModel = null;
							currentUMLModel = new ASTReader2(projectFolder, parser, analyzeMethodInvocations).getUmlModelSet();
						}
						
						// Recupera o parent commit
						parentCommit = walk.parseCommit(currentCommit.getParent(0));
						
						// Faz checkout e gera UML model da revisao parent
						checkoutCommand(git, parentCommit);
						parentUMLModel = null;
						parentUMLModel = new ASTReader2(projectFolder, parser, analyzeMethodInvocations).getUmlModelSet();
						
						// Diff entre currentModel e parentModel
						List<Refactoring> refactoringsAtRevision = parentUMLModel.detectRefactorings(currentUMLModel);
						refactoringsCount += refactoringsAtRevision.size();
						handler.handleDiff(parentCommit, parentUMLModel, currentCommit, currentUMLModel, refactoringsAtRevision);
						
					} catch (Exception e) {
						logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
						errorCommitsCount++;
					}

				}
				if (currentCommit.getParentCount() != 1) {
					mergeCommitsCount++;
				}
				commitsCount++;
				long time2 = System.currentTimeMillis();
				if ((time2 - time) > 20000) {
					time = time2;
					logger.info(String.format("Processing %s [Commits: %d, Merge: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, mergeCommitsCount, errorCommitsCount, refactoringsCount));
				}
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			walk.dispose();
		}

		handler.onFinish(refactoringsCount, commitsCount, mergeCommitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Merge: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, mergeCommitsCount, errorCommitsCount, refactoringsCount));
	}

	public void detectOne(ASTParser parser, Repository repository, String commitId, String parentCommitId, RefactoringHandler handler) {
		File metadataFolder = repository.getDirectory();
		Git git = new Git(repository);
		File projectFolder = metadataFolder.getParentFile();
		try {
			checkoutCommand(git, commitId);
			ASTParser parser1 = RefactoringDetectorImpl.buildAstParser(projectFolder, true);
			UMLModelSet currentUMLModel = new ASTReader2(projectFolder, parser1, analyzeMethodInvocations).getUmlModelSet();
			
			checkoutCommand(git, parentCommitId);
			ASTParser parser2 = RefactoringDetectorImpl.buildAstParser(projectFolder, true);
			UMLModelSet parentUMLModel = new ASTReader2(projectFolder, parser2, analyzeMethodInvocations).getUmlModelSet();
			
			List<Refactoring> refactoringsAtRevision = parentUMLModel.detectRefactorings(currentUMLModel);
			handler.handleDiff(null, parentUMLModel, null, currentUMLModel, refactoringsAtRevision);

		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
		}
	}

	private void checkoutCommand(Git git, RevCommit commit) throws Exception {
		CheckoutCommand checkout = git.checkout().setStartPoint(commit).setName(commit.getId().getName());
		checkout.call();		
	}

	private void checkoutCommand(Git git, String commitId) throws Exception {
		CheckoutCommand checkout = git.checkout().setName(commitId);
		checkout.call();		
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
