package br.ufmg.dcc.labsoft.refactoringanalyzer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.json.JsonObject;

import org.eclipse.jgit.api.Git;

import com.jcabi.github.Github;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;

public class TestGitHub {

	public static void main(String[] args) {
		int count = 0;
		try {
			Github github = new RtGithub("asergufmg", "aserg.ufmg2009");
			String baseFolder = "test/repos";
			
			// Busca projetos Java de tamanho menor que 5MB ordenados por stars decrescente.
			// https://developer.github.com/v3/search/#search-repositories
			Iterator<Repo> repos = github.search().repos("language:Java size:<5000", "stars", "desc").iterator();
			while(repos.hasNext() && count < 5) {
				Repo repo = repos.next();
				JsonObject repoData = repo.json();
				String name = repoData.getString("name");
				String cloneUrl = repoData.getString("clone_url");
				System.out.println(cloneUrl);
				File folder = new File(baseFolder + "/" + name);
				cloneRepository(folder, cloneUrl);
				System.out.println("Repository cloned at " + folder.toString());
				count++;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	static void cloneRepository(File folder, String cloneUrl) {
		Git.cloneRepository()
			.setDirectory(folder)
			.setURI(cloneUrl)
			.setCloneAllBranches(false)
			.call();
	}
}
