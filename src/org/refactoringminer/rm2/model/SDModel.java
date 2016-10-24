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
            return BEFORE.exists(entity.key());
        }
	}
	
    public boolean existsAfter(SDEntity entity) {
        if (isMatched(entity)) {
            return true;
        } else {
            return AFTER.exists(entity.key());
        }
    }
	
	public interface Snapshot {
		
		<T extends SDEntity> T find(Class<T> entityType, EntityKey key);

		<T extends SDEntity> T findByName(Class<T> entityType, String fullName);
		
		boolean exists(EntityKey key);
		
		Collection<SDEntity> getAllEntities();
		
		SDPackage getOrCreatePackage(String fullName, String sourceFolder);
			
		SDType createType(String typeName, SDContainerEntity container, String sourceFilePath);
		
		SDMethod createMethod(String methodSignature, SDContainerEntity container, boolean isConstructor);
		
		SDAttribute createAttribute(String attributeName, SDContainerEntity container);

		SDType createAnonymousType(SDContainerEntity peek, String sourceFilePath, String localName);
		
		Set<SDType> getUnmatchedTypes();

        Set<SDMethod> getUnmatchedMethods();

        Set<SDAttribute> getUnmatchedAttributes();
	}
	
	private class SnapshotImpl implements Snapshot {
		private final Map<EntityKey, SDEntity> map = new HashMap<EntityKey, SDEntity>();
		private final Map<String, EntityKey> nameToKey = new HashMap<String, EntityKey>();

		private final Set<SDType> unmatchedTypes = new TreeSet<SDType>();
	    private final Set<SDMethod> unmatchedMethods = new TreeSet<SDMethod>();
	    private final Set<SDAttribute> unmatchedAttributes = new TreeSet<SDAttribute>();
		
		public <T extends SDEntity> T find(Class<T> entityType, EntityKey key) {
			SDEntity sdEntity = map.get(key);
			if (entityType.isInstance(sdEntity)) {
				return entityType.cast(sdEntity);
			}
			return null;
		}

		public <T extends SDEntity> T findByName(Class<T> entityType, String fullName) {
		  SDEntity sdEntity = map.get(nameToKey.get(fullName));
		  if (entityType.isInstance(sdEntity)) {
		    return entityType.cast(sdEntity);
		  }
		  return null;
		}
		
		private void putAtMap(EntityKey key, SDEntity entity) {
		    if (map.containsKey(key)) {
		        throw new DuplicateEntityException(key.toString());
		    }
		    map.put(key, entity);
		    nameToKey.put(key.toName(), key);
		}
		
		public boolean exists(EntityKey key) {
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

        public SDPackage getOrCreatePackage(String fullName, String sourceFolder) {
			EntityKey key = new EntityKey(sourceFolder + fullName);
      SDPackage p = find(SDPackage.class, key);
			if (p == null) {
				p = new SDPackage(this, getId(), fullName, sourceFolder);
				putAtMap(key, p);
			}
			return p;
		}

		public SDType createType(String typeName, SDContainerEntity container, String sourceFilePath) {
		  EntityKey key = new EntityKey(container.key() + "." + typeName);
			SDType sdType = new SDType(this, getId(), typeName, container, sourceFilePath);
			putAtMap(key, sdType);
			if (!sdType.isAnonymous()) {
			    unmatchedTypes.add(sdType);
			}
			return sdType;
		}

		public SDType createAnonymousType(SDContainerEntity container, String sourceFilePath, String localName) {
			SDType parent = (SDType) container;
			SDType sdType = parent.addAnonymousClass(getId(), localName);
			putAtMap(sdType.key(), sdType);
			return sdType;
		}

		public SDMethod createMethod(String methodSignature, SDContainerEntity container, boolean isConstructor) {
		  EntityKey key = new EntityKey(container.key() + "#" + methodSignature);
			SDMethod sdMethod = new SDMethod(this, getId(), methodSignature, container, isConstructor);
			putAtMap(key, sdMethod);
			if (!sdMethod.isAnonymous()) {
                unmatchedMethods.add(sdMethod);
            }
			return sdMethod;
		}
		
		public SDAttribute createAttribute(String attributeName, SDContainerEntity container) {
		  EntityKey key = new EntityKey(container.key() + "#" + attributeName);
			SDAttribute sdAttribute = new SDAttribute(this, getId(), attributeName, container);
			putAtMap(key, sdAttribute);
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
	            SDEntity entityAfter = AFTER.find(SDEntity.class, entityBefore.key());
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
                EntityKey newKey = childEntity.key(entityAfter);
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

    public boolean hasRelationship(RelationshipType relationshipType, SDEntity entityBefore, SDEntity entityAfter) {
        for (Relationship r : relationships.get(entityBefore.getId())) {
            if (r.getType() == relationshipType && r.getEntityAfter().equals(entityAfter)) {
                return true;
            }
        }
        return false;
    }
}
