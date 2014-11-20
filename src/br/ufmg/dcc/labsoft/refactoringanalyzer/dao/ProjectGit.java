package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
@NamedQueries({ @NamedQuery(name = "projectGit.findAll", query = "SELECT i FROM ProjectGit i") })
public class ProjectGit extends Entidade implements Serializable {

	private String name;
	@Column(unique = true)
	private String cloneUrl;
	private int size;
	private int stargazers_count;
	private int watchers_count;
	private int forks_count;
	private String default_branch;
	private int open_issues;
	private Date created_at;
	private Date updated_at;
	private Date pushed_at;
	@Column(length = 5000)
	private String description;
	private String language;

	private int countCommits;
	private int countCommitsNotParents;
	private boolean finalizado;

	@OneToMany(mappedBy = "projectGit", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<RevisionGit> revisionGitList = new ArrayList<RevisionGit>();

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCloneUrl() {
		return cloneUrl;
	}

	public void setCloneUrl(String cloneUrl) {
		this.cloneUrl = cloneUrl;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getStargazers_count() {
		return stargazers_count;
	}

	public void setStargazers_count(int stargazers_count) {
		this.stargazers_count = stargazers_count;
	}

	public int getWatchers_count() {
		return watchers_count;
	}

	public void setWatchers_count(int watchers_count) {
		this.watchers_count = watchers_count;
	}

	public int getForks_count() {
		return forks_count;
	}

	public void setForks_count(int forks_count) {
		this.forks_count = forks_count;
	}

	public String getDefault_branch() {
		return default_branch;
	}

	public void setDefault_branch(String default_branch) {
		this.default_branch = default_branch;
	}

	public int getOpen_issues() {
		return open_issues;
	}

	public void setOpen_issues(int open_issues) {
		this.open_issues = open_issues;
	}

	public Date getCreated_at() {
		return created_at;
	}

	public void setCreated_at(Date created_at) {
		this.created_at = created_at;
	}

	public Date getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(Date updated_at) {
		this.updated_at = updated_at;
	}

	public Date getPushed_at() {
		return pushed_at;
	}

	public void setPushed_at(Date pushed_at) {
		this.pushed_at = pushed_at;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isFinalizado() {
		return finalizado;
	}

	public void setFinalizado(boolean finalizado) {
		this.finalizado = finalizado;
	}

	public List<RevisionGit> getRevisionGitList() {
		return revisionGitList;
	}

	public void setRevisionGitList(List<RevisionGit> revisionGitList) {
		this.revisionGitList = revisionGitList;
	}

	public int getCountCommits() {
		return countCommits;
	}

	public void setCountCommits(int countCommits) {
		this.countCommits = countCommits;
	}

	public int getCountCommitsNotParents() {
		return countCommitsNotParents;
	}

	public void setCountCommitsNotParents(int countCommitsNotParents) {
		this.countCommitsNotParents = countCommitsNotParents;
	}

}
