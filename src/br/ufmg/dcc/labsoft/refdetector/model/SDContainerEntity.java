package br.ufmg.dcc.labsoft.refdetector.model;

public abstract class SDContainerEntity extends SDEntity {

	public SDContainerEntity(int id, String fullName) {
		super(id, fullName, null);
	}

	public SDContainerEntity(int id, String fullName, SDContainerEntity container) {
		super(id, fullName, container);
	}

}
