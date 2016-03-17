package org.refactoringminer.rm2.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm2.exception.DuplicateEntityException;

public class SDModel {

	private final SnapshotImpl BEFORE;
	private final SnapshotImpl AFTER;

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
	    Relationship matchRelationship = getMatchRelationship(entity);
	    return (T) (matchRelationship != null ? matchRelationship.getEntityBefore() : null);
	}
	
	public <T extends SDEntity> T after(T entity) {
	    Relationship matchRelationship = getMatchRelationship(entity);
	    return (T) (matchRelationship != null ? matchRelationship.getEntityAfter() : null);
	}
	
	public boolean existsBefore(SDEntity entity) {
	    if (isMatched(entity)) {
            return true;
        } else {
            return BEFORE.exists(entity.fullName());
        }
	}
	
    public boolean existsAfter(SDEntity entity) {
        if (isMatched(entity)) {
            return true;
        } else {
            return AFTER.exists(entity.fullName());
        }
    }
	
	public interface Snapshot {
		
		<T extends SDEntity> T find(Class<T> entityType, String key);
		
		boolean exists(String key);
		
		Collection<SDEntity> getAllEntities();
		
		SDPackage getOrCreatePackage(String fullName);
			
		SDType createType(String typeName, SDContainerEntity container, String sourceFilePath);
		
		SDMethod createMethod(String methodSignature, SDContainerEntity container, boolean isConstructor);
		
		SDAttribute createAttribute(String attributeName, SDContainerEntity container);

		SDType createAnonymousType(SDContainerEntity peek, String sourceFilePath);
		
		Set<SDType> getUnmatchedTypes();

        Set<SDMethod> getUnmatchedMethods();

        Set<SDAttribute> getUnmatchedAttributes();
	}
	
	private class SnapshotImpl implements Snapshot {
		private final Map<String, SDEntity> map = new HashMap<String, SDEntity>();

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
		
		private void putAtMap(String key, SDEntity entity) {
		    if (map.containsKey(key)) {
		        throw new DuplicateEntityException(key);
		    }
		    map.put(key, entity);
		}
		
		public boolean exists(String key) {
			return map.containsKey(key);
		}
		
		public Collection<SDEntity> getAllEntities() {
			return map.values();
		}
		
		public Set<SDType> getUnmatchedTypes() {
            return unmatchedTypes;
        }

        public Set<SDMethod> getUnmatchedMethods() {
            return unmatchedMethods;
        }

        public Set<SDAttribute> getUnmatchedAttributes() {
            return unmatchedAttributes;
        }

        public SDPackage getOrCreatePackage(String fullName) {
			SDPackage p = find(SDPackage.class, fullName);
			if (p == null) {
				p = new SDPackage(this, getId(), fullName);
				putAtMap(fullName, p);
			}
			return p;
		}

		public SDType createType(String typeName, SDContainerEntity container, String sourceFilePath) {
			String fullName = container.fullName() + "." + typeName;
			SDType sdType = new SDType(this, getId(), typeName, container, sourceFilePath);
			putAtMap(fullName, sdType);
			if (!sdType.isAnonymous()) {
			    unmatchedTypes.add(sdType);
			}
			return sdType;
		}

		public SDType createAnonymousType(SDContainerEntity container, String sourceFilePath) {
			SDType parent = (SDType) container;
			int anonId = parent.anonymousClasses().size() + 1;
			String fullName = container.fullName() + "$" + anonId;
			SDType sdType = parent.addAnonymousClass(getId(), anonId);
			putAtMap(fullName, sdType);
			return sdType;
		}

		public SDMethod createMethod(String methodSignature, SDContainerEntity container, boolean isConstructor) {
			String fullName = container.fullName() + "#" + methodSignature;
			SDMethod sdMethod = new SDMethod(this, getId(), methodSignature, container, isConstructor);
			putAtMap(fullName, sdMethod);
			if (!sdMethod.isAnonymous()) {
                unmatchedMethods.add(sdMethod);
            }
			return sdMethod;
		}
		
		public SDAttribute createAttribute(String attributeName, SDContainerEntity container) {
			String fullName = container.fullName() + "#" + attributeName;
			SDAttribute sdAttribute = new SDAttribute(this, getId(), attributeName, container);
			putAtMap(fullName, sdAttribute);
			if (!sdAttribute.isAnonymous()) {
                unmatchedAttributes.add(sdAttribute);
            }
			return sdAttribute;
		}

	}
	
	private int nextId = 0;
