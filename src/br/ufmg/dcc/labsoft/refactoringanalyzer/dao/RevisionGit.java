package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

@Entity
@NamedQueries({
	@NamedQuery(name = "revisionGit.findByProject", query = "SELECT i FROM RevisionGit i "),
	//@NamedQuery(name = "revisionGit.findByProjectByCommits", query = "SELECT i FROM RevisionGit i where i.top_k = 0 and i.project = :projeto"),
})
public class RevisionGit extends AbstractEntity {

	@Column (unique=true)
	private String idCommit;
	private String idCommitParent;
	private String authorName;
	private String authorIdent;               
	private String encoding;                
	private Date commitTime;

	@Column(columnDefinition="TEXT")
	private String FullMessage;

	@Column(length = 5000)
	private String shortMessage;

	@ManyToOne(cascade = CascadeType.PERSIST) 
	@JoinColumn(name="projectGit_id")
	private ProjectGit projectGit; 

	@OneToMany(mappedBy = "revisiongit", targetEntity = RefactoringGit.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<RefactoringGit> refactorygit;

	public RevisionGit() {
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}


	public ProjectGit getProjectGit() {
		return projectGit;
	}

	public void setProjectGit(ProjectGit projectGit) {
		this.projectGit = projectGit;
	}


	public String getIdCommit() {
		return idCommit;
	}

	public void setIdCommit(String idCommit) {
		this.idCommit = idCommit;
	}

	public String getIdCommitParent() {
		return idCommitParent;
	}

	public void setIdCommitParent(String idCommitPai) {
		this.idCommitParent = idCommitPai;
	}

	public String getAuthorIdent() {
		return authorIdent;
	}

	public void setAuthorIdent(String authorIdent) {
		this.authorIdent = authorIdent;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public Date getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(Date commitTime) {
		this.commitTime = commitTime;
	}

	public String getFullMessage() {
		return FullMessage;
	}

	public void setFullMessage(String FullMessage) {
		this.FullMessage = FullMessage;
	}

	public String getShortMessage() {
		return shortMessage;
	}

	public void setShortMessage(String shortMessage) {
		this.shortMessage = shortMessage;
	}

	public Set<RefactoringGit> getRefactorygit() {
		return refactorygit;
	}

	public void setRefactorygit(Set<RefactoringGit> refactorygit) {
		this.refactorygit = refactorygit;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

}
