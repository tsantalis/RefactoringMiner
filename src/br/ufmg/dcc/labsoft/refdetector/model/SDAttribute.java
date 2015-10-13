package br.ufmg.dcc.labsoft.refdetector.model;

public class SDAttribute extends SDEntity {

	public SDAttribute(int id, String fullName, SDContainerEntity container) {
		super(id, fullName, container);
	}

	@Override
	public boolean isTestCode() {
		return container.isTestCode();
	}

}
