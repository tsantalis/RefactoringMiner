package org.refactoringminer.rm2.model;

public abstract class SDContainerEntity extends SDEntity {

	public SDContainerEntity(SDModel.Snapshot snapshot, int id, String fullName, EntityKey key) {
		super(snapshot, id, fullName, key, null);
	}

	public SDContainerEntity(SDModel.Snapshot snapshot, int id, String fullName, EntityKey key, SDContainerEntity container) {
		super(snapshot, id, fullName, key, container);
	}

	public abstract boolean isPackage();

	public boolean isNestedIn(SDContainerEntity other) {
		if (this.fullName.length() > other.fullName.length() && this.fullName.startsWith(other.fullName)) {
			char sep = this.fullName.charAt(other.fullName.length());
			return sep == '.' || sep == '$';
		}
		return false;
	}

}
