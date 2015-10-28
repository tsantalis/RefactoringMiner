package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

@Entity
@Table(
	name = "refactoringmotivation",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"refactoring", "tag"})}
)
public class RefactoringMotivation extends AbstractEntity {

	@ManyToOne
	@JoinColumn(name = "refactoring")
	@Index(name="index_refactoringmotivation_ref")
	private RefactoringGit refactoring;
	
	@ManyToOne
	@JoinColumn(name = "tag")
	@Index(name="index_refactoringmotivation_tag")
	private Tag tag;
	
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public RefactoringGit getRefactoring() {
		return refactoring;
	}

	public void setRefactoring(RefactoringGit refactoring) {
		this.refactoring = refactoring;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

}
