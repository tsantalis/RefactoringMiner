package br.ufmg.dcc.labsoft.refdetector.model;

public class SDEntity {

	private int id;
	private String fullName;
	
	public SDEntity(int id, String fullName) {
		this.id = id;
		this.fullName = fullName;
	}
	
	public int getId() {
		return id;
	}
	
	public String getFullName() {
		return fullName;
	}
	
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

}
