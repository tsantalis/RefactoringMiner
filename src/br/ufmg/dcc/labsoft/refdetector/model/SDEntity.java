package br.ufmg.dcc.labsoft.refdetector.model;

public abstract class SDEntity {

	private int id;
	protected final SDModel.Snapshot snapshot;
	protected String fullName;
	protected SDContainerEntity container;
	
	public SDEntity(SDModel.Snapshot snapshot, int id, String fullName, SDContainerEntity container) {
		this.snapshot = snapshot;
		this.id = id;
		this.fullName = fullName;
		this.container = container;
	}
	
	public int getId() {
		return id;
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public abstract boolean isTestCode();
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SDEntity) {
			SDEntity e = (SDEntity) obj;
			return e.id == id;
		}
		return false;
	}

	@Override
	public String toString() {
		return fullName;
	}
}
