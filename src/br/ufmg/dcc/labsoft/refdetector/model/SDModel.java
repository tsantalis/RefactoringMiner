package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SDModel {

	final SnapshotImpl BEFORE;
	final SnapshotImpl AFTER;

	public SDModel() {
		BEFORE = new SnapshotImpl();
		AFTER = new SnapshotImpl();
	}
	
	public Snapshot before() {
		return BEFORE;
	}

	public Snapshot after() {
		return AFTER;
	}
	
	public <T extends SDEntity> T before(T entity) {
		return BEFORE.get(entity.getId());
	}
	
	public <T extends SDEntity> T after(T entity) {
		return AFTER.get(entity.getId());
	}
	
	public interface Snapshot {
		
		<T extends SDEntity> T find(Class<T> entityType, String key);
		
		Collection<SDEntity> getAllEntities();
		
		SDPackage getOrCreatePackage(String fullName);
			
		SDType createType(String typeName, SDContainerEntity container, String sourceFilePath);
		
		SDMethod createMethod(String methodSignature, SDContainerEntity container);
		
		SDAttribute createAttribute(String attributeName, SDContainerEntity container);

		SDType createAnonymousType(SDContainerEntity peek, String sourceFilePath);
		
	}
	
	private class SnapshotImpl implements Snapshot {
		private final Map<String, SDEntity> map = new HashMap<String, SDEntity>();
		private final ArrayList<SDEntity> entities = new ArrayList<SDEntity>(1000);

		public <T extends SDEntity> T find(Class<T> entityType, String key) {
			SDEntity sdEntity = map.get(key);
			if (entityType.isInstance(sdEntity)) {
				return entityType.cast(sdEntity);
			}
			return null;
		}
		
		public <T extends SDEntity> T get(int id) {
			if (id >= entities.size()) {
				return null;
			}
			return (T) entities.get(id);
		}
		
		@Override
		public Collection<SDEntity> getAllEntities() {
			return map.values();
		}
		
		private void addEntity(SDEntity entity) {
			map.put(entity.fullName, entity);
			int index = entity.getId();
			while (index > entities.size()) {
				entities.add(null);
			}
			entities.add(index, entity);
//			if (entity.fullName.startsWith("org.bitcoinj.core.BitcoinSerializer")) {
//				System.out.println("Entity: " + entity.fullName);
//			}
		}

		public SDPackage getOrCreatePackage(String fullName) {
			SDPackage p = find(SDPackage.class, fullName);
			if (p == null) {
				p = new SDPackage(this, getId(fullName, this), fullName);
				addEntity(p);
			}
			return p;
		}

		public SDType createType(String typeName, SDContainerEntity container, String sourceFilePath) {
			String fullName = container.getFullName() + "." + typeName;
			SDType sdType = new SDType(this, getId(fullName, this), typeName, container, sourceFilePath);
			addEntity(sdType);
			return sdType;
		}

		public SDType createAnonymousType(SDContainerEntity container, String sourceFilePath) {
			SDType parent = (SDType) container;
			int anonId = parent.anonymousClasses().size() + 1;
			String fullName = container.getFullName() + "$" + anonId;
			SDType sdType = parent.addAnonymousClass(getId(fullName, this), anonId);
			return sdType;
		}

		public SDMethod createMethod(String methodSignature, SDContainerEntity container) {
			String fullName = container.getFullName() + "#" + methodSignature;
			SDMethod sdMethod = new SDMethod(this, getId(fullName, this), methodSignature, container);
			addEntity(sdMethod);
			return sdMethod;
		}
		
		public SDAttribute createAttribute(String attributeName, SDContainerEntity container) {
			String fullName = container.getFullName() + "#" + attributeName;
			SDAttribute sdAttribute = new SDAttribute(this, getId(fullName, this), fullName, container);
			addEntity(sdAttribute);
			return sdAttribute;
		}

	}
	
	private int size = 0;
	EntitySet<SDMethod> extractedMethods = new EntitySet<SDMethod>();
	
	private int getId(String key, Snapshot snapshot) {
//		if (key.equals("io.realm.Realm#allObjectsSorted(Class, String, boolean)")) {
//			System.out.println();
//		}
		SDEntity a = AFTER.map.get(key);
		SDEntity b = BEFORE.map.get(key);
		if (a == null && b == null) {
			return size++;
		} else if (a != null && snapshot == BEFORE) {
			return a.getId();
		} else if (b != null && snapshot == AFTER) {
			return b.getId();
		} else {
			throw new RuntimeException("Duplicate entity key: " + key);
		}
	}
	
	public void reportExtractedMethod(SDMethod extracted, SDMethod from) {
		extracted.addOrigin(from);
		extractedMethods.add(extracted);
	}
}
