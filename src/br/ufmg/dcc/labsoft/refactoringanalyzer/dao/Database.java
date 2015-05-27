package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class Database {

	EntityManager em;

	public Database() {
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("refactoringDB");
		em = factory.createEntityManager();
	}

	interface Transaction {
		void run(EntityManager em);
	}
	
	private void perform(Transaction transaction) {
		//EntityManager em = createEm();
		em.getTransaction().begin();
		try {
			transaction.run(em);
			em.getTransaction().commit();
		} catch (Exception e) {
			em.getTransaction().rollback();
			throw e;
		} finally {
			em.clear();
		}
	}
	
	public ProjectGit getProjectById(Long id) {
		return em.find(ProjectGit.class, id);
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

	public void insertIfNotExists(final ProjectGit project) {
		perform(new Transaction(){
			public void run(EntityManager em) {
				em.persist(project);
			}
		});
	}

	public void insert(final RevisionGit revision) {
		perform(new Transaction(){
			public void run(EntityManager em) {
				em.persist(revision);
			}
		});
	}

	public void insert(final ExtractMethodInfo emi) {
		perform(new Transaction(){
			public void run(EntityManager em) {
				em.persist(emi);
			}
		});
	}

	public void update(final ProjectGit project) {
		perform(new Transaction(){
			public void run(EntityManager em) {
				em.merge(project);
			}
		});
	}

	public void releaseLocks(final String pid) {
		perform(new Transaction(){
			public void run(EntityManager em) {
				em.createNamedQuery("projectGit.releaseLocks").setParameter("pid", pid).executeUpdate();
			}
		});
	}

	public ProjectGit findNonAnalyzedProjectAndLock(String pid) {
		ProjectGit project = null;
		em.getTransaction().begin();
		try {
			@SuppressWarnings("unchecked")
			List<ProjectGit> projects = em.createNamedQuery("projectGit.findNonAnalyzed").setMaxResults(1).getResultList();
			if (projects.size() > 0) {
				project = projects.get(0);
				project.setRunning_pid(pid);
				em.merge(project);
			}
			em.getTransaction().commit();
			return project;
		} catch (Exception e) {
			em.getTransaction().rollback();
			throw e;
		} finally {
			em.clear();
		}
	}
	
	public ProjectGit findProjectToMonitorAndLock(String pid, Date beforeDate) {
		ProjectGit project = null;
		em.getTransaction().begin();
		try {
			@SuppressWarnings("unchecked")
			List<ProjectGit> projects = em.createNamedQuery("projectGit.findToMonitor")
					.setParameter("date", beforeDate)
					.setMaxResults(1)
					.getResultList();
			if (projects.size() > 0) {
				project = projects.get(0);
				project.setRunning_pid(pid);
				em.merge(project);
			}
			em.getTransaction().commit();
			return project;
		} catch (Exception e) {
			em.getTransaction().rollback();
			throw e;
		} finally {
			em.clear();
		}
	}
	
	public ProjectGit findNonCountedProjectAndLock(String pid) {
		ProjectGit project = null;
		em.getTransaction().begin();
		try {
			@SuppressWarnings("unchecked")
			List<ProjectGit> projects = em.createNamedQuery("projectGit.findNonCounted").setMaxResults(1).getResultList();
			if (projects.size() > 0) {
				project = projects.get(0);
				project.setRunning_pid(pid);
				em.merge(project);
			}
			em.getTransaction().commit();
			return project;
		} catch (Exception e) {
			em.getTransaction().rollback();
			throw e;
		} finally {
			em.clear();
		}
	}

}
