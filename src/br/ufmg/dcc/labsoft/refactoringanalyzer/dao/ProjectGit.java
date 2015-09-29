package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

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
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
@NamedQueries({
	@NamedQuery(name = "projectGit.findAll", query = "SELECT i FROM ProjectGit i"),
	@NamedQuery(name = "projectGit.findByCloneUrl", query = "SELECT i FROM ProjectGit i where i.cloneUrl = :cloneUrl"),
	@NamedQuery(name = "projectGit.releaseLocks", query = "update ProjectGit i set i.running_pid = NULL where i.running_pid = :pid"),
	@NamedQuery(name = "projectGit.findNonAnalyzed", query = "SELECT i FROM ProjectGit i where i.analyzed = false and i.running_pid is null and i.status = 'pending' order by rand() asc"),
	@NamedQuery(name = "projectGit.findToMonitor", query = "SELECT i FROM ProjectGit i where i.monitoring_enabled = true and i.running_pid is null and i.last_update < :date order by i.last_update asc"),
	@NamedQuery(name = "projectGit.findNonCounted", query = "SELECT i FROM ProjectGit i where i.analyzed = false and commits_count = 0 and i.running_pid is null order by i.size asc")
})
@Table(name = "projectgit")
public class ProjectGit extends AbstractEntity {

	private String name;
	@Column(unique = true)
	private String cloneUrl;
	private int size;
	private boolean fork;
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
	private String running_pid;
	private String machine;
	private String status;
	private Date last_update;
	private boolean monitoring_enabled;
	
	private int commits_count;
	private int contributors;
	private int java_files;
//	private int merge_commits_count;
	private int error_commits_count;
	private boolean analyzed;

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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

	public boolean isFork() {
		return fork;
	}

	public void setFork(boolean fork) {
		this.fork = fork;
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

	public boolean isAnalyzed() {
		return analyzed;
	}
	
	public String getRunning_pid() {
		return running_pid;
	}

	public void setRunning_pid(String pid) {
		this.running_pid = pid;
	}

	public void setAnalyzed(boolean finalizado) {
		this.analyzed = finalizado;
	}

	public List<RevisionGit> getRevisionGitList() {
		return revisionGitList;
	}

	public void setRevisionGitList(List<RevisionGit> revisionGitList) {
		this.revisionGitList = revisionGitList;
	}

	public int getCommits_count() {
		return commits_count;
	}

	public void setCommits_count(int commits_count) {
		this.commits_count = commits_count;
	}

	public int getContributors() {
		return contributors;
	}

	public void setContributors(int contributors) {
		this.contributors = contributors;
	}

	public int getJava_files() {
		return java_files;
	}

	public void setJava_files(int java_files) {
		this.java_files = java_files;
	}

	public int getError_commits_count() {
		return error_commits_count;
	}

	public void setError_commits_count(int count) {
		this.error_commits_count = count;
	}

//	public int getMerge_commits_count() {
//		return merge_commits_count;
//	}
//
//	public void setMerge_commits_count(int count) {
//		this.merge_commits_count = count;
//	}

	@Override
	public String toString() {
		return this.cloneUrl;
	}

	public Date getLast_update() {
		return last_update;
	}

	public void setLast_update(Date last_update) {
		this.last_update = last_update;
	}

	public boolean isMonitoring_enabled() {
		return monitoring_enabled;
	}

	public void setMonitoring_enabled(boolean monitoring_enabled) {
		this.monitoring_enabled = monitoring_enabled;
	}

	public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
