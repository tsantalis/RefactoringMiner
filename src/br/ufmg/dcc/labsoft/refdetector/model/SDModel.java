package br.ufmg.dcc.labsoft.refdetector.model;

import gr.uom.java.xmi.diff.Refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
		return BEFORE.get(entity);
	}
	
	public <T extends SDEntity> T after(T entity) {
		return AFTER.get(entity);
	}
	
	public interface Snapshot {
		
		<T extends SDEntity> T find(Class<T> entityType, String key);
		
		boolean exists(String key);
		
		boolean exists(SDEntity entity);
		
		Collection<SDEntity> getAllEntities();
		
		Iterable<SDMethod> getUnmatchedMethods();
		
		Iterable<SDType> getUnmatchedTypes();
		
		SDPackage getOrCreatePackage(String fullName);
			
		SDType createType(String typeName, SDContainerEntity container, String sourceFilePath);
		
		SDMethod createMethod(String methodSignature, SDContainerEntity container, boolean isConstructor);
		
		SDAttribute createAttribute(String attributeName, SDContainerEntity container);

		SDType createAnonymousType(SDContainerEntity peek, String sourceFilePath);
		
	}
	
	private class SnapshotImpl implements Snapshot {
		private final Map<String, SDEntity> map = new HashMap<String, SDEntity>();
		//private final ArrayList<SDEntity> entities = new ArrayList<SDEntity>(1000);
		private final ArrayList<SDEntity> matched = new ArrayList<SDEntity>();
		private final Set<SDPackage> unmatchedPackages = new TreeSet<SDPackage>();
		private final Set<SDType> unmatchedTypes = new TreeSet<SDType>();
		private final Set<SDMethod> unmatchedMethods = new TreeSet<SDMethod>();
		private final Set<SDAttribute> unmatchedAttributes = new TreeSet<SDAttribute>();

		public <T extends SDEntity> T find(Class<T> entityType, String key) {
			SDEntity sdEntity = map.get(key);
			if (entityType.isInstance(sdEntity)) {
				return entityType.cast(sdEntity);
			}
			return null;
		}
		
		public boolean exists(String key) {
			return map.containsKey(key);
		}
		
		public boolean exists(SDEntity entity) {
			if (isMatched(entity)) {
				return true;
			} else {
				return map.containsKey(entity.fullName());
			}
		}

		public <T extends SDEntity> T get(T entity) {
			if (isMatched(entity)) {
				return (T) matched.get(entity.getId());
			} else {
				return (T) map.get(entity.fullName());
			}
		}
		
		@Override
		public Collection<SDEntity> getAllEntities() {
			return map.values();
		}
		
		@Override
		public Iterable<SDMethod> getUnmatchedMethods() {
			return unmatchedMethods;
		}
		
		@Override
		public Iterable<SDType> getUnmatchedTypes() {
			return unmatchedTypes;
		}
		
		void addAsMatched(SDEntity entity) {
			matched.add(entity);
			unmatchedPackages.remove(entity);
			unmatchedTypes.remove(entity);
			unmatchedMethods.remove(entity);
			unmatchedAttributes.remove(entity);
		}
		
		public SDPackage getOrCreatePackage(String fullName) {
			SDPackage p = find(SDPackage.class, fullName);
			if (p == null) {
				p = new SDPackage(this, getId(), fullName);
				matchByFullName(p, this);
				map.put(fullName, p);
				if (!isMatched(p)) {
					unmatchedPackages.add(p);
				}
			}
			return p;
		}

		public SDType createType(String typeName, SDContainerEntity container, String sourceFilePath) {
			String fullName = container.fullName() + "." + typeName;
			SDType sdType = new SDType(this, getId(), typeName, container, sourceFilePath);
			matchByFullName(sdType, this);
			map.put(fullName, sdType);
			if (!isMatched(sdType)) {
				unmatchedTypes.add(sdType);
			}
			return sdType;
		}

		public SDType createAnonymousType(SDContainerEntity container, String sourceFilePath) {
			SDType parent = (SDType) container;
			int anonId = parent.anonymousClasses().size() + 1;
			String fullName = container.fullName() + "$" + anonId;
			SDType sdType = parent.addAnonymousClass(getId(), anonId);
			//matchByFullName(sdType, this);
			map.put(fullName, sdType);
			return sdType;
		}

		public SDMethod createMethod(String methodSignature, SDContainerEntity container, boolean isConstructor) {
			String fullName = container.fullName() + "#" + methodSignature;
			SDMethod sdMethod = new SDMethod(this, getId(), methodSignature, container, isConstructor);
			matchByFullName(sdMethod, this);
			map.put(fullName, sdMethod);
			if (!isMatched(sdMethod)) {
				unmatchedMethods.add(sdMethod);
			}
			return sdMethod;
		}
		
		public SDAttribute createAttribute(String attributeName, SDContainerEntity container) {
			String fullName = container.fullName() + "#" + attributeName;
			SDAttribute sdAttribute = new SDAttribute(this, getId(), attributeName, container);
			matchByFullName(sdAttribute, this);
			map.put(fullName, sdAttribute);
			if (!isMatched(sdAttribute)) {
				unmatchedAttributes.add(sdAttribute);
			}
			return sdAttribute;
		}

	}
	
	private int nextId = 0;
