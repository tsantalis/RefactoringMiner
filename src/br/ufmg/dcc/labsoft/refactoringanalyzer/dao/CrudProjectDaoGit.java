package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.validation.ConstraintViolationException;

public class CrudProjectDaoGit<T extends AbstractEntity> {

	private static EntityManager em;

	public CrudProjectDaoGit() {
		em = getEntityManager();

	}

	private EntityManager getEntityManager() {

		EntityManagerFactory factory = Persistence.createEntityManagerFactory("refactoringDB");
		if (em == null) {
		em = factory.createEntityManager();
		}
		return em;
	}

	public T persistObject(T obj) {
		try {

			em.getTransaction().begin();
			em.persist(obj);
			em.getTransaction().commit();
		} catch (Exception ex) {
			ex.printStackTrace();
			em.getTransaction().rollback();
		}finally{
			//em.close();
		}
		return obj;
	}

	public void persistRevision(RevisionGit r) {
		try {
			em.getTransaction().begin();
			em.persist(r);
			em.getTransaction().commit();
		} catch (Exception ex) {
			ex.printStackTrace();
			em.getTransaction().rollback();
		}finally{
			//em.close();
		}
	}

	public T mergeObject(T obj) {
		try {

			em.getTransaction().begin();
			em.merge(obj);
			em.flush();
			em.getTransaction().commit();
		}catch (Exception e) {
			    Throwable t = e.getCause();
			    while ((t != null) && !(t instanceof ConstraintViolationException)) {
			        t = t.getCause();
			    }
			    if (t instanceof ConstraintViolationException) {
			    	System.out.println("Operação já foi incluida!!!");
					e.printStackTrace();
			    }else{
			    	System.out.println("Exception: ");
					e.printStackTrace();
			    }
			    	
			}finally{
			//em.close();
		}
		return obj;
	}

	public List<T> listarProject(String classe) {
		return em.createQuery("from " + classe + " c ").getResultList();
	}

	public void salvarList(List<T> objList) {
		em.getTransaction().begin();
		em.merge(objList);
		em.flush();
		em.getTransaction().commit();
	}

	public List<T> findByAll(String query) {
		try {
			return em.createNamedQuery(query).getResultList();
		} catch (Exception ex) {
			Logger.getLogger(CrudProjectDaoGit.class.getName()).log(
					Level.SEVERE, null, ex);
			return null;
		}
	}

	public List<T> getProjects() {
		// String sqlQuery = "SELECT * FROM projectGit;";
		String sqlQuery = "SELECT * FROM projectgit where finalizado = 0 order by size;";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<T> getPreProjects() {
		String sqlQuery = "SELECT * FROM ProjectGit where id in (27, 11, 50);";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);
		return q.getResultList();
	}

	public List<T> getMaxForksProjects() {
		// String sqlQuery =
		// "SELECT * FROM project order by count desc limit 3";
		String sqlQuery = "SELECT * FROM ProjectGit where finalizado = 1 order by forks_count desc;";
		// String sqlQuery = "SELECT * FROM urlPreProjectGit where id = 46;";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);

		return q.getResultList();
	}

	public T getProjectSelected(long id) {

		String sqlQuery = "SELECT * FROM ProjectGit where id=" + id + ";";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);

		return (T) q.getSingleResult();
	}

	public List<T> getRevisionsProjects() {
		// String sqlQuery =
		// "SELECT * FROM project order by count desc limit 3";
		// String sqlQuery =
		// "SELECT * FROM projectGit where finalizado = 1 order by countCommits desc limit 20;";
		String sqlQuery = "SELECT * FROM projectGit where finalizado = 1 order by forks;";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);

		return q.getResultList();
	}

	public List<RefactoringGit> findRefactoringDuplicado(String hash) {
		
		return (List<RefactoringGit>) em
				.createNamedQuery("refactoringGit.findRefactoringDuplicado")
				.setParameter("hashOperacao", hash).getResultList();
	}

	/*
	 * SELECT * FROM project p where DATE_FORMAT(lastDateCommit,'%d/%m/%Y') =
	 * '12/09/2014';
	 */
}
