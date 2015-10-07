package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.HashMap;
import java.util.Map;

public class SDModel {

	enum Mode {
		BEFORE,
		AFTER	
	}
	
	private int size = 0;
	private final Map<String, SDEntity> before = new HashMap<String, SDEntity>();
	private final Map<String, SDEntity> after = new HashMap<String, SDEntity>();
	
	private Mode mode = Mode.AFTER;
	
	private Map<String, SDEntity> getMap() {
		return (mode == Mode.AFTER) ? after : before;
	}
	
	public void setAfter() {
		this.mode = Mode.AFTER;
	}

	public void setBefore() {
		this.mode = Mode.AFTER;
	}
	
	private int getId(String key) {
		SDEntity a = after.get(key);
		SDEntity b = before.get(key);
		if (a == null && b == null) {
			return size++;
		} else if (a != null && mode == Mode.BEFORE) {
			return a.getId();
		} else if (b != null && mode == Mode.AFTER) {
			return b.getId();
		} else {
			throw new RuntimeException("Duplicate entity key: " + key);
		}
	}
	
	public SDPackage getOrCreatePackage(String fullName) {
		SDPackage p = find(SDPackage.class, fullName);
		if (p == null) {
			p = new SDPackage(getId(fullName), fullName);
			getMap().put(fullName, p);
		}
		return p;
	}

	public SDType createType(String typeName, SDContainerEntity container) {
		String fullName = container.getFullName() + "." + typeName;
		SDType sdType = new SDType(getId(fullName), fullName);
		getMap().put(fullName, sdType);
		return sdType;
	}

	public SDMethod createMethod(String methodSignature, SDContainerEntity container) {
		String fullName = container.getFullName() + "#" + methodSignature;
		SDMethod sdMethod = new SDMethod(getId(fullName), fullName);
		getMap().put(fullName, sdMethod);
		return sdMethod;
	}

	public <T extends SDEntity> T find(Class<T> entityType, String key) {
		SDEntity sdEntity = getMap().get(key);
		if (entityType.isInstance(sdEntity)) {
			return entityType.cast(sdEntity);
		}
		return null;
	}
	
}
