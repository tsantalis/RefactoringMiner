package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class RefactoringDetectorImpl implements RefactoringDetector {

	private final String projectFolder;
	private final RefactoringHandler handler;

	private RevCommit currentCommit;
	private RevCommit parentCommit;
	private UMLModel currentUMLModel;
	private UMLModel parentUMLModel;

	private Calendar startTime;
	private Calendar endTime;

	private long numberOfOkRevisions = 0;
	private long numberOfMergeRevisions = 0;

	List<Refactoring> refactorings = new ArrayList<Refactoring> ();


	public RefactoringDetectorImpl(String projectFolder, RefactoringHandler handler) {
		this.projectFolder = projectFolder;
		this.handler = handler;
	}

	@Override
	public void detectAll() {
		startTime = Calendar.getInstance();

		try {
			RepositoryBuilder builder = new RepositoryBuilder();
			org.eclipse.jgit.lib.Repository repository = builder
					.setGitDir(new File(this.projectFolder + File.separator + ".git"))
					.readEnvironment() // scan environment GIT_* variables
					.findGitDir() // scan up the file system tree
					.build();

			// Inicializa repositorio
			Git git = new Git(repository);
			checkoutHead(git);
			currentUMLModel = new ASTReader2(new File(this.projectFolder)).getUmlModel();
			this.handler.handleCurrent(currentUMLModel);
			
			Ref head = repository.getRef(Constants.MASTER);
			String headId = head.getObjectId().getName();
			
			RevWalk walk = new RevWalk(repository);
			Iterable<RevCommit> logs = git.log().call();
			Iterator<RevCommit> i = logs.iterator();		

			//Itera em todas as revisoes do projeto
			while (i.hasNext()) {				
				currentCommit = walk.parseCommit(i.next());		

				if (currentCommit.getParentCount() == 1) {

					try {
						// Ganho de performance - Aproveita a UML Model que ja se encontra em memorioa da comparacao anterior
						if (parentCommit != null && currentCommit.getId().equals(parentCommit.getId())) {
							currentUMLModel = parentUMLModel;
						} else {
							// Faz checkout e gera UML model da revisao current
							checkoutCommand(git, currentCommit);
							currentUMLModel = new ASTReader2(new File(this.projectFolder)).getUmlModel();
						}
						
						// Recupera o parent commit
						parentCommit = walk.parseCommit(currentCommit.getParent(0));
						
						Revision prevRevision = new Revision(parentCommit, headId == parentCommit.getId().getName());
						Revision curRevision = new Revision(currentCommit, headId == currentCommit.getId().getName());
						//System.out.println(String.format("Comparando %s e %s", prevRevision.getId(), curRevision.getId()));
						
						// Faz checkout e gera UML model da revisao parent
						checkoutCommand(git, parentCommit);
						parentUMLModel = new ASTReader2(new File(this.projectFolder)).getUmlModel();
						
						// Diff entre currentModel e parentModel
						UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
						List<Refactoring> refactoringsAtRevision = modelDiff.getRefactorings();
						refactorings.addAll(refactoringsAtRevision);
						this.handler.handleDiff(prevRevision, parentUMLModel, curRevision, currentUMLModel);
						
						for (Refactoring ref : refactoringsAtRevision) {
							this.handler.handleRefactoring(curRevision, ref);
						}
					} catch (Exception e) {
						System.out.println("ERRO, revisão ignorada: " + currentCommit.getId().getName() + "\n");
					}

					numberOfOkRevisions++;
				} else {
					numberOfMergeRevisions++;
				}

				//System.out.println(String.format("Revisões: %5d Ignoradas: %5d Refactorings: %4d ", numberOfOkRevisions, numberOfMergeRevisions, refactorings.size()));
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		endTime = Calendar.getInstance();

		System.out.println("=====================================================");
		System.out.println(this.projectFolder);
		System.out.println(String.format("Revisões: %5d  Merge: %5d  Refactorings: %4d ", numberOfOkRevisions, numberOfMergeRevisions, refactorings.size()));
		System.out.println("Início: " + startTime.get(Calendar.HOUR) + ":" + startTime.get(Calendar.MINUTE));
		System.out.println("Fim:    " + endTime.get(Calendar.HOUR) + ":" + endTime.get(Calendar.MINUTE));	
	}

	private void checkoutCommand(Git git, RevCommit commit) throws Exception {
		CheckoutCommand checkout = git.checkout().setStartPoint(commit).setName(commit.getId().getName());
		checkout.call();		
	}

	private void checkoutHead(Git git) throws Exception {
		CheckoutCommand checkout = git.checkout().setStartPoint(Constants.HEAD).setName(Constants.MASTER);
		checkout.call();
	}

}