//	EntitySet<SDMethod> extractedMethods = new EntitySet<SDMethod>();
	List<Refactoring> refactorings = new ArrayList<Refactoring>();
	
	private int getId() {
		// unmatched ID's are negative numbers
		int entityId = -((nextId++) + 1);
		return entityId;
	}
	
	private boolean matchByFullName(SDEntity entity, Snapshot snapshot) {
		String key = entity.fullName();
		SDEntity a = AFTER.map.get(key);
		SDEntity b = BEFORE.map.get(key);
		
		if (a != null && snapshot == AFTER || b != null && snapshot == BEFORE) {
			throw new RuntimeException("Duplicate entity key: " + key);
		}
		if (a != null && snapshot == BEFORE) {
			matchEntities(entity, a);
			return true;
		}
		if (b != null && snapshot == AFTER) {
			matchEntities(b, entity);
			return true;
		}
		return false;
	}
	
	public void matchEntities(SDEntity entityBefore, SDEntity sameEntityAfter) {
		if (entityBefore.getId() == sameEntityAfter.getId()) {
			return;
		}
		if (isMatched(entityBefore)) {
			throw new IllegalArgumentException(entityBefore + " is already matched");
		}
		if (isMatched(sameEntityAfter)) {
			throw new IllegalArgumentException(sameEntityAfter + " is already matched");
		}
		int newId = BEFORE.matched.size();
		entityBefore.setId(newId);
		BEFORE.addAsMatched(entityBefore);
		sameEntityAfter.setId(newId);
		AFTER.addAsMatched(sameEntityAfter);
		
		for (SDEntity childEntity : entityBefore.children()) {
			String newKey = childEntity.fullName(sameEntityAfter);
			SDEntity childEntityAfter = after().find(SDEntity.class, newKey);
			if (childEntityAfter != null) {
				matchEntities(childEntity, childEntityAfter);
			}
		}
	}
	
	private boolean isMatched(SDEntity entity) {
		int id = entity.getId();
		return id >= 0;
	}
	
	public void reportExtractedMethod(SDMethod extracted, SDMethod from) {
		extracted.addOrigin(from, 1);
	}

	public void addRefactoring(Refactoring ref) {
		refactorings.add(ref);
	}

	public List<Refactoring> getRefactorings() {
		return refactorings;
	}

	public <T extends SDEntity> Filter<T> isMatched() {
		return new Filter<T>() {
			@Override
			public boolean accept(T element) {
				return isMatched(element);
			}
		};
	}

	public <T extends SDEntity> Filter<T> isUnmatched() {
		return new NotFilter<T>(this.<T>isMatched());
	}
}
