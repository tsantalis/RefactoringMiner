package br.ufmg.dcc.labsoft.refdetector.model;

public class SDType extends SDContainerEntity {

	private String simpleName;
	private String sourceFilePath;
	
	public SDType(int id, String simpleName, SDContainerEntity container, String sourceFilePath) {
		super(id, container.getFullName() + "." + simpleName, container);
		this.simpleName = simpleName;
		this.sourceFilePath = sourceFilePath;
	}

	public boolean isSubtypeOf(SDType supertype) {
		return false;
	}

	public EntitySet<SDType> directSubtypes() {
		return null;
	}

	@Override
	public boolean isTestCode() {
		if (simpleName.endsWith("Test") || simpleName.startsWith("Test")) {
			return true;
		}
		
		boolean test = false;
		String[] parts = sourceFilePath.split("/");
		for (String part : parts) {
			test = test || part.equals("test") || part.equals("tests") || part.equals("src-test");
		}
		return test;
	}
}
