package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class Database {

	private static EntityManager em;

	public Database() {
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("refactoringDB");
		em = factory.createEntityManager();
	}

	public ProjectGit getProjectByCloneUrl(String cloneUrl) {
		@SuppressWarnings("unchecked")
		List<ProjectGit> projects = em.createNamedQuery("projectGit.findByCloneUrl")
			.setParameter("cloneUrl", cloneUrl).getResultList();
		if (projects.size() > 0) {
			return projects.get(0);
		}
		return null;
	}

	public void insertIfNotExists(ProjectGit project) {
		try {
			em.getTransaction().begin();
			if (this.getProjectByCloneUrl(project.getCloneUrl()) == null) {
				em.persist(project);
			}
			em.getTransaction().commit();
		} catch (Exception e) {
			em.getTransaction().rollback();
			throw e;
		}
	}

	public RevisionGit getRevisionById(ProjectGit project, String id) {
		@SuppressWarnings("unchecked")
		List<RevisionGit> revisions = em.createNamedQuery("revisionGit.findByProjectAndCommit")
			.setParameter("project", project)
			.setParameter("commitId", id).getResultList();
		if (revisions.size() > 0) {
			return revisions.get(0);
		}
		return null;
	}

	public List<RefactoringGit> findExtractMethodRefactoringsByProjectAndCommit(ProjectGit project, String commit) {
		@SuppressWarnings("unchecked")
		List<RefactoringGit> refactorings = em.createNamedQuery("refactoringGit.extractMethods")
			.setParameter("cloneUrl", project.getCloneUrl())
			.setParameter("commitId", commit).getResultList();
		return refactorings;
	}

	public List<RevisionGit> findRevisionsByProjectAndExtractMethod(ProjectGit project) {
		@SuppressWarnings("unchecked")
		List<RevisionGit> revisions = em.createNamedQuery("revisionGit.findByProjectAndExtractMethod")
		.setParameter("cloneUrl", project.getCloneUrl()).getResultList();
		return revisions;
	}

	public void insert(RevisionGit revision) {
		em.getTransaction().begin();
		em.persist(revision);
		em.getTransaction().commit();
	}

	public void insert(ExtractMethodInfo emi) {
		em.getTransaction().begin();
		em.persist(emi);
		em.getTransaction().commit();
	}

	public void update(ProjectGit project) {
		em.getTransaction().begin();
		em.merge(project);
		em.getTransaction().commit();
	}

	public void releaseLocks(String pid) {
		em.getTransaction().begin();
		em.createNamedQuery("projectGit.releaseLocks").setParameter("pid", pid).executeUpdate();
		em.getTransaction().commit();
	}

	public ProjectGit findAndLockProject(String pid) {
		ProjectGit project = null;
		em.getTransaction().begin();
		@SuppressWarnings("unchecked")
		List<ProjectGit> projects = em.createNamedQuery("projectGit.findNonAnalyzed").getResultList();
		if (projects.size() > 0) {
			project = projects.get(0);
			project.setRunning_pid(pid);
			em.merge(project);
		}
		em.getTransaction().commit();
		return project;
	}

}
