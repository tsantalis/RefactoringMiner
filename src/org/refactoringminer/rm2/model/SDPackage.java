package org.refactoringminer.rm2.model;

public class SDPackage extends SDContainerEntity {

	public SDPackage(SDModel.Snapshot snapshot, int id, String fullName, String sourceFolder) {
		super(snapshot, id, fullName, new EntityKey(sourceFolder + fullName));
	}

	@Override
	public boolean isTestCode() {
		boolean test = false;
		String[] parts = fullName.split("\\.");
		for (String part : parts) {
			test = test || part.endsWith("test");
		}
		return test;
	}

	@Override
	public String simpleName() {
		return fullName;
	}
	
	@Override
	public boolean isPackage() {
		return true;
	}

	@Override
	protected final String getNameSeparator() {
		return ".";
	}
	
}
