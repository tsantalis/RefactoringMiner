package br.ufmg.dcc.labsoft.refdetector.model;

public class SDPackage extends SDContainerEntity {

	public SDPackage(int id, String fullName) {
		super(id, fullName);
	}

	@Override
	public boolean isTestCode() {
		boolean test = false;
		String[] parts = fullName.split("\\.");
		for (String part : parts) {
			test = test || part.equals("test");
		}
		return test;
	}

}
