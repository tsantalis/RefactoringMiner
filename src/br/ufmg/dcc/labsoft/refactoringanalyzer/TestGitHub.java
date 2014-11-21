package br.ufmg.dcc.labsoft.refactoringanalyzer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.json.JsonObject;

import org.eclipse.jgit.api.Git;

import com.jcabi.github.Github;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;
import com.jcabi.github.Search.Order;



public class TestGitHub {

	public static void main(String[] args) {
		int count = 0;
		try {
			Github github = new RtGithub("asergufmg", "aserg.ufmg2009");
			String baseFolder = "test/repos";
			
			// Busca projetos Java de tamanho menor que 5MB ordenados por stars decrescente.
			// https://developer.github.com/v3/search/#search-repositories
			Iterator<Repo> repos = github.search().repos("language:Java", "stars", Order.DESC).iterator();
			
			while(repos.hasNext()) {
				Repo repo = repos.next();
				JsonObject repoData = repo.json();
				
				String name = repoData.getString("name");
				System.out.println("name: " + name);
				
				int size = repoData.getInt("size");
				System.out.println("Size: " + size);
				
				int stargazers_count = repoData.getInt("stargazers_count");
				System.out.println("stargazers_count: " + stargazers_count);
				
				int watchers_count = repoData.getInt("watchers_count");
				System.out.println("watchers_count: " + watchers_count);
				
				
				int forks_count = repoData.getInt("forks_count");
				System.out.println("forks_count: " + forks_count);
				
				
				String default_branch = repoData.getString("default_branch");
				System.out.println("default_branch: " + default_branch);
				
				
				int open_issues = repoData.getInt("open_issues");
				System.out.println("open_issues: " + open_issues);
				
				
				String created_at = repoData.getString("created_at");
				System.out.println("created_at: " + created_at);
				
				
				String updated_at = repoData.getString("updated_at");
				System.out.println("updated_at: " + updated_at);
				
				String pushed_at = repoData.getString("pushed_at");
				System.out.println("pushed_at: " + pushed_at);
				
				String language = repoData.getString("language");
				System.out.println("language: " + language);
				
				String cloneUrl = repoData.getString("clone_url");
				System.out.println("clone URL " + cloneUrl);
				
								
				try{
					String description = repoData.getString("description");
					System.out.println("description: " + description);
					} catch (ClassCastException cex) {
						cex.getMessage();
						System.out.println("description: " + null);
					}
				
				
				//String star = repoData.getString("star");
				//System.out.println("star: " + star);
				
				
				
				
				System.out.println();
				
				File folder = new File(baseFolder + "/" + name);
				//cloneRepository(folder, cloneUrl);
				//System.out.println("Repository cloned at " + folder.toString());
				count++;
			}
			System.out.println("total baixado: " + count);
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


/*public class TestGitHub {

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
}*/
