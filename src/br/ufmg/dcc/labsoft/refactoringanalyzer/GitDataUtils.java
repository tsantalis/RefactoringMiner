package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.ASTReader;
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
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class GitDataUtils {

	private final String GIT_PATH = "//home//ufmg//renato//git//junit";

	private RevCommit currentCommit;
	private RevCommit parentCommit;
	private UMLModel currentUMLModel;
	private UMLModel parentUMLModel;
	
	private Calendar startTime;
	private Calendar endTime;
	
	private long numberOfOkRevisions = 0;
	private long numberOfMergeRevisions = 0;
	
	List<Refactoring> refactorings = new ArrayList<Refactoring> ();

	public GitDataUtils() {
		startTime = Calendar.getInstance();
		
		try {
			RepositoryBuilder builder = new RepositoryBuilder();
			org.eclipse.jgit.lib.Repository repository = builder
					.setGitDir(new File(GIT_PATH + File.separator + ".git"))
					.readEnvironment() // scan environment GIT_* variables
					.findGitDir() // scan up the file system tree
					.build();
			
			// Inicializa repositorio
			Git git = new Git(repository);
			RevWalk walk = new RevWalk(repository);
			Iterable<RevCommit> logs = git.log().call();
			Iterator<RevCommit> i = logs.iterator();		
						
			//Itera em todas as revisões do projeto
			while (i.hasNext()) {				
				currentCommit = walk.parseCommit(i.next());		
				
				if (currentCommit.getParentCount() == 1) {
					
					// Ganho de performance - Aproveita a UML Model que já se encontra em memória da comparação anterior
					if (parentCommit != null && currentCommit.getId().equals(parentCommit.getId())) {
						currentUMLModel = parentUMLModel;
					} else {
						// Faz checkout e gera UML model da revisao current
						checkoutCommand(git, currentCommit);
						currentUMLModel = new ASTReader(new File(GIT_PATH)).getUmlModel();
					}				
					
					// Recupera o parent commit
					parentCommit = walk.parseCommit(currentCommit.getParent(0));
					
					// Faz checkout e gera UML model da revisao parent
					checkoutCommand(git, parentCommit);
					parentUMLModel = new ASTReader(new File(GIT_PATH)).getUmlModel();
					
					// Diff entre currentModel e parentModel
					UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
					refactorings.addAll(modelDiff.getRefactorings());
					
					numberOfOkRevisions++;
				} else {
					numberOfMergeRevisions++;
				}
								
				System.out.println("|-------------------------------------------------|");
				System.out.println("    Revisoes Verificadas: " + numberOfOkRevisions);
				System.out.println("    Revisoes Ignoradas (Merge): " + numberOfMergeRevisions);
				System.out.println("    Refactorings: " + refactorings.size());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		endTime = Calendar.getInstance();
		
		System.out.println("|-------------------------------------------------|");
		System.out.println("Início do Processo:  " + startTime.get(Calendar.HOUR) + ":" + startTime.get(Calendar.MINUTE));
		System.out.println("Fim do Processo:  " + endTime.get(Calendar.HOUR) + ":" + endTime.get(Calendar.MINUTE));		
	}

	private void checkoutCommand(Git git, RevCommit commit) throws Exception {
		CheckoutCommand checkout = git.checkout().setStartPoint(commit).setName(commit.getId().getName());
		checkout.call();		
	}
}
