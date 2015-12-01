package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.ArrayList;
import java.util.List;

import br.ufmg.dcc.labsoft.refdetector.model.builder.SourceRepresentation;

public abstract class SDEntity implements Comparable<SDEntity> {

	private int id;
	protected final SDModel.Snapshot snapshot;
	protected final String fullName;
	protected final SDContainerEntity container;
	protected final List<SDEntity> children;
	
	public SDEntity(SDModel.Snapshot snapshot, int id, String fullName, SDContainerEntity container) {
		this.snapshot = snapshot;
		this.id = id;
		this.fullName = fullName;
		this.container = container;
		this.children = new ArrayList<SDEntity>();
		if (container != null) {
		    this.container.children.add(this);
		}
	}
	
	public int getId() {
		return id;
	}
	
	int setId(int id) {
		return this.id = id;
	}
	
	public String fullName() {
		return fullName;
	}

	public String fullName(SDEntity parent) {
		return parent.fullName() + getNameSeparator() + simpleName();
	}
	
	protected abstract String getNameSeparator();

	public abstract String simpleName();
	
	public SDContainerEntity container() {
		return container;
	}
	
	public abstract boolean isTestCode();
	
	@Override
	public int hashCode() {
		return fullName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SDEntity) {
			SDEntity e = (SDEntity) obj;
			return e.fullName.equals(fullName);
		}
		return false;
	}

	@Override
	public String toString() {
		return fullName;
	}

	public Iterable<SDEntity> children() {
		return children;
	}
	
	@Override
	public int compareTo(SDEntity o) {
		return fullName.compareTo(o.fullName);
	}

	public boolean matches(SDEntity other) {
	    return this.id == other.id;
	}

	public SourceRepresentation sourceCode() {
	    throw new UnsupportedOperationException();
	}
}
