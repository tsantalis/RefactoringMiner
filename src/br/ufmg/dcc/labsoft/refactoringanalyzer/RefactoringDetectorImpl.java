package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class RefactoringDetectorImpl implements RefactoringDetector {

	@Override
	public void detectAll(Repository repository, RefactoringHandler handler) {
		long commitsCount = 0;
		long mergeCommitsCount = 0;
		long skippedCommitsCount = 0;
		long refactoringsCount = 0;
		RevCommit currentCommit = null;
		RevCommit parentCommit = null;
		UMLModel currentUMLModel = null;
		UMLModel parentUMLModel = null;
		Calendar startTime = Calendar.getInstance();

		File metadataFolder = repository.getDirectory();
		Git git = new Git(repository);
		File projectFolder = metadataFolder.getParentFile();
		try {
			
			RevWalk walk = new RevWalk(repository);
			walk.markStart(walk.parseCommit(repository.resolve("HEAD")));
			Iterator<RevCommit> i = walk.iterator();
			while (i.hasNext()) {				
				currentCommit = i.next();
				if (currentCommit.getParentCount() == 1) {
					try {
						// Ganho de performance - Aproveita a UML Model que ja se encontra em memorioa da comparacao anterior
						if (parentCommit != null && currentCommit.getId().equals(parentCommit.getId())) {
							currentUMLModel = parentUMLModel;
						} else {
							// Faz checkout e gera UML model da revisao current
							checkoutCommand(git, currentCommit);
							currentUMLModel = null;
							currentUMLModel = new ASTReader2(projectFolder).getUmlModel();
						}
						
						// Recupera o parent commit
						parentCommit = walk.parseCommit(currentCommit.getParent(0));
						
						Revision prevRevision = new Revision(parentCommit);
						Revision curRevision = new Revision(currentCommit);
						//System.out.println(String.format("Comparando %s e %s", prevRevision.getId(), curRevision.getId()));
						
						// Faz checkout e gera UML model da revisao parent
						checkoutCommand(git, parentCommit);
						parentUMLModel = null;
						parentUMLModel = new ASTReader2(projectFolder).getUmlModel();
						
						// Diff entre currentModel e parentModel
						UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
						List<Refactoring> refactoringsAtRevision = modelDiff.getRefactorings();
						refactoringsCount += refactoringsAtRevision.size();
						handler.handleDiff(prevRevision, parentUMLModel, curRevision, currentUMLModel, refactoringsAtRevision);
						
//						for (Refactoring ref : refactoringsAtRevision) {
//							handler.handleRefactoring(curRevision, currentUMLModel, ref);
//						}
					} catch (Exception e) {
						System.out.println("ERRO, revisão ignorada: " + currentCommit.getId().getName() + "\n");
						e.printStackTrace();
						skippedCommitsCount++;
					}

				} else {
					mergeCommitsCount++;
				}
				commitsCount++;
				//System.out.println(String.format("Revisões: %5d Ignoradas: %5d Refactorings: %4d ", numberOfOkRevisions, numberOfMergeRevisions, refactorings.size()));
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Calendar endTime = Calendar.getInstance();

		System.out.println("=====================================================");
		System.out.println(projectFolder.toString());
		System.out.println(String.format("Commits: %5d  Merge: %5d  Skipped: %d  Refactorings: %4d ", commitsCount, mergeCommitsCount, skippedCommitsCount, refactoringsCount));
		System.out.println("Início: " + startTime.get(Calendar.HOUR) + ":" + startTime.get(Calendar.MINUTE));
		System.out.println("Fim:    " + endTime.get(Calendar.HOUR) + ":" + endTime.get(Calendar.MINUTE));
	}

	private void checkoutCommand(Git git, RevCommit commit) throws Exception {
		CheckoutCommand checkout = git.checkout().setStartPoint(commit).setName(commit.getId().getName());
		checkout.call();		
	}

}
