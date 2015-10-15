package br.ufmg.dcc.labsoft.refdetector.model;

public abstract class SDContainerEntity extends SDEntity {

	public SDContainerEntity(SDModel.Snapshot snapshot, int id, String fullName) {
		super(snapshot, id, fullName, null);
	}

	public SDContainerEntity(SDModel.Snapshot snapshot, int id, String fullName, SDContainerEntity container) {
		super(snapshot, id, fullName, container);
	}

}