//	EntitySet<SDMethod> extractedMethods = new EntitySet<SDMethod>();
	List<Refactoring> refactorings = new ArrayList<Refactoring>();
	
	private ArrayList<LinkedList<Relationship>> relationships;
	
	private int getId() {
		return nextId++;
	}
	
	public void initRelationships() {
	    relationships = new ArrayList<LinkedList<Relationship>>(nextId);
	    for (int i = 0; i < nextId; i++) {
	        relationships.add(new LinkedList<Relationship>());
	    }
	    for (SDEntity entityBefore : BEFORE.getAllEntities()) {
	        if (!entityBefore.isAnonymous()) {
	            SDEntity entityAfter = AFTER.find(SDEntity.class, entityBefore.fullName());
	            if (entityAfter != null) {
	                addSameNameRelationship(entityBefore, entityAfter);
	            }
	        }
	    }
	}
	
	public boolean isMatched(SDEntity entity) {
		return getMatchRelationship(entity) != null;
	}
	

	public void addRefactoring(Refactoring ref) {
		refactorings.add(ref);
	}

	public List<Refactoring> getRefactorings() {
		return refactorings;
	}

	public boolean entitiesMatch(SDEntity before, SDEntity after) {
	    Relationship relationship = getMatchRelationship(before);
        return relationship != null && relationship.getEntityAfter().equals(after);
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

	public <T extends SDEntity> boolean addSameNameRelationship(T entityBefore, T entityAfter) {
	    if (entityBefore instanceof SDType && entityAfter instanceof SDType) {
	        SDType typeBefore = (SDType) entityBefore; 
	        SDType typeAfter = (SDType) entityAfter;
	        if (typeBefore.isInterface() && !typeAfter.isInterface()) {
	            return this.addRelationship(RelationshipType.CONVERT_TO_CLASS, entityBefore, entityAfter, 1);
	        }
	        if (!typeBefore.isInterface() && typeAfter.isInterface()) {
	            return this.addRelationship(RelationshipType.CONVERT_TO_INTERFACE, entityBefore, entityAfter, 1);
	        }
	    }
        return this.addRelationship(RelationshipType.SAME, entityBefore, entityAfter, 1);
	}

    public <T extends SDEntity> boolean addRelationship(RelationshipType type, T entityBefore, T entityAfter, int multiplicity) {
        Relationship r = null;// = new Relationship(type, secondary, entityBefore.getId(), entityAfter.getId(), multiplicity);
        if (type.isMatching()) {
            Relationship beforeMatch = getMatchRelationship(entityBefore);
            Relationship afterMatch = getMatchRelationship(entityAfter);
            if (beforeMatch == null && afterMatch == null) {
                r = new Relationship(type, false, entityBefore, entityAfter, multiplicity);
            } else {
                if ((beforeMatch == null && type.isMultisource() && afterMatch.getType() == type) || 
                    (afterMatch == null && type.isMultitarget() && beforeMatch.getType() == type)) {
                    r = new Relationship(type, true, entityBefore, entityAfter, multiplicity);
                }
            }
        } else {
            r = new Relationship(type, false, entityBefore, entityAfter, multiplicity);
        }
        
        if (r == null) {
            return false;
        }
        
        relationships.get(entityBefore.getId()).add(r);
        relationships.get(entityAfter.getId()).add(r);
        
        if (type.isMatching()) {
            BEFORE.unmatchedTypes.remove(entityBefore);
            BEFORE.unmatchedMethods.remove(entityBefore);
            BEFORE.unmatchedAttributes.remove(entityBefore);
            AFTER.unmatchedTypes.remove(entityAfter);
            AFTER.unmatchedMethods.remove(entityAfter);
            AFTER.unmatchedAttributes.remove(entityAfter);
            
            for (SDEntity childEntity : entityBefore.children()) {
                String newKey = childEntity.fullName(entityAfter);
                SDEntity childEntityAfter = after().find(SDEntity.class, newKey);
                if (childEntityAfter != null) {
                    addSameNameRelationship(childEntity, childEntityAfter);
                }
            }
        }
        
        return true;
    }

    public Relationship getMatchRelationship(SDEntity entity) {
        for (Relationship r : relationships.get(entity.getId())) {
            if (r.getType().isMatching()) {
                return r;
            }
        }
        return null;
    }
}
