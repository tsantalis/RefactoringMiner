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
	public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
		int commitsCount = 0;
		//int mergeCommitsCount = 0;
		int errorCommitsCount = 0;
		int refactoringsCount = 0;
		String parentCommit = null;
		UMLModelSet currentUMLModel = null;
		UMLModelSet parentUMLModel = null;

		File metadataFolder = repository.getDirectory();
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipRevision(sha1);
			}
		};
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();
		ASTParser parser = this.buildAstParser(projectFolder, analyzeMethodInvocations);
		
		RevWalk walk = gitService.createAllRevsWalk(repository, branch);
		try {
			Iterator<RevCommit> i = walk.iterator();
			long time = System.currentTimeMillis();
			while (i.hasNext()) {
				RevCommit currentCommit = i.next();
				String commitId = currentCommit.getId().getName();
				try {
					// Ganho de performance - Aproveita a UML Model que ja se encontra em memorioa da comparacao anterior
					if (parentCommit != null && commitId.equals(parentCommit)) {
						currentUMLModel = parentUMLModel;
					} else {
						// Faz checkout e gera UML model da revisao current
						gitService.checkout(repository, commitId);
						currentUMLModel = null;
						currentUMLModel = createModel(projectFolder, parser);
					}
					
					// Recupera o parent commit
					parentCommit = currentCommit.getParent(0).getName();
					
					// Faz checkout e gera UML model da revisao parent
					gitService.checkout(repository, parentCommit);
					parentUMLModel = null;
					parentUMLModel = createModel(projectFolder, parser);
					
					// Diff entre currentModel e parentModel
					List<Refactoring> refactoringsAtRevision = parentUMLModel.detectRefactorings(currentUMLModel);
					refactoringsCount += refactoringsAtRevision.size();
					handler.handleDiff(parentUMLModel, commitId, currentCommit, currentUMLModel, refactoringsAtRevision);
					
				} catch (Exception e) {
					logger.warn(String.format("Ignored revision %s due to error", commitId), e);
					errorCommitsCount++;
				}

				commitsCount++;
				long time2 = System.currentTimeMillis();
				if ((time2 - time) > 20000) {
					time = time2;
					logger.info(String.format("Processing %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
				}
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			walk.dispose();
		}

		handler.onFinish(refactoringsCount, commitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
	}

	private UMLModelSet createModel(File projectFolder, ASTParser parser) throws Exception {
		return new ASTReader2(projectFolder, parser, analyzeMethodInvocations).getUmlModelSet();
//		return new ASTReader3(projectFolder).getUmlModelSet();
	}

	public void detectOne(ASTParser parser, Repository repository, String commitId, String parentCommitId, RefactoringHandler handler) {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		try {
			GitService gitService = new GitServiceImpl();
			gitService.checkout(repository, commitId);
			ASTParser parser1 = RefactoringDetectorImpl.buildAstParser(projectFolder, true);
			UMLModelSet currentUMLModel = createModel(projectFolder, parser1);
			
			if (parentCommitId == null) {
				parentCommitId = repository.resolve(commitId + "^1").getName();
			}
			gitService.checkout(repository, parentCommitId);
			ASTParser parser2 = RefactoringDetectorImpl.buildAstParser(projectFolder, true);
			UMLModelSet parentUMLModel = createModel(projectFolder, parser2);
			
			List<Refactoring> refactoringsAtRevision = parentUMLModel.detectRefactorings(currentUMLModel);
			handler.handleDiff(parentUMLModel, commitId, null, currentUMLModel, refactoringsAtRevision);

		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
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
