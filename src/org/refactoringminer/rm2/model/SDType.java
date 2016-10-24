package org.refactoringminer.rm2.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SDType extends SDContainerEntity {

	private String simpleName;
	private String sourceFilePath;
	private boolean isInterface;
	private boolean isAnonymous;
	private boolean isLocalType = false;
	private boolean deprecatedAnnotation;
	private EntitySet<SDType> subtypes = new EntitySet<SDType>();
	//private List<SDType> anonymousClasses = new ArrayList<SDType>();
	private Map<String, List<SDType>> anonymousClasses = new HashMap<String, List<SDType>>();
	private SourceRepresentation source;
	private int nestingLevel;
	private Multiset<SDType> origins;
	private Multiset<SDType> referencedBy;
	
	public SDType(SDModel.Snapshot snapshot, int id, String simpleName, SDContainerEntity container, String sourceFilePath) {
		this(snapshot, id, simpleName, container.fullName() + "." + simpleName, new EntityKey(container.key() + "." + simpleName), container, sourceFilePath, false, false);
	}

	private SDType(SDModel.Snapshot snapshot, int id, String anonymousId, SDType container, String sourceFilePath, boolean localType) {
	    this(snapshot, id, "" + anonymousId, container.fullName() + "$" + anonymousId, new EntityKey(container.key() + "$" + anonymousId), container, sourceFilePath, true, localType);
	}

	private SDType(SDModel.Snapshot snapshot, int id, String simpleName, String fullName, EntityKey key, SDContainerEntity container, String sourceFilePath, boolean isAnonymous, boolean localType) {
	    super(snapshot, id, fullName, key, container);
	    this.simpleName = simpleName;
	    this.sourceFilePath = sourceFilePath;
	    this.isAnonymous = isAnonymous;
	    this.isLocalType = localType;
	    if (container instanceof SDType) {
	        SDType parentType = (SDType) container;
	        this.nestingLevel = parentType.nestingLevel + 1;
	    } else {
	        this.nestingLevel = 0;
	    }
	    this.origins = new Multiset<SDType>();
	    this.referencedBy = new Multiset<SDType>();
	}
	
	@Override
	protected final String getNameSeparator() {
		return isAnonymous ? "$" : ".";
	}
	
	@Override
	public String simpleName() {
		return simpleName;
	}
	
	public boolean isSubtypeOf(SDType supertype) {
		return supertype.subtypes.contains(this);
	}

	public boolean isInterface() {
		return isInterface;
	}

	public boolean isAnonymous() {
	    return isAnonymous || isLocalType;
	}
	
	@Override
	public boolean isPackage() {
		return false;
	}
	
	public List<SDType> getAnonymousClasses(String localName) {
	  List<SDType> list = anonymousClasses.get(localName);
    if (list == null) {
      list = new ArrayList<>();
      anonymousClasses.put(localName, list);
    }
		return list;
	}
	
	public SourceRepresentation sourceCode() {
        return this.source;
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

	public boolean isDeprecated() {
		return deprecatedAnnotation;
	}

	public EntitySet<SDType> subtypes() {
		return subtypes;
	}
	
	public int nestingLevel() {
        return nestingLevel;
    }

	public Multiset<SDType> origins() {
        return this.origins;
    }
	
	public Multiset<SDType> referencedBy() {
        return referencedBy;
    }

    @Override
    public void addReference(SDEntity entity) {
        entity.addReferencedBy(this);
    }
    
    @Override
    public void addReferencedBy(SDType type) {
        this.referencedBy.add(type);
    }
	
    public void addSubtype(SDType type) {
		subtypes.add(type);
	}

	public SDType addAnonymousClass(int id, String localName) {
	  List<SDType> list = getAnonymousClasses(localName);
	  int count = list.size() + 1;
	  String anonId = count + localName;
	  boolean isLocalType = !localName.equals("");
    SDType anon = new SDType(snapshot, id, anonId, this, sourceFilePath, isLocalType);
    list.add(anon);
		return anon;
	}

	public void setDeprecatedAnnotation(boolean deprecatedAnnotation) {
		this.deprecatedAnnotation = deprecatedAnnotation;
	}

	public void setIsInterface(boolean isInterface) {
	    this.isInterface = isInterface;
	}

	public void setSourceCode(SourceRepresentation source) {
        this.source = source;
    }
	
	public void addOrigin(SDType type, int multiplicity) {
        this.origins.add(type, multiplicity);
    }
}
