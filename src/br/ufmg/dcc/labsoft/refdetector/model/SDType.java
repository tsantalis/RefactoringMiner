package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.ArrayList;
import java.util.List;

public class SDType extends SDContainerEntity {

	private String simpleName;
	private String sourceFilePath;
	private boolean isAnonymous;
	private EntitySet<SDType> subtypes = new EntitySet<SDType>();
	private List<SDType> anonymousClasses = new ArrayList<SDType>();
	
	public SDType(SDModel.Snapshot snapshot, int id, String simpleName, SDContainerEntity container, String sourceFilePath) {
		super(snapshot, id, container.getFullName() + "." + simpleName, container);
		this.simpleName = simpleName;
		this.sourceFilePath = sourceFilePath;
		this.isAnonymous = false;
	}

	private SDType(SDModel.Snapshot snapshot, int id, int anonymousId, SDType container, String sourceFilePath) {
		super(snapshot, id, container.getFullName() + "$" + anonymousId, container);
		this.simpleName = "" + anonymousId;
		this.sourceFilePath = sourceFilePath;
		this.isAnonymous = true;
	}
	
	public boolean isSubtypeOf(SDType supertype) {
		return supertype.subtypes.contains(this);
	}

	public boolean isAnonymous() {
		return isAnonymous;
	}
	
	public List<SDType> anonymousClasses() {
		return anonymousClasses;
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

	public EntitySet<SDType> subtypes() {
		return subtypes;
	}
	
	public void addSubtype(SDType type) {
		subtypes.add(type);
	}

	public SDType addAnonymousClass(int id, int anonId) {
		SDType anon = new SDType(snapshot, id, anonId, this, sourceFilePath);
		anonymousClasses.add(anon);
		return anon;
	}

}
