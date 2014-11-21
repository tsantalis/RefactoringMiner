package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;


import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
	//@NamedQuery(name = "refactoringGit.findRefactoringDuplicado", query = "SELECT i FROM RefactoringGit i where i.hashOperacao = :hashOperacao")
})
public class RefactoringGit extends AbstractEntity {

	private String refactoringType;

	@Column(length = 15000)
	private String description;

	@ManyToOne
	@JoinColumn(name = "revisiongit_id")
	private RevisionGit revisiongit;


	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public String getRefactoringType() {
		return refactoringType;
	}

	public void setRefactoringType(String tipoOperacao) {
		this.refactoringType = tipoOperacao;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String operacaoCompleta) {
		this.description = operacaoCompleta;
	}

	public RevisionGit getRevisiongit() {
		return revisiongit;
	}

	public void setRevisiongit(RevisionGit revisiongit) {
		this.revisiongit = revisiongit;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + Objects.hashCode(this.refactoringType);
		hash = 89 * hash + Objects.hashCode(this.revisiongit);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final RefactoringGit other = (RefactoringGit) obj;
		if (!Objects.equals(this.description, other.description)) {
			return false;
		}
		if (!Objects.equals(this.revisiongit, other.revisiongit)) {
			return false;
		}
		return true;
	}

}
