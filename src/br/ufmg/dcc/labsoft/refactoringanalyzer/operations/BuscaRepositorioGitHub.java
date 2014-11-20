/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author THIAGO MAGELA
 */
package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonObject;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.CrudProjectDaoGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.util.StringToDate;

import com.jcabi.github.Github;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;

/**
 * 
 * @author THIAGO MAGELA
 */
public class BuscaRepositorioGitHub {

	private CrudProjectDaoGit crud = new CrudProjectDaoGit();

	public void cargaMetaDadosProjects() throws Exception {
		System.out
				.println("Carga dos dados de Projetos (metadados de projetos do github.com).");
		searchRepo();
	}

	private void searchRepo() throws ParseException {

		Github github = new RtGithub("asergufmg", "aserg.ufmg2009");
		List<ProjectGit> listProject = new ArrayList<ProjectGit>();

		// Busca projetos Java ordenados por stars decrescente.
		// https://developer.github.com/v3/search/#search-repositories
		try {
			Iterator<Repo> repos = github.search()
					.repos("language:Java", "stars", "desc").iterator();

			while (repos.hasNext()) {
				try {
					Repo repo = repos.next();
					JsonObject repoData = repo.json();
					ProjectGit p = new ProjectGit();
					p.setName(repoData.getString("name"));
					p.setSize(repoData.getInt("size"));
					p.setStargazers_count(repoData.getInt("stargazers_count"));
					p.setWatchers_count(repoData.getInt("watchers_count"));
					p.setForks_count(repoData.getInt("forks_count"));
					p.setDefault_branch(repoData.getString("default_branch"));
					p.setOpen_issues(repoData.getInt("open_issues"));
					p.setCreated_at(StringToDate.parseDatePatterns(repoData
							.getString("created_at")));
					p.setUpdated_at(StringToDate.parseDatePatterns(repoData
							.getString("updated_at")));
					p.setPushed_at(StringToDate.parseDatePatterns(repoData
							.getString("pushed_at")));
					p.setLanguage(repoData.getString("language"));
					p.setCloneUrl(repoData.getString("clone_url"));
					
					try {
						p.setDescription(repoData.getString("description"));
					} catch (ClassCastException cex) {
						p.setDescription(null);
					}
			
					crud.persistObject(p);

				} catch (ClassCastException cex) {
					cex.getStackTrace();
					System.out.println(cex);
				}
			}
		} catch (IOException e) {
			e.getStackTrace();
		}

	}

}
