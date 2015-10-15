package br.ufmg.dcc.labsoft.refdetector.model;

public class SDAttribute extends SDEntity {

	public SDAttribute(SDModel.Snapshot snapshot, int id, String fullName, SDContainerEntity container) {
		super(snapshot, id, fullName, container);
	}

	@Override
	public boolean isTestCode() {
		return container.isTestCode();
	}

}
