package br.ufmg.dcc.labsoft.refdetector.model;

public class SDType extends SDContainerEntity {

	public SDType(int id, String fullName) {
		super(id, fullName);
	}

	public boolean isSubtypeOf(SDType supertype) {
		return false;
	}

	public EntitySet<SDType> directSubtypes() {
		return null;
	}

}
