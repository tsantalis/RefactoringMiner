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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@NamedQueries({
	@NamedQuery(name = "revisionGit.findByProjectAndCommit", query = "SELECT i FROM RevisionGit i where i.project = :project and i.commitId = :commitId"),
	@NamedQuery(name = "revisionGit.findByProjectAndExtractMethod", query = "select distinct rev from RefactoringGit ref join ref.revision as rev join rev.project as p where refactoringType in ('Extract Operation', 'Extract & Move Operation') and p.cloneUrl = :cloneUrl")
})
@Table(
	name = "revisiongit",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"project", "commitId"})}
)
public class RevisionGit extends AbstractEntity {

	private String commitId;
	private String commitIdParent;
	private String committerName;
	private String committerEmail;               
	private String authorName;
	private String authorEmail;               
	private String encoding;                
	private Date commitTime;

	private Boolean mentionsRefactoring;
	private Boolean describesRefactoring;
	private Boolean describesFeatureOrBug;
	private Boolean ok;
	
	@Column(columnDefinition="TEXT")
	private String FullMessage;

	@Column(length = 5000)
	private String shortMessage;

	@ManyToOne(cascade = CascadeType.PERSIST) 
	@JoinColumn(name="project")
	private ProjectGit project; 

	@OneToMany(mappedBy = "revision", targetEntity = RefactoringGit.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<RefactoringGit> refactorings;

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
		return project;
	}

	public void setProjectGit(ProjectGit projectGit) {
		this.project = projectGit;
	}


	public String getIdCommit() {
		return commitId;
	}

	public void setIdCommit(String idCommit) {
		this.commitId = idCommit;
	}

	public String getIdCommitParent() {
		return commitIdParent;
	}

	public void setIdCommitParent(String idCommitPai) {
		this.commitIdParent = idCommitPai;
	}

	public String getAuthorEmail() {
		return authorEmail;
	}

	public void setAuthorEmail(String authorIdent) {
		this.authorEmail = authorIdent;
	}

	public String getCommiterEmail() {
		return committerEmail;
	}
	
	public void setCommitterEmail(String authorIdent) {
		this.committerEmail = authorIdent;
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

	public Set<RefactoringGit> getRefactorings() {
		return refactorings;
	}

	public void setRefactorings(Set<RefactoringGit> refactorygit) {
		this.refactorings = refactorygit;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public String getCommiterName() {
		return committerName;
	}
	
	public void setCommitterName(String authorName) {
		this.committerName = authorName;
	}

	public Boolean getMentionsRefactoring() {
		return mentionsRefactoring;
	}

	public void setMentionsRefactoring(Boolean mentionsRefactoring) {
		this.mentionsRefactoring = mentionsRefactoring;
	}

	public Boolean getDescribesRefactoring() {
		return describesRefactoring;
	}

	public void setDescribesRefactoring(Boolean describesRefactoring) {
		this.describesRefactoring = describesRefactoring;
	}

	public Boolean getDescribesFeatureOrBug() {
		return describesFeatureOrBug;
	}

	public void setDescribesFeatureOrBug(Boolean describesFeatureOrBug) {
		this.describesFeatureOrBug = describesFeatureOrBug;
	}

	public Boolean getOk() {
		return ok;
	}

	public void setOk(Boolean ok) {
		this.ok = ok;
	}

}
