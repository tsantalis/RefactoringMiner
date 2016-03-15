package br.ufmg.dcc.labsoft.refdetector.model;

public abstract class SDContainerEntity extends SDEntity {

	public SDContainerEntity(SDModel.Snapshot snapshot, int id, String fullName) {
		super(snapshot, id, fullName, null);
	}

	public SDContainerEntity(SDModel.Snapshot snapshot, int id, String fullName, SDContainerEntity container) {
		super(snapshot, id, fullName, container);
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
