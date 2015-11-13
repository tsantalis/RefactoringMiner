package br.ufmg.dcc.labsoft.refdetector.model;

public class SDAttribute extends SDEntity {

	private final String name; 
	
	public SDAttribute(SDModel.Snapshot snapshot, int id, String name, SDContainerEntity container) {
		super(snapshot, id, container.fullName() + "#" + name, container);
		this.name = name;
	}

	@Override
	public String simpleName() {
		return name;
	}
	
	@Override
	public boolean isTestCode() {
		return container.isTestCode();
	}

	@Override
	protected final String getNameSeparator() {
		return "#";
	}

}
