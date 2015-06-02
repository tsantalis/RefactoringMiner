package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

@Entity
@Table(name = "surveymail")
public class SurveyMail extends AbstractEntity {

	@Index(name="index_surveymail_recipient")
	private String recipient;

	private Date sentDate;
	
	@Column(columnDefinition="TEXT")
	private String body;
	
	@ManyToOne
	@JoinColumn(name = "revision")
	@Index(name="index_surveymail_revision")
	private RevisionGit revision;
	
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public RevisionGit getRevision() {
		return revision;
	}

	public void setRevision(RevisionGit revision) {
		this.revision = revision;
	}

}
