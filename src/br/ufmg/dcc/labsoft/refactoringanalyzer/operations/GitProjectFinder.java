package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.IOException;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.refactoringminer.util.StringToDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

import com.jcabi.github.Github;
import com.jcabi.github.RtGithub;
import com.jcabi.http.Request;
import com.jcabi.http.response.JsonResponse;

public class GitProjectFinder {

	Logger logger = LoggerFactory.getLogger(GitProjectFinder.class);
	
	private Database db = new Database();

	public static void main(String[] args) throws IOException {
		GitProjectFinder gitProjectFinder = new GitProjectFinder();
		for (int i = 1; i <= 10; i++) {
			gitProjectFinder.findRepos(i);
		}
	}

	private void findRepos(int page) throws IOException {
		Github github = new RtGithub("asergufmg", "aserg.ufmg2009");
		Request request = github.entry()
				.uri().path("/search/repositories")
				.queryParam("q", "language:Java created:<=2014-06-01")
				.queryParam("sort", "stars")
				.queryParam("order", "desc")
				.queryParam("per_page", "100")
				.queryParam("page", "" + page).back()
				.method(Request.GET);

		JsonArray items = request.fetch().as(JsonResponse.class).json().readObject().getJsonArray("items");
		for (JsonValue item : items) {
			JsonObject repoData = (JsonObject) item;
			ProjectGit p = new ProjectGit();
			p.setName(repoData.getString("name"));
			p.setSize(repoData.getInt("size"));
			p.setFork(repoData.getBoolean("fork"));
			p.setStargazers_count(repoData.getInt("stargazers_count"));
			p.setWatchers_count(repoData.getInt("watchers_count"));
			p.setForks_count(repoData.getInt("forks_count"));
			p.setDefault_branch(repoData.getString("default_branch"));
			p.setOpen_issues(repoData.getInt("open_issues"));
			p.setCreated_at(StringToDate.parseDatePatterns(repoData.getString("created_at")));
			p.setUpdated_at(StringToDate.parseDatePatterns(repoData.getString("updated_at")));
			p.setPushed_at(StringToDate.parseDatePatterns(repoData.getString("pushed_at")));
			p.setLast_update(StringToDate.parseDatePatterns(repoData.getString("pushed_at")));
			p.setLanguage(repoData.getString("language"));
			p.setCloneUrl(repoData.getString("clone_url"));
			p.setStatus("new");
			p.setMonitoring_enabled(false);

			if (!repoData.isNull("description")) {
				p.setDescription(repoData.getString("description"));
			}
			p.setAnalyzed(false);

			db.insertIfNotExists(p);
			this.logger.info("Project {}", p.getCloneUrl());
		}
	}

}
