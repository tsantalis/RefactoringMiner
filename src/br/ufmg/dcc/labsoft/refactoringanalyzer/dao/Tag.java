package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(
	name = "tag"
)
public class Tag extends AbstractEntity {

	@Column(unique = true, length = 100)
	private String label;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
